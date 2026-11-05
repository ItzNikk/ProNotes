package com.pronotes.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PaperStyle(val label: String) {
    BLANK("Blank"),
    RULED("Ruled"),
    GRID("Grid"),
    DOTTED("Dotted"),
}

@Serializable
data class Folder(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val color: Int,
    val created: Long,
)

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
)

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
    data class Text(val text: String, val x: Float, val y: Float, val size: Float, val color: Int) : ItemDoc()

    @Serializable
    @SerialName("image")
    data class Image(val file: String, val x: Float, val y: Float, val w: Float, val h: Float) : ItemDoc()
}

@Serializable
data class PageDoc(
    val id: String,
    val width: Float,
    val height: Float,
    val paper: PaperStyle,
    val pdfPage: Int = -1,
    val items: List<ItemDoc> = emptyList(),
)

@Serializable
data class NoteDoc(val version: Int = 1, val pages: List<PageDoc>)

/** Standard page size in world units (roughly A4 at 150 dpi). */
object PageSize {
    const val WIDTH = 1240f
    const val HEIGHT = 1754f
}

val CoverColors = listOf(
    0xFF3D6CF4.toInt(), 0xFF2BB673.toInt(), 0xFFF2994A.toInt(), 0xFFEB5757.toInt(),
    0xFF9B51E0.toInt(), 0xFF1B1F2A.toInt(), 0xFF56CCF2.toInt(), 0xFFF2C94C.toInt(),
)
