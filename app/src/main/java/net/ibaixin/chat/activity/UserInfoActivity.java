package net.ibaixin.chat.activity;

import org.jivesoftware.smack.SmackException.NotConnectedException;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.download.ImageDownloader.Scheme;
import com.nostra13.universalimageloader.utils.MemoryCacheUtils;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.R;
import net.ibaixin.chat.manager.UserManager;
import net.ibaixin.chat.manager.web.UserEngine;
import net.ibaixin.chat.model.NewFriendInfo;
import net.ibaixin.chat.model.NewFriendInfo.FriendStatus;
import net.ibaixin.chat.model.Personal;
import net.ibaixin.chat.model.User;
import net.ibaixin.chat.model.UserVcard;
import net.ibaixin.chat.model.web.VcardDto;
import net.ibaixin.chat.provider.Provider;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.Observable;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.util.XmppConnectionManager;
import net.ibaixin.chat.util.XmppUtil;
import net.ibaixin.chat.view.ProgressDialog;

/**
 * 好友详情界面
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年10月10日 下午8:16:59
 */
public class UserInfoActivity extends BaseActivity {
	public static final String ARG_USER = "arg_user";
	public static final String ARG_OPTION = "arg_option";
	public static final String ARG_USER_TYPE = "arg_user_type";
	
	public static final int TYPE_SELF = 1;
	public static final int TYPE_FRIEND = 2;
	public static final int TYPE_STRANGER = 3;
	
	/**
	 * 搜索好友进入的该界面
	 */
	public static final int OPTION_SEARCH = 1;
	/**
	 * 查看好友详情进入的该界面
	 */
	public static final int OPTION_LOAD = 2;

	private ImageView ivHeadIcon;
	private TextView tvUsername;
	private TextView tvMarkname;
	private TextView tvNickname;
	private TextView tvRealname;
	private TextView tvEmail;
	private TextView tvAddress;
	private TextView tvMobile;
	private TextView tvSignature;
	private Button btnOpt;
	
	private View layoutAddress;
	private View layoutMobile;
	private View layoutSignature;
	
	/**
	 * 被查看的人的类型，有"自己"、"本地好友"、"陌生人"，默认是陌生人
	 */
	private int userType = TYPE_STRANGER;
	
	ProgressDialog pDialog;
	
	private User user;
	
	private UserManager userManager = UserManager.getInstance();
	
	private ImageLoader mImageLoader = ImageLoader.getInstance();
	private DisplayImageOptions options = SystemUtil.getGeneralImageOptions();
	
	private VcardContentObserver mContentObserver;
	
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Constants.MSG_SUCCESS:	//好友信息加载成功
				User result = (User) msg.obj;
				if (result != null) {
					refresUI(result);
				}
				break;
			case Constants.MSG_SHOW_USR_ICON:	//显示好友头像
				ivHeadIcon.setImageBitmap((Bitmap) msg.obj);
				break;
			case Constants.MSG_CONNECTION_UNAVAILABLE:	//客户端与服务器没有连接
				hideLoadingDialog(pDialog);
				new AlertDialog.Builder(mContext)
					.setTitle(R.string.prompt)
					.setMessage(R.string.connection_unavailable)
					.setNegativeButton(android.R.string.cancel, null)
					.setCancelable(false)
					.setPositiveButton(android.R.string.ok, null).show();
				break;
			case Constants.MSG_SEND_ADD_FRIEND_REQUEST:	//发送添加好友的请求
				hideLoadingDialog(pDialog);
				SystemUtil.makeShortToast(R.string.contact_send_add_friend_request_success);
				btnOpt.setEnabled(false);
				break;
			default:
				break;
			}
		}
	};
	

	@Override
	protected int getContentView() {
		return R.layout.activity_user_info;
	}

	@Override
	protected void initWidow() {
//		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
	}

	@Override
	protected void initView() {
		ivHeadIcon = (ImageView) findViewById(R.id.iv_head_icon);
		tvUsername = (TextView) findViewById(R.id.tv_username);
		tvMarkname = (TextView) findViewById(R.id.tv_markname);
		tvNickname = (TextView) findViewById(R.id.tv_nickname);
		tvRealname = (TextView) findViewById(R.id.tv_realname);
		tvEmail = (TextView) findViewById(R.id.tv_email);
		tvAddress = (TextView) findViewById(R.id.tv_address);
		tvMobile = (TextView) findViewById(R.id.tv_mobile);
		tvSignature = (TextView) findViewById(R.id.tv_signature);
		btnOpt = (Button) findViewById(R.id.btn_opt);
		layoutAddress = findViewById(R.id.layout_address);
		layoutMobile = findViewById(R.id.layout_mobile);
		layoutSignature = findViewById(R.id.layout_signature);
	}
	
	/**
	 * 注册电子名片信息的监听器
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年10月4日 下午4:48:02
	 */
	private void registVcardContentObserver() {
		if (mContentObserver == null) {
			mContentObserver = new VcardContentObserver(mHandler);
		}
		userManager.addObserver(mContentObserver);
	}

	@Override
	protected void initData() {
		registVcardContentObserver();
		//显示进度条
//		setProgressBarIndeterminateVisibility(true);
		user = getIntent().getParcelableExtra(ARG_USER);
		if (user != null) {
			String username = user.getUsername();
			
			showMarkname(user);
			
			tvUsername.setText(getString(R.string.username, username));
			showNickanme(user);
			
			showEmail(user);
			
			int opt = getIntent().getIntExtra(ARG_OPTION, 0);
			switch (opt) {
			case OPTION_LOAD:	//查看好友详情
				showLocalFriendVcard(user);
				break;
			case OPTION_SEARCH:	//搜索好友的详情
				userType = getIntent().getIntExtra(ARG_USER_TYPE, TYPE_STRANGER);
				switch (userType) {
				case TYPE_STRANGER:	//陌生人
					loadFriendInfo(user);
//					new LoadVcardTask().execute(user.getJID());
					break;
				case TYPE_FRIEND:	//好友
					showLocalFriendVcard(user);
					break;
				case TYPE_SELF:	//自己
					showSelfVcard(ChatApplication.getInstance().getCurrentUser());
					break;
				default:
					break;
				}
				break;
			default:
				break;
			}
		}
	}

	@Override
	protected void onDestroy() {
		//注销电子名片的监听器
		if (mContentObserver != null) {
			userManager.removeObserver(mContentObserver);
		}
		super.onDestroy();
	}
	
	/**
	 * 显示本地好友的信息
	 * @update 2014年10月24日 下午5:06:34
	 * @param user
	 */
	private void showLocalFriendVcard(User user) {
		userType = TYPE_FRIEND;
		btnOpt.setText(R.string.contact_send_msg);
		UserVcard uCard = user.getUserVcard();
		if (uCard != null) {
			showIcon(uCard);
			/*Bitmap icon = SystemUtil.getImageFromLocal(uCard.getIconPath());
			if (icon != null) {
				ivHeadIcon.setImageBitmap(icon);
			}*/
			new LoadFriendInfoTask().execute(user);
		}
	}
	
	/**
	 * 显示个人信息
	 * @update 2014年10月24日 下午5:27:11
	 * @param personal
	 */
	private void showSelfVcard(Personal personal) {
		btnOpt.setText(R.string.contact_modify);
		String province = personal.getProvince() == null ? "" : personal.getProvince();
		String city = personal.getCity() == null ? "" : personal.getCity();
		String address =  province + " " + city;
		tvAddress.setText(address);
		
		String phone = personal.getPhone() == null ? "" : personal.getPhone();
		tvMobile.setText(phone);
		
		String realname = personal.getRealName();
		if (!TextUtils.isEmpty(realname)) {
			tvRealname.setVisibility(View.VISIBLE);
			tvRealname.setText(getString(R.string.realname, realname));
		} else {
			tvRealname.setVisibility(View.GONE);
		}
		String iconPath = personal.getIconShowPath();
		String iconUri = null;
		if (SystemUtil.isFileExists(iconPath)) {
			iconUri = Scheme.FILE.wrap(iconPath);
		}
		mImageLoader.displayImage(iconUri, ivHeadIcon, options);
	}
	
	@Override
	protected void addListener() {
		btnOpt.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				switch (userType) {
				case TYPE_STRANGER:	//不是本地好友，则发送添加好友的请求
					if (pDialog == null) {
						pDialog = ProgressDialog.show(mContext, null, getString(R.string.loading));
					} else {
						pDialog.show();
					}
					mHandler.post(new Runnable() {
						
						@Override
						public void run() {
							Message msg = mHandler.obtainMessage();
							try {
								XmppUtil.addFriend(XmppConnectionManager.getInstance().getConnection(), user.getJID());
								
								//添加新的好友信息到本地数据库
								NewFriendInfo newInfo = new NewFriendInfo();
								String from = ChatApplication.getInstance().getCurrentAccount();
								String to = SystemUtil.unwrapJid(user.getJID());
								newInfo.setFriendStatus(FriendStatus.VERIFYING);
								newInfo.setFrom(from);
								newInfo.setTo(to);
								newInfo.setTitle(from);
								newInfo.setContent(getString(R.string.contact_friend_add_target_request));
								newInfo.setCreationDate(System.currentTimeMillis());
								newInfo.setUser(user);
								UserVcard uCard = user.getUserVcard();
								if (uCard != null) {
									String iconPath = uCard.getIconPath();
									if (!TextUtils.isEmpty(iconPath)) {
										newInfo.setIconPath(iconPath);
										newInfo.setIconHash(uCard.getIconHash());
									}
								}
								userManager.addNewFriendInfo(newInfo);
								msg.what = Constants.MSG_SEND_ADD_FRIEND_REQUEST;
							} catch (NotConnectedException e) {
								e.printStackTrace();
								msg.what = Constants.MSG_CONNECTION_UNAVAILABLE;
							}
							mHandler.sendMessage(msg);
						}
					});
					break;
				case TYPE_FRIEND:	//是本地好友，则发送消息
					Intent intent = new Intent(mContext, ChatActivity.class);
					User arg = new User();
					arg.setId(user.getId());
					arg.setUsername(user.getUsername());
					arg.setNickname(user.getNickname());
					arg.setResource(user.getResource());
					arg.setUserVcard(user.getUserVcard());
					intent.putExtra(ARG_USER, arg);
					startActivity(intent);
					break;
				case TYPE_SELF:	//自己
					SystemUtil.makeShortToast("编辑个人信息");
					break;
				default:
					break;
				}
			}
		});
	}
	
	/**
	 * 从web上加载好友的信息 
	 * @param user
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年10月3日 下午4:14:48
	 */
	private void loadFriendInfo(User user) {
		//下载电子名片信息
		UserEngine userEngine = new UserEngine(mContext);
		userEngine.getVcardInfo(user.getUsername(), new UserEngine.VcardResponseListenter() {
			
			@Override
			public void onVcardResponseSuccess(VcardDto vcardDto) {
				Message msg = mHandler.obtainMessage();
				msg.what = Constants.MSG_SUCCESS;
				msg.obj = vcardDto;
				mHandler.sendMessage(msg);
			}
		});
	}
	
	/**
	 * 加载好友电子名片
	 * @author huanghui1
	 * @update 2014年10月10日 下午10:25:58
	 */
	class LoadFriendInfoTask extends AsyncTask<User, Void, User> {

		@Override
		protected User doInBackground(User... params) {
//			UserVcard uCard = userManager.getUserVcardById(params[0].getId());
//			return uCard;
			if (params != null && params.length > 0) {
				User user = params[0];
				User tUser = userManager.getUserDetailById(user.getId());
				return tUser;
			} else {
				//
				//TODO 加载用户电子名片信息
	//			User card = XmppUtil.syncUserVcard(XmppConnectionManager.getInstance().getConnection(), params[0]);
	//			return card;
				return null;
			}
			
		}
		
		@Override
		protected void onPostExecute(User result) {
			if (result != null) {
				/*user.setUserVcard(result);
				String province = result.getProvince() == null ? "" : result.getProvince();
				String city = result.getCity() == null ? "" : result.getCity();
				String address =  province + " " + city;
				tvAddress.setText(address);
				
				String phone = result.getMobile() == null ? "" : result.getMobile();
				tvMobile.setText(phone);
				
				String realname = result.getRealName();
				if (!TextUtils.isEmpty(realname)) {
					tvRealname.setVisibility(View.VISIBLE);
					tvRealname.setText(getString(R.string.realname, realname));
				} else {
					tvRealname.setVisibility(View.GONE);
				}*/
				
//				user = result;
				refresUI(result);
				user = result;
				
				loadFriendInfo(result);
//				Intent intent = new Intent(LoadDataBroadcastReceiver.ACTION_USER_INFOS);
//				sendBroadcast(intent);
			}
//			if (result != null) {
//				tvAddress.setText(result.getAddressFieldHome("REGION") + " " + result.getAddressFieldHome("LOCALITY"));	//省市
//				tvNickname.setText(getString(R.string.nickname, result.getNickName()));
//			}
//			setProgressBarIndeterminateVisibility(false);
		}
		
	}
	
	/**
	 * 更新好友详情的界面
	 * @param user
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年10月4日 下午3:59:26
	 */
	private void refresUI(User result) {
		UserVcard userVcard = result.getUserVcard();
		UserVcard oldVcard = user.getUserVcard();
		if (oldVcard == null) {
			oldVcard = new UserVcard();
		}
		if (userVcard != null) {
			String oldMarkname = user.getName();
			String markname = result.getName();
			
			boolean refreshMarkname = false;
			int oldSex = oldVcard.getSex();
			int sex = userVcard.getSex();
			if (oldSex != sex) {
				refreshMarkname = true;
			}
			if (!oldMarkname.equals(markname) || refreshMarkname) {	//备注或者性别不同，则需刷新显示
				showMarkname(result);
			}
			
			String oldNickname = oldVcard.getNickname();
			String nickName = userVcard.getNickname();
			boolean refreshNickname = false;
			if(!TextUtils.isEmpty(oldNickname)) {
				if (!oldNickname.equals(nickName)) {
					refreshNickname = true;
				}
			} else {
				if (!TextUtils.isEmpty(nickName)) {	//新查出来的昵称不为空
					refreshNickname = true;
				}
			}
			if (refreshNickname) {
				showNickanme(result);
			}
			
			String oldEmail = user.getEmail();
			String email = result.getEmail();
			boolean refreshEmail = false;
			if (!TextUtils.isEmpty(oldEmail)) {
				if (!oldEmail.equals(email)) {
					refreshEmail = true;
				}
			} else {
				if (!TextUtils.isEmpty(email)) {
					refreshEmail = true;
				}
			}
			if (refreshEmail) {
				showEmail(result);
			}
			
			String oldAddress = user.getShowAddress();
			String address = result.getShowAddress();
			boolean refreshAddres = false;
			if (!TextUtils.isEmpty(oldAddress)) {
				if (!oldAddress.equals(address)) {
					refreshAddres = true;
				}
			} else {
				if (!TextUtils.isEmpty(address)) {
					refreshAddres = true;
				}
			}
			if (refreshAddres) {
				showAddress(result);
			}
			
			String oldPhone = user.getPhone();
			String phone = result.getPhone();
			boolean refreshPhone = false;
			if (!TextUtils.isEmpty(oldPhone)) {
				if (!oldPhone.equals(phone)) {
					refreshPhone = true;
				}
			} else {
				if (!TextUtils.isEmpty(phone)) {
					refreshPhone = true;
				}
			}
			if (refreshPhone) {
				showPhone(result);
			}
			
			String oldSignature = oldVcard.getDesc();
			String signature = userVcard.getDesc();
			boolean refreshSignature = false;
			if (!TextUtils.isEmpty(oldSignature)) {
				if (oldSignature.equals(signature)) {
					refreshSignature = true;
				}
			} else {
				if (!TextUtils.isEmpty(signature)) {
					refreshSignature = true;
				}
			}
			if (refreshSignature) {
				showSignature(userVcard);
			}
				
			String oldRealname = oldVcard.getRealName();
			String realname = userVcard.getRealName();
			boolean refreshRealname = false;
			if (!TextUtils.isEmpty(oldRealname)) {
				if (!oldRealname.equals(realname)) {
					refreshRealname = true;
				}
			} else {
				if (!TextUtils.isEmpty(realname)) {
					refreshRealname = true;
				}
			}
			if (refreshRealname) {
				showRealName(userVcard);
			}
			
			String oldHash = oldVcard.getIconHash();
			String hash = userVcard.getIconHash();
			boolean refreshIcon = false;
			if (!TextUtils.isEmpty(oldHash)) {
				if (!oldHash.equals(hash)) {
					refreshIcon = true;
				}
			} else {
				if (!TextUtils.isEmpty(hash)) {
					refreshIcon = true;
				}
			}
			if (refreshIcon) {
				showIcon(userVcard);
			}
			
		}
	}
	
	/**
	 * 显示地理位置
	 * @param user
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年10月4日 下午2:56:55
	 */
	private void showAddress(User user) {
		String address = user.getShowAddress();
		if (!TextUtils.isEmpty(address)) {
			layoutAddress.setVisibility(View.VISIBLE);
			tvAddress.setText(address);
		} else {
			layoutAddress.setVisibility(View.GONE);
		}
	}
	
	/**
	 * 显示电话号码
	 * @param vcard
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年10月4日 下午3:00:02
	 */
	private void showPhone(User user) {
		String phone = user.getPhone();
		if (!TextUtils.isEmpty(phone)) {
			layoutMobile.setVisibility(View.VISIBLE);
			tvMobile.setText(phone);
		} else {
			layoutMobile.setVisibility(View.GONE);
		}
	}
	
	/**
	 * 显示个性签名
	 * @param vcard
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年10月4日 下午3:01:14
	 */
	private void showSignature(UserVcard vcard) {
		String signature = vcard.getDesc();
		if (!TextUtils.isEmpty(signature)) {
			layoutSignature.setVisibility(View.VISIBLE);
			tvSignature.setText(signature);
		} else {
			layoutSignature.setVisibility(View.GONE);
		}
	}
	
	/**
	 * 显示真实姓名
	 * @param vcard
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年10月4日 下午3:02:02
	 */
	private void showRealName(UserVcard vcard) {
		String realname = vcard.getRealName();
		if (!TextUtils.isEmpty(realname)) {
			tvRealname.setVisibility(View.VISIBLE);
			tvRealname.setText(getString(R.string.realname, realname));
		} else {
			tvRealname.setVisibility(View.GONE);
		}
	}
	
	/**
	 * 显示头像
	 * @param vcard
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年10月4日 下午3:03:05
	 */
	private void showIcon(UserVcard vcard) {
		String iconPath = vcard.getIconShowPath();
		String iconUri =  null;
		if(SystemUtil.isFileExists(iconPath)){
			iconUri = Scheme.FILE.wrap(iconPath);
		}
		if (iconUri != null) {
			//从缓存中删除该图像的内存缓存
			MemoryCacheUtils.removeFromCache(iconUri, mImageLoader.getMemoryCache());
			mImageLoader.displayImage(iconUri, ivHeadIcon, options);
		}
	}
	
	/**
	 * 显示备注名称
	 * @param user
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年10月4日 下午3:07:59
	 */
	private void showMarkname(User user) {
		String markname = user.getName();
		UserVcard vcard = user.getUserVcard();
		
		tvMarkname.setText(markname);
		if (vcard != null) {
			int sex = vcard.getSex();
			int sexRes = 0;
			if (sex > 0) {
				switch (sex) {
				case 1:	//男
					sexRes = R.drawable.ic_man;
					break;
				case 2:	//女
					sexRes = R.drawable.ic_woman;
				default:
					break;
				}
				tvMarkname.setCompoundDrawablesWithIntrinsicBounds(0, 0, sexRes, 0);
			}
		}
	}
	
	/**
	 * 显示昵称 
	 * @param user
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年10月4日 下午3:10:08
	 */
	private void showNickanme(User user) {
		String nickname = user.getVcardNickname();
		if (!TextUtils.isEmpty(nickname)) {
			tvNickname.setVisibility(View.VISIBLE);
			tvNickname.setText(getString(R.string.nickname, user.getNickname()));
		} else {
			tvNickname.setVisibility(View.GONE);
		}
	}
	
	/**
	 * 显示电子邮箱
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年10月4日 下午3:10:51
	 */
	private void showEmail(User user) {
		String email = user.getEmail();
		if (!TextUtils.isEmpty(email)) {
			tvEmail.setVisibility(View.VISIBLE);
			tvEmail.setText(getString(R.string.email, email));
		} else {
			tvEmail.setVisibility(View.GONE);
		}
	}
	
	/**
	 * 电子名片信息的数据库监听器
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年10月4日 下午4:31:10
	 */
	private class VcardContentObserver extends net.ibaixin.chat.util.ContentObserver {

		public VcardContentObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void update(Observable<?> observable, int notifyFlag, NotifyType notifyType, Object data) {
			Log.d("----VcardContentObserver---update---------notifyFlag------" + notifyFlag + "---notifyType----" + notifyType + "---data---" + data);
			switch (notifyFlag) {
			case Provider.UserVcardColumns.NOTIFY_FLAG:	//好友电子名片信息的通知
				switch (notifyType) {
				case ADD:
				case UPDATE:
					if (data != null) {
						UserVcard vcard = (UserVcard) data;
						int userId = vcard.getUserId();
						User result = userManager.getUserDetailById(userId);
						if (result != null) {
							Message msg = mHandler.obtainMessage();
							msg.what = Constants.MSG_SUCCESS;
							msg.obj = result;
							mHandler.sendMessage(msg);
						}
					}
					break;

				default:
					break;
				}
				break;

			default:
				break;
			}
		}
		
	}
}
