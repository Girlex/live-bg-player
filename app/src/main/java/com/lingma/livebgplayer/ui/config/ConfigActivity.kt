package com.lingma.livebgplayer.ui.config

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.lingma.livebgplayer.R
import com.lingma.livebgplayer.databinding.ActivityConfigBinding
import com.lingma.livebgplayer.domain.model.LoopMode
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

    private fun startPlayActivity(config: com.lingma.livebgplayer.domain.model.PlayConfig) {
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
