package demo.xm.com.demo.down3.dispatcher

import demo.xm.com.demo.down2.DownDispatcher
import demo.xm.com.demo.down2.log.BKLog
import demo.xm.com.demo.down3.config.DownConfig.Companion.DEFAULT
import demo.xm.com.demo.down3.task.DownTasker
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ExecutorService

class DownDispatcher {

    var pool: ExecutorService? = null
    var runqueues = DEFAULT
    var runningQueue: BlockingQueue<DownTasker>? = null
    var readyQueue: BlockingQueue<DownTasker>? = null

    fun enqueue(tasker: DownTasker) {
        /*任务添加到队列*/
        if (runningQueue?.size!! < runqueues) {
            runningQueue?.add(tasker)
            pool?.submit(tasker.runnable)
            BKLog.d(DownDispatcher.tag, "添加到“运行”队列 -> ${tasker.task.name}添加到“运行”下载队列，并添加到线程池中。。。")
        } else {
            readyQueue?.add(tasker)
            BKLog.d(DownDispatcher.tag, "添加到“准备”下载队列 -> ${tasker.task.name}")
        }
    }

    fun finish(tasker: DownTasker?) {
        /*任务下载完成*/
        runningQueue?.remove(tasker)//该任务从正在下载队列中移除
        BKLog.d(DownDispatcher.tag, "${tasker?.task?.name} 下载完成，runningQueue队列中移除")
        //移除后判断队列数是否小于指定队列数
        val runningQueueSize = runningQueue?.size!!
        if (runningQueueSize > runqueues) {
            return
        }
        for (tasker in readyQueue!!) {
            if (runningQueueSize < runqueues) {
                pool?.execute(tasker.runnable)
                runningQueue?.add(tasker)    //添加到运行队列
                readyQueue?.remove(tasker)   //移除准备队列
                //BKLog.d(DownDispatcher.tag, "从readyQueue中取出${tasker?.task?.name}任务，添加到下载runningQueue")
                BKLog.d(DownDispatcher.tag, "从readyQueue中取出${tasker.runnable?.name}任务，添加到下载runningQueue")
            }
        }
    }

    fun remove(tasker: DownTasker) {
        /*移除队列*/
        runningQueue?.remove(tasker)
        readyQueue?.remove(tasker)
    }
}