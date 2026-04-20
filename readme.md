# 零卡顿游戏直播背景视频循环播放 App 开发文档（完整版）

---

## 文档版本信息

| 版本 | 日期 | 修订内容 | 作者 |
|------|------|----------|------|
| 1.0 | 2026-04-20 | 初始完整版，包含配置页与全屏播放页 | Lingma |

---

## 一、项目概述

### 1.1 项目简介

本项目旨在开发一款专用于游戏直播场景的 Android 视频循环播放 App。App 启动后首先进入配置界面，用户可选择待播放的视频文件并进行基础设置；确认配置后进入全屏无遮挡的播放界面，持续不断地循环播放预设视频，为游戏直播提供稳定的背景画面、片头片尾或中场候场画面。

### 1.2 核心目标

| 目标 | 具体要求 |
|------|----------|
| 配置优先 | 第一界面为配置页，用户可选定视频源及播放参数 |
| 循环播放 | 无限循环播放指定视频，无缝衔接，无黑屏 |
| 零卡顿 | 杜绝任何掉帧、卡顿、缓冲中断问题 |
| 零 Bug | 代码健壮，全面覆盖异常场景 |
| 全屏无遮挡 | 播放页无状态栏、导航栏、任何悬浮控件，画面完全纯净 |
| 资源稳定 | 内存不泄漏，不 OOM，不 Crash |
| 低功耗 | 合理控制电量消耗，支持长时间运行 |

### 1.3 适用场景

- 游戏直播背景循环画面
- 直播间片头 / 片尾动画
- 直播中断时的候场视频
- 虚拟直播间场景展示

### 1.4 用户流程

```
┌─────────────────┐     确认配置      ┌─────────────────┐
│   ConfigActivity │ ───────────────► │  PlayActivity   │
│  (配置第一界面)   │                  │ (全屏无遮挡播放) │
└─────────────────┘                  └─────────────────┘
        │                                    │
        │ 选择视频源、循环模式、音量等          │ 全屏沉浸式播放
        │                                    │ 无任何控件、状态栏、导航栏
        └────────────────────────────────────┘
```

---

## 二、技术选型

### 2.1 播放器核心

选择 **AndroidX Media3 ExoPlayer** 作为核心播放引擎，理由如下：

- Google 官方维护，持续更新
- 支持 `REPEAT_MODE_ONE` 实现原生无缝循环
- 支持硬件加速解码，性能卓越
- 完善的缓冲机制和状态管理
- 可扩展性强，支持自定义组件

### 2.2 技术栈

| 层级 | 技术方案 |
|------|----------|
| 开发语言 | Kotlin |
| 最低 SDK | Android 5.0 (API 21) |
| 目标 SDK | Android 14 (API 34) |
| 播放核心 | AndroidX Media3 ExoPlayer 1.4.1 |
| UI 框架 | 传统 XML + ViewBinding（保证稳定性） |
| 架构模式 | MVVM + Repository + UseCase |
| 缓存方案 | ExoPlayer SimpleCache（磁盘缓存） |
| 日志监控 | Timber |
| 依赖注入 | 手动依赖注入（或 Koin） |

### 2.3 依赖配置

```kotlin
// build.gradle.kts (app)
dependencies {
    // Media3 ExoPlayer
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.4.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.4.1")
    
    // 磁盘缓存
    implementation("androidx.media3:media3-database:1.4.1")
    implementation("androidx.media3:media3-datasource-cronet:1.4.1")
    
    // 生命周期
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    
    // 协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // 日志
    implementation("com.jakewharton.timber:timber:5.0.1")
    
    // 内存泄漏检测（debug）
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
    
    // ViewBinding
    implementation("androidx.databinding:viewbinding:8.5.0")
}
```

---

## 三、系统架构设计

### 3.1 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                         Presentation Layer                   │
│  ┌─────────────────┐  ┌─────────────────┐                   │
│  │ ConfigActivity  │  │  PlayActivity   │                   │
│  └────────┬────────┘  └────────┬────────┘                   │
│           │                    │                            │
│  ┌────────▼────────────────────▼────────┐                   │
│  │      ConfigViewModel / PlayViewModel │                   │
│  └─────────────────┬────────────────────┘                   │
├────────────────────┼────────────────────────────────────────┤
│                    ▼                         Domain Layer    │
│  ┌─────────────────────────────────────┐                    │
│  │         VideoPlayUseCase            │                    │
│  │  ┌──────────┐  ┌──────────┐        │                    │
│  │  │PlayVideo │  │StopVideo │ ...    │                    │
│  │  └──────────┘  └──────────┘        │                    │
│  └─────────────────┬───────────────────┘                    │
├────────────────────┼────────────────────────────────────────┤
│                    ▼                         Data Layer      │
│  ┌─────────────────────────────────────┐                    │
│  │         VideoRepository              │                    │
│  │  ┌──────────────┐ ┌──────────────┐  │                    │
│  │  │VideoPreloader │ │CacheManager  │  │                    │
│  │  └──────────────┘ └──────────────┘  │                    │
│  └─────────────────┬───────────────────┘                    │
│                    ▼                                        │
│  ┌─────────────────────────────────────┐                    │
│  │    ExoPlayerManager (封装层)         │                    │
│  │  ┌─────────────────────────────┐    │                    │
│  │  │   AndroidX Media3 ExoPlayer │    │                    │
│  │  └─────────────────────────────┘    │                    │
│  └─────────────────────────────────────┘                    │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 模块划分

| 模块 | 职责 |
|------|------|
| `player-core` | ExoPlayer 初始化、配置、生命周期管理 |
| `video-cache` | 视频磁盘缓存策略实现 |
| `video-preload` | 视频预加载与预热机制 |
| `playback-control` | 播放控制（播放 / 暂停 / 循环 / 音量） |
| `error-handler` | 全局异常捕获与恢复 |
| `performance-monitor` | 性能监控（内存 / CPU / 帧率） |

---

## 四、配置界面详细设计

### 4.1 功能模块

- **视频源选择**：支持从本地文件选择（通过系统文件选择器）
- **循环模式**：单视频循环（默认）、列表顺序循环（预留扩展）
- **音量控制**：拖动条设置音量（0% ~ 100%），游戏直播通常建议静音
- **屏幕常亮开关**：默认开启
- **确认按钮**：点击后跳转播放页并传递配置参数

### 4.2 布局文件

```xml
<!-- res/layout/activity_config.xml -->
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="24dp"
    android:background="@color/background">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/app_name"
        android:textSize="24sp"
        android:textStyle="bold"
        android:layout_marginBottom="32dp"
        android:layout_gravity="center_horizontal"/>

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="选择直播背景视频"
        android:textSize="18sp"
        android:layout_marginBottom="16dp"/>

    <Button
        android:id="@+id/btn_select_video"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="从本地选择视频"/>

    <TextView
        android:id="@+id/tv_selected_path"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="未选择"
        android:textSize="14sp"
        android:layout_marginTop="8dp"
        android:layout_marginBottom="16dp"
        android:ellipsize="middle"
        android:singleLine="true"/>

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="循环模式"
        android:textSize="16sp"
        android:layout_marginTop="16dp"/>

    <Spinner
        android:id="@+id/spinner_loop_mode"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:entries="@array/loop_modes"/>

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="音量"
        android:textSize="16sp"
        android:layout_marginTop="24dp"/>

    <SeekBar
        android:id="@+id/seekbar_volume"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:max="100"
        android:progress="0"/>

    <TextView
        android:id="@+id/tv_volume_value"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="0%"
        android:textSize="14sp"
        android:layout_marginBottom="16dp"/>

    <CheckBox
        android:id="@+id/checkbox_keep_screen_on"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="保持屏幕常亮"
        android:checked="true"/>

    <Button
        android:id="@+id/btn_confirm"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="确认并开始播放"
        android:layout_marginTop="32dp"/>

</LinearLayout>
```

### 4.3 Activity 代码

```kotlin
// ConfigActivity.kt
package com.lingma.livebgplayer.ui.config

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.lingma.livebgplayer.R
import com.lingma.livebgplayer.databinding.ActivityConfigBinding
import com.lingma.livebgplayer.domain.model.LoopMode
import com.lingma.livebgplayer.domain.model.PlayConfig
import com.lingma.livebgplayer.ui.play.PlayActivity

class ConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfigBinding
    private lateinit var viewModel: ConfigViewModel
    
    // 文件选择器
    private val pickVideoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setSelectedVideo(it)
            updateSelectedPath(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this)[ConfigViewModel::class.java]
        
        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnSelectVideo.setOnClickListener {
            pickVideoLauncher.launch("video/*")
        }
        
        binding.seekbarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.tvVolumeValue.text = "$progress%"
                viewModel.setVolume(progress / 100f)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        binding.spinnerLoopMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val mode = if (position == 0) LoopMode.SINGLE else LoopMode.LIST
                viewModel.setLoopMode(mode)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        binding.checkboxKeepScreenOn.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setKeepScreenOn(isChecked)
        }
        
        binding.btnConfirm.setOnClickListener {
            val config = viewModel.getCurrentConfig()
            if (config.videoUri != Uri.EMPTY) {
                startPlayActivity(config)
            } else {
                // 提示选择视频
                Toast.makeText(this, "请先选择视频文件", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.selectedVideoUri.observe(this) { uri ->
            updateSelectedPath(uri)
            binding.btnConfirm.isEnabled = uri != Uri.EMPTY
        }
    }

    private fun updateSelectedPath(uri: Uri) {
        // 获取文件名显示
        val fileName = uri.lastPathSegment ?: uri.toString()
        binding.tvSelectedPath.text = "已选择: $fileName"
    }

    private fun startPlayActivity(config: PlayConfig) {
        val intent = Intent(this, PlayActivity::class.java).apply {
            putExtra(PlayActivity.EXTRA_VIDEO_URI, config.videoUri.toString())
            putExtra(PlayActivity.EXTRA_LOOP_MODE, config.loopMode.name)
            putExtra(PlayActivity.EXTRA_VOLUME, config.volume)
            putExtra(PlayActivity.EXTRA_KEEP_SCREEN_ON, config.keepScreenOn)
        }
        startActivity(intent)
        // 可选：finish() 使配置页不在返回栈中
    }
}
```

### 4.4 ViewModel 与数据模型

```kotlin
// ConfigViewModel.kt
package com.lingma.livebgplayer.ui.config

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lingma.livebgplayer.domain.model.LoopMode
import com.lingma.livebgplayer.domain.model.PlayConfig

class ConfigViewModel : ViewModel() {

    private val _selectedVideoUri = MutableLiveData(Uri.EMPTY)
    val selectedVideoUri: LiveData<Uri> = _selectedVideoUri

    private val _loopMode = MutableLiveData(LoopMode.SINGLE)
    val loopMode: LiveData<LoopMode> = _loopMode

    private val _volume = MutableLiveData(0f)
    val volume: LiveData<Float> = _volume

    private val _keepScreenOn = MutableLiveData(true)
    val keepScreenOn: LiveData<Boolean> = _keepScreenOn

    fun setSelectedVideo(uri: Uri) {
        _selectedVideoUri.value = uri
    }

    fun setLoopMode(mode: LoopMode) {
        _loopMode.value = mode
    }

    fun setVolume(value: Float) {
        _volume.value = value.coerceIn(0f, 1f)
    }

    fun setKeepScreenOn(enabled: Boolean) {
        _keepScreenOn.value = enabled
    }

    fun getCurrentConfig(): PlayConfig {
        return PlayConfig(
            videoUri = _selectedVideoUri.value ?: Uri.EMPTY,
            loopMode = _loopMode.value ?: LoopMode.SINGLE,
            volume = _volume.value ?: 0f,
            keepScreenOn = _keepScreenOn.value ?: true
        )
    }
}
```

```kotlin
// domain/model/PlayConfig.kt
package com.lingma.livebgplayer.domain.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlayConfig(
    val videoUri: Uri,
    val loopMode: LoopMode,
    val volume: Float,
    val keepScreenOn: Boolean
) : Parcelable

enum class LoopMode {
    SINGLE,     // 单视频循环
    LIST        // 列表顺序循环（预留）
}
```

---

## 五、全屏无遮挡播放界面详细设计

### 5.1 核心要求

| 要求 | 实现方式 |
|------|----------|
| 无状态栏 | 设置 `WindowInsetsController` 隐藏状态栏 |
| 无导航栏 | 沉浸模式隐藏虚拟按键 |
| 无任何控件覆盖 | 布局仅包含 `SurfaceView`，无 Button、TextView、SeekBar |
| 防止误触 | 拦截触摸事件，不响应点击（退出通过物理按键组合） |
| 屏幕常亮 | 根据配置设置 `keepScreenOn` |
| 旋转锁定 | 固定横屏，避免切换导致画面重建 |
| 退出方式 | 音量键组合长按退出（不显示任何 UI） |

### 5.2 布局与界面配置

**播放页不使用 XML 布局文件，纯代码创建 SurfaceView 作为根视图。**

```kotlin
// PlayActivity.kt
package com.lingma.livebgplayer.ui.play

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.lingma.livebgplayer.domain.model.LoopMode
import com.lingma.livebgplayer.player.ExoPlayerManager
import kotlinx.coroutines.launch

class PlayActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_VIDEO_URI = "extra_video_uri"
        const val EXTRA_LOOP_MODE = "extra_loop_mode"
        const val EXTRA_VOLUME = "extra_volume"
        const val EXTRA_KEEP_SCREEN_ON = "extra_keep_screen_on"
    }

    private lateinit var playerManager: ExoPlayerManager
    private lateinit var surfaceView: SurfaceView
    private var videoUri: Uri = Uri.EMPTY
    private var volume: Float = 0f
    private var keepScreenOn: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. 解析 Intent 参数
        parseIntent()
        
        // 2. 设置全屏标志
        setupFullScreen()
        
        // 3. 创建纯 SurfaceView 布局
        surfaceView = SurfaceView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // 设置背景色为黑色，防止可能的短暂黑屏闪烁
            setBackgroundColor(android.graphics.Color.BLACK)
            // 屏幕常亮控制
            holder.setKeepScreenOn(keepScreenOn)
        }
        setContentView(surfaceView)
        
        // 4. 初始化播放器（使用 Application Context 防止内存泄漏）
        playerManager = ExoPlayerManager(applicationContext)
        val player = playerManager.initialize()
        playerManager.setVolume(volume)
        player.setVideoSurfaceView(surfaceView)
        
        // 5. 开始播放
        lifecycleScope.launch {
            playerManager.prepareAndPlay(videoUri)
        }
    }

    private fun parseIntent() {
        val uriString = intent.getStringExtra(EXTRA_VIDEO_URI)
        if (!uriString.isNullOrEmpty()) {
            videoUri = Uri.parse(uriString)
        }
        volume = intent.getFloatExtra(EXTRA_VOLUME, 0f)
        keepScreenOn = intent.getBooleanExtra(EXTRA_KEEP_SCREEN_ON, true)
        // loopMode 暂用于后续扩展
    }

    private fun setupFullScreen() {
        // 设置全屏布局标志（让内容延伸到状态栏/导航栏区域）
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 使用 WindowInsetsController
            window.insetsController?.apply {
                hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // 旧版本使用 systemUiVisibility
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
        
        // 保持屏幕常亮（根据配置）
        if (keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // 拦截触摸事件，防止任何意外操作
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 消费所有触摸事件，不产生任何交互反馈
        return true
    }

    // 退出机制：长按音量上键或下键
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (event.isLongPress) {
                    finish()
                    true
                } else {
                    // 不处理短按，让系统调节音量（或可以屏蔽掉）
                    true
                }
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onStart() {
        super.onStart()
        playerManager.play()
    }

    override fun onStop() {
        super.onStop()
        playerManager.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        playerManager.release()
    }
}
```

### 5.3 AndroidManifest 配置

```xml
<!-- AndroidManifest.xml -->
<application
    android:hardwareAccelerated="true"
    android:theme="@style/Theme.LiveBgPlayer"
    ...>
    
    <!-- 配置页 -->
    <activity
        android:name=".ui.config.ConfigActivity"
        android:exported="true"
        android:screenOrientation="portrait">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
    
    <!-- 播放页：全屏无遮挡 -->
    <activity
        android:name=".ui.play.PlayActivity"
        android:exported="false"
        android:configChanges="orientation|screenSize|keyboardHidden"
        android:screenOrientation="landscape"
        android:theme="@style/Theme.FullScreen"
        android:hardwareAccelerated="true" />
        
</application>
```

### 5.4 主题样式

```xml
<!-- res/values/themes.xml -->
<resources>
    <!-- 基础主题 -->
    <style name="Theme.LiveBgPlayer" parent="Theme.AppCompat.Light.DarkActionBar">
        <item name="colorPrimary">@color/purple_500</item>
        <item name="colorPrimaryVariant">@color/purple_700</item>
        <item name="colorOnPrimary">@color/white</item>
    </style>
    
    <!-- 全屏无遮挡主题 -->
    <style name="Theme.FullScreen" parent="Theme.AppCompat.NoActionBar">
        <item name="android:windowFullscreen">true</item>
        <item name="android:windowLayoutInDisplayCutoutMode">shortEdges</item>
        <item name="android:windowTranslucentStatus">true</item>
        <item name="android:windowTranslucentNavigation">true</item>
        <item name="android:windowBackground">@android:color/black</item>
    </style>
</resources>
```

---

## 六、播放器核心实现

### 6.1 ExoPlayerManager 封装类

```kotlin
// player/ExoPlayerManager.kt
package com.lingma.livebgplayer.player

import android.content.Context
import android.net.Uri
import android.os.Looper
import android.util.Log
import android.view.SurfaceView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExoPlayerManager(private val context: Context) {

    companion object {
        private const val TAG = "ExoPlayerManager"
        // 缓冲区配置
        private const val MIN_BUFFER_MS = 30000L      // 最小缓冲 30 秒
        private const val MAX_BUFFER_MS = 120000L     // 最大缓冲 120 秒
        private const val BUFFER_FOR_PLAYBACK_MS = 15000L  // 起播缓冲 15 秒
        private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 10000L
    }

    private var exoPlayer: ExoPlayer? = null
    private var mediaSourceFactory: DefaultMediaSourceFactory

    init {
        // 创建数据源工厂（带缓存）
        val cacheDataSourceFactory = VideoCacheManager.getCacheDataSourceFactory(context)
        mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)
    }

    // 初始化播放器
    fun initialize(): ExoPlayer {
        return ExoPlayer.Builder(context)
            .setLooper(Looper.getMainLooper())
            .setLoadControl(createLoadControl())
            .setRenderersFactory(createRenderersFactory())
            .setTrackSelector(DefaultTrackSelector(context))
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                // 核心配置
                repeatMode = Player.REPEAT_MODE_ONE        // 单视频无限循环
                playWhenReady = true                       // 准备就绪后自动播放
                volume = 1.0f
                // 添加监听器
                addListener(createPlayerListener())
            }.also {
                exoPlayer = it
            }
    }

    // 自定义缓冲策略 - 核心防卡顿配置
    private fun createLoadControl(): LoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                MIN_BUFFER_MS,
                MAX_BUFFER_MS,
                BUFFER_FOR_PLAYBACK_MS,
                BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .setPrioritizeTimeOverSizeThresholds(true)  // 优先保证时间阈值
            .setBackBuffer(10000, false)  // 保留 10 秒回退缓冲
            .build()
    }

    // 渲染器工厂 - 启用硬件加速
    private fun createRenderersFactory(): DefaultRenderersFactory {
        return DefaultRenderersFactory(context).apply {
            setEnableDecoderFallback(true)           // 解码器降级
            setEnableAudioTrackPlaybackParams(true)  // 音频轨道参数优化
        }
    }

    // 播放器监听器
    private fun createPlayerListener(): Player.Listener {
        return object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_IDLE -> Log.d(TAG, "Player idle")
                    Player.STATE_BUFFERING -> Log.d(TAG, "Player buffering")
                    Player.STATE_READY -> Log.d(TAG, "Player ready")
                    Player.STATE_ENDED -> {
                        // 循环模式不会触发此状态，但为防万一，手动处理
                        Log.d(TAG, "Player ended, restarting")
                        exoPlayer?.seekToDefaultPosition()
                        exoPlayer?.playWhenReady = true
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Playback error: ${error.message}")
                handlePlaybackError(error)
            }
        }
    }

    // 错误处理
    private fun handlePlaybackError(error: PlaybackException) {
        // 简单重试策略
        if (error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
            error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED) {
            // 延迟重试
            exoPlayer?.prepare()
            exoPlayer?.play()
        }
    }

    // 准备并播放视频
    suspend fun prepareAndPlay(videoUri: Uri) {
        withContext(Dispatchers.IO) {
            val mediaSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(videoUri))
            withContext(Dispatchers.Main) {
                exoPlayer?.setMediaSource(mediaSource)
                exoPlayer?.prepare()
                exoPlayer?.playWhenReady = true
            }
        }
    }

    fun play() {
        exoPlayer?.playWhenReady = true
    }

    fun pause() {
        exoPlayer?.playWhenReady = false
    }

    fun setVolume(volume: Float) {
        exoPlayer?.volume = volume.coerceIn(0f, 1f)
    }

    fun release() {
        exoPlayer?.apply {
            stop()
            clearVideoSurface()
            release()
        }
        exoPlayer = null
    }
}
```

### 6.2 视频缓存管理

```kotlin
// player/VideoCacheManager.kt
package com.lingma.livebgplayer.player

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

object VideoCacheManager {

    private const val MAX_CACHE_SIZE = 500L * 1024 * 1024  // 500MB

    @Volatile
    private var simpleCache: SimpleCache? = null

    private fun getCache(context: Context): SimpleCache {
        return simpleCache ?: synchronized(this) {
            simpleCache ?: SimpleCache(
                File(context.cacheDir, "video_cache"),
                LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE),
                StandaloneDatabaseProvider(context)
            ).also { simpleCache = it }
        }
    }

    fun getCacheDataSourceFactory(context: Context): DataSource.Factory {
        val cache = getCache(context)
        val upstreamFactory = DefaultDataSource.Factory(context)
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
}
```

---

## 七、防卡顿与性能优化

### 7.1 硬件加速配置

已在 `AndroidManifest.xml` 和主题中启用硬件加速。

### 7.2 线程优化

所有播放器准备工作在 IO 线程执行，UI 更新在主线程。

### 7.3 电量优化

```kotlin
// utils/PowerOptimizer.kt
class PowerOptimizer(private val context: Context) {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    fun shouldThrottlePerformance(): Boolean {
        return powerManager.isPowerSaveMode
    }

    fun adjustPlaybackQuality(exoPlayer: ExoPlayer) {
        if (shouldThrottlePerformance()) {
            val trackSelector = exoPlayer.trackSelector as? DefaultTrackSelector
            trackSelector?.setParameters(
                trackSelector.parameters.buildUpon()
                    .setMaxVideoSize(1280, 720)
                    .setMaxVideoFrameRate(30)
                    .build()
            )
        }
    }
}
```

### 7.4 内存泄漏防范

- 使用 `applicationContext` 创建 `ExoPlayerManager`
- 在 `onDestroy` 中确保调用 `playerManager.release()`
- 不持有 Activity 引用

---

## 八、测试方案

### 8.1 功能测试用例

| 测试项 | 测试内容 | 预期结果 |
|--------|----------|----------|
| 配置页显示 | 启动 App | 显示配置界面，各控件正常 |
| 视频选择 | 点击选择视频按钮 | 打开系统文件选择器，可选视频 |
| 参数配置 | 调整音量、循环模式、常亮开关 | 状态正确保存 |
| 跳转播放页 | 选择视频后点击确认 | 跳转到全屏播放页，视频自动循环播放 |
| 全屏无遮挡 | 观察播放页 | 无状态栏、导航栏、任何控件 |
| 循环播放 | 视频播放至末尾 | 无缝重新开始，无黑屏停顿 |
| 前后台切换 | 按 Home 键后再返回 | 继续播放，无异常 |
| 长时间运行 | 连续播放 24 小时 | 无卡顿、无 Crash、内存稳定 |
| 退出方式 | 长按音量键 | 退出播放页，返回桌面 |
| 异常视频文件 | 选择损坏的视频文件 | 播放器重试或优雅降级，不 Crash |

### 8.2 性能测试指标

| 指标 | 目标值 | 监控方法 |
|------|--------|----------|
| 启动时间 | < 500ms | SystemClock 计时 |
| 循环切换间隔 | < 100ms | 帧时间戳监控 |
| 帧率稳定性 | > 55 FPS | GPU 渲染分析工具 |
| 内存占用 | < 150MB | Android Studio Profiler |
| CPU 占用 | < 10% | Android Studio Profiler |
| 内存泄漏 | 0 | LeakCanary |

### 8.3 压测脚本示例

```kotlin
@Test
fun testLongRunningPlayback() {
    val scenario = ActivityScenario.launch(PlayActivity::class.java)
    // 模拟长时间运行
    Thread.sleep(TimeUnit.HOURS.toMillis(1))
    scenario.onActivity { activity ->
        assertTrue(activity.playerManager.isPlaying())
    }
}
```

---

## 九、部署与发布

### 9.1 混淆配置

```proguard
# ProGuard rules for ExoPlayer
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Retain generic signatures of TypeToken and its subclasses
-keepattributes Signature

# For Guava
-dontwarn sun.misc.Unsafe
-dontwarn com.google.common.collect.MinMaxPriorityQueue
-keepclasseswithmembers class com.google.common.util.concurrent.** {
    <fields>;
    <methods>;
}

# Keep Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
```

### 9.2 最低系统要求

| 项目 | 要求 |
|------|------|
| Android 版本 | Android 5.0 (API 21) 及以上 |
| RAM | 建议 2GB 以上 |
| 存储 | 预留 1GB 以上空间用于视频缓存 |

---

## 十、常见问题与解决方案

### 10.1 循环时出现短暂黑屏

**原因**：Surface 重建或解码器重新初始化

**解决方案**：
- 使用 `REPEAT_MODE_ONE` 替代手动 seek
- 设置 SurfaceView 背景色为黑色
- 保持 SurfaceView 不被销毁重建

### 10.2 全屏后仍有小白条（导航栏指示条）

**解决方案**：
- 在 Android 10+ 上，需在主题中设置 `true` 隐藏手势指示条区域，或使用 `WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`

### 10.3 播放页如何优雅退出

**解决方案**：
- 采用音量键组合长按退出，不干扰画面捕获

### 10.4 内存持续增长

**解决方案**：
- 使用 LeakCanary 检测内存泄漏
- 确保 `onDestroy` 中释放播放器

---

## 十一、项目里程碑

| 阶段 | 时间 | 交付物 |
|------|------|--------|
| 需求分析与技术选型 | 第 1 周 | 技术方案文档 |
| 配置页开发 | 第 2 周 | 可用的配置界面 |
| 播放器核心开发 | 第 3 周 | 循环播放功能 |
| 全屏无遮挡适配 | 第 4 周 | 沉浸式播放页 |
| 性能优化与测试 | 第 5 周 | 性能达标版本 |
| 发布与上线 | 第 6 周 | 正式版本 APK |

---

**文档结束**

本文档完整涵盖了从配置界面到全屏无遮挡播放的整个流程，重点针对游戏直播场景的特殊需求进行了详细设计，确保 App 稳定、流畅、零卡顿，为游戏直播提供专业级的背景视频播放解决方案。