package dev.olog.msc.presentation.edit.album

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import dev.olog.msc.domain.entity.Song
import dev.olog.msc.presentation.model.DisplayableItem
import dev.olog.msc.utils.k.extension.get
import dev.olog.msc.utils.k.extension.unsubscribe
import io.reactivex.disposables.Disposable
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.TagOptionSingleton
import java.io.File

class EditAlbumFragmentViewModel(
        presenter: EditAlbumFragmentPresenter

) : ViewModel() {

    private val songListLiveData = MutableLiveData<List<Song>>()

    private val displayedAlbum = MutableLiveData<DisplayableAlbum>()

    private var songListDisposable: Disposable? = null

    init {
        TagOptionSingleton.getInstance().isAndroid = true

        songListDisposable = presenter.getSongList()
                .map { it[0].toDisplayableAlbum() to it }
                .subscribe({ (album, songList) ->
                    displayedAlbum.postValue(album)
                    songListLiveData.postValue(songList)
                }, {
                    it.printStackTrace()
                })
    }

    fun observeData(): LiveData<DisplayableAlbum> = displayedAlbum

    fun observeSongList(): LiveData<List<Song>> = songListLiveData

    private fun Song.toDisplayableAlbum(): DisplayableAlbum {
        val file = File(path)
        val audioFile = AudioFileIO.read(file)
        val tag = audioFile.tagOrCreateAndSetDefault

        return DisplayableAlbum(
                this.albumId,
                this.album,
                DisplayableItem.adjustArtist(this.artist),
                tag.get(FieldKey.GENRE),
                tag.get(FieldKey.YEAR),
                this.image
        )
    }

    override fun onCleared() {
        songListDisposable.unsubscribe()
    }

}