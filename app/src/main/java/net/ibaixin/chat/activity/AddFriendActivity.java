package net.ibaixin.chat.activity;

import java.util.ArrayList;
import java.util.List;

import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.R;
import net.ibaixin.chat.manager.UserManager;
import net.ibaixin.chat.model.NewFriendInfo;
import net.ibaixin.chat.model.User;
import net.ibaixin.chat.model.NewFriendInfo.FriendStatus;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.util.XmppConnectionManager;
import net.ibaixin.chat.util.XmppUtil;
import net.ibaixin.chat.view.ProgressDialog;

import org.jivesoftware.smack.SmackException.NotConnectedException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

/**
 * 添加好友界面，主要是查询好友
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年10月9日 下午9:11:37
 */
public class AddFriendActivity extends BaseActivity {
	
	private EditText etUsername;
	private Button btnSearch;
	private ListView lvResult;
	private TextView emptyView;
	
	private List<User> users = new ArrayList<>();
	private FriendResultAdapter adapter;
	private ProgressDialog pDialog;
	
	private UserManager userManager = UserManager.getInstance();
	
	private int userType = UserInfoActivity.TYPE_STRANGER;
	
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			hideLoadingDialog(pDialog);
			switch (msg.what) {
			case Constants.MSG_CONNECTION_UNAVAILABLE:	//客户端与服务器没有连接
				new AlertDialog.Builder(mContext)
					.setTitle(R.string.prompt)
					.setMessage(R.string.connection_unavailable)
					.setNegativeButton(android.R.string.cancel, null)
					.setCancelable(false)
					.setPositiveButton(android.R.string.ok, null).show();
				break;
			case Constants.MSG_SEND_ADD_FRIEND_REQUEST:	//发送添加好友的请求
				SystemUtil.makeShortToast(R.string.contact_send_add_friend_request_success);
				break;
			case Constants.MSG_ALREAD_ADDED:	//提示已添加过了
				SystemUtil.makeShortToast(R.string.contact_send_add_friend_request_repeat);
				break;
			default:
				break;
			}
		}
	};
	
	@Override
	protected int getContentView() {
		return R.layout.activity_search_friend;
	}

	@Override
	protected void initView() {
		etUsername = (EditText) findViewById(R.id.et_username);
		btnSearch = (Button) findViewById(R.id.btn_search);
		lvResult = (ListView) findViewById(R.id.lv_result);
		emptyView = (TextView) findViewById(R.id.empty_view);
	}

	@Override
	protected void initData() {
//		users = new ArrayList<>();
//		adapter = new FriendResultAdapter(users, mContext);
//		lvResult.setAdapter(adapter);
		
		lvResult.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				ViewHolder holder = (ViewHolder) view.getTag();
				User user = users.get(position);
				Intent intent = new Intent(mContext, UserInfoActivity.class);
				intent.putExtra(UserInfoActivity.ARG_USER, user);
				intent.putExtra(UserInfoActivity.ARG_OPTION, UserInfoActivity.OPTION_SEARCH);
				intent.putExtra(UserInfoActivity.ARG_USER_TYPE, holder.typtTag);
				startActivity(intent);
			}
		});
	}

	@Override
	protected void addListener() {
		etUsername.addTextChangedListener(new TextWatcher() {
			
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
				// TODO Auto-generated method stub
				if(TextUtils.isEmpty(s)) {
					btnSearch.setEnabled(false);
				} else {
					btnSearch.setEnabled(true);
				}
			}
		});
		
		btnSearch.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String username = etUsername.getText().toString();
				new SearchTask().execute(username);
			}
		});
	}
	
	/**
	 * 搜索好友
	 * @author huanghui1
	 * @update 2014年10月9日 下午9:38:32
	 */
	class SearchTask extends AsyncTask<String, Void, List<User>> {
		@Override
		protected void onPreExecute() {
			if (pDialog == null) {
				pDialog = ProgressDialog.show(mContext, null, getString(R.string.contact_searching), true);
			} else {
				pDialog.show();
			}
		}

		@Override
		protected List<User> doInBackground(String... params) {
			List<User> list = XmppUtil.searchUser(XmppConnectionManager.getInstance().getConnection(), params[0]);
			if (list != null && list.size() > 0) {
				users.clear();
				users.addAll(list);
				return list;
			} else {
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(List<User> result) {
			users.clear();
			if (SystemUtil.isNotEmpty(result)) {
				users.addAll(result);
			}
			if(adapter == null) {
				adapter = new FriendResultAdapter(users, mContext);
				lvResult.setAdapter(adapter);
				lvResult.setEmptyView(emptyView);
			} else {
				if (lvResult.getEmptyView() == null) {
					lvResult.setEmptyView(emptyView);
				}
				adapter.notifyDataSetChanged();
			}
			hideLoadingDialog(pDialog);
			
		}
		
	}
	
	/**
	 * 搜索好友的适配器
	 * @author huanghui1
	 * @update 2014年10月9日 下午10:46:08
	 */
	class FriendResultAdapter extends CommonAdapter<User> {

		public FriendResultAdapter(List<User> list, Context context) {
			super(list, context);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = inflater.inflate(R.layout.item_search_friend, parent, false);
				
				holder.tvUsername = (TextView) convertView.findViewById(R.id.tv_username);
				holder.tvNickname = (TextView) convertView.findViewById(R.id.tv_nickname);
				holder.btnAdd = (Button) convertView.findViewById(R.id.btn_add);
				
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			final User user = list.get(position);
			String username = user.getUsername();
			holder.tvUsername.setText(username);
			holder.tvNickname.setText(user.getNickname());
			
			//是否是自己
			boolean isSelf = ChatApplication.getInstance().isSelf(username);
			if (isSelf) {
				userType = UserInfoActivity.TYPE_SELF;
			} else {
				boolean isFriend = userManager.isLocalFriend(username);
				if (isFriend) {//是本地好友
					userType = UserInfoActivity.TYPE_FRIEND;
				} else {//本地没有该人的信息，则从网上加载
					userType = UserInfoActivity.TYPE_STRANGER;
				}
			}
			holder.typtTag = userType;
			final String jid = user.getJID();
			switch (userType) {
			case UserInfoActivity.TYPE_STRANGER:	//陌生人
				holder.btnAdd.setText(R.string.add);
				break;
			case UserInfoActivity.TYPE_SELF:
			case UserInfoActivity.TYPE_FRIEND:
				holder.btnAdd.setText(R.string.show);
				break;
			default:
				break;
			}
			holder.btnAdd.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					switch (userType) {
					case UserInfoActivity.TYPE_STRANGER:	//陌生人
						mHandler.post(new Runnable() {
							
							@Override
							public void run() {
								Message msg = mHandler.obtainMessage();
								String from = ChatApplication.getInstance().getCurrentAccount();
								String to = SystemUtil.unwrapJid(jid);
								UserManager userManager = UserManager.getInstance();
								NewFriendInfo newInfo = new NewFriendInfo();
								newInfo.setFrom(from);
								newInfo.setTo(to);
								boolean hasNewInfo = userManager.hasNewFriendInfo(newInfo);
								if (hasNewInfo) {	//已经添加过请求了，但对方一直没有回应，不需要重新添加了
									msg.what = Constants.MSG_ALREAD_ADDED;
								} else {
									try {
										XmppUtil.addFriend(XmppConnectionManager.getInstance().getConnection(), jid);
										
										newInfo.setFriendStatus(FriendStatus.VERIFYING);
										newInfo.setFrom(from);
										newInfo.setTo(to);
										newInfo.setTitle(to);
										newInfo.setContent(mContext.getString(R.string.contact_friend_add_target_request));
										newInfo.setCreationDate(System.currentTimeMillis());
										
										//如果本地有该好友，则看要不要更新头像
										newInfo = userManager.saveOrUpdateNewFriendInfo(newInfo);
										
										msg.what = Constants.MSG_SEND_ADD_FRIEND_REQUEST;
									} catch (NotConnectedException e) {
										Log.e(e.getMessage());
										msg.what = Constants.MSG_CONNECTION_UNAVAILABLE;
									}
								}
								mHandler.sendMessage(msg);
							}
						});
						break;
					case UserInfoActivity.TYPE_SELF:
					case UserInfoActivity.TYPE_FRIEND:
						Intent intent = new Intent(mContext, UserInfoActivity.class);
						intent.putExtra(UserInfoActivity.ARG_USER, user);
						intent.putExtra(UserInfoActivity.ARG_OPTION, UserInfoActivity.OPTION_SEARCH);
						intent.putExtra(UserInfoActivity.ARG_USER_TYPE, userType);
						startActivity(intent);
						break;
					default:
						break;
					}
				}
			});
			return convertView;
		}
		
	}
	
	/**
	 * listview item的缓存
	 * @author huanghui1
	 * @update 2014年10月9日 下午10:47:22
	 */
	private final class ViewHolder {
		TextView tvUsername;
		TextView tvNickname;
		Button btnAdd;
		int typtTag = UserInfoActivity.TYPE_STRANGER;
	}

}
