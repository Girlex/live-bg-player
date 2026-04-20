package com.lingma.livebgplayer.utils

import android.view.MotionEvent
import kotlin.math.abs

/**
 * V 字手势识别器
 * 
 * 检测规则：
 * 1. 单指触摸
 * 2. 先向侧下方移动（Y 增加）
 * 3. 再向侧上方移动（Y 减少）
 * 4. X 轴方向相反（形成 V 形）
 * 5. 两段位移均超过阈值
 * 6. 整个手势在时间限制内完成
 */
class VGestureDetector(
    private val onVGestureDetected: () -> Unit
) {
    companion object {
        private const val MIN_SEGMENT_LENGTH = 200f  // 每段最小位移（像素）
        private const val MAX_GESTURE_TIME = 1000L   // 最大手势时间（毫秒）
        private const val MIN_Y_CHANGE = 100f        // 最小 Y 轴变化
    }

    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L
    private var midX = 0f
    private var midY = 0f
    private var isTracking = false
    private var hasMovedDown = false

    /**
     * 处理触摸事件
     * @return true 如果事件已被消费
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // 只处理单指触摸
                if (event.pointerCount == 1) {
                    downX = event.x
                    downY = event.y
                    downTime = event.eventTime
                    isTracking = true
                    hasMovedDown = false
                    return true
                }
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (!isTracking || event.pointerCount > 1) {
                    reset()
                    return false
                }

                val currentX = event.x
                val currentY = event.y
                
                // 检测是否向下移动
                if (!hasMovedDown) {
                    val deltaY = currentY - downY
                    if (deltaY > MIN_Y_CHANGE) {
                        hasMovedDown = true
                        midX = currentX
                        midY = currentY
                    }
                } else {
                    // 已经向下移动过，现在检测是否向上移动形成 V 字
                    val timeElapsed = event.eventTime - downTime
                    
                    // 检查时间限制
                    if (timeElapsed > MAX_GESTURE_TIME) {
                        reset()
                        return false
                    }

                    val deltaY = midY - currentY
                    val deltaX1 = midX - downX  // 第一段 X 位移
                    val deltaX2 = currentX - midX  // 第二段 X 位移
                    
                    // 检测是否向上移动足够距离
                    if (deltaY > MIN_Y_CHANGE) {
                        // 计算两段位移长度
                        val segment1Length = kotlin.math.sqrt(
                            (midX - downX) * (midX - downX) + 
                            (midY - downY) * (midY - downY)
                        )
                        val segment2Length = kotlin.math.sqrt(
                            (currentX - midX) * (currentX - midX) + 
                            (currentY - midY) * (currentY - midY)
                        )
                        
                        // 检查两段位移是否都超过阈值
                        if (segment1Length >= MIN_SEGMENT_LENGTH && 
                            segment2Length >= MIN_SEGMENT_LENGTH) {
                            
                            // 检查 X 轴方向是否相反（形成 V 形）
                            val xDirectionOpposite = (deltaX1 > 0 && deltaX2 < 0) || 
                                                   (deltaX1 < 0 && deltaX2 > 0)
                            
                            if (xDirectionOpposite) {
                                // V 字手势识别成功！
                                onVGestureDetected()
                                reset()
                                return true
                            }
                        }
                    }
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                reset()
            }
        }
        
        return false
    }

    /**
     * 重置状态
     */
    private fun reset() {
        isTracking = false
        hasMovedDown = false
        downX = 0f
        downY = 0f
        midX = 0f
        midY = 0f
        downTime = 0L
    }
}
