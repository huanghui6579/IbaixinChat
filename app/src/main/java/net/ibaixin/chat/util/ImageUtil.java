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

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.download.ImageDownloader.Scheme;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.utils.DiskCacheUtils;
import com.nostra13.universalimageloader.utils.MemoryCacheUtils;

import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.R;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

public class ImageUtil {
	
	/**
	 * 清除该图片的内存缓存
	 * @param imagePath 图像的全路径，包含文件名
	 * @update 2015年8月21日 上午10:29:26
	 */
	public static void clearMemoryCache(String imagePath) {
		if (!TextUtils.isEmpty(imagePath)) {
			String fileUri = Scheme.FILE.wrap(imagePath);
			MemoryCacheUtils.removeFromCache(fileUri, ImageLoader.getInstance().getMemoryCache());
		}
	}

	/**
	 * 清除磁盘缓存
	 * @param imagePath 图像的全路径，包含文件名
	 */
	public static void clearDiskCache(String imagePath) {
		if (!TextUtils.isEmpty(imagePath)) {
			String fileUri = Scheme.FILE.wrap(imagePath);
			DiskCacheUtils.removeFromCache(fileUri, ImageLoader.getInstance().getDiskCache());
		}
	}
	
	/**
	 * 根据质量压缩图片
	 * @author Administrator
	 * @update 2014年11月16日 下午3:14:20
	 * @param image
	 * @return
	 */
	public static Bitmap compressImage(Bitmap image) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		image.compress(CompressFormat.JPEG, 100, baos);// 质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
		int options = 100;
		while (baos.toByteArray().length / 1024 > 100) { // 循环判断如果压缩后图片是否大于100kb,大于继续压缩
			baos.reset();// 重置baos即清空baos
			image.compress(CompressFormat.JPEG, options, baos);// 这里压缩options%，把压缩后的数据存放到baos中
			options -= 10;// 每次都减少10
		}
		ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());// 把压缩后的数据baos存放到ByteArrayInputStream中
		Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);// 把ByteArrayInputStream数据生成图片
		return bitmap;
	}
	
	/**
	 * 按质量压缩图片
	 * @param imagePath 要压缩的图片的全路径，包含文件名
	 * @param savePath 要保存压缩后的图片的全路径，包含文件名
	 * @param quality 要压缩的质量
	 * @return 是否压缩成功
	 * @update 2015年8月21日 上午10:01:23
	 */
	private static boolean compressImage(String imagePath, String savePath, int quality) {
		Bitmap bitmap = null;
		try {
			bitmap = BitmapFactory.decodeFile(imagePath);
			if (bitmap != null) {
				FileOutputStream fos = new FileOutputStream(savePath);
				return bitmap.compress(CompressFormat.JPEG, quality, fos);
			}
		} catch (FileNotFoundException e) {
			Log.e(e.getMessage());
		} finally {
			if (bitmap != null) {
				bitmap.recycle();
			}
		}
		return false;
	}

	/**
	 * 按质量压缩图片
	 * @param bitmap 要压缩的图片
	 * @param savePath 要保存压缩后的图片的全路径，包含文件名
	 * @param quality 要压缩的质量
	 * @return 是否压缩成功
	 * @update 2015年8月21日 上午10:01:23
	 */
	public static boolean compressImage(Bitmap bitmap, String savePath, int quality) {
		try {
			if (bitmap != null) {
				FileOutputStream fos = new FileOutputStream(savePath);
				return bitmap.compress(CompressFormat.JPEG, quality, fos);
			}
		} catch (FileNotFoundException e) {
			Log.e(e.getMessage());
		} finally {
			if (bitmap != null) {
				bitmap.recycle();
			}
		}
		return false;
	}

	/**
	 * 按质量压缩图片,默认10%
	 * @param bitmap 要压缩的图片
	 * @param savePath 要保存压缩后的图片的全路径，包含文件名
	 * @return 是否压缩成功
	 * @update 2015年8月21日 上午10:01:23
	 */
	public static boolean compressImage(Bitmap bitmap, String savePath) {
		return compressImage(bitmap, savePath, 10);
	}
	
	/**
	 * 按质量压缩图片,默认10%
	 * @param imagePath 要压缩的图片的全路径，包含文件名
	 * @param savePath 要保存压缩后的图片的全路径，包含文件名
	 * @return 是否压缩成功
	 * @update 2015年8月21日 上午10:01:23
	 */
	private static boolean compressImage(String imagePath, String savePath) {
		return compressImage(imagePath, savePath, 10);
	}

	/**
	 * 图片按比例大小压缩方法（根据路径获取图片并压缩）
	 * @param srcPath 要获取的图片的完整路径
	 * @return 获取后的bitmap对象
	 */
	public static Bitmap compressImageBySavePath(String srcPath) {
		BitmapFactory.Options newOpts = new BitmapFactory.Options();
		// 开始读入图片，此时把options.inJustDecodeBounds 设回true了
		newOpts.inJustDecodeBounds = true;
		Bitmap bitmap = BitmapFactory.decodeFile(srcPath, newOpts);// 此时返回bm为空
		newOpts.inJustDecodeBounds = false;
		int w = newOpts.outWidth;
		int h = newOpts.outHeight;
		
		/*int pixel=w;
		if(w < h){
			pixel = h;
		}
		int be = 1;// be=1表示不缩放
		if(pixel > 3000){
			be = 4;
		}else if(pixel > 1600){
			be = 2;
		}
		// 现在主流手机比较多是800*480分辨率，所以高和宽我们设置为
		float standard = 800f;// 这里设置高度为800f
		// 缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
		int be = 1;// be=1表示不缩放
		if (w >= h && w > standard) {// 如果宽度大的话根据宽度固定大小缩放
			be = (int) (newOpts.outWidth / standard);
		} else if (w < h && h > standard) {// 如果高度高的话根据宽度固定大小缩放
			be = (int) (newOpts.outHeight /standard);
		}
		if (be <= 0)
			be = 1;*/
		
		// 现在主流手机比较多是800*480分辨率，所以高和宽我们设置为
//		float hh = 800f;// 这里设置高度为800f
//		float ww = 480f;// 这里设置宽度为480f
		// 缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
//		int be = 1;// be=1表示不缩放
		/*if (w > h && w > ww) {// 如果宽度大的话根据宽度固定大小缩放
			be = (int) (newOpts.outWidth / ww);
		} else if (w < h && h > hh) {// 如果高度高的话根据宽度固定大小缩放
			be = (int) (newOpts.outHeight / hh);
		}
		if (be <= 0) {
			be = 1;
		}*/
		
		int reqWidth = 800;
		int reqHeight = 480;

		newOpts.inSampleSize = calculateInSampleSize(newOpts, reqWidth, reqHeight);// 设置缩放比例
		// 重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
		bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
		return compressImageByBitmap(bitmap);// 压缩好比例大小后再进行质量压缩
//		return bitmap;
	}

	/**
	 * 图片按比例大小压缩方法（根据Bitmap图片压缩）
	 * @param image 要压缩的bitmap对象
	 * @return 返回压缩后的bitmap对象
	 */
	public static Bitmap compressImageByBitmap(Bitmap image) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		image.compress(CompressFormat.JPEG, 100, baos);
		if (baos.toByteArray().length / 1024 > 1024) {// 判断如果图片大于1M,进行压缩避免在生成图片（BitmapFactory.decodeStream）时溢出
			baos.reset();// 重置baos即清空baos
			image.compress(CompressFormat.JPEG, 60, baos);// 这里压缩80%，把压缩后的数据存放到baos中
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
		if (be <= 0) {
			be = 1;
		}
		newOpts.inSampleSize = be;// 设置缩放比例
		// 重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
		isBm = new ByteArrayInputStream(baos.toByteArray());
		bitmap = BitmapFactory.decodeStream(isBm, null, newOpts);
		return compressImage(bitmap);// 压缩好比例大小后再进行质量压缩
//		return bitmap;
	}

	/**
	 * 将bitmap保存到SD卡
	 * @param bitmap 要保存的bitmap对象
	 * @param saveName 要保存的文件的全路径
	 * @return 是否保存成功
	 * @throws IOException
	 */
	public static boolean saveBitmap(Bitmap bitmap, String saveName) throws IOException {
		File file = new File(saveName);
		return saveBitmap(bitmap, file);
	}
	
	/**
	 * 将bitmap保存到SD卡
	 * @param bitmap 要保存的bitmap对象
	 * @param saveName 要保存的文件
	 * @return 是否保存成功
	 * @throws IOException
	 */
	public static boolean saveBitmap(Bitmap bitmap, File saveFile) throws IOException {
		if(bitmap == null) {
			return false;
		}
		if (saveFile != null && saveFile.exists()) {
			saveFile.delete();
		}
		BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(saveFile));
		bitmap.compress(CompressFormat.JPEG, 100, os);
		os.flush();
		os.close();
		return true;
	}
	
	/**
	 * 创建地理位置截图的图片文件
	 * @update 2015年2月12日 下午8:01:28
	 * @param bitmap 原bitmap对象
	 * @param saveFile 保存的文件
	 * @throws IOException
	 */
	public static boolean createLocationFile(Bitmap bitmap, File saveFile) throws IOException {
		if(bitmap == null) {
			return false;
		}
		if (saveFile != null && saveFile.exists()) {
			saveFile.delete();
		}
		BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(saveFile));
		bitmap.compress(CompressFormat.JPEG, 20, os);
		os.flush();
		os.close();
		return true;
	}
	
	/**
	 * 以最省内存的方式读取本地资源的图片
	 * @param file 要读取的文件
	 * @return 返回读取的bitmap
	 * @throws FileNotFoundException
	 */
	public static Bitmap readBitmap(File file) throws FileNotFoundException {
		if(file != null) {
			BitmapFactory.Options opt = new BitmapFactory.Options();
			opt.inPreferredConfig = Config.RGB_565;
			opt.inPurgeable = true;
			opt.inInputShareable = true;
			// 获取资源图片
			InputStream is = new FileInputStream(file);
			return BitmapFactory.decodeStream(is, null, opt);
		}
		return null;
	}
	
	/**
	 * 以最省内存的方式读取本地资源的图片
	 * @param fileName 要读取的文件的完整路径
	 * @return 返回读取的bitmap
	 * @throws FileNotFoundException
	 */
	public static Bitmap readBitmap(String fileName) throws FileNotFoundException {
		File file = new File(fileName);
		return readBitmap(file);
	}
	
	/**
	* 计算图片的缩放值
	* @param options
	* @param reqWidth
	* @param reqHeight
	* @return
	*/
	public static int calculateInSampleSize(BitmapFactory.Options options,
		int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;
		
		if (height > reqHeight || width > reqWidth) {
			
			// Calculate ratios of height and width to requested height and
			// width
			final int heightRatio = Math.round((float) height
			/ (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);
			
			// Choose the smallest ratio as inSampleSize value, this will
			// guarantee
			// a final image with both dimensions larger than or equal to the
			// requested height and width.
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}

		return inSampleSize;
	}
	
	/**
	 * 旋转bitmap
	 * @param b 要旋转的bitmap
	 * @param degrees 要旋转的角度
	 * @return 旋转后的bitmap
	 */
	 public static Bitmap rotate(Bitmap b, int degrees) {
        if (degrees != 0 && b != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees, (float) b.getWidth() / 2, (float) b.getHeight() / 2);
            try {
                Bitmap b2 = Bitmap.createBitmap(
                        b, 0, 0, b.getWidth(), b.getHeight(), m, true);
                if (b != b2) {
                    b.recycle();  //Android开发网再次提示Bitmap操作完应该显示的释放
                    b = b2;
                }
            } catch (OutOfMemoryError ex) {
                // Android123建议大家如何出现了内存不足异常，最好return 原始的bitmap对象。.
            }
        }
        return b;
	}
	
	/**
	 * 添加到图库
	 * @param context
	 * @param path 图片的完整路径
	 */
	public static void galleryAddPic(Context context, String path) {
		Intent mediaScanIntent = new Intent(
		Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		File f = new File(path);
		Uri contentUri = Uri.fromFile(f);
		mediaScanIntent.setData(contentUri);
		context.sendBroadcast(mediaScanIntent);
	}

	/**
	 * 根据View(主要是ImageView)的宽和高来获取图片的缩略图
	 * @param path
	 * @param viewWidth
	 * @param viewHeight
	 * @return
	 */
	private Bitmap decodeThumbBitmapForFile(String path, int viewWidth, int viewHeight){
		BitmapFactory.Options options = new BitmapFactory.Options();
		//设置为true,表示解析Bitmap对象，该对象不占内存
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, options);
		//设置缩放比例
		options.inSampleSize = computeScale(options, viewWidth, viewHeight);
		
		//设置为false,解析Bitmap对象加入到内存中
		options.inJustDecodeBounds = false;
		
		return BitmapFactory.decodeFile(path, options);
	}
	
	
	/**
	 * 根据View(主要是ImageView)的宽和高来计算Bitmap缩放比例。默认不缩放
	 * @param options
	 * @param width
	 * @param height
	 */
	private int computeScale(BitmapFactory.Options options, int viewWidth, int viewHeight){
		int inSampleSize = 1;
		if(viewWidth == 0 || viewWidth == 0){
			return inSampleSize;
		}
//		int bitmapWidth = options.outWidth;
//		int bitmapHeight = options.outHeight;
		
		
		//假如Bitmap的宽度或高度大于我们设定图片的View的宽高，则计算缩放比例
		/*if(bitmapWidth > viewWidth || bitmapHeight > viewWidth){
			int widthScale = Math.round((float) bitmapWidth / (float) viewWidth);
			int heightScale = Math.round((float) bitmapHeight / (float) viewWidth);
			
			//为了保证图片不缩放变形，我们取宽高比例最小的那个
			inSampleSize = widthScale < heightScale ? widthScale : heightScale;
		}*/
		// 计算缩放比
		int h = options.outHeight;
		int w = options.outWidth;
		int beWidth = w / viewWidth;
		int beHeight = h / viewHeight;
		if (beWidth < beHeight) {
			inSampleSize = beWidth;
		} else {
			inSampleSize = beHeight;
		}
		if (inSampleSize <= 0) {
			inSampleSize = 1;
		}
		return inSampleSize;
	}
	
	/**
	 * 根据指定的图像路径和大小来获取缩略图 此方法有两点好处： 1.
	 * 使用较小的内存空间，第一次获取的bitmap实际上为null，只是为了读取宽度和高度，
	 * 第二次读取的bitmap是根据比例压缩过的图像，第三次读取的bitmap是所要的缩略图。 2.
	 * 缩略图对于原图像来讲没有拉伸，这里使用了2.2版本的新工具ThumbnailUtils，使 用这个工具生成的图像不会被拉伸。
	 * 
	 * @param imagePath
	 *            图像的路径
	 * @param width
	 *            指定输出图像的宽度
	 * @param height
	 *            指定输出图像的高度
	 * @return 生成的缩略图
	 */

	public static Bitmap getImageThumbnail(String imagePath, int width, int height) {
		Bitmap bitmap = null;
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		// 获取这个图片的宽和高，注意此处的bitmap为null
		bitmap = BitmapFactory.decodeFile(imagePath, options);
		options.inJustDecodeBounds = false; // 设为 false
		// 计算缩放比
		int h = options.outHeight;
		int w = options.outWidth;
		int beWidth = w / width;
		int beHeight = h / height;
		int be = 1;
		if (beWidth < beHeight) {
			be = beWidth;
		} else {
			be = beHeight;
		}
		if (be <= 0) {
			be = 1;
		}
		options.inSampleSize = be;
		// 重新读入图片，读取缩放后的bitmap，注意这次要把options.inJustDecodeBounds 设为 false
		bitmap = BitmapFactory.decodeFile(imagePath, options);
		// 利用ThumbnailUtils来创建缩略图，这里要指定要缩放哪个Bitmap对象
		bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
		return bitmap;
	}
	
	/**
	 * 以最省内存的方式读取本地资源的图片
	 * @param file 要读取的文件
	 * @return 返回读取的bitmap
	 * @throws FileNotFoundException
	 */
	public static Bitmap readBitmap(File file, int outWidth, int outHeight) throws FileNotFoundException {
		if(file != null) {
			BitmapFactory.Options options = setBitmapOption(file, outWidth, outHeight);
			// 获取资源图片
			InputStream is = new FileInputStream(file);
			return BitmapFactory.decodeStream(is, null, options);
		}
		return null;
	}
	
	/**
	 * 以最省内存的方式读取本地资源的图片
	 * @param fileName 要读取的文件的完整路径
	 * @return 返回读取的bitmap
	 * @throws FileNotFoundException
	 */
	public static Bitmap readBitmap(String fileName, int outWidth, int outHeight) throws FileNotFoundException {
		File file = new File(fileName);
		return readBitmap(file, outWidth, outHeight);
	}
	
	/**
	 * 设置bitmap的options
	 * @param filePath 文件的完整路径
	 * @param width 要输出的图片的高度
	 * @param height 要输出的图片的宽度
	 * @return bitmap的options
	 */
	public static BitmapFactory.Options setBitmapOption(String filePath, int width, int height) {
		File file = new File(filePath);
        return setBitmapOption(file, width, height);
    }
	
	/**
	 * 设置bitmap的options
	 * @param file 文件
	 * @param width 要输出的图片的高度
	 * @param height 要输出的图片的宽度
	 * @return bitmap的options
	 */
	public static BitmapFactory.Options setBitmapOption(File file, int width, int height) {
		if(file != null && file.isFile()) {
			String path = file.getAbsolutePath();
			BitmapFactory.Options opt = new BitmapFactory.Options();  
			opt.inJustDecodeBounds = true;            
			//设置只是解码图片的边距，此操作目的是度量图片的实际宽度和高度  
			BitmapFactory.decodeFile(path, opt);  
			
			int outWidth = opt.outWidth; //获得图片的实际高和宽  
			int outHeight = opt.outHeight;  
			opt.inPurgeable = true;
			opt.inInputShareable = true;
			opt.inDither = false;  
			//设置加载图片的颜色数为16bit，默认是RGB_8888，表示24bit颜色和透明通道，但一般用不上
			opt.inPreferredConfig = Config.RGB_565;
			//设置缩放比,1表示原比例，2表示原来的四分之一....  
			opt.inSampleSize = calculateInSampleSize(opt, outWidth, outHeight);
			opt.inJustDecodeBounds = false;//最后把标志复原  
			return opt;
		}
		return new BitmapFactory.Options();
	}
	
	/**
	 * 将bitmap装换成drawable
	 * @param mContext
	 * @param bitmap
	 * @return
	 */
	public static Drawable bitmapToDrawable(Bitmap bitmap) {
		Drawable drawable = new BitmapDrawable(ChatApplication.getInstance().getResources(), bitmap);
		return drawable;
	}
	
	/**
	 * 将drawable转换成bitmap
	 * @param drawable
	 * @return
	 */
	public static Bitmap drawable2Bitmap(Drawable drawable){    
        if(drawable instanceof BitmapDrawable) {    
            return ((BitmapDrawable)drawable).getBitmap() ;    
        } else if (drawable instanceof NinePatchDrawable){    
            Bitmap bitmap = Bitmap    
                    .createBitmap(    
                            drawable.getIntrinsicWidth(),    
                            drawable.getIntrinsicHeight(),    
                            drawable.getOpacity() != PixelFormat.OPAQUE ? Config.ARGB_8888
                                    : Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);    
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),    
                    drawable.getIntrinsicHeight());    
            drawable.draw(canvas);    
            return bitmap;    
        }else{    
            return null ;    
        }    
    } 
	
	/**
	 * 获得圆角图片
	 * @param bitmap
	 * @param roundPx
	 * @return
	 */
	public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, float roundPx) {  
	    int w = bitmap.getWidth();  
	    int h = bitmap.getHeight();  
	    Bitmap output = Bitmap.createBitmap(w, h, Config.ARGB_8888);  
	    Canvas canvas = new Canvas(output);  
	    final int color = 0xff424242;  
	    final Paint paint = new Paint();  
	    final Rect rect = new Rect(0, 0, w, h);  
	    final RectF rectF = new RectF(rect);  
	    paint.setAntiAlias(true);  
	    canvas.drawARGB(0, 0, 0, 0);  
	    paint.setColor(color);  
	    canvas.drawRoundRect(rectF, roundPx, roundPx, paint);  
	    paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));  
	    canvas.drawBitmap(bitmap, rect, rect, paint);  
	  
	    return output;  
	} 
	
	/**
	 * 获得带倒影的图片
	 * @param bitmap
	 * @return
	 */
	public static Bitmap createReflectionImageWithOrigin(Bitmap bitmap) {  
	    final int reflectionGap = 4;  
	    int w = bitmap.getWidth();  
	    int h = bitmap.getHeight();  
	  
	    Matrix matrix = new Matrix();  
	    matrix.preScale(1, -1);  
	  
	    Bitmap reflectionImage = Bitmap.createBitmap(bitmap, 0, h / 2, w,  
	            h / 2, matrix, false);  
	  
	    Bitmap bitmapWithReflection = Bitmap.createBitmap(w, (h + h / 2),  
	            Config.ARGB_8888);  
	  
	    Canvas canvas = new Canvas(bitmapWithReflection);  
	    canvas.drawBitmap(bitmap, 0, 0, null);  
	    Paint deafalutPaint = new Paint();  
	    canvas.drawRect(0, h, w, h + reflectionGap, deafalutPaint);  
	  
	    canvas.drawBitmap(reflectionImage, 0, h + reflectionGap, null);  
	  
	    Paint paint = new Paint();  
	    LinearGradient shader = new LinearGradient(0, bitmap.getHeight(), 0,  
	            bitmapWithReflection.getHeight() + reflectionGap, 0x70ffffff,  
	            0x00ffffff, TileMode.CLAMP);  
	    paint.setShader(shader);  
	    // Set the Transfer mode to be porter duff and destination in  
	    paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));  
	    // Draw a rectangle using the paint with our linear gradient  
	    canvas.drawRect(0, h, w, bitmapWithReflection.getHeight()  
	            + reflectionGap, paint);  
	  
	    return bitmapWithReflection;  
	} 
	
	/**
	 * drawable的缩放
	 * @param drawable
	 * @param w
	 * @param h
	 * @return
	 */
    public static Drawable zoomDrawable(Drawable drawable, int w, int h) {  
        int width = drawable.getIntrinsicWidth();  
        int height = drawable.getIntrinsicHeight();  
        // drawable转换成bitmap  
        Bitmap oldbmp = drawable2Bitmap(drawable);  
        // 创建操作图片用的Matrix对象  
        Matrix matrix = new Matrix();  
        // 计算缩放比例  
        float sx = ((float) w / width);  
        float sy = ((float) h / height);  
        // 设置缩放比例  
        matrix.postScale(sx, sy);  
        // 建立新的bitmap，其内容是对原bitmap的缩放后的图  
        Bitmap newbmp = Bitmap.createBitmap(oldbmp, 0, 0, width, height,  
                matrix, true);  
        return new BitmapDrawable(newbmp);  
    }
    
    /**
     * 将bitmap转换成byte数组
     * @param bm
     * @return
     */
    public byte[] bitmap2Bytes(Bitmap bm) {  
        ByteArrayOutputStream baos = new ByteArrayOutputStream();  
        bm.compress(CompressFormat.PNG, 100, baos);
        return baos.toByteArray();  
    }
    
    /**
     * 将byte数组转换成bitmap
     * @param b
     * @return
     */
    public Bitmap bytes2Bimap(byte[] b) {  
        if (b.length != 0) {  
            return BitmapFactory.decodeByteArray(b, 0, b.length);  
        } else {  
            return null;  
        }  
    }
    
    /**
     * 根据Uri获取图片Bitmap
     * @param c
     * @param uri
     * @return
     */
    public static Bitmap getBitmapFromUri(Context c, Uri uri){
	     try{
		      // 读取uri所在的图片
		      Bitmap bitmap = MediaStore.Images.Media.getBitmap(c.getContentResolver(), uri);
		      return bitmap;
	     }catch (Exception e) {
		      Log.e("[Android]", e.getMessage());
		      Log.e("[Android]", "目录为：" + uri);
		      e.printStackTrace();
		      return null;
	     }
    }
    
    /**
     * 获取图片的缩略图
     * @param imagePath 原始图片的路径，含完整文件名
     * @param imageSize 指定的缩略图的参考尺寸
     * @param savePath 存储缩略图的路径，包含文件名
     * @return 缩略图
     * @author tiger
     * @version 1.0.0
     * @update 2015年5月3日 下午1:51:01
     */
    public static boolean generateThumbImage(String imagePath, ImageSize imageSize, String savePath) {
    	ImageLoader imageLoader = ImageLoader.getInstance();
    	//清除之前的缓存
    	Bitmap bitmap = imageLoader.loadImageSync(Scheme.FILE.wrap(imagePath), imageSize, SystemUtil.getAlbumImageOptions());
    	if (bitmap != null) {
    		try {
				return compressImage(bitmap, savePath);
			} catch (Exception e) {
				Log.e(e.getMessage());
			}
    	}
    	return false;
    }
    
    /**
     * 异步生成缩略图
     * @param imagePath 原始图片的文件的路径
     * @param imageSize 要生存缩略图的尺寸大小
     * @param savePath 保存缩略图的文职
     * @param loadingListener 生存缩略图的监听
     * @update 2015年7月27日 下午4:35:46
     */
	@Deprecated
    public static void generateThumbImageAsync(String imagePath, ImageSize imageSize, String savePath, ImageLoadingListener loadingListener) {
    	ImageLoader imageLoader = ImageLoader.getInstance();
    	String iconUri = Scheme.FILE.wrap(imagePath);
    	MemoryCacheUtils.removeFromCache(iconUri, imageLoader.getMemoryCache());
    	imageLoader.loadImage(iconUri, imageSize, SystemUtil.getAlbumImageOptions(), loadingListener);
    }
    
    /**
     * 异步生成缩略图,默认缩略图的尺寸是 100x100
     * @param imagePath 原始图片的文件的路径
     * @param imageSize 要生存缩略图的尺寸大小, 默认缩略图的尺寸是 100x100
     * @param savePath 保存缩略图的文职
     * @param loadingListener 生存缩略图的监听
     * @update 2015年7月27日 下午4:35:46
     */
	@Deprecated
    public static void generateThumbImageAsync(String imagePath, String savePath, ImageLoadingListener loadingListener) {
    	generateThumbImageAsync(imagePath, new ImageSize(Constants.IMAGE_THUMB_WIDTH, Constants.IMAGE_THUMB_HEIGHT), savePath, loadingListener);
    }
    
    /**
     * 获取图片的缩略图
     * @param imagePath 原始图片的路径，含完整文件名
     * @param savePath 存储缩略图的路径，包含文件名
     * @return 缩略图
     * @author tiger
     * @version 1.0.0
     * @update 2015年5月3日 下午1:51:01
     */
    public static boolean generateThumbImage(String imagePath, String savePath) {
    	return generateThumbImage(imagePath, null/*new ImageSize(Constants.IMAGE_THUMB_WIDTH, Constants.IMAGE_THUMB_HEIGHT)*/, savePath);
    }
    
    /**
     * 获取图片的缩略图
     * @param threadId 会话的ID
     * @param filename 文件的名称，不含有目录
     * @return 缩略图的存储完整路径，含文件名
     * @author tiger
     * @version 1.0.0
     * @update 2015年5月3日 下午1:51:01
     */
    public static String generateThumbImage(String imagePath, int threadId, String filename) {
    	String savePath = SystemUtil.generateChatThumbAttachFilePath(threadId, filename);
    	boolean success = generateThumbImage(imagePath, null/*new ImageSize(Constants.IMAGE_THUMB_WIDTH, Constants.IMAGE_THUMB_HEIGHT)*/, savePath);
    	if (success) {
    		return savePath;
    	} else {
    		return null;
    	}
    }

	/**
	 * 同步加载图片的缩略图
	 * @author tiger
	 * @update 2015年3月7日 下午5:53:46
	 * @param uri
	 * @param listener
	 */
	public static Bitmap loadImageThumbnailsSync(String uri) {
		return loadImageThumbnailsSync(uri, null);
	}

	/**
	 * 同步加载图片的缩略图
	 * @author tiger
	 * @update 2015年3月7日 下午5:53:46
	 * @param uri
	 * @param listener
	 */
	public static Bitmap loadImageThumbnailsSync(String uri, ImageSize imageSize) {
		DisplayImageOptions options = new DisplayImageOptions.Builder()
				.showImageForEmptyUri(R.drawable.ic_default_icon_error)
				.showImageOnFail(R.drawable.ic_default_icon_error)
				.cacheInMemory(true)
				.cacheOnDisk(false)
				.imageScaleType(ImageScaleType.IN_SAMPLE_INT)
				.bitmapConfig(Bitmap.Config.RGB_565)	//防止内存溢出
				.resetViewBeforeLoading(true)
				.build();
		ImageLoader imageLoader = ImageLoader.getInstance();
		return imageLoader.loadImageSync(uri, imageSize, options);
	}
}
