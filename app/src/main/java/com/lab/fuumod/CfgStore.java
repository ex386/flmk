package com.lab.fuumod;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

/** 配置读写：模块侧写 SharedPreferences，注入侧用 XSharedPreferences 读。 */
public final class CfgStore {

    private static final String FILE = "cfg";
    private static final String KEY = "profile_json";

    /** 注入进目标 App 的进程里读。 */
    public static JSONObject readInApp(Context ctx) {
        try {
            XSharedPreferences p = new XSharedPreferences(BuildConfig.APPLICATION_ID, FILE);
            p.reload();
            String s = p.getString(KEY, null);
            if (s != null) return new JSONObject(s);
        } catch (Throwable t) {
            XposedBridge.log("FuuMod cfg(prefs): " + t);
        }
        // 回退：手动放到 Download 的同一份 json
        try {
            File f = new File("/sdcard/Download/FuuMod/cfg.json");
            if (f.exists()) {
                return new JSONObject(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
            }
        } catch (Throwable t) {
            XposedBridge.log("FuuMod cfg(file): " + t);
        }
        return null;
    }

    /** 模块 App 自己读。 */
    public static JSONObject readOwn(Context ctx) {
        try {
            String s = prefs(ctx).getString(KEY, null);
            return s == null ? null : new JSONObject(s);
        } catch (Throwable t) {
            return null;
        }
    }

    /** 模块 App 自己写；同时镜像一份到 Download 方便排查。 */
    public static void writeOwn(Context ctx, DeviceProfile p) {
        String json = p.toJson().toString();
        prefs(ctx).edit().putString(KEY, json).apply();
        try {
            File dir = new File("/sdcard/Download/FuuMod");
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
            Files.write(new File(dir, "cfg.json").toPath(), json.getBytes(StandardCharsets.UTF_8));
        } catch (Throwable ignored) {
        }
    }

    public static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static boolean gateHash(Context ctx) {
        return prefs(ctx).getString("gate_hash", null) != null;
    }

    public static String gateHashValue(Context ctx) {
        return prefs(ctx).getString("gate_hash", null);
    }

    public static void setGateHash(Context ctx, String hash) {
        prefs(ctx).edit().putString("gate_hash", hash).apply();
    }
}
