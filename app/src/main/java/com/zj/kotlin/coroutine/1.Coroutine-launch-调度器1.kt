package com.zj.kotlin.coroutine

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_demo1.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


typealias Callback = (String) -> Unit


class Demo1Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo1)

        //协程内部使用launch函数会启动一个新的协程，并不是挂起协程，所以后面的代码还是会继续执行，"end"会在"2"之前打印出来
        /*
        [main]->start
        [main]->end
        [DefaultDispatcher-worker-1]->1
        [DefaultDispatcher-worker-1]->2
         */
        btn1.setOnClickListener {
            lifecycleScope.launch {
                log("start")
                val a = launch(Dispatchers.Default) {
                    log("1")
                    delay(1000)
                    log("2")
                }
                log("end")
            }
        }

        /*
        [main]->start
        [main]->end
        [main]->1
        [main]->2
         */
        btn2.setOnClickListener {
            lifecycleScope.launch {
                log("start")
                val a = launch {
                    log("1")
                    delay(1000)
                    log("2")
                }
                log("end")
            }
        }

    }

}
