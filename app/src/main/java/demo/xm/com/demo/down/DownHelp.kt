package demo.xm.com.demo.down

/**
 * 多任务下载:
 * 1 可同时下载多个文件（PDF)
 * 2 可实时显示下载进度
 * 3 遇到错误可重试
 * 4 可暂停可恢复可取消
 * 5 可以获取当前下载的任务数
 * 6 多个任务以列表方式展现(ListView)
 * 7 可后台运行（比如：前台切换到其它APP,仍然能正常下载）
 * 8 能跑在2.3--4.2的安卓系统上
 * 9 需要源码交付，代码要规范、完整、注释清楚
 * 10 不能依赖系统下载服务(Download Manager)
 * 11 可随时切换http请求引擎 例如okHttp vollery 原生
 */
class DownHelp {
    /**
     * 下载客户端
     */
    private var downClient: DownClient? = null

    /**
     * 存储下载任务集合
     */
    private val downCalls: HashMap<String, DownCall>? = HashMap()

    init {
        downClient = DownClient()
    }

    /**
     * 开始下载
     */
    fun start( downRequst: DownRequst, downCallback: DownCallback?) {
        val downCall = downClient?.newCall(downRequst)
        if (downCall != null) {
            downCalls?.put(downRequst.url!!, downCall)
        }
        downCall?.enqueue(downCallback)
    }

    /**
     * 暂停
     */
    fun pause(vararg downRequst: DownRequst) {}

    /**
     * 恢复
     */
    fun resume(vararg downRequst: DownRequst) {}

    /**
     * 停止
     */
    fun cancel(vararg downRequst: DownRequst) {}

    /**
     * 单例
     */
    companion object {
        fun getInstance(): DownHelp {
            return DownHelp()
        }
    }
}