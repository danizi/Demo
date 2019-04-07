package demo.xm.com.demo.down3

import android.content.Context
import android.os.Environment
import demo.xm.com.demo.down3.config.DownConfig
import demo.xm.com.demo.down3.db.DownDao
import demo.xm.com.demo.down3.dispatcher.DownDispatcher
import demo.xm.com.demo.down3.event.DownObservable
import demo.xm.com.demo.down3.task.DownTask
import demo.xm.com.demo.down3.task.DownTasker
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class DownManager {

    private var downDao: DownDao? = null //数据库操作对象
    private var downConfig: DownConfig? = null //配置对象
    private var downDispatcher: DownDispatcher? = null //下载分发器
    private var downObserverable: DownObservable? = null //被观察者对象 ps:监听下载状态，相比监听接口更灵活
    private var downTaskers: ArrayList<DownTasker>? = null

    private constructor()

    private constructor(downDao: DownDao?, downConfig: DownConfig, downDispatcher: DownDispatcher, downObserverable: DownObservable) {
        this.downDao = downDao
        this.downConfig = downConfig
        this.downDispatcher = downDispatcher
        this.downObserverable = downObserverable
        this.downTaskers = ArrayList()
    }

    companion object {
        fun createDownManager(context: Context): DownManager {
            //初始化数据库
            val dao = DownDao(context, "XmDown", null, 100)

            //初始化配置参数
            val config = DownConfig()
            config.path = Environment.getExternalStorageDirectory().absolutePath
            config.dir = "XmDown"
            config.threadNum = 3
            config.downTaskerPool = ThreadPoolExecutor(config.threadNum.toInt(), config.threadNum.toInt(), 30, TimeUnit.SECONDS, ArrayBlockingQueue(2000))
            config.isMultiRunnable = true
            config.isSingleRunnable = false
            config.runqueues = 5
            config.downDispatcherPool = ThreadPoolExecutor(config.runqueues.toInt(), config.runqueues.toInt(), 30, TimeUnit.SECONDS, ArrayBlockingQueue(2000))

            //初始化分发器
            val dispatcher = DownDispatcher()
            dispatcher.pool = config.downDispatcherPool
            dispatcher.runqueues = config.runqueues
            dispatcher.readyQueue = LinkedBlockingQueue<DownTasker>()
            dispatcher.runningQueue = LinkedBlockingQueue<DownTasker>()

            //初始化观察者 ps：任务状态监听器
            val observerable = DownObservable()

            /*创建下载管理者*/
            return DownManager(dao, config, dispatcher, observerable)
        }
    }

    fun createDownTasker(task: DownTask): DownTasker {
        /*创建下载者*/
        val downTasker = DownTasker(this, task)
        downTaskers?.add(downTasker)
        return downTasker
    }

    fun pauseAllDownTasker() {
        if (downTaskers?.isNotEmpty()!!) {
            for (tasker in downTaskers!!) {
                tasker.pause()
            }
        }
    }

    fun downDao(): DownDao? {
        return downDao
    }

    fun downConfig(): DownConfig? {
        return downConfig
    }

    fun downDispatcher(): DownDispatcher? {
        return downDispatcher
    }

    fun downObserverable(): DownObservable? {
        return downObserverable
    }
}