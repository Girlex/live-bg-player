package com.lingma.livebgplayer.ui.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.lingma.livebgplayer.domain.model.ClockPosition
import com.lingma.livebgplayer.domain.model.OverlayConfig
import com.lingma.livebgplayer.domain.model.TimerMode
import com.lingma.livebgplayer.domain.model.TimerPosition
import java.text.SimpleDateFormat
import java.util.*

/**
 * 透明覆盖层视图
 * 
 * 用于在视频上方绘制各种控件：
 * - 实时时钟
 * - 直播计时器
 * - 弹幕文字
 * - Logo 水印
 */
class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    private var config: OverlayConfig? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isAntiAlias = true
        textAlign = Paint.Align.LEFT
    }
    
    // 计时器相关
    private var timerStartTime: Long = 0L
    private var timerRunnable: Runnable? = null
    
    init {
        holder.addCallback(this)
        // 设置为媒体叠加层，确保在视频上方
        setZOrderOnTop(true)
        holder.setFormat(android.graphics.PixelFormat.TRANSPARENT)
    }

    /**
     * 更新配置
     */
    fun updateConfig(newConfig: OverlayConfig) {
        android.util.Log.d("OverlayView", "更新配置: clock=${newConfig.showClock}, timer=${newConfig.showTimer}, danmaku=${newConfig.showDanmaku}")
        config = newConfig
        
        // 启动或停止计时器
        if (newConfig.showTimer) {
            startTimer()
        } else {
            stopTimer()
        }
        
        // 触发重绘
        postInvalidate()
    }

    /**
     * 隐藏所有控件
     */
    fun hideAll() {
        visibility = GONE
        stopTimer()
    }

    /**
     * 显示所有控件
     */
    fun showAll() {
        visibility = VISIBLE
        if (config?.showTimer == true) {
            startTimer()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // Surface 创建后开始绘制
        if (visibility == VISIBLE) {
            startDrawing()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Surface 尺寸变化时重新绘制
        startDrawing()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopTimer()
    }

    /**
     * 开始绘制循环
     */
    private fun startDrawing() {
        // 使用 postInvalidate 触发重绘
        // 实际绘制在 onDraw 中进行
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (config == null) {
            android.util.Log.w("OverlayView", "配置为空，跳过绘制")
            return
        }
        
        android.util.Log.d("OverlayView", "开始绘制 overlay")
        
        config?.let { cfg ->
            val width = width.toFloat()
            val height = height.toFloat()
            
            android.util.Log.d("OverlayView", "Canvas 尺寸: ${width}x${height}")
            
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
            
            // 注意：Logo 需要使用 Bitmap，这里简化处理
            // 实际实现需要加载图片资源
        }
        
        // 持续刷新（每秒约 10 帧）
        postInvalidateDelayed(100)
    }

    /**
     * 绘制时钟
     */
    private fun drawClock(canvas: Canvas, width: Float, height: Float, config: OverlayConfig) {
        paint.apply {
            color = android.graphics.Color.WHITE
            textSize = config.clockFontSize * resources.displayMetrics.scaledDensity
            typeface = Typeface.DEFAULT_BOLD
            alpha = (255 * 0.8f).toInt() // 80% 不透明度
        }
        
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val currentTime = timeFormat.format(Date())
        
        val textWidth = paint.measureText(currentTime)
        val padding = 20f
        
        val (x, y) = when (config.clockPosition) {
            ClockPosition.TOP_LEFT -> Pair(padding, paint.textSize + padding)
            ClockPosition.TOP_RIGHT -> Pair(width - textWidth - padding, paint.textSize + padding)
            ClockPosition.BOTTOM_LEFT -> Pair(padding, height - padding)
            ClockPosition.BOTTOM_RIGHT -> Pair(width - textWidth - padding, height - padding)
        }
        
        canvas.drawText(currentTime, x, y, paint)
    }

    /**
     * 绘制计时器
     */
    private fun drawTimer(canvas: Canvas, width: Float, height: Float, config: OverlayConfig) {
        paint.apply {
            color = android.graphics.Color.WHITE
            textSize = config.timerFontSize * resources.displayMetrics.scaledDensity
            typeface = Typeface.MONOSPACE
            alpha = (255 * 0.8f).toInt()
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
        val padding = 20f
        
        val (x, y) = when (config.timerPosition) {
            TimerPosition.TOP_LEFT -> Pair(padding, paint.textSize + padding)
            TimerPosition.TOP_RIGHT -> Pair(width - textWidth - padding, paint.textSize + padding)
            TimerPosition.BOTTOM_LEFT -> Pair(padding, height - padding)
            TimerPosition.BOTTOM_RIGHT -> Pair(width - textWidth - padding, height - padding)
        }
        
        canvas.drawText(timeText, x, y, paint)
    }

    /**
     * 绘制弹幕（简化版 - 静态文本）
     */
    private fun drawDanmaku(canvas: Canvas, width: Float, height: Float, config: OverlayConfig) {
        paint.apply {
            color = android.graphics.Color.WHITE
            textSize = 16f * resources.displayMetrics.scaledDensity
            typeface = Typeface.DEFAULT
            alpha = (255 * 0.6f).toInt() // 60% 不透明度
        }
        
        val textWidth = paint.measureText(config.danmakuText)
        val y = height * 0.1f // 顶部 10% 位置
        
        // 简单的滚动效果（基于时间）
        val speed = config.danmakuSpeed * 2f
        val offsetX = (System.currentTimeMillis() / 1000f * speed) % (width + textWidth)
        val x = width - offsetX
        
        canvas.drawText(config.danmakuText, x, y, paint)
    }

    /**
     * 启动计时器
     */
    private fun startTimer() {
        if (timerRunnable == null) {
            timerStartTime = System.currentTimeMillis()
            timerRunnable = Runnable {
                postInvalidate()
                if (visibility == VISIBLE) {
                    handler.postDelayed(timerRunnable!!, 1000)
                }
            }
            handler.post(timerRunnable!!)
        }
    }

    /**
     * 停止计时器
     */
    private fun stopTimer() {
        timerRunnable?.let {
            handler.removeCallbacks(it)
            timerRunnable = null
        }
        timerStartTime = 0L
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopTimer()
    }
}
