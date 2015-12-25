package net.ibaixin.chat.activity;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;

import net.ibaixin.chat.R;
import net.ibaixin.chat.fragment.ContactFragment;
import net.ibaixin.chat.fragment.MineFragment;
import net.ibaixin.chat.fragment.ThreadListFragment;
import net.ibaixin.chat.model.Personal;
import net.ibaixin.chat.model.SystemConfig;
import net.ibaixin.chat.service.CoreService;
import net.ibaixin.chat.service.CoreService.MainBinder;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.XmppConnectionManager;
import net.ibaixin.chat.util.XmppUtil;
import net.ibaixin.chat.view.IconPagerAdapterProvider;
import net.ibaixin.chat.view.IconTabPageIndicator;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.packet.Presence;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
/**
 * 系统主界面
 * @author huanghui1
 * @update 2014年10月8日 下午9:15:47
 */
public class MainActivity extends BaseActivity {
	private static final int FRAGMENT_SESSION_LIST = 0;
	private static final int FRAGMENT_CONTACT = 1;
	private static final int FRAGMENT_MINE = 2;

	public static final String ARG_SYNC_FRIENDS = "arg_sync_friends";
	public static final String ARG_INIT_POSITION = "arg_init_position";
	
	private IconTabPageIndicator mPageIndicator;
	private ViewPager mViewPager;

	//底部栏分割线
//	private View mDivider;
	
	private FragmentAdapter adapter;
	
	/**
	 * 是否初始化fragment标签页的索引位置
	 */
	private boolean initPosition = false;
	
	private static Map<Integer, String> tagMap = new HashMap<>();
	
	private CoreService coreService;
	
	private static String[] CONTENT = null;
    private static int[] ICONS = new int[] {
    	R.drawable.main_fun_session_selector,
    	R.drawable.main_fun_contact_selector,
    	R.drawable.main_fun_mine_selector
    };

    private ServiceConnection serviceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub
			MainBinder mBinder = (MainBinder) service;
			coreService = mBinder.getService();
//			boolean logined = XmppUtil.checkAuthenticated(XmppConnectionManager.getInstance().getConnection());
//			Log.d("------onServiceConnected--initPosition------" + initPosition + "--logined--" + logined);
//			if (!initPosition && logined) {	//不初始化卡片位置且已经登录了
//				coreService.initCurrentUser(application.getCurrentUser());
//			}
		}
	};

	@Override
	protected boolean isHomeAsUpEnabled() {
		return false;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
	    if((featureId == Window.FEATURE_ACTION_BAR || featureId == Window.FEATURE_OPTIONS_PANEL) && menu != null) {
	        if(menu.getClass().getSimpleName().equals("MenuBuilder")) {
	            try{
	                Method m = menu.getClass().getDeclaredMethod(
	                    "setOptionalIconsVisible", Boolean.TYPE);
	                m.setAccessible(true);
	                m.invoke(menu, true);
	            } catch(NoSuchMethodException e) {
	                Log.e(TAG, "onMenuOpened", e);
	            } catch(Exception e) {
	                throw new RuntimeException(e);
	            }
	        }
	    }
	    return super.onMenuOpened(featureId, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		Intent intent = null;
		switch (item.getItemId()) {
		case R.id.action_add_friend:	//添加好友
			intent = new Intent(mContext, AddFriendActivity.class);
			startActivity(intent);
			break;
		/*case R.id.action_exit:	//退出
			application.exit();
			return true;*/

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
    /**
     * 初始化当前用户的信息
     */
    private Personal initCurrentUserInfo() {
    	Personal temp = application.getCurrentUser();
    	SystemConfig sc = application.getSystemConfig();
    	temp.setUsername(sc.getAccount());
    	temp.setPassword(sc.getPassword());
    	temp.setStatus(Presence.Type.available.name());
    	temp.setMode(Presence.Mode.available.name());
    	temp.setResource(Constants.CLIENT_RESOURCE);
    	
    	application.setCurrentAccount(temp.getUsername());
    	
    	return temp;
    }

	@Override
	protected int getContentView() {
		return R.layout.activity_main;
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	@Override
	protected void initView() {
		mPageIndicator = (IconTabPageIndicator) findViewById(R.id.page_indicator);
		mViewPager = (ViewPager) findViewById(R.id.view_pager);
		/*mDivider = findViewById(R.id.divider);
		
		if (SystemUtil.hasSDK21()) {	//Android5.0或者之上
			mDivider.setOutlineProvider(new ViewOutlineProvider() {
				
				@TargetApi(Build.VERSION_CODES.LOLLIPOP)
				@Override
				public void getOutline(View view, Outline outline) {
					outline.setRect(0, 0, view.getWidth(), view.getHeight());
				}
			});
		}*/
	}
	
	@Override
	protected boolean hasExitAnim() {
		return false;
	}

	@Override
	protected void initData() {
		initCurrentUserInfo();
		
		AbstractXMPPConnection connection = XmppConnectionManager.getInstance().getConnection();
		boolean isLogined = XmppUtil.checkConnected(connection) && XmppUtil.checkAuthenticated(connection);
		Log.d("-----登录状态-----" + isLogined);
		Intent intent = getIntent();
		int syncFlag = 0;
		initPosition = false;
		if (intent == null) {
			syncFlag = CoreService.FLAG_SYNC_FRENDS;
		} else {
			if (isLogined) {	//已经连接上并且登录了，则需同步好友信息
				if (intent.getBooleanExtra(ARG_SYNC_FRIENDS, true)) {	//默认登录后需要同步好友基本信息
					Log.d("---已经通过登录或者注册界面登录，需要同步好友信息---");
					syncFlag = CoreService.FLAG_SYNC_FRENDS;
				} else {
					Log.d("---已经通过登录或者注册界面登录，不需要同步好友信息---");
					syncFlag = 0;
				}
			} else {//如果没有连接或者登录，则在后台登录
				Log.d("---后台登录---");
				syncFlag = CoreService.FLAG_LOGIN;
			}
			initPosition = intent.getBooleanExtra(MainActivity.ARG_INIT_POSITION, false);
		}
		CONTENT = getResources().getStringArray(R.array.main_function_lable);
		if (adapter == null) {
			adapter = new FragmentAdapter(getSupportFragmentManager());
		}
		
		mViewPager.setAdapter(adapter);

		Intent service = new Intent(mContext, CoreService.class);
		bindService(service, serviceConnection, Context.BIND_AUTO_CREATE);
		
		//从网络上更新好友列表的数据
//		Intent intent = new Intent(mContext, CoreService.class);
		service.putExtra(CoreService.FLAG_INIT_CURRENT_USER, CoreService.FLAG_INIT_PERSONAL_INFO);
//		if (isLogined) {	//要等好友信息同步好了后才能接受离线消息
//			service.putExtra(CoreService.FLAG_RECEIVE_OFFINE_MSG, CoreService.FLAG_RECEIVE_OFFINE);
			//发送同步个人信息的广播
//			Intent data = new Intent(CoreService.CoreReceiver.ACTION_SYNC_VCARD);
//			sendBroadcast(data);
//		}
		service.putExtra(CoreService.FLAG_SYNC, syncFlag);
		startService(service);
		
		if (initPosition) {
			coreService.clearNotify(CoreService.NOTIFY_ID_CHAT_MSG);
			mPageIndicator.setViewPager(mViewPager, 0);
		} else {
			mPageIndicator.setViewPager(mViewPager);
		}
		
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		
		initData();
		
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
	protected void addListener() {
		// TODO Auto-generated method stub
		mPageIndicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			
			@Override
			public void onPageSelected(int position) {
				// TODO Auto-generated method stub
				switch (position) {
				case FRAGMENT_CONTACT:	//好友列表
					String tag = tagMap.get(position);
					ContactFragment contactFragment = (ContactFragment) getSupportFragmentManager().findFragmentByTag(tag);
					if (contactFragment != null) {
						if (initPosition && contactFragment.isLoaded()) {
							contactFragment.setLoaded(false);
						}
						contactFragment.onload();
					}
					break;

				default:
					break;
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
		mPageIndicator.setOnTabReselectedListener(new IconTabPageIndicator.OnTabReselectedListener() {
			
			@Override
			public void onTabReselected(int position) {
				// TODO Auto-generated method stub
				
			}
		});
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_MENU:
			if (toolbar.isOverflowMenuShowing()) {
				toolbar.hideOverflowMenu();
			} else {
				toolbar.showOverflowMenu();
			}
			return true;

		default:
			break;
		}
		return super.onKeyUp(keyCode, event);
	}
	
	@Override
	protected void onDestroy() {
		unbindService(serviceConnection);
		super.onDestroy();
	}
	
	/**
	 * 延迟加载的回调
	 * @author huanghui1
	 * @update 2014年10月23日 下午8:54:56
	 */
	public interface LazyLoadCallBack {
		public void onload();
	}
	
	class FragmentAdapter extends FragmentPagerAdapter implements IconPagerAdapterProvider {

		public FragmentAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			Fragment fragment = null;
			switch (position) {
			case FRAGMENT_SESSION_LIST:	//会话聊天列表
				fragment = ThreadListFragment.newInstance();
				break;
			case FRAGMENT_CONTACT:	//好友列表
				fragment = ContactFragment.newInstance();
				break;
			case FRAGMENT_MINE:	//我
				fragment = MineFragment.newInstance();
				break;
			default:
				fragment = ThreadListFragment.newInstance();
				break;
			}
			return fragment;
		}
		
		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			if (tagMap == null) {
				tagMap = new HashMap<>();
			}
			// TODO Auto-generated method stub
			Fragment fragment = (Fragment) super.instantiateItem(container, position);
			tagMap.put(position, fragment.getTag());
			return fragment;
		}
		
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return CONTENT.length;
		}

		@Override
		public int getIconResId(int index) {
			// TODO Auto-generated method stub
			return ICONS[index];
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return CONTENT[position % CONTENT.length];
		}

		@Override
		public int getExtraCount() {
			return 0;
		}
		
	}
	
	@Override
	public void onBackPressed() {
		moveTaskToBack(true);
	}
	
}
