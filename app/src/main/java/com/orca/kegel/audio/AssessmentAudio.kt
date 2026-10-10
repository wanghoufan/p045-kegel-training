package com.orca.kegel.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.annotation.RawRes
import com.orca.kegel.KegelApplication
import com.orca.kegel.R

/**
 * 摸底（自测）用的轻量提示音播放器。
 *
 * 复用训练模块已有的本地语音资产（收紧/放松/完成/3-2-1 倒数）＋每秒 tick 提示音，
 * 让用户在摸底计时时听到引导，不必盯着屏幕。与训练前台服务相互独立。
 * SoundPool 低延迟、离线可用；加载失败静默降级（不阻塞交互）。
 */
object AssessmentAudio {

    private var pool: SoundPool? = null
    private val ids = HashMap<Int, Int>()
    private var warmed = false

    /** 摸底页需要的美音资产（含每秒 tick） */
    @RawRes
    private val needed = intArrayOf(
        R.raw.tick,
        R.raw.cue_tighten,
        R.raw.cue_relax,
        R.raw.cue_done,
        R.raw.cue_num3,
        R.raw.cue_num2,
        R.raw.cue_num1,
    )

    /** 提前建池并预加载，避免首次点击时还在加载、听不到声音 */
    fun warmUp() {
        if (warmed) return
        warmed = true
        pool()
    }

    private fun pool(): SoundPool? {
        pool?.let { return it }
        return runCatching {
            val ctx: Context = KegelApplication.instance
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            val sp = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build()
            needed.forEach { res ->
                runCatching {
                    ctx.resources.openRawResourceFd(res)?.use { afd ->
                        ids[res] = sp.load(afd, 1)
                    }
                }
            }
            pool = sp
            sp
        }.getOrNull()
    }

    fun play(@RawRes res: Int) {
        val sp = pool() ?: return
        val id = ids[res] ?: return
        runCatching { sp.play(id, 1f, 1f, 1, 0, 1f) }
    }
}