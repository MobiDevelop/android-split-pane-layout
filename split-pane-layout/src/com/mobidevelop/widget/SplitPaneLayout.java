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

package com.mobidevelop.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class SplitPaneLayout extends ViewGroup {
	private int mOrientation;
	private int mSplitterSize;
	private int mSplitterPositionPixel;
	private int mSplitterPositionPercent;

	private int mSplitterBackgroundResource;

	private boolean mSplitterPositionConfigured;

	private boolean isDragging = false;
	private Rect curr = new Rect();
	private Rect temp = new Rect();
	private Paint paint = new Paint();
	int lastX;
	int lastY;

	Drawable mSplitter;

	public SplitPaneLayout(Context context) {
		super(context);
	}

	public SplitPaneLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public SplitPaneLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		if (attrs != null) {
			TypedArray a = context.obtainStyledAttributes(attrs,
					R.styleable.SplitPaneLayout);
			mOrientation = a.getInt(R.styleable.SplitPaneLayout_orientation, 0);
			mSplitterSize = a.getDimensionPixelSize(
					R.styleable.SplitPaneLayout_splitterSize, 12);

			TypedValue value = a
					.peekValue(R.styleable.SplitPaneLayout_splitterPosition);
			if (value != null) {
				if (value.type == TypedValue.TYPE_DIMENSION) {
					mSplitterPositionPixel = a.getDimensionPixelSize(
							R.styleable.SplitPaneLayout_splitterPosition, 0);
				} else if (value.type == TypedValue.TYPE_FRACTION) {
					mSplitterPositionPercent = Math.round(a.getFraction(
							R.styleable.SplitPaneLayout_splitterPosition, 100,
							100, 50));
				}
			} else {
				mSplitterPositionPixel = 0;
				mSplitterPositionPercent = 50;
			}
			mSplitterBackgroundResource = a.getResourceId(
					R.styleable.SplitPaneLayout_splitterBackground, 0);
			if (mSplitterBackgroundResource != 0) {
				mSplitter = context.getResources().getDrawable(
						mSplitterBackgroundResource);
			}
			a.recycle();
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);

		check();
		switch (mOrientation) {
		case 0: {
			if (!mSplitterPositionConfigured) {
				if (mSplitterPositionPixel == 0
						&& mSplitterPositionPercent != 0) {
					mSplitterPositionPixel = Math.round(widthSize
							* (mSplitterPositionPercent / 100.0f));
					mSplitterPositionConfigured = true;
				} else if (mSplitterPositionPixel != 0
						&& mSplitterPositionPercent == 0) {
					mSplitterPositionPercent = Math
							.round(((float) mSplitterPositionPixel / (float) widthSize) * 100);
					mSplitterPositionConfigured = true;
				}
				this.getChildAt(0).measure(
						MeasureSpec.makeMeasureSpec(mSplitterPositionPixel
								- (mSplitterSize / 2), MeasureSpec.EXACTLY),
						MeasureSpec.makeMeasureSpec(heightSize,
								MeasureSpec.EXACTLY));

				this.getChildAt(1).measure(
						MeasureSpec.makeMeasureSpec(widthSize
								- mSplitterPositionPixel - (mSplitterSize / 2),
								MeasureSpec.EXACTLY),
						MeasureSpec.makeMeasureSpec(heightSize,
								MeasureSpec.EXACTLY));
			}
		}
		case 1: {
			if (!mSplitterPositionConfigured) {
				if (mSplitterPositionPixel == 0
						&& mSplitterPositionPercent != 0) {
					mSplitterPositionPixel = Math.round(heightSize
							* (mSplitterPositionPercent / 100.0f));
					mSplitterPositionConfigured = true;
				} else if (mSplitterPositionPixel != 0
						&& mSplitterPositionPercent == 0) {
					mSplitterPositionPercent = Math
							.round(((float) mSplitterPositionPixel / (float) heightSize) * 100);
					mSplitterPositionConfigured = true;
				}

				this.getChildAt(0).measure(
						MeasureSpec.makeMeasureSpec(widthSize,
								MeasureSpec.EXACTLY),
						MeasureSpec.makeMeasureSpec(

						mSplitterPositionPixel - (mSplitterSize / 2),
								MeasureSpec.EXACTLY));

				this.getChildAt(1).measure(
						MeasureSpec.makeMeasureSpec(heightSize,
								MeasureSpec.EXACTLY),
						MeasureSpec.makeMeasureSpec(heightSize
								- mSplitterPositionPixel - (mSplitterSize / 2),
								MeasureSpec.EXACTLY));
			}
		}
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		int w = r - l;
		int h = b - t;
		
		check();
		switch (mOrientation) {
		case 0: {
			if (mSplitterPositionPixel == 0 && mSplitterPositionPercent != 0) {
				mSplitterPositionPixel = Math.round(w
						* (mSplitterPositionPercent / 100.0f));
				mSplitterPositionConfigured = true;
			} else if (mSplitterPositionPixel != 0
					&& mSplitterPositionPercent == 0) {
				mSplitterPositionPercent = Math
						.round(((float) mSplitterPositionPixel / (float) w) * 100);
				mSplitterPositionConfigured = true;
			} else {
				mSplitterPositionPercent = Math
						.round(((float) mSplitterPositionPixel / (float) w) * 100);
				mSplitterPositionConfigured = true;
			}
			this.getChildAt(0).measure(
					MeasureSpec.makeMeasureSpec(mSplitterPositionPixel
							- (mSplitterSize / 2) - l, MeasureSpec.EXACTLY),
					MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
			this.getChildAt(0).layout(l, t,
					mSplitterPositionPixel - (mSplitterSize / 2), b);
			curr.set(mSplitterPositionPixel - (mSplitterSize / 2), t,
					mSplitterPositionPixel + (mSplitterSize / 2), b);
			this.getChildAt(1).measure(
					MeasureSpec.makeMeasureSpec(r
							- (mSplitterPositionPixel + (mSplitterSize / 2)),
							MeasureSpec.EXACTLY),
					MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
			this.getChildAt(1).layout(
					mSplitterPositionPixel + (mSplitterSize / 2), t, r, b);
			break;
		}
		case 1: {
			if (mSplitterPositionPixel == 0 && mSplitterPositionPercent != 0) {
				mSplitterPositionPixel = Math.round(h
						* (mSplitterPositionPercent / 100.0f));
				mSplitterPositionConfigured = true;
			} else if (mSplitterPositionPixel != 0
					&& mSplitterPositionPercent == 0) {
				mSplitterPositionPercent = Math
						.round(((float) mSplitterPositionPixel / (float) h) * 100);
				mSplitterPositionConfigured = true;
			} else {
				mSplitterPositionPercent = Math
						.round(((float) mSplitterPositionPixel / (float) h) * 100);
				mSplitterPositionConfigured = true;
			}

			this.getChildAt(0).layout(l, t, r,
					mSplitterPositionPixel - (mSplitterSize / 2));
			curr.set(l, mSplitterPositionPixel - (mSplitterSize / 2), r,
					mSplitterPositionPixel + (mSplitterSize / 2));
			this.getChildAt(1).layout(l,
					mSplitterPositionPixel + (mSplitterSize / 2), r, b);
			break;
		}
		}
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		mSplitter.setBounds(curr);
		mSplitter.draw(canvas);
		if (isDragging) {
			paint.setColor(Color.argb(128, 255, 255, 255));
			canvas.drawRect(temp, paint);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int x = (int) event.getX();
		int y = (int) event.getY();

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN: {
			switch (mOrientation) {
			case 0: {
				temp.set(curr);
				break;
			}
			case 1: {
				temp.set(curr);
				break;
			}
			}
			if (temp.contains(x, y)) {
				isDragging = true;
			}
			lastX = x;
			lastY = y;
			invalidate();
			break;
		}
		case MotionEvent.ACTION_MOVE: {
			if (isDragging) {
				switch (mOrientation) {
				case 0: {
					temp.offset((x - lastX), 0);
					break;
				}
				case 1: {
					temp.offset(0, (int) (y - lastY));
					break;
				}
				}
				lastX = x;
				lastY = y;
				invalidate();
			}
			break;
		}
		case MotionEvent.ACTION_UP: {
			if (isDragging) {
				isDragging = false;
				switch (mOrientation) {
				case 0: {
					mSplitterPositionPixel = temp.left;
					break;
				}
				case 1: {
					mSplitterPositionPixel = temp.top;
					break;
				}
				}
				mSplitterPositionPercent = 0;
				requestLayout();
				invalidate();
			}
			break;
		}
		}
		return true;
	}

	private void check()
	{
		if (getChildCount() != 2)
		{
			throw new RuntimeException("SplitPaneLayout must have exactly two child views.");
		}
	}
}
