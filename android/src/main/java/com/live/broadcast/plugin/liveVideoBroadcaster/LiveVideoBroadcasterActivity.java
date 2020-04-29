package com.live.broadcast.plugin.liveVideoBroadcaster;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.renderscript.Sampler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.getcapacitor.JSObject;
import com.live.broadcast.plugin.LiveBroadcastPlugin;
import com.live.broadcast.plugin.MainActivity;
import com.live.broadcast.plugin.livebroadcastplugin.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import io.antmedia.android.broadcaster.ILiveVideoBroadcaster;
import io.antmedia.android.broadcaster.LiveVideoBroadcaster;
import io.antmedia.android.broadcaster.utils.Resolution;

import static com.live.broadcast.plugin.MainActivity.RTMP_BASE_URL;
import static com.live.broadcast.plugin.MainActivity.SERVER_IP;
import static com.live.broadcast.plugin.MainActivity.SERVER_PORT;

public class LiveVideoBroadcasterActivity extends AppCompatActivity {


    private static final String TAG = LiveVideoBroadcasterActivity.class.getSimpleName();
    private ViewGroup mRootView;
    boolean mIsRecording = false;
    private TextView mStreamNameEditText;
    private TextView mStreamLiveEditID;
    private Timer mTimer;
    private Timer mTimerWatcher;
    private long mElapsedTime;
    private TextView mWatcherStatus;
    public TimerHandler mTimerHandler;
    private ImageButton mSettingsButton;
    private CameraResolutionsFragment mCameraResolutionsDialog;
    private Intent mLiveVideoBroadcasterServiceIntent;
    private TextView mStreamLiveStatus;

    private GLSurfaceView mGLView;
    private ILiveVideoBroadcaster mLiveVideoBroadcaster;
    private ImageButton mBroadcastControlButton;

    private int peakWatcher = 0;

  public static final String REQUEST_METHOD = "GET";
  public static final int READ_TIMEOUT = 15000;
  public static final int CONNECTION_TIMEOUT = 15000;
  private String mStreamId ;

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LiveVideoBroadcaster.LocalBinder binder = (LiveVideoBroadcaster.LocalBinder) service;
            if (mLiveVideoBroadcaster == null) {
                mLiveVideoBroadcaster = binder.getService();
                mLiveVideoBroadcaster.init(LiveVideoBroadcasterActivity.this, mGLView);
                mLiveVideoBroadcaster.setAdaptiveStreaming(true);
            }
            mLiveVideoBroadcaster.openCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mLiveVideoBroadcaster = null;
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getIntent().getExtras();
        // Hide title
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        mStreamId = bundle.getString("streamName");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //binding on resume not to having leaked service connection
        mLiveVideoBroadcasterServiceIntent = new Intent(this, LiveVideoBroadcaster.class);
        //this makes service do its job until done
        startService(mLiveVideoBroadcasterServiceIntent);

        setContentView(R.layout.activity_live_video_broadcaster);

        mTimerHandler = new TimerHandler();

        mStreamNameEditText = (TextView) findViewById(R.id.stream_name_edit_text);
        mStreamLiveEditID = findViewById(R.id.stream_name_edit_text_id);
        mStreamNameEditText.setEnabled(false);
        mStreamNameEditText.setText(bundle.getString("streamName"));
        mStreamLiveEditID.setText(getString(R.string.your_id ) + " ");
        mRootView = (ViewGroup)findViewById(R.id.root_layout);
        mSettingsButton = (ImageButton)findViewById(R.id.settings_button);
        mStreamLiveStatus = (TextView) findViewById(R.id.stream_live_status);
        mWatcherStatus = (TextView) findViewById(R.id.stream_watcher);
        mBroadcastControlButton = (ImageButton) findViewById(R.id.toggle_broadcasting);

        // Configure the GLSurfaceView.  This will start the Renderer thread, with an
        // appropriate EGL activity.
        mGLView = (GLSurfaceView) findViewById(R.id.cameraPreview_surfaceView);
        if (mGLView != null) {
            mGLView.setEGLContextClientVersion(2);     // select GLES 2.0
        }
    }

    public void changeCamera(View v) {
        if (mLiveVideoBroadcaster != null) {
            mLiveVideoBroadcaster.changeCamera();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //this lets activity bind
        bindService(mLiveVideoBroadcasterServiceIntent, mConnection, 0);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case LiveVideoBroadcaster.PERMISSIONS_REQUEST: {
                if (mLiveVideoBroadcaster.isPermissionGranted()) {
                    mLiveVideoBroadcaster.openCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
                }
                else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.CAMERA) ||
                            ActivityCompat.shouldShowRequestPermissionRationale(this,
                                    Manifest.permission.RECORD_AUDIO) ) {
                        mLiveVideoBroadcaster.requestPermission();
                    }
                    else {
                        new AlertDialog.Builder(LiveVideoBroadcasterActivity.this)
                                .setTitle(R.string.permission)
                                .setMessage(getString(R.string.app_doesnot_work_without_permissions))
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {

                                        try {
                                            //Open the specific App Info page:
                                            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                            intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                                            startActivity(intent);

                                        } catch ( ActivityNotFoundException e ) {
                                            //e.printStackTrace();

                                            //Open the generic Apps page:
                                            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                                            startActivity(intent);

                                        }
                                    }
                                })
                                .show();
                    }
                }
                return;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");

        //hide dialog if visible not to create leaked window exception
        if (mCameraResolutionsDialog != null && mCameraResolutionsDialog.isVisible()) {
            mCameraResolutionsDialog.dismiss();
        }
        if(mLiveVideoBroadcaster != null) mLiveVideoBroadcaster.pause();
    }


    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mConnection);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE || newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            mLiveVideoBroadcaster.setDisplayOrientation();
        }

    }

    public void showSetResolutionDialog(View v) {

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment fragmentDialog = getSupportFragmentManager().findFragmentByTag("dialog");
        if (fragmentDialog != null) {

            ft.remove(fragmentDialog);
        }

        ArrayList<Resolution> sizeList = mLiveVideoBroadcaster.getPreviewSizeList();


        if (sizeList != null && sizeList.size() > 0) {
            mCameraResolutionsDialog = new CameraResolutionsFragment();
            mCameraResolutionsDialog.setCameraResolutions(sizeList, mLiveVideoBroadcaster.getPreviewSize());
            mCameraResolutionsDialog.show(ft, "resolutiton_dialog");
        }
        else {
            Snackbar.make(mRootView, "No resolution available",Snackbar.LENGTH_LONG).show();
        }

    }

    public void toggleBroadcasting(View v) {
        if (!mIsRecording)
        {
            if (mLiveVideoBroadcaster != null) {
                if (!mLiveVideoBroadcaster.isConnected()) {
                    final String streamName = mStreamNameEditText.getText().toString();
                    new AsyncTask<String, String, Boolean>() {
                        ContentLoadingProgressBar
                                progressBar;
                        @Override
                        protected void onPreExecute() {
                            progressBar = new ContentLoadingProgressBar(LiveVideoBroadcasterActivity.this);
                            progressBar.show();
                        }

                        @Override
                        protected Boolean doInBackground(String... url) {
                            return mLiveVideoBroadcaster.startBroadcasting(url[0]);

                        }

                        @Override
                        protected void onPostExecute(Boolean result) {
                            progressBar.hide();
                            mIsRecording = result;
                            if (result) {
                                mStreamLiveStatus.setVisibility(View.VISIBLE);
                                mWatcherStatus.setVisibility(View.VISIBLE);

//                                mBroadcastControlButton.setText(R.string.stop_broadcasting);
                                mBroadcastControlButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_stop_button));
                                mSettingsButton.setVisibility(View.GONE);
                                startTimer();//start the recording duration
                                startTimerWatcher(streamName);
                            }
                            else {
                                Snackbar.make(mRootView, R.string.stream_not_started, Snackbar.LENGTH_LONG).show();

                                triggerStopRecording();
                            }
                        }
                    }.execute(RTMP_BASE_URL + streamName);
                }
                else {
                    Snackbar.make(mRootView, R.string.streaming_not_finished, Snackbar.LENGTH_LONG).show();
                }
            }
            else {
                Snackbar.make(mRootView, R.string.oopps_shouldnt_happen, Snackbar.LENGTH_LONG).show();
            }
        }
        else
        {
            triggerStopRecording();
        }

    }

    public void getStatistic(String id) {
        String url = "http://" + SERVER_IP + ":" + SERVER_PORT + "/LiveApp/rest/v2/broadcasts/"+id+"/broadcast-statistics";
        new AsyncTask<String, String, String>(){
          String result;
          String inputLine;
          @Override
          protected String doInBackground(String... urls) {
            try {
              URL myUrl = new URL(urls[0]);
              HttpURLConnection connection =(HttpURLConnection)
              myUrl.openConnection();
              connection.setRequestMethod(REQUEST_METHOD);
              connection.setReadTimeout(READ_TIMEOUT);
              connection.setConnectTimeout(CONNECTION_TIMEOUT);
              connection.connect();

              //Create a new InputStreamReader
              InputStreamReader streamReader = new
                InputStreamReader(connection.getInputStream());
              //Create a new buffered reader and String Builder
              BufferedReader reader = new BufferedReader(streamReader);
              StringBuilder stringBuilder = new StringBuilder();
              //Check if the line we are reading is not null
              while((inputLine = reader.readLine()) != null){
                stringBuilder.append(inputLine);
              }
              //Close our InputStream and Buffered reader
              reader.close();
              streamReader.close();
              //Set our result equal to our stringBuilder
              result = stringBuilder.toString();

            } catch(IOException e){
              e.printStackTrace();
              result = null;
            }
            return  result;
          }

          @Override
          protected void onPostExecute(String result) {
            try {
              JSONObject obj = new JSONObject(result);
              mWatcherStatus.setText(" " + obj.getString("totalHLSWatchersCount"));
              if(Integer.parseInt(obj.getString("totalHLSWatchersCount")) > peakWatcher){
                peakWatcher = Integer.parseInt(obj.getString("totalHLSWatchersCount"));
              }
            } catch (JSONException e) {
              e.printStackTrace();
            }


            // Call activity method with results);
          }
        }.execute(url);

  }


    public void triggerStopRecording() {
        if (mIsRecording) {
//            mBroadcastControlButton.setText(R.string.start_broadcasting);
            mBroadcastControlButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_record_button));

            mStreamLiveStatus.setVisibility(View.GONE);
            mWatcherStatus.setVisibility(View.GONE);
            mStreamLiveStatus.setText(R.string.live_indicator);
            mSettingsButton.setVisibility(View.VISIBLE);
            stopTimer();
            mLiveVideoBroadcaster.stopBroadcasting();
            JSObject response = new JSObject();
          try {
            response.putSafe("peakWatcher",peakWatcher);
            response.putSafe("streamId",mStreamId);
            LiveBroadcastPlugin.showPopup(this,new AlertDialog.Builder(this).create(),response);
          } catch (JSONException e) {
            e.printStackTrace();
          }
//            finish();
        }

        mIsRecording = false;
    }

    //This method starts a mTimer and updates the textview to show elapsed time for recording
    public void startTimer() {

        if(mTimer == null) {
            mTimer = new Timer();
        }

        mElapsedTime = 0;
        mTimer.scheduleAtFixedRate(new TimerTask() {

            public void run() {
                mElapsedTime += 1; //increase every sec
                mTimerHandler.obtainMessage(TimerHandler.INCREASE_TIMER).sendToTarget();
                if (mLiveVideoBroadcaster == null || !mLiveVideoBroadcaster.isConnected()) {
                    mTimerHandler.obtainMessage(TimerHandler.CONNECTION_LOST).sendToTarget();
                }
            }
        }, 0, 1000);
    }
  public void startTimerWatcher(final String name) {

    if(mTimerWatcher == null) {
      mTimerWatcher = new Timer();
    }

    mTimerWatcher.scheduleAtFixedRate(new TimerTask() {

      public void run() {
        getStatistic(name);
      }
    }, 0, 5000);
  }


    public void stopTimer()
    {
        if (mTimer != null) {
            this.mTimer.cancel();
        }
        if (mTimerWatcher != null) {
          this.mTimerWatcher.cancel();
        }
        this.mTimer = null;
        this.mElapsedTime = 0;
        this.mTimerWatcher = null;
    }

    public void setResolution(Resolution size) {
        mLiveVideoBroadcaster.setResolution(size);
    }

    private class TimerHandler extends Handler {
        static final int CONNECTION_LOST = 2;
        static final int INCREASE_TIMER = 1;

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case INCREASE_TIMER:
                    mStreamLiveStatus.setText(getString(R.string.live_indicator) + "   •   " + getDurationString((int) mElapsedTime));
                    break;
                case CONNECTION_LOST:
                    triggerStopRecording();
                    new AlertDialog.Builder(LiveVideoBroadcasterActivity.this)
                            .setMessage(R.string.broadcast_connection_lost)
                            .setPositiveButton(android.R.string.yes, null)
                            .show();

                    break;
            }
        }
    }


    public static String getDurationString(int seconds) {

        if(seconds < 0 || seconds > 2000000)//there is an codec problem and duration is not set correctly,so display meaningfull string
            seconds = 0;
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;

        if(hours == 0)
            return twoDigitString(minutes) + " : " + twoDigitString(seconds);
        else
            return twoDigitString(hours) + " : " + twoDigitString(minutes) + " : " + twoDigitString(seconds);
    }

    public static String twoDigitString(int number) {

        if (number == 0) {
            return "00";
        }

        if (number / 10 == 0) {
            return "0" + number;
        }

        return String.valueOf(number);
    }
}
