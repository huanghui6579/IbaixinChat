package net.ibaixin.chat.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 消息的附件类
 * 
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年10月29日 上午10:07:09
 */
public class MsgPart implements Parcelable, Cloneable {
	/**
	 * 主键
	 */
	private int id;
	/**
	 * 文件所属的消息，依赖于{@link MsgInfo}的id
	 */
	private int msgId;
	/**
	 * 文件名称，不含有路径名称，但含有文件的格式后缀名
	 */
	private String fileName;
	/**
	 * 文件的缩略图名名称
	 */
	private String thumbName;
	/**
	 * 文件的本地存放的全名称，由目录名和文件名组成
	 */
	private String filePath;
	
	/**
	 * 缩略图的存储位置
	 */
	private String thumbPath;
	
	/**
	 * 文件的大小
	 */
	private long size;
	/**
	 * 文件的类型
	 */
	private String mimeType;
	
	/**
	 * 文件创建时间
	 */
	private long creationDate;
	
	/**
	 * 文件的服务器请求地址
	 */
	private String fileToken;
	
	/**
	 * 文件的描述，可作为扩展字段
	 */
	private String desc;
	
	/**
	 * 附件是否已下载
	 */
	private boolean downloaded;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getMsgId() {
		return msgId;
	}

	public void setMsgId(int msgId) {
		this.msgId = msgId;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	public String getThumbName() {
		return thumbName;
	}

	public void setThumbName(String thumbName) {
		this.thumbName = thumbName;
	}

	public String getThumbPath() {
		return thumbPath;
	}

	public void setThumbPath(String thumbPath) {
		this.thumbPath = thumbPath;
	}

	public String getFileToken() {
		return fileToken;
	}

	public void setFileToken(String fileToken) {
		this.fileToken = fileToken;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public boolean isDownloaded() {
		return downloaded;
	}

	public void setDownloaded(boolean downloaded) {
		this.downloaded = downloaded;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + msgId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MsgPart other = (MsgPart) obj;
		if (id != other.id)
			return false;
		if (msgId != other.msgId)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "MsgPart [id=" + id + ", msgId=" + msgId + ", fileName="
				+ fileName + ", thumbName=" + thumbName + ", filePath="
				+ filePath + ", thumbPath=" + thumbPath + ", size=" + size
				+ ", mimeType=" + mimeType + ", creationDate=" + creationDate
				+ ", fileToken=" + fileToken + ", desc=" + desc
				+ ", downloaded=" + downloaded + "]";
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		MsgPart part = null;
		try {
			part = (MsgPart) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return part;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(id);
		dest.writeInt(msgId);
		dest.writeString(fileName);
		dest.writeString(filePath);
		dest.writeString(mimeType);
		dest.writeLong(size);
		dest.writeLong(creationDate);
		dest.writeString(fileToken);
		dest.writeString(thumbName);
		dest.writeString(thumbPath);
		dest.writeString(desc);
		dest.writeByte((byte) (downloaded ? 1 :0));
	}
	
	public MsgPart() {
	}
	
	public MsgPart(Parcel in) {
		id = in.readInt();
		msgId = in.readInt();
		fileName = in.readString();
		filePath = in.readString();
		mimeType = in.readString();
		size = in.readLong();
		creationDate = in.readLong();
		fileToken = in.readString();
		thumbName = in.readString();
		thumbPath = in.readString();
		desc = in.readString();
		downloaded = in.readByte() == 1 ? true : false;
	}
	
	public static final Creator<MsgPart> CREATOR = new Creator<MsgPart>() {
		
		@Override
		public MsgPart[] newArray(int size) {
			return new MsgPart[size];
		}
		
		@Override
		public MsgPart createFromParcel(Parcel source) {
			return new MsgPart(source);
		}
	};
}
