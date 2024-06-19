package com.tari.android.wallet.ui.fragment.contact_book.root.action_menu

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.view.children
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.bumptech.glide.Glide
import com.tari.android.wallet.R
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.databinding.ViewActionMenuBinding
import com.tari.android.wallet.databinding.ViewActionMenuItemBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.ui.component.tari.TariTextView
import com.tari.android.wallet.ui.extension.dpToPx
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.contact.badges.BadgesController
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.home.HomeActivity
import com.tari.android.wallet.util.extractEmojis
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

class ActionMenuView : CommonView<CommonViewModel, ViewActionMenuBinding> {
    constructor(context: Context) : super(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): ViewActionMenuBinding =
        ViewActionMenuBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    val speed = 1.0
    val baseTime = 100L

    var side = false
    lateinit var sharedPrefsRepository: CorePrefRepository

    val avatar = ui.rootAvatarContainer
    val firstCircle = ui.firstCircle
    val secondCircle = ui.secondCircle
    val closeButton = ui.closeButton
    val changeSideButton = ui.changeSideButton

    var currentAnimation: AnimatorSet? = null
    var wheelAnimation: ValueAnimator? = null

    val itemsView = mutableListOf<View>()

    fun init(sharedPrefsRepository: CorePrefRepository) {
        this.sharedPrefsRepository = sharedPrefsRepository
        side = !sharedPrefsRepository.actionMenuSide
        changeSide()
        this.isClickable = true
        this.isFocusable = true
        this.layoutParams = FrameLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)
        changeSideButton.setOnClickListener { changeSide() }
        this.closeButton.setOnClickListener { close() }
        close()
        gone()
    }

    fun onBackPressed(): Boolean {
        if (currentAnimation != null) {
            close()
            return true
        }
        return false
    }

    private var lastPosition: Point? = null
    private var lastStartClick: View? = null

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        handleTouch(event ?: return super.onTouchEvent(event))
        return true
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        handleTouch(event ?: return super.onTouchEvent(event))
        return super.onTouchEvent(event)
    }

    private fun handleTouch(event: MotionEvent) {
        println(event)
        try {
            val offset = lastPosition.let {
                val dx = event.rawX - (it?.x?.toDouble() ?: 0.0)
                val dy = event.rawY - (it?.y?.toDouble() ?: 0.0)
                println("dx $dx dy $dy")
                -(dx * dx + dy * dy).pow(0.5) * sign(dy)
            }
            wheelAnimation?.cancel()
            println(offset)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastPosition = Point(event.rawX.toInt(), event.rawY.toInt())

                    lastStartClick = (itemsView + closeButton + changeSideButton).firstOrNull {
                        val rect = Rect()
                        it.getHitRect(rect)
                        rect.contains(event.x.toInt(), event.y.toInt())
                    }
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    if (lastStartClick != null) {
                        lastStartClick?.performClick()
                    }
                    lastPosition = null
                    lastStartClick = null
                    wheelAnimation = ObjectAnimator.ofFloat(offset.toFloat(), 0.0F).apply {
                        this.addUpdateListener {
                            calculatePositionsForItems(it.animatedValue as Float)
                        }
                        interpolator = FastOutSlowInInterpolator()
                        duration = (baseTime * speed * 2).toLong()
                        start()
                    }
                }

                else -> {
                    if (lastStartClick != null) {
                        val rect = Rect()
                        lastStartClick?.getHitRect(rect)
                        if (!rect.contains(event.x.toInt(), event.y.toInt())) {
                            lastStartClick = null
                        }
                    }
                    calculatePositionsForItems(offset.toFloat())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showContact(contact: ContactDto) {
        val pictureUrl = contact.getPhoneContactInfo()?.avatar
        ui.avatar.setVisible(pictureUrl.isNullOrEmpty())
        ui.avatarImageCardView.setVisible(!pictureUrl.isNullOrEmpty())
        if (pictureUrl.isNullOrEmpty()) {
            val avatar = contact.getFFIContactInfo()?.extractWalletAddress()?.emojiId?.extractEmojis()?.take(1)?.joinToString()
                ?: contact.contactInfo.getAlias().firstOrNull()?.toString() ?: ""
            ui.avatar.text = avatar
        } else {
            Glide.with(this).load(pictureUrl).into(ui.avatarImage)
        }

        initItems(contact)

        doAnimation()
    }

    private fun initItems(contact: ContactDto) {
        ui.menuItems.removeAllViews()
        itemsView.clear()

        val actions = contact.getContactActions()

        BadgesController.availableContactActions.forEach { action ->
            if (!actions.contains(action)) return@forEach

            val view = ViewActionMenuItemBinding.inflate(LayoutInflater.from(context), ui.menuItems, false)
            view.root.layoutParams =
                FrameLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL
                )
            view.root.setOnClickListener {
                close {
                    HomeActivity.instance.get()?.actionMenuViewModel?.doAction(action, contact)
                }
            }
            view.itemText.setText(action.title)
            view.itemImage.setImageResource(action.icon)
            ui.menuItems.addView(view.root)
            itemsView.add(view.root)
        }
        applySideToItems()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        calculatePositionsForItems(0.0F)
    }

    private fun calculatePositionsForItems(offsetInPx: Float = 0.0f) {
        val baseAngleTurn = 25
        val middleIndex = (itemsView.size - 1) / 2.0
        val radius = context.dpToPx(165f)
        val addasinAngle = (Math.floor(Math.abs(offsetInPx.toDouble()) / radius).toInt()) * 90 * sign(offsetInPx)
        val offsetInAngle = Math.toDegrees(asin((offsetInPx / radius) % 1).toDouble()) + addasinAngle
        println(offsetInAngle)
        for ((index, item) in itemsView.withIndex()) {
            val angle = -(index - middleIndex) * baseAngleTurn + offsetInAngle + 90
            val angleInRadian = Math.toRadians(angle)
            val xSin = sin(angleInRadian)
            val yCos = cos(angleInRadian)
            val x = xSin * radius
            val y = yCos * radius
            println("angle $index, $angle, $angleInRadian, $x, $y, ${cos(angleInRadian)}, ${sin(angleInRadian)}")
            println("item ${item.width}, ${item.height}")
            val rotationAngle = 90 - angle
            val oldRotation = item.rotation
            item.rotation = rotationAngle.toFloat()

            println("rotation $rotationAngle, $oldRotation")

            item.translationX = x.toFloat() + ((item.width / 2) * cos(Math.toRadians(rotationAngle)).toFloat()) - (item.width / 2)
            item.translationY = y.toFloat() + ((item.width / 2) * sin(Math.toRadians(rotationAngle)).toFloat())

            println("translation ${item.translationX}, ${item.translationY}")
        }
    }

    private fun changeSide() {
        close {
            side = !side
            sharedPrefsRepository.actionMenuSide = side
            scaleX = if (side) -1.0f else 1.0f
            avatar.scaleX = if (side) -1.0f else 1.0f
            applySideToItems()
            doAnimation()
        }
    }

    private fun applySideToItems() {
        itemsView.forEach {
            it.scaleX = if (side) -1.0f else 1.0f
            val cardView = it.findViewById<CardView>(R.id.card_view)
            val text = it.findViewById<TariTextView>(R.id.item_text)
            with(it as ViewGroup) {
                removeAllViews()
                if (side) {
                    addView(text)
                    addView(cardView)
                } else {
                    addView(cardView)
                    addView(text)
                }
            }
        }
    }

    private fun doAnimation() {
        visible()

        val animatedObjects = mutableListOf(this, avatar, firstCircle, secondCircle, closeButton, changeSideButton)
        animatedObjects.addAll(ui.menuItems.children)

        val startAlpha = 0.0f
        val endAlpha = 1f
        this.alpha = startAlpha

        animatedObjects.forEach {
            it.alpha = startAlpha
        }

        val backObjectAnimator = ObjectAnimator.ofFloat(this, "alpha", startAlpha, endAlpha).setDuration((baseTime * speed * 4).toLong())
        val avatarObjectAnimator = ObjectAnimator.ofFloat(avatar, "alpha", startAlpha, endAlpha).setDuration((baseTime * speed * 1).toLong())
        val firstLineObjectAnimator =
            ObjectAnimator.ofFloat(firstCircle, "alpha", startAlpha, endAlpha * 0.4f).setDuration((baseTime * speed * 0.5).toLong())
        val secondLineObjectAnimator =
            ObjectAnimator.ofFloat(secondCircle, "alpha", startAlpha, endAlpha * 0.4f).setDuration((baseTime * speed * 0.5).toLong())
        val itemsSet = AnimatorSet().apply {
            val itemsAnimators = itemsView.mapIndexed { index, view ->
                val itemObjectAnimator = ObjectAnimator.ofFloat(view, "alpha", startAlpha, endAlpha)
                    .setDuration((baseTime * speed * 1.5).toLong())
                itemObjectAnimator.startDelay = (baseTime * speed * 0.5).toLong() * (index + 1)
                itemObjectAnimator
            }

            playTogether(itemsAnimators)
        }
        val closeButtonObjectAnimator =
            ObjectAnimator.ofFloat(closeButton, "alpha", startAlpha, endAlpha).setDuration((baseTime * speed * 1).toLong())
        val changeSideButtonObjectAnimator =
            ObjectAnimator.ofFloat(changeSideButton, "alpha", startAlpha, endAlpha).setDuration((baseTime * speed * 1).toLong())


        currentAnimation = AnimatorSet().apply {
            val buttonsSet = AnimatorSet().apply {
                playTogether(closeButtonObjectAnimator, changeSideButtonObjectAnimator)
            }

            playSequentially(backObjectAnimator, avatarObjectAnimator, firstLineObjectAnimator, secondLineObjectAnimator, itemsSet, buttonsSet)
        }
        currentAnimation?.start()
    }

    private fun close(endAction: () -> Unit = {}) {
        try {
            if (currentAnimation != null) {
                currentAnimation?.doOnEnd {
                    gone()
                    endAction()
                }
                runCatching { currentAnimation?.reverse() }
                currentAnimation = null
            } else {
                endAction()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}