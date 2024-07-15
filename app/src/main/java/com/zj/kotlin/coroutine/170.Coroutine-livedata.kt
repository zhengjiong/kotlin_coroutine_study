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
 * 使用liveData{}扩展函数,处理一些需要转换的耗时操作
 *
 * CreateTime:2024-05-16 11:16:20
 * @author zhengjiong
 */
class Demo170Activity : AppCompatActivity(R.layout.activity_demo170) {

    @OptIn(FlowPreview::class)
    override fun setContentView(layoutResID: Int) {
        val inflateView = layoutInflater.inflate(layoutResID, null)
        setContentView(inflateView)
        val binding = DataBindingUtil.bind<ActivityDemo160Binding>(inflateView)!!


        //使用liveData{}扩展函数,处理一些需要转换的耗时操作

        val liveData1 = MutableLiveData<Int>()
        val transLiveData = Transformations.map(liveData1) {
            //此处是在主线程中执行, 如果要做一些耗时操作,可以使用下面的liveData{}扩展函数
            //....
            it.toString()
        }


        val liveData = liveData<Int> {
            //val data = doSuspendingFunction()//协程中处理
            //emit(data)
        }



        lifecycleScope.launch {
            //通过asFlow将liveData转换成flow, 然后使用flowWithLifecycle在生命周期中获取数据
            liveData1.asFlow().flowWithLifecycle(this@Demo170Activity.lifecycle, Lifecycle.State.STARTED)
                .collect{

                }
        }
    }
}