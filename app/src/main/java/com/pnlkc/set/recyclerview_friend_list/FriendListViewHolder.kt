package com.pnlkc.set.recyclerview_friend_list

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.pnlkc.set.R
import com.pnlkc.set.databinding.ItemFriendListBinding
import com.pnlkc.set.model.Friend

class FriendListViewHolder(
    binding: ItemFriendListBinding,
    private val iRecyclerView: IFriendList,
) : RecyclerView.ViewHolder(binding.root), View.OnClickListener, View.OnLongClickListener {

    private val itemLinearLayout = binding.friendListItemLinearlayout
    private val nicknameTextView = binding.friendListItemNicknameTextview
    private val statusTextView = binding.friendListItemStatusTextview
    private val inviteBtn = binding.friendListItemInviteBtn

    init {
        itemLinearLayout.setOnLongClickListener(this)
        inviteBtn.setOnClickListener(this)
    }

    fun bind(friend: Friend, context: Context) {
        nicknameTextView.text = friend.nickname
        when (friend.status) {
            "online" -> {
                statusTextView.text = "온라인"

                val dialogBtnTextBg = ContextCompat.getColor(context, R.color.dialog_btn_text)
                val dialogTextBg = ContextCompat.getColor(context, R.color.dialog_text)
                nicknameTextView.setTextColor(dialogBtnTextBg)
                statusTextView.setTextColor(dialogTextBg)
                inviteBtn.visibility = View.VISIBLE
            }
            "offline" -> {
                statusTextView.text = "오프라인"

                val offlineColor = ContextCompat.getColor(context, R.color.friend_offline)
                nicknameTextView.setTextColor(offlineColor)
                statusTextView.setTextColor(offlineColor)
                inviteBtn.visibility = View.GONE
            }
            "play" -> {
                statusTextView.text = "게임중"

                val dialogBtnTextBg = ContextCompat.getColor(context, R.color.dialog_btn_text)
                val dialogTextBg = ContextCompat.getColor(context, R.color.dialog_text)
                nicknameTextView.setTextColor(dialogBtnTextBg)
                statusTextView.setTextColor(dialogTextBg)
                inviteBtn.visibility = View.GONE
            }
        }
    }

    override fun onClick(view: View?) {
        when (view) {
            inviteBtn -> iRecyclerView.inviteBtnClicked(bindingAdapterPosition)
        }
    }

    override fun onLongClick(view: View?): Boolean {
        when (view) {
            itemLinearLayout -> iRecyclerView.friendLongClicked(bindingAdapterPosition)
        }
        return true
    }
}