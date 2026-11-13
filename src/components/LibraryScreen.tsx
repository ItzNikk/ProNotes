import React, { useMemo, useState } from 'react';
import {
  BookOpen,
  Brain,
  ChevronRight,
  Clock,
  Cloud,
  Copy,
  Cpu,
  FileText,
  Folder as FolderIcon,
  FolderPlus,
  Grid,
  Heart,
  Layers,
  List,
  LogIn,
  MoreVertical,
  PanelLeftClose,
  PanelLeftOpen,
  Plus,
  RefreshCw,
  RotateCcw,
  Search,
  Settings,
  Star,
  Trash2,
  Upload,
  Users,
  Wand2,
  X,
} from 'lucide-react';
import {
  ACCENT_COLORS,
  AppSettings,
  FOLDER_COLORS,
  Folder,
  LocalModelState,
  NotebookMeta,
  Page,
} from '../types/models';
import { CoverCanvas, FolderArt, NewNotebookModal, NewNotebookPayload } from './CoversAndPatterns';
import { IMAGE_MODELS_CATALOG, LOCAL_MODELS_CATALOG } from '../utils/localAi';
import { parseImportedFiles, parseImportedText } from '../utils/noteImporter';
import { SupportBanner } from './SupportBannerModal';

type Section = 'DOCUMENTS' | 'FAVORITES' | 'RECENT' | 'TRASH';

interface LibraryScreenProps {
  folders: Folder[];
  notebooks: NotebookMeta[];
  settings: AppSettings;
  modelStates: Record<string, LocalModelState>;
  userEmail?: string | null;
  userName?: string | null;
  syncBusy?: boolean;
  lastSyncAt?: number | null;
  supportBannerOpen?: boolean;
  onSignInGoogle?: () => void;
  onSyncNow?: () => void;
  onOpenCollab?: () => void;
  onOpenSupport?: () => void;
  onDismissSupportBanner?: () => void;
  onOpenNote: (id: string) => void;
  onCreateNotebook: (payload: NewNotebookPayload & { folderId: string | null }) => void;
  onImportParsedNotebook: (title: string, pages: Page[], folderId: string | null) => void;
  onImportBackup: (raw: string) => number;
  onCreateFolder: (name: string, parentId: string | null, color: string, emoji: string) => void;
  onUpdateFolder: (id: string, patch: Partial<Folder>) => void;
  onDeleteFolder: (id: string) => void;
  onUpdateNotebook: (id: string, updater: (nb: NotebookMeta) => NotebookMeta) => void;
  onDuplicateNotebook: (id: string) => void;
  onSoftDeleteNotebook: (id: string) => void;
  onRestoreNotebook: (id: string) => void;
  onDeleteForeverNotebook: (id: string) => void;
  onEmptyTrash: () => void;
  onOpenSettings: () => void;
  onOpenLocalModels: () => void;
  onOpenSetupWizard?: () => void;
}

const formatDate = (ts: number) =>
  new Date(ts).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });

const NotebookGridCard = React.memo<{
  note: NotebookMeta;
  menuOpen: boolean;
  onOpen: (id: string) => void;
  onToggleMenu: (id: string) => void;
  onDragStart: (id: string) => void;
  onToggleFavorite: (id: string) => void;
  onDuplicate: (id: string) => void;
  onSoftDelete: (id: string) => void;
  onRestore: (id: string) => void;
  onDeleteForever: (id: string) => void;
}>(({
  note,
  menuOpen,
  onOpen,
  onToggleMenu,
  onDragStart,
  onToggleFavorite,
  onDuplicate,
  onSoftDelete,
  onRestore,
  onDeleteForever,
}) => (
  <div
    draggable={note.deletedAt === null}
    onDragStart={() => onDragStart(note.id)}
    className="ios-stretch-card group relative flex flex-col items-center"
  >
    <div
      onClick={() => (note.deletedAt === null ? onOpen(note.id) : onToggleMenu(note.id))}
      className="relative cursor-pointer transition-transform duration-200 group-hover:-translate-y-1"
    >
      <CoverCanvas spec={note.cover} title={note.title} width={154} height={206} />
      {note.favorite && (
        <Star className="absolute right-2.5 top-2.5 h-4 w-4 fill-amber-400 text-amber-400 drop-shadow" />
      )}
      {(note.isWhiteboard || note.isMindMap) && (
        <span className="absolute bottom-2.5 left-2.5 inline-flex items-center gap-1 rounded-md bg-black/65 px-1.5 py-0.5 text-[10px] font-semibold text-white backdrop-blur-sm">
          {note.isMindMap ? (
            <>
              <Brain className="h-2.5 w-2.5" /> Mind Map
            </>
          ) : (
            <>
              <Layers className="h-2.5 w-2.5" /> Board
            </>
          )}
        </span>
      )}
    </div>

    <div className="mt-2.5 flex w-full max-w-[154px] items-start justify-between gap-1">
      <div className="min-w-0 flex-1">
        <div className="truncate text-sm font-semibold">{note.title}</div>
        <div className="font-mono-num text-[11px] text-slate-500 dark:text-slate-400">
          {formatDate(note.modified)}
        </div>
      </div>
      <div className="relative">
        <button
          type="button"
          onClick={() => onToggleMenu(note.id)}
          className="rounded-lg p-1 text-slate-400 hover:bg-black/5 hover:text-slate-700 dark:hover:bg-white/10"
        >
          <MoreVertical className="h-4 w-4" />
        </button>
        {menuOpen && (
          <div className="absolute right-0 top-full z-40 mt-1 w-48 rounded-2xl border border-black/10 bg-white/95 p-1.5 shadow-xl backdrop-blur-md dark:border-white/10 dark:bg-[#22232A]/95">
            {note.deletedAt === null ? (
              <>
                <div className="px-2.5 pb-1 pt-0.5 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                  Notebook Actions
                </div>
                <button
                  type="button"
                  onClick={() => onOpen(note.id)}
                  className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-xs font-medium hover:bg-black/5 dark:hover:bg-white/10"
                >
                  <BookOpen className="h-3.5 w-3.5 text-blue-600" /> Open Notebook
                </button>
                <button
                  type="button"
                  onClick={() => onToggleFavorite(note.id)}
                  className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-xs font-medium hover:bg-black/5 dark:hover:bg-white/10"
                >
                  <Star className="h-3.5 w-3.5 text-amber-500" />
                  {note.favorite ? 'Remove favorite' : 'Add favorite'}
                </button>
                <button
                  type="button"
                  onClick={() => onDuplicate(note.id)}
                  className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-xs font-medium hover:bg-black/5 dark:hover:bg-white/10"
                >
                  <Copy className="h-3.5 w-3.5 text-indigo-500" /> Duplicate copy
                </button>
                <div className="my-1 h-px bg-black/6 dark:bg-white/8" />
                <button
                  type="button"
                  onClick={() => onSoftDelete(note.id)}
                  className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-xs font-medium text-red-600 hover:bg-red-500/10"
                >
                  <Trash2 className="h-3.5 w-3.5" /> Move to Trash
                </button>
              </>
            ) : (
              <>
                <div className="px-2.5 pb-1 pt-0.5 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                  Trash Actions
                </div>
                <button
                  type="button"
                  onClick={() => onRestore(note.id)}
                  className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-xs font-medium hover:bg-black/5 dark:hover:bg-white/10"
                >
                  <RotateCcw className="h-3.5 w-3.5 text-emerald-600" /> Restore
                </button>
                <button
                  type="button"
                  onClick={() => onDeleteForever(note.id)}
                  className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-xs font-medium text-red-600 hover:bg-red-500/10"
                >
                  <Trash2 className="h-3.5 w-3.5" /> Delete Forever
                </button>
              </>
            )}
          </div>
        )}
      </div>
    </div>
  </div>
));

export const LibraryScreen: React.FC<LibraryScreenProps> = ({
  folders,
  notebooks,
  settings,
  modelStates,
  userEmail,
  userName,
  syncBusy,
  lastSyncAt,
  supportBannerOpen,
  onSignInGoogle,
  onSyncNow,
  onOpenCollab,
  onOpenSupport,
  onDismissSupportBanner,
  onOpenNote,
  onCreateNotebook,
  onImportParsedNotebook,
  onImportBackup,
  onCreateFolder,
  onDeleteFolder,
  onUpdateNotebook,
  onDuplicateNotebook,
  onSoftDeleteNotebook,
  onRestoreNotebook,
  onDeleteForeverNotebook,
  onEmptyTrash,
  onOpenSettings,
  onOpenLocalModels,
  onOpenSetupWizard,
}) => {
  const [section, setSection] = useState<Section>('DOCUMENTS');
  const [folderId, setFolderId] = useState<string | null>(null);
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const [sortBy, setSortBy] = useState<'modified' | 'title' | 'created'>('modified');
  const [query, setQuery] = useState('');
  const [newModal, setNewModal] = useState<{ open: boolean; mode: 'notebook' | 'whiteboard' | 'mindmap' }>({
    open: false,
    mode: 'notebook',
  });
  const [importModalOpen, setImportModalOpen] = useState(false);
  const [importTab, setImportTab] = useState<'files' | 'paste'>('files');
  const [pasteTitle, setPasteTitle] = useState('');
  const [pasteAppPreset, setPasteAppPreset] = useState<'Notion' | 'Obsidian' | 'Apple Notes' | 'Google Keep'>('Notion');
  const [pasteBody, setPasteBody] = useState('');
  const [importBusy, setImportBusy] = useState(false);
  const [importToast, setImportToast] = useState<string | null>(null);
  const [newFolderOpen, setNewFolderOpen] = useState(false);
  const [folderName, setFolderName] = useState('New Folder');
  const [folderColor, setFolderColor] = useState(FOLDER_COLORS[1]);
  const [folderEmoji, setFolderEmoji] = useState('📚');
  const [menuId, setMenuId] = useState<string | null>(null);
  const [dragPayload, setDragPayload] = useState<string | null>(null);
  const [dropHoverId, setDropHoverId] = useState<string | null>(null);

  const accent = ACCENT_COLORS[settings.accent];
  const readyModelCount = [...LOCAL_MODELS_CATALOG, ...IMAGE_MODELS_CATALOG].filter(
    (m) => modelStates[m.id]?.status === 'READY'
  ).length;
  const activeModel = LOCAL_MODELS_CATALOG.find((m) => m.id === settings.activeLocalModelId);

  const trashCount = useMemo(() => notebooks.filter((n) => n.deletedAt !== null).length, [notebooks]);

  const breadcrumbs = useMemo(() => {
    const chain: Folder[] = [];
    let cur = folderId;
    while (cur) {
      const f = folders.find((x) => x.id === cur);
      if (!f) break;
      chain.unshift(f);
      cur = f.parentId;
    }
    return chain;
  }, [folders, folderId]);

  const visibleFolders = useMemo(() => {
    if (section !== 'DOCUMENTS') return [];
    const q = query.trim().toLowerCase();
    return folders
      .filter((f) => (q ? f.name.toLowerCase().includes(q) : f.parentId === folderId))
      .sort((a, b) => a.name.localeCompare(b.name));
  }, [folders, folderId, section, query]);

  const visibleNotebooks = useMemo(() => {
    const q = query.trim().toLowerCase();
    let list = notebooks.filter((n) => {
      if (section === 'TRASH') return n.deletedAt !== null;
      if (n.deletedAt !== null) return false;
      if (q) return n.title.toLowerCase().includes(q);
      if (section === 'FAVORITES') return n.favorite;
      if (section === 'RECENT') return true;
      return n.folderId === folderId;
    });
    if (section === 'RECENT') {
      list = [...list].sort((a, b) => b.opened - a.opened);
    } else if (sortBy === 'title') {
      list = [...list].sort((a, b) => a.title.localeCompare(b.title));
    } else if (sortBy === 'created') {
      list = [...list].sort((a, b) => b.created - a.created);
    } else {
      list = [...list].sort((a, b) => b.modified - a.modified);
    }
    return list;
  }, [notebooks, section, folderId, query, sortBy]);

  const showImportBanner = (msg: string) => {
    setImportToast(msg);
    window.setTimeout(() => setImportToast(null), 3200);
  };

  const handleImportFilesList = async (files: FileList | File[]) => {
    const arr = Array.from(files);
    if (arr.length === 0) return;
    setImportBusy(true);
    try {
      let backupCount = 0;
      const nonBackup: File[] = [];
      for (const f of arr) {
        if (f.name.toLowerCase().endsWith('.json')) {
          const text = await f.text();
          try {
            const obj = JSON.parse(text);
            if (obj && Array.isArray(obj.notebooks)) {
              backupCount += onImportBackup(text);
              continue;
            }
          } catch {
            // treat as regular JSON note
          }
        }
        nonBackup.push(f);
      }
      const parsed = nonBackup.length > 0 ? await parseImportedFiles(nonBackup, settings.paperTint || '#FFFFFF') : [];
      parsed.forEach((res) => {
        onImportParsedNotebook(res.title, res.pages, folderId);
      });
      setImportModalOpen(false);
      const total = backupCount + parsed.length;
      if (total > 0) {
        showImportBanner(`Imported ${total} item${total === 1 ? '' : 's'} into library`);
      }
    } catch (err) {
      showImportBanner(`Import failed: ${(err as Error).message}`);
    } finally {
      setImportBusy(false);
    }
  };

  const pastePreview = useMemo(
    () =>
      pasteBody.trim()
        ? parseImportedText(
            pasteTitle.trim() || `${pasteAppPreset} Import`,
            pasteBody,
            settings.paperTint || '#FFFFFF',
            pasteAppPreset
          )
        : null,
    [pasteBody, pasteTitle, pasteAppPreset, settings.paperTint]
  );

  const handleConfirmPasteImport = () => {
    if (!pastePreview) return;
    onImportParsedNotebook(pastePreview.title, pastePreview.pages, folderId);
    setPasteTitle('');
    setPasteBody('');
    setImportModalOpen(false);
    showImportBanner(`Imported "${pastePreview.title}" (${pastePreview.pages.length} page${pastePreview.pages.length === 1 ? '' : 's'})`);
  };

  const handleDropOnFolder = (targetFolderId: string | null) => {
    if (!dragPayload) return;
    const [kind, id] = dragPayload.split(':');
    if (kind === 'note') {
      onUpdateNotebook(id, (nb) => ({ ...nb, folderId: targetFolderId, deletedAt: null }));
    }
    setDragPayload(null);
    setDropHoverId(null);
  };

  return (
    <div className="flex h-screen w-screen overflow-hidden bg-[#F7F7F5] text-[#1B1F2A] dark:bg-[#0F0F0F] dark:text-[#F3F4F6]">
      {sidebarOpen && (
        <aside className="flex w-60 shrink-0 flex-col border-r border-black/8 bg-[#EFEFEF]/85 px-3.5 py-4 dark:border-white/8 dark:bg-[#171718]">
          <div className="mb-5 flex items-center justify-between px-1.5">
            <div className="flex items-center gap-2.5">
              <div
                className="flex h-9 w-9 items-center justify-center rounded-xl text-white shadow-sm"
                style={{
                  background: `linear-gradient(135deg, ${accent.primary}, ${accent.secondary})`,
                }}
              >
                <span className="font-display text-base font-extrabold">P</span>
              </div>
              <div>
                <h1 className="text-base font-bold leading-tight tracking-tight">Pro Notes</h1>
                <span className="text-[11px] font-semibold text-blue-600 dark:text-blue-400">v10.0 · Ultra</span>
              </div>
            </div>
            <button
              type="button"
              onClick={() => setSidebarOpen(false)}
              className="rounded-lg p-1.5 text-slate-500 hover:bg-black/5 dark:hover:bg-white/10"
              title="Collapse sidebar"
            >
              <PanelLeftClose className="h-4 w-4" />
            </button>
          </div>

          <nav className="space-y-1">
            {(
              [
                { id: 'DOCUMENTS', label: 'Documents', icon: FolderIcon },
                { id: 'FAVORITES', label: 'Favorites', icon: Star },
                { id: 'RECENT', label: 'Recent', icon: Clock },
                { id: 'TRASH', label: 'Trash', icon: Trash2, count: trashCount },
              ] as { id: Section; label: string; icon: React.ComponentType<{ className?: string }>; count?: number }[]
            ).map((item) => {
              const Icon = item.icon;
              const active = section === item.id;
              return (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => {
                    setSection(item.id);
                    if (item.id !== 'DOCUMENTS') setFolderId(null);
                  }}
                  onDragOver={(e) => {
                    e.preventDefault();
                    setDropHoverId(item.id);
                  }}
                  onDragLeave={() => setDropHoverId(null)}
                  onDrop={() => {
                    if (!dragPayload) return;
                    const [kind, id] = dragPayload.split(':');
                    if (kind === 'note') {
                      if (item.id === 'FAVORITES') onUpdateNotebook(id, (nb) => ({ ...nb, favorite: true }));
                      else if (item.id === 'TRASH') onSoftDeleteNotebook(id);
                      else if (item.id === 'DOCUMENTS') onUpdateNotebook(id, (nb) => ({ ...nb, folderId: null, deletedAt: null }));
                    }
                    setDragPayload(null);
                    setDropHoverId(null);
                  }}
                  className={`flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition ${
                    dropHoverId === item.id
                      ? 'bg-blue-500/20 text-blue-600'
                      : active
                        ? 'bg-white text-slate-900 shadow-sm dark:bg-[#242529] dark:text-white'
                        : 'text-slate-600 hover:bg-black/5 dark:text-slate-300 dark:hover:bg-white/5'
                  }`}
                >
                  <Icon className="h-4 w-4 shrink-0" />
                  <span className="flex-1 text-left">{item.label}</span>
                  {item.count !== undefined && item.count > 0 && (
                    <span className="font-mono-num rounded-md bg-black/8 px-1.5 py-0.5 text-[11px] font-semibold dark:bg-white/10">
                      {item.count}
                    </span>
                  )}
                </button>
              );
            })}
          </nav>

          {/* Cloud Sync, On-Device AI & Support */}
          <div className="mt-auto space-y-2 pt-4">
            {userEmail ? (
              <button
                type="button"
                onClick={onSyncNow}
                className="ios-stretch flex w-full items-center justify-between gap-2 rounded-2xl border border-emerald-500/25 bg-emerald-500/10 px-3 py-2 text-left"
              >
                <div className="min-w-0">
                  <div className="flex items-center gap-1.5 text-[11px] font-bold text-emerald-700 dark:text-emerald-300">
                    <Cloud className="h-3.5 w-3.5 shrink-0" /> {userName || 'Google Sync'}
                  </div>
                  <div className="truncate text-[10px] text-slate-500 dark:text-slate-400">
                    {syncBusy
                      ? 'Syncing...'
                      : lastSyncAt
                        ? `Synced ${new Date(lastSyncAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
                        : userEmail}
                  </div>
                </div>
                <RefreshCw className={`h-3.5 w-3.5 shrink-0 text-emerald-600 ${syncBusy ? 'animate-spin' : ''}`} />
              </button>
            ) : (
              onSignInGoogle && (
                <button
                  type="button"
                  onClick={onSignInGoogle}
                  className="ios-stretch flex w-full items-center gap-2 rounded-2xl border border-blue-500/25 bg-blue-500/10 px-3 py-2 text-xs font-bold text-blue-700 hover:bg-blue-500/18 dark:text-blue-300"
                >
                  <LogIn className="h-3.5 w-3.5 shrink-0" /> Sign in with Google Sync
                </button>
              )
            )}

            <button
              type="button"
              onClick={onOpenLocalModels}
              className="ios-stretch flex w-full flex-col gap-1 rounded-2xl border border-black/8 bg-white/80 p-3 text-left transition hover:border-violet-500/40 dark:border-white/10 dark:bg-[#202126]"
            >
              <div className="flex items-center justify-between">
                <span className="inline-flex items-center gap-1.5 text-xs font-bold text-violet-700 dark:text-violet-300">
                  <Cpu className="h-3.5 w-3.5" /> Local AI Models
                </span>
                <span className="font-mono-num rounded-md bg-violet-500/12 px-1.5 py-0.5 text-[10px] font-bold text-violet-700 dark:text-violet-300">
                  {readyModelCount} ready
                </span>
              </div>
              <span className="truncate text-[11px] text-slate-500 dark:text-slate-400">
                {activeModel ? activeModel.name : `${LOCAL_MODELS_CATALOG.length + IMAGE_MODELS_CATALOG.length} text & image models`}
              </span>
            </button>

            {onOpenSetupWizard && (
              <button
                type="button"
                onClick={onOpenSetupWizard}
                className="ios-stretch flex w-full items-center gap-3 rounded-xl px-3 py-2 text-sm font-medium text-slate-600 hover:bg-black/5 dark:text-slate-300 dark:hover:bg-white/5"
              >
                <Wand2 className="h-4 w-4 text-blue-600" />
                <span>Ultra Setup Studio</span>
              </button>
            )}

            {onOpenSupport && (
              <button
                type="button"
                onClick={onOpenSupport}
                className="ios-stretch flex w-full items-center gap-3 rounded-xl px-3 py-2 text-sm font-medium text-rose-600 hover:bg-rose-500/10 dark:text-rose-400"
              >
                <Heart className="h-4 w-4" />
                <span>Support Pro Notes</span>
              </button>
            )}

            <button
              type="button"
              onClick={onOpenSettings}
              className="ios-stretch flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-slate-600 hover:bg-black/5 dark:text-slate-300 dark:hover:bg-white/5"
            >
              <Settings className="h-4 w-4" />
              <span>Settings</span>
            </button>
          </div>
        </aside>
      )}

      <main className="flex min-w-0 flex-1 flex-col overflow-hidden">
        <header className="flex flex-wrap items-center justify-between gap-3 border-b border-black/8 bg-white/75 px-6 py-4 backdrop-blur-md dark:border-white/8 dark:bg-[#141415]/80">
          <div className="flex items-center gap-2">
            {!sidebarOpen && (
              <button
                type="button"
                onClick={() => setSidebarOpen(true)}
                className="rounded-xl p-2 text-slate-500 hover:bg-black/5 dark:hover:bg-white/10"
              >
                <PanelLeftOpen className="h-5 w-5" />
              </button>
            )}
            <div className="flex items-center gap-1.5 text-lg font-bold tracking-tight">
              <button type="button" onClick={() => setFolderId(null)} className="hover:underline">
                {section === 'DOCUMENTS'
                  ? 'Documents'
                  : section === 'FAVORITES'
                    ? 'Favorites'
                    : section === 'RECENT'
                      ? 'Recent'
                      : 'Trash'}
              </button>
              {breadcrumbs.map((f) => (
                <React.Fragment key={f.id}>
                  <ChevronRight className="h-4 w-4 text-slate-400" />
                  <button type="button" onClick={() => setFolderId(f.id)} className="hover:underline">
                    {f.name}
                  </button>
                </React.Fragment>
              ))}
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <div className="relative">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                type="text"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Search notebooks..."
                className="w-48 rounded-xl border border-black/10 bg-black/[0.03] py-1.5 pl-9 pr-3 text-sm outline-none focus:border-blue-500 dark:border-white/10 dark:bg-white/[0.04] sm:w-60"
              />
            </div>

            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value as 'modified' | 'title' | 'created')}
              className="rounded-xl border border-black/10 bg-white px-2.5 py-1.5 text-xs font-semibold dark:border-white/10 dark:bg-[#1E1F24]"
            >
              <option value="modified">Last Modified</option>
              <option value="created">Date Created</option>
              <option value="title">Title A–Z</option>
            </select>

            <div className="flex rounded-xl border border-black/10 bg-white p-0.5 dark:border-white/10 dark:bg-[#1E1F24]">
              <button
                type="button"
                onClick={() => setViewMode('grid')}
                className={`rounded-lg p-1.5 ${viewMode === 'grid' ? 'bg-black/8 dark:bg-white/12' : 'text-slate-400'}`}
              >
                <Grid className="h-4 w-4" />
              </button>
              <button
                type="button"
                onClick={() => setViewMode('list')}
                className={`rounded-lg p-1.5 ${viewMode === 'list' ? 'bg-black/8 dark:bg-white/12' : 'text-slate-400'}`}
              >
                <List className="h-4 w-4" />
              </button>
            </div>
          </div>
        </header>

        {supportBannerOpen && onOpenSupport && onDismissSupportBanner && (
          <SupportBanner onOpenSupport={onOpenSupport} onDismiss={onDismissSupportBanner} />
        )}

        {importToast && (
          <div className="mx-6 mt-3 rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-2 text-xs font-semibold text-emerald-700 dark:text-emerald-300">
            {importToast}
          </div>
        )}

        {section !== 'TRASH' ? (
          <div className="flex flex-wrap items-center gap-2.5 px-6 pt-5 pb-2">
            <button
              type="button"
              onClick={() => setNewModal({ open: true, mode: 'notebook' })}
              className="ios-stretch inline-flex shrink-0 items-center gap-2 whitespace-nowrap rounded-xl px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:opacity-95"
              style={{ backgroundColor: accent.primary }}
            >
              <Plus className="h-4 w-4" /> New Notebook
            </button>
            <button
              type="button"
              onClick={() => setNewModal({ open: true, mode: 'whiteboard' })}
              className="ios-stretch inline-flex shrink-0 items-center gap-2 whitespace-nowrap rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-3.5 py-2 text-sm font-semibold text-emerald-700 transition hover:bg-emerald-500/18 dark:text-emerald-300"
            >
              <Layers className="h-4 w-4" /> Infinite Whiteboard
            </button>
            <button
              type="button"
              onClick={() => setNewModal({ open: true, mode: 'mindmap' })}
              className="ios-stretch inline-flex shrink-0 items-center gap-2 whitespace-nowrap rounded-xl border border-violet-500/30 bg-violet-500/10 px-3.5 py-2 text-sm font-semibold text-violet-700 transition hover:bg-violet-500/18 dark:text-violet-300"
            >
              <Brain className="h-4 w-4" /> Mind Map
            </button>
            {section === 'DOCUMENTS' && (
              <button
                type="button"
                onClick={() => setNewFolderOpen(true)}
                className="ios-stretch inline-flex shrink-0 items-center gap-1.5 whitespace-nowrap rounded-xl border border-black/10 bg-white px-3.5 py-2 text-sm font-semibold text-slate-700 hover:bg-black/5 dark:border-white/10 dark:bg-[#1E1F24] dark:text-slate-200"
              >
                <FolderPlus className="h-4 w-4" /> New Folder
              </button>
            )}
            <button
              type="button"
              onClick={() => setImportModalOpen(true)}
              className="ios-stretch inline-flex shrink-0 items-center gap-1.5 whitespace-nowrap rounded-xl border border-black/10 bg-white px-3.5 py-2 text-sm font-semibold text-slate-700 hover:bg-black/5 dark:border-white/10 dark:bg-[#1E1F24] dark:text-slate-200"
            >
              <Upload className="h-4 w-4" /> Import Notes
            </button>
            {onOpenCollab && (
              <button
                type="button"
                onClick={onOpenCollab}
                className="ios-stretch inline-flex shrink-0 items-center gap-1.5 whitespace-nowrap rounded-xl border border-cyan-500/30 bg-cyan-500/10 px-3.5 py-2 text-sm font-semibold text-cyan-700 transition hover:bg-cyan-500/18 dark:text-cyan-300"
              >
                <Users className="h-4 w-4" /> Live Collab Room
              </button>
            )}
          </div>
        ) : (
          trashCount > 0 && (
            <div className="flex items-center justify-between px-6 pt-5 pb-2">
              <span className="text-xs text-slate-500">Deleted notebooks stay in Trash until emptied.</span>
              <button
                type="button"
                onClick={onEmptyTrash}
                className="rounded-xl bg-red-600 px-3.5 py-1.5 text-xs font-semibold text-white hover:bg-red-700"
              >
                Empty Trash
              </button>
            </div>
          )
        )}

        <div className="flex-1 overflow-y-auto px-6 py-4">
          {visibleFolders.length === 0 && visibleNotebooks.length === 0 ? (
            <div className="flex h-72 flex-col items-center justify-center text-center">
              <p className="text-base font-bold text-slate-700 dark:text-slate-200">
                {section === 'TRASH' ? 'Trash is empty' : 'Nothing here yet'}
              </p>
              <p className="mt-1 text-sm text-slate-500">
                {section === 'TRASH'
                  ? 'Deleted notebooks will appear here.'
                  : 'Create a notebook, infinite whiteboard, or mind map to begin.'}
              </p>
            </div>
          ) : viewMode === 'grid' ? (
            <div className="grid grid-cols-2 gap-6 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">
              {visibleFolders.map((f) => {
                const count = notebooks.filter((n) => n.folderId === f.id && n.deletedAt === null).length;
                return (
                  <div
                    key={f.id}
                    onDragOver={(e) => {
                      e.preventDefault();
                      setDropHoverId(f.id);
                    }}
                    onDragLeave={() => setDropHoverId(null)}
                    onDrop={() => handleDropOnFolder(f.id)}
                    className={`group relative flex flex-col items-center rounded-2xl p-2 transition ${
                      dropHoverId === f.id ? 'scale-105 bg-blue-500/15' : 'hover:bg-black/[0.03] dark:hover:bg-white/[0.03]'
                    }`}
                  >
                    <button
                      type="button"
                      onClick={() => setFolderId(f.id)}
                      className="flex flex-col items-center"
                    >
                      <FolderArt color={f.color} emoji={f.emoji} className="h-28 w-32 transition-transform group-hover:scale-[1.03]" />
                    </button>
                    <div className="mt-2 flex w-full items-center justify-between px-1">
                      <div className="min-w-0 flex-1 text-center">
                        <div className="truncate text-sm font-semibold">{f.name}</div>
                        <div className="text-[11px] text-slate-500">{count} items</div>
                      </div>
                      <button
                        type="button"
                        onClick={() => onDeleteFolder(f.id)}
                        title="Delete folder"
                        className="rounded p-1 text-slate-400 opacity-0 transition group-hover:opacity-100 hover:text-red-500"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </button>
                    </div>
                  </div>
                );
              })}

              {visibleNotebooks.map((note) => (
                <NotebookGridCard
                  key={note.id}
                  note={note}
                  menuOpen={menuId === note.id}
                  onOpen={onOpenNote}
                  onToggleMenu={(id) => setMenuId((cur) => (cur === id ? null : id))}
                  onDragStart={(id) => setDragPayload(`note:${id}`)}
                  onToggleFavorite={(id) => {
                    onUpdateNotebook(id, (n) => ({ ...n, favorite: !n.favorite }));
                    setMenuId(null);
                  }}
                  onDuplicate={(id) => {
                    onDuplicateNotebook(id);
                    setMenuId(null);
                  }}
                  onSoftDelete={(id) => {
                    onSoftDeleteNotebook(id);
                    setMenuId(null);
                  }}
                  onRestore={(id) => {
                    onRestoreNotebook(id);
                    setMenuId(null);
                  }}
                  onDeleteForever={(id) => {
                    onDeleteForeverNotebook(id);
                    setMenuId(null);
                  }}
                />
              ))}
            </div>
          ) : (
            <div className="space-y-1.5">
              {visibleFolders.map((f) => (
                <div
                  key={f.id}
                  onClick={() => setFolderId(f.id)}
                  className="flex cursor-pointer items-center gap-4 rounded-2xl border border-black/6 bg-white px-4 py-2.5 hover:bg-slate-50 dark:border-white/8 dark:bg-[#1A1B20] dark:hover:bg-[#22232A]"
                >
                  <FolderArt color={f.color} emoji={f.emoji} className="h-10 w-12" />
                  <div className="flex-1 font-semibold">{f.name}</div>
                </div>
              ))}
              {visibleNotebooks.map((note) => (
                <div
                  key={note.id}
                  onClick={() => note.deletedAt === null && onOpenNote(note.id)}
                  className="flex cursor-pointer items-center gap-4 rounded-2xl border border-black/6 bg-white px-4 py-2.5 hover:bg-slate-50 dark:border-white/8 dark:bg-[#1A1B20] dark:hover:bg-[#22232A]"
                >
                  <CoverCanvas spec={note.cover} title="" width={40} height={54} />
                  <div className="min-w-0 flex-1">
                    <div className="truncate font-semibold">{note.title}</div>
                    <div className="font-mono-num text-xs text-slate-500">
                      {note.pageCount} page(s) · {formatDate(note.modified)}
                    </div>
                  </div>
                  {note.favorite && <Star className="h-4 w-4 fill-amber-400 text-amber-400" />}
                </div>
              ))}
            </div>
          )}
        </div>
      </main>

      <NewNotebookModal
        open={newModal.open}
        defaults={settings}
        initialMode={newModal.mode}
        onClose={() => setNewModal((s) => ({ ...s, open: false }))}
        onCreate={(payload) => {
          setNewModal((s) => ({ ...s, open: false }));
          onCreateNotebook({ ...payload, folderId });
        }}
      />

      {newFolderOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
          <div className="w-full max-w-sm rounded-3xl border border-black/10 bg-white p-6 shadow-2xl dark:border-white/10 dark:bg-[#191A1F]">
            <h2 className="text-base font-bold">New Folder</h2>
            <div className="my-4 flex justify-center">
              <FolderArt color={folderColor} emoji={folderEmoji} className="h-24 w-28" />
            </div>
            <input
              type="text"
              value={folderName}
              onChange={(e) => setFolderName(e.target.value)}
              className="mb-3 w-full rounded-xl border border-black/12 px-3 py-2 text-sm dark:border-white/15 dark:bg-[#22242C]"
            />
            <div className="mb-3 flex flex-wrap gap-2">
              {FOLDER_COLORS.map((c) => (
                <button
                  key={c}
                  type="button"
                  onClick={() => setFolderColor(c)}
                  className={`h-6 w-6 rounded-full ${folderColor === c ? 'ring-2 ring-blue-600 ring-offset-2' : ''}`}
                  style={{ backgroundColor: c }}
                />
              ))}
            </div>
            <div className="mb-4 flex flex-wrap gap-1.5">
              {['', '📚', '📝', '🎓', '🧪', '🎨', '💡', '🧠', '📈'].map((em) => (
                <button
                  key={em || 'none'}
                  type="button"
                  onClick={() => setFolderEmoji(em)}
                  className={`h-8 w-8 rounded-lg text-base ${
                    folderEmoji === em ? 'bg-blue-500/20' : 'hover:bg-black/5'
                  }`}
                >
                  {em || '∅'}
                </button>
              ))}
            </div>
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setNewFolderOpen(false)}
                className="rounded-xl px-3.5 py-2 text-xs font-semibold text-slate-500"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => {
                  onCreateFolder(folderName, folderId, folderColor, folderEmoji);
                  setNewFolderOpen(false);
                }}
                className="rounded-xl bg-blue-600 px-4 py-2 text-xs font-semibold text-white"
              >
                Save
              </button>
            </div>
          </div>
        </div>
      )}

      {importModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
          <div className="flex max-h-[88vh] w-full max-w-xl flex-col overflow-hidden rounded-3xl border border-black/10 bg-white shadow-2xl dark:border-white/10 dark:bg-[#191A1F]">
            <div className="flex items-center justify-between border-b border-black/8 px-6 py-4 dark:border-white/10">
              <div>
                <h2 className="text-base font-bold">Import Notes from Other Apps</h2>
                <p className="text-xs text-slate-500 dark:text-slate-400">
                  Markdown, Notion, Obsidian, Google Keep, Apple Notes, HTML, PDF &amp; Images
                </p>
              </div>
              <button
                type="button"
                onClick={() => setImportModalOpen(false)}
                className="rounded-full p-1.5 text-slate-500 hover:bg-black/5 dark:hover:bg-white/10"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="flex gap-2 border-b border-black/8 px-6 py-2.5 dark:border-white/10">
              <button
                type="button"
                onClick={() => setImportTab('files')}
                className={`inline-flex items-center gap-1.5 rounded-xl px-3.5 py-1.5 text-xs font-semibold transition ${
                  importTab === 'files'
                    ? 'bg-blue-600 text-white'
                    : 'bg-black/5 text-slate-600 hover:bg-black/10 dark:bg-white/10 dark:text-slate-300'
                }`}
              >
                <Upload className="h-3.5 w-3.5" /> Files &amp; Backups
              </button>
              <button
                type="button"
                onClick={() => setImportTab('paste')}
                className={`inline-flex items-center gap-1.5 rounded-xl px-3.5 py-1.5 text-xs font-semibold transition ${
                  importTab === 'paste'
                    ? 'bg-blue-600 text-white'
                    : 'bg-black/5 text-slate-600 hover:bg-black/10 dark:bg-white/10 dark:text-slate-300'
                }`}
              >
                <FileText className="h-3.5 w-3.5" /> Paste from App
              </button>
            </div>

            <div className="flex-1 overflow-y-auto p-6">
              {importTab === 'files' ? (
                <div
                  onDragOver={(e) => e.preventDefault()}
                  onDrop={(e) => {
                    e.preventDefault();
                    if (e.dataTransfer.files?.length) handleImportFilesList(e.dataTransfer.files);
                  }}
                  className="flex flex-col items-center justify-center rounded-2xl border-2 border-dashed border-black/15 bg-slate-50/70 px-6 py-10 text-center dark:border-white/15 dark:bg-[#22242C]/60"
                >
                  <Upload className="mb-3 h-8 w-8 text-blue-600" />
                  <p className="text-sm font-bold">
                    {importBusy ? 'Parsing imported notes...' : 'Drop files here or browse'}
                  </p>
                  <p className="mt-1 max-w-sm text-xs text-slate-500 dark:text-slate-400">
                    Supports .md, .txt, .html, .csv, Google Keep .json, Pro Notes backups, PDFs, and images. Multiple files become structured A4 notebooks.
                  </p>
                  <label className="mt-4 inline-flex cursor-pointer items-center gap-2 rounded-xl bg-blue-600 px-4 py-2 text-xs font-semibold text-white hover:bg-blue-700">
                    <Upload className="h-3.5 w-3.5" /> Choose Files
                    <input
                      type="file"
                      multiple
                      accept=".md,.markdown,.txt,.html,.htm,.csv,.json,.pdf,image/*"
                      onChange={(e) => {
                        if (e.target.files?.length) handleImportFilesList(e.target.files);
                        e.target.value = '';
                      }}
                      className="hidden"
                    />
                  </label>
                </div>
              ) : (
                <div className="space-y-3">
                  <div className="flex flex-wrap gap-1.5">
                    {(['Notion', 'Obsidian', 'Apple Notes', 'Google Keep'] as const).map((src) => (
                      <button
                        key={src}
                        type="button"
                        onClick={() => setPasteAppPreset(src)}
                        className={`rounded-lg px-2.5 py-1 text-xs font-semibold transition ${
                          pasteAppPreset === src
                            ? 'bg-blue-600 text-white'
                            : 'bg-black/5 text-slate-600 dark:bg-white/10 dark:text-slate-300'
                        }`}
                      >
                        {src}
                      </button>
                    ))}
                  </div>
                  <input
                    type="text"
                    value={pasteTitle}
                    onChange={(e) => setPasteTitle(e.target.value)}
                    placeholder={`Notebook title (e.g. ${pasteAppPreset} Lecture Notes)`}
                    className="w-full rounded-xl border border-black/12 bg-white px-3 py-2 text-sm font-medium outline-none focus:border-blue-500 dark:border-white/15 dark:bg-[#22242C]"
                  />
                  <textarea
                    value={pasteBody}
                    onChange={(e) => setPasteBody(e.target.value)}
                    rows={8}
                    placeholder="Paste Markdown headings (#, ##), Obsidian [[links]], checklists ([ ] / [x]), callouts (> quote), or plain text..."
                    className="w-full rounded-xl border border-black/12 bg-white px-3 py-2 text-xs leading-relaxed outline-none focus:border-blue-500 dark:border-white/15 dark:bg-[#22242C]"
                  />
                  {pastePreview && (
                    <div className="flex items-center justify-between rounded-xl border border-blue-500/20 bg-blue-50/70 px-3.5 py-2 text-xs dark:bg-blue-950/25">
                      <span className="font-semibold text-blue-700 dark:text-blue-300">
                        Preview ready
                      </span>
                      <span className="font-mono-num text-slate-600 dark:text-slate-300">
                        {pastePreview.blockCount} blocks · {pastePreview.pages.length} A4 page{pastePreview.pages.length === 1 ? '' : 's'}
                      </span>
                    </div>
                  )}
                  <div className="flex justify-end gap-2 pt-1">
                    <button
                      type="button"
                      onClick={() => setImportModalOpen(false)}
                      className="rounded-xl px-4 py-2 text-xs font-semibold text-slate-500"
                    >
                      Cancel
                    </button>
                    <button
                      type="button"
                      disabled={!pastePreview}
                      onClick={handleConfirmPasteImport}
                      className="rounded-xl bg-blue-600 px-4 py-2 text-xs font-semibold text-white hover:bg-blue-700 disabled:opacity-45"
                    >
                      Import Notebook
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
