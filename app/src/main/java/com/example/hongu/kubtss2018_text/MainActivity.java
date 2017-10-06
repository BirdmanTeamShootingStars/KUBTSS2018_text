package com.example.hongu.kubtss2018_text;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.net.InetAddress;

//import android.support.v7.app.AppCompatActivity;


public class MainActivity extends Activity {

    private final int REQUEST_PERMISSION = 1000;
    private int p = 0;

    private static final String TAG = MainActivity.class.getSimpleName();
    private ReceivedDataAdapter mReceivedDataAdapter;
    private SensorAdapter mSensorAdapter;
    private TextSensorViewThread mTextSensorViewThread;//テキスト形式のUI用スレッド
    private Sound sound;
    private double atmLapse, atmStandard;

    private String url;
    private CloudLoggerService mCloudLoggerService = null;
    private CloudLoggerAdapter mCloudLoggerAdapter;
    private InetAddress inetAddress;
    private CloudLoggerSendThread mCloudLoggerSendThread;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//スリープ抑制
        setContentView(R.layout.activity_main);//テキスト形式のUI
        mReceivedDataAdapter = new ReceivedDataAdapter(getBaseContext());
        SensorManager mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Configuration config = getResources().getConfiguration();
        mSensorAdapter = new SensorAdapter(mSensorManager, mLocationManager, mReceivedDataAdapter, config);
        mTextSensorViewThread = new TextSensorViewThread(mSensorAdapter, mReceivedDataAdapter);

        // Android 6, API 23以上でパーミッシンの確認
        if(Build.VERSION.SDK_INT >= 23){
            checkPermission();
        }
        else{
            mTextSensorViewThread.start();
            p = 1;
        }



        if (Build.VERSION.SDK_INT >= 19) {
            Log.i(TAG, "getExternalFilesDirを呼び出します");
            File[] extDirs = getExternalFilesDirs(Environment.DIRECTORY_DOWNLOADS);
            File extSdDir = extDirs[extDirs.length - 1];
            Logger.setExternalDir(extSdDir);
            Log.i(TAG, "getExternalFilesDirが返すパス: " + extSdDir.getAbsolutePath());
        }else{
            Log.e(TAG, "This SDK version is under 18.");
            finish();
        }

        mReceivedDataAdapter = new ReceivedDataAdapter(getBaseContext());



      //  sound = new Sound(getApplicationContext(), R.drawable.warn05);

        Switch connectSwitch = (Switch) findViewById(R.id.reConnectSwitch);
        connectSwitch.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mReceivedDataAdapter.setReconnection(isChecked);
            }
        });

        if (mCloudLoggerService == null) {
            url = "http://quatronic.php.xdomain.jp/birdman/writer.php";
            mCloudLoggerService = new CloudLoggerService(url);
        }
        mCloudLoggerAdapter = new CloudLoggerAdapter(mSensorAdapter,mReceivedDataAdapter,mCloudLoggerService);
        mCloudLoggerSendThread = new CloudLoggerSendThread(mCloudLoggerService);
        mCloudLoggerSendThread.start();
    }

    // 位置情報許可の確認
    public void checkPermission() {
        // 既に許可している
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED){

            if (p ==0) {
                mTextSensorViewThread.start();
            }
        }
        // 拒否していた場合
        else{
            requestLocationPermission();
        }
    }
    // 許可を求める
    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION);

        } else {
            Toast toast = Toast.makeText(this, "許可されないとアプリが実行できません", Toast.LENGTH_SHORT);
            toast.show();

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, REQUEST_PERMISSION);

        }
    }

    // 結果の受け取り
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION) {
            // 使用が許可された
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (p ==0) {
                    mTextSensorViewThread.start();
                }
                return;

            } else {
                // それでも拒否された時の対応
                Toast toast = Toast.makeText(this, "これ以上なにもできません", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }

    // Intent でLocation
//    private void locationActivity() {
//        Intent intent = new Intent(getApplication(), Text.class);
//        startActivity(intent);
//    }


    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        String lapseStr = pref.getString(SettingPrefActivity.PREF_KEY_LAPSE, "0.12");
        String standardStr = pref.getString(SettingPrefActivity.PREF_KEY_STANDARD, "1013.25");
        atmLapse = Double.parseDouble(lapseStr);
        atmStandard = Double.parseDouble(standardStr);

        mTextSensorViewThread.setPressureParam(atmStandard, atmLapse);
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTextSensorViewThread.stopRunning();
        mCloudLoggerSendThread.stopRunning();

        sound.release();
        mReceivedDataAdapter.stop();
        mSensorAdapter.stopSensor();
        mCloudLoggerService.close();
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
            Intent intent = new Intent(MainActivity.this, SettingPrefActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class TextSensorViewThread extends Thread {
        SensorAdapter mSensorAdapter;
        ReceivedDataAdapter mReceivedDataAdapter;
        Handler handler = new Handler();

        private TextView txtYaw, txtPitch, txtRoll, txtLati, txtLong, txtCnt, txtStraight, txtIntegral;
        private TextView txtStatus, txtSelector;
        private TextView txtTime, txtRud, txtEle, txtTrim, txtAirspeed, txtCadence, txtUltsonic, txtAtmpress, txtAltitude;
        private TextView txtCadencevolt, txtUltsonicvolt, txtServovolt;
        private boolean running = true;

        private double atmStandard, atmLapse;

        public TextSensorViewThread(SensorAdapter mSensorAdapter, ReceivedDataAdapter mReceivedDataAdapter){
            this.mSensorAdapter = mSensorAdapter;
            this.mReceivedDataAdapter = mReceivedDataAdapter;
            txtYaw   = (TextView) findViewById(R.id.textViewYaw);
            txtPitch = (TextView) findViewById(R.id.textViewPitch);
            txtRoll  = (TextView) findViewById(R.id.textViewRoll);
            txtLati  = (TextView) findViewById(R.id.textViewLati);
            txtLong  = (TextView) findViewById(R.id.textViewLong);
            txtCnt   = (TextView) findViewById(R.id.textViewCnt);
            txtStraight = (TextView) findViewById(R.id.textViewStraight);
            txtIntegral = (TextView) findViewById(R.id.textViewIntegral);

            txtStatus   = (TextView) findViewById(R.id.textViewStatus);
            txtSelector = (TextView) findViewById(R.id.textViewSelector);
            txtTime     = (TextView) findViewById(R.id.textViewTime);
            txtRud      = (TextView) findViewById(R.id.textViewRud);
            txtEle      = (TextView) findViewById(R.id.textViewEle);
            txtTrim     = (TextView) findViewById(R.id.textViewTrim);
            txtAirspeed = (TextView) findViewById(R.id.textViewAirspeed);
            txtCadence  = (TextView) findViewById(R.id.textViewCadence);
            txtUltsonic = (TextView) findViewById(R.id.textViewUltsonic);
            txtAtmpress = (TextView) findViewById(R.id.textViewAtmpress);
            txtAltitude = (TextView) findViewById(R.id.textViewAltitude);
            txtCadencevolt = (TextView) findViewById(R.id.textViewCadencevolt);
            txtUltsonicvolt = (TextView) findViewById(R.id.textViewUltsonicvolt);
            txtServovolt = (TextView) findViewById(R.id.textViewServovolt);
        }
        public void start(){
            new Thread(this).start();
        }

        public void stopRunning() {
            running = false;
        }

        public void setPressureParam(double atmStandard, double atmLapse){
            this.atmStandard = atmStandard;
            this.atmLapse    = atmLapse;
        }

        @Override
        public void run(){
            while(running){
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        txtYaw.setText(String.valueOf(mSensorAdapter.getYaw()));
                        txtPitch.setText(String.valueOf(mSensorAdapter.getPitch()));
                        txtRoll.setText(String.valueOf(mSensorAdapter.getRoll()));
                        txtLati.setText(String.valueOf(mSensorAdapter.getLatitude()));
                        txtLong.setText(String.valueOf(mSensorAdapter.getLongitude()));
                        txtCnt.setText(String.valueOf(mSensorAdapter.getGpsCnt()));
                        txtStraight.setText(String.format("%.0f", mSensorAdapter.getStraightDistance()));
                        txtIntegral.setText(String.format("%.0f", mSensorAdapter.getIntegralDistance()));

                        txtTime.setText(String.valueOf(mReceivedDataAdapter.getTime()));
                        txtEle.setText(String.format("%.2f", mReceivedDataAdapter.getElevator()));
                        txtRud.setText(String.format("%.2f", mReceivedDataAdapter.getRudder()));
                        txtTrim.setText(String.valueOf(mReceivedDataAdapter.getTrim()));
                        txtAirspeed.setTextSize(60.0f);
                        txtAirspeed.setText(String.format("%.2f", mReceivedDataAdapter.getAirspeed())+"m/s");
                        txtCadence.setTextSize(60.0f);
                        txtCadence.setText(String.format("%.2f", mReceivedDataAdapter.getCadence())+"RPM");
                        txtUltsonic.setText(String.format("%.2f", mReceivedDataAdapter.getUltsonic()));
                        txtAtmpress.setText(String.format("%.2f", mReceivedDataAdapter.getAtmpress()));
                        double altitude = -(mReceivedDataAdapter.getAtmpress() - atmStandard) / atmLapse;
                        txtAltitude.setText(String.format("%.2f", altitude));
                        switch(mReceivedDataAdapter.getState()){
                            case BluetoothChatService.STATE_CONNECTED:
                                txtStatus.setText("Connected");
                                break;
                            case BluetoothChatService.STATE_CONNECTING:
                                txtStatus.setText("Connecting...");
                                break;
                            case BluetoothChatService.STATE_LISTEN:
                                txtStatus.setText("Listen");
                                break;
                            case BluetoothChatService.STATE_NONE:
                                txtStatus.setText("None");
                                break;
                        }
                        txtSelector.setText(String.valueOf(mReceivedDataAdapter.getSelector()));
                        txtCadencevolt.setText(String.valueOf(mReceivedDataAdapter.getCadencevolt()));
                        txtUltsonicvolt.setText(String.valueOf(mReceivedDataAdapter.getUltsonicvolt()));
                        txtServovolt.setText(String.valueOf(mReceivedDataAdapter.getServovolt()));


                        //sound.set(mSensorAdapter.getRoll(), 40, 60);
                    }
                });
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Log.e(TAG, "ReConnectThread exception");
                }
            }
        }

    }

    private class CloudLoggerSendThread extends Thread {
        CloudLoggerService mCloudLoggerService;
        Handler handler = new Handler();
        private boolean running = true;
        public CloudLoggerSendThread(CloudLoggerService mCloudLoggerService){
            this.mCloudLoggerService = mCloudLoggerService;
        }
        public void start(){
            new Thread(this).start();
        }
        public void stopRunning() {
            running = false;
        }
        @Override
        public void run(){
            while(running){
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        //mCloudLoggerService.send();
                    }
                });
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.e(TAG, "CloudLoggerSendThread exception");
                }
            }
        }
    }
}
