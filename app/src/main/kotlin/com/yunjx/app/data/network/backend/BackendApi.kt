package com.yunjx.app.data.network.backend

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

object BackendApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson: Gson = GsonBuilder().create()

    /** 获取公告 */
    suspend fun getAnnouncement(): AnnouncementResponse? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${BackendConstants.BASE_URL}${BackendConstants.API_ANNOUNCEMENT}")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            gson.fromJson(body, AnnouncementResponse::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** 检查版本更新 */
    suspend fun checkVersion(): VersionCheckResponse? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${BackendConstants.BASE_URL}${BackendConstants.API_VERSION_CHECK}")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            gson.fromJson(body, VersionCheckResponse::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** 生成4位随机验证码 */
    fun generateVerifyCode(): String {
        val random = SecureRandom()
        val code = 1000 + random.nextInt(9000)
        return code.toString()
    }

    /** 用户注册 */
    suspend fun register(
        qqNumber: String,
        password: String,
        confirmPassword: String,
        verifyCode: String
    ): BaseResponse? = withContext(Dispatchers.IO) {
        try {
            val requestBody = FormBody.Builder()
                .add("qq_number", qqNumber)
                .add("password", password)
                .add("confirm_password", confirmPassword)
                .add("verify_code", verifyCode)
                .build()
            val request = Request.Builder()
                .url("${BackendConstants.BASE_URL}${BackendConstants.API_USER_REGISTER}")
                .post(requestBody)
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            gson.fromJson(body, BaseResponse::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** 用户登录 */
    suspend fun login(
        qqNumber: String,
        password: String,
        verifyCode: String
    ): LoginResponse? = withContext(Dispatchers.IO) {
        try {
            val requestBody = FormBody.Builder()
                .add("qq_number", qqNumber)
                .add("password", password)
                .add("verify_code", verifyCode)
                .build()
            val request = Request.Builder()
                .url("${BackendConstants.BASE_URL}${BackendConstants.API_USER_LOGIN}")
                .post(requestBody)
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            gson.fromJson(body, LoginResponse::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** 获取QQ头像URL */
    fun getQQAvatarUrl(qqNumber: String): String {
        return "https://q1.qlogo.cn/g?b=qq&nk=$qqNumber&s=100"
    }
}
