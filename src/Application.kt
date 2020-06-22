package com.adedom.heroku

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
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

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    Database.connect(
        "jdbc:mysql://us-cdbr-east-05.cleardb.net:3306/heroku_1393de2d66fc96b?reconnect=true",
        driver = "com.mysql.jdbc.Driver",
        user = "bc162b7210edb9",
        password = "dae67b90"
    )

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

    }
}

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
