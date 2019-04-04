package demo.xm.com.demo.down2

import demo.xm.com.demo.down2.log.BKLog
import java.util.concurrent.*

/**
 * 下载调度器，管理下载任务，提供了下载监听，暂停，下载，添加任务，取消任务等职责
 * 生产者-消费者 并发队列 https://blog.csdn.net/qq_26676207/article/details/80844665 dfgfggsdfgs
 */
class DownDispatcher {
    companion object {
        const val tag = "DownDispatcher"
    }

    var pool: ThreadPoolExecutor? = null
    var poolCount: Int = 0
    var runingQueue: BlockingQueue<DownTasker>? = null
    var readyQueue: BlockingQueue<DownTasker>? = null

    private constructor()

    constructor(builder: Builder) {
        this.pool = builder.pool
        this.runingQueue = builder.runingQueue
        this.readyQueue = builder.readyQueue
        this.poolCount = builder.poolCount
    }

    fun enqueue(downTasker: DownTasker) {
        /*任务放入下载队列中*/
        if (runingQueue?.size!! < poolCount) {
            runingQueue?.add(downTasker)
            pool?.execute(downTasker.runnable)//放入线程池中
            BKLog.d(tag, "${downTasker.downTask?.url}添加到“运行”下载队列，并添加到线程池中。。。")
        } else {
            readyQueue?.add(downTasker)
            BKLog.d(tag, "${downTasker.downTask?.url}添加到“准备”下载队列。。。")
        }
    }

    fun finish(downTasker: DownTasker?) {
        /*任务下载完成*/
        if (runingQueue === null) return
        runingQueue?.remove(downTasker)//该任务从正在下载队列中移除
        BKLog.d(tag, "${downTasker?.downTask?.url}任务下载完成，将该任务从正在下载队列中移除。。。")
        if (runingQueue?.size!! > poolCount) return//正在下载队列长度大于限定数量，则不将任务往放入线程池执行
        for (tasker in readyQueue!!) {
            if (runingQueue?.size!! < poolCount) {
                runingQueue?.add(tasker)
                pool?.execute(tasker.runnable)
                readyQueue?.remove(tasker)
                BKLog.d(tag, "从准备队列中，提取${tasker?.downTask?.url}任务添加到下载runingQueue")
            }
        }
    }

    fun removeAll() {
        if (runingQueue?.isNotEmpty()!!) {
            for (run in runingQueue!!) {
                run.cancel()
                pool?.remove(run.runnable)
            }
        }
    }

    /**
     * 建造者
     */
    class Builder {
        var pool: ThreadPoolExecutor? = null//线程池
        var runingQueue: BlockingQueue<DownTasker>? = null//正在运行的队列
        var readyQueue: BlockingQueue<DownTasker>? = null//已经准备好的队列
        var poolCount = 0                               //队列最大下载的数量

        fun setPool(pool: ThreadPoolExecutor): Builder {
            this.pool = pool
            return this
        }

        fun setPoolCount(poolCount: Int): Builder {
            this.poolCount = poolCount
            return this
        }

        fun build(): DownDispatcher {
            if (runingQueue == null) {
                runingQueue = LinkedBlockingQueue<DownTasker>()
            }
            if (readyQueue == null) {
                readyQueue = LinkedBlockingQueue<DownTasker>()
            }
            if (poolCount == 0) {
                poolCount = 3
            }
            if (pool == null) {
                pool = ThreadPoolExecutor(poolCount, poolCount, 30, TimeUnit.SECONDS, ArrayBlockingQueue(2000))
            }
            return DownDispatcher(this)
        }

    }
}