package com.tari.android.wallet.ui.component

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.DITHER_FLAG
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.tari.android.wallet.ui.extension.removeListenersAndCancel
import org.joda.time.DateTime
import kotlin.math.sin

class WaveView : View {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    private val color = Color.parseColor("#6239FF")
    private val alphaColor = Color.argb((0.4F * 255).toInt(), color.red, color.green, color.blue)
    private val paint = Paint(DITHER_FLAG).apply {
        color = alphaColor
        isAntiAlias = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
    }
    private val path = Path()
    private var currentSpeed = 0F
    private val baseTime = DateTime.now().millis

    private val amplitude = 75F
    private val waveSpeed = 0.5F
    private var waveLength = 1500F
    private val pixelAccuracy = 5

    private var animation: ValueAnimator? = null

    init {
        animation = ValueAnimator.ofInt(0, Int.MAX_VALUE).apply {
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                currentSpeed = (baseTime - DateTime.now().millis) * waveSpeed / 1000
                invalidate()
            }
            start()
        }
        alpha = 1F
        setBackgroundColor(Color.TRANSPARENT)
    }

    override fun invalidate() {
        super.invalidate()
        waveLength = width * 1.5F
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        doWave()
        canvas.drawPath(path, paint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        animation?.removeListenersAndCancel()
        animation = null
    }

    private fun doWave() {
        path.reset()
        path.moveTo(0f, 0f)
        var i = 0
        while (i < width + pixelAccuracy) {
            val wx = i.toFloat()
            val wy = amplitude * 2 + amplitude * sin((i + 10) * Math.PI / waveLength + currentSpeed).toFloat()
            path.lineTo(wx, wy)
            i += pixelAccuracy
        }
        path.lineTo(width.toFloat(), height.toFloat())
        path.lineTo(0f, height.toFloat())
        path.close()
    }
}