package com.example.sensorsfilesave;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class SensorService extends Service implements SensorEventListener {
    String TAG = "SensorService";
    // Check and request necessary permissions
    private SensorManager sensorManager;
    private Sensor accelerometer, gyroscope;
    private FileWriter csvWriter;
    private Timer timer;
    private static final long INTERVAL = 1 * 60 * 1000; // 1 minutes in milliseconds
    private static final int NOTIFICATION_ID = 1;
    private long startTime;
    private static final String CHANNEL_ID = "sensor_notification_channel";
    // Global variables to store accelerometer and gyroscope values
    private float[] accelerometerValues = new float[3];
    private float[] gyroscopeValues = new float[3];


    // Constants for sampling rate and duration
    private static final int SAMPLING_RATE_HZ = 30;
    private static final long DURATION_IN_SECONDS = 60;

    // Arrays to store sensor data for one minute
    private List<float[]> accelerometerData = new ArrayList<>();
    private List<float[]> gyroscopeData = new ArrayList<>();

    public static Intent getStartIntent(Context context) {
        return new Intent(context, SensorService.class);
    }
    @Override
    public void onCreate() {
        super.onCreate();
        initCSVFile();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        startTime = System.currentTimeMillis();

        startTime = System.currentTimeMillis();
        startForeground(NOTIFICATION_ID, createNotification());
        onResume();
        startDataCollection();

        return START_STICKY;
    }

    private void initSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(this, accelerometer,1000000 / 30);
        sensorManager.registerListener(this, gyroscope,1000000 / 30);
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    protected void onResume() {

        initSensors();


    }
    private void initCSVFile() {
        File directory = new File(Environment.getExternalStorageDirectory(), "SensorData");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File sdCard = Environment.getExternalStorageDirectory();
        String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

        final File dir = new File(sdCard.getAbsolutePath() + "/Download/" + currentDate);

        File csvFile = new File(dir, "sensor_data_" +System.currentTimeMillis()+".csv");
        try {
            csvWriter = new FileWriter(csvFile, true);
            // Write CSV header if the file is newly created
            if (csvFile.length() == 0) {
                csvWriter.append("Timestamp,Accelerometer_X,Accelerometer_Y,Accelerometer_Z,Gyroscope_X,Gyroscope_Y,Gyroscope_Z\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void startDataCollection() {
        // Calculate the delay between samples in microseconds for a given sampling rate
        long delayBetweenSamples = 1000000 / SAMPLING_RATE_HZ;

        // Calculate the duration of one minute in milliseconds
        long oneMinuteInMillis = DURATION_IN_SECONDS * 1000;

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            long startTimeMillis = System.currentTimeMillis();

            @Override
            public void run() {
                // Calculate elapsed time in milliseconds
                long elapsedTimeMillis = System.currentTimeMillis() - startTimeMillis;

                // Check if one minute has passed
                if (elapsedTimeMillis >= oneMinuteInMillis) {
                    // If yes, create a new CSV file and write all data
                    initCSVFile();
                    saveSensorDataToCSV(accelerometerData, gyroscopeData);
                    int numberOfRows = accelerometerData.size();
                    int numberOfRowsGry = gyroscopeData.size();

                    Log.d(TAG, "Count Accelerometer: ========================" + numberOfRows);
                    Log.d(TAG, "Count Gryoscope: ========================" + numberOfRowsGry);

                    Log.d(TAG, "Saved Data to CSV========================");

                    // Reset arrays for the new minute
                    accelerometerData.clear();
                    gyroscopeData.clear();

                    startTimeMillis = System.currentTimeMillis();
                }

                // Log the number of samples collected
//                Log.d(TAG, "Samples Count: " + accelerometerData.size());

                // Save sensor data to arrays
                accelerometerData.add(accelerometerValues.clone());
                gyroscopeData.add(gyroscopeValues.clone());
            }
        }, 0, delayBetweenSamples); // Set the delay between samples
    }


    private void saveSensorDataToCSV(List<float[]> accelerometerData, List<float[]> gyroscopeData) {
        try {
            for (int i = 0; i < accelerometerData.size(); i++) {
                float[] accelerometerValues = accelerometerData.get(i);
                float[] gyroscopeValues = gyroscopeData.get(i);

                // Get the current timestamp
                long timestamp = System.currentTimeMillis();

                // Append sensor data to CSV
                csvWriter.append(timestamp + ",");
                csvWriter.append(accelerometerValues[0] + "," + accelerometerValues[1] + "," + accelerometerValues[2] + ",");
                csvWriter.append(gyroscopeValues[0] + "," + gyroscopeValues[1] + "," + gyroscopeValues[2] + "\n");
            }

            // Flush the writer to ensure data is written immediately
            csvWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onSensorChanged(SensorEvent event) {
        // Update sensor data as needed
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Handle accelerometer data
//            accelerometerValues = event.values.clone();
            accelerometerValues.add(accelerometerData);
            //Log.d(TAG, "Accelerometer Values: X=" + accelerometerValues[0] + ", Y=" + accelerometerValues[1] + ", Z=" + accelerometerValues[2]);
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            // Handle gyroscope data
            gyroscopeValues = event.values.clone();
          //  Log.d(TAG, "Gyroscope Values: X=" + gyroscopeValues[0] + ", Y=" + gyroscopeValues[1] + ", Z=" + gyroscopeValues[2]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle accuracy changes if needed
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    // ... rest of the methods ...
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up resources when the service is destroyed

        // Cancel the timer only if it's not null
        if (timer != null) {
            timer.cancel();
        }

        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        try {
            if (csvWriter != null) {
                csvWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Foreground Service")
                .setContentText("Collecting sensor data")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        return builder.build();
    }
}
