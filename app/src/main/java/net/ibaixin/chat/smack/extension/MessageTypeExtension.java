package net.ibaixin.chat.smack.extension;

import net.ibaixin.chat.model.MsgInfo;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

import android.text.TextUtils;

/**
 * 丰富消息的扩展，包括文件，语音、图片、地理位置等非普通文本消息
 * @author tiger
 * @version 2015年4月25日 下午6:29:04
 */
public class MessageTypeExtension implements ExtensionElement {
	
	public static final String NAMESPACE = "urn:xmpp:messageType";
    public static final String ELEMENT = "messagetype";
    
    /**
     * 消息的类型，主要是图片消息和其他文件消息
     */
    private MsgInfo.Type msgType;
    
    /**
     * 文件的名称 
     */
    private String fileName;
    
    /**
     * 缩略图的名称
     */
	private String thumbName;
	
	/**
	 * 文件的mime类型
	 */
	private String mimeType;
	
	/**
	 * 文件的hash值
	 */
	private String hash;
	
	/**
	 * 文件的主键，根据该主键来下载文件
	 */
	private String fileId;
	
	/**
	 * 对该文件的一些描述，可作为扩展字段
	 */
	private String desc;
	
	/**
	 * 文件的大小
	 */
	private long size;

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getThumbName() {
		return thumbName;
	}

	public void setThumbName(String thumbName) {
		this.thumbName = thumbName;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public MsgInfo.Type getMsgType() {
		return msgType;
	}

	public void setMsgType(MsgInfo.Type msgType) {
		this.msgType = msgType;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getFileId() {
		return fileId;
	}

	public void setFileId(String fileId) {
		this.fileId = fileId;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	@Override
	public String getElementName() {
		return ELEMENT;
	}

	@Override
	public CharSequence toXML() {
		XmlStringBuilder buf = new XmlStringBuilder(this);
		buf.attribute("type", msgType.ordinal());
		buf.attribute("fileId", fileId);
		buf.rightAngleBracket();
		buf.element("fileName", fileName);
		if (!TextUtils.isEmpty(thumbName)) {
			buf.element("thumbName", thumbName);
		}
		buf.element("mimeType", mimeType);
		buf.element("hash", hash);
		buf.element("size", String.valueOf(size));
		if (!TextUtils.isEmpty(desc)) {
			buf.element("desc", desc);
		}
		buf.closeElement(this);
		return buf;
	}

	@Override
	public String getNamespace() {
		return NAMESPACE;
	}

	@Override
	public String toString() {
		return "MessageTypeExtension [msgType=" + msgType + ", fileName="
				+ fileName + ", thumbName=" + thumbName + ", mimeType="
				+ mimeType + ", hash=" + hash + ", fileId=" + fileId
				+ ", desc=" + desc + ", size=" + size + "]";
	}
}
