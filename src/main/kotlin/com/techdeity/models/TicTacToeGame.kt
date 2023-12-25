package com.techdeity.models

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class TicTacToeGame {
    private val state = MutableStateFlow(GameState())
    private val playerSockets = ConcurrentHashMap<Char,WebSocketSession>()
    private val gameScope = CoroutineScope(SupervisorJob( )+ Dispatchers.IO)

    private var delayGameJob: Job?=null
    init {
        state.onEach(::broadcaste).launchIn(gameScope)
    }

    fun connectPlayer(session: WebSocketSession): Char? {
        // Check if 'x' player is already connected
        val isPlayerX = state.value.connectedPlayers.any { it == 'x' }

        // Determine the player character ('x' or 'o') based on availability
        val player = if (isPlayerX) 'o' else 'x'

        // Update the state
        state.update {
            // If the chosen player character is already connected, return null
            if (state.value.connectedPlayers.contains(player)) {
                return null
            }

            // If the chosen player character is not connected, add it to the state
            if (!playerSockets.containsKey(player)) {
                playerSockets[player] = session
            }

            // Update the state with the new connected player
            it.copy(connectedPlayers = it.connectedPlayers + player)
        }

        // Return the assigned player character ('x' or 'o')
        return player
    }


    fun disconnectPlayer(player:Char){
        playerSockets.remove(player)
           state.update {
                it.copy(connectedPlayers = it.connectedPlayers - player)
        }
    }

    suspend fun broadcaste(state: GameState){
        playerSockets.values.forEach { socket->
            socket.send(Json.encodeToString(state)
            )
        }
    }

    fun finishTurn(player:Char,x:Int,y:Int){
        if (state.value.field[x][y]!=null || state.value.winningPlayer!=null){
            return
        }
        if (state.value.playerAtTurn!=player){
            return
        }
        val currentPlayer=state.value.playerAtTurn
        state.update {
            val newField = it.field.also { field-> field[y][x] = currentPlayer }
            val isBoardFull = newField.all { row -> row.all { cell -> cell != null } }
            if (isBoardFull){
                startNewRoundDelayed()
            }
                it.copy(playerAtTurn = if(currentPlayer=='x') 'o' else 'x',
                    field = newField,

                    isBoardFull = isBoardFull,
                    winningPlayer = calculateWinner()?.also{
                        startNewRoundDelayed()
                    }

                )
        }

    }

    private fun calculateWinner(): Char? {

        val  field = state.value.field
        return if (field[0][0]!= null && field[0][0]==field[0][1] && field[0][1]==field[0][2]){
            field[0][0]
        }else if (field[1][0]!= null && field[1][0]==field[1][1] && field[1][1]==field[1][2]){
            field[1][0]
        }else if (field[2][0]!= null && field[2][0]==field[2][1] && field[2][1]==field[2][2]) {
            field[2][0]
        } else if (field[0][0]!= null && field[0][0]==field[1][0] && field[1][0]==field[2][0]) {
            field[0][0]
        } else if (field[0][1]!= null && field[0][1]==field[1][1] && field[1][1]==field[2][1]) {
            field[0][1]
        } else if (field[0][2]!= null && field[0][2]==field[1][2] && field[1][2]==field[2][2]) {
            field[0][2]
        } else if (field[0][0]!= null && field[0][0]==field[1][1] && field[1][1]==field[2][2]) {
            field[0][0]
        } else if (field[0][2]!= null && field[0][2]==field[1][1] && field[1][1]==field[2][0]) {
            field[0][2]
        } else null



    }

    private fun startNewRoundDelayed() {
        delayGameJob?.cancel()
        delayGameJob = gameScope.launch {
            delay(5000L)
            state.update {
                it.copy(
                    playerAtTurn = 'x',
                    field = GameState.emptyField(),
                    winningPlayer = null,
                    isBoardFull = false
                )
            }
        }
    }
}

