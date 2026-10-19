package com.lab.fuumod;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/** 极简 TXT 阅读器：UTF-8 优先，失败退回 GBK；按 URI 记住上次阅读位置。 */
public class ReaderActivity extends AppCompatActivity {

    private ScrollView sv;
    private String key = "";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        String name = getIntent().getStringExtra("name");
        if (name != null && !name.isEmpty()) setTitle(name);

        final TextView tv = new TextView(this);
        tv.setTextSize(17);
        tv.setLineSpacing(6f, 1f);
        tv.setPadding(32, 32, 32, 96);
        tv.setText("读取中…");

        sv = new ScrollView(this);
        sv.addView(tv);
        setContentView(sv);

        if (getIntent().getData() != null) key = getIntent().getData().toString();

        new Thread(() -> {
            String text;
            try (InputStream in = getContentResolver().openInputStream(getIntent().getData())) {
                byte[] all = readAll(in);
                Charset cs = isUtf8(all) ? Charset.forName("UTF-8") : Charset.forName("GBK");
                text = new String(all, cs);
            } catch (Throwable e) {
                text = "读取失败: " + e;
            }
            final String out = text;
            runOnUiThread(() -> {
                tv.setText(out);
                if (!key.isEmpty()) {
                    final int y = pos().getInt(key, 0);
                    if (y > 0) sv.post(() -> sv.scrollTo(0, y));
                }
            });
        }).start();
    }

    private SharedPreferences pos() {
        return getSharedPreferences("readpos", MODE_PRIVATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sv != null && !key.isEmpty()) pos().edit().putInt(key, sv.getScrollY()).apply();
    }

    static byte[] readAll(InputStream in) throws Exception {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        return bos.toByteArray();
    }

    static boolean isUtf8(byte[] d) {
        try {
            Charset.forName("UTF-8").newDecoder().decode(ByteBuffer.wrap(d));
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}