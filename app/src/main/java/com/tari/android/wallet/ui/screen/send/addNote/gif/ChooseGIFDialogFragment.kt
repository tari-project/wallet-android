package com.tari.android.wallet.ui.screen.send.addNote.gif

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnNextLayout
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.giphy.sdk.core.models.Media
import com.giphy.sdk.core.models.enums.MediaType
import com.giphy.sdk.ui.pagination.GPHContent
import com.giphy.sdk.ui.views.GPHGridCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.FragmentChooseGifBinding
import com.tari.android.wallet.di.DiContainer
import com.tari.android.wallet.ui.common.giphy.GiphyKeywordsRepository
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ChooseGIFDialogFragment : DialogFragment() {
    private lateinit var ui: FragmentChooseGifBinding
    private lateinit var behavior: BottomSheetBehavior<View>
    private lateinit var searchSubscription: Disposable

    @Inject
    lateinit var giphyKeywordsRepository: GiphyKeywordsRepository

    override fun onAttach(context: Context) {
        super.onAttach(context)
        DiContainer.appComponent.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentChooseGifBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val searchSubject = BehaviorSubject.create<String>()
        searchSubscription = searchSubject
            .debounce(500L, TimeUnit.MILLISECONDS)
            .map { it.ifEmpty { giphyKeywordsRepository.getCurrent() } }
            .map { GPHContent.searchQuery(it, mediaType = MediaType.gif) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { ui.giphyGridView.content = it }
        setupUI(searchSubject)
    }

    override fun onDestroyView() {
        searchSubscription.dispose()
        super.onDestroyView()
    }

    private fun setupUI(observer: Observer<String>) {
        ui.giphyGridView.content = GPHContent.searchQuery(giphyKeywordsRepository.getCurrent(), MediaType.gif)
        ui.giphyGridView.callback = object : GPHGridCallback {
            override fun contentDidUpdate(resultCount: Int) = Unit

            override fun didSelectMedia(media: Media) {
                val intent = Intent().apply { putExtra(MEDIA_DELIVERY_KEY, media) }
                targetFragment!!.onActivityResult(targetRequestCode, Activity.RESULT_OK, intent)
                behavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }
        ui.gifSearchEditText.addTextChangedListener(afterTextChanged = afterChanged@{ observer.onNext(it?.toString() ?: return@afterChanged) })
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        BottomSheetDialog(requireContext(), R.style.ChooseGIFDialog)
            .apply { setOnShowListener { setupDialog(this) } }

    private fun setupDialog(bottomSheetDialog: BottomSheetDialog) {
        val bottomSheet: View = bottomSheetDialog.findViewById(R.id.design_bottom_sheet)!!
        behavior = BottomSheetBehavior<View>()
        behavior.isHideable = true
        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_HIDDEN
        val layoutParams = bottomSheet.layoutParams as CoordinatorLayout.LayoutParams
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        layoutParams.behavior = behavior
        bottomSheetDialog.setOnKeyListener { _, keyCode, _ ->
            (keyCode == KeyEvent.KEYCODE_BACK && behavior.state != BottomSheetBehavior.STATE_HIDDEN &&
                    behavior.state != BottomSheetBehavior.STATE_COLLAPSED).also {
                if (it) behavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }
        ui.root.doOnNextLayout {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.addCallback(
                onStateChange = { _, state ->
                    if (state == BottomSheetBehavior.STATE_HIDDEN || state == BottomSheetBehavior.STATE_COLLAPSED) dismiss()
                },
                onSlided = { _, slideOffset ->
                    val alpha = (slideOffset.coerceIn(0F, 1F) * 255).toInt()
                    val color = Color.argb(alpha, 0, 0, 0)
                    (bottomSheet.parent as View).setBackgroundColor(color)
                }
            )
        }
    }

    private fun BottomSheetBehavior<*>.addCallback(
        onStateChange: (View, Int) -> Unit = { _, _ -> },
        onSlided: (View, Float) -> Unit = { _, _ -> },
    ) = addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) = onStateChange(bottomSheet, newState)

        override fun onSlide(bottomSheet: View, slideOffset: Float) = onSlided(bottomSheet, slideOffset)
    })

    companion object {
        @Suppress("DEPRECATION")
        fun newInstance() = ChooseGIFDialogFragment()
        const val MEDIA_DELIVERY_KEY = "key_media"
    }
}