package net.ibaixin.chat.manager.web;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.download.ImageDownloader.Scheme;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.download.DownloadManager;
import net.ibaixin.chat.download.DownloadRequest;
import net.ibaixin.chat.download.SimpleDownloadListener;
import net.ibaixin.chat.manager.PersonalManage;
import net.ibaixin.chat.model.ActionResult;
import net.ibaixin.chat.model.Personal;
import net.ibaixin.chat.model.web.AttachDto;
import net.ibaixin.chat.model.web.VcardDto;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.ImageUtil;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.volley.toolbox.MultiPartStringRequest;

/**
 * 个性信息与web服务器连接管理的服务层
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年7月20日 下午2:25:03
 */
public class PersonalEngine {
	private Context mContext;
	
	public PersonalEngine(Context mContext) {
		super();
		this.mContext = mContext;
	}

	/**
	 * 从web服务器上获取对应用户的个性信息
	 * @param username 用户名，不能为空
	 * @return personal实体对象
	 * @update 2015年7月20日 下午2:34:05
	 */
	public void getPersonalInfo(final String username) {
		if (!TextUtils.isEmpty(username)) {
			final Uri uri = Uri.parse(Constants.BASE_API_URL);
			Uri.Builder builder = uri.buildUpon();
			builder.appendPath("user")
				.appendPath("vcard")
				.appendEncodedPath(username);
			JsonObjectRequest jsonRequest = new JsonObjectRequest(builder.toString(), null, new Response.Listener<JSONObject>() {

				@Override
				public void onResponse(JSONObject response) {
					if (response != null) {
						final Gson gson = new Gson();
						ActionResult<VcardDto> result = gson.fromJson(response.toString(), new TypeToken<ActionResult<VcardDto>>() {}.getType());
						if (result != null) {
							int resultCode = result.getResultCode();
							final ChatApplication app = (ChatApplication) mContext.getApplicationContext();
							final Personal person = app.getCurrentUser();
							switch (resultCode) {
							case ActionResult.CODE_SUCCESS:	//成功
								final VcardDto vcardDto = result.getData();
								if (vcardDto != null) {
									Log.d("------getPersonalInfo---username--" + username + "----成功---vcardDto---" + vcardDto);
									//头像的hash
									final String hash = vcardDto.getHash();
									//本地头像的hash
									final String localHash = person.getIconHash();
									
									final String username = person.getUsername();
									
									
									if (TextUtils.isEmpty(localHash) || !SystemUtil.isFileExists(person.getThumbPath())) {	//本地头像为空
										Log.d("----本地头像为空-----");
										if (!TextUtils.isEmpty(hash)) {	//服务器头像不为空,则下载web服务器上的头像
											Log.d("----服务器头像不为空--hash---" + hash);
											//下载头像的地址,此时只下载头像的缩略图,fileType--1：表示缩略图，2：表示原始图片
											Uri.Builder downloadBuilder = uri.buildUpon();
											downloadBuilder.appendPath("user")
												.appendPath("avatar")
												.appendPath(username)
												.appendQueryParameter("fileType", String.valueOf(Constants.FILE_TYPE_THUMB));
											
											DownloadManager manager = new DownloadManager();
											File iconFile = SystemUtil.generateIconFile(username, Constants.FILE_TYPE_THUMB);
											String iconPath = iconFile.getAbsolutePath();
											int downloadId = iconPath.hashCode();
											DownloadRequest downloadRequest = new DownloadRequest();
											downloadRequest.setUrl(downloadBuilder.toString())
												.setDownloadId(downloadId)
												.setDestFilePath(iconPath);
											downloadRequest.setSimpleDownloadListener(new SimpleDownloadListener() {
												
												@Override
												public void onSuccess(int downloadId, String filePath) {
													Log.d("----getPersonalInfo---download head icon onSuccess---username--" + username + "---filePath-----" + filePath);
													person.setIconHash(hash);
													person.setMimeType(vcardDto.getMimeType());
													//下载头像成功后，更新用户电子名片信息
													person.setThumbPath(filePath);
													//清除原始头像
													String oldPath = person.getIconPath();
													SystemUtil.deleteFile(oldPath);
													person.setIconPath(null);
													
													//更新头像信息
													PersonalManage personalManage = PersonalManage.getInstance();
													personalManage.updateHeadIcon(person);
												}
												
												@Override
												public void onFailure(int downloadId, int statusCode, String errMsg) {
													Log.e("----getPersonalInfo---download head icon failed---username--" + username + "---statusCode-----" + statusCode + "----errMsg--" + errMsg);
												}
											});
											
											manager.add(downloadRequest);
										}
									} else {	//本地头像不为空
										if (TextUtils.isEmpty(hash) || !localHash.equals(hash)) {	//服务器上头像为空，或者服务器上头像与本地不一致，则以本地为主，将上传本地头像到服务器
											Log.d("---服务器头像为空或者与本地不一致--localHash---" + localHash + "-----hash----" + hash);
											//上传本地头像
											final String iconPath = person.getIconPath();
											final File[] fileArray = new File[2];
											//第一个文件为原始图像
											fileArray[0] = new File(iconPath);
											//缩略图文件保存路径
											String thumbIconPath = person.getThumbPath();
											if (!SystemUtil.isFileExists(thumbIconPath)) {	//本地缩略图不存在，则需重新生成
												thumbIconPath = SystemUtil.generateIconPath(username, Constants.FILE_TYPE_THUMB);
												
												//设置缩略图地址
												person.setThumbPath(thumbIconPath);
												person.setIconHash(localHash);
												person.setMimeType(vcardDto.getMimeType());

												//第二个文件为缩略图
												SystemUtil.getCachedThreadPool().execute(new Runnable() {
													@Override
													public void run() {
														boolean success = ImageUtil.generateThumbImage(iconPath, person.getThumbPath());
														if (success) {
															fileArray[1] = new File(person.getThumbPath());
														}
														uploadAvatar(app, person, fileArray);
													}
												});
												
												
												/*ImageUtil.generateThumbImageAsync(iconPath, thumbIconPath, new SimpleImageLoadingListener() {
													
													@Override
													public void onLoadingFailed(String imageUri, View view,
															FailReason failReason) {
														uploadAvatar(app, person, fileArray);
													}
													
													@Override
													public void onLoadingComplete(String imageUri, View view,
															Bitmap loadedImage) {
														fileArray[1] = new File(Scheme.FILE.crop(imageUri));
														uploadAvatar(app, person, fileArray);
													}
													
												});*/
											} else {	//缩略图存在，则直接设置参数
												fileArray[1] = new File(thumbIconPath);
												uploadAvatar(app, person, fileArray);
											}
										} else {
											Log.d("-----------服务器与本地头像一致-------username------" +username);
										}
									}
									person.setCity(vcardDto.getCity());
									person.setCityId(vcardDto.getCityId());
									person.setDesc(vcardDto.getSignature());
									person.setNickname(vcardDto.getNickName());
									person.setPhone(vcardDto.getMobilePhone());
									person.setProvince(vcardDto.getProvince());
									person.setProvinceId(vcardDto.getProvinceId());
									person.setCountry(vcardDto.getCountry());
									person.setCountryId(vcardDto.getCountryId());
									person.setRealName(vcardDto.getRealName());
									person.setSex(vcardDto.getGender());
									person.setStreet(vcardDto.getStreet());
									PersonalManage personalManage = PersonalManage.getInstance();
									personalManage.saveOrUpdateCurrentUser(person);
								}
								break;
							case ActionResult.CODE_ERROR:	//请求处理错误
								Log.e("-----getPersonalInfo------" + username + "---CODE_ERROR---" + resultCode);
								break;
							case ActionResult.CODE_ERROR_PARAM:	//错误的参数
								Log.e("-----getPersonalInfo------" + username + "---CODE_ERROR_PARAM---" + resultCode);
								break;
							case ActionResult.CODE_NO_DATA:	//没有数据，则首次添加用户信息
								Log.e("-----getPersonalInfo------" + username + "---CODE_NO_DATA---" + resultCode);
								addPersonalInfo(person);
								break;
							default:
								break;
							}
						}
					}
				}
			}, new Response.ErrorListener() {

				@Override
				public void onErrorResponse(VolleyError error) {
					Log.e(error.getMessage(), error);
				}
			});
			ChatApplication.getInstance().addToRequestQueue(jsonRequest, "getPersonalInfo");
		} else {
			Log.d("-----getPersonalInfo---用户名为空---" + username);
		}
	}
	
	/**
	 * 设置头像上传的参数
	 * @param url 上传地址
	 * @param person 个人信息实体
	 * @param fileArray 文件数组
	 * @param params 需要提交的额外的参数
	 * @param tag 网络请求的唯一标识
	 * @param listener 网络请求后的回调监听
	 * @param errorListener 网络请求失败后的监听
	 * @return
	 * @update 2015年7月28日 上午9:33:10
	 */
	private MultiPartStringRequest initMultiPartRequest(String url, Personal person, File[] fileArray, Map<String, String> params, String tag, Response.Listener<String> listener, Response.ErrorListener errorListener) {
		Gson gson = new Gson();
		String username = person.getUsername();
		String iconPath = person.getIconPath();
		String localHash = person.getIconHash();
		final String iconName = SystemUtil.getFilename(iconPath);
		Map<String, File[]> files = null;
		if (!SystemUtil.isEmpty(fileArray)) {
			files = new HashMap<>();
			files.put("avatarFile", fileArray);
		}
		if (params == null) {
			params = new HashMap<>();
			AttachDto attachDto = new AttachDto();
			attachDto.setSender(username);
			attachDto.setFileName(iconName);
			attachDto.setHash(localHash);
			
			String mimeType = person.getMimeType();
			if (TextUtils.isEmpty(mimeType)) {	//默认为图片类型
				mimeType = Constants.MIME_IMAGE;
			}
			attachDto.setMimeType(mimeType);
			String jsonStr = gson.toJson(attachDto);
			if (!TextUtils.isEmpty(jsonStr)) {
				params.put("jsonStr", jsonStr);
			}
		}
		Log.d("-------initMultiPartRequest-----params---" + params);
		if (listener == null) {
			listener = new Response.Listener<String>() {

				@Override
				public void onResponse(String response) {
					Log.d("---initMultiPartRequest-----response---" + response);
				}
				
			};
		}
		if (errorListener == null) {
			errorListener = new Response.ErrorListener() {

				@Override
				public void onErrorResponse(VolleyError error) {
					Log.e("---initMultiPartRequest-----error---" + error.getMessage());
				}
				
			};
		}
		if (tag == null) {
			tag = username;
		}
		MultiPartStringRequest multiPartRequest = SystemUtil.getUploadFileRequest(url, files, params, listener, errorListener, tag, null);
		return multiPartRequest;
	}
	
	/**
	 * 将个人信息保存到web服务器上
	 * @param person
	 * @update 2015年7月21日 下午7:39:29
	 */
	private void addPersonalInfo(final Personal person) {
		if (person != null) {
			
			final String username = person.getUsername();
			final String iconHash = person.getIconHash();
			
			VcardDto vcardDto = new VcardDto();
			vcardDto.setCountry(person.getCountry());
			vcardDto.setCountryId(person.getCountryId());
			vcardDto.setMimeType(person.getMimeType());
			vcardDto.setCity(person.getCity());
			vcardDto.setCityId(person.getCityId());
			vcardDto.setGender(person.getSex());
			vcardDto.setHash(iconHash);
			vcardDto.setMobilePhone(person.getPhone());
			vcardDto.setNickName(person.getNickname());
			vcardDto.setProvince(person.getProvince());
			vcardDto.setProvinceId(person.getProvinceId());
			vcardDto.setRealName(person.getRealName());
			vcardDto.setSignature(person.getDesc());
			vcardDto.setStreet(person.getStreet());
			vcardDto.setUsername(username);
			
			final Gson gson = new Gson();
			try {
				final String jsonStr = gson.toJson(vcardDto);
				if (!TextUtils.isEmpty(jsonStr)) {
					Uri uri = Uri.parse(Constants.BASE_API_URL);
					Uri.Builder builder = uri.buildUpon();
					builder.appendPath("user")
						.appendPath("vcard")
						.appendPath("add");
					
					String iconPath = person.getIconPath();
					String thumbIconPath = person.getThumbPath();
					boolean isIconExists = SystemUtil.isFileExists(iconPath);	//头像文件是否存在
					boolean isIconThumbExists = SystemUtil.isFileExists(thumbIconPath);	//头像文件的缩略图是否存在
					
					Map<String, String> params = new HashMap<>();
					
					params.put("jsonStr", jsonStr);
					
					File[] fileArray = null;
					if (isIconExists || isIconThumbExists) {	//文件头像存在或者文件的缩略图存在
						try {
							String iconName = SystemUtil.generateIconName(username, Constants.FILE_TYPE_ORIGINAL);
							String mimeType = person.getMimeType();
							if (TextUtils.isEmpty(mimeType)) {	//默认为图片类型
								mimeType = Constants.MIME_IMAGE;
							}
							
							fileArray = new File[2];
							AttachDto attachDto = new AttachDto();
							attachDto.setSender(username);
							attachDto.setFileName(iconName);
							attachDto.setHash(iconHash);
							attachDto.setMimeType(mimeType);
							
							person.setMimeType(mimeType);
							
							if (!isIconExists && isIconThumbExists) {	//缩略图文件存在，原始头像不存在，则只上传缩略图文件
								fileArray[1] = new File(thumbIconPath);
							} else if (isIconExists && !isIconThumbExists) {	//原始图片存在，但缩略图不存在，则需生存缩略图
								//第一个文件为原始图像
								fileArray[0] = new File(iconPath);
								
								thumbIconPath = SystemUtil.generateIconPath(username, Constants.FILE_TYPE_THUMB);
								
								//第二个文件为缩略图
								boolean success = ImageUtil.generateThumbImage(iconPath, thumbIconPath);
								if (success) {
									//设置缩略图地址
									person.setThumbPath(thumbIconPath);
									fileArray[1] = new File(thumbIconPath);
								}
							} else {	//原始图片和缩略图都存在
								//第一个文件为原始图像
								fileArray[0] = new File(iconPath);
								fileArray[1] = new File(thumbIconPath);
							}
							Log.d("--addPersonalInfo---iconPath---" + iconPath + "--isIconExists--" + isIconExists + "-----thumbIconPath---" + thumbIconPath + "----isIconThumbExists---" + isIconThumbExists);
							String attachStr = gson.toJson(attachDto);
							if (!TextUtils.isEmpty(attachStr)) {
								params.put("attachStr", attachStr);
							}
							
							
						} catch (Exception e) {
							Log.e(e.getMessage());
						}
					}
					
					//上传个人信息到web服务器，若有头像，则上传，没有则不上传
					final ChatApplication app = ChatApplication.getInstance();
					uploadAvatar(app, builder.toString(), person, fileArray, params, "addPersonalInfo", new Response.Listener<String>() {

						@Override
						public void onResponse(String response) {
							Log.d("------addPersonalInfo---response--" + response);
							if (response != null) {
								ActionResult<Void> result = gson.fromJson(response, new TypeToken<ActionResult<Void>>() {}.getType());
								if (result != null) {
									int resultCode = result.getResultCode();
									switch (resultCode) {
									case ActionResult.CODE_SUCCESS:	//成功
										PersonalManage personalManage = PersonalManage.getInstance();
										personalManage.saveOrUpdateCurrentUser(person);
										app.setCurrentUser(person);
										
										break;
									case ActionResult.CODE_ERROR:	//请求处理错误
										Log.e("-----addPersonalInfo------" + person + "---CODE_ERROR---" + resultCode);
										break;
									case ActionResult.CODE_ERROR_PARAM:	//错误的参数
										Log.e("-----addPersonalInfo------" + person + "---CODE_ERROR_PARAM---" + resultCode);
										break;
									default:
										break;
									}
								}
							}
						}
					}, new Response.ErrorListener() {

						@Override
						public void onErrorResponse(VolleyError error) {
							Log.e("-----addPersonalInfo----onErrorResponse-----" + error.getMessage());
						}
					});
					/*StringRequest jsonRequest = new StringRequest(Method.POST, builder.toString(), new Response.Listener<String>() {

						@Override
						public void onResponse(String response) {
							Log.d("------addPersonalInfo---response--" + response);
							if (response != null) {
								ActionResult<Void> result = gson.fromJson(response, new TypeToken<ActionResult<Void>>() {}.getType());
								if (result != null) {
									int resultCode = result.getResultCode();
									Log.d("----addPersonalInfo----resultCode--" + resultCode);
									switch (resultCode) {
									case ActionResult.CODE_SUCCESS:	//成功
										ChatApplication app = (ChatApplication) mContext.getApplicationContext();
										PersonalManage personalManage = PersonalManage.getInstance();
										personalManage.saveOrUpdateCurrentUser(person);
										app.setCurrentUser(person);
										
										//上传头像
										break;
									case ActionResult.CODE_ERROR:	//请求处理错误
										Log.e("-----addPersonalInfo------" + person + "---CODE_ERROR---" + resultCode);
										break;
									case ActionResult.CODE_ERROR_PARAM:	//错误的参数
										Log.e("-----addPersonalInfo------" + person + "---CODE_ERROR_PARAM---" + resultCode);
										break;
									default:
										break;
									}
								}
							}
						}
					}, new Response.ErrorListener() {

						@Override
						public void onErrorResponse(VolleyError error) {
							Log.e(error.getMessage());
						}
					}) {
						@Override
						protected Map<String, String> getParams() throws AuthFailureError {
							Map<String, String> params = new HashMap<>();
							params.put("jsonStr", jsonStr);
							return params;
						}
					};*/
				}
			} catch (Exception e) {
				Log.e(e.getMessage(), e);
			}
		}
	}
	
	/**
	 * 上传头像
	 * @param app
	 * @param person 用户实体
	 * @param fileArray 文件数组
	 * @param tag 网络请求的唯一标识
	 * @param listener 网络请求的监听
	 * @param errorListener 网络请求失败的监听
	 * @update 2015年8月8日 下午4:16:17
	 */
	public void uploadAvatar(ChatApplication app, Personal person, File[] fileArray, String tag, Response.Listener<String> listener, Response.ErrorListener errorListener) {
		uploadAvatar(app, person, fileArray, null, tag, listener, errorListener);
	}
	
	/**
	 * 上传头像
	 * @param app
	 * @param url 上传地址， 可为空，默认的是上传头像的地址
	 * @param person 用户实体
	 * @param fileArray 文件数组
	 * @param params 提交的额外的参数
	 * @param tag 网络请求的唯一标识
	 * @param listener 网络请求的监听
	 * @param errorListener 网络请求失败的监听
	 * @update 2015年8月8日 下午4:16:17
	 */
	public void uploadAvatar(ChatApplication app, String url, Personal person, File[] fileArray, Map<String, String> params, String tag, Response.Listener<String> listener, Response.ErrorListener errorListener) {
		if (tag == null) {
			tag = person.getUsername();
		}
		if (url == null) {
			Uri uri = Uri.parse(Constants.BASE_API_URL);
			Uri.Builder uploadBuilder = uri.buildUpon();
			uploadBuilder.appendPath("user")
				.appendPath("uploadAvatar");
			url = uploadBuilder.toString();
		}
		MultiPartStringRequest multiPartRequest = initMultiPartRequest(url, person, fileArray, params, tag, listener, errorListener);
		app.addToRequestQueue(multiPartRequest, tag);
	}
	
	/**
	 * 上传头像
	 * @param app
	 * @param person 用户实体
	 * @param fileArray 文件数组
	 * @param params 提交的额外的参数
	 * @param tag 网络请求的唯一标识
	 * @param listener 网络请求的监听
	 * @param errorListener 网络请求失败的监听
	 * @update 2015年8月8日 下午4:16:17
	 */
	public void uploadAvatar(ChatApplication app, Personal person, File[] fileArray, Map<String, String> params, String tag, Response.Listener<String> listener, Response.ErrorListener errorListener) {
		uploadAvatar(app, null, person, fileArray, params, tag, listener, errorListener);
	}
	
	/**
	 * 上传头像
	 * @param app
	 * @param url 上传的地址
	 * @param person 用户实体
	 * @param fileArray 文件数组
	 * @param listener 网络请求的监听
	 * @param errorListener 网络请求失败的监听
	 * @update 2015年8月8日 下午4:16:17
	 */
	public void uploadAvatar(ChatApplication app, Personal person, File[] fileArray, Response.Listener<String> listener, Response.ErrorListener errorListener) {
		uploadAvatar(app, person, fileArray, null, listener, errorListener);
	}
	
	
	/**
	 * 上传头像
	 * @param app
	 * @param url 上传的地址
	 * @param person 用户实体
	 * @param fileArray 文件数组
	 * @update 2015年8月8日 下午4:16:17
	 */
	public void uploadAvatar(ChatApplication app, Personal person, File[] fileArray) {
		uploadAvatar(app, person, fileArray, null);
	}
	
	/**
	 * 上传头像
	 * @param app
	 * @param url 上传的地址
	 * @param person 用户实体
	 * @param fileArray 文件数组
	 * @update 2015年8月8日 下午4:16:17
	 */
	public void uploadAvatar(ChatApplication app, Personal person, File[] fileArray, String tag) {
		uploadAvatar(app, person, fileArray, null, null);
	}
	
}
