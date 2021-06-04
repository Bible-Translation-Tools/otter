package org.wycliffeassociates.otter.jvm.workbookapp.ui.screens

import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXSnackbar
import com.jfoenix.controls.JFXSnackbarLayout
import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.MaterialIconView
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Control
import javafx.scene.effect.DropShadow
import javafx.scene.input.DragEvent
import javafx.scene.input.Dragboard
import javafx.scene.input.TransferMode
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.util.Duration
import org.slf4j.LoggerFactory
import org.wycliffeassociates.otter.common.persistence.repositories.PluginType
import org.wycliffeassociates.otter.jvm.controls.button.highlightablebutton
import org.wycliffeassociates.otter.jvm.controls.card.ResourceTakeCard
import org.wycliffeassociates.otter.jvm.controls.card.events.DeleteTakeEvent
import org.wycliffeassociates.otter.jvm.controls.card.events.TakeEvent
import org.wycliffeassociates.otter.jvm.controls.dialog.PluginOpenedPage
import org.wycliffeassociates.otter.jvm.controls.dragtarget.DragTargetBuilder
import org.wycliffeassociates.otter.jvm.utils.onChangeAndDoNow
import org.wycliffeassociates.otter.jvm.workbookapp.SnackbarHandler
import org.wycliffeassociates.otter.jvm.workbookapp.plugin.PluginClosedEvent
import org.wycliffeassociates.otter.jvm.workbookapp.plugin.PluginOpenedEvent
import org.wycliffeassociates.otter.jvm.workbookapp.theme.AppTheme
import org.wycliffeassociates.otter.jvm.workbookapp.ui.model.TakeCardModel
import org.wycliffeassociates.otter.jvm.workbookapp.ui.styles.RecordResourceStyles
import org.wycliffeassociates.otter.jvm.workbookapp.ui.viewmodel.AudioPluginViewModel
import org.wycliffeassociates.otter.jvm.workbookapp.ui.viewmodel.RecordResourceViewModel
import org.wycliffeassociates.otter.jvm.workbookapp.ui.viewmodel.RecordableViewModel
import org.wycliffeassociates.otter.jvm.workbookapp.ui.viewmodel.WorkbookDataStore
import tornadofx.*

class RecordResourceFragment(private val recordableViewModel: RecordableViewModel) : Fragment() {
    private val logger = LoggerFactory.getLogger(RecordResourceFragment::class.java)

    private val recordResourceViewModel: RecordResourceViewModel by inject()
    private val audioPluginViewModel: AudioPluginViewModel by inject()
    private val workbookDataStore: WorkbookDataStore by inject()

    val formattedTextProperty = SimpleStringProperty()
    private val isDraggingProperty = SimpleBooleanProperty(false)
    private val draggingNodeProperty = SimpleObjectProperty<Node>()

    val dragTarget =
        DragTargetBuilder(DragTargetBuilder.Type.RESOURCE_TAKE)
            .build(isDraggingProperty.toBinding())
            .apply {
                addClass("card--resource-take--empty")
                recordableViewModel.selectedTakeProperty.onChangeAndDoNow { take ->
                    /* We can't just add the node being dragged, since the selected take might have just been
                        loaded from the database */
                    this.selectedNodeProperty.value = take?.let { createTakeCard(take) }
                }
            }

    private val dragContainer = VBox().apply {
        this.prefWidthProperty().bind(dragTarget.widthProperty())
        draggingNodeProperty.onChange { draggingNode ->
            clear()
            draggingNode?.let { add(draggingNode) }
        }
    }

    private val pluginOpenedPage: PluginOpenedPage

    private val alternateTakesList = TakesListView(
        items = recordableViewModel.takeCardModels,
        createTakeNode = ::createTakeCard
    )

    private val mainContainer = VBox()

    override val root: Parent = anchorpane {
        addButtonEventHandlers()

        createSnackBar()

        add(mainContainer
            .apply {
                anchorpaneConstraints {
                    leftAnchor = 0.0
                    rightAnchor = 0.0
                    bottomAnchor = 0.0
                    topAnchor = 0.0
                }
            }
        )
        add(dragContainer)
    }

    private val newTakeButton =
        highlightablebutton {
            highlightColor = AppTheme.colors.appBlue
            secondaryColor = AppTheme.colors.white
            isHighlighted = true
            graphic = MaterialIconView(MaterialIcon.MIC, "25px")
            maxWidth = 500.0
            text = messages["record"]

            action {
                recordableViewModel.recordNewTake()
            }
        }

    private val previousButton = JFXButton().apply {
        addClass(RecordResourceStyles.bottomButton)
        text = messages["previousChunk"]
        graphic = MaterialIconView(MaterialIcon.ARROW_BACK, "26px")
        action {
            recordResourceViewModel.previousChunk()
        }
        enableWhen(recordResourceViewModel.hasPrevious)
    }

    private val nextButton = JFXButton().apply {
        addClass(RecordResourceStyles.bottomButton)
        text = messages["nextChunk"]
        graphic = MaterialIconView(MaterialIcon.ARROW_FORWARD, "26px")
        action {
            recordResourceViewModel.nextChunk()
        }
        enableWhen(recordResourceViewModel.hasNext)
    }

    private val leftRegion = VBox().apply {
        vgrow = Priority.ALWAYS

        hbox {
            region {
                hgrow = Priority.ALWAYS
            }
            add(
                dragTarget.apply {
                    hgrow = Priority.ALWAYS
                }
            )
            region {
                hgrow = Priority.ALWAYS
            }
        }

        scrollpane {
            addClass(RecordResourceStyles.contentScrollPane)
            isFitToWidth = true
            vgrow = Priority.ALWAYS
            label(formattedTextProperty) {
                isWrapText = true
                addClass(RecordResourceStyles.contentText)
            }
        }

        vbox {
            addClass(RecordResourceStyles.newTakeRegion)
            add(newTakeButton).apply {
                effect = DropShadow(2.0, 0.0, 3.0, Color.valueOf("#0d4082"))
            }
        }
    }

    private val grid = gridpane {
        vgrow = Priority.ALWAYS
        addClass(RecordResourceStyles.takesTab)

        constraintsForRow(0).percentHeight = 90.0
        constraintsForRow(1).percentHeight = 10.0

        row {
            vbox {
                gridpaneColumnConstraints {
                    percentWidth = 50.0
                }
                addClass(RecordResourceStyles.leftRegionContainer)
                add(leftRegion)
            }
            vbox(20.0) {
                gridpaneColumnConstraints {
                    percentWidth = 50.0
                }
                addClass(RecordResourceStyles.rightRegion)
                add(alternateTakesList)
            }
        }

        row {
            vbox {
                alignment = Pos.CENTER
                gridpaneColumnConstraints {
                    percentWidth = 50.0
                }
                add(previousButton)
            }

            vbox {
                alignment = Pos.CENTER
                gridpaneColumnConstraints {
                    percentWidth = 50.0
                }
                add(nextButton)
            }
        }
    }

    init {
        importStylesheet<RecordResourceStyles>()
        importStylesheet(resources.get("/css/takecard.css"))
        importStylesheet(resources.get("/css/resourcetakecard.css"))

        isDraggingProperty.onChange {
            if (it) recordableViewModel.stopPlayers()
        }

        pluginOpenedPage = createPluginOpenedPage()
        workspace.subscribe<PluginOpenedEvent> { pluginInfo ->
            if (!pluginInfo.isNative) {
                workspace.dock(pluginOpenedPage)
            }
        }
        workspace.subscribe<PluginClosedEvent> {
            (workspace.dockedComponentProperty.value as? PluginOpenedPage)?.let {
                workspace.navigateBack()
            }
            recordableViewModel.openPlayers()
        }

        mainContainer.apply {
            addEventHandler(DragEvent.DRAG_ENTERED) { isDraggingProperty.value = true }
            addEventHandler(DragEvent.DRAG_EXITED) { isDraggingProperty.value = false }

            add(grid)
        }

        dragTarget.setOnDragDropped {
            val db: Dragboard = it.dragboard
            var success = false
            if (db.hasString()) {
                recordableViewModel.selectTake(db.string)
                success = true
            }
            (it.source as? ResourceTakeCard)?.let {
                it.isDraggingProperty().value = false
            }
            it.isDropCompleted = success
            it.consume()
        }

        dragTarget.setOnDragOver {
            if (it.gestureSource != dragTarget && it.dragboard.hasString()) {
                it.acceptTransferModes(*TransferMode.ANY)
            }
            it.consume()
        }
    }

    private fun createTakeCard(take: TakeCardModel): Control {
        return ResourceTakeCard().apply {
            audioPlayerProperty().set(take.audioPlayer)
            takeProperty().set(take.take)
            takeNumberProperty().set(take.take.number.toString())
        }
    }

    private fun createPluginOpenedPage(): PluginOpenedPage {
        // Plugin active cover
        return PluginOpenedPage().apply {
            dialogTitleProperty.bind(recordableViewModel.dialogTitleBinding())
            dialogTextProperty.bind(recordableViewModel.dialogTextBinding())
            playerProperty.bind(recordableViewModel.sourceAudioPlayerProperty)
            audioAvailableProperty.bind(recordableViewModel.sourceAudioAvailableProperty)
            sourceTextProperty.bind(workbookDataStore.sourceTextBinding())
            sourceContentTitleProperty.bind(workbookDataStore.activeChunkTitleBinding())
        }
    }

    private fun createSnackBar() {
        recordableViewModel
            .snackBarObservable
            .doOnError { e ->
                logger.error("Error in creating no plugin snackbar", e)
            }
            .subscribe { pluginErrorMessage ->
                SnackbarHandler.enqueue(
                    JFXSnackbar.SnackbarEvent(
                        JFXSnackbarLayout(
                            pluginErrorMessage,
                            messages["addPlugin"].toUpperCase()
                        ) {
                            audioPluginViewModel.addPlugin(true, false)
                        },
                        Duration.millis(5000.0),
                        null
                    )
                )
            }
    }

    private fun Parent.addButtonEventHandlers() {
        addEventHandler(DeleteTakeEvent.DELETE_TAKE) {
            recordableViewModel.deleteTake(it.take)
        }
        addEventHandler(TakeEvent.EDIT_TAKE) {
            recordableViewModel.processTakeWithPlugin(it, PluginType.EDITOR)
        }
    }
}
