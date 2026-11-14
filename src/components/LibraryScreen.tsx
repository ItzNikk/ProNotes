import React, { useMemo, useState } from 'react';
import {
  ArrowUpDown,
  BookOpen,
  Brain,
  ChevronRight,
  Clock,
  Cloud,
  Copy,
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
import { parseImportedFiles, parseImportedText } from '../utils/noteImporter';
import { SupportBanner } from './SupportBannerModal';
import {
  LiquidBottomTab,
  LiquidBottomTabs,
  LiquidButton,
  LiquidGlassSurface,
  LiquidInput,
  LiquidMenuButton,
  LiquidMenuItemOption,
  LiquidModal,
  LiquidSegmentedControl,
} from './LiquidGlassSurface';

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
  onOpen: (id: string) => void;
  onDragStart: (id: string) => void;
  onToggleFavorite: (id: string) => void;
  onDuplicate: (id: string) => void;
  onSoftDelete: (id: string) => void;
  onRestore: (id: string) => void;
  onDeleteForever: (id: string) => void;
}>(({
  note,
  onOpen,
  onDragStart,
  onToggleFavorite,
  onDuplicate,
  onSoftDelete,
  onRestore,
  onDeleteForever,
}) => {
  const menuItems: LiquidMenuItemOption[] =
    note.deletedAt === null
      ? [
          {
            id: 'open',
            label: 'Open Notebook',
            icon: <BookOpen className="h-3.5 w-3.5 text-[#24483F] dark:text-[#8CC1AC]" />,
            onSelect: () => onOpen(note.id),
          },
          {
            id: 'fav',
            label: note.favorite ? 'Remove favorite' : 'Add favorite',
            icon: <Star className="h-3.5 w-3.5 text-amber-500" />,
            onSelect: () => onToggleFavorite(note.id),
          },
          {
            id: 'dup',
            label: 'Duplicate copy',
            icon: <Copy className="h-3.5 w-3.5 text-indigo-500" />,
            onSelect: () => onDuplicate(note.id),
          },
          {
            id: 'trash',
            label: 'Move to Trash',
            icon: <Trash2 className="h-3.5 w-3.5" />,
            destructive: true,
            dividerBefore: true,
            onSelect: () => onSoftDelete(note.id),
          },
        ]
      : [
          {
            id: 'restore',
            label: 'Restore',
            icon: <RotateCcw className="h-3.5 w-3.5 text-emerald-600" />,
            onSelect: () => onRestore(note.id),
          },
          {
            id: 'forever',
            label: 'Delete Forever',
            icon: <Trash2 className="h-3.5 w-3.5" />,
            destructive: true,
            dividerBefore: true,
            onSelect: () => onDeleteForever(note.id),
          },
        ];

  return (
    <div
      draggable={note.deletedAt === null}
      onDragStart={() => onDragStart(note.id)}
      className="ios-stretch-card group relative flex flex-col items-center"
    >
      <div
        onClick={() => (note.deletedAt === null ? onOpen(note.id) : onRestore(note.id))}
        className="relative cursor-pointer transition-transform duration-200 group-hover:-translate-y-1"
      >
        <CoverCanvas spec={note.cover} title={note.title} width={154} height={206} />
        {note.favorite && (
          <Star className="absolute right-2.5 top-2.5 h-4 w-4 fill-amber-400 text-amber-400 drop-shadow" />
        )}
        {(note.isWhiteboard || note.isMindMap) && (
          <span className="liquid-glass-frost absolute bottom-2.5 left-2.5 inline-flex items-center gap-1 rounded-md px-1.5 py-0.5 text-[10px] font-semibold">
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
          <div className="font-mono-num text-[11px] text-[#5A5F58] dark:text-[#B1AC9F]">
            {formatDate(note.modified)}
          </div>
        </div>
        <LiquidMenuButton
          items={menuItems}
          align="right"
          menuWidthClass="min-w-[180px]"
          trigger={() => (
            <span className="liquid-glass-frost inline-flex h-7 w-7 items-center justify-center rounded-full text-[#5A5F58] hover:text-[#1F2421] dark:text-[#B1AC9F] dark:hover:text-[#EDE8DB]">
              <MoreVertical className="h-3.5 w-3.5" />
            </span>
          )}
        />
      </div>
    </div>
  );
});

export const LibraryScreen: React.FC<LibraryScreenProps> = ({
  folders,
  notebooks,
  settings,
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
  const [dragPayload, setDragPayload] = useState<string | null>(null);
  const [dropHoverId, setDropHoverId] = useState<string | null>(null);

  const navTabItems = useMemo(
    () => [
      { id: 'DOCUMENTS' as const, label: 'Documents', icon: FileText },
      { id: 'RECENT' as const, label: 'Recent', icon: Clock },
      { id: 'FAVORITES' as const, label: 'Favorites', icon: Star },
      { id: 'TRASH' as const, label: 'Trash', icon: Trash2 },
    ],
    []
  );

  const selectedTabIndex = useMemo(
    () => Math.max(0, navTabItems.findIndex((t) => t.id === section)),
    [navTabItems, section]
  );

  const accent = ACCENT_COLORS[settings.accent];
  const sortLabels: Record<'modified' | 'title' | 'created', string> = {
    modified: 'Last Modified',
    created: 'Date Created',
    title: 'Title A–Z',
  };

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
    showImportBanner(
      `Imported "${pastePreview.title}" (${pastePreview.pages.length} page${pastePreview.pages.length === 1 ? '' : 's'})`
    );
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
    <div className="relative flex h-screen w-screen overflow-hidden bg-[#EFEBE2] text-[#1F2421] dark:bg-[#141411] dark:text-[#EDE8DB]">
      {/* OpenLeaf Ambient Botanical Mesh (refraction backdrop for Liquid Glass) */}
      <div className="pointer-events-none absolute inset-0 overflow-hidden">
        <div
          className="absolute -left-20 -top-24 h-96 w-96 rounded-full opacity-35 blur-3xl dark:opacity-20"
          style={{ background: `radial-gradient(circle, ${accent.primary}45, transparent 70%)` }}
        />
        <div
          className="absolute bottom-0 right-12 h-[420px] w-[420px] rounded-full opacity-28 blur-3xl dark:opacity-18"
          style={{ background: `radial-gradient(circle, ${accent.secondary}40, transparent 70%)` }}
        />
      </div>

      {sidebarOpen && (
        <LiquidGlassSurface
          enabled={settings.liquidGlass}
          variant="chrome"
          radius={0}
          className="relative z-20 hidden w-60 shrink-0 border-r border-white/45 dark:border-[#EDE8DB]/12 md:flex"
        >
          <aside className="flex h-full w-60 flex-col px-3.5 py-4">
            <div className="mb-5 flex items-center justify-between px-1.5">
              <div className="flex items-center gap-2.5">
                <div
                  className="flex h-9 w-9 items-center justify-center rounded-xl text-[#F5F2E8] shadow-sm"
                  style={{
                    background: `linear-gradient(135deg, ${accent.primary}, ${accent.secondary})`,
                  }}
                >
                  <span className="font-display text-base font-extrabold">P</span>
                </div>
                <div>
                  <h1 className="font-display text-base font-bold leading-tight tracking-tight">Pro Notes</h1>
                  <span className="text-[11px] font-semibold text-[#24483F] dark:text-[#8CC1AC]">
                    OpenLeaf Edition
                  </span>
                </div>
              </div>
              <LiquidButton
                onClick={() => setSidebarOpen(false)}
                className="!h-8 !w-8 !p-0"
                title="Collapse sidebar"
              >
                <PanelLeftClose className="h-4 w-4" />
              </LiquidButton>
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
                        else if (item.id === 'DOCUMENTS')
                          onUpdateNotebook(id, (nb) => ({ ...nb, folderId: null, deletedAt: null }));
                      }
                      setDragPayload(null);
                      setDropHoverId(null);
                    }}
                    className={`flex w-full items-center gap-3 rounded-2xl px-3 py-2.5 text-sm font-semibold transition ${
                      dropHoverId === item.id
                        ? 'bg-[#24483F]/20 text-[#24483F] dark:text-[#8CC1AC]'
                        : active
                          ? 'liquid-glass-frost text-[#24483F] dark:text-[#8CC1AC]'
                          : 'text-[#5A5F58] hover:bg-black/5 dark:text-[#B1AC9F] dark:hover:bg-white/5'
                    }`}
                  >
                    <Icon className="h-4 w-4 shrink-0" />
                    <span className="flex-1 text-left">{item.label}</span>
                    {item.count !== undefined && item.count > 0 && (
                      <span className="font-mono-num text-[11px] font-bold opacity-80">{item.count}</span>
                    )}
                  </button>
                );
              })}
            </nav>

            {/* Cloud Sync, Setup & Support */}
            <div className="mt-auto space-y-2 pt-4">
              {userEmail ? (
                <LiquidGlassSurface
                  enabled={settings.liquidGlass}
                  variant="frost"
                  radius={18}
                  className="p-2.5"
                >
                  <button
                    type="button"
                    onClick={onSyncNow}
                    className="flex w-full items-center justify-between gap-2 text-left"
                  >
                    <div className="min-w-0">
                      <div className="flex items-center gap-1.5 text-[11px] font-bold text-[#24483F] dark:text-[#8CC1AC]">
                        <Cloud className="h-3.5 w-3.5 shrink-0" /> {userName || 'Google Sync'}
                      </div>
                      <div className="truncate text-[10px] text-[#5A5F58] dark:text-[#B1AC9F]">
                        {syncBusy
                          ? 'Syncing...'
                          : lastSyncAt
                            ? `Synced ${new Date(lastSyncAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
                            : userEmail}
                      </div>
                    </div>
                    <RefreshCw
                      className={`h-3.5 w-3.5 shrink-0 text-[#24483F] dark:text-[#8CC1AC] ${syncBusy ? 'animate-spin' : ''}`}
                    />
                  </button>
                </LiquidGlassSurface>
              ) : (
                onSignInGoogle && (
                  <LiquidButton onClick={onSignInGoogle} tint={accent.primary} className="w-full !justify-start !text-xs">
                    <LogIn className="h-3.5 w-3.5 shrink-0" /> Sign in with Google Sync
                  </LiquidButton>
                )
              )}

              {onOpenSetupWizard && (
                <LiquidButton onClick={onOpenSetupWizard} className="w-full !justify-start !text-xs">
                  <Wand2 className="h-4 w-4 text-[#24483F] dark:text-[#8CC1AC]" />
                  <span>Setup Studio</span>
                </LiquidButton>
              )}

              {onOpenSupport && (
                <LiquidButton onClick={onOpenSupport} className="w-full !justify-start !text-xs text-rose-600 dark:text-rose-400">
                  <Heart className="h-4 w-4" />
                  <span>Support Pro Notes</span>
                </LiquidButton>
              )}

              <LiquidButton onClick={onOpenSettings} className="w-full !justify-start !text-xs">
                <Settings className="h-4 w-4" />
                <span>Settings</span>
              </LiquidButton>
            </div>
          </aside>
        </LiquidGlassSurface>
      )}

      <main className="relative z-10 flex min-w-0 flex-1 flex-col overflow-hidden">
        <LiquidGlassSurface
          enabled={settings.liquidGlass}
          variant="chrome"
          radius={0}
          className="relative z-30 shrink-0 border-b border-white/45 dark:border-[#EDE8DB]/12"
        >
          <header className="flex flex-wrap items-center justify-between gap-3 px-6 py-3.5">
            <div className="flex items-center gap-2">
              {!sidebarOpen && (
                <LiquidButton
                  onClick={() => setSidebarOpen(true)}
                  className="!h-9 !w-9 !p-0"
                  title="Show sidebar"
                >
                  <PanelLeftOpen className="h-4 w-4" />
                </LiquidButton>
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
                    <ChevronRight className="h-4 w-4 text-[#5A5F58] dark:text-[#B1AC9F]" />
                    <button type="button" onClick={() => setFolderId(f.id)} className="hover:underline">
                      {f.name}
                    </button>
                  </React.Fragment>
                ))}
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <LiquidInput
                type="text"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Search notebooks..."
                icon={<Search className="h-3.5 w-3.5" />}
                wrapperClassName="w-44 sm:w-56"
              />

              <LiquidMenuButton
                label={sortLabels[sortBy]}
                icon={<ArrowUpDown className="h-3.5 w-3.5" />}
                items={[
                  {
                    id: 'modified',
                    label: 'Last Modified',
                    selected: sortBy === 'modified',
                    onSelect: () => setSortBy('modified'),
                  },
                  {
                    id: 'created',
                    label: 'Date Created',
                    selected: sortBy === 'created',
                    onSelect: () => setSortBy('created'),
                  },
                  {
                    id: 'title',
                    label: 'Title A–Z',
                    selected: sortBy === 'title',
                    onSelect: () => setSortBy('title'),
                  },
                ]}
              />

              <LiquidSegmentedControl<'grid' | 'list'>
                value={viewMode}
                onChange={setViewMode}
                accentColor={accent.primary}
                options={[
                  { value: 'grid', label: 'Grid' },
                  { value: 'list', label: 'List' },
                ]}
              />
            </div>
          </header>
        </LiquidGlassSurface>

        {supportBannerOpen && onOpenSupport && onDismissSupportBanner && (
          <SupportBanner onOpenSupport={onOpenSupport} onDismiss={onDismissSupportBanner} />
        )}

        {importToast && (
          <LiquidGlassSurface
            enabled={settings.liquidGlass}
            variant="frost"
            radius={16}
            className="mx-6 mt-3 px-4 py-2 text-xs font-semibold text-[#24483F] dark:text-[#8CC1AC]"
          >
            {importToast}
          </LiquidGlassSurface>
        )}

        {section !== 'TRASH' ? (
          <div className="flex flex-wrap items-center gap-2.5 px-6 pt-5 pb-2">
            <LiquidButton onClick={() => setNewModal({ open: true, mode: 'notebook' })} tint={accent.primary}>
              <Plus className="h-4 w-4" /> New Notebook
            </LiquidButton>
            <LiquidButton onClick={() => setNewModal({ open: true, mode: 'whiteboard' })}>
              <Layers className="h-4 w-4 text-[#24483F] dark:text-[#8CC1AC]" /> Infinite Whiteboard
            </LiquidButton>
            <LiquidButton onClick={() => setNewModal({ open: true, mode: 'mindmap' })}>
              <Brain className="h-4 w-4 text-[#24483F] dark:text-[#8CC1AC]" /> Mind Map
            </LiquidButton>
            {section === 'DOCUMENTS' && (
              <LiquidButton onClick={() => setNewFolderOpen(true)}>
                <FolderPlus className="h-4 w-4" /> New Folder
              </LiquidButton>
            )}
            <LiquidButton onClick={() => setImportModalOpen(true)}>
              <Upload className="h-4 w-4" /> Import Notes
            </LiquidButton>
            {onOpenCollab && (
              <LiquidButton onClick={onOpenCollab}>
                <Users className="h-4 w-4 text-[#24483F] dark:text-[#8CC1AC]" /> Live Collab Room
              </LiquidButton>
            )}
          </div>
        ) : (
          trashCount > 0 && (
            <div className="flex items-center justify-between px-6 pt-5 pb-2">
              <span className="text-xs text-[#5A5F58] dark:text-[#B1AC9F]">
                Deleted notebooks stay in Trash until emptied.
              </span>
              <LiquidButton onClick={onEmptyTrash} tint="#9B3B3B">
                Empty Trash
              </LiquidButton>
            </div>
          )
        )}

        <div className="flex-1 overflow-y-auto px-6 py-4 pb-28">
          {visibleFolders.length === 0 && visibleNotebooks.length === 0 ? (
            <div className="flex h-72 flex-col items-center justify-center text-center">
              <p className="text-base font-bold">
                {section === 'TRASH' ? 'Trash is empty' : 'Nothing here yet'}
              </p>
              <p className="mt-1 text-sm text-[#5A5F58] dark:text-[#B1AC9F]">
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
                  <LiquidGlassSurface
                    key={f.id}
                    enabled={settings.liquidGlass}
                    variant="frost"
                    radius={22}
                    className={`group relative flex flex-col items-center p-3 transition ${
                      dropHoverId === f.id ? 'scale-105 ring-2 ring-[#24483F]' : ''
                    }`}
                  >
                    <div
                      onDragOver={(e) => {
                        e.preventDefault();
                        setDropHoverId(f.id);
                      }}
                      onDragLeave={() => setDropHoverId(null)}
                      onDrop={() => handleDropOnFolder(f.id)}
                      className="flex w-full flex-col items-center"
                    >
                      <button
                        type="button"
                        onClick={() => setFolderId(f.id)}
                        className="flex flex-col items-center"
                      >
                        <FolderArt
                          color={f.color}
                          emoji={f.emoji}
                          className="h-24 w-28 transition-transform group-hover:scale-[1.03]"
                        />
                      </button>
                      <div className="mt-2 flex w-full items-center justify-between px-1">
                        <div className="min-w-0 flex-1 text-center">
                          <div className="truncate text-sm font-semibold">{f.name}</div>
                          <div className="font-mono-num text-[11px] text-[#5A5F58] dark:text-[#B1AC9F]">
                            {count} items
                          </div>
                        </div>
                        <button
                          type="button"
                          onClick={() => onDeleteFolder(f.id)}
                          title="Delete folder"
                          className="rounded p-1 text-[#5A5F58] opacity-0 transition group-hover:opacity-100 hover:text-red-500"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    </div>
                  </LiquidGlassSurface>
                );
              })}

              {visibleNotebooks.map((note) => (
                <NotebookGridCard
                  key={note.id}
                  note={note}
                  onOpen={onOpenNote}
                  onDragStart={(id) => setDragPayload(`note:${id}`)}
                  onToggleFavorite={(id) => onUpdateNotebook(id, (n) => ({ ...n, favorite: !n.favorite }))}
                  onDuplicate={onDuplicateNotebook}
                  onSoftDelete={onSoftDeleteNotebook}
                  onRestore={onRestoreNotebook}
                  onDeleteForever={onDeleteForeverNotebook}
                />
              ))}
            </div>
          ) : (
            <div className="space-y-2">
              {visibleFolders.map((f) => (
                <LiquidGlassSurface
                  key={f.id}
                  enabled={settings.liquidGlass}
                  variant="frost"
                  radius={18}
                  className="cursor-pointer px-4 py-2.5 transition hover:brightness-[0.98]"
                >
                  <div onClick={() => setFolderId(f.id)} className="flex items-center gap-4">
                    <FolderArt color={f.color} emoji={f.emoji} className="h-10 w-12" />
                    <div className="flex-1 font-semibold">{f.name}</div>
                  </div>
                </LiquidGlassSurface>
              ))}
              {visibleNotebooks.map((note) => (
                <LiquidGlassSurface
                  key={note.id}
                  enabled={settings.liquidGlass}
                  variant="frost"
                  radius={18}
                  className="cursor-pointer px-4 py-2.5 transition hover:brightness-[0.98]"
                >
                  <div
                    onClick={() => note.deletedAt === null && onOpenNote(note.id)}
                    className="flex items-center gap-4"
                  >
                    <CoverCanvas spec={note.cover} title="" width={40} height={54} />
                    <div className="min-w-0 flex-1">
                      <div className="truncate font-semibold">{note.title}</div>
                      <div className="font-mono-num text-xs text-[#5A5F58] dark:text-[#B1AC9F]">
                        {note.pageCount} page(s) · {formatDate(note.modified)}
                      </div>
                    </div>
                    {note.favorite && <Star className="h-4 w-4 fill-amber-400 text-amber-400" />}
                  </div>
                </LiquidGlassSurface>
              ))}
            </div>
          )}
        </div>

        {/* ntvien/liquid-glass Prebuilt LiquidBottomTabs + LiquidButton */}
        <div className="pointer-events-none fixed bottom-4 left-1/2 z-30 flex -translate-x-1/2 items-center gap-3 px-4 select-none">
          <LiquidBottomTabs
            selectedTabIndex={selectedTabIndex}
            tabCount={navTabItems.length}
            enabled={settings.liquidGlass}
            onTabSelected={(idx) => {
              const target = navTabItems[idx];
              if (target) {
                setSection(target.id);
                setFolderId(null);
              }
            }}
          >
            {navTabItems.map((tab) => {
              const IconComp = tab.icon;
              const active = section === tab.id;
              return (
                <LiquidBottomTab
                  key={tab.id}
                  active={active}
                  onClick={() => {
                    setSection(tab.id);
                    setFolderId(null);
                  }}
                >
                  <IconComp className="h-4 w-4" />
                  <span>{tab.label}</span>
                </LiquidBottomTab>
              );
            })}
          </LiquidBottomTabs>

          <LiquidButton
            onClick={() => setNewModal({ open: true, mode: 'notebook' })}
            title="New Notebook"
            tint={accent.primary}
            className="pointer-events-auto !h-12 !w-12 !p-0 shrink-0"
          >
            <Plus className="h-5 w-5" />
          </LiquidButton>
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

      <LiquidModal open={newFolderOpen} onClose={() => setNewFolderOpen(false)} maxWidthClass="max-w-sm">
        <div className="p-6">
          <h2 className="text-base font-bold">New Folder</h2>
          <div className="my-4 flex justify-center">
            <FolderArt color={folderColor} emoji={folderEmoji} className="h-24 w-28" />
          </div>
          <LiquidInput
            type="text"
            value={folderName}
            onChange={(e) => setFolderName(e.target.value)}
            wrapperClassName="mb-3"
          />
          <div className="mb-3 flex flex-wrap gap-2">
            {FOLDER_COLORS.map((c) => (
              <button
                key={c}
                type="button"
                onClick={() => setFolderColor(c)}
                className={`h-6 w-6 rounded-full ${folderColor === c ? 'ring-2 ring-[#24483F] ring-offset-2' : ''}`}
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
                  folderEmoji === em ? 'bg-[#24483F]/20' : 'hover:bg-black/5 dark:hover:bg-white/10'
                }`}
              >
                {em || '∅'}
              </button>
            ))}
          </div>
          <div className="flex justify-end gap-2">
            <LiquidButton onClick={() => setNewFolderOpen(false)}>Cancel</LiquidButton>
            <LiquidButton
              tint={accent.primary}
              onClick={() => {
                onCreateFolder(folderName, folderId, folderColor, folderEmoji);
                setNewFolderOpen(false);
              }}
            >
              Save
            </LiquidButton>
          </div>
        </div>
      </LiquidModal>

      <LiquidModal open={importModalOpen} onClose={() => setImportModalOpen(false)} maxWidthClass="max-w-xl">
        <div className="flex max-h-[88vh] flex-col">
          <div className="flex items-center justify-between border-b border-black/8 px-6 py-4 dark:border-white/10">
            <div>
              <h2 className="text-base font-bold">Import Notes from Other Apps</h2>
              <p className="text-xs text-[#5A5F58] dark:text-[#B1AC9F]">
                Markdown, Notion, Obsidian, Google Keep, Apple Notes, HTML, PDF &amp; Images
              </p>
            </div>
            <LiquidButton onClick={() => setImportModalOpen(false)} className="!h-9 !w-9 !p-0">
              <X className="h-4 w-4" />
            </LiquidButton>
          </div>

          <div className="border-b border-black/8 px-6 py-2.5 dark:border-white/10">
            <LiquidSegmentedControl<'files' | 'paste'>
              value={importTab}
              onChange={setImportTab}
              accentColor={accent.primary}
              options={[
                { value: 'files', label: 'Files & Backups' },
                { value: 'paste', label: 'Paste from App' },
              ]}
            />
          </div>

          <div className="flex-1 overflow-y-auto p-6">
            {importTab === 'files' ? (
              <div
                onDragOver={(e) => e.preventDefault()}
                onDrop={(e) => {
                  e.preventDefault();
                  if (e.dataTransfer.files?.length) handleImportFilesList(e.dataTransfer.files);
                }}
                className="liquid-glass-frost flex flex-col items-center justify-center rounded-2xl border-2 border-dashed border-black/15 px-6 py-10 text-center dark:border-white/15"
              >
                <Upload className="mb-3 h-8 w-8 text-[#24483F] dark:text-[#8CC1AC]" />
                <p className="text-sm font-bold">
                  {importBusy ? 'Parsing imported notes...' : 'Drop files here or browse'}
                </p>
                <p className="mt-1 max-w-sm text-xs text-[#5A5F58] dark:text-[#B1AC9F]">
                  Supports .md, .txt, .html, .csv, Google Keep .json, Pro Notes backups, PDFs, and images.
                </p>
                <label
                  className="mt-4 inline-flex cursor-pointer items-center gap-2 rounded-full px-4 py-2 text-xs font-semibold text-white shadow-sm"
                  style={{ backgroundColor: accent.primary }}
                >
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
                <LiquidSegmentedControl<'Notion' | 'Obsidian' | 'Apple Notes' | 'Google Keep'>
                  value={pasteAppPreset}
                  onChange={setPasteAppPreset}
                  accentColor={accent.primary}
                  options={[
                    { value: 'Notion', label: 'Notion' },
                    { value: 'Obsidian', label: 'Obsidian' },
                    { value: 'Apple Notes', label: 'Apple Notes' },
                    { value: 'Google Keep', label: 'Keep' },
                  ]}
                />
                <LiquidInput
                  type="text"
                  value={pasteTitle}
                  onChange={(e) => setPasteTitle(e.target.value)}
                  placeholder={`Notebook title (e.g. ${pasteAppPreset} Lecture Notes)`}
                />
                <textarea
                  value={pasteBody}
                  onChange={(e) => setPasteBody(e.target.value)}
                  rows={8}
                  placeholder="Paste Markdown headings (#, ##), Obsidian [[links]], checklists ([ ] / [x]), callouts (> quote), or plain text..."
                  className="liquid-glass-frost w-full rounded-2xl px-3.5 py-2.5 text-xs leading-relaxed outline-none"
                />
                {pastePreview && (
                  <LiquidGlassSurface
                    enabled={settings.liquidGlass}
                    variant="frost"
                    radius={14}
                    className="flex items-center justify-between px-3.5 py-2 text-xs"
                  >
                    <span className="font-semibold text-[#24483F] dark:text-[#8CC1AC]">Preview ready</span>
                    <span className="font-mono-num">
                      {pastePreview.blockCount} blocks · {pastePreview.pages.length} A4 page
                      {pastePreview.pages.length === 1 ? '' : 's'}
                    </span>
                  </LiquidGlassSurface>
                )}
                <div className="flex justify-end gap-2 pt-1">
                  <LiquidButton onClick={() => setImportModalOpen(false)}>Cancel</LiquidButton>
                  <LiquidButton disabled={!pastePreview} onClick={handleConfirmPasteImport} tint={accent.primary}>
                    Import Notebook
                  </LiquidButton>
                </div>
              </div>
            )}
          </div>
        </div>
      </LiquidModal>
    </div>
  );
};
