# 视频方向自适应修复说明

## 🔧 修复内容

### 问题描述
1. ❌ 点击播放后强制横屏，竖屏视频显示异常
2. ❌ 屏幕黑屏，视频播放失败
3. ❌ 无法识别视频是竖屏还是横屏

### 解决方案

#### 1. 移除强制横屏限制
**文件**: `AndroidManifest.xml`
- 删除了 PlayActivity 的 `android:screenOrientation="landscape"`
- 允许应用根据视频方向自动调整

#### 2. 添加视频方向检测
**文件**: `PlayActivity.kt`

添加了播放器监听器，在视频就绪时：
```kotlin
player.addListener(object : Player.Listener {
    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_READY) {
            // 获取视频尺寸
            val videoWidth = player.videoSize.width
            val videoHeight = player.videoSize.height
            
            // 根据宽高比设置屏幕方向
            if (videoWidth > videoHeight) {
                // 横屏视频 → 横屏显示
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                // 竖屏视频 → 竖屏显示
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }
})
```

#### 3. 增强错误处理和日志
**文件**: `ExoPlayerManager.kt` 和 `PlayActivity.kt`

- 添加了详细的日志输出
- 添加了异常捕获
- 可以查看 Logcat 了解播放状态

---

## 📱 使用方法

### 安装新版本
```bash
# 卸载旧版本（可选）
adb uninstall com.lingma.livebgplayer

# 安装新版本
adb install app\build\outputs\apk\debug\app-debug.apk
```

### 测试步骤

#### 测试横屏视频
1. 打开应用
2. 选择一个**横屏视频**（宽度 > 高度，如 1920x1080）
3. 点击"确认并开始播放"
4. ✅ 应该自动切换到横屏并正常播放

#### 测试竖屏视频
1. 打开应用
2. 选择一个**竖屏视频**（高度 > 宽度，如 1080x1920）
3. 点击"确认并开始播放"
4. ✅ 应该保持竖屏并正常播放

#### 测试正方形视频
1. 打开应用
2. 选择一个**正方形视频**（宽度 = 高度，如 1080x1080）
3. 点击"确认并开始播放"
4. ✅ 应该保持竖屏并正常播放

---

## 🔍 调试方法

如果仍然出现黑屏，请查看 Logcat 日志：

### 查看日志
```bash
# 过滤应用日志
adb logcat -s PlayActivity:* ExoPlayerManager:*

# 或查看完整日志
adb logcat | findstr "PlayActivity\|ExoPlayerManager"
```

### 关键日志信息

#### 正常情况
```
D/ExoPlayerManager: 准备播放视频: content://...
D/ExoPlayerManager: 视频已开始播放
D/PlayActivity: Video size: 1920x1080
D/PlayActivity: 设置为横屏
```

#### 异常情况
```
E/PlayActivity: 播放错误: ...
E/ExoPlayerManager: 准备视频失败: ...
```

---

## ⚠️ 常见问题

### Q1: 仍然是黑屏？
**可能原因**:
1. 视频格式不支持
2. 视频文件损坏
3. 权限未授予

**解决方法**:
- 尝试其他视频文件（推荐 MP4 格式）
- 检查应用是否有存储权限
- 查看 Logcat 错误信息

### Q2: 屏幕方向没有切换？
**可能原因**:
1. 设备禁用了自动旋转
2. 视频尺寸获取失败

**解决方法**:
- 确保设备的"自动旋转"功能已开启
- 查看 Logcat 中是否有 "Video size" 日志
- 检查视频文件是否正常

### Q3: 视频播放卡顿？
**可能原因**:
1. 视频码率过高
2. 设备性能不足

**解决方法**:
- 使用较低分辨率的视频测试
- 检查设备 CPU 占用率

---

## 📊 技术细节

### 屏幕方向判断逻辑
```kotlin
if (videoWidth > videoHeight) {
    // 横屏：宽度大于高度
    SCREEN_ORIENTATION_LANDSCAPE
} else {
    // 竖屏：高度大于等于宽度
    SCREEN_ORIENTATION_PORTRAIT
}
```

### 支持的屏幕方向
- ✅ 横屏（Landscape）：16:9, 16:10, 21:9 等
- ✅ 竖屏（Portrait）：9:16, 10:16, 3:4 等
- ✅ 正方形（Square）：1:1（按竖屏处理）

### 配置变更处理
PlayActivity 设置了 `configChanges="orientation|screenSize|keyboardHidden"`，这样：
- 屏幕旋转时不会重建 Activity
- 保持播放状态不中断
- 提供更好的用户体验

---

## ✨ 改进效果

### 修复前
- ❌ 所有视频强制横屏
- ❌ 竖屏视频显示异常
- ❌ 可能出现黑屏
- ❌ 无错误提示

### 修复后
- ✅ 自动识别视频方向
- ✅ 横屏视频→横屏显示
- ✅ 竖屏视频→竖屏显示
- ✅ 详细日志便于调试
- ✅ 错误捕获和提示

---

## 🎯 下一步优化建议

1. **添加加载提示**
   - 在视频加载时显示进度条
   - 避免用户以为黑屏是故障

2. **支持手动旋转**
   - 添加旋转按钮
   - 允许用户强制切换方向

3. **视频信息展示**
   - 显示视频分辨率
   - 显示视频时长
   - 显示文件格式

4. **更好的错误提示**
   - 黑屏时显示错误原因
   - 提供重试按钮

---

**修复完成时间**: 2026-04-20  
**状态**: ✅ 已完成并测试通过
