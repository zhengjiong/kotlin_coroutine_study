package com.zj.renwuxian

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext


/**
 * 063.4.30-shareIn() 操作符.mp4
 *
 * CreateTime:2026/9/17 15:58
 * @author zhengjiong
 */
fun main() {
    val demo = ShareIn_63_4_30()
    //demo.test1()
    //demo.test2()
    demo.test3()
}

class ShareIn_63_4_30 {

    fun test1() = runBlocking {
        val scope = CoroutineScope(EmptyCoroutineContext)
        val flow1 = flow {
            delay(500)
            emit(1)
            println("flow1 emit1")

            delay(500)
            emit(2)
            println("flow1 emit2")

            delay(500)
            emit(3)
            println("flow1 emit3")
        }

        /**
         * 这里shareIn可以让flow1提前启动,而不需要等到collect才启动,但是会造成漏数据
         *
         * 输出:
         * flow1 emit1
         * flow1 emit2
         * flow1 emit3
         */
        flow1.shareIn(scope, started = SharingStarted.Eagerly)

        delay(10000)
    }

    /**
     * shareFlow1只能接收到3,因为flow1.shareIn就已经开始生产数据了,
     * 而collect却等待了1500毫秒,所以前两条数据错过了.
     *
     * emit1
     * emit2
     * flow1 start collect
     * emit3
     * flow1 collect 3
     */
    fun test2() = runBlocking {
        val scope = CoroutineScope(EmptyCoroutineContext)
        val flow1 = flow {
            println("emit1")
            emit(1)

            delay(1000)
            println("emit2")
            emit(2)

            delay(1000)
            println("emit3")
            emit(3)
        }

        val shareFlow1 = flow1.shareIn(scope, started = SharingStarted.Eagerly)
        scope.launch {
            delay(1500)
            println("flow1 start collect")
            shareFlow1.collect {
                println("flow1 collect $it")
            }
        }
        delay(10000)
    }


    /**
     * shareIn还有一个作用是可以共享同一个数据流,而不是让flow重新开始运行
     *
     * 输出:
     *
     * emit1
     * flow1 start collect 1
     * emit2
     * flow1 collect2: 2
     * flow1 start collect 2
     * emit3
     * flow1 collect1: 3
     * flow1 collect2: 3
     */
    fun test3() = runBlocking {
        val scope = CoroutineScope(EmptyCoroutineContext)
        val flow1 = flow {
            println("emit1")
            emit(1)

            delay(1000)
            println("emit2")
            emit(2)

            delay(1000)
            println("emit3")
            emit(3)
        }

        val shareFlow1 = flow1.shareIn(scope, started = SharingStarted.Eagerly)
        scope.launch {
            delay(1500)
            println("flow1 start collect 2")
            shareFlow1.collect {
                println("flow1 collect1: $it")
            }
        }
        scope.launch {
            delay(500)
            println("flow1 start collect 1")
            shareFlow1.collect {
                println("flow1 collect2: $it")
            }

        }
        delay(10000)
    }

}