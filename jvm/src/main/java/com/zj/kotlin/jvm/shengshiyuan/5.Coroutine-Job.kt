package com.zj.kotlin.jvm.shengshiyuan

import com.zj.kotlin.jvm.Logger
import kotlinx.coroutines.*

/**
 *
 * CreateTime:2019-07-18  13:37
 * @author 郑炯
 * @version 1.0
 */

fun main() {
    val demo = HelloCoroutine5()
    demo.test1()
}

class HelloCoroutine5 {

    /**
     * todo
     * 这里没有懂 后面再研究
     */
    fun test1() {
        runBlocking {
            val job = launch (context = Dispatchers.Default){
                while (isActive) {
                    Thread.sleep(300)

                    //1.使用delay
                    //2.使用isActive判断
                    Logger.i("job: I'm sleeping ...")
                }
            }
            /**
             * 一旦job开始执行,再使用cancel是取消不掉的,出发得job中使用delay或者加if判断才能够取消掉该协程
             */
            delay(1200)
            Logger.i("-------------------------------------------------------------------------3")

            /**
             * 一旦 main 函数调用了 job.cancel，job中就看不到任何输出，因为它被取消了。
             * 这里也有一个可以使 Job 挂起的函数 cancelAndJoin 它合并了对 cancel 以及 join 的调用。
             */
            job.cancel() // 取消该作业


            job.join() // 等待作业执行结束
            println("end")
        }
    }
}