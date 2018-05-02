package com.example.yoshikura.samplegpsapp

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

class MainActivity : AppCompatActivity() {

    private val requestCode = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("MainActivity", "onCreate()")

        if (Build.VERSION.SDK_INT >= 23) {
            checkPermission()
        } else {
            locationActivity()
        }
    }

    // 位置情報許可の確認
    fun checkPermission() {
        // 既に許可している
        if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationActivity()
        } else {
            // 拒否または初回起動時
            requestLocationPermission()
        }
    }

    // 許可を求める
    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
            // ダイアログ表示
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    requestCode)

        } else {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    requestCode)

        }
    }

    // 結果の受け取り
    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray) {

        if (requestCode == this.requestCode) {
            // 使用が許可された
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationActivity()

            } else {
                // 拒否された場合は何もしない
            }
        }
    }

    // Intent でLocation
    private fun locationActivity() {
        val intent = Intent(application, LocationActivity::class.java)
        startActivity(intent)
    }
}
