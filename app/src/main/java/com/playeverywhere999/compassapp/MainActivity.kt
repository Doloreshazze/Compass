package com.playeverywhere999.compassapp

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform


class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var handler: Handler
    private lateinit var runnable: Runnable

    

    //private lateinit var sensorManager: SensorManager
    private var rotationVectorSensor: Sensor? = null
    private lateinit var azimuthTextView: TextView
    private lateinit var _accuracy: TextView
    private lateinit var tvDirection: TextView
    private lateinit var earth: ImageView
    private lateinit var redArrow: ImageView
    private lateinit var limb: ImageView
    private var current_degree = 0f
    private var degree = 0f
    private lateinit var adView: AdView
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var magnetometer: Sensor
    private lateinit var btCalibration: Button


    private val previousValue: Float = 0f

    private var interAd: InterstitialAd? = null












    private fun initAdMob() {
        MobileAds.initialize(this)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }
    fun onClickCalibrationActivity(view: View) {

        showInterAd()

//        val intent = Intent(this, CalibrationActivity::class.java)
//        startActivity(intent)

    }
    fun checkSensors(context: Context): Boolean {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val hasAccelerometer = accelerometer != null
        val hasMagneticField = magneticField != null

        return hasAccelerometer && hasMagneticField
    }




        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            adView = findViewById(R.id.adView)
            azimuthTextView = findViewById(R.id.azimuth)
            earth = findViewById(R.id.earth)
            btCalibration = findViewById(R.id.btCalibration)
            btCalibration.text = getString(R.string.goToCalibrationButton)
            redArrow = findViewById(R.id.redArrow)
            limb = findViewById(R.id.limb)
            tvDirection = findViewById(R.id.tvDirection)
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)!!
            initAdMob()

            // Установка флага для удержания экрана включенным
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            // Создание Handler и Runnable для обновления рекламы
            handler = Handler(Looper.getMainLooper())
            runnable = object : Runnable {
                override fun run() { adView.loadAd(AdRequest.Builder().build())
                    handler.postDelayed(this, 20000)
                    // Обновление каждые 20 секунд
                } }

            // Создайте параметры запроса согласия
            val params = ConsentRequestParameters.Builder().build()

            // Инициализация ConsentInformation
            val consentInformation = UserMessagingPlatform.getConsentInformation(this)

            // Загрузите информацию о согласии
            consentInformation.requestConsentInfoUpdate(
                this,
                params,
                object : ConsentInformation.OnConsentInfoUpdateSuccessListener {
                    override fun onConsentInfoUpdateSuccess() {
                        // Проверьте, требуется ли пользователю отображать форму согласия
                        if (consentInformation.isConsentFormAvailable) {
                            // Загрузите и покажите форму согласия
                            loadAndShowConsentForm()
                        }
                    }
                },
                object : ConsentInformation.OnConsentInfoUpdateFailureListener {
                    override fun onConsentInfoUpdateFailure(formError: FormError) {
                        // Обработка ошибок
                    }
                }
            )
        }

        private fun loadAndShowConsentForm() {
            UserMessagingPlatform.loadConsentForm(
                this,
                object : UserMessagingPlatform.OnConsentFormLoadSuccessListener {
                    override fun onConsentFormLoadSuccess(consentForm: ConsentForm) {
                        // Показать форму согласия
                        consentForm.show(
                            this@MainActivity,
                            object : ConsentForm.OnConsentFormDismissedListener {
                                override fun onConsentFormDismissed(formError: FormError?) {
                                    // Обработка результата закрытия формы
                                }
                            }
                        )
                    }
                },
                object : UserMessagingPlatform.OnConsentFormLoadFailureListener {
                    override fun onConsentFormLoadFailure(formError: FormError) {
                        // Обработка ошибок загрузки формы
                    }
                }
            )
        }






    override fun onResume() {
        super.onResume()

        adView.resume()

        // Запуск обновления рекламы при возврате активности на передний план
        //handler.postDelayed(runnable, 10000)

        loadInterAd() // advertising раскомментировать для рекламы
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
        //val result = checkSensors(this)
        //if(!result)  _accuracy.text = getString(R.string.message_sensors)

        //sensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        //onAccuracyChanged(sensor, SensorManager.SENSOR_STATUS_ACCURACY_LOW) // Возможно, это не нужно

        //Log.d("MyTag", "onResume")

    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        adView.pause()

        // Остановка обновления рекламы при уходе активности с переднего плана
        handler.removeCallbacks(runnable)

        sensorManager.unregisterListener(this, accelerometer)
        sensorManager.unregisterListener(this, magnetometer)
    }

    override fun onDestroy() {
        super.onDestroy()
        adView.destroy()
    }

    private var currentAzimuth = 0f
    private var gravity = FloatArray(3)
    private var geomagnetic = FloatArray(3)
    private val alpha = 0.98f // Коэффициент фильтрации

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            gravity = lowPassFilter(event.values, gravity)
        } else if (event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = lowPassFilter(event.values, geomagnetic)
        }

        if (gravity != null && geomagnetic != null) {
            val rotationMatrix = FloatArray(9)
            val inclinationMatrix = FloatArray(9)

            if (SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, gravity, geomagnetic)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)

                var azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                azimuth = if (azimuth < 0) (azimuth + 360) else azimuth

                // Интерполяция для плавной ротации
                currentAzimuth = interpolateAzimuth(currentAzimuth, azimuth, 0.5f)
                limb.rotation = -currentAzimuth

                val azimuthDegree = azimuth.toInt()
                val directionText: String = when (azimuthDegree) {
                    in 350..360, in 0..10 -> getString(R.string.orientation1)
                    in 11..33 -> getString(R.string.orientation2)
                    in 34..57 -> getString(R.string.orientation3)
                    in 58..80 -> getString(R.string.orientation4)
                    in 81..103 -> getString(R.string.orientation5)
                    in 104..126 -> getString(R.string.orientation6)
                    in 127..149 -> getString(R.string.orientation7)
                    in 150..169 -> getString(R.string.orientation8)
                    in 170..190 -> getString(R.string.orientation9)
                    in 191..213 -> getString(R.string.orientation10)
                    in 214..236 -> getString(R.string.orientation11)
                    in 237..259 -> getString(R.string.orientation12)
                    in 260..280 -> getString(R.string.orientation13)
                    in 281..303 -> getString(R.string.orientation14)
                    in 304..326 -> getString(R.string.orientation15)
                    in 327..349 -> getString(R.string.orientation16)
                    else -> "Unknown"
                }




                tvDirection.text = "$directionText"
                degree = azimuth
                val formattedNumber = String.format("%03d", degree.toInt()) // Форматируем число с ведущими нулями до 3 разрядов
                azimuthTextView.text = "$formattedNumber\u00B0"






            }
        }
    }

    private fun lowPassFilter(input: FloatArray, output: FloatArray): FloatArray {
        for (i in input.indices) {
            output[i] = alpha * output[i] + (1 - alpha) * input[i]
        }
        return output
    }

    private fun interpolateAzimuth(currentValue: Float, targetValue: Float, factor: Float): Float {
        var delta = targetValue - currentValue

        if (delta > 180) {
            delta -= 360
        } else if (delta < -180) {
            delta += 360
        }

        return currentValue + delta * factor
    }







    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d("BensorB", "Accuracy changed: $accuracy")
        when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> {
                // Высокая точность
                //_accuracy.text = getString(R.string.message_precision1)
            }
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> {
                // Средняя точность
                //_accuracy.text = getString(R.string.message_calibration)
            }
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> {
                // Низкая точность
                //_accuracy.text = getString(R.string.message_calibration)
            }
            SensorManager.SENSOR_STATUS_UNRELIABLE -> {
                // Ненадежная точность
                //_accuracy.text = getString(R.string.message_calibration)
            }
            else -> {
                // Неизвестная точность
                //_accuracy.text = getString(R.string.message_calibration)
            }
        }
    }
    private fun loadInterAd(){
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this,
            "ca-app-pub-9400779051238454/2146361818", adRequest,
            object : InterstitialAdLoadCallback(){
                override fun onAdFailedToLoad(p0: LoadAdError) {
                    interAd = null
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    interAd = ad
                }
            })
    }

    private fun showInterAd(){
        if(interAd != null){
            interAd?.fullScreenContentCallback =
                object : FullScreenContentCallback(){
                    override fun onAdDismissedFullScreenContent() {

                        //showContent()
                        interAd = null
                        loadInterAd() // advertising раскомментировать для рекламы
                        backToCalibrationFromAdd()


                    }

                    override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                        //showContent()
                        interAd = null
                        loadInterAd()
                        backToCalibrationFromAdd()
                    }

                    override fun onAdShowedFullScreenContent() {
                        interAd = null
                        loadInterAd()
                    }
                }

            interAd?.show(this)
        } else {
            backToCalibrationFromAdd()
            //showContent()
        }
    }

    private fun backToCalibrationFromAdd() {
        val intent = Intent(this, CalibrationActivity::class.java)
        startActivity(intent)
    }

    private fun showContent(){
        Toast.makeText(this, "Запуск контента", Toast.LENGTH_LONG).show()
    }


}
