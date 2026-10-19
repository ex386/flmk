package com.lab.fuumod;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Entry implements IXposedHookLoadPackage {

    static final String PKG = "com.fuulea.venus.g";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        if (!PKG.equals(lp.packageName)) return;

        // 顺序很重要：trace 通道 -> 真实快照 -> 上伪装 -> 观察 UUID
        try {
            XposedHelpers.findAndHookMethod("android.app.ActivityThread", lp.classLoader,
                    "handleBindApplication", "android.app.ActivityThread$AppBindData",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam mp) {
                            try {
                                Object data = mp.args[0];
                                Context app = (Context) XposedHelpers.getObjectField(data, "appContext");
                                if (app == null) return;
                                Trace.init(app);
                                RealSnapshot.dump(app);
                                DeviceProfile p = DeviceProfile.from(CfgStore.readInApp(app));
                                Trace.snapshot("spoof_profile", p.toJson().toString());
                                SpoofEngine.apply(p);
                                SpoofEngine.traceUuid();
                            } catch (Throwable t) {
                                XposedBridge.log("FuuMod init: " + t);
                            }
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log("FuuMod hook bind: " + t);
        }

        // 悬浮「其他」入口
        try {
            XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam mp) {
                    Activity a = (Activity) mp.thisObject;
                    if (a == null || !a.getClass().getName().startsWith(PKG)) return;
                    new Handler(Looper.getMainLooper()).post(() -> FloatEntry.show(a, v -> {
                        Intent i = new Intent(a, Gate.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        a.startActivity(i);
                    }));
                }
            });
        } catch (Throwable t) {
            XposedBridge.log("FuuMod float: " + t);
        }
    }
}
