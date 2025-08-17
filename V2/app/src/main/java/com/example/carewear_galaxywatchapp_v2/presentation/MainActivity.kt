package com.example.carewear_galaxywatchapp_v2.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.carewear_galaxywatchapp_v2.R
import android.graphics.Color

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_PERMISSIONS = 1001
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.BODY_SENSORS,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_main)

        // Request permissions at startup
        if (!allPermissionsGranted()) {

            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        findViewById<Button>(R.id.startButton).setOnClickListener {
            Log.d("MainActivity","Start button pressed");
            if (allPermissionsGranted()) {
                val intent = Intent(this, SensorService::class.java)
                startForegroundService(intent)
            } else {
                Toast.makeText(this, "Please grant all permissions", Toast.LENGTH_SHORT).show()
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
            }
        }

        findViewById<Button>(R.id.stopButton).setOnClickListener {
            Log.d("MainActivity","Stop button pressed");
            val intent = Intent(this, SensorService::class.java)
            stopService(intent)
        }
    }
    private val bgColorReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val color = intent?.getStringExtra("color")
            val rootView = findViewById<View>(android.R.id.content)
            when (color) {
                "Yellow" -> rootView.setBackgroundColor(Color.YELLOW)
                "Green" -> rootView.setBackgroundColor(Color.GREEN)
            }
        }
    }
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()
        registerReceiver(
            bgColorReceiver,
            IntentFilter("com.example.carewear_galaxywatchapp_v2.BG_COLOR_UPDATE"),
            Context.RECEIVER_NOT_EXPORTED
        )
    }



    override fun onPause() {
        super.onPause()
        unregisterReceiver(bgColorReceiver)
    }
    // Check if all required permissions are granted
    private fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all { perm ->
            ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Handle the permissions request result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissions not granted. App cannot function.", Toast.LENGTH_LONG).show()
            }
        }
    }

}
