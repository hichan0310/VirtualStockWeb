package me.iroom.plugins

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.time.LocalDateTime


fun sleep(time: Double) {
    val nextTime = LocalDateTime.now().plusNanos((time * 1000000000).toLong())
    while (nextTime >= LocalDateTime.now()) {
    }
}


@Serializable
data class AdminPacket(val execute: String) {}

@Serializable
data class BuyPacket(val uid: String, val amount: Int, val ipAddr: String, val userAgent: String) {}

@Serializable
data class SellPacket(val uid: String, val amount: Int, val ipAddr: String, val userAgent: String) {}

@Serializable
data class PricePacket(val startPrice: Int, val endPrice: Int, val maxPrice: Int, val minPrice: Int)


// 아마도 프론트엔드가 건드리게 될 부분
fun Application.configureRouting() {
    CoroutineScope(Dispatchers.IO).launch{

    }
    routing {
        static("/static") {
            resources("static")
        }
        get("/") {
            call.respondText("/index.html로 이동하세요")
        }
        get("/index.html") {
            call.respondFile(File("src/main/resources/static/html/index.html"))
        }
        post("/buy") {
            val data = call.receive<BuyPacket>()
            call.respondText("receive: ${data}\n")
            call.respond(mapOf("result" to true))
            val datagramSocket: DatagramSocket = withContext(Dispatchers.IO) {
                DatagramSocket()
            }
            val sendData = ("Buy : $data").toByteArray()
            val packet = DatagramPacket(
                sendData, sendData.size,
                InetSocketAddress("localhost", 3000)
            )
            withContext(Dispatchers.IO) {
                datagramSocket.send(packet)
            }
        }
        post("/sell") {
            val data = call.receive<SellPacket>()
            call.respondText("receive: ${data}\n")
            call.respond(mapOf("result" to true))
            val datagramSocket: DatagramSocket = withContext(Dispatchers.IO) {
                DatagramSocket()
            }
            val sendData = ("Sell : $data").toByteArray()
            val packet = DatagramPacket(
                sendData, sendData.size,
                InetSocketAddress("localhost", 3000)
            )
            withContext(Dispatchers.IO) {
                datagramSocket.send(packet)
            }
        }
        post("/admin") {
            val data = call.receive<AdminPacket>().execute
            val datagramSocket: DatagramSocket = withContext(Dispatchers.IO) {
                DatagramSocket()
            }
            val sendData = ("AdminTest : $data").toByteArray()
            val packet = DatagramPacket(
                sendData, sendData.size,
                InetSocketAddress("localhost", 3000)
            )
            withContext(Dispatchers.IO) {
                datagramSocket.send(packet)
            }
            call.respondText("receive: ${data}\n")


            try {
                val execute: List<String> = data.split(' ')

                /*
                 * 명령어 목록
                 * creat account [username(sessionId?)] [money]
                 * charge money [username(sessionId?)] [money]
                 * delete account [username(sessionId?)]
                 * get DB
                 */
            } catch (e: Exception) {
                call.respondText(e.message.toString())
            }
        }
    }
}