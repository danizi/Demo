package demo.xm.com.demo.down2

import android.app.Application
import android.content.Context
import android.os.Environment
import android.text.TextUtils
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 下载调度器，管理下载任务，提供了下载监听，暂停，下载，添加任务，取消任务等职责
 */
class DownDispatcher(b: DownDispatcherBuilder) : OnDownDispatcher {
    /* 调度器 */
    private var builder: DownDispatcherBuilder? = b
    /* 下载监听器 */
    var listener: OnDownListener? = null
    /* 下载任务集合 */
    private var downTasks: ArrayList<DownTask>? = null
    /* 下载任务器集合 */
    private var downTaskerMaps: HashMap<String, DownTasker>? = null
    /* 已下载完成任务集合 */
    private var downTaskeds: ArrayList<DownTask>? = null

    init {
        downTasks = ArrayList()
        downTaskeds = ArrayList()
        downTaskerMaps = HashMap()
    }

    override fun setOnDownListener(listener: OnDownListener?) {
        this.listener = listener
    }

    override fun addTask(task: DownTask?) {
        if (task != null && !TextUtils.isEmpty(task.id)) {
            downTasks?.add(task)
            downTaskerMaps?.put(task.id!!, DownTasker(builder, task)) //将调度器的配置信息“传递”给下载器 PS: xxxx
        }
    }

    override fun startTask(id: String?) {
        downTaskerMaps?.get(id)?.onStart(id)
    }

    override fun pauseTask(id: String?) {
        downTaskerMaps?.get(id)?.onPause(id)
    }

    override fun cancelTask(id: String?) {
        downTaskerMaps?.get(id)?.onCancle(id)
    }

    override fun deleteTask(id: String?) {
        downTaskerMaps?.get(id)?.onDelete(id)
    }

    override fun startAllTask() {
        for (enter in downTaskerMaps?.entries!!) {
            enter.value.onStart(enter.key)
        }
    }

    override fun pauseAllTask() {
        for (enter in downTaskerMaps?.entries!!) {
            enter.value.onPause(enter.key)
        }
    }

    override fun cancelAllTask() {
        for (enter in downTaskerMaps?.entries!!) {
            enter.value.onCancle(enter.key)
        }
    }

    override fun deleteAllTask() {
        for (enter in downTaskerMaps?.entries!!) {
            enter.value.onDelete(enter.key)
        }
    }

    /**
     * 调度器建造者
     */
    class DownDispatcherBuilder {
        var ctx: Context? = null
        /* 线程池 */
        var maxDownloadingTask = 0                    // 最大同时“调度器”分配的任务数量
        var maxMultipleThreadNum = 0                  // 最大同时“下载器”分配的线程数量
        var pool: ThreadPoolExecutor? = null          // 调度器线程池
        var multiplePool: ThreadPoolExecutor? = null  // 下载器线程池
        var connectTimeout = 0                        // 设置连接超时时间
        var readTimeout = 0                           // 设置读取超时时间

        /* 文件格式
         * 下载“文件”命名          ：path/dir/xxxx.xx
         * 下载临时“目录”命名格式  ：path/dir/xxxx.xx_Temp
         * 下载临时“文件”命名格式  ：path/dir/xxxx.xx_Temp/Thread_segment_1.temp
         *                             path/dir/xxxx.xx_Temp/Thread_segment_2.temp
         *                             ...
         *
         * 例如：下载地址是http://gyxz.ukdj3d.cn/vp/yx_sw1/warsong.apk，下载配置目录是xmDown，配置三个线程来下载，文件结构如下：
         * 下载“文件”命名         ：/storage/sdcard/xm/xmDown/warsong.apk
         * 下载临时“目录”命名格式 ：path/dir/warsong.apk_Temp
         * 下载临时“文件”命名格式 ：path/dir/warsong.apk_Temp/Thread_segment_1.temp
         *                            path/dir/warsong.apk_Temp/Thread_segment_2.temp
         *                            path/dir/warsong.apk_Temp/Thread_segment_3.temp
         */
        var path: String? = ""                                //文件下载路径
        var dir: String? = ""                                 // 下载文件的父目录
        var buffer: ByteArray? = null                         // 缓存字节大小
        var tempSuffix: String = ""                           // 临时文件的后缀名称
        var suffix: String = ""                               // 如果获取不到下载链接的文件名称，则默认设置该文件后缀

        /* 线程命名格式
         * threadNameprefix+segment
         *
         * 例如：使用三个线程下载任务，名称分别如下
         * Thread_segment_0
         * Thread_segment_1
         * Thread_segment_2
         */
        var threadNamePrefix: String = ""

        /*构建调度器*/
        fun builder(): DownDispatcher {
            if (ctx == null) {
                throw NullPointerException("context is null")
            }
            if (maxDownloadingTask <= 0) {
                maxDownloadingTask = 5
            }
            if (maxMultipleThreadNum <= 0) {
                maxMultipleThreadNum = 1
            }
            if (connectTimeout <= 0) {
                connectTimeout = 60000
            }
            if (connectTimeout <= 0) {
                readTimeout = 60000
            }
            if (TextUtils.isEmpty(tempSuffix)) {
                tempSuffix = ".temp"
            }
            if (TextUtils.isEmpty(suffix)) {
                suffix = ".xm"
            }
            if (TextUtils.isEmpty(threadNamePrefix)) {
                threadNamePrefix = "Thread_segment_"
            }
            if (TextUtils.isEmpty(dir)) {
                dir = "xmDown"
            }
            if (TextUtils.isEmpty(path)) {
                path = Environment.getExternalStorageDirectory().canonicalPath
            }
            if (buffer == null) {
                buffer = ByteArray(1024 * 1024 * 4)
            }
            if (pool == null) {
                pool = ThreadPoolExecutor(maxDownloadingTask, maxDownloadingTask, 30, TimeUnit.SECONDS, ArrayBlockingQueue(2000))
            }
            if (multiplePool == null) {
                multiplePool = ThreadPoolExecutor(maxMultipleThreadNum, maxMultipleThreadNum, 30, TimeUnit.SECONDS, ArrayBlockingQueue(2000))
            }
            return DownDispatcher(this)
        }
    }
}

/**
 * 对外提供接口
 */
interface OnDownDispatcher {

    /*设置下载监听*/
    fun setOnDownListener(listener: OnDownListener?)

    /**
     * 操作单个任务相关方法
     */
    /*添加下载任务*/
    fun addTask(task: DownTask?)

    /*启动下载任务*/
    fun startTask(id: String?)

    /*暂停下载任务，其实就是移除线程池中的任务线程*/
    fun pauseTask(id: String?)

    /*取消“未下载完成任务”*/
    fun cancelTask(id: String?)

    /*删除“已下载任务”*/
    fun deleteTask(id: String?)

    /**
     * 操作所有任务相关方法
     */
    fun startAllTask()

    fun pauseAllTask()

    fun cancelAllTask()

    fun deleteAllTask()
}