package com.zj.kotlin.coroutine

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.*
import com.zj.kotlin.coroutine.databinding.ActivityDemo88Binding
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.internal.ChannelFlow
import kotlin.concurrent.thread

/**
 *
 * callbackFlow 是 Kotlin 协程库中的一个 API，用于将回调风格的代码转换为流 (Flow)。这对于处理异步事件非常有用，例如网络请求、传感器数据、用户输入事件等。
 *
 * callbackFlow 是一个冷流 (cold flow)，它只有在被收集的时候才会启动。通过 callbackFlow，我们可以将回调函数的结果发送到流中，并在流被收集时处理这些结果。
 *
 * 以下是一个使用 callbackFlow 的示例，演示如何将一个假设的回调 API 转换为 Flow。
 *
 * CreateTime:2022/7/24 11:50
 * @author zhengjiong
 */
class Demo88Activity : AppCompatActivity() {
    @OptIn(InternalCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityDemo88Binding =
            DataBindingUtil.setContentView(this, R.layout.activity_demo88)

        //编译报错
        //Suspension functions can be called only within coroutine body
        //挂起函数只能在协程体内使用，也就是 emit 方法时一个挂起函数，只能用在协程体内。
        //那该怎么处理呢？用 callbackFlow 就可以了。
        //什么是 callbackFlow？官方的答案是：将基于回调的 API 转换为数据流。
        //callbackFlow 是冷流，没有接收者，不会产生数据。
        //button2使用callbackFlow代替flow
        binding.button1.setOnClickListener {
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


//        System.out: callbackFlow start  Thread[main,5,main]
//        System.out: 1 trySend -> 1661783682433  Thread[Thread-6,5,main]
//        System.out: 1 isSuccess=true
//        System.out: close
//        System.out: flow1 collect ->1661783682433   Thread[main,5,main]
//        System.out: 2 trySend -> 1661783682433  Thread[Thread-6,5,main]
//        System.out: 2 onClosed java.lang.RuntimeException: 1 zj exception channelResult onClosed
//
//        --------- beginning of crash
//        System.out: awaitClose  Thread[main,5,main]
//        System.out: cancelApi
//        System.out: CoroutineExceptionHandler throwable=java.lang.RuntimeException: 1 zj exception
        binding.button2.setOnClickListener {
            val flow1 = callbackFlow {
                println("callbackFlow start  ${Thread.currentThread()}")
                //模拟网络请求回调
                    requestApi { result ->
                        //发送数据
                        try {
                            println("1 trySend -> $result  ${Thread.currentThread()}")
                            val channelResult = trySend(result)
                            channelResult.onClosed {
                                //do some thing
                                println("1 onClosed $it channelResult onClosed")
                                throw it ?: ClosedSendChannelException("Channel was closed normally")
                            }
                            println("1 isSuccess=${channelResult.isSuccess}")
                            throw RuntimeException("1 zj exception")
                        } catch (e: Exception) {
                            println("close ")
                            close(e)
                            val channelResult = trySend("2 try send close")
                            println("2 trySend -> $result  ${Thread.currentThread()}")
                            channelResult.onClosed {
                                //do some thing
                                println("2 onClosed $it channelResult onClosed")
                                throw it ?: ClosedSendChannelException("2 Channel was closed normally")
                            }
                            println("2 isSuccess=${channelResult.isSuccess}")
                        }
                    }

                awaitClose {
                    println("awaitClose  ${Thread.currentThread()}")
                    //当数据接收者所在的协程被关闭的时候会调用。
                    //作用是：用来释放资源
                    cancelApi()
                }
            }
            lifecycleScope.launch(CoroutineExceptionHandler { coroutineContext, throwable ->
                println("CoroutineExceptionHandler throwable=$throwable")
            }) {
                flow1.collect {
                    println("flow1 collect ->$it   ${Thread.currentThread()}")
                }
            }
        }


//        System.out: ------------> trySend 0   Thread[main,5,main]
//        System.out: 0 isSuccess=true  Thread[main,5,main]
//        System.out: collect flow2 ->0  Thread[main,5,main]
//        System.out: ------------> trySend 1   Thread[main,5,main]
//        System.out: collect flow2 ->1  Thread[main,5,main]
//        System.out: 1 isSuccess=true  Thread[main,5,main]
//        System.out: it == 1 close
//        System.out: ------------> trySend 2   Thread[main,5,main]
//        System.out: onClosed java.lang.RuntimeException: close exception   Thread[main,5,main]    Thread[main,5,main]
//        System.out: 2 isSuccess=false  Thread[main,5,main]
//        System.out: catch com.zj.kotlin.coroutine.Demo88Activity$onCreate$3$2$2@116dda4   Thread[main,5,main]
        var btn3Job: Job? = null
        binding.button3.setOnClickListener {
            //注意: callbackFlow 内部使用 channel 实现，其概念与阻塞 队列 十分类似，并且默认容量为 64。
            // 基于 Channel 实现的冷流
            var flow2 = callbackFlow {
                for (it in 0..3) {
                    println("------------> trySend $it   ${Thread.currentThread()}")
                    if (isClosedForSend) {
                        println("isClosedForSend=true break  $it")
                        break
                    }
                    val result = trySend(it.toString()).onFailure {
//                    val result = trySendBlocking(it.toString()).onFailure {
                        println("onFailure  $it")
                    }.onClosed {
                        println("onClosed $it    ${Thread.currentThread()}")
                        //throw it ?: ClosedSendChannelException("Channel was closed normally")
                        //cancel("zj cancel")
                    }

                    println("$it isSuccess=${result.isSuccess}  ${Thread.currentThread()}")

                    delay(1000)

                    if (it == 2) {
                        println("it == 3 close  ${Thread.currentThread()}")
                        // 在出现异常时关闭 Flow
                        close(RuntimeException("finish"))
                    }
                    println("------------> isActive=$isActive    isClosedForSend=$isClosedForSend")
                }
                // 在 Flow 收集结束时进行清理操作
                awaitClose {
                    //removeLocationUpdates(callback)
                    println("awaitClose remove callback  isActive=$isActive    isClosedForSend=$isClosedForSend  ${Thread.currentThread()}")
                }
            }
            btn3Job = lifecycleScope.launch(CoroutineExceptionHandler { coroutineContext, throwable ->
                println("CoroutineExceptionHandler  $throwable   ${Thread.currentThread()}")
            }) {
                flow2.catch {
                    println("catch ${this}   ${Thread.currentThread()}")
                }.onCompletion {  }.collect {
                    println("collect flow2 ->$it  ${Thread.currentThread()}")
                }
            }
        }

        binding.button4.setOnClickListener {
            if (btn3Job?.isActive == true) {
                btn3Job?.cancel()
            }
        }

        var job5:Job?=null
        binding.button5.setOnClickListener {
            val flow = callbackFlow<Int> {
                for (i in 1..4) {
                    thread {
                        trySend(i).onClosed {
                            println("onClosed  $i  $it  isActive=$isActive   isClosedForSend=$isClosedForSend  throwable=$it")
                        }.onFailure {
                            println("onFailure $i  $it  isActive=$isActive   isClosedForSend=$isClosedForSend  throwable=$it")
                        }.onSuccess {
                            println("onSuccess $i  $it  isActive=$isActive   isClosedForSend=$isClosedForSend  ")
                        }
                        Thread.sleep(1000)
                        if (i == 4) {
                            close()
                        }
                    }

                }
                awaitClose { log("release resources") }
            }

            job5 = lifecycleScope.launch(CoroutineExceptionHandler { coroutineContext, throwable ->
                println("CoroutineExceptionHandler  $throwable")
            }) {
                flow.onCompletion {
                    println("onCompletion")
                }.collect {
                    log(it)
                }
            }
        }

        var job6:Job?=null
        binding.button6.setOnClickListener {
            val flow = callbackFlow<Int> {
                for (i in 1..2) {
                    thread {
                        if (i == 1) {
                            Thread.sleep(2000)
                        }else if (i == 2) {
                            Thread.sleep(8000)
                        }
                        //trySendBlocking如果发送成功会立即返回
                        //trySendBlocking()
                        trySend(i).onClosed {
                            println("onClosed  $i  $it  isActive=$isActive   isClosedForSend=$isClosedForSend  throwable=$it")
                        }.onFailure {
                            println("onFailure $i  $it  isActive=$isActive   isClosedForSend=$isClosedForSend  throwable=$it")
                        }.onSuccess {
                            println("onSuccess $i  $it  isActive=$isActive   isClosedForSend=$isClosedForSend  ")
                        }
                    }

                }
                awaitClose { log("release resources") }
            }

            job6 = lifecycleScope.launch(CoroutineExceptionHandler { coroutineContext, throwable ->
                println("CoroutineExceptionHandler  $throwable")
            }) {
                flow.onCompletion {
                    println("onCompletion")
                }.collect {
                    log("collect $it")
                }
            }
        }
        binding.button7.setOnClickListener {
            if (job6?.isCancelled == false && job6?.isActive == true) {
                job6?.cancel(CancellationException("custom cancel exception"))
            }
        }
    }

    //加了inline, 上面的button1中的emit才可以调用,不然报错
    /*inline*/ fun requestApi(block: (String) -> Unit) {
        thread {
            Thread.sleep(3000)
            block(System.currentTimeMillis().toString())
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