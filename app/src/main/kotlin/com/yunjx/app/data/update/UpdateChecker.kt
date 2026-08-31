package com.yunjx.app.data.update

import android.content.Context
import com.yunjx.app.data.network.HttpClients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

/**
 * 更新检测器（支持后端API + GitHub双源）
 */
object UpdateChecker {

    /** 后端API地址 */
    private const val BACKEND_VERSION_URL = "https://qlyyz.xyz/yun/version.php"
    
    /** GitHub Release 地址（备用） */
    private const val GITHUB_RELEASES_URL =
        "https://api.github.com/repos/qldwj/YunX/releases/latest"

    /** GitHub 下载加速镜像站前缀 */
    const val MIRROR_PREFIX = "https://cdn.gh-proxy.org/"

    /** 把 GitHub release 直链转成镜像站直链 */
    fun mirrorUrl(url: String): String = MIRROR_PREFIX + url

    data class Asset(
        val name: String,
        val downloadUrl: String
    )

    data class Release(
        val tagName: String,
        val body: String,
        val assets: List<Asset>,
        val publishedAt: String = "",
        val downloadUrl: String = ""
    )

    /** 比较两个版本号 */
    fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.trimStart('v').split(".")
        val parts2 = v2.trimStart('v').split(".")
        val maxLength = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLength) {
            val num1 = parts1.getOrNull(i)?.toIntOrNull() ?: 0
            val num2 = parts2.getOrNull(i)?.toIntOrNull() ?: 0
            if (num1 != num2) return num1 - num2
        }
        return 0
    }

    /** 当前应用版本号 */
    fun currentVersion(context: Context): String =
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"

    /** 当前应用版本号（数字） */
    fun currentVersionCode(context: Context): Int =
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).let {
                @Suppress("DEPRECATION")
                it.versionCode
            }
        }.getOrNull() ?: 1

    /**
     * 通过后端API检查更新
     * 
     * @return Release? 有更新时返回新版本信息，无更新返回null
     */
    suspend fun checkUpdateFromBackend(context: Context): Release? = withContext(Dispatchers.IO) {
        runCatching {
            val currentVersion = currentVersion(context)
            val currentCode = currentVersionCode(context)
            
            val client = HttpClients.apiClient()
            val request = Request.Builder()
                .url("$BACKEND_VERSION_URL?current_version=$currentVersion&version_code=$currentCode")
                .get()
                .build()
            
            val body = client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching null
                resp.body?.string() ?: return@runCatching null
            }
            
            val json = JSONObject(body)
            val hasUpdate = json.optBoolean("has_update", false)
            
            if (hasUpdate) {
                val versionName = json.optString("version_name", "")
                val versionCode = json.optInt("version_code", 0)
                val downloadUrl = json.optString("download_url", "")
                val changelog = json.optString("changelog", "")
                
                Release(
                    tagName = versionName,
                    body = changelog,
                    assets = emptyList(),
                    downloadUrl = downloadUrl
                )
            } else {
                null
            }
        }.getOrNull()
    }

    /**
     * 通过GitHub检查更新（备用）
     */
    suspend fun fetchLatestRelease(): Release? = withContext(Dispatchers.IO) {
        runCatching {
            val client = HttpClients.apiClient()
            val request = Request.Builder()
                .url(GITHUB_RELEASES_URL)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "YunX")
                .get()
                .build()
            val body = client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching null
                resp.body?.string() ?: return@runCatching null
            }
            val json = JSONObject(body)
            val tag = json.optString("tag_name")
            if (tag.isBlank()) return@runCatching null
            val assets = buildList {
                json.optJSONArray("assets")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val a = arr.optJSONObject(i) ?: continue
                        add(Asset(a.optString("name"), a.optString("browser_download_url")))
                    }
                }
            }
            Release(
                tagName = tag,
                body = json.optString("body"),
                assets = assets,
                publishedAt = json.optString("published_at")
            )
        }.getOrNull()
    }
}
