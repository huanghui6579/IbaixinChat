package net.ibaixin.chat.activity;

import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.R;
import net.ibaixin.chat.manager.PersonalManage;
import net.ibaixin.chat.model.Personal;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.util.XmppConnectionManager;
import net.ibaixin.chat.util.XmppUtil;
import net.ibaixin.chat.view.ProgressDialog;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.MenuItemCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

/**
 * 修改昵称的界面
 * @author tiger
 * @version 2015年3月15日 下午9:04:15
 */
public class EditNicknameActivity extends BaseActivity {
	public static final String ARG_NICKNAME = "arg_nickname";
	
	private EditText etNickname;
	
	private Personal mPersonal;
	
//	private TextView btnOpt;
	
	private MenuItem mMenuDone;
	
//	private ProgressDialog pDialog;
	
	private String oNickname;
	
	private boolean isEmptyName;
	
	private PersonalManage mPersonalManage;
	
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Constants.MSG_SUCCESS:
				Intent intent = new Intent();
				intent.putExtra(ARG_NICKNAME, (String) msg.obj);
				setResult(RESULT_OK, intent);
				finish();
				break;
			case Constants.MSG_FAILED:
				SystemUtil.makeShortToast(R.string.opt_failed);
				break;
			default:
				break;
			}
		}
	};

	@Override
	protected int getContentView() {
		return R.layout.activity_edit_nickname;
	}

	@Override
	protected void initView() {
		etNickname = (EditText) findViewById(R.id.et_nickname);
	}

	@Override
	protected void initData() {
		mPersonal = ChatApplication.getInstance().getCurrentUser();
		mPersonalManage = PersonalManage.getInstance();
		if (mPersonal != null) {
			oNickname = mPersonal.getNickname();
			isEmptyName = TextUtils.isEmpty(oNickname);
			etNickname.setText(oNickname);
		}
	}

	@Override
	protected void addListener() {
		etNickname.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				if (isEmptyName) {	//初始昵称为空
					if (TextUtils.isEmpty(s)) {
						setMenuEnable(false);
					} else {
						setMenuEnable(true);
					}
				} else {	//初始昵称不为空
					if (oNickname.equals(s.toString())) {
						setMenuEnable(false);
					} else {
						setMenuEnable(true);
					}
				}
			}
		});
	}
	
	/**
	 * 设置菜单的可用状态
	 * @update 2015年8月25日 下午4:11:22
	 */
	private void setMenuEnable(boolean enable) {
		if (mMenuDone != null) {
			mMenuDone.setEnabled(enable);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.menu_save, menu);
		mMenuDone = menu.findItem(R.id.action_select_complete);
		mMenuDone.setTitle(R.string.save);
		mMenuDone.setEnabled(false);
		/*btnOpt = (TextView) MenuItemCompat.getActionView(menuDone);
		btnOpt.setText(R.string.save);
		btnOpt.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				final String username = ChatApplication.getInstance().getCurrentAccount();
				if (!TextUtils.isEmpty(username)) {
					pDialog = ProgressDialog.show(mContext, null, getString(R.string.loading), true);
					SystemUtil.getCachedThreadPool().execute(new Runnable() {
						
						@Override
						public void run() {
							Message msg = mHandler.obtainMessage();
							try {
								AbstractXMPPConnection connection = XmppConnectionManager.getInstance().getConnection();
								
								//通知好友更新头像
								if (XmppUtil.checkAuthenticated(connection)) {
									Editable editable = etNickname.getText();
									String nickname = editable == null ? null : editable.toString();
									
									XmppUtil.updateNickname(connection, nickname);
									mPersonal.setNickname(nickname);
									//保存昵称信息到本地数据库
									mPersonalManage.updateNickname(mPersonal);
									
									msg.what = Constants.MSG_SUCCESS;
									msg.obj = nickname;
								} else {
									msg.what = Constants.MSG_FAILED;
								}
									
							} catch (NoResponseException | XMPPErrorException | NotConnectedException e) {
								msg.what = Constants.MSG_FAILED;
								e.printStackTrace();
							}
							pDialog.dismiss();
							mHandler.sendMessage(msg);
						}
					});
				} else {
					SystemUtil.makeShortToast(R.string.not_login);
				}
			}
		});*/
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_select_complete:	//保存
			final String username = ChatApplication.getInstance().getCurrentAccount();
			if (!TextUtils.isEmpty(username)) {
//				pDialog = ProgressDialog.show(mContext, null, getString(R.string.loading), true);
				SystemUtil.getCachedThreadPool().execute(new Runnable() {
					
					@Override
					public void run() {
						Message msg = mHandler.obtainMessage();
						try {
//							AbstractXMPPConnection connection = XmppConnectionManager.getInstance().getConnection();
							
							//通知好友更新头像
							/*if (XmppUtil.checkAuthenticated(connection)) {
								Editable editable = etNickname.getText();
								String nickname = editable == null ? null : editable.toString();
								
								XmppUtil.updateNickname(connection, nickname);
								mPersonal.setNickname(nickname);
								//保存昵称信息到本地数据库
								mPersonalManage.updateNickname(mPersonal);
								
								msg.what = Constants.MSG_SUCCESS;
								msg.obj = nickname;
							} else {
								msg.what = Constants.MSG_FAILED;
							}*/
							Editable editable = etNickname.getText();
							String nickname = editable == null ? null : editable.toString();
							mPersonal.setNickname(nickname);
							//保存昵称信息到本地数据库
							mPersonalManage.updateNickname(mPersonal);
							
							msg.what = Constants.MSG_SUCCESS;
							msg.obj = nickname;
								
						} catch (/*NoResponseException | XMPPErrorException | NotConnected*/Exception e) {
							msg.what = Constants.MSG_FAILED;
							e.printStackTrace();
						}
//						pDialog.dismiss();
						mHandler.sendMessage(msg);
					}
				});
			} else {
				SystemUtil.makeShortToast(R.string.not_login);
			}
			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

}
