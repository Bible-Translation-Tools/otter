package org.wycliffeassociates.otter.jvm.workbookapp.ui.takemanagement.viewmodel

import com.github.thomasnield.rxkotlinfx.observeOnFx
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import org.wycliffeassociates.otter.common.data.workbook.Chapter
import org.wycliffeassociates.otter.common.data.workbook.Chunk
import org.wycliffeassociates.otter.common.domain.resourcecontainer.SourceAudio
import org.wycliffeassociates.otter.jvm.workbookapp.ui.workbook.viewmodel.WorkbookViewModel
import org.wycliffeassociates.otter.jvm.utils.onChangeAndDoNow
import org.wycliffeassociates.resourcecontainer.ResourceContainer
import tornadofx.*

class RecordScriptureViewModel : ViewModel() {
    private enum class StepDirection {
        FORWARD,
        BACKWARD
    }

    private val workbookViewModel: WorkbookViewModel by inject()
    private val audioPluginViewModel: AudioPluginViewModel by inject()

    val recordableViewModel = RecordableViewModel(audioPluginViewModel)

    // This will be bidirectionally bound to workbookViewModel's activeChunkProperty
    private val activeChunkProperty = SimpleObjectProperty<Chunk>()
    private val activeChunk: Chunk
        get() = activeChunkProperty.value ?: throw IllegalStateException("Chunk is null")

    private val titleProperty = SimpleStringProperty()
    private var title by titleProperty

    private val chunkList: ObservableList<Chunk> = observableListOf()
    val hasNext = SimpleBooleanProperty(false)
    val hasPrevious = SimpleBooleanProperty(false)

    private var activeChunkSubscription: Disposable? = null

    private val sourceAudio: SourceAudio
    val sourceAudioPath = SimpleStringProperty()

    init {
        val rcPath = workbookViewModel.workbook.source.resourceMetadata.path
        sourceAudio = SourceAudio(ResourceContainer.load(rcPath))

        activeChunkProperty.bindBidirectional(workbookViewModel.activeChunkProperty)

        workbookViewModel.activeChapterProperty.onChangeAndDoNow { chapter ->
            chapter?.let { getChunkList(chapter.chunks) }
        }

        activeChunkProperty.onChangeAndDoNow { chunk ->
            if (chunk != null) {
                setTitle(chunk)
                setHasNextAndPrevious()
                // This will trigger loading takes in the RecordableViewModel
                recordableViewModel.recordable = chunk
                updateSourceAudio(workbookViewModel.activeChapterProperty.value, chunk)
            } else {
                workbookViewModel.activeChapterProperty.value?.let {
                    recordableViewModel.recordable = it
                }
                sourceAudioPath.set(null)
            }
        }
    }

    private fun updateSourceAudio(chapter: Chapter, chunk: Chunk) {
        println(chunk)
        sourceAudioPath.set(sourceAudio.get(workbookViewModel.workbook.source.slug, chapter.sort)?.absolutePath)
    }

    fun nextChunk() {
        stepToChunk(StepDirection.FORWARD)
    }

    fun previousChunk() {
        stepToChunk(StepDirection.BACKWARD)
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

    private fun setTitle(chunk: Chunk) {
        val label = messages["verse"]
        val start = chunk.start
        title = "$label $start"
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

    private fun stepToChunk(direction: StepDirection) {
        val amount = when (direction) {
            StepDirection.FORWARD -> 1
            StepDirection.BACKWARD -> -1
        }
        chunkList
            .find { it.start == activeChunk.start + amount }
            ?.let { newChunk -> activeChunkProperty.set(newChunk) }
    }
}