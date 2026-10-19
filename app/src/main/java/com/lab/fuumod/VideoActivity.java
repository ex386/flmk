package com.lab.fuumod;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

/** Media3 / ExoPlayer 播放器；按 URI 记住上次播放位置，播放期间屏幕常亮。 */
public class VideoActivity extends AppCompatActivity {

    private ExoPlayer player;
    private String key = "";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        String name = getIntent().getStringExtra("name");
        if (name != null && !name.isEmpty()) setTitle(name);

        PlayerView pv = new PlayerView(this);
        setContentView(pv);
        player = new ExoPlayer.Builder(this).build();
        pv.setPlayer(player);
        if (getIntent().getData() != null) {
            key = getIntent().getData().toString();
            long start = key.isEmpty() ? 0 : pos().getLong(key, 0);
            try {
                player.setMediaItem(MediaItem.fromUri(getIntent().getData()), start);
            } catch (Throwable t) {
                player.setMediaItem(MediaItem.fromUri(getIntent().getData()));
            }
            player.setPlayWhenReady(true);
            player.prepare();
        }
    }

    private SharedPreferences pos() {
        return getSharedPreferences("videopos", MODE_PRIVATE);
    }

    private void savePos() {
        if (player == null || key.isEmpty()) return;
        try {
            long p = Math.max(0, player.getCurrentPosition());
            long d = player.getDuration();
            // 快播完了就从头开始，免得下次进来停在片尾
            if (d > 0 && d - p < 5000) p = 0;
            pos().edit().putLong(key, p).apply();
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) player.pause();
        savePos();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}