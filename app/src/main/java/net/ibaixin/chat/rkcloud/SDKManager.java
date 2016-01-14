package net.ibaixin.chat.rkcloud;

import com.rongkecloud.av.RKCloudAV;
import com.rongkecloud.sdkbase.RKCloud;
import com.rongkecloud.sdkbase.RKCloudBaseErrorCode;
import com.rongkecloud.sdkbase.RKCloudModelType;
import com.rongkecloud.sdkbase.interfaces.InitCallBack;
import com.rongkecloud.sdkbase.interfaces.RKCloudFatalExceptionCallBack;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.model.SystemConfig;
import net.ibaixin.chat.rkcloud.av.RKCloudAVDemoManager;
import net.ibaixin.chat.rkcloud.http.HttpFatalExceptionCallBack;

public class SDKManager implements RKCloudFatalExceptionCallBack, HttpFatalExceptionCallBack {
    private static final String TAG = SDKManager.class.getSimpleName();
    private static final int SDK_INIT_WHAT = 88;
    private static final int ACCOUNT_EXCEPTION_WHAT = 99;

    private static SDKManager mInstance = null;
    private Handler mHandler;
    private Handler mUiHandler;

    private static boolean sdkInitProcess = false;// SDK初始化是否正在进行，true:正在进行
                                                  // false:已完成
    private static boolean sdkInitStatus = false;// SDK初始化结果，true:初始化成功
                                                 // false:初始化失败

    public static boolean isSdkInitStatus() {
        return sdkInitStatus;
    }

    private SDKManager() {
        sdkInitProcess = false;
        sdkInitStatus = false;
        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                case SDK_INIT_WHAT:
                    if (0 == msg.arg1) {
                        Log.d(TAG, "initSDK success.");
                        sdkSuccessDo();
                    } else {
                        Log.e(TAG, "initSDK failed, code=" + msg.arg1);
                    }
                    if (null != mUiHandler) {
                        Message initMsg = mUiHandler.obtainMessage();
                        initMsg.what = AccountUiMessage.SDK_INIT_FINISHED;
                        initMsg.arg1 = msg.arg1;
                        initMsg.sendToTarget();
                    }
                    break;

                case ACCOUNT_EXCEPTION_WHAT:
                    sdkInitProcess = false;
                    sdkInitStatus = false;
                    Log.e(TAG,"ACCOUNT_EXCEPTION_WHAT");
                    /*Intent intent = new Intent(RKCloudDemo.context, ReminderActivity.class);
                    if(1 == msg.arg1){
                        intent.setAction(ReminderBroadcast.ACTION_REMIND_KICKED_USER);
                    }else if(2 == msg.arg1){
                        intent.setAction(ReminderBroadcast.ACTION_REMIND_BANNED_USER);
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    RKCloudDemo.context.startActivity(intent);*/
                    break;
                }
            }
        };
    }

    public static SDKManager getInstance() {
        if (null == mInstance) {
            mInstance = new SDKManager();
        }
        return mInstance;
    }

    public void bindUiHandler(Handler handler) {
        mUiHandler = handler;
    }

    /**
     * 初始化云视互动sdk
     * 
     * @param rkcloudAccount
     *            用户名
     * @param rkcloudPwd
     *            密码
     */
    public boolean initSDK() {
        if (!sdkInitProcess && sdkInitStatus) {
            return true;
        }
        Log.d(TAG, "initSDK--begin");
        // update by dudejin
         String rkcloudAccount = RKCloudDemo.config.getString(Config.LOGIN_NAME, null);
         String rkcloudPwd = RKCloudDemo.config.getString(Config.LOGIN_RKCLOUD_PWD, null);

        if (!TextUtils.isEmpty(rkcloudAccount)) {
            // 设置Debug模式为打开状态
            RKCloud.setDebugMode(true);
            // 设置云视互动加载即时通信SDK
            // RKCloud.use(RKCloudModelType.CHAT);
            // 设置云视互动加载音视频sdk
            RKCloud.use(RKCloudModelType.AV);
            // 设置云视互动加载多人语音
            // RKCloud.use(RKCloudModelType.MULTIVOICE);

            sdkInitProcess = true;

            // 云视互动SDK初始化
            RKCloud.init(RKCloudDemo.context, rkcloudAccount, rkcloudPwd, new InitCallBack() {
                @Override
                public void onSuccess() {
                    sdkInitProcess = false;
                    sdkInitStatus = true;
                    Message msg = mHandler.obtainMessage();
                    msg.what = SDK_INIT_WHAT;
                    msg.arg1 = 0;
                    msg.sendToTarget();
                }

                @Override
                public void onFail(int failCode) {
                    sdkInitProcess = false;

                    if (RKCloudBaseErrorCode.RK_NOT_NETWORK == failCode
                            || RKCloudBaseErrorCode.RK_SUCCESS == failCode) {
                        sdkInitStatus = true;
                    }

                    Message msg = mHandler.obtainMessage();
                    msg.what = SDK_INIT_WHAT;
                    msg.arg1 = failCode;
                    msg.sendToTarget();
                }
            });
        }
        return false;
    }

    /**
     * 初始化sdk成功要处理的内容
     */
    private void sdkSuccessDo() {
        // 设置SDK账号异常的回调处理
        RKCloud.setOnRKCloudFatalExceptionCallBack(this);
        // 设置推送消息的回调处理
        // RKCloud.setOnRKCloudReceivedUserDefinedMsgCallBack(MessageManager.getInstance());
        // 云视互动即时通信SDK的初始化
        // RKCloudChat.init();
        // 绑定消息回调、群变更的通知
        // RKCloudChatMessageManager manager =
        // RKCloudChatMessageManager.getInstance(RKCloudDemo.context);
        // manager.registerRKCloudChatReceivedMsgCallBack(RKCloudChatMmsManager.getInstance(RKCloudDemo.context));
        // manager.registerRKCloudChatGroupCallBack(RKCloudChatMmsManager.getInstance(RKCloudDemo.context));
        // 与应用端的联系人接口进行绑定
        // manager.registerRKCloudContactCallBack(RKCloudChatContactManager.getInstance(RKCloudDemo.context));

        // 音视频互动初始化
        RKCloudAV.init(RKCloudAVDemoManager.getInstance(RKCloudDemo.context));
        // 多人语音初始化
        // RKCloudMeeting.init(RKCloudMeetingDemoManager.getInstance(RKCloudDemo.context));
    }

    @Override
    public void onRKCloudFatalException(int errorCode) {
        Message msg = mHandler.obtainMessage();
        msg.what = ACCOUNT_EXCEPTION_WHAT;
        msg.arg1 = errorCode;
        msg.sendToTarget();
    }

    @Override
    public void onHttpFatalException(int errorCode) {
        Message msg = mHandler.obtainMessage();
        msg.what = ACCOUNT_EXCEPTION_WHAT;
        msg.arg1 = errorCode;
        msg.sendToTarget();
    }

    public void logout() {
        // 退出之后，一定要清理已经初始化的SDK内容
        sdkInitProcess = false;
        sdkInitStatus = false;
        // 清除所有图片缓存
        // RKCloudChatImageAsyncLoader.getInstance(RKCloudDemo.context).removeAllImages();
        // // 结束所有的UI
        // RKCloudChatLogoutManager.getInstance(RKCloudDemo.context).logout();
        // // 音视频退出时的操作
        RKCloudAVDemoManager.getInstance(RKCloudDemo.context).logout();
        // RKCloudMeetingDemoManager.getInstance(RKCloudDemo.context).logout();
        // RKCloudChat.unInit();
        RKCloudAV.unInit();
        // RKCloudMeeting.unInit();
        RKCloud.unInit();
    }
}
