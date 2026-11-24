package io.github.simon1uo.autopilot.control

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import io.github.simon1uo.autopilot.data.TaskRepository
import io.github.simon1uo.autopilot.engine.CoreActions
import io.github.simon1uo.autopilot.engine.Scheduler
import io.github.simon1uo.autopilot.model.Task
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.io.IOException
import kotlin.concurrent.thread

/**
 * 内嵌 HTTP 控制服务（NanoHTTPD）。默认 0.0.0.0:8080。
 * GET  /api/tasks            -> 列出任务(JSON)
 * POST /api/tasks            -> 新建任务(JSON body)
 * DELETE /api/tasks/{id}     -> 删除
 * POST /api/tasks/{id}/trigger -> 立即触发
 * POST /api/tasks/{id}/toggle  -> 启停
 * GET  /api/health
 */
class HttpServerService : Service() {
    companion object {
        const val TAG = "AutoPilot/HTTP"
        const val PORT = 8080
        const val EXTRA_STOP = "stop"
        fun start(ctx: Context) {
            val i = Intent(ctx, HttpServerService::class.java).putExtra(EXTRA_STOP, false)
            // 需在 App 前台时调用（Android 12+ 后台启动 FGS 会被系统拒绝）。
            try { ctx.startService(i) } catch (e: Exception) { Log.e(TAG, "start failed: ${e.message}") }
        }
        fun stop(ctx: Context) { ctx.stopService(Intent(ctx, HttpServerService::class.java)) }
        private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true }
    }

    private var server: Server? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundCompat()
        server = Server(this).also { runCatching { it.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false) } }
        Log.i(TAG, "HTTP server started on port $PORT")
    }

    private fun startForegroundCompat() {
        val channelId = "autopilot_http"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(channelId) == null) {
                nm.createNotificationChannel(NotificationChannel(channelId, "AutoPilot HTTP", NotificationManager.IMPORTANCE_LOW))
            }
            val n = Notification.Builder(this, channelId)
                .setContentTitle("AutoPilot HTTP 服务运行中")
                .setContentText("端口 $PORT")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()
            startForeground(1001, n)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getBooleanExtra(EXTRA_STOP, false) == true) { stopSelf(); return START_NOT_STICKY }
        return START_STICKY
    }

    override fun onDestroy() { server?.stop(); server = null; super.onDestroy() }

    class Server(private val ctx: Context) : NanoHTTPD(PORT) {
        private val repo get() = TaskRepository(ctx)

        override fun serve(session: IHTTPSession): Response {
            val uri = session.uri.trimEnd('/')
            val method = session.method
            return try {
                when {
                    uri == "/api/health" -> json(200, """{"ok":true,"app":"AutoPilot"}""")
                    uri == "/api/tasks" && method == Method.GET -> {
                        json(200, json.encodeToString(kotlinx.serialization.builtins.ListSerializer(Task.serializer()), repo.load()))
                    }
                    uri == "/api/tasks" && method == Method.POST -> {
                        val body = readBody(session)
                        val t = json.decodeFromString<Task>(body)
                        val list = repo.load(); list.add(t); repo.save(list)
                        if (t.enabled) Scheduler.schedule(ctx, t)
                        json(201, json.encodeToString<Task>(t))
                    }
                    uri.matches(Regex("/api/tasks/\\d+/trigger")) && method == Method.POST -> {
                        val id = uri.split("/")[3].toLong()
                        val t = repo.load().firstOrNull { it.id == id }
                        if (t != null) { thread { kotlinx.coroutines.runBlocking { CoreActions.runTask(ctx.applicationContext, t) } }; json(200, """{"status":"triggered"}""") }
                        else json(404, """{"error":"not found"}""")
                    }
                    uri.matches(Regex("/api/tasks/\\d+/toggle")) && method == Method.POST -> {
                        val id = uri.split("/")[3].toLong()
                        val list = repo.load(); val idx = list.indexOfFirst { it.id == id }
                        if (idx >= 0) {
                            list[idx] = list[idx].copy(enabled = !list[idx].enabled)
                            repo.save(list)
                            if (list[idx].enabled) Scheduler.schedule(ctx, list[idx]) else Scheduler.cancel(ctx, list[idx])
                            json(200, json.encodeToString<Task>(list[idx]))
                        } else json(404, """{"error":"not found"}""")
                    }
                    uri.matches(Regex("/api/tasks/\\d+")) && method == Method.DELETE -> {
                        val id = uri.split("/").last().toLong()
                        val list = repo.load()
                        list.firstOrNull { it.id == id }?.let { Scheduler.cancel(ctx, it) }
                        repo.save(list.filter { it.id != id })
                        json(200, """{"status":"deleted"}""")
                    }
                    else -> json(404, """{"error":"unknown endpoint"}""")
                }
            } catch (e: Exception) {
                json(500, """{"error":"${e.message?.replace("\"","'")}"}""")
            }
        }

        private fun readBody(session: IHTTPSession): String {
            // 直接读取原始输入流并按 UTF-8 解码，避免 NanoHTTPD 按 ISO-8859-1 处理导致中文乱码。
            return try {
                val len = session.headers["content-length"]?.toIntOrNull() ?: 0
                if (len <= 0) return "{}"
                val buf = ByteArray(len)
                var off = 0
                val ins = session.inputStream
                while (off < len) {
                    val n = ins.read(buf, off, len - off)
                    if (n <= 0) break
                    off += n
                }
                String(buf, 0, off, Charsets.UTF_8)
            } catch (_: Exception) { "{}" }
        }

        private fun json(code: Int, body: String): Response {
            val bytes = body.toByteArray(Charsets.UTF_8)
            val resp = newFixedLengthResponse(
                Response.Status.lookup(code) ?: Response.Status.OK,
                "application/json; charset=utf-8",
                bytes.inputStream(),
                bytes.size.toLong()
            )
            return resp
        }
    }
}