package net.ibaixin.chat.manager;

import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.model.Personal;
import net.ibaixin.chat.provider.Provider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/**
 * 操作本地数据库个人信息服务层
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年3月9日 下午8:06:14
 */
public class PersonalManage {
	private static PersonalManage instance = null;
	
	private Context mContext;
	
	private PersonalManage() {
		mContext = ChatApplication.getInstance();
	}
	
	/**
	 * 获取单例
	 * @update 2015年3月9日 下午8:09:47
	 * @return
	 */
	public static PersonalManage getInstance() {
		if (instance == null) {
			synchronized (PersonalManage.class) {
				if (instance == null) {
					instance = new PersonalManage();
				}
			}
		}
		return instance;
	}
	
	/**
	 * 判断有无该用户的账户记录
	 * @update 2015年3月9日 下午8:18:32
	 * @param account 用户的登录账号，格式为xxx
	 * @return
	 */
	public boolean hasAccount(String account) {
		boolean flag = false;
		Cursor cursor = mContext.getContentResolver().query(Provider.PersonalColums.CONTENT_URI, new String[] {Provider.PersonalColums._ID}, Provider.PersonalColums.USERNAME + " = ?", new String[] {account}, null);
		if (cursor != null) {
			flag = cursor.getCount() > 0;
			cursor.close();
		}
		return flag;
	}
	
	/**
	 * 添加登录用户
	 * @update 2015年3月10日 上午8:50:58
	 * @param personal
	 * @return
	 */
	public Personal addPersonal(Personal personal) {
		ContentValues values = initPersonalContentVaules(personal);
		Uri uri = mContext.getContentResolver().insert(Provider.PersonalColums.CONTENT_URI, values);
		personal.setId(Integer.parseInt(uri.getLastPathSegment()));
		return personal;
	}
	
	/**
	 * 更新个人的头像信息
	 * @update 2015年3月13日 下午5:45:30
	 * @param personal
	 * @return
	 */
	public Personal updateHeadIcon(Personal personal) {
		ContentValues values = new ContentValues();
		values.put(Provider.PersonalColums.ICONHASH, personal.getIconHash());
		values.put(Provider.PersonalColums.THUMBPATH, personal.getThumbPath());
		values.put(Provider.PersonalColums.MIMETYPE, personal.getMimeType());
		values.put(Provider.PersonalColums.ICONPATH, personal.getIconPath());
		mContext.getContentResolver().update(Uri.withAppendedPath(Provider.PersonalColums.CONTENT_URI, String.valueOf(personal.getId())), values, null, null);
		return personal;
	}
	
	/**
	 * 更新个人的昵称
	 * @author tiger
	 * @update 2015年3月15日 下午10:02:21
	 * @param personal
	 * @return
	 */
	public Personal updateNickname(Personal personal) {
		ContentValues values = new ContentValues();
		values.put(Provider.PersonalColums.NICKNAME, personal.getNickname());
		mContext.getContentResolver().update(Uri.withAppendedPath(Provider.PersonalColums.CONTENT_URI, String.valueOf(personal.getId())), values, null, null);
		return personal;
	}
	
	/**
	 * 更新个人的个性签名
	 * @author tiger
	 * @update 2015年3月15日 下午10:02:21
	 * @param personal
	 * @return
	 */
	public Personal updateSignature(Personal personal) {
		ContentValues values = new ContentValues();
		values.put(Provider.PersonalColums.DESC, personal.getDesc());
		mContext.getContentResolver().update(Uri.withAppendedPath(Provider.PersonalColums.CONTENT_URI, String.valueOf(personal.getId())), values, null, null);
		return personal;
	}
	
	/**
	 * 更新个人的性别
	 * @param personal
	 * @return
	 * @update 2015年8月27日 上午10:28:48
	 */
	public Personal updateGender(Personal personal) {
		ContentValues values = new ContentValues();
		values.put(Provider.PersonalColums.SEX, personal.getSex());
		mContext.getContentResolver().update(Uri.withAppendedPath(Provider.PersonalColums.CONTENT_URI, String.valueOf(personal.getId())), values, null, null);
		return personal;
	}
	
	/**
	 * 更新个人的地理位置信息
	 * @param personal
	 * @return
	 * @update 2015年9月6日 下午3:13:09
	 */
	public Personal updateGeo(Personal personal) {
		ContentValues values = new ContentValues();
		values.put(Provider.PersonalColums.COUNTRY, personal.getCountry());
		values.put(Provider.PersonalColums.COUNTRY_ID, personal.getCountryId());
		values.put(Provider.PersonalColums.PROVINCE, personal.getProvince());
		values.put(Provider.PersonalColums.PROVINCE_ID, personal.getProvinceId());
		values.put(Provider.PersonalColums.CITY, personal.getCity());
		values.put(Provider.PersonalColums.CITY_ID, personal.getCityId());
		mContext.getContentResolver().update(Uri.withAppendedPath(Provider.PersonalColums.CONTENT_URI, String.valueOf(personal.getId())), values, null, null);
		return personal;
	}
	
	/**
	 * 获取个人本地数据库里的信息，一般用于首次登录同步数据的判断
	 * @update 2014年10月24日 下午7:21:42
	 * @param temp
	 * @return
	 */
	public Personal getLocalSelfInfoByUsername(Personal person) {
		Personal temp = null;
		Cursor cursor = mContext.getContentResolver().query(Provider.PersonalColums.CONTENT_URI, null, Provider.PersonalColums.USERNAME + " = ?", new String[] {person.getUsername()}, null);
		if (cursor != null && cursor.moveToFirst()) {
			temp = getPersonalFromCursor(person, cursor);
		}
		if (cursor != null) {
			cursor.close();
		}
		return temp;
	}
	
	/**
	 * 获取个人本地数据库里的信息，一般用于首次登录同步数据的判断
	 * @update 2014年10月24日 下午7:21:42
	 * @param temp
	 * @return
	 */
	public Personal getLocalSelfInfoById(Personal person) {
		Personal temp = null;
		Cursor cursor = mContext.getContentResolver().query(Provider.PersonalColums.CONTENT_URI, null, Provider.PersonalColums._ID + " = ?", new String[] {String.valueOf(person.getId())}, null);
		if (cursor != null && cursor.moveToFirst()) {
			temp = getPersonalFromCursor(person, cursor);
		}
		if (cursor != null) {
			cursor.close();
		}
		return temp;
	}
	
	/**
	 * 将cursor转换成personal
	 * @param cursor
	 * @return
	 * @update 2015年8月20日 下午6:33:41
	 */
	private Personal getPersonalFromCursor(Personal personal, Cursor cursor) {		
		personal.setId(cursor.getInt(cursor.getColumnIndex(Provider.PersonalColums._ID)));
		personal.setNickname(cursor.getString(cursor.getColumnIndex(Provider.PersonalColums.NICKNAME)));
		personal.setEmail(cursor.getString(cursor.getColumnIndex(Provider.PersonalColums.EMAIL)));
		personal.setPhone(cursor.getString(cursor.getColumnIndex(Provider.PersonalColums.PHONE)));
		personal.setCountry(cursor.getString(cursor.getColumnIndex(Provider.PersonalColums.COUNTRY)));
		personal.setCountryId(cursor.getInt(cursor.getColumnIndex(Provider.PersonalColums.COUNTRY_ID)));
		personal.setProvince(cursor.getString(cursor.getColumnIndex(Provider.PersonalColums.PROVINCE)));
		personal.setProvinceId(cursor.getInt(cursor.getColumnIndex(Provider.PersonalColums.PROVINCE_ID)));
		personal.setCity(cursor.getString(cursor.getColumnIndex(Provider.PersonalColums.CITY)));
		personal.setCityId(cursor.getInt(cursor.getColumnIndex(Provider.PersonalColums.CITY_ID)));
		personal.setStreet(cursor.getString(cursor.getColumnIndex(Provider.PersonalColums.STREET)));
		personal.setRealName(cursor.getString(cursor.getColumnIndex(Provider.PersonalColums.REALNAME)));
		personal.setZipCode(cursor.getString(cursor.getColumnIndex(Provider.PersonalColums.ZIPCODE)));
		personal.setIconPath(cursor.getString(cursor.getColumnIndex(Provider.PersonalColums.ICONPATH)));
		personal.setThumbPath(cursor.getString(cursor.getColumnIndex(Provider.PersonalColums.THUMBPATH)));
		personal.setMimeType(cursor.getString(cursor.getColumnIndex(Provider.PersonalColums.MIMETYPE)));
		personal.setIconHash(cursor.getString(cursor.getColumnIndex(Provider.PersonalColums.ICONHASH)));
		personal.setSex(cursor.getInt(cursor.getColumnIndex(Provider.PersonalColums.SEX)));
		personal.setDesc(cursor.getString(cursor.getColumnIndex(Provider.PersonalColums.DESC)));
		return personal;
	}
	

	/**
	 * 更新个人信息
	 * @update 2014年10月24日 下午7:36:18
	 * @param person
	 */
	public void updatePersonInfo(Personal person) {
		ContentValues values = initPersonalContentVaules(person);
		mContext.getContentResolver().update(Uri.withAppendedPath(Provider.PersonalColums.CONTENT_URI, String.valueOf(person.getId())), values, null, null);
	}
	
	
	/**
	 * 更新个人的状态，一般用于个人的状态发生变化时调用，如刚登录、下载等等
	 * @update 2014年10月24日 下午7:38:41
	 * @param person
	 */
	public void updatePersonStatus(Personal person) {
		ContentValues values = new ContentValues();
		values.put(Provider.PersonalColums.STATUS, person.getStatus());
		values.put(Provider.PersonalColums.MODE, person.getMode());
		values.put(Provider.PersonalColums.RESOURCE, person.getResource());
		mContext.getContentResolver().update(Provider.PersonalColums.CONTENT_URI, values, Provider.PersonalColums.USERNAME + " = ?", new String[] {person.getUsername()});
	}
	
	/**
	 * 初始化PersonalContentVaules数据
	 * @update 2014年10月24日 下午7:13:50
	 * @param personal
	 * @return
	 */
	private ContentValues initPersonalContentVaules(Personal person) {
		ContentValues values = new ContentValues();
		values.put(Provider.PersonalColums.USERNAME, person.getUsername());
		values.put(Provider.PersonalColums.NICKNAME, person.getNickname());
		values.put(Provider.PersonalColums.PASSWORD, person.getPassword());
		values.put(Provider.PersonalColums.REALNAME, person.getRealName());
		values.put(Provider.PersonalColums.EMAIL, person.getEmail());
		values.put(Provider.PersonalColums.PHONE, person.getPhone());
		values.put(Provider.PersonalColums.MODE, person.getMode());
		values.put(Provider.PersonalColums.COUNTRY, person.getCountry());
		values.put(Provider.PersonalColums.COUNTRY_ID, person.getCountryId());
		values.put(Provider.PersonalColums.PROVINCE, person.getProvince());
		values.put(Provider.PersonalColums.PROVINCE_ID, person.getProvinceId());
		values.put(Provider.PersonalColums.RESOURCE, person.getResource());
		values.put(Provider.PersonalColums.STATUS, person.getStatus());
		values.put(Provider.PersonalColums.CITY, person.getCity());
		values.put(Provider.PersonalColums.CITY_ID, person.getCityId());
		values.put(Provider.PersonalColums.STREET, person.getStreet());
		values.put(Provider.PersonalColums.ZIPCODE, person.getZipCode());
		values.put(Provider.PersonalColums.ICONPATH, person.getIconPath());
		values.put(Provider.PersonalColums.THUMBPATH, person.getThumbPath());
		values.put(Provider.PersonalColums.MIMETYPE, person.getMimeType());
		values.put(Provider.PersonalColums.ICONHASH, person.getIconHash());
		values.put(Provider.PersonalColums.SEX, person.getSex());
		values.put(Provider.PersonalColums.DESC, person.getDesc());
		return values;
	}
	

	/**
	 * 初始化当前用户的个人信息，用户刚登录或者注册
	 * @param person
	 */
	public Personal saveOrUpdateCurrentUser(final Personal person) {
		Cursor cursor = mContext.getContentResolver().query(Provider.PersonalColums.CONTENT_URI, null, Provider.PersonalColums.USERNAME + " = ?", new String[] {person.getUsername()}, null);
		ContentValues values = initPersonalContentVaules(person);
		if (cursor != null && cursor.moveToFirst()) {	//有数据，直接赋值返回
			mContext.getContentResolver().update(Provider.PersonalColums.CONTENT_URI, values, Provider.PersonalColums.USERNAME + " = ?", new String[] {person.getUsername()});
		} else {	//没有数据，插入数据
			Uri uri = mContext.getContentResolver().insert(Provider.PersonalColums.CONTENT_URI, values);
			person.setId(Integer.parseInt(uri.getLastPathSegment()));
		}
		if (cursor != null) {
			cursor.close();
		}
		return person;
	}
}
