package net.ibaixin.chat.loader;

import java.io.File;
import java.util.List;

import net.ibaixin.chat.manager.MsgManager;
import net.ibaixin.chat.model.FileItem;
import net.ibaixin.chat.util.SystemUtil;
import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

/**
 * 文件列表加载器
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年11月21日 下午3:17:49
 */
public class FileItemLoder extends AsyncTaskLoader<List<FileItem>> {
	private MsgManager msgManager = MsgManager.getInstance();
	private List<FileItem> list;
	private File dir;

	public FileItemLoder(Context context, File dir) {
		super(context);
		this.dir = dir;
	}

	@Override
	protected void onStartLoading() {
		if (!SystemUtil.isEmpty(list)) {
			deliverResult(list);
		}
		if (takeContentChanged() || SystemUtil.isEmpty(list)) {
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
	public List<FileItem> loadInBackground() {
		if (dir == null) {
			dir = SystemUtil.getSDCardRoot();
		}
		list = msgManager.listFileItems(dir);
		return list;
	}

}
