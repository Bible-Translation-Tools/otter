package org.wycliffeassociates.otter.jvm.controls.card

import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import org.kordamp.ikonli.javafx.FontIcon
import org.kordamp.ikonli.materialdesign.MaterialDesign

class NewRecordingCard(
    val labelText: String,
    val buttonText: String,
    val action: () -> Unit
) : VBox() {
    init {
        with(this) {
            styleClass.addAll("card--scripture-take", "card--take--new")

            children.addAll(
                Label(labelText).apply {
                    styleClass.add("card--take--new-label")
                },
                Button(buttonText).apply {
                    styleClass.addAll(
                        "btn",
                        "card--take--new-button"
                    )
                    graphic = FontIcon(MaterialDesign.MDI_MICROPHONE)
                    setOnAction { action() }
                }
            )
        }
    }
}
