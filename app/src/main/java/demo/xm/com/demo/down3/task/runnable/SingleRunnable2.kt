package demo.xm.com.demo.down3.task.runnable

import demo.xm.com.demo.down3.config.DownConfig.Companion.DEFAULT
import demo.xm.com.demo.down3.enum_.DownErrorType
import demo.xm.com.demo.down3.utils.BKLog
import demo.xm.com.demo.down3.utils.FileUtil.getSizeUnit
import demo.xm.com.demo.down3.utils.FileUtil.getUsableSpace
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.RandomAccessFile

/**
 * 任务单个线程下载
 */
open class SingleRunnable2 : BaseRunnable() {

    companion object {
        const val TAG = "SingleRunnable"
        //const val DEFAULT = -1 //数字默认值
        const val DEFAULT_BUFFER_SIZE = 1024 * 4 //文件写入的缓存字节大小
    }

    private var bufferSize = DEFAULT_BUFFER_SIZE
    var rangeStartIndex = DEFAULT
    var rangeEndIndex = DEFAULT

    override fun down() {
        // 连接下载资源
        try {
            val conn = iniConn()
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

            if (total > getUsableSpace()) {
                listener?.onError(this, DownErrorType.NO_SPACE, "空间不足，下载资源大小 ：${getSizeUnit(total)} 可用资源大小 ：${getSizeUnit(getUsableSpace())}")
                runing(false)
                return
            }

            BKLog.d(TAG, "code:${conn.responseCode}")
            write(inputStream, raf)
            callBackComplete()
        } catch (e: Exception) {
            e.printStackTrace()
            listener?.onError(this, DownErrorType.UNKNOWN, e.message!!)
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
                if (length == -1) {
                    BKLog.d(TAG, "")
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
        }
    }

    override fun exit() {
        /*处于停止下载状态*/
        runing(false)
    }

}
