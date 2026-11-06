<div align="center">

# Pro Notes

### Your ideas deserve a better workspace.

A thoughtfully crafted space for notes, handwritten ideas, PDF study and everything in between.  
Create freely, stay organised, and make room for focused work on Android.

<br />

<a href="https://github.com/ItzNikk/ProNotes/releases/latest">
  <img src="https://img.shields.io/github/v/release/ItzNikk/ProNotes?label=LATEST%20RELEASE&style=flat-square&color=7C3AED" alt="Latest release" />
</a>
<a href="https://github.com/ItzNikk/ProNotes/releases/latest/download/ProNotes.apk">
  <img src="https://img.shields.io/badge/DOWNLOAD-APK-111827?style=flat-square&logo=android&logoColor=white" alt="Download APK" />
</a>
<a href="LICENSE">
  <img src="https://img.shields.io/badge/LICENSE-Apache%202.0-64748B?style=flat-square" alt="Apache 2.0 license" />
</a>
<img src="https://img.shields.io/badge/ANDROID-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android 8.0+" />

</div>

---

## Get Pro Notes

**[Download the latest APK](https://github.com/ItzNikk/ProNotes/releases/latest/download/ProNotes.apk)** · [All releases](https://github.com/ItzNikk/ProNotes/releases)

Install the APK from a release, then check **Settings → About** for in-app updates. Releases are signed with the same key.

> Always download builds from this repository's official Releases page. Android may ask you to allow installation from your browser or file manager.

## Made for the way you study

Pro Notes brings handwriting, PDF study and notebook organisation together in a native Android workspace.

| Write | Study | Organise |
|:--|:--|:--|
| Natural, pressure-aware ink and a range of pen styles | Import, mark up, search and export PDFs | Folders, notebook covers, bookmarks and page templates |
| Stylus buttons, hover preview and gesture shortcuts | Read-only revision mode and a per-notebook study timer | Customisable toolbar, themes and library backup |

## Features

### Writing that stays out of your way
- Low-latency, pressure-sensitive handwriting powered by Jetpack Ink and motion prediction.
- Fountain, ball, brush and dashed pens, with individual pen settings.
- Samsung S Pen, Wacom, USI 2.0, Lenovo, Xiaomi, Huawei, OnePlus and Microsoft Pen Protocol support.
- Stylus shortcuts: hold the side button to erase or lasso, use the hover radial wheel, and double-click to undo.
- Gesture tools for circle-to-select, scribble-to-erase, hold-to-snap shapes and two-finger undo.
- Lasso and rectangular selection, with move, resize, rotate, flip, recolour, arrange, copy and image sharing.
- Automatic shape recognition, plus 30 drawable shapes with fill and dashed outlines.
- A round-tip highlighter with adjustable opacity and consistent width.

### PDFs and pages
- Import PDFs, annotate them, search their text, use dark reading mode, and export or share.
- Vertical or horizontal page navigation, custom page sizes, nine templates and page colours.
- Picture pages, bookmarks and drag-to-reorder page management.

### A library that feels like yours
- Colour-coded folders, drag and drop, writable covers and 14 cover materials.
- Favourites, recents, trash and tabs.
- Notebook-specific memory for your last tool, pen, colours, sizes and page.
- Library backup and restore, themes, accent colours and a customisable toolbar.

### Small details for focused work
- In-app updates with release notes and system-installer handoff.
- Pen setup that helps identify and configure your stylus.
- Floating toolbar that can dock to any edge, or an alternate top-bar and side-panel layout.
- Option to hide the toolbar while screen recording.
- Study stopwatch with notebook totals and a read-only mode to prevent accidental marks.

## Compatibility

- **Android:** 8.0 (API 26) or newer.
- **Devices:** designed with tablets in mind; also works on phones.
- **Background blur:** requires Android 12 or newer for the real blur effect.
- **Build environment:** JDK 17 and Android SDK API 36.

## Build from source

```bash
git clone https://github.com/ItzNikk/ProNotes.git
cd ProNotes
gradle assembleRelease
```

CI builds on pushes to the repository. Version tags such as `v5.0.0` publish a GitHub Release, using [CHANGELOG.md](CHANGELOG.md) for release notes.

Optional: set `pronotes.upi` in `gradle.properties` (or `SUPPORT_UPI` in CI) to enable the in-app support screen.

## Built with

[Jetpack Compose](https://developer.android.com/compose) · [Jetpack Ink](https://developer.android.com/develop/ui/compose/touch-input/stylus-input/ink-api) · Jetpack Graphics Shapes · PdfBox-Android · Coil · colorpicker-compose · Reorderable · kotlinx.serialization

---

## A note from the developer

Pro Notes is an independent project built and maintained by a student. Studies and other responsibilities come first, so development may be intermittent and replies to issues or pull requests may take time. Please do not expect a full-time response schedule or guaranteed turnaround.

This project is shared as a gift to students and learners who want a capable, open-source notebook without a barrier to getting started. Use it, learn from it, and make the most of it.

## Forks, reuse and attribution

You are welcome to **fork Pro Notes, study the source, adapt it and build your own version** under the terms of the Apache License 2.0.

Please preserve the original copyright and licence notices, include the [NOTICE](NOTICE) file in redistributed copies, and give clear credit to the original project. A suitable attribution is:

> Based on Pro Notes by ItzNikk — https://github.com/ItzNikk/ProNotes

The **Pro Notes** name and app icon identify the original project; please do not use them to endorse or brand a derived app without prior written permission. See [LICENSE](LICENSE) and [NOTICE](NOTICE) for the applicable terms.

---

<div align="center">

**Made by a student, shared with students.**

[Download](https://github.com/ItzNikk/ProNotes/releases/latest) · [Source code](https://github.com/ItzNikk/ProNotes) · [Changelog](CHANGELOG.md)

</div>
