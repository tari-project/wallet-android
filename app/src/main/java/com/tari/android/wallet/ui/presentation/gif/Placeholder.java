/*
  Copyright 2020 The Tari Project

  Redistribution and use in source and binary forms, with or
  without modification, are permitted provided that the
  following conditions are met:

  1. Redistributions of source code must retain the above copyright notice,
  this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above
  copyright notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution.

  3. Neither the name of the copyright holder nor the names of
  its contributors may be used to endorse or promote products
  derived from this software without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
  CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
  HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
  OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.ui.presentation.gif;

import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;

import com.facebook.drawee.drawable.RoundedColorDrawable;

public interface Placeholder {

    Drawable asDrawable();

    static Placeholder color(Object target) {
        return ColorPlaceholder.generate(target);
    }

    static Placeholder color(Object target, float cornerRadius) {
        return ColorPlaceholder.generate(target, cornerRadius);
    }

}

class ColorPlaceholder implements Placeholder {

    private static final float DEFAULT_CORNER_RADIUS = 20F;
    private static final int[] BUILT_IN_COLORS = {
            0xFF00B7E5,
            0xFFE5DA53,
            0xFF892EE5,
            0xFF00E589,
            0xFFE55C5C
    };

    @ColorInt
    private final int color;
    private final float cornerRadius;

    public static ColorPlaceholder generate(Object target) {
        return generate(target, DEFAULT_CORNER_RADIUS);
    }

    public static ColorPlaceholder generate(Object target, float cornerRadius) {
        return new ColorPlaceholder(
                BUILT_IN_COLORS[Math.abs(target.hashCode()) % BUILT_IN_COLORS.length],
                cornerRadius
        );
    }

    private ColorPlaceholder(@ColorInt int color, float cornerRadius) {
        this.color = color;
        this.cornerRadius = cornerRadius;
    }

    @Override
    public Drawable asDrawable() {
        return new RoundedColorDrawable(cornerRadius, color);
    }
}
