package com.zj.kotlin.coroutine

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.*
import com.zj.kotlin.coroutine.databinding.ActivityDemo77Binding
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlin.concurrent.thread

/**
 *
 * CreateTime:2022/7/24 11:50
 * @author zhengjiong
 */
class Demo77Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityDemo77Binding =
            DataBindingUtil.setContentView(this, R.layout.activity_demo77)
        binding.btnStart.setOnClickListener {
            startActivity(Intent(this, EmptyActivity::class.java))
        }
        binding.button1.setOnClickListener {
            /**
             * launchWhenResumed会把后面的方法当做参数传到一个队列中去,只有当生命周期处于Resumed的时候
             * 执行该Block(Runnable),当activity处于stop后会暂停该线程
             */
            //切到后台后,或者启动另外一个activity后能停止while循环
            lifecycleScope.launchWhenResumed {
                var i = 1
                while (true) {
                    delay(3000)
                    println("1  launchWhenResumed ----> $i   currentState=${lifecycle.currentState}  ${Thread.currentThread()}")
                    i++
                }
            }

            //切到后台,或者启动另外一个activity后不会停止while循环
            lifecycleScope.launchWhenResumed {
                thread {
                    var i = 1
                    while (true) {
                        Thread.sleep(3000)
                        println("2  launchWhenResumed ----> $i   currentState=${lifecycle.currentState}  ${Thread.currentThread()}")
                        i++
                    }
                }

            }

            //切到后台,或者启动另外一个activity后不会停止while循环
            lifecycleScope.launchWhenResumed {
                launch(Dispatchers.Default) {
                    var i = 1
                    while (true) {
                        delay(3000)
                        println("3  launchWhenResumed ----> $i   currentState=${lifecycle.currentState}  ${Thread.currentThread()}")
                        i++
                    }
                }
            }

            //切到后台,或者启动另外一个activity后能停止while循环
            lifecycleScope.launchWhenResumed {
                launch {
                    var i = 1
                    while (true) {
                        delay(3000)
                        println("4  launchWhenResumed ----> $i   currentState=${lifecycle.currentState}  ${Thread.currentThread()}")
                        i++
                    }
                }
            }

            //切到后台,或者启动另外一个activity后不会停止while循环
            lifecycleScope.launchWhenResumed {
                withContext(Dispatchers.Default) {
                    var i = 1
                    while (true) {
                        delay(3000)
                        println("5  launchWhenResumed ----> $i   currentState=${lifecycle.currentState}  ${Thread.currentThread()}")
                        i++
                    }
                }
            }

            //切到后台,或者启动另外一个activity后能停止while循环
            lifecycleScope.launchWhenResumed {
                async {
                    var i = 1
                    while (true) {
                        delay(3000)
                        println("6  launchWhenResumed ----> $i   currentState=${lifecycle.currentState}  ${Thread.currentThread()}")
                        i++
                    }
                }
            }

            //切到后台后不能停止while循环
            lifecycleScope.launchWhenResumed {
                async(Dispatchers.Main) {
                    var i = 1
                    while (true) {
                        delay(3000)
                        println("7  launchWhenResumed ----> $i   currentState=${lifecycle.currentState}  ${Thread.currentThread()}")
                        i++
                    }
                }
            }
        }

        binding.button2.setOnClickListener {
            lifecycleScope.launch {

                // activity处于 STARTED状态的时候执行该方法,
                // 生命周期进入 STOPPED 状态时 停止该方法
                // 它会在生命周期再次进入 STARTED 状态时自动开始进行数据收集操作。
                /**
                 * 内部会创建一个LifecycleEventObserver,并且会执行addObserver, 加入到Activity
                 * 的LifecycleRegistry中,activity声明周期变化后,会执行这里设置的对应的方法:(Lifecycle.State.STARTED)
                 */
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    var i = 1
                    while (true) {
                        delay(500)
                        println("1  repeatOnLifecycle ----> $i   currentState=${lifecycle.currentState}  ${Thread.currentThread()}")
                        i++
                    }
                }

            }
        }

        //切到后台后,还是会继续执行
        binding.button21.setOnClickListener {
            lifecycleScope.launch {
                //切到后台后不会停止线程中的代码,因为协程cancel, 没办法关闭掉线程
                repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    thread {
                        var i = 1
                        while (true) {
                            Thread.sleep(1500)
                            println("2  repeatOnLifecycle ----> $i   currentState=${lifecycle.currentState}  ${Thread.currentThread()}")
                            i++
                        }
                    }
                }
            }
        }

        //切到后台后,不会执行while中的循环,当切到前台后会重新执行repeatOnLifecycle中的内容
        binding.button22.setOnClickListener {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    launch(Dispatchers.Default) {
                        var i = 1
                        while (true) {
                            delay(1500)
                            println("3  repeatOnLifecycle ----> $i   currentState=${lifecycle.currentState}  ${Thread.currentThread()}")
                            i++
                        }
                    }
                }
            }
        }

        class Test : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                println("onStateChanged  event=${event.name}    source=${lifecycle.currentState}")
            }
        }

        binding.button3.setOnClickListener {
            lifecycle.addObserver(Test())
        }

        binding.button4.setOnClickListener {
            val flow = flow<Int> {
                repeat(3) {
                    delay(3000)
                    println("emit $it")
                    emit(it)
                }
            }.shareIn(lifecycleScope, SharingStarted.WhileSubscribed(2000), 0)

            lifecycleScope.launch {
                flow.collect {
                    println("collect $it")
                }
            }
        }

        binding.button5.setOnClickListener {
            /**
             * 如果只需从一个数据流中进行收集，则可使用 flowWithLifecycle 来收集数据，它能够在生命周期进入目标
             * 状态时发送数据，并在离开目标状态时取消内部的生产者
             * flowWithLifeCycle内部也是调用了repeatOnLifecycle
             */
            val flow = flow<Int> {
                repeat(10) {
                    delay(1000)
                    println("button5 emit $it")
                    emit(it)
                }
            }.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)

            lifecycleScope.launch {
                flow.collect {
                    println("button5 collect $it")
                }
            }
        }


        //编译报错
        //Suspension functions can be called only within coroutine body
        //挂起函数只能在协程体内使用，也就是 emit 方法时一个挂起函数，只能用在协程体内。
        //那该怎么处理呢？用 callbackFlow 就可以了。
        //什么是 callbackFlow？官方的答案是：将基于回调的 API 转换为数据流。
        //callbackFlow 是冷流，没有接收者，不会产生数据。
        //button7使用callbackFlow代替flow
        binding.button6.setOnClickListener {
            /*val flow = flow {
                requestApi {
                    //编译报错
                    //Suspension functions can be called only within coroutine body
                    //emit(1)
                }
            }*/

            /*flow.collect {
                Log.d("list-", "$it")
            }*/
        }


        binding.button7.setOnClickListener {
            val flow1 = callbackFlow {

                repeat(5){
                    println("repeat start  ${Thread.currentThread()}")
                    //模拟网络请求回调
                    requestApi { result ->
                        //发送数据
                        println("trySend $result  ${Thread.currentThread()}")
                        val channelResult = trySend(result)
                        channelResult.onClosed {
                            //do some thing
                            println("$it channelResult onClosed")
                            throw it ?: ClosedSendChannelException("Channel was closed normally")
                        }
                    }
                }

                awaitClose {
                    //当数据接收者所在的协程被关闭的时候会调用。
                    //作用是：用来释放资源
                    cancelApi()
                }
            }

            lifecycleScope.launch {
                flow1.collect{
                    println("flow1 ->$it   ${Thread.currentThread()}")
                }
            }
        }

        binding.button8.setOnClickListener {
            //注意: callbackFlow 内部使用 channel 实现，其概念与阻塞 队列 十分类似，并且默认容量为 64。
            // 基于 Channel 实现的冷流
            val flow2 = callbackFlow {
                repeat(20) {
                    val result = trySend(it.toString())
                    result.onClosed {
                        println("$it onClosed")
                        throw it ?: ClosedSendChannelException("Channel was closed normally")
                    }

                    /*val result = channel.trySend("1")
                        .onClosed {

                        }.isSuccess*/
                    println("result=${result.isSuccess}")

                    delay(1000)

                    if (it == 8) {
                        delay(2000)
                        println("it == 8 close")
                        // 在出现异常时关闭 Flow
                        close(RuntimeException("close exception"))
                    }
                }
                // 在 Flow 收集结束时进行清理操作
                awaitClose {
                    //removeLocationUpdates(callback)
                    println("awaitClose remove callback")
                }
            }
            lifecycleScope.launch {
                flow2.collect{
                    println("flow2 ->$it  ${Thread.currentThread()}")
                }
            }
        }
    }

    //加了inline, 上面的emit才可以调用,不然报错
    fun requestApi(block: (Long) -> Unit) {
        thread {
            Thread.sleep(2000)
            block(System.currentTimeMillis())
        }
    }

    /**
     * 模拟取消网络请求
     */
    fun cancelApi() {
        //do some thing
        println("cancelApi")
    }

}