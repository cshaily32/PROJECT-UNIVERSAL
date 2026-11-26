package sgyt.pu.core.json

/**
 * Small dependency-free JSON implementation used by PU's on-device persistence.
 *
 * The Android runtime already ships JSON APIs, but keeping the parser in shared
 * Kotlin makes catalog, cache, and backup logic testable on a plain JVM and
 * avoids pulling another package into the small base APK.
 */
sealed interface PuJsonValue {
    data class JsonObject(val values: Map<String, PuJsonValue>) : PuJsonValue
    data class JsonArray(val values: List<PuJsonValue>) : PuJsonValue
    data class JsonString(val value: String) : PuJsonValue
    data class JsonNumber(val value: Double) : PuJsonValue
    data class JsonBoolean(val value: Boolean) : PuJsonValue
    data object JsonNull : PuJsonValue
}

class PuJsonObject(values: Map<String, PuJsonValue>) {
    val values: Map<String, PuJsonValue> = values.toMap()

    operator fun get(key: String): PuJsonValue? = values[key]
    fun string(key: String): String? = (values[key] as? PuJsonValue.JsonString)?.value
    fun long(key: String): Long? = (values[key] as? PuJsonValue.JsonNumber)?.value?.toLong()
    fun int(key: String): Int? = long(key)?.toInt()
    fun double(key: String): Double? = (values[key] as? PuJsonValue.JsonNumber)?.value
    fun boolean(key: String): Boolean? = (values[key] as? PuJsonValue.JsonBoolean)?.value
    fun array(key: String): List<PuJsonValue> = (values[key] as? PuJsonValue.JsonArray)?.values.orEmpty()
    fun jsonObject(key: String): PuJsonObject? = (values[key] as? PuJsonValue.JsonObject)?.let { PuJsonObject(it.values) }
}

class PuJsonArray(values: List<PuJsonValue>) {
    val values: List<PuJsonValue> = values.toList()
}

object PuJson {
    fun parse(text: String): PuJsonValue = Parser(text).parseDocument()

    fun encode(value: PuJsonValue, pretty: Boolean = false): String = StringBuilder().apply {
        writeValue(this, value, pretty, 0)
    }.toString()

    fun obj(vararg pairs: Pair<String, PuJsonValue>): PuJsonValue.JsonObject =
        PuJsonValue.JsonObject(linkedMapOf(*pairs))

    fun arr(values: List<PuJsonValue>): PuJsonValue.JsonArray = PuJsonValue.JsonArray(values)
    fun string(value: String): PuJsonValue = PuJsonValue.JsonString(value)
    fun number(value: Long): PuJsonValue = PuJsonValue.JsonNumber(value.toDouble())
    fun number(value: Int): PuJsonValue = PuJsonValue.JsonNumber(value.toDouble())
    fun number(value: Double): PuJsonValue = PuJsonValue.JsonNumber(value)
    fun bool(value: Boolean): PuJsonValue = PuJsonValue.JsonBoolean(value)
    fun nullableString(value: String?): PuJsonValue = value?.let(::string) ?: PuJsonValue.JsonNull
    fun nullableNumber(value: Long?): PuJsonValue = value?.let { number(it) } ?: PuJsonValue.JsonNull

    private fun writeValue(out: StringBuilder, value: PuJsonValue, pretty: Boolean, depth: Int) {
        when (value) {
            is PuJsonValue.JsonObject -> {
                out.append('{')
                val entries = value.values.entries.toList()
                entries.forEachIndexed { index, (key, child) ->
                    if (index > 0) out.append(',')
                    if (pretty) newlineAndIndent(out, depth + 1)
                    writeString(out, key)
                    out.append(':')
                    if (pretty) out.append(' ')
                    writeValue(out, child, pretty, depth + 1)
                }
                if (entries.isNotEmpty() && pretty) newlineAndIndent(out, depth)
                out.append('}')
            }

            is PuJsonValue.JsonArray -> {
                out.append('[')
                value.values.forEachIndexed { index, child ->
                    if (index > 0) out.append(',')
                    if (pretty) newlineAndIndent(out, depth + 1)
                    writeValue(out, child, pretty, depth + 1)
                }
                if (value.values.isNotEmpty() && pretty) newlineAndIndent(out, depth)
                out.append(']')
            }

            is PuJsonValue.JsonString -> writeString(out, value.value)
            is PuJsonValue.JsonNumber -> out.append(formatNumber(value.value))
            is PuJsonValue.JsonBoolean -> out.append(if (value.value) "true" else "false")
            PuJsonValue.JsonNull -> out.append("null")
        }
    }

    private fun formatNumber(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "0"
        return if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
    }

    private fun newlineAndIndent(out: StringBuilder, depth: Int) {
        out.append('\n')
        repeat(depth) { out.append("  ") }
    }

    private fun writeString(out: StringBuilder, value: String) {
        out.append('"')
        value.forEach { character ->
            when (character) {
                '"' -> out.append("\\\"")
                '\\' -> out.append("\\\\")
                '\b' -> out.append("\\b")
                '\u000C' -> out.append("\\f")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                else -> {
                    if (character.code < 0x20) {
                        out.append("\\u")
                        out.append(character.code.toString(16).padStart(4, '0'))
                    } else {
                        out.append(character)
                    }
                }
            }
        }
        out.append('"')
    }

    private class Parser(private val source: String) {
        private var index = 0

        fun parseDocument(): PuJsonValue {
            skipWhitespace()
            val value = parseValue()
            skipWhitespace()
            require(index == source.length) { "Unexpected JSON content at offset $index" }
            return value
        }

        private fun parseValue(): PuJsonValue {
            skipWhitespace()
            require(index < source.length) { "Unexpected end of JSON" }
            return when (source[index]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> PuJsonValue.JsonString(parseString())
                't' -> parseLiteral("true", PuJsonValue.JsonBoolean(true))
                'f' -> parseLiteral("false", PuJsonValue.JsonBoolean(false))
                'n' -> parseLiteral("null", PuJsonValue.JsonNull)
                '-', in '0'..'9' -> parseNumber()
                else -> error("Unexpected JSON character '${source[index]}' at offset $index")
            }
        }

        private fun parseObject(): PuJsonValue.JsonObject {
            expect('{')
            val values = linkedMapOf<String, PuJsonValue>()
            skipWhitespace()
            if (consume('}')) return PuJsonValue.JsonObject(values)
            while (true) {
                skipWhitespace()
                require(index < source.length && source[index] == '"') {
                    "Expected JSON object key at offset $index"
                }
                val key = parseString()
                skipWhitespace()
                expect(':')
                values[key] = parseValue()
                skipWhitespace()
                if (consume('}')) break
                expect(',')
            }
            return PuJsonValue.JsonObject(values)
        }

        private fun parseArray(): PuJsonValue.JsonArray {
            expect('[')
            val values = mutableListOf<PuJsonValue>()
            skipWhitespace()
            if (consume(']')) return PuJsonValue.JsonArray(values)
            while (true) {
                values += parseValue()
                skipWhitespace()
                if (consume(']')) break
                expect(',')
            }
            return PuJsonValue.JsonArray(values)
        }

        private fun parseString(): String {
            expect('"')
            val result = StringBuilder()
            while (index < source.length) {
                when (val character = source[index++]) {
                    '"' -> return result.toString()
                    '\\' -> {
                        require(index < source.length) { "Unterminated JSON escape" }
                        when (val escaped = source[index++]) {
                            '"' -> result.append('"')
                            '\\' -> result.append('\\')
                            '/' -> result.append('/')
                            'b' -> result.append('\b')
                            'f' -> result.append('\u000C')
                            'n' -> result.append('\n')
                            'r' -> result.append('\r')
                            't' -> result.append('\t')
                            'u' -> {
                                require(index + 4 <= source.length) { "Invalid unicode escape" }
                                val code = source.substring(index, index + 4).toIntOrNull(16)
                                    ?: error("Invalid unicode escape")
                                result.append(code.toChar())
                                index += 4
                            }

                            else -> error("Unsupported JSON escape '\\$escaped'")
                        }
                    }

                    else -> result.append(character)
                }
            }
            error("Unterminated JSON string")
        }

        private fun parseNumber(): PuJsonValue.JsonNumber {
            val start = index
            if (source[index] == '-') index++
            require(index < source.length && source[index].isDigit()) { "Invalid JSON number" }
            while (index < source.length && source[index].isDigit()) index++
            if (index < source.length && source[index] == '.') {
                index++
                require(index < source.length && source[index].isDigit()) { "Invalid JSON fraction" }
                while (index < source.length && source[index].isDigit()) index++
            }
            if (index < source.length && (source[index] == 'e' || source[index] == 'E')) {
                index++
                if (index < source.length && (source[index] == '+' || source[index] == '-')) index++
                require(index < source.length && source[index].isDigit()) { "Invalid JSON exponent" }
                while (index < source.length && source[index].isDigit()) index++
            }
            val number = source.substring(start, index).toDoubleOrNull() ?: error("Invalid JSON number")
            return PuJsonValue.JsonNumber(number)
        }

        private fun parseLiteral(literal: String, value: PuJsonValue): PuJsonValue {
            require(source.regionMatches(index, literal, 0, literal.length)) {
                "Invalid JSON literal at offset $index"
            }
            index += literal.length
            return value
        }

        private fun skipWhitespace() {
            while (index < source.length && source[index].isWhitespace()) index++
        }

        private fun consume(character: Char): Boolean {
            if (index < source.length && source[index] == character) {
                index++
                return true
            }
            return false
        }

        private fun expect(character: Char) {
            require(consume(character)) { "Expected '$character' at offset $index" }
        }
    }
}
