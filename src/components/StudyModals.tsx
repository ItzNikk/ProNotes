import React, { useEffect, useMemo, useState } from 'react';
import {
  ArrowDown,
  ArrowUp,
  BookOpen,
  Check,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Copy,
  Edit3,
  Eye,
  EyeOff,
  FileDown,
  Flame,
  Layers,
  Network,
  Pause,
  Play,
  Plus,
  RotateCcw,
  Search,
  Shuffle,
  Trash2,
  Trophy,
  X,
} from 'lucide-react';
import {
  Flashcard,
  QuizQuestion,
  SLIDE_THEMES,
  SlideCard,
  SlideKind,
  SlideThemeId,
} from '../types/models';
import { RichSummaryResult } from '../utils/localAi';
import {
  LiquidButton,
  LiquidGlassSurface,
  LiquidInput,
  LiquidMenuButton,
  LiquidModal,
  LiquidSegmentedControl,
  LiquidSlider,
} from './LiquidGlassSurface';

// ================================================================
// Study Digest & Outline Summary Modal
// ================================================================

export const SummaryModal: React.FC<{
  open: boolean;
  title: string;
  summary: RichSummaryResult | null;
  loadingStage?: string | null;
  explainText?: string | null;
  onClose: () => void;
  onAddToPage: (text: string) => void;
  onTurnIntoMindMap: () => void;
}> = ({
  open,
  title,
  summary,
  loadingStage,
  explainText,
  onClose,
  onAddToPage,
  onTurnIntoMindMap,
}) => {
  const [copied, setCopied] = useState(false);

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
    <LiquidModal open={open} onClose={onClose} maxWidthClass="max-w-2xl">
      <div className="flex max-h-[88vh] flex-col">
        <div className="flex items-center justify-between border-b border-[#1F2421]/10 px-6 py-4 dark:border-[#EDE8DB]/10">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-[#24483F] text-[#F5F2E8] shadow-sm dark:bg-[#8CC1AC] dark:text-[#151E1B]">
              <BookOpen className="h-4.5 w-4.5" />
            </div>
            <div>
              <h2 className="text-base font-bold">{title}</h2>
              <p className="text-[11px] font-medium text-[#5A5F58] dark:text-[#B1AC9F]">
                {loadingStage || 'Structured Page Digest'}
              </p>
            </div>
          </div>
          <LiquidButton onClick={onClose} className="!h-9 !w-9 !p-0">
            <X className="h-4 w-4" />
          </LiquidButton>
        </div>

        <div className="flex-1 space-y-4 overflow-y-auto p-6">
          {loadingStage ? (
            <LiquidGlassSurface variant="frost" radius={20} className="p-4">
              <div className="text-xs font-bold text-[#24483F] dark:text-[#8CC1AC]">
                {loadingStage}…
              </div>
              <div className="mt-2.5 h-1.5 w-full overflow-hidden rounded-full bg-[#24483F]/15">
                <div className="h-full w-1/3 rounded-full bg-[#24483F] dark:bg-[#8CC1AC] animate-ai-bar" />
              </div>
            </LiquidGlassSurface>
          ) : explainText ? (
            <LiquidGlassSurface variant="frost" radius={20} className="p-5 text-sm leading-relaxed">
              {explainText}
            </LiquidGlassSurface>
          ) : summary ? (
            <>
              <LiquidGlassSurface variant="frost" radius={20} className="p-4">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="text-[11px] font-bold uppercase tracking-wider text-[#24483F] dark:text-[#8CC1AC]">
                    Study Digest
                  </span>
                  <span className="font-mono-num rounded-md bg-black/6 px-2 py-0.5 text-[11px] font-semibold text-[#5A5F58] dark:bg-white/10 dark:text-[#B1AC9F]">
                    {summary.wordCount} words scanned
                  </span>
                </div>
                <p className="mt-1.5 text-sm font-bold">{summary.headline}</p>
                {summary.keywords.length > 0 && (
                  <div className="mt-2.5 flex flex-wrap gap-1.5">
                    {summary.keywords.map((kw) => (
                      <span
                        key={kw}
                        className="rounded-lg bg-black/6 px-2 py-0.5 text-[11px] font-semibold dark:bg-white/10"
                      >
                        #{kw}
                      </span>
                    ))}
                  </div>
                )}
              </LiquidGlassSurface>

              <div className="space-y-2">
                <div className="text-[11px] font-bold uppercase tracking-wider text-[#5A5F58] dark:text-[#B1AC9F]">
                  Key Takeaways
                </div>
                {summary.points.map((pt, idx) => (
                  <LiquidGlassSurface key={idx} variant="frost" radius={18} className="p-3.5">
                    <div className="flex items-start gap-3 text-sm leading-relaxed">
                      <span className="font-mono-num mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-lg bg-[#24483F] text-[11px] font-bold text-[#F5F2E8] dark:bg-[#8CC1AC] dark:text-[#151E1B]">
                        {idx + 1}
                      </span>
                      <span>{pt}</span>
                    </div>
                  </LiquidGlassSurface>
                ))}
              </div>

              {summary.definitions.length > 0 && (
                <div className="space-y-2">
                  <div className="text-[11px] font-bold uppercase tracking-wider text-[#5A5F58] dark:text-[#B1AC9F]">
                    Key Concepts ({summary.definitions.length})
                  </div>
                  <div className="grid gap-2.5 sm:grid-cols-2">
                    {summary.definitions.map((d, i) => (
                      <LiquidGlassSurface key={i} variant="frost" radius={18} className="p-3">
                        <div className="text-xs font-bold text-[#24483F] dark:text-[#8CC1AC]">{d.term}</div>
                        <div className="mt-1 text-xs leading-relaxed text-[#5A5F58] dark:text-[#B1AC9F]">
                          {d.meaning}
                        </div>
                      </LiquidGlassSurface>
                    ))}
                  </div>
                </div>
              )}

              {summary.formulas.length > 0 && (
                <div className="space-y-2">
                  <div className="text-[11px] font-bold uppercase tracking-wider text-[#5A5F58] dark:text-[#B1AC9F]">
                    Extracted Equations
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {summary.formulas.map((f, i) => (
                      <LiquidGlassSurface
                        key={i}
                        variant="frost"
                        radius={14}
                        className="font-mono-num px-3 py-1.5 text-xs font-semibold"
                      >
                        {f}
                      </LiquidGlassSurface>
                    ))}
                  </div>
                </div>
              )}
            </>
          ) : null}
        </div>

        <div className="flex flex-wrap items-center justify-between gap-2 border-t border-[#1F2421]/10 px-6 py-4 dark:border-[#EDE8DB]/10">
          <LiquidButton
            disabled={!fullCopyText}
            onClick={() => {
              navigator.clipboard?.writeText(fullCopyText);
              setCopied(true);
              window.setTimeout(() => setCopied(false), 1600);
            }}
            className="!px-3.5 !py-2 !text-xs"
          >
            {copied ? <Check className="h-3.5 w-3.5 text-emerald-600" /> : <Copy className="h-3.5 w-3.5" />}
            {copied ? 'Copied' : 'Copy All'}
          </LiquidButton>

          <div className="flex flex-wrap items-center gap-2">
            <LiquidButton
              disabled={!!loadingStage}
              onClick={onTurnIntoMindMap}
              className="!px-3.5 !py-2 !text-xs"
            >
              <Network className="h-3.5 w-3.5" /> Build Mind Map
            </LiquidButton>
            <LiquidButton
              tint="#24483F"
              disabled={!!loadingStage || !fullCopyText}
              onClick={() => {
                onAddToPage(fullCopyText);
                onClose();
              }}
              className="!px-4 !py-2 !text-xs"
            >
              <Plus className="h-3.5 w-3.5" /> Add Sticky to Page
            </LiquidButton>
          </div>
        </div>
      </div>
    </LiquidModal>
  );
};

// ================================================================
// Flashcards Studio (SM-2 Spaced Repetition + Deck Manager)
// ================================================================

export const FlashcardsModal: React.FC<{
  open: boolean;
  notebookTitle: string;
  notebookId: string;
  cards: Flashcard[];
  loadingStage?: string | null;
  onClose: () => void;
  onAddCard: (front: string, back: string) => void;
  onUpdateCard: (card: Flashcard) => void;
  onDeleteCard: (id: string) => void;
  onGenerateAi: () => void;
}> = ({
  open,
  notebookTitle,
  cards,
  loadingStage,
  onClose,
  onAddCard,
  onUpdateCard,
  onDeleteCard,
  onGenerateAi,
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
    <LiquidModal open={open} onClose={onClose} maxWidthClass="max-w-2xl">
      <div className="flex max-h-[90vh] flex-col">
        <div className="flex flex-wrap items-center justify-between gap-2 border-b border-[#1F2421]/10 px-6 py-4 dark:border-[#EDE8DB]/10">
          <div>
            <h2 className="text-lg font-bold tracking-tight">Flashcards · {notebookTitle}</h2>
            <div className="mt-1 flex flex-wrap items-center gap-2 text-[11px] font-semibold">
              <span className="font-mono-num rounded-md bg-[#24483F]/12 px-2 py-0.5 text-[#24483F] dark:bg-[#8CC1AC]/18 dark:text-[#8CC1AC]">
                {stats.due} due
              </span>
              <span className="font-mono-num rounded-md bg-amber-500/15 px-2 py-0.5 text-amber-700 dark:text-amber-300">
                {stats.learning} learning
              </span>
              <span className="font-mono-num rounded-md bg-emerald-500/15 px-2 py-0.5 text-emerald-700 dark:text-emerald-300">
                {stats.mastered} mastered
              </span>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <LiquidButton
              tint="#24483F"
              onClick={onGenerateAi}
              disabled={!!loadingStage}
              className="!px-3.5 !py-1.5 !text-xs"
            >
              <Plus className="h-3.5 w-3.5" />
              {loadingStage || 'Extract from Notes'}
            </LiquidButton>
            {cards.length > 0 && (
              <LiquidButton onClick={exportCsv} title="Export CSV" className="!h-9 !w-9 !p-0">
                <FileDown className="h-4 w-4" />
              </LiquidButton>
            )}
            <LiquidButton onClick={onClose} className="!h-9 !w-9 !p-0">
              <X className="h-4 w-4" />
            </LiquidButton>
          </div>
        </div>

        <div className="flex flex-wrap items-center justify-between gap-2 border-b border-[#1F2421]/8 px-6 py-2.5 dark:border-[#EDE8DB]/8">
          <LiquidSegmentedControl
            value={tab}
            onChange={setTab}
            options={[
              { value: 'study', label: 'Study Deck' },
              { value: 'manage', label: `Manage (${cards.length})` },
            ]}
          />
          {tab === 'study' && deck.length > 1 && (
            <LiquidButton
              onClick={() => {
                const shuffled = [...deck].sort(() => Math.random() - 0.5).map((c) => c.id);
                setOrderIds(shuffled);
                setIdx(0);
                setFlipped(false);
              }}
              className="!px-3 !py-1.5 !text-xs"
            >
              <Shuffle className="h-3.5 w-3.5" /> Shuffle
            </LiquidButton>
          )}
        </div>

        <div className="flex-1 overflow-y-auto p-6">
          {tab === 'study' ? (
            current ? (
              <div className="space-y-5">
                <div className="flex items-center justify-between text-xs text-[#5A5F58] dark:text-[#B1AC9F]">
                  <div className="flex items-center gap-2">
                    <LiquidButton
                      onClick={() => {
                        setFlipped(false);
                        setIdx((i) => (i - 1 + deck.length) % deck.length);
                      }}
                      className="!h-8 !w-8 !p-0"
                    >
                      <ChevronLeft className="h-4 w-4" />
                    </LiquidButton>
                    <span className="font-mono-num font-semibold text-[#1F2421] dark:text-[#EDE8DB]">
                      Card {safeIdx + 1} / {deck.length} · Box {current.box}
                    </span>
                    <LiquidButton
                      onClick={() => {
                        setFlipped(false);
                        setIdx((i) => (i + 1) % deck.length);
                      }}
                      className="!h-8 !w-8 !p-0"
                    >
                      <ChevronRight className="h-4 w-4" />
                    </LiquidButton>
                  </div>
                  <span className="hidden text-[11px] sm:inline">Space to flip · 1–4 to rate</span>
                </div>

                <div className="h-1.5 w-full overflow-hidden rounded-full bg-[#1F2421]/10 dark:bg-[#EDE8DB]/10">
                  <div
                    className="h-full rounded-full bg-[#24483F] transition-all duration-300 dark:bg-[#8CC1AC]"
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
                    <LiquidGlassSurface
                      variant="frost"
                      radius={24}
                      className="backface-hidden absolute inset-0 flex flex-col items-center justify-center p-8 text-center"
                    >
                      <span className="mb-3 rounded-lg bg-[#24483F]/12 px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-widest text-[#24483F] dark:bg-[#8CC1AC]/18 dark:text-[#8CC1AC]">
                        Prompt · Tap to Reveal
                      </span>
                      <p className="text-lg font-semibold leading-snug">{current.front}</p>
                    </LiquidGlassSurface>

                    <LiquidGlassSurface
                      variant="frost"
                      radius={24}
                      className="backface-hidden rotate-y-180 absolute inset-0 flex flex-col items-center justify-center p-8 text-center"
                    >
                      <span className="mb-3 rounded-lg bg-emerald-500/15 px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-widest text-emerald-700 dark:text-emerald-300">
                        Answer
                      </span>
                      <p className="text-base font-medium leading-relaxed">{current.back}</p>
                    </LiquidGlassSurface>
                  </div>
                </div>

                <div className="grid grid-cols-4 gap-2">
                  {(
                    [
                      { grade: 0 as const, label: 'Again', sub: 'Now', tone: 'text-[#9B3B3B] dark:text-[#E58A8A]' },
                      { grade: 1 as const, label: 'Hard', sub: '12h', tone: 'text-amber-700 dark:text-amber-300' },
                      { grade: 2 as const, label: 'Good', sub: `${Math.pow(2, Math.min(4, current.box))}d`, tone: 'text-[#24483F] dark:text-[#8CC1AC]' },
                      { grade: 3 as const, label: 'Easy', sub: `${Math.pow(2, Math.min(5, current.box + 1))}d`, tone: 'text-emerald-700 dark:text-emerald-300' },
                    ]
                  ).map((g) => (
                    <LiquidButton
                      key={g.grade}
                      onClick={() => rateCard(g.grade)}
                      className={`!flex-col !gap-0.5 !rounded-2xl !py-2.5 !text-xs ${g.tone}`}
                    >
                      <span>{g.label}</span>
                      <span className="font-mono-num text-[10px] font-medium opacity-75">{g.sub}</span>
                    </LiquidButton>
                  ))}
                </div>
              </div>
            ) : (
              <div className="py-12 text-center text-sm text-[#5A5F58] dark:text-[#B1AC9F]">
                No flashcards yet. Extract terms from notes above or switch to Manage to add your own.
              </div>
            )
          ) : (
            <div className="space-y-4">
              <LiquidGlassSurface variant="frost" radius={20} className="space-y-2.5 p-4">
                <div className="text-[11px] font-bold uppercase tracking-wider text-[#24483F] dark:text-[#8CC1AC]">
                  {editingId ? 'Edit Flashcard' : 'Create New Flashcard'}
                </div>
                <LiquidInput
                  value={frontInput}
                  onChange={(e) => setFrontInput(e.target.value)}
                  placeholder="Front / Question prompt"
                />
                <textarea
                  value={backInput}
                  onChange={(e) => setBackInput(e.target.value)}
                  placeholder="Back / Answer or definition"
                  rows={2}
                  className="liquid-glass-frost w-full rounded-2xl px-3.5 py-2 text-xs outline-none placeholder:text-[#5A5F58] dark:placeholder:text-[#B1AC9F]"
                />
                <div className="flex justify-end gap-2">
                  {editingId && (
                    <LiquidButton
                      onClick={() => {
                        setEditingId(null);
                        setFrontInput('');
                        setBackInput('');
                      }}
                      className="!px-3 !py-1.5 !text-xs"
                    >
                      Cancel Edit
                    </LiquidButton>
                  )}
                  <LiquidButton
                    tint="#24483F"
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
                    className="!px-4 !py-1.5 !text-xs"
                  >
                    <Plus className="h-3.5 w-3.5" />
                    {editingId ? 'Update Card' : 'Add Card'}
                  </LiquidButton>
                </div>
              </LiquidGlassSurface>

              {cards.length > 0 && (
                <LiquidInput
                  icon={<Search className="h-3.5 w-3.5" />}
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  placeholder="Filter cards..."
                />
              )}

              <div className="space-y-2">
                {filteredManage.map((c) => (
                  <LiquidGlassSurface key={c.id} variant="frost" radius={18} className="p-3.5">
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-2">
                          <span className="font-mono-num rounded bg-[#24483F]/12 px-1.5 py-0.5 text-[10px] font-bold text-[#24483F] dark:bg-[#8CC1AC]/18 dark:text-[#8CC1AC]">
                            Box {c.box}
                          </span>
                          <span className="truncate text-sm font-semibold">{c.front}</span>
                        </div>
                        <p className="mt-1 text-xs text-[#5A5F58] dark:text-[#B1AC9F]">{c.back}</p>
                      </div>
                      <div className="flex shrink-0 items-center gap-1">
                        <LiquidButton
                          onClick={() => {
                            setEditingId(c.id);
                            setFrontInput(c.front);
                            setBackInput(c.back);
                          }}
                          className="!h-8 !w-8 !p-0"
                        >
                          <Edit3 className="h-3.5 w-3.5" />
                        </LiquidButton>
                        <LiquidButton
                          onClick={() => onDeleteCard(c.id)}
                          className="!h-8 !w-8 !p-0 text-[#9B3B3B] dark:text-[#E58A8A]"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </LiquidButton>
                      </div>
                    </div>
                  </LiquidGlassSurface>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </LiquidModal>
  );
};

// ================================================================
// Study Quiz Studio Modal
// ================================================================

export const QuizModal: React.FC<{
  open: boolean;
  notebookTitle: string;
  questions: QuizQuestion[];
  bestScore?: number;
  engineLabel: string;
  loadingStage?: string | null;
  onClose: () => void;
  onScoreRecorded: (pct: number) => void;
}> = ({
  open,
  notebookTitle,
  questions,
  bestScore = 0,
  loadingStage,
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

  const q = pool[qIdx];

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
    <LiquidModal open={open} onClose={onClose} maxWidthClass="max-w-xl">
      <div className="flex max-h-[90vh] flex-col">
        <div className="flex items-center justify-between border-b border-[#1F2421]/10 px-6 py-4 dark:border-[#EDE8DB]/10">
          <div>
            <h2 className="text-base font-bold">Quiz Studio · {notebookTitle}</h2>
            <p className="text-[11px] text-[#5A5F58] dark:text-[#B1AC9F]">
              {loadingStage || 'Active Recall Practice'} · Best:{' '}
              <span className="font-mono-num font-semibold text-[#24483F] dark:text-[#8CC1AC]">
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
              <span className="font-mono-num rounded-lg bg-black/6 px-2.5 py-1 text-xs font-semibold dark:bg-white/10">
                {Math.floor(elapsedSec / 60)}:{String(elapsedSec % 60).padStart(2, '0')}
              </span>
            )}
            <LiquidButton onClick={onClose} className="!h-9 !w-9 !p-0">
              <X className="h-4 w-4" />
            </LiquidButton>
          </div>
        </div>

        {pool.length > 0 && !loadingStage && quizPhase === 'playing' && (
          <div className="flex gap-1 px-6 pt-3">
            {pool.map((_, idx) => (
              <div
                key={idx}
                className={`h-1.5 flex-1 rounded-full transition-all ${
                  idx < qIdx
                    ? 'bg-emerald-500'
                    : idx === qIdx
                      ? 'bg-[#24483F] dark:bg-[#8CC1AC]'
                      : 'bg-black/10 dark:bg-white/12'
                }`}
              />
            ))}
          </div>
        )}

        <div className="flex-1 overflow-y-auto p-6">
          {loadingStage ? (
            <LiquidGlassSurface variant="frost" radius={20} className="p-4">
              <div className="text-xs font-bold text-[#24483F] dark:text-[#8CC1AC]">
                {loadingStage}…
              </div>
              <div className="mt-2.5 h-1.5 w-full overflow-hidden rounded-full bg-[#24483F]/15">
                <div className="h-full w-1/3 rounded-full bg-[#24483F] dark:bg-[#8CC1AC] animate-ai-bar" />
              </div>
            </LiquidGlassSurface>
          ) : pool.length === 0 ? (
            <div className="py-10 text-center text-sm text-[#5A5F58] dark:text-[#B1AC9F]">
              Add a few definitions or notes (e.g. &ldquo;Osmosis is the movement of water…&rdquo;) to build an interactive quiz!
            </div>
          ) : quizPhase === 'start' ? (
            <div className="flex flex-col items-center py-6 text-center">
              <span className="mb-3 rounded-full bg-[#24483F]/12 px-3 py-1 text-[10px] font-bold uppercase tracking-widest text-[#24483F] dark:bg-[#8CC1AC]/18 dark:text-[#8CC1AC]">
                Quiz Challenge Lobby
              </span>
              <h3 className="text-2xl font-extrabold tracking-tight">Ready to test {notebookTitle}?</h3>
              <p className="mt-2 max-w-sm text-xs leading-relaxed text-[#5A5F58] dark:text-[#B1AC9F]">
                {pool.length} multiple-choice challenges extracted from your study notes. Beat your {bestScore}% best score.
              </p>

              <div className="mt-6 grid w-full max-w-sm grid-cols-3 gap-2.5">
                <LiquidGlassSurface variant="frost" radius={18} className="p-3">
                  <div className="font-mono-num text-lg font-extrabold">{pool.length}</div>
                  <div className="text-[10px] font-semibold uppercase tracking-wider text-[#5A5F58] dark:text-[#B1AC9F]">Questions</div>
                </LiquidGlassSurface>
                <LiquidGlassSurface variant="frost" radius={18} className="p-3">
                  <div className="font-mono-num text-lg font-extrabold text-[#24483F] dark:text-[#8CC1AC]">{bestScore}%</div>
                  <div className="text-[10px] font-semibold uppercase tracking-wider text-[#5A5F58] dark:text-[#B1AC9F]">Personal Best</div>
                </LiquidGlassSurface>
                <LiquidGlassSurface variant="frost" radius={18} className="p-3">
                  <div className="font-mono-num text-lg font-extrabold text-amber-600 dark:text-amber-300">SM-2</div>
                  <div className="text-[10px] font-semibold uppercase tracking-wider text-[#5A5F58] dark:text-[#B1AC9F]">Active Recall</div>
                </LiquidGlassSurface>
              </div>

              <LiquidButton
                tint="#24483F"
                onClick={() => {
                  setElapsedSec(0);
                  setQuizPhase('playing');
                }}
                className="mt-7 !px-7 !py-3 !text-sm"
              >
                Start Quiz Challenge
              </LiquidButton>
            </div>
          ) : quizPhase === 'finished' ? (
            <div className="flex flex-col items-center py-4 text-center">
              <div className="relative flex h-32 w-32 items-center justify-center">
                <svg className="h-full w-full -rotate-90" viewBox="0 0 100 100">
                  <circle cx="50" cy="50" r={radius} stroke="rgba(148,163,184,0.22)" strokeWidth="8" fill="none" />
                  <circle
                    cx="50"
                    cy="50"
                    r={radius}
                    stroke={pct >= 80 ? '#10B981' : '#24483F'}
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
                <LiquidGlassSurface variant="frost" radius={16} className="p-2.5">
                  <div className="font-mono-num text-sm font-bold">{correctCount}/{pool.length}</div>
                  <div className="text-[10px] text-[#5A5F58] dark:text-[#B1AC9F]">Correct</div>
                </LiquidGlassSurface>
                <LiquidGlassSurface variant="frost" radius={16} className="p-2.5">
                  <div className="font-mono-num text-sm font-bold">{elapsedSec}s</div>
                  <div className="text-[10px] text-[#5A5F58] dark:text-[#B1AC9F]">Time</div>
                </LiquidGlassSurface>
                <LiquidGlassSurface variant="frost" radius={16} className="p-2.5">
                  <div className="font-mono-num text-sm font-bold">{bestStreak}</div>
                  <div className="text-[10px] text-[#5A5F58] dark:text-[#B1AC9F]">Best Streak</div>
                </LiquidGlassSurface>
              </div>

              {missed.length > 0 && (
                <div className="mt-5 w-full space-y-2 text-left">
                  <div className="text-xs font-bold uppercase tracking-wider text-[#5A5F58] dark:text-[#B1AC9F]">
                    Review Missed ({missed.length})
                  </div>
                  {missed.map((mq, i) => (
                    <LiquidGlassSurface key={i} variant="frost" radius={16} className="p-3 text-xs">
                      <div className="font-semibold">{mq.question}</div>
                      <div className="mt-1 font-medium text-emerald-700 dark:text-emerald-300">
                        Answer: {mq.options[mq.answer]}
                      </div>
                    </LiquidGlassSurface>
                  ))}
                  <LiquidButton
                    tint="#24483F"
                    onClick={() => {
                      setPool(missed);
                      setQIdx(0);
                      setPicked(null);
                      setCorrectCount(0);
                      setStreak(0);
                      setMissed([]);
                      setQuizPhase('playing');
                    }}
                    className="mt-2 !px-4 !py-2 !text-xs"
                  >
                    <RotateCcw className="h-3.5 w-3.5" /> Retry Missed
                  </LiquidButton>
                </div>
              )}
            </div>
          ) : (
            <div className="space-y-4">
              <div className="flex items-center justify-between text-xs text-[#5A5F58] dark:text-[#B1AC9F]">
                <span className="font-mono-num font-semibold">
                  Question {qIdx + 1} of {pool.length}
                </span>
                <span className="font-mono-num font-semibold text-emerald-700 dark:text-emerald-300">
                  {correctCount} correct
                </span>
              </div>

              <p className="text-base font-bold leading-snug">{q.question}</p>

              <div className="space-y-2.5">
                {q.options.map((opt, idx) => {
                  const isCorrect = idx === q.answer;
                  const isSelected = idx === picked;
                  let extra = '';
                  if (picked !== null) {
                    if (isCorrect) extra = 'ring-2 ring-emerald-500 bg-emerald-500/15';
                    else if (isSelected) extra = 'ring-2 ring-[#9B3B3B] bg-[#9B3B3B]/15';
                  }
                  return (
                    <button
                      key={idx}
                      type="button"
                      onClick={() => handlePick(idx)}
                      className={`liquid-glass-frost flex w-full items-center gap-3 rounded-2xl p-3.5 text-left text-sm font-medium transition ${extra}`}
                    >
                      <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-lg bg-black/8 text-xs font-bold dark:bg-white/12">
                        {String.fromCharCode(65 + idx)}
                      </span>
                      <span className="flex-1">{opt}</span>
                    </button>
                  );
                })}
              </div>

              {picked !== null && (
                <LiquidGlassSurface variant="frost" radius={16} className="p-3.5 text-xs leading-relaxed">
                  {q.explanation}
                </LiquidGlassSurface>
              )}
            </div>
          )}
        </div>

        <div className="flex justify-end border-t border-[#1F2421]/10 px-6 py-4 dark:border-[#EDE8DB]/10">
          {quizPhase === 'playing' && pool.length > 0 && picked !== null ? (
            <LiquidButton tint="#24483F" onClick={handleNext} className="!px-5 !py-2 !text-xs">
              {qIdx + 1 >= pool.length ? 'Finish Quiz' : 'Next Question'}
            </LiquidButton>
          ) : (
            <LiquidButton onClick={onClose} className="!px-4 !py-2 !text-xs">
              Close
            </LiquidButton>
          )}
        </div>
      </div>
    </LiquidModal>
  );
};

// ================================================================
// Slide Show Revision Deck Modal (Manual + Outline Extraction)
// ================================================================

interface SlideShowModalProps {
  open: boolean;
  notebookTitle: string;
  totalPages: number;
  currentPageIdx: number;
  slides: SlideCard[];
  loadingStage?: string | null;
  engineLabel?: string;
  onClose: () => void;
  onSaveSlides: (slides: SlideCard[]) => void;
  onGenerateAi: (
    scope: 'CURRENT' | 'RANGE' | 'ALL',
    fromPage: number,
    toPage: number,
    slideCount: number
  ) => void;
}

const SLIDE_KIND_META: Record<SlideKind, { label: string; hint: string }> = {
  TITLE: { label: 'Title Cover', hint: 'Chapter intro & core focus' },
  CONCEPT: { label: 'Concept Card', hint: 'Key ideas & bullets' },
  FORMULA: { label: 'Formula Vault', hint: 'Laws, rules & equations' },
  CHECK_QA: { label: 'Active Recall Q&A', hint: '3D flip reveal card' },
  SUMMARY: { label: 'Quick Recap', hint: '60-second checklist' },
};

export const SlideShowModal: React.FC<SlideShowModalProps> = ({
  open,
  notebookTitle,
  totalPages,
  currentPageIdx,
  slides,
  loadingStage,
  onClose,
  onSaveSlides,
  onGenerateAi,
}) => {
  const [view, setView] = useState<'STUDIO' | 'PRESENT'>('STUDIO');
  const [scope, setScope] = useState<'CURRENT' | 'RANGE' | 'ALL'>('ALL');
  const [fromPage, setFromPage] = useState(1);
  const [toPage, setToPage] = useState(Math.max(1, totalPages));
  const [slideCount, setSlideCount] = useState(6);

  const [editingId, setEditingId] = useState<string | null>(null);
  const [draftKind, setDraftKind] = useState<SlideKind>('CONCEPT');
  const [draftTheme, setDraftTheme] = useState<SlideThemeId>('AURORA_INDIGO');
  const [draftTitle, setDraftTitle] = useState('');
  const [draftSubtitle, setDraftSubtitle] = useState('');
  const [draftBullets, setDraftBullets] = useState('');
  const [draftCallout, setDraftCallout] = useState('');
  const [draftQuestion, setDraftQuestion] = useState('');
  const [draftAnswer, setDraftAnswer] = useState('');
  const [showManualForm, setShowManualForm] = useState(false);

  const [activeIdx, setActiveIdx] = useState(0);
  const [autoPlay, setAutoPlay] = useState(false);
  const [qaFlipped, setQaFlipped] = useState(false);

  useEffect(() => {
    setToPage((prev) => Math.max(1, Math.min(totalPages, prev || totalPages)));
    setFromPage((prev) => Math.max(1, Math.min(totalPages, prev || 1)));
  }, [totalPages]);

  useEffect(() => {
    if (activeIdx >= slides.length && slides.length > 0) {
      setActiveIdx(slides.length - 1);
    }
    setQaFlipped(false);
  }, [slides.length, activeIdx]);

  useEffect(() => {
    if (!open || view !== 'PRESENT' || !autoPlay || slides.length <= 1) return;
    const timer = window.setInterval(() => {
      setQaFlipped(false);
      setActiveIdx((i) => (i + 1) % slides.length);
    }, 5000);
    return () => window.clearInterval(timer);
  }, [open, view, autoPlay, slides.length]);

  useEffect(() => {
    if (!open || view !== 'PRESENT') return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'ArrowRight') {
        setQaFlipped(false);
        setActiveIdx((i) => Math.min(slides.length - 1, i + 1));
      } else if (e.key === 'ArrowLeft') {
        setQaFlipped(false);
        setActiveIdx((i) => Math.max(0, i - 1));
      } else if (e.key === ' ') {
        e.preventDefault();
        if (slides[activeIdx]?.kind === 'CHECK_QA') setQaFlipped((f) => !f);
        else setAutoPlay((p) => !p);
      } else if (e.key === 'Escape') {
        setAutoPlay(false);
        setView('STUDIO');
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open, view, slides, activeIdx]);

  const resetForm = () => {
    setEditingId(null);
    setDraftKind('CONCEPT');
    setDraftTheme('AURORA_INDIGO');
    setDraftTitle('');
    setDraftSubtitle('');
    setDraftBullets('');
    setDraftCallout('');
    setDraftQuestion('');
    setDraftAnswer('');
    setShowManualForm(false);
  };

  const openEditSlide = (sl: SlideCard) => {
    setEditingId(sl.id);
    setDraftKind(sl.kind);
    setDraftTheme(sl.theme);
    setDraftTitle(sl.title);
    setDraftSubtitle(sl.subtitle || '');
    setDraftBullets(sl.bullets.join('\n'));
    setDraftCallout(sl.callout || '');
    setDraftQuestion(sl.question || '');
    setDraftAnswer(sl.answer || '');
    setShowManualForm(true);
  };

  const handleSaveManualSlide = (e: React.FormEvent) => {
    e.preventDefault();
    if (!draftTitle.trim() && !draftQuestion.trim()) return;
    const newSlide: SlideCard = {
      id: editingId || `sl-man-${Date.now()}`,
      kind: draftKind,
      theme: draftTheme,
      title: draftTitle.trim() || draftQuestion.trim().slice(0, 60) || 'Revision Card',
      subtitle: draftSubtitle.trim() || undefined,
      bullets: draftBullets
        .split('\n')
        .map((s) => s.trim())
        .filter(Boolean),
      callout: draftCallout.trim() || undefined,
      question: draftKind === 'CHECK_QA' ? draftQuestion.trim() || draftTitle.trim() : undefined,
      answer: draftKind === 'CHECK_QA' ? draftAnswer.trim() || draftBullets.trim() : undefined,
      badge: SLIDE_KIND_META[draftKind].label.toUpperCase(),
    };

    if (editingId) {
      onSaveSlides(slides.map((s) => (s.id === editingId ? newSlide : s)));
    } else {
      onSaveSlides([...slides, newSlide]);
    }
    resetForm();
  };

  const moveSlide = (idx: number, delta: -1 | 1) => {
    const nextIdx = idx + delta;
    if (nextIdx < 0 || nextIdx >= slides.length) return;
    const copy = [...slides];
    const [picked] = copy.splice(idx, 1);
    copy.splice(nextIdx, 0, picked);
    onSaveSlides(copy);
  };

  const currentSlide = slides[Math.max(0, Math.min(slides.length - 1, activeIdx))];
  const themeSpec = currentSlide ? SLIDE_THEMES[currentSlide.theme] : SLIDE_THEMES.AURORA_INDIGO;

  if (!open) return null;

  if (view === 'PRESENT' && currentSlide) {
    return (
      <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/55 p-4 backdrop-blur-md">
        <div
          key={currentSlide.id}
          className="relative flex h-[86vh] w-full max-w-4xl flex-col overflow-hidden rounded-[2rem] border border-white/25 text-white shadow-2xl"
          style={{
            background: `linear-gradient(135deg, ${themeSpec.gradA} 0%, ${themeSpec.gradB} 100%)`,
          }}
        >
          <div className="flex items-center justify-between border-b border-white/15 bg-black/20 px-6 py-4 backdrop-blur-md">
            <div className="flex items-center gap-3">
              <span
                className="rounded-lg px-2.5 py-1 text-[10px] font-extrabold uppercase tracking-wider text-slate-950"
                style={{ backgroundColor: themeSpec.accent }}
              >
                {currentSlide.badge || SLIDE_KIND_META[currentSlide.kind].label}
              </span>
              <span className="font-mono-num text-xs font-semibold text-white/85">
                Slide {activeIdx + 1} / {slides.length}
              </span>
            </div>

            <div className="flex items-center gap-2">
              <LiquidButton
                onClick={() => setAutoPlay((p) => !p)}
                className="!px-3 !py-1.5 !text-xs"
              >
                {autoPlay ? <Pause className="h-3.5 w-3.5" /> : <Play className="h-3.5 w-3.5" />}
                {autoPlay ? 'Auto-Play On' : 'Auto-Play'}
              </LiquidButton>
              <LiquidButton
                onClick={() => {
                  setAutoPlay(false);
                  setView('STUDIO');
                }}
                className="!px-3 !py-1.5 !text-xs"
              >
                Back to Deck
              </LiquidButton>
              <LiquidButton
                onClick={() => {
                  setAutoPlay(false);
                  onClose();
                }}
                className="!h-8 !w-8 !p-0"
              >
                <X className="h-4 w-4" />
              </LiquidButton>
            </div>
          </div>

          <div className="flex gap-1.5 px-6 pt-3">
            {slides.map((sl, i) => (
              <button
                key={sl.id}
                type="button"
                onClick={() => {
                  setQaFlipped(false);
                  setActiveIdx(i);
                }}
                className="h-1.5 flex-1 overflow-hidden rounded-full bg-white/20 transition-all hover:bg-white/35"
              >
                <div
                  className="h-full rounded-full transition-all duration-300"
                  style={{
                    width: i <= activeIdx ? '100%' : '0%',
                    backgroundColor: i === activeIdx ? themeSpec.accent : '#ffffff',
                  }}
                />
              </button>
            ))}
          </div>

          <div className="flex flex-1 flex-col justify-center overflow-y-auto px-8 py-6 sm:px-12">
            {currentSlide.kind === 'CHECK_QA' ? (
              <div className="mx-auto flex w-full max-w-2xl flex-col items-center">
                <div className="mb-3 text-xs font-bold uppercase tracking-widest text-white/75">
                  {currentSlide.subtitle || 'Active Recall Challenge — Tap card to flip'}
                </div>
                <button
                  type="button"
                  onClick={() => setQaFlipped((f) => !f)}
                  className="perspective-1000 w-full text-left"
                >
                  <div
                    className={`preserve-3d relative min-h-[260px] w-full rounded-3xl transition-transform duration-500 ${
                      qaFlipped ? 'rotate-y-180' : ''
                    }`}
                  >
                    <div className="backface-hidden absolute inset-0 flex flex-col justify-between rounded-3xl border border-white/25 bg-black/20 p-8 backdrop-blur-md">
                      <span className="inline-flex w-fit items-center gap-1.5 rounded-lg bg-white/15 px-2.5 py-1 text-[11px] font-bold uppercase tracking-wider">
                        Prompt Question
                      </span>
                      <div className="font-display text-2xl font-bold leading-snug sm:text-3xl">
                        {currentSlide.question || currentSlide.title}
                      </div>
                      <div className="inline-flex items-center gap-1.5 text-xs font-semibold text-white/80">
                        <Eye className="h-4 w-4" /> Tap to reveal answer (or press Space)
                      </div>
                    </div>

                    <div className="backface-hidden rotate-y-180 absolute inset-0 flex flex-col justify-between rounded-3xl border border-emerald-300/40 bg-emerald-950/80 p-8 backdrop-blur-md">
                      <span className="inline-flex w-fit items-center gap-1.5 rounded-lg bg-emerald-400/20 px-2.5 py-1 text-[11px] font-bold uppercase tracking-wider text-emerald-200">
                        <CheckCircle2 className="h-3.5 w-3.5" /> Answer
                      </span>
                      <div className="text-lg font-medium leading-relaxed text-white sm:text-xl">
                        {currentSlide.answer || currentSlide.bullets[0] || currentSlide.title}
                      </div>
                      <div className="inline-flex items-center gap-1.5 text-xs font-semibold text-emerald-200">
                        <EyeOff className="h-4 w-4" /> Tap to flip back
                      </div>
                    </div>
                  </div>
                </button>
              </div>
            ) : (
              <div className="mx-auto w-full max-w-3xl space-y-5">
                <div>
                  <h2 className="font-display text-3xl font-extrabold leading-tight tracking-tight sm:text-4xl">
                    {currentSlide.title}
                  </h2>
                  {currentSlide.subtitle && (
                    <p className="mt-1.5 text-sm font-medium text-white/80 sm:text-base">
                      {currentSlide.subtitle}
                    </p>
                  )}
                </div>

                {currentSlide.bullets.length > 0 && (
                  <div className="space-y-2.5">
                    {currentSlide.bullets.map((b, idx) => (
                      <div
                        key={idx}
                        className="flex items-start gap-3 rounded-2xl border border-white/18 bg-black/15 px-4 py-3 backdrop-blur-sm"
                      >
                        <span
                          className="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-lg text-xs font-extrabold text-slate-950"
                          style={{ backgroundColor: themeSpec.accent }}
                        >
                          {idx + 1}
                        </span>
                        <span className="text-sm font-medium leading-relaxed sm:text-base">{b}</span>
                      </div>
                    ))}
                  </div>
                )}

                {currentSlide.callout && (
                  <div className="rounded-2xl border border-white/25 bg-black/25 px-5 py-3.5 text-sm font-semibold leading-relaxed text-white/95 backdrop-blur-md">
                    <span className="mr-2 text-xs uppercase tracking-wider" style={{ color: themeSpec.accent }}>
                      Takeaway:
                    </span>
                    {currentSlide.callout}
                  </div>
                )}
              </div>
            )}
          </div>

          <div className="flex items-center justify-between border-t border-white/15 bg-black/20 px-6 py-4 backdrop-blur-md">
            <LiquidButton
              disabled={activeIdx === 0}
              onClick={() => {
                setQaFlipped(false);
                setActiveIdx((i) => Math.max(0, i - 1));
              }}
              className="!px-4 !py-2 !text-xs"
            >
              <ChevronLeft className="h-4 w-4" /> Previous
            </LiquidButton>

            <span className="hidden text-[11px] text-white/75 sm:inline">
              Use ← / → arrows to step · Space to flip or pause
            </span>

            {activeIdx + 1 >= slides.length ? (
              <LiquidButton
                onClick={() => {
                  setQaFlipped(false);
                  setActiveIdx(0);
                }}
                className="!px-4 !py-2 !text-xs"
              >
                <RotateCcw className="h-3.5 w-3.5" /> Replay Deck
              </LiquidButton>
            ) : (
              <LiquidButton
                onClick={() => {
                  setQaFlipped(false);
                  setActiveIdx((i) => Math.min(slides.length - 1, i + 1));
                }}
                className="!px-5 !py-2 !text-xs"
              >
                Next Slide <ChevronRight className="h-4 w-4" />
              </LiquidButton>
            )}
          </div>
        </div>
      </div>
    );
  }

  return (
    <LiquidModal open={open} onClose={onClose} maxWidthClass="max-w-4xl">
      <div className="flex max-h-[88vh] flex-col">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[#1F2421]/10 px-6 py-4 dark:border-[#EDE8DB]/10">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-[#24483F] text-[#F5F2E8] shadow-md dark:bg-[#8CC1AC] dark:text-[#151E1B]">
              <Layers className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-base font-bold">Slide Show Revision — {notebookTitle}</h2>
              <p className="text-xs text-[#5A5F58] dark:text-[#B1AC9F]">
                {slides.length} slide{slides.length === 1 ? '' : 's'} ready · Deck Studio
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {slides.length > 0 && (
              <LiquidButton
                tint="#24483F"
                onClick={() => {
                  setActiveIdx(0);
                  setView('PRESENT');
                }}
                className="!px-4 !py-2 !text-xs"
              >
                <Play className="h-3.5 w-3.5" /> Launch Slide Show
              </LiquidButton>
            )}
            <LiquidButton onClick={onClose} className="!h-9 !w-9 !p-0">
              <X className="h-4 w-4" />
            </LiquidButton>
          </div>
        </div>

        <div className="flex-1 space-y-5 overflow-y-auto p-6">
          <LiquidGlassSurface variant="frost" radius={22} className="p-5">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <div className="text-sm font-bold text-[#24483F] dark:text-[#8CC1AC]">
                  Chapter Slide Builder
                </div>
                <p className="mt-0.5 text-xs text-[#5A5F58] dark:text-[#B1AC9F]">
                  Extract structured Title, Concept, Formula, Q&amp;A, and Recap cards directly from your pages
                </p>
              </div>
              <LiquidButton
                tint="#24483F"
                disabled={Boolean(loadingStage)}
                onClick={() =>
                  onGenerateAi(
                    scope,
                    Math.min(fromPage, toPage),
                    Math.max(fromPage, toPage),
                    slideCount
                  )
                }
                className="!px-4 !py-2 !text-xs"
              >
                <Layers className="h-3.5 w-3.5" />
                {loadingStage || 'Build Deck from Notes'}
              </LiquidButton>
            </div>

            <div className="mt-4 flex flex-wrap items-center gap-3">
              <LiquidSegmentedControl
                value={scope}
                onChange={setScope}
                options={[
                  { value: 'CURRENT', label: `Page ${currentPageIdx + 1}` },
                  { value: 'RANGE', label: 'Chapter Range' },
                  { value: 'ALL', label: `All (${totalPages}p)` },
                ]}
              />

              {scope === 'RANGE' && (
                <div className="flex items-center gap-1.5 text-xs font-semibold">
                  <span>From</span>
                  <input
                    type="number"
                    min={1}
                    max={totalPages}
                    value={fromPage}
                    onChange={(e) => setFromPage(Math.max(1, Math.min(totalPages, Number(e.target.value) || 1)))}
                    className="liquid-glass-frost w-14 rounded-xl px-2 py-1 text-center text-xs outline-none"
                  />
                  <span>to</span>
                  <input
                    type="number"
                    min={1}
                    max={totalPages}
                    value={toPage}
                    onChange={(e) => setToPage(Math.max(1, Math.min(totalPages, Number(e.target.value) || 1)))}
                    className="liquid-glass-frost w-14 rounded-xl px-2 py-1 text-center text-xs outline-none"
                  />
                </div>
              )}

              <div className="ml-auto flex items-center gap-2.5 text-xs font-semibold">
                <span className="text-[#5A5F58] dark:text-[#B1AC9F]">Slides: {slideCount}</span>
                <LiquidSlider
                  min={4}
                  max={12}
                  step={1}
                  value={slideCount}
                  onChange={setSlideCount}
                  className="w-28"
                />
              </div>
            </div>
          </LiquidGlassSurface>

          <div className="flex items-center justify-between">
            <h3 className="text-xs font-bold uppercase tracking-wider text-[#5A5F58] dark:text-[#B1AC9F]">
              Deck Cards ({slides.length})
            </h3>
            <LiquidButton
              onClick={() => {
                resetForm();
                setShowManualForm((v) => !v);
              }}
              className="!px-3 !py-1.5 !text-xs"
            >
              <Plus className="h-3.5 w-3.5" /> Add Manual Slide
            </LiquidButton>
          </div>

          {showManualForm && (
            <LiquidGlassSurface variant="frost" radius={20} className="p-4">
              <form onSubmit={handleSaveManualSlide} className="space-y-3">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="text-xs font-bold">{editingId ? 'Edit Slide' : 'New Manual Slide'}</span>
                  <div className="flex flex-wrap gap-1.5">
                    {(Object.keys(SLIDE_THEMES) as SlideThemeId[]).map((tid) => (
                      <button
                        key={tid}
                        type="button"
                        onClick={() => setDraftTheme(tid)}
                        title={SLIDE_THEMES[tid].label}
                        className={`h-5 w-7 rounded-md border ${
                          draftTheme === tid ? 'ring-2 ring-[#24483F] ring-offset-1 dark:ring-[#8CC1AC]' : 'border-black/15'
                        }`}
                        style={{
                          background: `linear-gradient(135deg, ${SLIDE_THEMES[tid].gradA}, ${SLIDE_THEMES[tid].gradB})`,
                        }}
                      />
                    ))}
                  </div>
                </div>

                <div className="grid gap-2.5 sm:grid-cols-3">
                  <LiquidMenuButton
                    align="left"
                    label={SLIDE_KIND_META[draftKind].label}
                    items={(Object.keys(SLIDE_KIND_META) as SlideKind[]).map((k) => ({
                      id: k,
                      label: SLIDE_KIND_META[k].label,
                      subtitle: SLIDE_KIND_META[k].hint,
                      selected: draftKind === k,
                      onSelect: () => setDraftKind(k),
                    }))}
                  />
                  <LiquidInput
                    type="text"
                    value={draftTitle}
                    onChange={(e) => setDraftTitle(e.target.value)}
                    placeholder="Slide title / topic"
                    wrapperClassName="sm:col-span-2"
                  />
                </div>

                {draftKind === 'CHECK_QA' ? (
                  <div className="grid gap-2.5 sm:grid-cols-2">
                    <LiquidInput
                      type="text"
                      value={draftQuestion}
                      onChange={(e) => setDraftQuestion(e.target.value)}
                      placeholder="Question / prompt..."
                    />
                    <LiquidInput
                      type="text"
                      value={draftAnswer}
                      onChange={(e) => setDraftAnswer(e.target.value)}
                      placeholder="Answer / explanation..."
                    />
                  </div>
                ) : (
                  <>
                    <LiquidInput
                      type="text"
                      value={draftSubtitle}
                      onChange={(e) => setDraftSubtitle(e.target.value)}
                      placeholder="Subtitle (optional)"
                    />
                    <textarea
                      rows={3}
                      value={draftBullets}
                      onChange={(e) => setDraftBullets(e.target.value)}
                      placeholder="Key bullets (one per line)..."
                      className="liquid-glass-frost w-full rounded-2xl px-3.5 py-2 text-xs outline-none placeholder:text-[#5A5F58] dark:placeholder:text-[#B1AC9F]"
                    />
                    <LiquidInput
                      type="text"
                      value={draftCallout}
                      onChange={(e) => setDraftCallout(e.target.value)}
                      placeholder="Formula or Takeaway callout (optional)"
                    />
                  </>
                )}

                <div className="flex justify-end gap-2">
                  <LiquidButton onClick={resetForm} className="!px-3 !py-1.5 !text-xs">
                    Cancel
                  </LiquidButton>
                  <button
                    type="submit"
                    className="inline-flex items-center gap-1 rounded-full bg-[#24483F] px-4 py-1.5 text-xs font-semibold text-[#F5F2E8] dark:bg-[#8CC1AC] dark:text-[#151E1B]"
                  >
                    <Check className="h-3.5 w-3.5" /> {editingId ? 'Save Slide' : 'Add Slide'}
                  </button>
                </div>
              </form>
            </LiquidGlassSurface>
          )}

          {slides.length === 0 ? (
            <LiquidGlassSurface variant="frost" radius={20} className="border-dashed py-10 text-center">
              <p className="text-sm font-bold">No revision slides yet</p>
              <p className="mt-1 text-xs text-[#5A5F58] dark:text-[#B1AC9F]">
                Click &ldquo;Build Deck from Notes&rdquo; above or add manual slides.
              </p>
            </LiquidGlassSurface>
          ) : (
            <div className="grid gap-3 sm:grid-cols-2">
              {slides.map((sl, idx) => {
                const th = SLIDE_THEMES[sl.theme];
                return (
                  <div
                    key={sl.id}
                    className="group relative flex flex-col justify-between overflow-hidden rounded-2xl p-4 text-white shadow-sm transition hover:-translate-y-0.5 hover:shadow-md"
                    style={{
                      background: `linear-gradient(135deg, ${th.gradA}, ${th.gradB})`,
                    }}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <span
                        className="rounded-md px-2 py-0.5 text-[10px] font-extrabold uppercase text-slate-950"
                        style={{ backgroundColor: th.accent }}
                      >
                        {idx + 1}. {SLIDE_KIND_META[sl.kind].label}
                      </span>
                      <div className="flex items-center gap-0.5">
                        <button
                          type="button"
                          disabled={idx === 0}
                          onClick={() => moveSlide(idx, -1)}
                          className="rounded p-1 text-white/75 hover:bg-white/15 disabled:opacity-25"
                          title="Move up"
                        >
                          <ArrowUp className="h-3.5 w-3.5" />
                        </button>
                        <button
                          type="button"
                          disabled={idx === slides.length - 1}
                          onClick={() => moveSlide(idx, 1)}
                          className="rounded p-1 text-white/75 hover:bg-white/15 disabled:opacity-25"
                          title="Move down"
                        >
                          <ArrowDown className="h-3.5 w-3.5" />
                        </button>
                        <button
                          type="button"
                          onClick={() => openEditSlide(sl)}
                          className="rounded p-1 text-white/75 hover:bg-white/15"
                          title="Edit slide"
                        >
                          <Edit3 className="h-3.5 w-3.5" />
                        </button>
                        <button
                          type="button"
                          onClick={() => onSaveSlides(slides.filter((s) => s.id !== sl.id))}
                          className="rounded p-1 text-white/75 hover:bg-red-500/30"
                          title="Delete slide"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    </div>

                    <div
                      onClick={() => {
                        setActiveIdx(idx);
                        setView('PRESENT');
                      }}
                      className="mt-3 cursor-pointer"
                    >
                      <div className="line-clamp-1 font-display text-base font-bold">{sl.title}</div>
                      <div className="mt-1 line-clamp-2 text-xs text-white/85">
                        {sl.kind === 'CHECK_QA'
                          ? sl.question || sl.answer
                          : sl.bullets[0] || sl.callout || sl.subtitle || 'Tap to present'}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </LiquidModal>
  );
};
