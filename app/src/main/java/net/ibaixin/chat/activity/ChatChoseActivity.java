package net.ibaixin.chat.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
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
import net.ibaixin.chat.model.MsgThread;
import net.ibaixin.chat.model.ShowInfo;
import net.ibaixin.chat.model.User;
import net.ibaixin.chat.model.UserVcard;
import net.ibaixin.chat.receiver.NetworkReceiver;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.util.XmppConnectionManager;
import net.ibaixin.chat.view.ProgressDialog;

import org.jivesoftware.smack.AbstractXMPPConnection;

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
    
    private ListView mLvData;
    private View mEmptyView;
    private View mFooteryView;
    private List<ChatChoseItem> mChoseItems;
    
    private ChatChoseAdapter mChoseAdapter;
    
    private ImageLoader mImageLoader = ImageLoader.getInstance();

    private DisplayImageOptions options = SystemUtil.getGeneralImageOptions();

    /**
     * 自身登录的账号
     */
    private String mCurrentAccount;

    /**
     * 传入的消息实体
     */
    private ArrayList<MsgInfo> mMsgInfos;

    /**
     * 加载数据的类型，默认是会话
     */
    private int mLoadType = ChatChoseItem.DATA_THREAD;
    
    private ProgressDialog pDialog;
    
    private AbstractXMPPConnection mConnection;
    
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
            }
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

        mConnection = XmppConnectionManager.getInstance().getConnection();

        pDialog = ProgressDialog.show(mContext, null, getString(R.string.loading));
        SystemUtil.getCachedThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                boolean hasNetWork = NetworkReceiver.checkNetwork(mContext);
                if (hasNetWork) {   //有网络
                    boolean hasAuthority = XmppConnectionManager.getInstance().checkAuthority(mConnection, application);
                    pDialog.dismiss();
                    if (hasAuthority) { //有权限或者登录成功
                        mCurrentAccount = application.getCurrentAccount();

                        getSupportLoaderManager().initLoader(0, null, ChatChoseActivity.this);

                        Intent intent = getIntent();
                        if (intent != null) {
                            mMsgInfos = intent.getParcelableArrayListExtra(ARG_MSG_INFOS);
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
                                    if (choseItem.isItemType()) {
                                        MsgManager msgManager = MsgManager.getInstance();
                                        MsgThread msgThread = null;
                                        if (choseItem.getDataType() == ChatChoseItem.DATA_CONTACT) { //点击的是好友
                                            User user = choseItem.getUser();
                                            if (user != null) {
                                                //1、获取会话，如果没有就创建
                                                String username = user.getUsername();
                                                msgThread = msgManager.getThreadByMembers(true, username);
                                            } else {
                                                Log.w("---onItemClick---choseItem---" + choseItem + "--user--is null--");
                                            }
                                        } else {    //点击的是会话
                                            msgThread = choseItem.getMsgThread();
                                        }
                                        if (msgThread != null) {
                                            if (SystemUtil.isNotEmpty(mMsgInfos)) {
                                                //重新设置消息的信息
                                                for (MsgInfo msgInfo : mMsgInfos) {
                                                    long time = System.currentTimeMillis();
                                                    msgInfo.setThreadID(msgThread.getId());
                                                    msgInfo.setMsgId(SystemUtil.generateUUID());
                                                    msgInfo.setComming(false);
                                                    msgInfo.setCreationDate(time);
                                                    msgInfo.setFromUser(mCurrentAccount);

                                                    MsgPart msgPart = msgInfo.getMsgPart();
                                                    if (msgPart != null) {
                                                        msgPart.setMsgId(msgInfo.getMsgId());
                                                        msgPart.setCreationDate(time);
                                                        msgPart.setDownloaded(true);
                                                        msgPart.setId(0);
                                                    }

                                                }
                                            }
                                            Intent intent = new Intent(mContext, ChatActivity.class);
                                            intent.putExtra(ARG_FORWARD_FLAG, true);
                                            intent.putExtra(ChatActivity.ARG_THREAD, msgThread);
                                            intent.putParcelableArrayListExtra(ARG_MSG_INFOS, mMsgInfos);
                                            startActivity(intent);
                                        } else {
                                            Log.w("---onItemClick---choseItem---" + choseItem + "----msgThread-----is null---");
                                        }
                                    }
                                }
                            }).show();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        getSupportLoaderManager().destroyLoader(0);
        super.onDestroy();
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
            viewStub.inflate();
            mEmptyView = findViewById(R.id.empty_view);
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
     * 列表项的holder
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
