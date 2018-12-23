package demo.xm.com.demo.down

/**
 * 下载客服端配置信息类
 */
class DownConfige(builder: Builder?) {
    var dir: String? = builder?.getDir()
    var taskNum: Int? = builder?.getTaskNum()
    var retryNum: Int? = builder?.getRetryNum()
    var debug: Boolean? = builder?.getDebug()

    class Builder {
        private var dir: String? = null
        private var taskNum: Int? = null
        private var retryNum: Int? = null
        private var debug: Boolean? = null

        /**
         * 配置下载路径
         */
        fun dir(dir: String?): Builder {
            this.dir = dir
            return this
        }

        fun getDir(): String? {
            return this.dir
        }

        /**
         * 下载的任务数量
         */
        fun taskNum(taskNum: Int): Builder {
            this.taskNum = taskNum
            return this
        }

        fun getTaskNum(): Int? {
            return this.taskNum
        }

        /**
         * 下载失败重试的数量
         */
        fun retryNum(retryNum: Int): Builder {
            this.retryNum = retryNum
            return this
        }

        fun getRetryNum(): Int? {
            return this.retryNum
        }

        /**
         * debug模式
         */
        fun debug(debug: Boolean): Builder {
            this.debug = debug
            return this
        }

        fun getDebug(): Boolean? {
            return this.debug
        }

        /**
         * 构建下载配置类
         */
        fun build(): DownConfige? {
            return DownConfige(Builder())
        }
    }
}