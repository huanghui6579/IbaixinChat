package net.ibaixin.chat.update;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import net.ibaixin.chat.ChatApplication;
import net.ibaixin.chat.R;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.StreamTool;
import net.ibaixin.chat.util.SystemUtil;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
/**
 * 应用更新
 * @author dudejin
 *
 */
public class UpdateManager {

	private Context mContext;

	// private String version ;

	private Dialog noticeDialog;

	private Dialog downloadDialog;
	/* 下载包安装路径 */
	private static String savePath = Environment.getExternalStorageDirectory() + "/";

	private static final String saveFileName = SystemUtil.getSDCardRootPath() + "/IbaixinChat.apk";
	/******* down APP name ******/
	public static final String appName = "百信";
	/* 进度条与通知ui刷新的handler和msg常量 */
	private ProgressBar mProgress;

	private static final int DOWN_UPDATE = 1;

	private static final int DOWN_OVER = 2;

	private int progress;

	private Thread downLoadThread;

	private boolean interceptFlag = false;

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case DOWN_UPDATE:
				mProgress.setProgress(progress);
				break;
			case DOWN_OVER:
				installApk();
				break;
			default:
				break;
			}
		};
	};

	public UpdateManager(Context context) {
		this.mContext = context;
	}

	// 外部接口让主Activity调用
	public void checkUpdateInfo() {
		showNoticeDialog();
	}

	private void showNoticeDialog() {
		Builder builder = new Builder(mContext);
		builder.setTitle("发现新版本");
		builder.setMessage("建议您立即更新版本");
		builder.setPositiveButton("确定", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				String sdStatus = Environment.getExternalStorageState();
				if (!sdStatus.equals(Environment.MEDIA_MOUNTED)) { // 检测sd是否可用
					/*
					 * Log.v("TestFile",
					 * "SD card is not avaiable/writeable right now.");
					 * Toast.makeText(mContext, "内存卡不可用,无法更新",
					 * Toast.LENGTH_LONG).show(); Activity ac =
					 * (Activity)mContext; Intent it = new
					 * Intent(ac,LoginActivity.class); ac.startActivity(it);
					 * ac.finish();
					 */
					savePath = mContext.getFilesDir().getAbsolutePath();
					return;
				}
				startDownLoad();
//				showDownloadDialog();
			}
		});
		builder.setNegativeButton("取消", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// ChatApplication.getInstance().exit();
				noticeDialog.dismiss();
			}
		});
		noticeDialog = builder.create();
		noticeDialog.getWindow().setType(
				WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		noticeDialog.setCancelable(false);
		noticeDialog.show();
	}

	private void startDownLoad(){
		Intent i = new Intent(mContext, UpdateService.class);
		i.putExtra("Key_App_Name", appName);
		i.putExtra("Key_Down_Url", Constants.apkDownloadUrl);
		mContext.startService(i);
	}

	private void showDownloadDialog() {
		Builder builder = new Builder(mContext);
		builder.setTitle("安装文件下载中，请稍后...");
		final LayoutInflater inflater = LayoutInflater.from(mContext);
		View v = inflater.inflate(R.layout.progress, null);
		mProgress = (ProgressBar) v.findViewById(R.id.progress);
		builder.setView(v);
		builder.setNegativeButton("取消", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				interceptFlag = true;
				// ChatApplication.getInstance().exit();
				/*
				 * //点击了取消结束下载进程，然后再去登陆 Activity ac = (Activity)mContext; Intent
				 * it = new Intent(ac,LoginActivity.class);
				 * ac.startActivity(it); ac.finish();
				 */
			}
		});
		downloadDialog = builder.create();
		downloadDialog.getWindow().setType(
				WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		downloadDialog.setCancelable(false);
		downloadDialog.show();
		downloadApk();
	}

	private Runnable mdownApkRunnable = new Runnable() {
		@Override
		public void run() {
			try {
				URL url = new URL(Constants.apkDownloadUrl);

				HttpURLConnection conn = (HttpURLConnection) url
						.openConnection();
				conn.connect();
				int length = conn.getContentLength();
				InputStream is = conn.getInputStream();

				File file = new File(savePath);
				if (!file.exists()) {
					file.mkdir();
				}
				String apkFile = saveFileName;
				File ApkFile = new File(apkFile);
				System.out.println("ApkFile" + ApkFile.exists());
				FileOutputStream fos = new FileOutputStream(ApkFile);

				int count = 0;
				byte buf[] = new byte[1024];

				do {
					int numread = is.read(buf);
					count += numread;
					progress = (int) (((float) count / length) * 100);
					// 更新进度
					mHandler.sendEmptyMessage(DOWN_UPDATE);
					if (numread <= 0) {
						// 下载完成通知安装
						mHandler.sendEmptyMessage(DOWN_OVER);
						break;
					}
					fos.write(buf, 0, numread);
				} while (!interceptFlag);// 点击取消就停止下载.

				fos.close();
				is.close();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	};

	/**
	 * 下载apk
	 *
	 * @param url
	 */
	private void downloadApk() {
		downLoadThread = new Thread(mdownApkRunnable);
		downLoadThread.start();
	}

	/**
	 * 安装apk
	 *
	 * @param url
	 */
	private void installApk() {
		File apkfile = new File(saveFileName);
		if (!apkfile.exists()) {
			return;
		}
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setDataAndType(Uri.parse("file://" + apkfile.toString()), "application/vnd.android.package-archive");
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
		mContext.startActivity(i);
		// 安装的时候也关闭 程序的界面免得在安装的时候点击了取消 后出现程序中断 无法进行下步操作
//		Activity ac = (Activity) mContext;
//		ac.finish();
		// SaveParam.saveVersion(this.mContext, version);
		ChatApplication.getInstance().exit();
	}

	/**
	 * 查询服务器软件最新版本编号,是最新版本返回true,否则返回false
	 *
	 * @return
	 */
	public static boolean checkSoftVersionIsLast() {
		String data = StreamTool.connectServer(Constants.softVersionUrl);
		if (data == null || Constants.SOFTVERSION.equals(data))
			return true;
		else
			return false;

	}
}