package net.ibaixin.chat.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import net.ibaixin.chat.R;
import net.ibaixin.chat.fragment.PhotoFragment;
import net.ibaixin.chat.manager.MsgManager;
import net.ibaixin.chat.model.DownloadItem;
import net.ibaixin.chat.model.FileItem;
import net.ibaixin.chat.model.MsgInfo;
import net.ibaixin.chat.model.PhotoItem;
import net.ibaixin.chat.util.Constants;
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
public class PhotoPreviewActivity extends BaseActivity implements PhotoFragment.OnViewTapListener {
	public static final String ARG_PHOTO_LIST = "arg_photo_list";
	public static final String ARG_POSITION = "arg_position";
	public static final String ARG_SHOW_MODE = "arg_show_mode";
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
	private List<PhotoItem> mPhotos;
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
	
	private boolean mShow = true;
	
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Constants.MSG_SUCCESS:
				Intent data = new Intent();
				data.putExtra(ARG_ORIGINAO_IMAGE, cbOrigianlImage.isChecked());
				data.putParcelableArrayListExtra(ChatActivity.ARG_MSG_INFO_LIST, (ArrayList<MsgInfo>)msg.obj);
				setResult(RESULT_OK, data);
				break;
			case Constants.MSG_FAILED:
				SystemUtil.makeShortToast(R.string.album_photo_chose_error);
				setResult(RESULT_CANCELED);
				break;
			default:
				break;
			}
			if (pDialog != null && pDialog.isShowing()) {
				pDialog.dismiss();
			}
			finish();
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
		photoAdapter = new PhotoFragmentViewPager(getSupportFragmentManager());
		mViewPager.setAdapter(photoAdapter);
		if (currentPostion != 0) {
			mViewPager.setCurrentItem(currentPostion);
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
			if (msgInfo != null) {
				AsyncTaskCompat.executeParallel(new LoadImageMsgTask(), msgInfo.getThreadID());
			}
			layoutBottom.setVisibility(View.GONE);
			fullScreen(true);
			mAppBar.setVisibility(View.GONE);
		}
		totalCount = mPhotos.size();
		setTitle(getString(R.string.album_preview_photo_index, currentPostion + 1, totalCount));
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

	@Override
	public void onBackPressed() {
		ActivityCompat.finishAfterTransition(this);
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
				// TODO Auto-generated method stub
				currentPostion = position;
				setTitle(getString(R.string.album_preview_photo_index, currentPostion + 1, totalCount));
				cbChose.setOnCheckedChangeListener(null);
				cbChose.setChecked(selectArray.indexOfKey(position) >= 0 ? selectArray.get(position) : false);
				addCheckImageListener();
				if (mMenuDone != null) {
					updateBtnOpt(selectCount);
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
				
			}
		});
		
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
				if (photoItem.getFileType() == FileItem.FileType.VIDEO) {	//视频文件
					cbOrigianlImage.setVisibility(View.GONE);
					mTvFileSize.setVisibility(View.VISIBLE);
					mTvFileSize.setText(getString(R.string.album_video_size, SystemUtil.sizeToString(photoItem.getSize())));
				}
				args.putParcelable(PhotoFragment.ARG_PHOTO, photoItem);
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
				return super.getItemPosition(object);
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
					} else {
						mShow = true;
						ViewPropertyAnimatorCompatSet anim = new ViewPropertyAnimatorCompatSet();
						ViewPropertyAnimatorCompat bottomAnim = ViewCompat.animate(layoutBottom).translationYBy(-bottomHeight).setInterpolator(new AccelerateInterpolator(2));
						ViewPropertyAnimatorCompat toolBarAnim = ViewCompat.animate(mAppBar).translationY(0).setInterpolator(new DecelerateInterpolator(2));
						anim.play(toolBarAnim)
								.play(bottomAnim);
						anim.start();
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

			}
		}
	}
}
