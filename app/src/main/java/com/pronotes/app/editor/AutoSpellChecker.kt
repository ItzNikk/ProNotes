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

    private val multilingualFixes: Map<String, Map<String, String>> = mapOf(
        "es" to mapOf(
            "tambien" to "también", "mas" to "más", "despues" to "después", "asi" to "así",
            "aqui" to "aquí", "facil" to "fácil", "dificil" to "difícil", "rapido" to "rápido",
            "numero" to "número", "pagina" to "página", "capitulo" to "capítulo", "musica" to "música",
            "matematicas" to "matemáticas", "fisica" to "física", "quimica" to "química",
            "biologia" to "biología", "geografia" to "geografía", "economia" to "economía",
            "informacion" to "información", "educacion" to "educación", "solucion" to "solución",
            "ecuacion" to "ecuación", "funcion" to "función", "definicion" to "definición",
            "explicacion" to "explicación", "conclusion" to "conclusión", "introduccion" to "introducción",
            "practica" to "práctica", "examenes" to "exámenes", "metodo" to "método",
            "codigo" to "código", "grafico" to "gráfico", "teoria" to "teoría", "articulo" to "artículo",
            "miercoles" to "miércoles", "sabado" to "sábado", "dia" to "día", "dias" to "días",
            "porq" to "porque", "xq" to "porque", "habia" to "había", "podria" to "podría",
        ),
        "fr" to mapOf(
            "ca" to "ça", "deja" to "déjà", "tres" to "très", "apres" to "après", "etre" to "être",
            "meme" to "même", "tete" to "tête", "etude" to "étude", "etudier" to "étudier",
            "ecole" to "école", "eleve" to "élève", "lecon" to "leçon", "resume" to "résumé",
            "reponse" to "réponse", "numero" to "numéro", "mathematiques" to "mathématiques",
            "geographie" to "géographie", "economie" to "économie", "theorie" to "théorie",
            "methode" to "méthode", "systeme" to "système", "probleme" to "problème",
            "modele" to "modèle", "regle" to "règle", "idee" to "idée", "equipe" to "équipe",
            "general" to "général", "special" to "spécial", "francais" to "français",
            "beacoup" to "beaucoup", "toujour" to "toujours", "peut-etre" to "peut-être",
        ),
        "de" to mapOf(
            "uber" to "über", "fur" to "für", "konnen" to "können", "mussen" to "müssen",
            "mochten" to "möchten", "naturlich" to "natürlich", "losung" to "Lösung",
            "ubung" to "Übung", "prufung" to "Prüfung", "schuler" to "Schüler",
            "worter" to "Wörter", "bucher" to "Bücher", "moglich" to "möglich",
            "große" to "Größe", "groser" to "größer", "spater" to "später", "fruber" to "früher",
            "zusammenfasung" to "Zusammenfassung", "notizbuc" to "Notizbuch", "seiet" to "Seite",
            "wichtg" to "wichtig", "beispeil" to "Beispiel", "aufgab" to "Aufgabe",
        ),
        "pt" to mapOf(
            "nao" to "não", "entao" to "então", "tambem" to "também", "voce" to "você",
            "voces" to "vocês", "ja" to "já", "ate" to "até", "so" to "só", "facil" to "fácil",
            "dificil" to "difícil", "rapido" to "rápido", "numero" to "número", "pagina" to "página",
            "capitulo" to "capítulo", "informacao" to "informação", "educacao" to "educação",
            "solucao" to "solução", "equacao" to "equação", "funcao" to "função",
            "definicao" to "definição", "explicacao" to "explicação", "conclusao" to "conclusão",
            "introducao" to "introdução", "matematica" to "matemática", "fisica" to "física",
            "quimica" to "química", "historia" to "história", "geografia" to "geografia",
            "metodo" to "método", "codigo" to "código", "grafico" to "gráfico", "sabado" to "sábado",
        ),
        "it" to mapOf(
            "perche" to "perché", "poiche" to "poiché", "cosi" to "così", "piu" to "più",
            "puo" to "può", "gia" to "già", "citta" to "città", "universita" to "università",
            "attivita" to "attività", "qualita" to "qualità", "quantita" to "quantità",
            "possibilita" to "possibilità", "difficolta" to "difficoltà", "verita" to "verità",
            "lunedi" to "lunedì", "martedi" to "martedì", "mercoledi" to "mercoledì",
            "giovedi" to "giovedì", "venerdi" to "venerdì", "percio" to "perciò",
        ),
        "nl" to mapOf(
            "ideeen" to "ideeën", "bezigheden" to "bezigheden", "opdract" to "opdracht",
            "notite" to "notitie", "samenvating" to "samenvatting", "hoofdstk" to "hoofdstuk",
            "belangrjk" to "belangrijk", "voorbeel" to "voorbeeld", "mogelijkhied" to "mogelijkheid",
        ),
        "pl" to mapOf(
            "tez" to "też", "juz" to "już", "moze" to "może", "bedzie" to "będzie",
            "ktory" to "który", "ktora" to "która", "ktore" to "które", "wiecej" to "więcej",
            "bardz" to "bardzo", "strona" to "strona", "notaktka" to "notatka",
            "zadane" to "zadanie", "przyklad" to "przykład", "rozwiazanie" to "rozwiązanie",
        ),
        "tr" to mapOf(
            "cok" to "çok", "icin" to "için", "degil" to "değil", "simdi" to "şimdi",
            "boyle" to "böyle", "soyle" to "şöyle", "cunku" to "çünkü", "ogrenci" to "öğrenci",
            "ogretmen" to "öğretmen", "cozum" to "çözüm", "ornek" to "örnek", "calisma" to "çalışma",
            "turkce" to "Türkçe", "tesekkur" to "teşekkür", "gunaydin" to "günaydın",
        ),
        "id" to mapOf(
            "yg" to "yang", "dgn" to "dengan", "utk" to "untuk", "tdk" to "tidak",
            "krn" to "karena", "dlm" to "dalam", "sdh" to "sudah", "blm" to "belum",
            "catatan" to "catatan", "pelajarn" to "pelajaran", "ringksan" to "ringkasan",
        ),
        "ru" to mapOf(
            "привт" to "привет", "спасиб" to "спасибо", "сегодя" to "сегодня",
            "завтр" to "завтра", "конспет" to "конспект", "заметк" to "заметка",
            "страниц" to "страница", "задаче" to "задача", "решени" to "решение",
        ),
    )

    private val unicodeWordRegex = Regex("[\\p{L}\\p{M}']+")

    private fun baseLang(languageTag: String): String =
        languageTag.lowercase().substringBefore('-').ifBlank { "en" }

    private val mathSymbolChars = setOf('=', '+', '×', '÷', '±', '≠', '≤', '≥', '≈', '√', '^', '<', '>', 'π', 'Δ', 'Σ', 'θ', 'α', 'β')

    /**
     * Auto-corrects obvious typos in [text] for [languageTag] while preserving
     * punctuation, math expressions, numbers, newlines, capitalization, and non-Latin scripts untouched.
     */
    fun autoCorrect(text: String, languageTag: String = "en-US"): String {
        if (text.isBlank()) return text
        val lang = baseLang(languageTag)
        // Leave CJK, Devanagari, Arabic, Hebrew, Thai, and other scripts intact.
        if (lang != "en" && lang !in multilingualFixes) return text
        // Do not alter short equations or expressions containing math operators
        val looksLikeMath = text.any { it in mathSymbolChars }
        return unicodeWordRegex.replace(text) { match ->
            val start = match.range.first
            val end = match.range.last
            val prev = text.getOrNull(start - 1)
            val next = text.getOrNull(end + 1)
            val adjacentNumericOrMath = prev?.isDigit() == true || next?.isDigit() == true ||
                prev in mathSymbolChars || next in mathSymbolChars ||
                prev in setOf('$', '₹', '€', '£', '@', '#', '/', '\\', '_') ||
                next in setOf('(', '/', '\\', '_')
            if (adjacentNumericOrMath || (looksLikeMath && match.value.length <= 3)) {
                match.value
            } else {
                correctWord(match.value, lang)
            }
        }
    }

    /** Returns up to [maxSuggestions] candidate corrections for a single misspelled word (empty if valid). */
    fun suggestions(word: String, maxSuggestions: Int = 3, languageTag: String = "en-US"): List<String> {
        val lang = baseLang(languageTag)
        val lower = word.lowercase()
        if (lower.length < 2) return emptyList()
        if (lang != "en") {
            val langMap = multilingualFixes[lang] ?: return emptyList()
            langMap[lower]?.let { return listOf(matchCase(word, it)) }
            return emptyList()
        }
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

    fun correctWord(raw: String, languageTag: String = "en-US"): String {
        if (raw.length < 2) return raw
        val lang = baseLang(languageTag)
        val lower = raw.lowercase()
        if (lang != "en") {
            val langMap = multilingualFixes[lang] ?: return raw
            langMap[lower]?.let { return matchCase(raw, it) }
            return raw
        }
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
