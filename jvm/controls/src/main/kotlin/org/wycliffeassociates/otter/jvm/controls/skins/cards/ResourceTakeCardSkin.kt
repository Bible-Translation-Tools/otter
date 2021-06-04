package org.wycliffeassociates.otter.jvm.controls.skins.cards

import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.SnapshotParameters
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.Label
import javafx.scene.control.SkinBase
import javafx.scene.control.Slider
import javafx.scene.input.ClipboardContent
import javafx.scene.input.MouseEvent
import javafx.scene.input.TransferMode
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import org.kordamp.ikonli.javafx.FontIcon
import org.wycliffeassociates.otter.jvm.controls.card.EmptyCardCell
import org.wycliffeassociates.otter.jvm.controls.card.ResourceTakeCard
import org.wycliffeassociates.otter.jvm.controls.card.ScriptureTakeCard
import org.wycliffeassociates.otter.jvm.controls.card.events.DeleteTakeEvent
import org.wycliffeassociates.otter.jvm.controls.card.events.TakeEvent
import org.wycliffeassociates.otter.jvm.controls.controllers.AudioPlayerController
import org.wycliffeassociates.otter.jvm.utils.onChangeAndDoNow
import tornadofx.*

class ResourceTakeCardSkin(val card: ResourceTakeCard) : SkinBase<ResourceTakeCard>(card) {

    lateinit var cardNode: Node

    private val dragDropContainer = StackPane()
    private val back = EmptyCardCell()
    private val sliderActiveClass = "card--take__slider--active"

    @FXML
    lateinit var playBtn: Button

    @FXML
    lateinit var editBtn: Button

    @FXML
    lateinit var deleteBtn: Button

    @FXML
    lateinit var slider: Slider

    @FXML
    lateinit var takeLabel: Label

    @FXML
    lateinit var takeDrag: Label

    lateinit var audioPlayerController: AudioPlayerController

    private val playIcon = FontIcon("fa-play")
    private val pauseIcon = FontIcon("fa-pause")

    init {
        loadFXML()
        initializeControl()
    }

    fun initializeControl() {
        bindText()
        initController()

        back.apply {
            addClass("card--resource-take--empty")
            widthProperty().bind(skinnable.widthProperty())
            heightProperty().bind(skinnable.heightProperty())
        }
    }

    fun bindText() {
        takeLabel.textProperty().bind(card.takeNumberProperty())
    }

    fun initController() {
        audioPlayerController = AudioPlayerController(slider)
        audioPlayerController.isPlayingProperty.onChangeAndDoNow { isPlaying ->
            if (isPlaying != null && isPlaying != true) {
                playBtn.graphicProperty().set(playIcon)
            } else {
                playBtn.graphicProperty().set(pauseIcon)
            }
        }
        playBtn.setOnAction {
            audioPlayerController.toggle()
        }
        deleteBtn.setOnAction {
            error(
                FX.messages["deleteTakePrompt"],
                FX.messages["cannotBeUndone"],
                ButtonType.YES,
                ButtonType.NO,
                title = FX.messages["deleteTakePrompt"]
            ) { button: ButtonType ->
                if (button == ButtonType.YES) {
                    skinnable.fireEvent(
                        DeleteTakeEvent(card.takeProperty().value)
                    )
                }
            }
        }
        editBtn.setOnAction {
            skinnable.fireEvent(
                TakeEvent(
                    card.takeProperty().value,
                    {
                        card.audioPlayerProperty().value.load(card.takeProperty().value.file)
                    },
                    TakeEvent.EDIT_TAKE
                )
            )
        }
        slider.valueProperty().onChange {
            when {
                it > 0.0 -> {
                    if (!slider.styleClass.contains(sliderActiveClass)) {
                        slider.styleClass.add(sliderActiveClass)
                    }
                }
                else -> slider.styleClass.remove(sliderActiveClass)
            }
        }
        card.apply {
            vgrow = Priority.ALWAYS
            audioPlayerProperty().onChangeAndDoNow { player ->
                player?.let {
                    audioPlayerController.load(it)
                }
            }
        }
        cardNode.apply {
            setOnDragDetected {
                startDrag(it)
                card.isDraggingProperty().value = true
                it.consume()
            }
            setOnDragDone {
                card.isDraggingProperty().value = false
                it.consume()
            }
            setOnMouseReleased {
                card.isDraggingProperty().value = false
                it.consume()
            }
            hiddenWhen(card.isDraggingProperty())
        }
    }

    private fun startDrag(evt: MouseEvent) {
        val db = cardNode.startDragAndDrop(*TransferMode.ANY)
        val content = ClipboardContent()
        content.putString(card.takeProperty().value.name)
        db.setContent(content)
        val sp = SnapshotParameters()
        sp.fill = Color.TRANSPARENT
        db.dragView = skinnable.snapshot(sp, null)
        evt.consume()
    }

    private fun loadFXML() {
        val loader = FXMLLoader(javaClass.getResource("ResourceTakeCard.fxml"))
        loader.setController(this)
        cardNode = loader.load()

        dragDropContainer.add(back)
        dragDropContainer.add(cardNode)
        children.addAll(dragDropContainer)
    }
}
