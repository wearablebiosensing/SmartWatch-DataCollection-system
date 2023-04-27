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
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class FileIO {
    String TAG = "FileIO";
    /*
     * Writes the CSV file with the current timestamp in the file name for accelerometer data.
     * Takes in the toggle button view, the data to be added, and filename Eg: acc,gry ...
     * */
    public void save_data( ArrayList<String> data, String filename){
        Log.d(TAG, "DAA FROM FILE IO - save_data() "+data);
        //System.out.println("BUTTON PRESSED : Sensors Button Pressed");
        try{
            File sdCard = Environment.getExternalStorageDirectory();
            String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

            File dir = new File(sdCard.getAbsolutePath() + "/Download/" + currentDate);
            System.out.println("DIRECTORY: ---" + dir.toString());
            if(!dir.exists()) { // if directory does not exist then create one.
                dir.mkdirs();
            }
            long time= System.currentTimeMillis();
            System.out.println("DATE AND TIME CURRENT: ---" + time);
            // Depending on the user selection enter the.
            File file = new File(dir, "/"+ filename +"_"+ time +".csv");
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream f = new FileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(f);
//            BufferedWriter writer = new BufferedWriter(osw);
            // Buffer is needed to create the UTF 8 formatting and
            String mHeader ="Timestamp," + "x," + "y," + "z";
            String mHeaderHr ="Timestamp," + "HR_BPM";

            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(file.toPath(), perms);
            if(filename.contains("hr")) {
                osw.append(mHeaderHr);
                osw.write("\n");
            }
            else{
                osw.append(mHeader);
                osw.write("\n");
            }

            try {
                for (int i = 0 ; i <data.size() ; i++){
                   // Log.d(TAG, "data.get(i): "+ data.get(i));
//                    if(filename.contains("hr"))
//                    {
//                        writer.append(">");
//                    Thread.sleep(10);
//                    }
                    osw.write(data.get(i));
                    osw.write("\n");

                    //writer.write(data.get(i));

                }
                //f.flush();
                osw.close();
               f.close();
                Log.i(TAG, "Data saved");
//                Toast.makeText(getBaseContext(), filename + " Data saved", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG,"IO Exception");
            }
        } catch (FileNotFoundException e) {
            Log.d(TAG,"FILE NOT FOUND Exception");

            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG,"IO Exception Exception");

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
