package com.orca.kegel

import android.Manifest
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.orca.kegel.audio.TrainingService
import com.orca.kegel.ui.AppViewModel
import com.orca.kegel.ui.KegelApp
import com.orca.kegel.ui.theme.KegelTheme

class MainActivity : AppCompatActivity() {

    private val vm: AppViewModel by viewModels()

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* 允许或拒绝都可训练 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureMediaController()
        if (Build.VERSION.SDK_INT >= 33) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            val state by vm.state.collectAsState()
            KegelTheme(state.theme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    KegelApp(vm)
                }
            }
        }
    }

    /**
     * 建立进程级 MediaController 连接：让 Media3 前台服务被创建并保活，
     * 锁屏/后台时训练不中断；Activity 重建（主题/语言切换）不会断开。
     */
    private fun ensureMediaController() {
        val app = application as KegelApplication
        if (app.mediaControllerFuture == null) {
            val token = SessionToken(this, ComponentName(this, TrainingService::class.java))
            app.mediaControllerFuture = MediaController.Builder(this, token).buildAsync()
        }
    }
}