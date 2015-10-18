package net.ibaixin.chat.listener;

import java.util.List;

import net.ibaixin.chat.model.User;

/**
 * roster列表加载完成后的列表
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年8月4日 下午7:36:09
 */
public interface RosterLoadedCallback {
	/**
	 * 加载成功后返回好友列表
	 * @param userList 加载完成后的数据
	 * @update 2015年8月4日 下午7:38:45
	 */
	public void loadSuccessful(List<User> userList);
}
