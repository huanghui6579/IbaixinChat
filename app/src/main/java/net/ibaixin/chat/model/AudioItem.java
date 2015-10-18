package net.ibaixin.chat.model;

/**
 * 音乐实体类
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年11月22日 下午2:53:31
 */
public class AudioItem {
	/**
	 * 音乐显示的标题
	 */
	private String title;
	/**
	 * 文件的名称
	 */
	private String fileName;
	/**
	 * 文件的全路径
	 */
	private String filePath;
	/**
	 * 艺术家
	 */
	private String artist;
	/**
	 * 文件的大小
	 */
	private long size;
	/**
	 * 音乐的时长
	 */
	private int duration;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}
}
