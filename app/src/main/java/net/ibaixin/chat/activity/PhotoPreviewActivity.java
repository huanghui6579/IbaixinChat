package net.ibaixin.chat.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.os.AsyncTaskCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v7.internal.view.ViewPropertyAnimatorCompatSet;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import net.ibaixin.chat.R;
import net.ibaixin.chat.fragment.PhotoFragment;
import net.ibaixin.chat.manager.MsgManager;
import net.ibaixin.chat.model.DownloadItem;
import net.ibaixin.chat.model.FileItem;
import net.ibaixin.chat.model.MsgInfo;
import net.ibaixin.chat.model.MsgPart;
import net.ibaixin.chat.model.PhotoItem;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.MimeUtils;
import net.ibaixin.chat.util.SystemUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 相片预览容器
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年11月15日 上午9:37:58
 */
public class PhotoPreviewActivity extends BaseActivity implements PhotoFragment.OnViewTapListener, View.OnClickListener, PhotoFragment.OnLongClickListener {
	public static final String ARG_PHOTO_LIST = "arg_photo_list";
	public static final String ARG_POSITION = "arg_position";
	public static final String ARG_SHOW_MODE = "arg_show_mode";
	public static final String ARG_QUERY_FLAG = "arg_query_flag";

	public static final int QUERY_FLAG_NONE = 0;
	public static final int QUERY_FLAG_ALL = 1;
	public static final int QUERY_FLAG_BUCKET = 2;

	/**
	 * 延迟5秒“图片的更多”控件隐藏
	 */
	private static final int HIDE_DELAY = 5000;

	/**
	 * 是否原图发送
	 */
	public static final String ARG_ORIGINAO_IMAGE = "arg_originao_image";
	
	/**
	 * 浏览模式进入
	 */
	public static final int MODE_BROWSE = 1;
	/**
	 * 选择模式进入
	 */
	public static final int MODE_CHOSE = 2;
	/**
	 * 图片查看模式进入,该模式只是查看图片，不提供发送、选择等入口
	 */
	public static final int MODE_DISPLAY = 3;
	
	private ViewPager mViewPager;
	private CheckBox cbOrigianlImage;
	private CheckBox cbChose;
	private TextView mTvFileSize;
	private View layoutBottom;
	private MenuItem mMenuDone;
//	private TextView btnOpt;
	
	private ProgressDialog pDialog;
	private MsgManager msgManager = MsgManager.getInstance();
	
	/**
	 * 所选图片的数量
	 */
	private int selectCount = 0;
	private SparseBooleanArray selectArray = new SparseBooleanArray();
	
	/**
	 * 所浏览的图片集合
	 */
	private ArrayList<PhotoItem> mPhotos;
	private MsgInfo msgInfo;
	
	/**
	 * 选择的图片集合
	 */
	private ArrayList<PhotoItem> mSelectList = new ArrayList<>();
	private int currentPostion = 0;
	private int totalCount;
	private int showMode = 0;
	
	/**
	 * 选择的图片的原始大小
	 */
	private long selectOriginalSize = 0;
	
	PhotoFragmentViewPager photoAdapter;

	/**
	 * 标题栏和底部栏是否显示
	 */
	private boolean mShow = true;

	/**
	 * 是否点击屏幕就退出该界面
	 */
	private boolean mOnTouchFinish = false;

	/**
	 * 更多图片的按钮，进入图片管理
	 */
	private View mMoreView;

	/**
	 * 下载原始图片的按钮
	 */
	private Button mBtnDownload;

	/**
	 * 是否需要查询图片，当通过intent传入的数据过大时，会出现报错，只有重新查询刷新界面
	 */
	private int mQueryFlag;

	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Constants.MSG_SUCCESS:
				Intent data = new Intent();
				data.putExtra(ARG_ORIGINAO_IMAGE, cbOrigianlImage.isChecked());
				data.putParcelableArrayListExtra(ChatActivity.ARG_MSG_INFO_LIST, (ArrayList<MsgInfo>) msg.obj);
				setResult(RESULT_OK, data);
				finish();
				break;
			case Constants.MSG_FAILED:
				SystemUtil.makeShortToast(R.string.album_photo_chose_error);
				setResult(RESULT_CANCELED);
				finish();
				break;
			case Constants.MSG_HIDE_DELAY:	//延迟隐藏
				if (mMoreView.getVisibility() == View.VISIBLE) {
					mMoreView.setVisibility(View.GONE);
				}
				break;
			case Constants.MSG_DOWNLOAD_SUCCESS:	//文件下载成功的消息
				if (mBtnDownload.getVisibility() == View.VISIBLE) {	//隐藏下载按钮
					mBtnDownload.setVisibility(View.GONE);
				}
				break;
			case Constants.MSG_DOWNLOAD_FAILED:	//文件下载失败
				SystemUtil.makeShortToast(R.string.album_photo_download_failed);
				break;
			default:
				break;
			}
			if (pDialog != null && pDialog.isShowing()) {
				pDialog.dismiss();
			}
		}
		
	};

	@Override
	protected int getContentView() {
		return R.layout.activity_photo_preview;
	}

	@Override
	protected void initView() {
		mViewPager = (ViewPager) findViewById(R.id.view_pager);
		cbOrigianlImage = (CheckBox) findViewById(R.id.cb_original_image);
		cbChose = (CheckBox) findViewById(R.id.cb_chose);
		mTvFileSize = (TextView) findViewById(R.id.tv_file_size);
		layoutBottom = findViewById(R.id.layout_bottom);
		layoutBottom.setAlpha(0.8f);

		mBtnDownload = (Button) findViewById(R.id.downloadOriginal);

		mMoreView = findViewById(R.id.iv_more);

	}
	
	@Override
	public boolean isSwipeBackEnabled() {
		return false;
	}

	@Override
	protected boolean hasExitAnim() {
		return false;
	}
	
	@Override
	protected void initData() {
		Intent intent = getIntent();
		mPhotos = intent.getParcelableArrayListExtra(ARG_PHOTO_LIST);
		currentPostion = intent.getIntExtra(ARG_POSITION, 0);
		showMode = intent.getIntExtra(ARG_SHOW_MODE, MODE_DISPLAY);
		msgInfo = intent.getParcelableExtra(ChatActivity.ARG_MSG_INFO);
		mOnTouchFinish = intent.getBooleanExtra(PhotoFragment.ARG_TOUCH_FINISH, false);
		photoAdapter = new PhotoFragmentViewPager(getSupportFragmentManager());
		mViewPager.setAdapter(photoAdapter);
		mQueryFlag = intent.getIntExtra(ARG_QUERY_FLAG, QUERY_FLAG_NONE);
		if (currentPostion != 0) {
			if (currentPostion < photoAdapter.getCount()) {
				mViewPager.setCurrentItem(currentPostion);
			} else {
				currentPostion = 0;
			}
		}
		if (showMode == MODE_CHOSE) {	//选择模式，则默认选中的就是所有列表
			mSelectList.addAll(mPhotos);
			selectCount = mSelectList.size();
			for (int i = 0; i < selectCount; i++) {
				selectArray.put(i, true);
			}
			cbChose.setChecked(true);
			selectOriginalSize = SystemUtil.getFileListSize(mSelectList);
			cbOrigianlImage.setText(getString(R.string.album_preview_original_image_size, SystemUtil.sizeToString(selectOriginalSize)));
		} else if (showMode == MODE_DISPLAY) {	//图片的查看模式
			fullScreen(true);
			if (SystemUtil.isNotEmpty(mPhotos)) {	//先显示第0条
				PhotoItem photoItem = mPhotos.get(0);
				initDownloadButton(photoItem);
			}
			if (msgInfo != null) {
				AsyncTaskCompat.executeParallel(new LoadImageMsgTask(), msgInfo.getThreadID());
			}
			layoutBottom.setVisibility(View.GONE);
			mAppBar.setVisibility(View.GONE);
		} else {	//图片浏览模式
			if (mQueryFlag != QUERY_FLAG_NONE) {	//需要查询某路径下的所有图片
				if (SystemUtil.isNotEmpty(mPhotos)) {	//先显示第0条
					PhotoItem photoItem = mPhotos.get(0);
					AsyncTaskCompat.executeParallel(new LoadImageTask(), photoItem);
				}
			}
		}
		totalCount = mPhotos.size();
		setTitle(getString(R.string.album_preview_photo_index, currentPostion + 1, totalCount));
	}

	/**
	 * 显示底部栏
	 */
	private void showBottomLayout() {
		if (layoutBottom != null) {
			if (layoutBottom.getVisibility() != View.VISIBLE) {
				layoutBottom.setVisibility(View.VISIBLE);
			}
		}
	}
	
	/**
	 * 初始化一些数据
	 * @author huanghui1
	 * @update 2015/12/4 15:46
	 * @version: 0.0.1
	 */
	private void resetData() {
		mShow = true;
		mQueryFlag = QUERY_FLAG_NONE;
		if (pDialog != null && pDialog.isShowing()) {
			pDialog.dismiss();
		}
		if (SystemUtil.isFullScreen(this)) {
			fullScreen(false);
		}
	}

	/**
	 * 是否开启全屏模式
	 * @param enable true:开启全屏模式，false：取消全屏模式
	 * @author tiger
	 * @update 2015/11/28 10:12
	 * @version 1.0.0
	 */
	private void fullScreen(boolean enable) {
		if (enable) {
			WindowManager.LayoutParams lp = getWindow().getAttributes();
			lp.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
			getWindow().setAttributes(lp);
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
		} else {
			WindowManager.LayoutParams attr = getWindow().getAttributes();
			attr.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().setAttributes(attr);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
		}
	}
	
	/**
	 * 刷新发送按钮
	 * @update 2014年11月15日 下午4:05:34
	 * @param selectCount
	 */
	private void updateBtnOpt(int selectCount) {
		if (mMenuDone != null) {
			if (selectCount <= 0) {	//没有选中的
				mMenuDone.setEnabled(false);
				mMenuDone.setTitle(getString(R.string.action_select_complete));
			} else {
				mMenuDone.setEnabled(true);
				mMenuDone.setTitle(getString(R.string.action_select_complete) + "(" + selectCount + "/" + Constants.ALBUM_SELECT_SIZE + ")");
			}
		}
	}
	
	/**
	 * 更新选择原图发送的复选框的样式
	 * @update 2014年11月15日 下午5:52:01
	 * @param selectSize 当前选择的数量
	 */
	private void updateOriginalCheckbox(long selectSize) {
		if (selectSize > 0) {	//选中了图片
			cbOrigianlImage.setText(getString(R.string.album_preview_original_image_size, SystemUtil.sizeToString(selectOriginalSize)));
		} else {
			cbOrigianlImage.setText(R.string.album_preview_original_image);
		}
	}

	/**
	 * 为呃照片复选框添加监听器
	 * @update 2015年2月12日 下午7:26:31
	 */
	private void addCheckImageListener() {
		cbChose.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					selectCount++;
					if (selectCount > Constants.ALBUM_SELECT_SIZE) {    //选择的多于9张
						selectCount = Constants.ALBUM_SELECT_SIZE;
						SystemUtil.makeShortToast(getString(R.string.album_tip_max_select, Constants.ALBUM_SELECT_SIZE));
						cbChose.setChecked(false);
						return;
					}
					mSelectList.add(mPhotos.get(currentPostion));
				} else {
					mSelectList.remove(mPhotos.get(currentPostion));
					selectCount--;
					selectCount = selectCount < 0 ? 0 : selectCount;
				}
				selectOriginalSize = SystemUtil.getFileListSize(mSelectList);
				selectArray.put(currentPostion, isChecked);
				updateBtnOpt(selectCount);
				updateOriginalCheckbox(selectOriginalSize);
			}
		});
	}
	
	@Override
	protected void addListener() {
		cbOrigianlImage.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// TODO Auto-generated method stub
				
			}
		});
		addCheckImageListener();
		mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

			@Override
			public void onPageSelected(int position) {
				Log.d("----onPageSelected----position---" + position);
				// TODO Auto-generated method stub
				currentPostion = position;
				setTitle(getString(R.string.album_preview_photo_index, currentPostion + 1, totalCount));
				if (isDisplayMode()) {    //查看图片的模式
					PhotoItem photoItem = mPhotos.get(position);
					initDownloadButton(photoItem);
				} else {
					cbChose.setOnCheckedChangeListener(null);
					cbChose.setChecked(selectArray.indexOfKey(position) >= 0 ? selectArray.get(position) : false);
					addCheckImageListener();
					if (mMenuDone != null) {
						updateBtnOpt(selectCount);
					}
				}
			}

			@Override
			public void onPageScrolled(int position, float positionOffset,
									   int positionOffsetPixels) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onPageScrollStateChanged(int state) {
				// TODO Auto-generated method stub
				if (showMode == MODE_DISPLAY) {    //图片查看模式
					mHandler.removeMessages(Constants.MSG_HIDE_DELAY);
					if (state == ViewPager.SCROLL_STATE_IDLE) {    //闲置状态
						mHandler.sendEmptyMessageDelayed(Constants.MSG_HIDE_DELAY, HIDE_DELAY);
					} else {
						if (mMoreView.getVisibility() != View.VISIBLE) {    //隐藏状态，则立即显示
							mMoreView.setVisibility(View.VISIBLE);
						}
					}
				}
			}
		});

		mMoreView.setOnClickListener(this);

		mBtnDownload.setOnClickListener(this);
		
	}

	/**
	 * 是否是查看图片的模式
	 * @author huanghui1
	 * @update 2015/12/4 17:13
	 * @version: 0.0.1
	 */
	private boolean isDisplayMode() {
		return showMode == MODE_DISPLAY;
	}

	/**
	 * 初始化图片的下载按钮
	 * @param photoItem 相册的item
	 */
	private void initDownloadButton(PhotoItem photoItem) {
		if (photoItem != null) {
			boolean showDownloadBtn = false;
			String filePath = photoItem.getFilePath();
			if (photoItem.isNeedDownload() || !SystemUtil.isFileExists(filePath)) {	//需要下载原始图片
				showDownloadBtn = true;
			}
			if (photoItem.getFileType() == FileItem.FileType.VIDEO) {	//视频文件
				cbOrigianlImage.setVisibility(View.GONE);
				mTvFileSize.setVisibility(View.VISIBLE);
				mTvFileSize.setText(getString(R.string.album_video_size, SystemUtil.sizeToString(photoItem.getSize())));
			} else {	//图片
				showDownloadBtn = photoItem.isNeedDownload();
			}
			if (showDownloadBtn) {
				mBtnDownload.setText(getString(R.string.album_download_original_image_size, SystemUtil.sizeToString(photoItem.getSize())));
				mBtnDownload.setVisibility(View.VISIBLE);
			} else {
				mBtnDownload.setVisibility(View.GONE);
			}
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		/*
    	 * 主要原因是该activity的android:launchMode="singleTask"
    	 * 所以第一次会或得到intent里的数据，但第二次或者以后就获取不到了，所以需要获取原来intent中的数据并且重新设置
    	 */
		super.onNewIntent(intent);
		setIntent(intent);

		resetView();

		resetData();

		initData();
	}

	/**
	 * 重置view的一些状态
	 */
	private void resetView() {
		if (SystemUtil.isFullScreen(this)) {	//判断是否是全屏
			fullScreen(false);
		}
		mBtnDownload.setVisibility(View.GONE);
		showBottomLayout();
		if (mAppBar != null) {
			mAppBar.setVisibility(View.VISIBLE);
		}
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (showMode != MODE_DISPLAY) {	//图片的查看模式
			MenuInflater menuInflater = getMenuInflater();
			menuInflater.inflate(R.menu.menu_save, menu);
			mMenuDone = menu.findItem(R.id.action_select_complete);
//			btnOpt = (TextView) MenuItemCompat.getActionView(menuDone);
			if (showMode == MODE_CHOSE) {	//选择模式，则默认选中的就是所有列表
				mMenuDone.setEnabled(true);
				mMenuDone.setTitle(getString(R.string.action_select_complete) + "(" + selectCount + "/" + Constants.ALBUM_SELECT_SIZE + ")");
			} else {
				mMenuDone.setEnabled(false);
			}
		}
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_select_complete:
			pDialog = ProgressDialog.show(mContext, null, getString(R.string.chat_sending_file), false, true);
			SystemUtil.getCachedThreadPool().execute(new Runnable() {
				
				@Override
				public void run() {
					ArrayList<MsgInfo> msgList = msgManager.getMsgInfoListByPhotos(msgInfo, mSelectList, cbOrigianlImage.isChecked());
					Message msg = mHandler.obtainMessage();
					if (!SystemUtil.isEmpty(msgList)) {	//消息集合
						msg.what = Constants.MSG_SUCCESS;
						msg.obj = msgList;
					} else {
						msg.what = Constants.MSG_FAILED;
					}
					mHandler.sendMessage(msg);
				}
			});
			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * 获取当前的photoitem
	 * @return
	 */
	private PhotoItem getCurrentItem() {
		if (mViewPager != null) {
			int position = mViewPager.getCurrentItem();
			PhotoItem item = mPhotos.get(position);
			return item;
		} else {
			return null;
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.downloadOriginal:	//下载原始图片
				if (photoAdapter != null) {
					int position = mViewPager.getCurrentItem();
					Object obj = photoAdapter.instantiateItem(mViewPager, position);
					if (obj != null) {
						PhotoFragment photoFragment = (PhotoFragment) obj;
						photoFragment.downloadPhotoItem(new PhotoFragment.DownloadCallback() {
							@Override
							public void onSuccess(PhotoItem photoItem, String filePath) {
								PhotoItem currentItem = getCurrentItem();
								if (currentItem != null) {
									if (photoItem.getMsgId().equals(currentItem.getMsgId())) {	//同一张图片，界面没有一切换
										currentItem.setNeedDownload(false);
										currentItem.setFilePath(filePath);
										mHandler.sendEmptyMessage(Constants.MSG_DOWNLOAD_SUCCESS);
									}
								}
							}

							@Override
							public void onFailed(PhotoItem photoItem, int statusCode, String errMsg) {
								mHandler.sendEmptyMessage(Constants.MSG_DOWNLOAD_FAILED);
								Log.d("----photoItem--orifinal image download failed--" + photoItem + "---statusCode---" + statusCode + "--errMsg--" + errMsg);
							}
						});
					} else {
						Log.d("---download---photoitem---viewpager---position---" + position + "---fragment--is--null---");
					}
				}
				break;
			case R.id.iv_more:	//管理聊天图片
				Intent intent = new Intent(mContext, AlbumActivity.class);
				intent.putExtra(AlbumActivity.ARG_ALBUM_MANAGER, true);
				if (mPhotos.size() > AlbumActivity.MAX_PHOTO_NUMBER) {	//数据超过100条就重新查询，不然会崩溃
					if (msgInfo != null) {
						int position = mViewPager.getCurrentItem();
						PhotoItem item = mPhotos.get(position);
						MsgInfo msg = new MsgInfo();
						msg.setMsgId(item.getMsgId());
						msg.setThreadID(msgInfo.getThreadID());
						ArrayList<PhotoItem> list = new ArrayList<>(1);
						list.add(item);
						intent.putParcelableArrayListExtra(ARG_PHOTO_LIST, list);
						intent.putExtra(ChatActivity.ARG_MSG_INFO, msg);
						intent.putExtra(ARG_QUERY_FLAG, QUERY_FLAG_ALL);
					}
				} else {
					intent.putParcelableArrayListExtra(ARG_PHOTO_LIST, mPhotos);
				}
				startActivity(intent);
				break;
		}
	}

	@Override
	public void onLongClick(View view, final DownloadItem downloadItem) {
		if (downloadItem != null && isDisplayMode()) {	//只有相册管理时才有菜单
			if (downloadItem instanceof PhotoItem) {
				final PhotoItem photoItem = (PhotoItem) downloadItem;
				MaterialDialog.Builder builder = new MaterialDialog.Builder(mContext);
				builder.items(R.array.album_photo_context_menu)
						.itemsCallback(new MaterialDialog.ListCallback() {
							@Override
							public void onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
								switch (which) {
									case 0:    //转发
										forwardMsg(photoItem);
										break;
									case 1:    //保存图片到本地
										mHandler.post(new Runnable() {
											@Override
											public void run() {
												String downloadPath = photoItem.downloadItem();
												if (SystemUtil.isFileExists(downloadPath)) {    //保存成功
													SystemUtil.scanFileAsync(mContext, downloadPath);    //添加到多媒体扫描里
													SystemUtil.makeLongToast(getString(R.string.album_save_photo_success, downloadPath));
												} else {
													SystemUtil.makeShortToast(R.string.album_save_photo_failed);
												}
											}
										});
										break;
								}
							}
						}).show();
			}
		}
	}

	/**
	 * 转发消息
	 * @param photoItem 图片
	 * @author tiger
	 * @update 2016/1/17 10:35
	 * @version 1.0.0
	 */
	private void forwardMsg(PhotoItem photoItem) {
		MsgInfo msgInfo = new MsgInfo();

		FileItem.FileType fileType = photoItem.getFileType();
		if (fileType == FileItem.FileType.IMAGE) {	//图片
			msgInfo.setMsgType(MsgInfo.Type.IMAGE);
		} else if (fileType == FileItem.FileType.VIDEO) {	//视频
			msgInfo.setMsgType(MsgInfo.Type.VIDEO);
		} else {
			msgInfo.setMsgType(MsgInfo.Type.FILE);
		}
		MsgPart msgPart = new MsgPart();
		msgPart.setFilePath(photoItem.getFilePath());
		msgPart.setFileName(photoItem.getFileName());
		msgPart.setThumbPath(photoItem.getThumbPath());
		msgPart.setMimeType(photoItem.getMime());
		msgPart.setSize(photoItem.getSize());

		msgInfo.setMsgPart(msgPart);

		ArrayList<MsgInfo> argMsgs = new ArrayList<>(1);
		argMsgs.add(msgInfo);
		Intent intent = new Intent(mContext, ChatChoseActivity.class);
		intent.putParcelableArrayListExtra(ChatChoseActivity.ARG_MSG_INFOS, argMsgs);
		intent.putExtra(ChatChoseActivity.ARG_SEND_OPT, ChatChoseActivity.OPT_FINISH);
		startActivity(intent);
	}
	
	/**
	 * 相片预览适配器
	 * @author huanghui1
	 * @update 2014年11月15日 上午10:49:30
	 */
	class PhotoFragmentViewPager extends FragmentStatePagerAdapter {

		public PhotoFragmentViewPager(FragmentManager fm) {
			super(fm);
		}

		@Override
		public android.support.v4.app.Fragment getItem(int position) {
			Bundle args = new Bundle();
			if (!SystemUtil.isEmpty(mPhotos)) {
				PhotoItem photoItem = mPhotos.get(position);
				if (msgInfo != null) {
					photoItem.setMsgId(msgInfo.getMsgId());
				}
				args.putParcelable(PhotoFragment.ARG_PHOTO, photoItem);
				args.putBoolean(PhotoFragment.ARG_TOUCH_FINISH, mOnTouchFinish);
			}
			return android.support.v4.app.Fragment.instantiate(mContext, PhotoFragment.class.getCanonicalName(), args);
		}

		@Override
		public int getCount() {
			return mPhotos.size();
		}

		@Override
		public int getItemPosition(Object object) {
			if (showMode == MODE_DISPLAY) {
				//解决调用adapter.notifyDataSetChanged方法后当前页的左右两边的fragment不刷新数据的问题
				return POSITION_NONE;
			} else {
				if (showMode == MODE_BROWSE && mQueryFlag != QUERY_FLAG_NONE) {
					return POSITION_NONE;
				} else {
					return super.getItemPosition(object);
				}
			}
		}
	}

	@Override
	public void onTap(View view, FileItem.FileType fileType, DownloadItem downloadItem) {
		if (FileItem.FileType.VIDEO == fileType) {	//视频文件，则打开
			if (downloadItem != null) {
				String filePath = downloadItem.getFilePath();
				File file = new File(filePath);
				if (SystemUtil.isFileExists(file)) {	//文件存在
					Intent intent = MimeUtils.getVideoFileIntent(file);
					if (intent != null && (intent.resolveActivity(getPackageManager()) != null)) {
						intent = Intent.createChooser(intent, SystemUtil.getFilename(filePath));
						startActivity(intent);
					} else {
						SystemUtil.makeShortToast(R.string.file_open_intent_error);
					}
				} else {
					SystemUtil.makeShortToast(R.string.file_not_exists);
				}
			}
		} else {
			if (showMode == MODE_DISPLAY) {	//图片查看模式
				finishAfterTransitionCompt();
			} else {
				if (mAppBar != null) {
					int bottomHeight = layoutBottom.getHeight();
					if (mShow) {	//hide
						mShow = false;
						ViewPropertyAnimatorCompatSet anim = new ViewPropertyAnimatorCompatSet();
						int height = mAppBar.getHeight();
						ViewPropertyAnimatorCompat toolBarAnim = ViewCompat.animate(mAppBar).translationY(-height).setInterpolator(new AccelerateInterpolator(2));
						ViewPropertyAnimatorCompat bottomAnim = ViewCompat.animate(layoutBottom).translationYBy(bottomHeight).setInterpolator(new AccelerateInterpolator(2));
						anim.play(toolBarAnim)
								.play(bottomAnim);
						anim.start();
						hideStatuBar();
					} else {
						mShow = true;
						ViewPropertyAnimatorCompatSet anim = new ViewPropertyAnimatorCompatSet();
						ViewPropertyAnimatorCompat bottomAnim = ViewCompat.animate(layoutBottom).translationYBy(-bottomHeight).setInterpolator(new AccelerateInterpolator(2));
						ViewPropertyAnimatorCompat toolBarAnim = ViewCompat.animate(mAppBar).translationY(0).setInterpolator(new DecelerateInterpolator(2));
						anim.play(toolBarAnim)
								.play(bottomAnim);
						anim.start();
						showStatuBar();
					}
				}
			}
		}
	}
	/**
	 * 加载消息图片的的后台任务
	 * @author tiger
	 * @update 2015/11/28 10:29
	 * @version 1.0.0
	 */
	class LoadImageMsgTask extends AsyncTask<Integer, Void, List<PhotoItem>> {

		@Override
		protected void onPreExecute() {
			if (mMoreView.getVisibility() == View.VISIBLE) {
				mMoreView.setVisibility(View.GONE);
			}
		}
		
		@Override
		protected List<PhotoItem> doInBackground(Integer... params) {
			List<PhotoItem> photoItems = null;
			if (SystemUtil.isNotEmpty(params)) {
				int threadId = params[0];
				PhotoItem currentItem = mPhotos.get(0);
				Map<String, Object> map = msgManager.getMsgImagesByThreadId(threadId, currentItem);
				if (map != null) {
					photoItems = (List<PhotoItem>) map.get("photoItems");
					currentPostion = (int) map.get("currentPosition");
				}
			}
			return photoItems;
		}

		@Override
		protected void onPostExecute(List<PhotoItem> photoItems) {
			if (photoItems != null) {
				mPhotos.clear();

				mPhotos.addAll(photoItems);
				if (photoAdapter != null) {
					photoAdapter.notifyDataSetChanged();
				} else {
					photoAdapter = new PhotoFragmentViewPager(getSupportFragmentManager());
					mViewPager.setAdapter(photoAdapter);
				}
				mViewPager.setCurrentItem(currentPostion, false);
				
				if (mMoreView.getVisibility() != View.VISIBLE) {
					mMoreView.setVisibility(View.VISIBLE);
					
					mHandler.sendEmptyMessageDelayed(Constants.MSG_HIDE_DELAY, HIDE_DELAY);
				}
			}
		}
	}
	
	/**
	 * 根据路径来加载该路径下的图片
	 * @author huanghui1
	 * @update 2015/12/21 14:03
	 * @version: 0.0.1
	 */
	class LoadImageTask extends AsyncTask<PhotoItem, Void, List<PhotoItem>> {

		@Override
		protected List<PhotoItem> doInBackground(PhotoItem... params) {
			List<PhotoItem> list = null;
			if (SystemUtil.isNotEmpty(params)) {
				PhotoItem currentItem = params[0];
				boolean loadAll = mQueryFlag == QUERY_FLAG_ALL;
				Map<String, Object> map = msgManager.getImagesByBucket(currentItem, loadAll);
				if (map != null) {
					list = (List<PhotoItem>) map.get("photoItems");
					currentPostion = (int) map.get("currentPosition");
					totalCount = list.size();
				}
			}
			return list;
		}

		@Override
		protected void onPostExecute(List<PhotoItem> photoItems) {
			if (photoItems != null) {
				mPhotos.clear();

				mPhotos.addAll(photoItems);
				if (photoAdapter != null) {
					photoAdapter.notifyDataSetChanged();
				} else {
					photoAdapter = new PhotoFragmentViewPager(getSupportFragmentManager());
					mViewPager.setAdapter(photoAdapter);
				}
				mViewPager.setCurrentItem(currentPostion, false);
				setTitle(getString(R.string.album_preview_photo_index, currentPostion + 1, totalCount));
			}
		}
	}
}
