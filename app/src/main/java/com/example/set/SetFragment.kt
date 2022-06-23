package com.example.set

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.set.data.DataSource.KEY_CARD_DATA_LIST
import com.example.set.data.DataSource.KEY_PREFS
import com.example.set.data.DataSource.KEY_SCORE
import com.example.set.data.DataSource.KEY_USED_CARD_LIST
import com.example.set.databinding.SetFragmentBinding
import com.example.set.model.CardItem
import com.example.set.model.SetViewModel
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import render.animations.*

class SetFragment : Fragment() {
    private var _binding: SetFragmentBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SetViewModel by activityViewModels()

    private lateinit var backPressCallback: OnBackPressedCallback

    private lateinit var bindingCardList: List<ImageView>
    private lateinit var bindingSelectedCardList: List<ImageView>

    // 게임을 저장할지 말지 알려주는 변수
    private var saveIndicator: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = SetFragmentBinding.inflate(inflater, container, false)

        // OnBackPressedCallback (익명 클래스) 객체 생성
        backPressCallback = object : OnBackPressedCallback(true) {
            // 뒤로가기 했을 때 실행되는 기능
            var backWait: Long = 0
            override fun handleOnBackPressed() {
                if (System.currentTimeMillis() - backWait >= 2000) {
                    backWait = System.currentTimeMillis()
                    Toast.makeText(context, "뒤로가기 버튼을 한번 더 누르면 시작화면으로 돌아갑니다", Toast.LENGTH_SHORT).show()

                } else {
                    findNavController().navigate(R.id.action_setFragment_to_startFragment)
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
            setFragment = this@SetFragment
        }

        sharedViewModel.resetAllValue()

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

        when (sharedViewModel.isContinueGame) {
            true -> {
                saveAndLoadGame("load")
                bindingCardList.forEachIndexed { index, imageView ->
                    imageView.setImageResource(sharedViewModel.cardDataList[index].cardImage)
                }
                sharedViewModel.showLeftCard()
                sharedViewModel.showLeftCombination()
            }
            false -> startGame()
        }

        clickCard()

    }

    // 카드 클릭시 코드
    private fun clickCard() {
        bindingCardList.forEachIndexed { index, imageView ->
            imageView.setOnClickListener { clickImage(sharedViewModel.cardDataList[index]) }
        }
    }

    // 초기 12장 세팅
    private fun startGame() {
        bindingCardList.forEach {
// 게임 시작 될 때 한번만 실행하면 될거 같은데...
            if (sharedViewModel.leftCard.value != 0) it.visibility = View.VISIBLE
            connectImageToCard(it)
        }
    }

    // startGame() 실행시 카드속성도 같이 연결
    private fun connectImageToCard(imageView: ImageView) {
        if (sharedViewModel.leftCard.value != 0) {
            sharedViewModel.changeCardImage(imageView)
            bindingCardList.forEachIndexed { index, it ->
                if (it == imageView) sharedViewModel.cardDataList[index] = sharedViewModel.temp
            }
        } else {
            bindingCardList.forEachIndexed { index, it ->
                if (imageView == it) {
                    it.visibility = View.INVISIBLE
                    sharedViewModel.cardDataList[index] =
                        CardItem(0, 0, 0, 0, 0)
                }
            }
        }
        sharedViewModel.showLeftCombination()
    }

    // 카드 클릭시 카드 값을 selectedCard에 저장하는 기능
    private fun clickImage(cardItem: CardItem) {
        when {
            // 같은 카드를 다시 눌렀을 때 처리
            sharedViewModel.selectedCardList.contains(cardItem) -> {
                setVisibilitySelectedCard(cardItem, View.INVISIBLE)
                val index = sharedViewModel.selectedCardList.indexOf(cardItem)
                sharedViewModel.selectedCardList[index] = null
            }

            // 3개가 선택되면 정답인지 확인하도록 처리
            sharedViewModel.selectedCardList[0] == null -> {
                sharedViewModel.selectedCardList[0] = cardItem
                setVisibilitySelectedCard(sharedViewModel.selectedCardList[0]!!, View.VISIBLE)
            }
            sharedViewModel.selectedCardList[0] != null
                    && sharedViewModel.selectedCardList[0] != cardItem
                    && sharedViewModel.selectedCardList[1] == null -> {
                sharedViewModel.selectedCardList[1] = cardItem
                setVisibilitySelectedCard(sharedViewModel.selectedCardList[1]!!, View.VISIBLE)
            }

            sharedViewModel.selectedCardList[0] != null
                    && sharedViewModel.selectedCardList[0] != cardItem
                    && sharedViewModel.selectedCardList[1] != null
                    && sharedViewModel.selectedCardList[1] != cardItem
                    && sharedViewModel.selectedCardList[2] == null -> {
                sharedViewModel.selectedCardList[2] = cardItem
                setVisibilitySelectedCard(sharedViewModel.selectedCardList[2]!!, View.VISIBLE)
                isCorrect()
            }
        }
    }

    // 정답이면 카드 변경하는 코드
    private fun isCorrect() {
        sharedViewModel.checkCard(
            sharedViewModel.selectedCardList[0]!!,
            sharedViewModel.selectedCardList[1]!!,
            sharedViewModel.selectedCardList[2]!!
        )
        sharedViewModel.increaseScore().apply {
            if (this) {
                // 정답 애니메이션
                sharedViewModel.selectedCardList.forEach { bounceInCard(it) }

                // 정답시 처리
                sharedViewModel.selectedCardList.forEach { selectedCardMatchCardImage(it) }

                // 마지막 조합 정답시 처리
                if (
                    sharedViewModel.leftCard.value == 0
                    && sharedViewModel.leftCombination.value == 0
                ) {
                    showFinalScoreDialog()
                }

                invisibleAllSelectedCard()
                sharedViewModel.resetSelectedCard()
            } else {
                // 오답 애니메이션
                sharedViewModel.selectedCardList.forEach { shakeCard(it) }

                invisibleAllSelectedCard()
                sharedViewModel.resetSelectedCard()
            }
        }
    }

    // 새 카드가 들어올 때 애니메이션
    private fun bounceInAnimation(imageView: ImageView) {
        val render = Render(requireContext())
        render.setAnimation(Bounce().In(imageView))
        render.setDuration(300L)
        render.start()
    }

    // 정답일 때 카드 흔드는 기능
    private fun bounceInCard(cardItem: CardItem?) {
        sharedViewModel.cardDataList.forEachIndexed { index, it ->
            if (it == cardItem) bounceInAnimation(bindingCardList[index])
        }
    }

    // 카드(이미지뷰) 흔드는 애니메이션
    private fun shakeAnimation(imageView: ImageView) {
        val render = Render(requireContext())
        render.setAnimation(Attention().Tada(imageView))
        render.setDuration(700L)
        render.start()
    }

    // 오답일 때 카드 흔드는 기능
    private fun shakeCard(cardItem: CardItem?) {
        sharedViewModel.cardDataList.forEachIndexed { index, it ->
            if (it == cardItem) shakeAnimation(bindingCardList[index])
        }
    }

    // 정답이면 selectedCard 와 동일한 카드를 찾는 코드
    private fun selectedCardMatchCardImage(cardItem: CardItem?) {
        sharedViewModel.cardDataList.forEachIndexed { index, it ->
            if (it == cardItem) connectImageToCard(bindingCardList[index])
        }
    }

    // 카드 선택시 노란색 표시
    private fun setVisibilitySelectedCard(cardItem: CardItem, visibility: Int) {
        sharedViewModel.cardDataList.forEachIndexed { index, it ->
            if (it == cardItem) bindingSelectedCardList[index].visibility = visibility
        }
    }

    // 카드 선택시 노란색 표시
    private fun visibleSelectedCard(cardItem: CardItem) {
        sharedViewModel.cardDataList.forEachIndexed { index, it ->
            if (it == cardItem) bindingSelectedCardList[index].visibility = View.VISIBLE
        }
    }

    // 카드 두번 선택시 노란색 해제 기능
    private fun invisibleSelectedCard(cardItem: CardItem) {
        sharedViewModel.cardDataList.forEachIndexed { index, it ->
            if (it == cardItem) bindingSelectedCardList[index].visibility = View.INVISIBLE
        }
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
                startGame()

                render.setAnimation(Slide().InLeft(binding.constraintLayout))
                render.setDuration(250L)
                render.start()
            }
        } else {
            Toast.makeText(activity, "남은카드가 없습니다", Toast.LENGTH_SHORT).show()
        }
    }

    // 끝내기 버튼 기능
    fun endButton() {
        showFinalScoreDialog()
    }

    // 끝내기 버튼 누르면 나오는 MaterialDialog
    @SuppressLint("SetTextI18n")
    fun showFinalScoreDialog() {

        // 다이얼로그를 띄웠으면 게임 저장 안하도록 하는 코드
        saveIndicator = false

        // 커스텀 Dialog 만들기
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.end_dialog)
        dialog.setCancelable(false)
        dialog.show()

        // Dialog 레이아웃의 뷰를 변수와 연결하기
        // 그냥 연결이 안되서 dialog 변수를 따로 만들고 거기서 findViewById해서 찾음
        val dialogTextView = dialog.findViewById<TextView>(R.id.dialog_textview)
        val dialogExitBtn = dialog.findViewById<TextView>(R.id.dialog_exit_btn)
        val dialogReplayBtn = dialog.findViewById<TextView>(R.id.dialog_replay_btn)

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
        dialogTextView.text = spannableString
        dialogExitBtn.setOnClickListener {
            activity?.finish()
        }
        dialogReplayBtn.setOnClickListener {
            sharedViewModel.resetAllValue()
            startGame()
            dialog.dismiss()
        }
    }

    private fun saveAndLoadGame(string: String) {
        val sharedPreferences =
            requireActivity().getSharedPreferences(KEY_PREFS, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Json 파일 변환을 위한 Gson 객체
        val gson = GsonBuilder().create()

        val typeUsedCardList: TypeToken<MutableList<CardItem>> =
            object : TypeToken<MutableList<CardItem>>() {}
        val typeCardDataList: TypeToken<Array<CardItem>> = object : TypeToken<Array<CardItem>>() {}

        when (string) {
            "save" -> {
                val jsonUsedCardList =
                    gson.toJson(sharedViewModel.usedCardList, typeUsedCardList.type)
                val jsonCardDataList =
                    gson.toJson(sharedViewModel.cardDataList, typeCardDataList.type)

                editor.putString(KEY_USED_CARD_LIST, jsonUsedCardList)
                editor.putString(KEY_CARD_DATA_LIST, jsonCardDataList)
                editor.putInt(KEY_SCORE, sharedViewModel.score.value!!)
                editor.apply()
            }
            "load" -> {
                val jsonUsedCardList = sharedPreferences.getString(KEY_USED_CARD_LIST, "")
                val jsonCardDataList = sharedPreferences.getString(KEY_CARD_DATA_LIST, "")

                sharedViewModel.usedCardList =
                    gson.fromJson(jsonUsedCardList, typeUsedCardList.type)
                sharedViewModel.cardDataList =
                    gson.fromJson(jsonCardDataList, typeCardDataList.type)
                sharedViewModel.score.value = sharedPreferences.getInt(KEY_SCORE, 0)
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        backPressCallback.remove()
    }
}