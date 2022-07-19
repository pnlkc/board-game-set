package com.pnlkc.set.data

import com.pnlkc.set.R
import com.pnlkc.set.model.CardItem

enum class UserMode {
    HOST, CLIENT
}

enum class GameState {
    EXIT, WAIT, READY, START, END
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
            CardItem(1, 1, 1, 1, R.drawable.aaaa),
            CardItem(1, 1, 1, 2, R.drawable.aaab),
            CardItem(1, 1, 1, 3, R.drawable.aaac),
            CardItem(1, 1, 2, 1, R.drawable.aaba),
            CardItem(1, 1, 2, 2, R.drawable.aabb),
            CardItem(1, 1, 2, 3, R.drawable.aabc),
            CardItem(1, 1, 3, 1, R.drawable.aaca),
            CardItem(1, 1, 3, 2, R.drawable.aacb),
            CardItem(1, 1, 3, 3, R.drawable.aacc),
            CardItem(1, 2, 1, 1, R.drawable.abaa),
            CardItem(1, 2, 1, 2, R.drawable.abab),
            CardItem(1, 2, 1, 3, R.drawable.abac),
            CardItem(1, 2, 2, 1, R.drawable.abba),
            CardItem(1, 2, 2, 2, R.drawable.abbb),
            CardItem(1, 2, 2, 3, R.drawable.abbc),
            CardItem(1, 2, 3, 1, R.drawable.abca),
            CardItem(1, 2, 3, 2, R.drawable.abcb),
            CardItem(1, 2, 3, 3, R.drawable.abcc),
            CardItem(1, 3, 1, 1, R.drawable.acaa),
            CardItem(1, 3, 1, 2, R.drawable.acab),
            CardItem(1, 3, 1, 3, R.drawable.acac),
            CardItem(1, 3, 2, 1, R.drawable.acba),
            CardItem(1, 3, 2, 2, R.drawable.acbb),
            CardItem(1, 3, 2, 3, R.drawable.acbc),
            CardItem(1, 3, 3, 1, R.drawable.acca),
            CardItem(1, 3, 3, 2, R.drawable.accb),
            CardItem(1, 3, 3, 3, R.drawable.accc),
            CardItem(2, 1, 1, 1, R.drawable.baaa),
            CardItem(2, 1, 1, 2, R.drawable.baab),
            CardItem(2, 1, 1, 3, R.drawable.baac),
            CardItem(2, 1, 2, 1, R.drawable.baba),
            CardItem(2, 1, 2, 2, R.drawable.babb),
            CardItem(2, 1, 2, 3, R.drawable.babc),
            CardItem(2, 1, 3, 1, R.drawable.baca),
            CardItem(2, 1, 3, 2, R.drawable.bacb),
            CardItem(2, 1, 3, 3, R.drawable.bacc),
            CardItem(2, 2, 1, 1, R.drawable.bbaa),
            CardItem(2, 2, 1, 2, R.drawable.bbab),
            CardItem(2, 2, 1, 3, R.drawable.bbac),
            CardItem(2, 2, 2, 1, R.drawable.bbba),
            CardItem(2, 2, 2, 2, R.drawable.bbbb),
            CardItem(2, 2, 2, 3, R.drawable.bbbc),
            CardItem(2, 2, 3, 1, R.drawable.bbca),
            CardItem(2, 2, 3, 2, R.drawable.bbcb),
            CardItem(2, 2, 3, 3, R.drawable.bbcc),
            CardItem(2, 3, 1, 1, R.drawable.bcaa),
            CardItem(2, 3, 1, 2, R.drawable.bcab),
            CardItem(2, 3, 1, 3, R.drawable.bcac),
            CardItem(2, 3, 2, 1, R.drawable.bcba),
            CardItem(2, 3, 2, 2, R.drawable.bcbb),
            CardItem(2, 3, 2, 3, R.drawable.bcbc),
            CardItem(2, 3, 3, 1, R.drawable.bcca),
            CardItem(2, 3, 3, 2, R.drawable.bccb),
            CardItem(2, 3, 3, 3, R.drawable.bccc),
            CardItem(3, 1, 1, 1, R.drawable.caaa),
            CardItem(3, 1, 1, 2, R.drawable.caab),
            CardItem(3, 1, 1, 3, R.drawable.caac),
            CardItem(3, 1, 2, 1, R.drawable.caba),
            CardItem(3, 1, 2, 2, R.drawable.cabb),
            CardItem(3, 1, 2, 3, R.drawable.cabc),
            CardItem(3, 1, 3, 1, R.drawable.caca),
            CardItem(3, 1, 3, 2, R.drawable.cacb),
            CardItem(3, 1, 3, 3, R.drawable.cacc),
            CardItem(3, 2, 1, 1, R.drawable.cbaa),
            CardItem(3, 2, 1, 2, R.drawable.cbab),
            CardItem(3, 2, 1, 3, R.drawable.cbac),
            CardItem(3, 2, 2, 1, R.drawable.cbba),
            CardItem(3, 2, 2, 2, R.drawable.cbbb),
            CardItem(3, 2, 2, 3, R.drawable.cbbc),
            CardItem(3, 2, 3, 1, R.drawable.cbca),
            CardItem(3, 2, 3, 2, R.drawable.cbcb),
            CardItem(3, 2, 3, 3, R.drawable.cbcc),
            CardItem(3, 3, 1, 1, R.drawable.ccaa),
            CardItem(3, 3, 1, 2, R.drawable.ccab),
            CardItem(3, 3, 1, 3, R.drawable.ccac),
            CardItem(3, 3, 2, 1, R.drawable.ccba),
            CardItem(3, 3, 2, 2, R.drawable.ccbb),
            CardItem(3, 3, 2, 3, R.drawable.ccbc),
            CardItem(3, 3, 3, 1, R.drawable.ccca),
            CardItem(3, 3, 3, 2, R.drawable.cccb),
            CardItem(3, 3, 3, 3, R.drawable.cccc)
        )
}

