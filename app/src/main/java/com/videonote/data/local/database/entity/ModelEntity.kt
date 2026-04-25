package com.videonote.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * AI模型实体类 - 存储AI服务提供商提供的具体模型信息的数据库表
 * 对应数据库表名: models
 */
@Entity(tableName = "models")
data class ModelEntity(
    /**
     * 主键 - 模型唯一标识符
     * 整数类型的自增主键，用于唯一标识每个模型记录
     */
    @PrimaryKey val id: Int,

    /**
     * 提供商ID - 关联的AI服务提供商标识
     * 外键关联到providers表，指明该模型属于哪个提供商
     */
    val providerId: String,

    /**
     * 模型名称 - AI模型的具体名称或标识
     * 例如: gpt-4, gpt-3.5-turbo, claude-3-opus, text-davinci-003 等
     * 用于API请求时指定使用的具体模型
     */
    val modelName: String,

    /**
     * 创建时间 - 模型记录的创建时间
     * 可选字段，格式为字符串，通常使用ISO 8601格式
     * 例如: "2024-01-01T00:00:00Z"
     * 用于追踪模型添加到系统的时间
     */
    val createdAt: String? = null
)