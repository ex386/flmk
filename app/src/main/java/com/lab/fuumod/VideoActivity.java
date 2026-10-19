package com.lab.fuumod;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

/** Media3 / ExoPlayer 播放器。 */
public class VideoActivity extends AppCompatActivity {

    private ExoPlayer player;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        PlayerView pv = new PlayerView(this);
        setContentView(pv);
        player = new ExoPlayer.Builder(this).build();
        pv.setPlayer(player);
        if (getIntent().getData() != null) {
            player.setMediaItem(MediaItem.fromUri(getIntent().getData()));
            player.setPlayWhenReady(true);
            player.prepare();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) player.pause();
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