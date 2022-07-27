package com.pnlkc.set.data

import com.pnlkc.set.R
import com.pnlkc.set.model.CardItem

enum class UserMode {
    HOST, CLIENT
}

enum class GameState {
    WAIT, READY, START
}

object DataSource {
    // sharedPreference 키
    const val KEY_PREFS = "set_prefs"
    const val KEY_SHUFFLED_CARD_LIST = "shuffled_card_list"
    const val KEY_FIELD_CARD_LIST = "field_card_list"
    const val KEY_SCORE = "score"

    // 카드 81장 정보
    val allCardList: List<CardItem> =
        listOf(
            CardItem(1, 1, 1, 1, "aaaa"),
            CardItem(1, 1, 1, 2, "aaab"),
            CardItem(1, 1, 1, 3, "aaac"),
            CardItem(1, 1, 2, 1, "aaba"),
            CardItem(1, 1, 2, 2, "aabb"),
            CardItem(1, 1, 2, 3, "aabc"),
            CardItem(1, 1, 3, 1, "aaca"),
            CardItem(1, 1, 3, 2, "aacb"),
            CardItem(1, 1, 3, 3, "aacc"),
            CardItem(1, 2, 1, 1, "abaa"),
            CardItem(1, 2, 1, 2, "abab"),
            CardItem(1, 2, 1, 3, "abac"),
            CardItem(1, 2, 2, 1, "abba"),
            CardItem(1, 2, 2, 2, "abbb"),
            CardItem(1, 2, 2, 3, "abbc"),
            CardItem(1, 2, 3, 1, "abca"),
            CardItem(1, 2, 3, 2, "abcb"),
            CardItem(1, 2, 3, 3, "abcc"),
            CardItem(1, 3, 1, 1, "acaa"),
            CardItem(1, 3, 1, 2, "acab"),
            CardItem(1, 3, 1, 3, "acac"),
            CardItem(1, 3, 2, 1, "acba"),
            CardItem(1, 3, 2, 2, "acbb"),
            CardItem(1, 3, 2, 3, "acbc"),
            CardItem(1, 3, 3, 1, "acca"),
            CardItem(1, 3, 3, 2, "accb"),
            CardItem(1, 3, 3, 3, "accc"),
            CardItem(2, 1, 1, 1, "baaa"),
            CardItem(2, 1, 1, 2, "baab"),
            CardItem(2, 1, 1, 3, "baac"),
            CardItem(2, 1, 2, 1, "baba"),
            CardItem(2, 1, 2, 2, "babb"),
            CardItem(2, 1, 2, 3, "babc"),
            CardItem(2, 1, 3, 1, "baca"),
            CardItem(2, 1, 3, 2, "bacb"),
            CardItem(2, 1, 3, 3, "bacc"),
            CardItem(2, 2, 1, 1, "bbaa"),
            CardItem(2, 2, 1, 2, "bbab"),
            CardItem(2, 2, 1, 3, "bbac"),
            CardItem(2, 2, 2, 1, "bbba"),
            CardItem(2, 2, 2, 2, "bbbb"),
            CardItem(2, 2, 2, 3, "bbbc"),
            CardItem(2, 2, 3, 1, "bbca"),
            CardItem(2, 2, 3, 2, "bbcb"),
            CardItem(2, 2, 3, 3, "bbcc"),
            CardItem(2, 3, 1, 1, "bcaa"),
            CardItem(2, 3, 1, 2, "bcab"),
            CardItem(2, 3, 1, 3, "bcac"),
            CardItem(2, 3, 2, 1, "bcba"),
            CardItem(2, 3, 2, 2, "bcbb"),
            CardItem(2, 3, 2, 3, "bcbc"),
            CardItem(2, 3, 3, 1, "bcca"),
            CardItem(2, 3, 3, 2, "bccb"),
            CardItem(2, 3, 3, 3, "bccc"),
            CardItem(3, 1, 1, 1, "caaa"),
            CardItem(3, 1, 1, 2, "caab"),
            CardItem(3, 1, 1, 3, "caac"),
            CardItem(3, 1, 2, 1, "caba"),
            CardItem(3, 1, 2, 2, "cabb"),
            CardItem(3, 1, 2, 3, "cabc"),
            CardItem(3, 1, 3, 1, "caca"),
            CardItem(3, 1, 3, 2, "cacb"),
            CardItem(3, 1, 3, 3, "cacc"),
            CardItem(3, 2, 1, 1, "cbaa"),
            CardItem(3, 2, 1, 2, "cbab"),
            CardItem(3, 2, 1, 3, "cbac"),
            CardItem(3, 2, 2, 1, "cbba"),
            CardItem(3, 2, 2, 2, "cbbb"),
            CardItem(3, 2, 2, 3, "cbbc"),
            CardItem(3, 2, 3, 1, "cbca"),
            CardItem(3, 2, 3, 2, "cbcb"),
            CardItem(3, 2, 3, 3, "cbcc"),
            CardItem(3, 3, 1, 1, "ccaa"),
            CardItem(3, 3, 1, 2, "ccab"),
            CardItem(3, 3, 1, 3, "ccac"),
            CardItem(3, 3, 2, 1, "ccba"),
            CardItem(3, 3, 2, 2, "ccbb"),
            CardItem(3, 3, 2, 3, "ccbc"),
            CardItem(3, 3, 3, 1, "ccca"),
            CardItem(3, 3, 3, 2, "cccb"),
            CardItem(3, 3, 3, 3, "cccc")
        )
}