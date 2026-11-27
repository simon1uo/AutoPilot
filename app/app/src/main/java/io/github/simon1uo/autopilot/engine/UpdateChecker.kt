package io.github.simon1uo.autopilot.engine

import io.github.simon1uo.autopilot.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * 版本更新检查 + 应用内自更新信息。
 * 主策略：GitHub Releases API（同时拿到 tag 和 APK 下载地址）。
 * 回退策略：releases/latest 的 302 重定向拿 tag（仅用于版本比较）。
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
        val apkUrl: String = "",
        val apkSha256: String? = null,
        val releaseNotes: String = "",
        val error: String? = null,
    )

    suspend fun check(): Result = withContext(Dispatchers.IO) {
        val current = BuildConfig.VERSION_NAME
        try {
            viaApi(current)?.let { return@withContext it }
        } catch (_: Exception) { }
        try {
            val tag = latestTagViaRedirect()
            if (tag.isNotBlank()) {
                val hasUpdate = compareVersion(tag, current) > 0
                return@withContext Result(hasUpdate, current, tag, "https://github.com/$OWNER_REPO/releases/tag/v$tag")
            }
        } catch (_: Exception) { }
        Result(false, current, current, "", error = "网络错误，请稍后再试")
    }

    private fun viaApi(current: String): Result? {
        val conn = (URL(API).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 8000; readTimeout = 8000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "AutoPilot-Android")
        }
        val code = conn.responseCode
        if (code != 200) {
            conn.disconnect()
            return if (code == 403) Result(false, current, current, "", error = "GitHub 接口限流，稍后再试") else null
        }
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()

        val tag = Regex("""\x22tag_name\x22\s*:\s*\x22([^\x22]+)\x22""")
            .find(body)?.groupValues?.get(1)?.trimStart('v') ?: return null
        val htmlUrl = Regex("""\x22html_url\x22\s*:\s*\x22([^\x22]+)\x22""")
            .find(body)?.groupValues?.get(1) ?: "https://github.com/$OWNER_REPO/releases"

        // APK 资产（优先 release 且非 debug）
        val assets = Regex("""\x22name\x22\s*:\s*\x22([^\x22]+\.apk)\x22[\s\S]*?\x22browser_download_url\x22\s*:\s*\x22([^\x22]+)\x22""")
            .findAll(body)
            .map { it.groupValues[1] to it.groupValues[2] }
            .toList()
        val chosen = assets.firstOrNull { it.first.contains("release", true) && !it.first.contains("debug", true) }
            ?: assets.firstOrNull { !it.first.contains("debug", true) }
            ?: assets.firstOrNull()

        val hasUpdate = compareVersion(tag, current) > 0
        return Result(hasUpdate, current, tag, htmlUrl, chosen?.second ?: "")
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
            val m = Regex("/releases/tag/([^/?#]+)").find(loc)
            if (m != null) return m.groupValues[1].trimStart('v')
        }
        if (code == 200) {
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
