package net.ibaixin.chat.model;

/**
 *
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年10月27日 上午11:14:53
 */
public class Emoji {
	/**
	 * 表情资源
	 */
	public static final int TYPE_EMOJI = 1;
	/**
	 * 删除类型
	 */
	public static final int TYPE_DEL = 2;
	/**
	 * 空的类型
	 */
	public static final int TYPE_EMPTY = 3;
	
	/**
	 * 资源id
	 */
	private int resId;
	
	/**
	 * 表情的资源类型，分为“表情、删除、空”三种，默认是“表情类型”
	 */
	private int resTpe = TYPE_EMOJI;
	
	/**
	 * 表情描述文字
	 */
	private String description;
	
	/**
	 * 表情文件名 
	 */
	private String faceName;

	public int getResId() {
		return resId;
	}

	public void setResId(int resId) {
		this.resId = resId;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getFaceName() {
		return faceName;
	}

	public void setFaceName(String faceName) {
		this.faceName = faceName;
	}

	public int getResTpe() {
		return resTpe;
	}

	public void setResTpe(int resTpe) {
		this.resTpe = resTpe;
	}

	@Override
	public String toString() {
		return "Emoji [resId=" + resId + ", description=" + description
				+ ", faceName=" + faceName + "]";
	}
}
