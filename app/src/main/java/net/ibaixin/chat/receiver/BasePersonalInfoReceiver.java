package net.ibaixin.chat.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 更新个人信息的父类广播
 * @author tiger
 * @version 2015年3月15日 下午2:34:45
 */
public abstract class BasePersonalInfoReceiver extends BroadcastReceiver {
	public static final String ACTION_REFRESH_PERSONAL_INFO = "net.ibaixin.chat.REFRESH_PERSONAL_INFO_RECEIVER";
}
