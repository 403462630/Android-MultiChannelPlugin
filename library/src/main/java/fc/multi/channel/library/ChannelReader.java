package fc.multi.channel.library;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.text.TextUtils;
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
    private static final String APP_CHANNEL_ID = "app_channel_id";
    private static final String APP_CHANNEL_EXT_INFO = "app_channel_ext_info";
    private final static String DEFAULT_CHANNEL_ID = "0";
    private static SharedPreferences getSharePreferences(Context context) {
        return context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    public static void init(Context context) {
        update(context);
    }

    private static String update(Context context) {
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

        if (TextUtils.isEmpty(ret)) {
            return DEFAULT_CHANNEL_ID;
        } else {
            String marketId = DEFAULT_CHANNEL_ID;
            final int contentMinLen = 3;
            String[] split = ret.split("-");
            if (split != null && split.length >= contentMinLen) {
                marketId = split[split.length - 1];
            }
            SharedPreferences.Editor editor = getSharePreferences(context).edit();
            editor.putString(APP_CHANNEL_ID, marketId);
            editor.putString(APP_CHANNEL_EXT_INFO, extInfo);
            editor.commit();
            return marketId;
        }
    }

    public static String getChannelId(Context context) {
        if (getSharePreferences(context).getString(APP_CHANNEL_ID, null) != null) {
            return getSharePreferences(context).getString(APP_CHANNEL_ID, DEFAULT_CHANNEL_ID);
        } else {
            return update(context);
        }
    }

    public static Map getExtInfo(Context context) {
        String value = getSharePreferences(context).getString(APP_CHANNEL_EXT_INFO, "");
        return new Gson().fromJson(value, Map.class);
    }
}
