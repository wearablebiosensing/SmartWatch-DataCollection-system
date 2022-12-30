package com.example.smartwatchdaq;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.widget.Toast;

import com.example.smartwatchdaq.databinding.ActivityMainBinding;
import com.google.type.Date;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import java.io.PrintWriter;


public class MainActivity extends Activity implements SensorEventListener{

    private TextView mTextView;
    private ActivityMainBinding binding;
    SensorManager mSensorManager;
    private Sensor mAccelerometer;
    ArrayList<String> AccelerometerData = new ArrayList<String>();
    int counter = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mSensorManager = ((SensorManager)getSystemService(SENSOR_SERVICE));
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        super.onCreate(savedInstanceState);
        if (checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions( new String[]{Manifest.permission.BODY_SENSORS}, 1);
        } else {
            Log.d( "ADD tag","ALREADY GRANTED");
        }
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions( new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            Log.d( "ADD tag","ALREADY GRANTED");
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mTextView = binding.text;
        // List all different types of sensors getSensorList(). https://developer.android.com/guide/topics/sensors/sensors_overview
        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
//        List<Sensor> sensorsVendor = mSensorManager.get//(Sensor.TYPE_ALL);

        ArrayList<String> arrayList = new ArrayList<String>();
        ArrayList<String> sensorVendor = new ArrayList<String>();

        for (Sensor sensor : sensors) {
            sensorVendor.add(sensor.getVendor());
            arrayList.add(sensor.getName());
        }
        //arrayList.forEach((n) -> System.out.println(n));
        for (int i = 0 ; i <arrayList.size() ; i++){
            System.out.println(arrayList.get(i) + ",vendor," +sensorVendor.get(i));
        }


        System.out.println("Sensor Vendor");

//        sensorVendor.forEach((n) -> System.out.println(n));
    }
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener((SensorEventListener) this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            Log.d("Sensor Sampling Rate--", String.valueOf(SensorManager.SENSOR_DELAY_NORMAL));


        }
        mSensorManager.registerListener((SensorEventListener) this, accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL);
        //Log.d("Accelerometer data ------",accelerometer.toString());
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener((SensorEventListener) this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Log.d("Accelerometer Sensor data ------",sensor.toString());
        //Log.d("Accelerometer Sensor data accuracy ------", String.valueOf(accuracy));

    }
    // SensorEvent: This class represents a Sensor event and holds information such as the sensor's type, the time-stamp, accuracy and of course the sensor's data.
    public void onSensorChanged(SensorEvent event) {
        try {
            String data_accelerometer = event.accuracy + "," + event.sensor + "," + event.timestamp +","+ String.valueOf(event.values[0]) + ","+String.valueOf(event.values[1] + "," + String.valueOf(event.values[2]));
            System.out.println("data_accelerometer: " + data_accelerometer);
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File(sdCard.getAbsolutePath() + "/Download");
            System.out.println("DIRECTORY: ---" + dir.toString());
            if(!dir.exists()) {
                dir.mkdirs();
            }
            //System.out.println(dirsMade);
//            Log.v("Accel", dirsMade.toString());

            File file = new File(dir, "/"+ "output_acc_example1.csv");
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream f = new FileOutputStream(file);


            try {
                f.write(data_accelerometer.getBytes());
                f.flush();
                f.close();
                Toast.makeText(getBaseContext(), "Data saved", Toast.LENGTH_LONG).show();
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