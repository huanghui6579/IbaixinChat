package net.ibaixin.chat.update;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.net.MalformedURLException;

/***
 * 软件升级服务
 * 
 * @author dudejin
 * 
 */
public class UpdateService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /*public static final String Install_Apk = "Install_Apk";
    *//******** download progress step *********//*
    private static final int down_step_custom = 1;

    private static final int TIMEOUT = 10 * 1000;// 超时
    private static String down_url;
    private static final int DOWN_OK = 1;
    private static final int DOWN_ERROR = 0;

    private String app_name;

    private NotificationManager notificationManager;
    private Notification notification;
    private Intent updateIntent;
    private PendingIntent pendingIntent;
    private RemoteViews contentView;
    public static final String FLAG_SYNC = "flag_sync";
    private Context mContext ;
    *//**
     * 软件版本更新
     *//*
    public static final int FLAG_UPDATESOFT = 3;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }

    *//**
     * 方法描述：onStartCommand方法
     * 
     * @param Intent
     *            intent, int flags, int startId
     * @return int
     * @see UpdateService
     *//*
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            //监听消息
            int syncFlag = intent.getIntExtra(FLAG_SYNC, 0);
            switch (syncFlag) {
                case FLAG_UPDATESOFT://更新软件本
                    UpdateManager um = new UpdateManager(mContext);
                    um.checkUpdateInfo();
                    break ;
                default:
                    app_name = intent.getStringExtra("Key_App_Name");
                    down_url = intent.getStringExtra("Key_Down_Url");
                    // create file,应该在这个地方加一个返回值的判断SD卡是否准备好，文件是否创建成功，等等！
                    FileUtil.createFile(app_name);
                    if (FileUtil.isCreateFileSucess == true) {
                        createNotification();
                        createThread();
                    } else {
                        SystemUtil.makeLongToast("抱歉,无法读写手机内部存储和SD卡");
                        *//*************** stop service ************//*
                        stopSelf();
                    }
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    *//********* update UI ******//*
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case DOWN_OK:

                *//********* 下载完成，点击安装 ***********//*
                Uri uri = Uri.fromFile(FileUtil.updateFile);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "application/vnd.android.package-archive");
                pendingIntent = PendingIntent.getActivity(UpdateService.this, 0, intent, 0);

                notification.flags = Notification.FLAG_AUTO_CANCEL;

                NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplication());
                builder.setSmallIcon(R.drawable.ic_launcher)
                        .setAutoCancel(true)
                        .setShowWhen(true)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setAutoCancel(true)
                        .setTicker("百信更新包下载完成")
                        .setContentTitle("下载完成")
                        .setContentText("百信更新包下载完成,点击安装")
                        .setPriority(NotificationCompat.PRIORITY_HIGH);
                builder.setContentIntent(pendingIntent);
//                notification.setLatestEventInfo(UpdateService.this, app_name, "下载成功", pendingIntent);
                // notification.setLatestEventInfo(UpdateService.this,app_name,
                // app_name + getString(R.string.down_sucess), null);
                notificationManager.notify(R.layout.notification_item, builder.build());

                *//***** 安装APK ******//*
                 installApk();

                // stopService(updateIntent);
                *//*** stop service *****//*
                stopSelf();
                break;

            case DOWN_ERROR:
                notification.flags = Notification.FLAG_AUTO_CANCEL;
                // notification.setLatestEventInfo(UpdateService.this,app_name,
                // getString(R.string.down_fail), pendingIntent);
//                notification.setLatestEventInfo(UpdateService.this, app_name, "下载失败", null);
                Intent i = new Intent(UpdateService.this, UpdateService.class);
                i.putExtra("Key_App_Name", UpdateManager.appName);
                i.putExtra("Key_Down_Url", Constants.apkDownloadUrl);

                NotificationCompat.Builder bd = new NotificationCompat.Builder(getApplication());
                bd.setSmallIcon(R.drawable.ic_net_error)
                        .setAutoCancel(true)
                        .setShowWhen(true)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setAutoCancel(true)
                        .setTicker("百信更新包下载失败")
                        .setContentTitle("下载失败")
                        .setContentText("百信更新包下载失败")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                bd.setContentIntent(PendingIntent.getActivity(UpdateService.this, 0, i, 0));
                notificationManager.notify(R.layout.notification_item, bd.build());
                *//*** stop service *****//*
                // onDestroy();
                stopSelf();
                break;

            default:
                // stopService(updateIntent);
                *//****** Stop service ******//*
                // stopService(intentname)
                // stopSelf();
                break;
            }
        }
    };

    private void installApk() {
        // TODO Auto-generated method stub
        *//********* 下载完成，点击安装 ***********//*
        Uri uri = Uri.fromFile(FileUtil.updateFile);
        Intent intent = new Intent(Intent.ACTION_VIEW);

        *//**********
         * 加这个属性是因为使用Context的startActivity方法的话，就需要开启一个新的task
         **********//*
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        UpdateService.this.startActivity(intent);
    }

    *//**
     * 方法描述：createThread方法, 开线程下载
     * 
     * @param
     * @return
     * @see UpdateService
     *//*
    public void createThread() {
        new DownLoadThread().start();
    }

    private class DownLoadThread extends Thread {
        @Override
        public void run() {
            // TODO Auto-generated method stub
            Message message = new Message();
            try {
                long downloadSize = downloadUpdateFile(down_url, FileUtil.updateFile.toString());
                if (downloadSize > 0) {
                    // down success
                    message.what = DOWN_OK;
                    handler.sendMessage(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
                message.what = DOWN_ERROR;
                handler.sendMessage(message);
            }
        }
    }

    *//**
     * 方法描述：createNotification方法
     * 
     * @param
     * @return
     * @see UpdateService
     *//*
    public void createNotification() {

        // notification = new Notification(R.drawable.dot_enable,app_name +
        // getString(R.string.is_downing) ,System.currentTimeMillis());
        notification = new Notification(R.drawable.ic_launcher,app_name + "更新包正在下载...", System.currentTimeMillis());
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        // notification.flags = Notification.FLAG_AUTO_CANCEL;

        *//*** 自定义 Notification 的显示 ****//*
        contentView = new RemoteViews(getPackageName(), R.layout.notification_item);
        contentView.setTextViewText(R.id.notificationTitle, app_name + "更新包正在下载...");
        contentView.setTextViewText(R.id.notificationPercent, "0%");
        contentView.setProgressBar(R.id.notificationProgress, 100, 0, false);
        notification.contentView = contentView;

        // updateIntent = new Intent(this, AboutActivity.class);
        // updateIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // //updateIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // pendingIntent = PendingIntent.getActivity(this, 0, updateIntent, 0);
        // notification.contentIntent = pendingIntent;

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(R.layout.notification_item, notification);
    }

    *//***
     * down file
     * 
     * @return
     * @throws MalformedURLException
     *//*
    public long downloadUpdateFile(String down_url, String file) throws Exception {

        int down_step = down_step_custom;// 提示step
        int totalSize;// 文件总大小
        int downloadCount = 0;// 已经下载好的大小
        int updateCount = 0;// 已经上传的文件大小

        InputStream inputStream;
        OutputStream outputStream;

        URL url = new URL(down_url);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setConnectTimeout(TIMEOUT);
        httpURLConnection.setReadTimeout(TIMEOUT);
        // 获取下载文件的size
        totalSize = httpURLConnection.getContentLength();

        if (httpURLConnection.getResponseCode() == 404) {
            throw new Exception("fail!");
            // 这个地方应该加一个下载失败的处理，但是，因为我们在外面加了一个try---catch，已经处理了Exception,
            // 所以不用处理
        }

        inputStream = httpURLConnection.getInputStream();
        outputStream = new FileOutputStream(file, false);// 文件存在则覆盖掉

        byte buffer[] = new byte[1024];
        int readsize = 0;

        while ((readsize = inputStream.read(buffer)) != -1) {

            // *//*********如果下载过程中出现错误，就弹出错误提示，并且把notificationManager取消*********//*
            // if (httpURLConnection.getResponseCode() == 404) {
            // notificationManager.cancel(R.layout.notification_item);
            // throw new Exception("fail!");
            // //这个地方应该加一个下载失败的处理，但是，因为我们在外面加了一个try---catch，已经处理了Exception,
            // //所以不用处理
            // }

            outputStream.write(buffer, 0, readsize);
            downloadCount += readsize;// 时时获取下载到的大小
            *//*** 每次增张1% **//*
            if (updateCount == 0 || (downloadCount * 100 / totalSize - down_step) >= updateCount) {
                updateCount += down_step;
                // 改变通知栏
                contentView.setTextViewText(R.id.notificationPercent, updateCount + "%");
                contentView.setProgressBar(R.id.notificationProgress, 100, updateCount, false);
                notification.contentView = contentView;
                notificationManager.notify(R.layout.notification_item, notification);
            }
        }
        if (httpURLConnection != null) {
            httpURLConnection.disconnect();
        }
        inputStream.close();
        outputStream.close();

        return downloadCount;
    }*/

}