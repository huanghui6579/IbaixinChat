package net.ibaixin.chat.util;

import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.listener.ChatConnectionListener;
import net.ibaixin.chat.listener.ChatPacketListener;
import net.ibaixin.chat.manager.PersonalManage;
import net.ibaixin.chat.model.Personal;
import net.ibaixin.chat.model.SystemConfig;
import net.ibaixin.chat.smack.extension.MessageTypeExtension;
import net.ibaixin.chat.smack.packet.VcardX;
import net.ibaixin.chat.smack.provider.MessageTypeProvider;
import net.ibaixin.chat.smack.provider.VcardXProvider;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.Roster.SubscriptionMode;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.debugger.android.AndroidDebugger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * xmpp服务器的连接管理器
 * @author Administrator
 * @update 2014年10月7日 上午9:37:01
 *
 */
public class XmppConnectionManager {
	private AbstractXMPPConnection connection;
	
	private Object lock = new Object();
	
	private static XmppConnectionManager instance = null;
	private static XMPPTCPConnectionConfiguration configuration;
	
	private ChatPacketListener mChatPacketListener;
	private ChatConnectionListener mChatConnectionListener;
	
	private XmppConnectionManager() {}
	
	public static XmppConnectionManager getInstance() {
		if (instance == null) {
			synchronized (XmppConnectionManager.class) {
				if (instance == null) {
					instance = new XmppConnectionManager();
				}
			}
		}
		return instance;
	}
	
	/**
	 * 初始化连接
	 * @param systemConfig
	 * @return
	 */
	public synchronized AbstractXMPPConnection init(SystemConfig systemConfig) {
		SmackConfiguration.DEBUG = true;
//		configuration = new XMPPTCPConnectionConfiguration(Constants.SERVER_HOST, Constants.SERVER_PORT, Constants.SERVER_NAME);
		configuration = XMPPTCPConnectionConfiguration.builder()
				.setServiceName(Constants.SERVER_NAME)
				.setHost(Constants.SERVER_HOST)
				.setPort(Constants.SERVER_PORT)
				.setSecurityMode(SecurityMode.disabled)
				.setSendPresence(false)	//不允许登录成功后更新状态，主要是为了接收离线消息才这样设置的
				.build();
		
		//允许自动连接
//		configuration.setReconnectionAllowed(true);
		//不允许登录成功后更新状态，主要是为了接收离线消息才这样设置的
		// 收到好友邀请后manual表示需要经过同意,accept_all表示不经同意自动为好友
		Roster.setDefaultSubscriptionMode(SubscriptionMode.manual);
		connection = new XMPPTCPConnection(configuration);
		connection.setPacketReplyTimeout(15000);	//毫秒为单位
		SASLAuthentication.unBlacklistSASLMechanism("PLAIN");
		SASLAuthentication.blacklistSASLMechanism("DIGEST-MD5");
//		ReconnectionManager.setEnabledPerDefault(true);
//		ReconnectionManager.getInstanceFor(connection);

		if (mChatPacketListener == null) {
			mChatPacketListener = new ChatPacketListener();
		}
		
		if (mChatConnectionListener == null) {
			mChatConnectionListener = new ChatConnectionListener();
		}
		
		//添加监听器
		connection.addConnectionListener(mChatConnectionListener);
		StanzaFilter packetFilter = new OrFilter(new StanzaTypeFilter(IQ.class), new StanzaTypeFilter(Presence.class));
		connection.addAsyncStanzaListener(mChatPacketListener, packetFilter);
		
		//初始化额外的扩展提供者
		initProvider();
		
		AndroidDebugger androidDebugger = new AndroidDebugger(connection, new PrintWriter(System.out), new InputStreamReader(System.in));
		System.setProperty("smack.debuggerClass", androidDebugger.getClass().getCanonicalName());
		return connection;
	}
	
	/**
	 * 初始化额外的扩展提供者
	 * @update 2015年8月10日 上午10:17:42
	 */
	private void initProvider() {
		//添加消息扩展的提供者
		ProviderManager.addExtensionProvider(MessageTypeExtension.ELEMENT, MessageTypeExtension.NAMESPACE, new MessageTypeProvider());
		ProviderManager.addIQProvider(VcardX.ELEMENT, VcardX.NAMESPACE, new VcardXProvider());
	}
	
	/**
	 * 返回一个有效的xmpp连接
	 * @return
	 */
	public AbstractXMPPConnection getConnection() {
		return connection;
	}
	
	/**
	 * 断开连接
	 * @author Administrator
	 * @update 2014年10月7日 上午9:35:03
	 */
	public void disconnect() {
		if(connection != null) {
			connection.disconnect();
		}
	}

	/**
	 * 权限检查，主要检查有木有登录
	 * @param connection 连接
	 * @param  application 应用对象
	 * @return 是否连接且登录
	 * 创建人：huanghui1
	 * 创建时间： 2015/11/13 10:21
	 * 修改人：huanghui1
	 * 修改时间：2015/11/13 10:21
	 * 修改备注：
	 * @version: 0.0.1
	 */
	public boolean checkAuthority(AbstractXMPPConnection connection, ChatApplication application) {
		boolean isConnected = false;
		boolean isLogined = false;
		String username = null;
		synchronized (lock) {
			if (connection == null) {
				connection = getConnection();
				if (connection == null) {
					connection = init(application.getSystemConfig());
				}
			}
			try {
				if (!connection.isConnected()) {
					connection.connect();
				}
				isConnected = true;
				if (!connection.isAuthenticated()) {
					SystemConfig systemConfig = application.getSystemConfig();
					username = systemConfig.getAccount();
					String password = systemConfig.getPassword();
					connection.login(username, password, Constants.CLIENT_RESOURCE);
					application.setCurrentAccount(username);
				}
				isLogined = true;
			} catch (SmackException e) {
				Log.e(e.getMessage());
				connection.disconnect();
			} catch (IOException e) {
				Log.e(e.getMessage());
				connection.disconnect();
			} catch (XMPPException e) {
				Log.e(e.getMessage());
				connection.disconnect();
			}
		}
		if (isLogined) {
			//初始化个人信息
			Personal currentUser = application.getCurrentUser();
			if (currentUser.getUsername() == null) {
				currentUser.setUsername(username);
			}
			Personal tmpPerson = PersonalManage.getInstance().getLocalSelfInfoByUsername(currentUser);
			application.setCurrentUser(tmpPerson);
		}
		return isConnected && isLogined;
	}

	/**
	 * 后台登录
	 * @param connection 连接
	 * @throws IOException
	 * @throws XMPPException
	 * @throws SmackException
	 * @author huanghui1
	 * @update  2015/11/16 17:27
	 * @version: 0.0.1
	 */
	public void login(AbstractXMPPConnection connection) throws IOException, XMPPException, SmackException {
		ChatApplication application = ChatApplication.getInstance();
		SystemConfig systemConfig = application.getSystemConfig();
		String username = systemConfig.getAccount();
		String password = systemConfig.getPassword();
		connection.login(username, password, Constants.CLIENT_RESOURCE);
	}
}
