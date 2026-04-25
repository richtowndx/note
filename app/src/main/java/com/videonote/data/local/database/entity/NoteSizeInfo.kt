package com.videonote.data.local.database.entity

import androidx.room.ColumnInfo

/**
 * 笔记大小信息 - 用于分析数据库记录的字段长度
 */
data class NoteSizeInfo(
    val id: String,
    @ColumnInfo(name = "markdown_size") val markdownSize: Int,
    @ColumnInfo(name = "original_size") val originalSize: Int,
    @ColumnInfo(name = "transcript_size") val transcriptSize: Int
) {
    val totalSize: Int get() = markdownSize + originalSize + transcriptSize
}