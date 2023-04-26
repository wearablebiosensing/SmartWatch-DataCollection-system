package com.example.carewear;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import androidx.core.app.NotificationCompat;

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
               // Log.i(TAG,"Countdown miliseconds remaining: " + l);
                Log.i(TAG,"Countdown seconds remaining: " + l/1000);
                sendBroadcast(intent);
                Log.i(TAG,"onTick(): Background Timer isTimerFinished: " + isTimerFinished);

                if(l/1000==0){
                    isTimerFinished=1;// is timer finished? 1 - true.
                    Log.i(TAG, "onTick(): Timer is finished - " + isTimerFinished);

                    intent.putExtra("isTimerFinished",isTimerFinished);
                    sendBroadcast(intent);

                }
                else if(l/1000<=countdownSeconds){

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
                //isTimerFinished=1;
                //Log.i(TAG, "onFinish(): Timer is finished - " + isTimerFinished);

              //  sendBroadcast(intent);
                // Repeat the timer every time it is done.
                countDownTimer.start();
            }
        };
//        String CHANNEL_ID = "my_channel_01";
//        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
//                "Channel human readable title",
//                NotificationManager.IMPORTANCE_DEFAULT);
//
//        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
//
//        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
//                .setContentTitle("")
//                .setContentText("").build();
//
//        startForeground(1, notification);
//
        countDownTimer.start();
        super.onCreate();


    }

//    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String CHANNELID = "Foreground Service ID2";
        NotificationChannel channel = new NotificationChannel(
                CHANNELID,
                CHANNELID,
                NotificationManager.IMPORTANCE_LOW
        );

        getSystemService(NotificationManager.class).createNotificationChannel(channel);
        Notification.Builder notification = new Notification.Builder(this, CHANNELID)
                .setContentText("Service is running")
                .setContentTitle("Service enabled")
                .setSmallIcon(R.drawable.ic_launcher_background);

        startForeground(1001, notification.build());


        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
