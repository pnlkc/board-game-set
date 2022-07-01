package com.pnlkc.set.model

import androidx.annotation.DrawableRes

data class CardItem (
    val shape: Int,
    val color: Int,
    val number: Int,
    val shade: Int,
    @DrawableRes val cardImage: Int
)