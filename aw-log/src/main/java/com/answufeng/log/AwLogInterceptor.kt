package com.answufeng.log

/**
 * 日志拦截器接口，采用责任链模式（类似 OkHttp Interceptor）。
 *
 * 拦截器可以在日志被写入之前进行过滤、脱敏或增强。
 * 每个拦截器通过 [Chain.proceed] 将修改后的消息传递给下一个拦截器。
 * 拦截器链中任一拦截器抛出异常时，会被自动捕获并跳过，不会中断链路。
 *
 * 使用示例：
 * ```kotlin
 * // 脱敏拦截器
 * addInterceptor(object : AwLogInterceptor {
 *     override fun intercept(chain: Chain): LogResult {
 *         val message = chain.message.replace(Regex("password=\\S+"), "password=****")
 *         return chain.proceed(message = message)
 *     }
 * })
 *
 * // 过滤拦截器
 * addInterceptor(object : AwLogInterceptor {
 *     override fun intercept(chain: Chain): LogResult {
 *         return if (chain.priority >= Log.WARN) chain.proceed()
 *         else LogResult.Rejected("low priority")
 *     }
 * })
 * ```
 */
interface AwLogInterceptor {

    /**
     * 处理拦截逻辑。
     *
     * 通过 [chain] 获取当前日志信息，修改后调用 [Chain.proceed] 传递给下一个拦截器。
     * 若要拒绝该条日志，返回 [LogResult.Rejected]。
     *
     * @param chain 拦截器链，包含当前日志上下文
     * @return 拦截结果，[LogResult.Accepted] 表示接受，[LogResult.Rejected] 表示拒绝
     */
    fun intercept(chain: Chain): LogResult

    /**
     * 拦截器责任链。
     *
     * 提供当前日志的上下文信息（优先级、Tag、消息、异常），
     * 以及 [proceed] 方法将修改后的消息传递给下一个拦截器。
     */
    interface Chain {
        /** 日志优先级，对应 [android.util.Log] 的常量 */
        val priority: Int

        /** 日志标签 */
        val tag: String?

        /** 日志消息内容 */
        val message: String

        /** 关联的异常 */
        val throwable: Throwable?

        /**
         * 将修改后的消息传递给下一个拦截器。
         *
         * @param message 修改后的消息，默认为当前消息
         * @param tag 修改后的标签，默认为当前标签
         * @return 下一个拦截器的处理结果
         */
        fun proceed(message: String = this.message, tag: String? = this.tag): LogResult
    }

    /**
     * 拦截结果。
     *
     * [Accepted] 表示接受该条日志，可携带修改后的消息和标签。
     * [Rejected] 表示拒绝该条日志，日志不会被写入。
     */
    sealed class LogResult {
        /**
         * 接受日志。
         *
         * @param message 最终消息内容
         * @param tag 最终标签，为 null 时使用原始标签
         */
        data class Accepted(val message: String, val tag: String? = null) : LogResult()

        /**
         * 拒绝日志。
         *
         * @param reason 拒绝原因，用于调试
         */
        data class Rejected(val reason: String? = null) : LogResult()
    }
}
