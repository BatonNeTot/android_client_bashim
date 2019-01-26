package com.notjuststudio.bashim.custom

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import java.lang.Math.max
import java.lang.Math.min
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator


class ZoomImageView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : View(context, attrs, defStyleAttr) {

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    companion object {

        private const val INVALID_POINTER_ID = -1

        private const val SCALE_FACTOR_MAX = 8f

        private const val DOUBLE_TAP_SCALE = 4f
        private const val DOUBLE_TAP_DURATION = 250L

        private const val PROPERTY_SCALE = "scale"

    }

    private var mImage: Drawable? = null
    private var mPosX = 0
    private var mPosY = 0
    private var mMidPosX = 0
    private var mMidPosY = 0

    private var mLastTouchX = 0
    private var mLastTouchY = 0
    private var mLastScale = 1f
    private var mActivePointerId = INVALID_POINTER_ID

    private var mAnimator: ValueAnimator? = null
    private var mGestureDetector: GestureDetector
    private var mScaleDetector: ScaleGestureDetector
    private var mScaleWidthBigger = true
    private var mScaleFactor = 1f
    private var mScaleFactorMin = 1f
    private var mScaleFactorMax = 10f

    init {
        mGestureDetector = GestureDetector(context, GestureListener())
        mScaleDetector = ScaleGestureDetector(context, ScaleListener())
    }

    fun resetZoom() {
        if (mImage == null)
            return

        mScaleFactor = if (mScaleWidthBigger) {
            width.toFloat() / mImage!!.intrinsicWidth
        } else {
            height.toFloat() / mImage!!.intrinsicHeight
        }

        if (mScaleWidthBigger) {
            mPosX = 0
            mPosY = (height - mImage!!.intrinsicHeight * mScaleFactor).toInt() / 2
        } else {
            mPosX = (width - mImage!!.intrinsicWidth * mScaleFactor).toInt() / 2
            mPosY = 0
        }
    }

    fun setImageBitmap(bitmap: Bitmap?) {
        mImage = BitmapDrawable(resources, bitmap)
        mImage!!.setBounds(0, 0, mImage!!.intrinsicWidth, mImage!!.intrinsicHeight)

        val imageFactor = mImage!!.intrinsicWidth.toFloat() / mImage!!.intrinsicHeight
        val formFactor = width.toFloat() / height

        mScaleWidthBigger = imageFactor > formFactor

        resetZoom()

        mScaleFactorMin = mScaleFactor
        mScaleFactorMax = mScaleFactor * SCALE_FACTOR_MAX

        invalidate()
    }

    private fun midPos(event: MotionEvent) {
        val x = event.getX (0) + event.getX(1)
        val y = event.getY (0) + event.getY(1)

        mMidPosX = x.toInt() / 2
        mMidPosY = y.toInt() / 2
    }

    override fun canScrollHorizontally(direction: Int): Boolean {
        if (mImage == null)
            return super.canScrollHorizontally(direction)

        if (direction < 0) {
            return mPosX < 0
        } else {
            if (mScaleWidthBigger) {
                return mPosX > (width - mImage!!.intrinsicWidth * mScaleFactor).toInt()
            } else {
                return mPosX > min((width - mImage!!.intrinsicWidth * mScaleFactor).toInt() / 2, (width - mImage!!.intrinsicWidth * mScaleFactor).toInt())
            }
        }
    }

    override fun canScrollVertically(direction: Int): Boolean {
        if (mImage == null)
            return super.canScrollVertically(direction)

        if (direction < 0) {
            return mPosY < 0
        } else {
            if (mScaleWidthBigger) {
                return mPosY > (height - mImage!!.intrinsicHeight * mScaleFactor).toInt()
            } else {
                return mPosX > min((height - mImage!!.intrinsicHeight * mScaleFactor).toInt() / 2, (height - mImage!!.intrinsicHeight * mScaleFactor).toInt())
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent) : Boolean {
        if (mImage == null || mAnimator != null)
            return false
        // Let the ScaleGestureDetector inspect all events.

        if (event.pointerCount > 1) {
            midPos(event)
        }

        mGestureDetector.onTouchEvent(event)

        if (mAnimator != null) {
            return true
        }

        mScaleDetector.onTouchEvent(event)

        val action = event.action
        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.getX().toInt()
                val y = event.getY().toInt()

                mLastTouchX = x
                mLastTouchY = y
                mActivePointerId = event.getPointerId(0)
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                midPos(event)

                mLastTouchX = mMidPosX
                mLastTouchY = mMidPosY
            }

            MotionEvent.ACTION_MOVE -> {
                val x: Int
                val y: Int

                if (event.pointerCount > 1) {
                    x = mMidPosX
                    y = mMidPosY
                } else {
                    val pointerIndex = event.findPointerIndex(mActivePointerId)
                    x = event.getX(pointerIndex).toInt()
                    y = event.getY(pointerIndex).toInt()
                }


                val dx = x - mLastTouchX
                val dy = y - mLastTouchY

                mPosX += dx
                mPosY += dy

                applyBound()

                invalidate()

                mLastTouchX += dx
                mLastTouchY += dy
            }

            MotionEvent.ACTION_UP -> {
                mActivePointerId = INVALID_POINTER_ID
                this.performClick()
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (event.actionIndex == 1) {
                    mActivePointerId = event.getPointerId(0)
                } else {
                    mActivePointerId = event.getPointerId(1)
                }

                if (event.pointerCount == 2) {
                    val pointerIndex = event.findPointerIndex(mActivePointerId)
                    mLastTouchX = event.getX(pointerIndex).toInt()
                    mLastTouchY = event.getY(pointerIndex).toInt()
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                mActivePointerId = INVALID_POINTER_ID
            }
        }

        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (mImage == null) {
            return
        }

        canvas.save()

        canvas.translate(mPosX.toFloat(), mPosY.toFloat())
        canvas.scale(mScaleFactor, mScaleFactor, 0f, 0f)
        mImage!!.draw(canvas)

        canvas.restore()
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (mAnimator == null) {
                mMidPosX = e.getX().toInt()
                mMidPosY = e.getY().toInt()

                val propertyScale = PropertyValuesHolder.ofFloat(PROPERTY_SCALE, mScaleFactor,
                        if (mScaleFactor < DOUBLE_TAP_SCALE) {
                            mScaleFactorMin * DOUBLE_TAP_SCALE
                        } else {
                            mScaleFactorMin
                        })

                mAnimator = ValueAnimator()
                mAnimator?.setValues(propertyScale)
                mAnimator?.setDuration(DOUBLE_TAP_DURATION)
                mAnimator?.addUpdateListener {
                    applyScale(it.getAnimatedValue(PROPERTY_SCALE) as Float)
                    applyBound()

                    invalidate()
                }
                mAnimator?.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(animation: Animator?) {}
                    override fun onAnimationEnd(animation: Animator?) {
                        mAnimator = null
                    }
                    override fun onAnimationCancel(animation: Animator?) {}
                    override fun onAnimationStart(animation: Animator?) {}
                })

                mAnimator?.start()
            }

            return true
        }

    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector) : Boolean {
            applyScale(mScaleFactor * detector.scaleFactor)

            invalidate()

            return true
        }
    }

    private fun applyScale(scaleFactor: Float) {
        val newScaleFactor = min(mScaleFactorMax, max(mScaleFactorMin, scaleFactor))
        mLastScale = mScaleFactor
        mScaleFactor = newScaleFactor

        val scale = newScaleFactor / mLastScale

        mPosX = mMidPosX - ((mMidPosX - mPosX) * scale).toInt()
        mPosY = mMidPosY - ((mMidPosY - mPosY) * scale).toInt()
    }

    private fun applyBound() {
        if (mScaleWidthBigger) {
            mPosX = min(0, max((width - mImage!!.intrinsicWidth * mScaleFactor).toInt(), mPosX))
            mPosY = min(
                    max(0, (height - mImage!!.intrinsicHeight * mScaleFactor).toInt() / 2),
                    max(min((height - mImage!!.intrinsicHeight * mScaleFactor).toInt() / 2, (height - mImage!!.intrinsicHeight * mScaleFactor).toInt()),
                            mPosY))
        } else {
            mPosX = min(
                    max(0, (width - mImage!!.intrinsicWidth * mScaleFactor).toInt() / 2),
                    max(min((width - mImage!!.intrinsicWidth * mScaleFactor).toInt() / 2, (width - mImage!!.intrinsicWidth * mScaleFactor).toInt()),
                            mPosX))
            mPosY = min(0, max((height - mImage!!.intrinsicHeight * mScaleFactor).toInt(), mPosY))
        }
    }

}