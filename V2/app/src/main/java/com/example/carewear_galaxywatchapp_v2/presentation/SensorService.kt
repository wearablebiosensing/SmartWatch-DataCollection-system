package com.example.carewear_galaxywatchapp_v2.presentation

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.security.Provider
import java.text.SimpleDateFormat
import java.util.Locale
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.SensorEvent
import android.os.BatteryManager
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import com.example.carewear_galaxywatchapp_v2.R
import java.util.Date
import java.util.Timer
import java.util.TimerTask

class SensorService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var heartRate: Sensor? = null
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    private val hrData = mutableListOf<String>()
    private val accData = mutableListOf<String>()
    private val gyroData = mutableListOf<String>()
    private val hrWindow = ArrayDeque<Float>() // Rolling window for last 5 HR values
    private val HR_WINDOW_SIZE = 5
    private var wakeLock: PowerManager.WakeLock? = null
    private val batteryData = mutableListOf<String>()
    private var batteryTimer: Timer? = null

    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "CareWear::SensorWakeLock"
        )
        wakeLock?.acquire()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        heartRate = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        heartRate?.let { sensorManager.registerListener(this, it, 1_000_000) } // 1Hz
        accelerometer?.let { sensorManager.registerListener(this, it, 33_333) } // 30Hz
        gyroscope?.let { sensorManager.registerListener(this, it, 33_333) } // 30Hz

        startForeground(1, createNotification())
        startBatteryLogging()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        super.onDestroy()
        wakeLock?.release()

        sensorManager.unregisterListener(this)
        batteryTimer?.cancel()

        saveDataToCSV()
    }
    @SuppressLint("ServiceCast")
    private fun startBatteryLogging() {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        batteryTimer = Timer()
        batteryTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val batteryPct: Int = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                val timestamp = timestampFormat.format(Date())
                val record = "$batteryPct,$timestamp"
                Log.d("BatteryLogger", record)
                batteryData.add(record)
            }
        }, 0, 1000) // Every 1 second
    }

    private fun calculateStd(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val mean = values.sum() / values.size
        val variance = values.map { (it - mean) * (it - mean) }.sum() / values.size
        return kotlin.math.sqrt(variance)
    }
    private fun sendBackgroundColorUpdate(color: String) {
        val intent = Intent("com.example.carewear_galaxywatchapp_v2.BG_COLOR_UPDATE")
        intent.putExtra("color", color)
        sendBroadcast(intent)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val timestamp = timestampFormat.format(Date())
        when (event.sensor.type) {
            Sensor.TYPE_HEART_RATE -> {
                Log.d("SENSOR SERVICE","HR values = ${event.values[0]},${timestamp}");
                hrData.add("${event.values[0]},$timestamp,None")
                val hrValue = event.values[0]

                // Update rolling window
                hrWindow.addLast(hrValue)
                if (hrWindow.size > HR_WINDOW_SIZE) {
                    hrWindow.removeFirst()
                }

                // Only check if we have enough samples
                if (hrWindow.size == HR_WINDOW_SIZE) {
                    val std = calculateStd(hrWindow.toList())
                    Log.d("SENSOR SERVICE", "SENSOR SERVICE / HR Std: $std")
                    // Send broadcast or notification to update UI
                    if (std == 0f) {
                        Toast.makeText(this, "HR is still 0!", Toast.LENGTH_SHORT).show()

                        Log.d("SENSOR SERVICE","SENSOR SERVICE/ set background color to yellow")
                        sendBackgroundColorUpdate("Yellow")
                    } else {
                        Toast.makeText(this, "HR is clear", Toast.LENGTH_SHORT).show()
                        sendBackgroundColorUpdate("Green")
                    }
                }

            }
            Sensor.TYPE_ACCELEROMETER -> {
                Log.d("SENSOR SERVICE","${event.values[0]},${event.values[1]},${event.values[2]},$timestamp");

                accData.add("${event.values[0]},${event.values[1]},${event.values[2]},$timestamp")
            }
            Sensor.TYPE_GYROSCOPE -> {
                Log.d("SENSOR SERVICE","${event.values[0]},${event.values[1]},${event.values[2]},$timestamp");

                gyroData.add("${event.values[0]},${event.values[1]},${event.values[2]},$timestamp")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun saveDataToCSV() {
        val folder = SimpleDateFormat("MM_dd_yyyy", Locale.US).format(Date())

        val now = SimpleDateFormat("MM_dd_yyyy_HH_mm_ss", Locale.US).format(Date())
//        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)'
        // File path ============== /storage/emulated/0/Android/data/com.example.carewear_galaxywatchapp_v2/files
        val downloads = getExternalFilesDir("$folder") // or getExternalFilesDir("your_subfolder")
        Log.d("SensorService","SensorService/ File path ============== $downloads");
        writeCSV(File(downloads, "heart_rate_$now.csv"), "HeartRate,Timestamp,activity", hrData)
        Log.d("SensorService","SensorService /File Saved ${"HR_$now.csv"}");
        Toast.makeText(this, " HR File is saved!", Toast.LENGTH_SHORT).show()

        writeCSV(File(downloads, "acc_$now.csv"), "x,y,z,Timestamp,activity", accData)
        Log.d("SensorService","SensorService/ File Saved ${"acc_$now.csv"}");
        Toast.makeText(this, " Acc File is saved!", Toast.LENGTH_SHORT).show()

        writeCSV(File(downloads, "gry_$now.csv"), "x,y,z,Timestamp,activity", gyroData)
        Log.d("SensorService","SensorService /File Saved ${"gyr_$now.csv"}");
        Toast.makeText(this, " Gry File is saved!", Toast.LENGTH_SHORT).show()
        writeCSV(File(downloads, "battery_level_$now.csv"), "BatteryLevelPercentage,Timestamp", batteryData)
        Toast.makeText(this, " Battery File is saved!", Toast.LENGTH_SHORT).show()
    }

    private fun writeCSV(file: File, header: String, data: List<String>) {
        try {
            FileWriter(file).use { writer ->
                writer.write("$header\n")
                data.forEach { writer.write("$it\n") }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun createNotification(): Notification {
        val channelId = "sensor_channel"
        val channel = NotificationChannel(channelId, "Sensor Service", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
        return Notification.Builder(this, channelId)
            .setContentTitle("Sensor Collection Running")
            .setSmallIcon(R.drawable.splash_icon)
            .build()
    }
}
