package net.ibaixin.chat.manager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.model.GeoInfo;
import net.ibaixin.chat.util.Log;

/**
 * 加载行政区划数据的manager
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年8月31日 下午2:51:41
 */
public class GeoInfoManager {
	private Context mContext;
	private SQLiteDatabase db;
	
	private static final String TABLE_COUNTRY = "country";
	private static final String TABLE_PROVINCE = "province";
	private static final String TABLE_CITY = "city";
	
	public GeoInfoManager() {
		mContext = ChatApplication.getInstance();
		db = openDistrictDatabase(mContext);
	}
	
	/**
	 * 关闭数据库
	 * @update 2015年9月2日 下午5:26:25
	 */
	public void closeDb() {
		if (db != null && db.isOpen()) {
			db.close();
		}
	}
	
	/**
	 * 获取所有的国家
	 * @param parentGeo 上层geo
	 * @return 国家信息的列表
	 * @update 2015年8月31日 下午2:55:13
	 */
	public List<GeoInfo> getCountries(GeoInfo parentGeo) {
		List<GeoInfo> geoInfos = null;
		if (db != null) {
			try {
				Cursor cursor = db.query(TABLE_COUNTRY, new String[] {"countryid", "country"}, null, null, null, null, null);
				if (cursor != null) {
					geoInfos = new ArrayList<>();
					while (cursor.moveToNext()) {
						GeoInfo info = new GeoInfo();
						int countryid = cursor.getInt(0);
						String country = cursor.getString(1);
						info.setCountryId(countryid);
						info.setCountry(country);
						if (countryid == parentGeo.getCountryId()) {	//默认的国家排在第一位
							info.setChecked(true);
							info.setViewType(GeoInfo.VIEW_TYPE_CHECK);
							geoInfos.add(0, info);
						} else {
							info.setChecked(false);
							info.setViewType(GeoInfo.VIEW_TYPE_ITEM);
							geoInfos.add(info);
						}
						if (countryid == 86) {	//这里固定写的，86是中国编码，目前没有录入国外的省份
							info.setHasChildren(true);
						} else {
							info.setHasChildren(false);
						}
					}
					cursor.close();
				}
			} catch (Exception e) {
				Log.e(e.getMessage());
			}
		} else {
			Log.w("---getCountries---district.db----can not read---geoInfos----" + geoInfos);
		}
		return geoInfos;
	}
	
	/**
	 * 根据国家获取对应的省份，目前只能获取中国的省份
	 * @param geoInfo
	 * @return
	 * @update 2015年8月31日 下午8:36:17
	 */
	public List<GeoInfo> getProvinces(GeoInfo parentGeo) {
		List<GeoInfo> geoInfos = null;
		if (parentGeo == null) {
			return null;
		}
		if (db != null) {
			try {
				Cursor cursor = db.query(TABLE_PROVINCE, new String[] {"provinceid", "province"}, "countryid = ?", new String[] {String.valueOf(parentGeo.getCountryId())}, null, null, null);
				if (cursor != null) {
					geoInfos = new ArrayList<>();
					while (cursor.moveToNext()) {
						GeoInfo info = parentGeo.clone();
						int provinceid = cursor.getInt(0);
						String province = cursor.getString(1);
						info.setProvinceId(provinceid);
						info.setProvince(province);
						info.setHasChildren(true);
						info.setCity(null);
						info.setCityId(0);
						if (provinceid == parentGeo.getProvinceId()) {	//已选择的排在第一位
							info.setChecked(true);
							info.setViewType(GeoInfo.VIEW_TYPE_CHECK);
							geoInfos.add(0, info);
						} else {
							info.setChecked(false);
							info.setViewType(GeoInfo.VIEW_TYPE_ITEM);
							geoInfos.add(info);
						}
					}
					cursor.close();
				}
			} catch (Exception e) {
				Log.e(e.getMessage());
			}
		} else {
			Log.w("---getProvinces---district.db----can not read---geoInfos----" + geoInfos);
		}
		return geoInfos;
	}
	
	/**
	 * 根据省份获取对应的市，目前只能获取中国的市
	 * @param geoInfo
	 * @return
	 * @update 2015年8月31日 下午8:36:17
	 */
	public List<GeoInfo> getCities(GeoInfo parentGeo) {
		List<GeoInfo> geoInfos = null;
		if (parentGeo == null) {
			return null;
		}
		if (db != null) {
			try {
				Cursor cursor = db.query(TABLE_CITY, new String[] {"cityid", "city"}, "provinceid = ?", new String[] {String.valueOf(parentGeo.getProvinceId())}, null, null, null);
				if (cursor != null) {
					geoInfos = new ArrayList<>();
					while (cursor.moveToNext()) {
						GeoInfo info = parentGeo.clone();
						int cityid = cursor.getInt(0);
						String city = cursor.getString(1);
						info.setCityId(cityid);
						info.setCity(city);
						info.setHasChildren(false);
						if (cityid == parentGeo.getCityId()) {	//已选择的排在第一位
							info.setChecked(true);
							info.setViewType(GeoInfo.VIEW_TYPE_CHECK);
							geoInfos.add(0, info);
						} else {
							info.setChecked(false);
							info.setViewType(GeoInfo.VIEW_TYPE_ITEM);
							geoInfos.add(info);
						}
					}
					cursor.close();
				}
			} catch (Exception e) {
				Log.e(e.getMessage());
			}
		} else {
			Log.w("---getCities---district.db----can not read---geoInfos----" + geoInfos);
		}
		return geoInfos;
	}
	
	/**
	 * 根据行政区划名称来获取对应的名称编码，比如，北京对应的编码为110000
	 * @param matching 是否模糊匹配，名称是否模糊匹配，模糊匹配只截取前面一段文字来查询
	 * @param geoInfo
	 * @return
	 * @update 2015年9月2日 上午11:33:15
	 */
	public GeoInfo geoReCoder(GeoInfo geoInfo, boolean matching) {
		if (geoInfo == null) {
			return null;
		}
		if (db != null) {
			String country = geoInfo.getCountry();
			String province = geoInfo.getProvince();
			String city = geoInfo.getCity();
			if (country != null) {
				Cursor countryCursor = db.query(TABLE_COUNTRY, new String[] {"countryid"}, "country = ?", new String[] {country}, null, null, null);
				if (countryCursor != null && countryCursor.moveToFirst()) {
					int countryid = countryCursor.getInt(0);
					geoInfo.setCountryId(countryid);
				}
				if (countryCursor != null) {
					countryCursor.close();
				}
			}
			String provinceSelection = null;
			String[] provinceSelectionArgs = null;
			String citySelection = null;
			String[] citySelectionArgs = null;
			if (matching) {
				//国家不进行模糊匹配
				if (province != null) {
					if (province.length() > 3) {
						province = province.substring(0, 3);
					} else if (province.length() > 2) {
						province = province.substring(0, 2);
					}
					provinceSelectionArgs = new String[] {province + "%"};
				}
				
				provinceSelection = "province like ?";
				citySelection = "city like ?";
				
				if (city != null) {
					if (city.length() > 4) {
						city = city.substring(0, 4);
					} else if (city.length() > 3) {
						city = city.substring(0, 3);
					} else if (city.length() > 2) {
						city = city.substring(0, 2);
					}
					citySelectionArgs = new String[] {city + "%"};
				}
			} else {
				provinceSelection = "province = ?";
				citySelection = "city = ?";
				provinceSelectionArgs = new String[] {province};
				citySelectionArgs = new String[] {city};
			}
			if (province != null) {
				Cursor provinceCursor = db.query(TABLE_PROVINCE, new String[] {"provinceid", "province"}, provinceSelection, provinceSelectionArgs, null, null, null);
				if (provinceCursor != null && provinceCursor.moveToFirst()) {
					int provinceid = provinceCursor.getInt(0);
					province = provinceCursor.getString(1);
					geoInfo.setProvince(province);
					geoInfo.setProvinceId(provinceid);
				}
				if (provinceCursor != null) {
					provinceCursor.close();
				}
			}
			
			if (city != null) {
				Cursor cityCursor = db.query(TABLE_CITY, new String[] {"cityid", "city"}, citySelection, citySelectionArgs, null, null, null);
				if (cityCursor != null && cityCursor.moveToFirst()) {
					int cityid = cityCursor.getInt(0);
					city = cityCursor.getString(1);
					
					geoInfo.setCity(city);
					geoInfo.setCityId(cityid);
				}
				if (cityCursor != null) {
					cityCursor.close();
				}
			}
			return geoInfo;
		} else {
			return null;
		}
	}
	
	/**
	 * 打开行政区划的数据库
	 * @param context
	 * @return
	 * @update 2015年8月31日 下午4:12:45
	 */
	private SQLiteDatabase openDistrictDatabase(Context context) {
		try {
			String dbFileName = "district.db";
			String dirname = "db";
			//读取asserts目录下的district.db文件
			File dbDir = getDiskFileDir(context, dirname);
			File dbFile = new File(dbDir, dbFileName);
			String dbPath = dbFile.getAbsolutePath();
			SQLiteDatabase db = null;
			if (!dbFile.exists()) {	//需要拷贝，拷贝后再读取
				if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState())) {	//sd卡只读，则拷贝到手机内部存储目录中
					dbDir = getDiskInnerFileDir(mContext, dirname);
					dbFile = new File(dbDir, dbFileName);
					dbPath = dbFile.getAbsolutePath();
				}
				copyDbFile(context, dbFileName, dbPath);
				db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);
			} else {
				//检查原始的db版本与现在的是否一致，不一致就替换
				InputStream is = context.getAssets().open("app.properties");
				Properties props = new Properties();
				props.load(new InputStreamReader(is, Charset.forName("UTF-8")));
				int dbOldVersion = Integer.parseInt(props.getProperty("district.version", String.valueOf(-1)));
				db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);
				int dbVersion = db.getVersion();
				if (dbVersion != dbOldVersion) {	//需要替换
					//关闭该数据库
					db.close();
					//删除文件
					dbFile.delete();
					if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState())) {	//sd卡只读，则拷贝到手机内部存储目录中
						dbDir = getDiskInnerFileDir(mContext, dirname);
						dbFile = new File(dbDir, dbFileName);
						dbPath = dbFile.getAbsolutePath();
					}
					copyDbFile(context, dbFileName, dbPath);
					db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);
				}
			}
			return db;
		} catch (UnsupportedCharsetException | IOException e) {
			Log.e(e.getMessage());
		}
		return null;
	}
	
	/**
	 * 复制文件
	 * @param context
	 * @param dbFileName 文件的名称，如:xxx.db
	 * @param destPath 拷贝到的全路径，包含文件名
	 * @update 2015年9月1日 下午4:24:39
	 */
	private void copyDbFile(Context context, String dbFileName, String destPath) {
		InputStream is = null;
		FileOutputStream fos = null;
		try {
			is = context.getAssets().open(dbFileName);
			fos = new FileOutputStream(destPath);
			byte[] buf = new byte[8192];
			int count = -1;
			while ((count = is.read(buf)) > 0) {
				fos.write(buf, 0, count);
			}
			fos.flush();
			
		} catch (IOException e) {
			Log.e(e.getMessage());
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					Log.e(e.getMessage());
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					Log.e(e.getMessage());
				}
			}
		}
	}
	
	/**
	 * 根据文件夹名获取对应的磁盘存储目录，如/sdcard/Android/data/<application package>/files/这个目录下，
	 * 如果sd卡不可用，则使用内部存储，如 /data/data/<application package>/files/这个目录
	 * @param context
	 * @param dirname
	 * @return
	 * @update 2015年8月31日 下午3:39:28
	 */
	public File getDiskFileDir(Context context, String dirname) {
		File dir = null;
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {	//sd卡是否可用
			dir = context.getExternalFilesDir(dirname);
		} else {
			dir = context.getFileStreamPath(dirname);
		}
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}
	
	/**
	 * 获取内部的文件存储目录，如 /data/data/<application package>/files/这个目录
	 * @param context
	 * @param dirname
	 * @return
	 * @update 2015年8月31日 下午4:00:32
	 */
	public File getDiskInnerFileDir(Context context, String dirname) {
		File dir = context.getFileStreamPath(dirname);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}
}
