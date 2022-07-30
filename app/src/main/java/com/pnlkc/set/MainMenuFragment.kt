@file:Suppress("OPT_IN_IS_NOT_ENABLED")

package com.pnlkc.set

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.pnlkc.set.data.DataSource
import com.pnlkc.set.data.DataSource.KEY_SHUFFLED_CARD_LIST
import com.pnlkc.set.data.UserMode
import com.pnlkc.set.databinding.DialogConfirmDeleteFriendBinding
import com.pnlkc.set.databinding.DialogFriendBinding
import com.pnlkc.set.databinding.DialogMyProfileBinding
import com.pnlkc.set.databinding.MainMenuFragmentBinding
import com.pnlkc.set.model.Friend
import com.pnlkc.set.recyclerview_friend_list.FriendListAdaptor
import com.pnlkc.set.recyclerview_friend_list.IFriendList
import com.pnlkc.set.recyclerview_friend_request_list.FriendRequestListAdaptor
import com.pnlkc.set.recyclerview_friend_request_list.IFriendRequestList
import com.pnlkc.set.util.App
import com.pnlkc.set.util.App.Companion.isNicknameExist
import com.pnlkc.set.util.CustomFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MainMenuFragment : CustomFragment(), IFriendList, IFriendRequestList {

    private var _binding: MainMenuFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var backPressCallback: OnBackPressedCallback

    // 아래 두 변수는 클래스 내에서 자유롭게 사용하기 위해 최상단에서 선언
    // connectGoogleAccount()에서 사용
    private lateinit var gsoLauncher: ActivityResultLauncher<Intent>

    // onViewCreated() - gsoLauncher에서 Dialog의 버튼의 visibility 설정하기 위해 사용
    private lateinit var connectGoogleAccountBtn: Button

    private lateinit var collection: CollectionReference

    private lateinit var friendSnapshotListener: ListenerRegistration
    private var friendStatusSnapshotListener: ListenerRegistration? = null

    private var friendRequestList: List<String> = listOf()
    private var friendList: List<String> = listOf()
    private var resultList = mutableListOf<Friend>()

    private val friendRequestListAdaptor = FriendRequestListAdaptor(this)
    private lateinit var friendListAdaptor: FriendListAdaptor

    private val friendCount = MutableLiveData(arrayOf(0, 0))

    private var dialogFriend: Dialog? = null

    private var myJob = Job()
    private var searchTerm = ""

    private var invitedNickname: String? = null

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
                    Toast.makeText(context,
                        getString(R.string.back_btn_twice),
                        Toast.LENGTH_SHORT).show()
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

        friendListAdaptor = FriendListAdaptor(this, requireContext())

        collection = App.firestore.collection("USER_LIST")

        checkNicknameExist()

        observeFriendChange()

        sharedPreferences =
            requireActivity().getSharedPreferences(DataSource.KEY_PREFS, Context.MODE_PRIVATE)

        // 로티 애니메이션 재생 길이 제한
        binding.lottieAnimationView.setMaxFrame(80)

        // 싱글플레이하기 버튼
        binding.singleGameBtn.setOnClickListener { showDialogPlaySingle() }

        // ?(룰) 버튼 기능
        binding.ruleBtn.setOnClickListener {
            isForcedExit = false
            findNavController().navigate(R.id.action_mainMenuFragment_to_setRuleFragment)
        }

        // 설정 버튼 기능
        binding.settingBtn.setOnClickListener {
            showDialogSetting()
        }

        // 익명 로그인 구글 계정 전환
        gsoLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                // 구글 로그인 결과 처리
                if (it.resultCode == Activity.RESULT_OK) {
                    val result = GoogleSignIn.getSignedInAccountFromIntent(it.data)
                    try {
                        val account = result.result
                        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                        App.auth.currentUser!!.linkWithCredential(credential)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(requireContext(),
                                        getString(R.string.connection_successful),
                                        Toast.LENGTH_SHORT).show()
                                    connectGoogleAccountBtn.visibility = View.GONE
                                } else {
                                    Toast.makeText(requireContext(),
                                        getString(R.string.connection_failure),
                                        Toast.LENGTH_SHORT).show()
                                }
                            }
                    } catch (e: ApiException) {
                        Toast.makeText(requireContext(),
                            getString(R.string.connection_failure),
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    @Suppress("UNCHECKED_CAST")
    private fun showDialogPlayMulti() {
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
            makeRoomCode("makeRoomBtn")
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
                    Toast.makeText(activity,
                        getString(R.string.enter_code),
                        Toast.LENGTH_SHORT).show()
                } else {
                    val collection = App.firestore.collection(roomCode)
                    collection.get()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                if (task.result.size() == 0) {
                                    Toast.makeText(activity,
                                        getString(R.string.check_code),
                                        Toast.LENGTH_SHORT)
                                        .show()
                                } else {
                                    collection.document("user").get()
                                        .addOnSuccessListener { snapshot ->
                                            sharedViewModel.roomCode = roomCode
                                            val result: MutableList<String> =
                                                snapshot.data!!["user"] as MutableList<String>

                                            if (result.size < 4) {
                                                if (snapshot.data!!["start"] == false) {
                                                    result.add(sharedViewModel.nickname!!)
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
                                                        getString(R.string.already_start),
                                                        Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(activity,
                                                    getString(R.string.full_room),
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

    private fun makeRoomCode(mode: String) {
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
                        makeRoomCode(mode)
                    } else {
                        sharedViewModel.roomCode = roomCode

                        App.firestore.runBatch { batch ->
                            val data = hashMapOf(
                                "user" to mutableListOf(sharedViewModel.nickname),
                                "score" to mutableListOf("0"),
                                "start" to false
                            )

                            batch.set(
                                App.firestore.collection(roomCode).document("user"),
                                data
                            )

                            val ready = hashMapOf(sharedViewModel.nickname to false)

                            batch.set(
                                App.firestore.collection(roomCode).document("ready"),
                                ready
                            )
                        }.addOnSuccessListener {
                            isForcedExit = false
                            if (mode == "invite") inviteFriendAction()
                            findNavController().navigate(R.id.action_mainMenuFragment_to_setMultiReadyFragment)
                            dialogFriend?.dismiss()
                        }
                    }
                } else {
                    Log.d("로그", "데이터 로드 실패")
                }
            }
    }

    private fun showDialogPlaySingle() {
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
            newGameBtn.text = getString(R.string.new_game)
        } else {
            continueBtn.visibility = View.GONE
            newGameBtn.text = getString(R.string.play_set)
        }

        continueBtn.setOnClickListener {
            isForcedExit = false
            findNavController().navigate(R.id.action_mainMenuFragment_to_setSinglePlayFragment)
            sharedViewModel.isContinueGame = true
            dialog.dismiss()
        }

        newGameBtn.setOnClickListener {
            if (sharedPreferences.contains(KEY_SHUFFLED_CARD_LIST)) {
                showDialogNewGame()
            } else {
                isForcedExit = false
                findNavController().navigate(R.id.action_mainMenuFragment_to_setSinglePlayFragment)
                sharedViewModel.isContinueGame = false
            }
            dialog.dismiss()
        }
    }

    private fun showDialogNewGame() {
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
            showDialogPlaySingle()
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
        collection.document(App.auth.currentUser!!.uid).get()
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
                        if (isNicknameExist) showDialogPlayMulti() else showDialogSetNickname("multi")
                    }

                    // 닉네임 유무 확인 후 친구창 버튼 활성화
                    binding.friendBtn.setOnClickListener {
                        if (isNicknameExist) showDialogFriend() else showDialogSetNickname("friend")
                    }
                }
            }
    }

    // 닉네임 설정 Dialog 보여주기
    private fun showDialogSetNickname(mode: String) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_set_nickname)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(true)
        dialog.show()

        // Dialog 레이아웃의 뷰를 변수와 연결하기
        // 그냥 연결이 안되서 dialog 변수를 따로 만들고 거기서 findViewById해서 찾음
        val nicknameEditText = dialog.findViewById<EditText>(R.id.dialog_set_nickname_edittext)
        val confirmBtn = dialog.findViewById<Button>(R.id.dialog_set_nickname_confirm_btn)

        confirmBtn.setOnClickListener {
            val inputText = nicknameEditText.text.toString()
            if (inputText.isNotBlank()) {
                collection.whereEqualTo("nickname", inputText)
                    .get().addOnSuccessListener { snapshot ->
                        if (snapshot.isEmpty) {
                            val oldNickname = sharedViewModel.nickname ?: ""
                            val data = hashMapOf(
                                "nickname" to inputText,
                                "status" to "online"
                            )
                            collection.document(App.auth.currentUser!!.uid)
                                .set(data, SetOptions.merge())
                            Toast.makeText(requireContext(),
                                getString(R.string.set_nickname_finish),
                                Toast.LENGTH_SHORT)
                                .show()
                            isNicknameExist = true
                            sharedViewModel.nickname = inputText
                            dialog.dismiss()

                            if (mode == "myProfile" && oldNickname.isNotEmpty()) {
                                collection.whereArrayContains("friend_list", oldNickname).get()
                                    .addOnCompleteListener { documents ->
                                        if (!(documents.result.isEmpty)) {
                                            App.firestore.runBatch { batch ->
                                                for (document in documents.result) {
                                                    val uid = document.id
                                                    batch.update(
                                                        collection.document(uid),
                                                        "friend_list",
                                                        FieldValue.arrayRemove(oldNickname)
                                                    )

                                                    batch.update(
                                                        collection.document(uid),
                                                        "friend_list",
                                                        FieldValue.arrayUnion(sharedViewModel.nickname)
                                                    )
                                                }
                                            }
                                        }
                                    }
                            }

                            when (mode) {
                                "multi" -> showDialogPlayMulti()
                                "myProfile" -> showDialogMyProfile()
                                "friend" -> showDialogFriend()
                            }
                        } else {
                            if (inputText == sharedViewModel.nickname) {
                                Toast.makeText(requireContext(),
                                    getString(R.string.set_nickname_different),
                                    Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                Toast.makeText(requireContext(),
                                    getString(R.string.set_nickname_used),
                                    Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }
            } else {
                Toast.makeText(requireContext(),
                    getString(R.string.set_nickname_blank),
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 설정 다이얼로그 보여주기
    private fun showDialogSetting() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_setting)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(true)
        dialog.show()

        // Dialog 레이아웃의 뷰를 변수와 연결하기
        // 그냥 연결이 안되서 dialog 변수를 따로 만들고 거기서 findViewById해서 찾음
        val myProfileBtn = dialog.findViewById<Button>(R.id.dialog_setting_my_profile)
        connectGoogleAccountBtn =
            dialog.findViewById(R.id.dialog_setting_connect_google_account)
        val logoutBtn = dialog.findViewById<Button>(R.id.dialog_setting_logout)

        // 내 프로필 버튼
        myProfileBtn.setOnClickListener {
            showDialogMyProfile()
            dialog.dismiss()
        }

        // 구글 로그인인지 확인
        val providerId = App.auth.currentUser!!.providerData.last().providerId
        if (!providerId.contains("google")) {
            connectGoogleAccountBtn.visibility = View.VISIBLE
            connectGoogleAccountBtn.setOnClickListener {
                connectGoogleAccount()
            }
        }

        // 로그아웃 버튼
        logoutBtn.setOnClickListener {
            dialog.dismiss()
            logout()
        }
    }

    // 내 프로필 다이얼로그 보여주기
    private fun showDialogMyProfile() {
        // 뷰바인딩 사용
        val inflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dBinding = DialogMyProfileBinding.inflate(inflater)

        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(dBinding.root)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(true)
        dialog.show()

        if (sharedViewModel.nickname != null) {
            dBinding.dialogMyProfileNicknameTextview.text = sharedViewModel.nickname
            dBinding.dialogMyProfileNicknameChangeBtn.setImageResource(R.drawable.nickname_change_icon)
        } else {
            dBinding.dialogMyProfileNicknameTextview.hint =
                getString(R.string.empty_nickname)
            dBinding.dialogMyProfileNicknameChangeBtn.setImageResource(R.drawable.nickname_set_icon)
        }

        dBinding.dialogMyProfileNicknameLinearlayout.setOnClickListener {
            dialog.dismiss()
            showDialogSetNickname("myProfile")
        }

        dBinding.dialogMyProfileBackBtn.setOnClickListener {
            dialog.dismiss()
            showDialogSetting()
        }
    }

    // 익명 계정을 구글 계정으로 전환
    private fun connectGoogleAccount() {
        val gso = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            //R.string.default_web_client_id 에러시 project 수준의 classpath ...google-services 버전 확인
            .requestIdToken(getString(R.string.default_web_client_id))
            .build()
        val signInIntent = GoogleSignIn.getClient(requireContext(), gso).signInIntent
        gsoLauncher.launch(signInIntent)
    }

    // 로그아웃 기능
    private fun logout() {
        // 유저 상태를 오프라인으로 변경
        collection.document(App.auth.currentUser!!.uid)
            .update("status", "offline")

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

        sharedViewModel.nickname = null
        isNicknameExist = false
        isForcedExit = false
        findNavController().navigate(R.id.action_mainMenuFragment_to_loginFragment)
    }

    // 친구창 다이얼로그 보여주기
    @SuppressLint("SetTextI18n")
    private fun showDialogFriend() {
        // 뷰바인딩 사용
        val inflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dBinding = DialogFriendBinding.inflate(inflater)

        dialogFriend = Dialog(requireContext())
        dialogFriend!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogFriend!!.setContentView(dBinding.root)
        dialogFriend!!.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        dialogFriend!!.setCancelable(true)
        dialogFriend!!.show()

        dBinding.dialogFriendRequestListRecyclerview.adapter = friendRequestListAdaptor
        dBinding.dialogFriendRequestListRecyclerview.layoutManager =
            LinearLayoutManager(requireContext())

        dBinding.dialogFriendListRecyclerview.adapter = friendListAdaptor
        dBinding.dialogFriendListRecyclerview.layoutManager = LinearLayoutManager(requireContext())

        dBinding.dialogFriendBackBtn.setOnClickListener {
            dialogFriend!!.dismiss()
        }

        dBinding.dialogFriendAddFriendBtn.setOnClickListener {
            dBinding.dialogFriendAddFriendBtn.visibility = View.GONE
            dBinding.dialogFriendFilterLinearlayout.visibility = View.GONE
            dBinding.dialogFriendAddFriendCancelBtn.visibility = View.VISIBLE
            dBinding.dialogFriendSendLinearlayout.visibility = View.VISIBLE
            dBinding.dialogFriendFilterEdittext.text?.clear()
        }

        dBinding.dialogFriendAddFriendCancelBtn.setOnClickListener {
            dBinding.dialogFriendAddFriendBtn.visibility = View.VISIBLE
            dBinding.dialogFriendFilterLinearlayout.visibility = View.VISIBLE
            dBinding.dialogFriendAddFriendCancelBtn.visibility = View.GONE
            dBinding.dialogFriendSendLinearlayout.visibility = View.GONE
            dBinding.dialogFriendSendEdittext.text?.clear()
        }

        dBinding.dialogFriendFilterCancelBtn.setOnClickListener {
            dBinding.dialogFriendFilterEdittext.text!!.clear()
        }

        dBinding.dialogFriendSendBtn.setOnClickListener {
            val inputText = dBinding.dialogFriendSendEdittext.text.toString()
            if (inputText.isNotBlank()) {
                sendFriendRequest(inputText, dBinding.dialogFriendSendEdittext)
            }
        }

        dBinding.dialogFriendRequestTitleLinearlayout.setOnClickListener {
            if (dBinding.dialogFriendRequestListRecyclerview.visibility == View.VISIBLE) {
                dBinding.dialogFriendRequestFoldImageview.setBackgroundResource(R.drawable.friend_fold_icon)
                dBinding.dialogFriendRequestListRecyclerview.visibility = View.GONE
            } else {
                dBinding.dialogFriendRequestFoldImageview.setBackgroundResource(R.drawable.friend_unfold_icon)
                dBinding.dialogFriendRequestListRecyclerview.visibility = View.VISIBLE
            }
        }

        dBinding.dialogFriendStatusTitleLinearlayout.setOnClickListener {
            if (dBinding.dialogFriendListRecyclerview.visibility == View.VISIBLE) {
                dBinding.dialogFriendStatusFoldImageview.setBackgroundResource(R.drawable.friend_fold_icon)
                dBinding.dialogFriendListRecyclerview.visibility = View.GONE
            } else {
                dBinding.dialogFriendStatusFoldImageview.setBackgroundResource(R.drawable.friend_unfold_icon)
                dBinding.dialogFriendListRecyclerview.visibility = View.VISIBLE
            }
        }

        dBinding.dialogFriendFilterCancelBtn.setOnClickListener {
            dBinding.dialogFriendFilterEdittext.text!!.clear()
        }

        friendCount.observe(viewLifecycleOwner) {
            dBinding.dialogFriendStatusTextview.text =
                getString(R.string.friend_status) + it[0] + "/" + it[1] + ")"
        }

        // 친구 검색시 검색어 변경이 0.35초 동안 없을시 검색어를 searchTerm 변수에 저장
        CoroutineScope(Dispatchers.Main + myJob).launch {
            val editTextFlow = dBinding.dialogFriendFilterEdittext.textChangesToFlow()
            editTextFlow
                .onEach { text ->
                    if (text!!.isNotEmpty()) {
                        dBinding.dialogFriendFilterCancelBtn.visibility = View.VISIBLE
                        dBinding.dialogFriendFilterBtn.visibility = View.GONE
                    } else {
                        dBinding.dialogFriendFilterCancelBtn.visibility = View.GONE
                        dBinding.dialogFriendFilterBtn.visibility = View.VISIBLE
                    }
                }
                .debounce(350)
                .onEach { text ->
                    searchTerm = text?.toString() ?: ""
                    setDataFriendListAdapter()
                }
                .launchIn(this)
        }
    }

    // 친구 목록 리사이클러뷰 리스트 설정
    private fun setDataFriendListAdapter() {
        if (searchTerm.isNotBlank()) {
            if (resultList.isNotEmpty()) {
                val filteredList = resultList.filter { friend ->
                    friend.nickname.lowercase().contains(searchTerm)
                }
                friendListAdaptor.setData(filteredList.toMutableList())
                setFriendCount(filteredList)
            } else {
                friendListAdaptor.setData(resultList)
                friendCount.value = arrayOf(0, 0)
            }
        } else {
            friendListAdaptor.setData(resultList)
            setFriendCount(resultList)
        }
    }

    // 전체 친구 숫자 및 온라인 상태 친구 숫자 계산
    private fun setFriendCount(list: List<Friend>) {
        val allFriend = list.count()
        val onlineFriend = allFriend - list.count { it.status == "offline" }
        friendCount.value = arrayOf(onlineFriend, allFriend)
    }

    private fun sendFriendRequest(inputText: String, editText: EditText) {
        collection.whereEqualTo("nickname", inputText)
            .get().addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    if (inputText == sharedViewModel.nickname) {
                        Toast.makeText(requireContext(),
                            getString(R.string.friend_request_self),
                            Toast.LENGTH_SHORT)
                            .show()
                        editText.text.clear()
                    } else {
                        val uid = documents.first().id
                        collection.document(uid).update(
                            "friend_request",
                            FieldValue.arrayUnion(sharedViewModel.nickname)
                        ).addOnSuccessListener {
                            Toast.makeText(requireContext(),
                                getString(R.string.friend_request_complete),
                                Toast.LENGTH_SHORT)
                                .show()
                            editText.text.clear()
                        }
                    }
                } else {
                    Toast.makeText(requireContext(),
                        getString(R.string.friend_request_null),
                        Toast.LENGTH_SHORT)
                        .show()
                    editText.text.clear()
                }
            }
    }

    // 친구 요청 및 추가 스냅샷 리스너
    @Suppress("UNCHECKED_CAST")
    private fun observeFriendChange() {
        friendSnapshotListener = collection.document(App.auth.currentUser!!.uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener

                if (snapshot.data?.get("friend_request") != null) {
                    friendRequestList = snapshot.data!!["friend_request"] as List<String>
                    friendRequestListAdaptor.submitList(friendRequestList.toMutableList())
                    if (friendRequestList.isNotEmpty()) {
                        binding.friendRequestCount.visibility = View.VISIBLE
                        binding.friendRequestCount.text = friendRequestList.size.toString()
                    } else {
                        collection.document(App.auth.currentUser!!.uid).update(
                            "friend_request", FieldValue.delete()
                        )
                    }
                } else {
                    friendRequestListAdaptor.submitList(listOf())
                    binding.friendRequestCount.visibility = View.GONE
                }

                if (snapshot.data?.get("friend_list") != null) {
                    friendList = snapshot.data!!["friend_list"] as List<String>
                    if (friendList.isNotEmpty()) {
                        observeFriendStatusChange()
                    } else {
                        collection.document(App.auth.currentUser!!.uid).update(
                            "friend_list", FieldValue.delete()
                        )
                    }
                } else {
                    if (friendStatusSnapshotListener != null) friendStatusSnapshotListener!!.remove()
                    friendList = listOf()
                    resultList = mutableListOf()
                    setDataFriendListAdapter()
                    friendCount.value = arrayOf(0, 0)
                }
            }
    }

    private fun observeFriendStatusChange() {
        if (friendStatusSnapshotListener != null) friendStatusSnapshotListener!!.remove()
        friendStatusSnapshotListener =
            collection.whereIn("nickname", friendList).addSnapshotListener { documents, _ ->
                if (documents == null) return@addSnapshotListener

                val nicknameList = mutableListOf<String>()
                val statusList = mutableListOf<String>()

                for (document in documents) {
                    val nickname = document.data["nickname"] as String
                    val status = document.data["status"] as String
                    if (friendList.contains(nickname)) {
                        nicknameList.add(nickname)
                        statusList.add(status)
                    }
                }

                resultList.clear()
                for (i in nicknameList.indices) {
                    resultList.add(Friend(nicknameList[i], statusList[i]))
                }

                setFriendCount(resultList)

                resultList = resultList.sortedBy { it.status == "offline" }.toMutableList()
                setDataFriendListAdapter()
            }
    }

    // 친구 목록 리사이클러뷰 아이템 롱클릭시
    override fun friendLongClicked(position: Int) {
        val nickname = resultList[position].nickname
        showDialogConfirmDeleteFriend(nickname)
        dialogFriend?.dismiss()
    }

    // 친구 삭제 확인 다이얼로그 보여주기
    private fun showDialogConfirmDeleteFriend(nickname: String) {
        // 뷰바인딩 사용
        val inflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dBinding = DialogConfirmDeleteFriendBinding.inflate(inflater)

        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(dBinding.root)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(true)
        dialog.show()

        dBinding.dialogConfirmDeleteFriendTextview.text =
            getString(R.string.delete_friend_confirm, nickname)

        dBinding.dialogConfirmDeleteFriendPositiveBtn.setOnClickListener {
            collection.whereEqualTo("nickname", nickname).get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val uid = documents.first().id

                        App.firestore.runBatch { batch ->
                            batch.update(
                                collection.document(App.auth.currentUser!!.uid),
                                "friend_list",
                                FieldValue.arrayRemove(nickname)
                            )

                            batch.update(
                                collection.document(uid),
                                "friend_list",
                                FieldValue.arrayRemove(sharedViewModel.nickname)
                            )
                        }.addOnSuccessListener {
                            Toast.makeText(requireContext(),
                                getString(R.string.delete_friend_complete, nickname),
                                Toast.LENGTH_SHORT).show()
                            showDialogFriend()
                            dialog.dismiss()
                        }
                    }
                }
        }

        dBinding.dialogConfirmDeleteFriendNegativeBtn.setOnClickListener {
            showDialogFriend()
            dialog.dismiss()
        }
    }

    // 초대 버튼 클릭시
    override fun inviteBtnClicked(position: Int) {
        invitedNickname = resultList[position].nickname
        sharedViewModel.needDialogFriendOpen = true
        makeRoomCode("invite")
    }

    private fun inviteFriendAction() {
        if (invitedNickname != null) {
            CoroutineScope(Dispatchers.IO).launch {
                collection.whereEqualTo("nickname", invitedNickname)
                    .get().addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            val uid = documents.first().id
                            if (documents.first().data["invite_nickname"] == null
                                && documents.first().data["invite_roomCode"] == null
                            ) {
                                val data = hashMapOf(
                                    "invite_nickname" to sharedViewModel.nickname,
                                    "invite_roomCode" to sharedViewModel.roomCode
                                )
                                collection.document(uid).set(data, SetOptions.merge())
                                    .addOnSuccessListener {
                                        Toast.makeText(requireContext(),
                                            getString(R.string.invitation_complete),
                                            Toast.LENGTH_SHORT)
                                            .show()
                                    }
                            } else {
                                if (documents.first().data["invite_nickname"] == sharedViewModel.nickname) {
                                    Toast.makeText(requireContext(),
                                        getString(R.string.invitation_already),
                                        Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(requireContext(),
                                        getString(R.string.invitation_another),
                                        Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
            }
        }
    }

    // 친구 요청 수락 버튼
    override fun positiveBtnClicked(position: Int) {
        val nickname = friendRequestList[position]
        collection.whereEqualTo("nickname", nickname)
            .get().addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val uid = documents.first().id

                    App.firestore.runBatch { batch ->
                        // 친구의 친구목록에 나를 추가
                        batch.update(
                            collection.document(uid),
                            "friend_list",
                            FieldValue.arrayUnion(sharedViewModel.nickname)
                        )

                        // 내 친구목록에 친구를 추가
                        batch.update(
                            collection.document(App.auth.currentUser!!.uid),
                            "friend_list",
                            FieldValue.arrayUnion(nickname)
                        )

                        // 친구 요청 목록에서 선택된 아이템을 삭제
                        batch.update(
                            collection.document(App.auth.currentUser!!.uid),
                            "friend_request",
                            FieldValue.arrayRemove(nickname)
                        )
                    }.addOnSuccessListener {
                        Toast.makeText(requireContext(),
                            getString(R.string.friend_request_accepted),
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    // 친구 요청 거절 버튼
    override fun negativeBtnClicked(position: Int) {
        val nickname = friendRequestList[position]
        collection.document(App.auth.currentUser!!.uid).update(
            "friend_request", FieldValue.arrayRemove(nickname)
        ).addOnSuccessListener {
            Toast.makeText(requireContext(),
                getString(R.string.friend_request_declined),
                Toast.LENGTH_SHORT).show()
        }
    }

    // 게임 접속시 상태를 온라인으로 설정
    private fun setOnlineStatus() {
        collection.document(App.auth.currentUser!!.uid)
            .update("status", "online")
    }

    // 게임 초대 수락 후 화면 이동
    // CustomFragment()에서 이미 isForcedExit = false 처리됨
    override fun acceptInviteMoveFragment() {
        findNavController().navigate(R.id.action_mainMenuFragment_to_setMultiReadyFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        backPressCallback.remove()
        friendSnapshotListener.remove()
        if (friendStatusSnapshotListener != null) friendStatusSnapshotListener!!.remove()
        myJob.cancel()
    }
}