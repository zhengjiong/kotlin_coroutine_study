package com.zj.kotlin.api

import com.zj.kotlin.bean.User
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.http.GET
import retrofit2.http.Path

/**
 *
 * CreateTime:2019-08-07  17:51
 * @author 郑炯
 * @version 1.0
 */
interface GitHubService {

    @GET("users/{login}")
    suspend fun getUser(@Path("login") login: String): User
}


val gitHubServiceApi by lazy {
    val retrofit = retrofit2.Retrofit.Builder()
        .client(OkHttpClient.Builder().addInterceptor(Interceptor {
            it.proceed(it.request()).apply {
                println("request: " + code())
            }
        }).build())
        .baseUrl("https://api.github.com")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}