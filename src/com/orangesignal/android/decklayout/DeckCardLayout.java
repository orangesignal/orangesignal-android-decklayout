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

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 * デッキカードのレイアウトクラスを提供します。<p>
 * このクラスの実装は {@link FrameLayout} とほぼ同等ですが、このビュー自信がタップされた場合にイベントを消費する点が異なります。
 * これは縦置き状態の時にデッキカードが重なった場合に下のデッキカードへイベントが通知されるのを妨げるためです。
 * 
 * @author Koji Sugisawa
 */
public class DeckCardLayout extends FrameLayout {

	/**
	 * コンストラクタです。
	 * 
	 * @param context コンテキスト
	 */
	public DeckCardLayout(final Context context) {
		super(context);
	}

	/**
	 * コンストラクタです。
	 * 
	 * @param context コンテキスト
	 * @param attrs
	 */
	public DeckCardLayout(final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}

	/**
	 * コンストラクタです。
	 * 
	 * @param context コンテキスト
	 * @param attrs
	 * @param defStyle
	 */
	public DeckCardLayout(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
	}

	/**
	 * この実装は常に {@code true} を返します。
	 */
	@Override
	public boolean onTouchEvent(final MotionEvent event) {
		final float touchX = event.getX();
		final float x = getX();
		return (touchX >= (x + getPaddingLeft()) && touchX <= (x + getWidth() - getPaddingRight()));
	}

}
