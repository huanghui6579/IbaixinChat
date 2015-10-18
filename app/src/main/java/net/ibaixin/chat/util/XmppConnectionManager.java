package net.ibaixin.chat.util;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.Provider;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackConfiguration;
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

import net.ibaixin.chat.listener.ChatConnectionListener;
import net.ibaixin.chat.listener.ChatPacketListener;
import net.ibaixin.chat.model.SystemConfig;
import net.ibaixin.chat.smack.extension.MessageTypeExtension;
import net.ibaixin.chat.smack.packet.VcardX;
import net.ibaixin.chat.smack.provider.MessageTypeProvider;
import net.ibaixin.chat.smack.provider.VcardXProvider;

/**
 * xmpp服务器的连接管理器
 * @author Administrator
 * @update 2014年10月7日 上午9:37:01
 *
 */
public class XmppConnectionManager {
	private AbstractXMPPConnection connection;
	
	private static XmppConnectionManager instance = null;
	private static XMPPTCPConnectionConfiguration configuration;
	
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
	public AbstractXMPPConnection init(SystemConfig systemConfig) {
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
		
//		ReconnectionManager.setEnabledPerDefault(true);
//		ReconnectionManager.getInstanceFor(connection);
		//添加监听器
		connection.addConnectionListener(new ChatConnectionListener());
		StanzaFilter packetFilter = new OrFilter(new StanzaTypeFilter(IQ.class), new StanzaTypeFilter(Presence.class));
		connection.addAsyncStanzaListener(new ChatPacketListener(), packetFilter);
		
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
}
