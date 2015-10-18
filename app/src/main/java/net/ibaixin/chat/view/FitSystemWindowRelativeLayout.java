package net.ibaixin.chat.view;

import net.ibaixin.chat.R;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

public class FitSystemWindowRelativeLayout extends RelativeLayout {

	public FitSystemWindowRelativeLayout(Context context) {
		this(context, null);
	}

	public FitSystemWindowRelativeLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public FitSystemWindowRelativeLayout(Context context, AttributeSet attrs,
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