package org.wycliffeassociates.otter.jvm.controls.card

import javafx.beans.property.*
import javafx.scene.control.Control
import javafx.scene.control.Skin
import org.wycliffeassociates.otter.common.data.workbook.Take
import org.wycliffeassociates.otter.common.device.IAudioPlayer
import org.wycliffeassociates.otter.jvm.controls.skins.cards.ResourceTakeCardSkin

class ResourceTakeCard : Control() {

    private val takeProperty = SimpleObjectProperty<Take>()
    private val audioPlayerProperty = SimpleObjectProperty<IAudioPlayer>()
    private val takeNumberProperty = SimpleStringProperty("Take 01")
    private val isDraggingProperty = SimpleBooleanProperty(false)

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

    override fun createDefaultSkin(): Skin<*> {
        return ResourceTakeCardSkin(this)
    }
}
