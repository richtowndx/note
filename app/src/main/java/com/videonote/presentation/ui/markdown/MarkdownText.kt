package com.videonote.presentation.ui.markdown

import android.webkit.WebView
import android.webkit.WebSettings
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.text.Spanned
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import com.videonote.util.Logger
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon

/**
 * 预处理转义的 $ 符号
 * 将 \$ 替换为临时占位符，避免被误识别为公式边界
 * @return Pair<处理后的文本, 占位符列表>
 */
private fun preprocessEscapedDollars(text: String): Pair<String, List<String>> {
    val escapedDollars = mutableListOf<String>()
    var index = 0
    val placeholder = "\u0000ESCAPED_DOLLAR_"

    var processed = text.replace(Regex("""\\\$""")) {
        val id = "$placeholder$index\u0000"
        escapedDollars.add(id)
        index++
        id
    }

    return Pair(processed, escapedDollars)
}

/**
 * 恢复转义的 $ 符号
 * 将临时占位符恢复为 \$
 */
private fun restoreEscapedDollars(text: String, escapedDollars: List<String>): String {
    var result = text
    escapedDollars.forEach { placeholder ->
        result = result.replace(placeholder, "\\$")
    }
    return result
}

/**
 * 从 LaTeX 公式中提取可读文本（用于 TTS）
 *
 * 移除 $ 符号和部分 LaTeX 命令，保留基础数学表达式
 *
 * 示例：
 * - "$E=mc^2$" → "E等于mc平方"
 * - "$$a^2+b^2=c^2$$" → "a的平方加b的平方等于c的平方"
 */
private fun extractReadableTextFromFormula(formula: String): String {
    // 移除常见 LaTeX 命令和符号，转换为可读文本
    return formula
        .replace("\\frac{", "分式")
        .replace("\\sqrt{", "根号")
        .replace("\\sum", "求和")
        .replace("\\int", "积分")
        .replace("\\infty", "无穷大")
        .replace("\\alpha", "阿尔法")
        .replace("\\beta", "贝塔")
        .replace("\\gamma", "伽马")
        .replace("\\delta", "德尔塔")
        .replace("\\theta", "西塔")
        .replace("\\pi", "派")
        .replace(Regex("""\\([a-zA-Z]+)"""), "")  // 移除其他反斜杠命令
        .replace("{", "").replace("}", "")  // 移除花括号
        .replace("^2", "的平方")
        .replace("^3", "的立方")
        .replace(Regex("""\^\{([0-9]+)\}"""), "的$1次方")  // 处理上标
        .replace(Regex("""_\{([0-9a-zA-Z]+)\}"""), "$1下标")  // 处理下标
        .replace("\\le", "小于等于")
        .replace("\\ge", "大于等于")
        .replace("\\ne", "不等于")
        .replace("\\to", "趋于")
        .replace("\\rightarrow", "箭头")
        .replace("\\left", "").replace("\\right", "")
        .replace("\\(", "").replace("\\)", "")
        .replace("\\[", "").replace("\\]", "")
        .trim()
}

/**
 * 从 Markdown 文本中提取 TTS 文本块
 * 与 parseMarkdownWithIds 的 blockIndex 一一对应
 * 确保文本块分割与 HTML 渲染完全一致
 *
 * LaTeX 公式处理：
 * - 块级公式已由 parseMarkdownWithIds 转换为可读文本
 * - 行内公式在文本块中保留可读形式
 *
 * @param markdown Markdown 文本
 * @return TTS 文本块列表，每个元素对应一个 blockIndex
 */
fun splitMarkdownIntoTTSBlocks(markdown: String): List<String> {
    // parseMarkdownWithIds 已集成公式处理，直接调用即可
    val (_, textBlocks) = parseMarkdownWithIds(markdown)
    return textBlocks
}

/**
 * Markdown文本渲染组件
 *
 * - 纯 Markdown：使用 WebView 渲染（表格正确显示，文字可复制）
 * - 含 LaTeX 公式：使用 Markwon 渲染（公式以图片显示，支持 TTS 高亮）
 *
 * @param markdown 要渲染的Markdown源文本
 * @param modifier 组件修饰符，默认为空
 * @param textColor 文字颜色，null则使用主题色
 * @param backgroundColor 背景颜色，null则使用主题背景色
 * @param fontSize 字体大小，null则使用默认大小
 * @param padding 内容内边距，默认16dp
 * @param loading 是否显示加载状态
 * @param error 错误信息，非null时显示错误页面
 * @param onError 错误发生时的回调
 * @param highlightText 需要高亮的文本内容（用于TTS当前朗读段落）
 * @param highlightBlockIndex TTS文本块索引，用于定位高亮位置
 * @param highlightColor 高亮颜色（当前未使用，预留参数）
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color? = null,
    backgroundColor: Color? = null,
    fontSize: androidx.compose.ui.unit.TextUnit? = null,
    padding: PaddingValues = PaddingValues(16.dp),
    loading: Boolean = false,
    error: String? = null,
    onError: (Exception) -> Unit = {},
    highlightText: String? = null,
    highlightBlockIndex: Int = -1,
    highlightColor: Color = Color.Yellow.copy(alpha = 0.3f)
) {
    val materialColors = MaterialTheme.colors

    // 预处理 Markdown：移除代码块包裹
    var processed = markdown
    if (processed.startsWith("```markdown")) {
        processed = processed.substring(12)
        if (processed.endsWith("```")) processed = processed.dropLast(3)
    }

    // 检查是否包含 LaTeX 公式（用于调试）
    val hasMath = LatexMathRenderer.containsMathFormula(processed)
    Logger.d("VideoNote.MarkdownText", "[Markdown渲染] hasMath=$hasMath, 使用WebView渲染（支持LaTeX公式、表格和文本复制）")

    Box(modifier = modifier.fillMaxSize()) {
        when {
            loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = materialColors.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "加载Markdown内容中...",
                        style = MaterialTheme.typography.body1,
                        color = materialColors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            error != null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "渲染错误",
                        style = MaterialTheme.typography.h6,
                        color = materialColors.error,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.body2,
                        color = materialColors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            else -> {
                // 始终使用 WebView 渲染（支持表格正确渲染、文字可复制、LaTeX 公式）
                MarkdownWithWebView(
                    markdown = processed,
                    modifier = Modifier.fillMaxSize(),
                    textColor = textColor ?: materialColors.onSurface,
                    backgroundColor = backgroundColor ?: materialColors.background,
                    padding = padding,
                    highlightText = highlightText,
                    highlightBlockIndex = highlightBlockIndex
                )
            }
        }
    }
}

/**
 * 使用 WebView 渲染 Markdown
 * 支持正确渲染表格，文字可复制
 */
@Composable
private fun MarkdownWithWebView(
    markdown: String,
    modifier: Modifier,
    textColor: Color,
    backgroundColor: Color,
    padding: PaddingValues,
    highlightText: String? = null,
    highlightBlockIndex: Int = -1
) {
    // 将 Markdown 转换为 HTML，并为每个文本块添加 ID
    val htmlContent = remember(markdown) {
        val result = markdownToHtmlWithIds(markdown, textColor, backgroundColor)
        result.first
    }

    // 计算需要高亮的元素 ID
    val highlightId = remember(highlightText, highlightBlockIndex) {
        if (!highlightText.isNullOrEmpty() && highlightBlockIndex >= 0) {
            "md-block-$highlightBlockIndex"
        } else {
            null
        }
    }

    // WebView 引用，用于在 LaunchedEffect 中访问
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // 监听 highlightId 变化并执行高亮（仅更新 CSS 高亮，不自动滚动）
    LaunchedEffect(highlightId) {
        highlightId?.let { id ->
            webViewRef?.postDelayed({
                // 高亮目标元素，由页面内 JS 的 lastUserTouchTime 控制是否自动滚动
                val js = """
                    (function() {
                        document.querySelectorAll('.md-highlight').forEach(el => {
                            el.classList.remove('md-highlight');
                        });
                        var target = document.getElementById('$id');
                        if (target) {
                            target.classList.add('md-highlight');
                            // 仅在用户未触摸时自动滚动
                            if (Date.now() - lastUserTouchTime > 2000) {
                                target.scrollIntoView({ behavior: 'smooth', block: 'center' });
                            }
                        }
                    })();
                """.trimIndent()
                webViewRef?.evaluateJavascript(js, null)
            }, 150)
        }
    }

    AndroidView(
        modifier = modifier.padding(padding),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.textZoom = 100

                // 启用缩放控件
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false

                webViewClient = WebViewClient()

                // 设置背景色
                setBackgroundColor(backgroundColor.toArgb())
            }.also { webViewRef = it }
        },
        update = { webView ->
            // 仅在引用变化时更新（避免每次重组都写状态）
            if (webViewRef !== webView) {
                webViewRef = webView
            }
            // 只在内容变化时重新加载
            val currentUrl = webView.url
            if (currentUrl == null || !currentUrl.startsWith("data:")) {
                webView.loadDataWithBaseURL(
                    null,
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        }
    )
}

/**
 * 使用 WebView 渲染 Markdown，带滚动进度跟踪
 * 支持正确渲染表格、LaTeX 公式，并提供滚动进度回调
 */
@Composable
private fun MarkdownWithWebViewAndProgress(
    markdown: String,
    modifier: Modifier,
    textColor: Color?,
    backgroundColor: Color?,
    padding: PaddingValues,
    initialScrollProgress: Float = 0f,
    onScrollProgress: (Float) -> Unit = {}
) {
    val materialColors = MaterialTheme.colors

    // 将 Markdown 转换为 HTML
    val (htmlContent, _) = remember(markdown) {
        markdownToHtmlWithIds(markdown, textColor ?: materialColors.onSurface, backgroundColor ?: materialColors.background)
    }

    // WebView 引用
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // 滚动进度状态
    var scrollProgress by remember { mutableStateOf(initialScrollProgress) }

    // 监听 initialScrollProgress 变化，恢复滚动位置
    LaunchedEffect(initialScrollProgress) {
        if (initialScrollProgress > 0f) {
            webViewRef?.postDelayed({
                val js = """
                    (function() {
                        const scrollHeight = document.documentElement.scrollHeight - window.innerHeight;
                        if (scrollHeight > 0) {
                            window.scrollTo(0, scrollHeight * $initialScrollProgress);
                        }
                    })();
                """.trimIndent()
                webViewRef?.evaluateJavascript(js, null)
            }, 300)
        }
    }

    AndroidView(
        modifier = modifier.padding(padding),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.textZoom = 100

                // 启用缩放控件
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false

                webViewClient = WebViewClient()

                // 设置背景色
                setBackgroundColor((backgroundColor ?: materialColors.background).toArgb())

                // 添加滚动监听
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // 注入JavaScript来监听滚动
                        view?.evaluateJavascript("""
                            (function() {
                                let lastScrollTop = 0;
                                let scrollTimer = null;
                                window.addEventListener('scroll', function() {
                                    const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
                                    const scrollHeight = document.documentElement.scrollHeight - window.innerHeight;
                                    const progress = scrollHeight > 0 ? scrollTop / scrollHeight : 0;

                                    // 节流，避免频繁回调
                                    clearTimeout(scrollTimer);
                                    scrollTimer = setTimeout(function() {
                                        window.AndroidInterface.onScrollProgress(progress);
                                    }, 50);
                                });
                            })();
                        """.trimIndent(), null)
                    }
                }

                // 添加JavaScript接口
                addJavascriptInterface(object {
                    @android.webkit.JavascriptInterface
                    fun onScrollProgress(progress: Double) {
                        scrollProgress = progress.toFloat()
                        onScrollProgress(progress.toFloat())
                    }

                    @android.webkit.JavascriptInterface
                    fun onMathJaxReady(status: String) {
                        Logger.d("VideoNote.MarkdownText", "[MathJax] MathJax ready: $status")
                    }
                }, "AndroidInterface")
            }.also { webViewRef = it }
        },
        update = { webView ->
            webViewRef = webView
            // 只在内容变化时重新加载
            val currentUrl = webView.url
            if (currentUrl == null || !currentUrl.startsWith("data:")) {
                webView.loadDataWithBaseURL(
                    null,
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        }
    )
}

/**
 * 将 Markdown 转换为 HTML，并为每个文本块添加唯一 ID
 * 用于 TTS 高亮跟随功能
 */
private fun markdownToHtmlWithIds(
    markdown: String,
    textColor: Color,
    backgroundColor: Color
): Pair<String, List<String>> {
    val html = StringBuilder()

    // HTML 头部
    html.append("""
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=3.0, user-scalable=yes">
            <!-- MathJax for LaTeX rendering -->
            <script>
            // 配置 MathJax
            window.MathJax = {
                tex: {
                    inlineMath: [['$', '$'], ['\\(', '\\)']],
                    displayMath: [['$$', '$$'], ['\\[', '\\]']],
                    processEscapes: true
                },
                options: {
                    skipHtmlTags: ['script', 'noscript', 'style', 'textarea', 'pre']
                },
                startup: {
                    ready: function() {
                        console.log('[MathJax] Ready, typesetting...');
                        MathJax.startup.defaultReady();
                    }
                }
            };
            </script>
            <!-- 同步加载 MathJax -->
            <script src="https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js" id="MathJax-script"></script>
            <style>
                body {
                    margin: 0;
                    padding: 16px;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                    font-size: 16px;
                    line-height: 1.6;
                    color: ${textColorToHex(textColor)};
                    background-color: ${textColorToHex(backgroundColor)};
                }
                h1, h2, h3, h4, h5, h6 {
                    margin-top: 1em;
                    margin-bottom: 0.5em;
                    font-weight: 600;
                }
                h1 { font-size: 1.5em; }
                h2 { font-size: 1.3em; }
                h3 { font-size: 1.15em; }
                p { margin: 0.5em 0; }
                ul, ol { margin: 0.5em 0; padding-left: 1.5em; }
                li { margin: 0.25em 0; }
                code {
                    background-color: rgba(128, 128, 128, 0.1);
                    padding: 2px 4px;
                    border-radius: 3px;
                    font-family: monospace;
                }
                pre {
                    background-color: rgba(128, 128, 128, 0.1);
                    padding: 12px;
                    border-radius: 6px;
                    overflow-x: auto;
                    overflow-y: auto;
                    margin: 0.5em 0;
                    white-space: pre-wrap;
                    word-wrap: break-word;
                    max-height: none;
                }
                pre code {
                    background: none;
                    padding: 0;
                    white-space: pre-wrap;
                    font-family: 'Courier New', Courier, monospace;
                    font-size: 14px;
                    line-height: 1.5;
                }
                blockquote {
                    border-left: 3px solid rgba(128, 128, 128, 0.3);
                    padding-left: 1em;
                    margin: 0.5em 0;
                    color: rgba(128, 128, 128, 0.7);
                }
                table {
                    border-collapse: collapse;
                    width: 100%;
                    margin: 0.5em 0;
                }
                th, td {
                    border: 1px solid rgba(128, 128, 128, 0.3);
                    padding: 8px 12px;
                    text-align: left;
                }
                th {
                    background-color: rgba(128, 128, 128, 0.1);
                    font-weight: 600;
                }
                tr:nth-child(even) {
                    background-color: rgba(128, 128, 128, 0.03);
                }
                a {
                    color: ${textColorToHex(textColor)};
                    text-decoration: underline;
                }
                img {
                    max-width: 100%;
                    height: auto;
                }
                /* TTS 高亮样式 */
                .md-highlight {
                    background-color: rgba(76, 175, 80, 0.3);
                    border-left: 4px solid #4CAF50;
                    padding-left: 8px;
                    transition: background-color 0.3s ease;
                }
                /* LaTeX 公式样式 */
                .latex-formula-inline {
                    display: inline;
                }
                .latex-formula-block {
                    display: block;
                    margin: 1em 0;
                    text-align: center;
                }
            </style>
            <script>
                // 用户触摸时间戳，用于 TTS 高亮时判断是否应自动滚动
                var lastUserTouchTime = 0;
                document.addEventListener('touchstart', function() { lastUserTouchTime = Date.now(); });
                document.addEventListener('touchmove', function() { lastUserTouchTime = Date.now(); });
            </script>
        </head>
        <body>
    """.trimIndent())

    // 解析并转换 Markdown，为每个文本块添加 ID，同时获取 TTS 文本块
    // 公式处理已集成到 parseMarkdownWithIds 中
    val (parsedHtml, textBlocks) = parseMarkdownWithIds(markdown)
    html.append(parsedHtml)

    // HTML 尾部
    html.append("""
        </body>
        </html>
    """.trimIndent())

    return Pair(html.toString(), textBlocks)
}

/**
 * 将 Markdown 转换为 HTML，并为每个文本块添加唯一 ID
 * 用于 TTS 高亮跟随功能
 *
 * LaTeX 公式处理：
 * - 块级公式 $$...$$ 作为独立块，分配 md-block-x ID
 * - 行内公式 $...$ 在行内处理，不分配独立 ID
 *
 * @return Pair<HTML字符串, TTS文本块列表>，文本块列表与 blockIndex 一一对应
 */
private fun parseMarkdownWithIds(markdown: String): Pair<String, List<String>> {
    val html = StringBuilder()
    val textBlocks = mutableListOf<String>()

    // 预处理转义的 $ 符号
    val (preprocessedMarkdown, escapedDollars) = preprocessEscapedDollars(markdown)
    val lines = preprocessedMarkdown.lines()
    var i = 0
    var blockIndex = 0

    // 块级公式正则：$$...$$，支持跨行，支持空公式
    val blockFormulaPattern = Regex("""\$\$([^$]*?)\$\$""", RegexOption.DOT_MATCHES_ALL)

    while (i < lines.size) {
        val line = lines[i].trim()

        when {
            // 空行
            line.isEmpty() -> {
                html.append("<br>")
            }

            // 块级 LaTeX 公式 $$...$$（需要在代码块之前检测）
            line.contains("$$") && blockFormulaPattern.containsMatchIn(line) -> {
                // 处理可能跨行的块级公式
                val remainingText = lines.drop(i).joinToString("\n")
                val matchResult = blockFormulaPattern.find(remainingText)

                if (matchResult != null) {
                    val formula = matchResult.groupValues[1].trim()
                    val id = "md-block-$blockIndex"
                    val displayText = extractReadableTextFromFormula(formula)

                    // 生成 HTML，直接将公式放入内容供 MathJax 处理
                    val escapedFormula = escapeHtml(formula)
                    val escapedDisplayText = escapeHtmlAttribute(displayText)

                    html.append("""<div id="$id" class="latex-formula-block" data-tts="$escapedDisplayText">$$escapedFormula$$</div>""")
                    textBlocks.add(displayText)
                    blockIndex++

                    // 计算公式跨越的行数
                    val matchedText = matchResult.value
                    val linesConsumed = matchedText.count { it == '\n' } + 1
                    i += linesConsumed
                } else {
                    // 未完整匹配，作为普通行处理
                    val id = "md-block-$blockIndex"
                    html.append("<p id=\"$id\">${parseInlineMarkdown(line)}</p>")
                    textBlocks.add(extractTTSFromMarkdown(line))
                    blockIndex++
                }
            }

            // 标题 - 标准 Markdown 格式: 1-6 个 # 后跟空格
            line.matches(Regex("^#{1,6}\\s+.*")) -> {
                val headerPattern = Regex("^(#{1,6})\\s+(.+)$")
                val matchResult = headerPattern.find(line)
                if (matchResult != null) {
                    val level = matchResult.groupValues[1].length
                    val text = matchResult.groupValues[2].trim()
                    val id = "md-block-$blockIndex"
                    html.append("<h$level id=\"$id\">${escapeHtml(text)}</h$level>")
                    textBlocks.add(extractTTSFromMarkdown(text))
                    blockIndex++
                } else {
                    // 不应该到这里，但作为降级处理
                    val id = "md-block-$blockIndex"
                    html.append("<p id=\"$id\">${parseInlineMarkdown(line)}</p>")
                    textBlocks.add(extractTTSFromMarkdown(line))
                    blockIndex++
                }
            }

            // 代码块
            line.startsWith("```") -> {
                var language = line.drop(3).trim()
                val id = "md-block-$blockIndex"
                Logger.d(TAG, "[MarkdownText] 代码块开始: language='$language', id=$id, 起始行=$i")
                i++

                if (language.isEmpty() && i < lines.size) {
                    val nextLine = lines[i].trim()
                    if (!nextLine.startsWith("```")) {
                        language = nextLine.removePrefix("```").trim()
                        Logger.d(TAG, "[MarkdownText] 从第二行获取语言标识: language='$language'")
                        i++
                    }
                }

                val code = StringBuilder()
                while (i < lines.size) {
                    val currentLine = lines[i]
                    if (currentLine.trim().startsWith("```")) {
                        break
                    }
                    code.append(currentLine).append("\n")
                    i++
                }
                val codeContent = code.toString().trim()
                Logger.d(TAG, "[MarkdownText] 代码块解析完成: language='$language'")
                html.append("<pre id=\"$id\"><code class=\"language-$language\">${escapeHtml(codeContent)}</code></pre>")
                textBlocks.add(codeContent)
                blockIndex++
            }

            // 无序列表
            line.matches(Regex("^[-*+]\\s+.*")) -> {
                html.append("<ul>")
                val firstText = line.drop(2).trim()
                val firstId = "md-block-$blockIndex"
                html.append("<li id=\"$firstId\">${parseInlineMarkdown(firstText)}</li>")
                textBlocks.add(extractTTSFromMarkdown(firstText))
                blockIndex++
                i++

                while (i < lines.size && lines[i].matches(Regex("^[-*+]\\s+.*"))) {
                    val itemId = "md-block-$blockIndex"
                    val itemText = lines[i].drop(2).trim()
                    html.append("<li id=\"$itemId\">${parseInlineMarkdown(itemText)}</li>")
                    textBlocks.add(extractTTSFromMarkdown(itemText))
                    blockIndex++
                    i++
                }
                html.append("</ul>")
                i--
            }

            // 有序列表
            line.matches(Regex("^\\d+\\.\\s+.*")) -> {
                html.append("<ol>")
                val firstText = line.drop(line.indexOf('.') + 1).trim()
                val firstId = "md-block-$blockIndex"
                html.append("<li id=\"$firstId\">${parseInlineMarkdown(firstText)}</li>")
                textBlocks.add(extractTTSFromMarkdown(firstText))
                blockIndex++
                i++

                while (i < lines.size && lines[i].matches(Regex("^\\d+\\.\\s+.*"))) {
                    val itemId = "md-block-$blockIndex"
                    val itemText = lines[i].drop(lines[i].indexOf('.') + 1).trim()
                    html.append("<li id=\"$itemId\">${parseInlineMarkdown(itemText)}</li>")
                    textBlocks.add(extractTTSFromMarkdown(itemText))
                    blockIndex++
                    i++
                }
                html.append("</ol>")
                i--
            }

            // 表格 - 每个单元格独立ID和TTS块
            line.contains("|") && line.trim().startsWith("|") -> {
                val tableResult = parseTableWithCellIds(lines, i, blockIndex)
                html.append(tableResult.html)
                // 将表格的所有单元格添加到textBlocks
                textBlocks.addAll(tableResult.textBlocks)
                i = tableResult.lineIndex - 1
                blockIndex = tableResult.blockIndex
            }

            // 引用
            line.startsWith(">") -> {
                val text = line.drop(1).trim()
                val id = "md-block-$blockIndex"
                html.append("<blockquote id=\"$id\">${parseInlineMarkdown(text)}</blockquote>")
                textBlocks.add(extractTTSFromMarkdown(text))
                blockIndex++
            }

            // 水平线
            line.matches(Regex("^[-*]{3,}$")) -> {
                html.append("<hr>")
            }

            // 普通段落
            else -> {
                val id = "md-block-$blockIndex"
                html.append("<p id=\"$id\">${parseInlineMarkdown(line)}</p>")
                textBlocks.add(extractTTSFromMarkdown(line))
                blockIndex++
            }
        }

        i++
    }

    val finalHtml = restoreEscapedDollars(html.toString(), escapedDollars)
    return Pair(finalHtml, textBlocks)
}

/**
 * 表格解析结果数据类
 * 包含HTML、TTS文本块列表、更新后的行索引和更新后的块索引
 */
data class TableParseResult(
    val html: String,
    val textBlocks: List<String>,
    val lineIndex: Int,
    val blockIndex: Int
)

/**
 * 解析表格，为每个单元格分配独立的ID和TTS块
 * 返回HTML、TTS文本块列表、更新后的行索引和更新后的块索引
 */
private fun parseTableWithCellIds(lines: List<String>, startIndex: Int, startBlockIndex: Int): TableParseResult {
    val html = StringBuilder()
    val textBlocks = mutableListOf<String>()
    var i = startIndex
    var blockIndex = startBlockIndex

    html.append("<table>")

    // 表头
    val headerLine = lines[i]
    val headers = headerLine.split("|").filter { it.trim().isNotEmpty() }
    html.append("<thead><tr>")
    for (header in headers) {
        val cellId = "md-block-$blockIndex"
        val cellContent = parseInlineMarkdown(header.trim())
        html.append("<th id=\"$cellId\">$cellContent</th>")
        // 将表头单元格添加到TTS文本块
        textBlocks.add(extractTTSFromMarkdown(header.trim()))
        blockIndex++
    }
    html.append("</tr></thead>")

    // 跳过下一行
    i++

    // 检查并跳过分隔行 (|---|---|)
    if (i < lines.size && lines[i].trim().matches(Regex("^\\|?\\s*:?-+:?\\s*(\\|\\s*:?-+:?\\s*)*\\|?$"))) {
        i++  // 跳过分隔行
    }

    // 表体
    html.append("<tbody>")
    while (i < lines.size && lines[i].contains("|") && !lines[i].trim().isEmpty()) {
        // 再次检查，确保不是分隔行
        if (lines[i].trim().matches(Regex("^\\|?\\s*:?-+:?\\s*(\\|\\s*:?-+:?\\s*)*\\|?$"))) {
            i++
            continue  // 跳过分隔行
        }

        // 分割并过滤空单元格，包括首尾的空字符串
        val cells = lines[i].split("|")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        // 只在有有效单元格时才渲染行
        if (cells.isNotEmpty()) {
            html.append("<tr>")
            for (cell in cells) {
                val cellId = "md-block-$blockIndex"
                val cellContent = parseInlineMarkdown(cell)
                html.append("<td id=\"$cellId\">$cellContent</td>")
                // 将单元格内容添加到TTS文本块
                textBlocks.add(extractTTSFromMarkdown(cell))
                blockIndex++
            }
            html.append("</tr>")
        }
        i++
    }
    html.append("</tbody></table>")

    return TableParseResult(html.toString(), textBlocks, i, blockIndex)
}

/**
 * 解析行内 Markdown（粗体、斜体、代码、链接、行内公式）
 *
 * 处理顺序：
 * 1. 先处理行内代码（避免被公式解析干扰）
 * 2. 再处理行内公式 $...$
 * 3. 最后处理其他 Markdown 语法
 */
private fun parseInlineMarkdown(text: String): String {
    var result = text

    // 检测是否包含公式（用于调试日志）
    val hasInlineFormula = "$" in result

    // 1. 处理行内代码 `code`（优先处理，避免公式解析干扰）
    result = result.replace(Regex("`(.+?)`")) { matchResult ->
        val codeContent = matchResult.groupValues[1]
        "${escapeHtml(codeContent)}"
    }

    // 2. 处理行内 LaTeX 公式 $...$（必须在escapeHtml之前处理）
    // 正则说明：\$ 匹配 $，([^$]*?) 匹配公式内容（可为空），\$ 匹配结束 $
    var formulaCount = 0
    result = result.replace(Regex("""\$([^$]*?)\$""", RegexOption.DOT_MATCHES_ALL)) { matchResult ->
        formulaCount++
        val formula = matchResult.groupValues[1].trim()
        val displayText = extractReadableTextFromFormula(formula)
        // 直接将公式放入 HTML 内容，让 MathJax 处理
        // 同时保存可读文本用于 TTS
        val escapedFormula = escapeHtml(formula)
        val escapedDisplayText = escapeHtmlAttribute(displayText)
        Logger.d(TAG, "[parseInlineMarkdown] Inline formula #${formulaCount}: original='$formula', display='$displayText'")
        """<span class="latex-formula-inline" data-tts="$escapedDisplayText">$escapedFormula</span>"""
    }

    if (hasInlineFormula && formulaCount == 0) {
        Logger.w(TAG, "[parseInlineMarkdown] Dollar symbol detected but no formula matched, possibly affected by escapeHtml: text='$text'")
    }

    // 3. 转义 HTML 特殊字符（必须在公式和代码处理之后）
    result = escapeHtml(result)

    // 4. 粗体 **text** 或 __text__
    result = result.replace(Regex("\\*\\*(.+?)\\*\\*"), "<strong>$1</strong>")
    result = result.replace(Regex("__(.+?)__"), "<strong>$1</strong>")

    // 5. 斜体 *text* 或 _text_
    result = result.replace(Regex("\\*(.+?)\\*"), "<em>$1</em>")
    result = result.replace(Regex("_(.+?)_"), "<em>$1</em>")

    // 6. 链接 [text](url)
    result = result.replace(Regex("\\[(.+?)\\]\\((.+?)\\)")) { matchResult ->
        val linkText = matchResult.groupValues[1]
        val url = matchResult.groupValues[2]
        """<a href="$url">$linkText</a>"""
    }

    return result
}

/**
 * 从 Markdown 文本中提取 TTS 文本
 * 平衡复杂度和效果的方案：
 * 1. 图片：![alt](url) -> 图片：alt
 * 2. 链接：[text](url) -> text
 * 3. 行内代码：`code` -> 代码：code
 */
private fun extractTTSFromMarkdown(text: String): String {
    var result = text

    // 1. 图片：![alt](url) -> 图片：alt
    result = result.replace(Regex("""!\[([^\]]*)\]\([^\)]+\)"""), "图片：$1")

    // 2. 链接：[text](url) -> text
    result = result.replace(Regex("""\[([^\]]+)\]\([^\)]+\)"""), "$1")

    // 3. 行内代码：`code` -> 代码：code
    result = result.replace(Regex("""`([^`]+)`"""), "代码：$1")

    return result
}

/**
 * 转义 HTML 特殊字符（用于 HTML 内容）
 */
private fun escapeHtml(text: String): String {
    return text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}

/**
 * 转义 HTML 属性值特殊字符
 * 用于 data-formula 等属性值的转义
 */
private fun escapeHtmlAttribute(text: String): String {
    return text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
        .replace("\n", "&#10;")
        .replace("\r", "&#13;")
        .replace("\t", "&#9;")
}

/**
 * 将 Color 转换为十六进制颜色字符串
 */
private fun textColorToHex(color: Color): String {
    val red = (color.red * 255).toInt()
    val green = (color.green * 255).toInt()
    val blue = (color.blue * 255).toInt()
    return "#%02X%02X%02X".format(red, green, blue)
}

/**
 * 带滚动进度跟踪的Markdown渲染组件
 */
@Composable
fun MarkdownTextWithProgress(
    markdown: String,
    modifier: Modifier = Modifier,
    initialScrollProgress: Float = 0f,
    onScrollProgress: (Float) -> Unit = {},
    textColor: Color? = null,
    backgroundColor: Color? = null,
    padding: PaddingValues = PaddingValues(16.dp),
    loading: Boolean = false,
    error: String? = null
) {
    val context = LocalContext.current
    val materialColors = MaterialTheme.colors

    @Suppress("UNUSED_VARIABLE")
    val unusedContext = context

    // 预处理 Markdown：移除代码块包裹
    var processed = markdown
    if (processed.startsWith("```markdown")) {
        processed = processed.substring(12)
        if (processed.endsWith("```")) processed = processed.dropLast(3)
    }

    // 检查是否包含 LaTeX 公式
    val hasMath = LatexMathRenderer.containsMathFormula(processed)

    Box(modifier = modifier.fillMaxSize()) {
        when {
            loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = materialColors.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "加载Markdown内容中...",
                        style = MaterialTheme.typography.body1,
                        color = materialColors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            error != null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "渲染错误",
                        style = MaterialTheme.typography.h6,
                        color = materialColors.error,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.body2,
                        color = materialColors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            else -> {
                // 使用 WebView 渲染以支持 LaTeX 公式
                Logger.d("VideoNote.MarkdownText", "[MarkdownTextWithProgress] Using WebView rendering for markdown with hasMath=$hasMath")
                MarkdownWithWebViewAndProgress(
                    markdown = processed,
                    modifier = Modifier.fillMaxSize(),
                    textColor = textColor,
                    backgroundColor = backgroundColor,
                    padding = padding,
                    initialScrollProgress = initialScrollProgress,
                    onScrollProgress = onScrollProgress
                )
            }
        }
    }
}

/**
 * 计算目标文本在TextView中的布局位置
 */
private fun calculateLineInfo(
    textView: TextView,
    highlightText: String,
    highlightBlockIndex: Int
): Triple<Int, Int, Int>? {
    val layout = textView.layout ?: return null
    val fullText = textView.text.toString()

    Logger.d("VideoNote.MarkdownText", "[左侧竖条] ========== 计算开始 ==========")
    Logger.d("VideoNote.MarkdownText", "[左侧竖条] 目标文本='$highlightText', TTS块索引: $highlightBlockIndex")

    // 按段落分割渲染后的文本
    val paragraphs = fullText.split("\n\n", "\n")
        .filter { it.trim().isNotEmpty() }
        .map { it.trim() }

    Logger.d("VideoNote.MarkdownText", "[左侧竖条] 渲染后共分割出 ${paragraphs.size} 个段落")

    // 查找匹配的段落索引
    var matchIndex = -1
    var findIndex = 0
    for (i in paragraphs.indices) {
        val para = paragraphs[i]
        if (para == highlightText || para.contains(highlightText)) {
            findIndex++
            if (i >= highlightBlockIndex && highlightBlockIndex >= 0) {
                matchIndex = i
                Logger.d("VideoNote.MarkdownText", "[左侧竖条] 找到匹配段落: $i")
                break
            }
        }
    }

    // 确定最终目标段落索引
    val finalIndex = if (matchIndex >= 0) {
        Logger.d("VideoNote.MarkdownText", "[左侧竖条] 使用匹配段落索引: $matchIndex")
        matchIndex
    } else {
        val idx = highlightBlockIndex % paragraphs.size.coerceAtLeast(1)
        Logger.d("VideoNote.MarkdownText", "[左侧竖条] 使用TTS块索引: $idx")
        idx
    }

    // 计算目标段落的布局信息
    if (finalIndex >= 0 && finalIndex < paragraphs.size) {
        val targetParagraph = paragraphs[finalIndex]

        // 计算目标段落之前的累积字符数
        var currentIndex = 0
        var targetPos = -1
        var accumulatedLength = 0

        val rawParagraphs = fullText.split("\n")
        for (i in rawParagraphs.indices) {
            val para = rawParagraphs[i].trim()
            if (para.isNotEmpty()) {
                if (currentIndex == finalIndex) {
                    val trimmedStart = rawParagraphs[i].indexOfFirst { !it.isWhitespace() }
                    targetPos = if (trimmedStart >= 0) accumulatedLength + trimmedStart else accumulatedLength
                    Logger.d("VideoNote.MarkdownText", "[左侧竖条] 目标段落: '$targetParagraph'")
                    Logger.d("VideoNote.MarkdownText", "[左侧竖条] 文本起始位置: $targetPos")
                    break
                }
                currentIndex++
            }
            accumulatedLength += rawParagraphs[i].length + 1
        }

        if (targetPos >= 0 && targetPos < fullText.length) {
            val line = layout.getLineForOffset(targetPos)
            val top = layout.getLineTop(line)
            val bottom = layout.getLineBottom(line)
            val lineHeight = (bottom - top).toInt()

            Logger.d("VideoNote.MarkdownText", "[左侧竖条] 行号: $line, 顶部位置: ${top.toInt()}, 底部位置: ${bottom.toInt()}")
            Logger.d("VideoNote.MarkdownText", "[左侧竖条] 行高: $lineHeight")
            Logger.d("VideoNote.MarkdownText", "[左侧竖条] ========== 计算完成 ==========")
            return Triple(line, top.toInt(), lineHeight)
        }
    }

    Logger.w("VideoNote.MarkdownText", "[左侧竖条] 无法计算行信息")
    Logger.d("VideoNote.MarkdownText", "[左侧竖条] ========== 计算失败 ==========")
    return null
}

private const val TAG = "VideoNote.MarkdownText"
