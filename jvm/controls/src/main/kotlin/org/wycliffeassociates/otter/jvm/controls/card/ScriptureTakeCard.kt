package org.wycliffeassociates.otter.jvm.controls.card

import javafx.beans.property.*
import javafx.scene.control.Control
import javafx.scene.control.Skin
import org.wycliffeassociates.otter.common.data.workbook.Take
import org.wycliffeassociates.otter.common.device.IAudioPlayer
import org.wycliffeassociates.otter.jvm.controls.skins.cards.ScriptureTakeCardSkin
import tornadofx.*

class ScriptureTakeCard : Control() {

    private val takeProperty = SimpleObjectProperty<Take>()
    private val audioPlayerProperty = SimpleObjectProperty<IAudioPlayer>()
    private val takeNumberProperty = SimpleStringProperty("Take 01")
    private val isDraggingProperty = SimpleBooleanProperty(false)
    private val allowMarkerProperty = SimpleBooleanProperty(true)

    fun takeProperty(): ObjectProperty<Take> {
        return takeProperty
    }

    fun audioPlayerProperty(): ObjectProperty<IAudioPlayer> {
        return audioPlayerProperty
    }

    fun takeNumberProperty(): StringProperty {
        return takeNumberProperty
    }

    fun isDraggingProperty(): BooleanProperty {
        return isDraggingProperty
    }

    fun allowMarkerProperty(): BooleanProperty {
        return allowMarkerProperty
    }

    override fun createDefaultSkin(): Skin<*> {
        return ScriptureTakeCardSkin(this)
    }
}
