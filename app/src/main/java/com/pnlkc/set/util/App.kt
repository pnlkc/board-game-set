package com.pnlkc.set.util

import android.content.Context
import androidx.multidex.MultiDexApplication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
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

        // FirebaseAuth 객체와 관련된 변수를 전역으로 사용하기 위해 필요
        lateinit var auth: FirebaseAuth
        lateinit var firestore: FirebaseFirestore

        // 로그인 한 사람의 email을 담을 변수
        var email: String? = null

        // 이메일 인증 여부 확인 및 유저 이메일 값 저장
        fun checkAuth(): Boolean {
            // 현재 유저를 정보를 나타내는 변수
            val currentUser = auth.currentUser

            return currentUser?.let {
                // email 변수에 현재 로그인된 유저의 email을 설정
                email = currentUser.email

                // 이메일 인증이 되었는지 여부에 따라 true, false 반환
                currentUser.isEmailVerified
            } ?: let {
                email = null
                // current 유저가 없으면(null) false 반환
                false
            }
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