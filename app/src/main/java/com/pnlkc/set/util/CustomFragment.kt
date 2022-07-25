package com.pnlkc.set.util

import androidx.fragment.app.Fragment

// 서비스 시작(onStop), 종료(onStart) 코드 추가된 프래그먼트
open class CustomFragment : Fragment() {
    open var isForcedExit = true

    // 앱이 (Re)Start 상태가 되면 온라인으로 설정
    override fun onStart() {
        super.onStart()
        isForcedExit = true
        if (App.isNicknameExist) setOnlineStatus()
    }

    // 유저 상태를 온라인으로 설정
    private fun setOnlineStatus() {
        if (App.checkAuth()) {
            App.firestore.collection("USER_LIST").document(App.auth.currentUser!!.uid)
                .update("status", "online")
        }
    }

    // 앱이 Stop 상태가 되면 오프라인으로 설정
    override fun onStop() {
        super.onStop()
        if (isForcedExit) setOfflineStatus()
    }

    // 유저 상태를 오프라인으로 설정
    private fun setOfflineStatus() {
        if (App.checkAuth()) {
            App.firestore.collection("USER_LIST").document(App.auth.currentUser!!.uid)
                .update("status", "offline")
        }
    }
}