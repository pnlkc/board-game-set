package com.pnlkc.set

import android.app.Dialog
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.os.SystemClock
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Chronometer.OnChronometerTickListener
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.pnlkc.set.data.DataSource.KEY_FIELD_CARD_LIST
import com.pnlkc.set.data.DataSource.KEY_PAUSE_TIME
import com.pnlkc.set.data.DataSource.KEY_PREFS
import com.pnlkc.set.data.DataSource.KEY_SCORE
import com.pnlkc.set.data.DataSource.KEY_SHUFFLED_CARD_LIST
import com.pnlkc.set.databinding.SetSinglePlayFragmentBinding
import com.pnlkc.set.model.CardItem
import com.pnlkc.set.util.CustomFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import render.animations.Attention
import render.animations.Bounce
import render.animations.Render
import render.animations.Slide


class SetSinglePlayFragment : CustomFragment() {
    private var _binding: SetSinglePlayFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var backPressCallback: OnBackPressedCallback

    private lateinit var bindingCardList: List<ImageView>
    private lateinit var bindingSelectedCardList: List<ImageView>

    // 게임을 저장할지 안할지 알려주는 변수
    private var saveIndicator: Boolean = true

    // onStop 상태가 되었을 때 타이머 시간 정지용
    private var pauseTime = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = SetSinglePlayFragmentBinding.inflate(inflater, container, false)

        // OnBackPressedCallback (익명 클래스) 객체 생성
        backPressCallback = object : OnBackPressedCallback(true) {
            // 뒤로가기 했을 때 실행되는 기능
            var backWait: Long = 0
            override fun handleOnBackPressed() {
                if (System.currentTimeMillis() - backWait >= 2000) {
                    backWait = System.currentTimeMillis()
                    Toast.makeText(context, getText(R.string.back_btn_twice_main_menu),
                        Toast.LENGTH_SHORT).show()
                } else {
                    isForcedExit = false
                    findNavController().navigate(R.id.action_setSinglePlayFragment_pop)
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
            setSinglePlayFragment = this@SetSinglePlayFragment
        }

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

        // 시간점수용 Chronometer 시간 포맷 설정
        binding.timeScore.onChronometerTickListener = OnChronometerTickListener { timeScore ->
            val time = SystemClock.elapsedRealtime() - timeScore.base
            val h = (time / 3600000).toInt()
            val hText = if (h < 10) "0$h" else h
            val m = (time - h * 3600000).toInt() / 60000
            val mText = if (m < 10) "0$m" else m
            val s = (time - h * 3600000 - m * 60000).toInt() / 1000
            val sText = if (s < 10) "0$s" else s
            timeScore.text = when (h) {
                0 -> "$mText:$sText"
                else -> "$hText:$mText:$sText"
            }
        }

        // 저장된 게임을 불러오는지 아닌지 확인
        when (sharedViewModel.isContinueGame) {
            true -> {
                saveAndLoadGame("load")
                sharedViewModel.resetSelectedCard()
                startGame()
            }
            false -> {
                sharedViewModel.resetAllValue()
                binding.timeScore.base = SystemClock.elapsedRealtime() + pauseTime
                binding.timeScore.start()
                startGame()
            }
        }

        clickCard()

        // 남은 카드가 있고 가능한 조합이 없을 때 자동으로 카드 셔플하기
        sharedViewModel.leftCombination.observe(viewLifecycleOwner) {
            if (sharedViewModel.allCombination != 0 && it == 0) {
                Toast.makeText(activity, getString(R.string.no_combination),
                    Toast.LENGTH_SHORT).show()
                sharedViewModel.resetSelectedCard()
                invisibleAllSelectedCard()
                sharedViewModel.shuffleCard()
                sharedViewModel.initCard()
                startGame()
            } else if (sharedViewModel.allCombination == 0 && it == 0) {
                showFinalScoreDialog()
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
    private fun startGame() {
        bindingCardList.forEachIndexed { index, _ -> connectImageToCard(index) }
        sharedViewModel.calcLeftCardAndCombination()
        sharedViewModel.calcAllCombination()
    }

    // startGame() 실행시 카드속성도 같이 연결
    private fun connectImageToCard(index: Int) {
        if (sharedViewModel.fieldCardList[index] != CardItem(0, 0, 0, 0, "0")) {
            bindingCardList[index].visibility = View.VISIBLE
            val packageName = requireActivity().packageName
            val cardImgId = resources.getIdentifier(
                sharedViewModel.fieldCardList[index].cardImageName,
                "drawable",
                packageName
            )
            bindingCardList[index].setBackgroundResource(cardImgId)
        } else {
            bindingCardList[index].visibility = View.INVISIBLE
            bindingCardList[index].setBackgroundResource(R.drawable.emptycard)
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
            if (this) {
                // 정답 처리 및 애니메이션
                sharedViewModel.selectedCardIndex.forEach {
                    bounceInCard(it!!)
                    connectImageToCard(it)
                }

                sharedViewModel.calcLeftCardAndCombination()
                sharedViewModel.calcAllCombination()
            } else {
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

    // 카드섞기 버튼 기능
    fun shuffleButton() {
        if (sharedViewModel.leftCard.value != 0) {
            CoroutineScope(Dispatchers.Main).launch {
                val render = Render(requireContext())
                render.setAnimation(Slide().OutRight(binding.constraintLayout))
                render.setDuration(250L)
                render.start()

                delay(50L)
                sharedViewModel.resetSelectedCard()
                invisibleAllSelectedCard()
                sharedViewModel.shuffleCard()
                sharedViewModel.initCard()
                startGame()

                render.setAnimation(Slide().InLeft(binding.constraintLayout))
                render.setDuration(250L)
                render.start()
            }
        } else {
            Toast.makeText(activity, getString(R.string.no_card), Toast.LENGTH_SHORT).show()
        }
    }

    // 끝내기 버튼 기능
    fun endButton() {
        showFinalScoreDialog()
    }

    // 끝내기 버튼 누르면 나오는 MaterialDialog
    private fun showFinalScoreDialog() {
        // 다이얼로그를 띄웠으면 게임 저장 안하도록 하는 코드
        saveIndicator = false

        // 커스텀 Dialog 만들기
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_score_single)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(false)
        dialog.show()

        // Dialog 레이아웃의 뷰를 변수와 연결하기
        // 그냥 연결이 안되서 dialog 변수를 따로 만들고 거기서 findViewById해서 찾음
        val textView = dialog.findViewById<TextView>(R.id.dialog_score_single_textview)
        val leftBtn = dialog.findViewById<TextView>(R.id.dialog_score_single_left_btn)
        val rightBtn = dialog.findViewById<TextView>(R.id.dialog_score_single_right_btn)

        binding.timeScore.stop()
        val time = SystemClock.elapsedRealtime() - binding.timeScore.base
        val h = (time / 3600000).toInt()
        val m = (time - h * 3600000).toInt() / 60000
        val s = (time - h * 3600000 - m * 60000).toInt() / 1000
        val timeScoreText = when {
            h == 0 && m == 0 -> "$s" + getString(R.string.time_second)
            h == 0 -> "$m" + getString(R.string.time_minute) + " $s" + getString(R.string.time_second)
            else -> {
                "$h" + getString(R.string.time_hour) + " $m" + getString(R.string.time_minute) + " $s" + getString(
                    R.string.time_second)
            }
        }

        // Dialog 창에 스코어를 알려주는 TextView 부분만 강조해서 보여주는 코드
        val dialogText = getString(R.string.singleplay_result,
            sharedViewModel.score.value.toString(),
            timeScoreText)
        val spannableString = SpannableString(dialogText)

        // 시간 점수 텍스트 강조
        val startOne = dialogText.indexOf(timeScoreText)
        val endOne = startOne + timeScoreText.length
        spannableString.setSpan(ForegroundColorSpan(ContextCompat
            .getColor(requireContext(), R.color.dialog_score_text)),
            startOne, endOne, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(StyleSpan(Typeface.BOLD),
            startOne, endOne, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(RelativeSizeSpan(1.3f),
            startOne, endOne, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // 맞춘 개수 텍스트 강조
        val wordTwo = getString(R.string.singleplay_result_score_highlight,
            sharedViewModel.score.value.toString())
        val startTwo = dialogText.indexOf(wordTwo)
        val endTwo = startTwo + wordTwo.length
        spannableString.setSpan(ForegroundColorSpan(ContextCompat
            .getColor(requireContext(), R.color.dialog_score_text)),
            startTwo, endTwo, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(StyleSpan(Typeface.BOLD),
            startTwo, endTwo, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(RelativeSizeSpan(1.3f),
            startTwo, endTwo, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Dialog 뷰 기능 구현
        textView.text = spannableString
        leftBtn.setOnClickListener {
            isForcedExit = false
            findNavController().navigate(R.id.action_setSinglePlayFragment_pop)
            dialog.dismiss()
        }
        rightBtn.setOnClickListener {
            sharedViewModel.resetAllValue()
            startGame()
            saveIndicator = true
            pauseTime = 0L
            binding.timeScore.base = SystemClock.elapsedRealtime() + pauseTime
            binding.timeScore.start()
            dialog.dismiss()
        }
    }

    private fun saveAndLoadGame(string: String) {
        val sharedPreferences =
            requireActivity().getSharedPreferences(KEY_PREFS, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Json 파일 변환을 위한 Gson 객체
        val gson = GsonBuilder().create()

        val typeCardList: TypeToken<MutableList<CardItem>> =
            object : TypeToken<MutableList<CardItem>>() {}

        when (string) {
            "save" -> {
                val jsonShuffledCardList =
                    gson.toJson(sharedViewModel.shuffledCardList, typeCardList.type)
                val jsonFieldCardList =
                    gson.toJson(sharedViewModel.fieldCardList, typeCardList.type)

                editor.putString(KEY_SHUFFLED_CARD_LIST, jsonShuffledCardList)
                editor.putString(KEY_FIELD_CARD_LIST, jsonFieldCardList)
                editor.putInt(KEY_SCORE, sharedViewModel.score.value!!)
                editor.putLong(KEY_PAUSE_TIME,
                    binding.timeScore.base - SystemClock.elapsedRealtime())
                editor.apply()
            }
            "load" -> {
                val jsonShuffledCardList = sharedPreferences.getString(KEY_SHUFFLED_CARD_LIST, "")
                val jsonFieldCardList = sharedPreferences.getString(KEY_FIELD_CARD_LIST, "")

                sharedViewModel.shuffledCardList =
                    gson.fromJson(jsonShuffledCardList, typeCardList.type)
                sharedViewModel.fieldCardList =
                    gson.fromJson(jsonFieldCardList, typeCardList.type)
                sharedViewModel.score.value = sharedPreferences.getInt(KEY_SCORE, 0)
                pauseTime = sharedPreferences.getLong(KEY_PAUSE_TIME, 0L)
                binding.timeScore.base = SystemClock.elapsedRealtime() + pauseTime
                binding.timeScore.start()
            }
            "delete" -> {
                editor.clear()
                editor.apply()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // 게임이 완료되었거나 끝내기 버튼을 누르지 않았으면 게임 저장
        if (saveIndicator) {
            saveAndLoadGame("save")
        } else {
            saveAndLoadGame("delete")
        }

        // 시간점수 정지
        pauseTime = binding.timeScore.base - SystemClock.elapsedRealtime()
        binding.timeScore.stop()
    }

    override fun onStart() {
        super.onStart()
        // 시간점수 재개
        binding.timeScore.base = SystemClock.elapsedRealtime() + pauseTime
        binding.timeScore.start()
    }

    // 게임 초대 수락 후 화면 이동
    // CustomFragment()에서 이미 isForcedExit = false 처리됨
    override fun acceptInviteMoveFragment() {
        findNavController().navigate(R.id.action_setSinglePlayFragment_to_setMultiReadyFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        backPressCallback.remove()
    }
}