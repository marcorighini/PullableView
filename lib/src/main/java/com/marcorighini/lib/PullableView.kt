package com.cynny.cynny.misc.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.app.Activity
import android.content.Context
import android.support.v4.view.MotionEventCompat
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import com.marcorighini.lib.*
import timber.log.Timber


class PullableView : FrameLayout {
    private val slope: Int = ViewConfiguration.get(context).scaledTouchSlop
    lateinit var anchorOffset: AnchorOffset
    lateinit var scrollThreshold: ScrollThreshold
    var direction: Direction
    var boundViews = listOf<BoundView>()
    var listener: PullListener? = null
    private var downX: Int = 0
    private var downY: Int = 0
    private var animationRunning = false
    private var snapped: Boolean = false

    interface PullListener {
        fun onReset()
        fun onPullStart()
        fun onSnap()
    }

    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        val displayMetrics = DisplayMetrics()
        (context as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
        val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.PullableView, 0, 0)
        direction = Direction.from(typedArray.getInt(R.styleable.PullableView_direction, Direction.BOTH.value))

        addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
            anchorOffset = AnchorOffset(typedArray.getInteger(R.styleable.PullableView_anchorOffsetUp, -v.top),
                    typedArray.getInteger(R.styleable.PullableView_anchorOffsetDown, displayMetrics.heightPixels - v.bottom))
            scrollThreshold = ScrollThreshold(typedArray.getInteger(R.styleable.PullableView_scrollThresholdUp, anchorOffset.up / 2),
                    typedArray.getInteger(R.styleable.PullableView_scrollThresholdDown, anchorOffset.down / 2))
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        Timber.d("onInterceptTouchEvent")
        var consumed = false

        if (isPullable()) {
            when (MotionEventCompat.getActionMasked(event)) {
                MotionEvent.ACTION_DOWN -> onDown(event)
                MotionEvent.ACTION_MOVE -> {
                    consumed = checkMoveInterception(event)
                    if (consumed) {
                        Timber.d("consuming move")
                        listener?.onPullStart()
                    }
                }
            }
        }

        return consumed
    }

    fun isPullable(): Boolean {
        val isPullable = !animationRunning && !snapped
        Timber.d("isPullable=%b", isPullable)
        return isPullable
    }

    private fun onDown(event: MotionEvent) {
        downY = event.rawY.toInt()
        downX = event.rawX.toInt()
        Timber.d("onDown: downY=%d", downY)
    }

    private fun checkMoveInterception(event: MotionEvent): Boolean {
        val y = event.rawY.toInt()
        val x = event.rawX.toInt()
        Timber.d("checkMoveInterception: y=%d downY=%d", y, downY)
        val moveX = Math.abs(x - downX)
        val moveY = y - downY
        return event.pointerCount == 1 && isOverSlope(moveY, slope, direction) && isVerticalMovement(moveY, moveX, 1.73f)
    }

    private fun isVerticalMovement(yMove: Int, xMove: Int, ratio: Float): Boolean = Math.abs(yMove) / (if (xMove == 0) 1 else xMove) > ratio

    private fun isOverSlope(yMove: Int, ySlope: Int, direction: Direction) = (direction.downEnabled() && yMove > ySlope) ||
            (direction.upEnabled() && yMove < -ySlope)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        Timber.d("onTouchEvent")
        if (isPullable()) {
            val currentMoveY = event.rawY.toInt() - downY

            when (MotionEventCompat.getActionMasked(event)) {
                MotionEvent.ACTION_DOWN -> onDown(event)
                MotionEvent.ACTION_MOVE ->
                    if (checkMove(currentMoveY, direction, anchorOffset)) {
                        Timber.d("Apply transformations")
                        boundViews.forEach { v -> v.transform(currentMoveY, anchorOffset) }
                    }
                MotionEvent.ACTION_UP -> {
                    Timber.d("ActionUp: y=%f downY=%d scrollThreshold=%s", y, downY, scrollThreshold)
                    if (isOverThreshold(currentMoveY, direction, scrollThreshold)) {
                        snap()
                    } else {
                        resetAnimated()
                    }
                }
            }

            return true
        }

        return false
    }

    private fun checkMove(moveY: Int, direction: Direction, anchorOffset: AnchorOffset): Boolean {
        Timber.d("checkMove: moveY=%d direction=%s anchorOffset=%s", moveY, direction, anchorOffset)
        return (direction.upEnabled() && moveY < 0 && moveY >= anchorOffset.up)
                || (direction.downEnabled() && moveY > 0 && moveY <= anchorOffset.down)
    }

    private fun isOverThreshold(moveY: Int, direction: Direction, scrollThreshold: ScrollThreshold) =
            (direction.upEnabled() && moveY < scrollThreshold.up) ||
                    (direction.downEnabled() && moveY > scrollThreshold.down)

    fun resetAnimated() {
        Timber.d("resetAnimated: animationRunning=%b", animationRunning)
        if (!animationRunning) {
            animationRunning = true
            boundViews.forEach { it.view.isClickable = false }
            AnimatorSet().apply {
                playTogether(boundViews.flatMap { it.getAnimators(anchorOffset, 0.0f) })
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        Timber.d("resetStart animation started")
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        Timber.d("resetStart animation end")
                        boundViews.forEach { it.view.isClickable = true }
                        animationRunning = false
                        snapped = false
                        listener?.onReset()
                    }
                })
            }.start()
        }
    }

    fun resetImmediate() {
        Timber.d("resetImmediate: animationRunning=%b", animationRunning)
        if (!animationRunning) {
            boundViews.forEach { v -> v.transform(0, anchorOffset) }
            snapped = false
            listener?.onReset()
        }
    }

    fun snap() {
        Timber.d("snap: animationRunning=%b", animationRunning)
        if (!animationRunning) {
            animationRunning = true
            boundViews.forEach { it.view.isClickable = false }
            AnimatorSet().apply {
                playTogether(boundViews.flatMap { it.getAnimators(anchorOffset, 1.0f) })
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        Timber.d("snap animation start")
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        Timber.d("snap animation end")
                        animationRunning = false
                        snapped = true
                        listener?.onSnap()
                    }
                })
            }.start()
        }
    }
}

