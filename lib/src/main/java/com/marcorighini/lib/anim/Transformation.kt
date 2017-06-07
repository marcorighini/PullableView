package com.marcorighini.lib.anim

import android.animation.ObjectAnimator
import android.view.View
import com.marcorighini.lib.Direction
import com.marcorighini.lib.ScrollOffsetLimit

interface Transformation {
    fun transform(view: View, moveY: Int, scrollOffsetLimit: ScrollOffsetLimit)
    fun getAnimator(view: View, scrollOffsetLimit: ScrollOffsetLimit, toProgress: Float, duration: Long): ObjectAnimator
}
