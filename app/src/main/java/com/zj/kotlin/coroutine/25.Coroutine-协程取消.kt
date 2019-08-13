package com.zj.kotlin.coroutine

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.zj.kotlin.bean.User
import kotlinx.android.synthetic.main.activity_coroutine_cancel.*
import kotlinx.coroutines.*
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import retrofit2.HttpException
import java.io.IOException
import java.lang.NullPointerException
import kotlin.coroutines.resumeWithException

/**
 *
 * CreateTime:2019-08-08  09:43
 * @author 郑炯
 * @version 1.0
 */


class Coroutine_Cancel_Example : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coroutine_cancel)


        /*
        2019-08-12 22:27:36.443 14581-14581/com.zj.kotlin.coroutine I/System.out: 22:27:36:441 [main] onclick start
        2019-08-12 22:27:36.494 14581-14581/com.zj.kotlin.coroutine I/System.out: 22:27:36:494 [main] onclick end
        2019-08-12 22:27:36.499 14581-14581/com.zj.kotlin.coroutine I/System.out: 22:27:36:499 [main] 1-launch
        2019-08-12 22:27:36.500 14581-14581/com.zj.kotlin.coroutine I/System.out: 22:27:36:500 [main] getUserCoroutine start
        2019-08-12 22:27:36.530 14581-14581/com.zj.kotlin.coroutine I/System.out: 22:27:36:529 [main] getUserCoroutine end
        2019-08-12 22:27:38.086 14581-14653/com.zj.kotlin.coroutine I/System.out: 22:27:38:085 [OkHttp https://api.github.com/...] addInterceptor code:200 okhttp3.internal.http.RealResponseBody@5f2204d
        2019-08-12 22:27:38.086 14581-14653/com.zj.kotlin.coroutine I/System.out: 22:27:38:086 [OkHttp https://api.github.com/...] onResponse = 200
        2019-08-12 22:27:38.091 14581-14581/com.zj.kotlin.coroutine I/System.out: 22:27:38:091 [main] user= {"login":"zhengjiong","id":3313291,"node_id":"MDQ6VXNlcjMzMTMyOTE=","avatar_url":"https://avatars0.githubusercontent.com/u/3313291?v=4","gravatar_id":"","url":"https://api.github.com/users/zhengjiong","html_url":"https://github.com/zhengjiong","followers_url":"https://api.github.com/users/zhengjiong/followers","following_url":"https://api.github.com/users/zhengjiong/following{/other_user}","gists_url":"https://api.github.com/users/zhengjiong/gists{/gist_id}","starred_url":"https://api.github.com/users/zhengjiong/starred{/owner}{/repo}","subscriptions_url":"https://api.github.com/users/zhengjiong/subscriptions","organizations_url":"https://api.github.com/users/zhengjiong/orgs","repos_url":"https://api.github.com/users/zhengjiong/repos","events_url":"https://api.github.com/users/zhengjiong/events{/privacy}","received_events_url":"https://api.github.com/users/zhengjiong/received_events","type":"User","site_admin":false,"name":"zhengjiong","company":null,"blog":"qq:253586504","location":"ChengDu","email":null,"hireable":null,"bio":null,"public_repos":157,"public_gists":1,"followers":13,"following":39,"created_at":"2013-01-19T08:59:22Z","updated_at":"2019-07-25T13:40:09Z"}
         */
        btn1.setOnClickListener {
            log("onclick start")
            MainScope().launch {
                log("1-launch")
                val user = getUserCoroutine()
                log("user= $user")
            }
            log("onclick end")
        }


        /*
        2019-08-12 22:59:20.385 19172-19172/com.zj.kotlin.coroutine I/System.out: 22:59:20:382 [main] onclick start
        2019-08-12 22:59:20.432 19172-19172/com.zj.kotlin.coroutine I/System.out: 22:59:20:432 [main] onclick end
        2019-08-12 22:59:20.436 19172-19172/com.zj.kotlin.coroutine I/System.out: 22:59:20:436 [main] 1-launch
        2019-08-12 22:59:20.440 19172-19172/com.zj.kotlin.coroutine I/System.out: 22:59:20:439 [main] 2-launch start
        2019-08-12 22:59:20.440 19172-19172/com.zj.kotlin.coroutine I/System.out: 22:59:20:440 [main] getUserCoroutine start
        2019-08-12 22:59:20.470 19172-19172/com.zj.kotlin.coroutine I/System.out: 22:59:20:470 [main] getUserCoroutine end
        2019-08-12 22:59:20.473 19172-19172/com.zj.kotlin.coroutine I/System.out: 22:59:20:473 [main] invokeOnCancellation invoke
        2019-08-12 22:59:20.475 19172-19172/com.zj.kotlin.coroutine I/System.out: 22:59:20:474 [main] 2-launch end
        2019-08-12 22:59:20.475 19172-19294/com.zj.kotlin.coroutine I/System.out: 22:59:20:475 [OkHttp https://api.github.com/...] onFailure = java.io.IOException: Canceled
         */
        btn2.setOnClickListener {
            log("onclick start")
            GlobalScope.launch(context = Dispatchers.Main) {
                log("1-launch")
                val job = launch {
                    log("2-launch start")
                    val user = getUserCoroutine()
                    log("3")
                }
                delay(20)
                job.cancelAndJoin()
                log("2-launch end")
            }
            log("onclick end")
        }

        /*
        2019-08-12 22:59:20.385 19172-19172/com.zj.kotlin.coroutine I/System.out: 22:59:20:382 [main] onclick start
        2019-08-12 22:59:20.432 19172-19172/com.zj.kotlin.coroutine I/System.out: 22:59:20:432 [main] onclick end
        2019-08-12 22:59:20.436 19172-19172/com.zj.kotlin.coroutine I/System.out: 22:59:20:436 [main] 1-launch
        2019-08-12 22:59:20.440 19172-19172/com.zj.kotlin.coroutine I/System.out: 22:59:20:439 [main] 2-launch start
        2019-08-12 22:59:20.440 19172-19172/com.zj.kotlin.coroutine I/System.out: 22:59:20:440 [main] getUserCoroutine start
        2019-08-12 22:59:20.470 19172-19172/com.zj.kotlin.coroutine I/System.out: 22:59:20:470 [main] getUserCoroutine end
        2019-08-12 22:59:20.473 19172-19172/com.zj.kotlin.coroutine I/System.out: 22:59:20:473 [main] invokeOnCancellation invoke
        2019-08-12 22:59:20.475 19172-19172/com.zj.kotlin.coroutine I/System.out: 22:59:20:474 [main] 2-launch end
        2019-08-12 22:59:20.475 19172-19294/com.zj.kotlin.coroutine I/System.out: 22:59:20:475 [OkHttp https://api.github.com/...] onFailure = java.io.IOException: Canceled
         */
        btn3.setOnClickListener {
            log("onclick start")
            MainScope().launch {
                log("1-launch")
                val job = launch {
                    log("2-launch start")
                    val user = getUserCoroutine()
                    log("3")
                }
                delay(20)
                job.cancelAndJoin()
                log("2-launch end")
            }
            log("onclick end")
        }
    }

    suspend fun getUserCoroutine() = suspendCancellableCoroutine<String> { continuation ->
        log("getUserCoroutine start")
        val call =
            OkHttpClient.Builder()
                .addInterceptor {
                    it.proceed(it.request()).apply {
                        log("addInterceptor code:${this.code()} ${this.body().toString()}")
                    }
                }
                .build()
                .newCall(
                    Request.Builder()
                        .get().url("https://api.github.com/users/zhengjiong")
                        .build()
                )



        continuation.invokeOnCancellation {
            log("invokeOnCancellation invoke")
            if (!call.isCanceled) {
                call.cancel()
            }
        }

        call.enqueue(object : okhttp3.Callback {
            override fun onFailure(call: Call, e: IOException) {
                log("onFailure = ${e}")
                continuation.resumeWithException(e)
            }

            /*override fun onResponse(call: Call, response: Response) {
                log("onResponse = " + response.code())
                try {
                    response.body()?.let {
                        continuation.resume(it.string()) {
                            log("onResponse cancel ${it.message}")
                        }
                    } ?: continuation.resumeWithException(NullPointerException("null point"))
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }*/

            override fun onResponse(call: Call, response: Response) {
                log("onResponse = " + response.code())
                continuation.resumeWith(runCatching {
                    /* response.body()?.let {
                         continuation.resume(it.string()) {
                             log("onResponse cancel ${it.message}")
                         }
                     } ?: continuation.resumeWithException(NullPointerException("null point"))*/
                    if (response.isSuccessful) {
                        response.body()?.string()
                            ?: throw NullPointerException("Response body is null: $response")
                    } else {
                        throw HttpException(retrofit2.Response.error<Any>(response.code(), response.body()!!))
                    }
                })
            }
        })
        log("getUserCoroutine end")
    }
}