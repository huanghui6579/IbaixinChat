package net.ibaixin.chat.manager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;

import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.db.ChatDatabaseHelper;
import net.ibaixin.chat.model.MsgThread;
import net.ibaixin.chat.model.NewFriendInfo;
import net.ibaixin.chat.model.User;
import net.ibaixin.chat.model.UserVcard;
import net.ibaixin.chat.model.web.VcardDto;
import net.ibaixin.chat.provider.Provider;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.Observable;
import net.ibaixin.chat.util.Observer;
import net.ibaixin.chat.util.Observer.NotifyType;
import net.ibaixin.chat.util.SystemUtil;

import org.jivesoftware.smack.packet.Presence;
import org.jxmpp.util.XmppStringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 用户操作的业务逻辑层
 * @author coolpad
 *
 */
public class UserManager extends Observable<Observer> {
	private static UserManager instance = null;
	
	private Context mContext;
	
	private ChatDatabaseHelper mChatDBHelper;
	
	/**
	 * 以username为key，user实体为value的用户缓存
	 */
	private Map<String, User> mUserCache = new WeakHashMap<>();
	
	private UserManager() {
		mContext = ChatApplication.getInstance();
		ChatApplication app = (ChatApplication) mContext.getApplicationContext();
		mChatDBHelper = new ChatDatabaseHelper(mContext, app.getAccountDbDir());
	}
	
	/**
	 * 获取单例的实例
	 * @return
	 */
	public static UserManager getInstance() {
		if (instance == null) {
			synchronized (UserManager.class) {
				if (instance == null) {
					instance = new UserManager();
				}
			}
		}
		return instance;
	}
	
	/**
	 * 从本地数据库中获取当前用户的好友列表
	 * @return 好友列表
	 */
	public List<User> getFriends() {
		return getFriends(null);
	}
	
	/**
	 * 获取所有好友列表
	 * @param db 数据库
	 * @return 返回好友列表             
	 * 创建人：huanghui1
	 * 创建时间： 2015/11/11 10:31
	 * 修改人：huanghui1
	 * 修改时间：2015/11/11 10:31
	 * 修改备注：
	 * @version: 0.0.1
	 */
	public List<User> getFriends(SQLiteDatabase db) {
		List<User> users = null;
//		Cursor cursor = mContext.getContentResolver().query(Provider.UserColumns.CONTENT_URI, null, null, null, null);
		if (db == null) {
			db = mChatDBHelper.getReadableDatabase();
		}
		Cursor cursor = db.query(Provider.UserColumns.TABLE_NAME, Provider.UserColumns.DEFAULT_PROJECTION, null, null, null, null, Provider.UserColumns.DEFAULT_SORT_ORDER);
		if (cursor != null) {
			users = new ArrayList<User>();
			//清空缓存，重新加载
			mUserCache.clear();
			while(cursor.moveToNext()) {
				User user = new User();
				user.setId(cursor.getInt(cursor.getColumnIndex(Provider.UserColumns._ID)));
				user.setUsername(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.USERNAME)));
				user.setNickname(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.NICKNAME)));
				user.setMode(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.MODE)));
				user.setPhone(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.PHONE)));
				user.setResource(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.RESOURCE)));
				user.setEmail(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.EMAIL)));
				user.setFullPinyin(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.FULLPINYIN)));
				user.setShortPinyin(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.SHORTPINYIN)));
				user.setSortLetter(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.SORTLETTER)));
				user.setJID(user.initJID(user.getUsername()));
//				Cursor cardCursor = mContext.getContentResolver().query(Provider.UserVcardColumns.CONTENT_URI, new String[] {Provider.UserVcardColumns._ID, Provider.UserVcardColumns.NICKNAME, Provider.UserVcardColumns.ICONPATH, Provider.UserVcardColumns.ICONHASH}, Provider.UserVcardColumns.USERID + " = ?", new String[] {String.valueOf(user.getId())}, null);
				String[] projection = {Provider.UserVcardColumns._ID, Provider.UserVcardColumns.NICKNAME, Provider.UserVcardColumns.ICONPATH, Provider.UserVcardColumns.THUMBPATH, Provider.UserVcardColumns.ICONHASH};
				Cursor cardCursor = db.query(Provider.UserVcardColumns.TABLE_NAME, projection, Provider.UserVcardColumns.USERID + " = ?", new String[] {String.valueOf(user.getId())}, null, null, null);
				if (cardCursor != null && cardCursor.moveToFirst()) {	//查询该好友对应的电子名片
					UserVcard uCard = new UserVcard();
					uCard.setId(cardCursor.getInt(cardCursor.getColumnIndex(Provider.UserVcardColumns._ID)));
					uCard.setUserId(user.getId());
					uCard.setNickname(cardCursor.getString(cardCursor.getColumnIndex(Provider.UserVcardColumns.NICKNAME)));
					uCard.setIconPath(cardCursor.getString(cardCursor.getColumnIndex(Provider.UserVcardColumns.ICONPATH)));
					uCard.setThumbPath(cardCursor.getString(cardCursor.getColumnIndex(Provider.UserVcardColumns.THUMBPATH)));
					uCard.setIconHash(cardCursor.getString(cardCursor.getColumnIndex(Provider.UserVcardColumns.ICONHASH)));
					user.setUserVcard(uCard);
				}
				if (cardCursor != null) {
					cardCursor.close();
				}
				users.add(user);
				
				mUserCache.put(user.getUsername(), user);
			}
			cursor.close();
			Collections.sort(users, new User());
		}
		return users;
	}
	
	/**
	 * 更新本地数据库的所有好友信息，将网络上的好友同步到本地数据库
	 * @param list
	 */
	public void updateFriends(List<User> list) {
		if (!SystemUtil.isEmpty(list)) {
			
			SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
			
			db.beginTransaction();
			try {
				for (User user : list) {
					saveOrUpdateFriend(user, db, false);
				}
				
				List<User> allUsers = getLocalFriendSimpleInfo(db);
				if (SystemUtil.isNotEmpty(allUsers)) {	//全部的本地联系人
					if (allUsers.removeAll(list)) {	//多出的人，可以删除了
						MsgManager msgManager = MsgManager.getInstance();
						//删除多余的本地好友
						delteUsers(allUsers, db, msgManager, false);
					}
				}
				db.setTransactionSuccessful();
				notifyObservers(Provider.UserColumns.NOTIFY_FLAG, NotifyType.BATCH_UPDATE, list);
			} catch (Exception e) {
				Log.e("---updateFriends--error--list---" + list + "----------" + e.getMessage());
			} finally {
				db.endTransaction();
			}
			
		} else {
			Log.w("--updateFriends--list---为空或者数量为0--" + list);
		}
	}
	
	/**
	 * 根据好友的用户名列表来获取对应好友的基本信息列表,紧包含基本的id和vcard的id
	 * @return 好友的基本信息列表                    
	 * @author huanghui1
	 * @update 2015/11/26 9:14
	 * @version: 0.0.1
	 */
	public List<User> getLocalFriendSimpleInfo(SQLiteDatabase db) {
		if (db == null) {
			db = mChatDBHelper.getReadableDatabase();
		}
		List<User> usersList = new ArrayList<>();
		String[] projection = {
				Provider.UserColumns._ID,
				Provider.UserColumns.USERNAME
		};
		Cursor cursor = db.query(Provider.UserColumns.TABLE_NAME, projection, null, null, null, null, null);
		if (cursor != null) {
			while (cursor.moveToNext()) {
				int id = cursor.getInt(0);
				String username = cursor.getString(1);

				User user = new User();
				user.setId(id);
				user.setUsername(username);
				
				int vcardId = getUserVcardIdByUserId(db, id);
				if (vcardId > 0) {	//有电子名片信息
					UserVcard vcard = new UserVcard();
					vcard.setId(vcardId);
					vcard.setUserId(id);
					user.setUserVcard(vcard);
				}

				usersList.add(user);
			}
			cursor.close();
		}
		return usersList;
	}
	
	/**
	 * 清除本地数据库中所有的好友
	 * @return  是否成功删除所有的数据
	 */
	public boolean clearFriends() {
//		int count = mContext.getContentResolver().delete(Provider.UserColumns.CONTENT_URI, null, null);
		SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
		int count = db.delete(Provider.UserColumns.TABLE_NAME, null, null);
		boolean flag = count > 0;
		if (flag) {
			mUserCache.clear();
			notifyObservers(Provider.UserColumns.NOTIFY_FLAG, NotifyType.DELETE);
		}
		return flag;
	}
	
	/**
	 * 更新用户的状态
	 * @update 2014年12月2日 上午11:22:58
	 * @param presence
	 * @return
	 */
	public User updateUserPresence(Presence presence) {
		String from = presence.getFrom();
		//用户名，不含有"@及其之后的字符串"
		String username = XmppStringUtils.parseLocalpart(from);
		//用户使用的资源名（用什么设备登录的）
		String resource = XmppStringUtils.parseResource(from);
		String status = presence.getStatus();
		//用户在线的状态，如“chat、away等”
		Presence.Mode mode = presence.getMode();
		User user = new User();
		user.setUsername(username);
		user.setResource(resource);
		user.setStatus(status);
		if (mode == null) {//默认是在线
			mode = Presence.Mode.available;
		}
		user.setMode(mode.name());
		user = updateFriendStatus(user, false);
		return user;
	}
	
	/**
	 * 根据用户名来更新用户的在线状态信息
	 * @update 2014年12月2日 上午11:10:50
	 * @param user
	 * @param refreshUI 是否刷新界面
	 * @return
	 */
	public User updateFriendStatus(User user, boolean refreshUI) {
		if (user == null) {
			return null;
		}
		String username = user.getUsername();
		if (TextUtils.isEmpty(username)) {
			return null;
		}
		User u = getUserByUsername(username);
		if (u == null) {
			return null;
		}
		u.setMode(user.getMode());
		u.setResource(user.getResource());
		u.setStatus(user.getStatus());
		ContentValues userVaules = initUserPresenceContentVaules(u);
//		mContext.getContentResolver().update(Uri.withAppendedPath(Provider.UserColumns.CONTENT_URI, String.valueOf(u.getId())), userVaules, null, null);
		SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
		int count = db.update(Provider.UserColumns.TABLE_NAME, userVaules, Provider.UserColumns._ID + " = ?", new String[] {String.valueOf(u.getId())});
		if (count > 0) {
			if (refreshUI) {
				notifyObservers(Provider.UserColumns.NOTIFY_FLAG, NotifyType.UPDATE, user);
			}
		}
		return u;
	}
	
	/**
	 * 更新用户的昵称
	 * @update 2015年2月27日 下午2:22:39
	 * @param user 要更新的用户实体
	 * @return 更新后的用户实体
	 */
	public User updateFriendNick(User user) {
		if (user == null) {
			return null;
		}
		ContentValues values = new ContentValues();
		values.put(Provider.UserColumns.NICKNAME, user.getNickname());
//		mContext.getContentResolver().update(Uri.withAppendedPath(Provider.UserColumns.CONTENT_URI, String.valueOf(user.getId())), values, null, null);
		SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
		int count = db.update(Provider.UserColumns.TABLE_NAME, values, Provider.UserColumns._ID + " = ?", new String[] {String.valueOf(user.getId())});
		if (count > 0) {
			//更新缓存中的昵称
			User tUser = mUserCache.get(user.getUsername());
			if (tUser != null) {
				tUser.setNickname(user.getNickname());
			}
			notifyObservers(Provider.UserColumns.NOTIFY_FLAG, NotifyType.UPDATE, user);
		}
		return user;
	}
	
	/**
	 * 保存或更新好友信息
	 * @update 2014年10月23日 下午7:34:16
	 * @param user
	 * @return
	 */
	public User saveOrUpdateFriend(User user) {
		return saveOrUpdateFriend(user, null, true);
	}
	
	/**
	 * 保存或更新好友信息
	 * @update 2014年10月23日 下午7:34:16
	 * @param db 数据库对象
	 * @param user 用户实体
	 * @param notifyObserver 是否通知观察者刷新数据 
	 * @return
	 */
	public User saveOrUpdateFriend(User user, SQLiteDatabase db, boolean notifyObserver) {
//		Cursor cursor = mContext.getContentResolver().query(Provider.UserColumns.CONTENT_URI, new String[] {Provider.UserVcardColumns._ID}, Provider.UserColumns.USERNAME + " = ?", new String[] {user.getUsername()}, null);
		if (db == null) {
			db = mChatDBHelper.getWritableDatabase();
		}
//		Cursor cursor = db.rawQuery("select _id from t_user where username = ?", new String[] {user.getUsername()});
		Cursor cursor = db.query(Provider.UserColumns.TABLE_NAME, new String[] {Provider.UserColumns._ID}, Provider.UserColumns.USERNAME + " = ?", new String[] {user.getUsername()}, null, null, null);
		ContentValues userVaules = initUserContentVaules(user);
		if (cursor != null && cursor.moveToFirst()) {	//有好友，就更新
			//1、更新好友表
			user.setId(cursor.getInt(cursor.getColumnIndex(Provider.UserColumns._ID)));
//			mContext.getContentResolver().update(Uri.withAppendedPath(Provider.UserColumns.CONTENT_URI, String.valueOf(user.getId())), userVaules, null, null);
			db.update(Provider.UserColumns.TABLE_NAME, userVaules, Provider.UserColumns._ID + " = ?", new String[] {String.valueOf(user.getId())});
			//2、更新好友名片表
			UserVcard uCard = user.getUserVcard();
			if (uCard != null) {	//有名片就插入或更新
//				ContentValues cardValues = initUserVcardContentVaules(uCard);
//				mContext.getContentResolver().update(Uri.withAppendedPath(Provider.UserVcardColumns.CONTENT_URI, String.valueOf(uCard.getId())), cardValues, null, null);
				uCard = saveOrUpdateUserVacard(user, uCard, db, false);
				Log.d("-----saveOrUpdateFriend---update--UserVcard--成功--" + uCard);
				user.setUserVcard(uCard);
			} else {	//没有名片就查询
				uCard = new UserVcard();
//				Cursor cardCursor = mContext.getContentResolver().query(Provider.UserVcardColumns.CONTENT_URI, new String[] {Provider.UserVcardColumns._ID, Provider.UserVcardColumns.NICKNAME, Provider.UserVcardColumns.ICONHASH, Provider.UserVcardColumns.ICONPATH}, Provider.UserVcardColumns.USERID + " = ?", new String[] {String.valueOf(user.getId())}, null);
				String[] projection = {Provider.UserVcardColumns._ID, Provider.UserVcardColumns.NICKNAME, Provider.UserVcardColumns.ICONHASH, Provider.UserVcardColumns.THUMBPATH, Provider.UserVcardColumns.ICONPATH};
				Cursor cardCursor = db.query(Provider.UserVcardColumns.TABLE_NAME, projection, Provider.UserVcardColumns.USERID + " = ?", new String[] {String.valueOf(user.getId())}, null, null, null);
				if (cardCursor != null && cardCursor.moveToFirst()) {
					uCard.setId(cardCursor.getInt(cardCursor.getColumnIndex(Provider.UserVcardColumns._ID)));
					uCard.setUserId(user.getId());
					uCard.setNickname(cardCursor.getString(cardCursor.getColumnIndex(Provider.UserVcardColumns.NICKNAME)));
					uCard.setIconHash(cardCursor.getString(cardCursor.getColumnIndex(Provider.UserVcardColumns.ICONHASH)));
					uCard.setThumbPath(cardCursor.getString(cardCursor.getColumnIndex(Provider.UserVcardColumns.THUMBPATH)));
					uCard.setIconPath(cardCursor.getString(cardCursor.getColumnIndex(Provider.UserVcardColumns.ICONPATH)));
					user.setUserVcard(uCard);
					
					//将缓存中的user更新
					mUserCache.put(user.getUsername(), user);
				}
				if (cardCursor != null) {
					cardCursor.close();
				}
			}
			//更新与该好友有关的会话名称，群组除外
			MsgManager.getInstance().updateMsgThreadNameByUser(user, db, notifyObserver);
		} else {	//添加好友
			/*Uri uri = mContext.getContentResolver().insert(Provider.UserColumns.CONTENT_URI, userVaules);
			if (uri != null) {
				user.setId(Integer.parseInt(uri.getLastPathSegment()));
			}*/
			long rowId = db.insert(Provider.UserColumns.TABLE_NAME, Provider.UserColumns.DEAULT_NULL_COLUMN, userVaules);
			if (rowId > 0) {	//插入成功
//				cursor = db.query(Provider.UserColumns.TABLE_NAME, new String[] {Provider.UserColumns._ID}, Provider.UserColumns.USERNAME + " = ?", new String[] {user.getUsername()}, null, null, null);
//				if (cursor != null && cursor.moveToFirst()) {
//					user.setId(cursor.getInt(0));
//				}
				user.setId((int) rowId);
				Log.d("-----saveOrUpdateFriend---add--user--成功--" + user);
				if (notifyObserver) {
					notifyObservers(Provider.UserColumns.NOTIFY_FLAG, NotifyType.ADD, user);
				}
				//添加好友名片
				UserVcard uCard = user.getUserVcard();
				if (uCard != null) {
					uCard.setUserId(user.getId());
					saveOrUpdateUserVacard(user, uCard, db, false);
					user.setUserVcard(uCard);
					Log.d("-----saveOrUpdateFriend---add--UserVcard--成功--" + uCard);
				} else {
					//将user添加到缓存
					mUserCache.put(user.getUsername(), user);
				}
			} else {
				Log.w("-----saveOrUpdateFriend---add--user--失败--" + user);
			}
		}
		if (cursor != null) {
			cursor.close();
		}
		
		return user;
	}
	
	/**
	 * 更新用户
	 * @param user
	 * @return
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年10月3日 下午2:54:08
	 */
	public User updateFriend(User user, boolean notifyObserver) {
		if (user != null) {
			SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
			ContentValues userVaules = initUserContentVaules(user);
			int rowId = db.update(Provider.UserColumns.TABLE_NAME, userVaules, Provider.UserColumns._ID + " = ?", new String[]{String.valueOf(user.getId())});
			if (rowId > 0) {
				if (notifyObserver) {
					//查询有没对应好友的会话
					MsgManager msgManager = MsgManager.getInstance();
					boolean hasThread = msgManager.hasThread(user, db);
					if (hasThread) {	//有该会话，则需更新，但不需要更新会话的界面
						msgManager.updateMsgThreadNameByUser(user, db, false);
					}
					notifyObservers(Provider.UserColumns.NOTIFY_FLAG, NotifyType.UPDATE, user);
				}
				UserVcard vcard = user.getUserVcard();
				if (vcard != null) {
					//更新电子名片信息
					updateUserVcard(user, vcard, db, true);
				}
			}
		}
		return user;
	}
	
	/**
	 * 本地添加好友
	 * @update 2014年11月12日 下午4:01:23
	 * @param user
	 * @return
	 */
	public User addFriend(User user) {
		/*Uri uri;
		try {
			uri = mContext.getContentResolver().insert(Provider.UserColumns.CONTENT_URI, userVaules);
			if (uri != null) {
				user.setId(Integer.parseInt(uri.getLastPathSegment()));
			} else {
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}*/
		SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
		ContentValues userVaules = initUserContentVaules(user);
		long id = db.insert(Provider.UserColumns.TABLE_NAME, Provider.UserColumns.DEAULT_NULL_COLUMN, userVaules);
		if (id > 0) {
			//查询该好友的id
			int userId = getUserIdByUsername(user.getUsername());
			user.setId(userId);
			
			//添加好友名片
			UserVcard uCard = user.getUserVcard();
			if (uCard != null) {
				saveOrUpdateUserVacard(user, uCard, null, false);
				user.setUserVcard(uCard);
			} else {
				//将user添加到缓存
				mUserCache.put(user.getUsername(), user);
			}
			
			notifyObservers(Provider.UserColumns.NOTIFY_FLAG, NotifyType.ADD, user);
		}
		
		return user;
	}
	
	/**
	 * 删除指定的用户
	 * @update 2014年11月12日 下午7:24:54
	 * @param user
	 * @return
	 */
	public boolean deleteUser(User user) {
		MsgManager msgManager = MsgManager.getInstance();
		boolean flag = false;
		if (user == null) {
			return false;
		}
		//查询和自己有没有会话，群聊不算
		MsgThread msgThread = msgManager.getMsgThreadIdByMembers(Arrays.asList(user));
		//触发器会自动删除关联的会话
//		if (msgThread != null) {	//有会话
//			msgManager.deleteMsgThreadById(msgThread.getId());
//		}
//		int ucount = mContext.getContentResolver().delete(ContentUris.withAppendedId(Provider.UserColumns.CONTENT_URI, user.getId()), null, null);
		SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
		String uIdStr = String.valueOf(user.getId());
		int ucount = db.delete(Provider.UserColumns.TABLE_NAME, Provider.UserColumns._ID + " = ?", new String[]{uIdStr});
		if (ucount > 0) {	//删除成功
			
			if (msgThread != null) {
				msgManager.removeThreadCache(msgThread, true);
			}
			
			//删除该用户的缓存
			mUserCache.remove(user.getUsername());
			notifyObservers(Provider.UserColumns.NOTIFY_FLAG, NotifyType.DELETE, user);
			UserVcard ucard = user.getUserVcard();
			//删除好友的电子名片
			if (ucard != null) {	//有电子名片
//				int vcount = mContext.getContentResolver().delete(ContentUris.withAppendedId(Provider.UserVcardColumns.CONTENT_URI, user.getId()), null, null);
				//触发器已做了删除处理
//				int vcount = db.delete(Provider.UserVcardColumns.TABLE_NAME, Provider.UserVcardColumns._ID + " = ?", new String[] {uIdStr});
				//删除本地头像
				String iconPath = ucard.getIconPath();
				if (iconPath != null) {
					//删除头像文件
					SystemUtil.deleteFile(iconPath);
				}
				notifyObservers(Provider.UserVcardColumns.NOTIFY_FLAG, NotifyType.DELETE, ucard);
				
			}
			flag = true;
		}
		return flag;
	}
	
	/**
	 * 批量删除好友
	 * @param users 好友列表
	 * @param db 数据库，若为空，则自己创建
	 * @param refreshUI 是否通知界面更新
	 * @author huanghui1
	 * @update 2015/11/26 10:47
	 * @version: 0.0.1
	 * @return 返回是否删除成功
	 */
	public boolean delteUsers(List<User> users, SQLiteDatabase db, MsgManager msgManager, boolean refreshUI) {
		boolean flag = false;
		if (SystemUtil.isNotEmpty(users)) {
			if (db == null) {
				db = mChatDBHelper.getWritableDatabase();
			}
			if (msgManager == null) {
				msgManager = MsgManager.getInstance();
			}
			int length = users.size();
			String[] idStrs = new String[length];
			for (int i = 0; i < length; i++) {
				idStrs[i] = String.valueOf(users.get(i).getId());
			}
			int count = db.delete(Provider.UserColumns.TABLE_NAME, Provider.UserColumns._ID + " in (" + SystemUtil.makePlaceholders(length) + ")", idStrs);
			if (count > 0) {
				//查询和自己有没有会话，群聊不算
				List<MsgThread> msgThreads = msgManager.getMsgThreadIdsByMember(users, db);
				//删除会话的内存缓存，数据库的数据在删除用户的时候已经有触发器删除了
				if (SystemUtil.isNotEmpty(msgThreads)) {
					for (MsgThread msgThread : msgThreads) {
						msgManager.removeThreadCache(msgThread, true);
					}
				}

				for (User user : users) {
					//删除该用户的缓存
					mUserCache.remove(user.getUsername());
					if (refreshUI) {
						notifyObservers(Provider.UserColumns.NOTIFY_FLAG, NotifyType.DELETE, user);
					}
					UserVcard ucard = user.getUserVcard();
					//删除好友的电子名片
					if (ucard != null) {	//有电子名片
//				int vcount = mContext.getContentResolver().delete(ContentUris.withAppendedId(Provider.UserVcardColumns.CONTENT_URI, user.getId()), null, null);
						//触发器已做了删除处理
//				int vcount = db.delete(Provider.UserVcardColumns.TABLE_NAME, Provider.UserVcardColumns._ID + " = ?", new String[] {uIdStr});
						//删除本地头像
						String iconPath = ucard.getIconPath();
						if (iconPath != null) {
							//删除头像文件
							SystemUtil.deleteFile(iconPath);
						}
						if (refreshUI) {
							notifyObservers(Provider.UserVcardColumns.NOTIFY_FLAG, NotifyType.DELETE, ucard);
						}

					}
				}
				flag = true;
			}
		}
		return flag;
	}

	/**
	 * 更新用户信息，只更新nickname、uservcard等基本信息
	 * @update 2014年11月11日 下午9:51:42
	 * @param user
	 * @return
	 */
	public User updateSimpleUser(User user) {
		ContentValues values = new ContentValues();
		values.put(Provider.UserColumns.NICKNAME, user.getNickname());
		values.put(Provider.UserColumns.EMAIL, user.getEmail());
		values.put(Provider.UserColumns.PHONE, user.getPhone());
//		mContext.getContentResolver().update(ContentUris.withAppendedId(Provider.UserColumns.CONTENT_URI, user.getId()), values, null, null);
		SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
		int count = db.update(Provider.UserColumns.TABLE_NAME, values, Provider.UserColumns._ID + " = ?", new String[] {String.valueOf(user.getId())});
		if (count > 0) {
			notifyObservers(Provider.UserColumns.NOTIFY_FLAG, NotifyType.UPDATE, user);
		}
		//更新电子名片
		UserVcard uCard = user.getUserVcard();
		if (uCard != null) {
			uCard = saveOrUpdateUserVacard(user, uCard);
			user.setUserVcard(uCard);
		} else {
			//将user添加到缓存
			mUserCache.put(user.getUsername(), user);
		}
		return user;
	}
	
	/**
	 * 更新或保存好友的名片
	 * @update 2014年10月23日 下午8:20:43
	 * @param uCard
	 * @return
	 */
	public UserVcard saveOrUpdateUserVacard(User user, UserVcard uCard) {
		return saveOrUpdateUserVacard(user, uCard, null, true);
	}
	
	/**
	 * 更新或保存好友的名片
	 * @update 2014年10月23日 下午8:20:43
	 * @param db 数据库对象
	 * @param uCard 用户的电子名片信息
	 * @param notifyObserver 是否通知观察则刷新界面
	 * @return
	 */
	public UserVcard saveOrUpdateUserVacard(User user, UserVcard uCard, SQLiteDatabase db, boolean notifyObserver) {
//		Cursor cursor = mContext.getContentResolver().query(Provider.UserVcardColumns.CONTENT_URI, new String[] {Provider.UserVcardColumns._ID}, Provider.UserVcardColumns.USERID + " = ?", new String[] {String.valueOf(uCard.getUserId())}, null);
		if (db == null) {
			db = mChatDBHelper.getWritableDatabase();
		}
		Cursor cursor = db.query(Provider.UserVcardColumns.TABLE_NAME, new String[] {Provider.UserVcardColumns._ID}, Provider.UserVcardColumns.USERID + " = ?", new String[] {String.valueOf(uCard.getUserId())}, null, null, null);
		ContentValues cardVaules = initUserVcardContentVaules(uCard);
		if (cursor != null && cursor.moveToFirst()) {	//已经有名片数据了，就更新
			uCard.setId(cursor.getInt(cursor.getColumnIndex(Provider.UserVcardColumns._ID)));
//			mContext.getContentResolver().update(Uri.withAppendedPath(Provider.UserVcardColumns.CONTENT_URI, String.valueOf(uCard.getId())), cardVaules, null, null);
			int count = db.update(Provider.UserVcardColumns.TABLE_NAME, cardVaules, Provider.UserVcardColumns._ID + " = ?", new String[] {String.valueOf(uCard.getId())});
			if (count > 0) {
				//更新缓存的电子名片信息
				User tUser = mUserCache.get(user.getUsername());
				if (tUser != null) {
					tUser.setUserVcard(uCard);
				} else {
					user.setUserVcard(uCard);
					mUserCache.put(user.getUsername(), user);
				}
				if (notifyObserver) {
					Log.d("-----call----saveOrUpdateUserVacard----Provider.UserVcardColumns.NOTIFY_FLAG----NotifyType.UPDATE----");
					notifyObservers(Provider.UserVcardColumns.NOTIFY_FLAG, NotifyType.UPDATE, uCard);
				}
			}
		} else {	//添加名片
//			Uri uri = mContext.getContentResolver().insert(Provider.UserVcardColumns.CONTENT_URI, cardVaules);
//			if (uri != null) {
//				uCard.setId(Integer.parseInt(uri.getLastPathSegment()));
//			}
			long id = db.insert(Provider.UserVcardColumns.TABLE_NAME, Provider.UserVcardColumns.DEAULT_NULL_COLUMN, cardVaules);
			if (id > 0) {
				//查询添加的电子名片id
				int uCardId = getUserVcardIdByUserId(db, uCard.getUserId());
				if (uCardId != -1) {
					uCard.setId(uCardId);
				}
				//更新缓存的电子名片信息
				User tUser = mUserCache.get(user.getUsername());
				if (tUser != null) {
					tUser.setUserVcard(uCard);
				} else {
					user.setUserVcard(uCard);
					mUserCache.put(user.getUsername(), user);
				}
				if (notifyObserver) {
					Log.d("-----call----saveOrUpdateUserVacard----Provider.UserVcardColumns.NOTIFY_FLAG----NotifyType.UPDATE----");
					notifyObservers(Provider.UserVcardColumns.NOTIFY_FLAG, NotifyType.ADD, uCard);
				}
			}
		}
		if (cursor != null) {
			cursor.close();
		}
		return uCard;
	}
	
	/**
	 * 更新电子名片信息
	 * @param user 用户
	 * @param vcard 用户对应的电子名片信息
	 * @param db
	 * @param notifyObserver
	 * @return
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年10月3日 下午2:48:12
	 */
	public UserVcard updateUserVcard(User user, UserVcard vcard, SQLiteDatabase db, boolean notifyObserver) {
		if (db == null) {
			db = mChatDBHelper.getWritableDatabase();
		}
		ContentValues cardVaules = initUserVcardContentVaules(vcard);
		String iconHash = vcard.getIconHash();
		if (TextUtils.isEmpty(iconHash)) {	//没有头像信息，则删除本地文件
			String thumbPath = vcard.getThumbPath();
			if (!TextUtils.isEmpty(thumbPath)) {
				SystemUtil.deleteFile(thumbPath);
			}
			String iconPath = vcard.getIconPath();
			if (!TextUtils.isEmpty(iconPath)) {
				SystemUtil.deleteFile(iconPath);
			}
		}
		int rowId = db.update(Provider.UserVcardColumns.TABLE_NAME, cardVaules, Provider.UserVcardColumns._ID + " = ?", new String[] {String.valueOf(vcard.getId())});
		if (rowId > 0) {
			//更新缓存的电子名片信息
			User tUser = mUserCache.get(user.getUsername());
			if (tUser != null) {
				tUser.setUserVcard(vcard);
			} else {
				user.setUserVcard(vcard);
				mUserCache.put(user.getUsername(), user);
			}
			if (notifyObserver) {
				Log.d("-----call----updateUserVcard----Provider.UserVcardColumns.NOTIFY_FLAG----NotifyType.UPDATE----");
				notifyObservers(Provider.UserVcardColumns.NOTIFY_FLAG, NotifyType.UPDATE, vcard);
			}
		} else {
			Log.d("-----call----updateUserVcard----db.update.rowId 不大于0--更新失败--");
		}
		return vcard;
	}
	
	/**
	 * 添加用户的电子名片信息
	 * @param user
	 * @param uCard 用户电子名片信息 
	 * @return 添加后的电子名片信息
	 * @update 2015年8月12日 下午4:59:43
	 */
	public UserVcard addUserVcard(User user, UserVcard uCard) {
		SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
		try {
			ContentValues cardVaules = initUserVcardContentVaules(uCard);
			long id = db.insert(Provider.UserVcardColumns.TABLE_NAME, Provider.UserVcardColumns.DEAULT_NULL_COLUMN, cardVaules);
			if (id > 0) {
				int cardId = getUserVcardIdByUserId(db, uCard.getUserId());
				uCard.setId(cardId);
				User tUser = mUserCache.get(user.getUsername());
				if (tUser != null) {
					tUser.setUserVcard(uCard);
				} else {
					user.setUserVcard(uCard);
					mUserCache.put(user.getUsername(), user);
				}
				notifyObservers(Provider.UserVcardColumns.NOTIFY_FLAG, NotifyType.ADD, uCard);
			}
		} catch (Exception e) {
			Log.e(e.getMessage());
		}
		return uCard;
	}
	
	/**
	 * 保存或者更新简单的电子名片信息，只包括昵称、头像的hash
	 * @param user
	 * @param vcard
	 * @param db
	 * @param notifyObserver
	 * @return
	 * @update 2015年7月31日 下午7:32:32
	 */
	public UserVcard saveOrUpdateSimpleVcard(User user, UserVcard vcard, SQLiteDatabase db, boolean notifyObserver) {
		try {
			if (db == null) {
				db = mChatDBHelper.getWritableDatabase();
			}
			ContentValues values = new ContentValues();
			values.put(Provider.UserVcardColumns.NICKNAME, vcard.getNickname());
			values.put(Provider.UserVcardColumns.ICONHASH, vcard.getIconHash());
			User tUser = mUserCache.get(user.getUsername());
			long rowId = db.update(Provider.UserVcardColumns.TABLE_NAME, values, Provider.UserVcardColumns.USERID + " = ?", new String[] {String.valueOf(vcard.getUserId())});
			if (rowId > 0) {	//更新成功
				if (tUser != null) {
					UserVcard tCard = tUser.getUserVcard();
					if (tCard != null) {
						tCard.setNickname(vcard.getNickname());
						tCard.setIconHash(vcard.getIconHash());
					} else {
						tUser.setUserVcard(vcard);
					}
				}
				if (notifyObserver) {
					Log.d("-----call----saveOrUpdateSimpleVcard----Provider.UserVcardColumns.NOTIFY_FLAG----NotifyType.UPDATE----");
					notifyObservers(Provider.UserVcardColumns.NOTIFY_FLAG, NotifyType.UPDATE, vcard);
				}
			} else {	//添加该电子名片信息
				values.put(Provider.UserVcardColumns.USERID, vcard.getUserId());
				rowId= db.insert(Provider.UserVcardColumns.TABLE_NAME, Provider.UserVcardColumns.USERID, values);
				if (rowId > 0) {	//添加成功
					if (tUser != null) {
						UserVcard tCard = tUser.getUserVcard();
						if (tCard != null) {
							tCard.setNickname(vcard.getNickname());
							tCard.setIconHash(vcard.getIconHash());
						} else {
							tUser.setUserVcard(vcard);
						}
					}
					if (notifyObserver) {
						Log.d("-----call----saveOrUpdateSimpleVcard----Provider.UserVcardColumns.NOTIFY_FLAG----NotifyType.UPDATE----");
						notifyObservers(Provider.UserVcardColumns.NOTIFY_FLAG, NotifyType.UPDATE, vcard);
					}
				} else {
					return null;
				}
			}
			return vcard;
		} catch (Exception e) {
			Log.e("---saveOrUpdateSimpleVcard--vcard--" + vcard + "--------" + e.getMessage());
		}
		return null;
	}
	
	/**
	 * 更新用户的缩略图信息
	 * @param user
	 * @param vcard 用户的电子名片信息，只包含hash和thumbPath
	 * @return 是否更新成功
	 * @update 2015年8月4日 上午10:07:43
	 */
	public boolean updateUserVcardThumbIcon(User user, UserVcard vcard) {
		try {
			SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(Provider.UserVcardColumns.THUMBPATH, vcard.getThumbPath());
			values.put(Provider.UserVcardColumns.MIMETYPE, vcard.getMimeType());
			values.put(Provider.UserVcardColumns.ICONHASH, vcard.getIconHash());
			long rowId = db.update(Provider.UserVcardColumns.TABLE_NAME, values, Provider.UserVcardColumns.USERID + " = ?", new String[] {String.valueOf(vcard.getUserId())});
			boolean success = false;
			if (rowId > 0){
				User tUser = mUserCache.get(user.getUsername());
				if (tUser != null) {
					UserVcard tVcard = tUser.getUserVcard();
					if (tVcard != null) {
						tVcard.setThumbPath(vcard.getThumbPath());
						tVcard.setMimeType(vcard.getMimeType());
						tVcard.setIconHash(vcard.getIconHash());
					} else {
						tUser.setUserVcard(vcard);
					}
				}
				success = true;
				Log.d("-----call----updateUserVcardThumbIcon----Provider.UserVcardColumns.NOTIFY_FLAG----NotifyType.UPDATE----" + vcard);
				notifyObservers(Provider.UserVcardColumns.NOTIFY_FLAG, NotifyType.UPDATE, vcard);
			}
			return success;
		} catch (Exception e) {
			Log.e(e.getMessage());
		}
		return false;
	}
	
	/**
	 * 更新用户电子名片的头像hash值
	 * @param user
	 * @param vcard 用户的电子名片信息，只包含hash
	 * @return
	 * @update 2015年9月28日 下午3:44:42
	 */
	public boolean updateUserVcardIconHash(User user, UserVcard vcard) {
		try {
			SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(Provider.UserVcardColumns.ICONHASH, vcard.getIconHash());
			long rowId = db.update(Provider.UserVcardColumns.TABLE_NAME, values, Provider.UserVcardColumns.USERID + " = ?", new String[] {String.valueOf(vcard.getUserId())});
			boolean success = false;
			if (rowId > 0){
				User tUser = mUserCache.get(user.getUsername());
				if (tUser != null) {
					UserVcard tVcard = tUser.getUserVcard();
					if (tVcard != null) {
						tVcard.setIconHash(vcard.getIconHash());
					} else {
						tUser.setUserVcard(vcard);
					}
				}
				success = true;
				Log.d("-----call----updateUserVcardIconHash----Provider.UserVcardColumns.NOTIFY_FLAG----NotifyType.UPDATE----" + vcard);
				notifyObservers(Provider.UserVcardColumns.NOTIFY_FLAG, NotifyType.UPDATE, vcard);
			}
			return success;
		} catch (Exception e) {
			Log.e(e.getMessage());
		}
		return false;
	}
	
	/**
	 * 更新用户的原始图信息
	 * @param user
	 * @param vcard 用户的电子名片信息，只包含hash、mimeType和iconPath
	 * @return
	 * @update 2015年8月12日 下午3:46:55
	 */
	public boolean updateUserVcardOriginalIcon(User user, UserVcard vcard) {
		try {
			SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(Provider.UserVcardColumns.ICONPATH, vcard.getIconPath());
			values.put(Provider.UserVcardColumns.MIMETYPE, vcard.getMimeType());
			values.put(Provider.UserVcardColumns.ICONHASH, vcard.getIconHash());
			long rowId = db.update(Provider.UserVcardColumns.TABLE_NAME, values, Provider.UserVcardColumns.USERID + " = ?", new String[] {String.valueOf(vcard.getUserId())});
			boolean success = false;
			if (rowId > 0) {
				User tUser = mUserCache.get(user.getUsername());
				if (tUser != null) {
					UserVcard tVcard = tUser.getUserVcard();
					if (tVcard != null) {
						tVcard.setIconPath(vcard.getIconPath());
						tVcard.setMimeType(vcard.getMimeType());
						tVcard.setIconHash(vcard.getIconHash());
					} else {
						tUser.setUserVcard(vcard);
					}
				}
				success = true;
				Log.d("-----call----updateUserVcardOriginalIcon----Provider.UserVcardColumns.NOTIFY_FLAG----NotifyType.UPDATE----");
				notifyObservers(Provider.UserVcardColumns.NOTIFY_FLAG, NotifyType.UPDATE, vcard);
			}
			return success;
		} catch (Exception e) {
			Log.e(e.getMessage());
		}
		return false;
	}
	
	/**
	 * 更新好友的电子名片的头像信息，包括缩略图、原始图、hash、mimeType字段
	 * @param user
	 * @param vcard 好友的电子名片信息
	 * @return 是否更新成功，true:更新成功，false:更新失败
	 * @update 2015年8月12日 下午3:30:27
	 */
	public boolean updateUserVcardIcon(User user, UserVcard vcard) {
		try {
			SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(Provider.UserVcardColumns.THUMBPATH, vcard.getThumbPath());
			values.put(Provider.UserVcardColumns.ICONPATH, vcard.getIconPath());
			values.put(Provider.UserVcardColumns.ICONHASH, vcard.getIconHash());
			values.put(Provider.UserVcardColumns.MIMETYPE, vcard.getMimeType());
			long rowId = db.update(Provider.UserVcardColumns.TABLE_NAME, values, Provider.UserVcardColumns.USERID + " = ?", new String[] {String.valueOf(vcard.getUserId())});
			boolean success = false;
			if (rowId > 0) {
				success = true;
				User tUser = mUserCache.get(user.getUsername());
				if (tUser != null) {
					UserVcard tCard = tUser.getUserVcard();
					if (tCard != null) {
						tCard.setThumbPath(vcard.getThumbPath());
						tCard.setIconPath(vcard.getIconPath());
						tCard.setIconHash(vcard.getIconHash());
						tCard.setMimeType(vcard.getMimeType());
					} else {
						tUser.setUserVcard(vcard);
					}
				}
				Log.d("-----call----updateUserVcardIcon----Provider.UserVcardColumns.NOTIFY_FLAG----NotifyType.UPDATE----");
				notifyObservers(Provider.UserVcardColumns.NOTIFY_FLAG, NotifyType.UPDATE, vcard);
			}
			return success;
		} catch (Exception e) {
			Log.e(e.getMessage());
		}
		return false;
	}
	
	/**
	 * 批量更新电子名片信息列表
	 * @param vcardList
	 * @param map key为username,值为UserVcard的map
	 * @return 返回需要更新头像的map,username为key, UserVcard为value
	 * @update 2015年7月31日 下午8:32:14
	 */
	public Map<String, UserVcard> updateUserVcardList(List<VcardDto> vcardList, Map<String, UserVcard> map) {
		Map<String, UserVcard> downloadMap = new HashMap<>();
		SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			//以username为key，uservcard为value的map
			Map<String, UserVcard> userVcards = new HashMap<>();
			for (VcardDto vcardDto : vcardList) {
				String username = vcardDto.getUsername();
				UserVcard vcard = map.get(username);
				if (vcard != null) {	//存在vcard
					map.remove(username);	//找到userId后就移除，提高后期的查找速度
					vcard.setNickname(vcardDto.getNickName());
					vcard.setMimeType(vcardDto.getMimeType());
					//头像的hash需要下载头像的缩略图后再更新或者保存，不然头像下载出错后就无法再更新了
//					vcard.setIconHash(vcardDto.getHash());
					String oldHash = vcard.getIconHash();
					String newHash = vcardDto.getHash();
					//缩略图地址
					String thumbIconPath = vcard.getThumbPath();
					String iconPath = vcard.getIconPath();
					if (TextUtils.isEmpty(newHash)) {	//服务器上图像不存在，则删除本地头像
						vcard.setIconHash(null);
						//若本地头像文件存在，则删除本地文件，缩略图和原始头像都删除
						SystemUtil.deleteFile(thumbIconPath);
						//删除原始头像文件
						SystemUtil.deleteFile(iconPath);
					} else {	//服务器上头像存在
						if (!newHash.equals(oldHash) || (!SystemUtil.isFileExists(thumbIconPath) && !SystemUtil.isFileExists(iconPath))) {	//服务器上的头像与本地头像不一致，则需要下载服务器上的头像
							//本地缩略图不存在，也需要更新头像
							UserVcard tmpVcard = (UserVcard) vcard.clone();
							tmpVcard.setIconHash(newHash);
							downloadMap.put(username, tmpVcard);
						}
					}
					User user = new User();
					user.setUsername(username);
					user.setId(vcard.getUserId());
					vcard = saveOrUpdateSimpleVcard(user, vcard, db, false);
					if (vcard != null) {
						userVcards.put(username ,vcard);
					}
				}
			}
			db.setTransactionSuccessful();
			Log.d("-----call----updateUserVcardList----Provider.UserVcardColumns.NOTIFY_FLAG----NotifyType.BATCH_UPDATE----");
			notifyObservers(Provider.UserVcardColumns.NOTIFY_FLAG, NotifyType.BATCH_UPDATE, userVcards);
		} catch (Exception e) {
			Log.e("----updateUserVcardList---vcardList---" + vcardList + "-------" + e.getMessage());
		} finally {
			db.endTransaction();
		}
		Log.d("----需要更新头像的用户集合---downloadMap---" + downloadMap);
		return downloadMap;
	}
	
	/**
	 * 组装user表的键值对
	 * @update 2014年10月23日 下午8:14:28
	 * @param user
	 * @return
	 */
	private ContentValues initUserContentVaules(User user) {
		ContentValues userVaules = new ContentValues();
		userVaules.put(Provider.UserColumns.USERNAME, user.getUsername());
		userVaules.put(Provider.UserColumns.NICKNAME, user.getNickname());
		userVaules.put(Provider.UserColumns.EMAIL, user.getEmail());
		userVaules.put(Provider.UserColumns.PHONE, user.getPhone());
		String resource = user.getResource();
		if (!TextUtils.isEmpty(resource)) {
			userVaules.put(Provider.UserColumns.RESOURCE, resource);
		}
		String status = user.getStatus();
		if (!TextUtils.isEmpty(status)) {
			userVaules.put(Provider.UserColumns.STATUS, status);
		}
		String mode = user.getMode();
		if (!TextUtils.isEmpty(mode)) {
			userVaules.put(Provider.UserColumns.MODE, mode);
		}
		userVaules.put(Provider.UserColumns.FULLPINYIN, user.getFullPinyin());
		userVaules.put(Provider.UserColumns.SHORTPINYIN, user.getShortPinyin());
		userVaules.put(Provider.UserColumns.SORTLETTER, user.getSortLetter());
		return userVaules;
	}
	
	/**
	 * 初始化用户在线状态的信息
	 * @update 2014年12月2日 上午11:18:36
	 * @param user
	 * @return
	 */
	private ContentValues initUserPresenceContentVaules(User user) {
		ContentValues userVaules = new ContentValues();
		userVaules.put(Provider.UserColumns.RESOURCE, user.getResource());
		userVaules.put(Provider.UserColumns.STATUS, user.getStatus());
		userVaules.put(Provider.UserColumns.MODE, user.getMode());
		return userVaules;
	}
	
	/**
	 * 组装好友名片的键值对
	 * @update 2014年10月23日 下午8:14:48
	 * @param uCard
	 * @return
	 */
	private ContentValues initUserVcardContentVaules(UserVcard uCard) {
		ContentValues cardValues = new ContentValues();
		cardValues.put(Provider.UserVcardColumns.USERID, uCard.getUserId());
		cardValues.put(Provider.UserVcardColumns.NICKNAME, uCard.getNickname());
		cardValues.put(Provider.UserVcardColumns.REALNAME, uCard.getRealName());
		cardValues.put(Provider.UserVcardColumns.MOBILE, uCard.getMobile());
		cardValues.put(Provider.UserVcardColumns.COUNTRY, uCard.getCountry());
		cardValues.put(Provider.UserVcardColumns.PROVINCE, uCard.getProvince());
		cardValues.put(Provider.UserVcardColumns.CITY, uCard.getCity());
		cardValues.put(Provider.UserVcardColumns.STREET, uCard.getStreet());
		cardValues.put(Provider.UserVcardColumns.ZIPCODE, uCard.getZipCode());
		cardValues.put(Provider.UserVcardColumns.EMAIL, uCard.getEmail());
		cardValues.put(Provider.UserVcardColumns.ICONPATH, uCard.getIconPath());
		cardValues.put(Provider.UserVcardColumns.THUMBPATH, uCard.getThumbPath());
		cardValues.put(Provider.UserVcardColumns.ICONHASH, uCard.getIconHash());
		cardValues.put(Provider.UserVcardColumns.MIMETYPE, uCard.getMimeType());
		cardValues.put(Provider.UserVcardColumns.SEX, uCard.getSex());
		cardValues.put(Provider.UserVcardColumns.DESC, uCard.getDesc());
		return cardValues;
	}
	
	/**
	 * 根据用户id获取用户信息
	 * @update 2014年10月31日 下午10:00:48
	 * @param userId 用户的id
	 * @return
	 */
	public User getUserById(int userId) {
		User user = null;
		String[] projection = {
				Provider.UserColumns.USERNAME,
				Provider.UserColumns.NICKNAME,
				Provider.UserColumns.RESOURCE,
				Provider.UserColumns.FULLPINYIN,
				Provider.UserColumns.SHORTPINYIN,
				Provider.UserColumns.SORTLETTER
		};
//		Cursor cursor = mContext.getContentResolver().query(Provider.UserColumns.CONTENT_URI, projection, Provider.UserColumns._ID + " = ?", new String[] {String.valueOf(userId)}, null);
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		Cursor cursor = db.query(Provider.UserColumns.TABLE_NAME, projection, Provider.UserColumns._ID + " = ?", new String[] {String.valueOf(userId)}, null, null, null);
		if (cursor != null && cursor.moveToFirst()) {
			user = new User();
			user.setId(userId);
			user.setUsername(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.USERNAME)));
			user.setNickname(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.NICKNAME)));
			user.setResource(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.RESOURCE)));
			user.setFullPinyin(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.FULLPINYIN)));
			user.setShortPinyin(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.SHORTPINYIN)));
			user.setSortLetter(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.SORTLETTER)));
			
			UserVcard uCard = getSimpleUserVcardByUserId(userId);
			
			user.setUserVcard(uCard);
		}
		if (cursor != null) {
			cursor.close();
		}
		return user;
	}
	
	/**
	 * 根据username的数组来获取对应的用户集合
	 * @param usernames
	 * @return
	 * @update 2015年9月16日 下午7:52:51
	 */
	public List<User> getUsersByNames(String... usernames) {
		if (SystemUtil.isEmpty(usernames)) {
			return null;
		}
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		String[] projection = {
				Provider.UserColumns._ID,
				Provider.UserColumns.USERNAME,
				Provider.UserColumns.NICKNAME,
				Provider.UserColumns.RESOURCE,
				Provider.UserColumns.FULLPINYIN,
				Provider.UserColumns.SHORTPINYIN,
				Provider.UserColumns.SORTLETTER
		};
		List<User> users = null;
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		int length = usernames.length;
		for (int i = 0; i < length; i++) {
			sb.append("?").append(",");
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(")");
		Cursor cursor = db.query(Provider.UserColumns.TABLE_NAME, projection, Provider.UserColumns.USERNAME + " in " + sb.toString(), usernames, null, null, null);
		if (cursor != null) {
			users = new ArrayList<>();
			while (cursor.moveToNext()) {
				User user = new User();
				user.setId(cursor.getInt(cursor.getColumnIndex(Provider.UserColumns._ID)));
				user.setUsername(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.USERNAME)));
				user.setNickname(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.NICKNAME)));
				user.setResource(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.RESOURCE)));
				user.setFullPinyin(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.FULLPINYIN)));
				user.setShortPinyin(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.SHORTPINYIN)));
				user.setSortLetter(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.SORTLETTER)));
				UserVcard uCard = getSimpleUserVcardByUserId(user.getId());
				
				user.setUserVcard(uCard);
				
				users.add(user);
				
				mUserCache.put(user.getUsername(), user);
			}
			cursor.close();
		}
		return users;
	}
	
	/**
	 * 根据username的数组来获取对应的用户集合
	 * @param userIds
	 * @return
	 * @update 2015年9月16日 下午7:52:51
	 */
	public List<User> getUsersByIds(String... userIds) {
		if (userIds == null || userIds.length == 0) {
			return null;
		}
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		String[] projection = {
				Provider.UserColumns._ID,
				Provider.UserColumns.USERNAME,
				Provider.UserColumns.NICKNAME,
				Provider.UserColumns.RESOURCE,
				Provider.UserColumns.FULLPINYIN,
				Provider.UserColumns.SHORTPINYIN,
				Provider.UserColumns.SORTLETTER
		};
		List<User> users = null;
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		int length = userIds.length;
		for (int i = 0; i < length; i++) {
			sb.append("?").append(",");
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(")");
		Cursor cursor = db.query(Provider.UserColumns.TABLE_NAME, projection, Provider.UserColumns._ID + " in " + sb.toString(), userIds, null, null, null);
		if (cursor != null) {
			users = new ArrayList<>();
			while (cursor.moveToNext()) {
				User user = new User();
				user.setId(cursor.getInt(cursor.getColumnIndex(Provider.UserColumns._ID)));
				user.setUsername(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.USERNAME)));
				user.setNickname(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.NICKNAME)));
				user.setResource(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.RESOURCE)));
				user.setFullPinyin(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.FULLPINYIN)));
				user.setShortPinyin(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.SHORTPINYIN)));
				user.setSortLetter(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.SORTLETTER)));
				UserVcard uCard = getSimpleUserVcardByUserId(user.getId());
				
				user.setUserVcard(uCard);
				
				users.add(user);
				
				mUserCache.put(user.getUsername(), user);
			}
			cursor.close();
		}
		return users;
	}
	
	/**
	 * 根据用户名获取用户的信息
	 * @update 2014年10月31日 下午10:00:48
	 * @param username 用户名
	 * @return
	 */
	public User getUserByUsername(String username) {
		User user = null;
		if (username != null) {
			//先从缓存中查询
			user = mUserCache.get(username);
			if (user == null) {	//缓存中没有查到，则查询数据库
				String[] projection = {
						Provider.UserColumns._ID,
						Provider.UserColumns.NICKNAME,
						Provider.UserColumns.RESOURCE,
						Provider.UserColumns.FULLPINYIN,
						Provider.UserColumns.SHORTPINYIN,
						Provider.UserColumns.SORTLETTER
				};
//		Cursor cursor = mContext.getContentResolver().query(Provider.UserColumns.CONTENT_URI, projection, Provider.UserColumns.USERNAME + " = ?", new String[] {username}, null);
				SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
				Cursor cursor = db.query(Provider.UserColumns.TABLE_NAME, projection, Provider.UserColumns.USERNAME + " = ?", new String[] {username}, null, null, null);
				if (cursor != null && cursor.moveToFirst()) {
					user = new User();
					user.setId(cursor.getInt(cursor.getColumnIndex(Provider.UserColumns._ID)));
					user.setUsername(username);
					user.setNickname(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.NICKNAME)));
					user.setResource(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.RESOURCE)));
					user.setFullPinyin(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.FULLPINYIN)));
					user.setShortPinyin(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.SHORTPINYIN)));
					user.setSortLetter(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.SORTLETTER)));
					UserVcard uCard = getSimpleUserVcardByUserId(user.getId());
					
					user.setUserVcard(uCard);
					
					mUserCache.put(username, user);
				}
				if (cursor != null) {
					cursor.close();
				}
			}
		}
		return user;
	}
	
	/**
	 * 根据用户ID获取用的详细信息
	 * @param userId
	 * @return
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年10月2日 下午2:23:16
	 */
	public User getUserDetailById(int userId) {
		User user = null;
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		Cursor cursor = db.query(Provider.UserColumns.TABLE_NAME, Provider.UserColumns.DEFAULT_PROJECTION, Provider.UserColumns._ID + " = ?", new String[] {String.valueOf(userId)}, null, null, null);
		if (cursor != null && cursor.moveToFirst()) {
			user = new User();
			user.setId(userId);
			//_ID, USERNAME, EMAIL, NICKNAME, PHONE, RESOURCE, STATUS, MODE, FULLPINYIN, SHORTPINYIN, SORTLETTER
			user.setUsername(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.USERNAME)));
			user.setEmail(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.EMAIL)));
			user.setNickname(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.NICKNAME)));
			user.setPhone(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.PHONE)));
			user.setResource(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.RESOURCE)));
			user.setStatus(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.STATUS)));
			user.setMode(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.MODE)));
			user.setFullPinyin(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.FULLPINYIN)));
			user.setShortPinyin(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.SHORTPINYIN)));
			user.setSortLetter(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.SORTLETTER)));
			
			UserVcard vcard = getUserVcardByUserId(db, userId);
			if (vcard != null) {
				user.setUserVcard(vcard);
			}
			//更新缓存
			mUserCache.put(user.getUsername(), user);
		}
		if (cursor != null) {
			cursor.close();
		}
		return user;
	}
	
	/**
	 * 根据用户名来获取用户的详细信息
	 * @param username 用户名
	 * @return
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年10月3日 下午2:05:14
	 */
	public User getUserDetailByUsername(String username) {
		User user = null;
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		Cursor cursor = db.query(Provider.UserColumns.TABLE_NAME, Provider.UserColumns.DEFAULT_PROJECTION, Provider.UserColumns.USERNAME + " = ?", new String[] {username}, null, null, null);
		if (cursor != null && cursor.moveToFirst()) {
			user = new User();
			user.setId(cursor.getInt(cursor.getColumnIndex(Provider.UserColumns._ID)));
			//_ID, USERNAME, EMAIL, NICKNAME, PHONE, RESOURCE, STATUS, MODE, FULLPINYIN, SHORTPINYIN, SORTLETTER
			user.setUsername(username);
			user.setEmail(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.EMAIL)));
			user.setNickname(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.NICKNAME)));
			user.setPhone(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.PHONE)));
			user.setResource(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.RESOURCE)));
			user.setStatus(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.STATUS)));
			user.setMode(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.MODE)));
			user.setFullPinyin(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.FULLPINYIN)));
			user.setShortPinyin(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.SHORTPINYIN)));
			user.setSortLetter(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.SORTLETTER)));
			
			UserVcard vcard = getUserVcardByUserId(db, user.getId());
			if (vcard != null) {
				user.setUserVcard(vcard);
			}
			//更新缓存
			mUserCache.put(user.getUsername(), user);
		}
		if (cursor != null) {
			cursor.close();
		}
		return user;
	}
	
	/**
	 * 根据用户名获取用户的id
	 * @param username 用户名
	 * @return 用户id
	 * @update 2015年8月12日 下午5:07:16
	 */
	public int getUserIdByUsername(String username) {
		int userId = -1;
		if (username != null) {
			//先从缓存中查询
			User user = mUserCache.get(username);
			if (user != null) {
				userId = user.getId();
			} else {
				String[] projection = {
						Provider.UserColumns._ID
				};
//		Cursor cursor = mContext.getContentResolver().query(Provider.UserColumns.CONTENT_URI, projection, Provider.UserColumns.USERNAME + " = ?", new String[] {username}, null);
				SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
				Cursor cursor = db.query(Provider.UserColumns.TABLE_NAME, projection, Provider.UserColumns.USERNAME + " = ?", new String[] {username}, null, null, null);
				if (cursor != null && cursor.moveToFirst()) {
					userId = cursor.getInt(0);
				}
				if (cursor != null) {
					cursor.close();
				}
			}
		}
		return userId;
	}
	
	/**
	 * 根据用户名的数组来获取对应的id集合
	 * @param usernames
	 * @return
	 * @update 2015年9月17日 上午9:47:01
	 */
	public List<Integer> getUserIdsByNames(String... usernames) {
		if (SystemUtil.isEmpty(usernames)) {
			return null;
		}
		if (usernames.length == 1) {	//只有一个用户
			int userId = getUserIdByUsername(usernames[0]);
			if (userId != -1) {
				return Arrays.asList(userId);
			} else {
				return null;
			}
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append("(");
			int length = usernames.length;
			for (int i = 0; i < length; i++) {
				sb.append("?").append(",");
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.append(")");
			String[] projection = {
					Provider.UserColumns._ID
			};
			SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
			Cursor cursor = db.query(Provider.UserColumns.TABLE_NAME, projection, Provider.UserColumns.USERNAME + " in " + sb.toString(), usernames, null, null, null);
			List<Integer> list = null;
			if (cursor != null) {
				list = new ArrayList<>();
				while (cursor.moveToNext()) {
					list.add(cursor.getInt(0));
				}
				cursor.close();
			}
			return list;
		}
	}
	
	/**
	 * 获取指定用户的名片
	 * @update 2014年10月24日 下午3:36:39
	 * @param userId
	 * @return
	 */
	public UserVcard getSimpleUserVcardByUserId(int userId) {
		UserVcard uCard = null;
		String[] projection = {
				Provider.UserVcardColumns._ID,
				Provider.UserVcardColumns.NICKNAME,
				Provider.UserVcardColumns.ICONPATH,
				Provider.UserVcardColumns.THUMBPATH,
				Provider.UserVcardColumns.MIMETYPE,
				Provider.UserVcardColumns.ICONHASH
		};
//		Cursor cursor = mContext.getContentResolver().query(Provider.UserVcardColumns.CONTENT_URI, projection, Provider.UserVcardColumns.USERID + " = ?", new String[] {String.valueOf(userId)}, null);
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		Cursor cursor = db.query(Provider.UserVcardColumns.TABLE_NAME, projection, Provider.UserVcardColumns.USERID + " = ?", new String[] {String.valueOf(userId)}, null, null, null);
		if (cursor != null && cursor.moveToFirst()) {
			uCard = new UserVcard();
			uCard.setId(cursor.getInt(cursor.getColumnIndex(Provider.UserVcardColumns._ID)));
			uCard.setUserId(userId);
			uCard.setNickname(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.NICKNAME)));
			uCard.setIconPath(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.ICONPATH)));
			uCard.setThumbPath(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.THUMBPATH)));
			uCard.setMimeType(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.MIMETYPE)));
			uCard.setIconHash(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.ICONHASH)));
		}
		if (cursor != null) {
			cursor.close();
		}
		return uCard;
	}
	
	/**
	 * 获取指定用户的名片
	 * @update 2014年10月24日 下午3:36:39
	 * @param cardId
	 * @return
	 */
	public UserVcard getUserVcardById(int cardId) {
		UserVcard uCard = null;
//		Cursor cursor = mContext.getContentResolver().query(Uri.withAppendedPath(Provider.UserVcardColumns.CONTENT_URI, String.valueOf(cardId)), null, null, null, null);
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		Cursor cursor = db.query(Provider.UserVcardColumns.TABLE_NAME, Provider.UserVcardColumns.DEFAULT_PROJECTION, Provider.UserVcardColumns._ID + " = ?", new String[] {String.valueOf(cardId)}, null, null, null);
		if (cursor != null && cursor.moveToFirst()) {
			uCard = new UserVcard();
			uCard.setId(cardId);
			uCard.setUserId(cursor.getInt(cursor.getColumnIndex(Provider.UserVcardColumns.USERID)));
			uCard.setNickname(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.NICKNAME)));
			uCard.setRealName(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.REALNAME)));
			uCard.setEmail(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.EMAIL)));
			uCard.setMobile(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.MOBILE)));
			uCard.setCountry(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.COUNTRY)));
			uCard.setProvince(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.PROVINCE)));
			uCard.setCity(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.CITY)));
			uCard.setStreet(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.STREET)));
			uCard.setZipCode(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.ZIPCODE)));
			uCard.setIconPath(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.ICONPATH)));
			uCard.setThumbPath(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.THUMBPATH)));
			uCard.setIconHash(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.ICONHASH)));
			uCard.setMimeType(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.MIMETYPE)));
			uCard.setSex(cursor.getInt(cursor.getColumnIndex(Provider.UserVcardColumns.SEX)));
			uCard.setDesc(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.DESC)));
		}
		if (cursor != null) {
			cursor.close();
		}
		return uCard;
	}
	
	/**
	 * 获取指定用户的名片
	 * @update 2014年10月24日 下午3:36:39
	 * @param user
	 * @return
	 */
	public UserVcard getUserVcardByUserId(User user) {
		UserVcard uCard = null;
		User tUser = mUserCache.get(user.getUsername());
		if (tUser != null) {
			uCard = tUser.getUserVcard();
			if (uCard != null) {
				return uCard;
			}
		}
//		Cursor cursor = mContext.getContentResolver().query(Provider.UserVcardColumns.CONTENT_URI, null, Provider.UserVcardColumns.USERID + " = ?", new String[] {String.valueOf(userId)}, null);
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		Cursor cursor = db.query(Provider.UserVcardColumns.TABLE_NAME, Provider.UserVcardColumns.DEFAULT_PROJECTION, Provider.UserVcardColumns.USERID + " = ?", new String[] {String.valueOf(user.getId())}, null, null, null);
		if (cursor != null && cursor.moveToFirst()) {
			uCard = new UserVcard();
			uCard.setId(cursor.getInt(cursor.getColumnIndex(Provider.UserVcardColumns._ID)));
			uCard.setUserId(user.getId());
			uCard.setNickname(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.NICKNAME)));
			uCard.setRealName(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.REALNAME)));
			uCard.setEmail(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.EMAIL)));
			uCard.setMobile(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.MOBILE)));
			uCard.setCountry(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.COUNTRY)));
			uCard.setProvince(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.PROVINCE)));
			uCard.setCity(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.CITY)));
			uCard.setStreet(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.STREET)));
			uCard.setZipCode(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.ZIPCODE)));
			uCard.setIconPath(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.ICONPATH)));
			uCard.setThumbPath(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.THUMBPATH)));
			uCard.setIconHash(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.ICONHASH)));
			uCard.setMimeType(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.MIMETYPE)));
			uCard.setSex(cursor.getInt(cursor.getColumnIndex(Provider.UserVcardColumns.SEX)));
			uCard.setDesc(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.DESC)));
			
			if (tUser != null) {
				tUser.setUserVcard(uCard);
			}
		}
		if (cursor != null) {
			cursor.close();
		}
		return uCard;
	}
	
	/**
	 * 根据用户ID获取对应的vCard信息，直接插数据库，不从缓存中获取
	 * @param userId 用户的ID
	 * @return
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年10月2日 下午2:32:17
	 */
	public UserVcard getUserVcardByUserId(SQLiteDatabase db, int userId) {
		UserVcard uCard = null;
		if (db == null) {
			db = mChatDBHelper.getReadableDatabase();
		}
//		Cursor cursor = mContext.getContentResolver().query(Provider.UserVcardColumns.CONTENT_URI, null, Provider.UserVcardColumns.USERID + " = ?", new String[] {String.valueOf(userId)}, null);
		Cursor cursor = db.query(Provider.UserVcardColumns.TABLE_NAME, Provider.UserVcardColumns.DEFAULT_PROJECTION, Provider.UserVcardColumns.USERID + " = ?", new String[] {String.valueOf(userId)}, null, null, null);
		if (cursor != null && cursor.moveToFirst()) {
			uCard = new UserVcard();
			uCard.setId(cursor.getInt(cursor.getColumnIndex(Provider.UserVcardColumns._ID)));
			uCard.setUserId(userId);
			uCard.setNickname(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.NICKNAME)));
			uCard.setRealName(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.REALNAME)));
			uCard.setEmail(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.EMAIL)));
			uCard.setMobile(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.MOBILE)));
			uCard.setCountry(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.COUNTRY)));
			uCard.setProvince(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.PROVINCE)));
			uCard.setCity(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.CITY)));
			uCard.setStreet(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.STREET)));
			uCard.setZipCode(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.ZIPCODE)));
			uCard.setIconPath(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.ICONPATH)));
			uCard.setThumbPath(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.THUMBPATH)));
			uCard.setIconHash(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.ICONHASH)));
			uCard.setMimeType(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.MIMETYPE)));
			uCard.setSex(cursor.getInt(cursor.getColumnIndex(Provider.UserVcardColumns.SEX)));
			uCard.setDesc(cursor.getString(cursor.getColumnIndex(Provider.UserVcardColumns.DESC)));
			
		}
		if (cursor != null) {
			cursor.close();
		}
		return uCard;
	}
	
	/**
	 * 根据用户id获取对应的电子名片id
	 * @param db
	 * @param userId
	 * @return
	 * @update 2015年9月16日 下午4:06:02
	 */
	public int getUserVcardIdByUserId(SQLiteDatabase db, int userId) {
		if (db == null) {
			db = mChatDBHelper.getReadableDatabase();
		}
		int uCardId = -1;
//		Cursor cursor = mContext.getContentResolver().query(Provider.UserVcardColumns.CONTENT_URI, null, Provider.UserVcardColumns.USERID + " = ?", new String[] {String.valueOf(userId)}, null);
		Cursor cursor = db.query(Provider.UserVcardColumns.TABLE_NAME, new String[] {Provider.UserVcardColumns._ID}, Provider.UserVcardColumns.USERID + " = ?", new String[] {String.valueOf(userId)}, null, null, null);
		if (cursor != null && cursor.moveToFirst()) {
			uCardId = cursor.getInt(0);
		}
		if (cursor != null) {
			cursor.close();
		}
		return uCardId;
	}
	
	/**
	 * 加载本地好友的信息
	 * @update 2014年10月24日 下午4:47:38
	 * @param username
	 * @return
	 */
	public User loadLocalFriend(String username) {
		User user = null;
		
		User tUser = mUserCache.get(username);
		if (tUser != null) {
			return tUser;
		}
		
		String[] projection = {
				Provider.UserColumns._ID,
				Provider.UserColumns.NICKNAME,
				Provider.UserColumns.EMAIL,
				Provider.UserColumns.PHONE,
				Provider.UserColumns.RESOURCE,
		};
//		Cursor cursor = mContext.getContentResolver().query(Provider.UserColumns.CONTENT_URI, projection, Provider.UserColumns.USERNAME + " = ?", new String[] {username}, null);
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		Cursor cursor = db.query(Provider.UserColumns.TABLE_NAME, projection, Provider.UserColumns.USERNAME + " = ?", new String[] {username}, null, null, null);
		if (cursor != null && cursor.moveToFirst()) {	//能够给加载到本地好友的数据
			user = new User();
			user.setId(cursor.getInt(cursor.getColumnIndex(Provider.UserColumns._ID)));
			user.setUsername(username);
			user.setNickname(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.NICKNAME)));
			user.setPhone(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.PHONE)));
			user.setEmail(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.EMAIL)));
			user.setResource(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.RESOURCE)));
			user.setJID(user.initJID(user.getUsername()));
			
			//加载好友的本地电子名片
			UserVcard uCard = getUserVcardByUserId(user);
			if (uCard != null) {
				user.setUserVcard(uCard);
			}
			mUserCache.put(username, user);
		}
		if (cursor != null) {
			cursor.close();
		}
		return user;
	}
	
	/**
	 * 是否是本地好友
	 * @update 2014年10月24日 下午8:51:13
	 * @param username
	 * @return
	 */
	public boolean isLocalFriend(String username) {
		boolean flag = false;
//		Cursor cursor = mContext.getContentResolver().query(Provider.UserColumns.CONTENT_URI, new String[] {Provider.UserColumns._ID}, Provider.UserColumns.USERNAME + " = ?", new String[] {username}, null);
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		Cursor cursor = db.query(Provider.UserColumns.TABLE_NAME, new String[] {Provider.UserColumns._ID}, Provider.UserColumns.USERNAME + " = ?", new String[] {username}, null, null, null);
		if (cursor != null && cursor.moveToFirst()) {
			int id = cursor.getInt(0);
			if (id > 0) {
				flag = true;
			}
		}
		if (cursor != null) {
			cursor.close();
		}
		return flag;
	}
	
	/**
	 * 根据用户名获取用户电子名片的userId、头像hash值、原始图像地址、缩略图地址、mimeType
	 * @param username 用户名
	 * @return
	 * @update 2015年8月12日 下午3:04:02
	 */
	public UserVcard getUserIconVcard(String username) {
		if (TextUtils.isEmpty(username)) {
			return null;
		}
		User tUser = mUserCache.get(username);
		if (tUser != null) {
			UserVcard tCard = tUser.getUserVcard();
			if (tCard != null) {
				return tCard;
			}
		}
		UserVcard userVcard = null;
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		String sql = "select v.userId, v.iconPath, v.thumbPath, v.iconHash, v.mimeType from t_user_vcard v left join t_user u on v.userId = u._id where u.username = ?";
		Cursor cursor = db.rawQuery(sql, new String[] {username});
		if (cursor != null) {
			try {
				if (cursor.moveToFirst()) {
					userVcard = new UserVcard();
					userVcard.setUserId(cursor.getInt(0));
					userVcard.setIconPath(cursor.getString(1));
					userVcard.setThumbPath(cursor.getString(2));
					userVcard.setIconHash(cursor.getString(3));
					userVcard.setMimeType(cursor.getString(4));
					
					if (tUser != null) {
						tUser.setUserVcard(userVcard);
						mUserCache.put(username, tUser);
					}
				}
			} catch (Exception e) {
				Log.e(e.getMessage());
			} finally {
				if (cursor != null) {
					cursor.close();
				}
			}
		}
		return userVcard;
	}
	
	/**
	 * 将cursor转换为对象
	 * @update 2014年11月10日 下午10:13:11
	 * @param cursor
	 * @return
	 */
	private NewFriendInfo cursoToInfo(Cursor cursor) {
		NewFriendInfo newFriendInfo = new NewFriendInfo();
		newFriendInfo.setId(cursor.getInt(cursor.getColumnIndex(Provider.NewFriendColumns._ID)));
		newFriendInfo.setFriendStatus(NewFriendInfo.FriendStatus.valueOf(cursor.getInt(cursor.getColumnIndex(Provider.NewFriendColumns.FRIEND_STATUS))));
		newFriendInfo.setCreationDate(cursor.getLong(cursor.getColumnIndex(Provider.NewFriendColumns.CREATION_DATE)));
		newFriendInfo.setContent(cursor.getString(cursor.getColumnIndex(Provider.NewFriendColumns.CONTENT)));
		newFriendInfo.setFrom(cursor.getString(cursor.getColumnIndex(Provider.NewFriendColumns.FROM_USER)));
		newFriendInfo.setTo(cursor.getString(cursor.getColumnIndex(Provider.NewFriendColumns.TO_USER)));
		newFriendInfo.setIconHash(cursor.getString(cursor.getColumnIndex(Provider.NewFriendColumns.ICON_HASH)));
		newFriendInfo.setIconPath(cursor.getString(cursor.getColumnIndex(Provider.NewFriendColumns.ICON_PATH)));
		int userId = cursor.getInt(cursor.getColumnIndex(Provider.NewFriendColumns.USER_ID));
		User user = getUserById(userId);
		newFriendInfo.setUser(user);
		return newFriendInfo;
	}
	
	/**
	 * 获取新的朋友列表信息
	 * @update 2014年11月10日 下午2:48:16
	 * @return
	 */
	public List<NewFriendInfo> getNewFriendInfos() {
		List<NewFriendInfo> list = null;
//		Cursor cursor = mContext.getContentResolver().query(Provider.NewFriendColumns.CONTENT_URI, null, null, null, null);
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		Cursor cursor = db.query(Provider.NewFriendColumns.TABLE_NAME, Provider.NewFriendColumns.DEFAULT_PROJECTION, null, null, null, null, Provider.NewFriendColumns.DEFAULT_SORT_ORDER);
		if (cursor != null) {
			list = new ArrayList<>();
			while (cursor.moveToNext()) {
				NewFriendInfo newFriendInfo = cursoToInfo(cursor);
				list.add(newFriendInfo);
			}
			
			cursor.close();
		}
		return list;
	}
	
	/**
	 * 根据新的新的朋友信息id获得其详细信息
	 * @update 2014年11月10日 下午10:04:20
	 * @param infoId
	 * @return
	 */
	public NewFriendInfo getNewFriendInfoById(int infoId) {
//		Uri uri = ContentUris.withAppendedId(Provider.NewFriendColumns.CONTENT_URI, infoId);
		NewFriendInfo newInfo = null;
//		Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null);
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		Cursor cursor = db.query(Provider.NewFriendColumns.TABLE_NAME, Provider.NewFriendColumns.DEFAULT_PROJECTION, null, null, null, null, null);
		if (cursor != null && cursor.moveToFirst()) {
			newInfo = cursoToInfo(cursor);
		}
		if (cursor != null) {
			cursor.close();
		}
		return newInfo;
	}
	
	/**
	 * 根据新的新的朋友信息uri获得其详细信息,该方法已被废弃，使用来{@link UserManager#getNewFriendInfoById(int)}来代替
	 * @update 2014年11月11日 下午3:53:35
	 * @param uri
	 * @return
	 */
	@Deprecated
	public NewFriendInfo getNewFriendInfoByUri(Uri uri) {
		NewFriendInfo newInfo = null;
//		Uri uri = ContentUris.withAppendedId(Provider.NewFriendColumns.CONTENT_URI, infoId);
		Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null);
		if (cursor != null && cursor.moveToFirst()) {
			newInfo = cursoToInfo(cursor);
		}
		if (cursor != null) {
			cursor.close();
		}
		return newInfo;
	}
	
	/**
	 * 根据新的朋友信息id获得其详细信息
	 * @update 2014年11月10日 下午10:04:20
	 * @param from
	 * @param to
	 * @return
	 */
	public NewFriendInfo getNewFriendInfoByAccounts(String from, String to) {
		NewFriendInfo newInfo = null;
//		Cursor cursor = mContext.getContentResolver().query(Provider.NewFriendColumns.CONTENT_URI, null, Provider.NewFriendColumns.FROM_USER + " = ? and " + Provider.NewFriendColumns.TO_USER + " = ?", new String[] {from, to}, null);
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		Cursor cursor = db.query(Provider.NewFriendColumns.TABLE_NAME, Provider.NewFriendColumns.DEFAULT_PROJECTION, Provider.NewFriendColumns.FROM_USER + " = ? and " + Provider.NewFriendColumns.TO_USER + " = ?", new String[] {from, to}, null, null, null);
		if (cursor != null && cursor.moveToFirst()) {
			newInfo = cursoToInfo(cursor);
		}
		if (cursor != null) {
			cursor.close();
		}
		return newInfo;
	}
	
	/**
	 * 初始化新的朋友的数据库值
	 * @update 2014年11月10日 下午3:18:40
	 * @param newInfo
	 * @return
	 */
	private ContentValues initNewFriendSimpleContentValues(NewFriendInfo newInfo) {
		ContentValues values = new ContentValues();
		values.put(Provider.NewFriendColumns.FRIEND_STATUS, newInfo.getFriendStatus().ordinal());
		values.put(Provider.NewFriendColumns.CONTENT, newInfo.getContent());
		User user = newInfo.getUser();
		if (user != null) {
			values.put(Provider.NewFriendColumns.USER_ID, user.getId());
		}
		return values;
	}
	
	/**
	 * 初始化新的朋友的数据库值
	 * @update 2014年11月10日 下午3:18:40
	 * @param newInfo
	 * @return
	 */
	private ContentValues initNewFriendContentValues(NewFriendInfo newInfo) {
		ContentValues values = new ContentValues();
		values.put(Provider.NewFriendColumns.CREATION_DATE, newInfo.getCreationDate());
		values.put(Provider.NewFriendColumns.FRIEND_STATUS, newInfo.getFriendStatus().ordinal());
		values.put(Provider.NewFriendColumns.CONTENT, newInfo.getContent());
		values.put(Provider.NewFriendColumns.FROM_USER, newInfo.getFrom());
		values.put(Provider.NewFriendColumns.TO_USER, newInfo.getTo());
		values.put(Provider.NewFriendColumns.ICON_HASH, newInfo.getIconHash());
		values.put(Provider.NewFriendColumns.ICON_PATH, newInfo.getIconPath());
		User user = newInfo.getUser();
		int id = 0;
		if (user != null) {
			id = user.getId();
		}
		values.put(Provider.NewFriendColumns.USER_ID, id);
		return values;
	}
	
	/**
	 * 
	 * @update 2014年11月10日 下午3:24:54
	 * @param newInfo
	 * @return
	 */
	public NewFriendInfo addNewFriendInfo(NewFriendInfo newInfo) {
		if (newInfo == null) {
			return null;
		}
		ContentValues values = initNewFriendContentValues(newInfo);
		/*Uri uri = mContext.getContentResolver().insert(Provider.NewFriendColumns.CONTENT_URI, values);
		if (uri != null) {
			int id = Integer.parseInt(uri.getLastPathSegment());
			newInfo.setId(id);
		}*/
		SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
		long rowId = db.insert(Provider.NewFriendColumns.TABLE_NAME, null, values);
		if (rowId > 0) {
			int id = getNewFriendInfoId(db, newInfo.getFrom(), newInfo.getTo());
			if (id != -1) {
				newInfo.setId(id);
			}
			notifyObservers(Provider.NewFriendColumns.NOTIFY_FLAG, NotifyType.ADD, newInfo);
		}
		return newInfo;
	}
	
	/**
	 * 获取新的好友信息id
	 * @param from
	 * @param to
	 * @return
	 * @update 2015年9月28日 下午5:34:35
	 */
	public int getNewFriendInfoId(SQLiteDatabase db, String from, String to) {
		if (db == null) {
			db = mChatDBHelper.getReadableDatabase();
		}
		int id = -1;
		Cursor cursor = db.query(Provider.NewFriendColumns.TABLE_NAME, new String[] {Provider.NewFriendColumns._ID}, Provider.NewFriendColumns.FROM_USER + " = ? AND " + Provider.NewFriendColumns.TO_USER + " = ?", new String[] {from, to}, null, null, null);
		if (cursor != null && cursor.moveToFirst()) {
			id = cursor.getInt(0);
		}
		if (cursor != null) {
			cursor.close();
		}
		return id;
	}
	
	/**
	 * 保存或添加新的朋友信息
	 * @update 2014年11月10日 下午10:02:52
	 * @param newInfo
	 * @return
	 */
	public NewFriendInfo saveOrUpdateNewFriendInfo(NewFriendInfo newInfo) {
		if (newInfo == null) {
			return null;
		}
		NewFriendInfo temp = getNewFriendInfoByAccounts(newInfo.getFrom(), newInfo.getTo());
		if (temp == null) {	//不存在，则添加
			newInfo = addNewFriendInfo(newInfo);
		} else {	//更新
			newInfo.setId(temp.getId());
			User user = newInfo.getUser();
			if (user == null) {
				newInfo.setUser(temp.getUser());
			}
			newInfo = updateNewFriendInfo(newInfo);
		}
		return newInfo;
	}
	
	/**
	 * 判断本地是否有对应的新的好友请求信息
	 * @param newInfo
	 * @return
	 * @update 2015年9月24日 下午7:12:41
	 */
	public boolean hasNewFriendInfo(NewFriendInfo newInfo) {
		if (newInfo == null) {
			return false;
		}
		boolean flag = false;
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		Cursor cursor = db.query(Provider.NewFriendColumns.TABLE_NAME, new String[] {"count(*)"}, Provider.NewFriendColumns.FROM_USER + " = ? and " + Provider.NewFriendColumns.TO_USER + " = ?", new String[] {newInfo.getFrom(), newInfo.getTo()}, null, null, null);
		
		if (cursor != null && cursor.moveToFirst()) {
			flag = cursor.getLong(0) > 0;
		}
		if (cursor != null) {
			cursor.close();
		}
		return flag;
	}
	
	/**
	 * 根据id删除新的朋友信息
	 * @update 2014年11月10日 下午3:29:47
	 * @param infoId
	 * @return 是否删除成功
	 */
	public boolean deleteNewFriendInfo(int infoId) {
//		Uri uri = ContentUris.withAppendedId(Provider.NewFriendColumns.CONTENT_URI, infoId);
//		int count = mContext.getContentResolver().delete(uri, null, null);
		SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
		int count = db.delete(Provider.NewFriendColumns.TABLE_NAME, Provider.NewFriendColumns._ID + " = ?", new String[] {String.valueOf(infoId)});
		if (count > 0) {
			NewFriendInfo newInfo = new NewFriendInfo();
			newInfo.setId(infoId);
			notifyObservers(Provider.NewFriendColumns.NOTIFY_FLAG, NotifyType.DELETE, newInfo);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 更新新的朋友的信息的状态
	 * @update 2014年11月10日 下午3:17:14
	 * @param newInfo
	 * @return
	 */
	public NewFriendInfo updateNewFriendInfoState(NewFriendInfo newInfo) {
		if (newInfo == null) {
			return null;
		}
		ContentValues values = initNewFriendSimpleContentValues(newInfo);
//		Uri uri = ContentUris.withAppendedId(Provider.NewFriendColumns.CONTENT_URI, newInfo.getId());
//		mContext.getContentResolver().update(uri, values, null, null);
		SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
		int count = db.update(Provider.NewFriendColumns.TABLE_NAME, values, Provider.NewFriendColumns._ID + " = ?", new String[] {String.valueOf(newInfo.getId())});
		if (count > 0) {
			notifyObservers(Provider.NewFriendColumns.NOTIFY_FLAG, NotifyType.UPDATE, newInfo);
		}
		return newInfo;
	}
	
	/**
	 * 更新新的朋友的信息
	 * @update 2014年11月10日 下午3:17:14
	 * @param newInfo
	 * @return
	 */
	public NewFriendInfo updateNewFriendInfo(NewFriendInfo newInfo) {
		if (newInfo == null) {
			return null;
		}
		ContentValues values = initNewFriendContentValues(newInfo);
//		Uri uri = ContentUris.withAppendedId(Provider.NewFriendColumns.CONTENT_URI, newInfo.getId());
//		mContext.getContentResolver().update(uri, values, null, null);
		SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
		int count = db.update(Provider.NewFriendColumns.TABLE_NAME, values, Provider.NewFriendColumns._ID + " = ?", new String[] {String.valueOf(newInfo.getId())});
		if (count > 0) {
			notifyObservers(Provider.NewFriendColumns.NOTIFY_FLAG, NotifyType.UPDATE, newInfo);
		}
		return newInfo;
	}
	
	/**
	 * 更新好友请求信息的头像
	 * @param newInfo 好友请求信息
	 * @return
	 * @update 2015年9月28日 下午7:01:24
	 */
	public NewFriendInfo updateNewFriendInfoAvatar(NewFriendInfo newInfo) {
		if (newInfo == null) {
			return null;
		}
		ContentValues values = new ContentValues();
		values.put(Provider.NewFriendColumns.ICON_HASH, newInfo.getIconHash());
		values.put(Provider.NewFriendColumns.ICON_PATH, newInfo.getIconPath());
		SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
		int rowId = db.update(Provider.NewFriendColumns.TABLE_NAME, values, Provider.NewFriendColumns._ID + " = ?", new String[] {String.valueOf(newInfo.getId())});
		
		User user = newInfo.getUser();
		if (user != null) {	//需更新好友请求的信息
			updateUserVcardThumbIcon(user, user.getUserVcard());
		} else {
			if (rowId > 0) {
				notifyObservers(Provider.NewFriendColumns.NOTIFY_FLAG, NotifyType.UPDATE, newInfo);
			}
		}
		return newInfo;
	}
}