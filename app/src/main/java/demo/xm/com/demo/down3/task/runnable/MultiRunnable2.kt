package demo.xm.com.demo.down3.task.runnable

import android.os.Environment
import demo.xm.com.demo.down3.enum_.DownErrorType
import demo.xm.com.demo.down3.utils.BKLog

import demo.xm.com.demo.down3.utils.CommonUtil
import demo.xm.com.demo.down3.utils.CommonUtil.getFileName
import demo.xm.com.demo.down3.utils.FileUtil
import demo.xm.com.demo.down3.utils.FileUtil.del
import demo.xm.com.demo.down3.utils.FileUtil.getSizeUnit
import demo.xm.com.demo.down3.utils.FileUtil.mergeFiles
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 任务多个线程下载（分段）
 */
class MultiRunnable2 : BaseRunnable() {

    companion object {
        const val TAG = "MultiRunnable2"
    }

    var threadNum = 3   //限定线程数量
    private var pool = ThreadPoolExecutor(threadNum, threadNum, 20, TimeUnit.SECONDS, ArrayBlockingQueue(2000)) //线程池
    private var subThreadCompleteCount = 0 //下载线程完成集合数量
    private var downRunnables: ArrayList<SingleRunnable> = ArrayList() //线程集合

    init {

    }

    override fun down() {
        /*执行下载操作*/
        //1 获取资源的大小
        val conn = iniConn()
        total = conn.contentLength.toLong()

        val lump = total / threadNum
        BKLog.d(TAG, "分成$threadNum lump -> $lump B ${getSizeUnit(lump.toLong())}M，总大小${getSizeUnit(total.toLong())} M $total B")
        //2 分配子线程数量,并开始下载

        //创建临时文件夹与文件
        val files = ArrayList<File>()
        val rafs = ArrayList<RandomAccessFile>()
        for (i in 0..(threadNum - 1)) {
            val dir = "$dir/${CommonUtil.getFileName(url)}_Temp"
            val fileName = "$i.temp"
            val file = FileUtil.createNewFile(path, dir, fileName)
            val raf = RandomAccessFile(file, "rw")
            raf.seek(file.length())
            files.add(file)
            rafs.add(raf)
        }

        //线程启动
        for (i in 0..(threadNum - 1)) {
            val file = files[i]
            val length = file.length()
            val startIndex = if (length == (lump * (i + 1) - 1).toLong()) {
                BKLog.d(TAG, "${file.name} 块下载完成")
                return
            } else {
                length + i * lump
            }
            val endIndex = if (i == (threadNum - 1)) {
                total
            } else {
                lump * (i + 1) - 1
            }

            val singleRunnable = SingleRunnable()
            singleRunnable.url = url
            singleRunnable.threadName = "$name MultiRunnable_SingleRunnable_$i"
            singleRunnable.raf = rafs[i]
            singleRunnable.rangeStartIndex = startIndex.toInt()
            singleRunnable.rangeEndIndex = endIndex.toInt()
            singleRunnable.process = files[i].length()
            singleRunnable.listener = object : SingleRunnable.OnListener {

                override fun onProcess(singleRunnable: SingleRunnable, process: Long, total: Long, present: Float) {
                    //3 获取下载的进度
                    callbackProcess(singleRunnable, process, total, present)
                }

                override fun onComplete(singleRunnable: SingleRunnable, total: Long) {
                    //4 下载完成
                    callbackComplete(singleRunnable, total)
                }

                override fun onError(singleRunnable: SingleRunnable, type: DownErrorType, s: String) {
                    //下载失败
                    callbackError(singleRunnable, type, s)
                }

                private fun callbackProcess(singleRunnable: SingleRunnable, process: Long, total: Long, present: Float) {
                    //Thread.sleep(500)
                    this@MultiRunnable2.process += process
                    listener?.onProcess(this@MultiRunnable2, process, this@MultiRunnable2.total, (process * 100 / total).toFloat())
                }

                private fun callbackComplete(singleRunnable: SingleRunnable, total: Long) {
                    subThreadCompleteCount++
                    if (subThreadCompleteCount == threadNum) {
                        runing(false)
                        exit()
                        val path = Environment.getExternalStorageDirectory().absolutePath
                        val outFile = FileUtil.createNewFile(path, dir, getFileName(url))
                        val inFile = File(path + File.separator + "$dir/${getFileName(url)}_Temp")
                        mergeFiles(outFile, inFile)
                        del(inFile)
                        listener?.onComplete(this@MultiRunnable2, total)
                    }
                }

                private fun callbackError(singleRunnable: SingleRunnable, type: DownErrorType, s: String) {
                    listener?.onError(this@MultiRunnable2, type, s)
                }
            }
            pool.submit(singleRunnable)
            downRunnables.add(singleRunnable)
        }
    }

    override fun exit() {
        /*用户退出停止一切线程操作*/
        runing(false)
        for (downRunnable in downRunnables) {
            downRunnable.exit()
        }
    }
}

