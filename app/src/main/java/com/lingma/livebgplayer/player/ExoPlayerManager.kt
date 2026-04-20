package com.lingma.livebgplayer.player

import android.content.Context
import android.net.Uri
import android.os.Looper
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

class ExoPlayerManager(private val context: Context) {

    companion object {
        private const val TAG = "ExoPlayerManager"
        // 缓冲区配置 - 针对低内存设备优化
        private const val MIN_BUFFER_MS = 15000      // 最小缓冲 15 秒（降低）
        private const val MAX_BUFFER_MS = 50000     // 最大缓冲 50 秒（降低）
        private const val BUFFER_FOR_PLAYBACK_MS = 5000  // 起播缓冲 5 秒（降低）
        private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 5000
    }

    private var exoPlayer: ExoPlayer? = null
    private var mediaSourceFactory: DefaultMediaSourceFactory

    init {
        // 创建数据源工厂（带缓存）
        val cacheDataSourceFactory = VideoCacheManager.getCacheDataSourceFactory(context)
        mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)
    }

    // 初始化播放器
    fun initialize(): ExoPlayer {
        return ExoPlayer.Builder(context)
            .setLooper(Looper.getMainLooper())
            .setLoadControl(createLoadControl())
            .setRenderersFactory(createRenderersFactory())
            .setTrackSelector(DefaultTrackSelector(context))
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                // 核心配置
                repeatMode = Player.REPEAT_MODE_ONE        // 单视频无限循环
                playWhenReady = true                       // 准备就绪后自动播放
                volume = 1.0f
                // 添加监听器
                addListener(createPlayerListener())
            }.also {
                exoPlayer = it
            }
    }

    // 自定义缓冲策略 - 核心防卡顿配置
    private fun createLoadControl(): LoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                MIN_BUFFER_MS,
                MAX_BUFFER_MS,
                BUFFER_FOR_PLAYBACK_MS,
                BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .setPrioritizeTimeOverSizeThresholds(true)  // 优先保证时间阈值
            .setBackBuffer(10000, false)  // 保留 10 秒回退缓冲
            .build()
    }

    // 渲染器工厂 - 启用硬件加速
    private fun createRenderersFactory(): DefaultRenderersFactory {
        return DefaultRenderersFactory(context).apply {
            setEnableDecoderFallback(true)           // 解码器降级
            setEnableAudioTrackPlaybackParams(true)  // 音频轨道参数优化
            // 强制使用软件解码（模拟器兼容性更好）
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
        }
    }

    // 播放器监听器
    private fun createPlayerListener(): Player.Listener {
        return object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_IDLE -> Log.d(TAG, "Player idle")
                    Player.STATE_BUFFERING -> Log.d(TAG, "Player buffering")
                    Player.STATE_READY -> Log.d(TAG, "Player ready")
                    Player.STATE_ENDED -> {
                        // 循环模式不会触发此状态，但为防万一，手动处理
                        Log.d(TAG, "Player ended, restarting")
                        exoPlayer?.seekToDefaultPosition()
                        exoPlayer?.playWhenReady = true
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Playback error: ${error.message}")
                handlePlaybackError(error)
            }
        }
    }

    // 错误处理
    private fun handlePlaybackError(error: PlaybackException) {
        // 简单重试策略
        if (error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
            error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED) {
            // 延迟重试
            exoPlayer?.prepare()
            exoPlayer?.play()
        }
    }

    // 准备并播放视频
    suspend fun prepareAndPlay(videoUri: Uri) {
        Log.d(TAG, "准备播放视频: $videoUri")
        try {
            val mediaSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(videoUri))
            exoPlayer?.setMediaSource(mediaSource)
            exoPlayer?.prepare()
            exoPlayer?.playWhenReady = true
            Log.d(TAG, "视频已开始播放")
        } catch (e: Exception) {
            Log.e(TAG, "准备视频失败: ${e.message}", e)
        }
    }

    fun play() {
        exoPlayer?.playWhenReady = true
    }

    fun pause() {
        exoPlayer?.playWhenReady = false
    }

    fun setVolume(volume: Float) {
        exoPlayer?.volume = volume.coerceIn(0f, 1f)
    }

    fun release() {
        exoPlayer?.apply {
            stop()
            clearVideoSurface()
            release()
        }
        exoPlayer = null
    }
}
