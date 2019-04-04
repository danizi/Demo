package demo.xm.com.demo.down2

import android.content.Context
import android.text.TextUtils
import demo.xm.com.demo.down2.db.DownDao
import demo.xm.com.demo.down2.log.BKLog
import demo.xm.com.demo.down2.utils.CommonUtil
import demo.xm.com.demo.down2.utils.FileUtil
import demo.xm.com.demo.down2.utils.FileUtil.getSizeUnit
import demo.xm.com.demo.down2.utils.FileUtil.getTotalSpace
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.atomic.AtomicBoolean

/**
 *  多线程下载器
 *  Java代码实现多线程下载和断点续传 https://blog.csdn.net/qq_32101859/article/details/53177428
 *  JAVA代码实现多线程下载 https://blog.csdn.net/weixin_41619686/article/details/81558306
 *  Android 文件下载三种基本方式 https://blog.csdn.net/u010203716/article/details/73194804
 *  https://blog.csdn.net/u010105970/article/details/51225850
 *  文件合并 https://www.cnblogs.com/lojun/articles/6111812.html
 */
class DownTasker {
    companion object {
        const val tag = "DownTasker"
    }

    private var downDao: DownDao? = null
    private var downManager: DownManager? = null
    private var downConfig: DownConfig? = null
    var downTask: DownTask? = null
    var runnable: Runnable? = null

    private constructor() : super()

    constructor(builder: Builder) {
        downDao = builder.downDao
        downManager = builder.downManager
        downTask = builder.downTask
        downConfig = builder.downConfig
        runnable = DownTaskerRunnable(this, downDao, downManager, downTask, downConfig)
    }

    fun enqueue() {
        /*加入下载队列*/
        downManager?.dispatcher?.enqueue(this)
    }

    fun pause() {
        /*暂停队列,将*/
        downManager?.dispatcher?.remove(this)
    }

    fun cancel() {
        /*取消下载任务，并将缓存删除*/
        (runnable as DownTaskerRunnable).removePool()
    }

    /**
     * 下载者构建类
     */
    class Builder {
        var downDao: DownDao? = null
        var downManager: DownManager? = null//下载管理器
        var downTask: DownTask? = null//下载任务信息
        var downConfig: DownConfig? = null//下载配置信息

        fun setDownManager(downManager: DownManager?): Builder {
            this.downManager = downManager
            return this
        }

        fun setTask(task: DownTask): Builder {
            this.downTask = task
            return this
        }

        fun setConfig(downConfig: DownConfig?): Builder {
            this.downConfig = downConfig
            return this
        }

        fun setDownDao(downDao: DownDao?): Builder {
            this.downDao = downDao
            return this
        }

        fun build(): DownTasker {
            if (downManager == null) throw NullPointerException("downManager is null")
            if (downTask == null) throw NullPointerException("downTask is null")
            if (downConfig == null) throw NullPointerException("downConfig is null")
            if (downDao == null) throw NullPointerException("downDao is null")
            return DownTasker(this)
        }
    }

    /**
     * 分段下载Runnable接口
     */
    private class DownTaskerRunnable(val downTasker: DownTasker, val downDao: DownDao?, val downManager: DownManager?, val downTask: DownTask?, downConfig: DownConfig?) : Runnable {

        private var url: String = ""
        private var ctx: Context? = null
        private var path: String? = ""
        private var dir: String? = ""
        private var threadNamePrefix: String? = ""
        private var tempSuffix: String? = ""
        private var buffer: ByteArray? = null
        private var multiplePool: ThreadPoolExecutor? = null
        private var maxMultipleThreadNum: Int = 0
        private var connectTimeout: Int = 0
        private var readTimeout: Int = 0
        var flag: AtomicBoolean = AtomicBoolean(true)

        init {
            this.url = downTask?.url!!
            this.ctx = downConfig?.ctx
            this.path = downConfig?.path
            this.dir = downConfig?.dir
            this.threadNamePrefix = downConfig?.threadNamePrefix
            this.tempSuffix = downConfig?.tempSuffix
            this.buffer = downConfig?.buffer
            this.multiplePool = downConfig?.multiplePool
            this.maxMultipleThreadNum = downConfig?.maxMultipleThreadNum!!
            this.connectTimeout = downConfig.connectTimeout
            this.readTimeout = downConfig.readTimeout
        }

        override fun run() {
            //主要获取下载“文件总大小”和 每个分段线程下载的大小
            val url = URL(url)
            val conn: HttpURLConnection? = url.openConnection() as HttpURLConnection //建立连接
            conn?.requestMethod = "GET"                       //请求方法
            conn?.doInput = true                              //打开获取输入流权限
            conn?.connectTimeout = connectTimeout             //连接超时时间
            conn?.readTimeout = readTimeout                   //读取超时时间
            val total = conn?.contentLength                   //下载的大小

            //判断可用空间大小
            if (total!! > FileUtil.getUsableSpace(ctx)) {
                BKLog.e(tag, "空间不足，下载内容大小:" + getSizeUnit(total.toLong()) + " 可用空间：" + getSizeUnit(FileUtil.getUsableSpace(ctx)) + " sd卡总空间：" + getSizeUnit(getTotalSpace()))
                downManager?.downObserverable?.notifyObserverError(downTasker, DownErrorType.NO_SPACE) //下载出错通知观察者
                return
            }
            BKLog.d(tag, "下载内容大小:" + getSizeUnit(total.toLong()) + " 可用空间：" + getSizeUnit(FileUtil.getUsableSpace(ctx)) + " sd卡总空间：" + getSizeUnit(getTotalSpace()))

            //执行多片段线程执行下载
            val segmentSize = total / maxMultipleThreadNum   //多线程每段下载的大小
            BKLog.d(tag, url.toString() + "总大小：$total B,分成$maxMultipleThreadNum 段下载，每段文件大小$segmentSize B")
            val file = if (TextUtils.isEmpty(downTask?.fileName) && TextUtils.isEmpty(downTask?.fileHouzui)) {
                val fileName = CommonUtil.getFileName(url.toString())
                BKLog.d(tag, "根据下载地址获取名称，下载文件名称:$fileName")
                FileUtil.createNewFile(path, dir, fileName)
            } else {
                val fileName = downTask?.fileName + File.separator + downTask?.fileHouzui
                BKLog.d(tag, "根据用户配置获取名称，下载文件名称:$fileName")
                FileUtil.createNewFile(path, dir, fileName)
            }
            this.downCores = createMultipleThread(segmentSize)!!//创建下载线程
            if (downCores.isNotEmpty()) {
                addPool(downCores)  // 线程加到线程池中执行
                getProcess(downCores, file, total) //获取下载的进度

                if (complete) {
                    //合并文件，合并成功，删除临时文件 PS:合成过程中需要对临时文件需要进行排序
                    val inFile = File(file.absolutePath + "_Temp")
                    FileUtil.mergeFiles(file, inFile)
                    FileUtil.del(inFile)
                }

            } else {
                BKLog.d(tag, "downCores is null")
            }

            if (complete) {
                downManager?.downObserverable?.notifyObserverComplete(downTasker, total.toLong()) //下载完成通知观察者
                downManager?.dispatcher?.finish(downTasker)    //任务下载完成通知分发器
            }
        }

        var complete = false
        private fun getProcess(downCores: ArrayList<DownCore>, file: File, total: Int) {
            /*获取下载进度*/
            complete = false
            var process: Long = 0
            while (!complete && flag.get()) {
                complete = true

                //遍历每个分段下载的状态，如果全部都下载成功
                for (downCore in downCores) {

                    if (downCore.downCoreReadable?.downState == false) {
                        complete = false
                    }

                    // 获取每个分段线程下载的字节数量
                    if (maxMultipleThreadNum == 1) {
                        process = downCore.downCoreReadable?.process!!
                    } else {
                        process += downCore.downCoreReadable?.process!!
                    }
                    downManager?.downObserverable?.notifyObserverProcess(downTasker, process, total.toLong(), ((process * 100 / total)).toFloat())//下载进度通知观察者
                }
            }
            if (!flag.get()) {
                complete = false
            }
        }

        private fun createMultipleThread(segmentSize: Int): ArrayList<DownCore>? {
            /*将分段任务线程放入到线程池中，进行下载处理*/
            val downCores = ArrayList<DownCore>()
            val maxMultipleThreadSize = maxMultipleThreadNum - 1
            for (i in 0..maxMultipleThreadSize) {
                val pair = getDownIndex(i, segmentSize, maxMultipleThreadSize)  //获取分段下载范围 PS:如果数据存在则直接放回记录

                val threadName = threadNamePrefix + i           //构建下载Runnable
                val downCore = DownCore.Builder().dir(dir)
                        .segment(i)
                        .downTask(downTask)
                        .startIndex(startIndex = pair.first.toLong())
                        .endIndex(endIndex = pair.second.toLong())
                        .buffer(buffer)
                        .threadNamePrefix(threadNamePrefix)
                        .tempSuffix(tempSuffix)
                        .downDao(downDao)
                        .build()

                downCores.add(downCore)//线程集合

                BKLog.d(tag, "*****************************")
                BKLog.d(tag, "$threadName 分段任务添加到下载线程池子中....")
                BKLog.d(tag, "$threadName startIndex:${pair.first.toLong()} endIndex:${pair.second.toLong()}")
                BKLog.d(tag, "")
            }
            return downCores
        }

        private fun getDownIndex(i: Int, segmentSize: Int, maxMultipleThreadSize: Int): Pair<Int, Int> {
            /*获取下载判断范围*/
            var startIndex = i * segmentSize

            //获取缓存的下载进度（字节）
            val downInfos = downDao?.query(downTask?.id!!)
            if (downInfos?.isNotEmpty()!!) {
                startIndex = downInfos[0].progress
                BKLog.d(tag, "本地缓存的下载信息:${downInfos.toString()}")
            }

            var endIndex = ((i + 1) * segmentSize) - 1
            if (i == maxMultipleThreadSize) {
                endIndex = -1
            }
            return Pair(startIndex, endIndex)
        }

        private var downCores = ArrayList<DownCore>()
        private fun addPool(downCores: ArrayList<DownCore>) {
            /*将分段任务线程放入到线程池中，进行下载处理*/
            for (downCore in downCores) {
                multiplePool?.execute(downCore.downCoreReadable)
            }
        }

        fun removePool() {
            BKLog.d(tag, "flag -> false")
            flag.set(false)
            for (downCore in downCores) {
                downCore.downCoreReadable?.flag?.set(false)
                multiplePool?.remove(downCore.downCoreReadable)
            }
        }
    }
}