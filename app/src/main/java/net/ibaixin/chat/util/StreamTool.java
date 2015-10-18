package net.ibaixin.chat.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import android.util.Log;
/**
 * 上传堆Stream的处理工具
 * @author dudejin
 *
 */
public class StreamTool {
	 public static void save(File file, byte[] data) throws Exception {
		 FileOutputStream outStream = new FileOutputStream(file);
		 outStream.write(data);
		 outStream.close();
	 }
	 
	 public static String readLine(PushbackInputStream in) throws IOException {
			char buf[] = new char[128];
			int room = buf.length;
			int offset = 0;
			int c;
loop:		while (true) {
				switch (c = in.read()) {
					case -1:
					case '\n':
						break loop;
					case '\r':
						int c2 = in.read();
						if ((c2 != '\n') && (c2 != -1)) in.unread(c2);
						break loop;
					default:
						if (--room < 0) {
							char[] lineBuffer = buf;
							buf = new char[offset + 128];
						    room = buf.length - offset - 1;
						    System.arraycopy(lineBuffer, 0, buf, 0, offset);
						   
						}
						buf[offset++] = (char) c;
						break;
				}
			}
			if ((c == -1) && (offset == 0)) return null;
			return String.copyValueOf(buf, 0, offset);
	}
	 
	 /**
		 * 从输入流中获取数据
		 * @param inStream 输入流
		 * @return
		 * @throws Exception
		 */
		public static byte[] readInputStream(InputStream inStream) throws Exception{
			ByteArrayOutputStream outStream = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int len = 0;
			while( (len=inStream.read(buffer)) != -1 ){
				outStream.write(buffer, 0, len);
			}
			inStream.close();
			return outStream.toByteArray();
		}
		
		/**
		 * 与服务器连接通用方法
		 * @param path 服务器路径
		 * @return
		 */
		public static String connectServer(String path) {
			byte[] data = null;
			String json = null;
			try {
				URL url = new URL(path);
				HttpURLConnection conn = (HttpURLConnection)url.openConnection();
				conn.setReadTimeout(30000);
				conn.setConnectTimeout(10000);
				conn.setRequestMethod("POST");
				InputStream inStream = conn.getInputStream();
				data = StreamTool.readInputStream(inStream);
				json = new String(data);
			} catch (Exception e) {
				Log.e("Exception", e.toString());
			}
			return json;
		}
		
		/**
		 * 从服务器获取文件的二进制数据
		 * @param path 请求路径
		 * @return byte[]
		 */
		public static byte[] getFileByte(String path) {
			byte[] data = null;
			String json ="";
			try {
				URL url = new URL(path);
				HttpURLConnection conn = (HttpURLConnection)url.openConnection();
				conn.setReadTimeout(30000);
				conn.setConnectTimeout(10000);
				conn.setRequestMethod("POST");
				InputStream inStream = conn.getInputStream();
				data = StreamTool.readInputStream(inStream);
			} catch (Exception e) {
				Log.e("Exception", e.toString());
			}
			return data;
		}
}