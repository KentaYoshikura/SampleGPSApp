package com.example.yoshikura.samplegpsapp

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

import kotlin.math.*

/**
 * 回転角度取得クラス
 *
 * @author
 */
class OrientationListener : SensorEventListener {
    /** センサー管理クラス  */
    private var mManager: SensorManager? = null
    /** 地磁気行列  */
    private var mMagneticValues: FloatArray? = null
    /** 加速度行列  */
    private var mAccelerometerValues: FloatArray? = null

    /** X軸の回転角度  */
    /**
     * X軸の回転角度を取得する
     *
     * @return X軸の回転角度
     */
    @get:Synchronized
    var pitch: Int = 0
        private set
    /** Y軸の回転角度  */
    /**
     * Y軸の回転角度を取得する
     *
     * @return Y軸の回転角度
     */
    @get:Synchronized
    var roll: Int = 0
        private set
    /** Z軸の回転角度(方位角)  */
    /**
     * Z軸の回転角度(方位角)を取得する
     *
     * @return Z軸の回転角度
     */
    @get:Synchronized
    var azimuth: Int = 0
        private set

    /**
     * センサーイベント取得開始
     *
     * @param context
     * コンテキスト
     */
    @Synchronized
    fun resume(context: Context?) {
        if (context == null) {
            // 引数不正
            return
        }
        // 登録済なら一旦止める
        pause()
        if (mManager == null) {
            // 初回実行時
            mManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        }
        // 地磁気センサー登録
        mManager!!.registerListener(this, mManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_UI)
        // 加速度センサー登録
        mManager!!.registerListener(this, mManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI)

    }

    /**
     * センサーイベント取得終了
     */
    @Synchronized
    fun pause() {
        if (mManager != null) {
            mManager!!.unregisterListener(this)
        }
    }

    /**
     * ラジアンを角度に変換する
     *
     * @param radian
     * ラジアン
     * @return 角度
     */
    private fun radianToDegrees(radian: Float): Int {

        // ラジアン→角度変換
        var rad: Double = floor(Math.toDegrees(radian.toDouble()))

        if(rad < 0){
            rad += 360
        }

        return rad.toInt()
    }

    @Synchronized
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // 処理なし
    }

    @Synchronized
    override fun onSensorChanged(event: SensorEvent) {
        // センサーイベント
        when (event.sensor.type) {
            Sensor.TYPE_MAGNETIC_FIELD ->
                // 地磁気センサー
                mMagneticValues = event.values.clone()
            Sensor.TYPE_ACCELEROMETER ->
                // 加速度センサー
                mAccelerometerValues = event.values.clone()
            else ->
                // それ以外は無視
                return
        }
        if (mMagneticValues != null && mAccelerometerValues != null) {
            val rotationMatrix = FloatArray(MATRIX_SIZE)
            val inclinationMatrix = FloatArray(MATRIX_SIZE)
            val remapedMatrix = FloatArray(MATRIX_SIZE)
            val orientationValues = FloatArray(DIMENSION)
            // 加速度センサーと地磁気センサーから回転行列を取得
            SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, mAccelerometerValues, mMagneticValues)
            SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y, remapedMatrix)
            SensorManager.getOrientation(remapedMatrix, orientationValues)
            // ラジアン値を変換し、それぞれの回転角度を取得する
//            azimuth = radianToDegrees(orientationValues[0])
//            pitch = radianToDegrees(orientationValues[1])
//            roll = radianToDegrees(orientationValues[2])
            azimuth = (orientationValues[0] * (180 / Math.PI)).toInt()
            pitch = (orientationValues[1] * (180 / Math.PI)).toInt()
            roll = (orientationValues[2] * (180 / Math.PI)).toInt()

        }
    }

    // 方位を文字列として取得
    fun getOrientation(): String {
        return "X=$pitch Y=$roll Z=$azimuth"
    }

    companion object {
        /** デバッグ用  */
        private val DEBUG = true
        private val TAG = "OrientationListener"
        /** 行列数  */
        private val MATRIX_SIZE = 9
        /** 三次元(XYZ)  */
        private val DIMENSION = 3
    }
}
