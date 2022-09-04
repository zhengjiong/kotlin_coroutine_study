package com.zj.kotlin.coroutine

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.*
import com.zj.kotlin.coroutine.databinding.ActivityDemo9Binding
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlin.concurrent.thread

/**
 *
 * CreateTime:2022/7/24 11:50
 * @author zhengjiong
 */
class Demo9Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityDemo9Binding =
            DataBindingUtil.setContentView(this, R.layout.activity_demo9)

        binding.button1.setOnClickListener {
            log("click--------")
            val result = fun1()
            log("result=$result")
        }
    }


    fun fun1(): String = runBlocking {
        log("runBlocking start ")
        delay(5000)
        log("runBlocking end")
        return@runBlocking "fun1"
    }
}