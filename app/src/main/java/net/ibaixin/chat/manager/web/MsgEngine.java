package net.ibaixin.chat.manager.web;

import android.content.Context;
import android.net.Uri;

import net.ibaixin.chat.download.DownloadListener;
import net.ibaixin.chat.download.DownloadManager;
import net.ibaixin.chat.download.DownloadRequest;
import net.ibaixin.chat.model.DownloadItem;
import net.ibaixin.chat.model.PhotoItem;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.Log;

/**
 * 处理消息与web服务器之间交互的服务层
 * @author tiger
 * @version 1.0.0
 * @update 2015/10/24 11:06
 */
public class MsgEngine {
    private Context mContext;
    private DownloadManager mDownloadManager;

    public MsgEngine(Context context) {
        this.mContext = context;
        mDownloadManager = new DownloadManager();
    }

    /**
     * 下载文件
     * @param downloadItem 文件实体
     * @param downloadListener 文件下载的监听器
     */
    public void downloadFile(DownloadItem downloadItem, DownloadListener downloadListener) {
        downloadFile(null, downloadItem, downloadListener);
    }

    /**
     * 下载文件
     * @param downloadManager 下载管理器
     * @param downloadItem 文件实体
     * @param downloadListener 文件下载的监听器
     */
    public void downloadFile(DownloadManager downloadManager, DownloadItem downloadItem, DownloadListener downloadListener) {
        if (downloadItem != null) {
            int fileType = downloadItem.getDownloadType();
            String fileToken = downloadItem.getFileToken();
            Uri uri = Uri.parse(Constants.BASE_API_URL);
            Uri.Builder builder = uri.buildUpon();
            builder.appendPath("receiverFile")
                    .appendQueryParameter("fileToken", fileToken)
                    .appendQueryParameter("fileType", String.valueOf(fileType));
            String destFilePath = downloadItem.getFilePath();
            switch (fileType) {
                case Constants.FILE_TYPE_THUMB: //下载缩略图
                    if (downloadItem instanceof  PhotoItem) {
                        PhotoItem photoItem = (PhotoItem) downloadItem;
                        destFilePath = photoItem.getThumbPath();
                    }
                    break;
                default:
                    break;
            }
            DownloadRequest downloadRequest = new DownloadRequest();
            downloadRequest.setUrl(builder.toString())
                    .setDownloadId(downloadItem.getMsgId().hashCode())
                    .setDestFilePath(destFilePath)
                    .setDownloadListener(downloadListener);
            if (downloadManager != null) {
                downloadManager.add(downloadRequest);
            } else {
                mDownloadManager.add(downloadRequest);
            }
        } else {
            Log.w("-----downloadFile----downloadItem---is null-----" + downloadItem);
        }
    }
}
