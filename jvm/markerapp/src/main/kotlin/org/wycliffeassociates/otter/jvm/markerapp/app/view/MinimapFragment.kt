package org.wycliffeassociates.otter.jvm.markerapp.app.view

import javafx.geometry.Pos
import javafx.scene.layout.Priority
import org.kordamp.ikonli.javafx.FontIcon
import tornadofx.*

class MinimapFragment : Fragment() {

    private val USER_AGENT_STYLESHEET = javaClass.getResource("/css/verse-marker-app.css").toExternalForm()

    init {
        FX.stylesheets.setAll(USER_AGENT_STYLESHEET)
    }

    val slider = slider { }

    override val root = hbox {
        alignment = Pos.CENTER_LEFT
        styleClass.add("vm-minimap-container")
        hbox {
            alignment = Pos.CENTER_LEFT
            spacing = 10.0
            button {
                graphic = FontIcon("gmi-bookmark")
                styleClass.add("vm-marker-count__icon")
            }
            add(label().apply {
            })
        }
        add(slider.apply {
            hgrow = Priority.ALWAYS
        })
    }
}