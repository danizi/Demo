package demo.xm.com.demo.down2.utils

import android.text.TextUtils
import demo.xm.com.demo.down2.log.BKLog

object CommonUtil {
    private val TAG = this.javaClass.simpleName

    fun getFileName(url: String?): String {
        return "xm.jpg"
        var fileName = "down.xm"
        if (TextUtils.isEmpty(url)) {
            BKLog.e(TAG,"下载地址为null")
            return fileName
        }
        val s = url?.split("/")

        if (s != null) {
            fileName = s[s.size - 1]
        }
        return fileName
    }

    /**
     * 写入文件
     */
    fun getSize(num: Long): Float {
        val unit = "M"
        return when (unit) {
            "KB" -> {
                num / 1024f
            }
            "M" -> {
                num / 1024f / 1024f
            }
            "G" -> {
                num / 1024f / 1024f
            }
            else -> {
                throw Throwable("输入单位错误")
            }
        }
    }
}