package org.wycliffeassociates.otter.jvm.controls.card

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.scene.control.Control
import javafx.scene.control.Label
import javafx.scene.control.Skin
import javafx.scene.layout.Priority
import javafx.scene.text.Text
import org.kordamp.ikonli.javafx.FontIcon
import org.wycliffeassociates.otter.jvm.controls.skins.ProjectCardSkin
import tornadofx.*

class ProjectCard(
    private val title: String = "",
    private val slug: String = "",
    private val language: String = "",
    private val actionText: String = "",
    private val secondaryActions: List<Action>? = null
) : Control() {

    private val onPrimaryAction = SimpleObjectProperty<() -> Unit>()
    private val titleTextProperty = SimpleStringProperty(title)
    private val slugTextProperty = SimpleStringProperty(slug)
    private val languageTextProperty = SimpleStringProperty(language)
    private val actionTextProperty = SimpleStringProperty(actionText)
    val secondaryActionsList: ObservableList<Action> = FXCollections.observableArrayList<Action>()

    init {
        if (secondaryActions != null) {
            addActions(*secondaryActions.toTypedArray())
        }
    }

    fun titleTextProperty(): StringProperty {
        return titleTextProperty
    }

    fun slugTextProperty(): StringProperty {
        return slugTextProperty
    }

    fun languageTextProperty(): StringProperty {
        return languageTextProperty
    }

    fun actionTextProperty(): StringProperty {
        return actionTextProperty
    }

    fun onPrimaryActionProperty() = onPrimaryAction

    fun setOnAction(op: () -> Unit) {
        onPrimaryAction.set(op)
    }

    fun addActions(vararg actions: Action) {
        secondaryActionsList.addAll(actions)
    }

    override fun createDefaultSkin(): Skin<*> {
        return ProjectCardSkin(this)
    }
}

fun EventTarget.projectcard(
    title: String = "",
    slug: String = "",
    language: String = "",
    actionText: String = "",
    moreActions: List<Action>? = null,
    op: ProjectCard.() -> Unit = {}
) = ProjectCard(title, slug, language, actionText, moreActions).attachTo(this, op)

class Action(val text: String, val iconCode: String, val onClicked: () -> Unit)