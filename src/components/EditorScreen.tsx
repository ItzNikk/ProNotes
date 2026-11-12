import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  ArrowLeft,
  BookOpen,
  Brain,
  Calculator,
  ChevronLeft,
  ChevronRight,
  Columns2,
  Cpu,
  Download,
  Eraser,
  FileText,
  GitBranch,
  Hand,
  Highlighter,
  Home,
  Image as ImageIcon,
  Lasso as LassoIcon,
  Layers,
  Maximize2,
  MoreHorizontal,
  Network,
  PanelLeft,
  Pen,
  Plus,
  Redo2,
  RotateCcw,
  Search,
  Shapes,
  Sparkles,
  Sticker,
  BookmarkMinus,
  StickyNote,
  Timer,
  Trash2,
  Type,
  Undo2,
  Wand2,
  X,
  ZoomIn,
  ZoomOut,
} from 'lucide-react';
import {
  ACCENT_COLORS,
  AppSettings,
  Flashcard,
  InkItem,
  LocalModelState,
  MindNodeItem,
  NotebookMeta,
  Page,
  PageItem,
  PAPER_STYLES,
  PAPER_TINTS,
  PaperStyle,
  PenKind,
  QuizQuestion,
  ShapeKind,
  StickyItem,
  StrokePt,
  TAPE_PATTERNS,
  TextFont,
  TextItem,
  ToolId,
} from '../types/models';
import {
  dist,
  getBox,
  getItemBounds,
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
} from '../utils/geometry';
import {
  drawPaperTemplate,
  drawWhiteboardBackdrop,
  getHiddenMindNodeIds,
  isColorDark,
  renderPageItems,
} from '../utils/canvasRenderer';
import { MathSolver } from '../utils/mathEngine';
import {
  extractNotebookText,
  generateMindMapFromText,
  LOCAL_MODELS_CATALOG,
  runLocalExplain,
  runLocalFlashcards,
  runLocalQuiz,
  runLocalSummary,
} from '../utils/localAi';
import { AiThinkingPill, PipPet } from './PipPet';
import { PaperPreviewCanvas } from './CoversAndPatterns';
import { FlashcardsModal, QuizModal, SummaryModal } from './StudyModals';

interface PuffParticle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  r: number;
  born: number;
  color: string;
}

interface GlowSweep {
  x: number;
  y: number;
  w: number;
  h: number;
  born: number;
}

interface LaserTrail {
  pts: Pt[];
  born: number;
}

interface EditorScreenProps {
  notebook: NotebookMeta;
  allNotebooks: NotebookMeta[];
  openTabIds: string[];
  settings: AppSettings;
  flashcards: Flashcard[];
  bestQuizScore: number;
  modelStates: Record<string, LocalModelState>;
  onBackHome: () => void;
  onSwitchNote: (id: string) => void;
  onCloseTab: (id: string) => void;
  onUpdateNotebook: (id: string, updater: (nb: NotebookMeta) => NotebookMeta) => void;
  onAddFlashcards: (cards: Flashcard[]) => void;
  onUpdateFlashcard: (card: Flashcard) => void;
  onDeleteFlashcard: (id: string) => void;
  onRecordQuizScore: (notebookId: string, pct: number) => void;
  onOpenSettings: () => void;
  onOpenLocalModels: () => void;
}

const PEN_COLORS = ['#1B1F2A', '#2A78D6', '#E5484D', '#0F7E5C', '#7C3AED', '#F2994A', '#FFFFFF'];
const HL_COLORS = ['#FFE066', '#8CE99A', '#74C0FC', '#FFA8A8', '#D0BFFF'];
const STICKY_COLORS = ['#FFF3B0', '#D8F3DC', '#CFE2FF', '#FDE2E4', '#E9D8FD'];
const MIND_COLORS = ['#3D6CF4', '#2BB673', '#F2994A', '#9B51E0', '#EB5757', '#0EA5E9'];
const STICKERS = ['⭐', '💡', '📌', '🔥', '✅', '🧠', '🎯', '📐', '🧪', '⚡', '🏆', '❤️'];

export const EditorScreen: React.FC<EditorScreenProps> = ({
  notebook,
  allNotebooks,
  openTabIds,
  settings,
  flashcards,
  bestQuizScore,
  onBackHome,
  onSwitchNote,
  onCloseTab,
  onUpdateNotebook,
  onAddFlashcards,
  onUpdateFlashcard,
  onDeleteFlashcard,
  onRecordQuizScore,
  onOpenLocalModels,
}) => {
  const accent = ACCENT_COLORS[settings.accent];
  const activeModel = LOCAL_MODELS_CATALOG.find((m) => m.id === settings.activeLocalModelId);

  const [pageIdx, setPageIdx] = useState(0);
  const [tool, setTool] = useState<ToolId>(notebook.isMindMap ? 'MINDMAP' : 'PEN');
  const [penKind, setPenKind] = useState<PenKind>('fountain');
  const [penColor, setPenColor] = useState('#1B1F2A');
  const [penSize, setPenSize] = useState(3.5);
  const [hlColor, setHlColor] = useState('#FFE066');
  const [hlSize, setHlSize] = useState(22);
  const [eraserSize, setEraserSize] = useState(26);
  const [shapeKind, setShapeKind] = useState<ShapeKind>('AUTO');
  const [tapeColor, setTapeColor] = useState('#7161E0');
  const [tapePattern, setTapePattern] = useState('stripes');
  const [stickyColor, setStickyColor] = useState(STICKY_COLORS[0]);
  const [textFont, setTextFont] = useState<TextFont>('SANS');
  const [textSize, setTextSize] = useState(26);

  // Selection & Editing
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [editingTextId, setEditingTextId] = useState<string | null>(null);
  const [textDraft, setTextDraft] = useState('');
  const [editingMindId, setEditingMindId] = useState<string | null>(null);
  const [mindDraft, setMindDraft] = useState('');

  // History per notebook
  const [undoStack, setUndoStack] = useState<Page[][]>([]);
  const [redoStack, setRedoStack] = useState<Page[][]>([]);

  // Drawers & Modals
  const [pagesOpen, setPagesOpen] = useState(false);
  const [templateSheetOpen, setTemplateSheetOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [aiMenuOpen, setAiMenuOpen] = useState(false);
  const [moreMenuOpen, setMoreMenuOpen] = useState(false);
  const [stickerOpen, setStickerOpen] = useState(false);
  const [mathPadOpen, setMathPadOpen] = useState(false);
  const [mathInput, setMathInput] = useState('2x + 3 = x + 7');
  const [splitNoteId, setSplitNoteId] = useState<string | null>(null);

  // Study & AI States
  const [aiBusyLabel, setAiBusyLabel] = useState<string | null>(null);
  const [toast, setToast] = useState<string | null>(null);
  const [cheerTrigger, setCheerTrigger] = useState(0);
  const [summaryModal, setSummaryModal] = useState<{
    open: boolean;
    title: string;
    points: string[];
    engineLabel: string;
    explainText?: string | null;
  }>({ open: false, title: '', points: [], engineLabel: '' });
  const [flashModalOpen, setFlashModalOpen] = useState(false);
  const [quizModal, setQuizModal] = useState<{
    open: boolean;
    questions: QuizQuestion[];
    engineLabel: string;
  }>({ open: false, questions: [], engineLabel: '' });

  // Timer
  const [studySec, setStudySec] = useState(0);
  const [timerRunning, setTimerRunning] = useState(settings.studyAutoStart);

  // Viewport / Camera (works seamlessly for both bounded Pages AND Infinite Whiteboard)
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

  // Active gesture refs (mutated at 60fps without React re-render overhead)
  const gestureRef = useRef<{
    active: boolean;
    mode: 'NONE' | 'INK' | 'ERASE' | 'SHAPE' | 'LASSO' | 'PAN' | 'DRAG_ITEMS' | 'DRAG_MIND' | 'TAPE' | 'LASER';
    pts: StrokePt[];
    startScreen: Pt;
    lastScreen: Pt;
    startWorld: Pt;
    lastWorld: Pt;
    scribbleConsumed: boolean;
    dragMindId: string | null;
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
    dragOrigItems: null,
  });

  const puffsRef = useRef<PuffParticle[]>([]);
  const glowsRef = useRef<GlowSweep[]>([]);
  const lasersRef = useRef<LaserTrail[]>([]);
  const [writingActive, setWritingActive] = useState(false);

  const safeIdx = Math.min(pageIdx, Math.max(0, notebook.pages.length - 1));
  const page = notebook.pages[safeIdx];
  const isWhiteboard = Boolean(page?.isWhiteboard || notebook.isWhiteboard);

  const showToast = useCallback((msg: string) => {
    setToast(msg);
    setTimeout(() => setToast((cur) => (cur === msg ? null : cur)), 2600);
  }, []);

  // Reset camera appropriately when switching notebook or page
  const fitPageToView = useCallback(
    (targetPage: Page | undefined) => {
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
    },
    []
  );

  useEffect(() => {
    setPageIdx(0);
    setSelectedIds(new Set());
    setUndoStack([]);
    setRedoStack([]);
    setTool(notebook.isMindMap ? 'MINDMAP' : 'PEN');
    setTimeout(() => fitPageToView(notebook.pages[0]), 20);
  }, [notebook.id, fitPageToView]);

  // Study timer
  useEffect(() => {
    if (!settings.studyTimer || !timerRunning) return;
    const id = setInterval(() => setStudySec((s) => s + 1), 1000);
    return () => clearInterval(id);
  }, [settings.studyTimer, timerRunning]);

  // Spacebar pan + keyboard shortcuts
  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
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
  });

  const commitPages = useCallback(
    (updater: (pages: Page[]) => Page[], pushHistory = true) => {
      if (pushHistory) {
        setUndoStack((prev) => [...prev.slice(-30), structuredClone(notebook.pages)]);
        setRedoStack([]);
      }
      onUpdateNotebook(notebook.id, (nb) => ({
        ...nb,
        pages: updater(nb.pages),
      }));
    },
    [notebook.id, notebook.pages, onUpdateNotebook]
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
    if (undoStack.length === 0) return;
    const prev = undoStack[undoStack.length - 1];
    setUndoStack((s) => s.slice(0, -1));
    setRedoStack((s) => [...s, structuredClone(notebook.pages)]);
    onUpdateNotebook(notebook.id, (nb) => ({ ...nb, pages: prev }));
    setSelectedIds(new Set());
  }, [undoStack, notebook.id, notebook.pages, onUpdateNotebook]);

  const handleRedo = useCallback(() => {
    if (redoStack.length === 0) return;
    const next = redoStack[redoStack.length - 1];
    setRedoStack((s) => s.slice(0, -1));
    setUndoStack((s) => [...s, structuredClone(notebook.pages)]);
    onUpdateNotebook(notebook.id, (nb) => ({ ...nb, pages: next }));
    setSelectedIds(new Set());
  }, [redoStack, notebook.id, notebook.pages, onUpdateNotebook]);

  const spawnPuffForItems = useCallback((items: PageItem[]) => {
    const now = performance.now();
    for (const it of items) {
      const b = getItemBounds(it);
      for (let k = 0; k < 14; k++) {
        puffsRef.current.push({
          x: b.l + Math.random() * Math.max(12, b.w),
          y: b.t + Math.random() * Math.max(12, b.h),
          vx: (Math.random() - 0.5) * 2.2,
          vy: -0.8 - Math.random() * 2.0,
          r: 2.2 + Math.random() * 2.8,
          born: now,
          color: it.kind === 'ink' ? it.color : '#8A93A3',
        });
      }
    }
  }, []);

  const deleteSelected = useCallback(() => {
    if (selectedIds.size === 0 || !page) return;
    const mindNodes = page.items.filter((i): i is MindNodeItem => i.kind === 'mindnode');
    const toRemove = new Set(selectedIds);
    // Cascade delete child mind map nodes
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
  }, [selectedIds, page, commitPageItems, spawnPuffForItems]);

  // Coordinate conversions
  const screenToWorld = useCallback(
    (sx: number, sy: number): Pt => ({
      x: (sx - camera.panX) / camera.zoom,
      y: (sy - camera.panY) / camera.zoom,
    }),
    [camera]
  );

  // ================================================================
  // Mind Map Helpers (Add Child / Sibling / Auto-Layout / AI Expand)
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

  const addMindNode = useCallback(
    (parent: MindNodeItem | null, worldPos?: Pt, label = 'New Idea') => {
      const siblings = mindNodesOnPage.filter((n) => n.parentId === (parent?.id ?? null));
      const color =
        parent === null
          ? MIND_COLORS[siblings.length % MIND_COLORS.length]
          : parent.parentId === null
            ? MIND_COLORS[siblings.length % MIND_COLORS.length]
            : parent.color;

      const x = worldPos
        ? worldPos.x - 75
        : parent
          ? parent.x + parent.w + 75
          : 400;
      const y = worldPos
        ? worldPos.y - 21
        : parent
          ? parent.y + (siblings.length - 0.5) * 68
          : 300;

      const newNode: MindNodeItem = {
        id: `mm-${Date.now()}-${Math.floor(Math.random() * 999)}`,
        kind: 'mindnode',
        x,
        y,
        w: 162,
        h: 44,
        label,
        parentId: parent?.id ?? null,
        color,
        shape: parent ? 'card' : 'pill',
      };
      commitPageItems((items) => [...items, newNode]);
      setSelectedIds(new Set([newNode.id]));
      setEditingMindId(newNode.id);
      setMindDraft(label);
    },
    [mindNodesOnPage, commitPageItems]
  );

  const autoLayoutMindMap = useCallback(
    (layoutType: 'TREE' | 'RADIAL') => {
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
              radius + 185
            );
          });
        };
        placeRadial(root, -Math.PI, Math.PI, 240);
      } else {
        // Horizontal balanced tree
        let nextY = cy - 180;
        const placeTree = (node: MindNodeItem, depth: number): number => {
          const kids = byParent.get(node.id) || [];
          if (kids.length === 0) {
            const y = nextY;
            nextY += 72;
            posMap.set(node.id, { x: cx - 100 + depth * 240, y });
            return y;
          }
          const ys = kids.map((k) => placeTree(k, depth + 1));
          const avgY = (ys[0] + ys[ys.length - 1]) / 2;
          posMap.set(node.id, { x: cx - 100 + depth * 240, y: avgY });
          return avgY;
        };
        placeTree(root, 0);
      }

      commitPageItems((items) =>
        items.map((it) => {
          if (it.kind !== 'mindnode') return it;
          const p = posMap.get(it.id);
          return p ? { ...it, x: p.x, y: p.y } : it;
        })
      );
      showToast(`Applied ${layoutType === 'RADIAL' ? 'Radial' : 'Tree'} Mind Map layout`);
    },
    [mindNodesOnPage, commitPageItems, showToast]
  );

  // ================================================================
  // Maths Solver Integration
  // ================================================================

  const placeMathAnswer = useCallback(
    (expr: string, anchorX: number, anchorY: number, sourceWidth = 180, sourceHeight = 42) => {
      const ans = MathSolver.solve(expr);
      if (!ans) {
        showToast(`Could not evaluate "${expr}"`);
        return false;
      }
      const displayAns = expr.trim().includes('=') && !expr.trim().endsWith('=') ? `✓ ${ans}` : `= ${ans}`;
      const ansItem: TextItem = {
        id: `math-${Date.now()}`,
        kind: 'text',
        text: displayAns,
        x: anchorX + sourceWidth + 16,
        y: anchorY,
        size: Math.max(24, Math.min(44, sourceHeight * 0.78)),
        color: '#E69A28',
        font: 'HAND',
        isMathAnswer: true,
        mathExpr: expr,
        createdAt: Date.now(),
      };
      if (settings.aiAnimations) {
        glowsRef.current.push({
          x: anchorX,
          y: anchorY,
          w: sourceWidth + 130,
          h: sourceHeight + 12,
          born: performance.now(),
        });
      }
      commitPageItems((items) => [...items, ansItem]);
      setCheerTrigger((c) => c + 1);
      return true;
    },
    [commitPageItems, settings.aiAnimations, showToast]
  );

  // ================================================================
  // Canvas Animation & Rendering Loop
  // ================================================================

  useEffect(() => {
    let rafId = 0;
    const draw = () => {
      const canvas = canvasRef.current;
      const container = containerRef.current;
      if (canvas && container && page) {
        const dpr = window.devicePixelRatio || 1;
        const cw = container.clientWidth;
        const ch = container.clientHeight;
        if (canvas.width !== Math.round(cw * dpr) || canvas.height !== Math.round(ch * dpr)) {
          canvas.width = Math.round(cw * dpr);
          canvas.height = Math.round(ch * dpr);
        }
        const ctx = canvas.getContext('2d');
        if (ctx) {
          ctx.save();
          ctx.scale(dpr, dpr);

          const paperBg = page.paperColor || settings.paperTint || '#FFFFFF';

          if (isWhiteboard) {
            drawWhiteboardBackdrop(ctx, cw, ch, camera.panX, camera.panY, camera.zoom, paperBg);
          } else {
            ctx.fillStyle = isColorDark(paperBg) ? '#121316' : '#E7E9EF';
            ctx.fillRect(0, 0, cw, ch);
          }

          // World Space Transform
          ctx.save();
          ctx.translate(camera.panX, camera.panY);
          ctx.scale(camera.zoom, camera.zoom);

          if (!isWhiteboard) {
            // Page drop shadow + template
            ctx.save();
            ctx.shadowColor = 'rgba(0,0,0,0.16)';
            ctx.shadowBlur = 24;
            ctx.shadowOffsetY = 8;
            ctx.fillStyle = paperBg;
            ctx.fillRect(0, 0, page.width, page.height);
            ctx.restore();

            drawPaperTemplate(
              ctx,
              page.width,
              page.height,
              page.paper,
              paperBg,
              page.lineScale ?? 1,
              page.lineColor
            );
          }

          // Render committed page items
          renderPageItems(ctx, page, imageCacheRef.current, selectedIds, settings.highlighterBehind);

          // Render live in-progress gesture (wet ink / shape preview / lasso / tape / laser)
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
                opacity: settings.highlighterOpacity,
                penKind,
              };
              renderPageItems(
                ctx,
                { ...page, items: [liveInk] },
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
              ctx.lineDashOffset = -(performance.now() * 0.03) % 26;
              ctx.beginPath();
              g.pts.forEach((p, idx) => (idx === 0 ? ctx.moveTo(p.x, p.y) : ctx.lineTo(p.x, p.y)));
              ctx.closePath();
              ctx.fill();
              ctx.stroke();
              ctx.restore();
            } else if (g.mode === 'ERASE') {
              ctx.save();
              ctx.strokeStyle = 'rgba(229,72,77,0.85)';
              ctx.fillStyle = 'rgba(229,72,77,0.12)';
              ctx.lineWidth = 1.6 / camera.zoom;
              ctx.beginPath();
              ctx.arc(g.lastWorld.x, g.lastWorld.y, eraserSize / 2, 0, Math.PI * 2);
              ctx.fill();
              ctx.stroke();
              ctx.restore();
            }
          }

          // Selection bounding box
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
              ctx.save();
              ctx.strokeStyle = '#3D6CF4';
              ctx.lineWidth = 2 / camera.zoom;
              ctx.setLineDash([7 / camera.zoom, 5 / camera.zoom]);
              ctx.strokeRect(l - 6, t - 6, r - l + 12, b - t + 12);
              ctx.restore();
            }
          }

          // Animated Apple-Notes-style Math Glow Sweeps
          const now = performance.now();
          glowsRef.current = glowsRef.current.filter((sw) => now - sw.born < 950);
          for (const sw of glowsRef.current) {
            const t = (now - sw.born) / 950;
            const headX = sw.x + sw.w * Math.min(1, t * 1.15);
            ctx.save();
            const grad = ctx.createLinearGradient(sw.x, sw.y, headX + 40, sw.y);
            grad.addColorStop(0, 'rgba(139,92,246,0)');
            grad.addColorStop(0.5, `rgba(242,169,42,${0.55 * (1 - t)})`);
            grad.addColorStop(1, 'rgba(56,189,248,0)');
            ctx.fillStyle = grad;
            ctx.beginPath();
            ctx.roundRect(sw.x - 10, sw.y - 6, Math.max(20, headX - sw.x + 20), sw.h + 12, 12);
            ctx.fill();
            ctx.restore();
          }

          // Scribble-erase Crumble Particles
          puffsRef.current = puffsRef.current.filter((pf) => now - pf.born < 520);
          for (const pf of puffsRef.current) {
            const t = (now - pf.born) / 520;
            ctx.save();
            ctx.globalAlpha = 1 - t;
            ctx.fillStyle = pf.color;
            ctx.beginPath();
            ctx.arc(pf.x + pf.vx * t * 32, pf.y + pf.vy * t * 32, pf.r * (1 - t * 0.4), 0, Math.PI * 2);
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

          ctx.restore(); // end world transform
          ctx.restore(); // end dpr
        }
      }
      rafId = requestAnimationFrame(draw);
    };
    rafId = requestAnimationFrame(draw);
    return () => cancelAnimationFrame(rafId);
  }, [page, isWhiteboard, camera, settings, selectedIds, tool, penColor, penSize, penKind, hlColor, hlSize, eraserSize, shapeKind, tapeColor]);

  // ================================================================
  // Canvas Pointer Handlers (with Live Scribble-to-Erase!)
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

  const eraseAtPoint = (world: Pt) => {
    if (!page) return;
    const r = eraserSize / 2;
    const hit = page.items.filter((it) => {
      if (it.kind === 'ink') {
        return it.points.some((p) => dist(p, world) <= r + it.size / 2);
      }
      if (it.kind === 'text') {
        const b = getItemBounds(it);
        return world.x >= b.l - r && world.x <= b.r + r && world.y >= b.t - r && world.y <= b.b + r;
      }
      return false;
    });
    if (hit.length > 0) {
      const hitIds = new Set(hit.map((h) => h.id));
      spawnPuffForItems(hit);
      commitPageItems((items) => items.filter((it) => !hitIds.has(it.id)), false);
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

    // Pan with Spacebar, Middle mouse, or Pan tool
    if (spaceDown || e.button === 1 || tool === 'PAN') {
      g.mode = 'PAN';
      return;
    }

    // Check Mind Map node hit first in MINDMAP or LASSO mode
    const hitMind = hitTestMindNode(world);
    if (tool === 'MINDMAP') {
      if (hitMind) {
        setSelectedIds(new Set([hitMind.id]));
        g.mode = 'DRAG_MIND';
        g.dragMindId = hitMind.id;
        g.dragOrigItems = structuredClone(page.items);
      } else {
        setSelectedIds(new Set());
        // Clicking empty canvas in Mind Map tool creates a node if no parent or adds child to root!
        const root = mindNodesOnPage[0] || null;
        addMindNode(root, world, root ? 'Branch Idea' : 'Central Topic');
        g.active = false;
      }
      return;
    }

    // Check dragging existing selection
    if (selectedIds.size > 0 && tool !== 'ERASER') {
      const hitSel = hitTestAnyItem(world);
      if (hitSel && selectedIds.has(hitSel.id)) {
        g.mode = 'DRAG_ITEMS';
        g.dragOrigItems = structuredClone(page.items);
        return;
      }
      setSelectedIds(new Set());
    }

    if (tool === 'PEN' || tool === 'HIGHLIGHTER') {
      g.mode = 'INK';
      setWritingActive(true);
    } else if (tool === 'ERASER') {
      g.mode = 'ERASE';
      setUndoStack((s) => [...s.slice(-30), structuredClone(notebook.pages)]);
      eraseAtPoint(world);
    } else if (tool === 'SHAPE') {
      if (shapeKind === 'AUTO') {
        g.mode = 'INK';
      } else {
        g.mode = 'SHAPE';
      }
    } else if (tool === 'LASSO') {
      g.mode = 'LASSO';
      g.pts = [{ x: world.x, y: world.y }];
    } else if (tool === 'TAPE') {
      g.mode = 'TAPE';
    } else if (tool === 'LASER') {
      g.mode = 'LASER';
      lasersRef.current.push({ pts: [world], born: performance.now() });
    } else if (tool === 'STICKY') {
      const hit = hitTestAnyItem(world);
      if (hit && hit.kind === 'sticky') {
        setSelectedIds(new Set([hit.id]));
        const nextText = window.prompt('Edit sticky note:', hit.text);
        if (nextText !== null) {
          commitPageItems((items) =>
            items.map((it) => (it.id === hit.id && it.kind === 'sticky' ? { ...it, text: nextText } : it))
          );
        }
      } else {
        const newSticky: StickyItem = {
          id: `stk-${Date.now()}`,
          kind: 'sticky',
          x: world.x - 120,
          y: world.y - 90,
          w: 240,
          h: 180,
          color: stickyColor,
          text: 'Sticky note…',
        };
        commitPageItems((items) => [...items, newSticky]);
        setSelectedIds(new Set([newSticky.id]));
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
  };

  const handlePointerMove = (e: React.PointerEvent<HTMLCanvasElement>) => {
    const g = gestureRef.current;
    if (!g.active || !page) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const sx = e.clientX - rect.left;
    const sy = e.clientY - rect.top;
    const world = screenToWorld(sx, sy);

    if (g.mode === 'PAN') {
      const dx = sx - g.lastScreen.x;
      const dy = sy - g.lastScreen.y;
      setCamera((c) => ({ ...c, panX: c.panX + dx, panY: c.panY + dy }));
      g.lastScreen = { x: sx, y: sy };
      return;
    }

    if (g.mode === 'DRAG_MIND' && g.dragMindId) {
      const dx = world.x - g.startWorld.x;
      const dy = world.y - g.startWorld.y;
      const snap = settings.whiteboardGridSnap ? 20 : 1;
      const orig = g.dragOrigItems?.find((i) => i.id === g.dragMindId) as MindNodeItem | undefined;
      if (orig) {
        const nx = Math.round((orig.x + dx) / snap) * snap;
        const ny = Math.round((orig.y + dy) / snap) * snap;
        commitPageItems(
          (items) => items.map((it) => (it.id === g.dragMindId ? { ...(it as MindNodeItem), x: nx, y: ny } : it)),
          false
        );
      }
      g.lastWorld = world;
      return;
    }

    if (g.mode === 'DRAG_ITEMS' && g.dragOrigItems) {
      const dx = world.x - g.startWorld.x;
      const dy = world.y - g.startWorld.y;
      commitPageItems(
        () =>
          g.dragOrigItems!.map((it) => {
            if (!selectedIds.has(it.id)) return it;
            if (it.kind === 'ink') {
              return { ...it, points: it.points.map((p) => ({ ...p, x: p.x + dx, y: p.y + dy })) };
            }
            if (it.kind === 'fill') {
              return { ...it, points: it.points.map((p) => ({ x: p.x + dx, y: p.y + dy })) };
            }
            return { ...it, x: it.x + dx, y: it.y + dy };
          }),
        false
      );
      g.lastWorld = world;
      return;
    }

    if (g.mode === 'INK') {
      const prev = g.pts[g.pts.length - 1];
      if (!prev || dist(prev, world) >= 2.2) {
        g.pts.push({ x: world.x, y: world.y, p: e.pressure || 0.65 });
      }

      // LIVE SCRIBBLE-TO-ERASE WHILE DRAWING!
      if (
        settings.scribbleToErase &&
        settings.liveScribbleErase &&
        (tool === 'PEN' || tool === 'HIGHLIGHTER') &&
        isLiveScribble(g.pts)
      ) {
        const isHl = tool === 'HIGHLIGHTER';
        const recent = g.pts.slice(-52);
        const hits = page.items.filter((it) => scribbleHitsItem(recent, it, isHl));
        if (hits.length > 0) {
          if (!g.scribbleConsumed) {
            setUndoStack((s) => [...s.slice(-30), structuredClone(notebook.pages)]);
          }
          g.scribbleConsumed = true;
          const hitIds = new Set(hits.map((h) => h.id));
          spawnPuffForItems(hits);
          commitPageItems((items) => items.filter((it) => !hitIds.has(it.id)), false);
          // Keep tail of stroke so continuous scrubbing can keep erasing nearby ink seamlessly
          g.pts = g.pts.slice(-14);
        }
      }
    } else if (g.mode === 'ERASE') {
      eraseAtPoint(world);
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
    } else if (g.mode === 'LASER') {
      const last = lasersRef.current[lasersRef.current.length - 1];
      if (last) {
        last.pts.push(world);
        last.born = performance.now();
      }
    }

    g.lastScreen = { x: sx, y: sy };
    g.lastWorld = world;
  };

  const handlePointerUp = () => {
    const g = gestureRef.current;
    if (!g.active || !page) return;
    g.active = false;
    setWritingActive(false);

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

    if (g.mode === 'DRAG_MIND' || g.mode === 'DRAG_ITEMS') {
      if (g.dragOrigItems) {
        setUndoStack((s) => [
          ...s.slice(-30),
          notebook.pages.map((pg, i) => (i === safeIdx ? { ...pg, items: g.dragOrigItems! } : pg)),
        ]);
      }
      g.dragMindId = null;
      g.dragOrigItems = null;
      g.mode = 'NONE';
      return;
    }

    if (g.mode === 'INK') {
      if (g.scribbleConsumed) {
        g.mode = 'NONE';
        g.pts = [];
        return;
      }

      // Final check for fast scribble-to-erase on release
      if (
        settings.scribbleToErase &&
        (tool === 'PEN' || tool === 'HIGHLIGHTER') &&
        isScribble(g.pts)
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

      // Auto Shape Tool
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
            penKind: 'ballpoint',
          };
          commitPageItems((items) => [...items, ink]);
          g.mode = 'NONE';
          g.pts = [];
          return;
        }
      }

      // Highlighter Auto-Straighten
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

      // Tap on Tape toggles reveal
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
        opacity: settings.highlighterOpacity,
        penKind,
      };
      commitPageItems((items) => [...items, ink]);
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
    }

    g.mode = 'NONE';
    g.pts = [];
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
  // AI Actions (Summary, Flashcards, Quiz, Explain, Mind Map Gen)
  // ================================================================

  const handleAiSummary = async () => {
    setAiMenuOpen(false);
    setAiBusyLabel('Summarizing notes');
    try {
      const txt = extractNotebookText(notebook.pages);
      const res = await runLocalSummary(txt, settings.activeLocalModelId);
      setSummaryModal({
        open: true,
        title: `Key Points · ${notebook.title}`,
        points: res.points,
        engineLabel: res.engineLabel,
        explainText: null,
      });
      setCheerTrigger((c) => c + 1);
    } finally {
      setAiBusyLabel(null);
    }
  };

  const handleAiFlashcards = async () => {
    setAiMenuOpen(false);
    setAiBusyLabel('Crafting flashcards');
    try {
      const txt = extractNotebookText(notebook.pages);
      const res = await runLocalFlashcards(txt, notebook.id, settings.activeLocalModelId);
      if (res.cards.length > 0) {
        onAddFlashcards(res.cards);
        showToast(`Added ${res.cards.length} flashcards (${res.engineLabel})`);
        setCheerTrigger((c) => c + 1);
      } else {
        showToast('Add definitions like "X is..." to auto-generate cards');
      }
      setFlashModalOpen(true);
    } finally {
      setAiBusyLabel(null);
    }
  };

  const handleAiQuiz = async () => {
    setAiMenuOpen(false);
    setAiBusyLabel('Building quiz');
    try {
      const txt = extractNotebookText(notebook.pages);
      const res = await runLocalQuiz(txt, settings.activeLocalModelId);
      setQuizModal({
        open: true,
        questions: res.questions,
        engineLabel: res.engineLabel,
      });
      setCheerTrigger((c) => c + 1);
    } finally {
      setAiBusyLabel(null);
    }
  };

  const handleAiGenerateMindMap = async () => {
    setAiMenuOpen(false);
    setAiBusyLabel('Mapping concepts');
    try {
      const txt = extractNotebookText(notebook.pages) || notebook.title;
      const center = screenToWorld(
        (containerRef.current?.clientWidth || 800) / 2,
        (containerRef.current?.clientHeight || 600) / 2
      );
      const res = await generateMindMapFromText(txt, center.x, center.y, settings.activeLocalModelId);
      commitPageItems((items) => [...items, ...res.nodes]);
      setTool('MINDMAP');
      showToast(`Mind Map generated (${res.engineLabel})`);
      setCheerTrigger((c) => c + 1);
    } finally {
      setAiBusyLabel(null);
    }
  };

  const handleAiExplainSelection = async () => {
    if (!page) return;
    const selItems = page.items.filter((i) => selectedIds.has(i.id));
    const selTxt = selItems
      .map((i) => (i.kind === 'text' ? i.text : i.kind === 'sticky' ? i.text : i.kind === 'mindnode' ? i.label : ''))
      .filter(Boolean)
      .join(' ');
    if (!selTxt.trim()) {
      showToast('Select text, a sticky note, or a mind map node to explain');
      return;
    }
    setAiBusyLabel('Explaining selection');
    try {
      const ctxTxt = extractNotebookText([page]);
      const res = await runLocalExplain(selTxt, ctxTxt, settings.activeLocalModelId);
      setSummaryModal({
        open: true,
        title: 'Simple Explanation',
        points: [],
        engineLabel: res.engineLabel,
        explainText: res.explanation,
      });
    } finally {
      setAiBusyLabel(null);
    }
  };

  const handleSolveSelectionMath = () => {
    if (!page) return;
    const selItems = page.items.filter((i) => selectedIds.has(i.id));
    const txtItem = selItems.find((i): i is TextItem => i.kind === 'text');
    if (txtItem) {
      const b = getItemBounds(txtItem);
      if (placeMathAnswer(txtItem.text, b.l, b.t, b.w, b.h)) return;
    }
    setMathPadOpen(true);
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
      width: page?.width || 1240,
      height: page?.height || 1754,
      paper: page?.paper || notebook.paper,
      isWhiteboard: page?.isWhiteboard,
      paperColor: page?.paperColor,
      items: [],
    };
    commitPages((pages) => [...pages, newPg]);
    setPageIdx(notebook.pages.length);
  };

  const searchHits = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    if (!q) return [];
    const hits: { pageIdx: number; preview: string }[] = [];
    notebook.pages.forEach((pg, idx) => {
      for (const it of pg.items) {
        const txt =
          it.kind === 'text' ? it.text : it.kind === 'sticky' ? it.text : it.kind === 'mindnode' ? it.label : '';
        if (txt.toLowerCase().includes(q)) {
          hits.push({ pageIdx: idx, preview: txt.slice(0, 72) });
        }
      }
    });
    return hits;
  }, [notebook.pages, searchQuery]);

  const splitNotebook = splitNoteId ? allNotebooks.find((n) => n.id === splitNoteId) : null;

  const TOOLS: { id: ToolId; label: string; icon: React.ComponentType<{ className?: string }> }[] = [
    { id: 'PEN', label: 'Pen', icon: Pen },
    { id: 'HIGHLIGHTER', label: 'Highlight', icon: Highlighter },
    { id: 'ERASER', label: 'Eraser', icon: Eraser },
    { id: 'SHAPE', label: 'Shape', icon: Shapes },
    { id: 'LASSO', label: 'Lasso', icon: LassoIcon },
    { id: 'MINDMAP', label: 'Mind Map', icon: Network },
    { id: 'TEXT', label: 'Text', icon: Type },
    { id: 'STICKY', label: 'Sticky', icon: StickyNote },
    { id: 'TAPE', label: 'Tape', icon: BookmarkMinus },
    { id: 'STICKER', label: 'Sticker', icon: Sticker },
    { id: 'IMAGE', label: 'Image', icon: ImageIcon },
    { id: 'LASER', label: 'Laser', icon: Wand2 },
    { id: 'PAN', label: 'Pan', icon: Hand },
  ];

  // Calculate screen coordinates of Selected Mind Node for contextual floating controls
  const selectedMindScreen = useMemo(() => {
    if (!selectedMindNode) return null;
    return {
      left: (selectedMindNode.x + selectedMindNode.w / 2) * camera.zoom + camera.panX,
      top: selectedMindNode.y * camera.zoom + camera.panY,
    };
  }, [selectedMindNode, camera]);

  return (
    <div className="flex h-screen w-screen flex-col overflow-hidden bg-[#E7E9EF] text-[#1B1F2A] dark:bg-[#0F1013] dark:text-[#F3F4F6]">
      {/* Open Tabs Strip */}
      {settings.showTabs && openTabIds.length > 0 && (
        <div className="flex h-8 shrink-0 items-center gap-1 border-b border-black/8 bg-[#ECEEF3] px-2 dark:border-white/8 dark:bg-[#141518]">
          {openTabIds.map((tid) => {
            const nb = allNotebooks.find((n) => n.id === tid);
            if (!nb) return null;
            const active = tid === notebook.id;
            return (
              <div
                key={tid}
                className={`group flex max-w-[180px] items-center gap-1.5 rounded-t-lg px-2.5 py-1 text-xs font-medium transition ${
                  active
                    ? 'bg-white text-slate-900 shadow-sm dark:bg-[#212329] dark:text-white'
                    : 'text-slate-500 hover:bg-black/5 dark:text-slate-400'
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
      )}

      {/* Main Editor Header Bar */}
      <header
        className="relative z-30 flex shrink-0 items-center justify-between gap-2 border-b border-black/10 px-3 py-2 shadow-sm dark:border-white/10"
        style={{
          backgroundColor: document.documentElement.classList.contains('dark')
            ? accent.softDarkBg
            : accent.softBg,
        }}
      >
        {/* Left Nav & History & Pip */}
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
          {settings.pet && (
            <div className="ml-1 hidden sm:block">
              <PipPet writing={writingActive} thinking={Boolean(aiBusyLabel)} cheerTrigger={cheerTrigger} />
            </div>
          )}
        </div>

        {/* Center Tools Strip */}
        <div className="flex items-center gap-0.5 overflow-x-auto rounded-2xl border border-black/8 bg-white/85 px-1.5 py-1 shadow-sm backdrop-blur-md dark:border-white/10 dark:bg-[#1B1D23]/90">
          {TOOLS.map((t) => {
            const Icon = t.icon;
            const active = tool === t.id;
            return (
              <button
                key={t.id}
                type="button"
                title={t.label}
                onClick={() => {
                  if (t.id === 'IMAGE') {
                    fileInputRef.current?.click();
                  } else if (t.id === 'STICKER') {
                    setStickerOpen((v) => !v);
                  } else {
                    setTool(t.id);
                    setStickerOpen(false);
                  }
                }}
                className={`flex items-center gap-1.5 rounded-xl px-2.5 py-1.5 text-xs font-semibold transition ${
                  active
                    ? 'text-white shadow-sm'
                    : 'text-slate-600 hover:bg-black/5 dark:text-slate-300 dark:hover:bg-white/10'
                }`}
                style={active ? { backgroundColor: accent.primary } : undefined}
              >
                <Icon className="h-4 w-4 shrink-0" />
                <span className="hidden xl:inline">{t.label}</span>
              </button>
            );
          })}
          <input ref={fileInputRef} type="file" accept="image/*" onChange={handleInsertImage} className="hidden" />
        </div>

        {/* Right Actions: Study Timer, AI Spark, +Page, More */}
        <div className="flex items-center gap-1">
          {settings.studyTimer && (
            <button
              type="button"
              onClick={() => setTimerRunning((r) => !r)}
              onDoubleClick={() => setStudySec(0)}
              title="Tap to pause/resume · Double-click to reset"
              className="hidden items-center gap-1.5 rounded-xl bg-white/75 px-2.5 py-1.5 text-xs font-semibold shadow-sm dark:bg-white/10 md:inline-flex"
            >
              <Timer className={`h-3.5 w-3.5 ${timerRunning ? 'text-emerald-600' : 'text-slate-400'}`} />
              <span className="font-mono-num">
                {Math.floor(studySec / 60)}:{String(studySec % 60).padStart(2, '0')}
              </span>
            </button>
          )}

          {/* Maths Quick-Pad button */}
          <button
            type="button"
            onClick={() => setMathPadOpen((v) => !v)}
            title="Maths & Equation Solver"
            className="inline-flex items-center gap-1 rounded-xl bg-amber-500/15 px-2.5 py-1.5 text-xs font-bold text-amber-800 hover:bg-amber-500/25 dark:text-amber-300"
          >
            <Calculator className="h-3.5 w-3.5" />
            <span className="hidden lg:inline">Maths</span>
          </button>

          {/* AI Spark Menu */}
          <div className="relative">
            <button
              type="button"
              onClick={() => setAiMenuOpen((v) => !v)}
              className="inline-flex items-center gap-1.5 rounded-xl bg-gradient-to-r from-violet-600 to-blue-600 px-3 py-1.5 text-xs font-semibold text-white shadow-sm hover:opacity-95"
            >
              <Sparkles className="h-3.5 w-3.5" />
              <span className="hidden sm:inline">AI Study</span>
            </button>
            {aiMenuOpen && (
              <div className="absolute right-0 top-full z-50 mt-2 w-64 rounded-2xl border border-black/10 bg-white p-1.5 shadow-2xl dark:border-white/10 dark:bg-[#1E2027]">
                <div className="px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                  {activeModel ? `Model: ${activeModel.name}` : 'Built-in On-Device Engine'}
                </div>
                <button
                  type="button"
                  onClick={handleAiSummary}
                  className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-xs font-semibold hover:bg-black/5 dark:hover:bg-white/10"
                >
                  <FileText className="h-4 w-4 text-violet-600" /> Summarize Page Key Points
                </button>
                <button
                  type="button"
                  onClick={handleAiGenerateMindMap}
                  className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-xs font-semibold hover:bg-black/5 dark:hover:bg-white/10"
                >
                  <Brain className="h-4 w-4 text-emerald-600" /> Turn Notes into Mind Map
                </button>
                <button
                  type="button"
                  onClick={handleAiFlashcards}
                  className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-xs font-semibold hover:bg-black/5 dark:hover:bg-white/10"
                >
                  <BookOpen className="h-4 w-4 text-blue-600" /> Make Flashcards from Notes
                </button>
                <button
                  type="button"
                  onClick={handleAiQuiz}
                  className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-xs font-semibold hover:bg-black/5 dark:hover:bg-white/10"
                >
                  <Sparkles className="h-4 w-4 text-amber-500" /> Take Study Quiz
                </button>
                <div className="my-1 border-t border-black/8 dark:border-white/10" />
                <button
                  type="button"
                  onClick={() => {
                    setAiMenuOpen(false);
                    onOpenLocalModels();
                  }}
                  className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-xs font-semibold text-violet-700 hover:bg-violet-500/10 dark:text-violet-300"
                >
                  <Cpu className="h-4 w-4" /> Download / Switch Local Models…
                </button>
              </div>
            )}
          </div>

          <button
            type="button"
            onClick={addNewPage}
            title="Add new page"
            className="rounded-xl p-2 hover:bg-black/8 dark:hover:bg-white/10"
          >
            <Plus className="h-4 w-4" />
          </button>

          <div className="relative">
            <button
              type="button"
              onClick={() => setMoreMenuOpen((v) => !v)}
              className="rounded-xl p-2 hover:bg-black/8 dark:hover:bg-white/10"
            >
              <MoreHorizontal className="h-4 w-4" />
            </button>
            {moreMenuOpen && (
              <div className="absolute right-0 top-full z-50 mt-2 w-60 rounded-2xl border border-black/10 bg-white p-1.5 shadow-2xl dark:border-white/10 dark:bg-[#1E2027]">
                <button
                  type="button"
                  onClick={() => {
                    commitPages((pages) =>
                      pages.map((pg, i) => (i === safeIdx ? { ...pg, isWhiteboard: !pg.isWhiteboard } : pg))
                    );
                    setMoreMenuOpen(false);
                    showToast(isWhiteboard ? 'Switched to bounded page' : 'Switched to Infinite Whiteboard');
                  }}
                  className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-xs font-semibold hover:bg-black/5 dark:hover:bg-white/10"
                >
                  <Layers className="h-4 w-4 text-emerald-600" />
                  {isWhiteboard ? 'Use Standard Bounded Page' : 'Toggle Infinite Whiteboard'}
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setTemplateSheetOpen(true);
                    setMoreMenuOpen(false);
                  }}
                  className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-xs font-semibold hover:bg-black/5 dark:hover:bg-white/10"
                >
                  <FileText className="h-4 w-4 text-blue-600" /> Paper Template & Color…
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setFlashModalOpen(true);
                    setMoreMenuOpen(false);
                  }}
                  className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-xs font-semibold hover:bg-black/5 dark:hover:bg-white/10"
                >
                  <BookOpen className="h-4 w-4" /> View Flashcard Deck ({flashcards.length})
                </button>
                <button
                  type="button"
                  onClick={() => {
                    const other = allNotebooks.find((n) => n.id !== notebook.id && n.deletedAt === null);
                    setSplitNoteId((cur) => (cur ? null : other?.id || null));
                    setMoreMenuOpen(false);
                  }}
                  className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-xs font-semibold hover:bg-black/5 dark:hover:bg-white/10"
                >
                  <Columns2 className="h-4 w-4" /> {splitNoteId ? 'Close Split View' : 'Open Split View'}
                </button>
                <button
                  type="button"
                  onClick={() => {
                    handleExportPng();
                    setMoreMenuOpen(false);
                  }}
                  className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-xs font-semibold hover:bg-black/5 dark:hover:bg-white/10"
                >
                  <Download className="h-4 w-4" /> Export Current View as PNG
                </button>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* Secondary Contextual Tool Options Bar */}
      <div className="z-20 flex shrink-0 flex-wrap items-center justify-center gap-3 border-b border-black/6 bg-white/90 px-4 py-1.5 text-xs backdrop-blur-md dark:border-white/8 dark:bg-[#191A20]/90">
        {(tool === 'PEN' || tool === 'SHAPE' || tool === 'TEXT') && (
          <>
            {tool === 'PEN' && (
              <div className="flex items-center gap-1 rounded-lg bg-black/5 p-0.5 dark:bg-white/8">
                {(['ballpoint', 'fountain', 'brush'] as PenKind[]).map((k) => (
                  <button
                    key={k}
                    type="button"
                    onClick={() => setPenKind(k)}
                    className={`rounded-md px-2 py-0.5 text-[11px] font-semibold capitalize ${
                      penKind === k ? 'bg-white shadow-sm dark:bg-[#2A2C34]' : 'text-slate-500'
                    }`}
                  >
                    {k}
                  </button>
                ))}
              </div>
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
            </div>
            {tool === 'TEXT' ? (
              <div className="flex items-center gap-2">
                <select
                  value={textFont}
                  onChange={(e) => setTextFont(e.target.value as TextFont)}
                  className="rounded-lg border border-black/10 bg-transparent px-2 py-0.5 text-[11px] font-semibold dark:border-white/15"
                >
                  <option value="SANS">Sans</option>
                  <option value="SERIF">Serif</option>
                  <option value="MONO">Mono</option>
                  <option value="HAND">Handwriting</option>
                </select>
                <input
                  type="range"
                  min={16}
                  max={64}
                  value={textSize}
                  onChange={(e) => setTextSize(Number(e.target.value))}
                  className="w-20 accent-blue-600"
                />
                <span className="font-mono-num text-[11px]">{textSize}px</span>
              </div>
            ) : (
              <div className="flex items-center gap-2">
                <span className="text-[11px] text-slate-500">Size</span>
                <input
                  type="range"
                  min={1.5}
                  max={14}
                  step={0.5}
                  value={penSize}
                  onChange={(e) => setPenSize(Number(e.target.value))}
                  className="w-20 accent-blue-600"
                />
                <span className="font-mono-num text-[11px]">{penSize}</span>
              </div>
            )}
            {tool === 'SHAPE' && (
              <select
                value={shapeKind}
                onChange={(e) => setShapeKind(e.target.value as ShapeKind)}
                className="rounded-lg border border-black/10 bg-transparent px-2 py-0.5 text-[11px] font-semibold dark:border-white/15"
              >
                {SHAPE_KINDS.map((sk) => (
                  <option key={sk.id} value={sk.id}>
                    {sk.label}
                  </option>
                ))}
              </select>
            )}
            {tool === 'PEN' && settings.liveScribbleErase && (
              <span className="hidden text-[11px] text-emerald-600 dark:text-emerald-400 md:inline">
                • Live scribble-erase active
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
                    hlColor === c ? 'scale-115 ring-2 ring-blue-500' : 'border-black/15'
                  }`}
                  style={{ backgroundColor: c }}
                />
              ))}
            </div>
            <input
              type="range"
              min={12}
              max={44}
              value={hlSize}
              onChange={(e) => setHlSize(Number(e.target.value))}
              className="w-24 accent-amber-500"
            />
            <span className="font-mono-num text-[11px]">{hlSize}px</span>
          </>
        )}

        {tool === 'ERASER' && (
          <div className="flex items-center gap-2">
            <span className="text-[11px] font-semibold">Eraser diameter</span>
            <input
              type="range"
              min={10}
              max={64}
              value={eraserSize}
              onChange={(e) => setEraserSize(Number(e.target.value))}
              className="w-28 accent-red-500"
            />
            <span className="font-mono-num text-[11px]">{eraserSize}px</span>
          </div>
        )}

        {tool === 'TAPE' && (
          <div className="flex items-center gap-2">
            <select
              value={tapePattern}
              onChange={(e) => setTapePattern(e.target.value)}
              className="rounded-lg border border-black/10 bg-transparent px-2 py-0.5 text-[11px] font-semibold dark:border-white/15"
            >
              {TAPE_PATTERNS.map((tp) => (
                <option key={tp.id} value={tp.id}>
                  {tp.label}
                </option>
              ))}
            </select>
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
            <span className="text-[11px] text-slate-500">Sticky color:</span>
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
          </div>
        )}

        {tool === 'MINDMAP' && (
          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              onClick={() =>
                addMindNode(
                  selectedMindNode || mindNodesOnPage[0] || null,
                  undefined,
                  mindNodesOnPage.length === 0 ? 'Central Topic' : 'Sub-topic'
                )
              }
              className="inline-flex items-center gap-1 rounded-lg bg-blue-600 px-2.5 py-1 text-[11px] font-semibold text-white"
            >
              <Plus className="h-3 w-3" /> {mindNodesOnPage.length === 0 ? 'Root Node' : 'Add Branch'}
            </button>
            <button
              type="button"
              onClick={() => autoLayoutMindMap('RADIAL')}
              className="inline-flex items-center gap-1 rounded-lg bg-black/5 px-2.5 py-1 text-[11px] font-semibold hover:bg-black/10 dark:bg-white/10"
            >
              <Network className="h-3 w-3" /> Radial Layout
            </button>
            <button
              type="button"
              onClick={() => autoLayoutMindMap('TREE')}
              className="inline-flex items-center gap-1 rounded-lg bg-black/5 px-2.5 py-1 text-[11px] font-semibold hover:bg-black/10 dark:bg-white/10"
            >
              <GitBranch className="h-3 w-3" /> Tree Layout
            </button>
            <button
              type="button"
              onClick={handleAiGenerateMindMap}
              className="inline-flex items-center gap-1 rounded-lg bg-violet-600/12 px-2.5 py-1 text-[11px] font-semibold text-violet-700 hover:bg-violet-600/20 dark:text-violet-300"
            >
              <Sparkles className="h-3 w-3" /> AI Map from Notes
            </button>
          </div>
        )}

        {tool === 'LASSO' && (
          <span className="text-[11px] text-slate-500">
            Draw a loop around ink, text, shapes, or nodes to select & transform
          </span>
        )}
        {tool === 'PAN' && (
          <span className="text-[11px] text-slate-500">
            Drag to pan canvas · Scroll or pinch to zoom freely
          </span>
        )}
      </div>

      {/* Main Workspace Area (Pages Drawer + Canvas + Optional Split View) */}
      <div className="relative flex min-h-0 flex-1 overflow-hidden">
        {/* Pages Drawer */}
        {pagesOpen && (
          <aside className="z-20 flex w-52 shrink-0 flex-col border-r border-black/8 bg-white/95 p-3 dark:border-white/10 dark:bg-[#18191E]">
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
          </aside>
        )}

        {/* Canvas Container */}
        <div ref={containerRef} className="relative min-w-0 flex-1 overflow-hidden">
          <canvas
            ref={canvasRef}
            onPointerDown={handlePointerDown}
            onPointerMove={handlePointerMove}
            onPointerUp={handlePointerUp}
            onPointerCancel={handlePointerUp}
            onWheel={handleWheel}
            className={`h-full w-full touch-none ${
              spaceDown || tool === 'PAN'
                ? 'cursor-grab active:cursor-grabbing'
                : tool === 'ERASER'
                  ? 'cursor-cell'
                  : 'cursor-crosshair'
            }`}
          />

          {/* Floating Sticker Picker */}
          {stickerOpen && (
            <div className="absolute left-1/2 top-3 z-40 -translate-x-1/2 rounded-2xl border border-black/10 bg-white p-3 shadow-2xl dark:border-white/10 dark:bg-[#1E2027] animate-stagger-in">
              <div className="mb-1.5 text-[11px] font-bold text-slate-500">Tap a sticker to place</div>
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
                    className="rounded-xl p-1.5 text-2xl hover:bg-black/5 dark:hover:bg-white/10"
                  >
                    {s}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Floating Maths Quick-Pad */}
          {mathPadOpen && (
            <div className="absolute right-4 top-3 z-40 w-80 rounded-2xl border border-black/10 bg-white/95 p-4 shadow-2xl backdrop-blur-xl dark:border-white/10 dark:bg-[#1E2027]/95 animate-stagger-in">
              <div className="mb-2 flex items-center justify-between">
                <span className="inline-flex items-center gap-1.5 text-xs font-bold text-amber-600 dark:text-amber-400">
                  <Calculator className="h-4 w-4" /> Enhanced Maths Engine
                </span>
                <button type="button" onClick={() => setMathPadOpen(false)}>
                  <X className="h-4 w-4 text-slate-400" />
                </button>
              </div>
              <input
                type="text"
                value={mathInput}
                onChange={(e) => setMathInput(e.target.value)}
                placeholder="e.g. x^2 - 5x + 6 = 0 or 72 f to c"
                className="w-full rounded-xl border border-black/12 bg-white px-3 py-2 font-mono-num text-sm dark:border-white/15 dark:bg-[#14151A]"
              />
              <div className="mt-2 flex items-center justify-between rounded-xl bg-amber-500/10 px-3 py-2">
                <span className="text-[11px] font-medium text-slate-500">Live Answer:</span>
                <span className="font-mono-num text-sm font-bold text-amber-700 dark:text-amber-300">
                  {MathSolver.solve(mathInput) || '—'}
                </span>
              </div>
              <div className="mt-2 flex flex-wrap gap-1">
                {['2x + 3 = x + 7', 'x^2 - 5x + 6 = 0', '20% of 150', 'sqrt(144) + 5!', '72 f to c'].map((ex) => (
                  <button
                    key={ex}
                    type="button"
                    onClick={() => setMathInput(ex)}
                    className="rounded-md bg-black/5 px-1.5 py-0.5 font-mono-num text-[10px] hover:bg-black/10 dark:bg-white/10"
                  >
                    {ex}
                  </button>
                ))}
              </div>
              <button
                type="button"
                onClick={() => {
                  const ans = MathSolver.solve(mathInput);
                  if (!ans) return;
                  const c = screenToWorld(
                    (containerRef.current?.clientWidth || 800) / 2,
                    (containerRef.current?.clientHeight || 600) / 2
                  );
                  const exprItem: TextItem = {
                    id: `mexp-${Date.now()}`,
                    kind: 'text',
                    text: mathInput,
                    x: c.x - 160,
                    y: c.y - 20,
                    size: 34,
                    color: penColor,
                    font: 'HAND',
                  };
                  commitPageItems((items) => [...items, exprItem]);
                  setTimeout(() => placeMathAnswer(mathInput, c.x - 160, c.y - 20, mathInput.length * 16 + 20, 44), 30);
                  setMathPadOpen(false);
                }}
                className="mt-3 w-full rounded-xl bg-amber-500 py-2 text-xs font-bold text-white hover:bg-amber-600"
              >
                Write Sum + Answer to Canvas
              </button>
            </div>
          )}

          {/* Contextual Floating Text Editor */}
          {editingTextId && (
            <div className="absolute left-1/2 top-16 z-40 w-80 -translate-x-1/2 rounded-2xl border border-blue-500/30 bg-white p-3.5 shadow-2xl dark:bg-[#1E2027] animate-stagger-in">
              <div className="mb-1.5 flex items-center justify-between text-xs font-bold">
                <span>Edit Text (ends with = to auto-solve)</span>
                <button
                  type="button"
                  onClick={() => {
                    const finalTxt = textDraft.trim();
                    const curItem = page?.items.find((i): i is TextItem => i.id === editingTextId);
                    if (finalTxt && curItem) {
                      commitPageItems((items) =>
                        items.map((it) => (it.id === editingTextId && it.kind === 'text' ? { ...it, text: finalTxt } : it))
                      );
                      if (settings.mathAssist && (finalTxt.endsWith('=') || /[xX]/.test(finalTxt))) {
                        const ans = MathSolver.solve(finalTxt);
                        if (ans) {
                          const b = getItemBounds({ ...curItem, text: finalTxt });
                          placeMathAnswer(finalTxt, b.l, b.t, b.w, b.h);
                        }
                      }
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
                placeholder="Write notes or math like 2x + 3 = x + 7 ="
                className="w-full rounded-xl border border-black/12 p-2 text-sm outline-none focus:border-blue-500 dark:border-white/15 dark:bg-[#14151A]"
              />
            </div>
          )}

          {/* Contextual Floating Mind Map Node Action Bar */}
          {selectedMindNode && selectedMindScreen && (
            <div
              style={{
                left: Math.max(140, Math.min((containerRef.current?.clientWidth || 800) - 160, selectedMindScreen.left)),
                top: Math.max(12, selectedMindScreen.top - 54),
              }}
              className="absolute z-30 flex -translate-x-1/2 items-center gap-1 rounded-2xl border border-black/10 bg-white/95 px-2 py-1.5 shadow-xl backdrop-blur-md dark:border-white/10 dark:bg-[#20222A]/95 animate-stagger-in"
            >
              <button
                type="button"
                onClick={() => addMindNode(selectedMindNode, undefined, 'Child Idea')}
                className="rounded-lg bg-blue-600 px-2 py-1 text-[11px] font-semibold text-white"
              >
                + Child
              </button>
              <button
                type="button"
                onClick={() => {
                  setEditingMindId(selectedMindNode.id);
                  setMindDraft(selectedMindNode.label);
                }}
                className="rounded-lg px-2 py-1 text-[11px] font-semibold hover:bg-black/5 dark:hover:bg-white/10"
              >
                Rename
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
                className="rounded-lg px-2 py-1 text-[11px] font-semibold hover:bg-black/5 dark:hover:bg-white/10"
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
                className="rounded-lg px-2 py-1 text-[11px] font-semibold hover:bg-black/5 dark:hover:bg-white/10"
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
                  className="h-4 w-4 rounded-full"
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
          )}

          {/* Mind Node Inline Rename Modal */}
          {editingMindId && (
            <div className="absolute left-1/2 top-20 z-40 w-72 -translate-x-1/2 rounded-2xl border border-black/10 bg-white p-3.5 shadow-2xl dark:border-white/10 dark:bg-[#1E2027]">
              <div className="mb-2 text-xs font-bold">Node Label</div>
              <input
                autoFocus
                type="text"
                value={mindDraft}
                onChange={(e) => setMindDraft(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    commitPageItems((items) =>
                      items.map((it) =>
                        it.id === editingMindId && it.kind === 'mindnode'
                          ? { ...it, label: mindDraft.trim() || 'Node' }
                          : it
                      )
                    );
                    setEditingMindId(null);
                  }
                }}
                className="w-full rounded-xl border border-black/15 px-3 py-1.5 text-sm dark:border-white/15 dark:bg-[#14151A]"
              />
              <div className="mt-2.5 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setEditingMindId(null)}
                  className="rounded-lg px-2.5 py-1 text-xs text-slate-500"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={() => {
                    commitPageItems((items) =>
                      items.map((it) =>
                        it.id === editingMindId && it.kind === 'mindnode'
                          ? { ...it, label: mindDraft.trim() || 'Node' }
                          : it
                      )
                    );
                    setEditingMindId(null);
                  }}
                  className="rounded-lg bg-blue-600 px-3 py-1 text-xs font-semibold text-white"
                >
                  Save
                </button>
              </div>
            </div>
          )}

          {/* Floating Lasso Selection Action Bar */}
          {selectedIds.size > 0 && !selectedMindNode && (
            <div className="absolute bottom-16 left-1/2 z-30 flex -translate-x-1/2 items-center gap-1.5 rounded-2xl border border-black/10 bg-white/95 px-3 py-2 shadow-2xl backdrop-blur-xl dark:border-white/10 dark:bg-[#1F2128]/95 animate-stagger-in">
              <span className="font-mono-num pr-1 text-xs font-bold text-blue-600">
                {selectedIds.size} selected
              </span>
              <button
                type="button"
                onClick={handleSolveSelectionMath}
                className="inline-flex items-center gap-1 rounded-xl bg-amber-500/15 px-2.5 py-1 text-xs font-semibold text-amber-800 hover:bg-amber-500/25 dark:text-amber-300"
              >
                <Calculator className="h-3.5 w-3.5" /> Solve Maths
              </button>
              <button
                type="button"
                onClick={handleAiExplainSelection}
                className="inline-flex items-center gap-1 rounded-xl bg-violet-500/15 px-2.5 py-1 text-xs font-semibold text-violet-700 hover:bg-violet-500/25 dark:text-violet-300"
              >
                <Sparkles className="h-3.5 w-3.5" /> AI Explain
              </button>
              <button
                type="button"
                onClick={deleteSelected}
                className="inline-flex items-center gap-1 rounded-xl bg-red-500/12 px-2.5 py-1 text-xs font-semibold text-red-600 hover:bg-red-500/20"
              >
                <Trash2 className="h-3.5 w-3.5" /> Delete
              </button>
            </div>
          )}

          {/* Bottom-Left Page Pill or Whiteboard HUD */}
          <div className="absolute bottom-4 left-4 z-20 flex items-center gap-2 rounded-2xl border border-black/10 bg-white/90 px-3 py-1.5 text-xs font-semibold shadow-lg backdrop-blur-md dark:border-white/10 dark:bg-[#1D1F26]/90">
            {isWhiteboard ? (
              <>
                <Layers className="h-3.5 w-3.5 text-emerald-600" />
                <span className="font-mono-num">
                  Infinite Board · {Math.round(camera.zoom * 100)}%
                </span>
                <button
                  type="button"
                  onClick={() => setCamera({ panX: 80, panY: 60, zoom: 0.9 })}
                  title="Reset origin"
                  className="rounded-md bg-black/5 px-1.5 py-0.5 text-[10px] hover:bg-black/10 dark:bg-white/10"
                >
                  Origin
                </button>
              </>
            ) : (
              <>
                <button
                  type="button"
                  disabled={safeIdx === 0}
                  onClick={() => {
                    const ni = Math.max(0, safeIdx - 1);
                    setPageIdx(ni);
                    fitPageToView(notebook.pages[ni]);
                  }}
                  className="disabled:opacity-30"
                >
                  <ChevronLeft className="h-4 w-4" />
                </button>
                <span className="font-mono-num">
                  {safeIdx + 1} / {notebook.pages.length}
                </span>
                <button
                  type="button"
                  disabled={safeIdx >= notebook.pages.length - 1}
                  onClick={() => {
                    const ni = Math.min(notebook.pages.length - 1, safeIdx + 1);
                    setPageIdx(ni);
                    fitPageToView(notebook.pages[ni]);
                  }}
                  className="disabled:opacity-30"
                >
                  <ChevronRight className="h-4 w-4" />
                </button>
              </>
            )}
          </div>

          {/* Bottom-Right Zoom & Fit Controls */}
          <div className="absolute bottom-4 right-4 z-20 flex items-center gap-1 rounded-2xl border border-black/10 bg-white/90 p-1 shadow-lg backdrop-blur-md dark:border-white/10 dark:bg-[#1D1F26]/90">
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
        </div>

        {/* Optional Split View Side Notebook */}
        {splitNotebook && (
          <aside className="hidden w-80 shrink-0 flex-col border-l border-black/10 bg-white dark:border-white/10 dark:bg-[#18191E] lg:flex">
            <div className="flex items-center justify-between border-b border-black/8 px-4 py-3 dark:border-white/10">
              <div className="min-w-0">
                <div className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Split Reference</div>
                <div className="truncate text-sm font-bold">{splitNotebook.title}</div>
              </div>
              <button
                type="button"
                onClick={() => setSplitNoteId(null)}
                className="rounded-lg p-1 text-slate-400 hover:bg-black/5"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
            <div className="flex-1 space-y-3 overflow-y-auto p-4 text-xs leading-relaxed">
              {splitNotebook.pages.map((sp, i) => (
                <div key={sp.id} className="rounded-2xl border border-black/8 bg-slate-50 p-3 dark:border-white/10 dark:bg-[#20222A]">
                  <div className="mb-1 font-mono-num text-[10px] font-bold text-slate-400">Page {i + 1}</div>
                  {sp.items
                    .filter((it) => it.kind === 'text' || it.kind === 'sticky' || it.kind === 'mindnode')
                    .map((it) => (
                      <p key={it.id} className="mb-1.5 text-slate-700 dark:text-slate-200">
                        {it.kind === 'text' ? it.text : it.kind === 'sticky' ? it.text : `• ${it.label}`}
                      </p>
                    ))}
                </div>
              ))}
            </div>
          </aside>
        )}

        {/* Search Drawer */}
        {searchOpen && (
          <aside className="absolute right-4 top-4 z-40 w-72 rounded-2xl border border-black/10 bg-white p-3.5 shadow-2xl dark:border-white/10 dark:bg-[#1E2027]">
            <div className="mb-2 flex items-center gap-2">
              <Search className="h-4 w-4 text-slate-400" />
              <input
                autoFocus
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Find text, stickies, nodes..."
                className="flex-1 bg-transparent text-sm outline-none"
              />
              <button type="button" onClick={() => setSearchOpen(false)}>
                <X className="h-4 w-4 text-slate-400" />
              </button>
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
                  className="flex w-full flex-col rounded-xl bg-black/[0.03] px-2.5 py-2 text-left text-xs hover:bg-blue-500/10 dark:bg-white/[0.04]"
                >
                  <span className="font-mono-num text-[10px] font-bold text-blue-600">
                    Page {h.pageIdx + 1}
                  </span>
                  <span className="truncate">{h.preview}</span>
                </button>
              ))}
              {searchQuery && searchHits.length === 0 && (
                <div className="py-3 text-center text-xs text-slate-400">No matches found</div>
              )}
            </div>
          </aside>
        )}
      </div>

      {/* Template Sheet Modal */}
      {templateSheetOpen && page && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
          <div className="flex max-h-[85vh] w-full max-w-2xl flex-col overflow-hidden rounded-3xl border border-black/10 bg-white shadow-2xl dark:border-white/10 dark:bg-[#191A1F]">
            <div className="flex items-center justify-between border-b border-black/8 px-6 py-4 dark:border-white/10">
              <h2 className="text-base font-bold">Page Template & Tint</h2>
              <button type="button" onClick={() => setTemplateSheetOpen(false)}>
                <X className="h-5 w-5 text-slate-400" />
              </button>
            </div>
            <div className="flex-1 space-y-4 overflow-y-auto p-6">
              <div className="flex items-center gap-2">
                <span className="text-xs font-semibold text-slate-500">Paper Tint:</span>
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
                        pages.map((pg, i) => (i === safeIdx ? { ...pg, paper: ps.id } : pg))
                      );
                      setTemplateSheetOpen(false);
                    }}
                    className={`flex flex-col items-center rounded-xl border p-1.5 ${
                      page.paper === ps.id ? 'border-blue-600 ring-2 ring-blue-500/20' : 'border-black/10'
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
        </div>
      )}

      {/* AI Thinking Animated Pill */}
      {aiBusyLabel && <AiThinkingPill label={aiBusyLabel} modelName={activeModel?.name} />}

      {/* Toast Feedback */}
      {toast && (
        <div className="pointer-events-none fixed bottom-20 left-1/2 z-50 -translate-x-1/2 rounded-full bg-[#1B1F2A]/95 px-4 py-2 text-xs font-semibold text-white shadow-xl backdrop-blur-md animate-stagger-in">
          {toast}
        </div>
      )}

      {/* Study Modals */}
      <SummaryModal
        open={summaryModal.open}
        title={summaryModal.title}
        points={summaryModal.points}
        engineLabel={summaryModal.engineLabel}
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
            },
          ]);
        }}
      />

      <FlashcardsModal
        open={flashModalOpen}
        notebookTitle={notebook.title}
        notebookId={notebook.id}
        cards={flashcards}
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
        onClose={() => setQuizModal((s) => ({ ...s, open: false }))}
        onScoreRecorded={(pct) => onRecordQuizScore(notebook.id, pct)}
      />
    </div>
  );
};
