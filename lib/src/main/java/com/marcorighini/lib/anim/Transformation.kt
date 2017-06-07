package com.marcorighini.lib.anim

import android.animation.ObjectAnimator
import android.view.View
import com.marcorighini.lib.AnchorOffset

interface Transformation {
    fun transform(view: View, moveY: Int, anchorOffset: AnchorOffset)
    fun getAnimator(view: View, anchorOffset: AnchorOffset, toProgress: Float, duration: Long): ObjectAnimator
}
