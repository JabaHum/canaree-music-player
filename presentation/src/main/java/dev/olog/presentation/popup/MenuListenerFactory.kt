package dev.olog.presentation.popup

import dev.olog.core.entity.track.*
import dev.olog.presentation.popup.album.AlbumPopupListener
import dev.olog.presentation.popup.artist.ArtistPopupListener
import dev.olog.presentation.popup.folder.FolderPopupListener
import dev.olog.presentation.popup.genre.GenrePopupListener
import dev.olog.presentation.popup.playlist.PlaylistPopupListener
import dev.olog.presentation.popup.song.SongPopupListener
import javax.inject.Inject
import javax.inject.Provider

class MenuListenerFactory @Inject constructor(
    private val folderPopupListener: Provider<FolderPopupListener>,
    private val playlistPopupListener: Provider<PlaylistPopupListener>,
    private val songPopupListener: Provider<SongPopupListener>,
    private val albumPopupListener: Provider<AlbumPopupListener>,
    private val artistPopupListener: Provider<ArtistPopupListener>,
    private val genrePopupListener: Provider<GenrePopupListener>
) {

    fun folder(folder: Folder, song: Song?) = folderPopupListener.get().setData(folder, song)
    fun playlist(playlist: Playlist, song: Song?) =
        playlistPopupListener.get().setData(playlist, song)

    fun song(song: Song) = songPopupListener.get().setData(song)
    fun album(album: Album, song: Song?) = albumPopupListener.get().setData(album, song)
    fun artist(artist: Artist, song: Song?) = artistPopupListener.get().setData(artist, song)
    fun genre(genre: Genre, song: Song?) = genrePopupListener.get().setData(genre, song)

}