package com.zj.kotlin.coroutine

import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by zhengjiong
 * date: 2019/7/29 21:12
 */
val dateFormat = SimpleDateFormat("HH:mm:ss:SSS")

val now = {
    dateFormat.format(Date(System.currentTimeMillis()))
}

fun log(msg: Any?) = println("${now()} [${Thread.currentThread().name}] $msg")