package com.pnlkc.set.recyclerview_friend_request_list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.pnlkc.set.databinding.ItemFriendRequestListBinding
import com.pnlkc.set.util.CustomDiffUtil

class FriendRequestListAdaptor(private val iRecyclerView: IFriendRequestList) :
    ListAdapter<String, FriendRequestListViewHolder>(CustomDiffUtil.stringDiffUtilItemCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendRequestListViewHolder {
        val binding =
            ItemFriendRequestListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FriendRequestListViewHolder(binding, iRecyclerView)
    }

    override fun onBindViewHolder(holder: FriendRequestListViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}