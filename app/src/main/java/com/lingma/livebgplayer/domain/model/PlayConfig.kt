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
