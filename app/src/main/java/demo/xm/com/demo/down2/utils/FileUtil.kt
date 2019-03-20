package demo.xm.com.demo.down2.utils

import android.content.Context
import android.os.Environment
import android.text.TextUtils
import demo.xm.com.demo.down2.log.BKLog
import java.io.*
import java.net.HttpURLConnection
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.text.DecimalFormat

/**
 * android 文件操作工具类
 * 文件的操作模式四种
 * MODE_PRIVATE          默认操作模式，文件私有，只能本应用访问，写入内容会覆盖原文件内容
 * MODE_APPEND           会检查文件是否存在，存在则追加，否则建立新的文件
 * MODE_WORLD_READABLE   可被其他应用读取
 * MODE_WORLD_WRITEABLE  可悲其他应用写入
 *
 * ps:若文件既要可读可写就可以用以下形式
 *    OpenFileOutput("test.txt",MODE_WORLD_READABLE+MODE_WORLD_WRITEABLE)
 *
 *
 * API
 * openFileOutput(filename,mode)   打开输出流，写入字节流数据
 * openFileInout(filename)         打开输出流，读取字节流数据
 * getDir(name,mode)               在app的data目录获取或者创建name对应的子目录
 * getFileDir()                    获取app的data目录file的绝对路径
 * fileList()                      返回app的data目录下的全部文件
 * deleteFile(filename)            删除app的data目录下的指定文件
 *
 * ps:sharedpreferences,数据库都是私有的，除非开放权限，才能被外部应用访问。data/data/<包名>/file
 *
 * 读取SD卡上面的文件
 * 操作步骤
 * 1 读写判断sd卡是否插入,且读写                                Environment.getExternalStorageState.equals(Environment.MEDIA_MOUNTED)
 * 2 获取sd卡的外部目录                                         Environment.getExternalStorageDirectory().getCanonicalPath()
 * 3 使用FileOutputStream FileInputStream进行SD卡的读写操作     mmt\sdcard 或者 /storage
 * 4 添加权限
 *
 * ps: 1 在android6.0以上就算添加了操作文件的权限还是报没有权限的错误。使用动态获取权限的开源库也还是出错。
 *     2 在android某些机型，创建文件必须添加创建目录（如果创建失败，请尝试一级目录一级目录创建），然后再创建文件。
 *
 *
 * RandomAccessFile简述
 * 没有继承字节流字符流家中的任何一个类，它只实现了DataInput DataOutput这两个接口，意味着它能读能写。并且它可以“自由访问文件的任意位置”如果需要读取文件的一部分内容，它无疑是最好的选择。
 * long getFilePointer()：返回文件记录指针的当前位置
 * void seek(long pos)  ：将文件记录指针定位到pos位置
 *
 * 读取raw和assets文件夹下的文件
 * 菜鸟学院 IO流 http://www.runoob.com/java/java-files-io.html
 */
object FileUtil {

    private val TAG = this.javaClass.simpleName

    /**
     * 写入数据到本地不提供记录功能
     */
    @Deprecated("下载效率低下，并且不支持断点下载")
    fun writeToLocal(fileName: String, conn: HttpURLConnection?) {
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            BKLog.e("请检查sd卡是否插入且具备读写权限")
            return
        }
        /* 1 获取网络资源相关*/
        //下载文件总大小
        val downSize = conn?.contentLength
        //获取网络输入流
        val inputStream = conn?.inputStream
        //判断获取网络资源是否成功
        if (downSize!! < 0) {
            BKLog.e("获取大小失败，请检查网络或者下载url")
            return
        }

        /* 2 本地文件相关操作*/
        val downDir = ""
        val path = Environment.getExternalStorageDirectory().canonicalPath
        val dirFile = File(path + File.separator + downDir)
        val downFile = File(path + File.separator + downDir + File.separator + fileName)
        //首先检查目录是否存在，不存在先创建目录
        if (!dirFile.exists()) {
            if (dirFile.mkdirs()) {
                //创建目录成功，在检查文件是否存在
                if (!downFile.exists()) {
                    BKLog.d(downFile.absolutePath + "创建状态：" + downFile.createNewFile())
                } else {
                    BKLog.d(downFile.absolutePath + "存在，" + "文件大小：" + downFile.totalSpace + "单位(字节)")
                }
            } else {
                BKLog.e("创建下载文件目录失败，请检查当前Android版本")
            }
        }

        /* 3 读写操作*/
        val currentTime = System.currentTimeMillis()
        var length: Int
        val buffer = ByteArray(1024 * 1024)
        var process = 0f
        val bis = BufferedInputStream(inputStream)
        val bos = BufferedOutputStream(FileOutputStream(downFile))
        while (true) {
            length = bis.read(buffer)
            if (length == -1)
                break

            bos.write(buffer, 0, length)
            if (downSize > 0) {
                process += CommonUtil.getSize(length.toLong()) / CommonUtil.getSize(downSize.toLong())//下载进度
                //BKLog.d("Current:" + getSize(length.toLong()) + "M" + " Total:" + getSize(downSize.toLong()) + "M")
                BKLog.d("下载进度：$process")
            }
        }
        bos.close()
        bis.close()
        BKLog.d("下载" + downFile.absolutePath + "文件成功" + " 总耗时：" + ((System.currentTimeMillis() - currentTime / 1000) % 60) + "s")
    }

    /**
     * 指定文件位置读写，支持断点下载，退出客户端没有保存状态
     */
    @Deprecated("过时")
    fun writeToLocal2(fileName: String, conn: HttpURLConnection?, startIndex: Long, endIndex: Long) {
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            BKLog.e("请检查sd卡是否插入且具备读写权限")
            return
        }
        /* 1 获取网络资源相关*/
        //支持断点下载
        /* ange 请求头格式
         *                  Range: bytes=start-end
         * 例如：
         *      Range: bytes=10-     ：第10个字节及最后个字节的数据
         *      Range: bytes=40-100  ：第40个字节到第100个字节之间的数据.
         */
        if (endIndex == (-1).toLong()) {
            conn?.setRequestProperty("Range", "bytes=$startIndex-")
        } else {
            conn?.setRequestProperty("Range", "bytes=$startIndex-$endIndex")
        }
        //下载文件总大小
        val downSize = conn?.contentLength
        //获取网络输入流
        val inputStream = conn?.inputStream
        //判断获取网络资源是否成功
        if (downSize!! < 0) {
            BKLog.e("获取大小失败，请检查网络或者下载url")
            return
        }

        /* 2 本地文件相关操作*/
        val downDir = ""
        val path = Environment.getExternalStorageDirectory().canonicalPath
        val dirFile = File(path + File.separator + downDir)
        val downFile = File(path + File.separator + downDir + File.separator + fileName)
        //首先检查目录是否存在，不存在先创建目录
        if (!dirFile.exists()) {
            if (dirFile.mkdirs()) {
                //创建目录成功，在检查文件是否存在
                if (!downFile.exists()) {
                    BKLog.d(downFile.absolutePath + "创建状态：" + downFile.createNewFile())
                } else {
                    BKLog.d(downFile.absolutePath + "存在，" + "文件大小：" + downFile.length() + "单位(字节)")
                }
            } else {
                BKLog.e("创建下载文件目录失败，请检查当前Android版本")
            }
        }

        /* 3 读写操作*/
        val currentTime = System.currentTimeMillis()
        var length: Int
        val buffer = ByteArray(1024 * 1024 * 4)
        var process = (startIndex.toFloat() / CommonUtil.getSize(downSize.toLong()))
        val bis = BufferedInputStream(inputStream)
        val raf = RandomAccessFile(downFile, "rwd")
        raf.seek(startIndex)
        while (true) {
            length = bis.read(buffer)
            if (length == -1)
                break

            raf.write(buffer, 0, length)
            if (downSize > 0) {
                process += CommonUtil.getSize(length.toLong()) / CommonUtil.getSize(downSize.toLong())//下载进度
                //BKLog.d("Current:" + getSize(length.toLong()) + "M" + " Total:" + getSize(downSize.toLong()) + "M")
                BKLog.d("下载进度：$process")
            }
        }
        raf.close()
        bis.close()
        BKLog.d("下载" + downFile.absolutePath + "文件成功" + " 总耗时：" + ((System.currentTimeMillis() - currentTime / 1000)) + "s")
    }

    @Deprecated("过时", ReplaceWith("writeToLocal2(fileName, conn, startIndex, -1)", "demo.xm.com.demo.down2.utils.FileUtil.writeToLocal2"))
    fun writeToLocal2(fileName: String, conn: HttpURLConnection?, startIndex: Long) {
        writeToLocal2(fileName, conn, startIndex, -1)
    }

    /**
     * sd卡空间总大小
     */
    fun getTotalSpace(): Long {
        if (filePermission()) {
            return Environment.getExternalStorageDirectory().totalSpace
        }
        return -1
    }

    /**
     * sd卡可用空间大小
     */
    fun getUsableSpace(context: Context?): Long {
        if (filePermission()) {
            return Environment.getExternalStorageDirectory().usableSpace
        }
        return -1
    }

    /**
     * 获取字节转换成KB MB GB 单位的字符串
     */
    fun getSizeUnit(var0: Long): String {
        val var2 = DecimalFormat("###.00")
        return when {
            var0 < 1024L -> (var0).toString() + "bytes"
            var0 < 1048576L -> var2.format((var0.toFloat() / 1024.0f).toDouble()) + "KB"
            else -> when {
                var0 < 1073741824L -> var2.format((var0.toFloat() / 1024.0f / 1024.0f).toDouble()) + "MB"
                var0 > 0L -> var2.format((var0.toFloat() / 1024.0f / 1024.0f / 1024.0f).toDouble()) + "GB"
                else -> "error"
            }
        }
    }


    /**
     * 判断Android是否具备文件读写权限
     */
    fun filePermission(): Boolean {
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            BKLog.e("请检查sd卡是否插入且具备读写权限")
            return false
        }
        return true
    }

    /**
     * 创建文件
     */
    fun createNewFile(path: String?, dir: String?, fileName: String?): File {
        val file = File(path + File.separator + dir + File.separator + fileName)
        if (mkdirs(path, dir)) {
            if (!file.exists()) {
                BKLog.d(TAG, file.absolutePath + "文件创建状态：" + file.createNewFile())
            } else {
                BKLog.d(TAG, file.absolutePath + "文件已存在，文件大小：" + file.length() + "B")
            }
        }
        return file
    }

    /**
     * 创建目录
     */
    fun mkdirs(path: String?, dir: String?): Boolean {
        if (TextUtils.isEmpty(path)) {
            return false
        }
        if (TextUtils.isEmpty(dir)) {
            return false
        }
        val dirFile = File(path + File.separator + dir)
        val mkdirsState: Boolean
        if (!dirFile.exists()) {
            mkdirsState = dirFile.mkdirs()
            if (mkdirsState) {
                BKLog.d(TAG, dirFile.absolutePath + "创建目录成功")
            } else {
                BKLog.e(TAG, dirFile.absolutePath + "创建目录失败")
            }
        } else {
            BKLog.d(TAG, dirFile.absolutePath + "目录已存在")
            mkdirsState = true
        }

        return mkdirsState
    }

    /**
     * 将网络数据缓存到本地
     * @inputStream 网络的输入流
     * @file         缓存本地的文件
     * @buffer       缓存字节
     * @startIndex  下载文件的起始点
     * @endIndex    下载文件的终点
     * @listener    下载字节监听
     */
    fun write(inputStream: InputStream?, file: File?, buffer: ByteArray?, startIndex: Long, endIndex: Long, listener: OnWriteProcess?) {
        var bis: BufferedInputStream? = null
        var raf: RandomAccessFile? = null
        try {
            var length: Int
            bis = BufferedInputStream(inputStream)
            raf = RandomAccessFile(file, "rwd")
            raf.seek(startIndex)
            while (true) {
                length = bis.read(buffer)
                if (length == -1)
                    break
                raf.write(buffer, 0, length)
                listener?.onProcess(length.toLong(), endIndex)
            }
            raf.close()
            bis.close()
        } catch (e: Exception) {
            BKLog.e("缓存网络资源失败!!!")
            e.printStackTrace()
        } finally {
            raf?.close()
            bis?.close()
        }
    }

    /**
     * 获取本地是否存在缓存文件，存在返回文件大小，否则返回0
     */
    fun getStartIndex(fileName: String): Long {
        val downDir = ""
        val path = Environment.getExternalStorageDirectory().canonicalPath
        val downFile = File(path + File.separator + downDir + File.separator + fileName)
        if (downFile.exists()) {
            val size = downFile.length()
            BKLog.d(downFile.absolutePath + "当前文件大小:" + size + "StartIndex = 当前文件大小 + 1")
            return size + 1
        }
        return 0
    }

    /**
     * 文件合并
     */
    fun mergeFiles(outFile: File, inFile: File) {
        val BUFSIZE = 1024 * 8
        BKLog.d(TAG, "Merge " + inFile.absolutePath + "目录下的所有子文件，into " + "" + outFile.absolutePath)
        val outChannel = FileOutputStream(outFile).channel
        var fc: FileChannel? = null
        for (subFile in inFile.listFiles()) {  //获取inFile下的所有文件
            val charset = Charset.forName("utf-8")
            val dCoder = charset.newDecoder()
            val eCoder = charset.newEncoder()
            fc = FileInputStream(subFile).channel
            val bb = ByteBuffer.allocate(BUFSIZE)
            val charBuffer = dCoder.decode(bb)
            val nbuBuffer = eCoder.encode(charBuffer)
            while (fc.read(nbuBuffer) != -1) {
                bb.flip()
                nbuBuffer.flip()
                outChannel.write(nbuBuffer)
                bb.clear()
                nbuBuffer.clear()
            }
        }
        fc?.close()
        outChannel.close()
    }

    /**
     * 递归删除文件
     */
    fun del(f: File) {
        val b = f.listFiles()
        for (i in (0 until b.size)) {
            if (b[i].isFile) {
                b[i].delete()
            } else {
                del(b[i])
            }
        }
        f.delete()
    }

}

interface OnWriteProcess {
    /**
     * 文件写入进度，process , total （单位：B）
     */
    fun onProcess(process: Long, total: Long)
}