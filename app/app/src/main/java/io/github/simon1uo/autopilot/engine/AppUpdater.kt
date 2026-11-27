package io.github.simon1uo.autopilot.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * 应用内自更新：下载 APK → （可选 SHA-256 校验）→ 拉起系统安装器。
 * 注意：Android 安装器自身会校验新 APK 与原应用签名一致，签名不同会被系统拒绝，这是最后一道防线。
 */
object AppUpdater {
    private const val APK_MIME = "application/vnd.android.package-archive"

    /** 下载 APK 到 cacheDir/updates/update.apk */
    suspend fun download(
        ctx: Context,
        url: String,
        expectedSha256: String? = null,
        onProgress: (Int) -> Unit = {},
    ): File = withContext(Dispatchers.IO) {
        require(url.isNotBlank()) { "下载地址为空" }
        val dir = File(ctx.cacheDir, "updates").apply { mkdirs() }
        val out = File(dir, "update.apk")
        if (out.exists()) out.delete()

        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("User-Agent", "AutoPilot-Android")
        }
        try {
            conn.connect()
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                out.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var read: Int
                    var done = 0L
                    var lastPct = -1
                    while (input.read(buf).also { read = it } != -1) {
                        output.write(buf, 0, read)
                        done += read
                        if (total > 0) {
                            val pct = (done * 100 / total).toInt()
                            if (pct != lastPct) { lastPct = pct; onProgress(pct) }
                        }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }

        if (out.length() == 0L) {
            out.delete()
            throw IllegalStateException("下载失败：文件为空")
        }
        if (!expectedSha256.isNullOrBlank()) {
            val actual = sha256(out)
            if (!actual.equals(expectedSha256, ignoreCase = true)) {
                out.delete()
                throw IllegalStateException("APK 校验失败（SHA-256 不匹配）")
            }
        }
        out
    }

    private fun sha256(f: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { ins ->
            val buf = ByteArray(8192)
            var r: Int
            while (ins.read(buf).also { r = it } != -1) md.update(buf, 0, r)
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /** Android 8+ 是否允许本应用「安装未知应用」 */
    fun canInstall(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            ctx.packageManager.canRequestPackageInstalls()
        else true

    /** 跳转「安装未知应用」授权页 */
    fun openInstallPermission(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                ctx.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${ctx.packageName}")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    /** 拉起系统安装器 */
    fun install(ctx: Context, apk: File) {
        val uri: Uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(intent)
    }

    /** 下载并安装（一步到位） */
    suspend fun downloadAndInstall(
        ctx: Context,
        url: String,
        sha256: String? = null,
        onProgress: (Int) -> Unit = {},
    ) {
        val apk = download(ctx, url, sha256, onProgress)
        withContext(Dispatchers.Main) { install(ctx, apk) }
    }
}
