package demo.xm.com.demo.down2

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

/**
 * 被观察接口
 */
class DownObserverable {
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
                    downObserverable.onComplete(tasker)
                }
                1 -> {
                    if (typeError == null) return
                    downObserverable.onError(tasker, typeError)
                }
                2 -> {
                    if (process > 0 && total > 0 && present > 0) {
                        downObserverable.onProcess(tasker, process, total, present)
                    }
                }
            }
        }
    }

    fun notifyObserverComplete(tasker: DownTasker) {
        notifyObserver(0, tasker)
    }

    fun notifyObserverError(tasker: DownTasker, typeError: DownErrorType) {
        notifyObserver(1, tasker, typeError)
    }

    fun notifyObserverProcess(tasker: DownTasker, process: Long, total: Long, present: Float) {
        notifyObserver(2, tasker, null, process, total, present)
    }
}