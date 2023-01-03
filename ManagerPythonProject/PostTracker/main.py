import socket

localIP = "localhost"
localPort = 3000
bufferSize = 10000

# 데이터그램 소켓을 생성
UDPServerSocket = socket.socket(family=socket.AF_INET, type=socket.SOCK_DGRAM)

# 주소와 IP로 Bind
UDPServerSocket.bind((localIP, localPort))

print("print all http post")

# 들어오는 데이터그램 Listen
while (True):
    bytesAddressPair = UDPServerSocket.recvfrom(bufferSize)
    message = bytesAddressPair[0]
    address = bytesAddressPair[1]

    clientMsg = str(message, "utf-8")

    print(clientMsg)
    print()
