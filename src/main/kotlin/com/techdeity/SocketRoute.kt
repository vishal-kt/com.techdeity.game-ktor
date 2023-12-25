package com.techdeity

import com.techdeity.models.TicTacToeGame
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*

fun Route.socket(game:TicTacToeGame){
    webSocket("/play"){
        val player = game.connectPlayer(this)
        if(player==null){
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY,"Game is full"))
            return@webSocket
        }
        try {
            send(Frame.Text("You are player $player"))
            for (frame in incoming){
                when(frame){
                    is Frame.Text ->{
                        val command = frame.readText()
                        game.handleCommand(player,command)
                    }
                }
            }
        }finally {
            game.disconnectPlayer(player)
        }
    }
}