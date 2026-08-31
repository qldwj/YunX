package com.yunjx.app.data.network.backend

import com.google.gson.annotations.SerializedName

/**
 * 后端 API 基础地址
 */
object BackendConstants {
    const val BASE_URL = "https://qlyyz.xyz/yun/"
    
    // API 端点
    const val API_ANNOUNCEMENT = "announcement.php"
    const val API_VERSION_CHECK = "version.php"
    const val API_USER_REGISTER = "register.php"
    const val API_USER_LOGIN = "login.php"
    const val API_GET_QQ_AVATAR = "qq_avatar.php"
}

/**
 * 通用 API 响应
 */
data class BaseResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String
) {
    companion object {
        const val SUCCESS = 0
        const val ERROR = -1
    }
}

/**
 * 公告响应
 */
data class AnnouncementResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("title") val title: String,
    @SerializedName("content") val content: String,
    @SerializedName("time") val time: String
)

/**
 * 版本检查响应
 */
data class VersionCheckResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("has_update") val hasUpdate: Boolean,
    @SerializedName("version_name") val versionName: String,
    @SerializedName("version_code") val versionCode: Int,
    @SerializedName("download_url") val downloadUrl: String,
    @SerializedName("changelog") val changelog: String
)

/**
 * 用户登录请求
 */
data class LoginRequest(
    @SerializedName("qq_number") val qqNumber: String,
    @SerializedName("password") val password: String,
    @SerializedName("verify_code") val verifyCode: String
)

/**
 * 用户登录响应
 */
data class LoginResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("user_id") val userId: String?,
    @SerializedName("nickname") val nickname: String?,
    @SerializedName("token") val token: String?
)

/**
 * 用户注册请求
 */
data class RegisterRequest(
    @SerializedName("qq_number") val qqNumber: String,
    @SerializedName("password") val password: String,
    @SerializedName("confirm_password") val confirmPassword: String,
    @SerializedName("verify_code") val verifyCode: String
)

/**
 * 获取验证码响应
 */
data class VerifyCodeResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("verify_code") val verifyCode: String,
    @SerializedName("expire_time") val expireTime: Long
)
