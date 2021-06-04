package org.wycliffeassociates.otter.jvm.workbookapp.ui.screens

import com.jfoenix.controls.JFXSnackbar
import com.jfoenix.controls.JFXSnackbarLayout
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Control
import javafx.scene.input.DragEvent
import javafx.scene.input.Dragboard
import javafx.scene.input.TransferMode
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.util.Duration
import org.kordamp.ikonli.javafx.FontIcon
import org.kordamp.ikonli.materialdesign.MaterialDesign
import org.slf4j.LoggerFactory
import org.wycliffeassociates.otter.common.persistence.repositories.PluginType
import org.wycliffeassociates.otter.jvm.controls.breadcrumbs.BreadCrumb
import org.wycliffeassociates.otter.jvm.controls.card.ScriptureTakeCard
import org.wycliffeassociates.otter.jvm.controls.card.events.DeleteTakeEvent
import org.wycliffeassociates.otter.jvm.controls.card.events.TakeEvent
import org.wycliffeassociates.otter.jvm.controls.dialog.PluginOpenedPage
import org.wycliffeassociates.otter.jvm.controls.dragtarget.DragTargetBuilder
import org.wycliffeassociates.otter.jvm.controls.media.SourceContent
import org.wycliffeassociates.otter.jvm.utils.onChangeAndDoNow
import org.wycliffeassociates.otter.jvm.workbookapp.SnackbarHandler
import org.wycliffeassociates.otter.jvm.workbookapp.plugin.PluginClosedEvent
import org.wycliffeassociates.otter.jvm.workbookapp.plugin.PluginOpenedEvent
import org.wycliffeassociates.otter.jvm.workbookapp.ui.NavigationMediator
import org.wycliffeassociates.otter.jvm.workbookapp.ui.model.TakeCardModel
import org.wycliffeassociates.otter.jvm.workbookapp.ui.viewmodel.AudioPluginViewModel
import org.wycliffeassociates.otter.jvm.workbookapp.ui.viewmodel.RecordScriptureViewModel
import org.wycliffeassociates.otter.jvm.workbookapp.ui.viewmodel.WorkbookDataStore
import tornadofx.*

class RecordScriptureFragment : Fragment() {
    private val logger = LoggerFactory.getLogger(RecordScriptureFragment::class.java)

    private val recordScriptureViewModel: RecordScriptureViewModel by inject()
    private val workbookDataStore: WorkbookDataStore by inject()
    private val navigator: NavigationMediator by inject()
    private val audioPluginViewModel: AudioPluginViewModel by inject()
    private val recordableViewModel = recordScriptureViewModel.recordableViewModel

    private val mainContainer = VBox()
    private val takesGrid = ScriptureTakesGridView(
        workbookDataStore.activeChunkProperty.isNull,
        recordableViewModel::recordNewTake
    )
    private val pluginOpenedPage: PluginOpenedPage

    private val isDraggingProperty = SimpleBooleanProperty(false)
    private val draggingNodeProperty = SimpleObjectProperty<Node>()

    private val dragTarget =
        DragTargetBuilder(DragTargetBuilder.Type.SCRIPTURE_TAKE)
            .build(isDraggingProperty.toBinding())
            .apply {
                addClass("card--scripture-take--empty")
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

    private val sourceContent =
        SourceContent().apply {
            sourceTextProperty.bind(workbookDataStore.sourceTextBinding())
            audioPlayerProperty.bind(recordableViewModel.sourceAudioPlayerProperty)

            audioNotAvailableTextProperty.set(messages["audioNotAvailable"])
            textNotAvailableTextProperty.set(messages["textNotAvailable"])
            playLabelProperty.set(messages["playSource"])
            pauseLabelProperty.set(messages["pauseSource"])

            contentTitleProperty.bind(workbookDataStore.activeChunkTitleBinding())
        }

    private val breadCrumb = BreadCrumb().apply {
        titleProperty.bind(recordScriptureViewModel.breadcrumbTitleBinding)
        iconProperty.set(FontIcon(MaterialDesign.MDI_LINK_OFF))
        onClickAction {
            navigator.dock(this@RecordScriptureFragment)
        }
    }

    override val root = anchorpane {
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

    init {
        importStylesheet(resources.get("/css/record-scripture.css"))
        importStylesheet(resources.get("/css/audioplayer.css"))
        importStylesheet(resources.get("/css/takecard.css"))
        importStylesheet(resources.get("/css/scripturetakecard.css"))

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

        recordableViewModel.takeCardModels.onChangeAndDoNow {
            takesGrid.gridItems.setAll(it)
        }

        recordableViewModel.selectedTakeProperty.onChangeAndDoNow {
            if (it != null) {
                dragTarget.selectedNodeProperty.set(createTakeCard(it))
            }
        }

        dragTarget.setOnDragDropped {
            val db: Dragboard = it.dragboard
            var success = false
            if (db.hasString()) {
                recordableViewModel.selectTake(db.string)
                success = true
            }
            (it.source as? ScriptureTakeCard)?.let {
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

        mainContainer.apply {
            addEventHandler(DragEvent.DRAG_ENTERED) { isDraggingProperty.value = true }
            addEventHandler(DragEvent.DRAG_EXITED) { isDraggingProperty.value = false }

            hgrow = Priority.ALWAYS

            // Top items above the alternate takes
            // Drag target and/or selected take, Next Verse Button, Previous Verse Button
            hbox {
                addClass("record-scripture__top")

                // previous verse button
                button(messages["previousVerse"]) {
                    addClass("btn", "btn--secondary")
                    graphic = FontIcon(MaterialDesign.MDI_ARROW_LEFT)
                    action {
                        recordScriptureViewModel.previousChunk()
                    }
                    enableWhen(recordScriptureViewModel.hasPrevious)
                }

                vbox {
                    add(dragTarget)
                }

                // next verse button
                button(messages["nextVerse"]) {
                    addClass("btn", "btn--secondary")
                    graphic = FontIcon(MaterialDesign.MDI_ARROW_RIGHT)
                    action {
                        recordScriptureViewModel.nextChunk()
                    }
                    enableWhen(recordScriptureViewModel.hasNext)
                }
            }

            add(takesGrid)
            add(sourceContent)
        }
    }

    private fun Parent.addButtonEventHandlers() {
        addEventHandler(DeleteTakeEvent.DELETE_TAKE) {
            recordableViewModel.deleteTake(it.take)
        }
        addEventHandler(TakeEvent.EDIT_TAKE) {
            recordableViewModel.processTakeWithPlugin(it, PluginType.EDITOR)
        }
        addEventHandler(TakeEvent.MARK_TAKE) {
            recordableViewModel.processTakeWithPlugin(it, PluginType.MARKER)
        }
    }

    private fun createTakeCard(take: TakeCardModel): Control {
        return ScriptureTakeCard().apply {
            audioPlayerProperty().set(take.audioPlayer)
            takeProperty().set(take.take)
            takeNumberProperty().set(take.take.number.toString())
            allowMarkerProperty().bind(recordableViewModel.workbookDataStore.activeChunkProperty.isNull)
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

    override fun onUndock() {
        super.onUndock()
        recordableViewModel.closePlayers()
    }

    override fun onDock() {
        super.onDock()
        recordableViewModel.openPlayers()
        recordScriptureViewModel.recordableViewModel.currentTakeNumberProperty.set(null)
        navigator.dock(this, breadCrumb)
    }
}
