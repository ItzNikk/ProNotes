# Changelog

All notable changes to Pro Notes are documented in this file.

## v13.2.0 — 2026-11-18

### New
- **Unified Toolbar State (`ToolbarState`):** Persists dock edge (`Top`, `Bottom`, `Left`, `Right`, `Floating`), toolbar style (`Fixed bar`, `Floating pill`, `Compact`), custom tool order, visible tools, and per-tool defaults (`ToolDefaults`).
- **Per-Tool Settings Sheets:** Added dedicated configuration sheets for Pen, Highlighter, Eraser (with custom radius slider and targets), Shapes (stroke/fill opacity and fill swatches), Selection (Lasso), Text and Write Box, Sticker, Image, Masking tape, Sticky note, Link Previews, Concept Maps, and Laser pointer.
- **Quick Insert & Quick Actions:** Added long-press `Quick Insert` on empty canvas pages (Write Box, Text box, Sticky note, Link Preview, Image, Sticker, Paste) and upgraded the stylus button radial menu to `Quick Actions` with Undo/Redo and Escape key dismissal.
- **Ink-to-Text Selection Action:** Added `Convert to text` directly inside the `Selection (Lasso)` action bar and Selection settings sheet.
- **Appearance Switches:** Added `Classic appearance` (disables blur and translucent surface layers) and `Reduce glow and shadow effects` toggles under Look settings, backed by Android 12+ `RenderEffect` backdrop blur with solid translucent fallback on older versions.

### Improved
- **Fixed Top Bar by Default:** Made the fixed top bar the default toolbar layout with the colour and size strip docked cleanly beside/below the main bar so it never overlaps the canvas, while removing redundant duplicate controls from the notebook header bar.
- **Single-Pass Adaptive Toolbar Layout & Clamping:** Automatically recomputes toolbar size, orientation, overflow drawer (`More tools`), popover caret side, and viewport clamping across dock changes, window resizes, rotation, and split-screen changes so floating bars and menus stay onscreen and clear of system bars.
- **Modular Editor Architecture:** Extracted `EditorToolbar.kt`, `ToolSettingsSheets.kt`, and `EditorOverlays.kt` from `EditorScreen.kt`.
- **Accessibility & Motion Compliance:** Enforced 48 dp minimum touch targets, icon content descriptions, keyboard focusability, high-contrast borders, and system remove-animations (`LocalReduceMotion`) checks across editor toolbars and overlays.

## v13.1.0 — 2026-11-18

### New
- Added `GestureConfig` and unified `CircleDetector` state machine for consistent hold times, slop thresholds, loop closing ratios, and scribble sensitivity.
- Added `'Scribble erased'` undo snackbar and `'Shape not recognised'` hint when freehand shape fitting finds no supported geometry.
- Added recorded-point unit tests covering `isClosedLoop`, `isScribble`, `ShapeRecognizer`, `GestureConfig`, and `CircleDetector`.
- Added `docs/ARCHITECTURE.md` documenting the `app` and `benchmark` module structure and `com.pronotes.app` packages.

### Improved
- Updated the Shape tool to snap on hold regardless of the global hold-to-shape toggle, attempt nearest-shape fitting on lift, and keep unrecognised strokes editable.
- Unified live and post-lift circle-to-lasso detection onto one closing rule and short window (`1.8s`), eliminating async `itemId` race drops and replaying initial pointer-down events so no initial stroke segment is lost.
- Enforced one shared eraser-policy check across live and lift-time scribble erase, increased minimum scribble lengths, and differentiated `Gentle`, `Balanced`, and `Instant` sensitivity thresholds.
- Made pen-up and cancel exits symmetrical across hold timers, wet strokes, hover dots, and eraser overlays, passing the real last `MotionEvent` and adding a debug-only surviving-stroke banner assertion.
- Consolidated `app/build.gradle.kts` lint configuration into a single block and aligned release versioning metadata.
- Standardised internal and user-facing feature terminology (`Write Box`, `Reader Mode`, `Keyboard Mode`, `Concept Maps`, `Selection (Lasso)`) across UI copy and repository documentation.
- Updated `NOTICE` third-party attributions to reflect only libraries compiled in the current build.

### Fixed
- Removed unused Kotlin imports, dead helper declarations, unused `material3-adaptive-navigation-suite` dependency, and unused `welcome.mp4` media asset.

### Removed
- Removed stale scratch directories, duplicate backup source trees, unused update scripts, and unused root configuration files from the repository.

## v13.0.0

### New
- **Write Box and Keyboard Mode:** Added on-canvas `WriteBox` ruled pads for handwriting-to-text conversion alongside full-page typed `Keyboard Mode` with headings, checklists, lists, and font controls.
- **Granular Eraser Filters:** Added per-category eraser targets (`Pen ink`, `Highlighter`, `Shapes`, `Text and Write Boxes`, `Sticky notes and tape`, `Images`) plus quick page clear actions.
- **Eight UI Colour Schemes:** Added switchable themes (`Slate Studio`, `Warm Ivory`, `Ocean Breeze`, `Forest Sage`, `Rose Quartz`, `Midnight OLED`, `Nordic Slate`, `Solar Amber`).
- **Multi-format Document Importer:** Added native import support for Word (`.docx`, `.doc`, `.odt`, `.rtf`), PowerPoint (`.pptx`, `.ppt`, `.odp`), spreadsheets (`.xlsx`, `.xls`, `.ods`, `.csv`, `.tsv`), and EPUB files with page-loading previews.
- **Welcome Notebook:** Added an interactive first-launch starter notebook and flashcard set demonstrating core tools.

### Improved
- **Reader Mode Physics and Audio:** Added curved corner-peel rendering, spine shadows, specular highlights, backside bleed-through, and optional synthesised paper-turn audio (`BookFlipSoundEngine`).
- **Toolbar Docking:** Fixed vertical `Left` and `Right` docked toolbars to keep consistent rail spacing without clipping sub-tool panels.
- **Adaptive Icon Borders:** Updated ink-filled tool icons and colour swatches to compute luminance-aware contrast borders on light and dark backgrounds.
- **Flashcards Studio:** Added note-to-flashcard extraction, multiple-choice quiz review, starred filtering, hints, and deck shuffle.

## v12.0.0

### New
- **Reader Mode:** Added a distraction-free book reading workspace with Paper, Sepia, and Night themes, single-page and two-page tablet spreads, page-turn animations, and a bottom progress scrubber.
- **Kinetic Typography (`MovingLetters`):** Added character reveal animations for headings and study prompts, respecting system motion preferences.

### Improved
- **Workspace Chrome:** Redesigned connected notebook tabs, Undo/Redo controls, floating/dockable toolbars, and anchored tool popovers on solid high-contrast Material 3 surfaces with customisable corner roundness (`8dp`–`32dp`).
- **Native Focus:** Streamlined the codebase around local handwriting, PDF annotation, Flashcards, Concept Maps, SlideShow, ink links, Live Study Rooms, and Split View.

## v11.0.0

### Improved
- **Library Search:** Integrated direct search on the main library bar with real-time in-note and in-PDF indexing.
- **Scribble-to-Erase Filter:** Tuned motion filtering to reduce accidental erasures while keeping deliberate zig-zag erasures responsive.
- **Settings Layout:** Reorganised settings into categorised cards with unified stylus profile management and Google Account sync.
- **Direct Launch:** Removed the initial setup gate so Pro Notes opens directly into the notebook library.

## v10.0.0

### New
- **Live Study Rooms and Cloud Sync:** Added cross-device notebook/settings sync and room-code collaboration backed by an optional zero-secret relay or custom Firestore REST configuration.
- **Pomodoro Timer:** Added Pomodoro countdown mode to the in-editor Study Timer.

### Improved
- **Canvas Item Transforms:** Added 8-handle X/Y scaling and rotation across ink strokes, shapes, text boxes, sticky notes, images, stickers, tape, concept nodes, and web link cards.
- **Split View Editing:** Added Read/Write switching with compact Pen/Highlighter/Eraser controls and expanded zoom-out floor to `35%`.

## v9.11.0

### New
- **Resumable In-App Updater:** Added HTTP `Range` resume support, `.part` download persistence, ETag/length checks, and background notification controls.
- **SlideShow Presenter:** Added multi-theme study slides (`Cover`, `Concept`, `Formula`, `Check Q&A`, `Recap`) with flip Q&A cards and manual slide editing.
- **Web Link Cards:** Added native `WebLink` items on the canvas with preview cards and safe external launch.

### Improved
- **Split View Layout:** Added scroll-direction-aware pane orientation, page sync lock, pane swap, pinch zoom presets, and in-split keyword search.
- **Concept Maps:** Added viewport margin clamping on regular pages and a one-tap `Fit Map` action.
- **Sticky Notes:** Added dedicated titles, four card styles (`Classic`, `Lined`, `Glass`, `Outline`), and direct canvas checklist toggles.
- **Customisation:** Added font choices (`Modern Sans`, `Editorial Serif`, `Studio Mono`, `Soft Rounded`), motion profiles, and stylus hover tooltips.

## v9.7.1

### New
- **Structured Text Import:** Added paste and file import for Markdown, HTML, CSV, TXT, PDF, and images.

### Improved
- **Concept Maps Studio:** Added tapered Bezier connectors, root halos, tag badges, descendant counts, subtree dragging, three auto-layouts (`Radial`, `Tree`, `Org Chart`), and outline/flashcard export.
- **Scribble Erase Feedback:** Added crumbling particle animations on scribble-erase and page ink clear actions.
- **Updater Handoff:** Improved foreground Activity handoff for Android `PackageInstaller`.

## v9.6.0

### New
- **Live Scribble-to-Erase:** Added real-time scribble detection while the stylus is still down.
- **Infinite Whiteboard Pages:** Added unbounded pannable and zoomable whiteboard pages with dot/grid backgrounds and origin reset.
- **Concept Mapping Tool:** Added node-and-branch diagramming directly in the editor toolbar.

### Improved
- **Notebook Creation Previews:** Highlighted A4 as the default page size with live paper pattern, tint, and orientation previews.

## v9.5.0

### Improved
- **Top Bar Layout:** Made fixed top-bar tool controls the default layout.
- **Hover Cursor Reliability:** Prevented stale pen hover dots and unfinished strokes from remaining on screen.
- **Shape and Selection Gestures:** Improved rough-shape snapping on lift and hold-to-lasso loop detection.

## v9.4.0

### New
- **Split View:** Added side-by-side notebook viewing and scrolling from the editor menu.
- **Audio Feedback:** Added velocity-responsive writing sounds, highlighter glide, eraser audio, and undo/redo sound cues.

## v9.0.0

### New
- **Ink Links Management:** Added an `Unlink` action for selected linked ink and improved long-press link removal.

### Improved
- **Page Stability:** Fixed unexpected scroll jumps when toolbars collapse or the soft keyboard opens.
- **Eraser Safety:** Restricted scribble-to-erase to pen ink and text strokes (never images, tape, or stickies).

## v8.0.0

### New
- **Expanded Paper Templates:** Added 25 templates organised across `Writing`, `Grids`, `Planning`, `Study`, and `Music`, with configurable line spacing, line colour, and paper colour.
- **Patterned Tape:** Added 10 masking tape patterns alongside custom image tape.
- **PDF Text Selection:** Added passage highlighting, underlining, strikethrough, search, and flashcard capture.

## v7.0.0

### New
- **Handwriting Recognition and Search:** Added offline handwriting-to-text conversion and indexed handwriting search across notebooks.
- **Flashcards Studio:** Added page-region flashcard capture, Leitner spaced repetition, source-page jump-back, and CSV export.

### Improved
- **Large PDF Rendering:** Implemented fast low-res first-pass PDF page rendering followed by sharp tile/zoom refinement and prefetching.
- **Paper-Feel Haptics:** Added tactile stylus touchdown and snap feedback using the `VIBRATE` permission.

## v6.0.0

### New
- **Ink Links:** Added cross-page link badges connecting any two points or boxes in a notebook.
- **Study Timer:** Added a draggable session stopwatch with auto-start and idle pause options.

## v5.0.0

### New
- **Round-Tip Highlighter:** Added a consistent-width highlighter ignoring pressure and tilt with adjustable opacity (`20%`–`80%`).
- **Notebook Memory:** Remembered last used tool, pen style, colours, sizes, and page index per notebook.

## v4.0.0

### New
- **Core Release:** Introduced pen profiles, per-pen shortcut wheels, custom page sizes, covers, backup/restore, and Apache-2.0 licensing with `NOTICE`.
