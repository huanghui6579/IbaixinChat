package net.ibaixin.chat.view;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

import net.ibaixin.chat.util.Log;

/**
 * 主要界面图片查看时多点触控会报错的问题，解决问题的链接<a href="http://blog.csdn.net/nnmmbb/article/details/28419779">http://blog.csdn.net/nnmmbb/article/details/28419779</a><br/>
 * 具体报错信息：java.lang.IllegalArgumentException: pointerIndex out of range
 * @author huanghui1
 * @update 2015/12/22 18:35
 * @version: 0.0.1
 */
public class ViewPagerFixed extends ViewPager {


	public ViewPagerFixed(Context context) {
		super(context);
	}

	public ViewPagerFixed(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		try {
			return super.onTouchEvent(ev);
		} catch (Exception e) {
			Log.e(e.getMessage());
		}
		return false;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		try {
			return super.onInterceptTouchEvent(ev);
		} catch (Exception e) {
			Log.e(e.getMessage());
		}
		return false;
	}
}