package com.orca.audiopoc

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * PRE-02 技术样机｜前台媒体服务
 *
 * 覆盖点：
 *  - Media3 MediaSessionService + ExoPlayer：离线本地音频序列播放
 *  - foregroundServiceType=mediaPlayback：播放期间以前台服务运行，锁屏/后台不被回收
 *  - 媒体通知 + 锁屏播控：由 MediaSessionService 默认 provider 提供（播放/暂停）
 *  - 自管音频焦点（不走 media3 默认自动恢复）：任何焦点丢失即暂停，恢复焦点后
 *    仍停在暂停页，必须由用户显式点「继续」——对应 AC-22「不暗中计时、不自动发声」
 *  - setHandleAudioBecomingNoisy(true)：耳机拔出（ACTION_AUDIO_BECOMING_NOISY）即暂停
 *  - 无声（低调）模式：锁屏（ACTION_SCREEN_OFF）立即暂停，解锁后需用户显式继续
 */
class PlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    private lateinit var audioManager: AudioManager

    private val mainHandler = Handler(Looper.getMainLooper())
    private var ticking = false

    /** 每 5 秒在主线程落一条播放时间线，作为锁屏持续播放的单调时钟证据
     *  （ExoPlayer 只允许在创建它的线程访问，必须在主线程读取） */
    private val tickRunnable = object : Runnable {
        override fun run() {
            val p = player
            if (p == null || !ticking) return
            PocState.positionMs.value = p.currentPosition.coerceAtLeast(0)
            PocState.durationMs.value = p.duration.coerceAtLeast(0)
            PocLog.tick(
                "phase=${PlaylistFactory.PHASES.getOrElse(p.currentMediaItemIndex) { "?" }}" +
                    " pos=${p.currentPosition} dur=${p.duration} idx=${p.currentMediaItemIndex}" +
                    " isPlaying=${p.isPlaying}"
            )
            mainHandler.postDelayed(this, 5_000)
        }
    }

    private val focusRequest: AudioFocusRequest by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setOnAudioFocusChangeListener { change -> handleFocusChange(change) }
            .setWillPauseWhenDucked(true)
            .build()
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    PocLog.event("SCREEN_OFF", "silentMode=${PocState.silentMode.value}")
                    if (PocState.silentMode.value) {
                        val p = player
                        if (p != null && (p.playWhenReady || p.isPlaying)) {
                            p.pause()
                            PocState.needsResume.value = true
                            PocState.pauseReason.value = "无声模式锁屏：已自动暂停，解锁后请点「继续」"
                            PocLog.event("SILENT_LOCK_AUTOPAUSE", "锁屏即暂停（无声模式绝不静默跑完）")
                        }
                    }
                }
                Intent.ACTION_USER_PRESENT -> {
                    PocLog.event("USER_PRESENT", "已解锁；needsResume=${PocState.needsResume.value}")
                }
                Intent.ACTION_SCREEN_ON -> {
                    PocLog.event("SCREEN_ON", "")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        PocLog.init(applicationContext)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val p = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                /* handleAudioFocus = */ false // 焦点由本服务自管，禁止自动恢复
            )
            setHandleAudioBecomingNoisy(true)
            setWakeMode(C.WAKE_MODE_NETWORK)
            repeatMode = Player.REPEAT_MODE_ALL
            setMediaItems(PlaylistFactory.build(this@PlaybackService))
            prepare()
        }
        p.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                PocState.isPlaying.value = isPlaying
                PocLog.event("IS_PLAYING", isPlaying.toString())
                if (isPlaying) startTicker() else stopTicker()
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                PocLog.event("PLAY_WHEN_READY", "$playWhenReady reason=$reason(${reasonText(reason)})")
                if (playWhenReady) requestFocusIfNeeded() else abandonFocusIfIdle()
            }

            override fun onPlaybackStateChanged(state: Int) {
                PocLog.event("PLAYBACK_STATE", stateText(state))
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val idx = p.currentMediaItemIndex
                val phase = PlaylistFactory.PHASES.getOrElse(idx) { "?" }
                PocState.phase.value = phase
                PocLog.event("PHASE", "$phase idx=$idx id=${mediaItem?.mediaId}")
            }

            override fun onPlayerError(error: PlaybackException) {
                PocLog.event("PLAYER_ERROR", "${error.errorCodeName}: ${error.message}")
            }
        })

        mediaSession = MediaSession.Builder(this, p)
            .setSessionActivity(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
        player = p

        registerReceiver(
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
        )
        PocLog.event(
            "SERVICE_ONCREATE",
            "mediaSession 已创建；cue=${PlaylistFactory.PHASES.size}；前台上限=mediaPlayback"
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) {
            PocLog.event("TASK_REMOVED", "stopSelf（未在播放）")
            stopSelf()
        } else {
            PocLog.event("TASK_REMOVED", "keep playing in background")
        }
    }

    override fun onDestroy() {
        stopTicker()
        mainHandler.removeCallbacksAndMessages(null)
        runCatching { unregisterReceiver(screenReceiver) }
        runCatching { audioManager.abandonAudioFocusRequest(focusRequest) }
        PocLog.event("SERVICE_ONDESTROY", "")
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null
        super.onDestroy()
    }

    // --- 音频焦点（自管，绝不自动恢复） ---

    private fun requestFocusIfNeeded() {
        val res = audioManager.requestAudioFocus(focusRequest)
        val granted = res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        PocLog.event("FOCUS_REQUEST", "granted=$granted")
        if (!granted) {
            PocState.needsResume.value = true
            PocState.pauseReason.value = "未取得音频焦点，已暂停"
            player?.pause()
        }
    }

    private fun abandonFocusIfIdle() {
        audioManager.abandonAudioFocusRequest(focusRequest)
        PocLog.event("FOCUS_ABANDON", "player paused -> abandon")
    }

    private fun handleFocusChange(change: Int) {
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                val name = focusText(change)
                val p = player
                PocLog.event("FOCUS_LOSS", "$name；当前 isPlaying=${p?.isPlaying}")
                if (p != null && (p.playWhenReady || p.isPlaying)) {
                    p.pause()
                    PocState.needsResume.value = true
                    PocState.pauseReason.value = "音频焦点丢失（$name）：已暂停，需手动「继续」"
                    PocLog.event("FOCUS_LOSS_PAUSE", "已暂停且不自动恢复（AC-22）")
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                PocLog.event("FOCUS_GAIN", "焦点已归还；保持暂停，等用户显式继续")
            }
        }
    }

    private fun focusText(c: Int) = when (c) {
        AudioManager.AUDIOFOCUS_LOSS -> "AUDIOFOCUS_LOSS"
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> "AUDIOFOCUS_LOSS_TRANSIENT"
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK"
        AudioManager.AUDIOFOCUS_GAIN -> "AUDIOFOCUS_GAIN"
        else -> "FOCUS_$c"
    }

    private fun reasonText(reason: Int) = when (reason) {
        Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST -> "USER_REQUEST"
        Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS -> "AUDIO_FOCUS_LOSS"
        Player.PLAY_WHEN_READY_CHANGE_REASON_REMOTE -> "REMOTE"
        Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM -> "END_OF_MEDIA_ITEM"
        else -> "OTHER"
    }

    private fun stateText(s: Int) = when (s) {
        Player.STATE_IDLE -> "IDLE"
        Player.STATE_BUFFERING -> "BUFFERING"
        Player.STATE_READY -> "READY"
        Player.STATE_ENDED -> "ENDED"
        else -> "?"
    }

    private fun startTicker() {
        if (ticking) return
        ticking = true
        mainHandler.postDelayed(tickRunnable, 5_000)
    }

    private fun stopTicker() {
        ticking = false
        mainHandler.removeCallbacks(tickRunnable)
    }
}
