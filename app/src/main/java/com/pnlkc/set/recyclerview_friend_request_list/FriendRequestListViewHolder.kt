package com.pnlkc.set.recyclerview_friend_request_list

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.pnlkc.set.databinding.ItemFriendRequestListBinding

class FriendRequestListViewHolder(
    binding: ItemFriendRequestListBinding,
    private val iRecyclerView: IFriendRequestList
) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

    private val nicknameTextview = binding.friendRequestListItemNicknameTextview
    private val positiveBtn = binding.friendRequestListItemPositiveBtn
    private val negativeBtn = binding.friendRequestListItemNegativeBtn

    init {
        positiveBtn.setOnClickListener(this)
        negativeBtn.setOnClickListener(this)
    }

    fun bind(nickname: String) {
        nicknameTextview.text = nickname
    }

    override fun onClick(view: View?) {
        when (view) {
            positiveBtn -> iRecyclerView.positiveBtnClicked(bindingAdapterPosition)
            negativeBtn -> iRecyclerView.negativeBtnClicked(bindingAdapterPosition)
        }
    }
}