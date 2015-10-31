package net.ibaixin.chat.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import net.ibaixin.chat.provider.Provider;

/**
 * 数据库创建
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年10月13日 上午11:26:25
 */
public class ChatDatabaseHelper extends SQLiteOpenHelper {
	public static final String DB_NAME = "ibaixin_chat.db";
	private static final int DB_VERSION = 9;
	
	public ChatDatabaseHelper(Context context) {
		this(context, null);
	}
	
	public ChatDatabaseHelper(Context context, String dbPath) {
		super(new DataBaseContext(context, dbPath), DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		//创建用户表
		db.execSQL("CREATE TABLE " + Provider.UserColumns.TABLE_NAME + " ("
				+ Provider.UserColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ Provider.UserColumns.USERNAME + " TEXT UNIQUE NOT NULL, "
				+ Provider.UserColumns.NICKNAME + " TEXT, "
				+ Provider.UserColumns.EMAIL + " TEXT, "
				+ Provider.UserColumns.PHONE + " TEXT, "
				+ Provider.UserColumns.RESOURCE + " TEXT, "
				+ Provider.UserColumns.STATUS + " TEXT, "
				+ Provider.UserColumns.MODE + " TEXT, "
				+ Provider.UserColumns.FULLPINYIN + " TEXT, "
				+ Provider.UserColumns.SHORTPINYIN + " TEXT, "
				+ Provider.UserColumns.SORTLETTER + " TEXT);");
		
		//创建用户名片表
		db.execSQL("CREATE TABLE " + Provider.UserVcardColumns.TABLE_NAME + " ("
				+ Provider.UserVcardColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ Provider.UserVcardColumns.USERID + " INTEGER UNIQUE NOT NULL, "
				+ Provider.UserVcardColumns.NICKNAME + " TEXT, "
				+ Provider.UserVcardColumns.REALNAME + " TEXT, "
				+ Provider.UserVcardColumns.MOBILE + " TEXT, "
				+ Provider.UserVcardColumns.EMAIL + " TEXT, "
				+ Provider.UserVcardColumns.COUNTRY + " TEXT, "
				+ Provider.UserVcardColumns.PROVINCE + " TEXT, "
				+ Provider.UserVcardColumns.STREET + " TEXT, "
				+ Provider.UserVcardColumns.CITY + " TEXT, "
				+ Provider.UserVcardColumns.ZIPCODE + " TEXT, "
				+ Provider.UserVcardColumns.ICONPATH + " TEXT, "
				+ Provider.UserVcardColumns.THUMBPATH + " TEXT, "
				+ Provider.UserVcardColumns.ICONHASH + " TEXT, "
				+ Provider.UserVcardColumns.MIMETYPE + " TEXT, "
				+ Provider.UserVcardColumns.SEX + " INTEGER DEFAULT 0, "
				+ Provider.UserVcardColumns.DESC + " TEXT);");
		
		//创建聊天消息表
		db.execSQL("CREATE TABLE " + Provider.MsgInfoColumns.TABLE_NAME + " ("
				+ Provider.MsgInfoColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ Provider.MsgInfoColumns.MSG_ID + " TEXT UNIQUE NOT NULL, "
				+ Provider.MsgInfoColumns.THREAD_ID + " INTEGER NOT NULL, "
				+ Provider.MsgInfoColumns.FROM_USER + " TEXT NOT NULL, "
				+ Provider.MsgInfoColumns.TO_USER + " TEXT NOT NULL, "
				+ Provider.MsgInfoColumns.CONTENT + " TEXT, "
				+ Provider.MsgInfoColumns.SUBJECT + " TEXT, "
				+ Provider.MsgInfoColumns.CREATIO_NDATE + " LONG, "
				+ Provider.MsgInfoColumns.IS_COMMING + " INTEGER, "
				+ Provider.MsgInfoColumns.IS_READ + " INTEGER, "
				+ Provider.MsgInfoColumns.MSG_TYPE + " INTEGER, "
				+ Provider.MsgInfoColumns.SEND_STATE + " INTEGER);");
		
		//创建聊天消息的附件表
		db.execSQL("CREATE TABLE " + Provider.MsgPartColumns.TABLE_NAME + " ("
				+ Provider.MsgPartColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ Provider.MsgPartColumns.MSG_ID + " TEXT UNIQUE NOT NULL, "
				+ Provider.MsgPartColumns.FILE_NAME + " TEXT NOT NULL, "
				+ Provider.MsgPartColumns.FILE_PATH + " TEXT NOT NULL, "
				+ Provider.MsgPartColumns.SIZE + " LONG, "
				+ Provider.MsgPartColumns.CREATION_DATE + " LONG, "
				+ Provider.MsgPartColumns.MIME_TYPE + " TEXT, "
				+ Provider.MsgPartColumns.FILE_TOKEN + " TEXT UNIQUE, "
				+ Provider.MsgPartColumns.DESC + " TEXT, "
				+ Provider.MsgPartColumns.FILE_THUMB_PATH + " TEXT, "
				+ Provider.MsgPartColumns.DOWNLOADED + " INTEGER);");
		
		//创建聊天会话表
		db.execSQL("CREATE TABLE " + Provider.MsgThreadColumns.TABLE_NAME + " ("
				+ Provider.MsgThreadColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ Provider.MsgThreadColumns.MSG_THREAD_NAME + " TEXT, "
				+ Provider.MsgThreadColumns.UNREAD_COUNT + " INTEGER, "
				+ Provider.MsgThreadColumns.MODIFY_DATE + " LONG, "
				+ Provider.MsgThreadColumns.SNIPPET_ID + " TEXT, "
				+ Provider.MsgThreadColumns.SNIPPET_CONTENT + " TEXT, "
				+ Provider.MsgThreadColumns.MEMBER_IDS + " TEXT, "
				+ Provider.MsgThreadColumns.IS_TOP + " INTEGER DEFAULT 0);");
		
		//创建新的朋友列表
		db.execSQL("CREATE TABLE " + Provider.NewFriendColumns.TABLE_NAME + " ("
				+ Provider.NewFriendColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ Provider.NewFriendColumns.USER_ID + " INTEGER UNIQUE NOT NULL, "
				+ Provider.NewFriendColumns.FRIEND_STATUS + " INTEGER DEFAULT 0, "
				+ Provider.NewFriendColumns.CONTENT + " TEXT, "
				+ Provider.NewFriendColumns.FROM_USER + " TEXT UNIQUE NOT NULL, "
				+ Provider.NewFriendColumns.TO_USER + " TEXT UNIQUE NOT NULL, "
				+ Provider.NewFriendColumns.ICON_HASH + " TEXT, "
				+ Provider.NewFriendColumns.ICON_PATH + " TEXT, "
				+ Provider.NewFriendColumns.CREATION_DATE + " LONG);");
		
		//创建用户电子名片的索引
		db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS " + Provider.UserVcardColumns.USERID_INDEX + " ON " + Provider.UserVcardColumns.TABLE_NAME + "(" + Provider.UserVcardColumns.USERID + ");");
		
		//添加用户表的username索引
		createUsernameIndex(db);
		
		//添加消息表的msgId字段的索引
		createMsgIdIndex(db);
		
		//添加消息附件表的msgId字段的索引
		createMsgIdPartIndex(db);
		
		//创建t_msg_info更新isRead=1时，更新t_msg_thread表的unReadCount-1
		createCountUnreadMsgTrigger(db);
		
		createUpdateThreadSnippetTrigger(db);
		
		//创建删除消息后，同时删除其对应附件的触发器
		createDeleteMsgPartTrigger(db);
		
		//创建删除用户后，同时删除用户对应的电子名片,会话的触发器
		createDeleteUserTrigger(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
		switch (newVersion) {
		case 8:
			//添加用户表的username索引
			createUsernameIndex(db);
			
			//创建t_msg_info更新isRead=1时，更新t_msg_thread表的unReadCount-1
			createCountUnreadMsgTrigger(db);
			
			//创建更新会话摘要的触发器，当添加一条新信息时，则会执行此触发器
			createUpdateThreadSnippetTrigger(db);
			
			//创建删除消息后，同时删除其对应附件的触发器
			createDeleteMsgPartTrigger(db);
			
			//创建删除用户后，同时删除用户对应的电子名片,会话的触发器
			createDeleteUserTrigger(db);
			break;
		default:
			//删除电子名片的索引
			db.execSQL("DROP INDEX IF EXISTS " + Provider.UserVcardColumns.USERID_INDEX);  
			db.execSQL("DROP INDEX IF EXISTS " + Provider.UserColumns.USERNAME_INDEX);  
			db.execSQL("DROP INDEX IF EXISTS " + Provider.MsgInfoColumns.MSGID_INDEX);  
			db.execSQL("DROP INDEX IF EXISTS " + Provider.MsgPartColumns.MSGID_PART_INDEX);  
			
			//删除会话更新未读信息数量的触发器
			db.execSQL("DROP TRIGGER IF EXISTS " + Provider.MsgInfoColumns.EVENT_COUNT_UNREAD_MSG);  
			//删除更新会话摘要的触发器，当添加一条新信息时，则会执行此触发器
			db.execSQL("DROP TRIGGER IF EXISTS " + Provider.MsgInfoColumns.EVENT_UPDATE_THREAD_SNIPPET);  
			//删除消息后，同时删除其对应附件的触发器
			db.execSQL("DROP TRIGGER IF EXISTS " + Provider.MsgInfoColumns.EVENT_DELETE_MSG_PART);  
			//删除用户后，同时删除用户对应的电子名片的触发器
			db.execSQL("DROP TRIGGER IF EXISTS " + Provider.UserColumns.EVENT_DELETE_VCARD);  
			
			db.execSQL("DROP TABLE IF EXISTS " + Provider.UserColumns.TABLE_NAME);  
	        db.execSQL("DROP TABLE IF EXISTS " + Provider.UserVcardColumns.TABLE_NAME);  
	        db.execSQL("DROP TABLE IF EXISTS " + Provider.MsgInfoColumns.TABLE_NAME);  
	        db.execSQL("DROP TABLE IF EXISTS " + Provider.MsgPartColumns.TABLE_NAME);  
	        db.execSQL("DROP TABLE IF EXISTS " + Provider.MsgThreadColumns.TABLE_NAME);  
	        db.execSQL("DROP TABLE IF EXISTS " + Provider.NewFriendColumns.TABLE_NAME);  
	        onCreate(db);
			break;
		}
		
	}
	
	/**
	 * 创建t_user中username的索引
	 * @param db
	 * @update 2015年9月17日 下午8:00:42
	 */
	private void createUsernameIndex(SQLiteDatabase db) {
		//CREATE UNIQUE INDEX " + Provider.UserColumns.USERNAME_INDEX + " ON " + Provider.UserColumns.TABLE_NAME + "(" + Provider.UserColumns.USERNAME + ");
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE UNIQUE INDEX IF NOT EXISTS ")
			.append(Provider.UserColumns.USERNAME_INDEX)
			.append(" ON ")
			.append(Provider.UserColumns.TABLE_NAME)
			.append("(")
			.append(Provider.UserColumns.USERNAME)
			.append(");");
		//添加用户表的username索引
		db.execSQL(sb.toString());
	}

	/**
	 * 创建t_msg_info表中的msgId索引
	 * @param db
	 */
	private void createMsgIdIndex(SQLiteDatabase db) {
		//CREATE UNIQUE INDEX 'msg_id_idx' ON t_msg_info (msgId);
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE UNIQUE INDEX IF NOT EXISTS ")
			.append(Provider.MsgInfoColumns.MSGID_INDEX)
			.append(" ON ")
			.append(Provider.MsgInfoColumns.TABLE_NAME)
			.append("(")
			.append(Provider.MsgInfoColumns.MSG_ID)
			.append(");");
		//添加消息表的msgId索引
		db.execSQL(sb.toString());
	}

	/**
	 * 创建t_msg_part表中的msgId索引
	 * @param db
	 */
	private void createMsgIdPartIndex(SQLiteDatabase db) {
		//CREATE UNIQUE INDEX 'msg_id_part_idx' ON t_msg_part(msgId);
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE UNIQUE INDEX IF NOT EXISTS ")
			.append(Provider.MsgPartColumns.MSGID_PART_INDEX)
			.append(" ON ")
			.append(Provider.MsgInfoColumns.TABLE_NAME)
			.append("(")
			.append(Provider.MsgInfoColumns.MSG_ID)
			.append(");");
		//添加消息表的msgId索引
		db.execSQL(sb.toString());
	}
	
	/**
	 * 创建更新t_thread中未读信息数量的触发器
	 * @update 2015年9月17日 下午7:50:16
	 */
	private void createCountUnreadMsgTrigger(SQLiteDatabase db) {
		//CREATE TRIGGER count_unread_msg_event AFTER UPDATE OF isRead ON t_msg_info WHEN NEW.isRead = 1 BEGIN UPDATE t_msg_thread SET unReadCount = unReadCount- 1 WHERE unReadCount > 0; END;
		StringBuilder sb = new StringBuilder();
		//创建t_msg_info更新isRead=1时，更新t_msg_thread表的unReadCount-1
		sb.append("CREATE TRIGGER IF NOT EXISTS ")
			.append(Provider.MsgInfoColumns.EVENT_COUNT_UNREAD_MSG)
			.append(" AFTER UPDATE OF ")
			.append(Provider.MsgInfoColumns.IS_READ)
			.append(" ON ")
			.append(Provider.MsgInfoColumns.TABLE_NAME)
			.append(" WHEN NEW.")
			.append(Provider.MsgInfoColumns.IS_READ)
			.append(" = 1")
			.append(" BEGIN UPDATE ")
			.append(Provider.MsgThreadColumns.TABLE_NAME)
			.append(" SET ")
			.append(Provider.MsgThreadColumns.UNREAD_COUNT)
			.append(" = ")
			.append(Provider.MsgThreadColumns.UNREAD_COUNT).append(" -1 ")
			.append(" WHERE ")
			.append(Provider.MsgThreadColumns.UNREAD_COUNT).append(" > 0; END;");
		db.execSQL(sb.toString());
	}
	
	/**
	 * 创建更新会话摘要的触发器，当添加一条新信息时，则会执行此触发器
	 * @param db
	 * @update 2015年9月18日 下午2:21:35
	 */
	private void createUpdateThreadSnippetTrigger(SQLiteDatabase db) {
		/*
		 * create trigger if not exists update_thread_snippet_event after insert  on t_msg_info 
			begin 
			     update t_msg_thread 
			            set unReadCount = case 
			                when new.isComming = 1 and new.isRead = 0 then unReadCount + 1                
			            else unReadCount            
			            end
			            ,
			     modifyDate = new.creationDate, snippetId = new._id, snippetContent = substr(new.content, 1, 100) where _id = new.threadID; 
			end;
		 */
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TRIGGER IF NOT EXISTS ")
			.append(Provider.MsgInfoColumns.EVENT_UPDATE_THREAD_SNIPPET)
			.append(" AFTER INSERT ON ")
			.append(Provider.MsgInfoColumns.TABLE_NAME)
			.append(" BEGIN UPDATE ")
			.append(Provider.MsgThreadColumns.TABLE_NAME)
			.append(" SET ")
			.append(Provider.MsgThreadColumns.UNREAD_COUNT)
			.append(" = CASE WHEN NEW.")
			.append(Provider.MsgInfoColumns.IS_COMMING)
			.append(" = 1 AND NEW.")
			.append(Provider.MsgInfoColumns.IS_READ)
			.append(" = 0 THEN ").append(Provider.MsgThreadColumns.UNREAD_COUNT).append(" + 1 ELSE ")
			.append(Provider.MsgThreadColumns.UNREAD_COUNT).append(" END, ")
			.append(Provider.MsgThreadColumns.MODIFY_DATE).append(" = NEW.").append(Provider.MsgInfoColumns.CREATIO_NDATE)
			.append(", ").append(Provider.MsgThreadColumns.SNIPPET_ID).append(" = NEW.").append(Provider.MsgInfoColumns._ID)
			.append(", ").append(Provider.MsgThreadColumns.SNIPPET_CONTENT).append(" = substr(NEW.").append(Provider.MsgInfoColumns.CONTENT)
			.append(", 1, 100) WHERE ").append(Provider.MsgThreadColumns._ID).append(" = NEW.").append(Provider.MsgInfoColumns.THREAD_ID).append("; END;");
		db.execSQL(sb.toString());
	}
	
	/**
	 * 创建删除消息后，同时删除其对应附件的触发器
	 * @param db
	 * @update 2015年9月18日 上午10:28:02
	 */
	private void createDeleteMsgPartTrigger(SQLiteDatabase db) {
		//create trigger if not exists delete_msg_part_event after delete on t_msg_info when old.msgType != 0 begin delete from t_msg_part where  msgId = old._id; end;
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TRIGGER IF NOT EXISTS ")
			.append(Provider.MsgInfoColumns.EVENT_DELETE_MSG_PART)
			.append(" AFTER ")
			.append(" DELETE ON ")
			.append(Provider.MsgInfoColumns.TABLE_NAME)
			.append(" WHEN OLD.")
			.append(Provider.MsgInfoColumns.MSG_TYPE).append(" != 0 ")	//0：表示文本消息，非文本消息才可能有附件
			.append(" BEGIN DELETE FROM ")
			.append(Provider.MsgPartColumns.TABLE_NAME)
			.append(" WHERE ")
			.append(Provider.MsgPartColumns.MSG_ID).append(" = OLD.").append(Provider.MsgInfoColumns._ID).append("; END;");
		db.execSQL(sb.toString());
	}
	
	/**
	 * 创建删除用户后，同时删除用户对应的电子名片,页删除对应的会话的触发器
	 * @param db
	 * @update 2015年9月18日 下午4:53:16
	 */
	private void createDeleteUserTrigger(SQLiteDatabase db) {
		/*
		 * create trigger if not exists delete_user_event after delete on t_user
			begin 
			      delete from t_user_vcard where userId = old._id;      
			
			      delete from t_msg_thread where memberIds = old._id;
			end;
		 */
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TRIGGER IF NOT EXISTS ")
			.append(Provider.UserColumns.EVENT_DELETE_VCARD)
			.append(" AFTER DELETE ON ")
			.append(Provider.UserColumns.TABLE_NAME)
			.append(" BEGIN DELETE FROM ")
			.append(Provider.UserVcardColumns.TABLE_NAME)
			.append(" WHERE ")
			.append(Provider.UserVcardColumns.USERID).append(" = old.")
			.append(Provider.UserColumns._ID).append(";")
			.append(" DELETE FROM ")
			.append(Provider.MsgThreadColumns.TABLE_NAME)
			.append(" where ")
			.append(Provider.MsgThreadColumns.MEMBER_IDS).append(" = old.")
			.append(Provider.UserColumns._ID).append("; END;");
		
		db.execSQL(sb.toString());
	}

}
