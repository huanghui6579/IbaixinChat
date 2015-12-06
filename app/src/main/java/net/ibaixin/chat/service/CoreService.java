package net.ibaixin.chat.service;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.download.ImageDownloader.Scheme;
import com.nostra13.universalimageloader.utils.DiskCacheUtils;

import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.R;
import net.ibaixin.chat.activity.ChatActivity;
import net.ibaixin.chat.activity.LoginActivity;
import net.ibaixin.chat.activity.MainActivity;
import net.ibaixin.chat.download.DownloadListener;
import net.ibaixin.chat.download.DownloadManager;
import net.ibaixin.chat.listener.ChatRostListener;
import net.ibaixin.chat.listener.RosterLoadedCallback;
import net.ibaixin.chat.manager.MsgManager;
import net.ibaixin.chat.manager.PersonalManage;
import net.ibaixin.chat.manager.UserManager;
import net.ibaixin.chat.manager.web.MsgEngine;
import net.ibaixin.chat.manager.web.PersonalEngine;
import net.ibaixin.chat.manager.web.UserEngine;
import net.ibaixin.chat.model.ActionResult;
import net.ibaixin.chat.model.MsgInfo;
import net.ibaixin.chat.model.MsgInfo.SendState;
import net.ibaixin.chat.model.MsgPart;
import net.ibaixin.chat.model.MsgSenderInfo;
import net.ibaixin.chat.model.MsgThread;
import net.ibaixin.chat.model.MsgUploadInfo;
import net.ibaixin.chat.model.Personal;
import net.ibaixin.chat.model.PhotoItem;
import net.ibaixin.chat.model.User;
import net.ibaixin.chat.model.UserVcard;
import net.ibaixin.chat.model.web.AttachDto;
import net.ibaixin.chat.provider.Provider;
import net.ibaixin.chat.receiver.BasePersonalInfoReceiver;
import net.ibaixin.chat.smack.extension.MessageTypeExtension;
import net.ibaixin.chat.task.ReConnectTask;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.JSONUtils;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.MimeUtils;
import net.ibaixin.chat.util.Observer;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.util.XmppConnectionManager;
import net.ibaixin.chat.util.XmppUtil;
import net.ibaixin.chat.volley.toolbox.MultiPartStringRequest;
import net.ibaixin.chat.volley.toolbox.ProgressUpdateCallback;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.filter.FlexibleStanzaTypeFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.ChatStateManager;
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.filetransfer.FileTransferListener;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;
import org.jivesoftware.smackx.offline.OfflineMessageManager;
import org.json.JSONException;
import org.json.JSONObject;
import org.jxmpp.util.XmppStringUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;

/**
 * 核心的service服务，主要用来同步联系人数据
 * @author coolpad
 */
public class CoreService extends Service {
	public static final String FLAG_SYNC = "flag_sync";
	public static final String FLAG_RECEIVE_OFFINE_MSG = "flag_receive_offine_msg";
	public static final String FLAG_INIT_CURRENT_USER = "flag_init_current_user"; 
	
	/**
	 * 聊天新消息的通知id
	 */
	public static final int NOTIFY_ID_CHAT_MSG = 100;
	
	/**
	 * 同步更新所有好友到本地数据库的标识
	 */
	public static final int FLAG_SYNC_FRENDS = 1;
	/**
	 * 接收离线消息
	 */
	public static final int FLAG_RECEIVE_OFFINE = 2;

	/**
	 * 重新登录成功
	 */
	public static final int FLAG_RELOGIN_OK = 4;
	/**
	 * 后台登录
	 */
	public static final int FLAG_LOGIN = 5;
	/**
	 * 初始化个人信息
	 */
	public static final int FLAG_INIT_PERSONAL_INFO = 6;
	
	private IBinder mBinder = new MainBinder();
	
	private UserManager userManager = UserManager.getInstance();
	private MsgManager msgManager = MsgManager.getInstance();
	private PersonalManage personalManage = PersonalManage.getInstance();
	
	private MyHandler mHandler = null;
	
	private HandlerThread mHandlerThread = null;
	private static RosterListener mRosterListener;
	private static FileTransferListener mFileTransferListener;
	private static ChatManagerListener mChatManagerListener;
	private static MyChatMessageListener mChatMessageListener;
	private static ChatManager mChatManager;
	private static OfflineMessageManager mOfflineMessageManager;
	private static FileTransferManager mFileTransferManager;
	XMPPTCPConnection connection = (XMPPTCPConnection) XmppConnectionManager.getInstance().getConnection();
	
	private ImageLoader mImageLoader = ImageLoader.getInstance();
	
	private ActivityManager mActivityManager;
	
	private Context mContext;
	
	private NotificationManager mNotificationManager;
	
	private CoreReceiver mCoreReceiver;
	
//	private DeliveryReceiptManager mDeliveryReceiptManager;
	
	/**
	 * 聊天消息状态监听的管理器
	 */
	private ChatStateManager mChatStateManager;
	
	private RequestQueue mRequestQueue;
	
	/**
	 * 文件下载器
	 */
	private DownloadManager mDownloadManager;
	
	/**
	 * 本地广播管理器
	 */
	private LocalBroadcastManager mLocalBroadcastManager;
	
//	SendChatMessageReceiver chatMessageReceiver;
	
	private class MyHandler extends Handler {
		
		public MyHandler() {
			super();
		}
		
		public MyHandler(Callback callback) {
			super(callback);
		}

		public MyHandler(Looper looper, Callback callback) {
			super(looper, callback);
		}

		public MyHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case Constants.MSG_RECEIVE_CHAT_MSG:	//接收聊天消息
				MsgInfo msgInfo = (MsgInfo) msg.obj;
				String contentTitle = null;
				String contentText = null;
				int msgCount = msg.arg1;
				Intent resultIntent = new Intent(mContext, ChatActivity.class);
				if (msgInfo != null) {
					resultIntent.putExtra(ChatActivity.ARG_THREAD_ID, msgInfo.getThreadID());
					contentTitle = msgInfo.getFromUser();
					contentText = msgInfo.getContent();
				} else {
					contentTitle = getString(R.string.notification_batch_promtp_title);
					contentText = getString(R.string.notification_batch_promtp_content, msgCount);
					resultIntent.setClass(mContext, MainActivity.class);
					resultIntent.putExtra(MainActivity.ARG_SYNC_FRIENDS, false);
					resultIntent.putExtra(MainActivity.ARG_INIT_POSITION, true);
				}
				// 100 毫秒延迟后，震动 200 毫秒，暂停 100 毫秒后，再震动 300 毫秒
//				long[] vibrate = {100,200,100,300};
				//发送广播到对应的界面处理
				NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplication());
				builder.setSmallIcon(R.drawable.ic_launcher)
						.setAutoCancel(true)
						.setShowWhen(true)
						.setDefaults(Notification.DEFAULT_ALL)
						.setAutoCancel(true)
						.setTicker(getString(R.string.notification_new_msg_title, msgCount))
						.setContentTitle(contentTitle)
						.setContentText(contentText)
						.setPriority(NotificationCompat.PRIORITY_HIGH);

//				TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
//				stackBuilder.addParentStack(MainActivity.class);
//				stackBuilder.addNextIntent(resultIntent);
				PendingIntent resultPendingIntent = PendingIntent.getActivity(mContext, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
				builder.setContentIntent(resultPendingIntent);
				if (mNotificationManager == null) {
					mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);;
				}
				// mId allows you to update the notification later on.
				mNotificationManager.notify(NOTIFY_ID_CHAT_MSG, builder.build());
				break;
			case Constants.MSG_UPDATE_FAILED:
				SystemUtil.makeShortToast(R.string.update_failed);
				break;
			default:
				break;
			}
		}
	}
	
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		mContext = this;
		
		mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
		
		if (mHandlerThread == null) {
			mHandlerThread = new HandlerThread(this.getClass().getCanonicalName());
			mHandlerThread.start();
			mHandler = new MyHandler(mHandlerThread.getLooper());
		}
		if (connection != null) {
			if (mRosterListener == null) {
				mRosterListener = new ChatRostListener();
				Roster roster = Roster.getInstanceFor(connection);
				roster.addRosterListener(mRosterListener);
				ChatRostListener.hasRosterListener = true;
			}
			
			if (mFileTransferListener == null) {
				mFileTransferManager = FileTransferManager.getInstanceFor(connection);
				mFileTransferListener = new MyFileTransferListener();
				mFileTransferManager.addFileTransferListener(mFileTransferListener);
			}
			
			if (mChatMessageListener == null) {
				mChatMessageListener = new MyChatMessageListener();
			}
			
			if (mChatManager == null) {
				mChatManager = ChatManager.getInstanceFor(connection);
				mChatManagerListener = new MyChatManagerListener();
				mChatManager.addChatListener(mChatManagerListener);
			}
			
			if (mOfflineMessageManager == null) {
				mOfflineMessageManager = new OfflineMessageManager(connection);
			}
			
			if (mChatStateManager == null) {
				mChatStateManager = ChatStateManager.getInstance(connection);
			}

			StanzaFilter packetFilter = new FlexibleStanzaTypeFilter<Message>() {

				@Override
				protected boolean acceptSpecific(Message packet) {
					if (packet != null) {
						String jid = packet.getFrom();
						//只处理发起消息的不是自己的情况
						return !XmppUtil.isOutMessage(jid);
					}
					return false;
				}
			};
			connection.addAsyncStanzaListener(mChatMessageListener, packetFilter);
			
			/*if (mDeliveryReceiptManager == null) {
				mDeliveryReceiptManager = DeliveryReceiptManager.getInstanceFor(connection);
				mDeliveryReceiptManager.setAutoReceiptMode(AutoReceiptMode.always);
				mDeliveryReceiptManager.addReceiptReceivedListener(new ReceiptReceivedListener() {
					
					@Override
					public void onReceiptReceived(String fromJid, String toJid,
							String receiptId, Stanza receipt) {
						// TODO Auto-generated method stub
						Log.d("-----fromJid-------" + fromJid + "---toJid----" + toJid + "--receiptId--" + receiptId + "---receipt---" + receipt);
					}
				});
			}*/
			
		}
		
		mCoreReceiver = new CoreReceiver();
		IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CoreReceiver.ACTION_RELOGIN);
        intentFilter.addAction(CoreReceiver.ACTION_SYNC_VCARD);
        intentFilter.addAction(CoreReceiver.ACTION_UPDATE_VCARD);
		registerReceiver(mCoreReceiver, intentFilter);
		
		mRequestQueue = ((ChatApplication) getApplication()).getRequestQueue();
		
		if (mDownloadManager == null) {
			mDownloadManager = new DownloadManager();
		}
		
//		SystemUtil.getCachedThreadPool().execute(new ReceiveMessageTask());
		
		//注册发送消息的广播
//		chatMessageReceiver = new SendChatMessageReceiver();
//		IntentFilter intentFilter = new IntentFilter(SendChatMessageReceiver.ACTION_SEND_CHAT_MSG);
//		registerReceiver(chatMessageReceiver, intentFilter);
		
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return mBinder;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			//监听消息
			int syncFlag = intent.getIntExtra(FLAG_SYNC, 0);
			
			int initCurrentUserFlag = intent.getIntExtra(FLAG_INIT_CURRENT_USER, 0);
			if (initCurrentUserFlag == FLAG_INIT_PERSONAL_INFO) {	//登录成功后初始化个人信息
				ChatApplication app = (ChatApplication) getApplication();
				initCurrentUser(app.getCurrentUser());
			}
			int offineFlag = intent.getIntExtra(FLAG_RECEIVE_OFFINE_MSG, 0);
			if (offineFlag == FLAG_RECEIVE_OFFINE) {	//登录成功后接受离线消息
				//接收离线消息
				receiveOffineMsg();
			}
			Log.d("----onStartCommand--syncFlag--" + syncFlag);
			//同步好友列表
			switch (syncFlag) {
			case FLAG_SYNC_FRENDS:	//从服务器上同步所有的好友列表到本地
//				new Thread(new SyncFriendsTask()).start();
				mHandler.post(new SyncFriendsTask());
				break;
			case FLAG_RELOGIN_OK://重新登录成功
				initCurrentUser(ChatApplication.getInstance().getCurrentUser());//同步自己的信息
//				mHandler.post(new SyncFriendsTask());//从服务器上同步所有的好友列表到本地
				SystemUtil.getCachedThreadPool().execute(new HandleOffineMsgTask());//接受离线消息
				//发送未发送成功的消息
				
				break;
			case FLAG_LOGIN:	//后台登陆
				SystemUtil.getCachedThreadPool().execute(new LoginTask());
				break;
			default:
				break;
			}
			Log.d("-------connection.isSmEnabled()------" + connection.isSmEnabled() + "----connection.isSmAvailable()----" + connection.isSmAvailable() + "-----connection.isSmResumptionPossible()-----" + connection.isSmResumptionPossible());
		}
		
		return Service.START_REDELIVER_INTENT;
	}
	
	@Override
	public void onDestroy() {
//		if (mConnectionListener != null) {
//			connection.removeConnectionListener(mConnectionListener);
//		}
		if (mCoreReceiver != null) {
			unregisterReceiver(mCoreReceiver);
		}
		if (mDownloadManager != null) {
			mDownloadManager.cancelAll();
		}
		super.onDestroy();
	}
	
	/**
	 * 一般是登录成功后，初始化当前用户的个人信息
	 */
	public void initCurrentUser(final Personal person) {
		SystemUtil.getCachedThreadPool().execute(new Runnable() {
			@Override
			public void run() {
				if (!person.isEmpty()) {
					/*Personal localPerson = personalManage.getLocalSelfInfo(person);
					if (localPerson == null) {	//本地没有个人信息，则从服务器上同步
						//从网上同步个人信息
						XmppUtil.syncPersonalInfo(connection, person);
						personalManage.saveOrUpdateCurrentUser(person);
					} else {	//本地有个人信息，则只需改变状态就行了
						personalManage.updatePersonStatus(person);
					}*/
					
					if(!LoginActivity.isLocalAccountLogin
							&& LoginActivity.isQQAccountRegister
							&& ChatApplication.getInstance().getSystemConfig().getNickname()!=null){
						//如果是QQ注册的话需要更新昵称
						updateNickOnRegister(ChatApplication.getInstance().getSystemConfig().getNickname());
					}
					//每次都需要从网上同步个人信息，才可以保证本地信息最新 update by dudejin 2015-03-17  
//					XmppUtil.syncPersonalInfo(connection, person);
					Personal tmpPerson = personalManage.getLocalSelfInfoByUsername(person);
					if (tmpPerson == null) {	//本地没有该用户，则创建
						tmpPerson = personalManage.addPersonal(person);
					}
					if (tmpPerson != null) {
						ChatApplication app = (ChatApplication) mContext.getApplicationContext();
						app.setCurrentUser(tmpPerson);
						
						PersonalEngine personalEngine = new PersonalEngine(mContext);
						personalEngine.getPersonalInfo(tmpPerson.getUsername());
					} else {
						Log.e("---initCurrentUser--初始化添加用户失败-----person----" + person);
					}
//					personalManage.saveOrUpdateCurrentUser(person);
					//发送广播，更新显示个人信息
//					Intent intent = new Intent(BasePersonalInfoReceiver.ACTION_REFRESH_PERSONAL_INFO);
//					sendBroadcast(intent);
				}
			}
		});
	}
	
	/**
	 * 发送消息的线程
	 * @author Administrator
	 * @update 2014年11月16日 下午5:35:14
	 * @param senderInfo 发送消息的实体
	 */
	public void sendChatMsg(MsgSenderInfo senderInfo) {
		if (senderInfo.chat != null) {
			if (senderInfo.msgInfo.getSendState() != SendState.SENDING) {
				senderInfo.msgInfo.setSendState(SendState.SENDING);
				senderInfo.handler.sendEmptyMessage(Constants.MSG_MODIFY_CHAT_MSG_SEND_STATE);
			}
			SystemUtil.getCachedThreadPool().execute(new SendMsgTask(senderInfo));
		} else {
			senderInfo.msgInfo.setSendState(SendState.FAILED);
			msgManager.updateMsgSendStatus(senderInfo.msgInfo);
			senderInfo.handler.sendEmptyMessage(Constants.MSG_MODIFY_CHAT_MSG_SEND_STATE);
		}
	}
	
	/**
	 * 发送对方输入状态的消息
	 * @param senderInfo
	 * @update 2015年9月12日 下午4:31:58
	 */
	public void sendChatStateMsg(ChatState state, MsgSenderInfo senderInfo) {
		if (senderInfo.chat != null) {
			SystemUtil.getCachedThreadPool().execute(new SendStateMsgTask(senderInfo, state));
		}
	}
	
	/**
	 * 后台登录的任务
	 * @author huanghui1
	 * @version 1.0.0
	 * @update 2015年6月26日 下午8:49:45
	 */
	class LoginTask implements Runnable, ReConnectTask.LoginCallBack {
		private int timeDelay = 2000;
		int loginTime = 0;

		@Override
		public void run() {
			ReConnectTask connectTask = new ReConnectTask(this);
			mHandler.post(connectTask);
		}

		@Override
		public void onLoginSuccessful() {
			initCurrentUser(ChatApplication.getInstance().getCurrentUser());//同步自己的信息
			SystemUtil.getCachedThreadPool().execute(new HandleOffineMsgTask());//接受离线消息
			SystemUtil.getCachedThreadPool().execute(new SyncFriendsTask());//从服务器上同步所有的好友列表到本地
		}

		@Override
		public void onLoginFailed(Exception e) {
			if (loginTime < ReConnectTask.RECONNECT_TIME) {	//重试3次就不再重试了
				Timer timer = new Timer();
				ReConnectTask connectTask = new ReConnectTask(this);
				timer.schedule(connectTask, timeDelay);
				loginTime ++;
			}
		}
		
	}
	
	/**
	 * 发送消息的任务线程
	 * @author huanghui1
	 * @update 2014年11月17日 上午9:05:04
	 */
	class SendMsgTask implements Runnable {
		private MsgSenderInfo senderInfo;

		public SendMsgTask(MsgSenderInfo senderInfo) {
			this.senderInfo = senderInfo;
		}

		@Override
		public void run() {
			final MsgInfo msgInfo =  senderInfo.msgInfo;
			String fromJid = msgInfo.getFromUser();
			String toJid = msgInfo.getToUser();
			String sender = XmppStringUtils.parseLocalpart(fromJid);
			String receiver = XmppStringUtils.parseLocalpart(toJid);
			//将请完整的还原为账号
			msgInfo.setFromUser(sender);
			msgInfo.setToUser(receiver);
			final MsgInfo.Type msgType = msgInfo.getMsgType();
			ChatApplication app = (ChatApplication) mContext.getApplicationContext();
			if (app.isNetWorking()) {
				try {
					
					final Message message = new Message();
					message.setType(Message.Type.chat);
//					DeliveryReceiptRequest.addTo(message);
					final String content = msgInfo.getContent();
					if (MsgInfo.Type.TEXT != msgType) {	//非文本消息
						Uri.Builder builder = Uri.parse(Constants.BASE_API_URL).buildUpon();
						final MsgPart msgPart = msgInfo.getMsgPart();// 创建文件传输管理器
//						String to = msgInfo.getToJid() + "/Spark 2.6.3";
//						String to = msgInfo.getToJid() + "/Android";
						
						builder.appendPath("sendFile");
						
						String fileName = msgPart.getFileName();
						String mimeType = msgPart.getMimeType();
						
						AttachDto attachDto = new AttachDto();
						attachDto.setFileName(fileName);
						attachDto.setSender(sender);
						attachDto.setReceiver(receiver);
						attachDto.setMimeType(mimeType);
						
						File sendFile = new File(msgPart.getFilePath());
						
						final MessageTypeExtension typeExtension = new MessageTypeExtension();
						typeExtension.setFileName(fileName);
						typeExtension.setMimeType(mimeType);
						
						Map<String, File[]> files = new HashMap<>();
						Map<String, String> params = new HashMap<>();
						typeExtension.setMsgType(msgType);
						switch (msgType) {
						case IMAGE:	//图片消息
							String thumbName = msgPart.getThumbName();
							attachDto.setThumbName(thumbName);
							typeExtension.setThumbName(thumbName);
							if (!senderInfo.originalImage) {	//非原图发送，则需压缩图片
								sendFile = DiskCacheUtils.findInCache(Scheme.FILE.wrap(msgPart.getFilePath()), mImageLoader.getDiskCache());
								File originalFile = new File(msgPart.getFilePath());
								if (sendFile.length() > originalFile.length()) {	//压缩后的图片比原始图片还大，则直接发送原始图片
									sendFile = originalFile;
								}
							}
							File[] uploads = new File[2];
							uploads[0] = sendFile;
							uploads[1] = new File(msgPart.getThumbPath());	//发送压缩图片的同时，还要发送该压缩图片的缩略图
							files.put("uploadFile", uploads);
							break;
						case LOCATION:	//地理位置消息
							typeExtension.setDesc(msgPart.getDesc());
						default:	//其他消息
							files.put("uploadFile", new File[] {sendFile});
							break;
						}
						if (sendFile.exists()) {
							//先将消息保存到数据库
							updateSendInfo(senderInfo, msgInfo);
							String hash = SystemUtil.encoderFileByMd5(sendFile);
							attachDto.setHash(hash);
							typeExtension.setHash(hash);
							typeExtension.setSize(sendFile.length());
							
							String jsonStr = JSONUtils.objToJson(attachDto);
							if (jsonStr != null) {
								Log.d("---------------" + jsonStr);
								params.put("jsonStr", jsonStr);
							}
							MultiPartStringRequest multiPartRequest = SystemUtil.getUploadFileRequest(builder.toString(), files, params, new Response.Listener<String>() {

								@Override
								public void onResponse(String response) {
									if (!TextUtils.isEmpty(response)) {
										JSONObject jsonObject = null;
										try {
											jsonObject = new JSONObject(response);
											if (!jsonObject.isNull("resultCode")) {
												int resultCode = jsonObject.getInt("resultCode");
												if (resultCode == ActionResult.CODE_SUCCESS) {
													if (!jsonObject.isNull("id")) {
														String id = jsonObject.getString("id");
														msgPart.setFileToken(id);
														
														typeExtension.setFileId(id);
														
														message.addExtension(typeExtension);
														
														send(senderInfo, message, content, msgInfo);
													}
												} else {
													msgInfo.setSendState(SendState.FAILED);
												}
											}
										} catch (JSONException | SmackException | IOException | XMPPException e) {
											msgInfo.setSendState(SendState.FAILED);
											Log.e(e.getMessage());
										}
									} else {
										Log.d("-----response---is----null---msginfo---" + msgInfo);
										msgInfo.setSendState(SendState.FAILED);
									}
									updateSendStatus(senderInfo, msgInfo);
								}
							}, new Response.ErrorListener() {

								@Override
								public void onErrorResponse(VolleyError error) {
									msgInfo.setSendState(SendState.FAILED);
									updateSendStatus(senderInfo, msgInfo);
									Log.e(error.toString());
								}
							}, null, mHandler);
							//发送文件
							if (multiPartRequest != null) {
								multiPartRequest.setProgressCallback(new UploadProgressCallback(msgInfo));
								mRequestQueue.add(multiPartRequest);
							}
						} else {
							msgInfo.setSendState(SendState.FAILED);
							msgManager.deleteMsgInfoById(msgInfo, senderInfo.msgThread);
							senderInfo.handler.sendEmptyMessage(Constants.MSG_MODIFY_CHAT_MSG_SEND_STATE);
							Log.d("-------发送失败---文件不存在---");
							return;
						}
						
					} else {	//文本消息
						updateSendInfo(senderInfo, msgInfo);
						send(senderInfo, message, content, msgInfo);
					}
				} catch (Exception e) {
					msgInfo.setSendState(SendState.FAILED);
					e.printStackTrace();
					Log.d("-------发送失败------");
				}
			} else {
				msgInfo.setSendState(SendState.FAILED);
			}
			updateSendStatus(senderInfo, msgInfo);
		}
		
	}

	/**
	 * 上传的监听器
	 */
	class UploadProgressCallback implements ProgressUpdateCallback {
		private MsgInfo msgInfo;

		public UploadProgressCallback(MsgInfo msgInfo) {
			this.msgInfo = msgInfo;
		}

		@Override
		public void setProgressUpdateStatus(int value) {
			MsgUploadInfo uploadInfo = new MsgUploadInfo(msgInfo, value);
			msgManager.updateMsgUploadInfo(uploadInfo);
		}
	}
	
	/**
	 * 发送对方输入状态的消息
	 * @author huanghui1
	 * @version 1.0.0
	 * @update 2015年9月12日 下午4:34:26
	 */
	class SendStateMsgTask implements Runnable {
		private MsgSenderInfo senderInfo;
		private ChatState state;

		public SendStateMsgTask(MsgSenderInfo senderInfo, ChatState state) {
			super();
			this.senderInfo = senderInfo;
			this.state = state;
		}

		@Override
		public void run() {
//				Message message = new Message();
//				message.setType(Message.Type.headline);
//				ChatStateExtension extension = new ChatStateExtension(state);
//				message.addExtension(extension);
//
//				senderInfo.chat.sendMessage(message);
				if (mChatStateManager != null) {
					if(senderInfo.chat != null && state != null) {
						boolean reflectSuccess = false;
						try {
							Method method = mChatStateManager.getClass().getDeclaredMethod("updateChatState", senderInfo.chat.getClass(), state.getDeclaringClass());
							if (method != null) {
                                method.setAccessible(true);
                                Boolean value = (Boolean) method.invoke(mChatStateManager, senderInfo.chat, state);
                                if (value != null && value) {
                                    Message message = new Message();
                                    message.setType(Type.headline);
                                    ChatStateExtension extension = new ChatStateExtension(state);
                                    message.addExtension(extension);
    
                                    String participant = null;
                                    String threadId = null;
                                    Field participantField = senderInfo.chat.getClass().getDeclaredField("participant");
                                    if (participantField != null) {
                                        participantField.setAccessible(true);
                                        participant = (String) participantField.get(senderInfo.chat);
                                    }
                                    Field threadIdField = senderInfo.chat.getClass().getDeclaredField("threadID");
                                    if (threadIdField != null) {
                                        threadIdField.setAccessible(true);
                                        threadId = (String) threadIdField.get(senderInfo.chat);
                                    }
                                    ChatManager chatManager = null;
                                    Field chatManagerField = senderInfo.chat.getClass().getDeclaredField("chatManager");
                                    if (chatManagerField != null) {
                                        chatManagerField.setAccessible(true);
                                        chatManager = (ChatManager) chatManagerField.get(senderInfo.chat);
                                    }
                                    if (participant != null && threadId != null && chatManager != null) {
                                        Method sendMessageMethod = chatManager.getClass().getDeclaredMethod("sendMessage", senderInfo.chat.getClass(), message.getClass());
                                        if (sendMessageMethod != null) {
                                            sendMessageMethod.setAccessible(true);
											message.setThread(threadId);
											message.setTo(participant);
                                            sendMessageMethod.invoke(chatManager, senderInfo.chat, message);
											reflectSuccess = true;
										}
										//chatManager.sendMessage(senderInfo.chat, message);
									}
                                }
                            }
						} catch (Exception e) {
							reflectSuccess = false;
							Log.e(e.getMessage());
						}
						
						if (!reflectSuccess) {
							try {
								mChatStateManager.setCurrentState(state, senderInfo.chat);
							} catch (Exception e) {
								Log.e(e.getMessage());
							}
						}
						
						/*if(!updateChatState(chat, newState)) {
							return;
						}*/
						 
					} else {
						Log.d("Arguments cannot be null.--------senderInfo.chat----is null---" + senderInfo.chat + " or ----state--is null--state--" + state);
					}
				}
		}
	}
	
	/**
	 * 更新消息的发送情况
	 * @param senderInfo
	 * @param msgInfo
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年5月1日 下午6:53:53
	 */
	private void updateSendInfo(MsgSenderInfo senderInfo ,MsgInfo msgInfo) {
		if (!senderInfo.isReSend) {	//不是重发该消息，则更新会话的一些摘要信息
			msgManager.addMsgInfo(msgInfo, false);
			senderInfo.msgThread.setSnippetId(msgInfo.getMsgId());
//			String snippetContent = msgManager.getSnippetContentByMsgType(msgInfo.getMsgType(), msgInfo);
			String snippetContent = msgInfo.getSnippetContent();
			senderInfo.msgThread.setSnippetContent(snippetContent);
			senderInfo.msgThread.setLastMsgInfo(msgInfo);
			senderInfo.msgThread.setModifyDate(System.currentTimeMillis());
			senderInfo.msgThread = msgManager.updateSnippet(senderInfo.msgThread ,true);
		}
	}
	
	/**
	 * 数据库中更新呢消息的发送状态
	 * @param msgInfo
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年5月1日 下午7:05:28
	 */
	private void updateSendStatus(final MsgSenderInfo senderInfo, final MsgInfo msgInfo) {
		msgManager.updateMsgSendStatus(msgInfo);
		senderInfo.handler.sendEmptyMessage(Constants.MSG_MODIFY_CHAT_MSG_SEND_STATE);
	}
	
	/**
	 * 开始发送消息
	 * @param senderInfo
	 * @param message
	 * @param content
	 * @param msgInfo
	 * @author tiger
	 * @version 1.0.0
	 * @throws XMPPException 
	 * @throws IOException 
	 * @throws SmackException 
	 * @update 2015年5月1日 下午7:08:56
	 */
	private void send(final MsgSenderInfo senderInfo, final Message message, final String content, final MsgInfo msgInfo) throws SmackException, IOException, XMPPException {
		if (!TextUtils.isEmpty(content)) {
			message.setBody(content);
		}
		if (!XmppUtil.checkConnected(connection)) {	//连接不可用，则重新登录
			connection.connect();
		}
		if (!XmppUtil.checkAuthenticated(connection)) {
			XmppConnectionManager.getInstance().login(connection);
		}
		senderInfo.chat.sendMessage(message);
		msgInfo.setSendState(SendState.SUCCESS);
	}
	
/*	class SendMsgTask implements Runnable {
		private MsgSenderInfo senderInfo;
		
		public SendMsgTask(MsgSenderInfo senderInfo) {
			this.senderInfo = senderInfo;
		}
		
		@Override
		public void run() {
			MsgInfo msgInfo =  senderInfo.msgInfo;
			String fromJid = msgInfo.getFromUser();
			String toJid = msgInfo.getToUser();
			//将请完整的还原为账号
			msgInfo.setFromUser(XmppStringUtils.parseLocalpart(fromJid));
			msgInfo.setToUser(XmppStringUtils.parseLocalpart(toJid));
			MsgInfo.Type msgType = msgInfo.getMsgType();
			if (ChatApplication.isNetWorking) {
				try {
					if (!senderInfo.isReSend) {	//不是重发该消息，则更新会话的一些摘要信息
						msgInfo = msgManager.addMsgInfo(msgInfo);
						senderInfo.msgThread.setSnippetId(msgInfo.getId());
						String snippetContent = msgManager.getSnippetContentByMsgType(msgType, msgInfo);
						senderInfo.msgThread.setSnippetContent(snippetContent);
						senderInfo.msgThread.setModifyDate(System.currentTimeMillis());
						senderInfo.msgThread = msgManager.updateMsgThread(senderInfo.msgThread);
					}
					if (MsgInfo.Type.TEXT == msgType) {	//文本消息
						try {
							Message message = new Message();
							message.setType(Message.Type.chat);
							DeliveryReceiptRequest.addTo(message);
							message.setBody(senderInfo.msgInfo.getContent());
							senderInfo.chat.sendMessage(message);
							msgInfo.setSendState(SendState.SUCCESS);
						} catch (NotConnectedException e) {
							msgInfo.setSendState(SendState.FAILED);
							e.printStackTrace();
						}
					} else {	//非文本消息，则以附件形式发送
						MsgPart msgPart = msgInfo.getMsgPart();// 创建文件传输管理器
						if (!XmppStringUtils.isFullJID(toJid)) {
							toJid = SystemUtil.wrapFullJid(toJid);
						}
//						String to = msgInfo.getToJid() + "/Spark 2.6.3";
//						String to = msgInfo.getToJid() + "/Android";
						OutgoingFileTransfer fileTransfer = mFileTransferManager.createOutgoingFileTransfer(toJid);
						
						File sendFile = null;
						if (msgType == MsgInfo.Type.IMAGE) {	//图片类型
							if (senderInfo.originalImage) {	//原图发送
								sendFile = new File(msgPart.getFilePath());
							} else {
								sendFile = DiskCacheUtils.findInCache(Scheme.FILE.wrap(msgPart.getFilePath()), mImageLoader.getDiskCache());
							}
						} else {
							sendFile = new File(msgPart.getFilePath());
						}
						if (sendFile.exists()) {
							try {
								StringBuilder description = new StringBuilder();
								switch (msgType) {
								case LOCATION:	//地理位置信息
									description.append(msgInfo.getContent()).append(Constants.SPLITE_TAG_MSG_TYPE).append(msgInfo.getSubject());
									break;
								case VOICE:	//语音消息
									description.append(msgInfo.getContent());
									break;
								default:
									description.append(msgPart.getFileName());
									break;
								}
								Log.d("------sendFile-------" + sendFile.getAbsolutePath() + "-exists---" + sendFile.exists());
								description.append(Constants.SPLITE_TAG_MSG_TYPE).append(msgType.ordinal());
								fileTransfer.sendStream(new FileInputStream(sendFile), msgPart.getFileName(), sendFile.length(), description.toString());
//								fileTransfer.sendFile(sendFile, msgPart.getFileName());
								boolean transferCompleted = false;
								while (!transferCompleted) {	//传输完毕
//									Log.d("-------------fileTransfer.getStatus()----------------" + fileTransfer.getStatus());
//									if (fileTransfer.getStatus() == FileTransfer.Status.error) {
//										msgInfo.setSendState(SendState.FAILED);
//										Log.d("----FileTransferManager------" + fileTransfer.getStatus() + "--" + fileTransfer.getProgress());
//									}
//									Log.d("----FileTransferManager------" + fileTransfer.getStatus() + "--" + fileTransfer.getProgress());
									switch (fileTransfer.getStatus()) {
									case complete:
										transferCompleted = true;
										msgInfo.setSendState(SendState.SUCCESS);
										break;
									case error:
									case cancelled:
									case refused:
										transferCompleted = true;
										msgInfo.setSendState(SendState.FAILED);
										break;
									case in_progress:
										Log.d("CoreService", "文件已经传输："+(fileTransfer.getProgress())*100+"%");
										break ;
									default:
										break;
									}
								}
								Log.d("-------发送完毕------");
//								msgInfo.setSendState(SendState.SUCCESS);
							} catch (FileNotFoundException e) {
								msgInfo.setSendState(SendState.FAILED);
								Log.d("-------发送失败---FileNotFoundException---");
								e.printStackTrace();
							}
						} else {
							msgInfo.setSendState(SendState.FAILED);
							msgManager.deleteMsgInfoById(msgInfo, senderInfo.msgThread);
							senderInfo.handler.sendEmptyMessage(Constants.MSG_MODIFY_CHAT_MSG_SEND_STATE);
							Log.d("-------发送失败---文件不存在---");
						}
					}//XMPPException | SmackException | 
				} catch (Exception e) {
					msgInfo.setSendState(SendState.FAILED);
					e.printStackTrace();
					Log.d("-------发送失败------");
				}
			} else {
				msgInfo.setSendState(SendState.FAILED);
			}
			msgInfo = msgManager.updateMsgSendStatus(msgInfo);
			senderInfo.handler.sendEmptyMessage(Constants.MSG_MODIFY_CHAT_MSG_SEND_STATE);
		}
		
	}
*/	
	/**
	 * 接收openfie的消息
	 * @author huanghui1
	 * @update 2014年11月1日 下午5:24:55
	 */
	class ReceiveMessageTask implements Runnable {

		@Override
		public void run() {
//			packetCollector.nextResult();
			if (XmppUtil.checkAuthenticated(connection)) {	//是否登录
				initChatManager(connection);
			} else {	//重新登录
				//TODO 重新登录
			}
		}
		
	}
	
	/**
	 * 初始化ChatManager
	 * @author Administrator
	 * @update 2014年11月16日 下午5:41:28
	 */
	private void initChatManager(AbstractXMPPConnection connection) {
		if (mChatManager == null) {
			synchronized (CoreService.class) {
				mChatManager = ChatManager.getInstanceFor(connection);
				mChatManager.addChatListener(new MyChatManagerListener());
			}
		}
	}
	
	/**
	 * 处理消息的后台线程，主要是将消息存入数据库
	 * @author huanghui1
	 * @update 2014年11月3日 下午10:40:19
	 */
	class ProcessMsgTask implements Runnable {
//		Chat chat;
		Message message;
		boolean notify = true;

		public ProcessMsgTask(/*Chat chat, */Message message) {
			super();
//			this.chat = chat;
			this.message = message;
		}

		public ProcessMsgTask(/*Chat chat, */Message message, boolean notify) {
			super();
			Log.w("------ProcessMsgTask----message-----" + message);
//			this.chat = chat;
			this.message = message;
			this.notify = notify;
		}

		@Override
		public void run() {
			MsgInfo msgInfo = processMsg(message);
			if (msgInfo != null) {
				msgInfo = msgManager.addMsgInfo(msgInfo, true);

				//下载文件
				downloadMsgFile(msgInfo, mContext);
				
				int threaId = msgInfo.getThreadID();
				MsgThread msgThread = msgManager.getThreadById(threaId);
				if (msgThread != null) {
					msgThread.setModifyDate(msgInfo.getCreationDate());
					msgThread.setSnippetId(msgInfo.getMsgId());
					String snippetContent = msgInfo.getSnippetContent()/*msgManager.getSnippetContentByMsgType(msgInfo.getMsgType(), msgInfo)*/;
					msgThread.setSnippetContent(snippetContent);
					msgThread.setLastMsgInfo(msgInfo);
					int unReadCount = msgThread.getUnReadCount();
					unReadCount += 1;
					msgThread.setUnReadCount(unReadCount);
					msgManager.updateSnippet(msgThread, true);
					
					//刷新ui
//					notifyUI(msgInfo, ChatActivity.MsgProcessReceiver.ACTION_PROCESS_MSG);
				} else {
					//TODO thread 为空的情况需要做处理
				}
				if (notify && msgInfo != null) {
					if (!isChatActivityOnTop()) {
						android.os.Message msg = mHandler.obtainMessage();
						msg.obj = msgInfo;
						msg.arg1 = 1;
						msg.what = Constants.MSG_RECEIVE_CHAT_MSG;
						mHandler.sendMessage(msg);
					}
				}
			}
			
		}
		
	}
	
	/**
	 * 处理状态消息的后台任务
	 * @author huanghui1
	 * @version 1.0.0
	 * @update 2015年9月12日 下午3:46:25
	 */
	class ProcessStateMsgTask implements Runnable {
		private Message message;
		private ChatState state;

		public ProcessStateMsgTask(ChatState state, Message message) {
			super();
			this.state = state;
			this.message = message;
		}

		@Override
		public void run() {
			MsgInfo msgInfo = new MsgInfo();
			msgInfo.setComming(true);
			String from = SystemUtil.unwrapJid(message.getFrom());
			msgInfo.setFromUser(from);
			int threadId = msgManager.getThreadIdByMembers(false, from);	//查找本地会话，如果没有就创建
			if (threadId > 0) {	//有该会话
				msgInfo.setThreadID(threadId);
				msgInfo.setContent(state.name());
				
				notifyUI(msgInfo, ChatActivity.MsgProcessReceiver.ACTION_UPDATE_CHAT_STATE);
			}
			
		}
		
	}
	
	/**
	 * 刷新界面
	 * @param msgInfo 新的消息
	 * @param action
	 * @update 2015年9月12日 下午3:35:22
	 */
	private void notifyUI(MsgInfo msgInfo, String action) {
		//发送本地广播，更新界面
		Intent intent = new Intent(action);
		intent.putExtra(ChatActivity.ARG_MSG_INFO, msgInfo);
		if (mLocalBroadcastManager != null) {
			mLocalBroadcastManager.sendBroadcast(intent);
		}
	}
	
	/**
	 * 处理聊天消息
	 * @update 2014年11月1日 下午5:46:44
	 * @param message
	 */
	private MsgInfo processMsg(Message message) {
		if (Type.chat == message.getType()) {	//聊天信息
			String from = SystemUtil.unwrapJid(message.getFrom());
			MsgInfo msgInfo = new MsgInfo();
			msgInfo.setComming(true);
			msgInfo.setContent(message.getBody());
			msgInfo.setFromUser(from);
			long msgTime = System.currentTimeMillis();
			MsgThread msgThread = msgManager.getThreadByMembers(true, from);
			int threadId = -1;
			if (msgThread != null) {
				threadId = msgThread.getId();
				msgInfo.setThreadID(threadId);
			}
			//获取离线时间
			if (message.hasExtension(DelayInformation.NAMESPACE)) {	//有延迟消息
				DelayInformation delayInformation = message.getExtension(DelayInformation.ELEMENT, DelayInformation.NAMESPACE);
				if (delayInformation != null) {
					msgTime = delayInformation.getStamp().getTime();
				}
			}
			msgInfo.setCreationDate(msgTime);
			if (message.hasExtension(MessageTypeExtension.NAMESPACE)) {	//有消息类型的扩展，说明该消息含有附件
				MessageTypeExtension typeExtension = message.getExtension(MessageTypeExtension.ELEMENT, MessageTypeExtension.NAMESPACE);
				if (typeExtension != null) {
					MsgPart msgPart = new MsgPart();
					String filename = typeExtension.getFileName();
					msgPart.setCreationDate(msgTime);
					msgPart.setFileName(filename);
					msgPart.setSize(typeExtension.getSize());
					msgPart.setFileToken(typeExtension.getFileId());
					msgPart.setMimeType(typeExtension.getMimeType());
					
					String saveName = SystemUtil.generateChatAttachFilename(msgTime);
					//设置附件
					String savePath = SystemUtil.generateChatAttachFilePath(threadId, saveName);
					msgPart.setFilePath(savePath);
					
					
					msgPart.setDesc(typeExtension.getDesc());
					
					MsgInfo.Type msgType = typeExtension.getMsgType();
					
					msgInfo.setMsgType(msgType);
					msgInfo.setMsgPart(msgPart);
					
					//设置下载文件的参数
					switch (msgType) {
					case IMAGE:	//先下载缩略图
						String thumbName = typeExtension.getThumbName();
						String thumbPath = SystemUtil.generateChatThumbAttachFilePath(threadId, SystemUtil.generateChatThumbAttachFilename(saveName));
						
						msgPart.setThumbName(thumbName);
						msgPart.setThumbPath(thumbPath);
						break;
					default:
						break;
					}
				}
			} else {	//普通文本消息
				msgInfo.setMsgType(MsgInfo.Type.TEXT);
			}
			msgInfo.setRead(false);
			msgInfo.setSubject(message.getSubject());
			msgInfo.setSendState(SendState.SUCCESS);
			msgInfo.setToUser(ChatApplication.getInstance().getCurrentAccount());
			return msgInfo;
		}
		return null;
	}

	/**
	 * 下载消息的文件
	 * @param msgInfo 消息实体
	 */
	private void downloadMsgFile(MsgInfo msgInfo, Context context) {
		if (msgInfo != null) {
			MsgPart msgPart = msgInfo.getMsgPart();
			if (msgPart != null) {
				MsgInfo.Type msgType = msgInfo.getMsgType();
				int fileType = -1;
				switch (msgType) {
					case IMAGE:	//先下载缩略图

						fileType = Constants.FILE_TYPE_THUMB;
						break;
					case LOCATION:	//地理位置,下载原始图片，地理位置不存在缩略图
					case VOICE:	//语音
					case VCARD:	//电子名片
						fileType = Constants.FILE_TYPE_ORIGINAL;
						break;

					default:
						break;
				}
				if (fileType != -1) {
					PhotoItem downloadItem = new PhotoItem();
					downloadItem.setThumbPath(msgPart.getThumbPath());
					downloadItem.setFilePath(msgPart.getFilePath());
					downloadItem.setMsgId(msgInfo.getMsgId());
					downloadItem.setFileToken(msgPart.getFileToken());
					downloadItem.setDownloadType(fileType);
					
					MsgEngine msgEngine = new MsgEngine(context);
					msgEngine.downloadFile(mDownloadManager, downloadItem, new MsgAttachDownloadListener(msgInfo));
				}
			}
		}
	}

	/**
	 * 消息附件的下载监听器
	 */
	class MsgAttachDownloadListener implements DownloadListener {
		private MsgInfo msgInfo;
		
		public MsgAttachDownloadListener(MsgInfo msgInfo) {
			this.msgInfo = msgInfo;
		}

		@Override
		public void onStart(int downloadId, long totalBytes) {
			
		}

		@Override
		public void onRetry(int downloadId) {

		}

		@Override
		public void onProgress(int downloadId, long bytesWritten, long totalBytes) {

		}

		@Override
		public void onSuccess(int downloadId, String filePath) {
			Log.d("---------文件下载成功-----downloadId-----" + downloadId + "-------------" + filePath);
			msgManager.notifyObservers(Provider.MsgInfoColumns.NOTIFY_FLAG, Observer.NotifyType.UPDATE, msgInfo);
//								Intent intent = new Intent(ChatActivity.MsgProcessReceiver.ACTION_REFRESH_MSG);
//								sendBroadcast(intent);
		}

		@Override
		public void onFailure(int downloadId, int statusCode, String errMsg) {
			Log.w("-----download msg part failed-----downloadId--------" + statusCode + "----------" + errMsg);
			// TODO Auto-generated method stub
//								Intent intent = new Intent(ChatActivity.MsgProcessReceiver.ACTION_REFRESH_MSG);
//								sendBroadcast(intent);
		}
	}
	
	/**
	 * 接收离线消息，要等好友都同步好了后才能接收该离校消息
	 * @author tiger
	 * @update 2015/12/6 11:19
	 * @version 1.0.0
	 */
	private void receiveOffineMsg() {
		SystemUtil.getCachedThreadPool().execute(new HandleOffineMsgTask());
	}
	
	/**
	 * 同步所有好友列表的任务线程
	 * @author coolpad
	 *
	 */
	class SyncFriendsTask implements Runnable {

		@Override
		public void run() {
			AbstractXMPPConnection connection = XmppConnectionManager.getInstance().getConnection();
			if (XmppUtil.checkAuthenticated(connection)) {
				//1、先从服务器上获取所有的好友列表
				XmppUtil.loadFriends(connection, new RosterLoadedCallback() {
					
					@Override
					public void loadSuccessful(List<User> userList) {
						if (!SystemUtil.isEmpty(userList)) {
							//2、更新本地数据库
							userManager.updateFriends(userList);
							
//							Intent intent = new Intent(LoadDataBroadcastReceiver.ACTION_USER_LIST);
//							sendBroadcast(intent);
							
							//接收离线消息
							receiveOffineMsg();

							//同步好友的电子名片信息
							SystemUtil.getCachedThreadPool().execute(new SyncFriendVcardTask(userList));
						}
					}
				});
				
				
			}
		}
		
	}
	
	/**
	 * 同步好友的电子名片信息
	 * @author tiger
	 * @update 2015年3月1日 下午6:13:36
	 *
	 */
	class SyncFriendVcardTask implements Runnable {
		List<User> users;

		public SyncFriendVcardTask(List<User> users) {
			super();
			this.users = users;
		}

		@Override
		public void run() {
			if (SystemUtil.isNotEmpty(users)) {
				StringBuilder sb = new StringBuilder();
				Map<String, UserVcard> map = new HashMap<>();
				for (User user : users) {
					String name = user.getUsername();
					if (!TextUtils.isEmpty(name)) {
						sb.append(name).append(",");
						UserVcard vcard = user.getUserVcard();
						if (vcard == null) {
							vcard = new UserVcard();
							vcard.setUserId(user.getId());
						}
						map.put(name, vcard);
					}
				}
				if (sb.length() > 1) {
					sb.deleteCharAt(sb.length() - 1);
				}
				//3、更新好友的头像等基本信息
				UserEngine userEngine = new UserEngine(mContext);
				userEngine.getSimpleVcardInfos(sb.toString(), map);
//				users = XmppUtil.syncFriendsVcard(connection, users);
//				userManager.updateFriends(users);
//				Intent intent = new Intent(LoadDataBroadcastReceiver.ACTION_USER_INFOS);
//				sendBroadcast(intent);
			}
		}
		
	}
	
	/**
	 * 同步好友基本的电子名片信息，紧包含头像，昵称，先获取头像的hash,与本地对比后决定是否下载新的头像
	 * @author huanghui1
	 * @version 1.0.0
	 * @update 2015年7月30日 下午3:19:24
	 */
	class SyncFriendSimpleVcardTask implements Runnable {
		List<User> users;

		public SyncFriendSimpleVcardTask(List<User> users) {
			super();
			this.users = users;
		}

		@Override
		public void run() {
			if (SystemUtil.isNotEmpty(users)) {
				
			}
		}
		
	}
	
	
	/**
	 * 处理离线消息的任务
	 * @author huanghui1
	 * @update 2015年2月27日 下午5:17:15
	 */
	class HandleOffineMsgTask implements Runnable {

		@Override
		public void run() {
			AbstractXMPPConnection connection = XmppConnectionManager.getInstance().getConnection();
			if (connection != null && connection.isConnected()) {
				//1、先从服务器上获取所有的好友列表
				try {
					if (mOfflineMessageManager != null && mOfflineMessageManager.supportsFlexibleRetrieval()) {
						int msgCount = mOfflineMessageManager.getMessageCount();
						if (msgCount > 0) {	//有离线消息
							if (msgCount > 1) {	//有多条离线消息
								if (mChatMessageListener != null) {
									mChatMessageListener.setNotify(false);
								}
							}
							//获取离线消息
							List<Message> offineMessges = mOfflineMessageManager.getMessages();
							
							//保存离线消息
							if (SystemUtil.isNotEmpty(offineMessges)) {
								mOfflineMessageManager.deleteMessages();	//上报服务器已获取，需删除服务器备份，不然下次登录会重新获取
								
								if (msgCount > 1 && !isChatActivityOnTop()) {	//聊天界面不在栈顶时才发送通知
									//离线消息处理完毕后再一起通知，避免过频繁的通知
									android.os.Message msg = mHandler.obtainMessage();
									msg.arg1 = msgCount;
									msg.what = Constants.MSG_RECEIVE_CHAT_MSG;
									mHandler.sendMessage(msg);
								}
							}
						}
					}
				} catch (NoResponseException | XMPPErrorException
						| NotConnectedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					//上报自己的状态为登录状态
					Presence presence = new Presence(Presence.Type.available);
					presence.setStatus("在线");
					presence.setPriority(1);
					presence.setMode(Presence.Mode.available);
					connection.sendStanza(presence);
				} catch (NotConnectedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			
			if (mChatMessageListener != null) {
				mChatMessageListener.setNotify(true);
			}
		}
		
	}
	
	/**
	 * 聊天消息管理器
	 * @author huanghui1
	 * @update 2014年11月10日 下午6:16:07
	 */
	public class MyChatManagerListener implements ChatManagerListener {

		@Override
		public void chatCreated(Chat chat, boolean createdLocally) {
			/*if (!createdLocally) {
				if (connection != null) {
					StanzaFilter packetFilter = new FlexibleStanzaTypeFilter<Message>() {
						@Override
						protected boolean acceptSpecific(Message packet) {
							return packet != null;
						}
					};
					connection.addAsyncStanzaListener(new MyChatMessageListener(), packetFilter);
				}
//				chat.addMessageListener(mChatMessageListener);
			}*/
		}
	}
	
	/**
	 * 消息监听器
	 * @author huanghui1
	 * @update 2014年11月20日 下午8:41:17
	 */
	public class MyChatMessageListener implements /*ChatMessageListener*/ StanzaListener {
		/**
		 * 是否通知,默认为true
		 */
		private boolean notify = true;
		
		public void setNotify(boolean notify) {
			this.notify = notify;
		}

		@Override
		public void processPacket(Stanza packet) throws NotConnectedException {
			if (packet != null) {
				if (packet instanceof Message) {	//消息类型
					Message message = (Message) packet;
					ChatState state = getChatState(message);
					if (state != null && isChatActivityOnTop()) {	//有消息状态的改变，且界面是chat界面
						if (state == ChatState.active) {
							SystemUtil.getCachedThreadPool().execute(new ProcessMsgTask(message, notify));
						} else {
							SystemUtil.getCachedThreadPool().execute(new ProcessStateMsgTask(state, message));
						}
					} else {
//						if (DeliveryReceipt.from(message) == null) {	//不是回执消息才处理
						if (state == null || state == ChatState.active) {
							SystemUtil.getCachedThreadPool().execute(new ProcessMsgTask(message, notify));
						}
//						}
					}
				}
			}
		}

		/*@Override
		public void processMessage(Chat chat, Message message) {
			if (message != null) {
				ChatState state = getChatState(message);
	            if (state != null && isChatActivityOnTop()) {	//有消息状态的改变，且界面是chat界面
	            	if (state == ChatState.active) {
	            		SystemUtil.getCachedThreadPool().execute(new ProcessMsgTask(chat, message, notify));
	            	} else {
	            		SystemUtil.getCachedThreadPool().execute(new ProcessStateMsgTask(state, message));
	            	}
	            } else {
//				if (DeliveryReceipt.from(message) == null) {	//不是回执消息才处理
	            	if (state == null || state == ChatState.active) {
	            		SystemUtil.getCachedThreadPool().execute(new ProcessMsgTask(chat, message, notify));
	            	}
//				}
	            }
			}
		}*/
		
	}
	
	/**
	 * 获取消息的状态，没有则返回null
	 * @param message
	 * @return
	 * @update 2015年9月12日 下午3:18:32
	 */
	private ChatState getChatState(Message message) {
		ExtensionElement extension = message.getExtension(ChatStateManager.NAMESPACE);
		ChatState state = null;
        if (extension != null) {	//有消息状态的改变
            try {
                state = ChatState.valueOf(extension.getElementName());
            } catch (Exception ex) {
                Log.e(ex.getMessage());
            }
        }
        return state;
	}
	
	/**
	 * 文件接收的监听器
	 * @author huanghui1
	 * @update 2014年11月20日 下午2:32:01
	 */
	class MyFileTransferListener implements FileTransferListener {

		@Override
		public void fileTransferRequest(FileTransferRequest request) {
			MsgInfo msgInfo = processFileMessage(request);
			msgInfo = msgManager.addMsgInfo(msgInfo, true);
			int threadId = msgInfo.getThreadID();
			MsgThread msgThread = msgManager.getThreadById(threadId);
			if (msgThread != null) {
				msgThread.setModifyDate(System.currentTimeMillis());
				msgThread.setSnippetId(msgInfo.getMsgId());
//				MsgInfo.Type msgType = msgInfo.getMsgType();
				String snippetContent = msgInfo.getMsgPart().getFileName()/*msgManager.getSnippetContentByMsgType(msgType, msgInfo)*/;
				msgThread.setSnippetContent(snippetContent);
				int unReadCount = msgThread.getUnReadCount();
				unReadCount += 1;
				msgThread.setUnReadCount(unReadCount);
				msgThread.setLastMsgInfo(msgInfo);
				msgManager.updateSnippet(msgThread);
				
				Intent intent = new Intent(ChatActivity.MsgProcessReceiver.ACTION_PROCESS_MSG);
				intent.putExtra(ChatActivity.ARG_MSG_INFO, msgInfo);
				sendBroadcast(intent);
				
				SystemUtil.getCachedThreadPool().execute(new ReceiveFileTask(request, msgInfo));
			}
		}
		
	}
	
	/**
	 * 处理文件类型的消息
	 * @update 2014年11月20日 下午2:57:06
	 * @param request
	 * @return
	 */
	private MsgInfo processFileMessage(FileTransferRequest request) {
		//获得发送人的账号，不包含完整的jid
		String fromUser = SystemUtil.unwrapJid(request.getRequestor());
		MsgInfo msgInfo = new MsgInfo();
		msgInfo.setComming(true);
		String description = request.getDescription();
		String desc = description;
		int type = -1;
		if (!TextUtils.isEmpty(description)) {
			int index = description.lastIndexOf(Constants.SPLITE_TAG_MSG_TYPE);
			if (index != -1) {
				desc = description.substring(0, index);
				try {
					type = Integer.parseInt(description.substring(index + 1));
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
		}
		msgInfo.setCreationDate(System.currentTimeMillis());
		msgInfo.setFromUser(fromUser);
		String mimeType = null;
		String subJect = null;
		//设置附件
		MsgPart msgPart = new MsgPart();
		if (type == MsgInfo.Type.VOICE.ordinal()) {	//语音消息
			msgInfo.setMsgType(MsgInfo.Type.VOICE);
			mimeType = MimeUtils.MIME_TYPE_AUDIO_AMR;
		} else if (type == MsgInfo.Type.LOCATION.ordinal()) {	//地理位置的消息
			msgInfo.setMsgType(MsgInfo.Type.LOCATION);
			mimeType = MimeUtils.MIME_TYPE_IMAGE_JPG;
			String[] array = desc.split(Constants.SPLITE_TAG_MSG_TYPE);
			if (SystemUtil.isNotEmpty(array)) {
				try {
					desc = array[0];
					subJect = array[1];
					msgPart.setDesc(subJect);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else {
			//TODO 类型匹配
			//获得文件的后缀名，不包含".",如mp3
			String subfix = SystemUtil.getFileSubfix(request.getFileName()).toLowerCase(Locale.getDefault());;
			//获得文件的mimetype，如image/jpeg
			mimeType = MimeUtils.guessMimeTypeFromExtension(subfix);
			mimeType = (mimeType == null) ? request.getMimeType() : mimeType;
			
			MsgInfo.Type msgType = SystemUtil.getMsgInfoType(subfix, mimeType);
			
			msgInfo.setMsgType(msgType);
		}
		msgInfo.setContent(desc);
		msgInfo.setRead(false);
		msgInfo.setSubject(subJect);
		msgInfo.setSendState(SendState.SUCCESS);
		msgInfo.setToUser(ChatApplication.getInstance().getCurrentAccount());
		
		int threadId = msgManager.getThreadIdByMembers(fromUser);	//查找本地会话，如果没有就创建
		if (threadId > 0) {
			msgInfo.setThreadID(threadId);
		}
		
		msgPart.setCreationDate(System.currentTimeMillis());
		msgPart.setFileName(request.getFileName());
		msgPart.setSize(request.getFileSize());
		msgPart.setMimeType(mimeType);
		String savePath = SystemUtil.generateChatAttachFilePath(threadId, msgPart.getFileName());
		msgPart.setFilePath(savePath);
		
		msgInfo.setMsgPart(msgPart);
		
		return msgInfo;
	}
	
	/**
	 * 根据通知id清除通知栏
	 * @update 2015年3月3日 下午2:05:56
	 * @param nofifyId 通知的id
	 */
	public void clearNotify(int nofifyId) {
		if (mNotificationManager == null) {
			mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);;
		}
		mNotificationManager.cancel(nofifyId);
	}
	
	/**
	 * 清除全部通知
	 * @update 2015年3月3日 下午2:07:11
	 */
	public void clearAllNotify() {
		if (mNotificationManager == null) {
			mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);;
		}
		mNotificationManager.cancelAll();
	}
	
	/**
	 * 接收文件的线程
	 * @author huanghui1
	 * @update 2014年11月20日 下午2:47:03
	 */
	class ReceiveFileTask implements Runnable {
		private FileTransferRequest request;
		private MsgInfo msgInfo;

		public ReceiveFileTask(FileTransferRequest request, MsgInfo msgInfo) {
			super();
			this.request = request;
			this.msgInfo = msgInfo;
		}

		@Override
		public void run() {
			IncomingFileTransfer fileTransfer = request.accept();
			String filePath = msgInfo.getMsgPart().getFilePath();
			File saveFile = new File(filePath);
			if (!saveFile.getParentFile().exists()) {
				saveFile.getParentFile().mkdirs();
			}
			try {
				fileTransfer.recieveFile(saveFile);
				while (!fileTransfer.isDone()) {
//					Log.d("------recieveFile-----------" + fileTransfer.getStatus() + "---" + fileTransfer.getProgress());
//					FileTransfer.Status status = fileTransfer.getStatus();
//					if (status ==)
				}
				Intent intent = new Intent(ChatActivity.MsgProcessReceiver.ACTION_REFRESH_MSG);
//				MsgInfo.Type msgType = msgInfo.getMsgType();
//				if (msgType == MsgInfo.Type.IMAGE || msgType == MsgInfo.Type.LOCATION) {	//消息是图片或者是地理位置，则需刷新图片的显示
//					intent.putExtra(ChatActivity.MsgProcessReceiver.ARG_FILE_PATH, filePath);
//				}
				sendBroadcast(intent);
			} catch (SmackException | IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
//	/**
//	 * 发送聊天消息的广播
//	 * @author huanghui1
//	 * @update 2014年11月17日 下午8:20:57
//	 */
//	public class SendChatMessageReceiver extends BroadcastReceiver {
//		public static final String ACTION_SEND_CHAT_MSG = "net.ibaixin.chat.SEND_CHAT_MSG";
//
//		@Override
//		public void onReceive(Context context, Intent intent) {
//			if (ACTION_SEND_CHAT_MSG.equals(intent.getAction())) {	//发送消息的广播
//				MsgInfo msgInfo = intent.getParcelableExtra(ChatActivity1.ARG_MSG_INFO);
//				if (msgInfo != null) {
////					sendChatMsg(senderInfo);
//				}
//			}
//		}
//		
//	}
	
	public class MainBinder extends Binder {
		public CoreService getService() {
			return CoreService.this;
		}
	}
	
	/**
	 * 判断聊天界面是否在栈顶
	 * @update 2015年2月28日 上午11:47:36
	 * @return
	 */
	public boolean isChatActivityOnTop() {
		if (mActivityManager == null) {
			mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
		}
		RunningTaskInfo info = mActivityManager.getRunningTasks(1).get(0);
		String className = info.topActivity.getClassName();
		if (!TextUtils.isEmpty(className)) {
			return ChatActivity.class.getCanonicalName().equals(className);
		}
		return false;
	}
	
	/**
	 * 核心的广播，主要用来在{@link CoreService}中处理各种后台任务
	 * @author tiger
	 * @version 2015年3月14日 下午11:56:52
	 */
	public class CoreReceiver extends BroadcastReceiver {
		
		public static final String ACTION_UPDATE_VCARD = "net.ibaixin.chat.UPDATE_VCARD_RECEIVER";
		public static final String ACTION_SYNC_VCARD = "net.ibaixin.chat.SYNC_VCARD_RECEIVER";
		//需要重新验证登录
		public static final String ACTION_RELOGIN = "net.ibaixin.chat.ACTION_RELOGIN";
		
		public static final String ARG_UPDATE_VCARD_TYPE = "arg_update_vcard_type";
		
		public static final String ARG_UPDATE_VCARD_AVATAR = "ARG_UPDATE_VCARD_AVATAR";
		

		@Override
		public void onReceive(Context context, final Intent intent) {
			String action = intent.getAction();
			final UpdateVcardType updateType = UpdateVcardType.valueOf(intent.getIntExtra(ARG_UPDATE_VCARD_TYPE, 0));
			Runnable task = null;
			switch (action) {
			case ACTION_SYNC_VCARD:	//同步个人电子名片，一般用于首次登录或者手动更新
				task = new Runnable() {
					
					@Override
					public void run() {
						boolean success = XmppUtil.syncPersonalInfo(connection, ChatApplication.getInstance().getCurrentUser());
						if (success) {	//同步成功
							Log.d("----ACTION_SYNC_VCARD--同步电子名片成功----");
						} else {
							Log.e("----ACTION_SYNC_VCARD--同步电子名片失败----");
						}
					}
				};
				break;
			case ACTION_UPDATE_VCARD:	//更新个人的电子名片
				task = new Runnable() {
					
					@Override
					public void run() {
						android.os.Message msg = mHandler.obtainMessage();
						try {
							switch (updateType) {
							case AVATAR:	//更新图像
								String iconPath = intent.getStringExtra(ARG_UPDATE_VCARD_AVATAR);
								XmppUtil.updateAvatar(connection, iconPath);
								break;

							default:
								break;
							}
						} catch (NoResponseException | XMPPErrorException
								| NotConnectedException e) {
							msg.what = Constants.MSG_UPDATE_FAILED;
							e.printStackTrace();
						}
						mHandler.sendMessage(msg);
					}
				};
				break;
			case ACTION_RELOGIN:
				Timer timer = new Timer();
				timer.schedule(new ReConnectTask(), 2000);
				break ;
			default:
				break;
			}
			if (task != null) {
				SystemUtil.getCachedThreadPool().execute(task);
			}
		}

	}
	
	/**
	 * 更新电子名片的类型
	 * @author tiger
	 * @update 2015年3月15日 上午12:09:27
	 *
	 */
	enum UpdateVcardType {
		/**
		 * 图像
		 */
		AVATAR,
		/**
		 * 昵称
		 */
		NICKNAME,
		/**
		 * 地址
		 */
		ADDRESS,
		/**
		 * 性别
		 */
		SEX;
		
		public static UpdateVcardType valueOf(int value) {
			switch (value) {
			case 0:	//图像
				return AVATAR; 
			case 1:
				return NICKNAME;
			case 2:
				return ADDRESS;
			case 3:
				return SEX;
			default:
				throw new IllegalArgumentException("参数不对");
			}
		}
	}
	
	/***
	 * 更新昵称 add by dudejin
	 * @param nickname
	 */
	private void updateNickOnRegister(final String nickname) {
		SystemUtil.getCachedThreadPool().execute(new Runnable() {
			
			@Override
			public void run() {
				try {
					AbstractXMPPConnection connection = XmppConnectionManager.getInstance().getConnection();
					//通知好友更新头像
					if (XmppUtil.checkAuthenticated(connection)) {
						XmppUtil.updateNickname(connection, nickname);
						ChatApplication.getInstance().getCurrentUser().setNickname(nickname);
						//保存昵称信息到本地数据库
						PersonalManage.getInstance().updateNickname(ChatApplication.getInstance().getCurrentUser());
						//刷新“我”的界面昵称显示
						Intent intent = new Intent(BasePersonalInfoReceiver.ACTION_REFRESH_PERSONAL_INFO);
						sendBroadcast(intent);
					}
				} catch (NoResponseException | XMPPErrorException | NotConnectedException e) {
					Log.e("updateNickOnRegister", e.toString());
				}
			}
		});
	}
}
