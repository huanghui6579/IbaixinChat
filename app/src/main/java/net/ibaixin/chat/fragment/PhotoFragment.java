package net.ibaixin.chat.fragment;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.download.ImageDownloader.Scheme;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import net.ibaixin.chat.R;
import net.ibaixin.chat.activity.PhotoPreviewActivity;
import net.ibaixin.chat.model.PhotoItem;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.view.PowerImageView;
import net.ibaixin.chat.view.ProgressWheel;

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
	
//	private PhotoView ivPhoto;
	private PowerImageView ivPhoto;
	private ProgressWheel pbLoading;
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
		pbLoading = (ProgressWheel) view.findViewById(R.id.pb_loading);
		pbLoading.setVisibility(View.GONE);
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
			String filePath = mPhoto.getFilePath();
			if (SystemUtil.isFileExists(filePath)) {
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
