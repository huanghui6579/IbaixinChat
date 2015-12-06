package net.ibaixin.chat.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lsjwzh.widget.materialloadingprogressbar.CircleProgressBar;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.download.ImageDownloader.Scheme;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import net.ibaixin.chat.R;
import net.ibaixin.chat.activity.PhotoPreviewActivity;
import net.ibaixin.chat.download.DownloadListener;
import net.ibaixin.chat.manager.MsgManager;
import net.ibaixin.chat.manager.web.MsgEngine;
import net.ibaixin.chat.model.DownloadItem;
import net.ibaixin.chat.model.FileItem;
import net.ibaixin.chat.model.MsgPart;
import net.ibaixin.chat.model.PhotoItem;
import net.ibaixin.chat.util.ImageUtil;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.view.PowerImageView;

import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * 照片预览的界面
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年11月15日 上午10:04:06
 */
public class PhotoFragment extends BaseFragment {
	public static final String ARG_PHOTO = "arg_photo";
	public static final String ARG_TOUCH_FINISH = "arg_touch_finish";
	public static final String ARG_DOWNLOAD_IMG = "arg_download_img";
	
//	private PhotoView ivPhoto;
	private PowerImageView ivPhoto;
//	private ProgressWheel pbLoading;
	private CircleProgressBar pbLoading;
	private View mIvFlag;
	private PhotoItem mPhoto;
	/**
	 * 是否是视频文件
	 */
	private boolean mIsVideo;
	
	private FinishCallBackListener mFinishCallBackListener;
	
	private OnViewTapListener mOnViewTapListener;
	
	/**
	 * 是否点击屏幕就退出该界面
	 */
	private boolean mOnTouchFinish = false;

	private ImageLoader mImageLoader = ImageLoader.getInstance();
	private DisplayImageOptions options = SystemUtil.getPhotoPreviewOptions();
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_photo_preview, container, false);
		ivPhoto = (PowerImageView) view.findViewById(R.id.iv_photo);
		mIvFlag = view.findViewById(R.id.iv_flag);
		pbLoading = (CircleProgressBar) view.findViewById(R.id.pb_loading);
		return view;
	}
	
	@Override
	public void onAttach(Context context) {
		if (context instanceof FinishCallBackListener) {
			mFinishCallBackListener = (FinishCallBackListener) context;
		}
		if (context instanceof PhotoPreviewActivity) {
			mOnViewTapListener = (PhotoPreviewActivity) context;
		}
		super.onAttach(context);
	}
	
	@Override
	public void onDetach() {
		if (mFinishCallBackListener != null) {
			mFinishCallBackListener = null;
		}
        if (mOnViewTapListener != null) {
            mOnViewTapListener = null;
        }
		super.onDetach();
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Bundle args = getArguments();
		if (args != null) {
			mPhoto = args.getParcelable(ARG_PHOTO);
			mOnTouchFinish = args.getBoolean(ARG_TOUCH_FINISH, false);
			mIsVideo = mPhoto.getFileType() == FileItem.FileType.VIDEO;
		}
		ivPhoto.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {

			@Override
			public void onViewTap(View view, float x, float y) {
				if (mOnTouchFinish && mFinishCallBackListener != null) {
					mFinishCallBackListener.onFinish();
				}
				if (mOnViewTapListener != null) {
					FileItem.FileType fileType = null;
					if (mPhoto != null) {
						fileType = mPhoto.getFileType();
					}
					mOnViewTapListener.onTap(view, fileType, mPhoto);
				}
			}
		});
		
		if (mPhoto != null) {
//			boolean download = mPhoto.isNeedDownload();
			final String showPath = mPhoto.getShowPath();
			String filePath = mPhoto.getFilePath();
			if (mIsVideo) {	//视频文件
				options = SystemUtil.getAlbumVideoOptions();
				mIvFlag.setVisibility(View.VISIBLE);
			} else {
				mIvFlag.setVisibility(View.GONE);
				options = SystemUtil.getPhotoPreviewOptions();
			}

			if (SystemUtil.isFileExists(showPath)) {

				String displayPath = null;
				if (!mIsVideo) {	//非视频
					if (SystemUtil.isFileExists(filePath)) {
						displayPath = filePath;
					} else {
						displayPath = showPath;
					}
					ImageUtil.clearMemoryCache(displayPath);
					ImageUtil.clearDiskCache(displayPath);
				} else {	//视频
					String thumbPath = mPhoto.getThumbPath();
					if (SystemUtil.isFileExists(thumbPath)) {
						displayPath = thumbPath;
					} else {
						displayPath = filePath;
					}
				}
				String imgUri = null;
				if (!TextUtils.isEmpty(displayPath)) {
					imgUri = Scheme.FILE.wrap(displayPath);
				}
				mImageLoader.displayImage(imgUri, ivPhoto, options, new ImageLoadingListener() {

					@Override
					public void onLoadingStarted(String imageUri, View view) {
						if (!SystemUtil.isViewVisible(pbLoading)) {
							pbLoading.setVisibility(View.VISIBLE);
						}
					}

					@Override
					public void onLoadingFailed(String imageUri, View view,
												FailReason failReason) {
						if (SystemUtil.isViewVisible(pbLoading)) {
							pbLoading.setVisibility(View.GONE);
						}
					}

					@Override
					public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
						if (SystemUtil.isViewVisible(pbLoading)) {
							pbLoading.setVisibility(View.GONE);
						}
					}

					@Override
					public void onLoadingCancelled(String imageUri, View view) {

					}
				});
			} else {
				ivPhoto.setImageResource(R.drawable.ic_default_icon_error);
			}
		} else {
			ivPhoto.setImageResource(R.drawable.ic_default_icon_error);
		}
	}
	
	/**
	 * 现在原始图片
	 * @param photoItem 图片或者视频项
	 * @author huanghui1
	 * @update 2015/12/4 17:47
	 * @version: 0.0.1
	 */
	public void downloadPhotoItem(final PhotoItem photoItem, final DownloadCallback downloadCallback) {
		//开始下载文件
		final String showPath = photoItem.getShowPath();
		final MsgEngine msgEngine = new MsgEngine(getActivity());
		msgEngine.downloadFile(photoItem, new DownloadListener() {

			@Override
			public void onStart(int downloadId, long totalBytes) {
				pbLoading.setVisibility(View.VISIBLE);
				pbLoading.setCircleBackgroundEnabled(true);
				pbLoading.setShowProgressText(true);
				pbLoading.setProgress(0);
			}

			@Override
			public void onRetry(int downloadId) {

			}

			@Override
			public void onProgress(int downloadId, long bytesWritten, long totalBytes) {
				if (totalBytes > 0) {
					int progress = (int) (bytesWritten * 100 / totalBytes);
					pbLoading.setProgress(progress);
				}
			}

			@Override
			public void onSuccess(int downloadId, final String filePath) {
				pbLoading.setVisibility(View.GONE);
				if (!TextUtils.isEmpty(filePath)) {
					photoItem.setNeedDownload(false);
					if (downloadCallback != null) {
						downloadCallback.onSuccess(photoItem, filePath);
					}
					if (!mIsVideo) {    //只有图片才清除缓存
						ImageUtil.clearMemoryCache(showPath);
						ImageUtil.clearDiskCache(showPath);
						//显示下载的原始图片
						mImageLoader.displayImage(Scheme.FILE.wrap(filePath), ivPhoto, options);
					}

					//更新本地数据库
					SystemUtil.getCachedThreadPool().execute(new Runnable() {
						@Override
						public void run() {
							String msgId = photoItem.getMsgId();
							MsgManager msgManager = MsgManager.getInstance();
							MsgPart msgPart = new MsgPart();
							msgPart.setMsgId(msgId);
							msgPart.setDownloaded(true);
							msgManager.updateMsgPartDownload(msgPart, true);    //更新本地数据库
						}
					});

				}
			}

			@Override
			public void onFailure(int downloadId, int statusCode, String errMsg) {
				if (downloadCallback != null) {
					downloadCallback.onFailed(photoItem, statusCode, errMsg);
				}
				pbLoading.setVisibility(View.GONE);
			}
		});
	}

	/**
	 * 现在原始图片
	 * @author huanghui1
	 * @update 2015/12/4 17:47
	 * @version: 0.0.1
	 */
	public void downloadPhotoItem(final DownloadCallback downloadCallback) {
		downloadPhotoItem(mPhoto, downloadCallback);
	}

	/**
	 * 该activity消失的回调事件
	 * @author huanghui1
	 * @update 2015年3月4日 下午3:15:34
	 */
	public interface FinishCallBackListener {
		/**
		 * 该activity消失
		 * @update 2015年3月4日 下午3:16:22
		 */
		public void onFinish();
	}

	/**
	 * 图片单击的回调
	 * @author huanghui1 
	 */
	public interface OnViewTapListener {
		/**
		 * 单击的响应事件
		 * @param view 点击的视图
		 * @param fileType 文件的类型      
		 */
		public void onTap(View view, FileItem.FileType fileType, DownloadItem photoItem);
	}
	
	/**
	 * 图片长按的监听器
	 * @author tiger
	 * @update 2015/12/5 12:11
	 * @version 1.0.0
	 */
	public interface OnLongClickListener {
		/**
		 * 长按事件
		 * @param view 长按的控件
		 * @param photoItem 长按的item
		 */
		public void onLongClick(View view, DownloadItem photoItem);
	}
	
	/**
	 * 文件下载后的回调,注：回调中的方法都是在非UI线程中执行
	 * @author huanghui1
	 * @update 2015/12/4 17:52
	 * @version: 0.0.1
	 */
	public interface DownloadCallback {
		/**
		 * 下载成功
		 * @param filePath
		 */
		public void onSuccess(PhotoItem photoItem, String filePath);

		/**
		 * 下载失败
		 * @param statusCode 失败的状态码
		 * @param errMsg 失败的信息
		 */
		public void onFailed(PhotoItem photoItem, int statusCode, String errMsg);
	}
}
