package com.zj.kotlin.jvm.shengshiyuan

import com.zj.kotlin.jvm.Logger
import kotlinx.coroutines.*

/**
 *
 * Default-立即开始调度
 * Lazy-只有在开始执行join/start/await的时候才开始调度
 * Atomic-立即开始调度,并且在第一个挂起点之前不能被取消
 * UNDISPATCHED-立即在当前线程执行协程体,直到遇到第一个挂起点(后面取决于调度器)
 *
 * CreateTime:2019-07-18  14:57
 * @author 郑炯
 * @version 1.0
 */

suspend fun main() {
    val demo = HelloCoroutine6()
    //demo.test1()
    //demo.test2()
    //demo.test3()
    //demo.test4()
    //demo.test5()
    demo.test6()
}

/**
 * CoroutineStart.DEFAULT
 * CoroutineStart.LAZY
 * CoroutineStart.ATOMIC
 * CoroutineStart.UNDISPATCHED
 */
class HelloCoroutine6 {
    /**
     * 输出:
     * Thread[main,5,main] -> 3
     * Thread[DefaultDispatcher-worker-1,5,main] -> 1
     * Thread[DefaultDispatcher-worker-1,5,main] -> 2
     * Thread[DefaultDispatcher-worker-1,5,main] -> 4
     */
    suspend fun test1() {
        /**
         * CoroutineStart.DEFAULT,该模式下协程被创建后会立马执行
         */
        val job = GlobalScope.launch(start = CoroutineStart.DEFAULT) {
            Logger.i("1")
            delay(1000)
            Logger.i("2")
        }
        Logger.i("3")
        job.join()
        //delay(8000)
        Logger.i(4)

    }

    /**
     * Thread[main,5,main] -> 3
     * Thread[DefaultDispatcher-worker-2,5,main] -> 1
     * Thread[DefaultDispatcher-worker-2,5,main] -> 2
     * Thread[DefaultDispatcher-worker-2,5,main] -> 4
     *
     * CoroutineStart.LAZY,该模式下协程被创建后会等到join或者start或者wait才会开始执行
     */
    suspend fun test2() {
        val job = GlobalScope.launch(start = CoroutineStart.LAZY) {
            Logger.i(1)
            delay(1000)
            Logger.i(2)
        }
        Logger.i(3)

        /**
         * join后job中的协程才开始执行
         */
        job.join()

        Logger.i(4)
    }

    /**
     * default cancel
     * 使用CoroutineStart.DEFAULT, job.cancel执行后1可能被打印也可能不被打印,
     * 但是使用ATOMIC,job.cancel执行后1是肯定会被打印的, 他会执行到第一个挂起点才会被取消!
     *
     * 输出:
     * Thread[main,5,main] -> 3
     * Thread[DefaultDispatcher-worker-1,5,main] -> 1
     * Thread[main,5,main] -> 4
     * Thread[DefaultDispatcher-worker-1,5,main] -> end
     */
    suspend fun test3() {
        /**
         *
         */
        val job = GlobalScope.launch(start = CoroutineStart.DEFAULT) {
            Logger.i(1)
            delay(1000)
            Logger.i(2)
        }

        Logger.i(3)
        job.cancel()
        Logger.i(4)
        job.join()
        Logger.i("end")
    }

    /**
     * atomic cancel
     * CoroutineStart.ATOMIC和Default差不多唯一的区别是cancel后会等待第一个挂起点才会被取消!
     *
     * 使用CoroutineStart.DEFAULT, job.cancel执行后1可能被打印也可能不被打印,
     * 但是使用ATOMIC,job.cancel执行后1是肯定会被打印的, 他会执行到第一个挂起点才会被取消!
     *
     * 输出:
     * Thread[main,5,main] -> 3
     * Thread[DefaultDispatcher-worker-1,5,main] -> 1
     * Thread[main,5,main] -> 4
     * Thread[DefaultDispatcher-worker-1,5,main] -> end
     */
    suspend fun test4() {
        /**
         *
         */
        val job = GlobalScope.launch(start = CoroutineStart.ATOMIC) {
            Logger.i(1)
            delay(1000)
            Logger.i(2)
        }

        Logger.i(3)
        job.cancel()
        Logger.i(4)
        job.join()
        Logger.i("end")
    }

    /**
     *
     * lazy cancel
     *
     * 使用CoroutineStart.LAZY,job.cancel执行的时候job还没有被执行, 所以1不可能被输出.
     *
     *
     * 输出:
     * Thread[main,5,main] -> 3
     * Thread[main,5,main] -> 4
     * Thread[main,5,main] -> end
     */
    suspend fun test5() {
        val job = GlobalScope.launch(start = CoroutineStart.LAZY) {
            Logger.i(1)
            delay(1000)
            Logger.i(2)
        }

        Logger.i(3)
        job.cancel()
        Logger.i(4)
        job.join()
        Logger.i("end")
    }

    /**
     * CoroutineStart.UNDISPATCHED立即在当前线程执行协程体,直到遇到第一个挂起点, 后面会在
     * GlobalScope.launch所使用的调度器的线程中调用, 当前是默认调度器就是DefaultDispatcher
     *
     * 输出:
     * Thread[main,5,main] -> 1
     * Thread[main,5,main] -> 3
     * Thread[DefaultDispatcher-worker-1,5,main] -> 2
     * todo
     * 这里很奇怪暂时不明白为什么end也是在DefaultDispatcher的线程中执行, 如果test6不用suspend改为runBlock{}就没有该问题!
     * Thread[DefaultDispatcher-worker-1,5,main] -> end
     * 结论:因为test6()本身也再协程中运行, delay之后因为UNDISPATCHED导致执行协程的线程发生改变了,后面没有再改变所以会沿用之前的线程来执行
     */
    suspend fun test6() {
        val job = GlobalScope.launch(start = CoroutineStart.UNDISPATCHED) {
            Logger.i(1)
            delay(1000)
            Logger.i(2)
        }
        Logger.i(3)
        //job.cancel()
        job.join()
        Logger.i("end")
    }
}