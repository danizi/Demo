package demo.xm.com.demo.down3.task.runnable

import demo.xm.com.demo.down3.DownManager
import demo.xm.com.demo.down3.config.DownConfig.Companion.DEFAULT
import demo.xm.com.demo.down3.enum_.DownErrorType
import demo.xm.com.demo.down3.task.DownTask
import demo.xm.com.demo.down3.utils.BKLog
import demo.xm.com.demo.down3.utils.FileUtil
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
        var DEFAULT_BUFFER_SIZE = 0
        fun newSingleRunnable2(task: DownTask?, downManager: DownManager?, listener: BaseRunnable.OnListener?): SingleRunnable2? {
            val file = FileUtil.createNewFile(task?.path, task?.dir, task?.fileName)
            val startIndex = file.length()
            val raf = RandomAccessFile(file, "rwd")
            raf.seek(startIndex)
            BKLog.d(TAG, "seek:$startIndex")
            DEFAULT_BUFFER_SIZE = downManager?.downConfig()?.bufferSize!!
            val singleRunnable = SingleRunnable2()
            singleRunnable.name = task?.name!!
            singleRunnable.url = task.url
            singleRunnable.total = task.total
            singleRunnable.process = startIndex
            singleRunnable.present = task.present.toFloat()

            singleRunnable.threadName = "SingleRunnable2"
            singleRunnable.raf = raf
            singleRunnable.rangeStartIndex = startIndex
            singleRunnable.rangeEndIndex = -1
            singleRunnable.listener = listener
            return singleRunnable
        }
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
            total = conn.contentLength.toLong() + rangeStartIndex

            if (total > getUsableSpace()) {
                listener?.onError(this, DownErrorType.NO_SPACE, "空间不足，下载资源大小 ：${getSizeUnit(total)} 可用资源大小 ：${getSizeUnit(getUsableSpace())}")
                runing(false)
                return
            }

            BKLog.d(TAG, "code:${conn.responseCode}")
            write(inputStream, raf)
            callBackComplete()  //完成回调
        } catch (e: Exception) {
            e.printStackTrace()
            listener?.onError(this, DownErrorType.UNKNOWN, e.message!!)
        }
    }

    private var isComplete = false
    private fun write(inputStream: InputStream?, raf: RandomAccessFile?) {
        /*文件写入操作*/
        var length: Int
        val bis = BufferedInputStream(inputStream)
        val buffer = ByteArray(bufferSize)
        try {
            while (true) {
                length = bis.read(buffer)
                if (length == -1) {
                    BKLog.d(TAG, "读取完成。")
                    isComplete = true
                    return
                }
                if (!runing.get()) {
                    BKLog.d(TAG, "外部修改了运行标志位，停止文件写入。")
                    return
                }
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
        val present = (100 * this.process / total).toFloat()
//        if (this.present < present) { //ps:测试多个任务下载时，偶尔出现进度来回跳动问题，为了确保回传进度是正常的，特加此判断
//            this.present = present
//        }
        listener?.onProcess(this, process, total, present)
    }

    private fun callBackComplete() {
        /*完成回调给观察者*/
        if (runing.get() && isComplete /*process == total*/) { //ps:在使用MultiRunnable下载时process total是不对应的，所以在这里加了标志位。
            listener?.onComplete(this, total)
            runing.set(false)
        }
    }

    override fun exit() {
        /*处于停止下载状态*/
        runing(false)
    }

}