package com.orca.audiopoc

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 进程内共享状态（Service 与 Activity 同进程），供 Compose 观察。
 * 仅用于样机 UI 呈现，不参与任何业务写入。
 */
object PocState {
    val connected = MutableStateFlow(false)
    val isPlaying = MutableStateFlow(false)
    val phase = MutableStateFlow("未开始")
    val positionMs = MutableStateFlow(0L)
    val durationMs = MutableStateFlow(0L)
    /** 无声（低调）模式：文字/计时呈现，不播语音；锁屏即自动暂停且不静默跑完 */
    val silentMode = MutableStateFlow(false)
    /** 需要用户显式点「继续」才恢复的标记（焦点丢失、无声锁屏） */
    val needsResume = MutableStateFlow(false)
    /** 最近一次暂停原因，用于页面提示 */
    val pauseReason = MutableStateFlow("")
    /** 最近 8 条事件，供页面滚动展示（也便于录屏取证） */
    val recentLog = MutableStateFlow<List<String>>(emptyList())
}
