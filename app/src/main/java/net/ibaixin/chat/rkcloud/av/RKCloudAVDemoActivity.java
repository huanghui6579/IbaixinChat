package net.ibaixin.chat.rkcloud.av;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

import com.rongkecloud.av.RKCloudAVCallInfo;
import com.rongkecloud.av.RKCloudAVCallState;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.R;
import net.ibaixin.chat.manager.UserManager;
import net.ibaixin.chat.model.User;
import net.ibaixin.chat.rkcloud.AccountManager;

public class RKCloudAVDemoActivity extends Activity implements OnClickListener, SensorEventListener {
    private static final String TAG = RKCloudAVDemoActivity.class.getSimpleName();

    public static final String INTENT_KEY_OUTCALL_ACCOUNT = "intent_key_outcall_account";// 外呼时的账号
    public static final String INTENT_KEY_INCALL_ACCOUNT = "intent_key_incall_account";// 呼入时的账号
    public static final String INTENT_KEY_SHOWNAME = "intent_key_showname";//显示的名称
    public static String mShowName = null;//显示的名称

    private static final int DELAY_EXECUTE_DIMSCREEN_TIME = 1000;// 1s

    // UI组件
    private View mRootView;// 根布局
    private View mCallBgView;

    private RelativeLayout mUserInfoLayout;
    private ImageView mHeaderImage;// 头像
    private TextView mShowNameTV;// 名称
    private TextView mCallStatusTV;// 通话状态

    private RelativeLayout mRemoteVideoLayout;

    private TextView mTimer;// 记时器

    private ImageView mHideInAudioBtn, mHideInVideoBtn;// 隐藏按钮
    private LinearLayout mAllBtnsLayout;// 按钮区域
    private LinearLayout mCallingBtnsLayout;// 通话中时的按钮区域
    // 静音按钮
    private LinearLayout mMuteLayout;
    private ImageView mMuteBtn;
    // 免提按钮
    private LinearLayout mHandFreeLayout;
    private ImageView mHandFreeBtn;
    // 切换摄像头
    private LinearLayout mSwitchCameraLayout;
    private ImageView mSwitchCameraBtn;
    // 切为语音聊天
    private LinearLayout mToAudioLayout;
    private ImageView mToAudioBtn;
    // 挂断按钮
    private LinearLayout mHangupLayout;
    private ImageView mHangupBtn;
    private TextView mHangupTV;
    // 接听按钮
    private LinearLayout mAnswerLayout;
    private ImageView mAnswerBtn;

    // 成员变量
    private RKCloudAVContact mContactObj;
    private RKCloudAVDemoManager mAVManager;
    private boolean mClickRootView = false;// 是否点击了根目录 true:视频聊天时隐藏部分组件

    private Handler mUiHandler;
    private AudioManager mAudioManager;
    // 距离感应相关的内容
    private SensorManager mSensorManager;
    private Sensor mSensor = null;
    private float mDistance;
    private boolean mIsSensorTimerRunning = false;// 距离感应使用的定时器是否在运行

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.rkcloud_av_call);
        initUiAndListeners();
        // 初始化变量内容
        mAVManager = RKCloudAVDemoManager.getInstance(this);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mUiHandler = new UiHandler(this);
        mAVManager.setCallUiShowVideoLayout(mRemoteVideoLayout);
        RKCloudAVCallInfo avCallInfo = mAVManager.getAVCallInfo();
        if (null == avCallInfo) {
            mAVManager.hideOutCallNotification();
            mAVManager.hideInCallNotification();
            finish();
            return;
        }
        // intent解析
        resolveIntent(getIntent());
        // 组件初始化
        showWidgets();
        // 如果是在通话中则更新通话时长
        if (RKCloudAVCallState.AV_CALL_STATE_ANSWER == mAVManager.getAVCallInfo().callState) {
            updateCallTime();
        } else {
            // 点亮屏幕
            RKCloudAVScreenUtils.getInstance(this).screenOn();
            // 设置音量键调节的音量类型
            setVolumeControlStream(AudioManager.STREAM_RING);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        resolveIntent(intent);
    };

    @Override
    protected void onResume() {
        super.onResume();
        mAVManager.bindUiHandler(mUiHandler);
        // dudejin
        // RKCloudAVContactManager.getInstance(this).bindUiHandler(mUiHandler);
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI);
        mAVManager.setCallUiIsShow(true);
    };

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        lightScreen();
    };

    @Override
    protected void onStop() {
        super.onStop();
        mIsSensorTimerRunning = false;
        mAVManager.setCallUiIsShow(false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.layout_callcontent:// 点击根布局时的操作，只有视频模式有此操作
            // 无通话或是非视频聊天时返回
            RKCloudAVCallInfo avCallInfo = mAVManager.getAVCallInfo();
            if (null == avCallInfo || RKCloudAVCallState.AV_CALL_STATE_ANSWER != avCallInfo.callState
                    || !avCallInfo.isCurrVideoOpen) {
                return;
            }
            if (mClickRootView) {
                mHideInVideoBtn.setVisibility(View.VISIBLE);
                mAllBtnsLayout.setVisibility(View.VISIBLE);
                mTimer.setVisibility(View.VISIBLE);

                mClickRootView = false;

            } else {
                mHideInVideoBtn.setVisibility(View.GONE);
                mAllBtnsLayout.setVisibility(View.GONE);
                mTimer.setVisibility(View.GONE);

                mClickRootView = true;
            }
            break;

        case R.id.hideui_invideo:
        case R.id.hideui_inaudio:
            onBackPressed();
            break;

        case R.id.mute:
            boolean mute = mMuteBtn.isSelected();
            mAVManager.mute(!mute);
            mMuteBtn.setSelected(!mute);
            break;

        case R.id.handfree:
            boolean handfree = mHandFreeBtn.isSelected();
            mAVManager.handFree(!handfree);
            mHandFreeBtn.setSelected(!handfree);
            break;

        case R.id.switchcamera:
            boolean cameraStatus = mSwitchCameraBtn.isSelected();
            mAVManager.switchCamera(!cameraStatus);
            mSwitchCameraBtn.setSelected(!cameraStatus);
            break;

        case R.id.toaudio:
            mAVManager.switchToAudioCall();
            showWidgets();
            break;

        case R.id.hangup:
            mAVManager.hangup();
            break;

        case R.id.answer:
            mAVManager.answer();
            break;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        mDistance = event.values[0];
        if (mDistance < mSensor.getMaximumRange()) {
            if (!mIsSensorTimerRunning) {
                mUiHandler.sendEmptyMessageDelayed(RKCloudAVUiHandlerMessage.HANDLER_MSG_WHAT_SENSOR,
                        DELAY_EXECUTE_DIMSCREEN_TIME);
                mIsSensorTimerRunning = true;
            }

        } else {
            lightScreen();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        RKCloudAVCallInfo avCallInfo = mAVManager.getAVCallInfo();
        int callState = null != avCallInfo ? avCallInfo.callState : RKCloudAVCallState.AV_CALL_STATE_IDLE;
        if (KeyEvent.KEYCODE_VOLUME_UP == keyCode) {// 音量键上调
            int streamType = AudioManager.STREAM_VOICE_CALL;
            if (RKCloudAVCallState.AV_CALL_STATE_RINGBACK == callState
                    || RKCloudAVCallState.AV_CALL_STATE_RINGIN == callState) {
                streamType = AudioManager.STREAM_RING;
            }
            mAudioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
            return true;

        } else if (KeyEvent.KEYCODE_VOLUME_DOWN == keyCode) {// 音量键下调
            int streamType = AudioManager.STREAM_VOICE_CALL;
            if (RKCloudAVCallState.AV_CALL_STATE_RINGBACK == callState
                    || RKCloudAVCallState.AV_CALL_STATE_RINGIN == callState) {
                streamType = AudioManager.STREAM_RING;
            }
            mAudioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
            return true;

        } else if (KeyEvent.KEYCODE_BACK == keyCode) {
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void lightScreen() {
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mCallBgView.setVisibility(View.VISIBLE);
        mCallBgView.setClickable(true);
        mRootView.setBackgroundColor(Color.TRANSPARENT);
        getWindow().setAttributes(attrs);
    }

    private void dimScreen() {
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        mCallBgView.setVisibility(View.INVISIBLE);
        mCallBgView.setClickable(false);
        mRootView.setBackgroundColor(Color.BLACK);
        getWindow().setAttributes(attrs);
    }

    private void initUiAndListeners() {
        mRootView = findViewById(R.id.layout_callroot);
        mCallBgView = findViewById(R.id.layout_callcontent);

        mUserInfoLayout = (RelativeLayout) findViewById(R.id.layout_userinfo);
        mHeaderImage = (ImageView) findViewById(R.id.headerimage);
        mShowNameTV = (TextView) findViewById(R.id.username);
        mCallStatusTV = (TextView) findViewById(R.id.callstatus);

        mRemoteVideoLayout = (RelativeLayout) findViewById(R.id.remotevideo);

        mTimer = (TextView) findViewById(R.id.timer);

        mHideInAudioBtn = (ImageView) findViewById(R.id.hideui_inaudio);
        mHideInVideoBtn = (ImageView) findViewById(R.id.hideui_invideo);

        mAllBtnsLayout = (LinearLayout) findViewById(R.id.btnzone);
        mCallingBtnsLayout = (LinearLayout) findViewById(R.id.btnzone_calling);

        mMuteLayout = (LinearLayout) findViewById(R.id.layout_mute);
        mMuteBtn = (ImageView) findViewById(R.id.mute);
        mHandFreeLayout = (LinearLayout) findViewById(R.id.layout_handfree);
        mHandFreeBtn = (ImageView) findViewById(R.id.handfree);
        mSwitchCameraLayout = (LinearLayout) findViewById(R.id.layout_switchcamera);
        mSwitchCameraBtn = (ImageView) findViewById(R.id.switchcamera);
        mToAudioLayout = (LinearLayout) findViewById(R.id.layout_toaudio);
        mToAudioBtn = (ImageView) findViewById(R.id.toaudio);
        mHangupLayout = (LinearLayout) findViewById(R.id.layout_hangup);
        mHangupBtn = (ImageView) findViewById(R.id.hangup);
        mHangupTV = (TextView) findViewById(R.id.hangup_text);
        mAnswerLayout = (LinearLayout) findViewById(R.id.layout_answer);
        mAnswerBtn = (ImageView) findViewById(R.id.answer);

        mCallBgView.setOnClickListener(this);
        mHideInAudioBtn.setOnClickListener(this);
        mHideInVideoBtn.setOnClickListener(this);
        mMuteBtn.setOnClickListener(this);
        mHandFreeBtn.setOnClickListener(this);
        mSwitchCameraBtn.setOnClickListener(this);
        mToAudioBtn.setOnClickListener(this);
        mHangupBtn.setOnClickListener(this);
        mAnswerBtn.setOnClickListener(this);
    }

    private void resolveIntent(Intent intent) {
        // 获取对端号码
        mShowName = intent.getStringExtra(INTENT_KEY_SHOWNAME);
        String outAccount = intent.getStringExtra(INTENT_KEY_OUTCALL_ACCOUNT);
        String inAccount = intent.getStringExtra(INTENT_KEY_INCALL_ACCOUNT);
        RKCloudAVCallInfo avCallInfo = mAVManager.getAVCallInfo();
        // 两个号码全为空或是两个号码都不空时返回并结束
        if ((TextUtils.isEmpty(outAccount) && TextUtils.isEmpty(inAccount))
                || (!TextUtils.isEmpty(outAccount) && !TextUtils.isEmpty(inAccount)) || null == avCallInfo
                || (RKCloudAVCallState.AV_CALL_STATE_IDLE == avCallInfo.callState
                        || RKCloudAVCallState.AV_CALL_STATE_PREPARING == avCallInfo.callState)) {
            mAVManager.hideOutCallNotification();
            mAVManager.hideInCallNotification();
            finish();
            return;
        }
        // 与当前正在通话的类型或号码不匹配时结束
        if (!TextUtils.isEmpty(inAccount)) {
            if (!inAccount.equalsIgnoreCase(avCallInfo.peerAccount) || avCallInfo.isCaller) {
                mAVManager.hideInCallNotification();
                finish();
                return;
            }
        } else if (!TextUtils.isEmpty(outAccount)) {
            if (!outAccount.equalsIgnoreCase(avCallInfo.peerAccount) || !avCallInfo.isCaller) {
                mAVManager.hideOutCallNotification();
                finish();
                return;
            }
        }

        // 用户信息显示
        showUserInfo(avCallInfo.peerAccount);
    }

    /*
     * 显示对端用户信息
     */
    private void showUserInfo(String account) {
        // dudejin
        // mContactObj = RKCloudAVContactManager.getContactInfo(account);
//        mShowNameTV.setText(null != mContactObj ? mContactObj.showName : account);
        if(TextUtils.isEmpty(mShowName)) {
            Map<String,String> map = AccountManager.getRkAccountUserMap();
            mShowName = map.get(account);
            User u = UserManager.getInstance().getUserByUsername(mShowName.toLowerCase());
            if(u!=null) {
                String name = u.getNickname();
                if (!TextUtils.isEmpty(name)) {
                    mShowName = name;
                }
            }
        }
        mShowNameTV.setText(!TextUtils.isEmpty(mShowName) ? mShowName : account);
        if (null != mContactObj && !TextUtils.isEmpty(mContactObj.thumbPath)
                && new File(mContactObj.thumbPath).exists()) {
            mHeaderImage.setImageBitmap(BitmapFactory.decodeFile(mContactObj.thumbPath));
        } else {
            mHeaderImage.setImageResource(R.drawable.rkcloud_av_img_header_default);
        }
    }

    private void showWidgets() {
        RKCloudAVCallInfo avCallInfo = mAVManager.getAVCallInfo();
        switch (avCallInfo.callState) {
        case RKCloudAVCallState.AV_CALL_STATE_RINGIN:
        case RKCloudAVCallState.AV_CALL_STATE_RINGBACK:
            mUserInfoLayout.setVisibility(View.VISIBLE);
            mCallStatusTV.setVisibility(View.VISIBLE);
            if (RKCloudAVCallState.AV_CALL_STATE_RINGBACK == avCallInfo.callState) {
                mCallStatusTV.setText(R.string.rkcloud_av_tip_caller_connect);
            } else {
                if (avCallInfo.isVideoCall) {
                    mCallStatusTV.setText(R.string.rkcloud_av_tip_callee_invitevideo);
                } else {
                    mCallStatusTV.setText(R.string.rkcloud_av_tip_callee_inviteaudio);
                }
            }

            mTimer.setVisibility(View.GONE);
            mRemoteVideoLayout.setVisibility(View.GONE);

            mHideInAudioBtn.setVisibility(View.GONE);
            mHideInVideoBtn.setVisibility(View.GONE);

            mCallingBtnsLayout.setVisibility(View.GONE);
            mMuteLayout.setVisibility(View.GONE);
            mHandFreeLayout.setVisibility(View.GONE);
            mSwitchCameraLayout.setVisibility(View.GONE);

            mToAudioLayout.setVisibility(View.GONE);
            mHangupLayout.setVisibility(View.VISIBLE);
            if (RKCloudAVCallState.AV_CALL_STATE_RINGIN == avCallInfo.callState) {
                mAnswerLayout.setVisibility(View.VISIBLE);
            } else {
                mAnswerLayout.setVisibility(View.GONE);
                mHangupTV.setText(R.string.rkcloud_av_btn_cancel);
            }

            break;

        case RKCloudAVCallState.AV_CALL_STATE_ANSWER:
            if (!avCallInfo.isCurrVideoOpen) {
                mUserInfoLayout.setVisibility(View.VISIBLE);
                mCallStatusTV.setVisibility(View.GONE);

                mRemoteVideoLayout.setVisibility(View.GONE);

                mHideInAudioBtn.setVisibility(View.VISIBLE);
                mHideInVideoBtn.setVisibility(View.GONE);

                mSwitchCameraLayout.setVisibility(View.GONE);
                mToAudioLayout.setVisibility(View.GONE);

            } else {
                mUserInfoLayout.setVisibility(View.GONE);
                mCallStatusTV.setVisibility(View.GONE);

                mRemoteVideoLayout.setVisibility(View.VISIBLE);

                mHideInAudioBtn.setVisibility(View.GONE);
                mHideInVideoBtn.setVisibility(View.VISIBLE);

                if (mAVManager.checkCameraHardware() && mAVManager.enableSwitchCamera()) {
                    mSwitchCameraLayout.setVisibility(View.VISIBLE);
                    mSwitchCameraBtn.setSelected(mAVManager.getSwitchCameraStatus());
                } else {
                    mSwitchCameraLayout.setVisibility(View.GONE);
                }

                mToAudioLayout.setVisibility(View.VISIBLE);
            }

            mTimer.setVisibility(View.VISIBLE);

            mCallingBtnsLayout.setVisibility(View.VISIBLE);
            mMuteLayout.setVisibility(View.VISIBLE);
            mMuteBtn.setSelected(mAVManager.getMuteStatus());
            mHandFreeLayout.setVisibility(View.VISIBLE);
            mHandFreeBtn.setSelected(mAVManager.getHandFreeStatus());
            mAnswerLayout.setVisibility(View.GONE);
            mHangupLayout.setVisibility(View.VISIBLE);
            mHangupTV.setText(R.string.rkcloud_av_btn_hangup);
            break;

        default:
            mUserInfoLayout.setVisibility(View.VISIBLE);
            mCallStatusTV.setVisibility(View.GONE);
            mRemoteVideoLayout.setVisibility(View.GONE);
            mTimer.setVisibility(View.GONE);
            mHideInAudioBtn.setVisibility(View.GONE);
            mHideInVideoBtn.setVisibility(View.GONE);
            mCallingBtnsLayout.setVisibility(View.GONE);
            mMuteLayout.setVisibility(View.VISIBLE);
            mHandFreeLayout.setVisibility(View.VISIBLE);
            mSwitchCameraLayout.setVisibility(View.GONE);
            mToAudioLayout.setVisibility(View.GONE);
            mAnswerLayout.setVisibility(View.GONE);
            mHangupLayout.setVisibility(View.VISIBLE);
            mHangupTV.setText(R.string.rkcloud_av_btn_hangup);
            break;
        }
    }

    /*
     * 更新通话时长
     */
    private void updateCallTime() {
        RKCloudAVCallInfo avCallInfo = mAVManager.getAVCallInfo();
        if (null != avCallInfo) {
            int duration = (int) Math.ceil((System.currentTimeMillis() - avCallInfo.callAnswerTime) / 1000);
            mTimer.setText(RKCloudAVUtils.secondConvertToTime(duration));
            mUiHandler.sendEmptyMessageDelayed(RKCloudAVUiHandlerMessage.HANDLER_MSG_WHAT_UPDATETIME, 1000);
        }
    }

    private void processResult(Message msg) {
        if (RKCloudAVUiHandlerMessage.HANDLER_MSG_WHAT_SENSOR == msg.what) {
            RKCloudAVCallInfo avCallInfo = mAVManager.getAVCallInfo();
            if (mDistance < mSensor.getMaximumRange()) {
                if (!avCallInfo.isCurrVideoOpen) {
                    // 只有语音通话时才起作用
                    dimScreen();
                }
            }
            mIsSensorTimerRunning = false;

        } else if (RKCloudAVUiHandlerMessage.HANDLER_MSG_WHAT_UPDATETIME == msg.what) {
            updateCallTime();

        } else if (RKCloudAVUiHandlerMessage.HANDLER_MSG_WHAT_AV == msg.what) {
            int state = msg.arg1;
            switch (state) {
            case RKCloudAVCallState.AV_CALL_STATE_RINGBACK:
                mCallStatusTV.setText(R.string.rkcloud_av_tip_caller_wait);
                break;

            case RKCloudAVCallState.AV_CALL_STATE_ANSWER:
                updateCallTime();
                showWidgets();
                // 设置音量键调节的音量类型
                setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
                if (mAVManager.getAVCallInfo().isCurrVideoOpen) {
                    // 视频通话时默认为免提
                    mAVManager.handFree(true);
                    mHandFreeBtn.setSelected(true);
                }
                break;

            case RKCloudAVCallState.AV_CALL_STATE_VIDEO_INIT:
                break;

            case RKCloudAVCallState.AV_CALL_STATE_VIDEO_STOP:
            case RKCloudAVCallState.AV_CALL_STATE_VIDEO_START:
                mAllBtnsLayout.setVisibility(View.VISIBLE);
                mClickRootView = false;
                showWidgets();
                if (RKCloudAVCallState.AV_CALL_STATE_VIDEO_STOP == state) {
                    mAVManager.showToastText(getString(R.string.rkcloud_av_tip_remote_toaudio));
                } else {
                    mAVManager.showToastText(getString(R.string.rkcloud_av_tip_remote_tovideo));
                }
                break;

            case RKCloudAVCallState.AV_CALL_STATE_HANGUP:
                finish();
                break;
            }
        } else if (RKCloudAVUiHandlerMessage.MSG_WHAT_MEETING_CONTACTSINFO_CHANGED == msg.what) { // 联系人信息有变更
            List<String> accounts = (List<String>) msg.obj;
            if (null != accounts && accounts.size() > 0) {
                RKCloudAVCallInfo avCallInfo = mAVManager.getAVCallInfo();
                if (null != avCallInfo && accounts.contains(avCallInfo.peerAccount)) {
                    showUserInfo(avCallInfo.peerAccount);
                }
            }

        } else if (RKCloudAVUiHandlerMessage.MSG_WHAT_MEETING_CONTACT_HEADERIMAGE_CHANGED == msg.what) { // 联系人头像有变更
            String account = (String) msg.obj;
            RKCloudAVCallInfo avCallInfo = mAVManager.getAVCallInfo();
            if (null != avCallInfo && account.equalsIgnoreCase(avCallInfo.peerAccount)) {
                showUserInfo(avCallInfo.peerAccount);
            }
        }
    }

    private class UiHandler extends Handler {
        private WeakReference<Activity> mContext;

        public UiHandler(Activity context) {
            mContext = new WeakReference<Activity>(context);
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (null == mContext || null == mContext.get()) {
                return;
            }
            processResult(msg);
        }
    }
}
