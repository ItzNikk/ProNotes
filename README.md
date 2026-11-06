<div align="center">

# Pro Notes

**A pen-first notebook for Android tablets.**
Handwrite, annotate PDFs and organise everything in one fast, native app.

[![Latest release](https://img.shields.io/github/v/release/ItzNikk/ProNotes?label=download&style=for-the-badge)](https://github.com/ItzNikk/ProNotes/releases/latest)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue?style=for-the-badge)](LICENSE)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)

</div>

---

## Download

Get **`ProNotes.apk`** from the [latest release](https://github.com/ItzNikk/ProNotes/releases/latest).
Every release is signed with the same key, and the app updates itself from inside
**Settings → About** once installed.

## Highlights

| | |
|---|---|
| **Natural ink** | Low-latency, pressure-sensitive strokes powered by Jetpack Ink and motion prediction. Fountain, ball, brush and dashed pens. |
| **Any Android pen** | Samsung S Pen, Wacom, USI 2.0, Lenovo, Xiaomi, Huawei, OnePlus and Microsoft Pen Protocol. Each pen keeps its own settings. |
| **Pen shortcuts** | Hold the side button to erase or lasso. Click it while hovering for a radial tool wheel; double-click to undo. Hover preview shows where ink will land. |
| **Smart gestures** | Circle content, then press and hold the loop's end to select, scribble to erase, hold to snap shapes, two-finger tap to undo, pull past the last page to add one. |
| **Selection** | Lasso or rectangle with filters. Move, resize, rotate, flip, recolour, arrange, copy and share as an image. |
| **Shapes** | Auto-recognised lines, polygons, stars and circles, plus 30 drawable shapes with fill and dashed outlines. |
| **Highlighter** | Even, round tip that ignores pressure and tilt. Adjustable opacity, sits beneath PDF text and ink, with auto-straighten and hold-to-snap. |
| **PDF** | Import, annotate, search text, dark reading mode, and export or share as PDF. Nearby pages are prepared ahead, and pinch-zoom keeps you on your page. |
| **Pages** | Vertical or horizontal paging, custom sizes, nine templates, page colours, picture pages, bookmarks and drag-to-reorder. |
| **Library** | Folders with colours, drag and drop, covers you can write on, 14 cover materials, favourites, recents, trash and tabs. |
| **Remembers you** | Your last tool, pen type, colours, sizes and the page you were on in every notebook. |
| **Yours** | Backup and restore the whole library, themes, accent colours and a customisable toolbar. |
| **Updates** | In-app updates show what changed first, install through the system installer, and can reopen the app when done. |
| **Welcome** | A full-screen intro and a step-by-step pen setup: hover, button, paced taps and a scan that picks your pen brand. |
| **Layout** | A floating toolbar you can dock on any edge, or tools in the top bar with a side panel. It can hide itself while you screen-record. |
| **Study** | A stopwatch for each study session with a total per notebook, and a read-only mode for revising without stray marks. |

## Requirements

- Android 8.0 (API 26) or newer. Built for tablets; works on phones.
- Real background blur needs Android 12 or newer.

## Building from source

```bash
git clone https://github.com/ItzNikk/ProNotes.git
cd ProNotes
gradle assembleRelease
```

JDK 17 and the Android SDK (API 36) are required. CI builds every push; tags such as `v5.0.0` publish a release, with notes taken from [CHANGELOG.md](CHANGELOG.md).

Optional: set `pronotes.upi` in `gradle.properties` (or `SUPPORT_UPI` in CI) to enable the in-app support screen.

## Built with

Jetpack Compose · Jetpack Ink · Jetpack Graphics Shapes · PdfBox-Android · Coil ·
colorpicker-compose · Reorderable · kotlinx.serialization

## License and credit

Pro Notes is © 2026 **ItzNikk** and released under the [Apache License 2.0](LICENSE).
If you redistribute it or build on it, you must keep the [NOTICE](NOTICE) file and credit the original project:

> Based on Pro Notes by ItzNikk — https://github.com/ItzNikk/ProNotes

The Pro Notes name and icon may not be used for derived apps without permission.
Pro Notes is an independent project and is not affiliated with GoodNotes.
