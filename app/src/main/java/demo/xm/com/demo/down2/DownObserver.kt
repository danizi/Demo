package demo.xm.com.demo.down2

/**
 * 观察者接口
 */
interface DownObserver {
    fun onComplete(tasker: DownTasker, total: Long)
    fun onError(tasker: DownTasker, typeError: DownErrorType)
    fun onProcess(tasker: DownTasker, process: Long, total: Long, present: Float)
}