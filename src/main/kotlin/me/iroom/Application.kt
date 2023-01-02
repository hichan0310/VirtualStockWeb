package me.iroom

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.CallFailed.install
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import me.iroom.plugins.*
import java.time.LocalDateTime

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}




fun Application.module() {
    install(ContentNegotiation){
        json()
    }
    configureRouting()
}
