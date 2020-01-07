package de.torbilicious

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.stirante.lolclient.ClientApi
import com.stirante.lolclient.ClientConnectionListener
import com.stirante.lolclient.ClientWebSocket
import generated.LolChampSelectChampSelectSession
import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import java.util.Observable
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis


val url = SimpleStringProperty("https://www.mobafire.com/league-of-legends")

class Main(args: Array<String>) {
    private val mapper = ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerKotlinModule()
    private val champions = readChampions()
    private val api = connectClient()



    init {

        api.openWebSocket().setSocketListener(object : ClientWebSocket.SocketListener {
            override fun onEvent(event: ClientWebSocket.Event?) {
                if (event == null || event.eventType != "Update" || event.uri != "/lol-champ-select/v1/session") {
                    return
                }

                val data = event.data
                if (data !is LolChampSelectChampSelectSession) {
                    return
                }

                val selection = data.myTeam.find { it.cellId == data.localPlayerCellId } ?: return

                if (selection.championId == 0) {
                    return
                }

                val id = selection.championId
                val name = champions.find { it.key == id.toString() }?.name ?: return

                println("Selected new champion: '$name'")

                val safeChampionName = getUrlSafeChampionName(name)

                runAsync {
                    mobafireUrlTemplate.replace("{{CHAMPION_NAME}}", safeChampionName)
                } ui {
                    url.set(it)
                }
            }

            override fun onClose(p0: Int, p1: String?) {
                exitProcess(1)
            }
        })

        launch<GUI>(args)
    }

    private fun connectClient(): ClientApi {
        println("Awaiting connection.")

        var connected = false

        val api = ClientApi()
        api.addClientConnectionListener(object : ClientConnectionListener {
            override fun onClientConnected() {
                connected = true
            }

            override fun onClientDisconnected() {
                exitProcess(2)
            }
        })


        val time = measureTimeMillis {
            while (!connected) {
                Thread.sleep(20)
            }
        }

        println("Connected in ${time}ms.")
        return api
    }

    data class Champion(val name: String, val key: String)
    data class ChampionJsonWrapper(val type: String, val data: List<Champion>)

    private fun readChampions(): List<Champion> {
        val championsJsonString = this.javaClass.getResource("/champions.json").readText()
        val json = mapper.readValue<ChampionJsonWrapper>(championsJsonString)

        return json.data
    }

    private fun getUrlSafeChampionName(name: String): String {
        return name
            .toLowerCase()
            .replace(".", "")
            .replace(" ", "-")
    }

    companion object {
        private const val mobafireUrlTemplate = "https://www.mobafire.com/league-of-legends/{{CHAMPION_NAME}}-guide"
    }
}

fun main(args: Array<String>) {
    Main(args)
}

