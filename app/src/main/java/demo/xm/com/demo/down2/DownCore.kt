package demo.xm.com.demo.down2

import android.os.Environment
import demo.xm.com.demo.down2.log.BKLog
import demo.xm.com.demo.down2.utils.CommonUtil
import demo.xm.com.demo.down2.utils.FileUtil
import demo.xm.com.demo.down2.utils.OnWriteProcess
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class DownCore(private var builder: DownCore.Builder?) : Runnable {

    private val TAG = this.javaClass.simpleName

    /**
     * 下载状态
     */
    var downState: Boolean? = null
    /**
     * 下载进度单位字节（B）
     */
    var process: Long = 0

    init {
        downState = false
    }

    override fun run() {
        val startTime = System.currentTimeMillis()
        val url = URL(builder?.downTask?.url)
        val urlConn = url.openConnection() as HttpURLConnection //建立连接
        urlConn.requestMethod = "GET"                           //请求方法
        urlConn.doInput = true                                  //打开获取输入流权限
        urlConn.connectTimeout = 600000                         //设置连接超时时间
        urlConn.readTimeout = 600000                            //设置读取超时时间
        addRangeRequestProperty(urlConn)                        //添加请求头 ps:setRequestProperty必须在获取资源数据之前，即获取inputStream 、contentLength之前
        val inputStream = urlConn.inputStream                   //获取输入流，读取资源数据
        val total = urlConn.contentLength                       //获取资源数据的大小

        //判断请求是否成功
        if (urlConn.responseCode != 200 && urlConn.responseCode != 206) {
            BKLog.e(TAG, "请求不成功，状态码：" + urlConn.responseCode)
            return
        }

        //检查文件权限 PS:6.0以上版本可能出现声明权限，但是报未定义权限错误，暂时还没有解决。
        if (!FileUtil.filePermission()) {
            BKLog.e(TAG, "请检查Android文件权限，在“Android6.0以上”需要申请动态权限")
            return
        }

        //创建临时文件占位
        val path = Environment.getExternalStorageDirectory().canonicalPath
        val tempDir = builder?.dir + File.separator + CommonUtil.getFileName(builder?.downTask?.url) + "_Temp"
        val tempFileName = builder?.threadNamePrefix + builder?.segment + builder?.tempSuffix
        val tempFile = FileUtil.createNewFile(path, tempDir, tempFileName)

        //数据缓存到本地
        builder?.startIndex = 0
        FileUtil.write(inputStream, tempFile, builder?.buffer, builder?.startIndex!!, builder?.endIndex!!, object : OnWriteProcess {
            override fun onProcess(process: Long, total: Long) {
                this@DownCore.process += process
                BKLog.d(TAG, CommonUtil.getFileName(builder?.downTask?.url) + " - $tempFileName" + "下载字节：$process" + "分段文件总大小：$total")
            }
        })

        //下载完成状态
        downState = true

        //打印下载总耗时
        val endTime = System.currentTimeMillis()
        BKLog.d(TAG, CommonUtil.getFileName(builder?.downTask?.url) + " - " + tempFileName + "下载完成，总耗时：" + (endTime - startTime) / 1000)
    }

    private fun addRangeRequestProperty(urlConn: HttpURLConnection?) {
        /*
         * 支持断点下载
         * ange 请求头格式
         *                  Range: bytes=start-end
         * 例如：
         *      Range: bytes=10-     ：第10个字节及最后个字节的数据
         *      Range: bytes=40-100  ：第40个字节到第100个字节之间的数据.
         */
        val endIndex = builder?.endIndex
        val startIndex = builder?.startIndex
        val segment = builder?.segment
        if (endIndex == (-1).toLong()) {
            urlConn?.setRequestProperty("Range", "bytes=$startIndex-")
        } else {
            urlConn?.setRequestProperty("Range", "bytes=$startIndex-$endIndex")
        }
        BKLog.d(TAG, "*****************************")
        BKLog.d(TAG, "Thread-Segment-$segment startIndex:$startIndex endIndex:$endIndex")
        BKLog.d(TAG, "")
    }

    class Builder {
        var dir: String? = ""           //下载路径
        var segment: Int = 0            //当前是第几个分段下载的线程
        var downTask: DownTask? = null  //下载任务信息
        var startIndex: Long = 0        //下载数据的起点
        var endIndex: Long = 0          //下载数据的终点
        var buffer: ByteArray? = null   //缓存的字节数组
        var tempSuffix: String? = ""    //临时文件的后缀
        var threadNamePrefix: String? = ""//线程名称的前缀

        fun build(): DownCore {
            return DownCore(this)
        }
    }
}