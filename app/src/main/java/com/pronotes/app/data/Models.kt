package com.pronotes.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PaperStyle(val label: String) {
    BLANK("Blank"),
    RULED("Ruled"),
    NARROW("Narrow ruled"),
    GRID("Grid"),
    DOTTED("Dotted"),
    CORNELL("Cornell"),
    TODO("To-do"),
    PLANNER("Weekly"),
    MUSIC("Music"),
    // v8 templates
    COLLEGE("College ruled"),
    WIDE("Wide ruled"),
    LEGAL("Legal pad"),
    HANDWRITING("Handwriting practice"),
    SMALL_GRID("Small grid"),
    GRAPH("Graph paper"),
    ISOMETRIC("Isometric"),
    HEX("Hexagons"),
    DAILY("Daily planner"),
    MONTHLY("Monthly"),
    MEETING("Meeting notes"),
    STUDY("Study notes"),
    TWO_COLUMN("Two columns"),
    STORYBOARD("Storyboard"),
    CHECKLIST("Checklist"),
    TABLATURE("Guitar tab"),
    ;

    /** Groups for the template picker, GoodNotes style. */
    val group: String get() = when (this) {
        BLANK, RULED, NARROW, COLLEGE, WIDE, LEGAL, HANDWRITING -> "Writing"
        GRID, SMALL_GRID, GRAPH, DOTTED, ISOMETRIC, HEX -> "Grids"
        TODO, CHECKLIST, PLANNER, DAILY, MONTHLY, MEETING -> "Planning"
        CORNELL, STUDY, TWO_COLUMN, STORYBOARD -> "Study"
        MUSIC, TABLATURE -> "Music"
    }

    val visibleInPicker: Boolean get() = this != HANDWRITING

    companion object {
        val pickerEntries: List<PaperStyle> get() = entries.filter { it.visibleInPicker }
    }
}

@Serializable
data class Folder(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val color: Int,
    val created: Long,
    val emoji: String = "",
    val modified: Long = created,
)

/**
 * Cover encodings:
 *   "solid:<argb>"            plain colour
 *   "pattern:<name>:<argb>"   generated pattern (dots, waves, grid, stripes, linen)
 *   "image:<file>"            photo stored in the notebook folder
 *   "thumb"                   live thumbnail of the first page
 */
@Serializable
data class NotebookMeta(
    val id: String,
    val title: String,
    val folderId: String? = null,
    val coverColor: Int,
    val paper: PaperStyle = PaperStyle.RULED,
    val created: Long,
    val modified: Long,
    val pageCount: Int = 1,
    val favorite: Boolean = false,
    val fromPdf: Boolean = false,
    val cover: String = "",
    val opened: Long = modified,
    /** Set when the notebook is in the trash. */
    val deletedAt: Long? = null,
    val isWhiteboard: Boolean = false,
) {
    val coverSpec: String get() = cover.ifBlank { if (fromPdf) "thumb" else "solid:$coverColor" }
}

@Serializable
data class LibraryIndex(
    val folders: List<Folder> = emptyList(),
    val notebooks: List<NotebookMeta> = emptyList(),
)

@Serializable
data class BrushDoc(val family: String, val color: Int, val size: Float, val epsilon: Float)

@Serializable
sealed class ItemDoc {
    @Serializable
    @SerialName("ink")
    data class Ink(val inputs: String, val brush: BrushDoc, val dx: Float = 0f, val dy: Float = 0f) : ItemDoc()

    @Serializable
    @SerialName("text")
    data class Text(
        val text: String, val x: Float, val y: Float, val size: Float, val color: Int,
        val font: String = "SANS", val bold: Boolean = false, val italic: Boolean = false,
        val underline: Boolean = false, val gradient: Int? = null, val align: String = "LEFT",
        val rot: Float = 0f,
    ) : ItemDoc()

    @Serializable
    @SerialName("fill")
    data class Fill(val points: List<Float>, val color: Int, val dx: Float = 0f, val dy: Float = 0f) : ItemDoc()

    @Serializable
    @SerialName("image")
    data class Image(val file: String, val x: Float, val y: Float, val w: Float, val h: Float, val rot: Float = 0f) : ItemDoc()

    @Serializable
    @SerialName("tape")
    data class Tape(
        val x: Float, val y: Float, val w: Float, val h: Float, val color: Int, val rot: Float = 0f,
        /** Freehand tape: path points relative to (x, y), and its thickness. */
        val pts: List<Float> = emptyList(), val thick: Float = 46f,
        /** "stripes", "plain", "dots", "grid", "checks", "zigzag", "stars", "hearts" or "image:<file>". */
        val pattern: String = "stripes",
    ) : ItemDoc()

    @Serializable
    @SerialName("sticky")
    data class Sticky(
        val x: Float, val y: Float, val w: Float, val h: Float, val color: Int, val text: String, val rot: Float = 0f,
        val fontSize: Float = 28f, val font: String = "SANS", val collapsed: Boolean = false, val pinned: Boolean = false,
    ) : ItemDoc()

    @Serializable
    @SerialName("mindnode")
    data class MindNode(
        val nodeId: Long,
        val x: Float,
        val y: Float,
        val w: Float,
        val h: Float,
        val label: String,
        val parentId: Long? = null,
        val color: Int,
        val shape: String = "pill",
        val collapsed: Boolean = false,
        val notes: String = "",
        val tag: String = "",
    ) : ItemDoc()
}

@Serializable
data class PageDoc(
    val id: String,
    val width: Float,
    val height: Float,
    val paper: PaperStyle,
    val pdfPage: Int = -1,
    val items: List<ItemDoc> = emptyList(),
    val bookmarked: Boolean = false,
    /** Paper colour for this page; null follows the app setting. */
    val paperColor: Int? = null,
    /** Set on a notebook's cover page: the cover design drawn full-page (see NotebookMeta.cover). */
    val cover: String? = null,
    /** Custom page: a picture file in the notebook folder used as the page background. */
    val bgImage: String? = null,
    /** Template tuning: line spacing multiplier and line colour (null = default). */
    val lineScale: Float = 1f,
    val lineColor: Int? = null,
    val isWhiteboard: Boolean = false,
)

@Serializable
data class NoteDoc(val version: Int = 2, val pages: List<PageDoc>)

/** Standard page size in world units (roughly A4 at 150 dpi). */
object PageSize {
    const val WIDTH = 1240f
    const val HEIGHT = 1754f
}

/** Page size presets in millimetres; world units are 150 dpi (5.906 per mm). */
enum class PagePreset(
    val label: String,
    val wMm: Float,
    val hMm: Float,
    val recommended: Boolean = false,
    val isWhiteboard: Boolean = false,
) {
    A4("A4", 210f, 297f, recommended = true),
    LETTER("Letter", 215.9f, 279.4f),
    A5("A5", 148f, 210f),
    A3("A3", 297f, 420f),
    SQUARE("Square", 210f, 210f),
    WIDE("16:9", 297f, 167f),
    PHONE("Phone", 75f, 160f),
    WHITEBOARD("Whiteboard", 520f, 360f, isWhiteboard = true),
    CUSTOM("Custom", 210f, 297f);

    companion object {
        const val UNITS_PER_MM = 1240f / 210f
    }
}

val CoverColors = listOf(
    0xFF3D6CF4.toInt(), 0xFF2BB673.toInt(), 0xFFF2994A.toInt(), 0xFFEB5757.toInt(),
    0xFF9B51E0.toInt(), 0xFF1B1F2A.toInt(), 0xFF56CCF2.toInt(), 0xFFF2C94C.toInt(),
    0xFF7BC8A4.toInt(), 0xFFF4A7C0.toInt(), 0xFF0E4C92.toInt(), 0xFF8D6E63.toInt(),
)

val FolderColors = listOf(
    0xFF5AC8FA.toInt(), 0xFF3D6CF4.toInt(), 0xFF34C759.toInt(), 0xFFFF9500.toInt(),
    0xFFFF3B30.toInt(), 0xFFAF52DE.toInt(), 0xFFFF2D55.toInt(), 0xFFFFCC00.toInt(),
    0xFF8E8E93.toInt(), 0xFF1B1F2A.toInt(),
)

val CoverPatterns = listOf(
    "leather", "kraft", "linen", "marble", "terrazzo", "plaid", "geometric", "gradient",
    "spiral", "dots", "waves", "grid", "stripes", "scales",
)
