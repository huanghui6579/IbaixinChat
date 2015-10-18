package net.ibaixin.chat.util;

import android.os.Handler;

/**
 * 观察者
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年3月10日 下午1:55:02
 */
public abstract class ContentObserver implements Observer {
	private Handler mHandler;
	
	public ContentObserver(Handler handler) {
		this.mHandler = handler;
	}

	@Override
	public abstract void update(Observable<?> observable, int notifyFlag, NotifyType notifyType, Object data);
	
	class NotificationRunnable implements Runnable {
		private Observable<?> observable;
		private int notifyFlag;
		private NotifyType notifyType;
		private Object data;

		public NotificationRunnable(Observable<?> observable, int notifyFlag, NotifyType notifyType, Object data) {
			super();
			this.observable = observable;
			this.notifyFlag = notifyFlag;
			this.notifyType = notifyType;
			this.data = data;
		}

		@Override
		public void run() {
			update(observable, notifyFlag, notifyType, data);
		}
		
	}

	@Override
	public void dispatchUpdate(Observable<?> observable, int notifyFlag, NotifyType notifyType,
			Object data) {
		if (mHandler != null) {
			mHandler.post(new NotificationRunnable(observable, notifyFlag, notifyType, data));
		} else {
			update(observable, notifyFlag, notifyType, data);
		}
	}

}
