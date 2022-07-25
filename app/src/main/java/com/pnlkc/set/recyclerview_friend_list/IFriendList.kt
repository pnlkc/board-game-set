package com.pnlkc.set.recyclerview_friend_list

interface IFriendList {
    // 친구 아이템 롱클릭시
    fun friendLongClicked(position: Int)

    // 초대 버튼 클릭시
    fun inviteBtnClicked(position: Int)
}