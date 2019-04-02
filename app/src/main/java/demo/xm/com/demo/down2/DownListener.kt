package demo.xm.com.demo.down2

/**
 * 下载任务监听
 */
@Deprecated("改用观察者模式，来处理")
interface DownListener {
    fun onProcess(tasker: DownTasker, process: Long, total: Long, present: Float)
    fun onComplete(tasker: DownTasker)
    fun onError(tasker: DownTasker, typeError: DownErrorType)
}