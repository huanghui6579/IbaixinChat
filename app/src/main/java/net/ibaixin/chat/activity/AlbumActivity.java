package net.ibaixin.chat.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.view.ActionMode;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AbsListView.LayoutParams;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.download.ImageDownloader.Scheme;

import net.ibaixin.chat.R;
import net.ibaixin.chat.app.ActivityCompatICS;
import net.ibaixin.chat.app.ActivityOptionsCompatICS;
import net.ibaixin.chat.fragment.PhotoFragment;
import net.ibaixin.chat.manager.MsgManager;
import net.ibaixin.chat.model.Album;
import net.ibaixin.chat.model.FileItem;
import net.ibaixin.chat.model.MsgInfo;
import net.ibaixin.chat.model.MsgPart;
import net.ibaixin.chat.model.PhotoItem;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.DensityUtil;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.view.ProgressDialog;
import net.ibaixin.chat.view.ProgressWheel;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 图片选择界面
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年11月13日 下午9:08:50
 */
public class AlbumActivity extends BaseActivity implements OnClickListener {
	public static final int REQ_PARENT_MAKE_IMG_MSG = 10;
	public static final int REQ_PARENT_MAKE_VIDEO_MSG = 11;
	public static final int REQ_PARENT_CLIP_ICON = 12;
	
	public static final int REQ_PREVIEW_IMAGE = 13;
	public static final int REQ_TAKE_PIC = 14;
	public static final int REQ_CLIP_PIC = 15;
	public static final int REQ_TAKE_CLIP_PIC = 16;
	public static final int REQ_TAKE_VIDEO = 17;
	
	public static final String ARG_REQ_CODE = "arg_req_code";
	public static final String ARG_IS_IMAGE = "arg_is_image";
	public static final String ARG_MAX_SELECT_SIZE = "arg_max_select_size";
	public static final String ARG_IS_SINGLE_CHOICE = "arg_is_single_choice";

	public static final String ARG_ALBUM_MANAGER = "arg_album_manager";
	
	
	private ImageLoader mImageLoader = ImageLoader.getInstance();
	
	private MsgManager msgManager = MsgManager.getInstance();
	
	private GridView gvPhoto;
	private ProgressWheel pbLoading;
	private Button tvAllPhoto;
	private TextView tvPreview;
	private TextView tvTime;
	private MenuItem mMenuDone;
//	private TextView btnOpt;
	
	private RelativeLayout layoutBottom;
	
	/**
	 * 相册分组的listview
	 */
	private ListView lvAlbum;
	
	//单元格的宽
	public int columnWith = 0;
	/**
	 * 文件列表
	 */
	private ArrayList<PhotoItem> mPhotos = new ArrayList<>();
	/**
	 * 文件按分组的集合
	 */
	private Map<String, List<PhotoItem>> folderMap = new HashMap<>();
	
	private PhotoAdapter mPhotoAdapter;
	
	PopupWindow mPopupWindow;
	
	private ProgressDialog pDialog;

	static int[] screenSize = null;
	
	private MsgInfo msgInfo;
	
	/**
	 * 相片选择的最大数量,默认为9张
	 */
	private int mMaxSelectSize = Constants.ALBUM_SELECT_SIZE;
	
	/**
	 * 资源内容是否是图片，分为视频和图片两类
	 */
	private boolean isImage = true;
	
	/**
	 * 是否是单选模式
	 */
	private boolean mIsSingleChoice = false;
	
	/**
	 * 请求码，根据不同的请求码来返回不同的数据
	 */
	private int mReqCode;
	
	/**
	 * 拍照后照片存放的地址
	 */
	private String mFilePath;

	/**
	 * 是否管理某条会话中所有的图片消息
	 */
	private boolean mIsAlbumManager = false;
	
	private ActionMode mActionMode;
	
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if (pDialog != null && pDialog.isShowing()) {
				pDialog.dismiss();
			}
			switch (msg.what) {
			case Constants.MSG_SUCCESS:
				Intent data = new Intent();
				data.putParcelableArrayListExtra(ChatActivity.ARG_MSG_INFO_LIST, (ArrayList<MsgInfo>) msg.obj);
				setResult(RESULT_OK, data);
				finish();
				break;
			case Constants.MSG_FAILED:
				SystemUtil.makeShortToast(R.string.album_photo_chose_error);
				setResult(RESULT_CANCELED);
				finish();
				break;
			case Constants.MSG_UPDATE_ONE://局部更新一个文件
				PhotoItem photoItem = (PhotoItem) msg.obj;
				if (photoItem != null) {
					mPhotos.add(0, photoItem);
					Intent intent = new Intent(mContext, PhotoPreviewActivity.class);
					intent.putExtra(PhotoPreviewActivity.ARG_SHOW_MODE, PhotoPreviewActivity.MODE_BROWSE);
					intent.putParcelableArrayListExtra(PhotoPreviewActivity.ARG_PHOTO_LIST, mPhotos);
					intent.putExtra(ChatActivity.ARG_MSG_INFO, msgInfo);
					startActivityForResult(intent, REQ_PREVIEW_IMAGE);

					mPhotoAdapter.notifyDataSetChanged();
				}
				break;
			default:
				break;
			}
		}
		
	};

	@Override
	protected int getContentView() {
		return R.layout.activity_album;
	}
	
	@Override
	public boolean isSwipeBackEnabled() {
		return false;
	}

	@Override
	protected void initView() {
		gvPhoto = (GridView) findViewById(R.id.gv_photo);
		pbLoading = (ProgressWheel) findViewById(R.id.pb_loading);
		tvAllPhoto = (Button) findViewById(R.id.tv_all_photo);
		tvPreview = (TextView) findViewById(R.id.tv_preview);
		tvTime = (TextView) findViewById(R.id.tv_time);
		layoutBottom = (RelativeLayout) findViewById(R.id.layout_bottom);
		
		screenSize = SystemUtil.getScreenSize();
		
	}

	@Override
	protected void initData() {
		Intent intent = getIntent();
		isImage = intent.getBooleanExtra(ARG_IS_IMAGE, true);
		msgInfo = intent.getParcelableExtra(ChatActivity.ARG_MSG_INFO);
		mMaxSelectSize = intent.getIntExtra(ARG_MAX_SELECT_SIZE, Constants.ALBUM_SELECT_SIZE);
		mIsSingleChoice = intent.getBooleanExtra(ARG_IS_SINGLE_CHOICE, false);
		mReqCode = intent.getIntExtra(ARG_REQ_CODE, 0);

		mIsAlbumManager = intent.getBooleanExtra(ARG_ALBUM_MANAGER, false);
		
		if (mIsAlbumManager) {	//相册管理
			mPhotos = intent.getParcelableArrayListExtra(PhotoPreviewActivity.ARG_PHOTO_LIST);
			mIsSingleChoice = true;
			if (mPhotos == null) {
				mPhotos = new ArrayList<>();
			}
			layoutBottom.setVisibility(View.GONE);
			setTitle(R.string.album_image_msg);
			pbLoading.setVisibility(View.GONE);
		} else {
			if (mIsSingleChoice) {
				tvPreview.setVisibility(View.GONE);
			} else {
				tvPreview.setVisibility(View.VISIBLE);
			}

			if (!isImage) {	//不是图片，则不显示预览选项
				tvAllPhoto.setText(R.string.album_all_video);
				setTitle(R.string.activity_lable_video);
			}

			new LoadPhotoTask().execute();
		}

		mPhotoAdapter = new PhotoAdapter(mPhotos, mContext, isImage);
		gvPhoto.setAdapter(mPhotoAdapter);
		
	}
	
	/**
	 * 重置数据
	 * @author huanghui1
	 * @update 2015/12/4 11:39
	 * @version: 0.0.1
	 */
	private void resetData() {
		mFilePath = null;
		outBatchMode();
		mActionMode = null;
		
		if (mPopupWindow != null && mPopupWindow.isShowing()) {
			mPopupWindow.dismiss();
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

		resetData();

		initData();
	}

	@Override
	protected void addListener() {
		tvAllPhoto.setOnClickListener(this);
		tvPreview.setOnClickListener(this);
		gvPhoto.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
									int position, long id) {
				PhotoItem item = (PhotoItem) mPhotoAdapter.getItem(position);
				if (item != null) {
					if (item.isEmpty()) {    //拍照
						Intent intent = null;
						if (isImage) {    //
							int reqCode = 0;
							switch (mReqCode) {
								case REQ_PARENT_CLIP_ICON:    //裁剪图片的拍照
									reqCode = REQ_TAKE_CLIP_PIC;
									break;
								case REQ_PARENT_MAKE_IMG_MSG:    //选择图片的拍照
									reqCode = REQ_TAKE_PIC;
									break;
								default:
									break;
							}
							intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
							mFilePath = SystemUtil.generatePhotoPath();
							File file = new File(mFilePath);
							Uri uri = Uri.fromFile(file);
							intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);    //将照片保存到指定位置
							ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(view, 0, 0, view.getWidth(), view.getHeight());
							ActivityCompat.startActivityForResult(AlbumActivity.this, intent, reqCode, options.toBundle());
						} else {
							intent = new Intent();
							intent.setAction(MediaStore.ACTION_VIDEO_CAPTURE);
							mFilePath = SystemUtil.generateVideoPath();
							File file = new File(mFilePath);
							if (file.exists()) {
								file.delete();
							}
							Uri uri = Uri.fromFile(file);
							intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
							intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
							ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(view, 0, 0, view.getWidth(), view.getHeight());
							ActivityCompat.startActivityForResult(AlbumActivity.this, intent, REQ_TAKE_VIDEO, options.toBundle());
//						SystemUtil.makeShortToast("录视频");
						}
					} else {
						Intent intent = null;
						int reqCode = 0;
						boolean startForResult = true;
						if (mIsSingleChoice) {    //单选模式,裁剪图像
							if (mIsAlbumManager) {//相册管理模式
								intent = new Intent(mContext, PhotoPreviewActivity.class);
								intent.putExtra(PhotoPreviewActivity.ARG_SHOW_MODE, PhotoPreviewActivity.MODE_DISPLAY);
								intent.putExtra(PhotoFragment.ARG_TOUCH_FINISH, true);
								intent.putExtra(PhotoPreviewActivity.ARG_POSITION, position);
								intent.putParcelableArrayListExtra(PhotoPreviewActivity.ARG_PHOTO_LIST, mPhotos);
								intent.putExtra(ChatActivity.ARG_MSG_INFO, msgInfo);
								startForResult = false;
							} else {
								reqCode = REQ_CLIP_PIC;
								intent = new Intent(mContext, ClipHeadIconActivity.class);
								intent.putExtra(ClipHeadIconActivity.ARG_IMAGE_PATH, item.getFilePath());
							}
						} else {
							int argPosition = position - 1;
							if (mIsAlbumManager) {    //相册管理模式，没有拍照功能
								argPosition = position;
							}
							intent = new Intent(mContext, PhotoPreviewActivity.class);
							intent.putExtra(PhotoPreviewActivity.ARG_POSITION, argPosition);
							intent.putExtra(PhotoPreviewActivity.ARG_SHOW_MODE, PhotoPreviewActivity.MODE_BROWSE);
							intent.putParcelableArrayListExtra(PhotoPreviewActivity.ARG_PHOTO_LIST, mPhotos);
							intent.putExtra(ChatActivity.ARG_MSG_INFO, msgInfo);

							reqCode = REQ_PREVIEW_IMAGE;
						}
//					ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(view, 0, 0, view.getWidth(), view.getHeight());
//					ActivityCompat.startActivityForResult(AlbumActivity.this, intent, reqCode, options.toBundle());
						ActivityOptionsCompatICS options = ActivityOptionsCompatICS.makeScaleUpAnimation(view, 0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
						if (startForResult) {
							ActivityCompatICS.startActivityForResult(AlbumActivity.this, intent, reqCode, options.toBundle());
						} else {
							ActivityCompatICS.startActivity(AlbumActivity.this, intent, options.toBundle());
						}

					}
				}
			}
		});
		gvPhoto.setOnScrollListener(new AbsListView.OnScrollListener() {

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {    //闲置状态，没有滚动，则不显示时间
					AlphaAnimation animation = null;
					if (tvTime.getVisibility() == View.VISIBLE) {    //隐藏
						animation = (AlphaAnimation) AnimationUtils.loadAnimation(mContext, R.anim.album_time_fade_out);
						tvTime.setVisibility(View.GONE);
					} else {    //显示
						animation = (AlphaAnimation) AnimationUtils.loadAnimation(mContext, R.anim.album_time_fade_in);
						tvTime.setVisibility(View.VISIBLE);
					}
					tvTime.startAnimation(animation);
				} else {
					int firstVisibleItem = gvPhoto.getFirstVisiblePosition();
					if (firstVisibleItem == 0) {
						PhotoItem photo = mPhotos.get(firstVisibleItem);
						showAlbumTime(photo, tvTime);
					}
					if (tvTime.getVisibility() == View.GONE) {
						tvTime.setVisibility(View.VISIBLE);
					}
				}
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
								 int visibleItemCount, int totalItemCount) {
				if (firstVisibleItem > 0) {
					PhotoItem photo = (PhotoItem) mPhotoAdapter.getItem(firstVisibleItem);
					showAlbumTime(photo, tvTime);
				}
			}
		});
		gvPhoto.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				hideWindow(mPopupWindow);
				return false;
			}
		});
	}
	
	/**
	 * 显示相册的分割时间
	 * @author huanghui1
	 * @update 2015/12/1 10:56
	 * @version: 0.0.1
	 */
	private void showAlbumTime(PhotoItem item, TextView textView) {
		String preTime = textView.getText().toString();
		String curTime = SystemUtil.formatTime(item.getTime(), Constants.DATEFORMA_TPATTERN_ALBUM_TIP);
		if (TextUtils.isEmpty(preTime) || !curTime.equals(preTime)) {
			textView.setText(curTime);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (mIsAlbumManager || !mIsSingleChoice) {	//是图片且是多选模式下或者是会话的图片消息管理才加载该菜单
			MenuInflater menuInflater = getMenuInflater();
			menuInflater.inflate(R.menu.menu_save, menu);
			mMenuDone = menu.findItem(R.id.action_select_complete);

			if (mIsAlbumManager) {
				mMenuDone.setTitle(R.string.choice);
			} else {
				mMenuDone.setEnabled(false);
			}
		}
		
		/*mHandler.post(new Runnable() {
			
			@Override
			public void run() {
				View view = findViewById(R.id.action_select_complete);
				view.setBackgroundResource(R.drawable.common_button_grey_selector);
			}
		});*/
		
		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * 隐藏ActionMode
	 * @param actionMode actionMode
	 * @author tiger
	 * @update 2015/11/8 10:56
	 * @version 1.0.0
	 */
	private void finishActionMode(ActionMode actionMode) {
		if (actionMode != null) {
			actionMode.finish();
		}
	}
	
	/**
	 * 进入批量选择模式
	 * @author huanghui1
	 * @update 2015/12/1 17:56
	 * @version: 0.0.1
	 */
	private void initBatchMode() {
		mIsSingleChoice = false;
		if (mPhotoAdapter != null) {
			mPhotoAdapter.clearSelect();
		}
	}
	
	/**
	 * 退出批量模式
	 * @author huanghui1
	 * @update 2015/12/2 14:44
	 * @version: 0.0.1
	 */
	private void outBatchMode() {
		mIsSingleChoice = true;
		if (mPhotoAdapter != null) {
			mPhotoAdapter.clearSelect();
		}
		finishActionMode(mActionMode);
	}

	@Override
	public void onBackPressed() {
		if (!hideWindow(mPopupWindow)) {
			super.onBackPressed();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_select_complete:	//发送
			if (mIsAlbumManager) {	//会话的聊天图片管理
				mActionMode = startSupportActionMode(new ActionModeCallback() {
					@Override
					public boolean onCreateActionMode(ActionMode mode, Menu menu) {
						MenuInflater menuInflater = getMenuInflater();
						menuInflater.inflate(R.menu.menu_album_opt, menu);
						
						setSubMenuEnabled(menu, false);
						return true;
					}
					
					@Override
					public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
						switch (item.getItemId()) {
							case R.id.action_forward:	//转发
								if (mPhotoAdapter != null) {
									SystemUtil.getCachedThreadPool().execute(new Runnable() {
										@Override
										public void run() {
											ArrayList<MsgInfo> msgInfos = mPhotoAdapter.getSelectMsgInfos();
											if (SystemUtil.isNotEmpty(msgInfos)) {

											} else {
												Log.d("---not select photoitem---");
											}
										}
									});
								}
								break;
						}
						return false;
					}

					@Override
					public void onDestroyActionMode(ActionMode mode) {
						outBatchMode();
						super.onDestroyActionMode(mode);
					}
					
				});
				initBatchMode();
								
			} else {
				final List<PhotoItem> selects = mPhotoAdapter.getSelectList();
				pDialog = ProgressDialog.show(mContext, null, getString(R.string.chat_sending_file), true);
				//发送图片
				SystemUtil.getCachedThreadPool().execute(new Runnable() {

					@Override
					public void run() {
						ArrayList<MsgInfo> msgList = null;
						if (isImage) {	//图片消息
							msgList = msgManager.getMsgInfoListByPhotos(msgInfo, selects, false);
						} else {
							msgList = msgManager.getMsgInfoListByVideos(msgInfo, selects);
						}
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
			}
			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	/**
	 * 获取手机状态栏高度
	 * @update 2014年11月14日 下午4:08:56
	 * @param context
	 * @return
	 */
    public int getStatusBarHeight(Context context){
    	Rect frame = new Rect();  
    	getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);  
    	int statusBarHeight = frame.top;
    	return statusBarHeight;
    }
    
    /**
     * 获得ActionBar的高度
     * @return
     */
    public int getActionBarHeight() {
    	TypedValue tv = new TypedValue();
    	int actionBarHeight = 0;
    	if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
    	    actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
    	}
    	return actionBarHeight;
    }
    
    /**
     * 根据文件夹获取对应的相册的数据
     * @update 2014年11月14日 下午4:28:20
     * @param map
     * @param isImage 是否是图片
     * @return
     */
    private List<AlbumItem> getAlbumList(Map<String, List<PhotoItem>> map, boolean isImage) {
    	if (SystemUtil.isEmpty(map)) {
			return null;
		}
    	List<AlbumItem> list = new ArrayList<>();
    	int resId = R.string.album_all_photo;
    	if (!isImage) {	//视频
    		resId = R.string.album_all_video;
    	}
    	AlbumItem defaultAlbum = new AlbumItem(getString(resId), mPhotos.size(), mPhotos.get(0).getFilePath());
    	list.add(0, defaultAlbum);
    	Set<String> keys = map.keySet();
    	for (String key : keys) {
    		List<PhotoItem> temp = map.get(key);
    		String topPath = null;
    		if (isImage) {	//图片
    			topPath = temp.get(0).getFilePath();
			} else {	//视频
				String thumbPath = temp.get(0).getThumbPath();
				if (!SystemUtil.isFileExists(thumbPath)) {
					topPath = temp.get(0).getFilePath();
				} else {
					topPath = thumbPath;
				}
			}
    		AlbumItem item = new AlbumItem(key, temp.size(), topPath);
    		list.add(item);
		}
    	return list;
    }
	
	/**
	 * 显示弹出菜单
	 * @update 2014年11月14日 下午4:02:16
	 * @param author
	 */
	private void showPopupWindow(final View author) {
		if (mPopupWindow == null) {
			int bottomHeight = SystemUtil.getViewSize(layoutBottom)[1];
			int statusHeight = getStatusBarHeight(mContext);
			int actionBarHeight = getActionBarHeight();
			
			LayoutInflater inflater = LayoutInflater.from(mContext);
			View contentView = inflater.inflate(R.layout.layout_album_list, null);
			lvAlbum = (ListView) contentView.findViewById(R.id.lv_album);
			
			final List<AlbumItem> list = getAlbumList(folderMap, isImage);
			final AlbumAdapter albumAdapter = new AlbumAdapter(list, mContext);
			lvAlbum.setAdapter(albumAdapter);
			
			//列表距上面的距离
			int topSpacing = DensityUtil.dip2px(mContext, getResources().getDimension(R.dimen.album_list_top_spacing));
			
			int maxHeight = screenSize[1] - statusHeight - actionBarHeight - bottomHeight - topSpacing;
			
			int listViewHeight = SystemUtil.getListViewHeight(lvAlbum);
			if (listViewHeight > maxHeight) {
				listViewHeight = maxHeight;
			}
			
			lvAlbum.setOnItemClickListener(new AdapterView.OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					albumAdapter.setCurrentPosition(position);
					togglewindow(mPopupWindow, author);
					if (position == 0) {	//加载全部
						new LoadPhotoTask().execute();
					} else {
						AlbumItem albumItem = list.get(position);
						List<PhotoItem> temp = folderMap.get(albumItem.getAlbumName());
						mPhotos.clear();

						mPhotos.addAll(temp);
						resetActionMenu();
	
						mPhotoAdapter.clearSelect();
					}
				}
			});
			
			mPopupWindow = new PopupWindow(contentView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			mPopupWindow.setHeight(listViewHeight);
			mPopupWindow.setContentView(lvAlbum);
			mPopupWindow.setOutsideTouchable(false);
			mPopupWindow.setFocusable(false);
			mPopupWindow.update();
//			mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//			mPopupWindow.setBackgroundDrawable(new BitmapDrawable(getResources(), (Bitmap)null));
		}
		togglewindow(mPopupWindow, author);
	}
	
	/**
	 * 显示和隐藏相册菜单
	 * @update 2014年11月14日 下午5:44:37
	 * @param window
	 * @param anchor
	 */
	private void togglewindow(PopupWindow window, View anchor) {
		if(window.isShowing()) {
			window.dismiss();
		} else {
			window.showAsDropDown(anchor, 0, 0);
		}
	}

	/**
	 * 隐藏弹出窗
	 * @param popupWindow
	 * @return 是否隐藏成功，true:之前window是显示状态，隐藏成功了，false:之前window没有显示
	 */
	private boolean hideWindow(PopupWindow popupWindow) {
		if (popupWindow != null && popupWindow.isShowing()) {
			popupWindow.dismiss();
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * 恢复菜单原样
	 * @update 2014年11月14日 下午9:06:53
	 */
	private void resetActionMenu() {
		tvPreview.setEnabled(false);
		tvPreview.setText(R.string.album_preview_photo);
		if (mMenuDone != null) {
			mMenuDone.setEnabled(false);
			mMenuDone.setTitle(R.string.action_select_complete);
		}
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.tv_all_photo:	//所有相册菜单
			if (SystemUtil.isEmpty(mPhotos)) {
				SystemUtil.makeShortToast(R.string.album_no_data);
			} else {
				showPopupWindow(v);
			}
			break;
		case R.id.tv_preview:	//预览选中的图片
			ArrayList<PhotoItem> selects = mPhotoAdapter.getSelectList();
			Intent intent = new Intent(mContext, PhotoPreviewActivity.class);
			intent.putExtra(PhotoPreviewActivity.ARG_SHOW_MODE, PhotoPreviewActivity.MODE_CHOSE);
			intent.putParcelableArrayListExtra(PhotoPreviewActivity.ARG_PHOTO_LIST, selects);
			intent.putExtra(ChatActivity.ARG_MSG_INFO, msgInfo);
			startActivityForResult(intent, REQ_PREVIEW_IMAGE);
			break;
		default:
			break;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (RESULT_OK == resultCode) {	//成功
			Intent intent = null;
			switch (requestCode) {
			case REQ_PREVIEW_IMAGE:	//预览相片
			case REQ_CLIP_PIC:	//裁剪图片后的返回
				setResult(RESULT_OK, data);
				finish();
				break;
			case REQ_TAKE_PIC:	//调用系统相机拍照
				File file = new File(mFilePath);
				SystemUtil.scanFileAsync(mContext, file);
				PhotoItem photoItem = new PhotoItem();
				photoItem.setFileType(FileItem.FileType.IMAGE);
				photoItem.setFilePath(mFilePath);
				photoItem.setSize(file.length());
				photoItem.setTime(file.lastModified());
				mPhotos.add(0, photoItem);	//拍照或者录视频占第一位
				
				intent = new Intent(mContext, PhotoPreviewActivity.class);
				intent.putExtra(PhotoPreviewActivity.ARG_SHOW_MODE, PhotoPreviewActivity.MODE_BROWSE);
				intent.putParcelableArrayListExtra(PhotoPreviewActivity.ARG_PHOTO_LIST, mPhotos);
				intent.putExtra(ChatActivity.ARG_MSG_INFO, msgInfo);
				startActivityForResult(intent, REQ_PREVIEW_IMAGE);

				mPhotoAdapter.notifyDataSetChanged();
				break;
			case REQ_TAKE_CLIP_PIC:	//裁剪图片
				SystemUtil.scanFileAsync(mContext, mFilePath);
				intent = new Intent(mContext, ClipHeadIconActivity.class);
				intent.putExtra(ClipHeadIconActivity.ARG_IMAGE_PATH, mFilePath);
				startActivityForResult(intent, REQ_CLIP_PIC);
				break;
			case REQ_TAKE_VIDEO:	//录视频
				if (data != null) {
					pDialog = ProgressDialog.show(mContext, null, getString(R.string.loading), true);
					final Uri uri = data.getData();
					SystemUtil.getCachedThreadPool().execute(new Runnable() {
						@Override
						public void run() {
							String[] pathArray = msgManager.getVideoThumbPath(uri);
							if (pathArray != null) {
								//[0]存入的是原始文件的绝对路径
								mFilePath = pathArray[0];
								PhotoItem photoItem = new PhotoItem();
								photoItem.setFileType(FileItem.FileType.VIDEO);
								photoItem.setFilePath(mFilePath);
								File file = new File(mFilePath);
								photoItem.setThumbPath(pathArray[1]);
								photoItem.setSize(file.length());
								photoItem.setTime(file.lastModified());
								Log.d("----mFilePath----" + mFilePath);
								Message msg = mHandler.obtainMessage();
								msg.obj = photoItem;
								msg.what = Constants.MSG_UPDATE_ONE;
								mHandler.sendMessage(msg);
							}
						}
					});
				}
			break;
			default:
				break;
			}
		} else if (RESULT_CANCELED == resultCode) {
			
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	/**
	 * 异步加载图片的任务
	 * @author huanghui1
	 * @update 2014年11月13日 下午10:04:54
	 */
	class LoadPhotoTask extends AsyncTask<String, Void, List<PhotoItem>> {
		
		@Override
		protected void onPreExecute() {
			if (pbLoading.getVisibility() == View.GONE) {
				pbLoading.setVisibility(View.VISIBLE);
			}
			mPhotos.clear();
			folderMap.clear();
		}

		@Override
		protected List<PhotoItem> doInBackground(String... params) {
			if (SystemUtil.isEmpty(params)) {	//默认加载全部
				Album album = msgManager.getAlbum(isImage);
				if (album != null) {
					List<PhotoItem> list = album.getmPhotos();
					folderMap = album.getFolderMap();
					return list;
				}
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(List<PhotoItem> result) {
			mPhotos.clear();
			if (!SystemUtil.isEmpty(result)) {
				mPhotos.addAll(result);
			} else {
				if (tvAllPhoto.getVisibility() == View.VISIBLE) {
					tvAllPhoto.setVisibility(View.GONE);
				}
			}
			if (pbLoading.getVisibility() == View.VISIBLE) {
				pbLoading.setVisibility(View.GONE);
			}
			mPhotoAdapter.notifyDataSetChanged();
		}
		
	}
	
	/**
	 * 相册适配器
	 * @author huanghui1
	 * @update 2014年11月14日 下午4:56:09
	 */
	class AlbumAdapter extends CommonAdapter<AlbumItem> {
		DisplayImageOptions options = SystemUtil.getAlbumImageOptions();
		private int currentPosition = 0;

		public AlbumAdapter(List<AlbumItem> list, Context context) {
			super(list, context);
		}
		
		public void setCurrentPosition(int currentPosition) {
			this.currentPosition = currentPosition;
			notifyDataSetChanged();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			AlbumViewHolder holder = null;
			if (convertView == null) {
				holder = new AlbumViewHolder();
				convertView = inflater.inflate(R.layout.item_album, parent, false);
				
				holder.ivCon = (ImageView) convertView.findViewById(R.id.iv_icon);
				holder.tvContent = (CheckedTextView) convertView.findViewById(R.id.tv_content);
				holder.ivFlag = (ImageView) convertView.findViewById(R.id.iv_flag);
				
				convertView.setTag(holder);
			} else {
				holder = (AlbumViewHolder) convertView.getTag();
			}
			
			final AlbumItem albumItem = list.get(position);
			String albumName = albumItem.getAlbumName();
			int count = albumItem.getPhotoCount();
			int resId = R.string.album_item_content;
			if (!isImage) {	//视频
				holder.ivFlag.setVisibility(View.VISIBLE);
				resId = R.string.album_item_video_content;
			} else {
				holder.ivFlag.setVisibility(View.GONE);
			}
			String str = getString(resId, albumName, count);
			SpannableStringBuilder spannableString = new SpannableStringBuilder(str);
			spannableString.setSpan(new TextAppearanceSpan(context, R.style.AlbumItemTitleStyle), 0, albumName.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
			spannableString.setSpan(new TextAppearanceSpan(context, R.style.AlbumItemSubTitleStyle), albumName.length(), str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
			holder.tvContent.setText(spannableString, TextView.BufferType.SPANNABLE);
			String filePath = albumItem.getTopPhotoPath();
			holder.ivCon.setTag(filePath);
			mImageLoader.displayImage(Scheme.FILE.wrap(filePath), holder.ivCon, options);
			
			if (position == currentPosition) {	//与上次点击的不同
				holder.tvContent.setChecked(true);
			} else {
				holder.tvContent.setChecked(false);
			}
			
			return convertView;
		}
		
	}
	
	final class AlbumViewHolder {
		ImageView ivCon;
		CheckedTextView tvContent;
		ImageView ivFlag;
	}
	
	/**
	 * 图片的适配器
	 * @author huanghui1
	 * @update 2014年11月13日 下午9:12:17
	 */
	class PhotoAdapter extends CommonAdapter<PhotoItem> {
		DisplayImageOptions options = null;
		
		private SparseBooleanArray selectArray = new SparseBooleanArray();
		int selectSize = 0;
		
		/**
		 * 显示的是否是图片，不是图片就是视频
		 */
		private boolean isImage;
		
		public PhotoAdapter(List<PhotoItem> list, Context context, boolean isImage) {
			super(list, context);
			this.isImage = isImage;
			
			if (isImage) {
				options = SystemUtil.getAlbumImageOptions();
			} else {
				options = SystemUtil.getAlbumVideoOptions();
			}
		}
		
		/**
		 * 清除所选项
		 * @update 2014年11月14日 下午9:09:06
		 */
		public void clearSelect() {
			clearSelect(true);
		}
		
		/**
		 * 清除所选项
		 * @param notify 是否刷新界面
		 * @author huanghui1
		 * @update 2015/12/1 18:01
		 * @version: 0.0.1
		 */
		public void clearSelect(boolean notify) {
			selectArray.clear();
			selectSize = 0;
			if (notify) {
				notifyDataSetChanged();
			}
		}
		
		/**
		 * 获得所选中的图片列表
		 * @update 2014年11月14日 下午10:18:54
		 * @return
		 */
		public ArrayList<PhotoItem> getSelectList() {
			ArrayList<PhotoItem> selects = new ArrayList<>();
			int len = selectArray.size();
			for (int i = 0; i < len; i++) {
				boolean value = selectArray.valueAt(i);
				if (value) {
					int position = selectArray.keyAt(i);
					if (mIsAlbumManager) {
						selects.add(mPhotos.get(position));
					} else {
						selects.add(mPhotos.get(position - 1));
					}
				}
			}
			return selects;
		}
		
		/**
		 * 将所选择的图片组装成一组消息
		 * @author tiger
		 * @update 2015/12/12 11:02
		 * @version 1.0.0
		 * @return 返回一组选择的消息
		 */
		public ArrayList<MsgInfo> getSelectMsgInfos() {
			if (mIsAlbumManager) {
				ArrayList<MsgInfo> selects = new ArrayList<>();
				int len = selectArray.size();
				for (int i = 0; i < len; i++) {
					boolean value = selectArray.valueAt(i);
					if (value) {
						int position = selectArray.keyAt(i);
						PhotoItem photoItem = mPhotos.get(position);

						MsgInfo msgInfo = new MsgInfo();
						FileItem.FileType fileType = photoItem.getFileType();
						switch (fileType) {
							case IMAGE:	//图片
								msgInfo.setMsgType(MsgInfo.Type.IMAGE);
								break;
							case VIDEO:	//视频
								msgInfo.setMsgType(MsgInfo.Type.VIDEO);
								break;
							default:	//其他
								msgInfo.setMsgType(MsgInfo.Type.FILE);
								break;
						}
						MsgPart msgPart = new MsgPart();
						msgPart.setThumbPath(photoItem.getThumbPath());
						msgPart.setFileName(photoItem.getFileName());
						msgPart.setFilePath(photoItem.getFilePath());
						msgPart.setMimeType(photoItem.getMime());
						msgPart.setSize(photoItem.getSize());

						msgInfo.setMsgPart(msgPart);

						selects.add(msgInfo);
					}
				}
				return selects;
			}
			return null;
		}
		
		@Override
		public Object getItem(int position) {
			if (list == null) {
				return null;
			} else {
				if (mIsAlbumManager) {
					return list.get(position);
				} else {
					if (position == 0) {
						return null;
					} else {
						return list.get(position - 1);
					}
				}
			}
		}

		@Override
		public int getCount() {
			if (mIsAlbumManager) {	//相册管理模式没有拍照功能
				return list.size();
			} else {
				return list.size() + 1;
			}
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			PhotoViewHolder holder = null;
			if (convertView == null) {
				if (columnWith == 0) {
					columnWith = (int)((float)(screenSize[0] - DensityUtil.dip2px(mContext, 1.0f) * 2) / (float) gvPhoto.getNumColumns());
				}
				gvPhoto.setColumnWidth(columnWith);
				holder = new PhotoViewHolder();
				convertView = inflater.inflate(R.layout.item_photo, parent, false);
				
				holder.ivPhoto = (ImageView) convertView.findViewById(R.id.iv_photo);
				holder.viewAplha = convertView.findViewById(R.id.view_alpha);
				holder.cbChose = (CheckBox) convertView.findViewById(R.id.cb_chose);
				holder.ivFlag = (ImageView) convertView.findViewById(R.id.iv_flag);
				
				FrameLayout frameLayout = (FrameLayout) convertView.findViewById(R.id.layout_item);
				LayoutParams layoutParams = (LayoutParams) frameLayout.getLayoutParams();
				layoutParams.width = columnWith;
				layoutParams.height = columnWith;
				frameLayout.setLayoutParams(layoutParams);
				
				convertView.setTag(holder);
			} else {
				holder = (PhotoViewHolder) convertView.getTag();
			}
			
			if (position == 0 && !mIsAlbumManager) {	//拍照
				holder.viewAplha.setVisibility(View.GONE);
				holder.cbChose.setVisibility(View.GONE);
				holder.ivPhoto.setScaleType(ScaleType.CENTER);
				holder.ivPhoto.setImageResource(R.drawable.album_take_pic_selector);
				holder.ivPhoto.setBackgroundResource(R.drawable.album_take_pic_bg_normal);
			} else {
				PhotoItem photoItem = (PhotoItem) getItem(position);
				if (photoItem != null) {
					holder.ivPhoto.setBackgroundResource(0);
//				holder.ivPhoto.setScaleType(ScaleType.FIT_XY);

					String filePath = null;
					if (mIsSingleChoice) {
						holder.cbChose.setVisibility(View.GONE);
					} else {
						holder.cbChose.setVisibility(View.VISIBLE);
					}
					holder.cbChose.setOnCheckedChangeListener(null);
					holder.cbChose.setChecked((selectArray.indexOfKey(position) >= 0) ? selectArray.get(position) : false);
					if (holder.cbChose.isChecked()) {
						holder.viewAplha.setVisibility(View.VISIBLE);
					} else {
						holder.viewAplha.setVisibility(View.GONE);
					}
					if (mIsAlbumManager) {
						holder.cbChose.setOnCheckedChangeListener(new OnCheckedChangeListenerImpl(holder, position, false));
					} else {
						holder.cbChose.setOnCheckedChangeListener(new OnCheckedChangeListenerImpl(holder, position));
					}
					if (isImage) {
						holder.ivFlag.setVisibility(View.GONE);
						filePath = photoItem.getFilePath();
					} else {
						holder.ivFlag.setVisibility(View.VISIBLE);
						holder.viewAplha.setVisibility(View.GONE);
						String thumbPath = photoItem.getThumbPath();
						if (!SystemUtil.isFileExists(thumbPath)) {	//文件不存在
							filePath = photoItem.getFilePath();
						} else {
							filePath = thumbPath;
						}
					}
					String uri = null;
					if (!TextUtils.isEmpty(filePath)) {
						uri = Scheme.FILE.wrap(filePath);
					}
					mImageLoader.displayImage(uri, holder.ivPhoto, options);
				}

			}
			return convertView;
		}
		
		/**
		 * 相片选择的监听器
		 * @author huanghui1
		 * @update 2014年11月14日 上午10:47:48
		 */
		class OnCheckedChangeListenerImpl implements CompoundButton.OnCheckedChangeListener {
			PhotoViewHolder holder;
			int position;
			/**
			 * 上限 
			 */
			boolean isLimited = true;
			
			public OnCheckedChangeListenerImpl(PhotoViewHolder holder,
					int position) {
				this(holder, position, true);
			}
			
			public OnCheckedChangeListenerImpl(PhotoViewHolder holder,
					int position, boolean isLimited) {
				super();
				this.holder = holder;
				this.position = position;
				this.isLimited = isLimited;
			}

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					selectSize ++;
				} else {
					selectSize --;
				}
				if (isLimited) {	//对于图片的选择有限制
					if (selectSize <= mMaxSelectSize) {	//少于9张
						selectSize = selectSize < 0 ? 0 : selectSize;
						if (isChecked) {
							holder.viewAplha.setVisibility(View.VISIBLE);
						} else {
							holder.viewAplha.setVisibility(View.GONE);
						}
						selectArray.put(position, isChecked);
						if (selectSize == 0) {	//没有图片选中
							resetActionMenu();
						} else {
							tvPreview.setEnabled(true);
							tvPreview.setText(getString(R.string.album_preview_photo_num, selectSize));
							if (mMenuDone != null) {
								mMenuDone.setEnabled(true);
								mMenuDone.setTitle(getString(R.string.action_select_complete) + "(" + selectSize + "/" + mMaxSelectSize + ")");
							}

						}
					} else {	//多于9张
						selectSize = selectSize > mMaxSelectSize ? mMaxSelectSize : selectSize;
						tvPreview.setEnabled(false);
						holder.cbChose.setChecked(false);
						int resId = 0;
						if (isImage) {	//选择的是图片
							resId = R.string.album_tip_max_select;
						} else {
							resId = R.string.album_video_tip_max_select;
						}
						SystemUtil.makeShortToast(getString(resId, mMaxSelectSize));
					}
				} else {
					selectSize = selectSize < 0 ? 0 : selectSize;
					if (isChecked) {
						holder.viewAplha.setVisibility(View.VISIBLE);
					} else {
						holder.viewAplha.setVisibility(View.GONE);
					}
					selectArray.put(position, isChecked);
					if (selectSize == 0) {	//没有图片选中
						if (mActionMode != null) {
							mActionMode.setTitle(null);
							Menu actionMenu = mActionMode.getMenu();
							setSubMenuEnabled(actionMenu, false);
						}
					} else {
						mActionMode.setTitle(String.valueOf(selectSize));
						if (mActionMode != null) {
							Menu actionMenu = mActionMode.getMenu();
							setSubMenuEnabled(actionMenu, true);
						}
					}
				}
			}
			
		}
		
	}
	
	/**
	 * 设置菜单的子菜单是否可用
	 * @param menu 父菜单
	 * @param enabled 是否可用 
	 * @author huanghui1
	 * @update 2015/12/2 14:38
	 * @version: 0.0.1
	 */
	private void setSubMenuEnabled(Menu menu, boolean enabled) {
		if (menu != null) {
			int menuSize = menu.size();
			for (int i = 0; i < menuSize; i++) {
				MenuItem menuItem = menu.getItem(i);
				if (menuItem != null) {
					menuItem.setEnabled(enabled);
				}
			}
		}
	}
	
	final class PhotoViewHolder {
		ImageView ivPhoto;
		ImageView ivFlag;
		View viewAplha;
		CheckBox cbChose;
	}
	
	/**
	 * 相册的实体
	 * @author huanghui1
	 * @version 1.0.0
	 * @update 2014年11月14日 下午4:18:06
	 */
	class AlbumItem {
		/**
		 * 相册名称，分组名称
		 */
		private String albumName;
		/**
		 * 相册里的相片数量
		 */
		private int photoCount;
		/**
		 * 第一张相片的本地完整路径
		 */
		private String topPhotoPath;

		public String getAlbumName() {
			return albumName;
		}

		public void setAlbumName(String albumName) {
			this.albumName = albumName;
		}

		public int getPhotoCount() {
			return photoCount;
		}

		public void setPhotoCount(int photoCount) {
			this.photoCount = photoCount;
		}

		public String getTopPhotoPath() {
			return topPhotoPath;
		}

		public void setTopPhotoPath(String topPhotoPath) {
			this.topPhotoPath = topPhotoPath;
		}

		public AlbumItem(String albumName, int photoCount, String topPhotoPath) {
			super();
			this.albumName = albumName;
			this.photoCount = photoCount;
			this.topPhotoPath = topPhotoPath;
		}

		public AlbumItem() {
			super();
		}

		@Override
		public String toString() {
			return "AlbumItem [albumName=" + albumName + ", photoCount=" + photoCount + ", topPhotoPath=" + topPhotoPath
					+ "]";
		}
		
	}
	
}
