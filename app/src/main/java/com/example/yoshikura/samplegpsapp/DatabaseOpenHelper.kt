package com.example.yoshikura.samplegpsapp

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseOpenHelper(context: Context?, name: String?, version: Int) : SQLiteOpenHelper(context, name, null, version) {


    companion object {

        private const val DB_NAME = "sample.db"

        private const val DB_VERSION = 1

        private var instance :DatabaseOpenHelper? = null

        internal const val TABLE_NAME = "locationinfo"

        internal val COLUMN = arrayOf("id", "latitude", "longitude", "altitude", "accuracy", "bearing", "date")

        fun getInstance(context: Context):DatabaseOpenHelper {
            return instance ?: DatabaseOpenHelper(context.applicationContext, DB_NAME, DB_VERSION);
        }

        private val CREATE_TABLE_QUERY = ("CREATE TABLE " + TABLE_NAME + " (" + COLUMN[0] + " INTEGER PRIMARY KEY," +
                COLUMN[1] + " REAL," + COLUMN[2] + " REAL," + COLUMN[3] + " REAL," + COLUMN[4] + " TEXT," + COLUMN[5] + " REAL," +
                COLUMN[6] + " TEXT)")
    }


    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_QUERY)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

    }
}