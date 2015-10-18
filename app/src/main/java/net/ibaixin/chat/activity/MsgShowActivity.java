package net.ibaixin.chat.activity;

import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import net.ibaixin.chat.R;
import net.ibaixin.chat.util.SystemUtil;

/**
 * 文本消息查看界面
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年3月4日 下午7:27:10
 */
public class MsgShowActivity extends BaseActivity {
	public static final String ARG_MSG_CONTENT = "arg_msg_content";
	
	private TextView tvContent;
	
	/**
	 * 文本内容
	 */
	private String msgContent;

	@Override
	protected int getContentView() {
		return R.layout.activity_show_msg;
	}

	@Override
	protected void initView() {
		tvContent = (TextView) findViewById(R.id.tv_content);
	}
	
	@Override
	protected boolean isFullScreen() {
		return true;
	}

	@Override
	protected void initData() {
		msgContent = getIntent().getStringExtra(ARG_MSG_CONTENT);
		if (!TextUtils.isEmpty(msgContent)) {
			tvContent.setText(msgContent);
		}
	}
	
	@Override
	protected void addListener() {
		tvContent.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

}
