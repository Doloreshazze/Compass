package com.playeverywhere999.compassapp

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.playeverywhere999.compassapp.R.id.button_back
import pl.droidsonroids.gif.GifImageView



class CalibrationActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var magnetometer: Sensor
    private lateinit var _acuuracy: TextView
    private lateinit var adView: AdView
    private lateinit var gifImageView: AdView
    private lateinit var manual: TextView
    private lateinit var buttonBack: Button

    private fun getContrastingColor(color: Int): Int {
        val yiq = ((Color.red(color) * 299) + (Color.green(color) * 587) + (Color.blue(color) * 114)) / 1000
        return if (yiq >= 128) Color.BLACK else Color.WHITE
    }

    fun checkSensors(context: Context): Boolean {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val hasAccelerometer = accelerometer != null
        val hasMagneticField = magneticField != null

        return hasAccelerometer && hasMagneticField
    }

    private fun initAdMob() {
        MobileAds.initialize(this)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calibration)
        _acuuracy = findViewById(R.id._acuuracy)
        manual = findViewById(R.id.manual)
        buttonBack = findViewById(button_back)
        buttonBack.text = getString(R.string.bt_back)

        adView = findViewById(R.id.adView)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)!!
        initAdMob() // advertising раскомментировать для рекламы

        val gifImageView: GifImageView = findViewById(R.id.gifImageView)
        gifImageView.setImageResource(R.drawable.calibration_gif)

        val backgroundColor = Color.parseColor("#FFFFFF") // Цвет фона
        val textColor = getContrastingColor(backgroundColor)

        _acuuracy.setBackgroundColor(backgroundColor)
        _acuuracy.setTextColor(textColor)



    }

    override fun onResume() {
        super.onResume()

        manual.text = getString(R.string.message_manual)

        adView.resume()

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)

        val result = checkSensors(this)
        if(!result)  _acuuracy.text = getString(R.string.message_sensors)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this, accelerometer)
        sensorManager.unregisterListener(this, magnetometer)
        adView.pause()
    }



    fun onClickGoBack(view: View) {

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)

    }

    private var gravity = FloatArray(3)
    private var geomagnetic = FloatArray(3)


    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values
        } else if (event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = event.values
        }

        if (gravity != null && geomagnetic != null) {
            val rotationMatrix = FloatArray(9)
            val inclinationMatrix = FloatArray(9)

            if (SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, gravity, geomagnetic)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)

                var azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                azimuth = if (azimuth < 0) (azimuth + 360) else azimuth







            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d("BensorB", "Accuracy changed: $accuracy")
        when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> {
                // Высокая точность
                Log.d("SensorAccuracy", "Высокая точность")
                _acuuracy.text = getString(R.string.message_precision1)
            }
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> {
                // Средняя точность
                Log.d("SensorAccuracy", "Средняя точность")
                _acuuracy.text = getString(R.string.message_precision2)
            }
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> {
                // Низкая точность
                Log.d("SensorAccuracy", "Низкая точность")
                _acuuracy.text = getString(R.string.message_precision3)
            }
            SensorManager.SENSOR_STATUS_UNRELIABLE -> {
                // Ненадежная точность
                Log.d("SensorAccuracy", "Ненадежная точность")
                _acuuracy.text = getString(R.string.message_precision4)
            }
            else -> {
                // Неизвестная точность
                Log.d("SensorAccuracy", "Неизвестная точность")
                _acuuracy.text = getString(R.string.message_precision5)
            }
        }
    }
}

