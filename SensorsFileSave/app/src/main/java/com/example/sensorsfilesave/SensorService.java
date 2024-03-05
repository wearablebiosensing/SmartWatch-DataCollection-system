package com.example.sensorsfilesave;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
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
    private Sensor accelerometer, gyroscope,orientation;
    private FileWriter csvWriter;
    private Timer timer;
    // Constants for sampling rate and duration
    private static final int SAMPLING_RATE_HZ = 30;
    private static final long INTERVAL = (5 * 60 * 1000) / (SAMPLING_RATE_HZ * 60); // Interval in milliseconds per sample

    private static final int NOTIFICATION_ID = 1;
    private long startTime;
    private static final String CHANNEL_ID = "sensor_notification_channel";
    // Global variables to store accelerometer and gyroscope values
    private String[] accelerometerValues;
    private String[] gyroscopeValues;
    // Arrays to store sensor data for one minute
    private List<String[]> accelerometerData = new ArrayList<>();
    private List<String[]> gyroscopeData = new ArrayList<>();
    private List<String[]> heartRateData = new ArrayList<>();
    private List<String[]> orientationData = new ArrayList<>();

    private Sensor heartRateSensor;
    private String[] heartRateValues;
    private String[] orientationValues;

    private long lastWriteTimestamp = System.currentTimeMillis();
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
    private final Object heartRateLock = new Object();
    private final Object OrientationLock = new Object();
    
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
//        initCSVFile();
        startDataCollection();

        return START_STICKY;
    }

    private void initSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        orientation = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);


        sensorManager.registerListener(this, accelerometer,1000000 / SAMPLING_RATE_HZ,1000000 / SAMPLING_RATE_HZ);
        sensorManager.registerListener(this, gyroscope,1000000 / SAMPLING_RATE_HZ,1000000 / SAMPLING_RATE_HZ);
        sensorManager.registerListener(this, orientation,1000000 / SAMPLING_RATE_HZ,1000000 / SAMPLING_RATE_HZ);

        sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);  // 1 sample per second (1 Hz)
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    protected void onResume() {
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
        // Schedule the timer to run every 5 minutes
        // Schedule the timer to run every 5 minutes
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long ts_diff = (System.currentTimeMillis() - lastWriteTimestamp)/1000;
                Log.d(TAG, String.format("Time Elapsed in seconds: " + ts_diff));
                // Check if the number of samples collected within 5 minutes has reached the expected value
                if (System.currentTimeMillis() - lastWriteTimestamp >= (5 * 60 * 1000)) {
                    startTime = System.currentTimeMillis();
                    initCSVFile();
                    // Save sensor data to CSV files
                    saveSensorDataToCSV(accelerometerData,gyroscopeData,orientationData,heartRateData);
                    lastWriteTimestamp = System.currentTimeMillis();

                    // Clear the sensor data lists for the next interval
                    accelerometerData.clear();
                    gyroscopeData.clear();
                    heartRateData.clear();
                    orientationData.clear();
                }
            }
        }, 0, delayBetweenSamples);
    }
    private void saveSensorDataToCSV(List<String[]> accelerometerData,List<String[]> gyroscopeData,List<String[]> orientationData, List<String[]> heartRateData) {
        try {
            initCSVFile();
            // Create copies of the lists to avoid ConcurrentModificationException
            List<String[]> accelerometerDataCopy = new ArrayList<>(accelerometerData);
            List<String[]> gyroscopeDataCopy = new ArrayList<>(gyroscopeData);
            List<String[]> orientationDataCopy = new ArrayList<>(orientationData);

            List<String[]> heartRateDataCopy = new ArrayList<>(heartRateData);
            // Update timestampString for the current interval
            timestampString = sdf.format(new Date(System.currentTimeMillis()));

            Log.d(TAG, "saveSensorDataToCSV(): timestampString: " + timestampString);

            // Write accelerometer data to CSV
            writeSensorDataToCSV(accelerometerDataCopy, "acc", timestampString);
            Log.d(TAG, "========================    ACC Saved Data to CSV ========================" + timestampString);

            // Write gyroscope data to CSV
            writeSensorDataToCSV(gyroscopeDataCopy, "gry", timestampString);
            Log.d(TAG, "========================    GRY Saved Data to CSV ========================" + timestampString);

            writeSensorDataToCSV(orientationDataCopy, "orientation", timestampString);
            Log.d(TAG, "========================    Roll,Pitch,Yaw Saved Data to CSV ========================" + timestampString);

            writeSensorDataToCSV(heartRateDataCopy, "heart_rate", timestampString + timestampString);
            Log.d(TAG, "========================    HR Saved Data to CSV ========================" + timestampString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeHeartRateDataToCSV(List<String[]> sensorData, String sensorType, String timestampString) throws IOException {
        if (!directory.exists()) {
            directory.mkdirs();
        }
        Log.d(TAG, "writeHeartRateDataToCSV(): timestampString: " + timestampString);

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
        Log.d(TAG, "writeSensorDataToCSV() ACC and GRY : timestampString: " + timestampString);


        // Create separate CSV file for each sensor type
        String sanitizedTimestamp = timestampString.replaceAll("[^a-zA-Z0-9.-]", "_");
        File csvFile = new File(dir, sensorType + "_" + sanitizedTimestamp + ".csv");
        FileWriter csvWriter = new FileWriter(csvFile, true);

        // Write CSV header if the file is newly created
        if (csvFile.length() == 0) {
            if (sensorType.equals("heart_rate")) {
                csvWriter.append("HeartRate,Timestamp\n");
            } else if (sensorType.equals("orientation")){
                csvWriter.append("roll,pitch,yaw,event.timestamp,Timestamp\n");
            }
            else if (sensorType.equals("gry")){
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
//                Log.d(TAG, "Accelerometer  event received");
                // Handle accelerometer data
                accelerometerValues[0] = String.valueOf(event.values[0]);
                accelerometerValues[1] = String.valueOf(event.values[1]);
                accelerometerValues[2] = String.valueOf(event.values[2]);
                accelerometerValues[3] = String.valueOf(event.timestamp) ;//String.valueOf(event.timestamp);  // Add timestamp in nanoseconds
                accelerometerValues[4] = formattedTimestamp; // Add timestamp in milliseconds

                // Add the data directly to the list without cloning
                accelerometerData.add(accelerometerValues);
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
           // Log.d(TAG, "Heart rate event received");
            // Handle heart rate data
            synchronized (heartRateLock) {
                heartRateValues = new String[2];
                heartRateValues[0] = String.valueOf(event.values[0]);  // Heart rate value
                heartRateValues[1] = formattedTimestamp;  // Timestamp in milliseconds
                heartRateData.add(heartRateValues);
            }
        }
        else if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
            // Log.d(TAG, "Heart rate event received");
            // Handle heart rate data
            synchronized (OrientationLock) {

                orientationValues = new String[5];
                // Handle accelerometer data
                orientationValues[0] = String.valueOf(event.values[0]); // Azimuth (angle around the z-axis). Degrees
                orientationValues[1] = String.valueOf(event.values[1]); // Pitch (angle around the x-axis). Degrees
                orientationValues[2] = String.valueOf(event.values[2]);// Roll (angle around the y-axis). Degrees
                orientationValues[3] = String.valueOf(event.timestamp) ;//String.valueOf(event.timestamp);  // Add timestamp in nanoseconds
                orientationValues[4] = formattedTimestamp; // Add timestamp in milliseconds
                orientationData.add(orientationValues);
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