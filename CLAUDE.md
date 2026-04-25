# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

**NoteTTS** 是一个Android笔记阅读应用，支持本地Markdown/TXT文件导入和TTS（文本转语音）朗读功能。采用 Clean Architecture 架构。

## 构建命令

```bash
# 构建Debug APK
./gradlew assembleDebug

# 构建Release APK
./gradlew assembleRelease

# 运行测试
./gradlew test

# 清理构建
./gradlew clean

# 运行单测试类（示例）
./gradlew test --tests "com.videonote.ExampleUnitTest"
```

## 技术架构

### 分层结构

```
app/src/main/java/com/videonote/
├── domain/           # 领域层：业务模型和仓库接口
│   ├── model/        # 业务模型（Note, FileTreeNode, TTSSettings 等）
│   └── repository/    # 仓库接口（VideoNoteRepository）
├── data/             # 数据层：实现仓库接口
│   ├── local/        # 本地数据（Room数据库、DAOs、Entities）
│   ├── remote/       # 远程数据（Retrofit API、DTOs）
│   ├── repository/   # 仓库实现
│   ├── tts/          # TTS引擎实现
│   └── preferences/  # SharedPreferences存储
└── presentation/     # 表现层：UI和ViewModel
    ├── screens/      # 页面组件（MainScreen, HistoryScreen, DirectorySettingsScreen）
    ├── viewmodel/    # ViewModels
    ├── navigation/   # Compose导航
    └── ui/          # UI组件（SidebarDrawer, TTSPlaybackBar, Markdown渲染器）
```

### 核心依赖

- **DI**: Hilt (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@Inject`)
- **UI**: Jetpack Compose + Material
- **数据库**: Room（本地笔记存储）
- **导航**: Navigation Compose
- **文件**: DocumentFile（SAF目录访问）

### Hilt 模块

- `DataModule`: 提供 Room 数据库、DAOs、Repository 绑定

### 数据存储

- **NotePreferences**: SharedPreferences存储
  - 笔记目录列表（NoteDirectory列表）
  - 当前选中的目录路径
  - 全局TTS设置（语速、音调、语音）

### 数据流

1. UI 层通过 ViewModel 调用 `VideoNoteRepository` 接口
2. Repository 实现在 `data/repository/` 中，操作 Room 数据库
3. Domain 层定义模型和接口，Data 层实现

## 核心功能

### 侧边栏文件管理

- **SidebarDrawer**: 左侧滑出侧边栏，显示目录树形结构
- **FileTreeLoader**: 扫描目录构建文件树
- 支持.md、.txt、.markdown文件

### 目录设置

- **DirectorySettingsScreen**: 管理笔记目录
- 支持添加/删除本地目录（通过SAF）
- 目录配置持久化到SharedPreferences

### TTS朗读

- **TTSEngine**: TTS 接口定义
- **AndroidTTSEngine**: Android 原生 TTS 实现
- **TTSViewModel**: TTS 状态和播放控制
- 支持语速、音调、语音选择
- 全局TTS设置存储在NotePreferences

### 笔记历史

- **HistoryScreen**: 本地文件笔记历史记录
- 支持删除笔记

## 关键文件

| 文件 | 描述 |
|------|------|
| `MainScreen.kt` | 主页面，包含侧边栏和TTS控制 |
| `SidebarDrawer.kt` | 侧边栏组件 |
| `DirectorySettingsScreen.kt` | 目录管理设置页面 |
| `MainViewModel.kt` | 主页ViewModel |
| `SidebarViewModel.kt` | 侧边栏ViewModel |
| `DirectorySettingsViewModel.kt` | 目录设置ViewModel |
| `TTSViewModel.kt` | TTS播放控制ViewModel |
| `FileTreeLoader.kt` | 文件树加载工具 |
| `NotePreferences.kt` | SharedPreferences管理 |

## 注意事项

- 使用SAF（Storage Access Framework）访问目录，需要用户授权
- `AndroidManifest.xml` 配置了 `configChanges="orientation|screenSize"`
- TTS 在 `onDestroy` 时仅在 `isFinishing=true` 时停止
- Release 构建使用签名配置 `signingConfigs.release`
