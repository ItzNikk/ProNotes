import React, { useEffect, useRef, useState } from 'react';
import { Check, Layers, Sparkles, X } from 'lucide-react';
import {
  AppSettings,
  COVER_COLORS,
  COVER_PATTERNS,
  PAGE_PRESETS,
  PagePresetId,
  PAPER_STYLES,
  PAPER_TINTS,
  PaperStyle,
  UNITS_PER_MM,
} from '../types/models';
import { drawCoverOnCanvas, drawPaperTemplate, drawWhiteboardBackdrop } from '../utils/canvasRenderer';

export const CoverCanvas: React.FC<{
  spec: string;
  title?: string;
  className?: string;
  width?: number;
  height?: number;
}> = ({ spec, title = '', className = '', width = 180, height = 240 }) => {
  const ref = useRef<HTMLCanvasElement>(null);
  useEffect(() => {
    const c = ref.current;
    if (!c) return;
    const ctx = c.getContext('2d');
    if (!ctx) return;
    ctx.clearRect(0, 0, c.width, c.height);
    drawCoverOnCanvas(ctx, c.width, c.height, spec, title);
  }, [spec, title, width, height]);

  return (
    <canvas
      ref={ref}
      width={width}
      height={height}
      className={`rounded-r-lg rounded-l-[3px] shadow-md ${className}`}
    />
  );
};

export const PaperPreviewCanvas: React.FC<{
  paper: PaperStyle;
  paperColor: string;
  isWhiteboard?: boolean;
  landscape?: boolean;
  className?: string;
  width?: number;
  height?: number;
}> = ({
  paper,
  paperColor,
  isWhiteboard = false,
  landscape = false,
  className = '',
  width = 160,
  height = 224,
}) => {
  const ref = useRef<HTMLCanvasElement>(null);
  const w = landscape ? Math.max(width, height) : Math.min(width, height);
  const h = landscape ? Math.min(width, height) : Math.max(width, height);

  useEffect(() => {
    const c = ref.current;
    if (!c) return;
    const ctx = c.getContext('2d');
    if (!ctx) return;
    ctx.clearRect(0, 0, c.width, c.height);
    if (isWhiteboard) {
      drawWhiteboardBackdrop(ctx, c.width, c.height, c.width * 0.5, c.height * 0.5, 0.5, paperColor);
    } else {
      drawPaperTemplate(ctx, c.width, c.height, paper, paperColor, 1, null, true);
    }
  }, [paper, paperColor, isWhiteboard, w, h]);

  return <canvas ref={ref} width={w} height={h} className={className} />;
};

export const FolderArt: React.FC<{ color: string; emoji?: string; className?: string }> = ({
  color,
  emoji,
  className = 'h-20 w-24',
}) => (
  <div className={`relative flex items-center justify-center select-none ${className}`}>
    <svg viewBox="0 0 120 100" className="h-full w-full drop-shadow-sm">
      <path
        d="M6 20 Q6 12 14 12 L42 12 Q47 12 51 18 L55 24 L106 24 Q114 24 114 32 L114 84 Q114 92 106 92 L14 92 Q6 92 6 84 Z"
        fill={color}
        opacity="0.78"
      />
      <rect x="6" y="30" width="108" height="62" rx="9" fill={color} />
      <rect x="6" y="30" width="108" height="7" rx="3" fill="rgba(255,255,255,0.22)" />
    </svg>
    {emoji && <span className="absolute pt-3 text-2xl leading-none">{emoji}</span>}
  </div>
);

export interface NewNotebookPayload {
  title: string;
  cover: string;
  coverColor: string;
  paper: PaperStyle;
  width: number;
  height: number;
  paperColor: string | null;
  coverPage: boolean;
  isWhiteboard: boolean;
  isMindMap: boolean;
}

interface NewNotebookModalProps {
  open: boolean;
  defaults: AppSettings;
  initialMode?: 'notebook' | 'whiteboard' | 'mindmap';
  onClose: () => void;
  onCreate: (payload: NewNotebookPayload) => void;
}

export const NewNotebookModal: React.FC<NewNotebookModalProps> = ({
  open,
  defaults,
  initialMode = 'notebook',
  onClose,
  onCreate,
}) => {
  const [title, setTitle] = useState(
    initialMode === 'whiteboard'
      ? 'Infinite Whiteboard'
      : initialMode === 'mindmap'
        ? 'Mind Map'
        : 'Untitled notebook'
  );
  const [mode, setMode] = useState<'notebook' | 'whiteboard' | 'mindmap'>(initialMode);
  const [tab, setTab] = useState<'paper' | 'cover' | 'size'>('paper');
  const [cover, setCover] = useState<string>(
    initialMode === 'mindmap'
      ? 'pattern:gradient:#6250D6'
      : initialMode === 'whiteboard'
        ? 'pattern:geometric:#0F7E5C'
        : 'pattern:leather:#3D6CF4'
  );
  const [paper, setPaper] = useState<PaperStyle>(
    initialMode === 'notebook' ? defaults.defaultPaper : 'DOTTED'
  );
  const [paperColor, setPaperColor] = useState<string>(defaults.paperTint || '#FFFFFF');
  const [preset, setPreset] = useState<PagePresetId>(
    initialMode === 'notebook' ? defaults.defaultPreset : 'WHITEBOARD'
  );
  const [landscape, setLandscape] = useState<boolean>(defaults.defaultLandscape);
  const [coverPage, setCoverPage] = useState<boolean>(defaults.defaultCoverPage);
  const [wMm, setWMm] = useState<string>('210');
  const [hMm, setHMm] = useState<string>('297');
  const [paperGroup, setPaperGroup] = useState<string>('All');

  useEffect(() => {
    if (!open) return;
    setMode(initialMode);
    if (initialMode === 'whiteboard') {
      setTitle('Infinite Whiteboard');
      setPreset('WHITEBOARD');
      setPaper('DOTTED');
      setCover('pattern:geometric:#0F7E5C');
    } else if (initialMode === 'mindmap') {
      setTitle('Concept Mind Map');
      setPreset('WHITEBOARD');
      setPaper('DOTTED');
      setCover('pattern:gradient:#6250D6');
    } else {
      setTitle('Untitled notebook');
      setPreset(defaults.defaultPreset || 'A4');
      setPaper(defaults.defaultPaper || 'RULED');
      setCover('pattern:leather:#3D6CF4');
    }
  }, [open, initialMode, defaults]);

  if (!open) return null;

  const isBoard = mode === 'whiteboard' || mode === 'mindmap' || preset === 'WHITEBOARD';
  const coverColor = cover.split(':').pop() || '#3D6CF4';

  const groups = ['All', 'Writing', 'Grids', 'Planning', 'Study', 'Music'];
  const visiblePapers =
    paperGroup === 'All' ? PAPER_STYLES : PAPER_STYLES.filter((p) => p.group === paperGroup);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    let w = Math.max(40, Math.min(1000, Number(wMm) || 210));
    let h = Math.max(40, Math.min(1000, Number(hMm) || 297));
    if (!isBoard && landscape !== (w > h)) {
      const tmp = w;
      w = h;
      h = tmp;
    }
    onCreate({
      title: title.trim() || 'Untitled notebook',
      cover,
      coverColor: coverColor.startsWith('#') ? coverColor : '#3D6CF4',
      paper,
      width: isBoard ? 2400 : Math.round(w * UNITS_PER_MM),
      height: isBoard ? 1600 : Math.round(h * UNITS_PER_MM),
      paperColor,
      coverPage: coverPage && !isBoard,
      isWhiteboard: isBoard,
      isMindMap: mode === 'mindmap',
    });
  };

  const coverOptions: string[] = [
    ...COVER_COLORS.map((c) => `solid:${c}`),
    ...COVER_PATTERNS.flatMap((pat) => {
      const tones =
        pat === 'leather'
          ? ['#5D3A23', '#1C1C1E', '#0E2A47', '#3D6CF4']
          : pat === 'kraft'
            ? ['#C49A6C', '#A77B4F']
            : pat === 'gradient'
              ? ['#3D6CF4', '#9B51E0', '#EB5757', '#2BB673']
              : ['#3D6CF4', '#2BB673', '#EB5757'];
      return tones.map((c) => `pattern:${pat}:${c}`);
    }),
  ];

  const activePaperMeta = PAPER_STYLES.find((p) => p.id === paper);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
      <form
        onSubmit={handleSubmit}
        className="flex max-h-[90vh] w-full max-w-4xl flex-col overflow-hidden rounded-3xl border border-black/10 bg-white shadow-2xl dark:border-white/10 dark:bg-[#191A1F] animate-stagger-in"
      >
        {/* Header */}
        <div className="flex items-center justify-between border-b border-black/8 px-6 py-4 dark:border-white/10">
          <div>
            <h2 className="text-lg font-bold tracking-tight">Create workspace</h2>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Preview paper patterns, choose page size, or launch an unbounded whiteboard / mind map
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-full p-2 text-slate-500 hover:bg-black/5 dark:hover:bg-white/10"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Body: Left live preview + Right configuration */}
        <div className="grid min-h-0 flex-1 grid-cols-1 overflow-hidden md:grid-cols-12">
          {/* Left Live Preview Column */}
          <div className="flex flex-col items-center justify-between border-b border-black/8 bg-[#F4F5F7] p-5 dark:border-white/10 dark:bg-[#131418] md:col-span-4 md:border-b-0 md:border-r">
            <div className="w-full space-y-3">
              {/* Quick Mode Selector */}
              <div className="grid grid-cols-3 gap-1.5 rounded-xl bg-black/5 p-1 dark:bg-white/5">
                {[
                  { id: 'notebook', label: 'Notebook' },
                  { id: 'whiteboard', label: 'Whiteboard' },
                  { id: 'mindmap', label: 'Mind Map' },
                ].map((m) => (
                  <button
                    key={m.id}
                    type="button"
                    onClick={() => {
                      const next = m.id as 'notebook' | 'whiteboard' | 'mindmap';
                      setMode(next);
                      if (next === 'whiteboard' || next === 'mindmap') {
                        setPreset('WHITEBOARD');
                        if (next === 'mindmap') setTitle('Concept Mind Map');
                        else setTitle('Infinite Whiteboard');
                      } else {
                        setPreset('A4');
                        setTitle('Untitled notebook');
                      }
                    }}
                    className={`rounded-lg py-1.5 text-xs font-semibold transition ${
                      mode === m.id
                        ? 'bg-white text-slate-900 shadow-sm dark:bg-[#252730] dark:text-white'
                        : 'text-slate-500 hover:text-slate-800 dark:text-slate-400'
                    }`}
                  >
                    {m.label}
                  </button>
                ))}
              </div>

              {/* Dual Live Preview: Cover + Paper Pattern */}
              <div className="flex flex-col items-center gap-3 rounded-2xl border border-black/8 bg-white/75 p-4 dark:border-white/10 dark:bg-[#1C1D24]">
                <div className="flex items-end justify-center gap-3">
                  <div className="flex flex-col items-center gap-1">
                    <CoverCanvas spec={cover} title={title} width={88} height={118} />
                    <span className="text-[10px] font-medium text-slate-500">Cover</span>
                  </div>
                  <div className="flex flex-col items-center gap-1">
                    <div className="overflow-hidden rounded-lg border border-black/15 shadow-md dark:border-white/15">
                      <PaperPreviewCanvas
                        paper={paper}
                        paperColor={paperColor}
                        isWhiteboard={isBoard}
                        landscape={isBoard ? true : landscape}
                        width={118}
                        height={156}
                      />
                    </div>
                    <span className="text-[10px] font-medium text-slate-500">
                      {isBoard ? 'Infinite Canvas' : `${activePaperMeta?.label || paper} Pattern`}
                    </span>
                  </div>
                </div>

                <div className="w-full rounded-xl bg-black/[0.04] px-3 py-2 text-center text-xs dark:bg-white/[0.04]">
                  <span className="font-semibold text-slate-700 dark:text-slate-200">
                    {isBoard
                      ? 'Unbounded Infinite Board'
                      : `${PAGE_PRESETS.find((x) => x.id === preset)?.label || 'A4'} ${landscape ? 'Landscape' : 'Portrait'}`}
                  </span>
                  {preset === 'A4' && !isBoard && (
                    <span className="ml-1.5 inline-flex items-center rounded-md bg-blue-500/15 px-1.5 py-0.5 text-[10px] font-semibold text-blue-600 dark:text-blue-300">
                      Recommended
                    </span>
                  )}
                </div>
              </div>
            </div>

            <div className="mt-4 w-full">
              <label className="mb-1 block text-xs font-semibold text-slate-600 dark:text-slate-300">
                Notebook Title
              </label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Enter notebook title..."
                className="w-full rounded-xl border border-black/12 bg-white px-3.5 py-2.5 text-sm font-medium outline-none focus:border-blue-500 dark:border-white/15 dark:bg-[#1E2028]"
              />
            </div>
          </div>

          {/* Right Configuration Tabs */}
          <div className="flex min-h-0 flex-col md:col-span-8">
            <div className="flex gap-2 border-b border-black/8 px-6 pt-3 dark:border-white/10">
              {[
                { id: 'paper', label: 'Paper & Pattern Preview' },
                { id: 'size', label: 'Page Size & Canvas' },
                { id: 'cover', label: 'Cover Design' },
              ].map((t) => (
                <button
                  key={t.id}
                  type="button"
                  onClick={() => setTab(t.id as 'paper' | 'cover' | 'size')}
                  className={`border-b-2 px-3 pb-2.5 text-sm font-semibold transition ${
                    tab === t.id
                      ? 'border-blue-600 text-blue-600 dark:border-blue-400 dark:text-blue-400'
                      : 'border-transparent text-slate-500 hover:text-slate-800 dark:text-slate-400'
                  }`}
                >
                  {t.label}
                </button>
              ))}
            </div>

            <div className="flex-1 overflow-y-auto p-6">
              {tab === 'paper' && (
                <div className="space-y-5">
                  {/* Paper Color Tint */}
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                      Paper Tint
                    </span>
                    <div className="flex items-center gap-2">
                      {PAPER_TINTS.map((tint) => (
                        <button
                          key={tint.id}
                          type="button"
                          onClick={() => setPaperColor(tint.color)}
                          title={tint.label}
                          className={`h-7 w-7 rounded-full border transition ${
                            paperColor === tint.color
                              ? 'scale-110 border-blue-600 ring-2 ring-blue-500/30'
                              : 'border-black/20 dark:border-white/25'
                          }`}
                          style={{ backgroundColor: tint.color }}
                        />
                      ))}
                    </div>
                  </div>

                  {/* Group Filter */}
                  <div className="flex flex-wrap gap-1.5">
                    {groups.map((grp) => (
                      <button
                        key={grp}
                        type="button"
                        onClick={() => setPaperGroup(grp)}
                        className={`rounded-lg px-2.5 py-1 text-xs font-semibold transition ${
                          paperGroup === grp
                            ? 'bg-blue-600 text-white'
                            : 'bg-black/5 text-slate-600 hover:bg-black/10 dark:bg-white/8 dark:text-slate-300'
                        }`}
                      >
                        {grp}
                      </button>
                    ))}
                  </div>

                  {/* Live Rendered Pattern Previews Grid */}
                  <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 lg:grid-cols-5">
                    {visiblePapers.map((p) => {
                      const sel = paper === p.id;
                      return (
                        <button
                          key={p.id}
                          type="button"
                          onClick={() => setPaper(p.id)}
                          className={`group flex flex-col items-center rounded-2xl border p-2 transition ${
                            sel
                              ? 'border-blue-600 bg-blue-50/70 ring-2 ring-blue-500/20 dark:bg-blue-950/30'
                              : 'border-black/8 hover:border-black/25 dark:border-white/10 dark:hover:border-white/25'
                          }`}
                        >
                          <div className="relative overflow-hidden rounded-lg border border-black/10 shadow-sm dark:border-white/10">
                            <PaperPreviewCanvas
                              paper={p.id}
                              paperColor={paperColor}
                              isWhiteboard={false}
                              landscape={!isBoard && landscape}
                              width={92}
                              height={120}
                            />
                            {sel && (
                              <span className="absolute right-1.5 top-1.5 flex h-5 w-5 items-center justify-center rounded-full bg-blue-600 text-white shadow">
                                <Check className="h-3 w-3" />
                              </span>
                            )}
                          </div>
                          <span className="mt-1.5 text-center text-[11px] font-medium leading-tight text-slate-700 dark:text-slate-200">
                            {p.label}
                          </span>
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}

              {tab === 'size' && (
                <div className="space-y-5">
                  <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2">
                    {PAGE_PRESETS.map((p) => {
                      const sel = preset === p.id;
                      return (
                        <button
                          key={p.id}
                          type="button"
                          onClick={() => {
                            setPreset(p.id);
                            if (p.id !== 'CUSTOM' && p.id !== 'WHITEBOARD') {
                              setWMm(String(Math.round(p.wMm)));
                              setHMm(String(Math.round(p.hMm)));
                            }
                            if (p.isWhiteboard && mode === 'notebook') {
                              setMode('whiteboard');
                            } else if (!p.isWhiteboard && mode === 'whiteboard') {
                              setMode('notebook');
                            }
                          }}
                          className={`flex items-start justify-between rounded-2xl border p-3.5 text-left transition ${
                            sel
                              ? 'border-blue-600 bg-blue-50/60 ring-2 ring-blue-500/20 dark:bg-blue-950/25'
                              : 'border-black/10 hover:border-black/25 dark:border-white/10 dark:hover:border-white/25'
                          }`}
                        >
                          <div>
                            <div className="flex items-center gap-2">
                              <span className="text-sm font-bold text-slate-800 dark:text-slate-100">
                                {p.label}
                              </span>
                              {p.recommended && (
                                <span className="inline-flex items-center gap-1 rounded-full bg-blue-600 px-2 py-0.5 text-[10px] font-bold text-white">
                                  <Sparkles className="h-2.5 w-2.5" /> Recommended
                                </span>
                              )}
                              {p.isWhiteboard && (
                                <span className="inline-flex items-center gap-1 rounded-full bg-emerald-600/15 px-2 py-0.5 text-[10px] font-bold text-emerald-700 dark:text-emerald-300">
                                  <Layers className="h-2.5 w-2.5" /> Infinite
                                </span>
                              )}
                            </div>
                            <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">
                              {p.subtitle}
                            </p>
                          </div>
                          {sel && <Check className="mt-0.5 h-4 w-4 shrink-0 text-blue-600" />}
                        </button>
                      );
                    })}
                  </div>

                  {!isBoard && (
                    <div className="flex items-center justify-between rounded-2xl border border-black/8 bg-black/[0.02] px-4 py-3 dark:border-white/10 dark:bg-white/[0.02]">
                      <div>
                        <div className="text-sm font-semibold">Landscape orientation</div>
                        <div className="text-xs text-slate-500">
                          Swaps width & height for horizontal writing
                        </div>
                      </div>
                      <button
                        type="button"
                        onClick={() => setLandscape((v) => !v)}
                        className={`relative h-6 w-11 rounded-full transition ${
                          landscape ? 'bg-blue-600' : 'bg-slate-300 dark:bg-slate-700'
                        }`}
                      >
                        <span
                          className={`absolute top-0.5 h-5 w-5 rounded-full bg-white shadow transition-transform ${
                            landscape ? 'left-[22px]' : 'left-0.5'
                          }`}
                        />
                      </button>
                    </div>
                  )}

                  {preset === 'CUSTOM' && (
                    <div className="grid grid-cols-2 gap-3">
                      <div>
                        <label className="mb-1 block text-xs font-semibold">Width (mm)</label>
                        <input
                          type="number"
                          min={40}
                          max={1000}
                          value={wMm}
                          onChange={(e) => setWMm(e.target.value)}
                          className="w-full rounded-xl border border-black/15 px-3 py-2 text-sm dark:border-white/15 dark:bg-[#22242C]"
                        />
                      </div>
                      <div>
                        <label className="mb-1 block text-xs font-semibold">Height (mm)</label>
                        <input
                          type="number"
                          min={40}
                          max={1000}
                          value={hMm}
                          onChange={(e) => setHMm(e.target.value)}
                          className="w-full rounded-xl border border-black/15 px-3 py-2 text-sm dark:border-white/15 dark:bg-[#22242C]"
                        />
                      </div>
                    </div>
                  )}
                </div>
              )}

              {tab === 'cover' && (
                <div className="space-y-4">
                  <div className="flex items-center justify-between rounded-2xl border border-black/8 bg-black/[0.02] px-4 py-3 dark:border-white/10 dark:bg-white/[0.02]">
                    <div>
                      <div className="text-sm font-semibold">Use cover as first page</div>
                      <div className="text-xs text-slate-500">Write directly onto Page 1 cover</div>
                    </div>
                    <button
                      type="button"
                      onClick={() => setCoverPage((v) => !v)}
                      className={`relative h-6 w-11 rounded-full transition ${
                        coverPage ? 'bg-blue-600' : 'bg-slate-300 dark:bg-slate-700'
                      }`}
                    >
                      <span
                        className={`absolute top-0.5 h-5 w-5 rounded-full bg-white shadow transition-transform ${
                          coverPage ? 'left-[22px]' : 'left-0.5'
                        }`}
                      />
                    </button>
                  </div>

                  <div className="grid grid-cols-4 gap-3 sm:grid-cols-6">
                    {coverOptions.map((spec) => (
                      <button
                        key={spec}
                        type="button"
                        onClick={() => setCover(spec)}
                        className={`relative flex items-center justify-center rounded-xl p-1 transition ${
                          cover === spec
                            ? 'ring-2 ring-blue-600 ring-offset-2 dark:ring-offset-[#191A1F]'
                            : 'hover:scale-105'
                        }`}
                      >
                        <CoverCanvas spec={spec} width={64} height={86} />
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end gap-3 border-t border-black/8 px-6 py-4 dark:border-white/10">
          <button
            type="button"
            onClick={onClose}
            className="rounded-xl px-4 py-2 text-sm font-semibold text-slate-600 hover:bg-black/5 dark:text-slate-300 dark:hover:bg-white/10"
          >
            Cancel
          </button>
          <button
            type="submit"
            className="rounded-xl bg-blue-600 px-5 py-2 text-sm font-semibold text-white shadow-sm hover:bg-blue-700"
          >
            Create {mode === 'whiteboard' ? 'Whiteboard' : mode === 'mindmap' ? 'Mind Map' : 'Notebook'}
          </button>
        </div>
      </form>
    </div>
  );
};
