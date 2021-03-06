package net.ibaixin.chat.model;

import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.SystemUtil;

import java.io.File;

/**
 * @author tiger
 * @version 1.0.0
 * @update 2015/10/24 11:02
 */
public abstract class DownloadItem {
    /**
     * 消息的id
     */
    protected String msgId;
    /**
     * 文件名
     */
    protected String fileName;
    /**
     * 文件的mime
     */
    protected String mime;
    /**
     * 文件的全路径
     */
    protected String filePath;
    /**
     * 文件的大小
     */
    protected long size;

    /**
     * 文件的修改时间
     */
    protected long time;

    /**
     * 是否需要下载文件
     */
    protected boolean needDownload;

    /**
     * 下载文件的凭证
     */
    protected String fileToken;

    /**
     * 下载文件的类型，1：表示缩略图，2：表示原始文件
     */
    protected int downloadType;

    /**
     * 下载的文件类型，默认为文件
     */
    protected FileItem.FileType fileType = FileItem.FileType.FILE;

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public boolean isNeedDownload() {
        return needDownload;
    }

    public void setNeedDownload(boolean needDownload) {
        this.needDownload = needDownload;
    }

    public String getFileToken() {
        return fileToken;
    }

    public void setFileToken(String fileToken) {
        this.fileToken = fileToken;
    }

    public int getDownloadType() {
        return downloadType;
    }

    public void setDownloadType(int downloadType) {
        this.downloadType = downloadType;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public FileItem.FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileItem.FileType fileType) {
        this.fileType = fileType;
    }

    public String getFileName() {
        if (fileName == null) {
            if (filePath != null) {
                File file = new File(filePath);
                fileName = file.getName();
            }
        }
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMime() {
        return mime;
    }

    public void setMime(String mime) {
        this.mime = mime;
    }
    
    /**
     * 删除该项
     * @author tiger
     * @update 2016/1/17 15:05
     * @version 1.0.0
     * @return 是否删除成功
     */
    public boolean deleteItem() {
        if (filePath != null) {
            try {
                File file = new File(filePath);
                return file.delete();
            } catch (Exception e) {
                Log.e(e.getMessage());
            } finally {
                try {
                    if (fileType == FileItem.FileType.IMAGE || fileType == FileItem.FileType.VIDEO) {
                        SystemUtil.removeImageCache(filePath);
                    }
                } catch (Exception e) {
                    Log.e(e.getMessage());
                }
            }
            return false;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "DownloadItem{" +
                "msgId='" + msgId + '\'' +
                ", fileName='" + fileName + '\'' +
                ", mime='" + mime + '\'' +
                ", filePath='" + filePath + '\'' +
                ", size=" + size +
                ", time=" + time +
                ", needDownload=" + needDownload +
                ", fileToken='" + fileToken + '\'' +
                ", downloadType=" + downloadType +
                ", fileType=" + fileType +
                '}';
    }
}
