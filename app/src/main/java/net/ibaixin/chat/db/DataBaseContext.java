package net.ibaixin.chat.db;

import java.io.File;

import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.SystemUtil;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.text.TextUtils;

/**
 * 自定义数据库目录的包装类,数据库默认的存放目录为/data/data/packname/IbaiXinChat/
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年3月9日 下午8:52:28
 */
public class DataBaseContext extends ContextWrapper {
	private static final String TAG = DataBaseContext.class.getCanonicalName();
	
	/**
	 * 数据库文件的父类路径
	 */
	private File parentDir;

	public DataBaseContext(Context base) {
		super(base);
	}
	
	public DataBaseContext(Context base, String dir) {
		this(base);
		if (!TextUtils.isEmpty(dir)) {
			this.parentDir = new File(dir);
		}
	}
	
	@Override
	public File getDatabasePath(String name) {
		if (parentDir == null) {	//选用默认的目录
			Log.d(TAG, "----选用默认的目录-----name---------" + name);
			parentDir = SystemUtil.getBaseDBDir();
		}
		if (!parentDir.exists()) {
			parentDir.mkdirs();
		}
		File result = new File(parentDir, name);
		Log.d(TAG, "-------getDatabasePath-------" + result.getAbsolutePath());
		return result;
	}
	
	@Override
	public SQLiteDatabase openOrCreateDatabase(String name, int mode,
			CursorFactory factory, DatabaseErrorHandler errorHandler) {
		return openOrCreateDatabase(name, mode, factory);
	}
	
	@Override
	public SQLiteDatabase openOrCreateDatabase(String name, int mode,
			CursorFactory factory) {
		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), null);
		Log.d(TAG, "-------openOrCreateDatabase-------" + name);
		return db;
	}

}
