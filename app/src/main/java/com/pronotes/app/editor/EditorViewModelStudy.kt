package com.pronotes.app.editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import androidx.lifecycle.viewModelScope
import com.pronotes.app.data.Flashcard
import com.pronotes.app.data.InkLink
import com.pronotes.app.data.SlideCard
import com.pronotes.app.data.SlideKind
import com.pronotes.app.data.SlideTheme
import com.pronotes.app.data.StudyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.abs

fun EditorViewModel.dueCards(now: Long = System.currentTimeMillis()): List<Flashcard> =
    cards.filter { it.due <= now }.sortedBy { it.due }

fun EditorViewModel.cardImage(name: String?): Bitmap? {
    if (name.isNullOrBlank()) return null
    return File(study.cardDir(), name).takeIf { it.exists() }?.let { runCatching { BitmapFactory.decodeFile(it.path) }.getOrNull() }
}

fun EditorViewModel.captureBack(draft: EditorViewModel.CardDraft) {
    backFor = draft
    cardDraft = null
    capturingCard = true
}

fun EditorViewModel.onAreaPicked(page: Int, rect: RectF) {
    capturingCard = false
    val waiting = backFor
    if (waiting != null) {
        backFor = null
        val p = pages.getOrNull(page) ?: return
        viewModelScope.launch {
            val bmp = regionImage(p, rect) ?: return@launch
            val name = "c_${UUID.randomUUID().toString().take(8)}.png"
            withContext(Dispatchers.IO) {
                FileOutputStream(File(study.cardDir(), name)).use { bmp.compress(Bitmap.CompressFormat.PNG, 92, it) }
            }
            bmp.recycle()
            cardDraft = waiting.copy(card = waiting.card.copy(backImage = name))
        }
        return
    }
    cutAreaFromPage(page, rect)
}

fun EditorViewModel.startCardFromArea(page: Int, rect: RectF) {
    capturingCard = false
    val p = pages.getOrNull(page) ?: return
    viewModelScope.launch {
        val bmp = regionImage(p, rect) ?: return@launch
        val name = "c_${UUID.randomUUID().toString().take(8)}.png"
        withContext(Dispatchers.IO) {
            FileOutputStream(File(study.cardDir(), name)).use { bmp.compress(Bitmap.CompressFormat.PNG, 92, it) }
        }
        bmp.recycle()
        cardDraft = EditorViewModel.CardDraft(
            Flashcard(
                id = UUID.randomUUID().toString(),
                frontImage = name,
                pageId = p.id,
                created = System.currentTimeMillis(),
            ),
            isNew = true,
        )
    }
}

fun EditorViewModel.cardFromSelection() {
    val p = pages.getOrNull(selectionPage) ?: return
    val rect = selectionUnion()?.apply { inset(-20f, -20f) } ?: return
    val text = p.items.filter { it.id in selection }.mapNotNull {
        when (it) {
            is TextItem -> it.text
            is WriteBoxItem -> it.convertedText.takeIf { s -> s.isNotBlank() }
            is StickyItem -> listOf(it.title, it.text).filter { s -> s.isNotBlank() }.joinToString(": ")
            else -> null
        }
    }.joinToString("\n").trim()
    val selPage = selectionPage
    val hadInk = p.items.any { it.id in selection && it is InkItem }
    onSelection(-1, emptySet(), null)
    if (text.isNotBlank() && !hadInk) {
        cardDraft = EditorViewModel.CardDraft(
            Flashcard(
                id = UUID.randomUUID().toString(),
                front = text,
                pageId = p.id,
                created = System.currentTimeMillis(),
            ),
            isNew = true,
        )
    } else {
        startCardFromArea(selPage.takeIf { it >= 0 } ?: pageIndex, rect)
    }
}

fun EditorViewModel.newTextCard() {
    val p = pages.getOrNull(pageIndex)
    cardDraft = EditorViewModel.CardDraft(
        Flashcard(
            id = UUID.randomUUID().toString(),
            pageId = p?.id,
            created = System.currentTimeMillis(),
        ),
        isNew = true,
    )
}

fun EditorViewModel.saveCard(card: Flashcard) {
    val idx = cards.indexOfFirst { it.id == card.id }
    cards = if (idx >= 0) cards.toMutableList().also { it[idx] = card } else cards + card
    cardDraft = null
    saveStudy()
}

fun EditorViewModel.deleteCard(id: String) {
    val victim = cards.firstOrNull { it.id == id }
    cards = cards.filterNot { it.id == id }
    saveStudy()
    if (victim != null) {
        viewModelScope.launch(Dispatchers.IO) {
            victim.frontImage?.let { File(study.cardDir(), it).delete() }
            victim.backImage?.let { File(study.cardDir(), it).delete() }
        }
    }
}

fun EditorViewModel.gradeCard(id: String, grade: Int) {
    cards = cards.map { if (it.id == id) StudyStore.review(it, grade) else it }
    saveStudy()
}

fun EditorViewModel.toggleStarCard(id: String) {
    cards = cards.map { if (it.id == id) it.copy(starred = !it.starred) else it }
    saveStudy()
}

fun EditorViewModel.showCardSource(card: Flashcard) {
    val idx = pages.indexOfFirst { it.id == card.pageId }
    if (idx >= 0) onScrollTo?.invoke(idx)
}

fun EditorViewModel.cardsCsv(): String {
    fun q(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
    return buildString {
        appendLine("Front,Back,Hint,Box,Reviews")
        for (c in cards) {
            appendLine("${q(c.front)},${q(c.back)},${q(c.hint)},${c.box},${c.reviews}")
        }
    }
}

internal suspend fun EditorViewModel.regionImage(page: Page, rect: RectF): Bitmap? = withContext(Dispatchers.Default) {
    val clamped = RectF(
        rect.left.coerceIn(0f, page.width),
        rect.top.coerceIn(0f, page.height),
        rect.right.coerceIn(0f, page.width),
        rect.bottom.coerceIn(0f, page.height),
    )
    if (clamped.width() < 8f || clamped.height() < 8f) return@withContext null
    val scale = (1200f / maxOf(clamped.width(), clamped.height())).coerceIn(0.6f, 2f)
    val outW = (clamped.width() * scale).toInt().coerceAtLeast(1)
    val outH = (clamped.height() * scale).toInt().coerceAtLeast(1)
    var bg: Bitmap? = null
    try {
        if (page.pdfPage >= 0 && isPdf) {
            bg = thumbLock.withLock {
                val pw = (page.width * scale).toInt().coerceIn(300, 1800)
                val ph = (pw * page.height / page.width).toInt().coerceAtLeast(1)
                runCatching { thumbPdf().render(page.pdfPage, pw, ph, useCache = false) }.getOrNull()
            }
        }
        val bmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val m = android.graphics.Matrix().apply {
            setTranslate(-clamped.left, -clamped.top)
            postScale(scale, scale)
        }
        c.concat(m)
        val r = PageRenderer(repo.noteDir(noteId)).apply {
            paperColor = settingsFlow.value.paperTint.argb
            darkPdf = settingsFlow.value.darkPdf
        }
        r.drawPaper(c, page, bg, shadow = false)
        r.drawItems(c, page.items, m)
        bmp
    } finally {
        bg?.recycle()
    }
}

// --------------------------------------------------------------- ink links

fun EditorViewModel.beginLinkFlow() {
    val p = pages.getOrNull(pageIndex) ?: return
    linkFrom = Triple(p.id, p.width * 0.5f, p.height * 0.25f)
    say("Tap anywhere to place the link target")
}

fun EditorViewModel.startLinkFromSelection() {
    val p = pages.getOrNull(selectionPage) ?: return
    val r = selectionUnion() ?: return
    linkSheet = p.id to r
    onSelection(-1, emptySet(), null)
}

private fun EditorViewModel.withoutOverlap(pageId: String, box: RectF): List<InkLink> =
    links.filterNot { l ->
        l.aPage == pageId && l.box.size == 4 &&
            RectF.intersects(RectF(l.box[0], l.box[1], l.box[2], l.box[3]), box)
    }

fun EditorViewModel.linkToPage(targetIndex: Int) {
    val (srcPageId, box) = linkSheet ?: return
    linkSheet = null
    val dst = pages.getOrNull(targetIndex) ?: return
    val link = InkLink(
        id = UUID.randomUUID().toString(),
        aPage = srcPageId,
        ax = box.right,
        ay = box.top,
        bPage = dst.id,
        bx = dst.width / 2f,
        by = dst.height * 0.18f,
        box = listOf(box.left, box.top, box.right, box.bottom),
    )
    links = withoutOverlap(srcPageId, box) + link
    saveStudy()
    onRedraw?.invoke()
    say("Linked to page ${targetIndex + 1}")
}

fun EditorViewModel.linkToUrl(rawUrl: String) {
    val (srcPageId, box) = linkSheet ?: return
    linkSheet = null
    val clean = rawUrl.trim()
    if (clean.isBlank()) return
    val url = if (clean.startsWith("http://", ignoreCase = true) || clean.startsWith("https://", ignoreCase = true)) {
        clean
    } else {
        "https://$clean"
    }
    val link = InkLink(
        id = UUID.randomUUID().toString(),
        aPage = srcPageId,
        ax = box.right,
        ay = box.top,
        box = listOf(box.left, box.top, box.right, box.bottom),
        url = url,
    )
    links = withoutOverlap(srcPageId, box) + link
    saveStudy()
    onRedraw?.invoke()
    say("Linked to website")
}

fun EditorViewModel.linkLabel(e: LinkEnd): String {
    if (!e.url.isNullOrBlank()) return e.url.removePrefix("https://").removePrefix("http://").take(28)
    return "Page ${e.toPage + 1}"
}

fun EditorViewModel.finishLink(page: Int, x: Float, y: Float) {
    val start = linkFrom ?: return
    linkFrom = null
    val dst = pages.getOrNull(page) ?: return
    if (start.first == dst.id && abs(start.second - x) < 24f && abs(start.third - y) < 24f) {
        say("Pick a different spot for the link target")
        return
    }
    links = links + InkLink(
        id = UUID.randomUUID().toString(),
        aPage = start.first,
        ax = start.second,
        ay = start.third,
        bPage = dst.id,
        bx = x,
        by = y,
    )
    saveStudy()
    onRedraw?.invoke()
    say("Link added")
}

fun EditorViewModel.selectionLinks(): List<String> {
    val p = pages.getOrNull(selectionPage) ?: return emptyList()
    val r = selectionUnion() ?: return emptyList()
    return links.filter { l ->
        l.aPage == p.id && l.box.size == 4 &&
            RectF.intersects(RectF(l.box[0], l.box[1], l.box[2], l.box[3]), r)
    }.map { it.id }
}

fun EditorViewModel.unlinkSelection() {
    val ids = selectionLinks().toSet()
    if (ids.isEmpty()) return
    links = links.filterNot { it.id in ids }
    saveStudy()
    onRedraw?.invoke()
    say("Removed link")
}

fun EditorViewModel.deleteLink(id: String) {
    links = links.filterNot { it.id == id }
    linkToDelete = null
    saveStudy()
    onRedraw?.invoke()
}

fun EditorViewModel.linkEnds(): List<LinkEnd> {
    if (links.isEmpty()) return emptyList()
    val pageIndexById = pages.withIndex().associate { it.value.id to it.index }
    val out = ArrayList<LinkEnd>(links.size * 2)
    for (l in links) {
        val ai = pageIndexById[l.aPage] ?: continue
        val box = l.box.takeIf { it.size == 4 }?.let { RectF(it[0], it[1], it[2], it[3]) }
        if (!l.url.isNullOrBlank()) {
            out += LinkEnd(l.id, ai, l.ax, l.ay, ai, l.ax, l.ay, box = box, url = l.url)
        } else {
            val bi = pageIndexById[l.bPage] ?: continue
            out += LinkEnd(l.id, ai, l.ax, l.ay, bi, l.bx, l.by, box = box)
            out += LinkEnd(l.id, bi, l.bx, l.by, ai, l.ax, l.ay)
        }
    }
    return out
}

// --------------------------------------------------------------- slideshow & auto flashcards

fun EditorViewModel.buildSlideShow(
    fromPage: Int = 0,
    toPage: Int = (pages.size - 1).coerceAtLeast(0),
    maxSlides: Int = 8,
) {
    val lo = fromPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
    val hi = toPage.coerceIn(lo, (pages.size - 1).coerceAtLeast(0))
    val themes = SlideTheme.entries
    val out = ArrayList<SlideCard>()
    out += SlideCard(
        id = UUID.randomUUID().toString(),
        kind = SlideKind.TITLE,
        theme = themes[0],
        title = title,
        subtitle = "Pages ${lo + 1}–${hi + 1}",
        badge = "${hi - lo + 1} pages",
    )
    var themeIdx = 1
    for (pi in lo..hi) {
        if (out.size >= maxSlides) break
        val summary = extractPageSummaryText(pi)
        val lines = summary.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) continue
        val heading = lines.first().take(72)
        val bullets = lines.drop(1).map { it.trimStart('-', '*', '•').trim().take(120) }.filter { it.isNotBlank() }.take(5)
        val kind = when {
            heading.contains('=') || bullets.any { it.contains('=') } -> SlideKind.FORMULA
            heading.endsWith('?') -> SlideKind.CHECK_QA
            else -> SlideKind.CONCEPT
        }
        out += SlideCard(
            id = UUID.randomUUID().toString(),
            kind = kind,
            theme = themes[themeIdx % themes.size],
            title = heading,
            bullets = bullets,
            question = if (kind == SlideKind.CHECK_QA) heading else "",
            answer = if (kind == SlideKind.CHECK_QA) bullets.joinToString("\n") else "",
            badge = "Page ${pi + 1}",
        )
        themeIdx++
    }
    if (out.size == 1) {
        say("Add typed text, stickies, or mind map nodes to build revision slides")
        return
    }
    slides = out
    showSlideShow = true
    saveStudy()
}

fun EditorViewModel.saveSlide(slide: SlideCard) {
    val idx = slides.indexOfFirst { it.id == slide.id }
    slides = if (idx >= 0) slides.toMutableList().also { it[idx] = slide } else slides + slide
    saveStudy()
}

fun EditorViewModel.deleteSlide(id: String) {
    slides = slides.filterNot { it.id == id }
    saveStudy()
}

fun EditorViewModel.moveSlide(from: Int, to: Int) {
    if (from !in slides.indices || to !in slides.indices || from == to) return
    val next = slides.toMutableList()
    val item = next.removeAt(from)
    next.add(to, item)
    slides = next
    saveStudy()
}

fun EditorViewModel.autoGenerateFlashcards() {
    val now = System.currentTimeMillis()
    val existingFronts = cards.map { it.front.trim().lowercase() }.toHashSet()
    val added = ArrayList<Flashcard>()

    for (p in pages) {
        val lines = ArrayList<String>()
        p.items.forEach { item ->
            when (item) {
                is TextItem -> lines += item.text.lines()
                is WriteBoxItem -> if (item.convertedText.isNotBlank()) lines += item.convertedText.lines()
                is StickyItem -> {
                    if (item.title.isNotBlank() && item.text.isNotBlank()) {
                        val key = item.title.trim().lowercase()
                        if (existingFronts.add(key)) {
                            added += Flashcard(
                                id = UUID.randomUUID().toString(),
                                front = item.title.trim(),
                                back = item.text.trim(),
                                pageId = p.id,
                                created = now,
                            )
                        }
                    } else {
                        lines += item.text.lines()
                    }
                }
                is MindNodeItem -> {
                    if (item.notes.isNotBlank()) {
                        val key = item.label.trim().lowercase()
                        if (existingFronts.add(key)) {
                            added += Flashcard(
                                id = UUID.randomUUID().toString(),
                                front = item.label.trim(),
                                back = item.notes.trim(),
                                pageId = p.id,
                                created = now,
                                hint = item.tag,
                            )
                        }
                    }
                }
                else -> Unit
            }
        }
        for (raw in lines) {
            val line = raw.trim().trimStart('-', '*', '•').trim()
            val sep = listOf(": ", " — ", " - ", " = ").firstOrNull { line.contains(it) } ?: continue
            val parts = line.split(sep, limit = 2)
            val front = parts.getOrNull(0)?.trim().orEmpty()
            val back = parts.getOrNull(1)?.trim().orEmpty()
            if (front.length >= 2 && back.length >= 2 && existingFronts.add(front.lowercase())) {
                added += Flashcard(
                    id = UUID.randomUUID().toString(),
                    front = front,
                    back = back,
                    pageId = p.id,
                    created = now,
                )
            }
        }
    }

    if (added.isEmpty()) {
        say("No new Term: Definition pairs found in this notebook")
        return
    }
    cards = cards + added
    saveStudy()
    say("Generated ${added.size} flashcard${if (added.size == 1) "" else "s"}")
}

/** A comment uses the existing sticky editor and persisted bidirectional content links. */
fun EditorViewModel.addSelectionComment() {
    val page = selectionPage
    val p = pages.getOrNull(page) ?: return
    val bounds = selectionUnion() ?: return
    val x = (bounds.right + 32f).coerceIn(16f, (p.width - 336f).coerceAtLeast(16f))
    val y = bounds.top.coerceIn(16f, (p.height - 256f).coerceAtLeast(16f))
    val comment = StickyItem(ItemIds.next(), x, y, 320f, 240f, stickyColor, "", title = "Comment")
    beginEdit(page)
    applyItems(page, p.items + comment)
    links = links + InkLink(
        id = UUID.randomUUID().toString(),
        aPage = p.id, ax = bounds.centerX(), ay = bounds.centerY(),
        bPage = p.id, bx = x + 38f, by = y + 38f,
        box = listOf(bounds.left, bounds.top, bounds.right, bounds.bottom),
    )
    saveStudy()
    onSelection(-1, emptySet(), null)
    stickyEdit = EditorViewModel.StickyEdit(page, comment, false)
    onRedraw?.invoke()
}