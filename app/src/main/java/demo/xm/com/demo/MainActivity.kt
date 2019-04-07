package demo.xm.com.demo

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import demo.xm.com.demo.down2.DownErrorType
import demo.xm.com.demo.down2.DownManager
import demo.xm.com.demo.down2.DownTask
import demo.xm.com.demo.down2.DownTasker
import demo.xm.com.demo.down2.db.DownDBContract
import demo.xm.com.demo.down2.event.DownObserver
import demo.xm.com.demo.down2.log.BKLog
import demo.xm.com.demo.down2.runnable.MultiRunnable
import demo.xm.com.demo.down2.runnable.SingleRunnable
import demo.xm.com.demo.down2.utils.CommonUtil
import demo.xm.com.demo.down2.utils.FileUtil
import demo.xm.com.demo.down2.utils.FileUtil.createNewFile
import java.io.RandomAccessFile
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


/**
 * 测试功能点
 * 1 在空间不足的情况下
 * 2 添加多个任务查看下载是否正常
 * 3 查看断点下载功能是否正常     https://blog.csdn.net/modurookie/article/details/80830763
 * 4 重复地址下载地址怎么处理
 * 5 在不同系统下是否正常
 */
class MainActivity : AppCompatActivity(), DownObserver {

    private var tag = "MainActivity"
    private var downManager: DownManager? = null
    private val downUrlArray = arrayOf(
//            "http://img1.imgtn.bdimg.com/it/u=2735633715,2749454924&fm=26&gp=0.jpg",
//            "http://img4.imgtn.bdimg.com/it/u=3590849871,3724521821&fm=26&gp=0.jpg",
//            "http://img5.imgtn.bdimg.com/it/u=4060543606,3642835235&fm=26&gp=0.jpg",
//            "http://img1.imgtn.bdimg.com/it/u=2430510654,3359275973&fm=26&gp=0.jpg",
//            "http://img0.imgtn.bdimg.com/it/u=3967239004,1951414302&fm=26&gp=0.jpg",
//            "https://apk.apk.xgdown.com/down/1hd.apk",
            "https://cavedl.leiting.com/full/caveonline_M141859.apk"
//            "http://gyxz.ukdj3d.cn/vp/yx_sw1/warsong.apk",
//            "http://gyxz.ukdj3d.cn/vp1/yx_ljun1/Pokemmo.apk",
//            "http://gyxz.ukdj3d.cn/hk1/yx_xxm1/shanshuozhiguang.apk"
    )
    private var myAdapter: MyAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViews()
        iniData()
        initEvent()
    }

    var singleRunnable: SingleRunnable? = null

    private var rv: RecyclerView? = null
    private var btnAdd: Button? = null

    private fun findViews() {
        rv = findViewById(R.id.rv)
        btnAdd = findViewById(R.id.btn_add)
    }

    private fun iniData() {
        downManager = DownManager(this)
        downManager?.downObserverable?.registerObserver(this)
        myAdapter = MyAdapter(downManager)
        rv?.adapter = myAdapter
        rv?.layoutManager = LinearLayoutManager(this)
        displayCache()
    }

    private fun displayCache() {
        /*读取数据库的记录*/
        val downInfos = downManager?.downDao?.queryAll()
        if (downInfos?.isNotEmpty()!!) {
            //刷新RecyclerView页面
            myAdapter?.data?.addAll(downInfos)
            val posStart = 0
            val itemCount = myAdapter?.data?.size
            myAdapter?.notifyItemRangeChanged(posStart, itemCount!!)
        } else {
            BKLog.d(tag, "读取数据库中下载信息为空")
        }
    }

    private fun test() {
        //val url = "https://apk.apk.xgdown.com/down/1hd.apk"   //39.38MB
        val pool = ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, ArrayBlockingQueue(2000))
        for (url in downUrlArray) {
            //val url = url   //39.38MB
            val path = Environment.getExternalStorageDirectory().absolutePath
            val dir = "xmDown"
            val fileName = CommonUtil.getFileName(url)
            val file = createNewFile(path, dir, fileName)
            val startIndex = file.length()
            val endIndex = -1
            val raf = RandomAccessFile(file, "rwd")
            raf.seek(startIndex)
            BKLog.d(tag, "seek:$startIndex")
            singleRunnable = SingleRunnable()
            singleRunnable?.url = url
            singleRunnable?.threadName = "singleRunnable"
            singleRunnable?.raf = raf
            singleRunnable?.downManager = downManager
            singleRunnable?.rangeStartIndex = startIndex.toInt()
            singleRunnable?.rangeEndIndex = endIndex
            singleRunnable?.process = startIndex
            singleRunnable?.listener = object : SingleRunnable.OnListener {
                override fun onProcess(singleRunnable: SingleRunnable, process: Long, total: Long, present: Float) {
                    BKLog.i(tag, "process$process total$total present$present")
                }

                override fun onComplete(singleRunnable: SingleRunnable, total: Long) {
                    //singleRunnable.exit()
                    BKLog.d(tag, "${singleRunnable.url}onComplete")
                }

                override fun onError(singleRunnable: SingleRunnable, type: DownErrorType, s: String) {
                    //singleRunnable.exit()
                    BKLog.d(tag, "${singleRunnable.url}onError")
                }
            }
            //Thread(singleRunnable).start()
            pool.submit(singleRunnable)
        }
    }

    private var multiRunnable: MultiRunnable? = null
    private fun test2() {
        val pool = ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, ArrayBlockingQueue(2000))
        for (url in downUrlArray) {
            multiRunnable = MultiRunnable()
            multiRunnable?.url = url
            multiRunnable?.threadNum = 3
            multiRunnable?.threadName = "MultiRunnable"
            multiRunnable?.listener = object : MultiRunnable.OnListener {
                override fun onProcess(multiRunnable: MultiRunnable, process: Long, total: Long, present: Float) {
                    BKLog.i(tag, "${multiRunnable.url}onProcess process$process total$total present$present")
                }

                override fun onComplete(multiRunnable: MultiRunnable, total: Long) {
                    BKLog.d(tag, "${multiRunnable.url}onComplete total$total")
                }

                override fun onError(multiRunnable: MultiRunnable, type: DownErrorType, s: String) {
                    BKLog.e(tag, "${multiRunnable.url}onError")
                }
            }
            pool.submit(multiRunnable)
//            Thread(multiRunnable).start()
        }
    }

    private fun initEvent() {
        var count = 0
        btnAdd?.setOnClickListener {
            //test()
            test2()
            //创建的内容
//            if (count < downUrlArray.size) {
//                val downInfo = DownDBContract.DownInfo()
//                val url = downUrlArray[count]
//                downInfo.url = url
//                downInfo.uuid = md5(url)
//                downInfo.name = downUrlArray[count]
//                downInfo.progress = 0
//                downInfo.state = "点击开始"
//                downInfo.total = 0
//                downInfo.present = 0
//                //累加刷新
//                val posStart = myAdapter?.data?.size
//                val itemCount = 1
//                myAdapter?.data?.add(downInfo)
//                myAdapter?.notifyItemRangeChanged(posStart!!, itemCount)
//
//                count++
//            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        downManager?.dispatcher?.removeAll()
        downManager?.downObserverable?.removeObserver(this)
        singleRunnable?.exit()
        multiRunnable?.exit()
    }

    override fun onProcess(tasker: DownTasker, process: Long, total: Long, present: Float) {
        BKLog.i(tag, "*************************************")
        BKLog.i(tag, "onProcess ${tasker.downTask.toString()}")
        BKLog.i(tag, "process $process total$total present$present")
        BKLog.i(tag, " ")
        updateDownInfo("onProcess", tasker, process, total, present)
        cache("onProcess", tasker, process, total, present)
    }

    override fun onComplete(tasker: DownTasker, total: Long) {
        BKLog.d(tag, "*************************************")
        BKLog.d(tag, "onComplete ${tasker.downTask.toString()}")
        BKLog.d(tag, " ")
        updateDownInfo("onComplete", tasker, -1, total)
        cache("onComplete", tasker, -1, total)
    }

    override fun onError(tasker: DownTasker, typeError: DownErrorType) {
        BKLog.d(tag, "*************************************")
        BKLog.d(tag, "onError ${tasker.downTask.toString()}")
        BKLog.d(tag, " ")
        updateDownInfo("onError", tasker, -1, -1, -1f, typeError)
        cache("onError", tasker, -1, -1, -1f, typeError)
    }

    private fun cache(type: String, tasker: DownTasker, process: Long = -1, total: Long = -1, present: Float = -1f, typeError: DownErrorType = DownErrorType.UNKNOWN) {
        return
        /*缓存数据*/
        val downInfos = myAdapter?.data!! as ArrayList<DownDBContract.DownInfo>
        for (i in 0..(downInfos.size - 1)) {
            val downInfo = downInfos[i]
            downManager?.downDao?.insert(downInfo)
            if (downInfo.name == tasker.downTask?.url) {
                when (type) {
                    "onProcess" -> {
                        downInfo.state = "正在下载中..."
                        downInfo.progress = process.toInt()
                        downInfo.present = present.toInt()
                        downInfo.total = total.toInt()
                        downManager?.downDao?.update(downInfo)
                    }
                    "onComplete" -> {
                        downInfo.state = "下载完成"
                        downInfo.progress = total.toInt()
                        downManager?.downDao?.update(downInfo)
                    }
                    "onError" -> {
                        downInfo.state = "下载错误"
                        downManager?.downDao?.update(downInfo)
                    }
                }
            }
        }
    }

    private fun updateDownInfo(type: String, tasker: DownTasker, process: Long = -1, total: Long = -1, present: Float = -1f, typeError: DownErrorType = DownErrorType.UNKNOWN) {
        return
        /*刷新下载信息*/
        val downInfos = myAdapter?.data!! as ArrayList<DownDBContract.DownInfo>
        for (i in 0..(downInfos.size - 1)) {
            val downInfo = downInfos[i]
            if (downInfo.name == tasker.downTask?.url) {
                when (type) {
                    "onProcess" -> {
                        downInfo.state = "正在下载中..."
                        downInfo.progress = process.toInt()
                        downInfo.present = present.toInt()
                        downInfo.total = total.toInt()
                    }
                    "onComplete" -> {
                        downInfo.state = "下载完成"
                        downInfo.progress = total.toInt()
                    }
                    "onError" -> {
                        downInfo.state = "下载错误"
                    }
                }
                if (type == "onProcess") {
                    Thread.sleep(1000)
                    this.runOnUiThread {
                        myAdapter?.notifyItemRangeChanged(i, 1)
                    }
                } else {
                    //刷新
                    this.runOnUiThread {
                        myAdapter?.notifyItemRangeChanged(i, 1)
                    }
                }
            }
        }
    }
}

private class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

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

    @SuppressLint("SetTextI18n")
    fun bind(downInfo: DownDBContract.DownInfo, downManager: DownManager?) {
        bindViews(itemView)
        mTv_name?.text = downInfo.name
        mTv_down_des?.text = FileUtil.getSizeUnit(downInfo.progress.toLong()) + "/ " + FileUtil.getSizeUnit(downInfo.total.toLong())
        mTv_state?.text = downInfo.state
        mProgressBar?.max = 100
        mProgressBar?.progress = downInfo.present.toInt()
        if (downInfo.progress == 100) {
            itemView.isClickable = true
        }
        mTv_state?.text = downInfo.state
        itemView.setOnClickListener {
            itemView.isClickable = false
            mTv_state?.text = "正在努力连接中..."
            //mProgressBar?.progress = 0
            val downTask = DownTask.Builder()
                    .id(downInfo.uuid)
                    .url(downInfo.name)
                    .build()
            val downTasker = downManager?.newDownTasker(downTask)
            downTasker?.enqueue()
        }
    }
}

private class MyAdapter(var downManager: DownManager?, var data: ArrayList<Any>? = ArrayList()) : RecyclerView.Adapter<MyViewHolder>() {

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): MyViewHolder {
        val view = LayoutInflater.from(p0.context).inflate(R.layout.item_down, p0, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return if (data?.isEmpty()!!) {
            0
        } else {
            data?.size!!
        }
    }

    override fun onBindViewHolder(p0: MyViewHolder, p1: Int) {
        p0.bind(data?.get(p1) as DownDBContract.DownInfo, downManager)
    }
}

