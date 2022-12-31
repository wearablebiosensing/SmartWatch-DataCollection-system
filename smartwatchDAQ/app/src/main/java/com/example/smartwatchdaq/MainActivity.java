package com.example.smartwatchdaq;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.widget.SwitchCompat;

import com.example.smartwatchdaq.databinding.ActivityMainBinding;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class MainActivity extends Activity implements SensorEventListener {
    SensorManager mSensorManager;
    private Sensor mAccelerometer;
    ArrayList<String> AccelerometerData = new ArrayList<String>();
    private TextView mTextView;
    private ActivityMainBinding binding;

    int counter = 0;
    boolean IsDataRequested = false;
    Spinner spinnerSampleRate;
    String[] sampling_rates = {"10000","20000","30000","40000","50000"};
    String selected_value;
    ToggleButton toggleSwitch;
    boolean isDataRequested =false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        toggleSwitch = findViewById(R.id.toggleButton);

        spinnerSampleRate = findViewById(R.id.spinner);
        // Initializing the drop down menue adapter.
        ArrayAdapter<String> adapter= new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_spinner_item,sampling_rates);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSampleRate.setAdapter(adapter);
        spinnerSampleRate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String value =adapterView.getItemAtPosition(i).toString();
                selected_value = value;
                Toast.makeText(MainActivity.this,value,Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        System.out.println("DROP DOWN VALUE" + selected_value);
        mTextView = binding.text;
        toggleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    // Run the sensor Activity in the background.
                    System.out.println("Toggle Button is ON !");
                    isDataRequested = true;
                    //startActivity(new Intent(MainActivity.this, SensorActivity.class));
                    onResume();
                } else {
                    onPause();
                    System.out.println("Toggle Button is OFF !");
                    save_data(buttonView,  AccelerometerData);
                }
            }
        });

        mSensorManager = ((SensorManager)getSystemService(SENSOR_SERVICE));
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // List all different types of sensors getSensorList(). https://developer.android.com/guide/topics/sensors/sensors_overview
        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        ArrayList<String> arrayList = new ArrayList<String>();
        ArrayList<String> sensorVendor = new ArrayList<String>();

        for (Sensor sensor : sensors) {
            sensorVendor.add(sensor.getVendor());
            arrayList.add(sensor.getName());
        }
        for (int i = 0 ; i <arrayList.size() ; i++){
            System.out.println(arrayList.get(i) + ",vendor," +sensorVendor.get(i));
        }
        System.out.println("Sensor Vendor");

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


    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener((SensorEventListener) this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (accelerometer != null) {

            Log.d("Sensor Sampling Rate--", String.valueOf(SensorManager.SENSOR_DELAY_NORMAL));

        }

        // To change the sampling rate apply it in microseconds.
        mSensorManager.registerListener((SensorEventListener) this, accelerometer,
                20000); // 50 Hz // 20000 = 50Hz in microseconds
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener((SensorEventListener) this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // SensorEvent: This class represents a Sensor event and holds information such as the sensor's type, the time-stamp, accuracy and of course the sensor's data.
    public void onSensorChanged(SensorEvent event) {
        System.out.println("onSensorChanged() isDataRequested: "  +isDataRequested);
        // If toggle button is on and off start and stop data collection.
        if (isDataRequested != false){
            onResume();
            String data_accelerometer = event.accuracy + "," + event.sensor + "," + event.timestamp + "," + String.valueOf(event.values[0]) + "," + String.valueOf(event.values[1] + "," + String.valueOf(event.values[2]));
            AccelerometerData.add(data_accelerometer);
            System.out.println("data_accelerometer: " + data_accelerometer);
        } else{
           System.out.println("Stop Data Collection");
//            mSensorManager.unregisterListener((SensorEventListener) this);

        }

    }
    /*
     * Writes the CSV file with the current timestamp in the file name for accelerometer data.
     * */
    public void save_data(View view, ArrayList<String> AccelerometerData){
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

            File file = new File(dir, "/"+ "acc_"+ time +".csv");
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream f = new FileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(f, StandardCharsets.UTF_8);
            // Buffer is needed to create the UTF 8 formatting and
            BufferedWriter writer = new BufferedWriter(osw);
            //total 13 String data_accelerometer = event.accuracy + "," + event.sensor + "," + event.timestamp +","+ String.valueOf(event.values[0]) + ","+String.valueOf(event.values[1] + "," + String.valueOf(event.values[2]));
            String mHeader ="EventAccuracy,"+"EventSensorName,"+"EventVendorName,"+"Version,"+"Type,"+"MaxRange,"+"Resolution,"+"Power,"+"minDelay,"+"Timestamp,"+"Ax,"+"Ay,"+"Az" ;
            writer.append(mHeader);
            try {
                for (int i = 0 ; i <AccelerometerData.size() ; i++){
                    writer.append(AccelerometerData.get(i));
                    writer.newLine();
                }
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