package com.answufeng.log

interface AwLogInterceptor {

    fun intercept(priority: Int, tag: String?, message: String, throwable: Throwable?): LogResult

    enum class LogResult {
        ACCEPTED,
        REJECTED
    }
}
