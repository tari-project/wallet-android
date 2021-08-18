package com.tari.android.wallet.ui.fragment.settings.backup.verifySeedPhrase

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.flexbox.FlexboxLayout
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.component.CustomFont
import com.tari.android.wallet.ui.extension.dimenPx

@SuppressLint("ViewConstructor")
class SelectableWordTextView(private val isSelectedFlag: Boolean, context: Context) : AppCompatTextView(context, null) {

    private val avenirHeavy: Typeface
            by lazy(LazyThreadSafetyMode.NONE) { CustomFont.AVENIR_LT_STD_BLACK.asTypeface(context) }
    private val selectedWordEndMargin: Int
            by lazy(LazyThreadSafetyMode.NONE) { dimenPx(R.dimen.verify_seed_phrase_selected_word_end_margin) }
    private val wordBottomMargin: Int
            by lazy(LazyThreadSafetyMode.NONE) { dimenPx(R.dimen.verify_seed_phrase_word_bottom_margin) }
    private val selectableWordHorizontalPadding: Int
            by lazy(LazyThreadSafetyMode.NONE) { dimenPx(R.dimen.verify_seed_phrase_selectable_word_horizontal_padding) }
    private val selectableWordVerticalPadding: Int
            by lazy(LazyThreadSafetyMode.NONE) { dimenPx(R.dimen.verify_seed_phrase_selectable_word_vertical_padding) }
    private val selectableWordEndMargin: Int
            by lazy(LazyThreadSafetyMode.NONE) { dimenPx(R.dimen.verify_seed_phrase_selectable_word_end_margin) }

    init {
        typeface = avenirHeavy
        if (isSelectedFlag) {
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
            layoutParams = FlexboxLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { setMargins(0, 0, selectedWordEndMargin, wordBottomMargin) }

        } else {
            layoutParams = FlexboxLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { setMargins(0, 0, selectableWordEndMargin, wordBottomMargin) }

            setPadding(
                selectableWordHorizontalPadding,
                selectableWordVerticalPadding,
                selectableWordHorizontalPadding,
                selectableWordVerticalPadding
            )
        }
    }

    companion object {
        fun createSelectableWord(context: Context, isSelectedFlag: Boolean): SelectableWordTextView {
            val theme = if (isSelectedFlag) R.style.SelectedSeedPhraseWord else R.style.SelectableSeedPhraseWord
            val themedContext = ContextThemeWrapper(context, theme)
            return SelectableWordTextView(isSelectedFlag, themedContext)
        }
    }
}