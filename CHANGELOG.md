# Changelog

## v9.7.0
- **Upgraded Split Notes Workspace**: Smooth 60fps two-finger pinch-to-zoom and panning without scroll jitter, zoom presets (`Fit`, `100%`, `150%`, `-`, `+`), in-split keyword search with result jump, page-jumper strip, ratio presets (`30/70`, `50/50`, `70/30`), swap panes, and Clip Sticky / Insert to Page actions.
- **Import Notes & Files from Other Apps**: Multi-format file/backup picker & drag-and-drop plus Paste-from-App (`Notion`, `Obsidian`, `Apple Notes`, `Google Keep` JSON/Markdown/HTML/CSV/TXT/PDF/Images) in the Library and directly into any active notebook.
- **Upgraded Mind Map Studio**: Elevated tapered Bezier connectors with soft halos and anchor dots, root halos, upper tag badges, node detail notes, recursive collapsed `+N` descendant badges, Subtree Drag toggle, 3 auto-layouts (`Radial`, `Tree`, `Org Chart`), `Outline → Page`, `→ Flashcards`, and `Tab`/`Enter` node shortcuts.
- **Classic Grey Particle Scribble-Erase**: Smooth crumbling grey dust particles and soft dispersing rings on scribble-erase and clear-ink actions.
- **In-App Updater & Settings Cleanup**: Reliable foreground Activity confirmation launch for Android `PackageInstaller`, resilient GitHub release/APK download verification, and removed the Settings pen name & calibration orb card.
- **Streamlined Focus**: Removed the companion pet system, maths solver/auto-solve, and handwriting conversion/recognition templates while retaining all core pen/highlighter, scribble-to-erase, hold-to-shape, lasso, and local AI study features.

## v9.6.0
- **Live scribble-to-erase:** scribble or zig-zag directly across ink or text and watch it erase live while your pen is still down, alongside improved zig-zag and underline recognition.
- **Upgraded maths engine:** now handles quadratic equations (`x^2 - 5x + 6 = 0`), percentages (`20% of 150`, `15% off 200`), unit conversions (`10 km to m`, `72 f to c`), `nCr` / `nPr`, `log2`, constants (`tau`, `phi`), and smart candidate ranking.
- **Whiteboard & infinite canvas:** create dedicated infinite-canvas whiteboards or toggle any page into an unbounded pannable & zoomable workspace with live grid/dot backdrop and quick origin reset.
- **Mind mapping:** new Mind Map tool in the toolbar with drag-to-arrange nodes, cubic Bezier connectors, collapse/expand branches, colour/shape styling, radial & tree auto-layout, and AI notes-to-mindmap generation.
- **Notebook creation previews & recommendations:** A4 is highlighted as the recommended page size, and the Paper tab shows a live full-card preview that updates with pattern, tint, and orientation.
- **Downloadable local AI models & richer AI UI:** download free lightweight on-device GGUF models directly in Settings → On-Device AI, paired with an animated orbital thinking pill, rotating Pip study halo, and offline Explain & Mind Map support.

## v9.5.1
- **Smarter maths:** handwriting candidates are checked before an answer is inserted, common pen/OCR confusions are normalised conservatively, and incorrect completed equalities are rejected instead of being treated as valid sums.
- **More than basic arithmetic:** the on-device evaluator now supports implicit multiplication, powers, percentages, factorials, square/cube roots, constants, trigonometric and logarithmic functions, and one-variable linear equations.
- **Apple Notes-inspired answer reveal:** the answer now arrives with a smoother colour-shifting light sweep, a soft endpoint bloom and a restrained spring-settle, while retaining the existing handwritten letter-by-letter reveal.
- **Existing tools preserved:** this is a focused maths and animation update; notebook, page, PDF, pen, lasso, shape, tape, study and settings features remain in the update package.

## v9.5.0
- **Fixed top bar is the default:** the pen, size and colour options now live inside the top bar next to the tools.
- **Ghost ink fixed:** the pen hover dot and unfinished strokes no longer get left behind on the page.
- **Scribble to erase** is more reliable: scrubbing across a word and thin lines like underlines now erase, and pausing at the end no longer turns a scribble into a shape.
- **Shapes** are built from where the pen lifts, and the auto shape picks up rougher drawings.
- **Circle to lasso:** draw a loop around your notes and hold the pen still before lifting to turn it into a lasso, like GoodNotes.

## v9.4.1
- **Better sounds:** the pencil sounds brighter when you write fast and has more body and grit; the highlighter glides with a faint felt squeak; a soft tap when the pen lifts; a quick swoosh for two- and three-finger undo and redo; smoother fade in and out, with no clicks.

## v9.4.0
- **Split view:** ⋯ → *Split view* shows another notebook beside the one you are writing in. Pick which one at the top, scroll its pages, or tap *Open* to jump into it.

## v9.3.0
- **Colour bar snaps** to the nearest spot (left, centre or right; top or bottom) when you let go, like the floating toolbar.
- **Flashcard answers from the page:** in the card editor tap *Take the answer from the page* and box it. PDF text in the box fills the answer too.
- **AI screens:** key points slide in one by one.

## v9.2.0
- **Maths like Apple Notes:** the answer is written in, letter by letter, in a handwriting style and a warm gold colour. Fixed: erasing part of a sum now always removes its answer (the canvas could put it back).
- **Fixed top bar:** undo, redo and Pip the pet live there now, and the bar has its own soft accent tint.
- **Pip got smarter:** hops on page turns, cheers new cards and quizzes, thinks with the AI and dozes off when you take a break. Tap for a cheer.
- **Colour bar** (top bar layout) can be dragged anywhere and stays there.
- **Zoom out** stops at the page, so you only see the page you are on.

## v9.1.0
- **Pip, your study buddy:** a little pet in the corner. It breathes and blinks, watches while you write, thinks along with the AI, cheers when cards or links are added, and wiggles when you tap it. Turn it off in Settings.
- **AI thinking animation:** a glowing *Thinking* pill with bouncing dots while on-device AI works.
- **Lasso:** ✨ → *Maths to typed text* turns a written sum into neat typed maths. ✨ → *Save as screenshot* saves the selection to Photos.
- **Liquid glass** can be turned off in Settings for solid bars.
- **Top bar layout:** like GoodNotes, the tools sit in the top bar and their options in a small bar under it.

## v9.0.0
- **Links:** select linked ink and tap the red **Unlink** button. Long-pressing a link badge also works more reliably.
- **Auto maths:** the answer now belongs to the whole sum, even if you wrote it in parts. Erase any number and the answer goes.
- **New maths reveal:** an orange glow lifts off the sum and flies in an arc to the answer, which bounces in from orange to blue. The bottom bar is gone.
- **Scribble to erase:** only erases ink or text it mostly covers, never pictures, tape or stickies. The highlighter only scribbles away highlights. Erased ink crumbles away in a small animation.
- **Pages:** fixed jumping back to an older page when bars hide or the keyboard opens.
- **Circle to select:** only a real, closed pen loop offers it. Other tools and the side button no longer trigger it.
- **Page space:** undo, redo and the page counter fade while you write.

## v8.5.0
- **Maths, fixed:** erase any part of a sum (even just one number) and its answer disappears. New reveal: a glow runs along the sum from its first symbol to the answer, shifting violet to blue to green, then the answer bounces in.
- **Quiz, rebuilt:** questions from a picture (box a diagram; Gemini Nano reads it where available), 2 to 6 options, a stopwatch, streaks, an animated score ring with feedback, a review of missed questions, *Retry missed* and best score per notebook.
- **Flashcards and summaries** get the same rich look: gradient headers, a study timer, progress and a results screen.
- **Floating toolbar** never grows over undo / redo or the page counter.
- **Updates clean up:** the downloaded update file is deleted once it is installed.
- **Covers:** choosing a notebook cover is smooth (previews are drawn once and reused).
- **Onboarding:** new tips for maths and quizzes, and a paper-feel choice during setup.

## v8.0.0
- **One toolbar:** colours and sizes now sit inside the toolbar itself, after a thin divider. The toolbar floats over the page and fades while your pen is down, so you can write right under it.
- **Templates, GoodNotes style:** 25 templates in groups (Writing, Grids, Planning, Study, Music): college, wide and legal ruled, handwriting practice, small grid, graph, isometric, hexagons, daily, monthly, meeting, study notes, two columns, storyboard, checklist, guitar tab and more. Pick line spacing, line colour and paper colour, and apply to this page, a new page or every page (+ menu or ⋯ → Page template).
- **Tape:** ten patterns (dots, grid, gingham, zigzag, stars, hearts, waves, flowers...) or your own picture.
- **Gemini Nano on device:** on phones and tablets that have it, flashcards, quizzes, summaries and the new Explain use Google's free on-device model. Others keep the built-in tools.
- **PDF text menu:** highlight in four colours, underline, strike through, explain, flashcard, quiz questions from the passage, search and share.
- **Maths:** the answer types itself in, and erasing the sum removes it, even after reopening the notebook.
- **Eraser sound:** a clear rubber-on-paper sound.
- **Phones:** undo and redo move into the toolbar to save room.

## v7.2.1
- **Maths:** erase a sum and its answer goes away with it.

## v7.2.0
- **Large PDFs run smoother:** pages use lighter base images, and when you zoom in only the part on screen is drawn sharp. The full-PDF text scan no longer runs on open (it was slowing big files); text is read per page when you need it.
- **Haptics fixed:** the app was missing the vibration permission, so paper ticks never fired. They work now, with a fallback for tablets that ignore short pulses.
- **More sounds:** pens scratch, the highlighter glides, the eraser rubs. Page turns swish, shapes snap with a click, selections pop and pulling a new page clicks too.
- **Summarise page:** key points from the page's text in one tap (notebook ⋯ menu), and add them to the page.
- **Smart ink fixes:** no double model downloads, and the busy bar can no longer get stuck.

## v7.1.0
- **Paper feel:** a soft pencil-on-paper sound that follows how fast you write, and tiny haptic ticks like paper grain. Both can be turned off, with a volume slider (pen card or Settings → Paper feel).
- **Smarter handwriting:** recognition now uses the line's size and the words before it, so letters and numbers are read more accurately. Choose the handwriting language in Settings → Smart ink.
- **Better maths:** Solve tries every reading of your sum and picks the one that works, and understands powers like x².
- **Better generated quizzes:** wrong answers no longer give the right one away.

## v7.0.2
- **Pull to add a page** works again: in page-by-page mode you can drag past the last page, and after a pull the app scrolls to the new page instead of bouncing back.

## v7.0.1
- **Links are easy to find:** a blue **Link** button now leads the selection bar. It is also in the selection's ⋯ menu and in the notebook's ⋯ menu (*Add link…*).
- **Long-press PDF text** now works right away, even on big PDFs. The page under your finger is read on the spot. Scanned PDFs without text show a short note.
- **Convert as I write** leaves drawings alone and only converts lines that look like writing.

## v7.0.0
- **Smart ink, on device:** select handwriting and tap ✨ to convert it to text, solve a sum, tidy crooked lines, or make a flashcard. Turn on *Convert to text as I write* and *Solve maths when I write =* in the pen card. Handwriting is read by Google ML Kit on the tablet; the language model downloads once, then works offline.
- **Handwriting search:** search now finds words you wrote by hand.
- **Quiz:** write your own multiple-choice questions, or let Pro Notes make them from the PDF and your notes. Take the quiz with instant feedback and a score.
- **Make flashcards for me:** build cards from definitions and key sentences on a page or the whole notebook.
- **PDF text:** long-press text with a finger to select it, drag to extend, then copy, highlight, make a flashcard or search.
- **Undo and redo** now always sit at the top left, wherever the toolbar is.
- **Hold to snap** turns a held circle into a neat circle again. To select, lift the pen and hold the blue dot.
- **Pen haptics:** a light tick when the pen touches down (Settings).
- **Setup:** choose theme, accent, toolbar, scrolling, paper and smart ink while setting up.
- **Fixes:** zoomed-in swipes stay on one page, a tap on a PDF no longer nudges the page, and search highlights now line up on every PDF.

## v6.1.0
- **Links, like GoodNotes:** select ink and tap the new link button in the selection bar. Pick a page from thumbnails, or type a website. Linked content gets a soft blue box; tap it with a finger to follow. Long-press the badge to remove.
- **Tour:** rebuilt as swipeable cards with small animated demos for each gesture.
- **Update reminders:** a notification once a day when a new version is out, and the what's-new sheet when you open the app. Turn it off in Settings.
- **PDF:** double-tap zoom now eases to a fixed end point. It no longer drifts to another page or stops between two.

## v6.0.0
- **Flashcards:** drag a box over any part of a PDF or your notes to make a card, or turn a selection into one. Write the answer on the back, or make text-only cards. Study due cards with a flip and Again / Good / Easy, spaced over days. Jump back to the source page, or export as CSV.
- **Ink links:** select something, choose Link to..., then tap anywhere in the notebook. A blue chain badge sits at both ends; tap one to jump to the other. Long-press a badge to remove the link.
- **Study timer:** drag it anywhere. New settings: start by itself, show seconds, pause when idle.
- **New notebooks:** default page size, landscape and cover-as-first-page in Settings.
- **Big PDFs:** pages appear faster. A light first pass shows at once, then sharpens.

## v5.2.0
- **Toolbar:** the floating toolbar is back as the default. The top bar + side panel layout stays in Settings.
- **Circle to select:** draw a loop around content and lift the pen. A blue dot pulses at the loop's end. Press and hold it to turn the loop into a lasso, then drag.
- **Study timer:** a stopwatch for each session. Tap to pause, long-press for the notebook total or to restart.
- **Read only:** lock a notebook from the More menu. The pen and fingers only scroll and zoom.
- **Recording:** option to hide the toolbar while the screen is recorded (Android 15+).

## v5.1.0
- **Toolbar:** new default layout. Tools sit in the top bar, undo and redo float at the top left, and the pen types, sizes and colours sit in a panel on the right. The page counter moves to the bottom left.
- **Circle to select:** draw a loop around content and hold. The loop turns into a dashed lasso, the toolbar switches to lasso, and you can drag right away without lifting the pen. Tap outside to let go and go back to the pen.
- **Lasso outline:** freeform selections keep their loop shape instead of a box.
- **Pen setup:** new steps. Bring the pen close, hold its button, then three paced taps. A short scan follows, bubbles burst and your pen is revealed. Pens without hover or a button can skip those steps.

## v5.0.1
- **Pen setup:** a glowing ring around the circle again, and it glows brighter as the scan goes on. The percentage now sits on a solid pill.
- **Highlighter:** stronger by default (65%), and text always stays on top. Dark PDF pages blend the highlight under their light text.

## v5.0.0
- **Highlighter:** an even, round tip. Same width in every direction, and it ignores pressure, tilt and speed.
- **Highlighter opacity:** new slider in the highlighter card, from 20% to 80%.
- **Pen setup:** the ring animation now plays inside the circle on every device.
- **PDF:** smoother swiping, no flips while pinching, nearby pages prepared ahead.
- **Updates:** fixed installs that never started; the download no longer depends on the dialog.
- **Remembers you:** last tool, colours, sizes and page per notebook.

## v4.2.4
- **PDF:** pinch-zooming no longer flips to another page. When you let go, you stay on the page you were on.
- **Updates:** fixed downloads that finished but never installed. The download now keeps going after the dialog closes, and the install screen opens even if you left the app.

## v4.2.3
- **PDF:** pages next to the one you are on are prepared ahead of time, so swiping no longer shows blank white pages.
- **Highlighter:** a flat band with soft rounded ends, blended onto the page like a real highlighter.
## v4.2.2
- **Updates:** installing from the app now uses the system installer directly. If it fails, the app tells you why. Granting the install permission continues the update by itself.
- **Highlighter:** round ends instead of a tilted chisel tip. Old highlights are redrawn round too.
- **Remembered:** your last tool, pen type, colors, sizes, eraser size and the page you were on in each notebook.
- **PDF:** cheaper page shadows and pages are prepared before they scroll in, for smoother scrolling.

## v4.2.1
- **PDF:** smoother pinch and scroll. Pages show quickly, then sharpen once you stop moving. Pages you scrolled past are no longer drawn.
- **Pen setup:** the ring video now plays inside the circle.

## v4.2.0
- **Welcome:** the gradient video now fills the whole first screen.
- **Pen setup:** a ring video with a live percentage. Three pen taps, then a short scan that checks pressure, tilt and hover before showing the brand.
- **Updates:** you now see what changed before downloading. After installing, Pro Notes can reopen itself (needs "Display over other apps"), or shows a tap-to-open notification.
- **Support:** type any amount, or enter it in your UPI app.

## v4.1.0
- **Drag and drop:** reworked so pages and notebooks move reliably.
- **Pen button:** adjustable click delay.
- **Speed:** faster PDF rendering and smoother scrolling on large pages.
- **Payments:** the UPI ID is no longer shown in the app.

## v4.0.0
- Pen profiles with per-pen shortcuts and a tool wheel.
- Welcome tour, support screen, custom page sizes and covers.
- Apache-2.0 license with NOTICE.
