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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * ドラッグイメージのスケーリングを指定可能な {@link View.DragShadowBuilder} を提供します。
 * 
 * @author Koji Sugisawa
 */
public class ScaledDragShadowBuilder extends View.DragShadowBuilder {

	/**
	 * デフォルトのスケーリングサイズです。
	 */
	public static final float DEFAULT_SCALE = 1.2F;

	/**
	 * デフォルトの背景色です。
	 */
	public static final int DEFAULT_BACKGROUND_COLOR = Color.WHITE;

	/**
	 * スケーリングサイズを保持します。
	 */
	private final float mScale;

	/**
	 * 背景イメージを保持します。
	 */
	private final Drawable mBackgroundDrawable;

	/**
	 * デフォルトのスケーリングサイズとデフォルトの背景色を使用してこのクラスのインスタンスを構築するコンストラクタです。
	 * 
	 * @param view ドラッグ対象のビュー
	 */
	public ScaledDragShadowBuilder(final View view) {
		this(view, DEFAULT_SCALE, DEFAULT_BACKGROUND_COLOR);
	}

	/**
	 * 指定されたスケーリングサイズとデフォルトの背景色を使用してこのクラスのインスタンスを構築するコンストラクタです。
	 * 
	 * @param view ドラッグ対象のビュー
	 * @param scale スケーリングサイズ
	 */
	public ScaledDragShadowBuilder(final View view, final float scale) {
		this(view, scale, DEFAULT_BACKGROUND_COLOR);
	}

	/**
	 * デフォルトのスケーリングサイズと指定された背景色を使用してこのクラスのインスタンスを構築するコンストラクタです。
	 * 
	 * @param view ドラッグ対象のビュー
	 * @param color 背景色
	 */
	public ScaledDragShadowBuilder(final View view, final int color) {
		this(view, DEFAULT_SCALE, new ColorDrawable(color));
	}

	/**
	 * 指定されたスケーリングサイズと指定された背景色を使用してこのクラスのインスタンスを構築するコンストラクタです。
	 * 
	 * @param view ドラッグ対象のビュー
	 * @param scale スケーリングサイズ
	 * @param color 背景色
	 */
	public ScaledDragShadowBuilder(final View view, final float scale, final int color) {
		this(view, scale, new ColorDrawable(color));
	}

	/**
	 * デフォルトのスケーリングサイズと指定された背景イメージを使用してこのクラスのインスタンスを構築するコンストラクタです。
	 * 
	 * @param view ドラッグ対象のビュー
	 * @param background 背景イメージ
	 */
	public ScaledDragShadowBuilder(final View view, final Drawable background) {
		this(view, DEFAULT_SCALE, background);
	}

	/**
	 * 指定されたスケーリングサイズと指定された背景イメージを使用してこのクラスのインスタンスを構築するコンストラクタです。
	 * 
	 * @param view ドラッグ対象のビュー
	 * @param scale スケーリングサイズ
	 * @param background 背景イメージ
	 */
	public ScaledDragShadowBuilder(final View view, final float scale, final Drawable background) {
		super(view);
		mScale = scale;
		mBackgroundDrawable = background;
	}

	@Override
	public void onProvideShadowMetrics(final Point shadowSize, final Point shadowTouchPoint) {
		final View v = getView();
		final int w = (int) (v.getWidth() * mScale);
		final int h = (int) (v.getHeight() * mScale);
		if (mBackgroundDrawable != null) {
			mBackgroundDrawable.setBounds(0, 0, w, h);
		}
		shadowSize.set(w, h);
		shadowTouchPoint.set(w / 2, h / 2);
	}

	@Override
	public void onDrawShadow(final Canvas canvas) {
		if (mBackgroundDrawable != null) {
			mBackgroundDrawable.draw(canvas);
		}
		canvas.scale(mScale, mScale);
		getView().draw(canvas);
	}

}
