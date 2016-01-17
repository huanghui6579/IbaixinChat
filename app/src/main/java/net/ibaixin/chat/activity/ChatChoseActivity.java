package net.ibaixin.chat.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.download.ImageDownloader;

import net.ibaixin.chat.R;
import net.ibaixin.chat.loader.ChatChoseLoader;
import net.ibaixin.chat.manager.MsgManager;
import net.ibaixin.chat.model.ChatChoseItem;
import net.ibaixin.chat.model.MsgInfo;
import net.ibaixin.chat.model.MsgPart;
import net.ibaixin.chat.model.MsgSenderInfo;
import net.ibaixin.chat.model.MsgThread;
import net.ibaixin.chat.model.ShowInfo;
import net.ibaixin.chat.model.User;
import net.ibaixin.chat.model.UserVcard;
import net.ibaixin.chat.service.CoreService;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.ImageUtil;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.util.XmppUtil;
import net.ibaixin.chat.view.ProgressDialog;

import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天选择联系人界面
 * 创建人：huanghui1
 * 创建时间： 2015/11/9 18:03
 * 修改人：huanghui1
 * 修改时间：2015/11/9 18:03
 * 修改备注：
 * @version: 0.0.1
 */
public class ChatChoseActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<List<ChatChoseItem>> {
    public static final String ARG_MSG_INFOS= "arg_msg_infos";
    public static final String ARG_FORWARD_FLAG = "arg_forward_flag";
    
    public static final String ARG_SEND_OPT = "arg_send_opt";

    public static final String ARG_SEND_TYPE = "arg_send_type";

    //跳转到chat界面，一般用于聊天界面的转发
    public static final int OPT_FORWARD = 0;
    //直接销毁该界面
    public static final int OPT_FINISH = 1;
    //让用户选择，一般用于外界分享
    public static final int OPT_CHOOSE = 2;

    /**
     * 转发类型
     */
    public static final int SEND_TYPE_FORWARD = 0;
    /**
     * 分享类型
     */
    public static final int SEND_TYPE_SHARE = 1;
    
    private ListView mLvData;
    private View mEmptyView;
    private View mFooteryView;
    private List<ChatChoseItem> mChoseItems;
    
    private ChatChoseAdapter mChoseAdapter;
    
    private ImageLoader mImageLoader = ImageLoader.getInstance();

    private DisplayImageOptions options = SystemUtil.getGeneralImageOptions();

    /**
     * 传入的消息实体
     */
    private ArrayList<MsgInfo> mMsgInfos;

    /**
     * 默认是转发类型
     */
    private int mSendType = SEND_TYPE_FORWARD;

    /**
     * 加载数据的类型，默认是会话
     */
    private int mLoadType = ChatChoseItem.DATA_THREAD;

    private ProgressDialog pDialog;

    /**
     * 默认消息发送成功后跳转到聊天界面
     */
    private int mSendOpt = OPT_FORWARD;

    private CoreService mCoreService;

    private Chat mChat;

    private ChatManager mChatManager;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MSG_SUCCESS:
                    switch (mSendOpt) {
                        case OPT_FORWARD:   //跳转到chat界面
                            MsgThread msgThread = (MsgThread) msg.obj;
                            Intent intent = new Intent(mContext, ChatActivity.class);
                            intent.putExtra(ARG_FORWARD_FLAG, true);
                            intent.putExtra(ChatActivity.ARG_THREAD, msgThread);
                            intent.putParcelableArrayListExtra(ARG_MSG_INFOS, mMsgInfos);
                            startActivity(intent);
                            finish();
                            break;
                        case OPT_FINISH:
                            SystemUtil.makeShortToast(R.string.chat_forward_msg_success);
                            finish();
                            break;
                        case OPT_CHOOSE:    //让用户自己选择
                            SystemUtil.makeShortToast(R.string.chat_forward_msg_success);
                            handleSendOk();
                            break;
                    }
                    break;
                case Constants.MSG_FAILED:
                    SystemUtil.makeShortToast(R.string.chat_forward_msg_error);
                    break;
            }
        }
    };
    
    /**
     * 处理消息发送成功后的界面跳转
     * @author tiger
     * @update 2016/1/16 15:09
     * @version 1.0.0
     */
    private void handleSendOk() {
        View view = getLayoutInflater().inflate(R.layout.layout_send_ok_dialog, null);
        MaterialDialog.Builder builder = new MaterialDialog.Builder(mContext);
        builder.customView(view, false)
                .positiveText(R.string.share_send_opt_finish)
                .negativeText(R.string.share_send_opt_remain)
                .forceStacking(true)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        finish();
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        Intent intent = new Intent(mContext, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    }
                })
                .show();
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            CoreService.MainBinder mBinder = (CoreService.MainBinder) service;
            mCoreService = mBinder.getService();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // TODO Auto-generated method stub

        }

    };
    
    @Override
    protected int getContentView() {
        return R.layout.activity_chat_chose;
    }

    @Override
    protected void initView() {
        mLvData = (ListView) findViewById(R.id.lv_data);
    }

    @Override
    protected void initData() {
        mChoseItems = new ArrayList<>();

        Intent service = new Intent(mContext, CoreService.class);
        bindService(service, mServiceConnection, Context.BIND_AUTO_CREATE);

        getSupportLoaderManager().initLoader(0, null, ChatChoseActivity.this);

        Intent intent = getIntent();
        if (intent != null) {
            mMsgInfos = intent.getParcelableArrayListExtra(ARG_MSG_INFOS);
            mSendType = intent.getIntExtra(ARG_SEND_TYPE, SEND_TYPE_FORWARD);
            mSendOpt = intent.getIntExtra(ARG_SEND_OPT, OPT_FORWARD);
        }
        
    }

    @Override
    protected void addListener() {
        mLvData.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                final ChatChoseItem choseItem = mChoseItems.get(position);
                MaterialDialog.Builder builder = new MaterialDialog.Builder(mContext);
                View contentView = LayoutInflater.from(mContext).inflate(R.layout.item_chat_chose, null); 
                ImageView ivIcon = (ImageView) contentView.findViewById(R.id.iv_head_icon);
                TextView ivName = (TextView) contentView.findViewById(R.id.tv_name);
                ShowInfo showInfo = choseItem.getChoseItemInfo();
                if (showInfo != null) {
                    ivName.setText(showInfo.getName());
                    Drawable icon = showInfo.getIcon();
                    if (icon != null) {
                        ivIcon.setImageDrawable(icon);
                    } else {
                        String imageUri = null;
                        String iconPath = showInfo.getIconPath();
                        if (!TextUtils.isEmpty(iconPath)) {
                            imageUri = ImageDownloader.Scheme.FILE.wrap(iconPath);
                        }
                        mImageLoader.displayImage(imageUri, ivIcon, options);
                    }
                    builder.title(R.string.chat_forward_msg_prompt)
                            .customView(contentView, true)
                            .positiveText(android.R.string.ok)
                            .negativeText(android.R.string.cancel)
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    pDialog = ProgressDialog.show(mContext, null, getString(R.string.loading));
                                    SystemUtil.getCachedThreadPool().execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (choseItem.isItemType()) {
                                                String fromUser = application.getCurrentUser().getFullJID();
                                                String toUser = null;
                                                MsgManager msgManager = MsgManager.getInstance();
                                                MsgThread msgThread = null;
                                                if (choseItem.getDataType() == ChatChoseItem.DATA_CONTACT) { //点击的是好友
                                                    User user = choseItem.getUser();
                                                    if (user != null) {
                                                        //1、获取会话，如果没有就创建
                                                        String username = user.getUsername();
                                                        toUser = user.getFullJid();
                                                        msgThread = msgManager.getThreadByMembers(true, username);
                                                    } else {
                                                        Log.w("---onItemClick---choseItem---" + choseItem + "--user--is null--");
                                                    }
                                                } else {    //点击的是会话
                                                    msgThread = choseItem.getMsgThread();
                                                }
                                                if (msgThread != null) {
                                                    toUser = msgThread.getMembers().get(0).getFullJid();
                                                    if (SystemUtil.isNotEmpty(mMsgInfos)) {
                                                        //重新设置消息的信息
                                                        for (MsgInfo msgInfo : mMsgInfos) {
                                                            long time = System.currentTimeMillis();
                                                            msgInfo.setThreadID(msgThread.getId());
                                                            msgInfo.setMsgId(SystemUtil.generateUUID());
                                                            msgInfo.setComming(false);
                                                            msgInfo.setCreationDate(time);
                                                            msgInfo.setRead(true);
                                                            msgInfo.setFromUser(fromUser);
                                                            msgInfo.setToUser(toUser);

                                                            MsgPart msgPart = msgInfo.getMsgPart();
                                                            if (msgPart != null) {
                                                                msgPart.setMsgId(msgInfo.getMsgId());
                                                                msgPart.setCreationDate(time);
                                                                msgPart.setDownloaded(true);
                                                                msgPart.setMsgId(msgInfo.getMsgId());
                                                                msgPart.setId(0);
                                                                msgPart.setFileToken(null);
                                                                String thumbPath = msgPart.getThumbPath();
                                                                String filePath = msgPart.getFilePath();
                                                                if (SystemUtil.isFileExists(filePath)) {    //原始文件存在
                                                                    SystemUtil.generateThumbFile(filePath);
                                                                    msgPart.setThumbName(SystemUtil.getFilename(thumbPath));
                                                                } else {    //原始文件不存在，则将缩略图当原始图片发送
                                                                    msgPart.setFilePath(thumbPath);
                                                                    msgPart.setFileName(SystemUtil.getFilename(thumbPath));

                                                                    msgPart.setThumbName(null);
                                                                    msgPart.setThumbPath(null);
                                                                }

                                                                if (mSendType == SEND_TYPE_SHARE) { //分享过来的
                                                                    if (msgInfo.getMsgType() == MsgInfo.Type.IMAGE) {
                                                                        //生成缩略图
                                                                        String thumbName = SystemUtil.generateChatThumbAttachFilename(time);
                                                                        String savePath = ImageUtil.generateThumbImage(filePath, msgInfo.getThreadID(), thumbName);
                                                                        if (savePath != null) {
                                                                            msgPart.setThumbName(thumbName);
                                                                            msgPart.setThumbPath(savePath);
                                                                        }
                                                                    }
                                                                }
                                                            }

                                                            if (mSendOpt != OPT_FORWARD) {  //不是聊天界面的消息转发，则不用立即跳转到chat界面
                                                                sendMsg(msgInfo, msgThread);
                                                            }

                                                        }

                                                        pDialog.dismiss();
                                                        Message msg = mHandler.obtainMessage();
                                                        msg.what = Constants.MSG_SUCCESS;
                                                        msg.obj = msgThread;
                                                        mHandler.sendMessage(msg);
                                                    } else {
                                                        pDialog.dismiss();
                                                        mHandler.sendEmptyMessage(Constants.MSG_FAILED);
                                                    }

                                                } else {
                                                    Log.w("---onItemClick---choseItem---" + choseItem + "----msgThread-----is null---");
                                                }
                                            }
                                        }
                                    });

                                }
                            }).show();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        getSupportLoaderManager().destroyLoader(0);
        mSendOpt = OPT_FORWARD;

        try {
            if (mServiceConnection != null) {
                unbindService(mServiceConnection);
            }
        } catch (Exception e) {
            Log.e(e.getMessage());
        }

        super.onDestroy();
    }

    /**
     * 发送消息
     * @param msgInfo 消息
     * @param msgThread 发送的会话
     * @author tiger
     * @update 2015/12/27 13:09
     * @version 1.0.0
     */
    private void sendMsg(MsgInfo msgInfo, MsgThread msgThread) {
        if (msgInfo != null) {
            if (mChat == null) {
                mChat = XmppUtil.createChat(mChatManager, mContext, msgInfo.getToUser(), true);
            }
            if (mChat != null) {
                MsgSenderInfo msgSenderInfo = new MsgSenderInfo(mChat, msgInfo, msgThread, mHandler, false);
                mCoreService.sendChatMsg(msgSenderInfo);
            } else {
                Log.d("-----sendMsgs----mChat---is----null-----");
            }
        } else {
            Log.d("-----sendMsgs----msgInfo---is----empty-----");
        }
    }

    @Override
    public Loader<List<ChatChoseItem>> onCreateLoader(int id, Bundle args) {
        if (args != null) {
            args = new Bundle();
            args.putInt(ChatChoseLoader.ARG_LOAG_TYPE, mLoadType);
        }
        return new ChatChoseLoader(mContext, args);
    }

    @Override
    public void onLoadFinished(Loader<List<ChatChoseItem>> loader, List<ChatChoseItem> data) {
        if (mLoadType == ChatChoseItem.DATA_THREAD) {   //加载会话
            mChoseItems.clear();
            if (SystemUtil.isNotEmpty(data)) {
                ChatChoseItem group = getChoseGroup(ChatChoseItem.DATA_THREAD);
                mChoseItems.add(group);
                mChoseItems.addAll(data);
            }

            //再加载联系人
            if (mFooteryView == null) {
                mFooteryView = getFooterView(mContext);
            }
            mLvData.addFooterView(mFooteryView);
            mLvData.setFooterDividersEnabled(false);
            mLoadType = ChatChoseItem.DATA_CONTACT;
            Bundle args = new Bundle();
            args.putInt(ChatChoseLoader.ARG_LOAG_TYPE, mLoadType);
            getSupportLoaderManager().initLoader(1, args, this);
        } else {    //加载好友
            if (SystemUtil.isNotEmpty(data)) {
                ChatChoseItem group = getChoseGroup(ChatChoseItem.DATA_CONTACT);
                mChoseItems.add(group);
                mChoseItems.addAll(data);
                if (mFooteryView != null) {
                    mLvData.removeFooterView(mFooteryView);
                }
            }
        }
        if (mChoseItems.size() == 0) {  //无数据
            if (mLvData.getEmptyView() == null) {
                mLvData.setEmptyView(getEmptyView());
            }
        }
        if (mChoseAdapter == null) {
            mChoseAdapter = new ChatChoseAdapter(mChoseItems, mContext);
            mLvData.setAdapter(mChoseAdapter);
        } else {
            mChoseAdapter.notifyDataSetChanged();
        }
    }
    
    /**
     * 获取列表的无记录视图
     * @return 返回view
     * 创建人：huanghui1
     * 创建时间： 2015/11/11 16:25
     * 修改人：huanghui1
     * 修改时间：2015/11/11 16:25
     * 修改备注：
     * @version: 0.0.1
     */
    private View getEmptyView() {
        if (mEmptyView == null) {
            ViewStub viewStub = (ViewStub) findViewById(R.id.empty_stub);
            View view = viewStub.inflate();
            mEmptyView = view.findViewById(R.id.empty_view);
        }
        return mEmptyView;
    }
    
    /**
     * 创建人：huanghui1
     * 创建时间： 2015/11/11 18:19
     * 修改人：huanghui1
     * 修改时间：2015/11/11 18:19
     * 修改备注：
     * @version: 0.0.1
     */
    private View getFooterView(Context context) {
        if (mFooteryView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            mFooteryView = inflater.inflate(R.layout.layout_head_loading, null);
        }
        return mFooteryView;
    }
    
    /**
     * 为列表添加分组的标题
     * @param dataType 加载数据的类型，主要是0:{@code ChatChoseItem.DATA_THREAD}, 1:{@code ChatChoseItem.DATA_CONTACT}
     * @return 返回分组                
     * 创建人：huanghui1
     * 创建时间： 2015/11/11 15:10
     * 修改人：huanghui1
     * 修改时间：2015/11/11 15:10
     * 修改备注：
     * @version: 0.0.1
     */
    private ChatChoseItem getChoseGroup(int dataType) {
        ChatChoseItem item = new ChatChoseItem();
        item.setItemType(ChatChoseItem.TYPE_GROUP);
        item.setDataType(dataType);
        return item;
    }

    @Override
    public void onLoaderReset(Loader<List<ChatChoseItem>> loader) {
        if (mChoseAdapter != null) {
            mChoseAdapter.swapData(null);
        }
    }
    
    /**
     * 转发、分享选择会话的适配器
     * 创建人：huanghui1
     * 创建时间： 2015/11/11 14:19
     * 修改人：huanghui1
     * 修改时间：2015/11/11 14:19
     * 修改备注：
     * @version: 0.0.1
     */
    class ChatChoseAdapter extends CommonAdapter<ChatChoseItem> {
        private int typeCount = 2;
        
        public ChatChoseAdapter(List<ChatChoseItem> list, Context context) {
            super(list, context);
        }

        @Override
        public int getViewTypeCount() {
            return typeCount;
        }

        @Override
        public int getItemViewType(int position) {
            ChatChoseItem item = list.get(position);
            return item.getItemType();
        }

        /**
         * 包装数据
         * 创建人：huanghui1
         * 创建时间： 2015/11/11 16:32
         * 修改人：huanghui1
         * 修改时间：2015/11/11 16:32
         * 修改备注：
         * @version: 0.0.1
         */
        public void swapData(List<ChatChoseItem> data) {
            list.clear();
            if (data != null) {
                list.addAll(data);
            }
            notifyDataSetChanged();
        }

        @Override
        public boolean isEnabled(int position) {
            ChatChoseItem item = list.get(position);
            return item.getItemType() == ChatChoseItem.TYPE_ITEM;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int viewType = getItemViewType(position);
            ChatChoseItem choseItem = list.get(position);
            switch (viewType) {
                case ChatChoseItem.TYPE_GROUP:  //分组
                    GroupViewHolder groupHolder = null;
                    if (convertView == null) {
                        groupHolder = new GroupViewHolder();
                        convertView = inflater.inflate(R.layout.item_chat_chose_spliter, parent, false);
                        groupHolder.tvTitle = (TextView) convertView.findViewById(R.id.tv_catalog);
                        convertView.setTag(groupHolder);
                    } else {
                        groupHolder = (GroupViewHolder) convertView.getTag();
                    }
                    if (choseItem.getDataType() == ChatChoseItem.DATA_THREAD) { //会话、聊天
                        groupHolder.tvTitle.setText(R.string.chat_chose_item_title_thread);
                    } else {
                        groupHolder.tvTitle.setText(R.string.chat_chose_item_title_contact);
                    }
                    break;
                case ChatChoseItem.TYPE_ITEM:   //每一项
                    ItemViewHolder itemHolder = null;
                    if (convertView == null) {
                        itemHolder = new ItemViewHolder();
                        convertView = inflater.inflate(R.layout.item_chat_chose, parent, false);
                        itemHolder.ivIcon = (ImageView) convertView.findViewById(R.id.iv_head_icon);
                        itemHolder.tvName = (TextView) convertView.findViewById(R.id.tv_name);
                        convertView.setTag(itemHolder);
                    } else {
                        itemHolder = (ItemViewHolder) convertView.getTag();
                    }
                    if (choseItem.getDataType() == ChatChoseItem.DATA_THREAD) { //会话
                        MsgThread msgThread = choseItem.getMsgThread();
                        if (msgThread != null) {
                            itemHolder.tvName.setText(msgThread.getMsgThreadName());
                            Drawable icon = msgThread.getIcon();
                            final User member = msgThread.getMembers().get(0);
                            if(member != null) {
                                final UserVcard uCard = member.getUserVcard();
                                if (uCard != null) {
                                    if (icon != null) {
                                        itemHolder.ivIcon.setImageDrawable(icon);
                                    } else {
                                        showIcon(uCard, itemHolder.ivIcon, options);
                                    }
                                } else {
                                    mImageLoader.displayImage(null, itemHolder.ivIcon, options);
                                }
                            } else {
                                mImageLoader.displayImage(null, itemHolder.ivIcon, options);
                            }
                        }
                    } else {    //好友
                        User user = choseItem.getUser();
                        if (user != null) {
                            final UserVcard userVcard = user.getUserVcard();
                            itemHolder.tvName.setText(user.getName());
                            showIcon(userVcard, itemHolder.ivIcon, options);
                        }
                    }
                    break;
                    
            }
            return convertView;
        }
    }

    /**
     * 显示用户头像
     * @param userVcard 用户的电子名片信息
     * @param imageView 图片view
     * @param options 图片的显示配置项               
     * @update 2015年8月20日 下午3:01:42
     */
    private void showIcon(UserVcard userVcard, ImageView imageView, DisplayImageOptions options) {
        if (userVcard != null) {
            String iconPath = userVcard.getIconShowPath();
            if (SystemUtil.isFileExists(iconPath)) {
                String imageUri = ImageDownloader.Scheme.FILE.wrap(iconPath);
                mImageLoader.displayImage(imageUri, imageView, options);
            } else {
                mImageLoader.displayImage(null, imageView, options);
            }
        } else {
            mImageLoader.displayImage(null, imageView, options);
        }
    }
    
    /**
     * 分组的holder
     * 创建人：huanghui1
     * 创建时间： 2015/11/11 15:25
     * 修改人：huanghui1
     * 修改时间：2015/11/11 15:25
     * 修改备注：
     * @version: 0.0.1
     */
    final class GroupViewHolder {
        TextView tvTitle;
    }
    
    /**
     * 创建人：huanghui1
     * 创建时间： 2015/11/11 15:25
     * 修改人：huanghui1
     * 修改时间：2015/11/11 15:25
     * 修改备注：
     * @version: 0.0.1
     */
    final class ItemViewHolder {
        ImageView ivIcon;
        TextView tvName;
    }
}
