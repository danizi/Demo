package demo.xm.com.demo.down3.test

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Message
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
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
                //notifyUI(tasker, DownStateType.DELETE)
                notifyDelete(tasker)
            }

            private fun notifyDelete(tasker: DownTasker) {
                val it = downAdapter?.data?.iterator() ?: return
                var i = 0
                while (it.hasNext()) {
                    val taskerTemp = (it.next() as DownTasker)
                    if (taskerTemp.task.uuid == tasker.task.uuid) {
                        it.remove()
                        (context as Activity).runOnUiThread {
                            rv?.adapter?.notifyItemRangeChanged(i, 1)
                        }
                        break
                    }
                    i++
                }
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

    private val handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            val tasker = msg?.obj as DownTasker
            notifyItem(tasker.task)
        }
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
                val msg = handler.obtainMessage()
                msg.what = 1
                msg.obj = tasker
                handler.sendMessageDelayed(msg, 1000)
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
            val tempTask = (downAdapter?.data!![i] as DownTasker)
            if (tempTask.task.uuid == task.uuid) {
                val tasker = ((downAdapter?.data!![i]) as DownTasker)
                tasker.task = task
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
        com.tbruyelle.rxpermissions.RxPermissions.getInstance(context)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe { aBoolean ->
                    if (aBoolean!!) {
                        //当所有权限都允许之后，返回true
                        val task = DownTask()
                        task.url = url
                        downAdapter?.data?.add(downManager?.createDownTasker(task)!!)
                        rv?.adapter?.notifyDataSetChanged()
                    } else {
                        //只要有一个权限禁止，返回false，
                        //下一次申请只申请没通过申请的权限
                        Log.i("permissions", "btn_more_sametime：$aBoolean")
                    }
                }
    }

    fun initDisplay() {
        //首先从数据库中读取任务
        val tasks = downManager?.downDao()?.queryAll() ?: return
        for (task in tasks) {
            downAdapter?.data?.add(downManager?.createDownTasker(task)!!)
        }
        rv?.adapter?.notifyDataSetChanged()
    }

    fun editMode() {
        /*编辑模式*/
        DownViewHolder.isEdit = !DownViewHolder.isEdit
        downAdapter?.notifyDataSetChanged()
    }

    fun delete() {
        /*删除任务*/
        for (tasker in downAdapter?.data!!) {
            val downTasker = tasker as DownTasker
            if (downTasker.task.isSelect) {
                downTasker.delete()
            }
        }
    }
}