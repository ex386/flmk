package com.lab.fuumod;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * 密码门之后的容器：小说 / 视频两个库。
 * 内容通过「导入文件」进入列表（系统文件选择器 + 持久读权限），之后点条目直接打开。
 */
public class AppsActivity extends AppCompatActivity {

    private int kind = Lib.NOVEL;
    private int pendingKind = Lib.NOVEL;
    private final List<Lib.Item> data = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private TextView empty;
    private ListView lv;
    private ActivityResultLauncher<String[]> picker;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        picker = registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(),
                uris -> {
                    if (uris == null || uris.isEmpty()) return;
                    int added = 0, dup = 0;
                    for (Uri u : uris) {
                        try {
                            getContentResolver().takePersistableUriPermission(
                                    u, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (Throwable ignored) {
                        }
                        if (Lib.add(this, pendingKind, u.toString(), displayName(u))) added++;
                        else dup++;
                    }
                    Toast.makeText(this, "导入 " + added + " 个" + (dup > 0 ? "，重复 " + dup + " 个" : ""),
                            Toast.LENGTH_SHORT).show();
                    reload();
                });

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        TabLayout tabs = new TabLayout(this);
        tabs.addTab(tabs.newTab().setText("小说"));
        tabs.addTab(tabs.newTab().setText("视频"));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab t) {
                kind = t.getPosition() == 0 ? Lib.NOVEL : Lib.VIDEO;
                reload();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab t) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab t) {
            }
        });
        root.addView(tabs);

        empty = new TextView(this);
        empty.setText("还没有内容\n点下面「导入文件」，从本机选小说(txt)或视频");
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(48, 140, 48, 48);
        root.addView(empty);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        lv = new ListView(this);
        lv.setAdapter(adapter);
        root.addView(lv, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(24, 12, 24, 24);
        Button imp = new Button(this);
        imp.setText("导入文件");
        Button cfg = new Button(this);
        cfg.setText("设置");
        Button clr = new Button(this);
        clr.setText("清空列表");
        bar.addView(imp, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        bar.addView(cfg, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        bar.addView(clr, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(bar);
        setContentView(root);

        imp.setOnClickListener(v -> {
            pendingKind = kind;
            picker.launch(kind == Lib.NOVEL
                    ? new String[]{"text/*", "application/octet-stream"}
                    : new String[]{"video/*"});
        });

        cfg.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        clr.setOnClickListener(v -> {
            if (data.isEmpty()) return;
            new AlertDialog.Builder(this)
                    .setTitle("清空当前列表？")
                    .setMessage("只清除本模块的记录，不会删除本机文件。")
                    .setPositiveButton("清空", (d, w) -> {
                        Lib.clear(this, kind);
                        reload();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        lv.setOnItemClickListener((p, v, pos, id) -> openItem(data.get(pos)));

        lv.setOnItemLongClickListener((p, v, pos, id) -> {
            final Lib.Item it = data.get(pos);
            new AlertDialog.Builder(this)
                    .setTitle("移出列表？")
                    .setMessage(it.name)
                    .setPositiveButton("移出", (d, w) -> {
                        Lib.remove(this, kind, it.uri);
                        reload();
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        });

        reload();
    }

    private void openItem(Lib.Item it) {
        Intent i = new Intent(this, kind == Lib.NOVEL ? ReaderActivity.class : VideoActivity.class);
        i.setData(Uri.parse(it.uri));
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        i.putExtra("name", it.name);
        startActivity(i);
    }

    private void reload() {
        data.clear();
        data.addAll(Lib.list(this, kind));
        adapter.clear();
        for (Lib.Item it : data) adapter.add(it.name);
        adapter.notifyDataSetChanged();
        boolean none = data.isEmpty();
        empty.setVisibility(none ? View.VISIBLE : View.GONE);
        lv.setVisibility(none ? View.GONE : View.VISIBLE);
    }

    /** 从 SAF URI 取显示名，取不到就退回路径末段。 */
    private String displayName(Uri u) {
        Cursor c = null;
        try {
            c = getContentResolver().query(u, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (c != null && c.moveToFirst()) {
                String n = c.getString(0);
                if (n != null && !n.isEmpty()) return n;
            }
        } catch (Throwable ignored) {
        } finally {
            if (c != null) c.close();
        }
        String last = u.getLastPathSegment();
        return last == null ? u.toString() : last;
    }
}