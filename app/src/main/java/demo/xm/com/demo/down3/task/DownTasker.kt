package demo.xm.com.demo.down3.task

import android.text.TextUtils
import demo.xm.com.demo.down3.DownManager
import demo.xm.com.demo.down3.config.DownConfig.Companion.DEFAULT
import demo.xm.com.demo.down3.enum_.DownErrorType
import demo.xm.com.demo.down3.enum_.DownStateType
import demo.xm.com.demo.down3.task.runnable.BaseRunnable
import demo.xm.com.demo.down3.task.runnable.MultiRunnable2
import demo.xm.com.demo.down3.task.runnable.SingleRunnable2
import demo.xm.com.demo.down3.utils.BKLog
import demo.xm.com.demo.down3.utils.CommonUtil
import demo.xm.com.demo.down3.utils.CommonUtil.getFileName
import demo.xm.com.demo.down3.utils.CommonUtil.md5
import demo.xm.com.demo.down3.utils.FileUtil
import java.io.File
import java.io.RandomAccessFile

class DownTasker(private val downManager: DownManager, val task: DownTask) {

    companion object {
        private const val TAG = "DownTasker"
    }

    var runnable: BaseRunnable? = null

    init {
        replenishTask()
        runnable = createRunnable()
    }

    private fun replenishTask() {
        /*补充任务的信息*/
        task.name = getFileName(task.url).replace(".apk", "")
        task.uuid = md5(task.url)
        if (TextUtils.isEmpty(task.fileName)) {
            task.fileName = getFileName(task.url)
        }
        task.state = DownStateType.NOT_STARTED.ordinal
        if (task.progress < 0) {
            task.progress = DEFAULT
        }
        if (task.total < 0) {
            task.total = DEFAULT
        }
        if (task.present < 0) {
            task.present = DEFAULT
        }
        task.path = downManager.downConfig()?.path!!
        task.dir = downManager.downConfig()?.dir!!
        task.absolutePath = task.path + File.separator + task.dir + File.separator + task.fileName
    }

    private fun createRunnable(): BaseRunnable? {
        /*获取下载Runnable接口，接口分为单线程和多线程下载*/
        val listener = object : BaseRunnable.OnListener {

            override fun onProcess(multiRunnable: BaseRunnable, process: Long, total: Long, present: Float) {
                task.progress = process
                task.total = total
                task.present = present.toLong()
                task.state = DownStateType.RUNNING.ordinal
                downManager.downObserverable()?.notifyObserverProcess(this@DownTasker, process, total, present) //通知“观察者”下载进度
                BKLog.i(MultiRunnable2.TAG, "taskName:${multiRunnable.name} process:$process present:${(process * 100 / total).toFloat()}")
            }

            override fun onComplete(multiRunnable: BaseRunnable, total: Long) {
                task.total = total
                task.state = DownStateType.COMPLETE.ordinal
                downManager.downObserverable()?.notifyObserverComplete(this@DownTasker, total) //通知“观察者”下载完成
                downManager.downDispatcher()?.finish(this@DownTasker) //通知“分发器”下载完成
                BKLog.d(TAG, "${multiRunnable.name} onComplete total$total")
            }

            override fun onError(multiRunnable: BaseRunnable, type: DownErrorType, s: String) {
                task.state = DownStateType.ERROR.ordinal
                downManager.downObserverable()?.notifyObserverError(this@DownTasker, type, s)//通知观察者下载错误
                downManager.downDispatcher()?.finish(this@DownTasker) //通知“分发器”下载错误
                BKLog.e(TAG, "${multiRunnable.name} onError $s")
            }
        }
        return if (downManager.downConfig()?.isMultiRunnable == true) {
            val multiRunnable = MultiRunnable2()

            multiRunnable.name = task.name
            multiRunnable.url = task.url
            multiRunnable.total = task.total
            multiRunnable.process = task.progress
            multiRunnable.present = task.present.toFloat()

            multiRunnable.threadName = "MultiRunnable2"
            multiRunnable.threadNum = downManager.downConfig()?.threadNum?.toInt()!!
            multiRunnable.dir = downManager.downConfig()?.dir!!
            multiRunnable.path = downManager.downConfig()?.path!!
            multiRunnable.listener = listener
            multiRunnable
        } else {
            val path = downManager.downConfig()?.path
            val dir = downManager.downConfig()?.dir
            val fileName = CommonUtil.getFileName(task.url)
            val file = FileUtil.createNewFile(path, dir, fileName)
            val startIndex = file.length()
            val endIndex = -1
            val raf = RandomAccessFile(file, "rwd")
            raf.seek(startIndex)
            BKLog.d(TAG, "seek:$startIndex")

            val singleRunnable = SingleRunnable2()
            singleRunnable.name = task.name
            singleRunnable.url = task.url
            singleRunnable.total = task.total
            singleRunnable.process = startIndex
            singleRunnable.present = task.present.toFloat()

            //singleRunnable.url = task.url
            singleRunnable.threadName = "SingleRunnable"
            singleRunnable.raf = raf
            singleRunnable.name = task.name
            singleRunnable.rangeStartIndex = startIndex
            singleRunnable.rangeEndIndex = endIndex.toLong()
            singleRunnable.process = startIndex
            //singleRunnable.total = task.total
            singleRunnable.listener = listener
            singleRunnable
        }
    }

    fun enqueue() {
        /*开始任务*/
        downManager.downDispatcher()?.enqueue(this) //下载分发器执行下载线程
        downManager.downDao()?.insert(task) //任务存入数据库
    }

    fun pause() {
        /*暂停任务*/
        runnable?.exit()
        downManager.downDispatcher()?.remove(this)//下载分发器队列中移除该任务
        if (task.state == DownStateType.RUNNING.ordinal) {
            task.state = DownStateType.NOT_STARTED.ordinal //更改状态
            task.state = DownStateType.PAUSE.ordinal       //更改状态
        }
        downManager.downDao()?.update(task) //任务更新数据库
    }

    fun delete() {
        /*删除任务*/
        pause()
        downManager.downDao()?.delete(task) //从数据库中移除
    }
}