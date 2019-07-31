package com.zj.kotlin.jvm

import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by zhengjiong
 * date: 2019/7/29 21:12
 */
val dateFormat = SimpleDateFormat("yyy-MM-dd HH:mm:ss:")

val now = {
    dateFormat.format(Date(System.currentTimeMillis()))
}

fun log(msg: Any?) = println("${now()} [${Thread.currentThread().name}] $msg")