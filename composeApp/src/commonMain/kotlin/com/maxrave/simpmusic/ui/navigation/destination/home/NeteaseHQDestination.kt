package com.maxrave.simpmusic.ui.navigation.destination.home

import kotlinx.serialization.Serializable

/** 网易精品歌单二级页(主页"精品歌单"行的更多入口,游标分页网格);title=行标题原样带入 */
@Serializable
data class NeteaseHQDestination(
    val title: String,
)

/** 网易排行榜二级页(主页"排行榜"行的更多入口,完整榜单网格) */
@Serializable
data class NeteaseToplistDestination(
    val title: String,
)
