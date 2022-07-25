package com.pnlkc.set

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.pnlkc.set.data.GameState
import com.pnlkc.set.data.UserMode
import com.pnlkc.set.databinding.SetMultiStartFragmentBinding
import com.pnlkc.set.model.CardItem
import com.pnlkc.set.model.SetViewModel
import com.pnlkc.set.util.App
import com.pnlkc.set.util.ForcedExitService
import com.pnlkc.set.util.Vibrator
import kotlinx.coroutines.*
import render.animations.Attention
import render.animations.Bounce
import render.animations.Render
import render.animations.Slide

class SetMultiStartFragment : Fragment() {
    private var _binding: SetMultiStartFragmentBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SetViewModel by activityViewModels()

    private lateinit var backPressCallback: OnBackPressedCallback

    private lateinit var bindingCardList: List<ImageView>
    private lateinit var bindingSelectedCardList: List<ImageView>

    private lateinit var userList: MutableList<String>
    private lateinit var scoreList: MutableList<String>

    // 뷰 리스트
    private lateinit var playerLinearLayoutList: List<LinearLayout>
    private lateinit var scoreTextViewList: List<TextView>
    private lateinit var nicknameTextViewList: List<TextView>

    // Firestore 경로 저장용 변수
    private lateinit var collection: CollectionReference

    // 카운트다운 코루틴 작업 취소를 위한 Job
    private var myJob: Job? = null

    private lateinit var userSnapshotListener: ListenerRegistration
    private lateinit var answerSnapshotListener: ListenerRegistration
    private lateinit var cardSnapshotListener: ListenerRegistration

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = SetMultiStartFragmentBinding.inflate(inflater, container, false)

        // OnBackPressedCallback (익명 클래스) 객체 생성
        backPressCallback = object : OnBackPressedCallback(true) {
            // 뒤로가기 했을 때 실행되는 기능
            var backWait: Long = 0
            override fun handleOnBackPressed() {
                if (System.currentTimeMillis() - backWait >= 2000) {
                    backWait = System.currentTimeMillis()
                    Toast.makeText(context, "게임 중 나갈시 패배처리 됩니다", Toast.LENGTH_SHORT)
                        .show()

                } else {
                    sharedViewModel.gameState = GameState.EXIT
                    deletePlayer()
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
            setOnlineFragment = this@SetMultiStartFragment
        }

        // 파이어스토어 경로 지정 (룸코드)
        collection = App.firestore.collection(sharedViewModel.roomCode!!)

        settingViewList()

        userListListener()
        settingInitCard()
        connectClickListener()
        attachAnswerSnapshotListener()
        attachCardSnapshotListener()
    }

    // 뷰리스트 초기화
    private fun settingViewList() {
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

        nicknameTextViewList = listOf(
            binding.player1NicknameTextview, binding.player2NicknameTextview,
            binding.player3NicknameTextview, binding.player4NicknameTextview
        )

        scoreTextViewList = listOf(
            binding.player1ScoreTextview, binding.player2ScoreTextview,
            binding.player3ScoreTextview, binding.player4ScoreTextview,
        )

        playerLinearLayoutList = listOf(
            binding.player1LinearLayout, binding.player2LinearLayout,
            binding.player3LinearLayout, binding.player4LinearLayout
        )
    }

    // 유저 리스트 리스너
    @Suppress("UNCHECKED_CAST")
    private fun userListListener() {
        userSnapshotListener = collection.document("user")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener

                userList = snapshot.data!!["user"] as MutableList<String>
                val myIndex = userList.indexOf(sharedViewModel.nickname)
                scoreList = snapshot.data!!["score"] as MutableList<String>

                // 플레이어 리스트 1번째에 있는 사람이 방장이 되는 코드 (방장이 나가는 경우 고려)
                sharedViewModel.userMode = if (myIndex == 0) UserMode.HOST else UserMode.CLIENT

                // 플레이어 숫자에 맞춰서 뷰 변경
                (0..3).forEach { index ->
                    if (index < userList.size) {
                        nicknameTextViewList[index].text = userList[index]
                        playerLinearLayoutList[index].visibility = View.VISIBLE
                        scoreTextViewList[index].text = scoreList[index]
                    } else {
                        playerLinearLayoutList[index].visibility = View.GONE
                    }
                }

                // 플레이어가 1명만 남았을 때 게임 종료 처리
                if (userList.size == 1 && userList.contains(sharedViewModel.nickname)) {
                    Toast.makeText(context, "남은 플레이어가 없어 게임을 플레이 할 수 없습니다",
                        Toast.LENGTH_SHORT).show()
                    showFinalScoreDialog()
                }
            }
    }

    // 카드 이미지뷰에 클릭리스너 추가
    private fun connectClickListener() {
        bindingCardList.forEachIndexed { index, imageView ->
            imageView.setOnClickListener { selectCard(index) }
        }
    }

    // 카드 클릭시 카드 값을 selectedCard에 저장하는 기능
    private fun selectCard(index: Int) {
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

    // 초기 12장 세팅
    private fun settingInitCard() {
        bindingCardList.forEachIndexed { index, _ -> connectImageToCard(index) }
        sharedViewModel.calcLeftCardAndCombination()
        sharedViewModel.calcAllCombination()
    }

    // 뷰와 fieldCardList 연결
    private fun connectImageToCard(index: Int) {
        if (sharedViewModel.fieldCardList[index] != CardItem(0, 0, 0, 0, 0)) {
            bindingCardList[index].visibility = View.VISIBLE
            bindingCardList[index].setBackgroundResource(sharedViewModel.fieldCardList[index].cardImage)
        } else {
            bindingCardList[index].visibility = View.INVISIBLE
            bindingCardList[index].setBackgroundResource(R.drawable.emptycard)
        }
    }

    // 정답이면 카드 변경하는 코드
    private fun isCorrect() {
        if (myJob != null) myJob!!.cancel()
        binding.cardTouchBlocker.visibility = View.VISIBLE
        binding.answerBtn.setBackgroundResource(R.drawable.btn_bg)
        binding.answerBtn.text = "정답"

        sharedViewModel.selectedCardIndex.forEach {
            setVisibilitySelectedCard(it!!, View.INVISIBLE)
        }

        sharedViewModel.checkCard(
            sharedViewModel.fieldCardList[sharedViewModel.selectedCardIndex[0]!!],
            sharedViewModel.fieldCardList[sharedViewModel.selectedCardIndex[1]!!],
            sharedViewModel.fieldCardList[sharedViewModel.selectedCardIndex[2]!!]
        )

        sharedViewModel.increaseScore().apply {
            if (this) {
                correctAnswer()
                // 정답 처리 및 애니메이션
                sharedViewModel.selectedCardIndex.forEach {
                    bounceInCard(it!!)
                    connectImageToCard(it)
                }
                sharedViewModel.calcLeftCardAndCombination()
                sharedViewModel.calcAllCombination()
            } else {
                incorrectAnswer()
                // 오답 애니메이션
                sharedViewModel.selectedCardIndex.forEach { shakeCard(it!!) }
            }
            sharedViewModel.resetSelectedCard()
        }
    }

    // 카드 선택시 노란색 표시
    private fun setVisibilitySelectedCard(index: Int, visibility: Int) {
        bindingSelectedCardList[index].visibility = visibility
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

    // 전체카드 선택 해제 코드
    private fun invisibleAllSelectedCard() {
        bindingSelectedCardList.forEach {
            it.visibility = View.INVISIBLE
        }
    }


    // 카드 관련 SnapshotListener
    @Suppress("UNCHECKED_CAST")
    private fun attachCardSnapshotListener() {
        cardSnapshotListener = collection.document("card").addSnapshotListener { snapshot, _ ->
            if (snapshot == null) return@addSnapshotListener

            if (snapshot.data!!["cardList"] != null) {
                val gson = GsonBuilder().create()
                val cardItemType: TypeToken<MutableList<CardItem>> =
                    object : TypeToken<MutableList<CardItem>>() {}

                if (snapshot.data!!["complete"] != null) {
                    val complete = snapshot.data!!["complete"] as MutableList<String>
                    if (complete.size != userList.size) {
                        if (!complete.contains(sharedViewModel.nickname)) {
                            sharedViewModel.shuffledCardList =
                                gson.fromJson(snapshot.data!!["cardList"].toString(),
                                    cardItemType.type)
                            sharedViewModel.initCard()
                            collection.document("card")
                                .update("complete", FieldValue.arrayUnion(sharedViewModel.nickname))
                                .addOnSuccessListener { shuffleCardAnimation() }
                        }
                    } else {
                        // 플레이들이 모두 카드를 받았으면 카드 문서를 초기화
                        if (sharedViewModel.userMode == UserMode.HOST) {
                            if (complete.size == userList.size) {
                                val data = hashMapOf<String, Any>(
                                    "cardList" to FieldValue.delete(),
                                    "complete" to FieldValue.delete()
                                )
                                collection.document("card").update(data)
                            }
                        }
                    }
                } else {
                    sharedViewModel.shuffledCardList =
                        gson.fromJson(snapshot.data!!["cardList"].toString(), cardItemType.type)
                    sharedViewModel.initCard()
                    val data = hashMapOf("complete" to mutableListOf(sharedViewModel.nickname))
                    collection.document("card").set(data)
                        .addOnSuccessListener { shuffleCardAnimation() }
                }
            }
        }
    }

    // answerSnapshotListener 설정
    @Suppress("UNCHECKED_CAST")
    private fun attachAnswerSnapshotListener() {
        answerSnapshotListener = collection.document("answer").addSnapshotListener { snapshot, _ ->
            if (snapshot == null) return@addSnapshotListener
            if (snapshot.exists()) {
                val playerNickname = snapshot.data!!["nickname"] as String
                val index = userList.indexOf(playerNickname)

                if (sharedViewModel.nickname != playerNickname) {
                    if (myJob != null) myJob!!.cancel()
                    binding.cardTouchBlocker.visibility = View.VISIBLE
                    binding.answerTouchBlocker.visibility = View.VISIBLE
                    binding.answerBtn.setBackgroundResource(R.drawable.btn_bg)
                    binding.noCombinationBtn.setBackgroundResource(R.drawable.btn_bg)
                    binding.answerBtn.text = "정답"
                    binding.noCombinationBtn.text = "X"
                    sharedViewModel.resetSelectedCard()
                    invisibleAllSelectedCard()

                    if (snapshot.data!!["result"] == null) {
                        playerLinearLayoutList[index].setBackgroundResource(R.drawable.answer_highlight_bg)
                        val textColor =
                            ContextCompat.getColor(requireContext(), R.color.answer_text)
                        nicknameTextViewList[index].setTextColor(textColor)
                        scoreTextViewList[index].setTextColor(textColor)
                    } else {
                        val gson = GsonBuilder().create()
                        val cardIndexType: TypeToken<MutableList<Int?>> =
                            object : TypeToken<MutableList<Int?>>() {}
                        val selectedCard: MutableList<Int?> =
                            gson.fromJson(snapshot.data!!["selectedCardIndex"].toString(),
                                cardIndexType.type)
                        sharedViewModel.selectedCardIndex = selectedCard

                        when (snapshot.data!!["mode"]) {
                            "answer" -> {
                                if (snapshot.data!!["result"] == true) {
                                    showResultAnswerPlayer(true, index)
                                    sharedViewModel.getNextCard()
                                    sharedViewModel.selectedCardIndex.forEach {
                                        bounceInCard(it!!)
                                        connectImageToCard(it)
                                    }
                                    sharedViewModel.calcLeftCardAndCombination()
                                    sharedViewModel.calcAllCombination()
                                } else {
                                    showResultAnswerPlayer(false, index)
                                    if (!sharedViewModel.selectedCardIndex.contains(null)) {
                                        sharedViewModel.selectedCardIndex.forEach { shakeCard(it!!) }
                                    }
                                }
                                sharedViewModel.resetSelectedCard()
                            }
                            "noCombination" -> {
                                if (snapshot.data!!["result"] == true) {
                                    showResultAnswerPlayer(true, index)
                                    sharedViewModel.calcLeftCardAndCombination()
                                    sharedViewModel.calcAllCombination()
                                } else {
                                    showResultAnswerPlayer(false, index)
                                }
                            }
                        }
                        binding.answerTouchBlocker.visibility = View.INVISIBLE
                    }
                } else {
                    if (snapshot.data!!["result"] == null) {
                        if (myJob != null) myJob!!.cancel()
                        when (snapshot.data!!["mode"]) {
                            "answer" -> answerCountDown()
                            "noCombination" -> {
                                myJob = CoroutineScope(Dispatchers.Main).launch {
                                    binding.noCombinationBtn.setBackgroundResource(R.drawable.highlight_btn_bg)
                                    delay(500)
                                    binding.noCombinationBtn.text = "..."
                                    Vibrator().makeVibration(requireContext())
                                    if (sharedViewModel.leftCombination.value == 0) {
                                        correctAnswer()
                                        shuffleCard()
                                    } else {
                                        incorrectAnswer()
                                    }
                                }
                            }
                        }
                    } else {
                        sharedViewModel.resetSelectedCard()
                        invisibleAllSelectedCard()
                        binding.answerTouchBlocker.visibility = View.INVISIBLE
                        binding.noCombinationBtn.setBackgroundResource(R.drawable.btn_bg)
                        binding.noCombinationBtn.text = "X"
                    }
                }
            }
        }
    }

    // 플레이어의 답에 따라 스코어판 색을 0.5초 동안 변경
    private fun showResultAnswerPlayer(result: Boolean, index: Int) {
        when (result) {
            true -> {
                CoroutineScope(Dispatchers.Main).launch {
                    playerLinearLayoutList[index].setBackgroundResource(R.drawable.answer_correct_bg)
                    delay(500)
                    playerLinearLayoutList[index].setBackgroundResource(0)
                    val textColor = ContextCompat.getColor(requireContext(), R.color.text_color)
                    nicknameTextViewList[index].setTextColor(textColor)
                    scoreTextViewList[index].setTextColor(textColor)
                }
            }
            else -> {
                CoroutineScope(Dispatchers.Main).launch {
                    playerLinearLayoutList[index].setBackgroundResource(R.drawable.answer_incorrect_bg)
                    var textColor = ContextCompat.getColor(requireContext(), R.color.answer_text)
                    nicknameTextViewList[index].setTextColor(textColor)
                    scoreTextViewList[index].setTextColor(textColor)
                    delay(500)
                    playerLinearLayoutList[index].setBackgroundResource(0)
                    textColor = ContextCompat.getColor(requireContext(), R.color.text_color)
                    nicknameTextViewList[index].setTextColor(textColor)
                    scoreTextViewList[index].setTextColor(textColor)
                }
            }
        }
    }

    // 정답 버튼 기능
    fun answerBtn() {
        binding.answerTouchBlocker.visibility = View.VISIBLE
        collection.document("answer").get().addOnSuccessListener { snapshot ->
            val data = hashMapOf(
                "nickname" to sharedViewModel.nickname,
                "mode" to "answer"
            )
            if (snapshot.data == null || snapshot.data!!.size == 4) {
                collection.document("answer").set(data)
            } else {
                if (myJob != null) myJob!!.cancel()
            }
        }
    }

    // 정답 버튼을 누르면 카운트 다운 시작
    private fun answerCountDown() {
        myJob = CoroutineScope(Dispatchers.Main).launch {
//            binding.answerBtn.text = "대기중"
//            delay(750)
            Vibrator().makeVibration(requireContext())
            binding.cardTouchBlocker.visibility = View.INVISIBLE
            binding.answerBtn.setBackgroundResource(R.drawable.highlight_btn_bg)
            binding.answerBtn.text = "5"
            delay(1000)
            binding.answerBtn.text = "4"
            delay(1000)
            binding.answerBtn.text = "3"
            delay(1000)
            binding.answerBtn.text = "2"
            delay(1000)
            binding.answerBtn.text = "1"
            delay(1000)
            invisibleAllSelectedCard()
            binding.answerBtn.setBackgroundResource(R.drawable.btn_bg)
            binding.answerBtn.text = "정답"
            binding.cardTouchBlocker.visibility = View.VISIBLE
            incorrectAnswer()
        }
    }

    // 정답인 카드를 골랐을 때
    @Suppress("UNCHECKED_CAST")
    private fun correctAnswer() {
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
        val index = userList.indexOf(sharedViewModel.nickname)
        scoreList[index] = (scoreList[index].toInt() + 1).toString()
        collection.document("user").update("score", scoreList)
    }

    // 오답인 카드를 골랐을 때
    @Suppress("UNCHECKED_CAST")
    private fun incorrectAnswer() {
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
        val index = userList.indexOf(sharedViewModel.nickname)
        scoreList[index] = (scoreList[index].toInt() - 1).toString()
        collection.document("user").update("score", scoreList)
    }

    // 조합없음 버튼
    fun noCombinationBtn() {
        binding.answerTouchBlocker.visibility = View.VISIBLE
        collection.document("answer").get().addOnSuccessListener { snapshot ->
            val data = hashMapOf(
                "nickname" to sharedViewModel.nickname,
                "mode" to "noCombination"
            )
            if (snapshot.data == null || snapshot.data!!.size == 4) {
                collection.document("answer").set(data)
            } else {
                if (myJob != null) myJob!!.cancel()
            }
        }
    }

    private fun shuffleCard() {
        val gson = GsonBuilder().create()
        val cardItemType: TypeToken<MutableList<CardItem>> =
            object : TypeToken<MutableList<CardItem>>() {}

        sharedViewModel.shuffleCard()
        val cardListJson = gson.toJson(sharedViewModel.shuffledCardList, cardItemType.type)
        val cardList = hashMapOf("cardList" to cardListJson)
        collection.document("card").set(cardList)
    }

    private fun shuffleCardAnimation() {
        // 남은 카드로 조합을 만들수 있을 때
        if (sharedViewModel.allCombination != 0) {
            CoroutineScope(Dispatchers.Main).launch {
                val render = Render(requireContext())
                render.setAnimation(Slide().OutRight(binding.cardConstraintLayout))
                render.setDuration(250L)
                render.start()

                delay(50L)
                sharedViewModel.resetSelectedCard()
                invisibleAllSelectedCard()
                settingInitCard()

                render.setAnimation(Slide().InLeft(binding.cardConstraintLayout))
                render.setDuration(250L)
                render.start()
            }
        }
        // 남은 카드로 조합을 만들수 없을 때
        else {
            showFinalScoreDialog()
        }
    }

    // 게임이 끝나면 나오는 Dialog
    @SuppressLint("SetTextI18n")
    private fun showFinalScoreDialog() {
        sharedViewModel.gameState = GameState.END

        // SnapshotListener remove 없이 collection 제거하면 앱 팅김
        userSnapshotListener.remove()
        answerSnapshotListener.remove()
        cardSnapshotListener.remove()

        if (sharedViewModel.userMode == UserMode.HOST) deleteCollection()

        // 커스텀 Dialog 만들기
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_score_multi)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(false)
        dialog.show()

        // 점수에 따라 플레이어 정렬
        val mapList = userList.mapIndexed { i, s -> s to scoreList[i].toInt() }
            .groupBy({ it.second }, { it.first }).toList().sortedByDescending { it.first }.toMap()

        // Dialog 레이아웃의 뷰를 변수와 연결하기
        // 그냥 연결이 안되서 dialog 변수를 따로 만들고 거기서 findViewById해서 찾음
        val dialogMainMenuBtn = dialog.findViewById<TextView>(R.id.dialog_score_multi_main_menu_btn)
        val playConstraintLayoutList = listOf<ConstraintLayout>(
            dialog.findViewById(R.id.dialog_score_multi_first_player_constraint_layout),
            dialog.findViewById(R.id.dialog_score_multi_second_player_constraint_layout),
            dialog.findViewById(R.id.dialog_score_multi_third_player_constraint_layout),
            dialog.findViewById(R.id.dialog_score_multi_fourth_player_constraint_layout)
        )
        val playTextViewList = listOf<TextView>(
            dialog.findViewById(R.id.dialog_score_multi_first_player_textview),
            dialog.findViewById(R.id.dialog_score_multi_second_player_textview),
            dialog.findViewById(R.id.dialog_score_multi_third_player_textview),
            dialog.findViewById(R.id.dialog_score_multi_fourth_player_textview)
        )

        (0 until mapList.size).forEach { i ->
            playConstraintLayoutList[i].visibility = View.VISIBLE
            var text = ""
            mapList[mapList.keys.toList()[i]]!!.forEach { text += it + "\n" }
            playTextViewList[i].text = text.slice(0..text.lastIndex - 2)
        }

        // Dialog 뷰 기능 구현
        dialogMainMenuBtn.setOnClickListener {
            findNavController().navigate(R.id.action_setMultiStartFragment_pop)
            dialog.dismiss()
        }
    }

    private fun deleteCollection() {
        collection.get().addOnSuccessListener { querySnapshot ->
            // 코루틴을 써서 뷰가 destroy 되도 작업이 계속되도록 설정
            CoroutineScope(Dispatchers.IO).launch {
                querySnapshot.forEach { snapshot -> snapshot.reference.delete() }
            }
        }
    }

    // 게임 중 플레이어가 나가면 플레이어 삭제
    private fun deletePlayer() {
        val index = userList.indexOf(sharedViewModel.nickname)
        userList.removeAt(index)
        scoreList.removeAt(index)
        App.firestore.runBatch { batch ->
            batch.update(collection.document("user"), "user", userList)
            batch.update(collection.document("user"), "score", scoreList)
            batch.update(collection.document("ready"),
                sharedViewModel.nickname!!,
                FieldValue.delete())
        }.addOnSuccessListener {
            findNavController().navigate(R.id.action_setMultiStartFragment_pop)
        }
    }

    // 앱이 Stop 상태가 되면 강제종료 감지 서비스 실행
    override fun onStop() {
        super.onStop()
        if (sharedViewModel.gameState != GameState.END && sharedViewModel.gameState != GameState.EXIT) {
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
        super.onDestroyView()
        _binding = null
        backPressCallback.remove()
        userSnapshotListener.remove()
        answerSnapshotListener.remove()
        cardSnapshotListener.remove()
    }
}