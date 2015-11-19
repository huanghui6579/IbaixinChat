package net.ibaixin.chat.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.listener.RosterLoadedCallback;
import net.ibaixin.chat.model.HeadIcon;
import net.ibaixin.chat.model.Personal;
import net.ibaixin.chat.model.User;
import net.ibaixin.chat.model.UserVcard;
import net.ibaixin.chat.smack.packet.VcardX;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.id.StanzaIdUtil;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jivesoftware.smackx.search.ReportedData;
import org.jivesoftware.smackx.search.ReportedData.Row;
import org.jivesoftware.smackx.search.UserSearchManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jivesoftware.smackx.xdata.Form;
import org.jxmpp.util.XmppStringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年10月9日 下午9:18:10
 */
public class XmppUtil {
	/**
	 * 搜索好友
	 * @update 2014年10月10日 下午8:45:50
	 * @param connection
	 * @param username
	 * @return
	 */
	public static List<User> searchUser(AbstractXMPPConnection connection, String username) {
		List<User> users = null;
		try {
			UserSearchManager searchManager = new UserSearchManager(connection);
			String searchService = "search." + connection.getServiceName();
			Form searchForm = searchManager.getSearchForm(searchService);
			Form answerForm = searchForm.createAnswerForm();
			answerForm.setAnswer("Username", true);
			answerForm.setAnswer("Name", true);
			answerForm.setAnswer("Email", true);
			answerForm.setAnswer("search", username);
			ReportedData reportedData = searchManager.getSearchResults(answerForm, searchService);
			List<Row> rows = reportedData.getRows();
			if(rows != null && rows.size() > 0) {
				users = new ArrayList<>();
				for(Row row : rows) {
					User user = new User();
					user.setUsername(row.getValues("Username").get(0));
					user.setJID(row.getValues("jid").get(0));
					user.setEmail(row.getValues("Email").get(0));
					user.setNickname(row.getValues("Name").get(0));
					users.add(user);
				}
			}
		} catch (NoResponseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XMPPErrorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotConnectedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return users;
	}
	
	/**
	 * 获取用户的好友列表
	 * @param connection
	 * @param loaedCallback 加载完成后的数据回调
	 * @return
	 */
	public static void loadFriends(final AbstractXMPPConnection connection, final RosterLoadedCallback loaedCallback) {
		Roster roster = Roster.getInstanceFor(connection);
		try {
			roster.reloadAndWait();
			Set<RosterEntry> entries = roster.getEntries();
			Log.d("-----getFriends----onRosterLoaded----entries-----" + entries + "------isLogin-----" + XmppUtil.checkAuthenticated(connection));
			List<User> users = null;
			if (entries != null && entries.size() > 0) {
				users = new ArrayList<User>();
				for (RosterEntry entry : entries) {
					if (entry != null) {
						RosterPacket.ItemType itemType = entry.getType();
						if (RosterPacket.ItemType.none == itemType) {
							continue;
						}
						User user = new User();
						String jid = entry.getUser();
						String name = entry.getName();
						if (jid.contains("/")) {
							String[] arr = jid.split("/");
							String resource = arr[1];
							user.setResource(resource);
							user.setJID(arr[0]);
						} else {
							user.setJID(jid);
						}
						String username = SystemUtil.unwrapJid(jid);
						Presence presence = roster.getPresence(jid);
						if (presence != null) {
							user.setStatus(presence.getStatus());
							String resource = XmppStringUtils.parseResource(presence.getFrom());
							if (TextUtils.isEmpty(resource)) {
								resource = SystemUtil.getPhoneModel();
							}
							user.setResource(resource);
							Presence.Type type = presence.getType();
							if (type != null) {
								user.setMode(type.name());
							}
						}
						user.setNickname(name);
						user.setUsername(username);
						user.setFullPinyin(user.initFullPinyin());
						user.setShortPinyin(user.initShortPinyin());
						user.setSortLetter(user.initSortLetter(user.getShortPinyin()));
						users.add(user);
					}
					
				}
				loaedCallback.loadSuccessful(users);
			}
			Log.d("-----getFriends---users-----" + users);
		} catch (NotLoggedInException | NotConnectedException e) {
			Log.e(e.getMessage());
		}
//		Set<RosterEntry> entries = roster.getEntries();
//		Log.d("-----getFriends---entries-----" + entries + "------isLogin-----" + XmppUtil.checkAuthenticated(connection));
	}
	
	/**
	 * 同步好友的头像等基本信息
	 * @update 2014年10月23日 下午4:38:54
	 * @param connection
	 * @param list
	 * @return
	 */
	public static List<User> syncFriendsVcard(AbstractXMPPConnection connection, List<User> list) {
		if (list != null && list.size() > 0) {
			for (User user : list) {
				syncUserVcard(connection, user);
			}
		}
		return list;
	}
	
	/**
	 * 将好友信息与服务器端同步
	 * @update 2014年10月23日 下午7:17:56
	 * @param user
	 * @return
	 */
	public static User syncUserVcard(AbstractXMPPConnection connection, User user) {
		VCard card = getUserVcard(connection, user.getJID());
		if (card != null) {
			UserVcard uv = user.getUserVcard();
			if (uv == null) {
				uv = new UserVcard();
				uv.setUserId(user.getId());
			}
			uv.setCity(card.getAddressFieldHome("LOCALITY"));
			uv.setProvince(card.getAddressFieldHome("REGION"));
			uv.setStreet(card.getAddressFieldHome("STREET"));
			uv.setEmail(card.getEmailHome());
			uv.setMobile(card.getPhoneHome("CELL"));
			uv.setNickname(card.getNickName());
			uv.setRealName(card.getLastName());
			uv.setZipCode(card.getAddressFieldHome("PCODE"));
			uv.setDesc(card.getField("DESC"));
			String iconHash = uv.getIconHash();
			boolean isIconExists = SystemUtil.isFileExists(uv.getIconPath());
			if (!isIconExists || TextUtils.isEmpty(iconHash) || !iconHash.equals(card.getAvatarHash())) {	//没有头像或者头像已经改变就需要更新头像
				File icon = SystemUtil.saveFile(card.getAvatar(), SystemUtil.generateIconFile(user.getUsername(), Constants.FILE_TYPE_ORIGINAL));
				if (icon != null) {
					uv.setIconPath(icon.getAbsolutePath());
					iconHash = SystemUtil.getFileHash(icon);
					uv.setIconHash(iconHash);
				}
			}
			if (TextUtils.isEmpty(user.getEmail())) {
				user.setEmail(card.getEmailHome());
			}
			user.setPhone(uv.getMobile());
			String resource = SystemUtil.getResourceWithJID(card.getJabberId());
			if (!TextUtils.isEmpty(resource)) {
				user.setResource(resource);
			}
			user.setUserVcard(uv);
		}
		return user;
	}
	
	/**
	 * 获取用户电子名片
	 * @update 2014年10月10日 下午8:49:35
	 * @param connection
	 * @param user 完整用户账号，格式为xxx@domain或者xxx@domain/resource
	 * @return
	 */
	public static VCard getUserVcard(AbstractXMPPConnection connection, String user) {
		VCard card = null;
		try {
			card = new VCard();
			card.load(connection, user);
		} catch (NoResponseException | XMPPErrorException
				| NotConnectedException | IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return card;
	}
	
	/**
	 * 获取用户的头像
	 * @update 2014年10月10日 下午8:51:25
	 * @param connection
	 * @param user 完整用户账号，格式为xxx@domain或者xxx@domain/resource
	 * @return
	 */
	public static Bitmap getUserIcon(AbstractXMPPConnection connection, String user) {
		Bitmap icon = null;
		VCard card = getUserVcard(connection, user);
		if(card != null) {
			byte[] data = card.getAvatar();
			if (data != null && data.length > 0) {
				icon = BitmapFactory.decodeByteArray(data, 0, data.length);
			}
		}
		return icon;
	}
	
	/**
	 * 从服务器上保存用户的头像
	 * @update 2014年11月11日 下午10:15:33
	 * @param connection
	 * @param username 用户的账号，不含有@及其之后的信息
	 * @return
	 */
	public static HeadIcon downloadUserIcon(AbstractXMPPConnection connection, String username) {
		VCard card = getUserVcard(connection, SystemUtil.wrapJid(username));
		HeadIcon headIcon = null;
		if (card != null) {
			byte[] data = card.getAvatar();
			if (data != null && data.length > 0) {
				headIcon = new HeadIcon();
				File file = SystemUtil.saveFile(data, SystemUtil.generateIconFile(username, Constants.FILE_TYPE_ORIGINAL));
				headIcon.setFilePath(file.getAbsolutePath());
				headIcon.setHash(card.getAvatarHash());
			}
		}
		return headIcon;
	}
	
	/**
	 * 获取用户的头像
	 * @update 2014年10月10日 下午8:51:25
	 * @param card
	 * @return
	 */
	public static Bitmap getUserIcon(VCard card) {
		Bitmap icon = null;
		if(card != null) {
			byte[] data = card.getAvatar();
			if (data != null && data.length > 0) {
				icon = BitmapFactory.decodeByteArray(data, 0, data.length);
			}
		}
		return icon;
	}
	
	/**
	 * 像对方发送一个添加好友的请求
	 * @update 2014年10月10日 下午10:29:35
	 * @param connection
	 * @param toUser
	 * @throws NotConnectedException 
	 */
	public static void addFriend(AbstractXMPPConnection connection, String toUser) throws NotConnectedException {
		Presence presence = new Presence(Presence.Type.subscribe);
		presence.setTo(toUser);
		connection.sendStanza(presence);
	}
	
	/**
	 * 接受别人的添加我为好友
	 * @update 2014年11月12日 下午2:35:57
	 * @param connection
	 * @param toUser
	 * @throws NotConnectedException
	 */
	public static void acceptFriend(AbstractXMPPConnection connection, String toUser) throws NotConnectedException {
		Presence presence = new Presence(Presence.Type.subscribed);
		presence.setTo(toUser);
		connection.sendStanza(presence);
	}
	
	/**
	 * 添加好友到组中
	 * @param connection
	 * @param jid 格式为xxx@domain或者xxx@domain/resource
	 * @param nickname 添加的昵称
	 * @param groups
	 * @throws NotConnectedException 
	 * @throws XMPPErrorException 
	 * @throws NoResponseException 
	 * @throws NotLoggedInException 
	 * @update 2015年9月25日 下午3:52:57
	 */
	public static void addEntry(AbstractXMPPConnection connection, String jid, String nickname, String[] groups) throws NotLoggedInException, NoResponseException, XMPPErrorException, NotConnectedException {
		Roster roster = Roster.getInstanceFor(connection);
		roster.createEntry(jid, nickname, groups);
	}
	
	/**
	 * 更新个人的状态
	 * @param connection
	 * @param type
	 * @throws NotConnectedException
	 * @update 2015年9月24日 下午5:59:04
	 */
	public static void updatePresenceType(AbstractXMPPConnection connection, Presence.Type type) throws NotConnectedException {
		Presence presence = new Presence(type);
		connection.sendStanza(presence);
	}
	
	/**
	 * 将对方从好友列表中删除
	 * @update 2014年12月1日 上午9:53:41
	 * @param connection
	 * @param toUser
	 * @throws NotConnectedException
	 */
	public static void removeFriend(AbstractXMPPConnection connection, String toUser) throws NotConnectedException {
		Presence presence = new Presence(Presence.Type.unavailable);
		presence.setTo(toUser);
		connection.sendStanza(presence);
	}
	
	/**
	 * 根据用户账号获得用户基本信息
	 * @update 2014年11月12日 下午3:39:01
	 * @param connection
	 * @param username
	 * @return
	 * @throws NotConnectedException
	 */
	public static User getUserEntry(AbstractXMPPConnection connection, String username) throws NotConnectedException {
		String jid = SystemUtil.wrapJid(username);
		Roster roster = Roster.getInstanceFor(connection);
		RosterEntry entry = roster.getEntry(jid);
		Presence presence = roster.getPresence(jid);
		User user = new User();
		user.setUsername(username);
		user.setFullPinyin(user.initFullPinyin());
		user.setShortPinyin(user.initShortPinyin());
		user.setSortLetter(user.initSortLetter(user.getShortPinyin()));
		if (entry != null) {
			user.setNickname(entry.getName());
			Presence.Mode mode = presence.getMode();
			if (mode != null) {
				user.setMode(mode.name());
			}
			user.setStatus(presence.getStatus());
		}
		return user;
	}
	
	/**
	 * 删除指定用户
	 * @update 2014年11月12日 下午9:01:10
	 * @param connection
	 * @param username 用户名，不含有@及其之后的信息
	 * @return
	 */
	public static boolean deleteUser(AbstractXMPPConnection connection, String username) {
		Roster roster = Roster.getInstanceFor(connection);
		if (roster == null) {	//已经没有改好友了
			return true;
		}
		RosterEntry rosterEntry = roster.getEntry(SystemUtil.wrapJid(username));
		if (rosterEntry == null) {	//没有该好友，则直接删除本地就行了
			return true;
		}
		boolean flag = false;
		try {
			roster.removeEntry(rosterEntry);
			flag = true;
		} catch (NotLoggedInException | NoResponseException
				| XMPPErrorException | NotConnectedException e) {
			e.printStackTrace();
		}
		return flag;
	}
	
	/**
	 * 从服务器上同步个人信息
	 * @update 2014年10月24日 下午5:59:33
	 * @param personal
	 * @return
	 */
	public static boolean syncPersonalInfo(AbstractXMPPConnection connection, Personal personal) {
		VCard card = getUserVcard(connection, personal.getJID());
		if (card != null) {
			personal.setCity(card.getAddressFieldHome("LOCALITY"));
			personal.setProvince(card.getAddressFieldHome("REGION"));
			personal.setStreet(card.getAddressFieldHome("STREET"));
			personal.setEmail(card.getEmailHome());
			personal.setPhone(card.getPhoneHome("CELL"));
			personal.setNickname(card.getNickName());
			personal.setRealName(card.getLastName());
			personal.setZipCode(card.getAddressFieldHome("PCODE"));
			personal.setDesc(card.getField("DESC"));
			String iconHash = personal.getIconHash();
			boolean isIconExists = SystemUtil.isFileExists(personal.getIconPath());
			if (!isIconExists || TextUtils.isEmpty(iconHash) || !iconHash.equals(card.getAvatarHash())) {	//没有头像或者头像已经改变就需要更新头像
				File icon = SystemUtil.saveFile(card.getAvatar(), SystemUtil.generateIconFile(personal.getUsername(), Constants.FILE_TYPE_ORIGINAL));
				if (icon != null) {
					personal.setIconPath(icon.getAbsolutePath());
					iconHash = SystemUtil.getFileHash(icon);
					personal.setIconHash(iconHash);
				}
			}
			return true;
		}
		return false;
	}
	
	/**
	 * 更新用户头像
	 * @author tiger
	 * @update 2015年3月14日 下午11:43:06
	 * @param connection
	 * @param iconFile 头像文件
	 * @throws NotConnectedException 
	 * @throws XMPPErrorException 
	 * @throws NoResponseException 
	 */
	public static void updateAvatar(AbstractXMPPConnection connection, File iconFile) throws NoResponseException, XMPPErrorException, NotConnectedException {
		VCard card = new VCard();
		card.load(connection);
		byte[] bytes = SystemUtil.getFileBytes(iconFile);
		card.setAvatar(bytes);
		card.save(connection);
	}
	
	/**
	 * 当自己更改头像后，通知好友自己更改了头像
	 * @param connection
	 * @throws NotConnectedException 
	 * @throws XMPPErrorException 
	 * @throws NoResponseException 
	 * @update 2015年8月8日 下午5:08:22
	 */
	public static void updateAvatar(AbstractXMPPConnection connection, VcardX vcardX) throws NoResponseException, XMPPErrorException, NotConnectedException {
		vcardX.setTo(null);
		vcardX.setType(IQ.Type.set);
        // Also make sure to generate a new stanza id (the given vcard could be a vcard result), in which case we don't
        // want to use the same stanza id again (although it wouldn't break if we did)
		vcardX.setStanzaId(StanzaIdUtil.newStanzaId());
		connection.sendStanza(vcardX);
	}
	
	/**
	 * 更新个人的昵称
	 * @author tiger
	 * @update 2015年3月15日 下午9:59:10
	 * @param connection
	 * @param nickname
	 * @throws NotConnectedException 
	 * @throws XMPPErrorException 
	 * @throws NoResponseException 
	 */
	public static void updateNickname(AbstractXMPPConnection connection, String nickname) throws NoResponseException, XMPPErrorException, NotConnectedException {
		VCard card = new VCard();
		card.load(connection);
		card.setNickName(nickname);
		card.save(connection);
	}
	
	/**
	 * 更新用户头像
	 * @author tiger
	 * @update 2015年3月15日 上午12:04:47
	 * @param connection
	 * @param iconPath 头像的全路径，包含文件名
	 * @throws NotConnectedException 
	 * @throws XMPPErrorException 
	 * @throws NoResponseException 
	 */
	public static void updateAvatar(AbstractXMPPConnection connection, String iconPath) throws NoResponseException, XMPPErrorException, NotConnectedException{
		if (SystemUtil.isFileExists(iconPath)) {
			updateAvatar(connection, new File(iconPath));
		}
	}
	
	/**
	 * 检查连接是否可用
	 * @author tiger
	 * @update 2015年3月15日 上午12:42:06
	 * @param connection
	 * @return
	 */
	public static boolean checkAuthenticated(XMPPConnection connection) {
		if (connection == null || !connection.isAuthenticated()) {
            return false;
        } else {
        	return true;
        }
	}
	
	/**
	 * 检查是否客户端与服务器是否连接成功
	 * @param connection
	 * @return false：客户端与服务器没有连接，true：客户端与服务器连接了
	 * @update 2015年7月20日 上午10:17:59
	 */
	public static boolean checkConnected(XMPPConnection connection) {
		if (connection == null || !connection.isConnected()) {
            return false;
        } else {
        	return true;
        }
	}
	
	/**
	 * 判断是否是自己发出去的消息，true：是自己发出去的消息，false:不是自己发出去的消息
	 * @author huanghui1
	 * @update 2015/11/19 15:51
	 * @version: 0.0.1
	 * @return true：是自己发出去的消息，false:不是自己发出去的消息
	 */
	public static boolean isOutMessage(String jid) {
		boolean isOut = false;
		if (jid != null) {
			try {
				String localPart = XmppStringUtils.parseLocalpart(jid);
				if (!TextUtils.isEmpty(localPart)) {
					isOut = localPart.equals(ChatApplication.getInstance().getCurrentAccount());
                }
			} catch (Exception e) {
				Log.e(e.getMessage());
			}
		}
		return isOut;
	}
}
