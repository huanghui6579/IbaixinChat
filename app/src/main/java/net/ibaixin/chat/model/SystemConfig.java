package net.ibaixin.chat.model;

/**
 * 系统常用配置实体
 * 
 * @author Administrator
 *
 */
public class SystemConfig {
//	private String host;
//	private int port;
//	private String serverName;
	private String account;
	private String password;
	private String nickname;
	private String email;
	private String sessionId;	//会话id
	//	/**
//	 * 用户使用的是哪种终端登录，如果Android手机、iPhone、web等等
//	 */
//	private String resource;
	private boolean isOnline;	//是否连接成功
	private boolean isFirstLogin;	//是否是首次登录

	/***
	 * 使用第三方登录发现系统不存在该用户，就会发起注册流程，
	 * 如果是使用第三方账号注册则第三方的头像网络url保存保存到这里
	 * Add by dudejin 2015/12/29
	 */
	private String mThirdAvatarUrl ;

	public String getmThirdAvatarUrl() {
		return mThirdAvatarUrl;
	}

	public void setmThirdAvatarUrl(String mThirdAvatarUrl) {
		this.mThirdAvatarUrl = mThirdAvatarUrl;
	}

	/*public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}*/

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public boolean isOnline() {
		return isOnline;
	}

	public void setOnline(boolean isOnline) {
		this.isOnline = isOnline;
	}

	public boolean isFirstLogin() {
		return isFirstLogin;
	}

	public void setFirstLogin(boolean isFirstLogin) {
		this.isFirstLogin = isFirstLogin;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	/*public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}*/

}
