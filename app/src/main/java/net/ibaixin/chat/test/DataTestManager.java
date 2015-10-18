package net.ibaixin.chat.test;

import net.ibaixin.chat.util.Observable;
import net.ibaixin.chat.util.Observer;
import net.ibaixin.chat.util.Observer.NotifyType;
import android.util.Log;

/**
 * 
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年3月9日 下午12:37:16
 */
public class DataTestManager extends Observable<Observer> {
	private static final String TAG = "TestActivity";
	
	private static DataTestManager instance = null;
	private DataTestManager() {}
	
	public static DataTestManager getInstance() {
		if (instance == null) {
			instance = new DataTestManager();
		}
		return instance;
	}
	
	public void add(String str) {
		Log.d(TAG, "-------------add--------" + str);
		notifyObservers(1, NotifyType.ADD, str);
	}
}
