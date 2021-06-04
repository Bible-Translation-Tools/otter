package org.wycliffeassociates.otter.jvm.workbookapp.ui.screens

import dev.jbs.gridview.control.GridCell
import javafx.beans.binding.BooleanBinding
import org.wycliffeassociates.otter.jvm.controls.card.EmptyCardCell
import org.wycliffeassociates.otter.jvm.controls.card.ScriptureTakeCard
import org.wycliffeassociates.otter.jvm.controls.card.NewRecordingCard
import org.wycliffeassociates.otter.jvm.workbookapp.ui.model.TakeCardType
import org.wycliffeassociates.otter.jvm.workbookapp.ui.model.TakeCardModel
import tornadofx.*

class ScriptureTakesGridCell(
    newRecordingAction: () -> Unit,
    private val contentIsMarkable: BooleanBinding
) : GridCell<Pair<TakeCardType, TakeCardModel?>>() {

    private var rect = EmptyCardCell()
    private var takeCard = ScriptureTakeCard()
    private var newRecording = NewRecordingCard(
        FX.messages["newTake"],
        FX.messages["record"],
        newRecordingAction
    )

    override fun updateItem(item: Pair<TakeCardType, TakeCardModel?>?, empty: Boolean) {
        super.updateItem(item, empty)

        if (!empty && item != null) {
            if (item.first == TakeCardType.NEW) {
                graphic = newRecording
            } else if (
                item.first == TakeCardType.TAKE &&
                item.second != null && !item.second!!.selected
            ) {
                val model = item.second!!
                takeCard.takeProperty().set(model.take)
                takeCard.audioPlayerProperty().set(model.audioPlayer)
                takeCard.takeNumberProperty().set(model.take.number.toString())
                takeCard.allowMarkerProperty().set(contentIsMarkable.value)

                takeCard.prefWidthProperty().bind(widthProperty())
                takeCard.prefHeightProperty().bind(heightProperty())
                this.graphic = takeCard
            } else {
                rect.apply {
                    addClass("card--scripture-take--empty")
                    heightProperty().bind(this@ScriptureTakesGridCell.heightProperty())
                    widthProperty().bind(this@ScriptureTakesGridCell.widthProperty())
                }
                this.graphic = rect
            }
        }
    }
}
