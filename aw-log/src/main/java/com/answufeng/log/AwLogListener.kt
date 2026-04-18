package com.answufeng.log

fun interface AwLogListener {

    fun onLog(priority: Int, tag: String?, message: String, throwable: Throwable?)
}
