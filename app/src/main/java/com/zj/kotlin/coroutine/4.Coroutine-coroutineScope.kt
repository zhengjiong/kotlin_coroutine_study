package com.zj.kotlin.coroutine

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_demo3.*
import kotlinx.coroutines.*
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



        /*
        [main]->coroutineScope end
        [DefaultDispatcher-worker-2]->launch end
        [main]->lifecycleScope end
         */
        btn2.setOnClickListener {
            lifecycleScope.launch {
                //这里适合用withContext替代就可以少一层嵌套，如果内部代码会自己启动一个新线程的话，使用coroutineScope比withContext更好
                coroutineScope {
                    launch(Dispatchers.Default) {
                        delay(2000)
                        log("launch end")
                    }
                    log("coroutineScope end")
                }

                log("lifecycleScope end")
            }
        }
    }
}