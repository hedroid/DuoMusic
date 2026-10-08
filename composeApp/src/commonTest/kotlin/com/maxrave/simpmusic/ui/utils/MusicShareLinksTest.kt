package com.maxrave.simpmusic.ui.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class MusicShareLinksTest {
    @Test
    fun shareLinksFollowTheContentSourceAndType() {
        assertEquals("https://music.163.com/artist?id=6452", artistShareLink("6452"))
        assertEquals("https://music.youtube.com/channel/UCabc", artistShareLink("UCabc"))
        assertEquals("https://music.163.com/album?id=3410984", albumShareLink("3410984"))
        assertEquals("https://music.youtube.com/browse/MPREabc", albumShareLink("MPREabc"))
        assertEquals("https://music.youtube.com/playlist?list=OLAK5uy_abc", albumShareLink("OLAK5uy_abc"))
        assertEquals("https://music.163.com/playlist?id=1234567", playlistShareLink("1234567"))
        assertEquals("https://music.youtube.com/playlist?list=PLabc", playlistShareLink("VLPLabc"))
    }
}
