package demo.xm.com.demo.down2.log

import android.util.Log

/**
 * 日志工具类
 */
object BKLog {
    private const val debug = false
    private val TAG: String? = "xmDown"

    fun d(msg: String) {
        Log.d(TAG, msg)
    }

    fun e(msg: String) {
        Log.e(TAG, msg)
    }

    fun d(tag: String, msg: String) {
        if (debug) {
            Log.d("$TAG-$tag", msg)
        }
    }

    fun e(tag: String, msg: String) {
        if (debug) {
            Log.e("$TAG-$tag", msg)
        }
    }

}