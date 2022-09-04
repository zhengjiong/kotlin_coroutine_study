package com.zj.kotlin.jvm.shengshiyuan

import com.zj.kotlin.jvm.Logger
import com.zj.kotlin.jvm.log
import kotlinx.coroutines.*
import kotlin.coroutines.suspendCoroutine

/**
 *
 * CreateTime:2019-07-18  09:33
 * @author 郑炯
 * @version 1.0
 */

fun main() {
    val demo = HelloCoroutine4()
    demo.test1()
    //demo.test2()
    //demo.test3()
    //demo.test4()
    //demo.test5()
    //demo.test6()
}

/**
 * 1.每一个协程构建器(包括runBlocking) 都会向其代码块作用域中添加一个CoroutineScope实例. 我们
 * 可以在该作用域中启动协程,而无需显示将其join到一起,这是因为外部协程(在下面的实例中就是runBlocking)会等
 * 待该作用域中的所有启动的协程全部完成后才会完成.
 *
 * 2.除去不同的协程构建器所提供的协程作用域(Coroutine scope)外, 我们还可以coroutineScope builder来
 * 声明自己的协程作用域. 该构建器会创建一个协程作用域,并且会等待所有启动的子协程全部完成后自身才完成.
 *
 */
class HelloCoroutine4 {

    /**
     * 输出:
     * Thread[main,5,main] -> -1
     * Thread[main,5,main] -> 2
     * Thread[main,5,main] -> 0
     * Thread[main,5,main] -> 1
     * Thread[main,5,main] -> 3
     */
    fun test1() {
        runBlocking {
            Logger.i("-1")
            /**
             * 这里不使用GlobalScope.launch,直接使用runBlocking的CoroutineScope,才能实现让runBlocking不结束!
             * 外部协程（runBlocking）直到在其作用域中启动的所有协程都执行完毕后才会结束。
             */
            launch {
                Logger.i("0")
                delay(2000)
                Logger.i("1")
            }

            /**
             * 2输出后runBlocking启动的协程不会结束, 会一直阻塞所启动的线程,直到launch所
             * 启动的协程执行结束后才结束, 这样就可以不用写join方法
             */
            Logger.i("2")
        }
        Logger.i("3")
    }

    /**
     *
     * "GlobalScope.launch end"和"launch"谁前谁后是不定的
     *
     * Thread[main,5,main] -> sleep
     * Thread[DefaultDispatcher-worker-1,5,main] -> 0
     * Thread[DefaultDispatcher-worker-1,5,main] -> 1
     * Thread[DefaultDispatcher-worker-1,5,main] -> GlobalScope.launch end
     * Thread[DefaultDispatcher-worker-1,5,main] -> launch
     * Thread[main,5,main] -> job.isCompleted=false
     * Thread[DefaultDispatcher-worker-1,5,main] -> launch end
     * Thread[main,5,main] -> end  job.isCompleted=true
     * Thread[main,5,main] -> end
     */
    fun test2() {
        val job = GlobalScope.launch {
            Logger.i("0")
            delay(1000)
            Logger.i("1")

            launch {
                Logger.i("launch")
                delay(1500)
                Logger.i("launch end")
            }

            /**
             * GlobalScope.launch end执行后, 其实GlobalScope.launch所启动的协程还没有结束,
             * 会等到内部的协程执行结束后才会真正的结束, 从下面打印的job.isCompleted可以看出来1500毫秒后是没有结束的,
             * 还在继续等待内部的其他协程结束.
             */
            Logger.i("GlobalScope.launch end")
        }
        Logger.i("sleep")
        Thread.sleep(1500)
        Logger.i("job.isCompleted=" + job.isCompleted)
        Thread.sleep(1500)
        Logger.i("end  job.isCompleted=" + job.isCompleted)
    }

    /**
     * 输出:
     * Thread[main,5,main] -> 2
     * Thread[main,5,main] -> 3
     * Thread[main,5,main] -> 4
     * Thread[main,5,main] -> runBlocking end
     * Thread[main,5,main] -> 1
     * Thread[main,5,main] -> end
     */
    fun test3() {
        runBlocking {
            launch {
                delay(2000)
                Logger.i("1")
            }

            /**
             * 创建一个协程作用域, coroutineScope会阻塞当前协程, 下面的runBlocking end, 会在
             * coroutineScope执行完成后才打印
             */
            coroutineScope {
                launch {
                    Logger.i("2")
                    delay(500)
                    Logger.i("3")
                }
                delay(1000)
                Logger.i("4")
            }


            Logger.i("runBlocking end")
        }
        Logger.i("end")
    }

    /**
     * Thread[main,5,main] -> 4
     * Thread[main,5,main] -> -3
     * Thread[main,5,main] -> 1
     * Thread[DefaultDispatcher-worker-1,5,main] -> -1
     * Thread[main,5,main] -> -2
     * Thread[main,5,main] -> -6
     * Thread[DefaultDispatcher-worker-3,5,main] -> -5
     * Thread[main,5,main] -> -4
     * Thread[main,5,main] -> 5
     * Thread[DefaultDispatcher-worker-2,5,main] -> -7
     * Thread[DefaultDispatcher-worker-4,5,main] -> -8
     * Thread[main,5,main] -> 6
     * Thread[main,5,main] -> 3
     */
    fun test4() {
        runBlocking {
            async {  }
            /**
             * 这里不使用GlobalScope.launch,直接使用runBlocking的CoroutineScope,才能实现让runBlocking不结束!
             * 外部协程（runBlocking）直到在其作用域中启动的所有协程都执行完毕后才会结束。
             */
            launch {
                delay(1000)
                Logger.i("1")
            }

            GlobalScope.launch {
                delay(1500)
                Logger.i("-1")
            }

            GlobalScope.launch(context = this.coroutineContext) {
                delay(2000)
                Logger.i("-2")
            }
            Logger.i("4")
            //coroutineScope会阻塞当前线程
            coroutineScope {
                Logger.i("-3")
                delay(2500)
                launch {
                    Logger.i("-4")
                }
                launch(context = Dispatchers.Default) {
                    Logger.i("-5")
                }
                Logger.i("-6")
            }
            Logger.i("5")
            //withContext会阻塞当前线程
            withContext(Dispatchers.IO) {
                Logger.i("-7")
                delay(3000)
                Logger.i("-8")
            }
            Logger.i("6")
        }
        Logger.i("3")
    }

    /**
     * 输出:
     * 16:46:33:144 [main] start
     * 16:46:33:150 [DefaultDispatcher-worker-1] 1
     * 16:46:33:154 [DefaultDispatcher-worker-2] 3
     * 16:46:34:160 [DefaultDispatcher-worker-3] 2
     * 16:46:34:161 [main] end
     */
    fun test5() = runBlocking {
        log("start")
        GlobalScope.launch {
            log(1)

            launch {
                delay(1000)
                log(2)
            }

            //使用外层协程的上下文来启动协程, 才可以做到和上面launch同样的效果(外层协程等待内部协程结束才结束)
            //结论:GlobalScope 启动的协程跟外部没有任何关系，外部也不会等他结束，除非主动调用它的 join 或者 await
            GlobalScope.launch(context = coroutineContext) {
                log(3)
                delay(2000)
                log(4)
            }
        }.join()
        log("end")
    }

    fun test6() = runBlocking {
        log("start")

        GlobalScope.launch {
            log(1)

            //coroutineScope会阻塞当前线程
            coroutineScope {
                log(2)
                delay(1000)
                log(3)
            }
            log(4)
        }.join()

        log("end")
    }
}