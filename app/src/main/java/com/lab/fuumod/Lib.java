package com.lab.fuumod;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 小说 / 视频库：记录通过系统文件选择器「导入」的文件 URI。
 * 导入时已 takePersistableUriPermission，重启后仍可读；这里只存元数据。
 */
public class Lib {

    public static final int NOVEL = 0;
    public static final int VIDEO = 1;

    public static class Item {
        public String uri = "";
        public String name = "";
        public long added = 0;
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("lib", Context.MODE_PRIVATE);
    }

    private static String key(int kind) {
        return kind == VIDEO ? "video" : "novel";
    }

    public static List<Item> list(Context c, int kind) {
        List<Item> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(sp(c).getString(key(kind), "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Item it = new Item();
                it.uri = o.optString("uri");
                it.name = o.optString("name");
                it.added = o.optLong("added");
                if (!it.uri.isEmpty()) out.add(it);
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    /** 返回 true 表示新增；同一 URI 重复导入视为已存在。 */
    public static boolean add(Context c, int kind, String uri, String name) {
        List<Item> items = list(c, kind);
        for (Item it : items) if (it.uri.equals(uri)) return false;
        Item n = new Item();
        n.uri = uri;
        n.name = (name == null || name.isEmpty()) ? uri : name;
        n.added = System.currentTimeMillis();
        items.add(n);
        save(c, kind, items);
        return true;
    }

    public static void remove(Context c, int kind, String uri) {
        List<Item> items = list(c, kind);
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).uri.equals(uri)) items.remove(i);
        }
        save(c, kind, items);
    }

    /** 只清记录，不动用户文件。 */
    public static void clear(Context c, int kind) {
        sp(c).edit().remove(key(kind)).apply();
    }

    private static void save(Context c, int kind, List<Item> items) {
        JSONArray a = new JSONArray();
        for (Item it : items) {
            JSONObject o = new JSONObject();
            try {
                o.put("uri", it.uri);
                o.put("name", it.name);
                o.put("added", it.added);
                a.put(o);
            } catch (Throwable ignored) {
            }
        }
        sp(c).edit().putString(key(kind), a.toString()).apply();
    }
}
