package demo.xm.com.demo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ListView
import android.widget.SimpleAdapter
import demo.xm.com.demo.down2.*
import demo.xm.com.demo.down2.log.BKLog

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val lv: ListView = findViewById(R.id.lv)

        // 初始化下载器
        val builder = DownDispatcher.DownDispatcherBuilder()
        builder.ctx = this
        val dispatcher = builder.builder()
        dispatcher.setOnDownListener(object : OnDownListener {
            override fun onProcess(downTasker: DownTasker, process: Long) {

            }

            override fun onComplete(downTasker: DownTasker) {

            }

            override fun onError(downTasker: DownTasker, typeError: DownTypeError) {

            }
        })
        var downIndex = 0
        val downUrlArray = arrayOf(
                "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1553109541840&di=c8fdea99440e6fc75fd65334be792ed1&imgtype=0&src=http%3A%2F%2Fupload.17350.com%2F2019%2F0313%2F20190313091848993.jpg",
                "http://gyxz.ukdj3d.cn/hk/yx_lf1/gxnsmnq.apk",
                "http://down.s.qq.com/download/1106467070/apk/10018365_com.tencent.tmgp.pubgmhd.apk",
                "http://gyxz.ukdj3d.cn/yq1/yx_yjk1/wdsj163.apk",
                "http://gyxz.ukdj3d.cn/yx/yx_wwq1/NBA2K18.apk",
                "https://dl.hz.37.com.cn/upload/1_1002822_10664/shikongzhiyiH5_10664.apk",
                "https://cavedl.leiting.com/full/caveonline_M141859.apk",
                "http://gyxz.ukdj3d.cn/vp/yx_sw1/warsong.apk",
                "http://gyxz.ukdj3d.cn/vp1/yx_ljun1/Pokemmo.apk",
                "http://gyxz.ukdj3d.cn/a31/rj_xgd1/mangguotv.apk",
                "https://s.click.taobao.com/QBOz9Lw",
                "http://gyxz.ukdj3d.cn/a31/rj_xgd1/wangyiyoudaocidian.apk",
                "http://down.s.qq.com/download/1106467070/apk/10018365_com.tencent.tmgp.pubgmhd.apk",
                "http://img1.imgtn.bdimg.com/it/u=2735633715,2749454924&fm=26&gp=0.jpg"
        )
        // 构建下载UI界面
        val action = arrayOf(
                "添加任务",
                "指定任务开始下载",
                "指定任务暂停下载",
                "指定任务取消下载",
                "所有下载任务开始",
                "所有下载任务暂停",
                "所有下载任务取消"
        )

        val data = ArrayList<Map<String, String>>()
        for (a in action) {
            val map = HashMap<String, String>()
            map["action"] = a
            data.add(map)
        }
        lv.adapter = SimpleAdapter(this, data, android.R.layout.simple_list_item_1, arrayOf("action"), IntArray(android.R.id.text1))
        lv.setOnItemClickListener { parent, view, position, id ->
            when (action[position]) {
                "添加任务" -> {
                    val downTask = DownTask()
                    val index = downIndex++
                    downTask.url = downUrlArray[index]
                    downTask.id = downTask.url
                    BKLog.d("当前任务数" + index + "添加下载任务：" + downTask.url)
                    dispatcher.addTask(downTask)
                }
                "指定任务开始下载" -> {
                    dispatcher.startTask(downUrlArray[0])
                }
                "指定任务暂停下载" -> {
                    dispatcher.pauseTask(downUrlArray[0])
                }
                "指定任务取消下载" -> {
                    dispatcher.cancelTask(downUrlArray[0])
                }
                "所有下载任务开始" -> {
                    dispatcher.startAllTask()
                }
                "所有下载任务暂停" -> {
                    dispatcher.pauseAllTask()
                }
                "所有下载任务取消" -> {
                    dispatcher.cancelAllTask()
                }
            }
        }
    }
}
