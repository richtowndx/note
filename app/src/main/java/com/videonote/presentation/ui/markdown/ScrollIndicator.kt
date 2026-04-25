package com.videonote.presentation.ui.markdown

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 滚动进度指示器组件
 * 在右侧显示垂直的滚动进度条，显示当前滚动位置
 *
 * @param progress 滚动进度，范围从0.0到1.0
 * @param modifier 修饰符
 * @param isActive 是否激活状态，影响透明度
 * @param color 进度条颜色，默认使用主题色彩
 * @param backgroundColor 背景颜色
 * @param thickness 进度条粗细，默认为4dp
 * @param animationDuration 动画持续时间，默认为300ms
 */
@Composable
fun ScrollIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    color: Color = MaterialTheme.colors.primary,
    backgroundColor: Color = MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
    thickness: androidx.compose.ui.unit.Dp = 4.dp,
    animationDuration: Int = 300
) {
    // 使用动画平滑过渡进度值
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = animationDuration),
        label = "scroll_progress"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(thickness + 4.dp)
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        // 背景轨道
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(thickness)
                .clip(RoundedCornerShape(thickness / 2))
                .background(backgroundColor.copy(alpha = if (isActive) 0.3f else 0.1f))
        )

        // 进度指示器
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(thickness)
                .clip(RoundedCornerShape(thickness / 2))
        ) {
            // 填充的进度部分
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(animatedProgress)
                    .clip(RoundedCornerShape(thickness / 2))
                    .background(color.copy(alpha = if (isActive) 0.8f else 0.4f))
            )
        }
    }
}

/**
 * 自动隐藏的滚动进度指示器
 * 当用户不滚动时自动隐藏，滚动时显示
 *
 * @param progress 滚动进度
 * @param isScrolling 是否正在滚动
 * @param modifier 修饰符
 * @param color 进度条颜色
 * @param backgroundColor 背景颜色
 * @param hideDelay 延迟隐藏时间（毫秒）
 */
@Composable
fun AutoHideScrollIndicator(
    progress: Float,
    isScrolling: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.primary,
    backgroundColor: Color = MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
    hideDelay: Int = 2000
) {
    var isVisible by remember { mutableStateOf(false) }

    // 根据滚动状态控制显示/隐藏
    LaunchedEffect(isScrolling) {
        if (isScrolling) {
            isVisible = true
        } else {
            kotlinx.coroutines.delay(hideDelay.toLong())
            isVisible = false
        }
    }

    // 使用动画控制透明度
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "indicator_alpha"
    )

    if (alpha > 0.01f) {
        ScrollIndicator(
            progress = progress,
            modifier = modifier,
            isActive = true,
            color = color.copy(alpha = alpha),
            backgroundColor = backgroundColor.copy(alpha = alpha * 0.3f)
        )
    }
}