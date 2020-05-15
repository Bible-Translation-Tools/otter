package org.wycliffeassociates.otter.jvm.workbookapp.ui.resourcetakes.viewmodel

import com.github.thomasnield.rxkotlinfx.observeOnFx
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import org.wycliffeassociates.otter.common.data.model.ContentType
import org.wycliffeassociates.otter.common.domain.content.Recordable
import org.wycliffeassociates.otter.jvm.workbookapp.ui.workbook.viewmodel.WorkbookViewModel
import org.wycliffeassociates.otter.jvm.workbookapp.ui.takemanagement.viewmodel.AudioPluginViewModel
import org.wycliffeassociates.otter.jvm.utils.getNotNull
import java.util.EnumMap
import javafx.collections.ListChangeListener
import org.wycliffeassociates.otter.common.data.workbook.Chunk
import org.wycliffeassociates.otter.common.data.workbook.Resource
import org.wycliffeassociates.otter.jvm.utils.onChangeAndDoNow
import org.wycliffeassociates.otter.jvm.workbookapp.ui.resources.viewmodel.ResourceListViewModel
import org.wycliffeassociates.otter.jvm.workbookapp.ui.takemanagement.viewmodel.RecordScriptureViewModel
import org.wycliffeassociates.otter.jvm.workbookapp.ui.takemanagement.viewmodel.RecordableViewModel
import tornadofx.*

class RecordResourceViewModel : ViewModel() {
    private enum class StepDirection {
        FORWARD,
        BACKWARD
    }

    private val workbookViewModel: WorkbookViewModel by inject()
    private val resourceListViewModel: ResourceListViewModel by inject()
    private val audioPluginViewModel: AudioPluginViewModel by inject()

    val recordableViewModel = RecordableViewModel(audioPluginViewModel)

    // This will be bidirectionally bound to workbookViewModel's activeChunkProperty
    private val activeChunkProperty = SimpleObjectProperty<Chunk>()
    private val activeChunk: Chunk
        get() = activeChunkProperty.value ?: throw IllegalStateException("Chunk is null")

    private val chunkList: ObservableList<Chunk> = observableListOf()
    val hasNext = SimpleBooleanProperty(false)
    val hasPrevious = SimpleBooleanProperty(false)
    private var activeChunkSubscription: Disposable? = null

    internal val recordableList: ObservableList<Recordable> = FXCollections.observableArrayList()

    class ContentTypeToViewModelMap(map: Map<ContentType, RecordableTabViewModel>) :
        EnumMap<ContentType, RecordableTabViewModel>(map)

    val contentTypeToViewModelMap = ContentTypeToViewModelMap(
        hashMapOf(
            ContentType.TITLE to tabRecordableViewModel(),
            ContentType.BODY to tabRecordableViewModel()
        )
    )

    private fun tabRecordableViewModel() =
        RecordableTabViewModel(SimpleStringProperty(), audioPluginViewModel)

    init {
        activeChunkProperty.bindBidirectional(workbookViewModel.activeChunkProperty)

        initTabs()

        recordableList.onChange {
            updateRecordables(it)
        }

        workbookViewModel.activeResourceMetadataProperty.onChangeAndDoNow { metadata ->
            metadata?.let { setTabLabels(metadata.identifier) }
        }

        /*workbookViewModel.activeChapterProperty.onChangeAndDoNow { chapter ->
            chapter?.let {
                getChunkList(chapter.chunks)
                if (activeChunkProperty.value == null) {
                    recordableViewModel.recordable = it
                    setHasNextAndPrevious()
                }
            }
        }*/

        activeChunkProperty.onChangeAndDoNow { chunk ->
            if (chunk != null) {
                // setTitle(chunk) TODO !!!
                setHasNextAndPrevious()
                // This will trigger loading takes in the RecordableViewModel
                recordableViewModel.recordable = chunk
            }
        }
    }

    fun onTabSelect(recordable: Recordable) {
        workbookViewModel.activeResourceComponentProperty.set(recordable as Resource.Component)
    }

    fun setRecordableListItems(items: List<Recordable>) {
        if (!recordableList.containsAll(items))
            recordableList.setAll(items)
    }

    private fun initTabs() {
        recordableList.forEach {
            addRecordableToTabViewModel(it)
        }
    }

    private fun setTabLabels(resourceSlug: String?) {
        when (resourceSlug) {
            "tn" -> {
                setLabelProperty(ContentType.TITLE, messages["snippet"])
                setLabelProperty(ContentType.BODY, messages["note"])
            }
            "tq" -> {
                setLabelProperty(ContentType.TITLE, messages["question"])
                setLabelProperty(ContentType.BODY, messages["answer"])
            }
        }
    }

    private fun setLabelProperty(contentType: ContentType, label: String) {
        contentTypeToViewModelMap.getNotNull(contentType).labelProperty.set(label)
    }

    private fun updateRecordables(change: ListChangeListener.Change<out Recordable>) {
        while (change.next()) {
            change.removed.forEach { recordable ->
                removeRecordableFromTabViewModel(recordable)
            }
            change.addedSubList.forEach { recordable ->
                addRecordableToTabViewModel(recordable)
            }
        }
    }

    private fun addRecordableToTabViewModel(item: Recordable) {
        contentTypeToViewModelMap.getNotNull(item.contentType).recordable = item
    }

    private fun removeRecordableFromTabViewModel(item: Recordable) {
        contentTypeToViewModelMap.getNotNull(item.contentType).recordable = null
    }

    fun nextChunk() {
        stepToChunk(StepDirection.FORWARD)
    }

    fun previousChunk() {
        stepToChunk(StepDirection.BACKWARD)
    }

    private fun stepToChunk(direction: StepDirection) {
        val amount = when (direction) {
            StepDirection.FORWARD -> 1
            StepDirection.BACKWARD -> -1
        }
        chunkList
            .find { it.start == activeChunk.start + amount }
            ?.let { newChunk -> activeChunkProperty.set(newChunk) }
    }

    private fun setHasNextAndPrevious() {
        activeChunkProperty.value?.let { chunk ->
            if (chunkList.isNotEmpty()) {
                hasNext.set(chunk.start < chunkList.last().start)
                hasPrevious.set(chunk.start > chunkList.first().start)
            } else {
                hasNext.set(false)
                hasPrevious.set(false)
                chunkList.sizeProperty.onChangeOnce {
                    setHasNextAndPrevious()
                }
            }
        }
    }

    private fun getChunkList(chunks: Observable<Chunk>) {
        activeChunkSubscription?.dispose()
        activeChunkSubscription = chunks
            .toList()
            .map { it.sortedBy { chunk -> chunk.start } }
            .observeOnFx()
            .subscribe { list ->
                chunkList.setAll(list)
            }
    }
}