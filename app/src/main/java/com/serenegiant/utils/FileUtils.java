package com.serenegiant.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtils {

    public static String DIR_NAME = "UsbWebCamera";
	private static final SimpleDateFormat mDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);

    /**
     * キャプチャ用のファイル名を生成
     * @param type Environment.DIRECTORY_MOVIES / Environment.DIRECTORY_DCIM
     * @param ext .mp4 または .png
     * @return 書き込み出来なければnullを返す
     */
    public static final File getCaptureFile(final Context context, final String type, final String ext, final boolean preferSD) {
    	// 保存先のファイル名を生成
		final File dir = getCaptureDir(context, type, preferSD);
        if (dir != null) {
        	return new File(dir, getDateTimeString() + ext);
        }
    	return null;
    }

	public static final File getCaptureFile(final String type, final String ext) {
		final File dir = new File(Environment.getExternalStoragePublicDirectory(type), DIR_NAME);
		dir.mkdirs();
		if (dir.canWrite()) {
        	return new File(dir, getDateTimeString() + ext);
		}
		return null;
	}

	private static final Pattern mPathPattern = Pattern.compile("(.*/)(Android/data/)(.*)");
	private static final Pattern mEmulatedPattern = Pattern.compile("(.*/)(emulated/)(.*)");
	@SuppressWarnings("unused")
	@SuppressLint("NewApi")
	public static final File getCaptureDir(final Context context, final String type, final boolean preferSD) {
    	if (false/*preferSD*/) {
/*        	final String ext_mount = getExternalMounts();
        	if (!TextUtils.isEmpty(ext_mount)) {
				try {
        		final File path = new File(new File(ext_mount + type), DIR_NAME);
				if (path.mkdirs() || path.isDirectory()) {
					final File dummy = new File(path, getDateTimeString());
						if (dummy.createNewFile()) {
							dummy.delete();
							Log.i("FileUtils:", "found:" + path.toString());
							return path;
						}
					if (path.canWrite()) {
						Log.i("FileUtils:", "found:" + path.toString());
						return path;
					} else {
						Log.w("FileUtils", "can not write:" + path.toString());
					}
				} else {
					Log.w("FileUtils", "can not create:" + path.toString());
				}
				} catch (final IOException e) {
					Log.e("FileUtils", "", e);
				}
        	} */
	    	if (BuildCheck.isKitKat()) {
	    		// Context#getExternalFilesDirsが返すのはアプリケーション専用のストレージ領域
	    		final File[] ext_dirs = context.getExternalFilesDirs(type);
	    		if (ext_dirs != null) {
					Log.i("FileUtils", "ext:n=" + (ext_dirs != null ? ext_dirs.length : 0));
	    			for (int i = ext_dirs.length - 1; i >= 0; i--) {
	    				Log.i("FileUtils", "ext:" + ext_dirs[i].toString());
	    				final Matcher emulated = mEmulatedPattern.matcher(ext_dirs[i].toString());
	    				if (emulated.find()) continue;
//	    				return ext_dirs[i];
						try {
		    				final Matcher mt = mPathPattern.matcher(ext_dirs[i].toString());
		    				if (mt.find()) {
//			    				Log.i("FileUtils", mt.group(0));	// これは入力文字列全体
//			    				Log.i("FileUtils", "\t1:" + mt.group(1));		// これはユーザー毎のトップディレクトリ
//			    				Log.i("FileUtils", "\t\t2:" + mt.group(2));		// "Android/data/"
//			    				Log.i("FileUtils", "\t\t\t3:" + mt.group(3));	// ここにパッケージ名以下のディレクトリパスが入る
			    				final String path_str = mt.group(1);
			    				if (!TextUtils.isEmpty(path_str)) {
			    					final File path = new File(new File(new File(path_str), type), DIR_NAME);
			    					if (path.mkdirs() || path.isDirectory()) {
				    					final File dummy = new File(path, getDateTimeString());
				    					if (dummy.createNewFile()) {
				    						dummy.delete();
//				    						Log.i("FileUtils:", "found:" + path.toString());
				    						return path;
				    					}
				    					if (path.canWrite()) {
//				    						Log.i("FileUtils:", "found:" + path.toString());
				    						return path;
				    					} else {
//				    						Log.w("FileUtils", "can not write:" + path.toString());
				    					}
			    					} else {
//			    						Log.w("FileUtils", "can not create:" + path.toString());
			    					}
			    				}
		    				}
						} catch (final Exception e) {
//							Log.e("FileUtils", "", e);
						}
	    			}
	    		}
/*		    	if (BuildCheck.isLollipop()) {
		    		// これで取得できるのはgetExternalFilesDirsで取得できる各タイプ毎のディレクトリの2つ上
		    		// この下にfiles/タイプ毎ディレクトリがくる・・・プライマリストレージだけ
		    		final File[] media_dirs = context.getExternalMediaDirs();
		    		if (media_dirs != null) {
		    			for (int i = media_dirs.length-1; i >= 0; i--)
		    				Log.i("FileUtils", "media:" + media_dirs[i].toString());
		    		}
		    	} */
	    	}	// if (BuildCheck.isKitKat())
    	} // if (preferSD)
		final File dir = new File(Environment.getExternalStoragePublicDirectory(type), DIR_NAME);
//		Log.i("FileUtils", "pub:" + dir.toString());
		dir.mkdirs();	// Nexus5だとパスが全部存在しないと値がちゃんと返ってこないのでパスを生成
        if (dir.canWrite()) {
        	return dir;
        }
		return null;
    }
    /**
     * 現在の日時を表す文字列を取得する
     * @return
     */
    private static final String getDateTimeString() {
    	final GregorianCalendar now = new GregorianCalendar();
    	return mDateTimeFormat.format(now.getTime());
    }


/*    public static String getExternalMounts() {
    	String externalpath = null;
    	String internalpath = new String();

    	final Runtime runtime = Runtime.getRuntime();
    	try {
    		String line;
    		final Process proc = runtime.exec("mount");
    		final BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
    		while ((line = br.readLine()) != null) {
//    			Log.i("FileUtils", "getExternalMounts:" + line);
    			if (line.contains("secure")) continue;
    			if (line.contains("asec")) continue;

    			if (line.contains("fat")) {//external card
    				final String columns[] = line.split(" ");
    				if (columns != null && (columns.length > 1) && !TextUtils.isEmpty(columns[1])) {
    					externalpath = columns[1];
    					if (!externalpath.endsWith("/")) {
    						externalpath = externalpath + "/";
    					}
    				}
    			} else if (line.contains("fuse")) {//internal storage
    				final String columns[] = line.split(" ");
    				if (columns != null && columns.length > 1) {
    					internalpath = internalpath.concat("[" + columns[1] + "]");
    				}
    			}
    		}
    	}
    	catch(final Exception e) {
    	    e.printStackTrace();
    	}
//    	Log.i("FileUtils", "Path  of sd card external: " + externalpath);
//    	Log.i("FileUtils", "Path  of internal memory: " + internalpath);
    	return externalpath;
    } */

	// 外部ストレージの空き容量の制限(1分に10MBとみなす。実際は7〜8MB)
    public static float FREE_RATIO = 0.03f;					// 空き領域が3%より大きいならOK
    public static float FREE_SIZE_OFFSET = 20 * 1024 * 1024;
    public static float FREE_SIZE = 300 * 1024 * 1024;		// 空き領域が300MB以上ならOK
    public static float FREE_SIZE_MINUTE = 40 * 1024 * 1024;	// 1分当たりの動画容量(5Mbpsで38MBぐらいなので)
	public static long CHECK_INTERVAL = 45 * 1000L;	// 空き容量,EOSのチェクする間隔[ミリ秒](=45秒)

    /**
     * 外部ストレージの空き容量のチェック
     * 外部ストレージの空き容量がFREE_RATIO(5%)以上かつFREE_SIZE(20MB)以上ならtrueを返す
     * @return 使用可能であればtrue
     */
    public static final boolean checkFreeSpace(final Context context,
    	final long max_duration, final long start_time, final boolean preferSD) {

    	if (context == null) return false;
    	return checkFreeSpace(context, FREE_RATIO,
    		max_duration > 0	// 最大録画時間が設定されている時
        	? (max_duration - (System.currentTimeMillis() - start_time)) / 60000.f * FREE_SIZE_MINUTE + FREE_SIZE_OFFSET
        	: FREE_SIZE, preferSD);
    }

    /**
     * 外部ストレージの空き容量のチェック
     * @param ratio 空き容量の割合(0-1]
     * @param minFree 最小空き容量[バイト]
     * @return 使用可能であればtrue
     */
    public static final boolean checkFreeSpace(final Context context, final float ratio, final float minFree, final boolean preferSD) {
//    	if (DEBUG) Log.v(TAG, String.format("checkFreeSpace:ratio=%f,min=%f", ratio, minFree));
    	if (context == null) return false;
    	boolean result = false;
//		final String state = Environment.getExternalStorageState();
//		if (Environment.MEDIA_MOUNTED.equals(state) ||
//		    !Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // 外部保存領域が書き込み可能な場合
			try {
				final File dir = FileUtils.getCaptureDir(context, Environment.DIRECTORY_MOVIES, preferSD);
				final float freeSpace = (dir != null) && dir.canWrite() ? dir.getUsableSpace() : 0;
				if (dir.getTotalSpace() > 0)
					result = (freeSpace / dir.getTotalSpace() > ratio) || (freeSpace > minFree);
			} catch (final Exception e) {
				Log.w("checkFreeSpace:", e);
			}
//		}
        return result;
    }

}
