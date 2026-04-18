package com.slock.app.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val MarkdownLinkTag = "markdown_link"
private val mentionPattern = Regex("@[\\w.-]+")
private val codeBlockRegex = Regex("```(?:\\w*\\n)?([\\s\\S]*?)```")
private val headingRegex = Regex("^(#{1,6})\\s+(.+)$")
private val unorderedListRegex = Regex("^\\s*[-*+]\\s+(.+)$")
private val orderedListRegex = Regex("^\\s*(\\d+)[.)]\\s+(.+)$")
private val horizontalRuleRegex = Regex("^\\s{0,3}((\\*\\s*){3,}|(-\\s*){3,}|(_\\s*){3,})$")
private val tableSeparatorRegex = Regex("^\\s*\\|?(\\s*:?-{3,}:?\\s*\\|)+\\s*:?-{3,}:?\\s*\\|?\\s*$")
private val markdownTokenChars = setOf('[', '*', '_', '`', '@', '~')
private val LinkBlue = Color(0xFF0055FF)

sealed interface MarkdownBlock {
    data class Paragraph(val text: AnnotatedString) : MarkdownBlock
    data class Heading(val level: Int, val text: AnnotatedString) : MarkdownBlock
    data class Quote(val text: AnnotatedString) : MarkdownBlock
    data class CodeBlock(val code: String) : MarkdownBlock
    data class ListBlock(
        val ordered: Boolean,
        val items: List<AnnotatedString>,
        val startNumber: Int = 1
    ) : MarkdownBlock
    object HorizontalRule : MarkdownBlock
    data class Table(
        val headers: List<AnnotatedString>,
        val rows: List<List<AnnotatedString>>
    ) : MarkdownBlock
}

private data class MarkdownInlineContext(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val strikethrough: Boolean = false,
    val link: String? = null
) {
    fun spanStyle(extra: SpanStyle = SpanStyle()): SpanStyle {
        val linkDecoration = if (link != null) TextDecoration.Underline else null
        val strikeDecoration = if (strikethrough) TextDecoration.LineThrough else null
        val combined = TextDecoration.combine(listOfNotNull(linkDecoration, strikeDecoration))
        val base = SpanStyle(
            fontWeight = if (bold) FontWeight.Bold else null,
            fontStyle = if (italic) FontStyle.Italic else null,
            color = if (link != null) LinkBlue else Color.Unspecified,
            textDecoration = combined.takeIf { it != TextDecoration.None }
        )
        return base.merge(extra)
    }
}

private data class LinkToken(
    val label: String,
    val url: String,
    val endIndex: Int
)

private data class ListParseResult(
    val block: MarkdownBlock.ListBlock,
    val nextIndex: Int
)

private data class TableParseResult(
    val block: MarkdownBlock.Table,
    val nextIndex: Int
)

fun buildMentionAnnotatedString(content: String, highlightQuery: String = ""): AnnotatedString {
    return buildAnnotatedString {
        appendMarkdownInline(content, highlightQuery, MarkdownInlineContext())
    }
}

fun extractMarkdownCodeBlocks(content: String): List<String> {
    return parseMarkdownBlocks(content)
        .filterIsInstance<MarkdownBlock.CodeBlock>()
        .map { it.code }
}

fun parseMarkdownBlocks(content: String, highlightQuery: String = ""): List<MarkdownBlock> {
    if (content.isBlank()) return emptyList()

    val blocks = mutableListOf<MarkdownBlock>()
    var lastIndex = 0

    codeBlockRegex.findAll(content).forEach { match ->
        val before = content.substring(lastIndex, match.range.first)
        if (before.isNotBlank()) {
            blocks += parseTextBlocks(before.trim('\n'), highlightQuery)
        }
        blocks += MarkdownBlock.CodeBlock(match.groupValues[1].trimEnd())
        lastIndex = match.range.last + 1
    }

    val remaining = content.substring(lastIndex)
    if (remaining.isNotBlank()) {
        blocks += parseTextBlocks(remaining.trim('\n'), highlightQuery)
    }

    return blocks
}

@Composable
fun NeoMessageContent(
    content: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color(0xFF222222),
    highlightQuery: String = ""
) {
    val blocks = remember(content, highlightQuery) {
        parseMarkdownBlocks(content, highlightQuery)
    }
    if (blocks.isEmpty()) return

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            MarkdownBlockView(block = block, textColor = textColor)
        }
    }
}

@Composable
private fun MarkdownBlockView(
    block: MarkdownBlock,
    textColor: Color
) {
    when (block) {
        is MarkdownBlock.CodeBlock -> {
            val clipboardManager = LocalClipboardManager.current
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Black)
                    .border(2.dp, Black, RectangleShape)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    androidx.compose.material3.Text(
                        text = "CODE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = White.copy(alpha = 0.7f)
                    )
                    androidx.compose.material3.Text(
                        text = "COPY",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = White,
                        modifier = Modifier.clickable {
                            clipboardManager.setText(AnnotatedString(block.code))
                        }
                    )
                }
                androidx.compose.material3.Text(
                    text = block.code,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp
                    ),
                    color = White
                )
            }
        }

        is MarkdownBlock.Heading -> {
            val headingStyle = when (block.level) {
                1 -> MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.Black, lineHeight = 30.sp)
                2 -> MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Black, lineHeight = 28.sp)
                3 -> MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 26.sp)
                else -> MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp)
            }
            MarkdownAnnotatedText(text = block.text, style = headingStyle, color = textColor)
        }

        is MarkdownBlock.ListBlock -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                block.items.forEachIndexed { index, item ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        androidx.compose.material3.Text(
                            text = if (block.ordered) "${block.startNumber + index}." else "•",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = textColor,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        MarkdownAnnotatedText(
                            text = item,
                            style = MessageTextStyles.bodyStyle(MaterialTheme.typography),
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        MarkdownBlock.HorizontalRule -> {
            Divider(
                color = Black,
                thickness = 2.dp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        is MarkdownBlock.Paragraph -> {
            MarkdownAnnotatedText(
                text = block.text,
                style = MessageTextStyles.bodyStyle(MaterialTheme.typography),
                color = textColor
            )
        }

        is MarkdownBlock.Quote -> {
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(Black)
                )
                MarkdownAnnotatedText(
                    text = block.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                        lineHeight = 20.sp
                    ),
                    color = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }

        is MarkdownBlock.Table -> {
            MarkdownTable(block = block, textColor = textColor)
        }
    }
}

@Composable
private fun MarkdownAnnotatedText(
    text: AnnotatedString,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    ClickableText(
        text = text,
        style = style.copy(color = color),
        modifier = modifier,
        onClick = { offset ->
            text.getStringAnnotations(MarkdownLinkTag, offset, offset)
                .firstOrNull()
                ?.let { uriHandler.openUri(it.item) }
        }
    )
}

@Composable
private fun MarkdownTable(
    block: MarkdownBlock.Table,
    textColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Black, RectangleShape)
    ) {
        MarkdownTableRow(
            cells = block.headers,
            textColor = textColor,
            header = true
        )
        block.rows.forEach { row ->
            MarkdownTableRow(cells = row, textColor = textColor)
        }
    }
}

@Composable
private fun MarkdownTableRow(
    cells: List<AnnotatedString>,
    textColor: Color,
    header: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Black, RectangleShape)
    ) {
        cells.forEach { cell ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Black, RectangleShape)
                    .background(if (header) Yellow.copy(alpha = 0.35f) else White)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                MarkdownAnnotatedText(
                    text = cell,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (header) FontWeight.Bold else FontWeight.Normal,
                        lineHeight = 18.sp
                    ),
                    color = textColor
                )
            }
        }
    }
}

private fun parseTextBlocks(content: String, highlightQuery: String): List<MarkdownBlock> {
    if (content.isBlank()) return emptyList()

    val lines = content.lines()
    val blocks = mutableListOf<MarkdownBlock>()
    var index = 0

    while (index < lines.size) {
        val line = lines[index]
        if (line.isBlank()) {
            index++
            continue
        }

        val headingMatch = headingRegex.matchEntire(line.trim())
        if (headingMatch != null) {
            blocks += MarkdownBlock.Heading(
                level = headingMatch.groupValues[1].length,
                text = buildMentionAnnotatedString(headingMatch.groupValues[2].trim(), highlightQuery)
            )
            index++
            continue
        }

        if (horizontalRuleRegex.matches(line.trim())) {
            blocks += MarkdownBlock.HorizontalRule
            index++
            continue
        }

        val table = parseTable(lines, index, highlightQuery)
        if (table != null) {
            blocks += table.block
            index = table.nextIndex
            continue
        }

        if (line.trimStart().startsWith(">")) {
            val quoteLines = mutableListOf<String>()
            while (index < lines.size && lines[index].trimStart().startsWith(">")) {
                quoteLines += lines[index].trimStart().removePrefix(">").trimStart()
                index++
            }
            blocks += MarkdownBlock.Quote(
                text = buildMentionAnnotatedString(quoteLines.joinToString("\n"), highlightQuery)
            )
            continue
        }

        val listBlock = parseList(lines, index, highlightQuery)
        if (listBlock != null) {
            blocks += listBlock.block
            index = listBlock.nextIndex
            continue
        }

        val paragraphLines = mutableListOf<String>()
        while (index < lines.size) {
            if (lines[index].isBlank()) break
            if (paragraphLines.isNotEmpty() && startsNewBlock(lines, index)) break
            paragraphLines += lines[index].trimEnd()
            index++
        }
        blocks += MarkdownBlock.Paragraph(
            text = buildMentionAnnotatedString(paragraphLines.joinToString("\n"), highlightQuery)
        )
    }

    return blocks
}

private fun parseList(lines: List<String>, startIndex: Int, highlightQuery: String): ListParseResult? {
    val firstLine = lines[startIndex]
    val firstOrdered = orderedListRegex.matchEntire(firstLine)
    val firstUnordered = unorderedListRegex.matchEntire(firstLine)
    if (firstOrdered == null && firstUnordered == null) return null

    val ordered = firstOrdered != null
    val startNumber = firstOrdered?.groupValues?.get(1)?.toIntOrNull() ?: 1
    val items = mutableListOf<AnnotatedString>()
    var index = startIndex

    while (index < lines.size) {
        val currentLine = lines[index]
        val orderedMatch = orderedListRegex.matchEntire(currentLine)
        val unorderedMatch = unorderedListRegex.matchEntire(currentLine)
        val content = when {
            ordered && orderedMatch != null -> orderedMatch.groupValues[2]
            !ordered && unorderedMatch != null -> unorderedMatch.groupValues[1]
            else -> break
        }

        val itemLines = mutableListOf(content)
        index++
        while (index < lines.size) {
            val nextLine = lines[index]
            if (nextLine.isBlank()) break
            if (startsNewBlock(lines, index) || isListLine(nextLine)) break
            itemLines += nextLine.trim()
            index++
        }
        items += buildMentionAnnotatedString(itemLines.joinToString(" "), highlightQuery)

        if (index < lines.size && lines[index].isBlank()) break
    }

    return if (items.isEmpty()) null else ListParseResult(
        block = MarkdownBlock.ListBlock(ordered = ordered, items = items, startNumber = startNumber),
        nextIndex = index
    )
}

private fun parseTable(lines: List<String>, startIndex: Int, highlightQuery: String): TableParseResult? {
    if (startIndex + 1 >= lines.size) return null
    val headerLine = lines[startIndex]
    val separatorLine = lines[startIndex + 1]
    if (!headerLine.contains("|") || !tableSeparatorRegex.matches(separatorLine.trim())) return null

    val headers = splitTableRow(headerLine)
    if (headers.size < 2) return null

    val rows = mutableListOf<List<AnnotatedString>>()
    var index = startIndex + 2
    while (index < lines.size && lines[index].isNotBlank() && lines[index].contains("|")) {
        rows += splitTableRow(lines[index]).map { buildMentionAnnotatedString(it, highlightQuery) }
        index++
    }

    return TableParseResult(
        block = MarkdownBlock.Table(
            headers = headers.map { buildMentionAnnotatedString(it, highlightQuery) },
            rows = rows
        ),
        nextIndex = index
    )
}

private fun splitTableRow(line: String): List<String> {
    return line.trim().trim('|').split('|').map { it.trim() }
}

private fun startsNewBlock(lines: List<String>, index: Int): Boolean {
    val line = lines[index]
    return headingRegex.matches(line.trim()) ||
        horizontalRuleRegex.matches(line.trim()) ||
        line.trimStart().startsWith(">") ||
        isListLine(line) ||
        parseTable(lines, index, "") != null
}

private fun isListLine(line: String): Boolean {
    return unorderedListRegex.matches(line) || orderedListRegex.matches(line)
}

private fun AnnotatedString.Builder.appendMarkdownInline(
    content: String,
    highlightQuery: String,
    context: MarkdownInlineContext
) {
    var index = 0
    while (index < content.length) {
        val link = parseLinkAt(content, index)
        if (link != null) {
            pushStringAnnotation(MarkdownLinkTag, link.url)
            withStyle(context.copy(link = link.url).spanStyle()) {
                appendMarkdownInline(link.label, highlightQuery, context.copy(link = link.url))
            }
            pop()
            index = link.endIndex
            continue
        }

        val strongStar = parseDelimited(content, index, "**")
        if (strongStar != null) {
            appendMarkdownInline(strongStar.first, highlightQuery, context.copy(bold = true))
            index = strongStar.second
            continue
        }

        val strongUnderscore = parseDelimited(content, index, "__")
        if (strongUnderscore != null) {
            appendMarkdownInline(strongUnderscore.first, highlightQuery, context.copy(bold = true))
            index = strongUnderscore.second
            continue
        }

        val strikethrough = parseDelimited(content, index, "~~")
        if (strikethrough != null) {
            appendMarkdownInline(strikethrough.first, highlightQuery, context.copy(strikethrough = true))
            index = strikethrough.second
            continue
        }

        val italicStar = parseDelimited(content, index, "*")
        if (italicStar != null) {
            appendMarkdownInline(italicStar.first, highlightQuery, context.copy(italic = true))
            index = italicStar.second
            continue
        }

        val italicUnderscore = parseDelimited(content, index, "_")
        if (italicUnderscore != null) {
            appendMarkdownInline(italicUnderscore.first, highlightQuery, context.copy(italic = true))
            index = italicUnderscore.second
            continue
        }

        if (content[index] == '`') {
            val end = content.indexOf('`', startIndex = index + 1)
            if (end > index + 1) {
                appendHighlighted(
                    text = content.substring(index + 1, end),
                    query = highlightQuery,
                    style = context.spanStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Yellow.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Medium
                        )
                    )
                )
                index = end + 1
                continue
            }
        }

        val mentionMatch = mentionPattern.find(content, index)
        if (mentionMatch != null && mentionMatch.range.first == index) {
            appendHighlighted(
                text = mentionMatch.value,
                query = highlightQuery,
                style = context.spanStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        background = Cyan.copy(alpha = 0.3f)
                    )
                )
            )
            index = mentionMatch.range.last + 1
            continue
        }

        val nextIndex = findNextTokenIndex(content, index + 1)
        appendHighlighted(
            text = content.substring(index, nextIndex),
            query = highlightQuery,
            style = context.spanStyle()
        )
        index = nextIndex
    }
}

private fun findNextTokenIndex(content: String, startIndex: Int): Int {
    for (i in startIndex until content.length) {
        if (content[i] in markdownTokenChars) return i
    }
    return content.length
}

private fun parseDelimited(content: String, startIndex: Int, delimiter: String): Pair<String, Int>? {
    if (!content.startsWith(delimiter, startIndex)) return null
    val contentStart = startIndex + delimiter.length
    if (contentStart >= content.length) return null
    val endIndex = content.indexOf(delimiter, startIndex = contentStart)
    if (endIndex <= contentStart) return null
    return content.substring(contentStart, endIndex) to (endIndex + delimiter.length)
}

private fun parseLinkAt(content: String, startIndex: Int): LinkToken? {
    if (content.getOrNull(startIndex) != '[') return null
    val labelEnd = content.indexOf(']', startIndex = startIndex + 1)
    if (labelEnd <= startIndex + 1 || content.getOrNull(labelEnd + 1) != '(') return null
    val urlEnd = content.indexOf(')', startIndex = labelEnd + 2)
    if (urlEnd <= labelEnd + 2) return null

    val label = content.substring(startIndex + 1, labelEnd)
    val url = content.substring(labelEnd + 2, urlEnd).trim()
    if (url.isBlank()) return null

    return LinkToken(label = label, url = url, endIndex = urlEnd + 1)
}

private fun AnnotatedString.Builder.appendHighlighted(
    text: String,
    query: String,
    style: SpanStyle = SpanStyle()
) {
    if (text.isEmpty()) return

    if (query.isBlank()) {
        appendStyled(text, style)
        return
    }

    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    var start = 0

    while (start < text.length) {
        val index = lowerText.indexOf(lowerQuery, startIndex = start)
        if (index < 0) {
            appendStyled(text.substring(start), style)
            break
        }

        if (index > start) {
            appendStyled(text.substring(start, index), style)
        }

        withStyle(style.merge(SpanStyle(background = Yellow.copy(alpha = 0.6f), fontWeight = FontWeight.Bold))) {
            append(text.substring(index, index + query.length))
        }
        start = index + query.length
    }
}

private fun AnnotatedString.Builder.appendStyled(
    text: String,
    style: SpanStyle
) {
    if (text.isEmpty()) return
    if (style == SpanStyle()) {
        append(text)
        return
    }
    withStyle(style) {
        append(text)
    }
}
