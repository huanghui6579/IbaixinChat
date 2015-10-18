package net.ibaixin.chat.smack.packet;

import org.jivesoftware.smack.packet.IQ;

/**
 * 电子名片的IQ扩展
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年8月8日 下午5:25:15
 */
public class VcardX extends IQ {

	public static final String ELEMENT = "vCardX";
    public static final String NAMESPACE = "vcardX-temp";
	
	/**
	 * 新头像的hash值
	 */
	private String iconHash;
	
	/**
	 * 头像的mimeType
	 */
	private String mimeType;
	

	public String getIconHash() {
		return iconHash;
	}

	public void setIconHash(String iconHash) {
		this.iconHash = iconHash;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public VcardX() {
		super(ELEMENT, NAMESPACE);
	}
	
	/**
	 * 是否有内容，没有内容，就设置一个空的vcardx
	 * @return
	 * @update 2015年8月8日 下午5:39:06
	 */
	private boolean hasContent() {
		return iconHash != null;
	}

	@Override
	protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
		if (!hasContent()) {
			xml.setEmptyElement();
			return xml;
		}
		xml.rightAngleBracket();
		if (mimeType != null) {
			xml.optElement("MIMETYPE", mimeType);
		}
		if (iconHash != null) {
			xml.optElement("ICONHASH", iconHash);
		}
		return xml;
	}

}
