package com.pnlkc.set.util

import android.content.Context
import androidx.multidex.MultiDexApplication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

// 사용하려면 AndroidManifest 파일 <application>에 android:name=".App" 을 추가해야됨
class App : MultiDexApplication() {

    companion object {
        // 전역 context 사용가능하도록 하는 클래스
        lateinit var instance: App
            private set

        fun context(): Context {
            return instance.applicationContext
        }

        // 서비스(ForcedExitService)가 실행중인지 확인하기 위한 변수
        var isServiceRunning = false

        // 닉네임이 설정 되었는지 확인하는 변수
        var isNicknameExist = false

        // FirebaseAuth 객체와 관련된 변수를 전역으로 사용하기 위해 필요
        lateinit var auth: FirebaseAuth
        lateinit var firestore: FirebaseFirestore

        // 현재 로그인 되어 있는지 확인인
        fun checkAuth(): Boolean {
            // 현재 유저를 정보를 나타내는 변수
            val currentUser = auth.currentUser
            return currentUser != null
        }
    }

    override fun onCreate() {
        super.onCreate()
        // App 클래스가 호출되면 초기화
        instance = this
        auth = Firebase.auth
        firestore = FirebaseFirestore.getInstance()
    }
}