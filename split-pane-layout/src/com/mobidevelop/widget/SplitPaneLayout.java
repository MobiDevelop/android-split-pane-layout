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
import android.graphics.drawable.PaintDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ViewGroup;

public class SplitPaneLayout extends ViewGroup {
	public static final int ORIENTATION_HORIZONTAL = 0;
	public static final int ORIENTATION_VERTICAL = 1;
	
	private int mOrientation;
	private int mSplitterSize;
	private int mSplitterPositionPixel;
	private int mSplitterPositionPercent;

	private boolean isDragging = false;
	private Rect curr = new Rect();
	private Rect temp = new Rect();
	private Paint paint = new Paint();
	private int lastX;
	private int lastY;

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
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SplitPaneLayout);
			mOrientation = a.getInt(R.styleable.SplitPaneLayout_orientation, 0);
			mSplitterSize = a.getDimensionPixelSize(R.styleable.SplitPaneLayout_splitterSize, 12);

			TypedValue value = a.peekValue(R.styleable.SplitPaneLayout_splitterPosition);
			if (value != null) {
				if (value.type == TypedValue.TYPE_DIMENSION) {
					mSplitterPositionPixel = a.getDimensionPixelSize(R.styleable.SplitPaneLayout_splitterPosition, 0);
				} else if (value.type == TypedValue.TYPE_FRACTION) {
					mSplitterPositionPercent = Math.round(a.getFraction(R.styleable.SplitPaneLayout_splitterPosition, 100, 100, 50));
				}
			} else {
				mSplitterPositionPixel = 0;
				mSplitterPositionPercent = 50;
			}
			value = a.peekValue(R.styleable.SplitPaneLayout_splitterBackground);
			if (value != null) {
				if (value.type == TypedValue.TYPE_REFERENCE ||
					value.type == TypedValue.TYPE_STRING) {
					mSplitter = a.getDrawable(R.styleable.SplitPaneLayout_splitterBackground);
				}
				else
				if (value.type == TypedValue.TYPE_INT_COLOR_ARGB8 ||
					value.type == TypedValue.TYPE_INT_COLOR_ARGB4 ||
					value.type == TypedValue.TYPE_INT_COLOR_RGB8  ||
					value.type == TypedValue.TYPE_INT_COLOR_RGB4) {
					mSplitter = new PaintDrawable(a.getColor(R.styleable.SplitPaneLayout_splitterBackground, 0));
				}
			}
			a.recycle();
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		check();
		doMeasure(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		check();
		doMeasure(r - l, b - t);
		switch (mOrientation) {
			case ORIENTATION_HORIZONTAL: {
				this.getChildAt(0).layout(l, t, mSplitterPositionPixel - (mSplitterSize / 2), b);
				curr.set(mSplitterPositionPixel - (mSplitterSize / 2), t, mSplitterPositionPixel + (mSplitterSize / 2), b);
				this.getChildAt(1).layout(mSplitterPositionPixel + (mSplitterSize / 2), t, r, b);
				break;
			}
			case ORIENTATION_VERTICAL: {
				this.getChildAt(0).layout(l, t, r, mSplitterPositionPixel - (mSplitterSize / 2));
				curr.set(l, mSplitterPositionPixel - (mSplitterSize / 2), r, mSplitterPositionPixel + (mSplitterSize / 2));
				this.getChildAt(1).layout(l, mSplitterPositionPixel + (mSplitterSize / 2), r, b);
				break;
			}
		}
	}

	private void doMeasure(int width, int height) {
		switch (mOrientation) {
			case ORIENTATION_HORIZONTAL: {
				if (mSplitterPositionPixel == 0 && mSplitterPositionPercent != 0) {
					mSplitterPositionPixel = Math.round(width * (mSplitterPositionPercent / 100.0f));
				} else if (mSplitterPositionPixel != 0 && mSplitterPositionPercent == 0) {
					mSplitterPositionPercent = Math.round(((float) mSplitterPositionPixel / (float) width) * 100);
				} else {
					mSplitterPositionPercent = Math.round(((float) mSplitterPositionPixel / (float) width) * 100);
				}
				this.getChildAt(0).measure(
					MeasureSpec.makeMeasureSpec(mSplitterPositionPixel - (mSplitterSize / 2), MeasureSpec.EXACTLY),
					MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
				);

				this.getChildAt(1).measure(
					MeasureSpec.makeMeasureSpec(width - mSplitterPositionPixel - (mSplitterSize / 2), MeasureSpec.EXACTLY),
					MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
				);
				break;
			}
			case ORIENTATION_VERTICAL: {
				if (mSplitterPositionPixel == 0 && mSplitterPositionPercent != 0) {
					mSplitterPositionPixel = Math.round(height * (mSplitterPositionPercent / 100.0f));
				} else if (mSplitterPositionPixel != 0 && mSplitterPositionPercent == 0) {
					mSplitterPositionPercent = Math.round(((float) mSplitterPositionPixel / (float) height) * 100);
				} else {
					mSplitterPositionPercent = Math.round(((float) mSplitterPositionPixel / (float) height) * 100);
				}

				this.getChildAt(0).measure(
					MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
					MeasureSpec.makeMeasureSpec(mSplitterPositionPixel - (mSplitterSize / 2), MeasureSpec.EXACTLY)
				);

				this.getChildAt(1).measure(
					MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
					MeasureSpec.makeMeasureSpec(height - mSplitterPositionPixel - (mSplitterSize / 2), MeasureSpec.EXACTLY)
				);
				break;
			}
		}
	}
	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		if (mSplitter != null) {
			mSplitter.setBounds(curr);
			mSplitter.draw(canvas);
		}
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
					case ORIENTATION_HORIZONTAL: {
						temp.set(curr);
						break;
					}
					case ORIENTATION_VERTICAL: {
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
						case ORIENTATION_HORIZONTAL: {
							temp.offset((x - lastX), 0);
							break;
						}
						case ORIENTATION_VERTICAL: {
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
						case ORIENTATION_HORIZONTAL: {
							mSplitterPositionPixel = temp.left + temp.width() / 2;
							break;
						}
						case ORIENTATION_VERTICAL: {
							mSplitterPositionPixel = temp.top + temp.height() / 2;
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

	private void check() {
		if (getChildCount() != 2) {
			throw new RuntimeException("SplitPaneLayout must have exactly two child views.");
		}
	}
}
