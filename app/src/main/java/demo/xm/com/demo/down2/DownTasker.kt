package demo.xm.com.demo.down2

import demo.xm.com.demo.down2.log.BKLog
import demo.xm.com.demo.down2.utils.CommonUtil
import demo.xm.com.demo.down2.utils.FileUtil
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 *  多线程下载器
 *  Java代码实现多线程下载和断点续传 https://blog.csdn.net/qq_32101859/article/details/53177428
 *  JAVA代码实现多线程下载 https://blog.csdn.net/weixin_41619686/article/details/81558306
 *  Android 文件下载三种基本方式 https://blog.csdn.net/u010203716/article/details/73194804
 *  https://blog.csdn.net/u010105970/article/details/51225850
 *  文件合并 https://www.cnblogs.com/lojun/articles/6111812.html
 */
class DownTasker(private val builder: DownDispatcher.DownDispatcherBuilder?, private var downTask: DownTask) : Thread(), OnDownListener, OnDownTasker {

    private val TAG = this::class.java.simpleName
    private var downCores: ArrayList<DownCore>? = null

    init {
        downCores = ArrayList()
    }

    @Synchronized
    override fun run() {
        /*主要获取下载“文件总大小”和 每个分段线程下载的大小*/
        val (maxMultipleThread, urlConn: HttpURLConnection?) = initConn()
        //下载的大小
        //多线程每段下载的大小
        val total = urlConn?.contentLength
        val segmentSize = total!! / maxMultipleThread
        BKLog.d(downTask.url + "总大小：$total B,分成$maxMultipleThread 段下载，每段文件大小$segmentSize B")

        /*判断可用空间大小*/
        if (total > FileUtil.getUsableSpace(builder?.ctx)) {
            BKLog.e(TAG, "空间不足")
            return
        }
        BKLog.d("下载内容大小:" + FileUtil.getSizeUnit(total.toLong()) + " 可用空间：" + FileUtil.getSizeUnit(FileUtil.getUsableSpace(builder?.ctx)) + " sd卡总空间：" + FileUtil.getSizeUnit(FileUtil.getTotalSpace()))

        /*创建一个占位文件，即下载的文件*/
        val file = FileUtil.createNewFile(builder?.path, builder?.dir, CommonUtil.getFileName(downTask.url))

        /*创建maxMultipleThread个Runnable实现下载，并添加到线程池中*/
        addPool(maxMultipleThread, segmentSize)

        /*获取下载的进度*/
        getProcess(file, total)

        /*合并文件，合并成功，删除临时文件 PS:合成过程中需要对临时文件需要进行排序*/
        val inFile = File(file.absolutePath + "_Temp")
        FileUtil.mergeFiles(file, inFile)
        FileUtil.del(inFile)
    }

    private fun getProcess(file: File, total: Int) {
        var complete = false
        var process: Long = 0
        while (!complete) {
            complete = true

            //遍历每个分段下载的状态，如果全部都下载成功
            for (downCore in downCores?.iterator()!!) {
                if (downCore.downState == false) {
                    complete = false
                }

                // 获取每个分段线程下载的字节数量
                process += downCore.process
                BKLog.d(TAG, file.absolutePath + "任务，下载进度：" + ((process / total) * 100) + "%")
            }
        }
        BKLog.d(TAG, file.absolutePath + "任务下载完成")
        BKLog.d(file.absolutePath + "任务下载完成")
    }

    private fun addPool(maxMultipleThread: Int, segmentSize: Int) {
        //将分段任务线程放入到线程池中，进行下载处理
        val maxMultipleThreadSize = maxMultipleThread - 1
        for (i in 0..maxMultipleThreadSize) {

            //获取分段下载范围
            val pair = getDownIndex(i, segmentSize, maxMultipleThreadSize)
            val startIndex = pair.first
            val endIndex = pair.second

            //构建下载Runnable
            //下载Runnable添加到线程池
            //Runnable添加到集合中
            val threadName = builder?.threadNamePrefix + i
            val downCore = buildDownCore(i, startIndex, endIndex)
            builder?.multiplePool?.submit(downCore)
            downCores?.add(downCore)

            BKLog.d(TAG, "*****************************")
            BKLog.d(TAG, "$threadName 分段任务添加到下载线程池子中....")
            BKLog.d(TAG, "$threadName startIndex:$startIndex endIndex:$endIndex")
            BKLog.d(TAG, "")
        }
    }

    private fun buildDownCore(i: Int, startIndex: Int, endIndex: Int): DownCore {
        val downCoreBuilder = DownCore.Builder()
        downCoreBuilder.dir = builder?.dir
        downCoreBuilder.segment = i
        downCoreBuilder.downTask = downTask
        downCoreBuilder.startIndex = startIndex.toLong()
        downCoreBuilder.endIndex = endIndex.toLong()
        downCoreBuilder.buffer = builder?.buffer
        downCoreBuilder.threadNamePrefix = builder?.threadNamePrefix
        downCoreBuilder.tempSuffix = builder?.tempSuffix
        return downCoreBuilder.build()
    }

    private fun getDownIndex(i: Int, segmentSize: Int, maxMultipleThreadSize: Int): Pair<Int, Int> {
        val startIndex = i * segmentSize
        var endIndex = ((i + 1) * segmentSize) - 1
        if (i == maxMultipleThreadSize) {
            endIndex = -1
        }
        return Pair(startIndex, endIndex)
    }

    private fun initConn(): Pair<Int, HttpURLConnection?> {
        val maxMultipleThread = builder?.maxMultipleThreadNum!!
        val url = URL(downTask.url)
        val urlConn: HttpURLConnection? = url.openConnection() as HttpURLConnection //建立连接
        urlConn?.requestMethod = "GET"                       //请求方法
        urlConn?.doInput = true                              //打开获取输入流权限
        urlConn?.connectTimeout = builder.connectTimeout
        urlConn?.readTimeout = builder.readTimeout
        return Pair(maxMultipleThread, urlConn)
    }

    override fun onStart(id: String?) {
        builder?.pool?.execute(this)
    }

    override fun onPause(id: String?) {
        builder?.pool?.remove(this)
    }

    override fun onCancle(id: String?) {
        builder?.pool?.remove(this)
    }

    override fun onDelete(id: String?) {
        builder?.pool?.remove(this)
    }

    override fun onProcess(downTasker: DownTasker, process: Long) {

    }

    override fun onComplete(downTasker: DownTasker) {

    }

    override fun onError(downTasker: DownTasker, typeError: DownTypeError) {

    }

}

/**
 * 对外提供接口
 */
interface OnDownTasker {

    /*启动下载任务*/
    fun onStart(id: String?)

    /*暂停下载任务*/
    fun onPause(id: String?)

    /*取消未下载完成的任务*/
    fun onCancle(id: String?)

    /*删除已下载任务的缓存*/
    fun onDelete(id: String?)
}

/**
 * 下载任务监听
 */
interface OnDownListener {
    fun onProcess(downTasker: DownTasker, process: Long)
    fun onComplete(downTasker: DownTasker)
    fun onError(downTasker: DownTasker, typeError: DownTypeError)
}

/**
 * 下载错误类型
 */
enum class DownTypeError {
    //文件创建错误
    //空间不足情况
    //连接超时情况
    //...
}