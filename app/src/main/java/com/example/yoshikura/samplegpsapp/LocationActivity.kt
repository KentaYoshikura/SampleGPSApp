package com.example.yoshikura.samplegpsapp

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.OnSuccessListener
import java.text.DateFormat
import java.util.Date

class LocationActivity : AppCompatActivity() {

    // Fused Location Provider API.
    private var fusedLocationClient: FusedLocationProviderClient? = null

    // Location Settings APIs.
    private var settingsClient: SettingsClient? = null
    private var locationSettingsRequest: LocationSettingsRequest? = null
    private var locationCallback: LocationCallback? = null
    private var locationRequest: LocationRequest? = null
    private var location: Location? = null
    private var locationManager: LocationManager? = null

    private var lastUpdateTime: String? = null
    private var requestingLocationUpdates: Boolean? = null
    private var priority = 0
    private var textView: TextView? = null
    private var textLog: String? = null
    private var provider: String? = null
    private var orientation: OrientationListener? = null
    // データベース
    private var helper: DatabaseOpenHelper? = null
    private var db: SQLiteDatabase? = null
    // 方位の表示
    private var orientationView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("MainActivity", "LocationActivity onCreate()")

        // 初期化
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        settingsClient = LocationServices.getSettingsClient(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        priority = 0

        helper = DatabaseOpenHelper.getInstance(this)
        db = helper!!.writableDatabase

        orientation = OrientationListener()

        orientation!!.resume(this)

        createLocationCallback()
        createLocationRequest()
        buildLocationSettingsRequest()

        textView = findViewById(R.id.textView)
        orientationView = findViewById(R.id.orientationView)

        // 測位開始
        startLocationUpdates()
    }

    // locationのコールバックを受け取る
    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)

                location = locationResult!!.lastLocation

                lastUpdateTime = DateFormat.getTimeInstance().format(Date())
                updateLocationUI()
            }
        }
    }

    private fun updateLocationUI() {

        textLog = ""

        // getLastLocation()からの情報がある場合のみ
        if (location != null) {

            val fusedName = arrayOf("緯度", "経度", "標高", "精度")
            Log.d("locate", location!!.provider)
            val fusedData = arrayOf(location!!.latitude, location!!.longitude, location!!.altitude, location!!.provider)

            val strBuf = StringBuilder("---------- Location ---------- \n")

            for (i in fusedName.indices) {
                strBuf.append(fusedName[i])
                strBuf.append(" = ")
                strBuf.append(fusedData[i].toString())
                strBuf.append("\n")
            }

            strBuf.append("Time")
            strBuf.append(" = ")
            strBuf.append(lastUpdateTime)
            strBuf.append("\n")

            textLog += strBuf
            textView!!.text = textLog
        }

        orientationView?.text = orientation!!.getOrientation()

      //  insertDataBase()
      //  selectDataBase()

    }

    private fun insertDataBase() {
        try {
            // データの挿入
            val insertValues = ContentValues()
            insertValues.put(DatabaseOpenHelper.COLUMN[1], location!!.latitude)
            insertValues.put(DatabaseOpenHelper.COLUMN[2], location!!.longitude)
            insertValues.put(DatabaseOpenHelper.COLUMN[3], location!!.altitude)
            insertValues.put(DatabaseOpenHelper.COLUMN[4], provider)
            insertValues.put(DatabaseOpenHelper.COLUMN[5], 0.0)
            insertValues.put(DatabaseOpenHelper.COLUMN[6], lastUpdateTime)

            var result = db!!.insert(DatabaseOpenHelper.TABLE_NAME, null, insertValues)

        } catch (e: Exception) {
            Log.e("DB_ERROR", e.message)
            db!!.close()
            helper!!.close()
        }
    }

    private fun selectDataBase(){
        var c = db!!.rawQuery("SELECT * FROM " + DatabaseOpenHelper.TABLE_NAME, null)
        // DBの中身表示
        while(c.moveToNext()){
            print(c.getString(c.getColumnIndex(DatabaseOpenHelper.COLUMN[1])) + "\t")
            print(c.getString(c.getColumnIndex(DatabaseOpenHelper.COLUMN[2])) + "\t")
            print(c.getString(c.getColumnIndex(DatabaseOpenHelper.COLUMN[3])) + "\t")
            print(c.getString(c.getColumnIndex(DatabaseOpenHelper.COLUMN[4])) + "\t")
            print(c.getString(c.getColumnIndex(DatabaseOpenHelper.COLUMN[5])) + "\t")
            println(c.getString(c.getColumnIndex(DatabaseOpenHelper.COLUMN[6])))
        }
        c.close()
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest()

        if(locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // 高精度
            provider = LocationManager.GPS_PROVIDER
            locationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }else if(locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            // ネットワーク
            provider = LocationManager.NETWORK_PROVIDER
            locationRequest!!.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }else{
            // 他アプリの位置情報を使う
            provider = LocationManager.PASSIVE_PROVIDER
            locationRequest!!.priority = LocationRequest.PRIORITY_NO_POWER
        }

        // アップデートのインターバル期間設定
        // 単位：msec
        locationRequest!!.interval = 5000
        // 正確なインターバル期間設定
        // 単位：msec
        locationRequest!!.fastestInterval = 1000

    }

    // 端末で測位できる状態か確認する。wifi, GPSなどがOffになっているとエラー情報のダイアログが出る
    private fun buildLocationSettingsRequest() {
        val builder = LocationSettingsRequest.Builder()

        builder.addLocationRequest(locationRequest!!)
        locationSettingsRequest = builder.build()
    }

    override fun onActivityResult(requestCode: Int,
                                  resultCode: Int, data: Intent) {
        when (requestCode) {
            // requestCodeのチェック
            REQUEST_CHECK_SETTINGS -> when (resultCode) {
                Activity.RESULT_OK -> Log.i("debug", "User agreed to make required location settings changes.")
                Activity.RESULT_CANCELED -> {
                    Log.i("debug", "User chose not to make required location settings changes.")
                    requestingLocationUpdates = false
                }
            }
        }
    }

    // FusedLocationApiによるlocation updatesをリクエスト
    private fun startLocationUpdates() {
        // Begin by checking if the device has the necessary location settings.
        settingsClient!!.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(this,
                        OnSuccessListener {
                            Log.i("debug", "All location settings are satisfied.") //成功

                            // パーミッションの確認
                            if (ActivityCompat.checkSelfPermission(
                                            this@LocationActivity,
                                            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                    ActivityCompat.checkSelfPermission(
                                            this@LocationActivity,
                                            Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                                // TODO: Consider calling
                                //    ActivityCompat#requestPermissions
                                // here to request the missing permissions, and then overriding
                                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                //                                          int[] grantResults)
                                // to handle the case where the user grants the permission. See the documentation
                                // for ActivityCompat#requestPermissions for more details.
                                return@OnSuccessListener
                            }
                            fusedLocationClient!!.requestLocationUpdates(
                                    locationRequest, locationCallback!!, Looper.myLooper())
                        })
                .addOnFailureListener(this) { e ->
                    val statusCode = (e as ApiException).statusCode
                    when (statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                            Log.i("debug", "Location settings are not satisfied. Attempting to upgrade " + "location settings ") //失敗
                            try {
                                // Show the dialog by calling startResolutionForResult(), and check the
                                // result in onActivityResult().
                                val rae = e as ResolvableApiException
                                rae.startResolutionForResult(
                                        this@LocationActivity,
                                        REQUEST_CHECK_SETTINGS)

                            } catch (e: IntentSender.SendIntentException) {
                                Log.i("debug", "PendingIntent unable to execute request.")
                            }

                        }
                        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                            val errorMessage = "Location settings are inadequate, and cannot be " + "fixed here. Fix in Settings."
                            Log.e("debug", errorMessage)
                            Toast.makeText(this@LocationActivity,
                                    errorMessage, Toast.LENGTH_LONG).show()

                            requestingLocationUpdates = false
                        }
                    }
                }
        requestingLocationUpdates = true
    }

    private fun stopLocationUpdates() {
        Log.d("MainActivity", "onStop()")
        textView!!.text = textLog

        if (!(requestingLocationUpdates)!!) {
            Log.d("debug", "stopLocationUpdates: " + "updates never requested, no-op.")

            return
        }

        fusedLocationClient!!.removeLocationUpdates(locationCallback!!)
                .addOnCompleteListener(this
                ) { requestingLocationUpdates = false }
    }

    companion object {
        private const val REQUEST_CHECK_SETTINGS = 0x1
    }


    override fun onResume() {
        super.onResume()
        // 高精度にするよう促すダイアログを閉じたときに再度リクエストする
        startLocationUpdates()
    }

    override fun onStop() {
        super.onStop()
        orientation!!.pause()
        db!!.close()
        helper!!.close()
    }
}
