package com.pnlkc.set

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.pnlkc.set.data.GameState
import com.pnlkc.set.data.UserMode
import com.pnlkc.set.databinding.SetMultiReadyFragmentBinding
import com.pnlkc.set.model.CardItem
import com.pnlkc.set.model.SetViewModel
import com.pnlkc.set.util.App
import kotlinx.coroutines.*
import render.animations.Attention
import render.animations.Bounce
import render.animations.Render

class SetMultiReadyFragment : Fragment() {
    private var _binding: SetMultiReadyFragmentBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SetViewModel by activityViewModels()

    private lateinit var backPressCallback: OnBackPressedCallback

    private lateinit var bindingCardList: List<ImageView>
    private lateinit var bindingSelectedCardList: List<ImageView>

    private lateinit var userList: MutableList<String>

    // 뷰 리스트
    private lateinit var readyViewList: List<TextView>
    private lateinit var waitViewList: List<TextView>
    private lateinit var scoreLinearLayoutList: List<LinearLayout>
    private lateinit var scoreViewList: List<TextView>
    private lateinit var nicknameViewList: List<TextView>

    // Firestore 경로 저장용 변수
    private lateinit var collection: CollectionReference

    // SnapshotListener remove()를 위한 변수
    private lateinit var userSnapshotListener: ListenerRegistration
    private lateinit var cardSnapshotListener: ListenerRegistration
    private lateinit var answerSnapshotListener: ListenerRegistration

    // 카운트다운 코루틴 작업 취소를 위한 Job
    private var myJob: Job? = null

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
                    Toast.makeText(context, "뒤로가기 버튼을 한번 더 누르면 대기실을 나갑니다", Toast.LENGTH_SHORT)
                        .show()

                } else {
                    findNavController().navigate(R.id.action_setMultiReadyFragment_pop)
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

        // 게임 창에 들어오면 대기 상태로 변경
        sharedViewModel.gameState = GameState.WAIT

        // 파이어스토어 경로 지정 (룸코드)
        collection = App.firestore.collection(sharedViewModel.roomCode!!)

        bindingCardList = listOf(
            binding.card1, binding.card2, binding.card3, binding.card4,
            binding.card5, binding.card6, binding.card7, binding.card8,
            binding.card9, binding.card10, binding.card11, binding.card12
        )

        bindingSelectedCardList = listOf(
            binding.selectedCard1, binding.selectedCard2, binding.selectedCard3,
            binding.selectedCard4, binding.selectedCard5, binding.selectedCard6,
            binding.selectedCard7, binding.selectedCard8, binding.selectedCard9,
            binding.selectedCard10, binding.selectedCard11, binding.selectedCard12
        )

        readyViewList = listOf(
            binding.player1ReadyTextview, binding.player2ReadyTextview,
            binding.player3ReadyTextview, binding.player4ReadyTextview
        )

        waitViewList = listOf(
            binding.player1WaitTextview, binding.player2WaitTextview,
            binding.player3WaitTextview, binding.player4WaitTextview
        )

        nicknameViewList = listOf(
            binding.player1NicknameTextview, binding.player2NicknameTextview,
            binding.player3NicknameTextview, binding.player4NicknameTextview
        )

        scoreViewList = listOf(
            binding.player1ScoreTextview, binding.player2ScoreTextview,
            binding.player3ScoreTextview, binding.player4ScoreTextview,
        )

        scoreLinearLayoutList = listOf(
            binding.player1LinearLayout, binding.player2LinearLayout,
            binding.player3LinearLayout, binding.player4LinearLayout
        )

        controlReadySituation()
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
                    startCountDown()
                } else {
                    // 플레이어 숫자에 맞춰서 뷰 변경
                    when (userList.size) {
                        1 -> {
                            binding.player1NicknameTextview.text = userList[0]
                            binding.player1ReadyNicknameTextview.text = userList[0]

                            binding.player2LinearLayout.visibility = View.GONE
                            binding.player2ReadyLinearLayout.visibility = View.GONE

                            binding.player3LinearLayout.visibility = View.GONE
                            binding.player3ReadyLinearLayout.visibility = View.GONE

                            binding.player4LinearLayout.visibility = View.GONE
                            binding.player4ReadyLinearLayout.visibility = View.GONE

                        }
                        2 -> {
                            binding.player1NicknameTextview.text = userList[0]
                            binding.player1ReadyNicknameTextview.text = userList[0]

                            binding.player2NicknameTextview.text = userList[1]
                            binding.player2ReadyNicknameTextview.text = userList[1]

                            binding.player2LinearLayout.visibility = View.VISIBLE
                            binding.player2ReadyLinearLayout.visibility = View.VISIBLE

                            binding.player3LinearLayout.visibility = View.GONE
                            binding.player3ReadyLinearLayout.visibility = View.GONE

                            binding.player4LinearLayout.visibility = View.GONE
                            binding.player4ReadyLinearLayout.visibility = View.GONE
                        }
                        3 -> {
                            binding.player1NicknameTextview.text = userList[0]
                            binding.player1ReadyNicknameTextview.text = userList[0]

                            binding.player2NicknameTextview.text = userList[1]
                            binding.player2ReadyNicknameTextview.text = userList[1]

                            binding.player3NicknameTextview.text = userList[2]
                            binding.player3ReadyNicknameTextview.text = userList[2]

                            binding.player2LinearLayout.visibility = View.VISIBLE
                            binding.player2ReadyLinearLayout.visibility = View.VISIBLE

                            binding.player3LinearLayout.visibility = View.VISIBLE
                            binding.player3ReadyLinearLayout.visibility = View.VISIBLE

                            binding.player4LinearLayout.visibility = View.GONE
                            binding.player4ReadyLinearLayout.visibility = View.GONE
                        }
                        4 -> {
                            binding.player1NicknameTextview.text = userList[0]
                            binding.player1ReadyNicknameTextview.text = userList[0]

                            binding.player2NicknameTextview.text = userList[1]
                            binding.player2ReadyNicknameTextview.text = userList[1]

                            binding.player3NicknameTextview.text = userList[2]
                            binding.player3ReadyNicknameTextview.text = userList[2]

                            binding.player4NicknameTextview.text = userList[3]
                            binding.player4ReadyNicknameTextview.text = userList[3]

                            binding.player2LinearLayout.visibility = View.VISIBLE
                            binding.player2ReadyLinearLayout.visibility = View.VISIBLE

                            binding.player3LinearLayout.visibility = View.VISIBLE
                            binding.player3ReadyLinearLayout.visibility = View.VISIBLE

                            binding.player4LinearLayout.visibility = View.VISIBLE
                            binding.player4ReadyLinearLayout.visibility = View.VISIBLE
                        }
                    }

                    // 다른 유저의 준비 상태에 맞춰 뷰 변경하는 코드
                    val readyList = snapshot.data!!["ready"] as MutableList<Boolean>
                    readyList.forEachIndexed { index, b ->
                        if (b) {
                            if (index == myIndex) binding.readyBtn.text = "취소"
                            binding.readyBtn.setBackgroundResource(R.drawable.btn_bg)
                            readyViewList[index].visibility = View.VISIBLE
                            waitViewList[index].visibility = View.GONE
                        } else {
                            if (index == myIndex) binding.readyBtn.text = "준비 완료"
                            binding.readyBtn.setBackgroundResource(R.drawable.btn_bg)
                            readyViewList[index].visibility = View.GONE
                            waitViewList[index].visibility = View.VISIBLE
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
    }

    // 준비 버튼 기능
    @Suppress("UNCHECKED_CAST")
    fun readyBtn() {
        App.firestore.runTransaction { transaction ->
            if (sharedViewModel.userMode == UserMode.HOST && sharedViewModel.gameState == GameState.READY) {
                transaction.update(collection.document("user"), mapOf("start" to true))
            } else {
                val snapshot = transaction.get(collection.document("user"))
                val myIndex = userList.indexOf(sharedViewModel.nickname)
                val readyList = snapshot.data!!["ready"] as MutableList<Boolean>
                readyList[myIndex] = !readyList[myIndex]
                transaction.update(collection.document("user"), mapOf("ready" to readyList))
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
            binding.readyBtn.visibility = View.INVISIBLE

            binding.countdownTextview.text = "3"
            binding.countdownTextview.visibility = View.VISIBLE
            delay(750)
            binding.countdownTextview.text = "2"
            delay(750)
            binding.countdownTextview.text = "1"
            delay(750)
            binding.countdownTextview.text = "GAME START!"
            delay(750)
            binding.countdownTextview.visibility = View.INVISIBLE
            binding.readyConstraintLayout.visibility = View.INVISIBLE
            binding.roomCodeTextview.visibility = View.INVISIBLE

            binding.cardConstraintLayout.visibility = View.VISIBLE
            binding.leftcardTextView.visibility = View.VISIBLE
            binding.scoreLinearLayout.visibility = View.VISIBLE
            binding.answerBtn.visibility = View.VISIBLE
            binding.noCombinationBtn.visibility = View.VISIBLE
            binding.touchBlocker.visibility = View.VISIBLE
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
                val data = hashMapOf("cardList" to cardListJson)
                collection.document("card").set(data).addOnSuccessListener {
                    sharedViewModel.initCard()
                    settingCard()
                    clickCard()
                    attachAnswerListener()
                }
            }
            UserMode.CLIENT -> {
                cardSnapshotListener =
                    collection.document("card").addSnapshotListener { snapshot, _ ->
                        if (snapshot == null) return@addSnapshotListener
                        sharedViewModel.shuffledCardList =
                            gson.fromJson(snapshot.data!!["cardList"].toString(), cardItemType.type)
                        sharedViewModel.initCard()
                        settingCard()
                        clickCard()
                        attachAnswerListener()
                    }
            }
        }
    }

    // 카드 클릭시 코드
    private fun clickCard() {
        bindingCardList.forEachIndexed { index, imageView ->
            imageView.setOnClickListener { clickImage(index) }
        }
    }

    // 초기 12장 세팅
    private fun settingCard() {
        // cardSnapshotListener 제거
        if (sharedViewModel.userMode == UserMode.CLIENT) cardSnapshotListener.remove()

        bindingCardList.forEachIndexed { index, imageView ->
            if (sharedViewModel.leftCard.value != 0) imageView.visibility = View.VISIBLE
            connectImageToCard(index)
        }
        sharedViewModel.showLeftCardAndCombination()
    }

    // settingCard() 실행시 카드속성도 같이 연결
    private fun connectImageToCard(index: Int) {
        if (!sharedViewModel.fieldCardList.contains(CardItem(0, 0, 0, 0, 0))) {
            bindingCardList[index].setImageResource(sharedViewModel.fieldCardList[index].cardImage)
        } else {
            bindingCardList[index].visibility = View.INVISIBLE
            bindingCardList[index].setImageResource(R.drawable.emptycard)
        }
    }

    // 카드 클릭시 카드 값을 selectedCard에 저장하는 기능
    private fun clickImage(index: Int) {
        when {
            // 같은 카드를 다시 눌렀을 때 처리
            sharedViewModel.selectedCardIndex.contains(index) -> {
                setVisibilitySelectedCard(index, View.INVISIBLE)
                sharedViewModel.selectedCardIndex[sharedViewModel.selectedCardIndex.indexOf(index)] =
                    null
            }

            // 3개가 선택되면 정답인지 확인하도록 처리
            sharedViewModel.selectedCardIndex[0] == null -> {
                sharedViewModel.selectedCardIndex[0] = index
                setVisibilitySelectedCard(index, View.VISIBLE)
            }
            sharedViewModel.selectedCardIndex[0] != null
                    && sharedViewModel.selectedCardIndex[0] != index
                    && sharedViewModel.selectedCardIndex[1] == null -> {
                sharedViewModel.selectedCardIndex[1] = index
                setVisibilitySelectedCard(index, View.VISIBLE)
            }
            sharedViewModel.selectedCardIndex[0] != null
                    && sharedViewModel.selectedCardIndex[0] != index
                    && sharedViewModel.selectedCardIndex[1] != null
                    && sharedViewModel.selectedCardIndex[1] != index
                    && sharedViewModel.selectedCardIndex[2] == null -> {
                sharedViewModel.selectedCardIndex[2] = index
                setVisibilitySelectedCard(index, View.VISIBLE)
                isCorrect()
            }
        }
    }

    // 정답이면 카드 변경하는 코드
    private fun isCorrect() {
        sharedViewModel.selectedCardIndex.forEach {
            setVisibilitySelectedCard(it!!, View.INVISIBLE)
        }

        sharedViewModel.checkCard(
            sharedViewModel.fieldCardList[sharedViewModel.selectedCardIndex[0]!!],
            sharedViewModel.fieldCardList[sharedViewModel.selectedCardIndex[1]!!],
            sharedViewModel.fieldCardList[sharedViewModel.selectedCardIndex[2]!!]
        )

        sharedViewModel.increaseScore().apply {
            if (myJob != null) myJob!!.cancel()
            binding.answerBtn.setBackgroundResource(R.drawable.btn_bg)
            binding.answerBtn.text = "정답"
            binding.touchBlocker.visibility = View.VISIBLE
            binding.answerBtn.isClickable = true

            if (this) {
                correctAnswer()
                // 정답 처리 및 애니메이션
                sharedViewModel.selectedCardIndex.forEach {
                    bounceInCard(it!!)
                    connectImageToCard(it)
                }
                sharedViewModel.showLeftCardAndCombination()
            } else {
                incorrectAnswer()
                // 오답 애니메이션
                sharedViewModel.selectedCardIndex.forEach { shakeCard(it!!) }
            }
            sharedViewModel.resetSelectedCard()
        }
    }

    // 정답일 때 카드 흔드는 기능
    private fun bounceInCard(index: Int) {
        val render = Render(requireContext())
        render.setAnimation(Bounce().In(bindingCardList[index]))
        render.setDuration(300L)
        render.start()
    }

    // 오답일 때 카드 흔드는 기능
    private fun shakeCard(index: Int) {
        val render = Render(requireContext())
        render.setAnimation(Attention().Tada(bindingCardList[index]))
        render.setDuration(700L)
        render.start()
    }

    // 카드 선택시 노란색 표시
    private fun setVisibilitySelectedCard(index: Int, visibility: Int) {
        bindingSelectedCardList[index].visibility = visibility
    }

    // 전체카드 선택 해제 코드
    private fun invisibleAllSelectedCard() {
        bindingSelectedCardList.forEach {
            it.visibility = View.INVISIBLE
        }
    }

    // answerSnapshotListener 설정
    @Suppress("UNCHECKED_CAST")
    private fun attachAnswerListener() {
        answerSnapshotListener = collection.document("answer").addSnapshotListener { snapshot, _ ->
            if (snapshot == null) return@addSnapshotListener
            if (snapshot.exists()) {
                val index = (snapshot.data!!["index"] as Long).toInt()

                if (userList.indexOf(sharedViewModel.nickname) != index) {
                    if (myJob != null) myJob!!.cancel()
                    binding.answerBtn.setBackgroundResource(R.drawable.btn_bg)
                    binding.answerBtn.text = "정답"
                    binding.touchBlocker.visibility = View.VISIBLE

                    if (snapshot.data!!["result"] == null) {
                        binding.answerBtn.isClickable = false
                        scoreLinearLayoutList[index].setBackgroundResource(R.drawable.answer_highlight_btn_bg)
                        nicknameViewList[index].setTextColor(R.color.btn_text_color)
                        scoreViewList[index].setTextColor(R.color.btn_text_color)
                    } else {
                        var score = scoreViewList[index].text.toString().toInt()
                        val gson = GsonBuilder().create()
                        val cardIndexType: TypeToken<MutableList<Int?>> =
                            object : TypeToken<MutableList<Int?>>() {}
                        val selectedCard: MutableList<Int?> =
                            gson.fromJson(snapshot.data!!["selectedCardIndex"].toString(),
                                cardIndexType.type)
                        sharedViewModel.selectedCardIndex = selectedCard

                        if (snapshot.data!!["result"] == true) {
                            score += 1
                            scoreViewList[index].text = score.toString()
                            sharedViewModel.getNextCard()
                            sharedViewModel.selectedCardIndex.forEach {
                                bounceInCard(it!!)
                                connectImageToCard(it)
                            }
                            sharedViewModel.showLeftCardAndCombination()
                        } else {
                            score -= 1
                            scoreViewList[index].text = score.toString()
                            if (!sharedViewModel.selectedCardIndex.contains(null)) {
                                sharedViewModel.selectedCardIndex.forEach { shakeCard(it!!) }
                            }
                        }
                        sharedViewModel.resetSelectedCard()
                        binding.answerBtn.isClickable = true
                        scoreLinearLayoutList[index].setBackgroundResource(0)
                        nicknameViewList[index].setTextColor(R.color.text_color)
                        scoreViewList[index].setTextColor(R.color.text_color)
                    }
                } else {
                    if (snapshot.data!!["result"] == null) {
                        binding.touchBlocker.visibility = View.INVISIBLE
                        if (myJob != null) myJob!!.cancel()
                        answerCountDown()
                    }
                }
            }
        }
    }

    // 정답 버튼 기능
    fun answerBtn() {
        standByAnswer()
        App.firestore.runTransaction { transaction ->
            val snapshot = transaction.get(collection.document("answer"))
            val data = hashMapOf(
                "nickname" to sharedViewModel.nickname,
                "index" to userList.indexOf(sharedViewModel.nickname)
            )
            if (snapshot.data == null || snapshot.data!!.size == 4) {
                transaction.set(collection.document("answer"), data)
            }
        }
    }

    // 정답 버튼을 누르면 동시에 누른 사람이 있는지 확인
    private fun standByAnswer() {
        binding.answerBtn.isClickable = false
        binding.answerBtn.setBackgroundResource(R.drawable.highlight_btn_bg)
        myJob = CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                binding.answerBtn.text = "대기중."
                delay(250)
                binding.answerBtn.text = "대기중.."
                delay(250)
                binding.answerBtn.text = "대기중..."
                delay(250)
            }
        }
    }

    // 정답 버튼을 제일 먼저 누른게 확인되면 카운트 다운 시작
    private fun answerCountDown() {
        myJob = CoroutineScope(Dispatchers.Main).launch {
            binding.answerBtn.text = "카운트다운!"
            delay(830)
            binding.answerBtn.text = "5"
            delay(830)
            binding.answerBtn.text = "4"
            delay(830)
            binding.answerBtn.text = "3"
            delay(830)
            binding.answerBtn.text = "2"
            delay(830)
            binding.answerBtn.text = "1"
            delay(830)
            binding.answerBtn.setBackgroundResource(R.drawable.btn_bg)
            binding.answerBtn.text = "정답"
            binding.touchBlocker.visibility = View.VISIBLE
            binding.answerBtn.isClickable = true
            incorrectAnswer()
        }
    }

    fun correctAnswer() {
        val gson = GsonBuilder().create()
        val cardIndexType: TypeToken<MutableList<Int?>> =
            object : TypeToken<MutableList<Int?>>() {}
        val selectedCardIndexJson =
            gson.toJson(sharedViewModel.selectedCardIndex, cardIndexType.type)
        val data = hashMapOf(
            "result" to true,
            "selectedCardIndex" to selectedCardIndexJson
        )
        collection.document("answer").set(data, SetOptions.merge())

        val myIndex = userList.indexOf(sharedViewModel.nickname)
        var score = scoreViewList[myIndex].text.toString().toInt()
        score += 1
        scoreViewList[myIndex].text = score.toString()
    }

    fun incorrectAnswer() {
        val gson = GsonBuilder().create()
        val cardIndexType: TypeToken<MutableList<Int?>> =
            object : TypeToken<MutableList<Int?>>() {}
        val selectedCardIndexJson =
            gson.toJson(sharedViewModel.selectedCardIndex, cardIndexType.type)
        val data = hashMapOf(
            "result" to false,
            "selectedCardIndex" to selectedCardIndexJson
        )
        collection.document("answer").set(data, SetOptions.merge())

        val myIndex = userList.indexOf(sharedViewModel.nickname)
        var score = scoreViewList[myIndex].text.toString().toInt()
        score -= 1
        scoreViewList[myIndex].text = score.toString()
    }

    // 조합없음 버튼
    fun noCombinationBtn() {
//        if (sharedViewModel.leftCard.value != 0) {
//            CoroutineScope(Dispatchers.Main).launch {
//                val render = Render(requireContext())
//                render.setAnimation(Slide().OutRight(binding.cardConstraintLayout))
//                render.setDuration(250L)
//                render.start()
//
//                delay(50L)
//                sharedViewModel.resetSelectedCard()
//                invisibleAllSelectedCard()
//                sharedViewModel.shuffleCard()
//                settingCard()
//
//                render.setAnimation(Slide().InLeft(binding.cardConstraintLayout))
//                render.setDuration(250L)
//                render.start()
//            }
//        } else {
//            Toast.makeText(activity, "남은카드가 없습니다", Toast.LENGTH_SHORT).show()
//        }
    }

    // 게임이 끝나면 나오는 MaterialDialog
    @SuppressLint("SetTextI18n")
    fun showFinalScoreDialog() {
        // 커스텀 Dialog 만들기
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_one)
        dialog.setCancelable(false)
        dialog.show()

        // Dialog 레이아웃의 뷰를 변수와 연결하기
        // 그냥 연결이 안되서 dialog 변수를 따로 만들고 거기서 findViewById해서 찾음
        val dialogTitleTextView = dialog.findViewById<TextView>(R.id.dialog_title_textview)
        val dialogTextView = dialog.findViewById<TextView>(R.id.dialog_textview)
        val dialogLeftBtn = dialog.findViewById<TextView>(R.id.dialog_left_btn)
        val dialogRightBtn = dialog.findViewById<TextView>(R.id.dialog_right_btn)

        // Dialog 창에 스코어를 알려주는 TextView 부분만 강조해서 보여주는 코드
        val dialogText = "당신은 ${sharedViewModel.score.value}개를\n 맞추셨습니다!"
        val spannableString = SpannableString(dialogText)
        val word = sharedViewModel.score.value.toString() + "개"
        val start = dialogText.indexOf(word)
        val end = start + word.length
//        spannableString.setSpan(ForegroundColorSpan(Color.parseColor("#ae3b75")),
        spannableString.setSpan(ForegroundColorSpan(ContextCompat
            .getColor(requireContext(), R.color.dialog_score_text)),
            start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(StyleSpan(Typeface.BOLD),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(RelativeSizeSpan(1.3f),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Dialog 뷰 기능 구현
        dialogTitleTextView.text = "대단합니다"
        dialogTextView.text = spannableString
        dialogLeftBtn.apply {
            text = "나가기"
            setOnClickListener { activity?.finish() }
        }
        dialogRightBtn.apply {
            text = "다시하기"
            setOnClickListener {
                sharedViewModel.resetAllValue()
                settingCard()
                dialog.dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        backPressCallback.remove()
    }
}