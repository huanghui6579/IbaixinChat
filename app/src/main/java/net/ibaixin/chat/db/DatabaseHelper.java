package net.ibaixin.chat.db;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import net.ibaixin.chat.provider.Provider;
import net.ibaixin.chat.util.Log;

/**
 * 数据库创建
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年10月13日 上午11:26:25
 */
public class DatabaseHelper extends SQLiteOpenHelper {
//	private static final String DB_NAME = "ibaixin_chat.db";
	private static final String DB_NAME = "ibaixin_personal.db";
	private static final int DB_VERSION = 11;

	public DatabaseHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		
		//创建个人信息表
		db.execSQL("CREATE TABLE IF NOT EXISTS " + Provider.PersonalColums.TABLE_NAME + " ("
				+ Provider.PersonalColums._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ Provider.PersonalColums.USERNAME + " TEXT UNIQUE NOT NULL, "
				+ Provider.PersonalColums.PASSWORD + " TEXT NOT NULL, "
				+ Provider.PersonalColums.NICKNAME + " TEXT, "
				+ Provider.PersonalColums.REALNAME + " TEXT, "
				+ Provider.PersonalColums.EMAIL + " TEXT, "
				+ Provider.PersonalColums.PHONE + " TEXT, "
				+ Provider.PersonalColums.RESOURCE + " TEXT, "
				+ Provider.PersonalColums.STATUS + " TEXT, "
				+ Provider.PersonalColums.MODE + " TEXT, "
				+ Provider.PersonalColums.COUNTRY + " TEXT, "
				+ Provider.PersonalColums.COUNTRY_ID + " INTEGER, "
				+ Provider.PersonalColums.PROVINCE + " TEXT, "
				+ Provider.PersonalColums.PROVINCE_ID + " INTEGER, "
				+ Provider.PersonalColums.STREET + " TEXT, "
				+ Provider.PersonalColums.CITY + " TEXT, "
				+ Provider.PersonalColums.CITY_ID + " INTEGER, "
				+ Provider.PersonalColums.ZIPCODE + " TEXT, "
				+ Provider.PersonalColums.ICONPATH + " TEXT, "
				+ Provider.PersonalColums.THUMBPATH + " TEXT, "
				+ Provider.PersonalColums.MIMETYPE + " TEXT, "
				+ Provider.PersonalColums.ICONHASH + " TEXT, "
				+ Provider.PersonalColums.SEX + " INTEGER DEFAULT 0, "
				+ Provider.PersonalColums.DESC + " TEXT);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		switch (newVersion) {
		case 11:	//添加countryId, provinceId, cityId字段
			try {
				db.execSQL("alter table t_personal add column countryId integer;");
				db.execSQL("alter table t_personal add column provinceId integer;");
				db.execSQL("alter table t_personal add column cityId integer;");
			} catch (SQLException e) {
				db.execSQL("DROP TABLE IF EXISTS " + Provider.PersonalColums.TABLE_NAME);  
				onCreate(db);
				Log.e(e.getMessage());
			}
			break;

		default:
			db.execSQL("DROP TABLE IF EXISTS " + Provider.PersonalColums.TABLE_NAME);  
			onCreate(db);
			break;
		}
	}

}
