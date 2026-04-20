package com.lingma.livebgplayer.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaSource
import com.lingma.livebgplayer.domain.model.VideoItem

/**
 * 视频播放列表管理器
 * 
 * 功能：
 * 1. 管理多个视频的播放列表
 * 2. 支持无缝切换视频
 * 3. 预加载下一个视频
 */
class VideoPlaylistManager(
    private val context: Context,
    private val onPlaylistEmpty: () -> Unit = {}
) {
    private val playlist = mutableListOf<VideoItem>()
    private var currentIndex = 0
    
    /**
     * 添加视频到播放列表
     */
    fun addVideo(uri: Uri, name: String, duration: Long = 0L) {
        val videoItem = VideoItem(uri.toString(), name, duration)
        playlist.add(videoItem)
    }
    
    /**
     * 添加视频列表
     */
    fun addVideos(videos: List<VideoItem>) {
        playlist.addAll(videos)
    }
    
    /**
     * 删除视频
     */
    fun removeVideo(index: Int) {
        if (index in playlist.indices) {
            playlist.removeAt(index)
            // 调整当前索引
            if (currentIndex >= playlist.size && playlist.isNotEmpty()) {
                currentIndex = playlist.size - 1
            }
        }
    }
    
    /**
     * 清空播放列表
     */
    fun clearPlaylist() {
        playlist.clear()
        currentIndex = 0
    }
    
    /**
     * 获取当前视频
     */
    fun getCurrentVideo(): VideoItem? {
        return if (playlist.isNotEmpty() && currentIndex in playlist.indices) {
            playlist[currentIndex]
        } else {
            null
        }
    }
    
    /**
     * 切换到下一个视频
     * @return 下一个视频的 MediaSource，如果已到达末尾则返回 null
     */
    fun getNextMediaSource(): MediaSource? {
        if (playlist.isEmpty()) {
            onPlaylistEmpty()
            return null
        }
        
        // 循环到下一个
        currentIndex = (currentIndex + 1) % playlist.size
        
        val nextVideo = playlist[currentIndex]
        return createMediaSource(nextVideo.uri)
    }
    
    /**
     * 获取第一个视频的 MediaSource
     */
    fun getFirstMediaSource(): MediaSource? {
        if (playlist.isEmpty()) {
            onPlaylistEmpty()
            return null
        }
        
        currentIndex = 0
        val firstVideo = playlist[0]
        return createMediaSource(firstVideo.uri)
    }
    
    /**
     * 预加载下一个视频（返回 MediaSource 但不播放）
     */
    fun preloadNextMediaSource(): MediaSource? {
        if (playlist.isEmpty()) return null
        
        val nextIndex = (currentIndex + 1) % playlist.size
        val nextVideo = playlist[nextIndex]
        return createMediaSource(nextVideo.uri)
    }
    
    /**
     * 获取播放列表大小
     */
    fun getPlaylistSize(): Int = playlist.size
    
    /**
     * 获取当前索引
     */
    fun getCurrentIndex(): Int = currentIndex
    
    /**
     * 获取完整播放列表
     */
    fun getPlaylist(): List<VideoItem> = playlist.toList()
    
    /**
     * 检查是否有下一个视频
     */
    fun hasNextVideo(): Boolean = playlist.size > 1
    
    /**
     * 创建 MediaSource
     */
    private fun createMediaSource(uriString: String): MediaSource {
        val uri = Uri.parse(uriString)
        val mediaItem = MediaItem.fromUri(uri)
        
        // 使用 ExoPlayerManager 的媒体源工厂
        // 这里简化处理，实际应该注入 MediaSource.Factory
        return androidx.media3.exoplayer.source.ProgressiveMediaSource
            .Factory(
                androidx.media3.datasource.DefaultDataSource.Factory(context)
            )
            .createMediaSource(mediaItem)
    }
}
