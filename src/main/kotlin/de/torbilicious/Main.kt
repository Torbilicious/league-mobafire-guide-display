package de.torbilicious

import org.http4k.client.ApacheClient
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.format.Jackson.auto
import java.time.Duration
import java.time.Instant


private val API_KEY: String? = System.getenv("API_KEY")
private const val REGION: String = "euw1"
private const val API_BASE: String = "https://${REGION}.api.riotgames.com/lol"


data class Summoner(
    val name: String,
    val accountId: String
)

data class Game(
    val timestamp: Long,
    val gameId: String,


    val lane: String,
    val champion: String,
    val platformId: String,
    val queue: Int,
    val role: String,
    val season: Int,

    val time: Instant = Instant.ofEpochMilli(timestamp)
)

data class GamesResponse(
    val matches: List<Game>,
    val endIndex: Int,
    val startIndex: Int,
    val totalGames: Int
)

class Main {
    private val client = ApacheClient()
    private val summonerLens = Body.auto<Summoner>().toLens()
    private val gamesLens = Body.auto<GamesResponse>().toLens()

    init {
        require(API_KEY != null) { "You must provide the key via an environment variable named 'API_KEY'!" }

        val summonerName = "Torbilicious"
        val summoner = getSummonerByName(summonerName)

        println(summoner)
        println()


        val games = getGamesForSummoner(summoner).sortedByDescending { it.timestamp }.take(3)

        games.forEach { println(it) }
        println()
        println()


        val fiveMinutesAgoAsEpoch = Instant.now().minus(Duration.ofMinutes(5)).toEpochMilli()
        val lastGame = games.first()
        val lastGameStartedLessThanFiveMinutesAgo = lastGame.timestamp > fiveMinutesAgoAsEpoch

        println("lastGameTime = ${lastGame.time}")
        println("lastGameStartedLessThanFiveMinutesAgo = $lastGameStartedLessThanFiveMinutesAgo")
    }

    private fun getSummonerByName(name: String): Summoner {
        val response = client(
            Request(GET, "${API_BASE}/summoner/v4/summoners/by-name/${name}")
                .headers(listOf("X-Riot-Token" to API_KEY))
        )

        return summonerLens(response)
    }

    private fun getGamesForSummoner(summoner: Summoner): List<Game> {
        val response = client(
            Request(GET, "${API_BASE}/match/v4/matchlists/by-account/${summoner.accountId}")
                .headers(listOf("X-Riot-Token" to API_KEY))
        )

        val gamesResponse = gamesLens(response)

        return gamesResponse.matches
    }
}

fun main() {
    Main()
}

