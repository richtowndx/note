package com.videonote.data.remote.dto

import com.videonote.domain.model.ModelListItem
import kotlinx.serialization.Serializable

/**
 * AI模型数据传输对象 - 用于与后端API交互的模型信息
 * 用于序列化和反序列化API请求和响应中的模型数据
 */
@Serializable
data class ModelDto(
    /**
     * 模型ID - 模型的唯一标识符
     * 整数类型，用于唯一标识系统中的每个模型
     */
    val id: Int,

    /**
     * 提供商ID - 关联的AI服务提供商标识
     * 字符串类型，指明该模型属于哪个服务提供商
     */
    val provider_id: String,

    /**
     * 模型名称 - AI模型的具体名称
     * 例如: gpt-4, claude-3-opus 等，用于API调用时指定模型
     */
    val model_name: String,

    /**
     * 创建时间 - 模型记录的创建时间
     * 可选字段，ISO 8601格式的字符串，表示模型添加到系统的时间
     */
    val created_at: String? = null
)

/**
 * 模型列表响应数据传输对象 - API返回的模型列表响应结构
 * 用于包装从API获取的模型信息列表
 */
@Serializable
data class ModelListResponse(
    /**
     * 模型信息列表 - 从API获取的模型详细信息数组
     * 包含每个模型的完整信息，如ID、创建时间、所有者等
     */
    val models: List<ModelInfoDto>
)

/**
 * 模型详细信息数据传输对象 - API返回的单个模型详细信息
 * 包含模型在AI服务提供商系统中的完整元数据
 */
@Serializable
data class ModelInfoDto(
    /**
     * 模型标识符 - 在AI服务提供商系统中的唯一ID
     * 通常是字符串格式，如 "gpt-4" 或 "claude-3-opus-20240229"
     */
    val id: String,

    /**
     * 创建时间戳 - 模型在AI服务系统中的创建时间
     * Unix时间戳格式，表示模型创建的精确时间
     */
    val created: Long,

    /**
     * 对象类型 - API响应中的对象类型标识
     * 通常固定为 "model"，表示这是一个模型对象
     */
    val `object`: String,

    /**
     * 所有者 - 模型的所有者标识
     * 表示模型属于哪个组织或用户，如 "openai" 或 "anthropic"
     */
    val owned_by: String,

    /**
     * 权限信息 - 模型的访问权限描述
     * 字符串格式，描述模型的使用权限和访问限制
     */
    val permission: String,

    /**
     * 根模型 - 模型的根标识符
     * 表示模型的基础版本或父模型标识
     */
    val root: String
)

/**
 * 启用的AI模型数据传输对象 - 专门用于getEnabledModels API的响应
 * 用于处理返回的简化模型信息，仅包含必要的字段
 */
@Serializable
data class EnabledModelDto(
    /**
     * 模型ID - 模型的唯一标识符
     * 整数类型，用于唯一标识系统中的每个模型
     */
    val id: Int,

    /**
     * 模型名称 - AI模型的具体名称
     * 例如: gpt-4, claude-3-opus 等，用于API调用时指定模型
     */
    val model_name: String
) {
    /**
     * 转换为业务层模型列表项
     * @param providerId 提供商ID，从API调用参数中获取
     * @return ModelListItem 业务层模型对象
     */
    fun asDomainModel(providerId: String): ModelListItem {
        return ModelListItem(
            id = id.toString(),
            providerId = providerId,
            modelName = model_name,
            createdAt = null
        )
    }
}

/**
 * 创建模型请求数据传输对象 - 向API创建新模型时的请求结构
 * 用于向系统添加新的AI模型配置
 */
@Serializable
data class CreateModelRequest(
    /**
     * 提供商ID - 要创建模型所属的服务提供商标识
     * 字符串类型，指定新模型属于哪个AI服务提供商
     */
    val provider_id: String,

    /**
     * 模型名称 - 要创建的AI模型名称
     * 字符串类型，指定新模型的具体名称或标识符
     */
    val model_name: String
)