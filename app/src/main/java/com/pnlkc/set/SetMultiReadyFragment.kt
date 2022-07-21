package com.pnlkc.set

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.pnlkc.set.data.GameState
import com.pnlkc.set.data.UserMode
import com.pnlkc.set.databinding.SetMultiReadyFragmentBinding
import com.pnlkc.set.model.CardItem
import com.pnlkc.set.model.SetViewModel
import com.pnlkc.set.util.App
import com.pnlkc.set.util.ForcedExitService
import com.pnlkc.set.util.Vibrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SetMultiReadyFragment : Fragment() {
    private var _binding: SetMultiReadyFragmentBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SetViewModel by activityViewModels()

    private lateinit var backPressCallback: OnBackPressedCallback

    // 유저 리스트 저장용 변수
    private lateinit var userList: MutableList<String>

    // 뷰 리스트
    private lateinit var readyTextViewList: List<TextView>
    private lateinit var waitTextViewList: List<TextView>
    private lateinit var readyLinearLayout: List<LinearLayout>
    private lateinit var readyNicknameTextview: List<TextView>

    // Firestore 경로 저장용 변수
    private lateinit var collection: CollectionReference

    // 카드 정보를 받아왔는지 확인하는 변수
    private var isCardSettingDone = false

    private lateinit var userSnapshotListener: ListenerRegistration
    private lateinit var readySnapshotListener: ListenerRegistration
    private var cardSnapshotListener: ListenerRegistration? = null

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
                    Toast.makeText(context, "뒤로가기 버튼을 한번 더 누르면 대기실을 나갑니다",
                        Toast.LENGTH_SHORT).show()
                } else {
                    sharedViewModel.gameState = GameState.EXIT
                    if (userList.size > 1) deletePlayer() else deleteCollection()
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

        // 대기실에 들어오면 게임 상태를 대기 상태로 변경
        sharedViewModel.gameState = GameState.WAIT

        // 파이어스토어 경로 지정 (룸코드)
        collection = App.firestore.collection(sharedViewModel.roomCode!!)

        settingViewList()

        controlReadySituation()
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

    // 게임 시작전 준비창 관련
    @Suppress("UNCHECKED_CAST")
    private fun controlReadySituation() {
        userSnapshotListener = collection.document("user")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener

                userList = snapshot.data!!["user"] as MutableList<String>
                val myIndex = userList.indexOf(sharedViewModel.nickname)

                // 플레이어 리스트 1번째에 있는 사람이 방장이 되는 코드 (방장이 나가는 경우 고려)
                sharedViewModel.userMode = if (myIndex == 0) UserMode.HOST else UserMode.CLIENT

                if (snapshot.data!!["start"] == true) {
                    sharedViewModel.gameState = GameState.START
                    binding.readyBtn.isClickable = false
                    startCountDown()
                } else {
                    // 플레이어 숫자에 맞춰서 뷰 변경
                    (0..3).forEach { index ->
                        if (index < userList.size) {
                            readyNicknameTextview[index].text = userList[index]
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
                val myIndex = userList.indexOf(sharedViewModel.nickname)
                val readyList = MutableList(userList.size) { false }
                userList.forEachIndexed { index, s ->
                    if (snapshot.data!![s] == true) {
                        readyList[index] = true
                        if (index == myIndex) binding.readyBtn.text = "취소"
                        binding.readyBtn.setBackgroundResource(R.drawable.btn_bg)
                        readyTextViewList[index].visibility = View.VISIBLE
                        waitTextViewList[index].visibility = View.GONE
                    } else {
                        readyList[index] = false
                        if (index == myIndex) binding.readyBtn.text = "준비 완료"
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
                collection.document("ready").update(sharedViewModel.nickname, !result)
            }
        }
    }

    // 모두 준비가 완료 되었을 때 방장에게 게임 시작 버튼 보여주기
    private fun allReady() {
        if (sharedViewModel.userMode == UserMode.HOST) {
            binding.readyBtn.setBackgroundResource(R.drawable.highlight_btn_bg)
            binding.readyBtn.text = "게임 시작"
        }
    }

    // 방장이 게임 시작 버튼을 누르면 카운트다운 실행
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
            binding.countdownTextview.text = "게임 시작!"
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

    @Suppress("UNCHECKED_CAST")
    private fun deletePlayer() {
        val index = userList.indexOf(sharedViewModel.nickname)
        collection.document("user").get().addOnSuccessListener { snapshot ->
            val userList = snapshot.data!!["user"] as MutableList<String>
            val scoreList = snapshot.data!!["score"] as MutableList<String>
            userList.removeAt(index)
            scoreList.removeAt(index)

            App.firestore.runBatch { batch ->
                batch.update(collection.document("user"), "user", userList)
                batch.update(collection.document("user"), "score", scoreList)
                batch.update(collection.document("ready"),
                    sharedViewModel.nickname,
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
        if (sharedViewModel.gameState != GameState.START && sharedViewModel.gameState != GameState.EXIT) {
            // 강제종료했는지 알기 위한 서비스 등록
            val intent = Intent(requireContext(), ForcedExitService::class.java)
            intent.putExtra("userList", userList.toTypedArray())
            intent.putExtra("nickname", sharedViewModel.nickname)
            intent.putExtra("roomCode", sharedViewModel.roomCode)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 버전 O 부터는 백그라운드에서 서비스 실행이 안되서 startForegroundService 사용해야 됨
                // 매니페스트에 FOREGROUND_SERVICE 권한 추가해야됨
                requireActivity().startForegroundService(intent)
            } else {
                requireActivity().startService(intent)
            }
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
                requireActivity().stopService(Intent(requireContext(), ForcedExitService::class.java))
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    delay(100)
                    checkServiceRunning(count + 1)
                }
            }
        }
    }

    override fun onDestroyView() {
        Log.d("로그", "SetMultiReadyFragment - onDestroyView() 호출됨")
        super.onDestroyView()
        _binding = null
        backPressCallback.remove()
        // snapshotListener 제거 안하면 다시 들어올 때 팅기는 문제 발생
        userSnapshotListener.remove()
        readySnapshotListener.remove()
        if (cardSnapshotListener != null) cardSnapshotListener!!.remove()
    }
}