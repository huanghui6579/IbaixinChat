package net.ibaixin.chat.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import net.ibaixin.chat.update.FileUtil;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.MimeUtils;
import net.ibaixin.chat.util.SystemUtil;

import java.io.File;

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

	/**
	 * 文件夹的id
	 */
	private String bucketId;

	public String getThumbPath() {
		return thumbPath;
	}

	public void setThumbPath(String thumbPath) {
		this.thumbPath = thumbPath;
	}

	public String getBucketId() {
		return bucketId;
	}

	public void setBucketId(String bucketId) {
		this.bucketId = bucketId;
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
	
	/**
	 * 判断该项是否是视频项
	 * @author huanghui1
	 * @update 2015/12/23 15:14
	 * @version: 0.0.1
	 * @return 是否是视频文件，true:是视频
	 */
	public boolean isVideoItem() {
		return fileType == FileItem.FileType.VIDEO;
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
		dest.writeString(bucketId);
	}

	@Override
	public String toString() {
		return "PhotoItem{" +
				"thumbPath='" + thumbPath + '\'' +
				", bucketId='" + bucketId + '\'' +
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

	@Override
	public boolean deleteItem() {
		if (SystemUtil.isFileExists(filePath)) {
			return super.deleteItem();
		} else {
			if (thumbPath != null) {
				try {
					File file = new File(thumbPath);
					return file.delete();
				} catch (Exception e) {
					Log.e(e.getMessage());
				} finally {
					if (fileType == FileItem.FileType.IMAGE || fileType == FileItem.FileType.VIDEO) {
						SystemUtil.removeImageCache(thumbPath);
					}
				}
				return false;
			} else {
				return false;
			}
		}
	}

	/**
	 * 保存图片到本地
	 * @author tiger
	 * @update 2016/1/17 10:36
	 * @version 1.0.0
	 * @return 下载文件的路径
	 */
	public String downloadItem() {
		String destPath = null;
		if (fileType == FileItem.FileType.IMAGE) {	//图片才有下载
			File destFile = null;
			String srcPath = null;
			if (SystemUtil.isFileExists(filePath)) {	//保存原始图片
				srcPath = filePath;
				destFile = SystemUtil.generateDownloadFile(srcPath);
			} else if (SystemUtil.isFileExists(thumbPath)) {
				srcPath = thumbPath;
				String subFix = "jpg";
				if (MimeUtils.MIME_TYPE_IMAGE_JPG.equalsIgnoreCase(mime)) {
					subFix = "jpg";
				} else if (MimeUtils.MIME_TYPE_IMAGE_JPG.equalsIgnoreCase(mime)) {
					subFix = "png";
				}
				destFile = SystemUtil.generateDownloadFile(srcPath, subFix);
			}
			if (destFile != null) {
				destPath = destFile.getAbsolutePath();
				boolean success = FileUtil.copyFile(srcPath, destPath);
				if (!success) {
					destPath = null;
				}
			}
		} else {
			Log.d("---downloadItem---is--not----image----" + this);
		}
		return destPath;
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
		bucketId = in.readString();
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
