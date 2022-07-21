# board-game-set  
보드게임 set를 혼자 플레이할 수 있도록 만든 앱입니다

>이어하기 기능은 SharedPreference과 Gson을 사용  
>카드 애니메이션은 Android View Animations in Kotlin(https://github.com/gayanvoice/android-animations-kotlin) 사용  
>멀티플레이 기능은 Firebase - FireStore 사용  
>멀티플레이 중 동시 클릭 방지 기능은 FireStroe - Transaction 사용 -> 클릭 반응 지연 문제로 사용 중지  
>멀티플레이 중 강제 종료 대응은 Service(버전별 대응) - onTaskRemoved 사용  
>멀티플레이 중 다른화면 이동 대응은 NotificationChannel, NotificationCompat.Builder, PendingIntent(버전별 대응) 사용  
>진동 피드백은 (버전별 대응) VibratorManager, Vibrator 사용

시작 화면 이미지 lottie animation 출처 : https://lottiefiles.com/91317-card-game  
사용된 폰트 : Gmarket Sans / 배민 주아체
<br>
<br>
### changelog 
***ver 1.3***  
>UI 개선  
  
***ver 1.4***  
>기기의 화면 길이에 따라 화면이 자연스럽게 보이도록 수정

***ver 1.5***  
>남은 카드가 없는데 조합을 완성하면 강제 종료되던 버그 수정  
>남은 카드가 없을 때 조합을 완성하면 카드가 사라지게 함  
>남은 카드가 없을 때 가능한 조합이 없을 시 종료 다이얼로그 띄우게 수정

***ver 1.5.1***  
>불필요한 변수 제거 및 코드 축소  
  
***ver 1.6***  
>이어하기 기능 추가 (게임을 완료하거나 끝내기 버튼을 누르지 않으면 게임 자동 저장)

***ver 1.7***
>세트 플레이 알고리즘 최적화

***ver 1.8***
>멀티플레이 기능 추가(미완성)

***ver 2.0***
>멀티플레이 기능  
-> 진동 피드백 추가 및 게임 완료 후 방 자동 삭제 기능 추가

***ver 2.0.1***
>대기실이나 게임 중 앱을 강제종료 했을 때 대응  
>-> Service - onTaskRemoved 사용해서 처리  
>대기실이나 게임 중 화면 이동시 알림 생성 및 클릭시 앱으로 돌아오는 기능 추가  
>-> notification과 PendingIntent 기능 사용  
>선택된 카드 표시해주는 색상 변경

***ver 2.0.2***
>게임 접속 상태 기능 추가  
>-> 서비스에서 접속 상태 관리 + onStart, onStop에서 관리(커스텀 프래그먼트 클래스 추가)  
>서비스 기능 사용 중 다크모드, 화면 회전 등에서 에러 발생하는 문제 해결  
>-> 원인: 앱의 onStart, onStop이 빨리 바뀌어 서비스가 실행되기 전에 서비스를 중지하면 에러 발생  
>-> 해결: 서비스가 확실히 실행 중인지 확인후 서비스 중지 기능 실행
