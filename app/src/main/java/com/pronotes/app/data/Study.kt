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
    val lapses: Int = 0,
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
    /** Picture shown with the question (file in the notebook's cards folder). */
    val image: String? = null,
)

@Serializable
enum class SlideKind(val label: String) {
    TITLE("Cover"),
    CONCEPT("Concept"),
    FORMULA("Formula"),
    CHECK_QA("Check Q&A"),
    SUMMARY("Recap"),
}

@Serializable
enum class SlideTheme(val label: String, val gradA: Long, val gradB: Long, val accent: Long) {
    AURORA_INDIGO("Aurora Indigo", 0xFF1E1B4B, 0xFF4338CA, 0xFF818CF8),
    SUNSET_CORAL("Sunset Coral", 0xFF7C2D12, 0xFFEA580C, 0xFFFDBA74),
    EMERALD_MINT("Emerald Mint", 0xFF064E3B, 0xFF059669, 0xFF6EE7B7),
    COSMIC_GOLD("Cosmic Gold", 0xFF78350F, 0xFFD97706, 0xFFFDE68A),
    MIDNIGHT_PRISM("Midnight Prism", 0xFF0F172A, 0xFF1E40AF, 0xFF38BDF8),
    ROSE_VELVET("Rose Velvet", 0xFF881337, 0xFFE11D48, 0xFFFDA4AF),
}

@Serializable
data class SlideCard(
    val id: String,
    val kind: SlideKind = SlideKind.CONCEPT,
    val theme: SlideTheme = SlideTheme.AURORA_INDIGO,
    val title: String,
    val subtitle: String = "",
    val bullets: List<String> = emptyList(),
    val callout: String = "",
    val question: String = "",
    val answer: String = "",
    val badge: String = "",
)

@Serializable
data class StudyData(
    val cards: List<Flashcard> = emptyList(),
    val links: List<InkLink> = emptyList(),
    val quiz: List<QuizQuestion> = emptyList(),
    val slides: List<SlideCard> = emptyList(),
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

        /** Spaced repetition (4 grades): 0 = Again, 1 = Hard, 2 = Good, 3 = Easy. */
        fun review(card: Flashcard, grade: Int, now: Long = System.currentTimeMillis()): Flashcard {
            val box = when (grade) {
                0 -> 0
                1 -> card.box.coerceAtLeast(1)
                2 -> (card.box + 1).coerceAtMost(5)
                else -> (card.box + 2).coerceAtMost(5)
            }
            val due = when (grade) {
                0 -> now + 10 * 60 * 1000
                1 -> now + 12 * 60 * 60 * 1000
                else -> now + gaps[box].coerceAtLeast(1) * DAY
            }
            return card.copy(
                box = box,
                due = due,
                reviews = card.reviews + 1,
                lapses = card.lapses + if (grade == 0) 1 else 0,
            )
        }
    }
}
