package demo.xm.com.demo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.widget.Button
import demo.xm.com.demo.down3.test.XmDownTest

class DownMainActivity : AppCompatActivity() {

    private var rv: RecyclerView? = null
    private var btnAdd: Button? = null
    private var xmDownTest: XmDownTest? = null
    private var count = 0
    private val downUrlArray = arrayOf(
            "http://img1.imgtn.bdimg.com/it/u=2735633715,2749454924&fm=26&gp=0.jpg",
            "http://img4.imgtn.bdimg.com/it/u=3590849871,3724521821&fm=26&gp=0.jpg",
            "http://img5.imgtn.bdimg.com/it/u=4060543606,3642835235&fm=26&gp=0.jpg",
            "http://img0.imgtn.bdimg.com/it/u=3282593745,642847689&fm=26&gp=0.jpg",
            "https://apk.apk.xgdown.com/down/1hd.apk",
            "https://dl.hz.37.com.cn/upload/1_1002822_10664/shikongzhiyiH5_10664.apk"

    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViews()
        iniData()
        initEvent()
        initDisplay()
    }

    private fun findViews() {
        rv = findViewById(R.id.rv)
        btnAdd = findViewById(R.id.btn_add)
    }

    private fun iniData() {
        xmDownTest = XmDownTest(this)
    }


    private fun initEvent() {
        btnAdd?.setOnClickListener {
            if (count < downUrlArray.size) {
                xmDownTest?.add(downUrlArray[count])
                count++
            }
        }
    }

    private fun initDisplay() {
        xmDownTest?.bindRv(rv)
        xmDownTest?.initDisplay()
    }

    override fun onDestroy() {
        super.onDestroy()
        xmDownTest?.exit()
    }
}

