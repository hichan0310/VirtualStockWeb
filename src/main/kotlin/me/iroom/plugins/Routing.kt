package me.iroom.plugins

import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.get
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.title
import java.io.File

fun Application.configureRouting() {
    routing {
        static("/static") {
            resources("static")
        }
        get("/") {
            call.respondText("asdf")
        }
        get("/index.html") {
            call.respondFile(File("몰?루겠음"))
        }
    }
}
