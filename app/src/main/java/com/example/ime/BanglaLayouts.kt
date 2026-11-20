/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.example.ime

import com.example.data.prefs.TypingMode

enum class KeyRole {
    CHARACTER,
    FUNCTION,
    HASANTA,
    SPACE,
    BACKSPACE,
    ENTER,
    SHIFT,
    MODE_SWITCH,
    SYMBOLS
}

data class KeySpec(
    val id: String,
    val label: String,
    val output: String = label,
    val weight: Float = 1f,
    val role: KeyRole = KeyRole.CHARACTER,
    val hint: String? = null,
    val alternates: List<String> = emptyList(),
    val shiftLabel: String? = null,
    val shiftOutput: String? = null
)

/**
 * Keyboard row & key specifications matching Blueprint Part 05.2, 05.8, and Appendix 10.2.
 */
object BanglaLayouts {

    /**
     * Appendix 10.2 Fixed layout long-press alternates (top-frequency conjunct set).
     */
    val fixedAlternates: Map<String, List<String>> = mapOf(
        "ক" to listOf("ক্ষ"),
        "গ" to listOf("গ্র", "গ্ল"),
        "জ" to listOf("জ্জ", "জ্ঞ", "জ্ব"),
        "ট" to listOf("ট্ট"),
        "ড" to listOf("ড্ড"),
        "ত" to listOf("ত্ত", "ত্ন", "ত্ম", "ত্র"),
        "ন" to listOf("ন্দ", "ন্ধ", "ন্ন", "ন্ব", "ন্ম"),
        "প" to listOf("প্ট", "প্ত", "প্ন", "প্প", "প্র", "প্ল"),
        "ব" to listOf("ব্দ", "ব্ল"),
        "ম" to listOf("ম্ন", "ম্ব", "ম্প", "ম্র"),
        "য" to listOf("য্য"),
        "র" to listOf("র্য", "র্র"),
        "ল" to listOf("ল্ল", "ল্ক", "ল্গ"),
        "শ" to listOf("শ্ছ", "শ্ব", "শ্ল"),
        "স" to listOf("স্ত", "স্থ", "স্ব", "স্প", "স্ল"),
        "হ" to listOf("হ্ণ", "হ্ন", "হ্ব", "হ্ম", "হ্ল"),
        // Matra & vowel long-press shortcuts
        "া" to listOf("আ"),
        "ি" to listOf("ই"),
        "ী" to listOf("ঈ"),
        "ু" to listOf("উ"),
        "ূ" to listOf("ঊ"),
        "ে" to listOf("এ"),
        "ৈ" to listOf("ঐ"),
        "ো" to listOf("ও"),
        "ৌ" to listOf("ঔ"),
        "্" to listOf("্র", "্য", "্ব", "\u200D", "\u200C")
    )

    private fun bnKey(ch: String): KeySpec {
        val alts = fixedAlternates[ch].orEmpty()
        return KeySpec(
            id = "bn_$ch",
            label = ch,
            output = ch,
            alternates = alts,
            hint = alts.firstOrNull()
        )
    }

    /**
     * Blueprint Figure 5-1: Fixed Bangla Layout, Page 1 (primary frequency-ordered rows).
     */
    fun fixedPage1(modeLabel: String): List<List<KeySpec>> = listOf(
        listOf(
            KeySpec("mode", modeLabel, role = KeyRole.MODE_SWITCH, weight = 1.25f),
            bnKey("ক"), bnKey("খ"), bnKey("গ"), bnKey("ঘ"), bnKey("ঙ"),
            bnKey("চ"), bnKey("ছ"), bnKey("জ"), bnKey("ঝ"),
            KeySpec("backspace", "⌫", role = KeyRole.BACKSPACE, weight = 1.25f)
        ),
        listOf(
            KeySpec("tab", "↹", output = "\t", role = KeyRole.FUNCTION, weight = 1.15f),
            bnKey("ট"), bnKey("ঠ"), bnKey("ড"), bnKey("ঢ"), bnKey("ণ"),
            bnKey("ত"), bnKey("থ"), bnKey("দ"), bnKey("ধ"),
            KeySpec("enter", "↵", role = KeyRole.ENTER, weight = 1.35f)
        ),
        listOf(
            KeySpec("shift", "⇧", role = KeyRole.SHIFT, weight = 1.15f),
            bnKey("প"), bnKey("ফ"), bnKey("ব"), bnKey("ভ"), bnKey("ম"),
            bnKey("য"), bnKey("র"), bnKey("ল"), bnKey("শ"), bnKey("ষ")
        ),
        listOf(
            bnKey("া"), bnKey("ি"), bnKey("ী"), bnKey("ু"), bnKey("ূ"),
            bnKey("ে"), bnKey("ৈ"), bnKey("ো"), bnKey("ৌ"),
            KeySpec(
                id = "hasanta",
                label = "্",
                output = "\u09CD",
                role = KeyRole.HASANTA,
                alternates = fixedAlternates["্"].orEmpty(),
                hint = "্য"
            )
        ),
        listOf(
            KeySpec("symbols", "১২৩", role = KeyRole.SYMBOLS, weight = 1.2f),
            bnKey("স"), bnKey("হ"), bnKey("ড়"), bnKey("ঢ়"), bnKey("য়"),
            bnKey("ৎ"), bnKey("ং"), bnKey("ঃ"), bnKey("ঁ"),
            KeySpec("space", "␣", output = " ", role = KeyRole.SPACE, weight = 1.5f, alternates = listOf("।", "."))
        )
    )

    /**
     * Blueprint Figure 5-2: Fixed Bangla Layout, Page 2 (Shift layer).
     * Resolves Figure 5-2 caption's note on duplicate '২' by placing all 10 Bangla digits (০-৯) cleanly in Row 3.
     */
    fun fixedPage2(modeLabel: String): List<List<KeySpec>> = listOf(
        listOf(
            KeySpec("mode", modeLabel, role = KeyRole.MODE_SWITCH, weight = 1.25f),
            bnKey("অ"), bnKey("ই"), bnKey("ঈ"), bnKey("উ"), bnKey("ঊ"),
            bnKey("ঋ"), bnKey("এ"), bnKey("ঐ"), bnKey("ও"), bnKey("ঔ"),
            KeySpec("backspace", "⌫", role = KeyRole.BACKSPACE, weight = 1.25f)
        ),
        listOf(
            bnKey("আ"), bnKey("জ্ঞ"), bnKey("ত্র"), bnKey("ক্ষ"), bnKey("শ্র"),
            bnKey("ঞ"), bnKey("ণ"), bnKey("ৎ"), bnKey("ং"), bnKey("ঃ"),
            KeySpec("enter", "↵", role = KeyRole.ENTER, weight = 1.2f)
        ),
        listOf(
            KeySpec("shift", "⇧", role = KeyRole.SHIFT, weight = 1.15f),
            bnKey("০"), bnKey("১"), bnKey("২"), bnKey("৩"), bnKey("৪"),
            bnKey("৫"), bnKey("৬"), bnKey("৭"), bnKey("৮"), bnKey("৯")
        ),
        listOf(
            bnKey("।"), bnKey("॥"), bnKey("/"), bnKey(","), bnKey("-"),
            bnKey("!"), bnKey("?"), bnKey("("), bnKey(")"),
            KeySpec("hasanta", "্", output = "\u09CD", role = KeyRole.HASANTA)
        ),
        listOf(
            KeySpec("symbols", "১২৩", role = KeyRole.SYMBOLS, weight = 1.2f),
            bnKey("৳"), bnKey(":"), bnKey(";"), bnKey("\""), bnKey("'"),
            KeySpec("space", "␣", output = " ", role = KeyRole.SPACE, weight = 2.4f),
            bnKey("="), bnKey("+"), bnKey(".")
        )
    )

    /**
     * Phonetic (Mode A) or English QWERTY (Mode C) with toggleable Bangla digits row on long-press
     * and explicit hasanta key (`\`) in Phonetic mode (Blueprint Part 03.2 & 03.8).
     */
    fun qwertyLayout(
        mode: TypingMode,
        shifted: Boolean,
        preferBanglaDigits: Boolean,
        forceLatinDigits: Boolean
    ): List<List<KeySpec>> {
        val isPhonetic = mode == TypingMode.PHONETIC
        val digitsRow = (0..9).map { idx ->
            val digitChar = ((idx + 1) % 10).toString()
            val bnDigit = ((0x09E6 + ((idx + 1) % 10)).toChar()).toString()
            val showBangla = isPhonetic && preferBanglaDigits && !forceLatinDigits
            KeySpec(
                id = "num_$digitChar",
                label = if (showBangla) bnDigit else digitChar,
                output = if (showBangla) bnDigit else digitChar,
                alternates = if (forceLatinDigits) listOf(digitChar) else listOf(bnDigit, digitChar),
                hint = if (showBangla) digitChar else if (!forceLatinDigits) bnDigit else null
            )
        }

        fun alpha(lower: String, upper: String, phoneticHint: String? = null): KeySpec {
            val label = if (shifted) upper else lower
            return KeySpec(
                id = "key_$lower",
                label = label,
                output = label,
                hint = if (isPhonetic) phoneticHint else null,
                alternates = if (lower != upper) listOf(if (shifted) lower else upper) else emptyList()
            )
        }

        val row1 = listOf(
            alpha("q", "Q"), alpha("w", "W", "্ব"), alpha("e", "E", "ে"),
            alpha("r", "R", "ড়"), alpha("t", "T", "ট"), alpha("y", "Y", "য়"),
            alpha("u", "U", "ু"), alpha("i", "I", "ি"), alpha("o", "O", "ো"),
            alpha("p", "P")
        )
        val row2 = listOf(
            alpha("a", "A", "া"), alpha("s", "S", "ষ"), alpha("d", "D", "ড"),
            alpha("f", "F"), alpha("g", "G"), alpha("h", "H"),
            alpha("j", "J"), alpha("k", "K"), alpha("l", "L")
        )
        val row3 = buildList {
            add(KeySpec("shift", "⇧", role = KeyRole.SHIFT, weight = 1.4f))
            add(alpha("z", "Z"))
            add(alpha("x", "X"))
            add(alpha("c", "C"))
            add(alpha("v", "V"))
            add(alpha("b", "B"))
            add(alpha("n", "N", "ণ"))
            add(alpha("m", "M"))
            if (isPhonetic) {
                // Explicit hasanta override slot (`\` / `]`) per Blueprint Part 03.2 #3
                add(
                    KeySpec(
                        id = "hasanta_escape",
                        label = "্",
                        output = "\\",
                        role = KeyRole.HASANTA,
                        hint = "\\",
                        alternates = listOf("\\", "]", "\u200D", "\u200C")
                    )
                )
            }
            add(KeySpec("backspace", "⌫", role = KeyRole.BACKSPACE, weight = 1.4f))
        }

        val modeButtonLabel = when (mode) {
            TypingMode.PHONETIC -> "Bn/En"
            TypingMode.FIXED_BANGLA -> "Bn/En"
            TypingMode.ENGLISH_QWERTY -> "En/Bn"
        }
        val spaceLabel = when (mode) {
            TypingMode.PHONETIC -> "একুশ · Phonetic"
            TypingMode.FIXED_BANGLA -> "একুশ · নির্ধারিত"
            TypingMode.ENGLISH_QWERTY -> "Ekush · English"
        }
        val periodOutput = if (isPhonetic) "।" else "."
        val periodHint = if (isPhonetic) "." else "।"

        val row4 = listOf(
            KeySpec("symbols", if (isPhonetic && preferBanglaDigits && !forceLatinDigits) "১২৩" else "123", role = KeyRole.SYMBOLS, weight = 1.25f),
            KeySpec("mode", modeButtonLabel, role = KeyRole.MODE_SWITCH, weight = 1.35f),
            KeySpec("comma", ",", output = ",", alternates = listOf(";", ":", "\"", "'")),
            KeySpec("space", spaceLabel, output = " ", role = KeyRole.SPACE, weight = 3.6f),
            KeySpec("period", periodOutput, output = periodOutput, hint = periodHint, alternates = listOf("।", "॥", ".", "?", "!")),
            KeySpec("enter", "↵", role = KeyRole.ENTER, weight = 1.5f)
        )

        return listOf(digitsRow, row1, row2, row3, row4)
    }

    /**
     * Number & Symbol (`123`) page honoring Bangla digit preference unless OTP/phone/money field forces Latin.
     */
    fun symbolsLayout(preferBanglaDigits: Boolean, forceLatinDigits: Boolean): List<List<KeySpec>> {
        val useBangla = preferBanglaDigits && !forceLatinDigits
        val digits = (0..9).map { idx ->
            val num = (idx + 1) % 10
            val latin = num.toString()
            val bn = ((0x09E6 + num).toChar()).toString()
            val active = if (useBangla) bn else latin
            KeySpec(
                id = "sym_num_$num",
                label = active,
                output = active,
                alternates = if (forceLatinDigits) listOf(latin) else listOf(bn, latin)
            )
        }
        val symRow1 = listOf("@", "#", "৳", "%", "&", "-", "+", "(", ")", "/").map {
            KeySpec("sym_$it", it, output = it)
        }
        val symRow2 = buildList {
            add(KeySpec("sym_danda", "।", output = "।", alternates = listOf("॥")))
            listOf("*", "\"", "'", ":", ";", "!", "?", "~").forEach {
                add(KeySpec("sym_$it", it, output = it))
            }
            add(KeySpec("backspace", "⌫", role = KeyRole.BACKSPACE, weight = 1.4f))
        }
        val symRow3 = listOf(
            KeySpec("symbols_back", "ABC", role = KeyRole.SYMBOLS, weight = 1.4f),
            KeySpec("mode", "Bn/En", role = KeyRole.MODE_SWITCH, weight = 1.3f),
            KeySpec("sym_comma", ",", output = ","),
            KeySpec("space", "␣", output = " ", role = KeyRole.SPACE, weight = 3.5f),
            KeySpec("sym_dot", ".", output = "."),
            KeySpec("enter", "↵", role = KeyRole.ENTER, weight = 1.5f)
        )
        return listOf(digits, symRow1, symRow2, symRow3)
    }
}
