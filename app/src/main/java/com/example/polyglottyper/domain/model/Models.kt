package com.example.polyglottyper.domain.model

/**
 * A practice language. [specialChars] are the accented characters the language
 * needs beyond the base layout rows; the practice keyboard appends any of them
 * that the selected layout does not already carry on its keys.
 */
enum class Language(
    val code: String,
    val displayName: String,
    val flagEmoji: String,
    val specialChars: List<Char>,
) {
    ENGLISH("en", "English", "🇬🇧", emptyList()),
    SPANISH("es", "Español", "🇪🇸", "áéíóúñü".toList()),
    FRENCH("fr", "Français", "🇫🇷", "'àâçéèêëîïôùû".toList()),
    GERMAN("de", "Deutsch", "🇩🇪", "äöüß".toList()),
    ITALIAN("it", "Italiano", "🇮🇹", "àèéìòù".toList()),
    PORTUGUESE("pt", "Português", "🇵🇹", "áàâãçéêíóôõú".toList()),
    TURKISH("tr", "Türkçe", "🇹🇷", "çğışöü".toList());

    companion object {
        fun fromCode(code: String): Language =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

enum class KeyboardCategory { STANDARD, ERGONOMIC, LEARNING }

/**
 * A keyboard layout. [rows] are the three letter rows rendered by the practice
 * keyboard; adding an entry here automatically registers it in the picker,
 * the stats screen, and the practice keyboard.
 */
enum class KeyboardLayout(
    val id: String,
    val displayName: String,
    val category: KeyboardCategory,
    val rows: List<List<Char>>,
) {
    QWERTY("qwerty", "QWERTY", KeyboardCategory.STANDARD, listOf(
        "qwertyuiop".toList(),
        "asdfghjkl".toList(),
        "zxcvbnm".toList(),
    )),
    AZERTY("azerty", "AZERTY", KeyboardCategory.STANDARD, listOf(
        "azertyuiop".toList(),
        "qsdfghjklm".toList(),
        "wxcvbn".toList(),
    )),
    QWERTZ("qwertz", "QWERTZ", KeyboardCategory.STANDARD, listOf(
        "qwertzuiop".toList(),
        "asdfghjkl".toList(),
        "yxcvbnm".toList(),
    )),
    DVORAK("dvorak", "Dvorak", KeyboardCategory.ERGONOMIC, listOf(
        "',.pyfgcrl".toList(),
        "aoeuidhtns".toList(),
        ";qjkxbmwvz".toList(),
    )),
    COLEMAK("colemak", "Colemak", KeyboardCategory.ERGONOMIC, listOf(
        "qwfpgjluy;".toList(),
        "arstdhneio".toList(),
        "zxcvbkm".toList(),
    )),
    BEPO("bepo", "BÉPO", KeyboardCategory.ERGONOMIC, listOf(
        "bépoèvdljzw".toList(),
        "auiectsrnmç".toList(),
        "àyxkqghf".toList(),
    )),
    NEO("neo", "Neo", KeyboardCategory.ERGONOMIC, listOf(
        "xvlcwkhgfqß".toList(),
        "uiaeosnrtdy".toList(),
        "üöäpzbmj".toList(),
    )),
    TURKISH_F("turkish_f", "Turkish F", KeyboardCategory.ERGONOMIC, listOf(
        "fgğiodrnhp".toList(),
        "uieaütkmlyş".toList(),
        "jövcçzsb".toList(),
    )),
    ABC("abc", "ABC (Alphabetical)", KeyboardCategory.LEARNING, listOf(
        "abcdefghij".toList(),
        "klmnopqrs".toList(),
        "tuvwxyz".toList(),
    ));

    companion object {
        fun fromId(id: String): KeyboardLayout =
            entries.firstOrNull { it.id == id } ?: QWERTY

        /** Traditional layout associated with each language ("Automatic" mode). */
        fun recommendedFor(language: Language): KeyboardLayout = when (language) {
            Language.FRENCH -> AZERTY
            Language.GERMAN -> QWERTZ
            Language.TURKISH -> TURKISH_F
            else -> QWERTY
        }
    }
}

sealed interface GameMode {
    val id: String
    val label: String

    data class Timed(val seconds: Int) : GameMode {
        override val id: String get() = "TIME_$seconds"
        override val label: String get() = "${seconds}s"
    }

    data class WordCount(val words: Int) : GameMode {
        override val id: String get() = "WORDS_$words"
        override val label: String get() = "$words words"
    }

    /** 90s run — a single wrong word ends the game. */
    data class SuddenDeath(val seconds: Int = 90) : GameMode {
        override val id: String get() = "SUDDEN_$seconds"
        override val label: String get() = "Sudden death"
    }

    /** Endless, no timer — the word list tops itself up; ends via Finish. */
    data object Zen : GameMode {
        override val id: String get() = "ZEN"
        override val label: String get() = "Zen"
    }

    /** Seeded daily run — identical words for everyone on a given day. */
    data object DailyChallenge : GameMode {
        const val WORD_COUNT = 25
        override val id: String get() = "DAILY"
        override val label: String get() = "Daily"
    }

    /** Arcade falling-words game (own screen, same result pipeline). */
    data object WordRain : GameMode {
        override val id: String get() = "RAIN"
        override val label: String get() = "Word Rain"
    }

    companion object {
        /** Shown as chips on Home. */
        val ALL: List<GameMode> = listOf(
            Timed(30), Timed(60), WordCount(10), WordCount(25), SuddenDeath(90), Zen,
        )

        private val PARSEABLE = ALL + listOf(DailyChallenge, WordRain)

        fun fromId(id: String): GameMode =
            PARSEABLE.firstOrNull { it.id == id } ?: Timed(60)
    }
}

data class TypingResult(
    val id: Long = 0L,                                  // Room PK — stable list key
    val language: Language,
    val layout: KeyboardLayout = KeyboardLayout.QWERTY, // recorded on every game
    val mode: GameMode,
    val wpm: Int,
    val accuracy: Int,
    val correctWords: Int,
    val incorrectWords: Int,
    val durationSeconds: Int,
    val timestamp: Long = System.currentTimeMillis(),
)

data class LanguageStats(
    val languageCode: String,
    val bestWpm: Int,
    val avgAccuracy: Int,
    val gamesPlayed: Int,
)

data class LayoutStats(
    val layoutId: String,
    val bestWpm: Int,
    val avgAccuracy: Int,
    val gamesPlayed: Int,
)

/** One point of the WPM history sparkline, ordered by time. */
data class WpmPoint(val wpm: Int, val timestamp: Long)
