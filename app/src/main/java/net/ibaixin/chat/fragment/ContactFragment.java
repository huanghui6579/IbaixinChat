package net.ibaixin.chat.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.download.ImageDownloader.Scheme;

import net.ibaixin.chat.R;
import net.ibaixin.chat.activity.CommonAdapter;
import net.ibaixin.chat.activity.MainActivity.LazyLoadCallBack;
import net.ibaixin.chat.activity.NewFriendInfoActivity;
import net.ibaixin.chat.activity.RemarkEditActivity;
import net.ibaixin.chat.activity.UserInfoActivity;
import net.ibaixin.chat.manager.UserManager;
import net.ibaixin.chat.model.User;
import net.ibaixin.chat.model.UserVcard;
import net.ibaixin.chat.provider.Provider;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.Observable;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.util.XmppConnectionManager;
import net.ibaixin.chat.util.XmppUtil;
import net.ibaixin.chat.view.ProgressDialog;
import net.ibaixin.chat.view.ProgressWheel;
import net.ibaixin.chat.view.SideBar;
import net.ibaixin.chat.view.SideBar.OnTouchingLetterChangedListener;

import org.jivesoftware.smack.AbstractXMPPConnection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 好友列表界面
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年10月8日 下午7:44:40
 */
public class ContactFragment extends BaseFragment implements LazyLoadCallBack {
	private static final int MENU_EDIT_REMARK = 0;
	private static final int MENU_DELETE = 0x1;
	
	private ListView lvContact;
	private TextView tvIndexDialog;
	private SideBar sideBar;
	private ProgressWheel pbLoading;
	
	private ContactAdapter mAdapter;
	
	private List<User> mUsers = new ArrayList<>();
	
	private UserManager userManager = UserManager.getInstance();
	
//	private LoadDataBroadcastReceiver loadDataReceiver;
	
	/**
	 * 好友信息的观察者
	 */
	private UserContentObserver mUserContentObserver;
	
	/**
	 * 是否已经加载数据，该变量作为fragment初始化是否需要加载数据的依据
	 */
	private boolean isLoaded = false;
	
	/**
	 * 当数据库数据发生变化时，是否自动刷新该列表界面，只有删除时才不自动刷新
	 */
	private boolean autoRefresh = true;
	
	ProgressDialog pDialog;
	
	public static String[] USER_PROJECTION = {
		Provider.UserColumns._ID,
		Provider.UserColumns.USERNAME,
		Provider.UserColumns.NICKNAME,
		Provider.UserColumns.EMAIL,
		Provider.UserColumns.PHONE,
		Provider.UserColumns.RESOURCE,
		Provider.UserColumns.MODE,
		Provider.UserColumns.STATUS,
		Provider.UserColumns.FULLPINYIN,
		Provider.UserColumns.SHORTPINYIN,
		Provider.UserColumns.SORTLETTER
	};
	
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if (pDialog != null && pDialog.isShowing()) {
				pDialog.dismiss();
			}
			autoRefresh = true;
			switch (msg.what) {
			case Constants.MSG_SUCCESS:	//操作成功
				mAdapter.notifyDataSetChanged();
				SystemUtil.makeShortToast(R.string.delete_success);
				break;
			case Constants.MSG_FAILED:	//操作失败
				SystemUtil.makeShortToast(R.string.delete_failed);
				break;
			case Constants.MSG_UPDATE_SUCCESS:
				if (mAdapter != null) {
					mAdapter.notifyDataSetChanged();
				}
				break;
			case Constants.MSG_UPDATE_ONE:	//局部更新
				User targetUser = (User) msg.obj;
				int position = msg.arg1;
				if (targetUser != null) {
					updateView(position, targetUser);
				}
				break;
			default:
				break;
			}
		}
		
	};
	
	/**
	 * 初始化fragment
	 * @update 2014年10月8日 下午10:07:58
	 * @return
	 */
	public static ContactFragment newInstance() {
		ContactFragment fragment = new ContactFragment();
		return fragment;
	}
	
	public boolean isLoaded() {
		return isLoaded;
	}

	public void setLoaded(boolean isLoaded) {
		this.isLoaded = isLoaded;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_contact, container, false);
		
		return view;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		//注册好友的观察者
		registUserContentObserver();
	}
	
	/**
	 * 注册好友数据的观察者
	 * 
	 * @update 2015年7月29日 下午9:09:29
	 */
	private void registUserContentObserver() {
		mUserContentObserver = new UserContentObserver(mHandler);
		
		userManager.addObserver(mUserContentObserver);
	}
	
	@Override
	public void onDetach() {
		isLoaded = false;
		
		//注销好友信息的观察者
		if (mUserContentObserver != null) {
			userManager.removeObserver(mUserContentObserver);
		}
		super.onDetach();
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		lvContact = (ListView) view.findViewById(R.id.lv_contact);
		tvIndexDialog = (TextView) view.findViewById(R.id.tv_text_dialog);
		sideBar = (SideBar) view.findViewById(R.id.sidrbar);
		pbLoading = (ProgressWheel) view.findViewById(R.id.pb_loading);
		
		sideBar.setTextView(tvIndexDialog);
		
		sideBar.setOnTouchingLetterChangedListener(new OnTouchingLetterChangedListener() {
			
			@Override
			public void onTouchingLetterChanged(String s) {
				//该字母首次出现的位置
				int position = mAdapter.getPositionForSection(s.charAt(0));
				if(position != -1){
					lvContact.setSelection(position);
				}
				
			}
		});
		
//		View headView = LayoutInflater.from(mContext).inflate(R.layout.layout_contact_head, null);
//		lvContact.addHeaderView(headView, null, false);
		/*TextView headView = new TextView(mContext);
		headView.setText("头部");
		lvContact.addHeaderView(headView);*/
		
		lvContact.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				final User user = (User) mAdapter.getItem(position);
				if (user != null) {
					MaterialDialog.Builder builder = new MaterialDialog.Builder(mContext);
					builder.title(user.getName())
						.items(R.array.contact_list_context_menu)
						.itemsCallback(new MaterialDialog.ListCallback() {
							
							@Override
							public void onSelection(MaterialDialog dialog, View itemView, int which,
									CharSequence text) {
								switch (which) {
								case MENU_EDIT_REMARK:	//修改备注
									Intent intent = new Intent(mContext, RemarkEditActivity.class);
									intent.putExtra(UserInfoActivity.ARG_USER, user);
									startActivity(intent);
									break;
								case MENU_DELETE:	//删除好友
									MaterialDialog.Builder builder = new MaterialDialog.Builder(mContext);
									builder.title(R.string.prompt)
										.content(R.string.contact_list_content_delete_prompt, user.getName())
										.positiveText(android.R.string.ok)
										.negativeText(android.R.string.cancel)
										.callback(new MaterialDialog.ButtonCallback() {

											@Override
											public void onPositive(MaterialDialog dialog) {
												pDialog = ProgressDialog.show(mContext, null, getString(R.string.loading), true);
												
												SystemUtil.getCachedThreadPool().execute(new Runnable() {
													
													@Override
													public void run() {
														AbstractXMPPConnection connection = XmppConnectionManager.getInstance().getConnection();
														//发送删除好友的信息
														boolean success = XmppUtil.deleteUser(connection, user.getUsername());
														if (success) {
															autoRefresh = false;
															//删除好友
															success = userManager.deleteUser(user);
															//是否成功删除该好友
															if (success) {
																mUsers.remove(user);
																mHandler.sendEmptyMessage(Constants.MSG_SUCCESS);
															} else {
																mHandler.sendEmptyMessage(Constants.MSG_FAILED);
															}
														} else {
															mHandler.sendEmptyMessage(Constants.MSG_FAILED);
														}
													}
												});
											}
										})
										.show();
									break;
								default:
									break;
								}
							}
						})
						.show();
				}
				return true;
			}
		});
		
		lvContact.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				int type = mAdapter.getItemViewType(position);
				Intent intent = null;
				switch (type) {
				case ContactAdapter.TYPE_NEW_FRIEND:	//新的朋友
					intent = new Intent(mContext, NewFriendInfoActivity.class);
					startActivity(intent);
					break;
				case ContactAdapter.TYPE_GROUP_CHAT:	//群聊
					SystemUtil.makeShortToast("群聊功能正在开发中，敬请期待");
					
					break;
				case ContactAdapter.TYPE_CONTACT:	//联系人列表
					User target = (User) mAdapter.getItem(position);
					intent = new Intent(mContext, UserInfoActivity.class);
					intent.putExtra(UserInfoActivity.ARG_USER, target);
					intent.putExtra(UserInfoActivity.ARG_OPTION, UserInfoActivity.OPTION_LOAD);
					startActivity(intent);
					break;
				default:
					break;
				}
			}
		});
	}
	
	/**
	 * 改变sideBar的显示和隐藏的状态
	 * @update 2014年10月13日 上午9:53:10
	 * @param flag
	 */
	public void setHideSideBar(boolean flag) {
		if (sideBar != null && tvIndexDialog != null) {
			if (flag) {	//需要隐藏
				sideBar.setVisibility(View.GONE);
				tvIndexDialog.setVisibility(View.GONE);
			} else {
				sideBar.setVisibility(View.VISIBLE);
				tvIndexDialog.setVisibility(View.VISIBLE);
			}
		}
	}
	
	/**
	 * 初始化数据
	 * @update 2014年10月11日 下午8:43:34
	 */
	private void initData() {
		if (!isLoaded) {	//没有加载过数据
			if (mAdapter == null) {
				mAdapter = new ContactAdapter(mUsers, mContext);
				lvContact.setAdapter(mAdapter);
			}
			if (lvContact.getAdapter() == null) {
				lvContact.setAdapter(mAdapter);
			}
			new LoadDataTask().execute();
		}
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
//		registerForContextMenu(lvContact);
		
		//注册加载好友列表的广播
//		loadDataReceiver = new LoadDataBroadcastReceiver();
//		IntentFilter filter = new IntentFilter();
//		filter.addAction(LoadDataBroadcastReceiver.ACTION_USER_LIST);
//		filter.addAction(LoadDataBroadcastReceiver.ACTION_USER_INFOS);
//		filter.addAction(LoadDataBroadcastReceiver.ACTION_USER_ADD);
//		filter.addAction(LoadDataBroadcastReceiver.ACTION_USER_UPDATE);
//		mContext.registerReceiver(loadDataReceiver, filter);
		
		//初始化数据
//		initData();
		
	}
	
	@Override
	public void onDestroy() {
//		mContext.unregisterReceiver(loadDataReceiver);
		super.onDestroy();
	}
	
	/**
	 * 异步加载数据的后台任务线程
	 * @author huanghui1
	 * @update 2014年10月23日 下午2:13:48
	 */
	class LoadDataTask extends AsyncTask<Void, Void, List<User>> {

		@Override
		protected List<User> doInBackground(Void... params) {
			List<User> list = userManager.getFriends();
			return list;
		}
		
		@Override
		protected void onPostExecute(List<User> result) {
			if (SystemUtil.isNotEmpty(result)) {
				if (mUsers.size() > 0) {
					mUsers.clear();
				}
				mUsers.addAll(result);
			}
			Log.d("------LoadDataTask-----onPostExecute---isLoaded---" + isLoaded + "-----mUsers-------" + mUsers);
			mAdapter.notifyDataSetChanged();
			pbLoading.setVisibility(View.GONE);
			isLoaded = true;
		}
		
	}
	
	/**
	 * 联系人适配器
	 * @author huanghui1
	 * @update 2014年10月11日 下午10:10:14
	 */
	class ContactAdapter extends CommonAdapter<User> implements SectionIndexer {
		//listview头部的特殊分类数量
		int headCount = 2;
		/**
		 * 新的好友
		 */
		private static final int TYPE_NEW_FRIEND = 0;
		/**
		 * 群聊
		 */
		private static final int TYPE_GROUP_CHAT = 1;
		/**
		 * 好友列表
		 */
		private static final int TYPE_CONTACT = 2;
		
		private ImageLoader imageLoader = ImageLoader.getInstance();
		
		DisplayImageOptions options = SystemUtil.getGeneralImageOptions();

		public ContactAdapter(List<User> list, Context context) {
			super(list, context);
		}
		
		/**
		 * 返回头部的数量
		 * @return
		 * @update 2015年8月20日 下午5:00:41
		 */
		private int getHeadCount() {
			return headCount;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			int type = getItemViewType(position);
			if (convertView == null) {
				holder = new ViewHolder();
				
				switch (type) {
				case TYPE_NEW_FRIEND:	//新的朋友
				case TYPE_GROUP_CHAT:	//群聊
					convertView = inflater.inflate(R.layout.layout_contact_head, parent, false);
					break;
				case TYPE_CONTACT:	//联系人
					convertView = inflater.inflate(R.layout.item_contact, parent, false);
					holder.tvCatalog = (TextView) convertView.findViewById(R.id.tv_catalog);
					break;
				}
				
				holder.tvName = (TextView) convertView.findViewById(R.id.tv_name);
				holder.ivIcon = (ImageView) convertView.findViewById(R.id.iv_head_icon);
				
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			switch (type) {
			case TYPE_NEW_FRIEND:	//新的朋友
				holder.tvName.setText(R.string.contact_list_new_friend);
				holder.ivIcon.setImageResource(R.drawable.contact_new_friend);
				break;
			case TYPE_GROUP_CHAT:	//群聊
				holder.tvName.setText(R.string.contact_list_group_chat);
				holder.ivIcon.setImageResource(R.drawable.contact_group_chat);
				break;
			case TYPE_CONTACT:	//联系人
				final User user = (User) getItem(position);
				if (user != null) {
					String sortLetter = user.getSortLetter();
					final UserVcard userVcard = user.getUserVcard();
					holder.tvName.setText(user.getName());
					
					showIcon(userVcard, holder.ivIcon);
					
					//根据position获取分类的首字母的Char ascii值
					int section = getSectionForPosition(position);
					if (position == getPositionForSection(section)) {
						holder.tvCatalog.setVisibility(View.VISIBLE);
						holder.tvCatalog.setText(sortLetter);
					} else {
						holder.tvCatalog.setVisibility(View.GONE);
					}
				}
				
				break;
			}
			
			return convertView;
		}
		
		/**
		 * 显示用户头像
		 * @param userVcard
		 * @param imageView
		 * @update 2015年8月20日 下午3:01:42
		 */
		private void showIcon(UserVcard userVcard, ImageView imageView) {
			if (userVcard != null) {
				String iconPath = userVcard.getIconShowPath();
				if (SystemUtil.isFileExists(iconPath)) {
					String imageUri = Scheme.FILE.wrap(iconPath);
					imageLoader.displayImage(imageUri, imageView, options);
				} else {
					imageLoader.displayImage(Scheme.DRAWABLE.wrap(String.valueOf(R.drawable.contact_head_icon_default)), imageView, options);
				}
			} else {
				imageLoader.displayImage(Scheme.DRAWABLE.wrap(String.valueOf(R.drawable.contact_head_icon_default)), imageView, options);
			}
		}

		@Override
		public Object[] getSections() {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public Object getItem(int position) {
			if (position < headCount) {
				return null;
			} else {
				return list.get(position - headCount);
			}
		}
		
		@Override
		public int getItemViewType(int position) {
			int type = TYPE_CONTACT;
			switch (position) {
			case 0:	//新的朋友
				type = TYPE_NEW_FRIEND;
				break;
			case 1:	//群聊
				type = TYPE_GROUP_CHAT;
				break;
			default:
				type = TYPE_CONTACT;
				break;
			}
			return type;
		}

		@Override
		public int getViewTypeCount() {
			return headCount + 1;
		}

		@Override
		public int getCount() {
			return list.size() + headCount;
		}

		@Override
		public int getPositionForSection(int sectionIndex) {
			if (sectionIndex == SystemUtil.getContactListFirtSection()) {
				return 0;
			} else {
				for (int i = headCount; i < getCount(); i++) {
					String sortStr = list.get(i - headCount).getSortLetter();
					char fisrtChar = sortStr.charAt(0);
					if (fisrtChar == sectionIndex) {
						return i;
					}
				}
				return -1;
			}
		}

		/*
		 * 根据ListView的当前位置获取分类的首字母的Char ascii值
		 */
		@Override
		public int getSectionForPosition(int position) {
			if (position < headCount) {
				return SystemUtil.getContactListFirtSection();
			} else {
				return list.get(position - headCount).getSortLetter().charAt(0);
			}
		}
		
	}
	
	private final class ViewHolder {
		TextView tvCatalog;
		TextView tvName;
		ImageView ivIcon;
	}
	
	/**
	 * 加载数据完成后的广播
	 * @author huanghui1
	 * @update 2014年10月23日 下午3:39:25
	 */
	/*public class LoadDataBroadcastReceiver extends BroadcastReceiver {
		public static final String ACTION_USER_LIST = "net.ibaixin.chat.USER_LIST_RECEIVER";
		public static final String ACTION_USER_INFOS = "net.ibaixin.chat.USER_INFOS_RECEIVER";
		public static final String ACTION_USER_ADD = "net.ibaixin.chat.USER_ADD_RECEIVER";
		public static final String ACTION_USER_UPDATE = "net.ibaixin.chat.USER_UPDATE_RECEIVER";

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if (intent != null) {
				User user = null;
				String action = intent.getAction();
				switch (action) {
				case ACTION_USER_LIST:	//更新好友列表
				case ACTION_USER_INFOS:	//从网上更新好友列表信息到本地数据库
					if (isLoaded) {	//只有已经加载过数据并在界面上显示了才响应service发过来的广播
						new LoadDataTask().execute();
					}
					break;
				case ACTION_USER_ADD:	//列表中添加一个好友信息
					user = intent.getParcelableExtra(UserInfoActivity.ARG_USER);
					if (user != null) {
						mUsers.add(user);
						if (mAdapter == null) {
							mAdapter= new ContactAdapter(mUsers, mContext);
							lvContact.setAdapter(mAdapter);
						} else {
							mAdapter.notifyDataSetChanged();
						}
					}
					break;
				case ACTION_USER_UPDATE:	//列表中更新好友
					user = intent.getParcelableExtra(UserInfoActivity.ARG_USER);
					if (user != null) {
						if (mUsers.contains(user)) {
							mUsers.remove(user);
						}
						mUsers.add(user);
						Collections.sort(mUsers, user);
						if (mAdapter == null) {
							mAdapter= new ContactAdapter(mUsers, mContext);
							lvContact.setAdapter(mAdapter);
						} else {
							mAdapter.notifyDataSetChanged();
						}
					}
					break;
				default:
					break;
				}
			}
		}
	}*/
	
	/**
	 * 好友信息的观察者
	 * @author huanghui1
	 * @version 1.0.0
	 * @update 2015年7月29日 下午8:40:07
	 */
	private class UserContentObserver extends net.ibaixin.chat.util.ContentObserver {

		public UserContentObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void update(Observable<?> observable, int notifyFlag, NotifyType notifyType, Object data) {
			Log.d("----UserContentObserver---update---isLoaded--" + isLoaded + "------notifyFlag------" + notifyFlag + "---notifyType----" + notifyType + "---data---" + data);
			if (isLoaded) {
				switch (notifyFlag) {
				case Provider.UserColumns.NOTIFY_FLAG:	//好友信息的通知
					User user = null;
					switch (notifyType) {
					case ADD:	//添加单个好友
						if (data != null) {
							user = (User) data;
							mUsers.add(user);
							Collections.sort(mUsers, user);
							if (mAdapter == null) {
								mAdapter= new ContactAdapter(mUsers, mContext);
								lvContact.setAdapter(mAdapter);
							} else {
								mAdapter.notifyDataSetChanged();
							}
						}
						break;
					case UPDATE:	//单个好友信息更新
						if (data != null) {
							user = (User) data;
							if (mUsers.contains(user)) {
								mUsers.remove(user);
							}
							mUsers.add(user);
							Collections.sort(mUsers, user);
							if (mAdapter == null) {
								mAdapter= new ContactAdapter(mUsers, mContext);
								lvContact.setAdapter(mAdapter);
							} else {
								mAdapter.notifyDataSetChanged();
							}
						}
						break;
					case BATCH_UPDATE:	//批量更新好友信息
						if (data != null) {
							List<User> tmpList = (List<User>) data;
							mUsers.clear();
							mUsers.addAll(tmpList);
							if (mAdapter == null) {
								mAdapter= new ContactAdapter(mUsers, mContext);
								lvContact.setAdapter(mAdapter);
							} else {
								mAdapter.notifyDataSetChanged();
							}
						} else {
							if (isLoaded) {	//只有已经加载过数据并在界面上显示了才响应service发过来的广播
								new LoadDataTask().execute();
							}
						}
						break;
					default:
						break;
					}
					break;
				case Provider.UserVcardColumns.NOTIFY_FLAG:	//好友电子名片信息的通知
					switch (notifyType) {
					case ADD:	//添加好友的电子名片信息
					case UPDATE:	//更新好友的电子名片信息
						if (data != null) {
							UserVcard userVcard = (UserVcard) data;
							updateUserInfo(userVcard);
						}
						break;
					case BATCH_UPDATE:	//批量更新
						if (data != null) {
							Map<String, UserVcard> userVcards = (Map<String, UserVcard>) data;
							if (!SystemUtil.isEmpty(userVcards)) {
								updateUserInfos(userVcards);
							}
						}
						break;
					default:
						break;
					}
					break;
				default:
					break;
				}
			}
		}
		
	}
	
	/**
	 * 根据用户id来重新设置好友信息
	 * @param userVcard 好友的电子名片信息
	 * @update 2015年7月29日 下午9:45:11
	 */
	private void updateUserInfo(final UserVcard userVcard) {
		SystemUtil.getCachedThreadPool().execute(new Runnable() {
			
			@Override
			public void run() {
				int position = -1;
				int userCount = mUsers.size();
				User targetUser = null;
				boolean clearCache = false;
				for (int i = 0; i < userCount; i++) {
					User user = mUsers.get(i);
					UserVcard vcard = user.getUserVcard();
					if (vcard == null) {
						vcard = new UserVcard();
						vcard.setUserId(user.getId());
					}
					if (vcard.equals(userVcard)) {
						position = i;
						targetUser = user;
						String newNick = userVcard.getNickname();
						vcard.setNickname(newNick);
						String oldHash = vcard.getIconHash();
						String newHash = userVcard.getIconHash();
						if (oldHash != null) {	//之前有头像
							if (!oldHash.equals(newHash)) {	//头像有改变
								clearCache = true;
								vcard.setIconHash(newHash);
							}
						} else {
							clearCache = true;
						}
						vcard.setIconPath(userVcard.getIconPath());
						vcard.setThumbPath(userVcard.getThumbPath());
						break;
					}
				}
				
				if (targetUser != null) {
					
	            	if (mAdapter != null) {
	            		Message msg = mHandler.obtainMessage();
						msg.what = Constants.MSG_UPDATE_ONE;
						msg.obj = targetUser;
						msg.arg1 = position;
						mHandler.sendMessage(msg);
	            	}
					
				}
				
				/*if (!refreshOne) {	//全局刷新
					Collections.sort(mUsers, new User());
					ContactAdapter contactAdapter = (ContactAdapter) lvContact.getAdapter();
	            	if (contactAdapter != null) {
	            		contactAdapter.setClearIconCache(clearCache);
	            	}
					//刷新界面
					mHandler.sendEmptyMessage(Constants.MSG_UPDATE_SUCCESS);
				} else {	//局部刷新
*/					
//				}
				
			}
		});
	}
	
	/**
	 * 局部更新adapter
	 * @param position 要更新的索引位置
	 * @param user 要更新的实体对象
	 * @update 2015年8月20日 下午2:54:22
	 */
	private void updateView(int position, User user) {
		//得到第一个可显示控件的位置，  
        int visiblePosition = lvContact.getFirstVisiblePosition();
        //只有当要更新的view在可见的位置时才更新，不可见时，跳过不更新 
        int relativePosition = position - visiblePosition + mAdapter.getHeadCount();
        if (mAdapter != null) {
        	if (relativePosition >= 0) {
        		//得到要更新的item的view  
        		View view = lvContact.getChildAt(relativePosition);
        		//从view中取得holder  
        		Object tag = view.getTag();
        		if (tag != null && tag instanceof ViewHolder) {
        			ViewHolder holder = (ViewHolder) tag;
        			holder.tvName.setText(user.getName());
        			UserVcard userVcard = user.getUserVcard();
        			mAdapter.showIcon(userVcard, holder.ivIcon);
        		}
        	}
        }
	}
	
	/**
	 * 更新一组用户电子名片信息
	 * @param userVcards username为key， uservcard为value的map
	 * @update 2015年8月4日 上午10:45:32
	 */
	private void updateUserInfos(final Map<String, UserVcard> userVcards) {
		SystemUtil.getCachedThreadPool().execute(new Runnable() {
			
			@Override
			public void run() {
				Set<String> keys = userVcards.keySet();
				for (String username : keys) {
					UserVcard vcard = userVcards.get(username);
					if (vcard != null) {
						User user = new User();
						user.setUsername(username);
						user.setId(vcard.getId());
						
						int index = mUsers.indexOf(user);
						if (index != -1) {
							//集合中的user
							User originUser = mUsers.get(index);
							UserVcard originVcard = originUser.getUserVcard();
							if (originVcard != null) {
								originVcard.setNickname(vcard.getNickname());
								originVcard.setIconHash(vcard.getIconHash());
								originVcard.setIconPath(vcard.getIconPath());
								originVcard.setThumbPath(vcard.getThumbPath());
							} else {	//原来没有电子名片信息就设置
								originUser.setUserVcard(vcard);
							}
						}
					}
				}
				Collections.sort(mUsers, new User());
				//刷新界面
				mHandler.sendEmptyMessage(Constants.MSG_UPDATE_SUCCESS);
			}
		});
	}

	@Override
	public void onload() {
		// TODO Auto-generated method stub
		initData();
	}
	
}
