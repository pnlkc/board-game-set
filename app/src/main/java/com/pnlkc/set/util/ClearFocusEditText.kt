package com.pnlkc.set.util

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.appcompat.widget.AppCompatEditText
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart

class ClearFocusEditText : AppCompatEditText {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent?): Boolean {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            clearFocus()
        }
        return super.onKeyPreIme(keyCode, event)
    }

    // 에딧텍스트 textChange를 flow로 변환
    fun textChangesToFlow() : Flow<CharSequence?> {
        return callbackFlow<CharSequence> {
            val textChangeListener = object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    trySend(text!!).isSuccess
                }

                override fun afterTextChanged(p0: Editable?) = Unit
            }

            // 위에서 설정한 리스너 달아주기
            addTextChangedListener(textChangeListener)

            // 콜백이 사라질때 실행됨
            awaitClose {
                removeTextChangedListener(textChangeListener)
            }
        }.onStart {
            // emit으로 이벤트를 전달
            emit(text!!)
        }
    }
}