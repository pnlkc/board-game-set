package com.pnlkc.set.util

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.pnlkc.set.data.UserMode
import com.pnlkc.set.databinding.DialogConfirmDeleteFriendBinding
import com.pnlkc.set.databinding.DialogInvitedBinding
import com.pnlkc.set.model.SetViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// 서비스 시작(onStop), 종료(onStart) 코드 추가된 프래그먼트
open class CustomFragment : Fragment() {
    open var isForcedExit = true
    private lateinit var inviteListener: ListenerRegistration

    val sharedViewModel: SetViewModel by activityViewModels()

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        inviteListener()
    }

    private fun inviteListener() {
        inviteListener = App.firestore.collection("USER_LIST").document(App.auth.currentUser!!.uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener

                if (snapshot.data!!["invite_nickname"] != null && snapshot.data!!["invite_roomCode"] != null) {
                    val nickname = snapshot.data!!["invite_nickname"] as String
                    val roomCode = snapshot.data!!["invite_roomCode"] as String
                    showDialogInvited(nickname, roomCode)
                }
            }
    }

    private fun showDialogInvited(nickname: String, roomCode: String) {
        val inflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dBinding = DialogInvitedBinding.inflate(inflater)

        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(dBinding.root)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(true)
        dialog.show()

        dBinding.dialogInvitedTextview.text = "\"${nickname}\"님께서\n게임에 초대하셨습니다"

        dBinding.dialogInvitedPositiveBtn.setOnClickListener {
            joinRoom(dialog, roomCode)
        }

        dBinding.dialogInvitedNegativeBtn.setOnClickListener {
            declineInvite()
            dialog.dismiss()
        }

        dialog.setOnCancelListener {
            declineInvite()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun joinRoom(dialog: Dialog, roomCode: String) {
        sharedViewModel.userMode = UserMode.CLIENT
        val collection = App.firestore.collection(roomCode)
        collection.get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (task.result.size() == 0) {
                        Toast.makeText(activity, "초대된 방이 존재하지 않습니다", Toast.LENGTH_SHORT)
                            .show()
                        dialog.dismiss()
                    } else {
                        collection.document("user").get()
                            .addOnSuccessListener { snapshot ->
                                sharedViewModel.roomCode = roomCode
                                val result: MutableList<String> =
                                    snapshot.data!!["user"] as MutableList<String>

                                if (result.size < 4) {
                                    if (snapshot.data!!["start"] == false) {
                                        App.firestore.runBatch { batch ->
                                            result.add(sharedViewModel.nickname!!)

                                            val scoreList =
                                                snapshot.data!!["score"] as MutableList<String>
                                            scoreList.add("0")

                                            val ready =
                                                hashMapOf(sharedViewModel.nickname to false)

                                            batch.update(
                                                collection.document("user"),
                                                "user", result,
                                                "score", scoreList
                                            )

                                            batch.set(
                                                collection.document("ready"),
                                                ready,
                                                SetOptions.merge()
                                            )

                                            batch.update(
                                                App.firestore.collection("USER_LIST")
                                                    .document(App.auth.currentUser!!.uid),
                                                "invite_nickname", FieldValue.delete(),
                                                "invite_roomCode", FieldValue.delete()
                                            )
                                        }.addOnSuccessListener {
                                            dialog.dismiss()
                                            isForcedExit = false
                                            acceptInviteMoveFragment()
                                        }
                                    } else {
                                        Toast.makeText(activity,
                                            "게임이 이미 시작되어 참가할 수 없습니다",
                                            Toast.LENGTH_SHORT).show()
                                        dialog.dismiss()
                                    }
                                } else {
                                    Toast.makeText(activity,
                                        "최대인원을 초과하여 참가할 수 없습니다",
                                        Toast.LENGTH_SHORT).show()
                                    dialog.dismiss()
                                }
                            }
                    }
                } else {
                    dialog.dismiss()
                    Log.d("로그", "데이터 로드 실패")
                }
            }
    }

    private fun declineInvite() {
        CoroutineScope(Dispatchers.IO).launch {
            App.firestore.collection("USER_LIST").document(App.auth.currentUser!!.uid)
                .update(
                    "invite_nickname", FieldValue.delete(),
                    "invite_roomCode", FieldValue.delete())
        }
    }

    // 각 프래그먼트에서 대기실로 이동하도록 설정해야 됨
    open fun acceptInviteMoveFragment() {

    }

    override fun onDestroyView() {
        super.onDestroyView()
        inviteListener.remove()
    }
}