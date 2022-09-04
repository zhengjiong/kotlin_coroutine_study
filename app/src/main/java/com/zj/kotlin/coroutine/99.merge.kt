package com.zj.kotlin.coroutine

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_demo1.*
import kotlinx.coroutines.*

/**
 * Created by zhengjiong
 * date: 2020/8/27 21:27
 */

class Demo99Activity : AppCompatActivity() {
    val jobScope = CoroutineScope(Job())
    val supervisorScope = CoroutineScope(SupervisorJob())
    val mainScope = MainScope()
    val scope = CoroutineScope(Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo1)

        btn1.setOnClickListener {
            lifecycleScope.launch {
                println(coroutineContext[Job.Key])
                launch {
                    println(coroutineContext[Job.Key])
                }
                withContext(Dispatchers.Default) {
                    println(coroutineContext[Job.Key])
                }
            }
            jobScope.launch {
                println(coroutineContext[Job.Key])
                launch {
                    println(coroutineContext[Job.Key])
                }
                withContext(Dispatchers.Default) {
                    println(coroutineContext[Job.Key])
                }
            }
            supervisorScope.launch {
                println(coroutineContext[Job.Key])
                launch {
                    println(coroutineContext[Job.Key])
                }
                withContext(Dispatchers.Default) {
                    println(coroutineContext[Job.Key])
                }
            }
            mainScope.launch {
                println(coroutineContext[Job.Key])
                launch {
                    println(coroutineContext[Job.Key])
                }
                withContext(Dispatchers.Default) {
                    println(coroutineContext[Job.Key])
                }
            }
            scope.launch(SupervisorJob()) {
                println(coroutineContext[Job.Key])
                launch {
                    println(coroutineContext[Job.Key])
                }
                withContext(Dispatchers.Default) {
                    println(coroutineContext[Job.Key])
                }
            }
        }

        btn2.setOnClickListener {
            lifecycleScope.launch {
                try {
                    //这里用supervisorScope或者CoroutineScope均可以捕获到异常
                    //因为withContext并不会开启一个新的协程,所以它被取消后均可以在外层catch到
                    //受影响的只是里面的launch启动的协程
                    supervisorScope {
                        println(coroutineContext[Job.Key])

                        launch {
                            try {
                                //这里就算是使用了supervisorScope, 也会被下面withContext的异常所取消,
                                //所以这里的supervisorScope根本没有用!!!
                                //因为withContext的异常会取消掉supervisorScope, supervisorScope又是这里launch的父协程
                                //父协程取消了,子协程肯定会被取消, 想不被取消看下方btn3
                                supervisorScope {
                                    println(coroutineContext[Job.Key])
                                    delay(3000)
                                    println("delay-end")
                                }
                            } catch (e: Exception) {
                                // JobCancellationException: Parent job is Cancelling; job=ScopeCoroutine{Cancelling}@9d362ed
                                println(e)
                            }
                        }



                        //withContext(Dispatchers.Default) {
                            println(coroutineContext[Job.Key])
                            delay(1000)
                            throw KotlinNullPointerException()
                        //}
                    }
                } catch (e: Exception) {
                    println(e)
                }
            }
        }

        /*btn2.setOnClickListener {
            lifecycleScope.launch {
                try {
                    //这里用supervisorScope或者CoroutineScope均可以捕获到异常
                    //因为withContext并不会开启一个新的协程,所以它被取消后均可以在外层catch到
                    //受影响的只是里面的launch启动的协程
                    supervisorScope {
                        println(coroutineContext[Job.Key])

                        //方法1:launch想要不被下面的异常所取消, 可以在这里使用Job或者SupervisorJob
                        //因为这样就是单独的一个作用域,不受父协程控制
                        launch(Job()) {
                            try {
                                println(coroutineContext[Job.Key])
                                delay(3000)
                                //3000ms后 delay-end会顺利输出
                                println("delay-end")
                            } catch (e: Exception) {
                                //这里不会输出异常
                                println(e)
                            }
                        }

                        println("withContext")

                        //withContext(Dispatchers.Default) {
                            println(coroutineContext[Job.Key])
                            delay(1000)
                            throw KotlinNullPointerException()
                        //}
                    }
                } catch (e: Exception) {
                    println(e)
                }
            }
        }*/

        /*btn2.setOnClickListener {
            lifecycleScope.launch {
                try {
                    //这里用supervisorScope或者CoroutineScope均可以捕获到异常
                    //因为withContext并不会开启一个新的协程,所以它被取消后均可以在外层catch到
                    //受影响的只是里面的launch启动的协程
                    supervisorScope {
                        println(coroutineContext[Job.Key])

                        //这里没有测试意义, withContext会在coroutineScope内部的launch执行完成后才执行
                        coroutineScope {
                            launch(Dispatchers.IO) {
                                try {
                                    println(coroutineContext[Job.Key])
                                    delay(3000)
                                    //3000ms后 delay-end会顺利输出
                                    println("delay-end")
                                } catch (e: Exception) {
                                    //这里不会输出异常
                                    println(e)
                                }
                            }
                        }
                        println("withContext")
                        withContext(Dispatchers.Default) {
                            println(coroutineContext[Job.Key])
                            delay(1000)
                            throw KotlinNullPointerException()
                        }
                    }
                } catch (e: Exception) {
                    println(e)
                }
            }
        }*/
    }
}