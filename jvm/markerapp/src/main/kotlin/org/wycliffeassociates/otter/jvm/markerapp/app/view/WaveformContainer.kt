package org.wycliffeassociates.otter.jvm.markerapp.app.view

import com.sun.glass.ui.Screen
import javafx.animation.AnimationTimer
import javafx.geometry.Rectangle2D
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import org.wycliffeassociates.otter.jvm.controls.waveform.WaveformImageBuilder
import org.wycliffeassociates.otter.jvm.markerapp.app.view.layers.PlaceMarkerLayer
import org.wycliffeassociates.otter.jvm.markerapp.app.viewmodel.VerseMarkerViewModel
import tornadofx.*

class WaveformContainer : Fragment() {

    val verseMarkerViewModel: VerseMarkerViewModel by inject()
    var imageView = ImageView().apply { style { backgroundColor += Paint.valueOf("#0a337333") } }

    init {
        val width = Screen.getMainScreen().platformWidth
        val height = Screen.getMainScreen().platformHeight

        val imageWidth =
            (44100 * 10 / width.toDouble()) * (verseMarkerViewModel.audioPlayer.getAbsoluteDurationMs() / 1000.0)

        WaveformImageBuilder(
            paddingColor = Color.web("#0a337333"),
            wavColor = Color.web("#0A337360"),
            background = Color.web("#F7FAFF")
        ).build(
            verseMarkerViewModel.audioPlayer.getAudioReader()!!,
            fitToAudioMax = false,
            width = imageWidth.toInt() + width,
            height = height
        ).subscribe { image ->
            imageView.imageProperty().set(image)
        }
    }

    override val root = pane {
        hgrow = Priority.ALWAYS
        vgrow = Priority.ALWAYS

        imageView.fitHeightProperty().bind(this.heightProperty())
        imageView.fitWidthProperty().bind(this.widthProperty())


        val ht = Screen.getMainScreen().platformHeight
        val wd = Screen.getMainScreen().platformWidth

        val at = object : AnimationTimer() {
            override fun handle(currentNanoTime: Long) {
                if (imageView != null && imageView.image != null) {
                    val player = verseMarkerViewModel.audioPlayer
                    val padding = Screen.getMainScreen().platformWidth / 2
                    val width = imageView.image.width
                    val pos =
                        (player.getAbsoluteLocationInFrames() / player.getAbsoluteDurationInFrames().toDouble()) * width
                    imageView.viewport = Rectangle2D(pos - padding, 0.0, wd.toDouble(), ht.toDouble())
                }
            }
        }.start()

        stackpane {
            fitToParentWidth()
            fitToParentHeight()
            add(imageView)
            add(PlaceMarkerLayer())

            style {
                backgroundColor += Paint.valueOf("#0a337333")
            }
        }
    }
}
