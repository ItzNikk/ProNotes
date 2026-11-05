# Pro Notes

A handwriting notebook for Android tablets, built for the pen.

**Download:** grab `ProNotes.apk` from the [latest release](../../releases/latest).

## Features

- **Pen first.** Low-latency ink with pressure, via Jetpack Ink and motion prediction.
- **Any Android stylus.** Samsung S Pen, Wacom, USI 2.0, Lenovo, Xiaomi, OnePlus, Microsoft-protocol pens.
- **Side button.** Hold the pen button to erase (or lasso, set in Pen & input). Pens with an eraser end erase when flipped.
- **Palm-friendly.** With a stylus, fingers scroll and pinch-zoom. Two-finger tap = undo, three-finger tap = redo.
- **Toolbar:** pen, eraser, highlighter, shapes, lasso, image, text, laser pointer, with per-tool colours and sizes.
- **Lasso.** Circle ink to select it; drag to move, recolour, duplicate or delete. With the pen, draw a loop and hold to select.
- **Shapes.** Draw and hold to snap to a line, circle, ellipse, triangle, rectangle.
- **Scribble to erase.** Zig-zag over ink to delete it.
- **Folders.** Nested folders, favourites, search, rename, move.
- **PDF.** Import a PDF (or open one from another app) and write on it. Export or share any notebook as PDF.
- **Paper:** blank, ruled, grid, dotted.

## Built with

| Part | Library |
|---|---|
| UI | Jetpack Compose, Material 3 |
| Ink, eraser hit-testing, storage | [Jetpack Ink](https://developer.android.com/jetpack/androidx/releases/ink) |
| Latency | androidx.input motion prediction |
| PDF | Android `PdfRenderer` / `PdfDocument` |
| Thumbnails | Coil |
| Data | kotlinx.serialization |

## Build

```
gradle assembleRelease
```

CI builds every push. Push a tag like `v1.0.0` to publish a Release with the APK.

Optional signing secrets: `KEYSTORE_B64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`.
Without them the APK is signed with a cached CI key.

Pro Notes is an independent project. It is not affiliated with GoodNotes.

## License

MIT
