package net.ibaixin.chat.activity;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import net.ibaixin.chat.R;
import net.ibaixin.chat.model.web.AttachDto;
import net.ibaixin.chat.util.JSONUtils;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.MimeUtils;
import net.ibaixin.chat.util.Observable;
import net.ibaixin.chat.util.Observer;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.view.ProgressDialog;
import net.ibaixin.chat.volley.toolbox.MultiPartStack;
import net.ibaixin.chat.volley.toolbox.MultiPartStringRequest;

import org.json.JSONArray;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年1月31日 上午11:50:55
 */
public class TestActivity extends BaseActivity implements Observer, View.OnClickListener {
	
	private static String BASE_URL = "http://192.168.0.103:9090/chatapi/api";
	
	private ListView listView;
	private List<String> data;
	private ArrayAdapter<String> adapter;
	
	private EditText etContent;
	private Button btnChoiceFile;
	private Button btnUpload;
	private Button btnRequest;
	private TextView tvResult;
	private EditText etUsername;
	private TextView tvProgress;
	
	private Button btnDownload;
	private TextView tvPath;
	
	ProgressDialog pDialog;
	
	private static RequestQueue mRequestQueue;

	@Override
	protected int getContentView() {
		return R.layout.activity_test;
	}

	@Override
	protected void initView() {
//		listView = (ListView) findViewById(R.id.lv_data);
		etContent = (EditText) findViewById(R.id.et_content);
		btnChoiceFile = (Button) findViewById(R.id.btn_choice_file);
		btnUpload = (Button) findViewById(R.id.btn_upload);
		btnRequest = (Button) findViewById(R.id.btn_request);
		tvResult = (TextView) findViewById(R.id.tv_result);
		etUsername = (EditText) findViewById(R.id.et_username);
		tvProgress = (TextView) findViewById(R.id.tv_progress);
		
		btnDownload = (Button) findViewById(R.id.btn_download);
		tvPath = (TextView) findViewById(R.id.tv_path);
	}
	
	@Override
	protected void initData() {
		/*DataTestManager.getInstance().addObserver(this);
		data = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			data.add("test" + i);
		}
		adapter = new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1, android.R.id.text1, data);
		adapter.registerDataSetObserver(new DataSetObserver() {
		});
		listView.setAdapter(adapter);
		
		listView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				MaterialDialog.Builder builder = new MaterialDialog.Builder(mContext);
				builder.title("测试标题")
				.content("测试内容")
				.positiveText("确定")
				.negativeText("取消")
				.show();
				return true;
			}
		});*/
	}
	
	private void showProgress() {
		pDialog = ProgressDialog.show(mContext, null, getString(R.string.loading));
	}
	
	private void stopProgress() {
		if (pDialog != null) {
			pDialog.dismiss();
		}
	}
	
	public void addPutUploadFileRequest(final String url,
			final Map<String, File[]> files, final Map<String, String> params,
			final Listener<String> responseListener, final ErrorListener errorListener,
			final Object tag) {
		if (null == url || null == responseListener) {
			return;
		}

		MultiPartStringRequest multiPartRequest = new MultiPartStringRequest(
				Request.Method.POST, url, responseListener, errorListener) {

			@Override
			public Map<String, File[]> getFileUploads() {
				return files;
			}

			@Override
			public Map<String, String> getStringUploads() {
				return params;
			}
			
		};
		if (tag != null) {
			multiPartRequest.setTag(tag);
		}
		multiPartRequest.setHandler(new Handler());
		multiPartRequest.setTargetView(tvProgress);
//		multiPartRequest.setProgressCallback(new ProgressUpdateCallback() {
//			
//			@Override
//			public void setProgressUpdateStatus(int value) {
//				
//			}
//		});
		Log.i(TAG, " volley post : uploadFile " + url);

		mRequestQueue.add(multiPartRequest);
	}
	

	@TargetApi(Build.VERSION_CODES.KITKAT)
	@Override
	public void onClick(View v) {
		Uri.Builder builder = Uri.parse(BASE_URL).buildUpon();
		switch (v.getId()) {
		case R.id.btn_choice_file:	//选择文件
			Intent intent = null;
			if (SystemUtil.hasSDK19()) {
				intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
				intent.addCategory(Intent.CATEGORY_OPENABLE);
			} else {
				intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.addCategory(Intent.CATEGORY_DEFAULT);
			}
			intent.setType("*/*");
			startActivityForResult(intent, 100);
			break;
		case R.id.btn_upload:	//上传文件
//			showProgress();
			if (mRequestQueue == null) {
				mRequestQueue = Volley.newRequestQueue(this, new MultiPartStack());
			}
			builder.appendPath("sendFile");
			File file = new File(etContent.getText().toString());
			if (file.exists()) {
				String fileName = file.getName();
				AttachDto attachDto = new AttachDto();
				attachDto.setFileName(fileName);
				attachDto.setReceiver("bbb");
				attachDto.setSender("aaa");
				attachDto.setHash(SystemUtil.encoderByMd5(fileName));
				attachDto.setMimeType(MimeUtils.guessMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(fileName)));
				String jsonStr = JSONUtils.objToJson(attachDto);
				
				Map<String, File[]> files = new HashMap<>();
				files.put("uploadFile", new File[] {file});
				Map<String, String> params = new HashMap<String, String>();
				if (jsonStr != null) {
					Log.d("---------------" + jsonStr);
					params.put("jsonStr", jsonStr);
				}
				addPutUploadFileRequest(builder.toString(), files, params, new Response.Listener<String>() {

					@Override
					public void onResponse(String response) {
						tvResult.setText(response);
//						stopProgress();
					}
				}, new Response.ErrorListener() {

					@Override
					public void onErrorResponse(VolleyError error) {
						tvResult.setText("");
//						stopProgress();
						if (error != null) {
							Log.d("------" + error.getMessage());
						}
					}
				}, null);
			}
			break;
		case R.id.btn_request:	//json数据请求
			showProgress();
			if (mRequestQueue == null) {
				mRequestQueue = Volley.newRequestQueue(this, new MultiPartStack());
			}
			
			builder.appendPath("user").appendPath("users");
			mRequestQueue.add(new JsonArrayRequest(builder.toString(), new Response.Listener<JSONArray>() {

				@Override
				public void onResponse(JSONArray response) {
					stopProgress();
					if (response != null) {
						List<Object> users = JSONUtils.fromJson(response);
						if (!SystemUtil.isEmpty(users)) {
							for (Object obj : users) {
								if (obj instanceof Map) {
									Map<?, ?> map = (Map<?, ?>) obj;
									String id = (String) map.get("id");
									String username = (String) map.get("username");
									String password = (String) map.get("password");
									
									UserModel model = new UserModel(id, username, password);
									Log.d("-----------" + model);
								}
							}
						}
						tvResult.setText(response.toString());
					}
				}
			}, new Response.ErrorListener() {

				@Override
				public void onErrorResponse(VolleyError error) {
					stopProgress();
					if (error != null) {
						Log.d("------" + error.getMessage());
					}
				}
			}));
/*			mRequestQueue.add(new JsonObjectRequest(Request.Method.GET, builder.toString(), null, new Response.Listener<JSONObject>() {

				@Override
				public void onResponse(JSONObject response) {
					stopProgress();
					if (response != null) {
						tvResult.setText(response.toString());
					}
				}
			}, new Response.ErrorListener() {

				@Override
				public void onErrorResponse(VolleyError error) {
					stopProgress();
					if (error != null) {
						Log.d("------" + error.getMessage());
					}
				}
			}));
*/			break;
		case R.id.btn_download:	//download file
			/*DownloadManager downloadManager = new DownloadManager();
			builder.appendPath("receiverFile");
			builder.appendQueryParameter("fileToken", "d2a12961fe4248a257079df27ee1a72b")
				.appendQueryParameter("fileType", String.valueOf(2));
			DownloadRequest request = new DownloadRequest();
			request.setRetryTime(0);
			request.setDestFilePath(getSvaeDir() + "/d2a12961fe4248a257079df27ee1a72b");
			request.setUrl(builder.toString())
					.setDownloadListener(new DownloadListener() {
						
						@Override
						public void onSuccess(int downloadId, String filePath) {
							tvPath.setText("文件下载到：" + filePath);
						}
						
						@Override
						public void onStart(int downloadId, long totalBytes) {
							tvPath.setText("开始下载...");
						}
						
						@Override
						public void onRetry(int downloadId) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void onProgress(int downloadId, long bytesWritten, long totalBytes) {
							if (totalBytes > 0) {
								int progress = (int) (100 * bytesWritten / totalBytes);
								tvPath.setText("已下载:" + progress + "%");
								Log.d("------------" + tvPath.getText().toString());
							} else {
								tvPath.setText("下载失败:文件大小为" + totalBytes);
							}
						}
						
						@Override
						public void onFailure(int downloadId, int statusCode, String errMsg) {
							tvPath.setText("下载失败:" + statusCode + ",失败原因:" + errMsg);
						}
					});
			
			downloadManager.add(request);*/
			setResult(RESULT_OK);
			finish();
			break;
		default:
			break;
		}
	}
	
	public String getSvaeDir() {
		return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
	}
	
	
	@Override
	protected void addListener() {
		btnChoiceFile.setOnClickListener(this);
		btnUpload.setOnClickListener(this);
		btnRequest.setOnClickListener(this);
		btnDownload.setOnClickListener(this);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 1, 0, "添加");
		MenuItem menuItem = menu.getItem(0);
		menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 1:	//添加
//			DataTestManager.getInstance().add(String.valueOf(new Random().nextInt(9999)));
			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onDestroy() {
//		DataTestManager.getInstance().removeObserver(this);
		super.onDestroy();
	}

	@Override
	public void update(Observable<?> observable, int notifyFlag,
			NotifyType updateType, Object data) {
		if (updateType == NotifyType.ADD) {
			this.data.add((String) data);
			adapter.notifyDataSetChanged();
			Log.d(TAG, "----------update------updateType----data--" + updateType.toString() + data);
		}
	}

	@Override
	public void dispatchUpdate(Observable<?> observable, int notifyFlag,
			NotifyType updateType, Object data) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			if (requestCode == 100 && data != null) {
				String filePath = getPath(mContext, data.getData());
				etContent.setText(filePath);
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

    @TargetApi(Build.VERSION_CODES.KITKAT)
	public static String getPath(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/"
                            + split[1];
                }
                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = MediaColumns._ID + "=?";
                final String[] selectionArgs = new String[] { split[1] };
                return getDataColumn(context, contentUri, selection,
                        selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    /**
     * Get the value of the data column for this Uri . This is useful for
     * MediaStore Uris , and other file - based ContentProviders.
     * 
     * @param context
     *            The context.
     * @param uri
     *            The Uri to query.
     * @param selection
     *            (Optional) Filter used in the query.
     * @param selectionArgs
     *            (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri,
            String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = MediaColumns.DATA;
        final String[] projection = { column };
        try {
            cursor = context.getContentResolver().query(uri, projection,
                    selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri
                .getAuthority());
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri
                .getAuthority());
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri
                .getAuthority());
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri
                .getAuthority());
    }

    class UserModel {
    	String id;
    	String username;
    	String password;
    	
		public UserModel(String id, String username, String password) {
			super();
			this.id = id;
			this.username = username;
			this.password = password;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		@Override
		public String toString() {
			return "UserModel [id=" + id + ", username=" + username
					+ ", password=" + password + "]";
		}
    }
    
}
