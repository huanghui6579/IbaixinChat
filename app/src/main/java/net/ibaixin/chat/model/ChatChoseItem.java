package net.ibaixin.chat.model;

/**
 * 转发、分享选择会话的列表项
 * 创建人：huanghui1
 * 创建时间： 2015/11/10 16:16
 * 修改人：huanghui1
 * 修改时间：2015/11/10 16:16
 * 修改备注：
 *
 * @version: 0.0.1
 */
public class ChatChoseItem {
    /**
     * 会话的类型
     */
    public static final int TYPE_THREAD = 0;
    /**
     * 联系人的类型
     */
    public static final int TYPE_CONTACT = 1;

    /**
     * 默认是会话的类型
     */
    private int itemType = TYPE_THREAD;
    
    private MsgThread msgThread;
    
    private User user;

    public int getItemType() {
        return itemType;
    }

    public void setItemType(int itemType) {
        this.itemType = itemType;
    }

    public MsgThread getMsgThread() {
        return msgThread;
    }

    public void setMsgThread(MsgThread msgThread) {
        this.msgThread = msgThread;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "ChatChoseItem{" +
                "itemType=" + itemType +
                ", msgThread=" + msgThread +
                ", user=" + user +
                '}';
    }
}
