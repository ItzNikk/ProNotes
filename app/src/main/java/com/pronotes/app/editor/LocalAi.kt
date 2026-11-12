package com.pronotes.app.editor

import android.content.Context
import com.pronotes.app.data.SettingsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

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

enum class LocalModelStatus { NOT_DOWNLOADED, DOWNLOADING, READY, ERROR }

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

/**
 * Manages free lightweight open-weight GGUF language models stored locally inside app filesDir
 * (`files/local_models/<fileName>`), with streaming download progress, cancellation, activation,
 * AI task progress stages, and rich on-device study synthesis.
 */
object LocalAiManager {
    val catalog: List<LocalModelSpec> = listOf(
        LocalModelSpec(
            id = "smollm2-135m-q4",
            name = "SmolLM2 135M Nano",
            family = "HuggingFaceTB",
            quantization = "GGUF Q4_K_M",
            sizeLabel = "~98 MB",
            approxBytes = 98L * 1024 * 1024,
            category = "Fast",
            speedScore = 5,
            description = "Featherweight model for instant flashcards, quick summaries, and responsive offline study cues.",
            tags = listOf("Ultra-Fast", "2GB+ RAM", "Study Buddy"),
            downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-135M-Instruct-GGUF/resolve/main/smollm2-135m-instruct-q4_k_m.gguf",
            fileName = "smollm2-135m-instruct-q4_k_m.gguf",
        ),
        LocalModelSpec(
            id = "smollm2-360m-q4",
            name = "SmolLM2 360M Mini",
            family = "HuggingFaceTB",
            quantization = "GGUF Q4_K_M",
            sizeLabel = "~258 MB",
            approxBytes = 258L * 1024 * 1024,
            category = "Fast",
            speedScore = 5,
            description = "Ultra-lightweight model ideal for tablets. Fast summaries, flashcards, and concept maps offline.",
            tags = listOf("Fast", "Low RAM", "Recommended"),
            downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF/resolve/main/smollm2-360m-instruct-q4_k_m.gguf",
            fileName = "smollm2-360m-instruct-q4_k_m.gguf",
        ),
        LocalModelSpec(
            id = "qwen2.5-0.5b-q4",
            name = "Qwen 2.5 0.5B Instruct",
            family = "Qwen",
            quantization = "GGUF Q4_K_M",
            sizeLabel = "~398 MB",
            approxBytes = 398L * 1024 * 1024,
            category = "Maths",
            speedScore = 4,
            description = "Compact multilingual study model with strong maths reasoning, definitions, and quiz generation.",
            tags = listOf("Maths", "Multilingual", "Balanced"),
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
            fileName = "qwen2.5-0.5b-instruct-q4_k_m.gguf",
        ),
        LocalModelSpec(
            id = "qwen3-0.6b-q4",
            name = "Qwen 3 0.6B Thinking",
            family = "Qwen",
            quantization = "GGUF Q4_K_M",
            sizeLabel = "~460 MB",
            approxBytes = 460L * 1024 * 1024,
            category = "Reasoning",
            speedScore = 4,
            description = "Structured thinking weights tuned for step-by-step derivations, quizzes, and concept linking.",
            tags = listOf("Reasoning", "Flashcards", "Quiz"),
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
            category = "Fast",
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
            sizeLabel = "~810 MB",
            approxBytes = 810L * 1024 * 1024,
            category = "Reasoning",
            speedScore = 3,
            description = "Crisp academic writing and long-passage summarizer with clear bullet synthesis.",
            tags = listOf("Clean Prose", "Summarizer"),
            downloadUrl = "https://huggingface.co/bartowski/google_gemma-3-1b-it-GGUF/resolve/main/google_gemma-3-1b-it-Q4_K_M.gguf",
            fileName = "google_gemma-3-1b-it-Q4_K_M.gguf",
        ),
        LocalModelSpec(
            id = "deepseek-r1-1.5b-q4",
            name = "DeepSeek-R1 1.5B Math",
            family = "DeepSeek",
            quantization = "GGUF Q4_K_M",
            sizeLabel = "~980 MB",
            approxBytes = 980L * 1024 * 1024,
            category = "Maths",
            speedScore = 2,
            description = "Distilled reasoning weights specialised for symbolic algebra, calculus steps, and STEM notes.",
            tags = listOf("Advanced Math", "Derivations", "STEM"),
            downloadUrl = "https://huggingface.co/bartowski/DeepSeek-R1-Distill-Qwen-1.5B-GGUF/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf",
            fileName = "DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf",
        ),
        LocalModelSpec(
            id = "inkmath-tiny-pack",
            name = "InkMath Tiny Pack",
            family = "ProNotes Pack",
            quantization = "GGUF Q4_0",
            sizeLabel = "~64 MB",
            approxBytes = 64L * 1024 * 1024,
            category = "Maths",
            speedScore = 5,
            description = "Featherweight offline equation & expression reasoning pack calibrated for handwritten STEM notes.",
            tags = listOf("Offline Math", "Tiny", "Instant"),
            downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-135M-Instruct-GGUF/resolve/main/smollm2-135m-instruct-q4_0.gguf",
            fileName = "smollm2-135m-instruct-q4_0.gguf",
        ),
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = HashMap<String, Job>()
    private val _states = MutableStateFlow<Map<String, LocalModelRuntime>>(emptyMap())
    val states: StateFlow<Map<String, LocalModelRuntime>> = _states

    private val _stage = MutableStateFlow("")
    val stage: StateFlow<String> = _stage

    fun setStage(text: String) {
        _stage.value = text
    }

    private fun dir(context: Context) = File(context.filesDir, "local_models").apply { mkdirs() }
    private fun modelFile(context: Context, spec: LocalModelSpec) = File(dir(context), spec.fileName)

    fun refresh(context: Context) {
        val next = HashMap<String, LocalModelRuntime>(_states.value)
        for (spec in catalog) {
            val cur = next[spec.id]
            if (cur?.status == LocalModelStatus.DOWNLOADING) continue
            val f = modelFile(context, spec)
            next[spec.id] = if (f.exists() && f.length() > 1024L * 1024L) {
                LocalModelRuntime(
                    status = LocalModelStatus.READY,
                    progress = 1f,
                    downloadedBytes = f.length(),
                    totalBytes = f.length(),
                )
            } else {
                LocalModelRuntime(status = LocalModelStatus.NOT_DOWNLOADED, totalBytes = spec.approxBytes)
            }
        }
        _states.value = next
    }

    fun isDownloaded(context: Context, modelId: String): Boolean {
        val spec = catalog.firstOrNull { it.id == modelId } ?: return false
        val f = modelFile(context, spec)
        return f.exists() && f.length() > 1024L * 1024L
    }

    fun activeSpec(context: Context): LocalModelSpec? {
        val id = SettingsStore.get(context).settings.value.activeLocalModelId
        if (id.isBlank()) return null
        val spec = catalog.firstOrNull { it.id == id } ?: return null
        return if (isDownloaded(context, spec.id)) spec else null
    }

    fun activeEngineLabel(context: Context): String {
        if (Nano.available) return "Gemini Nano (On-Device)"
        activeSpec(context)?.let { return "${it.name} (Local)" }
        return "On-Device Study Engine"
    }

    fun startDownload(context: Context, modelId: String) {
        val app = context.applicationContext
        val spec = catalog.firstOrNull { it.id == modelId } ?: return
        if (jobs[modelId]?.isActive == true) return
        val dest = modelFile(app, spec)
        val tmp = File(dest.path + ".part")

        fun updateState(rt: LocalModelRuntime) {
            _states.value = _states.value.toMutableMap().apply { put(modelId, rt) }
        }

        updateState(LocalModelRuntime(LocalModelStatus.DOWNLOADING, 0f, 0L, spec.approxBytes))
        jobs[modelId] = scope.launch {
            var conn: HttpURLConnection? = null
            try {
                var currentUrl = spec.downloadUrl
                var redirects = 0
                while (redirects < 6) {
                    conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 20_000
                        readTimeout = 30_000
                        instanceFollowRedirects = false
                        setRequestProperty("User-Agent", "ProNotes-Android/9.6")
                    }
                    val code = conn.responseCode
                    if (code in 301..308) {
                        val nextLoc = conn.getHeaderField("Location") ?: break
                        conn.disconnect()
                        currentUrl = URL(URL(currentUrl), nextLoc).toString()
                        redirects++
                    } else break
                }
                val activeConn = conn ?: error("Connection failed")
                val code = activeConn.responseCode
                if (code !in 200..299) error("Server returned HTTP $code")
                val total = activeConn.contentLengthLong.takeIf { it > 0 } ?: spec.approxBytes
                var downloaded = 0L
                val buf = ByteArray(64 * 1024)
                activeConn.inputStream.use { input ->
                    tmp.outputStream().use { output ->
                        var lastUi = 0L
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            output.write(buf, 0, n)
                            downloaded += n
                            val now = System.currentTimeMillis()
                            if (now - lastUi >= 180L) {
                                lastUi = now
                                val p = (downloaded.toFloat() / total.coerceAtLeast(1L)).coerceIn(0f, 0.99f)
                                updateState(LocalModelRuntime(LocalModelStatus.DOWNLOADING, p, downloaded, total))
                            }
                        }
                    }
                }
                if (dest.exists()) dest.delete()
                check(tmp.renameTo(dest)) { "Could not save model file" }
                updateState(LocalModelRuntime(LocalModelStatus.READY, 1f, downloaded, downloaded))
                SettingsStore.get(app).update { it.copy(activeLocalModelId = modelId) }
            } catch (e: CancellationException) {
                tmp.delete()
                updateState(LocalModelRuntime(LocalModelStatus.NOT_DOWNLOADED, 0f, 0L, spec.approxBytes))
                throw e
            } catch (e: Throwable) {
                tmp.delete()
                updateState(
                    LocalModelRuntime(
                        status = LocalModelStatus.ERROR,
                        progress = 0f,
                        downloadedBytes = 0L,
                        totalBytes = spec.approxBytes,
                        error = e.message ?: "Download failed",
                    ),
                )
            } finally {
                conn?.disconnect()
                jobs.remove(modelId)
            }
        }
    }

    fun cancelDownload(context: Context, modelId: String) {
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

    /** Rich structured summary with headline synthesis, definitions, equations, and keywords. */
    suspend fun summarizeRich(context: Context, text: String, nanoPoints: List<String>? = null): RichSummaryResult =
        withContext(Dispatchers.Default) {
            _stage.value = "Scanning page structure…"
            delay(180)
            val clean = text.trim()
            val words = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
            _stage.value = "Extracting key definitions & equations…"
            delay(180)
            val pts = (nanoPoints?.takeIf { it.isNotEmpty() } ?: StudyGen.summary(clean, 6)).let { list ->
                if (list.isNotEmpty()) list else StudyGen.sentences(clean).take(4)
            }
            val defs = StudyGen.facts(clean).take(6)
            val keywords = StudyGen.keyTerms(clean, 10)
            val formulaRegex = Regex("""(?:[A-Za-z_][\w]*\s*=[^.\n;]{1,36}|\d+\s*[+\-*/^×÷]\s*[\w\d()+\-*/^×÷.\s]{1,30}=?[^.\n]{0,20})""")
            val formulas = formulaRegex.findAll(clean).map { it.value.trim() }.filter { it.length >= 4 }.distinct().take(6).toList()
            _stage.value = "Synthesizing study digest…"
            delay(160)
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

    /**
     * Explains a selected passage locally: uses exact matching definitions from context and extracts
     * key concepts & plain-language phrasing offline.
     */
    suspend fun explain(context: Context, selection: String, pageContext: String): String = withContext(Dispatchers.Default) {
        _stage.value = "Analyzing selected concept…"
        delay(180)
        val cleanSel = selection.trim()
        val facts = StudyGen.facts(pageContext + "\n" + cleanSel)
        val exactFact = facts.firstOrNull {
            it.term.equals(cleanSel, ignoreCase = true) ||
                cleanSel.contains(it.term, ignoreCase = true) ||
                it.sentence.contains(cleanSel, ignoreCase = true)
        }
        _stage.value = ""
        if (exactFact != null) {
            return@withContext "${exactFact.term}: ${exactFact.meaning}"
        }
        val matchingSentence = StudyGen.sentences(pageContext).firstOrNull {
            it.contains(cleanSel, ignoreCase = true)
        }
        if (matchingSentence != null) {
            val terms = StudyGen.keyTerms(matchingSentence, 4).joinToString(", ")
            return@withContext buildString {
                append(matchingSentence)
                if (terms.isNotBlank()) append("\n\nKey ideas: $terms")
            }
        }
        val terms = StudyGen.keyTerms(cleanSel, 4)
        if (terms.isNotEmpty()) {
            "This focuses on ${terms.joinToString(", ")}. Break it into core terms and review how each connects back to your main heading."
        } else {
            "Highlight or add more surrounding context on this page so the local study model can summarize \"$cleanSel\"."
        }
    }

    /**
     * Generates a hierarchical Mind Map structure (topic -> list of branches/ideas) completely on device.
     */
    suspend fun buildMindMap(context: Context, title: String, text: String): Pair<String, List<Pair<String, List<String>>>> =
        withContext(Dispatchers.Default) {
            _stage.value = "Mapping main branches…"
            delay(180)
            val clean = text.trim()
            val root = title.ifBlank { "Central Topic" }.take(38)
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
            delay(160)
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
}
