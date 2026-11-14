import { useCallback, useEffect, useRef, useState } from 'react';
import {
  AppSettings,
  DEFAULT_PEN_PRESETS,
  DEFAULT_TOOLBAR_ORDER,
  Flashcard,
  Folder,
  InkItem,
  LocalModelState,
  MindNodeItem,
  NotebookMeta,
  Page,
  PageItem,
  PaperStyle,
  PenPreset,
  SlideCard,
  ToolId,
  UNITS_PER_MM,
} from '../types/models';
import {
  cancelModelDownload,
  deleteLocalModel,
  downloadLocalModel,
  IMAGE_MODELS_CATALOG,
  loadModelStates,
  LOCAL_MODELS_CATALOG,
  saveModelStates,
} from '../utils/localAi';

export interface ProNotesState {
  folders: Folder[];
  notebooks: NotebookMeta[];
  tabs: string[];
  flashcards: Flashcard[];
  bestQuizScores: Record<string, number>;
  studyMinutes: Record<string, number>;
  settings: AppSettings;
}

const DEFAULT_SETTINGS: AppSettings = {
  themeMode: 'SYSTEM',
  accent: 'BLUE',
  appFont: 'MODERN_SANS',
  motionStyle: 'ULTRA_SPRING',
  paperTint: '#FFFFFF',
  defaultPaper: 'RULED',
  defaultPreset: 'A4',
  defaultLandscape: false,
  defaultCoverPage: false,
  scroll: 'VERTICAL',
  toolbarDock: 'HEADER',
  toolbarOrder: DEFAULT_TOOLBAR_ORDER,
  liquidGlass: true,
  glassTransparency: 0.58,
  showTabs: true,
  pageNumbers: true,
  showHoverTooltips: true,
  setupCompleted: false,
  scribbleToErase: true,
  liveScribbleErase: true,
  scribbleSensitivity: 'balanced',
  holdToShape: true,
  circleToLasso: true,
  highlighterBehind: true,
  highlighterMode: 'AUTO',
  highlighterOpacity: 0.58,
  eraserMode: 'STROKE',
  eraseHighlighterOnly: false,
  lassoMode: 'FREEFORM',
  lassoTouching: false,
  studyTimer: true,
  studyAutoStart: true,
  studySeconds: true,
  pomodoroMinutes: 25,
  supportReminders: true,
  lastSupportShownAt: 0,
  activeLocalModelId: null,
  activeImageModelId: 'sdxl-turbo-q4',
  aiAnimations: true,
  customAiEnabled: false,
  customAiProvider: 'OPENAI_COMPATIBLE',
  customAiBaseUrl: 'https://api.openai.com/v1',
  customAiApiKey: '',
  customAiModel: 'gpt-4o-mini',
  whiteboardGridSnap: false,
  penName: 'Pro Stylus',
  penKind: 'fountain',
  penColor: '#1B1F2A',
  penSize: 3.5,
  penOpacity: 1,
  penStabilizer: 0.35,
  penPresets: DEFAULT_PEN_PRESETS,
};

function makeWaveInk(id: string, x0: number, y0: number, color: string): InkItem {
  const pts = [];
  for (let i = 0; i <= 26; i++) {
    const t = i / 26;
    pts.push({
      x: x0 + t * 240,
      y: y0 + Math.sin(t * Math.PI * 3) * 14 - t * 6,
      p: 0.65,
    });
  }
  return { id, kind: 'ink', points: pts, color, size: 3.5, isHighlighter: false, penKind: 'fountain' };
}

function createStarterData(): ProNotesState {
  const now = Date.now();
  const a4W = Math.round(210 * UNITS_PER_MM);
  const a4H = Math.round(297 * UNITS_PER_MM);

  const folderMath: Folder = {
    id: 'fld-stem',
    name: 'STEM & Research',
    parentId: null,
    color: '#3D6CF4',
    emoji: '🧪',
    created: now - 86400000 * 5,
    modified: now - 86400000,
  };

  const nbMathId = 'nb-calculus';
  const nbBoardId = 'nb-whiteboard';
  const nbMindId = 'nb-biology-mindmap';

  const nbMath: NotebookMeta = {
    id: nbMathId,
    title: 'Physics & Study Notes',
    folderId: folderMath.id,
    coverColor: '#3D6CF4',
    paper: 'RULED',
    created: now - 86400000 * 4,
    modified: now - 3600000 * 2,
    opened: now - 1800000,
    pageCount: 2,
    favorite: true,
    deletedAt: null,
    cover: 'pattern:leather:#0E2A47',
    pages: [
      {
        id: 'pg-math-1',
        width: a4W,
        height: a4H,
        paper: 'RULED',
        bookmarked: true,
        items: [
          {
            id: 'txt-m1',
            kind: 'text',
            text: 'Classical Mechanics & Conservation Laws',
            x: 155,
            y: 112,
            size: 36,
            color: '#1B1F2A',
            font: 'SANS',
            bold: true,
          },
          {
            id: 'txt-m2',
            kind: 'text',
            text: 'Tip: Sketch freely with the Pen, hold to snap clean Shapes, or scribble zig-zag directly across ink while drawing to erase it live!',
            x: 155,
            y: 192,
            size: 23,
            color: '#4E586B',
            font: 'SANS',
          },
          {
            id: 'txt-m3',
            kind: 'text',
            text: 'Newton Second Law: F = m × a\nMomentum: p = m × v (conserved in closed systems)',
            x: 165,
            y: 310,
            size: 26,
            color: '#1E293B',
            font: 'SANS',
          },
          {
            id: 'txt-m5',
            kind: 'text',
            text: 'Kinetic Energy is the energy an object possesses due to its motion (KE = 1/2 m v^2).\nEscape Velocity refers to the minimum speed needed to break free from gravitational attraction.',
            x: 165,
            y: 530,
            size: 24,
            color: '#1B1F2A',
            font: 'SANS',
          },
          makeWaveInk('ink-wave-1', 165, 760, '#2A78D6'),
          {
            id: 'stk-m1',
            kind: 'sticky',
            x: 760,
            y: 280,
            w: 320,
            h: 220,
            color: '#FFF3B0',
            text: 'Exam Formulae:\n• sind(30) + cosd(60) = 1\n• d/dx(3x^2 - 4x) = 6x - 4\n• 72 f to c = 22.22 °C',
            rot: -2,
            pinned: true,
          },
          {
            id: 'tape-m1',
            kind: 'tape',
            x: 165,
            y: 860,
            w: 360,
            h: 48,
            color: '#7161E0',
            pattern: 'stripes',
            revealed: false,
          },
        ],
      },
      {
        id: 'pg-math-2',
        width: a4W,
        height: a4H,
        paper: 'GRAPH',
        items: [
          {
            id: 'txt-m6',
            kind: 'text',
            text: 'Coordinate Graph Workspace',
            x: 120,
            y: 120,
            size: 32,
            color: '#1B1F2A',
            font: 'SANS',
            bold: true,
          },
        ],
      },
    ],
  };

  const nbBoard: NotebookMeta = {
    id: nbBoardId,
    title: 'Architecture Infinite Board',
    folderId: null,
    coverColor: '#0F7E5C',
    paper: 'DOTTED',
    isWhiteboard: true,
    created: now - 86400000 * 3,
    modified: now - 7200000,
    opened: now - 3600000,
    pageCount: 1,
    favorite: true,
    deletedAt: null,
    cover: 'pattern:geometric:#0F7E5C',
    pages: [
      {
        id: 'pg-wb-1',
        width: 2400,
        height: 1600,
        paper: 'DOTTED',
        isWhiteboard: true,
        wbPanX: 80,
        wbPanY: 60,
        wbZoom: 0.9,
        items: [
          {
            id: 'wb-title',
            kind: 'text',
            text: 'System Spatial Whiteboard (Pan & Zoom Freely)',
            x: 120,
            y: 90,
            size: 32,
            color: '#1B1F2A',
            font: 'SANS',
            bold: true,
          },
          {
            id: 'wb-stk-1',
            kind: 'sticky',
            x: 140,
            y: 180,
            w: 250,
            h: 190,
            color: '#D8F3DC',
            text: 'Client Layer\n• Low-latency Canvas\n• Live Scribble Erase\n• Local Model Cache',
          },
          {
            id: 'wb-stk-2',
            kind: 'sticky',
            x: 470,
            y: 180,
            w: 250,
            h: 190,
            color: '#CFE2FF',
            text: 'On-Device AI\n• SmolLM2 135M & 360M\n• Qwen 2.5 & Gemma 3\n• Offline Study Gen',
            rot: 1.5,
          },
          {
            id: 'wb-stk-3',
            kind: 'sticky',
            x: 310,
            y: 440,
            w: 270,
            h: 170,
            color: '#FDE2E4',
            text: 'Hold Space or use Pan/Wheel to explore unbounded coordinates!',
            rot: -1.5,
          },
        ],
      },
    ],
  };

  const rootNode: MindNodeItem = {
    id: 'mm-1',
    kind: 'mindnode',
    x: 480,
    y: 340,
    w: 200,
    h: 58,
    label: 'Cellular Respiration',
    parentId: null,
    color: '#1B1F2A',
    shape: 'pill',
  };
  const mmNodes: MindNodeItem[] = [
    rootNode,
    { id: 'mm-2', kind: 'mindnode', x: 140, y: 180, w: 170, h: 44, label: 'Glycolysis', parentId: 'mm-1', color: '#3D6CF4', shape: 'card' },
    { id: 'mm-2a', kind: 'mindnode', x: 50, y: 90, w: 155, h: 36, label: '2 ATP + 2 Pyruvate', parentId: 'mm-2', color: '#3D6CF4', shape: 'pill' },
    { id: 'mm-3', kind: 'mindnode', x: 820, y: 180, w: 170, h: 44, label: 'Krebs Cycle', parentId: 'mm-1', color: '#2BB673', shape: 'card' },
    { id: 'mm-3a', kind: 'mindnode', x: 920, y: 90, w: 165, h: 36, label: 'Mitochondrial Matrix', parentId: 'mm-3', color: '#2BB673', shape: 'pill' },
    { id: 'mm-4', kind: 'mindnode', x: 140, y: 520, w: 180, h: 44, label: 'Electron Transport', parentId: 'mm-1', color: '#9B51E0', shape: 'card' },
    { id: 'mm-5', kind: 'mindnode', x: 820, y: 520, w: 170, h: 44, label: '36–38 Total ATP', parentId: 'mm-1', color: '#F2994A', shape: 'card' },
  ];

  const nbMind: NotebookMeta = {
    id: nbMindId,
    title: 'Cellular Biology Mind Map',
    folderId: folderMath.id,
    coverColor: '#6250D6',
    paper: 'BLANK',
    isWhiteboard: true,
    isMindMap: true,
    created: now - 86400000 * 2,
    modified: now - 1800000,
    opened: now - 900000,
    pageCount: 1,
    favorite: false,
    deletedAt: null,
    cover: 'pattern:gradient:#6250D6',
    pages: [
      {
        id: 'pg-mm-1',
        width: 2200,
        height: 1500,
        paper: 'DOTTED',
        isWhiteboard: true,
        wbPanX: 40,
        wbPanY: 40,
        wbZoom: 0.95,
        items: mmNodes,
      },
    ],
  };

  return {
    folders: [folderMath],
    notebooks: [nbMath, nbBoard, nbMind],
    tabs: [nbMath.id, nbBoard.id, nbMind.id],
    flashcards: [
      {
        id: 'fc-init-1',
        notebookId: nbMathId,
        front: 'What is Kinetic Energy?',
        back: 'The energy an object possesses due to its motion (KE = 1/2 m v²).',
        box: 1,
        due: now,
        created: now,
      },
      {
        id: 'fc-init-2',
        notebookId: nbMindId,
        front: 'Where does the Krebs Cycle take place?',
        back: 'In the mitochondrial matrix.',
        box: 2,
        due: now,
        created: now,
      },
    ],
    bestQuizScores: {},
    studyMinutes: { [nbMathId]: 18 },
    settings: DEFAULT_SETTINGS,
  };
}

const STATE_KEY = 'pronotes-workspace-v960';

function loadInitialState(): ProNotesState {
  try {
    const raw = localStorage.getItem(STATE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as Partial<ProNotesState>;
      if (parsed && Array.isArray(parsed.notebooks) && parsed.notebooks.length > 0) {
        const savedOrder = parsed.settings?.toolbarOrder;
        const mergedOrder: ToolId[] = savedOrder && savedOrder.length > 0
          ? [
              ...savedOrder.filter((id) => DEFAULT_TOOLBAR_ORDER.includes(id)),
              ...DEFAULT_TOOLBAR_ORDER.filter((id) => !savedOrder.includes(id)),
            ]
          : DEFAULT_TOOLBAR_ORDER;
        const mergedSettings: AppSettings = {
          ...DEFAULT_SETTINGS,
          ...(parsed.settings || {}),
          glassTransparency: Math.max(0.15, Math.min(0.95, Number(parsed.settings?.glassTransparency ?? DEFAULT_SETTINGS.glassTransparency) || 0.58)),
          toolbarOrder: mergedOrder,
          penPresets:
            parsed.settings?.penPresets && parsed.settings.penPresets.length > 0
              ? parsed.settings.penPresets
              : DEFAULT_PEN_PRESETS,
        };
        return {
          folders: parsed.folders || [],
          notebooks: parsed.notebooks,
          tabs: parsed.tabs || [],
          flashcards: parsed.flashcards || [],
          bestQuizScores: parsed.bestQuizScores || {},
          studyMinutes: parsed.studyMinutes || {},
          settings: mergedSettings,
        };
      }
    }
  } catch {
    // ignore storage errors
  }
  return createStarterData();
}

export function useProNotesStore() {
  const [state, setState] = useState<ProNotesState>(loadInitialState);
  const [modelStates, setModelStates] = useState<Record<string, LocalModelState>>(loadModelStates);
  const latestStateRef = useRef(state);
  latestStateRef.current = state;

  // Debounced localStorage persistence (prevents synchronous JSON.stringify stalls on every stroke/drag)
  useEffect(() => {
    const timer = window.setTimeout(() => {
      try {
        localStorage.setItem(STATE_KEY, JSON.stringify(latestStateRef.current));
      } catch {
        // ignore quota limits
      }
    }, 450);
    return () => window.clearTimeout(timer);
  }, [state]);

  useEffect(() => {
    const flush = () => {
      try {
        localStorage.setItem(STATE_KEY, JSON.stringify(latestStateRef.current));
      } catch {
        // ignore
      }
    };
    window.addEventListener('beforeunload', flush);
    return () => {
      flush();
      window.removeEventListener('beforeunload', flush);
    };
  }, []);

  useEffect(() => {
    saveModelStates(modelStates);
  }, [modelStates]);

  const updateSettings = useCallback((patch: Partial<AppSettings>) => {
    setState((prev) => ({
      ...prev,
      settings: { ...prev.settings, ...patch },
    }));
  }, []);

  const savePenPreset = useCallback((preset: PenPreset) => {
    setState((prev) => {
      const exists = prev.settings.penPresets.some((p) => p.id === preset.id);
      const next = exists
        ? prev.settings.penPresets.map((p) => (p.id === preset.id ? preset : p))
        : [...prev.settings.penPresets, preset];
      return {
        ...prev,
        settings: { ...prev.settings, penPresets: next },
      };
    });
  }, []);

  const deletePenPreset = useCallback((id: string) => {
    setState((prev) => ({
      ...prev,
      settings: {
        ...prev.settings,
        penPresets: prev.settings.penPresets.filter((p) => p.id !== id),
      },
    }));
  }, []);

  const createFolder = useCallback((name: string, parentId: string | null, color: string, emoji: string) => {
    const now = Date.now();
    const fld: Folder = {
      id: `fld-${now}`,
      name: name.trim() || 'New Folder',
      parentId,
      color,
      emoji,
      created: now,
      modified: now,
    };
    setState((prev) => ({ ...prev, folders: [...prev.folders, fld] }));
    return fld;
  }, []);

  const updateFolder = useCallback((id: string, patch: Partial<Folder>) => {
    setState((prev) => ({
      ...prev,
      folders: prev.folders.map((f) => (f.id === id ? { ...f, ...patch, modified: Date.now() } : f)),
    }));
  }, []);

  const deleteFolder = useCallback((id: string) => {
    const now = Date.now();
    setState((prev) => ({
      ...prev,
      folders: prev.folders.filter((f) => f.id !== id),
      notebooks: prev.notebooks.map((n) =>
        n.folderId === id ? { ...n, folderId: null, deletedAt: now } : n
      ),
    }));
  }, []);

  const createNotebook = useCallback(
    (opts: {
      title: string;
      folderId: string | null;
      cover: string;
      coverColor: string;
      paper: PaperStyle;
      width: number;
      height: number;
      paperColor: string | null;
      coverPage: boolean;
      isWhiteboard?: boolean;
      isMindMap?: boolean;
    }): NotebookMeta => {
      const now = Date.now();
      const id = `nb-${now}`;
      const pages: Page[] = [];
      if (opts.coverPage && opts.cover !== 'thumb' && !opts.isWhiteboard && !opts.isMindMap) {
        pages.push({
          id: `pg-cov-${now}`,
          width: opts.width,
          height: opts.height,
          paper: 'BLANK',
          cover: opts.cover,
          items: [],
        });
      }

      const initialItems: PageItem[] = opts.isMindMap
        ? [
            {
              id: `mm-root-${now}`,
              kind: 'mindnode',
              x: 420,
              y: 300,
              w: 190,
              h: 56,
              label: opts.title.slice(0, 28) || 'Central Topic',
              parentId: null,
              color: opts.coverColor || '#3D6CF4',
              shape: 'pill',
            },
          ]
        : [];

      pages.push({
        id: `pg-${now}-1`,
        width: opts.width,
        height: opts.height,
        paper: opts.paper,
        isWhiteboard: opts.isWhiteboard || opts.isMindMap,
        paperColor: opts.paperColor,
        wbPanX: 40,
        wbPanY: 40,
        wbZoom: 1,
        items: initialItems,
      });

      const nb: NotebookMeta = {
        id,
        title: opts.title.trim() || 'Untitled notebook',
        folderId: opts.folderId,
        coverColor: opts.coverColor,
        paper: opts.paper,
        isWhiteboard: opts.isWhiteboard || opts.isMindMap,
        isMindMap: opts.isMindMap,
        created: now,
        modified: now,
        opened: now,
        pageCount: pages.length,
        favorite: false,
        deletedAt: null,
        cover: opts.cover,
        pages,
      };

      setState((prev) => ({
        ...prev,
        notebooks: [nb, ...prev.notebooks],
        tabs: Array.from(new Set([...prev.tabs, nb.id])).slice(-8),
      }));
      return nb;
    },
    []
  );

  const updateNotebook = useCallback((id: string, updater: (nb: NotebookMeta) => NotebookMeta) => {
    setState((prev) => ({
      ...prev,
      notebooks: prev.notebooks.map((nb) => {
        if (nb.id !== id) return nb;
        const next = updater(nb);
        return { ...next, pageCount: next.pages.length, modified: Date.now() };
      }),
    }));
  }, []);

  const openNotebookTab = useCallback((id: string) => {
    setState((prev) => ({
      ...prev,
      notebooks: prev.notebooks.map((n) => (n.id === id ? { ...n, opened: Date.now() } : n)),
      tabs: prev.tabs.includes(id) ? prev.tabs : [...prev.tabs, id].slice(-8),
    }));
  }, []);

  const closeTab = useCallback((id: string) => {
    setState((prev) => ({ ...prev, tabs: prev.tabs.filter((t) => t !== id) }));
  }, []);

  const softDeleteNotebook = useCallback((id: string) => {
    setState((prev) => ({
      ...prev,
      notebooks: prev.notebooks.map((n) => (n.id === id ? { ...n, deletedAt: Date.now() } : n)),
      tabs: prev.tabs.filter((t) => t !== id),
    }));
  }, []);

  const restoreNotebook = useCallback((id: string) => {
    setState((prev) => ({
      ...prev,
      notebooks: prev.notebooks.map((n) => (n.id === id ? { ...n, deletedAt: null } : n)),
    }));
  }, []);

  const deleteForeverNotebook = useCallback((id: string) => {
    setState((prev) => ({
      ...prev,
      notebooks: prev.notebooks.filter((n) => n.id !== id),
      tabs: prev.tabs.filter((t) => t !== id),
      flashcards: prev.flashcards.filter((c) => c.notebookId !== id),
    }));
  }, []);

  const emptyTrash = useCallback(() => {
    setState((prev) => {
      const keepIds = new Set(prev.notebooks.filter((n) => n.deletedAt === null).map((n) => n.id));
      return {
        ...prev,
        notebooks: prev.notebooks.filter((n) => n.deletedAt === null),
        flashcards: prev.flashcards.filter((c) => keepIds.has(c.notebookId)),
      };
    });
  }, []);

  const duplicateNotebook = useCallback((id: string) => {
    const now = Date.now();
    setState((prev) => {
      const orig = prev.notebooks.find((n) => n.id === id);
      if (!orig) return prev;
      const copy: NotebookMeta = {
        ...structuredClone(orig),
        id: `nb-${now}`,
        title: `${orig.title} (Copy)`,
        created: now,
        modified: now,
        opened: now,
      };
      return { ...prev, notebooks: [copy, ...prev.notebooks] };
    });
  }, []);

  const addFlashcards = useCallback((cards: Flashcard[]) => {
    if (cards.length === 0) return;
    setState((prev) => ({
      ...prev,
      flashcards: [...prev.flashcards, ...cards],
    }));
  }, []);

  const updateFlashcard = useCallback((card: Flashcard) => {
    setState((prev) => ({
      ...prev,
      flashcards: prev.flashcards.map((c) => (c.id === card.id ? card : c)),
    }));
  }, []);

  const deleteFlashcard = useCallback((id: string) => {
    setState((prev) => ({
      ...prev,
      flashcards: prev.flashcards.filter((c) => c.id !== id),
    }));
  }, []);

  const recordQuizScore = useCallback((notebookId: string, pct: number) => {
    setState((prev) => ({
      ...prev,
      bestQuizScores: {
        ...prev.bestQuizScores,
        [notebookId]: Math.max(prev.bestQuizScores[notebookId] || 0, pct),
      },
    }));
  }, []);

  const addStudyMinutes = useCallback((notebookId: string, mins: number) => {
    setState((prev) => ({
      ...prev,
      studyMinutes: {
        ...prev.studyMinutes,
        [notebookId]: (prev.studyMinutes[notebookId] || 0) + mins,
      },
    }));
  }, []);

  const startModelDownload = useCallback(async (modelId: string) => {
    const textSpec = LOCAL_MODELS_CATALOG.find((m) => m.id === modelId);
    const imgSpec = IMAGE_MODELS_CATALOG.find((m) => m.id === modelId);
    const spec = textSpec || imgSpec;
    if (!spec) return;
    const finalState = await downloadLocalModel(spec, (st) => {
      setModelStates((prev) => ({ ...prev, [modelId]: st }));
    });
    if (finalState.status === 'READY') {
      setState((prev) => ({
        ...prev,
        settings: {
          ...prev.settings,
          ...(textSpec
            ? { activeLocalModelId: prev.settings.activeLocalModelId || modelId }
            : { activeImageModelId: modelId }),
        },
      }));
    }
  }, []);

  const pauseModelDownload = useCallback((modelId: string) => {
    cancelModelDownload(modelId);
  }, []);

  const removeLocalModel = useCallback(async (modelId: string) => {
    const reset = await deleteLocalModel(modelId);
    setModelStates((prev) => ({ ...prev, [modelId]: reset }));
    setState((prev) => ({
      ...prev,
      settings: {
        ...prev.settings,
        activeLocalModelId:
          prev.settings.activeLocalModelId === modelId ? null : prev.settings.activeLocalModelId,
        activeImageModelId:
          prev.settings.activeImageModelId === modelId ? 'vectorcraft-svg' : prev.settings.activeImageModelId,
      },
    }));
  }, []);

  const exportBackupJson = useCallback((): string => {
    return JSON.stringify(state, null, 2);
  }, [state]);

  const importBackupJson = useCallback((raw: string): number => {
    const data = JSON.parse(raw) as Partial<ProNotesState>;
    if (!Array.isArray(data.notebooks)) throw new Error('Invalid backup format');
    setState((prev) => {
      const existingIds = new Set(prev.notebooks.map((n) => n.id));
      const addedNbs = data.notebooks!.filter((n) => !existingIds.has(n.id));
      const existingFids = new Set(prev.folders.map((f) => f.id));
      const addedFlds = (data.folders || []).filter((f) => !existingFids.has(f.id));
      return {
        ...prev,
        folders: [...prev.folders, ...addedFlds],
        notebooks: [...addedNbs, ...prev.notebooks],
      };
    });
    return data.notebooks.length;
  }, []);

  const importParsedNotebook = useCallback(
    (title: string, pages: Page[], folderId: string | null = null): NotebookMeta => {
      const now = Date.now();
      const id = `nb-${now}-${Math.random().toString(36).slice(2, 6)}`;
      const safePages = pages.length > 0 ? pages : [
        {
          id: `pg-${now}`,
          width: Math.round(210 * UNITS_PER_MM),
          height: Math.round(297 * UNITS_PER_MM),
          paper: 'RULED' as PaperStyle,
          items: [],
        },
      ];
      const nb: NotebookMeta = {
        id,
        title: title.trim() || 'Imported Note',
        folderId,
        coverColor: '#3D6CF4',
        paper: safePages[0].paper || 'RULED',
        created: now,
        modified: now,
        opened: now,
        pageCount: safePages.length,
        favorite: false,
        deletedAt: null,
        cover: 'pattern:gradient:#3D6CF4',
        pages: safePages,
      };
      setState((prev) => ({
        ...prev,
        notebooks: [nb, ...prev.notebooks],
        tabs: Array.from(new Set([...prev.tabs, nb.id])).slice(-8),
      }));
      return nb;
    },
    []
  );

  const appendPagesToNotebook = useCallback((noteId: string, newPages: Page[]) => {
    if (newPages.length === 0) return;
    setState((prev) => ({
      ...prev,
      notebooks: prev.notebooks.map((nb) => {
        if (nb.id !== noteId) return nb;
        const pages = [...nb.pages, ...newPages];
        return { ...nb, pages, pageCount: pages.length, modified: Date.now() };
      }),
    }));
  }, []);

  const updateNotebookSlides = useCallback((noteId: string, slides: SlideCard[]) => {
    setState((prev) => ({
      ...prev,
      notebooks: prev.notebooks.map((nb) =>
        nb.id === noteId ? { ...nb, slides, modified: Date.now() } : nb
      ),
    }));
  }, []);

  const mergeCloudLibrary = useCallback(
    (cloud: {
      settingsPatch: Partial<AppSettings> | null;
      folders: Folder[];
      flashcards: Flashcard[];
      notebooks: NotebookMeta[];
    }) => {
      setState((prev) => {
        const folderMap = new Map<string, Folder>();
        prev.folders.forEach((f) => folderMap.set(f.id, f));
        cloud.folders.forEach((cf) => {
          const ex = folderMap.get(cf.id);
          if (!ex || cf.modified > ex.modified) folderMap.set(cf.id, cf);
        });

        const nbMap = new Map<string, NotebookMeta>();
        prev.notebooks.forEach((n) => nbMap.set(n.id, n));
        cloud.notebooks.forEach((cn) => {
          const ex = nbMap.get(cn.id);
          if (!ex || cn.modified > ex.modified) nbMap.set(cn.id, cn);
        });

        const fcMap = new Map<string, Flashcard>();
        prev.flashcards.forEach((c) => fcMap.set(c.id, c));
        cloud.flashcards.forEach((cc) => {
          if (!fcMap.has(cc.id)) fcMap.set(cc.id, cc);
        });

        const mergedSettings = cloud.settingsPatch
          ? {
              ...prev.settings,
              ...cloud.settingsPatch,
              // Preserve local custom API key if user only stored it locally
              customAiApiKey: prev.settings.customAiApiKey || cloud.settingsPatch.customAiApiKey || '',
            }
          : prev.settings;

        return {
          ...prev,
          folders: Array.from(folderMap.values()),
          notebooks: Array.from(nbMap.values()).sort((a, b) => b.modified - a.modified),
          flashcards: Array.from(fcMap.values()),
          settings: mergedSettings,
        };
      });
    },
    []
  );

  return {
    state,
    modelStates,
    updateSettings,
    savePenPreset,
    deletePenPreset,
    createFolder,
    updateFolder,
    deleteFolder,
    createNotebook,
    updateNotebook,
    updateNotebookSlides,
    openNotebookTab,
    closeTab,
    softDeleteNotebook,
    restoreNotebook,
    deleteForeverNotebook,
    emptyTrash,
    duplicateNotebook,
    addFlashcards,
    updateFlashcard,
    deleteFlashcard,
    recordQuizScore,
    addStudyMinutes,
    startModelDownload,
    pauseModelDownload,
    removeLocalModel,
    exportBackupJson,
    importBackupJson,
    importParsedNotebook,
    appendPagesToNotebook,
    mergeCloudLibrary,
  };
}
