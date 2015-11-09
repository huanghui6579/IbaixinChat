package net.ibaixin.chat.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
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
import net.ibaixin.chat.activity.ChatActivity;
import net.ibaixin.chat.activity.CommonAdapter;
import net.ibaixin.chat.loader.ThreadListLoader;
import net.ibaixin.chat.manager.MsgManager;
import net.ibaixin.chat.manager.UserManager;
import net.ibaixin.chat.model.MsgInfo;
import net.ibaixin.chat.model.MsgThread;
import net.ibaixin.chat.model.User;
import net.ibaixin.chat.model.UserVcard;
import net.ibaixin.chat.provider.Provider;
import net.ibaixin.chat.receiver.NetworkReceiver;
import net.ibaixin.chat.receiver.NetworkReceiver.NetworkChangeCallback;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.ImageUtil;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.Observable;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.view.ProgressDialog;
import net.ibaixin.chat.view.ProgressWheel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 聊天会话列表
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年10月8日 下午7:36:50
 */
public class ThreadListFragment extends BaseFragment implements LoaderCallbacks<List<MsgThread>>, NetworkChangeCallback {
	private static final int MENU_TOP = 0;
	private static final int MENU_DELETE = 0x1;
	
	private ImageLoader mImageLoader = ImageLoader.getInstance();
	
	private ListView mListView;
	private ProgressWheel pbLoading;
	private View emptyView;
	private View netErrorView;
	
	private MsgManager msgManager = MsgManager.getInstance();
	
	/**
	 * 是否需要listview重设置adapter,一般用在fragment的stop后载onresume时需要
	 */
	private boolean resetAdapter = false;
	
	private ProgressDialog pDialog;
	
	/**
	 * 会话集合
	 */
	private List<MsgThread> mMsgThreads = new ArrayList<>();
	private MsgThreadAdapter mThreadAdapter;
	
	private MsgThreadContentObserver threadContentObserver;
	
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if (pDialog != null && pDialog.isShowing()) {
				pDialog.dismiss();
			}
			switch (msg.what) {
			case Constants.MSG_SUCCESS:	//会话删除成功
				mThreadAdapter.notifyDataSetChanged();
				break;
			case Constants.MSG_FAILED:	//删除失败
				SystemUtil.makeShortToast(R.string.delete_failed);
				break;
			case Constants.MSG_THREAD_TOP_SUCCESS:	//会话置顶/取消置顶成功
				mThreadAdapter.notifyDataSetChanged();
				break;
			case Constants.MSG_THREAD_TOP_FAILED:	//会话置顶/取消置顶失败
				SystemUtil.makeShortToast(R.string.opt_failed);
				break;
			case Constants.MSG_UPDATE_ONE:	//局部更新
				MsgThread targetThread = (MsgThread) msg.obj;
				if (targetThread != null) {
					int position = msg.arg1;
					updateView(position, targetThread);
				}
				break;
			default:
				break;
			}
		}
	};
	
	/**
	 * 初始化fragment
	 * @update 2014年10月8日 下午10:09:08
	 * @return
	 */
	public static ThreadListFragment newInstance() {
		ThreadListFragment fragment = new ThreadListFragment();
		return fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		NetworkReceiver.attachNetworkCallback(this);
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		
		NetworkReceiver.detachNetworkCallback(this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_session_list, container, false);
		mListView = (ListView) view.findViewById(R.id.lv_session);
		emptyView = view.findViewById(R.id.empty_view);
		pbLoading = (ProgressWheel) view.findViewById(R.id.pb_loading);
		netErrorView = view.findViewById(R.id.tv_tip);
		
		//注册会话观察者
		registerContentOberver();
		
		return view;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		netErrorView.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Settings.ACTION_SETTINGS);
				if (intent.resolveActivity(mContext.getPackageManager()) != null) {
					startActivity(intent);
				}
			}
		});
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				MsgThread msgThread = mMsgThreads.get(position);
				Intent intent = new Intent(mContext, ChatActivity.class);
				intent.putExtra(ChatActivity.ARG_THREAD, msgThread);
				startActivity(intent);
			}
		});
		mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				final MsgThread thread = (MsgThread) mThreadAdapter.getItem(position);
				if (thread != null) {
					String[] menuArray = getResources().getStringArray(R.array.thread_list_context_menu);
					if (thread.isTop()) {	//已经置顶了，就取消置顶
						menuArray[0] = getString(R.string.thread_list_context_menu_top_cancel);
					}
					/*AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
					builder.setTitle(thread.getMsgThreadName())
							.setItems(menuArray, new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									switch (which) {
									case MENU_TOP:	//置顶/取消置顶该聊天
										pDialog = ProgressDialog.show(mContext, null, getString(R.string.loading));
										SystemUtil.getCachedThreadPool().execute(new Runnable() {
											
											@Override
											public void run() {
												thread.setTop(!thread.isTop());
												boolean success = msgManager.updateMsgThreadTop(thread);
												Collections.sort(mMsgThreads, thread);
												if (success) {
													mHandler.sendEmptyMessage(Constants.MSG_THREAD_TOP_SUCCESS);
												} else {
													mHandler.sendEmptyMessage(Constants.MSG_THREAD_TOP_FAILED);
												}
											}
										});
										break;
									case MENU_DELETE:	//删除该聊天会话
										AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
										builder.setTitle(R.string.prompt)
												.setMessage(getString(R.string.contact_list_content_delete_prompt, thread.getMsgThreadName()))
												.setNegativeButton(android.R.string.cancel, null)
												.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
													
													@Override
													public void onClick(DialogInterface dialog, int which) {
														pDialog = ProgressDialog.show(mContext, null, getString(R.string.loading));
														SystemUtil.getCachedThreadPool().execute(new Runnable() {
															
															@Override
															public void run() {
																boolean success = msgManager.deleteMsgThreadById(thread.getId());
																if (success) {	//
																	mMsgThreads.remove(thread);
																	mHandler.sendEmptyMessage(Constants.MSG_SUCCESS);
																} else {
																	mHandler.sendEmptyMessage(Constants.MSG_FAILED);
																}
															}
														});
													}
												}).show();
										break;
									default:
										break;
									}
								}
							}).show();*/
					MaterialDialog.Builder builder = new MaterialDialog.Builder(mContext);
					builder.title(thread.getMsgThreadName())
						.items(menuArray)
						.itemsCallback(new MaterialDialog.ListCallback() {
							
							@Override
							public void onSelection(MaterialDialog dialog, View itemView, int which,
									CharSequence text) {
								switch (which) {
								case MENU_TOP:	//置顶/取消置顶该聊天
									pDialog = ProgressDialog.show(mContext, null, getString(R.string.loading));
									SystemUtil.getCachedThreadPool().execute(new Runnable() {
										
										@Override
										public void run() {
											thread.setTop(!thread.isTop());
											boolean success = msgManager.updateMsgThreadTop(thread);
											Collections.sort(mMsgThreads, thread);
											if (success) {
												mHandler.sendEmptyMessage(Constants.MSG_THREAD_TOP_SUCCESS);
											} else {
												mHandler.sendEmptyMessage(Constants.MSG_THREAD_TOP_FAILED);
											}
										}
									});
									break;
								case MENU_DELETE:	//删除该聊天会话
									MaterialDialog.Builder builder = new MaterialDialog.Builder(mContext);
									builder.title(R.string.prompt)
										.content(R.string.contact_list_content_delete_prompt, thread.getMsgThreadName())
										.positiveText(android.R.string.ok)
										.negativeText(android.R.string.cancel)
										.callback(new MaterialDialog.ButtonCallback() {

											@Override
											public void onPositive(
													MaterialDialog dialog) {
												pDialog = ProgressDialog.show(mContext, null, getString(R.string.loading));
												SystemUtil.getCachedThreadPool().execute(new Runnable() {
													
													@Override
													public void run() {
														boolean success = msgManager.deleteMsgThreadById(thread.getId());
														if (success) {	//
															mMsgThreads.remove(thread);
															mHandler.sendEmptyMessage(Constants.MSG_SUCCESS);
														} else {
															mHandler.sendEmptyMessage(Constants.MSG_FAILED);
														}
													}
												});
											}
											
										}).show();
									
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
//		mThreadAdapter = new MsgThreadAdapter(mMsgThreads, mContext);
//		mListView.setAdapter(mThreadAdapter);
		getLoaderManager().initLoader(0, null, this);
	}
	
	@Override
	public void onStop() {
		resetAdapter = true;
		super.onStop();
	}
	
	@Override
	public void onDestroyView() {
		if (threadContentObserver != null) {
			msgManager.removeObserver(threadContentObserver);
			
			UserManager.getInstance().removeObserver(threadContentObserver);
		}
		getLoaderManager().destroyLoader(0);
		super.onDestroyView();
	}
	
	/**
	 * 注册会话观察者
	 * @update 2014年11月7日 下午10:05:30
	 */
	private void registerContentOberver() {
		threadContentObserver = new MsgThreadContentObserver(mHandler);
//		mContext.getContentResolver().registerContentObserver(Provider.MsgThreadColumns.CONTENT_URI, true, msgContentObserver);
		msgManager.addObserver(threadContentObserver);
		
		UserManager.getInstance().addObserver(threadContentObserver);
	}
	
	/**
	 * 会话列表的适配器
	 * @author huanghui1
	 * @update 2014年10月31日 下午9:18:43
	 */
	class MsgThreadAdapter extends CommonAdapter<MsgThread> {
		
		DisplayImageOptions options = SystemUtil.getGeneralImageOptions();
		
		public MsgThreadAdapter(List<MsgThread> list, Context context) {
			super(list, context);
		}
		
		/**
		 * 包装数据
		 * @update 2014年11月1日 上午10:56:41
		 * @param data
		 */
		public void swapData(List<MsgThread> data) {
			list.clear();
			if (data != null) {
				list.addAll(data);
			}
			notifyDataSetChanged();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			MsgThreadViewHolder holder = null;
			if (convertView == null) {
				holder = new MsgThreadViewHolder();
				
				convertView = inflater.inflate(R.layout.item_msg_thread, parent, false);
				
				holder.itemThreadLayout = convertView.findViewById(R.id.item_thread_layout);
				holder.ivHeadIcon = (ImageView) convertView.findViewById(R.id.iv_head_icon);
				holder.tvTitle = (TextView) convertView.findViewById(R.id.tv_title);
				holder.tvTime = (TextView) convertView.findViewById(R.id.tv_time);
				holder.tvContent = (TextView) convertView.findViewById(R.id.tv_content);
				
				convertView.setTag(holder);
			} else {
				holder = (MsgThreadViewHolder) convertView.getTag();
			}
			
			final MsgThread msgThread = list.get(position);
			if (msgThread.isTop()) {	//该会话已置顶
				holder.itemThreadLayout.setBackgroundColor(getResources().getColor(R.color.primary_light_color));
			} else {
				holder.itemThreadLayout.setBackgroundColor(getResources().getColor(android.R.color.transparent));
			}
			holder.tvTitle.setText(msgThread.getMsgThreadName());
			holder.tvTime.setText(SystemUtil.formatMsgThreadTime(msgThread.getModifyDate()));
			String snippetContent = msgThread.getSnippetContent();
			
			MsgInfo lastMsg = msgThread.getLastMsgInfo();
			if (lastMsg != null) {
				MsgInfo.Type msgType = lastMsg.getMsgType();
				if (MsgInfo.Type.TEXT != msgType) {	//非文本消息
					int tipRes = 0;
					switch (msgType) {
					case IMAGE:	//图片消息
						tipRes = R.string.msg_thread_snippet_content_image;
						break;
					case AUDIO:	//音频文件
						tipRes = R.string.msg_thread_snippet_content_audio;
						break;
					case VIDEO:	//视频文件
						tipRes = R.string.msg_thread_snippet_content_video;
						break;
					case LOCATION:	//地理位置
						tipRes = R.string.msg_thread_snippet_content_location;
						break;
					case VOICE:	//语音信息
						tipRes = R.string.msg_thread_snippet_content_voice;
						break;
					case VCARD:	//电子名片信息
						tipRes = R.string.msg_thread_snippet_content_vcard;
						break;
					case FILE:	//文件信息
						tipRes = R.string.msg_thread_snippet_content_file;
						break;
					default:
						break;
					}
					if (tipRes != 0) {
						snippetContent = getString(tipRes, snippetContent);
					}
				}
			}
			
			holder.tvContent.setText(snippetContent);
			Drawable icon = msgThread.getIcon();
			final User member = msgThread.getMembers().get(0);
			if(member != null) {
				final UserVcard uCard = member.getUserVcard();
				if (uCard != null) {
					if (icon != null) {
						holder.ivHeadIcon.setImageDrawable(icon);
					} else {
						showIcon(uCard, holder.ivHeadIcon);
					}
				} else {
					mImageLoader.displayImage(null, holder.ivHeadIcon, options);
				}
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
					mImageLoader.displayImage(imageUri, imageView, options);
				} else {
					mImageLoader.displayImage(null, imageView, options);
				}
			} else {
				mImageLoader.displayImage(null, imageView, options);
			}
		}
		
	}
	
	private final class MsgThreadViewHolder {
		View itemThreadLayout;
		ImageView ivHeadIcon;
		TextView tvTime;
		TextView tvTitle;
		TextView tvContent;
	}
	
	@Override
	public Loader<List<MsgThread>> onCreateLoader(int id, Bundle args) {
		return new ThreadListLoader(mContext);
	}

	@Override
	public void onLoadFinished(Loader<List<MsgThread>> loader,
			List<MsgThread> data) {
		/*if (!SystemUtil.isEmpty(data)) {
			if (mThreadAdapter == null) {
				mMsgThreads.addAll(data);
				mThreadAdapter = new MsgThreadAdapter(mMsgThreads, mContext);
				mListView.setAdapter(mThreadAdapter);
				mListView.setEmptyView(emptyView);
			} else {
				if (resetAdapter) {
					mListView.setAdapter(mThreadAdapter);
					mListView.setEmptyView(emptyView);
				} else {
					mThreadAdapter.swapData(data);
				}
			}
		} else {
			mListView.setEmptyView(emptyView);
		}*/
		mMsgThreads.clear();
		boolean refresh = true;
		if (!SystemUtil.isEmpty(data)) {
			mMsgThreads.addAll(data);
		}
		if (mThreadAdapter == null) {
			mThreadAdapter = new MsgThreadAdapter(mMsgThreads, mContext);
			refresh = false;
		}
		if (resetAdapter) {
			mListView.setAdapter(mThreadAdapter);
			mListView.setEmptyView(emptyView);
		} else {
			if (mListView.getEmptyView() == null) {
				mListView.setEmptyView(emptyView);
			}
			if (!refresh) {
				mListView.setAdapter(mThreadAdapter);
			} else {
				mThreadAdapter.notifyDataSetChanged();
			}
		}
		pbLoading.setVisibility(View.GONE);
		
	}

	@Override
	public void onLoaderReset(Loader<List<MsgThread>> loader) {
		if (mThreadAdapter != null) {
			mThreadAdapter.swapData(null);
		}
	}
	
	/**
	 * 会话的内容观察者
	 * @author huanghui1
	 * @update 2014年11月7日 下午9:39:06
	 */
	class MsgThreadContentObserver extends net.ibaixin.chat.util.ContentObserver {

		public MsgThreadContentObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void update(Observable<?> observable, int notifyFlag, NotifyType notifyType,
				Object data) {
			MsgThread thread = null;
//			Log.d("---------MsgThreadContentObserver-----update-------notifyFlag------" + notifyFlag + "-updateType--" + updateType.toString() + "--data--" + data);
			switch (notifyFlag) {
			case Provider.MsgThreadColumns.NOTIFY_FLAG:	//聊天会话的通知
				try {
					thread = (MsgThread) data;
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (thread == null) {
					return;
				}
				switch (notifyType) {
				case ADD:	//添加
					mMsgThreads.add(0, thread);
					mThreadAdapter.notifyDataSetChanged();
					break;
				case UPDATE:	//更新
					if (mMsgThreads.contains(thread)) {
						mMsgThreads.remove(thread);
					}
					mMsgThreads.add(0, thread);
					Log.d("---call----Provider.MsgThreadColumns.NOTIFY_FLAG--UPDATE--thread--" + thread);
					mThreadAdapter.notifyDataSetChanged();
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
						final UserVcard userVcard = (UserVcard) data;
						SystemUtil.getCachedThreadPool().execute(new Runnable() {
							
							@Override
							public void run() {
								Log.d("--call----Provider.UserVcardColumns-----UPDATE---userVcard---" + userVcard);
								updateUserInfo(userVcard);
							}
						});
					}
					break;
				case BATCH_UPDATE:	//批量更新
					if (data != null) {
						//username为key，vcard为value
						final Map<String, UserVcard> userVcards = (Map<String, UserVcard>) data;
						if (!SystemUtil.isEmpty(userVcards)) {
							final Set<String> keys = userVcards.keySet();
							SystemUtil.getCachedThreadPool().execute(new Runnable() {
								
								@Override
								public void run() {
									for (String key : keys) {
										Log.d("--call----Provider.UserVcardColumns-----BATCH_UPDATE---userVcard---" + userVcards.get(key));
										updateUserInfo(userVcards.get(key));
									}
								}
							});
						}
					}
					break;
				default:
					break;
				}
				break;
			/*case Provider.UserColumns.NOTIFY_FLAG:	//好友信息更新的通知
				switch (notifyType) {
				case BATCH_UPDATE:	//批量更新后的操作
					//重新加载该会话列表信息
					getLoaderManager().restartLoader(0, null, ThreadListFragment.this);
					break;

				default:
					break;
				}
				break;*/
			default:
				break;
			}
		}
		
	}
	
	/**
	 * 根据用户id来重新设置好友信息
	 * @param userVcard 好友的电子名片信息
	 * @update 2015年7月29日 下午9:45:11
	 */
	private void updateUserInfo(final UserVcard userVcard) {
		int position = -1;
		MsgThread targetThread = null;
		if (SystemUtil.isNotEmpty(mMsgThreads)) {
			int threadSize = mMsgThreads.size();
			for (int i = 0; i < threadSize; i++) {
				MsgThread thread = mMsgThreads.get(i);
				List<User> members = thread.getMembers();
				if (members.size() == 1) {	//只有一位成员，除自己外
					User user = members.get(0);
					UserVcard vcard = user.getUserVcard();
					if (vcard == null) {
						vcard = new UserVcard();
						vcard.setUserId(user.getId());
					}
					if (vcard.equals(userVcard)) {	//找到了对应的用户
						
						vcard.setNickname(userVcard.getNickname());
						String oldHash = vcard.getIconHash();
						String newHash = userVcard.getIconHash();
						boolean clearCache = false;
						if (oldHash != null) {	//之前有头像
							if (!oldHash.equals(newHash)) {	//头像有改变
								vcard.setIconHash(newHash);
								clearCache = true;
							}
						}
						String iconPath = userVcard.getIconPath();
						String thumbPath = userVcard.getThumbPath();
						if (clearCache) {
							ImageUtil.clearMemoryCache(iconPath);
							ImageUtil.clearMemoryCache(thumbPath);
						}
						vcard.setIconPath(iconPath);
						vcard.setThumbPath(thumbPath);
						
						thread.setMsgThreadName(user.getName());
						
						position = i;
						targetThread = thread;
						break;
					}
				}
			}
			
			if (targetThread != null) {
				if (mThreadAdapter != null) {
					Message msg = mHandler.obtainMessage();
					msg.what = Constants.MSG_UPDATE_ONE;
					msg.obj = targetThread;
					msg.arg1 = position;
					mHandler.sendMessage(msg);
				}
			}
		}
	}
	
	/**
	 * 局部更新回话
	 * @param position
	 * @param thread
	 * @update 2015年8月20日 下午5:28:51
	 */
	private void updateView(int position, MsgThread thread) {
		//得到第一个可显示控件的位置，  
        int visiblePosition = mListView.getFirstVisiblePosition();
        //只有当要更新的view在可见的位置时才更新，不可见时，跳过不更新 
        int relativePosition = position - visiblePosition;
        if (mThreadAdapter != null) {
        	if (relativePosition >= 0) {
        		//得到要更新的item的view  
        		View view = mListView.getChildAt(relativePosition);
				if (view != null) {
					//从view中取得holder  
					Object tag = view.getTag();
					if (tag != null && tag instanceof MsgThreadViewHolder) {
						MsgThreadViewHolder holder = (MsgThreadViewHolder) tag;
						holder.tvTitle.setText(thread.getMsgThreadName());
						UserVcard userVcard = thread.getMembers().get(0).getUserVcard();
						mThreadAdapter.showIcon(userVcard, holder.ivHeadIcon);
					}
				}
        	}
        }
	}
	
	/*class MsgThreadContentObserver extends ContentObserver {

		public MsgThreadContentObserver(Handler handler) {
			super(handler);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void onChange(boolean selfChange) {
			if (autoRefresh) {
				reLoadData();
			}
		}

		@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
		@Override
		public void onChange(boolean selfChange, Uri uri) {
			if (autoRefresh) {
				if (uri != null) {
					MsgThread thread = msgManager.getThreadByUri(uri);
					if (thread != null) {	//非删除行为
						if (mMsgThreads.contains(thread)) {
							mMsgThreads.remove(thread);
						}
						mMsgThreads.add(thread);
						Collections.sort(mMsgThreads, thread);
						mThreadAdapter.notifyDataSetChanged();
					} else {
						onChange(selfChange);
					}
				} else {
					onChange(selfChange);
				}
			}
		}
		
	}*/

	@Override
	public void handlerNetworkChanged(boolean networkAvailable) {
		if (networkAvailable) {
			netErrorView.setVisibility(View.GONE);
		} else {
			netErrorView.setVisibility(View.VISIBLE);
		}
	}
}
