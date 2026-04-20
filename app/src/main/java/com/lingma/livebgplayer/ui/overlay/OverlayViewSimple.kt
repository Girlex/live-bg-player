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
     * 绘制时钟（带装饰）
     */
    private fun drawClock(canvas: Canvas, width: Float, height: Float, config: OverlayConfig) {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val currentTime = timeFormat.format(Date())
        
        // 设置文字样式
        paint.apply {
            color = Color.WHITE
            textSize = config.clockFontSize * resources.displayMetrics.scaledDensity
            typeface = Typeface.DEFAULT_BOLD
            alpha = 255
        }
        
        val textWidth = paint.measureText(currentTime)
        val textHeight = paint.textSize
        val padding = 40f
        val boxPadding = 16f // 背景框内边距
        
        // 计算位置
        val (baseX, baseY) = when (config.clockPosition) {
            ClockPosition.TOP_LEFT -> Pair(padding, padding + textHeight)
            ClockPosition.TOP_RIGHT -> Pair(width - textWidth - padding, padding + textHeight)
            ClockPosition.BOTTOM_LEFT -> Pair(padding, height - padding)
            ClockPosition.BOTTOM_RIGHT -> Pair(width - textWidth - padding, height - padding)
        }
        
        // 绘制背景框（圆角矩形）
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CC000000") // 半透明黑色背景
            style = Paint.Style.FILL
        }
        
        val left = baseX - boxPadding
        val top = baseY - textHeight - boxPadding / 2
        val right = baseX + textWidth + boxPadding
        val bottom = baseY + boxPadding / 2
        
        canvas.drawRoundRect(left, top, right, bottom, 12f, 12f, backgroundPaint)
        
        // 绘制边框
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4DFFFFFF") // 白色半透明边框
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(left, top, right, bottom, 12f, 12f, borderPaint)
        
        // 绘制装饰图标（小圆点）
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF6B6B") // 红色装饰点
            style = Paint.Style.FILL
        }
        
        // 左上角装饰点
        canvas.drawCircle(left + 8f, top + 8f, 3f, dotPaint)
        // 右上角装饰点
        canvas.drawCircle(right - 8f, top + 8f, 3f, dotPaint)
        
        // 绘制时间文字（带阴影效果）
        val shadowPaint = Paint(paint).apply {
            color = Color.parseColor("#40000000") // 黑色阴影
            alpha = 100
        }
        canvas.drawText(currentTime, baseX + 2f, baseY + 2f, shadowPaint)
        canvas.drawText(currentTime, baseX, baseY, paint)
        
        android.util.Log.d("OverlayView", "时钟绘制在 ($baseX, $baseY): $currentTime")
    }

    /**
     * 绘制计时器（带装饰）
     */
    private fun drawTimer(canvas: Canvas, width: Float, height: Float, config: OverlayConfig) {
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
        
        // 设置文字样式
        paint.apply {
            color = Color.WHITE
            textSize = config.timerFontSize * resources.displayMetrics.scaledDensity
            typeface = Typeface.MONOSPACE
            alpha = 255
        }
        
        val textWidth = paint.measureText(timeText)
        val textHeight = paint.textSize
        val padding = 40f
        val boxPadding = 16f
        
        // 计算位置
        val (baseX, baseY) = when (config.timerPosition) {
            TimerPosition.TOP_LEFT -> Pair(padding, padding + textHeight)
            TimerPosition.TOP_RIGHT -> Pair(width - textWidth - padding, padding + textHeight)
            TimerPosition.BOTTOM_LEFT -> Pair(padding, height - padding)
            TimerPosition.BOTTOM_RIGHT -> Pair(width - textWidth - padding, height - padding)
        }
        
        // 绘制渐变背景框
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CC1a1a2e") // 深蓝色半透明背景
            style = Paint.Style.FILL
        }
        
        val left = baseX - boxPadding
        val top = baseY - textHeight - boxPadding / 2
        val right = baseX + textWidth + boxPadding
        val bottom = baseY + boxPadding / 2
        
        canvas.drawRoundRect(left, top, right, bottom, 12f, 12f, backgroundPaint)
        
        // 绘制边框（蓝色）
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4D4A90E0") // 蓝色半透明边框
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(left, top, right, bottom, 12f, 12f, borderPaint)
        
        // 绘制装饰图标（计时器图标 - 小圆圈）
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4A90E0") // 蓝色
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawCircle(left + 8f, top + 8f, 4f, iconPaint)
        
        // 绘制时间文字（带阴影）
        val shadowPaint = Paint(paint).apply {
            color = Color.parseColor("#40000000")
            alpha = 100
        }
        canvas.drawText(timeText, baseX + 2f, baseY + 2f, shadowPaint)
        canvas.drawText(timeText, baseX, baseY, paint)
        
        android.util.Log.d("OverlayView", "计时器绘制在 ($baseX, $baseY): $timeText")
    }

    /**
     * 绘制弹幕（带装饰）
     */
    private fun drawDanmaku(canvas: Canvas, width: Float, height: Float, config: OverlayConfig) {
        val textWidth = paint.measureText(config.danmakuText)
        
        // 设置文字样式
        paint.apply {
            color = Color.WHITE
            textSize = 18f * resources.displayMetrics.scaledDensity
            typeface = Typeface.DEFAULT_BOLD
            alpha = 255
        }
        
        val textHeight = paint.textSize
        val y = height * 0.15f // 顶部 15% 位置
        val boxPadding = 12f
        
        // 简单的滚动效果（基于时间）
        val speed = config.danmakuSpeed * 3f
        val offsetX = (System.currentTimeMillis() / 1000f * speed) % (width + textWidth + boxPadding * 2)
        val baseX = width - offsetX
        
        // 绘制背景框（渐变效果）
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#99000000") // 半透明黑色
            style = Paint.Style.FILL
        }
        
        val left = baseX - boxPadding
        val top = y - textHeight - boxPadding / 2
        val right = baseX + textWidth + boxPadding
        val bottom = y + boxPadding / 2
        
        canvas.drawRoundRect(left, top, right, bottom, 20f, 20f, backgroundPaint)
        
        // 绘制边框（彩色）
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#66FFD700") // 金色半透明边框
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(left, top, right, bottom, 20f, 20f, borderPaint)
        
        // 绘制文字（带阴影）
        val shadowPaint = Paint(paint).apply {
            color = Color.parseColor("#40000000")
            alpha = 100
        }
        canvas.drawText(config.danmakuText, baseX + 1f, y + 1f, shadowPaint)
        canvas.drawText(config.danmakuText, baseX, y, paint)
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
