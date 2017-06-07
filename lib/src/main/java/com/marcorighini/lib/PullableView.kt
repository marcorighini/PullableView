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
import android.graphics.Rect


class PullableView : FrameLayout {
    private val ySlope: Int = ViewConfiguration.get(context).scaledTouchSlop
    lateinit var startArea: StartArea
    lateinit var scrollOffsetLimit: ScrollOffsetLimit
    lateinit var scrollOffsetThreshold: ScrollOffsetThreshold
    var direction: Direction
    var boundViews = listOf<BoundView>()
    var listener: PullListener? = null
    private var downX: Int = 0
    private var downY: Int = 0

    interface PullListener {
        val isPullable: Boolean
        fun onResetToStart()
        fun onPullStart()
        fun onSnapToEnd()
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
            val r = Rect()
            v.getGlobalVisibleRect(r)
            val x = r.left
            val y = r.top

            startArea = StartArea(typedArray.getInteger(R.styleable.PullableView_startAreaMinX, x),
                    typedArray.getInteger(R.styleable.PullableView_startAreaMinY, y),
                    typedArray.getInteger(R.styleable.PullableView_startAreaMaxX, x + v.width),
                    typedArray.getInteger(R.styleable.PullableView_startAreaMaxY, y + v.height))
            scrollOffsetLimit = ScrollOffsetLimit(typedArray.getInteger(R.styleable.PullableView_scrollMin, -startArea.minY),
                    typedArray.getInteger(R.styleable.PullableView_scrollMax, displayMetrics.heightPixels - startArea.maxY))
            scrollOffsetThreshold = ScrollOffsetThreshold(typedArray.getInteger(R.styleable.PullableView_scrollThresholdMin, scrollOffsetLimit.min / 2),
                    typedArray.getInteger(R.styleable.PullableView_scrollThresholdMax, scrollOffsetLimit.max / 2))
        }
    }


    private var snappingToLimit = false
    private var animationRunning = false
    private var lockOnSnap: Boolean = false

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        Timber.d("onInterceptTouchEvent")
        var consumed = false

        if (isPullable) {
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

    private val isPullable: Boolean
        get() {
            val isPullable = !animationRunning && (!lockOnSnap || !snappingToLimit) && (listener != null && listener!!.isPullable || listener == null)
            Timber.d("isPullable %b", isPullable)
            return isPullable
        }

    private fun onDown(event: MotionEvent) {
        downY = event.rawY.toInt()
        downX = event.rawX.toInt()
        Timber.d("onDown: downY=%d startArea=%s", downY, startArea)
    }

    private fun checkMoveInterception(event: MotionEvent): Boolean {
        val y = event.rawY.toInt()
        val x = event.rawX.toInt()
        Timber.d("checkMoveInterception: y=%d downY=%d startArea=%s", y, downY, startArea)
        val moveX = Math.abs(x - downX)
        val moveY = y - downY
        return event.pointerCount == 1 && startArea.inBounds(x, y) && isOverSlope(moveY, ySlope, direction) && isVerticalMovement(moveY, moveX, 1.73f)
    }

    private fun isVerticalMovement(yMove: Int, xMove: Int, ratio: Float): Boolean = Math.abs(yMove) / (if (xMove == 0) 1 else xMove) > ratio

    private fun isOverSlope(yMove: Int, ySlope: Int, direction: Direction) = (direction.downEnabled() && yMove > ySlope) ||
            (direction.upEnabled() && yMove < -ySlope)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        Timber.d("onTouchEvent")
        if (isPullable) {
            val currentMoveY = event.rawY.toInt() - downY

            when (MotionEventCompat.getActionMasked(event)) {
                MotionEvent.ACTION_DOWN -> onDown(event)
                MotionEvent.ACTION_MOVE ->
                    if (checkMove(currentMoveY, direction, scrollOffsetLimit)) {
                        Timber.d("Apply transformations")
                        boundViews.forEach { v -> v.transform(currentMoveY, scrollOffsetLimit) }
                    }
                MotionEvent.ACTION_UP -> {
                    Timber.d("ActionUp: y=%f downY=%d scrollOffsetThreshold=%s", y, downY, scrollOffsetThreshold)
                    if (isOverThreshold(currentMoveY, direction, scrollOffsetThreshold)) {
                        snapToLimit()
                    } else {
                        resetAnimated()
                    }
                }
            }

            return true
        }

        return false
    }

    private fun checkMove(moveY: Int, direction: Direction, scrollOffsetLimit: ScrollOffsetLimit): Boolean {
        Timber.d("checkMove: moveY=%d direction=%s scrollOffsetLimit=%s", moveY, direction, scrollOffsetLimit)
        return (direction.upEnabled() && moveY < 0 && moveY >= scrollOffsetLimit.min)
                || (direction.downEnabled() && moveY > 0 && moveY <= scrollOffsetLimit.max)
    }

    private fun isOverThreshold(moveY: Int, direction: Direction, scrollOffsetThreshold: ScrollOffsetThreshold) =
            (direction.upEnabled() && moveY < scrollOffsetThreshold.min) ||
                    (direction.downEnabled() && moveY > scrollOffsetThreshold.max)

    fun resetAnimated() {
        Timber.d("resetAnimated: animationRunning=%b", animationRunning)
        if (!animationRunning) {
            animationRunning = true
            boundViews.forEach { it.view.isClickable = false }
            AnimatorSet().apply {
                playTogether(boundViews.flatMap { it.getAnimators(scrollOffsetLimit, 0.0f) })
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        Timber.d("resetStart animation started")
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        boundViews.forEach { it.view.isClickable = true }
                        animationRunning = false
                        listener?.onResetToStart()
                        Timber.d("resetStart animation end")
                    }
                })
            }.start()
        }
    }

    fun resetImmediate() {
        Timber.d("resetImmediate: animationRunning=%b", animationRunning)
        if (!animationRunning) {
            boundViews.forEach { v -> v.transform(0, scrollOffsetLimit) }
            listener?.onResetToStart()
        }
    }

    fun snapToLimit() {
        Timber.d("snapToLimit: animationRunning=%b", animationRunning)
        if (!animationRunning) {
            animationRunning = true
            boundViews.forEach { it.view.isClickable = false }
            AnimatorSet().apply {
                playTogether(boundViews.flatMap { it.getAnimators(scrollOffsetLimit, 1.0f) })
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        Timber.d("snapToLimit animation start")
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        animationRunning = false
                        listener?.onSnapToEnd()
                        Timber.d("snapToLimit animation end")
                    }
                })
            }.start()
        }
    }
}

