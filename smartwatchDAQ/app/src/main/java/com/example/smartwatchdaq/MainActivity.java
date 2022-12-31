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


public class MainActivity<one> extends Activity implements SensorEventListener {
    SensorManager mSensorManager;
    private Sensor mAccelerometer;
    // Variables to store data streams from Sesnosrs.
    ArrayList<String> AccelerometerData = new ArrayList<String>();
    ArrayList<String> GryData = new ArrayList<String>();
    ArrayList<String> HRData = new ArrayList<String>();

    // Wearable DAQ Text View.
    private TextView mTextView;
    // Adapter String list for the Spinner drop down menue. Used to select the sampling rate by the user.
    ArrayList<String> SampleRates = new ArrayList<String>();
    // sampling rate spinner
    Spinner spinnerSampleRate1;
    Spinner spinnerSampleRate2;
    Spinner spinnerSampleRate3;
    String JSONdata;
    // To get the selected value to use it in on_resume() methods.
    String selected_value_sr;
    ToggleButton toggleSwitch;
    boolean acc = false;
    ToggleButton toggleSwitch2;
    boolean gry = false;
    ToggleButton toggleSwitch3;
    boolean hr = false;


    private ActivityMainBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize prefered sampling rates.
        SampleRates.add(0, "Select SR");
        SampleRates.add("20000");
        SampleRates.add("30000");
        SampleRates.add("40000");
        SampleRates.add("50000");
        // Initialize the Sensor Manager
        mSensorManager = ((SensorManager)getSystemService(SENSOR_SERVICE));

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mTextView = binding.text;

        toggleSwitch = findViewById(R.id.toggleButton);
        toggleSwitch2 = findViewById(R.id.toggleButton2);
        toggleSwitch3 = findViewById(R.id.toggleButton3);

        spinnerSampleRate1 = findViewById(R.id.spinner);

        // Initializing the drop down menue.
        ArrayAdapter<String> adapter= new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_list_item_1,SampleRates);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSampleRate1.setAdapter(adapter);
        spinnerSampleRate1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String value =adapterView.getItemAtPosition(i).toString();
                selected_value_sr = value;
                Toast.makeText(MainActivity.this,value,Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        /*Toggle Switch On Change Event listenters*/
        toggleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    // Run the sensor Activity in the background.
                    System.out.println("Toggle Button is ON !");
                    System.out.println("DROP DOWN VALUE" + selected_value_sr);

                    acc = true;
                    //startActivity(new Intent(MainActivity.this, SensorActivity.class));
                    onResume();
                } else {
                    onPause();
                    System.out.println("Toggle Button is OFF !");
                    acc = false;
//                    save_data(buttonView,  AccelerometerData);
                    save_data(buttonView, AccelerometerData, "acc");
                    save_Jsondata(buttonView,  JSONdata, "acc_info");
                }
            }
        });
        toggleSwitch2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    // Run the sensor Activity in the background.
                    System.out.println("Toggle Button is ON !");
                    gry = true;
                    //startActivity(new Intent(MainActivity.this, SensorActivity.class));
                    onResume();
                } else {
                    onPause();
                    System.out.println("Toggle Button is OFF !");
                    gry = false;
                    save_data(buttonView,  GryData,"gry");
                }
            }
        });

        toggleSwitch3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    // Run the sensor Activity in the background.
                    System.out.println("Toggle Button is ON !");
                    hr = true;
                    //startActivity(new Intent(MainActivity.this, SensorActivity.class));
                    onResume();
                } else {
                    onPause();
                    System.out.println("Toggle Button is OFF !");
                    hr = false;
                    save_data(buttonView,  HRData,"hr");
                }
            }
        });


        // List all different types of sensors getSensorList(). https://developer.android.com/guide/topics/sensors/sensors_overview
        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        ArrayList<String> arrayList = new ArrayList<String>();
        ArrayList<String> sensorVendor = new ArrayList<String>();
        // Add general device information.
       /* for (Sensor sensor : sensors) {
            sensorVendor.add(sensor.getVendor());
            arrayList.add(sensor.getName());
        }
        for (int i = 0 ; i <arrayList.size() ; i++){
            System.out.println(arrayList.get(i) + ",vendor," +sensorVendor.get(i));
        } */

        // Android sensors and file write permissions.
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
        // if recording acc data register listener for acc values.
        if (acc ==true){
            Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            // To change the sampling rate apply it in microseconds.
            mSensorManager.registerListener((SensorEventListener) this, accelerometer,
                    20000); // 50 Hz // 20000 = 50Hz in microseconds
        }
        // if recording acc data register listener for gry values.
        if (gry == true){
            Sensor gyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            // To change the sampling rate apply it in microseconds.
            mSensorManager.registerListener((SensorEventListener) this, gyroscope,
                    20000); // 50 Hz // 20000 microseconds = 50Hz in
        }
        // if recording acc data register listener for gry values.
        if (hr == true){
            Sensor heart_rate = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
            // To change the sampling rate apply it in microseconds.
            mSensorManager.registerListener((SensorEventListener) this, heart_rate,
                    20000); // 1 Hz // 1000000 = 1Hz
        }
       // HRData
    }
    // unregisted all sensors when not used.
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener((SensorEventListener) this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // SensorEvent: This class represents a Sensor event and holds information such as the sensor's type, the time-stamp, accuracy and of course the sensor's data.
    public void onSensorChanged(SensorEvent event) {
        System.out.println("onSensorChanged() acc: "  + acc);
        System.out.println("onSensorChanged() gry: "  + gry);
        // If toggle button is on and off start and stop data collection.
        if (acc != false){
            onResume();
            JSONdata =  event.accuracy + "," + event.sensor;

            String data_accelerometer =  event.timestamp + "," + String.valueOf(event.values[0]) + "," + String.valueOf(event.values[1] + "," + String.valueOf(event.values[2]));
            AccelerometerData.add(data_accelerometer);
            System.out.println("data_accelerometer: " + data_accelerometer);
        } if(gry != false){
            onResume();
            String data_gryo =  event.timestamp + "," + String.valueOf(event.values[0]) + "," + String.valueOf(event.values[1]) + "," + String.valueOf(event.values[2]);
            GryData.add(data_gryo);
            System.out.println("data_Gryo--: " + GryData);
        }if(hr != false){
            onResume();
            String data_hr =  event.timestamp + "," + "0" + ",0," + String.valueOf(event.values[0]);
            HRData.add(data_hr);
            System.out.println("data Heart Rate--: " + HRData);
        }

        else{
           System.out.println("Stop Data Collection!");
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
    public void save_Jsondata(View view, String jsonData, String filename){
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
            File file = new File(dir, "/"+ filename +"_"+ time +".txt");
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream f = new FileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(f, StandardCharsets.UTF_8);
            // Buffer is needed to create the UTF 8 formatting and
            BufferedWriter writer = new BufferedWriter(osw);
            System.out.println("JSON DATA: " + jsonData);
            //total 13 String data_accelerometer = event.accuracy + "," + event.sensor + "," + event.timestamp +","+ String.valueOf(event.values[0]) + ","+String.valueOf(event.values[1] + "," + String.valueOf(event.values[2]));
            try {
                writer.append(jsonData);
                writer.newLine();
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