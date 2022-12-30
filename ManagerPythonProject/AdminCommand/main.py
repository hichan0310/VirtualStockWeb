import requests

url="http://localhost:8080/admin"
print("This is admin page manager")
print("input admin command")
while True:
    print(">>>", end=' ')
    response = requests.post(
        url=url,
        json={
            "execute" : input()
        }
    )
    print(response)
    print(response.text)