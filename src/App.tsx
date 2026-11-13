import React, { useEffect, useState } from 'react';
import { useProNotesStore } from './store/useProNotesStore';
import { LibraryScreen } from './components/LibraryScreen';
import { EditorScreen } from './components/EditorScreen';
import { SettingsScreen } from './components/SettingsScreen';
import { LocalAiModal } from './components/StudyModals';
import { SetupWizardModal } from './components/SetupWizardModal';

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
  } = useProNotesStore();

  const [route, setRoute] = useState<{ screen: 'LIBRARY' | 'EDITOR' | 'SETTINGS'; noteId?: string }>({
    screen: 'LIBRARY',
  });
  const [modelsModalOpen, setModelsModalOpen] = useState(false);
  const [setupOpen, setSetupOpen] = useState(false);

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
          onDeleteForeverNotebook={deleteForeverNotebook}
          onEmptyTrash={emptyTrash}
          onOpenSettings={() => setRoute({ screen: 'SETTINGS' })}
          onOpenLocalModels={() => setModelsModalOpen(true)}
          onOpenSetupWizard={() => setSetupOpen(true)}
        />
      )}

      <LocalAiModal
        open={modelsModalOpen}
        modelStates={modelStates}
        activeModelId={state.settings.activeLocalModelId}
        onClose={() => setModelsModalOpen(false)}
        onDownload={startModelDownload}
        onPause={pauseModelDownload}
        onDelete={removeLocalModel}
        onActivate={(id) => updateSettings({ activeLocalModelId: id })}
      />

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
    </>
  );
}

export default App;

