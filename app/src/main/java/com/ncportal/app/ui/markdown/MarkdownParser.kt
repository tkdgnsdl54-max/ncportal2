package com.ncportal.app.ui.markdown

private val HEADING = Regex("""^(#{1,6})\s+(.*)$""")
private val ORDERED = Regex("""^\d+\.\s+(.*)$""")
private val RULE = Regex("""^(-{3,}|\*{3,}|_{3,})$""")

fun parseMarkdown(text: String): List<MdBlock> =
    parseLines(text.split("\n").map { it.removeSuffix("\r") })

private fun parseLines(lines: List<String>): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val t = line.trim()
        when {
            t.startsWith("```") -> {                             // fence FIRST (verbatim)
                val sb = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    if (sb.isNotEmpty()) sb.append('\n')
                    sb.append(lines[i])
                    i++
                }
                if (i < lines.size) i++                          // consume closing fence
                blocks += MdBlock.CodeBlock(sb.toString())
            }
            t.isEmpty() -> i++                                   // paragraph separator
            RULE.matches(t) -> { blocks += MdBlock.Rule; i++ }
            HEADING.matches(t) -> {
                val m = HEADING.find(t)!!
                blocks += MdBlock.Heading(
                    m.groupValues[1].length.coerceAtMost(3),
                    m.groupValues[2].trim(),
                )
                i++
            }
            t.startsWith(">") -> {
                val inner = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    inner += lines[i].trim().removePrefix(">").removePrefix(" ")
                    i++
                }
                blocks += MdBlock.Quote(parseLines(inner))
            }
            isBullet(t) -> {
                val items = mutableListOf<String>()
                while (i < lines.size && isBullet(lines[i].trim())) {
                    items += lines[i].trim().substring(2).trim()
                    i++
                }
                blocks += MdBlock.BulletList(items)
            }
            ORDERED.matches(t) -> {
                val items = mutableListOf<String>()
                while (i < lines.size && ORDERED.matches(lines[i].trim())) {
                    items += ORDERED.find(lines[i].trim())!!.groupValues[1].trim()
                    i++
                }
                blocks += MdBlock.OrderedList(items)
            }
            else -> {                                            // paragraph: join consecutive lines
                val para = mutableListOf<String>()
                while (i < lines.size &&
                    lines[i].trim().isNotEmpty() &&
                    !startsNewBlock(lines[i].trim())
                ) {
                    para += lines[i].trim()
                    i++
                }
                if (para.isNotEmpty()) {
                    blocks += MdBlock.Paragraph(para.joinToString("\n"))
                } else {
                    i++ // unrecognized line — skip to prevent infinite loop
                }
            }
        }
    }
    return blocks
}

private fun isBullet(t: String): Boolean =
    t.length >= 2 && (t[0] == '-' || t[0] == '*' || t[0] == '+') && t[1] == ' '

private fun startsNewBlock(t: String): Boolean =
    t.startsWith("```") || t.startsWith("#") || t.startsWith(">") ||
        isBullet(t) || ORDERED.matches(t) || RULE.matches(t)
