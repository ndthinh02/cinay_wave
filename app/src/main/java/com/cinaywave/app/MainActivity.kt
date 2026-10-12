package com.cinaywave.app

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.cinaywave.app.ui.WaveApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val playback get() = (application as WaveApplication).playback
    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.cinaywave.app.PIP_TOGGLE") playback.toggle()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(
            pipReceiver,
            IntentFilter("com.cinaywave.app.PIP_TOGGLE"),
            RECEIVER_NOT_EXPORTED
        )
        else @Suppress("DEPRECATION") registerReceiver(pipReceiver, IntentFilter("com.cinaywave.app.PIP_TOGGLE"))
        lifecycleScope.launch {
            playback.state.collect {
                if (it.current != null && packageManager.hasSystemFeature(
                        PackageManager.FEATURE_PICTURE_IN_PICTURE
                    )
                ) setPictureInPictureParams(pipParams())
            }
        }
        setContent { WaveApp((application as WaveApplication).repository, playback, ::enterPip, ::setFullscreen) }
    }

    private fun pipParams(): PictureInPictureParams {
        val playing = playback.state.value.playing
        val action = RemoteAction(
            Icon.createWithResource(
                this,
                if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            ),
            if (playing) "Tạm dừng" else "Phát",
            "Phát / tạm dừng",
            PendingIntent.getBroadcast(
                this,
                0,
                Intent("com.cinaywave.app.PIP_TOGGLE").setPackage(packageName),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        return PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).setActions(listOf(action))
            .apply { if (Build.VERSION.SDK_INT >= 31) setSeamlessResizeEnabled(true) }.build()
    }

    fun enterPip() {
        if (playback.state.value.current == null || !packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return
        playback.save(); playback.pip(true)
        if (!enterPictureInPictureMode(pipParams())) playback.pip(false)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint(); if (playback.state.value.current != null) enterPip()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig); playback.pip(isInPictureInPictureMode)
    }

    override fun onStart() {
        super.onStart(); playback.foreground()
    }

    override fun onStop() {
        playback.background(); super.onStop()
    }

    override fun onDestroy() {
        unregisterReceiver(pipReceiver); super.onDestroy(); if (isFinishing) playback.close()
    }

    private fun setFullscreen(value: Boolean) {
        playback.fullscreen(value)
        requestedOrientation =
            if (value) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (value) hide(WindowInsetsCompat.Type.systemBars()) else show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
