package com.orca.kegel.audio

import androidx.annotation.RawRes
import com.orca.kegel.R
import com.orca.kegel.domain.CueId

/** CueId → res/raw 资产（按当前语言解析：默认 zh-CN，values-en 走 raw-en 限定符） */
object Cues {

    @RawRes
    fun resOf(cue: CueId): Int = when (cue) {
        CueId.READY -> R.raw.cue_ready
        CueId.TIGHTEN -> R.raw.cue_tighten
        CueId.HOLD -> R.raw.cue_hold
        CueId.RELAX -> R.raw.cue_relax
        CueId.REST -> R.raw.cue_rest
        CueId.NEXT -> R.raw.cue_next
        CueId.FAST_TIGHTEN -> R.raw.cue_fast_tighten
        CueId.FAST_RELAX -> R.raw.cue_fast_relax
        CueId.DONE -> R.raw.cue_done
        CueId.FAST_INTRO -> R.raw.cue_fast_intro
        CueId.QUALITY -> R.raw.cue_quality
        CueId.NUM3 -> R.raw.cue_num3
        CueId.NUM2 -> R.raw.cue_num2
        CueId.NUM1 -> R.raw.cue_num1
    }

    val all: List<Pair<CueId, Int>> = CueId.entries.map { it to resOf(it) }

    /** 无声承载音（保持前台媒体服务不被打断；本身完全静音） */
    @get:RawRes
    val carrier: Int = R.raw.silent_loop
}