package demo.xm.com.demo.down3.task

import demo.xm.com.demo.down3.config.DownConfig.Companion.DEFAULT
import demo.xm.com.demo.down3.config.DownConfig.Companion.EMPTY_STRING
import demo.xm.com.demo.down3.enum_.DownStateType

class DownTask {

    //请求下载字段
    var name = EMPTY_STRING
    var url = EMPTY_STRING
    var uuid = EMPTY_STRING
    var fileName = EMPTY_STRING
    var state = DownStateType.NOT_STARTED.ordinal

    //存储到数据所用字段
    var progress = DEFAULT
    var total = DEFAULT
    var present = DEFAULT
    var path = EMPTY_STRING
    var dir = EMPTY_STRING
    var absolutePath = EMPTY_STRING
}