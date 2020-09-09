package dev.olog.feature.player.adapter

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import dev.olog.core.extensions.findActivity
import dev.olog.domain.prefs.MusicPreferencesGateway
import dev.olog.feature.player.IPlayerAppearanceAdaptiveBehavior
import dev.olog.feature.player.PlayerFragmentPresenter
import dev.olog.feature.player.PlayerFragmentViewModel
import dev.olog.feature.player.R
import dev.olog.feature.presentation.base.adapter.*
import dev.olog.lib.media.MediaProvider
import dev.olog.lib.media.model.PlayerMetadata
import dev.olog.lib.media.model.PlayerPlaybackState
import dev.olog.lib.media.model.PlayerState
import dev.olog.feature.presentation.base.adapter.drag.IDragListener
import dev.olog.feature.presentation.base.adapter.drag.TouchableAdapter
import dev.olog.feature.presentation.base.activity.HasSlidingPanel
import dev.olog.feature.presentation.base.loadSongImage
import dev.olog.feature.presentation.base.model.DisplayableItem
import dev.olog.feature.presentation.base.model.DisplayableTrack
import dev.olog.navigation.Navigator
import dev.olog.feature.player.volume.PlayerVolumeFragment
import dev.olog.feature.player.widgets.PlayerImageView
import dev.olog.feature.presentation.base.widget.swipeable.SwipeableView
import dev.olog.feature.presentation.base.model.toDomain
import dev.olog.feature.presentation.base.extensions.isCollapsed
import dev.olog.feature.presentation.base.extensions.isExpanded
import dev.olog.feature.presentation.base.widget.StatusBarView
import dev.olog.shared.TextUtils
import dev.olog.shared.android.theme.themeManager
import dev.olog.shared.swap
import kotlinx.android.synthetic.main.item_mini_queue.view.*
import kotlinx.android.synthetic.main.layout_view_switcher.view.*
import kotlinx.android.synthetic.main.player_controls_default.view.*
import kotlinx.android.synthetic.main.player_layout_default.view.*
import kotlinx.android.synthetic.main.player_toolbar_default.view.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.*

internal class PlayerFragmentAdapter(
    private val mediaProvider: MediaProvider,
    private val navigator: Navigator,
    private val viewModel: PlayerFragmentViewModel,
    private val presenter: PlayerFragmentPresenter,
    private val musicPrefs: MusicPreferencesGateway,
    private val dragListener: IDragListener,
    private val playerAppearanceAdaptiveBehavior: IPlayerAppearanceAdaptiveBehavior

) : ObservableAdapter<DisplayableItem>(DiffCallbackDisplayableItem),
    TouchableAdapter,
    CanShowIsPlaying by CanShowIsPlayingImpl() {

    private val playerViewTypes = listOf(
        R.layout.player_layout_default,
        R.layout.player_layout_spotify,
        R.layout.player_layout_flat,
        R.layout.player_layout_big_image,
        R.layout.player_layout_fullscreen,
        R.layout.player_layout_clean,
        R.layout.player_layout_mini
    )

    override fun initViewHolderListeners(viewHolder: DataBoundViewHolder, viewType: Int) {
        when (viewType) {
            R.layout.item_mini_queue -> {
                viewHolder.setOnClickListener(this) { item, _, _ ->
                    require(item is DisplayableTrack)
                    mediaProvider.skipToQueueItem(item.idInPlaylist)
                }
                viewHolder.setOnLongClickListener(this) { item, _, _ ->
                    navigator.toDialog(item.mediaId.toDomain(), viewHolder.itemView, viewHolder.itemView)
                }
                viewHolder.setOnClickListener(R.id.more, this) { item, _, view ->
                    navigator.toDialog(item.mediaId.toDomain(), view, viewHolder.itemView)
                }
                viewHolder.elevateAlbumOnTouch()

                viewHolder.setOnDragListener(R.id.dragHandle, dragListener)
            }
            R.layout.player_layout_default,
            R.layout.player_layout_spotify,
            R.layout.player_layout_fullscreen,
            R.layout.player_layout_flat,
            R.layout.player_layout_big_image,
            R.layout.player_layout_clean,
            R.layout.player_layout_mini -> {
                setupListeners(viewHolder)

                viewHolder.setOnClickListener(R.id.more, this) { _, _, view ->
                    try {
                        navigator.toDialog(playingMediaId!!.toDomain(), view, viewHolder.itemView)
                    } catch (ex: NullPointerException){
                        Timber.e(ex)
                    }
                }
                viewHolder.itemView.volume?.musicPrefs = musicPrefs
            }
        }

    }

    override fun onViewAttachedToWindow(holder: DataBoundViewHolder) {
        super.onViewAttachedToWindow(holder)

        val viewType = holder.itemViewType

        if (viewType in playerViewTypes) {

            val view = holder.itemView
            view.imageSwitcher?.let { switcher ->
                switcher.observeProcessorColors()
                    .onEach { presenter.updateProcessorColors(it) }
                    .launchIn(holder.lifecycleScope)

                switcher.observePaletteColors()
                    .onEach { presenter.updatePaletteColors(it) }
                    .launchIn(holder.lifecycleScope)
            }
            view.findViewById<PlayerImageView>(R.id.miniCover)?.let { imageView ->
                imageView.observeProcessorColors()
                    .onEach { presenter.updateProcessorColors(it) }
                    .launchIn(holder.lifecycleScope)

                imageView.observePaletteColors()
                    .onEach { presenter.updatePaletteColors(it) }
                    .launchIn(holder.lifecycleScope)
            }

            bindPlayerControls(holder, view)

            playerAppearanceAdaptiveBehavior(holder, presenter)
        }
    }

    private fun setupListeners(holder: DataBoundViewHolder) {
        val view = holder.itemView
        view.repeat.setOnClickListener { mediaProvider.toggleRepeatMode() }
        view.shuffle.setOnClickListener { mediaProvider.toggleShuffleMode() }
        view.favorite.setOnClickListener {
            view.favorite.toggleFavorite()
            mediaProvider.togglePlayerFavorite()
        }
        view.lyrics.setOnClickListener { navigator.toOfflineLyrics() }
        view.next.setOnClickListener { mediaProvider.skipToNext() }
        view.playPause.setOnClickListener { mediaProvider.playPause() }
        view.previous.setOnClickListener { mediaProvider.skipToPrevious() }

        view.replay.setOnClickListener {
            mediaProvider.replayTenSeconds()
        }

        view.replay30.setOnClickListener {
            mediaProvider.replayThirtySeconds()
        }

        view.forward.setOnClickListener {
            mediaProvider.forwardTenSeconds()
        }

        view.forward30.setOnClickListener {
            mediaProvider.forwardThirtySeconds()
        }

        view.playbackSpeed.setOnClickListener { openPlaybackSpeedPopup(it) }

        view.seekBar.setListener(
            onProgressChanged = {
                view.bookmark.text = TextUtils.formatMillis(it)
            }, onStartTouch = {

            }, onStopTouch = {
                mediaProvider.seekTo(it.toLong())
            }
        )
    }

    private fun bindPlayerControls(holder: DataBoundViewHolder, view: View) {
        val playerAppearance = view.context.themeManager.playerAppearance

        if (!playerAppearance.isSpotify && !playerAppearance.isBigImage){
            view.next.setDefaultColor()
            view.previous.setDefaultColor()
            view.playPause.setDefaultColor()
        }

        mediaProvider.observeMetadata()
            .onEach {
                updateMetadata(view, it)
                updateImage(view, it)
            }.launchIn(holder.lifecycleScope)

        view.volume?.setOnClickListener {
            // TODO refactor
            val outLocation = intArrayOf(0, 0)
            it.getLocationInWindow(outLocation)
            val yLocation = (outLocation[1] - StatusBarView.viewHeight).toFloat()
            (view.context as FragmentActivity).supportFragmentManager.commit {
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                add(android.R.id.content, PlayerVolumeFragment.newInstance(
                    R.layout.player_volume,
                    yLocation
                ), PlayerVolumeFragment.TAG)
                addToBackStack(PlayerVolumeFragment.TAG)
            }
        }

        mediaProvider.observePlaybackState()
            .onEach {
                onPlaybackStateChanged(view, it)
                view.seekBar.onStateChanged(it)
            }.launchIn(holder.lifecycleScope)

        mediaProvider.observeRepeat()
            .onEach { view.repeat.cycle(it) }
            .launchIn(holder.lifecycleScope)

        mediaProvider.observeShuffle()
            .onEach { view.shuffle.cycle(it) }
            .launchIn(holder.lifecycleScope)

        view.swipeableView?.setOnSwipeListener(object : SwipeableView.SwipeListener {
            override fun onSwipedLeft() {
                mediaProvider.skipToNext()
            }

            override fun onSwipedRight() {
                mediaProvider.skipToPrevious()
            }

            override fun onClick() {
                mediaProvider.playPause()
            }

            override fun onLeftEdgeClick() {
                mediaProvider.skipToPrevious()
            }

            override fun onRightEdgeClick() {
                mediaProvider.skipToNext()
            }
        })

        viewModel.onFavoriteStateChanged
            .onEach { view.favorite.onNextState(it) }
            .launchIn(holder.lifecycleScope)

        viewModel.skipToNextVisibility
            .onEach { view.next.updateVisibility(it) }
            .launchIn(holder.lifecycleScope)

        viewModel.skipToPreviousVisibility
            .onEach { view.previous.updateVisibility(it) }
            .launchIn(holder.lifecycleScope)

        presenter.observePlayerControlsVisibility()
            .filter { !playerAppearance.isFullscreen
                    && !playerAppearance.isMini
                    && !playerAppearance.isSpotify
                    && !playerAppearance.isBigImage
            }
            .onEach { visible ->
                view.findViewById<View>(R.id.playerControls)
                    ?.findViewById<View>(R.id.player)
                    ?.isVisible = visible
            }.launchIn(holder.lifecycleScope)

        mediaProvider.observePlaybackState()
            .filter { it.isSkipTo }
            .map { it.state == PlayerState.SKIP_TO_NEXT }
            .onEach { animateSkipTo(view, it) }
            .launchIn(holder.lifecycleScope)

        mediaProvider.observePlaybackState()
            .filter { it.isPlayOrPause }
            .map { it.state }
            .distinctUntilChanged()
            .onEach { state ->
                when (state) {
                    PlayerState.PLAYING -> playAnimation(view)
                    PlayerState.PAUSED -> pauseAnimation(view)
                    else -> throw IllegalArgumentException("invalid state $state")
                }
            }.launchIn(holder.lifecycleScope)
    }

    private fun updateMetadata(view: View, metadata: PlayerMetadata) {

        val duration = metadata.duration

        val readableDuration = metadata.readableDuration
        view.duration.text = readableDuration
        view.seekBar.max = duration.toInt()

        val playerControlsRoot = view.findViewById<ViewGroup>(R.id.playerControls)
        playerControlsRoot.podcast_controls.isVisible = metadata.isPodcast

        val title = if (view.context.themeManager.playerAppearance.isFlat){
            // WORKAROUND, all caps attribute is not working for some reason
            metadata.title.toUpperCase(Locale.getDefault())
        } else {
            metadata.title
        }
        view.textWrapper.update(title, metadata.artist)
        view.lyrics.isEnabled = metadata.id.toIntOrNull() != null
        view.favorite.isEnabled = view.lyrics.isEnabled
    }


    private fun updateImage(view: View, metadata: PlayerMetadata) {
        view.imageSwitcher?.loadImage(metadata)
        view.findViewById<PlayerImageView>(R.id.miniCover)?.loadImage(metadata.mediaId)
    }

    private fun openPlaybackSpeedPopup(view: View) {
        val popup = PopupMenu(view.context, view)
        popup.inflate(R.menu.dialog_playback_speed)
        popup.menu.getItem(viewModel.getPlaybackSpeed()).isChecked = true
        popup.setOnMenuItemClickListener {
            viewModel.setPlaybackSpeed(it.itemId)
            true
        }
        popup.show()
    }

    private fun onPlaybackStateChanged(view: View, playbackState: PlayerPlaybackState) {
        val isPlaying = playbackState.isPlaying

        if (isPlaying || playbackState.isPaused) {
            view.nowPlaying?.isActivated = isPlaying
            view.imageSwitcher?.setChildrenActivated(isPlaying)
        }
    }

    private fun animateSkipTo(view: View, toNext: Boolean) {
        val hasSlidingPanel = (view.findActivity()) as HasSlidingPanel
        if (hasSlidingPanel.getSlidingPanel().isCollapsed()) return

        if (toNext) {
            view.next.playAnimation()
        } else {
            view.previous.playAnimation()
        }
    }

    private fun playAnimation(view: View) {
        val hasSlidingPanel = (view.findActivity()) as HasSlidingPanel
        val isPanelExpanded = hasSlidingPanel.getSlidingPanel().isExpanded()
        view.playPause.animationPlay(isPanelExpanded)
    }

    private fun pauseAnimation(view: View) {
        val hasSlidingPanel = (view.findActivity()) as HasSlidingPanel
        val isPanelExpanded = hasSlidingPanel.getSlidingPanel().isExpanded()
        view.playPause.animationPause(isPanelExpanded)
    }

    override fun bind(holder: DataBoundViewHolder, item: DisplayableItem, position: Int) {
        if (item is DisplayableTrack){
            holder.itemView.apply {
                holder.imageView!!.loadSongImage(item.mediaId.toDomain())
                firstText.text = item.title
                secondText.text = item.artist
                explicit.onItemChanged(item.title)
            }
        }
    }

    override fun canInteractWithViewHolder(viewType: Int): Boolean {
        return viewType == R.layout.item_mini_queue
    }

    override fun onMoved(from: Int, to: Int) {
        val realFrom = from - 1
        val realTo = to - 1
        mediaProvider.swapRelative(realFrom, realTo)
        backedList.swap(from, to)
        notifyItemMoved(from, to)
    }

    override fun onSwipedRight(viewHolder: RecyclerView.ViewHolder) {
        val realPosition = viewHolder.adapterPosition - 1
        mediaProvider.removeRelative(realPosition)
    }

    override fun afterSwipeRight(viewHolder: RecyclerView.ViewHolder) {
        backedList.removeAt(viewHolder.adapterPosition)
        notifyItemRemoved(viewHolder.adapterPosition)
    }

    override fun afterSwipeLeft(viewHolder: RecyclerView.ViewHolder) {
        val realPosition = viewHolder.adapterPosition - 1
        mediaProvider.moveRelative(realPosition)
        notifyItemChanged(viewHolder.adapterPosition)
    }

}