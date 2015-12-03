package net.ibaixin.chat.activity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.AlreadyLoggedInException;
import org.jivesoftware.smack.SmackException.ConnectionException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.XMPPException;
import org.json.JSONException;
import org.json.JSONObject;

import com.baidu.api.Baidu;
import com.baidu.api.BaiduDialog.BaiduDialogListener;
import com.baidu.api.BaiduDialogError;
import com.baidu.api.BaiduException;
import com.baidu.api.Util;
import com.tencent.connect.UserInfo;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.R;
import net.ibaixin.chat.model.SystemConfig;
import net.ibaixin.chat.receiver.NetworkReceiver;
import net.ibaixin.chat.service.CoreService;
import net.ibaixin.chat.task.RegistTask;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.QQUtil;
import net.ibaixin.chat.util.StreamTool;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.util.UpdateManager;
import net.ibaixin.chat.util.XmppConnectionManager;
import net.ibaixin.chat.view.ProgressDialog;
/**
 * 登录主界面
 * @author huanghui1
 *
 */
public class LoginActivity extends BaseActivity implements OnClickListener {
	
	public static Map<String,String>  CookieContiner = new HashMap<String,String>() ;
	private EditText etAccount;
	private EditText etPassword;
	private Button btnLogin;
	
	private TextView tvRegist;
	
	private SystemConfig systemConfig;
	private ProgressDialog pDialog;
	
	// 下边的是QQ/微博登录的相关变量
//	private Button sinaLogin;
	
	private Button qqLogin;
	private Button baidulogin;
	
	private Baidu baidu = null;
	//百度是否每次授权都强制登陆
	private boolean isForceLogin = false;
	private boolean isConfirmLogin = true;
	
	public Tencent mTencent;
	private UserInfo mInfo;
	/** 是否是本系统账号登录，如果是QQ、sina等账户登陆的时候该属性为false,否则为true */
	public static boolean isLocalAccountLogin = true ;
	/** 是否来自QQ注册*/
	public static boolean isQQAccountRegister = false ;
	
	private static final int UPDATESOFTVERSION = 11;
	private static final int RESULT_QQLOGIN_ERROR = 13;
	private static final int RESULT_QQLOGIN_USERINFO = 14;
	private static final int RESULT_QQLOGIN_USERAVATAR = 15;
	private static final int TASK_REGISTER = 16;//启动QQ/百度等第三方账号注册本系统
	
	private static final int TASK_BAIDU_LOGIN_USERINFO_OK = 17;//百度账号登录获取用户资料成功
	private static final int TASK_BAIDU_LOGIN_USERINFO_ERROR = 18;//百度账号登录获取用户资料失败
	
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			JSONObject json = null ;
			hideLoadingDialog(pDialog);
			switch (msg.what) {
			case UPDATESOFTVERSION:
				Intent service = new Intent(mContext, CoreService.class);
				service.putExtra(CoreService.FLAG_SYNC, CoreService.FLAG_UPDATESOFT);
				startService(service);
				break;
			case RESULT_QQLOGIN_USERINFO:
				json = (JSONObject)msg.obj;
				try {
					String username = mTencent.getOpenId() ;//137E794C1E05DF568C5F03A3E1D98AE3
					String pass = mTencent.getOpenId() ;
					String nickname = json.getString("nickname") ;
					systemConfig.setAccount(username);
					systemConfig.setPassword(pass);
					systemConfig.setNickname(nickname);
					systemConfig.setEmail("");
					isLocalAccountLogin = false ;
					new LoginTask().execute(systemConfig);
				} catch (JSONException e) {
					SystemUtil.makeShortToast(R.string.login_failed);
				}
				break;
			case RESULT_QQLOGIN_USERAVATAR:
				
				break;
			case TASK_REGISTER:
				isQQAccountRegister = true ;
				new RegistTask(mContext).execute(systemConfig);
				break;
			case RESULT_QQLOGIN_ERROR:
				SystemUtil.makeShortToast("抱歉，QQ登录出错啦");
				break;
			case TASK_BAIDU_LOGIN_USERINFO_ERROR:
				SystemUtil.makeShortToast("抱歉，百度账号登录出错啦");
				break;
			case TASK_BAIDU_LOGIN_USERINFO_OK:
				baidu.clearAccessToken();//清除Token，避免下次不提示用户输入账号而直接登录
				String data = (String) msg.obj ;
				try {
					json = new JSONObject(data);
					//{"uid":"2198532955","uname":"DDJ\u91d1\u91d1","portrait":"bd8344444ae98791e98791840e"}
					//根据这个网址即可获取用户头像 http://tb.himg.baidu.com/sys/portrait/item/{$portrait}
//					String avatar = json.getString("portrait") ;//用户头像
					String username = json.getString("uid") ;
					String pass = json.getString("uid") ;
					String nickname;
					try {
						nickname = URLDecoder.decode(json.getString("uname"), "UTF-8");
					} catch (UnsupportedEncodingException e) {
						nickname = json.getString("uname");
						Log.e(TAG, e.toString());
					}
					systemConfig.setAccount(username);
					systemConfig.setPassword(pass);
					systemConfig.setNickname(nickname);
					systemConfig.setEmail("");
					isLocalAccountLogin = false ;
					new LoginTask().execute(systemConfig);
				} catch (JSONException e) {
					SystemUtil.makeShortToast(R.string.login_failed);
				}
				break;
			default:
				break;
			}
		}
    };
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(R.string.activity_lable_login);
		
		//发送检测网络的广播
		Intent action = new Intent(NetworkReceiver.ACTION_CHECK_NETWORK);
		sendBroadcast(action);
		
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
	protected int getContentView() {
		return R.layout.activity_login;
	}
	
	@Override
	protected boolean isHomeAsUpEnabled() {
		Intent intent = getIntent();
		if (intent != null) {
			boolean isUp = intent.getBooleanExtra(ARG_DISPLAY_UP, false);
			return isUp;
		}
		return false;
	}

	@Override
	protected void initView() {
		etAccount = (EditText) findViewById(R.id.et_account);
		etPassword = (EditText) findViewById(R.id.et_password);
		btnLogin = (Button) findViewById(R.id.btn_login);
		qqLogin = (Button) findViewById(R.id.qqlogin);
		baidulogin = (Button) findViewById(R.id.baidulogin);
//		sinaLogin = (Button) findViewById(R.id.sinalogin);
		tvRegist = (TextView) findViewById(R.id.tv_regist);
	}

	@Override
	protected void initData() {
		systemConfig = application.getSystemConfig();
		
		String tAccount = systemConfig.getAccount();
		String tPassword = systemConfig.getPassword();
		if (!TextUtils.isEmpty(tAccount)) {
			etAccount.setText(tAccount);
			
			if (!TextUtils.isEmpty(tPassword)) {
				etPassword.setText(tPassword);
				btnLogin.setEnabled(true);
			} else {	//密码为空，则密码输入框获得焦点
				etPassword.requestFocus();
			}
		}
		
	}

	@Override
	protected void addListener() {
		etAccount.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub
				etPassword.setText("");
				if(TextUtils.isEmpty(s) || TextUtils.isEmpty(etPassword.getText().toString())) {
					setLoginBtnState(false);
				} else {
					setLoginBtnState(true);
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				
			}
		});
		etPassword.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub
				if(TextUtils.isEmpty(s) || TextUtils.isEmpty(etAccount.getText().toString())) {
					setLoginBtnState(false);
				} else {
					setLoginBtnState(true);
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
		etPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if(actionId == EditorInfo.IME_ACTION_DONE || actionId == KeyEvent.ACTION_DOWN) {
					String username = etAccount.getText().toString();
					String password = etPassword.getText().toString();
					if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {	//用户名和密码都不为空
						SystemUtil.hideSoftInput(v);
						v.clearFocus();
						systemConfig.setAccount(username);
						systemConfig.setPassword(password);
						
						if (application.isNetWorking()) {	//网络可用
							new LoginTask().execute(systemConfig);
						} else {	//网络不可用
							SystemUtil.makeShortToast(R.string.network_error);
						}
						return true;
					} else {
						return false;
					}
				}
				return false;
			}
		});
		
		btnLogin.setOnClickListener(this);
		baidulogin.setOnClickListener(this);
//		sinaLogin.setOnClickListener(this);
		qqLogin.setOnClickListener(this);
		tvRegist.setOnClickListener(this);
	}
	
	/*@Override
	protected void initAlimamaSDK() {
		super.initAlimamaSDK();
		ViewGroup nat = (ViewGroup) findViewById(R.id.bannerParent);
	    String slotId = "65570";
	    setupAlimama(nat, slotId);
	}*/
	/**
	 * 设置登录按钮状态，true表示可用，false表示不可用
	 * @param enable 使用、否可用
	 */
	private void setLoginBtnState(boolean enable) {
		btnLogin.setEnabled(enable);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_login:	//登录
			systemConfig.setAccount(etAccount.getText().toString());
			systemConfig.setPassword(etPassword.getText().toString());
			
			if (application.isNetWorking()) {	//网络可用
				new LoginTask().execute(systemConfig);
			} else {	//网络不可用
				SystemUtil.makeShortToast(R.string.network_error);
			}
			
//			if(!enterBackdoor(etAccount.getText().toString(), etPassword.getText().toString())) {
//				new LoginTask().execute(systemConfig);
//			}
			break;
//		case R.id.sinalogin:	//新浪微博登录
//			SystemUtil.makeShortToast("抱歉，微博登陆占时不可用");
//			break;
		case R.id.baidulogin:	//百度登录
			if(baidu==null){
				baidu = new Baidu(Constants.BAIDUAPPID, mContext);
			}
			baidu.authorize((Activity) mContext, isForceLogin,isConfirmLogin,baiduDialogListener) ;
			break;
		case R.id.qqlogin:	//qq登录
			if (mTencent == null) {
		        mTencent = Tencent.createInstance(Constants.TENCENTAPPID, this);
			}
			if (!mTencent.isSessionValid()) {
				mTencent.login(this, "all", loginListener);
				Log.d("SDKQQAgentPref", "FirstLaunch_SDK:" + SystemClock.elapsedRealtime());
			}else{
				getQQUserInfo() ;
			}
			break;
		case R.id.tv_regist:	//进入注册界面
//			Intent intent = new Intent(mContext, MainActivity.class);
			Intent intent = new Intent(mContext, RegistActivity.class);
			intent.putExtra(RegistActivity.ARG_SHOW_LOGIN, false);
			startActivity(intent, true);
			break;
		default:
			break;
		}
	}

	/**
	 * 登录的异步任务
	 * @author Administrator
	 * @update 2014年10月7日 上午9:55:00
	 *
	 */
	class LoginTask extends AsyncTask<SystemConfig, Void, Integer> {
		@Override
		protected void onPreExecute() {
			if (SystemUtil.isSoftInputActive()) {	//输入法已展开，则关闭输入法
				SystemUtil.hideSoftInput(LoginActivity.this);
			}
			pDialog = ProgressDialog.show(mContext, null, getString(R.string.logining), true);
		}

		@Override
		protected Integer doInBackground(SystemConfig... params) {
//			if(!loginIbaixinJoke()) {
//				return Constants.MSG_FAILED;
//			}
			SystemConfig sc = params[0];
			int result = login(sc);
			if (Constants.MSG_SUCCESS == result) {	//登录成功
				systemConfig.setOnline(true);
				systemConfig.setFirstLogin(false);
				//如果该账号没有对应的数据库，则根据账号创建对应的数据库，并设置当前的账号数据库
				SystemUtil.initAccountDbDir(sc.getAccount());
			} else {	//登录失败
				systemConfig.setPassword("");
			}
			if(isLocalAccountLogin) {
				application.saveSystemConfig();
			}
			return result;
		}
		

		@Override
		protected void onPostExecute(Integer result) {
			if (pDialog != null) {
				pDialog.dismiss();
			}
			switch (result) {
			case Constants.MSG_SUCCESS:	//登录成功
				Intent intent = new Intent(mContext, MainActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent, true);
				finish();
				break;
			case Constants.MSG_REQUEST_ADDRESS_FAILED:	//网络请求的地址不对
				SystemUtil.makeShortToast(R.string.request_address_failed);
				break;
			case Constants.MSG_REQUEST_ALREADY_LOGIN:	//用户已经登录过了
				SystemUtil.makeShortToast(R.string.request_address_failed);
				break;
			case Constants.MSG_NO_RESPONSE:	//服务器没有响应
				SystemUtil.makeShortToast(R.string.request_no_response);
				break;
			case Constants.MSG_FAILED:	//登录失败
				if(isLocalAccountLogin){
					SystemUtil.makeShortToast(R.string.login_failed);
				} else {
					mHandler.sendEmptyMessage(TASK_REGISTER);
				}
				break;
			default:
				SystemUtil.makeShortToast(R.string.login_failed);
				break;
			}
		}
		
		/**
		 * 登录web服务器
		 * 	add by dudejin 2015-03-05
		 * @return
		 */
		public boolean loginIbaixinJoke() {
			try {
				String urlstr = Constants.loginUrl+"?loginName="+application.getSystemConfig().getAccount()+"&loginPassword="+application.getSystemConfig().getPassword() ;
				String json = StreamTool.connectServer(urlstr);;
				if("Y".equals(json)){
//						// 取得sessionid.
//						String cookieval = conn.getHeaderField("Set-Cookie");
//						if(cookieval != null) {
//							Constants.WEB_COOKIE = cookieval/*.substring(0, cookieval.indexOf(";"))*/;
						return true ;
//						}
				}
			} catch (Exception e) {
				Log.e(TAG, e.toString());
			}
			return false;
		}
	}
	
	/**
	 * 登录
	 * @author Administrator
	 * @update 2014年10月7日 下午12:20:10
	 * @param config
	 * @return
	 */
	public int login(SystemConfig config) {
		String account = config.getAccount();
		String password = config.getPassword();
		int code = Constants.MSG_FAILED;	//登录是否成功的标识
		AbstractXMPPConnection connection = null;
		try {
			connection = XmppConnectionManager.getInstance().getConnection();
			if (!connection.isConnected()) {
				connection.connect();
			}
			connection.login(account, password, Constants.CLIENT_RESOURCE);
			ChatApplication.getInstance().setCurrentAccount(account);
//			Presence presence = new Presence(Presence.Type.available);
//			presence.setStatus("聊天中");
//			presence.setPriority(1);
//			presence.setMode(Presence.Mode.chat);
//			connection.sendPacket(presence);
			code = Constants.MSG_SUCCESS;
		} catch (SmackException e) {
			if (e instanceof ConnectionException) {	//连接地址不可用
				code = Constants.MSG_REQUEST_ADDRESS_FAILED;
			} else if (e instanceof AlreadyLoggedInException) {
				code = Constants.MSG_REQUEST_ALREADY_LOGIN;	//用户已经登录过了
			} else if (e instanceof NoResponseException) {
				code = Constants.MSG_NO_RESPONSE;	//服务器没有响应
			} else if (e instanceof SmackException.AlreadyConnectedException) {
				if (connection != null) {
					connection.disconnect();
				}
				code = Constants.MSG_FAILED;
			} else {
				code = Constants.MSG_FAILED;
			}
			Log.e(e.toString());
		} catch (IOException e) {
			Log.e(e.toString());
		} catch (XMPPException e) {
			if (connection != null) {
				connection.disconnect();
			}
			Log.e(e.toString());
		}
		return code;
	}
	
    /*@Override
    public void onBackPressed() {
        boolean interrupt = false;
        if (mController != null) {// 通知Banner推广返回键按下，如果Banner进行了一些UI切换将返回true
            // 否则返回false(如从 expand状态切换会normal状态将返回true)
            interrupt = mController.onBackPressed();
        }

        if (!interrupt)
        	application.exit();;
    }*/

	private boolean enterBackdoor(String name,String pass){
//		if("ddj".equals(name) && pass.equals("ddjasdfghjkl")){
//			Intent intent = new Intent(mContext, BackDoorActivity.class);
//			startActivity(intent);
//			return true ;
//		}
		return false ;
	}
	
	private class BaseUiListener implements IUiListener {

		@Override
		public void onComplete(Object response) {
            if (null == response) {
                SystemUtil.makeShortToast("抱歉，QQ登录失败");
                return;
            }
            JSONObject jsonResponse = (JSONObject) response;
            if (null != jsonResponse && jsonResponse.length() == 0) {
            	SystemUtil.makeShortToast("抱歉，QQ登录失败");
                return;
            }
//            SystemUtil.makeShortToast(response.toString()+"登录成功");
            // 有奖分享处理
            // handlePrizeShare();
			doComplete((JSONObject)response);
		}

		/**
		 * 子类实现此方法
		 * @param values
		 */
		protected void doComplete(JSONObject values) {}

		@Override
		public void onError(UiError e) {
//			SystemUtil.makeShortToast("onError: " + e.errorDetail);
			QQUtil.dismissDialog();
		}

		@Override
		public void onCancel() {
//			SystemUtil.makeShortToast("onCancel: ");
			QQUtil.dismissDialog();
		}
	}
	
	IUiListener loginListener = new BaseUiListener() {
        @Override
        protected void doComplete(JSONObject values) {
        	Log.d("SDKQQAgentPref", "AuthorSwitch_SDK:" + SystemClock.elapsedRealtime());
            initOpenidAndToken(values);
            getQQUserInfo();
        }
    };
	
    public void initOpenidAndToken(JSONObject jsonObject) {
        try {
            String token = jsonObject.getString(com.tencent.connect.common.Constants.PARAM_ACCESS_TOKEN);
            String expires = jsonObject.getString(com.tencent.connect.common.Constants.PARAM_EXPIRES_IN);
            String openId = jsonObject.getString(com.tencent.connect.common.Constants.PARAM_OPEN_ID);
            if (!TextUtils.isEmpty(token) && !TextUtils.isEmpty(expires)
                    && !TextUtils.isEmpty(openId)) {
                mTencent.setAccessToken(token, expires);
                mTencent.setOpenId(openId);
            }
        } catch(Exception e) {
        }
    }
    
	/**
	 * 获取QQ用户资料
	 */
	private void getQQUserInfo() {
		if (mTencent != null && mTencent.isSessionValid()) {
			IUiListener listener = new IUiListener() {

				@Override
				public void onError(UiError e) {

				}

				@Override
				public void onComplete(final Object response) {
					/**
					 * {
					 * "is_yellow_year_vip":"0",
					 * "ret":0,
					 * "figureurl_qq_1":"http:\/\/q.qlogo.cn\/qqapp\/1104396884\/137E794C1E05DF568C5F03A3E1D98AE3\/40",
					 * "figureurl_qq_2":"http:\/\/q.qlogo.cn\/qqapp\/1104396884\/137E794C1E05DF568C5F03A3E1D98AE3\/100",
					 * "nickname":"Kimdu",
					 * "yellow_vip_level":"0",
					 * "is_lost":0,
					 * "msg":"",
					 * "city":"深圳",
					 * "figureurl_1":"http:\/\/qzapp.qlogo.cn\/qzapp\/1104396884\/137E794C1E05DF568C5F03A3E1D98AE3\/50",
					 * "vip":"0",
					 * "level":"0",
					 * "figureurl_2":"http:\/\/qzapp.qlogo.cn\/qzapp\/1104396884\/137E794C1E05DF568C5F03A3E1D98AE3\/100",
					 * "province":"广东",
					 * "is_yellow_vip":"0",
					 * "gender":"男",
					 * "figureurl":"http:\/\/qzapp.qlogo.cn\/qzapp\/1104396884\/137E794C1E05DF568C5F03A3E1D98AE3\/30"
					 * }
					 */
					Message msg = mHandler.obtainMessage(RESULT_QQLOGIN_USERINFO);
					msg.obj = response;
					mHandler.sendMessage(msg);
					/*
					 //获取用户头像
					 new Thread(){
						@Override
						public void run() {
							JSONObject json = (JSONObject)response;
							if(json.has("figureurl")){
								Bitmap bitmap = null;
								try {
									bitmap = QQUtil.getbitmap(json.getString("figureurl_qq_2"));
								} catch (JSONException e) {
								}
								Message msg = new Message();
								msg.obj = bitmap;
								msg.what = RESULT_QQLOGIN_USERAVATAR;
								mHandler.sendMessage(msg);
							}
						}

					}.start();*/
				}
				@Override
				public void onCancel() {

				}
			};
			mInfo = new UserInfo(this, mTencent.getQQToken());
			mInfo.getUserInfo(listener);
		}
	}
	
	/**
	 * 百度登录监听
	 */
	BaiduDialogListener baiduDialogListener = new BaiduDialogListener() {

        @Override
        public void onComplete(Bundle values) {
        	showLoadingDialog(pDialog);
        	SystemUtil.getCachedThreadPool().execute(new Runnable() {
				@Override
				public void run() {
					baidu.init(mContext);
//		            AccessTokenManager atm = baidu.getAccessTokenManager();
//		            String accessToken = atm.getAccessToken();
		            String json = null;
		            try {
		            	json = baidu.request(Baidu.LoggedInUser_URL, null, "GET");
					} catch (Exception e) {
						mHandler.sendEmptyMessage(TASK_BAIDU_LOGIN_USERINFO_ERROR);
						Log.e(TAG, e.toString());
					}
		            if(json!=null){
		            	Message msg = mHandler.obtainMessage(TASK_BAIDU_LOGIN_USERINFO_OK);
		            	msg.obj = json ;
		            	mHandler.sendMessage(msg);
		            }
				}
			});
        }

        @Override
        public void onBaiduException(BaiduException e) {
        	mHandler.sendEmptyMessage(TASK_BAIDU_LOGIN_USERINFO_ERROR);
        	Util.logd("cancle",e.toString());
        }

        @Override
        public void onError(BaiduDialogError e) {
        	mHandler.sendEmptyMessage(TASK_BAIDU_LOGIN_USERINFO_ERROR);
        	Util.logd("cancle",e.toString());
        }

        @Override
        public void onCancel() {
               Util.logd("cancle","I am back");
        }
    };
}
