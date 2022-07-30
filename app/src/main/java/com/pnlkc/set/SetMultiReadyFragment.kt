package com.pnlkc.set

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.pnlkc.set.data.GameState
import com.pnlkc.set.data.UserMode
import com.pnlkc.set.databinding.DialogConfirmDeleteFriendBinding
import com.pnlkc.set.databinding.DialogFriendBinding
import com.pnlkc.set.databinding.SetMultiReadyFragmentBinding
import com.pnlkc.set.model.CardItem
import com.pnlkc.set.model.Friend
import com.pnlkc.set.model.SetViewModel
import com.pnlkc.set.recyclerview_friend_list.FriendListAdaptor
import com.pnlkc.set.recyclerview_friend_list.IFriendList
import com.pnlkc.set.recyclerview_friend_request_list.FriendRequestListAdaptor
import com.pnlkc.set.recyclerview_friend_request_list.IFriendRequestList
import com.pnlkc.set.util.App
import com.pnlkc.set.util.ForcedExitService
import com.pnlkc.set.util.Vibrator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SetMultiReadyFragment : Fragment(), IFriendList, IFriendRequestList {
    private var _binding: SetMultiReadyFragmentBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SetViewModel by activityViewModels()

    private lateinit var backPressCallback: OnBackPressedCallback

    // 플레이어 리스트 저장용 변수
    private lateinit var playerList: MutableList<String>

    // 뷰 리스트
    private lateinit var readyTextViewList: List<TextView>
    private lateinit var waitTextViewList: List<TextView>
    private lateinit var readyLinearLayout: List<LinearLayout>
    private lateinit var readyNicknameTextview: List<TextView>

    // Firestore 경로 저장용 변수
    private lateinit var collection: CollectionReference
    private lateinit var userListCollection: CollectionReference

    // 카드 정보를 받아왔는지 확인하는 변수
    private var isCardSettingDone = false

    private lateinit var userSnapshotListener: ListenerRegistration
    private lateinit var readySnapshotListener: ListenerRegistration
    private var cardSnapshotListener: ListenerRegistration? = null
    private lateinit var friendSnapshotListener: ListenerRegistration
    private var friendStatusSnapshotListener: ListenerRegistration? = null

    private var dialogFriend: Dialog? = null
    private var friendRequestList: List<String> = listOf()
    private var friendList: List<String> = listOf()
    private var resultList = mutableListOf<Friend>()

    private val friendRequestListAdaptor = FriendRequestListAdaptor(this)
    private lateinit var friendListAdaptor: FriendListAdaptor

    private val friendCount = MutableLiveData(arrayOf(0, 0))

    private var needStartService = true

    private var myJob = Job()
    private var searchTerm = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = SetMultiReadyFragmentBinding.inflate(inflater, container, false)

        // OnBackPressedCallback (익명 클래스) 객체 생성
        backPressCallback = object : OnBackPressedCallback(true) {
            // 뒤로가기 했을 때 실행되는 기능
            var backWait: Long = 0
            override fun handleOnBackPressed() {
                if (System.currentTimeMillis() - backWait >= 2000) {
                    backWait = System.currentTimeMillis()
                    Toast.makeText(context, getString(R.string.back_btn_twice_room),
                        Toast.LENGTH_SHORT).show()
                } else {
                    needStartService = false
                    if (playerList.size > 1) deletePlayer() else deleteCollection()
                }
            }
        }
        // 액티비티의 BackPressedDispatcher에 여기서 만든 callback 객체를 등록
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressCallback)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // DataBinding 사용하기 위한 코드
        binding.apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = sharedViewModel
            setOnlineFragment = this@SetMultiReadyFragment
        }

        friendListAdaptor = FriendListAdaptor(this, requireContext())

        // 대기실에 들어오면 게임 상태를 대기 상태로 변경
        sharedViewModel.gameState = GameState.WAIT

        // 유저 상태를 게임중으로 설정
        setPlayStatus()

        // 파이어스토어 경로 지정 (룸코드)
        collection = App.firestore.collection(sharedViewModel.roomCode!!)
        userListCollection = App.firestore.collection("USER_LIST")

        observeFriendChange()

        settingViewList()

        controlReadySituation()

        binding.friendBtn.setOnClickListener { showDialogFriend() }

        if (sharedViewModel.needDialogFriendOpen) {
            showDialogFriend()
            sharedViewModel.needDialogFriendOpen = false
        }
    }

    // 뷰리스트 초기화
    private fun settingViewList() {
        readyTextViewList = listOf(
            binding.player1ReadyTextview, binding.player2ReadyTextview,
            binding.player3ReadyTextview, binding.player4ReadyTextview
        )

        waitTextViewList = listOf(
            binding.player1WaitTextview, binding.player2WaitTextview,
            binding.player3WaitTextview, binding.player4WaitTextview
        )

        readyLinearLayout = listOf(
            binding.player1ReadyLinearLayout, binding.player2ReadyLinearLayout,
            binding.player3ReadyLinearLayout, binding.player4ReadyLinearLayout
        )

        readyNicknameTextview = listOf(
            binding.player1ReadyNicknameTextview, binding.player2ReadyNicknameTextview,
            binding.player3ReadyNicknameTextview, binding.player4ReadyNicknameTextview
        )
    }

    // 유저 상태를 게임중으로 설정
    private fun setPlayStatus() {
        if (App.checkAuth()) {
            App.firestore.collection("USER_LIST").document(App.auth.currentUser!!.uid)
                .update("status", "play")
        }
    }

    // 게임 시작전 준비창 관련
    @Suppress("UNCHECKED_CAST")
    private fun controlReadySituation() {
        userSnapshotListener = collection.document("user")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener

                playerList = snapshot.data!!["user"] as MutableList<String>
                val myIndex = playerList.indexOf(sharedViewModel.nickname)

                // 플레이어 리스트 1번째에 있는 사람이 방장이 되는 코드 (방장이 나가는 경우 고려)
                sharedViewModel.userMode = if (myIndex == 0) UserMode.HOST else UserMode.CLIENT

                if (snapshot.data!!["start"] == true) {
                    needStartService = false
                    sharedViewModel.gameState = GameState.START
                    binding.readyBtn.isClickable = false
                    startCountDown()
                } else {
                    // 플레이어 숫자에 맞춰서 뷰 변경
                    (0..3).forEach { index ->
                        if (index < playerList.size) {
                            readyNicknameTextview[index].text = playerList[index]
                            readyLinearLayout[index].visibility = View.VISIBLE
                        } else {
                            readyLinearLayout[index].visibility = View.GONE
                        }
                    }
                }
            }

        readySnapshotListener = collection.document("ready")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                val myIndex = playerList.indexOf(sharedViewModel.nickname)
                val readyList = MutableList(playerList.size) { false }
                playerList.forEachIndexed { index, s ->
                    if (snapshot.data!![s] == true) {
                        readyList[index] = true
                        if (index == myIndex) binding.readyBtn.text = getString(R.string.cancel)
                        binding.readyBtn.setBackgroundResource(R.drawable.btn_bg)
                        readyTextViewList[index].visibility = View.VISIBLE
                        waitTextViewList[index].visibility = View.GONE
                    } else {
                        readyList[index] = false
                        if (index == myIndex) binding.readyBtn.text =
                            getString(R.string.ready_btn_text)
                        binding.readyBtn.setBackgroundResource(R.drawable.btn_bg)
                        readyTextViewList[index].visibility = View.GONE
                        waitTextViewList[index].visibility = View.VISIBLE
                    }
                }

                if (readyList.size > 1 && !readyList.contains(false)) {
                    allReady()
                    sharedViewModel.gameState = GameState.READY
                } else {
                    sharedViewModel.gameState = GameState.WAIT
                }
            }
    }

    // 준비 버튼 기능
    @Suppress("UNCHECKED_CAST")
    fun readyBtn() {
        if (sharedViewModel.userMode == UserMode.HOST && sharedViewModel.gameState == GameState.READY) {
            binding.readyBtn.isClickable = false
            collection.document("user").update("start", true)
        } else {
            collection.document("ready").get().addOnSuccessListener { snapshot ->
                val result = snapshot.data!![sharedViewModel.nickname] as Boolean
                collection.document("ready").update(sharedViewModel.nickname!!, !result)
            }
        }
    }

    // 모두 준비가 완료 되었을 때 방장에게 게임 시작 버튼 보여주기
    private fun allReady() {
        if (sharedViewModel.userMode == UserMode.HOST) {
            binding.readyBtn.setBackgroundResource(R.drawable.highlight_btn_bg)
            binding.readyBtn.text = getString(R.string.game_start)
        }
    }

    // 방장이 게임 시작 버튼을 누르면 카운트다운 실행
    @SuppressLint("SetTextI18n")
    private fun startCountDown() {
        CoroutineScope(Dispatchers.Main).launch {
            startGame()
            Vibrator().makeVibration(requireContext())
            binding.readyBtn.visibility = View.INVISIBLE
            binding.roomCodeTextview.visibility = View.INVISIBLE
            binding.countdownTextview.text = "3"
            binding.countdownTextview.visibility = View.VISIBLE
            delay(750)
            binding.countdownTextview.text = "2"
            delay(750)
            binding.countdownTextview.text = "1"
            delay(750)
            binding.countdownTextview.text = getString(R.string.game_start) + "!"
            delay(750)

            moveSetMultiStartFragment()
        }
    }

    // 카드세팅이 완료되면 게임 화면으로 이동
    private fun moveSetMultiStartFragment() {
        if (isCardSettingDone && isCardSettingDone) {
            findNavController().navigate(R.id.action_setMultiReadyFragment_to_setMultiStartFragment)
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                moveSetMultiStartFragment()
            }, 100)
        }
    }

    // 게임이 시작되면 카드 세팅
    @Suppress("UNCHECKED_CAST")
    private fun startGame() {
        val gson = GsonBuilder().create()
        val cardItemType: TypeToken<MutableList<CardItem>> =
            object : TypeToken<MutableList<CardItem>>() {}

        when (sharedViewModel.userMode) {
            UserMode.HOST -> {
                sharedViewModel.resetShuffledCardList()
                val cardListJson = gson.toJson(sharedViewModel.shuffledCardList, cardItemType.type)
                val cardList = hashMapOf(
                    "cardList" to cardListJson,
                    "complete" to mutableListOf(sharedViewModel.nickname)
                )
                collection.document("card").set(cardList).addOnSuccessListener {
                    sharedViewModel.initCard()
                    isCardSettingDone = true
                }
            }
            UserMode.CLIENT -> {
                cardSnapshotListener =
                    collection.document("card").addSnapshotListener { snapshot, _ ->
                        if (snapshot == null) return@addSnapshotListener
                        if (snapshot.data != null) {
                            sharedViewModel.shuffledCardList =
                                gson.fromJson(snapshot.data!!["cardList"].toString(),
                                    cardItemType.type)
                            sharedViewModel.initCard()
                            collection.document("card")
                                .update("complete", FieldValue.arrayUnion(sharedViewModel.nickname))
                                .addOnSuccessListener {
                                    isCardSettingDone = true
                                }
                        }
                    }
            }
        }
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
        userListCollection.whereEqualTo("nickname", inputText)
            .get().addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    if (inputText == sharedViewModel.nickname) {
                        Toast.makeText(requireContext(),
                            getString(R.string.friend_request_self),
                            Toast.LENGTH_SHORT).show()
                        editText.text.clear()
                    } else {
                        val uid = documents.first().id
                        userListCollection.document(uid).update(
                            "friend_request",
                            FieldValue.arrayUnion(sharedViewModel.nickname)
                        ).addOnSuccessListener {
                            Toast.makeText(requireContext(),
                                getString(R.string.friend_request_complete),
                                Toast.LENGTH_SHORT).show()
                            editText.text.clear()
                        }
                    }
                } else {
                    Toast.makeText(requireContext(),
                        getString(R.string.friend_request_null),
                        Toast.LENGTH_SHORT).show()
                    editText.text.clear()
                }
            }
    }

    // 친구 요청 및 추가 스냅샷 리스너
    @Suppress("UNCHECKED_CAST")
    private fun observeFriendChange() {
        friendSnapshotListener = userListCollection.document(App.auth.currentUser!!.uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener

                if (snapshot.data?.get("friend_request") != null) {
                    friendRequestList = snapshot.data!!["friend_request"] as List<String>
                    friendRequestListAdaptor.submitList(friendRequestList.toMutableList())
                    if (friendRequestList.isNotEmpty()) {
                        binding.friendRequestCount.visibility = View.VISIBLE
                        binding.friendRequestCount.text = friendRequestList.size.toString()
                    } else {
                        userListCollection.document(App.auth.currentUser!!.uid).update(
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
                        userListCollection.document(App.auth.currentUser!!.uid).update(
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

                if (snapshot.data!!["invite_nickname"] != null
                    && snapshot.data!!["invite_roomCode"] != null
                ) {
                    App.firestore.collection("USER_LIST").document(App.auth.currentUser!!.uid)
                        .update(
                            "invite_nickname", FieldValue.delete(),
                            "invite_roomCode", FieldValue.delete()
                        )
                }
            }
    }

    private fun observeFriendStatusChange() {
        if (friendStatusSnapshotListener != null) friendStatusSnapshotListener!!.remove()
        friendStatusSnapshotListener =
            userListCollection.whereIn("nickname", friendList).addSnapshotListener { documents, _ ->
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
            userListCollection.whereEqualTo("nickname", nickname).get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val uid = documents.first().id

                        App.firestore.runBatch { batch ->
                            batch.update(
                                userListCollection.document(App.auth.currentUser!!.uid),
                                "friend_list",
                                FieldValue.arrayRemove(nickname)
                            )

                            batch.update(
                                userListCollection.document(uid),
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
        if (playerList.size == 4) {
            Toast.makeText(requireContext(), getString(R.string.invitation_maximum),
                Toast.LENGTH_SHORT).show()
        } else {
            val nickname = resultList[position].nickname
            userListCollection.whereEqualTo("nickname", nickname)
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
                            userListCollection.document(uid).set(data, SetOptions.merge())
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

    // 친구 요청 수락 버튼
    override fun positiveBtnClicked(position: Int) {
        val nickname = friendRequestList[position]
        userListCollection.whereEqualTo("nickname", nickname)
            .get().addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val uid = documents.first().id

                    App.firestore.runBatch { batch ->
                        // 친구의 친구목록에 나를 추가
                        batch.update(
                            userListCollection.document(uid),
                            "friend_list",
                            FieldValue.arrayUnion(sharedViewModel.nickname)
                        )

                        // 내 친구목록에 친구를 추가
                        batch.update(
                            userListCollection.document(App.auth.currentUser!!.uid),
                            "friend_list",
                            FieldValue.arrayUnion(nickname)
                        )

                        // 친구 요청 목록에서 선택된 아이템을 삭제
                        batch.update(
                            userListCollection.document(App.auth.currentUser!!.uid),
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
        userListCollection.document(App.auth.currentUser!!.uid).update(
            "friend_request", FieldValue.arrayRemove(nickname)
        ).addOnSuccessListener {
            Toast.makeText(requireContext(),
                getString(R.string.friend_request_declined),
                Toast.LENGTH_SHORT).show()
        }
    }


    @Suppress("UNCHECKED_CAST")
    private fun deletePlayer() {
        val index = playerList.indexOf(sharedViewModel.nickname)
        collection.document("user").get().addOnSuccessListener { snapshot ->
            val userList = snapshot.data!!["user"] as MutableList<String>
            val scoreList = snapshot.data!!["score"] as MutableList<String>
            userList.removeAt(index)
            scoreList.removeAt(index)

            App.firestore.runBatch { batch ->
                batch.update(collection.document("user"), "user", userList)
                batch.update(collection.document("user"), "score", scoreList)
                batch.update(collection.document("ready"),
                    sharedViewModel.nickname!!,
                    FieldValue.delete())
            }.addOnSuccessListener {
                findNavController().navigate(R.id.action_setMultiReadyFragment_pop)
            }
        }
    }

    // 게임이 완료되면 Firestore Collection(게임방) 삭제
    private fun deleteCollection() {
        userSnapshotListener.remove()
        readySnapshotListener.remove()
        if (cardSnapshotListener != null) cardSnapshotListener!!.remove()

        collection.get().addOnSuccessListener { querySnapshot ->
            // 코루틴을 써서 뷰가 destroy 되도 작업이 계속되도록 설정
            CoroutineScope(Dispatchers.IO).launch {
                querySnapshot.forEach { snapshot -> snapshot.reference.delete() }
            }
            findNavController().navigate(R.id.action_setMultiReadyFragment_pop)
        }
    }

    // 앱이 Stop 상태가 되면 강제종료 감지 서비스 실행
    override fun onStop() {
        super.onStop()
        if (needStartService) {
            // 강제종료했는지 알기 위한 서비스 등록
            val intent = Intent(requireContext(), ForcedExitService::class.java)
            intent.putExtra("userList", playerList.toTypedArray())
            intent.putExtra("nickname", sharedViewModel.nickname)
            intent.putExtra("roomCode", sharedViewModel.roomCode)

            // 버전 O 부터는 백그라운드에서 서비스 실행이 안되서 startForegroundService 사용해야 됨
            // 매니페스트에 FOREGROUND_SERVICE 권한 추가해야됨
            requireActivity().startForegroundService(intent)
        }
    }

    // // 앱이 (Re)Start 상태가 되면 강제종료 감지 서비스 중지
    override fun onStart() {
        super.onStart()
        checkServiceRunning(0)
    }

    // 서비스가 실행중인지 확인하고 stopService() 호출하도록 하는 기능
    // 다크모드 변경 화면 회전과 같이 onStop()과 onStart()가 연이어 실행되는 경우
    // ForegroundServiceDidNotStartInTimeException 에러 발생 방지
    private fun checkServiceRunning(count: Int) {
        if (count < 5) {
            if (App.isServiceRunning) {
                requireActivity().stopService(Intent(requireContext(),
                    ForcedExitService::class.java))
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    delay(100)
                    checkServiceRunning(count + 1)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        backPressCallback.remove()
        // snapshotListener 제거 안하면 다시 들어올 때 팅기는 문제 발생
        userSnapshotListener.remove()
        readySnapshotListener.remove()
        if (cardSnapshotListener != null) cardSnapshotListener!!.remove()
        friendSnapshotListener.remove()
        if (friendStatusSnapshotListener != null) friendStatusSnapshotListener!!.remove()
        myJob.cancel()
    }
}