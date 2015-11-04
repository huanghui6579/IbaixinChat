package net.ibaixin.chat.volley.toolbox;


import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Request.Method;
import com.android.volley.toolbox.HttpClientStack.HttpPatch;
import com.android.volley.toolbox.HurlStack;

import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.util.Log;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * 用于文件上传的扩展
 * @author huanghui1
 *
 */
public class MultiPartStack extends HurlStack {
	@SuppressWarnings("unused")
	private static final String TAG = MultiPartStack.class.getSimpleName();
    private final static String HEADER_CONTENT_TYPE = "Content-Type";
    
	@Override
	public HttpResponse performRequest(Request<?> request,
			Map<String, String> additionalHeaders) throws IOException, AuthFailureError {
		
		if(!(request instanceof MultiPartRequest)) {
			return super.performRequest(request, additionalHeaders);
		}
		else {
			return performMultiPartRequest(request, additionalHeaders);
		}
	}
	
    private void addHeaders(HttpUriRequest httpRequest, Map<String, String> headers) {
        for (String key : headers.keySet()) {
            httpRequest.setHeader(key, headers.get(key));
        }
    }
	
	public HttpResponse performMultiPartRequest(Request<?> request,
			Map<String, String> additionalHeaders)  throws IOException, AuthFailureError {
		HttpRequestBase httpRequest = createMultiPartRequest(request, additionalHeaders);
        addHeaders(httpRequest, additionalHeaders);
        addHeaders(httpRequest, request.getHeaders());
        
        HttpParams httpParams = httpRequest.getParams();
        int timeoutMs = request.getTimeoutMs();

        if(timeoutMs != -1) {
        	HttpConnectionParams.setSoTimeout(httpParams, timeoutMs * 2);
			HttpConnectionParams.setConnectionTimeout(httpParams, timeoutMs);
        }
        
        /* Make a thread safe connection manager for the client */
//        AndroidHttpClient httpClient = AndroidHttpClient.newInstance(getUserAgent(ChatApplication.getInstance()));
        CloseableHttpClient httpClient = HttpClientBuilder.create().setUserAgent(getUserAgent(ChatApplication.getInstance())).build();
        HttpResponse httpResponse = httpClient.execute(httpRequest);
        return httpResponse;
	}
	
	/**
	 * 获取userAgent
	 * @param context
	 * @return
	 */
	private String getUserAgent(Context context) {
		String userAgent = "volley/0";
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            userAgent = packageName + "/" + info.versionCode;
        } catch (NameNotFoundException e) {
        }

        return userAgent;
	}

    private HttpRequestBase createMultiPartRequest(Request<?> request,
            Map<String, String> additionalHeaders) throws AuthFailureError {
        switch (request.getMethod()) {
            case Method.DEPRECATED_GET_OR_POST: {
                // This is the deprecated way that needs to be handled for backwards compatibility.
                // If the request's post body is null, then the assumption is that the request is
                // GET.  Otherwise, it is assumed that the request is a POST.
                byte[] postBody = request.getBody();
                if (postBody != null) {
                    HttpPost postRequest = new HttpPost(request.getUrl());
                    if(request.getBodyContentType() != null)
                    	postRequest.addHeader(HEADER_CONTENT_TYPE, request.getBodyContentType());
                    HttpEntity entity;
                    entity = new ByteArrayEntity(postBody);
                    postRequest.setEntity(entity);
                    return postRequest;
                } else {
                    return new HttpGet(request.getUrl());
                }
            }
            case Method.GET:
                return new HttpGet(request.getUrl());
            case Method.DELETE:
                return new HttpDelete(request.getUrl());
            case Method.POST: {
                HttpPost postRequest = new HttpPost(request.getUrl());
                if(request.getBodyContentType() != null) {
                	postRequest.addHeader(HEADER_CONTENT_TYPE, request.getBodyContentType());
                }
                setMultiPartBody(postRequest,request);
                return postRequest;
            }
            case Method.PUT: {
                HttpPut putRequest = new HttpPut(request.getUrl());
                if(request.getBodyContentType() != null)
                	putRequest.addHeader(HEADER_CONTENT_TYPE, request.getBodyContentType());
                setMultiPartBody(putRequest,request);
                return putRequest;
            }
            // Added in source code of Volley libray.
            case Method.PATCH: {
            	HttpPatch patchRequest = new HttpPatch(request.getUrl());
            	if(request.getBodyContentType() != null)
            		patchRequest.addHeader(HEADER_CONTENT_TYPE, request.getBodyContentType());
                return patchRequest;
            }
            default:
                throw new IllegalStateException("Unknown request method.");
        }
    }
	
	/**
	 * If Request is MultiPartRequest type, then set MultipartEntity in the
	 * httpRequest object.
	 * 
	 * @param httpRequest
	 * @param request
	 * @throws AuthFailureError
	 */
	private void setMultiPartBody(HttpEntityEnclosingRequestBase httpRequest,
			Request<?> request) throws AuthFailureError {

		// Return if Request is not MultiPartRequest
		if (!(request instanceof MultiPartRequest)) {
			return;
		}

		// MultipartEntity multipartEntity = new
		// MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		
		final MultiPartRequest partRequest = ((MultiPartRequest) request);

		// 设置为浏览器兼容模式  
		builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		builder.setCharset(Consts.UTF_8);

		// Iterate the fileUploads
		Map<String, File[]> fileUpload = partRequest.getFileUploads();
		if (fileUpload != null && fileUpload.size() > 0) {
			Set<Entry<String, File[]>> entries = fileUpload.entrySet();
			for (Entry<String, File[]> entry : entries) {
				String key = entry.getKey();
				File[] files = entry.getValue();
				if (files != null && files.length > 0) {
					for (File file : files) {
						if (file != null && file.exists()) {
							builder.addPart(key, new FileBody(file));
						}
					}
				}
			}
		}

//		ContentType contentType = ContentType.create(HTTP.PLAIN_TEXT_TYPE, HTTP.UTF_8);
		// Iterate the stringUploads
		Map<String, String> stringUpload = partRequest.getStringUploads();
		if (stringUpload != null && stringUpload.size() > 0) {
			Set<Entry<String, String>> contentEntriers = stringUpload.entrySet();
			for (Entry<String, String> entry : contentEntriers) {
				try {
					String key = entry.getKey();
					String value = entry.getValue();
					if (key != null && value != null) {
						builder.addPart(key, new StringBody(value, ContentType.APPLICATION_JSON));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		HttpEntity entity = builder.build();
		
		final long totalSize = entity.getContentLength();
		
		final ProgressUpdateCallback callback = partRequest.getProgressCallback();
		
		Handler handler = partRequest.getHandler();
		if (handler == null) {
			handler = new Handler(Looper.getMainLooper());
		}
		final Handler tHandler = handler;
		ProgressOutHttpEntity outHttpEntity = new ProgressOutHttpEntity(entity, new ProgressListenerCallback() {
			
			@Override
			public void transferred(final long transferedBytes) {
				if (callback != null) {
					tHandler.post(new Runnable() {
						
						@Override
						public void run() {
							callback.setProgressUpdateStatus((int) (100 * transferedBytes / totalSize));
						}
					});
				} else {
					final View view = partRequest.getTargetView();
					if (view != null) {
						if (view instanceof TextView) {
							tHandler.post(new Runnable() {
								
								@Override
								public void run() {
									((TextView) view).setText(String.format("上传了%d", (int) (100 * transferedBytes / totalSize)));
								}
							});
						}
					}
				}
				Log.d("------progress-----" + ((int) (100 * transferedBytes / totalSize)));
			}
		});
		
		httpRequest.setEntity(outHttpEntity);
	}

}
