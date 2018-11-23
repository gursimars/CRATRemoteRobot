package net.ossrs.gursimar.crat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.github.faucamp.simplertmp.RtmpHandler;
import com.seu.magicfilter.utils.MagicFilterType;

import net.ossrs.gursimar.SrsCameraView;
import net.ossrs.gursimar.SrsEncodeHandler;
import net.ossrs.gursimar.SrsPublisher;
import net.ossrs.gursimar.SrsRecordHandler;

import java.io.IOException;
import java.net.SocketException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements RtmpHandler.RtmpListener,
                        SrsRecordHandler.SrsRecordListener, SrsEncodeHandler.SrsEncodeListener {

    private static final String TAG = "Yasea";

    private Button btnPublish;
    private Button btnSwitchCamera;
    private Button btnRecord;
    private Button btnSwitchEncoder;

    private SharedPreferences sp;
    private String rtmpUrl = "rtmp://xmeye03.australiaeast.cloudapp.azure.com:1935/live/code";
    private String recPath = Environment.getExternalStorageDirectory().getPath() + "/test.mp4";

    private SrsPublisher mPublisher;

    private Button button;
    private Button record;

    private LinearLayout One;
    private LinearLayout Two;
    private LinearLayout Three;
    private LinearLayout Four;

    private Button Forward;
    private Button Reverse;
    private Button Right;
    private Button Left;

    private Button Next;
    private EditText commanderID;

    protected PowerManager.WakeLock mWakeLock;
    JavaScriptInterface JSInterface;
    private String currentDateAndTime = "";

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        commanderID = findViewById(R.id.commanderID);

        One = findViewById(R.id.OneLight);
        Two = findViewById(R.id.TwoLight);
        Three = findViewById(R.id.ThreeLight);
        Four = findViewById(R.id.FourLight);

        /*MyTouchListener touchListener = new MyTouchListener();
        Forward.setOnTouchListener(touchListener);
        Reverse.setOnTouchListener(touchListener);
        Right.setOnTouchListener(touchListener);
        Left.setOnTouchListener(touchListener);*/

        // response screen rotation event
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);

        // restore data.
        sp = getSharedPreferences("CRAT", MODE_PRIVATE);
        rtmpUrl = sp.getString("rtmpUrl", rtmpUrl);

        // initialize url.

        btnPublish = (Button) findViewById(R.id.publish);
        btnSwitchCamera = (Button) findViewById(R.id.swCam);
        btnRecord = (Button) findViewById(R.id.record);
        btnSwitchEncoder = (Button) findViewById(R.id.swEnc);

        mPublisher = new SrsPublisher((SrsCameraView) findViewById(R.id.glsurfaceview_camera));
        mPublisher.setEncodeHandler(new SrsEncodeHandler(this));
        mPublisher.setRtmpHandler(new RtmpHandler(this));
        mPublisher.setRecordHandler(new SrsRecordHandler(this));
        mPublisher.setPreviewResolution(640, 360);
        mPublisher.setOutputResolution(360, 640);
        mPublisher.setVideoHDMode();
        mPublisher.startCamera();

        btnPublish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnPublish.getText().toString().contentEquals("publish")) {
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("rtmpUrl", rtmpUrl);
                    editor.apply();

                    mPublisher.startPublish(rtmpUrl);
                    mPublisher.startCamera();

                    if (btnSwitchEncoder.getText().toString().contentEquals("soft encoder")) {
                        Toast.makeText(getApplicationContext(), "Use hard encoder", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Use soft encoder", Toast.LENGTH_SHORT).show();
                    }
                    btnPublish.setText("stop");
                    btnSwitchEncoder.setEnabled(false);
                } else if (btnPublish.getText().toString().contentEquals("stop")) {
                    mPublisher.stopPublish();
                    mPublisher.stopRecord();
                    btnPublish.setText("publish");
                    btnRecord.setText("record");
                    btnSwitchEncoder.setEnabled(true);
                }
            }
        });

        btnSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPublisher.switchCameraFace((mPublisher.getCamraId() + 1) % Camera.getNumberOfCameras());
            }
        });

        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnRecord.getText().toString().contentEquals("record")) {
                    if (mPublisher.startRecord(recPath)) {
                        btnRecord.setText("pause");
                    }
                } else if (btnRecord.getText().toString().contentEquals("pause")) {
                    mPublisher.pauseRecord();
                    btnRecord.setText("resume");
                } else if (btnRecord.getText().toString().contentEquals("resume")) {
                    mPublisher.resumeRecord();
                    btnRecord.setText("pause");
                }
            }
        });

        btnSwitchEncoder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnSwitchEncoder.getText().toString().contentEquals("soft encoder")) {
                    mPublisher.switchToSoftEncoder();
                    btnSwitchEncoder.setText("hard encoder");
                } else if (btnSwitchEncoder.getText().toString().contentEquals("hard encoder")) {
                    mPublisher.switchToHardEncoder();
                    btnSwitchEncoder.setText("soft encoder");
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else {
            switch (id) {
                case R.id.cool_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.COOL);
                    break;
                case R.id.beauty_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.BEAUTY);
                    break;
                case R.id.early_bird_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.EARLYBIRD);
                    break;
                case R.id.evergreen_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.EVERGREEN);
                    break;
                case R.id.n1977_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.N1977);
                    break;
                case R.id.nostalgia_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.NOSTALGIA);
                    break;
                case R.id.romance_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.ROMANCE);
                    break;
                case R.id.sunrise_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.SUNRISE);
                    break;
                case R.id.sunset_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.SUNSET);
                    break;
                case R.id.tender_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.TENDER);
                    break;
                case R.id.toast_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.TOASTER2);
                    break;
                case R.id.valencia_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.VALENCIA);
                    break;
                case R.id.walden_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.WALDEN);
                    break;
                case R.id.warm_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.WARM);
                    break;
                case R.id.original_filter:
                default:
                    mPublisher.switchCameraFilter(MagicFilterType.NONE);
                    break;
            }
        }
        setTitle(item.getTitle());

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final Button btn = (Button) findViewById(R.id.publish);
        btn.setEnabled(true);
        mPublisher.resumeRecord();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPublisher.pauseRecord();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPublisher.stopPublish();
        mPublisher.stopRecord();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mPublisher.stopEncode();
        mPublisher.stopRecord();
        btnRecord.setText("record");
        mPublisher.setScreenOrientation(newConfig.orientation);
        if (btnPublish.getText().toString().contentEquals("stop")) {
            mPublisher.startEncode();
        }
        mPublisher.startCamera();
    }

    private static String getRandomAlphaString(int length) {
        String base = "abcdefghijklmnopqrstuvwxyz";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }

    private static String getRandomAlphaDigitString(int length) {
        String base = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }

    private void handleException(Exception e) {
        try {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            mPublisher.stopPublish();
            mPublisher.stopRecord();
            btnPublish.setText("publish");
            btnRecord.setText("record");
            btnSwitchEncoder.setEnabled(true);
        } catch (Exception e1) {
            //
        }
    }

    // Implementation of SrsRtmpListener.

    @Override
    public void onRtmpConnecting(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRtmpConnected(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        // Stream Started
        webView.loadUrl("javascript:SetActionDetails('STREAM_STARTED')");
    }

    @Override
    public void onRtmpVideoStreaming() {
    }

    @Override
    public void onRtmpAudioStreaming() {
    }

    @Override
    public void onRtmpStopped() {
        Toast.makeText(getApplicationContext(), "Stopped", Toast.LENGTH_SHORT).show();
        // Stream Stop
        webView.loadUrl("javascript:SetActionDetails('STREAM_ENDED')");
    }

    @Override
    public void onRtmpDisconnected() {
        Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRtmpVideoFpsChanged(double fps) {
        Log.i(TAG, String.format("Output Fps: %f", fps));
    }

    @Override
    public void onRtmpVideoBitrateChanged(double bitrate) {
        int rate = (int) bitrate;
        if (rate / 1000 > 0) {
            Log.i(TAG, String.format("Video bitrate: %f kbps", bitrate / 1000));
        } else {
            Log.i(TAG, String.format("Video bitrate: %d bps", rate));
        }
    }

    @Override
    public void onRtmpAudioBitrateChanged(double bitrate) {
        int rate = (int) bitrate;
        if (rate / 1000 > 0) {
            Log.i(TAG, String.format("Audio bitrate: %f kbps", bitrate / 1000));
        } else {
            Log.i(TAG, String.format("Audio bitrate: %d bps", rate));
        }
    }

    @Override
    public void onRtmpSocketException(SocketException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIOException(IOException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIllegalStateException(IllegalStateException e) {
        handleException(e);
    }

    // Implementation of SrsRecordHandler.

    @Override
    public void onRecordPause() {
        Toast.makeText(getApplicationContext(), "Record paused", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordResume() {
        Toast.makeText(getApplicationContext(), "Record resumed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordStarted(String msg) {
        Toast.makeText(getApplicationContext(), "Recording file: " + msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordFinished(String msg) {
        Toast.makeText(getApplicationContext(), "MP4 file saved: " + msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordIOException(IOException e) {
        handleException(e);
    }

    @Override
    public void onRecordIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }

    // Implementation of SrsEncodeHandler.

    @Override
    public void onNetworkWeak() {
        Toast.makeText(getApplicationContext(), "Network weak", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNetworkResume() {
        Toast.makeText(getApplicationContext(), "Network resume", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEncodeIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }

    // CRAT

    public void setCommander(View view) {
        String commanderid = commanderID.getText().toString();

        webView = (WebView) findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);

        JSInterface = new JavaScriptInterface(this);
        webView.addJavascriptInterface(JSInterface, "JSInterface");

        webView.loadUrl("https://crat.azurewebsites.net/robots/Robot02.html?commander=" + commanderid);
        SendPhoneData();
    }

    public class MyTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            /*switch(v.getId()){
                case R.id.forward:
                    Forward();
                    break;
                case R.id.back:
                    Reverse();
                    break;
                case R.id.right:
                    Left();
                    break;
                case R.id.left:
                    Right();
                    break;
                default:
                    Reset();
            }*/
            return true;
        }

    }

    public void Forward() {
        Reset();
        Two.setBackgroundColor(Color.parseColor("#FFFFFF"));
        Three.setBackgroundColor(Color.parseColor("#FFFFFF"));
    }

    public void Reverse() {
        Reset();
        One.setBackgroundColor(Color.parseColor("#FFFFFF"));
        Four.setBackgroundColor(Color.parseColor("#FFFFFF"));
    }

    public void Right() {
        Reset();
        Three.setBackgroundColor(Color.parseColor("#FFFFFF"));
        Four.setBackgroundColor(Color.parseColor("#FFFFFF"));
    }

    public void Left() {
        Reset();
        One.setBackgroundColor(Color.parseColor("#FFFFFF"));
        Two.setBackgroundColor(Color.parseColor("#FFFFFF"));
    }

    void Reset()
    {
        One.setBackgroundColor(Color.parseColor("#000000"));
        Two.setBackgroundColor(Color.parseColor("#000000"));
        Three.setBackgroundColor(Color.parseColor("#000000"));
        Four.setBackgroundColor(Color.parseColor("#000000"));
    }

    public class JavaScriptInterface {
        Context mContext;

        /** Instantiate the interface and set the context */
        JavaScriptInterface(Context c) {
            mContext = c;
        }

        @android.webkit.JavascriptInterface
        public void sendData(int vx1x1, int vx1x2, int vx2x1, int vx2x2)
        {
            if (vx1x1 != 0 && vx1x2 != 0 && vx2x1 == 0 && vx2x2 == 0)
            {
                //Log.e("Where", "Forward");
                Two.setBackgroundColor(Color.parseColor("#FFFFFF"));
                Three.setBackgroundColor(Color.parseColor("#FFFFFF"));
            }
            else {
                Reset();
            }
        }

        @android.webkit.JavascriptInterface
        public void startTheStream()
        {
            //Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();

            SharedPreferences.Editor editor = sp.edit();
            editor.putString("rtmpUrl", rtmpUrl);
            editor.apply();

            mPublisher.startPublish(rtmpUrl);
            mPublisher.startCamera();

            if (btnSwitchEncoder.getText().toString().contentEquals("soft encoder")) {
                Toast.makeText(getApplicationContext(), "Use hard encoder", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Use soft encoder", Toast.LENGTH_SHORT).show();
            }
            btnPublish.setText("stop");
            btnSwitchEncoder.setEnabled(false);
        }

        @android.webkit.JavascriptInterface
        public void stopTheStream()
        {
            mPublisher.stopPublish();
            mPublisher.stopRecord();
            btnPublish.setText("publish");
            btnRecord.setText("record");
            btnSwitchEncoder.setEnabled(true);
        }

        @android.webkit.JavascriptInterface
        public void switchCamera()
        {
            mPublisher.switchCameraFace((mPublisher.getCamraId() + 1) % Camera.getNumberOfCameras());
        }

        @android.webkit.JavascriptInterface
        public void connected(String data)
        {
            Log.e("COMMANDER CONNECTED?", data);
        }
    }

    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {

            int battery = getBatteryPercentage(getApplicationContext());
            String device = getDeviceName();
            String model = android.os.Build.MODEL;
            String product = android.os.Build.PRODUCT;

            Log.d("BATTERY LEVEL ---- ", battery + "%");
            Log.d("DEVICE ---- ", device);
            Log.d("MODEL ---- ", model);
            Log.d("PRODUCT ---- ", product);

            webView.loadUrl("javascript:TestPhoneData('" + battery + "','" + device + "','" + "" + "', '" + "" + "')");
            handler.postDelayed(this, 300000);
        }
    };

    public String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }


    private String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    private void SendPhoneData()
    {
        //Start
        handler.postDelayed(runnable, 1000);
    }

    public static int getBatteryPercentage(Context context) {

        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, iFilter);

        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;

        float batteryPct = level / (float) scale;

        return (int) (batteryPct * 100);
    }
}
