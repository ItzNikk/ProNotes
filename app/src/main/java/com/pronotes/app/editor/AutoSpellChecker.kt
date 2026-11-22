package com.pronotes.app.editor

import kotlin.math.abs
import kotlin.math.min

/**
 * Fast, offline Auto Spell Check engine for both converted handwriting and typed text.
 * Handles common typing typos, OCR confusions, and closest-dictionary Levenshtein fixes
 * while preserving original capitalization and surrounding punctuation.
 */
object AutoSpellChecker {
    private val directFixes: Map<String, String> = mapOf(
        "teh" to "the", "hte" to "the", "thier" to "their", "recieve" to "receive",
        "recieved" to "received", "acheive" to "achieve", "beleive" to "believe",
        "seperate" to "separate", "definately" to "definitely", "definatly" to "definitely",
        "occured" to "occurred", "occurence" to "occurrence", "untill" to "until",
        "tommorow" to "tomorrow", "tommorrow" to "tomorrow", "wierd" to "weird",
        "freind" to "friend", "freinds" to "friends", "frend" to "friend",
        "becuase" to "because", "becouse" to "because", "beacuse" to "because",
        "wich" to "which", "whitch" to "which", "writting" to "writing",
        "studing" to "studying", "studys" to "studies", "exampel" to "example",
        "exmple" to "example", "improtant" to "important", "importnat" to "important",
        "diffrent" to "different", "diferent" to "different", "diffcult" to "difficult",
        "langauge" to "language", "languge" to "language", "knowlege" to "knowledge",
        "knwoledge" to "knowledge", "calender" to "calendar", "grammer" to "grammar",
        "neccessary" to "necessary", "necesary" to "necessary", "enviroment" to "environment",
        "goverment" to "government", "proffesor" to "professor", "profesional" to "professional",
        "sucess" to "success", "sucessful" to "successful", "begining" to "beginning",
        "comming" to "coming", "runing" to "running", "planing" to "planning",
        "stoped" to "stopped", "droped" to "dropped", "hopefuly" to "hopefully",
        "realy" to "really", "finaly" to "finally", "usally" to "usually",
        "actualy" to "actually", "basicly" to "basically", "probly" to "probably",
        "probaly" to "probably", "maby" to "maybe", "alway" to "always",
        "allways" to "always", "alright" to "alright", "alot" to "a lot",
        "dont" to "don't", "cant" to "can't", "wont" to "won't", "didnt" to "didn't",
        "doesnt" to "doesn't", "isnt" to "isn't", "arent" to "aren't", "wasnt" to "wasn't",
        "werent" to "weren't", "havent" to "haven't", "hasnt" to "hasn't", "hadnt" to "hadn't",
        "wouldnt" to "wouldn't", "couldnt" to "couldn't", "shouldnt" to "shouldn't",
        "im" to "I'm", "ive" to "I've", "ill" to "I'll", "id" to "I'd",
        "youre" to "you're", "theyre" to "they're", "thats" to "that's", "whats" to "what's",
        "theres" to "there's", "heres" to "here's", "lets" to "let's",
        "speel" to "spell", "speling" to "spelling", "sentance" to "sentence",
        "paragrah" to "paragraph", "queston" to "question", "answr" to "answer",
        "notebok" to "notebook", "notbook" to "notebook", "stiky" to "sticky",
        "sumary" to "summary", "chaptr" to "chapter", "lectur" to "lecture",
        "mathmatics" to "mathematics", "physic" to "physics", "chemisty" to "chemistry",
        "biolog" to "biology", "histroy" to "history", "geograpy" to "geography",
        "economcs" to "economics", "algoritm" to "algorithm", "equaton" to "equation",
        "funciton" to "function", "varible" to "variable", "theroem" to "theorem",
        "formlua" to "formula", "definion" to "definition", "explanaton" to "explanation",
        "experment" to "experiment", "concluson" to "conclusion", "hypotesis" to "hypothesis",
        "introducion" to "introduction", "refrence" to "reference", "asignmnet" to "assignment",
        "homwork" to "homework", "examinaton" to "examination", "presntation" to "presentation",
        "schdule" to "schedule", "remnder" to "reminder", "importnt" to "important",
        "togther" to "together", "betwen" to "between", "throgh" to "through",
        "thougth" to "thought", "tought" to "taught", "bcz" to "because",
        "plz" to "please", "pls" to "please", "thx" to "thanks",
        "wih" to "with", "wiht" to "with", "fo" to "of", "adn" to "and", "nad" to "and",
        "taht" to "that", "thta" to "that", "waht" to "what", "whta" to "what",
        "wher" to "where", "wehn" to "when", "hwere" to "where", "form" to "form",
        "fomr" to "from", "frmo" to "from", "haev" to "have", "hvae" to "have",
        "jsut" to "just", "juts" to "just", "knwo" to "know", "konw" to "know",
        "liek" to "like", "lkie" to "like", "mkae" to "make", "maek" to "make",
        "moer" to "more", "mroe" to "more", "onyl" to "only", "olny" to "only",
        "otehr" to "other", "ohter" to "other", "peopel" to "people", "poeple" to "people",
        "saerch" to "search", "serach" to "search", "shoudl" to "should", "shuold" to "should",
        "coudl" to "could", "cuold" to "could", "woudl" to "would", "wuold" to "would",
        "soem" to "some", "smoe" to "some", "thign" to "thing", "hting" to "thing",
        "thnik" to "think", "htink" to "think", "tiem" to "time", "tmei" to "time",
        "verry" to "very", "vrey" to "very", "wrok" to "work", "owrk" to "work",
        "yaer" to "year", "yera" to "year", "yuor" to "your", "yrou" to "your",
        "abotu" to "about", "aobut" to "about", "afetr" to "after", "agian" to "again",
        "alreayd" to "already", "anoter" to "another", "aroudn" to "around",
        "befoer" to "before", "beter" to "better", "chang" to "change", "chagne" to "change",
        "complt" to "complete", "complet" to "complete", "corret" to "correct",
        "creat" to "create", "custo" to "custom", "desgin" to "design",
        "detial" to "detail", "devlop" to "develop", "documnt" to "document",
        "enogh" to "enough", "evry" to "every", "evrything" to "everything",
        "firs" to "first", "floder" to "folder", "fodler" to "folder",
        "folow" to "follow", "genral" to "general", "graet" to "great",
        "gropu" to "group", "handwrting" to "handwriting", "handwritng" to "handwriting",
        "helful" to "helpful", "hwo" to "how", "infromation" to "information",
        "intrest" to "interest", "keybord" to "keyboard", "learing" to "learning",
        "letr" to "letter", "litle" to "little", "mesage" to "message",
        "minuts" to "minutes", "mornig" to "morning", "mush" to "much",
        "nmae" to "name", "numbr" to "number", "ofice" to "office",
        "opton" to "option", "ordr" to "order", "pgae" to "page",
        "papr" to "paper", "particlar" to "particular", "persn" to "person",
        "pictur" to "picture", "plce" to "place", "poitn" to "point",
        "posible" to "possible", "practce" to "practice", "preapre" to "prepare",
        "problm" to "problem", "progrss" to "progress", "projct" to "project",
        "quik" to "quick", "realy" to "really", "recongize" to "recognize",
        "recoginze" to "recognize", "rember" to "remember", "repot" to "report",
        "reslt" to "result", "reviw" to "review", "rgiht" to "right",
        "scool" to "school", "scince" to "science", "scren" to "screen",
        "secnd" to "second", "secton" to "section", "sensitvity" to "sensitivity",
        "setings" to "settings", "shrae" to "share", "shrot" to "short",
        "simpl" to "simple", "smal" to "small", "somthing" to "something",
        "speical" to "special", "stady" to "study", "strat" to "start",
        "stikcy" to "sticky", "stlye" to "style", "subjet" to "subject",
        "sysetm" to "system", "tabe" to "table", "taks" to "task",
        "techr" to "teacher", "templat" to "template", "tets" to "test",
        "tody" to "today", "tolbar" to "toolbar", "topci" to "topic",
        "tuch" to "touch", "undrstand" to "understand", "updat" to "update",
        "usful" to "useful", "vlaue" to "value", "versin" to "version",
        "vidoe" to "video", "wekly" to "weekly", "wihout" to "without",
        "wrld" to "world", "wrod" to "word", "wrte" to "write",
    )

    private val coreDictionary: Set<String> = (
        directFixes.values + listOf(
            "the", "be", "to", "of", "and", "a", "in", "that", "have", "i", "it", "for", "not", "on", "with", "he",
            "as", "you", "do", "at", "this", "but", "his", "by", "from", "they", "we", "say", "her", "she", "or",
            "an", "will", "my", "one", "all", "would", "there", "their", "what", "so", "up", "out", "if", "about",
            "who", "get", "which", "go", "me", "when", "make", "can", "like", "time", "no", "just", "him", "know",
            "take", "people", "into", "year", "your", "good", "some", "could", "them", "see", "other", "than",
            "then", "now", "look", "only", "come", "its", "over", "think", "also", "back", "after", "use", "two",
            "how", "our", "work", "first", "well", "way", "even", "new", "want", "because", "any", "these", "give",
            "day", "most", "us", "note", "notes", "page", "pages", "book", "read", "write", "draw", "pen", "ink",
            "crayon", "pencil", "eraser", "lasso", "shape", "shapes", "text", "font", "bold", "italic", "color",
            "highlight", "folder", "folders", "file", "files", "share", "chat", "friend", "friends", "study",
            "exam", "test", "quiz", "math", "algebra", "calculus", "geometry", "physics", "chemistry", "biology",
            "history", "english", "science", "computer", "code", "data", "graph", "chart", "diagram", "matrix",
            "vector", "energy", "force", "mass", "velocity", "acceleration", "gravity", "atom", "molecule", "cell",
            "dna", "protein", "enzyme", "acid", "base", "reaction", "solution", "theory", "proof", "lemma",
            "chapter", "unit", "lesson", "class", "course", "homework", "assignment", "project", "deadline",
            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday", "january", "february",
            "march", "april", "may", "june", "july", "august", "september", "october", "november", "december",
            "today", "tomorrow", "yesterday", "morning", "afternoon", "evening", "night", "week", "month", "daily",
            "weekly", "monthly", "todo", "done", "check", "list", "plan", "goal", "goals", "habit", "habits",
            "meeting", "agenda", "summary", "review", "outline", "concept", "mind", "map", "sticky", "template",
            "search", "find", "looking", "settings", "custom", "style", "theme", "light", "dark", "blur",
            "hello", "world", "welcome", "please", "thank", "thanks", "sorry", "great", "awesome", "important",
            "remember", "check", "update", "version", "release", "feature", "button", "screen", "tool", "tools",
            "pressure", "sensitivity", "touch", "stylus", "keyboard", "typing", "spelling", "auto", "align", "guide",
        )
    ).map { it.lowercase() }.toSet()

    /** Auto-corrects obvious typos in [text] while preserving punctuation, newlines, and capitalization. */
    fun autoCorrect(text: String): String {
        if (text.isBlank()) return text
        val tokenRegex = Regex("[A-Za-z']+")
        return tokenRegex.replace(text) { match ->
            val raw = match.value
            correctWord(raw)
        }
    }

    /** Returns up to [maxSuggestions] candidate corrections for a single misspelled word (empty if valid). */
    fun suggestions(word: String, maxSuggestions: Int = 3): List<String> {
        val lower = word.lowercase()
        if (lower.length < 3 || lower in coreDictionary) return emptyList()
        directFixes[lower]?.let { return listOf(matchCase(word, it)) }
        val candidates = ArrayList<Pair<String, Int>>()
        for (dictWord in coreDictionary) {
            if (abs(dictWord.length - lower.length) > 1) continue
            if (dictWord.firstOrNull() != lower.firstOrNull()) continue
            val d = editDistance(lower, dictWord)
            if (d == 1) candidates += dictWord to d
        }
        return candidates
            .sortedBy { it.second }
            .map { matchCase(word, it.first) }
            .distinct()
            .take(maxSuggestions)
    }

    fun correctWord(raw: String): String {
        if (raw.length < 2) return raw
        val lower = raw.lowercase()
        if (lower in coreDictionary) return raw
        directFixes[lower]?.let { return matchCase(raw, it) }
        // Only apply edit-distance-1 auto-fix for words >= 4 chars where there is a single unambiguous match
        if (lower.length >= 4) {
            var best: String? = null
            var hits = 0
            for (dictWord in coreDictionary) {
                if (abs(dictWord.length - lower.length) > 1) continue
                if (dictWord.firstOrNull() != lower.firstOrNull()) continue
                if (editDistance(lower, dictWord) == 1) {
                    best = dictWord
                    hits++
                    if (hits > 1) return raw
                }
            }
            if (hits == 1 && best != null) return matchCase(raw, best)
        }
        return raw
    }

    private fun matchCase(template: String, replacement: String): String {
        if (replacement.contains('\'') && template.lowercase() in setOf("im", "ive", "ill", "id")) {
            return replacement
        }
        return when {
            template.all { it.isUpperCase() } && template.length > 1 -> replacement.uppercase()
            template.firstOrNull()?.isUpperCase() == true -> replacement.replaceFirstChar { it.uppercase() }
            else -> replacement
        }
    }

    fun editDistance(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        if (abs(m - n) > 2) return 3
        var prev = IntArray(n + 1) { it }
        var cur = IntArray(n + 1)
        for (i in 1..m) {
            cur[0] = i
            val ca = a[i - 1]
            for (j in 1..n) {
                val cost = if (ca == b[j - 1]) 0 else 1
                var best = min(min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost)
                // Damerau transposition (e.g. "teh" -> "the", "fomr" -> "from")
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    best = min(best, prev[j - 2] + 1)
                }
                cur[j] = best
            }
            val tmp = prev; prev = cur; cur = tmp
        }
        return prev[n]
    }
}
