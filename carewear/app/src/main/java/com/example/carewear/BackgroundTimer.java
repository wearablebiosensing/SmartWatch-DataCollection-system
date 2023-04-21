package com.example.carewear;

import android.app.Service;
import android.content.BroadcastReceiver;
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
    int countdownSeconds = 30;
    Intent intent = new Intent(COUNTDOWN_BR);
    public int fileIsWritten;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        countDownTimer = new CountDownTimer(countdownSeconds*1000,1000) {
            @Override
            public void onTick(long l) {
                isTimerFinished = 0;
                intent.putExtra("isTimerFinished",isTimerFinished);
                // Sennd broadcast to MainActivity
                intent.putExtra("countdown",l/1000);
              //  Log.i(TAG, "Intent from Sensor Activity fileIsWritten: "+ fileIsWritten);
                Log.i(TAG,"Countdown miliseconds remaining: " + l);
                Log.i(TAG,"Countdown seconds remaining: " + l/1000);
                sendBroadcast(intent);

                if(l/1000==0){
                    isTimerFinished=1;// is timer finished? 1 - true.
                    Log.i(TAG, "onTick(): Timer is finished - " + isTimerFinished);

                    intent.putExtra("isTimerFinished",isTimerFinished);
                    sendBroadcast(intent);
                }
                else if(l/1000<countdownSeconds){

                    // set timer finished variable to 0 and then send broadcast event
                    isTimerFinished=0;
                    intent.putExtra("isTimerFinished",isTimerFinished);
                    sendBroadcast(intent);
                }
                else{
                   // set timer finished variable to 0 and then send broadcast event
                   isTimerFinished=0;
                   intent.putExtra("isTimerFinished",isTimerFinished);
                   sendBroadcast(intent);
               }
                // else timer is not finished so send a 0.
//                isTimerFinished = 0;
//                intent.putExtra("isTimerFinished",isTimerFinished);
//                sendBroadcast(intent);

            }

            @Override
            public void onFinish() {
                isTimerFinished=1;
                Log.i(TAG, "onFinish(): Timer is finished - " + isTimerFinished);

                sendBroadcast(intent);
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
