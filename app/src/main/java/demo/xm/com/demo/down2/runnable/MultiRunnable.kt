package demo.xm.com.demo.down2.runnable

import android.os.Environment
import demo.xm.com.demo.down2.DownErrorType
import demo.xm.com.demo.down2.utils.FileUtil
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 任务多个线程下载（分段）
 */
class MultiRunnable : Runnable {

    private var pool: ExecutorService? = null
    private var url = ""
    private var total = 0
    private var threadNum = 2
    private var downRunnables: ArrayList<SingleRunnable> = ArrayList()
    private var exit = AtomicBoolean(false)

    override fun run() {
        exit.set(false) //应用退出标志，用来停止线程的
        down()
    }

    private fun down() {
        /*执行下载操作*/
        //1 获取资源的大小
        iniConn()

        //2 分配子线程数量,并开始下载
        for (i in 0..(threadNum - 1)) {

            val url = "http://img1.imgtn.bdimg.com/it/u=2735633715,2749454924&fm=26&gp=0.jpg"   //39.38MB
            val path = Environment.getExternalStorageDirectory().absolutePath
            val dir = "xmDown"
            val fileName = "u=2735633715,2749454924&fm=26&gp=0.jpg"
            val file = FileUtil.createNewFile(path, dir, fileName)
            val startIndex = file.length()
            val endIndex = -1
            val raf = RandomAccessFile(file, "rwd")
            raf.seek(startIndex + 1)

            val singleRunnable = SingleRunnable()
            singleRunnable.url = url
            singleRunnable.raf = raf
            singleRunnable.rangeStartIndex = startIndex.toInt()
            singleRunnable.rangeEndIndex = endIndex
            singleRunnable.process = startIndex
            singleRunnable.listener = object : OnListener {
                override fun onProcess(singleRunnable: SingleRunnable, process: Long, total: Long, present: Float) {

                }

                override fun onComplete(singleRunnable: SingleRunnable, total: Long) {
                    singleRunnable.exit()
                }

                override fun onError(singleRunnable: SingleRunnable, type: DownErrorType, s: String) {
                    singleRunnable.exit()
                }
            }
            pool?.submit(singleRunnable)
            downRunnables.add(singleRunnable)
        }

        //3 获取下载的进度
        present()

        //4 下载完成
        complete()
    }

    private fun iniConn() {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.doInput = true
        total = conn.contentLength
    }

    private fun present() {
        /*获取下载进度,单位百分比*/
        var process = 0L
        var runing = false
        while (!runing) {
            runing = true
            for (downRunnable in downRunnables) {
                process += downRunnable.process
                if (!downRunnable.runing()) {
                    runing = false
                }
            }
        }
    }

    private fun complete() {
        /*完成下载*/
        if (!exit.get()) {

        }
    }

    fun exit() {
        /*用户退出停止一切线程操作*/
        exit.set(true)
        for (downRunnable in downRunnables) {
            downRunnable.exit()
            pool = null
        }
    }
}