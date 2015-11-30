package net.ibaixin.chat.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * 创建人：huanghui1
 * 创建时间： 2015/11/30 11:16
 * 修改人：huanghui1
 * 修改时间：2015/11/30 11:16
 * 修改备注：
 *
 * @version: 0.0.1
 */
public class ActivityCompatICS {
    
    public static void startActivity(Activity activity, Intent intent, Bundle bundle) {
        startActivityForResult(activity, intent, -1, bundle);
    }
    
    public static void startActivityForResult(Activity activity, Intent intent, int requestCode, Bundle bundle) {
//        if (bundle == null) {
//            throw new RuntimeException("Bundle must be not null");
//        } 
        int enterResId = 0;
        int exitResId = 0;
        if (bundle != null) {
            int animType = bundle.getInt(ActivityOptionsCompatICS.KEY_ANIM_TYPE, 0);
            if (animType == ActivityOptionsCompatICS.ANIM_CUSTOM) {
                enterResId = bundle.getInt(ActivityOptionsCompatICS.KEY_ANIM_ENTER_RES_ID);
                exitResId = bundle.getInt(ActivityOptionsCompatICS.KEY_ANIM_EXIT_RES_ID);
            } else {
                enterResId = 0;
                exitResId = 0;
            }
            intent.putExtras(bundle);

        }
        activity.startActivityForResult(intent, requestCode);
        activity.overridePendingTransition(enterResId, exitResId);
    }
}
