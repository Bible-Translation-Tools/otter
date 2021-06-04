package org.wycliffeassociates.otter.jvm.workbookapp.ui.screens

import javafx.collections.FXCollections
import dev.jbs.gridview.control.GridView
import javafx.beans.binding.BooleanBinding
import javafx.scene.layout.Priority
import org.wycliffeassociates.otter.jvm.utils.onChangeAndDoNow
import org.wycliffeassociates.otter.jvm.workbookapp.ui.model.TakeCardType
import org.wycliffeassociates.otter.jvm.workbookapp.ui.model.TakeCardModel
import tornadofx.*
import kotlin.math.pow

class ScriptureTakesGridView(
    private val contentIsMarkable: BooleanBinding,
    private val recordNewTake: () -> Unit
) : GridView<Pair<TakeCardType, TakeCardModel?>>() {

    val gridItems = FXCollections.observableArrayList<TakeCardModel>()

    init {
        setCellFactory { ScriptureTakesGridCell(recordNewTake, contentIsMarkable) }
        cellHeightProperty().set(144.0)
        cellWidthProperty().set(200.0)

        hgrow = Priority.ALWAYS
        vgrow = Priority.ALWAYS

        fitToParentWidth()
        fitToParentHeight()

        widthProperty().onChange {
            updateItems()
        }

        gridItems.onChangeAndDoNow {
            updateItems()
        }
    }

    private fun updateItems() {
        val _items = gridItems
        if (_items == null || _items.isEmpty()) {
            items.clear()
            items.add(Pair(TakeCardType.NEW, null))
            // start from 2 because we just added the new recording card
            for (i in 2..3.toDouble().pow(2.0).toInt()) {
                items.add(Pair(TakeCardType.EMPTY, null))
            }
        } else {
            items.clear()
            items.add(Pair(TakeCardType.NEW, null))
            items.addAll(_items.map { Pair(TakeCardType.TAKE, it) })
            val mod = items.size % 3
            val needed = 3 - mod
            for (i in 1..needed) {
                items.add(Pair(TakeCardType.EMPTY, null))
            }
            val remaining = 3.toDouble().pow(2.0).toInt() - items.size
            if (remaining > 0) {
                for (i in 1..remaining) {
                    items.add(Pair(TakeCardType.EMPTY, null))
                }
            }
        }
    }
}
