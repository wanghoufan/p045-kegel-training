package com.orca.audiopoc

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/**
 * 离线语音序列清单。每个 cue 都是打包进 APK 的本地 WAV（res/raw），
 * 用 `android.resource://` URI 交给 ExoPlayer 播放，全程不触网。
 * 单次序列约 63 秒，播放器设为 REPEAT_MODE_ALL 以覆盖「锁屏 ≥1 分钟」场景。
 */
object PlaylistFactory {

    val PHASES = listOf(
        "准备", "收紧", "保持", "完全放松", "休息", "下一次", "快速收紧", "快速放松"
    )

    private val RAW = listOf(
        R.raw.cue01_ready,
        R.raw.cue02_tighten,
        R.raw.cue03_hold,
        R.raw.cue04_relax,
        R.raw.cue05_rest,
        R.raw.cue06_next,
        R.raw.cue07_quick_tighten,
        R.raw.cue08_quick_relax,
    )

    fun build(ctx: Context): List<MediaItem> = RAW.mapIndexed { i, resId ->
        MediaItem.Builder()
            .setUri("android.resource://${ctx.packageName}/$resId")
            .setMediaId("cue_%02d".format(i + 1))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(PHASES[i])
                    .setArtist("PRE-02 技术样机｜离线语音序列")
                    .build()
            )
            .build()
    }
}
