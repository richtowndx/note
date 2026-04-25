package com.videonote.presentation.ui.tts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.videonote.domain.model.TTSState
import com.videonote.domain.model.TTSVoice
import com.videonote.presentation.viewmodel.TTSViewModel
import com.videonote.presentation.viewmodel.TTSUiState

/**
 * TTS控制面板组件（仅设置）
 * 仅包含语速调节和语音选择功能
 *
 * @param isVisible 是否显示控制面板
 * @param onDismiss 关闭面板回调
 * @param viewModel TTS ViewModel
 */
@Composable
fun TTSControlPanel(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    @Suppress("UNUSED_PARAMETER") text: String = "",
    @Suppress("UNUSED_PARAMETER") noteId: String? = null,
    viewModel: TTSViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    if (isVisible) {
        Dialog(onDismissRequest = onDismiss) {
            TTSSettingsContent(
                uiState = uiState,
                onDismiss = onDismiss,
                onRateChange = { viewModel.setSpeechRate(it) },
                onVoiceSelect = { viewModel.setVoice(it) },
                onErrorClear = { viewModel.clearError() }
            )
        }
    }
}

/**
 * TTS设置面板内容（仅语速和语音选择）
 */
@Composable
private fun TTSSettingsContent(
    uiState: TTSUiState,
    onDismiss: () -> Unit,
    onRateChange: (Float) -> Unit,
    onVoiceSelect: (TTSVoice) -> Unit,
    onErrorClear: () -> Unit
) {
    var showVoiceSelector by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = 8.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "语音播放设置",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // 语速滑块
            SpeechRateSlider(
                currentRate = uiState.settings.speechRate,
                onRateChange = onRateChange
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 语音选择按钮
            SettingButton(
                icon = Icons.Default.RecordVoiceOver,
                text = uiState.selectedVoice?.name?.takeIf { it.length <= 8 } ?: "语音",
                onClick = { showVoiceSelector = true },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // 错误提示
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // 显示错误后延迟清除
            kotlinx.coroutines.delay(3000)
            onErrorClear()
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colors.error,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = error,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.error
                )
            }
        }
    }

    // 语音选择对话框
    if (showVoiceSelector) {
        VoiceSelectorDialog(
            voices = uiState.availableVoices,
            selectedVoice = uiState.selectedVoice,
            onVoiceSelected = {
                onVoiceSelect(it)
                showVoiceSelector = false
            },
            onDismiss = { showVoiceSelector = false }
        )
    }
}

/**
 * 语速滑块
 */
@Composable
private fun SpeechRateSlider(
    currentRate: Float,
    onRateChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "语速",
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${String.format("%.1f", currentRate)}x",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.primary
            )
        }

        Slider(
            value = currentRate,
            onValueChange = onRateChange,
            valueRange = 0.2f..3.0f,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("慢", style = MaterialTheme.typography.caption)
            Text("1.0x", style = MaterialTheme.typography.caption)
            Text("快", style = MaterialTheme.typography.caption)
        }
    }
}

/**
 * 设置按钮
 */
@Composable
private fun SettingButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            maxLines = 1
        )
    }
}

/**
 * 语音选择对话框
 */
@Composable
private fun VoiceSelectorDialog(
    voices: List<TTSVoice>,
    selectedVoice: TTSVoice?,
    onVoiceSelected: (TTSVoice) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "选择语音",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(voices) { voice ->
                        VoiceItem(
                            voice = voice,
                            isSelected = voice.id == selectedVoice?.id,
                            onClick = { onVoiceSelected(voice) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 语音项
 */
@Composable
private fun VoiceItem(
    voice: TTSVoice,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        backgroundColor = if (isSelected) {
            MaterialTheme.colors.primary.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colors.surface
        },
        elevation = if (isSelected) 4.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = voice.name,
                    style = MaterialTheme.typography.body1,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = voice.lang,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "已选中",
                    tint = MaterialTheme.colors.primary
                )
            }
        }
    }
}

/**
 * TTS播放控制条（顶部栏版本）
 * 用于在TopAppBar中显示播放控制
 * 布局：按钮占2/3，进度文本占1/3
 * 优化：紧凑布局，减少按钮间距
 */
@Composable
fun TTSPlaybackBar(
    uiState: TTSUiState,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 按钮区域：紧凑布局
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 后退按钮
            IconButton(
                onClick = onPrevious,
                enabled = uiState.currentPosition > 0,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.FastRewind,
                    contentDescription = "后退",
                    modifier = Modifier.size(18.dp),
                    tint = if (uiState.currentPosition > 0) {
                        MaterialTheme.colors.onPrimary
                    } else {
                        MaterialTheme.colors.onPrimary.copy(alpha = 0.3f)
                    }
                )
            }

            Spacer(modifier = Modifier.width(2.dp))

            // 播放/暂停按钮
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    when {
                        uiState.isPlaying -> Icons.Default.Pause
                        else -> Icons.Default.PlayArrow
                    },
                    contentDescription = if (uiState.isPlaying) "暂停" else "播放",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colors.onPrimary
                )
            }

            Spacer(modifier = Modifier.width(2.dp))

            // 前进按钮
            IconButton(
                onClick = onNext,
                enabled = uiState.currentPosition < uiState.totalBlocks - 1,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.FastForward,
                    contentDescription = "前进",
                    modifier = Modifier.size(18.dp),
                    tint = if (uiState.currentPosition < uiState.totalBlocks - 1) {
                        MaterialTheme.colors.onPrimary
                    } else {
                        MaterialTheme.colors.onPrimary.copy(alpha = 0.3f)
                    }
                )
            }
        }

        // 进度显示区域：固定宽度
        if (uiState.totalBlocks > 0) {
            Spacer(modifier = Modifier.width(4.dp))
            val progressText = "${uiState.currentPosition + 1}/${uiState.totalBlocks}"
            Text(
                text = progressText,
                style = MaterialTheme.typography.caption.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colors.onPrimary.copy(alpha = 0.9f),
                maxLines = 1
            )
        }
    }
}
