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
    private static final long INTERVAL = 5 * 60 * 1000; // 1 minutes in milliseconds
    private static final int NOTIFICATION_ID = 1;
    private long startTime;
    private static final String CHANNEL_ID = "sensor_notification_channel";
    // Global variables to store accelerometer and gyroscope values
    private String[] accelerometerValues;
    private String[] gyroscopeValues ;
    // Constants for sampling rate and duration
    private static final int SAMPLING_RATE_HZ = 30;

    // Arrays to store sensor data for one minute
    private List<String[]> accelerometerData = new ArrayList<>();
    private List<String[]> gyroscopeData = new ArrayList<>();

    private Sensor heartRateSensor;
    private String[] heartRateValues;
    private List<String[]> heartRateData = new ArrayList<>();
    private final Object heartRateLock = new Object();


    private long lastWriteTimestamp = 0;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
    private long currentTimeMillis = System.currentTimeMillis();
    private Date currentDate = new Date(currentTimeMillis);
    private String timestampString = sdf.format(currentDate);
    // Format the date as a string
    private String formattedDate = sdf.format(currentDate);

    // Other class-level variables
    private File sdCard = Environment.getExternalStorageDirectory();
    private String currentDateFile = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
    private final File dir = new File(sdCard.getAbsolutePath() + "/Download/" + currentDateFile);
    private File directory;  // Declare directory as a class-level variable
    // Add these two objects for synchronization
    private final Object accelerometerLock = new Object();
    private final Object gyroscopeLock = new Object();

    public static Intent getStartIntent(Context context) {
        return new Intent(context, SensorService.class);
    }
    @Override
    public void onCreate() {
        super.onCreate();

    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        startTime = System.currentTimeMillis();
        startForeground(NOTIFICATION_ID, createNotification());
        initSensors();
        initCSVFile();
        startDataCollection();

        return START_STICKY;
    }

    private void initSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(this, accelerometer,1000000 / SAMPLING_RATE_HZ);
        sensorManager.registerListener(this, gyroscope,1000000 / SAMPLING_RATE_HZ);
        sensorManager.registerListener(this, heartRateSensor, 1000000);  // 1 sample per second (1 Hz)

    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    protected void onResume() {
//        initSensors();
    }
    private void initCSVFile() {
        directory = new File(Environment.getExternalStorageDirectory(), "SensorData");

        if (!directory.exists()) {
            directory.mkdirs();
        }

        if (!dir.exists()) {
            dir.mkdirs();
        }

    }
    private void startDataCollection() {
        long delayBetweenSamples = 1000 / SAMPLING_RATE_HZ; // Delay between samples in milliseconds

        // Schedule the timer to run every 5 minutes
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
//                Log.d(TAG, "Samples Count: " + accelerometerData.size());
                // Save sensor data to arrays
//                if (accelerometerValues != null) {
//                    accelerometerData.add(accelerometerValues);
//                }
//
//                if (gyroscopeValues != null) {
//                    gyroscopeData.add(gyroscopeValues);
//                }
//                if (heartRateValues != null) {
//                    heartRateData.add(heartRateValues);
//                }

                // Check if 5 minutes have passed since the last write
                if (System.currentTimeMillis() - lastWriteTimestamp >= INTERVAL) {
                    // Create a new CSV file and write all data
                    saveSensorDataToCSV();
                    // Reset arrays for the new 5-minute interval
                    accelerometerData.clear();
                    gyroscopeData.clear();
                    // Update the last write timestamp
                    lastWriteTimestamp = System.currentTimeMillis();
                    // Update the start time for the next interval
                    startTime = lastWriteTimestamp;
                }
            }
        }, 0, delayBetweenSamples);
    }

    private void saveSensorDataToCSV() {
        try {
            // Create copies of the lists to avoid ConcurrentModificationException
            List<String[]> accelerometerDataCopy = new ArrayList<>(accelerometerData);
            List<String[]> gyroscopeDataCopy = new ArrayList<>(gyroscopeData);
            List<String[]> heartRateDataCopy = new ArrayList<>(heartRateData);

            // Write accelerometer data to CSV
            writeSensorDataToCSV(accelerometerDataCopy, "acc", timestampString);
            // Write gyroscope data to CSV
            writeSensorDataToCSV(gyroscopeDataCopy, "gry", timestampString);
            writeSensorDataToCSV(heartRateDataCopy, "heart_rate", timestampString);

            Log.d(TAG, "======================== Saved Data to CSV ========================");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void writeHeartRateDataToCSV(List<String[]> sensorData, String sensorType, String timestampString) throws IOException {
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Create separate CSV file for heart rate data
        String sanitizedTimestamp = timestampString.replaceAll("[^a-zA-Z0-9.-]", "_");
        File csvFile = new File(dir, sensorType + "_" + sanitizedTimestamp + ".csv");
        FileWriter csvWriter = new FileWriter(csvFile, true);

        // Write CSV header if the file is newly created
        if (csvFile.length() == 0) {
            csvWriter.append("HeartRate,Timestamp\n");
        }

        // Write heart rate data to CSV
        for (String[] values : sensorData) {
            csvWriter.append(values[0] + "," + values[1] + "\n");
        }

        // Flush the writer to ensure data is written immediately
        csvWriter.flush();
        csvWriter.close();
    }


    private void writeSensorDataToCSV(List<String[]> sensorData, String sensorType, String timestampString) throws IOException {
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Create separate CSV file for each sensor type
        String sanitizedTimestamp = timestampString.replaceAll("[^a-zA-Z0-9.-]", "_");
        File csvFile = new File(dir, sensorType + "_" + sanitizedTimestamp + ".csv");
        FileWriter csvWriter = new FileWriter(csvFile, true);

        // Write CSV header if the file is newly created
        if (csvFile.length() == 0) {
            if (sensorType.equals("heart_rate")) {
                csvWriter.append("HeartRate,Timestamp\n");
            } else {
                csvWriter.append("X,Y,Z,event.timestamp,Timestamp\n");
            }
        }

        // Write sensor data to CSV
        for (String[] values : sensorData) {
            if (sensorType.equals("heart_rate")) {
                csvWriter.append(values[0] + "," + values[1] + "\n");
            } else {
                csvWriter.append(values[0] + "," + values[1] + "," + values[2] + "," + values[3] + "," + values[4] + "\n");
            }
        }

        // Flush the writer to ensure data is written immediately
        csvWriter.flush();
        csvWriter.close();
    }

    public void onSensorChanged(SensorEvent event) {
        // Get current timestamp in milliseconds
        long currentTimeMillisSensorChanged = System.currentTimeMillis();

        // Format the current date and time as a human-readable timestamp
        String formattedTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                .format(new Date(currentTimeMillisSensorChanged));

        // Update sensor data as needed
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            synchronized (accelerometerLock) {
                accelerometerValues = new String[5];

                // Handle accelerometer data
                accelerometerValues[0] = String.valueOf(event.values[0]);
                accelerometerValues[1] = String.valueOf(event.values[1]);
                accelerometerValues[2] = String.valueOf(event.values[2]);
                accelerometerValues[3] = String.valueOf(event.timestamp);  // Add timestamp in nanoseconds
                accelerometerValues[4] = formattedTimestamp; // Add timestamp in milliseconds

                // Add the data directly to the list without cloning
                accelerometerData.add(accelerometerValues);
                Log.d(TAG, "Timestamp: " + formattedTimestamp);
            }
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            synchronized (gyroscopeLock) {
                gyroscopeValues = new String[5];
                // Handle gyroscope data
                gyroscopeValues[0] = String.valueOf(event.values[0]);
                gyroscopeValues[1] = String.valueOf(event.values[1]);
                gyroscopeValues[2] = String.valueOf(event.values[2]);
                gyroscopeValues[3] = String.valueOf(event.timestamp);  // Add timestamp in nanoseconds
                gyroscopeValues[4] = formattedTimestamp; // Add timestamp in milliseconds

                // Add the data directly to the list without cloning
                gyroscopeData.add(gyroscopeValues);
            }
        }
        else if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            Log.d(TAG, "Heart rate event received");
            // Handle heart rate data
            synchronized (heartRateLock) {
                heartRateValues = new String[2];
                heartRateValues[0] = String.valueOf(event.values[0]);  // Heart rate value
                heartRateValues[1] = formattedTimestamp;  // Timestamp in milliseconds
                heartRateData.add(heartRateValues);
            }
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
            sensorManager.unregisterListener(this, heartRateSensor);

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
                .setContentText("Collecting sensor data including heart rate")
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