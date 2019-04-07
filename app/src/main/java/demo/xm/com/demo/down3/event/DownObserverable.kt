package demo.xm.com.demo.down3.event

import demo.xm.com.demo.down3.enum_.DownErrorType
import demo.xm.com.demo.down3.task.DownTasker
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class DownObservable {
    companion object {
        const val TAG = "DownObserverable"
    }

    private var queue: BlockingQueue<DownObserver>? = LinkedBlockingQueue<DownObserver>()

    @Synchronized
    fun registerObserver(o: DownObserver) {
        queue?.add(o)
    }

    @Synchronized
    fun removeObserver(o: DownObserver) {
        queue?.remove(o)
    }

    fun notifyObserver(type: Int, tasker: DownTasker? = null, typeError: DownErrorType? = null, process: Long = -1, total: Long = -1, present: Float = -1f) {
        if (tasker == null) return
        for (downObserverable in queue!!) {
            when (type) {
                0 -> {
                    //BKLog.e(TAG, "notifyObserverComplete total $total")
                    downObserverable.onComplete(tasker, total)
                }
                1 -> {
                    if (typeError == null) return
                    //BKLog.e(TAG, "notifyObserverError typeError $typeError")
                    downObserverable.onError(tasker, typeError)
                }
                2 -> {
                    if (process > 0 && total > 0 && present > 0) {
                        //BKLog.i(TAG, "notifyObserverProcess process $process total $total present $present")
                        downObserverable.onProcess(tasker, process, total, present)
                    }
                }
            }
        }
    }

    fun notifyObserverComplete(tasker: DownTasker?, total: Long) {
        notifyObserver(0, tasker, null, -1, total)
    }

    fun notifyObserverError(tasker: DownTasker?, typeError: DownErrorType) {
        notifyObserver(1, tasker, typeError)
    }

    fun notifyObserverProcess(tasker: DownTasker?, process: Long, total: Long, present: Float) {
        notifyObserver(2, tasker, null, process, total, present)
    }
}