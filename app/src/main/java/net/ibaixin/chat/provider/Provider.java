package net.ibaixin.chat.provider;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 *
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年10月13日 上午11:31:24
 */
public class Provider {
	public static final String AUTHORITY_USER = "net.ibaixin.chat.provider.user";
	public static final String AUTHORITY_MSG = "net.ibaixin.chat.provider.msg";
	public static final String AUTHORITY_PERSONAL = "net.ibaixin.chat.provider.personal";
	public static final String AUTHORITY_NEW_FRIEND = "net.ibaixin.chat.provider.newFriend";
	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.example.chat";
	public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.example.chat";

	/**
	 * 用户的表字段
	 * @author huanghui1
	 * @update 2014年10月13日 上午11:36:35
	 */
	public static final class UserColumns implements BaseColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY_USER + "/users");
		public static final Uri CONTENT_SEARCH_URI = Uri.parse("content://" + AUTHORITY_USER + "/users/search");
		
		public static final String USERNAME_INDEX = "username_idx";
		
		/*
		 * 删除用户的同时删除用户对应的电子名片触发器
		 */
		public static final String EVENT_DELETE_VCARD = "delete_vcard_event";
		
		/**
		 * 通知标识，主要是区分别的通知内容
		 */
		public static final int NOTIFY_FLAG = 1;
		
		public static final String TABLE_NAME = "t_user";
        public static final String DEFAULT_SORT_ORDER = "sortLetter ASC";
        
        public static final String USERNAME = "username";
        public static final String EMAIL = "email";
        public static final String NICKNAME = "nickname";
        public static final String PHONE = "phone";
        public static final String RESOURCE = "resource";
        public static final String STATUS = "status";
        public static final String MODE = "mode";
        public static final String FULLPINYIN = "fullPinyin";
        public static final String SHORTPINYIN = "shortPinyin";
        public static final String SORTLETTER = "sortLetter";
        
        public static final String[] DEFAULT_PROJECTION = {_ID, USERNAME, EMAIL, NICKNAME, PHONE, RESOURCE, STATUS, MODE, FULLPINYIN, SHORTPINYIN, SORTLETTER};
        
        public static final String DEAULT_NULL_COLUMN = USERNAME;
	}
	
	/**
	 * 个人信息的表字段
	 * @author coolpad
	 *
	 */
	public static final class PersonalColums implements BaseColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY_PERSONAL + "/personals");
		
		public static final String TABLE_NAME = "t_personal";
        public static final String DEFAULT_SORT_ORDER = "username ASC";
        
        public static final String USERNAME = "username";
        public static final String PASSWORD = "password";
        public static final String NICKNAME = "nickname";
        public static final String REALNAME = "realName";
        public static final String EMAIL = "email";
        public static final String PHONE = "phone";
        public static final String RESOURCE = "resource";
        public static final String STATUS = "status";
        public static final String MODE = "mode";
        public static final String STREET = "street";
        public static final String CITY = "city";
        public static final String CITY_ID = "cityId";
        public static final String PROVINCE = "province";
        public static final String PROVINCE_ID = "provinceId";
        public static final String COUNTRY = "country";
        public static final String COUNTRY_ID = "countryId";
        public static final String ZIPCODE = "zipCode";
        public static final String ICONPATH = "iconPath";
        public static final String THUMBPATH = "thumbPath";
        public static final String ICONHASH = "iconHash";
        public static final String MIMETYPE = "mimeType";
        public static final String SEX = "sex";
        public static final String DESC = "desc";
	}
	
	/**
	 * 用户名片表字段
	 * @author huanghui1
	 * @update 2014年10月13日 上午11:39:41
	 */
	public static final class UserVcardColumns implements BaseColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY_USER + "/userVcards");
		
		/**
		 * 通知标识，主要是区分别的通知内容
		 */
		public static final int NOTIFY_FLAG = 2;
		
		public static final String USERID_INDEX = "user_id_idx";
		
		public static final String TABLE_NAME = "t_user_vcard";
        public static final String DEFAULT_SORT_ORDER = "userId ASC";
        
        public static final String USERID = "userId";
        public static final String NICKNAME = "nickname";
        public static final String REALNAME = "realName";
        public static final String EMAIL = "email";
        public static final String STREET = "street";
        public static final String CITY = "city";
        public static final String PROVINCE = "province";
        public static final String COUNTRY = "country";
        public static final String ZIPCODE = "zipCode";
        public static final String MOBILE = "mobile";
        public static final String ICONPATH = "iconPath";
        public static final String THUMBPATH = "thumbPath";
        public static final String ICONHASH = "iconHash";
        public static final String MIMETYPE = "mimeType";
        public static final String SEX = "sex";
        public static final String DESC = "desc";
        
        public static final String[] DEFAULT_PROJECTION = {_ID, USERID, NICKNAME, REALNAME, EMAIL, STREET, CITY, PROVINCE, COUNTRY, ZIPCODE, MOBILE, ICONPATH, THUMBPATH, ICONHASH, MIMETYPE, SEX, DESC};
        
        public static final String DEAULT_NULL_COLUMN = USERID;
	}
	
	/**
	 * 聊天消息的字段
	 * @author huanghui1
	 * @update 2014年10月30日 下午3:42:57
	 */
	public static final class MsgInfoColumns implements BaseColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY_MSG + "/msgInfos");
		
		/*
		 * 更新未读信息数量的触发器
		 */
		public static final String EVENT_COUNT_UNREAD_MSG = "count_unread_msg_event";
		
		/*
		 * 删除消息附件的触发器
		 */
		public static final String EVENT_DELETE_MSG_PART = "delete_msg_part_event";
		
		/*
		 * 更新回话摘要的触发器
		 */
		public static final String EVENT_UPDATE_THREAD_SNIPPET = "update_thread_snippet_event";

		/**
		 * msgId字段的索引
		 */
		public static final String MSGID_INDEX = "msg_id_idx";
		
		/**
		 * 通知标识，主要是区分别的通知内容
		 */
		public static final int NOTIFY_FLAG = 3;
		
		public static final String TABLE_NAME = "t_msg_info";
        public static final String DEFAULT_SORT_ORDER = "creationDate DESC";	//按时间降序，后面的消息先查出来
        public static final String REVERSAL_SORT_ORDER = "creationDate ASC";	//默认按时间升序，后面的消息后查出来
        
		public static final String MSG_ID = "msgId";
        public static final String THREAD_ID = "threadID";
        public static final String FROM_USER = "fromUser";
        public static final String TO_USER = "toUser";
        public static final String CONTENT = "content";
        public static final String SUBJECT = "subject";
        public static final String CREATIO_NDATE = "creationDate";
        public static final String IS_COMMING = "isComming";
        public static final String IS_READ = "isRead";
        public static final String MSG_TYPE = "msgType";
        public static final String SEND_STATE = "sendState";
        
		public static final String[] DEFAULT_PROJECTION = {_ID, MSG_ID, THREAD_ID, FROM_USER, TO_USER, CONTENT, SUBJECT, CREATIO_NDATE, IS_COMMING, IS_READ, MSG_TYPE, SEND_STATE};
		
		public static final String DEAULT_NULL_COLUMN = THREAD_ID;
	}
	
	/**
	 * 聊天消息附件的字段
	 * @author huanghui1
	 * @update 2014年10月30日 下午4:05:48
	 */
	public static final class MsgPartColumns implements BaseColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY_MSG + "/msgParts");
		public static final String TABLE_NAME = "t_msg_part";
		public static final String DEFAULT_SORT_ORDER = "msgId DESC";

		/**
		 * msgId字段的索引
		 */
		public static final String MSGID_PART_INDEX = "msg_id_part_idx";
		
		/**
		 * 通知标识，主要是区分别的通知内容
		 */
		public static final int NOTIFY_FLAG = 4;
		
		public static final String MSG_ID = "msgId";
		public static final String FILE_NAME = "fileName";
		public static final String FILE_PATH = "filePath";
		public static final String SIZE = "size";
		public static final String MIME_TYPE = "mimeType";
		public static final String CREATION_DATE = "creationDate";
		public static final String FILE_TOKEN = "file_token";
		public static final String DESC= "desc";
		public static final String FILE_THUMB_PATH = "fileThumbPath";
		public static final String DOWNLOADED = "downloaded";
		
		public static final String[] DEFAULT_PROJECTION = {_ID, MSG_ID, FILE_NAME, FILE_PATH, CREATION_DATE, MIME_TYPE, SIZE, FILE_TOKEN, DESC, FILE_THUMB_PATH, DOWNLOADED};
		
		public static final String DEAULT_NULL_COLUMN = MSG_ID;
	}
	
	/**
	 * 聊天的会话列表字段
	 * @author huanghui1
	 * @update 2014年10月30日 下午4:05:48
	 */
	public static final class MsgThreadColumns implements BaseColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY_MSG + "/msgThreads");
		public static final String TABLE_NAME = "t_msg_thread";
		public static final String DEFAULT_SORT_ORDER = "isTop DESC, modifyDate DESC";	//最后修改时间的降序
		
		/**
		 * 通知标识，主要是区分别的通知内容
		 */
		public static final int NOTIFY_FLAG = 5;
		
		public static final String MSG_THREAD_NAME = "msgThreadName";
		public static final String UNREAD_COUNT = "unReadCount";
		public static final String MODIFY_DATE = "modifyDate";
		public static final String SNIPPET_ID = "snippetId";
		public static final String SNIPPET_CONTENT = "snippetContent";
		public static final String MEMBER_IDS = "memberIds";
		public static final String IS_TOP = "isTop";	//是否置顶
		
		public static final String[] DEFAULT_PROJECTION = {_ID, MSG_THREAD_NAME, UNREAD_COUNT, MODIFY_DATE, SNIPPET_ID, SNIPPET_CONTENT, MEMBER_IDS, IS_TOP};
		
		public static final String DEAULT_NULL_COLUMN = MSG_THREAD_NAME;
	}
	
	/**
	 * 新的朋友信息列表
	 * @author Administrator
	 * @update 2014年11月9日 下午2:57:53
	 * @version 1.0.0
	 */
	public static final class NewFriendColumns implements BaseColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY_NEW_FRIEND + "/newFirends");
		public static final String TABLE_NAME = "t_new_friend";
		public static final String DEFAULT_SORT_ORDER = "creationDate DESC";	//最后修改时间的降序
		
		/**
		 * 通知标识，主要是区分别的通知内容
		 */
		public static final int NOTIFY_FLAG = 6;
		
		public static final String USER_ID = "userId";
		public static final String FRIEND_STATUS = "friendStatus";
		public static final String CONTENT = "content";
		public static final String CREATION_DATE = "creationDate";
		public static final String FROM_USER = "from_user";
		public static final String TO_USER = "to_user";
		public static final String ICON_HASH = "iconHash";
		public static final String ICON_PATH = "iconPath";
		
		public static final String[] DEFAULT_PROJECTION = {_ID, USER_ID, FRIEND_STATUS, CONTENT, CREATION_DATE, FROM_USER, TO_USER, ICON_HASH, ICON_PATH};
	}
}
