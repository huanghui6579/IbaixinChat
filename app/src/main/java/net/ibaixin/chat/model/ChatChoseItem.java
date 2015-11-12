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
     * 组标题类型
     */
    public static final int TYPE_GROUP = 0;
    /**
     * 列表项类型
     */
    public static final int TYPE_ITEM = 1;
    /**
     * 会话的类型
     */
    public static final int DATA_THREAD = 0;
    /**
     * 联系人的类型
     */
    public static final int DATA_CONTACT = 1;

    /**
     * 默认是组标题的类型
     */
    private int itemType = TYPE_GROUP;

    /**
     * 默认是会话的类型
     */
    private int dataType = DATA_THREAD;
    
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

    public int getDataType() {
        return dataType;
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }
    
    /**
     * 是否是列表项
     * 创建人：huanghui1
     * 创建时间： 2015/11/12 17:27
     * 修改人：huanghui1
     * 修改时间：2015/11/12 17:27
     * 修改备注：
     * @version: 0.0.1
     */
    public boolean isItemType() {
        return ChatChoseItem.TYPE_ITEM == itemType;
    }

    @Override
    public String toString() {
        return "ChatChoseItem{" +
                "itemType=" + itemType +
                ", dataType=" + dataType +
                ", msgThread=" + msgThread +
                ", user=" + user +
                '}';
    }
}
