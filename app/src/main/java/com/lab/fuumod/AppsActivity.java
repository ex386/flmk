package com.lab.fuumod;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayout;

/** 密码门之后的容器：小说 / 视频 两个入口。 */
public class AppsActivity extends AppCompatActivity {

    static final int REQ_NOVEL = 101;
    static final int REQ_VIDEO = 102;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        TabLayout tabs = new TabLayout(this);
        tabs.addTab(tabs.newTab().setText("小说"));
        tabs.addTab(tabs.newTab().setText("视频"));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab t) {
                open(t.getPosition() == 0 ? REQ_NOVEL : REQ_VIDEO);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab t) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab t) {
                open(t.getPosition() == 0 ? REQ_NOVEL : REQ_VIDEO);
            }
        });

        root.addView(tabs);
        setContentView(root);
        open(REQ_NOVEL);
    }

    void open(int req) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(req == REQ_NOVEL ? "text/*" : "video/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(i, req);
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null) return;
        Uri u = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Throwable ignored) {
        }
        Intent next = new Intent(this, req == REQ_NOVEL ? ReaderActivity.class : VideoActivity.class);
        next.setData(u);
        startActivity(next);
    }
}