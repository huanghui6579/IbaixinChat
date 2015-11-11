package net.ibaixin.chat.activity;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.download.ImageDownloader;

import net.ibaixin.chat.R;
import net.ibaixin.chat.loader.ChatChoseLoader;
import net.ibaixin.chat.model.ChatChoseItem;
import net.ibaixin.chat.model.MsgThread;
import net.ibaixin.chat.model.User;
import net.ibaixin.chat.model.UserVcard;
import net.ibaixin.chat.util.SystemUtil;

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
    private ListView mLvData;
    private View mEmptyView;
    private View mFooteryView;
    private List<ChatChoseItem> mChoseItems;
    
    private ChatChoseAdapter mChoseAdapter;
    
    private ImageLoader mImageLoader = ImageLoader.getInstance();

    /**
     * 加载数据的类型，默认是会话
     */
    private int mLoadType = ChatChoseItem.DATA_THREAD;

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
        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void addListener() {

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
        private DisplayImageOptions options = SystemUtil.getGeneralImageOptions(); 
        
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