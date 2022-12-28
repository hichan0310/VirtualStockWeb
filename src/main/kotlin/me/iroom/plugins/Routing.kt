package me.iroom.plugins

import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.title
import java.io.File
import java.time.LocalDateTime


fun sleep(time: Double) {
    val nextTime = LocalDateTime.now().plusNanos((time * 1000000000).toLong())
    while (nextTime >= LocalDateTime.now()) {
    }
}


// 아마도 프론트엔드가 건드리게 될 부분
fun Application.configureRouting() {
    val t=10
    CoroutineScope(Dispatchers.IO).launch {

    }
    routing {
        static("/static") {
            resources("static")
        }
        get("/") {
            call.respondText("asdf")
        }
        get("/index.html") {
            call.respondFile(File("src/main/resources/static/html/main.html"))
        }
    }
}
