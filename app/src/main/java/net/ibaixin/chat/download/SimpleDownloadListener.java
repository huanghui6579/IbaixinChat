package net.ibaixin.chat.download;

/**
 * Interface definition for a callback to be invoked when downloading. This
 * is a simple download listener, it's only contains two method, so if detail
 * download information is needed, then use {@link DownloadListener}
 *
 * @author Vincent Cheung
 * @since  Jul. 22, 2015
 */
public interface SimpleDownloadListener {
	/**
	 * Invoked when downloading successfully.
	 */
	void onSuccess(int downloadId, String filePath);
	/**
	 * Invoked when downloading failed.
	 */
	void onFailure(int downloadId, int statusCode, String errMsg);
}
