package demo.xm.com.demo.down2

/**
 * 下载任务
 */
class DownTask {

    var id: String? = null
    var url: String? = null
    var fileName: String? = null
    var fileHouzui: String? = null

    private constructor()

    constructor(b: Builder) {
        this.id = b.id
        this.url = b.url
        this.fileName = b.fileName
        this.fileHouzui = b.fileHouzui
    }

    /**
     * 建造者
     */
    class Builder {
        var id: String? = null//唯一标识
        var url: String? = null//下载资源
        var fileName: String? = null//下载文件名称
        var fileHouzui: String? = null//下载后缀名

        fun id(id: String): Builder {
            this.id = id
            return this
        }

        fun url(url: String): Builder {
            this.url = url
            return this
        }

        fun fileName(fileName: String): Builder {
            this.fileName = fileName
            return this
        }

        fun fileHouzui(fileHouzui: String): Builder {
            this.fileHouzui = fileHouzui
            return this
        }

        fun build(): DownTask {
            return DownTask(this)
        }
    }

    override fun toString(): String {
        return "DownTask(id=$id, url=$url, fileName=$fileName, fileHouzui=$fileHouzui)"
    }
}