package com.marcorighini.lib

import android.animation.ObjectAnimator
import android.view.View
import com.marcorighini.lib.anim.Transformation

class BoundView(val view: View, val transformations: List<Transformation> = listOf<Transformation>()) {
    fun transform(currentMoveY: Int, scrollOffsetLimit: ScrollOffsetLimit) {
        transformations.forEach { it.transform(view, currentMoveY, scrollOffsetLimit) }
    }

    fun getAnimators(scrollOffsetLimit: ScrollOffsetLimit, progress: Float, duration: Long = 300) =
        transformations.map { it.getAnimator(view, scrollOffsetLimit, progress, duration) }
}
