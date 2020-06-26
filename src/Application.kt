package com.adedom.heroku

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused")
fun Application.module() {
    val config = HikariConfig().apply {
        jdbcUrl = "jdbc:mysql://us-cdbr-east-05.cleardb.net:3306/heroku_1393de2d66fc96b?reconnect=true"
        driverClassName = "com.mysql.cj.jdbc.Driver"
        username = "bc162b7210edb9"
        password = "dae67b90"
        maximumPoolSize = 10
    }
    val dataSource = HikariDataSource(config)
    Database.connect(dataSource)

    install(DefaultHeaders)
    install(CallLogging)
    install(ContentNegotiation) {
        jackson {
        }
    }

    install(Routing) {

        get("/test") {
            val response = BaseResponse()
            call.respond(response)
        }

        get("players") {
            val response = PlayerResponse()
            val players = transaction {
                Players.selectAll()
                    .map { Players.toPlayer(it) }
            }
            response.players = players
            response.success = true
            response.message = "Fetch players success"
            call.respond(response)
        }

        get("call-sign-in") {
            httpClient {
                val response = it.post<SignInResponse> {
                    url("https://the-egg-game.herokuapp.com/api/account/sign-in")
                    contentType(ContentType.Application.Json)
                    body = SignInRequest("admin", "1234")
                }
                call.respond(response)
            }
        }
    }
}

inline fun httpClient(bloc: (HttpClient) -> Unit) {
    val client = HttpClient {
        install(JsonFeature) {
            serializer = JacksonSerializer()
        }
    }
    bloc.invoke(client)
    client.close()
}

data class SignInRequest(
    val username: String? = null,
    val password: String? = null
)

data class SignInResponse(
    var success: Boolean = false,
    var message: String? = "Error",
    var accessToken: String? = null
)

open class BaseResponse(
    var success: Boolean = false,
    var message: String? = "Error"
)

data class PlayerResponse(
    var players: List<Player>? = null
) : BaseResponse()

data class Player(
    val playerId: Int? = null,
    val name: String? = null,
    val image: String? = null,
    val state: String? = null,
    val gender: String? = null
)

object Players : Table(name = "player") {
    val playerId = Players.integer(name = "player_id").autoIncrement()
    val name = Players.varchar(name = "name", length = 50)
    val image = Players.varchar(name = "image", length = 50)
    val state = Players.varchar(name = "state", length = 10)
    val gender = Players.varchar(name = "gender", length = 5)

    override val primaryKey: PrimaryKey?
        get() = PrimaryKey(playerId, name = "PK_Player_ID")

    fun toPlayer(row: ResultRow) = Player(
        playerId = row[playerId],
        name = row[name],
        image = row[image],
        state = row[state],
        gender = row[gender]
    )

}
