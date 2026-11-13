export type PaperStyle =
  | 'BLANK'
  | 'RULED'
  | 'NARROW'
  | 'COLLEGE'
  | 'WIDE'
  | 'LEGAL'
  | 'HANDWRITING'
  | 'GRID'
  | 'SMALL_GRID'
  | 'GRAPH'
  | 'DOTTED'
  | 'ISOMETRIC'
  | 'HEX'
  | 'CORNELL'
  | 'TWO_COLUMN'
  | 'CHECKLIST'
  | 'TODO'
  | 'DAILY'
  | 'PLANNER'
  | 'MONTHLY'
  | 'MEETING'
  | 'STUDY'
  | 'STORYBOARD'
  | 'MUSIC'
  | 'TABLATURE';

export interface PaperStyleMeta {
  id: PaperStyle;
  label: string;
  group: 'Writing' | 'Grids' | 'Planning' | 'Study' | 'Music';
}

export const PAPER_STYLES: PaperStyleMeta[] = [
  { id: 'RULED', label: 'Ruled', group: 'Writing' },
  { id: 'COLLEGE', label: 'College ruled', group: 'Writing' },
  { id: 'NARROW', label: 'Narrow ruled', group: 'Writing' },
  { id: 'WIDE', label: 'Wide ruled', group: 'Writing' },
  { id: 'LEGAL', label: 'Legal pad', group: 'Writing' },
  { id: 'TWO_COLUMN', label: 'Two columns', group: 'Writing' },
  { id: 'BLANK', label: 'Blank', group: 'Writing' },
  { id: 'DOTTED', label: 'Dotted', group: 'Grids' },
  { id: 'GRID', label: 'Square grid', group: 'Grids' },
  { id: 'SMALL_GRID', label: 'Small grid', group: 'Grids' },
  { id: 'GRAPH', label: 'Graph', group: 'Grids' },
  { id: 'ISOMETRIC', label: 'Isometric', group: 'Grids' },
  { id: 'HEX', label: 'Hexagonal', group: 'Grids' },
  { id: 'CORNELL', label: 'Cornell notes', group: 'Study' },
  { id: 'STUDY', label: 'Study sheet', group: 'Study' },
  { id: 'CHECKLIST', label: 'Checklist', group: 'Planning' },
  { id: 'TODO', label: 'To do', group: 'Planning' },
  { id: 'DAILY', label: 'Daily plan', group: 'Planning' },
  { id: 'PLANNER', label: 'Weekly plan', group: 'Planning' },
  { id: 'MONTHLY', label: 'Monthly', group: 'Planning' },
  { id: 'MEETING', label: 'Meeting', group: 'Planning' },
  { id: 'STORYBOARD', label: 'Storyboard', group: 'Planning' },
  { id: 'MUSIC', label: 'Music staff', group: 'Music' },
  { id: 'TABLATURE', label: 'Guitar tabs', group: 'Music' },
];

export type PagePresetId =
  | 'A4'
  | 'LETTER'
  | 'A5'
  | 'B5'
  | 'LEGAL'
  | 'WIDE_16_9'
  | 'SQUARE'
  | 'WHITEBOARD'
  | 'CUSTOM';

export interface PagePresetMeta {
  id: PagePresetId;
  label: string;
  subtitle: string;
  wMm: number;
  hMm: number;
  recommended?: boolean;
  isWhiteboard?: boolean;
}

export const UNITS_PER_MM = 5.714;

export const PAGE_PRESETS: PagePresetMeta[] = [
  {
    id: 'A4',
    label: 'A4 Standard',
    subtitle: '210 × 297 mm · Best all-round for study & PDF export',
    wMm: 210,
    hMm: 297,
    recommended: true,
  },
  {
    id: 'LETTER',
    label: 'US Letter',
    subtitle: '216 × 279 mm · Standard US binder size',
    wMm: 216,
    hMm: 279,
  },
  {
    id: 'B5',
    label: 'B5 Notebook',
    subtitle: '176 × 250 mm · Classic Japanese composition feel',
    wMm: 176,
    hMm: 250,
  },
  {
    id: 'A5',
    label: 'A5 Compact',
    subtitle: '148 × 210 mm · Journal & pocket notes',
    wMm: 148,
    hMm: 210,
  },
  {
    id: 'WIDE_16_9',
    label: 'Presentation 16:9',
    subtitle: '297 × 167 mm · Widescreen slides & lectures',
    wMm: 297,
    hMm: 167,
  },
  {
    id: 'SQUARE',
    label: 'Square 1:1',
    subtitle: '220 × 220 mm · Diagrams & visual sketches',
    wMm: 220,
    hMm: 220,
  },
  {
    id: 'WHITEBOARD',
    label: '∞ Infinite Whiteboard',
    subtitle: 'Unbounded pan & zoom canvas · Great for mind maps',
    wMm: 420,
    hMm: 280,
    isWhiteboard: true,
  },
  {
    id: 'CUSTOM',
    label: 'Custom size',
    subtitle: 'Specify exact width & height in mm',
    wMm: 210,
    hMm: 297,
  },
];

export type ThemeMode = 'SYSTEM' | 'LIGHT' | 'DARK';
export type AccentColorId = 'BLUE' | 'VIOLET' | 'EMERALD' | 'AMBER' | 'ROSE' | 'SLATE';
export type ScrollDirection = 'VERTICAL' | 'HORIZONTAL';
export type ToolbarDock = 'HEADER' | 'TOP' | 'BOTTOM';

export const ACCENT_COLORS: Record<
  AccentColorId,
  { label: string; primary: string; secondary: string; softBg: string; softDarkBg: string }
> = {
  BLUE: {
    label: 'Sapphire',
    primary: '#3D6CF4',
    secondary: '#6B8EFF',
    softBg: 'rgba(247, 249, 255, 0.94)',
    softDarkBg: 'rgba(20, 22, 29, 0.94)',
  },
  VIOLET: {
    label: 'Violet',
    primary: '#7C3AED',
    secondary: '#A855F7',
    softBg: 'rgba(250, 247, 255, 0.94)',
    softDarkBg: 'rgba(22, 20, 30, 0.94)',
  },
  EMERALD: {
    label: 'Emerald',
    primary: '#0F7E5C',
    secondary: '#2BB673',
    softBg: 'rgba(244, 252, 248, 0.94)',
    softDarkBg: 'rgba(18, 26, 23, 0.94)',
  },
  AMBER: {
    label: 'Amber',
    primary: '#D97706',
    secondary: '#F59E0B',
    softBg: 'rgba(255, 250, 242, 0.94)',
    softDarkBg: 'rgba(28, 24, 18, 0.94)',
  },
  ROSE: {
    label: 'Rose',
    primary: '#E11D48',
    secondary: '#FB7185',
    softBg: 'rgba(255, 246, 248, 0.94)',
    softDarkBg: 'rgba(28, 19, 23, 0.94)',
  },
  SLATE: {
    label: 'Graphite',
    primary: '#334155',
    secondary: '#64748B',
    softBg: 'rgba(246, 247, 249, 0.94)',
    softDarkBg: 'rgba(21, 22, 26, 0.94)',
  },
};

export type ToolId =
  | 'PEN'
  | 'HIGHLIGHTER'
  | 'ERASER'
  | 'SHAPE'
  | 'LASSO'
  | 'MINDMAP'
  | 'TEXT'
  | 'STICKY'
  | 'WEBLINK'
  | 'TAPE'
  | 'STICKER'
  | 'IMAGE'
  | 'LASER'
  | 'PAN';

export const DEFAULT_TOOLBAR_ORDER: ToolId[] = [
  'PEN',
  'HIGHLIGHTER',
  'ERASER',
  'SHAPE',
  'LASSO',
  'MINDMAP',
  'TEXT',
  'STICKY',
  'WEBLINK',
  'TAPE',
  'STICKER',
  'IMAGE',
  'LASER',
  'PAN',
];

export type AppFont = 'MODERN_SANS' | 'EDITORIAL_SERIF' | 'STUDIO_MONO' | 'ROUNDED';
export type MotionStyle = 'ULTRA_SPRING' | 'MINIMAL_CRISP' | 'REDUCED';
export type CustomAiProvider = 'OPENAI_COMPATIBLE' | 'GEMINI' | 'OLLAMA';

export type PenKind = 'ballpoint' | 'fountain' | 'brush' | 'fineliner' | 'pencil';

export type EraserMode = 'STROKE' | 'PIXEL' | 'HIGHLIGHTER';

export type ScribbleSensitivity = 'gentle' | 'balanced' | 'instant';

export type ShapeKind =
  | 'AUTO'
  | 'LINE'
  | 'ARROW'
  | 'DOUBLE_ARROW'
  | 'RECT'
  | 'ROUNDED'
  | 'SQUIRCLE'
  | 'PILL'
  | 'ELLIPSE'
  | 'TRIANGLE'
  | 'SOFT_TRIANGLE'
  | 'RIGHT_TRIANGLE'
  | 'DIAMOND'
  | 'PENTAGON'
  | 'HEXAGON'
  | 'OCTAGON'
  | 'STAR'
  | 'SOFT_STAR'
  | 'SPARKLE'
  | 'SUNNY'
  | 'BURST'
  | 'FLOWER'
  | 'CLOVER'
  | 'HEART'
  | 'CLOUD'
  | 'SPEECH'
  | 'GEM'
  | 'BADGE'
  | 'COOKIE'
  | 'PILL_STAR';

export type TextFont = 'SANS' | 'SERIF' | 'MONO' | 'HAND' | 'CURSIVE';

export interface StrokePt {
  x: number;
  y: number;
  p?: number;
}

export interface InkItem {
  id: string;
  kind: 'ink';
  points: StrokePt[];
  color: string;
  size: number;
  opacity?: number;
  isHighlighter: boolean;
  dashed?: boolean;
  penKind?: PenKind;
}

export interface FillItem {
  id: string;
  kind: 'fill';
  points: StrokePt[];
  color: string;
}

export interface TextItem {
  id: string;
  kind: 'text';
  x: number;
  y: number;
  text: string;
  size: number;
  color: string;
  font: TextFont;
  bold?: boolean;
  italic?: boolean;
  underline?: boolean;
  gradient?: string;
  rot?: number;
  createdAt?: number;
}

export type StickyFont = 'SANS' | 'SERIF' | 'MONO' | 'HAND';
export type StickyStyle = 'classic' | 'lined' | 'glass' | 'outline';

export interface StickyItem {
  id: string;
  kind: 'sticky';
  x: number;
  y: number;
  w: number;
  h: number;
  color: string;
  title?: string;
  text: string;
  style?: StickyStyle;
  rot?: number;
  fontSize?: number;
  font?: StickyFont;
  collapsed?: boolean;
  pinned?: boolean;
}

export interface WebLinkItem {
  id: string;
  kind: 'weblink';
  url: string;
  title: string;
  note?: string;
  x: number;
  y: number;
  w: number;
  h: number;
  color: string;
  rot?: number;
}

export const WEBLINK_COLORS = ['#3D6CF4', '#0F7E5C', '#7C3AED', '#EA580C', '#0EA5E9', '#E11D48'];

export const TAPE_PATTERNS: { id: string; label: string }[] = [
  { id: 'stripes', label: 'Stripes' },
  { id: 'dots', label: 'Polka dots' },
  { id: 'grid', label: 'Grid' },
  { id: 'checks', label: 'Checks' },
  { id: 'zigzag', label: 'Zig-zag' },
  { id: 'waves', label: 'Waves' },
  { id: 'stars', label: 'Stars' },
  { id: 'hearts', label: 'Hearts' },
  { id: 'flowers', label: 'Flowers' },
];

export interface TapeItem {
  id: string;
  kind: 'tape';
  x: number;
  y: number;
  w: number;
  h: number;
  color: string;
  pattern: string;
  rot?: number;
  revealed?: boolean;
}

export interface ImageItem {
  id: string;
  kind: 'image';
  x: number;
  y: number;
  w: number;
  h: number;
  src: string;
  rot?: number;
}

export type MindNodeShape = 'pill' | 'card' | 'diamond';

export interface MindNodeItem {
  id: string;
  kind: 'mindnode';
  x: number;
  y: number;
  w: number;
  h: number;
  label: string;
  tag?: string;
  notes?: string;
  parentId: string | null;
  color: string;
  shape: MindNodeShape;
  collapsed?: boolean;
}

export type PageItem =
  | InkItem
  | FillItem
  | TextItem
  | StickyItem
  | WebLinkItem
  | TapeItem
  | ImageItem
  | MindNodeItem;

export interface Page {
  id: string;
  width: number;
  height: number;
  paper: PaperStyle;
  paperColor?: string | null;
  lineScale?: number;
  lineColor?: string | null;
  cover?: string | null;
  bookmarked?: boolean;
  isWhiteboard?: boolean;
  wbPanX?: number;
  wbPanY?: number;
  wbZoom?: number;
  items: PageItem[];
}

export interface Flashcard {
  id: string;
  notebookId: string;
  front: string;
  back: string;
  pageId?: string;
  box: number;
  due: number;
  created: number;
  lapses?: number;
  lastGrade?: 0 | 1 | 2 | 3;
}

export interface QuizQuestion {
  id: string;
  question: string;
  options: string[];
  answer: number;
  explanation: string;
  pageId?: string;
  generated?: boolean;
  right?: number;
  wrong?: number;
}

export type SlideKind = 'TITLE' | 'CONCEPT' | 'FORMULA' | 'CHECK_QA' | 'SUMMARY';
export type SlideThemeId =
  | 'AURORA_INDIGO'
  | 'SUNSET_CORAL'
  | 'EMERALD_MINT'
  | 'COSMIC_GOLD'
  | 'MIDNIGHT_PRISM'
  | 'ROSE_VELVET';

export const SLIDE_THEMES: Record<
  SlideThemeId,
  { label: string; gradA: string; gradB: string; accent: string }
> = {
  AURORA_INDIGO: { label: 'Aurora Indigo', gradA: '#1E1B4B', gradB: '#4338CA', accent: '#818CF8' },
  SUNSET_CORAL: { label: 'Sunset Coral', gradA: '#7C2D12', gradB: '#EA580C', accent: '#FDBA74' },
  EMERALD_MINT: { label: 'Emerald Mint', gradA: '#064E3B', gradB: '#059669', accent: '#6EE7B7' },
  COSMIC_GOLD: { label: 'Cosmic Gold', gradA: '#78350F', gradB: '#D97706', accent: '#FDE68A' },
  MIDNIGHT_PRISM: { label: 'Midnight Prism', gradA: '#0F172A', gradB: '#1E40AF', accent: '#38BDF8' },
  ROSE_VELVET: { label: 'Rose Velvet', gradA: '#881337', gradB: '#E11D48', accent: '#FDA4AF' },
};

export interface SlideCard {
  id: string;
  kind: SlideKind;
  theme: SlideThemeId;
  title: string;
  subtitle?: string;
  bullets: string[];
  callout?: string;
  question?: string;
  answer?: string;
  badge?: string;
}

export interface NotebookMeta {
  id: string;
  title: string;
  folderId: string | null;
  cover: string;
  coverColor: string;
  paper: PaperStyle;
  created: number;
  modified: number;
  opened: number;
  pageCount: number;
  favorite?: boolean;
  deletedAt: number | null;
  isWhiteboard?: boolean;
  isMindMap?: boolean;
  slides?: SlideCard[];
  pages: Page[];
}

export interface Folder {
  id: string;
  name: string;
  parentId: string | null;
  color: string;
  emoji: string;
  created: number;
  modified: number;
}

export interface LocalModelSpec {
  id: string;
  name: string;
  family: string;
  sizeMb: number;
  quantization: string;
  ramRequired: string;
  speedTag: string;
  category: 'fast' | 'balanced' | 'reasoning';
  speedScore: number;
  description: string;
  capabilities: string[];
  recommended?: boolean;
}

export type ModelStatus = 'NOT_DOWNLOADED' | 'DOWNLOADING' | 'PAUSED' | 'READY' | 'ERROR';

export interface LocalModelState {
  status: ModelStatus;
  progress: number;
  downloadedBytes: number;
  totalBytes: number;
  speedMbps?: number;
  downloadedAt?: number;
  shaTag?: string;
  error?: string;
}

export interface PenPreset {
  id: string;
  name: string;
  kind: PenKind;
  color: string;
  size: number;
  opacity: number;
  stabilizer: number;
}

export interface AppSettings {
  themeMode: ThemeMode;
  accent: AccentColorId;
  appFont: AppFont;
  motionStyle: MotionStyle;
  paperTint: string;
  defaultPaper: PaperStyle;
  defaultPreset: PagePresetId;
  defaultLandscape: boolean;
  defaultCoverPage: boolean;
  scroll: ScrollDirection;
  toolbarDock: ToolbarDock;
  toolbarOrder: ToolId[];
  liquidGlass: boolean;
  showTabs: boolean;
  pageNumbers: boolean;
  showHoverTooltips: boolean;
  setupCompleted: boolean;
  scribbleToErase: boolean;
  liveScribbleErase: boolean;
  scribbleSensitivity: ScribbleSensitivity;
  holdToShape: boolean;
  circleToLasso: boolean;
  highlighterBehind: boolean;
  highlighterMode: 'AUTO' | 'STRAIGHT' | 'FREE';
  highlighterOpacity: number;
  eraserMode: EraserMode;
  eraseHighlighterOnly: boolean;
  lassoMode: 'FREEFORM' | 'RECT';
  lassoTouching: boolean;
  studyTimer: boolean;
  studyAutoStart: boolean;
  studySeconds: boolean;
  activeLocalModelId: string | null;
  aiAnimations: boolean;
  customAiEnabled: boolean;
  customAiProvider: CustomAiProvider;
  customAiBaseUrl: string;
  customAiApiKey: string;
  customAiModel: string;
  whiteboardGridSnap: boolean;
  penName: string;
  penKind: PenKind;
  penColor: string;
  penSize: number;
  penOpacity: number;
  penStabilizer: number;
  penPresets: PenPreset[];
}

export const DEFAULT_PEN_PRESETS: PenPreset[] = [
  {
    id: 'preset-fountain',
    name: 'Classic Fountain',
    kind: 'fountain',
    color: '#1B1F2A',
    size: 3.5,
    opacity: 1,
    stabilizer: 0.35,
  },
  {
    id: 'preset-fineliner',
    name: 'Study Fineliner',
    kind: 'fineliner',
    color: '#2A78D6',
    size: 2.2,
    opacity: 1,
    stabilizer: 0.45,
  },
  {
    id: 'preset-brush',
    name: 'Calligraphy Brush',
    kind: 'brush',
    color: '#7C3AED',
    size: 6.5,
    opacity: 0.95,
    stabilizer: 0.5,
  },
  {
    id: 'preset-pencil',
    name: 'Graphite Sketch',
    kind: 'pencil',
    color: '#4B5563',
    size: 3.2,
    opacity: 0.85,
    stabilizer: 0.2,
  },
];

export const FOLDER_COLORS = [
  '#3D6CF4',
  '#0F7E5C',
  '#7C3AED',
  '#D97706',
  '#E11D48',
  '#0EA5E9',
  '#475569',
];

export const COVER_COLORS = [
  '#3D6CF4',
  '#0F7E5C',
  '#6250D6',
  '#D95926',
  '#C04873',
  '#1E293B',
  '#0E2A47',
  '#6D4C41',
];

export const COVER_PATTERNS = [
  'solid',
  'leather',
  'gradient',
  'geometric',
  'dots',
  'grid',
  'waves',
  'kraft',
  'marble',
] as const;

export const PAPER_TINTS = [
  { id: 'white', label: 'White', color: '#FFFFFF' },
  { id: 'cream', label: 'Cream', color: '#FBF7EC' },
  { id: 'warm', label: 'Warm', color: '#FFF4E0' },
  { id: 'mint', label: 'Sage', color: '#EFF8EC' },
  { id: 'slate', label: 'Dark Slate', color: '#22252B' },
];
