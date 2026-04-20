# 黑屏问题调试指南

## 🔍 问题诊断步骤

### 第一步：查看日志（最重要！）

连接手机后，在命令行执行：

```bash
# 方法1: 实时查看应用日志
adb logcat -c
adb logcat -s PlayActivity:* ExoPlayerManager:*

# 方法2: 查看所有相关日志
adb logcat | findstr "PlayActivity\|ExoPlayerManager"

# 方法3: 导出日志到文件
adb logcat -d > debug_log.txt
```

### 第二步：分析日志输出

#### ✅ 正常情况的日志
```
D/PlayActivity: 开始初始化播放器
D/PlayActivity: Surface created
D/PlayActivity: Surface changed: 1920x1080
D/ExoPlayerManager: 准备播放视频: content://...
D/ExoPlayerManager: 视频已开始播放
D/PlayActivity: 播放器状态: BUFFERING
D/PlayActivity: 播放器状态: READY - 视频已就绪
D/PlayActivity: 视频尺寸: 1920x1080
D/PlayActivity: 设置为横屏模式
D/PlayActivity: 播放状态改变: isPlaying=true
```

#### ❌ 异常情况的日志

**情况1: 权限问题**
```
E/ExoPlayerManager: 准备视频失败: Permission denied
```
→ 解决方法：在手机设置中授予存储权限

**情况2: 文件不存在**
```
E/ExoPlayerManager: 准备视频失败: File not found
```
→ 解决方法：重新选择视频文件

**情况3: 格式不支持**
```
E/PlayActivity: ========== 播放错误 ==========
E/PlayActivity: 错误代码: ERROR_CODE_PARSING_CONTAINER_MALFORMED
E/PlayActivity: 错误消息: ...
```
→ 解决方法：使用标准 MP4 格式视频

**情况4: Surface 未创建**
```
(没有任何 Surface created 日志)
```
→ 解决方法：重启应用或设备

---

## 🛠️ 常见问题和解决方案

### 问题1: 完全黑屏，无任何日志

**可能原因**:
- 应用崩溃
- Activity 未启动

**解决步骤**:
```bash
# 1. 检查应用是否运行
adb shell ps | findstr livebgplayer

# 2. 查看完整系统日志
adb logcat -v time

# 3. 重新启动应用
adb shell am start -n com.lingma.livebgplayer/.ui.config.ConfigActivity
```

### 问题2: 有日志但显示错误

**根据错误代码查找解决方案**:

| 错误代码 | 含义 | 解决方法 |
|---------|------|---------|
| ERROR_CODE_IO_UNSPECIFIED | 文件读取错误 | 检查文件是否存在，重新选择 |
| ERROR_CODE_PARSING_CONTAINER_MALFORMED | 文件格式错误 | 使用标准 MP4 格式 |
| ERROR_CODE_DECODER_INIT_FAILED | 解码器初始化失败 | 重启设备，检查视频编码 |
| ERROR_CODE_BEHIND_LIVE_WINDOW | 直播窗口错误 | 不适用（本地视频） |

### 问题3: 显示"播放器状态: READY"但仍然黑屏

**可能原因**:
- SurfaceView 渲染问题
- 视频编码不兼容

**解决步骤**:
```bash
# 1. 检查视频信息
adb shell media info <video_path>

# 2. 尝试其他视频文件
# 推荐使用这个测试视频参数：
# - 格式: MP4 (H.264 + AAC)
# - 分辨率: 1920x1080 或 1280x720
# - 帧率: 30fps
# - 时长: 10-30秒
```

### 问题4: 权限被拒绝

**Android 13+ 需要特殊处理**:

```bash
# 手动授予权限
adb shell pm grant com.lingma.livebgplayer android.permission.READ_MEDIA_VIDEO

# Android 12 及以下
adb shell pm grant com.lingma.livebgplayer android.permission.READ_EXTERNAL_STORAGE
```

---

## 📱 推荐的测试视频

### 测试视频规格
为了确保不是视频本身的问题，请使用以下规格的视频测试：

**推荐配置**:
- **格式**: MP4
- **视频编码**: H.264 (AVC)
- **音频编码**: AAC
- **分辨率**: 1920x1080 (横屏) 或 1080x1920 (竖屏)
- **帧率**: 30 fps
- **码率**: 5-10 Mbps
- **时长**: 10-30 秒

**获取测试视频**:
1. 用手机相机录制一段视频
2. 从网上下载标准 MP4 测试视频
3. 使用格式工厂转换现有视频

---

## 🔧 高级调试技巧

### 1. 使用 Android Studio Profiler

```
1. 打开 Android Studio
2. 运行应用
3. View → Tool Windows → Profiler
4. 选择你的设备和进程
5. 查看 CPU、Memory、Network 使用情况
```

### 2. 检查 GPU 渲染

```bash
# 启用 GPU 渲染分析
adb shell setprop debug.hwui.profile visual_bars

# 查看渲染性能
adb shell dumpsys gfxinfo com.lingma.livebgplayer
```

### 3. 检查媒体编解码器

```bash
# 列出所有可用的编解码器
adb shell media codecs

# 检查 H.264 支持
adb shell media codecs | findstr "avc"
```

### 4. 测试 SurfaceView

创建一个简单的测试 Activity 来验证 SurfaceView 是否正常工作。

---

## 📋 检查清单

在报告问题前，请确认：

- [ ] 已查看 Logcat 日志
- [ ] 已授予存储权限
- [ ] 使用的是标准 MP4 格式视频
- [ ] 视频文件未损坏
- [ ] 设备有足够的存储空间
- [ ] 已尝试重启应用
- [ ] 已尝试其他视频文件
- [ ] 设备 Android 版本 >= 5.0

---

## 🆘 仍然无法解决？

如果按照以上步骤仍然无法解决，请提供以下信息：

### 必需信息
1. **完整日志**
   ```bash
   adb logcat -d > full_log.txt
   ```

2. **设备信息**
   ```bash
   adb shell getprop ro.build.version.release  # Android 版本
   adb shell getprop ro.product.model           # 设备型号
   adb shell getprop ro.product.manufacturer    # 制造商
   ```

3. **视频信息**
   - 视频格式
   - 视频分辨率
   - 视频时长
   - 文件大小

4. **问题描述**
   - 何时开始出现黑屏
   - 是否所有视频都黑屏
   - 是否有错误提示
   - 之前是否能正常播放

---

## 💡 临时解决方案

如果急需使用，可以尝试：

### 方案1: 使用系统播放器测试
```bash
# 用系统播放器打开视频，确认视频本身没问题
adb shell am start -a android.intent.action.VIEW \
  -d "<video_uri>" \
  -t "video/*"
```

### 方案2: 降低视频规格
- 转换为更低分辨率（如 720p）
- 降低码率
- 使用标准 H.264 编码

### 方案3: 清除应用数据
```bash
adb shell pm clear com.lingma.livebgplayer
```

---

## 📞 联系支持

如果问题依然存在，请：
1. 保存完整日志文件
2. 记录重现步骤
3. 提供设备型号和 Android 版本
4. 附上测试视频样本

---

**最后更新**: 2026-04-20  
**文档版本**: 1.0
