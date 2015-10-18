package net.ibaixin.chat.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.ibaixin.chat.db.DatabaseHelper;
import net.ibaixin.chat.util.Log;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

/**
 * 消息的内容提供者
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年10月30日 下午3:33:33
 */
public class MsgProvider extends ContentProvider {
	private static final int MSG_INFOS = 1;
	private static final int MSG_INFO_ID = 2;
	
	private static final int MSG_PARTS = 3;
	private static final int MSG_PART_ID = 4;
	
	private static final int MSG_THREADS = 5;
	private static final int MSG_THREAD_ID = 6;
	
	private static final UriMatcher mUriMatcher;
	private static Map<String, String> mMsgInfoProjection = null;
	private static Map<String, String> mMsgPartProjection = null;
	private static Map<String, String> mMsgThreadProjection = null;
	
	private DatabaseHelper mDBHelper;
	
	static {
		mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		mUriMatcher.addURI(Provider.AUTHORITY_MSG, "msgInfos", MSG_INFOS);
		mUriMatcher.addURI(Provider.AUTHORITY_MSG, "msgInfos/#", MSG_INFO_ID);
		mUriMatcher.addURI(Provider.AUTHORITY_MSG, "msgParts", MSG_PARTS);
		mUriMatcher.addURI(Provider.AUTHORITY_MSG, "msgParts/#", MSG_PART_ID);
		mUriMatcher.addURI(Provider.AUTHORITY_MSG, "msgThreads", MSG_THREADS);
		mUriMatcher.addURI(Provider.AUTHORITY_MSG, "msgThreads/#", MSG_THREAD_ID);
		
		mMsgInfoProjection = new HashMap<>();
		mMsgInfoProjection.put(Provider.MsgInfoColumns._ID, Provider.MsgInfoColumns._ID);
		mMsgInfoProjection.put(Provider.MsgInfoColumns.THREAD_ID, Provider.MsgInfoColumns.THREAD_ID);
		mMsgInfoProjection.put(Provider.MsgInfoColumns.FROM_USER, Provider.MsgInfoColumns.FROM_USER);
		mMsgInfoProjection.put(Provider.MsgInfoColumns.TO_USER, Provider.MsgInfoColumns.TO_USER);
		mMsgInfoProjection.put(Provider.MsgInfoColumns.SUBJECT, Provider.MsgInfoColumns.SUBJECT);
		mMsgInfoProjection.put(Provider.MsgInfoColumns.CONTENT, Provider.MsgInfoColumns.CONTENT);
		mMsgInfoProjection.put(Provider.MsgInfoColumns.IS_COMMING, Provider.MsgInfoColumns.IS_COMMING);
		mMsgInfoProjection.put(Provider.MsgInfoColumns.IS_READ, Provider.MsgInfoColumns.IS_READ);
		mMsgInfoProjection.put(Provider.MsgInfoColumns.CREATIO_NDATE, Provider.MsgInfoColumns.CREATIO_NDATE);
		mMsgInfoProjection.put(Provider.MsgInfoColumns.MSG_TYPE, Provider.MsgInfoColumns.MSG_TYPE);
		mMsgInfoProjection.put(Provider.MsgInfoColumns.SEND_STATE, Provider.MsgInfoColumns.SEND_STATE);
		
		mMsgPartProjection = new HashMap<>();
		mMsgPartProjection.put(Provider.MsgPartColumns._ID, Provider.MsgPartColumns._ID);
		mMsgPartProjection.put(Provider.MsgPartColumns.MSG_ID, Provider.MsgPartColumns.MSG_ID);
		mMsgPartProjection.put(Provider.MsgPartColumns.FILE_NAME, Provider.MsgPartColumns.FILE_NAME);
		mMsgPartProjection.put(Provider.MsgPartColumns.FILE_PATH, Provider.MsgPartColumns.FILE_PATH);
		mMsgPartProjection.put(Provider.MsgPartColumns.CREATION_DATE, Provider.MsgPartColumns.CREATION_DATE);
		mMsgPartProjection.put(Provider.MsgPartColumns.MIME_TYPE, Provider.MsgPartColumns.MIME_TYPE);
		mMsgPartProjection.put(Provider.MsgPartColumns.SIZE, Provider.MsgPartColumns.SIZE);
		mMsgPartProjection.put(Provider.MsgPartColumns.FILE_TOKEN, Provider.MsgPartColumns.FILE_TOKEN);
		mMsgPartProjection.put(Provider.MsgPartColumns.DESC, Provider.MsgPartColumns.DESC);
		mMsgPartProjection.put(Provider.MsgPartColumns.FILE_THUMB_PATH, Provider.MsgPartColumns.FILE_THUMB_PATH);
		mMsgPartProjection.put(Provider.MsgPartColumns.DOWNLOADED, Provider.MsgPartColumns.DOWNLOADED);
		
		mMsgThreadProjection = new HashMap<>();
		mMsgThreadProjection.put(Provider.MsgThreadColumns._ID, Provider.MsgThreadColumns._ID);
		mMsgThreadProjection.put(Provider.MsgThreadColumns.MSG_THREAD_NAME, Provider.MsgThreadColumns.MSG_THREAD_NAME);
		mMsgThreadProjection.put(Provider.MsgThreadColumns.UNREAD_COUNT, Provider.MsgThreadColumns.UNREAD_COUNT);
		mMsgThreadProjection.put(Provider.MsgThreadColumns.MODIFY_DATE, Provider.MsgThreadColumns.MODIFY_DATE);
		mMsgThreadProjection.put(Provider.MsgThreadColumns.SNIPPET_ID, Provider.MsgThreadColumns.SNIPPET_ID);
		mMsgThreadProjection.put(Provider.MsgThreadColumns.SNIPPET_CONTENT, Provider.MsgThreadColumns.SNIPPET_CONTENT);
		mMsgThreadProjection.put(Provider.MsgThreadColumns.MEMBER_IDS, Provider.MsgThreadColumns.MEMBER_IDS);
		mMsgThreadProjection.put(Provider.MsgThreadColumns.IS_TOP, Provider.MsgThreadColumns.IS_TOP);
	}

	@Override
	public boolean onCreate() {
		mDBHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		String orderBy = null;
		switch (mUriMatcher.match(uri)) {
		case MSG_INFO_ID:
			qb.appendWhere(Provider.MsgInfoColumns._ID + " = " + uri.getLastPathSegment());
		case MSG_INFOS:	//查询所有的聊天消息
			qb.setTables(Provider.MsgInfoColumns.TABLE_NAME);
			qb.setProjectionMap(mMsgInfoProjection);
			if (!TextUtils.isEmpty(sortOrder)) {
				orderBy = sortOrder;
			} else {
				orderBy = Provider.MsgInfoColumns.DEFAULT_SORT_ORDER;
			}
			break;
		case MSG_PART_ID:
			qb.appendWhere(Provider.MsgPartColumns.MSG_ID + " = " + uri.getLastPathSegment());
		case MSG_PARTS:	//查询消息的附件
			qb.setTables(Provider.MsgPartColumns.TABLE_NAME);
			qb.setProjectionMap(mMsgPartProjection);
			if (!TextUtils.isEmpty(sortOrder)) {
				orderBy = sortOrder;
			} else {
				orderBy = Provider.MsgPartColumns.DEFAULT_SORT_ORDER;
			}
			break;
		case MSG_THREAD_ID:
			qb.appendWhere(Provider.MsgThreadColumns._ID + " = " + uri.getLastPathSegment());
		case MSG_THREADS:	//查询会话列表
			qb.setTables(Provider.MsgThreadColumns.TABLE_NAME);
			if (!TextUtils.isEmpty(sortOrder)) {
				orderBy = sortOrder;
			} else {
				orderBy = Provider.MsgThreadColumns.DEFAULT_SORT_ORDER;
			}
			qb.setProjectionMap(mMsgThreadProjection);
			break;
		default:
			break;
		}
		SQLiteDatabase db = mDBHelper.getReadableDatabase();
		Cursor cursor = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	@Override
	public String getType(Uri uri) {
		switch (mUriMatcher.match(uri)) {
		case MSG_INFOS:
		case MSG_PARTS:
		case MSG_THREADS:
			return Provider.CONTENT_TYPE;
		case MSG_INFO_ID:
		case MSG_PART_ID:
		case MSG_THREAD_ID:
			return Provider.CONTENT_ITEM_TYPE;

		default:
			break;
		}
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		ContentValues cv = null;
		if (values != null) {
			cv = new ContentValues(values);
		} else {
			cv = new ContentValues();
		}
		String tableName = "";
		String nullColumn = "";
		switch (mUriMatcher.match(uri)) {
		case MSG_INFOS:
			tableName = Provider.MsgInfoColumns.TABLE_NAME;
			nullColumn = Provider.MsgInfoColumns.THREAD_ID;
			if (!cv.containsKey(Provider.MsgInfoColumns.THREAD_ID)) {
				cv.put(Provider.MsgInfoColumns.THREAD_ID, 0);
			}
			if (!cv.containsKey(Provider.MsgInfoColumns.FROM_USER)) {
				cv.put(Provider.MsgInfoColumns.FROM_USER, "");
			}
			if (!cv.containsKey(Provider.MsgInfoColumns.TO_USER)) {
				cv.put(Provider.MsgInfoColumns.TO_USER, "");
			}
			break;
		case MSG_PARTS:
			tableName = Provider.MsgPartColumns.TABLE_NAME;
			nullColumn = Provider.MsgPartColumns.MSG_ID;
			if (!cv.containsKey(Provider.MsgPartColumns.MSG_ID)) {
				cv.put(Provider.MsgPartColumns.MSG_ID, 0);
			}
			if (!cv.containsKey(Provider.MsgPartColumns.FILE_NAME)) {
				cv.put(Provider.MsgPartColumns.FILE_NAME, 0);
			}
			if (!cv.containsKey(Provider.MsgPartColumns.FILE_PATH)) {
				cv.put(Provider.MsgPartColumns.FILE_PATH, 0);
			}
			break;
		case MSG_THREADS:
			tableName = Provider.MsgThreadColumns.TABLE_NAME;
			nullColumn = Provider.MsgThreadColumns.MSG_THREAD_NAME;
			if (!cv.containsKey(Provider.MsgThreadColumns.MSG_THREAD_NAME)) {
				cv.put(Provider.MsgThreadColumns.MSG_THREAD_NAME, "");
			}
			break;
		default:
			break;
		}
		SQLiteDatabase db = mDBHelper.getWritableDatabase();
//		db.beginTransaction();
		try {
			long rowId = db.insert(tableName, nullColumn, cv);
			if (rowId > 0) {
				Uri noteUri = ContentUris.withAppendedId(uri, rowId);
				getContext().getContentResolver().notifyChange(noteUri, null);
//				db.setTransactionSuccessful();
				return noteUri;
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(e.getMessage());
		}/* finally {
			db.endTransaction();
		}*/
		return null;
	}
	
	/**
	 * 批量添加数据，提供事务支持
	 */
	@Override
	public int bulkInsert(Uri uri, ContentValues[] values) {
		int numValues = values.length;
		SQLiteDatabase db = null;
        try {
        	db = mDBHelper.getWritableDatabase();
        	db.beginTransaction();
			for (int i = 0; i < numValues; i++) {
			    insert(uri, values[i]);
			}
			db.setTransactionSuccessful();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}
        return numValues;
	}
	
	@Override
	public ContentProviderResult[] applyBatch(
			ArrayList<ContentProviderOperation> operations)
			throws OperationApplicationException {
		SQLiteDatabase db = mDBHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			ContentProviderResult[] results = super.applyBatch(operations);
			db.setTransactionSuccessful();
			return results;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}
		return null;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = mDBHelper.getWritableDatabase();
		int count = 0;
//		db.beginTransaction();
		try {
			switch (mUriMatcher.match(uri)) {
			case MSG_INFOS:
				count = db.delete(Provider.MsgInfoColumns.TABLE_NAME, selection, selectionArgs);
				break;
			case MSG_INFO_ID:
				count = db.delete(Provider.MsgInfoColumns.TABLE_NAME, (Provider.MsgInfoColumns._ID + " = " + uri.getLastPathSegment() + (TextUtils.isEmpty(selection) ? "" : " AND (" + selection + ")")), selectionArgs);
				break;
			case MSG_PARTS:
				count = db.delete(Provider.MsgPartColumns.TABLE_NAME, selection, selectionArgs);
				break;
			case MSG_PART_ID:
				count = db.delete(Provider.MsgPartColumns.TABLE_NAME, (Provider.MsgPartColumns.MSG_ID + " = " + uri.getLastPathSegment() + (TextUtils.isEmpty(selection) ? "" : " AND (" + selection + ")")), selectionArgs);
				break;
			case MSG_THREADS:
				count = db.delete(Provider.MsgThreadColumns.TABLE_NAME, selection, selectionArgs);
				break;
			case MSG_THREAD_ID:
				count = db.delete(Provider.MsgThreadColumns.TABLE_NAME, (Provider.MsgThreadColumns._ID + " = " + uri.getLastPathSegment() + (TextUtils.isEmpty(selection) ? "" : " AND (" + selection + ")")), selectionArgs);
				break;
			default:
				break;
			}
			getContext().getContentResolver().notifyChange(uri, null);
//			db.setTransactionSuccessful();
		} catch (Exception e) {
			e.printStackTrace();
		}/* finally {
			db.endTransaction();
		}*/
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		SQLiteDatabase db = mDBHelper.getWritableDatabase();
		int count = 0;
//		db.beginTransaction();
		try {
			switch (mUriMatcher.match(uri)) {
			case MSG_INFOS:
				count = db.update(Provider.MsgInfoColumns.TABLE_NAME, values, selection, selectionArgs);
				break;
			case MSG_INFO_ID:
				count = db.update(Provider.MsgInfoColumns.TABLE_NAME, values, (Provider.MsgInfoColumns._ID + " = " + uri.getLastPathSegment() + (TextUtils.isEmpty(selection) ? "" : " AND (" + selection + ")")), selectionArgs);
				break;
			case MSG_PARTS:
				count = db.update(Provider.MsgPartColumns.TABLE_NAME, values, selection, selectionArgs);
				break;
			case MSG_PART_ID:
				count = db.update(Provider.MsgPartColumns.TABLE_NAME, values, (Provider.MsgPartColumns.MSG_ID + " = " + uri.getLastPathSegment() + (TextUtils.isEmpty(selection) ? "" : " AND (" + selection + ")")), selectionArgs);
				break;
			case MSG_THREADS:
				count = db.update(Provider.MsgThreadColumns.TABLE_NAME, values, selection, selectionArgs);
				break;
			case MSG_THREAD_ID:
				count = db.update(Provider.MsgThreadColumns.TABLE_NAME, values, (Provider.MsgThreadColumns._ID + " = " + uri.getLastPathSegment() + (TextUtils.isEmpty(selection) ? "" : " AND (" + selection + ")")), selectionArgs);
				break;
			default:
				break;
			}
			getContext().getContentResolver().notifyChange(uri, null);
//			db.setTransactionSuccessful();
		} catch (Exception e) {
			e.printStackTrace();
		}/* finally {
			db.endTransaction();
		}*/
		return count;
	}
	
}
