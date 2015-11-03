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
		pbLoading = (CircleProgressBar) view.findViewById(R.id.pb_loading);
		mIvFlag = view.findViewById(R.id.iv_flag);
//		pbLoading.setVisibility(View.GONE);
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
			boolean download = mPhoto.isNeedDownload();
			final String showPath = mPhoto.getShowPath();
			String filePath = mPhoto.getFilePath();
			if (mIsVideo) {	//视频文件
				options = SystemUtil.getAlbumVideoOptions();
				mIvFlag.setVisibility(View.VISIBLE);
			} else {
				mIvFlag.setVisibility(View.GONE);
				options = SystemUtil.getPhotoPreviewOptions();
			}
			if (download) {	//需要下载文件
				mImageLoader.displayImage(Scheme.FILE.wrap(showPath), ivPhoto, options);
				//开始下载文件
				final MsgEngine msgEngine = new MsgEngine(getActivity());
				msgEngine.downloadFile(mPhoto, new DownloadListener() {
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
						int progress = (int) (bytesWritten * 100 / totalBytes);
						pbLoading.setProgress(progress);
					}

					@Override
					public void onSuccess(int downloadId, final String filePath) {
						pbLoading.setVisibility(View.GONE);
						if (!TextUtils.isEmpty(filePath)) {
							if (!mIsVideo) {	//只有图片才清除缓存
								ImageUtil.clearMemoryCache(showPath);
								ImageUtil.clearDiskCache(showPath);
								//显示下载的原始图片
								mImageLoader.displayImage(Scheme.FILE.wrap(filePath), ivPhoto, options);
							}

							//更新本地数据库
							SystemUtil.getCachedThreadPool().execute(new Runnable() {
								@Override
								public void run() {
									String msgId = mPhoto.getMsgId();
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
						pbLoading.setVisibility(View.GONE);
					}
				});
			} else {	//不需要下载图片，则优先显示原始图片，如果原始图片不存在，则显示缩略图
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
			}
		} else {
			ivPhoto.setImageResource(R.drawable.ic_default_icon_error);
		}
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
}
