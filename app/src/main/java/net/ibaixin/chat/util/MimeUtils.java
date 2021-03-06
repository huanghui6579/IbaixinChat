package net.ibaixin.chat.util;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import net.ibaixin.chat.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class MimeUtils {
	private static final Map<String, String> mimeTypeToExtensionMap = new HashMap<>();

	private static final Map<String, String> extensionToMimeTypeMap = new HashMap<>();
	
	private static final Map<String, Integer> extensionResMap = new HashMap<>();
	
	public static final String MIME_TYPE_TEXT = "text/plain";
	public static final String MIME_TYPE_IMAGE_JPG = "image/jpeg";
	public static final String MIME_TYPE_IMAGE_PNG = "image/png";
	public static final String MIME_TYPE_AUDIO_AMR = "audio/amr";
	public static final String MIME_FILE = "*/*";
	
	static {
		add("application/andrew-inset", "ez");
		add("application/dsptype", "tsp");
		add("application/futuresplash", "spl");
		add("application/hta", "hta");
		add("application/mac-binhex40", "hqx");
		add("application/mac-compactpro", "cpt");
		add("application/mathematica", "nb");
		add("application/msaccess", "mdb");
		add("application/oda", "oda");
		add("application/ogg", "ogg");
		add("application/pdf", "pdf");
		add("application/pgp-keys", "key");
		add("application/pgp-signature", "pgp");
		add("application/pics-rules", "prf");
		add("application/rar", "rar");
		add("application/rdf+xml", "rdf");
		add("application/rss+xml", "rss");
		add("application/zip", "zip");
		add("application/vnd.android.package-archive", "apk");
		add("application/vnd.cinderella", "cdy");
		add("application/vnd.ms-pki.stl", "stl");
		add("application/vnd.oasis.opendocument.database", "odb");
		add("application/vnd.oasis.opendocument.formula", "odf");
		add("application/vnd.oasis.opendocument.graphics", "odg");
		add("application/vnd.oasis.opendocument.graphics-template", "otg");
		add("application/vnd.oasis.opendocument.image", "odi");
		add("application/vnd.oasis.opendocument.spreadsheet", "ods");
		add("application/vnd.oasis.opendocument.spreadsheet-template", "ots");
		add("application/vnd.oasis.opendocument.text", "odt");
		add("application/vnd.oasis.opendocument.text-master", "odm");
		add("application/vnd.oasis.opendocument.text-template", "ott");
		add("application/vnd.oasis.opendocument.text-web", "oth");
		add("application/vnd.google-earth.kml+xml", "kml");
		add("application/vnd.google-earth.kmz", "kmz");
		add("application/msword", "doc");
		add("application/msword", "dot");
		add("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
				"docx");
		add("application/vnd.openxmlformats-officedocument.wordprocessingml.template",
				"dotx");
		add("application/vnd.ms-excel", "xls");
		add("application/vnd.ms-excel", "xlt");
		add("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				"xlsx");
		add("application/vnd.openxmlformats-officedocument.spreadsheetml.template",
				"xltx");
		add("application/vnd.ms-powerpoint", "ppt");
		add("application/vnd.ms-powerpoint", "pot");
		add("application/vnd.ms-powerpoint", "pps");
		add("application/vnd.openxmlformats-officedocument.presentationml.presentation",
				"pptx");
		add("application/vnd.openxmlformats-officedocument.presentationml.template",
				"potx");
		add("application/vnd.openxmlformats-officedocument.presentationml.slideshow",
				"ppsx");
		add("application/vnd.rim.cod", "cod");
		add("application/vnd.smaf", "mmf");
		add("application/vnd.stardivision.calc", "sdc");
		add("application/vnd.stardivision.draw", "sda");
		add("application/vnd.stardivision.impress", "sdd");
		add("application/vnd.stardivision.impress", "sdp");
		add("application/vnd.stardivision.math", "smf");
		add("application/vnd.stardivision.writer", "sdw");
		add("application/vnd.stardivision.writer", "vor");
		add("application/vnd.stardivision.writer-global", "sgl");
		add("application/vnd.sun.xml.calc", "sxc");
		add("application/vnd.sun.xml.calc.template", "stc");
		add("application/vnd.sun.xml.draw", "sxd");
		add("application/vnd.sun.xml.draw.template", "std");
		add("application/vnd.sun.xml.impress", "sxi");
		add("application/vnd.sun.xml.impress.template", "sti");
		add("application/vnd.sun.xml.math", "sxm");
		add("application/vnd.sun.xml.writer", "sxw");
		add("application/vnd.sun.xml.writer.global", "sxg");
		add("application/vnd.sun.xml.writer.template", "stw");
		add("application/vnd.visio", "vsd");
		add("application/x-abiword", "abw");
		add("application/x-apple-diskimage", "dmg");
		add("application/x-bcpio", "bcpio");
		add("application/x-bittorrent", "torrent");
		add("application/x-cdf", "cdf");
		add("application/x-cdlink", "vcd");
		add("application/x-chess-pgn", "pgn");
		add("application/x-cpio", "cpio");
		add("application/x-debian-package", "deb");
		add("application/x-debian-package", "udeb");
		add("application/x-director", "dcr");
		add("application/x-director", "dir");
		add("application/x-director", "dxr");
		add("application/x-dms", "dms");
		add("application/x-doom", "wad");
		add("application/x-dvi", "dvi");
		add("application/x-flac", "flac");
		add("application/x-font", "pfa");
		add("application/x-font", "pfb");
		add("application/x-font", "gsf");
		add("application/x-font", "pcf");
		add("application/x-font", "pcf.Z");
		add("application/x-freemind", "mm");
		add("application/x-futuresplash", "spl");
		add("application/x-gnumeric", "gnumeric");
		add("application/x-go-sgf", "sgf");
		add("application/x-graphing-calculator", "gcf");
		add("application/x-gtar", "gtar");
		add("application/x-gtar", "tgz");
		add("application/x-gtar", "taz");
		add("application/x-hdf", "hdf");
		add("application/x-ica", "ica");
		add("application/x-internet-signup", "ins");
		add("application/x-internet-signup", "isp");
		add("application/x-iphone", "iii");
		add("application/x-iso9660-image", "iso");
		add("application/x-jmol", "jmz");
		add("application/x-kchart", "chrt");
		add("application/x-killustrator", "kil");
		add("application/x-koan", "skp");
		add("application/x-koan", "skd");
		add("application/x-koan", "skt");
		add("application/x-koan", "skm");
		add("application/x-kpresenter", "kpr");
		add("application/x-kpresenter", "kpt");
		add("application/x-kspread", "ksp");
		add("application/kswps", ".wps");
		add("application/kset", ".et");
		add("application/ksdps", ".dps");
		add("application/x-kword", "kwd");
		add("application/x-kword", "kwt");
		add("application/x-latex", "latex");
		add("application/x-lha", "lha");
		add("application/x-lzh", "lzh");
		add("application/x-lzx", "lzx");
		add("application/x-maker", "frm");
		add("application/x-maker", "maker");
		add("application/x-maker", "frame");
		add("application/x-maker", "fb");
		add("application/x-maker", "book");
		add("application/x-maker", "fbdoc");
		add("application/x-mif", "mif");
		add("application/x-ms-wmd", "wmd");
		add("application/x-ms-wmz", "wmz");
		add("application/x-msi", "msi");
		add("application/x-ns-proxy-autoconfig", "pac");
		add("application/x-nwc", "nwc");
		add("application/x-object", "o");
		add("application/x-oz-application", "oza");
		add("application/x-pkcs12", "p12");
		add("application/x-pkcs12", "pfx");
		add("application/x-pkcs7-certreqresp", "p7r");
		add("application/x-pkcs7-crl", "crl");
		add("application/x-quicktimeplayer", "qtl");
		add("application/x-shar", "shar");
		add("application/x-shockwave-flash", "swf");
		add("application/x-stuffit", "sit");
		add("application/x-sv4cpio", "sv4cpio");
		add("application/x-sv4crc", "sv4crc");
		add("application/x-tar", "tar");
		add("application/x-texinfo", "texinfo");
		add("application/x-texinfo", "texi");
		add("application/x-troff", "t");
		add("application/x-troff", "roff");
		add("application/x-troff-man", "man");
		add("application/x-ustar", "ustar");
		add("application/x-wais-source", "src");
		add("application/x-wingz", "wz");
		add("application/x-webarchive", "webarchive");
		add("application/x-webarchive-xml", "webarchivexml");
		add("application/x-x509-ca-cert", "crt");
		add("application/x-x509-user-cert", "crt");
		add("application/x-xcf", "xcf");
		add("application/x-xfig", "fig");
		add("application/xhtml+xml", "xhtml");
		add("audio/3gpp", "3gpp");
		add("audio/amr", "amr");
		add("audio/basic", "snd");
		add("audio/midi", "mid");
		add("audio/midi", "midi");
		add("audio/midi", "kar");
		add("audio/midi", "xmf");
		add("audio/mobile-xmf", "mxmf");
		add("audio/mpeg", "mpga");
		add("audio/mpeg", "mpega");
		add("audio/mpeg", "mp2");
		add("audio/mpeg", "mp3");
		add("audio/mpeg", "m4a");
		add("audio/mpegurl", "m3u");
		add("audio/prs.sid", "sid");
		add("audio/x-aiff", "aif");
		add("audio/x-aiff", "aiff");
		add("audio/x-aiff", "aifc");
		add("audio/x-gsm", "gsm");
		add("audio/x-mpegurl", "m3u");
		add("audio/x-ms-wma", "wma");
		add("audio/x-ms-wax", "wax");
		add("audio/x-pn-realaudio", "ra");
		add("audio/x-pn-realaudio", "rm");
		add("audio/x-pn-realaudio", "ram");
		add("audio/x-realaudio", "ra");
		add("audio/x-scpls", "pls");
		add("audio/x-sd2", "sd2");
		add("audio/x-wav", "wav");
		add("image/bmp", "bmp");
		add("image/gif", "gif");
		add("image/ico", "cur");
		add("image/ico", "ico");
		add("image/ief", "ief");
		add("image/jpeg", "jpeg");
		add("image/jpeg", "jpg");
		add("image/jpeg", "jpe");
		add("image/pcx", "pcx");
		add("image/png", "png");
		add("image/svg+xml", "svg");
		add("image/svg+xml", "svgz");
		add("image/tiff", "tiff");
		add("image/tiff", "tif");
		add("image/vnd.djvu", "djvu");
		add("image/vnd.djvu", "djv");
		add("image/vnd.wap.wbmp", "wbmp");
		add("image/x-cmu-raster", "ras");
		add("image/x-coreldraw", "cdr");
		add("image/x-coreldrawpattern", "pat");
		add("image/x-coreldrawtemplate", "cdt");
		add("image/x-corelphotopaint", "cpt");
		add("image/x-icon", "ico");
		add("image/x-jg", "art");
		add("image/x-jng", "jng");
		add("image/x-ms-bmp", "bmp");
		add("image/x-photoshop", "psd");
		add("image/x-portable-anymap", "pnm");
		add("image/x-portable-bitmap", "pbm");
		add("image/x-portable-graymap", "pgm");
		add("image/x-portable-pixmap", "ppm");
		add("image/x-rgb", "rgb");
		add("image/x-xbitmap", "xbm");
		add("image/x-xpixmap", "xpm");
		add("image/x-xwindowdump", "xwd");
		add("model/iges", "igs");
		add("model/iges", "iges");
		add("model/mesh", "msh");
		add("model/mesh", "mesh");
		add("model/mesh", "silo");
		add("text/calendar", "ics");
		add("text/calendar", "icz");
		add("text/comma-separated-values", "csv");
		add("text/css", "css");
		add("text/html", "htm");
		add("text/html", "html");
		add("text/h323", "323");
		add("text/iuls", "uls");
		add("text/mathml", "mml");

		add("text/plain", "txt");
		add("text/plain", "asc");
		add("text/plain", "text");
		add("text/plain", "diff");
		add("text/plain", "po");
		add("text/richtext", "rtx");
		add("text/rtf", "rtf");
		add("text/texmacs", "ts");
		add("text/text", "phps");
		add("text/tab-separated-values", "tsv");
		add("text/xml", "xml");
		add("text/x-bibtex", "bib");
		add("text/x-boo", "boo");
		add("text/x-c++hdr", "h++");
		add("text/x-c++hdr", "hpp");
		add("text/x-c++hdr", "hxx");
		add("text/x-c++hdr", "hh");
		add("text/x-c++src", "c++");
		add("text/x-c++src", "cpp");
		add("text/x-c++src", "cxx");
		add("text/x-chdr", "h");
		add("text/x-component", "htc");
		add("text/x-csh", "csh");
		add("text/x-csrc", "c");
		add("text/x-dsrc", "d");
		add("text/x-haskell", "hs");
		add("text/x-java", "java");
		add("text/x-literate-haskell", "lhs");
		add("text/x-moc", "moc");
		add("text/x-pascal", "p");
		add("text/x-pascal", "pas");
		add("text/x-pcs-gcd", "gcd");
		add("text/x-setext", "etx");
		add("text/x-tcl", "tcl");
		add("text/x-tex", "tex");
		add("text/x-tex", "ltx");
		add("text/x-tex", "sty");
		add("text/x-tex", "cls");
		add("text/x-vcalendar", "vcs");
		add("text/x-vcard", "vcf");
		add("video/3gpp", "3gpp");
		add("video/3gpp", "3gp");
		add("video/3gpp", "3g2");
		add("video/dl", "dl");
		add("video/dv", "dif");
		add("video/dv", "dv");
		add("video/fli", "fli");
		add("video/m4v", "m4v");
		add("video/mpeg", "mpeg");
		add("video/mpeg", "mpg");
		add("video/mpeg", "mpe");
		add("video/mp4", "mp4");
		add("video/mpeg", "VOB");
		add("video/rm", "rm");
		add("video/rmvb", "rmvb");
		add("video/x-matroska", "mkv");
		add("video/quicktime", "qt");
		add("video/quicktime", "mov");
		add("video/vnd.mpegurl", "mxu");
		add("video/x-la-asf", "lsf");
		add("video/x-la-asf", "lsx");
		add("video/x-mng", "mng");
		add("video/x-ms-asf", "asf");
		add("video/x-ms-asf", "asx");
		add("video/x-ms-wm", "wm");
		add("video/x-ms-wmv", "wmv");
		add("video/x-ms-wmx", "wmx");
		add("video/x-ms-wvx", "wvx");
		add("video/x-msvideo", "avi");
		add("video/x-sgi-movie", "movie");
		add("video/x-webex", "wrf");
		add("x-conference/x-cooltalk", "ice");
		add("x-epoc/x-sisx-app", "sisx");
		
		initresMap();
		
		applyOverrides();
	}
	
	private static void initresMap() {
		extensionResMap.put("apk", R.drawable.ic_apk);
		
		extensionResMap.put("zip", R.drawable.ic_zip);
		extensionResMap.put("tar", R.drawable.ic_zip);
		extensionResMap.put("rar", R.drawable.ic_zip);
		extensionResMap.put("7z", R.drawable.ic_zip);
		
		extensionResMap.put("doc", R.drawable.ic_doc);
		extensionResMap.put("docx", R.drawable.ic_doc);
		extensionResMap.put("wps", R.drawable.ic_doc);
		
		extensionResMap.put("ppt", R.drawable.ic_ppt);
		extensionResMap.put("pptx", R.drawable.ic_ppt);
		extensionResMap.put("dps", R.drawable.ic_ppt);
		
		extensionResMap.put("xls", R.drawable.ic_xls);
		extensionResMap.put("xlsx", R.drawable.ic_xls);
		extensionResMap.put("et", R.drawable.ic_xls);
		
		extensionResMap.put("pdf", R.drawable.ic_pdf);
		
		extensionResMap.put("image", R.drawable.ic_image);
		
		extensionResMap.put("audio", R.drawable.ic_audio);
		
		extensionResMap.put("video", R.drawable.ic_video);
		
		extensionResMap.put("text", R.drawable.ic_text);
		
		extensionResMap.put("file", R.drawable.ic_attach_file);
		
	}

	private static void add(String mimeType, String extension) {
		if (!mimeTypeToExtensionMap.containsKey(mimeType)) {
			mimeTypeToExtensionMap.put(mimeType, extension);
		}
		extensionToMimeTypeMap.put(extension, mimeType);
	}

	private static InputStream getContentTypesPropertiesStream() {
		String userTable = System.getProperty("content.types.user.table");
		if (userTable != null) {
			File f = new File(userTable);
			if (f.exists()) {
				try {
					return new FileInputStream(f);
				} catch (IOException ignored) {
				}
			}
		}

		File f = new File(System.getProperty("java.home"), "lib"
				+ File.separator + "content-types.properties");
		if (f.exists()) {
			try {
				return new FileInputStream(f);
			} catch (IOException ignored) {
			}
		}

		return null;
	}

	private static void applyOverrides() {
		InputStream stream = getContentTypesPropertiesStream();
		if (stream == null) {
			return;
		}
		try {
			try {
				Properties overrides = new Properties();
				overrides.load(stream);

				for (Map.Entry<Object, Object> entry : overrides.entrySet()) {
					String extension = (String) entry.getKey();
					String mimeType = (String) entry.getValue();
					add(mimeType, extension);
				}
			} finally {
				stream.close();
			}
		} catch (IOException ignored) {
		}
	}

	public static boolean hasMimeType(String mimeType) {
		if ((mimeType == null) || (mimeType.isEmpty())) {
			return false;
		}
		return mimeTypeToExtensionMap.containsKey(mimeType);
	}

	public static String guessMimeTypeFromExtension(String extension) {
		if ((extension == null) || (extension.isEmpty())) {
			return null;
		}
		return (String) extensionToMimeTypeMap.get(extension);
	}

	public static String guessMimeTypeFromFilename(String fileName) {
		if (fileName == null) {
			return null;
		}
		String subfix = SystemUtil.getFileSubfix(fileName);
		String mimeType = guessMimeTypeFromExtension(subfix);
		return mimeType;
	}

	public static boolean hasExtension(String extension) {
		if ((extension == null) || (extension.isEmpty())) {
			return false;
		}
		return extensionToMimeTypeMap.containsKey(extension);
	}

	public static String guessExtensionFromMimeType(String mimeType) {
		if ((mimeType == null) || (mimeType.isEmpty())) {
			return null;
		}
		return (String) mimeTypeToExtensionMap.get(mimeType);
	}
	
	public static Integer guessResIdFromExtension(String extension) {
		return extensionResMap.get(extension);
	}
	
	/**
	 * android获取一个用于打开音频文件的intent
	 * @update 2015年3月3日 下午5:32:44
	 * @param file
	 * @return
	 */
	public static Intent getAudioFileIntent(File file) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Uri uri = Uri.fromFile(file);
        intent.setDataAndType(uri, "audio/*");
        return intent;
	}
	
	/**
	 * android获取一个用于打开HTML文件的intent
	 * @update 2015年3月3日 下午5:32:41
	 * @param file
	 * @return
	 */
	public static Intent getHtmlFileIntent(File file) {
		Uri uri = Uri.parse(file.toString()).buildUpon()
				.encodedAuthority("com.android.htmlfileprovider")
				.scheme("content").encodedPath(file.toString()).build();
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(uri, "text/html");
		return intent;
	}

	/**
	 * android获取一个用于打开图片文件的intent
	 * @update 2015年3月3日 下午5:32:38
	 * @param file
	 * @return
	 */
	public static Intent getImageFileIntent(File file) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Uri uri = Uri.fromFile(file);
		intent.setDataAndType(uri, "image/*");
		return intent;
	}

	/**
	 * android获取一个用于打开PDF文件的intent
	 * @update 2015年3月3日 下午5:32:36
	 * @param file
	 * @return
	 */
	public static Intent getPdfFileIntent(File file) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Uri uri = Uri.fromFile(file);
		intent.setDataAndType(uri, "application/pdf");
		return intent;
	}

	/**
	 * android获取一个用于打开文本文件的intent
	 * @update 2015年3月3日 下午5:32:32
	 * @param file
	 * @return
	 */
	public static Intent getTextFileIntent(File file) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Uri uri = Uri.fromFile(file);
		intent.setDataAndType(uri, "text/plain");
		return intent;
	}

	/**
	 * android获取一个用于打开视频文件的intent
	 * @update 2015年3月3日 下午5:32:29
	 * @param file
	 * @return
	 */
	public static Intent getVideoFileIntent(File file) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
//		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra("oneshot", 0);
		intent.putExtra("configchange", 0);
		Uri uri = Uri.fromFile(file);
		intent.setDataAndType(uri, "video/*");
		return intent;
	}

	/**
	 * android获取一个用于打开CHM文件的intent
	 * @update 2015年3月3日 下午5:31:24
	 * @param file
	 * @return
	 */
	public static Intent getChmFileIntent(File file) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Uri uri = Uri.fromFile(file);
		intent.setDataAndType(uri, "application/x-chm");
		return intent;
	}

	/**
	 * android获取一个用于打开Word文件的intent
	 * @update 2015年3月3日 下午5:31:20
	 * @param file
	 * @return
	 */
	public static Intent getWordFileIntent(File file) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Uri uri = Uri.fromFile(file);
		intent.setDataAndType(uri, "application/msword");
		return intent;
	}

	/**
	 * android获取一个用于打开Excel文件的intent
	 * @update 2015年3月3日 下午5:30:30
	 * @param file
	 * @return
	 */
	public static Intent getExcelFileIntent(File file) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Uri uri = Uri.fromFile(file);
		intent.setDataAndType(uri, "application/vnd.ms-excel");
		return intent;
	}

	/**
	 * android获取一个用于打开PPT文件的intent
	 * @update 2015年3月3日 下午5:30:22
	 * @param file
	 * @return
	 */
	public static Intent getPPTFileIntent(File file) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Uri uri = Uri.fromFile(file);
		intent.setDataAndType(uri, "application/vnd.ms-powerpoint");
		return intent;
	}

	/**
	 * android获取一个用于打开apk文件的intent
	 * @update 2015年3月3日 下午5:30:18
	 * @param file
	 * @return
	 */
	public static Intent getApkFileIntent(File file) {
		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setAction(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.fromFile(file),
				"application/vnd.android.package-archive");
		return intent;
	}
    
	/**
	 * android获取一个用于打开文件的intent
	 * @update 2015年3月3日 下午5:30:14
	 * @param file
	 * @return
	 */
	public static Intent getFileIntent(File file) {
		return getFileIntent(file, null);
	}
	
	/**
	 * android获取一个用于打开文件的intent
	 * @update 2015年3月3日 下午7:07:48
	 * @param file
	 * @param mimeType
	 * @return
	 */
	public static Intent getFileIntent(File file, String mimeType) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		String mime = null;
		if (TextUtils.isEmpty(mimeType)) {
			String subfix = SystemUtil.getFileSubfix(file);
			if (!TextUtils.isEmpty(subfix)) {
				mime = guessMimeTypeFromExtension(subfix);
			}
			if (TextUtils.isEmpty(mime)) {
				mime = MIME_FILE;
			}
		} else {
			mime = mimeType;
		}
		Uri uri = Uri.fromFile(file);
		intent.setDataAndType(uri, mime);
		return intent;
	}

	private MimeUtils() {
	}
}
