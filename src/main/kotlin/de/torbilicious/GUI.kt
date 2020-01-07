package de.torbilicious

import javafx.scene.Parent
import javafx.scene.layout.BorderPane
import javafx.scene.web.WebEngine
import tornadofx.*

class GUI : App(MainView::class)

class MainView: View() {
    private val engine: WebEngine by singleAssign()
    override val root = borderpane {
        top {
            label("Current URL:") {
                url.onChange {
                    text = "Current URL: $it"
                }
            }
        }

        center {
            webview {
                url.onChange {
                    engine.load(it)
                }
            }
        }
    }
}
