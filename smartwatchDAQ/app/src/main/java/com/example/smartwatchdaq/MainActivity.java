package com.example.smartwatchdaq;
import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import com.example.smartwatchdaq.databinding.ActivityMainBinding;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;

import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import android.widget.Toast;
import android.widget.ToggleButton;


import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

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


/*
* Useful Resources:
* https://developer.android.com/training/monitoring-device-state/battery-monitoring
* https://hivemq.github.io/hivemq-mqtt-client/docs/client-configuration/
* https://github.com/hivemq/hivemq-mqtt-client
*   Foreground Services:
* https://www.youtube.com/watch?v=G9M_HEdclTg
* https://www.youtube.com/watch?v=BbXuumYactY
* Android Sensors API:
* https://developer.android.com/guide/topics/sensors/sensors_overview
* */

public class MainActivity<one> extends Activity implements SensorEventListener {
    private ArrayList permissionsToRequest;
    private ArrayList permissionsRejected = new ArrayList();
    private ArrayList permissions = new ArrayList();

    private final static int ALL_PERMISSIONS_RESULT = 101;
    LocationTrack locationTrack;

    SensorManager mSensorManager;
    MqttHelper mqttHelper;

    private Sensor mAccelerometer;
    // Variables to store data streams from Sensosrs.
    ArrayList<String> AccelerometerData = new ArrayList<String>();
    ArrayList<Float> AccelerometerDataFloat = new ArrayList<Float>();

    ArrayList<String> GryData = new ArrayList<String>();
    ArrayList<String> HRData = new ArrayList<String>();
    // Create an MQTT Client.
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
    String selected_value_sr_acc; // for Acc
    String selected_value_sr_gry; // for gry
    String selected_value_sr_hr; // for HR

    ToggleButton toggleSwitch;
    boolean acc = false;
    ToggleButton toggleSwitch2;
    boolean gry = false;
    ToggleButton toggleSwitch3;
    boolean hr = false;


    private ActivityMainBinding binding;
//    Intent serviceIntent = new Intent(this, MyService.class);
//    startForegroundService(serviceIntent);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // Initialize prefered sampling rates.
        /* https://www.ehow.co.uk/how_7566601_calculate-original-price-after-discount.html
        * 10-100000,20-50000,30-33333.33,40-25000,50-200000,
        * 60-16666.666,70-14285.71,80-12500,90-11111.111,100 - 10000
        * 110-9090.909,120-8333.333,130-7692.307,140-7142.857,150-6666.6666,160 - 6250,
        * 170- 5882.352,180-5555.55555,190-5263.15789,200 - 5000
        * */
        // Conversion base : 1 µs(p) = 10,00,000 Hz.
        // 100000 us = ? Hz .1000000/100000.
        SampleRates.add(0, "Select SR..");
        // A period of 1 Millisecond is equal to 1000 Hertz frequency.
        // 1Hz = 1/1 Millisecond.
       // 1 mili = 1000 micoro second
        // 10Hz = 1/10Hz * 1000 milisecond.
        // ==> 10Hz = 1/10Hz * 1000000 micro seconds.
        // =======>>>> ==> #Hz = 1/#Hz * 1000000 micro seconds.
        SampleRates.add("10"); // 1,00,000us - 10Hz ( 10,00,000/1,00,000)- COVERT TO HZs.
        SampleRates.add("20");  // 50000 us - 20Hz (10,00,000/50000)
        SampleRates.add("40");  // 25000 us 40Hz (10,00,000/25000)
        SampleRates.add("50"); // 200000 us - 50Hz (10,00,000/200000)
        SampleRates.add("64"); // 1562 us - 64Hz (10,00,000/ 1562)
        SampleRates.add("100");  // 10000 us - 100Hz (10,00,000/10000)
        SampleRates.add("128");//  7812 - 128Hz (10,00,000/7812)
        SampleRates.add("160");   // 6250 - 160Hz (10,00,000/"6250")
        SampleRates.add("200");  // 5000 - 200Hz (10,00,000/5000)

        // Initialize the Sensor Manager
        mSensorManager = ((SensorManager)getSystemService(SENSOR_SERVICE));

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mTextView = binding.text;

        toggleSwitch = findViewById(R.id.toggleButton);
        toggleSwitch2 = findViewById(R.id.toggleButton2);
        toggleSwitch3 = findViewById(R.id.toggleButton3);

        spinnerSampleRate1 = findViewById(R.id.spinner);
        spinnerSampleRate2 = findViewById(R.id.spinner2);
        spinnerSampleRate3 = findViewById(R.id.spinner3);

        // Initializing the drop down menue.
        ArrayAdapter<String> adapter= new ArrayAdapter<String>(MainActivity.this,R.layout.selected_dropdown_item,SampleRates);
        adapter.setDropDownViewResource(R.layout.dropdown_item);
        spinnerSampleRate1.setAdapter(adapter);
        spinnerSampleRate2.setAdapter(adapter);
        spinnerSampleRate3.setAdapter(adapter);

        spinnerSampleRate1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selected_value_sr_acc  = adapterView.getItemAtPosition(i).toString();
//                selected_value_sr = value;
                //Toast.makeText(MainActivity.this,"SR selected:" + selected_value_sr_acc,Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        spinnerSampleRate2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selected_value_sr_gry  = adapterView.getItemAtPosition(i).toString();
//                selected_value_sr = value;
                Toast.makeText(MainActivity.this,"SR selected:" + selected_value_sr_gry,Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        spinnerSampleRate3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selected_value_sr_hr  = adapterView.getItemAtPosition(i).toString();
//                selected_value_sr = value;
                Toast.makeText(MainActivity.this,"SR selected:" + selected_value_sr_hr,Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        /*Toggle Switch On Change Event listeners*/
        toggleSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // start the service in background even if the user quits the app.
                if(toggleSwitch.isChecked()){
//                    startServiceButtonPressed(view);
                    // The toggle is enabled
                    // Run the sensor Activity in the background.
                    System.out.println("Toggle Button is ON !");
                    System.out.println("DROP DOWN VALUE---" + selected_value_sr_acc);
                    acc = true;
                    //startActivity(new Intent(MainActivity.this, SensorActivity.class));
                    onResume();
                } else if(!toggleSwitch.isChecked()){
                    onPause();
                    System.out.println("Toggle Button is OFF !");
                    acc = false;
//                    save_data(buttonView,  AccelerometerData);
                    save_data(view, AccelerometerData, selected_value_sr_acc +"_acc");
//                    save_Jsondata(view,  JSONdata, "acc_info");
                }

            }
        });
        toggleSwitch2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (toggleSwitch2.isChecked()) {
//                    startServiceButtonPressed(view);
                    // The toggle is enabled
                    // Run the sensor Activity in the background.
                    System.out.println("Toggle Button is ON !");
                    gry = true;
                    System.out.println("DROP DOWN VALUE gry---" + selected_value_sr_gry);

                    //startActivity(new Intent(MainActivity.this, SensorActivity.class));
                    onResume();
                } else {
//                    onPause();
                    System.out.println("Toggle Button is OFF !");
                    gry = false;
                    save_data(view,  GryData,selected_value_sr_gry+"_gry");
                }
            }
        });

        toggleSwitch3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(toggleSwitch3.isChecked()){
                    // The toggle is enabled
                    // Run the sensor Activity in the background.
                    System.out.println("Toggle Button is ON !");
                    System.out.println("DROP DOWN VALUE hr---" + selected_value_sr_hr);
                    hr = true;
                    //startActivity(new Intent(MainActivity.this, SensorActivity.class));
                    onResume();
                } else {
//                    startServiceButtonPressed(view);
//                    onPause();
                    System.out.println("Toggle Button is OFF !");
                    hr = false;
                    save_data(view,  HRData,selected_value_sr_hr + "_hr");
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

        /*Location service init*/
        permissions.add(ACCESS_FINE_LOCATION);
        permissions.add(ACCESS_COARSE_LOCATION);

        permissionsToRequest = findUnAskedPermissions(permissions);
        //get the permissions we have asked for before but are not granted..
        //we will store this in a global list to access later.


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {


            if (permissionsToRequest.size() > 0)
                requestPermissions((String[]) permissionsToRequest.toArray(new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
        }


        Button btn = (Button) findViewById(R.id.btn);


        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                locationTrack = new LocationTrack(MainActivity.this);


                if (locationTrack.canGetLocation()) {


                    double longitude = locationTrack.getLongitude();
                    double latitude = locationTrack.getLatitude();

                    Toast.makeText(getApplicationContext(), "Longitude:" + Double.toString(longitude) + "\nLatitude:" + Double.toString(latitude), Toast.LENGTH_SHORT).show();
                } else {

                    locationTrack.showSettingsAlert();
                }

            }
        });
    }
    /*
    private void startMqtt(){
        mqttHelper = new MqttHelper(getApplicationContext());
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {

            }

            @Override
            public void connectionLost(Throwable throwable) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                Log.w("Debug",mqttMessage.toString());
                dataReceived.setText(mqttMessage.toString());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }

     */
    protected void onResume() {
        super.onResume();
        // if recording acc data register listener for acc values.
        if (acc ==true){
            System.out.println("Int Vale for SR --"+Integer.parseInt(selected_value_sr_acc));
            Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            // To change the sampling rate apply it in microseconds.
            // 1/#Hz * 1000000 micro seconds.
            mSensorManager.registerListener((SensorEventListener) this, accelerometer,
                    (1/Integer.parseInt(selected_value_sr_acc))*1000000); // 50 Hz // 20000 = 50Hz in microseconds
        }
        // if recording acc data register listener for gry values.
        if (gry == true){
            Sensor gyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            // To change the sampling rate apply it in microseconds.
            mSensorManager.registerListener((SensorEventListener) this, gyroscope,
                    (1/Integer.parseInt(selected_value_sr_gry))*1000000); // 50 Hz // 20000 microseconds = 50Hz in
        }
        // if recording acc data register listener for gry values.
        if (hr == true){
            Sensor heart_rate = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
            // To change the sampling rate apply it in microseconds.
            mSensorManager.registerListener((SensorEventListener) this, heart_rate,
                    (1/Integer.parseInt(selected_value_sr_hr))*1000000); // 1 Hz // 1000000 = 1Hz
        }
       // HRData
    }
//    // unregisted all sensors when not used.
//    protected void onPause() {
//        super.onPause();
//        mSensorManager.unregisterListener((SensorEventListener) this);
//    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // SensorEvent: This class represents a Sensor event and holds information such as the sensor's type, the time-stamp, accuracy and of course the sensor's data.
    public void onSensorChanged(SensorEvent event) {
//        System.out.println("onSensorChanged() acc: "  + acc);
//        System.out.println("onSensorChanged() gry: "  + gry);
        // If toggle button is on and off start and stop data collection.
        if (acc != false){
            onResume();
            JSONdata =  event.accuracy + "," + event.sensor;

            String data_accelerometer =  event.timestamp + "," + String.valueOf(event.values[0]) + "," + String.valueOf(event.values[1] + "," + String.valueOf(event.values[2]));
            float sum = event.values[0] +event.values[1]+event.values[2];
            AccelerometerDataFloat.add(sum);
            double sum_full_array = 0;
            AccelerometerData.add(data_accelerometer);
            // To get the RMS based on only the number of samples.
            if (AccelerometerDataFloat.size() == 20){
                for(int i = 0; i < AccelerometerDataFloat.size(); i++) {
                    sum_full_array += AccelerometerDataFloat.get(i);
                }
                double rms = Math.sqrt(sum_full_array*sum_full_array);
                System.out.println("RMS after 20 Samples =: " + rms);
            }
            System.out.println("data_accelerometer: " + data_accelerometer);
//            System.out.println("onSensorChanged() rms: " + rms);

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

//        else{
//           System.out.println("Stop Data Collection!");
//            onResume();
//        }
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

    private ArrayList findUnAskedPermissions(ArrayList wanted) {
        ArrayList result = new ArrayList();

        for (Object perm : wanted) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }

        return result;
    }

    private boolean hasPermission(Object permission) {
        if (canMakeSmores()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return (checkSelfPermission((String) permission) == PackageManager.PERMISSION_GRANTED);
            }
        }
        return true;
    }

    private boolean canMakeSmores() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }


    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {

            case ALL_PERMISSIONS_RESULT:
                for (Object perms : permissionsToRequest) {
                    if (!hasPermission(perms)) {
                        permissionsRejected.add(perms);
                    }
                }

                if (permissionsRejected.size() > 0) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale((String) permissionsRejected.get(0))) {
                            showMessageOKCancel("These permissions are mandatory for the application. Please allow access.",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermissions((String[]) permissionsRejected.toArray(new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                                            }
                                        }
                                    });
                            return;
                        }
                    }

                }

                break;
        }

    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationTrack.stopListener();
    }
}