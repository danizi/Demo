package demo.xm.com.demo.down

/**
 * 下载的任务
 */
class DownRequst(builder: Builder) {
    var url: String? = builder.url

    class Builder {
        var url: String? = null
        fun url(url: String?): Builder {
            this.url = url
            return this
        }

        fun build(): DownRequst {
            return DownRequst(this)
        }
    }
}