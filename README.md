# VirtualStockWeb
1-1 부스 운영을 위한 웹 프로젝트  
ktor을 사용하여 제작된 웹  
다른 환경에서 어떻게 빌드하는지는 몰?루  
지민선배한테 가면 될지도  (+ 유민선배)  
html 파일은 /src/main/resource/static/html에 있음  
html 파일에 있는 경로들도 많이 변경되었으니 잘 보고 하면 됨  
  
  
명령어들 모음
1. VirtualJusic
   1. help
      + 도움말을 줍니다. 
      + 근데 업데이트 안 한지 꽤 돼서 내용이 틀릴 수 있습니다. 
   2. print
      + print history all
        + 모든 사람들의 히스토리를 출력합니다. 
        + 히스토리는 간단히 몇개 사고 몇개 팔았는지 정도만 있습니다.
      + print history [id]
        + 특정 인물의 히스토리를 더 자세히 볼 수 있습니다. 
        + 언제 얼마나 샀는지 볼 수 있습니다.
      + print allCoins
        + 시장에 풀린 코인량을 출력합니다. 
      + print price
        + 가격을 출력합니다.
      + print buySpeed
        + 순간구매량을 출력합니다.
      + print sellSpeed
        + 순간판매량을 출력합니다. 
        + 이 2개의 값을 잘 보고 가격이 줄어들게 할지, 늘어나게 할지 결정해야 합니다. 
        + 가격이 너무 늘어난다면 이 2개의 값을 보고 buySpeed/sellSpeed 값보다 조금 높게 set parameter buySellRate 명령어를 실행시키세요. 
      + print revenue
        + 우리가 너무 큰 수익이나 너무 큰 손실을 보지 않도록 합시다.
      + print parameters
        + 파라미터를 모두 출력합니다. 
        + 무슨 파라미터가 있는지는 츨력헤보고 확인하세요. 
   3. set
      + set parameters
        + buyDelay
          + 사는 것이 가격에 반영되는 지연시간입니다. 
          + 가격이 높아지면 이 값을 높게 설정할 수 있습니다. 
        + sellDelay
          + 파는 것이 가격에 반영되는 지연시간입니다. 
          + 가격이 낮아지면 이 값을 낮게 설정할 수 있습니다. 
        + buyLife
          + 가격에 반영되는 구매 행동이 남아있는 수명입니다. 
          + 가격이 높아지면 이 값을 낮게 설정할 수 있습니다. 
        + sellLife
          + 가격에 반영되는 판매 행동이 남아있는 수명입니다. 
          + 가격이 높아지면 이 값을 높게 설정할 수 있습니다. 
        + limitSijang
          + 이건 진짜 조심해야 하는데 수시로 allCoins 확인해서 limitSijang 조절해야 합니다. 
          + 이 값을 allCoins가 넘어가면 큰일납니다. 
        + buySellRate
          + 이건 우리가 사기를 칠 수도 있고 사기친다고 오해받기도 쉬운 파라미터입니다. 
          + buySpeed, sellSpeed를 이용해서 잘 조절합시다. 
        + updateRate
          + 이건 걍 업데이트 변화량이 이 값으로 나누어져서 적용됩니다. 
          + 변화 속도가 너무 빠를 때 높일 수 있습니다. 
      + price
        + 금단의 명령어
        + 가격을 그냥 조절해버릴 수 있습니다. 
      + buySpeed
        + 조절은 가능한데 잘못해서 기본값이 0이 아니게 될 수 있으니 주의
      + sellSpeed
        + 조절은 가능한데 잘못해서 기본값이 0이 아니게 될 수 있으니 주의  
2. AdminManager
   1. creat account [id] [pw] [money]
      + 계정을 만들어줍니다. 
   2. charge money [id] [money]
      + 계정에 돈을 충전합니다. 
   3. return money [id] [money]
      + 계정에서 돈을 빼줍니다. 
      + 해킹한 사람의 돈을 음수로 만드는 용도로도 사용될 예정입니다. 
      + 정상적인 상황에서는 print money를 사용한 다음 실행합시다. 
   4. print money [id]
      + 계정의 돈을 출력합니다. 
   5. print password [id]
      + 계정의 비밀번호를 출력합니다. 
      + 잊어버린 비밀번호 찾기가 가능합니다. 
