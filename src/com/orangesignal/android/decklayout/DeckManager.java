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

import java.util.ArrayList;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

/**
 * バックスタックを使用せずに {@link Deck} で使用するフラグメントの関連付けを管理する機能を提供します。
 * 
 * @author 杉澤 浩二
 */
public class DeckManager {

	/**
	 * デッキカードのフラグメントのリストを保持します。
	 */
	private final ArrayList<Fragment> mFragments = new ArrayList<Fragment>(0);

	private int mAddAnimationId;
	private int mInAnimationId;
	private int mOutAnimationId;
	private int mRemoveAnimationId;

	private final Activity mActivity;
	private final Deck mDeck;

	/**
	 * コンストラクタです。
	 * 
	 * @param activity アクティビティ
	 * @param deckResId {@link Deck} のリソースID
	 */
	public DeckManager(final Activity activity, final int deckResId) {
		mActivity = activity;
		mDeck = (Deck) mActivity.findViewById(deckResId);
	}

	/**
	 * 指定されたフラグメントを指定されたタグで関連付けます。
	 * 
	 * @param fragment フラグメント
	 * @param tag タグ
	 */
	public void attach(final Fragment fragment, final String tag) {
		final InputMethodManager inputMethodManager = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (inputMethodManager != null) {
			final View currentFocus = mActivity.getCurrentFocus();
			if (currentFocus != null) {
				inputMethodManager.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
			}
		}

		final FragmentManager fm = mActivity.getFragmentManager();
		final FragmentTransaction ft = fm.beginTransaction();
		if (ft.isAddToBackStackAllowed()) {
			ft.disallowAddToBackStack();
		}

		if (fm.findFragmentByTag(tag) == null) {
			if (mAddAnimationId != 0) {
				ft.setCustomAnimations(mAddAnimationId, 0);
			}
			ft.add(mDeck.getId(), fragment, tag);
			mFragments.add(fragment);
		} else {
			// 指定されたタグより後ろのフラグメントを除去します。
			final int current = getFragmentPositionByTag(tag);
			if (current != FRAGMENT_NOT_FOUND) {
				for (int i = mFragments.size() - 1; i > current; i--) {
					if (mRemoveAnimationId != 0) {
						ft.setCustomAnimations(0, mRemoveAnimationId);
					}
					ft.remove(mFragments.remove(i));
				}
			}

			if (mInAnimationId != 0 && mOutAnimationId != 0) {
				ft.setCustomAnimations(mInAnimationId, mOutAnimationId);
			}
			if (current != FRAGMENT_NOT_FOUND) {
				ft.remove(mFragments.remove(current));
			}
			ft.add(mDeck.getId(), fragment, tag);
			mFragments.add(fragment);
		}

		ft.commit();
	}

	/**
	 * 指定されたビューに紐付くフラグメントから後ろのフラグメントを切り離します。
	 * 
	 * @param v フラグメントに紐付くビュー
	 * @param animation アニメーションを行うかどうか
	 */
	public void detach(final View v, final boolean animation) {
		final InputMethodManager inputMethodManager = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (inputMethodManager != null) {
			final View currentFocus = mActivity.getCurrentFocus();
			if (currentFocus != null) {
				inputMethodManager.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
			}
		}

		final FragmentManager fm = mActivity.getFragmentManager();
		final FragmentTransaction ft = fm.beginTransaction();
		if (ft.isAddToBackStackAllowed()) {
			ft.disallowAddToBackStack();
		}

		// 指定された View より後ろのフラグメントを除去します。
		final int current = getFragmentPositionByView(v);
		if (current != FRAGMENT_NOT_FOUND) {
			for (int i = mFragments.size() - 1; i > current; i--) {
				if (animation && mRemoveAnimationId != 0) {
					ft.setCustomAnimations(0, mRemoveAnimationId);
				}
				ft.remove(mFragments.remove(i));
			}
		}

		ft.commit();
	}

	/**
	 * 指定されたフラグメント以外の状態変更通知を受け取れるすべてのフラグメントへ通知します。
	 * 
	 * @param fragment 変更の起因となるフラグメント
	 */
	public void notifyDeckChanged(final Fragment fragment) {
		for (final Fragment f : mFragments) {
			if (!f.equals(fragment) && f instanceof DeckListener) {
				final DeckListener l = (DeckListener) f;
				l.onDeckChanged();
			}
		}
	}

	private static final int FRAGMENT_NOT_FOUND = -1;

	private int getFragmentPositionByView(final View v) {
		int i = 0;
		for (final Fragment f : mFragments) {
			if (v.equals(f.getView())) {
				return i;
			}
			i++;
		}
		return FRAGMENT_NOT_FOUND;
	}

	private int getFragmentPositionByTag(final String tag) {
		int i = 0;
		for (final Fragment f : mFragments) {
			if (tag.equals(f.getTag())) {
				return i;
			}
			i++;
		}
		return FRAGMENT_NOT_FOUND;
	}

	public void setAddAnimation(final int add) {
		mAddAnimationId = add;
	}

	public void setReplaceAnimation(final int in, final int out) {
		mInAnimationId = in;
		mOutAnimationId = out;
	}

	public void setReplaceInAnimation(final int in) {
		mInAnimationId = in;
	}

	public void setReplaceOutAnimation(final int out) {
		mOutAnimationId = out;
	}

	public void setRemoveAnimation(final int remove) {
		mRemoveAnimationId = remove;
	}

}
