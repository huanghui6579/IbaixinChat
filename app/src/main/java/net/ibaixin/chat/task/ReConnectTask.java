package net.ibaixin.chat.task;

import java.io.IOException;
import java.util.TimerTask;

import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.model.SystemConfig;
import net.ibaixin.chat.service.CoreService;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.XmppConnectionManager;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import android.content.Intent;

/**
 * 重新登录任务
 * @author Administrator
 * @update 2015年03月27日  by dudejin
 * 
 */
public class ReConnectTask extends TimerTask {
	private LoginCallBack mCallback;
	
	/**
	 * 重连次数，最多重连3次
	 */
	public static final int RECONNECT_TIME = 3;
	
	public ReConnectTask() {}
	
	public ReConnectTask(LoginCallBack callback) {
		this.mCallback = callback;
	}
	
	/**
	 * 设置回调
	 * @update 2015年6月26日 下午8:32:50
	 * @param callback
	 */
	public void setLoginCallback(LoginCallBack callback) {
		this.mCallback = callback;
	}
	
	@Override
	public void run() {
		Log.d("--------ReConnectTask------run--");
		SystemConfig systemConfig = ChatApplication.getInstance().getSystemConfig();
		String username = systemConfig.getAccount();
		String password = systemConfig.getPassword();
		if (username != null && password != null) {
			AbstractXMPPConnection connection = XmppConnectionManager.getInstance().getConnection();
			try {
				if (connection != null) {
					if (!connection.isConnected()) {
						connection.connect();
					}
					if (!connection.isAuthenticated()) {
						connection.login(username, password, Constants.CLIENT_RESOURCE);
					}
					if (mCallback != null) {
						mCallback.onLoginSuccessful();
					}
				}
			} catch (Exception e) {
				if (mCallback != null) {
					mCallback.onLoginFailed(e);
				}
				Log.e(e == null ? "" : e.getMessage());
			}
		}
	}
	
	/**
	 * 登录后的回调
	 * @author huanghui1
	 * @version 1.0.0
	 * @update 2015年6月26日 下午8:30:14
	 */
	public interface LoginCallBack {
		/**
		 * 登录成功
		 * @update 2015年6月26日 下午8:30:56
		 */
		public void onLoginSuccessful();
		
		/**
		 * 登录失败
		 * @update 2015年6月26日 下午8:31:12
		 */
		public void onLoginFailed(Exception e);
	}
}