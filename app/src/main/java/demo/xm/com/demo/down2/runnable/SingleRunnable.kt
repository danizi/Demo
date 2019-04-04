package demo.xm.com.demo.down2.runnable

import android.content.Context
import demo.xm.com.demo.down2.DownErrorType
import demo.xm.com.demo.down2.DownManager
import demo.xm.com.demo.down2.log.BKLog
import demo.xm.com.demo.down2.utils.FileUtil.getSizeUnit
import demo.xm.com.demo.down2.utils.FileUtil.getUsableSpace
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

    private var threadName = DEFAULT_EMPTY
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
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.doInput = true
            if (rangeStartIndex != DEFAULT && rangeEndIndex != DEFAULT) { //分段
                conn.setRequestProperty("Range", "bytes=$rangeStartIndex-$rangeEndIndex")
            } else if (rangeStartIndex > 0 && rangeEndIndex == DEFAULT) { //下载全部
                conn.setRequestProperty("Range", "bytes=$rangeStartIndex-")
            }
            total = conn.contentLength.toLong()
            raf?.setLength(total)

            if (conn.responseCode != 200 && conn.responseCode != 206) {
                callBackError(DownErrorType.REQUEST_FAILURE)
                return
            }

            if (callBackNoSpaceError(conn.contentLength)) {
                return
            }

            runing(true)
            write(conn.inputStream, raf)
            callBackComplete()
        } catch (e: Exception) {
            e.printStackTrace()
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
                if (length == -1) return//读取完成
                if (!runing.get()) return  //退出下载标志位
                callBackProcess(length.toLong()) //进度回调
                raf?.write(buffer, 0, length)   //写入文件中
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
        //downManager?.downObserverable?.notifyObserverProcess(null, process, total, present)  //PS：当用退出下载线程的时候，文件缓存的位置与数据库登录的位置不一致，需要重新校验一次
    }

    private fun callBackComplete() {
        /*完成回调给观察者*/
        if (runing.get()) {
            listener?.onComplete(this, total)
            //downManager?.downObserverable?.notifyObserverComplete(null, total)
        }
    }

    private fun callBackNoSpaceError(contentLength: Int): Boolean {
        /*错误回调给观察者*/
        if (contentLength > getUsableSpace(context)) {
            BKLog.e(TAG, "空间不足，下载资源大小 ：${getSizeUnit(contentLength.toLong())} 可用资源大小 ：${getSizeUnit(getUsableSpace(context))}")
            //通知用户下载错误
            downManager?.downObserverable?.notifyObserverError(null, DownErrorType.NO_SPACE)
            exit()
            return true
        }
        return false
    }

    private fun callBackError(type: DownErrorType): Boolean {
        return when (type) {
            DownErrorType.NO_SPACE -> {
                listener?.onError(this, type, "NO_SPACE")
                true
            }
            DownErrorType.CONNECT_TIMEOUT -> {
                true
            }
            DownErrorType.UNKNOWN -> {
                true
            }
            else -> {
                return false
            }
        }
    }

    fun runing(): Boolean {
        return runing.get()
    }

    private fun runing(flag: Boolean) {
        runing.set(flag)
        if (flag) {
            //处于下载状态
        } else {
            //处于停止下载状态
            context = null
            downManager = null
            raf = null
            context = null
            downManager = null
            process = DEFAULT.toLong()
            total = DEFAULT.toLong()
            present = DEFAULT.toFloat()
            bufferSize = DEFAULT_BUFFER_SIZE
            rangeStartIndex = DEFAULT
            rangeEndIndex = DEFAULT
        }
    }

    fun exit() {
        /*处于停止下载状态*/
        runing(false)
    }
}

/**
 * 下载监听
 */
interface OnListener {
    fun onProcess(singleRunnable: SingleRunnable, process: Long, total: Long, present: Float)
    fun onComplete(singleRunnable: SingleRunnable, total: Long)
    fun onError(singleRunnable: SingleRunnable, type: DownErrorType, s: String)
}