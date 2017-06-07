package com.marcorighini.lib

import android.view.View
import com.marcorighini.lib.anim.Transformation

class BoundView(val view: View, val transformations: List<Transformation> = listOf<Transformation>()) {
    fun transform(currentMoveY: Int, anchorOffset: AnchorOffset) {
        transformations.forEach { it.transform(view, currentMoveY, anchorOffset) }
    }

    fun getAnimators(anchorOffset: AnchorOffset, progress: Float, duration: Long = 300) =
        transformations.map { it.getAnimator(view, anchorOffset, progress, duration) }
}
