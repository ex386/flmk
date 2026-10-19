package com.lab.fuumod;

import org.json.JSONObject;

/** 可伪装的身份字段集合，字段名与 Build / Settings 中的语义一致。 */
public class DeviceProfile {

    public String manufacturer = "Xiaomi";
    public String brand = "Xiaomi";
    public String model = "M2012K11AC";
    public String device = "alioth";
    public String product = "alioth";
    public String board = "alioth";
    public String hardware = "qcom";
    public String id = "SKQ1.211006.001";
    public String fingerprint = "";
    public String serial = "SERIAL";
    public String androidId = "0123456789abcdef";
    public String imei = "000000000000000";
    public String mac = "02:00:00:00:00:00";
    public String radio = "unknown";
    public String bootloader = "unknown";

    public static DeviceProfile from(JSONObject o) {
        DeviceProfile p = new DeviceProfile();
        if (o == null) return p;
        p.manufacturer = o.optString("manufacturer", p.manufacturer);
        p.brand = o.optString("brand", p.brand);
        p.model = o.optString("model", p.model);
        p.device = o.optString("device", p.device);
        p.product = o.optString("product", p.product);
        p.board = o.optString("board", p.board);
        p.hardware = o.optString("hardware", p.hardware);
        p.id = o.optString("id", p.id);
        p.fingerprint = o.optString("fingerprint", "");
        p.serial = o.optString("serial", p.serial);
        p.androidId = o.optString("androidId", p.androidId);
        p.imei = o.optString("imei", p.imei);
        p.mac = o.optString("mac", p.mac);
        p.radio = o.optString("radio", p.radio);
        p.bootloader = o.optString("bootloader", p.bootloader);
        if (p.fingerprint.isEmpty()) p.fingerprint = p.buildFingerprint();
        return p;
    }

    public String buildFingerprint() {
        return brand + "/" + product + "/" + device + ":11/" + id + "/eng.release-keys";
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("manufacturer", manufacturer);
            o.put("brand", brand);
            o.put("model", model);
            o.put("device", device);
            o.put("product", product);
            o.put("board", board);
            o.put("hardware", hardware);
            o.put("id", id);
            o.put("fingerprint", fingerprint);
            o.put("serial", serial);
            o.put("androidId", androidId);
            o.put("imei", imei);
            o.put("mac", mac);
            o.put("radio", radio);
            o.put("bootloader", bootloader);
        } catch (Throwable ignored) {
        }
        return o;
    }

    /** SettingsActivity 用：按字段名读写，避免一大堆 setter。 */
    public String get(String key) {
        try {
            Object v = getClass().getField(key).get(this);
            return v == null ? "" : v.toString();
        } catch (Throwable t) {
            return "";
        }
    }

    public void set(String key, String value) {
        try {
            getClass().getField(key).set(this, value == null ? "" : value);
        } catch (Throwable ignored) {
        }
    }

    public static final String[] KEYS = {
            "manufacturer", "brand", "model", "device", "product", "board", "hardware",
            "id", "fingerprint", "serial", "androidId", "imei", "mac", "radio", "bootloader"
    };
}
