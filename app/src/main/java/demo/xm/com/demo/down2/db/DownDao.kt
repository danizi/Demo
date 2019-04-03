package demo.xm.com.demo.down2.db

import android.content.Context
import android.database.sqlite.SQLiteOpenHelper
import android.text.TextUtils
import demo.xm.com.demo.down2.db.DownDBContract.DownTable.SQL_DELETE
import demo.xm.com.demo.down2.db.DownDBContract.DownTable.SQL_QUERY_ALL
import demo.xm.com.demo.down2.db.DownDBContract.DownTable.SQL_QUERY_INSERT
import demo.xm.com.demo.down2.db.DownDBContract.DownTable.SQL_QUERY_UUID
import demo.xm.com.demo.down2.db.DownDBContract.DownTable.SQL_UPDATE
import demo.xm.com.demo.down2.log.BKLog

/**
 * 数据库操作类
 */
class DownDao(context: Context?) {
    companion object {
        const val TAG = "DownDao"
    }

    private var helper: SQLiteOpenHelper? = null

    init {
        helper = DownDBHelp(context)
        /* 数据库操作
         * helper.writableDatabase 可读可写数据库
         * helper.readableDatabase 只读数据库
         *
         * 数据库生成所在路径
         * dada/data/包名/databases/xxx.db
         */
    }

    fun insert(downInfo: DownDBContract.DownInfo) {
        /*插入数据*/
        //首先判断是否存在
        if (exists(downInfo.uuid)) {
            BKLog.d(TAG, "数据数据库已存在...，${downInfo.toString()}")
            return
        }
        //数据插入数据库
        helper?.writableDatabase?.execSQL(
                SQL_QUERY_INSERT,
                arrayOf(downInfo.uuid,
                        downInfo.url,
                        downInfo.name,
                        downInfo.present,
                        downInfo.total,
                        downInfo.progress,
                        downInfo.absolutePath))
        helper?.writableDatabase?.close()
    }

    fun update(downInfo: DownDBContract.DownInfo) {
        /*更新数据*/
        helper?.writableDatabase?.execSQL(
                SQL_UPDATE,
                arrayOf(downInfo.url,
                        downInfo.present,
                        downInfo.total,
                        downInfo.progress,
                        downInfo.uuid /*查询条件*/))
        helper?.writableDatabase?.close()
    }

    fun delete(downInfo: DownDBContract.DownInfo) {
        /*删除数据*/
        helper?.writableDatabase?.execSQL(SQL_DELETE, arrayOf(downInfo.uuid/*查询条件*/))
        helper?.writableDatabase?.close()
    }

    fun queryAll(): ArrayList<DownDBContract.DownInfo> {
        /*查询所有数据*/
        val downs = ArrayList<DownDBContract.DownInfo>()
        val cursor = helper?.readableDatabase?.rawQuery(SQL_QUERY_ALL, null) ?: return downs
        while (cursor.moveToNext()) {
            val down = DownDBContract.DownInfo()
            BKLog.d("id: ${cursor.getInt(0)} url:${cursor.getString(2)}")
            down.uuid = cursor.getString(1)
            down.url = cursor.getString(2)
            down.name = cursor.getString(3)
            down.present = cursor.getInt(4)
            down.total = cursor.getInt(5)
            down.progress = cursor.getInt(6)
            down.absolutePath = cursor.getString(7)
            down.state = "点击恢复下载"
            downs.add(down)
        }
        helper?.writableDatabase?.close()
        return downs
    }

    fun query(uuid: String): ArrayList<DownDBContract.DownInfo> {
        /*查询数据通过uuid*/
        var downs = ArrayList<DownDBContract.DownInfo>()
        if (TextUtils.isEmpty(uuid)) return downs
        downs = ArrayList<DownDBContract.DownInfo>()
        val cursor = helper?.readableDatabase?.rawQuery(
                SQL_QUERY_UUID,
                arrayOf(uuid)) ?: return downs
        while (cursor.moveToNext()) {
            val down = DownDBContract.DownInfo()
            BKLog.d("id: ${cursor.getInt(0)} url:${cursor.getString(2)}")
            down.uuid = cursor.getString(1)
            down.url = cursor.getString(2)
            down.name = cursor.getString(3)
            down.present = cursor.getInt(4)
            down.total = cursor.getInt(5)
            down.progress = cursor.getInt(6)
            down.absolutePath = cursor.getString(7)
            down.state = "点击恢复下载"
            downs.add(down)
        }
        helper?.writableDatabase?.close()
        return downs
    }

    private fun exists(uuid: String): Boolean {
        /*通过uuid列值，判断数据库是否存在*/
        val cursor = helper?.readableDatabase?.rawQuery(
                SQL_QUERY_UUID,
                arrayOf(uuid)) ?: return false
        if (cursor.moveToNext()) {
            helper?.readableDatabase?.close()
            return true
        }
        return false
    }
}