package com.lab.fuumod;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.util.LinkedHashSet;
import java.util.Set;

import de.robv.android.xposed.XposedBridge;

/** 采集 / 导出：伪装前的真实快照 + 每次身份读取的调用点。 */
public final class Trace {

    public static final String PATH_HINT =
            "/sdcard/Android/data/com.fuulea.venus.g/files/fuumod/trace.jsonl";

    private static File out;
    private static final Set<String> seen = new LinkedHashSet<>();

    public static void init(Context ctx) {
        File base = null;
        try {
            base = ctx.getExternalFilesDir(null);
        } catch (Throwable ignored) {
        }
        if (base == null) base = new File(Environment.getExternalStorageDirectory(), "FuuMod");
        File dir = new File(base, "fuumod");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        out = new File(dir, "trace.jsonl");
        // 每次进程启动截断，避免跨天混淆
        try {
            if (out.exists()) {
                //noinspection ResultOfMethodCallIgnored
                out.delete();
            }
        } catch (Throwable ignored) {
        }
        XposedBridge.log("FuuMod trace -> " + out.getAbsolutePath());
    }

    public static String path() {
        return out == null ? "-" : out.getAbsolutePath();
    }

    public static synchronized void rec(String api, Object value) {
        String caller = caller();
        String key = api + "|" + caller;
        if (!seen.add(key)) return;
        write("{\"t\":\"read\",\"api\":\"" + esc(api) + "\",\"value\":\"" + esc(String.valueOf(value))
                + "\",\"caller\":\"" + esc(caller) + "\"}");
    }

    public static synchronized void snapshot(String tag, String json) {
        write("{\"t\":\"" + esc(tag) + "\",\"data\":" + json + "}");
    }

    private static void write(String line) {
        XposedBridge.log("FuuMod " + line);
        if (out == null) return;
        try (FileWriter w = new FileWriter(out, true)) {
            w.write(line);
            w.write('\n');
        } catch (Throwable ignored) {
        }
    }

    /** 第一个非系统 / 非模块帧 = App 读身份的落点。 */
    private static String caller() {
        for (StackTraceElement e : new Throwable().getStackTrace()) {
            String c = e.getClassName();
            if (c.startsWith("android.") || c.startsWith("java.") || c.startsWith("javax.")
                    || c.startsWith("dalvik.") || c.startsWith("libcore.") || c.startsWith("kotlin.")
                    || c.startsWith("com.lab.fuumod") || c.startsWith("de.robv")) continue;
            return c + "#" + e.getMethodName() + ":" + e.getLineNumber();
        }
        return "-";
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");
    }
}
