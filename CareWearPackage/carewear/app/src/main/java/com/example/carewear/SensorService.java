package com.example.carewear;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.StatFs;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.TimeZone;


public class SensorService extends Service implements SensorEventListener {
    String TAG = "SensorService";
    public static final String COUNTDOWN_BR = "com.example.carewear";
    Intent intentSensorService = new Intent(COUNTDOWN_BR);
    int intent_isFinished;
    FileIO fileio = new FileIO();
    SensorManager mSensorManagerAcc;
    SensorManager mSensorManagerGry;
    SensorManager mSensorManagerHr;
    ArrayList<String> AccelerometerData = new ArrayList<String>();
    ArrayList<String> GryData = new ArrayList<String>();
    ArrayList<String> HRData = new ArrayList<String>();
    ArrayList<String> BatteryInfo = new ArrayList<String>();
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference databaseReference;
    int level;
    private StorageReference firebaseStorage;
    FirebaseStorage storage;
    // Keep track of file names that are alreeady uploaded to firebase.
    private HashSet<String> uploadedFileNames = new HashSet<>();
    // Device ID
    private String deviceId;
    BluetoothAdapter bluetoothAdapter;
    private MQTTHelper mqttHelper;

    @Override
    public void onCreate() {
        super.onCreate();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mqttHelper = new MQTTHelper(getApplicationContext(), "tcp://test.mosquitto.org:1883");

        // Set up Firebase
        databaseReference = database.getReference("sensors_message");
        // Obtain a reference to the Firebase Storage instance
        storage = FirebaseStorage.getInstance();
//        firebaseStorage = FirebaseStorage.getInstance();
//        storageRef = storage.getReference().child("csv_files");

        // Initialize the Sensor Manager
        mSensorManagerAcc = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorManagerGry = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorManagerHr = (SensorManager) getSystemService(SENSOR_SERVICE);
        Log.d(TAG, "onCreate() mSensorManagerAcc:----  " + mSensorManagerAcc);
        Log.d(TAG, "onCreate() mSensorManagerGry:----  " + mSensorManagerGry);
        Log.d(TAG, "onCreate()  mSensorManagerHr:---- " + mSensorManagerHr);
//        File sdCard = Environment.getExternalStorageDirectory();
        /*Gets the SD CARD STORAGE AAVALIABLE Approx 7GB */
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long bytesAvailable;
        if (android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            bytesAvailable = stat.getBlockSizeLong() * stat.getAvailableBlocksLong();
        } else {
            bytesAvailable = (long) stat.getBlockSize() * (long) stat.getAvailableBlocks();
        }
        long megAvailable = bytesAvailable / (1024 * 1024 * 1024);
        Log.e(TAG, "STORAGE INFO - Available GB : " + megAvailable);
        //Change this topic to whatever we use in MindGame
        String mqttTopic = "hello/world";

    }

    /*
    * Receive Intent from the BackgroundTimer class and set the intent_isFinished to
    1 once it is done to let the class know when to save the file.
    * */
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            intent_isFinished = intent.getIntExtra("isTimerFinished", 0);
            //startWakefulService(context, SensorService.class);
        }
    };
    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            // TODO Auto-generated method stub
            level = intent.getIntExtra("level", 0);
            Log.d(TAG, "mBatInfoReceiver BATTERY LEVEL INFO ------------------- " + String.valueOf(level) + "%");
            // https://developer.android.com/reference/java/lang/System#currentTimeMillis()
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss:SSS");

            // Get the system milisecond unix time stamp
            // https://developer.android.com/reference/java/lang/System#currentTimeMillis()
            dateFormat.setTimeZone(TimeZone.getDefault()); //TimeZone.getTimeZone("UTC")

            long currentTimestampMillis = System.currentTimeMillis();
            String batteryFormattedDateTime = dateFormat.format(new Date(currentTimestampMillis));

            String batteryinfo = batteryFormattedDateTime + "," + String.valueOf(level);
            BatteryInfo.add(batteryinfo);
            //contentTxt.setText(String.valueOf(level) + "%");
        }
    };

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        final String CHANNELID = "Foreground Service ID";
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

        registerReceiver(broadcastReceiver, new IntentFilter(BackgroundTimer.COUNTDOWN_BR));
        registerReceiver(mBatInfoReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        mqttHelper.connect(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                // Connection successful
                Log.d(TAG, " CONNECTED TO MQTT ----------: " );

            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.d(TAG, "NOT CONNECTED TO MQTT ----------: " );

//                Toast.makeText(Sen.this, "Failed to Connect", Toast.LENGTH_SHORT).show();
            }
        });
        onResume();


        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    protected void onResume() {

        //  Register broadcast receiver from Background Timer to check if it is over or not.
        //Log.i(TAG,"Registered Broadcast Receiver");

        // if recording acc data register listener for acc values.
        Sensor accelerometer = mSensorManagerAcc.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // To change the sampling rate apply it in microseconds.
        // 1/#Hz * 1000000 micro seconds.
        mSensorManagerAcc.registerListener((SensorEventListener) this, accelerometer, (1 / 30) * 1000000); // 50 Hz // 20000 = 50Hz in microseconds
        Sensor gyroscope = mSensorManagerGry.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        // To change the sampling rate apply it in microseconds.
        mSensorManagerGry.registerListener((SensorEventListener) this, gyroscope, (1 / 30) * 1000000); // 50 Hz // 20000 microseconds = 50Hz in

        Sensor mHeartRate = mSensorManagerHr.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        if (mSensorManagerHr.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null) {
            Log.d(TAG, "============== HR SENSOR  AVAILABLE==============");
        } else {
            Log.d(TAG, "============== HR SENSOR NOT AVAILABLE==============");
        }
        //Log.d(TAG,"HEART RATE SENSOR ----------: "+heart_rate);

        // To change the sampling rate apply it in microseconds.
        mSensorManagerHr.registerListener((SensorEventListener) this, mHeartRate, (1 / 1) * 1000000); // 1 Hz // 1000000 = 1Hz
        Log.d(TAG, "HEART RATE SENSOR mSensorManagerHr ----------: " + mSensorManagerHr);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void getAccelerometerData(SensorEvent event) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss:SSS");
        dateFormat.setTimeZone(TimeZone.getDefault());

        long currentTimestampMillis = System.currentTimeMillis();
        String AccformattedDateTime = dateFormat.format(new Date(currentTimestampMillis));

//        System.out.println(AccformattedDateTime);

        // System.out.println(TAG + ": Intent from Background Timer Variable: intent_isFinished " + intent_isFinished);
        // event.timestamp: The time in nanoseconds at which the event happened.
        // event.values: public final float[]	values
        String data_accelerometer = event.timestamp + "," + AccformattedDateTime + "," + String.valueOf(event.values[0]) + "," + String.valueOf(event.values[1] + "," + String.valueOf(event.values[2]));
        // uncoment to push data to realtime database.
        //        databaseReference.push().setValue(data_accelerometer);
        AccelerometerData.add(data_accelerometer);


        //IMPORTANT - Get device ID to be used in specific topic
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String displayId = deviceId.substring(0, Math.min(deviceId.length(), 8)); // Display first 8 characters

        // Prepare the MQTT message payload to include both accelerometer data and battery level
        String mqttPayload = data_accelerometer;


        // existing code...
        if (mqttHelper != null && mqttHelper.isConnected()) {
            mqttHelper.publishMessage("AndroidWatch/acceleration/" + displayId, mqttPayload, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                }
                // Callback implementations...
            });
        } else {
            Log.d(TAG, "MQTT Client is not connected");
        }
    }



    public void getGryData(SensorEvent event) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss:SSS");

        // Get the system milisecond unix time stamp
        // https://developer.android.com/reference/java/lang/System#currentTimeMillis()
        dateFormat.setTimeZone(TimeZone.getDefault());

        long currentTimestampMillis = System.currentTimeMillis();
        String GryformattedDateTime = dateFormat.format(new Date(currentTimestampMillis));

//        System.out.println(GryformattedDateTime);
        String data_gryo = event.timestamp + "," + GryformattedDateTime + "," + String.valueOf(event.values[0]) + "," + String.valueOf(event.values[1]) + "," + String.valueOf(event.values[2]);

        //IMPORTANT - Get device ID to be used in specific topic
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String displayId = deviceId.substring(0, Math.min(deviceId.length(), 8)); // Display first 8 characters

        // Prepare the MQTT message payload to include both accelerometer data and battery level
        String mqttPayload = data_gryo;


        // existing code...
        if (mqttHelper != null && mqttHelper.isConnected()) {
            mqttHelper.publishMessage("AndroidWatch/gyro/" + displayId, mqttPayload, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                }
                // Callback implementations...
            });
        } else {
            Log.d(TAG, "MQTT Client is not connected");
        }

        GryData.add(data_gryo);
    }

    public void getHrData(SensorEvent event) {
        // https://developer.android.com/reference/java/lang/System#currentTimeMillis()
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss:SSS");


        // Get the system milisecond unix time stamp
        // https://developer.android.com/reference/java/lang/System#currentTimeMillis()
        dateFormat.setTimeZone(TimeZone.getDefault()); //TimeZone.getTimeZone("UTC")

        long currentTimestampMillis = System.currentTimeMillis();
        String HRformattedDateTime = dateFormat.format(new Date(currentTimestampMillis));
        String data_hr = HRformattedDateTime + "," + String.valueOf(event.values[0]);
        Log.d(TAG, "hr_data: " + String.valueOf(event.values[0])); // 402550836440 , 401550836440


        //IMPORTANT - Get device ID to be used in specific topic
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String displayId = deviceId.substring(0, Math.min(deviceId.length(), 8)); // Display first 8 characters

        // Prepare the MQTT message payload to include both accelerometer data and battery level
        String mqttPayload = data_hr;




        // existing code...
        if (mqttHelper != null && mqttHelper.isConnected()) {
            mqttHelper.publishMessage("AndroidWatch/hr/" + displayId, mqttPayload, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                }
                // Callback implementations...
            });
        } else {
            Log.d(TAG, "MQTT Client is not connected");
        }

        HRData.add(data_hr);
    }


    // SensorEvent: This class represents a Sensor event and holds information such as the sensor's type, the time-stamp, accuracy and of course the sensor's data.@Override
    @Override
    public void onSensorChanged(SensorEvent event) {
        // onResume(); // call this to register listiner for everytime HR was registered.
        // Log.i(TAG,"intent_isFinished: Intent recieved - : " +intent_isFinished);
        // Log.d(TAG,"SENSOR TYPE: event.sensor.getType() "+event.sensor.getType());
        //Log.d(TAG,"Sensor.TYPE_ACCELEROMETER  "+Sensor.TYPE_ACCELEROMETER);
        //Log.d(TAG,"Sensor.TYPE_GYROSCOPE  "+Sensor.TYPE_GYROSCOPE);
        //Log.d(TAG,"Sensor.TYPE_HEART_RATE  "+Sensor.TYPE_HEART_RATE);
        // Log.i(TAG,"onSensorChanged() broadcasted from timer intent_isFinished : " + intent_isFinished);
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            getAccelerometerData(event);
        }// start_time - 148597546056210 # second st time - 148837473364559
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            getGryData(event);
        }
        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            getHrData(event);

        }
        //Log.d(TAG,"onSensorChanged() HR DATA --" +HRData);
        //Log.d(TAG,"onSensorChanged() ACC DATA --" +AccelerometerData);
        //Log.d(TAG,"onSensorChanged() GRY DATA --" +GryData);

        // Get intent form background Timer and once finished save the files and clear the array data.
        if (intent_isFinished == 1) {
            // Log.d(TAG,"Message from backgroung timer that is done one: "+intent_isFinished);
            // Log.d(TAG,"onSensorChanged() Call SAVE DATA CLASS hr/.");
            // Log.d(TAG,"onSensorChanged() Call SAVE DATA CLASS gry/.");
            // BatteryInfo.add(batteryinfo);
            Log.d(TAG, "onSensorChanged(): bBATTERY INFO" + BatteryInfo);
            String filepath_battery = fileio.save_data(BatteryInfo, "onchange" + "_battery");
            String filepath_gry = fileio.save_data(GryData, "30Hz" + "_gry");
            // Log.d(TAG,"onSensorChanged() Call SAVE DATA CLASS gry/.");
            Log.d(TAG, "onSensorChanged() ACC DATA --" + AccelerometerData);
            String filepath_acc = fileio.save_data(AccelerometerData, "30Hz" + "_acc");
            // If Internet is avaliable then save files in the firebase storage checks network connections fter every 5 mins..
            if (NetworkUtils.isNetworkAvailable(this)) {
                // Internet connection is available
                // Perform your tasks requiring internet here
                uploadFilesFromFolderToStorage(filepath_acc);
                Toast.makeText(this, "Internet connection available", Toast.LENGTH_SHORT).show();
            } else {
                // No internet connection
                // Handle the lack of connectivity here
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            }

            Log.d(TAG, "filepath_acc(): " + filepath_acc);
            // upload to firebase
//                uploadCsvFile(filepath_acc);
            Log.d(TAG, "onSensorChanges() HRData: " + HRData);
            String filepath_hr = fileio.save_data(HRData, "1Hz" + "_hr");
            //Log.d(TAG,"onSensorChanged() Call SAVE DATA CLASS hr/." +HRData);
            // Clear the previous written array.
            BatteryInfo.clear();
            GryData.clear();
            AccelerometerData.clear();
            HRData.clear();
            // set intent finished back to 0
            intent_isFinished = 0;
        }
    }

    private void uploadFilesFromFolderToStorage(String folderPath) {
        // Get a reference to the storage location
        StorageReference storageRef = storage.getReference();

        // Get the file names from the folder
        File folder = new File(folderPath);
        File[] files = folder.listFiles();

        // Iterate through the files and upload each one
        for (File file : files) {
            Uri fileUri = Uri.fromFile(file);
            String fileName = file.getName();

            // Check if the file has already been uploaded
            if (uploadedFileNames.contains(fileName)) {
                continue; // Skip already uploaded files
            }

            String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
//            String device_id =   getDeviceId();
            // Check if Bluetooth is supported on the device
            String device_id =  Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);;
            Log.d(TAG,"device_id:  "+device_id);
            // Create a reference to the file in Firebase Storage.
            StorageReference fileRef = storageRef.child(device_id+"/"+currentDate + "/"+ fileName);

            // Upload file to Firebase Storage
            fileRef.putFile(fileUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // File uploaded successfully
                            // Handle the success event
                        }
                    })
                    .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            if (task.isSuccessful()) {
                                // File upload completed
                                // Handle the completion event
                            } else {
                                // File upload failed
                                // Handle the failure event
                            }
                        }
                    });
        }
    }
//    @SuppressLint("HardwareIds")



/*
        private void uploadCsvFile(String filePath ) {
        try {
            // Get the file path from the file Uri
//            String filePath = fileUri.getPath();

            // Create an InputStream from the CSV file
            InputStream stream = new FileInputStream(filePath);

            // Create a reference to the file in Firebase Storage
//            StorageReference storageRef = null;
            StorageReference fileRef = firebaseStorage.child(filePath);

            // Upload the file to Firebase Storage
            UploadTask uploadTask = fileRef.putStream(stream);

            // Listen for upload success and failure events
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // Handle successful upload
                    Log.d(TAG, "File uploaded successfully!");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // Handle unsuccessful upload
                    Log.e(TAG, "Failed to upload file: " + e.getMessage());
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    // Track upload progress if needed
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    Log.d(TAG, "Upload progress: " + progress + "%");
                }
            });
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "File not found: " + e.getMessage());
        }
    }
*/
}
