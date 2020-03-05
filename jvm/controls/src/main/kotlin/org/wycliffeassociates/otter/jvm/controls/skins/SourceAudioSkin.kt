package org.wycliffeassociates.otter.jvm.controls.skins

import com.github.thomasnield.rxkotlinfx.observeOnFx
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.SkinBase
import javafx.scene.control.Slider
import org.kordamp.ikonli.javafx.FontIcon
import org.wycliffeassociates.otter.common.device.AudioPlayerEvent
import org.wycliffeassociates.otter.jvm.controls.AudioPlayerNode
import java.util.concurrent.TimeUnit

private const val PLAY_ICON = "fa-play"
private const val PAUSE_ICON = "fa-pause"

class SourceAudioSkin(private val player: AudioPlayerNode) : SkinBase<AudioPlayerNode>(player) {

    @FXML
    lateinit var playBtn: Button
    @FXML
    lateinit var audioSlider: Slider

    var disposable: Disposable? = null
    var dragging = false
    var resumeAfterDrag = false

    init {
        loadFXML()
        initializeControl()
    }

    private fun initializeControl() {
        playBtn.setOnMouseClicked {
            toggle()
        }
        audioSlider.value = 0.0
        audioSlider.setOnDragDetected {
            if (player.isPlaying) {
                resumeAfterDrag = true
                toggle()
            }
            dragging = true
        }

        audioSlider.setOnMouseReleased {
            player.seek(audioSlider.value.toFloat() / 100F)
            if (dragging) {
                dragging = false
                if (resumeAfterDrag) {
                    toggle()
                    resumeAfterDrag = false
                }
            }
        }
    }

    private fun startProgressUpdate(): Disposable {
        return Observable
            .interval(20, TimeUnit.MILLISECONDS)
            .observeOnFx()
            .subscribe {
                if (player.isPlaying && !audioSlider.isValueChanging && !dragging) {
                    audioSlider.value = player.playbackPosition()
                }
            }
    }

    private fun toggle() {
        disposable?.dispose()
        if (player.isPlaying) {
            player.pause()
            playBtn.graphic = FontIcon(PLAY_ICON)
        } else {
            disposable = startProgressUpdate()
            player.play()
            player.player?.addEventListener {
                if (it == AudioPlayerEvent.STOP) {
                    disposable?.dispose()
                }
                Platform.runLater {
                    playBtn.graphic = FontIcon(PLAY_ICON)
                }
            }
            playBtn.graphic = FontIcon(PAUSE_ICON)
        }
    }

    private fun loadFXML() {
        val loader = FXMLLoader(javaClass.getResource("SourceAudioPlayer.fxml"))
        loader.setController(this)
        val root: Node = loader.load()
        children.add(root)
    }
}