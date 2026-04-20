package com.lingma.livebgplayer.ui.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.lingma.livebgplayer.domain.model.ClockPosition
import com.lingma.livebgplayer.domain.model.OverlayConfig
import com.lingma.livebgplayer.domain.model.TimerMode
import com.lingma.livebgplayer.domain.model.TimerPosition
import java.text.SimpleDateFormat
import java.util.*

/**
 * 透明覆盖层视图（使用普通 View）
 * 
 * 用于在视频上方绘制各种控件：
 * - 实时时钟
 * - 直播计时器
 * - 弹幕文字
 * - Logo 水印
 */
class OverlayViewSimple @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var config: OverlayConfig? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isAntiAlias = true
        textAlign = Paint.Align.LEFT
    }
    
    // 计时器相关
    private var timerStartTime: Long = 0L
    private val updateRunnable = object : Runnable {
        override fun run() {
            invalidate() // 触发重绘
            postDelayed(this, 100) // 10 FPS
        }
    }

    init {
        // 设置为透明背景
        setBackgroundColor(Color.TRANSPARENT)
        android.util.Log.d("OverlayView", "OverlayView 初始化完成")
    }

    /**
     * 更新配置
     */
    fun updateConfig(newConfig: OverlayConfig) {
        android.util.Log.d("OverlayView", "更新配置: clock=${newConfig.showClock}, timer=${newConfig.showTimer}, danmaku=${newConfig.showDanmaku}")
        config = newConfig
        
        // 启动或停止计时器
        if (newConfig.showTimer || newConfig.showClock || newConfig.showDanmaku) {
            startUpdates()
        } else {
            stopUpdates()
        }
        
        // 触发重绘
        invalidate()
    }

    /**
     * 隐藏所有控件
     */
    fun hideAll() {
        visibility = GONE
        stopUpdates()
    }

    /**
     * 显示所有控件
     */
    fun showAll() {
        visibility = VISIBLE
        if (config != null) {
            startUpdates()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (config == null) {
            android.util.Log.w("OverlayView", "配置为空，跳过绘制")
            return
        }
        
        android.util.Log.d("OverlayView", "开始绘制 overlay, 尺寸: ${width}x${height}")
        
        val width = width.toFloat()
        val height = height.toFloat()
        
        if (width <= 0 || height <= 0) {
            android.util.Log.w("OverlayView", "尺寸为 0，跳过绘制")
            return
        }
        
        config?.let { cfg ->
            // 绘制时钟
            if (cfg.showClock) {
                android.util.Log.d("OverlayView", "绘制时钟")
                drawClock(canvas, width, height, cfg)
            }
            
            // 绘制计时器
            if (cfg.showTimer) {
                android.util.Log.d("OverlayView", "绘制计时器")
                drawTimer(canvas, width, height, cfg)
            }
            
            // 绘制弹幕
            if (cfg.showDanmaku) {
                android.util.Log.d("OverlayView", "绘制弹幕")
                drawDanmaku(canvas, width, height, cfg)
            }
        }
    }

    /**
     * 绘制时钟
     */
    private fun drawClock(canvas: Canvas, width: Float, height: Float, config: OverlayConfig) {
        paint.apply {
            color = Color.WHITE
            textSize = config.clockFontSize * resources.displayMetrics.scaledDensity
            typeface = Typeface.DEFAULT_BOLD
            alpha = (255 * 0.9f).toInt() // 90% 不透明度
        }
        
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val currentTime = timeFormat.format(Date())
        
        val textWidth = paint.measureText(currentTime)
        val padding = 40f
        
        val (x, y) = when (config.clockPosition) {
            ClockPosition.TOP_LEFT -> Pair(padding, paint.textSize + padding)
            ClockPosition.TOP_RIGHT -> Pair(width - textWidth - padding, paint.textSize + padding)
            ClockPosition.BOTTOM_LEFT -> Pair(padding, height - padding)
            ClockPosition.BOTTOM_RIGHT -> Pair(width - textWidth - padding, height - padding)
        }
        
        canvas.drawText(currentTime, x, y, paint)
        android.util.Log.d("OverlayView", "时钟绘制在 ($x, $y): $currentTime")
    }

    /**
     * 绘制计时器
     */
    private fun drawTimer(canvas: Canvas, width: Float, height: Float, config: OverlayConfig) {
        paint.apply {
            color = Color.WHITE
            textSize = config.timerFontSize * resources.displayMetrics.scaledDensity
            typeface = Typeface.MONOSPACE
            alpha = (255 * 0.9f).toInt()
        }
        
        val elapsedSeconds = if (timerStartTime > 0) {
            (System.currentTimeMillis() - timerStartTime) / 1000
        } else {
            0L
        }
        
        val displaySeconds = when (config.timerMode) {
            TimerMode.COUNT_UP -> elapsedSeconds
            TimerMode.COUNT_DOWN -> {
                val remaining = config.timerInitialSeconds.toLong() - elapsedSeconds
                if (remaining < 0) 0 else remaining
            }
        }
        
        val hours = displaySeconds / 3600
        val minutes = (displaySeconds % 3600) / 60
        val seconds = displaySeconds % 60
        val timeText = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        
        val textWidth = paint.measureText(timeText)
        val padding = 40f
        
        val (x, y) = when (config.timerPosition) {
            TimerPosition.TOP_LEFT -> Pair(padding, paint.textSize + padding)
            TimerPosition.TOP_RIGHT -> Pair(width - textWidth - padding, paint.textSize + padding)
            TimerPosition.BOTTOM_LEFT -> Pair(padding, height - padding)
            TimerPosition.BOTTOM_RIGHT -> Pair(width - textWidth - padding, height - padding)
        }
        
        canvas.drawText(timeText, x, y, paint)
        android.util.Log.d("OverlayView", "计时器绘制在 ($x, $y): $timeText")
    }

    /**
     * 绘制弹幕（简化版 - 静态文本）
     */
    private fun drawDanmaku(canvas: Canvas, width: Float, height: Float, config: OverlayConfig) {
        paint.apply {
            color = Color.WHITE
            textSize = 18f * resources.displayMetrics.scaledDensity
            typeface = Typeface.DEFAULT
            alpha = (255 * 0.7f).toInt() // 70% 不透明度
        }
        
        val textWidth = paint.measureText(config.danmakuText)
        val y = height * 0.15f // 顶部 15% 位置
        
        // 简单的滚动效果（基于时间）
        val speed = config.danmakuSpeed * 3f
        val offsetX = (System.currentTimeMillis() / 1000f * speed) % (width + textWidth)
        val x = width - offsetX
        
        canvas.drawText(config.danmakuText, x, y, paint)
    }

    /**
     * 启动更新循环
     */
    private fun startUpdates() {
        if (timerStartTime == 0L) {
            timerStartTime = System.currentTimeMillis()
        }
        removeCallbacks(updateRunnable)
        post(updateRunnable)
        android.util.Log.d("OverlayView", "启动更新循环")
    }

    /**
     * 停止更新循环
     */
    private fun stopUpdates() {
        removeCallbacks(updateRunnable)
        timerStartTime = 0L
        android.util.Log.d("OverlayView", "停止更新循环")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopUpdates()
        android.util.Log.d("OverlayView", "OverlayView 已销毁")
    }
}
