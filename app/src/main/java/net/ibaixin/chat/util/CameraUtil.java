package net.ibaixin.chat.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
/**
 * 相机拍照相关操作
 * @author DUDEJIN
 */
public class CameraUtil {

	public final static int TAKE_PICCODE = 100 ;
	public final static int SELECT_PICCODE = 101 ;
	public final static int CROP_PICCODE = 102 ;
	public final static String PIC_NAME = "ibaixinjoke.jpg" ;
	public final static String PIC_CROP_NAME = "ibaixinjoke_crop.jpg" ;
	/**
	 * 获取照片的途径
	 */
	public static void selectPictureOption(final Context context) {
		final String[] mItems = { "相机拍摄", "手机相册" };
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("选择图片来源");
		builder.setItems(mItems, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (which == 0) {// IMAGE_CAPTURE
					Intent intent = new Intent(
							"android.media.action.IMAGE_CAPTURE");
					String sdStatus = Environment.getExternalStorageState();
					if (!sdStatus.equals(Environment.MEDIA_MOUNTED)) { // 检测sd是否可用
						Log.v("TestFile","SD card is not avaiable/writeable right now.");
						SystemUtil.makeShortToast("内存卡不可用");
						return;
					} else {
						// 下面这句指定调用相机拍照后的照片存储的路径
						intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri
								.fromFile(new File(SystemUtil.getPublicFilePath(),PIC_NAME)));
					}
					((Activity)context).startActivityForResult(intent, TAKE_PICCODE);
				} else {// 获取相册的片
					Intent intent = new Intent(Intent.ACTION_PICK, null);
					intent.setDataAndType(
							MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
							MediaStore.Images.Media.CONTENT_TYPE);
					((Activity)context).startActivityForResult(intent, SELECT_PICCODE);
				}
			}
		});
		builder.create().show();
	}

	/**
	 * 裁剪图片方法实现
	 * 
	 * @param uri
	 */
	public static void startPhotoZoom(Context context, Uri uri) {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(uri, "image/*");
		// 下面这个crop=true是设置在开启的Intent中设置显示的VIEW可裁剪
		intent.putExtra("crop", "true");
		// aspectX aspectY 是宽高的比例
//		intent.putExtra("aspectX", 1);
//		intent.putExtra("aspectY", 1);
		// outputX outputY 是裁剪图片宽高
		DisplayMetrics dm = new DisplayMetrics();  
		dm = context.getResources().getDisplayMetrics();  
		int screenWidth = dm.widthPixels; // 屏幕宽（像素，如：480px）  
		int screenHeight = dm.heightPixels; // 屏幕高（像素，如：800px）  
		intent.putExtra("outputX", screenWidth*3);
		intent.putExtra("outputY", screenHeight*3);
		intent.putExtra("noFaceDetection", true);  //关闭人脸识别
		intent.putExtra("return-data", true); //需要返回数据
		((Activity)context).startActivityForResult(intent, CROP_PICCODE);
	}
	
	/**
	 * 图片质量压缩方法
	 * 
	 * @param image
	 * @return
	 */
	public static Bitmap compressImage(Bitmap image) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		image.compress(CompressFormat.JPEG, 100, baos);// 质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
		int options = 100;
		while (baos.toByteArray().length / 1024 > 60) { // 循环判断如果压缩后图片是否大于100kb,大于继续压缩
			baos.reset();// 重置baos即清空baos
			image.compress(CompressFormat.JPEG, options, baos);// 这里压缩options%，把压缩后的数据存放到baos中
			options -= 10;// 每次都减少10
		}
		ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());// 把压缩后的数据baos存放到ByteArrayInputStream中
		Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);// 把ByteArrayInputStream数据生成图片
		return bitmap;
	}

	/**
	 * 图片按比例大小压缩方法（根据路径获取图片并压缩）
	 * 
	 * @param srcPath
	 * @return
	 */
	public static Bitmap getCompressImageBySavePath(String srcPath) {
		BitmapFactory.Options newOpts = new BitmapFactory.Options();
		// 开始读入图片，此时把options.inJustDecodeBounds 设回true了
		newOpts.inJustDecodeBounds = true;
		Bitmap bitmap = BitmapFactory.decodeFile(srcPath, newOpts);// 此时返回bm为空
		newOpts.inJustDecodeBounds = false;
		int w = newOpts.outWidth;
		int h = newOpts.outHeight;
		
		int pixel=w;
		if(w<h){
			pixel=h;
		}
		int be = 1;// be=1表示不缩放
		if(pixel>3000){
			be=4;
		}else if(pixel>1600){
			be=2;
		}
/*		// 现在主流手机比较多是800*480分辨率，所以高和宽我们设置为
		float standard = 800f;// 这里设置高度为800f
		// 缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
		int be = 1;// be=1表示不缩放
		if (w >= h && w > standard) {// 如果宽度大的话根据宽度固定大小缩放
			be = (int) (newOpts.outWidth / standard);
		} else if (w < h && h > standard) {// 如果高度高的话根据宽度固定大小缩放
			be = (int) (newOpts.outHeight /standard);
		}
		if (be <= 0)
			be = 1;
*/
		newOpts.inSampleSize = be;// 设置缩放比例
		// 重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
		bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
		return compressImage(bitmap);// 压缩好比例大小后再进行质量压缩
	}

	/**
	 * 图片按比例大小压缩方法（根据Bitmap图片压缩）
	 * 
	 * @param image
	 * @return
	 */
	private static Bitmap comp(Bitmap image) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		image.compress(CompressFormat.JPEG, 100, baos);
		if (baos.toByteArray().length / 1024 > 1024) {// 判断如果图片大于1M,进行压缩避免在生成图片（BitmapFactory.decodeStream）时溢出
			baos.reset();// 重置baos即清空baos
			image.compress(CompressFormat.JPEG, 50, baos);// 这里压缩50%，把压缩后的数据存放到baos中
		}
		ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
		BitmapFactory.Options newOpts = new BitmapFactory.Options();
		// 开始读入图片，此时把options.inJustDecodeBounds 设回true了
		newOpts.inJustDecodeBounds = true;
		Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, newOpts);
		newOpts.inJustDecodeBounds = false;
		int w = newOpts.outWidth;
		int h = newOpts.outHeight;
		// 现在主流手机比较多是800*480分辨率，所以高和宽我们设置为
		float hh = 800f;// 这里设置高度为800f
		float ww = 480f;// 这里设置宽度为480f
		// 缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
		int be = 1;// be=1表示不缩放
		if (w > h && w > ww) {// 如果宽度大的话根据宽度固定大小缩放
			be = (int) (newOpts.outWidth / ww);
		} else if (w < h && h > hh) {// 如果高度高的话根据宽度固定大小缩放
			be = (int) (newOpts.outHeight / hh);
		}
		if (be <= 0)
			be = 1;
		newOpts.inSampleSize = be;// 设置缩放比例
		// 重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
		isBm = new ByteArrayInputStream(baos.toByteArray());
		bitmap = BitmapFactory.decodeStream(isBm, null, newOpts);
		return compressImage(bitmap);// 压缩好比例大小后再进行质量压缩
	}

	public static boolean saveBitmap(Bitmap bitmap, String saveName) throws IOException {
		if(bitmap==null)
			return false;
		File file = new File(saveName);
		if (file.exists()) {
			file.delete();
		}
		BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file));
		bitmap.compress(CompressFormat.JPEG, 80, os);
		os.flush();
		os.close();
		return true;
	}
	
	/**
	 * 以最省内存的方式读取本地资源的图片
	 * 
	 * @param context
	 * @param resId
	 * @return
	 * @throws FileNotFoundException 
	 */
	public static Bitmap readBitMap(File file) throws FileNotFoundException {
		BitmapFactory.Options opt = new BitmapFactory.Options();
		opt.inPreferredConfig = Bitmap.Config.RGB_565;
		opt.inPurgeable = true;
		opt.inInputShareable = true;
		// 获取资源图片
		InputStream is = new FileInputStream(file);
		return BitmapFactory.decodeStream(is, null, opt);
	}
}
