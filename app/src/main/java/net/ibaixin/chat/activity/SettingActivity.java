package net.ibaixin.chat.activity;

import java.io.File;
import java.util.List;

import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;

import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.R;
import net.ibaixin.chat.manager.MsgManager;
import net.ibaixin.chat.model.MsgThread;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.view.ProgressDialog;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * 设置界面
 * @author dudejin
 * @version 1.0.0
 * @update 2015年3月16日
 */
public class SettingActivity extends BaseActivity implements OnClickListener {
	private ProgressDialog proDialog;
	
	/** 清理缓存 **/
	private View setting_item_clear ;
	/** 关于 **/
	private View setting_item_about ;
	/** 退出 **/
	private View setting_item_exit ;
	
	private final int MSG_CLEAR_OK = 0x01;
	private final int MSG_CLEAR_ERROR = 0x02;
	
	Handler mHander = new Handler(){
		public void handleMessage(android.os.Message msg) {
			if(proDialog != null && proDialog.isShowing()) {
				proDialog.dismiss();
			}
			switch (msg.what) {
			case MSG_CLEAR_OK:
				SystemUtil.makeShortToast(R.string.clear_done);
				break;
			case MSG_CLEAR_ERROR:
				SystemUtil.makeShortToast(R.string.clear_error);
				break;
			default:
				break;
			}
		};
	};
	
	@Override
	protected boolean isHomeAsUpEnabled() {
		return true;
	}
	
	@Override
	protected int getContentView() {
		return R.layout.activity_setting;
	}

	@Override
	protected void initView() {
		setting_item_clear = findViewById(R.id.setting_item_clear);
		setting_item_about = findViewById(R.id.setting_item_about);
		setting_item_exit = findViewById(R.id.setting_item_exit);
	}

	@Override
	protected void initData() {

	}

	@Override
	protected void addListener() {
		setting_item_about.setOnClickListener(this);
		setting_item_exit.setOnClickListener(this);
		setting_item_clear.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.setting_item_clear:
			MaterialDialog.Builder builder = new MaterialDialog.Builder(mContext);
			builder.title(R.string.prompt)
				.content(R.string.setting_clear_confirm)
				.positiveText(android.R.string.ok)
				.negativeText(android.R.string.cancel)
				.callback(new MaterialDialog.ButtonCallback() {
					@Override
					public void onPositive(MaterialDialog dialog) {
						clearLocalFiles() ;
					}
				}).show();
			break;
		case R.id.setting_item_about:
			Intent intent = new Intent(mContext, AboutActivity.class);
			startActivity(intent);
			break;
		case R.id.setting_item_exit:
			ChatApplication.getInstance().exit();
			break;
		default:
			break;
		}
	}
	
	/**
	 * 清理本地缓存
	 */
	private void clearLocalFiles() {
		if (proDialog == null) {
			proDialog = ProgressDialog.show(mContext, null, getString(R.string.clear_doing), true);
		} else {
			proDialog.show();
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					SystemUtil.deletFiles(SystemUtil.getPublicFilePath());//删除公共文件目录资源
					SystemUtil.deletFiles(SystemUtil.getDefaultRoot().getAbsolutePath()+File.separator + Constants.DATA_MSG_ATT_FOLDER_NAME);//删除自己的聊天记录文件
					SystemUtil.deletFiles(SystemUtil.getDataParentFile()+File.separator+"app_webview");//删除webview缓存
					List<MsgThread> list = MsgManager.getInstance().getMsgThreadList();
					for(MsgThread m: list){
						MsgManager.getInstance().deleteMsgThreadById(m.getId());
					}
					mHander.sendEmptyMessage(MSG_CLEAR_OK);
				} catch (Exception e) {
					mHander.sendEmptyMessage(MSG_CLEAR_ERROR);
					Log.e(TAG, e.toString());
				}
			}
		}).start();
	}
}
