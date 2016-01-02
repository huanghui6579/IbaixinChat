package net.ibaixin.chat.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;

import com.nostra13.universalimageloader.core.download.ImageDownloader;

import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.R;
import net.ibaixin.chat.model.MsgInfo;
import net.ibaixin.chat.model.MsgPart;
import net.ibaixin.chat.receiver.NetworkReceiver;
import net.ibaixin.chat.service.CoreService;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.ImageUtil;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.MimeUtils;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.util.XmppConnectionManager;
import net.ibaixin.chat.view.ProgressDialog;

import org.jivesoftware.smack.AbstractXMPPConnection;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 分享的入口界面，主要进行权限的认证
 * @author tiger
 * @update 2015/11/14 10:06
 * @version 1.0.0
 */
public class ActionShareActivity extends BaseActivity {
    private ProgressDialog pDialog;

    private AbstractXMPPConnection mConnection;
    
    public static final String ARG_ACTION_SHARE = "arg_action_share";
    
    private final int REQ_LOGIN = 1;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MSG_CONNECTION_UNAVAILABLE:  //无网络连接
                    SystemUtil.makeShortToast(R.string.network_error);
                    break;
                case Constants.MSG_FAILED:  //登录失败
                    SystemUtil.makeShortToast(R.string.login_failed);
                    break;
                case Constants.MSG_FILE_MAX_NUM:    //文件同时分享超过指定数量，现在是9个
                    SystemUtil.makeShortToast(getString(R.string.album_file_tip_max_select, Constants.ALBUM_SELECT_SIZE));
                    finish();
                    break;
                case Constants.MSG_NO_DATA: //没有内容
                    SystemUtil.makeShortToast(R.string.chat_share_no_files);
                    break;
            }
        }
    };

    @Override
    protected int getContentView() {
        return R.layout.activity_action_share;
    }

    @Override
    protected void initView() {
        setTitle(R.string.app_name);
    }

    @Override
    protected void initData() {
        int loginState = application.getFirstLoginState();
        Intent intent = null;
        switch (loginState) {
            case ChatApplication.LOGIN_STATE_NONE:  //从未登录过，或者本地数据被清除了
            case ChatApplication.LOGIN_STATE_LOGOUT:    //用户注销了，则进入登录界面
                intent = new Intent(mContext, SplashActivity.class);
                intent.putExtra(ARG_ACTION_SHARE, true);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivityForResult(intent, REQ_LOGIN);
//                finish();
                break;
            case ChatApplication.LOGIN_STATE_LOGIN: //用户已经本地登录过了，则只需在后台登录或者不用登录了
                handleMsgSender();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQ_LOGIN: //从登录界面返回
                    afterLogin();
                    handleMsgSender();
                    break;
            }
        }
        
        super.onActivityResult(requestCode, resultCode, data);
    }
    
    /**
     * 在后台服务里处理登录后的一些数据加载工作
     * @author huanghui1
     * @update 2016/1/2 14:52
     * @version: 1.0.0
     */
    private void afterLogin() {
        Intent service = new Intent(mContext, CoreService.class);
        service.putExtra(CoreService.FLAG_INIT_CURRENT_USER, CoreService.FLAG_INIT_PERSONAL_INFO);
        service.putExtra(CoreService.FLAG_SYNC, CoreService.FLAG_SYNC_FRENDS);
        startService(service);
    }

    /**
     * 处理消息的发送
     * @author huanghui1
     * @update 2016/1/2 11:46
     * @version: 1.0.0
     */
    private void handleMsgSender() {
        pDialog = ProgressDialog.show(mContext, null, getString(R.string.loading));
        SystemUtil.getCachedThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                boolean hasNetWork = NetworkReceiver.checkNetwork(mContext);
                if (hasNetWork) {   //有网络
                    mConnection = XmppConnectionManager.getInstance().getConnection();
                    boolean hasAuthority = XmppConnectionManager.getInstance().checkAuthority(mConnection, application);
                    pDialog.dismiss();
                    if (hasAuthority) { //有权限或者登录成功
                        // Get intent, action and MIME type
                        Intent intent = getIntent();
                        String action = intent.getAction();
                        String type = intent.getType();

                        ArrayList<MsgInfo> msgInfos = new ArrayList<>();
                        switch (action) {
                            case Intent.ACTION_SEND:    //单个文件的分享
                                if (type != null) {
                                    MsgInfo msgInfo = null;
                                    if (MimeUtils.MIME_TYPE_TEXT.equals(type)) {    //普通文本
                                        String text = handleSendText(intent);
                                        if (text == null) {
                                            msgInfo = getFileMsg(handleSendFile(intent));
                                        } else {
                                            msgInfo = getTextMsg(handleSendText(intent));
                                        }
                                    } else {    //文件
                                        msgInfo = getFileMsg(handleSendFile(intent));
                                    }
                                    if (msgInfo != null) {
                                        msgInfos.add(msgInfo);
                                    }
                                } else {
                                    Log.d("------Intent.ACTION_SEND----type -- is null----");
                                }
                                break;
                            case Intent.ACTION_SEND_MULTIPLE:   //多文件分享
                                if (type != null) {
                                    List<Uri> uris = handleSendMultipleFiles(intent);
                                    if (uris != null) {
                                        if (uris.size() <= Constants.ALBUM_SELECT_SIZE) {    //最多只能一次性分享9个文件
                                            for (Uri uri : uris) {
                                                MsgInfo msgInfo = getFileMsg(uri);
                                                if (msgInfo != null) {
                                                    msgInfos.add(msgInfo);
                                                }
                                            }
                                        } else {
                                            mHandler.sendEmptyMessage(Constants.MSG_FILE_MAX_NUM);
                                            return;
                                        }
                                    }
                                } else {
                                    Log.d("------Intent.ACTION_SEND_MULTIPLE----type -- is null----");
                                }
                                break;
                        }
                        if (SystemUtil.isNotEmpty(msgInfos)) {
                            intent = new Intent(mContext, ChatChoseActivity.class);
                            intent.putExtra(ChatChoseActivity.ARG_SEND_TYPE, ChatChoseActivity.SEND_TYPE_SHARE);
                            intent.putParcelableArrayListExtra(ChatChoseActivity.ARG_MSG_INFOS, msgInfos);
                            startActivity(intent);
                            finish();
                        } else {
                            mHandler.sendEmptyMessage(Constants.MSG_NO_DATA);
                        }
                    } else {    //登录失败
                        mHandler.sendEmptyMessage(Constants.MSG_FAILED);
                    }
                } else {    //无网络
                    pDialog.dismiss();
                    mHandler.sendEmptyMessage(Constants.MSG_CONNECTION_UNAVAILABLE);
                }
            }
        });
    }

    /**
     * 处理普通文本的分享处理
     * @param  intent intent
     * @return 返回文本内容
     * @author tiger
     * @update 2015/11/14 10:44
     * @version 1.0.0
     */
    private String handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        return sharedText;
    }

    /**
     * 处理分享过来的文件
     * @param intent intent
     * @return 文件的uri
     * @author tiger
     * @update 2015/11/15 10:41
     * @version 1.0.0
     */
    private Uri handleSendFile(Intent intent) {
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        return uri;
    }

    /**
     * 处理多文件分享
     * @param intent intent
     * @return 返回多文件的uri集合
     * @author tiger
     * @update 2015/11/15 10:46
     * @version 1.0.0
     */
    private ArrayList<Uri> handleSendMultipleFiles(Intent intent) {
        ArrayList<Uri> fileUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        return fileUris;
    }
    
    /**
     * 删除普通的文本消息
     * @param text 文本内容
     * @return 返回消息对象
     * @author tiger
     * @update 2015/11/14 10:50
     * @version 1.0.0
     */
    private MsgInfo getTextMsg(String text) {
        MsgInfo msgInfo = null;
        if (text != null) {
            msgInfo = new MsgInfo();
            msgInfo.setContent(text);
            msgInfo.setMsgType(MsgInfo.Type.TEXT);
        }
        return msgInfo;
    }

    /**
     * 根据uri或者文件消息
     * @param uri 文件的uri
     * @return 返回消息对象
     * @author tiger
     * @update 2015/11/14 18:33
     * @version 1.0.0
     */
    private MsgInfo getFileMsg(Uri uri) {
        MsgInfo msgInfo = null;
        if (uri != null) {
            msgInfo = new MsgInfo();
            String filePath = SystemUtil.getFilePathFromUri(mContext, uri);
            msgInfo.setMsgType(SystemUtil.getMsgInfoType(filePath));
            if (filePath != null) {
                File file = new File(filePath);
                if (file.exists()) {
                    MsgPart msgPart = new MsgPart();
                    msgPart.setFileName(file.getName());
                    msgPart.setFilePath(filePath);
                    msgPart.setSize(file.length());
                    msgPart.setMimeType(MimeUtils.guessMimeTypeFromFilename(msgPart.getFileName()));
                    if (msgInfo.getMsgType() == MsgInfo.Type.IMAGE) {   //图片，则需要压缩了再发送
                        Bitmap loadedImage = ImageUtil.loadImageThumbnailsSync(ImageDownloader.Scheme.FILE.wrap(filePath));
                        if (loadedImage != null) {
                            SystemUtil.saveBitmap(loadedImage, filePath);//保存压缩过的图片的到本地磁盘
                        }
                    }
                    msgInfo.setMsgPart(msgPart);
                }
            }
        }
        return msgInfo;
    }

    @Override
    protected void addListener() {

    }
}
