package com.example.sensorsfilesave;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class MainActivity extends Activity implements ItemClickListener  {
    private static final int PERMISSION_REQUEST_CODE = 123;
    private ItemSelectionListener itemSelectionListener;
    private List<String> data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        itemSelectionListener = new TargetClass();

        WearableRecyclerView wearableRecyclerView = findViewById(R.id.wearable_recycler_view);
        // Set a layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        wearableRecyclerView.setLayoutManager(layoutManager);

        // Sample data
         data = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            data.add("Item " + i);
        }

        CustomAdapter adapter = new CustomAdapter(data, this);
        wearableRecyclerView.setAdapter(adapter);

        // Optional: Center the items on the screen
        wearableRecyclerView.setCircularScrollingGestureEnabled(true);
        wearableRecyclerView.setBezelFraction(0.5f);
        wearableRecyclerView.setScrollDegreesPerScreen(90);



        checkAndRequestPermissions();
        openAppSettings();

        // Start the sensor data collection service
        startService(SensorService.getStartIntent(this));

    }
    @Override
    public void onItemClick(int position) {
        String selectedItem = data.get(position);
        Toast.makeText(this, "Selected: " + selectedItem, Toast.LENGTH_SHORT).show();

        // Start SensorService and pass the selected item string
        Intent serviceIntent = new Intent(this, SensorService.class);
        serviceIntent.putExtra("SELECTED_ITEM", selectedItem);
        startService(serviceIntent);

        // Optionally start DetailActivity and pass the selected item string
//        Intent detailIntent = new Intent(this, DetailActivity.class);
//        detailIntent.putExtra("SELECTED_ITEM", selectedItem);
//        startActivity(detailIntent);
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }
    private void checkAndRequestPermissions() {
        // Check if the required permissions are granted
        if (
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, android.Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED||
                        ContextCompat.checkSelfPermission(this, android.Manifest.permission.HIGH_SAMPLING_RATE_SENSORS) != PackageManager.PERMISSION_GRANTED||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED||

                        ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED

        ) {

            // Request the necessary permissions using the service's context
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WAKE_LOCK, android.Manifest.permission.FOREGROUND_SERVICE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    // ...

    // Override onRequestPermissionsResult to handle the result of the permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Check if all requested permissions are granted
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                // Permissions are granted, continue with your logic
            } else {
                // Permissions are not granted, handle accordingly (e.g., show a message or exit the service)
            }
        }
    }

}
