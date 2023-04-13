package com.example.carewear;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.Nullable;
public class BackgroundTimer extends Service {
    public int isTimerFinished  = 0;
    String TAG = "BackgroundTimer";
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
                Log.i(TAG, "Timer is finished - " + isTimerFinished);
                intent.putExtra("isTimerFinished",isTimerFinished);
                sendBroadcast(intent);
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
