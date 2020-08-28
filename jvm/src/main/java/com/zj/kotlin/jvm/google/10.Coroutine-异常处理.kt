package com.zj.kotlin.jvm.google

import kotlinx.coroutines.*

/**
 *
 * CreateTime:2020/8/23 10:24
 * @author zhengjiong
 */

fun main() {
    val demo = Demo10()
    //demo.test1()
//    demo.test2()
    //demo.test3()
//    demo.test4()
    demo.test5()
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
        如果 Child 1 失败了，无论是 scope 还是 Child 2 都会被取消。

        输出:
        1
        KotlinNullPointerException
        2
     */
    fun test2(){
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
    fun test4(){
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
    fun test5(){
        jobScope.launch {
            try {
                println(1)
                throw KotlinNullPointerException()
            } catch (e: Exception) {
                println(e)
            }
        }
    }
}
