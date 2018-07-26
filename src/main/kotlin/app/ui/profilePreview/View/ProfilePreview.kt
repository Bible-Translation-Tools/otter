package app.ui.profilePreview.View

import app.ui.userCreation.*
import app.ui.styles.ButtonStyles
import app.ui.profilePreview.ViewModel.ProfilePreviewViewModel
import app.widgets.profileIcon.ProfileIcon
import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.MaterialIconView
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.effect.DropShadow
import app.ui.welcomeScreen.WelcomeScreen
import javafx.scene.paint.Color
import tornadofx.*

class ProfilePreview : View() {

    var iconHash = PublishSubject.create<String>()                // subject to get the user iconHash
    var onClickNext = PublishSubject.create<Boolean>()          // subject to check if the NEXT button was clicked
    var onClickRedo = PublishSubject.create<Boolean>()             // subject to check if the REDO button was clicked
    var audioListened = PublishSubject.create<Boolean>()            // subject to check if the audio was listened

    private val viewModel = ProfilePreviewViewModel(iconHash, onClickNext, onClickRedo, audioListened)

    var newUserButton = ProfileIcon("12345678901", 152.0)
    val micIcon = MaterialIconView(MaterialIcon.MIC_NONE, "25px")
    val rightArrow = MaterialIconView(MaterialIcon.ARROW_FORWARD, "25px")
    val closeIcon = MaterialIconView(MaterialIcon.CLOSE, "25px")

    override val root = borderpane {

        val closeButton = button(messages["close"], closeIcon) {
            importStylesheet(ButtonStyles::class)
            addClass(ButtonStyles.rectangleButtonDefault)

            style {
                alignment = Pos.CENTER
                closeIcon.fill = c("#CC4141")
                effect = DropShadow(10.0, Color.GRAY)

            }
            action {
                find(ProfilePreview::class).replaceWith(WelcomeScreen::class)  // navigate to home, todo implement ui navigator
                viewModel.listenedAudio(false)                        // set listened audio false to reset the ui state and hide the next and redo buttons
            }
        }

        top {

            hbox {
                alignment = Pos.BOTTOM_RIGHT
                add(closeButton)
                style {
                    alignment = Pos.BOTTOM_RIGHT
                    paddingRight = 40.0
                    paddingTop = 40.0
                }
            }
        }

        center {

            hbox {
                spacing = 48.0
                alignment = Pos.CENTER

                vbox {
                    micIcon.fill = c("#CC4141")
                    spacing = 12.0
                    alignment = Pos.CENTER

                    stackpane {
                        hide()                                                    // each ui element is hidden or showed individuallly because otherwise the middle widget button makes an unnatural shift
                        audioListened.subscribeBy(                                // subject used to verify if the audio has been listened
                                onNext = { if (it) show() else hide() }           // check if the audio has been listened if so, display the button REDO if not just hide it
                        )
                        circle {

                            style {
                                radius = 55.0
                                fill = c("#E5E5E5")
                            }
                        }

                        button("", micIcon) {

                            importStylesheet(ButtonStyles::class)
                            addClass(ButtonStyles.roundButton)
                            style {

                                backgroundColor += Color.WHITE
                                cursor = Cursor.HAND
                                minWidth = 75.0.px
                                minHeight = 75.0.px
                                fontSize = 2.em
                                textFill = c("#CC4141")
                            }
                            action {
                                viewModel.listenedAudio(false)
                                find(ProfilePreview::class).replaceWith(UserCreation::class)

                            }
                        }
                    }

                    label(messages["redo"]) {
                        hide()
                        audioListened.subscribeBy(
                                onNext = { if (it) show() else hide() }
                        )
                    }
                }

                stackpane {
                    circle {

                        style {
                            radius = 120.0
                            fill = c("#E5E5E5")
                        }

                    }
                    iconHash.subscribeBy(
                            onNext = {
                                add(newUserButton)
                                newUserButton.svgHash = it          // update iconhash from subject
                            }
                    )


                    newUserButton.profIcon.action {
                        viewModel.listenedAudio(true)
                    }
                }

                vbox {
                    spacing = 12.0
                    alignment = Pos.CENTER
                    rightArrow.fill = c("#FFFFFF")
                    stackpane {
                        hide()
                        audioListened.subscribeBy(                           // check if the audio has been listened if so, display the button NEXT if not just hide it
                                onNext = { if (it) show() else hide() }
                        )
                        circle {

                            style {
                                radius = 55.0
                                fill = c("#E5E5E5")
                            }

                        }
                        button("", rightArrow) {

                            importStylesheet(ButtonStyles::class)
                            addClass(ButtonStyles.roundButton)
                            style {
                                backgroundColor += c("#CC4141")
                                cursor = Cursor.HAND
                                minWidth = 75.0.px
                                minHeight = 75.0.px
                                fontSize = 2.em
                                textFill = c("#CC4141")
                            }
                            // TODO("insert action here when user clicks next button")
                        }
                    }

                    label(messages["next"]) {
                        hide()
                        audioListened.subscribeBy(
                                onNext = {
                                    if (it) show() else hide()    // check if the audio has been listened if so, display the Label NEXT if not just hide it
                                }
                        )
                    }
                }

            }

        }
    }

    init {
        viewModel.newIconHash("12345678901") // set an icon hash string to the subject iconHash declared in top
    }
}