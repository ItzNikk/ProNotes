package com.pronotes.app.editor

/**
 * Lightweight no-op stub while AI features are disabled for maximum smoothness.
 * All study tools (Flashcards, Quiz, Summary, MindMap) use fast deterministic rule-based extraction.
 */
object Nano {
    @Volatile var available: Boolean = false
        private set

    suspend fun ready(): Boolean = false

    suspend fun ask(prompt: String, timeoutMs: Long = 45_000): String? = null

    fun clip(text: String, words: Int = 2200): String =
        text.split(Regex("\\s+")).take(words).joinToString(" ")

    suspend fun flashcards(text: String, max: Int): List<StudyGen.Card>? = null

    suspend fun quiz(text: String, max: Int): List<StudyGen.Mcq>? = null

    suspend fun quizFromImage(image: android.graphics.Bitmap, max: Int): List<StudyGen.Mcq>? = null

    suspend fun summary(text: String): List<String>? = null

    suspend fun explain(selection: String, context: String): String? = null

    suspend fun mindMap(title: String, text: String): Pair<String, List<Pair<String, List<String>>>>? = null
}
