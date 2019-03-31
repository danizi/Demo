package demo.xm.com.demo.down2

import android.content.Context

/**
 * 下载
 */
class DownManager {
    var dispatcher: DownDispatcher? = null
    var downConfig: DownConfig? = null
    var downObserverable: DownObserverable? = null

    constructor(context: Context) {
        downConfig = DownConfig.Builder().ctx(context).build()
        dispatcher = DownDispatcher.Builder().build()
        downObserverable = DownObserverable()
        Builder()
                .setDispatcher(dispatcher)
                .setConfig(downConfig)
                .setDownObserverable(downObserverable)
                .build()
    }

    constructor(builder: Builder?) {
        dispatcher = builder?.dispatcher
        downConfig = builder?.downConfig
        downObserverable = builder?.downObserverable
    }

    fun newDownTasker(task: DownTask): DownTasker {
        /*创建一个下载者*/
        return DownTasker.Builder()
                .setDownManager(this)
                .setTask(task)
                .setConfig(downConfig)
                .build()
    }

    /**
     * 下载管理建造者
     */
    class Builder {
        var dispatcher: DownDispatcher? = null
        var downConfig: DownConfig? = null
        var downObserverable: DownObserverable? = null

        fun setDispatcher(dispatcher: DownDispatcher?): Builder {
            this.dispatcher = dispatcher
            return this
        }

        fun setConfig(downConfig: DownConfig?): Builder {
            this.downConfig = downConfig
            return this
        }

        fun setDownObserverable(downObserverable: DownObserverable?): Builder {
            this.downObserverable = downObserverable
            return this
        }

        fun build(): DownManager {
            return DownManager(this)
        }
    }
}