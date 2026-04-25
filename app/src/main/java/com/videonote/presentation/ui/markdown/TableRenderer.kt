package com.videonote.presentation.ui.markdown

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.text.Spanned
import android.text.style.ImageSpan
import android.view.WindowManager
import com.videonote.util.Logger
import java.util.UUID
import java.util.regex.Pattern

/**
 * Markdown表格渲染工具类
 * 将Markdown表格转换为图片进行渲染，适配屏幕宽度
 */
object TableRenderer {

    private const val TAG = "TableRenderer"
    private const val DEFAULT_PADDING = 16
    private const val CELL_PADDING = 12
    private const val ROW_HEIGHT = 50
    private const val TEXT_SIZE = 28f
    private const val STROKE_WIDTH = 2f
    private const val MAX_SCREEN_WIDTH_RATIO = 0.95f  // 最大宽度为屏幕宽度的95%

    /**
     * 表格占位符数据类
     */
    data class TablePlaceholder(
        val placeholder: String,
        val markdown: String,
        val drawable: BitmapDrawable
    )

    /**
     * 获取屏幕宽度
     */
    @Suppress("DEPRECATION")
    private fun getScreenWidth(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val point = android.graphics.Point()
        @Suppress("DEPRECATION")
        val display = wm.defaultDisplay
        @Suppress("DEPRECATION")
        display.getSize(point)
        return point.x
    }

    /**
     * 检测文本中是否包含Markdown表格
     */
    fun containsTable(text: String): Boolean {
        // 简单检测：查找包含 | 的行
        val lines = text.lines()
        var pipeCount = 0
        for (line in lines) {
            if (line.contains("|")) {
                pipeCount++
                if (pipeCount >= 2) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * 从Markdown中提取表格并替换为占位符
     */
    fun extractTables(markdown: String): Pair<String, List<TableData>> {
        val tables = mutableListOf<TableData>()
        val lines = markdown.lines()
        val result = mutableListOf<String>()
        var i = 0

        while (i < lines.size) {
            val line = lines[i].trim()

            // 检测表格开始（包含 | 且不是代码块）
            if (line.contains("|") && !line.startsWith("```")) {
                val tableLines = mutableListOf<String>()
                tableLines.add(line)
                i++

                // 收集表格的后续行
                while (i < lines.size) {
                    val nextLine = lines[i].trim()
                    if (nextLine.contains("|") || nextLine.contains("|---")) {
                        tableLines.add(nextLine)
                        i++
                    } else if (nextLine.isEmpty()) {
                        i++
                        continue
                    } else {
                        break
                    }
                }

                // 验证是否是有效表格（至少2行，包含分隔符）
                if (tableLines.size >= 2 && tableLines.any { it.contains("---") }) {
                    val tableMarkdown = tableLines.joinToString("\n")
                    val placeholder = "TABLE_PLACEHOLDER_${tables.size}_END"
                    tables.add(TableData(placeholder, tableMarkdown))
                    result.add(placeholder)
                    continue
                } else {
                    // 不是有效表格，还原原始行
                    result.addAll(tableLines)
                    continue
                }
            }

            result.add(lines[i])
            i++
        }

        return Pair(result.joinToString("\n"), tables)
    }

    /**
     * 解析表格数据
     */
    private fun parseTable(markdown: String): ParsedTable {
        val lines = markdown.lines().filter { it.trim().isNotEmpty() }
        val rows = mutableListOf<List<String>>()

        for (line in lines) {
            if (line.contains("---") || line.trim().all { it == '|' || it == '-' || it == ' ' }) {
                // 分隔符行，跳过
                continue
            }

            // 解析表格行
            val cells = line.split("|")
                .filter { it.trim().isNotEmpty() }
                .map { it.trim() }
            if (cells.isNotEmpty()) {
                rows.add(cells)
            }
        }

        return ParsedTable(rows)
    }

    /**
     * 渲染表格为Bitmap，适配屏幕宽度
     */
    fun renderTable(
        context: Context,
        tableData: TableData,
        textColor: Int,
        backgroundColor: Int,
        borderColor: Int
    ): BitmapDrawable? {
        return try {
            val screenWidth = getScreenWidth(context)
            val maxWidth = (screenWidth * MAX_SCREEN_WIDTH_RATIO).toInt()

            val parsed = parseTable(tableData.markdown)
            if (parsed.rows.isEmpty()) {
                Logger.w(TAG, "Empty table: ${tableData.markdown}")
                return null
            }

            // 计算尺寸
            val paint = android.graphics.Paint().apply {
                this.textSize = TEXT_SIZE
                color = textColor
                isAntiAlias = true
            }

            val columnCount = parsed.rows[0].size
            val rowCount = parsed.rows.size

            // 计算每列宽度
            val columnWidths = mutableMapOf<Int, Float>()
            for (row in parsed.rows) {
                for ((col, cell) in row.withIndex()) {
                    val width = paint.measureText(cell) + CELL_PADDING * 2
                    columnWidths[col] = maxOf(columnWidths[col] ?: 0f, width)
                }
            }

            var totalWidth = columnWidths.values.sum() + DEFAULT_PADDING * 2 + (columnCount - 1) * STROKE_WIDTH
            var totalHeight = rowCount * ROW_HEIGHT + DEFAULT_PADDING * 2 + rowCount * STROKE_WIDTH

            // 如果超出屏幕宽度，按比例缩放
            var scale = 1f
            if (totalWidth > maxWidth) {
                scale = maxWidth.toFloat() / totalWidth
                totalWidth = maxWidth.toFloat()
                totalHeight *= scale
            }

            // 创建Bitmap
            val bitmap = Bitmap.createBitmap(
                totalWidth.toInt(),
                totalHeight.toInt(),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)

            // 应用缩放
            if (scale != 1f) {
                canvas.scale(scale, scale)
            }

            // 绘制背景
            canvas.drawColor(backgroundColor)

            // 绘制表格
            var y = DEFAULT_PADDING.toFloat()

            val strokePaint = android.graphics.Paint().apply {
                color = borderColor
                strokeWidth = STROKE_WIDTH
                style = android.graphics.Paint.Style.STROKE
                isAntiAlias = true
            }

            val fillPaint = android.graphics.Paint().apply {
                color = backgroundColor.copy(alpha = 50)
                style = android.graphics.Paint.Style.FILL
            }

            for ((rowIndex, row) in parsed.rows.withIndex()) {
                var x = DEFAULT_PADDING.toFloat()

                for ((colIndex, cell) in row.withIndex()) {
                    val cellWidth = columnWidths[colIndex] ?: 100f

                    // 绘制单元格边框
                    val rect = android.graphics.RectF(
                        x, y, x + cellWidth, y + ROW_HEIGHT
                    )

                    // 表头添加背景
                    if (rowIndex == 0) {
                        canvas.drawRect(rect, fillPaint)
                    }

                    canvas.drawRect(rect, strokePaint)

                    // 绘制文字
                    val textX = x + CELL_PADDING
                    val textY = y + ROW_HEIGHT / 2 + TEXT_SIZE / 3
                    canvas.drawText(cell, textX, textY, paint)

                    x += cellWidth + STROKE_WIDTH
                }

                y += ROW_HEIGHT + STROKE_WIDTH
            }

            BitmapDrawable(context.resources, bitmap).apply {
                setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to render table", e)
            null
        }
    }

    /**
     * 预处理Markdown，将表格转换为占位符
     */
    fun preprocessMarkdown(
        context: Context,
        markdown: String,
        textColor: Int,
        backgroundColor: Int,
        borderColor: Int
    ): Pair<String, List<TablePlaceholder>> {
        val placeholders = mutableListOf<TablePlaceholder>()

        // 提取表格
        val (processedMarkdown, tables) = extractTables(markdown)

        // 渲染每个表格
        for (table in tables) {
            val drawable = renderTable(context, table, textColor, backgroundColor, borderColor)
            if (drawable != null) {
                placeholders.add(TablePlaceholder(table.placeholder, table.markdown, drawable))
            }
        }

        return Pair(processedMarkdown, placeholders)
    }

    /**
     * 应用表格图片到Spanned文本
     */
    fun applyTableSpans(
        spanned: Spanned,
        placeholders: List<TablePlaceholder>
    ): Spanned {
        val text = spanned.toString()
        val result = android.text.SpannableStringBuilder(spanned)

        // 从后往前替换，避免位置偏移
        for (placeholder in placeholders.sortedByDescending { text.indexOf(it.placeholder) }) {
            val startIndex = text.indexOf(placeholder.placeholder)
            if (startIndex >= 0) {
                val endIndex = startIndex + placeholder.placeholder.length

                // 替换占位符为换行符+ImageSpan+换行符
                result.replace(startIndex, endIndex, "\n \n")

                val imageSpan = ImageSpan(placeholder.drawable, ImageSpan.ALIGN_CENTER)
                result.setSpan(
                    imageSpan,
                    startIndex + 1,
                    startIndex + 2,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        return result
    }

    /**
     * 解析后的表格数据
     */
    data class ParsedTable(
        val rows: List<List<String>>
    )

    /**
     * 表格数据
     */
    data class TableData(
        val placeholder: String,
        val markdown: String
    )

    /**
     * 扩展函数：颜色复制
     */
    private fun Int.copy(alpha: Int = 0xFF): Int {
        return (this and 0x00FFFFFF) or (alpha shl 24)
    }
}
