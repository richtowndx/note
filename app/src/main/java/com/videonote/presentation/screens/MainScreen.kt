package com.videonote.presentation.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.videonote.domain.model.Note
import com.videonote.domain.model.NoteStatus
import com.videonote.presentation.viewmodel.MainViewModel
import com.videonote.presentation.viewmodel.SidebarViewModel
import com.videonote.presentation.viewmodel.TTSViewModel
import com.videonote.presentation.ui.markdown.MarkdownText
import com.videonote.presentation.ui.tts.TTSControlPanel
import com.videonote.presentation.ui.tts.TTSPlaybackBar
import com.videonote.presentation.ui.sidebar.SidebarDrawer
import com.videonote.util.Logger
import kotlinx.coroutines.launch

/**
 * 主页面
 * 包含侧边栏、TTS播放控制和笔记内容显示
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    sidebarViewModel: SidebarViewModel = hiltViewModel(),
    ttsViewModel: TTSViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val ttsUiState by ttsViewModel.uiState.collectAsState()
    val sidebarUiState by sidebarViewModel.uiState.collectAsState()
    var showTTSSettings by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // 每次页面重新显示时（从设置页返回），刷新侧边栏目录
    LaunchedEffect(Unit) {
        sidebarViewModel.refreshDirectories()
    }

    // Modal侧边栏
    ModalDrawer(
        drawerState = drawerState,
        drawerContent = {
            SidebarDrawer(
                rootNodes = sidebarUiState.rootNodes,
                selectedDirectory = sidebarUiState.selectedDirectory,
                selectedDirectoryName = sidebarUiState.selectedDirectoryName,
                isLoading = sidebarUiState.isLoading,
                onNodeClick = { node ->
                    if (node.isDirectory) {
                        sidebarViewModel.toggleNodeExpand(node)
                    } else {
                        // 点击文件，加载文件
                        val uri = Uri.parse(node.path)
                        viewModel.loadFile(uri, node.name)
                        // 关闭侧边栏
                        coroutineScope.launch {
                            drawerState.close()
                        }
                    }
                },
                onToggleExpand = { node ->
                    sidebarViewModel.toggleNodeExpand(node)
                },
                onSettingsClick = {
                    coroutineScope.launch {
                        drawerState.close()
                    }
                    onNavigateToSettings()
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 侧边栏菜单按钮：直接放在 title 内，紧贴笔记名称
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "打开侧边栏",
                                tint = MaterialTheme.colors.onPrimary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable(role = Role.Button) {
                                        coroutineScope.launch { drawerState.open() }
                                    }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            // 笔记名称：尽量展示完整
                            Text(
                                text = uiState.currentNote?.fileName ?: "",
                                style = MaterialTheme.typography.body2,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            // TTS播放控制条（仅在笔记内容存在时显示）
                            if (uiState.currentNote?.status == NoteStatus.SUCCESS) {
                                Spacer(modifier = Modifier.width(4.dp))
                                TTSPlaybackBar(
                                    uiState = ttsUiState,
                                    onPlayPause = {
                                        val textToRead = uiState.currentNote?.markdownContent
                                            ?: uiState.currentNote?.originalContent ?: ""

                                        if (ttsUiState.isPlaying) {
                                            ttsViewModel.pause()
                                        } else if (ttsUiState.isPaused) {
                                            ttsViewModel.resume()
                                        } else {
                                            val fromStart = (ttsUiState.displayNoteProgress == 0)
                                            ttsViewModel.play(textToRead, fromStart = fromStart)
                                        }
                                    },
                                    onPrevious = { ttsViewModel.backward() },
                                    onNext = { ttsViewModel.forward() }
                                )
                            }
                        }
                    },
                    actions = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "语音播放设置",
                            tint = MaterialTheme.colors.onPrimary,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable(role = Role.Button) { showTTSSettings = true }
                        )
                    },
                    backgroundColor = MaterialTheme.colors.primary,
                    contentColor = MaterialTheme.colors.onPrimary,
                    navigationIcon = null
                )
            }
        ) { paddingValues ->
            if (uiState.currentNote == null) {
                // 空状态
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    uiState.error?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.body1,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colors.error,
                            modifier = Modifier.padding(16.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (uiState.isLoading) {
                        CircularProgressIndicator()
                    } else {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暂无笔记内容",
                            style = MaterialTheme.typography.h6,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "点击左上角打开侧边栏\n选择笔记文件进行阅读",
                            style = MaterialTheme.typography.body1,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                // 有笔记内容
                uiState.currentNote?.let { currentNote ->
                    // TTS 生命周期：切换笔记时停止 TTS 并重置进度，再加载新笔记设置
                    LaunchedEffect(currentNote.id) {
                        ttsViewModel.stop()
                        ttsViewModel.loadNoteTtsSettings(currentNote.id)
                    }
                    DisposableEffect(currentNote.id) {
                        onDispose { ttsViewModel.saveCurrentProgress() }
                    }

                    // 仅在 TTS 活跃时传递高亮信息，避免非播放状态下的无效重组
                    val isTTSActive = ttsUiState.isPlaying || ttsUiState.isPaused
                    val highlightText = if (isTTSActive) ttsUiState.currentTextBlock else null
                    val highlightBlockIndex = if (isTTSActive) ttsUiState.currentPosition else -1

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        NoteContentTab(
                            note = currentNote,
                            highlightText = highlightText,
                            highlightBlockIndex = highlightBlockIndex
                        )
                    }
                }
            }
        }
    }

    // TTS设置面板
    if (showTTSSettings) {
        TTSControlPanel(
            isVisible = showTTSSettings,
            onDismiss = { showTTSSettings = false },
            viewModel = ttsViewModel
        )
    }
}

@Composable
private fun NoteContentTab(
    note: Note,
    highlightText: String? = null,
    highlightBlockIndex: Int = -1
) {
    var renderError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(note.id) {
        renderError = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (note.status) {
            NoteStatus.SUCCESS -> {
                if (note.markdownContent != null) {
                    MarkdownText(
                        markdown = note.markdownContent,
                        modifier = Modifier.fillMaxSize(),
                        error = renderError,
                        onError = { exception ->
                            renderError = exception.message ?: "Markdown渲染失败"
                        },
                        fontSize = 16.sp,
                        padding = PaddingValues(16.dp),
                        highlightText = highlightText,
                        highlightBlockIndex = highlightBlockIndex
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "笔记内容为空",
                            style = MaterialTheme.typography.h6,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "处理中...",
                        style = MaterialTheme.typography.h6
                    )
                }
            }
        }

        renderError?.let { error ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.1f)
            ) {
                Row(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "渲染错误: $error",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.error
                    )
                }
            }
        }
    }
}

private const val TAG = "VideoNote.MainScreen"
