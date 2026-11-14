import React, { useEffect, useRef, useState } from 'react';
import { Check, Layers, Sparkles, X } from 'lucide-react';
import {
  ACCENT_COLORS,
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
import {
  LiquidButton,
  LiquidGlassSurface,
  LiquidInput,
  LiquidModal,
  LiquidSegmentedControl,
  LiquidToggle,
} from './LiquidGlassSurface';

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
      setCover('pattern:geometric:#2F6B4F');
    } else if (initialMode === 'mindmap') {
      setTitle('Concept Mind Map');
      setPreset('WHITEBOARD');
      setPaper('DOTTED');
      setCover('pattern:linen:#2E4A7A');
    } else {
      setTitle('Untitled notebook');
      setPreset(defaults.defaultPreset || 'A4');
      setPaper(defaults.defaultPaper || 'RULED');
      setCover('pattern:linen:#24483F');
    }
  }, [open, initialMode, defaults]);

  if (!open) return null;

  const accent = ACCENT_COLORS[defaults.accent];
  const isBoard = mode === 'whiteboard' || mode === 'mindmap' || preset === 'WHITEBOARD';
  const coverColor = cover.split(':').pop() || '#24483F';

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
      coverColor: coverColor.startsWith('#') ? coverColor : '#24483F',
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
    ...COVER_COLORS.map((c) => `pattern:linen:${c}`),
    ...COVER_COLORS.map((c) => `solid:${c}`),
    ...COVER_PATTERNS.filter((p) => p !== 'linen').flatMap((pat) => {
      const tones =
        pat === 'leather'
          ? ['#5D4037', '#1E2321', '#24483F', '#7A2E42']
          : pat === 'kraft'
            ? ['#C49A6C', '#A77B4F']
            : ['#24483F', '#2E4A7A', '#7A2E42', '#B98A2E'];
      return tones.map((c) => `pattern:${pat}:${c}`);
    }),
  ];

  const activePaperMeta = PAPER_STYLES.find((p) => p.id === paper);

  return (
    <LiquidModal open={open} onClose={onClose} maxWidthClass="max-w-4xl">
      <form onSubmit={handleSubmit} className="flex max-h-[90vh] flex-col">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-black/8 px-6 py-4 dark:border-white/10">
          <div>
            <h2 className="text-lg font-bold tracking-tight">Create workspace</h2>
            <p className="text-xs text-[#5A5F58] dark:text-[#B1AC9F]">
              Preview paper patterns, choose page size, or launch an unbounded whiteboard / mind map
            </p>
          </div>
          <LiquidButton onClick={onClose} className="!h-9 !w-9 !p-0">
            <X className="h-4.5 w-4.5" />
          </LiquidButton>
        </div>

        {/* Body: Left live preview + Right configuration */}
        <div className="grid min-h-0 flex-1 grid-cols-1 overflow-hidden md:grid-cols-12">
          {/* Left Live Preview Column */}
          <div className="flex flex-col items-center justify-between border-b border-black/8 bg-black/[0.03] p-5 dark:border-white/10 dark:bg-white/[0.03] md:col-span-4 md:border-b-0 md:border-r">
            <div className="w-full space-y-3">
              <LiquidSegmentedControl<'notebook' | 'whiteboard' | 'mindmap'>
                value={mode}
                accentColor={accent.primary}
                onChange={(next) => {
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
                options={[
                  { value: 'notebook', label: 'Notebook' },
                  { value: 'whiteboard', label: 'Board' },
                  { value: 'mindmap', label: 'Mind Map' },
                ]}
              />

              {/* Dual Live Preview: Cover + Paper Pattern */}
              <LiquidGlassSurface enabled={defaults.liquidGlass} variant="frost" radius={20} className="p-4">
                <div className="flex flex-col items-center gap-3">
                  <div className="flex items-end justify-center gap-3">
                    <div className="flex flex-col items-center gap-1">
                      <CoverCanvas spec={cover} title={title} width={88} height={118} />
                      <span className="text-[10px] font-medium text-[#5A5F58] dark:text-[#B1AC9F]">Cover</span>
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
                      <span className="text-[10px] font-medium text-[#5A5F58] dark:text-[#B1AC9F]">
                        {isBoard ? 'Infinite Canvas' : `${activePaperMeta?.label || paper}`}
                      </span>
                    </div>
                  </div>

                  <div className="w-full rounded-xl bg-black/[0.04] px-3 py-2 text-center text-xs dark:bg-white/[0.05]">
                    <span className="font-semibold">
                      {isBoard
                        ? 'Unbounded Infinite Board'
                        : `${PAGE_PRESETS.find((x) => x.id === preset)?.label || 'A4'} ${landscape ? 'Landscape' : 'Portrait'}`}
                    </span>
                  </div>
                </div>
              </LiquidGlassSurface>
            </div>

            <div className="mt-4 w-full">
              <label className="mb-1 block text-xs font-semibold text-[#5A5F58] dark:text-[#B1AC9F]">
                Notebook Title
              </label>
              <LiquidInput
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Enter notebook title..."
              />
            </div>
          </div>

          {/* Right Configuration Tabs */}
          <div className="flex min-h-0 flex-col md:col-span-8">
            <div className="border-b border-black/8 px-6 py-3 dark:border-white/10">
              <LiquidSegmentedControl<'paper' | 'size' | 'cover'>
                value={tab}
                onChange={setTab}
                accentColor={accent.primary}
                options={[
                  { value: 'paper', label: 'Paper & Pattern' },
                  { value: 'size', label: 'Page Size & Canvas' },
                  { value: 'cover', label: 'Cover Design' },
                ]}
              />
            </div>

            <div className="flex-1 overflow-y-auto p-6">
              {tab === 'paper' && (
                <div className="space-y-5">
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <span className="text-xs font-semibold uppercase tracking-wider text-[#5A5F58] dark:text-[#B1AC9F]">
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
                              ? 'scale-110 ring-2 ring-[#24483F] dark:ring-[#8CC1AC]'
                              : 'border-black/20 dark:border-white/25'
                          }`}
                          style={{ backgroundColor: tint.color }}
                        />
                      ))}
                    </div>
                  </div>

                  <LiquidSegmentedControl<string>
                    value={paperGroup}
                    onChange={setPaperGroup}
                    accentColor={accent.primary}
                    options={groups.map((g) => ({ value: g, label: g }))}
                  />

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
                              ? 'border-[#24483F] bg-[#24483F]/10 ring-2 ring-[#24483F]/25 dark:border-[#8CC1AC] dark:bg-[#8CC1AC]/12'
                              : 'border-black/10 hover:border-black/25 dark:border-white/10 dark:hover:border-white/25'
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
                              <span
                                className="absolute right-1.5 top-1.5 flex h-5 w-5 items-center justify-center rounded-full text-white shadow"
                                style={{ backgroundColor: accent.primary }}
                              >
                                <Check className="h-3 w-3" />
                              </span>
                            )}
                          </div>
                          <span className="mt-1.5 text-center text-[11px] font-medium leading-tight">
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
                              ? 'border-[#24483F] bg-[#24483F]/10 ring-2 ring-[#24483F]/20 dark:border-[#8CC1AC] dark:bg-[#8CC1AC]/12'
                              : 'border-black/10 hover:border-black/25 dark:border-white/10 dark:hover:border-white/25'
                          }`}
                        >
                          <div>
                            <div className="flex items-center gap-2">
                              <span className="text-sm font-bold">{p.label}</span>
                              {p.recommended && (
                                <span
                                  className="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-bold text-white"
                                  style={{ backgroundColor: accent.primary }}
                                >
                                  <Sparkles className="h-2.5 w-2.5" /> Recommended
                                </span>
                              )}
                              {p.isWhiteboard && (
                                <span className="inline-flex items-center gap-1 rounded-full bg-emerald-600/15 px-2 py-0.5 text-[10px] font-bold text-emerald-700 dark:text-emerald-300">
                                  <Layers className="h-2.5 w-2.5" /> Infinite
                                </span>
                              )}
                            </div>
                            <p className="mt-0.5 text-xs text-[#5A5F58] dark:text-[#B1AC9F]">{p.subtitle}</p>
                          </div>
                          {sel && <Check className="mt-0.5 h-4 w-4 shrink-0 text-[#24483F] dark:text-[#8CC1AC]" />}
                        </button>
                      );
                    })}
                  </div>

                  {!isBoard && (
                    <LiquidGlassSurface
                      enabled={defaults.liquidGlass}
                      variant="frost"
                      radius={18}
                      className="flex items-center justify-between px-4 py-3"
                    >
                      <div>
                        <div className="text-sm font-semibold">Landscape orientation</div>
                        <div className="text-xs text-[#5A5F58] dark:text-[#B1AC9F]">
                          Swaps width & height for horizontal writing
                        </div>
                      </div>
                      <LiquidToggle selected={landscape} onSelect={setLandscape} accentColor={accent.primary} />
                    </LiquidGlassSurface>
                  )}

                  {preset === 'CUSTOM' && (
                    <div className="grid grid-cols-2 gap-3">
                      <div>
                        <label className="mb-1 block text-xs font-semibold">Width (mm)</label>
                        <LiquidInput
                          type="number"
                          min={40}
                          max={1000}
                          value={wMm}
                          onChange={(e) => setWMm(e.target.value)}
                        />
                      </div>
                      <div>
                        <label className="mb-1 block text-xs font-semibold">Height (mm)</label>
                        <LiquidInput
                          type="number"
                          min={40}
                          max={1000}
                          value={hMm}
                          onChange={(e) => setHMm(e.target.value)}
                        />
                      </div>
                    </div>
                  )}
                </div>
              )}

              {tab === 'cover' && (
                <div className="space-y-4">
                  <LiquidGlassSurface
                    enabled={defaults.liquidGlass}
                    variant="frost"
                    radius={18}
                    className="flex items-center justify-between px-4 py-3"
                  >
                    <div>
                      <div className="text-sm font-semibold">Use cover as first page</div>
                      <div className="text-xs text-[#5A5F58] dark:text-[#B1AC9F]">
                        Write directly onto Page 1 cover
                      </div>
                    </div>
                    <LiquidToggle selected={coverPage} onSelect={setCoverPage} accentColor={accent.primary} />
                  </LiquidGlassSurface>

                  <div className="grid grid-cols-4 gap-3 sm:grid-cols-6">
                    {coverOptions.map((spec) => (
                      <button
                        key={spec}
                        type="button"
                        onClick={() => setCover(spec)}
                        className={`ios-stretch relative flex items-center justify-center rounded-xl p-1 transition ${
                          cover === spec
                            ? 'ring-2 ring-[#24483F] ring-offset-2 dark:ring-[#8CC1AC]'
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
          <LiquidButton onClick={onClose}>Cancel</LiquidButton>
          <button
            type="submit"
            className="inline-flex items-center justify-center rounded-full px-5 py-2 text-sm font-semibold text-[#F5F2E8] shadow-sm transition hover:brightness-110"
            style={{ backgroundColor: accent.primary }}
          >
            Create {mode === 'whiteboard' ? 'Whiteboard' : mode === 'mindmap' ? 'Mind Map' : 'Notebook'}
          </button>
        </div>
      </form>
    </LiquidModal>
  );
};
