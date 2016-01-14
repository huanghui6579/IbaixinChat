package net.ibaixin.chat.model;

/**
 * 添加附件的实体
 * 
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年10月28日 下午4:23:29
 */
public class AttachItem {
	/**
	 * 图片
	 */
	public static final int ACTION_IMAGE = 1;
	/**
	 * 音频
	 */
	public static final int ACTION_AUDIO = 2;
	/**
	 * 视频
	 */
	public static final int ACTION_VIDEO = 3;
	/**
	 * 位置
	 */
	public static final int ACTION_LOCATION = 4;
	/**
	 * 名片
	 */
	public static final int ACTION_VCARD = 5;
	/**
	 * 文件
	 */
	public static final int ACTION_FILE = 6;
	/**
	 * 语音通话
	 */
	public static final int ACTION_CALL_AUDIO = 7;
	/**
	 * 视频通话
	 */
	public static final int ACTION_CALL_VIDEO = 8;

	/**
	 * 图像资源id
	 */
	private int resId;
	/**
	 * 投标名称
	 */
	private String name;
	/**
	 * 投标对应的操作
	 */
	private int action;

	public int getResId() {
		return resId;
	}

	public void setResId(int resId) {
		this.resId = resId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAction() {
		return action;
	}

	public void setAction(int action) {
		this.action = action;
	}
}
