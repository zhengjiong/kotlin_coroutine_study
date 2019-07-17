package com.zj.kotlin.jvm

/**
 *
 * CreateTime:2019-07-17  16:57
 * @author 郑炯
 * @version 1.0
 */
class Logger {
    companion object {
        fun i(any: Any?) {
            println(Thread.currentThread().toString() + " -> " + any)
        }
    }
}