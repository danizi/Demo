package demo.xm.com.demo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import demo.xm.com.demo.down2.*
import demo.xm.com.demo.down2.log.BKLog


class MainActivity : AppCompatActivity(), DownObserver {

    private var tag = "MainActivity"
    private var downManager: DownManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        downManager = DownManager(this)
        downManager?.downObserverable?.registerObserver(this)
        val downUrlArray = arrayOf(
//                "https://cavedl.leiting.com/full/caveonline_M141859.apk",
//                "http://gyxz.ukdj3d.cn/vp/yx_sw1/warsong.apk",
//                "http://gyxz.ukdj3d.cn/vp1/yx_ljun1/Pokemmo.apk",
//                "http://gyxz.ukdj3d.cn/hk1/yx_xxm1/shanshuozhiguang.apk",
                "http://img1.imgtn.bdimg.com/it/u=2735633715,2749454924&fm=26&gp=0.jpg",
                "http://img4.imgtn.bdimg.com/it/u=3590849871,3724521821&fm=26&gp=0.jpg",
                "http://img5.imgtn.bdimg.com/it/u=4060543606,3642835235&fm=26&gp=0.jpg",
                "http://img1.imgtn.bdimg.com/it/u=2430510654,3359275973&fm=26&gp=0.jpg",
                "http://img0.imgtn.bdimg.com/it/u=3967239004,1951414302&fm=26&gp=0.jpg"
        )
        var count = 0

        findViewById<Button>(R.id.btn_start).setOnClickListener {
            while (count < (downUrlArray.size - 1)) {
                val url = downUrlArray[count]
                val downTask = DownTask.Builder()
                        //.id(UUID.randomUUID().toString().replace("-", ""))
                        .url(url)
                        .build()
                val downTasker = downManager?.newDownTasker(downTask)
                downTasker?.enqueue()
                count++
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        downManager?.downObserverable?.removeObserver(this)
    }

    override fun onProcess(tasker: DownTasker, process: Long, total: Long, present: Float) {
//        BKLog.d(tag, "*************************************")
//        BKLog.d(tag, "onProcess ${tasker.downTask.toString()}")
//        BKLog.d(tag, "process $process total$total present$present")
//        BKLog.d(tag, " ")
    }

    override fun onComplete(tasker: DownTasker) {
        BKLog.d(tag, "*************************************")
        BKLog.d(tag, "onComplete ${tasker.downTask.toString()}")
        BKLog.d(tag, " ")
    }

    override fun onError(tasker: DownTasker, typeError: DownErrorType) {
        BKLog.d(tag, "*************************************")
        BKLog.d(tag, "onError ${tasker.downTask.toString()}")
        BKLog.d(tag, " ")
    }
}
