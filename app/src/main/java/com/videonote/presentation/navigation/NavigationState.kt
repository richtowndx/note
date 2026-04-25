package com.videonote.presentation.navigation

/**
 * 应用程序导航状态管理
 * 用于在主界面和历史记录页面之间共享当前选中的笔记ID
 * 以及处理待处理的文件导入
 */
data class NavigationState(
    var currentTaskId: String? = null,
    var pendingFileImport: com.videonote.presentation.FileImportInfo? = null
)