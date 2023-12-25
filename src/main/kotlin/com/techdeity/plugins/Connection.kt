package com.techdeity.plugins

import io.ktor.websocket.*
import java.util.concurrent.atomic.AtomicInteger

class Connection(val session:DefaultWebSocketSession) {
    companion object{
        val lastID = AtomicInteger(0)
    }
    val name ="user${lastID.getAndIncrement()}"
}