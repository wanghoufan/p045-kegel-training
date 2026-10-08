package com.orca.audiopoc

import android.content.Context
import android.os.SystemClock
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 样机事件日志：同时写 logcat 与 App 私有目录文件，
 * 供真机取证时用 `adb exec-out run-as ... cat files/poc-events.txt` 回捞。
 *
 * 每行格式：`HH:mm:ss.SSS | <epochMs> | <elapsedMs> | NAME | detail`
 * 其中 elapsedMs = SystemClock.elapsedRealtime()，为单调时钟，用于证明锁屏期间
 * 音频时间线持续推进（不受系统时间调整影响）。
 */
object PocLog {
    const val TAG = "ORCA-POC"

    private val wall = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    @Volatile private var file: File? = null

    @Synchronized
    fun init(ctx: Context) {
        if (file == null) {
            val f = File(ctx.filesDir, "poc-events.txt")
            if (!f.exists()) f.writeText("")
            file = f
            event("LOG_INIT", f.absolutePath)
        }
    }

    fun event(name: String, detail: String) = write(name, detail)

    fun tick(detail: String) = write("TICK", detail)

    @Synchronized
    private fun write(name: String, detail: String) {
        val now = System.currentTimeMillis()
        val line = "${wall.format(Date(now))} | $now | ${SystemClock.elapsedRealtime()} | $name | $detail"
        Log.i(TAG, line)
        runCatching { file?.appendText(line + "\n") }
        runCatching {
            PocState.recentLog.value = (PocState.recentLog.value + line).takeLast(8)
        }
    }
}
