package com.ncportal.app.ui.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
fun MarkdownBody(text: String, modifier: Modifier = Modifier) {
    val blocks = remember(text) { parseMarkdown(text) }
    val codeBg = MaterialTheme.colorScheme.surfaceVariant
    val linkColor = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        blocks.forEach { BlockView(it, codeBg, linkColor) }
    }
}

@Composable
private fun BlockView(block: MdBlock, codeBg: Color, linkColor: Color) {
    when (block) {
        is MdBlock.Heading -> Text(
            inlineAnnotated(block.text, codeBg, linkColor),
            style = when (block.level) {
                1 -> MaterialTheme.typography.headlineSmall
                2 -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.titleMedium
            },
        )
        is MdBlock.Paragraph -> Text(
            inlineAnnotated(block.text, codeBg, linkColor),
            style = MaterialTheme.typography.bodyLarge,
        )
        is MdBlock.CodeBlock -> Surface(
            color = codeBg,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = block.code,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp),
            )
        }
        is MdBlock.BulletList -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            block.items.forEach { item ->
                Row {
                    Text("•  ", style = MaterialTheme.typography.bodyLarge)
                    Text(inlineAnnotated(item, codeBg, linkColor), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        is MdBlock.OrderedList -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            block.items.forEachIndexed { idx, item ->
                Row {
                    Text("${idx + 1}.  ", style = MaterialTheme.typography.bodyLarge)
                    Text(inlineAnnotated(item, codeBg, linkColor), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        is MdBlock.Quote -> Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                block.blocks.forEach { BlockView(it, codeBg, linkColor) }
            }
        }
        MdBlock.Rule -> HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

// ---- inline char-scanner (no regex) ----------------------------------------

fun inlineAnnotated(text: String, codeBg: Color, linkColor: Color): AnnotatedString =
    buildAnnotatedString { appendInline(text, codeBg, linkColor) }

private fun AnnotatedString.Builder.appendInline(s: String, codeBg: Color, linkColor: Color) {
    var i = 0
    while (i < s.length) {
        val c = s[i]
        when {
            s.startsWith("**", i) -> {                          // bold (checked before single *)
                val end = s.indexOf("**", i + 2)
                if (end >= 0) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        appendInline(s.substring(i + 2, end), codeBg, linkColor)
                    }
                    i = end + 2
                } else { append(c); i++ }
            }
            c == '`' -> {                                       // inline code (no inner markers)
                val end = s.indexOf('`', i + 1)
                if (end >= 0) {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg)) {
                        append(s.substring(i + 1, end))
                    }
                    i = end + 1
                } else { append(c); i++ }
            }
            c == '[' -> {                                       // link
                val close = s.indexOf(']', i + 1)
                if (close >= 0 && s.getOrNull(close + 1) == '(') {
                    val urlEnd = s.indexOf(')', close + 2)
                    if (urlEnd >= 0) {
                        val url = s.substring(close + 2, urlEnd)
                        withLink(
                            LinkAnnotation.Url(
                                url,
                                TextLinkStyles(
                                    SpanStyle(
                                        color = linkColor,
                                        textDecoration = TextDecoration.Underline,
                                    ),
                                ),
                            ),
                        ) { appendInline(s.substring(i + 1, close), codeBg, linkColor) }
                        i = urlEnd + 1
                    } else { append(c); i++ }
                } else { append(c); i++ }
            }
            (c == '*' || c == '_') && isEmphasisStart(s, i) -> { // italic
                val end = findEmphasisEnd(s, i, c)
                if (end >= 0) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        appendInline(s.substring(i + 1, end), codeBg, linkColor)
                    }
                    i = end + 1
                } else { append(c); i++ }
            }
            else -> { append(c); i++ }
        }
    }
}

private fun isEmphasisStart(s: String, i: Int): Boolean {
    val next = s.getOrNull(i + 1) ?: return false
    if (next.isWhitespace()) return false
    if (s[i] == '_' && s.getOrNull(i - 1)?.isLetterOrDigit() == true) return false // snake_case
    return true
}

private fun findEmphasisEnd(s: String, start: Int, marker: Char): Int {
    var j = start + 1
    while (j < s.length) {
        if (s[j] == marker && !s[j - 1].isWhitespace()) {
            if (marker == '_' && s.getOrNull(j + 1)?.isLetterOrDigit() == true) { j++; continue }
            return j
        }
        j++
    }
    return -1
}
