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
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.firestore.SetOptions
import com.pnlkc.set.data.DataSource
import com.pnlkc.set.data.DataSource.KEY_SHUFFLED_CARD_LIST
import com.pnlkc.set.data.UserMode
import com.pnlkc.set.databinding.MainMenuFragmentBinding
import com.pnlkc.set.model.SetViewModel
import com.pnlkc.set.util.App
import com.pnlkc.set.util.App.Companion.isNicknameExist
import com.pnlkc.set.util.CustomFragment

class MainMenuFragment : CustomFragment() {

    private var _binding: MainMenuFragmentBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SetViewModel by activityViewModels()

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var backPressCallback: OnBackPressedCallback

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = MainMenuFragmentBinding.inflate(inflater, container, false)

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

       if (isNicknameExist) {
           binding.multiGameBtn.setOnClickListener { showMultiDialog() }
       } else {
           checkNicknameExist()
       }

        sharedPreferences =
            requireActivity().getSharedPreferences(DataSource.KEY_PREFS, Context.MODE_PRIVATE)

        // 로티 애니메이션 재생 길이 제한
        binding.lottieAnimationView.setMaxFrame(80)

        // 싱글플레이하기 버튼
        binding.singleGameBtn.setOnClickListener { showSingleDialog() }

        // ?(룰) 버튼튼
        binding.ruleBtn.setOnClickListener {
            isForcedExit = false
            findNavController().navigate(R.id.action_mainMenuFragment_to_setRuleFragment)
        }

        binding.settingBtn.setOnClickListener {
            logout()
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
        val makeRoomBtn = dialog.findViewById<TextView>(R.id.dialog_play_multi_make_room)
        val joinRoomBtn = dialog.findViewById<TextView>(R.id.dialog_play_multi_join_room)
        val roomCodeEditText =
            dialog.findViewById<EditText>(R.id.dialog_play_multi_room_code_edittext)

        // 게임 만들기
        makeRoomBtn.setOnClickListener {
            sharedViewModel.userMode = UserMode.HOST
            makeRoomCode()
            dialog.dismiss()
        }

        // 게임 참가하기
        joinRoomBtn.setOnClickListener {
            sharedViewModel.userMode = UserMode.CLIENT

            if (roomCodeEditText.visibility == View.GONE) {
                roomCodeEditText.visibility = View.VISIBLE
                makeRoomBtn.visibility = View.GONE
            } else {
                val roomCode = roomCodeEditText.text.toString().uppercase()

                if (roomCode.isBlank()) {
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
                                                if (snapshot.data!!["start"] == false) {
                                                    result.add(sharedViewModel.nickname)
                                                    val scoreList =
                                                        snapshot.data!!["score"] as MutableList<String>
                                                    scoreList.add("0")
                                                    collection.document("user")
                                                        .update(
                                                            "user", result,
                                                            "score", scoreList
                                                        )
                                                    val ready =
                                                        hashMapOf(sharedViewModel.nickname to false)
                                                    collection.document("ready")
                                                        .set(ready, SetOptions.merge())
                                                    dialog.dismiss()
                                                    isForcedExit = false
                                                    findNavController().navigate(R.id.action_mainMenuFragment_to_setMultiReadyFragment)
                                                } else {
                                                    Toast.makeText(activity,
                                                        "게임이 이미 시작되어 참가할 수 없습니다",
                                                        Toast.LENGTH_SHORT).show()
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

                        isForcedExit = false
                        findNavController().navigate(R.id.action_mainMenuFragment_to_setMultiReadyFragment)
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
            isForcedExit = false
            findNavController().navigate(R.id.action_mainMenuFragment_to_setSinglePlayFragment)
            sharedViewModel.isContinueGame = true
            dialog.dismiss()
        }

        newGameBtn.setOnClickListener {
            if (sharedPreferences.contains(KEY_SHUFFLED_CARD_LIST)) {
                showNewGameDialog()
            } else {
                isForcedExit = false
                findNavController().navigate(R.id.action_mainMenuFragment_to_setSinglePlayFragment)
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
            isForcedExit = false
            findNavController().navigate(R.id.action_mainMenuFragment_to_setSinglePlayFragment)
            sharedViewModel.isContinueGame = false
            dialog.dismiss()
        }
    }

    // 유저의 닉네임이 설정 되었는지 확인
    private fun checkNicknameExist() {
        App.firestore.collection("USER_LIST").document(App.auth.currentUser!!.uid).get()
            .addOnCompleteListener { snapshot ->
                if (snapshot.isSuccessful) {
                    if (snapshot.result.exists()) {
                        isNicknameExist = true
                        sharedViewModel.nickname = snapshot.result.data!!["nickname"].toString()
                        setOnlineStatus()
                    } else {
                        isNicknameExist = false
                    }
                    // 닉네임 유무 확인 후 멀티 버튼 활성화
                    binding.multiGameBtn.setOnClickListener {
                        if (isNicknameExist) showMultiDialog() else showDialogSetNickname()
                    }
                }
            }
    }

    // 닉네임 설정 Dialog 보여주기
    private fun showDialogSetNickname() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_set_nickname)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(true)
        dialog.show()

        // Dialog 레이아웃의 뷰를 변수와 연결하기
        // 그냥 연결이 안되서 dialog 변수를 따로 만들고 거기서 findViewById해서 찾음
        val nicknameEditText = dialog.findViewById<TextView>(R.id.dialog_set_nickname_edittext)
        val confirmBtn = dialog.findViewById<TextView>(R.id.dialog_set_nickname_confirm_btn)

        confirmBtn.setOnClickListener {
            val data = hashMapOf(
                "nickname" to nicknameEditText.text.toString(),
                "status" to true
            )
            App.firestore.collection("USER_LIST").document(App.auth.currentUser!!.uid)
                .set(data)
            isNicknameExist = true
            sharedViewModel.nickname = nicknameEditText.text.toString()
            dialog.dismiss()
            showMultiDialog()
        }
    }

    // 로그아웃 기능
    private fun logout() {
        // 구글 로그인인지 확인
        val providerId = App.auth.currentUser!!.providerData.last().providerId
        if (providerId.contains("google")) {
            // 구글 계정 로그아웃
            // 이 코드가 없으면 재로그인시 초기 로그인시 나오는 인텐트 팝업이 뜨지 않음
            GoogleSignIn.getClient(
                requireContext(),
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            ).signOut()
        }

        // 파이어베이스 로그아아웃
        App.auth.signOut()

        isNicknameExist = false
        isForcedExit = false
        findNavController().navigate(R.id.action_mainMenuFragment_to_loginFragment)
    }

    // 게임 접속시 상태를 온라인으로 설정
    private fun setOnlineStatus() {
        App.firestore.collection("USER_LIST").document(App.auth.currentUser!!.uid)
            .update("status", true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        backPressCallback.remove()
    }
}