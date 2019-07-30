package com.zj.kotlin.jvm.zhihu

import com.zj.kotlin.jvm.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created by zhengjiong
 * date: 2019/7/30 22:49
 */

typealias Callback = (String) -> Unit


fun main() {
    //看app中:21.Coroutine-调度器1.kt



    //val example = CoroutineExample21()
    //example.onClick()
}

/*class CoroutineExample21 {

    fun onClick() {
        log("onClick start")
        GlobalScope.launch(Dispatchers.Unconfined) {
            log("launch")
            val result = getUserCoroutine()
            log("result=$result")

        }
        log("onClick end")
    }

    suspend fun getUserCoroutine() = suspendCoroutine<String> { continuation ->
        log("getUserCoroutine start")
        getUser {
            continuation.resume(it)
        }
        log("getUserCoroutine end")
    }

    fun getUser(callBack: Callback) {
        log("getUser start")
        thread(isDaemon = true) {
            log("sleep 1000")
            Thread.sleep(1000)
            log("invoke zj")
            callBack.invoke("zj")
        }
        log("getUser end")
    }

}*/

