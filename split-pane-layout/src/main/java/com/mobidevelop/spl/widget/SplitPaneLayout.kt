/*
 *  Android Split Pane Layout.
 *  https://github.com/MobiDevelop/android-split-pane-layout
 *  
 *  Copyright (C) 2012 Justin Shapcott
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.mobidevelop.spl.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.PaintDrawable
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.*
import com.mobidevelop.spl.R

/**
 * A layout that splits the available space between two child views.
 *
 *
 * An optionally movable bar exists between the children which allows the user
 * to redistribute the space allocated to each view.
 */
class SplitPaneLayout : ViewGroup {
    interface OnSplitterPositionChangedListener {
        fun onSplitterPositionChanged(splitPaneLayout: SplitPaneLayout, fromUser: Boolean)
    }

    /**
     * Whether the splitter is movable by the user
     */
    var isSplitterMovable = DEFAULT_IS_MOVABLE

    /**
     * Listener to receive callbacks when the splitter position is changed
     */
    var onSplitterPositionChangedListener: OnSplitterPositionChangedListener? = null

    private var mOrientation = DEFAULT_ORIENTATION
    private var mSplitterSize = 8
    private var mSplitterPosition = Int.MIN_VALUE
    private var mSplitterPositionPercent = DEFAULT_POSITION_PERCENT
    private var mSplitterTouchSlop = 0
    private var minSplitterPosition = 0
    private var mSplitterDrawable: Drawable = DEFAULT_DRAWABLE
    private var mSplitterDraggingDrawable: Drawable = DEFAULT_DRAWABLE
    private val mSplitterBounds = Rect()
    private val mSplitterTouchBounds = Rect()
    private val mSplitterDraggingBounds = Rect()
    private var lastTouchX = 0
    private var lastTouchY = 0
    private var isDragging = false
    private var isMovingSplitter = false
    private var isMeasured = false

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) :
            super(context, attrs, defStyle) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        extractAttributes(context, attrs)
        descendantFocusability = FOCUS_AFTER_DESCENDANTS
        isFocusable = true
        isFocusableInTouchMode = false
    }

    private fun extractAttributes(context: Context, attrs: AttributeSet?) {
        if (attrs == null) return

        val a = context.obtainStyledAttributes(attrs, R.styleable.SplitPaneLayout)
        mOrientation = a.getInt(R.styleable.SplitPaneLayout_orientation, ORIENTATION_HORIZONTAL)
        mSplitterSize = a.getDimensionPixelSize(
                R.styleable.SplitPaneLayout_splitterSize,
                context.resources.getDimensionPixelSize(R.dimen.spl_default_splitter_size)
        )
        isSplitterMovable = a.getBoolean(R.styleable.SplitPaneLayout_splitterMovable, true)
        a.peekValue(R.styleable.SplitPaneLayout_splitterPosition)?.let {
            when (it.type) {
                TypedValue.TYPE_DIMENSION -> {
                    mSplitterPosition = a.getDimensionPixelSize(
                            R.styleable.SplitPaneLayout_splitterPosition, Int.MIN_VALUE
                    )
                }
                TypedValue.TYPE_FRACTION -> {
                    mSplitterPositionPercent = a.getFraction(
                            R.styleable.SplitPaneLayout_splitterPosition, 100, 100, 50f
                    ) * 0.01f
                }
            }
        }
        mSplitterDrawable = a.peekValue(R.styleable.SplitPaneLayout_splitterBackground)?.let {
            when (it.type) {
                TypedValue.TYPE_REFERENCE,
                TypedValue.TYPE_STRING ->
                    a.getDrawable(R.styleable.SplitPaneLayout_splitterBackground)
                TypedValue.TYPE_INT_COLOR_ARGB8,
                TypedValue.TYPE_INT_COLOR_ARGB4,
                TypedValue.TYPE_INT_COLOR_RGB8,
                TypedValue.TYPE_INT_COLOR_RGB4 -> PaintDrawable(
                        a.getColor(R.styleable.SplitPaneLayout_splitterBackground, DEFAULT_SPLITTER_COLOR)
                )
                else -> DEFAULT_DRAWABLE
            }
        } ?: DEFAULT_DRAWABLE
        mSplitterDraggingDrawable =
                a.peekValue(R.styleable.SplitPaneLayout_splitterDraggingBackground)?.let {
                    when (it.type) {
                        TypedValue.TYPE_REFERENCE,
                        TypedValue.TYPE_STRING ->
                            a.getDrawable(R.styleable.SplitPaneLayout_splitterDraggingBackground)
                        TypedValue.TYPE_INT_COLOR_ARGB8,
                        TypedValue.TYPE_INT_COLOR_ARGB4,
                        TypedValue.TYPE_INT_COLOR_RGB8,
                        TypedValue.TYPE_INT_COLOR_RGB4 -> PaintDrawable(
                                a.getColor(
                                        R.styleable.SplitPaneLayout_splitterDraggingBackground, DEFAULT_DRAGGING_COLOR
                                )
                        )
                        else -> DEFAULT_DRAWABLE
                    }
                } ?: DEFAULT_DRAWABLE
        mSplitterTouchSlop = a.getDimensionPixelSize(
                R.styleable.SplitPaneLayout_splitterTouchSlop,
                ViewConfiguration.get(context).scaledTouchSlop
        )
        minSplitterPosition =
                a.getDimensionPixelSize(R.styleable.SplitPaneLayout_paneSizeMin, 0)
        a.recycle()
    }

    private fun computeSplitterPosition() {
        if (measuredWidth <= 0 || measuredHeight <= 0) return

        when (mOrientation) {
            ORIENTATION_HORIZONTAL -> {
                if (mSplitterPosition == Int.MIN_VALUE && mSplitterPositionPercent < 0) {
                    mSplitterPosition = measuredWidth / 2
                } else if (mSplitterPosition == Int.MIN_VALUE && mSplitterPositionPercent >= 0) {
                    mSplitterPosition = (measuredWidth * mSplitterPositionPercent).toInt()
                    if (!mSplitterPosition.between(minSplitterPosition, maxSplitterPosition)) {
                        mSplitterPosition = clamp(mSplitterPosition, minSplitterPosition, maxSplitterPosition)
                        mSplitterPositionPercent = mSplitterPosition.toFloat() / measuredWidth.toFloat()
                    }
                } else if (mSplitterPosition != Int.MIN_VALUE && mSplitterPositionPercent < 0) {
                    if (!mSplitterPosition.between(minSplitterPosition, maxSplitterPosition)) {
                        mSplitterPosition = clamp(mSplitterPosition, minSplitterPosition, maxSplitterPosition)
                    }
                    mSplitterPositionPercent = mSplitterPosition.toFloat() / measuredWidth.toFloat()
                }
                mSplitterBounds[mSplitterPosition - mSplitterSize / 2, 0, mSplitterPosition + mSplitterSize / 2] = measuredHeight
                mSplitterTouchBounds[mSplitterBounds.left - mSplitterTouchSlop, mSplitterBounds.top, mSplitterBounds.right + mSplitterTouchSlop] = mSplitterBounds.bottom
            }
            ORIENTATION_VERTICAL -> {
                if (mSplitterPosition == Int.MIN_VALUE && mSplitterPositionPercent < 0) {
                    mSplitterPosition = measuredHeight / 2
                } else if (mSplitterPosition == Int.MIN_VALUE && mSplitterPositionPercent >= 0) {
                    mSplitterPosition = (measuredHeight * mSplitterPositionPercent).toInt()
                    if (!mSplitterPosition.between(minSplitterPosition, maxSplitterPosition)) {
                        mSplitterPosition = clamp(mSplitterPosition, minSplitterPosition, maxSplitterPosition)
                        mSplitterPositionPercent = mSplitterPosition.toFloat() / measuredHeight.toFloat()
                    }
                } else if (mSplitterPosition != Int.MIN_VALUE && mSplitterPositionPercent < 0) {
                    if (!mSplitterPosition.between(minSplitterPosition, maxSplitterPosition)) {
                        mSplitterPosition = clamp(mSplitterPosition, minSplitterPosition, maxSplitterPosition)
                    }
                    mSplitterPositionPercent = mSplitterPosition.toFloat() / measuredHeight.toFloat()
                }
                mSplitterBounds[0, mSplitterPosition - mSplitterSize / 2, measuredWidth] = mSplitterPosition + mSplitterSize / 2
                mSplitterTouchBounds[mSplitterBounds.left, mSplitterBounds.top - mSplitterTouchSlop / 2, mSplitterBounds.right] = mSplitterBounds.bottom + mSplitterTouchSlop / 2
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        check()
        if (measuredWidth <= 0 || measuredHeight <= 0) return

        computeSplitterPosition()
        when (mOrientation) {
            ORIENTATION_HORIZONTAL -> {
                getChildAt(0).measure(MeasureSpec.makeMeasureSpec(mSplitterPosition - mSplitterSize / 2, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY))
                getChildAt(1).measure(MeasureSpec.makeMeasureSpec(measuredWidth - mSplitterSize / 2 - mSplitterPosition, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY))
            }
            ORIENTATION_VERTICAL -> {
                getChildAt(0).measure(MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(mSplitterPosition - mSplitterSize / 2, MeasureSpec.EXACTLY))
                getChildAt(1).measure(MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(measuredHeight - mSplitterSize / 2 - mSplitterPosition, MeasureSpec.EXACTLY))
            }
        }
        isMeasured = true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = right - left
        val height = bottom - top
        when (mOrientation) {
            ORIENTATION_HORIZONTAL -> {
                getChildAt(0).layout(0, 0, mSplitterPosition - mSplitterSize / 2, height)
                getChildAt(1).layout(mSplitterPosition + mSplitterSize / 2, 0, right, height)
            }
            ORIENTATION_VERTICAL -> {
                getChildAt(0).layout(0, 0, width, mSplitterPosition - mSplitterSize / 2)
                getChildAt(1).layout(0, mSplitterPosition + mSplitterSize / 2, width, height)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        var remeasure = false
        var offset = mSplitterSize
        if (event.isShiftPressed) {
            offset *= 5
        }
        when (mOrientation) {
            ORIENTATION_HORIZONTAL -> if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                mSplitterPosition = clamp(mSplitterPosition - offset, minSplitterPosition, maxSplitterPosition)
                mSplitterPositionPercent = -1f
                remeasure = true
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                mSplitterPosition = clamp(mSplitterPosition + offset, minSplitterPosition, maxSplitterPosition)
                mSplitterPositionPercent = -1f
                remeasure = true
            }
            ORIENTATION_VERTICAL -> if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                mSplitterPosition = clamp(mSplitterPosition - offset, minSplitterPosition, maxSplitterPosition)
                mSplitterPositionPercent = -1f
                remeasure = true
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                mSplitterPosition = clamp(mSplitterPosition + offset, minSplitterPosition, maxSplitterPosition)
                mSplitterPositionPercent = -1f
                remeasure = true
            }
        }
        if (remeasure) {
            remeasure()
            notifySplitterPositionChanged(true)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isSplitterMovable) {
            val x = event.x.toInt()
            val y = event.y.toInt()
            when (event.action) {
                MotionEvent.ACTION_DOWN -> handleTouchDown(x, y)
                MotionEvent.ACTION_MOVE -> handleTouchMove(x, y)
                MotionEvent.ACTION_UP -> handleTouchUp(x, y)
            }
            return true
        }
        return false
    }

    private fun handleTouchDown(x: Int, y: Int) {
        if (mSplitterTouchBounds.contains(x, y)) {
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            isDragging = true
            mSplitterDraggingBounds.set(mSplitterBounds)
            invalidate(mSplitterDraggingBounds)
            lastTouchX = x
            lastTouchY = y
        }
    }

    private fun handleTouchMove(x: Int, y: Int) {
        if (isDragging) {
            if (!isMovingSplitter) {
                // Verify we've moved far enough to leave the touch bounds before moving the splitter
                isMovingSplitter = if (mSplitterTouchBounds.contains(x, y)) {
                    return
                } else {
                    true
                }
            }
            var take = true
            when (mOrientation) {
                ORIENTATION_HORIZONTAL -> {
                    mSplitterDraggingBounds.offset(x - lastTouchX, 0)
                    if (mSplitterDraggingBounds.centerX() < minSplitterPosition) {
                        take = false
                        mSplitterDraggingBounds.offset(minSplitterPosition - mSplitterDraggingBounds.centerX(), 0)
                    }
                    if (mSplitterDraggingBounds.centerX() > maxSplitterPosition) {
                        take = false
                        mSplitterDraggingBounds.offset(maxSplitterPosition - mSplitterDraggingBounds.centerX(), 0)
                    }
                }
                ORIENTATION_VERTICAL -> {
                    mSplitterDraggingBounds.offset(0, y - lastTouchY)
                    if (mSplitterDraggingBounds.centerY() < minSplitterPosition) {
                        take = false
                        mSplitterDraggingBounds.offset(0, minSplitterPosition - mSplitterDraggingBounds.centerY())
                    }
                    if (mSplitterDraggingBounds.centerY() > maxSplitterPosition) {
                        take = false
                        mSplitterDraggingBounds.offset(0, maxSplitterPosition - mSplitterDraggingBounds.centerY())
                    }
                }
            }
            if (take) {
                lastTouchX = x
                lastTouchY = y
            }
            invalidate()
        }
    }

    private fun handleTouchUp(x: Int, y: Int) {
        if (isDragging) {
            isDragging = false
            isMovingSplitter = false
            when (mOrientation) {
                ORIENTATION_HORIZONTAL -> {
                    mSplitterPosition = clamp(x, minSplitterPosition, maxSplitterPosition)
                    mSplitterPositionPercent = -1f
                }
                ORIENTATION_VERTICAL -> {
                    mSplitterPosition = clamp(y, minSplitterPosition, maxSplitterPosition)
                    mSplitterPositionPercent = -1f
                }
            }
            remeasure()
            notifySplitterPositionChanged(true)
        }
    }

    private val maxSplitterPosition: Int
        get() {
            when (mOrientation) {
                ORIENTATION_HORIZONTAL -> return measuredWidth - minSplitterPosition
                ORIENTATION_VERTICAL -> return measuredHeight - minSplitterPosition
            }
            return 0
        }

    public override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        return SavedState(superState).apply {
            mSplitterPositionPercent = this@SplitPaneLayout.mSplitterPositionPercent
        }
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        } else {
            super.onRestoreInstanceState(state.superState)
            splitterPositionPercent = state.mSplitterPositionPercent
        }
    }

    /**
     * Convenience for calling own measure method.
     */
    private fun remeasure() {
        // TODO: Performance: Guard against calling too often, can it be done without requestLayout?
        forceLayout()
        measure(MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY)
        )
        requestLayout()
    }

    /**
     * Checks that layout has exactly two children.
     */
    private fun check() {
        require(childCount == 2) { "SplitPaneLayout must have exactly two child views." }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        mSplitterDrawable.state = drawableState
        mSplitterDrawable.bounds = mSplitterBounds
        mSplitterDrawable.draw(canvas)
        if (isDragging) {
            mSplitterDraggingDrawable.state = drawableState
            mSplitterDraggingDrawable.bounds = mSplitterDraggingBounds
            mSplitterDraggingDrawable.draw(canvas)
        }
    }

    /**
     * Current drawable used for the splitter.
     */
    var splitterDrawable: Drawable
        get() = mSplitterDrawable
        set(value) {
            mSplitterDrawable = value
            check()
            remeasure()
        }

    /**
     * The current drawable to use while dragging the splitter.
     */
    var splitterDraggingDrawable: Drawable
        get() = mSplitterDraggingDrawable
        set(value) {
            mSplitterDraggingDrawable = value
            if (isDragging) {
                invalidate()
            }
        }

    /**
     * The current orientation of the layout.
     */
    var orientation: Int
        get() = mOrientation
        set(value) {
            if (mOrientation != value) {
                mOrientation = value
                check()
                remeasure()
            }
        }

    /**
     * The current size of the splitter in pixels.
     */
    var splitterSize: Int
        get() = mSplitterSize
        set(value) {
            mSplitterSize = value
            check()
            remeasure()
        }

    /**
     * The current position of the splitter in pixels.
     */
    var splitterPosition: Int
        get() = mSplitterPosition
        set(value) {
            mSplitterPosition = clamp(value, 0, Int.MAX_VALUE)
            mSplitterPositionPercent = -1f
            remeasure()
            notifySplitterPositionChanged(false)
        }

    /**
     * The current position of the splitter as a percentage of the layout.
     */
    var splitterPositionPercent: Float
        get() = mSplitterPositionPercent
        set(value) {
            mSplitterPosition = Int.MIN_VALUE
            mSplitterPositionPercent = clamp(value, 0f, 1f)
            remeasure()
            notifySplitterPositionChanged(false)
        }

    /**
     * The current "touch slop" which is used to extends the grab size of the splitter
     * and requires the splitter to be dragged at least this far to be considered a move.
     */
    var splitterTouchSlop: Int
        get() = mSplitterTouchSlop
        set(value) {
            mSplitterTouchSlop = value
            computeSplitterPosition()
        }

    /**
     * Minimum size of panes, in pixels.
     */
    var paneSizeMin: Int
        get() = minSplitterPosition
        set(value) {
            minSplitterPosition = value
            if (isMeasured) {
                val newSplitterPosition =
                        clamp(mSplitterPosition, minSplitterPosition, maxSplitterPosition)
                if (newSplitterPosition != mSplitterPosition) {
                    splitterPosition = newSplitterPosition
                }
            }
        }

    private fun notifySplitterPositionChanged(fromUser: Boolean) {
        Log.d("SPL", "Splitter Position Changed")
        onSplitterPositionChangedListener?.onSplitterPositionChanged(this, fromUser)
    }

    /**
     * Holds important values when we need to save instance state.
     */
    class SavedState : BaseSavedState {
        var mSplitterPositionPercent = DEFAULT_POSITION_PERCENT

        internal constructor(superState: Parcelable?) : super(superState)
        private constructor(parcel: Parcel) : super(parcel) {
            mSplitterPositionPercent = parcel.readFloat()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeFloat(mSplitterPositionPercent)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }

    companion object {
        const val ORIENTATION_HORIZONTAL = 0
        const val ORIENTATION_VERTICAL = 1
        const val DEFAULT_ORIENTATION = ORIENTATION_HORIZONTAL
        const val DEFAULT_POSITION_PERCENT = 0.5f
        const val DEFAULT_IS_MOVABLE = true
        const val DEFAULT_DRAGGING_COLOR = -0x77000001
        const val DEFAULT_SPLITTER_COLOR = -0x1000000
        val DEFAULT_DRAWABLE = PaintDrawable(DEFAULT_DRAGGING_COLOR)
        private fun clamp(value: Float, min: Float, max: Float): Float {
            return when {
                value < min -> min
                value > max -> max
                else -> value
            }
        }

        private fun clamp(value: Int, min: Int, max: Int): Int {
            return when {
                value < min -> min
                value > max -> max
                else -> value
            }
        }

        private fun Int.between(min: Int, max: Int): Boolean {
            return this in min..max
        }
    }
}