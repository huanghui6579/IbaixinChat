package net.ibaixin.chat.model;

import java.util.List;
import java.util.Map;

/**
 * 相册中存放的临时集合
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年11月14日 下午3:35:28
 */
public class Album {
	/**
	 * 文件列表
	 */
	private List<PhotoItem> mPhotos;
	/**
	 * 文件按分组的集合
	 */
	private Map<String, List<PhotoItem>> folderMap;

	public List<PhotoItem> getmPhotos() {
		return mPhotos;
	}

	public void setmPhotos(List<PhotoItem> mPhotos) {
		this.mPhotos = mPhotos;
	}

	public Map<String, List<PhotoItem>> getFolderMap() {
		return folderMap;
	}

	public void setFolderMap(Map<String, List<PhotoItem>> folderMap) {
		this.folderMap = folderMap;
	}
}