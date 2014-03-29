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

/**
 * 余白がなくなるように幅を拡げるデッキカードレイアウトクラスを提供します。
 * 
 * @author Koji Sugisawa
 */
public class FillDeckCardLayout extends DeckCardLayout {

	/**
	 * コンストラクタです。
	 * 
	 * @param context コンテキスト
	 */
	public FillDeckCardLayout(final Context context) {
		super(context);
	}

	/**
	 * コンストラクタです。
	 * 
	 * @param context コンテキスト
	 * @param attrs
	 */
	public FillDeckCardLayout(final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}

	/**
	 * コンストラクタです。
	 * 
	 * @param context コンテキスト
	 * @param attrs
	 * @param defStyle
	 */
	public FillDeckCardLayout(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
	}

}
