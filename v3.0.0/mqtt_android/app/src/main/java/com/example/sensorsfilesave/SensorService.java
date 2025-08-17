package com.example.sensorsfilesave;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import info.mqtt.android.service.Ack;
import info.mqtt.android.service.MqttAndroidClient;

public class SensorService extends Service implements SensorEventListener {
    private static final String TAG = "SensorService";
    private SensorManager sensorManager;
    private Sensor gyroscope;
    private Sensor heartRateSensor;
    private Sensor acceleration;
    private Sensor linearAcceleration;

    private String watchID;

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "sensor_notification_channel";

    private HashMap<Integer, ArrayList<String>> sensorDataBuffers = new HashMap<>();
    private static final int BUFFER_SIZE = 2;

    private MqttAndroidClient mqttAndroidClient;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "SensorService onStartCommand.");
        try {
            if (intent != null) {
                String action = intent.getStringExtra("action");
                if ("start".equals(action)) {
                    initMqttClient();
                } else if ("stop".equals(action)) {
                    disconnectMqttClient();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onStartCommand: " + e.getMessage(), e);
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "SensorService created.");

        try {
            sensorDataBuffers.put(Sensor.TYPE_GYROSCOPE, new ArrayList<>());
            sensorDataBuffers.put(Sensor.TYPE_ACCELEROMETER, new ArrayList<>());
            sensorDataBuffers.put(Sensor.TYPE_HEART_RATE, new ArrayList<>());
            sensorDataBuffers.put(Sensor.TYPE_LINEAR_ACCELERATION, new ArrayList<>());

            initSensors();
            watchID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID).substring(0, 6);
            startForeground(NOTIFICATION_ID, createNotification());
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
        }
    }

    private void initSensors() {
        try {
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager != null) {
                gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
                heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
                acceleration = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                linearAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

                sensorManager.registerListener(this, gyroscope, 33333);
                sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
                sensorManager.registerListener(this, acceleration, 33333);
                sensorManager.registerListener(this, linearAcceleration, 33333);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in initSensors: " + e.getMessage(), e);
        }
    }

    private void initMqttClient() {
        try {
            if (mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
                Log.d("SensorService", "Already connected or connection in progress.");
                return;
            }
            String clientId = MqttClient.generateClientId();
            mqttAndroidClient = new MqttAndroidClient(this, "tcp://broker.hivemq.com:1883", clientId, Ack.AUTO_ACK);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            if (mqttAndroidClient.isConnected()) {
                Log.d(TAG, "Already connected or connection in progress.");
                return;
            }
            Log.d(TAG, "Connecting to MQTT broker...");
            mqttAndroidClient.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Connected to MQTT broker.");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Failed to connect to MQTT broker.", exception);
                    if (exception instanceof MqttException) {
                        MqttException mqttException = (MqttException) exception;
                        Log.e(TAG, "Reason: " + mqttException.getReasonCode());
                        Log.e(TAG, "Message: " + mqttException.getMessage());
                        if (mqttException.getCause() != null) {
                            Log.e(TAG, "Cause: " + mqttException.getCause().toString());
                        }
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in initMqttClient: " + e.getMessage(), e);
        }
    }

    private void disconnectMqttClient() {
        try {
            if (mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
                mqttAndroidClient.disconnect();
                mqttAndroidClient = null;
                Log.d(TAG, "Disconnected from MQTT broker.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in disconnectMqttClient: " + e.getMessage(), e);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
            String formattedDate = sdf.format(new Date());
            String topicBase = watchID + "/";
            String type = "";
            String data = "";
            int sensorType = event.sensor.getType();

            switch (sensorType) {
                case Sensor.TYPE_GYROSCOPE:
                    type = "gyroscope";
                    data = String.format("%f,%f,%f,%d,%s",
                            event.values[0], event.values[1], event.values[2],
                            event.timestamp, formattedDate);
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    type = "accelerometer";
                    data = String.format("%f,%f,%f,%d,%s",
                            event.values[0], event.values[1], event.values[2],
                            event.timestamp, formattedDate);
                    break;
                case Sensor.TYPE_HEART_RATE:
                    type = "heartrate";
                    data = String.format("%f,%d,%s",
                            event.values[0],
                            event.timestamp, formattedDate);
                    break;
                case Sensor.TYPE_LINEAR_ACCELERATION:
                    type = "linear_acceleration";
                    data = String.format("%f,%f,%f,%d,%s",
                            event.values[0], event.values[1], event.values[2],
                            event.timestamp, formattedDate);
                    break;
                default:
                    data = formattedDate;
                    break;
            }

            ArrayList<String> buffer = sensorDataBuffers.get(sensorType);
            if (buffer != null) {
                buffer.add(data);
                if (buffer.size() >= BUFFER_SIZE) {
                    String topic = topicBase + type;
                    String payload = TextUtils.join("\n", buffer);
                    Log.i(TAG, "Error in payload: " + payload);

                    publishSensorData(topic, payload);
                    buffer.clear();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onSensorChanged: " + e.getMessage(), e);
        }
    }

    private void publishSensorData(String topic, String payload) {
        Log.d(TAG, "MQTT Sensors Manager Topic: " + topic );
//        Log.d(TAG, "MQTT Sensors Manager payload: " + payload );

        try {
            if (mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
                mqttAndroidClient.publish(topic, new MqttMessage(payload.getBytes()));
                //Log.d(TAG, "Data published to topic: " + topic);
            } else {
                //Log.e(TAG, "MQTT client is not connected. Cannot publish to topic: " + topic);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in publishSensorData: " + e.getMessage(), e);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (sensorManager != null) {
                sensorManager.unregisterListener(this);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: " + e.getMessage(), e);
        }
    }

    private Notification createNotification() {
        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Sensor Service")
                    .setContentText("Publishing sensor data to MQTT topics")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID, "Sensor Service Channel", NotificationManager.IMPORTANCE_DEFAULT);
                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                }
            }
            return builder.build();
        } catch (Exception e) {
            Log.e(TAG, "Error in createNotification: " + e.getMessage(), e);
            return null;
        }
    }

    /*
     * Writes the CSV file with the current timestamp in the file name for accelerometer data.
     * Takes in the toggle button view, the data to be added, and filename Eg: acc,gry ...
     * */
    public void save_data(View view, ArrayList<String> data, String filename){
        System.out.println("BUTTON PRESSED : Sensors Button Pressed");
        try{
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File(sdCard.getAbsolutePath() + "/Download");
            System.out.println("DIRECTORY: ---" + dir.toString());
            if(!dir.exists()) { // if directory does not exist then create one.
                dir.mkdirs();
            }
            Date currentTime = Calendar.getInstance().getTime();
            long time= System.currentTimeMillis();
            System.out.println("DATE AND TIME CURRENT: ---" + time);
            // Depending on the user selection enter the.
            File file = new File(dir, "/"+ filename +"_"+ time +".csv");
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream f = new FileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(f, StandardCharsets.UTF_8);
            // Buffer is needed to create the UTF 8 formatting and
            BufferedWriter writer = new BufferedWriter(osw);
            String mHeader ="Timestamp," + "x," + "y," + "z";
            writer.append(mHeader);
            writer.newLine();
            try {
                for (int i = 0 ; i <data.size() ; i++){
                    writer.append(data.get(i));
                    writer.newLine();
                }
                f.flush();
                f.close();
                Toast.makeText(getBaseContext(), filename + " Data saved", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
