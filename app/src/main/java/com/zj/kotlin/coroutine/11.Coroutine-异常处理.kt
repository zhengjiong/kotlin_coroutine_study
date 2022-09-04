package com.zj.kotlin.coroutine

import android.os.Bundle
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.zj.kotlin.coroutine.viewmodel.Demo11ViewModel

/**
 *
 * CreateTime:2022/4/30 20:00
 * @author zhengjiong
 */
class Demo11Activity : AppCompatActivity() {
    val viewModel by viewModels<Demo11ViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo11)

        findViewById<Button>(R.id.button1).setOnClickListener {
            viewModel.test1()
        }

        findViewById<Button>(R.id.button2).setOnClickListener {
            viewModel.test2()
        }

        findViewById<Button>(R.id.button3).setOnClickListener {
            viewModel.test3()
        }
        findViewById<Button>(R.id.button4).setOnClickListener {
            viewModel.test4()
        }
        findViewById<Button>(R.id.button5).setOnClickListener {
            viewModel.test5()
        }
        findViewById<Button>(R.id.button6).setOnClickListener {
            viewModel.test6()
        }
        findViewById<Button>(R.id.button7).setOnClickListener {
            viewModel.test7()
        }

        findViewById<Button>(R.id.button8).setOnClickListener {
            viewModel.test8()
        }
        findViewById<Button>(R.id.button8_1).setOnClickListener {
            viewModel.test8_1()
        }
        findViewById<Button>(R.id.button8_2).setOnClickListener {
            viewModel.test8_2()
        }
        findViewById<Button>(R.id.button8_3).setOnClickListener {
            viewModel.test8_3()
        }
        findViewById<Button>(R.id.button8_31).setOnClickListener {
            viewModel.test8_31()
        }
        findViewById<Button>(R.id.button8_4).setOnClickListener {
            viewModel.test8_4()
        }
        findViewById<Button>(R.id.button8_5).setOnClickListener {
            viewModel.test8_5()
        }
        findViewById<Button>(R.id.button8_6).setOnClickListener {
            viewModel.test8_6()
        }
        findViewById<Button>(R.id.button8_7).setOnClickListener {
            viewModel.test8_7()
        }
        findViewById<Button>(R.id.button9).setOnClickListener {
            viewModel.test9()
        }
        findViewById<Button>(R.id.button10).setOnClickListener {
            viewModel.test10()
        }
        findViewById<Button>(R.id.button11).setOnClickListener {
            viewModel.test11()
        }
        findViewById<Button>(R.id.button11_1).setOnClickListener {
            viewModel.test11_1()
        }
        findViewById<Button>(R.id.button11_1_1).setOnClickListener {
            viewModel.test11_1_1()
        }
        findViewById<Button>(R.id.button11_1_2).setOnClickListener {
            viewModel.test11_1_2()
        }
        findViewById<Button>(R.id.button11_2).setOnClickListener {
            viewModel.test11_2()
        }
        findViewById<Button>(R.id.button11_3).setOnClickListener {
            viewModel.test11_3()
        }
        findViewById<Button>(R.id.button11_4).setOnClickListener {
            viewModel.test11_4()
        }
        findViewById<Button>(R.id.button12).setOnClickListener {
            viewModel.test12()
        }
        findViewById<Button>(R.id.button13).setOnClickListener {
            viewModel.test13()
        }
        findViewById<Button>(R.id.button14).setOnClickListener {
            viewModel.test14()
        }
        findViewById<Button>(R.id.button15).setOnClickListener {
            viewModel.test15()
        }
        findViewById<Button>(R.id.button16).setOnClickListener {
            viewModel.test16()
        }
        findViewById<Button>(R.id.button17).setOnClickListener {
            viewModel.test17()
        }
        findViewById<Button>(R.id.button18).setOnClickListener {
            viewModel.test18()
        }
        findViewById<Button>(R.id.button19).setOnClickListener {
            viewModel.test19()
        }
        findViewById<Button>(R.id.button20).setOnClickListener {
            viewModel.test20()
        }
    }
}