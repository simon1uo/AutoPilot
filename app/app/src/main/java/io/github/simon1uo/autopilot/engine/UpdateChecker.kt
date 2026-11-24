package io.github.simon1uo.autopilot.engine

import io.github.simon1uo.autopilot.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * 版本更新检查。
 * 主策略：请求 https://github.com/<repo>/releases/latest 的 302 重定向，
 *         从 Location 头读取最新 tag（无需 API、不受未认证限流影响）。
 * 回退策略：GitHub Releases API。
 */
object UpdateChecker {
    private const val OWNER_REPO = "simon1uo/AutoPilot"
    private const val LATEST_URL = "https://github.com/$OWNER_REPO/releases/latest"
    private const val API = "https://api.github.com/repos/$OWNER_REPO/releases/latest"

    data class Result(
        val hasUpdate: Boolean,
        val currentVersion: String,
        val latestVersion: String,
        val releaseUrl: String,
        val releaseNotes: String = "",
        val error: String? = null,
    )

    suspend fun check(): Result = withContext(Dispatchers.IO) {
        val current = BuildConfig.VERSION_NAME
        // 1) 无 API 重定向方式
        try {
            val tag = latestTagViaRedirect()
            if (tag.isNotBlank()) {
                val hasUpdate = compareVersion(tag, current) > 0
                return@withContext Result(hasUpdate, current, tag, "https://github.com/$OWNER_REPO/releases/tag/v$tag")
            }
        } catch (_: Exception) { /* 落到 API */ }
        // 2) 回退 API
        try {
            val conn = (URL(API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"; connectTimeout = 8000; readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "AutoPilot-Android")
            }
            if (conn.responseCode != 200) {
                val msg = if (conn.responseCode == 403) "GitHub 接口限流，稍后再试" else "HTTP ${conn.responseCode}"
                return@withContext Result(false, current, current, "", "", msg)
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            var tag = ""
            var htmlUrl = "https://github.com/$OWNER_REPO/releases"
            var notes = ""
            val tagM = Regex("\"tag_name\"\\s*:\\s*\"([^\"]+)\"").find(body)
            val urlM = Regex("\"html_url\"\\s*:\\s*\"([^\"]+)\"").find(body)
            if (tagM != null) tag = tagM.groupValues[1].trimStart('v')
            if (urlM != null) htmlUrl = urlM.groupValues[1]
            val hasUpdate = tag.isNotBlank() && compareVersion(tag, current) > 0
            Result(hasUpdate, current, tag, htmlUrl, notes)
        } catch (e: Exception) {
            Result(false, current, current, "", "", e.message ?: "网络错误")
        }
    }

    /** 通过 releases/latest 的 302 重定向拿到最新 tag（无需 API）。 */
    private fun latestTagViaRedirect(): String {
        val conn = (URL(LATEST_URL).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            requestMethod = "GET"; connectTimeout = 8000; readTimeout = 8000
            setRequestProperty("User-Agent", "Mozilla/5.0 (AutoPilot)")
        }
        val code = conn.responseCode
        val loc = conn.getHeaderField("Location") ?: ""
        conn.disconnect()
        if (code in 300..399 && loc.isNotBlank()) {
            // Location 形如 .../releases/tag/v0.3.0
            val m = Regex("/releases/tag/([^/?#]+)").find(loc)
            if (m != null) return m.groupValues[1].trimStart('v')
        }
        if (code == 200) {
            // 极少数情况没有重定向：抓 final URL
            val m = Regex("/releases/tag/([^/?#]+)").find(conn.url.toString())
            if (m != null) return m.groupValues[1].trimStart('v')
        }
        return ""
    }

    fun compareVersion(a: String, b: String): Int {
        val pa = a.split(".", "-").map { it.toIntOrNull() ?: 0 }
        val pb = b.split(".", "-").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val x = pa.getOrElse(i) { 0 }; val y = pb.getOrElse(i) { 0 }
            if (x != y) return x - y
        }
        return 0
    }
}
