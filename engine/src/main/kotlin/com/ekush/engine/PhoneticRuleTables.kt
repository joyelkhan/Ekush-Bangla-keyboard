/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.ekush.engine

/**
 * Vowel mapping holding standalone vowel glyph and combining kar sign.
 */
data class VowelRule(
    val standalone: String,
    val kar: String
)

/**
 * Immutable phonetic rule tables loaded from versioned JSON (`rules/bn.json`, Blueprint Part 04.10).
 */
data class RuleTables(
    val consonants: Map<String, String>,
    val vowels: Map<String, VowelRule>,
    val symbols: Map<String, String>,
    val canonicalWordOverrides: Map<String, String>
) {
    val allKeysDescending: List<String> =
        (consonants.keys + vowels.keys + symbols.keys)
            .distinct()
            .sortedByDescending { it.length }

    fun isConsonantUnit(unit: String): Boolean = consonants.containsKey(unit)

    fun isVowelUnit(unit: String): Boolean = vowels.containsKey(unit)

    fun consonantOf(unit: String): String = consonants[unit] ?: unit

    fun standaloneVowelOf(unit: String): String = vowels[unit]?.standalone ?: unit

    fun karOf(unit: String): String = vowels[unit]?.kar ?: ""

    fun symbolOf(unit: String): String = symbols[unit] ?: unit

    companion object {
        /**
         * Loads rule tables from `rules/bn.json` on the classpath, falling back to the embedded
         * defaults if resources are stripped.
         */
        fun loadDefault(): RuleTables {
            val stream = RuleTables::class.java.classLoader?.getResourceAsStream("rules/bn.json")
            if (stream != null) {
                val jsonText = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                return fromJson(jsonText)
            }
            return embeddedFallback()
        }

        fun fromJson(json: String): RuleTables {
            val root = SimpleJsonParser(json).parseObject()
            val consObj = root["consonants"] as? Map<*, *> ?: emptyMap<String, Any>()
            val vowelsObj = root["vowels"] as? Map<*, *> ?: emptyMap<String, Any>()
            val symbolsObj = root["symbols"] as? Map<*, *> ?: emptyMap<String, Any>()
            val overridesObj = root["canonicalWordOverrides"] as? Map<*, *> ?: emptyMap<String, Any>()

            val consonants = consObj.entries.associate { it.key.toString() to it.value.toString() }
            val vowels = vowelsObj.entries.associate { (k, v) ->
                val map = v as Map<*, *>
                k.toString() to VowelRule(
                    standalone = map["standalone"].toString(),
                    kar = map["kar"].toString()
                )
            }
            val symbols = symbolsObj.entries.associate { it.key.toString() to it.value.toString() }
            val overrides = overridesObj.entries
                .filter { !it.key.toString().startsWith("_") }
                .associate { it.key.toString() to it.value.toString() }

            return RuleTables(
                consonants = consonants,
                vowels = vowels,
                symbols = symbols,
                canonicalWordOverrides = overrides
            )
        }

        fun embeddedFallback(): RuleTables = RuleTables(
            consonants = linkedMapOf(
                "chh" to "ছ",
                "shh" to "ষ",
                "ksh" to "ক্ষ",
                "kh" to "খ",
                "gh" to "ঘ",
                "ng" to "ং",
                "ch" to "চ",
                "jh" to "ঝ",
                "Th" to "ঠ",
                "Dh" to "ঢ",
                "th" to "থ",
                "dh" to "ধ",
                "ph" to "ফ",
                "bh" to "ভ",
                "sh" to "শ",
                "Rh" to "ঢ়",
                "k" to "ক",
                "g" to "গ",
                "c" to "চ",
                "j" to "জ",
                "z" to "জ",
                "T" to "ট",
                "D" to "ড",
                "N" to "ণ",
                "t" to "ত",
                "d" to "দ",
                "n" to "ন",
                "p" to "প",
                "f" to "ফ",
                "b" to "ব",
                "v" to "ভ",
                "m" to "ম",
                "r" to "র",
                "l" to "ল",
                "S" to "ষ",
                "s" to "স",
                "h" to "হ",
                "R" to "ড়",
                "Y" to "য়",
                "y" to "য",
                "w" to "ব"
            ),
            vowels = linkedMapOf(
                "rri" to VowelRule("ঋ", "ৃ"),
                "ee" to VowelRule("ঈ", "ী"),
                "ii" to VowelRule("ঈ", "ী"),
                "oo" to VowelRule("ঊ", "ূ"),
                "uu" to VowelRule("ঊ", "ূ"),
                "oi" to VowelRule("ঐ", "ৈ"),
                "ou" to VowelRule("ঔ", "ৌ"),
                "aa" to VowelRule("আ", "া"),
                "a" to VowelRule("অ", "া"),
                "i" to VowelRule("ই", "ি"),
                "I" to VowelRule("ঈ", "ী"),
                "u" to VowelRule("উ", "ু"),
                "U" to VowelRule("ঊ", "ূ"),
                "e" to VowelRule("এ", "ে"),
                "E" to VowelRule("এ", "ে"),
                "o" to VowelRule("ও", "ো"),
                "O" to VowelRule("ও", "ো")
            ),
            symbols = linkedMapOf(
                ":" to "ঃ",
                "^" to "ঁ",
                "0" to "০",
                "1" to "১",
                "2" to "২",
                "3" to "৩",
                "4" to "৪",
                "5" to "৫",
                "6" to "৬",
                "7" to "৭",
                "8" to "৮",
                "9" to "৯"
            ),
            canonicalWordOverrides = linkedMapOf(
                "bishwabidyalay" to "বিশ্ববিদ্যালয়",
                "kobita" to "কবিতা",
                "ganga" to "গঙ্গা",
                "phool" to "ফুল",
                "amar" to "আমার",
                "sonar" to "সোনার",
                "ami" to "আমি",
                "banglay" to "বাংলায়",
                "matro" to "মাত্র",
                "minite" to "মিনিটে",
                "Taip" to "টাইপ",
                "korte" to "করতে",
                "pari" to "পারি",
                "ekush" to "একুশ",
                "matribhashay" to "মাতৃভাষায়",
                "gorbe" to "গর্বে"
            )
        )
    }
}

/**
 * Minimal zero-dependency JSON parser for pure-Kotlin :engine data files.
 */
internal class SimpleJsonParser(private val src: String) {
    private var pos = 0

    fun parseObject(): Map<String, Any> {
        skipWhitespace()
        expect('{')
        val result = linkedMapOf<String, Any>()
        skipWhitespace()
        if (peek() == '}') {
            pos++
            return result
        }
        while (pos < src.length) {
            skipWhitespace()
            val key = parseString()
            skipWhitespace()
            expect(':')
            skipWhitespace()
            val value = parseValue()
            result[key] = value
            skipWhitespace()
            if (peek() == ',') {
                pos++
            } else if (peek() == '}') {
                pos++
                break
            } else {
                throw IllegalArgumentException("Unexpected char '${peek()}' at $pos")
            }
        }
        return result
    }

    private fun parseValue(): Any {
        skipWhitespace()
        return when (val ch = peek()) {
            '{' -> parseObject()
            '"' -> parseString()
            't', 'f' -> parseBoolean()
            in '0'..'9', '-' -> parseNumber()
            else -> throw IllegalArgumentException("Unexpected JSON value char '$ch' at $pos")
        }
    }

    private fun parseString(): String {
        expect('"')
        val sb = StringBuilder()
        while (pos < src.length) {
            val ch = src[pos++]
            when (ch) {
                '"' -> return sb.toString()
                '\\' -> {
                    val esc = src[pos++]
                    when (esc) {
                        '"', '\\', '/' -> sb.append(esc)
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            val hex = src.substring(pos, pos + 4)
                            sb.append(hex.toInt(16).toChar())
                            pos += 4
                        }
                        else -> sb.append(esc)
                    }
                }
                else -> sb.append(ch)
            }
        }
        throw IllegalArgumentException("Unterminated string")
    }

    private fun parseBoolean(): Boolean {
        return if (src.startsWith("true", pos)) {
            pos += 4
            true
        } else {
            pos += 5
            false
        }
    }

    private fun parseNumber(): Number {
        val start = pos
        while (pos < src.length && (src[pos] in '0'..'9' || src[pos] == '.' || src[pos] == '-')) {
            pos++
        }
        val numStr = src.substring(start, pos)
        return if (numStr.contains('.')) numStr.toDouble() else numStr.toInt()
    }

    private fun skipWhitespace() {
        while (pos < src.length && src[pos].isWhitespace()) pos++
    }

    private fun expect(c: Char) {
        if (pos >= src.length || src[pos] != c) {
            throw IllegalArgumentException("Expected '$c' at $pos")
        }
        pos++
    }

    private fun peek(): Char = if (pos < src.length) src[pos] else '\u0000'
}
