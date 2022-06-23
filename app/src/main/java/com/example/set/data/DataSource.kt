package com.example.set.data

import com.example.set.R
import com.example.set.model.CardItem

object DataSource {
    // sharedPreference 키
    const val KEY_PREFS = "set_prefs"
    const val KEY_USED_CARD_LIST = "used_card_list"
    const val KEY_CARD_DATA_LIST = "card_data_list"
    const val KEY_SCORE = "score"

    // 카드 81장 정보
    val allCardList: Map<String, CardItem> =
        mapOf(
            "card1" to CardItem(1, 1, 1, 1, R.drawable.aaaa),
            "card2" to CardItem(1, 1, 1, 2, R.drawable.aaab),
            "card3" to CardItem(1, 1, 1, 3, R.drawable.aaac),
            "card4" to CardItem(1, 1, 2, 1, R.drawable.aaba),
            "card5" to CardItem(1, 1, 2, 2, R.drawable.aabb),
            "card6" to CardItem(1, 1, 2, 3, R.drawable.aabc),
            "card7" to CardItem(1, 1, 3, 1, R.drawable.aaca),
            "card8" to CardItem(1, 1, 3, 2, R.drawable.aacb),
            "card9" to CardItem(1, 1, 3, 3, R.drawable.aacc),
            "card10" to CardItem(1, 2, 1, 1, R.drawable.abaa),
            "card11" to CardItem(1, 2, 1, 2, R.drawable.abab),
            "card12" to CardItem(1, 2, 1, 3, R.drawable.abac),
            "card13" to CardItem(1, 2, 2, 1, R.drawable.abba),
            "card14" to CardItem(1, 2, 2, 2, R.drawable.abbb),
            "card15" to CardItem(1, 2, 2, 3, R.drawable.abbc),
            "card16" to CardItem(1, 2, 3, 1, R.drawable.abca),
            "card17" to CardItem(1, 2, 3, 2, R.drawable.abcb),
            "card18" to CardItem(1, 2, 3, 3, R.drawable.abcc),
            "card19" to CardItem(1, 3, 1, 1, R.drawable.acaa),
            "card20" to CardItem(1, 3, 1, 2, R.drawable.acab),
            "card21" to CardItem(1, 3, 1, 3, R.drawable.acac),
            "card22" to CardItem(1, 3, 2, 1, R.drawable.acba),
            "card23" to CardItem(1, 3, 2, 2, R.drawable.acbb),
            "card24" to CardItem(1, 3, 2, 3, R.drawable.acbc),
            "card25" to CardItem(1, 3, 3, 1, R.drawable.acca),
            "card26" to CardItem(1, 3, 3, 2, R.drawable.accb),
            "card27" to CardItem(1, 3, 3, 3, R.drawable.accc),
            "card28" to CardItem(2, 1, 1, 1, R.drawable.baaa),
            "card29" to CardItem(2, 1, 1, 2, R.drawable.baab),
            "card30" to CardItem(2, 1, 1, 3, R.drawable.baac),
            "card31" to CardItem(2, 1, 2, 1, R.drawable.baba),
            "card32" to CardItem(2, 1, 2, 2, R.drawable.babb),
            "card33" to CardItem(2, 1, 2, 3, R.drawable.babc),
            "card34" to CardItem(2, 1, 3, 1, R.drawable.baca),
            "card35" to CardItem(2, 1, 3, 2, R.drawable.bacb),
            "card36" to CardItem(2, 1, 3, 3, R.drawable.bacc),
            "card37" to CardItem(2, 2, 1, 1, R.drawable.bbaa),
            "card38" to CardItem(2, 2, 1, 2, R.drawable.bbab),
            "card39" to CardItem(2, 2, 1, 3, R.drawable.bbac),
            "card40" to CardItem(2, 2, 2, 1, R.drawable.bbba),
            "card41" to CardItem(2, 2, 2, 2, R.drawable.bbbb),
            "card42" to CardItem(2, 2, 2, 3, R.drawable.bbbc),
            "card43" to CardItem(2, 2, 3, 1, R.drawable.bbca),
            "card44" to CardItem(2, 2, 3, 2, R.drawable.bbcb),
            "card45" to CardItem(2, 2, 3, 3, R.drawable.bbcc),
            "card46" to CardItem(2, 3, 1, 1, R.drawable.bcaa),
            "card47" to CardItem(2, 3, 1, 2, R.drawable.bcab),
            "card48" to CardItem(2, 3, 1, 3, R.drawable.bcac),
            "card49" to CardItem(2, 3, 2, 1, R.drawable.bcba),
            "card50" to CardItem(2, 3, 2, 2, R.drawable.bcbb),
            "card51" to CardItem(2, 3, 2, 3, R.drawable.bcbc),
            "card52" to CardItem(2, 3, 3, 1, R.drawable.bcca),
            "card53" to CardItem(2, 3, 3, 2, R.drawable.bccb),
            "card54" to CardItem(2, 3, 3, 3, R.drawable.bccc),
            "card55" to CardItem(3, 1, 1, 1, R.drawable.caaa),
            "card56" to CardItem(3, 1, 1, 2, R.drawable.caab),
            "card57" to CardItem(3, 1, 1, 3, R.drawable.caac),
            "card58" to CardItem(3, 1, 2, 1, R.drawable.caba),
            "card59" to CardItem(3, 1, 2, 2, R.drawable.cabb),
            "card60" to CardItem(3, 1, 2, 3, R.drawable.cabc),
            "card61" to CardItem(3, 1, 3, 1, R.drawable.caca),
            "card62" to CardItem(3, 1, 3, 2, R.drawable.cacb),
            "card63" to CardItem(3, 1, 3, 3, R.drawable.cacc),
            "card64" to CardItem(3, 2, 1, 1, R.drawable.cbaa),
            "card65" to CardItem(3, 2, 1, 2, R.drawable.cbab),
            "card66" to CardItem(3, 2, 1, 3, R.drawable.cbac),
            "card67" to CardItem(3, 2, 2, 1, R.drawable.cbba),
            "card68" to CardItem(3, 2, 2, 2, R.drawable.cbbb),
            "card69" to CardItem(3, 2, 2, 3, R.drawable.cbbc),
            "card70" to CardItem(3, 2, 3, 1, R.drawable.cbca),
            "card71" to CardItem(3, 2, 3, 2, R.drawable.cbcb),
            "card72" to CardItem(3, 2, 3, 3, R.drawable.cbcc),
            "card73" to CardItem(3, 3, 1, 1, R.drawable.ccaa),
            "card74" to CardItem(3, 3, 1, 2, R.drawable.ccab),
            "card75" to CardItem(3, 3, 1, 3, R.drawable.ccac),
            "card76" to CardItem(3, 3, 2, 1, R.drawable.ccba),
            "card77" to CardItem(3, 3, 2, 2, R.drawable.ccbb),
            "card78" to CardItem(3, 3, 2, 3, R.drawable.ccbc),
            "card79" to CardItem(3, 3, 3, 1, R.drawable.ccca),
            "card80" to CardItem(3, 3, 3, 2, R.drawable.cccb),
            "card81" to CardItem(3, 3, 3, 3, R.drawable.cccc)
        )
}

