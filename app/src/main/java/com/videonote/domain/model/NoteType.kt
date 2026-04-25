package com.videonote.domain.model

/**
 * 笔记类型枚举 - 区分不同来源的笔记
 * 用于支持多种笔记来源的差异化管理和显示
 */
enum class NoteType {
    /**
     * 视频笔记 - 通过视频链接生成的笔记
     * 支持哔哩哔哩、YouTube、抖音、快手等视频平台
     */
    VIDEO,

    /**
     * 本地文件笔记 - 从本地Markdown/TXT文件导入的笔记
     * 用户直接打开本地文件作为笔记使用
     */
    LOCAL_FILE
}
