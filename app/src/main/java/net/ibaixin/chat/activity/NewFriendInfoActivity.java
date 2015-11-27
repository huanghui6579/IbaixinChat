package net.ibaixin.chat.activity;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.download.ImageDownloader.Scheme;

import net.ibaixin.chat.R;
import net.ibaixin.chat.loader.NewFriendInfoLoader;
import net.ibaixin.chat.manager.UserManager;
import net.ibaixin.chat.model.ContextMenuItem;
import net.ibaixin.chat.model.NewFriendInfo;
import net.ibaixin.chat.model.NewFriendInfo.FriendStatus;
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
import net.ibaixin.chat.view.adapter.MenuItemAdapter;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;

import java.util.ArrayList;
import java.util.List;

/**
 * 新的朋友信息列表
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年11月10日 下午3:34:41
 */
public class NewFriendInfoActivity extends BaseActivity implements LoaderCallbacks<List<NewFriendInfo>> {
	
	private ImageLoader mImageLoader = ImageLoader.getInstance();
	
	private ListView lvNewInfos;
	private View emptyView;
	private ProgressWheel pbLoading;
	
	private List<NewFriendInfo> mNewInfos = new ArrayList<>();
	private NewFriendAdapter mNewFriendAdapter;
	
	private UserManager userManager = UserManager.getInstance();
	
	ProgressDialog pDialog;
	
	/**
	 * 更新数据库后是否自动刷新，只有删除才不会自动刷新
	 */
//	private boolean autoRefresh = true;
	
	private NewFriendInfoObserver newFriendInfoObserver;
	
	private Handler mHandler = new MyHandler();
	
	private MaterialDialog mMaterialDialog;
	
	@Override
	protected int getContentView() {
		return R.layout.activity_new_friend_info_list;
	}

	@Override
	protected void initView() {
		lvNewInfos = (ListView) findViewById(R.id.lv_new_friend_info);
		emptyView = findViewById(R.id.empty_view);
		pbLoading = (ProgressWheel) findViewById(R.id.pb_loading);
	}

	@Override
	protected void initData() {
		registerContentOberver();
		getSupportLoaderManager().initLoader(0, null, this);
		
//		registerForContextMenu(lvNewInfos);
	}

	@Override
	protected void addListener() {
		lvNewInfos.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				final NewFriendInfo newInfo = mNewInfos.get(position);
				final String title = newInfo.getTitle();
				if (mMaterialDialog == null) {
					final List<ContextMenuItem> menuItems = getContextMenuItems();
					MaterialDialog.Builder builder = new MaterialDialog.Builder(mContext);
					mMaterialDialog = builder.title(title)
							.adapter(new MenuItemAdapter(menuItems, mContext), new MaterialDialog.ListCallback() {
								@Override
								public void onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
									ContextMenuItem menuItem = menuItems.get(which);
									switch (menuItem.getItemId()) {
										case R.string.menu_delete:    //删除
											delteNewInfo(newInfo);
											break;
									}
									mMaterialDialog.dismiss();
								}
							}).build();
				} else {
					mMaterialDialog.setTitle(title);
				}
				mMaterialDialog.show();
				return true;
			}
		});
	}
	
	/**
	 * 删除新的好友信息
	 * @param newInfo 新的好友信息
	 * @author huanghui1
	 * @update 2015/11/27 10:55
	 * @version: 0.0.1
	 */
	private void delteNewInfo(final NewFriendInfo newInfo) {
		SystemUtil.getCachedThreadPool().execute(new Runnable() {
			@Override
			public void run() {
				boolean flag = userManager.deleteNewFriendInfo(newInfo.getId());
				if (flag) {    //删除成功
					SystemUtil.deleteFile(newInfo.getIconPath());
					mNewInfos.remove(newInfo);
					mHandler.sendEmptyMessage(Constants.MSG_DELETE_SUCCESS);
				} else {
					mHandler.sendEmptyMessage(Constants.MSG_DELETE_FAILED);
				}
			}
		});
	}
	
	/**
	 * 获取新的好友列表的长按菜单
	 * @author huanghui1
	 * @update 2015/11/27 10:43
	 * @version: 0.0.1
	 * @return 返回列表的长按菜单
	 */
	public List<ContextMenuItem> getContextMenuItems() {
		List<ContextMenuItem> list = new ArrayList<>();
		ContextMenuItem item = new ContextMenuItem(R.string.menu_delete, R.string.menu_delete);
		list.add(item);
		return list;
	}

	@Override
	public Loader<List<NewFriendInfo>> onCreateLoader(int id, Bundle args) {
		return new NewFriendInfoLoader(mContext);
	}
	
	/**
	 * 注册观察者
	 * @update 2014年11月7日 下午10:05:30
	 */
	private void registerContentOberver() {
//		NewFriendInfoContentObserver newFriendObserver = new NewFriendInfoContentObserver(mHandler);
//		mContext.getContentResolver().registerContentObserver(Provider.NewFriendColumns.CONTENT_URI, true, newFriendObserver);
		newFriendInfoObserver = new NewFriendInfoObserver(mHandler);
		userManager.addObserver(newFriendInfoObserver);
	}

	@Override
	public void onLoadFinished(Loader<List<NewFriendInfo>> loader,
			List<NewFriendInfo> data) {
		if (mNewFriendAdapter == null) {
			mNewFriendAdapter = new NewFriendAdapter(mNewInfos, mContext);
			lvNewInfos.setAdapter(mNewFriendAdapter);
		}
		mNewInfos.clear();
		if (!SystemUtil.isEmpty(data)) {
			mNewInfos.addAll(data);
		}
		if (lvNewInfos.getEmptyView() == null) {
			lvNewInfos.setEmptyView(emptyView);
		}
		mNewFriendAdapter.notifyDataSetChanged();
		if (pbLoading.getVisibility() == View.VISIBLE) {
			pbLoading.setVisibility(View.GONE);
		}
	}

	@Override
	public void onLoaderReset(Loader<List<NewFriendInfo>> loader) {
		if (mNewFriendAdapter != null) {
			mNewFriendAdapter.swapData(null);
		}
	}
	
	@Override
	protected void onDestroy() {
		if (newFriendInfoObserver != null) {
			userManager.removeObserver(newFriendInfoObserver);
		}
		super.onDestroy();
	}
	
	/**
	 * 新的朋友信息适配器
	 * @author huanghui1
	 * @update 2014年11月10日 下午4:10:37
	 */
	class NewFriendAdapter extends CommonAdapter<NewFriendInfo> {
		DisplayImageOptions options = SystemUtil.getGeneralImageOptions();

		public NewFriendAdapter(List<NewFriendInfo> list, Context context) {
			super(list, context);
		}

		/**
		 * 包装数据
		 * @update 2014年11月10日 下午4:45:32
		 * @param data
		 */
		public void swapData(List<NewFriendInfo> data) {
			list.clear();
			if (data != null) {
				list.addAll(data);
			}
			notifyDataSetChanged();
		}
		
		@SuppressWarnings("deprecation")
		@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			NewInfoViewHolder holder = null;
			if (convertView == null) {
				holder = new NewInfoViewHolder();
				convertView = inflater.inflate(R.layout.item_new_friend_info, parent, false);
				
				holder.ivHeadIcon = (ImageView) convertView.findViewById(R.id.iv_head_icon);
				holder.tvTitle = (TextView) convertView.findViewById(R.id.tv_title);
				holder.tvContent = (TextView) convertView.findViewById(R.id.tv_content);
				holder.tvState = (TextView) convertView.findViewById(R.id.tv_state);
				
				convertView.setTag(holder);
			} else {
				holder = (NewInfoViewHolder) convertView.getTag();
			}
			final NewFriendInfo newInfo = list.get(position);
			
			User user = newInfo.getUser();
			String iconPath = null;
			if (user != null) {
				UserVcard uCard = user.getUserVcard();
				if (uCard != null) {
					iconPath = uCard.getIconPath();
				} else {
					iconPath = newInfo.getIconPath();
				}
			} else {
				iconPath = newInfo.getIconPath();
			}
			if (SystemUtil.isFileExists(iconPath)) {
				mImageLoader.displayImage(Scheme.FILE.wrap(iconPath), holder.ivHeadIcon, options);
			} else {
				mImageLoader.displayImage(null, holder.ivHeadIcon, options);
			}
			String title = newInfo.getTitle();
			holder.tvTitle.setText(title);
			holder.tvContent.setText(newInfo.getContent());
			final FriendStatus friendStatus = newInfo.getFriendStatus();
			holder.tvState.setText(friendStatus.getTitle());
			switch (friendStatus) {
			case UNADD:	//还未添加，则显示“添加”的按钮样式
			case ACCEPT:	//别人请求添加自己为好友，则显示“接受”的样式
				holder.tvState.setTextColor(getResources().getColor(android.R.color.white));
				holder.tvState.setBackgroundResource(R.drawable.common_button_green_selector);
				holder.tvState.setClickable(true);
				holder.tvState.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						//接受对方添加自己为好友,并且也将对方添加为自己的好友
						pDialog = ProgressDialog.show(context, null, getString(R.string.loading), true);
						acceptFriend(newInfo);
					}
				});
				break;
			default:
				holder.tvState.setTextColor(getResources().getColor(R.color.session_list_item_content));
				if (SystemUtil.getCurrentSDK() >= Build.VERSION_CODES.JELLY_BEAN) {
					holder.tvState.setBackground(null);
				} else {
					holder.tvState.setBackgroundDrawable(null);
				}
				holder.tvState.setClickable(false);
				break;
			}
			return convertView;
		}
		
	}
	
	/**
	 * 接受对方添加自己为好友,并且也将对方添加为自己的好友
	 * @param newInfo 新的好友信息
	 * @author huanghui1
	 * @update 2015/11/27 11:05
	 * @version: 0.0.1
	 */
	private void acceptFriend(final NewFriendInfo newInfo) {
		SystemUtil.getCachedThreadPool().execute(new Runnable() {

			@Override
			public void run() {
				final FriendStatus friendStatus = newInfo.getFriendStatus();
				Message msg = mHandler.obtainMessage();
				AbstractXMPPConnection connection = XmppConnectionManager.getInstance().getConnection();
				try {
					String otherJid = SystemUtil.wrapJid(newInfo.getFrom());
					if(friendStatus == FriendStatus.UNADD) {//update by dudejin
						XmppUtil.addFriend(connection, otherJid);
					} else {
//										XmppUtil.updatePresenceType(connection, Presence.Type.subscribe);
						XmppUtil.acceptFriend(connection, otherJid);
						XmppUtil.addEntry(connection, otherJid, newInfo.getNickname(), null);
					}
					//添加该好友
					User user = XmppUtil.getUserEntry(connection, newInfo.getFrom());
					if (user != null) {
						UserVcard uCard = new UserVcard();
						uCard.setThumbPath(newInfo.getIconPath());
						uCard.setIconHash(newInfo.getIconHash());
						user.setUserVcard(uCard);
						if(friendStatus == FriendStatus.ACCEPT) {//接收别人添加我为好友的时候才保存
							user = userManager.saveOrUpdateFriend(user);
						}
						newInfo.setUser(user);
						if(friendStatus == FriendStatus.ACCEPT) {
							newInfo.setFriendStatus(FriendStatus.ADDED);
						} else {
							newInfo.setFriendStatus(FriendStatus.VERIFYING);
						}
						newInfo.setTitle(user.getUsername());
						newInfo.setContent(user.getName());
						userManager.updateNewFriendInfoState(newInfo);
						//通知好友列表更新好友
//										Intent intent = new Intent(LoadDataBroadcastReceiver.ACTION_USER_LIST);
//										sendBroadcast(intent);

						msg.what = Constants.MSG_SUCCESS;
						//更改状态
					} else {
						msg.what = Constants.MSG_FAILED;
					}
				} catch (NotConnectedException | NotLoggedInException | NoResponseException | XMPPErrorException e) {
					msg.what = Constants.MSG_FAILED;
					Log.e(e.getMessage());
				}
				mHandler.sendMessage(msg);
			}
		});
	}
	
	/**
	 * 重新加载数据
	 * @update 2014年11月7日 下午10:01:23
	 *//*
	@Deprecated
	private void reLoadData() {
		getSupportLoaderManager().restartLoader(0, null, this);
	}*/
	
	final class NewInfoViewHolder {
		ImageView ivHeadIcon;
		TextView tvTitle;
		TextView tvContent;
		TextView tvState;
	}
	
	/**
	 * 新的朋友信息的观察者
	 * @author huanghui1
	 * @update 2015年3月10日 下午2:05:33
	 */
	class NewFriendInfoObserver extends net.ibaixin.chat.util.ContentObserver {
		
		public NewFriendInfoObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void update(Observable<?> observable, int notifyFlag, NotifyType notifyType,
				Object data) {
			if (Provider.NewFriendColumns.NOTIFY_FLAG == notifyFlag) {
				NewFriendInfo newInfo = null;
				try {
					newInfo = (NewFriendInfo) data;
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (newInfo == null) {
					return;
				}
				switch (notifyType) {
				case ADD:	//添加
					mNewInfos.add(0, newInfo);
					break;
//				case DELETE:	//删除
//					mNewInfos.remove(newInfo);
//					break;
				case UPDATE:
					if (mNewInfos.contains(newInfo)) {
						mNewInfos.remove(newInfo);
					}
					mNewInfos.add(0, newInfo);
					break;
				default:
					break;
				}
				mNewFriendAdapter.notifyDataSetChanged();
			}
		}
	}
	
	public class MyHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			if (pDialog != null && pDialog.isShowing()) {
				pDialog.dismiss();
			}
			switch (msg.what) {
			case Constants.MSG_SUCCESS:	//操作成功
				SystemUtil.makeShortToast(R.string.add_success);
				break;
			case Constants.MSG_FAILED:	//操作失败
				SystemUtil.makeShortToast(R.string.add_failed);
				break;
			case Constants.MSG_DELETE_SUCCESS:	//删除成功
				if (mNewFriendAdapter != null) {
					mNewFriendAdapter.notifyDataSetChanged();
				}
				break;
			case Constants.MSG_DELETE_FAILED:	//删除失败
				SystemUtil.makeShortToast(R.string.delete_failed);
				break;
			default:
				break;
			}
		}
		
	}
	
}
