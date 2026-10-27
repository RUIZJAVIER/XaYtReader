package com.deedeedev.ytreader.ui.reader

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit

/**
 * Lightweight Markdown renderer for AI chat answers.
 *
 * Supported syntax (rendered cleanly, without the raw markers):
 * - Headings: `#`, `##`, `###` ... `######`
 * - Bold: `**text**`
 * - Italic: `*text*`
 * - Inline code: `` `text` ``
 * - Bullet lists: lines starting with `- `, `* ` or `+ `
 * - Code fence markers (``` / ~~~) are stripped
 */
internal object MarkdownParser {

    private val headingRegex = Regex("^(#{1,6})\\s+(.*)$")
    private val bulletRegex = Regex("^[-*+]\\s+(.*)$")
    private val codeFenceRegex = Regex("^\\s*(```|~~~)\\s*\\w*\\s*$")
    private val orderedListRegex = Regex("^(\\d+)([.)])\\s+(.*)$")
    private val inlineRegex = Regex("\\*\\*(.+?)\\*\\*|\\*(.+?)\\*|`(.+?)`")

    private const val BULLET = "•  "

    fun parse(
        markdown: String,
        baseFontSize: TextUnit
    ): AnnotatedString = buildAnnotatedString {
        val lines = markdown.lines()
        lines.forEachIndexed { index, rawLine ->
            if (index > 0) append('\n')
            appendLine(rawLine, baseFontSize)
        }
    }

    private fun AnnotatedString.Builder.appendLine(
        rawLine: String,
        baseFontSize: TextUnit
    ) {
        val line = rawLine.trimEnd()
        if (line.isEmpty()) return

        if (codeFenceRegex.matches(line)) {
            return
        }

        val headingMatch = headingRegex.matchEntire(line)
        if (headingMatch != null) {
            val level = headingMatch.groupValues[1].length
            val content = headingMatch.groupValues[2]
            if (content.isBlank()) return
            val headingSpan = SpanStyle(
                fontWeight = FontWeight.Bold,
                fontSize = headingFontSize(level, baseFontSize)
            )
            pushStyle(headingSpan)
            appendInline(content)
            pop()
            return
        }

        val bulletMatch = bulletRegex.matchEntire(line)
        if (bulletMatch != null) {
            append(BULLET)
            appendInline(bulletMatch.groupValues[1])
            return
        }

        val orderedMatch = orderedListRegex.matchEntire(line)
        if (orderedMatch != null) {
            append("${orderedMatch.groupValues[1]}${orderedMatch.groupValues[2]}  ")
            appendInline(orderedMatch.groupValues[3])
            return
        }

        appendInline(line)
    }

    private fun headingFontSize(level: Int, baseFontSize: TextUnit): TextUnit {
        val factor = when (level) {
            1 -> 1.45f
            2 -> 1.3f
            3 -> 1.15f
            else -> 1.0f
        }
        return baseFontSize * factor
    }

    private fun AnnotatedString.Builder.appendInline(text: String) {
        var lastIndex = 0
        for (match in inlineRegex.findAll(text)) {
            val matchStart = match.range.first
            val matchEnd = match.range.last + 1
            if (matchStart > lastIndex) {
                append(text.substring(lastIndex, matchStart))
            }
            val boldContent = match.groupValues[1]
            val italicContent = match.groupValues[2]
            val codeContent = match.groupValues[3]
            when {
                boldContent.isNotEmpty() -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(boldContent)
                    pop()
                }

                italicContent.isNotEmpty() -> {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(italicContent)
                    pop()
                }

                else -> {
                    pushStyle(SpanStyle(fontFamily = FontFamily.Monospace))
                    append(codeContent)
                    pop()
                }
            }
            lastIndex = matchEnd
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}

@Composable
internal fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.bodyMedium
) {
    val annotated = remember(text, style) {
        MarkdownParser.parse(markdown = text, baseFontSize = style.fontSize)
    }
    Text(
        text = annotated,
        modifier = modifier,
        color = color,
        style = style
    )
}
