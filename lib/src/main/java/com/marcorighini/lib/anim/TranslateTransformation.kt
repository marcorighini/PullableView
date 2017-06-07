package com.marcorighini.lib.anim

import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.LinearInterpolator
import com.marcorighini.lib.AnchorOffset
import timber.log.Timber

class TranslateTransformation : Transformation {
    var startTranslationY: Float? = null

    override fun getAnimator(view: View, anchorOffset: AnchorOffset, toProgress: Float, duration: Long): ObjectAnimator {
        if (startTranslationY == null) startTranslationY = view.translationY
        val isUp = view.translationY < startTranslationY!!
        val targetTranslation = startTranslationY!! -
                toProgress * (startTranslationY!! - if (isUp) anchorOffset.up else anchorOffset.down)
        return ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, view.translationY, targetTranslation).apply {
            this.duration = duration
            interpolator = LinearInterpolator()
        }
    }

    override fun transform(view: View, moveY: Int, anchorOffset: AnchorOffset) {
        if (startTranslationY == null) startTranslationY = view.translationY
        view.translationY = startTranslationY!! + moveY
        Timber.d("TranslateTransformation.transform: moveY=%d anchorOffset=%s translationY=%f)", moveY, anchorOffset, view.translationY)
    }
}
