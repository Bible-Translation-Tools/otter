package org.wycliffeassociates.otter.jvm.workbookapp.ui.screens

import com.github.thomasnield.rxkotlinfx.observeOnFx
import io.reactivex.schedulers.Schedulers
import javafx.geometry.Pos
import javafx.scene.control.TextField
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javax.inject.Inject
import javax.inject.Provider
import org.kordamp.ikonli.javafx.FontIcon
import org.wycliffeassociates.otter.common.audio.wav.WavFile
import org.wycliffeassociates.otter.common.domain.plugins.LaunchPlugin
import org.wycliffeassociates.otter.common.domain.plugins.PluginParameters
import org.wycliffeassociates.otter.common.persistence.repositories.PluginType
import org.wycliffeassociates.otter.jvm.workbookapp.plugin.PluginClosedEvent
import org.wycliffeassociates.otter.jvm.workbookapp.plugin.PluginOpenedEvent
import org.wycliffeassociates.otter.jvm.workbookapp.ui.OtterApp
import org.wycliffeassociates.otter.jvm.workbookapp.ui.viewmodel.WorkbookDataStore
import tornadofx.*

class NotChunkedPage : Fragment() {

    val workbookDataStore: WorkbookDataStore by inject()

    @Inject
    lateinit var launchPluginProvider: Provider<LaunchPlugin>
    lateinit var input: TextField

    override val root = vbox {
        alignment = Pos.CENTER
        maxHeight = 571.0
        maxWidth = 800.0
        spacing = 16.0
        add(
            resources.imageview("/images/notchunked.png").apply {
                prefHeight = 240.0
                prefWidth = 240.0
            }
        )
        text("Chunking Not Complete").apply {
            style {
                fontSize = 36.px
            }
        }
        text("Please chunk chapter # to continue. You may skip the chunking step if you prefer translating verse by verse.").apply {
            wrappingWidth = 500.0
            style {
                fontSize = 16.px
            }
        }
        button(
            "Chunk Chapter",
            graphic = FontIcon("gmi-arrow-forward").apply {
                iconSize = 20
                iconColor = Color.WHITE
            }
        ) {
            action {
                (app as OtterApp).dependencyGraph.inject(this@NotChunkedPage)
                launchPlugin()
            }
            style {
                underline = true
                fontSize = 20.px
                textFill = Color.WHITE
                backgroundColor += Paint.valueOf("#015AD9")
            }
        }
        button(
            "Skip",
            graphic = FontIcon("gmi-bookmark-outline").apply {
                iconSize = 20
                iconColor = Paint.valueOf("#015AD9")
            }
        ) {
            action {
                val chapter = workbookDataStore.activeChapterProperty.value
                chapter.chunks
                    .subscribeOn(Schedulers.computation())
                    .observeOnFx()
                    .subscribe({}, {},
                        {
                            chapter.chunked = true
                            workspace.dockedComponent!!.replaceWith<ChapterPage>()
                        }
                    )
            }
            style {
                underline = true
                fontSize = 20.px
                backgroundColor += Color.WHITE
                textFill = Paint.valueOf("#015AD9")
            }
        }
        style {
            borderRadius += box(16.px)
            backgroundRadius += box(16.px)
            backgroundColor += Color.WHITE
        }
    }


//    hbox {
//        alignment = Pos.CENTER
//        input = textfield {
//            prefWidth = 50.0
//            prefHeight = 50.0
//        }
//        button("Chunk") {
//            prefWidth = 300.0

//        }
//        button("Skip") {
//            prefWidth = 300.0
//
//        }
//    }

    fun launchPlugin() {
        val workbook = workbookDataStore.activeWorkbookProperty.value
        val chapter = workbookDataStore.activeChapterProperty.value
        val sourceAudio = workbook.sourceAudioAccessor.getChapter(chapter.sort)
        val file = sourceAudio!!.file
        val wav = WavFile(file)
        (wav.metadata.getCues() as MutableList).clear()
        wav.update()
        val params = PluginParameters(
            languageName = "en",
            bookTitle = "jas",
            chapterLabel = chapter.sort.toString(),
            chapterNumber = chapter.sort,
            sourceChapterAudio = file,
            verseTotal = 30
        )
        fire(PluginOpenedEvent(PluginType.MARKER, true))
        launchPluginProvider
            .get()
            .launchPlugin(PluginType.MARKER, file, params)
            .subscribe { _ ->
                val wav = WavFile(sourceAudio!!.file)
                wav.update()
                val chunks = wav.metadata.getCues().size
                commitChunks(chunks)
            }
    }

    fun commitChunks(chunkCount: Int) {
        val chapter = workbookDataStore.activeChapterProperty.value
        val max = chunkCount
        val list = arrayListOf<Int>()
        for (i in 1..max) {
            list.add(i)
        }
        chapter.userChunkList!!.accept(list)
        chapter.chunks
            .subscribeOn(Schedulers.computation())
            .observeOnFx()
            .subscribe(
                {}, {},
                {
                    fire(PluginClosedEvent(PluginType.MARKER))
                    workbookDataStore.activeChapterProperty.value.chunked = true
                    workspace.dockedComponent!!.replaceWith<ChapterPage>()
                }
            )
    }
}