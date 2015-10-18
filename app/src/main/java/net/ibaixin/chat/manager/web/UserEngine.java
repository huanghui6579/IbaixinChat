package net.ibaixin.chat.manager.web;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.AuthFailureError;
import com.android.volley.Request.Method;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.nostra13.universalimageloader.core.download.ImageDownloader.Scheme;
import com.nostra13.universalimageloader.utils.MemoryCacheUtils;

import android.content.Context;
import android.net.Uri;
import android.net.Uri.Builder;
import android.text.TextUtils;
import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.download.DownloadManager;
import net.ibaixin.chat.download.DownloadRequest;
import net.ibaixin.chat.download.SimpleDownloadListener;
import net.ibaixin.chat.manager.UserManager;
import net.ibaixin.chat.model.ActionResult;
import net.ibaixin.chat.model.Personal;
import net.ibaixin.chat.model.User;
import net.ibaixin.chat.model.UserVcard;
import net.ibaixin.chat.model.web.VcardDto;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.ImageUtil;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.SystemUtil;

/**
 * 好友信息与web服务器的接口层
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年7月30日 下午4:27:12
 */
public class UserEngine {
	private Context mContext;

	public UserEngine(Context mContext) {
		super();
		this.mContext = mContext;
	}
	
	/**
	 * 根据用户名的字符串获取对应的好友电子名片简单信息，主要包括头像hash,昵称
	 * @param ids 用户名的字符串，多个用户名用","分割
	 * @param map 用户名为key，uservcard为值的map
	 * @update 2015年7月30日 下午4:55:11
	 */
	public void getSimpleVcardInfos(final String ids, final Map<String, UserVcard> map) {
		if (!TextUtils.isEmpty(ids)) {
			Log.d("----------getSimpleVcardInfo-----ids----" + ids);
			final Uri uri = Uri.parse(Constants.BASE_API_URL);
			Builder builder = uri.buildUpon();
			builder.appendPath("user")
				.appendPath("vcard")
				.appendPath("userSimpleVcards");
			StringRequest stringRequest = new StringRequest(Method.POST, builder.toString(), new Response.Listener<String>() {

				@Override
				public void onResponse(String response) {
					if (!TextUtils.isEmpty(response)) {
						Gson gson = new Gson();
						ActionResult<List<VcardDto>> result = gson.fromJson(response, new TypeToken<ActionResult<List<VcardDto>>>() {}.getType());
						if (result != null) {
							int resultCode = result.getResultCode();
							Log.w("----getSimpleVcardInfo-----获取所有好友电子名片信息列表-----resultCode----" + resultCode);
							switch (resultCode) {
							case ActionResult.CODE_SUCCESS:	//数据处理成功
								List<VcardDto> vcardList = result.getData();
								if (SystemUtil.isNotEmpty(vcardList)) {
									//需要更新头像的用户
									Map<String, UserVcard> downloadMap = UserManager.getInstance().updateUserVcardList(vcardList, map);
									if (!SystemUtil.isEmpty(downloadMap)) {	//有需要更新头像的用户
										downloadAvatar(downloadMap, Constants.FILE_TYPE_THUMB, null);
									}
								} else {
									Log.w("----getSimpleVcardInfo--获取所有好友电子名片信息列表----vcardList---为空----" + vcardList);
								}
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
					Log.e("----getSimpleVcardInfo----onErrorResponse---" + error.getMessage());
				}
			}) {
				@Override
				protected Map<String, String> getParams() throws AuthFailureError {
					Map<String, String> params = new HashMap<>();
					params.put("usernameStr", ids);
					return params;
				}
			};
			ChatApplication app = (ChatApplication) mContext.getApplicationContext();
			app.addToRequestQueue(stringRequest, "getSimpleVcardInfo");
		}
	}
	
	/**
	 * 根据用户名的字符串获取对应的好友电子名片简单信息，主要包括头像hash,昵称<br/>
	 * 注：此方法网络请求是同步的
	 * @param username 用户名
	 * @param map 
	 * @update 2015年9月28日 下午4:04:55
	 */
	public VcardDto getSimpleVcardInfoSync(final String username) {
		if (!TextUtils.isEmpty(username)) {
			final Uri uri = Uri.parse(Constants.BASE_API_URL);
			Builder builder = uri.buildUpon();
			builder.appendPath("user")	//user/vcard/userSimpleVcard/{username}
				.appendPath("vcard")
				.appendPath("userSimpleVcard")
				.appendPath(username);
			RequestFuture<String> future = RequestFuture.newFuture();
			StringRequest stringRequest = new StringRequest(builder.toString(), future, future);
			ChatApplication app = (ChatApplication) mContext.getApplicationContext();
			app.addToRequestQueue(stringRequest, "getSimpleVcardInfoSync");
			
			try {
				String response = future.get();
				if (!TextUtils.isEmpty(response)) {
					Gson gson = new Gson();
					VcardDto vcardDto = null;
					ActionResult<VcardDto> result = gson.fromJson(response, new TypeToken<ActionResult<VcardDto>>() {}.getType());
					if (result != null) {
						Log.d("----getSimpleVcardInfoSync--result--" + result);
						if (ActionResult.CODE_SUCCESS == result.getResultCode()) {
							vcardDto = result.getData();
						}
					}
					return vcardDto;
				}
			} catch (Exception e) {
				Log.e(e.getMessage());
			}
		}
		return null;
	}
	
	/**
	 * 获取指定用户的电子名片信息
	 * @param username 用户名
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年10月2日 下午5:45:17
	 */
	public void getVcardInfo(final String username, final VcardResponseListenter responseListenter) {
		if (!TextUtils.isEmpty(username)) {
			final Uri uri = Uri.parse(Constants.BASE_API_URL);
			final Builder builder = uri.buildUpon();
			builder.appendPath("user")	///user/vcard/{username}
				.appendPath("vcard")
				.appendPath(username);
			StringRequest stringRequest = new StringRequest(builder.toString(), new Response.Listener<String>() {

				@Override
				public void onResponse(String response) {
					if (!TextUtils.isEmpty(response)) {
						Gson gson = new Gson();
						try {
							ActionResult<VcardDto> result = gson.fromJson(response, new TypeToken<ActionResult<VcardDto>>() {}.getType());
							if (result != null) {
								Log.d("----getVcardInfo--username--" + username + "--result--" + result);
								if (ActionResult.CODE_SUCCESS == result.getResultCode()) {
									VcardDto vcardDto = result.getData();
									if (vcardDto != null) {
										String iconHash = vcardDto.getHash();
										UserManager userManager = UserManager.getInstance();
										User user = userManager.getUserDetailByUsername(username);
										if (user != null) {	//本地有该好友
											UserVcard vcard = user.getUserVcard();
											user.setPhone(vcardDto.getMobilePhone());
											if (vcard == null) {
												vcard = new UserVcard();
											}
											String oldHash = vcard.getIconHash();
											//更新电子名片的信息，如果之前没有就创建
											vcard = vcardDto2Vcard(vcardDto, vcard);
											vcard.setUserId(user.getId());
											
											boolean notifyUser = false;
											if (!user.hasMarkname()) {	//没有为好友设置备注名称，则比较电子名片的昵称
												String name = user.getName();
												String vcardName = vcard.getNickname();
												if (!name.equals(vcardName)) {	//名称不一样，需要刷新好友排序界面
													user.setFullPinyin(user.initFullPinyin());
													user.setShortPinyin(user.initShortPinyin());
													user.setSortLetter(user.initSortLetter(user.getShortPinyin()));
													notifyUser = true;
												}
												
											}
											boolean needDownload = false;
											//与服务器上的头像hash对比
											if (!TextUtils.isEmpty(iconHash)) {	//服务器上有头像
												if (!iconHash.equals(oldHash)) {	//本地头像与服务器上不同，则需要下载头像
													needDownload = true;
												}
											}
											if (needDownload) {	//下载头像
												DownloadManager downloadManager = new DownloadManager();
												Uri baseUri = Uri.parse(Constants.BASE_API_URL);
												final Builder downloadBuilder = baseUri.buildUpon();
												builder.appendPath("user")
													.appendPath("avatar")
													.appendPath(username);
												downloadAvatar(user, Constants.FILE_TYPE_THUMB, new VcardDownloadListener(true, user, Constants.FILE_TYPE_THUMB, downloadManager, downloadBuilder, notifyUser));
											} else {	//更新电子名片信息
												userManager.updateFriend(user, notifyUser);
											}
										} else {	//本地好友为空，则只返回查询到的数据
											if (responseListenter != null) {
												responseListenter.onVcardResponseSuccess(vcardDto);
											}
										}
									} else {
										Log.d("----getVcardInfo--username--" + username + "-----vcardDto--is null---" + vcardDto);
									}
								}
							}
						} catch (JsonSyntaxException e) {
							Log.e("------getVcardInfo---JsonSyntaxException----" + e.getMessage());
						}
					}
				}
			}, new Response.ErrorListener() {

				@Override
				public void onErrorResponse(VolleyError error) {
					Log.e("------getVcardInfo---onErrorResponse----" + error.getMessage());
				}
			});
			ChatApplication app = (ChatApplication) mContext.getApplicationContext();
			app.addToRequestQueue(stringRequest, "getVcardInfo");
			
		}
	}
	
	/**
	 * 获取用户的头像hash
	 * @param username
	 * @return 头像的信息
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年9月27日 上午11:01:05
	 */
	public VcardDto getIconHash(String username) {
		if (!TextUtils.isEmpty(username)) {
			final Uri uri = Uri.parse(Constants.BASE_API_URL);
			Builder builder = uri.buildUpon();
			builder.appendPath("user")	//vcard/icon/{username}
				.appendPath("vcard")
				.appendPath("icon")
				.appendPath(username);
			RequestFuture<String> future = RequestFuture.newFuture();
			StringRequest stringRequest = new StringRequest(builder.toString(), future, future);
			ChatApplication app = (ChatApplication) mContext.getApplicationContext();
			app.addToRequestQueue(stringRequest, "getIconHash");
			
			try {
				String response = future.get();
				if (!TextUtils.isEmpty(response)) {
					Gson gson = new Gson();
					VcardDto vcardDto = gson.fromJson(response, VcardDto.class);
					return vcardDto;
				}
			} catch (Exception e) {
				Log.e(e.getMessage());
			}
		}
		return null;
	}
	
	
	/**
	 * 更新自己的昵称
	 * @param personal
	 * @update 2015年8月25日 下午5:34:29
	 */
	public void updateNickname(Personal personal) {
		if (personal != null) {
			final String username = personal.getUsername();
			if (username != null) {
				final Uri uri = Uri.parse(Constants.BASE_API_URL);
				Builder builder = uri.buildUpon();
				builder.appendPath("user")
					.appendPath("modify")
					.appendPath("nick")//modify/nick/{username}
					.appendPath(username);
				final String nickname = personal.getNickname();
				StringRequest stringRequest = new StringRequest(Method.POST, builder.toString(), new Response.Listener<String>() {

					@Override
					public void onResponse(String response) {
						Log.d("---updateNickname----response-----" + response);
					}
					
				}, new Response.ErrorListener() {

					@Override
					public void onErrorResponse(VolleyError error) {
						Log.e("---updateNickname----error-----" + error.getMessage());
					}
				}) {
					@Override
					protected Map<String, String> getParams() throws AuthFailureError {
						Map<String, String> params = new HashMap<>(1);
						params.put("nickname", nickname);
						return params;
					}
				};
				ChatApplication app = (ChatApplication) mContext.getApplicationContext();
				app.addToRequestQueue(stringRequest, "updateNickname");
			}
		}
	}
	
	/**
	 * 更新自己的个性签名
	 * @param personal
	 * @update 2015年8月25日 下午5:34:29
	 */
	public void updateSignature(Personal personal) {
		if (personal != null) {
			final String username = personal.getUsername();
			if (username != null) {
				final Uri uri = Uri.parse(Constants.BASE_API_URL);
				Builder builder = uri.buildUpon();
				builder.appendPath("user")
					.appendPath("modify")
					.appendPath("signature")///modify/signature/{username}
					.appendPath(username);
				final String signature = personal.getDesc();
				StringRequest stringRequest = new StringRequest(Method.POST, builder.toString(), new Response.Listener<String>() {
					
					@Override
					public void onResponse(String response) {
						Log.d("---updateSignature----response-----" + response);
					}
					
				}, new Response.ErrorListener() {
					
					@Override
					public void onErrorResponse(VolleyError error) {
						Log.e("---updateSignature----error-----" + error.getMessage());
					}
				}) {
					@Override
					protected Map<String, String> getParams() throws AuthFailureError {
						Map<String, String> params = new HashMap<>(1);
						params.put("signature", signature);
						return params;
					}
				};
				ChatApplication app = (ChatApplication) mContext.getApplicationContext();
				app.addToRequestQueue(stringRequest, "updateSignature");
			}
		}
	}
	
	/**
	 * 更新自己的昵称
	 * @param personal
	 * @update 2015年8月25日 下午5:34:29
	 */
	public void updateGender(Personal personal) {
		if (personal != null) {
			final String username = personal.getUsername();
			if (username != null) {
				final Uri uri = Uri.parse(Constants.BASE_API_URL);
				Builder builder = uri.buildUpon();
				builder.appendPath("user")
					.appendPath("modify")
					.appendPath("gender")//modify/gender/{username}
					.appendPath(username);
				final int gender = personal.getSex();
				StringRequest stringRequest = new StringRequest(Method.POST, builder.toString(), new Response.Listener<String>() {
					
					@Override
					public void onResponse(String response) {
						Log.d("---updateGender----response-----" + response);
					}
					
				}, new Response.ErrorListener() {
					
					@Override
					public void onErrorResponse(VolleyError error) {
						Log.e("---updateGender----error-----" + error.getMessage());
					}
				}) {
					@Override
					protected Map<String, String> getParams() throws AuthFailureError {
						Map<String, String> params = new HashMap<>(1);
						params.put("gender", String.valueOf(gender));
						return params;
					}
				};
				ChatApplication app = (ChatApplication) mContext.getApplicationContext();
				app.addToRequestQueue(stringRequest, "updateGender");
			}
		}
	}
	
	/**
	 * 更新自己的地理位置
	 * @param personal
	 * @update 2015年8月25日 下午5:34:29
	 */
	public void updateGeo(Personal personal) {
		if (personal != null) {
			final String username = personal.getUsername();
			if (username != null) {
				final Uri uri = Uri.parse(Constants.BASE_API_URL);
				Builder builder = uri.buildUpon();
				builder.appendPath("user")
					.appendPath("modify")
					.appendPath("address")//modify/address/{username}
					.appendPath(username);
				
				JSONObject jsonRequest = new JSONObject();
				try {
					jsonRequest.put("country", personal.getCountry());
					jsonRequest.put("countryId", personal.getCountryId());
					jsonRequest.put("province", personal.getProvince());
					jsonRequest.put("provinceId", personal.getProvinceId());
					jsonRequest.put("city", personal.getCity());
					jsonRequest.put("cityId", personal.getCityId());
				} catch (JSONException e) {
					Log.e(e.getMessage());
				}
				JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(builder.toString(), jsonRequest, new Response.Listener<JSONObject>() {

					@Override
					public void onResponse(JSONObject response) {
						Log.d("---updateGeo----response-----" + response);
					}
				}, new Response.ErrorListener() {

					@Override
					public void onErrorResponse(VolleyError error) {
						Log.e("---updateGeo----error-----" + error.getMessage());
					}
				});
				ChatApplication app = (ChatApplication) mContext.getApplicationContext();
				app.addToRequestQueue(jsonObjectRequest, "updateGender");
			}
		}
	}
	
	/**
	 * 根据用户名的集合来下载对应用户的头像
	 * @param usernames username为key，UserVcard为value的map
	 * @param downloadType 要下载头像的类型，downloadType：1，表示缩略图；2：表示原始图片
	 * @update 2015年8月1日 上午11:30:11
	 */
	public void downloadAvatar(final Map<String, UserVcard> downloadMap, final int downloadType, SimpleDownloadListener downloadListener) {
		final DownloadManager downloadManager = new DownloadManager();
		Uri baseUri = Uri.parse(Constants.BASE_API_URL);
		String iconRootPath = SystemUtil.getDefaultIconPath(downloadType);
		Log.d("----downloadAvatar--downloadMap--" + downloadMap + "----downloadType---" + downloadType);
		Set<String> keys = downloadMap.keySet();
		for (final String username : keys) {
			if (!TextUtils.isEmpty(username)) {
				String iconName = SystemUtil.generateIconName(username, downloadType);
				String iconPath = iconRootPath + File.separator + iconName;
				final Builder builder = baseUri.buildUpon();
				builder.appendPath("user")
					.appendPath("avatar")
					.appendPath(username);

				SimpleDownloadListener listener = null;
				if (downloadListener == null) {
					listener = new AvatarSimpleDownloadListener(true, downloadMap.get(username), username, downloadType, downloadManager, builder);
				} else {
					listener = downloadListener;
				}
				builder.appendQueryParameter("fileType", String.valueOf(downloadType));
				DownloadRequest downloadRequest = new DownloadRequest();
				downloadRequest.setUrl(builder.toString())
					.setDestFilePath(iconPath)
					.setDownloadId(iconPath.hashCode())
					.setSimpleDownloadListener(listener);
				downloadManager.add(downloadRequest);
			}
		}
		
	}
	
	/**
	 * 基本的头像下载监听器
	 * @author huanghui1
	 * @version 1.0.0
	 * @update 2015年8月17日 下午9:29:43
	 */
	private class AvatarSimpleDownloadListener implements SimpleDownloadListener {
		//是否在处理下载失败的逻辑
		private boolean handleOnFailed = false;
		
		private UserVcard vcard;
		
		private String username;
		
		private int downloadType;
		
		private DownloadManager downloadManager;
		
		private Builder builder;

		public AvatarSimpleDownloadListener(boolean handleOnFailed, UserVcard vcard, String username,
				int downloadType, DownloadManager downloadManager, Builder builder) {
			this.handleOnFailed = handleOnFailed;
			this.vcard = vcard;
			this.username = username;
			this.downloadType = downloadType;
			this.downloadManager = downloadManager;
			this.builder = builder;
		}

		@Override
		public void onSuccess(int downloadId, String filePath) {
			Log.d("-----downloadAvatar-----onSuccess----downloadType ---- " + downloadType + "---downloadId---" + downloadId + "----filePath--" + filePath);
			//头像下载成功，将相关信息存入数据，刷新界面
			UserManager userManager = UserManager.getInstance();
			if (vcard != null) {	//电子名片信息不为空，则更新
				User user = new User();
				user.setId(vcard.getUserId());
				//清除该图像的内存缓存
				ImageUtil.clearMemoryCache(filePath);
				if (downloadType == Constants.FILE_TYPE_THUMB) {	//下载的是缩略图，则更新缩略图信息
					vcard.setThumbPath(filePath);
					userManager.updateUserVcardThumbIcon(user, vcard);
				} else if (downloadType == Constants.FILE_TYPE_ORIGINAL) {	//下载的是原始图片，则更新原始图片信息
					vcard.setIconPath(filePath);
					userManager.updateUserVcardOriginalIcon(user, vcard);
				}
			}
//			userManager.updateSimpleUser(user)
		}

		@Override
		public void onFailure(int downloadId, int statusCode, String errMsg) {
			if (handleOnFailed) {
				Log.w("-----downloadAvatar-----onFailure----downloadType + " + downloadType + "-----downloadId---" + downloadId + "----statusCode--" + statusCode + "---errMsg--" + errMsg + "------begin download---secondDownloadType---" + downloadType);
				
				if (builder != null && downloadManager != null) {
					
					//备用下载类型，当第一种下载类型失败后，则下载该备用类型，如：该用户没有缩略图像，则下载原始头像
					int secondDownloadType = 0;
					
					if (downloadType == Constants.FILE_TYPE_THUMB) {
						secondDownloadType = Constants.FILE_TYPE_ORIGINAL;
					} else if (downloadType == Constants.FILE_TYPE_ORIGINAL) {
						secondDownloadType = Constants.FILE_TYPE_THUMB;
					}
					
					builder.appendQueryParameter("fileType", String.valueOf(secondDownloadType));
					
					String secondPath = SystemUtil.getDefaultIconPath(secondDownloadType);
					String secondIconName = SystemUtil.generateIconName(username, secondDownloadType);
					String secondIconPath = secondPath + File.separator +  secondIconName;
					
					DownloadRequest downloadRequest = new DownloadRequest();
					downloadRequest.setUrl(builder.toString())
						.setDestFilePath(secondIconPath)
						.setDownloadId(secondIconPath.hashCode())
						.setSimpleDownloadListener(new AvatarSimpleDownloadListener(false, vcard, username, secondDownloadType, downloadManager, builder));
					downloadManager.add(downloadRequest);
				} else {
					Log.d("-----downloadAvatar-----onFailure----builder is null--" + builder + "or downloadManager is null---" + downloadManager + "---downloadType + " + downloadType);
				}
			} else {
				Log.d("-----downloadAvatar----secondDownload---onFailure----downloadType + " + downloadType + "-----downloadId---" + downloadId + "----statusCode--" + statusCode + "---errMsg--" + errMsg);
			}
		}
		
	}
	
	/**
	 * 更新完整的电子名片信息时头像的下载监听器
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年10月3日 下午2:31:35
	 */
	private class VcardDownloadListener implements SimpleDownloadListener {
		//是否在处理下载失败的逻辑
		private boolean handleOnFailed = false;
		
		private User user;
		
		private int downloadType;
		
		private DownloadManager downloadManager;
		
		private Builder builder;
		
		/**
		 * 若更新user，则是否刷新界面，如果更新前后的名称相同，则不需刷新界面
		 */
		private boolean notifyUser;

		public VcardDownloadListener(boolean handleOnFailed, User user, int downloadType,
				DownloadManager downloadManager, Builder builder, boolean notifyUser) {
			super();
			this.handleOnFailed = handleOnFailed;
			this.user = user;
			this.downloadType = downloadType;
			this.downloadManager = downloadManager;
			this.builder = builder;
			this.notifyUser = notifyUser;
		}

		@Override
		public void onSuccess(int downloadId, String filePath) {
			Log.d("-----downloadAvatar----VcardDownloadListener-----onSuccess----downloadType ---- " + downloadType + "---downloadId---" + downloadId + "----filePath--" + filePath);
			//头像下载成功，将相关信息存入数据，刷新界面
			UserManager userManager = UserManager.getInstance();
			UserVcard vcard = user.getUserVcard();
			if (downloadType == Constants.FILE_TYPE_THUMB) {	//下载的是缩略图，则更新缩略图信息
				vcard.setThumbPath(filePath);
			} else if (downloadType == Constants.FILE_TYPE_ORIGINAL) {	//下载的是原始图片，则更新原始图片信息
				vcard.setIconPath(filePath);
			}
			//清除该图像的内存缓存
			ImageUtil.clearMemoryCache(filePath);
			userManager.updateFriend(user, notifyUser);
		}

		@Override
		public void onFailure(int downloadId, int statusCode, String errMsg) {
			if (handleOnFailed) {
				Log.w("-----downloadAvatar----VcardDownloadListener---onFailure----downloadType + " + downloadType + "-----downloadId---" + downloadId + "----statusCode--" + statusCode + "---errMsg--" + errMsg + "------begin download---secondDownloadType---" + downloadType);
				
				if (builder != null && downloadManager != null) {
					
					//备用下载类型，当第一种下载类型失败后，则下载该备用类型，如：该用户没有缩略图像，则下载原始头像
					int secondDownloadType = 0;
					
					if (downloadType == Constants.FILE_TYPE_THUMB) {
						secondDownloadType = Constants.FILE_TYPE_ORIGINAL;
					} else if (downloadType == Constants.FILE_TYPE_ORIGINAL) {
						secondDownloadType = Constants.FILE_TYPE_THUMB;
					}
					
					builder.appendQueryParameter("fileType", String.valueOf(secondDownloadType));
					String username = user.getUsername();
					String secondPath = SystemUtil.getDefaultIconPath(secondDownloadType);
					String secondIconName = SystemUtil.generateIconName(username, secondDownloadType);
					String secondIconPath = secondPath + File.separator +  secondIconName;
					
					DownloadRequest downloadRequest = new DownloadRequest();
					downloadRequest.setUrl(builder.toString())
						.setDestFilePath(secondIconPath)
						.setDownloadId(secondIconPath.hashCode())
						.setSimpleDownloadListener(new VcardDownloadListener(false, user, secondDownloadType, downloadManager, builder, notifyUser));
					downloadManager.add(downloadRequest);
				} else {
					Log.d("-----downloadAvatar---VcardDownloadListener-----onFailure----builder is null--" + builder + "or downloadManager is null---" + downloadManager + "---downloadType + " + downloadType);
				}
			} else {
				Log.d("-----downloadAvatar----VcardDownloadListener---secondDownload---onFailure----downloadType + " + downloadType + "-----downloadId---" + downloadId + "----statusCode--" + statusCode + "---errMsg--" + errMsg);
			}
		}
		
	}
	
	/**
	 * 下载用户对应的头像
	 * @param user 用户
	 * @param downloadType 要下载头像的类型，downloadType：1，表示缩略图；2：表示原始图片
	 * @update 2015年8月12日 下午4:35:39
	 */
	public void downloadAvatar(final User user, final int downloadType, SimpleDownloadListener downloadListener) {
		if (user == null) {
			return;
		}
		Map<String, UserVcard> downloadMap = new HashMap<>(1);
		downloadMap.put(user.getUsername(), user.getUserVcard());
		downloadAvatar(downloadMap, downloadType, downloadListener);
	}
	
	/**
	 * 将vCardDto转换成vCard
	 * @param vcardDto
	 * @return
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年10月3日 下午2:10:30
	 */
	private UserVcard vcardDto2Vcard(VcardDto vcardDto, UserVcard vcard) {
		if (vcardDto != null) {
			vcard.setCity(vcardDto.getCity());
			vcard.setCountry(vcardDto.getCountry());
			vcard.setDesc(vcardDto.getSignature());
			vcard.setIconHash(vcardDto.getHash());
			vcard.setMimeType(vcardDto.getMimeType());
			vcard.setMobile(vcard.getMobile());
			vcard.setNickname(vcardDto.getNickName());
			vcard.setProvince(vcardDto.getProvince());
			vcard.setRealName(vcardDto.getRealName());
			vcard.setSex(vcardDto.getGender());
			vcard.setStreet(vcardDto.getStreet());
			return vcard;
		} else {
			return null;
		}
	}
	
	/**
	 * vCardDto信息返回的监听器
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年10月3日 下午4:05:48
	 */
	public interface VcardResponseListenter {
		/**
		 * vCardDto信息返回成功,<strong>注：次方法的执行在非UI线程中</strong>
		 * @param vcardDto 返回的vCardDto信息
		 * @author tiger
		 * @version 1.0.0
		 * @update 2015年10月3日 下午4:07:34
		 */
		public void onVcardResponseSuccess(VcardDto vcardDto);
	}
}
