package demo.xm.com.demo.down3.utils

import android.util.Log

/**
 * 日志工具类
 */
object BKLog {
    private const val debug = true
    private val TAG: String? = "xmDown"
    const val I = 3
    const val D = 2
    const val W = 1
    const val E = 0
    private var LEVEL = D

    fun d(msg: String?) {
        Log.d(TAG, msg)
    }

    fun e(msg: String?) {
        Log.e(TAG, msg)
    }

    fun i(tag: String?, msg: String) {
        if (LEVEL >= I) {
            Log.i("$TAG-$tag", msg)
        }
    }

    fun d(tag: String?, msg: String?) {
        if (LEVEL >= D) {
            Log.d("$TAG-$tag", msg)
        }
    }

    fun w(tag: String?, msg: String) {
        if (LEVEL >= W) {
            Log.w("$TAG-$tag", msg)
        }
    }

    fun e(tag: String?, msg: String?) {
        if (LEVEL >= E) {
            Log.e("$TAG-$tag", msg)
        }
    }


}