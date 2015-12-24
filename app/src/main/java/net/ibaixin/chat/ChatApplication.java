package net.ibaixin.chat;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.os.Environment;
import android.text.TextUtils;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.baidu.mapapi.SDKInitializer;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

import net.ibaixin.chat.model.Emoji;
import net.ibaixin.chat.model.EmojiType;
import net.ibaixin.chat.model.Personal;
import net.ibaixin.chat.model.SystemConfig;
import net.ibaixin.chat.service.CoreService;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.util.XmppConnectionManager;
import net.ibaixin.chat.volley.toolbox.MultiPartStack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 应用程序入口
 * @author huanghui1
 *
 */
public class ChatApplication extends Application {
	private LinkedList<Activity> activities = new LinkedList<>();
	
	private static final String TAG = ChatApplication.class.getSimpleName();
	
	private SystemConfig systemConfig;
	
	private static ChatApplication mInstance;
	
//	private SharedPreferences preferences;
	
	private String webviewCacheDir ;//webview缓存目录
	
	/**
	 * 当前的用户
	 */
	private Personal currentUser = null;
	/**
	 * 当前用户的账号
	 */
	private String currentAccount = null;
	
	/**
	 * 经典表情集合
	 */
	private Map<String, Emoji> mEmojiMap = null;
	private List<Emoji> mEmojis = null;
	/**
	 * 表情类型集合
	 */
	private List<EmojiType> mEmojiTypes = null;
	private int emojiTypeCount = 0;
	
	private int emojiPageCount = 0;
	
	/**
	 * 该账号对应的数据库根目录,默认在/data/data/packagename/IBaiXinChat/账号md5/目录下
	 */
	private String mAccountDbDir = null;
	
	/**
	 * 每页显示的表情数量，不含删除按钮
	 */
	private final int PAGE_SIZE = 20;
	
	/**
	 * 当前网络是否可用
	 */
	private boolean isNetWorking = true;
	
	/**
	 * volley 网络请求的队列
	 */
	private RequestQueue mRequestQueue;

	/**
	 * 是否主动退出该应用，如果不是，则连接断开后会自动重连
	 */
	private boolean mSureExit = false;
	
	@Override
	public void onCreate() {
		super.onCreate();
		mInstance = this;
		systemConfig = new SystemConfig();
		currentUser = new Personal();
		
		initNativeLib();
		
		Log.setPolicy(Log.LOG_ERROR_TO_FILE);
		
		// 在使用 SDK 各组间之前初始化 context 信息，传入 ApplicationContext
		SDKInitializer.initialize(this);
		
		initSystemConfig();
		
		initImageLoaderConfig();

		initEmojiType();
		
		initEmoji();

		CrashHandler crashHandler = CrashHandler.getInstance();
		crashHandler.init(getApplicationContext());

//		makeWebViewCacheDir() ;
		
	}
	
	/**
	 * 加载本地库
	 * @update 2015年8月25日 上午10:35:49
	 */
	private void initNativeLib() {
		//TODO 添加nativelib
		System.loadLibrary("IbaixinChat");
	}
	
	/**
	 * 获取volley请求队列
	 * @return volley请求队列
	 * @update 2015年7月20日 下午4:11:58
	 */
	public RequestQueue getRequestQueue() {
	    if (mRequestQueue == null) {
	    	synchronized (mInstance) {
	    		if (mRequestQueue == null) {
	    			mRequestQueue = Volley.newRequestQueue(getApplicationContext(), new MultiPartStack());
	    		}
			}
	    }
	    return mRequestQueue;
	}
	
	/**
	 * 添加网络请求道队列中
	 * @param request 请求
	 * @param tag 请求的唯一标识
	 * @update 2015年7月20日 下午4:12:54
	 */
	public <T> void addToRequestQueue(Request<T> request, String tag) {
	    // set the default tag if tag is empty
		request.setTag(TextUtils.isEmpty(tag) ? TAG : tag);
	    getRequestQueue().add(request);
	}

	/**
	 * 添加网络请求道队列中
	 * @param request 请求
	 * @update 2015年7月20日 下午4:16:13
	 */
	public <T> void addToRequestQueue(Request<T> request) {
		addToRequestQueue(request, null);
	}

	public void cancelPendingRequests(Object tag) {
	    if (mRequestQueue != null) {
	        mRequestQueue.cancelAll(tag);
	    }
	}
	
	public void setNetWorking(boolean isNetWorking) {
		this.isNetWorking = isNetWorking;
	}
	
	public boolean isNetWorking() {
		return isNetWorking;
	}

	public boolean isSureExit() {
		return mSureExit;
	}

	public void setSureExit(boolean sureExit) {
		this.mSureExit = sureExit;
	}

	public String getCurrentAccount() {
		if (currentAccount != null) {
			return currentAccount;
		} else {
			if (systemConfig != null) {
				return systemConfig.getAccount();
			} else {
				return null;
			}
		}
	}

	public void setCurrentAccount(String currentAccount) {
		this.currentAccount = currentAccount;
	}
	
	public String getAccountDbDir() {
		if (mAccountDbDir == null) {
			synchronized (ChatApplication.class) {
				SystemUtil.initAccountDbDir(currentAccount);
			}
		}
		return mAccountDbDir;
	}
	
	public void setAccountDbDir(String accountDbDir) {
		this.mAccountDbDir = accountDbDir;
	}
	
	public int getEmojiPageCount() {
		return emojiPageCount;
	}
	
	public int getEmojiTypeCount() {
		return emojiTypeCount;
	}

	/**
	 * 初始化表情
	 * @update 2014年10月27日 上午11:20:53
	 */
	private void initEmoji() {
		if (mEmojiMap == null) {
			mEmojiMap = new HashMap<>();
		} else {
			mEmojiMap.clear();
		}
		if (mEmojis == null) {
			mEmojis = new ArrayList<>();
		} else {
			mEmojis.clear();
		}
		List<String> list = SystemUtil.getEmojiFromFile("emoji");
		//表情格式为“f_static_000,[微笑]”
		if (list != null && list.size() > 0) {
			for (String str : list) {
				String[] arr = str.split(",");
				String faceName = arr[0];
				String description = arr[1];
				int resId = SystemUtil.getRespurceIdByName(faceName);
				if (resId > 0) {
					Emoji emoji = new Emoji();
					emoji.setResId(resId);
					emoji.setFaceName(faceName);
					emoji.setDescription(description);
					mEmojis.add(emoji);
					mEmojiMap.put(description, emoji);
				}
			}
		}
		int emojiSize = mEmojis.size();
		if (emojiSize > 0) {
			//向上取整：Math.ceil(1.4)=2.0 
			emojiPageCount = (int) Math.ceil(emojiSize / 20 + 0.1);
		}
	}
	
	/**
	 * 初始化表情的类型
	 * @update 2014年10月27日 下午7:59:06
	 */
	private void initEmojiType() {
		if (mEmojiTypes == null) {
			mEmojiTypes = new ArrayList<>();
		} else {
			mEmojiTypes.clear();
		}
		//assets文件内容格式：emotionstore_emoji_icon,经典表情,1,最后一个字段是表情的操作类型，分为“显示表情”、“管理本地表情”、“添加表情”
		List<String> list = SystemUtil.getEmojiFromFile("emojiType");
		if (list != null && list.size() > 0) {
			for (String str : list) {
				String[] arr = str.split(",");
				String fileName = arr[0];
				String description = arr[1];
				int optType = Integer.parseInt(arr[2]);
				int resId = SystemUtil.getRespurceIdByName(fileName);
				if (resId > 0) {
					EmojiType emojiType = new EmojiType();
					emojiType.setResId(resId);
					emojiType.setFileName(fileName);
					emojiType.setDescription(description);
					emojiType.setOptType(optType);
					mEmojiTypes.add(emojiType);
				}
			}
			emojiTypeCount = mEmojiTypes.size();
		}
	}
	
	/**
	 * 获得所有的表情集合
	 * @update 2014年10月27日 下午3:02:25
	 * @return
	 */
	public List<Emoji> getEmojis() {
		return mEmojis;
	}
	
	/**
	 * 获得所有的表情集合
	 * @update 2014年10月27日 下午3:02:25
	 * @return
	 */
	public Map<String, Emoji> getEmojiMap() {
		return mEmojiMap;
	}
	
	public List<EmojiType> geEmojiTypes() {
		return mEmojiTypes;
	}
	
	/**
	 * 根据当前页面的索引获得当前页面的所有表情
	 * @update 2014年10月27日 下午3:03:10
	 * @param position
	 * @return
	 */
	public List<Emoji> getCurrentPageEmojis(int position) {
		int startIndex = position * PAGE_SIZE;
		int endIndex = startIndex + PAGE_SIZE;
		int emojiSize = mEmojiMap.size();
		if (endIndex > emojiSize) {
			endIndex = emojiSize;
		}
		List<Emoji> subList = new ArrayList<>();
		subList.addAll(mEmojis.subList(startIndex, endIndex));
		if (subList.size() < PAGE_SIZE) {	//最后一页不足20个时，就补充空的占位置
			for (int i = subList.size(); i < PAGE_SIZE; i++) {
				Emoji emoji = new Emoji();
				emoji.setResTpe(Emoji.TYPE_EMPTY);
				subList.add(emoji);
			}
		}
		if (subList.size() == PAGE_SIZE) {
			Emoji emoji = new Emoji();
			emoji.setResId(R.drawable.chat_emoji_del_selector);
			emoji.setResTpe(Emoji.TYPE_DEL);
			subList.add(emoji);
		}
		return subList;
	}

	/**
	 * 配置图片加载的工具
	 * @update 2014年10月24日 上午11:08:16
	 */
	private void initImageLoaderConfig() {
		int width = 480;
		int height = 800;
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
					.memoryCacheExtraOptions(width, height)
			        .diskCacheExtraOptions(width, height, null)
			        .denyCacheImageMultipleSizesInMemory()	//同一个imageUri只允许在内存中有一个缓存的bitmap
			        .memoryCache(new LruMemoryCache(2 * 1024 * 1024))
			        .memoryCacheSize(2 * 1024 * 1024)
			        .diskCacheSize(50 * 1024 * 1024)
			        .defaultDisplayImageOptions(getDefaultDisplayOptions())
			        .writeDebugLogs()
			        .build();
		ImageLoader.getInstance().init(config);
	}
	
	/**
	 * 获取图片加载默认的图片显示配置
	 * @update 2014年10月24日 上午11:17:14
	 * @return
	 */
	private DisplayImageOptions getDefaultDisplayOptions() {
		DisplayImageOptions options = new DisplayImageOptions.Builder()
				.showImageOnLoading(R.drawable.ic_stub)
				.showImageForEmptyUri(R.drawable.ic_empty)
				.showImageOnFail(R.drawable.ic_error)
				.cacheInMemory(true)
				.cacheOnDisk(false)
				.imageScaleType(ImageScaleType.IN_SAMPLE_INT)
				.bitmapConfig(Bitmap.Config.RGB_565)	//防止内存溢出
				//.displayer(new FadeInBitmapDisplayer(200))
				.build();
		return options;
	}
	
	/**
	 * 初始化系统配置
	 * @update 2014年10月9日 上午8:17:20
	 */
	private void initSystemConfig() {
		SharedPreferences preferences = getSharedPreferences(Constants.SETTTING_LOGIN, Context.MODE_PRIVATE);
		systemConfig.setAccount(preferences.getString(Constants.USER_ACCOUNT, null));
		systemConfig.setPassword(preferences.getString(Constants.USER_PASSWORD, null));
		systemConfig.setFirstLogin(preferences.getBoolean(Constants.USER_ISFIRST, true));
//		systemConfig.setResource(preferences.getString(Constants.USER_RESOURCE, SystemUtil.getPhoneModel()));
//		systemConfig.setHost(preferences.getString(Constants.NAME_SERVER_HOST, Constants.SERVER_HOST));
//		systemConfig.setPort(preferences.getInt(Constants.NAME_SERVER_PORT, Constants.SERVER_PORT));
//		systemConfig.setServerName(preferences.getString(Constants.NAME_SERVER_NAME, Constants.SERVER_NAME));
	}
	
	/**
	 * 保存系统配置信息
	 * @update 2014年10月9日 上午8:22:30
	 */
	public void saveSystemConfig() {
		SharedPreferences preferences = getSharedPreferences(Constants.SETTTING_LOGIN, Context.MODE_PRIVATE);
		Editor editor = preferences.edit();
		editor.putString(Constants.USER_ACCOUNT, systemConfig.getAccount());
		editor.putString(Constants.USER_PASSWORD, systemConfig.getPassword());
		editor.putBoolean(Constants.USER_ISFIRST, systemConfig.isFirstLogin());
//		editor.putString(Constants.USER_RESOURCE, systemConfig.getResource());
//		editor.putString(Constants.NAME_SERVER_HOST, systemConfig.getHost());
//		editor.putString(Constants.NAME_SERVER_NAME, systemConfig.getServerName());
//		editor.putInt(Constants.NAME_SERVER_PORT, systemConfig.getPort());
		editor.commit();
	}
	
	/**
	 * systemconfig
	 * @update 2014年10月9日 上午8:17:49
	 * @return
	 */
	public SystemConfig getSystemConfig() {
		return systemConfig;
	}
	
	public Personal getCurrentUser() {
		return currentUser;
	}
	
	public void setCurrentUser(Personal currentUser) {
		this.currentUser = currentUser;
	}
	
	/**
	 * 获得全局的application
	 * @return 全局的application
	 */
	public static ChatApplication getInstance() {
		return mInstance;
	}
	
	/**
	 * 添加Activity的队列中，用于软件的退出
	 * @update 2014年10月8日 下午10:22:30
	 * @param activity
	 */
	public void addActivity(Activity activity) {
		activities.add(activity);
	}
	
	/**
	 * 退出应用应用程序
	 * @update 2014年10月8日 下午10:30:04
	 */
	public void exit() {
		exit(true);
	}

	/**
	 * 退出应用应用程序
	 * @param removePassword 是否移除本地密码
	 */
	public void exit(boolean removePassword) {
		if (removePassword) {
			removePassword();
		}
		setSureExit(true);
		XmppConnectionManager.getInstance().disconnect();
		Intent intent = new Intent(mInstance, CoreService.class);
		stopService(intent);
		for(Activity activity : activities) {
			activity.finish();
		}
		System.exit(0);
	}
	
	/**
	 * 从配置文件里移除密码，下载再启动应用时需要输入密码登录
	 * @update 2015年6月26日 下午9:25:46
	 */
	private void removePassword() {
		SharedPreferences preferences = getSharedPreferences(Constants.SETTTING_LOGIN, Context.MODE_PRIVATE);
		Editor editor = preferences.edit();
		editor.putString(Constants.USER_PASSWORD, "");
		editor.putBoolean(Constants.USER_ISFIRST, true);
//		editor.putString(Constants.USER_RESOURCE, systemConfig.getResource());
//		editor.putString(Constants.NAME_SERVER_HOST, systemConfig.getHost());
//		editor.putString(Constants.NAME_SERVER_NAME, systemConfig.getServerName());
//		editor.putInt(Constants.NAME_SERVER_PORT, systemConfig.getPort());
		editor.commit();
	}
	
	/**
	 * 将Activity从队列中移除
	 * @update 2014年10月8日 下午10:23:15
	 * @param activity
	 */
	public void removeActivity(Activity activity) {
		activities.remove(activity);
	}
	
	/**
	 * 检查该好友是否是自己
	 * @update 2014年10月24日 下午5:21:33
	 * @param username
	 * @return
	 */
	public boolean isSelf(String username) {
		return username.equals(currentUser.getUsername());
	}
	
	/**
	 * 创建webview缓存目录
	 */
	private void makeWebViewCacheDir() {
		// Check if the external storage is writeable
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			// Retrieve the base path for the application in the external
			// storage
			File externalStorageDir = Environment.getExternalStorageDirectory();
			if (externalStorageDir != null) {
				// {SD_PATH}/Android/data/com.devahead.androidwebviewcacheonsd
				webviewCacheDir = SystemUtil.getWebViewPath();
			}
			if (webviewCacheDir != null) {
				// {SD_PATH}/Android/data/com.devahead.androidwebviewcacheonsd/cache
				webviewCacheDir = SystemUtil.getDataParentFile().getAbsolutePath() + File.separator + "cache";
				boolean isCachePathAvailable = true;
				File dir = new File(webviewCacheDir);
				if (!dir.exists()) {
					// Create the cache path on the external storage
					isCachePathAvailable = dir.mkdirs();
				}
				if (!isCachePathAvailable) {
					// Unable to create the cache path
					dir = null;
				}
			}
		}
	}
	
/*	  @Override
	  public File getCacheDir()
	  {
	    // NOTE: this method is used in Android 2.2 and higher
	    if (webviewCacheDir != null)
	    {
	      // Use the external storage for the cache
	      return new File(webviewCacheDir);
	    }
	    else
	    {
	      // /data/data/com.devahead.androidwebviewcacheonsd/cache
	      return super.getCacheDir();
	    }
	  }*/
}
