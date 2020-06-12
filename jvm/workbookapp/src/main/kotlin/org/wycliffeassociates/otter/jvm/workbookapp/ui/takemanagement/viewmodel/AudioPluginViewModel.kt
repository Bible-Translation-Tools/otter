package org.wycliffeassociates.otter.jvm.workbookapp.ui.takemanagement.viewmodel

import io.reactivex.Single
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import org.wycliffeassociates.otter.common.data.PluginParameters
import org.wycliffeassociates.otter.common.data.config.AudioPluginData
import org.wycliffeassociates.otter.common.data.workbook.Take
import org.wycliffeassociates.otter.common.device.IAudioPlayer
import org.wycliffeassociates.otter.common.domain.content.*
import org.wycliffeassociates.otter.common.domain.plugins.LaunchPlugin
import org.wycliffeassociates.otter.jvm.workbookapp.ui.addplugin.view.AddPluginView
import org.wycliffeassociates.otter.jvm.workbookapp.ui.addplugin.viewmodel.AddPluginViewModel
import org.wycliffeassociates.otter.jvm.workbookapp.ui.inject.Injector
import org.wycliffeassociates.otter.jvm.workbookapp.ui.workbook.viewmodel.WorkbookViewModel
import org.wycliffeassociates.otter.jvm.workbookapp.io.wav.WaveFileCreator
import tornadofx.*

class AudioPluginViewModel : ViewModel() {
    private val injector: Injector by inject()
    private val pluginRepository = injector.pluginRepository

    private val workbookViewModel: WorkbookViewModel by inject()

    private val launchPlugin = LaunchPlugin(pluginRepository)
    private val recordTake = RecordTake(WaveFileCreator(), launchPlugin)
    private val editTake = EditTake(launchPlugin)

    fun getRecorder() = pluginRepository.getRecorder()
    fun getEditor() = pluginRepository.getEditor()

    val pluginNameProperty = SimpleStringProperty()
    val selectedRecorderProperty = SimpleObjectProperty<AudioPluginData>()
    val selectedEditorProperty = SimpleObjectProperty<AudioPluginData>()

    fun record(recordable: Recordable): Single<RecordTake.Result> {
        val params = constructPluginParameters()
        return recordTake.record(
            audio = recordable.audio,
            projectAudioDir = workbookViewModel.activeProjectAudioDirectory,
            namer = createFileNamer(recordable),
            pluginParameters = params
        )
    }

    private fun constructPluginParameters(): PluginParameters {
        val workbook = workbookViewModel.workbook
        val sourceAudio = workbook.sourceAudioAccessor

        val sourceAudioFile = workbookViewModel.chunk?.let { chunk ->
            sourceAudio.getChunk(
                workbookViewModel.activeChapterProperty.value.sort,
                chunk.start
            )
        } ?: run { sourceAudio.getChapter(workbookViewModel.activeChapterProperty.value.sort) }

        val chapterLabel = messages[workbookViewModel.activeChapterProperty.value.label]
        val chapterNumber = workbookViewModel.activeChapterProperty.value.sort
        val chunkLabel = workbookViewModel.activeChunkProperty.value?.let {
            messages[workbookViewModel.activeChunkProperty.value.label]
        }
        val chunkNumber = workbookViewModel.activeChunkProperty.value?.sort
        val resourceLabel = workbookViewModel.activeResourceComponentProperty.value?.let {
            messages[workbookViewModel.activeResourceComponentProperty.value.label]
        }
        val sourceText = "Hjef hsef hsej fhksejh fksehf kjsehkfjshef kjfse fhskejh fksjeh kfsjehk fhsekf hskehf sehfskje  sijefse jsfelfjs elkjf lskej lfskje lfksjel kfjsle kjflske jlfksjel kvslek vjlsje lksjel kvjlskej lvksjel kvjsle kjvlskej lvskje lvksjel kvjslek jvlskej lvksjel kvjlske jvlskej lkvsjel kvjslkej vlskjel vkjslekv jslke jvlskje vlksjel vjslekv"

        return PluginParameters(
            languageName = workbook.target.language.name,
            bookTitle = workbook.target.title,
            chapterLabel = chapterLabel,
            chapterNumber = chapterNumber,
            chunkLabel = chunkLabel,
            chunkNumber = chunkNumber,
            resourceLabel = resourceLabel,
            sourceChapterAudio = sourceAudioFile?.file,
            sourceChunkStart = sourceAudioFile?.start,
            sourceChunkEnd = sourceAudioFile?.end,
            sourceText = sourceText
        )
    }

    private fun createFileNamer(recordable: Recordable): FileNamer {
        return WorkbookFileNamerBuilder.createFileNamer(
            workbook = workbookViewModel.workbook,
            chapter = workbookViewModel.chapter,
            chunk = workbookViewModel.chunk,
            recordable = recordable,
            rcSlug = workbookViewModel.activeResourceMetadata.identifier
        )
    }

    fun edit(take: Take): Single<EditTake.Result> {
        val params = constructPluginParameters()
        return editTake.edit(take, params)
    }

    fun audioPlayer(): IAudioPlayer = injector.audioPlayer

    fun addPlugin(record: Boolean, edit: Boolean) {
        find<AddPluginViewModel>().apply {
            canRecord = record
            canEdit = edit
        }
        find<AddPluginView>().openModal()
    }
}