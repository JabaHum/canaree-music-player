package dev.olog.service.music.shared

import dev.olog.core.MediaId
import dev.olog.service.music.model.MediaEntity

internal object MusicServiceData {

    val mediaEntity = MediaEntity(
        1L,
        1,
        MediaId.songId(1L),
        1L,
        1L,

        "title",
        "artist",
        "album",
        "album",
        1,
        1,
        "",
        "",
        0,
        0,
        false
    )

    fun mediaEntityList(itemsCount: Int) = (0 until itemsCount).map {
        MediaEntity(
            it.toLong(),
            it,
            MediaId.songId(it.toLong()),
            it.toLong(),
            it.toLong(),
            "title",
            "artist",
            "album",
            "album",
            1,
            1,
            "",
            "",
            0,
            0,
            false
        )
    }

}