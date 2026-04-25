package com.videonote.presentation.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.videonote.domain.model.NoteDirectory
import com.videonote.presentation.viewmodel.DirectorySettingsViewModel

/**
 * 目录设置页面
 * 管理笔记目录的添加和删除
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DirectorySettingsScreen(
    viewModel: DirectorySettingsViewModel,
    onNavigateBack: () -> Unit,
    onDirectorySelected: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf<NoteDirectory?>(null) }
    var showErrorDialog by remember { mutableStateOf<String?>(null) }

    // 目录选择器 (SAF)
    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            // 持久化权限
            context.contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.addDirectory(it.toString())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("笔记目录设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = MaterialTheme.colors.onPrimary
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 说明文字
            Surface(
                color = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "添加包含笔记文件的本地目录，方便管理多个来源的笔记",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // 添加目录按钮
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable { directoryPickerLauncher.launch(null) },
                elevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "添加笔记目录",
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Divider()

            // 目录列表标题
            Text(
                text = "已添加的目录 (${uiState.directories.size})",
                style = MaterialTheme.typography.subtitle2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 目录列表
            if (uiState.directories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暂无已添加的目录",
                            style = MaterialTheme.typography.body1,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.directories) { directory ->
                        val isSelected = directory.path == uiState.selectedDirectoryPath
                        DirectoryItem(
                            directory = directory,
                            isSelected = isSelected,
                            onSelect = {
                                viewModel.selectDirectory(directory.path)
                                onDirectorySelected()
                            },
                            onDelete = { showDeleteDialog = directory }
                        )
                    }
                }
            }
        }
    }

    // 删除确认对话框
    showDeleteDialog?.let { directory ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("确认删除") },
            text = {
                Column {
                    Text("确定要删除目录 \"${directory.name}\" 吗？")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = directory.path,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeDirectory(directory.path)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消")
                }
            }
        )
    }

    // 错误提示对话框
    showErrorDialog?.let { error ->
        AlertDialog(
            onDismissRequest = { showErrorDialog = null },
            title = { Text("提示") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = null }) {
                    Text("确定")
                }
            }
        )
    }

    // 显示错误信息
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            showErrorDialog = error
            viewModel.clearError()
        }
    }
}

/**
 * 目录项组件
 */
@Composable
private fun DirectoryItem(
    directory: NoteDirectory,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        elevation = 1.dp,
        backgroundColor = if (isSelected) {
            MaterialTheme.colors.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colors.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.FolderOpen else Icons.Default.Folder,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = directory.name,
                    style = MaterialTheme.typography.body1,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = directory.path,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "当前激活",
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colors.error
                )
            }
        }
    }
}
