package net.ibaixin.chat.activity;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.download.ImageDownloader.Scheme;
import com.nostra13.universalimageloader.utils.MemoryCacheUtils;

import net.ibaixin.chat.R;
import net.ibaixin.chat.fragment.JokeFragment;
import net.ibaixin.chat.fragment.PhotoFragment;
import net.ibaixin.chat.model.FormFile;
import net.ibaixin.chat.util.CameraUtil;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.HttpRequest;
import net.ibaixin.chat.util.ImageUtil;
import net.ibaixin.chat.util.SystemUtil;
import net.ibaixin.chat.view.EmojiconEditText;
import net.ibaixin.chat.view.ProgressDialog;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
/**
 * 趣味段子发布界面
 * @author dudejin
 */
public class JokeAddActivity extends BaseActivity {
//	private Button btnSend;
	private EmojiconEditText addjoke_content;
	private ImageView addjoke_pic;
	private ImageView addjoke_pic_preview;
	private RadioGroup addjoke_type ;
	private int joke_type = 0;//段子类型，1——笑话，2——趣图，3——感悟
	private ProgressDialog proDialog;
	
	private ImageLoader mImageLoader = ImageLoader.getInstance();
	private DisplayImageOptions options = SystemUtil.getGeneralImageOptions();
	
	@Override
	protected boolean isHomeAsUpEnabled() {
		return true;
	}
	
	@Override
	protected int getContentView() {
		return R.layout.activity_jokeadd;
	}

	@Override
	protected void initView() {
		addjoke_content = (EmojiconEditText) findViewById(R.id.addjoke_content);
		addjoke_pic = (ImageView) findViewById(R.id.addjoke_pic);
		addjoke_pic_preview = (ImageView) findViewById(R.id.addjoke_pic_preview);
		addjoke_type = (RadioGroup) findViewById(R.id.addjoke_type) ;
	}
	
	@Override
	protected void initData() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected void addListener() {
		addjoke_pic.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				CameraUtil.selectPictureOption(mContext);
			}
		});
		addjoke_pic_preview.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final String filepathName = SystemUtil.getPublicFilePath() + CameraUtil.PIC_CROP_NAME;
				File f = new File(filepathName);
				if (f.exists()) {// 如果本地不存在就去下载
					Intent intent = new Intent(mContext,ChatImagePreviewActivity.class);
					intent.putExtra(ChatImagePreviewActivity.ARG_IMAGE_PATH,filepathName);
					intent.putExtra(PhotoFragment.ARG_TOUCH_FINISH, true);
					ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(addjoke_pic_preview, 0, 0,addjoke_pic_preview.getWidth(), addjoke_pic_preview.getHeight());
					ActivityCompat.startActivity((Activity) mContext, intent,options.toBundle());
				} 
			}
		});
		addjoke_type.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if(checkedId == R.id.addjoke_type1){
					joke_type = 1 ;
				}
				if(checkedId == R.id.addjoke_type2){
					joke_type = 2 ;
				}
				if(checkedId == R.id.addjoke_type3){
					joke_type = 3 ;
				}
			}
		});
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.menu_jokeadd, menu);
		return super.onCreateOptionsMenu(menu);
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch (item.getItemId()) {
		case R.id.action_jokesend://发送
			if(joke_type == 0){
				SystemUtil.makeShortToast(R.string.error_type_null);
				return false ;
			}
			String content = addjoke_content.getText().toString().trim() ;
			if(!"".equals(content)){
				Map<String, String> map = new HashMap<String, String>();
				map.put("content", content);
				map.put("type", String.valueOf(joke_type));
				map.put("createUser", application.getCurrentAccount());
				if(addjoke_pic_preview.getVisibility() == View.VISIBLE){
					File file = new File(SystemUtil.getPublicFilePath() + CameraUtil.PIC_CROP_NAME);
					FormFile f = new FormFile(CameraUtil.PIC_NAME, file, "file", "image/*") ;
					new AddJokeTask().executeOnExecutor(SystemUtil.getCachedThreadPool(), map,f) ;
				}else{
					new AddJokeTask().executeOnExecutor(SystemUtil.getCachedThreadPool(), map ,"UTF-8") ;
				}
			}else{
				SystemUtil.makeShortToast(R.string.error_content_null);
			}
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
		protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case CameraUtil.TAKE_PICCODE:// 如果是手机拍照 就没有data 返回 就是在内存卡中
			File temp = new File(SystemUtil.getPublicFilePath(),CameraUtil.PIC_NAME);
			// 取消照片选择拍照的照片
			if (temp.exists()) {
				Bitmap bm = ImageUtil.compressImageBySavePath(SystemUtil.getPublicFilePath()+CameraUtil.PIC_NAME);
				try {
					ImageUtil.saveBitmap(bm, SystemUtil.getPublicFilePath()+CameraUtil.PIC_CROP_NAME) ;
					String iconUri = Scheme.FILE.wrap(SystemUtil.getPublicFilePath()+CameraUtil.PIC_CROP_NAME);
					//从缓存中删除该图像的内存缓存
					MemoryCacheUtils.removeFromCache(iconUri, mImageLoader.getMemoryCache());
					addjoke_pic_preview.setVisibility(View.VISIBLE);
					mImageLoader.displayImage(iconUri, addjoke_pic_preview, options);
				} catch (Exception e) {
					Log.e(TAG, e.toString());
				}
//				CameraUtil.startPhotoZoom(mContext, Uri.fromFile(temp));
			}
			break;
		case CameraUtil.SELECT_PICCODE:// 来自手机相册
			if(data==null)
				return ;
			Uri uri = data.getData();
			Bitmap bm = ImageUtil.getBitmapFromUri(mContext, uri);
			if(bm!=null){
				try {
					bm = ImageUtil.compressImageByBitmap(bm);
					ImageUtil.saveBitmap(bm, SystemUtil.getPublicFilePath()+CameraUtil.PIC_CROP_NAME) ;
					String iconUri = Scheme.FILE.wrap(SystemUtil.getPublicFilePath()+CameraUtil.PIC_CROP_NAME);
					//从缓存中删除该图像的内存缓存
					MemoryCacheUtils.removeFromCache(iconUri, mImageLoader.getMemoryCache());
					addjoke_pic_preview.setVisibility(View.VISIBLE);
					mImageLoader.displayImage(iconUri, addjoke_pic_preview, options);
				} catch (Exception e) {
					Log.e(TAG, e.toString());
				}
			}
//			CameraUtil.startPhotoZoom(mContext,uri);
			break;
		case CameraUtil.CROP_PICCODE:// 来自手裁剪后的图片
			if(data==null)
				return ;
			Bundle extras = data.getExtras();
			if (extras != null) {
				Bitmap photo = extras.getParcelable("data");
				File temp2 = new File(SystemUtil.getPublicFilePath() + CameraUtil.PIC_CROP_NAME);
				if (temp2.exists()) {
					temp2.delete();
				}
				try {
					ImageUtil.saveBitmap(photo, temp2);
					String iconUri = Scheme.FILE.wrap(SystemUtil.getPublicFilePath()+CameraUtil.PIC_CROP_NAME);
					//从缓存中删除该图像的内存缓存
					MemoryCacheUtils.removeFromCache(iconUri, mImageLoader.getMemoryCache());
					addjoke_pic_preview.setVisibility(View.VISIBLE);
					mImageLoader.displayImage(iconUri, addjoke_pic_preview, options);
				} catch (Exception e) {
					SystemUtil.makeShortToast(R.string.error_image_notget);
					Log.e(TAG, e.toString());
				}
			}
			break;

		default:
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	class AddJokeTask extends AsyncTask<Object, Boolean, Boolean>{
		@Override
		protected void onPreExecute() {
			if (proDialog == null) {
				proDialog = ProgressDialog.show(mContext, null, getString(R.string.submittingdata), true);
			} else {
				proDialog.show();
			}
		}
		
		@SuppressWarnings("unchecked")
		@Override
		protected Boolean doInBackground(Object... params) {
			Map<String,String> map = (Map<String,String>) params[0] ;
			Object obj = params[1] ;
			boolean flag = false ;
			if(obj instanceof FormFile){
				FormFile f = (FormFile) obj ;
				try {
					flag = HttpRequest.post(Constants.addjokeUrl, map, f,null) ;
				} catch (Exception e) {
					Log.e(TAG, e.toString());
				}
			}else if(obj instanceof String){
				String enc = (String) obj ;
				try {
					flag = HttpRequest.post(Constants.addjokeUrl, map, enc,null);
				} catch (Exception e) {
					Log.e(TAG, e.toString());
				}
			}
			return flag ;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			if(proDialog != null && proDialog.isShowing()) {
				proDialog.dismiss();
			}
			if(result){
				JokeFragment.isNeedReload = true ;
				SystemUtil.makeShortToast(R.string.ok_addjoke);
				finish();
			}else{
				SystemUtil.makeShortToast(R.string.error_uploadjoke);
			}
			super.onPostExecute(result);
		}
	}
}