# Pro Notes

A handwriting notebook for Android tablets, built for the pen.

**Download:** grab `ProNotes.apk` from the [latest release](../../releases/latest). Free and public. Each release installs over the last one.

## What's new in 3.0

- **Resize and rotate** selections with corner and rotate handles (snaps to 15°). Flip, rotate 90°, bring to front, send to back, share as image.
- **Circle and hold** with the pen: the loop turns into a lasso and selects what it encloses.
- **Shape recognition rewritten**: lines, elbows, triangles, rectangles, squares, diamonds, quads, pentagons, hexagons, circles and ellipses.
- **Scribble to erase rewritten**: detects back-and-forth motion, so letters like "W" or "m" are never erased.
- **Highlighter**: freehand, auto-straighten, or always straight. Hold at the end to straighten. Draws behind ink.
- **Laser**: ink stays while the pen or a finger is on the screen.
- **Stickers**: planner labels and badges, emoji, and your own imported pictures.
- **Pages panel on the left**: thumbnails, drag to reorder, bookmark outline.
- **Trash**: deleted notebooks stay 30 days, restore or delete forever.
- **Tool cards**: tap a selected tool to see all of its settings (pen presets, eraser mode, lasso filters, laser time…).
- **Lasso options**: freeform or rectangle, filters (handwriting, highlighter, text, images, notes), select-on-touch.
- **Favourite pens**: save type + colour + thickness and switch in one tap.
- **Toolbar editor**: reorder and hide tools.
- **Backup and restore** the whole library to one ZIP file.
- **Dark PDF pages** for night reading, **go to page**, **share page as image**, **duplicate notebook**, **insert date** in text.
- Compact pill-shaped colour bar with custom colours, pill-style controls, square folder icons, proper dark mode.

## Everything else

Pressure ink (Jetpack Ink), S Pen / Wacom / USI side-button erase, palm rejection, pixel and stroke eraser,
vertical or horizontal page scrolling, pull to add a page, notebook tabs, folders with colours, covers,
9 page templates, PDF import / search / export, shapes with fill, rich text with gradients, tape, sticky notes.

## Built with

| Part | Library |
|---|---|
| UI | Jetpack Compose, Material 3 |
| Ink, eraser hit-testing, storage | [Jetpack Ink](https://developer.android.com/jetpack/androidx/releases/ink) |
| Shapes | [Jetpack Graphics Shapes](https://developer.android.com/jetpack/androidx/releases/graphics) |
| Drag to reorder | [Reorderable](https://github.com/Calvin-LL/Reorderable) |
| PDF search | [PdfBox-Android](https://github.com/TomRoush/PdfBox-Android) |
| Colour picker | [skydoves/colorpicker-compose](https://github.com/skydoves/colorpicker-compose) |
| Images | Coil |

Pro Notes is an independent project. It is not affiliated with GoodNotes.

## License

MIT
