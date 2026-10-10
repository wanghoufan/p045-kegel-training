package com.orca.kegel

import android.app.Application
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.ListenableFuture
import com.orca.kegel.data.AppRepository

class KegelApplication : Application() {

    val repository: AppRepository by lazy { AppRepository(applicationContext) }

    /**
     * 进程级 MediaController 连接（保活前台媒体服务）。
     * 放在 Application 上：主题/语言切换导致 Activity 重建时不会断连，
     * 从而保证「切换主题/语言不丢训练状态」。
     */
    var mediaControllerFuture: ListenableFuture<MediaController>? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: KegelApplication
            private set
    }
}