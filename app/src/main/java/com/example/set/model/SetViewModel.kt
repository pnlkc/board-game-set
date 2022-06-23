package com.example.set.model

import android.widget.ImageView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.set.data.DataSource

class SetViewModel : ViewModel() {

    private val allCardList = DataSource.allCardList

    var isContinueGame: Boolean = false

    // 점수 변수 (일단은 한개)
    private var _score = MutableLiveData(0)
    val score: MutableLiveData<Int> get() = _score

    // 남은 카드 변수
    private var _leftCard = MutableLiveData(81)
    val leftCard: MutableLiveData<Int> get() = _leftCard

    private var _leftCombination = MutableLiveData(0)
    val leftCombination: MutableLiveData<Int> get() = _leftCombination

    // 선택된 카드 3개
    var selectedCardList: Array<CardItem?> = arrayOf(null, null, null)

    // 선택된 카드 속성 확인용 변수 (shape, color, number, shade 순서)
    private var checkList = arrayOf(0, 0, 0, 0)

    // 사용된 카드 확인용 변수
    var usedCardList: MutableList<CardItem> = mutableListOf()
    private lateinit var currentCard: CardItem

    // ImageView 와 카드정보 연결용 변수
    lateinit var temp: CardItem

    // ImageView 와 카드정보 연결 및 남은 조합수 계산시 사용
    var cardDataList = Array(12) { CardItem(0, 0, 0, 0, 0) }

    // 다음 카드 받기
    private fun getNextCard() {
        if (_leftCard.value != 0) {
            currentCard = allCardList.values.toList().random()

            if (usedCardList.contains(currentCard)) {
                getNextCard()
            } else {
                usedCardList.add(currentCard)
                temp = currentCard
            }

            showLeftCard()
        }
    }

    // 남은 카드 수 계산 및 표시
    fun showLeftCard() {
        _leftCard.value = allCardList.size - usedCardList.size
    }

    // 다음 카드의 이미지값을 반환해주는 코드
    private fun setNextCardImageResource(): Int {
        getNextCard()
        return temp.cardImage
    }

    // 카드 이미지 바꾸는 코드
    fun changeCardImage(view: ImageView) {
        view.setImageResource(setNextCardImageResource())
    }

    // 카드 섞기 버튼 누르면 카드리스트에 지금 필드에 있는거를 제거해서 다시 뽑을 수 있도록 구현
    fun shuffleCard() {
        cardDataList.forEach { usedCardList.remove(it) }
    }

    // 선택된 카드 3개가 정답인지 확인하는 기능
    fun checkCard(card1: CardItem, card2: CardItem, card3: CardItem) {
        resetCheckValue()
        if (card1 != card2 && card2 != card3 && card1 != card3) {
            checkList[0] = when {
                card1.shape == card2.shape && card1.shape == card3.shape -> 1
                card1.shape != card2.shape
                        && card2.shape != card3.shape
                        && card1.shape != card3.shape -> 1
                else -> 0
            }

            checkList[1] = when {
                card1.color == card2.color && card1.color == card3.color -> 1
                card1.color != card2.color
                        && card2.color != card3.color
                        && card1.color != card3.color -> 1
                else -> 0
            }

            checkList[2] = when {
                card1.number == card2.number && card1.number == card3.number -> 1
                card1.number != card2.number
                        && card2.number != card3.number
                        && card1.number != card3.number -> 1
                else -> 0
            }

            checkList[3] = when {
                card1.shade == card2.shade && card1.shade == card3.shade -> 1
                card1.shade != card2.shade
                        && card2.shade != card3.shade
                        && card1.shade != card3.shade -> 1
                else -> 0
            }
        }
    }

    // 점수 올리는 기능 (현재는 1점씩 증가)
    fun increaseScore(): Boolean {
        return if (checkList.count {it == 1} == checkList.size) {
            _score.value = _score.value!! + 1
            resetCheckValue()
            true
        } else {
            resetCheckValue()
            false
        }
    }

    // 남은 조합수 계산 및 표시
    fun showLeftCombination() {
        _leftCombination.value = 0
        val tempList = cardDataList.distinct().toMutableList()
        tempList.remove(CardItem(0,0,0,0,0))

        if (tempList.size >= 3) {
            for (i in 0..tempList.size-3) {
                for (j in i + 1..tempList.size-2) {
                    for (k in j + 1 until tempList.size) {
                        checkCard(tempList[i], tempList[j], tempList[k])
                        if (checkList.count {it == 1} == checkList.size) {
                            _leftCombination.value = _leftCombination.value!! + 1
                            resetCheckValue()
                        } else {
                            resetCheckValue()
                        }
                    }
                }
            }
        }
    }

    fun resetSelectedCard() {
        selectedCardList = arrayOf(null, null, null)
    }

    private fun resetCheckValue() {
        checkList = arrayOf(0, 0, 0, 0)
    }

    fun resetAllValue() {
        resetCheckValue()
        resetSelectedCard()
        _leftCard.value = 81
        usedCardList.clear()
        _score.value = 0
    }
}
