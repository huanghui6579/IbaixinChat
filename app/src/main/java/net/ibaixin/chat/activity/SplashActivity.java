package net.ibaixin.chat.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import net.ibaixin.chat.R;
import net.ibaixin.chat.model.SystemConfig;
import net.ibaixin.chat.service.CoreService;
import net.ibaixin.chat.update.UpdateManager;
import net.ibaixin.chat.update.UpdateService;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.util.XmppConnectionManager;

/**
 * 欢迎界面
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年6月25日 下午8:36:01
 */
public class SplashActivity extends BaseActivity implements OnClickListener {
	private final int REQ_ACTION_SHARE = 1;
	/**
	 * 延迟时间2秒
	 */
	private int delayTime = 1500;
	
	private SystemConfig systemConfig;
	
	private Button btnLogin;
	private Button btnRegist;

	/**
	 * 是否分享过来的
	 */
	boolean mIsActionShare;

	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what){
				case UPDATESOFTVERSION:
					Intent service = new Intent(mContext, UpdateService.class);
					service.putExtra(UpdateService.FLAG_SYNC, UpdateService.FLAG_UPDATESOFT);
					startService(service);
					break;
			}
		}

	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		final boolean isfirstLogin = systemConfig.isFirstLogin();
		final String account = systemConfig.getAccount();
		final String password = systemConfig.getPassword();
		if (isfirstLogin && TextUtils.isEmpty(account)) {	//第一次启动应用，没有登录过
			btnLogin.setVisibility(View.VISIBLE);
			btnRegist.setVisibility(View.VISIBLE);
			
			application.addActivity(this);
			
		} else {	//延迟进入登录界面
			mHandler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					
					Intent intent = new Intent();
					if (!isfirstLogin && !TextUtils.isEmpty(account) && !TextUtils.isEmpty(password)) {	//不是第一次登录，则直接进入主界面，在后台登录
						intent.setClass(mContext, MainActivity.class);
					} else {
						intent.setClass(mContext, LoginActivity.class);
					}
					if (mIsActionShare) {
						intent.putExtra(ActionShareActivity.ARG_ACTION_SHARE, mIsActionShare);
						startActivityForResult(intent, REQ_ACTION_SHARE);
					} else {
						startActivity(intent, true);
						finish();
					}
				}
			}, delayTime);
		}

		if (application.isNetWorking()) {
			SystemUtil.getCachedThreadPool().execute(new Runnable() {// 检查软件版本
				public void run() {
					if (!UpdateManager.checkSoftVersionIsLast()) {
						mHandler.sendEmptyMessage(UPDATESOFTVERSION);
					}
				}
			});
		}
	}
	
	@Override
	protected boolean hasExitAnim() {
		return false;
	}
	
	@Override
	public void onBackPressed() {
		mHandler.removeCallbacksAndMessages(null);
		super.onBackPressed();
	}

	@Override
	protected int getContentView() {
		return R.layout.activity_splash;
	}

	@Override
	protected void initView() {
		btnLogin = (Button) findViewById(R.id.btn_login);
		btnRegist = (Button) findViewById(R.id.btn_regist);
	}

	@Override
	protected void initData() {
		systemConfig = application.getSystemConfig();
		XmppConnectionManager.getInstance().init(systemConfig);

		Intent actionIntent = getIntent();
		if (actionIntent != null) {
			mIsActionShare = actionIntent.getBooleanExtra(ActionShareActivity.ARG_ACTION_SHARE, false);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		setResult(resultCode);
		finish();
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void addListener() {
		btnLogin.setOnClickListener(this);
		btnRegist.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		
		Intent intent = new Intent();
		intent.putExtra(ActionShareActivity.ARG_ACTION_SHARE, mIsActionShare);
		switch (v.getId()) {
		case R.id.btn_login:	//进入登录界面
			intent.setClass(mContext, LoginActivity.class);
			intent.putExtra(ARG_DISPLAY_UP, true);
			break;
		case R.id.btn_regist:	//进入注册界面
			intent.setClass(mContext, RegistActivity.class);
			break;
		default:
			break;
		}
		startActivity(intent, true);
	}

}
