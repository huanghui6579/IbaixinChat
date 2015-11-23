package net.ibaixin.chat.listener;

import android.content.Intent;

import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.service.CoreService;
import net.ibaixin.chat.task.ReConnectTask;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.XmppConnectionManager;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException.AlreadyLoggedInException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.StreamErrorException;
import org.jivesoftware.smack.packet.StreamError;

import java.util.Timer;

/**
 * 客户端连接监听器
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年11月8日 下午2:54:16
 */
public class ChatConnectionListener implements ConnectionListener, ReConnectTask.LoginCallBack {
	private Timer mTimer;
	private AbstractXMPPConnection mConnection = XmppConnectionManager.getInstance().getConnection();
	private long timeDelay = 2000;
	private int loginTime = 0;

	@Override
	public void connected(XMPPConnection connection) {
		// TODO Auto-generated catch block
		Log.d("----ChatConnectionListener-----connected------------");
	}

	@Override
	public void connectionClosed() {
		Log.d("----ChatConnectionListener-----connectionClosed------------");
		ChatApplication application = ChatApplication.getInstance();
		if (!application.isSureExit()) {	//确定是手动退出的
			if (loginTime < ReConnectTask.RECONNECT_TIME) {
				// TODO Auto-generated method stub
				mConnection.disconnect();
				mTimer = new Timer();
				mTimer.schedule(new ReConnectTask(this), timeDelay);
			}
		}
	}

	@Override
	public void connectionClosedOnError(Exception e) {
		Log.d("----ChatConnectionListener-----connectionClosedOnError------------" + e.getMessage());
		// TODO Auto-generated method stub
		if (!(e instanceof AlreadyLoggedInException)) {	//账号没有登录
			if (e instanceof StreamErrorException) {
                StreamErrorException xmppEx = (StreamErrorException) e;
                StreamError error = xmppEx.getStreamError();

                if (StreamError.Condition.conflict == error.getCondition()) {
                    return;
                }
            }
			if (loginTime < ReConnectTask.RECONNECT_TIME) {
				mConnection.disconnect();
				mTimer = new Timer();
				mTimer.schedule(new ReConnectTask(this), timeDelay);
				
				loginTime ++;
			}
		}
	}

	@Override
	public void reconnectingIn(int seconds) {
		// TODO Auto-generated method stub
		Log.d("----ChatConnectionListener-----reconnectingIn------------" + seconds);
	}

	@Override
	public void reconnectionSuccessful() {
		loginTime = 0;
		if (mTimer != null) {
			mTimer.cancel();
		}
		// TODO Auto-generated method stub
		Log.d("----ChatConnectionListener-----reconnectionSuccessful------------");
	}

	@Override
	public void reconnectionFailed(Exception e) {
		Log.d("----ChatConnectionListener-----reconnectionFailed------------" + e.getMessage());
		if (loginTime < ReConnectTask.RECONNECT_TIME) {	//重连3次
			mConnection.disconnect();
			mTimer = new Timer();
			mTimer.schedule(new ReConnectTask(this), timeDelay);
			
			loginTime ++;
		}
	}

	@Override
	public void authenticated(XMPPConnection connection, boolean resumed) {
		// TODO Auto-generated method stub
		/*if (!resumed) {	//没有登录，则登录
			if (loginTime < ReConnectTask.RECONNECT_TIME) {	//重连3次
				mConnection.disconnect();
				mTimer = new Timer();
				mTimer.schedule(new ReConnectTask(this), timeDelay);
				
				loginTime ++;
			}
		}*/
		Log.d("----ChatConnectionListener-----authenticated------------" + resumed);
	}

	@Override
	public void onLoginSuccessful() {
		Log.d("----ChatConnectionListener-----onLoginSuccessful------------");
		loginTime = 0;
		//接收离线消息
		Intent service = new Intent(ChatApplication.getInstance(), CoreService.class);
//		service.putExtra(CoreService.FLAG_RECEIVE_OFFINE_MSG, CoreService.FLAG_RECEIVE_OFFINE);
		service.putExtra(CoreService.FLAG_SYNC, CoreService.FLAG_RELOGIN_OK);
		ChatApplication.getInstance().startService(service);
	}

	@Override
	public void onLoginFailed(Exception e) {
		Log.d("----ChatConnectionListener-----onLoginFailed------------" + e.getMessage());
		reconnectionFailed(e);
	}
	
}
