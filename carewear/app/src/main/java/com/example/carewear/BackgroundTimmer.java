package com.example.carewear;

import android.app.Service;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
public class BackgroundTimmer extends Service {
    public int isTimerFinished  = 0;

    String TAG = "BackgroundTimmer";
    public static final   String COUNTDOWN_BR  = "com.example.carewear";

    CountDownTimer countDownTimer = null;

    Intent intent = new Intent(COUNTDOWN_BR);

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        countDownTimer = new CountDownTimer(30000,1000) {
            @Override
            public void onTick(long l) {
                Log.i(TAG,"Countdown millies remaining: " + l);
                intent.putExtra("countdown",l/1000);
                sendBroadcast(intent);

            }

            @Override
            public void onFinish() {
                // Do something.
                isTimerFinished = 1;
                Log.i(TAG, "Timmer is finished - " + isTimerFinished);

            }
        };
        countDownTimer.start();
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        countDownTimer.cancel();
        super.onDestroy();
    }
}
