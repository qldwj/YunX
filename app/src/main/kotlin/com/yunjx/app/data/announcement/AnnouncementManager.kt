package com.yunjx.app.data.announcement

import android.content.Context
import com.yunjx.app.data.network.backend.BackendApi
import com.yunjx.app.data.network.backend.AnnouncementResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 公告管理器
 * 
 * 功能：
 * 1. 启动时检查公告
 * 2. 24小时内不重复显示（用户关闭后）
 * 3. 用户可永久关闭公告
 */
object AnnouncementManager {

    private const val PREFS_NAME = "announcement_prefs"
    private const val KEY_LAST_DISMISS_TIME = "last_dismiss_time"
    private const val KEY_PERMANENTLY_CLOSED = "permanently_closed"
    
    /** 公告显示间隔：24小时 */
    private const val DISMISS_INTERVAL = 24 * 60 * 60 * 1000L

    /**
     * 检查是否应该显示公告
     * 
     * @return AnnouncementResponse? 需要显示时返回公告内容，不需要显示返回null
     */
    suspend fun checkAnnouncement(context: Context): AnnouncementResponse? = withContext(Dispatchers.IO) {
        // 检查是否永久关闭
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_PERMANENTLY_CLOSED, false)) {
            return@withContext null
        }
        
        // 检查24小时内是否已关闭过
        val lastDismissTime = prefs.getLong(KEY_LAST_DISMISS_TIME, 0)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastDismissTime < DISMISS_INTERVAL) {
            return@withContext null
        }
        
        // 从后端获取公告
        val announcement = BackendApi.getAnnouncement()
        if (announcement != null && announcement.hasAnnouncement) {
            announcement
        } else {
            null
        }
    }

    /**
     * 关闭公告（24小时内不再显示）
     */
    fun dismissAnnouncement(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_DISMISS_TIME, System.currentTimeMillis()).apply()
    }

    /**
     * 永久关闭公告
     */
    fun permanentlyCloseAnnouncement(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PERMANENTLY_CLOSED, true).apply()
    }

    /**
     * 重新开启公告
     */
    fun reopenAnnouncement(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_PERMANENTLY_CLOSED, false)
            .putLong(KEY_LAST_DISMISS_TIME, 0)
            .apply()
    }

    /**
     * 检查是否永久关闭
     */
    fun isPermanentlyClosed(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PERMANENTLY_CLOSED, false)
    }
}
