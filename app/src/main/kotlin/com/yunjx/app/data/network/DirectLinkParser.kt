package com.yunjx.app.data.network

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * 直链文件解析器
 * 
 * 功能：
 * 1. 识别URL格式
 * 2. 检查是否为网页（黑名单过滤）
 * 3. 通过代理生成下载链接
 */
object DirectLinkParser {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /** 代理服务器地址 */
    private const val PROXY_BASE_URL = "https://qlyyz.xyz/generate"

    /** 网页文件后缀黑名单 */
    private val WEB_EXTENSIONS = setOf(
        // HTML 系列
        "html", "htm", "shtml", "xhtml",
        // 服务端脚本
        "php", "php3", "php4", "php5", "php7", "phtml", "phps",
        "asp", "aspx", "asa", "asax", "ascx", "ashx", "asmx",
        "jsp", "jspx", "jspa", "jsw", "jsv", "jtml",
        "cgi", "pl", "py", "rb", "lua",
        // 模板引擎
        "cfm", "cfml", "cfc", "tpl", "blade",
        // 数据格式
        "xml", "json", "yaml", "yml", "toml", "ini", "conf",
        "csv", "tsv",
        // 文本文件
        "txt", "md", "markdown", "rst", "log",
        // 样式和脚本
        "css", "scss", "sass", "less",
        "js", "mjs", "cjs", "jsx", "ts", "tsx",
        // 其他网页资源
        "svg", "ico", "map", "manifest", "webmanifest"
    )

    /**
     * 检查URL是否为网页文件
     */
    fun isWebPage(url: String): Boolean {
        val normalizedUrl = normalizeUrl(url)
        val uri = Uri.parse(normalizedUrl)
        
        // 获取路径中的文件名
        val path = uri.path ?: return false
        val lastSegment = path.substringAfterLast("/").lowercase()
        
        // 如果没有文件名或没有后缀，可能是网页
        if (lastSegment.isEmpty() || !lastSegment.contains(".")) {
            // 无后缀情况：检查是否为常见网页路径
            val webPaths = listOf(
                "/", "/index", "/default", "/home", "/main",
                "/login", "/register", "/admin", "/api"
            )
            return webPaths.any { path.lowercase().endsWith(it) || path.lowercase() == it }
        }
        
        // 有后缀：检查是否在黑名单中
        val extension = lastSegment.substringAfterLast(".").lowercase()
        return WEB_EXTENSIONS.contains(extension)
    }

    /**
     * 检查是否为有效的URL格式
     */
    fun isValidUrl(text: String): Boolean {
        val trimmed = text.trim()
        return try {
            val uri = Uri.parse(trimmed)
            uri.scheme?.lowercase() in listOf("http", "https") &&
                !uri.host.isNullOrBlank()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 规范化URL：多个斜杠替换为单个斜杠
     */
    fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        // 替换协议后面的双斜杠（保留 http:// 和 https://）
        val withProtocol = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
        // 规范化路径中的多个斜杠
        val scheme = withProtocol.substringBefore("://")
        val afterScheme = withProtocol.substringAfter("://")
        val normalizedPath = afterScheme.replace(Regex("/+"), "/")
        return "$scheme://$normalizedPath"
    }

    /**
     * 通过代理服务器生成下载链接
     * 
     * @param originalUrl 原始下载链接
     * @return ProxyResult 代理结果
     */
    suspend fun generateProxyLink(originalUrl: String): ProxyResult = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(originalUrl)
            val encodedUrl = URLEncoder.encode(normalizedUrl, "UTF-8")
            val requestUrl = "$PROXY_BASE_URL?url=$encodedUrl"

            val request = Request.Builder()
                .url(requestUrl)
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext ProxyResult(
                success = false,
                error = "服务器无响应"
            )

            // 解析JSON响应
            parseProxyResponse(body)
        } catch (e: Exception) {
            ProxyResult(
                success = false,
                error = "网络错误: ${e.message}"
            )
        }
    }

    /**
     * 解析代理服务器响应
     */
    private fun parseProxyResponse(json: String): ProxyResult {
        return try {
            // 简单解析JSON（不引入Gson依赖）
            val success = json.contains("\"success\": true") || json.contains("\"success\":true")
            val code = extractJsonValue(json, "code")
            val downloadUrl = extractJsonValue(json, "download_url")
            val expiresIn = extractJsonInt(json, "expires_in")
            val targetUrl = extractJsonValue(json, "target_url")
            val error = extractJsonValue(json, "error")

            if (success && !downloadUrl.isNullOrBlank()) {
                ProxyResult(
                    success = true,
                    code = code ?: "",
                    downloadUrl = downloadUrl,
                    expiresIn = expiresIn ?: 1800,
                    targetUrl = targetUrl ?: ""
                )
            } else {
                ProxyResult(
                    success = false,
                    error = error ?: "解析失败"
                )
            }
        } catch (e: Exception) {
            ProxyResult(
                success = false,
                error = "响应解析错误"
            )
        }
    }

    /**
     * 简单提取JSON字段值
     */
    private fun extractJsonValue(json: String, key: String): String? {
        val pattern = """"$key"\s*:\s*"([^"]*)"""".toRegex()
        return pattern.find(json)?.groupValues?.getOrNull(1)
    }

    /**
     * 简单提取JSON整数值
     */
    private fun extractJsonInt(json: String, key: String): Int? {
        val pattern = """"$key"\s*:\s*(\d+)""".toRegex()
        return pattern.find(json)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }
}

/**
 * 代理解析结果
 */
data class ProxyResult(
    val success: Boolean,
    val code: String = "",
    val downloadUrl: String = "",
    val expiresIn: Int = 1800,
    val targetUrl: String = "",
    val error: String? = null
)
