package com.zj.kotlin.coroutine

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.zj.kotlin.bean.User
import kotlinx.android.synthetic.main.activity_coroutine_cancel.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
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

        btn1.setOnClickListener {
            MainScope().launch {

            }
        }
    }

    suspend fun getUserCoroutine() = suspendCancellableCoroutine<User> { continuation ->
        val call =
            OkHttpClient.Builder()
                .addInterceptor {
                    it.proceed(it.request()).apply {
                        println("code:${this.code()} ${this.body().toString()}")
                    }
                }
                .build()
                .newCall(
                    Request.Builder()
                        .get().url("https://api.github.com/users/zhengjiong")
                        .build()
                )



        continuation.invokeOnCancellation {
            println("invokeOnCancellation invoke")
            if (!call.isCanceled) {
                call.cancel()
            }
        }

        call.enqueue(object : okhttp3.Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("onFailure = ${e}")
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                log("onResponse = " + response.code())
                try {
                    response.body()?.let {

                    }?: continuation.resumeWithException(NullPointerException("null point"))
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        })
    }
}