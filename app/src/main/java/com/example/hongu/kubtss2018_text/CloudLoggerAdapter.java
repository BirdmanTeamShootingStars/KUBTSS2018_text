package com.example.hongu.kubtss2018_text;

import android.os.Handler;
import android.util.Log;

import java.util.LinkedList;

/**
 * Created by takashi on 2017/03/16.
 */
public class CloudLoggerAdapter{
    private SetValueThread mSetValueThread;
    SensorAdapter mSensorAdapter;
    ReceivedDataAdapter mReceivedDataAdapter;
    CloudLoggerService mCloudLoggerService;
    public CloudLoggerAdapter(SensorAdapter mmSenserAdapter,ReceivedDataAdapter mmReceivedAdapter,CloudLoggerService mmCloudLoggerService){
        mSensorAdapter = mmSenserAdapter;
        mReceivedDataAdapter = mmReceivedAdapter;
        mCloudLoggerService = mmCloudLoggerService;
        mSetValueThread = new SetValueThread(mSensorAdapter, mReceivedDataAdapter,mCloudLoggerService);
        mSetValueThread.start();
    }

    private class SetValueThread extends Thread {
        private LinkedList<String> data;
        SensorAdapter mSensorAdapter;
        ReceivedDataAdapter mReceivedadapter;
        CloudLoggerService mCloudLoggerService;
        Handler handler = new Handler();
        private boolean running = true;
        public SetValueThread(SensorAdapter mSensorAdapter, ReceivedDataAdapter mReceivedDataAdapter,CloudLoggerService mCloudLoggerService){
            this.mSensorAdapter = mSensorAdapter;
            this.mReceivedadapter = mReceivedDataAdapter;
            this.mCloudLoggerService = mCloudLoggerService;
        }
        public void start(){
            new Thread(this).start();
            Log.d("TAG", "thread start");
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
                        data = new LinkedList<String>();
                        data.add(String.valueOf(mSensorAdapter.getPitch()));
                        data.add(String.valueOf(mSensorAdapter.getYaw()));
                        data.add(String.valueOf(mSensorAdapter.getRoll()));
                        ///////
                        mCloudLoggerService.bufferedWrite(data);
                        Log.d("TAG", "buffer write");
                    }
                });
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.e("TAG", "SetValueThread exception");
                }
            }
        }

    }
}
