package net.ibaixin.chat.volley.toolbox;

public interface UploadFileCallback {
	void onUploadFilePreExecute();

	void onUploadFileProgressUpdate(int value);

	void doUploadFilePostExecute(String result);
}