package net.ibaixin.chat.model;

/**
 * 头像实体
 * 
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年11月11日 下午10:22:51
 */
public class HeadIcon {
	/**
	 * 文件的完整路径
	 */
	private String filePath;
	/**
	 * 文件对应的hash值，算法是SHA-1
	 */
	private String hash;

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public HeadIcon(String filePath, String hash) {
		super();
		this.filePath = filePath;
		this.hash = hash;
	}

	public HeadIcon() {
		super();
	}

	@Override
	public String toString() {
		return "HeadIcon [filePath=" + filePath + ", hash=" + hash + "]";
	}
}
