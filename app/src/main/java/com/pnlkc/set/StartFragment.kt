package com.pnlkc.set

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.SetOptions
import com.pnlkc.set.data.DataSource
import com.pnlkc.set.data.DataSource.KEY_SHUFFLED_CARD_LIST
import com.pnlkc.set.data.UserMode
import com.pnlkc.set.databinding.StartFragmentBinding
import com.pnlkc.set.model.SetViewModel
import com.pnlkc.set.util.App

class StartFragment : Fragment() {

    private var _binding: StartFragmentBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SetViewModel by activityViewModels()

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var backPressCallback: OnBackPressedCallback

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = StartFragmentBinding.inflate(inflater, container, false)

        // OnBackPressedCallback (익명 클래스) 객체 생성
        backPressCallback = object : OnBackPressedCallback(true) {
            // 뒤로가기 했을 때 실행되는 기능
            var backWait: Long = 0
            override fun handleOnBackPressed() {
                if (System.currentTimeMillis() - backWait >= 2000) {
                    backWait = System.currentTimeMillis()
                    Toast.makeText(context, "뒤로가기 버튼을 한번 더 누르면 종료됩니다", Toast.LENGTH_SHORT).show()
                } else {
                    activity?.finish()
                }
            }
        }
        // 액티비티의 BackPressedDispatcher에 여기서 만든 callback 객체를 등록
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressCallback)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences =
            requireActivity().getSharedPreferences(DataSource.KEY_PREFS, Context.MODE_PRIVATE)

        // 로티 애니메이션 재생 길이 제한
        binding.lottieAnimationView.setMaxFrame(80)

        // 멀티플레이하기 버튼
        binding.multiGameBtn.setOnClickListener { showMultiDialog() }

        // 싱글플레이하기 버튼
        binding.singleGameBtn.setOnClickListener { showSingleDialog() }

        // ?(룰) 버튼튼
        binding.ruleBtn.setOnClickListener {
            findNavController().navigate(R.id.action_startFragment_to_setRuleFragment)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun showMultiDialog() {
        // 커스텀 Dialog 만들기
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_play_multi)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(true)
        dialog.show()

        // Dialog 레이아웃의 뷰를 변수와 연결하기
        // 그냥 연결이 안되서 dialog 변수를 따로 만들고 거기서 findViewById해서 찾음
        val dialogEditText1 =
            dialog.findViewById<TextView>(R.id.dialog_play_multi_nickname_edittext1)
        val dialogBtn1 = dialog.findViewById<TextView>(R.id.dialog_play_multi_btn1)
        val dialogEditText2 =
            dialog.findViewById<TextView>(R.id.dialog_play_multi_nickname_edittext2)
        val dialogBtn2 = dialog.findViewById<TextView>(R.id.dialog_play_multi_btn2)
        val roomCodeEditText =
            dialog.findViewById<EditText>(R.id.dialog_play_multi_room_code_edittext)

        // 게임 만들기
        dialogBtn1.setOnClickListener {
            sharedViewModel.userMode = UserMode.HOST

            if (dialogEditText1.visibility == View.GONE) {
                dialogEditText1.visibility = View.VISIBLE
                dialogBtn2.visibility = View.GONE
            } else {
                val nickname = dialogEditText1.text.toString()
                if (nickname.isNotBlank()) {
                    sharedViewModel.nickname = nickname
                    makeRoomCode()
                    dialog.dismiss()
                } else {
                    Toast.makeText(activity, "닉네임을 입력하세요", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 게임 참가하기
        dialogBtn2.setOnClickListener {
            sharedViewModel.userMode = UserMode.CLIENT

            if (dialogEditText2.visibility == View.GONE && roomCodeEditText.visibility == View.GONE) {
                dialogEditText2.visibility = View.VISIBLE
                roomCodeEditText.visibility = View.VISIBLE
                dialogBtn1.visibility = View.GONE
            } else {
                val nickname = dialogEditText2.text.toString()
                val roomCode = roomCodeEditText.text.toString().uppercase()

                if (nickname.isBlank() && roomCode.isBlank()) {
                    Toast.makeText(activity, "닉네임과 코드를 입력하세요", Toast.LENGTH_SHORT).show()
                } else if (nickname.isBlank()) {
                    Toast.makeText(activity, "닉네임을 입력하세요", Toast.LENGTH_SHORT).show()
                } else if (roomCode.isBlank()) {
                    Toast.makeText(activity, "코드를 입력하세요", Toast.LENGTH_SHORT).show()
                } else {
                    val collection = App.firestore.collection(roomCode)
                    collection.get()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                if (task.result.size() == 0) {
                                    Toast.makeText(activity, "코드를 확인해 주십시오", Toast.LENGTH_SHORT)
                                        .show()
                                } else {
                                    collection.document("user").get()
                                        .addOnSuccessListener { snapshot ->
                                            sharedViewModel.roomCode = roomCode
                                            val result: MutableList<String> =
                                                snapshot.data!!["user"] as MutableList<String>
                                            if (result.size < 4) {
                                                if (result.contains(nickname)) {
                                                    Toast.makeText(activity,
                                                        "이미 사용중인 닉네임입니다",
                                                        Toast.LENGTH_SHORT).show()
                                                } else {
                                                    if (snapshot.data!!["start"] == false) {
                                                        sharedViewModel.nickname = nickname
                                                        result.add(nickname)
                                                        val scoreList =
                                                            snapshot.data!!["score"] as MutableList<String>
                                                        scoreList.add("0")
                                                        collection.document("user")
                                                            .update(
                                                                "user", result,
                                                                "score", scoreList
                                                            )
                                                        val ready = hashMapOf(nickname to false)
                                                        collection.document("ready")
                                                            .set(ready, SetOptions.merge())
                                                        dialog.dismiss()
                                                        findNavController().navigate(R.id.action_startFragment_to_setMultiReadyFragment)
                                                    } else {
                                                        Toast.makeText(activity,
                                                            "게임이 이미 시작되어ㅜ 참가할 수 없습니다",
                                                            Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } else {
                                                Toast.makeText(activity,
                                                    "최대인원을 초과하여 참가할 수 없습니다",
                                                    Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                }
                            } else {
                                dialog.dismiss()
                                Log.d("로그", "데이터 로드 실패")
                            }
                        }
                }
            }
        }
    }

    private fun makeRoomCode() {
        val list = mutableListOf<Char>()
        list.addAll(('0'..'9').map { it })
        list.addAll(('A'..'Z').map { it })
        list.shuffle()
        var roomCode = ""
        repeat(6) { roomCode += list.random() }
        App.firestore.collection(roomCode).get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    if (it.result.size() > 0) {
                        makeRoomCode()
                    } else {
                        sharedViewModel.roomCode = roomCode
                        val data = hashMapOf(
                            "user" to mutableListOf(sharedViewModel.nickname),
                            "score" to mutableListOf("0"),
                            "start" to false
                        )
                        App.firestore.collection(roomCode).document("user")
                            .set(data)

                        val ready = hashMapOf(sharedViewModel.nickname to false)
                        App.firestore.collection(roomCode).document("ready").set(ready)

                        findNavController().navigate(R.id.action_startFragment_to_setMultiReadyFragment)
                    }
                } else {
                    Log.d("로그", "데이터 로드 실패")
                }
            }
    }

    private fun showSingleDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_play_single)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(true)
        dialog.show()

        val continueBtn = dialog.findViewById<Button>(R.id.dialog_play_single_continue_btn)
        val newGameBtn = dialog.findViewById<Button>(R.id.dialog_play_single_new_game_btn)

        if (sharedPreferences.contains(KEY_SHUFFLED_CARD_LIST)) {
            continueBtn.visibility = View.VISIBLE
            newGameBtn.text = "새로 시작하기"
        } else {
            continueBtn.visibility = View.GONE
            newGameBtn.text = "세트 플레이하기"
        }

        continueBtn.setOnClickListener {
            findNavController().navigate(R.id.action_startFragment_to_setSinglePlayFragment)
            sharedViewModel.isContinueGame = true
            dialog.dismiss()
        }

        newGameBtn.setOnClickListener {
            if (sharedPreferences.contains(KEY_SHUFFLED_CARD_LIST)) {
                showNewGameDialog()
            } else {
                findNavController().navigate(R.id.action_startFragment_to_setSinglePlayFragment)
                sharedViewModel.isContinueGame = false
            }
            dialog.dismiss()
        }
    }

    private fun showNewGameDialog() {
        // 커스텀 Dialog 만들기
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_new_game)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(true)
        dialog.show()

        // Dialog 레이아웃의 뷰를 변수와 연결하기
        // 그냥 연결이 안되서 dialog 변수를 따로 만들고 거기서 findViewById해서 찾음
        val leftBtn = dialog.findViewById<TextView>(R.id.dialog_new_game_left_btn)
        val rightBtn = dialog.findViewById<TextView>(R.id.dialog_new_game_right_btn)

        // Dialog 뷰 기능 구현
        leftBtn.setOnClickListener {
            dialog.dismiss()
            showSingleDialog()

        }
        rightBtn.setOnClickListener {
            findNavController().navigate(R.id.action_startFragment_to_setSinglePlayFragment)
            sharedViewModel.isContinueGame = false
            dialog.dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        backPressCallback.remove()
    }
}