package com.zj.kotlin.coroutine

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        btn1.setOnClickListener {
            GlobalScope.launch(context = Dispatchers.Main) {
                Logger.i(1)

                val job = GlobalScope.launch {
                    Logger.i(-1)
                    delay(5000)
                    //Thread.sleep(5000)
                    Logger.i(-2)
                }

                Logger.i(2)

                //job.join()

                Logger.i(3)
            }
            Logger.i("end")
        }
    }
}
