package com.lab.fuumod;

import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/** 极简 TXT 阅读器：UTF-8 优先，失败退回 GBK。 */
public class ReaderActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        final TextView tv = new TextView(this);
        tv.setTextSize(17);
        tv.setLineSpacing(6f, 1f);
        tv.setPadding(32, 32, 32, 96);
        tv.setText("读取中…");

        ScrollView sv = new ScrollView(this);
        sv.addView(tv);
        setContentView(sv);

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
            runOnUiThread(() -> tv.setText(out));
        }).start();
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