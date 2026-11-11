package com.potatameister.barenotes

/**
 * Converts between the app's WYSIWYG body (literal list prefixes + span ranges)
 * and a small, portable Markdown subset.
 */
object MarkdownCodec {

    private const val BULLET = "• "
    private const val CHECK_OPEN = "☐ "
    private const val CHECK_DONE = "☑ "
    private const val QUOTE = "❝ "
    private val NUMBER_PREFIX = Regex("^(\\d+)\\.\\s+")
    private val MD_CHECK_OPEN = Regex("^- \\[ \\] ")
    private val MD_CHECK_DONE = Regex("^- \\[[xX]\\] ")

    private val SAFE_ID = Regex("[A-Za-z0-9_-]+")

    fun isSafeId(id: String): Boolean = id.isNotBlank() && SAFE_ID.matches(id)

    private val INLINE_TYPES = setOf(
        NoteSpan.BOLD,
        NoteSpan.ITALIC,
        NoteSpan.STRIKE
    )

    fun encode(note: Note): String {
        val body = bodyToMarkdown(note.body, note.spans)
        return buildString {
            append("---\n")
            append("title: ").append(quote(note.title)).append('\n')
            append("created: ").append(note.createdAt).append('\n')
            append("updated: ").append(note.updatedAt).append('\n')
            append("folder: ").append(quote(note.folderId)).append('\n')
            append("---\n")
            append(body)
        }
    }

    fun decode(id: String, raw: String): Note? {
        if (!isSafeId(id)) return null
        val normalized = raw.replace("\r\n", "\n").replace('\r', '\n')
        val lines = normalized.split('\n')
        val meta = mutableMapOf<String, String>()
        var body = normalized

        if (lines.firstOrNull() == "---") {
            val end = lines.drop(1).indexOfFirst { it == "---" }
            if (end >= 0) {
                val endIndex = end + 1
                lines.subList(1, endIndex).forEach { line ->
                    val colon = line.indexOf(':')
                    if (colon > 0) {
                        meta[line.substring(0, colon).trim()] =
                            line.substring(colon + 1).trim()
                    }
                }
                body = lines.drop(endIndex + 1).joinToString("\n")
            }
        }

        val now = System.currentTimeMillis()
        val parsed = bodyFromMarkdown(body)
        return Note(
            id = id,
            title = unquote(meta["title"].orEmpty()),
            body = parsed.first,
            createdAt = meta["created"]?.toLongOrNull() ?: now,
            updatedAt = meta["updated"]?.toLongOrNull() ?: now,
            spans = parsed.second,
            folderId = unquote(meta["folder"].orEmpty())
        )
    }

    fun bodyToMarkdown(body: String, spans: List<NoteSpan>): String {
        val lines = body.split('\n')
        val out = StringBuilder()
        var lineStart = 0
        lines.forEachIndexed { index, line ->
            val lineEnd = lineStart + line.length
            val heading = spans.firstOrNull {
                (it.type == NoteSpan.H1 || it.type == NoteSpan.H2) &&
                    it.start <= lineStart && it.end >= lineEnd
            }
            val prefix = internalPrefix(line)
            val contentStart = lineStart + prefix.length
            val inline = inlineToMarkdown(
                body = body,
                start = contentStart,
                end = lineEnd,
                spans = spans
            )
            when {
                heading != null -> {
                    out.append(if (heading.type == NoteSpan.H1) "# " else "## ")
                    out.append(inline)
                }
                prefix == CHECK_OPEN -> out.append("- [ ] ").append(inline)
                prefix == CHECK_DONE -> out.append("- [x] ").append(inline)
                prefix == BULLET -> out.append("- ").append(inline)
                prefix == QUOTE -> out.append("> ").append(inline)
                prefix.isNotEmpty() && NUMBER_PREFIX.matches(prefix) ->
                    out.append(prefix).append(inline)
                else -> out.append(escapeLineStart(inline))
            }
            if (index != lines.lastIndex) out.append('\n')
            lineStart = lineEnd + 1
        }
        return out.toString()
    }

    fun bodyFromMarkdown(markdown: String): Pair<String, List<NoteSpan>> {
        val lines = markdown.split('\n')
        val out = StringBuilder()
        val spans = mutableListOf<NoteSpan>()
        var lineStart = 0
        lines.forEachIndexed { index, line ->
            val parsed = parseLine(line, lineStart)
            out.append(parsed.first)
            spans.addAll(parsed.second)
            if (index != lines.lastIndex) {
                out.append('\n')
                lineStart = out.length
            } else {
                lineStart = out.length
            }
        }
        return out.toString() to spans.distinct().sortedWith(
            compareBy({ it.start }, { it.end }, { it.type })
        )
    }

    private fun parseLine(line: String, lineStart: Int): Pair<String, List<NoteSpan>> {
        val escapedBlock = line.length > 1 && line[0] == '\\' &&
            (line[1] == '#' || line[1] == '>' || line[1] == '-' ||
                line[1] == '+' || line[1].isDigit())
        var content = line
        var prefix = ""
        var heading: String? = null

        if (!escapedBlock) {
            when {
                line.startsWith("# ") -> {
                    heading = NoteSpan.H1
                    content = line.substring(2)
                }
                line.startsWith("## ") -> {
                    heading = NoteSpan.H2
                    content = line.substring(3)
                }
                MD_CHECK_DONE.containsMatchIn(line) -> {
                    prefix = CHECK_DONE
                    content = line.substring(6)
                }
                MD_CHECK_OPEN.containsMatchIn(line) -> {
                    prefix = CHECK_OPEN
                    content = line.substring(6)
                }
                line.startsWith("- ") -> {
                    prefix = BULLET
                    content = line.substring(2)
                }
                line.startsWith("> ") -> {
                    prefix = QUOTE
                    content = line.substring(2)
                }
                line == ">" -> {
                    prefix = QUOTE
                    content = ""
                }
                NUMBER_PREFIX.containsMatchIn(line) -> {
                    val match = NUMBER_PREFIX.find(line)!!
                    prefix = match.value
                    content = line.substring(match.value.length)
                }
            }
        }

        val inline = parseInline(content)
        val internal = prefix + inline.first
        val spans = inline.second.map {
            NoteSpan(it.type, it.start + lineStart + prefix.length, it.end + lineStart + prefix.length)
        }.toMutableList()
        if (heading != null && internal.isNotEmpty()) {
            spans.add(NoteSpan(heading, lineStart, lineStart + internal.length))
        }
        return internal to spans
    }

    private fun parseInline(text: String): Pair<String, List<NoteSpan>> {
        val out = StringBuilder()
        val spans = mutableListOf<NoteSpan>()
        val opens = mutableMapOf<String, Int>()
        var i = 0
        while (i < text.length) {
            if (text[i] == '\\' && i + 1 < text.length) {
                out.append(text[i + 1])
                i += 2
                continue
            }
            val token = when {
                text.startsWith("**", i) -> "**"
                text.startsWith("~~", i) -> "~~"
                text[i] == '*' -> "*"
                else -> null
            }
            if (token == null) {
                out.append(text[i])
                i++
                continue
            }
            val type = when (token) {
                "**" -> NoteSpan.BOLD
                "~~" -> NoteSpan.STRIKE
                else -> NoteSpan.ITALIC
            }
            val start = opens.remove(type)
            if (start != null) {
                if (start < out.length) spans.add(NoteSpan(type, start, out.length))
            } else {
                opens[type] = out.length
            }
            i += token.length
        }
        opens.forEach { (type, start) ->
            if (start < out.length) spans.add(NoteSpan(type, start, out.length))
        }
        return out.toString() to spans
    }

    private fun inlineToMarkdown(
        body: String,
        start: Int,
        end: Int,
        spans: List<NoteSpan>
    ): String {
        if (start >= end) return ""
        val relevant = spans.filter {
            it.type in INLINE_TYPES && it.start < end && it.end > start
        }
        val points = sortedSetOf(start, end)
        relevant.forEach {
            points.add(maxOf(it.start, start))
            points.add(minOf(it.end, end))
        }
        val out = StringBuilder()
        var previous = start
        points.forEach { point ->
            if (point <= previous) return@forEach
            val text = body.substring(previous, point)
            val active = relevant.filter { it.start <= previous && it.end >= point }
            val open = buildString {
                if (active.any { it.type == NoteSpan.BOLD }) append("**")
                if (active.any { it.type == NoteSpan.ITALIC }) append('*')
                if (active.any { it.type == NoteSpan.STRIKE }) append("~~")
            }
            val close = buildString {
                if (active.any { it.type == NoteSpan.STRIKE }) append("~~")
                if (active.any { it.type == NoteSpan.ITALIC }) append('*')
                if (active.any { it.type == NoteSpan.BOLD }) append("**")
            }
            out.append(open).append(escapeInline(text)).append(close)
            previous = point
        }
        return out.toString()
    }

    private fun internalPrefix(line: String): String {
        for (prefix in listOf(CHECK_OPEN, CHECK_DONE, BULLET, QUOTE)) {
            if (line.startsWith(prefix)) return prefix
        }
        NUMBER_PREFIX.find(line)?.let { return it.value }
        return ""
    }

    private fun escapeInline(text: String): String = buildString {
        text.forEach { char ->
            when (char) {
                '\\', '*', '~' -> append('\\').append(char)
                else -> append(char)
            }
        }
    }

    private fun escapeLineStart(text: String): String {
        if (text.isEmpty()) return text
        val needsEscape = text.startsWith("#") ||
            text.startsWith(">") ||
            text.startsWith("-") ||
            text.startsWith("+") ||
            NUMBER_PREFIX.containsMatchIn(text) ||
            text == "---"
        return if (needsEscape) "\\$text" else text
    }

    private fun quote(value: String): String = buildString {
        append('"')
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (char.code < 0x20) {
                    append("\\u")
                    append(char.code.toString(16).padStart(4, '0'))
                } else {
                    append(char)
                }
            }
        }
        append('"')
    }

    private fun unquote(value: String): String {
        val trimmed = value.trim()
        if (trimmed.length < 2 || trimmed.first() != '"' || trimmed.last() != '"') {
            return trimmed
        }
        val out = StringBuilder()
        var i = 1
        while (i < trimmed.length - 1) {
            val char = trimmed[i]
            if (char == '\\' && i + 1 < trimmed.length - 1) {
                when (val next = trimmed[i + 1]) {
                    'n' -> out.append('\n')
                    'r' -> out.append('\r')
                    't' -> out.append('\t')
                    '"' -> out.append('"')
                    '\\' -> out.append('\\')
                    'u' -> {
                        if (i + 5 < trimmed.length) {
                            val hex = trimmed.substring(i + 2, i + 6)
                            hex.toIntOrNull(16)?.let { out.append(it.toChar()) }
                            i += 4
                        }
                    }
                    else -> out.append(next)
                }
                i += 2
            } else {
                out.append(char)
                i++
            }
        }
        return out.toString()
    }
}
