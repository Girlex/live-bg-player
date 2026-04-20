package com.lingma.livebgplayer.domain.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 画面控件配置
 */
@Parcelize
data class OverlayConfig(
    // 实时时钟
    val showClock: Boolean = false,
    val clockPosition: ClockPosition = ClockPosition.BOTTOM_RIGHT,
    val clockFontSize: Int = 16,
    val clockAutoSwitchPosition: Boolean = false, // 是否自动切换位置
    val clockSwitchIntervalSeconds: Int = 30, // 切换间隔（秒）
    
    // 直播计时器
    val showTimer: Boolean = false,
    val timerInitialSeconds: Int = 0,
    val timerMode: TimerMode = TimerMode.COUNT_UP,
    val timerPosition: TimerPosition = TimerPosition.BOTTOM_LEFT,
    val timerFontSize: Int = 16,
    val timerAutoSwitchPosition: Boolean = false, // 是否自动切换位置
    val timerSwitchIntervalSeconds: Int = 30, // 切换间隔（秒）
    
    // 弹幕模拟
    val showDanmaku: Boolean = false,
    val danmakuText: String = "欢迎来到直播间",
    val danmakuSpeed: Int = 5, // 1-10，数字越大越快
    
    // Logo 水印
    val showLogo: Boolean = false,
    val logoUri: String? = null,
    val logoAlpha: Float = 0.5f, // 透明度 0.0-1.0
    val logoSize: Int = 100 // 像素
) : Parcelable

/**
 * 时钟位置
 */
enum class ClockPosition {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

/**
 * 计时器模式
 */
enum class TimerMode {
    COUNT_UP,      // 正计时
    COUNT_DOWN     // 倒计时
}

/**
 * 计时器位置
 */
enum class TimerPosition {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

/**
 * 视频播放列表项
 */
@Parcelize
data class VideoItem(
    val uri: String,
    val name: String,
    val duration: Long = 0L // 毫秒
) : Parcelable
