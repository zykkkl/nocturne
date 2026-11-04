package com.nocturne.learn.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val version: String,
    val notes: String,
    val apkUrl: String,
    val pageUrl: String,
    val sizeBytes: Long,
)

sealed interface UpdateState {
    data object Idle : UpdateState

    data class Checking(val manual: Boolean) : UpdateState

    data class Available(val info: UpdateInfo) : UpdateState

    data class Downloading(val info: UpdateInfo, val progress: Float) : UpdateState

    data class Ready(val info: UpdateInfo, val file: File) : UpdateState

    data class UpToDate(val version: String) : UpdateState

    data class Failed(val manual: Boolean, val message: String) : UpdateState
}

/**
 * 通过 GitHub Releases 检查、下载新版本，并交给系统安装器。
 *
 * Android 不允许应用静默覆盖安装自己，因此最后一步必须由用户在系统弹窗中确认。
 */
class UpdateManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .build()

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _dialogVisible = MutableStateFlow(false)
    val dialogVisible: StateFlow<Boolean> = _dialogVisible.asStateFlow()

    private var lastAutoCheckAt = 0L

    /** 启动时静默检查；等网络就绪，失败再给一次机会。 */
    suspend fun autoCheck() {
        val now = System.currentTimeMillis()
        if (now - lastAutoCheckAt < AUTO_CHECK_INTERVAL_MS) return
        lastAutoCheckAt = now
        delay(1500)
        check(manual = false)
        if (_state.value is UpdateState.Idle) {
            delay(4000)
            check(manual = false)
        }
    }

    val currentVersion: String
        get() = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()

    suspend fun check(manual: Boolean) {
        if (manual && (_state.value is UpdateState.Ready || _state.value is UpdateState.Available)) {
            _dialogVisible.value = true
            return
        }
        if (_state.value is UpdateState.Checking || _state.value is UpdateState.Downloading) return
        _dialogVisible.value = manual
        _state.value = UpdateState.Checking(manual)
        try {
            val latest = withContext(Dispatchers.IO) { fetchLatest() }
            if (latest != null && isNewer(latest.version, currentVersion)) {
                _state.value = UpdateState.Available(latest)
                _dialogVisible.value = true
            } else {
                _state.value = if (manual) {
                    UpdateState.UpToDate(currentVersion)
                } else {
                    UpdateState.Idle
                }
            }
        } catch (e: Exception) {
            _state.value = if (manual) {
                UpdateState.Failed(true, "检查更新失败 · ${e.friendlyMessage()}")
            } else {
                UpdateState.Idle
            }
        }
    }

    suspend fun download() {
        val info = (_state.value as? UpdateState.Available)?.info ?: return
        _dialogVisible.value = true
        _state.value = UpdateState.Downloading(info, 0f)
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(directory, "nocturne-${info.version}.apk")
        val partial = File(directory, "nocturne-download.part")
        try {
            withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url(info.apkUrl)
                    .header("User-Agent", USER_AGENT)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                    val body = response.body ?: throw IOException("空响应")
                    val total = body.contentLength().takeIf { it > 0 } ?: info.sizeBytes
                    var downloaded = 0L
                    body.byteStream().use { input ->
                        partial.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                val read = input.read(buffer)
                                if (read <= 0) break
                                output.write(buffer, 0, read)
                                downloaded += read
                                val progress = if (total > 0) {
                                    (downloaded.toFloat() / total).coerceIn(0f, 1f)
                                } else {
                                    0f
                                }
                                _state.value = UpdateState.Downloading(info, progress)
                            }
                        }
                    }
                }
                if (partial.length() < 1024L) throw IOException("下载内容不完整")
                if (target.exists()) target.delete()
                if (!partial.renameTo(target)) throw IOException("无法保存安装包")
            }
            _state.value = UpdateState.Ready(info, target)
        } catch (e: Exception) {
            partial.delete()
            _state.value = UpdateState.Failed(true, "下载失败 · ${e.friendlyMessage()}")
        }
    }

    /** 交给系统安装器；若尚未授予「安装未知应用」权限，先跳到对应设置页。 */
    fun install() {
        val ready = _state.value as? UpdateState.Ready ?: return
        try {
            if (Build.VERSION.SDK_INT >= 26 &&
                !context.packageManager.canRequestPackageInstalls()
            ) {
                val settings = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(settings)
                return
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.updates",
                ready.file,
            )
            val install = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(install)
        } catch (e: Exception) {
            _state.value = UpdateState.Failed(true, "无法打开安装器 · ${e.friendlyMessage()}")
        }
    }

    fun dismiss() {
        _dialogVisible.value = false
    }

    private fun fetchLatest(): UpdateInfo? {
        val request = Request.Builder()
            .url(LATEST_RELEASE_API)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) throw IOException("空响应")
            val json = JSONObject(body)
            val version = json.optString("tag_name").removePrefix("v").ifBlank { return null }
            val assets = json.optJSONArray("assets") ?: return null
            var apkUrl = ""
            var size = 0L
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val name = asset.optString("name")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url")
                    size = asset.optLong("size")
                    if (name == "app-release.apk") break
                }
            }
            if (apkUrl.isBlank()) return null
            return UpdateInfo(
                version = version,
                notes = json.optString("body").trim(),
                apkUrl = apkUrl,
                pageUrl = json.optString("html_url"),
                sizeBytes = size,
            )
        }
    }

    private fun isNewer(latest: String, current: String): Boolean {
        val a = versionParts(latest)
        val b = versionParts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val left = a.getOrElse(i) { 0 }
            val right = b.getOrElse(i) { 0 }
            if (left != right) return left > right
        }
        return false
    }

    private fun versionParts(value: String): List<Int> =
        value.trim()
            .removePrefix("v")
            .substringBefore('-')
            .split('.')
            .map { part -> part.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }

    private fun Exception.friendlyMessage(): String =
        when (this) {
            is IOException -> message ?: "网络连接失败"
            else -> message ?: "未知错误"
        }

    private companion object {
        const val LATEST_RELEASE_API = "https://api.github.com/repos/zykkkl/nocturne/releases/latest"
        const val USER_AGENT = "Nocturne-Android"
        const val AUTO_CHECK_INTERVAL_MS = 30 * 60 * 1000L
    }
}
