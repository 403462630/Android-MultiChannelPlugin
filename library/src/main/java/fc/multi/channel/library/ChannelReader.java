package fc.multi.channel.library;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.Nullable;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by rjhy on 17-2-8.
 */
public class ChannelReader {
    private static final String FILE_NAME = "channel_file";
    private static final String KEY_APP_CHANNEL_ID = "app_channel_id";
    private static final String KEY_APP_DEFAULT_CHANNEL_ID = "app_default_channel_id";
    private static final String KEY_APP_CHANNEL_EXT_INFO = "app_channel_ext_info";
    private static final String KEY_APP_VERSION_CODE = "key_app_version_code";
    private final static String DEFAULT_CHANNEL_ID = "0";
    private static volatile String channelId;
    private static boolean debug; // 测试环境下每次app启动都从新解析apk的channelId
    private static SharedPreferences getSharePreferences(Context context) {
        return context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    /**
     * @param context
     * @param debug 为true时每次app启动都从新解析apk的channelId
     */
    public static void init(Context context, Boolean debug) {
        init(context, DEFAULT_CHANNEL_ID, debug);
    }

    /**
     *
     * @param context
     * @param defaultChannelId
     * @param debug 为true时每次app启动都从新解析apk的channelId
     */
    public static void init(Context context, String defaultChannelId, Boolean debug) {
        ChannelReader.debug = debug;
        saveDefaultChannelId(context, defaultChannelId);
        removeCachedChannelIdIfNeed(context);
        update(context, defaultChannelId);
    }

    /**
     *
     * @param context
     * @param defaultChannelId
     * @param debug 为true时每次app启动都从新解析apk的channelId
     */
    public static void initAsync(final Context context, final String defaultChannelId, Boolean debug) {
        ChannelReader.debug = debug;
        removeCachedChannelIdIfNeed(context);
        saveDefaultChannelId(context, defaultChannelId);
        new Thread(new Runnable() {
            @Override
            public void run() {
                update(context, defaultChannelId);
            }
        }).start();
    }

    private synchronized static String update(Context context, String defaultChannelId) {
        String cachedChannelId = getCachedChannelId(context);
        if (!TextUtils.isEmpty(cachedChannelId)) {
            ChannelReader.channelId = cachedChannelId;
            return cachedChannelId;
        }

        ApplicationInfo appinfo = context.getApplicationInfo();
        String sourceDir = appinfo.sourceDir;
        String ret = "";
        String extInfo = "";
        ZipFile zipfile = null;
        try {
            zipfile = new ZipFile(sourceDir);
            Enumeration<?> entries = zipfile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = ((ZipEntry) entries.nextElement());
                String entryName = entry.getName();
                if (entryName.startsWith("META-INF/fc-multi-channel-")) {
                    ret = entryName;
                    BufferedReader reader = null;
                    try {
                        reader = new BufferedReader(new InputStreamReader(zipfile.getInputStream(entry)));
                        extInfo = reader.readLine();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    } finally {
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (zipfile != null) {
                try {
                    zipfile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        SharedPreferences.Editor editor = getSharePreferences(context).edit();
        String marketId;
        if (TextUtils.isEmpty(ret)) {
            marketId = defaultChannelId;
        } else {
            marketId = defaultChannelId;
            final int contentMinLen = 3;
            String[] split = ret.split("-");
            if (split != null && split.length >= contentMinLen) {
                marketId = split[split.length - 1];
            }
            editor.putString(KEY_APP_CHANNEL_EXT_INFO, extInfo);
        }
        ChannelReader.channelId = marketId;

        editor.putString(KEY_APP_CHANNEL_ID, marketId);
        // 缓存当前app的versionCode
        editor.putLong(KEY_APP_VERSION_CODE, getAppVersionCode(context));
        editor.commit();

        return marketId;
    }

    private static long getCachedVersionCode(Context context) {
        return getSharePreferences(context).getLong(KEY_APP_VERSION_CODE, 0);
    }

    private static void removeCachedChannelIdIfNeed(Context context) {
        if (ChannelReader.debug) {
            SharedPreferences.Editor editor = getSharePreferences(context).edit();
            editor.clear();
            editor.commit();
        }
    }

    public static String getChannelId(Context context) {
        if (ChannelReader.channelId != null) {
            return ChannelReader.channelId;
        }

        String marketId = getCachedChannelId(context);
        if (!TextUtils.isEmpty(marketId)) {
            ChannelReader.channelId = marketId;
            return marketId;
        } else {
            return update(context, getDefaultChannelId(context));
        }
    }

    @Nullable
    private static String getCachedChannelId(Context context) {
        // 当前app版本AppVersionCode
        long currentVersionCode = getAppVersionCode(context);
        // 本地文件缓存的AppVersionCode
        long cachedVersionCode = getCachedVersionCode(context);
        String marketId = null;
        if (currentVersionCode == cachedVersionCode) {
            // 如果当前appVersionCode和缓存的appVersionCode一样，返回本地缓存的channelId
            marketId = getSharePreferences(context).getString(KEY_APP_CHANNEL_ID, null);
        }
        return marketId;
    }

    /**
     * 获取当前app version code
     */
    public static long getAppVersionCode(Context context) {
        long appVersionCode = 0;
        try {
            PackageInfo packageInfo = context.getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                appVersionCode = packageInfo.getLongVersionCode();
            } else {
                appVersionCode = packageInfo.versionCode;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("ChannelReader", e.getMessage());
        }
        return appVersionCode;
    }

    private static void saveDefaultChannelId(Context context, String defaultChannelId) {
        SharedPreferences.Editor editor = getSharePreferences(context).edit();
        editor.putString(KEY_APP_DEFAULT_CHANNEL_ID, defaultChannelId);
        editor.commit();
    }

    private static String getDefaultChannelId(Context context) {
        return getSharePreferences(context).getString(KEY_APP_DEFAULT_CHANNEL_ID, DEFAULT_CHANNEL_ID);
    }

    public static Map getExtInfo(Context context) {
        String value = getSharePreferences(context).getString(KEY_APP_CHANNEL_EXT_INFO, "");
        return new Gson().fromJson(value, Map.class);
    }
}
