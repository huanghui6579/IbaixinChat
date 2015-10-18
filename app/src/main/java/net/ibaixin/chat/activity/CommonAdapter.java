package net.ibaixin.chat.activity;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.BaseAdapter;

/**
 * 抽象的adapter
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年10月9日 下午10:43:10
 */
public abstract class CommonAdapter<T> extends BaseAdapter {
	protected List<T> list;
	protected Context context;
	protected LayoutInflater inflater;

	public CommonAdapter(List<T> list, Context context) {
		this.list = list;
		this.context = context;
		inflater = LayoutInflater.from(context);
	}

	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public Object getItem(int position) {
		return list.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

}
