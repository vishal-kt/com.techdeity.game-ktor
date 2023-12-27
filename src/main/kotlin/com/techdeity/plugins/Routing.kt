package com.techdeity.plugins

import com.techdeity.models.TicTacToeGame
import com.techdeity.socket
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(game: TicTacToeGame) {
    routing {
        socket(game)
    }
}
