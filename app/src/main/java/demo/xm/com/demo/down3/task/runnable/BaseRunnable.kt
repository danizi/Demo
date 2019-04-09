package demo.xm.com.demo.down3.task.runnable

import demo.xm.com.demo.down3.enum_.DownErrorType
import demo.xm.com.demo.down3.task.runnable.SingleRunnable.Companion.DEFAULT
import demo.xm.com.demo.down3.task.runnable.SingleRunnable.Companion.DEFAULT_EMPTY
import demo.xm.com.demo.down3.utils.BKLog
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

abstract class BaseRunnable : Runnable {
    /**
     * 共用属性
     */
    var runing = AtomicBoolean(false) //运行状态
    var threadName = DEFAULT_EMPTY  //线程名称
    var process: Long = DEFAULT.toLong() // 下载进度（单位 B）
    var total: Long = DEFAULT.toLong() //下载文件总大小（单位 B）
    var present: Float = DEFAULT.toFloat() //下载进度(单位 %)
    var listener: BaseRunnable.OnListener? = null //下载监听

    /**
     * 多线程会使用到的属性
     */
    var url = DEFAULT_EMPTY  //下载地址
    var name = DEFAULT_EMPTY //下载名称
    var path = DEFAULT_EMPTY //下载根目录
    var dir = DEFAULT_EMPTY //下载目录

    /**
     * 单线程会使用到的属性
     */
    var raf: RandomAccessFile? = null //下载文件

    override fun run() {
        runing(true)
        down()
    }

    /**
     * 处理下载
     */
    abstract fun down()

    /**
     * 打开网络流 ps:这里暂时使用硬编码，后期使用拦截器来处理
     */
    fun iniConn(): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.doInput = true
        conn.setRequestProperty("Accept-Encoding", "identity")
        return conn
    }

    /**
     * 设置线程是否标志
     */
    fun runing(flag: Boolean) {
        runing.set(flag)
        if (!flag) {
            BKLog.d(SingleRunnable.TAG, "$threadName 停止下载线程")
        }
    }

    /**
     * 线程退出下载
     */
    abstract fun exit()

    /**
     * 下载回调
     */
    interface OnListener {
        fun onProcess(baseRunnable: BaseRunnable, process: Long, total: Long, present: Float)
        fun onComplete(baseRunnable: BaseRunnable, total: Long)
        fun onError(baseRunnable: BaseRunnable, type: DownErrorType, s: String)
    }
}