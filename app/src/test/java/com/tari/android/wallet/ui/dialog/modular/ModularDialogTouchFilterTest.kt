package com.tari.android.wallet.ui.dialog.modular

import android.view.MotionEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModularDialogTouchFilterTest {

    private val partiallyObscured = MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED

    @Test
    fun `partially obscured touch is dropped on api 29 and 30 when filtering is on`() {
        assertTrue(isBlockedObscuredTouch(filterObscuredTouches = true, sdkInt = 29, eventFlags = partiallyObscured))
        assertTrue(isBlockedObscuredTouch(filterObscuredTouches = true, sdkInt = 30, eventFlags = partiallyObscured))
    }

    @Test
    fun `touch is kept when filtering is off, the touch is not obscured, or the api has other protection`() {
        assertFalse(isBlockedObscuredTouch(filterObscuredTouches = false, sdkInt = 30, eventFlags = partiallyObscured))
        assertFalse(isBlockedObscuredTouch(filterObscuredTouches = true, sdkInt = 30, eventFlags = 0))
        assertFalse(isBlockedObscuredTouch(filterObscuredTouches = true, sdkInt = 28, eventFlags = partiallyObscured))
        assertFalse(isBlockedObscuredTouch(filterObscuredTouches = true, sdkInt = 31, eventFlags = partiallyObscured))
    }
}
