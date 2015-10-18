package net.ibaixin.chat.util;

import java.util.ArrayList;

import net.ibaixin.chat.util.Observer.NotifyType;

/**
 * 被观察者
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年3月9日 上午11:56:49
 */
public abstract class Observable<T> {
	protected final ArrayList<T> mObservers = new ArrayList<T>();
	
	/**
	 * 添加观察者
	 * @update 2015年3月9日 上午11:58:08
	 * @param observer
	 */
	public void addObserver(T observer) {
		if (observer == null) {
            throw new IllegalArgumentException("The observer is null.");
        }
        synchronized(mObservers) {
            if (mObservers.contains(observer)) {
                throw new IllegalStateException("Observer " + observer + " is already registered.");
            }
            mObservers.add(observer);
        }
	}
	
	/**
	 * 删除观察者
	 * @update 2015年3月9日 上午11:59:28
	 * @param observer
	 */
	public synchronized void removeObserver(Observer observer) {
		if (observer == null) {
            throw new IllegalArgumentException("The observer is null.");
        }
        synchronized(mObservers) {
            int index = mObservers.indexOf(observer);
            if (index == -1) {
                throw new IllegalStateException("Observer " + observer + " was not registered.");
            }
            mObservers.remove(index);
        }
    }
	
	/**
	 * 删除所有的观察者
	 * @update 2015年3月9日 下午12:27:55
	 */
	public synchronized void removeObservers() {
		synchronized(mObservers) {
            mObservers.clear();
        }
    }
	
	/**
	 * 通知所有的观察者
	 * @update 2015年3月9日 下午12:42:07
	 * @param notifyFlag 通知标识，观察者根据次标识来决定是否更新
	 * @param updateType
	 */
	public void notifyObservers(int notifyFlag, NotifyType notifyType) {
        notifyObservers(notifyFlag, notifyType, null);
    }
	
	/**
	 * 通知所有的观察者
	 * @update 2015年3月9日 下午12:42:28
	 * @param updateType
	 * @param data
	 */
	public void notifyObservers(int notifyFlag, NotifyType notifyType, Object data) {
        int size = 0;
        Observer[] arrays = null;
        synchronized (this) {
            size = mObservers.size();
            arrays = new Observer[size];
            mObservers.toArray(arrays);
        }
        if (arrays != null) {
            for (Observer observer : arrays) {
                observer.dispatchUpdate(this, notifyFlag, notifyType, data);
            }
        }
    }
}
