package net.ibaixin.chat.manager;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.download.ImageDownloader.Scheme;
import com.nostra13.universalimageloader.utils.DiskCacheUtils;
import com.nostra13.universalimageloader.utils.MemoryCacheUtils;

import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.R;
import net.ibaixin.chat.db.ChatDatabaseHelper;
import net.ibaixin.chat.model.Album;
import net.ibaixin.chat.model.AudioItem;
import net.ibaixin.chat.model.ChatChoseItem;
import net.ibaixin.chat.model.FileItem;
import net.ibaixin.chat.model.MsgInfo;
import net.ibaixin.chat.model.MsgInfo.SendState;
import net.ibaixin.chat.model.MsgInfo.Type;
import net.ibaixin.chat.model.MsgPart;
import net.ibaixin.chat.model.MsgThread;
import net.ibaixin.chat.model.MsgUploadInfo;
import net.ibaixin.chat.model.PhotoItem;
import net.ibaixin.chat.model.User;
import net.ibaixin.chat.provider.Provider;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.ImageUtil;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.MimeUtils;
import net.ibaixin.chat.util.Observable;
import net.ibaixin.chat.util.Observer;
import net.ibaixin.chat.util.Observer.NotifyType;
import net.ibaixin.chat.util.SystemUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 聊天相关的业务逻辑层
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年10月31日 上午9:44:30
 */
public class MsgManager extends Observable<Observer> {
	private static MsgManager instance = null;
	private Context mContext = ChatApplication.getInstance();
	
	private ChatDatabaseHelper mChatDBHelper;
	
	/*
	 * 以threadId为key,MsgThread为value的缓存
	 */
	private Map<Integer, MsgThread> mThreadCache = new WeakHashMap<>();
	
	private MsgManager() {
		ChatApplication app = (ChatApplication) mContext.getApplicationContext();
		mChatDBHelper = new ChatDatabaseHelper(mContext, app.getAccountDbDir());
	}
	
	public static MsgManager getInstance() {
		if (instance == null) {
			synchronized (MsgManager.class) {
				if (instance == null) {
					instance = new MsgManager();
				}
			}
		}
		return instance;
	}
	
	/**
	 * 将msgthread添加到缓存中
	 * @param msgThread
	 * @update 2015年9月18日 下午5:58:56
	 */
	public void addThreadCache(MsgThread msgThread) {
		if (msgThread != null) {
			mThreadCache.put(msgThread.getId(), msgThread);
		}
	}
	
	/**
	 * 将msgthread从缓存中删除
	 * @param msgThread
	 * @param notifyUi 是否刷新界面
	 * @update 2015年9月18日 下午6:00:11
	 */
	public void removeThreadCache(MsgThread msgThread, boolean notifyUi) {
		if (msgThread != null) {
			mThreadCache.remove(msgThread.getId());
			if (notifyUi) {
				notifyObservers(Provider.MsgThreadColumns.NOTIFY_FLAG, NotifyType.DELETE, msgThread);
			}
		}
	}
	
	/**
	 * 从缓存中获取msgthread
	 * @param threadId
	 * @return
	 * @update 2015年9月18日 下午6:01:11
	 */
	public MsgThread getThreadCache(int threadId) {
		return mThreadCache.get(threadId);
	}
	
	/**
	 * 根据id获取会话信息
	 * @update 2014年10月31日 上午10:55:25
	 * @param id
	 * @return
	 */
	public MsgThread getThreadById(int id) {
		MsgThread mt = null;
		mt = mThreadCache.get(id);
		if (mt != null) {
			return mt;
		}
//		Uri uri = ContentUris.withAppendedId(Provider.MsgThreadColumns.CONTENT_URI, id);
//		Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null);
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		Cursor cursor = db.query(Provider.MsgThreadColumns.TABLE_NAME, Provider.MsgThreadColumns.DEFAULT_PROJECTION, Provider.MsgThreadColumns._ID + " = ?", new String[] {String.valueOf(id)}, null, null, null);
		if (cursor != null && cursor.moveToFirst()) {
			mt = new MsgThread();
			mt.setId(id);
			mt.setMsgThreadName(cursor.getString(cursor.getColumnIndex(Provider.MsgThreadColumns.MSG_THREAD_NAME)));
			mt.setModifyDate(cursor.getLong(cursor.getColumnIndex(Provider.MsgThreadColumns.MODIFY_DATE)));
			mt.setSnippetContent(cursor.getString(cursor.getColumnIndex(Provider.MsgThreadColumns.SNIPPET_CONTENT)));
			mt.setUnReadCount(cursor.getInt(cursor.getColumnIndex(Provider.MsgThreadColumns.UNREAD_COUNT)));
			String memberIds = cursor.getString(cursor.getColumnIndex(Provider.MsgThreadColumns.MEMBER_IDS));
			
			List<User> members = getMemebersByMemberIds(memberIds);
			mt.setMembers(members);
			
			String snippetId = cursor.getString(cursor.getColumnIndex(Provider.MsgThreadColumns.SNIPPET_ID));
			mt.setSnippetId(snippetId);
			//查询该会话的最后一条消息
			MsgInfo msgInfo = getMsgInfoById(snippetId);
			mt.setLastMsgInfo(msgInfo);
			
			mt.setTop(cursor.getInt(cursor.getColumnIndex(Provider.MsgThreadColumns.IS_TOP)) == 0 ? false : true);
			
			//加入到缓存中
			mThreadCache.put(id, mt);
			
		}
		if (cursor != null) {
			cursor.close();
		}
		return mt;
	}
	
	/**
	 * 根据聊天的参与成员获取对应的会话
	 * @update 2014年10月31日 上午10:00:59
	 * @param member 聊天成员
	 * @return
	 */
	public MsgThread getThreadByMember(User member) {
		if (member == null) {
			return null;
		}
		String memberIds = String.valueOf(member.getId());
		MsgThread mt = null;
//		Cursor cursor = mContext.getContentResolver().query(Provider.MsgThreadColumns.CONTENT_URI, null, Provider.MsgThreadColumns.MEMBER_IDS + " = ?", new String[] {memberIds}, null);
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		String[] projection = {Provider.MsgThreadColumns._ID, Provider.MsgThreadColumns.MSG_THREAD_NAME};
		Cursor cursor = db.query(Provider.MsgThreadColumns.TABLE_NAME, projection, Provider.MsgThreadColumns.MEMBER_IDS + " = ?", new String[] {memberIds}, null, null, null);
		if (cursor != null && cursor.moveToFirst()) {
			mt = new MsgThread();
			mt.setId(cursor.getInt(cursor.getColumnIndex(Provider.MsgThreadColumns._ID)));
			mt.setMsgThreadName(cursor.getString(cursor.getColumnIndex(Provider.MsgThreadColumns.MSG_THREAD_NAME)));
			mt.setMembers(Arrays.asList(member));
		}
		if (cursor != null) {
			cursor.close();
		}
		return mt;
	}
	
	/**
	 * 查询是否有对应好友的会话
	 * @param user
	 * @return true:有该会话
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年10月3日 下午3:31:36
	 */
	public boolean hasThread(User user, SQLiteDatabase db) {
		if (db == null) {
			db = mChatDBHelper.getReadableDatabase();
		}
		String memberIds = String.valueOf(user.getId());
		Cursor cursor = db.query(Provider.MsgThreadColumns.TABLE_NAME, new String[] {"count(*)"}, Provider.MsgThreadColumns.MEMBER_IDS + " = ?", new String[] {memberIds}, null, null, null);
		long count = 0;
		if (cursor != null && cursor.moveToFirst()) {
			count = cursor.getLong(0);
		}
		if (cursor != null) {
			cursor.close();
		}
		return count > 0;
	}
	
	/**
	 * 根据会话的成员获得会话的id，该实体中只包含msgThreadId
	 * @update 2014年11月12日 下午7:44:45
	 * @param members
	 * @return
	 */
	public MsgThread getMsgThreadIdByMembers(List<User> members) {
		if (SystemUtil.isEmpty(members)) {
			return null;
		}
		String memberIds = getMemberIds(members);
		MsgThread msgThread = null;
//		Cursor cursor = mContext.getContentResolver().query(Provider.MsgThreadColumns.CONTENT_URI, new String[] {Provider.MsgThreadColumns._ID}, Provider.MsgThreadColumns.MEMBER_IDS + " = ?", new String[] {memberIds}, null);
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		String[] projection = {Provider.MsgThreadColumns._ID};
		Cursor cursor = db.query(Provider.MsgThreadColumns.TABLE_NAME, projection, Provider.MsgThreadColumns.MEMBER_IDS + " = ?", new String[]{memberIds}, null, null, null);
		if (cursor != null && cursor.moveToFirst()) {
			msgThread = new MsgThread();
			msgThread.setId(cursor.getInt(0));
		}
		if (cursor != null) {
			cursor.close();
		}
		return msgThread;
	}
	
	/**
	 * 根据用户列表来获取该用户对应的会话列表，紧限于单用户的会话，不适用群聊
	 * @param users 用户列表
	 * @return 会话的列表                
	 * @author huanghui1
	 * @update 2015/11/26 11:01
	 * @version: 0.0.1
	 */
	public List<MsgThread> getMsgThreadIdsByMember(List<User> users, SQLiteDatabase db) {
		if (SystemUtil.isNotEmpty(users)) {
			if (db == null) {
				db = mChatDBHelper.getReadableDatabase();
			}
			String selection = null;
			int lenth = users.size();
			String[] ids = new String[lenth];
			for (int i = 0; i < lenth; i++) {
				ids[i] = String.valueOf(users.get(i).getId());
			}
			if (lenth == 1) {	//只有一个用户
				selection = Provider.MsgThreadColumns.MEMBER_IDS + " = ?";
			} else {	//多个用户
				selection = Provider.MsgThreadColumns.MEMBER_IDS + " in (" + SystemUtil.makePlaceholders(lenth) + ")";
			}
			String[] projection = {Provider.MsgThreadColumns._ID};
			Cursor cursor = db.query(Provider.MsgThreadColumns.TABLE_NAME, projection, selection, ids, null, null, null);
			List<MsgThread> list = null;
			if (cursor != null) {
				list = new ArrayList<>();
				while (cursor.moveToNext()) {
					MsgThread msgThread = new MsgThread();
					msgThread.setId(cursor.getInt(0));
					list.add(msgThread);
				}
				cursor.close();
			}
			return list;
		}
		return null;
	}
	
	/**
	 * 初始化MsgThread的数据源
	 * @update 2014年10月31日 下午2:06:34
	 * @param msgThread
	 * @return
	 */
	private ContentValues initMsgThreadVaule(MsgThread msgThread, String memberIds) {
		ContentValues values = new ContentValues();
//		String memberIds = getMemberIds(msgThread.getMembers());
		String threadNme = msgThread.getMsgThreadName();
		if (TextUtils.isEmpty(threadNme)) {
			threadNme = getMsgThreadName(msgThread.getMembers());
			msgThread.setMsgThreadName(threadNme);
		}
		values.put(Provider.MsgThreadColumns.MSG_THREAD_NAME, threadNme);
		values.put(Provider.MsgThreadColumns.MEMBER_IDS, memberIds);
		values.put(Provider.MsgThreadColumns.UNREAD_COUNT, msgThread.getUnReadCount());
		values.put(Provider.MsgThreadColumns.SNIPPET_ID, msgThread.getSnippetId());
		String snippetContent = msgThread.getSnippetContent();
		if (snippetContent == null) {
			snippetContent = "";
		}
		values.put(Provider.MsgThreadColumns.SNIPPET_CONTENT, snippetContent);
		long time = msgThread.getModifyDate();
		if (time <= 0) {
			time = System.currentTimeMillis();
		}
		values.put(Provider.MsgThreadColumns.MODIFY_DATE, time);
		values.put(Provider.MsgThreadColumns.IS_TOP, msgThread.isTop() ? 1 : 0);
		return values;
	}
	
	/**
	 * 根据成员列表获取成员id的字符串,用";"分割
	 * @update 2014年10月31日 下午2:09:36
	 * @param members
	 * @return
	 */
	private String getMemberIds(List<User> members) {
		return getMemberIds(members, Constants.SPLITE_SEMICOLON);
	}
	
	/**
	 * 根据成员列表获取成员id的字符串
	 * @param users
	 * @param splite 分隔符
	 * @author huanghui1
	 * @update 2015/11/26 11:21
	 * @version: 0.0.1
	 */
	private String getMemberIds(List<User> users, String splite) {
		String memberIds = null;
		if (users.size() == 1) {	//只有一个成员
			memberIds = String.valueOf(users.get(0).getId());
		} else {
			StringBuilder sb = new StringBuilder();
			for (User user : users) {
				sb.append(user.getId()).append(splite);
			}
			if (sb.length() > 1) {
				//去除最后一个多余的分隔符
				sb.deleteCharAt(sb.length() - 1);
			}
			memberIds = sb.toString();
		}
		return memberIds;
	}
	
	/**
	 * 将成员id数组组装成以,为分割的字符串
	 * @param memberIds
	 * @return
	 * @update 2015年9月16日 下午3:06:29
	 */
	public String getMemberIds(int... memberIds) {
		String idStr = null;
		if (memberIds != null && memberIds.length > 0) {
			if (memberIds.length == 1) {	//只有一个成员
				idStr = String.valueOf(memberIds[0]);
			} else {
				StringBuilder sb = new StringBuilder();
				for (int id : memberIds) {
					sb.append(id).append(Constants.SPLITE_SEMICOLON);
				}
				//去除最后一个多余的分隔符
				sb.deleteCharAt(sb.length() - 1);
				idStr = sb.toString();
			}
		}
		return idStr;
	}
	
	/**
	 * 根据用户名来获取对应的用户id
	 * @param memAccounts
	 * @return
	 * @update 2015年9月16日 下午8:18:37
	 */
	public String getMemberIds(String... memAccounts) {
		UserManager userManager = UserManager.getInstance();
		String strIds = null;
		if (memAccounts.length == 1) {	//该会话只有一个成员，除自己外
			int userId = userManager.getUserIdByUsername(memAccounts[0]);
			if (userId != -1) {
				strIds = String.valueOf(userId);
			}
		} else {
			StringBuilder sb = new StringBuilder();
			List<Integer> list = userManager.getUserIdsByNames(memAccounts);
			if (!SystemUtil.isEmpty(list)) {
				for (int id : list) {
					sb.append(id).append(Constants.SPLITE_SEMICOLON);
				}
				sb.deleteCharAt(sb.length() - 1);
				strIds = sb.toString();
			}
		}
		return strIds;
	}
	
	/**
	 * 根据成员获取会话名称
	 * @update 2014年10月31日 下午2:44:52
	 * @param members
	 * @return
	 */
	private String getMsgThreadName(List<User> members) {
		if (members == null || members.size() == 0) {
			return null;
		}
		int size = members.size();
		String threadName = null;
		if (size == 0) {	//只有一个人
			threadName = members.get(0).getName();
		} else if (size <= 3) {	//少于3个人
			StringBuilder sb = new StringBuilder();
			for (User user : members) {
				sb.append(user.getName()).append("、");
			}
			sb.deleteCharAt(sb.length() - 1);
			threadName = sb.toString();
		} else {	//是列出前三个人的名称作为会话名称
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < 4; i++) {
				User user = members.get(i);
				sb.append(user.getName()).append("、");
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.append("...");
			threadName = sb.toString();
		}
		return threadName;
	}
	
	/**
	 * 创建一个会话
	 * @update 2014年10月31日 下午2:55:27
	 * @param msgThread
	 * @return 创建后的会话
	 */
	public synchronized MsgThread createMsgThread(MsgThread msgThread) {
		if (msgThread == null) {
			return null;
		}
		
		String memberIds = getMemberIds(msgThread.getMembers());
		SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
		ContentValues values = initMsgThreadVaule(msgThread, memberIds);
		db.beginTransaction();
		try {
			long rowId = db.insert(Provider.MsgThreadColumns.TABLE_NAME, null, values);
			if (rowId > 0) {
//				if (memberIds != null) {
//					int threadId = getThreadIdByMemberIds(memberIds);
					//查询刚创建的会哈id
					msgThread.setId((int) rowId);
					
					//添加到缓存
					mThreadCache.put((int) rowId, msgThread);
//				}
				db.setTransactionSuccessful();
				notifyObservers(Provider.MsgThreadColumns.NOTIFY_FLAG, NotifyType.ADD, msgThread);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}
//		Uri uri = mContext.getContentResolver().insert(Provider.MsgThreadColumns.CONTENT_URI, initMsgThreadVaule(msgThread));
//		if (uri != null) {
//			int threadId = Integer.parseInt(uri.getLastPathSegment());  	
//			msgThread.setId(threadId);
//		}
		return msgThread;
	}
	
	/**
	 * 根据会话id获取该会话内的所有消息，每个msginfo值包含id和msgType
	 * @update 2014年11月12日 下午7:59:37
	 * @param threadId
	 * @return
	 */
	public List<MsgInfo> getMsgInfoIdsByThreadId(int threadId) {
//		Cursor cursor = mContext.getContentResolver().query(Provider.MsgInfoColumns.CONTENT_URI, new String[] {Provider.MsgInfoColumns.MSG_ID, Provider.MsgInfoColumns.MSG_TYPE}, Provider.MsgInfoColumns.THREAD_ID + " = ?", new String[] {String.valueOf(threadId)}, null);
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		String[] projection = {Provider.MsgInfoColumns.MSG_ID, Provider.MsgInfoColumns.MSG_TYPE};
		Cursor cursor = db.query(Provider.MsgInfoColumns.TABLE_NAME, projection, Provider.MsgInfoColumns.THREAD_ID + " = ?", new String[]{String.valueOf(threadId)}, null, null, Provider.MsgInfoColumns.DEFAULT_SORT_ORDER);
		List<MsgInfo> list = null;
		if (cursor != null) {
			list = new ArrayList<>();
			while (cursor.moveToNext()) {
				MsgInfo msgInfo = new MsgInfo();
				msgInfo.setMsgId(cursor.getString(0));
				msgInfo.setMsgType(Type.valueOf(cursor.getInt(1)));
				list.add(msgInfo);
			}
			cursor.close();
		}
		return list;
	}
	
	/**
	 * 根据会话id删除指定的会话
	 * @update 2014年11月12日 下午7:37:41
	 * @param threadId
	 * @return
	 */
	public boolean deleteMsgThreadById(int threadId) {
		boolean falg = false;
		SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
		int count = db.delete(Provider.MsgThreadColumns.TABLE_NAME, Provider.MsgThreadColumns._ID + " = ?", new String[]{String.valueOf(threadId)});
//		int count = mContext.getContentResolver().delete(ContentUris.withAppendedId(Provider.MsgThreadColumns.CONTENT_URI, threadId), null, null);
		if (count > 0) {	//删除成功
			//删除会话中的消息
			//查找该会话中的消息
			db.delete(Provider.MsgInfoColumns.TABLE_NAME, Provider.MsgInfoColumns.THREAD_ID + " = ?", new String[] {String.valueOf(threadId)});
			
			mThreadCache.remove(threadId);
			
			notifyObservers(Provider.MsgThreadColumns.NOTIFY_FLAG, NotifyType.DELETE, null);
			/*List<MsgInfo> msgInfos = getMsgInfoIdsByThreadId(threadId);
			if (!SystemUtil.isEmpty(msgInfos)) {	//该会话有消息
				//删除消息
				for (MsgInfo msgInfo : msgInfos) {
					deleteMsgInfoById(msgInfo, null);
				}
			}*/
			falg = true;
		}
		return falg;
	}
	
	/**
	 * 获得所有的会话列表
	 * @update 2014年10月31日 下午9:09:11
	 * @return 所有的会话列表
	 */
	public List<MsgThread> getMsgThreadList() {
		return getMsgThreadList(true);
	}
	
	/**
	 * 加载会话列表
	 * @param loadLastMsg 是否加载该会话最后一条信息
	 * @return 返回会话列表                
	 * 创建人：huanghui1
	 * 创建时间： 2015/11/9 19:34
	 * 修改人：huanghui1
	 * 修改时间：2015/11/9 19:34
	 * 修改备注：
	 * @version: 0.0.1
	 */
	public List<MsgThread> getMsgThreadList(boolean loadLastMsg) {
		List<MsgThread> list = null;
//		Cursor cursor = mContext.getContentResolver().query(Provider.MsgThreadColumns.CONTENT_URI, null, null, null, null);
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		Cursor cursor = db.query(Provider.MsgThreadColumns.TABLE_NAME, Provider.MsgThreadColumns.DEFAULT_PROJECTION, null, null, null, null, Provider.MsgThreadColumns.DEFAULT_SORT_ORDER);
		if (cursor != null) {
			list = new ArrayList<>();
			while (cursor.moveToNext()) {
				MsgThread msgThread = new MsgThread();
				msgThread.setId(cursor.getInt(cursor.getColumnIndex(Provider.MsgThreadColumns._ID)));
				msgThread.setModifyDate(cursor.getLong(cursor.getColumnIndex(Provider.MsgThreadColumns.MODIFY_DATE)));
				msgThread.setMsgThreadName(cursor.getString(cursor.getColumnIndex(Provider.MsgThreadColumns.MSG_THREAD_NAME)));
				msgThread.setSnippetContent(cursor.getString(cursor.getColumnIndex(Provider.MsgThreadColumns.SNIPPET_CONTENT)));
				msgThread.setUnReadCount(cursor.getInt(cursor.getColumnIndex(Provider.MsgThreadColumns.UNREAD_COUNT)));
				String memberIds = cursor.getString(cursor.getColumnIndex(Provider.MsgThreadColumns.MEMBER_IDS));
				
				List<User> members = getMemebersByMemberIds(memberIds);
				msgThread.setMembers(members);
				msgThread.setTop(cursor.getInt(cursor.getColumnIndex(Provider.MsgThreadColumns.IS_TOP)) == 0 ? false : true);
				
				String snippetId = cursor.getString(cursor.getColumnIndex(Provider.MsgThreadColumns.SNIPPET_ID));
				msgThread.setSnippetId(snippetId);
				if (loadLastMsg) {
					//查询该会话最后一条消息
					MsgInfo msgInfo = getMsgInfoById(snippetId);
					msgThread.setLastMsgInfo(msgInfo);
					mThreadCache.put(msgThread.getId(), msgThread);
				}
				list.add(msgThread);
				
			}
			cursor.close();
		}
		return list;
	}
	
	/**
	 * 获取转发、分享等选择会话的列表
	 * @param dataType 要加载的类型，主要是会话和联系人两类，0：{@code ChatChoseItem.TYPE_THREAD }表示会话，1:{@code ChatChoseItem.TYPE_CONTACT }表示好友
	 * 创建人：huanghui1
	 * 创建时间： 2015/11/10 17:24
	 * 修改人：huanghui1
	 * 修改时间：2015/11/10 17:24
	 * 修改备注：
	 * @version: 0.0.1
	 */
	public List<ChatChoseItem> getChatChoseItems(int dataType) {
		List<ChatChoseItem> list = null;
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		switch (dataType) {
			case ChatChoseItem.DATA_THREAD:	//加载会话
				Cursor cursor = db.query(Provider.MsgThreadColumns.TABLE_NAME, Provider.MsgThreadColumns.DEFAULT_PROJECTION, null, null, null, null, Provider.MsgThreadColumns.DEFAULT_SORT_ORDER);
				if (cursor != null) {
					list = new ArrayList<>();
					while (cursor.moveToNext()) {
						MsgThread msgThread = new MsgThread();
						msgThread.setId(cursor.getInt(cursor.getColumnIndex(Provider.MsgThreadColumns._ID)));
						msgThread.setModifyDate(cursor.getLong(cursor.getColumnIndex(Provider.MsgThreadColumns.MODIFY_DATE)));
						msgThread.setMsgThreadName(cursor.getString(cursor.getColumnIndex(Provider.MsgThreadColumns.MSG_THREAD_NAME)));
						msgThread.setSnippetContent(cursor.getString(cursor.getColumnIndex(Provider.MsgThreadColumns.SNIPPET_CONTENT)));
						msgThread.setUnReadCount(cursor.getInt(cursor.getColumnIndex(Provider.MsgThreadColumns.UNREAD_COUNT)));
						String memberIds = cursor.getString(cursor.getColumnIndex(Provider.MsgThreadColumns.MEMBER_IDS));

						List<User> members = getMemebersByMemberIds(memberIds);
						msgThread.setMembers(members);
						msgThread.setTop(cursor.getInt(cursor.getColumnIndex(Provider.MsgThreadColumns.IS_TOP)) == 0 ? false : true);

						String snippetId = cursor.getString(cursor.getColumnIndex(Provider.MsgThreadColumns.SNIPPET_ID));
						msgThread.setSnippetId(snippetId);

						ChatChoseItem choseItem = new ChatChoseItem();
						choseItem.setDataType(dataType);
						choseItem.setMsgThread(msgThread);
						choseItem.setItemType(ChatChoseItem.TYPE_ITEM);
						list.add(choseItem);

					}
					cursor.close();
				}
				break;
			case ChatChoseItem.DATA_CONTACT:	//加载好友
				List<User> users = UserManager.getInstance().getFriends(db);
				if (!SystemUtil.isEmpty(users)) {
					list = new ArrayList<>();
					//将排好顺序的users添加到
					for (User user : users) {
						ChatChoseItem choseItem = new ChatChoseItem();
						choseItem.setDataType(dataType);
						choseItem.setItemType(ChatChoseItem.TYPE_ITEM);
						choseItem.setUser(user);

						list.add(choseItem);
					}
				}
				break;
		}
		return list;
	}
	
	/**
	 * 根据聊天成员的id获取成员信息
	 * @update 2014年10月31日 下午9:54:50
	 * @param memberIds
	 * @return
	 */
	public List<User> getMemebersByMemberIds(String memberIds) {
		if (TextUtils.isEmpty(memberIds)) {
			return null;
		}
		UserManager userManager = UserManager.getInstance();
		List<User> list = null;
		if (memberIds.contains(";")) {	///有多个成员
			String[] ids = memberIds.split(";");
			list = userManager.getUsersByNames(ids);
		} else {
			list = new ArrayList<>();
			User user = userManager.getUserById(Integer.parseInt(memberIds));
			list.add(user);
		}
		return list;
	}
	
	/**
	 * 根据聊天的参与成员获取对应的会话
	 * @update 2014年10月31日 上午10:00:59
	 * @param members
	 * @return
	 */
	public MsgThread getThreadByMembers(List<User> members) {
		if (members == null || members.size() == 0) {
			return null;
		}
		
		String memberIds = getMemberIds(members);
		
		MsgThread msgThread = null;
//		Cursor cursor = mContext.getContentResolver().query(Provider.MsgThreadColumns.CONTENT_URI, null, Provider.MsgThreadColumns.MEMBER_IDS + " = ?", new String[] {memberIds}, null);
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		Cursor cursor = db.query(Provider.MsgThreadColumns.TABLE_NAME, Provider.MsgThreadColumns.DEFAULT_PROJECTION, Provider.MsgThreadColumns.MEMBER_IDS + " = ?", new String[] {memberIds}, null, null, null);
		if (cursor != null && cursor.moveToFirst()) {
			msgThread = new MsgThread();
			msgThread.setId(cursor.getInt(cursor.getColumnIndex(Provider.MsgThreadColumns._ID)));
			msgThread.setModifyDate(cursor.getLong(cursor.getColumnIndex(Provider.MsgThreadColumns.MODIFY_DATE)));
			msgThread.setMsgThreadName(cursor.getString(cursor.getColumnIndex(Provider.MsgThreadColumns.MSG_THREAD_NAME)));
			msgThread.setSnippetContent(cursor.getString(cursor.getColumnIndex(Provider.MsgThreadColumns.SNIPPET_CONTENT)));
			msgThread.setUnReadCount(cursor.getInt(cursor.getColumnIndex(Provider.MsgThreadColumns.UNREAD_COUNT)));
			msgThread.setTop(cursor.getInt(cursor.getColumnIndex(Provider.MsgThreadColumns.IS_TOP)) == 0 ? false : true);
			msgThread.setMembers(members);

			String snippetId = cursor.getString(cursor.getColumnIndex(Provider.MsgThreadColumns.SNIPPET_ID));
			msgThread.setSnippetId(snippetId);
			//查询会话最后一条消息
			MsgInfo msgInfo = getMsgInfoById(snippetId);
			msgThread.setLastMsgInfo(msgInfo);
		}
		if (cursor != null) {
			cursor.close();
		}
		return msgThread;
	}
	
	/**
	 * 根据消息id获取的附件信息
	 * @update 2014年10月31日 上午11:54:18
	 * @param msgId
	 * @return
	 */
	public MsgPart getMsgPartByMsgId(String msgId) {
//		Uri uri = ContentUris.withAppendedId(Provider.MsgPartColumns.CONTENT_URI, msgId);
//		Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null);
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		Cursor cursor = db.query(Provider.MsgPartColumns.TABLE_NAME, Provider.MsgPartColumns.DEFAULT_PROJECTION, Provider.MsgPartColumns.MSG_ID + " = ?", new String[]{msgId}, null, null, null);
		MsgPart msgPart = null;
		if (cursor != null && cursor.moveToFirst()) {
			msgPart = new MsgPart();
			msgPart.setId(cursor.getInt(cursor.getColumnIndex(Provider.MsgPartColumns._ID)));
			msgPart.setFileName(cursor.getString(cursor.getColumnIndex(Provider.MsgPartColumns.FILE_NAME)));
			msgPart.setFilePath(cursor.getString(cursor.getColumnIndex(Provider.MsgPartColumns.FILE_PATH)));
			msgPart.setCreationDate(cursor.getLong(cursor.getColumnIndex(Provider.MsgPartColumns.CREATION_DATE)));
			msgPart.setMimeType(cursor.getString(cursor.getColumnIndex(Provider.MsgPartColumns.MIME_TYPE)));
			msgPart.setSize(cursor.getLong(cursor.getColumnIndex(Provider.MsgPartColumns.SIZE)));
			msgPart.setFileToken(cursor.getString(cursor.getColumnIndex(Provider.MsgPartColumns.FILE_TOKEN)));
			msgPart.setDesc(cursor.getString(cursor.getColumnIndex(Provider.MsgPartColumns.DESC)));
			msgPart.setThumbPath(cursor.getString(cursor.getColumnIndex(Provider.MsgPartColumns.FILE_THUMB_PATH)));
			msgPart.setDownloaded(cursor.getInt(cursor.getColumnIndex(Provider.MsgPartColumns.DOWNLOADED)) == 1 ? true : false);
			msgPart.setMsgId(msgId);
		}
		if (cursor != null) {
			cursor.close();
		}
		return msgPart;
	}
	
	/**
	 * 根据消息id获取附件的本地存储路径，该附件实体值包含附件的路径filePath
	 * @update 2014年11月12日 下午8:29:18
	 * @param msgId
	 * @return
	 */
	public MsgPart getMsgPartPathByMsgId(String msgId) {
//		Uri uri = ContentUris.withAppendedId(Provider.MsgPartColumns.CONTENT_URI, msgId);
//		Cursor cursor = mContext.getContentResolver().query(uri, new String[] {Provider.MsgPartColumns.FILE_PATH}, null, null, null);
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		String[] projection = {Provider.MsgPartColumns.FILE_PATH, Provider.MsgPartColumns.FILE_THUMB_PATH, Provider.MsgPartColumns.DOWNLOADED};
		Cursor cursor = db.query(Provider.MsgPartColumns.TABLE_NAME, projection, Provider.MsgPartColumns.MSG_ID + " = ?", new String[]{msgId}, null, null, null);
		MsgPart msgPart = null;
		if (cursor != null && cursor.moveToFirst()) {
			msgPart = new MsgPart();
			msgPart.setMsgId(msgId);
			msgPart.setFilePath(cursor.getString(0));
			msgPart.setThumbPath(cursor.getString(1));
			msgPart.setDownloaded(cursor.getInt(2) == 1 ? true : false);
		}
		if (cursor != null) {
			cursor.close();
		}
		return msgPart;
	}
	
	/**
	 * 通过会话的id查询该会话下的聊天消息
	 * @update 2014年10月31日 上午11:23:57
	 * @param threadId 消息会话的id
	 * @param pageOffset 开始的查询索引
	 * @return
	 */
	public List<MsgInfo> getMsgInfosByThreadId(int threadId, int pageOffset) {
		List<MsgInfo> list = null;
//		String sortOrder = Provider.MsgInfoColumns.DEFAULT_SORT_ORDER + " limit " + Constants.PAGE_SIZE_MSG + " Offset " + pageOffset;
//		Cursor cursor = mContext.getContentResolver().query(Provider.MsgInfoColumns.CONTENT_URI, null, Provider.MsgInfoColumns.THREAD_ID + " = ?", new String[] {String.valueOf(threadId)}, sortOrder);
		String sortOrder = Provider.MsgInfoColumns.DEFAULT_SORT_ORDER;
		String limit = pageOffset + "," + Constants.PAGE_SIZE_MSG;
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		Cursor cursor = db.query(Provider.MsgInfoColumns.TABLE_NAME, Provider.MsgInfoColumns.DEFAULT_PROJECTION, Provider.MsgInfoColumns.THREAD_ID + " = ?", new String[]{String.valueOf(threadId)}, null, null, sortOrder, limit);
		if (cursor != null) {
			list = new ArrayList<>();
			while (cursor.moveToNext()) {
				MsgInfo msg = initMsgInfoByCursor(cursor, true, threadId, null);
				list.add(msg);
			}
			cursor.close();
			Collections.reverse(list);
		}
		return list;
	}

	/**
	 * 通过会话id查询该会话下的所有图片消息，包括视频消息
	 * @param threadId 会话id
	 * @param currentItem 当前浏览的item
	 * @author tiger
	 * @update 2015/11/28 10:38
	 * @version 1.0.0
	 * @return 图片消息的map,key中"currentPosition":当前浏览的图片在集合中的索引；"photoItems":图片消息的集合
	 */
	public Map<String, Object> getMsgImagesByThreadId(int threadId, PhotoItem currentItem) {
		Map<String, Object> map = null;
		List<PhotoItem> list = null;
		//时间的升序
		String order = Provider.MsgInfoColumns.REVERSAL_SORT_ORDER;
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		//_ID, MSG_ID, THREAD_ID, FROM_USER, TO_USER, CONTENT, SUBJECT, CREATIO_NDATE, IS_COMMING, IS_READ, MSG_TYPE, SEND_STATE
		String[] projection = {
				Provider.MsgInfoColumns._ID,
				Provider.MsgInfoColumns.MSG_ID,
				Provider.MsgInfoColumns.CREATIO_NDATE,
				Provider.MsgInfoColumns.MSG_TYPE,
		};
		int[] msgTypes = {
				MsgInfo.Type.IMAGE.ordinal(),
				MsgInfo.Type.VIDEO.ordinal()
		};
		int currentPosition = 0;
		Cursor cursor = db.query(Provider.MsgInfoColumns.TABLE_NAME, projection, Provider.MsgInfoColumns.THREAD_ID + " = ? AND " + Provider.MsgInfoColumns.MSG_TYPE + " in (" + SystemUtil.makePlaceholders(msgTypes.length) + ")", new String[]{String.valueOf(threadId), String.valueOf(MsgInfo.Type.IMAGE.ordinal()), String.valueOf(MsgInfo.Type.VIDEO.ordinal())}, null, null, order, null);
		if (cursor != null) {
			map = new HashMap<>();
			list = new ArrayList<>();
			int index = 0;
			while (cursor.moveToNext()) {
				PhotoItem item = new PhotoItem();
				String msgId = cursor.getString(cursor.getColumnIndex(Provider.MsgInfoColumns.MSG_ID));
				long date = cursor.getLong(cursor.getColumnIndex(Provider.MsgInfoColumns.CREATIO_NDATE));
				MsgInfo.Type msgType = Type.valueOf(cursor.getInt(cursor.getColumnIndex(Provider.MsgInfoColumns.MSG_TYPE)));

				MsgPart msgPart = getMsgPartByMsgId(msgId);

				if (currentItem != null) {
					if (msgId.equals(currentItem.getMsgId())) {
						currentPosition = index;
					}
				}

				item.setMsgId(msgId);
				item.setDownloadType(Constants.FILE_TYPE_ORIGINAL);
				item.setTime(date);
				if (MsgInfo.Type.VIDEO == msgType) {	//视频文件
					item.setFileType(FileItem.FileType.VIDEO);
				} else {	//图片
					item.setFileType(FileItem.FileType.IMAGE);
				}
				if (msgPart != null) {
					item.setThumbPath(msgPart.getThumbPath());
					item.setFilePath(msgPart.getFilePath());
					item.setFileName(msgPart.getFileName());
					item.setFileToken(msgPart.getFileToken());
					item.setNeedDownload(!msgPart.isDownloaded());
					item.setSize(msgPart.getSize());
					item.setMime(msgPart.getMimeType());

				}

				list.add(item);
				index ++;
			}
			cursor.close();

			map.put("currentPosition", currentPosition);
			map.put("photoItems", list);
		}
		return map;
	}
	
	/**
	 * 根据文件夹来获取对应文件夹里的所有图片
	 * @param photoItem 当前的图片
	 * @param loadAll 是否加载全部    
	 * @author huanghui1
	 * @update 2015/12/21 14:11
	 * @version: 0.0.1
	 * @return 返回文件夹中所有的图片的map，key中"currentPosition":当前浏览的图片在集合中的索引；"photoItems":图片消息的集合
	 */
	public Map<String, Object> getImagesByBucket(PhotoItem photoItem, boolean loadAll) {
		//TODO 未完成图片加载任务
		Map<String, Object> map = null;
		String bucketId = photoItem.getBucketId();
		String currentName = photoItem.getFileName();
		FileItem.FileType fileType = photoItem.getFileType();
		int currentPosition = 0;
		if (fileType == FileItem.FileType.IMAGE) {	//加载图片
			String[] projection = {
					MediaStore.Images.Media.DATA,
					MediaStore.Images.Media.SIZE,
					MediaStore.Images.Media.DATE_TAKEN,
					MediaStore.Images.Media.DISPLAY_NAME,
					MediaStore.Images.Media.MIME_TYPE
			};
			String selection = null;
			String[] selectionArgs = {
					/*"image/jpeg",
					"image/png",*/
					bucketId
			};
			if (!loadAll) {	//不加载全部
				selection = MediaStore.Images.Media.BUCKET_ID + " = ?";
			} else {	//加载全部
				selectionArgs = null;
			}
			
			Cursor cursor = mContext.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, /*MediaStore.Images.Media.MIME_TYPE + " in (" + SystemUtil.makePlaceholders(selectionArgs.length - 1) + ") AND " + */selection, selectionArgs, MediaStore.Images.Media.DATE_TAKEN + " DESC");
			if (cursor != null) {
				List<PhotoItem> list = new ArrayList<>();
				int index = 0;
				map = new HashMap<>();
				while (cursor.moveToNext()) {
					PhotoItem item = new PhotoItem();
					item.setFileType(photoItem.getFileType());
					item.setFilePath(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA)));
					item.setSize(cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media.SIZE)));
					item.setTime(cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)));
					item.setFileName(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)));
					item.setBucketId(bucketId);
					item.setMime(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)));

					if (item.getFileName().equals(currentName)) {	//当前的文件
						currentPosition = index;
					}

					list.add(item);

					index ++;
				}
				map.put("currentPosition", currentPosition);
				map.put("photoItems", list);
				
				cursor.close();
			}
		} else if (fileType == FileItem.FileType.VIDEO) {	//加载视频
			String[] projection = {
					MediaStore.Video.Media._ID,
					MediaStore.Video.Media.DATA,
					MediaStore.Video.Media.SIZE,
					MediaStore.Video.Media.DATE_TAKEN,
					MediaStore.Video.Media.MIME_TYPE,
					MediaStore.Video.Media.DISPLAY_NAME
			};
			String[] thumbProjection = {
					MediaStore.Video.Thumbnails.DATA
			};
			String[] selectionArgs = {
					bucketId
			};
			String selection = null;
			if (!loadAll) {	//不加载全部
				selection = MediaStore.Video.Media.BUCKET_ID + " = ?";
			} else {	//加载全部
				selectionArgs = null;
			}
			
			Cursor cursor = mContext.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, MediaStore.Video.Media.DATE_TAKEN + " DESC");
			if (cursor != null) {
				int index = 0;
				map = new HashMap<>();
				List<PhotoItem> list = new ArrayList<>();
				while (cursor.moveToNext()) {
					PhotoItem photo = new PhotoItem();
					photo.setFileType(FileItem.FileType.VIDEO);
					int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media._ID));
					photo.setFilePath(cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA)));
					photo.setSize(cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.SIZE)));
					photo.setTime(cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN)));
					photo.setBucketId(bucketId);
					photo.setMime(cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)));
					photo.setFileName(cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)));

					Cursor thumbCursor = mContext.getContentResolver().query(MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI, thumbProjection, MediaStore.Video.Thumbnails.VIDEO_ID + " = ?", new String[] {String.valueOf(id)}, null);
					if (thumbCursor != null && thumbCursor.moveToFirst()) {
						photo.setThumbPath(thumbCursor.getString(thumbCursor.getColumnIndex(MediaStore.Video.Thumbnails.DATA)));
					}
					if (thumbCursor != null) {
						thumbCursor.close();
					}
					
					if (photo.getFileName().equals(currentName)) {	//当前的视频
						currentPosition = index;
					}
					
					list.add(photo);
					
					index ++;
				}

				map.put("currentPosition", currentPosition);
				map.put("photoItems", list);

				cursor.close();

			}
		}
		return map;
	}

	/**
	 * 根据会话id查询该会的消息数量
	 * @update 2015年3月6日 下午4:55:31
	 * @param threadId 会话id
	 * @return 会话包含的消息数量
	 */
	public long getMsgCountByThreadId(int threadId) {
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		long count = 0;
		Cursor cursor = db.query(Provider.MsgInfoColumns.TABLE_NAME, new String[] {"count(*)"}, Provider.MsgInfoColumns.THREAD_ID + " = ?", new String[] {String.valueOf(threadId)}, null, null, null);
		if (cursor != null && cursor.moveToFirst()) {
			count = cursor.getLong(0);
		}
		if (cursor != null) {
			cursor.close();
		}
		return count;
	}
	
	/**
	 * 根据cursor来组装msginfo
	 * @update 2015年2月27日 上午10:29:30
	 * @param cursor
	 * @param loadAttach 是否加载消息的附件信息
	 * @param threadId 该消息所在的会话id,若该值<=0,则从数据库里获取
	 * @param msgId 该消息id,若该值为空,则从数据库里获取
	 * @return 组装后的消息对象
	 */
	private MsgInfo initMsgInfoByCursor(Cursor cursor, boolean loadAttach, int threadId, String msgId) {
		MsgInfo msg = new MsgInfo();
		if (msgId == null) {
			msg.setMsgId(cursor.getString(cursor.getColumnIndex(Provider.MsgInfoColumns.MSG_ID)));
		} else {
			msg.setMsgId(msgId);
		}
		if (threadId <= 0) {
			msg.setThreadID(cursor.getInt(cursor.getColumnIndex(Provider.MsgInfoColumns.THREAD_ID)));
		} else {
			msg.setThreadID(threadId);
		}
		msg.setFromUser(cursor.getString(cursor.getColumnIndex(Provider.MsgInfoColumns.FROM_USER)));
		msg.setToUser(cursor.getString(cursor.getColumnIndex(Provider.MsgInfoColumns.TO_USER)));
		msg.setContent(cursor.getString(cursor.getColumnIndex(Provider.MsgInfoColumns.CONTENT)));
		msg.setSubject(cursor.getString(cursor.getColumnIndex(Provider.MsgInfoColumns.SUBJECT)));
		msg.setCreationDate(cursor.getLong(cursor.getColumnIndex(Provider.MsgInfoColumns.CREATIO_NDATE)));
		msg.setComming(cursor.getInt(cursor.getColumnIndex(Provider.MsgInfoColumns.IS_COMMING)) == 0 ? false : true);
		msg.setRead(cursor.getInt(cursor.getColumnIndex(Provider.MsgInfoColumns.IS_READ)) == 0 ? false : true);
		msg.setMsgType(Type.valueOf(cursor.getInt(cursor.getColumnIndex(Provider.MsgInfoColumns.MSG_TYPE))));
		msg.setSendState(SendState.valueOf(cursor.getInt(cursor.getColumnIndex(Provider.MsgInfoColumns.SEND_STATE))));
		if (loadAttach) {
			Type msgType = msg.getMsgType();
			//如果消息不是文本类型，则加载附件
			if (msgType != Type.TEXT) {	//加载附件
				MsgPart msgPart = getMsgPartByMsgId(msg.getMsgId());
				msg.setMsgPart(msgPart);
			}
		}
		return msg;
	}
	
	/**
	 * 根据msgid获得消息对象
	 * @update 2014年11月6日 下午9:08:40
	 * @param msgId 消息id
	 * @return
	 */
	public MsgInfo getMsgInfoById(String msgId) {
		if (msgId == null) {
			return null;
		}
//		Uri uri = ContentUris.withAppendedId(Provider.MsgInfoColumns.CONTENT_URI, msgId);
//		Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null);
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		Cursor cursor = db.query(Provider.MsgInfoColumns.TABLE_NAME, Provider.MsgInfoColumns.DEFAULT_PROJECTION, Provider.MsgInfoColumns.MSG_ID + " = ?", new String[] {msgId}, null, null, null);
		MsgInfo msg = null;
		if (cursor != null && cursor.moveToFirst()) {
			msg = initMsgInfoByCursor(cursor, true, 0, msgId);
		}
		if (cursor != null) {
			cursor.close();
		}
		return msg;
	}
	
	/**
	 * 根据成员的id字符串获取会话的id
	 * @update 2015年3月10日 上午10:39:11
	 * @param ids 会话成员id的字符串，若为有多人时，则id之间使用";"隔开
	 * @return -1：表示没有查到该会话id
	 */
	public int getThreadIdByMemberIds(String ids) {
		if (TextUtils.isEmpty(ids)) {
			return -1;
		}
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		Cursor cursor = db.query(Provider.MsgThreadColumns.TABLE_NAME, new String[] {Provider.MsgThreadColumns._ID}, Provider.MsgThreadColumns.MEMBER_IDS + " = ?", new String[] {ids}, null, null, null);
		int threadId = -1;
		if (cursor != null && cursor.moveToFirst()) {	//有该会话
			threadId = cursor.getInt(0);
		}
		if (cursor != null) {
			cursor.close();
		}
		return threadId;
	}
	
	/**
	 * 根据会话成员的id字符串来获取对应的会话id
	 * @param db
	 * @param ids
	 * @return
	 * @update 2015年9月17日 上午11:37:04
	 */
	public int getThreadIdByMemberIds(SQLiteDatabase db, String ids) {
		if (TextUtils.isEmpty(ids)) {
			return -1;
		}
		if (db == null) {
			db = mChatDBHelper.getReadableDatabase();
		}
		Cursor cursor = db.query(Provider.MsgThreadColumns.TABLE_NAME, new String[] {Provider.MsgThreadColumns._ID}, Provider.MsgThreadColumns.MEMBER_IDS + " = ?", new String[] {ids}, null, null, null);
		int threadId = -1;
		if (cursor != null && cursor.moveToFirst()) {	//有该会话
			threadId = cursor.getInt(0);
		}
		if (cursor != null) {
			cursor.close();
		}
		return threadId;
	}
	
	/**
	 * 根据会话成员id查询会话的id
	 * @update 2014年11月4日 下午9:35:56
	 * @param memberIds 会话成员id
	 * @return
	 */
	public int getThreadIdByMembers(int... memberIds) {
		int tid = -1;
		//是否是多个人
		boolean multi = true;
		UserManager userManager = UserManager.getInstance();
		if (memberIds != null && memberIds.length > 0) {
			String strIds = null;
			if (memberIds.length == 1) {
				strIds = String.valueOf(memberIds[0]);
			} else {
				multi = false;
				StringBuilder sb = new StringBuilder();
				for (int id : memberIds) {
					sb.append(id).append(";");
				}
				sb.deleteCharAt(sb.length() - 1);
				strIds = sb.toString();
			}
//			Cursor cursor = mContext.getContentResolver().query(Provider.MsgThreadColumns.CONTENT_URI, new String[] {Provider.MsgThreadColumns._ID}, Provider.MsgThreadColumns.MEMBER_IDS + " = ?", new String[] {strIds}, null);
			tid = getThreadIdByMemberIds(strIds);
			if (tid <= 0) {	//没有该会话，则创建一个会话
				MsgThread msgThread = new MsgThread();
				List<User> list = new LinkedList<>();
				if (multi) {	//是多个人
					for (int id : memberIds) {
						User u = userManager.getUserById(id);
						list.add(u);
					}
				} else {
					User u = userManager.getUserById(memberIds[0]);
					list.add(u);
				}
				msgThread.setMembers(list);
				msgThread = createMsgThread(msgThread);
				tid = msgThread.getId();
			}
			/*SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
			Cursor cursor = db.query(Provider.MsgThreadColumns.TABLE_NAME, new String[] {Provider.MsgThreadColumns._ID}, Provider.MsgThreadColumns.MEMBER_IDS + " = ?", new String[] {strIds}, null, null, null);
			if (cursor != null && cursor.moveToFirst()) {	//有该会话
				tid = cursor.getInt(0);
			} else {	//没有改会话，则创建一个会话
				MsgThread msgThread = new MsgThread();
				List<User> list = new LinkedList<>();
				if (multi) {	//是多个人
					for (int id : memberIds) {
						User u = userManager.getUserById(id);
						list.add(u);
					}
				} else {
					User u = userManager.getUserById(memberIds[0]);
					list.add(u);
				}
				msgThread.setMembers(list);
				msgThread = createMsgThread(msgThread);
				tid = msgThread.getId();
			}
			if (cursor != null) {
				cursor.close();
			}*/
		}
		return tid;
	}
	
	
	/**
	 * 根据会话成员id查询会话的id,若会话不存在，则自动创建
	 * @update 2014年11月4日 下午9:35:56
	 * @param memAccounts 会话成员账号，即用户名
	 * @return
	 */
	public int getThreadIdByMembers(String... memAccounts) {
		return getThreadIdByMembers(true, memAccounts);
	}
	
	/**
	 * 根据会话成员id查询会话的id
	 * @update 2014年11月4日 下午9:35:56
	 * @param autoCreate 如果会话不存在，是否自动创建
	 * @param memAccounts 会话成员账号，即用户名
	 * @return
	 */
	public int getThreadIdByMembers(boolean autoCreate, String... memAccounts) {
		int tid = -1;
		//是否是多个人
		List<User> members = new LinkedList<>();
		UserManager userManager = UserManager.getInstance();
		if (memAccounts != null && memAccounts.length > 0) {
			String strIds = null;
			if (memAccounts.length == 1) {
				User u = userManager.getUserByUsername(memAccounts[0]);
				Log.d("----------user------" + (u == null ? "" : u.toString()));
				if (u != null) {
					strIds = String.valueOf(u.getId());
					members.add(u);
				}
			} else {
				StringBuilder sb = new StringBuilder();
				List<User> users = userManager.getUsersByNames(memAccounts);
				if (!SystemUtil.isEmpty(users)) {
					Log.d("-----多用户-----user------" + (users == null ? "" : users.toString()));
					for (User user : users) {
						if (user != null) {
							sb.append(user.getId()).append(";");
							members.add(user);
						}
					}
					sb.deleteCharAt(sb.length() - 1);
					strIds = sb.toString();
				}
			}
			if (strIds != null) {
				tid = getThreadIdByMemberIds(strIds);
				if (tid <= 0 && autoCreate) {	//没有该会话，则创建一个会话
					MsgThread msgThread = new MsgThread();
					msgThread.setMembers(members);
					msgThread = createMsgThread(msgThread);
					tid = msgThread.getId();
				}
			}
			/*Cursor cursor = mContext.getContentResolver().query(Provider.MsgThreadColumns.CONTENT_URI, new String[] {Provider.MsgThreadColumns._ID}, Provider.MsgThreadColumns.MEMBER_IDS + " = ?", new String[] {strIds}, null);
			if (cursor != null && cursor.moveToFirst()) {	//有该会话
				tid = cursor.getInt(0);
			} else {	//没有改会话，则创建一个会话
				MsgThread msgThread = new MsgThread();
				msgThread.setMembers(members);
				msgThread = createMsgThread(msgThread);
				tid = msgThread.getId();
			}
			if (cursor != null) {
				cursor.close();
			}*/
		}
		return tid;
	}
	
	/**
	 * 根据成员的用户名来获取该会话
	 * @param autoCreate 若该会话不存在，则是否自动创建
	 * @param memAccounts 会话成员，好友的账号数组
	 * @return
	 * @update 2015年9月16日 下午8:13:51
	 */
	public MsgThread getThreadByMembers(boolean autoCreate, String... memAccounts) {
		if (SystemUtil.isEmpty(memAccounts)) {
			return null;
		}
		MsgThread thread = null;
		UserManager userManager = UserManager.getInstance();
		List<User> members = new ArrayList<>();
		if (memAccounts.length == 1) {	//该会话只有一个成员，除自己外
			User u = userManager.getUserByUsername(memAccounts[0]);
			if (u != null) {
				members.add(u);
			}
		} else {
			members = userManager.getUsersByNames(memAccounts);
		}
		thread = getThreadByMembers(members);
		if (thread == null && autoCreate) {
			MsgThread msgThread = new MsgThread();
			msgThread.setMembers(members);
			thread = createMsgThread(msgThread);
		}
		
		if (thread != null) {
			updateThreadCache(thread);
		}
		
 		return thread;
	}
	
	/**
	 * 更新会话的缓存
	 * @param original 原始的会话
	 * @return
	 * @update 2015年9月17日 下午2:59:44
	 */
	public MsgThread updateThreadCache(MsgThread original) {
		//更新缓存
		MsgThread tThread = mThreadCache.get(original.getId());
		if (tThread != null) {
			tThread.setIcon(original.getIcon());
			tThread.setMembers(original.getMembers());
			tThread.setModifyDate(original.getModifyDate());
			tThread.setMsgThreadName(original.getMsgThreadName());
			tThread.setSnippetContent(original.getSnippetContent());
			tThread.setSnippetId(original.getSnippetId());
			tThread.setTop(original.isTop());
			tThread.setUnReadCount(original.getUnReadCount());
			tThread.setLastMsgInfo(original.getLastMsgInfo());
		} else {
			mThreadCache.put(original.getId(), original);
		}
		return original;
	}
	
	/**
	 * 将用户名的数组组装成以“;”分割的字符串
	 * @param memAccounts
	 * @return
	 * @update 2015年9月17日 上午11:24:01
	 */
	private String formatUsernames(String... memAccounts) {
		if (SystemUtil.isEmpty(memAccounts)) {
			return null;
		}
		if (memAccounts.length == 1) {
			return memAccounts[0];
		} else {
			StringBuilder sb = new StringBuilder();
			for (String account : memAccounts) {
				sb.append(account).append(";");
			}
			sb.deleteCharAt(sb.length() - 1);
			return sb.toString();
		}
	}
	
	
	/**
	 * 初始化msginfo的相关数据
	 * @update 2014年11月4日 下午10:43:06
	 * @param msgInfo
	 * @return
	 */
	private ContentValues initMsgInfoValues(MsgInfo msgInfo) {
		ContentValues values = new ContentValues();
		values.put(Provider.MsgInfoColumns.THREAD_ID, msgInfo.getThreadID());
		values.put(Provider.MsgInfoColumns.FROM_USER, msgInfo.getFromUser());
		values.put(Provider.MsgInfoColumns.TO_USER, msgInfo.getToUser());
		values.put(Provider.MsgInfoColumns.CONTENT, msgInfo.getContent());
		values.put(Provider.MsgInfoColumns.SUBJECT, msgInfo.getSubject());
		values.put(Provider.MsgInfoColumns.CREATIO_NDATE, msgInfo.getCreationDate());
		values.put(Provider.MsgInfoColumns.IS_COMMING, msgInfo.isComming() ? 1 : 0);
		values.put(Provider.MsgInfoColumns.IS_READ, msgInfo.isRead() ? 1 : 0);
		values.put(Provider.MsgInfoColumns.MSG_ID, msgInfo.getMsgId());
		Type type = msgInfo.getMsgType();
		if (type == null) {
			type = Type.TEXT;
		}
		values.put(Provider.MsgInfoColumns.MSG_TYPE, type.ordinal());
		SendState sendState = msgInfo.getSendState();
		if (sendState == null) {
			sendState = SendState.SUCCESS;
		}
		values.put(Provider.MsgInfoColumns.SEND_STATE, sendState.ordinal());
		return values;
	}
	
	/**
	 * 初始化msgPart的相关数据
	 * @update 2014年11月4日 下午10:43:06
	 * @param msgPart
	 * @return
	 */
	private ContentValues initMsgPartValues(MsgPart msgPart) {
		ContentValues values = new ContentValues();
		values.put(Provider.MsgPartColumns.MSG_ID, msgPart.getMsgId());
		values.put(Provider.MsgPartColumns.FILE_NAME, msgPart.getFileName());
		values.put(Provider.MsgPartColumns.FILE_PATH, msgPart.getFilePath());
		values.put(Provider.MsgPartColumns.SIZE, msgPart.getSize());
		values.put(Provider.MsgPartColumns.CREATION_DATE, msgPart.getCreationDate());
		values.put(Provider.MsgPartColumns.MIME_TYPE, msgPart.getMimeType());
		values.put(Provider.MsgPartColumns.FILE_TOKEN, msgPart.getFileToken());
		values.put(Provider.MsgPartColumns.DESC, msgPart.getDesc());
		values.put(Provider.MsgPartColumns.FILE_THUMB_PATH, msgPart.getThumbPath());
		values.put(Provider.MsgPartColumns.DOWNLOADED, msgPart.isDownloaded() ? 1 : 0);
		
		return values;
	}
	
	/**
	 * 添加附件信息
	 * @update 2014年11月5日 上午8:34:42
	 * @param msgPart
	 * @return
	 */
	public MsgPart addMsgPart(MsgPart msgPart) {
		if (msgPart == null) {
			return null;
		}
		ContentValues partValues = initMsgPartValues(msgPart);
		SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
		long id = db.insert(Provider.MsgPartColumns.TABLE_NAME, null, partValues);
		if (id > 0) {
			msgPart.setId((int) id);
			notifyObservers(Provider.MsgPartColumns.NOTIFY_FLAG, NotifyType.ADD, msgPart);
		}
		/*Uri uri = mContext.getContentResolver().insert(Provider.MsgPartColumns.CONTENT_URI, partValues);
		if (uri != null) {
			msgPart.setId(Integer.parseInt(uri.getLastPathSegment()));
		}*/
		return msgPart;
	}
	
	/**
	 * 根据消息id删除该条消息，该消息实体值包含msgId和msgType，默认不删除该消息对应的本地附件
	 * @update 2014年11月12日 下午8:08:44
	 * @param msgInfo
	 * @param msgThread
	 * @return
	 */
	public boolean deleteMsgInfoById(MsgInfo msgInfo, MsgThread msgThread) {
		return deleteMsgInfoById(msgInfo, msgThread, false);
	}
	
	/**
	 * 根据消息id删除该条消息，该消息实体值包含msgId和msgType
	 * @update 2015年2月26日 上午11:07:56
	 * @param msgInfo
	 * @param msgThread 该消息所在的会话，若会话为null，则不更新会话的摘要
	 * @param deleteAttach 是否删除该消息对应的附件
	 * @return
	 */
	public boolean deleteMsgInfoById(MsgInfo msgInfo, MsgThread msgThread, boolean deleteAttach) {
		if (msgInfo == null) {
			return false;
		}
		//查询该消息是否是该会话的最后一条消息，如果是最后一条消息，则更新该会话的最后一条消息
		boolean isLastMsg = false;
		if (msgThread != null) {
			isLastMsg = isSnippetMsgInThread(msgInfo.getMsgId(), msgThread.getId());
		}
//		int count = mContext.getContentResolver().delete(ContentUris.withAppendedId(Provider.MsgInfoColumns.CONTENT_URI, msgInfo.getId()), null, null);
		SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
		int count = db.delete(Provider.MsgInfoColumns.TABLE_NAME, Provider.MsgInfoColumns.MSG_ID + " = ?", new String[]{msgInfo.getMsgId()});
		if (count > 0) {	//删除消息成功
			if (isLastMsg) {	//是最后一条消息，就跟新会话的最后一条消息的摘要
				//获取最新的最后一条消息
				MsgInfo tempMsg = getLastMsgInThread(msgThread.getId());
				String snippetId = null;
				String snippetContent = null;
				int unReadCount = msgThread.getUnReadCount();
				if (tempMsg != null) {
					//重新设置该会话的最后一条消息的摘要
					snippetId = tempMsg.getMsgId();
//					snippetContent = getSnippetContentByMsgType(tempMsg);
					snippetContent = tempMsg.getSnippetContent();
					if (!tempMsg.isRead()) {	//该消息未读
						if (unReadCount > 0) {
							unReadCount -= 1;
							msgThread.setUnReadCount(unReadCount);
						}
					}
				}
				msgThread.setSnippetId(snippetId);
				msgThread.setSnippetContent(snippetContent);
				msgThread.setLastMsgInfo(tempMsg);
				updateSnippet(msgThread, true);
			}
			//触发器已经做了删除附件数据了
			/*if (deleteAttach) {
				Type msgType = msgInfo.getMsgType();
				if (MsgInfo.Type.TEXT != msgType) {	//有附件
					//删除附件
					deleteMsgPartByMsgId(msgInfo.getId());
				}
			}*/
			notifyObservers(Provider.MsgInfoColumns.NOTIFY_FLAG, NotifyType.DELETE, msgInfo);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 根据消息id的数组msgIds来删除对应的消息
	 * @param msgIds 消息id的数组
	 * @param unReadCount 这些消息中所包含的问题消息数量，如果为0，则都是已读消息
	 * @param msgThread 消息对应的会话
	 * @return 是否删除成功
	 * @author tiger
	 * @update 2015/11/7 11:10
	 * @version 1.0.0
	 */
	public boolean deleteMsgsByIds(String[] msgIds, int unReadCount, MsgThread msgThread) {
		boolean success = false;
		if (!SystemUtil.isEmpty(msgIds) && msgThread != null) {
			SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
			String sql = null;
			int len = msgIds.length;
			if (len == 1) {	//只有一条消息
				sql = " = ?";
			} else {
				sql = " in (" + SystemUtil.makePlaceholders(len) + ")";
			}
			int count = db.delete(Provider.MsgInfoColumns.TABLE_NAME, Provider.MsgInfoColumns.MSG_ID + sql, msgIds);
			if (count > 0) {	//消息删除成功
				//查询该消息是否是该会话的最后一条消息，如果是最后一条消息，则更新该会话的最后一条消息
				int threadId = msgThread.getId();
				String snippetId = getSnippetId(msgThread);
				if (!TextUtils.isEmpty(snippetId)) {	//该会话有最后一条消息的id
					boolean hasSnipppet = SystemUtil.hasObjInArray(msgIds, snippetId);
					if (hasSnipppet) {	//更新该会话最后一条消息的信息
						//获取该会话最后一条消息
						MsgInfo lastMsg = getLastMsgInThread(threadId);
						if (lastMsg != null) {	//有最后一条消息
							//更新会话的最后一条消息
							msgThread.setLastMsgInfo(lastMsg);
							msgThread.setSnippetContent(lastMsg.getSnippetContent());
							if (unReadCount > 0) {	//有未读消息
								int oldCount = msgThread.getUnReadCount();
								oldCount = oldCount - unReadCount;
								oldCount = oldCount < 0 ? 0 : oldCount;
								msgThread.setUnReadCount(oldCount);
							}
							msgThread.setSnippetId(lastMsg.getMsgId());
						} else {
							//已经没有最后一条消息了
							msgThread.setLastMsgInfo(null);
							msgThread.setSnippetContent(null);
							msgThread.setUnReadCount(0);
							msgThread.setSnippetId(null);
						}
						updateSnippet(msgThread, true);
					}
				}
				success = true;
			}
		}
		return success;
	}

	/**
	 * 根据消息id删除消息的附件，无需删除本地磁盘的文件
	 * @update 2014年11月12日 下午8:14:14
	 * @param msgId
	 */
	public void deleteMsgPartByMsgId(String msgId) {
		MsgPart msgPart = getMsgPartPathByMsgId(msgId);
		if (msgPart != null) {	//有附件
			//查询该消息对应的附件
//			mContext.getContentResolver().delete(ContentUris.withAppendedId(Provider.MsgPartColumns.CONTENT_URI, msgId), null, null);
			SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
			db.delete(Provider.MsgPartColumns.TABLE_NAME, Provider.MsgPartColumns.MSG_ID + " = ?", new String[] {msgId});
			notifyObservers(Provider.MsgPartColumns.NOTIFY_FLAG, NotifyType.DELETE, msgPart);
			//int count =
			//无需删除本地文件
//			if (count > 0) {	//删除成功
//				//则删除本地附件
//				String filePath = msgPart.getFilePath();
//				SystemUtil.deleteFile(filePath);
//			}
		}
	}
	
	/**
	 * 根据该消息id判断该消息是否是该会话的最后一条消息
	 * @update 2015年2月27日 上午10:04:55
	 * @param msgId 消息id
	 * @param threadId 会话id
	 * @return
	 */
	public boolean isSnippetMsgInThread(String msgId, int threadId) {
		boolean flag = false;
		MsgThread tThread = mThreadCache.get(threadId);
		if (tThread != null) {
			String snippetId = tThread.getSnippetId();
			if (msgId != null) {
				if (snippetId != null) {
					return snippetId.equals(msgId);
				} else {
					return false;
				}
			} else {
				if (snippetId != null) {
					return false;
				} else {
					return true;
				}
			}
		}
//		Cursor cursor = mContext.getContentResolver().query(Provider.MsgThreadColumns.CONTENT_URI, new String[] {Provider.MsgThreadColumns._ID}, Provider.MsgThreadColumns._ID + " = ? AND " + Provider.MsgThreadColumns.SNIPPET_ID + " = ?", new String[] {String.valueOf(threadId), String.valueOf(msgId)}, null);
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		Cursor cursor = db.query(Provider.MsgThreadColumns.TABLE_NAME, new String[]{"count(*)"}, Provider.MsgThreadColumns._ID + " = ? AND " + Provider.MsgThreadColumns.SNIPPET_ID + " = ?", new String[]{String.valueOf(threadId), msgId}, null, null, null);
		if (cursor != null && cursor.moveToFirst()) {
			flag = cursor.getLong(0) > 0;
		}
		if (cursor != null) {
			cursor.close();
		}
		return flag;
	}

	/**
	 * 获取会话最后一条消息的id
	 * @param msgThread 会话
	 * @return 最后一条消息的id，没有没有，则返回null
	 * @author tiger
	 * @update 2015/11/7 11:23
	 * @version 1.0.0
	 */
	public String getSnippetId(MsgThread msgThread) {
		if (msgThread == null) {
			return null;
		}
		String snippetId = msgThread.getSnippetId();
		//是否从数据库中查询
		boolean selectDb = false;
		if (TextUtils.isEmpty(snippetId)) {	//没有则从缓存查询
			MsgThread tThread = mThreadCache.get(msgThread.getId());
			if (tThread != null) {
				snippetId = tThread.getSnippetId();
				if (TextUtils.isEmpty(snippetId)) {
					selectDb = true;
				}
			} else {
				selectDb = true;
			}
			if (selectDb) {	//从数据库中查询
				SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
				Cursor cursor = db.query(Provider.MsgThreadColumns.TABLE_NAME, new String[]{Provider.MsgThreadColumns.SNIPPET_ID}, Provider.MsgThreadColumns._ID + " = ?", new String[]{String.valueOf(msgThread.getId())}, null, null, null);
				if (cursor != null && cursor.moveToFirst()) {
					snippetId = cursor.getString(0);
				}
				if (cursor != null) {
					cursor.close();
				}
			}
		}
		return snippetId;
	}


	/**
	 * 根据会话id获取该会话中最后的一条消息
	 * @update 2015年2月27日 上午10:14:33
	 * @param threadId 会话id
	 * @return 该会话中最后的一条消息
	 */
	public MsgInfo getLastMsgInThread(int threadId) {
//		String sortOrder = Provider.MsgInfoColumns.REVERSAL_SORT_ORDER + " limit 1 offset 0";	//取第一条记录
//		Cursor cursor = mContext.getContentResolver().query(Provider.MsgInfoColumns.CONTENT_URI, null, Provider.MsgInfoColumns.THREAD_ID + " = ?", new String[] {String.valueOf(threadId)}, sortOrder);
		SQLiteDatabase db = mChatDBHelper.getReadableDatabase();
		Cursor cursor = db.query(Provider.MsgInfoColumns.TABLE_NAME, Provider.MsgInfoColumns.DEFAULT_PROJECTION, Provider.MsgInfoColumns.THREAD_ID + " = ?", new String[]{String.valueOf(threadId)}, null, null, Provider.MsgInfoColumns.DEFAULT_SORT_ORDER, "1");
		MsgInfo msg = null;
		if (cursor != null && cursor.moveToFirst()) {
			msg = initMsgInfoByCursor(cursor, true, threadId, null);
		}
		if (cursor != null) {
			cursor.close();
		}
		return msg;
	}
	
	/**
	 * 添加一条消息记录
	 * @update 2014年11月4日 下午10:41:46
	 * @param msgInfo 消息记录
	 * @param refreshUi 是否刷新界面   
	 * @return
	 */
	public MsgInfo addMsgInfo(MsgInfo msgInfo, boolean refreshUi) {
		if (msgInfo == null) {
			return null;
		}
		ContentValues infoVaules = initMsgInfoValues(msgInfo);
		SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
		long id = db.insert(Provider.MsgInfoColumns.TABLE_NAME, Provider.MsgInfoColumns.DEAULT_NULL_COLUMN, infoVaules);
		if (id > 0) {
			msgInfo.setId((int) id);
			switch (msgInfo.getMsgType()) {
			case IMAGE:	//图片
			case VOICE:	//语音
			case AUDIO:	//音频
			case FILE:	//文件
			case VIDEO:	//视频
			case VCARD:	//电子名片
			case LOCATION:	//地理位置
				//添加附件信息
				MsgPart msgPart = msgInfo.getMsgPart();
				msgPart.setMsgId(msgInfo.getMsgId());
				msgPart = addMsgPart(msgPart);
				msgInfo.setMsgPart(msgPart);
				break;

			default:
				break;
			}
			if (refreshUi) {
				notifyObservers(Provider.MsgInfoColumns.NOTIFY_FLAG, NotifyType.ADD, msgInfo);
			}
		}
//		Uri uri = mContext.getContentResolver().insert(Provider.MsgInfoColumns.CONTENT_URI, infoVaules);
//		if (uri != null) {
//			String msgId = uri.getLastPathSegment();
//			msgInfo.setId(Integer.parseInt(msgId));
//			switch (msgInfo.getMsgType()) {
//			case IMAGE:	//图片
//			case VOICE:	//语音
//			case AUDIO:	//音频
//			case FILE:	//文件
//			case VIDEO:	//视频
//			case VCARD:	//电子名片
//			case LOCATION:	//地理位置
//				//添加附件信息
//				MsgPart msgPart = msgInfo.getMsgPart();
//				msgPart.setMsgId(msgInfo.getId());
//				msgPart = addMsgPart(msgPart);
//				msgInfo.setMsgPart(msgPart);
//				break;
//
//			default:
//				break;
//			}
//		}
		return msgInfo;
	}
	
	/**
	 * 批量添加消息对象，利用事务，提高效率，该方法已废弃，直接循环添加就行了{@link MsgManager#addMsgInfo(MsgInfo, boolean)}
	 * @update 2015年2月27日 下午5:58:00
	 * @param list 要添加的消息列表
	 * @return 添加的消息所属会话id的集合
	 */
	@Deprecated
	public List<Integer> addBatchMsgInfo(List<MsgInfo> list) {
		List<Integer> threadIdList = null;
		if (SystemUtil.isNotEmpty(list)) {
			int len = list.size();
			ContentValues[] arrayValues = new ContentValues[len];
			ArrayList<ContentProviderOperation> ops = new ArrayList<>();
			for (int i = 0; i < len; i++) {
				MsgInfo msgInfo = list.get(i);
				if (msgInfo != null) {
					arrayValues[i] = initMsgInfoValues(msgInfo);
					ops.add(ContentProviderOperation.newInsert(Provider.MsgInfoColumns.CONTENT_URI).withValues(arrayValues[i]).build());
				}
			}
			//批量添加：方法1
//			count = mContext.getContentResolver().bulkInsert(Provider.MsgInfoColumns.CONTENT_URI, arrayValues);
			//批量添加：方法2
			try {
				ContentProviderResult[] resultes = mContext.getContentResolver().applyBatch(Provider.AUTHORITY_MSG, ops);
				if (SystemUtil.isNotEmpty(resultes)) {
					Set<Integer> tempIdSet = new HashSet<>();
					int count = resultes.length;
					for (int i = 0; i < count; i++) {
						MsgInfo msgInfo = list.get(i);
						if (msgInfo != null) {
							tempIdSet.add(msgInfo.getThreadID());
							Uri uri = resultes[i].uri;
							if (uri != null) {
								int msgId = Integer.parseInt(uri.getLastPathSegment());
								msgInfo.setId(msgId);
								if (msgInfo.getMsgType() != Type.TEXT) {	//非文本消息就添加附件
									//添加附件信息
									MsgPart msgPart = msgInfo.getMsgPart();
									msgPart.setMsgId(msgInfo.getMsgId());
									msgPart = addMsgPart(msgPart);
									msgInfo.setMsgPart(msgPart);
								}
							}
						}
						
					}
					if (SystemUtil.isNotEmpty(tempIdSet)) {
						threadIdList = new ArrayList<>();
						threadIdList.addAll(tempIdSet);
					}
				}
			} catch (RemoteException | OperationApplicationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return threadIdList;
	}
	
	/**
	 * 更新该会话的最后一条消息记录, 默认不更新数据库，只更新缓存
	 * @update 2014年11月7日 下午8:57:42
	 * @param msgThread
	 * @return
	 */
	public MsgThread updateSnippet(MsgThread msgThread) {
		return updateSnippet(msgThread, false);
	}
	
	/**
	 * 更新该会话的最后一条消息记录, 默认不更新数据库，只更新缓存
	 * @param msgThread
	 * @param updateDb 是否跟新数据库
	 * @return
	 * @update 2015年9月18日 下午6:54:10
	 */
	public MsgThread updateSnippet(MsgThread msgThread, boolean updateDb) {
		//有了触发器，则不用再操作数据库了
		if (updateDb) {
			
//		ContentValues values = initMsgThreadVaule(msgThread);
//		Uri uri = ContentUris.withAppendedId(Provider.MsgThreadColumns.CONTENT_URI, msgThread.getId());
//		mContext.getContentResolver().update(uri, values, null, null);
			ContentValues values = new ContentValues();
			values.put(Provider.MsgThreadColumns.MODIFY_DATE, msgThread.getModifyDate());
			values.put(Provider.MsgThreadColumns.UNREAD_COUNT, msgThread.getUnReadCount());
			values.put(Provider.MsgThreadColumns.SNIPPET_CONTENT, msgThread.getSnippetContent());
			values.put(Provider.MsgThreadColumns.SNIPPET_ID, msgThread.getSnippetId());
			SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
			db.update(Provider.MsgThreadColumns.TABLE_NAME, values, Provider.MsgThreadColumns._ID + " = ?", new String[] {String.valueOf(msgThread.getId())});
		}
		
		MsgThread tThread = mThreadCache.get(msgThread.getId());
		if (tThread != null) {
			tThread.setModifyDate(msgThread.getModifyDate());
			tThread.setUnReadCount(msgThread.getUnReadCount());
			tThread.setSnippetId(msgThread.getSnippetId());
			tThread.setSnippetContent(msgThread.getSnippetContent());
			tThread.setLastMsgInfo(msgThread.getLastMsgInfo());
		} else {
			mThreadCache.put(msgThread.getId(), msgThread);
		}
		
		notifyObservers(Provider.MsgThreadColumns.NOTIFY_FLAG, NotifyType.UPDATE, msgThread);
		return msgThread;
	}
	
	/**
	 * 更新消息信息
	 * @update 2014年11月6日 下午10:09:06
	 * @param msgInfo
	 * @return
	 */
	public MsgInfo updateMsgInfo(MsgInfo msgInfo) {
		ContentValues values = initMsgInfoValues(msgInfo);
//		Uri uri = ContentUris.withAppendedId(Provider.MsgInfoColumns.CONTENT_URI, msgInfo.getId());
//		mContext.getContentResolver().update(uri, values, null, null);
		SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
		db.update(Provider.MsgInfoColumns.TABLE_NAME, values, Provider.MsgInfoColumns.MSG_ID + " = ?", new String[] {msgInfo.getMsgId()});
		notifyObservers(Provider.MsgInfoColumns.NOTIFY_FLAG, NotifyType.UPDATE, msgInfo);
		return msgInfo;
	}
	
	/**
	 * 更新消息的发送状态
	 * @update 2015年3月10日 上午11:18:47
	 * @param msgInfo
	 * @return
	 */
	public MsgInfo updateMsgSendStatus(MsgInfo msgInfo) {
//		ContentValues values = initMsgInfoValues(msgInfo);
//		Uri uri = ContentUris.withAppendedId(Provider.MsgInfoColumns.CONTENT_URI, msgInfo.getId());
//		mContext.getContentResolver().update(uri, values, null, null);
		SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		SendState sendState = msgInfo.getSendState();
		if (sendState == null) {
			sendState = SendState.SUCCESS;
		}
		values.put(Provider.MsgInfoColumns.SEND_STATE, sendState.ordinal());
		db.update(Provider.MsgInfoColumns.TABLE_NAME, values, Provider.MsgInfoColumns.MSG_ID + " = ?", new String[] {msgInfo.getMsgId()});
		notifyObservers(Provider.MsgInfoColumns.NOTIFY_FLAG, NotifyType.UPDATE, msgInfo);
		return msgInfo;
	}

	/**
	 * 通知界面更新上传进度
	 * @param uploadInfo 消息上传的实体
	 */
	public void updateMsgUploadInfo(MsgUploadInfo uploadInfo) {
		if (uploadInfo != null) {
			notifyObservers(Provider.NotifyColumns.NOTIFY_MSG_UPLOAD_FLAG, NotifyType.UPDATE, uploadInfo);
		}
	}
	
	/**
	 * 更新消息的未读/已读状态
	 * @update 2015年3月10日 上午11:16:30
	 * @param msgInfo
	 * @return
	 */
	public MsgInfo updateMsgReadStatus(MsgThread msgThread, MsgInfo msgInfo) {
//		Uri uri = ContentUris.withAppendedId(Provider.MsgInfoColumns.CONTENT_URI, msgInfo.getId());
//		mContext.getContentResolver().update(uri, values, null, null);
		ContentValues values = new ContentValues();
		values.put(Provider.MsgInfoColumns.IS_READ, msgInfo.isRead() ? 1 : 0);
		SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
		db.update(Provider.MsgInfoColumns.TABLE_NAME, values, Provider.MsgInfoColumns.MSG_ID + " = ?", new String[] {msgInfo.getMsgId()});
		notifyObservers(Provider.MsgInfoColumns.NOTIFY_FLAG, NotifyType.UPDATE, msgInfo);
		
		if (msgThread != null) {
			updateMsgUnreadCount(db, msgThread);
		}
		return msgInfo;
	}
	
	/**
	 * 更新会话的未读数量
	 * @param db
	 * @param msgThread 会话
	 * @return
	 * @update 2015年9月17日 下午4:10:49
	 */
	public MsgThread updateMsgUnreadCount(SQLiteDatabase db, MsgThread msgThread) {
		//触发器做了处理，不用在这里处理了
//		if (db == null) {
//			db = mChatDBHelper.getWritableDatabase();
//		}
//		int unReadCount = msgThread.getUnReadCount();
//		unReadCount -= 1;
//		unReadCount = unReadCount < 0 ? 0 : unReadCount;
//		ContentValues values = new ContentValues();
//		values.put(Provider.MsgThreadColumns.UNREAD_COUNT, unReadCount);
//		db.update(Provider.MsgThreadColumns.TABLE_NAME, values, Provider.MsgThreadColumns._ID + " = ?", new String[] {String.valueOf(msgThread.getId())});
		
		MsgThread tThread = mThreadCache.get(msgThread.getId());
		if (tThread != null) {
			int unReadCount = msgThread.getUnReadCount();
			unReadCount -= 1;
			unReadCount = unReadCount < 0 ? 0 : unReadCount;
			tThread.setUnReadCount(unReadCount);
		}
		
		notifyObservers(Provider.MsgThreadColumns.NOTIFY_FLAG, NotifyType.UPDATE, msgThread);
		return msgThread;
	}
	
	/**
	 * 更新会话的基本信息
	 * @update 2014年11月8日 上午10:44:48
	 * @param msgThread
	 * @return
	 */
	public MsgThread updateMsgThread(MsgThread msgThread) {
		String memberIds = getMemberIds(msgThread.getMembers());
		ContentValues values = initMsgThreadVaule(msgThread, memberIds);
//		Uri uri = ContentUris.withAppendedId(Provider.MsgThreadColumns.CONTENT_URI, msgThread.getId());
//		mContext.getContentResolver().update(uri, values, null, null);
		SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
		db.update(Provider.MsgThreadColumns.TABLE_NAME, values, Provider.MsgThreadColumns._ID + " = ?", new String[] {String.valueOf(msgThread.getId())});
		
		//更新缓存
		updateThreadCache(msgThread);
		
		notifyObservers(Provider.MsgThreadColumns.NOTIFY_FLAG, NotifyType.UPDATE, msgThread);
		return msgThread;
	}
	
	/**
	 * 根据用户来更新该用户对应的会话名称
	 * @param user
	 * @param db 数据库对象，可由外面传入
	 * @param notifyObserver 是否通知观察者刷新界面
	 * @return
	 * @update 2015年7月30日 上午9:51:21
	 */
	public boolean updateMsgThreadNameByUser(User user, SQLiteDatabase db, boolean notifyObserver) {
		if (db == null) {
			db = mChatDBHelper.getWritableDatabase();
		}
		ContentValues values = new ContentValues(1);
		values.put(Provider.MsgThreadColumns.MSG_THREAD_NAME, user.getName());
		long rowId = db.update(Provider.MsgThreadColumns.TABLE_NAME, values, Provider.MsgThreadColumns.MEMBER_IDS + " = ?", new String[] {String.valueOf(user.getId())});
		if (rowId > 0) {
			//根据用户id查询对应的会话，群组不适合
			int threadId = getThreadIdByMemberIds(db, String.valueOf(user.getId()));
			if (threadId != -1) {
				MsgThread tThread = mThreadCache.get(threadId);
				if (tThread != null) {
					tThread.setMsgThreadName(user.getName());
				}
			}
			if (notifyObserver) {
				notifyObservers(Provider.MsgThreadColumns.NOTIFY_FLAG, NotifyType.UPDATE, null);
			}
			Log.d("---updateMsgThreadNameByUser---user--" + user + "-----notifyObserver----" + notifyObserver + "---成功---");
			return true;
		} else {
			Log.w("---updateMsgThreadNameByUser---user--" + user + "-----notifyObserver----" + notifyObserver + "---失败---");
			return false;
		}
	}
	
	/**
	 * 根据消息类型来设置会话最后一条消息的内容
	 * @update 2014年11月20日 下午3:07:22
	 * @param msgType 消息类型
	 * @param msgInfo 消息实体
	 * @return
	 */
	public String getSnippetContentByMsgType(Type msgType, MsgInfo msgInfo) {
		String snippetContent = null;
		switch (msgType) {
		case TEXT:
			snippetContent = msgInfo.getSnippetContent();
			break;
		case IMAGE:
			snippetContent = mContext.getString(R.string.msg_thread_snippet_content_image);
			break;
		case AUDIO:
			snippetContent = mContext.getString(R.string.msg_thread_snippet_content_audio);
			break;
		case FILE:
			snippetContent = mContext.getString(R.string.msg_thread_snippet_content_file);
			break;
		case LOCATION:
			snippetContent = mContext.getString(R.string.msg_thread_snippet_content_location);
			break;
		case VIDEO:
			snippetContent = mContext.getString(R.string.msg_thread_snippet_content_video);
			break;
		case VCARD:
			snippetContent = mContext.getString(R.string.msg_thread_snippet_content_vcard);
			break;
		case VOICE:	//语音
			snippetContent = mContext.getString(R.string.msg_thread_snippet_content_voice, msgInfo.getContent());
			break;
		default:
			snippetContent = msgInfo.getSnippetContent();
			break;
		}
		return snippetContent;
	}
	
	/**
	 * 根据消息类型来设置会话最后一条消息的内容
	 * @update 2015年2月27日 上午10:53:59
	 * @param msgInfo 消息实体
	 * @return
	 */
	@Deprecated
	public String getSnippetContentByMsgType(MsgInfo msgInfo) {
		if (msgInfo == null) {
			return null;
		}
		Type msgType = msgInfo.getMsgType();
		return getSnippetContentByMsgType(msgType, msgInfo);
	}
	
	/**
	 * 根据图片获取其缩略图
	 * @update 2014年11月21日 下午7:56:39
	 * @param imagePath
	 * @return
	 */
	public String getImageThumbPath(String imagePath) {
		String path = null;
		Cursor cursor = mContext.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[] {MediaStore.Images.Media._ID,}, MediaStore.Images.Media.DATA + " = ?", new String[] {imagePath}, null);
		if (cursor != null && cursor.moveToFirst()) {
			int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media._ID));
			Cursor thumbCursor = mContext.getContentResolver().query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, new String[] {MediaStore.Images.Thumbnails.DATA}, MediaStore.Images.Thumbnails.IMAGE_ID + " = ?", new String[] {String.valueOf(id)}, null);
			if (thumbCursor != null && thumbCursor.moveToFirst()) {
				path = thumbCursor.getString(thumbCursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA));
			}
			if (thumbCursor != null) {
				thumbCursor.close();
			}
		}
		if (cursor != null) {
			cursor.close();
		}
		return path;
	}
	
	/**
	 * 获得视频文件的缩略图路径
	 * @update 2014年11月21日 下午5:44:14
	 * @return
	 */
	public String getVideoThumbPath(String audioPath) {
		String path = null;
		Cursor cursor = mContext.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, new String[] {MediaStore.Video.Media._ID,}, MediaStore.Video.Media.DATA + " = ?", new String[] {audioPath}, null);
		if (cursor != null && cursor.moveToFirst()) {
			int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media._ID));
			Cursor thumbCursor = mContext.getContentResolver().query(MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI, new String[] {MediaStore.Video.Thumbnails.DATA}, MediaStore.Video.Thumbnails.VIDEO_ID + " = ?", new String[] {String.valueOf(id)}, null);
			if (thumbCursor != null && thumbCursor.moveToFirst()) {
				path = thumbCursor.getString(thumbCursor.getColumnIndex(MediaStore.Video.Thumbnails.DATA));
			}
			if (thumbCursor != null) {
				thumbCursor.close();
			}
		}
		if (cursor != null) {
			cursor.close();
		}
		return path;
	}
	
	/**
	 * 获取视频文件的缩略图和原始文件地址
	 * @param uri 文件的数据库路径，格式为：content://media/external/video/media/4795
	 * @return 返回视频文件的路径数组，[0]:原始文件的绝对路径，[1]:视频文件的缩略图
	 * @author tiger
	 * @update 2015/11/1 10:59
	 * @version 1.0.0
	 */
	public String[] getVideoThumbPath(Uri uri) {
		String thumbPath = null;
		String path = null;
		String[] array = new String[2];
		if (uri != null) {
			String id = uri.getLastPathSegment();
			//根据ID查询缩略图地址
			Cursor thumbCursor = mContext.getContentResolver().query(MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Video.Thumbnails.DATA}, MediaStore.Video.Thumbnails.VIDEO_ID + " = ?", new String[]{id}, null);
			if (thumbCursor != null && thumbCursor.moveToFirst()) {
				thumbPath = thumbCursor.getString(0);
			}
			if (thumbCursor != null) {
				thumbCursor.close();
			}
			Cursor cursor = mContext.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, new String[] {MediaStore.Video.Media.DATA,}, MediaStore.Video.Media._ID + " = ?", new String[] {id}, null);
			if (cursor != null && cursor.moveToFirst()) {
				path = cursor.getString(0);
			}
			if (cursor != null) {
				cursor.close();
			}
		}
		array[0] = path;
		array[1] = thumbPath;
		return array;
	}

	/**
	 * 根据uri获取对应视频的本地绝对路径
	 * @param uri 图片的数据库uri,如：content://media/external/video/media/4795
	 * @return 本地视频文件的绝对路径
	 */
	public String getVideoPath(Uri uri) {
		String path = null;
		if (uri != null) {
			String id = uri.getLastPathSegment();
			Cursor cursor = mContext.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, new String[] {MediaStore.Video.Media.DATA,}, MediaStore.Video.Media._ID + " = ?", new String[] {id}, null);
			if (cursor != null && cursor.moveToFirst()) {
				path = cursor.getString(0);
			}
			if (cursor != null) {
				cursor.close();
			}
		}
		return path;
	}
	
	/**
	 * 在本地获取所有的图片，图片的类型为image/jpeg或者image/png
	 * @update 2014年11月13日 下午7:18:29
	 * @param isImage 加载的是图片还是视频
	 * @return
	 */
	public Album getAlbum(boolean isImage) {
		Album album = null;
		if (isImage) {
			String[] projection = {
					MediaStore.Images.Media.DATA,
					MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
					MediaStore.Images.Media.SIZE,
					MediaStore.Images.Media.DATE_TAKEN,
					MediaStore.Images.Media.BUCKET_ID,
					MediaStore.Images.Media.MIME_TYPE,
					MediaStore.Images.Media.DISPLAY_NAME
			};
//			String[] selectionArgs = {
//				"image/jpeg",
//				"image/png"
//			};
			Cursor cursor = mContext.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null/*MediaStore.Images.Media.MIME_TYPE + " in (" + SystemUtil.makePlaceholders(selectionArgs.length) + ")"*/, /*selectionArgs*/null, MediaStore.Images.Media.DATE_TAKEN + " DESC");
			if (cursor != null) {
				album = new Album();
				List<PhotoItem>  list = new ArrayList<>();
				Map<String, List<PhotoItem>>  map = new HashMap<>();
				while (cursor.moveToNext()) {
					PhotoItem photo = new PhotoItem();
					photo.setFileType(FileItem.FileType.IMAGE);
					photo.setFilePath(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA)));
					String parentName = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME));
					photo.setSize(cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media.SIZE)));
					photo.setTime(cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)));
					photo.setBucketId(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)));
					photo.setMime(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)));
					photo.setFileName(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)));
					if (TextUtils.isEmpty(parentName)) {
						File file = new File(photo.getFilePath()).getParentFile();
						if (file != null) {
							parentName = file.getName();
						} else {
							parentName = "/";
						}
					}
					if (map.containsKey(parentName)) {
						map.get(parentName).add(photo);
					} else {
						List<PhotoItem> temp = new ArrayList<>();
						temp.add(photo);
						map.put(parentName, temp);
					}
					list.add(photo);
				}
				cursor.close();
				
				album.setmPhotos(list);
				album.setFolderMap(map);
			}
		} else {
			String[] projection = {
					MediaStore.Video.Media._ID,
					MediaStore.Video.Media.DATA,
					MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
					MediaStore.Video.Media.SIZE,
					MediaStore.Video.Media.DATE_TAKEN,
					MediaStore.Video.Media.BUCKET_ID,
					MediaStore.Video.Media.MIME_TYPE,
					MediaStore.Video.Media.DISPLAY_NAME
			};
			String[] thumbProjection = {
					MediaStore.Video.Thumbnails.DATA
			};
			Cursor cursor = mContext.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null, MediaStore.Video.Media.DATE_TAKEN + " DESC");
			if (cursor != null) {
				album = new Album();
				List<PhotoItem>  list = new ArrayList<>();
				Map<String, List<PhotoItem>>  map = new HashMap<>();
				while (cursor.moveToNext()) {
					PhotoItem photo = new PhotoItem();
					photo.setFileType(FileItem.FileType.VIDEO);
					int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media._ID));
					photo.setFilePath(cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA)));
					String parentName = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME));
					photo.setSize(cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.SIZE)));
					photo.setTime(cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN)));
					photo.setBucketId(cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID)));
					photo.setMime(cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)));
					photo.setFileName(cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)));
					
					Cursor thumbCursor = mContext.getContentResolver().query(MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI, thumbProjection, MediaStore.Video.Thumbnails.VIDEO_ID + " = ?", new String[] {String.valueOf(id)}, null);
					if (thumbCursor != null && thumbCursor.moveToFirst()) {
						photo.setThumbPath(thumbCursor.getString(thumbCursor.getColumnIndex(MediaStore.Video.Thumbnails.DATA)));
					}
					if (thumbCursor != null) {
						thumbCursor.close();
					}
					if (TextUtils.isEmpty(parentName)) {
						File file = new File(photo.getFilePath()).getParentFile();
						if (file != null) {
							parentName = file.getName();
						} else {
							parentName = "/";
						}
					}
					if (map.containsKey(parentName)) {
						map.get(parentName).add(photo);
					} else {
						List<PhotoItem> temp = new ArrayList<>();
						temp.add(photo);
						map.put(parentName, temp);
					}
					list.add(photo);
				}
				
				cursor.close();
				
				album.setmPhotos(list);
				album.setFolderMap(map);
			}
		}
		
		return album;
	}
	
	/**
	 * 获得音乐的列表
	 * @update 2014年11月22日 下午2:58:01
	 * @return
	 */
	public List<AudioItem> getAudioList() {
		List<AudioItem> list = null;
		String[] projection = {
				MediaStore.Audio.Media.TITLE,
				MediaStore.Audio.Media.DISPLAY_NAME,
				MediaStore.Audio.Media.DATA,
				MediaStore.Audio.Media.ARTIST,
				MediaStore.Audio.Media.SIZE,
				MediaStore.Audio.Media.DURATION
		};
		
		Cursor cursor = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null, null, MediaStore.Audio.Media.TITLE + " ASC");
		if (cursor != null) {
			list = new ArrayList<>();
			while (cursor.moveToNext()) {
				String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
				String fileName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
				String filePath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
				String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
				long size = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE));
				int duration = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
				
				AudioItem item = new AudioItem();
				item.setTitle(title);
				item.setArtist(artist);
				item.setFileName(fileName);
				item.setFilePath(filePath);
				item.setDuration(duration);
				item.setSize(size);
				
				list.add(item);
			}
			cursor.close();
		}
		return list;
	}
	
	/**
	 * 设置消息信息
	 * @update 2014年11月18日 上午11:32:33
	 * @param msgInfo
	 * @param photoItem
	 * @return
	 */
	public MsgInfo setMsgInfo(MsgInfo msgInfo, PhotoItem photoItem) {
		return setMsgInfo(msgInfo, photoItem, true);
	}

	/**
	 * 设置消息信息
	 * @param msgInfo
	 * @param photoItem
	 * @return
	 */
	public MsgInfo setMsgInfo(MsgInfo msgInfo, PhotoItem photoItem, boolean isImage) {
//		MsgThread mt = new MsgThread();
//		mt.setId(msgInfo.getThreadID());
		
		long time = System.currentTimeMillis();
		
		MsgPart part = new MsgPart();
		String filePath = photoItem.getFilePath();
		String fileName = SystemUtil.getFilename(filePath);
		
		part.setFileName(fileName);
		part.setFilePath(photoItem.getFilePath());
		//TODO 文件类型匹配待做
		//获得文件的后缀名，不包含"."，如mp3
		String subfix = SystemUtil.getFileSubfix(fileName);
		String mimeType = MimeUtils.guessMimeTypeFromExtension(subfix);
		part.setMimeType(mimeType);
		part.setMsgId(msgInfo.getMsgId());
		part.setSize(photoItem.getSize());
		part.setCreationDate(time);
		part.setDownloaded(true);

		if (isImage) {	//图片消息
			String thumbName = SystemUtil.generateChatThumbAttachFilename(time);
			String savePath = ImageUtil.generateThumbImage(filePath, msgInfo.getThreadID(), thumbName);
			if (savePath != null) {
				part.setThumbName(thumbName);
				part.setThumbPath(savePath);
			}
		}
		
//		part = msgManager.addMsgPart(part);
		
		msgInfo.setMsgPart(part);
		msgInfo.setCreationDate(time);
		return msgInfo;
	}
	
	/**
	 * 设置消息信息
	 * @update 2014年11月18日 上午11:32:33
	 * @param msgInfo
	 * @param file
	 * @return
	 */
	public MsgInfo setMsgInfo(MsgInfo msgInfo, File file) {
//		MsgThread mt = new MsgThread();
//		mt.setId(msgInfo.getThreadID());
		
		MsgPart part = new MsgPart();
		part.setFileName(file.getName());
		part.setFilePath(file.getAbsolutePath());
		//TODO 文件类型匹配待做
		//获得文件的后缀名，不包含"."，如mp3
		String subfix = SystemUtil.getFileSubfix(part.getFileName());
		String mimeType = MimeUtils.guessMimeTypeFromExtension(subfix);
		part.setMimeType(mimeType);
		part.setMsgId(msgInfo.getMsgId());
		part.setSize(file.length());
		part.setCreationDate(System.currentTimeMillis());
		part.setDownloaded(true);
		
//		part = msgManager.addMsgPart(part);
		
		msgInfo.setMsgPart(part);
		msgInfo.setCreationDate(System.currentTimeMillis());
		return msgInfo;
	}
	
	/**
	 * 根据选择的图片列表创建消息列表
	 * @update 2014年11月20日 下午7:41:36
	 * @param msgInfo 对应的聊天消息
	 * @param selectList 选择的图片集合
	 * @param originalImage 是否需要发送原图
	 * @return
	 */
	public ArrayList<MsgInfo> getMsgInfoListByPhotos(MsgInfo msgInfo, List<PhotoItem> selectList, boolean originalImage) {
		final ImageLoader imageLoader = ImageLoader.getInstance();
		final ArrayList<MsgInfo> msgList = new ArrayList<>();
		for (final PhotoItem photoItem : selectList) {
			try {
				String filePath = photoItem.getFilePath();
				if (!SystemUtil.isFileExists(filePath)) {
					continue;
				}
				String fileUri = Scheme.FILE.wrap(filePath);
				final MsgInfo mi = (MsgInfo) msgInfo.clone();
				//生成消息的id
				mi.generateMsgId();
				if (originalImage) {	//原图发送
					msgList.add(setMsgInfo(mi, photoItem));
				} else {
					//现在本地发送目录里查找看有没之前发送的文件
					//现在磁盘缓存里查找文件
					File sendFile = DiskCacheUtils.findInCache(fileUri, imageLoader.getDiskCache());
					if (sendFile == null || !sendFile.exists() || sendFile.length() == 0) {	//文件不存在
						List<Bitmap> bitmapList = MemoryCacheUtils.findCachedBitmapsForImageUri(fileUri, imageLoader.getMemoryCache());
						if (!SystemUtil.isEmpty(bitmapList)) {	//内存缓存里找到了
							Bitmap bitmap = bitmapList.get(0);
							if (bitmap == null) {	//重新加载图片
								Bitmap loadedImage = ImageUtil.loadImageThumbnailsSync(fileUri);
								if (loadedImage != null) {
									if (SystemUtil.saveBitmap(imageLoader, loadedImage, photoItem)) {
										msgList.add(setMsgInfo(mi, photoItem));
									}
								}
							} else {
								if (SystemUtil.saveBitmap(imageLoader, bitmap, photoItem)) {
									msgList.add(setMsgInfo(mi, photoItem));
								}
							}
						} else {	//内存缓存里没有找到，则重新加载
							Bitmap loadedImage = ImageUtil.loadImageThumbnailsSync(fileUri);
							if (loadedImage != null) {
								if (SystemUtil.saveBitmap(imageLoader, loadedImage, photoItem)) {
									msgList.add(setMsgInfo(mi, photoItem));
								}
							}
						}
					} else {	//本地缓存文件存在
						msgList.add(setMsgInfo(mi, photoItem));
					}
				}
				
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
		}
		return msgList;
	}

	/**
	 * 根据选择的视频列表创建消息列表
	 * @param msgInfo
	 * @param selectList
	 * @return
	 */
	public ArrayList<MsgInfo> getMsgInfoListByVideos(MsgInfo msgInfo, List<PhotoItem> selectList) {
		final ArrayList<MsgInfo> msgList = new ArrayList<>();
		for (final PhotoItem photoItem : selectList) {
			try {
				String filePath = photoItem.getFilePath();
				if (!SystemUtil.isFileExists(filePath)) {
					continue;
				}
				final MsgInfo mi = (MsgInfo) msgInfo.clone();
				msgList.add(setMsgInfo(mi, photoItem, false));
				
			} catch (CloneNotSupportedException e) {
				Log.e(e.getMessage());
			}
		}
		return msgList;
	}
	
	/**
	 * 更新会话的置顶状态
	 * @update 2015年2月27日 下午3:54:28
	 * @param msgThread
	 * @return
	 */
	public boolean updateMsgThreadTop(MsgThread msgThread) {
		if (msgThread == null) {
			return false;
		}
//		Uri uri = ContentUris.withAppendedId(Provider.MsgThreadColumns.CONTENT_URI, msgThread.getId());
		SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
		ContentValues values = new ContentValues(1);
		values.put(Provider.MsgThreadColumns.IS_TOP, msgThread.isTop() ? 1 : 0);
//		int count = mContext.getContentResolver().update(uri, values, null, null);
		int count = db.update(Provider.MsgThreadColumns.TABLE_NAME, values, Provider.MsgThreadColumns._ID + " = ?", new String[] {String.valueOf(msgThread.getId())});
		if (count > 0) {
			
			MsgThread tThread = mThreadCache.get(msgThread.getId());
			if (tThread != null) {
				tThread.setTop(msgThread.isTop());
			}
			
			notifyObservers(Provider.MsgThreadColumns.NOTIFY_FLAG, NotifyType.UPDATE, msgThread);
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * 根据选择的文件来创建对应的消息信息列表
	 * @update 2014年11月21日 下午10:39:19
	 * @param msgInfo
	 * @param selectList
	 * @return
	 */
	public ArrayList<MsgInfo> getMsgInfoListByFileItems(MsgInfo msgInfo, List<FileItem> selectList) {
		ArrayList<MsgInfo> msgList = new ArrayList<>();
		for (FileItem fileItem : selectList) {
			File file = fileItem.getFile();
			if (file == null || !file.exists()) {
				continue;
			}
			try {
				final MsgInfo mi = (MsgInfo) msgInfo.clone();
				//生成消息的id
				mi.generateMsgId();
				msgList.add(setMsgInfo(mi, file));
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
		}
		return msgList;
	}
	
	/**
	 * 更新消息附件的下载状态
	 * @update 2015年6月9日 下午7:56:30
	 * @param msgPart 该附件实体
	 * @param refreshUi 是否需要刷新界面
	 * @return 是否更新成功
	 */
	public boolean updateMsgPartDownload(MsgPart msgPart, boolean refreshUi) {
		SQLiteDatabase db = mChatDBHelper.getWritableDatabase();
		ContentValues values = new ContentValues(1);
		values.put(Provider.MsgPartColumns.DOWNLOADED, msgPart.isDownloaded() ? 1 : 0);
		int count = db.update(Provider.MsgPartColumns.TABLE_NAME, values, Provider.MsgPartColumns.MSG_ID + " = ?", new String[] {msgPart.getMsgId()});
		if (count > 0 && refreshUi) {
			Log.d("-----updateMsgPartDownload---success----msgPart----" + msgPart);
			notifyObservers(Provider.MsgPartColumns.NOTIFY_FLAG, NotifyType.UPDATE, msgPart);
			return true;
		} else {
			Log.d("-----updateMsgPartDownload---failed----msgPart----" + msgPart);
			return false;
		}
	}
	
	/**
	 * 根据所选择的音频文件来设置msginfo
	 * @update 2014年11月22日 下午5:46:54
	 * @param msgInfo
	 * @param audioItem
	 * @return
	 */
	public MsgInfo getMsgInfoByAudio(MsgInfo msgInfo, AudioItem audioItem) {
		String filePath = audioItem.getFilePath();
		if (SystemUtil.isFileExists(filePath)) {
			File file = new File(filePath);
			return setMsgInfo(msgInfo, file);
		} else {
			return null;
		}
	}
	
	/**
	 * 根据目录列出文件集合
	 * @update 2014年11月21日 下午3:20:59
	 * @param dir
	 * @return
	 */
	public List<FileItem> listFileItems(File dir) {
		List<FileItem> list = null;
		try {
			if (dir.isDirectory() && dir.canRead()) {
				/*
				 * 注：使用list.listFiles在文件名是乱码的情况下jni底层会报错，如文件名是GBK的中文名，
				 * 但jni底层处理该字符串默认会按照UTF-8的编码去转换，这样会导致严重报错，程序崩溃。所以，这里不使用次方法来列表文件目录，而是使用自定义的jni来列表文件目录
				 */
				/*String rootPath = dir.getAbsolutePath();
				ArrayList<String> fileNames = NativeUtil.listFileNames(rootPath);
				if (!SystemUtil.isEmpty(fileNames)) {	//首选自定义的jni获取目录列表
					list = new ArrayList<>();
					for (String filename : fileNames) {
						File file = new File(dir, filename);
						FileItem fileItem = SystemUtil.getFileItem(file);
						list.add(fileItem);
					}
					Collections.sort(list, new FileItem());
				} else {*/
					File[] files = dir.listFiles();
					if (!SystemUtil.isEmpty(files)) {
						list = new ArrayList<>();
						for (File file : files) {
							FileItem fileItem = SystemUtil.getFileItem(file);
							list.add(fileItem);
						}
						Collections.sort(list, new FileItem());
					}
//				}
			}
		} catch (Exception e) {
			Log.e(e.getMessage());
		}
		return list;
	}
}
