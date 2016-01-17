package net.ibaixin.chat.update;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

    /**
     * 复制文件
     * @param srcPath 原始文件的路径
     * @param destPath 目标文件的路径
     * @author tiger
     * @update 2016/1/17 10:51
     * @version 1.0.0
     * @return 是否复制成功
     */
    public static boolean copyFile(String srcPath, String destPath) {
        boolean success = false;
        if (srcPath != null && destPath != null) {
            File srcFile = new File(srcPath);
            if (srcFile.exists()) { //原始文件存在
                FileInputStream fis = null;
                FileOutputStream fos = null;
                try {
                    fis = new FileInputStream(srcFile);
                    fos = new FileOutputStream(destPath);
                    int len = -1;
                    byte[] buf = new byte[8192];
                    while ((len = fis.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                    }
                    fos.flush();
                    success = true;
                } catch (IOException e) {
                    Log.d(e.getMessage());
                } finally {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e) {
                            Log.e(e.getMessage());
                        }
                    }
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            Log.e(e.getMessage());
                        }
                    }
                }
            }
        }
        return success;
    }
}