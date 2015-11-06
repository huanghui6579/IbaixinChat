package net.ibaixin.chat.model;

/**
 * 消息上传的实体，主要用于文件的上传
 * 创建人：huanghui1
 * 创建时间： 2015/11/6 18:02
 * 修改人：huanghui1
 * 修改时间：2015/11/6 18:02
 * 修改备注：
 *
 * @version: 0.0.1
 */
public class MsgUploadInfo {
    /**
     * 消息实体
     */
    private MsgInfo msgInfo;
    /**
     * 上传的进度
     */
    private int progress;

    public MsgUploadInfo(MsgInfo msgInfo, int progress) {
        this.msgInfo = msgInfo;
        this.progress = progress;
    }

    public MsgUploadInfo() {
    }

    public MsgInfo getMsgInfo() {
        return msgInfo;
    }

    public void setMsgInfo(MsgInfo msgInfo) {
        this.msgInfo = msgInfo;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    @Override
    public String toString() {
        return "MsgUploadInfo{" +
                "msgInfo=" + msgInfo +
                ", progress=" + progress +
                '}';
    }
}
