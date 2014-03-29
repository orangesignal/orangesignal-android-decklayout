/*
 * Copyright 2011-2014 the original author or authors.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 */

package com.orangesignal.android.decklayout;

import static android.view.ViewGroup.LayoutParams.FILL_PARENT;
import com.orangesignal.android.decklayout.R;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

/**
 * Displays children as a deck.
 * This layout handles gestures to show/hide the various cards of the deck.
 * 
 * @author Koji Sugisawa
 */
public class Deck extends FrameLayout {

	/**
	 * デッキカードの削除をハンドリングするためのコールバックインタフェースを提供します。
	 */
	public static interface OnDeckCardRemoveListener {

		/**
		 * デッキの {@link Deck#onLayout(boolean, int, int, int, int)} 時に呼び出されます。<p>
		 * サブクラスは削除可能通知領域の表示/非表示を行うべきです。
		 * 
		 * @param deck デッキ
		 */
		void onLayout(Deck deck);

		/**
		 * デッキカードがドラッグ中である場合に呼び出されます。
		 * サブクラスはデッキカードが一定量移動した場合に、ユーザーへの削除可能通知アニメーションなどの表示を行うべきです。
		 * 
		 * @param deck デッキ
		 * @param firstCard
		 */
		void onRemoveDrag(Deck deck, View firstCard);

		/**
		 * デッキカードのドラッグが完了した場合に呼び出されます。
		 * サブクラスは {@link #onRemoveDrag(Deck, View)} での削除判断基準にもとづいてデッキカードの削除処理を行うべきです。
		 * 
		 * @param deck デッキ
		 */
		void onRemoveDragEnd(Deck deck);
	};

	/**
	 * デッキカード削除用リスナを保持します。
	 */
	private OnDeckCardRemoveListener mOnDeckCardRemoveListener;

	/**
	 * Horizontal spacing between item.
	 */
	private int mSpacing = 0;

	private int mBounce = 20;

	private int mBounceDuration = 100;

	/**
	 * デッキカードの最小基準幅を保持します。
	 */
	private int mMinCardWidth;

	/**
	 * Left most edge of a child seen so far during layout.
	 */
	private int mMinLeftMost;

	/**
	 * Left most edge of a child seen so far during layout.
	 */
	private int mMaxLeftMost;

	private View mSideView;

	/**
	 * ドラッグの重み係数を保持します。
	 */
	private float mDragWeight = 1.0F;	// 1.0F で等価

	/**
	 * Position of the last motion event.
	 */
	private float mLastMotionX;

	private float mLastMotionY;

	/**
	 * True if the user is currently dragging this ScrollView around. This is
	 * not the same as 'is being flinged', which can be checked by
	 * mScroller.isFinished() (flinging begins when the user lifts his finger).
	 */
	private boolean mIsBeingDragged = false;

    public boolean mDisallowInterceptTouchEvent = false;

	/**
	 * Determines speed during touch scrolling
	 */
	private VelocityTracker mVelocityTracker;

	private final int mTouchSlop;
//	private final int mMinimumVelocity;
	private final int mMaximumVelocity;

	/**
	 * ID of the active pointer. This is used to retain consistency during
	 * drags/flings if multiple pointers are used.
	 */
	private int mActivePointerId = INVALID_POINTER;

	/**
	 * Sentinel value for no current active pointer.
	 * Used by {@link #mActivePointerId}.
	 */
	private static final int INVALID_POINTER = -1;

	//////////////////////////////////////////////////////////////////////////
	// コンストラクタ

	/**
	 * コンストラクタです。
	 * 
	 * @param context コンテキスト
	 */
	public Deck(final Context context) {
		this(context, null);
	}

	/**
	 * コンストラクタです。
	 * 
	 * @param context コンテキスト
	 * @param attrs
	 */
	public Deck(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * コンストラクタです。
	 * 
	 * @param context コンテキスト
	 * @param attrs
	 * @param defStyle
	 */
	public Deck(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);

		final ViewConfiguration configuration = ViewConfiguration.get(context);
		mTouchSlop = configuration.getScaledTouchSlop();
//		mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
		mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
	}

	//////////////////////////////////////////////////////////////////////////

	/**
	 * このビューとすべての子ビューに対するサイズの要求を決定するために呼び出され、
	 * 要求された幅と要求された高さから縦置き/横置きに適したデッキカードの幅を算出して各デッキカードに設定します。
	 */
	@Override
	protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
		// この View の幅と高さから縦置き/横置きに適したカード幅を算出して各カードに設定します。
		final int measureWidth = MeasureSpec.getSize(widthMeasureSpec);
		final int measureHeight = MeasureSpec.getSize(heightMeasureSpec);

		// このビューの要求する幅が要求する高さ未満の場合は、縦置きと判断します。
		if (measureWidth < measureHeight) {
			mMinCardWidth = measureWidth - mMaxLeftMost;
		// それ以外の場合は、横置きと判断します。
		} else {
			mMinCardWidth = (measureWidth - mMinLeftMost) / 2;
		}

		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			if (child.getVisibility() != GONE) {
				if (isCard(child)) {
					if (child instanceof FixedDeckCardLayout) {
						// 何も行いません
					} else if (child instanceof FillDeckCardLayout) {
						child.getLayoutParams().width = measureWidth - mMinLeftMost + child.getPaddingLeft() + child.getPaddingRight();
					} else {
						child.getLayoutParams().width = mMinCardWidth + child.getPaddingLeft() + child.getPaddingRight();
					}
				}
				measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
			}
		}

		setMeasuredDimension(measureWidth, measureHeight);
	}

	/**
	 * すべての子ビューに対してサイズと位置を割り当てるべきときに呼び出され、
	 * {@link #onMeasure(int, int)} で幅補正されたデッキカードを適切に並べます。
	 */
	@SuppressLint("WrongCall")
	@Override
	protected void onLayout(final boolean changed, final int l, final int t, final int r, final int b) {
		final int count = getChildCount();
		if (changed) {
			final int leftCardPosition = getLeftCardPosition();

			// SideView や見えていないデッキカードは一般的なレイアウト処理を行います。
			for (int i = 0; i < leftCardPosition; i++) {
				final View child = getChildAt(i);
				if (child.getVisibility() != GONE) {
					child.layout(child.getLeft(), child.getTop(), child.getLeft() + child.getMeasuredWidth(), child.getTop() + child.getMeasuredHeight());
				}
			}

			View v = null;
			for (int i = leftCardPosition; i < count; i++) {
				final View child = getChildAt(i);
				if (child != null && child.getVisibility() != GONE) {
					if (v != null) {
						child.setX(v.getX() + v.getWidth() - v.getPaddingRight() - child.getPaddingLeft() + mSpacing);
					}
					final int width = child.getMeasuredWidth();
					final int height = child.getMeasuredHeight();
					child.layout(child.getLeft(), child.getTop(), child.getLeft() + width, child.getTop() + height);
					v = child;
				}
			}
		} else {
			for (int i = 0; i < count; i++) {
				final View child = getChildAt(i);
				if (child.getVisibility() != GONE) {
					child.layout(child.getLeft(), child.getTop(), child.getLeft() + child.getMeasuredWidth(), child.getTop() + child.getMeasuredHeight());
				}
			}
		}
		if (mOnDeckCardRemoveListener != null) {
			mOnDeckCardRemoveListener.onLayout(this);
		}
	}

/*
	@Override
	protected void dispatchDraw(final Canvas canvas) {
		super.dispatchDraw(canvas);
		canvas.save();

		final Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(Color.BLACK);

		// NOTE: Twitter for iPad や じゃらん for iPad と同じにするならここで Deck の四隅に黒い角丸を描画する
		canvas.restore();
	}
*/

	@Override
	public void addView(final View child) {
		onAddView(child);
		super.addView(child);
	}

	@Override
	public void addView(final View child, final ViewGroup.LayoutParams params) {
		onAddView(child);
		super.addView(child, params);
	}

	@Override
	public void addView(final View child, final int index) {
		onAddView(child);
		super.addView(child, index);
	}

	@Override
	public void addView(final View child, final int index, final ViewGroup.LayoutParams params) {
		onAddView(child);
		super.addView(child, index, params);
	}

	@Override
	public void addView(final View child, final int width, final int height) {
		onAddView(child);
		super.addView(child, width, height);
	}

	protected void onAddView(final View v) {
		if (!isCard(v)) {
			return;	// 追加される子ビューがサイドビューの場合は何も行いません。
		}
		v.setBackgroundResource(R.drawable.deck_card_background);

		if (v instanceof FixedDeckCardLayout) {
			// 何も行いません
		} else if (v instanceof FillDeckCardLayout) {
			final int w = getWidth() - mMinLeftMost + v.getPaddingLeft() + v.getPaddingRight();
			ViewGroup.LayoutParams lp = v.getLayoutParams();
			if (lp == null) {
				lp = new ViewGroup.LayoutParams(w, FILL_PARENT);
				v.setLayoutParams(lp);
			} else if (lp.width <= 0 || lp.width < w) {
				lp.width = w;
			}

		} else if (mMinCardWidth > 0) {
			// 最低カード幅が指定されている場合は基準を満たすようにします。
			final int w = mMinCardWidth + v.getPaddingLeft() + v.getPaddingRight();
			ViewGroup.LayoutParams lp = v.getLayoutParams();
			if (lp == null) {
				lp = new ViewGroup.LayoutParams(w, FILL_PARENT);
				v.setLayoutParams(lp);
			} else if (lp.width <= 0 || lp.width < w) {
				lp.width = w;
			}
		}

		final int lastCardPosition = getLastCardPosition();
		// はじめてのカードの場合
		if (lastCardPosition == CARD_NOT_FOUND) {
			v.setX(mMaxLeftMost - v.getPaddingLeft());
		} else {
			final View last = getChildAt(lastCardPosition);
			v.setX(Math.min(
					mMinLeftMost - last.getPaddingLeft() + last.getWidth() - last.getPaddingRight() - v.getPaddingLeft() + mSpacing,
					getWidth() - (v.getLayoutParams().width - v.getPaddingLeft() - v.getPaddingRight())
				));

			final int count = getChildCount();
			for (int i = 0; i < count; i++) {
				final View child = getChildAt(i);
				if (!isCard(child)) {
					continue;
				}
				if (getCardLeft(child) > mMinLeftMost) {
					final float oldX = child.getX();
					final float newX = mMinLeftMost - child.getPaddingLeft();
					child.setX(newX);
					animation(child, oldX, newX, false, true);
				}
			}
		}
	}

	public void requestDisallowInterceptTouchEvent() {
		mDisallowInterceptTouchEvent = true;
	}

	@Override
	public boolean onInterceptTouchEvent(final MotionEvent ev) {
		/*
		 * This method JUST determines whether we want to intercept the motion.
		 * If we return true, onMotionEvent will be called and we do the actual scrolling there.
		 */

		/*
		 * Shortcut the most recurring case: the user is in the dragging state and he is moving his finger.
		 * We want to intercept this motion.
		 */
		final int action = ev.getAction();
		if (action == MotionEvent.ACTION_MOVE && mIsBeingDragged && !mDisallowInterceptTouchEvent) {
			return true;
		}

		switch (action & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				/*
				 * Remember location of down touch.
				 * ACTION_DOWN always refers to pointer index 0.
				 */
				mLastMotionX = ev.getX();
				mLastMotionY = ev.getY();
				mActivePointerId = ev.getPointerId(0);
				break;

			case MotionEvent.ACTION_MOVE:
				if (mDisallowInterceptTouchEvent) {
					return false;
				}
				/*
				 * mIsBeingDragged == false, otherwise the shortcut would have caught it.
				 * Check whether the user has moved far enough from his original down touch.
				 */

				/*
				 * Locally do absolute value. mLastMotionX is set to the x value of the down event.
				 */
				final int activePointerId = mActivePointerId;
				if (activePointerId == INVALID_POINTER) {
					// If we don't have a valid id, the touch down wasn't on content.
					break;
				}

				final int pointerIndex = ev.findPointerIndex(activePointerId);
				if (pointerIndex != INVALID_POINTER) {
					final float x = ev.getX(pointerIndex);
					final float y = ev.getY(pointerIndex);
					final int xDiff = (int) Math.abs(x - mLastMotionX);
					final int yDiff = (int) Math.abs(y - mLastMotionY);
					if (xDiff >= yDiff && xDiff > mTouchSlop) {
						mIsBeingDragged = true;
						mLastMotionX = x;
					}
				}
				break;

			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				/* Release the drag */
				mIsBeingDragged = false;
				mDisallowInterceptTouchEvent = false;
				mActivePointerId = INVALID_POINTER;
				break;

			case MotionEvent.ACTION_POINTER_UP:
				onSecondaryPointerUp(ev);
				break;
		}

		/*
		 * The only time we want to intercept motion events is if we are in the drag mode.
		 */
		return mIsBeingDragged;
	}

	@Override
	public boolean onTouchEvent(final MotionEvent ev) {
		if (ev.getAction() == MotionEvent.ACTION_DOWN && ev.getEdgeFlags() != 0) {
			// Don't handle edge touches immediately -- they may actually belong to one of our
			// descendants.
			return false;
		}

		if (mVelocityTracker == null) {
			 mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(ev);

		final int action = ev.getAction();
		switch (action & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				mIsBeingDragged = getChildCount() != 0;
				if (!mIsBeingDragged) {
					return false;
				}

				// Remember where the motion event started
				mLastMotionX = ev.getX();
				mActivePointerId = ev.getPointerId(0);
				break;

			case MotionEvent.ACTION_MOVE:
				if (mIsBeingDragged) {
					// Scroll to follow the motion event
					final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
					float x = ev.getX(activePointerIndex);
					final int deltaX = (int) ((mLastMotionX - x) * mDragWeight);
					mLastMotionX = x;

					View view = null;	// 一つ前に処理したカード
					final int count = getChildCount();

					// 左へドラッグ中の場合
					if (deltaX > 0) {
						// 表示する必要のないカードを非表示にして描画性能を向上させます。
						hideUnderCards();

						// 各カードの左ドラッグを処理します。
						float minX = getCardCount() > 1 ? mMinLeftMost : 0F;
						for (int i = count - 1; i >= 0; i--) {
							final View child = getChildAt(i);
							if (!isCard(child)) {
								continue;
							}

							// 最前面のカード
							if (view == null) {
								// 最前面のカードは minX より左に移動できないようにします。
								float newX = child.getX() - deltaX;
								if ((newX + child.getPaddingLeft()) < minX) {
									newX = minX - child.getPaddingLeft();
								}
								child.setX(newX);
								view = child;

							// それ以降のカード(※左にスタックされたカードを除く)
							} else if (getCardLeft(child) > mMinLeftMost) {
								// 最前面でないカードは左位置を制限して mMinLeftMost より左に移動できないようにします。
								final float newX = child.getX() - deltaX;
								if ((newX + child.getPaddingLeft()) > mMinLeftMost) {
									child.setX(newX);
								} else {
									child.setX(mMinLeftMost - child.getPaddingLeft());
								}
								view = child;
							}
						}
					// 右へドラッグ中の場合
					} else if (deltaX < 0) {
						// 表示する必要のあるカードを表示します。
						int pos = getLeftCardPosition();
						if (pos != CARD_NOT_FOUND) {
							pos = pos - 1;
							for (int i = count - 1; i >= pos; i--) {
								final View child = getChildAt(i);
								if (isCard(child)) {
									child.setVisibility(View.VISIBLE);
								}
							}
						}

						for (int i = count - 1; i >= 0; i--) {
							final View child = getChildAt(i);
							if (!isCard(child)) {
								continue;
							}

							// 最前面のカード
							if (view == null) {
								// 最前面のカードは横位置について特に制限しません。
								child.setX(child.getX() - deltaX);

							// 上のカードの左端とこのカードの右端が mSpacing 分以上に離れる場合は mSpacing の間隔を空けて隣接するようにします。
							} else if (getCardLeft(view) > (getCardRight(child) + mSpacing)) {
								final float newX = getCardLeft(view) - child.getWidth() + child.getPaddingRight() - mSpacing;
								child.setX(newX);
							}
							view = child;
						}
					}

					final View child = getChildAt(getFirstCardPosition());
					if (child != null && mOnDeckCardRemoveListener != null) {
						mOnDeckCardRemoveListener.onRemoveDrag(this, child);
					}
				}
				break;

			case MotionEvent.ACTION_UP:
				if (mIsBeingDragged) {
					final VelocityTracker velocityTracker = mVelocityTracker;
					velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
					final int initialVelocity = (int) velocityTracker.getXVelocity(mActivePointerId);

					if (getChildCount() > 0) {
						if (mOnDeckCardRemoveListener != null) {
							mOnDeckCardRemoveListener.onRemoveDragEnd(this);
						}
						fling(initialVelocity);
					}

					mActivePointerId = INVALID_POINTER;
					mIsBeingDragged = false;

					if (mVelocityTracker != null) {
						mVelocityTracker.recycle();
						mVelocityTracker = null;
					}
				}
				break;

			case MotionEvent.ACTION_CANCEL:
				if (mIsBeingDragged && getChildCount() > 0) {
					mActivePointerId = INVALID_POINTER;
					mIsBeingDragged = false;
					if (mVelocityTracker != null) {
						mVelocityTracker.recycle();
						mVelocityTracker = null;
					}
				}
				break;

			case MotionEvent.ACTION_POINTER_UP:
				onSecondaryPointerUp(ev);
				break;
		}

		return true;
	}

	private void onSecondaryPointerUp(final MotionEvent ev) {
		final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
		final int pointerId = ev.getPointerId(pointerIndex);
		if (pointerId == mActivePointerId) {
			// This was our active pointer going up. Choose a new
			// active pointer and adjust accordingly.
			// NOTE: Make this decision more intelligent.
			final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
			mLastMotionX = ev.getX(newPointerIndex);
			mActivePointerId = ev.getPointerId(newPointerIndex);
			if (mVelocityTracker != null) {
				mVelocityTracker.clear();
			}
		}
	}

	/**
	 * Fling the this view
	 * 
	 * @param velocity The initial velocity in the X direction.
	 * Positive numbers mean that the finger/cursor is moving down the screen, which means we want to scroll towards the top.
	 */
	private void fling(final int velocity) {
		if (velocity < 0) {
			// 左へフリックされた場合は、子ビューを全体的に左へスライドさせます。
			showNextCard();
		} else {
			// 右へフリックされた場合は、子ビューを全体的に右へスライドさせます。
			showPreviousCard();
		}
	}

	/**
	 * 子ビューを全体的に左へスライドさせて最後の子ビューを完全に表示させます。
	 */
	private void showNextCard() {
		if (getCardCount() == 1) {
			final View child = getChildAt(getFirstCardPosition());
			final float oldX = child.getX();
			final float newX = mMaxLeftMost - child.getPaddingLeft();
			child.setX(newX);
			animation(child, oldX, newX, true, false);
			return;
		}

		final int count = getChildCount();
		final int lastCardPosition = getLastCardPosition();
		final int w = getWidth();

		float baseX = mMinLeftMost - mSpacing;
		float prevX = baseX;
		View prev = null;

		View bounceCard = null;
		int bounceDelay = 0;
		int bounce = mBounce;

		boolean hideUnderCards = true;
		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			if (!isCard(child)) {
				continue;
			}

			if (getCardLeft(child) != mMinLeftMost || i == lastCardPosition) {
				final float oldX = child.getX();
				float newX = baseX - child.getPaddingLeft() + mSpacing;

				// カードが複数ある場合、最後のカードは重ねない
				if (prev != null && i >= lastCardPosition) {
					baseX = prevX + prev.getWidth() - prev.getPaddingRight();
					newX = baseX - child.getPaddingLeft() + mSpacing;
					if (baseX < w && (newX + child.getWidth() - child.getPaddingLeft()) > w) {
						newX = w - child.getWidth() + child.getPaddingRight();
						bounce = bounce * -1;
					}
					bounceCard = prev;
				}
				child.setX(newX);
				baseX = newX + child.getWidth() - child.getPaddingRight();
				prevX = newX;
				bounceDelay = animation(child, oldX, newX, true, hideUnderCards);
				hideUnderCards = false;
			} else {
				prevX = child.getX();
			}
			prev = child;
		}

		if (bounceCard != null && getCardLeft(bounceCard) == mMinLeftMost) {
			animation(bounceCard, bounceDelay, bounce);
		}
	}

	/**
	 * 子ビューを全体的に右へスライドさせます。
	 */
	private void showPreviousCard() {
		final int count = getChildCount();

		// 見えている左端のビューを取得します。
		int baseIndex = -1;
		float baseX = mMaxLeftMost - mSpacing;

		for (int i = count - 1; i >= 0; i--) {
			final View last = getChildAt(i);
			if (!isCard(last)) {
				continue;
			}

			// 最後のビュー以外で見えている左端のビューの右座標を取得します。
			for (int j = 0; j < i; j++) {
				final View view = getChildAt(j);
				if (!isCard(view)) {
					continue;
				}
				if (((int) view.getX() + view.getPaddingLeft()) == mMinLeftMost) {
					baseIndex = j;
					baseX = view.getX() + view.getWidth() - view.getPaddingRight();
					continue;
				}
			}
			break;
		}

		for (int i = baseIndex - 1; i <= baseIndex; i++) {
			final View child = getChildAt(i);
			if (!isCard(child)) {
				continue;
			}
			child.setVisibility(View.VISIBLE);
		}

		int bounceDelay = 0;
		for (int i = baseIndex + 1; i < count; i++) {
			final View child = getChildAt(i);
			if (!isCard(child)) {
				continue;
			}
			final float oldX = child.getX();
			final float newX = baseX - child.getPaddingLeft() + mSpacing;
			child.setX(newX);
			baseX = newX + child.getWidth() - child.getPaddingRight();
			bounceDelay = animation(child, oldX, newX, true, false);
		}

		// 見えている左端のビューをバウンドさせます。
		final View bounceCard = getChildAt(baseIndex);
		if (bounceCard != null) {
			animation(bounceCard, bounceDelay, mBounce);
		}
	}

	/**
	 * 子ビューを全体的に右へスライドさせて最初のカードを見せます。
	 */
	public void showFirstCard() {
		// 最初のカードが見えている場合は何も行いません。
		if (getCardLeft(getChildAt(getFirstCardPosition())) > mMinLeftMost) {
			return;
		}

		final int count = getChildCount();
		float baseX = mMaxLeftMost - mSpacing;
		for (int i = 0; i < count; i++) {
			final View view = getChildAt(i);
			if (!isCard(view)) {
				continue;
			}
			final float oldX = view.getX();
			final float newX = baseX - view.getPaddingLeft() + mSpacing;
			view.setX(newX);
			view.setVisibility(View.VISIBLE);
			baseX = newX + view.getWidth() - view.getPaddingRight();
			animation(view, oldX, newX, true, false);
		}
	}

	protected void hideUnderCards() {
		// 表示する必要のないカードを非表示にして描画性能を向上させます。
		final int pos = getLeftCardPosition() - 1;
		for (int i = 0; i < pos; i++) {
			final View child = getChildAt(i);
			if (isCard(child)) {
				child.setVisibility(View.GONE);
			}
		}
	}

	//////////////////////////////////////////////////////////////////////////
	// アニメーション

	private static final String X_ANIMATION = "x";
	private static final int MAX_DURATION = 300;
	private static final int ONE = 1;

	/**
	 * スライドアニメーションを行います。
	 * {@code bounce} が有効な場合は、スライドアニメーションの後にバウンドアニメーションも行います。
	 * 
	 * @param card アニメーションさせるビュー
	 * @param startX 開始 X 座標 (現 X 座標)
	 * @param endX 終了 X 座標 (新 X 座標)
	 * @param bounce バウンドアニメーションを行うかどうか
	 * @param hideUnderCards 
	 * @return 移動アニメーションにかかる時間 (ミリ秒)
	 */
	private int animation(final View card, final float startX, final float endX, final boolean bounce, final boolean hideUnderCards) {
		final AnimatorSet animatorSet = new AnimatorSet();

		final ObjectAnimator moveAnimation = ObjectAnimator.ofFloat(card, X_ANIMATION, startX, endX);
		final int duration = Math.min((int) (Math.abs(startX - endX) * 0.00085F * 0.8F * 1000), MAX_DURATION);
		moveAnimation.setDuration(duration);
		moveAnimation.setInterpolator(new LinearInterpolator());
		final AnimatorSet.Builder builder = animatorSet.play(moveAnimation);

		if (bounce) {
			final ObjectAnimator bounceAnimation = ObjectAnimator.ofFloat(card, X_ANIMATION, endX, endX + (startX < endX ? mBounce : -mBounce));
			bounceAnimation.setDuration(mBounceDuration);
			bounceAnimation.setRepeatCount(ONE);
			bounceAnimation.setRepeatMode(ValueAnimator.REVERSE);
			bounceAnimation.setInterpolator(new DecelerateInterpolator());
			builder.before(bounceAnimation);
		}

		if (hideUnderCards) {
			// バウンドアニメーションを行わない場合(つまり onAddView から呼び出された場合)は、アニメーション終了時に下に隠れるカードを非表示にしてその後の描画性能を向上させます。
			animatorSet.addListener(new AnimatorListener() {	// NOPMD
				@Override public void onAnimationStart(final Animator animation) {}
				@Override public void onAnimationCancel(final Animator animation) {}
				@Override public void onAnimationRepeat(final Animator animation) {}
				@Override
				public void onAnimationEnd(final Animator animation) {
					hideUnderCards();
				}
			});
		}

		animatorSet.start();
		return duration;
	}

	/**
	 * 指定されたカードをバウンドアニメーションさせます。
	 * 
	 * @param bounceCard カード
	 * @param delay アニメーション遅延開始時間 (ミリ秒)
	 * @param bounce バウンド幅 (ピクセル)
	 */
	private void animation(final View bounceCard, final int delay, final int bounce) {
		final AnimatorSet animatorSet = new AnimatorSet();

		// NOTE - bounce animation だけだと同じ感じにならない(しょうもない)ので移動しない移動アニメーションもつけている
		final ObjectAnimator moveAnimation = ObjectAnimator.ofFloat(bounceCard, X_ANIMATION, bounceCard.getX(), bounceCard.getX());
		moveAnimation.setDuration(delay);
		moveAnimation.setInterpolator(new LinearInterpolator());
		final AnimatorSet.Builder builder = animatorSet.play(moveAnimation);

		final ObjectAnimator bounceAnimation = ObjectAnimator.ofFloat(bounceCard, X_ANIMATION, bounceCard.getX(), bounceCard.getX() + bounce);
		bounceAnimation.setDuration(mBounceDuration);
		bounceAnimation.setRepeatCount(ONE);
		bounceAnimation.setRepeatMode(ValueAnimator.REVERSE);
		bounceAnimation.setInterpolator(new DecelerateInterpolator());
		builder.before(bounceAnimation);

		animatorSet.start();
	}

	//////////////////////////////////////////////////////////////////////////
	// 利便性の為のメソッド群

	/**
	 * 指定されたビューがデッキカードであるかどうかを返します。
	 * 
	 * @param view ビュー
	 * @return デッキカードである場合は <code>true</code>。それ以外の場合は <code>false</code>
	 */
	private static boolean isCard(final View view) {
		return (view != null) && (view instanceof DeckCardLayout);
	}

	/**
	 * このデッキに含まれるデッキカードの数を算出して返します。
	 * 
	 * @return デッキカードの数
	 */
	public int getCardCount() {
		int count = 0;
		for (int i = getChildCount() - 1; i >= 0; i--) {
			final View last = getChildAt(i);
			if (!isCard(last)) {
				continue;
			}
			count++;
		}
		return count;
	}

	/**
	 * 指定されたデッキカードの左座標を算出して返します。<p>
	 * このメソッドは利便性の為に提供しています。
	 * 
	 * @param view デッキカード用のビュー
	 * @return 指定されたデッキカードの左座標
	 */
	protected static float getCardLeft(final View view) {
		return view.getX() + view.getPaddingLeft();
	}

	/**
	 * 指定されたデッキカードの右座標を算出して返します。<p>
	 * このメソッドは利便性の為に提供しています。
	 * 
	 * @param view デッキカード用のビュー
	 * @return 指定されたデッキカードの右座標
	 */
	protected static float getCardRight(final View view) {
		return view.getX() + view.getWidth() - view.getPaddingRight();		
	}

//	/**
//	 * 指定されたデッキカードの幅を算出して返します。<p>
//	 * このメソッドは利便性の為に提供しています。
//	 * 
//	 * @param view デッキカード用のビュー
//	 * @return 指定されたデッキカードの幅
//	 */
//	protected static float getCardWidth(final View view) {
//		return view.getWidth() - view.getPaddingLeft() - view.getPaddingRight();
//	}

	/**
	 * 最初のデッキカードの位置を返します。見つからない場合は {@link #CARD_NOT_FOUND} を返します。
	 * 
	 * @return 最初のデッキカードの位置。または {@link #CARD_NOT_FOUND}
	 */
	public int getFirstCardPosition() {
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			if (isCard(getChildAt(i))) {
				return i;
			}
		}
		return CARD_NOT_FOUND;
	}

	/**
	 * 最後のデッキカードの位置を返します。見つからない場合は {@link #CARD_NOT_FOUND} を返します。
	 * 
	 * @return 最後のデッキカードの位置。または {@link #CARD_NOT_FOUND}
	 */
	public int getLastCardPosition() {
		final int count = getChildCount();
		for (int i = count - 1; i >= 0; i--) {
			if (isCard(getChildAt(i))) {
				return i;
			}
		}
		return CARD_NOT_FOUND;
	}

	/**
	 * 見えているデッキカード群で最も背面のデッキカードの位置を返します。見つからない場合は {@link #CARD_NOT_FOUND} を返します。
	 * 
	 * @return 見えているデッキカード群で最も背面のデッキカードの位置。または {@link #CARD_NOT_FOUND}
	 */
	public int getLeftCardPosition() {
		int pos = getLastCardPosition();
		if (pos == CARD_NOT_FOUND) {
			return CARD_NOT_FOUND;
		}

		for (int i = pos - 1; i >= 0; i--) {
			final View view = getChildAt(i);
			if (isCard(view)) {
				pos = i;
				if (getCardLeft(view) == mMinLeftMost) {
					break;
				}
			}
		}
		return pos;
	}

	/**
	 * 見えているデッキカード群で最も前面のデッキカードの位置を返します。見つからない場合は {@link #CARD_NOT_FOUND} を返します。
	 * 
	 * @return 見えているデッキカード群で最も前面のデッキカードの位置。または {@link #CARD_NOT_FOUND}
	 */
	public int getRightCardPosition() {
		for (int i = getChildCount() - 1; i >= 0; i--) {
			final View view = getChildAt(i);
			if (isCard(view)) {
				if (getCardLeft(view) < getWidth() && getCardRight(view) > getWidth()) {
					return i;
				}
			}
		}
		return CARD_NOT_FOUND;
	}

	/**
	 * カードが見つからないことを表す定数です。
	 */
	public static final int CARD_NOT_FOUND = -1;

	//////////////////////////////////////////////////////////////////////////
	// セッター/ゲッター

	public void setLeftMost(final int min, final int max) {
		mMinLeftMost = min;
		mMaxLeftMost = max;
	}

	public void setSpacing(final int spacing) { mSpacing = spacing; }
	public int getSpacing() { return mSpacing; }

	public void setBounce(final int bounce) { mBounce = bounce; }
	public int getBounce() { return mBounce; }

	public void setBounceDuration(final int duration) { mBounceDuration = duration; }
	public int getBounceDuration() { return mBounceDuration; }

	public void setSideView(final View view, final FrameLayout.LayoutParams params) {
		removeSideView();
		mSideView = view;
		super.addView(view, 0, params);
	}

	private void removeSideView() {
		if (mSideView != null) {
			removeView(mSideView);
			mSideView = null;
		}
	}

	public void setDragWeight(final float weight) { mDragWeight = weight; }
	public float getDragWeight() { return mDragWeight; }

	/**
	 * デッキカード削除用リスナを設定します。
	 * 
	 * @param l デッキカード削除用リスナ
	 */
	public void setOnDeckCardRemoveListener(final OnDeckCardRemoveListener l) {
		mOnDeckCardRemoveListener = l;
	}

}
