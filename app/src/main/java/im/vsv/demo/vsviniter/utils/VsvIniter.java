package im.vsv.demo.vsviniter.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import im.vsv.demo.vsviniter.R;

/**
 * 解压视频解码模块
 * <p/>
 * Tips : 如果修改解码器压缩包的内容，必须增加版本号 VSV_LIB_VERSION, 且保证压缩包内文件内容和 LIBS_IJk_PLAYER 一致。
 *
 * @author shenguanchu.
 */
public class VsvIniter {

  private static String TAG = "VsvIniter";
  private static final String LIBS_LOCK = ".lock";
  private static final int VSV_LIB_VERSION = 1; // 更新压缩文件之后修改版本号，用于区分压缩文件
  private static final String COMPRESS_LIBS_NAME = "vsvlibs.7z";
  private static final String LIBS_COMPRESS_PATH = "vsvlibs/";

  private static final String[] LIBS_IJk_PLAYER = {"libijkffmpeg.so", "libijkplayer.so",
      "libijksdl.so"}; // 根据压缩包内容填写，用于验证解压完整性,不可随意更改

  private static String LIB_TYPE = "arm";

  public static boolean init(Context context) {
    return isInitialized(context) || unZipLibs(context);
  }

  private static boolean unZipLibs(Context context) {
    long begin = System.currentTimeMillis();
    Log.d(TAG, "loadLibs start " + begin);
    String libPath = getLibPath(context);
    File lock = new File(libPath + LIBS_LOCK);
    if (lock.exists()) {
      if (false == lock.delete()) {
        Log.e(TAG, "extractLibs: delete lock file fail");
      }
    }

    // clean path
    deleteRecursive(new File(libPath));
    int rawId = R.raw.vsvlibs;
    // todo 区分 arm 和 x86 分别解压，加快解压速度。
    String compressFilePath = copyCompressedLib(context, rawId);
    Log.d(TAG, "copyCompressedLib time: "
        + (System.currentTimeMillis() - begin));
    int inited = initializeLibs(compressFilePath, libPath, LIB_TYPE);

    File oldZip = new File(compressFilePath);
    if (false == oldZip.delete()) {
      Log.e(TAG, "extractLibs: cannot delete zip file:" + libPath);
    } else {
      Log.d(TAG, "extractLibs: delete zip file:" + libPath);
    }
    FileWriter fw = null;
    try {
      if (false == lock.createNewFile()) {
        Log.e(TAG, "extractLibs: createNewFile return false");
      } else {
        // Log.e(TAG,"extractLibs: createNewFile return true");
      }
      fw = new FileWriter(lock);
      fw.write(String.valueOf(VSV_LIB_VERSION));
      return true;
    } catch (IOException e) {
      Log.e(TAG, "Error creating lock file, " + e.toString());
    } finally {
      Log.d(TAG, "initializeNativeLibs: " + inited + ", libsType:"
          + LIB_TYPE);
      Log.d(TAG, "loadLibs time: " + (System.currentTimeMillis() - begin));
      IOUtils.close(fw);
    }
    return false;
  }

  public static boolean isInitialized(Context context) {
    String libPath = getLibPath(context);
    File dir = new File(libPath);
    if (dir.exists() && dir.isDirectory()) {
      String[] libs = dir.list();
      if (libs != null) {
        Arrays.sort(libs);
        for (String L : LIBS_IJk_PLAYER) {
          if (Arrays.binarySearch(libs, L) < 0) {
            Log.e(TAG, "Native libs " + L + " not exists!");
            return false; // /发现异常后应该重新解压一遍
          }
        }
        File lock = new File(libPath + LIBS_LOCK); // 写版本文件 copy from sina
        BufferedReader buffer = null;
        try {
          buffer = new BufferedReader(new FileReader(lock));
          int libVersion = Integer.valueOf(buffer.readLine());
          Log.i(TAG, "isNativeLibsInited, LIB VERSION: " + VSV_LIB_VERSION
              + ", current version: " + libVersion);
          if (libVersion == VSV_LIB_VERSION)
            return true;
        } catch (IOException e) {
          Log.e(TAG, "isNativeLibsInited error," + e.toString());
        } catch (NumberFormatException e) {
          Log.e(TAG, "isNativeLibsInited error, " + e.toString());
        } finally {
          IOUtils.close(buffer);
        }
      }
    }
    return false;
  }

  private static String copyCompressedLib(Context context, int rawID) {
    byte[] buffer = new byte[1024];
    InputStream is = null;
    BufferedInputStream bis = null;
    FileOutputStream fos = null;
    String destPath = null;

    try {
      try {
        String dir = getLibPath(context);
        destPath = dir + COMPRESS_LIBS_NAME;
        File f = new File(dir);
        if (f.exists() && !f.isDirectory())
          f.delete();
        if (!f.exists())
          f.mkdirs();
        f = new File(destPath);
        if (f.exists() && !f.isFile())
          f.delete();
        if (!f.exists())
          f.createNewFile();
      } catch (Exception fe) {
        Log.e(TAG, "loadLib error, " + fe.toString());
      }

      is = context.getResources().openRawResource(rawID);
      bis = new BufferedInputStream(is);
      fos = new FileOutputStream(destPath);
      while (bis.read(buffer) != -1) {
        fos.write(buffer);
      }
    } catch (Exception e) {
      Log.e(TAG, "loadLib error, " + e.toString());
      return null;
    } finally {
      IOUtils.close(fos);
      IOUtils.close(bis);
      IOUtils.close(is);
    }

    return destPath;
  }

  public static String getLibPath(Context context) {
    return getDataDir(context) + LIBS_COMPRESS_PATH;
  }

  private static String getDataDir(Context context) {
    ApplicationInfo ai = context.getApplicationInfo();
    if (ai.dataDir != null)
      return fixLastSlash(ai.dataDir);
    else
      return "/data/data/" + ai.packageName + "/";
  }

  private static String fixLastSlash(String str) {
    String res = str == null ? "/" : str.trim() + "/";
    if (res.length() > 2 && res.charAt(res.length() - 2) == '/')
      res = res.substring(0, res.length() - 1);
    return res;
  }

  private static void deleteRecursive(File fileOrDirectory) {
    if (fileOrDirectory.isDirectory())
      for (File child : fileOrDirectory.listFiles())
        deleteRecursive(child);

    fileOrDirectory.delete();
  }

  static {
    System.loadLibrary("vsvinit");
  }

  private native static int initializeLibs(String libPath, String destDir, String prefix);
}
