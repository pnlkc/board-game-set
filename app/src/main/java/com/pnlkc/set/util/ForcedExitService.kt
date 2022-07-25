package com.pnlkc.set.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.pnlkc.set.MainActivity
import com.pnlkc.set.R
import kotlinx.coroutines.*

// 강제 종료했는지 확인하기 위한 서비스
class ForcedExitService : Service() {
    private var userList: Array<String>? = null
    private var nickname: String? = null
    private var roomCode: String? = null
    lateinit var collection: CollectionReference

    private val notificationId = 12
    private val channelId = "set_notification_channel"

    private var isDeleteDone = false
    private var isOfflineSetDone = false

    override fun onBind(p0: Intent?): IBinder? = null

    // 안드로이드 버전코드 O 이상 부터 Foreground 서비스 시작시 5초내에 알림과 연결해주어야 하는거 대응
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
            // 클릭시 원래 화면으로 돌아오게 하려면 아래 설정 필요
            val intent = Intent(this, MainActivity::class.java)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // 안드로이드 12 부터는 "or PendingIntent.FLAG_MUTABLE" 추가해야됨
            val pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

            val notification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentText("Set 앱이 실행 중입니다")
                .setContentIntent(pendingIntent)
                .build()

            startForeground(notificationId, notification)
        }
    }

    // 서비스가 시작 되면 인텐트로 정보 받아오기
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        App.isServiceRunning = true
        userList = intent.getStringArrayExtra("userList")
        nickname = intent.getStringExtra("nickname")
        roomCode = intent.getStringExtra("roomCode")
        return super.onStartCommand(intent, flags, startId)
    }

    // 앱이 강제 종료되었을 때
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (userList != null && nickname != null && roomCode != null) {
            collection = App.firestore.collection(roomCode!!)
            if (userList!!.size > 1) deletePlayer() else deleteCollection()
            setOfflineStatus()
        }
    }

    // 본인에 해당하는 정보 삭제
    @Suppress("UNCHECKED_CAST")
    private fun deletePlayer() {
        val index = userList!!.indexOf(nickname)
        collection.document("user").get().addOnSuccessListener { snapshot ->
            val userList = snapshot.data!!["user"] as MutableList<String>
            val scoreList = snapshot.data!!["score"] as MutableList<String>
            userList.removeAt(index)
            scoreList.removeAt(index)
            App.firestore.runBatch { batch ->
                batch.update(collection.document("user"), "user", userList)
                batch.update(collection.document("user"), "score", scoreList)
                batch.update(collection.document("ready"),
                    nickname!!,
                    FieldValue.delete())
            }.addOnSuccessListener {
                isDeleteDone = true
                if (isDeleteDone && isOfflineSetDone) stopSelf()
            }
        }
    }

    // 본인이 마지막 유저일시 방(collection) 삭제
    private fun deleteCollection() {
        collection.get().addOnSuccessListener { querySnapshot ->
            var count = 0
            querySnapshot.forEach { snapshot ->
                snapshot.reference.delete()
                    .addOnSuccessListener {
                        count++
                        // 마지막 작업까지 끝나면 서비스 중지
                        if (count == querySnapshot.size()) {
                            isDeleteDone = true
                            if (isDeleteDone && isOfflineSetDone) stopSelf()
                        }
                    }
            }

        }
    }

    // 앱 종료시 상태를 오프라인으로 변경
    private fun setOfflineStatus() {
        if (App.checkAuth()) {
            App.firestore.collection("USER_LIST").document(App.auth.currentUser!!.uid)
                .update("status", "offline").addOnSuccessListener {
                    isOfflineSetDone = true
                    if (isDeleteDone && isOfflineSetDone) stopSelf()
                }
        }
    }

    // notification channel 만드는 기능
    private fun createNotificationChannel() {
        val notificationChannel = NotificationChannel(
            channelId,
            "Set Notification",
            NotificationManager.IMPORTANCE_MIN
        )
        notificationChannel.enableLights(false)
        notificationChannel.enableVibration(false)
        notificationChannel.description = "Set앱 백그라운드 실행 중 안내 알림"

        val notificationManager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
    }

    override fun onDestroy() {
        super.onDestroy()
        App.isServiceRunning = false
    }
}