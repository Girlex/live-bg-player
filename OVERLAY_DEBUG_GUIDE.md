# 🔍 OverlayView 调试指南

## 📱 测试步骤

### 1. 安装新版本 APK

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 2. 启动日志监控

在电脑上运行：

```bash
# 清除旧日志
adb logcat -c

# 开始监控（过滤相关标签）
adb logcat | grep -E "PlayActivity|OverlayView"
```

### 3. 配置并进入播放页面

1. 打开应用
2. 选择视频文件
3. **勾选** "显示实时时钟"、"显示直播计时器"、"显示弹幕文字"
4. 点击 "确认并开始播放"

---

## 📊 预期日志输出

### ✅ 正常情况应该看到：

```
D/PlayActivity: 收到 OverlayConfig: clock=true, timer=true, danmaku=true
D/OverlayView: 更新配置: clock=true, timer=true, danmaku=true
D/PlayActivity: OverlayView 已添加到根布局
D/OverlayView: 开始绘制 overlay
D/OverlayView: Canvas 尺寸: 1080.0x1920.0
D/OverlayView: 绘制时钟
D/OverlayView: 绘制计时器
D/OverlayView: 绘制弹幕
```

### ❌ 如果看到这些，说明有问题：

**问题 1: 配置未传递**
```
D/PlayActivity: 未配置 Overlay，跳过初始化
```
→ 检查 ConfigActivity 是否正确创建和传递 OverlayConfig

**问题 2: Surface 未创建**
```
// 没有任何 OverlayView 的日志
```
→ OverlayView 可能没有被添加到布局中

**问题 3: 配置为空**
```
W/OverlayView: 配置为空，跳过绘制
```
→ updateConfig 未被调用或 config 为 null

---

## 🐛 常见问题排查

### 问题 A: 勾选了但看不到控件

**可能原因 1**: OverlayView 被视频遮挡

**检查方法**：
- 查看日志是否有 "OverlayView 已添加到根布局"
- 确认 binding.root.addView 被调用

**解决方法**：
- 已在代码中修复，确保添加到 binding.root

---

**可能原因 2**: SurfaceView 透明背景不工作

**检查方法**：
- 查看是否有 "开始绘制 overlay" 日志
- 如果有绘制日志但看不到，可能是透明度问题

**解决方法**：
- SurfaceView 的 setZOrderOnTop(true) 应该确保在最上层
- holder.setFormat(PixelFormat.TRANSPARENT) 设置透明

---

**可能原因 3**: onDraw 未被调用

**检查方法**：
- 查看是否有 "开始绘制 overlay" 日志
- 如果没有，说明 Surface 未创建或不可见

**解决方法**：
- 检查 OverlayView 的 visibility
- 确认 surfaceCreated 回调被触发

---

### 问题 B: 只看到部分控件

**检查日志**：
```
D/OverlayView: 绘制时钟        ← 看到了吗？
D/OverlayView: 绘制计时器      ← 看到了吗？
D/OverlayView: 绘制弹幕        ← 看到了吗？
```

**如果某个控件没有绘制日志**：
- 检查对应的 checkbox 是否勾选
- 检查 OverlayConfig 中对应字段是否为 true

---

## 🔧 临时调试方案

如果 SurfaceView 方式仍然有问题，可以尝试改用普通 View。

### 方案 1: 使用自定义 View 代替 SurfaceView

创建一个简单的测试版本，在 PlayActivity 中直接添加一个 TextView 测试：

```kotlin
// 在 setupOverlayView 中添加测试代码
val testView = TextView(this).apply {
    text = "测试覆盖层 - 如果看到这个说明叠加层工作正常"
    textSize = 20f
    setTextColor(Color.WHITE)
    setBackgroundColor(Color.parseColor("#80000000")) // 半透明黑色
    gravity = Gravity.CENTER
}

binding.root.addView(testView, FrameLayout.LayoutParams(
    FrameLayout.LayoutParams.MATCH_PARENT,
    200 // 高度 200px
).apply {
    gravity = Gravity.TOP
})
```

如果能看到这个测试文本，说明：
- ✅ 布局添加正常工作
- ✅ 层级关系正确
- ❌ 问题出在 SurfaceView 本身

---

### 方案 2: 检查 OverlayView 的可见性

在 setupOverlayView 中添加：

```kotlin
overlayView = OverlayView(this).apply {
    updateConfig(overlayConfig)
    
    // 调试：强制设置为可见
    visibility = View.VISIBLE
    
    // 调试：设置背景色测试
    setBackgroundColor(Color.parseColor("#40FF0000")) // 半透明红色
    
    binding.root.addView(...)
}
```

如果看到红色半透明背景，说明 OverlayView 显示了，但绘制内容有问题。

---

## 📝 反馈信息收集

如果仍然无法看到控件，请提供以下信息：

### 1. 完整日志

```bash
adb logcat -d > logcat_output.txt
```

发送 `logcat_output.txt` 文件

### 2. 截图

截取播放页面的屏幕，确认是否有任何覆盖层显示

### 3. 设备信息

```bash
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
```

### 4. 测试结果

回答以下问题：
- [ ] 是否看到 "收到 OverlayConfig" 日志？
- [ ] 是否看到 "OverlayView 已添加到根布局" 日志？
- [ ] 是否看到 "开始绘制 overlay" 日志？
- [ ] 是否看到 "绘制时钟/计时器/弹幕" 日志？
- [ ] 屏幕上是否有任何变化（即使不是预期的控件）？

---

## 💡 可能的根本原因

根据经验，SurfaceView 透明叠加层不显示的常见原因：

1. **SurfaceView 的 Z-order 问题**
   - SurfaceView 有独立的 Surface，可能不在正确的层级

2. **PlayerView 覆盖了 OverlayView**
   - PlayerView 内部也有 Surface，可能层级更高

3. **透明格式不支持**
   - 某些设备上 PixelFormat.TRANSPARENT 可能不工作

4. **绘制线程问题**
   - SurfaceView 需要在正确的线程上绘制

---

## 🎯 下一步

根据日志输出，我们可以确定：

- **如果有所有日志但看不到** → SurfaceView 渲染问题，需要改用其他方案
- **如果缺少某些日志** → 代码逻辑问题，需要修复相应部分
- **如果完全没有日志** → OverlayView 未被创建或添加

请运行测试并提供日志输出，我会根据实际情况调整方案！
