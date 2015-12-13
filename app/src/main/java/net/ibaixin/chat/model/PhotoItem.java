package net.ibaixin.chat.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

/**
 * 相片实体
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年11月13日 下午5:58:41
 */
public class PhotoItem extends DownloadItem implements Parcelable {
	/**
	 * 缩略图路径
	 */
	private String thumbPath;


	public String getThumbPath() {
		return thumbPath;
	}

	public void setThumbPath(String thumbPath) {
		this.thumbPath = thumbPath;
	}

	/**
	 * 获取文件可显示的路径，优先显示缩略图，如果没有缩略图，则显示原始图片
	 * @return
	 */
	public String getShowPath() {
		if (!TextUtils.isEmpty(thumbPath)) {
			return thumbPath;
		} else {
			return filePath;
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(filePath);
		dest.writeString(thumbPath);
		dest.writeLong(size);
		dest.writeLong(time);
		dest.writeInt(needDownload ? 1 : 0);
		dest.writeString(fileToken);
		dest.writeInt(downloadType);
		dest.writeString(msgId);
		dest.writeInt(fileType.ordinal());
	}

	@Override
	public String toString() {
		return "PhotoItem{" +
				"thumbPath='" + thumbPath + '\'' +
				"} " + super.toString();
	}

	public PhotoItem() {
	}

	/**
	 * 判断该项是否是空的
	 * @return true:是空的；false:不是空的
	 */
	public boolean isEmpty() {
		return TextUtils.isEmpty(filePath) && TextUtils.isEmpty(thumbPath);
	}
	
	public PhotoItem(Parcel in) {
		filePath = in.readString();
		thumbPath = in.readString();
		size = in.readLong();
		time = in.readLong();
		needDownload = in.readInt() == 1;
		fileToken = in.readString();
		downloadType = in.readInt();
		msgId = in.readString();
		fileType = FileItem.FileType.valueOf(in.readInt());
	}
	
	public static final Creator<PhotoItem> CREATOR = new Creator<PhotoItem>() {
		
		@Override
		public PhotoItem[] newArray(int size) {
			return new PhotoItem[size];
		}
		
		@Override
		public PhotoItem createFromParcel(Parcel source) {
			return new PhotoItem(source);
		}
	};
}
