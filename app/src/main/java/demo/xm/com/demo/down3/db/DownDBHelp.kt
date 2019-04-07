package demo.xm.com.demo.down3.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import demo.xm.com.demo.down3.utils.BKLog

class DownDBHelp(context: Context?, name: String?, factory: SQLiteDatabase.CursorFactory?, version: Int) : SQLiteOpenHelper(context, name, factory, version) {
    companion object {
        const val TAG = "DownDBHelp"
    }

    /**
     * 数据库第一次创建会调用此函数
     */
    override fun onCreate(db: SQLiteDatabase?) {
        //创建表
        db?.execSQL(DownDBContract.DownTable.SQL_CREATE_DOWN_TABLE)
        BKLog.d(TAG, "onCreate 创建表")
    }

    /**
     * 版本更新调用此函数
     */
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        BKLog.d(TAG, "onUpgrade 数据库升级，oldVersion$oldVersion newVersion$newVersion")
    }
}