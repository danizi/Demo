package demo.xm.com.demo.down3.test

import android.Manifest
import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import demo.xm.com.demo.R
import demo.xm.com.demo.down3.DownManager
import demo.xm.com.demo.down3.enum_.DownStateType
import demo.xm.com.demo.down3.task.DownTask
import demo.xm.com.demo.down3.utils.BKLog
import demo.xm.com.demo.down3.utils.FileUtil.getSizeUnit


class DownViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    companion object {
        const val TAG = "DownViewHolder"
    }

    private var mIv_icon: ImageView? = null
    private var mProgressBar: ProgressBar? = null
    private var mTv_name: TextView? = null
    private var mTv_state: TextView? = null
    private var mTv_down_des: TextView? = null

    private fun bindViews(view: View) {
        mIv_icon = view.findViewById(R.id.iv_icon)
        mProgressBar = view.findViewById(R.id.progressBar)
        mTv_name = view.findViewById(R.id.tv_name)
        mTv_state = view.findViewById(R.id.tv_state)
        mTv_down_des = view.findViewById(R.id.tv_down_des)
    }

    fun bind(task: DownTask, downManager: DownManager?) {
        bindViews(itemView)
        display(task)
        initEvent(downManager, task)
    }

    @SuppressLint("SetTextI18n")
    private fun display(task: DownTask) {
        mProgressBar?.max = 100
        mProgressBar?.progress = if (mProgressBar?.progress!! < task.present.toInt()) {
            task.present.toInt()
        } else {
            mProgressBar?.progress!!
        }
        mTv_name?.text = task.name
        mTv_down_des?.text = getSizeUnit(task.progress) + "/" + getSizeUnit(task.total)
        mTv_state?.text = when (task.state) {
            DownStateType.COMPLETE.ordinal -> {
                mProgressBar?.progress = 100
                mTv_down_des?.text = getSizeUnit(task.total) + "/" + getSizeUnit(task.total)
                "完成"
            }
            DownStateType.NOT_STARTED.ordinal -> {
                mTv_down_des?.text = "0M/0M"
                "点击下载"
            }
            DownStateType.PAUSE.ordinal -> {
                "暂停"
            }
            DownStateType.RUNNING.ordinal -> {
                "下载中..."
            }
            DownStateType.ERROR.ordinal -> {
                "下载出错啦..."
            }
            else -> {
                "xxx"
            }
        }

    }

    private fun initEvent(downManager: DownManager?, task: DownTask) {

        itemView.setOnClickListener {

            com.tbruyelle.rxpermissions.RxPermissions.getInstance(itemView.context)
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .subscribe { aBoolean ->
                        if (aBoolean!!) {
                            //当所有权限都允许之后，返回true
                            Log.i("permissions", "btn_more_sametime：$aBoolean")
                            if (task.state == DownStateType.NOT_STARTED.ordinal || task.state == DownStateType.PAUSE.ordinal) {
                                val downTask = DownTask()
                                downTask.url = task.url
                                downTask.uuid = task.uuid
                                downManager?.createDownTasker(downTask)?.enqueue()
                                mTv_state?.text = "加入下载队列"
                            } else {
                                BKLog.d(TAG, "item 无法点击 因为状态是:${task.state}")
                            }
                        } else {
                            //只要有一个权限禁止，返回false，
                            //下一次申请只申请没通过申请的权限
                            Log.i("permissions", "btn_more_sametime：$aBoolean")
                        }
                    }
        }
    }
}