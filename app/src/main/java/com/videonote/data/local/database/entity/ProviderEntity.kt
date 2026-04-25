package com.videonote.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * AI服务提供商实体类 - 存储AI模型服务提供商信息的数据库表
 * 对应数据库表名: providers
 */
@Entity(tableName = "providers")
data class ProviderEntity(
    /**
     * 主键 - 提供商唯一标识符
     * 系统生成的唯一ID，用于关联模型和笔记记录
     */
    @PrimaryKey val id: String,

    /**
     * 提供商名称 - 服务提供商的显示名称
     * 例如: OpenAI, Anthropic, 百度文心, 阿里通义等
     */
    val name: String,

    /**
     * 提供商Logo - 服务提供商的图标或标识URL
     * 用于UI显示，可以是网络图片URL或本地资源路径
     */
    val logo: String,

    /**
     * 提供商类型 - 区分内置和自定义提供商
     * 可能的值:
     * - built-in: 系统内置的知名AI服务提供商
     * - custom: 用户自定义添加的AI服务提供商
     */
    val type: String,

    /**
     * API密钥 - 访问AI服务的认证密钥
     * 敏感信息，用于API请求的身份验证
     * 在生产环境中应该加密存储
     */
    val apiKey: String,

    /**
     * 基础URL - AI服务的API基础地址
     * 例如: https://api.openai.com/v1 或自定义服务地址
     */
    val baseUrl: String,

    /**
     * 启用状态 - 提供商是否可用
     * 0: 禁用状态，不可用于生成笔记
     * 1: 启用状态，可用于生成笔记
     */
    val enabled: Int,

    /**
     * 创建时间 - 提供商记录的创建时间
     * 格式为字符串，通常使用ISO 8601格式
     * 例如: "2024-01-01T00:00:00Z"
     */
    val createdAt: String
)