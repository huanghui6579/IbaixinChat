package net.ibaixin.chat.model;

import java.util.Comparator;

import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.R;

/**
 * 新的朋友实体
 * @author Administrator
 * @update 2014年11月9日 下午2:30:42
 * @version 1.0.0
 */
public class NewFriendInfo implements Comparator<NewFriendInfo> {
	/**
	 * 主键
	 */
	private int id;
	/**
	 * 用户实体信息
	 */
	private User user;
	/**
	 * 好友请求的状态，默认是UNADD
	 */
	private FriendStatus friendStatus = FriendStatus.UNADD;
	/**
	 * 标题
	 */
	private String title;
	/**
	 * 描述信息
	 */
	private String content;
	/**
	 * 创建时间
	 */
	private long creationDate;
	/**
	 * 发送申请的人账号
	 */
	private String from;
	/**
	 * 接收人的账号
	 */
	private String to;
	/**
	 * 对方头像的hash值
	 */
	private String iconHash;
	/**
	 * 对方头像的本地地址
	 */
	private String iconPath;
	

	/**
	 * 好友请求的状态，目前分为四种：<br />
	 * <ul>
	 * 	<li>UNADD--未添加，此时是陌生人</li>
	 * 	<li>ADDED--已经添加，此时已经是好友</li>
	 * 	<li>VERIFYING--自己像对方发送添加好友的请求，对方还没有答应或回应/li>
	 * 	<li>ACCEPT--对方向自己发送添加好友的请求，自己还没有答应或者回应</li>
	 * </ul>
	 * @author Administrator
	 * @update 2014年11月9日 下午2:45:44
	 * @version 1.0.0
	 *
	 */
	public enum FriendStatus {
		/**
		 * 未添加，此时是陌生人
		 */
		UNADD {
			@Override
			public String getTitle() {
				return ChatApplication.getInstance().getString(R.string.contact_new_friend_status_title_unadd);
			}
		},
		/**
		 * 已经添加，此时已经是好友
		 */
		ADDED {
			@Override
			public String getTitle() {
				return ChatApplication.getInstance().getString(R.string.contact_new_friend_status_title_added);
			}
		},
		/**
		 * 自己像对方发送添加好友的请求，对方还没有答应或回应
		 */
		VERIFYING {
			@Override
			public String getTitle() {
				return ChatApplication.getInstance().getString(R.string.contact_new_friend_status_title_verifying);
			}
		},
		/**
		 * 对方向自己发送添加好友的请求，自己还没有答应或者回应
		 */
		ACCEPT {
			@Override
			public String getTitle() {
				return ChatApplication.getInstance().getString(R.string.contact_new_friend_status_title_accept);
			}
		};
		
		public abstract String getTitle();
		
		public String getValue() {
			return this.name();
		}
		
		public static FriendStatus valueOf(int value) {
			switch (value) {
			case 0:
				return UNADD;
			case 1:
				return ADDED;
			case 2:
				return VERIFYING;
			case 3:
				return ACCEPT;

			default:
				return UNADD;
			}
		}
		
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public FriendStatus getFriendStatus() {
		return friendStatus;
	}

	public void setFriendStatus(FriendStatus friendStatus) {
		this.friendStatus = friendStatus;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getIconHash() {
		return iconHash;
	}

	public void setIconHash(String iconHash) {
		this.iconHash = iconHash;
	}

	public String getIconPath() {
		return iconPath;
	}

	public void setIconPath(String iconPath) {
		this.iconPath = iconPath;
	}

	public String getTitle() {
		String s = null;
		if (title == null) {
			if (user != null) {
				s = user.getName();
			} else {
				switch (friendStatus) {
				case ACCEPT:	//对方发送的添加信息，等待我的同意
					s = from;
					break;
				case VERIFYING:	//我发送的添加信息，等对方我的同意
					s = to;
					break;

				default:
					break;
				}
			}
		} else {
			s = title;
		}
		return s;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	/**
	 * 获取对方的昵称
	 * @return
	 * @update 2015年9月25日 下午4:03:19
	 */
	public String getNickname() {
		if (user != null) {
			return user.getName();
		} else {
			if (from.equals(ChatApplication.getInstance().getCurrentAccount())) {	//自己主动添加对方为好友
				return to;
			} else {
				return from;
			}
		}
	}

	@Override
	public String toString() {
		return "NewFriendInfo [id=" + id + ", user=" + user + ", friendStatus="
				+ friendStatus + ", title=" + title + ", content=" + content
				+ ", creationDate=" + creationDate + ", from=" + from + ", to="
				+ to + ", iconHash=" + iconHash + ", iconPath=" + iconPath
				+ "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NewFriendInfo other = (NewFriendInfo) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public int compare(NewFriendInfo lhs, NewFriendInfo rhs) {
		long ltime = lhs.getCreationDate();
		long rtime = rhs.getCreationDate();
		if (ltime > rtime) {
			return -1;
		} else if (ltime < rtime) {
			return 1;
		} else {
			return 0;
		}
	}
	
}
