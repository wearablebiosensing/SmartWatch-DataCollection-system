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
    public int isTimerFinished;
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
                isTimerFinished = 0;
                intent.putExtra("isTimerFinished",isTimerFinished);
                // Sennd broadcast to MainActivity
                intent.putExtra("countdown",l/1000);
                Log.i(TAG,"Countdown seconds remaining: " + l/1000);
                if(l/1000==0){
                    isTimerFinished=1;
                    Log.i(TAG, "onTick(): Timer is finished - " + isTimerFinished);

                    intent.putExtra("isTimerFinished",isTimerFinished);
                    sendBroadcast(intent);
                }
                else if(l/1000<30){
                    isTimerFinished=0;
                    intent.putExtra("isTimerFinished",isTimerFinished);
                    sendBroadcast(intent);
                }
                sendBroadcast(intent);


            }

            @Override
            public void onFinish() {
                isTimerFinished=0;
                sendBroadcast(intent);

                // set boolean variable isTimerFinished to 1 i.e timer is finished
//                isTimerFinished = 1;
//                Log.i(TAG, "Timer is finished - " + isTimerFinished);
//                // Broadcast the Intent to the SensorsService Class that the timer is done.
//                intent.putExtra("isTimerFinished",isTimerFinished);
//                sendBroadcast(intent);
                // Repeat the timer every time it is done.
                countDownTimer.start();

            }
        };

        countDownTimer.start();
        super.onCreate();


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
