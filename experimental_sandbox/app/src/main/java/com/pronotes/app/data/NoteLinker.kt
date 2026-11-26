package com.pronotes.app.data

/**
 * Finds potential cross-links between notes by detecting shared named entities
 * (capitalized multi-word phrases and proper nouns) across a note corpus.
 *
 * Returns a list of [CrossLink]s: pairs of note indices that share at least
 * one common named entity, ranked by number of shared entities (strongest links first).
 */
object NoteLinker {

    data class CrossLink(
        val noteAIndex: Int,
        val noteBIndex: Int,
        val sharedEntities: List<String>,
        val strength: Int,  // number of shared entities
    )

    // Matches runs of Title-Cased or ALL-CAPS words (2+ chars), length 1-4 words
    private val ENTITY_RE = Regex("""(?:[A-Z][a-zA-Z]{1,}(?:\s+[A-Z][a-zA-Z]{1,}){0,3})""")
    private val STOP_ENTITIES = setOf(
        "The", "This", "That", "These", "Those", "With", "From", "When", "Where",
        "What", "How", "Which", "They", "It", "Its", "He", "She", "We", "You",
        "Also", "And", "Or", "But", "So", "If", "In", "On", "At", "By", "For",
        "To", "Of", "As", "An", "A",
    )

    fun findLinks(notes: List<String>, minStrength: Int = 1): List<CrossLink> {
        val entitySets = notes.map { extractEntities(it) }
        val links = mutableListOf<CrossLink>()

        for (i in entitySets.indices) {
            for (j in i + 1 until entitySets.size) {
                val shared = entitySets[i].intersect(entitySets[j]).toList().sorted()
                if (shared.size >= minStrength) {
                    links.add(CrossLink(
                        noteAIndex = i,
                        noteBIndex = j,
                        sharedEntities = shared,
                        strength = shared.size,
                    ))
                }
            }
        }

        return links.sortedByDescending { it.strength }
    }

    /**
     * Returns the best note index to link to for a given note (the one sharing the most entities).
     */
    fun suggestLinks(notes: List<String>, targetIndex: Int, maxSuggestions: Int = 3): List<CrossLink> =
        findLinks(notes).filter { it.noteAIndex == targetIndex || it.noteBIndex == targetIndex }
            .take(maxSuggestions)

    private fun extractEntities(text: String): Set<String> =
        ENTITY_RE.findAll(text)
            .map { it.value.trim() }
            .filter { it !in STOP_ENTITIES && it.length >= 4 }
            .toSet()
}
