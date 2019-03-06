package com.mcloyal.serialport;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.mcloyal.serialport.entity.Packet;
import com.mcloyal.serialport.utils.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Set;

/**
 * 文件数据操作类
 */
public class AppPreferences {
    /**
     * 登录帐号信息
     */
    public final static String SHARED_ACCOUNT = "share_account";

    private final static String SHARED_NAME = "share_name";
    private SharedPreferences sharedPreferences;
    private static AppPreferences appSharedPreferences;

    /**
     * 初始化缓存参数
     *
     * @param appLibsContext
     */
    private AppPreferences(AppLibsContext appLibsContext) {
        sharedPreferences = appLibsContext.getSharedPreferences(SHARED_NAME,
                AppLibsContext.MODE_PRIVATE);
    }

    /**
     * 单例模式
     *
     * @param appLibsContext
     * @return
     */
    public static AppPreferences getInstance(AppLibsContext appLibsContext) {
        if (appSharedPreferences == null) {
            appSharedPreferences = new AppPreferences(appLibsContext);
        }
        return appSharedPreferences;
    }

    public Map<String, ?> getAll() {
        return sharedPreferences.getAll();
    }

    public String getString(String key, String defValue) {
        return sharedPreferences.getString(key, defValue);
    }

    @SuppressLint("NewApi")
    public Set<String> getStringSet(String key, Set<String> defValues) {
        return sharedPreferences.getStringSet(key, defValues);
    }

    public int getInt(String key, int defValue) {
        return sharedPreferences.getInt(key, defValue);
    }

    public long getLong(String key, long defValue) {
        return sharedPreferences.getLong(key, defValue);
    }

    public float getFloat(String key, float defValue) {
        return sharedPreferences.getFloat(key, defValue);
    }

    public boolean getBoolean(String key, boolean defValue) {
        return sharedPreferences.getBoolean(key, defValue);
    }

    public boolean contains(String key) {
        return sharedPreferences.contains(key);
    }

    public Editor edit() {
        return sharedPreferences.edit();
    }


    /**
     * 退出应用程序的登录状态
     *
     * @return
     */
    public boolean exitLogin() {
        return edit().remove(SHARED_ACCOUNT).commit();
    }

    public boolean putString(String key, String value) {
        return edit().putString(key, value).commit();
    }

    @SuppressLint("NewApi")
    public boolean putStringSet(String key, Set<String> values) {
        return edit().putStringSet(key, values).commit();
    }

    public boolean putInt(String key, int value) {
        return edit().putInt(key, value).commit();
    }

    public boolean putLong(String key, long value) {
        return edit().putLong(key, value).commit();
    }

    public boolean putFloat(String key, float value) {
        return edit().putFloat(key, value).commit();
    }

    public boolean putBoolean(String key, boolean value) {
        return edit().putBoolean(key, value).commit();
    }

    public boolean remove(String key) {
        return edit().remove(key).commit();
    }

    public boolean clear() {
        return edit().clear().commit();
    }

    @SuppressLint("NewApi")
    public void apply() {
        edit().apply();
    }

    /**
     * 存储数据包
     *
     * @param packet
     * @return
     */
    public boolean savePacket(Packet packet) {
        ByteArrayOutputStream baos = null;
        ObjectOutputStream oos = null;
        try {
            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(packet);
            oos.flush();
            String base64String = new String(Base64.encode(baos.toByteArray()));
            Editor editor = edit();
            editor.putString(SHARED_ACCOUNT, base64String);
            editor.commit();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * 读取数据包
     *
     * @return
     */
    public Packet readPacket() {
        Packet packet = null;
        ByteArrayInputStream bais = null;
        ObjectInputStream bis = null;
        try {
            String base64String = sharedPreferences.getString(SHARED_ACCOUNT,
                    "");
            byte[] base64 = Base64.decode(base64String);
            bais = new ByteArrayInputStream(base64);
            bis = new ObjectInputStream(bais);
            packet = (Packet) bis.readObject();
        } catch (Exception e) {
            // e.printStackTrace();
        } finally {
            if (bais != null) {
                try {
                    bais.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return packet;
    }
}
