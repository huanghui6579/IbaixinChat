package net.ibaixin.chat.loader;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;

import net.ibaixin.chat.manager.MsgManager;
import net.ibaixin.chat.model.MsgThread;

import java.util.List;

/**
 * 会话列表加载的后台任务
 * @author huanghui1
 * @update 2014年10月31日 下午8:59:03
 */
public class ThreadListLoader extends AsyncTaskLoader<List<MsgThread>> {
	private MsgManager msgManager = MsgManager.getInstance();
	
	private List<MsgThread> list = null;
	
	private Bundle mBundle;

	public ThreadListLoader(Context context) {
		super(context);
	}

	public ThreadListLoader(Context context, Bundle args) {
		super(context);
		this.mBundle = args;
	}
	
	@Override
	protected void onStartLoading() {
		if (list != null) {
			deliverResult(list);
		}
		if (takeContentChanged() || list == null) {
			forceLoad();
		}
	}
	
	@Override
	protected void onStopLoading() {
		cancelLoad();
	}
	
	@Override
	protected void onReset() {
		onStopLoading();
		list = null;
	}

	@Override
	public List<MsgThread> loadInBackground() {
		list = msgManager.getMsgThreadList();
		return list;
	}
}