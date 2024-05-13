package com.zj.kotlin.coroutine

import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.*
import com.zj.kotlin.coroutine.databinding.ActivityDemo150Binding
import com.zj.kotlin.coroutine.databinding.ActivityDemo160Binding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 *
 * CreateTime:2023/1/11 16:41
 * @author zhengjiong
 */
class Demo160Activity : AppCompatActivity(R.layout.activity_demo160) {
    val viewModel by viewModels<MyViewModel>()

    @OptIn(FlowPreview::class)
    override fun setContentView(layoutResID: Int) {
        val inflateView = layoutInflater.inflate(layoutResID, null)
        setContentView(inflateView)
        val binding = DataBindingUtil.bind<ActivityDemo160Binding>(inflateView)!!

        binding.button1.setOnClickListener {
            lifecycleScope.launch {

            }
        }

        binding.button2.setOnClickListener {

        }


    }

    class MyViewModel : ViewModel() {

    }
}