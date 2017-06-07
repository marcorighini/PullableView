package com.marcorighini.lib.anim

import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.LinearInterpolator
import com.marcorighini.lib.ScrollOffsetLimit
import timber.log.Timber

class AlphaTransformation(val startAlpha: Float = 1.0f, val endAlpha: Float = 0.0f) : Transformation {
    override fun getAnimator(view: View, scrollOffsetLimit: ScrollOffsetLimit, toProgress: Float, duration: Long): ObjectAnimator =
            ObjectAnimator.ofFloat(view, View.ALPHA, view.alpha, startAlpha - toProgress * (startAlpha - endAlpha)).apply {
                this.duration = duration
                interpolator = LinearInterpolator()
            }

    override fun transform(view: View, moveY: Int, scrollOffsetLimit: ScrollOffsetLimit) {
        Timber.d("AlphaTransformation.transform: moveY=%d scrollOffsetLimit=%s)", moveY, scrollOffsetLimit)
        val progress = Math.abs(moveY).toFloat() / Math.abs(if (moveY < 0) scrollOffsetLimit.min else scrollOffsetLimit.max)
        view.alpha = startAlpha - progress * (startAlpha - endAlpha)
        Timber.d("AlphaTransformation.transform: moveY=%d scrollOffsetLimit=%s alpha=%f)", moveY, scrollOffsetLimit, view.alpha)
    }
}
