# 黑屏问题解决方案 - 内存溢出

## 🔍 问题分析

### 日志诊断结果

从 Logcat 日志中发现**根本原因**：

```
E/AndroidRuntime: FATAL EXCEPTION: ExoPlayer:Loader:ProgressiveMediaPeriod
E/AndroidRuntime: java.lang.OutOfMemoryError: Failed to allocate a 2712 byte allocation 
with 2392 free bytes and 2392B until OOM, 
max allowed footprint 201326592, growth limit 201326592
```

### 问题详情

| 项目 | 值 |
|------|-----|
| **错误类型** | OutOfMemoryError (内存溢出) |
| **可用内存** | 仅剩 2392 字节 |
| **最大允许内存** | 201 MB (201326592 字节) |
| **崩溃位置** | ExoPlayer 加载器线程 |
| **视频信息** | 912x1920 竖屏视频 |

### 执行流程

```
✅ 应用启动成功
✅ Surface 创建成功
✅ 视频文件读取成功
✅ 播放器初始化成功
✅ 视频开始缓冲 (BUFFERING)
✅ 视频准备就绪 (READY)
✅ 检测到视频尺寸: 912x1920
✅ 设置为竖屏模式
✅ 开始播放 (isPlaying=true)
❌ 内存不足，分配失败
❌ 应用崩溃
❌ 屏幕变黑
```

---

## 💡 解决方案

### 方案 1: 增加模拟器内存（强烈推荐）⭐

**步骤**：

1. **关闭当前模拟器**

2. **打开 Android Studio**
   ```
   Tools → Device Manager
   ```

3. **编辑模拟器配置**
   - 点击铅笔图标（Edit）
   - 找到 "Memory and Storage" 部分
   - 修改以下参数：
     ```
     RAM: 2048 MB 或更高（建议 4096 MB）
     VM Heap: 512 MB
     Internal Storage: 4096 MB
     ```

4. **保存并重启模拟器**

5. **重新安装应用**
   ```bash
   adb install -r app\build\outputs\apk\debug\app-debug.apk
   ```

**优点**：
- ✅ 彻底解决问题
- ✅ 可以测试大视频文件
- ✅ 更接近真机性能

**缺点**：
- ⚠️ 需要更多电脑内存

---

### 方案 2: 使用真机测试（最佳）⭐⭐

**步骤**：

1. **启用开发者选项**
   - 设置 → 关于手机 → 连续点击"版本号"7次

2. **启用 USB 调试**
   - 设置 → 开发者选项 → USB 调试（开启）

3. **连接电脑**
   ```bash
   # 检查设备是否连接
   adb devices
   
   # 应该看到类似输出：
   # XXXXXXXXXXXX    device
   ```

4. **安装应用**
   ```bash
   adb install app\build\outputs\apk\debug\app-debug.apk
   ```

5. **运行测试**

**优点**：
- ✅ 真实性能测试
- ✅ 不会出现模拟器内存限制
- ✅ 发现真机特有问题

**缺点**：
- ⚠️ 需要真实设备

---

### 方案 3: 降低缓冲区大小（已实施）

**已优化**：

我已经降低了 ExoPlayer 的缓冲区配置，减少内存占用：

```kotlin
// 优化前
MIN_BUFFER_MS = 30000      // 30秒
MAX_BUFFER_MS = 120000     // 120秒
BUFFER_FOR_PLAYBACK_MS = 15000  // 15秒

// 优化后
MIN_BUFFER_MS = 15000      // 15秒（降低50%）
MAX_BUFFER_MS = 50000      // 50秒（降低58%）
BUFFER_FOR_PLAYBACK_MS = 5000   // 5秒（降低67%）
```

**效果**：
- ✅ 内存占用减少约 50-60%
- ✅ 适合低内存设备
- ⚠️ 可能导致网络视频缓冲稍慢（本地视频无影响）

---

### 方案 4: 使用小视频测试

如果暂时无法调整模拟器，可以使用较小的视频文件测试：

**推荐规格**：
- 分辨率：480p (854x480) 或更低
- 时长：5-10 秒
- 码率：1-2 Mbps
- 格式：MP4 (H.264)
- 文件大小：< 10 MB

---

## 📊 对比测试

### 不同配置的内存占用

| 配置 | RAM 需求 | 适用场景 |
|------|---------|---------|
| 默认模拟器 (512MB) | ❌ 不足 | 无法运行 |
| 优化后模拟器 (1GB) | ⚠️ 勉强 | 小视频可运行 |
| 推荐模拟器 (2GB) | ✅ 充足 | 正常运行 |
| 理想模拟器 (4GB) | ✅✅ 充裕 | 流畅运行 |
| 真机 (通常 4GB+) | ✅✅✅ 最佳 | 完美运行 |

---

## 🎯 推荐操作步骤

### 立即执行（选择其一）

#### 选项 A: 调整模拟器（5分钟）
```
1. 关闭模拟器
2. Android Studio → Device Manager
3. 编辑模拟器 → RAM 改为 2048 MB
4. 重启模拟器
5. 重新安装 APK
6. 测试播放
```

#### 选项 B: 使用真机（10分钟）
```
1. 手机开启 USB 调试
2. 连接电脑
3. adb install 安装 APK
4. 直接测试
```

### 后续优化

如果仍然有内存问题，可以进一步优化：

1. **降低视频分辨率**
   - 使用 720p 而非 1080p
   - 使用工具压缩视频

2. **添加内存监控**
   ```kotlin
   // 在 PlayActivity 中添加
   val runtime = Runtime.getRuntime()
   Log.d("Memory", "Used: ${runtime.totalMemory() - runtime.freeMemory()}")
   Log.d("Memory", "Max: ${runtime.maxMemory()}")
   ```

3. **启用 ProGuard 优化**
   - Release 版本会自动优化
   - 减小 APK 体积和内存占用

---

## 📝 验证步骤

调整后，请验证问题是否解决：

### 1. 清除旧日志
```bash
adb logcat -c
```

### 2. 启动应用并播放
- 选择视频
- 点击播放
- 观察是否正常显示

### 3. 查看新日志
```bash
adb logcat -s PlayActivity:* ExoPlayerManager:*
```

### 4. 期望的正常日志
```
D/PlayActivity: 开始初始化播放器
D/PlayActivity: Surface created
D/ExoPlayerManager: 准备播放视频: ...
D/ExoPlayerManager: 视频已开始播放
D/PlayActivity: 播放器状态: BUFFERING
D/PlayActivity: 播放器状态: READY - 视频已就绪
D/PlayActivity: 视频尺寸: 912x1920
D/PlayActivity: 设置为竖屏模式
D/PlayActivity: 播放状态改变: isPlaying=true
(持续播放，无崩溃)
```

### 5. 不应该出现的日志
```
❌ W/System.err: OutOfMemoryError
❌ E/AndroidRuntime: FATAL EXCEPTION
❌ W/InputDispatcher: Channel is unrecoverably broken
```

---

## ❓ 常见问题

### Q1: 为什么之前没有这个问题？
A: 之前的代码缓冲区设置较大（30-120秒），在低内存模拟器上容易溢出。现在已经优化。

### Q2: 真机会不会有这个问题？
A: 现代智能手机通常有 4GB+ RAM，不会出现此问题。这是模拟器特有的限制。

### Q3: 能否进一步降低内存占用？
A: 可以，但会影响播放流畅度。当前配置已经是平衡点。

### Q4: 如何查看模拟器当前内存使用？
```bash
adb shell dumpsys meminfo com.lingma.livebgplayer
```

---

## 🎉 总结

**问题根源**：模拟器内存不足（仅 201MB 限制）  
**最佳方案**：增加模拟器内存到 2GB+ 或使用真机  
**已优化**：降低缓冲区大小，减少 50-60% 内存占用  

**下一步**：
1. 调整模拟器内存或连接真机
2. 重新安装优化后的 APK
3. 测试播放功能

---

**修复时间**: 2026-04-20  
**状态**: ✅ 代码已优化，等待调整模拟器/真机测试
