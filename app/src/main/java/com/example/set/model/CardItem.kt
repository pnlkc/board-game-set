package com.example.set.model

import android.media.Image
import androidx.annotation.DrawableRes

data class CardItem (
    val shape: Int,
    val color: Int,
    val number: Int,
    val shade: Int,
    @DrawableRes val cardImage: Int
)