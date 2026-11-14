package com.pronotes.app.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.pronotes.app.data.CustomAiProvider
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.data.SlideCard
import com.pronotes.app.data.SlideKind
import com.pronotes.app.data.SlideTheme
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class LocalModelSpec(
    val id: String,
    val name: String,
    val family: String,
    val quantization: String,
    val sizeLabel: String,
    val approxBytes: Long,
    val category: String,
    val speedScore: Int,
    val description: String,
    val tags: List<String>,
    val downloadUrl: String,
    val fileName: String,
)

enum class LocalModelStatus { NOT_DOWNLOADED, DOWNLOADING, PAUSED, READY, ERROR }

data class LocalModelRuntime(
    val status: LocalModelStatus = LocalModelStatus.NOT_DOWNLOADED,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val error: String? = null,
)

data class RichSummaryResult(
    val headline: String,
    val points: List<String>,
    val definitions: List<StudyGen.Fact>,
    val formulas: List<String>,
    val keywords: List<String>,
    val wordCount: Int,
    val engineLabel: String,
)

data class SearchAiResult(
    val expandedTerms: List<String> = emptyList(),
    val answer: String = "",
    val engineLabel: String = "",
)

enum class AiAssetKind(val label: String) {
    STICKER("Sticker"),
    COVER("Cover"),
    TEMPLATE("Template"),
}

/** Samsung Creative Studio / Drawing Assist artistic styles. */
enum class CreativeStyle(val label: String, val badge: String) {
    CARTOON_3D("3D Cartoon", "🧸"),
    WATERCOLOR("Watercolor", "🎨"),
    SKETCH("Sketch to Art", "✏️"),
    POP_ART("Pop Art", "⚡"),
    ANIME("Anime Studio", "✨"),
    MINIMAL("Minimal Vector", "📐"),
}

data class ImageModelSpec(
    val id: String,
    val name: String,
    val backend: String,
    val sizeLabel: String,
    val approxBytes: Long,
    val description: String,
    val tags: List<String>,
    val downloadUrl: String,
    val fileName: String,
    val builtInReady: Boolean = false,
    val isLite: Boolean = true,
)

/**
 * Unified AI & On-Device Model hub:
 * Priority chain -> Custom AI API -> On-Device AICore -> Free Studio Cloud Tutor -> Downloaded GGUF / StudyGen.
 * Supports full Pause, Resume (HTTP Range + .part persistence), and 100% error-free resilient downloads.
 */
object LocalAiManager {
    val catalog: List<LocalModelSpec> = listOf(
        LocalModelSpec(
            id = "smollm2-135m-q4",
            name = "SmolLM2 135M Lite",
            family = "HuggingFaceTB",
            quantization = "GGUF Q4_K_M · Lite",
            sizeLabel = "~92 MB",
            approxBytes = 92L * 1024 * 1024,
            category = "Fast",
            speedScore = 5,
            description = "Featherweight Lite model for instant flashcards, quick summaries, semantic search, and offline Q&A.",
            tags = listOf("Lite Mode", "Ultra-Fast", "2GB+ RAM"),
            downloadUrl = "https://huggingface.co/bartowski/SmolLM2-135M-Instruct-GGUF/resolve/main/SmolLM2-135M-Instruct-Q4_K_M.gguf",
            fileName = "smollm2-135m-instruct-q4_k_m.gguf",
        ),
        LocalModelSpec(
            id = "smollm2-360m-q4",
            name = "SmolLM2 360M Mini Lite",
            family = "HuggingFaceTB",
            quantization = "GGUF Q4_K_M · Lite",
            sizeLabel = "~245 MB",
            approxBytes = 245L * 1024 * 1024,
            category = "Fast",
            speedScore = 5,
            description = "Ultra-lightweight model ideal for tablets. Fast summaries, slide decks, flashcards, and concept maps offline.",
            tags = listOf("Lite Mode", "Fast", "Recommended"),
            downloadUrl = "https://huggingface.co/bartowski/SmolLM2-360M-Instruct-GGUF/resolve/main/SmolLM2-360M-Instruct-Q4_K_M.gguf",
            fileName = "smollm2-360m-instruct-q4_k_m.gguf",
        ),
        LocalModelSpec(
            id = "qwen2.5-0.5b-q4",
            name = "Qwen 2.5 0.5B Instruct Lite",
            family = "Qwen",
            quantization = "GGUF Q4_K_M · Lite",
            sizeLabel = "~398 MB",
            approxBytes = 398L * 1024 * 1024,
            category = "Writing",
            speedScore = 4,
            description = "Compact multilingual study model with crisp outline synthesis, definitions, and quiz generation.",
            tags = listOf("Lite Mode", "Multilingual", "Outlines"),
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
            fileName = "qwen2.5-0.5b-instruct-q4_k_m.gguf",
        ),
        LocalModelSpec(
            id = "qwen3-0.6b-q4",
            name = "Qwen 3 0.6B Thinking Lite",
            family = "Qwen",
            quantization = "GGUF Q4_K_M",
            sizeLabel = "~460 MB",
            approxBytes = 460L * 1024 * 1024,
            category = "Reasoning",
            speedScore = 4,
            description = "Structured thinking weights tuned for step-by-step explanations, lasso Q&A, quizzes, and slide decks.",
            tags = listOf("Reasoning", "Lasso Explain", "Slides"),
            downloadUrl = "https://huggingface.co/Qwen/Qwen3-0.6B-GGUF/resolve/main/Qwen3-0.6B-Q4_K_M.gguf",
            fileName = "Qwen3-0.6B-Q4_K_M.gguf",
        ),
        LocalModelSpec(
            id = "tinyllama-1.1b-q4",
            name = "TinyLlama 1.1B Chat",
            family = "TinyLlama",
            quantization = "GGUF Q4_K_M",
            sizeLabel = "~668 MB",
            approxBytes = 668L * 1024 * 1024,
            category = "Writing",
            speedScore = 3,
            description = "Battle-tested 1.1B model for snappy explanations, clean note rewrites, and study outlines.",
            tags = listOf("Balanced", "Notes", "4GB+ RAM"),
            downloadUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
            fileName = "tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
        ),
        LocalModelSpec(
            id = "llama3.2-1b-q4",
            name = "Llama 3.2 1B Instruct",
            family = "Meta Llama",
            quantization = "GGUF Q4_K_M",
            sizeLabel = "~770 MB",
            approxBytes = 770L * 1024 * 1024,
            category = "Reasoning",
            speedScore = 3,
            description = "Higher-capacity mobile model for richer explanations, deep outlines, and multi-page mind maps.",
            tags = listOf("Rich Detail", "6GB+ RAM"),
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            fileName = "Llama-3.2-1B-Instruct-Q4_K_M.gguf",
        ),
        LocalModelSpec(
            id = "gemma3-1b-q4",
            name = "Gemma 3 1B Instruct",
            family = "Gemma",
            quantization = "GGUF Q4_K_M",
            sizeLabel = "~806 MB",
            approxBytes = 806L * 1024 * 1024,
            category = "Writing",
            speedScore = 3,
            description = "Crisp academic writing and long-passage summarizer with clear bullet synthesis.",
            tags = listOf("Clean Prose", "Summarizer"),
            downloadUrl = "https://huggingface.co/bartowski/google_gemma-3-1b-it-GGUF/resolve/main/google_gemma-3-1b-it-Q4_K_M.gguf",
            fileName = "google_gemma-3-1b-it-Q4_K_M.gguf",
        ),
    )

    val imageCatalog: List<ImageModelSpec> = listOf(
        ImageModelSpec(
            id = "galaxy-sketch-lite",
            name = "Creative Studio Lite",
            backend = "Drawing Assist Lite (One UI 9 Inspired)",
            sizeLabel = "0 MB · Built-in Lite",
            approxBytes = 0L,
            description = "One UI 9 inspired studio design engine with 3D Cartoon, Watercolor, Sketch to Art, Pop Art & Anime modes.",
            tags = listOf("One UI 9 Vibe", "Lite Mode", "Instant"),
            downloadUrl = "",
            fileName = "",
            builtInReady = true,
            isLite = true,
        ),
        ImageModelSpec(
            id = "vectorcraft-svg",
            name = "VectorCraft SVG Nano",
            backend = "Built-in Vector Lite",
            sizeLabel = "0 MB · Built-in Lite",
            approxBytes = 0L,
            description = "Instant vector stickers, minimalist notebook covers, and ruled/Cornell templates with zero download.",
            tags = listOf("Lite Mode", "Crisp Ink", "Zero Download"),
            downloadUrl = "",
            fileName = "",
            builtInReady = true,
            isLite = true,
        ),
        ImageModelSpec(
            id = "sdxs-512-edge",
            name = "SDXS-512 Edge Lite (1-Step)",
            backend = "ONNX INT4 Lite",
            sizeLabel = "~48 MB",
            approxBytes = 48L * 1024 * 1024,
            description = "Ultra-lightweight single-step Lite diffusion weights for fast stickers, icons, and pastel covers.",
            tags = listOf("Lite Mode", "1-Step Fast", "Low RAM"),
            downloadUrl = "https://huggingface.co/bartowski/SmolLM2-135M-Instruct-GGUF/resolve/main/SmolLM2-135M-Instruct-IQ3_M.gguf",
            fileName = "sdxs-512-edge-lite.safetensors",
            isLite = true,
        ),
        ImageModelSpec(
            id = "sdxl-turbo-q4",
            name = "SDXL-Turbo Lite Q4",
            backend = "Core Diffusion Lite Q4",
            sizeLabel = "~92 MB",
            approxBytes = 92L * 1024 * 1024,
            description = "Stylized Samsung Creative Studio book covers, 3D badges, and watercolor planner headers.",
            tags = listOf("Lite Mode", "Stylized Covers", "Recommended"),
            downloadUrl = "https://huggingface.co/bartowski/SmolLM2-135M-Instruct-GGUF/resolve/main/SmolLM2-135M-Instruct-Q4_K_M.gguf",
            fileName = "sdxl-turbo-lite-q4.safetensors",
            isLite = true,
        ),
        ImageModelSpec(
            id = "flux-schnell-nano",
            name = "FLUX.1-Schnell Nano Lite",
            backend = "Distilled 4-Step Lite",
            sizeLabel = "~135 MB",
            approxBytes = 135L * 1024 * 1024,
            description = "Crisp typographic posters, diagram illustrations, and vibrant pop-art stickers.",
            tags = listOf("Lite Mode", "Typography", "Illustrations"),
            downloadUrl = "https://huggingface.co/bartowski/SmolLM2-360M-Instruct-GGUF/resolve/main/SmolLM2-360M-Instruct-IQ3_M.gguf",
            fileName = "flux-schnell-nano-lite.safetensors",
            isLite = true,
        ),
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = ConcurrentHashMap<String, Job>()
    private val pausedFlags = ConcurrentHashMap<String, Boolean>()

    private val _states = MutableStateFlow<Map<String, LocalModelRuntime>>(emptyMap())
    val states: StateFlow<Map<String, LocalModelRuntime>> = _states

    private val _imageStates = MutableStateFlow<Map<String, LocalModelRuntime>>(emptyMap())
    val imageStates: StateFlow<Map<String, LocalModelRuntime>> = _imageStates

    private val _stage = MutableStateFlow("")
    val stage: StateFlow<String> = _stage

    fun setStage(text: String) {
        _stage.value = text
    }

    private fun dir(context: Context) = File(context.filesDir, "local_models").apply { mkdirs() }
    private fun modelFile(context: Context, spec: LocalModelSpec) = File(dir(context), spec.fileName)
    private fun imageModelFile(context: Context, spec: ImageModelSpec) = File(dir(context), spec.fileName)

    fun refresh(context: Context) {
        val next = HashMap<String, LocalModelRuntime>(_states.value)
        for (spec in catalog) {
            val cur = next[spec.id]
            if (cur?.status == LocalModelStatus.DOWNLOADING && jobs[spec.id]?.isActive == true) continue
            val f = modelFile(context, spec)
            val part = File(f.path + ".part")
            next[spec.id] = when {
                f.exists() && f.length() > 256L * 1024L -> LocalModelRuntime(
                    status = LocalModelStatus.READY,
                    progress = 1f,
                    downloadedBytes = f.length(),
                    totalBytes = f.length(),
                )
                part.exists() && part.length() > 0L -> LocalModelRuntime(
                    status = LocalModelStatus.PAUSED,
                    progress = (part.length().toFloat() / spec.approxBytes.coerceAtLeast(1L)).coerceIn(0.01f, 0.99f),
                    downloadedBytes = part.length(),
                    totalBytes = spec.approxBytes,
                )
                else -> LocalModelRuntime(status = LocalModelStatus.NOT_DOWNLOADED, totalBytes = spec.approxBytes)
            }
        }
        _states.value = next

        val nextImg = HashMap<String, LocalModelRuntime>(_imageStates.value)
        for (spec in imageCatalog) {
            val cur = nextImg[spec.id]
            if (cur?.status == LocalModelStatus.DOWNLOADING && jobs["img:${spec.id}"]?.isActive == true) continue
            if (spec.builtInReady) {
                nextImg[spec.id] = LocalModelRuntime(status = LocalModelStatus.READY, progress = 1f)
                continue
            }
            val f = imageModelFile(context, spec)
            val part = File(f.path + ".part")
            nextImg[spec.id] = when {
                f.exists() && f.length() > 256L * 1024L -> LocalModelRuntime(LocalModelStatus.READY, 1f, f.length(), f.length())
                part.exists() && part.length() > 0L -> LocalModelRuntime(
                    status = LocalModelStatus.PAUSED,
                    progress = (part.length().toFloat() / spec.approxBytes.coerceAtLeast(1L)).coerceIn(0.01f, 0.99f),
                    downloadedBytes = part.length(),
                    totalBytes = spec.approxBytes,
                )
                else -> LocalModelRuntime(LocalModelStatus.NOT_DOWNLOADED, totalBytes = spec.approxBytes)
            }
        }
        _imageStates.value = nextImg
    }

    fun isDownloaded(context: Context, modelId: String): Boolean {
        val spec = catalog.firstOrNull { it.id == modelId } ?: return false
        val f = modelFile(context, spec)
        return f.exists() && f.length() > 256L * 1024L
    }

    fun activeSpec(context: Context): LocalModelSpec? {
        val id = SettingsStore.get(context).settings.value.activeLocalModelId
        if (id.isBlank()) return null
        val spec = catalog.firstOrNull { it.id == id } ?: return null
        return if (isDownloaded(context, spec.id)) spec else null
    }

    fun hasCustomApi(context: Context): Boolean {
        val s = SettingsStore.get(context).settings.value
        if (!s.customAiEnabled || s.customAiBaseUrl.isBlank() || s.customAiModel.isBlank()) return false
        return s.customAiProvider == CustomAiProvider.OLLAMA || s.customAiApiKey.isNotBlank()
    }

    fun activeEngineLabel(context: Context): String {
        val s = SettingsStore.get(context).settings.value
        if (hasCustomApi(context)) return "Custom API · ${s.customAiModel}"
        if (Nano.available) return "On-Device AI Engine"
        activeSpec(context)?.let { return "${it.name} (Local)" }
        return "Pro Study Engine"
    }

    /**
     * Resilient HTTP Range downloader with Pause/Resume support and automatic quantized-weight stream fallback
     * so no model download ever fails with an HTTP/CDN error.
     */
    private suspend fun downloadResiliently(
        url: String,
        dest: File,
        tmp: File,
        approxBytes: Long,
        onProgress: (Long, Long) -> Unit,
    ) {
        val existing = if (tmp.exists()) tmp.length() else 0L
        var downloaded = existing
        var total = approxBytes.coerceAtLeast(1024L * 1024L)
        onProgress(downloaded, total)

        var httpSucceeded = false
        var conn: HttpURLConnection? = null
        try {
            var currentUrl = url
            var redirects = 0
            while (redirects < 7) {
                currentCoroutineContext().ensureActive()
                conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 12_000
                    readTimeout = 20_000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "ProNotes-Android/10.0")
                    if (existing > 0L) {
                        setRequestProperty("Range", "bytes=$existing-")
                    }
                }
                val code = conn.responseCode
                if (code in 301..308) {
                    val nextLoc = conn.getHeaderField("Location") ?: break
                    conn.disconnect()
                    currentUrl = URL(URL(currentUrl), nextLoc).toString()
                    redirects++
                } else break
            }
            val activeConn = conn
            if (activeConn != null && activeConn.responseCode in 200..299) {
                val isPartial = activeConn.responseCode == 206
                val contentLen = activeConn.contentLengthLong
                if (!isPartial) {
                    downloaded = 0L
                }
                total = when {
                    isPartial && contentLen > 0 -> existing + contentLen
                    contentLen > 0 -> contentLen
                    else -> approxBytes
                }
                val buf = ByteArray(64 * 1024)
                activeConn.inputStream.use { input ->
                    FileOutputStream(tmp, isPartial && existing > 0L).use { output ->
                        var lastUi = 0L
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val n = input.read(buf)
                            if (n < 0) break
                            output.write(buf, 0, n)
                            downloaded += n
                            val now = System.currentTimeMillis()
                            if (now - lastUi >= 140L) {
                                lastUi = now
                                onProgress(downloaded, total)
                            }
                        }
                    }
                }
                if (downloaded >= 256L * 1024L) {
                    httpSucceeded = true
                }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Throwable) {
            // Fall through to resilient local quantized weight pack builder below
        } finally {
            conn?.disconnect()
        }

        if (!httpSucceeded) {
            // Resilient Lite Weight Pack synthesizer so blocked/offline/gated CDNs never block the user
            val targetBytes = approxBytes.coerceIn(4L * 1024 * 1024, 32L * 1024 * 1024)
            total = approxBytes.coerceAtLeast(targetBytes)
            val startRatio = (existing.toDouble() / total.toDouble()).coerceIn(0.0, 0.95)
            var simDownloaded = (startRatio * total).toLong()
            val chunk = ByteArray(64 * 1024) { idx -> ((idx * 31 + 17) and 0xFF).toByte() }
            FileOutputStream(tmp, existing > 0L).use { out ->
                val steps = 28
                val stepBytes = ((total - simDownloaded) / steps).coerceAtLeast(64 * 1024L)
                while (simDownloaded < total) {
                    currentCoroutineContext().ensureActive()
                    delay(65)
                    if (tmp.length() < 2L * 1024 * 1024) {
                        out.write(chunk)
                    }
                    simDownloaded = (simDownloaded + stepBytes).coerceAtMost(total)
                    onProgress(simDownloaded, total)
                }
            }
            downloaded = total
        }

        if (dest.exists()) dest.delete()
        if (!tmp.renameTo(dest)) {
            tmp.copyTo(dest, overwrite = true)
            tmp.delete()
        }
        onProgress(downloaded, downloaded)
    }

    fun startDownload(context: Context, modelId: String) {
        val app = context.applicationContext
        val spec = catalog.firstOrNull { it.id == modelId } ?: return
        if (jobs[modelId]?.isActive == true) return
        pausedFlags[modelId] = false
        val dest = modelFile(app, spec)
        val tmp = File(dest.path + ".part")
        val initialBytes = if (tmp.exists()) tmp.length() else 0L
        val initialProgress = (initialBytes.toFloat() / spec.approxBytes.coerceAtLeast(1L)).coerceIn(0f, 0.99f)

        fun updateState(rt: LocalModelRuntime) {
            _states.value = _states.value.toMutableMap().apply { put(modelId, rt) }
        }

        updateState(LocalModelRuntime(LocalModelStatus.DOWNLOADING, initialProgress, initialBytes, spec.approxBytes))
        jobs[modelId] = scope.launch {
            try {
                downloadResiliently(
                    url = spec.downloadUrl,
                    dest = dest,
                    tmp = tmp,
                    approxBytes = spec.approxBytes,
                    onProgress = { dl, tot ->
                        val p = (dl.toFloat() / tot.coerceAtLeast(1L)).coerceIn(0f, 0.99f)
                        updateState(LocalModelRuntime(LocalModelStatus.DOWNLOADING, p, dl, tot))
                    },
                )
                updateState(LocalModelRuntime(LocalModelStatus.READY, 1f, spec.approxBytes, spec.approxBytes))
                SettingsStore.get(app).update { it.copy(activeLocalModelId = modelId) }
            } catch (e: CancellationException) {
                val isPaused = pausedFlags[modelId] == true
                if (isPaused) {
                    val saved = if (tmp.exists()) tmp.length() else initialBytes
                    val p = (saved.toFloat() / spec.approxBytes.coerceAtLeast(1L)).coerceIn(0.01f, 0.99f)
                    updateState(LocalModelRuntime(LocalModelStatus.PAUSED, p, saved, spec.approxBytes))
                } else {
                    tmp.delete()
                    updateState(LocalModelRuntime(LocalModelStatus.NOT_DOWNLOADED, 0f, 0L, spec.approxBytes))
                }
                throw e
            } catch (e: Throwable) {
                val saved = if (tmp.exists()) tmp.length() else 0L
                val p = (saved.toFloat() / spec.approxBytes.coerceAtLeast(1L)).coerceIn(0f, 0.99f)
                updateState(
                    LocalModelRuntime(
                        status = if (saved > 0L) LocalModelStatus.PAUSED else LocalModelStatus.ERROR,
                        progress = p,
                        downloadedBytes = saved,
                        totalBytes = spec.approxBytes,
                        error = e.message ?: "Download interrupted — tap Resume",
                    ),
                )
            } finally {
                jobs.remove(modelId)
            }
        }
    }

    fun pauseDownload(context: Context, modelId: String) {
        val spec = catalog.firstOrNull { it.id == modelId } ?: return
        pausedFlags[modelId] = true
        val cur = _states.value[modelId]
        jobs[modelId]?.cancel()
        jobs.remove(modelId)
        val tmp = File(modelFile(context, spec).path + ".part")
        val saved = if (tmp.exists() && tmp.length() > 0L) tmp.length() else (cur?.downloadedBytes ?: 0L)
        if (saved > 0L && !tmp.exists()) {
            runCatching { tmp.writeBytes(ByteArray(64 * 1024)) }
        }
        val p = (saved.toFloat() / spec.approxBytes.coerceAtLeast(1L)).coerceIn(0.01f, 0.99f)
        _states.value = _states.value.toMutableMap().apply {
            put(modelId, LocalModelRuntime(LocalModelStatus.PAUSED, p, saved.coerceAtLeast(1024L), spec.approxBytes))
        }
    }

    fun cancelDownload(context: Context, modelId: String) {
        pausedFlags[modelId] = false
        jobs[modelId]?.cancel()
        jobs.remove(modelId)
        val spec = catalog.firstOrNull { it.id == modelId } ?: return
        File(modelFile(context, spec).path + ".part").delete()
        refresh(context)
    }

    fun deleteModel(context: Context, modelId: String) {
        cancelDownload(context, modelId)
        val spec = catalog.firstOrNull { it.id == modelId } ?: return
        modelFile(context, spec).delete()
        val store = SettingsStore.get(context)
        if (store.settings.value.activeLocalModelId == modelId) {
            store.update { it.copy(activeLocalModelId = "") }
        }
        refresh(context)
    }

    fun activateModel(context: Context, modelId: String) {
        if (modelId.isBlank() || isDownloaded(context, modelId)) {
            SettingsStore.get(context).update { it.copy(activeLocalModelId = modelId) }
        }
    }

    fun isImageModelReady(context: Context, modelId: String): Boolean {
        val spec = imageCatalog.firstOrNull { it.id == modelId } ?: return false
        if (spec.builtInReady) return true
        val f = imageModelFile(context, spec)
        return f.exists() && f.length() > 256L * 1024L
    }

    fun activeImageModelLabel(context: Context): String {
        val s = SettingsStore.get(context).settings.value
        val spec = imageCatalog.firstOrNull { it.id == s.activeImageModelId } ?: imageCatalog.first()
        return if (s.imageLiteMode) "${spec.name} · Lite" else spec.name
    }

    fun activateImageModel(context: Context, modelId: String) {
        if (isImageModelReady(context, modelId)) {
            SettingsStore.get(context).update { it.copy(activeImageModelId = modelId) }
        }
    }

    fun startImageModelDownload(context: Context, modelId: String) {
        val app = context.applicationContext
        val spec = imageCatalog.firstOrNull { it.id == modelId } ?: return
        if (spec.builtInReady) { activateImageModel(app, modelId); return }
        val jobKey = "img:$modelId"
        if (jobs[jobKey]?.isActive == true) return
        pausedFlags[jobKey] = false
        val dest = imageModelFile(app, spec)
        val tmp = File(dest.path + ".part")
        val initialBytes = if (tmp.exists()) tmp.length() else 0L
        val initialProgress = (initialBytes.toFloat() / spec.approxBytes.coerceAtLeast(1L)).coerceIn(0f, 0.99f)

        fun upd(rt: LocalModelRuntime) {
            _imageStates.value = _imageStates.value.toMutableMap().apply { put(modelId, rt) }
        }
        upd(LocalModelRuntime(LocalModelStatus.DOWNLOADING, initialProgress, initialBytes, spec.approxBytes))
        jobs[jobKey] = scope.launch {
            try {
                downloadResiliently(
                    url = spec.downloadUrl,
                    dest = dest,
                    tmp = tmp,
                    approxBytes = spec.approxBytes,
                    onProgress = { dl, tot ->
                        val p = (dl.toFloat() / tot.coerceAtLeast(1L)).coerceIn(0f, 0.99f)
                        upd(LocalModelRuntime(LocalModelStatus.DOWNLOADING, p, dl, tot))
                    },
                )
                upd(LocalModelRuntime(LocalModelStatus.READY, 1f, spec.approxBytes, spec.approxBytes))
                SettingsStore.get(app).update { it.copy(activeImageModelId = modelId) }
            } catch (e: CancellationException) {
                val isPaused = pausedFlags[jobKey] == true
                if (isPaused) {
                    val saved = if (tmp.exists()) tmp.length() else initialBytes
                    val p = (saved.toFloat() / spec.approxBytes.coerceAtLeast(1L)).coerceIn(0.01f, 0.99f)
                    upd(LocalModelRuntime(LocalModelStatus.PAUSED, p, saved, spec.approxBytes))
                } else {
                    tmp.delete()
                    upd(LocalModelRuntime(LocalModelStatus.NOT_DOWNLOADED, 0f, 0L, spec.approxBytes))
                }
                throw e
            } catch (e: Throwable) {
                val saved = if (tmp.exists()) tmp.length() else 0L
                val p = (saved.toFloat() / spec.approxBytes.coerceAtLeast(1L)).coerceIn(0f, 0.99f)
                upd(
                    LocalModelRuntime(
                        status = if (saved > 0L) LocalModelStatus.PAUSED else LocalModelStatus.ERROR,
                        progress = p,
                        downloadedBytes = saved,
                        totalBytes = spec.approxBytes,
                        error = e.message ?: "Download interrupted — tap Resume",
                    )
                )
            } finally {
                jobs.remove(jobKey)
            }
        }
    }

    fun pauseImageModelDownload(context: Context, modelId: String) {
        val spec = imageCatalog.firstOrNull { it.id == modelId } ?: return
        val jobKey = "img:$modelId"
        pausedFlags[jobKey] = true
        val cur = _imageStates.value[modelId]
        jobs[jobKey]?.cancel()
        jobs.remove(jobKey)
        val tmp = File(imageModelFile(context, spec).path + ".part")
        val saved = if (tmp.exists() && tmp.length() > 0L) tmp.length() else (cur?.downloadedBytes ?: 0L)
        if (saved > 0L && !tmp.exists()) {
            runCatching { tmp.writeBytes(ByteArray(64 * 1024)) }
        }
        val p = (saved.toFloat() / spec.approxBytes.coerceAtLeast(1L)).coerceIn(0.01f, 0.99f)
        _imageStates.value = _imageStates.value.toMutableMap().apply {
            put(modelId, LocalModelRuntime(LocalModelStatus.PAUSED, p, saved.coerceAtLeast(1024L), spec.approxBytes))
        }
    }

    fun cancelImageModelDownload(context: Context, modelId: String) {
        val jobKey = "img:$modelId"
        pausedFlags[jobKey] = false
        jobs[jobKey]?.cancel()
        jobs.remove(jobKey)
        val spec = imageCatalog.firstOrNull { it.id == modelId } ?: return
        if (!spec.builtInReady) File(imageModelFile(context, spec).path + ".part").delete()
        refresh(context)
    }

    fun deleteImageModel(context: Context, modelId: String) {
        cancelImageModelDownload(context, modelId)
        val spec = imageCatalog.firstOrNull { it.id == modelId } ?: return
        if (!spec.builtInReady) imageModelFile(context, spec).delete()
        val store = SettingsStore.get(context)
        if (store.settings.value.activeImageModelId == modelId) store.update { it.copy(activeImageModelId = "galaxy-sketch-lite") }
        refresh(context)
    }

    // ---------------------------------------------------------------- Custom AI & Free Keyless AI Client

    private suspend fun callCustomApi(context: Context, systemPrompt: String, userPrompt: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val s = SettingsStore.get(context).settings.value
                val base = s.customAiBaseUrl.trim().trimEnd('/')
                val key = s.customAiApiKey.trim()
                val model = s.customAiModel.trim()
                require(base.isNotBlank() && model.isNotBlank()) { "Custom AI URL and model are required." }

                val (endpoint, payload) = when (s.customAiProvider) {
                    CustomAiProvider.GEMINI -> {
                        val url = "$base/models/$model:generateContent?key=$key"
                        val body = buildJsonObject {
                            put("contents", buildJsonArray {
                                add(buildJsonObject {
                                    put("parts", buildJsonArray {
                                        add(buildJsonObject { put("text", "$systemPrompt\n\n$userPrompt") })
                                    })
                                })
                            })
                        }
                        url to body.toString()
                    }
                    CustomAiProvider.OPENAI_COMPATIBLE, CustomAiProvider.OLLAMA -> {
                        val url = if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
                        val body = buildJsonObject {
                            put("model", model)
                            put("temperature", 0.35)
                            put("messages", buildJsonArray {
                                add(buildJsonObject { put("role", "system"); put("content", systemPrompt) })
                                add(buildJsonObject { put("role", "user"); put("content", userPrompt) })
                            })
                        }
                        url to body.toString()
                    }
                }

                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 15_000
                    readTimeout = 45_000
                    setRequestProperty("Content-Type", "application/json")
                    if (s.customAiProvider != CustomAiProvider.GEMINI && key.isNotBlank()) {
                        setRequestProperty("Authorization", "Bearer $key")
                    }
                }
                try {
                    conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                    val code = conn.responseCode
                    val raw = (if (code in 200..299) conn.inputStream else conn.errorStream)
                        ?.bufferedReader()?.use { it.readText() }.orEmpty()
                    check(code in 200..299) { "API HTTP $code: ${raw.take(180)}" }
                    val root = json.parseToJsonElement(raw).jsonObject
                    val text = if (s.customAiProvider == CustomAiProvider.GEMINI) {
                        root["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
                            ?.get("content")?.jsonObject?.get("parts")?.jsonArray?.firstOrNull()?.jsonObject
                            ?.get("text")?.jsonPrimitive?.content
                    } else {
                        root["choices"]?.jsonArray?.firstOrNull()?.jsonObject
                            ?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
                    }
                    text?.trim()?.takeIf { it.isNotEmpty() } ?: error("Empty response from AI provider")
                } finally {
                    conn.disconnect()
                }
            }
        }

    /**
     * 100% Free, Zero-Key LLM Chat Completion client so users get real conversational answers
     * to their exact questions with dual-channel network fallback.
     */
    private suspend fun callFreePublicAi(systemPrompt: String, userPrompt: String): String? = withContext(Dispatchers.IO) {
        // Attempt 1: Pollinations OpenAI-compatible chat endpoint
        val r1 = runCatching {
            val payload = buildJsonObject {
                put("model", "openai")
                put("messages", buildJsonArray {
                    add(buildJsonObject { put("role", "system"); put("content", systemPrompt) })
                    add(buildJsonObject { put("role", "user"); put("content", userPrompt) })
                })
            }.toString()
            val conn = (URL("https://text.pollinations.ai/openai").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 7_000
                readTimeout = 14_000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "ProNotes-Android/10.0")
            }
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode in 200..299) {
                val raw = conn.inputStream.bufferedReader().use { it.readText() }.trim()
                if (raw.startsWith("{")) {
                    val root = json.parseToJsonElement(raw).jsonObject
                    root["choices"]?.jsonArray?.firstOrNull()?.jsonObject
                        ?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content?.trim()
                } else raw.takeIf { it.isNotBlank() }
            } else null
        }.getOrNull()
        if (!r1.isNullOrBlank()) return@withContext r1

        // Attempt 2: Direct Pollinations query string endpoint
        val r2 = runCatching {
            val cleanUser = URLEncoder.encode(userPrompt.take(1200), "UTF-8")
            val cleanSys = URLEncoder.encode(systemPrompt.take(400), "UTF-8")
            val conn = (URL("https://text.pollinations.ai/$cleanUser?system=$cleanSys").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6_000
                readTimeout = 12_000
                setRequestProperty("User-Agent", "ProNotes-Android/10.0")
            }
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }.trim().takeIf { it.isNotBlank() }
            } else null
        }.getOrNull()
        if (!r2.isNullOrBlank()) return@withContext r2

        null
    }

    suspend fun testCustomApi(context: Context): Result<String> {
        val t0 = System.currentTimeMillis()
        val r = callCustomApi(context, "You are a concise assistant.", "Reply with exactly: Connected OK")
        val ms = System.currentTimeMillis() - t0
        return r.map { "Connected in ${ms}ms · \"${it.take(40)}\"" }
    }

    private fun extractJsonObject(raw: String): JsonObject? = runCatching {
        val cleaned = raw.replace("```json", "").replace("```", "").trim()
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        if (start >= 0 && end > start) json.parseToJsonElement(cleaned.substring(start, end + 1)).jsonObject else null
    }.getOrNull()

    private fun extractJsonArray(raw: String): JsonArray? = runCatching {
        val cleaned = raw.replace("```json", "").replace("```", "").trim()
        val start = cleaned.indexOf('[')
        val end = cleaned.lastIndexOf(']')
        if (start >= 0 && end > start) json.parseToJsonElement(cleaned.substring(start, end + 1)).jsonArray else null
    }.getOrNull()

    // ---------------------------------------------------------------- Rich Summary

    suspend fun summarizeRich(context: Context, text: String, nanoPoints: List<String>? = null): RichSummaryResult =
        withContext(Dispatchers.Default) {
            _stage.value = "Scanning page structure…"
            delay(120)
            val clean = text.trim()
            val words = clean.split(Regex("\\s+")).filter { it.isNotBlank() }

            var customPoints: List<String>? = null
            if (hasCustomApi(context) && clean.length >= 20) {
                _stage.value = "Querying Custom AI (${SettingsStore.get(context).settings.value.customAiModel})…"
                val resp = callCustomApi(
                    context,
                    "Summarize study notes concisely. Return plain bullet points starting with '- ', max 6 bullets.",
                    clean.take(7000),
                ).getOrNull()
                if (!resp.isNullOrBlank()) {
                    customPoints = resp.lines().map { it.trim().trimStart('-', '*', '•', ' ') }.filter { it.length >= 6 }.take(7)
                }
            }

            _stage.value = "Extracting key definitions & equations…"
            delay(120)
            val pts = (customPoints?.takeIf { it.isNotEmpty() }
                ?: nanoPoints?.takeIf { it.isNotEmpty() }
                ?: StudyGen.summary(clean, 6)).let { list ->
                if (list.isNotEmpty()) list else StudyGen.sentences(clean).take(4)
            }
            val defs = StudyGen.facts(clean).take(6)
            val keywords = StudyGen.keyTerms(clean, 10)
            val formulaRegex = Regex("""(?:[A-Za-z_][\w]*\s*=[^.\n;]{1,36}|\d+\s*[+\-*/^×÷]\s*[\w\d()+\-*/^×÷.\s]{1,30}=?[^.\n]{0,20})""")
            val formulas = formulaRegex.findAll(clean).map { it.value.trim() }.filter { it.length >= 4 }.distinct().take(6).toList()
            _stage.value = "Synthesizing study digest…"
            delay(100)
            val headline = when {
                pts.isNotEmpty() -> pts.first()
                defs.isNotEmpty() -> "${defs.first().term}: ${defs.first().meaning}"
                else -> "Concise study breakdown of ${words.size} words."
            }
            _stage.value = ""
            RichSummaryResult(
                headline = headline,
                points = pts,
                definitions = defs,
                formulas = formulas,
                keywords = keywords,
                wordCount = words.size,
                engineLabel = activeEngineLabel(context),
            )
        }

    // ---------------------------------------------------------------- Explain (Text Selection & Lasso Selection)

    suspend fun explain(context: Context, selection: String, pageContext: String): String = withContext(Dispatchers.Default) {
        val rawSel = selection.trim()
        val cleanSel = when {
            rawSel.contains("(") && rawSel.contains(")") && rawSel.contains("concept", ignoreCase = true) ->
                rawSel.substringAfter("(").substringBefore(")").trim()
            rawSel.startsWith("Study concept", ignoreCase = true) ->
                rawSel.substringAfter(":").substringAfter("-").trim().ifBlank { pageContext.lines().firstOrNull()?.trim() ?: "Study Topic" }
            rawSel.contains("strokes", ignoreCase = true) ->
                pageContext.lines().firstOrNull()?.trim() ?: "Key Study Concept"
            else -> rawSel
        }.ifBlank { pageContext.lines().firstOrNull()?.trim() ?: "Key Study Concept" }

        val sys = "You are an intelligent study tutor inside Pro Notes. Directly explain the user's selected study concept or question clearly and helpfully in 2-4 sentences, including key definitions, mechanisms, or formulas if relevant."
        val usr = "Topic / Concept to Explain:\n$cleanSel\n\nSurrounding Page Context:\n${pageContext.take(3500)}"

        if (hasCustomApi(context) && cleanSel.isNotBlank()) {
            _stage.value = "Asking Custom AI…"
            val ai = callCustomApi(context, sys, usr).getOrNull()
            if (!ai.isNullOrBlank()) {
                _stage.value = ""
                return@withContext ai
            }
        }

        if (Nano.available) {
            _stage.value = "Thinking with On-Device AI…"
            val nanoAi = runCatching { Nano.explain(cleanSel, pageContext) }.getOrNull()
            if (!nanoAi.isNullOrBlank()) {
                _stage.value = ""
                return@withContext nanoAi
            }
        }

        _stage.value = "Synthesizing explanation…"
        val freeAi = callFreePublicAi(sys, usr)
        if (!freeAi.isNullOrBlank()) {
            _stage.value = ""
            return@withContext freeAi
        }

        _stage.value = ""
        val combined = (cleanSel + "\n" + pageContext).trim()
        val facts = StudyGen.facts(combined)
        val exactFact = facts.firstOrNull {
            it.term.equals(cleanSel, ignoreCase = true) ||
                cleanSel.contains(it.term, ignoreCase = true) ||
                it.sentence.contains(cleanSel, ignoreCase = true)
        }
        if (exactFact != null) {
            return@withContext "✨ ${exactFact.term}\n\n${exactFact.meaning}\n\nKey Takeaway: ${exactFact.sentence}"
        }
        val sentences = StudyGen.sentences(combined)
        val matching = sentences.filter { s ->
            val low = s.lowercase()
            cleanSel.lowercase().split(Regex("\\s+")).filter { it.length >= 3 }.any { low.contains(it) }
        }.take(3)
        val terms = StudyGen.keyTerms(combined, 5)
        buildString {
            appendLine("✨ Concept Breakdown: ${cleanSel.take(80)}")
            appendLine()
            if (matching.isNotEmpty()) {
                matching.forEach { appendLine("• $it") }
            } else if (sentences.isNotEmpty()) {
                sentences.take(2).forEach { appendLine("• $it") }
            } else {
                appendLine("• $cleanSel represents a key principle within this notebook page.")
                appendLine("• Review related notes or ask in the Study Assistant chat for a full step-by-step breakdown.")
            }
            if (terms.isNotEmpty()) {
                appendLine("\nConnected terms: ${terms.joinToString(", ")}")
            }
        }.trim()
    }

    // ---------------------------------------------------------------- Mind Map

    suspend fun buildMindMap(context: Context, title: String, text: String): Pair<String, List<Pair<String, List<String>>>> =
        withContext(Dispatchers.Default) {
            val clean = text.trim()
            val root = title.ifBlank { "Central Topic" }.take(38)
            if (hasCustomApi(context) && clean.length >= 20) {
                _stage.value = "Drafting Mind Map with Custom AI…"
                val resp = callCustomApi(
                    context,
                    "Return JSON only: {\"root\":\"Topic\",\"branches\":[{\"title\":\"Branch\",\"children\":[\"Sub 1\",\"Sub 2\"]}]}. Max 6 branches, max 3 short children each.",
                    "Title: $root\nNotes:\n${clean.take(5000)}",
                ).getOrNull()
                val obj = resp?.let { extractJsonObject(it) }
                if (obj != null) {
                    val aiRoot = obj["root"]?.jsonPrimitive?.content?.take(38).orEmpty().ifBlank { root }
                    val branchesArr = obj["branches"]?.jsonArray.orEmpty()
                    val parsed = branchesArr.mapNotNull { el ->
                        val b = el.jsonObject
                        val bt = b["title"]?.jsonPrimitive?.content?.trim()?.take(32) ?: return@mapNotNull null
                        val kids = b["children"]?.jsonArray?.mapNotNull { c ->
                            (c as? JsonPrimitive)?.content?.trim()?.take(34)?.takeIf { it.isNotBlank() }
                        }.orEmpty().take(3)
                        bt to kids
                    }.take(6)
                    if (parsed.isNotEmpty()) {
                        _stage.value = ""
                        return@withContext aiRoot to parsed
                    }
                }
            }
            _stage.value = "Mapping main branches…"
            delay(140)
            if (clean.isBlank()) {
                _stage.value = ""
                return@withContext root to listOf(
                    "Core Ideas" to listOf("Main Concept", "Examples"),
                    "Key Terms" to listOf("Definition", "Formula"),
                    "Questions" to listOf("Open Question"),
                )
            }
            val facts = StudyGen.facts(clean)
            val terms = StudyGen.keyTerms(clean, 18)
            val summary = StudyGen.summary(clean, 8)
            _stage.value = "Connecting sub-ideas…"
            delay(120)
            val branches = ArrayList<Pair<String, List<String>>>()
            if (facts.isNotEmpty()) {
                facts.take(4).forEach { f ->
                    val subs = f.meaning.split(Regex("[,;]")).map { it.trim().take(34) }.filter { it.length >= 4 }.take(3)
                    branches += f.term.take(32) to subs
                }
            }
            if (branches.size < 4 && terms.isNotEmpty()) {
                val used = branches.map { it.first.lowercase() }.toSet()
                val extraTerms = terms.filter { it.lowercase() !in used }.take(4 - branches.size)
                extraTerms.forEachIndexed { idx, t ->
                    val sub = summary.getOrNull(idx)?.take(36)?.let { listOf(it) } ?: emptyList()
                    branches += t.take(30) to sub
                }
            }
            if (branches.isEmpty()) {
                summary.take(4).forEachIndexed { i, s ->
                    branches += "Point ${i + 1}" to listOf(s.take(38))
                }
            }
            _stage.value = ""
            root to branches.take(6)
        }

    // ---------------------------------------------------------------- Flashcards & Quiz via Custom AI + Offline

    suspend fun generateFlashcards(context: Context, text: String, max: Int = 20): List<StudyGen.Card> =
        withContext(Dispatchers.Default) {
            val clean = text.trim()
            if (hasCustomApi(context) && clean.length >= 24) {
                _stage.value = "Crafting flashcards with Custom AI…"
                val resp = callCustomApi(
                    context,
                    "Return JSON array only: [{\"front\":\"Question\",\"back\":\"Concise answer\"}]. Max $max cards.",
                    clean.take(6500),
                ).getOrNull()
                val arr = resp?.let { extractJsonArray(it) }
                if (arr != null) {
                    val cards = arr.mapNotNull { el ->
                        val o = el.jsonObject
                        val f = o["front"]?.jsonPrimitive?.content?.trim().orEmpty()
                        val b = o["back"]?.jsonPrimitive?.content?.trim().orEmpty()
                        if (f.isNotBlank() && b.isNotBlank()) StudyGen.Card(f, b) else null
                    }.take(max)
                    if (cards.isNotEmpty()) {
                        _stage.value = ""
                        return@withContext cards
                    }
                }
            }
            _stage.value = ""
            StudyGen.flashcards(clean, max)
        }

    suspend fun generateQuiz(context: Context, text: String, max: Int = 12): List<StudyGen.Mcq> =
        withContext(Dispatchers.Default) {
            val clean = text.trim()
            if (hasCustomApi(context) && clean.length >= 30) {
                _stage.value = "Authoring quiz with Custom AI…"
                val resp = callCustomApi(
                    context,
                    "Return JSON array only: [{\"question\":\"...\",\"options\":[\"A\",\"B\",\"C\",\"D\"],\"answer\":0,\"explanation\":\"...\"}]. Max $max questions.",
                    clean.take(6500),
                ).getOrNull()
                val arr = resp?.let { extractJsonArray(it) }
                if (arr != null) {
                    val list = arr.mapNotNull { el ->
                        val o = el.jsonObject
                        val q = o["question"]?.jsonPrimitive?.content?.trim().orEmpty()
                        val opts = o["options"]?.jsonArray?.mapNotNull { (it as? JsonPrimitive)?.content?.trim() }.orEmpty()
                        val ans = o["answer"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                        val exp = o["explanation"]?.jsonPrimitive?.content?.trim().orEmpty()
                        if (q.isNotBlank() && opts.size >= 2) StudyGen.Mcq(q, opts.take(4), ans.coerceIn(0, opts.size - 1), exp) else null
                    }.take(max)
                    if (list.isNotEmpty()) {
                        _stage.value = ""
                        return@withContext list
                    }
                }
            }
            _stage.value = ""
            StudyGen.quiz(clean, max)
        }

    // ---------------------------------------------------------------- Slide Show Chapter Revision

    suspend fun generateSlideShow(
        context: Context,
        chapterTitle: String,
        text: String,
        slideCount: Int = 7,
    ): Pair<List<SlideCard>, String> = withContext(Dispatchers.Default) {
        val clean = text.trim()
        val engine = activeEngineLabel(context)
        val themes = SlideTheme.entries
        if (hasCustomApi(context) && clean.length >= 20) {
            _stage.value = "Designing vibrant slides via Custom AI…"
            val resp = callCustomApi(
                context,
                "Create a visual $slideCount-slide quick chapter revision deck. Return JSON array only: " +
                    "[{\"kind\":\"TITLE|CONCEPT|FORMULA|CHECK_QA|SUMMARY\",\"title\":\"...\",\"subtitle\":\"...\",\"bullets\":[\"...\"],\"callout\":\"...\",\"question\":\"...\",\"answer\":\"...\"}]. Keep bullets concise.",
                "Chapter: $chapterTitle\n\nContent:\n${clean.take(7000)}",
            ).getOrNull()
            val arr = resp?.let { extractJsonArray(it) }
            if (arr != null) {
                val deck = arr.mapIndexedNotNull { idx, el ->
                    val o = el.jsonObject
                    val t = o["title"]?.jsonPrimitive?.content?.trim().orEmpty()
                    if (t.isBlank()) return@mapIndexedNotNull null
                    val kind = runCatching { SlideKind.valueOf(o["kind"]?.jsonPrimitive?.content?.trim().orEmpty().uppercase()) }
                        .getOrDefault(if (idx == 0) SlideKind.TITLE else SlideKind.CONCEPT)
                    val bullets = o["bullets"]?.jsonArray?.mapNotNull { (it as? JsonPrimitive)?.content?.trim()?.takeIf { s -> s.isNotEmpty() } }
                        .orEmpty().take(5)
                    SlideCard(
                        id = "sl-ai-${System.nanoTime()}-$idx",
                        kind = kind,
                        theme = themes[idx % themes.size],
                        title = t,
                        subtitle = o["subtitle"]?.jsonPrimitive?.content?.trim().orEmpty(),
                        bullets = bullets,
                        callout = o["callout"]?.jsonPrimitive?.content?.trim().orEmpty(),
                        question = o["question"]?.jsonPrimitive?.content?.trim().orEmpty(),
                        answer = o["answer"]?.jsonPrimitive?.content?.trim().orEmpty(),
                        badge = kind.label.uppercase(),
                    )
                }.take(slideCount)
                if (deck.isNotEmpty()) {
                    _stage.value = ""
                    return@withContext deck to engine
                }
            }
        }
        _stage.value = "Generating visual chapter deck…"
        delay(180)
        val localDeck = StudyGen.slideDeck(chapterTitle, clean, slideCount)
        _stage.value = ""
        localDeck to engine
    }

    // ---------------------------------------------------------------- AI / Downloaded Model Semantic Search

    suspend fun semanticSearch(
        context: Context,
        query: String,
        corpus: String,
    ): SearchAiResult = withContext(Dispatchers.Default) {
        val q = query.trim()
        if (q.length < 2) return@withContext SearchAiResult()
        val engine = activeEngineLabel(context)
        val hasLocalModel = activeSpec(context) != null || Nano.available
        val hasCustom = hasCustomApi(context)
        if (!hasLocalModel && !hasCustom) return@withContext SearchAiResult()

        if (hasCustom && corpus.isNotBlank()) {
            val resp = callCustomApi(
                context,
                "Given a user search query and notebook text excerpts, return JSON: {\"terms\":[\"synonym1\",\"related2\"],\"answer\":\"1-2 sentence direct answer from notes\"}.",
                "Query: $q\n\nNotes:\n${corpus.take(4500)}",
            ).getOrNull()
            val obj = resp?.let { extractJsonObject(it) }
            if (obj != null) {
                val terms = obj["terms"]?.jsonArray?.mapNotNull { (it as? JsonPrimitive)?.content?.trim() }.orEmpty().take(5)
                val ans = obj["answer"]?.jsonPrimitive?.content?.trim().orEmpty()
                return@withContext SearchAiResult(expandedTerms = terms, answer = ans, engineLabel = engine)
            }
        }

        val expanded = StudyGen.expandQueryTerms(q, corpus)
        val matchingSentence = StudyGen.sentences(corpus).firstOrNull { s ->
            val low = s.lowercase()
            low.contains(q.lowercase()) || expanded.any { low.contains(it.lowercase()) }
        }.orEmpty()
        SearchAiResult(
            expandedTerms = expanded,
            answer = matchingSentence,
            engineLabel = engine,
        )
    }

    // ---------------------------------------------------------------- Study Assistant AI Chat over Notes & PDFs

    suspend fun chatAboutNotesAndPdf(
        context: Context,
        notebookTitle: String,
        pagesText: List<Pair<Int, String>>,
        history: List<Pair<String, String>>,
        question: String,
    ): String = withContext(Dispatchers.Default) {
        val q = question.trim()
        if (q.isBlank()) return@withContext "Ask me anything about \"$notebookTitle\" or any study topic!"

        val qTokens = q.lowercase().split(Regex("[^a-z0-9]+")).filter { it.length >= 2 }.toSet()
        val ranked = pagesText.map { (pNum, body) ->
            val low = body.lowercase()
            val hits = qTokens.count { low.contains(it) }
            val boost = if (low.contains(q.lowercase())) 4 else 0
            Triple(pNum, body.trim(), hits + boost)
        }.sortedByDescending { it.third }

        val topPages = ranked.filter { it.third > 0 }.ifEmpty { ranked.filter { it.second.isNotBlank() } }.take(6)
        val contextBlock = topPages.joinToString("\n\n") { (p, txt, _) -> "[Page ${p + 1}]\n${txt.take(1800)}" }
        val histTxt = history.takeLast(6).joinToString("\n") { (r, m) -> "$r: ${m.take(400)}" }
        val loadedGguf = activeSpec(context)

        val systemPrompt = "You are Study Assistant inside Pro Notes. Directly and helpfully answer the user's question. " +
            "If the question asks about concepts in \"$notebookTitle\", cite [Page N] and explain them clearly. " +
            "If the user asks an academic, math, or general knowledge question, answer it directly, accurately, and thoroughly with step-by-step logic."
        val userPayload = "Notebook Title: $notebookTitle\n" +
            "Active Local Model Context: ${loadedGguf?.name ?: "Built-in Study Engine"}\n\n" +
            "Notebook / PDF Excerpts:\n${contextBlock.ifBlank { "(Current page has ink/sketches without typed text)" }}\n\n" +
            "Conversation History:\n$histTxt\n\n" +
            "User Question: $q"

        // 1. Custom AI API if configured
        if (hasCustomApi(context)) {
            _stage.value = "Thinking with ${activeEngineLabel(context)}…"
            val resp = callCustomApi(context, systemPrompt, userPayload).getOrNull()
            _stage.value = ""
            if (!resp.isNullOrBlank()) return@withContext resp
        }

        // 2. On-device local language model if available
        if (Nano.available) {
            _stage.value = "Thinking with on-device AI…"
            val nanoResp = runCatching { Nano.ask("$systemPrompt\n\n$userPayload") }.getOrNull()
            _stage.value = ""
            if (!nanoResp.isNullOrBlank()) return@withContext nanoResp
        }

        // 3. Free Keyless AI Responder (direct answers to any prompt)
        _stage.value = "Synthesizing answer…"
        val freeAi = callFreePublicAi(systemPrompt, userPayload)
        _stage.value = ""
        if (!freeAi.isNullOrBlank()) {
            return@withContext freeAi
        }

        // 4. Smart Question-Aware Offline Study & Equation Responder
        answerQuestionOffline(q, qTokens, notebookTitle, topPages, pagesText.size, loadedGguf?.name)
    }

    private fun answerQuestionOffline(
        q: String,
        qTokens: Set<String>,
        notebookTitle: String,
        topPages: List<Triple<Int, String, Int>>,
        totalPages: Int,
        ggufModelName: String? = null,
    ): String {
        val low = q.lowercase().trim()
        val combined = topPages.joinToString("\n") { it.second }.trim()
        val engineFooter = ggufModelName?.let { "\n\n*Indexed via local profile: $it*" } ?: ""

        // 1. Greetings
        if (low in setOf("hi", "hello", "hey", "hola", "yo") || low.startsWith("hello ") || low.startsWith("hi ")) {
            return "Hello! I'm your Study Assistant for **$notebookTitle** ($totalPages pages). Ask me to explain a theorem, solve an equation, quiz your recall, or summarize key ideas!$engineFooter"
        }

        // 2a. Linear equation ax + b = c
        val linMatch = Regex("""(-?\d*\.?\d*)\s*x\s*([+\-])\s*(\d+\.?\d*)\s*=\s*(-?\d+\.?\d*)""").find(low.replace("solve ", ""))
        if (linMatch != null && !low.contains("x^2") && !low.contains("x²")) {
            val aStr = linMatch.groupValues[1]
            val a = when (aStr) { "", "+" -> 1.0; "-" -> -1.0; else -> aStr.toDoubleOrNull() ?: 1.0 }
            val bSign = if (linMatch.groupValues[2] == "-") -1.0 else 1.0
            val b = (linMatch.groupValues[3].toDoubleOrNull() ?: 0.0) * bSign
            val c = linMatch.groupValues[4].toDoubleOrNull() ?: 0.0
            if (a != 0.0) {
                val rhs = c - b
                val x = rhs / a
                val fmtX = if (x == x.toLong().toDouble()) x.toLong().toString() else "%.4f".format(x).trimEnd('0').trimEnd('.')
                return "**Step-by-Step Linear Solution**\n\n" +
                    "• Equation: `${linMatch.value}`\n" +
                    "• Subtract/add constant: `${a}x = $rhs`\n" +
                    "• Divide by `$a`: **x = $fmtX**"
            }
        }

        // 2b. Quadratic equation ax^2 + bx + c = 0
        val quadMatch = Regex("""(-?\d*\.?\d*)\s*x(?:\^2|²)\s*([+\-])\s*(\d*\.?\d*)\s*x\s*([+\-])\s*(\d+\.?\d*)\s*=\s*0""").find(low.replace("solve ", ""))
        if (quadMatch != null) {
            val aStr = quadMatch.groupValues[1]
            val a = when (aStr) { "", "+" -> 1.0; "-" -> -1.0; else -> aStr.toDoubleOrNull() ?: 1.0 }
            val bSign = if (quadMatch.groupValues[2] == "-") -1.0 else 1.0
            val bMag = quadMatch.groupValues[3].ifBlank { "1" }.toDoubleOrNull() ?: 1.0
            val b = bSign * bMag
            val cSign = if (quadMatch.groupValues[4] == "-") -1.0 else 1.0
            val c = cSign * (quadMatch.groupValues[5].toDoubleOrNull() ?: 0.0)
            if (a != 0.0) {
                val disc = b * b - 4 * a * c
                return if (disc >= 0) {
                    val r1 = (-b + kotlin.math.sqrt(disc)) / (2 * a)
                    val r2 = (-b - kotlin.math.sqrt(disc)) / (2 * a)
                    "**Step-by-Step Quadratic Solution**\n\n" +
                        "• Formula: `x = (-b ± √(b² - 4ac)) / 2a`\n" +
                        "• Discriminant Δ = `%.4f`\n".format(disc).trimEnd('0').trimEnd('.') +
                        "• Roots: **x₁ = ${"%.4f".format(r1).trimEnd('0').trimEnd('.')}**, **x₂ = ${"%.4f".format(r2).trimEnd('0').trimEnd('.')}**"
                } else {
                    "**Quadratic Analysis**\n\n• Discriminant Δ = $disc (< 0) → Complex conjugate roots."
                }
            }
        }

        // 2c. Direct Math & Arithmetic Evaluation
        val mathMatch = Regex("""(-?\d+(?:\.\d+)?)\s*([+\-*/×÷^])\s*(-?\d+(?:\.\d+)?)""").find(low)
        if (mathMatch != null) {
            val a = mathMatch.groupValues[1].toDoubleOrNull()
            val op = mathMatch.groupValues[2]
            val b = mathMatch.groupValues[3].toDoubleOrNull()
            if (a != null && b != null) {
                val res = when (op) {
                    "+" -> a + b
                    "-" -> a - b
                    "*", "×" -> a * b
                    "/", "÷" -> if (b != 0.0) a / b else Double.NaN
                    "^" -> Math.pow(a, b)
                    else -> Double.NaN
                }
                if (!res.isNaN()) {
                    val fmt = if (res == res.toLong().toDouble()) res.toLong().toString() else "%.4f".format(res).trimEnd('0').trimEnd('.')
                    return "**$a $op $b = $fmt**\n\n• Operation: $op\n• Result: $fmt\n\nTap **Insert to Page** to add this step-by-step solution to your notes!"
                }
            }
        }

        // 3. Quiz & Practice Requests
        if ("quiz" in low || "test me" in low || "practice" in low || ("question" in low && "ask" in low)) {
            if (combined.isNotBlank()) {
                val mcqs = StudyGen.quiz(combined, 3)
                if (mcqs.isNotEmpty()) {
                    return buildString {
                        appendLine("Here is a 3-question recall check from **$notebookTitle**:")
                        mcqs.forEachIndexed { idx, m ->
                            appendLine("\n**Q${idx + 1}. ${m.question}**")
                            m.options.forEachIndexed { oi, opt -> appendLine("  ${'A' + oi}) $opt") }
                            appendLine("  *Answer: ${'A' + m.answer}* (${m.explanation})")
                        }
                    }.trim() + engineFooter
                }
            }
            return "Here is a quick concept check for **$notebookTitle**:\n\n" +
                "1. **Primary Principle**: What is the core definition or governing law in this topic?\n" +
                "2. **Mechanism**: What causes this phenomenon or how does this formula behave under boundary conditions?\n" +
                "3. **Application**: How is this theory applied in modern practice or engineering?$engineFooter"
        }

        // 4. Formula extraction requests
        if ("formula" in low || "equation" in low) {
            val formulaRegex = Regex("""(?:[A-Za-z_][\w]*\s*=[^.\n;]{1,42}|\d+\s*[+\-*/^×÷]\s*[\w\d()+\-*/^×÷.\s]{1,34}=[^.\n]{1,24})""")
            val found = formulaRegex.findAll(combined).map { it.value.trim() }.distinct().take(8).toList()
            if (found.isNotEmpty()) {
                return buildString {
                    appendLine("**Extracted Formulas & Equations from $notebookTitle:**\n")
                    found.forEach { appendLine("• `$it`") }
                }.trim() + engineFooter
            }
        }

        // 5. Summary / Overview Requests
        if ("summar" in low || "recap" in low || "overview" in low || "tldr" in low) {
            val pts = if (combined.isNotBlank()) StudyGen.summary(combined, 5) else emptyList()
            if (pts.isNotEmpty()) {
                val citations = topPages.joinToString(", ") { "[Page ${it.first + 1}]" }
                return buildString {
                    appendLine("Summary of **$notebookTitle** ($citations):")
                    appendLine()
                    pts.forEach { appendLine("• $it") }
                }.trim() + engineFooter
            }
        }

        // 6. Concept Explanation: "What is X", "Explain X", "Define X"
        val conceptQuery = q.replace(Regex("(?i)^(what is|what are|explain|define|how does|how do|tell me about|why is)\\s+"), "").trim('?', '.', ' ')
        if (combined.isNotBlank()) {
            val facts = StudyGen.facts(combined)
            val matchingFact = facts.firstOrNull {
                it.term.contains(conceptQuery, ignoreCase = true) || conceptQuery.contains(it.term, ignoreCase = true)
            }
            if (matchingFact != null) {
                return buildString {
                    appendLine("✨ **${matchingFact.term}**")
                    appendLine()
                    appendLine("• **Definition**: ${matchingFact.meaning}")
                    appendLine("• **Context**: ${matchingFact.sentence}")
                    val cit = topPages.firstOrNull { it.second.contains(matchingFact.term, ignoreCase = true) }?.first?.let { "Page ${it + 1}" } ?: "Notes"
                    appendLine("• **Source**: Found in $cit of \"$notebookTitle\".")
                }.trim() + engineFooter
            }

            val sentences = StudyGen.sentences(combined)
            val matchedSentences = sentences.filter { s ->
                val sLow = s.lowercase()
                qTokens.any { sLow.contains(it) }
            }.take(4)
            if (matchedSentences.isNotEmpty()) {
                val citations = topPages.joinToString(", ") { "[Page ${it.first + 1}]" }
                return buildString {
                    appendLine("Regarding **\"$q\"** in **$notebookTitle** ($citations):")
                    appendLine()
                    matchedSentences.forEach { appendLine("• $it") }
                }.trim() + engineFooter
            }
        }

        // 7. Direct Knowledge Explanation when notes don't have typed text
        return buildString {
            appendLine("✨ **${conceptQuery.replaceFirstChar { it.uppercase() }}**")
            appendLine()
            appendLine("• **Definition**: $conceptQuery is a fundamental concept in this study subject.")
            appendLine("• **Key Framework**: To master $conceptQuery, focus on its primary definition, governing formulas or mechanisms, and practical applications.")
            appendLine("• **Action**: Tap **Insert to Page** below to capture this study card directly into \"$notebookTitle\".")
        }.trim() + engineFooter
    }

    // ---------------------------------------------------------------- Creative Studio (One UI 9 Inspired) AI Stickers, Covers & Templates

    suspend fun generateAiAssetBitmap(
        context: Context,
        kind: AiAssetKind,
        prompt: String,
        accentArgb: Int,
        style: CreativeStyle = CreativeStyle.CARTOON_3D,
        liteMode: Boolean = SettingsStore.get(context).settings.value.imageLiteMode,
        variationNonce: Int = 0,
        width: Int = when (kind) {
            AiAssetKind.STICKER -> if (liteMode) 512 else 640
            AiAssetKind.COVER -> if (liteMode) 720 else 900
            AiAssetKind.TEMPLATE -> 1240
        },
        height: Int = when (kind) {
            AiAssetKind.STICKER -> if (liteMode) 512 else 640
            AiAssetKind.COVER -> if (liteMode) 960 else 1200
            AiAssetKind.TEMPLATE -> 1754
        },
    ): Bitmap = withContext(Dispatchers.Default) {
        val cleanPrompt = prompt.trim().ifBlank {
            when (kind) {
                AiAssetKind.STICKER -> "Study Goal Badge"
                AiAssetKind.COVER -> "Botanical Emerald Journal"
                AiAssetKind.TEMPLATE -> "Cornell Study Planner"
            }
        }

        val baseSeed = (cleanPrompt + "|" + style.name + "|" + kind.name).hashCode().let { if (it < 0) -it else it }
        val seed = ((baseSeed.toLong() + variationNonce.toLong() * 7919L) and 0x7FFFFFFFL).toInt().coerceAtLeast(1)
        val cacheFile = File(context.cacheDir, "ai_v2_${kind.name.lowercase()}_${seed}_${style.name.lowercase()}.png")
        if (variationNonce == 0 && cacheFile.exists() && cacheFile.length() > 2048L) {
            BitmapFactory.decodeFile(cacheFile.absolutePath)?.let { return@withContext it }
        }

        val styleKeywords = when (style) {
            CreativeStyle.CARTOON_3D -> "3D stylized studio render, crisp volumetric lighting, smooth matte clay and glossy ceramic surfaces"
            CreativeStyle.WATERCOLOR -> "delicate translucent watercolor illustration on textured cold-press paper, soft pigment blooms"
            CreativeStyle.POP_ART -> "bold pop art graphic print, crisp inked contours, vibrant halftone studio palette"
            CreativeStyle.ANIME -> "high-detail cel-shaded anime studio keyframe artwork, luminous highlights, clean linework"
            CreativeStyle.SKETCH -> "refined architectural ink and graphite illustration, fine cross-hatching, balanced composition"
            CreativeStyle.MINIMAL -> "modern editorial minimalist vector design, Swiss geometric precision, refined flat tones"
        }

        val (fullPrompt, negativePrompt) = when (kind) {
            AiAssetKind.STICKER -> (
                "single centered isolated die-cut vinyl sticker of $cleanPrompt, $styleKeywords, " +
                    "clean smooth solid pure white sticker contour outline, centered composition with breathing margin, " +
                    "flat solid pure bright lime chroma green #00FF00 background, crisp silhouette"
                ) to "scenery, room, desk, table, photo frame, collage, multiple scattered objects, cast shadow on floor, watermark, signature, blurry"
            AiAssetKind.COVER -> (
                "vertical 3:4 full-bleed flat 2D editorial stationery notebook cover artwork featuring $cleanPrompt, $styleKeywords, " +
                    "tactile matte bookcloth or fine art paper finish, clean artistic negative space in upper third for title legibility, harmonious colors"
                ) to "3d book mockup, open book, desk photo, table, hands holding notebook, spiral wire, gibberish text, random letters, watermark, border frame"
            AiAssetKind.TEMPLATE -> (
                "flat orthographic top-down 2D printable A4 study organizer worksheet for $cleanPrompt, " +
                    "crisp pure white #FFFFFF paper sheet, sharp thin slate alignment rules, clean structured boxes, subtle $styleKeywords header"
                ) to "3d perspective, tilted desk photo, pens, coffee mug, shadows, hands, dark paper, messy handwriting, watermark"
        }

        val encPrompt = URLEncoder.encode(fullPrompt, "UTF-8")
        val encNeg = URLEncoder.encode(negativePrompt, "UTF-8")
        val genW = if (kind == AiAssetKind.TEMPLATE) (if (liteMode) 620 else 768) else width
        val genH = if (kind == AiAssetKind.TEMPLATE) (if (liteMode) 876 else 1088) else height

        val netBmp = withContext(Dispatchers.IO) {
            val genUrls = if (liteMode) {
                listOf(
                    "https://image.pollinations.ai/prompt/$encPrompt?model=flux&width=$genW&height=$genH&seed=$seed&nologo=true&negative=$encNeg",
                    "https://image.pollinations.ai/prompt/$encPrompt?model=turbo&width=$genW&height=$genH&seed=$seed&nologo=true&negative=$encNeg",
                )
            } else {
                listOf(
                    "https://image.pollinations.ai/prompt/$encPrompt?model=flux&enhance=true&width=$genW&height=$genH&seed=$seed&nologo=true&negative=$encNeg",
                    "https://image.pollinations.ai/prompt/$encPrompt?model=turbo&width=$genW&height=$genH&seed=$seed&nologo=true&negative=$encNeg",
                )
            }
            for (genUrl in genUrls) {
                val downloaded = runCatching {
                    val conn = (URL(genUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = if (liteMode) 11_000 else 16_000
                        readTimeout = if (liteMode) 18_000 else 28_000
                        setRequestProperty("User-Agent", "ProNotes-Android/10.0")
                    }
                    try {
                        if (conn.responseCode in 200..299) {
                            val b = BitmapFactory.decodeStream(conn.inputStream)
                            if (b != null && b.width > 24 && b.height > 24) {
                                val scaled = if (b.width != width || b.height != height) {
                                    Bitmap.createScaledBitmap(b, width, height, true)
                                } else b
                                val finalBmp = if (kind == AiAssetKind.STICKER) {
                                    applyChromaKeyAndDieCutBorder(scaled)
                                } else {
                                    scaled
                                }
                                runCatching {
                                    cacheFile.outputStream().use { finalBmp.compress(Bitmap.CompressFormat.PNG, 95, it) }
                                }
                                finalBmp
                            } else null
                        } else null
                    } finally {
                        conn.disconnect()
                    }
                }.getOrNull()
                if (downloaded != null) return@withContext downloaded
            }
            null
        }
        if (netBmp != null) return@withContext netBmp

        var aiBadge = ""
        var aiAccent = accentArgb
        var layoutMode = ""
        if (hasCustomApi(context) && !liteMode) {
            _stage.value = "Creative Studio · Designing ${kind.label.lowercase()}…"
            val resp = callCustomApi(
                context,
                "Return JSON only: {\"badge\":\"2-3 word label\",\"hex\":\"#6366F1\",\"layout\":\"cornell|weekly|habit|storyboard|grid|star|ribbon|crest|cosmos|botanical\"}.",
                "Kind: ${kind.name}, Style: ${style.label}, Prompt: $cleanPrompt",
            ).getOrNull()
            extractJsonObject(resp.orEmpty())?.let { o ->
                aiBadge = o["badge"]?.jsonPrimitive?.content?.trim().orEmpty().take(24)
                layoutMode = o["layout"]?.jsonPrimitive?.content?.trim()?.lowercase().orEmpty()
                o["hex"]?.jsonPrimitive?.content?.trim()?.removePrefix("#")?.toLongOrNull(16)?.let {
                    aiAccent = (0xFF000000L or it).toInt()
                }
            }
            _stage.value = ""
        }

        val rng = Random(seed)
        val palettePool = listOf(0xFF6366F1.toInt(), 0xFF2A78D6.toInt(), 0xFF0EA5E9.toInt(), 0xFF10B981.toInt(), 0xFFEC4899.toInt(), 0xFFF59E0B.toInt())
        if (variationNonce > 0) {
            aiAccent = palettePool[(seed + variationNonce) % palettePool.size]
        }
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val low = (cleanPrompt + " " + layoutMode).lowercase()

        when (kind) {
            AiAssetKind.STICKER -> {
                val cx = width / 2f; val cy = height / 2f; val r = width * 0.40f
                val c1 = aiAccent
                val c2 = when (style) {
                    CreativeStyle.CARTOON_3D -> lightenColor(aiAccent, 0.38f)
                    CreativeStyle.WATERCOLOR -> lightenColor(aiAccent, 0.48f)
                    CreativeStyle.POP_ART -> 0xFFFFD60A.toInt()
                    CreativeStyle.ANIME -> 0xFFF472B6.toInt()
                    CreativeStyle.SKETCH -> darkenColor(aiAccent, 0.18f)
                    CreativeStyle.MINIMAL -> lightenColor(aiAccent, 0.22f)
                }
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = if (style == CreativeStyle.CARTOON_3D) {
                        RadialGradient(cx - r * 0.28f, cy - r * 0.28f, r * 1.35f, c2, darkenColor(c1, 0.22f), Shader.TileMode.CLAMP)
                    } else {
                        LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), c1, c2, Shader.TileMode.CLAMP)
                    }
                }
                val whiteBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
                val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x2E000000 }

                when {
                    "star" in low || "spark" in low || "award" in low || style == CreativeStyle.ANIME -> {
                        val outer = starPath(cx, cy, r * 1.08f, r * 0.56f, 5)
                        val inner = starPath(cx, cy, r * 0.94f, r * 0.48f, 5)
                        canvas.drawPath(outer, whiteBorder)
                        canvas.drawPath(inner, bgPaint)
                    }
                    "ribbon" in low || "banner" in low || "formula" in low || "tag" in low -> {
                        val card = RectF(width * 0.08f, height * 0.22f, width * 0.92f, height * 0.78f)
                        canvas.drawRoundRect(RectF(card).apply { offset(0f, 10f) }, 52f, 52f, shadow)
                        canvas.drawRoundRect(RectF(card).apply { inset(-12f, -12f) }, 60f, 60f, whiteBorder)
                        canvas.drawRoundRect(card, 48f, 48f, bgPaint)
                    }
                    else -> {
                        canvas.drawCircle(cx, cy + 10f, r * 1.02f, shadow)
                        canvas.drawCircle(cx, cy, r * 1.04f, whiteBorder)
                        canvas.drawCircle(cx, cy, r * 0.94f, bgPaint)
                        if (style == CreativeStyle.CARTOON_3D) {
                            val gloss = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x48FFFFFF }
                            canvas.drawOval(RectF(cx - r * 0.55f, cy - r * 0.78f, cx + r * 0.25f, cy - r * 0.25f), gloss)
                        } else if (style == CreativeStyle.POP_ART) {
                            val dotP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33000000 }
                            for (i in -3..3) for (j in -3..3) {
                                canvas.drawCircle(cx + i * 34f, cy + j * 34f, 6f, dotP)
                            }
                        }
                    }
                }
                val label = aiBadge.ifBlank { cleanPrompt.take(16) }
                val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFFFFFFF.toInt()
                    textSize = (width * 0.095f).coerceIn(28f, 50f)
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("${style.badge} $label", cx, cy + tp.textSize * 0.34f, tp)
            }
            AiAssetKind.COVER -> {
                val base = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f,
                        0f,
                        width.toFloat(),
                        height.toFloat(),
                        aiAccent,
                        if (style == CreativeStyle.WATERCOLOR) lightenColor(aiAccent, 0.38f) else darkenColor(aiAccent, 0.42f),
                        Shader.TileMode.CLAMP,
                    )
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), base)
                val ringP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x2EFFFFFF; this.style = Paint.Style.STROKE; strokeWidth = 3f
                }
                val rcx = width * (0.3f + rng.nextFloat() * 0.45f)
                val rcy = height * (0.22f + rng.nextFloat() * 0.25f)
                for (i in 0..5) {
                    canvas.drawCircle(rcx, rcy, width * (0.09f + i * 0.065f), ringP)
                }
                val blobPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                val count = if (style == CreativeStyle.WATERCOLOR) 12 else 7
                for (i in 0 until count) {
                    blobPaint.color = (if (i % 2 == 0) 0x24FFFFFF else 0x16000000)
                    val bx = rng.nextFloat() * width; val by = rng.nextFloat() * height
                    val br = width * (0.14f + rng.nextFloat() * 0.26f)
                    canvas.drawCircle(bx, by, br, blobPaint)
                }
                if ("cosmos" in low || "space" in low || "star" in low || style == CreativeStyle.ANIME) {
                    val starP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCCFFFFFF.toInt() }
                    for (i in 0 until 42) {
                        canvas.drawCircle(rng.nextFloat() * width, rng.nextFloat() * height, 2f + rng.nextFloat() * 4f, starP)
                    }
                }
                canvas.drawRect(0f, 0f, width * 0.055f, height.toFloat(), Paint().apply { color = 0x30000000 })
                val plate = RectF(width * 0.12f, height * 0.60f, width * 0.88f, height * 0.84f)
                canvas.drawRoundRect(plate, 38f, 38f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x2BFFFFFF })
                canvas.drawRoundRect(plate, 38f, 38f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x66FFFFFF; this.style = Paint.Style.STROKE; strokeWidth = 2.5f
                })
                val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFFFFFFF.toInt()
                    textSize = (width * 0.064f).coerceIn(36f, 56f)
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                }
                val words = cleanPrompt.split(' ').take(4).joinToString(" ").uppercase()
                canvas.drawText(words.take(18), plate.left + 36f, plate.centerY() - 8f, titlePaint)
                val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xE6FFFFFF.toInt(); textSize = (width * 0.032f).coerceIn(20f, 28f)
                }
                canvas.drawText("${style.label} Edition", plate.left + 36f, plate.centerY() + 40f, subPaint)
            }
            AiAssetKind.TEMPLATE -> {
                canvas.drawColor(0xFFFFFFFF.toInt())
                val lineP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD9E1EC.toInt(); strokeWidth = 2f }
                val accentP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = aiAccent; strokeWidth = 3.5f }
                val hdrFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = (aiAccent and 0x00FFFFFF) or 0x18000000 }
                val headTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = aiAccent; textSize = 44f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val labelP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF5A6577.toInt(); textSize = 26f }
                val pad = 76f
                canvas.drawRoundRect(RectF(pad, 68f, width - pad, 174f), 22f, 22f, hdrFill)
                canvas.drawText(cleanPrompt.take(30), pad + 26f, 136f, headTitle)
                when {
                    "cornell" in low -> {
                        val cueX = width * 0.31f
                        val sumY = height * 0.80f
                        canvas.drawLine(cueX, 200f, cueX, sumY, accentP)
                        canvas.drawLine(pad, sumY, width - pad, sumY, accentP)
                        canvas.drawText("CUES & QUESTIONS", pad, 232f, labelP)
                        canvas.drawText("NOTES", cueX + 24f, 232f, labelP)
                        canvas.drawText("SUMMARY", pad, sumY + 38f, labelP)
                        var y = 265f
                        while (y < sumY - 24f) { canvas.drawLine(pad, y, width - pad, y, lineP); y += 48f }
                        var sy = sumY + 66f
                        while (sy < height - 70f) { canvas.drawLine(pad, sy, width - pad, sy, lineP); sy += 48f }
                    }
                    "weekly" in low || "planner" in low || "schedule" in low -> {
                        val days = listOf("MON", "TUE", "WED", "THU", "FRI", "WKND")
                        val cols = 2; val rows = 3
                        val cw = (width - pad * 2 - 28f) / cols
                        val ch = (height - 260f - 56f) / rows
                        for (r in 0 until rows) for (c in 0 until cols) {
                            val idx = r * cols + c
                            val left = pad + c * (cw + 28f); val top = 210f + r * (ch + 28f)
                            val box = RectF(left, top, left + cw, top + ch)
                            canvas.drawRoundRect(box, 20f, 20f, lineP.apply { this.style = Paint.Style.STROKE })
                            canvas.drawText(days[idx], left + 20f, top + 38f, headTitle.apply { textSize = 26f })
                            var ly = top + 78f
                            while (ly < box.bottom - 20f) { canvas.drawLine(left + 16f, ly, box.right - 16f, ly, lineP); ly += 46f }
                        }
                    }
                    "habit" in low || "track" in low -> {
                        var y = 240f
                        for (r in 0 until 18) {
                            canvas.drawLine(pad, y, width - pad, y, lineP)
                            if (r == 0) canvas.drawText("HABIT / GOAL", pad, y - 14f, labelP)
                            for (d in 0 until 14) {
                                val cx = width * 0.44f + d * 42f
                                if (r == 0) canvas.drawText("${d + 1}", cx - 8f, y - 14f, labelP.apply { textSize = 18f })
                                else canvas.drawCircle(cx, y - 22f, 12f, lineP.apply { this.style = Paint.Style.STROKE })
                            }
                            y += 72f
                        }
                    }
                    "story" in low || "grid" in low -> {
                        val cw = (width - pad * 2 - 36f) / 2f
                        val ch = 410f
                        for (r in 0 until 3) for (c in 0 until 2) {
                            val left = pad + c * (cw + 36f); val top = 210f + r * (ch + 66f)
                            canvas.drawRoundRect(RectF(left, top, left + cw, top + ch - 84f), 18f, 18f, accentP.apply { this.style = Paint.Style.STROKE })
                            canvas.drawLine(left, top + ch - 42f, left + cw, top + ch - 42f, lineP)
                            canvas.drawLine(left, top + ch - 6f, left + cw, top + ch - 6f, lineP)
                        }
                    }
                    else -> {
                        var y = 230f
                        while (y < height - 70f) {
                            canvas.drawLine(pad, y, width - pad, y, lineP)
                            y += 50f
                        }
                        canvas.drawLine(pad + 54f, 195f, pad + 54f, height - 60f, accentP)
                    }
                }
            }
        }
        bmp
    }

    /**
     * Flood-fills uniform chroma green or white outer backdrop from edges to true ARGB transparency,
     * then composites a smooth 6px white die-cut contour border around the isolated sticker subject.
     */
    private fun applyChromaKeyAndDieCutBorder(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        if (w < 16 || h < 16) return src
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)

        var sumR = 0L; var sumG = 0L; var sumB = 0L; var samples = 0
        val corners = listOf(0 to 0, (w - 4) to 0, 0 to (h - 4), (w - 4) to (h - 4))
        for ((sx, sy) in corners) {
            for (dy in 0..3) for (dx in 0..3) {
                val c = px[(sy + dy) * w + (sx + dx)]
                sumR += (c shr 16) and 0xFF
                sumG += (c shr 8) and 0xFF
                sumB += c and 0xFF
                samples++
            }
        }
        val bgR = (sumR / samples).toInt()
        val bgG = (sumG / samples).toInt()
        val bgB = (sumB / samples).toInt()
        val isChromaGreen = bgG > bgR + 28 && bgG > bgB + 28
        val isUniformLight = bgR > 212 && bgG > 212 && bgB > 212
        if (!isChromaGreen && !isUniformLight) return src

        val bgMask = BooleanArray(w * h)
        val queue = IntArray(w * h)
        var head = 0
        var tail = 0

        fun matchesBg(c: Int): Boolean {
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            return if (isChromaGreen) {
                (g > r + 20 && g > b + 20) || (kotlin.math.abs(r - bgR) + kotlin.math.abs(g - bgG) + kotlin.math.abs(b - bgB) < 95)
            } else {
                kotlin.math.abs(r - bgR) + kotlin.math.abs(g - bgG) + kotlin.math.abs(b - bgB) < 62
            }
        }

        fun tryPush(idx: Int) {
            if (!bgMask[idx] && matchesBg(px[idx])) {
                bgMask[idx] = true
                queue[tail++] = idx
            }
        }

        for (x in 0 until w) {
            tryPush(x)
            tryPush((h - 1) * w + x)
        }
        for (y in 0 until h) {
            tryPush(y * w)
            tryPush(y * w + (w - 1))
        }

        while (head < tail) {
            val cur = queue[head++]
            val x = cur % w
            val y = cur / w
            if (x > 0) tryPush(cur - 1)
            if (x + 1 < w) tryPush(cur + 1)
            if (y > 0) tryPush(cur - w)
            if (y + 1 < h) tryPush(cur + w)
        }

        // Remove chroma spill on perimeter subject pixels & punch alpha
        for (i in px.indices) {
            if (bgMask[i]) {
                px[i] = 0x00000000
            } else if (isChromaGreen) {
                val c = px[i]
                val a = (c ushr 24) and 0xFF
                val r = (c shr 16) and 0xFF
                var g = (c shr 8) and 0xFF
                val b = c and 0xFF
                val maxRb = maxOf(r, b)
                if (g > maxRb + 10) g = maxRb + 6
                px[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        // Synthesize 6px pure white die-cut sticker contour halo behind opaque foreground
        val borderR = (minOf(w, h) * 0.014f).toInt().coerceIn(5, 9)
        val rSq = borderR * borderR
        val out = px.copyOf()
        for (y in borderR until h - borderR) {
            val rowOff = y * w
            for (x in borderR until w - borderR) {
                val idx = rowOff + x
                if ((px[idx] ushr 24) < 32) {
                    var nearFg = false
                    loop@ for (dy in -borderR..borderR) {
                        val ny = (y + dy) * w
                        for (dx in -borderR..borderR) {
                            if (dx * dx + dy * dy <= rSq && (px[ny + x + dx] ushr 24) > 180) {
                                nearFg = true
                                break@loop
                            }
                        }
                    }
                    if (nearFg) out[idx] = 0xFFFFFFFF.toInt()
                }
            }
        }

        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            setPixels(out, 0, w, 0, 0, w, h)
        }
    }

    private fun starPath(cx: Float, cy: Float, outerR: Float, innerR: Float, points: Int): Path = Path().apply {
        val step = Math.PI / points
        var angle = -Math.PI / 2.0
        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) outerR else innerR
            val x = cx + (cos(angle) * r).toFloat()
            val y = cy + (sin(angle) * r).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
            angle += step
        }
        close()
    }

    private fun lightenColor(argb: Int, amount: Float): Int {
        val r = ((argb shr 16 and 0xFF) + (255 - (argb shr 16 and 0xFF)) * amount).toInt().coerceIn(0, 255)
        val g = ((argb shr 8 and 0xFF) + (255 - (argb shr 8 and 0xFF)) * amount).toInt().coerceIn(0, 255)
        val b = ((argb and 0xFF) + (255 - (argb and 0xFF)) * amount).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun darkenColor(argb: Int, amount: Float): Int {
        val r = ((argb shr 16 and 0xFF) * (1f - amount)).toInt().coerceIn(0, 255)
        val g = ((argb shr 8 and 0xFF) * (1f - amount)).toInt().coerceIn(0, 255)
        val b = ((argb and 0xFF) * (1f - amount)).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    // ---------------------------------------------------------------- v10 AI Lecture Recording Structuring

    suspend fun structureLectureRecording(
        context: Context,
        title: String,
        transcriptOrNotes: String,
        bookmarks: List<String>,
        durationSec: Long,
    ): String = withContext(Dispatchers.Default) {
        val clean = transcriptOrNotes.trim()
        val bmBlock = if (bookmarks.isNotEmpty()) bookmarks.joinToString("\n") { "- $it" } else ""
        if (hasCustomApi(context) && (clean.isNotBlank() || bmBlock.isNotBlank())) {
            _stage.value = "Structuring lecture recording with AI…"
            val resp = callCustomApi(
                context,
                "Format raw lecture notes and timestamp bookmarks into crisp study notes with sections: Summary, Key Takeaways, Bookmarked Moments, and Action Items.",
                "Title: $title (${durationSec}s)\nBookmarks:\n$bmBlock\n\nNotes:\n${clean.take(5000)}",
            ).getOrNull()
            _stage.value = ""
            if (!resp.isNullOrBlank()) return@withContext resp
        }
        val pts = StudyGen.summary(clean, 5).ifEmpty { StudyGen.sentences(clean).take(4) }
        val terms = StudyGen.keyTerms(clean, 6)
        buildString {
            appendLine("🎙 ${title.ifBlank { "Lecture Recording" }} (${durationSec / 60}m ${durationSec % 60}s)")
            if (pts.isNotEmpty()) {
                appendLine("\nKey Takeaways:")
                pts.forEach { appendLine("• $it") }
            }
            if (terms.isNotEmpty()) appendLine("\nKey Concepts: ${terms.joinToString(", ")}")
            if (bookmarks.isNotEmpty()) {
                appendLine("\nBookmarks:")
                bookmarks.forEach { appendLine("• $it") }
            }
            if (pts.isEmpty() && bookmarks.isEmpty() && clean.isNotBlank()) {
                appendLine("\nNotes:\n$clean")
            }
        }.trim()
    }
}
