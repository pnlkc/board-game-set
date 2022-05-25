package com.example.set

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.set.databinding.SetFragmentBinding
import com.example.set.model.CardItem
import com.example.set.model.SetViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import render.animations.Attention
import render.animations.Render
import render.animations.Slide

class SetFragment : Fragment() {
    private var _binding: SetFragmentBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SetViewModel by activityViewModels()

    private lateinit var backPressCallback: OnBackPressedCallback

    private lateinit var bindingCardList: List<ImageView>
    private lateinit var bindingSelectedCardList: List<ImageView>

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
                if (System.currentTimeMillis() - backWait >= 1000) {
                    backWait = System.currentTimeMillis()

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

        startGame()

        // 카드 클릭시 코드
        binding.card1.setOnClickListener { clickImage(sharedViewModel.cardImage1) }
        binding.card2.setOnClickListener { clickImage(sharedViewModel.cardImage2) }
        binding.card3.setOnClickListener { clickImage(sharedViewModel.cardImage3) }
        binding.card4.setOnClickListener { clickImage(sharedViewModel.cardImage4) }
        binding.card5.setOnClickListener { clickImage(sharedViewModel.cardImage5) }
        binding.card6.setOnClickListener { clickImage(sharedViewModel.cardImage6) }
        binding.card7.setOnClickListener { clickImage(sharedViewModel.cardImage7) }
        binding.card8.setOnClickListener { clickImage(sharedViewModel.cardImage8) }
        binding.card9.setOnClickListener { clickImage(sharedViewModel.cardImage9) }
        binding.card10.setOnClickListener { clickImage(sharedViewModel.cardImage10) }
        binding.card11.setOnClickListener { clickImage(sharedViewModel.cardImage11) }
        binding.card12.setOnClickListener { clickImage(sharedViewModel.cardImage12) }

    }

    // 초기 12장 세팅
    private fun startGame() {
        bindingCardList.forEach {
            if (sharedViewModel.leftCard.value != 0) it.visibility = View.VISIBLE
            connectImageToCard(it)
        }
    }

    // startGame() 실행시 카드속성도 같이 연결
    private fun connectImageToCard(imageView: ImageView) {
        if (sharedViewModel.leftCard.value != 0) {
            sharedViewModel.changeCardImage(imageView)
            when (imageView) {
                binding.card1 -> {
                    sharedViewModel.cardImage1 = sharedViewModel.temp
                    sharedViewModel.cardImageList[0] = sharedViewModel.cardImage1
                }
                binding.card2 -> {
                    sharedViewModel.cardImage2 = sharedViewModel.temp
                    sharedViewModel.cardImageList[1] = sharedViewModel.cardImage2
                }
                binding.card3 -> {
                    sharedViewModel.cardImage3 = sharedViewModel.temp
                    sharedViewModel.cardImageList[2] = sharedViewModel.cardImage3
                }
                binding.card4 -> {
                    sharedViewModel.cardImage4 = sharedViewModel.temp
                    sharedViewModel.cardImageList[3] = sharedViewModel.cardImage4
                }
                binding.card5 -> {
                    sharedViewModel.cardImage5 = sharedViewModel.temp
                    sharedViewModel.cardImageList[4] = sharedViewModel.cardImage5
                }
                binding.card6 -> {
                    sharedViewModel.cardImage6 = sharedViewModel.temp
                    sharedViewModel.cardImageList[5] = sharedViewModel.cardImage6
                }
                binding.card7 -> {
                    sharedViewModel.cardImage7 = sharedViewModel.temp
                    sharedViewModel.cardImageList[6] = sharedViewModel.cardImage7
                }
                binding.card8 -> {
                    sharedViewModel.cardImage8 = sharedViewModel.temp
                    sharedViewModel.cardImageList[7] = sharedViewModel.cardImage8
                }
                binding.card9 -> {
                    sharedViewModel.cardImage9 = sharedViewModel.temp
                    sharedViewModel.cardImageList[8] = sharedViewModel.cardImage9
                }
                binding.card10 -> {
                    sharedViewModel.cardImage10 = sharedViewModel.temp
                    sharedViewModel.cardImageList[9] = sharedViewModel.cardImage10
                }
                binding.card11 -> {
                    sharedViewModel.cardImage11 = sharedViewModel.temp
                    sharedViewModel.cardImageList[10] = sharedViewModel.cardImage11
                }
                binding.card12 -> {
                    sharedViewModel.cardImage12 = sharedViewModel.temp
                    sharedViewModel.cardImageList[11] = sharedViewModel.cardImage12
                }
            }
        } else {
            bindingCardList.forEachIndexed { index, it ->
                if (imageView == it) {
                    it.visibility = View.INVISIBLE
                    sharedViewModel.cardImageList[index] =
                        CardItem(0, 0, 0, 0, 0)
                }
            }
        }
        sharedViewModel.showLeftCombination()
    }

    // 카드 클릭시 카드 값을 selectedCard에 저장하는 기능
    private fun clickImage(it: CardItem) {
        when {
            sharedViewModel.selectedCard1 == null -> {
                sharedViewModel.selectedCard1 = it
                visibleSelectedCard(sharedViewModel.selectedCard1!!)
            }

            sharedViewModel.selectedCard1 != null
                    && sharedViewModel.selectedCard1 != it
                    && sharedViewModel.selectedCard2 == null -> {
                sharedViewModel.selectedCard2 = it
                visibleSelectedCard(sharedViewModel.selectedCard2!!)
            }

            sharedViewModel.selectedCard1 != null
                    && sharedViewModel.selectedCard1 != it
                    && sharedViewModel.selectedCard2 != null
                    && sharedViewModel.selectedCard2 != it
                    && sharedViewModel.selectedCard3 == null -> {
                sharedViewModel.selectedCard3 = it
                visibleSelectedCard(sharedViewModel.selectedCard3!!)
                isCorrect()
            }

            sharedViewModel.selectedCard1 == it -> {
                invisibleSelectedCard(it)
                sharedViewModel.selectedCard1 = null
            }

            sharedViewModel.selectedCard2 == it -> {
                invisibleSelectedCard(it)
                sharedViewModel.selectedCard2 = null
            }

            sharedViewModel.selectedCard3 == it -> {
                invisibleSelectedCard(it)
                sharedViewModel.selectedCard3 = null
            }
        }
    }

    // 정답이면 카드 변경하는 코드
    private fun isCorrect() {
        sharedViewModel.checkCard(sharedViewModel.selectedCard1!!,
            sharedViewModel.selectedCard2!!,
            sharedViewModel.selectedCard3!!)
        sharedViewModel.increaseScore().apply {
            if (this) {
                selectedCardMatchCardImage(sharedViewModel.selectedCard1)
                selectedCardMatchCardImage(sharedViewModel.selectedCard2)
                selectedCardMatchCardImage(sharedViewModel.selectedCard3)

                if (sharedViewModel.leftCard.value == 0 && sharedViewModel.leftCombination.value == 0) {
                    showFinalScoreDialog()
                }

                invisibleAllSelectedCard()
                sharedViewModel.resetSelectedCard()
            } else {
                // 오답이면 카드 흔드는 기능
                shakeCard(sharedViewModel.selectedCard1)
                shakeCard(sharedViewModel.selectedCard2)
                shakeCard(sharedViewModel.selectedCard3)

                invisibleAllSelectedCard()
                sharedViewModel.resetSelectedCard()
            }
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
        when (cardItem) {
            sharedViewModel.cardImage1 -> shakeAnimation(binding.card1)
            sharedViewModel.cardImage2 -> shakeAnimation(binding.card2)
            sharedViewModel.cardImage3 -> shakeAnimation(binding.card3)
            sharedViewModel.cardImage4 -> shakeAnimation(binding.card4)
            sharedViewModel.cardImage5 -> shakeAnimation(binding.card5)
            sharedViewModel.cardImage6 -> shakeAnimation(binding.card6)
            sharedViewModel.cardImage7 -> shakeAnimation(binding.card7)
            sharedViewModel.cardImage8 -> shakeAnimation(binding.card8)
            sharedViewModel.cardImage9 -> shakeAnimation(binding.card9)
            sharedViewModel.cardImage10 -> shakeAnimation(binding.card10)
            sharedViewModel.cardImage11 -> shakeAnimation(binding.card11)
            sharedViewModel.cardImage12 -> shakeAnimation(binding.card12)
        }
    }

    // 정답이면 selectedCard 와 동일한 카드를 찾는 코드
    private fun selectedCardMatchCardImage(cardItem: CardItem?) {
        when (cardItem) {
            sharedViewModel.cardImage1 -> connectImageToCard(binding.card1)
            sharedViewModel.cardImage2 -> connectImageToCard(binding.card2)
            sharedViewModel.cardImage3 -> connectImageToCard(binding.card3)
            sharedViewModel.cardImage4 -> connectImageToCard(binding.card4)
            sharedViewModel.cardImage5 -> connectImageToCard(binding.card5)
            sharedViewModel.cardImage6 -> connectImageToCard(binding.card6)
            sharedViewModel.cardImage7 -> connectImageToCard(binding.card7)
            sharedViewModel.cardImage8 -> connectImageToCard(binding.card8)
            sharedViewModel.cardImage9 -> connectImageToCard(binding.card9)
            sharedViewModel.cardImage10 -> connectImageToCard(binding.card10)
            sharedViewModel.cardImage11 -> connectImageToCard(binding.card11)
            sharedViewModel.cardImage12 -> connectImageToCard(binding.card12)
        }
    }

    // 카드 선택시 노란색 표시
    private fun visibleSelectedCard(cardItem: CardItem) {
        when (cardItem) {
            sharedViewModel.cardImage1 -> binding.selectedCard1.visibility = View.VISIBLE
            sharedViewModel.cardImage2 -> binding.selectedCard2.visibility = View.VISIBLE
            sharedViewModel.cardImage3 -> binding.selectedCard3.visibility = View.VISIBLE
            sharedViewModel.cardImage4 -> binding.selectedCard4.visibility = View.VISIBLE
            sharedViewModel.cardImage5 -> binding.selectedCard5.visibility = View.VISIBLE
            sharedViewModel.cardImage6 -> binding.selectedCard6.visibility = View.VISIBLE
            sharedViewModel.cardImage7 -> binding.selectedCard7.visibility = View.VISIBLE
            sharedViewModel.cardImage8 -> binding.selectedCard8.visibility = View.VISIBLE
            sharedViewModel.cardImage9 -> binding.selectedCard9.visibility = View.VISIBLE
            sharedViewModel.cardImage10 -> binding.selectedCard10.visibility = View.VISIBLE
            sharedViewModel.cardImage11 -> binding.selectedCard11.visibility = View.VISIBLE
            sharedViewModel.cardImage12 -> binding.selectedCard12.visibility = View.VISIBLE
        }
    }

    // 카드 두번 선택시 노란색 해제 기능
    private fun invisibleSelectedCard(cardItem: CardItem) {
        when (cardItem) {
            sharedViewModel.cardImage1 -> binding.selectedCard1.visibility = View.INVISIBLE
            sharedViewModel.cardImage2 -> binding.selectedCard2.visibility = View.INVISIBLE
            sharedViewModel.cardImage3 -> binding.selectedCard3.visibility = View.INVISIBLE
            sharedViewModel.cardImage4 -> binding.selectedCard4.visibility = View.INVISIBLE
            sharedViewModel.cardImage5 -> binding.selectedCard5.visibility = View.INVISIBLE
            sharedViewModel.cardImage6 -> binding.selectedCard6.visibility = View.INVISIBLE
            sharedViewModel.cardImage7 -> binding.selectedCard7.visibility = View.INVISIBLE
            sharedViewModel.cardImage8 -> binding.selectedCard8.visibility = View.INVISIBLE
            sharedViewModel.cardImage9 -> binding.selectedCard9.visibility = View.INVISIBLE
            sharedViewModel.cardImage10 -> binding.selectedCard10.visibility = View.INVISIBLE
            sharedViewModel.cardImage11 -> binding.selectedCard11.visibility = View.INVISIBLE
            sharedViewModel.cardImage12 -> binding.selectedCard12.visibility = View.INVISIBLE
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

                delay(125L)
                sharedViewModel.resetSelectedCard()
                invisibleAllSelectedCard()
                sharedViewModel.shuffleCard()
                startGame()

                delay(150L)
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

    // 끝내기 버튼 누르면 MaterialDialog가 나오는 기능
    @SuppressLint("SetTextI18n")
    fun showFinalScoreDialog() {

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
        spannableString.setSpan(ForegroundColorSpan(Color.parseColor("#ae3b75")),
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        backPressCallback.remove()
    }
}