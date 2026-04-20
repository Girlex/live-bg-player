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
import com.lingma.livebgplayer.ui.overlay.OverlayViewSimple
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
    private var overlayView: OverlayViewSimple? = null
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
        
        // 7. 初始化 V 字手势检测器
        vGestureDetector = VGestureDetector {
            Log.d(TAG, "检测到 V 字手势，切换视频")
            switchToNextVideo()
        }
        
        // 8. 初始化播放列表管理器
        playlistManager = VideoPlaylistManager(applicationContext) {
            Log.w(TAG, "播放列表为空")
        }
        
        // 9. 添加视频到播放列表
        setupVideoPlaylist()
        
        // 10. 初始化并添加 OverlayView
        setupOverlayView()
        
        // 11. 开始播放
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
        // 优先处理 V 字手势
        val gestureHandled = vGestureDetector.onTouchEvent(event)
        return gestureHandled || super.onTouchEvent(event)
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

    /**
     * 设置视频播放列表
     */
    private fun setupVideoPlaylist() {
        // 从 Intent 获取视频列表
        val videoList = intent.getParcelableArrayListExtra<com.lingma.livebgplayer.domain.model.VideoItem>(EXTRA_VIDEO_LIST)
        
        if (videoList != null && videoList.isNotEmpty()) {
            // 使用播放列表
            playlistManager.addVideos(videoList)
            Log.d(TAG, "已加载 ${videoList.size} 个视频到播放列表")
        } else {
            // 如果没有播放列表，只添加当前视频
            val name = videoUri.lastPathSegment ?: "Unknown"
            playlistManager.addVideo(videoUri, name)
            Log.d(TAG, "已添加单个视频到播放列表")
        }
    }

    /**
     * 设置 OverlayView
     */
    private fun setupOverlayView() {
        val overlayConfig = intent.getParcelableExtra<OverlayConfig>(EXTRA_OVERLAY_CONFIG)
        
        if (overlayConfig != null) {
            Log.d(TAG, "收到 OverlayConfig: clock=${overlayConfig.showClock}, timer=${overlayConfig.showTimer}, danmaku=${overlayConfig.showDanmaku}")
            
            // 创建并配置 OverlayViewSimple（使用普通 View，不是 SurfaceView）
            overlayView = OverlayViewSimple(this).apply {
                updateConfig(overlayConfig)
                // 直接添加到根布局（FrameLayout），确保在 PlayerView 上方
                binding.root.addView(
                    this,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                Log.d(TAG, "OverlayViewSimple 已添加到根布局")
            }
        } else {
            Log.d(TAG, "未配置 Overlay，跳过初始化")
        }
    }

    /**
     * 切换到下一个视频
     */
    private fun switchToNextVideo() {
        if (!playlistManager.hasNextVideo()) {
            Log.d(TAG, "只有一个视频，无法切换")
            return
        }
        
        val nextMediaSource = playlistManager.getNextMediaSource()
        if (nextMediaSource != null) {
            Log.d(TAG, "切换到下一个视频: ${playlistManager.getCurrentIndex() + 1}")
            playerManager.switchToMediaSource(nextMediaSource)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理 OverlayView
        overlayView?.let {
            (it.parent as? android.view.ViewGroup)?.removeView(it)
            it.hideAll()
        }
        overlayView = null
        
        binding.playerView.player = null
        playerManager.release()
    }
}
