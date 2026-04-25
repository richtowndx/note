package com.videonote.presentation.ui.sidebar

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videonote.domain.model.FileTreeNode

/**
 * 侧边栏组件
 * 显示笔记目录的文件树结构
 */
@Composable
fun SidebarDrawer(
    rootNodes: List<FileTreeNode>,
    selectedDirectory: String?,
    selectedDirectoryName: String?,
    isLoading: Boolean,
    onNodeClick: (FileTreeNode) -> Unit,
    onToggleExpand: (FileTreeNode) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(MaterialTheme.colors.surface)
    ) {
        // 顶部标题栏
        Surface(
            color = MaterialTheme.colors.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "笔记目录",
                    color = MaterialTheme.colors.onPrimary,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "设置",
                        tint = MaterialTheme.colors.onPrimary
                    )
                }
            }
        }

        // 当前选中的目录名称
        if (!selectedDirectoryName.isNullOrEmpty()) {
            Surface(
                color = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedDirectoryName,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Divider()

        // 文件列表
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                rootNodes.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暂无笔记目录",
                            style = MaterialTheme.typography.body1,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "点击右上角设置添加笔记目录",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(rootNodes) { node ->
                            FileTreeItem(
                                node = node,
                                depth = 0,
                                onNodeClick = onNodeClick,
                                onToggleExpand = onToggleExpand
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 文件树节点项
 */
@Composable
private fun FileTreeItem(
    node: FileTreeNode,
    depth: Int,
    onNodeClick: (FileTreeNode) -> Unit,
    onToggleExpand: (FileTreeNode) -> Unit
) {
    val indentPadding = (depth * 20).dp

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNodeClick(node) }
                .padding(
                    start = 16.dp + indentPadding,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 展开/折叠图标（仅目录显示）
            if (node.isDirectory) {
                IconButton(
                    onClick = { onToggleExpand(node) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (node.isExpanded) {
                            Icons.Default.ExpandMore
                        } else {
                            Icons.Default.ChevronRight
                        },
                        contentDescription = if (node.isExpanded) "折叠" else "展开",
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(24.dp))
            }

            Spacer(modifier = Modifier.width(4.dp))

            // 文件/目录图标
            Icon(
                imageVector = when {
                    node.isDirectory && node.isExpanded -> Icons.Default.FolderOpen
                    node.isDirectory -> Icons.Default.Folder
                    node.name.endsWith(".md") || node.name.endsWith(".markdown") -> Icons.Default.Description
                    node.name.endsWith(".txt") -> Icons.Default.TextSnippet
                    else -> Icons.Default.InsertDriveFile
                },
                contentDescription = null,
                tint = when {
                    node.isDirectory -> MaterialTheme.colors.primary
                    node.name.endsWith(".md") || node.name.endsWith(".markdown") -> MaterialTheme.colors.secondary
                    else -> MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                },
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 文件/目录名称
            Text(
                text = node.name,
                style = MaterialTheme.typography.body2,
                color = if (node.isDirectory) {
                    MaterialTheme.colors.onSurface
                } else {
                    MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                },
                fontWeight = if (node.isDirectory) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 递归显示子节点
        if (node.isDirectory && node.isExpanded) {
            node.children.forEach { child ->
                FileTreeItem(
                    node = child,
                    depth = depth + 1,
                    onNodeClick = onNodeClick,
                    onToggleExpand = onToggleExpand
                )
            }
        }
    }
}
