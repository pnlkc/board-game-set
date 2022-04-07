package com.example.set

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.set.databinding.SetFragmentBinding
import com.example.set.model.CardItem
import com.example.set.model.SetViewModel

class SetFragment : Fragment() {
    private var _binding: SetFragmentBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SetViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SetFragmentBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = sharedViewModel
            setFragment = this@SetFragment

            sharedViewModel.resetAllValue()
            sharedViewModel.resetScore()

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
    }


    // 초기 12장 세팅
    private fun startGame() {
        connectImageToCard(binding.card1)
        connectImageToCard(binding.card2)
        connectImageToCard(binding.card3)
        connectImageToCard(binding.card4)
        connectImageToCard(binding.card5)
        connectImageToCard(binding.card6)
        connectImageToCard(binding.card7)
        connectImageToCard(binding.card8)
        connectImageToCard(binding.card9)
        connectImageToCard(binding.card10)
        connectImageToCard(binding.card11)
        connectImageToCard(binding.card12)
    }

    // settingCard 실행시 카드속성도 같이 연결
    private fun connectImageToCard(imageView: ImageView) {
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

        sharedViewModel._leftCombination.value = 0
        sharedViewModel.showLeftCombination()
        println("조합 가능한 숫자 = ${sharedViewModel._leftCombination.value}")

        if (sharedViewModel.leftCard.value == 0) {
            Toast.makeText(activity, "남은 카드가 0장입니다", Toast.LENGTH_SHORT).show()
        }
    }

    // 카드 클릭시 카드 값을 selectedCardN에 저장하는 기능
    private fun clickImage(it: CardItem) {
        when {
            sharedViewModel.selectedCard1 == null
            -> {
                sharedViewModel.selectedCard1 = it
                visibleSelectedCard(sharedViewModel.selectedCard1!!)
            }

            sharedViewModel.selectedCard1 != null
                    && sharedViewModel.selectedCard1 != it
                    && sharedViewModel.selectedCard2 == null
            -> {
                sharedViewModel.selectedCard2 = it
                visibleSelectedCard(sharedViewModel.selectedCard2!!)
            }

            sharedViewModel.selectedCard1 != null
                    && sharedViewModel.selectedCard1 != it
                    && sharedViewModel.selectedCard2 != null
                    && sharedViewModel.selectedCard2 != it
                    && sharedViewModel.selectedCard3 == null
            -> {
                sharedViewModel.selectedCard3 = it
                visibleSelectedCard(sharedViewModel.selectedCard3!!)
                isCorrect()
            }

            sharedViewModel.selectedCard1 == it
            -> {
                invisibleSelectedCard(it)
                sharedViewModel.selectedCard1 = null
            }

            sharedViewModel.selectedCard2 == it
            -> {
                invisibleSelectedCard(it)
                sharedViewModel.selectedCard2 = null
            }

            sharedViewModel.selectedCard3 == it
            -> {
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
                Toast.makeText(activity, "정답", Toast.LENGTH_SHORT).show()
                selectedCardMatchCardImage(sharedViewModel.selectedCard1)
                selectedCardMatchCardImage(sharedViewModel.selectedCard2)
                selectedCardMatchCardImage(sharedViewModel.selectedCard3)
                invisibleAllSelectedCard()
                sharedViewModel.resetSelectedCard()
            } else {
                Toast.makeText(activity, "오답", Toast.LENGTH_SHORT).show()
                invisibleAllSelectedCard()
                sharedViewModel.resetSelectedCard()
            }
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
        binding.selectedCard1.visibility = View.INVISIBLE
        binding.selectedCard2.visibility = View.INVISIBLE
        binding.selectedCard3.visibility = View.INVISIBLE
        binding.selectedCard4.visibility = View.INVISIBLE
        binding.selectedCard5.visibility = View.INVISIBLE
        binding.selectedCard6.visibility = View.INVISIBLE
        binding.selectedCard7.visibility = View.INVISIBLE
        binding.selectedCard8.visibility = View.INVISIBLE
        binding.selectedCard9.visibility = View.INVISIBLE
        binding.selectedCard10.visibility = View.INVISIBLE
        binding.selectedCard11.visibility = View.INVISIBLE
        binding.selectedCard12.visibility = View.INVISIBLE
    }

    // 카드섞기 버튼 기능
    fun shuffleButton() {
        sharedViewModel.resetSelectedCard()
        invisibleAllSelectedCard()
        sharedViewModel.shuffleCard()
        startGame()
    }

    // 끝내기 버튼 기능
    fun endButton() {
        sharedViewModel.resetAllValue()
        findNavController().navigate(R.id.action_setFragment_to_endFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}