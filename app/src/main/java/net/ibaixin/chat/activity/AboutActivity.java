package net.ibaixin.chat.activity;

import net.ibaixin.chat.R;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;

/**
 * 关于界面
 * @author dudejin
 * @version 1.0.0
 * @update 2015年3月17日
 */
public class AboutActivity extends BaseActivity {
	private String sysVersion = null;
	TextView sversion = null;
	TextView version = null;
	TextView imei = null;
	@Override
	protected boolean isHomeAsUpEnabled() {
		return true;
	}
	
	@Override
	protected int getContentView() {
		return R.layout.activity_about;
	}

	@Override
	protected void initView() {
		version = (TextView)findViewById(R.id.version);
		sversion = (TextView)findViewById(R.id.sversion);
		imei = (TextView)findViewById(R.id.imei);
	}

	@Override
	protected void initData() {
		PackageManager manager = this.getPackageManager();
		try {
			PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
//			Commondata.VERSION = info.versionName;
//			version.setText("软件版本: " + Commondata.VERSION);
			sysVersion = android.os.Build.VERSION.RELEASE;
			sversion.setText("系统版本: Android "+info.versionName);
			TelephonyManager tm = (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);
			imei.setText("设备IMEI号: " + tm.getDeviceId());
		}
		catch (NameNotFoundException e)
		{
			Log.e(TAG, e.toString());
		}
	}

	@Override
	protected void addListener() {
	}

}
