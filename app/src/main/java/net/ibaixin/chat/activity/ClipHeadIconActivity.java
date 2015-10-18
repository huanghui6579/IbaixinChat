package net.ibaixin.chat.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.theartofdev.edmodo.cropper.CropImageView;

import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.R;
import net.ibaixin.chat.manager.PersonalManage;
import net.ibaixin.chat.manager.web.PersonalEngine;
import net.ibaixin.chat.model.ActionResult;
import net.ibaixin.chat.model.Personal;
import net.ibaixin.chat.model.web.AttachDto;
import net.ibaixin.chat.smack.packet.VcardX;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.ImageUtil;
import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.MimeUtils;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.util.XmppConnectionManager;
import net.ibaixin.chat.util.XmppUtil;
import net.ibaixin.chat.view.ProgressDialog;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;

import java.io.File;
import java.io.IOException;

/**
 * 裁剪头像的界面
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年3月13日 下午2:03:37
 */
public class ClipHeadIconActivity extends BaseActivity {
	
	public static final String ARG_IMAGE_PATH = "arg_image_path";
	
	public static final String ARG_DATA = "arg_data";
	
//	private ClipImageLayout iconLayout;
	
	private CropImageView mCropImageView;
	
//	private ProgressWheel pbLoading;
	
	/**
	 * 原始图片的路径
	 */
	private String imagePath;
	
	private MenuItem mMenuDone;
//	private TextView btnOpt;
	
	private ProgressDialog pDialog;
	
//	private ImageLoader mImageLoader = ImageLoader.getInstance();
//	private DisplayImageOptions options = SystemUtil.getPhotoPreviewOptions();
	
	private PersonalManage personalManage;
	
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Constants.MSG_SUCCESS:
				Intent intent = new Intent();
				intent.putExtra(ARG_DATA, (String) msg.obj);
				setResult(RESULT_OK, intent);
				finish();
				break;
			case Constants.MSG_FAILED:
				SystemUtil.makeShortToast(R.string.opt_failed);
				break;

			default:
				break;
			}
		}
	};

	@Override
	protected int getContentView() {
		return R.layout.activity_clip_head_icon;
	}

	@Override
	protected void initView() {
//		iconLayout = (ClipImageLayout) findViewById(R.id.iv_icon);
		mCropImageView = (CropImageView) findViewById(R.id.iv_icon);
//		pbLoading = (ProgressWheel) findViewById(R.id.pb_loading);
	}

	@Override
	protected void initData() {
		Intent intent = getIntent();
		imagePath = intent.getStringExtra(ARG_IMAGE_PATH);
		if (SystemUtil.isFileExists(imagePath)) {	//图片存在
			mCropImageView.setImageUri(Uri.fromFile(new File(imagePath)));
			personalManage = PersonalManage.getInstance();
			/*mImageLoader.displayImage(, imageView, options, new ImageLoadingListener() {
				
				@Override
				public void onLoadingStarted(String imageUri, View view) {
					if (!SystemUtil.isViewVisible(pbLoading)) {
						pbLoading.setVisibility(View.VISIBLE);
					}
				}
				
				@Override
				public void onLoadingFailed(String imageUri, View view,
						FailReason failReason) {
					if (SystemUtil.isViewVisible(pbLoading)) {
						pbLoading.setVisibility(View.GONE);
					}
				}
				
				@Override
				public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
					if (SystemUtil.isViewVisible(pbLoading)) {
						pbLoading.setVisibility(View.GONE);
					}
				}
				
				@Override
				public void onLoadingCancelled(String imageUri, View view) {
					
				}
			});*/
		} else {
//			pbLoading.setVisibility(View.GONE);
			SystemUtil.makeShortToast(R.string.file_not_exists);
			finish();
		}
	}

	@Override
	protected void addListener() {
		// TODO Auto-generated method stub

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.menu_save, menu);
		mMenuDone = menu.findItem(R.id.action_select_complete);
		mMenuDone.setTitle(R.string.apply);
		mMenuDone.setEnabled(true);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_select_complete:
			final String username = ChatApplication.getInstance().getCurrentAccount();
			if (!TextUtils.isEmpty(username)) {
				pDialog = ProgressDialog.show(mContext, null, getString(R.string.loading), true);
				SystemUtil.getCachedThreadPool().execute(new Runnable() {
					
					@Override
					public void run() {
						final Message msg = mHandler.obtainMessage();
						try {
							final Personal personal = ChatApplication.getInstance().getCurrentUser();
							String filePath = personal.getIconPath();
							Bitmap bitmap = mCropImageView.getCroppedImage(Constants.IMAGE_ORIGINAL_SIZE, Constants.IMAGE_ORIGINAL_SIZE);
							//保存图片到本地存储
							File saveFile = null;
							if (TextUtils.isEmpty(filePath) || !new File(filePath).getParentFile().exists()) {	//之前没有头像，或者头像路径被删除，也就没有头像路径，则需重新生成
								saveFile = SystemUtil.generateIconFile(username, Constants.FILE_TYPE_ORIGINAL);
								filePath = saveFile.getAbsolutePath();
							} else {
								saveFile = new File(filePath);
							}
							//保存到数据库
							boolean success = ImageUtil.saveBitmap(bitmap, saveFile);
							if (success) {
								File[] fileArray = new File[2];
								fileArray[0] = saveFile;
								//根据原始图片的全路径获取图片的mime类型
								String ext = SystemUtil.getFileSubfix(imagePath);
								//文件hash
								final String hash = SystemUtil.getFileHash(saveFile);
								String mimeType = MimeUtils.guessMimeTypeFromExtension(ext);
								if (TextUtils.isEmpty(mimeType)) {	//如果不存在mime,就设置个默认的图片mime
									mimeType = Constants.MIME_IMAGE;
								}
								personal.setMimeType(mimeType);
								personal.setIconPath(filePath);
								personal.setIconHash(hash);
								if (bitmap.getWidth() <= Constants.IMAGE_THUMB_WIDTH && bitmap.getHeight() <= Constants.IMAGE_THUMB_HEIGHT) {	//原始头像本身就比较小了，此时，不需要再压缩了
									personal.setThumbPath(null);
								} else {
									//再生存缩略图
									String thumbPath = personal.getThumbPath();
									if (TextUtils.isEmpty(thumbPath) || !new File(thumbPath).getParentFile().exists()) {	//之前没有缩略图路径，或者头像路径被删除，则重新生成路径
										thumbPath = SystemUtil.generateIconPath(username, Constants.FILE_TYPE_THUMB);
									}
									success = ImageUtil.compressImage(filePath, thumbPath);
									
									if (success) {	//生成缩略图成功
										fileArray[1] = new File(thumbPath);
										personal.setThumbPath(thumbPath);
									} else {
										personal.setThumbPath(null);
									}
								}
								
								if (bitmap != null) {
									bitmap.recycle();
								}
								
								//上传头像，更新数据库
								PersonalEngine personalEngine = new PersonalEngine(mContext);
								personalEngine.uploadAvatar(application, personal, fileArray, null, new Response.Listener<String>() {

									@Override
									public void onResponse(String response) {
										if (!TextUtils.isEmpty(response)) {
											Gson gson = new Gson();
											try {
												ActionResult<AttachDto> result = gson.fromJson(response, new TypeToken<ActionResult<AttachDto>> () {}.getType());
												if (result != null) {
													int resultCode = result.getResultCode();
													switch (resultCode) {
													case ActionResult.CODE_SUCCESS:	//处理成功
														//发送消息，通知好友，改变了头像
														AbstractXMPPConnection connection = XmppConnectionManager.getInstance().getConnection();
														if (XmppUtil.checkAuthenticated(connection)) {
															AttachDto attachDto = result.getData();
															if (attachDto != null) {
																VcardX vcardX = new VcardX();
																vcardX.setMimeType(personal.getMimeType());
																vcardX.setIconHash(attachDto.getHash());
																try {
																	//发送xmpp的IQ消息通知好友
																	XmppUtil.updateAvatar(connection, vcardX);
																} catch (NoResponseException | XMPPErrorException
																		| NotConnectedException e) {
																	Log.e("---updateAvatar--failed--" + e.getMessage());
																}
															}
														}
														//清除头像的内存缓存
														//从缓存中删除该图像的内存缓存
														ImageUtil.clearMemoryCache(personal.getThumbPath());
														
														ImageUtil.clearMemoryCache(personal.getIconPath());
														
														//保存头像信息到本地数据库
														personalManage.updateHeadIcon(personal);
														
														msg.what = Constants.MSG_SUCCESS;
														msg.obj = personal.getIconPath();
														Log.d("------uploadAvatar---处理成功--resultCode----" + resultCode);
														break;
													case ActionResult.CODE_ERROR:	//服务器处理错误
														msg.what = Constants.MSG_FAILED;
														Log.w("------uploadAvatar---服务器处理错误--resultCode----" + resultCode);
														break;
													case ActionResult.CODE_ERROR_PARAM:	//本地参数错误
														msg.what = Constants.MSG_FAILED;
														Log.w("------uploadAvatar---本地参数错误--resultCode----" + resultCode);
														break;
													default:
														break;
													}
												}
											} catch (JsonSyntaxException e) {
												msg.what = Constants.MSG_FAILED;
												Log.e(e.getMessage());
											}
										} else {
											msg.what = Constants.MSG_FAILED;
											Log.e("------uploadAvatar---response---为空---" + response);
										}
										mHandler.sendMessage(msg);
										if (pDialog != null) {
											pDialog.dismiss();
										}
									}
								}, new Response.ErrorListener() {

									@Override
									public void onErrorResponse(VolleyError error) {
										msg.what = Constants.MSG_FAILED;
										mHandler.sendMessage(msg);
										if (pDialog != null) {
											pDialog.dismiss();
										}
										Log.e("------uploadAvatar---" + error.getMessage());
									}
								});
								
//								AbstractXMPPConnection connection = XmppConnectionManager.getInstance().getConnection();
								
								/*//通知好友更新头像
								if (XmppUtil.checkAuthenticated(connection)) {
									XmppUtil.updateAvatar(connection, filePath);
									//保存头像信息到本地数据库
									personalManage.updateHeadIcon(personal);
									
									msg.what = Constants.MSG_SUCCESS;
									msg.obj = filePath;
								} else {
									msg.what = Constants.MSG_FAILED;
								}*/
								
							} else {
								msg.what = Constants.MSG_FAILED;
								mHandler.sendMessage(msg);
								if (pDialog != null) {
									pDialog.dismiss();
								}
							}
						} catch (IOException e) {
							msg.what = Constants.MSG_FAILED;
							mHandler.sendMessage(msg);
							if (pDialog != null) {
								pDialog.dismiss();
							}
							Log.e(e.getMessage());
						}
					}
				});
			} else {
				SystemUtil.makeShortToast(R.string.not_login);
			}
			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

}
