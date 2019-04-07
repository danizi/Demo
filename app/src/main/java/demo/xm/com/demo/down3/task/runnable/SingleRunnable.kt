package demo.xm.com.demo.down3.task.runnable

import android.content.Context
import demo.xm.com.demo.down3.DownManager
import demo.xm.com.demo.down3.enum_.DownErrorType
import demo.xm.com.demo.down3.utils.BKLog
import demo.xm.com.demo.down3.utils.FileUtil.getSizeUnit
import demo.xm.com.demo.down3.utils.FileUtil.getUsableSpace
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 任务单个线程下载
 */
open class SingleRunnable : Runnable {

    companion object {
        const val TAG = "SingleRunnable"
        const val DEFAULT = -1 //数字默认值
        const val DEFAULT_EMPTY = "" //数字默认值
        const val DEFAULT_BUFFER_SIZE = 1024 * 4 //文件写入的缓存字节大小
    }

    var threadName = DEFAULT_EMPTY
    private var context: Context? = null
    var downManager: DownManager? = null

    var url = DEFAULT_EMPTY
//    private var path = DEFAULT_EMPTY
//    private var dir = DEFAULT_EMPTY
//    private var fileName = DEFAULT_EMPTY

    private var runing = AtomicBoolean(false)
    var raf: RandomAccessFile? = null
    var process: Long = DEFAULT.toLong()
    private var total: Long = DEFAULT.toLong()
    private var present: Float = DEFAULT.toFloat()
    private var bufferSize = DEFAULT_BUFFER_SIZE
    var rangeStartIndex = DEFAULT
    var rangeEndIndex = DEFAULT
    var listener: OnListener? = null

    override fun run() {
        down()
    }

    private fun down() {
        // 连接下载资源
        try {
            val url = URL(url)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
//            conn.connectTimeout = 5000
//            conn.readTimeout = 5000
            conn.doInput = true
            val value = if (/*rangeStartIndex > 0 && */rangeEndIndex > 0) {
                BKLog.d(TAG, "请求头 bytes=$rangeStartIndex-$rangeEndIndex")
                "bytes=$rangeStartIndex-$rangeEndIndex"
            } else {
                BKLog.d(TAG, "请求头 bytes=$rangeStartIndex-")
                "bytes=$rangeStartIndex-"
            }
            conn.setRequestProperty("Range", value)
            val inputStream = conn.inputStream
            total = conn.contentLength.toLong()
            BKLog.d(TAG, "code:${conn.responseCode}")
            //raf?.setLength(total)

//            if (conn.responseCode != 200 || conn.responseCode != 206) {
//                callBackError(DownErrorType.REQUEST_FAILURE)
//                return
//            }

//            if (callBackError(DownErrorType.NO_SPACE)) {
//                return
//            }

            runing(true)
            write(inputStream, raf)
            callBackComplete()
        } catch (e: Exception) {
            e.printStackTrace()
            listener?.onError(this, DownErrorType.UNKNOWN, e.toString())
        }
    }

    private fun write(inputStream: InputStream?, raf: RandomAccessFile?) {
        /*文件写入操作*/
        var length: Int
        val bis = BufferedInputStream(inputStream)
        val buffer = ByteArray(bufferSize)
        try {
            while (true) {
                length = bis.read(buffer)
                if (length == -1){
                    BKLog.d(TAG,"")
                    return//读取完成
                }
                if (!runing.get())
                    return  //退出下载标志位
                callBackProcess(length.toLong()) //进度回调
                raf?.write(buffer, 0, length)   //写入文件中
            }
        } catch (e: Exception) {
            e.printStackTrace()
            listener?.onError(this, DownErrorType.UNKNOWN, e.toString())
        } finally {
            raf?.close()
            bis.close()
        }
    }

    private fun callBackProcess(length: Long) {
        /*进度回调给观察者*/
        this.process += length
        present = (100 * this.process / total).toFloat()
        listener?.onProcess(this, process, total, present)
    }

    private fun callBackComplete() {
        /*完成回调给观察者*/
        if (runing.get()) {
            listener?.onComplete(this, total)
            runing.set(false)
            //downManager?.downObserverable?.notifyObserverComplete(null, total)
        }
    }


    private fun callBackError(type: DownErrorType): Boolean {
        return when (type) {
            DownErrorType.NO_SPACE -> {
                listener?.onError(this, type, "NO_SPACE")
                BKLog.d(TAG, "")
                callBackNoSpaceError(total.toInt())
            }
            DownErrorType.CONNECT_TIMEOUT -> {
                listener?.onError(this, type, "TIMEOUT")
                true
            }
            DownErrorType.UNKNOWN -> {
                listener?.onError(this, type, "UNKNOWN")
                true
            }
            DownErrorType.REQUEST_FAILURE -> {
                listener?.onError(this, type, "REQUEST_FAILURE")
                true
            }
            else -> {
                return false
            }
        }
    }

    private fun callBackNoSpaceError(contentLength: Int): Boolean {
        /*错误回调给观察者*/
        if (contentLength < 0) {
            BKLog.e(TAG, "获取文件大小有误，$contentLength")
            return true
        }
        if (contentLength > getUsableSpace(context)) {
            BKLog.e(TAG, "空间不足，下载资源大小 ：${getSizeUnit(contentLength.toLong())} 可用资源大小 ：${getSizeUnit(getUsableSpace(context))}")
            //通知用户下载错误
            downManager?.downObserverable()?.notifyObserverError(null, DownErrorType.NO_SPACE)
            exit()
            return true
        }
        return false
    }

    fun runing(): Boolean {
        return runing.get()
    }

    private fun runing(flag: Boolean) {
        runing.set(flag)
        if (flag) {
            //处于下载状态
        } else {
            BKLog.d(TAG, "$threadName 停止下载线程")
            //处于停止下载状态
        }
    }

    fun exit() {
        /*处于停止下载状态*/
        runing(false)
    }

    /**
     * 下载监听
     */
    interface OnListener {
        fun onProcess(singleRunnable: SingleRunnable, process: Long, total: Long, present: Float)
        fun onComplete(singleRunnable: SingleRunnable, total: Long)
        fun onError(singleRunnable: SingleRunnable, type: DownErrorType, s: String)
    }
}
