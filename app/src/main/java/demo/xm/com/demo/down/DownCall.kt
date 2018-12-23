package demo.xm.com.demo.down

import android.os.Environment
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.*

/**
 * 下载要求
 */
class DownCall(requst: DownRequst?) {

    private var downRequst: DownRequst? = null
    private var instance: DownCall? = null

    private var okHttpClient: OkHttpClient? = null
    private var request: Request? = null
    private var call: Call? = null

    init {
        this.downRequst = requst
        this.instance = this
        okHttpClient = OkHttpClient()
    }

    /**
     * 下载状态下载的进度
     */
    var state: DownState = DownState.DEFAULT

    /**
     * 文件路径
     */
    var dir: String? = null

    /**
     * 文件名称
     */
    var fileName: String? = null

    /**
     * 下载文件记录
     */
    var fileRecord: FileRecord? = null

    /**
     * 下载文件的总大小
     */
    var filetotal: Long? = 0L

    /**
     * 下载进度
     */
    var process: Float? = 0F

    /**
     * 任务的状态
     */
    enum class DownState {
        DEFAULT,   //未处理
        PAUSE,     //暂停
        CANCEL,    //取消
        RESUME,    //恢复
        PROCESSING,//下载中
        COMPLETE,  //完成下载
    }

    /**
     * 取消
     */
    fun cancel() {

    }

    fun enqueue(callback: DownCallback?) {
        request = Request.Builder().url(downRequst?.url).build()
        call = okHttpClient?.newCall(request)
        call?.enqueue(object : okhttp3.Callback {

            override fun onFailure(call: Call, e: IOException) {
                callback?.onFailure(call = instance, e = e)
            }

            override fun onResponse(call: Call, response: Response) {

                //支持断点下载
                if (response.code() == 206) {

                }

                if (response.code() == 200) {
                    //获取输入流
                    val bis = BufferedInputStream(response.body()?.byteStream())
                    val contentLength = response.body()?.contentLength()

                    //创建文件
                    if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
                        return
                    }
                    val fileName = /*downRequst?.url?.split("/")*/"123.apk"
                    val pathName = Environment.getExternalStorageDirectory().absolutePath + File.separator + fileName
                    val file = File(pathName)
                    file.createNewFile()

                    //数据写入文件当中
                    val bos = BufferedOutputStream(FileOutputStream(file))
                    var lenth = 0
                    val b = ByteArray(1024)
                    do {
                        lenth = bis.read(b)
                        if (lenth < 0)
                            break
                        bos.write(b)//数据写入文件
                        state = DownState.PROCESSING //下载状态
                        process = process?.plus(lenth)?.div(contentLength!!)//下载进度
                        callback?.onProgress(call = instance)
                    } while (true)

                    //下载完成
                    filetotal = file.length()
                    process = 1f
                    state = DownState.COMPLETE
                    callback?.onProgress(call = instance)

                    //关闭流
                    bos.close()
                    bis.close()
                }
            }
        })
    }
}