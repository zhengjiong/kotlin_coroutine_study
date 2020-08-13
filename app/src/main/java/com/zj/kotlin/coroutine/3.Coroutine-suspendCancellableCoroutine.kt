package com.zj.kotlin.coroutine

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_demo3.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 *
 * CreateTime:2020/8/13 15:48
 * @author zhengjiong
 */

class Demo3Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo2)

        /*
        在 Kotlin 协程库中，有很多协程的构造器方法，这些构造器方法内部可以使用挂起函数来封装回调的 API。
        最主要的 API 是 suspendCoroutine() 和 suspendCancellableCoroutine()就是这类挂起函数，后者是可以被取消的。
        和withContext一样，suspendCancellableCoroutine也是挂起函数，并不会启动新的协程
         */
        //suspendCancellableCoroutine()不会创建新的协程，在指定协程上运行挂起代码块，并挂起该协程直至代码块运行完成。
        /*
        [main]->start
        [main]->suspendCancellableCoroutine  start
        [main]->suspendCancellableCoroutine  end
        [main]->end
         */
        btn1.setOnClickListener {
            try {
                lifecycleScope.launch {
                    log("start")

                    val scc = suspendCancellableCoroutine<String> {
                        log("suspendCancellableCoroutine  start")
                        it.invokeOnCancellation {
                            log("invokeOnCancellation")
                        }
                        //suspendCancellableCoroutine并不是启动一个协程,所以不能使用delay函数
                        //delay(1000)
                        thread {
                            try {
                                Thread.sleep(1000)
                            } catch (e: Exception) {
                                it.resumeWithException(e)
                            }
                            //如果这里不resume的话, 下面的end就不会打印, 该线程会被一直挂起
                            it.resume("zj")
                        }
                        log("suspendCancellableCoroutine  end")
                    }
                    log("end")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}