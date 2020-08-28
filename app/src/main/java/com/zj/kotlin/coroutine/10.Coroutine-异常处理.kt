package com.zj.kotlin.coroutine

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_demo10.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 *
 * CreateTime:2020/8/17 13:29
 * @author zhengjiong
 */
class Demo10Activity : AppCompatActivity() {
    val mainScope = MainScope()

    val jobScope = MyScope(Job() + CoroutineName("my-job-scope") + Dispatchers.Main)
    val supervisorScope =
        MyScope(SupervisorJob() + CoroutineName("my-supervisor-scope") + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo10)

        /*
            输出:
            1
            KotlinNullPointerException
            app崩溃
         */
        btn1.setOnClickListener {
            jobScope.launch {
                println("1")
                throw KotlinNullPointerException()
            }
            jobScope.launch {
                delay(1000)
                println("2")
            }
        }

        /*
            android中app直接crash导致2不会输出, 在jvm的测试中2是可以输出的
            输出:
            1
            KotlinNullPointerException
            app崩溃
         */
        btn2.setOnClickListener {
            //在这里加try-catch是没有用的
            supervisorScope.launch {
                println("1")
                throw KotlinNullPointerException()
            }
            supervisorScope.launch {
                delay(1000)
                println("2")
            }
        }

        /*
            android中app直接crash导致2不会输出, 在jvm的测试中2是可以输出的
            输出:
            1
            KotlinNullPointerException
            app崩溃
         */
        btn3.setOnClickListener {
            jobScope.launch {
                //这里加try-catch是捕获不到异常的, 除非把supervisorScope换成coroutineScope, 异常才会被正常抛出, 不然子协程会自己处理异常
                supervisorScope {
                    launch {
                        //child 1
                        println("1")
                        throw KotlinNullPointerException()
                    }

                    launch {
                        delay(1000)
                        println(2)
                    }

                }
            }
        }

        //app不会crash
        //输出:
        //child-1
        //child-2
        //KotlinNullPointerException
        btn4.setOnClickListener {
            //try只能加在coroutineScope或者withContext直接外层, 其他地方会catch不到, 导致应用crash
            jobScope.launch {
                try {
                    //这里用withContext也可以被try-catch
                    //但是不能用supervisorScope, supervisorScope会让子协程自己处理异常就会导致外层catch不到
                    coroutineScope {
                        launch {
                            //child1
                            println("child-1")
                            delay(1000)
                            throw KotlinNullPointerException("exception")
                        }

                        launch {
                            //child2
                            println("child-2")
                            delay(3000)
                            println("end-2")
                        }
                    }
                } catch (e: Exception) {
                    println(e)
                }
            }
        }
        //app crash
        //输出:
        //child-1
        //child-2
        btn5.setOnClickListener {
            jobScope.launch {
                try {
                    //直接对launch进行try-catch是捕获不到的
                    launch {
                        //child1
                        println("child-1")
                        delay(1000)
                        throw KotlinNullPointerException("exception")
                    }
                } catch (e: Exception) {
                    println(e)
                }
                launch {
                    //child2
                    println("child-2")
                    delay(3000)
                    println("end-2")
                }

            }
        }

        /**
         * app不会crash
         * 输出:
         * 1
         * KotlinNullPointerException
         */
        btn6.setOnClickListener {
            //jobScope.async不是挂起函数可以在这里直接启动, 但是下面的await是挂起函数，必须在协程体重调用
            //所以这里要用jobScope.launch来启动一个协程体
            jobScope.launch {
                //注意这里是使用jobScope.async,而不是直接async
                val deferred1 = jobScope.async {
                    println("1")
                    delay(500)
                    if (true) {
                        throw KotlinNullPointerException()
                    }
                    "result"
                }

                try {
                    deferred1.await()
                } catch (e: Exception) {
                    println(e)
                }
            }
        }

        /**
         * app不会crash
         *
         * 如果不进行await()的话, 异常永远都不会被抛出
         *
         * 输出:
         * 1
         *
         */
        btn7.setOnClickListener {
            val deferred = jobScope.async {
                println("1")
                delay(500)
                if (true) {
                    throw KotlinNullPointerException()
                }
                "result"
            }
        }


        //todo:这里不是太理解, 后面补充
        //注意这里异常会被捕获, 但是app还是会crash
        //app crash
        //由于 scope 的直接子协程是 launch，如果 async 中产生了一个异常，这个异常将会被立即抛出。
        //原因是 async (包含一个 Job 在它的 CoroutineContext 中) 会自动传播异常到它的父级 (launch)，
        //这会让异常被立即抛出。
        //输出:
        //1
        btn8.setOnClickListener {
            jobScope.launch {
                try {
                    //这里如果改成jobScope.async{}, 异常就会被正确捕获,app不会crash
                    val deferred = async {
                        println("1")
                        delay(500)
                        throw KotlinNullPointerException()
                    }

                    //这里不执行await也会导致抛出异常,且不被捕获
                    deferred.await()
                } catch (e: Exception) {
                    //注意这里异常会被捕获, 但是app还是会crash
                    println("------>$e")
                }
            }
        }

        //todo:不太明白,以后补充
        //app不会crash
        //会catch到两次异常
        //输出
        // 1
        // 1------>KotlinNullPointerException
        // 2------>KotlinNullPointerException
        btn9.setOnClickListener {
            jobScope.launch {
                try {
                    coroutineScope {
                        try {
                            //这里如果改成jobScope.async{}, 异常就会被正确捕获,app不会crash
                            val deferred = async<Unit> {
                                println("1")
                                delay(500)
                                throw KotlinNullPointerException()
                            }

                            //这里不执行await也会导致抛出异常,且不被捕获
                            deferred.await()
                        } catch (e: Exception) {
                            println("1------>$e")
                        }
                    }
                } catch (e: Exception) {
                    println("2------>$e")
                }
            }
        }

        //todo:不太明白,以后补充
        //app不会crash
        //输出
        // 1
        // 1------>KotlinNullPointerException
        btn10.setOnClickListener {
            jobScope.launch {
                try {
                    coroutineScope {
                        //这里如果改成jobScope.async{}, 异常就会被正确捕获,app不会crash
                        val deferred = async<Unit> {
                            println("1")
                            delay(500)
                            throw KotlinNullPointerException()
                        }

                        //这里不执行await也会导致抛出异常,且不被捕获
                        deferred.await()
                    }
                } catch (e: Exception) {
                    println("1------>$e")
                }
            }
        }

        //todo:不太明白,以后补充
        //app不会crash
        //输出
        // 1
        // 1------>KotlinNullPointerException
        btn11.setOnClickListener {
            jobScope.launch {
                try {
                    supervisorScope {
                        //这里如果改成jobScope.async{}, 异常就会被正确捕获,app不会crash
                        val deferred = async<Unit> {
                            println("1")
                            delay(500)
                            throw KotlinNullPointerException()
                        }

                        //这里不执行await也会导致抛出异常,且不被捕获
                        deferred.await()
                    }
                } catch (e: Exception) {
                    println("1------>$e")
                }
            }
        }

        //app不会crash
        //输出:
        //1
        //KotlinNullPointerException
        btn12.setOnClickListener {
            jobScope.launch {
                try {
                    val deferred = async(SupervisorJob()) {
                        println("1")
                        delay(500)
                        throw KotlinNullPointerException()
                    }

                    deferred.await()
                } catch (e: Exception) {
                    println(e)
                }
            }
        }

        //app crash
        //输出:
        //1
        //KotlinNullPointerException
        btn13.setOnClickListener {
            jobScope.launch {
                coroutineScope {
                    try {
                        val deferred = async<Unit> {
                            println("1")
                            delay(500)
                            throw KotlinNullPointerException()

                        }

                        deferred.await()
                    } catch (e: Exception) {
                        println(e)
                    }
                }
            }
        }


        //app不会crash
        //异常被正确捕获
        //输出:
        //1
        //KotlinNullPointerException
        btn14.setOnClickListener {
            jobScope.launch {
                supervisorScope {
                    val deferred = async {
                        println("1")
                        delay(500)
                        throw KotlinNullPointerException()

                    }
                    try {
                        deferred.await()
                    } catch (e: Exception) {
                        println(e)
                    }
                }
            }
        }

        //app crash
        //异常不能被捕获
        //输出:
        //1
        btn14.setOnClickListener {
            jobScope.launch(SupervisorJob()) {
                try {
                    val deferred = async {
                        println("1")
                        delay(500)
                        throw KotlinNullPointerException()

                    }

                    deferred.await()
                } catch (e: Exception) {
                    println(e)
                }
            }
        }
    }


    /**
     * 仿照MainScope的ContextScope来实现MyScope
     */
    class MyScope(override val coroutineContext: CoroutineContext) : CoroutineScope
}