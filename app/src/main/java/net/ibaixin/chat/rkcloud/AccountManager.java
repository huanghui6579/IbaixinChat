package net.ibaixin.chat.rkcloud;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import net.ibaixin.chat.rkcloud.http.HttpCallback;
import net.ibaixin.chat.rkcloud.http.HttpFatalExceptionCallBack;
import net.ibaixin.chat.rkcloud.http.HttpResponseCode;
import net.ibaixin.chat.rkcloud.http.HttpTools;
import net.ibaixin.chat.rkcloud.http.HttpType;
import net.ibaixin.chat.rkcloud.http.Progress;
import net.ibaixin.chat.rkcloud.http.Request;
import net.ibaixin.chat.rkcloud.http.Result;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.StreamTool;

public class AccountManager implements HttpFatalExceptionCallBack {
    private static final int ACCOUNT_EXCEPTION_WHAT = 0;

    private static AccountManager mInstance = null;
    private Handler mUiHandler;
    private Handler mHandler;
    /** 融科SDK应该执行注册还是登录，0：登录 ， 1：注册 **/
    public static int mNeedRegistRkCloud = 0;
    /** 用户是否已经登录 **/
    private boolean mAccountIsLogin = false;

    /**
     * 用户对应的融科音视频通话账户
     */
    private static Map<String,String> usersRkAccountMap = new HashMap<String,String>();
    /**
     * 融科音视频通话账户对应的百信用户
     */
    private static Map<String,String> rkAccountUserMap = new HashMap<String,String>();

    public boolean ismAccountIsLogin() {
        return mAccountIsLogin;
    }

    public void setmAccountIsLogin(boolean mAccountIsLogin) {
        this.mAccountIsLogin = mAccountIsLogin;
    }

    public static Map<String, String> getUsersRkAccountMap() {
        return usersRkAccountMap;
    }

    public static void setUsersRkAccountMap(Map<String, String> usersRkAccountMap) {
        AccountManager.usersRkAccountMap.clear();
        AccountManager.usersRkAccountMap.putAll(usersRkAccountMap);
    }

    public static Map<String, String> getRkAccountUserMap() {
        return rkAccountUserMap;
    }

    public static void setRkAccountUserMap(Map<String, String> rkAccountUserMap) {
        AccountManager.rkAccountUserMap.clear();
        AccountManager.rkAccountUserMap.putAll(rkAccountUserMap);
    }

    private AccountManager() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (ACCOUNT_EXCEPTION_WHAT == msg.what) {
                    // Intent intent = new Intent(RKCloudDemo.context,
                    // ReminderActivity.class);
                    // if (1 == msg.arg1) {
                    // intent.setAction(ReminderBroadcast.ACTION_REMIND_KICKED_USER);
                    // } else if (2 == msg.arg1) {
                    // intent.setAction(ReminderBroadcast.ACTION_REMIND_BANNED_USER);
                    // }
                    // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    // RKCloudDemo.context.startActivity(intent);
                }
            }
        };
    }

    public static AccountManager getInstance() {
        if (null == mInstance) {
            mInstance = new AccountManager();
        }
        return mInstance;
    }

    public void bindUiHandler(Handler handler) {
        mUiHandler = handler;
    }

    @Override
    public void onHttpFatalException(int errorCode) {
        Message msg = mHandler.obtainMessage();
        msg.what = ACCOUNT_EXCEPTION_WHAT;
        msg.arg1 = errorCode;
        msg.sendToTarget();
    }

    /**
     * @function getCurrentAccount 获取当前帐户
     * @param
     */
    public Account getCurrentAccount() {
        String name = RKCloudDemo.config.getString(Config.LOGIN_NAME, null);
        String pwd = RKCloudDemo.config.getString(Config.LOGIN_PWD, null);
        String session = RKCloudDemo.config.getString(Config.LOGIN_ACCOUNT_SESSION, null);
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(pwd) || TextUtils.isEmpty(session)) {
            return null;
        }

        // 获取当前注册用户信息
        Account account = new Account();
        account.loginName = RKCloudDemo.config.getString(Config.LOGIN_NAME, null);
        account.loginPwd = RKCloudDemo.config.getString(Config.LOGIN_PWD, null);
        account.session = RKCloudDemo.config.getString(Config.LOGIN_ACCOUNT_SESSION, null);
        return account;
    }

    /**
     * 获取session
     */
    public String getSession() {
        return RKCloudDemo.config.getString(Config.LOGIN_ACCOUNT_SESSION, "");
    }

    /**
     * 获取当前登录账号
     */
    public String getAccount() {
        return RKCloudDemo.config.getString(Config.LOGIN_NAME, "");
    }

    /**
     * 退出时需要删除的信息
     */
    public void logout() {
        mAccountIsLogin = false;
        // 清除相关SP信息
        RKCloudDemo.config.remove(Config.LOGIN_NAME);
        RKCloudDemo.config.remove(Config.LOGIN_PWD);
        RKCloudDemo.config.remove(Config.LOGIN_RKCLOUD_PWD);
        RKCloudDemo.config.remove(Config.LOGIN_ACCOUNT_SESSION);
        // RKCloudDemo.config.remove(Config.GET_CONTACT_GROUP_KEY);
        // RKCloudDemo.config.remove(Config.GET_CONTACT_FRIEND_KEY);

        // 退出之后，一定要清理已经初始化的SDK内容
        SDKManager.getInstance().logout();
    }

    ///////////////////////// 与api有关的交互 begin//////////////////////////////////
    /**
     * 注册
     * @param name用户名
     * @param pwd密码
     * @param type用户类型
     */
    public void register(final String name, final String pwd) {
        Request request = new Request(HttpType.REGISTER, RKCloudDemo.kit.getHttpHost(HttpTools.HTTPHOST_TYPE_ROOT),
                HttpTools.REGISTER_URL);
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("account", name);
        params.put("pwd", pwd);
        params.put("type", "1");
        request.params = params;
        request.mHttpCallback = new HttpCallback() {
            @Override
            public void onThreadResponse(Result result) {
                if (null != mUiHandler) {
                    Message msg = mUiHandler.obtainMessage();
                    msg.what = AccountUiMessage.RESPONSE_REGISTER;
                    msg.arg1 = result.opCode;
                    msg.sendToTarget();
                }
            }

            @Override
            public void onThreadProgress(Progress progress) {
            }

        };
        RKCloudDemo.kit.execute(request);
    }

    /**
     * 登录
     * @param name 用户名
     * @param pwd 密码
     */
    public void login(String account, final String pwd) {

        final String lowAccount = TextUtils.isEmpty(account) ? "" : account.toLowerCase();
        Request request = new Request(HttpType.lOGIN, RKCloudDemo.kit.getHttpHost(HttpTools.HTTPHOST_TYPE_ROOT),HttpTools.lOGIN_URL);
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("account", lowAccount);
        params.put("pwd", pwd);
        request.params = params;
        request.mHttpCallback = new HttpCallback() {
            @Override
            public void onThreadResponse(Result result) {
                if (HttpResponseCode.OK == result.opCode) {
                    try {
                        JSONObject jsonObj = new JSONObject(result.values.get("result"));
                        String type = jsonObj.getString("type");
                        String rkcloudPwd = jsonObj.getString("sdk_pwd");

                        RKCloudDemo.config.put(Config.LOGIN_ACCOUNT_SESSION, jsonObj.getString("ss"));
                        RKCloudDemo.config.put(Config.LOGIN_NAME, lowAccount);
                        RKCloudDemo.config.put(Config.LOGIN_PWD, pwd);
                        RKCloudDemo.config.put(Config.LOGIN_RKCLOUD_PWD, rkcloudPwd);
                        RKCloudDemo.config.put(Config.LOGIN_USER_TYPE, type);
                        RKCloudDemo.config.put(Config.LOGIN_ADD_FRIEND_PERMISSION, jsonObj.getInt("permission"));

                        // RKCloudDemo.config.put(Config.ACCOUNT_HAS_NOT_EXIST,
                        // false);
                        //
                        // ContentValues cv = new ContentValues();
                        // cv.put(PersonInfoColumns.USER_TYPE, type);
                        // cv.put(PersonInfoColumns.ACCOUNT,
                        // RKCloudDemo.config.getString(Config.LOGIN_NAME, ""));
                        // cv.put(PersonInfoColumns.NAME,
                        // jsonObj.getString("name"));
                        // cv.put(PersonInfoColumns.ADDRESS,
                        // jsonObj.getString("address"));
                        // cv.put(PersonInfoColumns.MOBILE,
                        // jsonObj.getString("mobile"));
                        // cv.put(PersonInfoColumns.EMAIL,
                        // jsonObj.getString("email"));
                        // cv.put(PersonInfoColumns.SEX,
                        // jsonObj.getString("sex"));
                        // int infoVersion = jsonObj.getInt("info_version");
                        // int avatarVersion = jsonObj.getInt("avatar_version");
                        // cv.put(PersonInfoColumns.INFO_SERVER_VERSION,
                        // infoVersion);
                        // cv.put(PersonInfoColumns.INFO_CLIENT_VERSION,
                        // infoVersion);
                        // cv.put(PersonInfoColumns.INFO_SYNC_LASTTIME,
                        // System.currentTimeMillis());
                        // cv.put(PersonInfoColumns.AVATAR_SERVER_VERSION,
                        // avatarVersion);
                        //
                        // ContactInfo info =
                        // PersonalManager.getInstance().getContactInfo(lowAccount);
                        // if (null == info) {
                        // PersonalManager.getInstance().insertContactInfo(cv);
                        // } else {
                        // PersonalManager.getInstance().updateContactInfo(lowAccount,
                        // cv);
                        // }
                        // // 如果有头像则更新
                        // if (null == info) {
                        // if (avatarVersion > 0) {
                        // PersonalManager.getInstance().getAvatarThumb(lowAccount,
                        // avatarVersion);
                        // }
                        // } else {
                        // if (avatarVersion > 0 && avatarVersion >
                        // info.mAvatarClientThumbVersion) {
                        // PersonalManager.getInstance().getAvatarThumb(lowAccount,
                        // avatarVersion);
                        // }
                        // }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    // 若上次登录的用户不是当前登录用户，清除主界面tab项配置信息
                    // if (!RKCloudDemo.config.getString(Config.LAST_LOGIN_NAME,
                    // "").equalsIgnoreCase(lowAccount)) {
                    // RKCloudDemo.config.remove(Config.LAST_SELECTED_TAB);
                    // }
                    // // 记录最后一个登录的用户信息
                    // RKCloudDemo.config.put(Config.LAST_LOGIN_NAME,
                    // lowAccount);
                    // // 同步好友信息
                    // ContactManager.getInstance().syncGroupInfos();
                }
                if (null != mUiHandler) {
                    Message msg = mUiHandler.obtainMessage();
                    msg.what = AccountUiMessage.RESPONSE_LOGIN;
                    msg.arg1 = result.opCode;
                    msg.sendToTarget();
                }
            }

            @Override
            public void onThreadProgress(Progress progress) {
            }
        };
        RKCloudDemo.kit.execute(request);
    }
    ///////////////////////// 与api有关的交互 end//////////////////////////////////


    public boolean updateIbaixinRkCloudAccount(String loginName,String loginPassword,String rkCloudAccount){
        try {//http://www.ibaixin.net/ibaixin/user/editRkCloudAccount?loginName=kim&loginPassword=123456&rkCloudAccount=8BN751452598888899
            String urlstr = Constants.registerRkCloudUrl+"?loginName="+loginName+"&loginPassword="+loginPassword+"&rkCloudAccount="+rkCloudAccount ;
            String json = StreamTool.connectServer(urlstr);
            Log.d("AccountManager","updateIbaixinRkCloudAccount:"+json);
            JSONObject jsonObject = new JSONObject(json);
            int code = jsonObject.getInt("code");
            if(code==0){
//						// 取得sessionid.
//						String cookieval = conn.getHeaderField("Set-Cookie");
//						if(cookieval != null) {
//							Constants.WEB_COOKIE = cookieval/*.substring(0, cookieval.indexOf(";"))*/;
						return true ;
            }
//				}
        } catch (Exception e) {
            Log.e("AccountManager", e.toString());
        }
        return false;
    }
}