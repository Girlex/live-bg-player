package com.lingma.livebgplayer.ui.play

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.lingma.livebgplayer.databinding.ActivityPlayBinding
import com.lingma.livebgplayer.domain.model.OverlayConfig
import com.lingma.livebgplayer.player.ExoPlayerManager
import com.lingma.livebgplayer.player.VideoPlaylistManager
import com.lingma.livebgplayer.ui.overlay.OverlayView
import com.lingma.livebgplayer.utils.VGestureDetector
import kotlinx.coroutines.launch

class PlayActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_VIDEO_URI = "extra_video_uri"
        const val EXTRA_VIDEO_LIST = "extra_video_list"
        const val EXTRA_OVERLAY_CONFIG = "extra_overlay_config"
        const val EXTRA_LOOP_MODE = "extra_loop_mode"
        const val EXTRA_VOLUME = "extra_volume"
        const val EXTRA_KEEP_SCREEN_ON = "extra_keep_screen_on"
        private const val TAG = "PlayActivity"
    }

    private lateinit var binding: ActivityPlayBinding
    private lateinit var playerManager: ExoPlayerManager
    private lateinit var playlistManager: VideoPlaylistManager
    private lateinit var vGestureDetector: VGestureDetector
    private var overlayView: OverlayView? = null
    private var videoUri: Uri = Uri.EMPTY
    private var volume: Float = 0f
    private var keepScreenOn: Boolean = true
    private var isVideoReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. 解析 Intent 参数
        parseIntent()
        
        // 2. 设置全屏标志
        setupFullScreen()
        
        // 3. 使用 ViewBinding 加载布局
        binding = ActivityPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        Log.d(TAG, "开始初始化播放器")
        
        // 4. 初始化播放器
        playerManager = ExoPlayerManager(applicationContext)
        val player = playerManager.initialize()
        playerManager.setVolume(volume)
        
        // 5. 绑定 PlayerView
        binding.playerView.player = player
        
        // 6. 添加播放器监听器
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_IDLE -> {
                        Log.d(TAG, "播放器状态: IDLE")
                    }
                    Player.STATE_BUFFERING -> {
                        Log.d(TAG, "播放器状态: BUFFERING")
                    }
                    Player.STATE_READY -> {
                        if (!isVideoReady) {
                            isVideoReady = true
                            Log.d(TAG, "播放器状态: READY - 视频已就绪")
                            
                            // 获取视频尺寸并调整屏幕方向
                            val videoWidth = player.videoSize.width
                            val videoHeight = player.videoSize.height
                            Log.d(TAG, "视频尺寸: ${videoWidth}x${videoHeight}")
                            
                            // 根据视频宽高比设置屏幕方向
                            if (videoWidth > 0 && videoHeight > 0) {
                                if (videoWidth > videoHeight) {
                                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    Log.d(TAG, "设置为横屏模式")
                                } else {
                                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    Log.d(TAG, "设置为竖屏模式")
                                }
                            } else {
                                Log.w(TAG, "警告: 视频尺寸为 0x0")
                            }
                        }
                    }
                    Player.STATE_ENDED -> {
                        Log.d(TAG, "播放器状态: ENDED - 循环重新开始")
                    }
                }
            }
            
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e(TAG, "========== 播放错误 ==========")
                Log.e(TAG, "错误代码: ${error.errorCode}")
                Log.e(TAG, "错误消息: ${error.message}")
                Log.e(TAG, "错误原因: ${error.cause}")
                Log.e(TAG, "============================")
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.d(TAG, "播放状态改变: isPlaying=$isPlaying")
            }
            
            override fun onRenderedFirstFrame() {
                Log.d(TAG, "========== 第一帧已渲染 ==========")
            }
        })
        
        // 7. 开始播放
        lifecycleScope.launch {
            playerManager.prepareAndPlay(videoUri)
        }
    }

    private fun parseIntent() {
        val uriString = intent.getStringExtra(EXTRA_VIDEO_URI)
        if (!uriString.isNullOrEmpty()) {
            videoUri = Uri.parse(uriString)
        }
        volume = intent.getFloatExtra(EXTRA_VOLUME, 0f)
        keepScreenOn = intent.getBooleanExtra(EXTRA_KEEP_SCREEN_ON, true)
    }

    private fun setupFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.apply {
                hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
        
        if (keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (event.isLongPress) {
                    finish()
                    true
                } else {
                    true
                }
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onStart() {
        super.onStart()
        playerManager.play()
    }

    override fun onStop() {
        super.onStop()
        playerManager.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.playerView.player = null
        playerManager.release()
    }
}
