package com.nocturne.learn.data.textbook

import java.io.File

enum class BookFormat(val label: String, val extension: String) {
    PDF("PDF", "pdf"),
    DOCX("Word", "docx"),
    PPTX("PPT", "pptx"),
    TXT("文本", "txt"),
    MARKDOWN("Markdown", "md"),
    ;

    companion object {
        fun from(fileName: String): BookFormat? {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            return entries.firstOrNull { it.extension == ext }
        }
    }
}

/** 解析后的一段教材内容。AI 按段拆知识点，段同时保留来源。 */
data class DocumentSection(
    val title: String,
    val text: String,
    val sourceRef: String,
)

data class ParsedBook(
    val title: String,
    val format: BookFormat,
    val sections: List<DocumentSection>,
    val pageCount: Int = 0,
) {
    val charCount: Int get() = sections.sumOf { it.text.length }
}

class TextbookParseException(message: String) : Exception(message)

data class ImportedBookFile(
    val file: File,
    val displayName: String,
    val format: BookFormat,
)
