package me.iroom.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.util.*
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
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.time.LocalDate
import java.util.logging.Logger

fun sendTo3000Port(message: String) {
    val sendData = message.toByteArray()
    DatagramSocket().send(
        DatagramPacket(
            sendData, sendData.size,
            InetSocketAddress("localhost", 3000)
        )
    )
}


@Serializable
data class AdminPacket(val execute: String) {}

@Serializable
data class BuyPacket(val amount: Int) {}

@Serializable
data class SellPacket(val amount: Int) {}

@Serializable
data class LoginPacket(val id: String, val pw: String) {}

data class Session(
    val id: String,
    val money: Int,
    val stock: Int,
    val inputMoney: Int,
    val outputMoney: Int,
    var available: Long?
) {}


// 아마도 프론트엔드가 건드리게 될 부분
// 은 개소리였고
fun Application.configureRouting() {
    install(Sessions) {
        val secretSignKey = hex("6819b57a326945c1968f45236589")
        cookie<Session>("sessionid", SessionStorageMemory()) {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 3600
            transform(SessionTransportTransformerMessageAuthentication(secretSignKey))
        }
    }
    val datagramSocket: DatagramSocket = DatagramSocket()
    var price = 1000
    var startPrice = 1000
    var maxPrice = 0
    var minPrice = 1000
    var priceAvg: Double = 0.0
    var len = 0
    CoroutineScope(Dispatchers.IO).launch {
        val socket: DatagramSocket = withContext(Dispatchers.IO) {
            DatagramSocket(5000)
        }
        while (true) {
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
            price = String(data).toInt()
            if (price > maxPrice) maxPrice = price
            if (price < minPrice) minPrice = price
            priceAvg = (priceAvg * len + price) / (len + 1)
            len += 1
        }
    }
    CoroutineScope(Dispatchers.IO).launch {
        var next = LocalDateTime.now().plusMinutes(1)
        while (true) {
            while (next > LocalDateTime.now()) {
            }

            val endPrice = price

            val connection = DriverManager.getConnection(
                "jdbc:mysql://localhost/ilbanbest",
                "ilban", "ilbanbest"
            )
            val sqlQ: String =
                "INSERT INTO pricedb VALUES (${next.year}, ${next.monthValue}, ${next.dayOfMonth}, ${next.hour}, ${next.minute}, ${startPrice}, ${maxPrice}, ${minPrice}, ${endPrice}, ${priceAvg})"
            next = next.plusMinutes(1)
            val pstmt = connection.prepareStatement(sqlQ)
            pstmt.executeUpdate()
            pstmt.close()
            connection.close()

            maxPrice = price
            minPrice = price
            startPrice = price
            len = 0
            priceAvg = 0.0
        }
    }
    routing {
        static("/static") {
            resources("static")
        }
        get("/") {
            call.respondRedirect("/main")
        }
        get("/main") {
            /*
            val accountData = call.sessions.get<Session>()
            var textttt: String = ""
            if (accountData == null) textttt = """
                    <!DOCTYPE html>
<!--
   Stellar by HTML5 UP
   html5up.net | @ajlkn
   Free for personal and commercial use under the CCA 3.0 license (html5up.net/license)
-->
<html>
<head>
    <!--    <meta name="viewport" content="width=device-width,initial-scale=1">-->
    <title>1학년 1반 주식투자</title>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, user-scalable=no"/>
    <link rel="stylesheet" href="/static/html/assets/css/main.css"/>
    <noscript>
        <link rel="stylesheet" href="/static/html/assets/css/noscript.css"/>
    </noscript>
    <script src="https://code.jquery.com/jquery-1.12.4.min.js"></script>
</head>

<body class="is-preload">

<!-- Wrapper -->
<div id="wrapper">

    <!-- Header -->
    <header class="alt">
        <div style="width: max-content; float: right; margin: auto; padding: 15px">
            <a id="LOGIN" class="button" style="align-content: end">LOGIN</a>
        </div>
    </header>
    <header id="header" class="alt">
        <span class="logo"><img src="/static/html/images/coin.png" alt=""/></span>
        <h1>삼선전자</h1>
        1학년 1반에서 만든 주식투자 사이트입니다<br/>
        제작자: <a href="">@김서호</a> / <a href="">@김주한</a> / <a href="">@신희찬</a> / <a href="">@박종현</a> / <a href="">@인유민</a> / <a href="">@김지민</a>
    </header>

    <!-- 버튼 -->
    <nav id="nav">
        <ul>
            <li><a href="#ghost_house" class="active">귀신의 집</a></li>
            <li><a href="#coin">나히다 코인</a></li>
            <li><a href="#lotto">로또</a></li>
        </ul>
    </nav>

    <!-- Main -->
    <div id="main">
        <!-- Introduction -->
        <section id="ghost_house" class="main">
            <header class="major special">
                <h2>귀신의 집</h2>
            </header>
            <div class="spotlight">
                <div class="content">
                    <header class="major">
                        <h1 style="align-content: center; text-align: left; margin-right: 0; line-height: 10pt">"글쎄, 여기서 여학생이 죽었다는데...</h1>
                        <h1 style="align-content: center; text-align: right; margin-right: 0;">아무도 이유를 모른다는 거야..."</h1>
                        <h2 style="text-align: start">당신은 죽지 않고 탈출할 수 있을까...?!</h2>
                    </header>
                    <div class="spotlight">
                        <p>
                            2023년 1월 3일 밤이었다. 달도 보이지 않는 소름 돋을 만큼 어두운 밤, 나는 별빛에만 의지해 깊은 산속 어딘가로 또각또각 걸어가고 있었다.
                            휘이잉. 들리는 것이라곤 바람 소리와 내 발걸음 소리뿐. 발걸음이 멈추었을 때, 내 눈앞에 있던 것은 폐가 한 채였다.
                            이곳을 들어갔던 사람들은 다시 나오지 못했다. 겉보기엔 그냥 평범한 폐가일 뿐인데 말이다. 나는 부서져 가는 울타리를 넘어 정원으로 들어갔다.
                            정원에는 까만 풀들이 자라있었다. 아니 죽은 식물들이었을지도 모르겠다. 나는 폐가에 더 가까이 다가가 보았다. 문 앞에 도착했다. 과연 다시 나올 수 있을까...?!
                            끼익. 문을 열었더니 깜깜한 내부가 보였다. 긴 복도를 따라 걸었다. 턱. 발에 무언가 걸려 보았더니 손전등이 하나 있었다. 설마 켜질까 싶어 전원 버튼을 눌러보았다.
                            딸깍. 불을 켜자마자 내 눈앞에 나타난 것은 입에 나뭇잎을 가득 물고 있는 여학생이었다. 여기서 죽었던 여학생이었던 것이다. 나는 그 자리에서 움직이지 못했다.
                            삐그덕삐그덕. 점점 나와 가까워지는 듯했다. 나는 정신을 차리고 달리기 시작했다. 내부로 말이다. 과연 나는, 당신은 이 집을 무사히 탈출할 수 있을까...?
                        </p>
                        <!--                    <ul class="actions">-->
                        <!--                        <li><a href="https://namu.wiki/w/%EB%82%98%ED%9E%88%EB%8B%A4" class="button">Learn More</a></li>-->
                        <!--                    </ul>-->
                        <span class="image"><img src="/static/html/images/ghost.jpg" alt=""/></span>
                    </div>
                </div>
            </div>
        </section>

        <!-- First Section -->
        <section id="coin" class="main special">
            <header class="major">
                <h2>나히다 코인</h2>
            </header>
            <div class="stikyMemo" id="stikyMemo">
                <div class="stikyMemoData" style="text-align: center; font-weight: bold"></div>
                <div class="stikyMemoData">보유 코인량 : <span id="coin_value">1000</span> <span class="stikyUnit">N</span>
                </div>
                <div class="stikyMemoData">보유 화폐량 : <span id="money_value">0</span> <span class="stikyUnit">W</span>
                </div>
                <div class="stikyMemoData">수익률 : <span id="benefit_value">0</span> <span class="stikyUnit">%</span>
                </div>
            </div>

            <div class="col-12" style="position: relative; display: flex; flex-direction: row-reverse;">

                <!--                <select name="demo-category" id="demo-category">-->
                <!--                    <option value="">- time - &nbsp;</option>-->
                <!--                    <option value="5">5 min &nbsp; </option>-->
                <!--                    <option value="10">10 min &nbsp; </option>-->
                <!--                    <option value="20">20 min &nbsp; </option>-->
                <!--                    <option value="60">60 min &nbsp; </option>-->
                <!--                    <option value="120">120 min &nbsp; </option>-->
                <!--                </select>-->

                <div class="col-6 col-12-small">
                    <input type="checkbox" id="demo-120" name="demo-120">
                    <label for="demo-120">120 min</label>
                </div>

                <div class="col-6 col-12-small">
                    <input type="checkbox" id="demo-60" name="demo-60">
                    <label for="demo-60">60 min</label>
                </div>

                <div class="col-6 col-12-small">
                    <input type="checkbox" id="demo-20" name="demo-20">
                    <label for="demo-20">20 min</label>
                </div>

                <div class="col-6 col-12-small">
                    <input type="checkbox" id="demo-10" name="demo-10">
                    <label for="demo-10">10 min</label>
                </div>

                <div class="col-6 col-12-small">
                    <input type="checkbox" id="demo-5" name="demo-5">
                    <label for="demo-5">5 min</label>
                </div>

            </div>

            <div class="box alt">
                <div class="row gtr-uniform">
                    <!--                    <div class="col-12"><span class="image fit"><img src="image/graph.png" alt=""/></span></div>-->
                    <div class="col-12">
                        <div id="chartContainer" style="height: 370px; width: 100%;"></div>
                        <script type="text/javascript" src="https://canvasjs.com/assets/script/canvasjs.min.js"></script>
                        <script type="text/javascript" src="https://canvasjs.com/assets/script/jquery-1.11.1.min.js"></script>
                    </div>

                    <div class="col-9" style="display: block">
                        <span class="nowPrice">&nbsp;&nbsp;현재 시세 : 1N = </span>
                        <span id="exchange_rate" class="nowPrice">1000</span>
                        <span class="nowPrice">W &nbsp;&nbsp;&nbsp;&nbsp;</span>
                    </div>
                    <div class="col-3" style="align-content: end; flex: none">
                        <div style="width: inherit; display: flex; justify-content: flex-end">
                            <button id="NOW" onclick="exchange_rate_update()">new</button>
                        </div>
                    </div>

                </div>
            </div>

        </section>

        <section id="lotto" class="main">
            <header class="major special">
                <h2>로또</h2>
            </header>
            <div class="spotlight">
                <div class="content">
                    <header class="major">
                        <h1 style="text-align: start">일확천금의 기회를 노려보세요!</h1>
                    </header>
                    <div class="spotlight">
                        <p>
                            당신은 10,000분의 1의 확률을 뚫을 수 있습니까? 일확천금의 기회를 노려보세요!
                            당신의 2023년 새해 운을 시험해보세요! 지금 당장 201호 앞으로 가서 원하는 숫자만 고르면 참여완료!
                            로또 수익의 90%를 나누어 드립니다! 작년 한해 수고한 당신에게 드리는 선물!
                        </p>
                        <!--                    <ul class="actions">-->
                        <!--                        <li><a href="https://namu.wiki/w/%EB%82%98%ED%9E%88%EB%8B%A4" class="button">Learn More</a></li>-->
                        <!--                    </ul>-->
                        <span class="image"><img src="/static/html/images/nahida.jpg" alt=""/></span>
                    </div>
                </div>
            </div>
        </section>

    </div>

    <!-- Footer -->
    <footer id="footer">
        <section>
            <h2>우리는 한국 과학의 미래를 선도한다</h2>
            <p>교훈은 切問近思(절문근사)로 논어의 한 구절을 따왔다.
                절문(切問)은 궁극적인 관심을 가지고 진지하게 묻고 고민하는 것을 말하며 창의성 및 수월성 교육을 뜻 한다.
                근사(近思)는 다른 사람의 일을 자신의 일상사를 중심으로 잘 정리하는 것을 말하며, 인성 교육을 뜻한다.
            </p>
            <ul class="actions">
                <li><a href="https://namu.wiki/w/%EA%B2%BD%EA%B8%B0%EB%B6%81%EA%B3%BC%ED%95%99%EA%B3%A0%EB%93%B1%ED%95%99%EA%B5%90" class="button">경기북과학고등학교</a></li>
                <li><a href="https://gbs.wiki/w/GBSWiki:%EB%8C%80%EB%AC%B8" class="button">북곽위키</a></li>
            </ul>
        </section>
        <section>
            <dl class="alt">
                <dt>Address</dt>
                <dd>경기도 의정부시 체육로 135번길 32 (녹양동)</dd>
                <dt>Phone</dt>
                <dd>031-870-2764</dd>
            </dl>
            <ul class="icons">

                <li><a href="https://github.com/hichan0310" class="icon brands fa-github alt"><span
                        class="label">GitHub</span></a></li>

            </ul>
        </section>
    </footer>

</div>
<!--graph-->
<script>
    ${'$'}("#LOGIN").click(function () {
        location.href = "login"
    })

    const url = 'http://192.168.24.182:8080'

    function exchange_rate_update() {
        document.getElementById("exchange_rate").innerText;

        fetch(url + "/now", {
            method: 'GET',
        }).then(x => console.log(x.json()))
    }

    var check5 = document.getElementById('demo-5');
    var check10 = document.getElementById('demo-10');
    var check20 = document.getElementById('demo-20');
    var check60 = document.getElementById('demo-60');
    var check120 = document.getElementById('demo-120');

    var dataPoints5 = [];
    var dataPoints10 = [];
    var dataPoints20 = [];
    var dataPoints60 = [];
    var dataPoints120 = [];
    var dataPoints_negative = [];
    var dataPoints_positive = [];
    var chartData = {
        // animationEnabled: true,
        theme: "light2", // "light1", "light2", "dark1", "dark2"
        // exportEnabled: true,
        subtitles: [{
            text: "1분간 평균"
        }],
        axisX: {
            interval: 1,
            valueFormatString: "HH:mm"
        },
        axisY: {
            prefix: "N",
            gridDashType: 'dash'
            // title: "가격"
        },
        data: [
            {
                type: "candlestick",
                xValueFormatString: "HH:mm",
                yValueFormatString: "N##0",
                dataPoints: dataPoints_negative,
                toolTipContent: '시간: {x}<br /><strong>가격:</strong><br />시가: {y[0]}, 종가: {y[3]}<br />고가: {y[1]}, 저가: {y[2]}',
                color: 'blue'
            },
            {
                type: "candlestick",
                xValueFormatString: "HH:mm",
                yValueFormatString: "N##0",
                dataPoints: dataPoints_positive,
                toolTipContent: '시간: {x}<br /><strong>가격:</strong><br />시가: {y[0]}, 종가: {y[3]}<br />고가: {y[1]}, 저가: {y[2]}',
                color: 'red',
                risingColor: 'red'
            },
            {
                type: "line",
                xValueFormatString: "HH:mm",
                yValueFormatString: "N##0.0",
                markerType: 'none',
                dataPoints: dataPoints5,
                color: 'black',
                visible: false,
                toolTipContent: "시간: {x}<br />평균 가격: {y}",
                showInLegend: true,
                legendText: '5분',
            },
            {
                type: "line",
                xValueFormatString: "HH:mm",
                yValueFormatString: "N##0.0",
                markerType: 'none',
                dataPoints: dataPoints10,
                color: 'black',
                visible: false,
                toolTipContent: "시간: {x}<br />평균 가격: {y}",
                showInLegend: true,
                legendText: '10분',
            },
            {
                type: "line",
                xValueFormatString: "HH:mm",
                yValueFormatString: "N##0.0",
                markerType: 'none',
                dataPoints: dataPoints20,
                color: 'black',
                visible: false,
                toolTipContent: "시간: {x}<br />평균 가격: {y}",
                showInLegend: true,
                legendText: '20분',
            },
            {
                type: "line",
                xValueFormatString: "HH:mm",
                yValueFormatString: "N##0.0",
                markerType: 'none',
                dataPoints: dataPoints60,
                color: 'black',
                visible: false,
                toolTipContent: "시간: {x}<br />평균 가격: {y}",
                showInLegend: true,
                legendText: '60분',
            },
            {
                type: "line",
                xValueFormatString: "HH:mm",
                yValueFormatString: "N##0.0",
                markerType: 'none',
                dataPoints: dataPoints120,
                color: 'black',
                visible: false,
                toolTipContent: "시간: {x}<br />평균 가격: {y}",
                showInLegend: true,
                legendText: '120분',
            },
        ]
    };

    var chart = new CanvasJS.Chart("chartContainer", chartData);

    var data = [
                """.trimIndent()
            else textttt = """
                    <!DOCTYPE html>
<!--
   Stellar by HTML5 UP
   html5up.net | @ajlkn
   Free for personal and commercial use under the CCA 3.0 license (html5up.net/license)
-->
<html>
<head>
    <!--    <meta name="viewport" content="width=device-width,initial-scale=1">-->
    <title>1학년 1반 주식투자</title>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, user-scalable=no"/>
    <link rel="stylesheet" href="/static/html/assets/css/main.css"/>
    <noscript>
        <link rel="stylesheet" href="/static/html/assets/css/noscript.css"/>
    </noscript>
    <script src="https://code.jquery.com/jquery-1.12.4.min.js"></script>
</head>

<body class="is-preload">

<!-- Wrapper -->
<div id="wrapper">

    <!-- Header -->
    <header id="header" class="alt">
        <span class="logo"><img src="/static/html/images/coin.png" alt=""/></span>
        <h1>삼선전자</h1>
        1학년 1반에서 만든 주식투자 사이트입니다<br/>
        제작자: <a href="">@김서호</a> / <a href="">@김주한</a> / <a href="">@신희찬</a> / <a href="">@박종현</a> / <a href="">@인유민</a> / <a href="">@김지민</a>
    </header>

    <!-- 버튼 -->
    <nav id="nav">
        <ul>
            <li><a href="#ghost_house" class="active">귀신의 집</a></li>
            <li><a href="#coin">나히다 코인</a></li>
            <li><a href="#lotto">로또</a></li>
        </ul>
    </nav>

    <!-- Main -->
    <div id="main">

        <!-- Introduction -->
        <section id="ghost_house" class="main">
            <header class="major special">
                <h2>귀신의 집</h2>
            </header>
            <div class="spotlight">
                <div class="content">
                    <header class="major">
                        <h1 style="align-content: center; text-align: left; margin-right: 0; line-height: 10pt">"글쎄, 여기서 여학생이 죽었다는데...</h1>
                        <h1 style="align-content: center; text-align: right; margin-right: 0;">아무도 이유를 모른다는 거야..."</h1>
                        <h2 style="text-align: start">당신은 죽지 않고 탈출할 수 있을까...?!</h2>
                    </header>
                    <div class="spotlight">
                        <p>
                            2023년 1월 3일 밤이었다. 달도 보이지 않는 소름 돋을 만큼 어두운 밤, 나는 별빛에만 의지해 깊은 산속 어딘가로 또각또각 걸어가고 있었다.
                            휘이잉. 들리는 것이라곤 바람 소리와 내 발걸음 소리뿐. 발걸음이 멈추었을 때, 내 눈앞에 있던 것은 폐가 한 채였다.
                            이곳을 들어갔던 사람들은 다시 나오지 못했다. 겉보기엔 그냥 평범한 폐가일 뿐인데 말이다. 나는 부서져 가는 울타리를 넘어 정원으로 들어갔다.
                            정원에는 까만 풀들이 자라있었다. 아니 죽은 식물들이었을지도 모르겠다. 나는 폐가에 더 가까이 다가가 보았다. 문 앞에 도착했다. 과연 다시 나올 수 있을까...?!
                            끼익. 문을 열었더니 깜깜한 내부가 보였다. 긴 복도를 따라 걸었다. 턱. 발에 무언가 걸려 보았더니 손전등이 하나 있었다. 설마 켜질까 싶어 전원 버튼을 눌러보았다.
                            딸깍. 불을 켜자마자 내 눈앞에 나타난 것은 입에 나뭇잎을 가득 물고 있는 여학생이었다. 여기서 죽었던 여학생이었던 것이다. 나는 그 자리에서 움직이지 못했다.
                            삐그덕삐그덕. 점점 나와 가까워지는 듯했다. 나는 정신을 차리고 달리기 시작했다. 내부로 말이다. 과연 나는, 당신은 이 집을 무사히 탈출할 수 있을까...?
                        </p>
                        <!--                    <ul class="actions">-->
                        <!--                        <li><a href="https://namu.wiki/w/%EB%82%98%ED%9E%88%EB%8B%A4" class="button">Learn More</a></li>-->
                        <!--                    </ul>-->
                        <span class="image"><img src="/static/html/images/ghost.jpg" alt=""/></span>
                    </div>
                </div>
            </div>
        </section>

        <!-- First Section -->
        <section id="coin" class="main special">
            <header class="major">
                <h2>나히다 코인</h2>
            </header>
            <div class="stikyMemo" id="stikyMemo">
                <div class="stikyMemoData" style="text-align: center; font-weight: bold">[ID]</div>
                <div class="stikyMemoData">보유 코인량 : <span id="coin_value">1000</span> <span class="stikyUnit">N</span>
                </div>
                <div class="stikyMemoData">보유 화폐량 : <span id="money_value">0</span> <span class="stikyUnit">W</span>
                </div>
                <div class="stikyMemoData">수익률 : <span id="benefit_value">0</span> <span class="stikyUnit">%</span>
                </div>
            </div>

            <div class="col-12" style="position: relative; display: flex; flex-direction: row-reverse;">

                <div class="col-6 col-12-small">
                    <input type="checkbox" id="demo-120" name="demo-120">
                    <label for="demo-120">120 min</label>
                </div>

                <div class="col-6 col-12-small">
                    <input type="checkbox" id="demo-60" name="demo-60">
                    <label for="demo-60">60 min</label>
                </div>

                <div class="col-6 col-12-small">
                    <input type="checkbox" id="demo-20" name="demo-20">
                    <label for="demo-20">20 min</label>
                </div>

                <div class="col-6 col-12-small">
                    <input type="checkbox" id="demo-10" name="demo-10">
                    <label for="demo-10">10 min</label>
                </div>

                <div class="col-6 col-12-small">
                    <input type="checkbox" id="demo-5" name="demo-5">
                    <label for="demo-5">5 min</label>
                </div>

            </div>

            <div class="box alt">
                <div class="row gtr-uniform">
                    <!--                    <div class="col-12"><span class="image fit"><img src="/static/html/image/graph.png" alt=""/></span></div>-->
                    <div class="col-12">
                        <div id="chartContainer" style="height: 370px; width: 100%;"></div>
                        <script type="text/javascript" src="https://canvasjs.com/assets/script/canvasjs.min.js"></script>
                        <script type="text/javascript" src="https://canvasjs.com/assets/script/jquery-1.11.1.min.js"></script>
                    </div>

                    <div class="col-9" style="display: block">
                        <span class="nowPrice">현재 시세 : 1N = </span>
                        <span id="exchange_rate" class="nowPrice">1000</span>
                        <span class="nowPrice">W &nbsp;&nbsp;&nbsp;&nbsp;</span>
                    </div>
                    <div class="col-4">
                        <a id="NOW" class="button" style="align-content: end;">시세 업데이트</a>
                    </div>

                    <div class="col-10"><input type="number" id="coin_amount" value=""
                                               placeholder="매도 또는 매수할 코인량을 입력해주세요..."
                                               style="width: 100%;"/></div>
                    <div class="col-2">
                        <label>Nahida</label>
                    </div>
                </div>
            </div>

            <label id="LABEL" style="color: maroon; text-align: center; width: auto"></label>

            <footer class="major">
                <ul class="actions special">
                    <li><a id="SELL" class="button large primary disabled">매도(SELL)</a></li>
                    <li><a id="BUY" class="button large primary disabled">매수(BUY)</a></li>
                </ul>
            </footer>
        </section>

        <section id="lotto" class="main">
            <header class="major special">
                <h2>로또</h2>
            </header>
            <div class="spotlight">
                <div class="content">
                    <header class="major">
                        <h1 style="text-align: start">일확천금의 기회를 노려보세요!</h1>
                    </header>
                    <div class="spotlight">
                        <p>
                            당신은 10,000분의 1의 확률을 뚫을 수 있습니까? 일확천금의 기회를 노려보세요!
                            당신의 2023년 새해 운을 시험해보세요! 지금 당장 201호 앞으로 가서 원하는 숫자만 고르면 참여완료!
                            로또 수익의 90%를 나누어 드립니다! 작년 한해 수고한 당신에게 드리는 선물!
                        </p>
                        <!--                    <ul class="actions">-->
                        <!--                        <li><a href="https://namu.wiki/w/%EB%82%98%ED%9E%88%EB%8B%A4" class="button">Learn More</a></li>-->
                        <!--                    </ul>-->
                        <span class="image"><img src="/static/html/images/nahida.jpg" alt=""/></span>
                    </div>
                </div>
            </div>
        </section>

    </div>

    <!-- Footer -->
    <footer id="footer">
        <section>
            <h2>우리는 한국 과학의 미래를 선도한다</h2>
            <p>교훈은 切問近思(절문근사)로 논어의 한 구절을 따왔다.
                절문(切問)은 궁극적인 관심을 가지고 진지하게 묻고 고민하는 것을 말하며 창의성 및 수월성 교육을 뜻 한다.
                근사(近思)는 다른 사람의 일을 자신의 일상사를 중심으로 잘 정리하는 것을 말하며, 인성 교육을 뜻한다.
            </p>
            <ul class="actions">
                <li><a href="https://namu.wiki/w/%EA%B2%BD%EA%B8%B0%EB%B6%81%EA%B3%BC%ED%95%99%EA%B3%A0%EB%93%B1%ED%95%99%EA%B5%90" class="button">경기북과학고등학교</a></li>
                <li><a href="https://gbs.wiki/w/GBSWiki:%EB%8C%80%EB%AC%B8" class="button">북곽위키</a></li>
            </ul>
        </section>
        <section>
            <dl class="alt">
                <dt>Address</dt>
                <dd>경기도 의정부시 체육로 135번길 32 (녹양동)</dd>
                <dt>Phone</dt>
                <dd>031-870-2764</dd>
            </dl>
            <ul class="icons">
                <!--                <li><a href="https://www.instagram.com/hichan0310/" class="icon brands fa-instagram alt"><span-->
                <!--                        class="label">Instagram</span></a></li>-->
                <li><a href="https://github.com/hichan0310" class="icon brands fa-github alt"><span
                        class="label">GitHub</span></a></li>
                <!--                <li><a href="https://www.instagram.com/millio_120/" class="icon brands fa-instagram alt"><span-->
                <!--                        class="label">Instagram</span></a></li>-->
            </ul>
        </section>
        <!--        <p class="copyright">&copy; Untitled. Design: <a href="https://html5up.net">HTML5 UP</a>.</p>  -->
    </footer>

</div>
<!--graph-->
<script>
    var coin_value
    var money_value
    var exchange_rate
    var id

    ${'$'}("#coin_amount").on("propertychange change keyup paste input", function () {
        document.getElementById("LABEL").innerText = ""
    })

    const url = 'http://192.168.24.182:8080'

    ${'$'}("#NOW").click(async function() {
        var res = fetch(url + "/now", {
            method: 'GET',
        }).then(await (await (x => x.json())))
        
        exchange_rate = res['price']
        document.getElementById("exchange_rate").innerText = exchange_rate
    })

    var check5 = document.getElementById('demo-5');
    var check10 = document.getElementById('demo-10');
    var check20 = document.getElementById('demo-20');
    var check60 = document.getElementById('demo-60');
    var check120 = document.getElementById('demo-120');

    var dataPoints5 = [];
    var dataPoints10 = [];
    var dataPoints20 = [];
    var dataPoints60 = [];
    var dataPoints120 = [];
    var dataPoints_negative = [];
    var dataPoints_positive = [];
    var chartData = {
        // animationEnabled: true,
        theme: "light2", // "light1", "light2", "dark1", "dark2"
        // exportEnabled: true,
        subtitles: [{
            text: "1분간 평균"
        }],
        axisX: {
            interval: 1,
            valueFormatString: "HH:mm"
        },
        axisY: {
            prefix: "N",
            gridDashType: 'dash'
            // title: "가격"
        },
        data: [
            {
                type: "candlestick",
                xValueFormatString: "HH:mm",
                yValueFormatString: "N##0",
                dataPoints: dataPoints_negative,
                toolTipContent: '시간: {x}<br /><strong>가격:</strong><br />시가: {y[0]}, 종가: {y[3]}<br />고가: {y[1]}, 저가: {y[2]}',
                color: 'blue'
            },
            {
                type: "candlestick",
                xValueFormatString: "HH:mm",
                yValueFormatString: "N##0",
                dataPoints: dataPoints_positive,
                toolTipContent: '시간: {x}<br /><strong>가격:</strong><br />시가: {y[0]}, 종가: {y[3]}<br />고가: {y[1]}, 저가: {y[2]}',
                color: 'red',
                risingColor: 'red'
            },
            {
                type: "line",
                xValueFormatString: "HH:mm",
                yValueFormatString: "N##0.0",
                markerType: 'none',
                dataPoints: dataPoints5,
                color: 'black',
                visible: false,
                toolTipContent: "시간: {x}<br />평균 가격: {y}",
                showInLegend: true,
                legendText: '5분',
            },
            {
                type: "line",
                xValueFormatString: "HH:mm",
                yValueFormatString: "N##0.0",
                markerType: 'none',
                dataPoints: dataPoints10,
                color: 'black',
                visible: false,
                toolTipContent: "시간: {x}<br />평균 가격: {y}",
                showInLegend: true,
                legendText: '10분',
            },
            {
                type: "line",
                xValueFormatString: "HH:mm",
                yValueFormatString: "N##0.0",
                markerType: 'none',
                dataPoints: dataPoints20,
                color: 'black',
                visible: false,
                toolTipContent: "시간: {x}<br />평균 가격: {y}",
                showInLegend: true,
                legendText: '20분',
            },
            {
                type: "line",
                xValueFormatString: "HH:mm",
                yValueFormatString: "N##0.0",
                markerType: 'none',
                dataPoints: dataPoints60,
                color: 'black',
                visible: false,
                toolTipContent: "시간: {x}<br />평균 가격: {y}",
                showInLegend: true,
                legendText: '60분',
            },
            {
                type: "line",
                xValueFormatString: "HH:mm",
                yValueFormatString: "N##0.0",
                markerType: 'none',
                dataPoints: dataPoints120,
                color: 'black',
                visible: false,
                toolTipContent: "시간: {x}<br />평균 가격: {y}",
                showInLegend: true,
                legendText: '120분',
            },
        ]
    };

    var chart = new CanvasJS.Chart("chartContainer", chartData);

    var data = [
                """.trimIndent()
            File("src/main/resources/static/html/main.html").bufferedWriter().use { it.write(textttt) }


            var str: String = ""
            val connection = DriverManager.getConnection(
                "jdbc:mysql://localhost/ilbanbest",
                "ilban", "ilbanbest"
            )
            val statement = connection.createStatement(
                ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE
            )
            val result = statement.executeQuery("SELECT * FROM `pricedb`")
            while (result.next())
                str += "[${result.getInt(1)}, ${result.getInt(2)}, ${result.getInt(3)}, ${result.getInt(4)}, ${
                    result.getInt(
                        5
                    )
                }, ${result.getInt(6)}, ${result.getInt(7)}, ${result.getInt(8)}, ${result.getInt(9)}, ${
                    result.getInt(
                        10
                    )
                }],\n"
            Files.write(
                Paths.get("src/main/resources/static/html/main.html"),
                str.toByteArray(),
                StandardOpenOption.APPEND
            )

            if (accountData == null) textttt = """
                    ];
                        const lengthList = [5, 10, 20, 60, 120];

                        var chartLoad = function () {
                            getDataPointsFromCSV(data)
                        }

                        var dataMinSet = [
                            [],
                            [],
                            [],
                            [],
                            [],
                        ];

                        function getDataPointsFromCSV(data) {
                            while (data.length > 200) {
                                data.shift()
                            }

                            for (var l = 0; l < 5; l++) {
                                var temp = [];
                                for (var i = 0; i < data.length; i++) {
                                    if (i >= lengthList[l]) {
                                        var tempArr = [...data[i - 1]]
                                        tempArr.pop()
                                        tempArr.pop()
                                        tempArr.pop()
                                        tempArr.pop()
                                        tempArr.pop()
                                        tempArr.push(temp.reduce((a, b) => a + b) / lengthList[l])
                                        dataMinSet[l].push(tempArr);
                                        temp.shift()
                                    }
                                    temp.push(data[i][9])
                                }
                                var tempArr = [...data[data.length - 1]]
                                tempArr.pop()
                                tempArr.pop()
                                tempArr.pop()
                                tempArr.pop()
                                tempArr.pop()
                                tempArr.push(temp.reduce((a, b) => a + b) / lengthList[l])
                                dataMinSet[l].push(tempArr);
                            }
                            for (var i = 0; i < 5; i++) {
                                while (dataMinSet[i].length > 45) {
                                    dataMinSet[i].shift()
                                }
                            }

                            for (var i = 45; i >= 1; i--) {
                                l = data.length - i
                                if (data[l].length > 0) {
                                    if (data[l][5] < data[l][8]) {
                                        dataPoints_positive.push({
                                            x: new Date(
                                                parseInt(data[l][0]),
                                                parseInt(data[l][1] - 1),
                                                parseInt(data[l][2]),
                                                parseInt(data[l][3]),
                                                parseInt(data[l][4])
                                            ),
                                            y: [
                                                parseInt(data[l][5]),
                                                parseInt(data[l][6]),
                                                parseInt(data[l][7]),
                                                parseInt(data[l][8])
                                            ],
                                            showToolTip: 'true'
                                        })
                                    } else {
                                        dataPoints_negative.push({
                                            x: new Date(
                                                parseInt(data[l][0]),
                                                parseInt(data[l][1] - 1),
                                                parseInt(data[l][2]),
                                                parseInt(data[l][3]),
                                                parseInt(data[l][4])
                                            ),
                                            y: [
                                                parseInt(data[l][5]),
                                                parseInt(data[l][6]),
                                                parseInt(data[l][7]),
                                                parseInt(data[l][8])
                                            ],
                                            showToolTip: 'true'
                                        })
                                    }
                                }
                            }

                            for (var i = 0; i < dataMinSet[0].length; i++) {
                                dataPoints5.push({
                                    x: new Date(
                                        parseInt(dataMinSet[0][i][0]),
                                        parseInt(dataMinSet[0][i][1] - 1),
                                        parseInt(dataMinSet[0][i][2]),
                                        parseInt(dataMinSet[0][i][3]),
                                        parseInt(dataMinSet[0][i][4])
                                    ),
                                    y: parseInt(dataMinSet[0][i][5]),
                                    showToolTip: "false"
                                })
                            }
                            for (var i = 0; i < dataMinSet[1].length; i++) {
                                dataPoints10.push({
                                    x: new Date(
                                        parseInt(dataMinSet[1][i][0]),
                                        parseInt(dataMinSet[1][i][1] - 1),
                                        parseInt(dataMinSet[1][i][2]),
                                        parseInt(dataMinSet[1][i][3]),
                                        parseInt(dataMinSet[1][i][4])
                                    ),
                                    y: parseInt(dataMinSet[1][i][5]),
                                    showToolTip: "false"
                                })
                            }
                            for (var i = 0; i < dataMinSet[2].length; i++) {
                                dataPoints20.push({
                                    x: new Date(
                                        parseInt(dataMinSet[2][i][0]),
                                        parseInt(dataMinSet[2][i][1] - 1),
                                        parseInt(dataMinSet[2][i][2]),
                                        parseInt(dataMinSet[2][i][3]),
                                        parseInt(dataMinSet[2][i][4])
                                    ),
                                    y: parseInt(dataMinSet[2][i][5]),
                                    showToolTip: "false"
                                })
                            }
                            for (var i = 0; i < dataMinSet[3].length; i++) {
                                dataPoints60.push({
                                    x: new Date(
                                        parseInt(dataMinSet[3][i][0]),
                                        parseInt(dataMinSet[3][i][1] - 1),
                                        parseInt(dataMinSet[3][i][2]),
                                        parseInt(dataMinSet[3][i][3]),
                                        parseInt(dataMinSet[3][i][4])
                                    ),
                                    y: parseInt(dataMinSet[3][i][5]),
                                    showToolTip: "false"
                                })
                            }
                            for (var i = 0; i < dataMinSet[4].length; i++) {
                                dataPoints120.push({
                                    x: new Date(
                                        parseInt(dataMinSet[4][i][0]),
                                        parseInt(dataMinSet[4][i][1] - 1),
                                        parseInt(dataMinSet[4][i][2]),
                                        parseInt(dataMinSet[4][i][3]),
                                        parseInt(dataMinSet[4][i][4])
                                    ),
                                    y: parseInt(dataMinSet[4][i][5]),
                                    showToolTip: "false"
                                })
                            }
                            chart.render();
                        }

                        window.onload = chartLoad();

                        check5.onclick = function () {
                            chartData.data[2].visible = !!check5.checked;
                            chart.render();
                        }
                        check10.onclick = function () {
                            chartData.data[3].visible = !!check10.checked;
                            chart.render();
                        }
                        check20.onclick = function () {
                            chartData.data[4].visible = !!check20.checked;
                            chart.render();
                        }
                        check60.onclick = function () {
                            chartData.data[5].visible = !!check60.checked;
                            chart.render();
                        }
                        check120.onclick = function () {
                            chartData.data[6].visible = !!check120.checked;
                            chart.render();
                        }

                        function update_chart(update_data) {
                            data.shift();
                            data.push(update_data);

                            if (update_data[5] < update_data[8]) {
                                chart.options.data[1].dataPoints.shift()
                                chart.options.data[1].dataPoints.push({
                                    x: new Date(
                                        parseInt(update_data[0]),
                                        parseInt(update_data[1] - 1),
                                        parseInt(update_data[2]),
                                        parseInt(update_data[3]),
                                        parseInt(update_data[4])
                                    ),
                                    y: [
                                        parseInt(update_data[5]),
                                        parseInt(update_data[6]),
                                        parseInt(update_data[7]),
                                        parseInt(update_data[8])
                                    ],
                                    showToolTip: 'true'
                                })
                            } else {
                                chart.options.data[0].dataPoints.shift()
                                chart.options.data[0].dataPoints.push({
                                    x: new Date(
                                        parseInt(update_data[0]),
                                        parseInt(update_data[1] - 1),
                                        parseInt(update_data[2]),
                                        parseInt(update_data[3]),
                                        parseInt(update_data[4])
                                    ),
                                    y: [
                                        parseInt(update_data[5]),
                                        parseInt(update_data[6]),
                                        parseInt(update_data[7]),
                                        parseInt(update_data[8])
                                    ],
                                    showToolTip: 'true'
                                })
                            }

                            for (var i = 0; i < 5; i++) {
                                var len = lengthList[i];

                                var temp_arr = [...data[data.length - 1]];
                                temp_arr.pop();
                                temp_arr.pop();
                                temp_arr.pop();
                                temp_arr.pop();
                                temp_arr.pop();
                                temp_arr.push((dataMinSet[i][dataMinSet[i].length - 1][5] * len - data[data.length - 1 - len][9] + data[data.length - 1][9]) / len);
                                chart.options.data[i + 2].dataPoints.shift()
                                chart.options.data[i + 2].dataPoints.push({
                                    x: new Date(
                                        temp_arr[0],
                                        temp_arr[1] - 1,
                                        temp_arr[2],
                                        temp_arr[3],
                                        temp_arr[4],
                                    ),
                                    y: temp_arr[5],
                                    showToolTip: 'false'
                                })
                            }

                            chart.render();
                        }


                    </script>
                    <!-- Scripts -->
                    <script src="/static/html/assets/js/jquery.min.js"></script>
                    <script src="/static/html/assets/js/jquery.scrollex.min.js"></script>
                    <script src="/static/html/assets/js/jquery.scrolly.min.js"></script>
                    <script src="/static/html/assets/js/browser.min.js"></script>
                    <script src="/static/html/assets/js/breakpoints.min.js"></script>
                    <script src="/static/html/assets/js/util.js"></script>
                    <script src="/static/html/assets/js/main.js"></script>

                    </body>
                    </html>
                """.trimIndent()
            else textttt = """
                    ];
                        const lengthList = [5, 10, 20, 60, 120];

                        function client_information_initialize(){
                            var res = fetch(url + "/onload", {
                                method: "GET",
                            }).then(x => x.json())
                            console.log(res)

                            coin_value = res['stock']
                            money_value = res['money']
                            exchange_rate = res['suic']
                            id = res['id']

                            document.getElementById("coin_value").innerText = coin_value
                            document.getElementById("money_value").innerText = money_value
                            document.getElementById("exchange_rate").innerText = exchange_rate
                            document.getElementById("ID").innerText = id
                        }

                        var window_onload = function () {
                            client_information_initialize()

                            getDataPointsFromCSV(data)
                        }

                        var dataMinSet = [
                            [],
                            [],
                            [],
                            [],
                            [],
                        ];

                        function getDataPointsFromCSV(data) {
                            while (data.length > 200) {
                                data.shift()
                            }

                            for (var l = 0; l < 5; l++) {
                                var temp = [];
                                for (var i = 0; i < data.length; i++) {
                                    if (i >= lengthList[l]) {
                                        var tempArr = [...data[i - 1]]
                                        tempArr.pop()
                                        tempArr.pop()
                                        tempArr.pop()
                                        tempArr.pop()
                                        tempArr.pop()
                                        tempArr.push(temp.reduce((a, b) => a + b) / lengthList[l])
                                        dataMinSet[l].push(tempArr);
                                        temp.shift()
                                    }
                                    temp.push(data[i][9])
                                }
                                var tempArr = [...data[data.length - 1]]
                                tempArr.pop()
                                tempArr.pop()
                                tempArr.pop()
                                tempArr.pop()
                                tempArr.pop()
                                tempArr.push(temp.reduce((a, b) => a + b) / lengthList[l])
                                dataMinSet[l].push(tempArr);
                            }
                            for (var i = 0; i < 5; i++) {
                                while (dataMinSet[i].length > 45) {
                                    dataMinSet[i].shift()
                                }
                            }

                            for (var i = 45; i >= 1; i--) {
                                l = data.length - i
                                if (data[l].length > 0) {
                                    if (data[l][5] < data[l][8]) {
                                        dataPoints_positive.push({
                                            x: new Date(
                                                parseInt(data[l][0]),
                                                parseInt(data[l][1] - 1),
                                                parseInt(data[l][2]),
                                                parseInt(data[l][3]),
                                                parseInt(data[l][4])
                                            ),
                                            y: [
                                                parseInt(data[l][5]),
                                                parseInt(data[l][6]),
                                                parseInt(data[l][7]),
                                                parseInt(data[l][8])
                                            ],
                                            showToolTip: 'true'
                                        })
                                    } else {
                                        dataPoints_negative.push({
                                            x: new Date(
                                                parseInt(data[l][0]),
                                                parseInt(data[l][1] - 1),
                                                parseInt(data[l][2]),
                                                parseInt(data[l][3]),
                                                parseInt(data[l][4])
                                            ),
                                            y: [
                                                parseInt(data[l][5]),
                                                parseInt(data[l][6]),
                                                parseInt(data[l][7]),
                                                parseInt(data[l][8])
                                            ],
                                            showToolTip: 'true'
                                        })
                                    }
                                }
                            }

                            for (var i = 0; i < dataMinSet[0].length; i++) {
                                dataPoints5.push({
                                    x: new Date(
                                        parseInt(dataMinSet[0][i][0]),
                                        parseInt(dataMinSet[0][i][1] - 1),
                                        parseInt(dataMinSet[0][i][2]),
                                        parseInt(dataMinSet[0][i][3]),
                                        parseInt(dataMinSet[0][i][4])
                                    ),
                                    y: parseInt(dataMinSet[0][i][5]),
                                    showToolTip: "false"
                                })
                            }
                            for (var i = 0; i < dataMinSet[1].length; i++) {
                                dataPoints10.push({
                                    x: new Date(
                                        parseInt(dataMinSet[1][i][0]),
                                        parseInt(dataMinSet[1][i][1] - 1),
                                        parseInt(dataMinSet[1][i][2]),
                                        parseInt(dataMinSet[1][i][3]),
                                        parseInt(dataMinSet[1][i][4])
                                    ),
                                    y: parseInt(dataMinSet[1][i][5]),
                                    showToolTip: "false"
                                })
                            }
                            for (var i = 0; i < dataMinSet[2].length; i++) {
                                dataPoints20.push({
                                    x: new Date(
                                        parseInt(dataMinSet[2][i][0]),
                                        parseInt(dataMinSet[2][i][1] - 1),
                                        parseInt(dataMinSet[2][i][2]),
                                        parseInt(dataMinSet[2][i][3]),
                                        parseInt(dataMinSet[2][i][4])
                                    ),
                                    y: parseInt(dataMinSet[2][i][5]),
                                    showToolTip: "false"
                                })
                            }
                            for (var i = 0; i < dataMinSet[3].length; i++) {
                                dataPoints60.push({
                                    x: new Date(
                                        parseInt(dataMinSet[3][i][0]),
                                        parseInt(dataMinSet[3][i][1] - 1),
                                        parseInt(dataMinSet[3][i][2]),
                                        parseInt(dataMinSet[3][i][3]),
                                        parseInt(dataMinSet[3][i][4])
                                    ),
                                    y: parseInt(dataMinSet[3][i][5]),
                                    showToolTip: "false"
                                })
                            }
                            for (var i = 0; i < dataMinSet[4].length; i++) {
                                dataPoints120.push({
                                    x: new Date(
                                        parseInt(dataMinSet[4][i][0]),
                                        parseInt(dataMinSet[4][i][1] - 1),
                                        parseInt(dataMinSet[4][i][2]),
                                        parseInt(dataMinSet[4][i][3]),
                                        parseInt(dataMinSet[4][i][4])
                                    ),
                                    y: parseInt(dataMinSet[4][i][5]),
                                    showToolTip: "false"
                                })
                            }
                            chart.render();
                        }

                        window.onload = window_onload();

                        check5.onclick = function () {
                            chartData.data[2].visible = !!check5.checked;
                            chart.render();
                        }
                        check10.onclick = function () {
                            chartData.data[3].visible = !!check10.checked;
                            chart.render();
                        }
                        check20.onclick = function () {
                            chartData.data[4].visible = !!check20.checked;
                            chart.render();
                        }
                        check60.onclick = function () {
                            chartData.data[5].visible = !!check60.checked;
                            chart.render();
                        }
                        check120.onclick = function () {
                            chartData.data[6].visible = !!check120.checked;
                            chart.render();
                        }

                        document.getElementById("stikyMemo").style.top = document.documentElement.scrollTop;
                        window.addEventListener('scroll', () => {
                            if (document.getElementById("lotto").offsetTop - 300 > window.scrollY && window.scrollY > document.getElementById("coin").offsetTop - 150) {
                                document.getElementById("stikyMemo").style.display = "block";
                                document.getElementById("stikyMemo").style.top = document.documentElement.scrollTop + 150 + 'px';
                            } else {
                                document.getElementById("stikyMemo").style.display = "none";
                            }
                        });

                        ${'$'}("#coin_amount").on("propertychange change keyup paste input", function () {
                            if (document.getElementById("coin_amount").value > 0) {
                                document.getElementById("SELL").className = "button large primary"
                                document.getElementById("BUY").className = "button large primary"
                            } else {
                                document.getElementById("SELL").className = "button large primary disabled"
                                document.getElementById("BUY").className = "button large primary disabled"
                            }
                        })

                        document.getElementById("SELL").onclick = function () {
                            var coin_amount = Number(document.getElementById('coin_amount').value);

                            if (coin_amount > coin_value) {
                                document.getElementById("LABEL").innerText = "팔 수 없습니다."
                            } else {
                                coin_value = coin_value - coin_amount
                                money_value = money_value + coin_amount * exchange_rate

                                document.getElementById('coin_value').innerText = coin_value
                                document.getElementById("money_value").innerText = money_value

                                fetch(url + "/sell", {
                                    method: 'POST',
                                    headers: {
                                        'Accept': 'application/json',
                                        'Content-Type': 'application/json',
                                    },
                                    body: JSON.stringify({
                                        amount: coin_amount,
                                    }),
                                })

                                console.log('sell')
                                client_information_initialize()
                            }
                        }
                        document.getElementById("BUY").onclick = function () {
                            var coin_amount = Number(document.getElementById('coin_amount').value);

                            if (coin_amount * exchange_rate > money_value) {
                                document.getElementById("LABEL").innerText = "살 수 없습니다."
                            } else {
                                coin_value = coin_value - coin_amount
                                money_value = money_value + coin_amount * exchange_rate

                                document.getElementById('coin_value').innerText = coin_value
                                document.getElementById("money_value").innerText = money_value

                                fetch(url + "/buy", {
                                    method: 'POST',
                                    headers: {
                                        'Accept': 'application/json',
                                        'Content-Type': 'application/json',
                                    },
                                    body: JSON.stringify({
                                        amount: coin_amount,
                                    }),
                                }).then(result => console.log(result))

                                console.log('buy')
                                client_information_initialize()
                            }
                        }


                    </script>
                    <!-- Scripts -->
                    <script src="/static/html/assets/js/jquery.min.js"></script>
                    <script src="/static/html/assets/js/jquery.scrollex.min.js"></script>
                    <script src="/static/html/assets/js/jquery.scrolly.min.js"></script>
                    <script src="/static/html/assets/js/browser.min.js"></script>
                    <script src="/static/html/assets/js/breakpoints.min.js"></script>
                    <script src="/static/html/assets/js/util.js"></script>
                    <script src="/static/html/assets/js/main.js"></script>

                    </body>
                    </html>
                """.trimIndent()
            Files.write(
                Paths.get("src/main/resources/static/html/main.html"),
                textttt.toByteArray(),
                StandardOpenOption.APPEND
            )
            call.respondFile(File("src/main/resources/static/html/main.html"))
             */
            if (call.sessions.get<Session>() == null) call.respondFile(File("src/main/resources/static/html/account_null_main.html"))
            else call.respondFile(File("src/main/resources/static/html/account_main.html"))
        }
        post("/buy") {
            val dataString = call.receive<String>()
            val accountData = call.sessions.get<Session>()
            val data: BuyPacket = Json.decodeFromString(dataString)
            sendTo3000Port("Buy : $data ($accountData)\ntime : ${System.currentTimeMillis()}")
            if (accountData == null) {
                call.respondRedirect("/login")
            } else {
                if (accountData.available != null) {
                    if (accountData.available!! < System.currentTimeMillis()) {
                        accountData.available = null
                    }
                }
                if (accountData.available == null) {
                    try {
                        val connection = DriverManager.getConnection(
                            "jdbc:mysql://localhost/ilbanbest",
                            "ilban", "ilbanbest"
                        )
                        if (accountData.money >= data.amount * price) {
                            val message = "buy,${data.amount},${accountData.id}".toByteArray()
                            val datagram = DatagramPacket(
                                message, message.size,
                                InetSocketAddress("localhost", 8888)
                            )
                            withContext(Dispatchers.IO) {
                                datagramSocket.send(datagram)
                            }
                            var sqlQ: String =
                                "UPDATE account SET money=money-${data.amount * price} WHERE id = '${accountData.id}'"
                            var pstmt = connection.prepareStatement(sqlQ)
                            pstmt.executeUpdate()
                            pstmt.close()
                            sqlQ = "UPDATE account SET stock=stock+${data.amount} WHERE id = '${accountData.id}'"
                            pstmt = connection.prepareStatement(sqlQ)
                            pstmt.executeUpdate()
                            pstmt.close()
                            call.sessions.set(
                                accountData.copy(
                                    money = accountData.money - data.amount * price,
                                    stock = accountData.stock + data.amount,
                                    inputMoney = accountData.inputMoney + data.amount * price,
                                    available = System.currentTimeMillis() + data.amount*30000
                                )
                            )
                            call.respondText("""{"success":1}""", ContentType.Application.Json)
                        } else {
                            call.respondText("""{"success":0}""", ContentType.Application.Json)
                        }
                    } catch (e: Exception) {
                        call.respondText("""{"success":0}""", ContentType.Application.Json)
                    }
                } else {
                    call.respondText("""{"success":0}""", ContentType.Application.Json)
                }
            }
        }
        post("/sell") {
            val dataString = call.receive<String>()
            val data: BuyPacket = Json.decodeFromString(dataString)
            val accountData = call.sessions.get<Session>()
            sendTo3000Port("Sell : $data ($accountData)\ntime : ${System.currentTimeMillis()}")
            if (accountData == null) {
                call.respondRedirect("/login")
            } else {
                if (accountData.available != null) {
                    if (accountData.available!! < System.currentTimeMillis()) {
                        accountData.available = null
                    }
                }
                if (accountData!!.available == null) {
                    try {
                        val connection = DriverManager.getConnection(
                            "jdbc:mysql://localhost/ilbanbest",
                            "ilban", "ilbanbest"
                        )
                        if (accountData.stock >= data.amount) {
                            val message = "sell,${data.amount},${accountData.id}".toByteArray()
                            val datagram = DatagramPacket(
                                message, message.size,
                                InetSocketAddress("localhost", 8888)
                            )
                            withContext(Dispatchers.IO) {
                                datagramSocket.send(datagram)
                            }
                            var sqlQ: String =
                                "UPDATE account SET money=money+${data.amount * price} WHERE id = '${accountData.id}'"
                            var pstmt = connection.prepareStatement(sqlQ)
                            pstmt.executeUpdate()
                            pstmt.close()
                            sqlQ = "UPDATE account SET stock=stock-${data.amount} WHERE id = '${accountData.id}'"
                            pstmt = connection.prepareStatement(sqlQ)
                            pstmt.executeUpdate()
                            pstmt.close()
                            call.sessions.set(
                                accountData.copy(
                                    money = accountData.money + data.amount * price,
                                    stock = accountData.stock - data.amount,
                                    outputMoney = accountData.outputMoney + data.amount * price,
                                    available = System.currentTimeMillis()+data.amount*30000
                                )
                            )
                            call.respondText("""{"success":1}""", ContentType.Application.Json)
                        } else {
                            call.respondText("""{"success":0}""", ContentType.Application.Json)
                        }
                    } catch (e: Exception) {
                        call.respondText("""{"success":0}""", ContentType.Application.Json)
                    }
                } else {
                    call.respondText("""{"success":0}""", ContentType.Application.Json)
                }
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
                val connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost/ilbanbest",
                    "ilban", "ilbanbest"
                )
                val statement = connection.createStatement(
                    ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE
                )
                val result =
                    statement.executeQuery("SELECT `id`, `pw`, `money`, `stock`, `input`, `output` FROM `account`")
                when (command[0] + command[1]) {
                    "creataccount" -> {
                        var tmp = true
                        while (result.next())
                            if (result.getString("id") == command[2]) tmp = false
                        if (tmp) {
                            val id = command[2]
                            val pw = command[3]
                            val money = command[4]
                            val sqlQ: String = "INSERT INTO `account` VALUES ('${id}', '${pw}', ${money}, 0, 0, 0)"
                            val pstmt = connection.prepareStatement(sqlQ)
                            pstmt.executeUpdate()
                            pstmt.close()
                            call.respondText("account created")
                        } else {
                            call.respondText("this id is already used")
                        }
                    }

                    "chargemoney" -> {
                        val id = command[2]
                        val money = command[3]
                        val sqlQ: String = "UPDATE `account` SET money=money+${money} WHERE id = '${id}'"
                        val pstmt = connection.prepareStatement(sqlQ)
                        pstmt.executeUpdate()
                        pstmt.close()
                        call.respondText("money updated")
                    }

                    "returnmoney" -> {
                        val id = command[2]
                        val money = command[3]
                        val sqlQ: String = "UPDATE `account` SET money=money-${money} WHERE id = '${id}'"
                        val pstmt = connection.prepareStatement(sqlQ)
                        pstmt.executeUpdate()
                        pstmt.close()
                        call.respondText("money updated")
                    }

                    "printmoney" -> {
                        val id = command[2]
                        var temp = true
                        while (result.next()) {
                            if (result.getString("id") == id) {
                                call.respondText("password : ${result.getString("money")}")
                                temp = false
                                break
                            }
                        }
                        if (temp) {
                            call.respondText("id가 존재하지 않음")
                        }
                    }

                    "printpassword" -> {
                        val id = command[2]
                        var temp = true
                        while (result.next()) {
                            if (result.getString("id") == id) {
                                call.respondText("password : ${result.getString("pw")}")
                                temp = false
                                break
                            }
                        }
                        if (temp) {
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
                call.respondText("뭔가 에러 있음\n" + e.message.toString())
            }
        }
        get("/admin") {
            call.respondText("어드민 페이지를 찾아낸 것은 축하한다. \n근데 왜 내가 어드민 페이지의 프론트엔드를 만들어 놓았을 거라고 생각하지? \n")
        }
        get("/login") {
            call.respondFile(File("src/main/resources/static/html/login.html"))
        }
        post("/login") {
            val dataString = call.receive<String>()
            val data: LoginPacket = Json.decodeFromString(dataString)
            val connection = DriverManager.getConnection(
                "jdbc:mysql://localhost/ilbanbest",
                "ilban", "ilbanbest"
            )
            val statement = connection.createStatement(
                ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE
            )
            val result = statement.executeQuery("SELECT `id`, `pw`, `money`, `stock`, `input`, `output` FROM `account`")
            while (result.next()) {
                if (result.getString("id") == data.id) {
                    if (result.getString("pw") == data.pw) {
                        call.sessions.set(
                            Session(
                                data.id,
                                result.getInt("money"),
                                result.getInt("stock"),
                                result.getInt("input"),
                                result.getInt("output"),
                                null        //쿠키 날리면 뚫리는 취약점
                            )
                        )
                        call.respondText("""{"login":1}""", ContentType.Application.Json)
                    } else {
                        call.respondText("""{"loginfail":1}""", ContentType.Application.Json)
                    }
                }
            }
            call.respondText("""{"loginfail":1}""", ContentType.Application.Json)
            result.close()
            connection.close()
            statement.close()
        }
        get("/now") {
            val accountData = call.sessions.get<Session>()
            val connection = DriverManager.getConnection(
                "jdbc:mysql://localhost/ilbanbest",
                "ilban", "ilbanbest"
            )
            val statement = connection.createStatement(
                ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE
            )
            var result = statement.executeQuery("SELECT `id`, `pw`, `money`, `stock`, `input`, `output` FROM `account`")
            while (result.next()) {
                if (result.getString("id") == accountData!!.id) {
                    call.sessions.set(
                        accountData.copy(money = result.getInt("money"))
                    )
                }
            }
            result = statement.executeQuery("SELECT * FROM `pricedb`")
            result.next()
            var text =
                "${result.getInt(1)},${result.getInt(2)},${result.getInt(3)},${result.getInt(4)},${result.getInt(5)},${
                    result.getInt(6)
                },${result.getInt(7)},${result.getInt(8)},${result.getInt(9)},${result.getInt(10)}"
            while (result.next()) {
                text += "/${result.getInt(1)},${result.getInt(2)},${result.getInt(3)},${result.getInt(4)},${
                    result.getInt(
                        5
                    )
                },${result.getInt(6)},${result.getInt(7)},${result.getInt(8)},${result.getInt(9)},${result.getInt(10)}"
            }

            result = statement.executeQuery("SELECT `price` FROM `nowprice`")
            while (result.next()) {
                call.respondText(
                    """
{
"price" : ${result.getInt("price")},
"pricedb" : "$text",
}
""".trimIndent(), ContentType.Application.Json
                )
            }
        }
        get("/onload") {
            var accountData = call.sessions.get<Session>()
            val connection = DriverManager.getConnection(
                "jdbc:mysql://localhost/ilbanbest",
                "ilban", "ilbanbest"
            )
            val statement = connection.createStatement(
                ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE
            )
            var result = statement.executeQuery("SELECT `id`, `pw`, `money`, `stock`, `input`, `output` FROM `account`")
            if (accountData != null) {
                while (result.next()) {
                    if (result.getString("id") == accountData!!.id) {
                        call.sessions.set(
                            accountData.copy(money = result.getInt("money"))
                        )
                        accountData = Session(
                            accountData.id,
                            accountData.money,
                            accountData.stock,
                            accountData.inputMoney,
                            accountData.outputMoney,
                            accountData.available
                        )
                    }
                }
            }
            result = statement.executeQuery("SELECT * FROM `pricedb`")
            result.next()
            var text =
                "${result.getInt(1)},${result.getInt(2)},${result.getInt(3)},${result.getInt(4)},${result.getInt(5)},${
                    result.getInt(6)
                },${result.getInt(7)},${result.getInt(8)},${result.getInt(9)},${result.getInt(10)}"
            while (result.next()) {
                text += "/${result.getInt(1)},${result.getInt(2)},${result.getInt(3)},${result.getInt(4)},${
                    result.getInt(
                        5
                    )
                },${result.getInt(6)},${result.getInt(7)},${result.getInt(8)},${result.getInt(9)},${result.getInt(10)}"
            }

            if (accountData == null) {
                call.respondText(
                    """
{
"id" : "0",
"money" : "0",
"stock" : "0",
"suic" : "0",
"now" : "${price}",
"pricedb" : "$text"
}
                    """.trimIndent(), ContentType.Application.Json
                )
            } else {
                if (accountData.inputMoney != 0) {
                    call.respondText(
                        """
{
"id" : "${accountData.id}",
"money" : "${accountData.money}",
"stock" : "${accountData.stock}",
"suic" : "${(accountData.outputMoney + price * accountData.stock - accountData.inputMoney).toDouble() / accountData.inputMoney * 100}",
"now" : "${price}",
"pricedb" : "$text"
}
                    """.trimIndent(), ContentType.Application.Json
                    )
                } else {
                    call.respondText(
                        """
{
"id" : "${accountData.id}",
"money" : "${accountData.money}",
"stock" : "${accountData.stock}",
"suic" : "0",
"now" : "${price}",
"pricedb" : "$text"
}
                    """.trimIndent(), ContentType.Application.Json
                    )
                }
            }
        }
    }
}