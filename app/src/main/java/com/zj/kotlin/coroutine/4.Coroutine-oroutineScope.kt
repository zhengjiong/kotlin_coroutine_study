package com.zj.kotlin.coroutine

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_demo3.*
import kotlinx.coroutines.coroutineScope
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

class Demo4Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo2)

        /*
            coroutineScope也是一个挂起函数
         */
        /*
        [main]->start
        [main]->coroutineScope  start
        [main]->coroutineScope  end
        [main]->result=end
        [main]->end
         */
        btn1.setOnClickListener {
            try {
                lifecycleScope.launch {
                    log("start")

                    val result = coroutineScope<String> {
                        log("coroutineScope  start")
                        //it.invokeOnCancellation {
                            //log("invokeOnCancellation")
                        //}
                        //suspendCancellableCoroutine并不是启动一个协程,所以不能使用delay函数
                        delay(1000)
                        log("coroutineScope  end")
                        return@coroutineScope "end"
                    }
                    log("result=$result")
                    log("end")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}