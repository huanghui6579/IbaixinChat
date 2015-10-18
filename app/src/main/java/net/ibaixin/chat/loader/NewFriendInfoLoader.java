package net.ibaixin.chat.loader;

import java.util.List;

import net.ibaixin.chat.manager.UserManager;
import net.ibaixin.chat.model.NewFriendInfo;
import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

/**
	 * 新的朋友异步加载器
	 * @author huanghui1
	 * @version 1.0.0
	 * @update 2014年11月10日 下午4:02:01
	 */
	public class NewFriendInfoLoader extends AsyncTaskLoader<List<NewFriendInfo>> {
		private UserManager userManager = UserManager.getInstance();
		private List<NewFriendInfo> list = null;
		
		public NewFriendInfoLoader(Context context) {
			super(context);
		}

		@Override
		protected void onStartLoading() {
			if (list != null) {
				deliverResult(list);
			}
			if (takeContentChanged() || list == null) {
				onForceLoad();
			}
		}

		@Override
		protected void onReset() {
			onStopLoading();
			list = null;
		}

		@Override
		protected void onStopLoading() {
			cancelLoad();
		}

		@Override
		public List<NewFriendInfo> loadInBackground() {
			list = userManager.getNewFriendInfos();
			return list;
		}
	}