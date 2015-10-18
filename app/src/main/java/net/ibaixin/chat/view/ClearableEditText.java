/**
 * Copyright 2014 Alex Yanchenko
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package net.ibaixin.chat.view;

import net.ibaixin.chat.R;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.view.adapter.TextWatcherAdapter;
import net.ibaixin.chat.view.adapter.TextWatcherAdapter.TextWatcherListener;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.widget.EditText;

/**
 * To change clear icon, set
 * 
 * <pre>
 * android:drawableRight="@drawable/custom_icon"
 * </pre>
 */
public class ClearableEditText extends EditText implements OnTouchListener,
		OnFocusChangeListener, TextWatcherListener {

	public interface Listener {
		void didClearText();
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	private Drawable xD;
	private Listener listener;
	
	private int baseColor;

	public ClearableEditText(Context context) {
		super(context);
		init();
	}

	public ClearableEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public ClearableEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	@Override
	public void setOnTouchListener(OnTouchListener l) {
		this.l = l;
	}

	@Override
	public void setOnFocusChangeListener(OnFocusChangeListener f) {
		this.f = f;
	}

	private OnTouchListener l;
	private OnFocusChangeListener f;

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (getCompoundDrawables()[2] != null) {
			boolean tappedX = event.getX() > (getWidth() - getPaddingRight() - xD
					.getIntrinsicWidth());
			if (tappedX) {
				if (event.getAction() == MotionEvent.ACTION_UP) {
					setText("");
					if (listener != null) {
						listener.didClearText();
					}
				}
				return true;
			}
		}
		if (l != null) {
			return l.onTouch(v, event);
		}
		return false;
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		if (hasFocus) {
			setClearIconVisible(!TextUtils.isEmpty(getText()));
		} else {
			setClearIconVisible(false);
		}
		if (f != null) {
			f.onFocusChange(v, hasFocus);
		}
	}

	@Override
	public void onTextChanged(EditText view, String text) {
		if (isFocused()) {
			setClearIconVisible(!TextUtils.isEmpty(text));
		}
	}

	private void init() {
		
		baseColor = getBaseColor();
		
		xD = getCompoundDrawables()[2];
		if (xD == null) {
			xD = getResources()
					.getDrawable(R.drawable.abc_ic_clear_mtrl_alpha);
		}
		xD.setColorFilter(baseColor, PorterDuff.Mode.SRC_IN);
		xD.setBounds(0, 0, xD.getIntrinsicWidth(), xD.getIntrinsicHeight());
		setClearIconVisible(false);
		super.setOnTouchListener(this);
		super.setOnFocusChangeListener(this);
		addTextChangedListener(new TextWatcherAdapter(this, this));
	}
	
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private int getBaseColor() {
		// retrieve the default primaryColor
		int defaultPrimaryColor = Color.BLACK;
		try {
			TypedValue primaryColorTypedValue = new TypedValue();
			int colorPrimaryId = getResources().getIdentifier("colorPrimary", "attr", getContext().getPackageName());
			if (colorPrimaryId != 0) {
				getContext().getTheme().resolveAttribute(colorPrimaryId, primaryColorTypedValue, true);
				defaultPrimaryColor = primaryColorTypedValue.data;
			} else {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					getContext().getTheme().resolveAttribute(android.R.attr.colorPrimary, primaryColorTypedValue, true);
					defaultPrimaryColor = primaryColorTypedValue.data;
				}
			}
		} catch (Exception e) {
			Log.e(e.getMessage());
		}
		return defaultPrimaryColor;
	}

	protected void setClearIconVisible(boolean visible) {
		boolean wasVisible = (getCompoundDrawables()[2] != null);
		if (visible != wasVisible) {
			Drawable x = visible ? xD : null;
			setCompoundDrawables(getCompoundDrawables()[0],
					getCompoundDrawables()[1], x, getCompoundDrawables()[3]);
		}
	}
}
