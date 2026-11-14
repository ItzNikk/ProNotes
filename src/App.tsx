import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useProNotesStore } from './store/useProNotesStore';
import { LibraryScreen } from './components/LibraryScreen';
import { EditorScreen } from './components/EditorScreen';
import { SettingsScreen } from './components/SettingsScreen';
import { LocalAiModal } from './components/StudyModals';
import { SetupWizardModal } from './components/SetupWizardModal';
import { SupportModal } from './components/SupportBannerModal';
import { CollabRoomModal } from './components/CollabAndAiModals';
import {
  auth,
  onAuthStateChanged,
  signInWithGoogleAccount,
  signOutAccount,
  User,
} from './firebase';
import {
  createCollabRoom,
  deleteNotebookFromCloud,
  joinCollabRoom,
  pullFullCloudLibrary,
  pushLibraryListsToCloud,
  pushNotebookToCloud,
  pushSettingsToCloud,
  sendCollabMessage,
  subscribeCollabRoom,
  syncUserProfileToCloud,
  updateCollabCursor,
  upsertCollabItem,
} from './services/cloudSync';
import {
  CollabChatMessage,
  CollabItemDoc,
  CollabMember,
  CollabRoom,
  PageItem,
} from './types/models';

export function App() {
  const {
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
    startModelDownload,
    pauseModelDownload,
    removeLocalModel,
    exportBackupJson,
    importBackupJson,
    importParsedNotebook,
    appendPagesToNotebook,
    mergeCloudLibrary,
  } = useProNotesStore();

  const [route, setRoute] = useState<{ screen: 'LIBRARY' | 'EDITOR' | 'SETTINGS'; noteId?: string }>({
    screen: 'LIBRARY',
  });
  const [modelsModalOpen, setModelsModalOpen] = useState(false);
  const [setupOpen, setSetupOpen] = useState(false);

  // Google Auth & Cloud Sync state
  const [firebaseUser, setFirebaseUser] = useState<User | null>(() => auth.currentUser);
  const [syncBusy, setSyncBusy] = useState(false);
  const [lastSyncAt, setLastSyncAt] = useState<number | null>(null);
  const lastCloudDigestRef = useRef<string>('');
  const latestStateRef = useRef(state);
  latestStateRef.current = state;

  // Support banner & modal (always enabled cleanly; dismissible / hideable)
  const [supportModalOpen, setSupportModalOpen] = useState(false);
  const [supportBannerOpen, setSupportBannerOpen] = useState<boolean>(() => {
    if (!state.settings.supportReminders) return false;
    const now = Date.now();
    const last = state.settings.lastSupportShownAt || 0;
    return now - last > 24 * 60 * 60 * 1000;
  });

  // Live Collaborative Study Room state
  const [libraryCollabOpen, setLibraryCollabOpen] = useState(false);
  const [activeCollabRoom, setActiveCollabRoom] = useState<CollabRoom | null>(null);
  const [collabMembers, setCollabMembers] = useState<CollabMember[]>([]);
  const [collabItems, setCollabItems] = useState<CollabItemDoc[]>([]);
  const [collabMessages, setCollabMessages] = useState<CollabChatMessage[]>([]);

  // Sync light/dark theme class + Ultra typography & motion style on <html>
  useEffect(() => {
    const applyTheme = () => {
      const m = state.settings.themeMode;
      const dark =
        m === 'DARK' ||
        (m === 'SYSTEM' && window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches);
      document.documentElement.classList.toggle('dark', dark);
    };
    applyTheme();
    document.documentElement.dataset.font = state.settings.appFont;
    document.documentElement.dataset.motion = state.settings.motionStyle;
    const mq = window.matchMedia?.('(prefers-color-scheme: dark)');
    mq?.addEventListener?.('change', applyTheme);
    return () => mq?.removeEventListener?.('change', applyTheme);
  }, [state.settings.themeMode, state.settings.appFont, state.settings.motionStyle]);

  const handleSyncNow = useCallback(
    async (explicitUser?: User | null) => {
      const user = explicitUser ?? firebaseUser;
      if (!user) return;
      setSyncBusy(true);
      try {
        await syncUserProfileToCloud(user);
        const cloud = await pullFullCloudLibrary(user.uid);
        mergeCloudLibrary(cloud);

        const cur = latestStateRef.current;
        await Promise.all([
          pushSettingsToCloud(user.uid, cur.settings),
          pushLibraryListsToCloud(user.uid, cur.folders, cur.flashcards),
          ...cur.notebooks.slice(0, 24).map((nb) => pushNotebookToCloud(user.uid, nb)),
        ]);
        setLastSyncAt(Date.now());
      } catch (err) {
        console.warn('Cloud sync skipped/deferred:', err);
      } finally {
        setSyncBusy(false);
      }
    },
    [firebaseUser, mergeCloudLibrary]
  );

  // Firebase Auth listener -> automatic pull & sync on login
  useEffect(() => {
    const unsub = onAuthStateChanged(auth, (user) => {
      setFirebaseUser(user);
      if (user) {
        void handleSyncNow(user);
      }
    });
    return unsub;
  }, [handleSyncNow]);

  // Debounced background cloud push when authenticated and local workspace changes
  useEffect(() => {
    if (!firebaseUser) return;
    const digest = JSON.stringify({
      s: state.settings,
      f: state.folders.length,
      fc: state.flashcards.length,
      nbs: state.notebooks.map((n) => `${n.id}:${n.modified}:${n.deletedAt ?? ''}`),
    });
    if (digest === lastCloudDigestRef.current) return;
    const timer = window.setTimeout(async () => {
      lastCloudDigestRef.current = digest;
      try {
        await Promise.all([
          pushSettingsToCloud(firebaseUser.uid, state.settings),
          pushLibraryListsToCloud(firebaseUser.uid, state.folders, state.flashcards),
          ...state.notebooks.slice(0, 12).map((nb) => pushNotebookToCloud(firebaseUser.uid, nb)),
        ]);
        setLastSyncAt(Date.now());
      } catch (err) {
        console.warn('Background cloud push error:', err);
      }
    }, 2200);
    return () => window.clearTimeout(timer);
  }, [firebaseUser, state.settings, state.folders, state.flashcards, state.notebooks]);

  // Subscribe to live Firestore collaborative room when joined
  useEffect(() => {
    if (!activeCollabRoom) {
      setCollabMembers([]);
      setCollabItems([]);
      setCollabMessages([]);
      return;
    }
    const unsub = subscribeCollabRoom(activeCollabRoom.roomId, {
      onMembers: setCollabMembers,
      onItems: setCollabItems,
      onMessages: setCollabMessages,
    });
    return unsub;
  }, [activeCollabRoom]);

  const handleSignInGoogle = useCallback(async () => {
    const user = await signInWithGoogleAccount();
    setFirebaseUser(user);
    await handleSyncNow(user);
  }, [handleSyncNow]);

  const handleSignOut = useCallback(async () => {
    setActiveCollabRoom(null);
    await signOutAccount();
    setFirebaseUser(null);
  }, []);

  const handleCreateCollabRoom = useCallback(
    async (title: string) => {
      const user = firebaseUser ?? (await signInWithGoogleAccount());
      setFirebaseUser(user);
      const room = await createCollabRoom(user, title);
      setActiveCollabRoom(room);
    },
    [firebaseUser]
  );

  const handleJoinCollabRoom = useCallback(
    async (code: string) => {
      const user = firebaseUser ?? (await signInWithGoogleAccount());
      setFirebaseUser(user);
      const room = await joinCollabRoom(user, code);
      setActiveCollabRoom(room);
    },
    [firebaseUser]
  );

  const handleSendCollabMessage = useCallback(
    async (text: string) => {
      if (!firebaseUser || !activeCollabRoom) return;
      await sendCollabMessage(activeCollabRoom.roomId, firebaseUser, text);
    },
    [firebaseUser, activeCollabRoom]
  );

  const handleBroadcastCollabItem = useCallback(
    (item: PageItem, pageIdx: number) => {
      if (!firebaseUser || !activeCollabRoom) return;
      void upsertCollabItem(activeCollabRoom.roomId, firebaseUser, {
        id: item.id,
        kind: item.kind === 'ink' || item.kind === 'fill' ? 'stroke' : item.kind,
        pageIdx,
        data: item as unknown as Record<string, unknown>,
      });
    },
    [firebaseUser, activeCollabRoom]
  );

  const handleBroadcastCollabCursor = useCallback(
    (x: number, y: number, pageIdx: number) => {
      if (!firebaseUser || !activeCollabRoom) return;
      void updateCollabCursor(activeCollabRoom.roomId, firebaseUser, x, y, pageIdx);
    },
    [firebaseUser, activeCollabRoom]
  );

  const openNote = (id: string) => {
    openNotebookTab(id);
    setRoute({ screen: 'EDITOR', noteId: id });
  };

  const activeNotebook =
    route.screen === 'EDITOR' && route.noteId
      ? state.notebooks.find((n) => n.id === route.noteId && n.deletedAt === null)
      : undefined;

  return (
    <>
      {route.screen === 'EDITOR' && activeNotebook ? (
        <EditorScreen
          notebook={activeNotebook}
          allNotebooks={state.notebooks}
          openTabIds={state.tabs}
          settings={state.settings}
          flashcards={state.flashcards.filter((c) => c.notebookId === activeNotebook.id)}
          bestQuizScore={state.bestQuizScores[activeNotebook.id] || 0}
          modelStates={modelStates}
          isSignedIn={Boolean(firebaseUser)}
          activeCollabRoom={activeCollabRoom}
          collabMembers={collabMembers}
          collabItems={collabItems}
          collabMessages={collabMessages}
          onSignInGoogle={handleSignInGoogle}
          onCreateCollabRoom={handleCreateCollabRoom}
          onJoinCollabRoom={handleJoinCollabRoom}
          onLeaveCollabRoom={() => setActiveCollabRoom(null)}
          onSendCollabMessage={handleSendCollabMessage}
          onBroadcastCollabItem={handleBroadcastCollabItem}
          onBroadcastCollabCursor={handleBroadcastCollabCursor}
          onBackHome={() => setRoute({ screen: 'LIBRARY' })}
          onSwitchNote={openNote}
          onCloseTab={(id) => {
            closeTab(id);
            if (id === activeNotebook.id) {
              const rem = state.tabs.filter((t) => t !== id);
              if (rem.length > 0) setRoute({ screen: 'EDITOR', noteId: rem[rem.length - 1] });
              else setRoute({ screen: 'LIBRARY' });
            }
          }}
          onUpdateNotebook={updateNotebook}
          onUpdateSlides={(slides) => updateNotebookSlides(activeNotebook.id, slides)}
          onAppendPages={(pages) => appendPagesToNotebook(activeNotebook.id, pages)}
          onUpdateSettings={updateSettings}
          onSavePenPreset={savePenPreset}
          onDeletePenPreset={deletePenPreset}
          onAddFlashcards={addFlashcards}
          onUpdateFlashcard={updateFlashcard}
          onDeleteFlashcard={deleteFlashcard}
          onRecordQuizScore={recordQuizScore}
          onOpenSettings={() => setRoute({ screen: 'SETTINGS' })}
          onOpenLocalModels={() => setModelsModalOpen(true)}
        />
      ) : route.screen === 'SETTINGS' ? (
        <SettingsScreen
          settings={state.settings}
          userEmail={firebaseUser?.email}
          userName={firebaseUser?.displayName}
          syncBusy={syncBusy}
          lastSyncAt={lastSyncAt}
          onSignInGoogle={() => void handleSignInGoogle()}
          onSignOut={() => void handleSignOut()}
          onSyncNow={() => void handleSyncNow()}
          onOpenSupport={() => setSupportModalOpen(true)}
          onUpdate={updateSettings}
          onSavePenPreset={savePenPreset}
          onDeletePenPreset={deletePenPreset}
          onBack={() => setRoute({ screen: 'LIBRARY' })}
          onOpenLocalModels={() => setModelsModalOpen(true)}
          onOpenSetupWizard={() => setSetupOpen(true)}
          onExportBackup={exportBackupJson}
          onImportBackup={importBackupJson}
        />
      ) : (
        <LibraryScreen
          folders={state.folders}
          notebooks={state.notebooks}
          settings={state.settings}
          modelStates={modelStates}
          userEmail={firebaseUser?.email}
          userName={firebaseUser?.displayName}
          syncBusy={syncBusy}
          lastSyncAt={lastSyncAt}
          supportBannerOpen={supportBannerOpen}
          onSignInGoogle={() => void handleSignInGoogle()}
          onSyncNow={() => void handleSyncNow()}
          onOpenCollab={() => setLibraryCollabOpen(true)}
          onOpenSupport={() => setSupportModalOpen(true)}
          onDismissSupportBanner={() => {
            setSupportBannerOpen(false);
            updateSettings({ lastSupportShownAt: Date.now() });
          }}
          onOpenNote={openNote}
          onCreateNotebook={(payload) => {
            const nb = createNotebook({
              title: payload.title,
              folderId: payload.folderId,
              cover: payload.cover,
              coverColor: payload.coverColor,
              paper: payload.paper,
              width: payload.width,
              height: payload.height,
              paperColor: payload.paperColor,
              coverPage: payload.coverPage,
              isWhiteboard: payload.isWhiteboard,
              isMindMap: payload.isMindMap,
            });
            openNote(nb.id);
          }}
          onImportParsedNotebook={(title, pages, folderId) => {
            const nb = importParsedNotebook(title, pages, folderId);
            openNote(nb.id);
          }}
          onImportBackup={importBackupJson}
          onCreateFolder={createFolder}
          onUpdateFolder={updateFolder}
          onDeleteFolder={deleteFolder}
          onUpdateNotebook={updateNotebook}
          onDuplicateNotebook={duplicateNotebook}
          onSoftDeleteNotebook={softDeleteNotebook}
          onRestoreNotebook={restoreNotebook}
          onDeleteForeverNotebook={(id) => {
            deleteForeverNotebook(id);
            if (firebaseUser) {
              void deleteNotebookFromCloud(firebaseUser.uid, id);
            }
          }}
          onEmptyTrash={emptyTrash}
          onOpenSettings={() => setRoute({ screen: 'SETTINGS' })}
          onOpenLocalModels={() => setModelsModalOpen(true)}
          onOpenSetupWizard={() => setSetupOpen(true)}
        />
      )}

      <SetupWizardModal
        open={!state.settings.setupCompleted || setupOpen}
        settings={state.settings}
        modelStates={modelStates}
        onUpdateSettings={updateSettings}
        onDownloadModel={startModelDownload}
        onFinish={() => {
          updateSettings({ setupCompleted: true });
          setSetupOpen(false);
        }}
      />

      <SupportModal
        open={supportModalOpen}
        onClose={() => {
          setSupportModalOpen(false);
          updateSettings({ lastSupportShownAt: Date.now() });
        }}
        onNeverRemind={() => {
          setSupportBannerOpen(false);
          updateSettings({ supportReminders: false, lastSupportShownAt: Date.now() });
        }}
      />

      <CollabRoomModal
        open={libraryCollabOpen}
        isSignedIn={Boolean(firebaseUser)}
        activeRoom={activeCollabRoom}
        members={collabMembers}
        messages={collabMessages}
        onSignInGoogle={handleSignInGoogle}
        onCreateRoom={handleCreateCollabRoom}
        onJoinRoom={handleJoinCollabRoom}
        onLeaveRoom={() => setActiveCollabRoom(null)}
        onSendMessage={handleSendCollabMessage}
        onClose={() => setLibraryCollabOpen(false)}
      />
    </>
  );
}

export default App;


