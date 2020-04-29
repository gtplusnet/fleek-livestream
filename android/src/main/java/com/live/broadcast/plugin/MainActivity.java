package com.live.broadcast.plugin;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.live.broadcast.plugin.liveVideoBroadcaster.*;
import com.live.broadcast.plugin.liveVideoPlayer.LiveVideoPlayerActivity;
import com.live.broadcast.plugin.livebroadcastplugin.R;

public class MainActivity extends AppCompatActivity {

    /**
     * PLEASE WRITE RTMP BASE URL of the your RTMP SERVER.
     */
    public static final String SERVER_IP = "192.168.1.101";
    public static final String SERVER_PORT = "5080";
    public static final String RTMP_BASE_URL = "rtmp://"+ SERVER_IP +"/LiveApp/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      Intent i = new Intent(this, LiveVideoBroadcasterActivity.class);
      Bundle bundle = getIntent().getExtras();
      i.putExtra("streamName",bundle.getString("streamName"));
      startActivity(i);
      finish();
    }

    public void openVideoBroadcaster(View view) {
        Intent i = new Intent(this, LiveVideoBroadcasterActivity.class);
        startActivity(i);
    }

    public void openVideoPlayer(View view) {
        Intent i = new Intent(this, LiveVideoPlayerActivity.class);
        startActivity(i);
    }
}
