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
