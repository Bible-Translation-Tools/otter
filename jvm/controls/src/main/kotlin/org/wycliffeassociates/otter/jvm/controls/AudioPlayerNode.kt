package org.wycliffeassociates.otter.jvm.controls

import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.control.Control
import javafx.scene.control.Skin
import org.wycliffeassociates.otter.common.device.IAudioPlayer
import org.wycliffeassociates.otter.jvm.controls.skins.AudioPlayerSkin

class AudioPlayerNode(var player: IAudioPlayer?) : Control() {

    var loaded = false
    var startAtPercent = 0F

    val isPlaying: Boolean
        get() = player?.isPlaying() ?: false

    val playbackPercentage = SimpleDoubleProperty(0.0)

    val sourceAvailable: Boolean
        get() = player != null

    fun load(player: IAudioPlayer) {
        this.player = player
    }

    fun play() {
        player?.play()
    }

    fun pause() {
        player?.pause()
    }

    fun seek(percent: Float) {
        player?.let {
            val position = (it.getAbsoluteDurationInFrames() * percent).toInt()
            it.seek(position)
        }
    }

    fun playbackPosition(): Double {
        return player?.let {
            val position = it.getAbsoluteLocationInFrames()
            val total = it.getAbsoluteDurationInFrames()
            (position / total.toDouble()).times(100)
        } ?: 0.0
    }

    override fun createDefaultSkin(): Skin<*> {
        return AudioPlayerSkin(this)
    }
}