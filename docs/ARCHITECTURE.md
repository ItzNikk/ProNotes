# Pro Notes Architecture

Pro Notes is a native Android application built with Kotlin, Jetpack Compose, Material 3, and Jetpack Ink.

## Repository Layout

| Path | Purpose |
|---|---|
| `app/` | Primary Android application module (`com.pronotes.app`) |
| `benchmark/` | Baseline profile and startup/scroll macrobenchmark module |
| `docs/` | Architecture documentation and README screenshots |
| `.github/workflows/` | CI APK build and GitHub Release automation |
| `scripts/` | Local preview server for workspace builds |

## Application Module (`com.pronotes.app`)

### Top-Level Entry Points & System Components
- `MainActivity.kt`: Single-activity Compose host managing navigation (`library`, `books`, `reader/{id}`, `editor/{id}`, `settings`, `support`) and inbound `ACTION_VIEW` / `ACTION_SEND` file imports.
- `ProNotesApplication.kt`: Application class configuring process-wide initialization.
- `UpdateCheckReceiver.kt`, `UpdatedReceiver.kt`, `InstallResultReceiver.kt`: Background release-check scheduling, package-replacement handling, and `PackageInstaller` status callbacks.

### `com.pronotes.app.data`
- `Models.kt`: Core library data models (`LibraryIndex`, `NotebookMeta`, `Folder`, `PaperStyle`).
- `Repository.kt`: Local filesystem persistence for notebooks, incremental page saves (`pages.index.json`, per-page `.json` files), thumbnails, PDF imports, and backup/restore archives.
- `OfficeDocumentImporter.kt`: Native parser and page generator for `.docx`, `.pptx`, `.xlsx`, `.csv`, `.epub`, and rich text documents.
- `Settings.kt`: App and per-stylus settings store (`AppSettings`, `SettingsStore`, `PenProfile`, `EraserFilter`, `LassoFilter`, `UiColorScheme`).
- `Study.kt`: Flashcards (`Flashcard`), ink links (`InkLink`), and SlideShow (`SlideCard`) models and `study.json` store.
- `AtomicFiles.kt`: Temp-file write-and-rename helper preventing partial writes on crash.
- `CloudSync.kt`: Cross-device sync and Live Study Room transport (zero-key public relay by default, optional Firestore REST configuration via build properties).
- `Updater.kt`, `UpdateDownloadService.kt`: Resumable background APK downloader and GitHub Releases checker.

### `com.pronotes.app.editor`
- `EditorScreen.kt`: Editor screen scaffold, canvas hosting, dock snap edge preview rails, and export/page dialogs.
- `EditorDialogs.kt`: Extracted editor modal and overlay sheets (`MindNodeDialog`, `StickerSheet`, `TextDialog`, `KeyboardNoteModeOverlay`, `StudyTimer`, `WebLinkDialog`).
- `EditorPanels.kt`: Notebook tab strip (`TabStrip`), workspace header bar (`TopBar`), in-note search bar (`SearchBar`), reorderable page thumbnail drawer (`PagesPanel`), and Write Box studio bar (`WriteBoxStudioBar`).
- `SplitViewPanel.kt`: Side-by-side Split View secondary notebook panel with independent page rendering and reduced-motion loading support.
- `EditorToolbar.kt`: Studio-grade adaptive fixed-bar (`Top`, `Bottom`, `Left`, `Right`) and floating-pill toolbars driven by `ToolbarState`, grouped History / Tool Tray / Preset capsules, horizontal and vertical drag-to-reorder, overflow tool menu, and shared swatch/size controls.
- `ToolSettingsSheets.kt`: Dedicated per-tool configuration sheets (`Pen`, `Highlighter`, `Eraser`, `Shapes`, `Selection (Lasso)`, `Text and Write Box`, `Sticker`, `Image`, `Masking tape`, `Sticky note`, `Link Previews`, `Concept Maps`, `Laser pointer`).
- `EditorOverlays.kt`: Clamped contextual overlays for `Selection (Lasso)` actions (including ink-to-text conversion), native anchored `Popup` stylus `Quick Actions`, and empty-page `Quick Insert`.
- `EditorViewModel.kt`: Notebook state, undo/redo command history, page lifecycle, search, Concept Map auto-layout, Flashcard extraction, ink-to-text selection conversion, and Split View coordination.
- `NoteCanvasView.kt`: Android `View` canvas integrating `InProgressStrokesView` (Jetpack Ink) and `MotionEventPredictor`, handling pen/finger input, palm rejection, zoom/pan, scribble-to-erase, hold-to-shape, circle-to-select, and empty-page long-press `Quick Insert`.
- `PageModel.kt`: Runtime canvas item and stroke models (`Page`, `InkStroke`, `TextItem`, `WriteBoxItem`, `StickyItem`, `ImageItem`, `TapeItem`, `WebLinkItem`, `MindNodeItem`).
- `InkCodec.kt`: Binary/JSON serialisation codec preserving backward compatibility with stored notebooks and strokes, plus per-tool stroke opacity helpers.
- `PageRenderer.kt`: High-resolution page background, PDF tile, ink stroke, shape, sticky, Write Box, and Concept Map renderer with LRU bitmap caching.
- `Geometry.kt`: Geometric shape recogniser, closed-loop and scribble detectors, and path hit-testing helpers.
- `PdfSearch.kt`: PdfBox-Android text extraction and bounding-box search highlighter.
- `PaperFeel.kt`: Synthesised pen/paper acoustic feedback and haptic triggers.
- `Stickers.kt`, `TapePatterns.kt`, `ToolbarOrder.kt`: Built-in sticker assets, masking tape patterns, toolbar ordering defaults, single-pass adaptive toolbar layout, and bounds-clamping math.
- `StudyKit.kt`, `StudyUi.kt`, `SlideShowUi.kt`, `CollabUi.kt`: Flashcard review, ink linking, SlideShow presentation, and Live Study Room UI overlays.

### `com.pronotes.app.library`
- `LibraryScreen.kt`: Notebook, folder, and tag browser with search, trash recovery, import/export workflows, 48 dp touch targets, and `LocalReduceMotion` transition compliance.
- `LibraryDialogs.kt`: Extracted notebook creation, folder, cover gallery, tag management, rename dialogs, and PDF import loading overlays.
- `Covers.kt`: Notebook cover renderers (`solid`, `pattern`, `image`, `thumb`) and paper template previews.
- `DragDrop.kt`: Cross-platform finger and stylus drag-and-drop helpers.

### `com.pronotes.app.reader`
- `BookReaderArea.kt`: Dedicated Reader Mode hub (`BookReaderHubScreen`) and page-turn reader (`BookReaderScreen`) with Paper/Sepia/Night themes, tablet spread layout, `LocalReduceMotion` instant-turn support, and localised 48 dp accessibility controls.
- `BookReaderCanvas.kt`: Extracted Reader Mode page-curl physics stage (`BookCurlStage`), curved sheet clip renderer, page sheet surface (`BookSheetFrame`), shimmer-aware page loader preview, and synthesised paper-turn audio engine (`BookFlipSoundEngine`).

### `com.pronotes.app.settings` & `com.pronotes.app.support`
- `SettingsScreen.kt`: Categorised settings screens for Stylus, Gestures, Workspace, Appearance (including Classic appearance and Reduced effects toggles), Backup, and About/Updates.
- `SupportScreen.kt`: Project highlights, sharing actions, optional developer UPI contribution, 48 dp Back navigation target, and `LocalReduceMotion` compliance on appreciation feedback.

### `com.pronotes.app.ui`
- `Theme.kt`: Material 3 theme builder (`ProNotesTheme`), typography presets, `UiColorScheme` palette mapping, and composition locals for Classic appearance, reduced effects, and reduced motion.
- `ProSurfaces.kt`, `Components.kt`: Shared surface, card, pill, chip, slider, dialog, bottom-sheet components, and Android 12+ backdrop blur modifier with solid-surface fallback.
- `ToolIcons.kt`: Vector path icon definitions (`ProIcon`, `ToolGlyph`) with adaptive ink-contrast borders.
- `MovingLetters.kt`: Kinetic text reveal animations (`ScaleInAnimatedText`, `FadeAnimatedText`).
- `UpdateDialog.kt`: Release-notes and download-progress dialog for in-app updates.
