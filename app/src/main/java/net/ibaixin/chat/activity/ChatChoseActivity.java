package net.ibaixin.chat.activity;

import android.view.View;
import android.widget.ListView;

import net.ibaixin.chat.R;

/**
 * 聊天选择联系人界面
 * 创建人：huanghui1
 * 创建时间： 2015/11/9 18:03
 * 修改人：huanghui1
 * 修改时间：2015/11/9 18:03
 * 修改备注：
 * @version: 0.0.1
 */
public class ChatChoseActivity extends BaseActivity {
    private ListView mLvData;
    private View mEmptyView;

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

    }

    @Override
    protected void addListener() {

    }
}
