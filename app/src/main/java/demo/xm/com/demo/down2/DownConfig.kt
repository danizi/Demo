package demo.xm.com.demo.down2

import android.content.Context
import android.os.Environment
import android.text.TextUtils
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 配置信息
 */
class DownConfig {

    var multiplePool: ThreadPoolExecutor? = null
    var connectTimeout = 0
    var readTimeout = 0
    var path: String? = ""
    var dir: String? = ""
    var buffer: ByteArray? = null
    var tempSuffix: String = ""
    var suffix: String = ""
    var threadNamePrefix: String = ""
    var maxMultipleThreadNum = 1
    var ctx: Context? = null

    constructor() {
        Builder().build()
    }

    constructor(b: Builder) {
        ctx = b.ctx
        multiplePool = b.multiplePool
        connectTimeout = b.connectTimeout
        readTimeout = b.readTimeout
        path = b.path
        dir = b.dir
        tempSuffix = b.tempSuffix
        suffix = b.suffix
        threadNamePrefix = b.threadNamePrefix
        maxMultipleThreadNum = b.maxMultipleThreadNum
        buffer = b.buffer
    }

    /**
     * 建造者
     */
    class Builder {
        var ctx: Context? = null
        /* 线程池 */
        var maxMultipleThreadNum = 0
        var multiplePool: ThreadPoolExecutor? = null  // 下载器线程池
        var connectTimeout = 0                        // 设置连接超时时间
        var readTimeout = 0                           // 设置读取超时时间

        /* 文件格式
         * 下载“文件”命名          ：path/dir/xxxx.xx
         * 下载临时“目录”命名格式  ：path/dir/xxxx.xx_Temp
         * 下载临时“文件”命名格式  ：path/dir/xxxx.xx_Temp/Thread_segment_1.temp
         *                             path/dir/xxxx.xx_Temp/Thread_segment_2.temp
         *                             ...
         *
         * 例如：下载地址是http://gyxz.ukdj3d.cn/vp/yx_sw1/warsong.apk，下载配置目录是xmDown，配置三个线程来下载，文件结构如下：
         * 下载“文件”命名         ：/storage/sdcard/xm/xmDown/warsong.apk
         * 下载临时“目录”命名格式 ：path/dir/warsong.apk_Temp
         * 下载临时“文件”命名格式 ：path/dir/warsong.apk_Temp/Thread_segment_1.temp
         *                            path/dir/warsong.apk_Temp/Thread_segment_2.temp
         *                            path/dir/warsong.apk_Temp/Thread_segment_3.temp
         */
        var path: String? = ""// 文件下载路径
        var dir: String? = ""                                 // 下载文件的父目录
        var buffer: ByteArray? = null                         // 缓存字节大小
        var tempSuffix: String = ""                           // 临时文件的后缀名称

        /* 部分链接的地址是不带后缀名称的，那么就临时给一个名称
         * 例如：http//:wwwwwdsfasfdsda
         */
        var suffix: String = ""

        /* 线程命名格式
         * threadNameprefix+segment
         *
         * 例如：使用三个线程下载任务，名称分别如下
         * Thread_segment_0
         * Thread_segment_1
         * Thread_segment_2
         */
        var threadNamePrefix: String = ""

        fun ctx(context: Context): Builder {
            this.ctx = context
            return this
        }

        fun maxMultipleThreadNum(maxMultipleThreadNum: Int): Builder {
            this.maxMultipleThreadNum = maxMultipleThreadNum
            return this
        }

        fun multiplePool(multiplePool: ThreadPoolExecutor): Builder {
            this.multiplePool = multiplePool
            return this
        }

        fun connectTimeout(connectTimeout: Int): Builder {
            this.connectTimeout = connectTimeout
            return this
        }

        fun readTimeout(readTimeout: Int): Builder {
            this.readTimeout = readTimeout
            return this
        }

        fun path(path: String): Builder {
            this.path = path
            return this
        }

        fun dir(dir: String): Builder {
            this.dir = dir
            return this
        }

        fun buffer(buffer: ByteArray): Builder {
            this.buffer = buffer
            return this
        }

        fun tempSuffix(tempSuffix: String): Builder {
            this.tempSuffix = tempSuffix
            return this
        }

        fun suffix(suffix: String): Builder {
            this.suffix = suffix
            return this
        }

        fun threadNamePrefix(threadNamePrefix: String): Builder {
            this.threadNamePrefix = threadNamePrefix
            return this
        }

        fun build(): DownConfig {
            if (maxMultipleThreadNum == 0) {
                maxMultipleThreadNum = 1
            }
            if (multiplePool == null) {
                multiplePool = ThreadPoolExecutor(maxMultipleThreadNum, maxMultipleThreadNum, 30, TimeUnit.SECONDS, ArrayBlockingQueue(2000))
            }
            if (connectTimeout == 0) {
                connectTimeout = 60000
            }
            if (readTimeout == 0) {
                readTimeout = 60000
            }
            if (TextUtils.isEmpty(path)) {
                path = Environment.getExternalStorageDirectory().canonicalPath
            }
            if (TextUtils.isEmpty(dir)) {
                dir = "xmDown"
            }
            if (buffer == null) {
                buffer = ByteArray(1024 * 1024 * 4)
            }
            if (TextUtils.isEmpty(tempSuffix)) {
                tempSuffix = ".temp"
            }
            if (TextUtils.isEmpty(suffix)) {
                tempSuffix = ".xm"
            }
            if (TextUtils.isEmpty(threadNamePrefix)) {
                threadNamePrefix = "Thread_segment_x"
            }
            return DownConfig(this)
        }
    }
}
