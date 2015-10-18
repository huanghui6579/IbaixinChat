package net.ibaixin.chat.view.listener;

import android.view.View;

/**
 * 列表每一项的单击事件监听
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年8月5日 下午8:07:15
 */
public interface OnItemClickListener {
	/**
	 * 每一项的点击事件
	 * @param view
	 * @param position
	 * @update 2015年8月5日 下午8:18:53
	 */
	public void onItemClick(View view, int position);
}