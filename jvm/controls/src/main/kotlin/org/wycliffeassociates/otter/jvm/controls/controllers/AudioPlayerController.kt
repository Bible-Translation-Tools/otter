package org.wycliffeassociates.otter.jvm.controls.controllers

import com.github.thomasnield.rxkotlinfx.observeOnFx
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.control.Slider
import org.wycliffeassociates.otter.common.device.AudioPlayerEvent
import org.wycliffeassociates.otter.common.device.IAudioPlayer
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

class AudioPlayerController(
    private var player: IAudioPlayer?,
    private val audioSlider: Slider
) {

    private var startAtLocation = 0
    private var disposable: Disposable? = null
    private var dragging = false
    private var resumeAfterDrag = false

    val isPlayingProperty = SimpleBooleanProperty(false)

    init {
        initializeSliderActions()
    }

    fun toggle() {
        disposable?.dispose()
        player?.let { _player ->
            if (_player.isPlaying()) {
                pause()
            } else {
                disposable = startProgressUpdate()
                play()
                _player.addEventListener {
                    if (
                        it == AudioPlayerEvent.PAUSE ||
                        it == AudioPlayerEvent.STOP ||
                        it == AudioPlayerEvent.COMPLETE
                    ) {
                        disposable?.dispose()
                        Platform.runLater {
                            isPlayingProperty.set(false)
                            if (it == AudioPlayerEvent.COMPLETE) {
                                audioSlider.value = 0.0
                                _player.getAudioReader()?.seek(0)
                            }
                        }
                    }
                }
                isPlayingProperty.set(true)
            }
        }
    }

    fun load(player: IAudioPlayer) {
        this.player?.let { oldPlayer ->
            oldPlayer.pause()
            oldPlayer.close()
        }
        audioSlider.value = 0.0
        this.player = player
    }

    private fun initializeSliderActions() {
        audioSlider.value = 0.0
        audioSlider.setOnDragDetected {
            if (player?.isPlaying() == true) {
                resumeAfterDrag = true
                toggle()
            }
            dragging = true
        }
        audioSlider.setOnMouseClicked {
            val percent = max(0.0, min(it.x / audioSlider.width, 1.0))
            var wasPlaying = false
            if (player?.isPlaying() == true) {
                toggle()
                wasPlaying = true
            }
            seek(percentageToLocation(percent))
            if (wasPlaying) {
                toggle()
            }
        }
    }

    private fun startProgressUpdate(): Disposable {
        return Observable
            .interval(8, TimeUnit.MILLISECONDS)
            .observeOnFx()
            .subscribe {
                if (player?.isPlaying() == true && !audioSlider.isValueChanging && !dragging) {
                    audioSlider.value = playbackPosition().toDouble()
                }
            }
    }

    private fun play() {
        if (startAtLocation != 0) {
            seek(startAtLocation)
        }
        player?.play()
        startAtLocation = 0
    }

    private fun pause() {
        player?.let {
            startAtLocation = it.getAbsoluteLocationInFrames()
            it.pause()
        }
    }

    fun seek(location: Int) {
        player?.let {
            it.seek(location)
            audioSlider.value = location.toDouble()
            if(!it.isPlaying()) {
                startAtLocation = location
            }
        } ?: run {
            startAtLocation = location
        }
    }

    private fun percentageToLocation(percent: Double): Int {
        var _percent = if (percent > 1.00) percent / 100F else percent
        player?.let{
            return (_percent * it.getAbsoluteDurationInFrames()).toInt()
        } ?: run {
            return 0
        }
    }

    private fun playbackPosition(): Int {
        return player?.let {
            it.getAbsoluteLocationInFrames()
        } ?: 0
    }
}
