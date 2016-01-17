package net.ibaixin.chat.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.SystemUtil;

import java.util.Comparator;

/**
 * 聊天消息实体类
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年10月28日 下午9:25:58
 */
public class MsgInfo implements Comparator<MsgInfo>, Parcelable, Cloneable {
	/**
	 * 主键
	 */
	private int id;
	/**
	 * uuid，可看作主键
	 */
	private String msgId;
	/**
	 * 会话id
	 */
	private int threadID;
	/**
	 * 发送人的fullJID，格式为：xxx@domain.com/resource
	 */
	private String fromUser;
	/**
	 * 收件人的fullJID，格式为：xxx@domain.com/resource
	 */
	private String toUser;
	/**
	 * 消息内容
	 */
	private String content;
	/**
	 * 消息主题
	 */
	private String subject;
	
	/**
	 * 消息创建时间
	 */
	private long creationDate;
	
	/**
	 * 是否是进来的消息
	 */
	private boolean isComming;
	
	/**
	 * 该消息是否已读，默认为未读
	 */
	private boolean isRead;
	
	/**
	 * 消息的附件
	 */
	private MsgPart msgPart;
	
	/**
	 * 消息的类型，默认是文本消息
	 */
	private Type msgType = Type.TEXT;
	
	/**
	 * 消息的发送状态，默认是正在发送
	 */
	private SendState sendState = SendState.SENDING;
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getThreadID() {
		return threadID;
	}

	public void setThreadID(int threadID) {
		this.threadID = threadID;
	}

	public String getFromUser() {
		return fromUser;
	}

	public void setFromUser(String fromUser) {
		this.fromUser = fromUser;
	}

	public String getToUser() {
		return toUser;
	}

	public void setToUser(String toUser) {
		this.toUser = toUser;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	public boolean isComming() {
		return isComming;
	}

	public void setComming(boolean isComming) {
		this.isComming = isComming;
	}

	public Type getMsgType() {
		return msgType;
	}

	public void setMsgType(Type msgType) {
		this.msgType = msgType;
	}

	public SendState getSendState() {
		return sendState;
	}

	public void setSendState(SendState sendState) {
		this.sendState = sendState;
	}

	public MsgPart getMsgPart() {
		return msgPart;
	}

	public void setMsgPart(MsgPart msgPart) {
		this.msgPart = msgPart;
	}
	
	public boolean isRead() {
		return isRead;
	}

	public void setRead(boolean isRead) {
		this.isRead = isRead;
	}

	public String getFromJid() {
		return fromUser + "@" + Constants.SERVER_NAME;
	}
	
	public String getToJid() {
		return toUser + "@" + Constants.SERVER_NAME;
	}

	public String getMsgId() {
		return msgId;
	}

	public void setMsgId(String msgId) {
		this.msgId = msgId;
	}
	
	/**
	 * 删除消息的id
	 * @author huanghui1
	 * @update 2015/12/3 16:37
	 * @version: 0.0.1
	 */
	public MsgInfo generateMsgId() {
		this.msgId = SystemUtil.generateUUID();
		return this;
	}

	/**
	 * 获取摘要信息，长度为100
	 * @return
	 * @update 2015年9月18日 下午6:21:51
	 */
	public String getShortContent(){
		if (content == null) {
			return null;
		}
		if (content.length() > Constants.THREAD_SNIPPET_LENGTH) {	//长度有100，则截取
			return content.substring(0, Constants.THREAD_SNIPPET_LENGTH);
		} else {	//直接返回整句
			return content;
		}
	}

	/**
	 * 判断该消息是否有缩略图
	 * @author tiger
	 * @update 2015/12/13 11:13
	 * @version 1.0.0
	 * @return 是否有缩略图
	 */
	public boolean hasThumbFile() {
		boolean flag = false;
		if (msgPart != null) {
			String thumbName = msgPart.getThumbName();
			if (TextUtils.isEmpty(thumbName)) {
				if (SystemUtil.isFileExists(msgPart.getThumbPath())) {
					flag = true;
				}
			} else {
				flag = true;
			}
		}
		return flag;
	}

	/**
	 * 设置消息的附件是否下载原始图片
	 * @param downloaded
	 */
	public void setDownloaded(boolean downloaded) {
		if (msgPart != null) {
			msgPart.setDownloaded(downloaded);
		}
	}

	/**
	 * 获取会话的摘要
	 * @return 范湖会话的摘要
	 * @author tiger
	 * @update 2015/11/8 9:39
	 * @version 1.0.0
	 */
	public String getSnippetContent() {
		String snippetContent = getShortContent()/*msgManager.getSnippetContentByMsgType(msgInfo.getMsgType(), msgInfo)*/;
		if (TextUtils.isEmpty(snippetContent)) {
			MsgPart msgPart = getMsgPart();
			if (msgPart != null) {
				snippetContent = msgPart.getFileName();
			}
		}
		return snippetContent;
	}

	@Override
	public String toString() {
		return "MsgInfo{" +
				"id=" + id +
				", msgId='" + msgId + '\'' +
				", threadID=" + threadID +
				", fromUser='" + fromUser + '\'' +
				", toUser='" + toUser + '\'' +
				", content='" + content + '\'' +
				", subject='" + subject + '\'' +
				", creationDate=" + creationDate +
				", isComming=" + isComming +
				", isRead=" + isRead +
				", msgPart=" + msgPart +
				", msgType=" + msgType +
				", sendState=" + sendState +
				'}';
	}

	/**
	 * 消息的分类，主要有:
	 * <ul>
	 * 	<li>Type.TEXT -- 普通文本</li>
	 * 	<li>Type.IMAGE -- 图片</li>
	 * 	<li>Type.AUDIO -- 音频</li>
	 * 	<li>Type.VIDEO -- 视频</li>
	 * 	<li>Type.LOCATION -- 地理位置</li>
	 * 	<li>Type.VCARD -- 名片</li>
	 * 	<li>Type.FILE -- 文件</li>
	 * 	<li>Type.VOICE -- 语音</li>
	 * </ul>
	 * @author huanghui1
	 * @update 2014年10月28日 下午9:57:28
	 */
	public enum Type {
		/**
		 * 普通文本
		 */
		TEXT,
		IMAGE,
		AUDIO,
		VIDEO,
		LOCATION,
		VCARD,
		FILE,
		VOICE,
		CALL_AUDIO,
		CALL_VIDEO;

		/**
		 * 将数字转换成Type
		 * @update 2014年10月28日 下午10:14:57
		 * @param value
		 * @return
		 */
		public static Type valueOf(int value) {
			switch (value) {
			case 0:
				return TEXT;
			case 1:
				return IMAGE;
			case 2:
				return AUDIO;
			case 3:
				return VIDEO;
			case 4:
				return LOCATION;
			case 5:
				return VCARD;
			case 6:
				return FILE;
			case 7:
				return VOICE;
			case 8:
				return CALL_AUDIO;
			case 9:
				return CALL_VIDEO;
			default:
				return FILE;
			}
		}
	}
	
	/**
	 * 消息的发送状态，主要有三种：
	 * <ul>
	 * 	<li>SENDING -- 正在发送，默认的发送状态</li>
	 * 	<li>SUCCESS -- 发送成功</li>
	 * 	<li>FAILED -- 发送失败</li>
	 * </ul>
	 * @author huanghui1
	 * @update 2014年10月29日 上午10:16:26
	 */
	public enum SendState {
		/**
		 * 正在发送，默认的发送状态
		 */
		SENDING,
		/**
		 * 发送成功
		 */
		SUCCESS,
		/**
		 * 发送失败
		 */
		FAILED;
		
		public static SendState valueOf(int vaule) {
			switch (vaule) {
			case 0:
				return SendState.SENDING;
			case 1:
				return SendState.SUCCESS;
			case 2:
				return SendState.FAILED;

			default:
				return SendState.SENDING;
			}
		}
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		MsgInfo mi = null;
		try {
			mi = (MsgInfo) super.clone();
			mi.sendState = sendState;	//枚举是单例的，所以只需拷贝引用就行了
			mi.msgType = msgType;
			if (msgPart != null) {
				mi.msgPart = (MsgPart) msgPart.clone();
			}
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return mi;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		MsgInfo msgInfo = (MsgInfo) o;

		if (threadID != msgInfo.threadID) return false;
		return !(msgId != null ? !msgId.equals(msgInfo.msgId) : msgInfo.msgId != null);

	}

	@Override
	public int hashCode() {
		int result = msgId != null ? msgId.hashCode() : 0;
		result = 31 * result + threadID;
		return result;
	}

	@Override
	public int compare(MsgInfo lhs, MsgInfo rhs) {
		int lId = lhs.getId();
		int rId = rhs.getId();
		if (lId > rId) {
			return 1;
		} else if (lId < rId) {
			return -1;
		} else {
			return 0;
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(id);
		dest.writeInt(threadID);
		dest.writeString(fromUser);
		dest.writeString(toUser);
		dest.writeString(content);
		dest.writeString(subject);
		dest.writeLong(creationDate);
		dest.writeInt(isComming ? 1 : 0);
		dest.writeInt(isRead ? 1 : 0);
		dest.writeParcelable(msgPart, flags);
		dest.writeInt(msgType.ordinal());
		dest.writeInt(sendState.ordinal());
		dest.writeString(msgId);
	}
	
	public MsgInfo() {
		msgId = SystemUtil.generateUUID();
	}
	
	public MsgInfo(Parcel in) {
		id = in.readInt();
		threadID = in.readInt();
		fromUser = in.readString();
		toUser = in.readString();
		content = in.readString();
		subject = in.readString();
		creationDate = in.readLong();
		isComming = in.readInt() == 1 ? true : false;
		isRead = in.readInt() == 1 ? true : false;
		msgPart = in.readParcelable(MsgPart.class.getClassLoader());
		msgType = Type.valueOf(in.readInt());
		sendState = SendState.valueOf(in.readInt());
		msgId = in.readString();
	}
	
	public static final Creator<MsgInfo> CREATOR = new Creator<MsgInfo>() {
		
		@Override
		public MsgInfo[] newArray(int size) {
			return new MsgInfo[size];
		}
		
		@Override
		public MsgInfo createFromParcel(Parcel source) {
			return new MsgInfo(source);
		}
	};
	
}
