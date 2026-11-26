# Changelog

All notable changes to Pro Notes are documented in this file.

## v18.0.0 - 2026-11-25

### What's changed

- Improved common shape recognition and snapping (`ShapeRecognizer` and `ShapeBuilder`): added uniform arc-length resampling, loop overshoot trimming, and head-to-head geometric template fitting across circles/ellipses, axis-aligned squares/rectangles, oriented rectangles, diamonds, and triangles so hand-drawn circles no longer collapse into irregular polygons and squares/rectangles snap crisply even when started mid-edge or slightly tilted, plus gentle aspect-ratio square/circle and 5° line axis snapping in Shape drag mode.
- Upgraded page swiping across both PDFs and Notes (`NoteCanvasView`, `NoteCanvasGestures`, `EditorViewModel`, and `BookReaderArea`): replaced the fixed 420ms snap with a velocity-aware smooth scale+position page-turn animator (no jumping between mixed-size PDF pages), added density-scaled flick detection and 22% swipe-turn thresholds, enabled elastic zoomed-in edge overswipe page turning so you can turn pages without zooming back out, added soft first-page rubber-band resistance, tuned vertical scroll friction, sped up ±2 PDF neighbor prefetching at 1080px width, and added velocity-aware quick-flick detection to Kindle Mode.
- Enabled direct **Lasso in PDF** (even with zero handwriting inside the lasso): lassoing any region on a PDF page opens a crisp floating action bar to **Cut** it straight into a movable clipped image (plus Copy image, Copy text, Highlight, and Save as Sticker); removed the flashcard-from-screen dialog (`"Flashcard from area"`, `"Make flashcard"`, and PDF `"Flashcard"` popup button) so area capture directly cuts the selected region.
- Reworked **Sticky Notes** 1:1 like Notein: folded paper-badge icon with corner crease, crisp expanded paper card with top header controls (`Pin/Lock`, `Edit`, `Fold`), direct bottom-right peel-corner drag resize, write-on-top and highlight-on-top support, Notein inline sticky editor (editable title, square swatches, paper templates, `+ Task` checklists, font/size controls, Text/Handwriting switch), and a searchable 2-column Notein Sticky Notes Manager (`StickyBoard`) with color filters, ink-stroke counts, and quick fold/pin toggles.
- Upgraded **Handwriting-to-Text** alignment to flow neatly in straight lines (merging onto same-line text items and snapping left margins/font size to paragraphs above instead of scattering floating boxes), fixed all 8 **Pen types** (`Fountain`, `Fineliner`, `Ballpoint`, `Lettering`, `Brush`, `Calligraphy`, `Pencil`, `Dashed`) to have genuinely distinct nib physics and rendering (graphite pencil tooth, painterly wet-edge brush, flat chisel calligraphy, directional thick-down/thin-up lettering, crisp monoline fineliner, ballpoint oil ink, tapered fountain), and upgraded the **Search Engine** with diacritic-insensitive, multi-word, OCR-confusable & fuzzy matching across Text, PDF, Sticky Notes, Mind Maps, and Handwriting with source filter chips and word-level ink highlights.
- Added full-device Android system clipboard support across the editor: Paste (from the top menu, long-press Quick Insert, and `Ctrl+V`) now pastes text, HTML, and images/screenshots copied from any app on the phone—automatically prioritizing the most recent external or in-app copy via Android clipboard change/timestamp tracking, wrapping pasted text cleanly to page margins, and mirroring in-app copies (text or rendered selection PNGs) back to the phone clipboard.
- Deeply upgraded the on-device handwriting recognizer (`HandwritingRecognizer`, `HandwritingLineLayout`, and `AutoSpellChecker`): added corner-preserving stylus denoising, multi-line reading-order segmentation with rolling context, optimal scale and padded `WritingArea` normalization for ML Kit Digital Ink, a dedicated geometric math & symbol classifier (`+`, `-`, `=`, `×`, `÷`, `±`, `≠`, `≤`, `≥`, `≈`, `√`, `∞`, `→`, `←`, `↑`, `↓`, `π`, `Δ`, `Σ`, `✓`, `%`, `#`, `$`, `₹`, `€`, `£`, `@`, `&`, brackets, punctuation), contextual digit/letter disambiguation, math-safe auto spell checking, and an expanded 90+ template offline engine covering `A–Z`, `a–z`, `0–9`, and symbols—including single digits and symbols in Auto-Text mode.
- Increased per-tool color slots up to **16** (with 8 richer default slots across Pen, Crayon, Highlighter, Shapes, Text, Tape, Sticky, Web Link, and Mind Map, plus 7 radial Quick Action swatches and 16 recent colors) and redesigned the color swatches and `ColorEditorDialog` into a crisp **square** studio palette with a 10×6 curated square color matrix, 7-step square shades & tints strip, square 2D saturation-brightness spectrum picker, and square hue bar.

- Fixed pages disappearing while writing: prevented cancelled debounced autosaves from consuming dirty-page or structure flags before disk commit, updated `Repository` to keep `note.json` in sync on structural changes and self-heal any missing per-page files from `note.json` instead of dropping pages, separated spatial-grid scratch buffers for highlighter pages in `PageRenderer`, and hardened `NoteCanvasView.visiblePageRange()` so active and visible pages never vanish during writing or mixed-size layouts.
- Made scrolling in the **New Notebook** creator dialog and Library buttery smooth: replaced `BoxWithConstraints` (`SubcomposeLayout`) in `NewNotebookDialog` with direct configuration width checks, rendered solid covers directly on the GPU without allocating bitmaps, fast-pathed blank paper previews, pre-warmed paper and textured cover thumbnails off the UI thread, and removed per-tile layout animation wrappers during Library scrolling.
- Added long-press on the Library `+` button to immediately create and open a new notebook with the user's saved default settings (default paper template, page size/whiteboard preset, orientation, cover preference, and current folder) with haptic feedback.

- Eliminated UI-wide scroll lag across Library, Settings, and Editor: wrapped scroll-offset thresholds in `derivedStateOf`, removed per-frame `onGloballyPositioned` state mutations from Library drag-and-drop tiles, memoized tile actions and date formatting, scoped page-index and lasso-selection overlays out of `EditorScreen` recomposition, switched canvas current-page lookup to zero-allocation binary search, and debounced zoomed PDF detail requests during active scrolling.
- Fixed buggy page loading and clipped elements caused by culling: guarded `visiblePageRange()` against unlaid-out views and added overscan margins so incoming pages, shadows, and PDFs load cleanly before entering the viewport; fixed `SpatialGrid` to bucket oversized items instead of truncating them, added empty-clip guards and generous overscan padding, expanded highlight `saveLayer` bounds to cover full brush width, and cleared spatial caches on memory trim.
- Styled lasso dashed outlines and transform handles in pure black and fixed the marching-ants animation to flow continuously without stopping, starting, or jumping (switched to `LinearInterpolator`, matched the exact `36dp` dash period, prevented mid-motion restarts, and restored the horizontal floating toolbar drag handle with fresh callbacks).

- Reverted the under-toolbar backdrop back to the original subtle soft glow (removing the aurora effect and continuous color-shift redraws) while keeping the progressive vertical blur.
- Added aggressive rendering culling across the canvas: cached page-space spatial-grid item culling for dense pages, tight viewport-culled highlight `saveLayer` blending, viewport culling for mind-map connectors, search/PDF highlights, PDF text selections, and off-screen page links, plus zero-allocation whiteboard backdrop rendering.
- Improved ink smoothness across all active and passive styluses with a sample-rate-independent One-Euro (`1€`) filter across all drawing tools (Pen, Crayon, Auto-Text, Highlighter, and Shapes), monotonic timestamp guarding, OEM hardware pressure/tilt calibration with soft liftoff taper, velocity-based fallback tapering for non-pressure styluses, and smooth quadratic Bezier crayon strokes.
- Improved button action smoothness and reliability by moving tactile press scaling strictly to the GPU `graphicsLayer` (zero Compose recompositions), removing the tap-stealing full-bar drag handler from the floating toolbar, adding dismiss-bounce protection and single-fire action guards to context/toolbar menus, and giving editor/overlay buttons consistent press feedback and generous touch targets.

- Refined the under-toolbar aurora glow into a single unified color across the full width that shifts gradually through the spectrum, with deeper blur (`26dp` on Android 12+) and a softer vertical fade.
- Fixed dragging lasso selections across pages in both vertical and horizontal scroll modes: added smooth frame-locked edge auto-scrolling when dragging near screen edges, automatic next-page creation when holding or dropping past the last page, reliable hold-and-scroll with a second finger, and clean destination-page placement.
- Preserved freeform lasso and circle-to-lasso selections in their exact drawn outline shape (with interior polygon hit-testing and smooth curved dashed outlines) instead of collapsing them into a rectangle.
- Unified app chrome bars (editor tab strip, top bar, fixed/floating toolbars, search bar, Write Box bar, floating canvas pills, Library top bar, Settings header, Kindle Mode header and reader scrubber bars, plus Android status/navigation bars) to pure black with crisp high-contrast light icons and text in both Light and Dark themes.

- Hardened broad Android 8.0–16.0 (`minSdk 26` to `targetSdk 36`) compatibility without removing any features: fixed an Android 13 `NoSuchMethodError` crash in the text box editor (`isAutoHandwritingEnabled` now properly gated at API 34+), isolated all Android 10–16 framework calls (`RenderNode`, `BlendMode`, `Activity.display`, `EditText.textCursorDrawable`/`isAutoHandwritingEnabled`, `VibratorManager`/`VibrationEffect` primitives, `RenderEffect`, Android 15 touch refresh-rate boost and screen-recording detection, and Android 16 promoted live alerts) into `@RequiresApi` helper objects for clean ART verification across older devices, verified all upgraded Compose/Haze/AndroidX AARs support `minSdk <= 23` with aligned Kotlin `2.2.21` stdlib, and added R8 ProGuard keep rules for Haze and Reorderable release stability.

- Upgraded Jetpack Compose BOM to `2026.06.01` (Compose `1.11.4` with Pausable Composition enabled by default in lazy prefetch + Material 3 `1.4.0`), `activity-compose:1.12.4`, `lifecycle-compose:2.10.0`, `navigation-compose:2.9.8`, and `haze:1.7.2` (matched to Kotlin `2.2.21` and `compileSdk 36` for smooth performance without ABI mismatches).
- Eliminated dropdown and notebook creation lag: replaced Material 3 `DropdownMenu`'s expensive `IntrinsicSize.Max` multi-pass layout with zero-jank spring-animated `Popup` menus (`LibraryCreateMenu` and `AppContextMenu`), virtualized the **New Notebook** paper and cover galleries with `LazyVerticalGrid`, removed `BoxWithConstraints` (`SubcomposeLayout`) from `CoverArt` and `PaperPreview`, added lock-free `ThreadLocal` parallel thumbnail rendering in `CoverCache`, isolated title input recompositions, and offloaded cover thumbnail PNG encoding off the notebook creation critical path.
- Upgraded `AnimatedGradientText` to flow smoothly from all directions (top, bottom, left, right, and diagonals) using a GPU-only 360° rotating 2D Lissajous gradient and opposing radial color washes.
- Enhanced the under-toolbar aurora glow with richly saturated, almost full-spectrum colors (crimson, tangerine, gold, neon lime/emerald, mint, cyan, electric blue, indigo, violet, and hot magenta) flowing smoothly while keeping existing blur effects unchanged.

- Reworked the lasso **Resize & Rotate** toolbar icon and on-canvas transform handles with an elevated rotation handle (curved arrow and arrowhead), corner resize cues, edge stretch pills, generous touch targets, and active drag highlights.
- Eliminated notebook screen lag by deferring pen-active overlay fades to the GPU draw layer, isolating library autosaves from recomposing the editor, scoping zoom/glow/page-indicator overlays, enabling unbuffered touch dispatch for scrolling, and removing per-frame canvas allocations.
- Optimized pen drawing for maximum smoothness with eager low-latency wet-ink initialization, full 120–240Hz batched historical stylus sample preservation, velocity-adaptive stroke stabilization, pre-allocated pointer pools, and read-only prediction remapping.
- Fixed lag on the `+` creation button and app dropdowns by removing redundant nested theme wrappers, memoizing menu entries, streamlining dropdown rows with spring press and 45° spin feedback, and updating all `+` menu options with distinct rounded icons.
- Locked the UI and video player to the highest available display refresh rate across launch, resume, focus, display changes, and Android 15 touch refresh-rate boost, and renamed **Reader Mode** to **Kindle Mode** across the app.

- Added a unified crisp spring motion language across screen transitions, dialogs, segmented controls, panels, Kindle Mode bars, and tactile card/pill presses while keeping existing blur effects unchanged.
- Boosted app-wide rendering and scroll smoothness with GPU-only animated gradient text, hardware `RenderNode` caching for paper patterns, full viewport culling for all page items, `StaticLayout` sticky-note caching, async cover and PDF rendering with off-thread GPU texture upload, pre-cached page-flip audio waveforms, diffed settings writes, and narrow theme state subscriptions.
- Added a **Performance Mode** quick toggle alongside Battery Saver, Balanced, and Maximum tiers in **Settings → Appearance & Theme → Performance**.

- Selected palette colors display a contrasting checkmark as well as an outline. Zoom feedback sits near the upper-left canvas edge instead of its center.
- Library documents support Select mode, grid/list checkboxes, Select all, Clear, bulk Favorite, and confirmed Move to Trash. Back exits selection before navigating away.

- Palette changes are deduplicated and bounded, unchanged palettes are no longer rewritten during size adjustments, and recent colors are cached in memory.
- Preset and HEX selections synchronize the color wheel, duplicate recent/preset chips are hidden, and palette editing guards against stale slots while preserving the latest tool settings.
- Color swatches have larger compact touch targets, accessible color names, and explicit selected state.

- Documents navigation is fixed above the content, with a plus-only creation button and horizontally scrollable tabs on narrow windows. The bottom navigation inset is removed from the relocated bar.
- Tool menu transitions use a restrained spring, and the under-toolbar blur alone changes colors smoothly. No decorative strip animations were restored.
- Gradient text crossfades between randomized color arrangements. Workspace, Settings cards, and the creation menu follow the active light/dark palette.
- The optional video toolbar action is visible by default with a migrated visibility preference; users can still hide it in Settings.

- Sticky editing has separate Text and Handwriting modes, a simplified header, larger new notes, and safe editor sizing on narrow windows. Handwriting mode exposes the live canvas and preserves the typed draft when switching back.
- Tool popovers use compact wrapping controls and animated opening/closing, fixing the vertical size/color controls shown in the previous pen menu.
- Removed the remaining decorative strip animation and Lottie dependency. Reduced the size and opacity of workspace glows to restore the black background.
- Added subtle card shadows, enabled native stylus handwriting for sticky text on supported Android versions, and removed the preview notice from the getting-started guide.

- Circle-to-lasso refreshes its targets at activation and includes boundary crossings. Selection uses clean dashed bounds instead of retaining the gesture loop.
- Triple-tap conversion is opt-in, requires deliberate stylus taps, and resets when handwriting resumes. Stylus handwriting no longer arms empty-paper long-press actions.
- Highlighter scribbles can erase. Shape holds require a quieter stationary endpoint.
- Stylus-only mode permits immediate finger navigation, long-press selection and dragging, plus expanded sticky-header dragging while preserving palm-size rejection.
- Renderer visibility metadata is cached by immutable item-list identity, and triple-tap clustering has bounded passes and hash-based membership.
- Long documents use binary-search visibility culling rather than scanning every page on every canvas redraw. The window requests the highest supported refresh rate at the current resolution and reapplies it on focus.
- Added two dark blue/purple workspace glows alongside the moving bright glows, with the original subtle soft glow below the drawing toolbar.
- Compact pen controls, text-tool controls tinted with the active text color, and New-menu shapes that follow the roundness preference.
- Optional video buttons for fixed and floating toolbars, available in Settings > Toolbar. Lasso > More > Add attached comment opens an in-page note linked to the selected content; ordinary handwritten notes retain collapse/expand behavior.

- Rough lasso and circle gestures now disappear after selection, leaving clean dashed bounds. Resize and rotate use a dedicated corner-and-arrow icon.
- Editor bars use opaque black surfaces and retain the original subtle soft glow below the toolbar. Settings retains its reduced-motion-aware decorative animation.
- Settings use Material 3 list items, accessible switches, outlined section cards, clearer category headings, and a wider tablet navigation pane. Workspace icons use a rounded style without changing drawing-toolbar glyphs.
- Restored the previous Compose/Material 3 and Android build-tool versions while investigating startup crashes after the dependency upgrade.

- Dashed lasso outlines are shared with circle-to-lasso conversion. Drag to move a selection; use Resize and Rotate to activate corner, edge, and rotation handles.
- The New button opens a grouped menu with colored icon tiles for creation, study tools, folders, and imports.
- Drop PDFs from other apps onto Documents to import them into the current folder, with progress feedback and support for multiple files.
- Editor bars use black surfaces.

- Failed or interrupted saves retain pending edits for the next save attempt and show a message on failure. Page files are cleaned up only after the new page index is written.
- Full saves refresh every page file as well as the backup snapshot, preventing stale pages after an interrupted autosave.

- Deleting a page or lasso selection, or dropping a notebook into Trash, now asks for confirmation.

- Cross-page lasso moves now undo and redo in one step, restoring both pages together.

- Collapsed sticky notes now carry their handwriting when moved. The wide Library layout animates smoothly when the navigation sidebar changes width.

- Library and Settings use the original broad, softly feathered background gradients with added mint, cyan, blue, violet, coral, pink, and amber. The gradients loop and stay still when motion is reduced.
- A brief glow over handwritten ink as it converts to text, plus thicker dashed lasso outlines.
- Hold a lasso selection with the pen or a finger, scroll with another finger, and release to drop it on the destination page.
- Press and hold empty paper with the pen to open the same Quick Insert menu as a finger long-press.
- Adaptive Library and Settings layouts for phones, tablets, and split-screen, with Android Predictive Back in Settings.
- A first-run guide available from **+ → Getting started**, and reduced allocations during Kindle Mode page turns.

## v17.1.0 - 2026-11-25

### Added
- **Smart Self-Arranging Study Timer & Live Alerts:** Drag the Study Timer anywhere on screen and it smoothly snaps to the nearest safe perimeter anchor while avoiding docked or floating toolbars. When the device supports Live Alerts (Android 16+ Live Updates / Samsung Now Bar / ongoing chronometer), starting the timer automatically launches a live ticking status-bar/lockscreen alert.
- **Triple-Tap Handwriting-to-Text Gesture:** Tapping handwritten ink strokes 3 times with the stylus automatically clusters the baseline word/line, cleans up the tap dots, and converts it into crisp typed text.
- **Settings JSON Import / Export:** Export all app preferences, palettes, and presets to a `.json` file and restore them anytime from **Settings → Library & backup → Settings file**.
- **High Refresh Rate Support:** Automatically selects the highest supported display refresh rate (90Hz / 120Hz / 144Hz) in `MainActivity` for smoother scrolling and ink rendering.
- **Cross-Page Lasso Movement:** Dragging a Lasso selection across page boundaries now transfers selected strokes, text, stickies, images, and cards cleanly to the destination page with unified undo/redo support.
- **System & Internal Clipboard Paste in Quick Insert:** Long-pressing an empty area on any page now always surfaces the **Paste** action, supporting both internal Pro Notes selections and Android system clipboard text.
- **In-Place Notion-Style Sticky Notes & Sticky Presets:** Replaced modal dialogs with interactive in-place sticky note editing anchored directly over canvas notes. Added sticky note presets (*Classic Yellow*, *Study Lined*, *Grid Checklist*, *Rose Memo*, *Lavender Idea*, *Ocean Action*) persisted in `SettingsStore` with 1-tap custom style saving, paper pattern selection (Plain, Lined, Grid, Dotted), and seamless handwriting-to-typing sync.
- **Expanded Vector Sticker Pack:** Expanded built-in stickers across 6 clean vector categories (**Study & Academic**, **Planner & Dates**, **Arrows & Callouts**, **Badges & Flags**, **Icons & Diagrams**, **Minimal Decorative**) with crisp procedural vector rendering at any zoom level and horizontal category filtering.
- **Advanced Offline Subject Segmentation & Background Erase:** Upgraded background erase pipeline using CIE-standard perceptual color distance, multi-cluster border seed sampling, edge-aware BFS flood-fill, interior enclosed pocket clearing, anti-aliased edge feathering, and color de-fringing to prevent halos on dark paper.
- **Dynamic Gradient Erase Particles:** Replaced plain gray scribble dust with luminous gradient particles blending erased stroke colors into active theme accent gradients via a pre-allocated Canvas particle pool.

### Improved
- **Drag-Anywhere Floating Toolbar & Auto-Side-Adjust:** Removed the drag-handle arrow on the floating toolbar so you can drag anywhere on it to move, and widened side-edge auto-adjustment (`< 22%` / `> 78%`) so moving it to either side snaps cleanly into vertical orientation.
- **Bright Vivid Red Laser Pointer:** Updated the laser pointer beam to a bright, high-visibility crimson red (`#FF1744`) with a luminous white-rose core.
- **S Pen Pressure Sensitivity:** Tuned the stylus pressure gamma curve (`0.68 / sensitivity`) and fast EMA smoothing (`0.55f`) with clean `ACTION_DOWN` initialization so S Pen, Wacom, and USI 2.0 strokes respond expressively to light and heavy pressure. Removed all box frames, borders, and `"Write here…"` placeholders from Keyboard Mode, text boxes, sticky notes, and write boxes. Tapping anywhere on the page in Keyboard Mode commits and immediately relocates the blinking caret to the tapped spot, snapped to ruled/Seyès/Cornell/grid lines.
- **Thick Dashed Lasso UI Border:** Upgraded both live lasso drawing paths and selection bounding boxes to thick, high-contrast dashed borders with outer halos.
- **Stylus Quick Actions Shortcut:** Upgraded the curved Quick Actions overlay with theme-matched surfaces and 1-tap **Auto-Text** (converts active selection or switches to Auto-Text) and **Keyboard Mode** shortcuts.
- **Jetpack Compose BOM `2025.10.00`, Material 3 & Haze Progressive Blur:** Upgraded Compose/Material 3 dependencies, removed `skydoves/Cloudy`, and integrated hardware-accelerated `dev.chrisbanes.haze` (`1.3.1`) progressive blur with native Android 12+ `RenderEffect` fallbacks for smoother, zero-flicker scrolling.
- **Flashcards & Quiz M3 Expressive Overhaul:** Redesigned study interfaces with 3D card perspective flips, dynamic mid-flip elevation, ergonomic `Again / Hard / Good / Easy` action bar with keyboard shortcuts (`Space`, `1`–`4`), responsive tablet layout, tactile multiple-choice quiz options, and session analytics.
- **Physical Keyboard Ergonomics:** Added `Enter` checklist/numbered list continuation, `Tab`/`Shift+Tab` indent/outdent, `Ctrl+B/I/U` formatting, `Ctrl+Z/Y` undo/redo, and `Esc` exit.

### Removed
- **SlideShow:** Removed the SlideShow dialog and menu entry to keep the workspace focused and lean.

### Fixed
- **Palm Rejection & Stylus Side-Button Erase:** Fixed resting-palm false touches by extending the active palm rejection window, tracking stylus hover state, rejecting large palm contact areas regardless of `stylusOnly` mode, and honoring Android `FLAG_CANCELED`. Fixed stylus side-button erase reliability across hover-to-touch transitions and mid-stroke button presses, and cleared lingering eraser rings on lift.
- **Stylus Pressure Sensitivity & Quantization Jitter:** Fixed active stylus pressure detection across `MotionEvent` / `AXIS_PRESSURE` using calibrated gamma response curves and low-pass EMA filtering.
- **Scribble-to-Erase False Positives:** Overhauled scribble detection with strict multi-signal verification, eliminating false erases when writing numbers (`2`, `3`, `5`, `8`, `0`) or cursive handwriting.

- **Adaptive flashcard review.** Quiz practice pairs prompts with real answers from the deck, supports changing a choice before checking, and places answers beside the prompt on wide windows. Long cards scroll and grading buttons wrap on smaller screens.
- **Library tools.** Restored online book search and import, free community sticker and cover sharing, and eight PDF operations. Built-in templates have their own menu entry.
- **Floating video.** Direct video links request picture-in-picture as playback begins on supported devices.
- **Handwriting.** Optional per-notebook ink straightening levels gently sloping lines while retaining the original pen strokes.
- **Notebook widgets.** Pin a specific notebook from More, or use the general widget to resume writing or start a quick note.
- **Page organizer.** Select multiple pages and add or clear their bookmarks together.

## v16.0.0 - 2026-11-23

### Added

- **Optional Downloadable World Languages & Automatic RTL (Lean Default APK).** Added **Settings -> Language & Region** with optional on-demand downloadable UI language packs (**25 world languages** alongside built-in English US/UK: Spanish, French, German, Hindi, Simplified & Traditional Chinese, Japanese, Korean, Arabic, Portuguese, Italian, Russian, Turkish, Indonesian, Vietnamese, Thai, Bengali, Tamil, Urdu, Persian, Hebrew, Polish, Dutch, Ukrainian, and Swedish) and automatic Right-to-Left (RTL) layout mirroring for Arabic, Hebrew, Urdu, and Persian. Only English resources are bundled in the default APK (`resourceConfigurations = ["en"]`) to minimize install size; UI packs (~3 KB each) and **58+ ML Kit offline handwriting recognition packs** download only when chosen and can be removed anytime to reclaim storage.
- **Two-Pane Notebook Creation Studio.** Redesigned notebook creation into a responsive two-pane modal featuring a left Title, Cover, and Paper live preview rail, top dropdown pills for Page Size (including new **Tabloid** preset), Paper Color (including **Yellow Paper**), global Language picker (58+ languages), and Portrait/Landscape orientation toggle, collapsible categorized paper & cover galleries, and a bottom Cancel / Create Notebook action bar.
- **Crayon Tool.** Added a dry-wax **Crayon** instrument to the toolbar with procedural paper-grain texture rendering, dedicated crayon palette, wax tip size, grain slider, and opacity control.
- **Top Soft Scroll Blur (`skydoves/Cloudy`).** Added a soft, feathered progressive Cloudy blur along the top edge while scrolling across Library, Settings, and the Editor canvas (toggleable under **Settings -> Appearance -> Top soft scroll blur**).
- **Custom Device Storage Folder & Nested Folder Breadcrumbs.** Pick any folder inside your device's internal or external storage (via Android SAF) from **Library -> Save Folder…** or **Settings -> Library & backup -> Notes storage folder** to save and mirror notebooks, alongside a 1-tap **+ Folder / Subfolder** button and clickable nested breadcrumb trail (`Documents › Folder › Subfolder`).
- **Font & Style Picker in Lasso.** Tap **Font** on the floating Lasso bar to switch fonts across selected text, sticky notes, and write boxes (all 16 built-in typefaces + custom `.ttf` / `.otf` fonts, **Bold**, **Italic**, **A− / A+**). Picking a font on selected handwritten ink converts it directly into typed text in that font.
- **Custom Fonts, Dynamic Bold & Writing Guide Line for Auto Handwriting.** Import `.ttf` / `.otf` fonts for converted handwriting, auto-bold larger headings via Dynamic Bold, and display a baseline writing guide line beneath active writing in **Align** and **Auto-Text** modes.
- **Multilingual Auto Spell Check.** Added an offline Damerau-Levenshtein & accent-restoration spell checker supporting Unicode word boundaries across **11 languages** (English, Spanish, French, German, Italian, Portuguese, Dutch, Polish, Turkish, Indonesian, and Russian) with live tappable suggestion chips in inline text editing.
- **Handwriting-Aware Search.** Upgraded in-notebook search to recognize human handwriting strokes (with per-page caching and fuzzy matching) alongside typed text and PDFs.
- **8 New Page Templates.** Added **French Seyès**, **Calligraphy**, **Engineering Grid**, **Cornell Grid**, **Dot Cornell**, **Habit Tracker**, **Day Timeblock**, and **Comic Panels** (plus exposed **Handwriting** guide sheets).
- **Hold-to-Move Collapsed Sticky Notes & Quick Edit.** Folded sticky notes shrink to a compact messaging-icon badge that expands on tap and drags on long-press (~280ms); expanded sticky notes include a quick **✎** edit button and header-bar dragging.
- **Circle-to-Lasso in Auto Handwriting & Single-Stroke Arrow Snapping.** Circle ink in **Auto-Text** mode to select via Circle-to-Lasso without premature conversion, and snap single-stroke hand-drawn arrows plus near-horizontal/vertical lines (within 6°).

### Improved

- **Direct-on-Page Keyboard Mode.** Removed box chrome and frames in Keyboard Mode so a blinking pointer sits directly on the live page; automatically detects hardware keyboards and opens the soft keyboard when none is connected.
- **Glowing Bézier Laser Pointer (No End Dot).** Upgraded the Laser Pointer to smooth quadratic Bézier curves with a 4-layer translucent crimson bloom, hot core filament, smooth ease-out fade, and clean tip (ugly end dot removed).
- **Overhauled Shapes & Hold-to-Shape.** Tuned hold responsiveness (`420ms` delay, `14dp` jitter slop), separated Shape Hold strictly from Circle-to-Lasso, preserved active Pen/Crayon/Auto-Text color and width on snap, and added live styled drag previews.
- **Upgraded Studio Buttons, Lasso Borders & Study Friends Chat.** Refined studio buttons and inside-tool headers, thickened dashed Lasso borders with a high-contrast halo, and upgraded Study Friends Chat with live search, pinned messages, quote-replies, emoji reactions, timestamps, and 1-tap **Share Page Notes**.
- **App-Wide Animated Sweeping Gradients & Smoothness.** Applied the sweeping animated gradient to screen titles and search/chat prompts (`"What are you looking for?"`) across Library, Study Cards, Split View, Book Reader, and Study Rooms; eliminated per-frame canvas allocations (`RectF`, `Path`, `DashPathEffect`) and added spring micro-animations.

### Fixed

- **Scribble-to-Erase Overhaul.** Reworked spatial hit-testing and scrub-density/reversal checks so deliberate scribbles erase reliably without ever falsely erasing normal print or cursive handwriting.
- **Real Stylus Pressure Sensitivity.** Fixed pressure-sensitive brushes (`StockBrushes.pressurePen`) and gamma sensitivity curves so stroke width responds naturally to stylus pressure.
- **Sticky Notes & Auto Handwriting Fixes.** Fixed 6 sticky note bugs (tinted style rendering, checklist hit-testing, placeholder hiding under ink, all 16 + custom fonts, collapsed Lasso transform stability, and pin lock enforcement) and enabled Auto Handwriting directly inside expanded sticky notes.
- **Lasso Recoloring & Unwired Settings.** Fixed Lasso color/rainbow tinting on Write Boxes and wired every newly exposed tool and writing setting cleanly into persistence and rendering.

## v15.1.0 - 2026-11-21

### Added

- **Personal Handwriting Style (offline letter library).** Record up to 3 samples each for `a–z`, `A–Z`, `0–9`, and `. , ? !` inside the new **My Handwriting Style** studio (available in the inline text bar, Text tool settings, and Settings -> Stylus & input) with a live ruled-paper preview.
- **Type to My Ink.** Tap **My Ink** in any inline text box or Keyboard Mode to convert typed sentences directly into real Jetpack Ink strokes in your own handwriting—randomly selecting recorded copies, applying subtle natural size, tilt, baseline, and spacing variation, aligning baselines to ruled paper lines, and wrapping cleanly at the box edge so Eraser, Lasso, Resize, Recolor, and Undo work natively.
- **Fast parallel in-app updater.** Enabled 4-stream concurrent HTTP Range downloading (`128 KB` buffers) with resumable per-segment state persistence, live throughput/ETA and active stream indicator (`4× Fast`), a fast-download toggle in the update dialog, and automatic single-stream fallback when a server does not support parallel ranges.

### Improved

- **Modular editor architecture.** Split the two largest editor files by responsibility to make maintenance and debugging cleaner: extracted `EditorViewModelMindMap.kt`, `EditorViewModelStudy.kt`, and `EditorViewModelWriteBox.kt` from `EditorViewModel.kt`, and extracted `NoteCanvasEraser.kt`, `NoteCanvasSelection.kt`, `NoteCanvasGestures.kt`, and `NoteCanvasDocLayer.kt` from `NoteCanvasView.kt` while keeping all public APIs unchanged.

### Fixed

- **Locked or corrupted PDF import crash.** Wrapped Reader Mode, Library, and in-editor (`importFilesIntoCurrent`) PDF imports in error handling that always clears progress overlays and displays a clear toast when a file is password-protected or unreadable.
- **Unreadable cover photo and image imports.** Guarded `saveCoverImage`, sticker imports, tape image fills, editor image insertions, and custom page backgrounds against unreadable URIs and surfaced clear user feedback instead of crashing or failing silently.
- **Null content-stream and MediaStore crashes.** Replaced forced non-null `openInputStream(uri)!!` calls in `Repository.kt` and MediaStore screenshot saves in `EditorViewModelWriteBox.kt` with explicit error handling caught at callers.
- **Bitmap and canvas detach memory leaks.** Recycled temporary rendered PDF bitmaps in `finally` blocks during thumbnail generation, PDF export, flashcard region capture (`regionImage`), and selection screenshots, and cleaned up active zoom/lasso animators, hover click callbacks, and `VelocityTracker` on `NoteCanvasView.onDetachedFromWindow()`.
- **Large sticker photo out-of-memory risk.** Upgraded `StickerLibrary.import` to two-pass `BitmapFactory` bounds decoding with power-of-2 `inSampleSize` downsampling and guaranteed source bitmap recycling.
- **Orphaned files on failed PDF import.** Cleaned up copied PDF files and empty notebook directories automatically whenever PDF import fails halfway.

## v15.0.0 - 2026-11-21

### Added

- **Automatic handwriting.** Turn it on from the editor's More menu or the pen's Writing tab. After a short pause, nearby pen strokes can become editable text. Choose a font and optionally align the result to nearby writing or a paper rule.
- **Home screen widget.** Open the library, resume the most recent notebook, or create a Quick Note.
- **Page bookmarks.** Jump directly to bookmarked pages from More.
- **Flashcards.** Preview the source page during review and return to the same card.
- **Handwriting review.** Choose a recognition language for each notebook, then review alternate readings or restore ink from the latest automatic conversion.
- **Floating video.** Play a direct HTTPS video or stream in Android picture-in-picture on supported devices.
- **Writing tools.** Added Fineliner and Lettering tips, a stylus pressure toggle, optional tap-to-select, and more font choices. Added Graph Notebook, Music Manuscript, and Journal templates.

### Improved

- **Canvas drag and drop.** Images and stickers show a placement guide and scroll the canvas near the edges.
- **Editor layout.** Pen controls are grouped into Brush, Writing, Presets, and Stylus. Compact screens move search, page navigation, and sharing into More. Full-page keyboard mode opens without placeholder text.
- **Sticky notes and selection.** Check off tasks directly from the sticky board. The lasso panel also offers copy, flip, 90° rotation, and handwriting restyling.
- **Appearance overhaul.** Colorful adds a violet, teal, coral, and indigo theme for light and dark mode. Shared cards, controls, editor toolbars, and reading and library backgrounds use the new palette.
- **Live blur.** Android 12 and newer can blur live content behind editor panels and dialogs. The Appearance toggle and Reduce effects control it. Removed Liquid Glass.
- **Settings navigation.** Settings uses a stable category layout on phones and tablets. Returning to a notebook restores the current page.

### Fixed

- **Handwriting placement.** Nearby strokes are grouped by writing line. Each converted group stays on one line, and the source ink is preserved when recognition fails or the result would overlap or fit poorly.
- **Scribble erase.** Automatic recognition waits until lift and requires a dense gesture that crosses the same pen ink repeatedly. Fingers scroll and zoom by default, including after upgrading. Enable Draw with finger in More to use touch for ink. Clearing a search also resets its progress indicator.
- **Notebook preferences.** Pen and shape share a color in each notebook. Highlighter and text keep independent colors. Tool thickness, width presets, eraser size, and text formatting are remembered.


## v14.0.0 - 2026-11-19

- Find online books by title or author from the reading room. Import a free Project Gutenberg text into the local library for offline reading and annotation.
- Browse community artwork from the library Create menu. Contributors attach images to a public GitHub issue. Readers can save artwork as a cover image and add it to their sticker tray.
- PDF Station converts images to PDF, merges files in a selectable order, rotates pages, reverses page order, extracts ranges, selects odd or even pages, and exports embedded text. Processing runs locally and writes a separate output.
- Build notebooks from lecture, planner, sketchbook and task-list starters. Cover, paper and layout controls remain available.
- Search sticky notes across a notebook, filter pinned notes and jump to their page from Sticky board.
- Flashcard review keeps a stable queue while grades change. Practice missed cards again at the end of a session.
- Notebook-specific editor tools, palettes, toolbar arrangement, scroll direction and writing options persist across sessions.
- Keyboard mode preserves separate page annotations and commits the current text when switching notebooks. Toolbar selection has a tonal highlight and larger touch targets. Page-curl mesh resolution adapts to sheet dimensions.
- Optional Liquid glass in Appearance uses the bundled Kyant backdrop renderer on supported Android devices. Classic appearance and Reduce effects take precedence.

## v13.6.0 — 2026-11-18

### Improved
- **Unified Direct-On-Page Text Tool & Keyboard Mode (`EditorDialogs.kt`, `EditorScreen.kt`, `NoteCanvasView.kt`, `PageRenderer.kt`, `EditorViewModel.kt`, `ToolSettingsSheets.kt`):** Replaced both the modal `TextDialog` and the opaque full-screen `KeyboardNoteModeOverlay` with a single unified `InlinePageTextEditor` mounted inside the `imePadding()` canvas container. Tapping or dragging with the Text tool or enabling Keyboard Mode opens a 100% transparent, resizable inline text box directly over the live notebook paper with crisp 8-point square resize handles, top Move/Delete controls, automatic word wrapping to the box width, a live native Android `EditText` (with API 33+ `isAutoHandwritingEnabled = true` for native stylus-to-text input), a 1-tap transparent ML Kit inline handwriting capture mode with candidate chips, and a slim formatting bar docked cleanly above the soft keyboard.
- **3D Cylindrical Bottom-Corner Page Curl (`BookReaderCanvas.kt`, `BookReaderArea.kt`):** Upgraded `BookCurlStage` from a flat reflection fold to genuine 3D cylindrical curl kinematics starting from the bottom-right (forward) or bottom-left (backward) corner, rendered via a 26×26 `Canvas.drawBitmapMesh` warp with a smooth curved cylindrical tube mouth silhouette, real upright back-page content on 2-page spreads (and subtle mirrored paper bleed-through on single-page books), stationary spread-page preservation via `PathOperation.Union`, glossy specular tube highlight, deep valley crease shading, dual cast shadows, and 2D fingertip diagonal drag tracking (`detectDragGestures`).

### Fixed
- **Keyboard Mode Viewport & Undo Stability:** Mounted inline text editing inside `.imePadding()` so the soft keyboard never covers typed content or formatting controls, suppressed duplicate static canvas text rendering (`hiddenTextItemId`) during live editing, committed edits in a single atomic undo step without removing other text boxes on the page, and hooked toolbar Keyboard Mode toggle-off to commit active drafts safely.
- **Release Alignment:** Updated default application version metadata to `13.6.0` (`versionCode = 17`) and synced `docs/ARCHITECTURE.md`.

## v13.5.0 — 2026-11-18

### New
- **On-Device Handwriting-to-Text Engine (`HandwritingRecognizer.kt`):** Replaced the heuristic glyph matcher from scratch with an on-device ML Kit Digital Ink Recognition pipeline (`TimedStrokePoint` timestamps, `WritingArea`, `preContext`, and ranked top-5 candidate suggestions) backed by a clean-room directional cosine polyline offline fallback, plus tappable live candidate chips inside `WriteBoxStudioBar` and async conversion for Write Boxes, Lasso selections, and the handwriting pad.

### Improved
- **Studio Workbench Toolbar Redesign (`EditorToolbar.kt`):** Completely redesigned the fixed bar and floating dock with an open, hairline-separated architectural rail, elevated ink-tinted active tool pedestals, angled proportional stroke-weight previews, and 1-tap inline controls for Pen nibs (`Fountain` / `Ball` / `Brush` / `Dashed`), Highlighter straightening (`Auto` / `Straight` / `Free`), Shapes (`ShapeKind` & `Fill`), Eraser (`Stroke` / `Pixel` & `HL only`), Lasso (`Freeform` / `Box` & `Touch` / `Inside`), and Text sizing & colour.
- **Scribble-to-Erase & Lasso Accuracy (`Geometry.kt`, `NoteCanvasView.kt`):** Added circular/oval swirl scrub detection alongside PCA axis and zig-zag scribble checks, gated live scribble-erase on actual erasable ink beneath the scribble (eliminating cursive false positives on blank paper), tightened scribble spatial hit-testing, upgraded `Lasso` with O(1) bounds rejection, segment-vs-boundary crossing, and inner-box containment, and added tap-to-select support in Lasso mode.
- **Notebook Creation Studio (`LibraryDialogs.kt`):** Redesigned `NewNotebookDialog` with a layered cover + spine + peeking-paper live preview stage, categorised Cover gallery (`Page 1`, `Custom photo`, `Classic Matte`, `Textured & Studio`), live-tinted Paper Tone & Template studio, and visual Page Format & Orientation cards.
- **Bottom-Corner Realistic Page Curl (`BookReaderCanvas.kt`, `BookReaderArea.kt`):** Replaced the full-height vertical fold with true bottom-corner perpendicular-bisector page-curl kinematics starting from the bottom-right (forward) or bottom-left (backward) corner, keeping the top corner flat until the diagonal fold reaches the top edge, with Householder-reflected backside bleed-through, cylindrical fold-normal specular lighting, and dual cast shadows.

### Fixed
- **Duplicate Stylus Profiles in Settings (`Settings.kt`, `SettingsScreen.kt`):** Added automatic pen-key normalisation and deduplication (`dedupePens`) on load/save, merged manual/generic stylus entries cleanly with real hardware digitiser names, and removed the duplicate `"While held"` side-button section above `"Your pens"`.
- **Release Alignment:** Updated default application version metadata to `13.5.0` (`versionCode = 16`) and synced `docs/ARCHITECTURE.md`.

## v13.4.0 — 2026-11-18

### Improved
- **Curved Dual-Arc Quick Actions Palette:** Rebuilt `QuickActionsOverlay` as a transparent native `Popup` with two concentric curved stadium ribbons anchored around the stylus tip (zero screen dim or canvas blur), featuring Undo, Redo, Pen, Eraser, Highlighter, Selection (Lasso), stroke thickness selector, and live colour swatch on the inner arc, plus an expandable outer arc for Colour Palette (with custom colour picker), Stroke Sizes, Tool Settings, and extra Pro Notes tools (`Shapes`, `Text and Write Box`, `Sticky note`, `Concept Maps`, `Masking tape`) with automatic edge mirroring.
- **Distraction-Free Canvas Feedback:** Removed noisy gesture popups (`Scribble erased` banner, `Shape not recognised` hint, and the red debug wet-stroke canvas banner), silenced routine Write Box/Keyboard/clear toasts, made `EditorViewModel.toast` auto-dismissing only (`private set`), and allowed tapping any status toast to dismiss immediately.
- **Modular Screen Architecture:** Extracted `EditorDialogs.kt` (`MindNodeDialog`, `StickerSheet`, `TextDialog`, `KeyboardNoteModeOverlay`, `StudyTimer`, `WebLinkDialog`) from `EditorScreen.kt`, `BookReaderCanvas.kt` (`BookFlipSoundEngine`, `BookCurlStage`, `BookSheetFrame`, `BookPageSurface`, `BookPageLoaderPreview`) from `BookReaderArea.kt`, and `LibraryDialogs.kt` (`PdfImportLoaderOverlay`, `ManageTagsDialog`, `FolderDialog`, `CoverDialog`, `NewNotebookDialog`, and helper dialogs) from `LibraryScreen.kt`.
- **Naming & Copy Hygiene:** Standardised remaining masking tape documentation and eraser filter copy (`Sticky notes and masking tape`), aligned `Keyboard Mode` and `Link Preview` modal titles, and removed remaining exclamation marks from user-facing copy.
- **Support Screen Accessibility & Motion:** Added 48 dp minimum touch sizing to the Support Back control and wired `LocalReduceMotion` to skip appreciation burst animations when system animations are disabled.
- **Release Alignment:** Updated default application version metadata to `13.4.0` (`versionCode = 15`) and synced `docs/ARCHITECTURE.md`.

## v13.3.0 — 2026-11-18

### Improved
- **Studio Toolbar Redesign & Mechanics:** Rebuilt `EditorToolbar.kt` around grouped History, borderless Tool Tray, Preset, and Layout capsules with an animated active tool pill, proportional dot `SizeDots`, horizontal and vertical drag-to-reorder, and live dock edge snap preview rails when dragging the floating toolbar.
- **Anchored Quick Actions Popup:** Replaced the full-screen dimmed/blurred `Quick Actions` overlay with a compact native Compose `Popup` (`268 dp`) anchored at the stylus pointer without blurring the canvas, featuring Undo/Redo, Previous tool swap, a 3×3 tool matrix, quick ink swatches, and direct `Tool settings` access.
- **Editor Modularisation:** Extracted `EditorPanels.kt` (`TabStrip`, `TopBar`, `SearchBar`, `PagesPanel`, `WriteBoxStudioBar`) and `SplitViewPanel.kt` (`SplitPanel`) out of `EditorScreen.kt`, trimming over 1,200 lines from the primary editor file.
- **Reader Mode & Library Accessibility and Motion Compliance:** Wired `LocalReduceMotion` across `BookReaderArea.kt` (instant page turns, disabled infinite loader shimmer, snapped chrome transitions) and `LibraryScreen.kt` (snapped sidebar, grid/list switches, item motion, and PDF import loader shimmer), added localised `reader_*` accessibility strings, and enforced 48 dp minimum touch targets.

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
