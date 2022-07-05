package com.pnlkc.set.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pnlkc.set.data.DataSource.allCardList

class SetViewModel : ViewModel() {

    // 셔플된 카드 리스트
    var shuffledCardList = allCardList.shuffled().toMutableList()

    var isContinueGame: Boolean = false

    // 점수 변수 (일단은 한개)
    private var _score = MutableLiveData(0)
    val score: MutableLiveData<Int> get() = _score

    // 필드에 있는 카드
    var fieldCardList = mutableListOf<CardItem>()

    // 남은 카드수 변수
    private val _leftCard = MutableLiveData(69)
    val leftCard: MutableLiveData<Int> get() = _leftCard

    // 남은 조합수 변수
    private var _leftCombination = MutableLiveData(0)
    val leftCombination: MutableLiveData<Int> get() = _leftCombination

    var selectedCardIndex: Array<Int?> = arrayOf(null, null, null)

    // 선택된 카드 정답 확인용 변수 (shape, color, number, shade 순서)
    private var checkList = arrayOf(0, 0, 0, 0)

    init {
        initCard()
    }

    // 초기 카드 목록 세팅
    private fun initCard() {
        fieldCardList.clear()
        repeat (12) {
            fieldCardList.add(shuffledCardList.first())
            shuffledCardList.removeFirst()
        }
    }

    // 다음 카드 받기
    private fun getNextCard() {
        if (shuffledCardList.isNotEmpty()) {
            fieldCardList[selectedCardIndex[0]!!] = shuffledCardList.first()
            shuffledCardList.removeFirst()
            fieldCardList[selectedCardIndex[1]!!] = shuffledCardList.first()
            shuffledCardList.removeFirst()
            fieldCardList[selectedCardIndex[2]!!] = shuffledCardList.first()
            shuffledCardList.removeFirst()
        } else {
            fieldCardList[selectedCardIndex[0]!!] = CardItem(0, 0, 0, 0, 0)
            fieldCardList[selectedCardIndex[1]!!] = CardItem(0, 0, 0, 0, 0)
            fieldCardList[selectedCardIndex[2]!!] = CardItem(0, 0, 0, 0, 0)
        }
    }

    // 카드 섞기 버튼 누르면 카드리스트에 지금 필드에 있는거를 제거해서 다시 뽑을 수 있도록 구현
    fun shuffleCard() {
        shuffledCardList.addAll(fieldCardList)
        shuffledCardList = shuffledCardList.shuffled().toMutableList()
        initCard()
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
        return if (!checkList.contains(0)) {
            resetCheckValue()
            getNextCard()
            _score.value = _score.value!! + 1
            true
        } else {
            resetCheckValue()
            false
        }

    }

    // 남은 카드수 표시 및 조합수 표시
    fun showLeftCardAndCombination() {
        // 남은 카드수 표시
        _leftCard.value = shuffledCardList.size

        // 남은 조합수 계산 및 표시
//        _leftCombination.value = 0
        var count = 0
        // .toMutableList()를 붙이지 않으면 tempList 값 변경시 fieldList 값이 같이 바뀜
        val tempList = fieldCardList.distinct().toMutableList()
        tempList.remove(CardItem(0,0,0,0,0))
        if (tempList.size >= 3) {
            for (i in 0..tempList.size-3) {
                for (j in i + 1..tempList.size-2) {
                    for (k in j + 1 until tempList.size) {
                        checkCard(tempList[i], tempList[j], tempList[k])
                        if (!checkList.contains(0)) {
                            count++
//                            _leftCombination.value = _leftCombination.value!! + 1
                            resetCheckValue()
                        } else {
                            resetCheckValue()
                        }
                    }
                }
            }
        }
        _leftCombination.value = count
    }

    fun resetSelectedCard() {
        selectedCardIndex = arrayOf(null, null, null)
    }

    private fun resetCheckValue() {
        checkList = arrayOf(0, 0, 0, 0)
    }

    fun resetAllValue() {
        shuffledCardList = allCardList.shuffled().toMutableList()
        initCard()
        _leftCard.value = 69
        _score.value = 0
    }
}
