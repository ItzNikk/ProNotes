package com.pronotes.app.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import java.io.File
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

/** A sticker in the picker. Built-in ones are drawn in code; the user's own are PNG files. */
sealed interface Sticker {
    data class Builtin(val id: String, val label: String, val category: String = StickerCategories.PLANNER) : Sticker
    data class Emoji(val text: String) : Sticker
    data class Custom(val file: File) : Sticker
}

object StickerCategories {
    const val STUDY = "Study"
    const val PLANNER = "Planner"
    const val ARROWS = "Arrows"
    const val BADGES = "Badges"
    const val ICONS = "Icons"
    const val MINIMAL = "Minimal"
}

/**
 * Sticker library: a set of planner-style stickers drawn in code (labels, badges,
 * arrows, check marks), emoji, and stickers the user imports from photos.
 */
object StickerLibrary {
    val builtins = listOf(
        // Study & Academic
        Sticker.Builtin("exam", "Exam", StickerCategories.STUDY),
        Sticker.Builtin("review", "Review", StickerCategories.STUDY),
        Sticker.Builtin("formula", "Formula", StickerCategories.STUDY),
        Sticker.Builtin("definition", "Definition", StickerCategories.STUDY),
        Sticker.Builtin("citation", "Quote", StickerCategories.STUDY),
        Sticker.Builtin("atom", "Atom", StickerCategories.STUDY),
        Sticker.Builtin("flask", "Lab", StickerCategories.STUDY),
        Sticker.Builtin("graduation", "Grad", StickerCategories.STUDY),
        Sticker.Builtin("pencil", "Draft", StickerCategories.STUDY),

        // Planner & Dates
        Sticker.Builtin("important", "Important", StickerCategories.PLANNER),
        Sticker.Builtin("todo", "To do", StickerCategories.PLANNER),
        Sticker.Builtin("done", "Done", StickerCategories.PLANNER),
        Sticker.Builtin("due", "Due", StickerCategories.PLANNER),
        Sticker.Builtin("note", "Note", StickerCategories.PLANNER),
        Sticker.Builtin("idea", "Idea", StickerCategories.PLANNER),
        Sticker.Builtin("urgent", "Urgent", StickerCategories.PLANNER),
        Sticker.Builtin("calendar", "Date", StickerCategories.PLANNER),
        Sticker.Builtin("clock", "Time", StickerCategories.PLANNER),
        Sticker.Builtin("priority", "Priority", StickerCategories.PLANNER),

        // Arrows & Callouts
        Sticker.Builtin("arrow", "Arrow", StickerCategories.ARROWS),
        Sticker.Builtin("arrow_curved", "Curved", StickerCategories.ARROWS),
        Sticker.Builtin("arrow_double", "Double", StickerCategories.ARROWS),
        Sticker.Builtin("callout_speech", "Speech", StickerCategories.ARROWS),
        Sticker.Builtin("callout_thought", "Thought", StickerCategories.ARROWS),
        Sticker.Builtin("bracket_left", "Focus", StickerCategories.ARROWS),

        // Badges & Flags
        Sticker.Builtin("flag", "Flag", StickerCategories.BADGES),
        Sticker.Builtin("pin", "Pin", StickerCategories.BADGES),
        Sticker.Builtin("check", "Check", StickerCategories.BADGES),
        Sticker.Builtin("cross", "Cross", StickerCategories.BADGES),
        Sticker.Builtin("verified", "Verified", StickerCategories.BADGES),
        Sticker.Builtin("tag", "Tag", StickerCategories.BADGES),
        Sticker.Builtin("bookmark", "Ribbon", StickerCategories.BADGES),

        // Icons & Diagrams
        Sticker.Builtin("bulb", "Bulb", StickerCategories.ICONS),
        Sticker.Builtin("lightning", "Energy", StickerCategories.ICONS),
        Sticker.Builtin("chart_up", "Trend", StickerCategories.ICONS),
        Sticker.Builtin("target", "Target", StickerCategories.ICONS),
        Sticker.Builtin("cloud", "Cloud", StickerCategories.ICONS),
        Sticker.Builtin("key", "Key", StickerCategories.ICONS),

        // Minimal Decorative
        Sticker.Builtin("star", "Star", StickerCategories.MINIMAL),
        Sticker.Builtin("heart", "Heart", StickerCategories.MINIMAL),
        Sticker.Builtin("sparkle", "Sparkle", StickerCategories.MINIMAL),
        Sticker.Builtin("flower", "Flower", StickerCategories.MINIMAL),
        Sticker.Builtin("leaf", "Leaf", StickerCategories.MINIMAL),
        Sticker.Builtin("coffee", "Coffee", StickerCategories.MINIMAL),
        Sticker.Builtin("sun", "Sun", StickerCategories.MINIMAL),
    )

    private val labelColors = mapOf(
        "important" to (0xFFE34948.toInt() to 0xFFD55181.toInt()),
        "todo" to (0xFF2A78D6.toInt() to 0xFF7161E0.toInt()),
        "done" to (0xFF0CA30C.toInt() to 0xFF1BAF7A.toInt()),
        "note" to (0xFFEDA100.toInt() to 0xFFEB6834.toInt()),
        "idea" to (0xFF8173E3.toInt() to 0xFFC04873.toInt()),
        "due" to (0xFFD95926.toInt() to 0xFFE34948.toInt()),
        "exam" to (0xFF256ABF.toInt() to 0xFF0F7E5C.toInt()),
        "review" to (0xFF5F5E5A.toInt() to 0xFF898781.toInt()),
        "formula" to (0xFF6366F1.toInt() to 0xFF8B5CF6.toInt()),
        "definition" to (0xFF0284C7.toInt() to 0xFF0D9488.toInt()),
        "urgent" to (0xFFDC2626.toInt() to 0xFFB91C1C.toInt()),
    )

    fun dir(context: Context) = File(context.filesDir, "stickers").apply { mkdirs() }

    fun custom(context: Context): List<Sticker.Custom> =
        dir(context).listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() }?.map { Sticker.Custom(it) } ?: emptyList()

    /** Whether the most recent sticker import automatically cut out a recognizable subject. */
    @Volatile var lastImportCutout: Boolean = false

    /**
     * Copies a picked picture into the sticker library (downsampled & scaled to 800 px).
     * When a recognizable foreground subject is detected against its background, automatically
     * removes the background with soft anti-aliased edges and crops cleanly around the subject.
     */
    fun import(context: Context, uri: Uri, autoRemoveBg: Boolean = true): File? = runCatching {
        lastImportCutout = false
        val maxSide = 800
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        (context.contentResolver.openInputStream(uri) ?: error("Can't open file")).use {
            BitmapFactory.decodeStream(it, null, boundsOpts)
        }
        var sample = 1
        val rawMax = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
        if (rawMax > maxSide) {
            while (rawMax / (sample * 2) >= maxSide) {
                sample *= 2
            }
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        val src = (context.contentResolver.openInputStream(uri) ?: error("Can't open file")).use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: return null
        var scaled: Bitmap? = null
        var cutout: Bitmap? = null
        try {
            val s = (maxSide.toFloat() / maxOf(src.width, src.height)).coerceAtMost(1f)
            val targetW = (src.width * s).toInt().coerceAtLeast(1)
            val targetH = (src.height * s).toInt().coerceAtLeast(1)
            val baseBmp = if (targetW < src.width || targetH < src.height) {
                Bitmap.createScaledBitmap(src, targetW, targetH, true).also { scaled = it }
            } else {
                src
            }
            val finalBmp = if (autoRemoveBg) {
                extractRecognizableSubject(baseBmp)?.also {
                    cutout = it
                    lastImportCutout = true
                } ?: baseBmp
            } else {
                baseBmp
            }
            val f = File(dir(context), "s_${UUID.randomUUID()}.png")
            f.outputStream().use { finalBmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            f
        } finally {
            if (cutout != null && cutout !== scaled && cutout !== src) cutout?.recycle()
            if (scaled != null && scaled !== src) scaled?.recycle()
            src.recycle()
        }
    }.getOrNull()

    /**
     * Offline recognizable-subject background remover:
     * 1. Skips images that already carry transparency.
     * 2. Samples multi-cluster border seeds using perceptual CIE-standard color difference.
     * 3. Runs edge-aware BFS flood-fill from all 4 borders.
     * 4. Detects and clears interior enclosed background pockets (holes inside loops/letters/subjects).
     * 5. Applies anti-aliased edge feathering and color de-fringing to eliminate white halos on dark paper.
     * 6. Crops cleanly with tight bounding margin.
     */
    private fun extractRecognizableSubject(bmp: Bitmap): Bitmap? {
        val w = bmp.width
        val h = bmp.height
        if (w < 16 || h < 16) return null
        val total = w * h
        val pixels = IntArray(total)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)

        // If image already has significant alpha cutout, keep as-is
        var transparentCount = 0
        for (i in 0 until total step 4) {
            if ((pixels[i] ushr 24) < 240) transparentCount++
        }
        if (transparentCount > (total / 4) * 0.06f) return null

        // Collect border samples (top, bottom, left, right strips)
        val borderColors = ArrayList<Int>(w * 2 + h * 2)
        for (x in 0 until w step 2) {
            borderColors += pixels[x]
            borderColors += pixels[(h - 1) * w + x]
        }
        for (y in 0 until h step 2) {
            borderColors += pixels[y * w]
            borderColors += pixels[y * w + (w - 1)]
        }
        if (borderColors.isEmpty()) return null

        // Build up to 4 representative border color seeds (handles gradients, shadows, two-tone backdrops)
        val seeds = ArrayList<Int>(4)
        seeds += borderColors.first()
        for (c in borderColors) {
            if (seeds.size >= 4) break
            if (seeds.all { perceptualColorDistSq(it, c) > 36 * 36 }) seeds += c
        }
        // Refine cluster centroids (2 k-means iterations)
        val seedR = DoubleArray(seeds.size) { Color.red(seeds[it]).toDouble() }
        val seedG = DoubleArray(seeds.size) { Color.green(seeds[it]).toDouble() }
        val seedB = DoubleArray(seeds.size) { Color.blue(seeds[it]).toDouble() }
        repeat(2) {
            val sumR = DoubleArray(seeds.size)
            val sumG = DoubleArray(seeds.size)
            val sumB = DoubleArray(seeds.size)
            val cnt = IntArray(seeds.size)
            for (c in borderColors) {
                val r = Color.red(c).toDouble(); val g = Color.green(c).toDouble(); val b = Color.blue(c).toDouble()
                var best = 0; var bestD = Double.MAX_VALUE
                for (k in seeds.indices) {
                    val dr = r - seedR[k]; val dg = g - seedG[k]; val db = b - seedB[k]
                    val d = dr * dr + dg * dg + db * db
                    if (d < bestD) { bestD = d; best = k }
                }
                sumR[best] += r; sumG[best] += g; sumB[best] += b; cnt[best]++
            }
            for (k in seeds.indices) {
                if (cnt[k] > 0) {
                    seedR[k] = sumR[k] / cnt[k]
                    seedG[k] = sumG[k] / cnt[k]
                    seedB[k] = sumB[k] / cnt[k]
                }
            }
        }
        val bgSeeds = IntArray(seeds.size) { k -> Color.rgb(seedR[k].toInt(), seedG[k].toInt(), seedB[k].toInt()) }

        // Measure border variance to calibrate tolerance
        var borderDistSum = 0.0
        for (c in borderColors) {
            var md = Int.MAX_VALUE
            for (s in bgSeeds) {
                val d = perceptualColorDistSq(c, s)
                if (d < md) md = d
            }
            borderDistSum += kotlin.math.sqrt(md.toDouble())
        }
        val avgBorderSpread = (borderDistSum / borderColors.size).toFloat()
        if (avgBorderSpread > 58f) return null

        val seedThresh = (34f + avgBorderSpread * 1.20f).coerceIn(32f, 74f)
        val seedThreshSq = (seedThresh * seedThresh).toInt()
        val localStepThreshSq = (28 * 28)

        // Edge-aware BFS flood fill from all 4 image borders
        val isBg = BooleanArray(total)
        val queue = IntArray(total)
        var qHead = 0
        var qTail = 0

        fun matchesBgSeed(c: Int): Boolean {
            for (s in bgSeeds) {
                if (perceptualColorDistSq(c, s) <= seedThreshSq) return true
            }
            return false
        }

        fun enqueueIfBg(idx: Int) {
            if (!isBg[idx] && matchesBgSeed(pixels[idx])) {
                isBg[idx] = true
                queue[qTail++] = idx
            }
        }

        for (x in 0 until w) {
            enqueueIfBg(x)
            enqueueIfBg((h - 1) * w + x)
        }
        for (y in 0 until h) {
            enqueueIfBg(y * w)
            enqueueIfBg(y * w + (w - 1))
        }

        while (qHead < qTail) {
            val cur = queue[qHead++]
            val cx = cur % w
            val cy = cur / w
            val curC = pixels[cur]
            fun tryNeighbor(nx: Int, ny: Int) {
                val nIdx = ny * w + nx
                if (isBg[nIdx]) return
                val nc = pixels[nIdx]
                if (matchesBgSeed(nc) || (perceptualColorDistSq(curC, nc) <= localStepThreshSq && minSeedDistSq(nc, bgSeeds) <= (seedThreshSq * 1.5f).toInt())) {
                    isBg[nIdx] = true
                    queue[qTail++] = nIdx
                }
            }
            if (cx > 0) tryNeighbor(cx - 1, cy)
            if (cx + 1 < w) tryNeighbor(cx + 1, cy)
            if (cy > 0) tryNeighbor(cx, cy - 1)
            if (cy + 1 < h) tryNeighbor(cx, cy + 1)
        }

        // Secondary pass: Detect interior enclosed background pockets (holes inside loops/subjects)
        // If an enclosed region closely matches the background cluster seeds, erase it as well
        val pocketVisited = BooleanArray(total)
        val pocketQueue = IntArray(total)
        for (i in 0 until total) {
            if (isBg[i] || pocketVisited[i]) continue
            if (!matchesBgSeed(pixels[i])) continue
            var pHead = 0; var pTail = 0
            pocketVisited[i] = true
            pocketQueue[pTail++] = i
            var touchesBorder = false
            while (pHead < pTail) {
                val cur = pocketQueue[pHead++]
                val cx = cur % w; val cy = cur / w
                if (cx == 0 || cx == w - 1 || cy == 0 || cy == h - 1) touchesBorder = true
                fun checkPocketNeighbor(nx: Int, ny: Int) {
                    val nIdx = ny * w + nx
                    if (!isBg[nIdx] && !pocketVisited[nIdx] && matchesBgSeed(pixels[nIdx])) {
                        pocketVisited[nIdx] = true
                        pocketQueue[pTail++] = nIdx
                    }
                }
                if (cx > 0) checkPocketNeighbor(cx - 1, cy)
                if (cx + 1 < w) checkPocketNeighbor(cx + 1, cy)
                if (cy > 0) checkPocketNeighbor(cx, cy - 1)
                if (cy + 1 < h) checkPocketNeighbor(cx, cy + 1)
            }
            // Enclosed pocket that matches background seeds: mark all its pixels as background
            if (!touchesBorder && pTail < total * 0.35f) {
                for (k in 0 until pTail) isBg[pocketQueue[k]] = true
            }
        }

        // Check foreground coverage
        var fgCount = 0
        for (i in 0 until total) if (!isBg[i]) fgCount++
        val fgRatio = fgCount.toFloat() / total.toFloat()
        if (fgRatio < 0.040f || fgRatio > 0.88f) return null

        // Verify subject coherence: largest connected foreground component should dominate
        val visited = BooleanArray(total)
        var largestComp = 0
        var largestSeedIdx = -1
        for (i in 0 until total) {
            if (isBg[i] || visited[i]) continue
            var head = 0; var tail = 0
            visited[i] = true
            queue[tail++] = i
            while (head < tail) {
                val cur = queue[head++]
                val cx = cur % w; val cy = cur / w
                if (cx > 0 && !isBg[cur - 1] && !visited[cur - 1]) { visited[cur - 1] = true; queue[tail++] = cur - 1 }
                if (cx + 1 < w && !isBg[cur + 1] && !visited[cur + 1]) { visited[cur + 1] = true; queue[tail++] = cur + 1 }
                if (cy > 0 && !isBg[cur - w] && !visited[cur - w]) { visited[cur - w] = true; queue[tail++] = cur - w }
                if (cy + 1 < h && !isBg[cur + w] && !visited[cur + w]) { visited[cur + w] = true; queue[tail++] = cur + w }
            }
            if (tail > largestComp) {
                largestComp = tail
                largestSeedIdx = i
            }
        }
        if (largestSeedIdx < 0 || largestComp.toFloat() / fgCount.toFloat() < 0.45f) return null

        // Primary background seed color for de-fringing
        val primaryBg = bgSeeds.firstOrNull() ?: Color.WHITE
        val bgR = Color.red(primaryBg)
        val bgG = Color.green(primaryBg)
        val bgB = Color.blue(primaryBg)

        // Compute alpha mask with anti-aliased edge feathering and color de-fringing
        val rawAlpha = IntArray(total) { i -> if (isBg[i]) 0 else 255 }
        val feathered = IntArray(total)
        val cleanedColors = IntArray(total)
        var minX = w; var minY = h; var maxX = 0; var maxY = 0

        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                val idx = row + x
                if (rawAlpha[idx] == 0) {
                    feathered[idx] = 0
                    continue
                }
                // Check 3x3 surrounding window for background contact
                var bgNeighbors = 0
                if (x > 0 && rawAlpha[idx - 1] == 0) bgNeighbors++
                if (x + 1 < w && rawAlpha[idx + 1] == 0) bgNeighbors++
                if (y > 0 && rawAlpha[idx - w] == 0) bgNeighbors++
                if (y + 1 < h && rawAlpha[idx + w] == 0) bgNeighbors++
                if (x > 0 && y > 0 && rawAlpha[idx - w - 1] == 0) bgNeighbors++
                if (x + 1 < w && y > 0 && rawAlpha[idx - w + 1] == 0) bgNeighbors++
                if (x > 0 && y + 1 < h && rawAlpha[idx + w - 1] == 0) bgNeighbors++
                if (x + 1 < w && y + 1 < h && rawAlpha[idx + w + 1] == 0) bgNeighbors++

                val outA = when {
                    bgNeighbors >= 5 -> 65
                    bgNeighbors in 3..4 -> 145
                    bgNeighbors in 1..2 -> 215
                    else -> 255
                }
                feathered[idx] = outA

                // Color de-fringing: remove background contamination on semi-transparent boundary pixels
                val orig = pixels[idx]
                if (outA in 30..240) {
                    val alphaRatio = outA / 255f
                    val invRatio = 1f - alphaRatio
                    val r = ((Color.red(orig) - bgR * invRatio) / alphaRatio).toInt().coerceIn(0, 255)
                    val g = ((Color.green(orig) - bgG * invRatio) / alphaRatio).toInt().coerceIn(0, 255)
                    val b = ((Color.blue(orig) - bgB * invRatio) / alphaRatio).toInt().coerceIn(0, 255)
                    cleanedColors[idx] = Color.argb(outA, r, g, b)
                } else {
                    cleanedColors[idx] = (orig and 0x00FFFFFF) or (outA shl 24)
                }

                if (outA > 20) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        if (maxX <= minX + 4 || maxY <= minY + 4) return null

        val pad = 8
        val cropW = (maxX - minX + 1) + pad * 2
        val cropH = (maxY - minY + 1) + pad * 2
        val outPixels = IntArray(cropW * cropH)
        for (y in minY..maxY) {
            val srcRow = y * w
            val dstRow = (y - minY + pad) * cropW + pad
            for (x in minX..maxX) {
                val a = feathered[srcRow + x]
                if (a > 0) {
                    outPixels[dstRow + (x - minX)] = cleanedColors[srcRow + x]
                }
            }
        }
        return Bitmap.createBitmap(cropW, cropH, Bitmap.Config.ARGB_8888).apply {
            setPixels(outPixels, 0, cropW, 0, 0, cropW, cropH)
        }
    }

    /** Perceptually-weighted color distance adhering to CIE sensitivity standard. */
    private fun perceptualColorDistSq(c1: Int, c2: Int): Int {
        val r1 = Color.red(c1); val g1 = Color.green(c1); val b1 = Color.blue(c1)
        val r2 = Color.red(c2); val g2 = Color.green(c2); val b2 = Color.blue(c2)
        val rmean = (r1 + r2) / 2
        val dr = r1 - r2
        val dg = g1 - g2
        val db = b1 - b2
        return (((512 + rmean) * dr * dr) shr 8) + 4 * dg * dg + (((767 - rmean) * db * db) shr 8)
    }

    private fun minSeedDistSq(c: Int, seeds: IntArray): Int {
        var best = Int.MAX_VALUE
        for (s in seeds) {
            val d = perceptualColorDistSq(c, s)
            if (d < best) best = d
        }
        return best
    }

    fun bitmap(sticker: Sticker, size: Int = 360): Bitmap? = when (sticker) {
        is Sticker.Custom -> BitmapFactory.decodeFile(sticker.file.path)
        is Sticker.Emoji -> emoji(sticker.text, size)
        is Sticker.Builtin -> builtin(sticker.id, size)
    }

    private fun emoji(text: String, size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size * 0.8f; textAlign = Paint.Align.CENTER }
        val c = Canvas(bmp)
        val fm = p.fontMetrics
        c.drawText(text, size / 2f, size / 2f - (fm.ascent + fm.descent) / 2f, p)
        return bmp
    }

    private fun builtin(id: String, size: Int): Bitmap {
        val label = labelColors[id]
        if (label != null) {
            val w = size; val h = (size * 0.42f).toInt()
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val r = RectF(6f, 6f, w - 6f, h - 6f)
            val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(r.left, r.top, r.right, r.bottom, label.first, label.second, Shader.TileMode.CLAMP)
                setShadowLayer(6f, 0f, 3f, 0x33000000)
            }
            c.drawRoundRect(r, r.height() / 2f, r.height() / 2f, bg)
            val t = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE; textSize = h * 0.40f; textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); letterSpacing = 0.06f
            }
            val fm = t.fontMetrics
            val stLabel = builtins.firstOrNull { it.id == id }?.label?.uppercase() ?: id.uppercase()
            c.drawText(stLabel, w / 2f, h / 2f - (fm.ascent + fm.descent) / 2f, t)
            return bmp
        }

        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val s = size.toFloat()
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = s * 0.08f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; color = Color.WHITE
        }

        fun disc(a: Int, b: Int) {
            fill.shader = LinearGradient(0f, 0f, s, s, a, b, Shader.TileMode.CLAMP)
            c.drawCircle(s / 2f, s / 2f, s * 0.44f, fill)
            fill.shader = null
        }

        when (id) {
            // Minimal & Decorative
            "star" -> {
                val path = Path()
                for (i in 0 until 10) {
                    val a = -Math.PI / 2 + i * Math.PI / 5
                    val k = if (i % 2 == 0) 0.46f else 0.20f
                    val x = s / 2 + (s * k * cos(a)).toFloat(); val y = s / 2 + (s * k * sin(a)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                fill.shader = LinearGradient(0f, 0f, s, s, 0xFFFAB219.toInt(), 0xFFEB6834.toInt(), Shader.TileMode.CLAMP)
                c.drawPath(path, fill)
            }
            "heart" -> {
                val path = Path()
                for (i in 0..120) {
                    val t = i / 120.0 * 2 * Math.PI
                    val x = 16 * sin(t) * sin(t) * sin(t)
                    val y = 13 * cos(t) - 5 * cos(2 * t) - 2 * cos(3 * t) - cos(4 * t)
                    val px = s / 2 + (x / 17.0 * s * 0.45).toFloat(); val py = s / 2 - (y / 17.0 * s * 0.45).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                fill.shader = LinearGradient(0f, 0f, s, s, 0xFFE46191.toInt(), 0xFFE34948.toInt(), Shader.TileMode.CLAMP)
                c.drawPath(path, fill)
            }
            "sparkle" -> {
                val path = Path()
                path.moveTo(s * 0.5f, s * 0.12f)
                path.quadTo(s * 0.5f, s * 0.5f, s * 0.88f, s * 0.5f)
                path.quadTo(s * 0.5f, s * 0.5f, s * 0.5f, s * 0.88f)
                path.quadTo(s * 0.5f, s * 0.5f, s * 0.12f, s * 0.5f)
                path.quadTo(s * 0.5f, s * 0.5f, s * 0.5f, s * 0.12f)
                path.close()
                fill.shader = LinearGradient(0f, 0f, s, s, 0xFFFDE047.toInt(), 0xFFF59E0B.toInt(), Shader.TileMode.CLAMP)
                c.drawPath(path, fill)
            }
            "flower" -> {
                fill.shader = LinearGradient(0f, 0f, s, s, 0xFFF472B6.toInt(), 0xFFEC4899.toInt(), Shader.TileMode.CLAMP)
                for (i in 0 until 5) {
                    val ang = i * (2.0 * Math.PI / 5.0)
                    val px = s / 2 + (cos(ang) * s * 0.22f).toFloat()
                    val py = s / 2 + (sin(ang) * s * 0.22f).toFloat()
                    c.drawCircle(px, py, s * 0.18f, fill)
                }
                fill.shader = null; fill.color = 0xFFFDE047.toInt()
                c.drawCircle(s / 2, s / 2, s * 0.14f, fill)
            }
            "leaf" -> {
                val path = Path()
                path.moveTo(s * 0.2f, s * 0.8f)
                path.quadTo(s * 0.2f, s * 0.2f, s * 0.8f, s * 0.2f)
                path.quadTo(s * 0.8f, s * 0.8f, s * 0.2f, s * 0.8f)
                path.close()
                fill.shader = LinearGradient(0f, s, s, 0f, 0xFF22C55E.toInt(), 0xFF10B981.toInt(), Shader.TileMode.CLAMP)
                c.drawPath(path, fill)
                line.color = Color.WHITE; line.strokeWidth = s * 0.04f
                c.drawLine(s * 0.25f, s * 0.75f, s * 0.75f, s * 0.25f, line)
            }
            "coffee" -> {
                disc(0xFF92400E.toInt(), 0xFF78350F.toInt())
                line.strokeWidth = s * 0.06f
                c.drawArc(RectF(s * 0.28f, s * 0.38f, s * 0.64f, s * 0.72f), 0f, 180f, false, line)
                c.drawLine(s * 0.28f, s * 0.38f, s * 0.64f, s * 0.38f, line)
                c.drawArc(RectF(s * 0.58f, s * 0.42f, s * 0.74f, s * 0.58f), -90f, 180f, false, line)
            }
            "sun" -> {
                disc(0xFFFBBF24.toInt(), 0xFFF59E0B.toInt())
                line.strokeWidth = s * 0.05f; line.color = 0xFFF59E0B.toInt()
                for (i in 0 until 8) {
                    val ang = i * (Math.PI / 4.0)
                    val x1 = s / 2 + (cos(ang) * s * 0.36f).toFloat()
                    val y1 = s / 2 + (sin(ang) * s * 0.36f).toFloat()
                    val x2 = s / 2 + (cos(ang) * s * 0.46f).toFloat()
                    val y2 = s / 2 + (sin(ang) * s * 0.46f).toFloat()
                    c.drawLine(x1, y1, x2, y2, line)
                }
            }

            // Badges & Flags
            "check" -> {
                disc(0xFF10B981.toInt(), 0xFF059669.toInt())
                c.drawPath(Path().apply { moveTo(s * 0.30f, s * 0.52f); lineTo(s * 0.44f, s * 0.66f); lineTo(s * 0.72f, s * 0.36f) }, line)
            }
            "cross" -> {
                disc(0xFFEF4444.toInt(), 0xFFDC2626.toInt())
                c.drawLine(s * 0.34f, s * 0.34f, s * 0.66f, s * 0.66f, line)
                c.drawLine(s * 0.66f, s * 0.34f, s * 0.34f, s * 0.66f, line)
            }
            "pin" -> {
                fill.color = 0xFFE11D48.toInt(); c.drawCircle(s / 2f, s * 0.36f, s * 0.22f, fill)
                fill.color = 0x33FFFFFF; c.drawCircle(s * 0.44f, s * 0.30f, s * 0.07f, fill)
                line.color = 0xFF4B5563.toInt(); line.strokeWidth = s * 0.05f
                c.drawLine(s / 2f, s * 0.58f, s / 2f, s * 0.92f, line)
            }
            "flag" -> {
                line.color = 0xFF374151.toInt(); line.strokeWidth = s * 0.05f
                c.drawLine(s * 0.24f, s * 0.12f, s * 0.24f, s * 0.90f, line)
                fill.shader = LinearGradient(0f, 0f, s, 0f, 0xFF3B82F6.toInt(), 0xFF8B5CF6.toInt(), Shader.TileMode.CLAMP)
                c.drawPath(Path().apply { moveTo(s * 0.26f, s * 0.14f); lineTo(s * 0.84f, s * 0.30f); lineTo(s * 0.26f, s * 0.48f); close() }, fill)
            }
            "verified" -> {
                disc(0xFF2563EB.toInt(), 0xFF1D4ED8.toInt())
                line.strokeWidth = s * 0.06f
                c.drawPath(Path().apply { moveTo(s * 0.32f, s * 0.52f); lineTo(s * 0.45f, s * 0.64f); lineTo(s * 0.70f, s * 0.38f) }, line)
            }
            "tag" -> {
                val path = Path()
                path.moveTo(s * 0.18f, s * 0.42f)
                path.lineTo(s * 0.42f, s * 0.18f)
                path.lineTo(s * 0.82f, s * 0.18f)
                path.lineTo(s * 0.82f, s * 0.58f)
                path.lineTo(s * 0.58f, s * 0.82f)
                path.close()
                fill.shader = LinearGradient(0f, 0f, s, s, 0xFFF97316.toInt(), 0xFFEA580C.toInt(), Shader.TileMode.CLAMP)
                c.drawPath(path, fill)
                fill.shader = null; fill.color = Color.WHITE
                c.drawCircle(s * 0.68f, s * 0.32f, s * 0.06f, fill)
            }
            "bookmark" -> {
                val path = Path()
                path.moveTo(s * 0.28f, s * 0.15f)
                path.lineTo(s * 0.72f, s * 0.15f)
                path.lineTo(s * 0.72f, s * 0.85f)
                path.lineTo(s * 0.50f, s * 0.66f)
                path.lineTo(s * 0.28f, s * 0.85f)
                path.close()
                fill.shader = LinearGradient(0f, 0f, 0f, s, 0xFFEC4899.toInt(), 0xFFBE185D.toInt(), Shader.TileMode.CLAMP)
                c.drawPath(path, fill)
            }

            // Arrows & Callouts
            "arrow" -> {
                line.color = 0xFF2563EB.toInt(); line.strokeWidth = s * 0.09f
                c.drawLine(s * 0.14f, s * 0.50f, s * 0.82f, s * 0.50f, line)
                c.drawPath(Path().apply { moveTo(s * 0.60f, s * 0.28f); lineTo(s * 0.84f, s * 0.50f); lineTo(s * 0.60f, s * 0.72f) }, line)
            }
            "arrow_curved" -> {
                line.color = 0xFF8B5CF6.toInt(); line.strokeWidth = s * 0.08f
                val arcPath = Path()
                arcPath.moveTo(s * 0.20f, s * 0.75f)
                arcPath.quadTo(s * 0.25f, s * 0.28f, s * 0.76f, s * 0.32f)
                c.drawPath(arcPath, line)
                c.drawPath(Path().apply { moveTo(s * 0.60f, s * 0.18f); lineTo(s * 0.82f, s * 0.32f); lineTo(s * 0.62f, s * 0.48f) }, line)
            }
            "arrow_double" -> {
                line.color = 0xFF0D9488.toInt(); line.strokeWidth = s * 0.07f
                c.drawLine(s * 0.20f, s * 0.50f, s * 0.80f, s * 0.50f, line)
                c.drawPath(Path().apply { moveTo(s * 0.35f, s * 0.35f); lineTo(s * 0.18f, s * 0.50f); lineTo(s * 0.35f, s * 0.65f) }, line)
                c.drawPath(Path().apply { moveTo(s * 0.65f, s * 0.35f); lineTo(s * 0.82f, s * 0.50f); lineTo(s * 0.65f, s * 0.65f) }, line)
            }
            "callout_speech" -> {
                val path = Path()
                path.addRoundRect(RectF(s * 0.15f, s * 0.20f, s * 0.85f, s * 0.68f), s * 0.15f, s * 0.15f, Path.Direction.CW)
                path.moveTo(s * 0.30f, s * 0.68f)
                path.lineTo(s * 0.22f, s * 0.85f)
                path.lineTo(s * 0.44f, s * 0.68f)
                fill.shader = LinearGradient(0f, 0f, s, s, 0xFF38BDF8.toInt(), 0xFF0284C7.toInt(), Shader.TileMode.CLAMP)
                c.drawPath(path, fill)
            }
            "callout_thought" -> {
                fill.shader = LinearGradient(0f, 0f, s, s, 0xFFA78BFA.toInt(), 0xFF7C3AED.toInt(), Shader.TileMode.CLAMP)
                c.drawCircle(s * 0.50f, s * 0.42f, s * 0.26f, fill)
                c.drawCircle(s * 0.32f, s * 0.46f, s * 0.18f, fill)
                c.drawCircle(s * 0.68f, s * 0.46f, s * 0.18f, fill)
                c.drawCircle(s * 0.26f, s * 0.72f, s * 0.08f, fill)
                c.drawCircle(s * 0.20f, s * 0.82f, s * 0.05f, fill)
            }
            "bracket_left" -> {
                line.color = 0xFFF59E0B.toInt(); line.strokeWidth = s * 0.08f
                val p = Path()
                p.moveTo(s * 0.65f, s * 0.18f)
                p.quadTo(s * 0.35f, s * 0.18f, s * 0.35f, s * 0.38f)
                p.lineTo(s * 0.35f, s * 0.46f)
                p.lineTo(s * 0.20f, s * 0.50f)
                p.lineTo(s * 0.35f, s * 0.54f)
                p.lineTo(s * 0.35f, s * 0.62f)
                p.quadTo(s * 0.35f, s * 0.82f, s * 0.65f, s * 0.82f)
                c.drawPath(p, line)
            }

            // Icons & Diagrams
            "bulb" -> {
                disc(0xFFFBBF24.toInt(), 0xFFD97706.toInt())
                line.strokeWidth = s * 0.06f
                c.drawCircle(s / 2f, s * 0.44f, s * 0.16f, line)
                c.drawLine(s * 0.43f, s * 0.68f, s * 0.57f, s * 0.68f, line)
            }
            "lightning" -> {
                val path = Path()
                path.moveTo(s * 0.55f, s * 0.12f)
                path.lineTo(s * 0.28f, s * 0.52f)
                path.lineTo(s * 0.50f, s * 0.52f)
                path.lineTo(s * 0.45f, s * 0.88f)
                path.lineTo(s * 0.74f, s * 0.44f)
                path.lineTo(s * 0.52f, s * 0.44f)
                path.close()
                fill.shader = LinearGradient(0f, 0f, 0f, s, 0xFFFBBF24.toInt(), 0xFFEA580C.toInt(), Shader.TileMode.CLAMP)
                c.drawPath(path, fill)
            }
            "chart_up" -> {
                disc(0xFF0284C7.toInt(), 0xFF0369A1.toInt())
                line.strokeWidth = s * 0.06f
                val p = Path()
                p.moveTo(s * 0.25f, s * 0.65f)
                p.lineTo(s * 0.45f, s * 0.50f)
                p.lineTo(s * 0.58f, s * 0.58f)
                p.lineTo(s * 0.75f, s * 0.35f)
                c.drawPath(p, line)
            }
            "target" -> {
                disc(0xFFEF4444.toInt(), 0xFFB91C1C.toInt())
                fill.color = Color.WHITE; c.drawCircle(s / 2f, s / 2f, s * 0.30f, fill)
                fill.color = 0xFFEF4444.toInt(); c.drawCircle(s / 2f, s / 2f, s * 0.16f, fill)
            }
            "cloud" -> {
                fill.shader = LinearGradient(0f, 0f, s, s, 0xFF38BDF8.toInt(), 0xFF0284C7.toInt(), Shader.TileMode.CLAMP)
                c.drawCircle(s * 0.45f, s * 0.45f, s * 0.22f, fill)
                c.drawCircle(s * 0.65f, s * 0.50f, s * 0.18f, fill)
                c.drawRoundRect(RectF(s * 0.22f, s * 0.48f, s * 0.78f, s * 0.72f), s * 0.12f, s * 0.12f, fill)
            }
            "key" -> {
                disc(0xFFF59E0B.toInt(), 0xFFD97706.toInt())
                line.strokeWidth = s * 0.06f
                c.drawCircle(s * 0.40f, s * 0.40f, s * 0.14f, line)
                c.drawLine(s * 0.50f, s * 0.50f, s * 0.72f, s * 0.72f, line)
                c.drawLine(s * 0.65f, s * 0.65f, s * 0.72f, s * 0.58f, line)
            }

            // Study & Academic
            "atom" -> {
                disc(0xFF6366F1.toInt(), 0xFF4338CA.toInt())
                line.strokeWidth = s * 0.04f
                c.save()
                c.rotate(30f, s / 2f, s / 2f)
                c.drawOval(RectF(s * 0.22f, s * 0.40f, s * 0.78f, s * 0.60f), line)
                c.rotate(60f, s / 2f, s / 2f)
                c.drawOval(RectF(s * 0.22f, s * 0.40f, s * 0.78f, s * 0.60f), line)
                c.rotate(60f, s / 2f, s / 2f)
                c.drawOval(RectF(s * 0.22f, s * 0.40f, s * 0.78f, s * 0.60f), line)
                c.restore()
                fill.color = Color.WHITE; c.drawCircle(s / 2f, s / 2f, s * 0.08f, fill)
            }
            "flask" -> {
                disc(0xFF0D9488.toInt(), 0xFF0F766E.toInt())
                line.strokeWidth = s * 0.05f
                val p = Path()
                p.moveTo(s * 0.44f, s * 0.25f)
                p.lineTo(s * 0.56f, s * 0.25f)
                p.lineTo(s * 0.56f, s * 0.42f)
                p.lineTo(s * 0.72f, s * 0.72f)
                p.lineTo(s * 0.28f, s * 0.72f)
                p.lineTo(s * 0.44f, s * 0.42f)
                p.close()
                c.drawPath(p, line)
            }
            "graduation" -> {
                disc(0xFF1E293B.toInt(), 0xFF0F172A.toInt())
                val cap = Path()
                cap.moveTo(s * 0.50f, s * 0.30f)
                cap.lineTo(s * 0.82f, s * 0.45f)
                cap.lineTo(s * 0.50f, s * 0.60f)
                cap.lineTo(s * 0.18f, s * 0.45f)
                cap.close()
                fill.color = 0xFFFCD34D.toInt(); c.drawPath(cap, fill)
                line.color = 0xFFFCD34D.toInt(); line.strokeWidth = s * 0.05f
                c.drawArc(RectF(s * 0.32f, s * 0.52f, s * 0.68f, s * 0.74f), 0f, 180f, false, line)
            }
            "pencil" -> {
                disc(0xFFF59E0B.toInt(), 0xFFB45309.toInt())
                line.strokeWidth = s * 0.06f
                c.drawLine(s * 0.28f, s * 0.72f, s * 0.68f, s * 0.32f, line)
                c.drawCircle(s * 0.72f, s * 0.28f, s * 0.06f, line)
            }
            "citation" -> {
                disc(0xFF8B5CF6.toInt(), 0xFF6D28D9.toInt())
                val t = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE; textSize = s * 0.55f; textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                }
                c.drawText("“", s / 2f, s * 0.68f, t)
            }

            // Planner & Dates
            "calendar" -> {
                disc(0xFFEF4444.toInt(), 0xFFDC2626.toInt())
                line.strokeWidth = s * 0.05f
                c.drawRoundRect(RectF(s * 0.28f, s * 0.32f, s * 0.72f, s * 0.74f), s * 0.06f, s * 0.06f, line)
                c.drawLine(s * 0.28f, s * 0.46f, s * 0.72f, s * 0.46f, line)
                c.drawLine(s * 0.38f, s * 0.26f, s * 0.38f, s * 0.34f, line)
                c.drawLine(s * 0.62f, s * 0.26f, s * 0.62f, s * 0.34f, line)
            }
            "clock" -> {
                disc(0xFF3B82F6.toInt(), 0xFF1D4ED8.toInt())
                line.strokeWidth = s * 0.06f
                c.drawCircle(s / 2f, s / 2f, s * 0.28f, line)
                c.drawLine(s / 2f, s / 2f, s / 2f, s * 0.32f, line)
                c.drawLine(s / 2f, s / 2f, s * 0.64f, s / 2f, line)
            }
            "priority" -> {
                val path = Path()
                path.moveTo(s * 0.50f, s * 0.15f)
                path.lineTo(s * 0.85f, s * 0.50f)
                path.lineTo(s * 0.50f, s * 0.85f)
                path.lineTo(s * 0.15f, s * 0.50f)
                path.close()
                fill.shader = LinearGradient(0f, 0f, s, s, 0xFFE11D48.toInt(), 0xFFBE123C.toInt(), Shader.TileMode.CLAMP)
                c.drawPath(path, fill)
                val t = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE; textSize = s * 0.32f; textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                }
                c.drawText("P1", s / 2f, s * 0.62f, t)
            }

            else -> {
                disc(0xFF6366F1.toInt(), 0xFF4F46E5.toInt())
                line.strokeWidth = s * 0.07f
                c.drawCircle(s / 2f, s / 2f, s * 0.24f, line)
            }
        }
        return bmp
    }
}
