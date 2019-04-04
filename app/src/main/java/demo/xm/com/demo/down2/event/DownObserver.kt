package demo.xm.com.demo.down2.event

import demo.xm.com.demo.down2.DownErrorType
import demo.xm.com.demo.down2.DownTasker

/**
 * 观察者接口
 */
interface DownObserver {
    fun onComplete(tasker: DownTasker, total: Long)
    fun onError(tasker: DownTasker, typeError: DownErrorType)
    fun onProcess(tasker: DownTasker, process: Long, total: Long, present: Float)
}