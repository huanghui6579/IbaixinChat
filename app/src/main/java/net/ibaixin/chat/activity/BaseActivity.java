package net.ibaixin.chat.activity;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.R;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.view.ProgressDialog;

import java.lang.reflect.Field;

import me.imid.swipebacklayout.lib.SwipeBackLayout;
import me.imid.swipebacklayout.lib.Utils;
import me.imid.swipebacklayout.lib.app.SwipeBackActivityBase;
import me.imid.swipebacklayout.lib.app.SwipeBackActivityHelper;

/**
 * 所有Activity的父类
 * @author huanghui1
 *
 */
public abstract class BaseActivity extends AppCompatActivity implements SwipeBackActivityBase {
	
	protected static final String ARG_DISPLAY_UP = "arg_display_up";
	
	protected Context mContext;
	protected SharedPreferences preferences;
	protected ChatApplication application;
	
	private SwipeBackActivityHelper mSwipeBackHelper;
	
	//淘宝联盟，阿里妈妈推广
//	protected BannerProperties properties;
//	protected BannerController<?> mController;
//	protected MmuSDK mmuSDK;
	
	/**
	 * ActionBar是否允许有返回按钮
	 */
	private boolean homeAsUpEnabled = true;
	
	/**
	 * 是否允许滑动返回，默认为true
	 */
	private boolean mSwipeBackEnabled = true;
	
	/**
	 * 是否设置为全屏
	 */
	private boolean mIsFullScreen = false;
	
	protected static String TAG = null; 
	
	protected Toolbar toolbar;
	
	protected View mAppBar;
	
	/**
	 * 是否有退出动画
	 */
	private boolean mExitAnim = true;
	
	/**
	 * 退出的动画
	 */
	private int mExitAnimRes = R.anim.slide_right_out;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mContext = this;
		TAG = this.getClass().getCanonicalName();
		
		application = ChatApplication.getInstance();
		
		mIsFullScreen = isFullScreen();
		
		homeAsUpEnabled = isHomeAsUpEnabled();
		
		mSwipeBackEnabled = isSwipeBackEnabled();
		
		mExitAnim = hasExitAnim();
		
		mExitAnimRes = getExitAnimRes();
		
		//初始化滑动返回activity的设置
		initSwipeBack();

		initWidow();
		
		super.onCreate(savedInstanceState);
		
		application.addActivity(this);
		
		preferences = getSharedPreferences(Constants.SETTTING_LOGIN, Context.MODE_PRIVATE);
		
		setContentView(getContentView());
		
		initToolBar();
		
		forceShowActionBarOverflowMenu();
		
		initView();
		
		initData();
		
		addListener();
		
//		initAlimamaSDK() ;
//		startSupportActionMode(new ActionModeCallback());
	}
	
	/**
	 * 是否有退出动画
	 * @return
	 * @update 2015年8月21日 下午7:52:43
	 */
	protected boolean hasExitAnim() {
		return mExitAnim;
	}
	
	/**
	 * 获取退出的动画资源
	 * @return
	 * @update 2015年8月21日 下午8:14:43
	 */
	protected int getExitAnimRes() {
		return mExitAnimRes;
	}
	
	/**
	 * 跳转界面
	 * @param intent
	 * @param enterRes 进入的anim动画
	 * @param exitRes 退出的anim动画
	 * @update 2015年8月21日 下午7:31:56
	 */
	public void startActivity(Intent intent, int enterRes, int exitRes) {
		ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(this, enterRes, exitRes);
    	ActivityCompat.startActivity(this, intent, options.toBundle());
	}
	
	/**
	 * 跳转界面，退出动画为0
	 * @param intent
	 * @param enterRes 进入的anim动画
	 * @update 2015年8月21日 下午7:31:56
	 */
	public void startActivity(Intent intent, int enterRes) {
		startActivity(intent, enterRes, 0);
	}
	
	/**
	 * 跳转界面
	 * @param intent
	 * @param anim 是否需要进入动画
	 * @update 2015年8月21日 下午7:36:22
	 */
	public void startActivity(Intent intent, boolean anim) {
		if (anim) {
			this.startActivity(intent, R.anim.slide_right_in);
		} else {
			super.startActivity(intent);
		}
	}
	
	/**
	 * 有返回的跳转界面
	 * @param intent
	 * @param requestCode
	 * @param enterRes 进入动画
	 * @param exitRes 退出动画
	 * @update 2015年8月21日 下午7:38:39
	 */
	public void startActivityForResult(Intent intent, int requestCode, int enterRes, int exitRes) {
		startActivityForResult(intent, requestCode, enterRes, exitRes, null);
	}
	
	/**
	 * 有返回的跳转界面
	 * @param intent
	 * @param requestCode
	 * @param enterRes 进入动画
	 * @param exitRes 退出动画
	 * @param options
	 * @update 2015年8月21日 下午7:41:56
	 */
	public void startActivityForResult(Intent intent, int requestCode, int enterRes, int exitRes, Bundle options) {
		ActivityOptionsCompat optionsCompat = ActivityOptionsCompat.makeCustomAnimation(this, enterRes, exitRes);
		if (options == null) {
			options = optionsCompat.toBundle();
		} else {
			options.putAll(optionsCompat.toBundle());
		}
    	ActivityCompat.startActivityForResult(this, intent, requestCode, options);
	}
	
	/**
	 * 有返回的界面跳转
	 * @param intent
	 * @param requestCode
	 * @param anim 是否需要动画
	 * @update 2015年8月21日 下午7:44:25
	 */
	public void startActivityForResult(Intent intent, int requestCode, boolean anim) {
		if (anim) {
			this.startActivityForResult(intent, requestCode);
		} else {
			super.startActivityForResult(intent, requestCode);
		}
	}
	
	/**
	 * 有返回的界面跳转
	 * @param intent
	 * @param requestCode
	 * @param options
	 * @param anim 是否需要动画
	 * @update 2015年8月21日 下午7:45:49
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public void startActivityForResult(Intent intent, int requestCode, Bundle options, boolean anim) {
		if (anim) {
			this.startActivityForResult(intent, requestCode, options);
		} else {
			super.startActivityForResult(intent, requestCode ,options);
		}
	}
	
	@Override
	public void finish() {
		super.finish();
		if (hasExitAnim()) {
//			scrollToFinishActivity();
			overridePendingTransition(0, mExitAnimRes);
		}
	}

	/**
	 * 结束界面，是否有退出动画
	 * @param exitAnim
	 */
	protected void finish(boolean exitAnim) {
		super.finish();
		if (exitAnim) {
//			scrollToFinishActivity();
			overridePendingTransition(0, mExitAnimRes);
		}
	}
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		
		if (canSwipeBack()) {
			mSwipeBackHelper.onPostCreate();
		}
	}
	
	@Override
	public View findViewById(int id) {
		View v = super.findViewById(id);
        if (v == null && canSwipeBack()) {
        	return mSwipeBackHelper.findViewById(id);
        }
        return v;
	}
	
	@Override
	public SwipeBackLayout getSwipeBackLayout() {
		if (canSwipeBack()) {
			SwipeBackLayout backLayout = mSwipeBackHelper.getSwipeBackLayout();
			backLayout.setEdgeTrackingEnabled(SwipeBackLayout.EDGE_LEFT);
			return backLayout;
		} else {
			Log.w("---BaseActivity------not ---enable---getSwipeBackLayout---is null-----");
			return null;
		}
	}

	@Override
	public void setSwipeBackEnable(boolean enable) {
		if (canSwipeBack()) {
			getSwipeBackLayout().setEnableGesture(enable);
		}
	}

	@Override
	public void scrollToFinishActivity() {
		if (canSwipeBack()) {
			Utils.convertActivityToTranslucent(this);
			getSwipeBackLayout().scrollToFinishActivity();
		}
	}
	
	/**
     * 设置滑动从哪边开始，默认是从左边开始
     * @param point 滑动开始的方向
     * @update 2015年8月7日 下午4:01:07
     */
    public void setEdgeTrackingPoint(int point) {
    	if (canSwipeBack()) {
    		getSwipeBackLayout().setEdgeTrackingEnabled(point);
    	}
    }

	/**
	 * 是否可以滑动返回
	 * @return
	 * @update 2015年8月7日 下午5:04:08
	 */
	private boolean canSwipeBack() {
		return homeAsUpEnabled && mSwipeBackEnabled && mSwipeBackHelper != null;
	}
	
	/**
	 * 初始化滑动返回的配置,如果需要滑动返回，前提是返回箭头要显示
	 * 
	 * @update 2015年8月7日 下午4:56:46
	 */
	private void initSwipeBack() {
		if (homeAsUpEnabled && mSwipeBackEnabled) {
			mSwipeBackHelper = new SwipeBackActivityHelper(this);
			mSwipeBackHelper.onActivityCreate();
		}
	}
	
	/*@Override
	protected void onResume() {
		if (mController != null) {
            mController.load();
            mController.show();
        }
		super.onResume();
	}*/
	
	class ActionModeCallback implements ActionMode.Callback {

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	/** 
     * 强制显示 overflow menu 
     */  
    protected void forceShowActionBarOverflowMenu() {  
        try {  
            ViewConfiguration config = ViewConfiguration.get(this);  
            if (ViewConfigurationCompat.hasPermanentMenuKey(config)) {
            	Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");  
                if (menuKeyField != null) {  
                    menuKeyField.setAccessible(true);  
                    menuKeyField.setBoolean(config, false);  
                }
            }
        } catch (Exception e) {  
            e.printStackTrace();  
        }  
    }
	
	/**
	 * 是否有允许ActionBar左上角显示返回按钮
	 * @update 2014年11月10日 下午3:48:11
	 * @return
	 */
	protected boolean isHomeAsUpEnabled() {
		return homeAsUpEnabled;
	}
	
	/**
	 * 是否允许滑动返回activity，默认为true
	 * @return
	 * @update 2015年8月7日 下午4:53:28
	 */
	public boolean isSwipeBackEnabled() {
		return mSwipeBackEnabled;
	}
	
	/**
	 * 是否设置为全屏
	 * @update 2015年3月4日 下午7:41:21
	 * @return
	 */
	protected boolean isFullScreen() {
		return mIsFullScreen;
	}
	
	/**
	 * 开启沉浸模式
	 * @author tiger
	 * @update 2015年3月3日 下午11:09:34
	 * @param view
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
	protected void hideSystemUi(View view) {
		int uiOptions = view.getSystemUiVisibility();
		if (SystemUtil.hasSDK16()) {
			uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
			uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
			uiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
		}
		if (SystemUtil.hasSDK19()) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		}
		view.setSystemUiVisibility(uiOptions);
		//开启全屏模式
//		view.setSystemUiVisibility(
//	        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//	        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//	        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
//	        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
	}
	
	/**
	 * 取消沉浸模式
	 * @author tiger
	 * @update 2015年3月3日 下午11:10:01
	 * @param view
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	protected void showSystemUi(View view) {
		if (SystemUtil.hasSDK16()) {
			view.setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_LAYOUT_STABLE
					| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
		}
	}
	
	/**
	 * 初始化一些窗口信息
	 * @update 2014年10月10日 下午9:29:18
	 */
	protected void initWidow() {
		if (mIsFullScreen) {
			if (SystemUtil.hasSDK19()) {
				View view = getWindow().getDecorView();
				if (view != null) {
					hideSystemUi(view);
				} else {
					//全屏模式
					getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,  WindowManager.LayoutParams.FLAG_FULLSCREEN);
				}
			} else {
				//全屏模式
				getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,  WindowManager.LayoutParams.FLAG_FULLSCREEN);
			}
		}
		/*ActionBar actionBar = getSupportActionBar();
		if (homeAsUpEnabled) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		} else {
			actionBar.setDisplayHomeAsUpEnabled(false);
		}*/
	}
	
	/**
	 * 初始化ToolBar
	 * @update 2015年1月21日 上午10:04:23
	 */
	protected void initToolBar() {
		toolbar = (Toolbar) findViewById(R.id.toolbar);
		mAppBar = findViewById(R.id.app_bar_layout);
		if (toolbar != null) {
			setSupportActionBar(toolbar);
		}
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			if (homeAsUpEnabled) {
				actionBar.setDisplayHomeAsUpEnabled(true);
			} else {
				actionBar.setDisplayHomeAsUpEnabled(false);
			}
		}/* else {
			SystemUtil.makeShortToast("没有ActionBar或者ToolBar");
		}*/
	}
	
	/**
	 * 获得界面的布局文件id
	 * @return 布局文件id
	 */
	protected abstract int getContentView();
	
	/**
	 * 初始化界面
	 */
	protected abstract void initView();
	
	/**
	 * 初始化数据
	 * @update 2015年1月5日 下午9:34:01
	 */
	protected abstract void initData();
	
	/**
	 * 为控件注册监听器
	 */
	protected abstract void addListener();
	
//	protected void initAlimamaSDK(){}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:	//返回
			beforeBack();
			finish();
			return true;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * 在按下左上角的返回按钮时，可以处理的操作，该方法在finish()前调用
	 * @update 2015年1月9日 上午9:41:44
	 */
	protected void beforeBack() {
		
	}
	
	/**
	 * 显示dialog
	 * @update 2015年3月25日 
	 * @param pDialog
	 */
	public void showLoadingDialog(ProgressDialog pDialog) {
		pDialog = ProgressDialog.show(mContext, null, getString(R.string.logining), true);
	}
	/**
	 * 隐藏dialog
	 * @update 2014年10月10日 上午8:10:47
	 * @param pDialog
	 */
	public void hideLoadingDialog(ProgressDialog pDialog) {
		if(pDialog != null && pDialog.isShowing()) {
			pDialog.dismiss();
		}
	}
	
	@Override
	protected void onDestroy() {
		application.removeActivity(this);
		super.onDestroy();
	}
	
	/*protected void setupAlimama(ViewGroup nat, String slotId) {
        mmuSDK = MmuSDKFactory.getMmuSDK();
        mmuSDK.accountServiceInit(this);
        mmuSDK.init(ChatApplication.getInstance());//初始化SDK,该方法必须保证在集成代码前调用，可移到程序入口处调用
        properties = new BannerProperties(slotId, nat);
        mmuSDK.attach(properties);
        properties.setClickCallBackListener(new ClickCallBackListener() {

            @Override
            public void onClick() {
                Log.i("munion", "onclick");
            }
        });
        properties.setOnStateChangeCallBackListener(new OnStateChangeCallBackListener() {

            @Override
            public void onStateChanged(BannerState state) {
                Log.i("munion", "state = " + state);
            }
        });
        mController =  properties.getMmuController();
    }*/
    
    /*@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (!mmuSDK.accountServiceHandleResult(requestCode, resultCode, data,this)) {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}*/
}
