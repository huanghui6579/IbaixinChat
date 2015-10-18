package net.ibaixin.chat.provider;

import java.util.HashMap;
import java.util.Map;

import net.ibaixin.chat.db.DatabaseHelper;
import net.ibaixin.chat.util.Log;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

/**
 * 用户数据库provider
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年10月13日 上午11:16:06
 */
public class PersonalProvider extends ContentProvider {
	
	private static final int PERSONALS = 1;
	private static final int PERSONAL_ID = 2;
	
	private static Map<String, String> mPersonalProjectionMap = null;
	
	private static final UriMatcher mUriMatcher;
	
	private DatabaseHelper mDBHelper;
	
	static {
		
		mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		mUriMatcher.addURI(Provider.AUTHORITY_PERSONAL, "personals", PERSONALS);
		mUriMatcher.addURI(Provider.AUTHORITY_PERSONAL, "personals/#", PERSONAL_ID);
		
		mPersonalProjectionMap = new HashMap<String, String>();
		mPersonalProjectionMap.put(Provider.PersonalColums._ID, Provider.PersonalColums._ID);
		mPersonalProjectionMap.put(Provider.PersonalColums.USERNAME, Provider.PersonalColums.USERNAME);
		mPersonalProjectionMap.put(Provider.PersonalColums.PASSWORD, Provider.PersonalColums.PASSWORD);
		mPersonalProjectionMap.put(Provider.PersonalColums.NICKNAME, Provider.PersonalColums.NICKNAME);
		mPersonalProjectionMap.put(Provider.PersonalColums.REALNAME, Provider.PersonalColums.REALNAME);
		mPersonalProjectionMap.put(Provider.PersonalColums.EMAIL, Provider.PersonalColums.EMAIL);
		mPersonalProjectionMap.put(Provider.PersonalColums.PHONE, Provider.PersonalColums.PHONE);
		mPersonalProjectionMap.put(Provider.PersonalColums.STATUS, Provider.PersonalColums.STATUS);
		mPersonalProjectionMap.put(Provider.PersonalColums.MODE, Provider.PersonalColums.MODE);
		mPersonalProjectionMap.put(Provider.PersonalColums.RESOURCE, Provider.PersonalColums.RESOURCE);
		mPersonalProjectionMap.put(Provider.PersonalColums.STREET, Provider.PersonalColums.STREET);
		mPersonalProjectionMap.put(Provider.PersonalColums.CITY, Provider.PersonalColums.CITY);
		mPersonalProjectionMap.put(Provider.PersonalColums.CITY_ID, Provider.PersonalColums.CITY_ID);
		mPersonalProjectionMap.put(Provider.PersonalColums.PROVINCE, Provider.PersonalColums.PROVINCE);
		mPersonalProjectionMap.put(Provider.PersonalColums.PROVINCE_ID, Provider.PersonalColums.PROVINCE_ID);
		mPersonalProjectionMap.put(Provider.PersonalColums.COUNTRY, Provider.PersonalColums.COUNTRY);
		mPersonalProjectionMap.put(Provider.PersonalColums.COUNTRY_ID, Provider.PersonalColums.COUNTRY_ID);
		mPersonalProjectionMap.put(Provider.PersonalColums.ZIPCODE, Provider.PersonalColums.ZIPCODE);
		mPersonalProjectionMap.put(Provider.PersonalColums.THUMBPATH, Provider.PersonalColums.THUMBPATH);
		mPersonalProjectionMap.put(Provider.PersonalColums.ICONPATH, Provider.PersonalColums.ICONPATH);
		mPersonalProjectionMap.put(Provider.PersonalColums.MIMETYPE, Provider.PersonalColums.MIMETYPE);
		mPersonalProjectionMap.put(Provider.PersonalColums.ICONHASH, Provider.PersonalColums.ICONHASH);
		mPersonalProjectionMap.put(Provider.PersonalColums.SEX, Provider.PersonalColums.SEX);
		mPersonalProjectionMap.put(Provider.PersonalColums.DESC, Provider.PersonalColums.DESC);
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
		qb.setTables(Provider.PersonalColums.TABLE_NAME);
		qb.setProjectionMap(mPersonalProjectionMap);
		switch (mUriMatcher.match(uri)) {
		case PERSONAL_ID:	//查询个人信息
			qb.appendWhere(Provider.PersonalColums.USERNAME + " = " + uri.getLastPathSegment());
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
		case PERSONALS:
			return Provider.CONTENT_TYPE;
		case PERSONAL_ID:
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
		case PERSONALS:
			tableName = Provider.PersonalColums.TABLE_NAME;
			nullColumn = Provider.PersonalColums.USERNAME;
			
			if (!cv.containsKey(Provider.PersonalColums.USERNAME)) {
				cv.put(Provider.PersonalColums.USERNAME, "");
			}
			
			if (!cv.containsKey(Provider.PersonalColums.PASSWORD)) {
				cv.put(Provider.PersonalColums.PASSWORD, "");
			}
			break;
		default:
			break;
		}
		SQLiteDatabase db = mDBHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			long rowId = db.insert(tableName, nullColumn, cv);
			if (rowId > 0) {
				Uri noteUri = ContentUris.withAppendedId(uri, rowId);
				getContext().getContentResolver().notifyChange(noteUri, null);
				db.setTransactionSuccessful();
				return noteUri;
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(e.getMessage());
		} finally {
			db.endTransaction();
		}
		return null;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = mDBHelper.getWritableDatabase();
		int count = 0;
		try {
			db.beginTransaction();
			switch (mUriMatcher.match(uri)) {
			case PERSONALS:
				count = db.delete(Provider.PersonalColums.TABLE_NAME, selection, selectionArgs);
				break;
			case PERSONAL_ID:
				count = db.delete(Provider.PersonalColums.TABLE_NAME, Provider.PersonalColums._ID + " = " + uri.getLastPathSegment() + (TextUtils.isEmpty(selection) ? "" : " and (" + selection + ")"), selectionArgs);
				break;
			default:
				break;
			}
			getContext().getContentResolver().notifyChange(uri, null);
			db.setTransactionSuccessful();
		} catch (Exception e) {
			e.printStackTrace();
			Log.d(e.getMessage());
		} finally {
			db.endTransaction();
		}
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		SQLiteDatabase db = mDBHelper.getWritableDatabase();
		int count = 0;
		db.beginTransaction();
		try {
			switch (mUriMatcher.match(uri)) {
			case PERSONALS:
				count = db.update(Provider.PersonalColums.TABLE_NAME, values, selection, selectionArgs);
				break;
			case PERSONAL_ID:
				count = db.update(Provider.PersonalColums.TABLE_NAME, values, Provider.PersonalColums._ID + " = " + uri.getLastPathSegment() + (TextUtils.isEmpty(selection) ? "" : " and (" + selection + ")"), selectionArgs);
				break;
			default:
				break;
			}
			getContext().getContentResolver().notifyChange(uri, null);
			db.setTransactionSuccessful();
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(e.getMessage());
		} finally {
			db.endTransaction();
		}
		return count;
	}

}
