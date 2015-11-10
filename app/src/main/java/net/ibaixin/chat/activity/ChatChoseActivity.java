package net.ibaixin.chat.activity;

import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;

import net.ibaixin.chat.R;
import net.ibaixin.chat.loader.ThreadListLoader;
import net.ibaixin.chat.model.ChatChoseItem;
import net.ibaixin.chat.model.MsgThread;

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
public class ChatChoseActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<List<MsgThread>> {
    private ListView mLvData;
    private View mEmptyView;
    private List<ChatChoseItem> mChoseItems;

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
    public Loader<List<MsgThread>> onCreateLoader(int id, Bundle args) {
        if (args != null) {
            args = new Bundle();
            args.putBoolean(ThreadListLoader.ARG_LOAG_LASTMSG, false);
        }
        return new ThreadListLoader(mContext, args);
    }

    @Override
    public void onLoadFinished(Loader<List<MsgThread>> loader, List<MsgThread> data) {

    }

    @Override
    public void onLoaderReset(Loader<List<MsgThread>> loader) {

    }
}
