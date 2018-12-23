package demo.xm.com.demo.down

/**
 * 下载客户端
 */
class DownClient {
    var downConfige: DownConfige? = null
    var httpCore: IHttpCore? = null

    init {
        downConfige = DownConfige.Builder()
                .dir("")
                .retryNum(1)
                .taskNum(3)
                .debug(false)
                .build()
    }

    fun newCall(requst: DownRequst?): DownCall? {
        return DownCall(requst)
    }
}