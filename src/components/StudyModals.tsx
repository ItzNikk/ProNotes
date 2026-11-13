import React, { useEffect, useMemo, useState } from 'react';
import {
  BookOpen,
  Brain,
  Check,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Copy,
  Cpu,
  Download,
  Edit3,
  FileDown,
  Flame,
  HardDrive,
  Pause,
  Plus,
  RotateCcw,
  Search,
  Shuffle,
  Sparkles,
  Trash2,
  Trophy,
  X,
  Zap,
} from 'lucide-react';
import { Flashcard, LocalModelState, PetBondState, PetId, QuizQuestion } from '../types/models';
import { LOCAL_MODELS_CATALOG, RichSummaryResult } from '../utils/localAi';
import { AiParticleBackdrop, PipPet } from './PipPet';

export interface ModalPetProps {
  petId: PetId;
  petName?: string;
  bond?: PetBondState;
  onSwitchPet?: (id: PetId) => void;
  onRenamePet?: (name: string) => void;
  onPetAction?: (kind: 'pet' | 'treat' | 'study', xpGain?: number) => void;
}

// ================================================================
// Local AI Model Hub Modal (9 Free Lightweight Local Models)
// ================================================================

interface LocalAiModalProps {
  open: boolean;
  modelStates: Record<string, LocalModelState>;
  activeModelId: string | null;
  petProps?: ModalPetProps;
  onClose: () => void;
  onDownload: (id: string) => void;
  onPause: (id: string) => void;
  onDelete: (id: string) => void;
  onActivate: (id: string | null) => void;
}

export const LocalAiModal: React.FC<LocalAiModalProps> = ({
  open,
  modelStates,
  activeModelId,
  petProps,
  onClose,
  onDownload,
  onPause,
  onDelete,
  onActivate,
}) => {
  const [catFilter, setCatFilter] = useState<'all' | 'fast' | 'balanced' | 'reasoning' | 'math'>('all');

  if (!open) return null;

  const readyModels = LOCAL_MODELS_CATALOG.filter((m) => modelStates[m.id]?.status === 'READY');
  const usedMb = readyModels.reduce((sum, m) => sum + m.sizeMb, 0);
  const downloadingAny = Object.values(modelStates).some((s) => s.status === 'DOWNLOADING');
  const filteredCatalog = LOCAL_MODELS_CATALOG.filter(
    (m) => catFilter === 'all' || m.category === catFilter
  );

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/55 p-4 backdrop-blur-sm">
      <div className="relative flex max-h-[88vh] w-full max-w-2xl flex-col overflow-hidden rounded-3xl border border-black/10 bg-white shadow-2xl dark:border-white/10 dark:bg-[#191A1F] animate-stagger-in">
        <AiParticleBackdrop accent="blue" variant="ai-wave" intensity={downloadingAny ? 'active' : 'soft'} />
        <div className="relative z-10 flex items-center justify-between border-b border-black/8 px-6 py-4 dark:border-white/10">
          <div className="flex items-center gap-3">
            <div className="relative flex h-10 w-10 items-center justify-center rounded-2xl bg-gradient-to-tr from-violet-600 to-blue-500 text-white shadow">
              <Cpu className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold tracking-tight">On-Device Local AI Models</h2>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                9 free lightweight quantized models cached on-device · 100% private & offline
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            {petProps && (
              <PipPet
                petId={petProps.petId}
                petName={petProps.petName}
                bond={petProps.bond}
                thinking={downloadingAny}
                align="right"
                onSwitchPet={petProps.onSwitchPet}
                onRenamePet={petProps.onRenamePet}
                onPetAction={petProps.onPetAction}
              />
            )}
            <button
              type="button"
              onClick={onClose}
              className="rounded-full p-2 text-slate-500 hover:bg-black/5 dark:hover:bg-white/10"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>

        <div className="relative z-10 flex flex-wrap items-center justify-between gap-2 bg-slate-50/85 px-6 py-3 text-xs backdrop-blur-xs dark:bg-[#14151A]/85">
          <div className="flex items-center gap-2 text-slate-600 dark:text-slate-300">
            <HardDrive className="h-4 w-4 text-blue-600" />
            <span className="font-mono-num font-semibold">{usedMb} MB</span> cached on device (
            {readyModels.length} ready)
          </div>
          <div className="text-slate-500 dark:text-slate-400">
            Active:{' '}
            <span className="font-semibold text-violet-600 dark:text-violet-400">
              {LOCAL_MODELS_CATALOG.find((m) => m.id === activeModelId)?.name ||
                'Built-in Study Engine'}
            </span>
          </div>
        </div>

        {/* Category Tabs */}
        <div className="relative z-10 flex gap-1.5 overflow-x-auto border-b border-black/6 px-6 py-2.5 dark:border-white/8">
          {(
            [
              { id: 'all', label: `All (${LOCAL_MODELS_CATALOG.length})` },
              { id: 'fast', label: 'Ultra-Fast' },
              { id: 'balanced', label: 'Balanced' },
              { id: 'reasoning', label: 'Deep Reasoning' },
              { id: 'math', label: 'STEM & Math' },
            ] as const
          ).map((tab) => (
            <button
              key={tab.id}
              type="button"
              onClick={() => setCatFilter(tab.id)}
              className={`shrink-0 rounded-xl px-3 py-1.5 text-xs font-semibold transition ${
                catFilter === tab.id
                  ? 'bg-violet-600 text-white shadow-sm'
                  : 'bg-black/5 text-slate-600 hover:bg-black/10 dark:bg-white/8 dark:text-slate-300'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <div className="relative z-10 flex-1 space-y-3 overflow-y-auto p-6">
          {filteredCatalog.map((model) => {
            const st = modelStates[model.id] || {
              status: 'NOT_DOWNLOADED',
              progress: 0,
              downloadedBytes: 0,
              totalBytes: model.sizeMb * 1024 * 1024,
            };
            const isReady = st.status === 'READY';
            const isDownloading = st.status === 'DOWNLOADING';
            const isActive = activeModelId === model.id && isReady;

            return (
              <div
                key={model.id}
                className={`rounded-2xl border p-4 transition ${
                  isActive
                    ? 'border-violet-500 bg-violet-50/60 dark:border-violet-500/70 dark:bg-violet-950/20'
                    : 'border-black/10 bg-white dark:border-white/10 dark:bg-[#1E2027]'
                }`}
              >
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <h3 className="text-sm font-bold text-slate-900 dark:text-white">
                        {model.name}
                      </h3>
                      {model.recommended && (
                        <span className="rounded-md bg-blue-600/12 px-2 py-0.5 text-[10px] font-bold text-blue-600 dark:text-blue-300">
                          Recommended
                        </span>
                      )}
                      <span className="font-mono-num rounded-md bg-black/5 px-2 py-0.5 text-[10px] font-semibold text-slate-600 dark:bg-white/10 dark:text-slate-300">
                        {model.sizeMb} MB · {model.quantization}
                      </span>
                    </div>
                    <p className="mt-1 text-xs leading-relaxed text-slate-600 dark:text-slate-300">
                      {model.description}
                    </p>
                    <div className="mt-2.5 flex flex-wrap items-center gap-2 text-[11px] text-slate-500 dark:text-slate-400">
                      <span className="inline-flex items-center gap-1 font-medium text-emerald-600 dark:text-emerald-400">
                        <Zap className="h-3 w-3" /> {model.speedTag}
                      </span>
                      <span>·</span>
                      <span>{model.ramRequired}</span>
                      <span>·</span>
                      <span>{model.capabilities.join(' · ')}</span>
                    </div>
                    <div className="mt-2 flex items-center gap-2">
                      <span className="text-[10px] font-semibold uppercase tracking-wider text-slate-400">
                        Speed
                      </span>
                      <div className="h-1.5 w-24 overflow-hidden rounded-full bg-black/8 dark:bg-white/10">
                        <div
                          className="h-full rounded-full bg-gradient-to-r from-emerald-500 to-blue-500"
                          style={{ width: `${model.speedScore}%` }}
                        />
                      </div>
                    </div>
                  </div>

                  <div className="flex shrink-0 items-center gap-2">
                    {st.status === 'NOT_DOWNLOADED' || st.status === 'PAUSED' || st.status === 'ERROR' ? (
                      <button
                        type="button"
                        onClick={() => onDownload(model.id)}
                        className="inline-flex items-center gap-1.5 rounded-xl bg-blue-600 px-3.5 py-2 text-xs font-semibold text-white shadow-sm hover:bg-blue-700"
                      >
                        <Download className="h-3.5 w-3.5" />
                        {st.status === 'PAUSED' ? 'Resume' : 'Download'}
                      </button>
                    ) : isDownloading ? (
                      <button
                        type="button"
                        onClick={() => onPause(model.id)}
                        className="inline-flex items-center gap-1.5 rounded-xl bg-amber-500/15 px-3.5 py-2 text-xs font-semibold text-amber-700 hover:bg-amber-500/25 dark:text-amber-300"
                      >
                        <Pause className="h-3.5 w-3.5" />
                        Pause
                      </button>
                    ) : (
                      <>
                        <button
                          type="button"
                          onClick={() => onActivate(isActive ? null : model.id)}
                          className={`inline-flex items-center gap-1.5 rounded-xl px-3.5 py-2 text-xs font-semibold transition ${
                            isActive
                              ? 'bg-violet-600 text-white shadow-sm'
                              : 'bg-black/5 text-slate-800 hover:bg-black/10 dark:bg-white/10 dark:text-slate-100'
                          }`}
                        >
                          <CheckCircle2 className="h-3.5 w-3.5" />
                          {isActive ? 'Active' : 'Use Model'}
                        </button>
                        <button
                          type="button"
                          onClick={() => onDelete(model.id)}
                          title="Remove from device storage"
                          className="rounded-xl p-2 text-slate-400 hover:bg-red-500/10 hover:text-red-500"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </>
                    )}
                  </div>
                </div>

                {isDownloading && (
                  <div className="mt-3 space-y-1.5">
                    <div className="flex items-center justify-between text-[11px] font-medium">
                      <span className="text-blue-600 dark:text-blue-400">
                        Caching quantized weights into device storage…
                      </span>
                      <span className="font-mono-num text-slate-600 dark:text-slate-300">
                        {Math.round(st.progress * 100)}% ({st.speedMbps ?? 18} MB/s)
                      </span>
                    </div>
                    <div className="h-2 w-full overflow-hidden rounded-full bg-black/8 dark:bg-white/10">
                      <div
                        className="h-full rounded-full bg-gradient-to-r from-blue-600 via-violet-500 to-sky-400 transition-all duration-150"
                        style={{ width: `${Math.round(st.progress * 100)}%` }}
                      />
                    </div>
                  </div>
                )}

                {isReady && st.shaTag && (
                  <div className="mt-2.5 flex items-center gap-2 text-[11px] text-emerald-600 dark:text-emerald-400">
                    <CheckCircle2 className="h-3.5 w-3.5" />
                    <span>Cached offline · Verified shard {st.shaTag}</span>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

// ================================================================
// Redesigned Summary & Explain Sheet
// ================================================================

export const SummaryModal: React.FC<{
  open: boolean;
  title: string;
  summary: RichSummaryResult | null;
  loadingStage?: string | null;
  explainText?: string | null;
  petProps?: ModalPetProps;
  onClose: () => void;
  onAddToPage: (text: string) => void;
  onTurnIntoMindMap: () => void;
}> = ({
  open,
  title,
  summary,
  loadingStage,
  explainText,
  petProps,
  onClose,
  onAddToPage,
  onTurnIntoMindMap,
}) => {
  const [copied, setCopied] = useState(false);

  if (!open) return null;

  const fullCopyText = explainText
    ? explainText
    : summary
      ? [
          summary.headline,
          '',
          ...summary.points.map((p, i) => `${i + 1}. ${p}`),
          ...(summary.definitions.length
            ? ['', 'Key Definitions:', ...summary.definitions.map((d) => `• ${d.term}: ${d.meaning}`)]
            : []),
          ...(summary.formulas.length ? ['', 'Formulas:', ...summary.formulas] : []),
        ].join('\n')
      : '';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/55 p-4 backdrop-blur-sm">
      <div className="relative flex max-h-[88vh] w-full max-w-2xl flex-col overflow-hidden rounded-3xl border border-black/10 bg-white shadow-2xl dark:border-white/10 dark:bg-[#191A1F] animate-stagger-in">
        <AiParticleBackdrop accent="blue" variant="ai-wave" intensity={loadingStage ? 'active' : 'soft'} />
        {/* Header */}
        <div className="relative z-10 overflow-hidden border-b border-black/8 bg-gradient-to-r from-violet-600/12 via-blue-600/8 to-transparent px-6 py-4 dark:border-white/10">
          {loadingStage && (
            <div className="pointer-events-none absolute inset-0 w-1/2 bg-gradient-to-r from-transparent via-violet-400/20 to-transparent animate-ai-shimmer" />
          )}
          <div className="relative flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="relative flex h-9 w-9 items-center justify-center rounded-xl bg-violet-600 text-white shadow-sm">
                <Sparkles className="h-4.5 w-4.5" />
              </div>
              <div>
                <h2 className="text-base font-bold">{title}</h2>
                <p className="text-[11px] font-medium text-violet-700 dark:text-violet-300">
                  {loadingStage || summary?.engineLabel || 'On-Device Synthesis'}
                </p>
              </div>
            </div>
            <div className="flex items-center gap-2">
              {petProps && (
                <PipPet
                  petId={petProps.petId}
                  petName={petProps.petName}
                  bond={petProps.bond}
                  thinking={Boolean(loadingStage)}
                  align="right"
                  onSwitchPet={petProps.onSwitchPet}
                  onRenamePet={petProps.onRenamePet}
                  onPetAction={petProps.onPetAction}
                />
              )}
              <button
                type="button"
                onClick={onClose}
                className="rounded-full p-1.5 text-slate-500 hover:bg-black/5 dark:hover:bg-white/10"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
          </div>
        </div>

        {/* Body */}
        <div className="relative z-10 flex-1 space-y-4 overflow-y-auto p-6">
          {loadingStage ? (
            <div className="space-y-3 py-4">
              <div className="rounded-2xl border border-violet-500/25 bg-violet-50/50 p-4 dark:bg-violet-950/20">
                <div className="flex items-center justify-between text-xs font-bold text-violet-700 dark:text-violet-300">
                  <span>{loadingStage}…</span>
                  <span>Running locally</span>
                </div>
                <div className="mt-2.5 h-1.5 w-full overflow-hidden rounded-full bg-violet-500/15">
                  <div className="h-full w-1/3 rounded-full bg-gradient-to-r from-violet-600 via-sky-500 to-emerald-400 animate-ai-bar" />
                </div>
              </div>
              {[1, 2, 3].map((row) => (
                <div
                  key={row}
                  className="relative h-16 overflow-hidden rounded-2xl bg-slate-100 dark:bg-[#22242C]"
                >
                  <div className="absolute inset-0 w-1/2 bg-gradient-to-r from-transparent via-white/50 to-transparent dark:via-white/10 animate-ai-shimmer" />
                </div>
              ))}
            </div>
          ) : explainText ? (
            <div className="rounded-2xl border border-violet-500/20 bg-violet-50/50 p-5 text-sm leading-relaxed text-slate-800 dark:bg-violet-950/20 dark:text-slate-100 animate-stagger-in">
              {explainText}
            </div>
          ) : summary ? (
            <>
              {/* Synthesis Banner */}
              <div className="rounded-2xl border border-blue-500/20 bg-gradient-to-br from-blue-50/70 to-violet-50/60 p-4 dark:from-blue-950/25 dark:to-violet-950/25 animate-stagger-in">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="text-[11px] font-bold uppercase tracking-wider text-blue-700 dark:text-blue-300">
                    Study Digest
                  </span>
                  <span className="font-mono-num rounded-md bg-white/75 px-2 py-0.5 text-[11px] font-semibold text-slate-600 dark:bg-black/30 dark:text-slate-300">
                    {summary.wordCount} words scanned
                  </span>
                </div>
                <p className="mt-1.5 text-sm font-bold text-slate-900 dark:text-white">
                  {summary.headline}
                </p>
                {summary.keywords.length > 0 && (
                  <div className="mt-2.5 flex flex-wrap gap-1.5">
                    {summary.keywords.map((kw) => (
                      <span
                        key={kw}
                        className="rounded-lg bg-white/80 px-2 py-0.5 text-[11px] font-semibold text-slate-700 shadow-2xs dark:bg-white/10 dark:text-slate-200"
                      >
                        #{kw}
                      </span>
                    ))}
                  </div>
                )}
              </div>

              {/* Key Takeaways */}
              <div className="space-y-2">
                <div className="text-[11px] font-bold uppercase tracking-wider text-slate-400">
                  Key Takeaways
                </div>
                {summary.points.map((pt, idx) => (
                  <div
                    key={idx}
                    style={{ animationDelay: `${idx * 65}ms` }}
                    className="flex items-start gap-3 rounded-2xl border border-black/8 bg-slate-50/70 p-3.5 text-sm leading-relaxed text-slate-800 dark:border-white/8 dark:bg-[#20222A] dark:text-slate-100 animate-stagger-in"
                  >
                    <span className="font-mono-num mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-lg bg-violet-600 text-[11px] font-bold text-white">
                      {idx + 1}
                    </span>
                    <span>{pt}</span>
                  </div>
                ))}
              </div>

              {/* Key Terms & Definitions */}
              {summary.definitions.length > 0 && (
                <div className="space-y-2">
                  <div className="text-[11px] font-bold uppercase tracking-wider text-slate-400">
                    Key Concepts ({summary.definitions.length})
                  </div>
                  <div className="grid gap-2.5 sm:grid-cols-2">
                    {summary.definitions.map((d, i) => (
                      <div
                        key={i}
                        style={{ animationDelay: `${(i + 2) * 55}ms` }}
                        className="rounded-2xl border border-black/8 bg-white p-3 dark:border-white/10 dark:bg-[#22242C] animate-stagger-in"
                      >
                        <div className="text-xs font-bold text-blue-600 dark:text-blue-400">
                          {d.term}
                        </div>
                        <div className="mt-1 text-xs leading-relaxed text-slate-600 dark:text-slate-300">
                          {d.meaning}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Equations & Formulas */}
              {summary.formulas.length > 0 && (
                <div className="space-y-2">
                  <div className="text-[11px] font-bold uppercase tracking-wider text-slate-400">
                    Extracted Equations
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {summary.formulas.map((f, i) => (
                      <span
                        key={i}
                        className="font-mono-num rounded-xl border border-amber-500/25 bg-amber-50/70 px-3 py-1.5 text-xs font-semibold text-amber-900 dark:bg-amber-950/25 dark:text-amber-200"
                      >
                        {f}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </>
          ) : null}
        </div>

        {/* Footer */}
        <div className="relative z-10 flex flex-wrap items-center justify-between gap-2 border-t border-black/8 px-6 py-4 dark:border-white/10">
          <button
            type="button"
            disabled={!fullCopyText}
            onClick={() => {
              navigator.clipboard?.writeText(fullCopyText);
              setCopied(true);
              window.setTimeout(() => setCopied(false), 1600);
            }}
            className="inline-flex items-center gap-1.5 rounded-xl border border-black/10 px-3 py-2 text-xs font-semibold text-slate-700 hover:bg-black/5 disabled:opacity-40 dark:border-white/10 dark:text-slate-200"
          >
            {copied ? <Check className="h-3.5 w-3.5 text-emerald-500" /> : <Copy className="h-3.5 w-3.5" />}
            {copied ? 'Copied' : 'Copy All'}
          </button>

          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              disabled={!!loadingStage}
              onClick={onTurnIntoMindMap}
              className="inline-flex items-center gap-1.5 rounded-xl border border-violet-500/30 bg-violet-500/10 px-3.5 py-2 text-xs font-semibold text-violet-700 hover:bg-violet-500/20 disabled:opacity-40 dark:text-violet-300"
            >
              <Brain className="h-3.5 w-3.5" /> Build Mind Map
            </button>
            <button
              type="button"
              disabled={!!loadingStage || !fullCopyText}
              onClick={() => {
                onAddToPage(fullCopyText);
                onClose();
              }}
              className="inline-flex items-center gap-1.5 rounded-xl bg-blue-600 px-4 py-2 text-xs font-semibold text-white hover:bg-blue-700 disabled:opacity-40"
            >
              <Plus className="h-3.5 w-3.5" /> Add Sticky to Page
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

// ================================================================
// Redesigned Flashcards Studio (SM-2 4-Grade + Deck Manager)
// ================================================================

export const FlashcardsModal: React.FC<{
  open: boolean;
  notebookTitle: string;
  notebookId: string;
  cards: Flashcard[];
  loadingStage?: string | null;
  petProps?: ModalPetProps;
  onClose: () => void;
  onAddCard: (front: string, back: string) => void;
  onUpdateCard: (card: Flashcard) => void;
  onDeleteCard: (id: string) => void;
  onGenerateAi: () => void;
  onStudyXp?: () => void;
}> = ({
  open,
  notebookTitle,
  cards,
  loadingStage,
  petProps,
  onClose,
  onAddCard,
  onUpdateCard,
  onDeleteCard,
  onGenerateAi,
  onStudyXp,
}) => {
  const [tab, setTab] = useState<'study' | 'manage'>('study');
  const [idx, setIdx] = useState(0);
  const [flipped, setFlipped] = useState(false);
  const [frontInput, setFrontInput] = useState('');
  const [backInput, setBackInput] = useState('');
  const [editingId, setEditingId] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [orderIds, setOrderIds] = useState<string[] | null>(null);

  useEffect(() => {
    setOrderIds(null);
    setIdx(0);
    setFlipped(false);
  }, [open, cards.length]);

  const deck = useMemo(() => {
    if (!orderIds) return cards;
    const map = new Map(cards.map((c) => [c.id, c]));
    const ordered = orderIds.map((id) => map.get(id)).filter((c): c is Flashcard => !!c);
    return ordered.length === cards.length ? ordered : cards;
  }, [cards, orderIds]);

  const safeIdx = deck.length > 0 ? idx % deck.length : 0;
  const current = deck[safeIdx];

  const stats = useMemo(() => {
    const now = Date.now();
    const due = cards.filter((c) => c.due <= now).length;
    const mastered = cards.filter((c) => c.box >= 4).length;
    const learning = cards.length - mastered;
    return { due, learning, mastered };
  }, [cards]);

  const rateCard = (grade: 0 | 1 | 2 | 3) => {
    if (!current) return;
    let nextBox = current.box;
    let hours = 12;
    if (grade === 0) {
      nextBox = 1;
      hours = 0;
    } else if (grade === 1) {
      nextBox = Math.max(1, current.box);
      hours = 12;
    } else if (grade === 2) {
      nextBox = Math.min(5, current.box + 1);
      hours = Math.pow(2, nextBox - 1) * 24;
    } else {
      nextBox = Math.min(5, current.box + 2);
      hours = Math.pow(2, nextBox) * 24;
    }
    onUpdateCard({
      ...current,
      box: nextBox,
      due: Date.now() + hours * 3600000,
      lapses: (current.lapses || 0) + (grade === 0 ? 1 : 0),
      lastGrade: grade,
    });
    onStudyXp?.();
    setFlipped(false);
    setIdx((prev) => (deck.length > 0 ? (prev + 1) % deck.length : 0));
  };

  useEffect(() => {
    if (!open || tab !== 'study' || !current) return;
    const onKey = (e: KeyboardEvent) => {
      const tag = (e.target as HTMLElement)?.tagName;
      if (tag === 'INPUT' || tag === 'TEXTAREA') return;
      if (e.code === 'Space') {
        e.preventDefault();
        setFlipped((f) => !f);
      } else if (e.key === '1') rateCard(0);
      else if (e.key === '2') rateCard(1);
      else if (e.key === '3') rateCard(2);
      else if (e.key === '4') rateCard(3);
      else if (e.key === 'ArrowRight') {
        setFlipped(false);
        setIdx((i) => (deck.length ? (i + 1) % deck.length : 0));
      } else if (e.key === 'ArrowLeft') {
        setFlipped(false);
        setIdx((i) => (deck.length ? (i - 1 + deck.length) % deck.length : 0));
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  });

  if (!open) return null;

  const exportCsv = () => {
    const csv = ['"Front","Back","Box"']
      .concat(cards.map((c) => `"${c.front.replace(/"/g, '""')}","${c.back.replace(/"/g, '""')}",${c.box}`))
      .join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${notebookTitle}-flashcards.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const filteredManage = cards.filter(
    (c) =>
      !searchTerm.trim() ||
      c.front.toLowerCase().includes(searchTerm.toLowerCase()) ||
      c.back.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/65 p-4 backdrop-blur-sm">
      <div className="relative flex max-h-[90vh] w-full max-w-2xl flex-col overflow-hidden rounded-3xl border border-white/15 bg-[#060916] text-white shadow-2xl animate-stagger-in">
        <AiParticleBackdrop
          accent="blue"
          variant={loadingStage ? 'ai-wave' : 'space-nebula'}
          intensity={loadingStage ? 'active' : 'soft'}
        />
        {/* Top Bar */}
        <div className="relative z-10 flex flex-wrap items-center justify-between gap-2 border-b border-white/12 bg-black/25 px-6 py-4 backdrop-blur-md">
          <div>
            <h2 className="text-lg font-bold tracking-tight text-white">Flashcards · {notebookTitle}</h2>
            <div className="mt-1 flex flex-wrap items-center gap-2 text-[11px] font-semibold">
              <span className="font-mono-num rounded-md bg-sky-500/20 px-2 py-0.5 text-sky-200 border border-sky-400/25">
                {stats.due} due
              </span>
              <span className="font-mono-num rounded-md bg-amber-500/20 px-2 py-0.5 text-amber-200 border border-amber-400/25">
                {stats.learning} learning
              </span>
              <span className="font-mono-num rounded-md bg-emerald-500/20 px-2 py-0.5 text-emerald-200 border border-emerald-400/25">
                {stats.mastered} mastered
              </span>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {petProps && (
              <PipPet
                petId={petProps.petId}
                petName={petProps.petName}
                bond={petProps.bond}
                thinking={Boolean(loadingStage)}
                align="right"
                onSwitchPet={petProps.onSwitchPet}
                onRenamePet={petProps.onRenamePet}
                onPetAction={petProps.onPetAction}
              />
            )}
            <button
              type="button"
              onClick={onGenerateAi}
              disabled={!!loadingStage}
              className="inline-flex items-center gap-1.5 rounded-xl border border-sky-400/30 bg-sky-500/20 px-3 py-1.5 text-xs font-semibold text-sky-200 hover:bg-sky-500/30 disabled:opacity-50"
            >
              <Sparkles className="h-3.5 w-3.5" />
              {loadingStage ? loadingStage : 'AI Generate'}
            </button>
            {cards.length > 0 && (
              <button
                type="button"
                onClick={exportCsv}
                title="Export CSV"
                className="rounded-xl border border-white/15 bg-white/8 p-2 text-slate-200 hover:bg-white/15"
              >
                <FileDown className="h-4 w-4" />
              </button>
            )}
            <button
              type="button"
              onClick={onClose}
              className="rounded-full p-1.5 text-slate-300 hover:bg-white/12 hover:text-white"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>

        {/* Mode Tabs */}
        <div className="relative z-10 flex items-center justify-between border-b border-white/10 bg-[#0A1024]/60 px-6 py-2 backdrop-blur-md">
          <div className="flex gap-1.5">
            <button
              type="button"
              onClick={() => setTab('study')}
              className={`inline-flex items-center gap-1.5 rounded-xl px-3.5 py-1.5 text-xs font-semibold transition ${
                tab === 'study'
                  ? 'bg-sky-500/25 text-white border border-sky-400/35 shadow-sm'
                  : 'text-slate-300 hover:text-white hover:bg-white/8'
              }`}
            >
              <BookOpen className="h-3.5 w-3.5" /> Study Deck
            </button>
            <button
              type="button"
              onClick={() => setTab('manage')}
              className={`inline-flex items-center gap-1.5 rounded-xl px-3.5 py-1.5 text-xs font-semibold transition ${
                tab === 'manage'
                  ? 'bg-sky-500/25 text-white border border-sky-400/35 shadow-sm'
                  : 'text-slate-300 hover:text-white hover:bg-white/8'
              }`}
            >
              <Edit3 className="h-3.5 w-3.5" /> Manage Cards ({cards.length})
            </button>
          </div>

          {tab === 'study' && deck.length > 1 && (
            <button
              type="button"
              onClick={() => {
                const shuffled = [...deck].sort(() => Math.random() - 0.5).map((c) => c.id);
                setOrderIds(shuffled);
                setIdx(0);
                setFlipped(false);
              }}
              className="inline-flex items-center gap-1 rounded-lg px-2.5 py-1 text-xs font-semibold text-slate-200 hover:bg-white/10"
            >
              <Shuffle className="h-3.5 w-3.5" /> Shuffle
            </button>
          )}
        </div>

        {/* Main Content */}
        <div className="relative z-10 flex-1 overflow-y-auto p-6">
          {loadingStage && (
            <div className="mb-4 rounded-2xl border border-sky-400/30 bg-sky-950/55 p-3.5 backdrop-blur-md">
              <div className="text-xs font-bold text-sky-200">
                {loadingStage}…
              </div>
              <div className="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-sky-500/20">
                <div className="h-full w-1/3 rounded-full bg-gradient-to-r from-blue-500 to-cyan-300 animate-ai-bar" />
              </div>
            </div>
          )}

          {tab === 'study' ? (
            current ? (
              <div className="space-y-5">
                <div className="flex items-center justify-between text-xs text-slate-300">
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() => {
                        setFlipped(false);
                        setIdx((i) => (i - 1 + deck.length) % deck.length);
                      }}
                      className="rounded-lg border border-white/15 bg-white/8 p-1 text-white hover:bg-white/15"
                    >
                      <ChevronLeft className="h-4 w-4" />
                    </button>
                    <span className="font-mono-num font-semibold text-white">
                      Card {safeIdx + 1} / {deck.length} · Box {current.box}
                    </span>
                    <button
                      type="button"
                      onClick={() => {
                        setFlipped(false);
                        setIdx((i) => (i + 1) % deck.length);
                      }}
                      className="rounded-lg border border-white/15 bg-white/8 p-1 text-white hover:bg-white/15"
                    >
                      <ChevronRight className="h-4 w-4" />
                    </button>
                  </div>
                  <span className="hidden text-[11px] text-slate-300 sm:inline">
                    Space to flip · 1–4 to rate
                  </span>
                </div>

                {/* Progress strip */}
                <div className="h-1.5 w-full overflow-hidden rounded-full bg-white/12">
                  <div
                    className="h-full rounded-full bg-gradient-to-r from-sky-400 to-indigo-400 transition-all duration-300"
                    style={{ width: `${Math.round(((safeIdx + 1) / deck.length) * 100)}%` }}
                  />
                </div>

                <div
                  onClick={() => setFlipped((f) => !f)}
                  className="perspective-1000 cursor-pointer select-none"
                >
                  <div
                    className={`preserve-3d relative min-h-[220px] w-full transition-transform duration-500 ${
                      flipped ? 'rotate-y-180' : ''
                    }`}
                  >
                    <div className="backface-hidden absolute inset-0 flex flex-col items-center justify-center rounded-3xl border border-white/20 bg-[#0E1630]/80 p-8 text-center shadow-2xl backdrop-blur-xl">
                      <span className="mb-3 rounded-lg border border-sky-400/30 bg-sky-500/20 px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-widest text-sky-200">
                        Prompt · Tap to Reveal
                      </span>
                      <p className="text-lg font-semibold leading-snug text-white">
                        {current.front}
                      </p>
                    </div>
                    <div className="backface-hidden rotate-y-180 absolute inset-0 flex flex-col items-center justify-center rounded-3xl border border-emerald-400/35 bg-[#0A2226]/85 p-8 text-center shadow-2xl backdrop-blur-xl">
                      <span className="mb-3 rounded-lg border border-emerald-400/30 bg-emerald-500/20 px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-widest text-emerald-200">
                        Answer
                      </span>
                      <p className="text-base font-medium leading-relaxed text-emerald-50">
                        {current.back}
                      </p>
                    </div>
                  </div>
                </div>

                <div className="grid grid-cols-4 gap-2">
                  {(
                    [
                      { grade: 0 as const, label: 'Again', sub: 'Now', cls: 'border border-red-400/30 bg-red-500/18 text-red-200 hover:bg-red-500/30' },
                      { grade: 1 as const, label: 'Hard', sub: '12h', cls: 'border border-amber-400/30 bg-amber-500/18 text-amber-200 hover:bg-amber-500/30' },
                      { grade: 2 as const, label: 'Good', sub: `${Math.pow(2, Math.min(4, current.box))}d`, cls: 'border border-sky-400/30 bg-sky-500/20 text-sky-200 hover:bg-sky-500/30' },
                      { grade: 3 as const, label: 'Easy', sub: `${Math.pow(2, Math.min(5, current.box + 1))}d`, cls: 'border border-emerald-400/30 bg-emerald-500/20 text-emerald-200 hover:bg-emerald-500/30' },
                    ]
                  ).map((g) => (
                    <button
                      key={g.grade}
                      type="button"
                      onClick={() => rateCard(g.grade)}
                      className={`flex flex-col items-center rounded-2xl py-2.5 text-xs font-bold backdrop-blur-md transition ${g.cls}`}
                    >
                      <span>{g.label}</span>
                      <span className="font-mono-num text-[10px] font-medium opacity-80">{g.sub}</span>
                    </button>
                  ))}
                </div>
              </div>
            ) : (
              <div className="py-12 text-center">
                <p className="text-sm font-medium text-slate-300">
                  No flashcards yet. Use AI Generate above or switch to Manage Cards to create one.
                </p>
              </div>
            )
          ) : (
            <div className="space-y-4">
              {/* Composer */}
              <div className="space-y-2.5 rounded-2xl border border-white/15 bg-[#0D142B]/80 p-4 backdrop-blur-md">
                <div className="text-xs font-bold uppercase tracking-wider text-sky-200">
                  {editingId ? 'Edit Flashcard' : 'Create New Flashcard'}
                </div>
                <input
                  value={frontInput}
                  onChange={(e) => setFrontInput(e.target.value)}
                  placeholder="Front / Question prompt"
                  className="w-full rounded-xl border border-white/15 bg-white/8 px-3 py-2 text-sm text-white placeholder:text-slate-400 outline-none focus:border-sky-400"
                />
                <textarea
                  value={backInput}
                  onChange={(e) => setBackInput(e.target.value)}
                  placeholder="Back / Answer or definition"
                  rows={2}
                  className="w-full rounded-xl border border-white/15 bg-white/8 px-3 py-2 text-sm text-white placeholder:text-slate-400 outline-none focus:border-sky-400"
                />
                <div className="flex justify-end gap-2">
                  {editingId && (
                    <button
                      type="button"
                      onClick={() => {
                        setEditingId(null);
                        setFrontInput('');
                        setBackInput('');
                      }}
                      className="rounded-xl px-3 py-1.5 text-xs font-semibold text-slate-300 hover:text-white"
                    >
                      Cancel Edit
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={() => {
                      if (!frontInput.trim() || !backInput.trim()) return;
                      if (editingId) {
                        const target = cards.find((c) => c.id === editingId);
                        if (target) {
                          onUpdateCard({ ...target, front: frontInput.trim(), back: backInput.trim() });
                        }
                        setEditingId(null);
                      } else {
                        onAddCard(frontInput.trim(), backInput.trim());
                      }
                      setFrontInput('');
                      setBackInput('');
                    }}
                    className="inline-flex items-center gap-1.5 rounded-xl bg-sky-500 px-4 py-1.5 text-xs font-semibold text-slate-950 hover:bg-sky-400"
                  >
                    <Plus className="h-3.5 w-3.5" />
                    {editingId ? 'Update Card' : 'Add Card'}
                  </button>
                </div>
              </div>

              {/* Search & Card List */}
              {cards.length > 0 && (
                <div className="relative">
                  <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-400" />
                  <input
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    placeholder="Filter cards..."
                    className="w-full rounded-xl border border-white/15 bg-white/8 py-1.5 pl-8 pr-3 text-xs text-white placeholder:text-slate-400 outline-none focus:border-sky-400"
                  />
                </div>
              )}

              <div className="space-y-2">
                {filteredManage.map((c) => (
                  <div
                    key={c.id}
                    className="flex items-start justify-between gap-3 rounded-2xl border border-white/15 bg-[#0E1630]/80 p-3.5 backdrop-blur-md"
                  >
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2">
                        <span className="font-mono-num rounded bg-sky-500/20 px-1.5 py-0.5 text-[10px] font-bold text-sky-200">
                          Box {c.box}
                        </span>
                        <span className="truncate text-sm font-semibold text-white">
                          {c.front}
                        </span>
                      </div>
                      <p className="mt-1 text-xs text-slate-300">{c.back}</p>
                    </div>
                    <div className="flex shrink-0 items-center gap-1">
                      <button
                        type="button"
                        onClick={() => {
                          setEditingId(c.id);
                          setFrontInput(c.front);
                          setBackInput(c.back);
                        }}
                        className="rounded-lg p-1.5 text-slate-300 hover:bg-white/10 hover:text-sky-300"
                      >
                        <Edit3 className="h-3.5 w-3.5" />
                      </button>
                      <button
                        type="button"
                        onClick={() => onDeleteCard(c.id)}
                        className="rounded-lg p-1.5 text-slate-300 hover:bg-red-500/20 hover:text-red-300"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

// ================================================================
// Redesigned Quiz Studio Modal
// ================================================================

export const QuizModal: React.FC<{
  open: boolean;
  notebookTitle: string;
  questions: QuizQuestion[];
  bestScore?: number;
  engineLabel: string;
  loadingStage?: string | null;
  petProps?: ModalPetProps;
  onClose: () => void;
  onScoreRecorded: (pct: number) => void;
}> = ({
  open,
  notebookTitle,
  questions,
  bestScore = 0,
  engineLabel,
  loadingStage,
  petProps,
  onClose,
  onScoreRecorded,
}) => {
  const [pool, setPool] = useState<QuizQuestion[]>(questions);
  const [qIdx, setQIdx] = useState(0);
  const [picked, setPicked] = useState<number | null>(null);
  const [correctCount, setCorrectCount] = useState(0);
  const [streak, setStreak] = useState(0);
  const [bestStreak, setBestStreak] = useState(0);
  const [missed, setMissed] = useState<QuizQuestion[]>([]);
  const [elapsedSec, setElapsedSec] = useState(0);
  const [quizPhase, setQuizPhase] = useState<'start' | 'playing' | 'finished'>('start');

  useEffect(() => {
    if (open) {
      setPool(questions);
      setQIdx(0);
      setPicked(null);
      setCorrectCount(0);
      setStreak(0);
      setBestStreak(0);
      setMissed([]);
      setElapsedSec(0);
      setQuizPhase('start');
    }
  }, [open, questions]);

  useEffect(() => {
    if (!open || quizPhase !== 'playing' || pool.length === 0 || !!loadingStage) return;
    const t = window.setInterval(() => setElapsedSec((s) => s + 1), 1000);
    return () => window.clearInterval(t);
  }, [open, quizPhase, pool.length, loadingStage]);

  if (!open) return null;

  const q = pool[qIdx];
  const isSpaceStart = quizPhase === 'start' && !loadingStage;

  const handlePick = (optIdx: number) => {
    if (picked !== null || !q) return;
    setPicked(optIdx);
    if (optIdx === q.answer) {
      setCorrectCount((c) => c + 1);
      setStreak((s) => {
        const next = s + 1;
        setBestStreak((b) => Math.max(b, next));
        return next;
      });
    } else {
      setStreak(0);
      setMissed((m) => [...m, q]);
    }
  };

  const handleNext = () => {
    if (qIdx + 1 >= pool.length) {
      setQuizPhase('finished');
      const pct = Math.round((correctCount / Math.max(1, pool.length)) * 100);
      onScoreRecorded(pct);
    } else {
      setQIdx((i) => i + 1);
      setPicked(null);
    }
  };

  const pct = Math.round((correctCount / Math.max(1, pool.length)) * 100);
  const radius = 44;
  const circ = 2 * Math.PI * radius;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
      <div
        className={`relative flex max-h-[90vh] w-full max-w-xl flex-col overflow-hidden rounded-3xl border shadow-2xl animate-stagger-in ${
          isSpaceStart
            ? 'border-white/15 bg-[#060916] text-white'
            : 'border-black/10 bg-white dark:border-white/10 dark:bg-[#191A1F]'
        }`}
      >
        <AiParticleBackdrop
          accent={isSpaceStart ? 'blue' : 'amber'}
          variant={loadingStage ? 'ai-wave' : quizPhase === 'start' ? 'space-nebula' : 'quiz-challenge'}
          intensity={loadingStage ? 'active' : 'soft'}
        />
        {/* Header */}
        <div
          className={`relative z-10 flex items-center justify-between border-b px-6 py-4 ${
            isSpaceStart
              ? 'border-white/12 bg-black/25 backdrop-blur-md'
              : 'border-black/8 dark:border-white/10'
          }`}
        >
          <div>
            <h2 className="text-base font-bold">Quiz Studio · {notebookTitle}</h2>
            <p className={`text-[11px] ${isSpaceStart ? 'text-slate-300' : 'text-slate-500'}`}>
              {loadingStage || engineLabel} · Best:{' '}
              <span className={`font-mono-num font-semibold ${isSpaceStart ? 'text-sky-300' : 'text-blue-600 dark:text-blue-400'}`}>
                {bestScore}%
              </span>
            </p>
          </div>
          <div className="flex items-center gap-2">
            {streak > 1 && (
              <span className="inline-flex items-center gap-1 rounded-lg bg-amber-500/15 px-2.5 py-1 text-xs font-bold text-amber-700 dark:text-amber-300">
                <Flame className="h-3.5 w-3.5" /> {streak}
              </span>
            )}
            {quizPhase !== 'start' && (
              <span className="font-mono-num rounded-lg bg-black/5 px-2.5 py-1 text-xs font-semibold dark:bg-white/10">
                {Math.floor(elapsedSec / 60)}:{String(elapsedSec % 60).padStart(2, '0')}
              </span>
            )}
            {petProps && (
              <PipPet
                petId={petProps.petId}
                petName={petProps.petName}
                bond={petProps.bond}
                thinking={Boolean(loadingStage)}
                align="right"
                onSwitchPet={petProps.onSwitchPet}
                onRenamePet={petProps.onRenamePet}
                onPetAction={petProps.onPetAction}
              />
            )}
            <button
              type="button"
              onClick={onClose}
              className={`rounded-full p-1.5 ${
                isSpaceStart
                  ? 'text-slate-300 hover:bg-white/12 hover:text-white'
                  : 'text-slate-500 hover:bg-black/5 dark:hover:bg-white/10'
              }`}
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>

        {/* Segmented Progress Bar */}
        {pool.length > 0 && !loadingStage && quizPhase === 'playing' && (
          <div className="relative z-10 flex gap-1 bg-slate-50/80 px-6 py-2 dark:bg-[#14151A]/80">
            {pool.map((_, idx) => (
              <div
                key={idx}
                className={`h-1.5 flex-1 rounded-full transition-all ${
                  idx < qIdx
                    ? 'bg-emerald-500'
                    : idx === qIdx
                      ? 'bg-blue-600'
                      : 'bg-black/10 dark:bg-white/12'
                }`}
              />
            ))}
          </div>
        )}

        <div className="relative z-10 flex-1 overflow-y-auto p-6">
          {loadingStage ? (
            <div className="space-y-3 py-6">
              <div className="rounded-2xl border border-sky-500/25 bg-sky-50/60 p-4 dark:bg-sky-950/30">
                <div className="text-xs font-bold text-sky-700 dark:text-sky-300">
                  {loadingStage}…
                </div>
                <div className="mt-2.5 h-1.5 w-full overflow-hidden rounded-full bg-sky-500/15">
                  <div className="h-full w-1/3 rounded-full bg-gradient-to-r from-blue-600 to-sky-400 animate-ai-bar" />
                </div>
              </div>
            </div>
          ) : pool.length === 0 ? (
            <div className="py-10 text-center text-sm text-slate-300">
              Add a few definitions or notes (e.g. &ldquo;Osmosis is the movement of water…&rdquo;) to generate an interactive quiz!
            </div>
          ) : quizPhase === 'start' ? (
            <div className="flex flex-col items-center py-6 text-center animate-stagger-in">
              <span className="mb-3 rounded-full border border-sky-400/35 bg-sky-500/20 px-3 py-1 text-[10px] font-bold uppercase tracking-widest text-sky-200">
                Quiz Challenge Lobby
              </span>
              <h3 className="text-2xl font-extrabold tracking-tight text-white">
                Ready to test {notebookTitle}?
              </h3>
              <p className="mt-2 max-w-sm text-xs leading-relaxed text-slate-300">
                {pool.length} adaptive multiple-choice challenges prepared by {engineLabel}. Build streaks and beat your {bestScore}% best score.
              </p>

              <div className="mt-6 grid w-full max-w-sm grid-cols-3 gap-2.5">
                <div className="rounded-2xl border border-white/15 bg-white/10 p-3 backdrop-blur-md">
                  <div className="font-mono-num text-lg font-extrabold text-white">{pool.length}</div>
                  <div className="text-[10px] font-semibold uppercase tracking-wider text-slate-300">Questions</div>
                </div>
                <div className="rounded-2xl border border-white/15 bg-white/10 p-3 backdrop-blur-md">
                  <div className="font-mono-num text-lg font-extrabold text-sky-300">{bestScore}%</div>
                  <div className="text-[10px] font-semibold uppercase tracking-wider text-slate-300">Personal Best</div>
                </div>
                <div className="rounded-2xl border border-white/15 bg-white/10 p-3 backdrop-blur-md">
                  <div className="font-mono-num text-lg font-extrabold text-amber-300">+XP</div>
                  <div className="text-[10px] font-semibold uppercase tracking-wider text-slate-300">Pet Bond</div>
                </div>
              </div>

              <button
                type="button"
                onClick={() => {
                  setElapsedSec(0);
                  setQuizPhase('playing');
                }}
                className="mt-7 inline-flex items-center gap-2 rounded-2xl bg-gradient-to-r from-sky-400 to-blue-500 px-7 py-3 text-sm font-bold text-slate-950 shadow-lg shadow-sky-500/25 transition hover:scale-[1.02] hover:brightness-110"
              >
                Start Quiz Challenge
              </button>
            </div>
          ) : quizPhase === 'finished' ? (
            <div className="flex flex-col items-center py-4 text-center animate-stagger-in">
              <div className="relative flex h-32 w-32 items-center justify-center">
                <svg className="h-full w-full -rotate-90" viewBox="0 0 100 100">
                  <circle cx="50" cy="50" r={radius} stroke="rgba(148,163,184,0.22)" strokeWidth="8" fill="none" />
                  <circle
                    cx="50"
                    cy="50"
                    r={radius}
                    stroke={pct >= 80 ? '#10B981' : '#3D6CF4'}
                    strokeWidth="8"
                    strokeLinecap="round"
                    fill="none"
                    strokeDasharray={circ}
                    strokeDashoffset={circ * (1 - pct / 100)}
                    className="transition-all duration-700"
                  />
                </svg>
                <span className="font-mono-num absolute text-3xl font-extrabold">{pct}%</span>
              </div>
              <div className="mt-3 flex items-center gap-1.5 text-base font-bold">
                <Trophy className="h-5 w-5 text-amber-500" />
                {pct >= 80 ? 'Outstanding mastery!' : pct >= 50 ? 'Solid progress!' : 'Keep reviewing!'}
              </div>
              <div className="mt-3 grid w-full max-w-sm grid-cols-3 gap-2">
                <div className="rounded-2xl bg-slate-50 p-2.5 dark:bg-[#22242C]">
                  <div className="font-mono-num text-sm font-bold">{correctCount}/{pool.length}</div>
                  <div className="text-[10px] text-slate-500">Correct</div>
                </div>
                <div className="rounded-2xl bg-slate-50 p-2.5 dark:bg-[#22242C]">
                  <div className="font-mono-num text-sm font-bold">{elapsedSec}s</div>
                  <div className="text-[10px] text-slate-500">Time</div>
                </div>
                <div className="rounded-2xl bg-slate-50 p-2.5 dark:bg-[#22242C]">
                  <div className="font-mono-num text-sm font-bold">{bestStreak}</div>
                  <div className="text-[10px] text-slate-500">Best Streak</div>
                </div>
              </div>

              {missed.length > 0 && (
                <div className="mt-5 w-full space-y-2 text-left">
                  <div className="text-xs font-bold uppercase tracking-wider text-slate-500">
                    Review Missed ({missed.length})
                  </div>
                  {missed.map((mq, i) => (
                    <div key={i} className="rounded-2xl border border-red-500/20 bg-red-50/40 p-3 text-xs dark:bg-red-950/20">
                      <div className="font-semibold text-slate-800 dark:text-slate-100">{mq.question}</div>
                      <div className="mt-1 font-medium text-emerald-700 dark:text-emerald-300">
                        Answer: {mq.options[mq.answer]}
                      </div>
                    </div>
                  ))}
                  <button
                    type="button"
                    onClick={() => {
                      setPool(missed);
                      setQIdx(0);
                      setPicked(null);
                      setCorrectCount(0);
                      setStreak(0);
                      setMissed([]);
                      setQuizPhase('playing');
                    }}
                    className="mt-2 inline-flex items-center gap-1.5 rounded-xl bg-blue-600 px-4 py-2 text-xs font-semibold text-white hover:bg-blue-700"
                  >
                    <RotateCcw className="h-3.5 w-3.5" /> Retry Missed
                  </button>
                </div>
              )}
            </div>
          ) : (
            <div className="space-y-4">
              <div className="flex items-center justify-between text-xs text-slate-500">
                <span className="font-mono-num font-semibold">
                  Question {qIdx + 1} of {pool.length}
                </span>
                <span className="font-mono-num font-semibold text-emerald-600 dark:text-emerald-400">
                  {correctCount} correct
                </span>
              </div>

              <p className="text-base font-bold leading-snug text-slate-900 dark:text-white">
                {q.question}
              </p>

              <div className="space-y-2.5">
                {q.options.map((opt, idx) => {
                  const isCorrect = idx === q.answer;
                  const isSelected = idx === picked;
                  let cls =
                    'border-black/10 bg-white/80 hover:border-blue-500/50 dark:border-white/10 dark:bg-[#1E212B]/85 dark:hover:border-blue-400/50';
                  if (picked !== null) {
                    if (isCorrect)
                      cls = 'border-emerald-500 bg-emerald-50 text-emerald-900 dark:bg-emerald-950/55 dark:text-emerald-200';
                    else if (isSelected)
                      cls = 'border-red-500 bg-red-50 text-red-900 dark:bg-red-950/55 dark:text-red-200';
                  }
                  return (
                    <button
                      key={idx}
                      type="button"
                      onClick={() => handlePick(idx)}
                      className={`flex w-full items-center gap-3 rounded-2xl border p-3.5 text-left text-sm font-medium backdrop-blur-sm transition ${cls}`}
                    >
                      <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-lg bg-black/5 text-xs font-bold dark:bg-white/10">
                        {String.fromCharCode(65 + idx)}
                      </span>
                      <span className="flex-1">{opt}</span>
                    </button>
                  );
                })}
              </div>

              {picked !== null && (
                <div className="rounded-2xl border border-black/8 bg-slate-50/90 p-3.5 text-xs leading-relaxed text-slate-700 dark:border-white/10 dark:bg-[#22242C]/90 dark:text-slate-200 animate-stagger-in">
                  {q.explanation}
                </div>
              )}
            </div>
          )}
        </div>

        <div
          className={`relative z-10 flex justify-end border-t px-6 py-4 ${
            isSpaceStart
              ? 'border-white/12 bg-black/25 backdrop-blur-md'
              : 'border-black/8 dark:border-white/10'
          }`}
        >
          {quizPhase === 'playing' && pool.length > 0 && picked !== null ? (
            <button
              type="button"
              onClick={handleNext}
              className="rounded-xl bg-blue-600 px-5 py-2 text-xs font-semibold text-white hover:bg-blue-700"
            >
              {qIdx + 1 >= pool.length ? 'Finish Quiz' : 'Next Question'}
            </button>
          ) : (
            <button
              type="button"
              onClick={onClose}
              className={`rounded-xl px-4 py-2 text-xs font-semibold ${
                isSpaceStart
                  ? 'bg-white/10 text-white hover:bg-white/20'
                  : 'bg-black/5 hover:bg-black/10 dark:bg-white/10'
              }`}
            >
              Close
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
