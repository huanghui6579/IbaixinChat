package net.ibaixin.chat.listener;

import java.io.File;
import java.util.Collection;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.DefaultExtensionElement;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.roster.Roster;
import org.jxmpp.util.XmppStringUtils;

import android.content.Context;
import android.text.TextUtils;
import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.R;
import net.ibaixin.chat.download.DownloadManager;
import net.ibaixin.chat.download.SimpleDownloadListener;
import net.ibaixin.chat.manager.UserManager;
import net.ibaixin.chat.manager.web.UserEngine;
import net.ibaixin.chat.model.HeadIcon;
import net.ibaixin.chat.model.NewFriendInfo;
import net.ibaixin.chat.model.NewFriendInfo.FriendStatus;
import net.ibaixin.chat.model.User;
import net.ibaixin.chat.model.UserVcard;
import net.ibaixin.chat.model.web.VcardDto;
import net.ibaixin.chat.smack.packet.VcardX;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.ImageUtil;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.util.XmppConnectionManager;
import net.ibaixin.chat.util.XmppUtil;

/**
 * 接收消息的监听器
 * @author huanghui1
 * @update 2014年11月10日 下午6:10:15
 */
public class ChatPacketListener implements StanzaListener {
	private Context mContext;
	
	public ChatPacketListener() {
		mContext = ChatApplication.getInstance();
	}

	@Override
	public void processPacket(Stanza packet) throws NotConnectedException {
		if (packet != null) {
			String from = packet.getFrom();
			if (!TextUtils.isEmpty(from)) {
				if (!XmppUtil.isOutMessage(from)) {    //只处理发起消息的不是自己的情况
					//TODO 其他各种消息处理
					if (packet instanceof Presence) {
						Presence presence = (Presence) packet;
						SystemUtil.getCachedThreadPool().execute(new HandlePresenceTask(presence));
					} else if (packet instanceof IQ) {	//处理iq的消息类型
						IQ iq = (IQ) packet;
						SystemUtil.getCachedThreadPool().execute(new HandleIQTask(iq));
					}
				} else {
					Log.d("------ChatPacketListener-----is---self---packet----" + packet);
				}
			} else {
				Log.w("----ChatPacketListener----processPacket----from----is null--" + from);
			}
		} else {
			Log.w("----ChatPacketListener----processPacket----packet----is null--" + packet);
		}
	}
	
	/**
	 * 处理添加好友请求等任务
	 * @author huanghui1
	 * @update 2014年11月10日 下午8:57:51
	 */
	class HandlePresenceTask implements Runnable {
		private Presence presence;
		
		private UserManager mUserManager = UserManager.getInstance();
		
		private AbstractXMPPConnection connection = XmppConnectionManager.getInstance().getConnection();

		public HandlePresenceTask(Presence presence) {
			super();
			this.presence = presence;
		}

		@Override
		public void run() {
//			Collection<ExtensionElement> extensions = presence.getExtensions();
//			boolean isEmpty = SystemUtil.isEmpty(extensions);
			Presence.Type type = presence.getType();
			/*
			 *  •	available: 表示处于在线状态
			•	unavailable: 表示处于离线状态
			•	subscribe: 表示发出添加好友的申请
			•	unsubscribe: 表示发出删除好友的申请
			•	unsubscribed: 表示拒绝添加对方为好友
			•	error: 表示presence信息报中包含了一个错误消息。

			 */
			NewFriendInfo newInfo = null;
			String from = SystemUtil.unwrapJid(presence.getFrom());
			String to = SystemUtil.unwrapJid(presence.getTo());
			switch (type) {
			case available:	//用户上线
				if (!ChatRostListener.hasRosterListener) {	
					/*
					 * 如果没有相关的监听器了，那就在这里重复处理了，否则，在{@linkplain ChatRostListener}中处理
					 */
					mUserManager.updateUserPresence(presence);
				}
				break;
			case subscribe:	//添加好友的申请(对方发出添加我为好友的消息)
				
				//查找数据库是否有我主动请求添加对方为好友的信息
				newInfo = mUserManager.getNewFriendInfoByAccounts(to, from);
				UserEngine userEngine = new UserEngine(mContext);
				VcardDto vcardDto = null;
				User user = null;
				if (newInfo != null) {	//有此信息了
					switch (newInfo.getFriendStatus()) {
					case VERIFYING:	//自己主动添加对方为好友，此时，对方同意了，并且添加我为好友，则自己直接同意并添加对方为好友
						try {
							XmppUtil.acceptFriend(connection, presence.getFrom());
							//修改状态为“已添加”
							newInfo.setFriendStatus(FriendStatus.ADDED);
							//查询该好友的头像信息
							vcardDto = userEngine.getSimpleVcardInfoSync(from);
							String nick = null;
							String iconHash = null;
							String mimeType = null;
							if (vcardDto != null) {
								nick = vcardDto.getNickName();
								iconHash = vcardDto.getHash();
								mimeType = vcardDto.getMimeType();
							}
							//将该好友添加至本地数据库
							user = newInfo.getUser();
							UserVcard vcard = null;
							if (user == null) {
								user = new User();
								user.setUsername(from);
								
								vcard = new UserVcard();
							}
							vcard = user.getUserVcard();
							if (vcard == null) {
								vcard = new UserVcard();
							}
							vcard.setNickname(nick);
							vcard.setMimeType(mimeType);
							vcard.setIconHash(iconHash);
							
							user.setNickname(nick);
							
							user.setUserVcard(vcard);
							
							user.setFullPinyin(user.initFullPinyin());
							user.setShortPinyin(user.initShortPinyin());
							user.setSortLetter(user.initSortLetter(user.getShortPinyin()));
							if (nick == null) {
								nick = user.getName();
							}
							newInfo.setIconHash(iconHash);
							newInfo.setTo(nick);
							Roster roster = Roster.getInstanceFor(connection);
							roster.createEntry(presence.getFrom(), nick, null);
							
							handleDownalodAvatar(newInfo, user, userEngine);
							
						} catch (NotConnectedException | NotLoggedInException | NoResponseException | XMPPErrorException e) {
							Log.e(e.getMessage());
						}
						break;
					case ACCEPT:	//只是别人请求添加我为好友，但之前已经请求过了只是我没有处理，则只更新下基本信息
						vcardDto = userEngine.getSimpleVcardInfoSync(from);
						if (vcardDto != null) {
							boolean needDownload = handleNewInfo(newInfo, vcardDto);
							
							if (needDownload) {	//需要下载头像
								handleDownalodAvatar(newInfo, userEngine, false);
							} else {
								mUserManager.updateNewFriendInfo(newInfo);
							}
						}
						break;
					default:
						break;
					}
				} else {
					newInfo = new NewFriendInfo();
					newInfo.setFriendStatus(FriendStatus.ACCEPT);
					newInfo.setFrom(from);
					newInfo.setTo(to);
					newInfo.setTitle(from);
					newInfo.setContent(mContext.getString(R.string.contact_friend_add_request));
					newInfo.setCreationDate(System.currentTimeMillis());

					vcardDto = userEngine.getSimpleVcardInfoSync(from);
					boolean needDownload = handleNewInfo(newInfo, vcardDto);
					
					if (needDownload) {	//需要下载头像
						handleDownalodAvatar(newInfo, userEngine, true);
					} else {
						mUserManager.addNewFriendInfo(newInfo);
					}
				}
				
				break;
//			case subscribed:	//对方添加自己为好友，可能只是单方面的
//				//查找数据库是否有我主动请求添加对方为好友的信息
//				newInfo = mUserManager.getNewFriendInfoByAccounts(to, from);
//				//自己主动添加对方为好友，此时，对方需要对方确认
//				if (newInfo != null && newInfo.getFriendStatus() == FriendStatus.VERIFYING) {
//					
//				}
//				break;
			default:
				break;
			}
		}
		
	}
	
	/**
	 * 更新用户电子名片的的信息，返回是否需要下载该好友的头像
	 * @param newInfo 新的好友请求信息
	 * @param vcardDto 
	 * @param userEngine
	 * @return 是否需要下载该好友的头像
	 * @update 2015年9月28日 下午5:01:31
	 */
	private boolean handleNewInfo(NewFriendInfo newInfo, VcardDto vcardDto) {
		String username = newInfo.getFrom();
		String iconHash = null;
		String nick = null;
		String mimeType = null;
		boolean needDownload = false;
		if (vcardDto != null) {
			iconHash = vcardDto.getHash();
			nick = vcardDto.getNickName();
			if (!TextUtils.isEmpty(nick)) {
				newInfo.setTitle(nick);
			}
			mimeType = vcardDto.getMimeType();
		}
		newInfo.setCreationDate(System.currentTimeMillis());
		
		UserManager userManager = UserManager.getInstance();
		User user = userManager.getUserByUsername(username);
		if (user != null) {	//如果存在本地好友
			newInfo.setIconHash(iconHash);
			UserVcard uCrad = user.getUserVcard();
			String fIconHash = newInfo.getIconHash();
			String uIconHash = null;
			if (uCrad != null) {	//本地好友有名片信息
				uIconHash = uCrad.getIconHash();
				uCrad.setNickname(nick);
				uCrad.setMimeType(mimeType);
				if (!TextUtils.isEmpty(fIconHash)) {	//对方有头像信息
					if (!fIconHash.equals(uIconHash)) {	//此时本地好友没有头像信息，或者头像没有更新下来
						//本地好友有头像信息，但需要对比一下头像是否已经改变，如果改变，则需要更新
//						uIconHash = fIconHash;
//						uCrad.setIconHash(uIconHash);
//						uCrad.setIconHash(fIconPath);
						needDownload = true;
					}
				} else {	//对方没有头像
					if (!TextUtils.isEmpty(uIconHash)) {	//但本地有头像，更新本地头像
						String uIconPath = uCrad.getIconPath();
						uIconHash = null;
						uCrad.setIconHash(null);
						uCrad.setIconPath(null);
						uCrad.setThumbPath(null);
						//删除本地图像
						SystemUtil.deleteFile(uIconPath);
						
					}
				}
				//更新电子名片信息
				userManager.updateUserVcardIcon(user, uCrad);
			} else {	//本地好友没有名片，则新建名片
				uCrad = new UserVcard();
				uCrad.setNickname(nick);
				uCrad.setMimeType(mimeType);
				if (!TextUtils.isEmpty(fIconHash)) {	//对方有图像
					needDownload = true;
					uCrad.setIconHash(fIconHash);
				}
				user.setUserVcard(uCrad);
				userManager.addUserVcard(user, uCrad);
			}
			newInfo.setUser(user);
			newInfo.setContent(user.getName());
			
			//下载用户头像
		} else {
			String oHash = newInfo.getIconHash();
			if (TextUtils.isEmpty(oHash)) {	//本地新的好友请求信息头像为空
				if (TextUtils.isEmpty(iconHash)) {	//服务器上的也为空，则不用下载
					needDownload = false;
				} else {
					needDownload = true;
				}
			} else {	//本地有头像
				if (!oHash.equals(iconHash)) {	//两者不同，则需要下载
					needDownload = true;
				}
			}
		}
		return needDownload;
	}
	
	/**
	 * 下载用户头像
	 * @param newInfo
	 * @param userEngine
	 * @param isAdd 是否是添加，不是添加就是更新
	 * @update 2015年9月28日 下午7:14:39
	 */
	private void handleDownalodAvatar(final NewFriendInfo newInfo, UserEngine userEngine, final boolean isAdd) {
		User user = newInfo.getUser();
		if (user == null) {	//本地没有该好友信息
			user = new User();
			user.setUsername(newInfo.getFrom());
		}
		final UserManager userManager = UserManager.getInstance();
		userEngine.downloadAvatar(user, Constants.FILE_TYPE_THUMB, new SimpleDownloadListener() {
			
			@Override
			public void onSuccess(int downloadId, String filePath) {
				Log.d("------handleDownalodAvatar----onSuccess-----downloadId-------" + downloadId + "------filePath-----" + filePath);
				newInfo.setIconPath(filePath);
				
				if (isAdd) {
					userManager.addNewFriendInfo(newInfo);
				} else {
					userManager.updateNewFriendInfo(newInfo);
				}
//				userManager.updateNewFriendInfoAvatar(newInfo);
			}
			
			@Override
			public void onFailure(int downloadId, int statusCode, String errMsg) {
				Log.w("------handleDownalodAvatar-----onFailure-----downloadId-------" + downloadId + "-----statusCode-----" + statusCode + "------errMsg-----" + errMsg);
				userManager.addNewFriendInfo(newInfo);
			}
		});
	}
	
	/**
	 * 下载用户头像
	 * @param user
	 * @param userEngine
	 * @update 2015年9月29日 下午3:21:00
	 */
	private void handleDownalodAvatar(final NewFriendInfo newInfo, final User user, UserEngine userEngine) {
		userEngine.downloadAvatar(user, Constants.FILE_TYPE_THUMB, new SimpleDownloadListener() {
			
			@Override
			public void onSuccess(int downloadId, String filePath) {
				Log.d("------handleDownalodAvatar----onSuccess-----downloadId-------" + downloadId + "------filePath-----" + filePath);
				UserManager userManager = UserManager.getInstance();
				UserVcard vcard = user.getUserVcard();
				if (vcard != null) {
					vcard.setThumbPath(filePath);
				}
				newInfo.setIconPath(filePath);
				//更新获取保存该用户
				userManager.saveOrUpdateFriend(user);
				userManager.saveOrUpdateNewFriendInfo(newInfo);
			}
			
			@Override
			public void onFailure(int downloadId, int statusCode, String errMsg) {
				Log.w("------handleDownalodAvatar-----onFailure-----downloadId-------" + downloadId + "-----statusCode-----" + statusCode + "------errMsg-----" + errMsg);
			}
		});
	}
	
	/**
	 * 处理iq包从任务
	 * @author huanghui1
	 * @version 1.0.0
	 * @update 2015年8月12日 上午11:35:45
	 */
	private class HandleIQTask implements Runnable {

		private IQ iq;
		
		public HandleIQTask(IQ iq) {
			super();
			this.iq = iq;
		}

		@Override
		public void run() {
			String childrenNamespace = iq.getChildElementNamespace();
			if (childrenNamespace != null) {
				switch (childrenNamespace) {
				case VcardX.NAMESPACE:	//好友头像改变的扩展消息
					VcardX vcardX = (VcardX) iq;
					String jid = vcardX.getFrom();
					final String mimeType = vcardX.getMimeType();
					final String name = XmppStringUtils.parseLocalpart(jid);
					//新的头像hash
					final String hash = vcardX.getIconHash();
					if (hash != null) {
						
						final UserManager userManager = UserManager.getInstance();
						final User user = new User();
						user.setUsername(name);
						
						//是否需要下载头像
						boolean needDownload = false;
						final UserVcard userVcard = userManager.getUserIconVcard(name);
						if (userVcard != null) {	//该好友有电子名片信息
							String oldHash = userVcard.getIconHash();
							if (!hash.equals(oldHash)) {	//好友新的头像与本地老的头像不同，则需下载该好友的头像，并存储
								needDownload = true;
								userVcard.setIconHash(hash);
								userVcard.setMimeType(mimeType);
								user.setUserVcard(userVcard);
							} else {
								Log.d("-------HandleIQTask----VcardX.NAMESPACE---头像hash相同,不下载头像---hash" + hash + "---name-----" + name);
							}
						} else {	//该好友没有电子名片信息，则需创建，并下载该好友的缩略图像
							needDownload = true;
						}
						
						if (needDownload) {	//需要下载图像，则下载
							final UserEngine userEngine = new UserEngine(mContext);
							//线下载缩略图，若没有缩略图，则再下载原始头像
							userEngine.downloadAvatar(user, Constants.FILE_TYPE_THUMB, new SimpleDownloadListener() {
								
								@Override
								public void onSuccess(int downloadId, String filePath) {
									Log.d("-------HandleIQTask----VcardX.NAMESPACE----FILE_TYPE_THUMB---name-----" + name + "-----downloadAvatar-----onSuccess----downloadId---" + downloadId + "----filePath--" + filePath);
									//清除原来头像的内存缓存
									ImageUtil.clearMemoryCache(filePath);
									if (userVcard != null) {	//更新电子名片信息
										userVcard.setThumbPath(filePath);
										userManager.updateUserVcardIcon(user, userVcard);
										Log.d("-------HandleIQTask----VcardX.NAMESPACE---FILE_TYPE_THUMB-----updateUserVcardIcon----name-----" + name + "--------更新电子名片信息-----");
									} else {	//添加电子名片信息
										//电子名片信息为空，则需创建
										int userId = userManager.getUserIdByUsername(name);
										if (userId > 0) {
											user.setId(userId);
											UserVcard vcard = new UserVcard();
											vcard.setUserId(userId);
											vcard.setIconHash(hash);
											vcard.setThumbPath(filePath);
											vcard.setIconPath(null);
											vcard.setMimeType(mimeType);
											userManager.addUserVcard(user, vcard);
											Log.d("-------HandleIQTask----VcardX.NAMESPACE----FILE_TYPE_THUMB-----name-----" + name + "--------没有电子名片，添加该用户电子名片信息-----");
										}
									}
								}
								
								@Override
								public void onFailure(int downloadId, int statusCode, String errMsg) {
									Log.w("-----HandleIQTask----VcardX.NAMESPACE----FILE_TYPE_ORIGINAL----downloadAvatar---name-----" + name + "-----下载缩略图失败，开始下载原始图像---statusCode--" + statusCode + "---errMsg--" + errMsg);
									userEngine.downloadAvatar(user, Constants.FILE_TYPE_ORIGINAL, new SimpleDownloadListener() {
										
										@Override
										public void onSuccess(int downloadId, String filePath) {
											Log.d("-------HandleIQTask----VcardX.NAMESPACE----FILE_TYPE_ORIGINAL-----name-----" + name + "-----downloadAvatar-----onSuccess----downloadId---" + downloadId + "----filePath--" + filePath);
											//清除原来头像的内存缓存
											ImageUtil.clearMemoryCache(filePath);
											if (userVcard != null) {	//更新电子名片信息
												userVcard.setIconPath(filePath);
												userManager.updateUserVcardIcon(user, userVcard);
												Log.d("-------HandleIQTask----VcardX.NAMESPACE---FILE_TYPE_THUMB-----updateUserVcardIcon----name-----" + name + "--------更新电子名片信息-----");
											} else {	//添加电子名片信息
												//电子名片信息为空，则需创建
												int userId = userManager.getUserIdByUsername(name);
												if (userId > 0) {
													user.setId(userId);
													UserVcard vcard = new UserVcard();
													vcard.setUserId(userId);
													vcard.setIconHash(hash);
													vcard.setThumbPath(null);
													vcard.setIconPath(filePath);
													vcard.setMimeType(mimeType);
													userManager.addUserVcard(user, vcard);
													Log.d("-------HandleIQTask----VcardX.NAMESPACE----FILE_TYPE_THUMB-----name-----" + name + "--------没有电子名片，添加该用户电子名片信息-----");
												}
											}
										}
										
										@Override
										public void onFailure(int downloadId, int statusCode, String errMsg) {
											Log.w("-----HandleIQTask----VcardX.NAMESPACE----FILE_TYPE_ORIGINAL----downloadAvatar---name-----" + name + "-----下载原始图像失败---statusCode--" + statusCode + "---errMsg--" + errMsg);
										}
									});
								}
							});
						}
						
					} else {
						Log.d("-------HandleIQTask----VcardX.NAMESPACE---头像hash为空-----" + hash + "---name-----" + name);
					}
					break;
					
				default:
					break;
				}
			}
		}
		
	}
	
}