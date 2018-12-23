package demo.xm.com.demo.down

/**
 * 下载的监听接口
 */
interface DownCallback {
    fun onProgress(call: DownCall?)
    fun onFailure(call: DownCall?, e: Exception?)
}