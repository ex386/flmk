package com.lab.fuumod;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import org.json.JSONObject;

/** 在伪装生效之前，把本机真实身份打一份快照。 */
public final class RealSnapshot {

    public static void dump(Context ctx) {
        JSONObject o = new JSONObject();
        try {
            o.put("MANUFACTURER", Build.MANUFACTURER);
            o.put("BRAND", Build.BRAND);
            o.put("MODEL", Build.MODEL);
            o.put("DEVICE", Build.DEVICE);
            o.put("PRODUCT", Build.PRODUCT);
            o.put("BOARD", Build.BOARD);
            o.put("HARDWARE", Build.HARDWARE);
            o.put("ID", Build.ID);
            o.put("FINGERPRINT", Build.FINGERPRINT);
            o.put("DISPLAY", Build.DISPLAY);
            o.put("TAGS", Build.TAGS);
            o.put("TYPE", Build.TYPE);
            o.put("HOST", Build.HOST);
            o.put("USER", Build.USER);
            o.put("SDK", Build.VERSION.SDK_INT);
            o.put("android_id", Settings.Secure.getString(
                    ctx.getContentResolver(), Settings.Secure.ANDROID_ID));
            try {
                o.put("serial", Build.getSerial());
            } catch (Throwable t) {
                o.put("serial", "denied");
            }
        } catch (Throwable ignored) {
        }
        Trace.snapshot("real_snapshot", o.toString());
    }
}
