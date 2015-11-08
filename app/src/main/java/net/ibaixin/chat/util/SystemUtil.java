package net.ibaixin.chat.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.internal.widget.TintTypedArray;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.nostra13.universalimageloader.cache.disc.DiskCache;
import com.nostra13.universalimageloader.cache.disc.impl.BaseDiskCache;
import com.nostra13.universalimageloader.cache.disc.impl.ext.LruDiskCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.download.ImageDownloader.Scheme;

import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.R;
import net.ibaixin.chat.model.Emoji;
import net.ibaixin.chat.model.FileItem;
import net.ibaixin.chat.model.FileItem.FileType;
import net.ibaixin.chat.model.MsgInfo;
import net.ibaixin.chat.model.MsgInfo.Type;
import net.ibaixin.chat.model.MsgThread;
import net.ibaixin.chat.model.PhotoItem;
import net.ibaixin.chat.model.emoji.Emojicon;
import net.ibaixin.chat.volley.toolbox.MultiPartStringRequest;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 系统常用的工具方法
 * 
 * @author huanghui1
 *
 */
public class SystemUtil {
	
	private static ExecutorService cachedThreadPool = null;//可缓存的线程池
	
	private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);
	
	private static final AtomicInteger REDIRECT_TIME = new AtomicInteger(0);
	
	/**
	 * 隐藏输入法
	 * @param view
	 */
	public static void hideSoftInput(View view) {
		InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}
	
	/**
	 * 隐藏软键盘
	 * @update 2015年1月9日 上午9:35:31
	 * @param activity
	 */
	public static void hideSoftInput(Activity activity) {
		View view = activity.getCurrentFocus();
	    if (view != null) {
	        InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
	        inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
	    }
	}
	
	/**
	 * 如果输入法在窗口上已经显示，则隐藏，反之则显示
	 * @update 2014年10月25日 下午4:47:02
	 */
	public static void toogleSoftInput() {
		InputMethodManager imm = (InputMethodManager) ChatApplication.getInstance().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
	}
	
	/**
	 * 显示输入法
	 * @update 2014年10月25日 下午4:47:46
	 * @param view
	 */
	public static void showSoftInput(View view) {
		InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(view, InputMethodManager.SHOW_FORCED);
	}
	
	/**
	 * 获取输入法打开的状态
	 * @update 2014年10月25日 下午4:49:06
	 * @return
	 */
	public static boolean isSoftInputActive() {
		InputMethodManager imm = (InputMethodManager) ChatApplication.getInstance().getSystemService(Context.INPUT_METHOD_SERVICE);
		boolean isOpen=imm.isActive();//isOpen若返回true，则表示输入法打开
		return isOpen;
	}
	
	/**
	 * 显示短时间的toast
	 * @author Administrator
	 * @update 2014年10月7日 上午9:49:18
	 * @param text
	 */
	public static void makeShortToast(CharSequence text) {
		Toast toast = Toast.makeText(ChatApplication.getInstance(), text, Toast.LENGTH_SHORT);
		toast = setToastStyle(toast);
		toast.show();
	}
	
	/**
	 * 显示短时间的toast
	 * @author Administrator
	 * @update 2014年10月7日 上午9:49:18
	 * @param resId
	 */
	public static void makeShortToast(int resId) {
		Toast toast = Toast.makeText(ChatApplication.getInstance(), resId, Toast.LENGTH_SHORT);
		toast = setToastStyle(toast);
		toast.show();
	}
	
	/**
	 * 显示长时间的toast
	 * @author Administrator
	 * @update 2014年10月7日 上午9:50:02
	 * @param text
	 */
	public static void makeLongToast(CharSequence text) {
		Toast toast = Toast.makeText(ChatApplication.getInstance(), text, Toast.LENGTH_LONG);
		toast = setToastStyle(toast);
		toast.show();
	}
	
	/**
	 * 显示长时间的toast
	 * @author Administrator
	 * @update 2014年10月7日 上午9:50:02
	 * @param resId
	 */
	public static void makeLongToast(int resId) {
		Toast toast = Toast.makeText(ChatApplication.getInstance(), resId, Toast.LENGTH_LONG);
		toast = setToastStyle(toast);
		toast.show();
	}
	
	/**
	 * 设置Toast的样式
	 * @update 2014年11月12日 下午4:22:41
	 * @param toast
	 * @return
	 */
	private static Toast setToastStyle(Toast toast) {
		View view = toast.getView();
		view.setBackgroundResource(R.drawable.toast_frame_holo);
		TextView textView = (TextView) view.findViewById(android.R.id.message);
		textView.setTextColor(Color.WHITE);
		return toast;
	}
	
	/**
	 * 获取手机型号
	 * @update 2014年10月9日 上午8:39:55
	 * @return
	 */
	public static String getPhoneModel() {
//		if(TextUtils.isEmpty(model)) {
		String model = "ibaixin";
//			model = "Android";
//		}
		return model;
	}
	
	/**
	 * 获得当前的Android版本
	 * @update 2014年10月13日 上午9:11:10
	 * @return
	 */
	public static final int getCurrentSDK() {
		return Build.VERSION.SDK_INT;
	}
	
	/**
	 * SD卡是否可用
	 * @update 2014年10月23日 下午5:17:09
	 * @return
	 */
	public static boolean isSdcardAvailable() {
		return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
	}
	
	/**
	 * 保存文件，根据用户名动态生成文件夹，该用户名为当前登录的用户名
	 * @update 2014年10月23日 下午5:06:50
	 * @param data
	 * @param filePath 保存文件的路径，不含文件名
	 * @param filename 保存的文件名称，不含有路径
	 * @return
	 */
	public static File saveFile(byte[] data, String filePath, String filename) {
		BufferedOutputStream bos = null;
		FileOutputStream fos = null;
		File file = null;
		try {
			File dir = new File(filePath);
			if (!dir.exists() && dir.isDirectory()) {
				dir.mkdirs();
			}
			file = new File(dir, filename);
			fos = new FileOutputStream(file);
			bos = new BufferedOutputStream(fos);
			bos.write(data);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (bos != null) {
				try {
					bos.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				bos = null;
			}
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				fos = null;
			}
		}
		return file;
	}
	
	/**
	 * 保存文件，根据用户名动态生成文件夹，该用户名为当前登录的用户名
	 * @update 2014年10月23日 下午5:06:50
	 * @param data
	 * @param savePath 保存文件的路径，含文件名
	 * @return
	 */
	public static File saveFile(byte[] data, String savePath) {
		BufferedOutputStream bos = null;
		FileOutputStream fos = null;
		File saveFile = null;
		try {
			saveFile = new File(savePath);
			File dir = saveFile.getParentFile();
			if (!dir.exists()) {
				dir.mkdirs();
			}
			fos = new FileOutputStream(saveFile);
			bos = new BufferedOutputStream(fos);
			bos.write(data);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (bos != null) {
				try {
					bos.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				bos = null;
			}
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				fos = null;
			}
		}
		return saveFile;
	}
	
	/**
	 * 根据文件的全路径判断文件是否存在
	 * @update 2014年10月24日 上午9:00:51
	 * @param filePath 文件的全路径
	 * @return
	 */
	public static boolean isFileExists(String filePath) {
		boolean flag = false;
		if (TextUtils.isEmpty(filePath)) {
			flag = false;
		} else {
			File file = new File(filePath);
			flag = file.exists();
		}
		return flag;
	}
	
	/**
	 * 根据文件判断文件是否存在
	 * @update 2015年3月3日 下午7:09:39
	 * @param file
	 * @return
	 */
	public static boolean isFileExists(File file) {
		if (file == null) {
			return false;
		} else {
			return file.exists();
		}
	}
	
	/**
	 * 保存文件，根据用户名动态生成文件夹，该用户名为当前登录的用户名
	 * @update 2014年10月23日 下午5:06:50
	 * @param data
	 * @param saveFile 保存文件的路径，不含文件名
	 * @return
	 */
	public static File saveFile(byte[] data, File saveFile) {
		if (data == null || data.length <= 0) {
			return null;
		}
		BufferedOutputStream bos = null;
		FileOutputStream fos = null;
		try {
			File dir = saveFile.getParentFile();
			if (!dir.exists() && dir.isDirectory()) {
				dir.mkdirs();
			}
			fos = new FileOutputStream(saveFile);
			bos = new BufferedOutputStream(fos);
			bos.write(data);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (bos != null) {
				try {
					bos.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				bos = null;
			}
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				fos = null;
			}
		}
		return saveFile;
	}
	
	/**
	 * 保存文件，根据用户名动态生成文件夹，该用户名为当前登录的用户名
	 * @update 2014年10月23日 下午5:06:50
	 * @param photoVal 图片的Base64位编码字符串
	 * @param saveFile 保存文件的路径，不含文件名
	 * @return
	 */
	public static File saveFile(String photoVal, File saveFile) {
		byte[] data = getAvatarByStringVal(photoVal);
		if (data != null && data.length > 0) {
			return saveFile(data, saveFile);
		} else {
			return null;
		}
	}
	
	/**
	 * 保存文件，根据用户名动态生成文件夹，该用户名为当前登录的用户名
	 * @update 2014年10月23日 下午5:06:50
	 * @param photoVal 图片的Base64位编码字符串
	 * @param savePath 保存文件的路径，不含文件名
	 * @return
	 */
	public static File saveFile(String photoVal, String savePath) {
		byte[] data = getAvatarByStringVal(photoVal);
		if (data != null && data.length > 0) {
			return saveFile(data, savePath);
		} else {
			return null;
		}
	}
	
	/**
	 * 将字节数组转换成文件
	 * @update 2014年10月23日 下午5:26:48
	 * @param file
	 * @return
	 */
	public static byte[] getFileBytes(File file) {
		
		byte[] data = null;
		FileInputStream fis = null;
		ByteArrayOutputStream baos = null;
		try {
			fis = new FileInputStream(file);
			baos = new ByteArrayOutputStream(1024);
			byte[] buf = new byte[1024];
			int len = -1;
			while ((len = fis.read(buf)) != -1) {
				baos.write(buf, 0, len);
			}
			data = baos.toByteArray();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (baos != null) {
				try {
					baos.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			baos = null;
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			fis = null;
		}
		return data;
	}
	
	/**
	 * 将字节数组转换成文件
	 * @update 2014年10月23日 下午5:26:48
	 * @param filename 文件的全路径，包含文件名
	 * @return
	 */
	public static byte[] getFileBytes(String filename) {
		File file = new File(filename);
		return getFileBytes(file);
	}
	
	private static String convertByteArrayToHexString(byte[] arrayBytes) {
	    StringBuffer stringBuffer = new StringBuffer();
	    for (int i = 0; i < arrayBytes.length; i++) {
	        stringBuffer.append(Integer.toString((arrayBytes[i] & 0xff) + 0x100, 16)
	                .substring(1));
	    }
	    return stringBuffer.toString();
	}
	
	/**
	 * 根据文件的字节数组获取文件的hash值
	 * @update 2014年10月23日 下午5:35:11
	 * @param bytes
	 * @return
	 */
	public static String getFileHash(byte[] bytes) {
		String hash= null;
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			digest.update(bytes);
			byte[] hashbyte = digest.digest();
			hash = convertByteArrayToHexString(hashbyte);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return hash;
	}
	
	/**
	 * 根据文件的字节数组获取文件的hash值
	 * @update 2014年10月23日 下午5:35:11
	 * @param file
	 * @return
	 */
	public static String getFileHash(File file) {
		String hash= null;
		FileInputStream fis = null;
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			fis = new FileInputStream(file);
			byte[] buf = new byte[1024];
			int len = -1;
			while ((len = fis.read(buf)) != -1) {
				digest.update(buf, 0, len);
			}
			byte[] hashbyte = digest.digest();
			hash = convertByteArrayToHexString(hashbyte);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				fis = null;
			}
		}
		return hash;
	}
	
	/**
	 * 通过64编码将图像的字符创解析为byte数组
	 * @update 2014年11月10日 下午9:45:31
	 * @param val
	 * @return
	 */
	public static byte[] getAvatarByStringVal(String val) {
        if (val == null) {
            return null;
        }
        return StringUtils.decodeBase64(val);
    }
	
	/**
	 * 根据当前用户获取root目录
	 * @update 2014年10月24日 下午8:12:44
	 * @return
	 */
	public static File getDefaultRoot() {
		String currentUser = ChatApplication.getInstance().getCurrentAccount();
		File root = new File(Environment.getExternalStorageDirectory(), Constants.DEAULT_APP_FOLDER_NAME + File.separator + currentUser);
		if (!root.exists()) {
			boolean success = root.mkdirs();
			Log.d("------getDefaultRoot---mkdirs--success--" + success + "----------" + root.getAbsolutePath());
		}
		return root;
	}
	
	/**
	 * 根据当前用户获取root目录
	 * @update 2014年10月24日 下午8:12:44
	 * @return
	 */
	public static String getDefaultRootPath() {
		String currentUser = ChatApplication.getInstance().getCurrentAccount();
		File root = new File(getDefaultAppPath(), currentUser);
		if (!root.exists()) {
			root.mkdirs();
		}
		return root.getAbsolutePath();
	}
	
	/**
	 * 获得SDcard的根目录
	 * @update 2014年11月21日 下午6:18:07
	 * @return
	 */
	public static String getSDCardRootPath() {
		return Environment.getExternalStorageDirectory().getAbsolutePath();
	}
	
	/**
	 * 获取该应用程序默认存储在sd卡中的文件夹名称，默认路径为/mnt/sdcard/ChatApp
	 * @author tiger
	 * @update 2015年3月12日 下午11:59:15
	 * @return
	 */
	public static String getDefaultAppPath() {
		return getDefaultAppFile().getAbsolutePath();
	}
	
	/**
	 * 获取该应用程序默认存储在sd卡中的文件夹名称，默认路径为/mnt/sdcard/ChatApp
	 * @author tiger
	 * @update 2015年3月13日 上午12:00:33
	 * @return
	 */
	public static File getDefaultAppFile() {
		File root = new File(Environment.getExternalStorageDirectory(), Constants.DEAULT_APP_FOLDER_NAME);
		if (!root.exists()) {
			root.mkdirs();
		}
		return root;
	}
	
	/**
	 * 获得SDcard的根目录
	 * @update 2014年11月21日 下午6:18:07
	 * @return
	 */
	public static File getSDCardRoot() {
		return Environment.getExternalStorageDirectory();
	}
	
	/**
	 * 该目录是否是根目录
	 * @update 2014年11月21日 下午7:40:34
	 * @param path
	 * @return
	 */
	public static boolean isRoot(String path) {
		return "/".equals(path);
	}
	
	/**
	 * 该目录是否是根目录
	 * @update 2014年11月21日 下午7:40:34
	 * @param path
	 * @return
	 */
	public static boolean isRoot(File path) {
		return isRoot(path.getAbsolutePath());
	}
	
	/**
	 * 获取头像默认的存放路径，格式为/mnt/sdcard/ChatApp/currentuser/head_icon/
	 * @update 2014年10月23日 下午6:09:27
	 * @return
	 */
	public static String getDefaultIconPath() {
		return getDefaultIconPath(Constants.FILE_TYPE_ORIGINAL);
	}
	
	/**
	 * 根据头像类型获取头像默认的存放路径，头像类型只有两种，一种是原始图片，值为2，另一种是缩略图，值为1，若为缩略图，则格式为/mnt/sdcard/ChatApp/currentuser/head_icon/thumb/,
	 * 其他情况格式为/mnt/sdcard/ChatApp/currentuser/head_icon/
	 * @update 2014年10月23日 下午6:09:27
	 * @param fileType 头像类型只有两种，一种是原始图片，值为2，另一种是缩略图，值为1
	 * @return
	 */
	public static String getDefaultIconPath(int fileType) {
		String filePath = getDefaultRoot().getAbsolutePath() + File.separator + "head_icon";
		if (fileType == Constants.FILE_TYPE_THUMB) {	//缩略图
			filePath = filePath + File.separator + "thumb";
		}
		File dir = new File(filePath);
		if (!dir.exists()) {
			boolean success = dir.mkdirs();
			Log.d("------getDefaultIconPath---mkdirs--success--" + success + "----------" + dir.getAbsolutePath());
		}
		return dir.getAbsolutePath();
	}
	
	/**
	 * 根据用户名创建图像
	 * @update 2014年10月23日 下午6:11:41
	 * @param username 用户名
	 * @param fileType 头像的类型，主要是缩略图：1,原始图片：2
	 * @return
	 */
	public static File generateIconFile(String username, int fileType) {
		return new File(getDefaultIconPath(fileType), generateIconName(username, fileType));
	}
	
	/**
	 * 根据用户名创建图像
	 * @param fileType 头像的类型，主要是缩略图：1,原始图片：2
	 * @param iconName 头像的文件名称
	 * @return
	 * @update 2015年7月27日 下午9:34:44
	 */
	public static File generateIconFile(int fileType, String iconName) {
		return new File(getDefaultIconPath(fileType), iconName);
	}
	
	/**
	 * 根据用户名创建图像的全路径
	 * @param username 头像的文件名称
	 * @param fileType 头像的类型，主要是缩略图：1,原始图片：2
	 * @return
	 * @update 2015年7月27日 下午9:35:19
	 */
	public static String generateIconPath(String username, int fileType) {
		return generateIconFile(username, fileType).getAbsolutePath();
	}
	
	public static String generateIconPath(String rootPath, String username, int fileType) {
//		String iconName = gener
		return generateIconFile(username, fileType).getAbsolutePath();
	}
	
	/**
	 * 根据用户名创建图像的全路径
	 * @param fileType 头像的类型，主要是缩略图：1,原始图片：2
	 * @param iconName 头像的文件名称
	 * @return
	 * @update 2015年7月27日 下午9:35:19
	 */
	public static String generateIconPath(int fileType, String iconName) {
		return generateIconFile(fileType, iconName).getAbsolutePath();
	}
	
	/**
	 * 根据用户名和头像的类型来生存对应的头像名称，若为原始图像，则格式为"icon_username.jpgz",若为缩略图，则格式为"icon_username_thumb.jpgz"
	 * @param username
	 * @param fileType
	 * @return
	 * @update 2015年7月25日 下午3:18:58
	 */
	public static String generateIconName(String username, int fileType) {
		StringBuilder prefix = new StringBuilder("icon_").append(username);
		if (fileType == Constants.FILE_TYPE_THUMB) {
			prefix.append("_thumb");
		}
		prefix.append(".jpgz");
		return prefix.toString();
	}
	
	/**
	 * 更具jid获取用户登录的资源
	 * @update 2014年10月23日 下午7:23:52
	 * @param jid
	 * @return
	 */
	public static String getResourceWithJID(String jid) {
		if (TextUtils.isEmpty(jid)) {
			return null;
		}
		if (jid.contains("/")) {
			return jid.substring(jid.indexOf("/") + 1);
		} else {
			return null;
		}
	}
	
	/**
	 * 从SD卡加载图片  
	 * @update 2014年10月24日 下午3:24:51
	 * @param imagePath
	 * @return
	 */
    public static Bitmap getImageFromLocal(String imagePath){
    	if (TextUtils.isEmpty(imagePath)) {
			return null;
		}
        File file = new File(imagePath);  
        if(file.exists()){  
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);  
            return bitmap;  
        }  
        return null;  
    }
    
    /**
     * 获取assets的内容
     * @update 2014年10月27日 上午11:27:27
     * @return
     */
    public static List<String> getEmojiFromFile(String filename) {
    	List<String> list = null;
    	BufferedReader br = null;
    	try {
    		InputStream is = ChatApplication.getInstance().getResources().getAssets().open(filename);
    		list = new ArrayList<>();
    		br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
    		String line = null;
    		while((line = br.readLine()) != null) {
    			list.add(line);
    		}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				br = null;
			}
		}
    	return list;
    }
    
    /**
     * 根据文件名获取文件的资源id
     * @update 2014年10月27日 上午11:31:25
     * @param fileName
     * @return
     */
    public static int getRespurceIdByName(String fileName) {
    	Context context = ChatApplication.getInstance();
    	int resID = context.getResources().getIdentifier(fileName,
				"drawable", context.getPackageName());
    	return resID;
    }
    
    /**
     * 添加表情到editext输入框
     * @update 2014年10月27日 下午4:51:25
     * @param emoji
     * @return
     */
    public static SpannableStringBuilder addEmojiString(Emoji emoji) {
    	SpannableStringBuilder sb = new SpannableStringBuilder();
    	int resId = emoji.getResId();
    	String description = emoji.getDescription();
    	
    	Context context = ChatApplication.getInstance();
    	
    	sb.append(description);
    	Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resId);
    	bitmap = Bitmap.createScaledBitmap(bitmap, 35, 35, true);
    	ImageSpan imageSpan = new ImageSpan(context, bitmap);
    	
    	sb.setSpan(imageSpan, 0, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    	return sb;
    }
    
    /**
     * 获得屏幕的大小,size[0]:屏幕的宽，size[1]:屏幕的高
     * @update 2014年10月29日 下午9:16:12
     * @return
     */
    public static int[] getScreenSize() {
    	int[] size = new int[2];
    	WindowManager wm = (WindowManager) ChatApplication.getInstance().getSystemService(Context.WINDOW_SERVICE);
    	Display display = wm.getDefaultDisplay();
    	Point point = new Point();
    	display.getSize(point);
    	size[0] = point.x;
    	size[1] = point.y;
    	return size;
    }
    
    /**
     * 获取控件的尺寸大小,size[0]:view的宽，size[1]:view的高
     * @update 2014年10月29日 下午9:25:46
     * @param view
     * @return
     */
	public static int[] getViewSize(final View view) {
    	final int[] size = new int[2];
    	/*ViewTreeObserver vto = view.getViewTreeObserver();   
    	vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() { 
    	    @SuppressWarnings("deprecation")
			@Override   
    	    public void onGlobalLayout() {
    	    	ViewTreeObserver obs = view.getViewTreeObserver();
    	    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
    	    		obs.removeOnGlobalLayoutListener(this);
    	        } else {
    	        	obs.removeGlobalOnLayoutListener(this);
    	        }
    	    	size[0] = view.getWidth();
    	    	size[1] = view.getHeight();
    	    	Log.d("-----onGlobalLayout-----" + size[0] + "--,--" + size[1]);
    	    }   
    	});*/
    	
    	int w = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
    	int h = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
    	view.measure(w, h); 
    	int height = view.getMeasuredHeight(); 
    	int width = view.getMeasuredWidth();
    	size[0] = width;
    	size[1] = height;
    	return size;
    }
	
	/**
	 * 判断集合是否为空
	 * @update 2014年10月31日 下午3:22:19
	 * @param collection
	 * @return
	 */
	public static boolean isEmpty(Collection<?> collection) {
		if (collection != null && collection.size() > 0) {
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * 判断集合是否不为空
	 * @update 2015年2月11日 下午9:33:02
	 * @param collection
	 * @return
	 */
	public static boolean isNotEmpty(Collection<?> collection) {
		return !isEmpty(collection);
	}
	
	/**
	 * 判断map是否为空
	 * @update 2014年10月31日 下午3:22:19
	 * @param map
	 * @return
	 */
	public static boolean isEmpty(Map<?, ?> map) {
		if (map != null && map.size() > 0) {
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * 判断map是否不为空
	 * @update 2015年2月11日 下午9:33:23
	 * @param map
	 * @return
	 */
	public static boolean isNotEmpty(Map<?, ?> map) {
		return !isEmpty(map);
	}
	
	/**
	 * 判断一个数组是否为空
	 * @update 2014年10月31日 下午3:24:11
	 * @param array
	 * @return
	 */
	public static <T> boolean isEmpty(T[] array) {
		if (array != null && array.length > 0) {
			return false;
		}
		return true;
	}
	
	/**
	 * 判断数组是否不为空
	 * @update 2015年2月11日 下午9:33:53
	 * @param array
	 * @return
	 */
	public static <T> boolean isNotEmpty(T[] array) {
		return !isEmpty(array);
	}
	
	/**
	 * 格式化会话的时间，时间格式为MM-dd HH:mm
	 * @update 2014年10月31日 下午10:32:10
	 * @param time
	 * @return
	 */
	public static String formatMsgThreadTime(long time) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATEFORMA_TPATTERN_THREAD, Locale.getDefault());
		return dateFormat.format(new Date(time));
	}
	
	/**
	 * 格式化会话的时间
	 * @update 2014年11月14日 下午8:11:37
	 * @param time
	 * @return
	 */
	public static String formatTime(long time, String pattern) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.getDefault());
		return dateFormat.format(new Date(time));
	}
	
	/**
	 * 对spanableString进行正则判断，如果符合要求，则以表情图片代替
	 * 
	 * @param context
	 * @param spannableString
	 * @param patten
	 * @param start
	 * @throws Exception
	 */
	private static void dealExpression(Context context,
			SpannableString spannableString, Pattern patten, int start)
			throws Exception {
		Matcher matcher = patten.matcher(spannableString);
		ChatApplication app = (ChatApplication) context.getApplicationContext();
		while (matcher.find()) {
			//[大哭]
			String key = matcher.group();
			// 返回第一个字符的索引的文本匹配整个正则表达式,ture 则继续递归
			if (matcher.start() < start) {
				continue;
			}
			Emoji emoji = app.getEmojiMap().get(key);
			if (emoji == null) {
				continue;
			}
			int resId = emoji.getResId();
			// 通过上面匹配得到的字符串来生成图片资源id
			if (resId != 0) {
				Bitmap bitmap = BitmapFactory.decodeResource(
						context.getResources(), resId);
				bitmap = Bitmap.createScaledBitmap(bitmap, 50, 50, true);
				// 通过图片资源id来得到bitmap，用一个ImageSpan来包装
				ImageSpan imageSpan = new ImageSpan(context, bitmap);
				// 计算该图片名字的长度，也就是要替换的字符串的长度
				int end = matcher.start() + key.length();
				// 将该图片替换字符串中规定的位置中
				spannableString.setSpan(imageSpan, matcher.start(), end,
						Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
				if (end < spannableString.length()) {
					// 如果整个字符串还未验证完，则继续。。
					dealExpression(context, spannableString, patten, end);
				}
				break;
			}
		}
	}
	
	/**
	 * 得到一个SpanableString对象，通过传入的字符串,并进行正则判断
	 * 
	 * @param context
	 * @param str
	 * @return
	 */
	public static SpannableString getExpressionString(Context context, String str) {
		if (str != null) {
			SpannableString spannableString = new SpannableString(str);
			// 正则表达式比配字符串里是否含有表情，如： 我好[开心]啊
			String zhengze = "\\[[^\\]]+\\]";
			// 通过传入的正则表达式来生成一个pattern
			Pattern sinaPatten = Pattern.compile(zhengze, Pattern.CASE_INSENSITIVE);
			try {
				dealExpression(context, spannableString, sinaPatten, 0);
			} catch (Exception e) {
				Log.e("dealExpression", e.getMessage());
			}
			return spannableString;
		} else {
			return null;
		}
	}
	
	/**
	 * 获得可缓存的线程池
	 * @return
	 */
	public static ExecutorService getCachedThreadPool(){
		if(cachedThreadPool == null) {
			synchronized (SystemUtil.class) {
				if(cachedThreadPool == null) {
					cachedThreadPool = Executors.newCachedThreadPool();
				}
			}
		}
		return cachedThreadPool;
	}
	
	/**
	 * 获得一般的图片加载选项，用户会话列表、聊天界面等的图片显示，该选项没有磁盘缓存图片
	 * @update 2014年11月8日 上午11:43:13
	 * @return
	 */
	public static DisplayImageOptions getGeneralImageOptions() {
		DisplayImageOptions options = new DisplayImageOptions.Builder()
			.showImageOnLoading(R.drawable.contact_head_icon_default)
			.showImageForEmptyUri(R.drawable.contact_head_icon_default)
			.showImageOnFail(R.drawable.contact_head_icon_default)
			.cacheInMemory(true)
			.cacheOnDisk(false)
			.imageScaleType(ImageScaleType.IN_SAMPLE_INT)
			.bitmapConfig(Bitmap.Config.RGB_565)	//防止内存溢出
			.build();
		return options;
	}
	
	
	/**
	 * 获得相册的图片加载选项该选项没有磁盘缓存图片
	 * @update 2014年11月8日 上午11:43:13
	 * @return
	 */
	public static DisplayImageOptions getAlbumImageOptions() {
		DisplayImageOptions options = new DisplayImageOptions.Builder()
			.showImageOnLoading(R.drawable.ic_image_default)
			.showImageForEmptyUri(R.drawable.ic_image_default)
			.showImageOnFail(R.drawable.ic_image_default)
			.cacheInMemory(true)
			.cacheOnDisk(false)
			.imageScaleType(ImageScaleType.IN_SAMPLE_INT)
			.bitmapConfig(Bitmap.Config.RGB_565)	//防止内存溢出
//			.displayer(new FadeInBitmapDisplayer(100))
			.build();
		return options;
	}
	
	/**
	 * 获得相册的视频加载选项该选项有磁盘缓存图片
	 * @update 2014年11月8日 上午11:43:13
	 * @return
	 */
	public static DisplayImageOptions getAlbumVideoOptions() {
		DisplayImageOptions options = new DisplayImageOptions.Builder()
				.showImageOnLoading(R.drawable.ic_image_default)
				.showImageForEmptyUri(R.drawable.ic_image_default)
				.showImageOnFail(R.drawable.ic_image_default)
				.cacheInMemory(true)
				.cacheOnDisk(true)
				.imageScaleType(ImageScaleType.IN_SAMPLE_INT)
				.bitmapConfig(Bitmap.Config.RGB_565)	//防止内存溢出
//			.displayer(new FadeInBitmapDisplayer(100))
				.build();
		return options;
	}
	
	/**
	 * 获得图片加载的选项
	 * @update 2014年11月15日 上午10:33:44
	 * @return
	 */
	public static DisplayImageOptions getPhotoPreviewOptions() {
		DisplayImageOptions options = new DisplayImageOptions.Builder()
			.showImageForEmptyUri(R.drawable.ic_default_icon_error)
			.showImageOnFail(R.drawable.ic_default_icon_error)
			.cacheInMemory(true)
			.cacheOnDisk(false)
			.imageScaleType(ImageScaleType.NONE)
			.bitmapConfig(Bitmap.Config.RGB_565)	//防止内存溢出
//			.displayer(new FadeInBitmapDisplayer(100))
			.build();
		return options;
	}
	
	/**
	 * 获得图片加载的选项
	 * @update 2014年11月15日 上午10:33:44
	 * @return
	 */
	public static DisplayImageOptions getChatImageOptions() {
		DisplayImageOptions options = new DisplayImageOptions.Builder()
			.showImageForEmptyUri(R.drawable.ic_default_icon_error)
			.showImageOnFail(R.drawable.ic_default_icon_error)
			.cacheInMemory(true)
			.cacheOnDisk(false)
			.imageScaleType(ImageScaleType.IN_SAMPLE_INT)
			.bitmapConfig(Bitmap.Config.RGB_565)	//防止内存溢出
			.build();
		return options;
	}
	
	/**
	 * 获得通讯录列表的特殊符号的选择器，特殊符号为：“↑”<br />
	 * <pre>
	 * String s = "↑";
	 * char c = s.charAt(0);
	 * System.out.println((int)c);
	 * </pre>
	 * @update 2014年11月8日 下午3:44:14
	 * @return
	 */
	public static int getContactListFirtSection() {
		return 8593;
	}
	
	/**
	 * 根据用户账号来包装成完整的jid,格式为:xxx@domain
	 * @update 2014年11月10日 下午8:47:14
	 * @param account 账号，格式为：xxx
	 * @return
	 */
	public static String wrapJid(String account) {
		if (!TextUtils.isEmpty(account)) {
			return account + "@" + Constants.SERVER_NAME;
		} else {
			return null;
		}
	}
	
	/**
	 * 将格式为xxx@domain的jid包装成格式为xxx@domain/resource的完整jid
	 * @update 2015年3月10日 下午5:45:42
	 * @param jid 简单的jid，格式为xxx@domain
	 * @param resource 资源，如Android，iphone，web
	 * @return 完整格式的jid，格式为xxx@domain/resource
	 */
	public static String wrapFullJid(String jid, String resource) {
		if (TextUtils.isEmpty(jid)) {
			return null;
		}
		return jid + "/" + resource;
	}
	
	/**
	 * 将格式为xxx@domain的jid包装成格式为xxx@domain/resource的完整jid
	 * @update 2015年3月10日 下午5:48:21
	 * @param jid 简单的jid，格式为xxx@domain
	 * @return 完整格式的jid，格式为xxx@domain/resource
	 */
	public static String wrapFullJid(String jid) {
		if (TextUtils.isEmpty(jid)) {
			return null;
		}
		return jid + "/" + Constants.CLIENT_RESOURCE;
	}
	
	/**
	 * 将完整的jid托包装为账号，格式为：xxx
	 * @update 2014年11月10日 下午8:47:14
	 * @param jid 账号，格式为：xxx@doamin
	 * @return
	 */
	public static String unwrapJid(String jid) {
		if (!TextUtils.isEmpty(jid)) {
			return jid.substring(0, jid.indexOf("@"));
		} else {
			return null;
		}
	}
	
	/**
	 * 删除文件
	 * @update 2014年11月11日 下午7:07:25
	 * @param filePath
	 */
	public static void deleteFile(String filePath) {
		if (filePath == null) {
			return;
		}
		File file = new File(filePath);
		deleteFile(file);
	}
	
	/**
	 * 删除文件
	 * @update 2014年11月11日 下午7:07:25
	 * @param file
	 */
	public static void deleteFile(File file) {
		if (file == null) {
			return;
		}
		try {
			if (file.exists()) {
				file.delete();
			}
		} catch (Exception e) {
			Log.d("---deleteFile--失败---" + e.getMessage());
		}
	}
	
	/**
	 * 通过索引位置获取listview的itemView
	 * @update 2014年11月12日 下午3:08:02
	 * @param pos
	 * @param listView
	 * @return
	 */
	public static View getViewByPosition(int pos, ListView listView) {
		final int firstListItemPosition = listView.getFirstVisiblePosition();
		final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

		if (pos < firstListItemPosition || pos > lastListItemPosition ) {
		    return listView.getAdapter().getView(pos, null, listView);
		} else {
		    final int childIndex = pos - firstListItemPosition;
		    return listView.getChildAt(childIndex);
		}
	}
	
	/**
	 * 获得listview的高度
	 * @update 2014年11月14日 下午6:15:12
	 * @param list
	 * @return
	 */
	public static int getListViewHeight(ListView list) {
        ListAdapter adapter = list.getAdapter();

        int listviewHeight = 0;

        list.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
				MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

        listviewHeight = list.getMeasuredHeight() * adapter.getCount() + (adapter.getCount() * list.getDividerHeight());

        return listviewHeight;
  }
	
	/**
	 * 根据listview的项设置listview的高度
	 * @update 2014年11月14日 下午6:13:05
	 * @param listView
	 */
	public static void setListViewHeightBasedOnChildren(ListView listView) {

	    ListAdapter mAdapter = listView.getAdapter();

	    int totalHeight = 0;

	    for (int i = 0; i < mAdapter.getCount(); i++) {
	        View mView = mAdapter.getView(i, null, listView);

	        mView.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
	                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

	        totalHeight += mView.getMeasuredHeight();

	    }
	    ViewGroup.LayoutParams params = listView.getLayoutParams();
	    params.height = totalHeight + (listView.getDividerHeight() * (mAdapter.getCount() - 1));
	    listView.setLayoutParams(params);
	    listView.requestLayout();

	}
	
	/**
	 * 判断一个view是否可见
	 * @update 2014年11月15日 上午10:40:26
	 * @param view
	 * @return
	 */
	public static boolean isViewVisible(View view) {
		return view.getVisibility() == View.VISIBLE;
	}
	
	/**
	 * 将长整型的字节单位转换成字符串，单位为KB、MB、GB
	 * @update 2014年11月15日 下午3:12:09
	 * @param size
	 * @return
	 */
	public static String sizeToString(long size) {
		long kb = 1024;
		long mb = kb * 1024;
		long gb = mb * 1024;
		String format = null;
		if (size >= gb) {
			if (size % gb == 0) {
				format = "%.0f G";
			} else {
				format = "%.2f G";
			}
			return String.format(Locale.getDefault(), format, (float) size / gb);
		} else if (size >= mb) {
			if (size % mb == 0) {
				format = "%.0f M";
			} else {
				format = "%.2f M";
			}
			float f = (float) size / mb;
			return String.format(Locale.getDefault(), format, f);
		} else if (size >= kb) {
			if (size % kb == 0) {
				format = "%.0f K";
			} else {
				format = "%.2f K";
			}
			float f = (float) size / kb;
			return String.format(Locale.getDefault(), format, f);
		} else {
			return String.format(Locale.getDefault(), "%d B", size);
		}
	}
	
	/**
	 * 获得相片集合的文件大小，并转换成字符串
	 * @update 2014年11月15日 下午3:09:34
	 * @param list
	 * @return
	 */
	public static String getFileListSizeStr(List<PhotoItem> list) {
		String sizeStr = null;
		long byteSize = 0;
		for (PhotoItem photoItem : list) {
			byteSize += photoItem.getSize();
		}
		sizeStr = sizeToString(byteSize);
		return sizeStr;
	}
	
	/**
	 * 获得相片集合的文件大小
	 * @update 2014年11月15日 下午3:09:34
	 * @param list
	 * @return
	 */
	public static long getFileListSize(List<PhotoItem> list) {
		if (SystemUtil.isEmpty(list)) {
			return 0;
		}
		long byteSize = 0;
		for (PhotoItem photoItem : list) {
			byteSize += photoItem.getSize();
		}
		return byteSize;
	}
	
	/**
	 * 根据文件名获得文件的后缀，如jpg
	 * @update 2014年11月17日 下午10:10:51
	 * @param url
	 * @return
	 */
	public static String getFileSubfix(String url) {
		if (TextUtils.isEmpty(url)) {
			return null;
		}
		int index = url.lastIndexOf(".");
		if (index != -1) {	//文件名不包含有.
			return url.substring(index + 1);
		} else {	//返回空串，直接作为文件名
			return "";
		}
	}

	/**
	 * 根据文件的全路径来判断文件是否是jpg/jpeg的文件
	 * @param filePath
	 * @return
	 */
	public static boolean isJpgFile(String filePath) {
		String subFix = getFileSubfix(filePath);
		if (TextUtils.isEmpty(subFix)) {
			return false;
		} else {
			if (subFix.equalsIgnoreCase("jpg") || subFix.equalsIgnoreCase("jpeg")) {
				return true;
			} else {
				return false;
			}
		}
	}
	
	/**
	 * 根据文件名获得文件的后缀，如.jpg
	 * @update 2014年11月17日 下午10:10:51
	 * @param file
	 * @return
	 */
	public static String getFileSubfix(File file) {
		if (file == null) {
			return null;
		}
		return getFileSubfix(file.getName());
	}
	
	/**
	 * 根据文件的名称或者路径来获得文件的真实名称
	 * @update 2014年11月19日 下午3:07:26
	 * @param filePath
	 * @return
	 */
	public static String getFilename(String filePath) {
		if (TextUtils.isEmpty(filePath)) {
			return null;
		}
		if (filePath.indexOf("/") != -1) {
			return filePath.substring(filePath.lastIndexOf("/") + 1);
		} else {
			return filePath;
		}
	}
	
	/**
	 * 根据会话id生成文件保存文件的路径，如:/mnt/sdcard/ChatApp/admin/attachment/12
	 * @update 2014年11月17日 下午9:51:15
	 * @param msgThread 当前会话
	 * @return
	 */
	public static String generateChatAttachPath(MsgThread msgThread) {
		return generateChatAttachPath(msgThread.getId());
	}
	
	/**
	 * 根据会话id生成文件保存文件的路径，如:/mnt/sdcard/ChatApp/admin/attachment/12
	 * @update 2014年11月17日 下午9:51:15
	 * @param threadId 当前会话id
	 * @return
	 */
	public static String generateChatAttachPath(int threadId) {
		String root = getDefaultRootPath();
		StringBuilder sb = new StringBuilder(root);
		sb.append(File.separator)
			.append(Constants.DATA_MSG_ATT_FOLDER_NAME)
			.append(File.separator)
			.append(threadId);
		String path = sb.toString();
		File dir = new File(path);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return path;
	}
	
	/**
	 * 根据会话id生成文件保存文件的缩略图的路径，如:/mnt/sdcard/ChatApp/admin/attachment/12/thumb
	 * @param threadId
	 * @return
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年5月3日 上午9:25:38
	 */
	public static String generateChatThumbAttachPath(int threadId) {
		String path = generateChatAttachPath(threadId) + File.separator + "thumb";
		File dir = new File(path);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return path;
	}
	
	/**
	 * 根据附件的发起者账号、当前时间戳来生成对应的文件目录，如：/mnt/sdcard/ChatApp/admin/attachment/12/434324324
	 * @update 2014年11月17日 下午10:06:38
	 * @param msgThread
	 * @param filename	文件名，可以包含完整目录，也可以不包含
	 * @return
	 */
	public static String generateChatAttachFilePath(MsgThread msgThread, String filename) {
		return generateChatAttachFilePath(msgThread.getId(), filename);
	}
	
	/**
	 * 根据附件的发起者账号、当前时间戳来生成对应的文件目录，如：/mnt/sdcard/ChatApp/admin/attachment/12/54564657657868768
	 * @update 2014年11月17日 下午10:06:38
	 * @param threadId
	 * @param filename
	 * @return
	 */
	public static String generateChatAttachFilePath(int threadId, String filename) {
		String path = generateChatAttachPath(threadId);
		StringBuilder sb = new StringBuilder(path);
		sb.append(File.separator)
		.append(filename);
		return sb.toString();
	}
	
	/**
	 * 根据附件的发起者账号、当前时间戳来生成对应的文件的缩略图的目录，如：/mnt/sdcard/ChatApp/admin/attachment/12/thumb/346675767_thumb
	 * @param threadId
	 * @return
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年5月3日 上午9:22:27
	 */
	public static String generateChatThumbAttachFilePath(int threadId, String filename) {
		String path = generateChatThumbAttachPath(threadId);
		StringBuilder sb = new StringBuilder(path);
		sb.append(File.separator)
		.append(filename);
		return sb.toString();
	}
	
	/**
	 * 根据附件的发起者账号、当前时间戳来生成对应的文件的缩略图的目录，如：/mnt/sdcard/ChatApp/admin/attachment/12/thumb/346675767_thumb
	 * @param msgThread
	 * @param filename
	 * @return
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年5月3日 上午10:32:30
	 */
	public static String generateChatThumbAttachFilePath(MsgThread msgThread, String filename) {
		return generateChatThumbAttachFilePath(msgThread.getId(), filename);
	}
	
	/**
	 * 根据附件的发起者账号、当前时间戳来生成对应的文件目录，如：/mnt/sdcard/ChatApp/admin/attachment/12/23453545
	 * @update 2014年11月17日 下午10:06:38
	 * @param msgThread
	 * @param filename	文件名，不含目录
	 * @return
	 */
	public static File generateChatAttachFile(MsgThread msgThread, String filename) {
		String path = generateChatAttachFilePath(msgThread, filename);
		if (path != null) {
			return new File(path);
		} else {
			return null;
		}
	}
	
	/**
	 * 根据附件的发起者账号、当前时间戳来生成对应的文件目录，如：/mnt/sdcard/ChatApp/admin/attachment/12/dfdgfgg.doc
	 * @update 2014年11月17日 下午10:06:38
	 * @param threadId
	 * @param filename	文件名，不含目录
	 * @return
	 */
	public static File generateChatAttachFile(int threadId, String filename) {
		String path = generateChatAttachFilePath(threadId, filename);
		if (path != null) {
			return new File(path);
		} else {
			return null;
		}
	}
	
	/**
	 * 根据附件的发起者账号、当前时间戳来生成对应的文件目录，如：/mnt/sdcard/ChatApp/admin/attachment/12/23453545
	 * @update 2014年11月17日 下午10:06:38
	 * @param msgThread
	 * @param filename	文件名，不含目录
	 * @return
	 */
	public static File generateChatThumbAttachFile(MsgThread msgThread, String filename) {
		String path = generateChatThumbAttachFilePath(msgThread, filename);
		if (path != null) {
			return new File(path);
		} else {
			return null;
		}
	}
	
	/**
	 * 根据附件的发起者账号、当前时间戳来生成对应的文件目录，如：/mnt/sdcard/ChatApp/admin/attachment/12/dfdgfgg.doc
	 * @update 2014年11月17日 下午10:06:38
	 * @param threadId
	 * @param filename	文件名，不含目录
	 * @return
	 */
	public static File generateChatThumbAttachFile(int threadId, String filename) {
		String path = generateChatThumbAttachFilePath(threadId, filename);
		if (path != null) {
			return new File(path);
		} else {
			return null;
		}
	}
	
	/**
	 * 生成聊天附件的名称，根据当前的时间戳和随机数来生成
	 * @param time
	 * @return
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年5月3日 上午10:19:45
	 */
	public static String generateChatAttachFilename(long time) {
		StringBuilder sb = new StringBuilder();
		sb.append(time)
			.append("_")
			.append(new Random().nextInt(999999));
		return sb.toString();
	}
	
	/**
	 * 生成聊天附件的名称，根据当前的时间戳和随机数来生成
	 * @param time
	 * @return
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年5月3日 上午10:19:45
	 */
	public static String generateChatThumbAttachFilename(long time) {
		return generateChatThumbAttachFilename(generateChatAttachFilename(time));
	}
	
	/**
	 * 生成聊天缩略图的名称，根据当前的时间戳和随机数来生成
	 * @param originFilename 原始图片的名称
	 * @return
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年5月3日 下午2:22:27
	 */
	public static String generateChatThumbAttachFilename(String originFilename) {
		return originFilename + "_thumb";
	}
	
	/**
	 * 生成录音文件名，文件以时间戳为为名方式，如:2014_11_24_21_41_56_5435646.amrz
	 * @update 2014年11月24日 下午9:40:15
	 * @return
	 */
	public static String generateRecordFilename() {
		return generateFilename("amrz");
	}
	
	/**
	 * 生成文件名，文件以时间戳为为名方式，如:2014_11_24_21_41_56_5435646.xxx
	 * @update 2015年2月12日 下午7:48:35
	 * @param subfix 文件名的后缀名，参数不包含.
	 * @return
	 */
	public static String generateFilename(String subfix) {
		String dot = ".";
		if (TextUtils.isEmpty(subfix)) {	//没有后缀名
			dot = "";
		}
		return formatTime(System.currentTimeMillis(), "yyyy_MM_dd_HH_mm_ss") + new Random().nextInt(999999) + dot + subfix;
	}
	
	/**
	 * 生成地理位置的截图文件
	 * @update 2015年2月12日 下午7:52:59
	 * @return
	 */
	public static String generateLocationFilename() {
		return generateFilename("jpgz");
	}
	
	/**
	 * 生成相机拍照后图片的名称，如/mnt/sdcard/ChatApp/DCIM/2014_11_24_21_41_56_543546.jpg
	 * @author tiger
	 * @update 2015年3月13日 上午12:01:59
	 * @return
	 */
	public static String generatePhotoPath() {
		return generatePhotoFile().getAbsolutePath();
	}

	/**
	 * 生成拍照图片的全路径
	 * @return
	 */
	public static File generatePhotoFile() {
		File file = generateMediaFile();
		return new File(file, generateFilename("jpg"));
	}

	/**
	 * 生成拍摄视频的全路径
	 * @return
	 */
	public static File generateVideoFile() {
		File file = generateMediaFile();
		return new File(file, generateFilename("mp4"));
	}

	/**
	 * 生成相机拍摄视频后视频文件的名称，如/mnt/sdcard/ChatApp/DCIM/2014_11_24_21_41_56_543546.mp4
	 * @author tiger
	 * @update 2015年3月13日 上午12:01:59
	 * @return
	 */
	public static String generateVideoPath() {
		return generateVideoFile().getAbsolutePath();
	}

	/**
	 * 生成多媒体文件的路径，主要是图片和视频
	 * @return
	 */
	private static File generateMediaFile() {
		StringBuilder sb = new StringBuilder();
		sb.append(getDefaultAppPath())
				.append(File.separator)
				.append("DCIM");
		File file = new File(sb.toString());
		if (!file.exists()) {
			file.mkdirs();
		}
		return file;
	}
	
	/**
	 * 还原附件的文件全名称，保存时间文件名全部加了"x"
	 * @update 2014年11月19日 下午4:53:24
	 * @param originalPath
	 * @return
	 */
	public static String resetAttachFilePath(String originalPath) {
		return originalPath.substring(0, originalPath.length() - 1);
	}
	
	/**
	 * 根据文件名或者文件路径获取对应的消息类型
	 * @update 2014年11月20日 下午5:42:26
	 * @param url 文件名称或者文件的路径
	 * @return
	 */
	public static MsgInfo.Type getMsgInfoType(String url) {
		//获得文件的后缀名，不包含".",如mp3
		String subfix = SystemUtil.getFileSubfix(url);
		//获得文件的mimetype，如image/jpeg
		String mimeType = MimeUtils.guessMimeTypeFromExtension(subfix);
		return getMsgInfoType(subfix, mimeType);
	}
	
	/**
	 * 根据文件名或者文件路径获取对应的消息类型
	 * @update 2014年11月20日 下午5:42:26
	 * @param subfix 文件的后缀，不包含“.”，如mp3
	 * @param mimeType 文件的MIME类型，如image/jpeg
	 * @return 返回对应的{@linkplain MsgInfo.Type}
	 */
	public static MsgInfo.Type getMsgInfoType(String subfix, String mimeType) {
		MsgInfo.Type type = Type.FILE;
		if (!TextUtils.isEmpty(mimeType)) {
			int prePos = mimeType.indexOf("/");
			if (prePos != -1) {
				String pre = mimeType.substring(0, prePos);
				switch (pre) {
				case Constants.MIME_IMAGE:	//图片类型
					type = Type.IMAGE;
					break;
				case Constants.MIME_AUDIO:	//音频类型
					type = Type.AUDIO;
					break;
				case Constants.MIME_VIDEO:	//视频类型
					type = Type.VIDEO;
					break;
				case Constants.MIME_TEXT:	//文本类型
					type = Type.TEXT;
					break;
				default:
					type = Type.FILE;
					break;
				}
			}
		}
		return type;
	}
	
	/**
	 * 根据完整的MIME类型获得简单的MIME类型
	 * @update 2014年11月21日 下午4:21:47
	 * @param mimeType 完整的MIME类型如：image/jpeg
	 * @return 简单的MIME类型，如image
	 */
	public static String getSimpleMimeType(String mimeType) {
		String simpleMimeType = Constants.MIME_FILE;
		if (!TextUtils.isEmpty(mimeType)) {
			int prePos = mimeType.indexOf("/");
			if (prePos != -1) {
				String pre = mimeType.substring(0, prePos);
				switch (pre) {
				case Constants.MIME_IMAGE:	//图片类型
					simpleMimeType = Constants.MIME_IMAGE;
					break;
				case Constants.MIME_AUDIO:	//音频类型
					simpleMimeType = Constants.MIME_AUDIO;
					break;
				case Constants.MIME_VIDEO:	//视频类型
					simpleMimeType = Constants.MIME_VIDEO;
					break;
				case Constants.MIME_TEXT:	//文本类型
					simpleMimeType = Constants.MIME_TEXT;
					break;
				default:
					simpleMimeType = Constants.MIME_FILE;
					break;
				}
			}
		}
		return simpleMimeType;
	}
	
	/**
	 * 根据原始图片生成本地图片的缓存
	 * @update 2014年11月19日 下午6:03:58
	 * @param bitmap
	 * @param photoItem
	 * @return
	 */
	public static boolean saveBitmap(ImageLoader imageLoader, Bitmap bitmap, PhotoItem photoItem) {
		try {
			DiskCache diskCache = imageLoader.getDiskCache();
			boolean isJpg = SystemUtil.isJpgFile(photoItem.getFilePath());
			Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.PNG;
			if (isJpg) {
				compressFormat = Bitmap.CompressFormat.JPEG;
			}
			if (diskCache instanceof LruDiskCache) {
				LruDiskCache lruDiskCache = (LruDiskCache) diskCache;
				lruDiskCache.setCompressFormat(compressFormat);
			} else if (diskCache instanceof BaseDiskCache) {
				BaseDiskCache baseDiskCache = (BaseDiskCache) diskCache;
				baseDiskCache.setCompressFormat(compressFormat);
			}
			return imageLoader.getDiskCache().save(Scheme.FILE.wrap(photoItem.getFilePath()), bitmap);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/** 采用了新的办法获取APK图标，之前的失败是因为android中存在的一个BUG,通过
	* appInfo.publicSourceDir = apkPath;来修正这个问题，详情参见:
	* http://code.google.com/p/android/issues/detail?id=9151
	*/
	public static Drawable getApkIcon(String apkPath) {
		PackageManager pm = ChatApplication.getInstance().getPackageManager();
		PackageInfo info = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
		if (info != null) {
			ApplicationInfo appInfo = info.applicationInfo;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                appInfo.sourceDir = apkPath;
                appInfo.publicSourceDir = apkPath;
            }
			try {
				return appInfo.loadIcon(pm);
			} catch (OutOfMemoryError e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	/**
	 * 根据文件获得fileitem
	 * @update 2014年11月22日 上午10:33:47
	 * @param file
	 * @return
	 */
	public static FileItem getFileItem(File file) {
		FileItem fileItem = new FileItem();
		fileItem.setFile(file);
		String ext = SystemUtil.getFileSubfix(file.getName()).toLowerCase(Locale.getDefault());
		if (!TextUtils.isEmpty(ext)) {
			if (Constants.MIME_APK.equals(ext)) {	//apk文件
				fileItem.setFileType(FileType.APK);
			} else {
				String mimeType = MimeUtils.guessMimeTypeFromExtension(ext);
				String simpleMimeType = SystemUtil.getSimpleMimeType(mimeType);
				fileItem.setFileType(FileType.valueOf(simpleMimeType.toUpperCase(Locale.getDefault())));
			}
		} else {
			fileItem.setFileType(FileType.FILE);
		}
		return fileItem;
	}
	
	/**
	 * 根据文件全路径获得fileitem
	 * @update 2014年11月22日 上午10:35:50
	 * @param filePath
	 * @return
	 */
	public static FileItem getFileItem(String filePath) {
		if (TextUtils.isEmpty(filePath)) {
			return null;
		}
		File file = new File(filePath);
		return getFileItem(file);
	}
	
	/**
	 * 根据文件的信息获得fileitem
	 * @update 2014年11月22日 上午10:40:15
	 * @param filePath
	 * @param fileName
	 * @param mimeType
	 * @return
	 */
	public static FileItem getFileItem(String filePath, String fileName, String mimeType) {
		if (TextUtils.isEmpty(filePath)) {
			return null;
		}
		File file = new File(filePath);
		FileItem fileItem = new FileItem();
		fileItem.setFile(file);
		String ext = SystemUtil.getFileSubfix(fileName).toLowerCase(Locale.getDefault());
		if (!TextUtils.isEmpty(ext)) {
			if (Constants.MIME_APK.equals(ext)) {	//apk文件
				fileItem.setFileType(FileType.APK);
			} else {
				String simpleMimeType = SystemUtil.getSimpleMimeType(mimeType);
				fileItem.setFileType(FileType.valueOf(simpleMimeType.toUpperCase(Locale.getDefault())));
			}
		} else {
			fileItem.setFileType(FileType.FILE);
		}
		return fileItem;
	}
	
	/**
	 * 根据文件来获取对应的资源
	 * @update 2014年11月22日 上午10:27:05
	 * @param fileItem  
	 * @param defaultResId 当没有匹配的资源时，显示默认的资源
	 * @return
	 */
	public static int getResIdByFile(FileItem fileItem, int defaultResId) {
		String extension = SystemUtil.getFileSubfix(fileItem.getFile().getName()).toLowerCase(Locale.getDefault());;
		Integer resId = MimeUtils.guessResIdFromExtension(extension);
		if (resId == null || resId == 0) {	//没有找到资源图片，则根据文件的mime类型来查找
			String extStr = fileItem.getFileType().name().toLowerCase(Locale.getDefault());
			resId = MimeUtils.guessResIdFromExtension(extStr);
			if (resId == null || resId == 0) {
				resId = defaultResId;
			}
		}
		return resId;
	}
	
	/**
	 * 将时长转换为字符串
	 * @update 2014年11月22日 下午3:49:23
	 * @param duration 时长,单位为毫秒
	 * @return 转换后的字符串,格式为:"mm:ss"，如："12:23"
	 */
	public static String timeToString(int duration) {
		//将毫秒转换为秒
		int second = duration / 1000;
		int unit = 60;	//时间的单位为60
		int hunit = 3600;	//小时的单位
		DecimalFormat decimalFormat = new DecimalFormat("00");
		if (second < unit) {	//少于一分钟
			return "00:" + decimalFormat.format(second);
		} else if (second < hunit) {	//多余一分钟，但少于一个小时
			int minu = second / unit;
			int sec = second % unit;
			return decimalFormat.format(minu) + ":" + decimalFormat.format(sec);
		} else {	//大于一个小时
			int hor = second / hunit;
			int msec = second % hunit;
			int minu = msec / unit;
			int sec = msec % unit;
			return decimalFormat.format(hor) + ":" + decimalFormat.format(minu) + ":" + decimalFormat.format(sec);
		}
	}
	
	/**
	 * 将时间转换成字符串，如：12'23"
	 * @update 2014年11月25日 下午5:11:03
	 * @param secondTime 秒数
	 * @return
	 */
	public static String shortTimeToString(int secondTime) {
		String minuChar = "'";
		String secChar = "\"";
		int unit = 60;	//时间的单位为60
		int hunit = 3600;	//小时的单位
		if (secondTime < unit) {	//少于一分钟
			return secondTime + secChar;
		} else if (secondTime < hunit) {	//多余一分钟，但少于一个小时
			int minu = secondTime / unit;
			int sec = secondTime % unit;
			return minu + minuChar + sec + secChar;
		} else {	//大于一个小时
			int hor = secondTime / hunit;
			int msec = secondTime % hunit;
			int minu = msec / unit;
			int sec = msec % unit;
			return hor + ":" + minu + minuChar + sec + secChar;
		}
	}
	
	/**
	 * 当前sdk版本是否在14或者之上
	 * @update 2015年1月22日 下午3:02:51
	 * @return
	 */
	public static boolean hasSDK14() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
	}
	
	/**
	 * 当前sdk版本是否在15或者之上
	 * @update 2015年1月22日 下午3:02:51
	 * @return
	 */
	public static boolean hasSDK15() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1;
	}
	
	/**
	 * 当前sdk版本是否在16或者之上
	 * @update 2015年1月22日 下午3:02:51
	 * @return
	 */
	public static boolean hasSDK16() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
	}
	
	/**
	 * 当前sdk版本是否在17或者之上
	 * @update 2015年1月22日 下午3:02:51
	 * @return
	 */
	public static boolean hasSDK17() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
	}
	
	/**
	 * 当前sdk版本是否在18或者之上
	 * @update 2015年1月22日 下午3:02:51
	 * @return
	 */
	public static boolean hasSDK18() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 ;
	}
	
	/**
	 * 当前sdk版本是否在19或者之上
	 * @update 2015年1月22日 下午3:02:51
	 * @return
	 */
	public static boolean hasSDK19() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
	}
	
	/**
	 * 当前sdk版本是否在21或者之上
	 * @update 2015年1月22日 下午3:02:51
	 * @return
	 */
	public static boolean hasSDK21() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
	}
	
	/**
	 * 根据提供的属性获得该属性值对应的资源id
	 * @update 2015年1月22日 下午6:48:30
	 * @param attr 属性
	 * @return
	 */
	public static int getResourceId(Context context, int attr) {
		TypedValue typedValue = new TypedValue();
		if (context.getTheme().resolveAttribute(attr, typedValue, true)) {
			if (typedValue.type >= TypedValue.TYPE_FIRST_INT && typedValue.type <= TypedValue.TYPE_LAST_INT) {
				return typedValue.data;
			} else if (typedValue.type == TypedValue.TYPE_STRING) {
				return typedValue.resourceId;
			} else {
				return 0;
			}
		} else {
			return 0;
		}
	}
	
	/**
	 * 根据提供的属性获得该属性值对应的资源id
	 * @update 2015年1月22日 下午6:48:30
	 * @param attr 属性
	 * @return
	 */
	public static int getResourceId(Context context, int defStyleAttr, int attr) {
		TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, null, new int[] {attr}, defStyleAttr, 0);
		if (a != null) {
			return a.getResourceId(0, 0);
		}
		return 0;
	}
	
	/**
	 * 根据表情的数量计算表情的页数
	 * @update 2015年1月26日 下午9:31:45
	 * @param total
	 * @return
	 */
	public static int getEmojiPageCount(int total) {
		if (total % Constants.PAGE_SIZE_EMOJI == 0) {	//能整除
			return total / Constants.PAGE_SIZE_EMOJI;
		} else {
			return (int) Math.ceil(total / Constants.PAGE_SIZE_EMOJI + 0.1);
		}
	}
	
	/**
	 * 根据第几页的索引位置获得该页的表情数组
	 * @update 2015年1月26日 下午9:36:00
	 * @param data 表情数组
	 * @param index 页数的索引，从0开始
	 * @return
	 */
	public static ArrayList<Emojicon> getCurrentPageEmojis(Emojicon[] data, int index) {
		return getCurrentPageEmojis(Arrays.asList(data), index);
	}
	
	/**
	 * 根据第几页的索引位置获得该页的表情数组
	 * @update 2015年1月26日 下午9:36:00
	 * @param data 表情数组
	 * @param index 页数的索引，从0开始
	 * @return
	 */
	public static ArrayList<Emojicon> getCurrentPageEmojis(List<Emojicon> data, int index) {
		ArrayList<Emojicon> subList = new ArrayList<>();
		if (SystemUtil.isEmpty(data)) {
			return subList;
		}
		int startIndex = index * Constants.PAGE_SIZE_EMOJI;
		int endIndex = startIndex + Constants.PAGE_SIZE_EMOJI;
		int size = data.size();
		endIndex = endIndex > size ? size : endIndex;
		subList.addAll(data.subList(startIndex, endIndex));
		return subList;
	}
	
	/**
	 * 将数组转换成list,该list可改变大小
	 * @update 2015年2月15日 下午8:38:01
	 * @param array
	 */
	public static <T> List<T> asList(T[] array) {
		if (isNotEmpty(array)) {
			List<T> list = new ArrayList<>();
			for (T t : array) {
				list.add(t);
			}
			return list;
		} else {
			return null;
		}
	}
	
	/**
	 * 复制文本内容到剪切板
	 * @update 2015年2月26日 上午10:54:06
	 * @param text 要复制的文本内容
	 * @param label 文本内容映射到剪切板的lable
	 */
	public static void copyText(String text, String label) {
		ClipboardManager clipboard = (ClipboardManager) ChatApplication.getInstance().getSystemService(Context.CLIPBOARD_SERVICE);
		if (TextUtils.isEmpty(label)) {
			label = Constants.LABEL_COPY;
		}
		ClipData clip = ClipData.newPlainText(label, text);
	    clipboard.setPrimaryClip(clip);
	    makeShortToast(R.string.copy_text_success);
	}
	
	/**
	 * 复制文本内容到剪切板
	 * @update 2015年2月26日 上午10:54:06
	 * @param text 要复制的文本内容
	 */
	public static void copyText(String text) {
		copyText(text, null);
	}
	
	/**
	 * 获取数据存数的父目录。为：/data/data/packagename/
	 * @author tiger
	 * @update 2015年3月8日 下午7:25:11
	 * @return
	 */
	public static File getDataParentFile() {
		File parentFile = ChatApplication.getInstance().getFilesDir().getParentFile();
		return parentFile;
	}
	
	/**
	 * 获得存放数据库的根目录，默认目录：/data/data/packname/IbaiXinChat/
	 * @update 2015年3月9日 下午8:59:59
	 * @return
	 */
	public static File getBaseDBDir() {
		return new File(getDataParentFile().getAbsolutePath(), Constants.DEAULT_APP_FOLDER_NAME);
	}
	
	/**
	 * 根据账号判断该账号是否有数据文件，主要是数据库文件
	 * @author tiger
	 * @update 2015年3月8日 下午7:21:25
	 * @param account 账号
	 * @return
	 */
	public static boolean hasDataFile(String account) {
		File parentFile = getDataParentFile();
		String encodeStr = encoderByMd5(account);
		if (!TextUtils.isEmpty(encodeStr)) {
			File targetFile = new File(parentFile.getAbsolutePath() + encodeStr, Constants.DEAULT_APP_FOLDER_NAME);
			return targetFile.exists();
		} else {
			return false;
		}
	}
	
	/**
	 * 将字符创进行MD5加密
	 * @author tiger
	 * @update 2015年3月8日 下午6:44:03
	 * @param str 待加密的字符串
	 * @return 加密后的字符串
	 */
	public static String encoderByMd5(String str) {
		if (TextUtils.isEmpty(str)) {
			return null;
		}
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.update(str.getBytes("UTF-8"));
			byte[] hash = digest.digest();
			StringBuilder hexString = new StringBuilder();
			for (int i = 0; i < hash.length; i++) {
				if ((0xff & hash[i]) < 0x10) {
					hexString.append(0).append(Integer.toHexString((0xFF & hash[i])));
				} else {
					hexString.append(Integer.toHexString(0xFF & hash[i]));
				}
			}
			return hexString.toString(); 
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 获取文件的MD5值
	 * @param file
	 * @return
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年5月1日 下午4:38:41
	 */
	public static String encoderFileByMd5(File file) {
		String result = null;
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			MappedByteBuffer byteBuffer = fis.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update(byteBuffer);
			BigInteger bi = new BigInteger(1, md5.digest());
			result = bi.toString(16);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (null != fis) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
        return result;
    }
	
	/**
	 * 获取文件的MD5值
	 * @param filePath
	 * @return
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年5月1日 下午4:38:41
	 */
	public static String encoderFileByMd5(String filePath) {
		File file = new File(filePath);
		return encoderFileByMd5(file);
	}
	
	/**
	 * 初始化置顶账号的数据库路径
	 * @update 2015年3月10日 下午3:39:47
	 * @param account
	 */
	public static void initAccountDbDir(String account) {
		
		if (TextUtils.isEmpty(account)) {
			ChatApplication app = ChatApplication.getInstance();
			SharedPreferences preferences = app.getSharedPreferences(Constants.SETTTING_LOGIN, Context.MODE_PRIVATE);
			account = preferences.getString(Constants.USER_ACCOUNT, "");
			app.setCurrentAccount(account);
		}
		Log.d("------initAccountDbDir---account---" + account);
		//如果该账号没有对应的数据库，则根据账号创建对应的数据库，并设置当前的账号数据库
		String accountMd5 = SystemUtil.encoderByMd5(account);
		File dbDir = new File(SystemUtil.getBaseDBDir(), accountMd5);
		if (!dbDir.exists()) {
			dbDir.mkdirs();
		}
		ChatApplication app = ChatApplication.getInstance();
		app.setAccountDbDir(dbDir.getAbsolutePath());
	}

	/**
	 * 获取公共文件文件路径(add by ddj)
	 * @return
	 */
	public static String getPublicFilePath(){
		String dirs = SystemUtil.getSDCardRootPath()+File.separator+Constants.DEAULT_APP_FOLDER_NAME+"/public/" ;
		File f = new File(dirs);
		if(!f.exists() && !f.mkdirs()){
			return null ;
		}else{
			return dirs ;
		}
	}
	/**
	 * 获取webview緩存文件路径(add by ddj)
	 * @return
	 */
	public static String getWebViewPath(){
		String dirs = SystemUtil.getPublicFilePath()+"/webcache/" ;
		File f = new File(dirs);
		if(!f.exists() && !f.mkdirs()){
			return null ;
		}else{
			return dirs ;
		}
	}
	
	/**
	 * 扫描指定的文件
	 * @update 2015年3月13日 上午11:22:00
	 * @param context
	 * @param filePath
	 */
	public static void scanFileAsync(Context context, String filePath) {
		scanFileAsync(context, new File(filePath));
	}
	
	/**
	 * 扫描指定的文件
	 * @update 2015年3月13日 上午11:22:53
	 * @param context
	 * @param file
	 */
	public static void scanFileAsync(Context context, File file) {
		if (file != null && file.exists()) {
			Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
			scanIntent.setData(Uri.fromFile(file));
			context.sendBroadcast(scanIntent);
		}
	}
	
	/**
	 * 扫描指定的文件夹
	 * @update 2015年3月13日 上午11:24:56
	 * @param context
	 * @param dir
	 */
    public static void scanDirAsync(Context context, String dir) {
	    Intent scanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_DIR");
	    scanIntent.setData(Uri.fromFile(new File(dir)));
	    context.sendBroadcast(scanIntent);
    }
    
    /**
	 * 根据文件夹位置对该文件夹及其内容全部删除
	 * @param path 路径
	 */
	public static void deletFiles(String path) {
		File dir = new File(path);
		if (dir.exists() && dir.isDirectory()) {
			File files[] = dir.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deletFiles(files[i].getAbsoluteFile().toString());
				} else
					files[i].delete();
			}
		}
//		dir.delete();
	}
	
	/**
	 * 检测当前的网络连接是否是WiFi
	 * @param context
	 * @return
	 */
	public static boolean isWifi(Context context) {
		if (context == null) {
			return false;
		}
		ConnectivityManager manager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = manager.getActiveNetworkInfo();

		return info == null ? false : (info.getType() ==
				ConnectivityManager.TYPE_WIFI ? true : false);
	}
	
	/* get uuid without '-' */
	private static String getUuid() {
		return UUID.randomUUID().toString().trim().replaceAll("-", "");
	}

	/* caculate md5 for string */
	private static String md5(String origin) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(origin.getBytes("UTF-8"));
			BigInteger bi = new BigInteger(1, md.digest());

			return bi.toString(16);
		} catch (Exception e) {
			return getUuid();
		}
	}
	
	/**
	 * md516位加密
	 * @param str
	 * @return
	 * @update 2015年9月17日 上午11:02:14
	 */
	public static String md516(String str) {
        String result = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            byte b[] = md.digest();
            int i;
            StringBuilder buf = new StringBuilder("");
            for (int offset = 0; offset < b.length; offset++) {
                i = b[offset];
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            result = buf.toString().substring(8, 24);
        } catch (NoSuchAlgorithmException e) {
            Log.e(e.getMessage());
        }
        return result;
	}
	
	/**
	 * Get filename from url.
	 *
	 * @param  url url
	 * @return     filename or null if no available filename
	 */
	public static String getFilenameFromUrl(String url) {
		String filename = md5(url) + ".down";

		int index = url.lastIndexOf("/");
		if (index > 0) {
			String tmpFilename = url.substring(index);
			int qmarkIndex = tmpFilename.indexOf("?");
			if (qmarkIndex > 0) {
				tmpFilename = tmpFilename.substring(0, qmarkIndex - 1);
			}

			/* if filename contains '.', then the filename has file extension */
			if (tmpFilename.contains(".")) {
				filename = tmpFilename;
			}
		}

		return filename;
	}

	/**
	 * Get real filename from http header.
	 *
	 * @param  downloadUrl the url to download
	 * @return             real filename
	 */
	public static String getFilenameFromHeader(String downloadUrl) {
		String filename = md5(downloadUrl) + ".down";
		HttpURLConnection conn = null;
		try {
			URL url = new URL(downloadUrl);
			conn = (HttpURLConnection) url.openConnection();
			conn.setInstanceFollowRedirects(false);

			int statusCode = conn.getResponseCode();
			switch (statusCode) {
			case 301:
			case 302:
			case 303:
			case 307:
				String location = conn.getHeaderField("Location");
				/* avoid to much redirection */
				if (REDIRECT_TIME.addAndGet(1) > 5 || TextUtils.isEmpty(location)) {
					filename = getFilenameFromUrl(downloadUrl);
				} else {
					filename = getFilenameFromHeader(location);
				}
				break;

			case 200:
			default:
				/* try to get filename from content disposition */
				String contentDispos = conn.getHeaderField("Content-Disposition");
				if (!TextUtils.isEmpty(contentDispos)) {
					int index = contentDispos.indexOf("filename");
					if (index > 0) {
						filename = contentDispos.substring(
								index + 10, contentDispos.length() - 1);
					} else {
						filename = getFilenameFromUrl(downloadUrl);
					}
				} else {
					filename = getFilenameFromUrl(downloadUrl);
				}
				break;
			}
		} catch (IOException e) {
			return filename;
		} finally {
			if (conn != null) {
				conn.disconnect();
			}

			REDIRECT_TIME.set(5);
		}

		try {
			filename = URLDecoder.decode(filename, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			/* ignore */
		}

		return filename;
	}
	
	/**
	 * Generate a value suitable for use in {@link #View.setId(int)}.
	 * This value will not collide with ID values generated at build time by aapt for R.id.
	 *
	 * @return a generated ID value
	 */
	private static int generateViewId() {
	    for (;;) {
	        final int result = sNextGeneratedId.get();
	        // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
	        int newValue = result + 1;
	        if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
	        if (sNextGeneratedId.compareAndSet(result, newValue)) {
	            return result;
	        }
	    }
	}
	
	/**
	 * 生成view的id
	 * @update 2015年6月27日 上午10:04:53
	 * @return
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	public static int generateId() {
		if (hasSDK17()) {
			return View.generateViewId();
	    } else {
	    	return generateViewId();
	    }
	}
	
	/**
	 * 获取上传文件的请求
	 * @param url 请求url
	 * @param files 上传的文件数组
	 * @param params 额外的参数
	 * @param responseListener 服务器返回的监听器
	 * @param errorListener 请求错误的监听器
	 * @param tag 任务队列的唯一标识，可用于取消该队列
	 * @param handler 刷新界面的handler
	 * @return 返回文件请求的request
	 * @author tiger
	 * @version 1.0.0
	 * @update 2015年5月1日 下午5:25:54
	 */
	public static MultiPartStringRequest getUploadFileRequest(final String url,
			final Map<String, File[]> files, final Map<String, String> params,
			final Listener<String> responseListener, final ErrorListener errorListener,
			final Object tag, Handler handler) {
		if (null == url || null == responseListener) {
			return null;
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
		
		multiPartRequest.setHandler(handler);
//		multiPartRequest.setTargetView(tvProgress);
//		multiPartRequest.setProgressCallback(new ProgressUpdateCallback() {
//			
//			@Override
//			public void setProgressUpdateStatus(int value) {
//				
//			}
//		});
		Log.d(" volley post : uploadFile " + url);

		return multiPartRequest;
	}
	
	/**
     * 判断字符串是否是乱码
     *
     * @param strName 字符串
     * @return 是否是乱码
     */
    public static boolean isMessyCode(String strName) {
        Pattern p = Pattern.compile("\\s*|t*|r*|n*");
        Matcher m = p.matcher(strName);
        String after = m.replaceAll("");
        String temp = after.replaceAll("\\p{P}", "");
        char[] ch = temp.trim().toCharArray();
        float chLength = ch.length;
        float count = 0;
        for (int i = 0; i < ch.length; i++) {
            char c = ch[i];
            if (!Character.isLetterOrDigit(c)) {
                if (!isChinese(c)) {
                    count = count + 1;
                }
            }
        }
        float result = count / chLength;
        if (result > 0.4) {
            return true;
        } else {
            return false;
        }
    }

	/**
	 * 判断字符是否是中文
	 *
	 * @param c 字符
	 * @return 是否是中文
	 */
	public static boolean isChinese(char c) {
		Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
		if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
				|| ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
				|| ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
				|| ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
				|| ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
				|| ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
			return true;
		}
		return false;
	}

	/**
	 * 检测某个权限是否可用
	 *
	 * @param context
	 * @param permission
	 * @return
	 * @update 2015年9月14日 下午7:25:07
	 */
	public static boolean isPermissionEnable(Context context, String permission) {
		String pkgName = context.getPackageName();
		// 结果为0则表示使用了该权限，-1则表求没有使用该权限
		int reslut = context.getPackageManager().checkPermission(permission, pkgName);
		return reslut == 0;
	}

	/**
	 * 获取uuid
	 * @return uuid
	 */
	public static String generateUUID() {
		return UUID.randomUUID().toString();
	}

	/***
	 * 判断该数据中是否有该对象，注：没有采用二分法查找，只是普通的遍历
	 * @param array 对象数组
	 * @param obj 要查找的对象
	 * @param <T> 对象泛型
	 * @return 返回是否包含该对象
	 * @author tiger
	 * @update 2015/11/7 11:33
	 * @version 1.0.0
	 */
	public static <T> boolean hasObjInArray(T[] array, T obj) {
		if (isEmpty(array) || obj == null) {
			return false;
		} else {
			int len = array.length;
			boolean has = false;
			for (int i = 0; i < len; i++) {
				if (obj.equals(array[i])) {
					has = true;
					break;
				}
			}
			return has;
		}
	}
}
