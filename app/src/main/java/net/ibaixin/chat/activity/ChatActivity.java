package net.ibaixin.chat.activity;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.support.v7.internal.view.ViewPropertyAnimatorCompatSet;
import android.support.v7.internal.widget.TintManager;
import android.support.v7.view.ActionMode;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.TextAppearanceSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.util.DialogUtils;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.download.ImageDownloader.Scheme;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.R;
import net.ibaixin.chat.fragment.EmojiFragment;
import net.ibaixin.chat.fragment.EmojiTypeFragment;
import net.ibaixin.chat.fragment.PhotoFragment;
import net.ibaixin.chat.manager.MsgManager;
import net.ibaixin.chat.model.AttachItem;
import net.ibaixin.chat.model.ContextMenuItem;
import net.ibaixin.chat.model.EmojiType;
import net.ibaixin.chat.model.FileItem;
import net.ibaixin.chat.model.MsgInfo;
import net.ibaixin.chat.model.MsgInfo.SendState;
import net.ibaixin.chat.model.MsgInfo.Type;
import net.ibaixin.chat.model.MsgPart;
import net.ibaixin.chat.model.MsgSenderInfo;
import net.ibaixin.chat.model.MsgThread;
import net.ibaixin.chat.model.MsgUploadInfo;
import net.ibaixin.chat.model.Personal;
import net.ibaixin.chat.model.User;
import net.ibaixin.chat.model.UserVcard;
import net.ibaixin.chat.model.emoji.Emojicon;
import net.ibaixin.chat.provider.Provider;
import net.ibaixin.chat.record.AudioRecorder;
import net.ibaixin.chat.service.CoreService;
import net.ibaixin.chat.service.CoreService.CoreReceiver;
import net.ibaixin.chat.service.CoreService.MainBinder;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.DensityUtil;
import net.ibaixin.chat.util.ImageUtil;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.MimeUtils;
import net.ibaixin.chat.util.Observable;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.util.XmppConnectionManager;
import net.ibaixin.chat.view.ProgressDialog;
import net.ibaixin.chat.view.RecordButton;
import net.ibaixin.chat.view.TextViewAware;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jxmpp.util.XmppStringUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 聊天界面
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年10月25日 上午10:38:11
 */
public class ChatActivity extends BaseActivity implements OnClickListener/*, OnItemClickListener*/, EmojiFragment.OnEmojiconClickedListener, EmojiTypeFragment.OnEmojiconBackspaceClickedListener {
	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

	public static final String ARG_MSG_INFO = "arg_msg_info";
	public static final String ARG_MSG_INFO_LIST = "arg_msg_info_list";
	public static final String ARG_THREAD_ID = "arg_thread_id";

	/**
	 * 调用相册的请请求码
	 */
//	public static final int REQ_ALBUM = 100;
	/**
	 * 调用视频的请请求码
	 */
//	public static final int REQ_VIDEO = 101;
	/**
	 * 调用文件的请请求码
	 */
	public static final int REQ_FILE = 102;
	/**
	 * 调用音频的请请求码
	 */
	public static final int REQ_AUDIO = 103;
	/**
	 * 地理位置请求码
	 */
	public static final int REQ_LOCATION = 104;
	
	/**
	 * 默认的编辑模式，文本框内没有任何内容
	 */
	private static final int MODE_DEFAULT = 0;
	/**
	 * 表情选择模式，此时，会显示表情选择面板
	 */
//	private static final int MODE_EMOJI = 1;
	
	/**
	 * 语音发送模式，此时会显示录音按钮
	 */
	private static final int MODE_VOICE = 1;
	
	/**
	 * 附件模式，此时会显示附件的选择面板
	 */
	private static final int MODE_ATTACH = 2;
	
	/**
	 * 发送模式，此时同时会显示文本输入框，但文本框里有内容
	 */
	private static final int MODE_SEND = 3;
	
	public static final String ARG_THREAD = "arg_thread";
	
	/**
	 * 菜单项：复制
	 */
	private static final int MENU_COPY = 0x1;
	/**
	 * 菜单项：转发
	 */
	private static final int MENU_FORWARD = 0x2;
	/**
	 * 菜单项：删除
	 */
	private static final int MENU_DELETE = 0x3;
	/**
	 * 菜单项：分享
	 */
	private static final int MENU_SHARE = 0x4;
	/**
	 * 菜单项：更多
	 */
	private static final int MENU_MORE = 0x5;
	
	/**
	 * 聊天的对方
	 */
	private User otherSide = null;
	private Personal mine = null;
	
	/**
	 * 当前的会话
	 */
	private MsgThread msgThread;
	
	/**
	 * 会话id
	 */
	private int mThreadId;
	
	private MsgManager msgManager = MsgManager.getInstance();
	
	/**
	 * 编辑模式
	 */
	private int editMode = MODE_DEFAULT;
	
	private static int[] attachItemRes = {
		R.drawable.att_item_image,
		R.drawable.att_item_audio,
		R.drawable.att_item_video,
		R.drawable.att_item_location,
		R.drawable.att_item_vcard,
		R.drawable.att_item_file
	};
	
	private static String[] attachItemNames;
	
	/**
	 * 是否正在显示表情面板
	 */
	private boolean isEmojiShow = false;
	
	private ListView lvMsgs;
	private TextView btnVoice;
	private TextView btnSend;
	private TextView btnEmoji;
	private /*Emojicon*/EditText etContent;
	
	/**
	 * 输入框底部的面板
	 */
	private FrameLayout layoutBottom;
	/**
	 * 底部编辑部分
	 */
	private View layoutContent;
	/**
	 * 表情面板
	 */
	private FrameLayout layoutEmoji;
	/**
	 * 中间的消息输入框
	 */
	private RelativeLayout layoutEdit;
	/**
	 * 语音模式下按住说话按钮
	 */
	private RecordButton btnMakeVoice;
	
	/**
	 * 录音的根布局
	 */
//	private View recordRootLayout;
	
//	private FragmentTabHost mTabHost;
	
	/**
	 * 附件面板
	 */
	private GridView gvAttach;
	
//	private LinearLayout layoutVoiceRecording;
//	private LinearLayout layoutRecord;
	/**
	 * 声音大小
	 */
//	private ImageView ivVolume;
	/**
	 * 取消录音的提示图片
	 */
//	private ImageView ivCancelTip;
//	private ImageView ivDelTip;
	/**
	 * 删除录音
	 */
//	private LinearLayout layoutDelRecord;
//	private LinearLayout layoutVoiceRecordLoading;
//	private LinearLayout layoutVoiceRecordTooshort;
	
//	private SoundMeter mSensor;
	
	private AttachPannelAdapter attachPannelAdapter;
	
	/**
	 * 添加附件的数据
	 */
	private List<AttachItem> mAttachItems = new ArrayList<>();
	
	private LinkedList<MsgInfo> mMsgInfos = new LinkedList<>();
	
	private MsgAdapter msgAdapter;
	
	/**
	 * 消息处理的广播
	 */
	MsgProcessReceiver msgProcessReceiver;
	
	/**
	 * 录音开始时间
	 */
	private long recordStartTime = 0L;
	/**
	 * 录音结束时间
	 */
	private long recordEndTime = 0L;
	
	/**
	 * 录音文件的全路径名，含文件名
	 */
//	private File volumeFile;
	
	/**
	 * 录音时间是否太短
	 */
//	private boolean isShort;
	
	/**
	 * 语音是否正在播放
	 */
	private boolean mIsPlaying = false;
	/**
	 * 当前播放语音的位置
	 */
	private int mPlayingPosition = -1;
	/**
	 * 语音播放器
	 */
	private MediaPlayer mPlayer = null;
	/**
	 * 当前播放语音的view
	 */
	private TextView mPlayingView;
	private int mPlayingType;
	/**
	 * 播放语音的动画
	 */
	private AnimationDrawable mPlayingAnimation;
	
	private ProgressDialog pDialog;
	
	/**
	 * 该会话的消息总数量
	 */
	private long mMsgTotalCount = 0;
	/**
	 * 加载更多是否加载完毕
	 */
	private boolean mLoadFinish = true;
	/**
	 * listview加载更多的头部view
	 */
	private View headView;
	/**
	 * 第一个可见项索引
	 */
	private int mFirstVisiableItem = 0;
//	private static final int POLL_INTERVAL = 300;
	/**
	 * 屏幕尺寸
	 */
	public static int[] screenSize = null;
	
	private AbstractXMPPConnection connection;
	private ChatManager chatManager = null;
	private Chat chat = null;
	
	//图片加载器
	private ImageLoader mImageLoader = null;
	
	CoreService coreService;
	
	private LocalBroadcastManager mLocalBroadcastManager;
	
	private ChatState mChatState;

	private MsgContentObserver mMsgContentObserver;
	
	private ActionMode mActionMode;

	/**
	 * 批量选择的集合
	 */
	private Map<String, Boolean> mSelectMap = null;

	/**
	 * 消息选中的条数
	 */
	private long mSelectSize = 0;
	
	/**
	 * 是否是批量模式
	 */
	private boolean mIsBatchMode;

	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case Constants.MSG_MODIFY_CHAT_MSG_SEND_STATE:	//改变聊天消息的发送状态
				msgAdapter.notifyDataSetChanged();
				scrollMyListViewToBottom(lvMsgs);
				break;
			case Constants.MSG_SUCCESS:	//删除成功
				int arg = msg.arg1;
				if (arg == 1) {	//隐藏actionMode
					finishActionMode(mActionMode);
				}
				msgAdapter.notifyDataSetChanged();
				break;
			case Constants.MSG_FAILED:	//删除失败
				SystemUtil.makeShortToast(R.string.delete_failed);
				break;
			default:
				break;
			}
		}
	};
	
	private ServiceConnection serviceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			MainBinder mBinder = (MainBinder) service;
			coreService = mBinder.getService();
			
			//清除聊天消息的通知栏
			clearChatNotify();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			
		}
		
	};
	
	@Override
	protected int getContentView() {
		// TODO Auto-generated method stub
		return R.layout.activity_chat;
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	protected void initView() {
		lvMsgs = (ListView) findViewById(R.id.lv_msgs);
		btnVoice = (TextView) findViewById(R.id.btn_voice);
		btnEmoji = (TextView) findViewById(R.id.btn_emoji);
		btnSend = (TextView) findViewById(R.id.btn_send);
		
		etContent = (/*Emojicon*/EditText) findViewById(R.id.et_content);
		layoutBottom = (FrameLayout) findViewById(R.id.layout_bottom);
		layoutEmoji = (FrameLayout) findViewById(R.id.layout_emoji);
		layoutContent = findViewById(R.id.layout_content);
		
		btnMakeVoice = (RecordButton) findViewById(R.id.btn_make_voice);
		layoutEdit = (RelativeLayout) findViewById(R.id.layout_edit);
		
//		recordRootLayout = findViewById(R.id.record_root_layout);
		
		gvAttach = (GridView) findViewById(R.id.gv_attach);
		
//		mTabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
//		mTabHost.setup(mContext, getSupportFragmentManager(), R.id.realtabcontent);
		
//		mTabHost.getTabWidget().setShowDividers(LinearLayout.SHOW_DIVIDER_NONE);
//		mTabHost.getTabWidget().setDividerDrawable(R.drawable.list_divider_drawable);
		
//		layoutVoiceRecording = (LinearLayout) findViewById(R.id.layout_voice_recording);
//		layoutDelRecord = (LinearLayout) findViewById(R.id.layout_del_record);
//		layoutRecord = (LinearLayout) findViewById(R.id.layout_record);
//		layoutVoiceRecordLoading = (LinearLayout) findViewById(R.id.layout_voice_record_loading);
//		layoutVoiceRecordTooshort = (LinearLayout) findViewById(R.id.layout_voice_record_tooshort);
//		ivVolume = (ImageView) findViewById(R.id.iv_volume);
//		ivCancelTip = (ImageView) findViewById(R.id.iv_cancel_tip);
//		ivDelTip = (ImageView) findViewById(R.id.iv_del_tip);
//		Drawable drawable = TintManager.getDrawable(mContext, SystemUtil.getResourceId(mContext, android.R.attr.editTextStyle, android.R.attr.background));
		Drawable drawable = TintManager.getDrawable(mContext, SystemUtil.getResourceId(mContext, R.attr.editTextBackground));
		if (SystemUtil.hasSDK16()) {
			layoutEdit.setBackground(drawable);
		} else {
			layoutEdit.setBackgroundDrawable(drawable);
		}
//		etcontent.setbackgroundresource(0);
		
		getSupportFragmentManager().beginTransaction()
			.replace(R.id.layout_emoji, EmojiTypeFragment.instantiate(mContext, EmojiTypeFragment.class.getCanonicalName()), "emojiFragment")
			.commit();
	}
	
	/**
	 * 初始化会话消息
	 * @update 2014年10月31日 下午3:26:57
	 */
	private void initMsgInfo() {
		new LoadDataTask(true).execute();
	}

	/**
	 * 进入菜单“更多”的模式
	 * @param msgId 选择的项的消息id
	 */
	private void initMoreMode(String msgId) {
		mIsBatchMode = true;
		
		if (mSelectMap == null) {
			mSelectMap = new HashMap<>();
		} else {
			mSelectMap.clear();
		}
		
		if (msgId != null) {
			mSelectSize ++;
			mSelectMap.put(msgId, true);
		}
		
		if (SystemUtil.isSoftInputActive()) {
			SystemUtil.hideSoftInput(this);
		}

		if (mActionMode != null) {
			if (mSelectSize > 0) {
				mActionMode.setTitle(String.valueOf(mSelectSize));
			} else {
				mActionMode.setTitle(null);
			}
		}
		
		msgAdapter.notifyDataSetChanged();

		//1、隐藏底部编辑部分和附件面板部分
		layoutBottom.setVisibility(View.GONE);
		
		int contentHeigth = layoutContent.getHeight();
		ViewPropertyAnimatorCompatSet anim = new ViewPropertyAnimatorCompatSet();
		ViewPropertyAnimatorCompat contentAnim = ViewCompat.animate(layoutContent).translationYBy(contentHeigth).setInterpolator(new AccelerateInterpolator(2));
		anim.setListener(new ViewPropertyAnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(View view) {
				layoutContent.setVisibility(View.GONE);
			}
		});
		anim.play(contentAnim);
		anim.start();
	}

	/**
	 * 退出菜单“更多”模式
	 * @param isAnim
	 */
	private void outMoreMode(boolean isAnim) {
		mIsBatchMode = false;
		mSelectSize = 0;
		if (mSelectMap != null) {
			mSelectMap.clear();
			mSelectMap = null;
		}
		finishActionMode(mActionMode);
		if (msgAdapter != null) {
			msgAdapter.notifyDataSetChanged();
		}
		if (layoutContent != null) {
			if (isAnim) {
				int contentHeigth = layoutContent.getHeight();
				ViewPropertyAnimatorCompatSet anim = new ViewPropertyAnimatorCompatSet();
				ViewPropertyAnimatorCompat contentAnim = ViewCompat.animate(layoutContent).translationYBy(-contentHeigth).setInterpolator(new AccelerateInterpolator(2));
				anim.setListener(new ViewPropertyAnimatorListenerAdapter() {
					@Override
					public void onAnimationEnd(View view) {
						layoutContent.setVisibility(View.VISIBLE);
					}
				});
				anim.play(contentAnim);
				anim.start();
			} else {
				layoutContent.setVisibility(View.VISIBLE);
			}
		}
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

	@Override
	protected void initData() {
//		resetVolumeFile();
		
		//重置录音的参数
		btnMakeVoice.setAudioRecorder(null);
		
		mChatState = ChatState.paused;
		
		Intent service = new Intent(mContext, CoreService.class);
		bindService(service, serviceConnection, Context.BIND_AUTO_CREATE);
		
//		mSensor = new SoundMeter();
		
		mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
		
		//注册处理聊天消息的广播
		msgProcessReceiver = new MsgProcessReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(MsgProcessReceiver.ACTION_PROCESS_MSG);
		intentFilter.addAction(MsgProcessReceiver.ACTION_REFRESH_MSG);
		intentFilter.addAction(MsgProcessReceiver.ACTION_UPDATE_CHAT_STATE);
		//使用本地广播
		mLocalBroadcastManager.registerReceiver(msgProcessReceiver, intentFilter);
//		registerReceiver(msgProcessReceiver, intentFilter);
		
		mImageLoader = ImageLoader.getInstance();
		
		if (screenSize == null) {
			screenSize = SystemUtil.getScreenSize();
		}
		
		Intent intent = getIntent();
		if (intent != null) {
			otherSide = intent.getParcelableExtra(UserInfoActivity.ARG_USER);
			msgThread = intent.getParcelableExtra(ARG_THREAD);
			if (msgThread != null) {
				mThreadId = msgThread.getId();
			} else {
				mThreadId = intent.getIntExtra(ARG_THREAD_ID, 0);
			}
		}

		//获取个人信息
		mine = ChatApplication.getInstance().getCurrentUser();
		
		initMsgInfo();
		
		msgAdapter = new MsgAdapter(mMsgInfos, mContext);
		lvMsgs.setAdapter(msgAdapter);
		
		//初始化表情分类数据
//		List<EmojiType> emojiTypes = ChatApplication.geEmojiTypes();
//		for (int i = 0; i < ChatApplication.emojiTypeCount; i++) {
//			EmojiType emojiType = emojiTypes.get(i);
//			TabSpec tabSpec = mTabHost.newTabSpec(emojiType.getFileName()).setIndicator(getTabIndicatorView(emojiType));
//			Bundle args = new Bundle();
//			args.putParcelable(EmojiFragment.ARG_EMOJI_TYPE, emojiType);
//			mTabHost.addTab(tabSpec, EmojiFragment.class, args);
//			mTabHost.setTag(i);
//			mTabHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.item_tab_selector);
//		}
		
		attachItemNames = getResources().getStringArray(R.array.att_item_name);
		//初始化添加附件选项的数据
		for (int i = 0; i < attachItemRes.length; i++) {
			AttachItem item = new AttachItem();
			item.setResId(attachItemRes[i]);
			item.setName(attachItemNames[i]);
			item.setAction(i + 1);
			
			mAttachItems.add(item);
		}
		
		attachPannelAdapter = new AttachPannelAdapter(mAttachItems, mContext);
		gvAttach.setAdapter(attachPannelAdapter);
		
		connection = XmppConnectionManager.getInstance().getConnection();
		
		//注册消息观察者
		registerContentOberver();
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		/*
    	 * 主要原因是该activity的android:launchMode="singleTask"
    	 * 所以第一次会或得到intent里的数据，但第二次或者以后就获取不到了，所以需要获取原来intent中的数据并且重新设置
    	 */
		super.onNewIntent(intent);
		setIntent(intent);

		resetState();
		
		outMoreMode(false);
		
		resetData();
		
		initData();
	}

	/**
	 * 重置一些控件、数据的状态
	 */
	private void resetState() {
		if (pDialog != null && pDialog.isShowing()) {
			pDialog.dismiss();
		}
	}
	
	/**
	 * 重置一些数据和状态
	 * @update 2015年9月14日 上午9:49:35
	 */
	private void resetData() {
		if (chat != null) {
			sendStateMsg(ChatState.gone);
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		//清除聊天消息的通知栏
		clearChatNotify();
	}
	
	/**
	 * 清除聊天消息的通知栏
	 * @update 2015年3月3日 下午2:03:47
	 */
	private void clearChatNotify() {
		if (coreService != null) {
			coreService.clearNotify(CoreService.NOTIFY_ID_CHAT_MSG);
		}
	}
	
	@Override
	public void onBackPressed() {
		if (isEmojiShow || editMode == MODE_ATTACH) {	//表情面板处于显示状态或者附件面板处于显示状态，则先隐藏，变为默认的编辑模式
			hideBottomLayout(true);
		} else {
			super.onBackPressed();
		}
	}
	
	@Override
	protected void onDestroy() {
		
		sendStateMsg(ChatState.gone);
		
		//TODO 添加接触服务绑定
		unbindService(serviceConnection);
		if (mLocalBroadcastManager != null) {
			mLocalBroadcastManager.unregisterReceiver(msgProcessReceiver);
		}
		
		//注销消息的观察者
		if (mMsgContentObserver != null) {
			msgManager.removeObserver(mMsgContentObserver);
		}
//		unregisterReceiver(msgProcessReceiver);
//		pageOffset = 0;
//		resetVolumeFile();
		
		if (chat != null) {
			chat.close();
		}
		
		super.onDestroy();
	}
	
//	private void resetVolumeFile() {
//		volumeFile = null;
//	}
	
	/**
	 * 创建聊天对象
	 * @update 2014年11月6日 下午9:38:50
	 * @return
	 */
	private Chat createChat(AbstractXMPPConnection connection) {
		if (connection == null) {
			connection = XmppConnectionManager.getInstance().getConnection();
		}
		if (connection.isAuthenticated()) {	//是否登录
			if (chatManager == null) {
				chatManager = ChatManager.getInstanceFor(connection);
			}
			if (chat == null) {
				chat = chatManager.createChat(otherSide.getJID(), null);
			}
			return chat;
		} else {
			//发送广播，重新登录
			Intent intent = new Intent(CoreReceiver.ACTION_RELOGIN);
			sendBroadcast(intent);
			return null;
		}
	}
	
	/**
	 * 注册消息观察者
	 * @update 2014年11月6日 下午7:32:34
	 */
	private void registerContentOberver() {
		mMsgContentObserver = new MsgContentObserver(mHandler);
		msgManager.addObserver(mMsgContentObserver);
//		getContentResolver().registerContentObserver(Provider.MsgInfoColumns.CONTENT_URI, true, msgContentObserver);
	}
	
	/**
	 * 加载聊天消息数据的后台任务
	 * @author huanghui1
	 * @update 2014年10月31日 上午9:18:23
	 */
	class LoadDataTask extends AsyncTask<Void, Void, List<MsgInfo>> {
		/**
		 * 是否需要滚动到最底部
		 */
		private boolean needScroll = true;

		public LoadDataTask(boolean needScroll) {
			this.needScroll = needScroll;
		}

		@Override
		protected List<MsgInfo> doInBackground(Void... params) {
			//根据参与者查询对应的会话
			MsgThread mt = null;
			List<MsgInfo> list = new ArrayList<>();
			if (otherSide != null) {
				mt = msgManager.getThreadByMember(otherSide);
				if (mt != null) {	//有该会话，才查询该会话下的消息
					msgThread = mt;
					mThreadId = mt.getId();
					list = msgManager.getMsgInfosByThreadId(mThreadId, getPageOffset());
					mMsgTotalCount = msgManager.getMsgCountByThreadId(mThreadId);
					
				} else {	//没有改会话，就创建一个
					mt = new MsgThread();
					mt.setMembers(Arrays.asList(otherSide));
					mt.setMsgThreadName(otherSide.getName());
					msgThread = msgManager.createMsgThread(mt);
					mMsgTotalCount = 0;
				}
			} else if (msgThread != null) {	//已经有会话了
				mThreadId = msgThread.getId();
				list = msgManager.getMsgInfosByThreadId(mThreadId, getPageOffset());
				mMsgTotalCount = msgManager.getMsgCountByThreadId(mThreadId);
				msgThread = msgManager.getThreadById(mThreadId);
				//TODO 目前固定写死，有、后期会改有群聊的模式
				otherSide = msgThread.getMembers().get(0);
			} else if (mThreadId > 0) {	//有会话id，可能是从通知栏进入的
				list = msgManager.getMsgInfosByThreadId(mThreadId, getPageOffset());
				mMsgTotalCount = msgManager.getMsgCountByThreadId(mThreadId);
				msgThread = msgManager.getThreadById(mThreadId);
				//TODO 目前固定写死，有、后期会改有群聊的模式
				otherSide = msgThread.getMembers().get(0);
			}
			return list;
		}
		
		@Override
		protected void onPostExecute(List<MsgInfo> result) {
			if (otherSide != null) {
				setTitle(otherSide.getName());
			}
			if (!SystemUtil.isEmpty(result)) {
				mMsgInfos.clear();
				mMsgInfos.addAll(result);
				msgAdapter.notifyDataSetChanged();
				if (needScroll) {
					scrollMyListViewToBottom(lvMsgs);
				}
			}
		}
	}
	
	/**
	 * 加载更多消息记录的后台任务,第一个参数是threadId,第二个参数是开始查询的索引位置:offset，从0开始
	 * @author huanghui1
	 * @update 2014年10月31日 下午3:16:09
	 */
	class LoadMoreDataTask extends AsyncTask<Integer, Void, List<MsgInfo>> {

		@Override
		protected List<MsgInfo> doInBackground(Integer... params) {
			List<MsgInfo> list = null;
			if (params != null && params.length == 2) {
				int msgThreadId = params[0];
				int offset = params[1];	//开始查询的索引
				if (msgThreadId > 0) {
					list = msgManager.getMsgInfosByThreadId(msgThreadId, offset);
				}
			}
			return list;
		}
		
		@Override
		protected void onPostExecute(List<MsgInfo> result) {
			mLoadFinish = true;
			if (headView != null) {
				lvMsgs.removeHeaderView(headView);
			}
			if (!SystemUtil.isEmpty(result)) {	//有数据
				mMsgInfos.addAll(0, result);
				msgAdapter.notifyDataSetChanged();
			}
		}
	}
	
	/**
	 * 获取分页时每次的分页开始索引
	 * @update 2014年10月31日 上午9:27:50
	 * @return
	 */
	private int getPageOffset() {
		int size = mMsgInfos.size();
		if (size > 0) {
			return size - 1;
		} else {
			return 0;
		}
	}
	
	/**
	 * 根据表情的分类获取对应的view
	 * @update 2014年10月27日 下午8:14:11
	 * @param emojiType
	 * @return
	 */
	private View getTabIndicatorView(EmojiType emojiType) {
		LayoutInflater inflater = LayoutInflater.from(mContext);
		View view = inflater.inflate(R.layout.layout_emoji_tab_view, null);
		
		ImageView ivIcon = (ImageView) view.findViewById(R.id.iv_icon);
		ivIcon.setImageResource(emojiType.getResId());
		return view;
	}
	
//	private Runnable mPollTask = new Runnable() {
//		
//		@Override
//		public void run() {
//			double amp = mSensor.getAmplitude();
//			updateDisplay(amp);
//			mHandler.postDelayed(mPollTask, POLL_INTERVAL);
//		}
//	};
	
//	private Runnable mSleepTask = new Runnable() {
//		public void run() {
//			stopRecord();
//		}
//	};
	
	/**
	 * 开始录音
	 * @update 2014年11月24日 下午9:50:49
	 */
//	private void startRecord() {
//		volumeFile = SystemUtil.generateChatAttachFile(mThreadId, SystemUtil.generateChatAttachFilename(System.currentTimeMillis()));
//		mSensor.start(volumeFile);
//		mHandler.postDelayed(mPollTask, POLL_INTERVAL);
//	}
	
	/**
	 * 停止录音
	 * @update 2014年11月24日 下午10:16:11
	 */
//	private void stopRecord() {
//		try {
//			mHandler.removeCallbacks(mSleepTask);
//			mHandler.removeCallbacks(mPollTask);
//			mSensor.stop();
//			ivVolume.setImageResource(R.drawable.amp1);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
	

	@Override
	protected void addListener() {
		btnEmoji.setOnClickListener(this);
		btnVoice.setOnClickListener(this);
		btnSend.setOnClickListener(this);
//		ivCancelTip.setOnClickListener(this);
//		etContent.setOnClickListener(this);
		gvAttach.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent intent = null;
				
				AttachItem attachItem = mAttachItems.get(position);
				MsgInfo msgInfo = new MsgInfo();
				if (canSend()) {
					msgInfo.setComming(false);
					msgInfo.setFromUser(mine.getFullJID());
					msgInfo.setToUser(otherSide.getFullJid());
					msgInfo.setRead(true);
					msgInfo.setSendState(SendState.SENDING);
					msgInfo.setThreadID(msgThread.getId());
				}
				
				int requestCode = 0;
				switch (attachItem.getAction()) {
				case AttachItem.ACTION_IMAGE:	//选择图片
					requestCode = AlbumActivity.REQ_PARENT_MAKE_IMG_MSG;
					intent = new Intent(mContext, AlbumActivity.class);
					msgInfo.setMsgType(Type.IMAGE);
					intent.putExtra(ARG_MSG_INFO, msgInfo);
					intent.putExtra(AlbumActivity.ARG_REQ_CODE, requestCode);
					break;
				case AttachItem.ACTION_VIDEO:	//选择视频
					requestCode = AlbumActivity.REQ_PARENT_MAKE_VIDEO_MSG;
					intent = new Intent(mContext, AlbumActivity.class);
					msgInfo.setMsgType(Type.VIDEO);
					intent.putExtra(ARG_MSG_INFO, msgInfo);
					intent.putExtra(AlbumActivity.ARG_IS_IMAGE, false);
					intent.putExtra(AlbumActivity.ARG_REQ_CODE, requestCode);
					break;
				case AttachItem.ACTION_FILE:	//选择文件
					intent = new Intent(mContext, FileExplorerActivity.class);
					msgInfo.setMsgType(Type.FILE);
					requestCode = REQ_FILE;
					break;
				case AttachItem.ACTION_AUDIO:	//选择音频
					intent = new Intent(mContext, AudioListActivity.class);
					msgInfo.setMsgType(Type.AUDIO);
					requestCode = REQ_AUDIO;
					break;
				case AttachItem.ACTION_LOCATION:	//地理位置
					intent = new Intent(mContext, LocationShareActivity.class);
					msgInfo.setMsgType(Type.LOCATION);
					requestCode = REQ_LOCATION;
					break;
				default:
					break;
				}
				if (intent != null) {
					intent.putExtra(ARG_MSG_INFO, msgInfo);
					startActivityForResult(intent, requestCode);
				}
			}
		});
		lvMsgs.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:    //点击外面
						//隐藏底部所有内容
						hideBottomLayout(true);
						//隐藏输入法
						hideKeybroad();
						break;

					default:
						break;
				}
				return false;
			}
		});
		lvMsgs.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (mIsBatchMode) {	//全选模式
					MsgViewHolder holder = (MsgViewHolder) view.getTag();
					if (holder != null) {
						holder.cbChose.toggle();
					}
				}
			}
		});
		lvMsgs.setOnScrollListener(new AbsListView.OnScrollListener() {
			
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				boolean canLoadMore = scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE;
				if (canLoadMore && mFirstVisiableItem == 0 && mLoadFinish && (mMsgInfos.size() < mMsgTotalCount)) {
					//加载跟多数据
					mLoadFinish = false;
					if (headView == null) {
						headView = LayoutInflater.from(mContext).inflate(R.layout.layout_head_loading, null);
					}
					lvMsgs.addHeaderView(headView);
					new LoadMoreDataTask().execute(mThreadId, getPageOffset());
				}
			}
			
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				mFirstVisiableItem = firstVisibleItem;
//				if (mCanLoadMore && firstVisibleItem == 0 && mLoadFinish && (getPageOffset() + 1) < mMsgTotalCount) {
//					//加载跟多数据
//					mLoadFinish = false;
//					if (headView == null) {
//						headView = LayoutInflater.from(mContext).inflate(R.layout.layout_head_loading, null);
//					}
//					lvMsgs.addHeaderView(headView);
//					new LoadMoreDataTask().execute(mThreadId, getPageOffset());
//				}
			}
		});
		etContent.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_UP:
					//文本框获取焦点，显示软键盘
					showKeybroad();
					//隐藏底部所有的面板
					hideBottomLayout(true);
					break;

				default:
					break;
				}
				return false;
			}
		});
		
		etContent.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {	//没有焦点就隐藏键盘
					if (mChatState != null && mChatState == ChatState.composing) {
						mChatState = ChatState.paused;
						sendStateMsg(mChatState);
					}
					hideKeybroad();
				} else {	//获取到焦点
					if (!TextUtils.isEmpty(etContent.getText())) {	//有内容
						if (mChatState != null && mChatState != ChatState.composing) {
							mChatState = ChatState.composing;
							sendStateMsg(mChatState);
						}
					}
				}
			}
		});
		etContent.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				setEditMode(s);	//设置对应的模式
				setChangeSendBtnStyle(editMode);
				if (gvAttach.getVisibility() == View.VISIBLE) {
					layoutBottom.setVisibility(View.GONE);
				}
				if (TextUtils.isEmpty(s)) {	//文本为空，发送对方暂停输入的消息
					if (mChatState != null && mChatState != ChatState.paused) {
						mChatState = ChatState.paused;
						sendStateMsg(mChatState);
					}
				} else {
					if (mChatState != null && mChatState != ChatState.composing) {
						mChatState = ChatState.composing;
						sendStateMsg(mChatState);
					}
				}
			}
		});
		
		/*
		 * 添加录音完成后的监听器
		 */
		btnMakeVoice.setRecordListener(new RecordButton.RecordListener() {
			
			@Override
			public void onRecordFinished(String filePath, int recordTime) {
				if (canSend() && SystemUtil.isFileExists(filePath)) {
					File file = new File(filePath);
					MsgInfo msgInfo = new MsgInfo();
					msgInfo.setComming(false);
					msgInfo.setFromUser(mine.getFullJID());
					msgInfo.setToUser(otherSide.getFullJid());
					msgInfo.setContent(SystemUtil.shortTimeToString(recordTime));
					msgInfo.setRead(true);
					msgInfo.setSendState(SendState.SENDING);
					msgInfo.setThreadID(msgThread.getId());
					msgInfo.setMsgType(Type.VOICE);
					msgInfo.setCreationDate(System.currentTimeMillis());
					//设置附件信息
					MsgPart msgPart = new MsgPart();
					msgPart.setCreationDate(System.currentTimeMillis());
					msgPart.setFileName(file.getName());
					msgPart.setFilePath(filePath);
					Log.d("-----------" + msgPart.getFilePath() + "-----exists-----" + SystemUtil.isFileExists(msgPart.getFilePath()));
//					String subfix = SystemUtil.getFileSubfix(volumeFile.getName());
//					msgPart.setMimeTye(MimeUtils.guessMimeTypeFromExtension(subfix));
					msgPart.setMimeType(MimeUtils.MIME_TYPE_AUDIO_AMR);
					msgPart.setSize(file.length());
					msgPart.setMsgId(msgInfo.getMsgId());
					msgInfo.setMsgPart(msgPart);
					
					mMsgInfos.add(msgInfo);
					
					addMsgTotalCount();

					sendMsg(msgInfo);

					msgAdapter.notifyDataSetChanged();
					//列表滚动到最底部
					scrollMyListViewToBottom(lvMsgs);
				}
			}
		});
	}
	
	/*@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (editMode == MODE_VOICE) {	//语音模式
			//获取语音按钮的坐标
			int[] recordPoint = new int[2];
			btnMakeVoice.getLocationInWindow(recordPoint);
			int recordX = recordPoint[0];
			int recordY = recordPoint[1];
			//获取删除按钮的位置
			int[] delPoint = new int[2];
			layoutDelRecord.getLocationInWindow(delPoint);
			int delX = delPoint[0];
			int delY = delPoint[1];
			if (event.getAction() == MotionEvent.ACTION_DOWN) {	//按下开始录音
//				//判断手指按下的坐标是否在按钮内部
				if (event.getRawY() > recordY && event.getRawX() > recordX) {
					recordRootLayout.setVisibility(View.VISIBLE);
					layoutVoiceRecordLoading.setVisibility(View.VISIBLE);
					layoutRecord.setVisibility(View.VISIBLE);
					layoutVoiceRecording.setVisibility(View.GONE);
					ivCancelTip.setVisibility(View.VISIBLE);
					
					layoutVoiceRecordTooshort.setVisibility(View.GONE);
					layoutDelRecord.setVisibility(View.GONE);
					
					btnMakeVoice.setPressed(true);
//					
//					//让加载按钮显示300毫秒
					mHandler.postDelayed(new Runnable() {
						public void run() {
							if (!isShort) {
								layoutVoiceRecordLoading.setVisibility(View.GONE);
								layoutVoiceRecording.setVisibility(View.VISIBLE);
							}
						}
					}, POLL_INTERVAL);
//					
					recordStartTime = System.currentTimeMillis();
//					//开始录音
					startRecord();
				}
			} else if (event.getAction() == MotionEvent.ACTION_UP) {	//松手
				btnMakeVoice.setPressed(false);
				float eX = event.getRawX();
				float eY = event.getRawY();
				layoutVoiceRecording.setVisibility(View.GONE);
				//判断松手时的坐标是否在删除区域内
				if (eY >= delY && eY <= delY + layoutDelRecord.getHeight() && eX >= delX && eX <= delX + layoutDelRecord.getWidth()) {	//在删除区域内
					cancelRecordVoice();
				} else {	//结束录音
					stopRecord();
					recordEndTime = System.currentTimeMillis();
					//计算时间差
					int time = (int) ((recordEndTime - recordStartTime) / 1000);
					if (time < Constants.COICE_RECORD_MIN_LENGTH) {	//少于1秒，则不发送，需重录
						deleteRecordFile();
						isShort = true;
						layoutVoiceRecordLoading.setVisibility(View.GONE);
						layoutDelRecord.setVisibility(View.GONE);
						layoutVoiceRecordTooshort.setVisibility(View.VISIBLE);
						//太短的提示信息显示300毫秒消失
						mHandler.postDelayed(new Runnable() {
							public void run() {
								layoutVoiceRecordTooshort.setVisibility(View.GONE);
								isShort = false;
							}
						}, POLL_INTERVAL);
					} else {
						//TODO 发送语音消息
						if (volumeFile != null && volumeFile.exists() && canSend()) {
							MsgInfo msgInfo = new MsgInfo();
							msgInfo.setComming(false);
							msgInfo.setFromUser(mine.getFullJID());
							msgInfo.setToUser(otherSide.getFullJid());
							msgInfo.setContent(SystemUtil.shortTimeToString(time));
							msgInfo.setRead(true);
							msgInfo.setSendState(MsgInfo.SendState.SENDING);
							msgInfo.setThreadID(msgThread.getId());
							msgInfo.setMsgType(MsgInfo.Type.VOICE);
							msgInfo.setCreationDate(System.currentTimeMillis());
							//设置附件信息
							MsgPart msgPart = new MsgPart();
							msgPart.setCreationDate(System.currentTimeMillis());
							msgPart.setFileName(volumeFile.getName());
							msgPart.setFilePath(volumeFile.getAbsolutePath());
							Log.d("-----------" + msgPart.getFilePath() + "-----exists-----" + SystemUtil.isFileExists(msgPart.getFilePath()));
//							String subfix = SystemUtil.getFileSubfix(volumeFile.getName());
//							msgPart.setMimeTye(MimeUtils.guessMimeTypeFromExtension(subfix));
							msgPart.setMimeType(MimeUtils.MIME_TYPE_AUDIO_AMR);
							msgPart.setSize(volumeFile.length());
							
							msgInfo.setMsgPart(msgPart);
							
							mMsgInfos.add(msgInfo);
							
							addMsgTotalCount();

							sendMsg(msgInfo);

							resetVolumeFile();
							
							msgAdapter.notifyDataSetChanged();
							//列表滚动到最底部
							scrollMyListViewToBottom(lvMsgs);
						}
					}
					
				}
			} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
				if (event.getRawY() < recordY) {	//手按下的位置不在语音按钮的区域内
					float eX = event.getRawX();
					float eY = event.getRawY();
					Animation inAnim = AnimationUtils.loadAnimation(mContext, R.anim.chat_record_voice_in);
					Animation outAnim = AnimationUtils.loadAnimation(mContext, R.anim.chat_record_voice_out);
					layoutDelRecord.setVisibility(View.VISIBLE);
					ivCancelTip.setVisibility(View.GONE);
					layoutDelRecord.setPressed(false);
					if (eY >= delY && eY <= delY + layoutDelRecord.getHeight() && eX >= delX && eX <= delX + layoutDelRecord.getWidth()) {	//在删除区域内
						layoutDelRecord.setPressed(true);
						ivDelTip.startAnimation(inAnim);
						ivDelTip.startAnimation(outAnim);
					}
				} else {
					ivCancelTip.setVisibility(View.VISIBLE);
					layoutDelRecord.setVisibility(View.GONE);
				}
			}
			return true;
		}
		return super.onTouchEvent(event);
	}*/
	
	/**
	 * 取消录音
	 * @update 2015年2月9日 下午8:36:54
	 */
//	private void cancelRecordVoice() {
//		recordRootLayout.setVisibility(View.GONE);
//		layoutVoiceRecordTooshort.setVisibility(View.GONE);
//		layoutDelRecord.setVisibility(View.GONE);
//		
//		deleteRecordFile();
//	}
	
	/**
	 * 删除录音文件
	 * @update 2014年11月25日 下午3:17:26
	 */
//	private void deleteRecordFile() {
//		if (volumeFile != null) {
//			if (volumeFile.exists()) {
//				volumeFile.delete();
//			}
//		}
//	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case AlbumActivity.REQ_PARENT_MAKE_IMG_MSG:	//相册
			case AlbumActivity.REQ_PARENT_MAKE_VIDEO_MSG:	//视频
			case REQ_FILE:	//文件
				if (data != null) {
					final List<MsgInfo> msgList = data.getParcelableArrayListExtra(ARG_MSG_INFO_LIST);
					final boolean originalImage = data.getBooleanExtra(PhotoPreviewActivity.ARG_ORIGINAO_IMAGE, false);
					if (!SystemUtil.isEmpty(msgList)) {
						hideAttachLayout();
						setEditMode();
						mMsgInfos.addAll(msgList);
						addMsgTotalCount(msgList.size());
						msgAdapter.notifyDataSetChanged();
						scrollMyListViewToBottom(lvMsgs);
						SystemUtil.getCachedThreadPool().execute(new Runnable() {
							
							@Override
							public void run() {
								if (chat == null) {
									chat = createChat(connection);
								}
								for (MsgInfo msgInfo : msgList) {
									MsgSenderInfo senderInfo = new MsgSenderInfo(chat, msgInfo, msgThread, mHandler);
									senderInfo.originalImage = originalImage;
									coreService.sendChatMsg(senderInfo);
								}
							}
						});
					}
				}
				break;
			case REQ_AUDIO:	//音频
			case REQ_LOCATION:	//地理位置分享
				if (data != null) {
					final MsgInfo mi = data.getParcelableExtra(ARG_MSG_INFO);
					if (mi != null) {
						hideAttachLayout();
						setEditMode();
						mMsgInfos.add(mi);
						addMsgTotalCount();
						msgAdapter.notifyDataSetChanged();
						scrollMyListViewToBottom(lvMsgs);
						SystemUtil.getCachedThreadPool().execute(new Runnable() {
							
							@Override
							public void run() {
								if (chat == null) {
									chat = createChat(connection);
								}
								MsgSenderInfo senderInfo = new MsgSenderInfo(chat, mi, msgThread, mHandler);
								coreService.sendChatMsg(senderInfo);
							}
						});
					}
				}
				break;
			default:
				break;
			}
		} else if (resultCode == RESULT_CANCELED) {
			
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	/**
	 * 将表情按钮的背景设置为正常，已废弃，使用btnEmoji.setSelected(false)代替
	 * @update 2014年10月28日 上午9:09:47
	 */
	@Deprecated
	private void changeEmojiBtnBackground2Normal() {
		btnEmoji.setBackgroundResource(R.drawable.chat_facial_selector);
	}
	
	/**
	 * 将表情按钮的背景设置为按下的背景，已废弃，使用btnEmoji.setSelected(true)代替
	 * @update 2014年10月28日 上午9:09:47
	 */
	@Deprecated
	private void changeEmojiBtnBackground2Pressed() {
		btnEmoji.setBackgroundResource(R.drawable.ic_facial_pressed);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_emoji:	//表情按钮
			handleEmojiMode();	//切换到表情模式
			break;
		case R.id.btn_send:	//发送或者附件选择
			handleSendMode();
			break;
		case R.id.btn_voice:	//语音输入按钮
			handleVoiceMode();
			break;
//		case R.id.iv_cancel_tip:	//取消录音
//			layoutVoiceRecording.setVisibility(View.GONE);
//			stopRecord();
//			break;
		
		default:
			break;
		}
	}
	
	/**
	 * 根据文本输入框里的内容来设置对应的模式
	 * @update 2014年10月28日 上午10:11:39
	 */
	private void setEditMode(CharSequence s) {
		//判断文本框里有无内容
		if (!TextUtils.isEmpty(s)) {	//有内容，则模式为发送模式，否则为添加模式
			editMode = MODE_SEND;
		} else {
			editMode = MODE_DEFAULT;
		}
	}
	
	/**
	 * 根据不同的模式设置发送按钮的样式
	 * @update 2014年10月28日 下午2:28:33
	 * @param mode
	 */
	private void setChangeSendBtnStyle(int mode) {
		if (editMode == MODE_SEND) {
			btnSend.setText(R.string.send);
			btnSend.setBackgroundResource(R.drawable.common_button_green_selector);
			int padding = getResources().getDimensionPixelSize(R.dimen.chat_send_btn_padding);
			btnSend.setPadding(padding, padding, padding, padding);
		} else {
			btnSend.setText("");
			btnSend.setBackgroundResource(R.drawable.chat_attach_selector);
			int padding = 0;
			btnSend.setPadding(padding, padding, padding, padding);
		}
	}
	
	/**
	 * 根据文本输入框里的内容来设置对应的模式
	 * @update 2014年10月28日 上午10:11:39
	 */
	private void setEditMode() {
		setEditMode(etContent.getText().toString());
	}
	
	/**
	 * 隐藏键盘
	 * @update 2014年10月28日 上午10:04:56
	 */
	private void hideKeybroad() {
		//3、若键盘为显示模式，则隐藏键盘
		boolean isSoftKeyboard = SystemUtil.isSoftInputActive();
		if (isSoftKeyboard) {	//键盘已显示，则隐藏
			//显示键盘
			SystemUtil.hideSoftInput(etContent);
		}
	}
	
	/**
	 * 显示表情面板
	 * @update 2014年10月28日 上午9:46:45
	 */
	private void showEmojiLayout() {
		//显示表情面板的父面板
		layoutBottom.setVisibility(View.VISIBLE);
		//隐藏附件面板
		gvAttach.setVisibility(View.GONE);
		//改变表情按钮背景为选中背景
		btnEmoji.setSelected(true);
//		changeEmojiBtnBackground2Pressed();
		//显示表情面板
		layoutEmoji.setVisibility(View.VISIBLE);
		setEditMode();
		isEmojiShow = true;
	}
	
	/**
	 * 隐藏表情面板
	 * @update 2014年10月28日 上午9:48:20
	 */
	private void hideEmojiLayout() {
		//显示表情面板的父面板
//		layoutBottom.setVisibility(View.GONE);
		//改变表情按钮背景为正常状态
		btnEmoji.setSelected(false);
//		changeEmojiBtnBackground2Normal();
		//隐藏表情面板
		layoutEmoji.setVisibility(View.GONE);
		isEmojiShow = false;
	}
	
	/**
	 * 隐藏底部的所有面板，切换到“文本输入模式”或“者语音输入模式”,语音输入也会隐藏底部面板，但不会进入到文本输入模式
	 * @param isTextMode 是否进入到输入模式，
	 * @update 2014年10月28日 上午10:17:51
	 */
	private void hideBottomLayout(boolean isTextMode) {
		layoutBottom.setVisibility(View.GONE);
		hideEmojiLayout();
		
		if (isTextMode) {
			//根据文本内容切换到输入模式
			setEditMode();
		}
	}
	
	/**
	 * 消息输入框获得焦点并显示键盘
	 * @update 2014年10月28日 上午9:17:27
	 */
	private void showKeybroad() {
		//文本框获取焦点
		etContent.requestFocus();
		//显示键盘
		SystemUtil.showSoftInput(etContent);
	}
	
	/**
	 * 隐藏输入法，但文本框仍保留焦点
	 * @update 2014年10月28日 上午9:17:27
	 */
	private void editHideKeybroadWithFocus() {
		//显示键盘
		SystemUtil.hideSoftInput(etContent);
		//文本框获取焦点
		etContent.requestFocus();
	}
	
	/**
	 * 隐藏附件选择面板
	 * @update 2014年10月28日 上午9:52:40
	 */
	private void hideAttachLayout() {
		layoutBottom.setVisibility(View.GONE);
//		layoutEmoji.setVisibility(View.GONE);
//		gvAttach.setVisibility(View.GONE);
	}
	
	/**
	 * 隐藏输入法，同时文本框也失去焦点
	 * @update 2014年10月28日 上午9:17:27
	 */
	private void editHideKeybroadNoFocus() {
		//文本框获取焦点
		hideKeybroad();
		etContent.clearFocus();
	}
	
	/**
	 * 隐藏语音输入按钮
	 * @update 2014年10月28日 上午11:59:05
	 */
	private void hideVoiceLayout() {
		btnMakeVoice.setVisibility(View.GONE);
		btnVoice.setBackgroundResource(R.drawable.chat_voice_mode_selector);
	}
	
	/**
	 * 显示语音输入按钮
	 * @update 2014年10月28日 上午11:59:05
	 */
	private void showVoiceLayout() {
		btnMakeVoice.setVisibility(View.VISIBLE);
		btnVoice.setBackgroundResource(R.drawable.chat_keyboard_mode_selector);
	}
	
	/**
	 * 显示文本编辑面板
	 * @update 2014年10月28日 下午12:01:05
	 */
	private void showEditLayout() {
		layoutEdit.setVisibility(View.VISIBLE);
		//隐藏底部所有面板
		hideBottomLayout(true);
		//显示软键盘
		showKeybroad();
	}
	
	/**
	 * 显示附件面板
	 * @update 2014年10月28日 下午5:26:00
	 */
	private void showAttLayout() {
		layoutBottom.setVisibility(View.VISIBLE);
		layoutEmoji.setVisibility(View.GONE);
		gvAttach.setVisibility(View.VISIBLE);
		//隐藏输入法和文本框失去焦点
		editHideKeybroadNoFocus();
	}
	
	/**
	 * 隐藏输入框面板，主要用于语音输入模式，其他模式不可能隐藏该面板
	 * @update 2014年10月28日 下午12:03:08
	 */
	private void hideEditLayout() {
		//隐藏输入法
		hideKeybroad();
		layoutEdit.setVisibility(View.GONE);
		//隐藏底部所有面板
//		hideBottomLayout(false);
		layoutBottom.setVisibility(View.GONE);
	}
	
	/**
	 * 将发送按钮改变成附件按钮
	 * @update 2014年10月28日 下午8:25:01
	 */
	private void changeSendBtn2Att() {
		if (editMode == MODE_SEND) {	//之前为发送模式
			btnSend.setText("");
			btnSend.setBackgroundResource(R.drawable.chat_attach_selector);
		}
	}
	
	/**
	 * listview滚动到最底部
	 * @update 2014年10月29日 下午5:57:29
	 * @param listView
	 */
	private void scrollMyListViewToBottom(final ListView listView) {
		if (listView != null) {
			listView.post(new Runnable() {
				@Override
				public void run() {
					// Select the last row so it will scroll into view...
					listView.setSelection(listView.getCount() - 1);
//	        	listView.smoothScrollToPosition(0);
				}
			});
		}
	}

	/**
	 * 切换到表情选择模式
	 * @update 2014年10月25日 下午4:43:17
	 */
	private void handleEmojiMode() {
		if (isEmojiShow) {	//判断点击之前的模式是否为“表情模式”，若是，则切换到文本输入模式
			//显示键盘
			showKeybroad();
			//直接隐藏底部面板
			hideBottomLayout(true);
			//隐藏表情面板
//			hideEmojiLayout();
			//判断文本框里有无内容
			//有内容，则模式为发送模式，否则为添加模式
//			setEditMode();
		} else {
			//点击表情按钮之前的模式不可能为“语音模式”，“语音模式时“，表情按钮式隐藏的
			switch (editMode) {
			case MODE_ATTACH:	//点击之前是附件选择模式，则隐藏附件面板，其他的模式已经处理
				//隐藏附件面板
				hideAttachLayout();
			case MODE_DEFAULT:
			case MODE_SEND:
				//2、消息输入文本框获得焦点
				editHideKeybroadWithFocus();
				//显示表情面板
				showEmojiLayout();
				break;
			default:
				break;
			}
		}
	}
	
	/**
	 * 发送文本消息
	 * @update 2014年11月6日 下午9:46:44
	 * @param msgInfo 消息实体信息
	 * @return
	 */
	private MsgInfo sendMsg(MsgInfo msgInfo) {
		return sendMsg(msgInfo, false);
	}
	
	/**
	 * 发送文本消息
	 * @author tiger
	 * @update 2015年3月28日 上午9:27:25
	 * @param msgInfo
	 * @param isReSend 是否是重发该消息
	 * @return
	 */
	private MsgInfo sendMsg(MsgInfo msgInfo, boolean isReSend) {
		if (chat == null) {
			chat = createChat(connection);
		}
		if(!XmppStringUtils.isBareJid(msgInfo.getFromJid())){
			msgInfo.setFromUser(mine.getFullJID());
		}
		if(!XmppStringUtils.isBareJid(msgInfo.getToJid())){
			msgInfo.setToUser(otherSide.getFullJid());
		}
		MsgSenderInfo msgSenderInfo = new MsgSenderInfo(chat, msgInfo, msgThread, mHandler, isReSend);
		coreService.sendChatMsg(msgSenderInfo);
		return msgInfo;
	}
	
	/**
	 * 发送输入状态的消息
	 * @param state 消息的状态
	 * @update 2015年9月12日 下午4:48:08
	 */
	private void sendStateMsg(ChatState state) {
		if (chat == null) {
			chat = createChat(connection);
		}
		MsgSenderInfo msgSenderInfo = new MsgSenderInfo(chat, null, null, null, false);
		coreService.sendChatStateMsg(state, msgSenderInfo);
	}
	
	/**
	 * 新创建一个文本消息
	 * @update 2014年11月25日 下午4:55:28
	 * @param content
	 */
	private MsgInfo newTextMsgInfo(String content) {
		MsgInfo msg = new MsgInfo();
		msg.setComming(false);
		msg.setCreationDate(System.currentTimeMillis());
		msg.setContent(content);
		msg.setSendState(SendState.SENDING);
		msg.setFromUser(mine.getFullJID());
		msg.setToUser(otherSide.getJID());
		msg.setMsgType(Type.TEXT);
		msg.setRead(true);
		msg.setMsgPart(null);
		msg.setSubject(null);
		msg.setThreadID(msgThread.getId());
		
		return msg;
	}
	
	/**
	 * 检查是否可以发送信息，前提条件是有个人信息和对方信息
	 * @update 2015年1月8日 下午8:59:25
	 * @return
	 */
	private boolean canSend() {
		if (otherSide == null || mine == null || msgThread == null) {
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * 
	 * 处理发送文本消息或者切换到附件模式
	 * @update 2014年10月28日 上午11:55:35
	 */
	private void handleSendMode() {
		if (isEmojiShow) {	//若显示有表情面板，则隐藏
			hideBottomLayout(false);
//			hideEmojiLayout();
		}
		switch (editMode) {
		case MODE_SEND:	//发送文本消息
			String content = etContent.getText().toString();
			if (!canSend()) {
				break;
			}
			MsgInfo msg = newTextMsgInfo(content);
			
			if (msg != null) {
				sendMsg(msg);
				
				mMsgInfos.add(msg);
				addMsgTotalCount();
			}

			msgAdapter.notifyDataSetChanged();

			scrollMyListViewToBottom(lvMsgs);

			etContent.setText("");
			//隐藏底部面板
			hideBottomLayout(false);
			editMode = MODE_DEFAULT;
			setChangeSendBtnStyle(editMode);
			break;
		case MODE_ATTACH:	//点击之前是附件模式
			//隐藏附件面板
			hideAttachLayout();
			//文本框获取焦点，弹出软键盘
			showKeybroad();
			editMode = MODE_DEFAULT;
			break;
		case MODE_VOICE:	//点击之前是语音模式，则隐藏语音按钮,显示附件选择面板
			//隐藏语音输入按钮
			hideVoiceLayout();
			//显示文本输入框
			layoutEdit.setVisibility(View.VISIBLE);
		case MODE_DEFAULT:
			
			//显示附件面板
			showAttLayout();
			editMode = MODE_ATTACH;
			break;
		default:
			break;
		}
	}
	
	/**
	 * 处理切换到“语音模式”
	 * @update 2014年10月28日 下午5:57:07
	 */
	private void handleVoiceMode() {
		if (isEmojiShow) {
//			hideEmojiLayout();	//隐藏表情面板
			hideBottomLayout(false);
		}
		switch (editMode) {
		case MODE_VOICE:	//点击之前是语音模式，则隐藏语音按钮
			hideVoiceLayout();
			//显示文本输入框
			showEditLayout();
			//根部不同的模式改变附件或发送按钮的样式
			setChangeSendBtnStyle(editMode);
			break;
		case MODE_DEFAULT:
		case MODE_SEND:	//隐藏输入法
		case MODE_ATTACH:	//隐藏附件面板
			hideAttachLayout();
			//隐藏文本输入框
			hideEditLayout();
			//显示语音按钮
			showVoiceLayout();
			//将发送按钮改变为附件按钮
			changeSendBtn2Att();
			editMode = MODE_VOICE;
			if (btnMakeVoice.getAudioRecorder() == null) {
				btnMakeVoice.setAudioRecorder(new AudioRecorder(mThreadId));
			}
			break;
		default:
			break;
		}
	}
	
	@Override
	protected void onPause() {
		//取消录音
//		recordRootLayout.setPressed(false);
//		cancelRecordVoice();
		//隐藏键盘
		hideKeybroad();
		super.onPause();
	}
	
//	/**
//	 * 发送消息的线程
//	 * @author huanghui1
//	 * @update 2014年11月6日 下午9:56:22
//	 */
//	class SendMsgTask implements Runnable {
//		private MsgInfo msgInfo;
//
//		public SendMsgTask(MsgInfo msgInfo) {
//			this.msgInfo = msgInfo;
//		}
//
//		@Override
//		public void run() {
//			try {
//				msgInfo = msgManager.addMsgInfo(msgInfo);
//				msgThread.setSnippetId(msgInfo.getId());
//				msgThread.setSnippetContent(msgInfo.getContent());
//				msgThread.setModifyDate(System.currentTimeMillis());
//				msgThread = msgManager.updateMsgThread(msgThread);
//				if (msgInfo != null) {
//					if (chat == null) {
//						chat = createChat(connection);
//					}
//					if (chat != null) {
//						chat.sendMessage(msgInfo.getContent());
//						msgInfo.setSendState(SendState.SUCCESS);
//					} else {
//						msgInfo.setSendState(SendState.FAILED);
//					}
//				} else {
//					return;
//				}
//			} catch (NotConnectedException | XMPPException e) {
//				msgInfo.setSendState(SendState.FAILED);
//				e.printStackTrace();
//			}
//			msgInfo = msgManager.updateMsgInfo(msgInfo);
//			mHandler.sendEmptyMessage(Constants.MSG_MODIFY_CHAT_MSG_SEND_STATE);
//		}
//		
//	}
	
	/**
	 * 添加附件的适配器
	 * @author huanghui1
	 * @update 2014年10月28日 下午4:29:25
	 */
	class AttachPannelAdapter extends CommonAdapter<AttachItem> {

		public AttachPannelAdapter(List<AttachItem> list, Context context) {
			super(list, context);
			// TODO Auto-generated constructor stub
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			AttItemViewHolder holder = null;
			if (convertView == null) {
				holder = new AttItemViewHolder();
				
				convertView = inflater.inflate(R.layout.item_attach, parent, false);
				
				holder.ivIcon = (ImageView) convertView.findViewById(R.id.iv_icon);
				holder.tvName = (TextView) convertView.findViewById(R.id.tv_name);
				
				convertView.setTag(holder);
			} else {
				holder = (AttItemViewHolder) convertView.getTag();
			}
			
			final AttachItem item = list.get(position);
			holder.ivIcon.setImageResource(item.getResId());
			holder.tvName.setText(item.getName());
			return convertView;
		}
		
	}
	
	final static class AttItemViewHolder {
		ImageView ivIcon;
		TextView tvName;
	}
	
	/**
	 * 聊天消息的适配器
	 * @author huanghui1
	 * @update 2014年10月29日 下午4:36:14
	 */
	class MsgAdapter extends CommonAdapter<MsgInfo> {
		DisplayImageOptions headIconOptions = SystemUtil.getGeneralImageOptions();
		DisplayImageOptions chatImageOptions = SystemUtil.getChatImageOptions();
		
		/**
		 * 发送的消息类型：0
		 */
		public static final int TYPE_OUT = 0;
		/**
		 * 接受的消息类型：1
		 */
		public static final int TYPE_IN = 1;
		/**
		 * item有两种类型
		 */
		private static final int TYPE_COUNT = 2;
		
		/**
		 * 消息内容view最大的宽度
		 */
		private int maxConentWidth = 0; 
		//十分钟，单位毫秒
		private long spliteTimeUnit = 600000;

		public MsgAdapter(List<MsgInfo> list, Context context) {
			super(list, context);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			MsgViewHolder holder = null;
			final MsgInfo msgInfo = list.get(position);
			int type = getItemViewType(position);
			String msgId = msgInfo.getMsgId();
			if (convertView == null) {
				holder = new MsgViewHolder();
				switch (type) {
				case TYPE_OUT:	//发出的消息
					convertView = inflater.inflate(R.layout.item_chat_msg_out, parent, false);
					
					break;
				case TYPE_IN:	//接收的消息
					convertView = inflater.inflate(R.layout.item_chat_msg_in, parent, false);
					
					break;
				}
				
				holder.tvMsgTime = (TextView) convertView.findViewById(R.id.tv_msg_time);
				holder.layoutBody = (RelativeLayout) convertView.findViewById(R.id.layout_body);
				holder.ivHeadIcon = (ImageView) convertView.findViewById(R.id.iv_head_icon);
				holder.ivMsgState = (ImageView) convertView.findViewById(R.id.iv_msg_state);
				holder.tvContent = (TextView) convertView.findViewById(R.id.tv_content);
				holder.ivContentImg = (ImageView) convertView.findViewById(R.id.iv_content_img);
				holder.tvContentDesc = (TextView) convertView.findViewById(R.id.tv_content_desc);
				holder.contentImgLayout = (FrameLayout) convertView.findViewById(R.id.content_img_layout);
				holder.contentLayout = convertView.findViewById(R.id.content_layout);
				holder.cbChose = (CheckBox) convertView.findViewById(R.id.cb_chose);
				
				convertView.setTag(holder);
			} else {
				holder = (MsgViewHolder) convertView.getTag();
			}
			if (maxConentWidth == 0) {
				//获取头像的宽度
				int iconWith = SystemUtil.getViewSize(holder.ivHeadIcon)[0];
				int stateWidth = SystemUtil.getViewSize(holder.ivMsgState)[0];
				int textMargin = DensityUtil.dip2px(context, getResources().getDimension(R.dimen.chat_msg_item_content_margin_left_right));
				int stateMargin = DensityUtil.dip2px(context, getResources().getDimension(R.dimen.chat_msg_item_send_state_margin_left_right));
				maxConentWidth = screenSize[0] - 2 * (iconWith + textMargin) - stateWidth - stateMargin;
			}

			holder.tvContent.setVisibility(View.VISIBLE);
			holder.ivContentImg.setVisibility(View.GONE);
			holder.tvContentDesc.setVisibility(View.GONE);
			
			holder.tvContent.setMaxWidth(maxConentWidth);
			holder.ivContentImg.setMaxWidth(getResources().getDimensionPixelSize(R.dimen.chat_msg_img_max_width));
			holder.ivContentImg.setMaxHeight(getResources().getDimensionPixelSize(R.dimen.chat_msg_img_max_height));
			
			holder.contentImgLayout.setForeground(null);
			
			if (mIsBatchMode) {	//选择了菜单“更多”，进入了多选模式
				holder.cbChose.setVisibility(View.VISIBLE);
				holder.cbChose.setOnCheckedChangeListener(null);
				if (mSelectMap != null) {
					holder.cbChose.setChecked(mSelectMap.containsKey(msgId));
				} else {
					holder.cbChose.setChecked(false);
				}
				holder.cbChose.setOnCheckedChangeListener(new OnCheckedChangeListenerImpl(holder, msgId));
			} else {
				if (holder.cbChose.getVisibility() == View.VISIBLE) {
					holder.cbChose.setVisibility(View.GONE);
				}
			}
			
//			int paddingLeft = 0;
//			int paddingRight = 0;
//			int extraPad = 1;
//			Resources resources = getResources();
//			int paddingVertical = resources.getDimensionPixelSize(R.dimen.chat_msg_item_content_padding_top_bottom);
//			if (type == TYPE_IN) {	//接收的消息
//				paddingLeft = resources.getDimensionPixelSize(R.dimen.chat_msg_item_in_content_padding_left);
//				paddingRight = resources.getDimensionPixelSize(R.dimen.chat_msg_item_in_content_padding_right);
//			} else {
//				paddingLeft = resources.getDimensionPixelSize(R.dimen.chat_msg_item_out_content_padding_left);
//				paddingRight = resources.getDimensionPixelSize(R.dimen.chat_msg_item_out_content_padding_right);
//			}
			
//			holder.tvContent.setPadding(paddingLeft, paddingVertical, paddingRight, paddingVertical);
			long curDate = msgInfo.getCreationDate();
			if (position == 0) {	//第一条记录，一定显示时间
				holder.tvMsgTime.setVisibility(View.VISIBLE);
				holder.tvMsgTime.setText(dateFormat.format(new Date(curDate)));
			} else if (position >= 1) {	//至少有两条数据时才显示
				//当条记录的时间
				//上一条记录的时间
				long preDate = list.get(position - 1).getCreationDate();
				if (Math.abs(curDate - preDate) > spliteTimeUnit) {	//时间间隔超过10分钟，则显示时间分割条
					holder.tvMsgTime.setVisibility(View.VISIBLE);
					holder.tvMsgTime.setText(dateFormat.format(new Date(curDate)));
				} else {
					holder.tvMsgTime.setVisibility(View.GONE);
				}
			}
			holder.tvContent.setText("");
			holder.tvContent.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
			Type msgType = msgInfo.getMsgType();
			MsgPart msgPart = msgInfo.getMsgPart();
			holder.tvContent.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
			holder.tvContent.setCompoundDrawablePadding(0);
			switch (msgType) {
			case TEXT:	//文本消息
				SpannableString spannableString = SystemUtil.getExpressionString(mContext, msgInfo.getContent());
				if (spannableString.length() <= context.getResources().getInteger(R.integer.msg_content_min_center_length)) {	//字符太短，居中显示
					holder.tvContent.setGravity(Gravity.CENTER);
				}
				holder.tvContent.setText(spannableString);
				break;
			case IMAGE:	//图片消息
//				int extraSpace = Math.abs(paddingLeft - paddingRight) + 3 * extraPad; 
//				if (type == TYPE_IN) {	//接收的消息
//					holder.tvContent.setPadding(extraSpace, extraPad, extraPad, extraPad);
//				} else {
//					holder.tvContent.setPadding(extraPad, extraPad, extraSpace, extraPad);
//				}
				holder.tvContent.setVisibility(View.GONE);
				holder.ivContentImg.setVisibility(View.VISIBLE);
				if (type == TYPE_OUT) {	//自己发出去的消息
					holder.contentImgLayout.setForeground(getResources().getDrawable(R.drawable.chat_msg_out_img_selector));
				} else {
					holder.contentImgLayout.setForeground(getResources().getDrawable(R.drawable.chat_msg_in_img_selector));
				}
				//显示图片
				displayImage(msgInfo, holder.ivContentImg);
				
				break;
			case VOICE:	//语音文件
				//TODO 等待解决
				Drawable drawable = null;
				holder.tvContent.setCompoundDrawablePadding(10);
				holder.tvContent.setText(msgInfo.getContent());
				if (type == TYPE_OUT) {	//自己发出去的消息
					drawable = getResources().getDrawable(R.drawable.chat_voice_out);
					/// 这一步必须要做,否则不会显示.
					drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
					holder.tvContent.setCompoundDrawables(null, null, drawable, null);
				} else {
					drawable = getResources().getDrawable(R.drawable.chat_voice_in);
					/// 这一步必须要做,否则不会显示.
					drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
					holder.tvContent.setCompoundDrawables(drawable, null, null, null);
				}
				break;
			case AUDIO:
			case VIDEO:
			case FILE:	//普通文件类型
//				holder.tvContent.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
				holder.tvContent.setCompoundDrawablePadding((int) getResources().getDimension(R.dimen.chat_msg_item_drawable_spacing));
				if (msgPart != null) {
					String partPath = msgPart.getFilePath();
					String fileName = msgPart.getFileName();
					String sizeStr = SystemUtil.sizeToString(msgPart.getSize());
					
					FileItem fileItem = SystemUtil.getFileItem(partPath, fileName, msgPart.getMimeType());
					
					int fileNameLength = fileName.length();
					
					String str = getString(R.string.chat_attach_file_desc, fileName, sizeStr);
					SpannableStringBuilder spannableDesc = new SpannableStringBuilder(str);
					spannableDesc.setSpan(new TextAppearanceSpan(context, R.style.ChatItemContentTitleStyle), 0, fileNameLength, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
					spannableDesc.setSpan(new TextAppearanceSpan(context, R.style.ChatItemContentSubTitleStyle), fileNameLength, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
					holder.tvContent.setText(spannableDesc, TextView.BufferType.SPANNABLE);
					
					Integer resId = SystemUtil.getResIdByFile(fileItem, R.drawable.ic_attach_file);
					holder.tvContent.setCompoundDrawablesWithIntrinsicBounds(resId, 0, 0, 0);
					
					if (fileItem != null) {
						switch (fileItem.getFileType()) {
							case IMAGE:	//图片,则直接加载图片缩略图
								holder.tvContent.setCompoundDrawablePadding(0);
								holder.tvContent.setText("");
								if (SystemUtil.isFileExists(partPath)) {
									mImageLoader.displayImage(Scheme.FILE.wrap(partPath), new TextViewAware(holder.tvContent), chatImageOptions);
								}
								break;
							case APK:	//安装文件
								new LoadApkIconTask(holder).execute(partPath);
								break;
							default:
								break;
						}
					}
				}
				break;
			case LOCATION:	//地理位置
//				holder.tvContent.setText("");
//				int locationXxtraSpace = Math.abs(paddingLeft - paddingRight) + 3 * extraPad; 
//				if (type == TYPE_IN) {	//接收的消息
//					holder.tvContent.setPadding(locationXxtraSpace, extraPad, extraPad, extraPad);
//				} else {
//					holder.tvContent.setPadding(extraPad, extraPad, locationXxtraSpace, extraPad);
//				}
				holder.tvContent.setVisibility(View.GONE);
				holder.ivContentImg.setVisibility(View.VISIBLE);
				holder.tvContentDesc.setVisibility(View.VISIBLE);
				
				holder.ivContentImg.setMaxWidth(Constants.IMAGE_LOCATION_THUMB_WIDTH);
				holder.ivContentImg.setMaxHeight(Constants.IMAGE_LOCATION_THUMB_HEIGHT);
				
				if (type == TYPE_OUT) {	//自己发出去的消息
					holder.contentImgLayout.setForeground(getResources().getDrawable(R.drawable.chat_msg_out_img_selector));
				} else {
					holder.contentImgLayout.setForeground(getResources().getDrawable(R.drawable.chat_msg_in_img_selector));
				}
				
				if (msgPart != null) {
					String filePath = msgPart.getFilePath();
//					ImageSize imageSize = new ImageSize(Constants.IMAGE_LOCATION_THUMB_WIDTH, Constants.IMAGE_LOCATION_THUMB_HEIGHT);
					if (SystemUtil.isFileExists(filePath)) {
//						mImageLoader.displayImage(Scheme.FILE.wrap(filePath), new TextViewAware(holder.tvContent, imageSize), chatImageOptions, new MyImageLoaderListener(holder.tvContentDesc, type, msgInfo));
						mImageLoader.displayImage(Scheme.FILE.wrap(filePath), holder.ivContentImg, chatImageOptions, new MyImageLoaderListener(holder.tvContentDesc, type, msgInfo));
					} else {
//						mImageLoader.displayImage(null, new TextViewAware(holder.tvContent, imageSize), chatImageOptions, new MyImageLoaderListener(holder.tvContentDesc, type, msgInfo));
						mImageLoader.displayImage(null, holder.ivContentImg, chatImageOptions, new MyImageLoaderListener(holder.tvContentDesc, type, msgInfo));
					}
				}
				break;
			default:
				break;
			}
			
//			holder.ivHeadIcon.setImageResource(R.drawable.ic_chat_default_big_head_icon);
			if (type == TYPE_OUT) {	//自己发送的消息
				//显示自己的头像
				String iconPath = mine.getIconShowPath();
				if (SystemUtil.isFileExists(iconPath)) {
					mImageLoader.displayImage(Scheme.FILE.wrap(iconPath), holder.ivHeadIcon, headIconOptions);
				} else {
					mImageLoader.displayImage(null, holder.ivHeadIcon, headIconOptions);
				}
				
				switch (msgInfo.getSendState()) {
				case SENDING:	//正在发送
					holder.ivMsgState.setVisibility(View.VISIBLE);
					holder.ivMsgState.setImageResource(R.drawable.chat_msg_state_sending);
					Animation rotateAnim = AnimationUtils.loadAnimation(context, R.anim.chat_msg_sending);
					holder.ivMsgState.startAnimation(rotateAnim);
					break;
				case SUCCESS:	//发送成功
					holder.ivMsgState.clearAnimation();
					holder.ivMsgState.setVisibility(View.GONE);
					break;
				case FAILED:
					holder.ivMsgState.setVisibility(View.VISIBLE);
					holder.ivMsgState.clearAnimation();
					holder.ivMsgState.setImageResource(R.drawable.chat_msg_state_failed_selector);
					break;
				default:
					break;
				}
			} else {	//接收的消息，对方发送的消息
				if (otherSide != null) {
					//显示用户图像
					UserVcard otherVcard = otherSide.getUserVcard();
					if (otherVcard != null) {
						String iconPath = otherVcard.getIconShowPath();
						if (SystemUtil.isFileExists(iconPath)) {
							mImageLoader.displayImage(Scheme.FILE.wrap(iconPath), holder.ivHeadIcon, headIconOptions);
						} else {
							mImageLoader.displayImage(null, holder.ivHeadIcon, headIconOptions);
						}
					} else {
						mImageLoader.displayImage(null, holder.ivHeadIcon, headIconOptions);
					}
				} else {
					mImageLoader.displayImage(null, holder.ivHeadIcon, headIconOptions);
				}
				
				holder.ivMsgState.setVisibility(View.GONE);
				if (!msgInfo.isRead()) {	//未读，则更新读取状态
					msgInfo.setRead(true);
					SystemUtil.getCachedThreadPool().execute(new Runnable() {
						
						@Override
						public void run() {
							msgManager.updateMsgReadStatus(msgThread, msgInfo);
						}
					});
				}
			}

			holder.contentLayout.setOnClickListener(new MsgItemClickListener(type, msgInfo, position, holder));
			
//			holder.tvContent.setOnTouchListener(new MyMsgItemTouchListener(msgInfo));
			holder.contentLayout.setOnLongClickListener(new MyMsgItemLongClickListener(type, msgInfo, msgId));
			holder.ivHeadIcon.setOnClickListener(new MsgItemClickListener(type, msgInfo, position, holder));
			holder.ivMsgState.setOnClickListener(new MsgItemClickListener(type, msgInfo, position, holder));
			return convertView;
		}

		@Override
		public int getItemViewType(int position) {
			MsgInfo mi = list.get(position);
			if (mi.isComming()) {	//接收的消息
				return TYPE_IN;
			} else {	//发送的消息
				return TYPE_OUT;
			}
		}

		@Override
		public int getViewTypeCount() {
			return TYPE_COUNT;
		}

		/**
		 * 显示图片
		 * @param msgInfo 对应的消息实体
		 * @param imageView 显示图片的控件
		 */
		public void displayImage(MsgInfo msgInfo, ImageView imageView) {
			if (msgInfo != null) {
				MsgPart msgPart = msgInfo.getMsgPart();
				if (msgPart != null) {
					String showPath = null;
					String thumbPath = msgPart.getThumbPath();
					if (SystemUtil.isFileExists(thumbPath)) {
						showPath = thumbPath;
					} else {
						String filePath = msgPart.getFilePath();
						if (SystemUtil.isFileExists(filePath)) {
							showPath = filePath;
						}
					}
					String imageUri = null;
					if (!TextUtils.isEmpty(showPath)) {
						imageUri = Scheme.FILE.wrap(showPath);
					}
					mImageLoader.displayImage(imageUri, imageView, chatImageOptions);
				}
			}
		}

		/**
		 * 复选框的选择监听器
		 */
		class OnCheckedChangeListenerImpl implements CompoundButton.OnCheckedChangeListener {
			private MsgViewHolder holder;
			private String msgId;

			public OnCheckedChangeListenerImpl(MsgViewHolder holder, String msgId) {
				this.holder = holder;
				this.msgId = msgId;
			}

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					mSelectSize ++;
					mSelectSize = (mSelectSize > mMsgTotalCount) ? mMsgTotalCount : mSelectSize;
					mSelectMap.put(msgId, true);
				} else {
					mSelectMap.remove(msgId);
					mSelectSize --;
					mSelectSize = mSelectSize < 0 ? 0 : mSelectSize;
				}
				if (mActionMode != null) {
					Menu actionMenu = mActionMode.getMenu();
					MenuItem forwardMenu = null;
					MenuItem deleteMenu = null;
					if (actionMenu != null) {
						forwardMenu = actionMenu.findItem(R.id.action_forward);	//转发
						deleteMenu = actionMenu.findItem(R.id.action_delete);	//删除
					}
					if (mSelectSize > 0) {
						mActionMode.setTitle(String.valueOf(mSelectSize));
						if (forwardMenu != null) {
							forwardMenu.setEnabled(true);
						}
						if (deleteMenu != null) {
							deleteMenu.setEnabled(true);
						}
					} else {
						mActionMode.setTitle(null);
						if (forwardMenu != null) {
							forwardMenu.setEnabled(false);
						}
						if (deleteMenu != null) {
							deleteMenu.setEnabled(false);
						}
					}
				}
			}
		}
		
	}
	
	
	/**
	 * 控件的touch事件监听器
	 * @author huanghui1
	 * @update 2015年3月4日 下午6:11:49
	 */
	class MyMsgItemTouchListener implements OnTouchListener {
		/**
		 * 消息对象
		 */
		private MsgInfo msgInfo;
		
		private final int DOUBLE_TAP_TIMEOUT = 200;
		private final int LONG_CLICK_TIMEOUT = 1000;
		private MotionEvent mCurrentDownEvent;  
		private MotionEvent mPreviousUpEvent; 
		private long firstDownTime = 0;

		public MyMsgItemTouchListener(MsgInfo msgInfo) {
			super();
			this.msgInfo = msgInfo;
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				v.setPressed(true);
			} else if (event.getAction() == MotionEvent.ACTION_UP) {
				v.setPressed(false);
			}
			if (msgInfo.getMsgType() == Type.TEXT) {	//文本消息
				if (event.getAction() == MotionEvent.ACTION_DOWN) {  //双击
					firstDownTime = System.currentTimeMillis();
		            if (mPreviousUpEvent != null  
		                    && mCurrentDownEvent != null  
		                    && isConsideredDoubleTap(mCurrentDownEvent,  
		                            mPreviousUpEvent, event)) {
		            	Intent intent = new Intent(mContext, MsgShowActivity.class);
		            	intent.putExtra(MsgShowActivity.ARG_MSG_CONTENT, msgInfo.getContent());
		            	ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight());
						ActivityCompat.startActivity(ChatActivity.this, intent, options.toBundle());
		            } else if (isLongClick(firstDownTime)) {
		            	v.performLongClick();
		            	return true;
		            }
		            mCurrentDownEvent = MotionEvent.obtain(event);  
//		            event.recycle();
		        } else if (event.getAction() == MotionEvent.ACTION_UP) {  
		            mPreviousUpEvent = MotionEvent.obtain(event);
//		            event.recycle();
		        }
				return true;
			} else {
				return false;
			}
		}
		
		private boolean isConsideredDoubleTap(MotionEvent firstDown,  
		        MotionEvent firstUp, MotionEvent secondDown) {  
		    if (secondDown.getEventTime() - firstUp.getEventTime() > DOUBLE_TAP_TIMEOUT) {  
		        return false;  
		    }  
		    int deltaX = (int) firstUp.getX() - (int) secondDown.getX();  
		    int deltaY = (int) firstUp.getY() - (int) secondDown.getY();  
		    return deltaX * deltaX + deltaY * deltaY < 10000;  
		}
		
		private boolean isLongClick(long previousTime) {
			return System.currentTimeMillis() - previousTime >= LONG_CLICK_TIMEOUT;
		}
		
	}
	
	/**
	 * 控件长按事件
	 * @author huanghui1
	 * @update 2015年2月15日 下午8:26:47
	 */
	class MyMsgItemLongClickListener implements OnLongClickListener {
		/**
		 * 消息的类型，分为接收的消息和发送的消息
		 */
		private int itemType;
		/**
		 * 消息对象
		 */
		private MsgInfo msgInfo;

		/**
		 * 选择项的消息id
		 */
		private String msgId;

		public MyMsgItemLongClickListener(int itemType, MsgInfo msgInfo, String msgId) {
			super();
			this.itemType = itemType;
			this.msgInfo = msgInfo;
			this.msgId = msgId;
		}

		@Override
		public boolean onLongClick(View v) {
			String[] array = getResources().getStringArray(R.array.chat_msg_context_menu);
			List<ContextMenuItem> menus = new ArrayList<>();
			for (int i = 0; i < array.length; i++) {
				ContextMenuItem item = new ContextMenuItem(i + 1, array[i]);
				menus.add(item);
			}
			if (SystemUtil.isNotEmpty(menus)) {
				if (Type.TEXT != msgInfo.getMsgType()) {
					menus.remove(0);	//除了文本外，其他的消息都不能复制,所以，删除“复制”菜单项
				}
				String dialogTitle = null;
				if (MsgAdapter.TYPE_IN == itemType) {	//接收的消息
					dialogTitle = otherSide.getName();
				} else {
					dialogTitle = mine.getName();
				}
				MaterialDialog.Builder builder = new MaterialDialog.Builder(mContext);
				final MaterialDialog dialog = builder.title(dialogTitle)
					.disableDefaultFonts()
					.adapter(new MenuItemAdapter(menus, mContext))
					.build();
				ListView listView = dialog.getListView();
				if (listView != null) {
					listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

						@Override
						public void onItemClick(AdapterView<?> parent, View view,
												final int position, long id) {
							switch ((int) id) {
								case MENU_COPY:	//复制
									SystemUtil.copyText(msgInfo.getContent());
									break;
								case MENU_FORWARD:	//转发
									break;
								case MENU_DELETE:	//删除
									pDialog = ProgressDialog.show(mContext, null, getString(R.string.loading), true);
									SystemUtil.getCachedThreadPool().execute(new Runnable() {

										@Override
										public void run() {
											pDialog.dismiss();
											if (msgManager.deleteMsgInfoById(msgInfo, msgThread)) {
												mMsgInfos.remove(msgInfo);
												minusMsgTotalCount();
												mHandler.sendEmptyMessage(Constants.MSG_SUCCESS);
											} else {
												mHandler.sendEmptyMessage(Constants.MSG_FAILED);
											}
										}
									});
									break;
								case MENU_SHARE:	//分享
									if (Type.TEXT != msgInfo.getMsgType()) {//除了文本外
										String filepath = msgInfo.getMsgPart().getFilePath();
										if (SystemUtil.isFileExists(filepath)) {
											Intent in = new Intent(Intent.ACTION_SEND);// 启动分享发送到属性
											in.setType(msgInfo.getMsgPart().getMimeType());// 分享发送到数据类型
											in.putExtra(Intent.EXTRA_STREAM,Uri.parse("file://"+filepath));// 分享的内容
											in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);// 允许intent启动新的activity
											startActivity(Intent.createChooser(in,getResources().getString(R.string.share)));// 目标应用选择对话框的标题
										} else {
											SystemUtil.makeShortToast(R.string.file_not_exists);
										}
									} else {
										Intent intent=new Intent(Intent.ACTION_SEND);
										intent.setType("text/plain");
//					             intent.setType("image/*");
//								intent.putExtra(Intent.EXTRA_SUBJECT, "百信趣味阅读");
										intent.putExtra(Intent.EXTRA_TEXT, msgInfo.getContent()+"【来自百信趣味阅读】");
										intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
										startActivity(Intent.createChooser(intent, getResources().getString(R.string.share)));
									}
									break;
								case MENU_MORE:	//更多
									if (toolbar != null) {
										mActionMode = startSupportActionMode(new ActionModeCallback() {
											@Override
											public boolean onCreateActionMode(ActionMode mode, Menu menu) {
												MenuInflater menuInflater = getMenuInflater();
												menuInflater.inflate(R.menu.menu_chat_opt, menu);
												
												return true;
											}

											@Override
											public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
												switch (item.getItemId()) {
													case R.id.action_forward:	//转发
														mActionMode.finish();
														break;
													case R.id.action_delete:	//删除
														MaterialDialog.Builder builder = new MaterialDialog.Builder(mContext);
														builder.title(R.string.prompt)
																.content(R.string.chat_delte_msg_prompt)
																.positiveText(android.R.string.ok)
																.negativeText(android.R.string.cancel)
																.callback(new MaterialDialog.ButtonCallback() {
																	@Override
																	public void onPositive(MaterialDialog dialog) {
																		pDialog = ProgressDialog.show(mContext, null, getString(R.string.loading), true);
																		SystemUtil.getCachedThreadPool().execute(new Runnable() {
																			@Override
																			public void run() {
																				Set<String> keys = mSelectMap.keySet();
																				List<String> msgIdList = new ArrayList<>();
																				List<MsgInfo> deleteList = new ArrayList<>();
																				int unreadCount = 0;
																				for (String key : keys) {
																					Boolean value = mSelectMap.get(key);
																					if (value != null && value) {	//选中
																						MsgInfo tmpInfo = new MsgInfo();
																						tmpInfo.setThreadID(mThreadId);
																						tmpInfo.setMsgId(key);
																						int index = mMsgInfos.indexOf(tmpInfo);
																						if (index != -1) {
																							msgIdList.add(key);
																							tmpInfo = mMsgInfos.get(index);
																							deleteList.add(tmpInfo);
																							if (!tmpInfo.isRead()) {
																								unreadCount ++;
																							}
																						}
																					}
																				}
																				String[] msgIdArray = new String[msgIdList.size()];
																				msgIdList.toArray(msgIdArray);
																				boolean success = msgManager.deleteMsgsByIds(msgIdArray, unreadCount, msgThread);
																				Message msg = mHandler.obtainMessage();
																				if (success) {
																					mMsgInfos.removeAll(deleteList);
																					msg.arg1 = 1;	//要隐藏actionMode
																					msg.what = Constants.MSG_SUCCESS;
																				} else {
																					msg.what = Constants.MSG_FAILED;
																				}
																				mHandler.sendMessage(msg);
																				pDialog.dismiss();
																			}
																		});
																	}
																}).show();
														break;
												}
												return super.onActionItemClicked(mode, item);
											}

											@Override
											public void onDestroyActionMode(ActionMode mode) {
												outMoreMode(true);
												super.onDestroyActionMode(mode);
											}
										});

										//变为多选模式
										initMoreMode(msgId);
										break;
									}
								default:
									break;
							}
							dialog.dismiss();
						}
					});
				}
				dialog.show();
				return true;
			} else {
				return false;
			}
		}
		
	}
	
	/**
	 * 菜单列表的适配器
	 * @author huanghui1
	 * @update 2015年2月25日 下午5:55:39
	 */
	class MenuItemAdapter extends CommonAdapter<ContextMenuItem> {

        final int itemColor;
        
        public MenuItemAdapter(List<ContextMenuItem> list, Context context) {
			super(list, context);
			itemColor = DialogUtils.resolveColor(context, R.attr.md_item_color, Color.BLACK);
		}

        @Override
        public long getItemId(int position) {
        	ContextMenuItem item = (ContextMenuItem) getItem(position);
            return item.getItemId();
        }
        
        @Override
        public boolean isEnabled(int position) {
        	ContextMenuItem item = (ContextMenuItem) getItem(position);
        	return item.isEnable();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            MenuViewHoler holer = null;
            if (convertView == null) {
            	holer = new MenuViewHoler();
            	convertView = inflater.inflate(R.layout.md_listitem, parent, false);
            	
            	holer.textView = (TextView) convertView.findViewById(R.id.title);
            	convertView.setTag(holer);
            } else {
            	holer = (MenuViewHoler) convertView.getTag();
            }
            ContextMenuItem item = list.get(position);
            holer.textView.setText(item.getTitle());
            holer.textView.setTextColor(itemColor);
            holer.textView.setTag(item.getItemId() + ":" + item.getTitle());
            return convertView;
        }
        
        class MenuViewHoler {
        	TextView textView;
        }
    }
	
	class MyImageLoaderListener implements ImageLoadingListener {
		private TextView tvDesc;
		private int itemType;
		private MsgInfo msgInfo;

		public MyImageLoaderListener(TextView tvDesc, int itemType, MsgInfo msgInfo) {
			super();
			this.tvDesc = tvDesc;
			this.itemType = itemType;
			this.msgInfo = msgInfo;
		}

		@Override
		public void onLoadingStarted(String imageUri, View view) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onLoadingFailed(String imageUri, View view,
				FailReason failReason) {
			// TODO Auto-generated method stub
			((ImageView) view).setImageResource(R.drawable.chat_msg_default_location); 
		}

		@Override
		public void onLoadingComplete(String imageUri, View view,
				Bitmap loadedImage) {
			// TODO Auto-generated method stub
			if (loadedImage != null) {
				if (view instanceof TextView) {
					TextView textView = (TextView) view;
					Drawable drawable = ImageUtil.bitmapToDrawable(loadedImage);
					if (itemType == MsgAdapter.TYPE_IN) {	//接收的消息
						textView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
					} else {
						textView.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null);
					}
				}
//				tvDesc.setMaxWidth(loadedImage.getWidth());
				tvDesc.setText(msgInfo.getContent());
			}
		}

		@Override
		public void onLoadingCancelled(String imageUri, View view) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	/**
	 * 消息item的点击事件
	 * @author huanghui1
	 * @update 2015年2月11日 上午11:14:35
	 */
	class MsgItemClickListener implements OnClickListener {
		/**
		 * 类型分为两种，TYPE_IN和TYPE_OUT
		 */
		private int itemType;
		private MsgInfo msgInfo;
		private int position;
		private MsgViewHolder holder;

		public MsgItemClickListener(int itemType, MsgInfo msgInfo, int position, MsgViewHolder holder) {
			super();
			this.itemType = itemType;
			this.msgInfo = msgInfo;
			this.position = position;
			this.holder = holder;
		}
		
		/**
		 * 显示地理位置
		 * @param v
		 * @param location
		 * @author tiger
		 * @version 1.0.0
		 * @update 2015年5月3日 下午3:19:58
		 */
		private void showLocation(View v, String location) {
			String[] array = location.split(Constants.SPLITE_TAG_LOCATION); 
			//查看地理位置
			//纬度
			double latitude = 0.00;
			//经度
			double longitude = 0.00;
			try {
				latitude = Double.parseDouble(array[0]);
				longitude = Double.parseDouble(array[1]);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
			Intent intent = new Intent(mContext, LocationShowActivity.class);
			intent.putExtra(LocationShowActivity.ARG_LATITUDE, latitude);
			intent.putExtra(LocationShowActivity.ARG_LONGITUDE, longitude);
			intent.putExtra(LocationShowActivity.ARG_LOCATION_INFO, msgInfo.getContent());
			ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight());
			ActivityCompat.startActivity(ChatActivity.this, intent, options.toBundle());
		}

		@Override
		public void onClick(View v) {
			if (mIsBatchMode) {	//多选模式
				holder.cbChose.toggle();
			} else {
				switch (v.getId()) {
					case R.id.iv_msg_state:	//重发消息
						if(msgInfo.getSendState() == SendState.FAILED) {//update by dudejin
							sendMsg(msgInfo, true);
						}
						break;
					case R.id.content_layout:	//消息实体
						Type msgType = msgInfo.getMsgType();
						Intent intent = null;
						if (msgType == Type.TEXT) {	//文本消息
							intent = new Intent(mContext, MsgShowActivity.class);
							intent.putExtra(MsgShowActivity.ARG_MSG_CONTENT, msgInfo.getContent());
							ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight());
							ActivityCompat.startActivity(ChatActivity.this, intent, options.toBundle());
						} else {
							MsgPart msgPart = msgInfo.getMsgPart();
							if (msgPart != null) {
								String showPath = msgPart.getShowPath();
								File file = new File(showPath);
								if (msgType == Type.LOCATION) {	//地理位置的文件，不需要判断文件是否存在
									String location = msgPart.getDesc();
									if (TextUtils.isEmpty(location)) {
										location = msgInfo.getSubject();
										if (TextUtils.isEmpty(location)) {
											SystemUtil.makeShortToast(R.string.location_info_error);
										} else {
											showLocation(v, location);
										}
									} else {
										showLocation(v, location);
									}
								} else {
									if (SystemUtil.isFileExists(file)) {
										switch (msgType) {
											case IMAGE:	//打开图片
												String filePath = msgPart.getFilePath();
												boolean download = false;	//是否需要下载原始图片
												if (msgInfo.isComming() && (!msgPart.isDownloaded() || !SystemUtil.isFileExists(filePath))) {	//原始图片不存在或者没有下载原始图片
													filePath = showPath;
													download = true;
												}
												if (!SystemUtil.isFileExists(filePath)) {
													filePath = msgPart.getThumbPath();
												}
												intent = new Intent(mContext, ChatImagePreviewActivity.class);
												intent.putExtra(ChatImagePreviewActivity.ARG_IMAGE_PATH, filePath);
												intent.putExtra(PhotoFragment.ARG_TOUCH_FINISH, true);
												intent.putExtra(PhotoFragment.ARG_DOWNLOAD_IMG, download);
												intent.putExtra(MsgPart.ARG_MSG_PART, msgPart);
												ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight());
												ActivityCompat.startActivity(ChatActivity.this, intent, options.toBundle());
												break;
											case VOICE:	//语音类型的消息
												TextView view = (TextView) v.findViewById(R.id.tv_content);
												playVoice(showPath, position, itemType, view);
												break;
											case AUDIO:	//音频文件，则调用系统或者第三方应用打开
												intent = MimeUtils.getAudioFileIntent(file);
												startActivity(intent);
												break;
											case VIDEO:	//视频
												intent = MimeUtils.getVideoFileIntent(file);
												startActivity(intent);
												break;
											case FILE:	//其他文件
												intent = MimeUtils.getFileIntent(file, msgPart.getMimeType());
												startActivity(intent);
												break;
											default:
												break;
										}
									} else {
										SystemUtil.makeShortToast(R.string.file_not_exists);
									}
								}
							}
						}
						break;
					case R.id.iv_head_icon:	//点击了头像
						SystemUtil.makeShortToast("点击了头像");
						break;
				}
			}
		}
		
	}
	
	/**
	 * 播放语音的初始化操作
	 * @update 2015年2月11日 下午9:25:15
	 * @param filePath 要播放的文件路径
	 * @param position 当前播放的位置索引
	 * @param itemType 当前播放的消息类型，主要分为接收的消息和发送的消息
	 * @param view
	 */
	private void playVoice(String filePath, int position, int itemType, View view) {
		TextView textView = (TextView) view;
		AnimationDrawable animationDrawable = null;
		if (itemType == MsgAdapter.TYPE_IN) {	//接收的消息
			animationDrawable = (AnimationDrawable) mContext.getResources().getDrawable(R.drawable.chat_voice_play_in_anim);
			animationDrawable.setBounds(0, 0, animationDrawable.getMinimumWidth(), animationDrawable.getMinimumHeight());
			textView.setCompoundDrawables(animationDrawable, null, null, null);
		} else {	//发送的消息
			animationDrawable = (AnimationDrawable) mContext.getResources().getDrawable(R.drawable.chat_voice_play_out_anim);
			animationDrawable.setBounds(0, 0, animationDrawable.getMinimumWidth(), animationDrawable.getMinimumHeight());
			textView.setCompoundDrawables(null, null, animationDrawable, null);
		}
		animationDrawable.setOneShot(false);
		if (mPlayingPosition == position) {
			if (mIsPlaying) {	//如果正在播放，就停止播放，反之，则开始播放
				stopPlaying(animationDrawable, itemType, textView);
			} else {
				startPlaying(filePath, animationDrawable, itemType, textView);
			}
		} else {	//如果点击的不是当前播放的条目
			if (mPlayingView != null) {
				stopPlaying(mPlayingAnimation, mPlayingType, mPlayingView);
			}
			startPlaying(filePath, animationDrawable, itemType, textView);
		}
		mPlayingPosition = position;
		mPlayingType = itemType;
		mPlayingView = textView;
		Drawable[] drawables = textView.getCompoundDrawables();
		if (SystemUtil.isNotEmpty(drawables)) {
			if (drawables[0] instanceof AnimationDrawable) {
				mPlayingAnimation = (AnimationDrawable) drawables[0];
			} else {
				mPlayingAnimation = null;
			}
		} else {
			mPlayingAnimation = null;
		}
	}
	
	/**
	 * 停止播放语音文件
	 * @update 2015年2月11日 下午9:10:13
	 * @param animation 动画图片资源
	 * @param itemType 消息类型，分为接收的消息和发出的消息
	 * @param view 填充资源图片的view
	 */
	public void stopPlaying(AnimationDrawable animation, int itemType, TextView view) {
		if (mPlayer != null) {
			mPlayer.release();
			mPlayer = null;
		}
		if (animation != null && animation.isRunning()) {
			animation.stop();
		}
		Drawable drawable = null;
		if (itemType == MsgAdapter.TYPE_IN) { //in
			drawable = mContext.getResources().getDrawable(R.drawable.chat_voice_in);
			drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
			view.setCompoundDrawables(drawable, null, null, null);
		} else if (itemType == MsgAdapter.TYPE_OUT) {
			drawable = mContext.getResources().getDrawable(R.drawable.chat_voice_out);
			drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
			view.setCompoundDrawables(null, null, drawable, null);
		}
		mIsPlaying = false;
	}
	
	/**
	 * 停止播放语音文件
	 * @update 2015年2月11日 下午9:14:00
	 * @param animation
	 * @param itemType
	 * @param view
	 */
	public void startPlaying(String filePath, AnimationDrawable animation, int itemType, TextView view) {
		mPlayer = new MediaPlayer();
		mPlayer.setOnCompletionListener(new MyOnCompletionListener(animation, itemType, view));
		try {
			//设置要播放的文件
			mPlayer.setDataSource(filePath);
			mPlayer.prepare();
			//播放
			mPlayer.start();
			animation.start();
			mIsPlaying = true;
		} catch (Exception e) {
			e.printStackTrace();
			stopPlaying(mPlayingAnimation, itemType, view);
		}
	}
	
	/**
	 * 语音播放完成的监听器
	 * @author huanghui1
	 * @update 2015年2月11日 下午9:14:57
	 */
	class MyOnCompletionListener implements OnCompletionListener {
		private AnimationDrawable animation;
		private int itemType;
		private TextView view;

		public MyOnCompletionListener(AnimationDrawable animation,
				int itemType, TextView view) {
			super();
			this.animation = animation;
			this.itemType = itemType;
			this.view = view;
		}

		@Override
		public void onCompletion(MediaPlayer mp) {
			if (mp != null) {
				stopPlaying(animation, itemType, view);
			}
		}
		
	}
	
	/**
	 * 异步加载apk图标的线程
	 * @author huanghui1
	 * @update 2014年11月21日 下午6:03:44
	 */
	class LoadApkIconTask extends AsyncTask<String, Drawable, Drawable> {
		MsgViewHolder holder;
		public LoadApkIconTask(MsgViewHolder holder) {
			super();
			this.holder = holder;
		}
		@Override
		protected Drawable doInBackground(String... params) {
			Drawable drawable = SystemUtil.getApkIcon(params[0]);
			return drawable;
		}
		@Override
		protected void onPostExecute(Drawable result) {
			if(result != null) {
				holder.tvContent.setCompoundDrawablesWithIntrinsicBounds(result, null, null, null);
			}
			super.onPostExecute(result);
		}
	}
	
	final static class MsgViewHolder {
		TextView tvMsgTime;
		RelativeLayout layoutBody;
		ImageView ivHeadIcon;
		TextView tvContent;
		ImageView ivContentImg;
		TextView tvContentDesc;
		ImageView ivMsgState;
		FrameLayout contentImgLayout;
		View contentLayout;
		private CheckBox cbChose;
	}
	
	/**
	 * 添加消息的总量
	 * @update 2015年3月6日 下午5:19:20
	 */
	private synchronized void addMsgTotalCount() {
		mMsgTotalCount += 1;
	}
	
	/**
	 * 添加置顶数量的消息数量
	 * @update 2015年3月6日 下午5:32:48
	 * @param count
	 */
	public synchronized void addMsgTotalCount(int count) {
		mMsgTotalCount += count;
	}
	
	/**
	 * 减少消息的总量
	 * @update 2015年3月6日 下午5:24:35
	 */
	private synchronized void minusMsgTotalCount() {
		mMsgTotalCount -= 1;
		mMsgTotalCount = mMsgTotalCount < 0 ? 0 : mMsgTotalCount;
	}
	
	/**
	 * 减少置顶数量的消息数量
	 * @update 2015年3月6日 下午5:33:16
	 * @param count
	 */
	private synchronized void minusMsgTotalCount(int count) {
		mMsgTotalCount -= count;
		mMsgTotalCount = mMsgTotalCount < 0 ? 0 : mMsgTotalCount;
	}
	
	/**
	 * 重置标题
	 * @update 2015年9月12日 下午4:28:36
	 */
	private void resetTitle() {
		if (otherSide != null) {
			String title = otherSide.getName();
			String oTitle = (String) getTitle();
			if (!title.equals(oTitle)) {
				setTitle(title);
			}
		}
	}
	
	/**
	 * 处理消息的广播
	 * @author huanghui1
	 * @update 2014年11月17日 上午9:20:42
	 */
	public class MsgProcessReceiver extends BroadcastReceiver {
		public static final String ACTION_PROCESS_MSG = "net.ibaixin.chat.PROCESS_ACCEPT_MSG_RECEIVER";
		public static final String ACTION_REFRESH_MSG = "net.ibaixin.chat.REFRESH_ACCEPT_MSG_RECEIVER";
		public static final String ACTION_UPDATE_CHAT_STATE = "net.ibaixin.chat.UPDATE_CHAT_STATE_MSG_RECEIVER";
		
		public static final String ARG_FILE_PATH = "arg_file_path";
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			MsgInfo msgInfo = null;
			switch (action) {
			case ACTION_PROCESS_MSG:	//处理接收的聊天消息
				msgInfo = intent.getParcelableExtra(ARG_MSG_INFO);
				if (msgInfo != null) {
					resetTitle();
					
					mMsgInfos.add(msgInfo);
					addMsgTotalCount();
					msgAdapter.notifyDataSetChanged();
					
				}
				break;
			case ACTION_REFRESH_MSG:	//刷新消息列表
				msgAdapter.notifyDataSetChanged();
				break;
			case ACTION_UPDATE_CHAT_STATE:	//根据对方的输入状态来更新顶部标题，如：对方正在输入...
				msgInfo = intent.getParcelableExtra(ARG_MSG_INFO);
				if (msgInfo != null) {
					int threadId = msgInfo.getThreadID();
					if (mThreadId == threadId) {	//同一个会话
						try {
							ChatState state = ChatState.valueOf(msgInfo.getContent());
							if (state != null) {
								String oTitle = (String) getTitle();
								switch (state) {
								case composing:	//对方正在输入...
									String newTitle = getString(R.string.chat_state_composing);
									if (!oTitle.equals(newTitle)) {
										setTitle(newTitle);
									}
									break;
//								case active:	//接收到消息
//									break;
//								case paused:	//对方暂停输入
//									break;
//								case inactive:	//对方聊天窗口失去焦点
//									break;
//								case gone:	//对方焦点被关闭
//									break;
								default:
									if (otherSide != null) {
										String title = otherSide.getName();
										if (!oTitle.equals(title)) {
											setTitle(title);
										}
									}
									break;
								}
							}
						} catch (Exception e) {
							Log.e(e.getMessage());
						}
						
					}
				}
				break;
			default:
				break;
			}
			if (etContent.hasFocus() && !lvMsgs.hasFocus()) {	//有焦点就滚动到最后一条记录
				scrollMyListViewToBottom(lvMsgs);
			}
		}
		
	}
	
	/**
	 * 消息监听的观察者
	 * @author huanghui1
	 * @update 2015年10月26日
	 */
	class MsgContentObserver extends net.ibaixin.chat.util.ContentObserver {

		public MsgContentObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void update(Observable<?> observable, int notifyFlag, NotifyType notifyType, final Object data) {
			MsgInfo msgInfo = null;
			switch (notifyFlag) {
				case Provider.MsgInfoColumns.NOTIFY_FLAG:	//消息的通知
					switch (notifyType) {
						case ADD:	//来了新消息
							if (data != null) {
								msgInfo = (MsgInfo) data;
								resetTitle();

								mMsgInfos.add(msgInfo);
								addMsgTotalCount();
								msgAdapter.notifyDataSetChanged();
							}
							break;
						case UPDATE:	//更新新消息
							if (data != null) {
								msgInfo = (MsgInfo) data;
								int index = mMsgInfos.indexOf(msgInfo);
								if (index != -1) {	//存在
									msgInfo = mMsgInfos.get(index);
									//局部更新
									updateView(index, msgInfo);
								}
							}
							break;
					}
					break;
				case Provider.MsgPartColumns.NOTIFY_FLAG:	//附件更新的通知
					switch (notifyType) {
						case UPDATE:	//更新
							if (data != null) {
								SystemUtil.getCachedThreadPool().execute(new Runnable() {
									@Override
									public void run() {
										MsgPart msgPart = (MsgPart) data;
										String msgId = msgPart.getMsgId();
										boolean download = msgPart.isDownloaded();
										MsgInfo tmpInfo = new MsgInfo();
										tmpInfo.setMsgId(msgId);
										tmpInfo.setThreadID(mThreadId);
										int index = mMsgInfos.indexOf(tmpInfo);
										if (index != -1) {	//存在
											MsgInfo msgInfo = mMsgInfos.get(index);
											MsgPart part = msgInfo.getMsgPart();
											if (part != null) {	//更新消息附件的下载状态，不需要刷新界面
												part.setDownloaded(download);
											}
										}
									}
								});

							}
							break;
					}
					break;
				case Provider.NotifyColumns.NOTIFY_MSG_UPLOAD_FLAG:	//消息附件上传进度条的更新
					if (data != null) {
						MsgUploadInfo uploadInfo = (MsgUploadInfo) data;
						msgInfo = uploadInfo.getMsgInfo();
						int progress = uploadInfo.getProgress();
					}
					break;
			}
		}
	}
	/*class MsgContentObserver extends ContentObserver {

		public MsgContentObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange, Uri uri) {
			if (uri != null) {
				MsgInfo msgInfo = msgManager.getMsgInfoByUri(uri);
				if (msgInfo != null) {
					if (!mMsgInfos.contains(msgInfo)) {
						mMsgInfos.add(msgInfo);
						addMsgTotalCount();
					}
					msgAdapter.notifyDataSetChanged();
					if (etContent.hasFocus() && !lvMsgs.hasFocus()) {	//有焦点就滚动到最后一条记录
						scrollMyListViewToBottom(lvMsgs);
					}
				}
			} else {
				onChange(selfChange);
			}
		}

		@Override
		public void onChange(boolean selfChange) {
			new LoadDataTask(false).execute();
		}
		
	}*/

	@Override
	public void onEmojiconClicked(Emojicon emojicon) {
		EmojiTypeFragment.input(etContent, emojicon);
	}

	@Override
	public void onEmojiconBackspaceClicked(View v) {
		EmojiTypeFragment.backspace(etContent);
	}

	/**
	 * 局部更新adapter
	 * @param position 要更新的索引位置
	 * @param msgInfo 要更新的实体对象
	 * @update 2015年8月20日 下午2:54:22
	 */
	private void updateView(int position, MsgInfo msgInfo) {
		//得到第一个可显示控件的位置，  
		int visiblePosition = lvMsgs.getFirstVisiblePosition();
		//只有当要更新的view在可见的位置时才更新，不可见时，跳过不更新 
		int relativePosition = position - visiblePosition;
		if (msgAdapter != null) {
			if (relativePosition >= 0) {
				//得到要更新的item的view  
				View view = lvMsgs.getChildAt(relativePosition);
				if (view != null) {
					//从view中取得holder  
					Object tag = view.getTag();
					if (tag != null && tag instanceof MsgViewHolder) {
						MsgViewHolder holder = (MsgViewHolder) tag;

						msgAdapter.displayImage(msgInfo, holder.ivContentImg);
					}
				}
			}
		}
	}

}
