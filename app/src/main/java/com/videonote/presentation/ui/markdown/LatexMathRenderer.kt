package com.videonote.presentation.ui.markdown

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.view.WindowManager
import com.videonote.util.Logger
import java.util.UUID
import java.util.regex.Pattern

/**
 * LaTeX数学公式渲染工具类
 *
 * 支持两种格式：
 * - 行内公式: $E = mc^2$
 * - 块级公式: $$\frac{a}{b} = \frac{c}{d}$$
 *
 * 适配屏幕宽度，公式不超过屏幕95%宽度
 */
object LatexMathRenderer {

    private const val TAG = "LatexMathRenderer"
    private const val MAX_SCREEN_WIDTH_RATIO = 0.95f  // 最大宽度为屏幕宽度的95%

    // 行内公式模式: $...$ (避免匹配$$)
    private val INLINE_MATH_PATTERN: Pattern = Pattern.compile(
        """(?<!\$)\$([^$\n]+?)\$(?!$)"""
    )

    // 块级公式模式: $$...$$
    private val BLOCK_MATH_PATTERN: Pattern = Pattern.compile(
        """\$\$([^$]+?)\$\$""",
        Pattern.DOTALL
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
     * 渲染LaTeX公式为BitmapDrawable，适配屏幕宽度
     *
     * @param context Android上下文
     * @param latex LaTeX公式字符串
     * @param textSize 文字大小（sp）
     * @param textColor 文字颜色（ARGB）
     * @return 渲染后的BitmapDrawable，失败返回null
     */
    fun renderFormula(
        context: Context,
        latex: String,
        textSize: Float = 32f,
        textColor: Int = 0xFF000000.toInt()
    ): BitmapDrawable? {
        return try {
            // 初始化 JLaTeXMath
            ru.noties.jlatexmath.JLatexMathAndroid.init(context)

            // 使用 JLaTeXMath 库渲染公式
            val drawable = ru.noties.jlatexmath.JLatexMathDrawable.builder(latex.trim())
                .textSize(textSize)
                .color(textColor)
                .build()

            // 获取原始尺寸
            val width = drawable.intrinsicWidth
            val height = drawable.intrinsicHeight

            if (width <= 0 || height <= 0) {
                Logger.e(TAG, "Invalid drawable size: ${width}x${height} for: $latex")
                return null
            }

            // 检查是否需要缩放以适配屏幕宽度
            val screenWidth = getScreenWidth(context)
            val maxWidth = (screenWidth * MAX_SCREEN_WIDTH_RATIO).toInt()
            val finalWidth = if (width > maxWidth) maxWidth else width
            val scale = finalWidth.toFloat() / width
            val finalHeight = (height * scale).toInt()

            // 创建适配后的Bitmap
            val bitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 应用缩放
            canvas.scale(scale, scale)

            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)

            BitmapDrawable(context.resources, bitmap).apply {
                setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to render LaTeX formula: $latex", e)
            null
        }
    }

    /**
     * 预处理Markdown文本，将LaTeX公式替换为占位符
     *
     * @param context Android上下文
     * @param markdown 原始Markdown文本
     * @param textColor 文字颜色
     * @return Pair(处理后的文本, 渲染好的图片映射列表)
     */
    fun preprocessMarkdown(
        context: Context,
        markdown: String,
        textColor: Int = 0xFF000000.toInt()
    ): Pair<String, List<MathPlaceholder>> {
        val placeholders = mutableListOf<MathPlaceholder>()
        var result = markdown
        var offset = 0

        // 先处理块级公式 $$...$$
        val blockMatcher = BLOCK_MATH_PATTERN.matcher(result)
        while (blockMatcher.find()) {
            val latex = blockMatcher.group(1)!!.trim()
            val start = blockMatcher.start() + offset
            val end = blockMatcher.end() + offset
            val length = end - start

            // 渲染公式
            val drawable = renderFormula(context, latex, 40f, textColor)
            if (drawable != null) {
                val placeholder = "MATH_BLOCK_${UUID.randomUUID()}"
                placeholders.add(MathPlaceholder(placeholder, latex, drawable, true))
                result = result.substring(0, start) + placeholder + result.substring(end)
                offset += placeholder.length - length
            }

            // 重新匹配，因为字符串已经改变
            blockMatcher.reset(result)
        }

        // 再处理行内公式 $...$
        val inlineMatcher = INLINE_MATH_PATTERN.matcher(result)
        while (inlineMatcher.find()) {
            val latex = inlineMatcher.group(1)!!.trim()
            val start = inlineMatcher.start() + offset
            val end = inlineMatcher.end() + offset
            val length = end - start

            // 渲染公式
            val drawable = renderFormula(context, latex, 28f, textColor)
            if (drawable != null) {
                val placeholder = "MATH_INLINE_${UUID.randomUUID()}"
                placeholders.add(MathPlaceholder(placeholder, latex, drawable, false))
                result = result.substring(0, start) + placeholder + result.substring(end)
                offset += placeholder.length - length
            }

            // 重新匹配，因为字符串已经改变
            inlineMatcher.reset(result)
        }

        return Pair(result, placeholders)
    }

    /**
     * 对渲染后的Spanned文本应用数学公式图片
     *
     * @param spanned Markwon渲染后的文本
     * @param placeholders 数学公式占位符列表
     * @return 应用数学公式后的SpannedString
     */
    fun applyMathSpans(
        spanned: Spanned,
        placeholders: List<MathPlaceholder>
    ): Spanned {
        val text = spanned.toString()
        val result = SpannableStringBuilder(spanned)

        // 从后往前替换，避免位置偏移
        for (placeholder in placeholders.sortedByDescending { text.indexOf(it.placeholder) }) {
            val startIndex = text.indexOf(placeholder.placeholder)
            if (startIndex >= 0) {
                val endIndex = startIndex + placeholder.placeholder.length

                // 替换占位符为空格+ImageSpan
                result.replace(startIndex, endIndex, " ")

                val imageSpan = ImageSpan(placeholder.drawable, ImageSpan.ALIGN_BASELINE)
                result.setSpan(
                    imageSpan,
                    startIndex,
                    startIndex + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        return result
    }

    /**
     * 数学公式占位符数据类
     */
    data class MathPlaceholder(
        val placeholder: String,
        val latex: String,
        val drawable: BitmapDrawable,
        val isBlock: Boolean
    )

    /**
     * 检测文本中是否包含LaTeX数学公式
     *
     * @param text 要检测的文本
     * @return true如果包含数学公式
     */
    fun containsMathFormula(text: String): Boolean {
        return INLINE_MATH_PATTERN.matcher(text).find() ||
                BLOCK_MATH_PATTERN.matcher(text).find()
    }
}
