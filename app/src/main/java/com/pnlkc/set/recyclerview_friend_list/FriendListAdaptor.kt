package com.pnlkc.set.recyclerview_friend_list

import android.content.Context
import android.text.method.TextKeyListener.clear
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.pnlkc.set.databinding.ItemFriendListBinding
import com.pnlkc.set.model.Friend
import com.pnlkc.set.util.CustomDiffUtil

class FriendListAdaptor(private var iRecyclerView: IFriendList, private var context: Context) :
    RecyclerView.Adapter<FriendListViewHolder>() {

    private var friendList = mutableListOf<Friend>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendListViewHolder {
        val binding =
            ItemFriendListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FriendListViewHolder(binding, iRecyclerView)
    }

    override fun onBindViewHolder(holder: FriendListViewHolder, position: Int) {
        holder.bind(friendList[position], context)
    }

    override fun getItemCount(): Int = friendList.size

    fun setData(friend: MutableList<Friend>) {
        friend.let {
            val diffCallback = CustomDiffUtil.FriendDiffUtilCallback(friendList, friend)
            val diffResult = DiffUtil.calculateDiff(diffCallback)

            friendList.run {
                clear()
                addAll(friend)
                diffResult.dispatchUpdatesTo(this@FriendListAdaptor)
            }
        }
    }
}