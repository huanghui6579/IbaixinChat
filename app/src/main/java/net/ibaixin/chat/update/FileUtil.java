package net.ibaixin.chat.update;

import java.io.File;
import java.io.IOException;

import android.os.Environment;
import net.ibaixin.chat.util.Constants;
import net.ibaixin.chat.util.Log;

/**
 * 类描述：FileUtil
 * 
 * @author dudejin
 * @version
 */
public class FileUtil {

    public static File updateDir = null;
    public static File updateFile = null;
    /*********** 保存升级APK的目录 ***********/
    public static final String APPLICATION = Constants.DEAULT_APP_FOLDER_NAME;

    public static boolean isCreateFileSucess;

    /**
     * 方法描述：createFile方法
     * 
     * @param String
     *            app_name
     * @return
     * @see FileUtil
     */
    public static void createFile(String app_name) {

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            isCreateFileSucess = true;

            updateDir = new File(Environment.getExternalStorageDirectory() + "/" + APPLICATION + "/");
            updateFile = new File(updateDir + "/" + app_name + ".apk");

            if (!updateDir.exists()) {
                updateDir.mkdirs();
            }
            if (!updateFile.exists()) {
                try {
                    updateFile.createNewFile();
                } catch (IOException e) {
                    isCreateFileSucess = false;
                    Log.e(e.toString());
                }
            }
        } else {
            isCreateFileSucess = false;
        }
    }
}