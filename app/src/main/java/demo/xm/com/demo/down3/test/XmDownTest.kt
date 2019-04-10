package demo.xm.com.demo.down3.test

import android.app.Activity
import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
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
            override fun onPause(tasker: DownTasker) {
                notifyUI(tasker, DownStateType.PAUSE)
            }

            override fun onDelete(tasker: DownTasker) {
                notifyUI(tasker, DownStateType.DELETE)
            }

            override fun onComplete(tasker: DownTasker, total: Long) {
                notifyUI(tasker, DownStateType.COMPLETE, total)
            }

            override fun onError(tasker: DownTasker, typeError: DownErrorType, s: String) {
                notifyUI(tasker, DownStateType.ERROR, DownConfig.DEFAULT, typeError, s)
            }

            override fun onProcess(tasker: DownTasker, process: Long, total: Long, present: Float) {
                notifyUI(tasker, DownStateType.RUNNING, total, null, "", process, present)
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

    fun notifyUI(tasker: DownTasker, stateType: DownStateType, total: Long = 0, typeError: DownErrorType? = null, s: String = "", process: Long = 0L, present: Float = 0F) {
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
            DownStateType.PAUSE -> {
                notifyItem(tasker.task)
            }
            DownStateType.DELETE -> {
                notifyItem(tasker.task)
            }
            else -> {
            }
        }
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


    fun initDisplay() {
        //首先从数据库中读取任务
        val tasks = downManager?.downDao()?.queryAll() ?: return
        for (task in tasks) {
            addNotifyUI(task)
        }
    }
}