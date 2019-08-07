package com.zj.kotlin.coroutine

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


typealias Callback = (String) -> Unit


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        test1()
    }

    private fun test1() {
        /**
         * 输出:
        23:19:26:088[main] onClick start
        23:19:26:128 [main] onClick end
        23:19:26:130 [main] launch
        23:19:26:131 [main] getUserCoroutine start
        23:19:26:131 [main] getUser start
        23:19:26:132 [main] getUser end
        23:19:26:132 [main] getUserCoroutine end
        23:19:26:132 [Thread-2] sleep 1000
        23:19:27:132 [Thread-2] invoke zj
        23:19:27:133 [Thread-2] continuation resume zj
        23:19:27:134 [main] result=zj
         */
        btn1.setOnClickListener {
            log("onClick start")
            GlobalScope.launch(Dispatchers.Main) {
                log("launch")
                val result = getUserCoroutine()
                log("result=$result")

            }
            log("onClick end")
        }

        btn2.setOnClickListener {
            GlobalScope.launch(Dispatchers.Main) {
                for (i in 0..3) {
                    delay(1000)
                    //Thread.sleep(1000)//如果用Thread.sleep就会卡主线程
                    btn2.text = i.toString()
                }
                btn2.text = "done!"
            }
        }

        btn3.setOnClickListener {
            OkHttpClient().newCall(
                Request.Builder()
                    .url("")
                    .build()
            )
        }
    }

    /**
     * suspendCoroutine 这个方法并不是帮我们启动协程的，它运行在协程当中并且帮我们获取到当前协程的 Continuation 实例，
     * 也就是拿到回调，方便后面我们调用它的 resume 或者 resumeWithException 来返回结果或者抛出异常。
     * 如果你重复调用 resume 或者 resumeWithException 会收获一枚 IllegalStateException.
     */
    suspend fun getUserCoroutine() = suspendCoroutine<String> { continuation ->
        log("getUserCoroutine start")
        getUser {
            log("continuation resume $it")
            continuation.resume(it)
        }
        log("getUserCoroutine end")
    }

    //getUser函数需要切到其他线程执行，因此回调通常也会在这个非UI的线程中调用
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
}
