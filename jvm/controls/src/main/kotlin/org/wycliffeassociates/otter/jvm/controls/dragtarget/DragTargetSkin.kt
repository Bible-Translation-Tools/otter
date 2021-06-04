package org.wycliffeassociates.otter.jvm.controls.dragtarget

import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.SkinBase
import javafx.scene.layout.StackPane
import org.kordamp.ikonli.javafx.FontIcon
import org.kordamp.ikonli.materialdesign.MaterialDesign
import org.wycliffeassociates.otter.jvm.utils.bindSingleChild
import tornadofx.*

abstract class DragTargetSkin(
    control: DragTarget
) : SkinBase<DragTarget>(control) {
    protected var selectedTakePlaceholder: Node by singleAssign()

    init {
        val root = StackPane().apply {
            selectedTakePlaceholder = vbox {
                addClass("card--take__placeholder")
                skinnable.dragBinding.onChange {
                    toggleClass("card--take__border-glow", it)
                }
            }
            vbox {
                bindSingleChild(skinnable.selectedNodeProperty)
            }
            vbox {
                addClass("card--take__dragtarget-overlay")
                alignment = Pos.CENTER
                label {
                    addClass("card--take__add")
                    graphic = FontIcon(MaterialDesign.MDI_PLUS)
                }
                visibleProperty().bind(skinnable.dragBinding)
            }
        }
        children.addAll(root)
    }
}
