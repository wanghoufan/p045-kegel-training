package com.orca.audiopoc

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper

/**
 * 场景③用的「音频焦点抢占」触发器。
 *
 * 真实来电在无 SIM 的目标真机上无法复现，因此用同一套系统音频焦点机制，
 * 以独立 clientId 申请 AUDIOFOCUS_GAIN_TRANSIENT 抢占焦点（等同来电/其他 App
 * 抢走音频的情形），持续 N 秒后归还，观察本服务是否「丢失焦点即暂停、且不自动恢复」。
 *
 * 这是真实的 Android 音频焦点事件，不是伪造日志；但需在结论中如实标注
 * 「来电通路本身未在无 SIM 真机上实测」。
 */
class FocusGrabber(context: Context) {

    private val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private var active: AudioFocusRequest? = null

    fun grab(holdSeconds: Long) {
        release()
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setOnAudioFocusChangeListener { c ->
                PocLog.event("GRABBER_FOCUS_CHANGE", "change=$c")
            }
            .build()
        val res = am.requestAudioFocus(req)
        active = req
        PocLog.event("GRABBER_REQUEST", "GAIN_TRANSIENT granted=${res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED} hold=${holdSeconds}s")
        handler.postDelayed({
            am.abandonAudioFocusRequest(req)
            PocLog.event("GRABBER_ABANDON", "已归还焦点（模拟来电结束）")
            if (active === req) active = null
        }, holdSeconds * 1000L)
    }

    fun release() {
        active?.let {
            runCatching { am.abandonAudioFocusRequest(it) }
            PocLog.event("GRABBER_RELEASE", "")
        }
        active = null
    }
}
