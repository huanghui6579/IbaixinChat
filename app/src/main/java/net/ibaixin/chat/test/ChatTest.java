package net.ibaixin.chat.test;

import net.ibaixin.chat.util.Log;
import net.ibaixin.chat.util.SystemUtil;
import junit.framework.TestCase;

/**
 * 
 * @author tiger
 * @version 2015年3月8日 下午6:53:20
 */
public class ChatTest extends TestCase {
	public void testMd5() {
		String str = "202cb962ac59075b964b07152d234b70";
		Log.d("----123---md5----" + SystemUtil.encoderByMd5(str));
	}
}
