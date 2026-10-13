package com.maxrave.simpmusic.ui.navigation.destination.home

import kotlinx.serialization.Serializable

/**
 * MV 二级页(主页"最新 MV/MV 总榜"行的更多入口=网格;艺人页"视频"行的更多入口=
 * 封面沉浸头+播放列表形式,对齐 YTM 该入口打开的歌单页形态):source 取
 * NeteaseMvGridSource 常量(first/toplist/artist),ARTIST 必带 artistId/artistName;
 * title=行标题原样带入。
 */
@Serializable
data class NeteaseMvGridDestination(
    val source: String,
    val artistId: Long = 0L,
    val title: String,
    val artistName: String = "",
)

/** 私人雷达二级页(主页"私人雷达"行的更多入口);title=行标题原样带入 */
@Serializable
data class NeteaseRadarDestination(
    val title: String,
)

/** 新碟上架二级页(主页"新碟上架"行的更多入口,2026-10-13):顶栏带行标题标签+
 *  地区 chips+offset 分页网格;title=行标题原样带入;area=主页行当前选中地区
 *  (2026-10-13 四轮:跳转保持选中标签,二级页初始即该地区)。 */
@Serializable
data class NeteaseNewAlbumsDestination(
    val title: String,
    val area: String = "ALL",
)

/**
 * 播客电台列表二级页(发现页五行更多入口):source 取 NeteaseRadioListSource 常量
 * (my_subs/guess/featured/hot/new);title=行标题原样带入。
 */
@Serializable
data class NeteaseRadioListDestination(
    val source: String,
    val title: String,
)
