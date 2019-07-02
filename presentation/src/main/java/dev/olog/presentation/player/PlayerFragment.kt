package dev.olog.presentation.player

import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.math.MathUtils.clamp
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dev.olog.core.MediaId
import dev.olog.core.gateway.PlayingQueueGateway
import dev.olog.media.MediaProvider
import dev.olog.media.extractBookmark
import dev.olog.media.isPlaying
import dev.olog.presentation.R
import dev.olog.presentation.tutorial.TutorialTapTarget
import dev.olog.shared.AppConstants.PROGRESS_BAR_INTERVAL
import dev.olog.presentation.base.BaseFragment
import dev.olog.presentation.model.DisplayableItem
import dev.olog.presentation.navigator.Navigator
import dev.olog.shared.extensions.*
import dev.olog.shared.theme.PlayerAppearance
import dev.olog.shared.theme.hasPlayerAppearance
import dev.olog.shared.utils.isMarshmallow
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_player.*
import kotlinx.android.synthetic.main.player_toolbar_default.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.abs

class PlayerFragment : BaseFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel by lazyFast {
        viewModelProvider<PlayerFragmentViewModel>(
            viewModelFactory
        )
    }
    @Inject
    lateinit var presenter: PlayerFragmentPresenter
    @Inject
    lateinit var navigator: Navigator

    private lateinit var layoutManager: LinearLayoutManager

    private lateinit var mediaProvider: MediaProvider

    private var lyricsDisposable: Disposable? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = PlayerFragmentAdapter(
            lifecycle, activity as MediaProvider,
            navigator, viewModel, presenter
        )

        layoutManager = LinearLayoutManager(context)
        list.adapter = adapter
        list.layoutManager = layoutManager
        list.setHasFixedSize(true)
        list.isNestedScrollingEnabled = false
//        val callback = TouchHelperAdapterCallback(adapter, ItemTouchHelper.RIGHT/* or ItemTouchHelper.LEFT*/) TODO
//        val touchHelper = ItemTouchHelper(callback)
//        touchHelper.attachToRecyclerView(list)
//        adapter.touchHelper = touchHelper

        val statusBarAlpha = if (!isMarshmallow()) 1f else 0f
        statusBar.alpha = statusBarAlpha

        val playerAppearance = requireContext().hasPlayerAppearance()
        if (playerAppearance.isBigImage()) {
            val set = ConstraintSet()
            set.clone(view as ConstraintLayout)
            set.connect(list.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            set.applyTo(view)
        }

        mediaProvider = (activity as MediaProvider)

        mediaProvider.observeQueue()
            .map { it.map { it.toDisplayableItem() } }
            .map { queue ->
                if (!playerAppearance.isMini()) {
                    val copy = queue.toMutableList()
                    if (copy.size > PlayingQueueGateway.MINI_QUEUE_SIZE - 1) {
                        copy.add(viewModel.footerLoadMore)
                    }
                    copy.add(0, viewModel.playerControls())
                    copy
                } else {
                    listOf(viewModel.playerControls())
                }
            }
            .assertBackground()
            .flowOn(Dispatchers.Default)
            .asLiveData()
            .subscribe(viewLifecycleOwner, adapter::updateDataSet)
    }

    override fun onResume() {
        super.onResume()
        getSlidingPanel()?.addPanelSlideListener(slidingPanelListener)
    }

    override fun onPause() {
        super.onPause()
        getSlidingPanel()?.removePanelSlideListener(slidingPanelListener)
    }

    override fun onStop() {
        super.onStop()
        lyricsDisposable.unsubscribe()
    }

    private fun MediaSessionCompat.QueueItem.toDisplayableItem(): DisplayableItem {
        val description = this.description

        return DisplayableItem(
            R.layout.item_mini_queue,
            MediaId.fromString(description.mediaId!!),
            description.title!!.toString(),
            DisplayableItem.adjustArtist(description.subtitle!!.toString()),
            isPlayable = true,
            trackNumber = "${this.queueId}"
        )
    }

    override fun provideLayoutId(): Int {
        val appearance = requireContext().hasPlayerAppearance()
        return when (appearance.playerAppearance()) {
            PlayerAppearance.FULLSCREEN -> R.layout.fragment_player_fullscreen
            PlayerAppearance.CLEAN -> R.layout.fragment_player_clean
            PlayerAppearance.MINI -> R.layout.fragment_player_mini
            else -> R.layout.fragment_player
        }
    }

    private val slidingPanelListener = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            if (!isMarshmallow() && slideOffset in .9f..1f) {
                val alpha = (1 - slideOffset) * 10
                statusBar?.alpha = clamp(abs(1 - alpha), 0f, 1f)
            }
            val alpha = clamp(slideOffset * 5f, 0f, 1f)
            view?.alpha = alpha
        }

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                lyricsDisposable.unsubscribe()
                lyricsDisposable = Completable.timer(50, TimeUnit.MILLISECONDS, Schedulers.io())
                    .andThen(viewModel.showLyricsTutorialIfNeverShown())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ lyrics?.let { TutorialTapTarget.lyrics(it) } }, {})
            } else {
                lyricsDisposable.unsubscribe()
            }
        }
    }
}