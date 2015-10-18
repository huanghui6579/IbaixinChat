package net.ibaixin.chat.receiver;

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;

import org.jivesoftware.smack.AbstractXMPPConnection;

import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.service.CoreService;
import net.ibaixin.chat.task.ReConnectTask;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.XmppConnectionManager;
import net.ibaixin.chat.util.XmppUtil;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
/**
 * 网络监听
 * @author ddj
 *
 */
public class NetworkReceiver extends BroadcastReceiver implements ReConnectTask.LoginCallBack {
	/**
	 * 检测网络的广播
	 */
	public static final String ACTION_CHECK_NETWORK = "net.ibaixin.chat.receiver.ACTION_CHECK_NETWORK";
	
	private static List<NetworkChangeCallback> mNetworkCallbackList = new LinkedList<>();
	private long timeDelay = 2000;
	private int loginTime = 0;
	
	// 侦听网络状态的变化
	@Override
	public void onReceive(final Context context, Intent intent) {
		String action = intent.getAction();
		Log.d("-----NetworkReceiver---onReceive---action--" + action);
		boolean isNetOk = false;
		switch (action) {
		case ConnectivityManager.CONNECTIVITY_ACTION:	//网络改变
			isNetOk = checkNetwork(context);
			AbstractXMPPConnection connection = XmppConnectionManager.getInstance().getConnection();
		if (connection != null && (!XmppUtil.checkConnected(connection) || !XmppUtil.checkAuthenticated(connection))) {	//当前用户没有登录
				Timer timer = new Timer();
				ReConnectTask connectTask = new ReConnectTask(this);
				if(!isNetOk) {	//网络不可用
					connectTask.cancel();
					timer.purge();
				} else {
					timer.schedule(connectTask, timeDelay);
				}
			}
			break;
		case ACTION_CHECK_NETWORK:	//检测网络
			isNetOk = checkNetwork(context);
			break;
		default:
			break;
		}
		for (NetworkChangeCallback callback : mNetworkCallbackList) {
			callback.handlerNetworkChanged(isNetOk);
		}
	}
	
	/**
	 * 添加一个回调
	 * @author tiger
	 * @update 2015年3月28日 上午11:05:21
	 * @param callback
	 */
	public static void attachNetworkCallback(NetworkChangeCallback callback) {
		mNetworkCallbackList.add(callback);
	}
	
	/**
	 * 删除一个回调
	 * @author tiger
	 * @update 2015年3月28日 上午11:05:21
	 * @param callback
	 */
	public static void detachNetworkCallback(NetworkChangeCallback callback) {
		mNetworkCallbackList.remove(callback);
	}

	/**
	 * 检查网络
	 * @author tiger
	 * @update 2015年3月28日 上午10:10:00
	 * @param context
	 * @return
	 */
	public static boolean checkNetwork(Context context) {// 获取手机所有连接管理对象（包括对wi-fi,net等连接的管理）
		boolean flag = false;
		try {
			ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			if (null != connectivity) {
				// 获取网络连接管理的对象
				NetworkInfo info = connectivity.getActiveNetworkInfo();
				if (null != info && info.isConnected()) {
					// 判断当前网络是否已经连接
					if (info.getState() == NetworkInfo.State.CONNECTED) {
						flag = true;
					}
				}
			}
		} catch (Exception e) {
			Log.e(e.getMessage());
		}
		ChatApplication app = (ChatApplication) context.getApplicationContext();
		app.setNetWorking(flag);
		return flag;
	}
	
	/**
	 * 网络状态改变的监听器
	 * @author tiger
	 * @update 2015年3月28日 上午10:34:49
	 *
	 */
	public interface NetworkChangeCallback {
		/**
		 * 网络状态的处理方法<br />
		 * <b>注：该方法在主线程中执行，所以一些耗时的操作需要到子线程里执行</b>
		 * @author tiger
		 * @update 2015年3月28日 上午10:35:14
		 * @param networkAvailable 网络是否可用
		 */
		public void handlerNetworkChanged(boolean networkAvailable);
	}

	@Override
	public void onLoginSuccessful() {
		Log.d("----onLoginSuccessful--重新登录成功--");
		//接收离线消息
		Intent service = new Intent(ChatApplication.getInstance(), CoreService.class);
//				service.putExtra(CoreService.FLAG_RECEIVE_OFFINE_MSG, CoreService.FLAG_RECEIVE_OFFINE);
		service.putExtra(CoreService.FLAG_SYNC, CoreService.FLAG_RELOGIN_OK);
		ChatApplication.getInstance().startService(service);
	}

	@Override
	public void onLoginFailed(Exception e) {
		if (loginTime < ReConnectTask.RECONNECT_TIME) {	//重连3次
			Timer timer = new Timer();
			ReConnectTask connectTask = new ReConnectTask(this);
			timer.schedule(connectTask, timeDelay);
			
			loginTime ++;
		}
	}
}
