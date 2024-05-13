package com.zj.kotlin.jvm.google

import kotlinx.coroutines.*

/**
 *
 * CreateTime:2020/8/23 10:24
 * @author zhengjiong
 */

fun main() {
    val demo = Demo10()
//    demo.test1()
//    demo.test2()
//    demo.test3()
//    demo.test4()
    //demo.test5()
//    demo.test6()
    demo.test7()
    //demo.test8()
    //demo.test9()
    System.`in`.read()
}

class Demo10 {
    val jobScope = CoroutineScope(Job() + CoroutineName("my-job-scope"))
    val supervisorScope = CoroutineScope(SupervisorJob() + CoroutineName("my-job-scope"))

    /*
        如果 Child 1 失败了，无论是 scope 还是 Child 2 都会被取消。

        输出:
        1
        KotlinNullPointerException
     */
    fun test1() {
        jobScope.launch {
            //child 1
            println("1")
            throw KotlinNullPointerException()
        }

        jobScope.launch {
            //child 2
            delay(1000)
            println("2")
        }

    }

    /*
        如果 Child 1 失败了，Child 2 不会被取消。

        输出:
        1
        KotlinNullPointerException
        2
     */
    fun test2() {
        supervisorScope.launch {
            //child 1
            println("1")
            throw KotlinNullPointerException()
        }

        supervisorScope.launch {
            //child 2
            delay(1000)
            println(2)
        }
    }

    /*

    由于 supervisorScope 使用 SupervisorJob 创建了一个子作用域，如果 Child 1 失败了，Child 2 不会被取消。
    而如果使用 coroutineScope 代替 supervisorScope ，错误就会被传播，而作用域最终也会被取消(不会输出2)。

    输出:
    1
    KotlinNullPointerException
    2
     */
    fun test3() {
        jobScope.launch {
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

    /*
        Child 1 的父级 Job 就只是 Job 类型! 虽然乍一看确实会让人以为是 SupervisorJob，但是因为新的协程被创建时，
        会生成新的 Job 实例替代 SupervisorJob，所以这里并不是。本例中的 SupervisorJob 是协程的父级通过
        scope.launch 创建的，所以真相是，SupervisorJob 在这段代码中完全没用！
        无论 Child 1 或 Child 2 运行失败，错误都会到达外层作用域，所有该作用域开启的协程都会被取消。
        输出:
        1
        KotlinNullPointerException
     */
    fun test4() {
        jobScope.launch(SupervisorJob()) {
            launch {
                // child1
                println("1")
                throw KotlinNullPointerException()
            }

            launch {
                // child2
                delay(1000)
                println("2")
            }
        }
    }

    /*
        使用 launch 时，异常会在它发生的第一时间被抛出，这样您就可以将抛出异常的代码包裹到 try/catch 中

        输出:
        1
        kotlin.KotlinNullPointerException
     */
    fun test5() {
        jobScope.launch {
            try {
                println(1)
                throw KotlinNullPointerException()
            } catch (e: Exception) {
                println(e)
            }
        }
    }

    /**
     * 1
    2
    Exception in thread "main" java.lang.ArithmeticException: / by zero
    at com.zj.kotlin.jvm.google.Demo10$test6$1$1$1.invokeSuspend(10.Coroutine-异常处理.kt:149)
    at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
    at kotlinx.coroutines.DispatchedTask.run(Dispatched.kt:241)
    at kotlinx.coroutines.EventLoopImplBase.processNextEvent(EventLoop.common.kt:270)
    at kotlinx.coroutines.BlockingCoroutine.joinBlocking(Builders.kt:79)
    at kotlinx.coroutines.BuildersKt__BuildersKt.runBlocking(Builders.kt:54)
    at kotlinx.coroutines.BuildersKt.runBlocking(Unknown Source)
    at kotlinx.coroutines.BuildersKt__BuildersKt.runBlocking$default(Builders.kt:36)
    at kotlinx.coroutines.BuildersKt.runBlocking$default(Unknown Source)
    at com.zj.kotlin.jvm.google.Demo10.test6(10.Coroutine-异常处理.kt:142)
    at com.zj.kotlin.jvm.google._10_Coroutine_异常处理Kt.main(10.Coroutine-异常处理.kt:18)
    at com.zj.kotlin.jvm.google._10_Coroutine_异常处理Kt.main(10.Coroutine-异常处理.kt)
    3
    4
     */
    fun test6() {
        runBlocking {
            println("1")
            //换成coroutineScope这里就可以try到, 但是下面的3不会输出
            supervisorScope {
                println("2")
                //这里try不到
                try {
                    // 启动一个子协程
                    launch {
                        1 / 0 // 故意让子协程出现异常
                    }
                } catch (e: Exception) {
                    println(e.message)
                }
                delay(100)
                println("3")
            }
            println("4")
        }
    }

    /*
    输出:
        1
        2
        exception
        4
     */
    fun test6_1() {
        runBlocking {
            println("1")
            //换成coroutineScope这里就可以try到, 但是下面的3不会输出
            try {
                coroutineScope {
                    println("2")
                    //这里try不到
                    try {
                        // 启动一个子协程
                        launch {
                            1 / 0 // 故意让子协程出现异常
                        }
                    } catch (e: Exception) {
                        println(e.message)
                    }
                    delay(100)
                    println("3")
                }
            } catch (e: Exception) {
                TODO("Not yet implemented")
            }
            println("4")
        }
    }

    /**
     * 1
    2
    error
    Exception in thread "main" java.lang.ArithmeticException: / by zero
    at com.zj.kotlin.jvm.google.Demo10$test7$1$1.invokeSuspend(10.Coroutine-异常处理.kt:197)
     */
    fun test7() {
        runBlocking(CoroutineExceptionHandler { coroutineContext, throwable ->
            println("throwable->"+throwable)
        }) {
            println("1")
            supervisorScope {
                println("2")
                // 启动一个子协程
                launch {
                    try {
                        delay(1000)
                        println("3")
                    } catch (e: Exception) {
                        //这里会输出
                        println("error")
                    }
                }
                delay(100)
                1 / 0 //父协程报错
                println("4")
            }
        }
    }

    /**
     * 1
       2
       e->/ by zero
     */
    fun test8() {
        runBlocking {
            println("1")
            try {
                //可以try到, 如果换成supervisorScope就try不到
                coroutineScope {
                    println("2")
                    // 启动一个子协程
                    launch {
                        1 / 0 // 故意让子协程出现异常
                    }
                    delay(100)
                    println("3")
                }
            } catch (e: Exception) {
                println("e->" + e.message)
            }
        }
    }

    /**
    1
    2

    Exception in thread "main" java.lang.ArithmeticException: / by zero
    at com.zj.kotlin.jvm.google.Demo10$test9$1$1$1.invokeSuspend(10.Coroutine-异常处理.kt:246)

    3
     */
    fun test9() {
        runBlocking {
            println("1")
            try {
                //try不到
                supervisorScope {
                    println("2")
                    // 启动一个子协程
                    launch {
                        1 / 0 // 故意让子协程出现异常
                    }
                    delay(100)
                    println("3")
                }
            } catch (e: Exception) {
                println("e->" + e.message)
            }
        }
    }
}
