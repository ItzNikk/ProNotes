package com.pronotes.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * A flashcard. Either side can hold text, a picture cut from a page, or both.
 * [box] is the Leitner box (0 = new / forgotten, 5 = well known); [due] is when it should be shown again.
 */
@Serializable
data class Flashcard(
    val id: String,
    val front: String = "",
    val back: String = "",
    val frontImage: String? = null,
    val backImage: String? = null,
    /** Page the card was cut from, so "Show in notebook" can jump back. */
    val pageId: String? = null,
    val box: Int = 0,
    val due: Long = 0L,
    val reviews: Int = 0,
    val created: Long = 0L,
)

/** An ink link: a tap on one end jumps to the other, across pages. Points are in page space. */
@Serializable
data class InkLink(
    val id: String,
    val aPage: String, val ax: Float, val ay: Float,
    val bPage: String = "", val bx: Float = 0f, val by: Float = 0f,
    /** Linked content box (left, top, right, bottom), like GoodNotes links. Empty for old point links. */
    val box: List<Float> = emptyList(),
    /** Web address, when the link goes to a website instead of a page. */
    val url: String? = null,
)

/** A multiple-choice question. [answer] is the index of the right option. */
@Serializable
data class QuizQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val answer: Int,
    val explanation: String = "",
    val pageId: String? = null,
    val generated: Boolean = false,
    val right: Int = 0,
    val wrong: Int = 0,
)

@Serializable
data class StudyData(
    val cards: List<Flashcard> = emptyList(),
    val links: List<InkLink> = emptyList(),
    val quiz: List<QuizQuestion> = emptyList(),
)

/** Flashcards and ink links of one notebook, kept in `study.json` next to its pages. */
class StudyStore(private val dir: File) {
    private val file = File(dir, "study.json")
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): StudyData = runCatching { json.decodeFromString(StudyData.serializer(), file.readText()) }.getOrDefault(StudyData())

    fun save(data: StudyData) {
        val tmp = File(dir, "study.json.tmp")
        tmp.writeText(json.encodeToString(StudyData.serializer(), data))
        if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
    }

    fun cardDir(): File = File(dir, "cards").apply { mkdirs() }

    companion object {
        private const val DAY = 24L * 60 * 60 * 1000
        /** Days until the next review for each box. */
        private val gaps = longArrayOf(0, 1, 3, 7, 16, 35)

        /** Spaced repetition: "again" starts over, "good" moves up one box, "easy" two. */
        fun review(card: Flashcard, grade: Int, now: Long = System.currentTimeMillis()): Flashcard {
            val box = when (grade) {
                0 -> 0
                1 -> (card.box + 1).coerceAtMost(5)
                else -> (card.box + 2).coerceAtMost(5)
            }
            val due = if (grade == 0) now + 10 * 60 * 1000 else now + gaps[box].coerceAtLeast(1) * DAY
            return card.copy(box = box, due = due, reviews = card.reviews + 1)
        }
    }
}
