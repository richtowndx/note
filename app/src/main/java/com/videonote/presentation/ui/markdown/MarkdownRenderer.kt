package com.videonote.presentation.ui.markdown

import android.content.Context
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import io.noties.markwon.Markwon
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.editor.MarkwonEditor
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.image.ImagesPlugin

/**
 * Markdown渲染器配置类
 * 提供预配置的Markwon实例，支持常用的Markdown语法和功能
 */
object MarkdownRenderer {

    /**
     * 创建基础Markdown渲染器
     * 支持标准Markdown语法，包括图片、表格、删除线、任务列表等
     *
     * @param context Android上下文，用于资源访问
     * @param primaryColor 主题色彩
     * @param onSurfaceColor 文本色
     * @param backgroundColor 背景色
     * @return 配置好的Markwon实例
     */
    fun createMarkwon(
        context: Context,
    ): Markwon {
        return Markwon.builder(context)
            .usePlugin(CorePlugin.create())
            .usePlugin(ImagesPlugin.create())
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(TaskListPlugin.create(context))
            .build()
    }
}