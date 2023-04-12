package com.example.carewear;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

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

public class FileIO {
    String TAG = "FileIO";
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
                Log.i(TAG, "Data saved");
//                Toast.makeText(getBaseContext(), filename + " Data saved", Toast.LENGTH_LONG).show();
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
//                Toast.makeText(getBaseContext(), "Data saved", Toast.LENGTH_LONG).show();
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
