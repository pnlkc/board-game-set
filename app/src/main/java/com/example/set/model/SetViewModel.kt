package com.example.set.model

import android.widget.ImageView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.set.data.DataSource
import java.util.ArrayList

class SetViewModel : ViewModel() {

    private val allCardList = DataSource.allCardList

    // 점수 변수 (일단은 한개)
    private var _score = MutableLiveData(0)
    val score: MutableLiveData<Int> get() = _score

    // 남은 카드 변수
    private var _leftCard = MutableLiveData(81)
    val leftCard: MutableLiveData<Int> = _leftCard

    var _leftCombination = MutableLiveData(0)
    val leftCombination: MutableLiveData<Int> = _leftCombination

    // 선택된 카드 3개
    var selectedCard1: CardItem? = null
    var selectedCard2: CardItem? = null
    var selectedCard3: CardItem? = null

    // 선택된 카드 속성 확인용 변수
    private var checkShape = 0
    private var checkColor = 0
    private var checkNumber = 0
    private var checkShade = 0

    // 사용된 카드 확인용 변수
    private var cardList: MutableList<CardItem> = mutableListOf()
    private lateinit var currentCard: CardItem
    private var tempCardList: MutableList<CardItem> = mutableListOf()

    // ImageView 와 카드정보 연결용 변수
    lateinit var temp: CardItem
    var cardImage1: CardItem = CardItem(0, 0, 0, 0, 0)
    var cardImage2: CardItem = CardItem(0, 0, 0, 0, 0)
    var cardImage3: CardItem = CardItem(0, 0, 0, 0, 0)
    var cardImage4: CardItem = CardItem(0, 0, 0, 0, 0)
    var cardImage5: CardItem = CardItem(0, 0, 0, 0, 0)
    var cardImage6: CardItem = CardItem(0, 0, 0, 0, 0)
    var cardImage7: CardItem = CardItem(0, 0, 0, 0, 0)
    var cardImage8: CardItem = CardItem(0, 0, 0, 0, 0)
    var cardImage9: CardItem = CardItem(0, 0, 0, 0, 0)
    var cardImage10: CardItem = CardItem(0, 0, 0, 0, 0)
    var cardImage11: CardItem = CardItem(0, 0, 0, 0, 0)
    var cardImage12: CardItem = CardItem(0, 0, 0, 0, 0)

    var cardImageList: MutableList<CardItem> = mutableListOf(
        cardImage1,
        cardImage2,
        cardImage3,
        cardImage4,
        cardImage5,
        cardImage6,
        cardImage7,
        cardImage8,
        cardImage9,
        cardImage10,
        cardImage11,
        cardImage12
    )


    // 다음 카드 받기
    private fun getNextCard() {
        if (_leftCard.value != 0) {
            currentCard = allCardList.values.toList().random()

            if (cardList.contains(currentCard)) {
                getNextCard()
            } else {
                cardList.add(currentCard)
                tempCardList.add(currentCard)
            }

            _leftCard.value = allCardList.size - cardList.size
        }
    }

    // 다음 카드의 이미지값을 반환해주는 코드
    private fun setNextCardImageResource(): Int {
        getNextCard()
        temp = tempCardList.random()
        tempCardList.remove(temp)
        return temp.cardImage
    }

    // 카드 섞기 버튼 누르면 카드리스트에 지금 필드에 있는거를 제거해서 다시 뽑을 수 있도록 구현
    fun shuffleCard() {
        cardList.remove(cardImage1)
        cardList.remove(cardImage2)
        cardList.remove(cardImage3)
        cardList.remove(cardImage4)
        cardList.remove(cardImage5)
        cardList.remove(cardImage6)
        cardList.remove(cardImage7)
        cardList.remove(cardImage8)
        cardList.remove(cardImage9)
        cardList.remove(cardImage10)
        cardList.remove(cardImage11)
        cardList.remove(cardImage12)

    }

    // 카드 이미지 바꾸는 코드
    fun changeCardImage(view: ImageView) {
        view.setImageResource(setNextCardImageResource())
    }


    // 선택된 카드 3개가 정답인지 확인하는 기능
    fun checkCard(
        selectedCard1: CardItem,
        selectedCard2: CardItem,
        selectedCard3: CardItem
    ) {
        resetCheckValue()
        if (selectedCard1 != selectedCard2
            && selectedCard2 != selectedCard3
            && selectedCard1 != selectedCard3
        ) {
            checkShape = when {
                selectedCard1.shape == selectedCard2.shape
                        && selectedCard1.shape == selectedCard3.shape
                -> 1
                selectedCard1.shape != selectedCard2.shape
                        && selectedCard2.shape != selectedCard3.shape
                        && selectedCard1.shape != selectedCard3.shape
                -> 1
                else -> 0
            }

            checkColor = when {
                selectedCard1.color == selectedCard2.color
                        && selectedCard1.color == selectedCard3.color
                -> 1
                selectedCard1.color != selectedCard2.color
                        && selectedCard2.color != selectedCard3.color
                        && selectedCard1.color != selectedCard3.color
                -> 1
                else -> 0
            }

            checkNumber = when {
                selectedCard1.number == selectedCard2.number
                        && selectedCard1.number == selectedCard3.number
                -> 1
                selectedCard1.number != selectedCard2.number
                        && selectedCard2.number != selectedCard3.number
                        && selectedCard1.number != selectedCard3.number
                -> 1
                else -> 0
            }

            checkShade = when {
                selectedCard1.shade == selectedCard2.shade
                        && selectedCard1.shade == selectedCard3.shade
                -> 1
                selectedCard1.shade != selectedCard2.shade
                        && selectedCard2.shade != selectedCard3.shade
                        && selectedCard1.shade != selectedCard3.shade
                -> 1
                else -> 0
            }
        }
    }

    // 점수 올리는 기능 (현재는 1점)
    fun increaseScore(): Boolean {
        return if (checkShape == 1 && checkColor == 1 && checkNumber == 1 && checkShade == 1) {
            _score.value = _score.value!! + 1
            resetCheckValue()
            true
        } else {
            resetCheckValue()
            false
        }
    }

    // 남은 카드수 표시
    fun showLeftCombination() {
        for (i in 0..11) {
            for (j in 0..11) {
                for (k in 0..11) {
                    if (i != j && j != k && i != k) {
                        checkCard(cardImageList[i], cardImageList[j], cardImageList[k])
                        if (checkShape == 1 && checkColor == 1 && checkNumber == 1 && checkShade == 1) {
                            _leftCombination.value = _leftCombination.value!! + 1
                            resetCheckValue()
                        } else {
                            resetCheckValue()
                        }
                    }
                }
            }
        }
        _leftCombination.value = _leftCombination.value!! / 6
    }

    fun resetSelectedCard() {
        selectedCard1 = null
        selectedCard2 = null
        selectedCard3 = null
    }

    private fun resetCheckValue() {
        checkShape = 0
        checkColor = 0
        checkNumber = 0
        checkShade = 0
    }

    fun resetAllValue() {
        checkShape = 0
        checkColor = 0
        checkNumber = 0
        checkShade = 0
        selectedCard1 = null
        selectedCard2 = null
        selectedCard3 = null
        _leftCard.value = 81
        cardList = mutableListOf()
        tempCardList = mutableListOf()
    }

    fun resetScore() {
        _score.value = 0
    }
}
