package com.pronotes.app.editor

import android.content.Context
import com.pronotes.app.data.CustomAiProvider
import com.pronotes.app.data.SettingsStore
import com.pronotes.app.data.SlideCard
import com.pronotes.app.data.SlideKind
import com.pronotes.app.data.SlideTheme
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

data class SearchAiResult(
    val expandedTerms: List<String> = emptyList(),
    val answer: String = "",
    val engineLabel: String = "",
)

/**
 * Unified AI & On-Device Model hub:
 * Priority chain -> Custom AI API (OpenAI-compatible / Gemini REST / Ollama) -> Gemini Nano -> Downloaded GGUF -> Offline StudyGen.
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
            description = "Featherweight model for instant flashcards, quick summaries, semantic search, and offline study cues.",
            tags = listOf("Ultra-Fast", "2GB+ RAM", "Lightweight"),
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
            description = "Ultra-lightweight model ideal for tablets. Fast summaries, slide decks, flashcards, and concept maps offline.",
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
            category = "Writing",
            speedScore = 4,
            description = "Compact multilingual study model with crisp outline synthesis, definitions, and quiz generation.",
            tags = listOf("Multilingual", "Outlines", "Balanced"),
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
            description = "Structured thinking weights tuned for step-by-step explanations, quizzes, slide decks, and concept linking.",
            tags = listOf("Reasoning", "Flashcards", "Slides"),
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
            sizeLabel = "~810 MB",
            approxBytes = 810L * 1024 * 1024,
            category = "Writing",
            speedScore = 3,
            description = "Crisp academic writing and long-passage summarizer with clear bullet synthesis.",
            tags = listOf("Clean Prose", "Summarizer"),
            downloadUrl = "https://huggingface.co/bartowski/google_gemma-3-1b-it-GGUF/resolve/main/google_gemma-3-1b-it-Q4_K_M.gguf",
            fileName = "google_gemma-3-1b-it-Q4_K_M.gguf",
        ),
    )

    private val json = Json { ignoreUnknownKeys = true }
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

    fun hasCustomApi(context: Context): Boolean {
        val s = SettingsStore.get(context).settings.value
        if (!s.customAiEnabled || s.customAiBaseUrl.isBlank() || s.customAiModel.isBlank()) return false
        return s.customAiProvider == CustomAiProvider.OLLAMA || s.customAiApiKey.isNotBlank()
    }

    fun activeEngineLabel(context: Context): String {
        val s = SettingsStore.get(context).settings.value
        if (hasCustomApi(context)) return "Custom API · ${s.customAiModel}"
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
                        setRequestProperty("User-Agent", "ProNotes-Android/9.11")
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

    // ---------------------------------------------------------------- Custom AI REST Client

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

    // ---------------------------------------------------------------- Explain

    suspend fun explain(context: Context, selection: String, pageContext: String): String = withContext(Dispatchers.Default) {
        val cleanSel = selection.trim()
        if (hasCustomApi(context) && cleanSel.isNotBlank()) {
            _stage.value = "Asking Custom AI…"
            val ai = callCustomApi(
                context,
                "You are a clear tutor. Explain the highlighted concept simply in 2-4 crisp sentences using the note context.",
                "Highlight: $cleanSel\n\nContext:\n${pageContext.take(4000)}",
            ).getOrNull()
            if (!ai.isNullOrBlank()) {
                _stage.value = ""
                return@withContext ai
            }
        }
        _stage.value = "Analyzing selected concept…"
        delay(140)
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

        // Fast local-model concept expansion & synthesized answer
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
}
