package demo.xm.com.demo.down3.test

import android.app.Activity
import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import demo.xm.com.demo.down2.log.BKLog
import demo.xm.com.demo.down3.DownManager
import demo.xm.com.demo.down3.config.DownConfig
import demo.xm.com.demo.down3.enum_.DownErrorType
import demo.xm.com.demo.down3.enum_.DownStateType
import demo.xm.com.demo.down3.event.DownObserver
import demo.xm.com.demo.down3.task.DownTask
import demo.xm.com.demo.down3.task.DownTasker
import demo.xm.com.demo.down3.utils.CommonUtil
import demo.xm.com.demo.down3.utils.CommonUtil.md5

class XmDownTest(var context: Context) {
    companion object {
        private const val TAG = "XmDownTest"
    }

    private var downManager: DownManager? = null
    private var rv: RecyclerView? = null
    private var downAdapter: DownAdapter? = null

    init {
        downManager = DownManager.createDownManager(context)
        downManager?.downObserverable()?.registerObserver(object : DownObserver {
            override fun onComplete(tasker: DownTasker, total: Long) {
                BKLog.d(TAG, "onComplete ${tasker.task.name}")
                notifyUI(tasker, DownStateType.COMPLETE, total)
            }

            override fun onError(tasker: DownTasker, typeError: DownErrorType) {
                BKLog.d(TAG, "onError ${tasker.task.name}")
                notifyUI(tasker, DownStateType.ERROR, DownConfig.DEFAULT, typeError)
            }

            override fun onProcess(tasker: DownTasker, process: Long, total: Long, present: Float) {
                BKLog.i(TAG, "*************************************")
                BKLog.i(TAG, "onProcess ${tasker.task.name}")
                BKLog.i(TAG, "process $process total$total present$present")
                BKLog.i(TAG, " ")
                notifyUI(tasker, DownStateType.RUNNING, total, null, process, present)
            }
        })
        downAdapter = DownAdapter(downManager, ArrayList())

    }

    fun bindRv(rv: RecyclerView?) {
        this.rv = rv
        rv?.itemAnimator = null
        rv?.adapter = downAdapter
        rv?.layoutManager = LinearLayoutManager(context)
    }

    fun notifyUI(tasker: DownTasker, stateType: DownStateType, total: Long, typeError: DownErrorType? = null, process: Long = 0L, present: Float = 0F) {
        when (stateType) {
            DownStateType.COMPLETE -> {
                notifyItem(tasker.task)
            }
            DownStateType.ERROR -> {
                notifyItem(tasker.task)
            }
            DownStateType.RUNNING -> {
                notifyItem(tasker.task)
            }
            else -> {
            }
        }
    }

    fun exit() {
        downManager?.pauseAllDownTasker()
    }

    fun add(url: String) {
        val task = DownTask()
        task.url = url
        task.uuid = md5(url)
        task.name = CommonUtil.getFileName(url).replace(".apk", "")
        task.progress = 0
        task.present = 0
        addNotifyUI(task)
    }

    private fun addNotifyUI(task: DownTask) {
        /*添加任务刷新UI*/
        val positionStart = downAdapter?.data?.size!!
        val itemCount = 1
        downAdapter?.data?.add(task)
        rv?.adapter?.notifyItemRangeChanged(positionStart, itemCount)
    }

    private fun notifyItem(task: DownTask) {
        for (i in 0..(downAdapter?.data?.size!! - 1)) {
            val tempTask = (downAdapter?.data!![i] as DownTask)
            if (tempTask.uuid == task.uuid) {
                downAdapter?.data!![i] = task
                //主线程上刷新
                (context as Activity).runOnUiThread {
                    rv?.adapter?.notifyItemRangeChanged(i, 1)
                }
            }
        }
    }


    fun initDisplay() {
        //首先从数据库中读取任务
        val tasks = downManager?.downDao()?.queryAll() ?: return
        for (task in tasks) {
            addNotifyUI(task)
        }
    }
}