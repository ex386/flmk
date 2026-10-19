package com.lab.fuumod;

import android.content.ContentResolver;
import android.net.wifi.WifiInfo;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/** 伪装层：把身份类 API 的返回值替换成 DeviceProfile。 */
public final class SpoofEngine {

    private static final Set<String> TEL = new HashSet<>(Arrays.asList(
            "getImei", "getDeviceId", "getMeid", "getSubscriberId",
            "getSimSerialNumber", "getLine1Number", "getVoiceMailNumber"));

    public static void apply(final DeviceProfile p) {
        buildFields(p);

        try {
            XposedHelpers.findAndHookMethod(Build.class, "getSerial", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam mp) {
                    mp.setResult(p.serial);
                    Trace.rec("Build.getSerial", p.serial);
                }
            });
        } catch (Throwable ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(Build.class, "getRadioVersion", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam mp) {
                    mp.setResult(p.radio);
                    Trace.rec("Build.getRadioVersion", p.radio);
                }
            });
        } catch (Throwable ignored) {
        }

        XC_MethodHook androidIdHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam mp) {
                if (mp.args.length < 2 || !(mp.args[1] instanceof String)) return;
                if (!Settings.Secure.ANDROID_ID.equals(mp.args[1])) return;
                mp.setResult(p.androidId);
                Trace.rec("Settings.Secure.getString(android_id)", p.androidId);
            }
        };
        try {
            XposedHelpers.findAndHookMethod(Settings.Secure.class, "getString",
                    ContentResolver.class, String.class, androidIdHook);
        } catch (Throwable ignored) {
        }
        try {
            XposedHelpers.findAndHookMethod(Settings.Secure.class, "getStringForUser",
                    ContentResolver.class, String.class, int.class, androidIdHook);
        } catch (Throwable ignored) {
        }

        for (final Method m : TelephonyManager.class.getDeclaredMethods()) {
            if (!TEL.contains(m.getName()) || m.getReturnType() != String.class) continue;
            try {
                XposedBridge.hookMethod(m, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam mp) {
                        mp.setResult(p.imei);
                        Trace.rec("TelephonyManager." + m.getName(), p.imei);
                    }
                });
            } catch (Throwable ignored) {
            }
        }

        try {
            XposedHelpers.findAndHookMethod(WifiInfo.class, "getMacAddress", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam mp) {
                    mp.setResult(p.mac);
                    Trace.rec("WifiInfo.getMacAddress", p.mac);
                }
            });
        } catch (Throwable ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(NetworkInterface.class, "getHardwareAddress",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam mp) {
                            mp.setResult(macBytes(p.mac));
                            Trace.rec("NetworkInterface.getHardwareAddress", p.mac);
                        }
                    });
        } catch (Throwable ignored) {
        }
    }

    private static void buildFields(DeviceProfile p) {
        String[][] kv = {
                {"MANUFACTURER", p.manufacturer},
                {"BRAND", p.brand},
                {"MODEL", p.model},
                {"DEVICE", p.device},
                {"PRODUCT", p.product},
                {"BOARD", p.board},
                {"HARDWARE", p.hardware},
                {"ID", p.id},
                {"FINGERPRINT", p.fingerprint},
                {"BOOTLOADER", p.bootloader},
                {"DISPLAY", p.id},
                {"TAGS", "release-keys"},
        };
        for (String[] e : kv) {
            try {
                XposedHelpers.setStaticObjectField(Build.class, e[0], e[1]);
            } catch (Throwable t) {
                XposedBridge.log("FuuMod: Build." + e[0] + " " + t);
            }
        }
    }

    private static byte[] macBytes(String mac) {
        String[] s = mac.split(":");
        byte[] b = new byte[s.length];
        for (int i = 0; i < s.length; i++) {
            try {
                b[i] = (byte) Integer.parseInt(s[i], 16);
            } catch (Throwable t) {
                b[i] = 0;
            }
        }
        return b;
    }

    /** 只观察不改：guid 若由此生成，trace 里会出现这条记录。 */
    public static void traceUuid() {
        try {
            XposedHelpers.findAndHookMethod(java.util.UUID.class, "randomUUID", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam mp) {
                    Trace.rec("UUID.randomUUID", mp.getResult());
                }
            });
        } catch (Throwable ignored) {
        }
    }
}
