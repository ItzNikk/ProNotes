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
  | 'TODO'
  | 'CHECKLIST'
  | 'PLANNER'
  | 'DAILY'
  | 'MONTHLY'
  | 'MEETING'
  | 'CORNELL'
  | 'STUDY'
  | 'TWO_COLUMN'
  | 'STORYBOARD'
  | 'MUSIC'
  | 'TABLATURE';

export interface PaperStyleMeta {
  id: PaperStyle;
  label: string;
  group: 'Writing' | 'Grids' | 'Planning' | 'Study' | 'Music';
}

export const PAPER_STYLES: PaperStyleMeta[] = [
  { id: 'BLANK', label: 'Blank', group: 'Writing' },
  { id: 'RULED', label: 'Ruled', group: 'Writing' },
  { id: 'NARROW', label: 'Narrow ruled', group: 'Writing' },
  { id: 'COLLEGE', label: 'College ruled', group: 'Writing' },
  { id: 'WIDE', label: 'Wide ruled', group: 'Writing' },
  { id: 'LEGAL', label: 'Legal pad', group: 'Writing' },
  { id: 'HANDWRITING', label: 'Handwriting', group: 'Writing' },
  { id: 'GRID', label: 'Grid', group: 'Grids' },
  { id: 'SMALL_GRID', label: 'Small grid', group: 'Grids' },
  { id: 'GRAPH', label: 'Graph paper', group: 'Grids' },
  { id: 'DOTTED', label: 'Dotted', group: 'Grids' },
  { id: 'ISOMETRIC', label: 'Isometric', group: 'Grids' },
  { id: 'HEX', label: 'Hexagons', group: 'Grids' },
  { id: 'TODO', label: 'To-do', group: 'Planning' },
  { id: 'CHECKLIST', label: 'Checklist', group: 'Planning' },
  { id: 'PLANNER', label: 'Weekly', group: 'Planning' },
  { id: 'DAILY', label: 'Daily planner', group: 'Planning' },
  { id: 'MONTHLY', label: 'Monthly', group: 'Planning' },
  { id: 'MEETING', label: 'Meeting notes', group: 'Planning' },
  { id: 'CORNELL', label: 'Cornell', group: 'Study' },
  { id: 'STUDY', label: 'Study notes', group: 'Study' },
  { id: 'TWO_COLUMN', label: 'Two columns', group: 'Study' },
  { id: 'STORYBOARD', label: 'Storyboard', group: 'Study' },
  { id: 'MUSIC', label: 'Music staff', group: 'Music' },
  { id: 'TABLATURE', label: 'Guitar tab', group: 'Music' },
];

export type PagePresetId =
  | 'A4'
  | 'LETTER'
  | 'A5'
  | 'A3'
  | 'SQUARE'
  | 'WIDE'
  | 'PHONE'
  | 'WHITEBOARD'
  | 'CUSTOM';

export interface PagePresetMeta {
  id: PagePresetId;
  label: string;
  wMm: number;
  hMm: number;
  recommended?: boolean;
  subtitle: string;
  isWhiteboard?: boolean;
}

export const UNITS_PER_MM = 1240 / 210;

export const PAGE_PRESETS: PagePresetMeta[] = [
  {
    id: 'A4',
    label: 'A4',
    wMm: 210,
    hMm: 297,
    recommended: true,
    subtitle: '210 × 297 mm · Recommended for notes & PDF export',
  },
  { id: 'LETTER', label: 'Letter', wMm: 215.9, hMm: 279.4, subtitle: '216 × 279 mm · US standard' },
  { id: 'A5', label: 'A5', wMm: 148, hMm: 210, subtitle: '148 × 210 mm · Compact journal' },
  { id: 'A3', label: 'A3', wMm: 297, hMm: 420, subtitle: '297 × 420 mm · Large diagrams' },
  { id: 'SQUARE', label: 'Square', wMm: 210, hMm: 210, subtitle: '210 × 210 mm · Sketchpad' },
  { id: 'WIDE', label: '16:9', wMm: 297, hMm: 167, subtitle: '297 × 167 mm · Slides & lectures' },
  { id: 'PHONE', label: 'Phone', wMm: 75, hMm: 160, subtitle: '75 × 160 mm · Quick pocket note' },
  {
    id: 'WHITEBOARD',
    label: 'Infinite Board',
    wMm: 360,
    hMm: 240,
    isWhiteboard: true,
    subtitle: 'Unbounded pan & zoom canvas',
  },
  { id: 'CUSTOM', label: 'Custom', wMm: 210, hMm: 297, subtitle: 'Set custom width & height' },
];

export type AccentColorId = 'BLUE' | 'VIOLET' | 'GREEN' | 'ORANGE' | 'PINK' | 'GRAPHITE';

export const ACCENT_COLORS: Record<
  AccentColorId,
  { label: string; primary: string; secondary: string; softBg: string; softDarkBg: string }
> = {
  BLUE: { label: 'Blue', primary: '#2A78D6', secondary: '#7161E0', softBg: '#E4EEFA', softDarkBg: '#192231' },
  VIOLET: { label: 'Violet', primary: '#6250D6', secondary: '#D55181', softBg: '#EBE8FA', softDarkBg: '#211D33' },
  GREEN: { label: 'Green', primary: '#0F7E5C', secondary: '#3987E5', softBg: '#E1F3EC', softDarkBg: '#152822' },
  ORANGE: { label: 'Orange', primary: '#D95926', secondary: '#E34948', softBg: '#FCEBE4', softDarkBg: '#2F1E18' },
  PINK: { label: 'Pink', primary: '#C04873', secondary: '#8173E3', softBg: '#F8E6EE', softDarkBg: '#2B1B24' },
  GRAPHITE: { label: 'Graphite', primary: '#4B4E56', secondary: '#7B7974', softBg: '#EAEBEE', softDarkBg: '#202226' },
};

export const COVER_COLORS = [
  '#3D6CF4', '#2BB673', '#F2994A', '#EB5757', '#9B51E0', '#1B1F2A',
  '#56CCF2', '#F2C94C', '#7BC8A4', '#F4A7C0', '#0E4C92', '#8D6E63',
];

export const FOLDER_COLORS = [
  '#5AC8FA', '#3D6CF4', '#34C759', '#FF9500', '#FF3B30',
  '#AF52DE', '#FF2D55', '#FFCC00', '#8E8E93', '#1B1F2A',
];

export const COVER_PATTERNS = [
  'leather', 'kraft', 'linen', 'marble', 'terrazzo', 'plaid',
  'geometric', 'gradient', 'spiral', 'dots', 'waves', 'grid', 'stripes', 'scales',
] as const;

export const PAPER_TINTS = [
  { id: 'WHITE', label: 'White', color: '#FFFFFF' },
  { id: 'CREAM', label: 'Cream', color: '#FBF7EC' },
  { id: 'GRAY', label: 'Gray', color: '#F1F2F4' },
  { id: 'MINT', label: 'Soft Mint', color: '#EFF8EC' },
  { id: 'SKY', label: 'Sky Tint', color: '#EAF4FF' },
  { id: 'DARK', label: 'Dark', color: '#22252B' },
];

export const TAPE_PATTERNS = [
  { id: 'stripes', label: 'Stripes' },
  { id: 'dots', label: 'Polka dots' },
  { id: 'grid', label: 'Grid' },
  { id: 'checks', label: 'Gingham' },
  { id: 'zigzag', label: 'Zig-zag' },
  { id: 'stars', label: 'Stars' },
  { id: 'hearts', label: 'Hearts' },
  { id: 'waves', label: 'Waves' },
  { id: 'flowers', label: 'Florals' },
  { id: 'plain', label: 'Solid matte' },
] as const;

export type ToolId =
  | 'PEN'
  | 'HIGHLIGHTER'
  | 'ERASER'
  | 'SHAPE'
  | 'LASSO'
  | 'MINDMAP'
  | 'TEXT'
  | 'STICKY'
  | 'TAPE'
  | 'STICKER'
  | 'IMAGE'
  | 'LASER'
  | 'PAN';

export type PenKind = 'ballpoint' | 'fountain' | 'brush';

export type ShapeKind =
  | 'AUTO' | 'LINE' | 'ARROW' | 'DOUBLE_ARROW' | 'RECT' | 'ROUNDED' | 'ELLIPSE'
  | 'TRIANGLE' | 'RIGHT_TRIANGLE' | 'DIAMOND' | 'PENTAGON' | 'HEXAGON' | 'STAR'
  | 'HEART' | 'CLOUD' | 'SPEECH' | 'OCTAGON' | 'SOFT_TRIANGLE' | 'SQUIRCLE'
  | 'PILL' | 'SOFT_STAR' | 'SUNNY' | 'BURST' | 'COOKIE' | 'CLOVER' | 'FLOWER'
  | 'GEM' | 'PILL_STAR' | 'BADGE' | 'SPARKLE';

export interface StrokePt {
  x: number;
  y: number;
  p?: number; // 0..1 pressure
}

export type TextFont = 'SANS' | 'SERIF' | 'MONO' | 'ROUNDED' | 'HAND' | 'CURSIVE';

export interface InkItem {
  id: string;
  kind: 'ink';
  points: StrokePt[];
  color: string;
  size: number;
  isHighlighter: boolean;
  opacity?: number;
  penKind?: PenKind;
  isMathAnswer?: boolean;
  mathSourceRect?: { x: number; y: number; w: number; h: number };
}

export interface FillItem {
  id: string;
  kind: 'fill';
  points: { x: number; y: number }[];
  color: string;
}

export interface TextItem {
  id: string;
  kind: 'text';
  text: string;
  x: number;
  y: number;
  size: number;
  color: string;
  font: TextFont;
  bold?: boolean;
  italic?: boolean;
  underline?: boolean;
  gradient?: string | null;
  align?: 'left' | 'center' | 'right';
  rot?: number;
  isMathAnswer?: boolean;
  mathExpr?: string;
  createdAt?: number;
}

export interface StickyItem {
  id: string;
  kind: 'sticky';
  x: number;
  y: number;
  w: number;
  h: number;
  color: string;
  text: string;
  rot?: number;
}

export interface TapeItem {
  id: string;
  kind: 'tape';
  x: number;
  y: number;
  w: number;
  h: number;
  color: string;
  pattern: string;
  revealed?: boolean;
  rot?: number;
}

export interface ImageItem {
  id: string;
  kind: 'image';
  src: string;
  x: number;
  y: number;
  w: number;
  h: number;
  rot?: number;
  label?: string;
}

export interface MindNodeItem {
  id: string;
  kind: 'mindnode';
  x: number; // top-left x
  y: number; // top-left y
  w: number;
  h: number;
  label: string;
  parentId: string | null;
  color: string;
  shape: 'pill' | 'card' | 'diamond';
  collapsed?: boolean;
  notes?: string;
}

export type PageItem =
  | InkItem
  | FillItem
  | TextItem
  | StickyItem
  | TapeItem
  | ImageItem
  | MindNodeItem;

export interface Page {
  id: string;
  width: number;
  height: number;
  paper: PaperStyle;
  isWhiteboard?: boolean;
  items: PageItem[];
  bookmarked?: boolean;
  paperColor?: string | null;
  cover?: string | null;
  bgImage?: string | null;
  lineScale?: number;
  lineColor?: string | null;
  /** Per-page infinite whiteboard camera state */
  wbPanX?: number;
  wbPanY?: number;
  wbZoom?: number;
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

export interface NotebookMeta {
  id: string;
  title: string;
  folderId: string | null;
  coverColor: string;
  paper: PaperStyle;
  isWhiteboard?: boolean;
  isMindMap?: boolean;
  created: number;
  modified: number;
  opened: number;
  pageCount: number;
  favorite: boolean;
  deletedAt: number | null;
  cover: string;
  pages: Page[];
}

export interface Flashcard {
  id: string;
  notebookId: string;
  front: string;
  back: string;
  box: number;
  due: number;
  created: number;
}

export interface QuizQuestion {
  id: string;
  question: string;
  options: string[];
  answer: number;
  explanation: string;
}

export type ThemeMode = 'SYSTEM' | 'LIGHT' | 'DARK';
export type ToolbarDock = 'HEADER' | 'TOP' | 'BOTTOM' | 'LEFT' | 'RIGHT';

export interface AppSettings {
  themeMode: ThemeMode;
  accent: AccentColorId;
  paperTint: string;
  defaultPaper: PaperStyle;
  defaultPreset: PagePresetId;
  defaultLandscape: boolean;
  defaultCoverPage: boolean;
  scroll: 'VERTICAL' | 'HORIZONTAL';
  toolbarDock: ToolbarDock;
  liquidGlass: boolean;
  showTabs: boolean;
  pageNumbers: boolean;
  pet: boolean;
  scribbleToErase: boolean;
  liveScribbleErase: boolean;
  holdToShape: boolean;
  circleToLasso: boolean;
  mathAssist: boolean;
  highlighterBehind: boolean;
  highlighterMode: 'FREEHAND' | 'AUTO' | 'STRAIGHT';
  highlighterOpacity: number;
  eraserMode: 'STROKE' | 'PIXEL';
  lassoMode: 'FREEFORM' | 'RECT';
  lassoTouching: boolean;
  studyTimer: boolean;
  studyAutoStart: boolean;
  studySeconds: boolean;
  activeLocalModelId: string | null;
  aiAnimations: boolean;
  whiteboardGridSnap: boolean;
}

export interface LocalModelSpec {
  id: string;
  name: string;
  family: string;
  sizeMb: number;
  quantization: string;
  ramRequired: string;
  speedTag: string;
  description: string;
  capabilities: string[];
  recommended?: boolean;
}

export interface LocalModelState {
  status: 'NOT_DOWNLOADED' | 'DOWNLOADING' | 'PAUSED' | 'READY' | 'ERROR';
  progress: number; // 0..1
  downloadedBytes: number;
  totalBytes: number;
  speedMbps?: number;
  downloadedAt?: number;
  shaTag?: string;
}
