/*
 * Copyright (C) 2013 Snowdream Mobile <yanghui1986527@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.ibaixin.chat.util;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;

import android.text.TextUtils;

/**
 * Wrapper API for sending log output <BR />
 * <BR />
 * <p/>
 * 1.enable/disable log
 * 
 * <pre>
 * Log.setEnabled(true);
 * Log.setEnabled(false);
 * </pre>
 * <p/>
 * 2.set the Tag for the log Log.setTag("Android"); </pre>
 * <p/>
 * 3.log simple
 * 
 * <pre>
 * Log.d(&quot;test&quot;);
 * Log.v(&quot;test&quot;);
 * Log.i(&quot;test&quot;);
 * Log.w(&quot;test&quot;);
 * Log.e(&quot;test&quot;);
 * </pre>
 * <p/>
 * 4.log simple -- set custom tag
 * 
 * <pre>
 * Log.d(&quot;TAG&quot;, &quot;test&quot;);
 * Log.v(&quot;TAG&quot;, &quot;test&quot;);
 * Log.i(&quot;TAG&quot;, &quot;test&quot;);
 * Log.w(&quot;TAG&quot;, &quot;test&quot;);
 * Log.e(&quot;TAG&quot;, &quot;test&quot;);
 * </pre>
 * <p/>
 * 5.log advance
 * 
 * <pre>
 * Log.d(&quot;test&quot;, new Throwable(&quot;test&quot;));
 * Log.v(&quot;test&quot;, new Throwable(&quot;test&quot;));
 * Log.i(&quot;test&quot;, new Throwable(&quot;test&quot;));
 * Log.w(&quot;test&quot;, new Throwable(&quot;test&quot;));
 * Log.e(&quot;test&quot;, new Throwable(&quot;test&quot;));
 * </pre>
 * <p/>
 * 6.log advance -- set custom tag
 * 
 * <pre>
 * Log.d(&quot;TAG&quot;, &quot;test&quot;, new Throwable(&quot;test&quot;));
 * Log.v(&quot;TAG&quot;, &quot;test&quot;, new Throwable(&quot;test&quot;));
 * Log.i(&quot;TAG&quot;, &quot;test&quot;, new Throwable(&quot;test&quot;));
 * Log.w(&quot;TAG&quot;, &quot;test&quot;, new Throwable(&quot;test&quot;));
 * Log.e(&quot;TAG&quot;, &quot;test&quot;, new Throwable(&quot;test&quot;));
 * </pre>
 * <p/>
 * 7.Log to File<BR>
 * log into one file
 * 
 * <pre>
 * Log.setPath(&quot;/mnt/sdcard/debug.txt&quot;);
 * Log.setPolicy(Log.LOG_ALL_TO_FILE);
 * 
 * Log.d(&quot;test 1&quot;);
 * Log.v(&quot;test 2&quot;);
 * Log.i(&quot;test 3&quot;);
 * Log.w(&quot;test 4&quot;);
 * Log.e(&quot;test 5&quot;);
 * </pre>
 * <p/>
 * log into one directory with a lot of log files
 * 
 * <pre>
 * Log.setPath(&quot;/mnt/sdcard/snowdream/log&quot;, &quot;log&quot;, &quot;log&quot;);
 * Log.setPolicy(Log.LOG_ALL_TO_FILE);
 * 
 * Log.d(&quot;test 1&quot;);
 * Log.v(&quot;test 2&quot;);
 * Log.i(&quot;test 3&quot;);
 * Log.w(&quot;test 4&quot;);
 * Log.e(&quot;test 5&quot;);
 * </pre>
 */
public class Log {
	/**
	 * ALL
	 */
	public static final int LOG_ALL_TO_FILE = 3;
	/**
	 * ERROR
	 */
	public static final int LOG_ERROR_TO_FILE = 2;
	/**
	 * None
	 */
	public static final int LOG_NONE_TO_FILE = 0;
	/**
	 * WARN
	 */
	public static final int LOG_WARN_TO_FILE = 1;
	/**
	 * The TAG of the Application
	 */
	public static String TAG = "SNOWDREAM";
	/**
	 * Whether to enable the log
	 */
	protected static boolean isEnable = true;
	/**
	 * The log dir path
	 */
	protected static String logDirPath = "/mnt/sdcard/snowdream/android/log";
	/**
	 * The log file base name
	 */
	protected static String logFileBaseName = "snowdream";
	/**
	 * The log file suffix,such as log.
	 */
	protected static String logFileSuffix = "log";
	/**
	 * The log file path
	 */
	protected static String path = "";
	/**
	 * Which will be logged into the file
	 */
	protected static int policy = LOG_NONE_TO_FILE;

	/**
	 * the constructor
	 */
	private Log() {
	}

	/**
	 * Send a DEBUG log message.
	 *
	 * @param msg
	 *            The message you would like logged.
	 */
	public static void d(String tag, String msg) {
		if (isEnable) {
			if (tag == null || tag == "") {
				d(msg);
			} else {
				android.util.Log.d(TAG,
						buildMessage(TYPE.DEBUG, TAG, msg, null));
			}
		}
	}

	/**
	 * Send a DEBUG log message.
	 */
	public static void d(String msg) {
		if (isEnable) {
			android.util.Log.d(TAG, buildMessage(TYPE.DEBUG, TAG, msg, null));
		}
	}

	/**
	 * Building Message
	 *
	 * @param msg
	 *            The message you would like logged.
	 * @return Message String
	 */
	protected static String buildMessage(TYPE type, String tag, String msg,
			Throwable thr) {
		// set the default log path
		if (TextUtils.isEmpty(path)) {
			setPath(logDirPath, logFileBaseName, logFileSuffix);
		}
		StackTraceElement caller = new Throwable().fillInStackTrace()
				.getStackTrace()[2];
		boolean isLog2File = false;
		switch (policy) {
		case LOG_NONE_TO_FILE:
			isLog2File = false;
			break;
		case LOG_WARN_TO_FILE:
			if (type == TYPE.WARN) {
				isLog2File = true;
			} else {
				isLog2File = false;
			}
			break;
		case LOG_ERROR_TO_FILE:
			if (type == TYPE.ERROR) {
				isLog2File = true;
			} else {
				isLog2File = false;
			}
			break;
		case LOG_ALL_TO_FILE:
			isLog2File = true;
			break;
		default:
			break;
		}
		// The log will be shown in logcat.
		StringBuffer bufferlog = new StringBuffer();
		bufferlog.append(caller.getClassName());
		bufferlog.append(".");
		bufferlog.append(caller.getMethodName());
		bufferlog.append("( ");
		bufferlog.append(caller.getFileName());
		bufferlog.append(": ");
		bufferlog.append(caller.getLineNumber());
		bufferlog.append(")");
		bufferlog.append(" : ");
		bufferlog.append(msg);
		if (thr != null) {
			bufferlog.append(System.getProperty("line.separator"));
			bufferlog.append(android.util.Log.getStackTraceString(thr));
		}
		if (isLog2File) {
			// The log will be written in the log file.
			StringBuffer filelog = new StringBuffer();
			filelog.append(type.name());
			filelog.append(" ");
			filelog.append(tag);
			filelog.append(" ");
			filelog.append(bufferlog);
			Log2File.log2file(path, filelog.toString());
		}
		return bufferlog.toString();
	}

	/**
	 * Send a DEBUG log message and log the exception.
	 *
	 * @param msg
	 *            The message you would like logged.
	 * @param thr
	 *            An exception to log
	 */
	public static void d(String tag, String msg, Throwable thr) {
		if (isEnable) {
			if (tag == null || tag == "") {
				d(msg, thr);
			} else {
				android.util.Log.d(TAG,
						buildMessage(TYPE.DEBUG, TAG, msg, thr), thr);
			}
		}
	}

	/**
	 * Send a DEBUG log message and log the exception.
	 *
	 * @param msg
	 *            The message you would like logged.
	 * @param thr
	 *            An exception to log
	 */
	public static void d(String msg, Throwable thr) {
		if (isEnable) {
			android.util.Log.d(TAG, buildMessage(TYPE.DEBUG, TAG, msg, thr),
					thr);
		}
	}

	/**
	 * Send a ERROR log message.
	 *
	 * @param msg
	 *            The message you would like logged.
	 */
	public static void e(String tag, String msg) {
		if (isEnable) {
			if (tag == null || tag == "") {
				e(msg);
			} else {
				android.util.Log.e(TAG,
						buildMessage(TYPE.ERROR, TAG, msg, null));
			}
		}
	}

	/**
	 * Send an ERROR log message.
	 *
	 * @param msg
	 *            The message you would like logged.
	 */
	public static void e(String msg) {
		if (isEnable) {
			android.util.Log.e(TAG, buildMessage(TYPE.ERROR, TAG, msg, null));
		}
	}

	/**
	 * Send a ERROR log message and log the exception.
	 *
	 * @param msg
	 *            The message you would like logged.
	 * @param thr
	 *            An exception to log
	 */
	public static void e(String tag, String msg, Throwable thr) {
		if (isEnable) {
			if (tag == null || tag == "") {
				e(msg, thr);
			} else {
				android.util.Log.e(TAG,
						buildMessage(TYPE.ERROR, TAG, msg, thr), thr);
			}
		}
	}

	/**
	 * Send an ERROR log message and log the exception.
	 *
	 * @param msg
	 *            The message you would like logged.
	 * @param thr
	 *            An exception to log
	 */
	public static void e(String msg, Throwable thr) {
		if (isEnable) {
			android.util.Log.e(TAG, buildMessage(TYPE.ERROR, TAG, msg, thr),
					thr);
		}
	}

	/**
	 * Get the ExecutorService
	 *
	 * @return the ExecutorService
	 */
	public static ExecutorService getExecutor() {
		return Log2File.getExecutor();
	}

	/**
	 * Set the ExecutorService
	 *
	 * @param executor
	 *            the ExecutorService
	 */
	public static void setExecutor(ExecutorService executor) {
		Log2File.setExecutor(executor);
	}

	/**
	 * get the log file path
	 *
	 * @return path
	 */
	public static String getPath() {
		return path;
	}

	/**
	 * set the path of the log file
	 *
	 * @param path
	 */
	public static void setPath(String path) {
		Log.path = path;
		createLogDir(path);
	}

	/**
	 * create the Directory from the path
	 *
	 * @param path
	 */
	private static void createLogDir(String path) {
		if (TextUtils.isEmpty(path)) {
			android.util.Log.e("Error", "The path is not valid.");
			return;
		}
		File file = new File(path);
		boolean ret;
		boolean exist;
		exist = file.getParentFile().exists();
		if (!exist) {
			ret = file.getParentFile().mkdirs();
			if (!ret) {
				android.util.Log.e("Error", "The Log Dir can not be created!");
				return;
			}
			android.util.Log.i(
					"Success",
					"The Log Dir was successfully created! -"
							+ file.getParent());
		}
	}

	/**
	 * get the policy of the log
	 *
	 * @return the policy of the log
	 */
	public static int getPolicy() {
		return policy;
	}

	/**
	 * set the policy of the log
	 *
	 * @param policy
	 *            the policy of the log
	 */
	public static void setPolicy(int policy) {
		Log.policy = policy;
	}

	/**
	 * Handy function to get a loggable stack trace from a Throwable
	 *
	 * @param tr
	 *            An exception to log
	 * @return
	 */
	public static String getStackTraceString(Throwable tr) {
		return android.util.Log.getStackTraceString(tr);
	}

	/**
	 * Get the Tag of the application
	 */
	public static String getTag() {
		return TAG;
	}

	/**
	 * Set the Tag of the application
	 *
	 * @param tag
	 *            the Tag of the application
	 */
	public static void setTag(String tag) {
		TAG = tag;
	}

	/**
	 * Send a INFO log message.
	 *
	 * @param msg
	 *            The message you would like logged.
	 */
	public static void i(String tag, String msg) {
		if (isEnable) {
			if (tag == null || tag == "") {
				i(msg);
			} else {
				android.util.Log
						.i(TAG, buildMessage(TYPE.INFO, TAG, msg, null));
			}
		}
	}

	/**
	 * Send an INFO log message.
	 *
	 * @param msg
	 *            The message you would like logged.
	 */
	public static void i(String msg) {
		if (isEnable) {
			android.util.Log.i(TAG, buildMessage(TYPE.INFO, TAG, msg, null));
		}
	}

	/**
	 * Send a INFO log message and log the exception.
	 *
	 * @param msg
	 *            The message you would like logged.
	 * @param thr
	 *            An exception to log
	 */
	public static void i(String tag, String msg, Throwable thr) {
		if (isEnable) {
			if (tag == null || tag == "") {
				i(msg, thr);
			} else {
				android.util.Log.i(TAG, buildMessage(TYPE.INFO, TAG, msg, thr),
						thr);
			}
		}
	}

	/**
	 * Send a INFO log message and log the exception.
	 *
	 * @param msg
	 *            The message you would like logged.
	 * @param thr
	 *            An exception to log
	 */
	public static void i(String msg, Throwable thr) {
		if (isEnable) {
			android.util.Log
					.i(TAG, buildMessage(TYPE.INFO, TAG, msg, thr), thr);
		}
	}

	/**
	 * is the log enabled?
	 */
	public static boolean isEnabled() {
		return isEnable;
	}

	/**
	 * enable or disable the log
	 *
	 * @param enabled
	 *            whether to enable the log
	 */
	public static void setEnabled(boolean enabled) {
		isEnable = enabled;
	}

	/**
	 * Checks to see whether or not a log for the specified tag is loggable at
	 * the specified level. The default level of any tag is set to INFO. This
	 * means that any level above and including INFO will be logged. Before you
	 * make any calls to a logging method you should check to see if your tag
	 * should be logged. You can change the default level by setting a system
	 * property: 'setprop log.tag.<YOUR_LOG_TAG> <LEVEL>' Where level is either
	 * VERBOSE, DEBUG, INFO, WARN, ERROR, ASSERT, or SUPPRESS. SUPPRESS will
	 * turn off all logging for your tag. You can also create a local.prop file
	 * that with the following in it: 'log.tag.<YOUR_LOG_TAG>=<LEVEL>' and place
	 * that in /data/local.prop.
	 *
	 * @param tag
	 *            The tag to check
	 * @param level
	 *            The level to check
	 * @return Whether or not that this is allowed to be logged.
	 */
	public static boolean isLoggable(String tag, int level) {
		return android.util.Log.isLoggable(tag, level);
	}

	/**
	 * Low-level logging call.
	 *
	 * @param priority
	 *            The priority/type of this log message
	 * @param tag
	 *            Used to identify the source of a log message. It usually
	 *            identifies the class or activity where the log call occurs.
	 * @param msg
	 *            The message you would like logged.
	 * @return The number of bytes written.
	 */
	public static int println(int priority, String tag, String msg) {
		return android.util.Log.println(priority, tag, msg);
	}

	/**
	 * set the log file path
	 * <p/>
	 * The log file path will be: logDirPath + logFileBaseName + Formated time
	 * +logFileSuffix
	 *
	 * @param logDirPath
	 *            the log file dir path,such as "/mnt/sdcard/snowdream/log"
	 * @param logFileBaseName
	 *            the log file base file name,such as "log"
	 * @param logFileSuffix
	 *            the log file suffix,such as "log"
	 */
	public static void setPath(String logDirPath, String logFileBaseName,
			String logFileSuffix) {
		if (!TextUtils.isEmpty(logDirPath)) {
			Log.logDirPath = logDirPath;
		}
		if (!TextUtils.isEmpty(logFileBaseName)) {
			Log.logFileBaseName = logFileBaseName;
		}
		if (!TextUtils.isEmpty(logFileSuffix)) {
			Log.logFileSuffix = logFileSuffix;
		}
		Date myDate = new Date();
//		FastDateFormat fdf = FastDateFormat.getInstance("yyyy-MM-dd-HH-mm-ss");
		SimpleDateFormat fdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		String myDateString = fdf.format(myDate);
		StringBuffer buffer = new StringBuffer();
		buffer.append(logDirPath);
		if (!logDirPath.endsWith("/")) {
			buffer.append("/");
		}
		buffer.append(logFileBaseName);
		buffer.append("-");
		buffer.append(myDateString);
		buffer.append(".");
		buffer.append(logFileSuffix);
		setPath(buffer.toString());
	}

	/**
	 * Send a VERBOSE log message.
	 *
	 * @param msg
	 *            The message you would like logged.
	 */
	public static void v(String tag, String msg) {
		if (isEnable) {
			if (tag == null || tag == "") {
				v(msg);
			} else {
				android.util.Log.v(TAG,
						buildMessage(TYPE.VERBOSE, TAG, msg, null));
			}
		}
	}

	/**
	 * Send a VERBOSE log message.
	 *
	 * @param msg
	 *            The message you would like logged.
	 */
	public static void v(String msg) {
		if (isEnable) {
			android.util.Log.v(TAG, buildMessage(TYPE.VERBOSE, TAG, msg, null));
		}
	}

	/**
	 * Send a VERBOSE log message and log the exception.
	 *
	 * @param msg
	 *            The message you would like logged.
	 * @param thr
	 *            An exception to log
	 */
	public static void v(String tag, String msg, Throwable thr) {
		if (isEnable) {
			if (tag == null || tag == "") {
				v(msg, thr);
			} else {
				android.util.Log.v(TAG,
						buildMessage(TYPE.VERBOSE, TAG, msg, thr), thr);
			}
		}
	}

	/**
	 * Send a VERBOSE log message and log the exception.
	 *
	 * @param msg
	 *            The message you would like logged.
	 * @param thr
	 *            An exception to log
	 */
	public static void v(String msg, Throwable thr) {
		if (isEnable) {
			android.util.Log.v(TAG, buildMessage(TYPE.VERBOSE, TAG, msg, thr),
					thr);
		}
	}

	/**
	 * Send an empty WARN log message and log the exception.
	 *
	 * @param thr
	 *            An exception to log
	 */
	public static void w(Throwable thr) {
		if (isEnable) {
			android.util.Log.w(TAG, buildMessage(TYPE.WARN, TAG, "", thr), thr);
		}
	}

	/**
	 * Send a WARN log message.
	 *
	 * @param msg
	 *            The message you would like logged.
	 */
	public static void w(String tag, String msg) {
		if (isEnable) {
			if (tag == null || tag == "") {
				w(msg);
			} else {
				android.util.Log
						.w(TAG, buildMessage(TYPE.WARN, TAG, msg, null));
			}
		}
	}

	/**
	 * Send a WARN log message
	 *
	 * @param msg
	 *            The message you would like logged.
	 */
	public static void w(String msg) {
		if (isEnable) {
			android.util.Log.w(TAG, buildMessage(TYPE.WARN, TAG, msg, null));
		}
	}

	/**
	 * Send a WARN log message and log the exception.
	 *
	 * @param msg
	 *            The message you would like logged.
	 * @param thr
	 *            An exception to log
	 */
	public static void w(String tag, String msg, Throwable thr) {
		if (isEnable) {
			if (tag == null || tag == "") {
				w(msg, thr);
			} else {
				android.util.Log.w(TAG, buildMessage(TYPE.WARN, TAG, msg, thr),
						thr);
			}
		}
	}

	/**
	 * Send a WARN log message and log the exception.
	 *
	 * @param msg
	 *            The message you would like logged.
	 * @param thr
	 *            An exception to log
	 */
	public static void w(String msg, Throwable thr) {
		if (isEnable) {
			android.util.Log
					.w(TAG, buildMessage(TYPE.WARN, TAG, msg, thr), thr);
		}
	}

	private enum TYPE {
		INFO, DEBUG, VERBOSE, WARN, ERROR
	}
}