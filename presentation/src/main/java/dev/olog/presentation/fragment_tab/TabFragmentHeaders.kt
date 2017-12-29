package dev.olog.presentation.fragment_tab

import android.content.res.Resources
import dev.olog.presentation.R
import dev.olog.presentation.model.DisplayableItem
import dev.olog.shared.MediaId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TabFragmentHeaders @Inject constructor(
        resources: Resources
) {

    val allPlaylistHeader = DisplayableItem(R.layout.item_tab_header,
            MediaId.headerId("all playlist"), resources.getString(R.string.tab_all_playlists))

    val autoPlaylistHeader = DisplayableItem(R.layout.item_tab_header,
            MediaId.headerId("auto playlist"), resources.getString(R.string.tab_auto_playlists))

    val shuffleHeader = DisplayableItem(R.layout.item_tab_shuffle,
            MediaId.headerId("tab shuffle"),"")

    val albumHeaders = listOf(
            DisplayableItem(R.layout.item_tab_header, MediaId.headerId("recent albums"), resources.getString(R.string.tab_recent)),
            DisplayableItem(R.layout.item_tab_last_played_album_horizontal_list, MediaId.headerId("horiz list album"), ""),
            DisplayableItem(R.layout.item_tab_header, MediaId.headerId("all albums"), resources.getString(R.string.tab_all_albums))
    )

    val artistHeaders = listOf(
            DisplayableItem(R.layout.item_tab_header, MediaId.headerId("recent artists"), resources.getString(R.string.tab_recent)),
            DisplayableItem(R.layout.item_tab_last_played_artist_horizontal_list, MediaId.headerId("horiz list artist"), ""),
            DisplayableItem(R.layout.item_tab_header, MediaId.headerId("all artists"), resources.getString(R.string.tab_all_artists))
    )

}