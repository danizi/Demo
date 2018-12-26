package demo.xm.com.demo

import android.Manifest
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.tbruyelle.rxpermissions2.RxPermissions
import demo.xm.com.demo.down.*


class MainActivity : AppCompatActivity() {

    private val tag: String = "MainActivity"
    private var btnStart: Button? = null
    private val urls: Array<String> = arrayOf(
            "https://download.tanwan.com/qsqsqdl/qsqsqdl_228785.apk",
            "http://gyxz.ukdj3d.cn/a31/yx_zh1/byzscq.apk",
            "http://bigota.d.miui.com/V10.1.3.0.OBGCNFI/miui_MI5SPlus_V10.1.3.0.OBGCNFI_fe85d745aa_8.0.zip",
            "http://bigota.d.miui.com/8.12.20/miui_MI5SPlus_8.12.20_ef1dda590f_8.0.zip"
    )
    val rxPermissions = RxPermissions(this) // where this is an Activity or Fragment instance
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnStart = findViewById(R.id.btn_start)
        btnStart?.setOnClickListener {
            rxPermissions
                    .request(Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .subscribe { granted ->
                        if (granted) {
                            // All requested permissions are granted
                            //开始下载任务
                            DownHelp.getInstance().start(DownRequst.Builder().url(urls[0]).build(), object : DownCallback {
                                override fun onProgress(call: DownCall?) {
                                    Log.d(tag, "------------------")
                                    Log.d(tag, "" + call?.dir + "-" + call?.fileName + "-" + call?.filetotal + "-" + call?.filetotal)
                                    Log.d(tag, "" + call?.process)
                                    Log.d(tag, "                  ")
                                }

                                override fun onFailure(call: DownCall?, e: Exception?) {
                                    Log.e(tag, e?.message)
                                }
                            })
                        } else {
                            // At least one permission is denied
                        }
                    }

        }
    }
}
