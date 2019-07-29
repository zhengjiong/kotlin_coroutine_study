package com.zj.kotlin.jvm.zhihu

import com.zj.kotlin.jvm.log
import kotlinx.coroutines.*
import kotlin.coroutines.suspendCoroutine

/**
 * Created by zhengjiong
 * date: 2019/7/29 23:06
 */
suspend fun main() {
    GlobalScope.launch {
        log(1)
        log("1-coroutineContext -> " + coroutineContext[Job])

        GlobalScope.launch(start = CoroutineStart.UNDISPATCHED) {
            log(2)
            log("2-coroutineContext -> " + coroutineContext[Job])
            delay(1000)
            log(3)
            log("3-coroutineContext -> " + coroutineContext[Job])
        }.join()
        suspendCoroutine<String> {
            log("-1-coroutineContext -> " + coroutineContext[Job])
            it.resumeWith(Result.success("-1"))
        }
        log(4)
        log("4-coroutineContext -> " + coroutineContext[Job])
    }.join()
}
