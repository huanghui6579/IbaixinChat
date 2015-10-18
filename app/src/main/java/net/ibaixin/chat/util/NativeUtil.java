package net.ibaixin.chat.util;

import java.util.ArrayList;

/**
 * 本地方法的工具类
 * @author huanghui1
 * @version 1.0.0
 * @update 2015年8月25日 上午9:38:55
 */
public class NativeUtil {
	//---------本地方法-------------//
   /**
    * 同过本地方法获取对应目录下的文件名，包含目录名
    * @param dir 要获取的目录
    * @return 文件的集合
    * @update 2015年8月24日 下午9:58:48
    */
   public static native ArrayList<String> listFileNames(String dir);
}
