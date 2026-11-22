package com.pronotes.app.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Mirrors notebooks and library index into any user-selected device storage folder
 * granted via Android Storage Access Framework (ACTION_OPEN_DOCUMENT_TREE).
 * Uses built-in [DocumentsContract] APIs with zero extra library dependencies.
 */
object StorageFolderSync {

    fun friendlyFolderName(uri: Uri): String {
        val docId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
            ?: uri.lastPathSegment
            ?: return "Custom Folder"
        val parts = docId.split(':', limit = 2)
        val root = when {
            parts.firstOrNull().equals("primary", ignoreCase = true) -> "Internal Storage"
            parts.size > 1 && parts[0].isNotBlank() -> parts[0]
            else -> "Storage"
        }
        val rel = parts.getOrNull(1)?.trim('/')?.takeIf { it.isNotBlank() }
        return if (rel != null) "$root / $rel" else root
    }

    private fun sanitizeFileName(title: String, id: String): String {
        val clean = title.trim()
            .replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_")
            .trim('_')
            .take(48)
            .ifBlank { "Notebook" }
        val shortId = id.take(8)
        return "${clean}_${shortId}.pronotes.json"
    }

    private fun findChildDocument(context: Context, treeUri: Uri, parentDocId: String, displayName: String): Uri? {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        )
        return runCatching {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val name = c.getString(1)
                    if (name == displayName) {
                        val docId = c.getString(0)
                        return@use DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    }
                }
                null
            }
        }.getOrNull()
    }

    private fun writeTextDocument(
        context: Context,
        treeUri: Uri,
        fileName: String,
        content: String,
    ): Boolean = runCatching {
        val parentDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentDocId)
        val targetUri = findChildDocument(context, treeUri, parentDocId, fileName)
            ?: DocumentsContract.createDocument(context.contentResolver, parentDocUri, "application/json", fileName)
            ?: return@runCatching false
        context.contentResolver.openOutputStream(targetUri, "wt")?.use { out ->
            out.write(content.toByteArray(Charsets.UTF_8))
            out.flush()
        } ?: return@runCatching false
        true
    }.getOrDefault(false)

    suspend fun mirrorNotebook(
        context: Context,
        treeUriStr: String,
        libraryJson: String,
        noteId: String? = null,
        noteTitle: String? = null,
        noteJson: String? = null,
    ) = withContext(Dispatchers.IO) {
        if (treeUriStr.isBlank()) return@withContext
        val treeUri = runCatching { Uri.parse(treeUriStr) }.getOrNull() ?: return@withContext
        writeTextDocument(context, treeUri, "library.json", libraryJson)
        if (!noteId.isNullOrBlank() && !noteJson.isNullOrBlank()) {
            val fileName = sanitizeFileName(noteTitle ?: "Notebook", noteId)
            writeTextDocument(context, treeUri, fileName, noteJson)
        }
    }

    suspend fun syncAllNotebooks(
        context: Context,
        treeUri: Uri,
        repo: Repository,
    ): Int = withContext(Dispatchers.IO) {
        val libJson = repo.readSerializedLibraryIndex()
        writeTextDocument(context, treeUri, "library.json", libJson)
        val activeNotebooks = repo.library.value.notebooks.filter { it.deletedAt == null }
        var synced = 0
        for (nb in activeNotebooks) {
            val raw = repo.readSerializedNoteDoc(nb.id)
            if (raw.isNotBlank() && raw != "{}") {
                val fileName = sanitizeFileName(nb.title, nb.id)
                if (writeTextDocument(context, treeUri, fileName, raw)) {
                    synced++
                }
            }
        }
        synced
    }
}
