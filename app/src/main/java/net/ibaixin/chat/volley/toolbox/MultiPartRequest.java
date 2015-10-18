package net.ibaixin.chat.volley.toolbox;

import java.io.File;
import java.util.Map;

import android.os.Handler;
import android.view.View;

/**
 * @author ZhiCheng Guo
 * @version 2014年10月7日 上午11:04:36
 */
public interface MultiPartRequest {

    public void addFileUpload(String param, File... file);
    
    public void addStringUpload(String param, String content);
    
    public Map<String,File[]> getFileUploads();
    
    public Map<String,String> getStringUploads(); 
    
    public void setHandler(Handler handler);
    
    public Handler getHandler();
    
    public View getTargetView();
    
    public void setTargetView(View view);
    
    public void setProgressCallback(ProgressUpdateCallback callback);
    
    public ProgressUpdateCallback getProgressCallback();
}