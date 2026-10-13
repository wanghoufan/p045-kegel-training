package com.orca.kegel.audio

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.SoundPool
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.orca.kegel.KegelApplication
import com.orca.kegel.MainActivity
import com.orca.kegel.R
import com.orca.kegel.domain.CueId
import com.orca.kegel.domain.DayUsage
import com.orca.kegel.domain.PauseReason
import com.orca.kegel.domain.Phase
import com.orca.kegel.domain.SafetyReason
import com.orca.kegel.domain.ScriptBuilder
import com.orca.kegel.domain.SessionPlan
import com.orca.kegel.domain.SessionRecord
import com.orca.kegel.domain.SessionScript
import com.orca.kegel.domain.TrainingStage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 训练前台服务（Media3 MediaSessionService）
 *
 * 设计要点：
 *  - 无声承载音（res/raw/silent_loop）持续播放，使服务保持 mediaPlayback 前台态：
 *    锁屏/后台不会被回收，媒体通知与锁屏播控可用；承载音本身完全静音。
 *  - 提示音用 SoundPool 低延迟播放，与承载音互不干扰；低调模式下不播放任何提示音。
 *  - 计时以 Handler/协程时钟为主时钟推进相位；语音只是提示，不让语音延迟拖节奏。
 *  - 音频焦点自管：任何焦点丢失即暂停，恢复需用户显式点「继续」（不会自动发声）。
 *  - 耳机拔出（becoming noisy）即暂停；低调模式锁屏即暂停。
 *  - 安全出口分类（疼痛/排尿排便困难/无法放松）停止后不可恢复。
 */
class TrainingService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var soundPool: SoundPool? = null
    private val cueSounds = mutableMapOf<CueId, Int>()

    private lateinit var audioManager: AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private var hasFocus = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var engineJob: Job? = null

    private var plan: SessionPlan? = null
    private var script: SessionScript? = null
    private var enduranceDone = 0
    private var fastDone = 0

    /** 实际已开始（进入 HOLD / QUICK_TIGHTEN 即扣额，中止不返还） */
    private var enduranceStarted = 0
    private var fastStarted = 0
    /** 按收缩开始时刻归属本地日的分桶（跨午夜正确扣额） */
    private val enduranceStartedByDay = mutableMapOf<Long, Int>()
    private val fastStartedByDay = mutableMapOf<Long, Int>()
    private var countedUpTo = 0

    private val repo get() = KegelApplication.instance.repository

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    if (TrainingState.silentMode.value && TrainingState.stage.value == TrainingStage.RUNNING) {
                        pauseEngine(PauseReason.SCREEN_OFF)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelName(R.string.notif_channel_name)
                .build()
        )

        val p = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(
                Media3AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                /* handleAudioFocus = */ false // 焦点由本服务自管，禁止自动恢复
            )
            setHandleAudioBecomingNoisy(true)
            setWakeMode(C.WAKE_MODE_NETWORK)
            repeatMode = Player.REPEAT_MODE_ALL
            setMediaItem(carrierItem())
            prepare()
        }
        p.addListener(object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady &&
                    reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY &&
                    TrainingState.stage.value == TrainingStage.RUNNING
                ) {
                    pauseEngine(PauseReason.NOISY)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                // 承载音失败不阻塞训练推进（本地资源，正常不会发生）
                TrainingState.stage.value.let { }
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

        buildSoundPool()

        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF) },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        scope.launch {
            TrainingBus.commands.collect { command -> handle(command) }
        }
    }

    private fun carrierItem(): MediaItem = MediaItem.Builder()
        .setUri("android.resource://$packageName/${Cues.carrier}")
        .setMediaId("carrier")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(getString(R.string.notif_title))
                .setArtist(getString(R.string.session_title))
                .build()
        )
        .build()

    private fun buildSoundPool() {
        val attrs = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val sp = SoundPool.Builder().setMaxStreams(3).setAudioAttributes(attrs).build()
        Cues.all.forEach { (cue, res) ->
            runCatching {
                resources.openRawResourceFd(res)?.use { afd ->
                    cueSounds[cue] = sp.load(afd, 1)
                }
            }
        }
        soundPool = sp
    }

    // ---------- 命令处理 ----------

    private fun handle(command: TrainingCommand) {
        when (command) {
            is TrainingCommand.Start -> startSession(command.plan, command.silent)
            TrainingCommand.Pause -> if (TrainingState.stage.value == TrainingStage.RUNNING) pauseEngine(PauseReason.MANUAL)
            TrainingCommand.Resume -> resumeEngine()
            TrainingCommand.Stop -> finishSession(completed = false, safety = null)
            is TrainingCommand.SafetyExit -> finishSession(completed = false, safety = command.reason)
            is TrainingCommand.SetSilent -> {
                TrainingState.silentMode.value = command.silent
                if (command.silent) soundPool?.autoPause()
            }
            is TrainingCommand.ExtendRest -> {
                if (TrainingState.stage.value == TrainingStage.RUNNING) {
                    if (script?.extendCurrent(command.extraMs) == true) pushStepState()
                }
            }
        }
    }

    private fun startSession(newPlan: SessionPlan, silent: Boolean) {
        val current = TrainingState.stage.value
        if (current == TrainingStage.RUNNING || current == TrainingStage.PAUSED) return

        plan = newPlan
        enduranceDone = 0
        fastDone = 0
        enduranceStarted = 0
        fastStarted = 0
        enduranceStartedByDay.clear()
        fastStartedByDay.clear()
        countedUpTo = 0
        TrainingState.silentMode.value = silent
        TrainingState.level.value = newPlan.level
        TrainingState.plan.value = newPlan
        TrainingState.outcome.value = null
        TrainingState.pauseReason.value = PauseReason.NONE

        val steps = ScriptBuilder.build(newPlan)
        val s = SessionScript(steps)
        script = s
        TrainingState.stepCount.value = steps.size
        TrainingState.stage.value = TrainingStage.RUNNING

        startCarrier()
        requestFocusIfNeeded()
        s.start()?.let { playCue(it) }
        countEnteredSteps(s)
        pushStepState()
        startEngineLoop()
    }

    /** 进入某一步即视为该次收缩已开始（HOLD / QUICK_TIGHTEN），据此扣当日额度。 */
    private fun countEnteredSteps(s: SessionScript) {
        val steps = s.steps
        if (steps.isEmpty()) return
        val upto = minOf(s.index, steps.size - 1)
        var i = countedUpTo
        while (i <= upto) {
            when (steps[i].phase) {
                Phase.HOLD -> {
                    enduranceStarted++
                    enduranceStartedByDay.merge(LocalDate.now().toEpochDay(), 1, Int::plus)
                }
                Phase.QUICK_TIGHTEN -> {
                    fastStarted++
                    fastStartedByDay.merge(LocalDate.now().toEpochDay(), 1, Int::plus)
                }
                else -> Unit
            }
            i++
        }
        countedUpTo = upto + 1
    }

    private fun startEngineLoop() {
        engineJob?.cancel()
        engineJob = scope.launch {
            var last = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(TICK_MS)
                val now = SystemClock.elapsedRealtime()
                val delta = now - last
                last = now
                val s = script ?: break
                val result = s.tick(delta)
                result.completed.forEach { step ->
                    if (step.phase == Phase.RELAX) enduranceDone++
                    if (step.phase == Phase.QUICK_RELAX) fastDone++
                }
                countEnteredSteps(s)
                result.cues.forEach { playCue(it) }
                pushStepState()
                if (result.finished) {
                    finishSession(completed = true, safety = null)
                    break
                }
            }
        }
    }

    private fun pushStepState() {
        val s = script ?: return
        TrainingState.stepIndex.value = s.index
        val step = s.current()
        if (step == null) {
            TrainingState.remainingMs.value = 0
            return
        }
        TrainingState.phase.value = step.phase
        TrainingState.section.value = step.section
        TrainingState.repIndex.value = step.repIndex
        TrainingState.repTotal.value = step.repTotal
        TrainingState.groupIndex.value = step.groupIndex
        TrainingState.groupTotal.value = step.groupTotal
        TrainingState.stepDurationMs.value = step.durationMs
        TrainingState.remainingMs.value = (step.durationMs - s.elapsedInStepMs).coerceAtLeast(0)
    }

    private fun pauseEngine(reason: PauseReason) {
        if (TrainingState.stage.value != TrainingStage.RUNNING) return
        engineJob?.cancel()
        engineJob = null
        soundPool?.autoPause()
        TrainingState.stage.value = TrainingStage.PAUSED
        TrainingState.pauseReason.value = reason
    }

    private fun resumeEngine() {
        if (TrainingState.stage.value != TrainingStage.PAUSED) return
        TrainingState.stage.value = TrainingStage.RUNNING
        TrainingState.pauseReason.value = PauseReason.NONE
        requestFocusIfNeeded()
        startCarrier()
        startEngineLoop()
        pushStepState()
    }

    private fun finishSession(completed: Boolean, safety: SafetyReason?) {
        engineJob?.cancel()
        engineJob = null

        val p = plan
        val s = script
        val elapsedSec = ((s?.elapsedTotalMs() ?: 0L) / 1000L).toInt()
        val outcome = p?.let {
            SessionOutcome(
                level = it.level,
                enduranceReps = enduranceDone,
                enduranceHoldSeconds = enduranceDone * it.holdSec,
                fastReps = fastDone,
                durationSeconds = elapsedSec,
                completed = completed,
                safetyReason = safety,
                halfDose = it.halfDose,
            )
        }
        TrainingState.outcome.value = outcome
        TrainingState.stage.value = if (safety != null) TrainingStage.SAFETY_STOPPED else TrainingStage.FINISHED
        TrainingState.remainingMs.value = 0
        TrainingState.stepDurationMs.value = 0
        stopCarrier()
        soundPool?.autoPause()
        abandonFocus()

        plan = null
        script = null

        if (outcome != null) {
            val today = LocalDate.now().toEpochDay()
            val buckets = (enduranceStartedByDay.keys + fastStartedByDay.keys)
                .map { d -> DayUsage(d, enduranceStartedByDay[d] ?: 0, fastStartedByDay[d] ?: 0) }
                .sortedBy { it.epochDay }
            val record = SessionRecord(
                epochDay = today,
                timestampMs = System.currentTimeMillis(),
                level = outcome.level,
                enduranceReps = outcome.enduranceReps,
                enduranceHoldSeconds = outcome.enduranceHoldSeconds,
                fastReps = outcome.fastReps,
                durationSeconds = outcome.durationSeconds,
                completed = outcome.completed,
                safetyReason = outcome.safetyReason,
                halfDose = outcome.halfDose,
                enduranceStarted = enduranceStarted,
                fastStarted = fastStarted,
                doseVersion = p?.doseVersion ?: SessionPlan.DOSE_VERSION,
                startedByDay = buckets,
            )
            scope.launch { runCatching { repo.addSession(record) } }
        }

        // 疲劳停止 → 次日自动整日半量（仅次日生效，不影响当日已冻结额度）
        if (safety == SafetyReason.FATIGUE) {
            val nextDay = LocalDate.now().toEpochDay() + 1
            scope.launch { runCatching { repo.scheduleHalfDose(nextDay) } }
        }
    }

    // ---------- 音频 ----------

    private fun playCue(cue: CueId) {
        if (TrainingState.silentMode.value) return
        val sp = soundPool ?: return
        val id = cueSounds[cue] ?: return
        runCatching { sp.play(id, 1f, 1f, 1, 0, 1f) }
    }

    private fun startCarrier() {
        val p = player ?: return
        if (p.mediaItemCount == 0) {
            p.setMediaItem(carrierItem())
            p.prepare()
        }
        p.play()
    }

    private fun stopCarrier() {
        player?.pause()
    }

    private fun buildFocusRequest(): AudioFocusRequest =
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener { change ->
                when (change) {
                    AudioManager.AUDIOFOCUS_LOSS,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        hasFocus = false
                        if (TrainingState.stage.value == TrainingStage.RUNNING) {
                            pauseEngine(PauseReason.FOCUS_LOST)
                        }
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> hasFocus = true
                }
            }
            .build()

    private fun requestFocusIfNeeded() {
        if (hasFocus) return
        val req = focusRequest ?: buildFocusRequest().also { focusRequest = it }
        hasFocus = audioManager.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        focusRequest?.let { runCatching { audioManager.abandonAudioFocusRequest(it) } }
        hasFocus = false
    }

    // ---------- 生命周期 ----------

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (TrainingState.stage.value == TrainingStage.RUNNING ||
            TrainingState.stage.value == TrainingStage.PAUSED
        ) {
            finishSession(completed = false, safety = null)
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        engineJob?.cancel()
        scope.cancel()
        runCatching { unregisterReceiver(screenReceiver) }
        soundPool?.release()
        soundPool = null
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        abandonFocus()
        super.onDestroy()
    }

    companion object {
        private const val TICK_MS = 100L
    }
}