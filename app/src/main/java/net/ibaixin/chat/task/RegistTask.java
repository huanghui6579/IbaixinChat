package net.ibaixin.chat.task;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.R;
import net.ibaixin.chat.activity.LoginActivity;
import net.ibaixin.chat.activity.MainActivity;
import net.ibaixin.chat.model.SystemConfig;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.StreamTool;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.util.XmppConnectionManager;
import net.ibaixin.chat.view.ProgressDialog;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FlexibleStanzaTypeFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.StanzaIdFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.Bind;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.iqregister.packet.Registration;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
/**
 * 注册的后台任务
 * @author Administrator
 * @update 2014年10月7日 下午4:56:24
 * @update 2015年03月21日  by dudejin
 * 
 */
public class RegistTask extends AsyncTask<SystemConfig, Void, Integer> {
	private static final String TAG = "RegistTask";
	private static final int REGIST_RESULT_SUCCESS = 1;
	private static final int REGIST_RESULT_CONFLICT = 2;	//账号已存在
	private static final int REGIST_RESULT_FAIL = 3;
	
	private ProgressDialog pDialog;
	private Context mContext;
	
	public RegistTask(Context mContext){
		this.mContext = mContext ;
	}
	@Override
	protected void onPreExecute() {
		if (pDialog == null) {
			pDialog = ProgressDialog.show(mContext, null, mContext.getString(R.string.registing), true);
		} else {
			pDialog.show();
		}
	}

	@Override
	protected Integer doInBackground(SystemConfig... params) {
		SystemConfig systemConfig = params[0];
		String account = systemConfig.getAccount();
		String password = systemConfig.getPassword();
		String nick = systemConfig.getNickname() ;
		String email = systemConfig.getEmail() ;
		return regist(account,password, nick,email);
	}
	
	@Override
	protected void onPostExecute(Integer result) {
		if(pDialog != null && pDialog.isShowing()) {
			pDialog.dismiss();
		}
		switch (result) {
		case REGIST_RESULT_SUCCESS:	//注册成功
			//保存用户信息
			ChatApplication.getInstance().getSystemConfig().setOnline(true);
			ChatApplication.getInstance().getSystemConfig().setFirstLogin(false);
			if(LoginActivity.isLocalAccountLogin)
				ChatApplication.getInstance().saveSystemConfig();
			Intent intent = new Intent(mContext, MainActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			mContext.startActivity(intent);
			break;
		case REGIST_RESULT_FAIL:	//失败
			SystemUtil.makeLongToast(R.string.regist_failed);
			break;
		case REGIST_RESULT_CONFLICT:	//用户已存在
			SystemUtil.makeLongToast(R.string.regist_account_conflict);
			break;
		default:
			break;
		}
	}
	
	/**
	 * 账号注册
	 * @author Administrator
	 * @update 2014年10月7日 下午6:00:55
	 * @param connection
	 * @param config
	 * @return
	 */
	public static int regist(String username,String password ,String nickName,String email) {
		try {
			AbstractXMPPConnection connection = XmppConnectionManager.getInstance().getConnection();
			connection.connect();
			connection.addAsyncStanzaListener(new StanzaListener() {
				
				@Override
				public void processPacket(Stanza packet) throws NotConnectedException {
					if (packet instanceof Bind) {
						Bind bind = (Bind) packet;
						bind.getJid();
					}
				}
			}, new AndFilter(new StanzaTypeFilter(Bind.class), new FlexibleStanzaTypeFilter<IQ>() {
				
				@Override
				protected boolean acceptSpecific(IQ iq) {
					// TODO Auto-generated method stub
					return iq.getType().equals(IQ.Type.result);
				}
			}));
			
			Map<String, String> attr = new HashMap<String, String>();
			attr.put("username", username);
			attr.put("password", password);
			attr.put("name", nickName);
			attr.put("email", email);
			Registration registration = new Registration(attr);
			registration.setType(IQ.Type.set);
			registration.setTo(connection.getServiceName());
			StanzaFilter filter = new AndFilter(new StanzaIdFilter(registration.getStanzaId()), new StanzaTypeFilter(IQ.class));
			PacketCollector collector = connection.createPacketCollector(filter);
			connection.sendStanza(registration);
			IQ result = (IQ) collector.nextResult(SmackConfiguration.getDefaultPacketReplyTimeout());
			collector.cancel();
			if (result == null) {
				Log.d("regist failed");
				return REGIST_RESULT_FAIL;
			} else if (IQ.Type.result == result.getType()) {
				if(!registerIbaixinJoke(nickName,username,password)){//注册web服务器 add by dudejin 2015-03-06
					return REGIST_RESULT_FAIL;
				}
				//如果该账号没有对应的数据库，则根据账号创建对应的数据库，并设置当前的账号数据库
				SystemUtil.initAccountDbDir(username);
				if(!connection.isConnected()) {
					connection.connect();
				}
				connection.login(username, password, Constants.CLIENT_RESOURCE);
				return REGIST_RESULT_SUCCESS;
			} else {
				if("conflict".equalsIgnoreCase(result.getError().toString())) {
					Log.d("regist conflict");
					return REGIST_RESULT_CONFLICT;
				} else {
					Log.d("regist error");
					return REGIST_RESULT_FAIL;
				}
			}
		} catch (SmackException | IOException | XMPPException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return REGIST_RESULT_FAIL;
	}
	
	/**
	 * 注册web服务器
	 * 	add by dudejin 2015-03-06
	 * @return
	 */
	public static boolean registerIbaixinJoke(String nickname,String account,String pass) {
		try {
			String urlstr = Constants.registerUrl+"?loginName="
					+account +"&loginPassword="+pass
					+"&realName="+URLEncoder.encode(nickname,"utf-8") ;
			String json = StreamTool.connectServer(urlstr);;
			if("Y".equals(json)){
					return true ;
			}
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}
		return false;
	}
}