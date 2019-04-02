package demo.xm.com.demo.down2.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import demo.xm.com.demo.down2.db.DownDBContract.DownTable.SQL_CREATE_DOWN_TABLE
import demo.xm.com.demo.down2.log.BKLog

/**
 * 数据库
 */
class DownDBHelp(context: Context?) : SQLiteOpenHelper(context, name, factory, version) {

    companion object {
        const val TAG = "DownDBHelp"
        const val name = "down.db"//数据库名称
        val factory: SQLiteDatabase.CursorFactory? = null//一般为null
        const val version = 1//版本号
    }

    /**
     * 数据库第一次创建会调用此函数
     */
    override fun onCreate(db: SQLiteDatabase?) {
        //创建表
        db?.execSQL(SQL_CREATE_DOWN_TABLE)
        BKLog.d(TAG, "onCreate 创建表")
    }

    /**
     * 版本更新调用此函数
     */
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        BKLog.d(TAG, "onUpgrade 数据库升级，oldVersion$oldVersion newVersion$newVersion")
    }
}