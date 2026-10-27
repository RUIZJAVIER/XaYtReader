package com.deedeedev.ytreader.ui.reader

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTextTest {

    private fun parse(markdown: String): AnnotatedString =
        MarkdownParser.parse(markdown = markdown, baseFontSize = 14.sp)

    @Test
    fun `renders headings bold italics and bullets without raw markers`() {
        val annotated = parse(
            "## Resumen\n\n**Importante**: revisa *esto* bien\n- primer punto\n- segundo **fuerte**"
        )

        assertEquals(
            "Resumen\n\nImportante: revisa esto bien\n•  primer punto\n•  segundo fuerte",
            annotated.text
        )
        assertFalse(annotated.text.contains("#"))
        assertFalse(annotated.text.contains("**"))
    }

    @Test
    fun `heading receives bold style and larger font size`() {
        val annotated = parse("## Título")

        assertEquals("Título", annotated.text)
        val headingSpan = annotated.spanStyles.single()
        assertEquals(0, headingSpan.start)
        assertEquals("Título".length, headingSpan.end)
        assertEquals(FontWeight.Bold, headingSpan.spanStyle.fontWeight)
        assertEquals(14.sp * 1.3f, headingSpan.spanStyle.fontSize)
    }

    @Test
    fun `bold segment is styled without the asterisks`() {
        val annotated = parse("Texto **en negrita** y normal")

        assertEquals("Texto en negrita y normal", annotated.text)
        val boldSpan = annotated.spanStyles.single()
        assertEquals("en negrita", annotated.text.substring(boldSpan.start, boldSpan.end))
        assertEquals(FontWeight.Bold, boldSpan.spanStyle.fontWeight)
    }

    @Test
    fun `italic and inline code are styled and cleaned`() {
        val annotated = parse("Esto es *cursiva* y `código` inline")

        assertEquals("Esto es cursiva y código inline", annotated.text)
        val italicSpan = annotated.spanStyles.first { it.spanStyle.fontStyle == FontStyle.Italic }
        assertEquals("cursiva", annotated.text.substring(italicSpan.start, italicSpan.end))
        val codeSpan = annotated.spanStyles.first { it.spanStyle.fontFamily == FontFamily.Monospace }
        assertEquals("código", annotated.text.substring(codeSpan.start, codeSpan.end))
    }

    @Test
    fun `bullet lists replace markdown markers with bullets`() {
        val annotated = parse("- uno\n- dos\n+ tres")

        assertEquals("•  uno\n•  dos\n•  tres", annotated.text)
    }

    @Test
    fun `ordered lists keep their numbers and strip nothing`() {
        val annotated = parse("1. uno\n2. dos")

        assertEquals("1.  uno\n2.  dos", annotated.text)
    }

    @Test
    fun `code fence markers are removed but content is kept`() {
        val annotated = parse("```\nplain code line\n```")

        assertEquals("plain code line", annotated.text)
        assertFalse(annotated.text.contains("```"))
    }

    @Test
    fun `plain text and empty input stay unchanged`() {
        assertEquals("Hola mundo", parse("Hola mundo").text)
        assertEquals("", parse("").text)
        assertEquals(0, parse("Sin estilos").spanStyles.size)
    }

    @Test
    fun `mixed heading and list styles do not overlap`() {
        val annotated = parse("### Pasos\n1. A\n2. B\n\n**Fin.**")

        assertTrue(annotated.spanStyles.size >= 2)
        assertEquals(
            "Pasos\n1.  A\n2.  B\n\nFin.",
            annotated.text
        )
    }
}
