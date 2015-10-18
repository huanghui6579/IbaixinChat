package net.ibaixin.chat.model;

import java.io.File;
import java.util.Comparator;

/**
 * 文件实体
 * @author huanghui1
 * @version 1.0.0
 * @update 2014年11月21日 上午11:36:11
 */
public class FileItem implements Comparator<FileItem> {
	private File file;
	private FileType fileType;
	
	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public FileType getFileType() {
		return fileType;
	}

	public void setFileType(FileType fileType) {
		this.fileType = fileType;
	}

	public enum FileType {
		TEXT,
		IMAGE,
		AUDIO,
		VIDEO,
		APK,
		FILE;
	}

	@Override
	public String toString() {
		return "FileItem [file=" + file + ", fileType=" + fileType + "]";
	}

	@Override
	public int compare(FileItem lhs, FileItem rhs) {
		File lf = lhs.getFile();
		File rf = rhs.getFile();
		if (lf.isDirectory() && rf.isFile()) {
			return -1;
		} else if (lf.isFile() && rf.isDirectory()) {
			return 1;
		} else {
			return lf.getName().compareTo(rf.getName());
		}
	}
}
