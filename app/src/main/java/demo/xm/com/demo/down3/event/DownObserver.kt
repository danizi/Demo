package demo.xm.com.demo.down3.event

import demo.xm.com.demo.down3.enum_.DownErrorType
import demo.xm.com.demo.down3.task.DownTasker


interface DownObserver {
    fun onComplete(tasker: DownTasker, total: Long)
    fun onError(tasker: DownTasker, typeError: DownErrorType)
    fun onProcess(tasker: DownTasker, process: Long, total: Long, present: Float)
}