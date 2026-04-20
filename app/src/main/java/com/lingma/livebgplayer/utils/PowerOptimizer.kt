package com.lingma.livebgplayer.utils

import android.content.Context
import android.os.PowerManager
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

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
