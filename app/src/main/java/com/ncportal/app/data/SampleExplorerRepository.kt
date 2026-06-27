package com.ncportal.app.data

import com.ncportal.app.model.Entry
import com.ncportal.app.model.EntryType

/**
 * In-memory placeholder data so the UI shell is runnable before a real backend
 * exists. Replace with a network/storage-backed [ExplorerRepository].
 */
class SampleExplorerRepository : ExplorerRepository {

    override fun root(): Entry = ROOT

    private companion object {
        private fun folder(id: String, name: String, children: List<Entry>) =
            Entry(id = id, name = name, type = EntryType.FOLDER, children = children)

        private fun file(id: String, name: String, sizeBytes: Long, modifiedAt: Long) =
            Entry(id = id, name = name, type = EntryType.FILE, sizeBytes = sizeBytes, modifiedAt = modifiedAt)

        val ROOT = folder(
            id = "root",
            name = "NC Portal",
            children = listOf(
                folder(
                    id = "documents",
                    name = "문서",
                    children = listOf(
                        file("doc-readme", "README.md", 2_048, 1_780_300_000_000),
                        file("doc-spec", "사양서.pdf", 482_133, 1_781_500_000_000),
                        folder(
                            id = "reports",
                            name = "보고서",
                            children = listOf(
                                file("rep-q1", "2026-1분기.xlsx", 91_200, 1_775_000_000_000),
                                file("rep-q2", "2026-2분기.xlsx", 88_400, 1_779_000_000_000),
                            ),
                        ),
                    ),
                ),
                folder(
                    id = "media",
                    name = "미디어",
                    children = listOf(
                        file("img-01", "표지.png", 1_204_000, 1_782_000_000_000),
                        file("vid-01", "소개영상.mp4", 24_800_000, 1_781_900_000_000),
                        file("aud-01", "안내음성.mp3", 3_120_000, 1_780_800_000_000),
                    ),
                ),
                folder(id = "archive", name = "압축본", children = listOf(
                    file("arc-01", "백업.zip", 58_900_000, 1_774_000_000_000),
                )),
                folder(id = "empty", name = "빈 폴더", children = emptyList()),
                file("note", "메모.txt", 312, 1_781_700_000_000),
            ),
        )
    }
}
