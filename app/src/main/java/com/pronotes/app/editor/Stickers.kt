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
    data class Builtin(val id: String, val label: String) : Sticker
    data class Emoji(val text: String) : Sticker
    data class Custom(val file: File) : Sticker
}

/**
 * Sticker library: a set of planner-style stickers drawn in code (labels, badges,
 * arrows, check marks), emoji, and stickers the user imports from photos.
 */
object StickerLibrary {
    val builtins = listOf(
        Sticker.Builtin("important", "Important"), Sticker.Builtin("todo", "To do"), Sticker.Builtin("done", "Done"),
        Sticker.Builtin("note", "Note"), Sticker.Builtin("idea", "Idea"), Sticker.Builtin("due", "Due"),
        Sticker.Builtin("exam", "Exam"), Sticker.Builtin("review", "Review"),
        Sticker.Builtin("star", "Star"), Sticker.Builtin("heart", "Heart"), Sticker.Builtin("check", "Check"),
        Sticker.Builtin("cross", "Cross"), Sticker.Builtin("arrow", "Arrow"), Sticker.Builtin("pin", "Pin"),
        Sticker.Builtin("bulb", "Bulb"), Sticker.Builtin("flag", "Flag"),
    )

    private val labelColors = mapOf(
        "important" to (0xFFE34948.toInt() to 0xFFD55181.toInt()), "todo" to (0xFF2A78D6.toInt() to 0xFF7161E0.toInt()),
        "done" to (0xFF0CA30C.toInt() to 0xFF1BAF7A.toInt()), "note" to (0xFFEDA100.toInt() to 0xFFEB6834.toInt()),
        "idea" to (0xFF8173E3.toInt() to 0xFFC04873.toInt()), "due" to (0xFFD95926.toInt() to 0xFFE34948.toInt()),
        "exam" to (0xFF256ABF.toInt() to 0xFF0F7E5C.toInt()), "review" to (0xFF5F5E5A.toInt() to 0xFF898781.toInt()),
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
     * 2. Samples border color clusters (supporting uniform, studio, paper, or gradient backgrounds).
     * 3. Runs edge-aware BFS flood-fill from all 4 edges to separate background from foreground.
     * 4. Verifies a coherent recognizable subject (coverage 5%-86%, dominant subject component >= 52%
     *    of foreground, and meaningful foreground/background color separation).
     * 5. Applies soft edge feathering and tight bounding-box cropping so only the subject is imported.
     */
    private fun extractRecognizableSubject(bmp: Bitmap): Bitmap? {
        val w = bmp.width
        val h = bmp.height
        if (w < 16 || h < 16) return null
        val total = w * h
        val pixels = IntArray(total)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)

        // If already has significant alpha cutout, keep as-is
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

        // Build up to 3 representative border color seeds (handles gradients / two-tone backdrops)
        val seeds = ArrayList<Int>(3)
        seeds += borderColors.first()
        for (c in borderColors) {
            if (seeds.size >= 3) break
            if (seeds.all { colorDistSq(it, c) > 42 * 42 }) seeds += c
        }
        // Refine cluster centroids (2 quick iterations)
        val seedR = DoubleArray(seeds.size) { Color.red(seeds[it]).toDouble() }
        val seedG = DoubleArray(seeds.size) { Color.green(seeds[it]).toDouble() }
        val seedB = DoubleArray(seeds.size) { Color.blue(seeds[it]).toDouble() }
        repeat(2) {
            val sumR = DoubleArray(seeds.size)
            val sumG = DoubleArray(seeds.size)
            val sumB = DoubleArray(seeds.size)
            val cnt = IntArray(seeds.size)
            for (c in borderColors) {
                val r = Color.red(c).toDouble()
                val g = Color.green(c).toDouble()
                val b = Color.blue(c).toDouble()
                var best = 0
                var bestD = Double.MAX_VALUE
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
                val d = colorDistSq(c, s)
                if (d < md) md = d
            }
            borderDistSum += kotlin.math.sqrt(md.toDouble())
        }
        val avgBorderSpread = (borderDistSum / borderColors.size).toFloat()
        // If border is extremely chaotic (busy natural photo with no separable backdrop), don't force bad cutout
        if (avgBorderSpread > 54f) return null

        val seedThresh = (32f + avgBorderSpread * 1.15f).coerceIn(30f, 68f)
        val seedThreshSq = (seedThresh * seedThresh).toInt()
        val localStepThreshSq = (26 * 26)

        // Edge-aware BFS flood fill from all 4 image borders
        val isBg = BooleanArray(total)
        val queue = IntArray(total)
        var qHead = 0
        var qTail = 0

        fun matchesBgSeed(c: Int): Boolean {
            for (s in bgSeeds) {
                if (colorDistSq(c, s) <= seedThreshSq) return true
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
                // Must be reasonably close to a border seed AND not cross a sharp edge from current bg pixel
                if (matchesBgSeed(nc) || (colorDistSq(curC, nc) <= localStepThreshSq && minSeedDistSq(nc, bgSeeds) <= (seedThreshSq * 1.55f).toInt())) {
                    isBg[nIdx] = true
                    queue[qTail++] = nIdx
                }
            }
            if (cx > 0) tryNeighbor(cx - 1, cy)
            if (cx + 1 < w) tryNeighbor(cx + 1, cy)
            if (cy > 0) tryNeighbor(cx, cy - 1)
            if (cy + 1 < h) tryNeighbor(cx, cy + 1)
        }

        // Check foreground coverage
        var fgCount = 0
        for (i in 0 until total) if (!isBg[i]) fgCount++
        val fgRatio = fgCount.toFloat() / total.toFloat()
        if (fgRatio < 0.045f || fgRatio > 0.87f) return null

        // Verify subject coherence: largest connected foreground component should dominate
        val visited = BooleanArray(total)
        var largestComp = 0
        var largestSeedIdx = -1
        for (i in 0 until total) {
            if (isBg[i] || visited[i]) continue
            var head = 0
            var tail = 0
            visited[i] = true
            queue[tail++] = i
            while (head < tail) {
                val cur = queue[head++]
                val cx = cur % w
                val cy = cur / w
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
        if (largestSeedIdx < 0 || largestComp.toFloat() / fgCount.toFloat() < 0.50f) return null

        // Compute alpha mask with 1-pass soft edge feathering & drop tiny stray background specks
        val alpha = IntArray(total) { i -> if (isBg[i]) 0 else 255 }
        val feathered = IntArray(total)
        var minX = w; var minY = h; var maxX = 0; var maxY = 0
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                val idx = row + x
                val a0 = alpha[idx]
                if (a0 == 0) {
                    feathered[idx] = 0
                    continue
                }
                // Count background neighbors in 3x3 window for smooth anti-aliased cutout border
                var bgNeighbors = 0
                if (x > 0 && alpha[idx - 1] == 0) bgNeighbors++
                if (x + 1 < w && alpha[idx + 1] == 0) bgNeighbors++
                if (y > 0 && alpha[idx - w] == 0) bgNeighbors++
                if (y + 1 < h && alpha[idx + w] == 0) bgNeighbors++
                val outA = when {
                    bgNeighbors >= 3 -> 70
                    bgNeighbors == 2 -> 165
                    bgNeighbors == 1 -> 225
                    else -> 255
                }
                feathered[idx] = outA
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
                    outPixels[dstRow + (x - minX)] = (pixels[srcRow + x] and 0x00FFFFFF) or (a shl 24)
                }
            }
        }
        return Bitmap.createBitmap(cropW, cropH, Bitmap.Config.ARGB_8888).apply {
            setPixels(outPixels, 0, cropW, 0, 0, cropW, cropH)
        }
    }

    private fun colorDistSq(c1: Int, c2: Int): Int {
        val dr = Color.red(c1) - Color.red(c2)
        val dg = Color.green(c1) - Color.green(c2)
        val db = Color.blue(c1) - Color.blue(c2)
        // Perceptually weighted RGB distance
        return (dr * dr * 3 + dg * dg * 4 + db * db * 2) / 3
    }

    private fun minSeedDistSq(c: Int, seeds: IntArray): Int {
        var best = Int.MAX_VALUE
        for (s in seeds) {
            val d = colorDistSq(c, s)
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
                color = Color.WHITE; textSize = h * 0.42f; textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); letterSpacing = 0.06f
            }
            val fm = t.fontMetrics
            c.drawText(builtins.first { it.id == id }.label.uppercase(), w / 2f, h / 2f - (fm.ascent + fm.descent) / 2f, t)
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
            c.drawCircle(s / 2, s / 2, s * 0.44f, fill)
            fill.shader = null
        }
        when (id) {
            "star" -> {
                val path = Path()
                for (i in 0 until 10) {
                    val a = -Math.PI / 2 + i * Math.PI / 5
                    val k = if (i % 2 == 0) 0.46f else 0.2f
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
            "check" -> { disc(0xFF35B231.toInt(), 0xFF0F7E5C.toInt()); c.drawPath(Path().apply { moveTo(s * 0.3f, s * 0.52f); lineTo(s * 0.44f, s * 0.66f); lineTo(s * 0.72f, s * 0.36f) }, line) }
            "cross" -> {
                disc(0xFFE34948.toInt(), 0xFFB93535.toInt())
                c.drawLine(s * 0.34f, s * 0.34f, s * 0.66f, s * 0.66f, line); c.drawLine(s * 0.66f, s * 0.34f, s * 0.34f, s * 0.66f, line)
            }
            "arrow" -> {
                line.color = 0xFF2A78D6.toInt(); line.strokeWidth = s * 0.09f
                c.drawLine(s * 0.14f, s * 0.5f, s * 0.82f, s * 0.5f, line)
                c.drawPath(Path().apply { moveTo(s * 0.6f, s * 0.28f); lineTo(s * 0.84f, s * 0.5f); lineTo(s * 0.6f, s * 0.72f) }, line)
            }
            "pin" -> {
                fill.color = 0xFFE34948.toInt(); c.drawCircle(s / 2, s * 0.36f, s * 0.22f, fill)
                fill.color = 0x33FFFFFF; c.drawCircle(s * 0.44f, s * 0.3f, s * 0.07f, fill)
                line.color = 0xFF5F5E5A.toInt(); line.strokeWidth = s * 0.05f; c.drawLine(s / 2, s * 0.58f, s / 2, s * 0.92f, line)
            }
            "bulb" -> {
                disc(0xFFFAB219.toInt(), 0xFFEDA100.toInt())
                line.strokeWidth = s * 0.06f
                c.drawCircle(s / 2, s * 0.44f, s * 0.16f, line)
                c.drawLine(s * 0.43f, s * 0.68f, s * 0.57f, s * 0.68f, line)
            }
            else -> { // flag
                line.color = 0xFF383835.toInt(); line.strokeWidth = s * 0.05f; c.drawLine(s * 0.24f, s * 0.12f, s * 0.24f, s * 0.9f, line)
                fill.shader = LinearGradient(0f, 0f, s, 0f, 0xFF2A78D6.toInt(), 0xFF7161E0.toInt(), Shader.TileMode.CLAMP)
                c.drawPath(Path().apply { moveTo(s * 0.26f, s * 0.14f); lineTo(s * 0.84f, s * 0.3f); lineTo(s * 0.26f, s * 0.48f); close() }, fill)
            }
        }
        return bmp
    }
}
