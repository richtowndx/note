package com.videonote.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * 通用API响应数据传输对象 - 标准化的API响应格式
 * 用于包装所有API调用的响应结果，提供统一的响应结构
 *
 * @param <T> 响应数据的泛型类型，具体数据根据不同的API端点而变化
 */
@Serializable
data class ApiResponse<T>(
    /**
     * 响应状态码 - API调用的结果状态码
     * 通常的值:
     * - 200: 请求成功
     * - 400: 请求参数错误
     * - 401: 未授权访问
     * - 403: 禁止访问
     * - 404: 资源不存在
     * - 500: 服务器内部错误
     */
    val code: Int,

    /**
     * 响应消息 - API调用的结果描述信息
     * 成功时通常返回"success"或具体的成功描述
     * 失败时返回具体的错误原因描述
     */
    val msg: String,

    /**
     * 响应数据 - API返回的具体业务数据
     * 泛型类型，根据不同的API端点返回不同的数据结构
     * 可能为null，特别是在某些失败情况下没有具体数据返回
     */
    val data: T?
)

/**
 * 任务ID响应数据传输对象 - 创建任务后返回的任务标识符
 * 用于异步任务处理，返回任务ID供后续状态查询使用
 */
@Serializable
data class TaskIdResponse(
    /**
     * 任务ID - 异步处理任务的唯一标识符
     * 用于后续查询任务状态和获取处理结果
     * 字符串格式，通常是UUID或系统生成的唯一标识
     */
    val task_id: String
)