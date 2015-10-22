package net.ibaixin.chat.fragment;

import android.app.Activity;
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
	private PhotoItem mPhoto;
	
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
//		pbLoading.setVisibility(View.GONE);
		return view;
	}
	
	@Override
	public void onAttach(Activity activity) {
		if (activity instanceof FinishCallBackListener) {
			mFinishCallBackListener = (FinishCallBackListener) activity;
		}
		if (activity instanceof PhotoPreviewActivity) {
			mOnViewTapListener = (PhotoPreviewActivity) activity;
		}
		super.onAttach(activity);
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
		}
		ivPhoto.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
			
			@Override
			public void onViewTap(View view, float x, float y) {
				if (mOnTouchFinish && mFinishCallBackListener != null) {
					mFinishCallBackListener.onFinish();
				}
                if (mOnViewTapListener != null) {
                    mOnViewTapListener.onTap(view);
                }
			}
		});
		
		if (mPhoto != null) {
			boolean download = mPhoto.isNeedDownload();
			final String showPath = mPhoto.getShowPath();
			String filePath = mPhoto.getFilePath();
			if (download) {	//需要下载文件
				filePath = mPhoto.getThumbPath();
			}
			if (SystemUtil.isFileExists(showPath)) {
				
				if (!TextUtils.isEmpty(filePath)) {	//不需要下载文件
					ImageUtil.clearMemoryCache(filePath);
					ImageUtil.clearDiskCache(filePath);
					mImageLoader.displayImage(Scheme.FILE.wrap(filePath), ivPhoto, options, new ImageLoadingListener() {

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
				} else {	//要下载文件，则不显示圆形进度条
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
							int progress = (int) (bytesWritten / totalBytes);
							pbLoading.setProgress(progress);
						}

						@Override
						public void onSuccess(int downloadId, String filePath) {
							pbLoading.setVisibility(View.GONE);
							if (!TextUtils.isEmpty(filePath)) {
								ImageUtil.clearMemoryCache(showPath);
								ImageUtil.clearDiskCache(showPath);
								//显示下载的原始图片
								mImageLoader.displayImage(Scheme.FILE.wrap(filePath), ivPhoto, options);

								//更新本地数据库
								SystemUtil.getCachedThreadPool().execute(new Runnable() {
									@Override
									public void run() {
										int msgId = mPhoto.getMsgId();
										MsgManager msgManager = MsgManager.getInstance();
										MsgPart msgPart = new MsgPart();
										msgPart.setMsgId(msgId);
										msgPart.setDownloaded(true);
										msgManager.updateMsgPartDownload(msgPart, false);    //更新本地数据库
									}
								});
								
							}
						}

						@Override
						public void onFailure(int downloadId, int statusCode, String errMsg) {
							pbLoading.setVisibility(View.GONE);
						}
					});
				}
			} else {
				ivPhoto.setImageResource(R.drawable.ic_default_icon_error);
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
		 * @param view
		 */
		public void onTap(View view);
	}
}
