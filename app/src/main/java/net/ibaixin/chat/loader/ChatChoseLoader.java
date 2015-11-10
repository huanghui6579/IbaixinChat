package net.ibaixin.chat.loader;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;

import net.ibaixin.chat.manager.MsgManager;
import net.ibaixin.chat.model.ChatChoseItem;

import java.util.List;

/**
 * 会话列表加载的后台任务
 * @author huanghui1
 * @update 2014年10月31日 下午8:59:03
 */
public class ChatChoseLoader extends AsyncTaskLoader<List<ChatChoseItem>> {
	private MsgManager msgManager = MsgManager.getInstance();
	
	public static final String ARG_LOAG_LASTMSG = "arg_loag_lastmsg";
	public static final String ARG_LOAG_TYPE = "arg_loag_type";
	
	private List<ChatChoseItem> list = null;
	
	private Bundle mBundle;

	public ChatChoseLoader(Context context) {
		super(context);
	}

	public ChatChoseLoader(Context context, Bundle args) {
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
	public List<ChatChoseItem> loadInBackground() {
		boolean loadLastMsg = true;
		int itemType = ChatChoseItem.TYPE_THREAD;
		if (mBundle != null) {
			loadLastMsg = mBundle.getBoolean(ARG_LOAG_LASTMSG, true);
			itemType = mBundle.getInt(ARG_LOAG_TYPE, ChatChoseItem.TYPE_THREAD);
		}
//		list = msgManager.getMsgThreadList(loadLastMsg);
		return list;
	}
}