package com.maxrave.simpmusic.ui.utils

/** Source is encoded in browse ids throughout the shared artist/album/playlist screens. */
fun artistShareLink(id: String): String =
    if (id.toLongOrNull() != null) "https://music.163.com/artist?id=$id" else "https://music.youtube.com/channel/$id"

fun albumShareLink(id: String): String =
    when {
        id.toLongOrNull() != null -> "https://music.163.com/album?id=$id"
        id.startsWith("OLAK5uy_") -> "https://music.youtube.com/playlist?list=$id"
        else -> "https://music.youtube.com/browse/$id"
    }

fun playlistShareLink(id: String): String =
    if (id.toLongOrNull() != null) {
        "https://music.163.com/playlist?id=$id"
    } else {
        "https://music.youtube.com/playlist?list=${id.removePrefix("VL")}"
    }
