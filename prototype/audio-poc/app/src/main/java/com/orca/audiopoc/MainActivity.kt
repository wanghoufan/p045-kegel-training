package com.orca.audiopoc

import android.Manifest
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private lateinit var grabber: FocusGrabber

    private val notifPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            PocLog.event("POST_NOTIFICATIONS_RESULT", "granted=$granted")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PocLog.init(applicationContext)
        PocLog.event("ACTIVITY_ONCREATE", "sdk=${Build.VERSION.SDK_INT} model=${Build.MODEL}")
        grabber = FocusGrabber(this)

        if (Build.VERSION.SDK_INT >= 33) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener({
            controller = future.get()
            PocState.connected.value = true
            PocLog.event("CONTROLLER_CONNECTED", "连接 MediaSession 成功")
            startPump()
        }, MoreExecutors.directExecutor())

        setContent {
            PocApp(
                onPlay = { doPlay() },
                onPause = {
                    controller?.pause()
                    PocLog.event("UI_PAUSE", "用户点击暂停")
                },
                onStop = {
                    controller?.pause()
                    controller?.seekTo(0)
                    PocState.needsResume.value = false
                    PocState.pauseReason.value = ""
                    PocLog.event("UI_RESET", "结束并回到开头")
                },
                onToggleSilent = { on -> applySilentMode(on) },
                onGrabFocus = { grabber.grab(8) },
            )
        }
    }

    private fun doPlay() {
        val c = controller ?: run {
            PocLog.event("UI_PLAY", "controller 未连接，忽略")
            return
        }
        PocState.needsResume.value = false
        PocState.pauseReason.value = ""
        if (c.mediaItemCount == 0) {
            c.setMediaItems(PlaylistFactory.build(this))
            c.prepare()
        }
        c.play()
        PocLog.event("UI_PLAY", "play() 请求（silent=${PocState.silentMode.value}）")
    }

    private fun applySilentMode(on: Boolean) {
        PocState.silentMode.value = on
        controller?.volume = if (on) 0f else 1f
        PocLog.event("SILENT_MODE", if (on) "开启（音量=0，锁屏即暂停）" else "关闭（音量=1，锁屏持续播放）")
    }

    private fun startPump() {
        lifecycleScope.launch {
            while (isActive) {
                controller?.let { c ->
                    PocState.isPlaying.value = c.isPlaying
                    PocState.positionMs.value = c.currentPosition.coerceAtLeast(0)
                    PocState.durationMs.value = c.duration.coerceAtLeast(0)
                    if (c.currentMediaItemIndex >= 0) {
                        PocState.phase.value =
                            PlaylistFactory.PHASES.getOrElse(c.currentMediaItemIndex) { "-" }
                    }
                }
                delay(250)
            }
        }
    }

    override fun onDestroy() {
        PocLog.event("ACTIVITY_ONDESTROY", "")
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        super.onDestroy()
    }
}

@Composable
private fun PocApp(
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onToggleSilent: (Boolean) -> Unit,
    onGrabFocus: () -> Unit,
) {
    val connected by PocState.connected.collectAsState()
    val isPlaying by PocState.isPlaying.collectAsState()
    val phase by PocState.phase.collectAsState()
    val pos by PocState.positionMs.collectAsState()
    val dur by PocState.durationMs.collectAsState()
    val silent by PocState.silentMode.collectAsState()
    val needsResume by PocState.needsResume.collectAsState()
    val pauseReason by PocState.pauseReason.collectAsState()
    val log by PocState.recentLog.collectAsState()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("P045 · PRE-02 技术样机", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Kotlin + Compose + Media3(MediaSessionService)",
                    style = MaterialTheme.typography.bodySmall,
                )
                HorizontalDivider()

                Text("会话连接：${if (connected) "已连接" else "连接中…"}")
                Text("播放状态：${if (isPlaying) "▶ 播放中" else "⏸ 未播放"}")
                Text("当前阶段：$phase", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "进度：${pos / 1000}s / ${dur / 1000}s   阶段序号=${phase}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (pauseReason.isNotEmpty()) {
                    Text(
                        pauseReason,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (needsResume) {
                    Text("需要显式点「开始/继续」才会恢复（不自动播放）")
                }

                HorizontalDivider()

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onPlay, modifier = Modifier.weight(1f)) { Text("开始/继续") }
                    OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f)) { Text("暂停") }
                    OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f)) { Text("结束") }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = silent, onCheckedChange = onToggleSilent)
                    Text("  无声模式（锁屏自动暂停）", style = MaterialTheme.typography.bodyMedium)
                }

                OutlinedButton(
                    onClick = onGrabFocus,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("场景③：模拟其他 App 抢占音频焦点（8 秒后归还）") }

                HorizontalDivider()
                Text("最近事件：", style = MaterialTheme.typography.bodySmall)
                log.forEach { line ->
                    Text(
                        line,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                Text(
                    "提示：锁屏/后台持续播放由前台媒体服务 + 媒体通知承载；本 APK 未申请 INTERNET 权限。",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
