package net.ibaixin.chat;

import android.content.Context;

import net.ibaixin.chat.util.Log;

/**
 * @author huanghui1
 * @update 2015/12/24 9:40
 * @version: 0.0.1
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = CrashHandler.class.getSimpleName();

    private static CrashHandler mInstance; // 单例模式

    private Thread.UncaughtExceptionHandler mDefalutHandler; // 系统默认的UncaughtException处理类
    
    private Context mContext;
    
    private CrashHandler() {}
    
    public static CrashHandler getInstance() {
        if (mInstance == null) {
            synchronized (CrashHandler.class) {
                if (mInstance == null) {
                    mInstance = new CrashHandler();
                }
            }
        }
        return mInstance;
    }
    
    /**
     * 初始化
     * @author huanghui1
     * @update 2015/12/24 9:46
     * @version: 0.0.1
     */
    public void init(Context context) {
        this.mContext = context;
        mDefalutHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }
    
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!handleException(ex) && mDefalutHandler != null) {
            mDefalutHandler.uncaughtException(thread, ex);
        } else {    //退出应用
            ChatApplication.getInstance().exit(false);
            //使用Toast来显示异常信息  
            /*new Thread() {
                @Override
                public void run() {
                    Looper.prepare();
//                    Toast.makeText(mContext, "很抱歉,程序出现异常,即将退出.", Toast.LENGTH_LONG).show();

                    MaterialDialog.Builder builder = new MaterialDialog.Builder(mContext);
                    MaterialDialog dialog = builder.title(R.string.prompt)
                            .content(R.string.app_exception_msg)
                            .positiveText(android.R.string.ok)
                            .cancelable(false)
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    ChatApplication.getInstance().exit(false);
                                }
                            })
                            .build();
                    dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                    dialog.show();
                    
                    Looper.loop();
                }
            }.start();*/
            /*MaterialDialog.Builder builder = new MaterialDialog.Builder(mContext);
            MaterialDialog dialog = builder.title(R.string.prompt)
                    .content(R.string.app_exception_msg)
                    .positiveText(android.R.string.ok)
                    .cancelable(false)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            ChatApplication.getInstance().exit(false);
                        }
                    })
                    .build();
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            dialog.show();*/

//            Intent intent = new Intent(mContext, TestActivity.class);
//            mContext.startActivity(intent);
            
//            ChatApplication.getInstance().exit(false);
            /*new Thread(new Runnable() {
                @Override
                public void run() {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            MaterialDialog.Builder builder = new MaterialDialog.Builder(mContext);
                            MaterialDialog dialog = builder.title(R.string.prompt)
                                    .content(R.string.app_exception_msg)
                                    .positiveText(android.R.string.ok)
                                    .cancelable(false)
                                    .callback(new MaterialDialog.ButtonCallback() {
                                        @Override
                                        public void onPositive(MaterialDialog dialog) {
                                            ChatApplication.getInstance().exit(false);
                                        }
                                    })
                                    .build();
                            dialog.show();
                        }
                    });
                }
            }).start();*/
            /*mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    SystemUtil.makeShortToast("应用退出");
                    ChatApplication.getInstance().exit(false);
                }
            }, 3000);*/
            /*Intent crashedIntent = new Intent(mContext, TestActivity.class);
            crashedIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            crashedIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mContext.startActivity(crashedIntent);*/
//            ChatApplication.getInstance().exit(false);
            /*mHandler.post(new Runnable() {
                @Override
                public void run() {
                    MaterialDialog.Builder builder = new MaterialDialog.Builder(mContext);
                    MaterialDialog dialog = builder.title(R.string.prompt)
                            .content(R.string.app_exception_msg)
                            .positiveText(android.R.string.ok)
                            .cancelable(false)
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    ChatApplication.getInstance().exit(false);
                                }
                            })
                            .build();
                    dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                    dialog.show();
                }
            });*/
            
        }
    }
    
    /**
     * 处理异常
     * @param ex 异常
     * @author huanghui1
     * @update 2015/12/24 9:48
     * @version: 0.0.1
     * @return 是否处理成功
     */
    public boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        Log.e("--CrashHandler----handleException----", ex);
        return true;
    }
}
