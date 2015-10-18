package net.ibaixin.chat.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 表情的分类，如经典表情、大表情等
 * 
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年10月27日 下午5:57:55
 */
public class EmojiType implements Parcelable {
	/**
	 * 表情
	 */
	public static final int OPT_EMOJI = 1;
	/**
	 * 本地表情管理
	 */
	public static final int OPT_MANAGE = 2;
	/**
	 * 表情添加
	 */
	public static final int OPT_ADD = 3;
	
	/**
	 * 删除表情
	 */
	public static final int OPT_DEL = 4;
	
	private int resId;
	private String fileName;
	private String description;
	private int optType = OPT_EMOJI;
	/**
	 * 表情的类型，每一种类型对应一组表情
	 */
	private int emojiType = -1;

	public int getResId() {
		return resId;
	}

	public void setResId(int resId) {
		this.resId = resId;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getOptType() {
		return optType;
	}

	public void setOptType(int optType) {
		this.optType = optType;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(resId);
		dest.writeString(fileName);
		dest.writeString(description);
		dest.writeInt(optType);
		dest.writeInt(emojiType);
	}
	
	public EmojiType() {
	}
	
	public EmojiType(int resId, String fileName, String description, int optType, int emojiType) {
		super();
		this.resId = resId;
		this.fileName = fileName;
		this.description = description;
		this.optType = optType;
		this.emojiType = emojiType;
	}

	public EmojiType(Parcel in) {
		resId = in.readInt();
		fileName = in.readString();
		description = in.readString();
		optType = in.readInt();
		emojiType = in.readInt();
	}
	
	public static final Creator<EmojiType> CREATOR = new Creator<EmojiType>() {

		@Override
		public EmojiType createFromParcel(Parcel source) {
			return new EmojiType(source);
		}

		@Override
		public EmojiType[] newArray(int size) {
			return new EmojiType[size];
		}
	};

}
