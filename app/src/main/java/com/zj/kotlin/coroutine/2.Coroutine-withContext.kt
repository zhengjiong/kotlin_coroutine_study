package com.zj.kotlin.coroutine

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_demo2.*
import kotlinx.coroutines.*

/**
 *
 * CreateTime:2020/8/13 15:48
 * @author zhengjiong
 */

class Demo2Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo2)

        //withContext()不会创建新的协程，在指定协程上运行挂起代码块，并挂起该协程直至代码块运行完成。
        /*
        [main]->start
        [DefaultDispatcher-worker-1]->withContext start
        [main]->result=zj
        [main]->end
         */
        btn1.setOnClickListener {
            lifecycleScope.launch {
                log("start")

                val result = withContext(Dispatchers.IO) {
                    log("withContext start")
                    delay(1000)
                    return@withContext "zj"
                }
                log("result=$result")
                log("end")
                coroutineScope {

                }
            }
        }
    }
}