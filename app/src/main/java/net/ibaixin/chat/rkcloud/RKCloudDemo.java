package net.ibaixin.chat.rkcloud;

import android.content.Context;
import android.util.DisplayMetrics;

import net.ibaixin.chat.rkcloud.http.HttpTools;

public class RKCloudDemo {
    private static RKCloudDemo mInstance;
    public static Context context = null;
    public static boolean debugModel = false;// 是否为debug模式，默认为false
    public static Config config = null;
    public static HttpTools kit = null;
    public static int screenWidth;
    public static int screenHeight;

    private RKCloudDemo(Context c) {
        context = c;
        config = new Config(context);
        kit = HttpTools.getInstance(context);
        kit.setOnHttpFatalException(SDKManager.getInstance());

        DisplayMetrics dm = new DisplayMetrics();
        dm = context.getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
    }

    public static RKCloudDemo create(Context c) {
        if (null == mInstance) {
            mInstance = new RKCloudDemo(c);
        }
        return mInstance;
    }
}
