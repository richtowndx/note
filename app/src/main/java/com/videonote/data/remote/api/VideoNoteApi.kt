package com.videonote.data.remote.api

import com.videonote.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * VideoNote API接口定义 - 与后端服务通信的Retrofit接口
 * 定义了所有与VideoNote后端服务交互的API端点
 * 使用Retrofit注解定义HTTP请求方法、路径和参数
 */
interface VideoNoteApi {

    // ==================== 系统健康检查 ====================

    /**
     * 系统健康检查 - 检查后端服务的基本运行状态
     * 用于验证服务是否可用，通常在应用启动时调用
     *
     * @return Response<ApiResponse<Unit>> 包含健康检查结果的响应
     */
    @GET("sys_health")
    suspend fun systemHealth(): Response<ApiResponse<Unit>>

    /**
     * 系统详细检查 - 检查系统的详细功能和配置状态
     * 比健康检查更全面，验证所有系统组件是否正常工作
     *
     * @return Response<ApiResponse<Unit>> 包含系统检查结果的响应
     */
    @GET("sys_check")
    suspend fun systemCheck(): Response<ApiResponse<Unit>>

    // ==================== AI服务提供商管理 ====================

    /**
     * 添加AI服务提供商 - 向系统添加新的AI服务提供商
     * 支持用户自定义添加AI服务，扩展系统的AI模型选择
     *
     * @param request 提供商创建请求，包含名称、API密钥、基础URL等信息
     * @return Response<ApiResponse<String>> 包含新创建提供商ID的响应
     */
    @POST("add_provider")
    suspend fun addProvider(
        @Body request: ProviderRequest
    ): Response<ApiResponse<String>>

    /**
     * 获取所有AI服务提供商 - 获取系统中配置的所有AI服务提供商
     * 用于显示提供商选择列表和管理现有提供商配置
     *
     * @return Response<ApiResponse<List<ProviderDto>>> 包含所有提供商信息的响应
     */
    @GET("get_all_providers")
    suspend fun getAllProviders(): Response<ApiResponse<List<ProviderDto>>>

    /**
     * 根据ID获取AI服务提供商 - 获取指定ID的AI服务提供商详细信息
     * 用于编辑提供商信息或查看提供商配置详情
     *
     * @param id 提供商的唯一标识符
     * @return Response<ApiResponse<ProviderDto>> 包含指定提供商信息的响应
     */
    @GET("get_provider_by_id/{id}")
    suspend fun getProviderById(
        @Path("id") id: String
    ): Response<ApiResponse<ProviderDto>>

    /**
     * 更新AI服务提供商 - 修改现有AI服务提供商的配置信息
     * 用于更新提供商的API密钥、基础URL、启用状态等
     *
     * @param request 提供商更新请求，包含要更新的字段和新值
     * @return Response<ApiResponse<Unit>> 包含更新操作结果的响应
     */
    @POST("update_provider")
    suspend fun updateProvider(
        @Body request: ProviderUpdateRequest
    ): Response<ApiResponse<Unit>>

    /**
     * 测试AI服务提供商连接 - 验证AI服务提供商的配置是否正确可用
     * 用于确认提供商的API密钥和网络连接是否正常工作
     *
     * @param request 连接测试请求，包含要测试的提供商ID
     * @return Response<ApiResponse<Unit>> 包含连接测试结果的响应
     */
    @POST("connect_test")
    suspend fun testConnection(
        @Body request: TestRequest
    ): Response<ApiResponse<Unit>>

    // ==================== AI模型管理 ====================

    /**
     * 获取模型列表 - 获取系统中所有可用的AI模型列表
     * 用于显示用户可以选择的AI模型选项
     *
     * @return Response<ApiResponse<List<ModelDto>>> 包含所有模型信息的响应
     */
    @GET("model_list")
    suspend fun getModelList(): Response<ApiResponse<List<ModelDto>>>

    /**
     * 根据提供商获取模型 - 获取指定AI服务提供商下的所有模型
     * 用于根据用户选择的提供商显示对应的模型列表
     *
     * @param providerId AI服务提供商的唯一标识符
     * @return Response<ApiResponse<ModelListResponse>> 包含指定提供商下所有模型的响应
     */
    @GET("model_list/{provider_id}")
    suspend fun getModelsByProvider(
        @Path("provider_id") providerId: String
    ): Response<ApiResponse<ModelListResponse>>

    /**
     * 创建AI模型 - 在指定提供商下添加新的AI模型
     * 用于扩展系统支持的AI模型选项
     *
     * @param request 模型创建请求，包含提供商ID和模型名称
     * @return Response<ApiResponse<Unit>> 包含模型创建结果的响应
     */
    @POST("models")
    suspend fun createModel(
        @Body request: CreateModelRequest
    ): Response<ApiResponse<Unit>>

    /**
     * 删除AI模型 - 从系统中移除指定的AI模型
     * 用于清理不再使用的模型配置
     *
     * @param modelId 要删除的模型唯一标识符
     * @return Response<ApiResponse<Unit>> 包含模型删除结果的响应
     */
    @GET("models/delete/{model_id}")
    suspend fun deleteModel(
        @Path("model_id") modelId: Int
    ): Response<ApiResponse<Unit>>

    /**
     * 获取启用的模型 - 获取指定提供商下已启用的AI模型列表
     * 用于显示用户实际可以选择和使用的模型
     *
     * @param providerId AI服务提供商的唯一标识符
     * @return Response<ApiResponse<List<EnabledModelDto>>> 包含启用模型列表的响应
     */
    @GET("model_enable/{provider_id}")
    suspend fun getEnabledModels(
        @Path("provider_id") providerId: String
    ): Response<ApiResponse<List<EnabledModelDto>>>

    // ==================== 视频笔记生成 ====================

    /**
     * 生成视频笔记 - 创建视频笔记生成任务（核心功能）
     * 向系统提交视频链接和配置，开始异步处理视频内容分析
     *
     * @param request 视频笔记生成请求，包含视频URL、平台、模型选择等参数
     * @return Response<ApiResponse<TaskIdResponse>> 包含任务ID的响应，用于后续状态查询
     */
    @POST("generate_note")
    suspend fun generateNote(
        @Body request: GenerateNoteRequest
    ): Response<ApiResponse<TaskIdResponse>>

    /**
     * 获取任务状态 - 查询视频笔记生成任务的当前状态和结果
     * 用于跟踪任务进度和获取生成的笔记内容
     *
     * @param taskId 任务的唯一标识符
     * @return Response<ApiResponse<TaskStatusResponse>> 包含任务状态和结果的响应
     */
    @GET("task_status/{task_id}")
    suspend fun getTaskStatus(
        @Path("task_id") taskId: String
    ): Response<ApiResponse<TaskStatusResponse>>

    /**
     * 删除任务 - 删除指定的视频笔记生成任务和相关记录
     * 用于清理用户不再需要的任务记录
     *
     * @param request 删除任务请求，包含视频ID和平台信息
     * @return Response<ApiResponse<Unit>> 包含删除操作结果的响应
     */
    @POST("delete_task")
    suspend fun deleteTask(
        @Body request: RecordRequest
    ): Response<ApiResponse<Unit>>

    // ==================== 系统配置管理 ====================

    /**
     * 获取下载器Cookie - 获取指定视频平台的认证Cookie
     * 用于访问需要登录的视频平台内容，如B站会员视频等
     *
     * @param platform 视频平台标识，如 bilibili, youtube 等
     * @return Response<ApiResponse<CookieResponse>> 包含Cookie信息的响应
     */
    @GET("get_downloader_cookie/{platform}")
    suspend fun getDownloaderCookie(
        @Path("platform") platform: String
    ): Response<ApiResponse<CookieResponse>>

    /**
     * 更新下载器Cookie - 更新指定视频平台的认证Cookie
     * 用于刷新或设置视频平台的访问凭据
     *
     * @param request Cookie更新请求，包含平台标识和新的Cookie字符串
     * @return Response<ApiResponse<Unit>> 包含更新操作结果的响应
     */
    @POST("update_downloader_cookie")
    suspend fun updateDownloaderCookie(
        @Body request: CookieUpdateRequest
    ): Response<ApiResponse<Unit>>
}