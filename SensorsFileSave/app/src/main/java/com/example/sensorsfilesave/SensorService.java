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
    private String[] accelerometerValues; //= new float[5];
    private String[] gyroscopeValues ;//= new float[5];
    // Constants for sampling rate and duration
    private static final int SAMPLING_RATE_HZ = 30;
    private static final long DURATION_IN_SECONDS = 60;
    // Arrays to store sensor data for one minute
    private List<String[]> accelerometerData = new ArrayList<>();
    private List<String[]> gyroscopeData = new ArrayList<>();
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
    private final File dir = new File(sdCard.getAbsolutePath() + "/Download/");
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
        initCSVFile();

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
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(this, accelerometer,1000000 / 30);
        sensorManager.registerListener(this, gyroscope,1000000 / 30);
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    protected void onResume() {
        initSensors();
    }
    private void initCSVFile() {
        directory = new File(Environment.getExternalStorageDirectory(), "SensorData");

        if (!directory.exists()) {
            directory.mkdirs();
        }

        if (!dir.exists()) {
            dir.mkdirs();
        }

//        try {
//            csvWriter = new FileWriter(csvFile, true);
//            // Write CSV header if the file is newly created
//            if (csvFile.length() == 0) {
//                csvWriter.append("Timestamp,Accelerometer_X,Accelerometer_Y,Accelerometer_Z,Gyroscope_X,Gyroscope_Y,Gyroscope_Z\n");
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
    private void startDataCollection() {
        long delayBetweenSamples = 1000 / SAMPLING_RATE_HZ; // Delay between samples in milliseconds

        // Schedule the timer to run every sampling interval
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Log the number of samples collected
                Log.d(TAG, "Samples Count: " + accelerometerData.size());

                // Save sensor data to arrays
                if (accelerometerValues != null) {
                    accelerometerData.add(accelerometerValues.clone());
                }

                if (gyroscopeValues != null) {
                    gyroscopeData.add(gyroscopeValues.clone());
                }

                // Check if one minute has passed
                if (accelerometerData.size() >= DURATION_IN_SECONDS * SAMPLING_RATE_HZ) {
                    // Create a new CSV file and write all data
                    saveSensorDataToCSV();
                    // Reset arrays for the new minute
                    accelerometerData.clear();
                    gyroscopeData.clear();
                }
            }
        }, 0, delayBetweenSamples);
    }

    private void saveSensorDataToCSV() {
        try {
            // Create copies of the lists to avoid ConcurrentModificationException
            List<String[]> accelerometerDataCopy = new ArrayList<>(accelerometerData);
            List<String[]> gyroscopeDataCopy = new ArrayList<>(gyroscopeData);

            // Write accelerometer data to CSV
            writeSensorDataToCSV(accelerometerDataCopy, "acc", timestampString);

            // Write gyroscope data to CSV
            writeSensorDataToCSV(gyroscopeDataCopy, "gry", timestampString);

            Log.d(TAG, "Saved Data to CSV========================");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void writeSensorDataToCSV(List<String[]> sensorData, String sensorType, String timestampString) throws IOException {
        if (!directory.exists()) {
            directory.mkdirs();
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        // Create separate CSV file for each sensor type
        String sanitizedTimestamp = timestampString.replaceAll("[^a-zA-Z0-9.-]", "_");
        File csvFile = new File(dir, sensorType + "_" + sanitizedTimestamp + ".csv");
        FileWriter csvWriter = new FileWriter(csvFile, true);

        // Write CSV header if the file is newly created
        if (csvFile.length() == 0) {
//            csvWriter.append("Timestamp,X,Y,Z\n");
        }

        // Write sensor data to CSV
        for (String[] values : sensorData) {
//            csvWriter.append(timestampString + ",");
            csvWriter.append(values[0] + "," + values[1] + "," + values[2] +  "," + values[3] +  "," + values[4] + "\n");
        }

        // Flush the writer to ensure data is written immediately
        csvWriter.flush();
        csvWriter.close();
    }
    public void onSensorChanged(SensorEvent event) {
        // Get current timestamp in milliseconds
        long currentTimeMillisSensorChanged = System.currentTimeMillis();

        // Format the current date and time as a human-readable timestamp
        String formattedTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date(currentTimeMillisSensorChanged));

        // Update sensor data as needed
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            synchronized (accelerometerLock) {
                accelerometerValues = new String[5];

                // Handle accelerometer data
                accelerometerValues[0] = String.valueOf(event.values[0]);
                accelerometerValues[1] = String.valueOf(event.values[1]);
                accelerometerValues[2] = String.valueOf(event.values[2]);
                accelerometerValues[3] = String.valueOf(event.timestamp) ;  // Add timestamp in nanoseconds
                accelerometerValues[4] = formattedTimestamp; // Add timestamp in milliseconds
                accelerometerData.add(accelerometerValues.clone());
                Log.d(TAG, "Timestamp: " + formattedTimestamp);
            }
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            synchronized (gyroscopeLock) {
                gyroscopeValues = new String[5];

                // Handle gyroscope data
                gyroscopeValues[0] =  String.valueOf(event.values[0]);
                gyroscopeValues[1] = String.valueOf(event.values[1]);
                gyroscopeValues[2] = String.valueOf(event.values[2]);
                gyroscopeValues[3] = String.valueOf(event.timestamp) ;  // Add timestamp in nanoseconds
                gyroscopeValues[4] = formattedTimestamp; // Add timestamp in milliseconds
                gyroscopeData.add(gyroscopeValues.clone());
//                Log.d(TAG, "Timestamp: " + formattedTimestamp);
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