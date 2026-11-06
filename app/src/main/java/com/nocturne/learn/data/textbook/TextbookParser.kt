package com.nocturne.learn.data.textbook

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Xml
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser

/**
 * 第一版只处理有文字层的教材：
 * PDF 文字层、DOCX、PPTX、TXT、Markdown。
 * 扫描版 PDF 与老式 .doc/.ppt 会给出明确提示。
 */
class TextbookParser(private val context: Context) {

    suspend fun copyToPrivate(uri: Uri): ImportedBookFile = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var displayName = ""
        var size = 0L
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIndex >= 0) displayName = cursor.getString(nameIndex).orEmpty()
                        if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
                    }
                }
        }
        if (displayName.isBlank()) displayName = uri.lastPathSegment.orEmpty().substringAfterLast('/')
        val format = BookFormat.from(displayName)
            ?: throw TextbookParseException("只支持 PDF、DOCX、PPTX、TXT、Markdown 文件")
        if (size > MAX_FILE_BYTES) {
            throw TextbookParseException("文件超过 ${MAX_FILE_BYTES / 1024 / 1024} MB，请先拆分后再导入")
        }

        val folder = File(context.filesDir, "imports").apply { mkdirs() }
        folder.listFiles()?.filter { it.isFile && System.currentTimeMillis() - it.lastModified() > 2L * 24 * 60 * 60 * 1000 }
            ?.forEach { it.delete() }

        val file = File(folder, "${UUID.randomUUID()}.${format.extension}")
        resolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        } ?: throw TextbookParseException("无法读取这个文件，请换一个文件试试")
        if (file.length() == 0L) {
            file.delete()
            throw TextbookParseException("这个文件是空的")
        }
        if (file.length() > MAX_FILE_BYTES) {
            file.delete()
            throw TextbookParseException("文件超过 ${MAX_FILE_BYTES / 1024 / 1024} MB，请先拆分后再导入")
        }
        ImportedBookFile(file = file, displayName = displayName, format = format)
    }

    suspend fun parse(
        book: ImportedBookFile,
        onProgress: (String, Float) -> Unit,
    ): ParsedBook = withContext(Dispatchers.IO) {
        val title = book.displayName.substringBeforeLast('.').trim().ifBlank { "未命名教材" }
        when (book.format) {
            BookFormat.PDF -> parsePdf(book.file, title, onProgress)
            BookFormat.DOCX -> parseDocx(book.file, title)
            BookFormat.PPTX -> parsePptx(book.file, title)
            BookFormat.TXT -> parseText(book.file, title, markdown = false)
            BookFormat.MARKDOWN -> parseText(book.file, title, markdown = true)
        }
    }

    fun delete(book: ImportedBookFile?) {
        runCatching { book?.file?.delete() }
    }

    private fun parsePdf(file: File, title: String, onProgress: (String, Float) -> Unit): ParsedBook {
        PDFBoxResourceLoader.init(context.applicationContext)
        val document = try {
            PDDocument.load(file)
        } catch (error: Exception) {
            throw TextbookParseException("PDF 打不开，可能已加密或损坏")
        }
        document.use { doc ->
            val pages = doc.numberOfPages
            if (pages == 0) throw TextbookParseException("PDF 里没有页面")
            if (pages > MAX_PAGES) {
                throw TextbookParseException("这个 PDF 有 $pages 页，第一版一次最多处理 $MAX_PAGES 页，请先拆分")
            }
            val stripper = PDFTextStripper().apply { sortByPosition = true }
            val pageTexts = ArrayList<String>(pages)
            for (page in 1..pages) {
                stripper.startPage = page
                stripper.endPage = page
                val text = runCatching { stripper.getText(doc) }.getOrDefault("")
                pageTexts += normalize(text)
                onProgress("正在读取第 $page / $pages 页", page.toFloat() / pages)
            }
            val totalChars = pageTexts.sumOf { it.length }
            if (totalChars < pages * 30) {
                throw TextbookParseException(
                    "这看起来是扫描版 PDF：几乎没有可提取的文字。第一版暂不支持扫描件，请换文字版 PDF。",
                )
            }

            val sections = mutableListOf<DocumentSection>()
            var startPage = 1
            var pageInChunk = 0
            val buffer = StringBuilder()
            pageTexts.forEachIndexed { index, text ->
                if (text.isBlank()) return@forEachIndexed
                if (buffer.isEmpty()) startPage = index + 1
                buffer.append("\n").append(text)
                pageInChunk++
                if (buffer.length >= PDF_CHUNK_CHARS || pageInChunk >= PDF_PAGES_PER_CHUNK) {
                    sections += DocumentSection(
                        title = if (startPage == index + 1) "第 ${startPage} 页" else "第 $startPage–${index + 1} 页",
                        text = buffer.toString(),
                        sourceRef = if (startPage == index + 1) "第 ${startPage} 页" else "第 $startPage–${index + 1} 页",
                    )
                    buffer.clear()
                    pageInChunk = 0
                }
            }
            if (buffer.isNotBlank()) {
                sections += DocumentSection(
                    title = "第 $startPage–$pages 页",
                    text = buffer.toString(),
                    sourceRef = "第 $startPage–$pages 页",
                )
            }
            if (sections.isEmpty()) throw TextbookParseException("PDF 里没有可用的文字")
            return ParsedBook(title, BookFormat.PDF, sections, pageCount = pages)
        }
    }

    private fun parseDocx(file: File, title: String): ParsedBook {
        val paragraphs = mutableListOf<DocParagraph>()
        ZipFile(file).use { zip ->
            val entry = zip.getEntry("word/document.xml")
                ?: throw TextbookParseException("Word 文件缺少正文，可能不是标准的 .docx")
            zip.getInputStream(entry).use { stream ->
                paragraphs += readDocxParagraphs(stream)
            }
        }
        val nonEmpty = paragraphs.filter { it.text.isNotBlank() }
        if (nonEmpty.isEmpty()) throw TextbookParseException("Word 里没有可读取的文字")

        val hasHeadings = nonEmpty.any { it.headingLevel > 0 }
        val sections = if (hasHeadings) {
            sectionsFromHeadings(nonEmpty, "Word")
        } else {
            chunkParagraphs(nonEmpty, "文档")
        }
        return ParsedBook(title, BookFormat.DOCX, sections)
    }

    private fun parsePptx(file: File, title: String): ParsedBook {
        val slides = mutableListOf<DocumentSection>()
        ZipFile(file).use { zip ->
            val entries = zip.entries().asSequence()
                .filter { it.name.matches(Regex("ppt/slides/slide\\d+\\.xml")) }
                .sortedBy { it.name.substringAfter("slide").substringBefore(".").toIntOrNull() ?: 0 }
                .toList()
            if (entries.isEmpty()) throw TextbookParseException("PPT 里没有幻灯片")
            entries.forEachIndexed { index, entry ->
                val texts = mutableListOf<String>()
                zip.getInputStream(entry).use { stream -> texts += readPptxTexts(stream) }
                val text = texts.filter { it.isNotBlank() }.joinToString("\n")
                if (text.isNotBlank()) {
                    slides += DocumentSection(
                        title = texts.firstOrNull { it.isNotBlank() }?.take(40) ?: "第 ${index + 1} 张幻灯片",
                        text = text,
                        sourceRef = "第 ${index + 1} 张幻灯片",
                    )
                }
            }
        }
        if (slides.isEmpty()) throw TextbookParseException("PPT 里没有可读取的文字")
        return ParsedBook(title, BookFormat.PPTX, groupSmallSections(slides, "幻灯片"))
    }

    private fun parseText(file: File, title: String, markdown: Boolean): ParsedBook {
        val text = file.readText(Charsets.UTF_8).removePrefix("\uFEFF")
        if (text.isBlank()) throw TextbookParseException("文件里没有文字")
        val sections = if (markdown) {
            splitMarkdown(text)
        } else {
            chunkPlainText(text)
        }
        if (sections.isEmpty()) throw TextbookParseException("文件里没有可用的文字")
        return ParsedBook(title, if (markdown) BookFormat.MARKDOWN else BookFormat.TXT, sections)
    }

    private fun readDocxParagraphs(stream: InputStream): List<DocParagraph> {
        val result = mutableListOf<DocParagraph>()
        val parser = Xml.newPullParser()
        parser.setInput(stream, "UTF-8")
        var event = parser.eventType
        var text = StringBuilder()
        var headingLevel = 0
        var inText = false
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "p" -> {
                        text = StringBuilder()
                        headingLevel = 0
                    }
                    "pStyle" -> {
                        val style = parser.getAttributeValue(null, "val")
                            ?: parser.getAttributeValue(W_NS, "val").orEmpty()
                        headingLevel = headingLevelOf(style)
                    }
                    "t" -> inText = true
                }
                XmlPullParser.TEXT -> if (inText && parser.text != null) text.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name) {
                    "t" -> inText = false
                    "p" -> {
                        val line = normalize(text.toString())
                        if (line.isNotBlank()) result += DocParagraph(line, headingLevel)
                    }
                }
            }
            event = parser.next()
        }
        return result
    }

    private fun readPptxTexts(stream: InputStream): List<String> {
        val result = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(stream, "UTF-8")
        var event = parser.eventType
        var current = StringBuilder()
        var inText = false
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "p" -> current = StringBuilder()
                    "t" -> inText = true
                }
                XmlPullParser.TEXT -> if (inText && parser.text != null) current.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name) {
                    "t" -> inText = false
                    "p" -> {
                        val line = normalize(current.toString())
                        if (line.isNotBlank()) result += line
                    }
                }
            }
            event = parser.next()
        }
        return result
    }

    private fun sectionsFromHeadings(paragraphs: List<DocParagraph>, fallback: String): List<DocumentSection> {
        val result = mutableListOf<DocumentSection>()
        var heading = ""
        val buffer = StringBuilder()
        var order = 0
        fun flush() {
            if (buffer.isBlank()) return
            val sectionTitle = heading.ifBlank { "$fallback · 第 ${order + 1} 部分" }
            result += DocumentSection(sectionTitle, buffer.toString(), sectionTitle)
            buffer.clear()
            order++
        }
        paragraphs.forEach { paragraph ->
            if (paragraph.headingLevel in 1..3) {
                flush()
                heading = paragraph.text.take(60)
            } else {
                buffer.append("\n").append(paragraph.text)
                if (buffer.length >= MAX_SECTION_CHARS) flush()
            }
        }
        flush()
        return result.ifEmpty { chunkParagraphs(paragraphs, fallback) }
    }

    private fun chunkParagraphs(paragraphs: List<DocParagraph>, fallback: String): List<DocumentSection> {
        val result = mutableListOf<DocumentSection>()
        var buffer = StringBuilder()
        var part = 0
        fun flush() {
            if (buffer.isBlank()) return
            part++
            val source = "$fallback · 第 $part 部分"
            result += DocumentSection(source, buffer.toString(), source)
            buffer = StringBuilder()
        }
        paragraphs.forEach { paragraph ->
            buffer.append("\n").append(paragraph.text)
            if (buffer.length >= MAX_SECTION_CHARS) flush()
        }
        flush()
        return result
    }

    private fun splitMarkdown(text: String): List<DocumentSection> {
        val lines = text.lines()
        val result = mutableListOf<DocumentSection>()
        var heading = "开头"
        val buffer = StringBuilder()
        fun flush() {
            if (buffer.isBlank()) return
            result += DocumentSection(heading, buffer.toString(), heading)
            buffer.clear()
        }
        lines.forEach { line ->
            val match = Regex("^#{1,6}\\s+(.+)$").find(line.trim())
            if (match != null) {
                flush()
                heading = match.groupValues[1].trim().take(60)
            } else {
                buffer.append("\n").append(line)
                if (buffer.length >= MAX_SECTION_CHARS) flush()
            }
        }
        flush()
        return result
    }

    private fun chunkPlainText(text: String): List<DocumentSection> {
        val result = mutableListOf<DocumentSection>()
        var index = 0
        var part = 0
        while (index < text.length) {
            val end = (index + MAX_SECTION_CHARS).coerceAtMost(text.length)
            val slice = text.substring(index, end)
            val cut = if (end < text.length) {
                val lastBreak = slice.lastIndexOf('\n').takeIf { it > slice.length / 2 } ?: slice.length
                index + lastBreak
            } else end
            part++
            val source = "第 $part 部分"
            result += DocumentSection(source, text.substring(index, cut).trim(), source)
            index = if (cut <= index) end else cut
        }
        return result
    }

    private fun groupSmallSections(sections: List<DocumentSection>, label: String): List<DocumentSection> {
        val result = mutableListOf<DocumentSection>()
        var buffer = StringBuilder()
        var title = ""
        var source = ""
        fun flush() {
            if (buffer.isBlank()) return
            result += DocumentSection(title.ifBlank { "$label · 第 ${result.size + 1} 组" }, buffer.toString(), source)
            buffer = StringBuilder()
        }
        sections.forEach { section ->
            if (buffer.isEmpty()) {
                title = section.title
                source = section.sourceRef
            }
            buffer.append("\n").append(section.text)
            if (buffer.length >= MAX_SECTION_CHARS) flush()
        }
        flush()
        return result
    }

    private fun normalize(text: String): String = text
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .replace(Regex("[\\t\\u000B\\f]+"), " ")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()

    private fun headingLevelOf(style: String): Int {
        if (style.isBlank()) return 0
        if (style.contains("标题", ignoreCase = true)) {
            return Regex("\\d+").find(style)?.value?.toIntOrNull() ?: 1
        }
        val match = Regex("heading\\s*(\\d+)", RegexOption.IGNORE_CASE).find(style) ?: return 0
        return match.groupValues[1].toIntOrNull() ?: 1
    }

    private data class DocParagraph(val text: String, val headingLevel: Int)

    private companion object {
        const val MAX_FILE_BYTES = 80L * 1024 * 1024
        const val MAX_PAGES = 400
        const val MAX_SECTION_CHARS = 5000
        const val PDF_CHUNK_CHARS = 5000
        const val PDF_PAGES_PER_CHUNK = 6
        const val W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
    }
}
