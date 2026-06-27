package com.ncportal.app.ui

import com.ncportal.app.model.Entry

/** First `# ` heading if present, else filename without extension. */
fun postTitle(entry: Entry): String {
    val heading = entry.content
        ?.lineSequence()
        ?.map { it.trimStart() }
        ?.firstOrNull { it.startsWith("# ") }
        ?.removePrefix("# ")
        ?.trim()
    return heading?.takeIf { it.isNotEmpty() } ?: entry.name.substringBeforeLast('.')
}

/** First non-heading / non-structural text line, inline markers stripped. */
fun postExcerpt(content: String?): String {
    val line = content
        ?.lineSequence()
        ?.map { it.trim() }
        ?.firstOrNull { l ->
            l.isNotEmpty() &&
                !l.startsWith("#") && !l.startsWith("```") &&
                !l.startsWith(">") && !l.startsWith("- ") && !l.startsWith("* ") &&
                !l.startsWith("---") && !Regex("""^\d+\.\s""").containsMatchIn(l)
        } ?: return ""
    return line
        .replace(Regex("""\*\*(.+?)\*\*"""), "$1")
        .replace(Regex("""\*(.+?)\*"""), "$1")
        .replace(Regex("""`(.+?)`"""), "$1")
        .replace(Regex("""\[(.+?)]\(.+?\)"""), "$1")
}

/** Number of .md posts in a board (folder). */
fun postCount(board: Entry): Int = board.children.count { it.isPost }

/** Newest modifiedAt among posts in a board; 0 if none. */
fun latestPostMillis(board: Entry): Long =
    board.children.filter { it.isPost }.maxOfOrNull { it.modifiedAt } ?: 0L
