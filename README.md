# Pro Notes

A handwriting notebook for Android tablets, built for the pen.

**Download:** grab `ProNotes.apk` from the [latest release](../../releases/latest). Free, public, no download limits.

## Features

**Writing**
- Low-latency ink with pressure (Jetpack Ink + motion prediction).
- Fountain pen, ball pen, brush pen, highlighter, with a pen card for type and thickness.
- Five colour swatches per tool. Tap a selected swatch to set any colour (wheel, hex, presets, recents).
- Shapes: auto-detect, line, arrow, double arrow, rectangle, rounded, ellipse, triangles, diamond, pentagon, hexagon, star, heart, cloud, speech bubble. Optional dashed line.
- Lasso, text, images, stickers, sticky notes, washi tape (tap to reveal), laser pointer.
- Laser ink stays for a few seconds, then fades. Circle content with the laser to select it.
- Draw and hold to snap a shape. Circle and hold to select. Scribble to erase.

**Pens**
- Samsung S Pen, Wacom, USI 2.0, Lenovo, Xiaomi, OnePlus, and other Android styluses.
- Hold the side button to erase (or lasso). Eraser-end pens erase when flipped.
- With a stylus, fingers scroll and zoom. Two-finger tap undo, three-finger tap redo.

**Pages**
- Scroll vertically or horizontally (page by page). Works for notes and PDFs.
- Pull past the last page to add a new one.
- Templates: blank, ruled, narrow, grid, dotted, Cornell, to-do, weekly planner, music.
- Paper colours: white, cream, gray, dark.
- Page grid with thumbnails, duplicate and delete pages.

**Library**
- Sidebar with Documents, Favorites, Recent. Grid or list view. Sort by date or name.
- Folders with any colour and an emoji icon.
- Notebook covers: colours, patterns, your own photo, or page 1 as the cover.

**PDF**
- Import a PDF or open one from another app. Write on it.
- Search the PDF's text; hits are highlighted on the page.
- Export or share any notebook as PDF.

**Toolbar**
- Floating and centred. Drag its grip to the top, bottom, left or right edge.

## Built with

| Part | Library |
|---|---|
| UI | Jetpack Compose, Material 3 |
| Ink, eraser hit-testing, storage | [Jetpack Ink](https://developer.android.com/jetpack/androidx/releases/ink) |
| Latency | androidx.input motion prediction |
| PDF render / export | Android `PdfRenderer` / `PdfDocument` |
| PDF search | [PdfBox-Android](https://github.com/TomRoush/PdfBox-Android) |
| Colour picker | [skydoves/colorpicker-compose](https://github.com/skydoves/colorpicker-compose) |
| Images | Coil |
| Data | kotlinx.serialization |

## Build

```
gradle assembleRelease
```

CI builds every push. Push a tag like `v2.0.0` to publish a Release with the APK.
Releases are signed with one fixed key, so updates install over older versions.

Pro Notes is an independent project. It is not affiliated with GoodNotes.

## License

MIT
