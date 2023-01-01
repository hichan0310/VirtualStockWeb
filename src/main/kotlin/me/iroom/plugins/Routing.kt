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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.time.LocalDateTime
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException


@Serializable
data class AdminPacket(val execute: String) {}

@Serializable
data class BuyPacket(val id: String, val amount: Int) {}

@Serializable
data class SellPacket(val id: String, val amount: Int) {}

@Serializable
data class LoginPacket(val id: String, val pw:String){}


// 아마도 프론트엔드가 건드리게 될 부분
fun Application.configureRouting() {
    val datagramSocket: DatagramSocket = DatagramSocket()
    var price=1000
    var startPrice=1000
    var maxPrice=0
    var minPrice=2100000000
    var priceAvg:Double=0.0
    var len=0
    CoroutineScope(Dispatchers.IO).launch {
        val socket: DatagramSocket = withContext(Dispatchers.IO) {
            DatagramSocket(5000)
        }
        while(true){
            val packet: DatagramPacket = DatagramPacket(ByteArray(100), 100)
            withContext(Dispatchers.IO) {
                socket.receive(packet)
            }
            var _data: List<Byte> = packet.data.toList()
            try {
                var i = 0
                while (_data[i].toInt() != 0) i += 1
                _data = _data.slice(IntRange(0, i - 1))
            } catch (e: Exception) {
                println("wrong packet")
                continue
            }
            val data: ByteArray = _data.toByteArray()
            price=String(data).toInt()
            if(price>maxPrice) maxPrice = price
            if(price<minPrice) minPrice = price
            priceAvg=(priceAvg*len+price)/(len+1)
            len+=1
        }
    }
    CoroutineScope(Dispatchers.IO).launch{
        var next=LocalDateTime.now().plusMinutes(10)
        while (true){
            while(next>LocalDateTime.now()){}
            next=next.plusMinutes(10)

            val endPrice=price

            val connection = DriverManager.getConnection(
                "jdbc:mysql://localhost/pdb",
                "asdf", "asdf"
            )
            val sqlQ:String="INSERT INTO pricedb VALUES (${priceAvg}, ${startPrice}, ${endPrice}, ${maxPrice}, ${minPrice})"
            val pstmt=connection.prepareStatement(sqlQ)
            pstmt.executeUpdate()
            pstmt.close()
            connection.close()

            maxPrice=price
            minPrice=price
            startPrice=price
            len=0
            priceAvg=0.0
        }
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
            val dataString = call.receive<String>()
            val data: BuyPacket = Json.decodeFromString(dataString)
            call.respond(mapOf("result" to true))
            val sendData = ("Buy : $data").toByteArray()
            val packet = DatagramPacket(
                sendData, sendData.size,
                InetSocketAddress("localhost", 3000)
            )
            withContext(Dispatchers.IO) {
                datagramSocket.send(packet)
            }

            try {
                val connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost/pdb",
                    "asdf", "asdf"
                )
                val statement = connection.createStatement(
                    ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE
                )
                val res = statement.executeQuery("SELECT `id`, `pw`, `ip`, `money` FROM `account`")

                while (res.next()) {
                    if (res.getString("id")==data.id) {
                        if (res.getString("ip")==this.context.request.local.remoteAddress) {
                            if(res.getInt("money")>=data.amount*price){
                                val message = "buy,${data.amount},${data.id}".toByteArray()
                                val datagram=DatagramPacket(
                                    message, message.size,
                                    InetSocketAddress("localhost", 8888)
                                )
                                withContext(Dispatchers.IO) {
                                    datagramSocket.send(datagram)
                                }
                                var sqlQ:String="UPDATE account SET money=money-${data.amount*price} WHERE id = '${data.id}'"
                                var pstmt=connection.prepareStatement(sqlQ)
                                pstmt.executeUpdate()
                                pstmt.close()
                                sqlQ = "UPDATE account SET stock=stock+${data.amount} WHERE id = '${data.id}'"
                                pstmt=connection.prepareStatement(sqlQ)
                                pstmt.executeUpdate()
                                pstmt.close()
                            }
                        }
                        else {
                            call.respondText("로그인 필요")
                        }
                    }
                }
            }
            catch (e: Exception) {
                call.respondText("요청이 처리되지 못했습니다. \n$e")
            }
        }
        post("/sell") {
            val dataString = call.receive<String>()
            val data: SellPacket = Json.decodeFromString(dataString)
            call.respond(mapOf("result" to true))

            val sendData = ("Sell : $data").toByteArray()
            val packet = DatagramPacket(
                sendData, sendData.size,
                InetSocketAddress("localhost", 3000)
            )
            withContext(Dispatchers.IO) {
                datagramSocket.send(packet)
            }
            try {
                val connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost/pdb",
                    "asdf", "asdf"
                )
                val statement = connection.createStatement(
                    ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE
                )
                val res = statement.executeQuery("SELECT `id`, `pw`, `ip`, `money`, `stock` FROM `account`")

                while (res.next()) {
                    if (res.getString("id")==data.id) {
                        if (res.getString("ip")==this.context.request.local.remoteAddress) {
                            if (res.getInt("stock")>=data.amount) {
                                val message = "sell,${data.amount},${data.id}".toByteArray()
                                val datagram = DatagramPacket(
                                    message, message.size,
                                    InetSocketAddress("localhost", 8888)
                                )
                                withContext(Dispatchers.IO) {
                                    datagramSocket.send(datagram)
                                }
                                var sqlQ: String = "UPDATE account SET money=money+${data.amount * price} WHERE id = '${data.id}'"
                                var pstmt = connection.prepareStatement(sqlQ)
                                pstmt.executeUpdate()
                                pstmt.close()
                                sqlQ="UPDATE account SET stock=stock-${data.amount} WHERE id = '${data.id}'"
                                pstmt=connection.prepareStatement(sqlQ)
                                pstmt.executeUpdate()
                            }
                        }
                        else {
                            call.respondText("로그인 필요")
                        }
                    }
                }
            }
            catch (e: Exception) {
                call.respondText("요청이 처리되지 못했습니다. \n$e")
            }
        }
        post("/admin") {
            val data = call.receive<AdminPacket>().execute
            val sendData = ("AdminTest : $data").toByteArray()
            val packet = DatagramPacket(
                sendData, sendData.size,
                InetSocketAddress("localhost", 3000)
            )
            withContext(Dispatchers.IO) {
                datagramSocket.send(packet)
            }

            try {
                val command: List<String> = data.split(' ')

                /*
                 * 명령어 목록
                 * creat account [id] [pw] [money]
                 * charge money [id] [money]
                 * return money [id] [money]
                 * print money [id]
                 * print password [id]
                 */
                val connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost/pdb",
                    "asdf", "asdf"
                )
                val statement = connection.createStatement(
                    ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE
                )
                val result = statement.executeQuery("SELECT `id`, `pw`, `ip`, `money` FROM `account`")
                when (command[0]+command[1]){
                    "creataccount" -> {
                        var tmp=true
                        while (result.next())
                            if(result.getString("id")==command[2]) tmp=false
                        if(tmp) {
                            val id = command[2]
                            val pw = command[3]
                            val money = command[4]
                            val sqlQ: String = "INSERT INTO `account` VALUES ('${id}', '${pw}', 'created', ${money}, 0)"
                            val pstmt = connection.prepareStatement(sqlQ)
                            pstmt.executeUpdate()
                            pstmt.close()
                            call.respondText("account created")
                        }
                        else{
                            call.respondText("this id is already used")
                        }
                    }
                    "chargemoney" -> {
                        val id=command[2]
                        val money=command[3]
                        val sqlQ:String="UPDATE `account` SET money=money+${money} WHERE id = '${id}'"
                        val pstmt=connection.prepareStatement(sqlQ)
                        pstmt.executeUpdate()
                        pstmt.close()
                        call.respondText("money updated")
                    }
                    "returnmoney" -> {
                        val id=command[2]
                        val money=command[3]
                        val sqlQ:String="UPDATE `account` SET money=money-${money} WHERE id = '${id}'"
                        val pstmt=connection.prepareStatement(sqlQ)
                        pstmt.executeUpdate()
                        pstmt.close()
                        call.respondText("money updated")
                    }
                    "printmoney" -> {
                        val id=command[2]
                        var temp=true
                        while (result.next()) {
                            if(result.getString("id")==id){
                                call.respondText("password : ${result.getString("money")}")
                                temp=false
                                break
                            }
                        }
                        if(temp){
                            call.respondText("id가 존재하지 않음")
                        }
                    }
                    "printpassword" -> {
                        val id=command[2]
                        var temp=true
                        while (result.next()) {
                            if(result.getString("id")==id){
                                call.respondText("password : ${result.getString("pw")}")
                                temp=false
                                break
                            }
                        }
                        if(temp){
                            call.respondText("id가 존재하지 않음")
                        }
                    }
                    else -> {
                        call.respondText("명령어 리스트 보고 제대로 입력해라")
                    }
                }
                connection.close()
                statement.close()
            } catch (e: Exception) {
                call.respondText("뭔가 에러 있음\n"+e.message.toString())
            }
        }
        get("/admin"){
            call.respondText("어드민 페이지를 찾아낸 것은 축하한다. \n근데 왜 내가 어드민 페이지의 프론트엔드를 만들어 놓았을 거라고 생각하지? \n")
        }
        get("/login") {
            //login.html
        }
        post("/login"){
            val dataString = call.receive<String>()
            val data: LoginPacket = Json.decodeFromString(dataString)
            call.respondText("접속자 ip : ${this.context.request.local.remoteAddress}")
            val connection = DriverManager.getConnection(
                "jdbc:mysql://localhost/pdb",
                "asdf", "asdf"
            )
            val statement = connection.createStatement(
                ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE
            )
            val result = statement.executeQuery("SELECT `id`, `pw`, `ip`, `money`, `stock` FROM `account`")
            while(result.next()){
                if(result.getString("id")==data.id){
                    if(result.getString("pw")==data.pw){
                        val sqlQ:String="UPDATE account SET ip = '${this.context.request.local.remoteAddress}' WHERE id = '${data.id}'"
                        val pstmt=connection.prepareStatement(sqlQ)
                        pstmt.executeUpdate()
                        pstmt.close()
                        call.respondText("로그인 완료")
                    }
                    else{
                        call.respondText("password not correct")
                    }
                }
            }
            result.close()
            connection.close()
            statement.close()
        }
        get("/text"){
            call.respondText("asdfasdfasdf")
        }
        get("/json"){
            call.respondFile(File("C:\\Users\\maxma\\IdeaProjects\\VirtualStockWeb\\asdf.json"))
        }
        post("/text"){
            call.respondText("asdfasdfasdf")
        }
        post("/json"){
            call.respondFile(File("C:\\Users\\maxma\\IdeaProjects\\VirtualStockWeb\\asdf.json"))
        }
    }
}