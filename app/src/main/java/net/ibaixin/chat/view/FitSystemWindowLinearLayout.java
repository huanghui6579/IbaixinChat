package net.ibaixin.chat.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class FitSystemWindowLinearLayout extends LinearLayout {

	public FitSystemWindowLinearLayout(Context context) {
		this(context, null);
	}

	public FitSystemWindowLinearLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public FitSystemWindowLinearLayout(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		
		setFitsSystemWindows(true);
	}

	@Override
	protected boolean fitSystemWindows(Rect insets) {
		// 将顶部的StatusBar、ActionBar的高度忽略掉，因为我们的实现是隐藏ActionBar，跟StatusBar重叠的
		insets.top = 0;
		super.fitSystemWindows(insets);
		return true;
	}
}