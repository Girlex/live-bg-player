package com.lingma.livebgplayer.ui.config

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.lingma.livebgplayer.R
import com.lingma.livebgplayer.databinding.ActivityConfigBinding
import com.lingma.livebgplayer.databinding.DialogOverlayPreviewBinding
import com.lingma.livebgplayer.domain.model.ClockPosition
import com.lingma.livebgplayer.domain.model.LoopMode
import com.lingma.livebgplayer.domain.model.OverlayConfig
import com.lingma.livebgplayer.domain.model.TimerMode
import com.lingma.livebgplayer.domain.model.TimerPosition
import com.lingma.livebgplayer.ui.overlay.OverlayViewSimple
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
        
        // 设置状态栏颜色与背景色一致
        setupStatusBar()
        
        binding = ActivityConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this)[ConfigViewModel::class.java]
        
        setupListeners()
        observeViewModel()
    }
    
    private fun setupStatusBar() {
        // 设置状态栏为浅蓝色，与背景色一致
        window.statusBarColor = Color.parseColor("#E3F2FD")
        
        // 设置状态栏文字为深色（因为背景是浅色）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    private fun setupListeners() {
        binding.btnSelectVideo.setOnClickListener {
            pickVideoLauncher.launch("video/*")
        }
        
        binding.seekbarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val volumeText = when {
                    progress == 0 -> "0% - 静音模式"
                    progress < 30 -> "$progress% - 低音量"
                    progress < 70 -> "$progress% - 中音量"
                    else -> "$progress% - 高音量"
                }
                binding.tvVolumeValue.text = volumeText
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
        
        // 画面控件设置
        var showClock = false
        var showTimer = false
        var showDanmaku = false
        var clockPosition = ClockPosition.BOTTOM_RIGHT
        var timerPosition = TimerPosition.BOTTOM_LEFT
        
        // 初始化位置选择器
        val positionOptions = arrayOf("左上角", "右上角", "左下角", "右下角")
        val clockAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, positionOptions)
        clockAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerClockPosition.adapter = clockAdapter
        
        val timerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, positionOptions)
        timerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTimerPosition.adapter = timerAdapter
        
        binding.checkboxShowClock.setOnCheckedChangeListener { _, isChecked ->
            showClock = isChecked
            binding.spinnerClockPosition.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        
        binding.checkboxShowTimer.setOnCheckedChangeListener { _, isChecked ->
            showTimer = isChecked
            binding.spinnerTimerPosition.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        
        binding.checkboxShowDanmaku.setOnCheckedChangeListener { _, isChecked ->
            showDanmaku = isChecked
        }
        
        binding.spinnerClockPosition.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                clockPosition = when (position) {
                    0 -> ClockPosition.TOP_LEFT
                    1 -> ClockPosition.TOP_RIGHT
                    2 -> ClockPosition.BOTTOM_LEFT
                    3 -> ClockPosition.BOTTOM_RIGHT
                    else -> ClockPosition.BOTTOM_RIGHT
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        binding.spinnerTimerPosition.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                timerPosition = when (position) {
                    0 -> TimerPosition.TOP_LEFT
                    1 -> TimerPosition.TOP_RIGHT
                    2 -> TimerPosition.BOTTOM_LEFT
                    3 -> TimerPosition.BOTTOM_RIGHT
                    else -> TimerPosition.BOTTOM_LEFT
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        binding.btnConfirm.setOnClickListener {
            val config = viewModel.getCurrentConfig()
            if (config.videoUri != Uri.EMPTY) {
                // 创建 OverlayConfig
                val overlayConfig = OverlayConfig(
                    showClock = showClock,
                    clockPosition = clockPosition,
                    clockFontSize = 16,
                    showTimer = showTimer,
                    timerInitialSeconds = 0,
                    timerMode = TimerMode.COUNT_UP,
                    timerPosition = timerPosition,
                    timerFontSize = 16,
                    showDanmaku = showDanmaku,
                    danmakuText = "欢迎来到直播间",
                    danmakuSpeed = 5
                )
                
                startPlayActivity(config, overlayConfig)
            } else {
                // 提示选择视频
                Toast.makeText(this, "请先选择视频文件", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 预览按钮
        binding.btnPreviewOverlay.setOnClickListener {
            showOverlayPreview(showClock, clockPosition, showTimer, timerPosition, showDanmaku)
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

    private fun startPlayActivity(
        config: com.lingma.livebgplayer.domain.model.PlayConfig,
        overlayConfig: OverlayConfig
    ) {
        val intent = Intent(this, PlayActivity::class.java).apply {
            putExtra(PlayActivity.EXTRA_VIDEO_URI, config.videoUri.toString())
            putExtra(PlayActivity.EXTRA_OVERLAY_CONFIG, overlayConfig)
            putExtra(PlayActivity.EXTRA_LOOP_MODE, config.loopMode.name)
            putExtra(PlayActivity.EXTRA_VOLUME, config.volume)
            putExtra(PlayActivity.EXTRA_KEEP_SCREEN_ON, config.keepScreenOn)
        }
        startActivity(intent)
        // 可选：finish() 使配置页不在返回栈中
    }

    /**
     * 显示覆盖层预览对话框
     */
    private fun showOverlayPreview(
        showClock: Boolean,
        clockPosition: ClockPosition,
        showTimer: Boolean,
        timerPosition: TimerPosition,
        showDanmaku: Boolean
    ) {
        // 创建对话框
        val dialogBuilder = AlertDialog.Builder(this)
        val previewBinding = DialogOverlayPreviewBinding.inflate(layoutInflater)
        
        dialogBuilder.setView(previewBinding.root)
        dialogBuilder.setTitle("🎨 控件预览")
        
        val dialog = dialogBuilder.create()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.window?.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        
        dialog.show()
        
        // 创建并添加 OverlayViewSimple
        val overlayConfig = OverlayConfig(
            showClock = showClock,
            clockPosition = clockPosition,
            clockFontSize = 18,
            showTimer = showTimer,
            timerInitialSeconds = 0,
            timerMode = TimerMode.COUNT_UP,
            timerPosition = timerPosition,
            timerFontSize = 18,
            showDanmaku = showDanmaku,
            danmakuText = "欢迎来到直播间 - 预览模式",
            danmakuSpeed = 5
        )
        
        val overlayView = OverlayViewSimple(this).apply {
            updateConfig(overlayConfig)
            previewBinding.root.addView(
                this,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        // 3秒后自动关闭
        android.os.Handler(mainLooper).postDelayed({
            if (dialog.isShowing) {
                dialog.dismiss()
            }
        }, 5000)
    }
}
