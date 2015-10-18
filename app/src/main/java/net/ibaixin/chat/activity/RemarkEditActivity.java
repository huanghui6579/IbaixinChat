package net.ibaixin.chat.activity;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jxmpp.util.XmppStringUtils;

import android.os.Handler;
import android.os.Message;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import net.ibaixin.chat.R;
import net.ibaixin.chat.manager.UserManager;
import net.ibaixin.chat.model.User;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.util.XmppConnectionManager;
import net.ibaixin.chat.util.XmppUtil;
import net.ibaixin.chat.view.ProgressDialog;

/**
 * 修改好友备注的界面
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年2月26日 下午2:06:59
 */
public class RemarkEditActivity extends BaseActivity {
	private EditText etNickname;
	
	private TextView btnOpt;
	
	private ProgressDialog pDialog;
	
	private UserManager mUserManager;
	
	/**
	 * 传过来的用户实体
	 */
	private User mUser;
	
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if (pDialog != null && pDialog.isShowing()) {
				pDialog.dismiss();
			}
			switch (msg.what) {
			case Constants.MSG_SUCCESS:	//保存成功
				finish();
				break;
			case Constants.MSG_FAILED:	//保存失败
				SystemUtil.makeShortToast(R.string.save_failed);
				break;

			default:
				break;
			}
		}
	};

	@Override
	protected int getContentView() {
		return R.layout.activity_edit_remark;
	}

	@Override
	protected void initView() {
		etNickname = (EditText) findViewById(R.id.et_nickname);
	}

	@Override
	protected void initData() {
		mUserManager = UserManager.getInstance();
		mUser = getIntent().getParcelableExtra(UserInfoActivity.ARG_USER);
		
		if (mUser != null) {
			etNickname.setText(mUser.getNickname());
		}
	}

	@Override
	protected void addListener() {
		// TODO Auto-generated method stub

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.common_opt, menu);
		MenuItem menuDone = menu.findItem(R.id.action_select_complete);
		btnOpt = (TextView) MenuItemCompat.getActionView(menuDone);
		btnOpt.setEnabled(true);
		btnOpt.setText(R.string.save);
		btnOpt.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (!etNickname.getText().toString().equals(mUser.getNickname())) {	//昵称没有做修改，则直接退出
					pDialog = ProgressDialog.show(mContext, null, getString(R.string.chat_sending_file), true);
					//保存备注
					SystemUtil.getCachedThreadPool().execute(new Runnable() {
						
						@Override
						public void run() {
							XMPPConnection connection = XmppConnectionManager.getInstance().getConnection();
							if (XmppUtil.checkAuthenticated(connection)) {
								Roster roster = Roster.getInstanceFor(connection);
								String jid  =  mUser.getFullJid();
								if(XmppStringUtils.isFullJID(jid)) {
									jid = XmppStringUtils.parseBareJid(jid);
								}
								RosterEntry rosterEntry = roster.getEntry(jid);
								try {
									String nickname = etNickname.getText().toString();
									if(rosterEntry==null){
										mHandler.sendEmptyMessage(Constants.MSG_FAILED);
										return ;
									}
									rosterEntry.setName(nickname);
									mUser.setNickname(nickname);
									mUserManager.updateFriendNick(mUser);
									//通知好友列表更新好友
//									Intent intent = new Intent(LoadDataBroadcastReceiver.ACTION_USER_UPDATE);
//									intent.putExtra(UserInfoActivity.ARG_USER, mUser);
//									sendBroadcast(intent);
									
									mHandler.sendEmptyMessage(Constants.MSG_SUCCESS);
								} catch (NotConnectedException | NoResponseException | XMPPErrorException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
									mHandler.sendEmptyMessage(Constants.MSG_FAILED);
								}
							}
						}
					});
				} else {
					mHandler.sendEmptyMessage(Constants.MSG_SUCCESS);
				}
				
			}
		});
		return super.onCreateOptionsMenu(menu);
	}

}
