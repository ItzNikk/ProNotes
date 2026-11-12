import React, { useEffect, useState } from 'react';
import {
  Brain,
  CheckCircle2,
  Cpu,
  Download,
  FileDown,
  HardDrive,
  Pause,
  Plus,
  RotateCcw,
  Sparkles,
  Trash2,
  Trophy,
  X,
  Zap,
} from 'lucide-react';
import { Flashcard, LocalModelState, QuizQuestion } from '../types/models';
import { LOCAL_MODELS_CATALOG } from '../utils/localAi';

// ================================================================
// Local AI Model Hub Modal (Download free lightweight models)
// ================================================================

interface LocalAiModalProps {
  open: boolean;
  modelStates: Record<string, LocalModelState>;
  activeModelId: string | null;
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
  onClose,
  onDownload,
  onPause,
  onDelete,
  onActivate,
}) => {
  if (!open) return null;

  const readyModels = LOCAL_MODELS_CATALOG.filter((m) => modelStates[m.id]?.status === 'READY');
  const usedMb = readyModels.reduce((sum, m) => sum + m.sizeMb, 0);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
      <div className="flex max-h-[88vh] w-full max-w-2xl flex-col overflow-hidden rounded-3xl border border-black/10 bg-white shadow-2xl dark:border-white/10 dark:bg-[#191A1F] animate-stagger-in">
        <div className="flex items-center justify-between border-b border-black/8 px-6 py-4 dark:border-white/10">
          <div className="flex items-center gap-3">
            <div className="relative flex h-10 w-10 items-center justify-center rounded-2xl bg-gradient-to-tr from-violet-600 to-blue-500 text-white shadow">
              <Cpu className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold tracking-tight">On-Device Local AI Models</h2>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Free lightweight quantized models stored locally on your device · 100% private & offline
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-full p-2 text-slate-500 hover:bg-black/5 dark:hover:bg-white/10"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="flex items-center justify-between bg-slate-50 px-6 py-3 text-xs dark:bg-[#14151A]">
          <div className="flex items-center gap-2 text-slate-600 dark:text-slate-300">
            <HardDrive className="h-4 w-4 text-blue-600" />
            <span className="font-mono-num font-semibold">{usedMb} MB</span> cached on device (
            {readyModels.length} ready)
          </div>
          <div className="text-slate-500 dark:text-slate-400">
            Active:{' '}
            <span className="font-semibold text-violet-600 dark:text-violet-400">
              {LOCAL_MODELS_CATALOG.find((m) => m.id === activeModelId)?.name ||
                'Built-in Rule Engine'}
            </span>
          </div>
        </div>

        <div className="flex-1 space-y-3 overflow-y-auto p-6">
          {LOCAL_MODELS_CATALOG.map((model) => {
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
                    <div className="mt-2 flex flex-wrap items-center gap-1.5 text-[11px] text-slate-500 dark:text-slate-400">
                      <span className="inline-flex items-center gap-1 font-medium text-emerald-600 dark:text-emerald-400">
                        <Zap className="h-3 w-3" /> {model.speedTag}
                      </span>
                      <span>·</span>
                      <span>{model.ramRequired}</span>
                      <span>·</span>
                      <span>{model.capabilities.join(' · ')}</span>
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
                        Downloading quantized weights to device storage…
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
// Summary & Explain Sheet (Staggered Reveal + Add to Page / Mind Map)
// ================================================================

export const SummaryModal: React.FC<{
  open: boolean;
  title: string;
  points: string[];
  engineLabel: string;
  explainText?: string | null;
  onClose: () => void;
  onAddToPage: (text: string) => void;
  onTurnIntoMindMap: () => void;
}> = ({
  open,
  title,
  points,
  engineLabel,
  explainText,
  onClose,
  onAddToPage,
  onTurnIntoMindMap,
}) => {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
      <div className="flex max-h-[85vh] w-full max-w-xl flex-col overflow-hidden rounded-3xl border border-black/10 bg-white shadow-2xl dark:border-white/10 dark:bg-[#191A1F] animate-stagger-in">
        <div className="flex items-center justify-between bg-gradient-to-r from-violet-600/10 via-blue-600/10 to-transparent px-6 py-4">
          <div className="flex items-center gap-2.5">
            <Sparkles className="h-5 w-5 text-violet-600 dark:text-violet-400" />
            <div>
              <h2 className="text-base font-bold">{title}</h2>
              <p className="text-[11px] font-medium text-violet-700 dark:text-violet-300">
                {engineLabel}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-full p-1.5 text-slate-500 hover:bg-black/5 dark:hover:bg-white/10"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="flex-1 space-y-2.5 overflow-y-auto p-6">
          {explainText ? (
            <div className="rounded-2xl border border-violet-500/20 bg-violet-50/50 p-4 text-sm leading-relaxed text-slate-800 dark:bg-violet-950/20 dark:text-slate-100 animate-stagger-in">
              {explainText}
            </div>
          ) : (
            points.map((pt, idx) => (
              <div
                key={idx}
                style={{ animationDelay: `${idx * 75}ms` }}
                className="flex items-start gap-3 rounded-2xl border border-black/8 bg-slate-50/70 p-3.5 text-sm leading-relaxed text-slate-800 dark:border-white/8 dark:bg-[#20222A] dark:text-slate-100 animate-stagger-in"
              >
                <span className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-violet-600 text-[11px] font-bold text-white">
                  {idx + 1}
                </span>
                <span>{pt}</span>
              </div>
            ))
          )}
        </div>

        <div className="flex flex-wrap items-center justify-end gap-2 border-t border-black/8 px-6 py-4 dark:border-white/10">
          <button
            type="button"
            onClick={onTurnIntoMindMap}
            className="inline-flex items-center gap-1.5 rounded-xl border border-violet-500/30 bg-violet-500/10 px-3.5 py-2 text-xs font-semibold text-violet-700 hover:bg-violet-500/20 dark:text-violet-300"
          >
            <Brain className="h-3.5 w-3.5" /> Build Mind Map
          </button>
          <button
            type="button"
            onClick={() => {
              onAddToPage(explainText || points.map((p) => `• ${p}`).join('\n'));
              onClose();
            }}
            className="inline-flex items-center gap-1.5 rounded-xl bg-blue-600 px-4 py-2 text-xs font-semibold text-white hover:bg-blue-700"
          >
            <Plus className="h-3.5 w-3.5" /> Add to Page
          </button>
        </div>
      </div>
    </div>
  );
};

// ================================================================
// Flashcards Modal (Leitner Spaced Repetition + 3D Flip + CSV)
// ================================================================

export const FlashcardsModal: React.FC<{
  open: boolean;
  notebookTitle: string;
  notebookId: string;
  cards: Flashcard[];
  onClose: () => void;
  onAddCard: (front: string, back: string) => void;
  onUpdateCard: (card: Flashcard) => void;
  onDeleteCard: (id: string) => void;
  onGenerateAi: () => void;
}> = ({
  open,
  notebookTitle,
  cards,
  onClose,
  onAddCard,
  onUpdateCard,
  onDeleteCard,
  onGenerateAi,
}) => {
  const [idx, setIdx] = useState(0);
  const [flipped, setFlipped] = useState(false);
  const [frontInput, setFrontInput] = useState('');
  const [backInput, setBackInput] = useState('');
  const [adding, setAdding] = useState(false);

  useEffect(() => {
    setIdx(0);
    setFlipped(false);
  }, [open, cards.length]);

  if (!open) return null;

  const current = cards[idx];

  const rateCard = (quality: 'again' | 'good' | 'easy') => {
    if (!current) return;
    const nextBox =
      quality === 'again' ? 1 : quality === 'good' ? Math.min(5, current.box + 1) : Math.min(5, current.box + 2);
    const days = nextBox === 1 ? 0 : Math.pow(2, nextBox - 1);
    onUpdateCard({
      ...current,
      box: nextBox,
      due: Date.now() + days * 86400000,
    });
    setFlipped(false);
    setIdx((prev) => (cards.length > 0 ? (prev + 1) % cards.length : 0));
  };

  const exportCsv = () => {
    const csv = ['"Front","Back"']
      .concat(cards.map((c) => `"${c.front.replace(/"/g, '""')}","${c.back.replace(/"/g, '""')}"`))
      .join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${notebookTitle}-flashcards.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
      <div className="flex max-h-[90vh] w-full max-w-2xl flex-col overflow-hidden rounded-3xl border border-black/10 bg-white shadow-2xl dark:border-white/10 dark:bg-[#191A1F] animate-stagger-in">
        <div className="flex items-center justify-between border-b border-black/8 px-6 py-4 dark:border-white/10">
          <div>
            <h2 className="text-lg font-bold">Flashcards · {notebookTitle}</h2>
            <p className="font-mono-num text-xs text-slate-500">
              {cards.length} cards · Spaced repetition
            </p>
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={onGenerateAi}
              className="inline-flex items-center gap-1.5 rounded-xl bg-violet-600/12 px-3 py-1.5 text-xs font-semibold text-violet-700 hover:bg-violet-600/20 dark:text-violet-300"
            >
              <Sparkles className="h-3.5 w-3.5" /> Auto-Generate
            </button>
            {cards.length > 0 && (
              <button
                type="button"
                onClick={exportCsv}
                title="Export CSV"
                className="rounded-xl border border-black/10 p-2 text-slate-600 hover:bg-black/5 dark:border-white/10 dark:text-slate-300"
              >
                <FileDown className="h-4 w-4" />
              </button>
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

        <div className="flex-1 overflow-y-auto p-6">
          {adding ? (
            <div className="space-y-3 rounded-2xl border border-black/10 p-4 dark:border-white/10">
              <h3 className="text-sm font-bold">New Flashcard</h3>
              <input
                value={frontInput}
                onChange={(e) => setFrontInput(e.target.value)}
                placeholder="Question / Front"
                className="w-full rounded-xl border border-black/12 px-3 py-2 text-sm dark:border-white/15 dark:bg-[#22242C]"
              />
              <textarea
                value={backInput}
                onChange={(e) => setBackInput(e.target.value)}
                placeholder="Answer / Back"
                rows={3}
                className="w-full rounded-xl border border-black/12 px-3 py-2 text-sm dark:border-white/15 dark:bg-[#22242C]"
              />
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setAdding(false)}
                  className="rounded-xl px-3 py-1.5 text-xs font-semibold text-slate-500"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={() => {
                    if (frontInput.trim() && backInput.trim()) {
                      onAddCard(frontInput.trim(), backInput.trim());
                      setFrontInput('');
                      setBackInput('');
                      setAdding(false);
                    }
                  }}
                  className="rounded-xl bg-blue-600 px-4 py-1.5 text-xs font-semibold text-white"
                >
                  Save Card
                </button>
              </div>
            </div>
          ) : current ? (
            <div className="space-y-5">
              <div className="flex items-center justify-between text-xs text-slate-500">
                <span className="font-mono-num">
                  Card {idx + 1} of {cards.length} · Box {current.box}
                </span>
                <button
                  type="button"
                  onClick={() => onDeleteCard(current.id)}
                  className="text-red-500 hover:underline"
                >
                  Delete card
                </button>
              </div>

              <div
                onClick={() => setFlipped((f) => !f)}
                className="perspective-1000 cursor-pointer select-none"
              >
                <div
                  className={`preserve-3d relative min-h-[210px] w-full transition-transform duration-500 ${
                    flipped ? 'rotate-y-180' : ''
                  }`}
                >
                  <div className="backface-hidden absolute inset-0 flex flex-col items-center justify-center rounded-3xl border border-black/10 bg-gradient-to-br from-slate-50 to-white p-8 text-center shadow-md dark:border-white/10 dark:from-[#22242E] dark:to-[#1B1D25]">
                    <span className="mb-2 text-[11px] font-bold uppercase tracking-widest text-blue-600 dark:text-blue-400">
                      Question (Tap to flip)
                    </span>
                    <p className="text-lg font-semibold leading-snug text-slate-900 dark:text-white">
                      {current.front}
                    </p>
                  </div>
                  <div className="backface-hidden rotate-y-180 absolute inset-0 flex flex-col items-center justify-center rounded-3xl border border-emerald-500/30 bg-gradient-to-br from-emerald-50 to-white p-8 text-center shadow-md dark:from-emerald-950/40 dark:to-[#1B1D25]">
                    <span className="mb-2 text-[11px] font-bold uppercase tracking-widest text-emerald-600 dark:text-emerald-400">
                      Answer
                    </span>
                    <p className="text-base font-medium leading-relaxed text-slate-800 dark:text-slate-100">
                      {current.back}
                    </p>
                  </div>
                </div>
              </div>

              <div className="flex items-center justify-center gap-3">
                <button
                  type="button"
                  onClick={() => rateCard('again')}
                  className="rounded-xl bg-red-500/12 px-5 py-2.5 text-xs font-bold text-red-600 hover:bg-red-500/20 dark:text-red-400"
                >
                  Again
                </button>
                <button
                  type="button"
                  onClick={() => rateCard('good')}
                  className="rounded-xl bg-blue-500/12 px-5 py-2.5 text-xs font-bold text-blue-600 hover:bg-blue-500/20 dark:text-blue-400"
                >
                  Good
                </button>
                <button
                  type="button"
                  onClick={() => rateCard('easy')}
                  className="rounded-xl bg-emerald-500/12 px-5 py-2.5 text-xs font-bold text-emerald-600 hover:bg-emerald-500/20 dark:text-emerald-400"
                >
                  Easy
                </button>
              </div>
            </div>
          ) : (
            <div className="py-10 text-center">
              <p className="text-sm text-slate-500">
                No flashcards yet for this notebook. Generate them from your notes or create one manually.
              </p>
            </div>
          )}
        </div>

        <div className="flex items-center justify-between border-t border-black/8 px-6 py-3.5 dark:border-white/10">
          <button
            type="button"
            onClick={() => setAdding(true)}
            className="inline-flex items-center gap-1.5 rounded-xl bg-black/5 px-3.5 py-2 text-xs font-semibold hover:bg-black/10 dark:bg-white/10"
          >
            <Plus className="h-3.5 w-3.5" /> Add Card
          </button>
          <button
            type="button"
            onClick={onClose}
            className="rounded-xl bg-blue-600 px-4 py-2 text-xs font-semibold text-white"
          >
            Done
          </button>
        </div>
      </div>
    </div>
  );
};

// ================================================================
// Quiz Studio Modal (Timer, Streak, Animated Score Ring, Retry Missed)
// ================================================================

export const QuizModal: React.FC<{
  open: boolean;
  notebookTitle: string;
  questions: QuizQuestion[];
  bestScore?: number;
  engineLabel: string;
  onClose: () => void;
  onScoreRecorded: (pct: number) => void;
}> = ({ open, notebookTitle, questions, bestScore = 0, engineLabel, onClose, onScoreRecorded }) => {
  const [pool, setPool] = useState<QuizQuestion[]>(questions);
  const [qIdx, setQIdx] = useState(0);
  const [picked, setPicked] = useState<number | null>(null);
  const [correctCount, setCorrectCount] = useState(0);
  const [streak, setStreak] = useState(0);
  const [missed, setMissed] = useState<QuizQuestion[]>([]);
  const [elapsedSec, setElapsedSec] = useState(0);
  const [finished, setFinished] = useState(false);

  useEffect(() => {
    if (open) {
      setPool(questions);
      setQIdx(0);
      setPicked(null);
      setCorrectCount(0);
      setStreak(0);
      setMissed([]);
      setElapsedSec(0);
      setFinished(false);
    }
  }, [open, questions]);

  useEffect(() => {
    if (!open || finished || pool.length === 0) return;
    const t = setInterval(() => setElapsedSec((s) => s + 1), 1000);
    return () => clearInterval(t);
  }, [open, finished, pool.length]);

  if (!open) return null;

  const q = pool[qIdx];

  const handlePick = (optIdx: number) => {
    if (picked !== null || !q) return;
    setPicked(optIdx);
    if (optIdx === q.answer) {
      setCorrectCount((c) => c + 1);
      setStreak((s) => s + 1);
    } else {
      setStreak(0);
      setMissed((m) => [...m, q]);
    }
  };

  const handleNext = () => {
    if (qIdx + 1 >= pool.length) {
      setFinished(true);
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
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
      <div className="flex max-h-[90vh] w-full max-w-xl flex-col overflow-hidden rounded-3xl border border-black/10 bg-white shadow-2xl dark:border-white/10 dark:bg-[#191A1F] animate-stagger-in">
        <div className="flex items-center justify-between border-b border-black/8 px-6 py-4 dark:border-white/10">
          <div>
            <h2 className="text-base font-bold">Study Quiz · {notebookTitle}</h2>
            <p className="text-[11px] text-slate-500">
              {engineLabel} · Best: <span className="font-mono-num font-semibold">{bestScore}%</span>
            </p>
          </div>
          <div className="flex items-center gap-3">
            <span className="font-mono-num rounded-lg bg-black/5 px-2.5 py-1 text-xs font-semibold dark:bg-white/10">
              {Math.floor(elapsedSec / 60)}:{String(elapsedSec % 60).padStart(2, '0')}
            </span>
            <button
              type="button"
              onClick={onClose}
              className="rounded-full p-1.5 text-slate-500 hover:bg-black/5 dark:hover:bg-white/10"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto p-6">
          {pool.length === 0 ? (
            <div className="py-10 text-center text-sm text-slate-500">
              Add a few definitions or notes (e.g. &ldquo;Osmosis is the movement of water…&rdquo;) to generate a quiz!
            </div>
          ) : finished ? (
            <div className="flex flex-col items-center py-4 text-center animate-stagger-in">
              <div className="relative flex h-28 w-28 items-center justify-center">
                <svg className="h-full w-full -rotate-90" viewBox="0 0 100 100">
                  <circle cx="50" cy="50" r={radius} stroke="rgba(148,163,184,0.22)" strokeWidth="8" fill="none" />
                  <circle
                    cx="50"
                    cy="50"
                    r={radius}
                    stroke="#3D6CF4"
                    strokeWidth="8"
                    strokeLinecap="round"
                    fill="none"
                    strokeDasharray={circ}
                    strokeDashoffset={circ * (1 - pct / 100)}
                    className="transition-all duration-700"
                  />
                </svg>
                <span className="font-mono-num absolute text-2xl font-extrabold">{pct}%</span>
              </div>
              <div className="mt-3 flex items-center gap-1.5 text-base font-bold">
                <Trophy className="h-5 w-5 text-amber-500" />
                {pct >= 80 ? 'Outstanding mastery!' : pct >= 50 ? 'Solid progress!' : 'Keep reviewing!'}
              </div>
              <p className="mt-1 text-xs text-slate-500">
                {correctCount} of {pool.length} correct in {elapsedSec}s
              </p>

              {missed.length > 0 && (
                <div className="mt-5 w-full space-y-2 text-left">
                  <div className="text-xs font-bold uppercase tracking-wider text-slate-500">
                    Missed Questions ({missed.length})
                  </div>
                  {missed.map((mq, i) => (
                    <div key={i} className="rounded-xl border border-red-500/20 bg-red-50/40 p-3 text-xs dark:bg-red-950/20">
                      <div className="font-semibold text-slate-800 dark:text-slate-100">{mq.question}</div>
                      <div className="mt-1 text-emerald-700 dark:text-emerald-300">
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
                      setMissed([]);
                      setFinished(false);
                    }}
                    className="mt-2 inline-flex items-center gap-1.5 rounded-xl bg-blue-600 px-4 py-2 text-xs font-semibold text-white"
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
                  Question {qIdx + 1} / {pool.length}
                </span>
                {streak > 1 && (
                  <span className="font-semibold text-amber-600 dark:text-amber-400">
                    🔥 {streak} streak
                  </span>
                )}
              </div>

              <p className="text-base font-bold leading-snug text-slate-900 dark:text-white">
                {q.question}
              </p>

              <div className="space-y-2.5">
                {q.options.map((opt, idx) => {
                  const isCorrect = idx === q.answer;
                  const isSelected = idx === picked;
                  let cls =
                    'border-black/10 hover:border-blue-500/50 dark:border-white/10 dark:hover:border-blue-400/50';
                  if (picked !== null) {
                    if (isCorrect)
                      cls = 'border-emerald-500 bg-emerald-50 text-emerald-900 dark:bg-emerald-950/35 dark:text-emerald-200';
                    else if (isSelected)
                      cls = 'border-red-500 bg-red-50 text-red-900 dark:bg-red-950/35 dark:text-red-200';
                  }
                  return (
                    <button
                      key={idx}
                      type="button"
                      onClick={() => handlePick(idx)}
                      className={`flex w-full items-center gap-3 rounded-2xl border p-3.5 text-left text-sm font-medium transition ${cls}`}
                    >
                      <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-black/5 text-xs font-bold dark:bg-white/10">
                        {String.fromCharCode(65 + idx)}
                      </span>
                      <span>{opt}</span>
                    </button>
                  );
                })}
              </div>

              {picked !== null && (
                <div className="rounded-2xl bg-slate-50 p-3.5 text-xs text-slate-600 dark:bg-[#22242C] dark:text-slate-300 animate-stagger-in">
                  {q.explanation}
                </div>
              )}
            </div>
          )}
        </div>

        <div className="flex justify-end border-t border-black/8 px-6 py-4 dark:border-white/10">
          {!finished && pool.length > 0 && picked !== null ? (
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
              className="rounded-xl bg-black/5 px-4 py-2 text-xs font-semibold hover:bg-black/10 dark:bg-white/10"
            >
              Close
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
