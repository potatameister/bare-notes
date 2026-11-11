@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.potatameister.barenotes

import androidx.compose.foundation.text.input.ExpandPolicy
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

val BoldStyle = SpanStyle(fontWeight = FontWeight.Bold)
val ItalicStyle = SpanStyle(fontStyle = FontStyle.Italic)
val StrikeStyle = SpanStyle(textDecoration = TextDecoration.LineThrough)
val H1Style = SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
val H2Style = SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)

private const val BULLET = "• "
private const val CHECK_OPEN = "☐ "
private const val CHECK_DONE = "☑ "
private const val QUOTE = "❝ "
private val NUMBER_PREFIX = Regex("^(\\d+)\\.\\s+")

private fun styleForType(type: String): SpanStyle? = when (type) {
    NoteSpan.BOLD -> BoldStyle
    NoteSpan.ITALIC -> ItalicStyle
    NoteSpan.STRIKE -> StrikeStyle
    NoteSpan.H1 -> H1Style
    NoteSpan.H2 -> H2Style
    else -> null
}

private fun isHeadingStyle(style: SpanStyle): Boolean =
    style.fontSize == 24.sp || style.fontSize == 20.sp

private fun spanType(style: SpanStyle): String? = when {
    style.fontSize == 24.sp -> NoteSpan.H1
    style.fontSize == 20.sp -> NoteSpan.H2
    style.fontWeight == FontWeight.Bold -> NoteSpan.BOLD
    style.fontStyle == FontStyle.Italic -> NoteSpan.ITALIC
    style.textDecoration == TextDecoration.LineThrough -> NoteSpan.STRIKE
    else -> null
}

private fun spanMatches(style: SpanStyle, target: SpanStyle): Boolean = when (target) {
    BoldStyle -> style.fontWeight == FontWeight.Bold && !isHeadingStyle(style)
    ItalicStyle -> style.fontStyle == FontStyle.Italic
    StrikeStyle -> style.textDecoration == TextDecoration.LineThrough
    H1Style -> style.fontSize == 24.sp
    H2Style -> style.fontSize == 20.sp
    else -> style == target
}

fun createRichTextState(text: String, spans: List<NoteSpan>): TextFieldState =
    TextFieldState(text).also { state ->
        if (spans.isNotEmpty()) {
            state.edit {
                val length = text.length
                spans.forEach { span ->
                    val style = styleForType(span.type) ?: return@forEach
                    val start = span.start.coerceIn(0, length)
                    val end = span.end.coerceIn(0, length)
                    if (start >= end) return@forEach
                    addStyle(
                        spanStyle = style,
                        range = TextRange(start, end),
                        expandPolicy = if (isHeadingStyle(style)) {
                            ExpandPolicy.InsideOnly
                        } else {
                            ExpandPolicy.AtEnd
                        }
                    )
                }
            }
        }
    }

val RichTextStateSaver = listSaver<TextFieldState, Any>(
    save = { state ->
        if (state.text.length > 100_000) {
            emptyList()
        } else {
            buildList {
                add(state.text.toString())
                add(state.selection.start)
                add(state.selection.end)
                serializeSpans(state).forEach { span ->
                    add("${span.type}:${span.start}:${span.end}")
                }
            }
        }
    },
    restore = { saved ->
        if (saved.isEmpty()) return@listSaver null
        val text = saved.getOrNull(0) as? String ?: return@listSaver null
        val start = saved.getOrNull(1) as? Int ?: 0
        val end = saved.getOrNull(2) as? Int ?: 0
        val spans = saved.drop(3).mapNotNull { entry ->
            val parts = (entry as? String)?.split(':') ?: return@mapNotNull null
            if (parts.size != 3) return@mapNotNull null
            val spanStart = parts[1].toIntOrNull() ?: return@mapNotNull null
            val spanEnd = parts[2].toIntOrNull() ?: return@mapNotNull null
            if (parts[0] !in NoteSpan.TYPES || spanStart >= spanEnd) {
                null
            } else {
                NoteSpan(parts[0], spanStart, spanEnd)
            }
        }
        createRichTextState(text, spans).also { state ->
            state.edit {
                if (this.length > 0) {
                    when {
                        start <= 0 -> placeCursorBeforeCharAt(0)
                        start >= this.length -> placeCursorAtEnd()
                        else -> placeCursorAfterCharAt(start - 1)
                    }
                }
            }
        }
    }
)

fun serializeSpans(state: TextFieldState): List<NoteSpan> =
    state.textStyles.getSpanStyles(TextRange(0, state.text.length))
        .mapNotNull { range ->
            spanType(range.item)?.let { NoteSpan(it, range.start, range.end) }
        }
        .distinct()
        .sortedWith(compareBy({ it.start }, { it.end }, { it.type }))

private fun hasFullCoverage(state: TextFieldState, range: TextRange, target: SpanStyle): Boolean {
    if (range.collapsed) return false
    val styles = state.textStyles.getSpanStyles(range)
        .filter { spanMatches(it.item, target) }
        .sortedBy { it.start }
    var covered = 0
    var reach = range.min
    styles.forEach { style ->
        val start = maxOf(style.start, range.min)
        val end = minOf(style.end, range.max)
        if (end > start) {
            covered += (end - maxOf(start, reach)).coerceAtLeast(0)
            reach = maxOf(reach, end)
        }
    }
    return covered >= range.length
}

private fun queryRange(state: TextFieldState): TextRange {
    val selection = state.selection
    if (!selection.collapsed) return selection
    val cursor = selection.start.coerceIn(0, state.text.length)
    return if (cursor > 0) TextRange(cursor - 1, cursor) else TextRange(0, 0)
}

fun isInlineStyleActive(state: TextFieldState, style: SpanStyle): Boolean =
    hasFullCoverage(state, queryRange(state), style)

private fun TextFieldBuffer.removeStyles(range: TextRange, predicate: (SpanStyle) -> Boolean) {
    getSpanStyles(range)
        .filter { predicate(it.spanStyle) }
        .forEach { tracked ->
            val current = tracked.textRange
            when {
                current.start >= range.min && current.end <= range.max ->
                    removeStyle(tracked)
                current.start < range.min && current.end > range.max -> {
                    val oldEnd = current.end
                    tracked.textRange = TextRange(current.start, range.min)
                    addStyle(tracked.spanStyle, TextRange(range.max, oldEnd), ExpandPolicy.AtEnd)
                }
                current.start < range.min ->
                    tracked.textRange = TextRange(current.start, range.min)
                else ->
                    tracked.textRange = TextRange(range.max, current.end)
            }
        }
}

private fun TextFieldBuffer.addMergedStyle(range: TextRange, style: SpanStyle) {
    val query = TextRange(
        (range.min - 1).coerceAtLeast(0),
        (range.max + 1).coerceAtMost(length)
    )
    val matches = getSpanStyles(query).filter { spanMatches(it.spanStyle, style) }
    var start = range.min
    var end = range.max
    matches.forEach {
        start = minOf(start, it.textRange.start)
        end = maxOf(end, it.textRange.end)
        removeStyle(it)
    }
    if (start < end) {
        addStyle(style, TextRange(start, end), ExpandPolicy.AtEnd)
    }
}

fun toggleInlineStyle(state: TextFieldState, style: SpanStyle) {
    val range = if (state.selection.collapsed) {
        wordRange(state.text, state.selection.start)
    } else {
        state.selection
    }
    if (range.collapsed) return
    val active = hasFullCoverage(state, range, style)
    state.edit {
        if (active) {
            removeStyles(range) { spanMatches(it, style) }
        } else {
            addMergedStyle(range, style)
        }
    }
}

fun toggleHeading(state: TextFieldState, level: Int) {
    val target = if (level == 1) H1Style else H2Style
    val line = lineRange(state.text, state.selection.start)
    if (line.collapsed) return
    val active = hasFullCoverage(state, line, target)
    state.edit {
        removeStyles(line) { isHeadingStyle(it) }
        if (!active) {
            addStyle(target, line, ExpandPolicy.InsideOnly)
        }
    }
}

fun isHeadingActive(state: TextFieldState, level: Int): Boolean {
    val style = if (level == 1) H1Style else H2Style
    return hasFullCoverage(state, lineRange(state.text, state.selection.start), style)
}

fun togglePrefix(state: TextFieldState, kind: PrefixKind) {
    val text = state.text
    val cursor = state.selection.start.coerceIn(0, text.length)
    val (lineStart, lineEnd) = fullLineBounds(text, cursor)
    val line = text.substring(lineStart, lineEnd)
    val (oldPrefix, content) = splitLinePrefix(line)
    val offset = (cursor - lineStart - oldPrefix.length).coerceIn(0, content.length)
    val newPrefix = when (kind) {
        PrefixKind.BULLET -> if (oldPrefix == BULLET) "" else BULLET
        PrefixKind.QUOTE -> if (oldPrefix == QUOTE) "" else QUOTE
        PrefixKind.CHECK -> when (oldPrefix) {
            CHECK_OPEN -> CHECK_DONE
            CHECK_DONE -> ""
            else -> CHECK_OPEN
        }
        PrefixKind.NUMBER -> if (numberOf(oldPrefix) != null) "" else {
            "${nextNumber(text, lineStart)}. "
        }
    }
    state.edit {
        replace(lineStart, lineEnd, newPrefix + content)
        placeCursor(this, lineStart + newPrefix.length + offset)
    }
}

fun isPrefixActive(state: TextFieldState, kind: PrefixKind): Boolean {
    val line = lineRange(state.text, state.selection.start)
    if (line.collapsed) return false
    val text = state.text.substring(line.min, line.max)
    val prefix = splitLinePrefix(text).first
    return when (kind) {
        PrefixKind.BULLET -> prefix == BULLET
        PrefixKind.QUOTE -> prefix == QUOTE
        PrefixKind.CHECK -> prefix == CHECK_OPEN || prefix == CHECK_DONE
        PrefixKind.NUMBER -> numberOf(prefix) != null
    }
}

enum class PrefixKind { BULLET, NUMBER, CHECK, QUOTE }

fun clearFormatting(state: TextFieldState) {
    val text = state.text
    if (text.isEmpty()) return
    val selection = state.selection
    val block = TextRange(
        fullLineBounds(text, selection.min).first,
        fullLineBounds(text, selection.max).second
    )
    if (block.collapsed) return
    val stripped = text.substring(block.min, block.max)
        .split('\n')
        .joinToString("\n") { splitLinePrefix(it).second }
    state.edit {
        removeStyles(block) { spanType(it) != null }
        replace(block.min, block.max, stripped)
        placeCursor(this, block.min + stripped.length)
    }
}

fun toggleCheckAt(state: TextFieldState, lineStart: Int, lineEnd: Int) {
    val text = state.text
    if (lineStart < 0 || lineEnd > text.length || lineStart >= lineEnd) return
    val line = text.substring(lineStart, lineEnd)
    val replacement = when {
        line.startsWith(CHECK_OPEN) -> CHECK_DONE
        line.startsWith(CHECK_DONE) -> CHECK_OPEN
        else -> return
    }
    val oldLength = if (line.startsWith(CHECK_OPEN)) CHECK_OPEN.length else CHECK_DONE.length
    if (lineStart + oldLength > lineEnd) return
    state.edit {
        replace(lineStart, lineStart + oldLength, replacement)
    }
}

fun checkboxPrefixAt(state: TextFieldState, offset: Int): Pair<Int, Int>? {
    val (start, end) = fullLineBounds(state.text, offset)
    val line = state.text.substring(start, end)
    return if (line.startsWith(CHECK_OPEN) || line.startsWith(CHECK_DONE)) {
        start to end
    } else {
        null
    }
}

fun placeCursorAt(state: TextFieldState, offset: Int) {
    state.edit {
        placeCursor(this, offset.coerceIn(0, length))
    }
}

fun checkboxOutputTransformation(): OutputTransformation = OutputTransformation {
    val buffer = asCharSequence()
    var lineStart = 0
    while (lineStart < length) {
        if (buffer[lineStart] == '☐' || buffer[lineStart] == '☑') {
            addStyle(SpanStyle(color = Color.Transparent), lineStart, lineStart + 1)
        }
        val newline = buffer.indexOf('\n', lineStart)
        if (newline < 0) break
        lineStart = newline + 1
    }
}

fun listContinuationTransformation(): InputTransformation = InputTransformation {
    if (changes.changeCount != 1) return@InputTransformation
    val inserted = changes.getRange(0)
    val original = changes.getOriginalRange(0)
    if (!original.collapsed || inserted.length != 1) return@InputTransformation
    val buffer = asCharSequence()
    if (inserted.start >= length || buffer[inserted.start] != '\n') return@InputTransformation

    val cursor = inserted.start
    val lineStart = buffer.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0))
        .let { if (it < 0) 0 else it + 1 }
    if (lineStart >= cursor) return@InputTransformation
    val previousLine = buffer.subSequence(lineStart, cursor).toString()
    val (prefix, content) = splitLinePrefix(previousLine)
    if (prefix.isEmpty()) return@InputTransformation

    if (content.isBlank()) {
        replace(lineStart, cursor + 1, "")
        placeCursor(this, lineStart)
        return@InputTransformation
    }

    val next = when {
        prefix == BULLET -> BULLET
        prefix == CHECK_OPEN || prefix == CHECK_DONE -> CHECK_OPEN
        prefix == QUOTE -> QUOTE
        numberOf(prefix) != null -> "${(numberOf(prefix) ?: 0) + 1}. "
        else -> return@InputTransformation
    }
    val at = cursor + 1
    insert(at, next)
    placeCursor(this, at + next.length)
}

private fun wordRange(text: CharSequence, cursor: Int): TextRange {
    if (text.isEmpty()) return TextRange(0, 0)
    val position = cursor.coerceIn(0, text.length)
    var start = position
    var end = position
    while (start > 0 && !text[start - 1].isWhitespace()) start--
    while (end < text.length && !text[end].isWhitespace()) end++
    return TextRange(start, end)
}

private fun lineRange(text: CharSequence, cursor: Int): TextRange {
    val position = cursor.coerceIn(0, text.length)
    val start = if (position <= 0) 0 else {
        text.lastIndexOf('\n', position - 1).let { if (it < 0) 0 else it + 1 }
    }
    val end = text.indexOf('\n', position).let { if (it < 0) text.length else it }
    return TextRange(start, end)
}

private fun fullLineBounds(text: CharSequence, cursor: Int): Pair<Int, Int> {
    val range = lineRange(text, cursor)
    return range.min to range.max
}

private fun splitLinePrefix(line: String): Pair<String, String> {
    for (prefix in listOf(BULLET, CHECK_OPEN, CHECK_DONE, QUOTE)) {
        if (line.startsWith(prefix)) return prefix to line.substring(prefix.length)
    }
    NUMBER_PREFIX.find(line)?.let { return it.value to line.substring(it.value.length) }
    return "" to line
}

private fun numberOf(prefix: String): Int? =
    NUMBER_PREFIX.find(prefix)?.groupValues?.getOrNull(1)?.toIntOrNull()

private fun nextNumber(text: CharSequence, lineStart: Int): Int {
    if (lineStart <= 0) return 1
    val previousEnd = lineStart - 1
    if (previousEnd <= 0) return 1
    val previousStart = text.lastIndexOf('\n', previousEnd - 1)
        .let { if (it < 0) 0 else it + 1 }
    val previous = text.substring(previousStart, previousEnd)
    return (numberOf(splitLinePrefix(previous).first) ?: 0) + 1
}

private fun placeCursor(buffer: TextFieldBuffer, index: Int) {
    if (buffer.length == 0) return
    val position = index.coerceIn(0, buffer.length)
    when {
        position <= 0 -> buffer.placeCursorBeforeCharAt(0)
        position >= buffer.length -> buffer.placeCursorAtEnd()
        else -> buffer.placeCursorAfterCharAt(position - 1)
    }
}

fun previewFor(body: String): String {
    val first = body.lineSequence().firstOrNull { it.isNotBlank() } ?: return ""
    val stripped = splitLinePrefix(first).second
    return stripped.ifBlank { first }
}

fun countWords(text: CharSequence): Int {
    var count = 0
    var inWord = false
    text.forEach { char ->
        if (char.isLetterOrDigit()) {
            if (!inWord) {
                count++
                inWord = true
            }
        } else {
            inWord = false
        }
    }
    return count
}
