package dev.olog.msc.presentation.main.di

import dagger.Subcomponent
import dagger.android.AndroidInjector
import dev.olog.msc.dagger.scope.PerActivity
import dev.olog.msc.presentation.detail.di.DetailFragmentInjector
import dev.olog.msc.presentation.dialog.add.favorite.di.AddFavoriteDialogInjector
import dev.olog.msc.presentation.dialog.add.queue.di.AddQueueDialogInjector
import dev.olog.msc.presentation.dialog.clear.playlist.di.ClearPlaylistDialogInjector
import dev.olog.msc.presentation.dialog.create.playlist.di.NewPlaylistDialogInjector
import dev.olog.msc.presentation.dialog.delete.di.DeleteDialogInjector
import dev.olog.msc.presentation.dialog.rename.di.RenameDialogInjector
import dev.olog.msc.presentation.dialog.set.ringtone.di.SetRingtoneDialogInjector
import dev.olog.msc.presentation.edit.info.di.EditSongFragmentInjector
import dev.olog.msc.presentation.library.categories.di.CategoriesFragmentInjector
import dev.olog.msc.presentation.library.tab.di.TabFragmentInjector
import dev.olog.msc.presentation.main.MainActivity
import dev.olog.msc.presentation.navigator.NavigatorModule
import dev.olog.msc.presentation.player.di.PlayerFragmentInjector
import dev.olog.msc.presentation.playing.queue.di.PlayingQueueFragmentInjector
import dev.olog.msc.presentation.recently.added.di.RecentlyAddedFragmentInjector
import dev.olog.msc.presentation.related.artists.di.RelatedArtistFragmentInjector
import dev.olog.msc.presentation.search.di.SearchFragmentInjector

@Subcomponent(modules = arrayOf(
        MainActivityModule::class,
        MainActivityFragmentsModule::class,
        NavigatorModule::class,
////        ProModule::class,
//
//        // fragments
        CategoriesFragmentInjector::class,
        TabFragmentInjector::class,
        DetailFragmentInjector::class,
        PlayerFragmentInjector::class,
        RecentlyAddedFragmentInjector::class,
        RelatedArtistFragmentInjector::class,
        SearchFragmentInjector::class,
        PlayingQueueFragmentInjector::class,
        EditSongFragmentInjector::class,

        // dialogs
        AddFavoriteDialogInjector::class,
        AddQueueDialogInjector::class,
        SetRingtoneDialogInjector::class,
        RenameDialogInjector::class,
        ClearPlaylistDialogInjector::class,
        DeleteDialogInjector::class,
        NewPlaylistDialogInjector::class
))
@PerActivity
interface MainActivitySubComponent : AndroidInjector<MainActivity> {

    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<MainActivity>() {

        abstract fun module(module: MainActivityModule): Builder

        override fun seedInstance(instance: MainActivity) {
            module(MainActivityModule(instance))
        }
    }

}