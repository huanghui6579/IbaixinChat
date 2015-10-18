package net.ibaixin.chat.util;

/**
 * 观察者
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年3月9日 下午12:29:25
 */
public interface Observer {
	public enum NotifyType {
		/**
		 * 单个添加
		 */
		ADD,
		/**
		 * 单个删除
		 */
		DELETE,
		/**
		 * 单个更新
		 */
		UPDATE,
		/**
		 * 批量更新
		 */
		BATCH_UPDATE,
	}
	
	/**
     * This method is called if the specified {@code Observable} object's
     * {@code notifyObservers} method is called (because the {@code Observable}
     * object has been updated.
     *
     * @param observable
     *            the {@link Observable} object.
     * @param notifyFlag 通知标识
     * @param data
     *            the data passed to {@link Observable#notifyObservers(Object)}.
     * @param updateType one of {@link ADD}, {@link DELETE}, {@link UPDATE}
     */
	public void update(Observable<?> observable, int notifyFlag, NotifyType notifyType, Object data);
	
	/**
	 * 处理更新
	 * @update 2015年3月10日 下午2:01:17
	 * @param observable
	 * @param notifyFlag 通知标识
	 * @param updateType
	 * @param data
	 */
	public void dispatchUpdate(Observable<?> observable, int notifyFlag, NotifyType notifyType, Object data);
}
