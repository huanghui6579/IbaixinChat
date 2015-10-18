package net.ibaixin.chat.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import net.ibaixin.chat.ChatApplication;

/**
 * fragment的基类
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年10月8日 下午7:35:15
 */
public abstract class BaseFragment extends Fragment {
	
	protected Context mContext;
	
	public BaseFragment() {
		Bundle args = new Bundle();
		setArguments(args);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		mContext = getActivity();
		super.onCreate(savedInstanceState);
	}
}
