import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  ArrowLeftRight,
  BookOpen,
  BookmarkMinus,
  Brain,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Columns2,
  Compass,
  Copy,
  Download,
  Eraser,
  ExternalLink,
  FileText,
  GitBranch,
  Hand,
  Highlighter,
  Home,
  Image as ImageIcon,
  Lasso as LassoIcon,
  Layers,
  Link2,
  ListChecks,
  Lock,
  Maximize2,
  Minimize2,
  MoreHorizontal,
  Move,
  Network,
  Palette,
  PanelLeft,
  Pen,
  Pin,
  PinOff,
  Play,
  Plus,
  Redo2,
  Search,
  Settings,
  Shapes,
  Sliders,
  Sticker,
  StickyNote,
  Timer,
  Trash2,
  Type,
  Undo2,
  Unlock,
  Upload,
  Users,
  Wand2,
  X,
  ZoomIn,
  ZoomOut,
} from 'lucide-react';
import {
  ACCENT_COLORS,
  AppSettings,
  CollabChatMessage,
  CollabItemDoc,
  CollabMember,
  CollabRoom,
  DEFAULT_TOOLBAR_ORDER,
  EraserMode,
  Flashcard,
  InkItem,
  LocalModelState,
  MindNodeItem,
  NotebookMeta,
  Page,
  PageItem,
  PAPER_STYLES,
  PAPER_TINTS,
  PenKind,
  PenPreset,
  QuizQuestion,
  ScribbleSensitivity,
  ScrollDirection,
  ShapeKind,
  SlideCard,
  StickyFont,
  StickyItem,
  StickyStyle,
  StrokePt,
  TAPE_PATTERNS,
  TextFont,
  TextItem,
  ToolId,
  WEBLINK_COLORS,
  WebLinkItem,
} from '../types/models';
import {
  clampMindNodesToPage,
  dist,
  getItemBounds,
  getStickyChecklistHitBoxes,
  isClosedLoop,
  isLiveScribble,
  isNearlyStraight,
  isScribble,
  lassoSelectsItem,
  pathLength,
  Pt,
  scribbleHitsItem,
  SHAPE_KINDS,
  ShapeBuilder,
  ShapeRecognizer,
  splitStrokeByEraser,
  StretchBox,
  stretchPageItem,
  toggleStickyChecklistLine,
} from '../utils/geometry';
import {
  drawPaperTemplate,
  drawWhiteboardBackdrop,
  getHiddenMindNodeIds,
  isColorDark,
  renderPageItems,
} from '../utils/canvasRenderer';
import { buildOutlineItemsFromMindNodes, parseImportedFiles } from '../utils/noteImporter';
import {
  extractNotebookText,
  generateMindMapFromText,
  RichSummaryResult,
  runLocalFlashcards,
  runLocalQuiz,
  runLocalSlideShow,
  runLocalSummary,
} from '../utils/localAi';
import { PaperPreviewCanvas } from './CoversAndPatterns';
import { FlashcardsModal, QuizModal, SlideShowModal, SummaryModal } from './StudyModals';
import {
  LiquidButton,
  LiquidGlassSurface,
  LiquidInput,
  LiquidMenuButton,
  LiquidModal,
  LiquidSegmentedControl,
  LiquidSlider,
} from './LiquidGlassSurface';
import {
  CollabRoomModal,
  CommandPaletteAction,
  CommandPaletteModal,
} from './CollabAndAiModals';

interface PuffParticle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  r: number;
  born: number;
  color: string;
}

interface EraseRing {
  x: number;
  y: number;
  maxR: number;
  born: number;
  color: string;
}

interface LaserTrail {
  pts: Pt[];
  born: number;
}

interface UndoEntry {
  pageIdx: number;
  items: PageItem[];
}

interface EditorScreenProps {
  notebook: NotebookMeta;
  allNotebooks: NotebookMeta[];
  openTabIds: string[];
  settings: AppSettings;
  flashcards: Flashcard[];
  bestQuizScore: number;
  modelStates: Record<string, LocalModelState>;
  isSignedIn?: boolean;
  activeCollabRoom?: CollabRoom | null;
  collabMembers?: CollabMember[];
  collabItems?: CollabItemDoc[];
  collabMessages?: CollabChatMessage[];
  onSignInGoogle?: () => Promise<void>;
  onCreateCollabRoom?: (title: string) => Promise<void>;
  onJoinCollabRoom?: (code: string) => Promise<void>;
  onLeaveCollabRoom?: () => void;
  onSendCollabMessage?: (text: string) => Promise<void>;
  onBroadcastCollabItem?: (item: PageItem, pageIdx: number) => void;
  onBroadcastCollabCursor?: (x: number, y: number, pageIdx: number) => void;
  onBackHome: () => void;
  onSwitchNote: (id: string) => void;
  onCloseTab: (id: string) => void;
  onUpdateNotebook: (id: string, updater: (nb: NotebookMeta) => NotebookMeta) => void;
  onUpdateSlides?: (slides: SlideCard[]) => void;
  onAppendPages?: (pages: Page[]) => void;
  onUpdateSettings: (patch: Partial<AppSettings>) => void;
  onSavePenPreset: (preset: PenPreset) => void;
  onDeletePenPreset: (id: string) => void;
  onAddFlashcards: (cards: Flashcard[]) => void;
  onUpdateFlashcard: (card: Flashcard) => void;
  onDeleteFlashcard: (id: string) => void;
  onRecordQuizScore: (notebookId: string, pct: number) => void;
  onOpenSettings: () => void;
  onOpenLocalModels: () => void;
}

const PEN_COLORS = ['#1E2321', '#2E4A7A', '#2F6B4F', '#7A2E42', '#B98A2E', '#5A625E', '#FFFFFF'];
const HL_COLORS = ['#F3E28C', '#B6DEC2', '#B4CDE6', '#E8B8C2', '#D2C4E8'];
const STICKY_COLORS = ['#FBF4D8', '#E2EFE4', '#DFE8F2', '#F5E1E4', '#EAE2F3', '#F7E8D4'];
const MIND_COLORS = ['#24483F', '#2E4A7A', '#B98A2E', '#7A2E42', '#2F6B4F', '#5C4B7D'];
const STICKERS = ['⭐', '💡', '📌', '🔥', '✅', '🧠', '🎯', '📐', '🧪', '⚡', '🏆', '❤️'];

const PEN_NIBS: { id: PenKind; label: string; desc: string }[] = [
  { id: 'ballpoint', label: 'Ballpoint', desc: 'Uniform crisp everyday line' },
  { id: 'fountain', label: 'Fountain', desc: 'Tapered calligraphic feel' },
  { id: 'brush', label: 'Brush', desc: 'Expressive thick-to-thin ink' },
  { id: 'fineliner', label: 'Fineliner', desc: 'Technical flat stroke' },
  { id: 'pencil', label: 'Pencil', desc: 'Textured graphite grain' },
];

// ================================================================
// Rewritten Writable, Zoom-Fixed & Clean Split Notes Workspace (v10)
// ================================================================
const SplitNotebookPane: React.FC<{
  splitNotebook: NotebookMeta;
  allNotebooks: NotebookMeta[];
  paperTintDefault: string;
  highlighterBehind: boolean;
  scrollDirection: ScrollDirection;
  primaryPageIdx: number;
  width: number;
  penColor: string;
  penSize: number;
  hlColor: string;
  hlSize: number;
  onChangeNotebook: (id: string) => void;
  onUpdateSplitNotebook: (id: string, updater: (nb: NotebookMeta) => NotebookMeta) => void;
  onSwapWithPrimary: (id: string) => void;
  onSetRatio: (ratio: number) => void;
  onCopyTextToPrimary: (text: string) => void;
  onInsertTextToActive: (text: string) => void;
  onInsertWebLink: (url: string, title: string) => void;
  onClose: () => void;
}> = ({
  splitNotebook,
  allNotebooks,
  paperTintDefault,
  highlighterBehind,
  scrollDirection,
  primaryPageIdx,
  width,
  penColor,
  penSize,
  hlColor,
  hlSize,
  onChangeNotebook,
  onUpdateSplitNotebook,
  onSwapWithPrimary,
  onSetRatio,
  onCopyTextToPrimary,
  onInsertTextToActive,
  onInsertWebLink,
  onClose,
}) => {
  const [splitPageIdx, setSplitPageIdx] = useState(0);
  const [syncPage, setSyncPage] = useState(false);
  const [splitTool, setSplitTool] = useState<'pan' | 'pen' | 'highlighter' | 'eraser'>('pan');
  const [splitInkColor, setSplitInkColor] = useState(penColor || '#24483F');
  const [viewport, setViewport] = useState<{ zoom: number; panX: number; panY: number }>({
    zoom: 0.5,
    panX: 16,
    panY: 16,
  });
  const [activeRatio, setActiveRatio] = useState<number>(0.62);
  const [splitSearchOpen, setSplitSearchOpen] = useState(false);
  const [splitSearchQuery, setSplitSearchQuery] = useState('');
  const [splitHitIdx, setSplitHitIdx] = useState(0);

  const viewportRef = useRef(viewport);
  viewportRef.current = viewport;

  const splitCanvasRef = useRef<HTMLCanvasElement>(null);
  const splitLiveCanvasRef = useRef<HTMLCanvasElement>(null);
  const splitLiveRafRef = useRef<number | null>(null);
  const splitWrapRef = useRef<HTMLDivElement>(null);
  const dragRef = useRef<{ active: boolean; lx: number; ly: number }>({ active: false, lx: 0, ly: 0 });
  const writeStrokeRef = useRef<{ active: boolean; pageIdx: number; pts: StrokePt[] }>({
    active: false,
    pageIdx: 0,
    pts: [],
  });
  const pointersRef = useRef<Map<number, { x: number; y: number }>>(new Map());
  const pinchRef = useRef<{
    initialDist: number;
    initialZoom: number;
    initialPan: { x: number; y: number };
    initialCenter: { x: number; y: number };
  } | null>(null);

  const PAGE_GAP = 32;
  const isVerticalMode = scrollDirection === 'VERTICAL';

  const pageLayout = useMemo(() => {
    const rects: { page: Page; x: number; y: number; w: number; h: number }[] = [];
    let cursor = 0;
    for (const pg of splitNotebook.pages) {
      if (isVerticalMode) {
        rects.push({ page: pg, x: 0, y: cursor, w: pg.width, h: pg.height });
        cursor += pg.height + PAGE_GAP;
      } else {
        rects.push({ page: pg, x: cursor, y: 0, w: pg.width, h: pg.height });
        cursor += pg.width + PAGE_GAP;
      }
    }
    return rects;
  }, [splitNotebook.pages, isVerticalMode]);

  const sSafeIdx = Math.min(splitPageIdx, Math.max(0, splitNotebook.pages.length - 1));
  const spPage = splitNotebook.pages[sSafeIdx];

  const jumpToSplitPage = useCallback(
    (idx: number, customZoom?: number) => {
      const clamped = Math.max(0, Math.min(splitNotebook.pages.length - 1, idx));
      setSplitPageIdx(clamped);
      const wrap = splitWrapRef.current;
      const entry = pageLayout[clamped];
      if (!wrap || !entry) return;
      const z =
        customZoom ??
        Math.max(
          0.25,
          Math.min(
            (wrap.clientWidth - 28) / entry.w,
            (wrap.clientHeight - 28) / entry.h,
            0.95
          )
        );
      setViewport({
        zoom: z,
        panX: (wrap.clientWidth - entry.w * z) / 2 - entry.x * z,
        panY: Math.max(12, (wrap.clientHeight - entry.h * z) / 2) - entry.y * z,
      });
    },
    [splitNotebook.pages.length, pageLayout]
  );

  const zoomAtCenter = useCallback((targetZoom: number, ox?: number, oy?: number) => {
    const el = splitWrapRef.current;
    const cx = ox ?? (el ? el.clientWidth / 2 : 180);
    const cy = oy ?? (el ? el.clientHeight / 2 : 240);
    setViewport((prev) => {
      const nextZoom = Math.max(0.25, Math.min(3.5, targetZoom));
      const scaleRatio = nextZoom / prev.zoom;
      return {
        zoom: nextZoom,
        panX: cx - (cx - prev.panX) * scaleRatio,
        panY: cy - (cy - prev.panY) * scaleRatio,
      };
    });
  }, []);

  useEffect(() => {
    setSplitPageIdx(0);
    setSplitSearchQuery('');
    setSplitHitIdx(0);
  }, [splitNotebook.id]);

  useEffect(() => {
    if (!syncPage) return;
    const target = Math.min(primaryPageIdx, Math.max(0, splitNotebook.pages.length - 1));
    jumpToSplitPage(target, viewportRef.current.zoom);
  }, [syncPage, primaryPageIdx, splitNotebook.pages.length, jumpToSplitPage]);

  useEffect(() => {
    jumpToSplitPage(sSafeIdx);
  }, [splitNotebook.id, width, scrollDirection]);

  useEffect(() => {
    const wrap = splitWrapRef.current;
    if (!wrap || pageLayout.length <= 1) return;
    const cxWorld = (wrap.clientWidth / 2 - viewport.panX) / viewport.zoom;
    const cyWorld = (wrap.clientHeight / 2 - viewport.panY) / viewport.zoom;
    let bestIdx = sSafeIdx;
    let bestDist = Infinity;
    pageLayout.forEach((r, idx) => {
      const rx = r.x + r.w / 2;
      const ry = r.y + r.h / 2;
      const d = isVerticalMode ? Math.abs(cyWorld - ry) : Math.abs(cxWorld - rx);
      if (d < bestDist) {
        bestDist = d;
        bestIdx = idx;
      }
    });
    if (bestIdx !== sSafeIdx) setSplitPageIdx(bestIdx);
  }, [viewport.panX, viewport.panY, viewport.zoom, pageLayout, isVerticalMode, sSafeIdx]);

  useEffect(() => {
    const canvas = splitCanvasRef.current;
    if (!canvas) return;
    const onWheel = (e: WheelEvent) => {
      e.preventDefault();
      e.stopPropagation();
      const wrap = splitWrapRef.current;
      if (e.ctrlKey || e.metaKey) {
        const rect = wrap?.getBoundingClientRect();
        const ox = rect ? e.clientX - rect.left : e.clientX;
        const oy = rect ? e.clientY - rect.top : e.clientY;
        const factor = e.deltaY < 0 ? 1.09 : 0.91;
        setViewport((prev) => {
          const nextZoom = Math.max(0.25, Math.min(3.5, prev.zoom * factor));
          const ratio = nextZoom / prev.zoom;
          return {
            zoom: nextZoom,
            panX: ox - (ox - prev.panX) * ratio,
            panY: oy - (oy - prev.panY) * ratio,
          };
        });
      } else {
        setViewport((prev) => ({
          ...prev,
          panX: isVerticalMode ? prev.panX - e.deltaX : prev.panX - (e.deltaX || e.deltaY),
          panY: isVerticalMode ? prev.panY - e.deltaY : prev.panY - (e.deltaX ? e.deltaY : 0),
        }));
      }
    };
    canvas.addEventListener('wheel', onWheel, { passive: false });
    return () => canvas.removeEventListener('wheel', onWheel);
  }, [isVerticalMode]);

  useEffect(() => {
    const canvas = splitCanvasRef.current;
    const wrap = splitWrapRef.current;
    if (!canvas || !wrap || pageLayout.length === 0) return;
    const dpr = window.devicePixelRatio || 1;
    const cw = wrap.clientWidth;
    const ch = wrap.clientHeight;
    if (cw <= 0 || ch <= 0) return;
    canvas.width = Math.round(cw * dpr);
    canvas.height = Math.round(ch * dpr);
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    ctx.save();
    ctx.scale(dpr, dpr);
    const baseBg = spPage?.paperColor || paperTintDefault || '#FFFFFF';
    if (spPage?.isWhiteboard) {
      drawWhiteboardBackdrop(ctx, cw, ch, viewport.panX, viewport.panY, viewport.zoom, baseBg);
    } else {
      ctx.fillStyle = isColorDark(baseBg) ? '#14151A' : '#E5E7EE';
      ctx.fillRect(0, 0, cw, ch);
    }

    ctx.save();
    ctx.translate(viewport.panX, viewport.panY);
    ctx.scale(viewport.zoom, viewport.zoom);

    const viewMinX = -viewport.panX / viewport.zoom - 60;
    const viewMaxX = (cw - viewport.panX) / viewport.zoom + 60;
    const viewMinY = -viewport.panY / viewport.zoom - 60;
    const viewMaxY = (ch - viewport.panY) / viewport.zoom + 60;

    pageLayout.forEach((slot, idx) => {
      if (
        slot.x + slot.w < viewMinX ||
        slot.x > viewMaxX ||
        slot.y + slot.h < viewMinY ||
        slot.y > viewMaxY
      ) {
        return;
      }
      ctx.save();
      ctx.translate(slot.x, slot.y);
      const pg = slot.page;
      const pBg = pg.paperColor || paperTintDefault || '#FFFFFF';
      if (!pg.isWhiteboard) {
        ctx.save();
        ctx.shadowColor = 'rgba(0,0,0,0.12)';
        ctx.shadowBlur = 16;
        ctx.shadowOffsetY = 4;
        ctx.fillStyle = pBg;
        ctx.fillRect(0, 0, pg.width, pg.height);
        ctx.restore();
        drawPaperTemplate(ctx, pg.width, pg.height, pg.paper, pBg, pg.lineScale ?? 1, pg.lineColor, false, pg.cover);
      }
      renderPageItems(ctx, pg, new Map(), new Set(), highlighterBehind);
      ctx.restore();
    });

    ctx.restore();
    ctx.restore();
  }, [pageLayout, spPage, paperTintDefault, highlighterBehind, viewport]);

  const scheduleSplitLiveOverlay = useCallback(() => {
    if (splitLiveRafRef.current !== null) return;
    splitLiveRafRef.current = requestAnimationFrame(() => {
      splitLiveRafRef.current = null;
      const overlay = splitLiveCanvasRef.current;
      const wrap = splitWrapRef.current;
      if (!overlay || !wrap) return;
      const dpr = window.devicePixelRatio || 1;
      const cw = wrap.clientWidth;
      const ch = wrap.clientHeight;
      if (cw <= 0 || ch <= 0) return;
      if (overlay.width !== Math.round(cw * dpr) || overlay.height !== Math.round(ch * dpr)) {
        overlay.width = Math.round(cw * dpr);
        overlay.height = Math.round(ch * dpr);
      }
      const octx = overlay.getContext('2d');
      if (!octx) return;
      octx.clearRect(0, 0, overlay.width, overlay.height);
      const st = writeStrokeRef.current;
      if (!st.active || st.pts.length === 0) return;
      const slot = pageLayout[st.pageIdx];
      if (!slot) return;
      const vp = viewportRef.current;
      octx.save();
      octx.scale(dpr, dpr);
      octx.translate(vp.panX, vp.panY);
      octx.scale(vp.zoom, vp.zoom);
      octx.translate(slot.x, slot.y);
      const liveInk: InkItem = {
        id: 'split-live',
        kind: 'ink',
        points: st.pts,
        color: splitTool === 'highlighter' ? hlColor : splitInkColor,
        size: splitTool === 'highlighter' ? hlSize : penSize,
        isHighlighter: splitTool === 'highlighter',
        opacity: splitTool === 'highlighter' ? 0.55 : 1,
        penKind: 'fountain',
      };
      renderPageItems(octx, { ...slot.page, items: [liveInk] }, new Map(), new Set(), highlighterBehind);
      octx.restore();
    });
  }, [pageLayout, splitTool, hlColor, splitInkColor, hlSize, penSize, highlighterBehind]);

  useEffect(
    () => () => {
      if (splitLiveRafRef.current !== null) cancelAnimationFrame(splitLiveRafRef.current);
    },
    []
  );

  const splitSearchHits = useMemo(() => {
    const q = splitSearchQuery.trim().toLowerCase();
    if (!q) return [];
    const hits: { pageIdx: number; preview: string }[] = [];
    splitNotebook.pages.forEach((pg, idx) => {
      for (const it of pg.items) {
        const txt =
          it.kind === 'text'
            ? it.text
            : it.kind === 'sticky'
              ? `${it.title || ''} ${it.text}`
              : it.kind === 'weblink'
                ? `${it.title} ${it.url}`
                : it.kind === 'mindnode'
                  ? `${it.label} ${it.notes || ''}`
                  : '';
        if (txt.toLowerCase().includes(q)) {
          hits.push({ pageIdx: idx, preview: txt.trim().slice(0, 64) });
        }
      }
      if (pg.pdfText && pg.pdfText.toLowerCase().includes(q)) {
        hits.push({ pageIdx: idx, preview: pg.pdfText.slice(0, 64) });
      }
    });
    return hits;
  }, [splitNotebook.pages, splitSearchQuery]);

  const pageText = useMemo(() => {
    if (!spPage) return '';
    return [
      spPage.pdfText || '',
      ...spPage.items.map((it) =>
        it.kind === 'text'
          ? it.text
          : it.kind === 'sticky'
            ? `${it.title ? `${it.title}\n` : ''}${it.text}`
            : it.kind === 'weblink'
              ? `${it.title} (${it.url})`
              : it.kind === 'mindnode'
                ? `${it.label}${it.notes ? `: ${it.notes}` : ''}`
                : ''
      ),
    ]
      .filter(Boolean)
      .join('\n');
  }, [spPage]);

  const eraseInSplit = (sx: number, sy: number) => {
    const rect = splitCanvasRef.current?.getBoundingClientRect();
    if (!rect) return;
    const wx = (sx - rect.left - viewport.panX) / viewport.zoom;
    const wy = (sy - rect.top - viewport.panY) / viewport.zoom;
    const slotIdx = pageLayout.findIndex(
      (r) => wx >= r.x && wx <= r.x + r.w && wy >= r.y && wy <= r.y + r.h
    );
    const targetIdx = slotIdx >= 0 ? slotIdx : sSafeIdx;
    const slot = pageLayout[targetIdx];
    if (!slot) return;
    const localPt = { x: wx - slot.x, y: wy - slot.y };
    const r = 18;
    const hit = slot.page.items.some(
      (it) => it.kind === 'ink' && it.points.some((p) => dist(p, localPt) <= r + it.size / 2)
    );
    if (!hit) return;
    onUpdateSplitNotebook(splitNotebook.id, (nb) => ({
      ...nb,
      pages: nb.pages.map((pg, i) =>
        i === targetIdx
          ? {
              ...pg,
              items: pg.items.filter(
                (it) => !(it.kind === 'ink' && it.points.some((p) => dist(p, localPt) <= r + it.size / 2))
              ),
            }
          : pg
      ),
    }));
  };

  return (
    <aside
      style={{ width }}
      className="liquid-glass-frost hidden shrink-0 flex-col border-l border-[#1F2421]/10 dark:border-[#EDE8DB]/10 md:flex"
    >
      {/* Clean Single Top Header + LiquidMenuButton Dropdown */}
      <LiquidGlassSurface
        enabled
        variant="chrome"
        radius={0}
        className="relative z-20 border-b border-[#1F2421]/10 px-3 py-2 dark:border-[#EDE8DB]/10"
      >
        <div className="flex items-center justify-between gap-1.5">
          <LiquidMenuButton
            align="left"
            label={splitNotebook.title}
            className="min-w-0 flex-1"
            items={allNotebooks
              .filter((n) => n.deletedAt === null)
              .map((n) => ({
                id: n.id,
                label: n.title,
                selected: n.id === splitNotebook.id,
                onSelect: () => onChangeNotebook(n.id),
              }))}
          />

          <LiquidButton
            tint={splitTool !== 'pan' ? '#24483F' : undefined}
            onClick={() => setSplitTool((t) => (t === 'pan' ? 'pen' : 'pan'))}
            title={splitTool === 'pan' ? 'Switch to Write Ink in Split' : 'Switch to Read/Pan'}
            className="!px-2.5 !py-1 !text-[11px]"
          >
            {splitTool === 'pan' ? 'Read' : 'Write'}
          </LiquidButton>

          <LiquidButton
            tint={splitSearchOpen ? '#24483F' : undefined}
            onClick={() => setSplitSearchOpen((v) => !v)}
            title="Search in split notebook"
            className="!h-8 !w-8 !p-0"
          >
            <Search className="h-3.5 w-3.5" />
          </LiquidButton>

          <LiquidMenuButton
            align="right"
            title="Split options"
            trigger={() => (
              <span className="liquid-glass-frost flex h-8 w-8 items-center justify-center rounded-full">
                <MoreHorizontal className="h-3.5 w-3.5" />
              </span>
            )}
            items={[
              ...[0.3, 0.5, 0.7].map((r) => ({
                id: `ratio-${r}`,
                label: `Width ${Math.round(r * 100)} / ${Math.round((1 - r) * 100)}`,
                selected: Math.abs(activeRatio - r) < 0.05,
                onSelect: () => {
                  setActiveRatio(r);
                  onSetRatio(r);
                },
              })),
              {
                id: 'sync-page',
                dividerBefore: true,
                label: syncPage ? 'Unlock Page Sync' : 'Lock Page Sync',
                icon: syncPage ? <Lock className="h-3.5 w-3.5" /> : <Unlock className="h-3.5 w-3.5" />,
                onSelect: () => setSyncPage((s) => !s),
              },
              {
                id: 'swap-primary',
                label: 'Swap Primary & Split',
                icon: <ArrowLeftRight className="h-3.5 w-3.5" />,
                onSelect: () => onSwapWithPrimary(splitNotebook.id),
              },
              ...(pageText
                ? [
                    {
                      id: 'clip-sticky',
                      label: 'Clip Text as Sticky',
                      icon: <Copy className="h-3.5 w-3.5" />,
                      onSelect: () => onCopyTextToPrimary(pageText),
                    },
                    {
                      id: 'insert-text',
                      label: 'Insert Text to Active',
                      icon: <FileText className="h-3.5 w-3.5" />,
                      onSelect: () => onInsertTextToActive(pageText),
                    },
                  ]
                : []),
              {
                id: 'cross-link',
                label: 'Insert Cross-Link Card',
                icon: <Link2 className="h-3.5 w-3.5" />,
                onSelect: () =>
                  onInsertWebLink(
                    `https://pronotes.app/notebook/${splitNotebook.id}?p=${sSafeIdx + 1}`,
                    `${splitNotebook.title} (p.${sSafeIdx + 1})`
                  ),
              },
            ]}
          />

          <LiquidButton
            onClick={onClose}
            title="Close split pane"
            className="!h-8 !w-8 !p-0"
          >
            <X className="h-3.5 w-3.5" />
          </LiquidButton>
        </div>
      </LiquidGlassSurface>

      {/* Collapsible Write Tools Bar */}
      {splitTool !== 'pan' && (
        <LiquidGlassSurface
          enabled
          variant="frost"
          radius={0}
          className="flex items-center justify-between gap-2 border-b border-[#1F2421]/10 px-3 py-1.5 text-[11px] dark:border-[#EDE8DB]/10"
        >
          <LiquidSegmentedControl
            value={splitTool}
            onChange={(v) => setSplitTool(v)}
            options={[
              { value: 'pen', label: 'Pen' },
              { value: 'highlighter', label: 'Hi-lite' },
              { value: 'eraser', label: 'Erase' },
            ]}
          />
          <div className="flex items-center gap-1">
            {['#1F2421', '#24483F', '#2E4A7A', '#9B3B3B'].map((c) => (
              <button
                key={c}
                type="button"
                onClick={() => {
                  setSplitInkColor(c);
                  if (splitTool === 'eraser') setSplitTool('pen');
                }}
                className={`h-4 w-4 rounded-full border border-black/20 ${
                  splitInkColor === c ? 'ring-2 ring-[#24483F] dark:ring-[#8CC1AC]' : ''
                }`}
                style={{ backgroundColor: c }}
              />
            ))}
            <LiquidButton
              onClick={() => {
                onUpdateSplitNotebook(splitNotebook.id, (nb) => ({
                  ...nb,
                  pages: nb.pages.map((pg, i) =>
                    i === sSafeIdx ? { ...pg, items: pg.items.slice(0, -1) } : pg
                  ),
                }));
              }}
              title="Undo last split stroke"
              className="ml-1 !h-7 !w-7 !p-0"
            >
              <Undo2 className="h-3.5 w-3.5" />
            </LiquidButton>
          </div>
        </LiquidGlassSurface>
      )}

      {/* Collapsible In-Split Search Bar */}
      {splitSearchOpen && (
        <div className="flex items-center gap-1.5 border-b border-[#1F2421]/10 px-3 py-1.5 text-xs dark:border-[#EDE8DB]/10">
          <LiquidInput
            autoFocus
            icon={<Search className="h-3.5 w-3.5" />}
            value={splitSearchQuery}
            onChange={(e) => {
              setSplitSearchQuery(e.target.value);
              setSplitHitIdx(0);
            }}
            placeholder="Find in reference..."
            wrapperClassName="flex-1 !py-1"
          />
          {splitSearchQuery && (
            <span className="font-mono-num shrink-0 text-[10px] font-semibold text-[#5A5F58] dark:text-[#B1AC9F]">
              {splitSearchHits.length > 0 ? `${Math.min(splitHitIdx + 1, splitSearchHits.length)}/${splitSearchHits.length}` : '0'}
            </span>
          )}
          <LiquidButton
            disabled={splitSearchHits.length === 0}
            onClick={() => {
              if (splitSearchHits.length === 0) return;
              const next = (splitHitIdx - 1 + splitSearchHits.length) % splitSearchHits.length;
              setSplitHitIdx(next);
              jumpToSplitPage(splitSearchHits[next].pageIdx, viewport.zoom);
            }}
            className="!h-7 !w-7 !p-0"
          >
            <ChevronLeft className="h-3.5 w-3.5" />
          </LiquidButton>
          <LiquidButton
            disabled={splitSearchHits.length === 0}
            onClick={() => {
              if (splitSearchHits.length === 0) return;
              const next = (splitHitIdx + 1) % splitSearchHits.length;
              setSplitHitIdx(next);
              jumpToSplitPage(splitSearchHits[next].pageIdx, viewport.zoom);
            }}
            className="!h-7 !w-7 !p-0"
          >
            <ChevronRight className="h-3.5 w-3.5" />
          </LiquidButton>
        </div>
      )}

      {/* Compact Page & Zoom Strip (with fixed 25% min zoom & 50%/Fit/100%/150% presets) */}
      <div className="flex items-center justify-between gap-1 border-b border-black/6 bg-slate-50/70 px-3 py-1 text-[11px] dark:border-white/8 dark:bg-[#14151A]">
        <div className="flex items-center gap-1">
          <button
            type="button"
            disabled={sSafeIdx === 0}
            onClick={() => jumpToSplitPage(sSafeIdx - 1, viewport.zoom)}
            className="rounded p-0.5 hover:bg-black/5 disabled:opacity-30"
          >
            <ChevronLeft className="h-3.5 w-3.5" />
          </button>
          <span className="font-mono-num text-[10px] font-bold text-slate-600 dark:text-slate-300">
            p.{sSafeIdx + 1}/{splitNotebook.pages.length}
          </span>
          <button
            type="button"
            disabled={sSafeIdx >= splitNotebook.pages.length - 1}
            onClick={() => jumpToSplitPage(sSafeIdx + 1, viewport.zoom)}
            className="rounded p-0.5 hover:bg-black/5 disabled:opacity-30"
          >
            <ChevronRight className="h-3.5 w-3.5" />
          </button>
        </div>

        <div className="flex items-center gap-0.5">
          <button
            type="button"
            onClick={() => zoomAtCenter(viewport.zoom * 0.8)}
            className="rounded p-1 hover:bg-black/5 dark:hover:bg-white/10"
            title="Zoom out"
          >
            <ZoomOut className="h-3.5 w-3.5" />
          </button>
          <span className="font-mono-num min-w-[34px] text-center text-[10px] font-semibold">
            {Math.round(viewport.zoom * 100)}%
          </span>
          <button
            type="button"
            onClick={() => zoomAtCenter(viewport.zoom * 1.2)}
            className="rounded p-1 hover:bg-black/5 dark:hover:bg-white/10"
            title="Zoom in"
          >
            <ZoomIn className="h-3.5 w-3.5" />
          </button>
          <button
            type="button"
            onClick={() => zoomAtCenter(0.5)}
            className="rounded px-1.5 py-0.5 font-mono-num text-[10px] font-semibold hover:bg-black/5 dark:hover:bg-white/10"
          >
            50%
          </button>
          <button
            type="button"
            onClick={() => jumpToSplitPage(sSafeIdx)}
            className="rounded px-1.5 py-0.5 text-[10px] font-bold text-blue-600 hover:bg-blue-500/10 dark:text-blue-400"
          >
            Fit
          </button>
          <button
            type="button"
            onClick={() => zoomAtCenter(1.0)}
            className="rounded px-1.5 py-0.5 font-mono-num text-[10px] font-semibold hover:bg-black/5 dark:hover:bg-white/10"
          >
            100%
          </button>
        </div>
      </div>

      {/* Interactive Pan/Pinch-Zoom + Ink Writable Canvas */}
      <div ref={splitWrapRef} className="relative min-h-0 flex-1 overflow-hidden">
        <canvas
          ref={splitCanvasRef}
          style={{ touchAction: 'none' }}
          onPointerDown={(e) => {
            e.currentTarget.setPointerCapture(e.pointerId);
            pointersRef.current.set(e.pointerId, { x: e.clientX, y: e.clientY });

            if (pointersRef.current.size === 1 && splitTool !== 'pan') {
              if (splitTool === 'eraser') {
                eraseInSplit(e.clientX, e.clientY);
                dragRef.current = { active: true, lx: e.clientX, ly: e.clientY };
                return;
              }
              const rect = e.currentTarget.getBoundingClientRect();
              const wx = (e.clientX - rect.left - viewport.panX) / viewport.zoom;
              const wy = (e.clientY - rect.top - viewport.panY) / viewport.zoom;
              const slotIdx = pageLayout.findIndex(
                (r) => wx >= r.x && wx <= r.x + r.w && wy >= r.y && wy <= r.y + r.h
              );
              const targetIdx = slotIdx >= 0 ? slotIdx : sSafeIdx;
              const slot = pageLayout[targetIdx];
              if (slot) {
                writeStrokeRef.current = {
                  active: true,
                  pageIdx: targetIdx,
                  pts: [{ x: wx - slot.x, y: wy - slot.y, p: e.pressure || 0.65 }],
                };
                scheduleSplitLiveOverlay();
                return;
              }
            }

            if (pointersRef.current.size === 1) {
              dragRef.current = { active: true, lx: e.clientX, ly: e.clientY };
            } else if (pointersRef.current.size >= 2) {
              writeStrokeRef.current.active = false;
              dragRef.current.active = false;
              const pts = Array.from(pointersRef.current.values());
              const d = Math.hypot(pts[0].x - pts[1].x, pts[0].y - pts[1].y);
              const cx = (pts[0].x + pts[1].x) / 2;
              const cy = (pts[0].y + pts[1].y) / 2;
              const cur = viewportRef.current;
              pinchRef.current = {
                initialDist: Math.max(10, d),
                initialZoom: cur.zoom,
                initialPan: { x: cur.panX, y: cur.panY },
                initialCenter: { x: cx, y: cy },
              };
            }
          }}
          onPointerMove={(e) => {
            if (!pointersRef.current.has(e.pointerId)) return;
            pointersRef.current.set(e.pointerId, { x: e.clientX, y: e.clientY });

            if (pointersRef.current.size === 1 && writeStrokeRef.current.active) {
              const rect = e.currentTarget.getBoundingClientRect();
              const wx = (e.clientX - rect.left - viewport.panX) / viewport.zoom;
              const wy = (e.clientY - rect.top - viewport.panY) / viewport.zoom;
              const slot = pageLayout[writeStrokeRef.current.pageIdx];
              if (slot) {
                writeStrokeRef.current.pts.push({
                  x: wx - slot.x,
                  y: wy - slot.y,
                  p: e.pressure || 0.65,
                });
                scheduleSplitLiveOverlay();
              }
              return;
            }

            if (pointersRef.current.size === 1 && splitTool === 'eraser' && dragRef.current.active) {
              eraseInSplit(e.clientX, e.clientY);
              return;
            }

            if (pointersRef.current.size >= 2 && pinchRef.current) {
              const pts = Array.from(pointersRef.current.values());
              const currentDist = Math.hypot(pts[0].x - pts[1].x, pts[0].y - pts[1].y);
              const curCenterX = (pts[0].x + pts[1].x) / 2;
              const curCenterY = (pts[0].y + pts[1].y) / 2;
              if (pinchRef.current.initialDist > 8) {
                const factor = currentDist / pinchRef.current.initialDist;
                const newZoom = Math.max(0.25, Math.min(3.5, pinchRef.current.initialZoom * factor));
                const rect = splitWrapRef.current?.getBoundingClientRect();
                const initOx = rect ? pinchRef.current.initialCenter.x - rect.left : pinchRef.current.initialCenter.x;
                const initOy = rect ? pinchRef.current.initialCenter.y - rect.top : pinchRef.current.initialCenter.y;
                const curOx = rect ? curCenterX - rect.left : curCenterX;
                const curOy = rect ? curCenterY - rect.top : curCenterY;
                const scaleRatio = newZoom / pinchRef.current.initialZoom;
                const newPanX = curOx - (initOx - pinchRef.current.initialPan.x) * scaleRatio;
                const newPanY = curOy - (initOy - pinchRef.current.initialPan.y) * scaleRatio;
                setViewport({ zoom: newZoom, panX: newPanX, panY: newPanY });
              }
            } else if (pointersRef.current.size === 1 && dragRef.current.active) {
              const dx = e.clientX - dragRef.current.lx;
              const dy = e.clientY - dragRef.current.ly;
              dragRef.current.lx = e.clientX;
              dragRef.current.ly = e.clientY;
              setViewport((p) => ({ ...p, panX: p.panX + dx, panY: p.panY + dy }));
            }
          }}
          onPointerUp={(e) => {
            pointersRef.current.delete(e.pointerId);
            if (writeStrokeRef.current.active) {
              const { pageIdx: pIdx, pts } = writeStrokeRef.current;
              writeStrokeRef.current = { active: false, pageIdx: pIdx, pts: [] };
              if (pts.length > 0) {
                const ink: InkItem = {
                  id: `spink-${Date.now()}-${Math.floor(Math.random() * 999)}`,
                  kind: 'ink',
                  points: pts,
                  color: splitTool === 'highlighter' ? hlColor : splitInkColor,
                  size: splitTool === 'highlighter' ? hlSize : penSize,
                  isHighlighter: splitTool === 'highlighter',
                  opacity: splitTool === 'highlighter' ? 0.55 : 1,
                  penKind: 'fountain',
                };
                onUpdateSplitNotebook(splitNotebook.id, (nb) => ({
                  ...nb,
                  pages: nb.pages.map((pg, i) => (i === pIdx ? { ...pg, items: [...pg.items, ink] } : pg)),
                }));
              }
              scheduleSplitLiveOverlay();
            }
            if (pointersRef.current.size < 2) pinchRef.current = null;
            if (pointersRef.current.size === 1) {
              const remaining = Array.from(pointersRef.current.values())[0];
              dragRef.current = { active: true, lx: remaining.x, ly: remaining.y };
            } else {
              dragRef.current.active = false;
            }
          }}
          onPointerCancel={(e) => {
            pointersRef.current.delete(e.pointerId);
            writeStrokeRef.current.active = false;
            scheduleSplitLiveOverlay();
            if (pointersRef.current.size < 2) pinchRef.current = null;
            dragRef.current.active = false;
          }}
          onDoubleClick={(e) => {
            if (splitTool !== 'pan') return;
            const el = splitWrapRef.current;
            if (!el || !spPage) return;
            if (viewport.zoom > 1.1) {
              jumpToSplitPage(sSafeIdx);
            } else {
              const rect = el.getBoundingClientRect();
              zoomAtCenter(1.6, e.clientX - rect.left, e.clientY - rect.top);
            }
          }}
          className={`h-full w-full touch-none select-none ${
            splitTool === 'pan' ? 'cursor-grab active:cursor-grabbing' : 'cursor-crosshair'
          }`}
        />
        <canvas
          ref={splitLiveCanvasRef}
          className="pointer-events-none absolute inset-0 h-full w-full"
        />
      </div>
    </aside>
  );
};

// ================================================================
// Main Editor Screen
// ================================================================

export const EditorScreen: React.FC<EditorScreenProps> = ({
  notebook,
  allNotebooks,
  openTabIds,
  settings,
  flashcards,
  bestQuizScore,
  modelStates: _modelStates,
  isSignedIn = false,
  activeCollabRoom = null,
  collabMembers = [],
  collabItems = [],
  collabMessages = [],
  onSignInGoogle,
  onCreateCollabRoom,
  onJoinCollabRoom,
  onLeaveCollabRoom,
  onSendCollabMessage,
  onBroadcastCollabItem,
  onBroadcastCollabCursor,
  onBackHome,
  onSwitchNote,
  onCloseTab,
  onUpdateNotebook,
  onUpdateSlides,
  onAppendPages,
  onUpdateSettings,
  onSavePenPreset,
  onDeletePenPreset,
  onAddFlashcards,
  onUpdateFlashcard,
  onDeleteFlashcard,
  onRecordQuizScore,
  onOpenSettings,
  onOpenLocalModels: _onOpenLocalModels,
}) => {
  const accent = ACCENT_COLORS[settings.accent];

  const [pageIdx, setPageIdx] = useState(0);
  const [tool, setTool] = useState<ToolId>(notebook.isMindMap ? 'MINDMAP' : 'PEN');

  // Pen & Tool Local State synced with settings
  const [penKind, setPenKind] = useState<PenKind>(settings.penKind);
  const [penColor, setPenColor] = useState(settings.penColor);
  const [penSize, setPenSize] = useState(settings.penSize);
  const [penOpacity, setPenOpacity] = useState(settings.penOpacity);
  const [penStabilizer, setPenStabilizer] = useState(settings.penStabilizer);
  const [penStudioOpen, setPenStudioOpen] = useState(false);
  const [newPresetName, setNewPresetName] = useState('');

  const [hlColor, setHlColor] = useState('#FFE066');
  const [hlSize, setHlSize] = useState(22);
  const [eraserSize, setEraserSize] = useState(26);
  const [eraserMode, setEraserMode] = useState<EraserMode>(settings.eraserMode);
  const [shapeKind, setShapeKind] = useState<ShapeKind>('AUTO');
  const [tapeColor, setTapeColor] = useState('#7161E0');
  const [tapePattern, setTapePattern] = useState('stripes');
  const [stickyColor, setStickyColor] = useState(STICKY_COLORS[0]);
  const [textFont, setTextFont] = useState<TextFont>('SANS');
  const [textSize, setTextSize] = useState(26);

  // Selection & Inline Studio Editors (No browser prompts!)
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [editingTextId, setEditingTextId] = useState<string | null>(null);
  const [textDraft, setTextDraft] = useState('');
  const [editingStickyId, setEditingStickyId] = useState<string | null>(null);
  const [editingMindId, setEditingMindId] = useState<string | null>(null);
  const [mindDraft, setMindDraft] = useState('');
  const [mindTagDraft, setMindTagDraft] = useState('');
  const [mindNotesDraft, setMindNotesDraft] = useState('');
  const [mindShapeDraft, setMindShapeDraft] = useState<'pill' | 'card' | 'diamond'>('card');
  const [mindColorDraft, setMindColorDraft] = useState(MIND_COLORS[0]);
  const [moveMindSubtree, setMoveMindSubtree] = useState(true);

  // Lightweight per-page undo/redo
  const [undoStack, setUndoStack] = useState<UndoEntry[]>([]);
  const [redoStack, setRedoStack] = useState<UndoEntry[]>([]);

  // Drawers & Modals
  const [pagesOpen, setPagesOpen] = useState(false);
  const [templateSheetOpen, setTemplateSheetOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [stickerOpen, setStickerOpen] = useState(false);
  const [hoveredToolId, setHoveredToolId] = useState<ToolId | null>(null);
  const [dragToolId, setDragToolId] = useState<ToolId | null>(null);

  // WebLink Studio modal state
  const [webLinkModal, setWebLinkModal] = useState<{
    open: boolean;
    editId: string | null;
    x: number;
    y: number;
    url: string;
    title: string;
    note: string;
    color: string;
  }>({
    open: false,
    editId: null,
    x: 240,
    y: 220,
    url: 'https://',
    title: '',
    note: '',
    color: WEBLINK_COLORS[0],
  });

  // Slide Show Revision modal state
  const [slideModalOpen, setSlideModalOpen] = useState(false);
  const [slideEngineLabel, setSlideEngineLabel] = useState('Deck Studio');
  const [slideLoadingStage, setSlideLoadingStage] = useState<string | null>(null);

  // Resizable Split Workspace
  const [splitNoteId, setSplitNoteId] = useState<string | null>(null);
  const [splitWidth, setSplitWidth] = useState(360);
  const splitResizeRef = useRef<{ active: boolean; startX: number; startW: number }>({
    active: false,
    startX: 0,
    startW: 360,
  });

  const [collabModalOpen, setCollabModalOpen] = useState(false);
  const [cmdPaletteOpen, setCmdPaletteOpen] = useState(false);
  const [pomodoroActive, setPomodoroActive] = useState(false);

  const [toast, setToast] = useState<string | null>(null);
  const [summaryModal, setSummaryModal] = useState<{
    open: boolean;
    title: string;
    summary: RichSummaryResult | null;
    loadingStage?: string | null;
    explainText?: string | null;
  }>({ open: false, title: '', summary: null });
  const [flashModalOpen, setFlashModalOpen] = useState(false);
  const [quizModal, setQuizModal] = useState<{
    open: boolean;
    questions: QuizQuestion[];
    engineLabel: string;
    loadingStage?: string | null;
  }>({ open: false, questions: [], engineLabel: '' });

  // Study / Pomodoro timer
  const [studySec, setStudySec] = useState(0);
  const [timerRunning, setTimerRunning] = useState(settings.studyAutoStart);

  // Camera
  const [camera, setCamera] = useState<{ panX: number; panY: number; zoom: number }>({
    panX: 60,
    panY: 32,
    zoom: 0.72,
  });
  const [spaceDown, setSpaceDown] = useState(false);

  const canvasRef = useRef<HTMLCanvasElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const imageCacheRef = useRef<Map<string, HTMLImageElement>>(new Map());
  const fileInputRef = useRef<HTMLInputElement>(null);
  const importFileRef = useRef<HTMLInputElement>(null);

  // Offscreen static paper cache & dirty-flag render loop
  const paperCacheRef = useRef<{ key: string; canvas: HTMLCanvasElement } | null>(null);
  const needsDrawRef = useRef(true);
  const requestDraw = useCallback(() => {
    needsDrawRef.current = true;
  }, []);

  // Mutable working copy of page items during drags/live-erase so React doesn't thrash at 60fps
  const workingItemsRef = useRef<PageItem[] | null>(null);
  const hoverWorldRef = useRef<Pt | null>(null);

  const gestureRef = useRef<{
    active: boolean;
    mode:
      | 'NONE'
      | 'INK'
      | 'ERASE'
      | 'SHAPE'
      | 'LASSO'
      | 'PAN'
      | 'DRAG_ITEMS'
      | 'DRAG_MIND'
      | 'RESIZE_STICKY'
      | 'STRETCH_ITEMS'
      | 'TAPE'
      | 'LASER';
    pts: StrokePt[];
    startScreen: Pt;
    lastScreen: Pt;
    startWorld: Pt;
    lastWorld: Pt;
    scribbleConsumed: boolean;
    dragMindId: string | null;
    resizeStickyId: string | null;
    origStickySize: { w: number; h: number } | null;
    stretchHandle: string | null;
    stretchOrigBox: StretchBox | null;
    dragOrigItems: PageItem[] | null;
  }>({
    active: false,
    mode: 'NONE',
    pts: [],
    startScreen: { x: 0, y: 0 },
    lastScreen: { x: 0, y: 0 },
    startWorld: { x: 0, y: 0 },
    lastWorld: { x: 0, y: 0 },
    scribbleConsumed: false,
    dragMindId: null,
    resizeStickyId: null,
    origStickySize: null,
    stretchHandle: null,
    stretchOrigBox: null,
    dragOrigItems: null,
  });

  const puffsRef = useRef<PuffParticle[]>([]);
  const ringsRef = useRef<EraseRing[]>([]);
  const lasersRef = useRef<LaserTrail[]>([]);
  const circleHoldTimerRef = useRef<number | null>(null);
  const circleChargeRef = useRef<{ x: number; y: number; born: number } | null>(null);

  const safeIdx = Math.min(pageIdx, Math.max(0, notebook.pages.length - 1));
  const page = notebook.pages[safeIdx];
  const isWhiteboard = Boolean(page?.isWhiteboard || notebook.isWhiteboard);

  const showToast = useCallback((msg: string) => {
    setToast(msg);
    window.setTimeout(() => setToast((cur) => (cur === msg ? null : cur)), 2600);
  }, []);

  const fitPageToView = useCallback((targetPage: Page | undefined) => {
    const el = containerRef.current;
    if (!el || !targetPage) return;
    const cw = el.clientWidth;
    const ch = el.clientHeight;
    if (targetPage.isWhiteboard) {
      setCamera({
        panX: targetPage.wbPanX ?? Math.max(40, cw * 0.12),
        panY: targetPage.wbPanY ?? Math.max(40, ch * 0.1),
        zoom: targetPage.wbZoom ?? 0.88,
      });
    } else {
      const fit = Math.min((cw - 64) / targetPage.width, (ch - 56) / targetPage.height, 1.0);
      const z = Math.max(0.28, fit);
      setCamera({
        panX: (cw - targetPage.width * z) / 2,
        panY: 28,
        zoom: z,
      });
    }
  }, []);

  useEffect(() => {
    setPageIdx(0);
    setSelectedIds(new Set());
    setEditingStickyId(null);
    setUndoStack([]);
    setRedoStack([]);
    workingItemsRef.current = null;
    setTool(notebook.isMindMap ? 'MINDMAP' : 'PEN');
    window.setTimeout(() => fitPageToView(notebook.pages[0]), 20);
  }, [notebook.id, fitPageToView]);

  // Mark canvas dirty whenever React props/state affecting display change
  useEffect(() => {
    requestDraw();
  }, [
    page,
    isWhiteboard,
    camera,
    settings,
    selectedIds,
    tool,
    penColor,
    penSize,
    penKind,
    penOpacity,
    hlColor,
    hlSize,
    eraserSize,
    eraserMode,
    shapeKind,
    tapeColor,
    requestDraw,
  ]);

  // Split pane resize pointer tracking
  useEffect(() => {
    const onMove = (e: MouseEvent) => {
      if (!splitResizeRef.current.active) return;
      const dx = splitResizeRef.current.startX - e.clientX;
      setSplitWidth(Math.max(240, Math.min(Math.round(window.innerWidth * 0.76), splitResizeRef.current.startW + dx)));
    };
    const onUp = () => {
      splitResizeRef.current.active = false;
    };
    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onUp);
    return () => {
      window.removeEventListener('mousemove', onMove);
      window.removeEventListener('mouseup', onUp);
    };
  }, []);

  // Study timer
  useEffect(() => {
    if (!settings.studyTimer || !timerRunning) return;
    const id = window.setInterval(() => setStudySec((s) => s + 1), 1000);
    return () => window.clearInterval(id);
  }, [settings.studyTimer, timerRunning]);

  // Lightweight per-page undo/redo
  const commitPages = useCallback(
    (updater: (pages: Page[]) => Page[], pushHistory = true) => {
      if (pushHistory && page) {
        setUndoStack((prev) => [...prev.slice(-28), { pageIdx: safeIdx, items: [...page.items] }]);
        setRedoStack([]);
      }
      onUpdateNotebook(notebook.id, (nb) => ({
        ...nb,
        pages: updater(nb.pages),
      }));
    },
    [notebook.id, page, safeIdx, onUpdateNotebook]
  );

  const commitPageItems = useCallback(
    (updater: (items: PageItem[]) => PageItem[], pushHistory = true) => {
      commitPages(
        (pages) => pages.map((pg, idx) => (idx === safeIdx ? { ...pg, items: updater(pg.items) } : pg)),
        pushHistory
      );
    },
    [commitPages, safeIdx]
  );

  const handleUndo = useCallback(() => {
    if (undoStack.length === 0 || !page) return;
    const entry = undoStack[undoStack.length - 1];
    const targetPage = notebook.pages[entry.pageIdx];
    setUndoStack((s) => s.slice(0, -1));
    if (targetPage) {
      setRedoStack((s) => [...s.slice(-28), { pageIdx: entry.pageIdx, items: [...targetPage.items] }]);
    }
    onUpdateNotebook(notebook.id, (nb) => ({
      ...nb,
      pages: nb.pages.map((pg, idx) => (idx === entry.pageIdx ? { ...pg, items: entry.items } : pg)),
    }));
    setSelectedIds(new Set());
  }, [undoStack, page, notebook.id, notebook.pages, onUpdateNotebook]);

  const handleRedo = useCallback(() => {
    if (redoStack.length === 0) return;
    const entry = redoStack[redoStack.length - 1];
    const targetPage = notebook.pages[entry.pageIdx];
    setRedoStack((s) => s.slice(0, -1));
    if (targetPage) {
      setUndoStack((s) => [...s.slice(-28), { pageIdx: entry.pageIdx, items: [...targetPage.items] }]);
    }
    onUpdateNotebook(notebook.id, (nb) => ({
      ...nb,
      pages: nb.pages.map((pg, idx) => (idx === entry.pageIdx ? { ...pg, items: entry.items } : pg)),
    }));
    setSelectedIds(new Set());
  }, [redoStack, notebook.id, notebook.pages, onUpdateNotebook]);

  const spawnPuffForItems = useCallback((items: PageItem[]) => {
    const now = performance.now();
    const GREY_PUFFS = ['#94A3B8', '#64748B', '#CBD5E1', '#475569', '#9CA3AF'];
    for (const it of items) {
      const b = getItemBounds(it);
      ringsRef.current.push({
        x: b.cx,
        y: b.cy,
        maxR: Math.max(28, Math.min(110, b.diag * 0.45)),
        born: now,
        color: '#94A3B8',
      });
      for (let k = 0; k < 16; k++) {
        const ang = (Math.PI * 2 * k) / 16 + Math.random() * 0.55;
        const speed = 0.8 + Math.random() * 2.2;
        puffsRef.current.push({
          x: b.l + Math.random() * Math.max(10, b.w),
          y: b.t + Math.random() * Math.max(10, b.h),
          vx: Math.cos(ang) * speed,
          vy: Math.sin(ang) * speed - 0.55,
          r: 2.2 + Math.random() * 3.8,
          born: now,
          color: GREY_PUFFS[k % GREY_PUFFS.length],
        });
      }
    }
    requestDraw();
  }, [requestDraw]);

  const deleteSelected = useCallback(() => {
    if (selectedIds.size === 0 || !page) return;
    const mindNodes = page.items.filter((i): i is MindNodeItem => i.kind === 'mindnode');
    const toRemove = new Set(selectedIds);
    let added = true;
    while (added) {
      added = false;
      for (const n of mindNodes) {
        if (n.parentId && toRemove.has(n.parentId) && !toRemove.has(n.id)) {
          toRemove.add(n.id);
          added = true;
        }
      }
    }
    const gone = page.items.filter((it) => toRemove.has(it.id));
    spawnPuffForItems(gone);
    commitPageItems((items) => items.filter((it) => !toRemove.has(it.id)));
    setSelectedIds(new Set());
    if (editingStickyId && toRemove.has(editingStickyId)) setEditingStickyId(null);
  }, [selectedIds, page, commitPageItems, spawnPuffForItems, editingStickyId]);

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        setCmdPaletteOpen((v) => !v);
        return;
      }
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return;
      if (e.code === 'Space') {
        e.preventDefault();
        setSpaceDown(true);
      }
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'z') {
        e.preventDefault();
        if (e.shiftKey) handleRedo();
        else handleUndo();
      }
      if ((e.key === 'Delete' || e.key === 'Backspace') && selectedIds.size > 0) {
        e.preventDefault();
        deleteSelected();
      }
    };
    const onKeyUp = (e: KeyboardEvent) => {
      if (e.code === 'Space') setSpaceDown(false);
    };
    window.addEventListener('keydown', onKeyDown);
    window.addEventListener('keyup', onKeyUp);
    return () => {
      window.removeEventListener('keydown', onKeyDown);
      window.removeEventListener('keyup', onKeyUp);
    };
  }, [handleUndo, handleRedo, deleteSelected, selectedIds]);

  const screenToWorld = useCallback(
    (sx: number, sy: number): Pt => ({
      x: (sx - camera.panX) / camera.zoom,
      y: (sy - camera.panY) / camera.zoom,
    }),
    [camera]
  );

  // ================================================================
  // Mind Map Helpers
  // ================================================================

  const mindNodesOnPage = useMemo(
    () => (page?.items.filter((i): i is MindNodeItem => i.kind === 'mindnode') || []),
    [page]
  );

  const selectedMindNode = useMemo(() => {
    if (selectedIds.size !== 1) return null;
    const id = Array.from(selectedIds)[0];
    return mindNodesOnPage.find((n) => n.id === id) || null;
  }, [selectedIds, mindNodesOnPage]);

  const selectedStickyNode = useMemo(() => {
    const id = editingStickyId || (selectedIds.size === 1 ? Array.from(selectedIds)[0] : null);
    if (!id || !page) return null;
    const found = page.items.find((i) => i.id === id && i.kind === 'sticky');
    return found && found.kind === 'sticky' ? found : null;
  }, [editingStickyId, selectedIds, page]);

  const selectedWebLinkNode = useMemo(() => {
    if (selectedIds.size !== 1 || !page) return null;
    const id = Array.from(selectedIds)[0];
    const found = page.items.find((i) => i.id === id && i.kind === 'weblink');
    return found && found.kind === 'weblink' ? found : null;
  }, [selectedIds, page]);

  const openMindEditor = useCallback((node: MindNodeItem) => {
    setEditingMindId(node.id);
    setMindDraft(node.label);
    setMindTagDraft(node.tag || '');
    setMindNotesDraft(node.notes || '');
    setMindShapeDraft(node.shape);
    setMindColorDraft(node.color);
  }, []);

  const fitMindMapToView = useCallback(() => {
    const el = containerRef.current;
    if (!el || mindNodesOnPage.length === 0) {
      fitPageToView(page);
      return;
    }
    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
    for (const n of mindNodesOnPage) {
      minX = Math.min(minX, n.x);
      minY = Math.min(minY, n.y);
      maxX = Math.max(maxX, n.x + n.w);
      maxY = Math.max(maxY, n.y + n.h);
    }
    const bw = Math.max(220, maxX - minX + 120);
    const bh = Math.max(160, maxY - minY + 120);
    const zoom = Math.max(0.28, Math.min(1.4, Math.min(el.clientWidth / bw, el.clientHeight / bh)));
    const cx = (minX + maxX) / 2;
    const cy = (minY + maxY) / 2;
    setCamera({
      zoom,
      panX: el.clientWidth / 2 - cx * zoom,
      panY: el.clientHeight / 2 - cy * zoom,
    });
  }, [mindNodesOnPage, fitPageToView, page]);

  const addMindNode = useCallback(
    (parent: MindNodeItem | null, worldPos?: Pt, label = 'New Idea') => {
      const siblings = mindNodesOnPage.filter((n) => n.parentId === (parent?.id ?? null));
      const color =
        parent === null
          ? MIND_COLORS[siblings.length % MIND_COLORS.length]
          : parent.parentId === null
            ? MIND_COLORS[siblings.length % MIND_COLORS.length]
            : parent.color;

      const x = worldPos ? worldPos.x - 75 : parent ? parent.x + parent.w + 80 : 400;
      const y = worldPos ? worldPos.y - 21 : parent ? parent.y + (siblings.length - 0.5) * 76 : 300;

      const newNode: MindNodeItem = {
        id: `mm-${Date.now()}-${Math.floor(Math.random() * 999)}`,
        kind: 'mindnode',
        x,
        y,
        w: 168,
        h: 46,
        label,
        parentId: parent?.id ?? null,
        color,
        shape: parent ? 'card' : 'pill',
      };
      commitPageItems((items) =>
        clampMindNodesToPage([...items, newNode], page?.width || 1200, page?.height || 1697, isWhiteboard)
      );
      setSelectedIds(new Set([newNode.id]));
      openMindEditor(newNode);
    },
    [mindNodesOnPage, commitPageItems, openMindEditor, page?.width, page?.height, isWhiteboard]
  );

  const addSiblingMindNode = useCallback(
    (node: MindNodeItem) => {
      const parent = node.parentId ? mindNodesOnPage.find((n) => n.id === node.parentId) || null : null;
      addMindNode(parent, undefined, parent ? 'Sibling Idea' : 'Topic');
    },
    [mindNodesOnPage, addMindNode]
  );

  useEffect(() => {
    const onMindKey = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement || e.target instanceof HTMLSelectElement) {
        return;
      }
      if (!selectedMindNode || editingMindId || editingTextId || editingStickyId) return;
      if (e.key === 'Tab') {
        e.preventDefault();
        addMindNode(selectedMindNode, undefined, 'Child Idea');
      } else if (e.key === 'Enter') {
        e.preventDefault();
        if (selectedMindNode.parentId) {
          addSiblingMindNode(selectedMindNode);
        } else {
          openMindEditor(selectedMindNode);
        }
      }
    };
    window.addEventListener('keydown', onMindKey);
    return () => window.removeEventListener('keydown', onMindKey);
  }, [selectedMindNode, editingMindId, editingTextId, editingStickyId, addMindNode, addSiblingMindNode, openMindEditor]);

  const autoLayoutMindMap = useCallback(
    (layoutType: 'TREE' | 'RADIAL' | 'ORG_CHART') => {
      if (mindNodesOnPage.length === 0) return;
      const roots = mindNodesOnPage.filter((n) => n.parentId === null);
      const root = roots[0] || mindNodesOnPage[0];
      const byParent = new Map<string, MindNodeItem[]>();
      for (const n of mindNodesOnPage) {
        if (n.parentId) {
          const arr = byParent.get(n.parentId) || [];
          arr.push(n);
          byParent.set(n.parentId, arr);
        }
      }

      const posMap = new Map<string, { x: number; y: number }>();
      const cx = root.x + root.w / 2;
      const cy = root.y + root.h / 2;
      posMap.set(root.id, { x: root.x, y: root.y });

      if (layoutType === 'RADIAL') {
        const placeRadial = (node: MindNodeItem, startAng: number, endAng: number, radius: number) => {
          const kids = byParent.get(node.id) || [];
          kids.forEach((k, idx) => {
            const ang = startAng + ((idx + 0.5) / Math.max(1, kids.length)) * (endAng - startAng);
            const nx = cx + Math.cos(ang) * radius - k.w / 2;
            const ny = cy + Math.sin(ang) * radius - k.h / 2;
            posMap.set(k.id, { x: nx, y: ny });
            placeRadial(
              k,
              startAng + (idx / Math.max(1, kids.length)) * (endAng - startAng),
              startAng + ((idx + 1) / Math.max(1, kids.length)) * (endAng - startAng),
              radius + 195
            );
          });
        };
        placeRadial(root, -Math.PI, Math.PI, 250);
      } else if (layoutType === 'ORG_CHART') {
        const spanOf = (node: MindNodeItem): number => {
          const kids = byParent.get(node.id) || [];
          if (kids.length === 0) return Math.max(195, node.w + 36);
          return kids.reduce((acc, k) => acc + spanOf(k), 0);
        };
        const totalSpan = spanOf(root);
        const startX = cx - totalSpan / 2;
        const startY = Math.max(80, cy - 140);
        const placeOrg = (node: MindNodeItem, leftX: number, depth: number) => {
          const kids = byParent.get(node.id) || [];
          const mySpan = spanOf(node);
          posMap.set(node.id, {
            x: leftX + mySpan / 2 - node.w / 2,
            y: startY + depth * 135,
          });
          let curX = leftX;
          for (const k of kids) {
            const ks = spanOf(k);
            placeOrg(k, curX, depth + 1);
            curX += ks;
          }
        };
        placeOrg(root, startX, 0);
      } else {
        let nextY = cy - 180;
        const placeTree = (node: MindNodeItem, depth: number): number => {
          const kids = byParent.get(node.id) || [];
          if (kids.length === 0) {
            const y = nextY;
            nextY += 86;
            posMap.set(node.id, { x: cx - 100 + depth * 250, y });
            return y;
          }
          const ys = kids.map((k) => placeTree(k, depth + 1));
          const avgY = (ys[0] + ys[ys.length - 1]) / 2;
          posMap.set(node.id, { x: cx - 100 + depth * 250, y: avgY });
          return avgY;
        };
        placeTree(root, 0);
      }

      commitPageItems((items) =>
        clampMindNodesToPage(
          items.map((it) => {
            if (it.kind !== 'mindnode') return it;
            const p = posMap.get(it.id);
            return p ? { ...it, x: p.x, y: p.y } : it;
          }),
          page?.width || 1200,
          page?.height || 1697,
          isWhiteboard
        )
      );
      const labelMap = { RADIAL: 'Radial', TREE: 'Tree', ORG_CHART: 'Org Chart' };
      showToast(`Applied ${labelMap[layoutType]} Mind Map layout`);
    },
    [mindNodesOnPage, commitPageItems, showToast, page?.width, page?.height, isWhiteboard]
  );

  // ================================================================
  // Idle-Aware Dirty-Flag Canvas Loop + Paper Template Caching
  // ================================================================

  useEffect(() => {
    let rafId = 0;

    const drawFrame = () => {
      rafId = requestAnimationFrame(drawFrame);

      const now = performance.now();
      const hasAnims =
        gestureRef.current.active ||
        puffsRef.current.length > 0 ||
        ringsRef.current.length > 0 ||
        circleChargeRef.current !== null ||
        lasersRef.current.length > 0;

      if (!needsDrawRef.current && !hasAnims) return;
      needsDrawRef.current = false;

      const canvas = canvasRef.current;
      const container = containerRef.current;
      if (!canvas || !container || !page) return;

      const dpr = window.devicePixelRatio || 1;
      const cw = container.clientWidth;
      const ch = container.clientHeight;
      if (canvas.width !== Math.round(cw * dpr) || canvas.height !== Math.round(ch * dpr)) {
        canvas.width = Math.round(cw * dpr);
        canvas.height = Math.round(ch * dpr);
      }

      const ctx = canvas.getContext('2d');
      if (!ctx) return;

      ctx.save();
      ctx.scale(dpr, dpr);

      const paperBg = page.paperColor || settings.paperTint || '#FFFFFF';

      if (isWhiteboard) {
        drawWhiteboardBackdrop(ctx, cw, ch, camera.panX, camera.panY, camera.zoom, paperBg);
      } else {
        ctx.fillStyle = isColorDark(paperBg) ? '#121316' : '#E7E9EF';
        ctx.fillRect(0, 0, cw, ch);
      }

      ctx.save();
      ctx.translate(camera.panX, camera.panY);
      ctx.scale(camera.zoom, camera.zoom);

      if (!isWhiteboard) {
        ctx.save();
        ctx.shadowColor = 'rgba(0,0,0,0.16)';
        ctx.shadowBlur = 24;
        ctx.shadowOffsetY = 8;
        ctx.fillStyle = paperBg;
        ctx.fillRect(0, 0, page.width, page.height);
        ctx.restore();

        // Offscreen cached paper template (supports custom AI image templates via page.cover)
        const cacheKey = `${page.width}x${page.height}-${page.paper}-${paperBg}-${page.lineScale ?? 1}-${page.lineColor ?? ''}-${page.cover ?? ''}`;
        if (!paperCacheRef.current || paperCacheRef.current.key !== cacheKey) {
          const off = document.createElement('canvas');
          off.width = page.width;
          off.height = page.height;
          const octx = off.getContext('2d');
          if (octx) {
            octx.fillStyle = paperBg;
            octx.fillRect(0, 0, page.width, page.height);
            drawPaperTemplate(
              octx,
              page.width,
              page.height,
              page.paper,
              paperBg,
              page.lineScale ?? 1,
              page.lineColor,
              false,
              page.cover
            );
          }
          paperCacheRef.current = { key: cacheKey, canvas: off };
        }
        ctx.drawImage(paperCacheRef.current.canvas, 0, 0);
      }

      const rawPageItems = workingItemsRef.current || page.items;
      const collabPageItems =
        activeCollabRoom && collabItems.length > 0
          ? collabItems
              .filter((ci) => ci.pageIdx === safeIdx && !rawPageItems.some((r) => r.id === ci.id))
              .map((ci) => ci.data as unknown as PageItem)
          : [];
      const renderPage = { ...page, items: [...rawPageItems, ...collabPageItems] };
      renderPageItems(ctx, renderPage, imageCacheRef.current, selectedIds, settings.highlighterBehind);

      // Selected Sticky resize grip handle (bottom-right, hidden when collapsed or pinned)
      if (selectedIds.size === 1) {
        const sid = Array.from(selectedIds)[0];
        const st = renderPage.items.find((i): i is StickyItem => i.id === sid && i.kind === 'sticky');
        if (st && !st.collapsed && !st.pinned) {
          ctx.save();
          ctx.fillStyle = '#3D6CF4';
          ctx.beginPath();
          ctx.arc(st.x + st.w, st.y + st.h, 7 / camera.zoom, 0, Math.PI * 2);
          ctx.fill();
          ctx.strokeStyle = '#FFFFFF';
          ctx.lineWidth = 2 / camera.zoom;
          ctx.stroke();
          ctx.restore();
        }
      }

      const g = gestureRef.current;
      if (g.active) {
        if (g.mode === 'INK' && g.pts.length > 0) {
          const liveInk: InkItem = {
            id: 'live',
            kind: 'ink',
            points: g.pts,
            color: tool === 'HIGHLIGHTER' ? hlColor : penColor,
            size: tool === 'HIGHLIGHTER' ? hlSize : penSize,
            isHighlighter: tool === 'HIGHLIGHTER',
            opacity: tool === 'HIGHLIGHTER' ? settings.highlighterOpacity : penOpacity,
            penKind,
          };
          renderPageItems(
            ctx,
            { ...renderPage, items: [liveInk] },
            imageCacheRef.current,
            selectedIds,
            settings.highlighterBehind
          );
        } else if (g.mode === 'SHAPE') {
          const shapes = ShapeBuilder.build(shapeKind, g.startWorld.x, g.startWorld.y, g.lastWorld.x, g.lastWorld.y);
          ctx.save();
          ctx.strokeStyle = penColor;
          ctx.lineWidth = penSize;
          ctx.lineCap = 'round';
          ctx.lineJoin = 'round';
          for (const poly of shapes) {
            ctx.beginPath();
            poly.forEach((p, idx) => (idx === 0 ? ctx.moveTo(p.x, p.y) : ctx.lineTo(p.x, p.y)));
            ctx.stroke();
          }
          ctx.restore();
        } else if (g.mode === 'TAPE') {
          const l = Math.min(g.startWorld.x, g.lastWorld.x);
          const t = Math.min(g.startWorld.y, g.lastWorld.y);
          const w = Math.abs(g.lastWorld.x - g.startWorld.x);
          const h = Math.max(36, Math.abs(g.lastWorld.y - g.startWorld.y));
          ctx.save();
          ctx.globalAlpha = 0.6;
          ctx.fillStyle = tapeColor;
          ctx.fillRect(l, t, Math.max(24, w), h);
          ctx.restore();
        } else if (g.mode === 'LASSO' && g.pts.length > 1) {
          ctx.save();
          ctx.strokeStyle = '#3D6CF4';
          ctx.fillStyle = 'rgba(61,108,244,0.08)';
          ctx.lineWidth = 2 / camera.zoom;
          ctx.setLineDash([8 / camera.zoom, 5 / camera.zoom]);
          ctx.lineDashOffset = -(now * 0.03) % 26;
          ctx.beginPath();
          g.pts.forEach((p, idx) => (idx === 0 ? ctx.moveTo(p.x, p.y) : ctx.lineTo(p.x, p.y)));
          ctx.closePath();
          ctx.fill();
          ctx.stroke();
          ctx.restore();
        }
      }

      // Live Eraser cursor ring (visible both hovering & dragging)
      if (tool === 'ERASER' && (hoverWorldRef.current || g.mode === 'ERASE')) {
        const pt = g.mode === 'ERASE' ? g.lastWorld : hoverWorldRef.current!;
        ctx.save();
        ctx.strokeStyle = eraserMode === 'PIXEL' ? '#F59E0B' : eraserMode === 'HIGHLIGHTER' ? '#EAB308' : '#E5484D';
        ctx.fillStyle =
          eraserMode === 'PIXEL'
            ? 'rgba(245,158,11,0.12)'
            : eraserMode === 'HIGHLIGHTER'
              ? 'rgba(234,179,8,0.14)'
              : 'rgba(229,72,77,0.12)';
        ctx.lineWidth = 1.6 / camera.zoom;
        ctx.beginPath();
        ctx.arc(pt.x, pt.y, eraserSize / 2, 0, Math.PI * 2);
        ctx.fill();
        ctx.stroke();
        ctx.restore();
      }

      // Universal Selection Bounding Box + 8 Free X/Y Stretch Handles for ALL selected elements
      if (selectedIds.size > 0) {
        const selItems = renderPage.items.filter((i) => selectedIds.has(i.id));
        if (selItems.length > 0) {
          let l = Infinity, t = Infinity, r = -Infinity, b = -Infinity;
          for (const it of selItems) {
            const bx = getItemBounds(it);
            l = Math.min(l, bx.l);
            t = Math.min(t, bx.t);
            r = Math.max(r, bx.r);
            b = Math.max(b, bx.b);
          }
          const cx = (l + r) / 2;
          const cy = (t + b) / 2;
          ctx.save();
          ctx.strokeStyle = '#3D6CF4';
          ctx.lineWidth = 2 / camera.zoom;
          ctx.setLineDash([7 / camera.zoom, 5 / camera.zoom]);
          ctx.strokeRect(l - 6, t - 6, r - l + 12, b - t + 12);
          ctx.setLineDash([]);

          const handleRad = 5.5 / camera.zoom;
          const handles: [number, number][] = [
            [l - 6, t - 6],
            [cx, t - 6],
            [r + 6, t - 6],
            [r + 6, cy],
            [r + 6, b + 6],
            [cx, b + 6],
            [l - 6, b + 6],
            [l - 6, cy],
          ];
          for (const [hx, hy] of handles) {
            ctx.fillStyle = '#FFFFFF';
            ctx.strokeStyle = '#3D6CF4';
            ctx.lineWidth = 2 / camera.zoom;
            ctx.beginPath();
            ctx.arc(hx, hy, handleRad, 0, Math.PI * 2);
            ctx.fill();
            ctx.stroke();
          }
          ctx.restore();
        }
      }

      // Live Collaborative Remote Cursors
      if (activeCollabRoom && collabMembers.length > 0) {
        for (const m of collabMembers) {
          if (m.activePageIdx !== safeIdx) continue;
          ctx.save();
          ctx.fillStyle = m.color;
          ctx.beginPath();
          ctx.arc(m.cursorX, m.cursorY, 5 / camera.zoom, 0, Math.PI * 2);
          ctx.fill();
          ctx.font = `700 ${Math.round(11 / camera.zoom)}px "Plus Jakarta Sans", sans-serif`;
          ctx.fillText(m.displayName, m.cursorX + 8 / camera.zoom, m.cursorY + 3 / camera.zoom);
          ctx.restore();
        }
      }

      // Hold-to-Lasso Radial Charging Ring (only when holding pen still on a closed loop)
      if (circleChargeRef.current && now >= circleChargeRef.current.born) {
        const cc = circleChargeRef.current;
        const pct = Math.min(1, (now - cc.born) / 320);
        const rad = 18 / camera.zoom;
        ctx.save();
        ctx.strokeStyle = 'rgba(61,108,244,0.22)';
        ctx.lineWidth = 3 / camera.zoom;
        ctx.beginPath();
        ctx.arc(cc.x, cc.y, rad, 0, Math.PI * 2);
        ctx.stroke();
        ctx.strokeStyle = '#3D6CF4';
        ctx.lineCap = 'round';
        ctx.beginPath();
        ctx.arc(cc.x, cc.y, rad, -Math.PI / 2, -Math.PI / 2 + Math.PI * 2 * pct);
        ctx.stroke();
        ctx.restore();
      }

      // Soft grey erase puff ring
      ringsRef.current = ringsRef.current.filter((rg) => now - rg.born < 380);
      for (const rg of ringsRef.current) {
        const t = (now - rg.born) / 380;
        const ease = 1 - Math.pow(1 - t, 2);
        const rad = 6 + (rg.maxR - 6) * ease;
        ctx.save();
        ctx.globalAlpha = Math.max(0, (1 - t) * 0.35);
        ctx.strokeStyle = rg.color;
        ctx.lineWidth = 2 * (1 - t * 0.5);
        ctx.beginPath();
        ctx.arc(rg.x, rg.y, rad, 0, Math.PI * 2);
        ctx.stroke();
        ctx.restore();
      }

      // Crumbling grey dust particles
      puffsRef.current = puffsRef.current.filter((pf) => now - pf.born < 440);
      for (const pf of puffsRef.current) {
        const t = (now - pf.born) / 440;
        const ease = 1 - Math.pow(1 - t, 2);
        const px = pf.x + pf.vx * t * 32;
        const py = pf.y + pf.vy * t * 32 - 10 * ease;
        ctx.save();
        ctx.globalAlpha = Math.max(0, (1 - t) * 0.72);
        ctx.fillStyle = pf.color;
        ctx.beginPath();
        ctx.arc(px, py, Math.max(0.8, pf.r * (1 - t * 0.35)), 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();
      }

      // Laser Trails
      lasersRef.current = lasersRef.current.filter((lt) => now - lt.born < 2200);
      for (const lt of lasersRef.current) {
        if (lt.pts.length < 2) continue;
        const alpha = Math.max(0, 1 - (now - lt.born) / 2200);
        ctx.save();
        ctx.lineCap = 'round';
        ctx.lineJoin = 'round';
        ctx.strokeStyle = `rgba(239, 68, 68, ${alpha * 0.35})`;
        ctx.lineWidth = 14;
        ctx.beginPath();
        lt.pts.forEach((p, idx) => (idx === 0 ? ctx.moveTo(p.x, p.y) : ctx.lineTo(p.x, p.y)));
        ctx.stroke();
        ctx.strokeStyle = `rgba(255, 255, 255, ${alpha * 0.9})`;
        ctx.lineWidth = 4;
        ctx.stroke();
        ctx.restore();
      }

      ctx.restore();
      ctx.restore();
    };

    rafId = requestAnimationFrame(drawFrame);
    return () => cancelAnimationFrame(rafId);
  }, [page, isWhiteboard, camera, settings, selectedIds, tool, penColor, penSize, penKind, penOpacity, hlColor, hlSize, eraserSize, eraserMode, shapeKind, tapeColor]);

  // ================================================================
  // Pointer Handlers (Zero-lag Mutable Drag + Precision/Live Scribble Erase)
  // ================================================================

  const hitTestMindNode = (world: Pt): MindNodeItem | undefined => {
    const hidden = getHiddenMindNodeIds(mindNodesOnPage);
    return [...mindNodesOnPage]
      .reverse()
      .find((n) => !hidden.has(n.id) && world.x >= n.x && world.x <= n.x + n.w && world.y >= n.y && world.y <= n.y + n.h);
  };

  const hitTestAnyItem = (world: Pt): PageItem | undefined => {
    if (!page) return undefined;
    const hidden = getHiddenMindNodeIds(mindNodesOnPage);
    return [...page.items].reverse().find((it) => {
      if (it.kind === 'mindnode' && hidden.has(it.id)) return false;
      const b = getItemBounds(it);
      return world.x >= b.l - 8 && world.x <= b.r + 8 && world.y >= b.t - 8 && world.y <= b.b + 8;
    });
  };

  const eraseInWorkingItems = (world: Pt) => {
    const current = workingItemsRef.current || page?.items;
    if (!current) return;
    const r = eraserSize / 2;
    const removed: PageItem[] = [];
    const next: PageItem[] = [];

    for (const it of current) {
      if (eraserMode === 'HIGHLIGHTER') {
        if (it.kind === 'ink' && it.isHighlighter && it.points.some((p) => dist(p, world) <= r + it.size / 2)) {
          removed.push(it);
        } else {
          next.push(it);
        }
        continue;
      }

      if (eraserMode === 'PIXEL' && it.kind === 'ink') {
        const split = splitStrokeByEraser(it, world, r);
        if (split !== null) {
          removed.push(it);
          next.push(...split);
          continue;
        }
        next.push(it);
        continue;
      }

      // Whole Stroke + Text eraser
      if (it.kind === 'ink') {
        if (it.points.some((p) => dist(p, world) <= r + it.size / 2)) {
          removed.push(it);
          continue;
        }
      } else if (it.kind === 'text') {
        const b = getItemBounds(it);
        if (world.x >= b.l - r && world.x <= b.r + r && world.y >= b.t - r && world.y <= b.b + r) {
          removed.push(it);
          continue;
        }
      }
      next.push(it);
    }

    if (removed.length > 0) {
      workingItemsRef.current = next;
      spawnPuffForItems(removed);
      requestDraw();
    }
  };

  const handlePointerDown = (e: React.PointerEvent<HTMLCanvasElement>) => {
    if (!page) return;
    e.currentTarget.setPointerCapture(e.pointerId);
    const rect = e.currentTarget.getBoundingClientRect();
    const sx = e.clientX - rect.left;
    const sy = e.clientY - rect.top;
    const world = screenToWorld(sx, sy);
    const g = gestureRef.current;
    g.active = true;
    g.startScreen = { x: sx, y: sy };
    g.lastScreen = { x: sx, y: sy };
    g.startWorld = world;
    g.lastWorld = world;
    g.scribbleConsumed = false;
    g.pts = [{ x: world.x, y: world.y, p: e.pressure || 0.65 }];

    if (spaceDown || e.button === 1 || tool === 'PAN') {
      g.mode = 'PAN';
      return;
    }

    // Interactive Sticky Note Checkbox Tap (no modal popup required!)
    if (tool !== 'ERASER' && tool !== 'LASSO') {
      const stickies = [...page.items].reverse().filter((i): i is StickyItem => i.kind === 'sticky');
      for (const st of stickies) {
        const hitBox = getStickyChecklistHitBoxes(st).find(
          (hb) => world.x >= hb.x && world.x <= hb.x + 36 && world.y >= hb.y && world.y <= hb.y + hb.h
        );
        if (hitBox) {
          const nextText = toggleStickyChecklistLine(st.text, hitBox.lineIndex);
          commitPageItems((items) =>
            items.map((it) => (it.id === st.id && it.kind === 'sticky' ? { ...it, text: nextText } : it))
          );
          g.active = false;
          requestDraw();
          return;
        }
      }
    }

    // Check 8-handle Universal Stretch Controls for ANY selected element(s)
    if (selectedIds.size > 0) {
      const selItems = page.items.filter((i) => selectedIds.has(i.id));
      if (selItems.length > 0) {
        let l = Infinity, t = Infinity, r = -Infinity, b = -Infinity;
        for (const it of selItems) {
          const bx = getItemBounds(it);
          l = Math.min(l, bx.l);
          t = Math.min(t, bx.t);
          r = Math.max(r, bx.r);
          b = Math.max(b, bx.b);
        }
        const cx = (l + r) / 2;
        const cy = (t + b) / 2;
        const hitR = 16 / camera.zoom;
        const handleCoords: { key: string; x: number; y: number }[] = [
          { key: 'nw', x: l - 6, y: t - 6 },
          { key: 'n', x: cx, y: t - 6 },
          { key: 'ne', x: r + 6, y: t - 6 },
          { key: 'e', x: r + 6, y: cy },
          { key: 'se', x: r + 6, y: b + 6 },
          { key: 's', x: cx, y: b + 6 },
          { key: 'sw', x: l - 6, y: b + 6 },
          { key: 'w', x: l - 6, y: cy },
        ];
        const hitHandle = handleCoords.find((hc) => dist(world, hc) <= hitR);
        if (hitHandle) {
          g.mode = 'STRETCH_ITEMS';
          g.stretchHandle = hitHandle.key;
          g.stretchOrigBox = { l, t, r, b };
          g.dragOrigItems = [...page.items];
          workingItemsRef.current = [...page.items];
          return;
        }
      }
    }

    const hitMind = hitTestMindNode(world);
    if (tool === 'MINDMAP') {
      if (hitMind) {
        setSelectedIds(new Set([hitMind.id]));
        g.mode = 'DRAG_MIND';
        g.dragMindId = hitMind.id;
        g.dragOrigItems = [...page.items];
        workingItemsRef.current = [...page.items];
      } else {
        setSelectedIds(new Set());
        const root = mindNodesOnPage[0] || null;
        addMindNode(root, world, root ? 'Branch Idea' : 'Central Topic');
        g.active = false;
      }
      return;
    }

    if (selectedIds.size > 0 && tool !== 'ERASER') {
      const hitSel = hitTestAnyItem(world);
      if (hitSel && selectedIds.has(hitSel.id)) {
        g.mode = 'DRAG_ITEMS';
        g.dragOrigItems = [...page.items];
        workingItemsRef.current = [...page.items];
        return;
      }
      setSelectedIds(new Set());
      setEditingStickyId(null);
    }

    // Clicking a WebLink card with any non-drawing tool selects it cleanly
    if (tool !== 'ERASER' && tool !== 'PEN' && tool !== 'HIGHLIGHTER' && tool !== 'LASSO') {
      const hitAny = hitTestAnyItem(world);
      if (hitAny && hitAny.kind === 'weblink' && tool !== 'WEBLINK') {
        setSelectedIds(new Set([hitAny.id]));
        g.mode = 'DRAG_ITEMS';
        g.dragOrigItems = [...page.items];
        workingItemsRef.current = [...page.items];
        return;
      }
    }

    if (tool === 'PEN' || tool === 'HIGHLIGHTER') {
      g.mode = 'INK';
      workingItemsRef.current = [...page.items];
    } else if (tool === 'ERASER') {
      g.mode = 'ERASE';
      workingItemsRef.current = [...page.items];
      eraseInWorkingItems(world);
    } else if (tool === 'SHAPE') {
      g.mode = shapeKind === 'AUTO' ? 'INK' : 'SHAPE';
    } else if (tool === 'LASSO') {
      g.mode = 'LASSO';
      g.pts = [{ x: world.x, y: world.y }];
    } else if (tool === 'TAPE') {
      g.mode = 'TAPE';
    } else if (tool === 'LASER') {
      g.mode = 'LASER';
      lasersRef.current.push({ pts: [world], born: performance.now() });
    } else if (tool === 'WEBLINK') {
      const hit = hitTestAnyItem(world);
      if (hit && hit.kind === 'weblink') {
        setSelectedIds(new Set([hit.id]));
        setWebLinkModal({
          open: true,
          editId: hit.id,
          x: hit.x,
          y: hit.y,
          url: hit.url,
          title: hit.title,
          note: hit.note || '',
          color: hit.color,
        });
      } else {
        setWebLinkModal({
          open: true,
          editId: null,
          x: world.x - 160,
          y: world.y - 48,
          url: 'https://',
          title: '',
          note: '',
          color: WEBLINK_COLORS[0],
        });
      }
      g.active = false;
    } else if (tool === 'STICKY') {
      const hit = hitTestAnyItem(world);
      if (hit && hit.kind === 'sticky') {
        setSelectedIds(new Set([hit.id]));
        setEditingStickyId(hit.id);
      } else {
        const newSticky: StickyItem = {
          id: `stk-${Date.now()}`,
          kind: 'sticky',
          x: world.x - 130,
          y: world.y - 95,
          w: 260,
          h: 195,
          color: stickyColor,
          title: 'Quick Note',
          text: '- [ ] First study task\nKey takeaway...',
          style: 'classic',
          fontSize: 18,
          font: 'SANS',
        };
        commitPageItems((items) => [...items, newSticky]);
        setSelectedIds(new Set([newSticky.id]));
        setEditingStickyId(newSticky.id);
      }
      g.active = false;
    } else if (tool === 'TEXT') {
      const hit = hitTestAnyItem(world);
      if (hit && hit.kind === 'text') {
        setEditingTextId(hit.id);
        setTextDraft(hit.text);
      } else {
        const id = `txt-${Date.now()}`;
        const newTxt: TextItem = {
          id,
          kind: 'text',
          text: 'Type here',
          x: world.x,
          y: world.y - textSize * 0.5,
          size: textSize,
          color: penColor,
          font: textFont,
        };
        commitPageItems((items) => [...items, newTxt]);
        setEditingTextId(id);
        setTextDraft('');
      }
      g.active = false;
    }
    requestDraw();
  };

  const handlePointerMove = (e: React.PointerEvent<HTMLCanvasElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const sx = e.clientX - rect.left;
    const sy = e.clientY - rect.top;
    const world = screenToWorld(sx, sy);

    if (tool === 'ERASER') {
      hoverWorldRef.current = world;
      requestDraw();
    }

    const g = gestureRef.current;
    if (!g.active || !page) return;

    if (g.mode === 'PAN') {
      const dx = sx - g.lastScreen.x;
      const dy = sy - g.lastScreen.y;
      setCamera((c) => ({ ...c, panX: c.panX + dx, panY: c.panY + dy }));
      g.lastScreen = { x: sx, y: sy };
      return;
    }

    if (activeCollabRoom && onBroadcastCollabCursor) {
      onBroadcastCollabCursor(world.x, world.y, safeIdx);
    }

    if (g.mode === 'STRETCH_ITEMS' && g.stretchHandle && g.stretchOrigBox && g.dragOrigItems) {
      const ob = g.stretchOrigBox;
      const dx = world.x - g.startWorld.x;
      const dy = world.y - g.startWorld.y;
      let nl = ob.l;
      let nt = ob.t;
      let nr = ob.r;
      let nb = ob.b;
      const hKey = g.stretchHandle;
      if (hKey.includes('w')) nl = Math.min(ob.r - 24, ob.l + dx);
      if (hKey.includes('e')) nr = Math.max(ob.l + 24, ob.r + dx);
      if (hKey.includes('n')) nt = Math.min(ob.b - 24, ob.t + dy);
      if (hKey.includes('s')) nb = Math.max(ob.t + 24, ob.b + dy);
      const nextBox: StretchBox = { l: nl, t: nt, r: nr, b: nb };
      workingItemsRef.current = g.dragOrigItems.map((it) =>
        selectedIds.has(it.id) ? stretchPageItem(it, ob, nextBox) : it
      );
      g.lastWorld = world;
      requestDraw();
      return;
    }

    if (g.mode === 'RESIZE_STICKY' && g.resizeStickyId && g.origStickySize && workingItemsRef.current) {
      const dx = world.x - g.startWorld.x;
      const dy = world.y - g.startWorld.y;
      const nw = Math.max(140, Math.round(g.origStickySize.w + dx));
      const nh = Math.max(90, Math.round(g.origStickySize.h + dy));
      workingItemsRef.current = workingItemsRef.current.map((it) =>
        it.id === g.resizeStickyId && it.kind === 'sticky' ? { ...it, w: nw, h: nh } : it
      );
      requestDraw();
      return;
    }

    if (g.mode === 'DRAG_MIND' && g.dragMindId && g.dragOrigItems) {
      const dx = world.x - g.startWorld.x;
      const dy = world.y - g.startWorld.y;
      const snap = settings.whiteboardGridSnap ? 20 : 1;
      const orig = g.dragOrigItems.find((i) => i.id === g.dragMindId) as MindNodeItem | undefined;
      if (orig) {
        const nx = Math.round((orig.x + dx) / snap) * snap;
        const ny = Math.round((orig.y + dy) / snap) * snap;
        const ddx = nx - orig.x;
        const ddy = ny - orig.y;

        const moveIds = new Set<string>([g.dragMindId]);
        if (moveMindSubtree) {
          const allMind = g.dragOrigItems.filter((i): i is MindNodeItem => i.kind === 'mindnode');
          let added = true;
          while (added) {
            added = false;
            for (const mn of allMind) {
              if (mn.parentId && moveIds.has(mn.parentId) && !moveIds.has(mn.id)) {
                moveIds.add(mn.id);
                added = true;
              }
            }
          }
        }

        workingItemsRef.current = g.dragOrigItems.map((it) =>
          it.kind === 'mindnode' && moveIds.has(it.id)
            ? { ...it, x: it.x + ddx, y: it.y + ddy }
            : it
        );
      }
      g.lastWorld = world;
      requestDraw();
      return;
    }

    if (g.mode === 'DRAG_ITEMS' && g.dragOrigItems) {
      const dx = world.x - g.startWorld.x;
      const dy = world.y - g.startWorld.y;
      workingItemsRef.current = g.dragOrigItems.map((it) => {
        if (!selectedIds.has(it.id)) return it;
        if (it.kind === 'sticky' && it.pinned) return it;
        if (it.kind === 'ink') {
          return { ...it, points: it.points.map((p) => ({ ...p, x: p.x + dx, y: p.y + dy })) };
        }
        if (it.kind === 'fill') {
          return { ...it, points: it.points.map((p) => ({ x: p.x + dx, y: p.y + dy })) };
        }
        return { ...it, x: it.x + dx, y: it.y + dy };
      });
      g.lastWorld = world;
      requestDraw();
      return;
    }

    if (g.mode === 'INK') {
      if (circleHoldTimerRef.current !== null && dist(g.lastWorld, world) > 3.5) {
        window.clearTimeout(circleHoldTimerRef.current);
        circleHoldTimerRef.current = null;
        circleChargeRef.current = null;
      }

      const prev = g.pts[g.pts.length - 1];
      // Apply gentle input stabilization
      const stab = tool === 'PEN' ? penStabilizer * 0.55 : 0;
      const nx = prev ? prev.x * stab + world.x * (1 - stab) : world.x;
      const ny = prev ? prev.y * stab + world.y * (1 - stab) : world.y;
      const pt = { x: nx, y: ny, p: e.pressure || 0.65 };

      if (!prev || dist(prev, pt) >= 2.0) {
        g.pts.push(pt);
      }

      // LIVE SCRIBBLE-TO-ERASE WHILE DRAWING!
      if (
        settings.scribbleToErase &&
        settings.liveScribbleErase &&
        (tool === 'PEN' || tool === 'HIGHLIGHTER') &&
        isLiveScribble(g.pts, settings.scribbleSensitivity)
      ) {
        const isHl = tool === 'HIGHLIGHTER';
        const recent = g.pts.slice(-56);
        const curItems = workingItemsRef.current || page.items;
        const hits = curItems.filter((it) => scribbleHitsItem(recent, it, isHl));
        if (hits.length > 0) {
          g.scribbleConsumed = true;
          const hitIds = new Set(hits.map((h) => h.id));
          spawnPuffForItems(hits);
          workingItemsRef.current = curItems.filter((it) => !hitIds.has(it.id));
          g.pts = g.pts.slice(-14);
        }
      }

      // Hold-only Circle-to-Lasso (Pen tool ONLY, Laser NEVER converts)
      if (
        settings.circleToLasso &&
        tool === 'PEN' &&
        !g.scribbleConsumed &&
        circleHoldTimerRef.current === null &&
        isClosedLoop(g.pts)
      ) {
        const snapPts = [...g.pts];
        circleChargeRef.current = { x: world.x, y: world.y, born: performance.now() + 90 };
        circleHoldTimerRef.current = window.setTimeout(() => {
          circleHoldTimerRef.current = null;
          circleChargeRef.current = null;
          if (!gestureRef.current.active || gestureRef.current.mode !== 'INK') return;
          const picked = page.items.filter((it) => lassoSelectsItem(snapPts, it, settings.lassoTouching));
          setSelectedIds(new Set(picked.map((i) => i.id)));
          if (picked.length === 1 && picked[0].kind === 'sticky') {
            setEditingStickyId(picked[0].id);
          }
          ringsRef.current.push({
            x: world.x,
            y: world.y,
            maxR: 62,
            born: performance.now(),
            color: '#3D6CF4',
          });
          gestureRef.current.active = false;
          gestureRef.current.mode = 'NONE';
          gestureRef.current.pts = [];
          workingItemsRef.current = null;
          showToast(picked.length > 0 ? `${picked.length} selected via Hold-Lasso` : 'Loop held — nothing inside');
          requestDraw();
        }, 410);
      }

      requestDraw();
    } else if (g.mode === 'ERASE') {
      eraseInWorkingItems(world);
    } else if (g.mode === 'LASSO') {
      if (settings.lassoMode === 'RECT') {
        const s = g.startWorld;
        g.pts = [
          { x: s.x, y: s.y },
          { x: world.x, y: s.y },
          { x: world.x, y: world.y },
          { x: s.x, y: world.y },
        ];
      } else {
        g.pts.push({ x: world.x, y: world.y });
      }
      requestDraw();
    } else if (g.mode === 'LASER') {
      const last = lasersRef.current[lasersRef.current.length - 1];
      if (last) {
        last.pts.push(world);
        last.born = performance.now();
      }
      requestDraw();
    } else if (g.mode === 'SHAPE' || g.mode === 'TAPE') {
      requestDraw();
    }

    g.lastScreen = { x: sx, y: sy };
    g.lastWorld = world;
  };

  const handlePointerUp = () => {
    if (circleHoldTimerRef.current !== null) {
      window.clearTimeout(circleHoldTimerRef.current);
      circleHoldTimerRef.current = null;
    }
    circleChargeRef.current = null;
    const g = gestureRef.current;
    if (!g.active || !page) return;
    g.active = false;

    if (g.mode === 'PAN') {
      if (isWhiteboard) {
        onUpdateNotebook(notebook.id, (nb) => ({
          ...nb,
          pages: nb.pages.map((pg, idx) =>
            idx === safeIdx ? { ...pg, wbPanX: camera.panX, wbPanY: camera.panY, wbZoom: camera.zoom } : pg
          ),
        }));
      }
      g.mode = 'NONE';
      return;
    }

    if (g.mode === 'STRETCH_ITEMS') {
      if (workingItemsRef.current) {
        const finalItems = workingItemsRef.current;
        workingItemsRef.current = null;
        commitPageItems(() => finalItems, true);
      }
      g.stretchHandle = null;
      g.stretchOrigBox = null;
      g.dragOrigItems = null;
      g.mode = 'NONE';
      return;
    }

    if (g.mode === 'RESIZE_STICKY') {
      if (workingItemsRef.current) {
        const finalItems = workingItemsRef.current;
        workingItemsRef.current = null;
        commitPageItems(() => finalItems, true);
      }
      g.resizeStickyId = null;
      g.origStickySize = null;
      g.mode = 'NONE';
      return;
    }

    if (g.mode === 'DRAG_MIND' || g.mode === 'DRAG_ITEMS') {
      if (workingItemsRef.current && (dist(g.startWorld, g.lastWorld) > 1.5)) {
        const finalItems =
          g.mode === 'DRAG_MIND'
            ? clampMindNodesToPage(workingItemsRef.current, page.width, page.height, isWhiteboard)
            : workingItemsRef.current;
        workingItemsRef.current = null;
        commitPageItems(() => finalItems, true);
      } else {
        workingItemsRef.current = null;
      }
      g.dragMindId = null;
      g.dragOrigItems = null;
      g.mode = 'NONE';
      return;
    }

    if (g.mode === 'ERASE') {
      if (workingItemsRef.current && workingItemsRef.current !== page.items) {
        const finalItems = workingItemsRef.current;
        workingItemsRef.current = null;
        if (finalItems.length !== page.items.length || finalItems.some((it, i) => it !== page.items[i])) {
          commitPageItems(() => finalItems, true);
        }
      }
      workingItemsRef.current = null;
      g.mode = 'NONE';
      return;
    }

    if (g.mode === 'INK') {
      if (g.scribbleConsumed) {
        const finalItems = workingItemsRef.current || page.items;
        workingItemsRef.current = null;
        commitPageItems(() => finalItems, true);
        g.mode = 'NONE';
        g.pts = [];
        return;
      }
      workingItemsRef.current = null;

      if (
        settings.scribbleToErase &&
        (tool === 'PEN' || tool === 'HIGHLIGHTER') &&
        isScribble(g.pts, settings.scribbleSensitivity)
      ) {
        const hits = page.items.filter((it) => scribbleHitsItem(g.pts, it, tool === 'HIGHLIGHTER'));
        if (hits.length > 0) {
          const hitIds = new Set(hits.map((h) => h.id));
          spawnPuffForItems(hits);
          commitPageItems((items) => items.filter((it) => !hitIds.has(it.id)));
          g.mode = 'NONE';
          g.pts = [];
          return;
        }
      }

      if (tool === 'SHAPE' && shapeKind === 'AUTO') {
        const recognized = ShapeRecognizer.recognize(g.pts);
        if (recognized) {
          const ink: InkItem = {
            id: `shp-${Date.now()}`,
            kind: 'ink',
            points: recognized,
            color: penColor,
            size: penSize,
            isHighlighter: false,
            opacity: penOpacity,
            penKind: 'ballpoint',
          };
          commitPageItems((items) => [...items, ink]);
          g.mode = 'NONE';
          g.pts = [];
          return;
        }
      }

      let finalPts = g.pts;
      if (tool === 'HIGHLIGHTER' && g.pts.length > 1) {
        const len = pathLength(g.pts);
        if (
          settings.highlighterMode === 'STRAIGHT' ||
          (settings.highlighterMode === 'AUTO' && isNearlyStraight(g.pts, Math.max(10, len * 0.08)))
        ) {
          const a = g.pts[0];
          let b = g.pts[g.pts.length - 1];
          if (Math.abs(b.y - a.y) < Math.abs(b.x - a.x) * 0.12) b = { x: b.x, y: a.y };
          finalPts = [a, b];
        }
      }

      const isTap = g.pts.length < 4 && pathLength(g.pts) < 10;
      if (isTap) {
        const hitTape = page.items.find((it) => {
          if (it.kind !== 'tape') return false;
          const b = getItemBounds(it);
          return g.startWorld.x >= b.l && g.startWorld.x <= b.r && g.startWorld.y >= b.t && g.startWorld.y <= b.b;
        });
        if (hitTape && hitTape.kind === 'tape') {
          commitPageItems((items) =>
            items.map((it) => (it.id === hitTape.id && it.kind === 'tape' ? { ...it, revealed: !it.revealed } : it))
          );
          g.mode = 'NONE';
          g.pts = [];
          return;
        }
      }

      const ink: InkItem = {
        id: `ink-${Date.now()}-${Math.floor(Math.random() * 999)}`,
        kind: 'ink',
        points: finalPts,
        color: tool === 'HIGHLIGHTER' ? hlColor : penColor,
        size: tool === 'HIGHLIGHTER' ? hlSize : penSize,
        isHighlighter: tool === 'HIGHLIGHTER',
        opacity: tool === 'HIGHLIGHTER' ? settings.highlighterOpacity : penOpacity,
        penKind,
      };
      commitPageItems((items) => [...items, ink]);
      if (activeCollabRoom && onBroadcastCollabItem) {
        onBroadcastCollabItem(ink, safeIdx);
      }
    } else if (g.mode === 'SHAPE') {
      const shapes = ShapeBuilder.build(shapeKind, g.startWorld.x, g.startWorld.y, g.lastWorld.x, g.lastWorld.y);
      if (dist(g.startWorld, g.lastWorld) > 10) {
        const inks: InkItem[] = shapes.map((pts, idx) => ({
          id: `shp-${Date.now()}-${idx}`,
          kind: 'ink',
          points: pts,
          color: penColor,
          size: penSize,
          isHighlighter: false,
          opacity: penOpacity,
          penKind: 'ballpoint',
        }));
        commitPageItems((items) => [...items, ...inks]);
      }
    } else if (g.mode === 'TAPE') {
      const w = Math.abs(g.lastWorld.x - g.startWorld.x);
      if (w < 16) {
        const hitTape = page.items.find((it) => {
          if (it.kind !== 'tape') return false;
          const b = getItemBounds(it);
          return g.startWorld.x >= b.l && g.startWorld.x <= b.r && g.startWorld.y >= b.t && g.startWorld.y <= b.b;
        });
        if (hitTape && hitTape.kind === 'tape') {
          commitPageItems((items) =>
            items.map((it) => (it.id === hitTape.id && it.kind === 'tape' ? { ...it, revealed: !it.revealed } : it))
          );
        }
      } else {
        const l = Math.min(g.startWorld.x, g.lastWorld.x);
        const t = Math.min(g.startWorld.y, g.lastWorld.y);
        const h = Math.max(40, Math.abs(g.lastWorld.y - g.startWorld.y));
        commitPageItems((items) => [
          ...items,
          {
            id: `tape-${Date.now()}`,
            kind: 'tape',
            x: l,
            y: t,
            w,
            h,
            color: tapeColor,
            pattern: tapePattern,
            revealed: false,
          },
        ]);
      }
    } else if (g.mode === 'LASSO' && g.pts.length > 2) {
      const picked = page.items.filter((it) => lassoSelectsItem(g.pts, it, settings.lassoTouching));
      setSelectedIds(new Set(picked.map((i) => i.id)));
      if (picked.length === 1 && picked[0].kind === 'sticky') {
        setEditingStickyId(picked[0].id);
      }
    }

    g.mode = 'NONE';
    g.pts = [];
    requestDraw();
  };

  const handleWheel = (e: React.WheelEvent<HTMLCanvasElement>) => {
    e.preventDefault();
    if (e.ctrlKey || e.metaKey || isWhiteboard) {
      const rect = e.currentTarget.getBoundingClientRect();
      const sx = e.clientX - rect.left;
      const sy = e.clientY - rect.top;
      const factor = e.deltaY < 0 ? 1.09 : 0.92;
      setCamera((c) => {
        const nz = Math.max(0.18, Math.min(3.5, c.zoom * factor));
        const wx = (sx - c.panX) / c.zoom;
        const wy = (sy - c.panY) / c.zoom;
        return {
          zoom: nz,
          panX: sx - wx * nz,
          panY: sy - wy * nz,
        };
      });
    } else {
      setCamera((c) => ({
        ...c,
        panX: c.panX - e.deltaX,
        panY: c.panY - e.deltaY,
      }));
    }
  };

  // ================================================================
  // Study & Revision Tools (Local Note Digestion, Flashcards, Quiz, Slides)
  // ================================================================

  const handleAiSummary = async () => {
    setSummaryModal({
      open: true,
      title: `Key Points · ${notebook.title}`,
      summary: null,
      loadingStage: 'Reading notebook pages',
      explainText: null,
    });
    const txt = extractNotebookText(notebook.pages);
    const res = await runLocalSummary(
      txt,
      settings.activeLocalModelId,
      (stage) => setSummaryModal((s) => ({ ...s, loadingStage: stage })),
      settings
    );
    setSummaryModal({
      open: true,
      title: `Key Points · ${notebook.title}`,
      summary: res,
      loadingStage: null,
      explainText: null,
    });
  };

  const handleAiFlashcards = async () => {
    const txt = extractNotebookText(notebook.pages);
    const res = await runLocalFlashcards(
      txt,
      notebook.id,
      settings.activeLocalModelId,
      undefined,
      settings
    );
    if (res.cards.length > 0) {
      onAddFlashcards(res.cards);
      showToast(`Added ${res.cards.length} flashcards`);
    } else {
      showToast('Add definitions like "X is..." to extract cards');
    }
    setFlashModalOpen(true);
  };

  const handleAiQuiz = async () => {
    setQuizModal({
      open: true,
      questions: [],
      engineLabel: 'Study Practice',
      loadingStage: 'Extracting quiz concepts',
    });
    const txt = extractNotebookText(notebook.pages);
    const res = await runLocalQuiz(
      txt,
      settings.activeLocalModelId,
      (stage) => setQuizModal((q) => ({ ...q, loadingStage: stage })),
      settings
    );
    setQuizModal({
      open: true,
      questions: res.questions,
      engineLabel: 'Study Practice',
      loadingStage: null,
    });
  };

  const handleAiGenerateMindMap = async () => {
    const txt = extractNotebookText(notebook.pages) || notebook.title;
    const center = screenToWorld(
      (containerRef.current?.clientWidth || 800) / 2,
      (containerRef.current?.clientHeight || 600) / 2
    );
    const res = await generateMindMapFromText(
      txt,
      center.x,
      center.y,
      settings.activeLocalModelId,
      undefined,
      settings
    );
    commitPageItems((items) =>
      clampMindNodesToPage([...items, ...res.nodes], page?.width || 1200, page?.height || 1697, isWhiteboard)
    );
    setTool('MINDMAP');
    showToast('Mind Map created from notes');
  };

  const handleGenerateSlides = async (
    scope: 'CURRENT' | 'RANGE' | 'ALL',
    fromPage: number,
    toPage: number,
    slideCount: number
  ) => {
    const pickedPages =
      scope === 'CURRENT'
        ? [notebook.pages[safeIdx]].filter(Boolean)
        : scope === 'RANGE'
          ? notebook.pages.slice(Math.max(0, fromPage - 1), toPage)
          : notebook.pages;
    const text = extractNotebookText(pickedPages) || notebook.title;
    setSlideLoadingStage('Building revision slides...');
    try {
      const res = await runLocalSlideShow(
        notebook.title,
        text,
        slideCount,
        settings.activeLocalModelId,
        settings,
        (stage) => setSlideLoadingStage(stage)
      );
      if (onUpdateSlides) {
        onUpdateSlides(res.slides);
      } else {
        onUpdateNotebook(notebook.id, (nb) => ({ ...nb, slides: res.slides }));
      }
      setSlideEngineLabel('Deck Studio');
      showToast(`Created ${res.slides.length} revision slides`);
    } finally {
      setSlideLoadingStage(null);
    }
  };

  const handleMindMapToOutline = useCallback(() => {
    if (mindNodesOnPage.length === 0) {
      showToast('Add Mind Map nodes first');
      return;
    }
    const outlineItems = buildOutlineItemsFromMindNodes(mindNodesOnPage);
    if (outlineItems.length === 0) return;
    const newPg: Page = {
      id: `pg-outline-${Date.now()}`,
      width: page?.width || 1200,
      height: page?.height || 1697,
      paper: 'RULED',
      paperColor: page?.paperColor || settings.paperTint,
      items: outlineItems,
    };
    const nextIdx = notebook.pages.length;
    commitPages((pages) => [...pages, newPg]);
    setPageIdx(nextIdx);
    showToast('Created Outline page from Mind Map');
  }, [mindNodesOnPage, page, settings.paperTint, notebook.pages.length, commitPages, showToast]);

  const handleMindMapToFlashcards = useCallback(() => {
    if (mindNodesOnPage.length === 0) {
      showToast('Add Mind Map nodes first');
      return;
    }
    const cards: Flashcard[] = [];
    for (const n of mindNodesOnPage) {
      const kids = mindNodesOnPage.filter((c) => c.parentId === n.id);
      const backParts = [
        n.notes?.trim() || '',
        kids.length > 0 ? `Subtopics: ${kids.map((k) => k.label).join(' · ')}` : '',
      ].filter(Boolean);
      if (backParts.length === 0 && n.parentId) {
        const parent = mindNodesOnPage.find((p) => p.id === n.parentId);
        if (parent) backParts.push(`Branch of ${parent.label}`);
      }
      if (backParts.length > 0) {
        cards.push({
          id: `fc-mm-${n.id}-${Date.now()}`,
          notebookId: notebook.id,
          front: n.tag ? `[${n.tag}] ${n.label}` : n.label,
          back: backParts.join('\n'),
          box: 1,
          due: Date.now(),
          created: Date.now(),
        });
      }
    }
    if (cards.length > 0) {
      onAddFlashcards(cards);
      showToast(`Created ${cards.length} flashcards from Mind Map`);
      setFlashModalOpen(true);
    } else {
      showToast('Add notes or branches to Mind Map nodes to create flashcards');
    }
  }, [mindNodesOnPage, notebook.id, onAddFlashcards, showToast]);

  const handleImportFilesIntoNotebook = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;
    try {
      const results = await parseImportedFiles(files, settings.paperTint);
      const importedPages = results.flatMap((r) => r.pages);
      if (importedPages.length === 0) {
        showToast('No pages extracted from selected file(s)');
        return;
      }
      const targetIdx = notebook.pages.length;
      if (onAppendPages) {
        onAppendPages(importedPages);
      } else {
        commitPages((pages) => [...pages, ...importedPages]);
      }
      setPageIdx(targetIdx);
      showToast(`Imported ${importedPages.length} page(s) into notebook`);
    } catch {
      showToast('Could not import selected file');
    } finally {
      e.target.value = '';
    }
  };

  const handleInsertImage = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !page) return;
    const reader = new FileReader();
    reader.onload = () => {
      const src = reader.result as string;
      const img = new Image();
      img.onload = () => {
        const maxW = 420;
        const scale = Math.min(1, maxW / img.width);
        const w = Math.round(img.width * scale);
        const h = Math.round(img.height * scale);
        const center = screenToWorld(
          (containerRef.current?.clientWidth || 800) / 2,
          (containerRef.current?.clientHeight || 600) / 2
        );
        commitPageItems((items) => [
          ...items,
          { id: `img-${Date.now()}`, kind: 'image', src, x: center.x - w / 2, y: center.y - h / 2, w, h },
        ]);
      };
      img.src = src;
    };
    reader.readAsDataURL(file);
    e.target.value = '';
  };

  const handleExportPng = () => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const a = document.createElement('a');
    a.href = canvas.toDataURL('image/png');
    a.download = `${notebook.title}-page-${safeIdx + 1}.png`;
    a.click();
    showToast('Exported PNG image');
  };

  const addNewPage = () => {
    const newPg: Page = {
      id: `pg-${Date.now()}`,
      width: page?.width || 1200,
      height: page?.height || 1697,
      paper: page?.paper || notebook.paper,
      isWhiteboard: page?.isWhiteboard,
      paperColor: page?.paperColor,
      items: [],
    };
    commitPages((pages) => [...pages, newPg]);
    setPageIdx(notebook.pages.length);
  };

  const applyPenPreset = (preset: PenPreset) => {
    setPenKind(preset.kind);
    setPenColor(preset.color);
    setPenSize(preset.size);
    setPenOpacity(preset.opacity);
    setPenStabilizer(preset.stabilizer);
    onUpdateSettings({
      penName: preset.name,
      penKind: preset.kind,
      penColor: preset.color,
      penSize: preset.size,
      penOpacity: preset.opacity,
      penStabilizer: preset.stabilizer,
    });
    showToast(`Switched to "${preset.name}"`);
  };

  const searchHits = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    if (!q) return [];
    const hits: { pageIdx: number; preview: string }[] = [];
    notebook.pages.forEach((pg, idx) => {
      for (const it of pg.items) {
        const txt =
          it.kind === 'text'
            ? it.text
            : it.kind === 'sticky'
              ? `${it.title || ''} ${it.text}`
              : it.kind === 'weblink'
                ? `${it.title} ${it.url}`
                : it.kind === 'mindnode'
                  ? `${it.label} ${it.notes || ''}`
                  : '';
        if (txt && txt.toLowerCase().includes(q)) {
          hits.push({ pageIdx: idx, preview: txt.trim().slice(0, 72) });
        }
      }
      if (pg.pdfText && pg.pdfText.toLowerCase().includes(q)) {
        hits.push({ pageIdx: idx, preview: pg.pdfText.slice(0, 72) });
      }
    });
    return hits;
  }, [notebook.pages, searchQuery]);

  const splitNotebook = splitNoteId ? allNotebooks.find((n) => n.id === splitNoteId && n.deletedAt === null) : null;

  const TOOL_META: Record<ToolId, { label: string; icon: React.ComponentType<{ className?: string }> }> = {
    PEN: { label: 'Pen', icon: Pen },
    HIGHLIGHTER: { label: 'Highlight', icon: Highlighter },
    ERASER: { label: 'Eraser', icon: Eraser },
    SHAPE: { label: 'Shape', icon: Shapes },
    LASSO: { label: 'Lasso', icon: LassoIcon },
    MINDMAP: { label: 'Mind Map', icon: Network },
    TEXT: { label: 'Text', icon: Type },
    STICKY: { label: 'Sticky', icon: StickyNote },
    WEBLINK: { label: 'Web Link', icon: Link2 },
    TAPE: { label: 'Tape', icon: BookmarkMinus },
    STICKER: { label: 'Sticker', icon: Sticker },
    IMAGE: { label: 'Image', icon: ImageIcon },
    LASER: { label: 'Laser', icon: Wand2 },
    PAN: { label: 'Pan', icon: Hand },
  };

  const orderedTools = useMemo(() => {
    const saved = settings.toolbarOrder && settings.toolbarOrder.length > 0 ? settings.toolbarOrder : DEFAULT_TOOLBAR_ORDER;
    const merged = Array.from(new Set<ToolId>([...saved, ...DEFAULT_TOOLBAR_ORDER]));
    return merged.map((id) => ({ id, ...TOOL_META[id] }));
  }, [settings.toolbarOrder]);

  const selectedMindScreen = useMemo(() => {
    if (!selectedMindNode) return null;
    return {
      left: (selectedMindNode.x + selectedMindNode.w / 2) * camera.zoom + camera.panX,
      top: selectedMindNode.y * camera.zoom + camera.panY,
    };
  }, [selectedMindNode, camera]);

  const selectedStickyScreen = useMemo(() => {
    if (!selectedStickyNode) return null;
    const h = selectedStickyNode.collapsed ? 52 : selectedStickyNode.h;
    return {
      left: (selectedStickyNode.x + selectedStickyNode.w / 2) * camera.zoom + camera.panX,
      top: (selectedStickyNode.y + h) * camera.zoom + camera.panY + 10,
    };
  }, [selectedStickyNode, camera]);

  const selectedWebLinkScreen = useMemo(() => {
    if (!selectedWebLinkNode) return null;
    return {
      left: (selectedWebLinkNode.x + selectedWebLinkNode.w / 2) * camera.zoom + camera.panX,
      top: selectedWebLinkNode.y * camera.zoom + camera.panY - 12,
    };
  }, [selectedWebLinkNode, camera]);

  return (
    <div className="flex h-screen w-screen flex-col overflow-hidden bg-[#EFEBE2] text-[#1F2421] dark:bg-[#111311] dark:text-[#EDE8DB]">
      {/* Open Tabs Strip */}
      {settings.showTabs && openTabIds.length > 0 && (
        <LiquidGlassSurface
          enabled={settings.liquidGlass}
          variant="frost"
          radius={0}
          className="flex h-8 shrink-0 items-center gap-1 border-b border-[#1F2421]/10 px-2 dark:border-[#EDE8DB]/10"
        >
          <div className="flex h-full items-center gap-1 overflow-x-auto">
            {openTabIds.map((tid) => {
              const nb = allNotebooks.find((n) => n.id === tid);
              if (!nb) return null;
              const active = tid === notebook.id;
              return (
                <div
                  key={tid}
                  className={`group flex max-w-[180px] items-center gap-1.5 rounded-t-xl px-2.5 py-1 text-xs font-medium transition ${
                    active
                      ? 'bg-[#24483F] text-[#F5F2E8] shadow-sm dark:bg-[#8CC1AC] dark:text-[#151E1B]'
                      : 'text-[#5A5F58] hover:bg-black/5 dark:text-[#B1AC9F]'
                  }`}
                >
                  <button type="button" onClick={() => onSwitchNote(tid)} className="truncate">
                    {nb.title}
                  </button>
                  {openTabIds.length > 1 && (
                    <button
                      type="button"
                      onClick={() => onCloseTab(tid)}
                      className="rounded p-0.5 opacity-60 hover:bg-black/10 hover:opacity-100"
                    >
                      <X className="h-3 w-3" />
                    </button>
                  )}
                </div>
              );
            })}
          </div>
        </LiquidGlassSurface>
      )}

      {/* Main Editor Header Bar */}
      <LiquidGlassSurface
        enabled={settings.liquidGlass}
        variant="chrome"
        radius={0}
        className="relative z-30 shrink-0 border-b border-[#1F2421]/10 px-3 py-2 shadow-sm dark:border-[#EDE8DB]/10"
      >
        <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={onBackHome}
            title="Back to Library"
            className="rounded-xl p-2 hover:bg-black/8 dark:hover:bg-white/10"
          >
            <Home className="h-4 w-4" />
          </button>
          <button
            type="button"
            onClick={() => setPagesOpen((v) => !v)}
            title="Pages & Templates"
            className={`rounded-xl p-2 ${pagesOpen ? 'bg-black/10 dark:bg-white/15' : 'hover:bg-black/8 dark:hover:bg-white/10'}`}
          >
            <PanelLeft className="h-4 w-4" />
          </button>
          <button
            type="button"
            onClick={() => setSearchOpen((v) => !v)}
            title="Search notebook"
            className={`rounded-xl p-2 ${searchOpen ? 'bg-black/10 dark:bg-white/15' : 'hover:bg-black/8 dark:hover:bg-white/10'}`}
          >
            <Search className="h-4 w-4" />
          </button>
          <div className="mx-1 h-5 w-px bg-black/12 dark:bg-white/12" />
          <button
            type="button"
            onClick={handleUndo}
            disabled={undoStack.length === 0}
            title="Undo (Ctrl+Z)"
            className="rounded-xl p-2 disabled:opacity-30 hover:bg-black/8 dark:hover:bg-white/10"
          >
            <Undo2 className="h-4 w-4" />
          </button>
          <button
            type="button"
            onClick={handleRedo}
            disabled={redoStack.length === 0}
            title="Redo (Ctrl+Shift+Z)"
            className="rounded-xl p-2 disabled:opacity-30 hover:bg-black/8 dark:hover:bg-white/10"
          >
            <Redo2 className="h-4 w-4" />
          </button>
        </div>

        {/* iOS-Style Stretchable Draggable Tools Dock inside Optional LiquidGlassSurface */}
        <LiquidGlassSurface
          enabled={settings.liquidGlass}
          radius={16}
          className="relative flex items-center overflow-x-auto rounded-2xl border border-black/8 bg-white/85 px-1.5 py-1 shadow-sm dark:border-white/10 dark:bg-[#1B1D23]/90"
        >
        <div className="relative flex items-center gap-0.5">
          {orderedTools.map((t) => {
            const Icon = t.icon;
            const active = tool === t.id;
            const isHovered = hoveredToolId === t.id && settings.showHoverTooltips;
            return (
              <div
                key={t.id}
                className="relative flex shrink-0 flex-col items-center"
                onDragOver={(e) => e.preventDefault()}
                onDrop={() => {
                  if (!dragToolId || dragToolId === t.id) return;
                  const cur = orderedTools.map((x) => x.id);
                  const fromIdx = cur.indexOf(dragToolId);
                  const toIdx = cur.indexOf(t.id);
                  if (fromIdx !== -1 && toIdx !== -1) {
                    cur.splice(fromIdx, 1);
                    cur.splice(toIdx, 0, dragToolId);
                    onUpdateSettings({ toolbarOrder: cur });
                  }
                  setDragToolId(null);
                }}
              >
                <button
                  type="button"
                  draggable
                  data-dragging={dragToolId === t.id ? 'true' : undefined}
                  onDragStart={() => setDragToolId(t.id)}
                  onDragEnd={() => setDragToolId(null)}
                  onMouseEnter={() => setHoveredToolId(t.id)}
                  onMouseLeave={() => setHoveredToolId((cur) => (cur === t.id ? null : cur))}
                  onPointerEnter={() => setHoveredToolId(t.id)}
                  onPointerLeave={() => setHoveredToolId((cur) => (cur === t.id ? null : cur))}
                  title={`${t.label} (drag to reorder)`}
                  onClick={() => {
                    if (t.id === 'IMAGE') {
                      fileInputRef.current?.click();
                    } else if (t.id === 'STICKER') {
                      setStickerOpen((v) => !v);
                    } else {
                      setTool(t.id);
                      setStickerOpen(false);
                      setPenStudioOpen(false);
                    }
                  }}
                  className={`ios-stretch-tool flex items-center gap-1.5 rounded-xl px-2.5 py-1.5 text-xs font-semibold ${
                    active
                      ? 'text-white shadow-sm'
                      : 'text-slate-600 hover:bg-black/5 dark:text-slate-300 dark:hover:bg-white/10'
                  }`}
                  style={active ? { backgroundColor: accent.primary } : undefined}
                >
                  <Icon className="h-4 w-4 shrink-0" />
                  <span className="hidden xl:inline">{t.label}</span>
                </button>
                {isHovered && (
                  <div className="pointer-events-none fixed top-14 z-50 rounded-xl border border-black/10 bg-[#181920]/92 px-2.5 py-1 text-[11px] font-bold text-white shadow-lg backdrop-blur-md">
                    {t.label}
                  </div>
                )}
              </div>
            );
          })}
          <input ref={fileInputRef} type="file" accept="image/*" onChange={handleInsertImage} className="hidden" />
        </div>
        </LiquidGlassSurface>

        {/* Right Actions */}
        <div className="flex items-center gap-1">
          {settings.studyTimer && (
            <button
              type="button"
              onClick={() => setTimerRunning((r) => !r)}
              onContextMenu={(e) => {
                e.preventDefault();
                setPomodoroActive((p) => !p);
                setStudySec(0);
              }}
              onDoubleClick={() => {
                setPomodoroActive((p) => !p);
                setStudySec(0);
              }}
              title="Tap to pause/resume · Double-click to toggle Pomodoro focus timer"
              className={`ios-stretch hidden items-center gap-1.5 rounded-xl px-2.5 py-1.5 text-xs font-semibold shadow-sm md:inline-flex ${
                pomodoroActive
                  ? 'bg-amber-500/15 text-amber-700 dark:text-amber-300'
                  : 'bg-white/75 dark:bg-white/10'
              }`}
            >
              <Timer className={`h-3.5 w-3.5 ${timerRunning ? (pomodoroActive ? 'text-amber-500' : 'text-emerald-600') : 'text-slate-400'}`} />
              <span className="font-mono-num">
                {(() => {
                  const totalPom = (settings.pomodoroMinutes || 25) * 60;
                  const shown = pomodoroActive ? Math.max(0, totalPom - studySec) : studySec;
                  return `${Math.floor(shown / 60)}:${String(shown % 60).padStart(2, '0')}`;
                })()}
              </span>
            </button>
          )}

          {/* Quick Live Collab Study Pill */}
          <button
            type="button"
            onClick={() => setCollabModalOpen(true)}
            title="Live Collaborative Study Room"
            className={`ios-stretch hidden items-center gap-1 rounded-xl px-2.5 py-1.5 text-xs font-semibold shadow-xs sm:inline-flex ${
              activeCollabRoom
                ? 'bg-emerald-600 text-white'
                : 'bg-white/75 text-slate-700 hover:bg-white dark:bg-white/10 dark:text-slate-200'
            }`}
          >
            <Users className="h-3.5 w-3.5" />
            <span className="hidden lg:inline">
              {activeCollabRoom ? `${collabMembers.length} Live` : 'Collab'}
            </span>
          </button>

          <LiquidMenuButton
            align="right"
            menuWidthClass="w-64"
            trigger={() => (
              <span className="inline-flex items-center gap-1.5 rounded-full bg-[#24483F] px-3 py-1.5 text-xs font-semibold text-[#F5F2E8] shadow-sm dark:bg-[#8CC1AC] dark:text-[#151E1B]">
                <BookOpen className="h-3.5 w-3.5" />
                <span className="hidden sm:inline">Study Tools</span>
              </span>
            )}
            items={[
              {
                id: 'summary',
                label: 'Summarize Page Key Points',
                icon: <FileText className="h-4 w-4 text-[#2F6B4F]" />,
                onSelect: handleAiSummary,
              },
              {
                id: 'mindmap',
                label: 'Turn Notes into Mind Map',
                icon: <Brain className="h-4 w-4 text-emerald-600" />,
                onSelect: handleAiGenerateMindMap,
              },
              {
                id: 'flashcards',
                label: 'Make Flashcards from Notes',
                icon: <BookOpen className="h-4 w-4 text-blue-600" />,
                onSelect: handleAiFlashcards,
              },
              {
                id: 'quiz',
                label: 'Take Study Quiz',
                icon: <ListChecks className="h-4 w-4 text-amber-500" />,
                onSelect: handleAiQuiz,
              },
              {
                id: 'slides',
                label: 'Slide Show Revision Deck',
                icon: <Play className="h-4 w-4 text-indigo-600" />,
                onSelect: () => setSlideModalOpen(true),
              },
            ]}
          />

          {isWhiteboard ? (
            <LiquidButton
              onClick={() => setCamera({ panX: 80, panY: 60, zoom: 0.9 })}
              title="Reset whiteboard to origin"
              className="hidden !px-2.5 !py-1 !text-[11px] md:inline-flex"
            >
              <Layers className="h-3.5 w-3.5 text-emerald-600" />
              <span>Board</span>
            </LiquidButton>
          ) : (
            <LiquidGlassSurface
              enabled={settings.liquidGlass}
              variant="frost"
              radius={999}
              className="hidden items-center gap-0.5 px-1.5 py-0.5 text-[11px] font-semibold md:flex"
            >
              <div className="flex items-center gap-0.5">
                <button
                  type="button"
                  disabled={safeIdx === 0}
                  onClick={() => {
                    const ni = Math.max(0, safeIdx - 1);
                    setPageIdx(ni);
                    fitPageToView(notebook.pages[ni]);
                  }}
                  className="rounded p-0.5 hover:bg-black/10 disabled:opacity-30"
                  title="Previous page"
                >
                  <ChevronLeft className="h-3.5 w-3.5" />
                </button>
                <span className="font-mono-num px-1">
                  {safeIdx + 1}/{notebook.pages.length}
                </span>
                <button
                  type="button"
                  disabled={safeIdx >= notebook.pages.length - 1}
                  onClick={() => {
                    const ni = Math.min(notebook.pages.length - 1, safeIdx + 1);
                    setPageIdx(ni);
                    fitPageToView(notebook.pages[ni]);
                  }}
                  className="rounded p-0.5 hover:bg-black/10 disabled:opacity-30"
                  title="Next page"
                >
                  <ChevronRight className="h-3.5 w-3.5" />
                </button>
              </div>
            </LiquidGlassSurface>
          )}

          <LiquidButton
            onClick={addNewPage}
            title="Add new page"
            className="!h-8 !w-8 !p-0"
          >
            <Plus className="h-4 w-4" />
          </LiquidButton>

          <LiquidMenuButton
            align="right"
            menuWidthClass="w-64"
            title="More options"
            trigger={() => (
              <span className="liquid-glass-frost flex h-8 w-8 items-center justify-center rounded-full">
                <MoreHorizontal className="h-4 w-4" />
              </span>
            )}
            items={[
              {
                id: 'toggle-wb',
                label: isWhiteboard ? 'Use Standard Bounded Page' : 'Toggle Infinite Whiteboard',
                icon: <Layers className="h-4 w-4 text-emerald-600" />,
                onSelect: () => {
                  commitPages((pages) =>
                    pages.map((pg, i) => (i === safeIdx ? { ...pg, isWhiteboard: !pg.isWhiteboard } : pg))
                  );
                  showToast(isWhiteboard ? 'Switched to bounded page' : 'Switched to Infinite Whiteboard');
                },
              },
              {
                id: 'template',
                label: 'Paper Template & Tint…',
                icon: <Palette className="h-4 w-4 text-blue-600" />,
                onSelect: () => setTemplateSheetOpen(true),
              },
              {
                id: 'split',
                label: splitNoteId ? 'Close Split View' : 'Open Split Notes',
                icon: <Columns2 className="h-4 w-4 text-cyan-600" />,
                onSelect: () => {
                  const other = allNotebooks.find((n) => n.id !== notebook.id && n.deletedAt === null);
                  setSplitNoteId((cur) => (cur ? null : other?.id || notebook.id));
                },
              },
              {
                id: 'weblink',
                label: 'Add Web Link Card…',
                icon: <Link2 className="h-4 w-4 text-sky-600" />,
                onSelect: () => {
                  const c = screenToWorld(
                    (containerRef.current?.clientWidth || 800) / 2,
                    (containerRef.current?.clientHeight || 600) / 2
                  );
                  setWebLinkModal({
                    open: true,
                    editId: null,
                    x: c.x - 160,
                    y: c.y - 48,
                    url: 'https://',
                    title: '',
                    note: '',
                    color: WEBLINK_COLORS[0],
                  });
                },
              },
              {
                id: 'slides',
                dividerBefore: true,
                label: `Slide Show Revision (${notebook.slides?.length || 0})`,
                icon: <Play className="h-4 w-4 text-indigo-600" />,
                onSelect: () => setSlideModalOpen(true),
              },
              {
                id: 'flashcards',
                label: `Open Flashcards (${flashcards.length})`,
                icon: <BookOpen className="h-4 w-4 text-amber-500" />,
                onSelect: () => setFlashModalOpen(true),
              },
              {
                id: 'import',
                dividerBefore: true,
                label: 'Import Files / Notes…',
                icon: <Upload className="h-4 w-4 text-emerald-600" />,
                onSelect: () => importFileRef.current?.click(),
              },
              {
                id: 'export',
                label: 'Export Current View as PNG',
                icon: <Download className="h-4 w-4 text-indigo-600" />,
                onSelect: handleExportPng,
              },
              {
                id: 'settings',
                dividerBefore: true,
                label: 'Studio Settings',
                icon: <Settings className="h-4 w-4" />,
                onSelect: onOpenSettings,
              },
            ]}
          />
          <input
            ref={importFileRef}
            type="file"
            multiple
            accept=".md,.markdown,.txt,.html,.htm,.csv,.json,.pdf,image/*"
            onChange={handleImportFilesIntoNotebook}
            className="hidden"
          />
        </div>
        </div>
      </LiquidGlassSurface>

      {/* Secondary Contextual Tool Options Bar */}
      <LiquidGlassSurface
        enabled={settings.liquidGlass}
        variant="chrome"
        radius={0}
        className="relative z-20 shrink-0 border-b border-[#1F2421]/10 px-4 py-1.5 text-xs dark:border-[#EDE8DB]/10"
      >
        <div className="flex flex-wrap items-center justify-center gap-3">
        {(tool === 'PEN' || tool === 'SHAPE' || tool === 'TEXT') && (
          <>
            {tool === 'PEN' && (
              <>
                <button
                  type="button"
                  onClick={() => setPenStudioOpen((v) => !v)}
                  className="inline-flex items-center gap-1.5 rounded-xl border border-black/10 bg-black/[0.04] px-2.5 py-1 text-[11px] font-bold hover:bg-black/8 dark:border-white/12 dark:bg-white/8"
                >
                  <Sliders className="h-3 w-3 text-blue-600" />
                  <span>{settings.penName || 'Pro Stylus'}</span>
                  <span className="capitalize text-slate-500">· {penKind}</span>
                  <ChevronDown className="h-3 w-3 text-slate-400" />
                </button>

                {/* Favorite Pen Presets Quick Chips */}
                <div className="hidden items-center gap-1 md:flex">
                  {settings.penPresets.slice(0, 4).map((p) => (
                    <button
                      key={p.id}
                      type="button"
                      onClick={() => applyPenPreset(p)}
                      className={`inline-flex items-center gap-1 rounded-lg px-2 py-0.5 text-[11px] font-semibold transition ${
                        penKind === p.kind && penColor === p.color
                          ? 'bg-blue-600/12 text-blue-700 dark:text-blue-300'
                          : 'hover:bg-black/5 text-slate-600 dark:text-slate-300'
                      }`}
                    >
                      <span className="h-2.5 w-2.5 rounded-full border border-black/15" style={{ backgroundColor: p.color }} />
                      {p.name}
                    </button>
                  ))}
                </div>
              </>
            )}

            <div className="flex items-center gap-1.5">
              {PEN_COLORS.map((c) => (
                <button
                  key={c}
                  type="button"
                  onClick={() => setPenColor(c)}
                  className={`h-5 w-5 rounded-full border transition ${
                    penColor === c ? 'scale-115 ring-2 ring-blue-500 ring-offset-1' : 'border-black/20'
                  }`}
                  style={{ backgroundColor: c }}
                />
              ))}
              <label className="relative flex h-5 w-5 cursor-pointer items-center justify-center overflow-hidden rounded-full border border-black/20 bg-gradient-to-tr from-rose-500 via-amber-400 to-blue-500" title="Custom color">
                <input
                  type="color"
                  value={penColor}
                  onChange={(e) => setPenColor(e.target.value)}
                  className="absolute inset-0 cursor-pointer opacity-0"
                />
              </label>
            </div>

            {tool === 'TEXT' ? (
              <div className="flex items-center gap-2">
                <LiquidMenuButton
                  align="left"
                  label={textFont === 'HAND' ? 'Handwriting' : textFont}
                  items={(
                    [
                      { id: 'SANS', label: 'Sans' },
                      { id: 'SERIF', label: 'Serif' },
                      { id: 'MONO', label: 'Mono' },
                      { id: 'HAND', label: 'Handwriting' },
                    ] as const
                  ).map((f) => ({
                    id: f.id,
                    label: f.label,
                    selected: textFont === f.id,
                    onSelect: () => setTextFont(f.id),
                  }))}
                />
                <LiquidSlider
                  min={16}
                  max={64}
                  step={1}
                  value={textSize}
                  onChange={setTextSize}
                  className="w-24"
                />
                <span className="font-mono-num text-[11px]">{textSize}px</span>
              </div>
            ) : (
              <div className="flex items-center gap-2">
                <span className="text-[11px] text-[#5A5F58] dark:text-[#B1AC9F]">Size</span>
                <LiquidSlider
                  min={1.5}
                  max={16}
                  step={0.5}
                  value={penSize}
                  onChange={setPenSize}
                  className="w-24"
                />
                <span className="font-mono-num text-[11px]">{penSize}</span>
              </div>
            )}

            {tool === 'SHAPE' && (
              <LiquidMenuButton
                align="left"
                label={SHAPE_KINDS.find((sk) => sk.id === shapeKind)?.label || shapeKind}
                items={SHAPE_KINDS.map((sk) => ({
                  id: sk.id,
                  label: sk.label,
                  selected: shapeKind === sk.id,
                  onSelect: () => setShapeKind(sk.id),
                }))}
              />
            )}

            {tool === 'PEN' && settings.liveScribbleErase && (
              <span className="hidden text-[11px] font-medium text-emerald-700 dark:text-emerald-300 lg:inline">
                • Live scribble-erase ({settings.scribbleSensitivity})
              </span>
            )}
          </>
        )}

        {tool === 'HIGHLIGHTER' && (
          <>
            <div className="flex items-center gap-1.5">
              {HL_COLORS.map((c) => (
                <button
                  key={c}
                  type="button"
                  onClick={() => setHlColor(c)}
                  className={`h-5 w-5 rounded-full border ${
                    hlColor === c ? 'scale-115 ring-2 ring-[#24483F] dark:ring-[#8CC1AC]' : 'border-black/15'
                  }`}
                  style={{ backgroundColor: c }}
                />
              ))}
            </div>
            <LiquidSlider
              min={12}
              max={44}
              step={1}
              value={hlSize}
              onChange={setHlSize}
              accentColor="#D97706"
              className="w-28"
            />
            <span className="font-mono-num text-[11px]">{hlSize}px</span>
          </>
        )}

        {tool === 'ERASER' && (
          <div className="flex flex-wrap items-center gap-3">
            <LiquidSegmentedControl
              value={eraserMode}
              accentColor="#9B3B3B"
              onChange={(m) => {
                setEraserMode(m);
                onUpdateSettings({ eraserMode: m });
              }}
              options={[
                { value: 'STROKE', label: 'Stroke' },
                { value: 'PIXEL', label: 'Precision' },
                { value: 'HIGHLIGHTER', label: 'Highlighter' },
              ]}
            />
            <div className="flex items-center gap-2">
              <span className="text-[11px] font-semibold">Size</span>
              <LiquidSlider
                min={8}
                max={68}
                step={1}
                value={eraserSize}
                onChange={setEraserSize}
                accentColor="#9B3B3B"
                className="w-28"
              />
              <span className="font-mono-num text-[11px]">{eraserSize}px</span>
            </div>
            <div className="hidden items-center gap-1.5 sm:flex">
              <span className="text-[11px] text-[#5A5F58] dark:text-[#B1AC9F]">Scribble:</span>
              <LiquidSegmentedControl
                value={settings.scribbleSensitivity}
                onChange={(s) => onUpdateSettings({ scribbleSensitivity: s })}
                options={[
                  { value: 'gentle', label: 'Gentle' },
                  { value: 'balanced', label: 'Balanced' },
                  { value: 'instant', label: 'Instant' },
                ]}
              />
            </div>
            <LiquidButton
              onClick={() => {
                const inkItems = page?.items.filter((i) => i.kind === 'ink') || [];
                if (inkItems.length === 0) return;
                spawnPuffForItems(inkItems);
                commitPageItems((items) => items.filter((i) => i.kind !== 'ink'));
                showToast('Cleared all ink on page');
              }}
              className="!px-3 !py-1 !text-[11px] text-[#9B3B3B] dark:text-[#E58A8A]"
            >
              Clear Page Ink
            </LiquidButton>
          </div>
        )}

        {tool === 'TAPE' && (
          <div className="flex items-center gap-2">
            <LiquidMenuButton
              align="left"
              label={TAPE_PATTERNS.find((tp) => tp.id === tapePattern)?.label || tapePattern}
              items={TAPE_PATTERNS.map((tp) => ({
                id: tp.id,
                label: tp.label,
                selected: tapePattern === tp.id,
                onSelect: () => setTapePattern(tp.id),
              }))}
            />
            {['#7161E0', '#2BB673', '#F2994A', '#EB5757', '#3D6CF4'].map((c) => (
              <button
                key={c}
                type="button"
                onClick={() => setTapeColor(c)}
                className={`h-5 w-5 rounded-full ${tapeColor === c ? 'ring-2 ring-blue-500' : ''}`}
                style={{ backgroundColor: c }}
              />
            ))}
          </div>
        )}

        {tool === 'STICKY' && (
          <div className="flex items-center gap-2">
            <span className="text-[11px] text-slate-500">Default Sticky Color:</span>
            {STICKY_COLORS.map((c) => (
              <button
                key={c}
                type="button"
                onClick={() => setStickyColor(c)}
                className={`h-5 w-5 rounded-md border border-black/15 ${
                  stickyColor === c ? 'ring-2 ring-blue-500' : ''
                }`}
                style={{ backgroundColor: c }}
              />
            ))}
            <span className="text-[11px] text-slate-400">· Tap canvas to place or tap sticky to customize</span>
          </div>
        )}

        {tool === 'MINDMAP' && (
          <div className="flex flex-wrap items-center gap-1.5">
            <button
              type="button"
              onClick={() =>
                addMindNode(
                  selectedMindNode || mindNodesOnPage[0] || null,
                  undefined,
                  mindNodesOnPage.length === 0 ? 'Central Topic' : 'Sub-topic'
                )
              }
              className="inline-flex items-center gap-1 whitespace-nowrap rounded-lg bg-blue-600 px-2.5 py-1 text-[11px] font-semibold text-white"
            >
              <Plus className="h-3 w-3" /> {mindNodesOnPage.length === 0 ? 'Root Node' : 'Add Branch'}
            </button>
            {selectedMindNode && (
              <button
                type="button"
                onClick={() => addSiblingMindNode(selectedMindNode)}
                className="inline-flex items-center gap-1 whitespace-nowrap rounded-lg bg-black/5 px-2.5 py-1 text-[11px] font-semibold hover:bg-black/10 dark:bg-white/10"
              >
                <Plus className="h-3 w-3" /> Sibling
              </button>
            )}
            <button
              type="button"
              onClick={() => setMoveMindSubtree((v) => !v)}
              title="Move entire branch subtree when dragging a node"
              className={`inline-flex items-center gap-1 whitespace-nowrap rounded-lg px-2.5 py-1 text-[11px] font-semibold transition ${
                moveMindSubtree
                  ? 'bg-emerald-600/15 text-emerald-700 dark:text-emerald-300'
                  : 'bg-black/5 text-slate-600 dark:bg-white/10 dark:text-slate-300'
              }`}
            >
              <Move className="h-3 w-3" /> Subtree Drag
            </button>
            <button
              type="button"
              onClick={() => autoLayoutMindMap('RADIAL')}
              className="inline-flex items-center gap-1 whitespace-nowrap rounded-lg bg-black/5 px-2.5 py-1 text-[11px] font-semibold hover:bg-black/10 dark:bg-white/10"
            >
              <Network className="h-3 w-3" /> Radial
            </button>
            <button
              type="button"
              onClick={() => autoLayoutMindMap('TREE')}
              className="inline-flex items-center gap-1 whitespace-nowrap rounded-lg bg-black/5 px-2.5 py-1 text-[11px] font-semibold hover:bg-black/10 dark:bg-white/10"
            >
              <GitBranch className="h-3 w-3" /> Tree
            </button>
            <button
              type="button"
              onClick={() => autoLayoutMindMap('ORG_CHART')}
              className="inline-flex items-center gap-1 whitespace-nowrap rounded-lg bg-black/5 px-2.5 py-1 text-[11px] font-semibold hover:bg-black/10 dark:bg-white/10"
            >
              <Layers className="h-3 w-3" /> Org Chart
            </button>
            <button
              type="button"
              onClick={handleMindMapToOutline}
              className="inline-flex items-center gap-1 whitespace-nowrap rounded-lg bg-black/5 px-2.5 py-1 text-[11px] font-semibold hover:bg-black/10 dark:bg-white/10"
            >
              <FileText className="h-3 w-3" /> Outline → Page
            </button>
            <button
              type="button"
              onClick={handleMindMapToFlashcards}
              className="inline-flex items-center gap-1 whitespace-nowrap rounded-lg bg-black/5 px-2.5 py-1 text-[11px] font-semibold hover:bg-black/10 dark:bg-white/10"
            >
              <BookOpen className="h-3 w-3" /> → Flashcards
            </button>
            <button
              type="button"
              onClick={fitMindMapToView}
              className="inline-flex items-center gap-1 whitespace-nowrap rounded-lg bg-blue-600/12 px-2.5 py-1 text-[11px] font-semibold text-blue-700 hover:bg-blue-600/20 dark:text-blue-300"
            >
              <Compass className="h-3 w-3" /> Fit Map
            </button>
          </div>
        )}

        {tool === 'WEBLINK' && (
          <div className="flex items-center gap-2">
            <span className="text-[11px] text-slate-500">
              Tap anywhere on the page to embed a Web Link Card, or tap an existing link card to open or edit it
            </span>
          </div>
        )}

        {tool === 'LASSO' && (
          <span className="text-[11px] text-[#5A5F58] dark:text-[#B1AC9F]">
            Draw a loop around ink, text, stickies, links, or nodes to move or delete
          </span>
        )}
        {tool === 'PAN' && (
          <span className="text-[11px] text-[#5A5F58] dark:text-[#B1AC9F]">
            Drag to pan canvas · Scroll or pinch to zoom freely
          </span>
        )}

        {/* Pen Customisation Studio Popover */}
        {penStudioOpen && (
          <LiquidGlassSurface
            enabled={settings.liquidGlass}
            variant="card"
            radius={24}
            className="absolute left-1/2 top-full z-50 mt-2 w-[360px] -translate-x-1/2 p-4 shadow-2xl animate-stagger-in"
          >
            <div className="mb-3 flex items-center justify-between">
              <span className="text-sm font-bold">Pen Studio</span>
              <LiquidButton onClick={() => setPenStudioOpen(false)} className="!h-7 !w-7 !p-0">
                <X className="h-3.5 w-3.5" />
              </LiquidButton>
            </div>

            {/* Live Stroke Preview */}
            <div className="relative mb-3 flex h-16 items-center justify-center overflow-hidden rounded-2xl border border-[#24483F]/25 bg-[#111513]">
              <svg viewBox="0 0 280 54" className="relative z-10 h-full w-full px-3">
                <path
                  d="M 14 34 C 55 10, 95 46, 140 24 C 185 6, 225 44, 266 22"
                  fill="none"
                  stroke={penColor === '#14171F' || penColor === '#1E2321' ? '#8CC1AC' : penColor}
                  strokeWidth={penSize}
                  strokeOpacity={penOpacity}
                  strokeLinecap={penKind === 'fineliner' ? 'butt' : 'round'}
                  strokeDasharray={penKind === 'pencil' ? '1.2 0.8' : undefined}
                />
              </svg>
            </div>

            <div className="mb-3 flex items-center gap-2">
              <span className="text-[11px] font-semibold text-[#5A5F58] dark:text-[#B1AC9F]">Pen Name</span>
              <LiquidInput
                type="text"
                value={settings.penName}
                onChange={(e) => onUpdateSettings({ penName: e.target.value })}
                wrapperClassName="flex-1 !py-1"
              />
            </div>

            <div className="mb-3 grid grid-cols-5 gap-1">
              {PEN_NIBS.map((n) => (
                <button
                  key={n.id}
                  type="button"
                  title={n.desc}
                  onClick={() => {
                    setPenKind(n.id);
                    onUpdateSettings({ penKind: n.id });
                  }}
                  className={`rounded-xl py-1.5 text-[10px] font-bold transition ${
                    penKind === n.id
                      ? 'bg-[#24483F] text-[#F5F2E8] shadow-sm dark:bg-[#8CC1AC] dark:text-[#151E1B]'
                      : 'liquid-glass-frost text-[#5A5F58] dark:text-[#B1AC9F]'
                  }`}
                >
                  {n.label}
                </button>
              ))}
            </div>

            <div className="space-y-2 text-xs">
              <div className="flex items-center justify-between gap-2">
                <span className="w-20 text-[#5A5F58] dark:text-[#B1AC9F]">Thickness</span>
                <LiquidSlider
                  min={1.5}
                  max={16}
                  step={0.5}
                  value={penSize}
                  onChange={setPenSize}
                  className="flex-1"
                />
                <span className="font-mono-num w-10 text-right font-semibold">{penSize}px</span>
              </div>
              <div className="flex items-center justify-between gap-2">
                <span className="w-20 text-[#5A5F58] dark:text-[#B1AC9F]">Opacity</span>
                <LiquidSlider
                  min={0.35}
                  max={1}
                  step={0.05}
                  value={penOpacity}
                  onChange={setPenOpacity}
                  className="flex-1"
                />
                <span className="font-mono-num w-10 text-right font-semibold">
                  {Math.round(penOpacity * 100)}%
                </span>
              </div>
              <div className="flex items-center justify-between gap-2">
                <span className="w-20 text-[#5A5F58] dark:text-[#B1AC9F]">Stabilizer</span>
                <LiquidSlider
                  min={0}
                  max={0.85}
                  step={0.05}
                  value={penStabilizer}
                  onChange={setPenStabilizer}
                  className="flex-1"
                />
                <span className="font-mono-num w-10 text-right font-semibold">
                  {Math.round(penStabilizer * 100)}%
                </span>
              </div>
            </div>

            <div className="mt-3 border-t border-[#1F2421]/10 pt-3 dark:border-[#EDE8DB]/10">
              <div className="mb-1.5 text-[10px] font-bold uppercase tracking-wider text-[#5A5F58] dark:text-[#B1AC9F]">
                Favorite Presets
              </div>
              <div className="mb-2 flex flex-wrap gap-1.5">
                {settings.penPresets.map((p) => (
                  <div
                    key={p.id}
                    className="liquid-glass-frost inline-flex items-center gap-1 rounded-xl px-2 py-1 text-[11px] font-semibold"
                  >
                    <button
                      type="button"
                      onClick={() => applyPenPreset(p)}
                      className="flex items-center gap-1.5"
                    >
                      <span className="h-2.5 w-2.5 rounded-full" style={{ backgroundColor: p.color }} />
                      <span>{p.name}</span>
                    </button>
                    {settings.penPresets.length > 1 && (
                      <button
                        type="button"
                        onClick={() => onDeletePenPreset(p.id)}
                        className="ml-0.5 opacity-60 hover:text-red-500 hover:opacity-100"
                      >
                        <X className="h-3 w-3" />
                      </button>
                    )}
                  </div>
                ))}
              </div>
              <div className="flex gap-1.5">
                <LiquidInput
                  type="text"
                  value={newPresetName}
                  onChange={(e) => setNewPresetName(e.target.value)}
                  placeholder="Preset name..."
                  wrapperClassName="flex-1 !py-1"
                />
                <LiquidButton
                  tint="#24483F"
                  onClick={() => {
                    const name = newPresetName.trim() || `${penKind} ${Math.round(penSize)}px`;
                    onSavePenPreset({
                      id: `preset-${Date.now()}`,
                      name,
                      kind: penKind,
                      color: penColor,
                      size: penSize,
                      opacity: penOpacity,
                      stabilizer: penStabilizer,
                    });
                    setNewPresetName('');
                    showToast(`Saved preset "${name}"`);
                  }}
                  className="!px-3 !py-1 !text-xs"
                >
                  Save
                </LiquidButton>
              </div>
            </div>
          </LiquidGlassSurface>
        )}
        </div>
      </LiquidGlassSurface>

      {/* Main Workspace Area */}
      <div className="relative flex min-h-0 flex-1 overflow-hidden">
        {pagesOpen && (
          <LiquidGlassSurface
            enabled={settings.liquidGlass}
            variant="chrome"
            radius={0}
            className="z-20 flex w-52 shrink-0 flex-col border-r border-[#1F2421]/10 p-3 dark:border-[#EDE8DB]/10"
          >
            <div className="mb-2 flex items-center justify-between">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-500">
                Pages ({notebook.pages.length})
              </span>
              <button
                type="button"
                onClick={addNewPage}
                className="rounded-lg bg-blue-600/12 p-1 text-blue-600 hover:bg-blue-600/20"
              >
                <Plus className="h-3.5 w-3.5" />
              </button>
            </div>
            <div className="flex-1 space-y-2.5 overflow-y-auto pr-1">
              {notebook.pages.map((pg, idx) => (
                <div
                  key={pg.id}
                  onClick={() => {
                    setPageIdx(idx);
                    fitPageToView(pg);
                  }}
                  className={`group relative flex cursor-pointer flex-col items-center rounded-xl border p-2 transition ${
                    idx === safeIdx
                      ? 'border-blue-600 bg-blue-50/60 dark:bg-blue-950/30'
                      : 'border-black/8 hover:border-black/20 dark:border-white/10'
                  }`}
                >
                  <PaperPreviewCanvas
                    paper={pg.paper}
                    paperColor={pg.paperColor || settings.paperTint}
                    isWhiteboard={pg.isWhiteboard}
                    width={90}
                    height={122}
                    className="rounded border border-black/10 shadow-sm"
                  />
                  <span className="font-mono-num mt-1 text-[11px] font-semibold">
                    {idx + 1} {pg.isWhiteboard ? '· Board' : ''}
                  </span>
                  {notebook.pages.length > 1 && (
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        commitPages((pages) => pages.filter((_, i) => i !== idx));
                        setPageIdx(Math.max(0, idx - 1));
                      }}
                      className="absolute right-1.5 top-1.5 hidden rounded bg-white/90 p-1 text-red-500 shadow group-hover:block dark:bg-black/70"
                    >
                      <Trash2 className="h-3 w-3" />
                    </button>
                  )}
                </div>
              ))}
            </div>
          </LiquidGlassSurface>
        )}

        {/* Primary Canvas Container */}
        <div ref={containerRef} className="relative min-w-0 flex-1 overflow-hidden">
          <canvas
            ref={canvasRef}
            onPointerDown={handlePointerDown}
            onPointerMove={handlePointerMove}
            onPointerUp={handlePointerUp}
            onPointerCancel={handlePointerUp}
            onPointerLeave={() => {
              hoverWorldRef.current = null;
              requestDraw();
            }}
            onWheel={handleWheel}
            className={`h-full w-full touch-none ${
              spaceDown || tool === 'PAN'
                ? 'cursor-grab active:cursor-grabbing'
                : tool === 'ERASER'
                  ? 'cursor-none'
                  : 'cursor-crosshair'
            }`}
          />

          {/* Floating Sticker Picker */}
          {stickerOpen && (
            <LiquidGlassSurface
              enabled={settings.liquidGlass}
              variant="card"
              radius={22}
              className="absolute left-1/2 top-3 z-40 w-72 -translate-x-1/2 p-3.5 shadow-2xl animate-stagger-in"
            >
              <div className="mb-2 flex items-center justify-between">
                <span className="text-[11px] font-bold text-[#5A5F58] dark:text-[#B1AC9F]">Tap a sticker to place</span>
              </div>
              <div className="grid grid-cols-6 gap-1.5">
                {STICKERS.map((s) => (
                  <button
                    key={s}
                    type="button"
                    onClick={() => {
                      const c = screenToWorld(
                        (containerRef.current?.clientWidth || 800) / 2,
                        (containerRef.current?.clientHeight || 600) / 2
                      );
                      commitPageItems((items) => [
                        ...items,
                        { id: `st-${Date.now()}`, kind: 'text', text: s, x: c.x - 28, y: c.y - 28, size: 58, color: '#000', font: 'SANS' },
                      ]);
                      setStickerOpen(false);
                    }}
                    className="ios-stretch rounded-xl p-1.5 text-2xl hover:bg-black/5 dark:hover:bg-white/10"
                  >
                    {s}
                  </button>
                ))}
              </div>
            </LiquidGlassSurface>
          )}

          {/* Contextual Floating Sticky Studio */}
          {selectedStickyNode && selectedStickyScreen && (
            <LiquidGlassSurface
              enabled={settings.liquidGlass}
              variant="card"
              radius={24}
              style={{
                left: Math.max(185, Math.min((containerRef.current?.clientWidth || 800) - 185, selectedStickyScreen.left)),
                top: Math.max(14, Math.min((containerRef.current?.clientHeight || 600) - 350, selectedStickyScreen.top)),
              }}
              className="absolute z-40 w-84 -translate-x-1/2 p-3.5 shadow-2xl animate-stagger-in"
            >
              <div className="mb-2 flex items-center justify-between">
                <span className="text-xs font-bold">Sticky Studio</span>
                <div className="flex items-center gap-1">
                  <button
                    type="button"
                    onClick={() => {
                      const nextTxt = selectedStickyNode.text
                        ? `${selectedStickyNode.text.replace(/\s+$/, '')}\n- [ ] New task`
                        : '- [ ] New task';
                      commitPageItems((items) =>
                        items.map((it) =>
                          it.id === selectedStickyNode.id && it.kind === 'sticky' ? { ...it, text: nextTxt } : it
                        )
                      );
                    }}
                    title="Append interactive checklist task"
                    className="inline-flex items-center gap-0.5 rounded-lg bg-emerald-500/12 px-2 py-1 text-[10px] font-bold text-emerald-700 hover:bg-emerald-500/20 dark:text-emerald-300"
                  >
                    <Plus className="h-3 w-3" /> Task
                  </button>
                  <button
                    type="button"
                    title={selectedStickyNode.pinned ? 'Unpin' : 'Pin sticky'}
                    onClick={() =>
                      commitPageItems((items) =>
                        items.map((it) =>
                          it.id === selectedStickyNode.id && it.kind === 'sticky'
                            ? { ...it, pinned: !it.pinned }
                            : it
                        )
                      )
                    }
                    className={`rounded-lg p-1 ${selectedStickyNode.pinned ? 'bg-red-500/15 text-red-600' : 'text-slate-400 hover:bg-black/5'}`}
                  >
                    {selectedStickyNode.pinned ? <PinOff className="h-3.5 w-3.5" /> : <Pin className="h-3.5 w-3.5" />}
                  </button>
                  <button
                    type="button"
                    title={selectedStickyNode.collapsed ? 'Expand sticky' : 'Fold sticky'}
                    onClick={() =>
                      commitPageItems((items) =>
                        items.map((it) =>
                          it.id === selectedStickyNode.id && it.kind === 'sticky'
                            ? { ...it, collapsed: !it.collapsed }
                            : it
                        )
                      )
                    }
                    className="rounded-lg p-1 text-slate-400 hover:bg-black/5"
                  >
                    {selectedStickyNode.collapsed ? (
                      <Maximize2 className="h-3.5 w-3.5" />
                    ) : (
                      <Minimize2 className="h-3.5 w-3.5" />
                    )}
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      const copy: StickyItem = {
                        ...selectedStickyNode,
                        id: `stk-${Date.now()}`,
                        x: selectedStickyNode.x + 28,
                        y: selectedStickyNode.y + 28,
                      };
                      commitPageItems((items) => [...items, copy]);
                      setSelectedIds(new Set([copy.id]));
                      setEditingStickyId(copy.id);
                    }}
                    title="Duplicate sticky"
                    className="rounded-lg p-1 text-slate-400 hover:bg-black/5"
                  >
                    <Copy className="h-3.5 w-3.5" />
                  </button>
                  <button
                    type="button"
                    onClick={deleteSelected}
                    title="Delete sticky"
                    className="rounded-lg p-1 text-red-500 hover:bg-red-500/10"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                  <button
                    type="button"
                    onClick={() => setEditingStickyId(null)}
                    className="rounded-lg p-1 text-slate-400 hover:bg-black/5"
                  >
                    <X className="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>

              <input
                type="text"
                value={selectedStickyNode.title || ''}
                onChange={(e) => {
                  const val = e.target.value;
                  commitPageItems(
                    (items) =>
                      items.map((it) =>
                        it.id === selectedStickyNode.id && it.kind === 'sticky' ? { ...it, title: val } : it
                      ),
                    false
                  );
                }}
                placeholder="Sticky title (optional)..."
                className="mb-2 w-full rounded-xl border border-black/10 bg-slate-50 px-2.5 py-1.5 text-xs font-bold outline-none focus:border-blue-500 dark:border-white/12 dark:bg-[#14151A]"
              />

              <textarea
                autoFocus
                rows={3}
                value={selectedStickyNode.text}
                onChange={(e) => {
                  const val = e.target.value;
                  commitPageItems(
                    (items) =>
                      items.map((it) =>
                        it.id === selectedStickyNode.id && it.kind === 'sticky' ? { ...it, text: val } : it
                      ),
                    false
                  );
                }}
                placeholder="Write notes or `- [ ]` interactive checklists..."
                className="w-full rounded-2xl border border-black/10 p-2.5 text-xs outline-none focus:border-blue-500 dark:border-white/12 dark:bg-[#14151A]"
                style={{ backgroundColor: `${selectedStickyNode.color}44` }}
              />

              <div className="mt-2 flex gap-1">
                {(['classic', 'lined', 'glass', 'outline'] as StickyStyle[]).map((st) => (
                  <button
                    key={st}
                    type="button"
                    onClick={() =>
                      commitPageItems((items) =>
                        items.map((it) =>
                          it.id === selectedStickyNode.id && it.kind === 'sticky' ? { ...it, style: st } : it
                        )
                      )
                    }
                    className={`flex-1 rounded-lg py-1 text-[10px] font-bold capitalize transition ${
                      (selectedStickyNode.style || 'classic') === st
                        ? 'bg-blue-600 text-white'
                        : 'bg-black/5 text-slate-600 dark:bg-white/10 dark:text-slate-300'
                    }`}
                  >
                    {st}
                  </button>
                ))}
              </div>

              <div className="mt-2.5 flex flex-wrap items-center justify-between gap-2">
                <div className="flex gap-1.5">
                  {STICKY_COLORS.map((c) => (
                    <button
                      key={c}
                      type="button"
                      onClick={() =>
                        commitPageItems((items) =>
                          items.map((it) =>
                            it.id === selectedStickyNode.id && it.kind === 'sticky' ? { ...it, color: c } : it
                          )
                        )
                      }
                      className={`h-5 w-5 rounded-md border border-black/20 ${
                        selectedStickyNode.color === c ? 'ring-2 ring-blue-500' : ''
                      }`}
                      style={{ backgroundColor: c }}
                    />
                  ))}
                </div>
                <LiquidMenuButton
                  align="right"
                  label={selectedStickyNode.font || 'SANS'}
                  items={(['SANS', 'SERIF', 'MONO', 'HAND'] as StickyFont[]).map((f) => ({
                    id: f,
                    label: f,
                    selected: (selectedStickyNode.font || 'SANS') === f,
                    onSelect: () =>
                      commitPageItems((items) =>
                        items.map((it) =>
                          it.id === selectedStickyNode.id && it.kind === 'sticky' ? { ...it, font: f } : it
                        )
                      ),
                  }))}
                />
              </div>

              <div className="mt-2 flex items-center gap-2 text-[11px]">
                <span className="text-[#5A5F58] dark:text-[#B1AC9F]">Font</span>
                <LiquidSlider
                  min={14}
                  max={32}
                  step={1}
                  value={selectedStickyNode.fontSize || 18}
                  onChange={(sz) =>
                    commitPageItems(
                      (items) =>
                        items.map((it) =>
                          it.id === selectedStickyNode.id && it.kind === 'sticky'
                            ? { ...it, fontSize: sz }
                            : it
                        ),
                      false
                    )
                  }
                  className="flex-1"
                />
                <span className="font-mono-num w-9 text-right">{selectedStickyNode.fontSize || 18}px</span>
              </div>
            </LiquidGlassSurface>
          )}

          {/* Contextual Floating WebLink Card Bar */}
          {selectedWebLinkNode && selectedWebLinkScreen && (
            <LiquidGlassSurface
              enabled={settings.liquidGlass}
              variant="card"
              radius={18}
              style={{
                left: Math.max(175, Math.min((containerRef.current?.clientWidth || 800) - 185, selectedWebLinkScreen.left)),
                top: Math.max(12, selectedWebLinkScreen.top - 48),
              }}
              className="absolute z-40 -translate-x-1/2 px-2.5 py-1.5 shadow-xl animate-stagger-in"
            >
              <div className="flex items-center gap-1.5">
                <a
                  href={/^https?:\/\//i.test(selectedWebLinkNode.url) ? selectedWebLinkNode.url : `https://${selectedWebLinkNode.url}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-1 rounded-full bg-[#24483F] px-2.5 py-1 text-[11px] font-semibold text-[#F5F2E8] dark:bg-[#8CC1AC] dark:text-[#151E1B]"
                >
                  <ExternalLink className="h-3 w-3" /> Open Link
                </a>
                <LiquidButton
                  onClick={() =>
                    setWebLinkModal({
                      open: true,
                      editId: selectedWebLinkNode.id,
                      x: selectedWebLinkNode.x,
                      y: selectedWebLinkNode.y,
                      url: selectedWebLinkNode.url,
                      title: selectedWebLinkNode.title,
                      note: selectedWebLinkNode.note || '',
                      color: selectedWebLinkNode.color,
                    })
                  }
                  className="!px-2.5 !py-1 !text-[11px]"
                >
                  Edit
                </LiquidButton>
                <LiquidButton
                  onClick={deleteSelected}
                  className="!h-7 !w-7 !p-0 text-[#9B3B3B] dark:text-[#E58A8A]"
                >
                  <Trash2 className="h-3.5 w-3.5" />
                </LiquidButton>
              </div>
            </LiquidGlassSurface>
          )}

          {/* Contextual Floating Text Editor */}
          {editingTextId && (
            <LiquidGlassSurface
              enabled={settings.liquidGlass}
              variant="card"
              radius={22}
              className="absolute left-1/2 top-16 z-40 w-80 -translate-x-1/2 p-3.5 shadow-2xl animate-stagger-in"
            >
              <div className="mb-1.5 flex items-center justify-between text-xs font-bold">
                <span>Edit Text</span>
                <button
                  type="button"
                  onClick={() => {
                    const finalTxt = textDraft.trim();
                    const curItem = page?.items.find((i): i is TextItem => i.id === editingTextId);
                    if (finalTxt && curItem) {
                      commitPageItems((items) =>
                        items.map((it) => (it.id === editingTextId && it.kind === 'text' ? { ...it, text: finalTxt } : it))
                      );
                    } else {
                      commitPageItems((items) => items.filter((it) => it.id !== editingTextId));
                    }
                    setEditingTextId(null);
                  }}
                  className="rounded-lg bg-blue-600 px-2.5 py-1 text-white"
                >
                  Done
                </button>
              </div>
              <textarea
                autoFocus
                rows={3}
                value={textDraft}
                onChange={(e) => setTextDraft(e.target.value)}
                placeholder="Write notes..."
                className="w-full rounded-xl border border-black/12 p-2 text-sm outline-none focus:border-blue-500 dark:border-white/15 dark:bg-[#14151A]"
              />
            </LiquidGlassSurface>
          )}

          {/* Contextual Floating Mind Map Node Action Bar */}
          {selectedMindNode && selectedMindScreen && (
            <LiquidGlassSurface
              enabled={settings.liquidGlass}
              variant="card"
              radius={18}
              style={{
                left: Math.max(175, Math.min((containerRef.current?.clientWidth || 800) - 185, selectedMindScreen.left)),
                top: Math.max(12, selectedMindScreen.top - 54),
              }}
              className="absolute z-30 -translate-x-1/2 px-2 py-1.5 shadow-xl animate-stagger-in"
            >
              <div className="flex items-center gap-1">
              <button
                type="button"
                onClick={() => addMindNode(selectedMindNode, undefined, 'Child Idea')}
                className="whitespace-nowrap rounded-lg bg-blue-600 px-2 py-1 text-[11px] font-semibold text-white"
              >
                + Child
              </button>
              <button
                type="button"
                onClick={() => addSiblingMindNode(selectedMindNode)}
                className="whitespace-nowrap rounded-lg px-2 py-1 text-[11px] font-semibold hover:bg-black/5 dark:hover:bg-white/10"
              >
                + Sibling
              </button>
              <button
                type="button"
                onClick={() => openMindEditor(selectedMindNode)}
                className="whitespace-nowrap rounded-lg px-2 py-1 text-[11px] font-semibold hover:bg-black/5 dark:hover:bg-white/10"
              >
                Edit
              </button>
              <button
                type="button"
                onClick={() =>
                  commitPageItems((items) =>
                    items.map((it) =>
                      it.id === selectedMindNode.id && it.kind === 'mindnode'
                        ? { ...it, collapsed: !it.collapsed }
                        : it
                    )
                  )
                }
                className="whitespace-nowrap rounded-lg px-2 py-1 text-[11px] font-semibold hover:bg-black/5 dark:hover:bg-white/10"
              >
                {selectedMindNode.collapsed ? 'Expand' : 'Collapse'}
              </button>
              <button
                type="button"
                onClick={() => {
                  const shapes: ('pill' | 'card' | 'diamond')[] = ['pill', 'card', 'diamond'];
                  const next = shapes[(shapes.indexOf(selectedMindNode.shape) + 1) % shapes.length];
                  commitPageItems((items) =>
                    items.map((it) =>
                      it.id === selectedMindNode.id && it.kind === 'mindnode' ? { ...it, shape: next } : it
                    )
                  );
                }}
                className="whitespace-nowrap rounded-lg px-2 py-1 text-[11px] font-semibold hover:bg-black/5 dark:hover:bg-white/10"
              >
                Shape
              </button>
              {MIND_COLORS.slice(0, 4).map((c) => (
                <button
                  key={c}
                  type="button"
                  onClick={() =>
                    commitPageItems((items) =>
                      items.map((it) =>
                        it.id === selectedMindNode.id && it.kind === 'mindnode' ? { ...it, color: c } : it
                      )
                    )
                  }
                  className="h-4 w-4 shrink-0 rounded-full"
                  style={{ backgroundColor: c }}
                />
              ))}
              <button
                type="button"
                onClick={deleteSelected}
                className="rounded-lg p-1 text-red-500 hover:bg-red-500/10"
              >
                <Trash2 className="h-3.5 w-3.5" />
              </button>
            </div>
            </LiquidGlassSurface>
          )}

          {/* Mind Node Full Studio Editor Modal */}
          {editingMindId && (
            <LiquidGlassSurface
              enabled={settings.liquidGlass}
              variant="card"
              radius={22}
              className="absolute left-1/2 top-20 z-40 w-80 -translate-x-1/2 p-4 shadow-2xl animate-stagger-in"
            >
              <div className="mb-2.5 flex items-center justify-between">
                <span className="text-xs font-bold">Edit Mind Node</span>
                <button type="button" onClick={() => setEditingMindId(null)}>
                  <X className="h-4 w-4 text-slate-400" />
                </button>
              </div>
              <div className="space-y-2.5">
                <div>
                  <label className="mb-1 block text-[11px] font-semibold text-slate-500">Title</label>
                  <input
                    autoFocus
                    type="text"
                    value={mindDraft}
                    onChange={(e) => setMindDraft(e.target.value)}
                    placeholder="Node title"
                    className="w-full rounded-xl border border-black/15 px-3 py-1.5 text-sm outline-none focus:border-blue-500 dark:border-white/15 dark:bg-[#14151A]"
                  />
                </div>
                <div>
                  <label className="mb-1 block text-[11px] font-semibold text-slate-500">Category Tag (optional)</label>
                  <input
                    type="text"
                    value={mindTagDraft}
                    onChange={(e) => setMindTagDraft(e.target.value)}
                    placeholder="e.g. Concept, Definition, Key"
                    className="w-full rounded-xl border border-black/15 px-3 py-1.5 text-xs outline-none focus:border-blue-500 dark:border-white/15 dark:bg-[#14151A]"
                  />
                </div>
                <div>
                  <label className="mb-1 block text-[11px] font-semibold text-slate-500">Notes (optional)</label>
                  <textarea
                    rows={2}
                    value={mindNotesDraft}
                    onChange={(e) => setMindNotesDraft(e.target.value)}
                    placeholder="Details or definition shown on node card..."
                    className="w-full rounded-xl border border-black/15 px-3 py-1.5 text-xs outline-none focus:border-blue-500 dark:border-white/15 dark:bg-[#14151A]"
                  />
                </div>
                <div className="flex items-center justify-between gap-2">
                  <div className="flex gap-1">
                    {(['pill', 'card', 'diamond'] as const).map((shp) => (
                      <button
                        key={shp}
                        type="button"
                        onClick={() => setMindShapeDraft(shp)}
                        className={`rounded-lg px-2 py-1 text-[10px] font-bold capitalize ${
                          mindShapeDraft === shp
                            ? 'bg-blue-600 text-white'
                            : 'bg-black/5 text-slate-600 dark:bg-white/10 dark:text-slate-300'
                        }`}
                      >
                        {shp}
                      </button>
                    ))}
                  </div>
                  <div className="flex gap-1">
                    {MIND_COLORS.map((c) => (
                      <button
                        key={c}
                        type="button"
                        onClick={() => setMindColorDraft(c)}
                        className={`h-4 w-4 rounded-full ${mindColorDraft === c ? 'ring-2 ring-blue-500 ring-offset-1' : ''}`}
                        style={{ backgroundColor: c }}
                      />
                    ))}
                  </div>
                </div>
              </div>
              <div className="mt-3 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setEditingMindId(null)}
                  className="rounded-lg px-2.5 py-1 text-xs text-slate-500 hover:bg-black/5"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={() => {
                    const label = mindDraft.trim() || 'Node';
                    const tag = mindTagDraft.trim() || undefined;
                    const notes = mindNotesDraft.trim() || undefined;
                    const nw = Math.max(162, Math.min(268, label.length * 9 + 52));
                    const nh = notes ? 68 : tag ? 54 : 46;
                    commitPageItems((items) =>
                      items.map((it) =>
                        it.id === editingMindId && it.kind === 'mindnode'
                          ? {
                              ...it,
                              label,
                              tag,
                              notes,
                              shape: mindShapeDraft,
                              color: mindColorDraft,
                              w: nw,
                              h: nh,
                            }
                          : it
                      )
                    );
                    setEditingMindId(null);
                  }}
                  className="rounded-lg bg-blue-600 px-3 py-1 text-xs font-semibold text-white hover:bg-blue-700"
                >
                  Save
                </button>
              </div>
            </LiquidGlassSurface>
          )}

          {/* Floating Lasso Selection Action Bar */}
          {selectedIds.size > 0 && !selectedMindNode && !selectedStickyNode && !selectedWebLinkNode && (
            <LiquidGlassSurface
              enabled={settings.liquidGlass}
              variant="chrome"
              radius={999}
              className="absolute bottom-16 left-1/2 z-30 -translate-x-1/2 px-3 py-1.5 shadow-2xl animate-stagger-in"
            >
              <div className="flex items-center gap-2">
                <span className="font-mono-num pr-1 text-xs font-bold text-[#24483F] dark:text-[#8CC1AC]">
                  {selectedIds.size} selected
                </span>
                <LiquidButton
                  onClick={deleteSelected}
                  className="!px-2.5 !py-1 !text-xs text-[#9B3B3B] dark:text-[#E58A8A]"
                >
                  <Trash2 className="h-3.5 w-3.5" /> Delete
                </LiquidButton>
              </div>
            </LiquidGlassSurface>
          )}

          {/* Bottom-Right Zoom Controls */}
          <LiquidGlassSurface
            enabled={settings.liquidGlass}
            variant="chrome"
            radius={18}
            className="absolute bottom-4 right-4 z-20 p-1 shadow-lg"
          >
            <div className="flex items-center gap-1">
              <button
                type="button"
                onClick={() => setCamera((c) => ({ ...c, zoom: Math.max(0.2, c.zoom * 0.85) }))}
                className="rounded-xl p-1.5 hover:bg-black/5 dark:hover:bg-white/10"
              >
                <ZoomOut className="h-4 w-4" />
              </button>
              <span className="font-mono-num min-w-[44px] text-center text-xs font-semibold">
                {Math.round(camera.zoom * 100)}%
              </span>
              <button
                type="button"
                onClick={() => setCamera((c) => ({ ...c, zoom: Math.min(3.5, c.zoom * 1.15) }))}
                className="rounded-xl p-1.5 hover:bg-black/5 dark:hover:bg-white/10"
              >
                <ZoomIn className="h-4 w-4" />
              </button>
              <button
                type="button"
                onClick={() => fitPageToView(page)}
                title="Fit to view"
                className="rounded-xl p-1.5 hover:bg-black/5 dark:hover:bg-white/10"
              >
                <Maximize2 className="h-4 w-4" />
              </button>
            </div>
          </LiquidGlassSurface>
        </div>

        {/* Draggable Split Resizer + Rewritten Scroll-Aware Split Notebook Workspace */}
        {splitNotebook && (
          <>
            <div
              onMouseDown={(e) => {
                splitResizeRef.current = { active: true, startX: e.clientX, startW: splitWidth };
              }}
              className="hidden w-1.5 shrink-0 cursor-col-resize bg-black/5 transition hover:bg-[#24483F]/40 dark:bg-white/5 md:block"
              title="Drag to resize split workspace"
            />
            <SplitNotebookPane
              splitNotebook={splitNotebook}
              allNotebooks={allNotebooks}
              paperTintDefault={settings.paperTint}
              highlighterBehind={settings.highlighterBehind}
              scrollDirection={settings.scroll}
              primaryPageIdx={safeIdx}
              width={splitWidth}
              penColor={penColor}
              penSize={penSize}
              hlColor={hlColor}
              hlSize={hlSize}
              onChangeNotebook={(id) => setSplitNoteId(id)}
              onUpdateSplitNotebook={onUpdateNotebook}
              onSwapWithPrimary={(id) => {
                setSplitNoteId(notebook.id);
                onSwitchNote(id);
              }}
              onSetRatio={(ratio) => {
                const total = containerRef.current?.parentElement?.clientWidth || window.innerWidth;
                setSplitWidth(Math.max(240, Math.min(Math.round(total * 0.76), Math.round(total * (1 - ratio)))));
              }}
              onCopyTextToPrimary={(txt) => {
                const c = screenToWorld(
                  (containerRef.current?.clientWidth || 800) / 2,
                  (containerRef.current?.clientHeight || 600) / 2
                );
                commitPageItems((items) => [
                  ...items,
                  {
                    id: `stk-clip-${Date.now()}`,
                    kind: 'sticky',
                    x: c.x - 140,
                    y: c.y - 95,
                    w: 280,
                    h: 200,
                    color: '#CFE2FF',
                    title: 'Split Note Clip',
                    text: txt.slice(0, 600),
                    style: 'classic',
                    fontSize: 16,
                  },
                ]);
                showToast('Copied reference sticky onto active page');
              }}
              onInsertTextToActive={(txt) => {
                const c = screenToWorld(
                  (containerRef.current?.clientWidth || 800) / 2,
                  (containerRef.current?.clientHeight || 600) / 2
                );
                commitPageItems((items) => [
                  ...items,
                  {
                    id: `txt-clip-${Date.now()}`,
                    kind: 'text',
                    text: txt.slice(0, 900),
                    x: Math.max(80, c.x - 220),
                    y: Math.max(80, c.y - 80),
                    size: 22,
                    color: '#1B1F2A',
                    font: 'SANS',
                    createdAt: Date.now(),
                  },
                ]);
                showToast('Inserted text onto active page');
              }}
              onInsertWebLink={(url, title) => {
                const c = screenToWorld(
                  (containerRef.current?.clientWidth || 800) / 2,
                  (containerRef.current?.clientHeight || 600) / 2
                );
                commitPageItems((items) => [
                  ...items,
                  {
                    id: `link-${Date.now()}`,
                    kind: 'weblink',
                    url,
                    title,
                    note: 'Cross-referenced from Split View',
                    x: c.x - 165,
                    y: c.y - 44,
                    w: 330,
                    h: 88,
                    color: WEBLINK_COLORS[0],
                  },
                ]);
                showToast('Inserted Web Link card onto page');
              }}
              onClose={() => setSplitNoteId(null)}
            />
          </>
        )}

        {/* In-Notebook Search Drawer */}
        {searchOpen && (
          <LiquidGlassSurface
            enabled={settings.liquidGlass}
            variant="card"
            radius={22}
            className="absolute right-4 top-4 z-40 w-80 p-3.5 shadow-2xl animate-stagger-in"
          >
            <div className="mb-2 flex items-center gap-2">
              <LiquidInput
                autoFocus
                icon={<Search className="h-4 w-4" />}
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search in notebook..."
                wrapperClassName="flex-1 !py-1.5"
              />
              <LiquidButton onClick={() => setSearchOpen(false)} className="!h-8 !w-8 !p-0">
                <X className="h-4 w-4" />
              </LiquidButton>
            </div>

            <div className="max-h-60 space-y-1.5 overflow-y-auto">
              {searchHits.map((h, i) => (
                <button
                  key={i}
                  type="button"
                  onClick={() => {
                    setPageIdx(h.pageIdx);
                    fitPageToView(notebook.pages[h.pageIdx]);
                  }}
                  className="liquid-glass-frost flex w-full flex-col rounded-xl px-2.5 py-2 text-left text-xs transition hover:brightness-95"
                >
                  <div className="flex w-full items-center justify-between gap-2">
                    <span className="font-mono-num text-[10px] font-bold text-[#24483F] dark:text-[#8CC1AC]">
                      Page {h.pageIdx + 1}
                    </span>
                  </div>
                  <span className="truncate">{h.preview}</span>
                </button>
              ))}
              {searchQuery && searchHits.length === 0 && (
                <div className="py-3 text-center text-xs text-[#5A5F58] dark:text-[#B1AC9F]">No text hits found</div>
              )}
            </div>
          </LiquidGlassSurface>
        )}
      </div>

      {/* Template Sheet Modal */}
      {page && (
        <LiquidModal open={templateSheetOpen} onClose={() => setTemplateSheetOpen(false)} maxWidthClass="max-w-2xl">
          <div className="flex max-h-[85vh] flex-col">
            <div className="flex items-center justify-between border-b border-[#1F2421]/10 px-6 py-4 dark:border-[#EDE8DB]/10">
              <h2 className="text-base font-bold">Page Template &amp; Tint</h2>
              <LiquidButton onClick={() => setTemplateSheetOpen(false)} className="!h-8 !w-8 !p-0">
                <X className="h-4 w-4" />
              </LiquidButton>
            </div>
            <div className="flex-1 space-y-4 overflow-y-auto p-6">
              <div className="flex items-center gap-2">
                <span className="text-xs font-semibold text-[#5A5F58] dark:text-[#B1AC9F]">Paper Tint:</span>
                {PAPER_TINTS.map((t) => (
                  <button
                    key={t.id}
                    type="button"
                    onClick={() =>
                      commitPages((pages) =>
                        pages.map((pg, i) => (i === safeIdx ? { ...pg, paperColor: t.color } : pg))
                      )
                    }
                    className="h-7 w-7 rounded-full border border-black/20"
                    style={{ backgroundColor: t.color }}
                  />
                ))}
              </div>
              <div className="grid grid-cols-4 gap-3 sm:grid-cols-5">
                {PAPER_STYLES.map((ps) => (
                  <button
                    key={ps.id}
                    type="button"
                    onClick={() => {
                      commitPages((pages) =>
                        pages.map((pg, i) => (i === safeIdx ? { ...pg, paper: ps.id, cover: null } : pg))
                      );
                      setTemplateSheetOpen(false);
                    }}
                    className={`liquid-glass-frost ios-stretch flex flex-col items-center rounded-xl p-1.5 ${
                      page.paper === ps.id && !page.cover ? 'ring-2 ring-[#24483F] dark:ring-[#8CC1AC]' : ''
                    }`}
                  >
                    <PaperPreviewCanvas
                      paper={ps.id}
                      paperColor={page.paperColor || settings.paperTint}
                      width={74}
                      height={98}
                      className="rounded"
                    />
                    <span className="mt-1 text-[10px] font-medium">{ps.label}</span>
                  </button>
                ))}
              </div>
            </div>
          </div>
        </LiquidModal>
      )}

      {/* Toast Feedback */}
      {toast && (
        <LiquidGlassSurface
          enabled={settings.liquidGlass}
          variant="chrome"
          radius={999}
          className="pointer-events-none fixed bottom-20 left-1/2 z-50 -translate-x-1/2 px-4 py-2 text-xs font-semibold shadow-xl animate-stagger-in"
        >
          {toast}
        </LiquidGlassSurface>
      )}

      {/* Study Modals */}
      <SummaryModal
        open={summaryModal.open}
        title={summaryModal.title}
        summary={summaryModal.summary}
        loadingStage={summaryModal.loadingStage}
        explainText={summaryModal.explainText}
        onClose={() => setSummaryModal((s) => ({ ...s, open: false }))}
        onTurnIntoMindMap={() => {
          setSummaryModal((s) => ({ ...s, open: false }));
          handleAiGenerateMindMap();
        }}
        onAddToPage={(text) => {
          const c = screenToWorld(
            (containerRef.current?.clientWidth || 800) / 2,
            (containerRef.current?.clientHeight || 600) / 2
          );
          commitPageItems((items) => [
            ...items,
            {
              id: `stk-ai-${Date.now()}`,
              kind: 'sticky',
              x: c.x - 160,
              y: c.y - 110,
              w: 320,
              h: 230,
              color: '#E9D8FD',
              text,
              fontSize: 16,
            },
          ]);
        }}
      />

      <FlashcardsModal
        open={flashModalOpen}
        notebookTitle={notebook.title}
        notebookId={notebook.id}
        cards={flashcards}
        loadingStage={null}
        onClose={() => setFlashModalOpen(false)}
        onAddCard={(front, back) =>
          onAddFlashcards([
            {
              id: `fc-${Date.now()}`,
              notebookId: notebook.id,
              front,
              back,
              box: 1,
              due: Date.now(),
              created: Date.now(),
            },
          ])
        }
        onUpdateCard={onUpdateFlashcard}
        onDeleteCard={onDeleteFlashcard}
        onGenerateAi={handleAiFlashcards}
      />

      <QuizModal
        open={quizModal.open}
        notebookTitle={notebook.title}
        questions={quizModal.questions}
        bestScore={bestQuizScore}
        engineLabel={quizModal.engineLabel}
        loadingStage={quizModal.loadingStage}
        onClose={() => setQuizModal((s) => ({ ...s, open: false }))}
        onScoreRecorded={(pct) => {
          onRecordQuizScore(notebook.id, pct);
        }}
      />

      <SlideShowModal
        open={slideModalOpen}
        notebookTitle={notebook.title}
        totalPages={notebook.pages.length}
        currentPageIdx={safeIdx}
        slides={notebook.slides || []}
        loadingStage={slideLoadingStage}
        engineLabel={slideEngineLabel}
        onClose={() => setSlideModalOpen(false)}
        onSaveSlides={(nextSlides) => {
          if (onUpdateSlides) onUpdateSlides(nextSlides);
          else onUpdateNotebook(notebook.id, (nb) => ({ ...nb, slides: nextSlides }));
        }}
        onGenerateAi={handleGenerateSlides}
      />

      {/* Web Link Card Modal */}
      <LiquidModal
        open={webLinkModal.open}
        onClose={() => setWebLinkModal((m) => ({ ...m, open: false }))}
        maxWidthClass="max-w-md"
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            const cleanUrl = webLinkModal.url.trim();
            if (!cleanUrl || cleanUrl === 'https://') return;
            const normalizedUrl = /^https?:\/\//i.test(cleanUrl) ? cleanUrl : `https://${cleanUrl}`;
            const linkTitle = webLinkModal.title.trim() || normalizedUrl.replace(/^https?:\/\//i, '').split('/')[0];
            if (webLinkModal.editId) {
              commitPageItems((items) =>
                items.map((it) =>
                  it.id === webLinkModal.editId && it.kind === 'weblink'
                    ? {
                        ...it,
                        url: normalizedUrl,
                        title: linkTitle,
                        note: webLinkModal.note.trim() || undefined,
                        color: webLinkModal.color,
                      }
                    : it
                )
              );
            } else {
              const newCard: WebLinkItem = {
                id: `link-${Date.now()}`,
                kind: 'weblink',
                url: normalizedUrl,
                title: linkTitle,
                note: webLinkModal.note.trim() || undefined,
                x: webLinkModal.x,
                y: webLinkModal.y,
                w: 330,
                h: 88,
                color: webLinkModal.color,
              };
              commitPageItems((items) => [...items, newCard]);
              setSelectedIds(new Set([newCard.id]));
            }
            setWebLinkModal((m) => ({ ...m, open: false }));
          }}
          className="space-y-3 p-5"
        >
          <div className="flex items-center justify-between">
            <h3 className="inline-flex items-center gap-2 text-sm font-bold">
              <Link2 className="h-4 w-4 text-[#24483F] dark:text-[#8CC1AC]" />
              {webLinkModal.editId ? 'Edit Web Link Card' : 'Add Web Link Card'}
            </h3>
            <LiquidButton
              onClick={() => setWebLinkModal((m) => ({ ...m, open: false }))}
              className="!h-8 !w-8 !p-0"
            >
              <X className="h-4 w-4" />
            </LiquidButton>
          </div>
          <div>
            <label className="mb-1 block text-[11px] font-bold uppercase text-[#5A5F58] dark:text-[#B1AC9F]">URL</label>
            <LiquidInput
              autoFocus
              type="text"
              value={webLinkModal.url}
              onChange={(e) => setWebLinkModal((m) => ({ ...m, url: e.target.value }))}
              placeholder="https://en.wikipedia.org/wiki/..."
            />
          </div>
          <div>
            <label className="mb-1 block text-[11px] font-bold uppercase text-[#5A5F58] dark:text-[#B1AC9F]">Display Title</label>
            <LiquidInput
              type="text"
              value={webLinkModal.title}
              onChange={(e) => setWebLinkModal((m) => ({ ...m, title: e.target.value }))}
              placeholder="e.g. Lecture Video / Research Paper"
            />
          </div>
          <div>
            <label className="mb-1 block text-[11px] font-bold uppercase text-[#5A5F58] dark:text-[#B1AC9F]">Quick Annotation</label>
            <LiquidInput
              type="text"
              value={webLinkModal.note}
              onChange={(e) => setWebLinkModal((m) => ({ ...m, note: e.target.value }))}
              placeholder="Why this link matters..."
            />
          </div>
          <div className="flex items-center justify-between pt-1">
            <div className="flex gap-1.5">
              {WEBLINK_COLORS.map((c) => (
                <button
                  key={c}
                  type="button"
                  onClick={() => setWebLinkModal((m) => ({ ...m, color: c }))}
                  className={`h-6 w-6 rounded-full border ${
                    webLinkModal.color === c ? 'ring-2 ring-[#24483F] ring-offset-1 dark:ring-[#8CC1AC]' : 'border-black/15'
                  }`}
                  style={{ backgroundColor: c }}
                />
              ))}
            </div>
            <div className="flex gap-2">
              <LiquidButton
                onClick={() => setWebLinkModal((m) => ({ ...m, open: false }))}
                className="!px-3.5 !py-2 !text-xs"
              >
                Cancel
              </LiquidButton>
              <button
                type="submit"
                className="rounded-full bg-[#24483F] px-4 py-2 text-xs font-semibold text-[#F5F2E8] dark:bg-[#8CC1AC] dark:text-[#151E1B]"
              >
                Save Link
              </button>
            </div>
          </div>
        </form>
      </LiquidModal>
      {/* Live Collaborative Study Room */}
      <CollabRoomModal
        open={collabModalOpen}
        isSignedIn={isSignedIn}
        activeRoom={activeCollabRoom}
        members={collabMembers}
        messages={collabMessages}
        onSignInGoogle={async () => {
          await onSignInGoogle?.();
        }}
        onCreateRoom={async (title) => {
          await onCreateCollabRoom?.(title);
        }}
        onJoinRoom={async (code) => {
          await onJoinCollabRoom?.(code);
        }}
        onLeaveRoom={() => onLeaveCollabRoom?.()}
        onSendMessage={async (text) => {
          await onSendCollabMessage?.(text);
        }}
        onClose={() => setCollabModalOpen(false)}
      />

      {/* Quick Command Palette (Ctrl/Cmd+K) */}
      <CommandPaletteModal
        open={cmdPaletteOpen}
        onClose={() => setCmdPaletteOpen(false)}
        actions={
          [
            {
              id: 'collab',
              label: 'Open Live Collaborative Study Room',
              hint: 'Multi-user',
              icon: 'collab',
              run: () => setCollabModalOpen(true),
            },
            {
              id: 'slides',
              label: 'Open Revision Slide Show Deck',
              hint: 'Deck Studio',
              icon: 'slides',
              run: () => setSlideModalOpen(true),
            },
            {
              id: 'pomodoro',
              label: pomodoroActive ? 'Switch to Elapsed Timer' : `Start ${settings.pomodoroMinutes || 25}m Pomodoro Timer`,
              hint: 'Focus',
              icon: 'timer',
              run: () => {
                setPomodoroActive((p) => !p);
                setStudySec(0);
                setTimerRunning(true);
              },
            },
            {
              id: 'export-png',
              label: 'Export Current View as PNG',
              hint: 'Download',
              icon: 'export',
              run: handleExportPng,
            },
          ] as CommandPaletteAction[]
        }
      />
    </div>
  );
};
