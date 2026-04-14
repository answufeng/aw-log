package com.answufeng.log

/**
 * 内置脱敏拦截器，预置常见敏感字段规则。
 *
 * 使用 DSL 创建：
 * ```kotlin
 * AwDesensitizeInterceptor.create {
 *     phone()       // 中国手机号
 *     idCard()      // 身份证号
 *     bankCard()    // 银行卡号
 *     email()       // 邮箱
 *     keyValue()    // password/token/secret 等 key=value 脱敏
 *     custom("myRule", Regex("pattern"), Strategy.FULL)
 * }
 * ```
 */
class AwDesensitizeInterceptor private constructor(
    private val rules: List<DesensitizeRule>,
    private val defaultStrategy: Strategy
) : AwLogInterceptor {

    /**
     * 脱敏策略。
     *
     * [PARTIAL] 保留前3后4位，中间用 **** 替代。
     * [FULL] 全部替换为 ******。
     * [HASH] 取字符串的 hashCode 十六进制表示。
     */
    enum class Strategy(val mask: (String) -> String) {
        PARTIAL({ s -> s.take(3) + "****" + s.takeLast(4) }),
        FULL({ _ -> "******" }),
        HASH({ s -> s.hashCode().toString(16) })
    }

    /**
     * 脱敏规则，包含名称、匹配正则和脱敏策略。
     */
    data class DesensitizeRule(
        val name: String,
        val pattern: Regex,
        val strategy: Strategy = Strategy.PARTIAL
    )

    override fun intercept(chain: AwLogInterceptor.Chain): AwLogInterceptor.LogResult {
        var message = chain.message
        for (rule in rules) {
            message = message.replace(rule.pattern) { matchResult ->
                rule.strategy.mask(matchResult.value)
            }
        }
        return chain.proceed(message, chain.tag)
    }

    companion object {
        /** 中国手机号规则（1[3-9]开头，11位）。 */
        @JvmField
        val PHONE = DesensitizeRule("phone", Regex("""1[3-9]\d{9}"""))

        /** 身份证号规则（18位，含末尾 X）。 */
        @JvmField
        val ID_CARD = DesensitizeRule("idCard", Regex("""[1-9]\d{5}(19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]"""))

        /** 银行卡号规则（16-19位数字）。 */
        @JvmField
        val BANK_CARD = DesensitizeRule("bankCard", Regex("""\d{16,19}"""))

        /** 邮箱规则，默认使用 [Strategy.FULL] 全掩码。 */
        @JvmField
        val EMAIL = DesensitizeRule("email", Regex("""[\w.\-]+@[\w.\-]+\.\w+"""), Strategy.FULL)

        /** key=value 规则（password/token/secret/key/auth），默认使用 [Strategy.FULL] 全掩码。 */
        @JvmField
        val KEY_VALUE = DesensitizeRule("keyValue", Regex("""(?i)(password|token|secret|key|auth)\s*[=:]\s*\S+"""), Strategy.FULL)

        /**
         * 使用 DSL 创建脱敏拦截器。
         *
         * ```kotlin
         * AwDesensitizeInterceptor.create {
         *     phone()
         *     email()
         *     keyValue()
         *     custom("myRule", Regex("pattern"))
         * }
         * ```
         */
        @JvmStatic
        fun create(block: DesensitizeBuilder.() -> Unit): AwDesensitizeInterceptor {
            return DesensitizeBuilder().apply(block).build()
        }
    }

    /**
     * 脱敏拦截器 DSL 构建器。
     */
    @AwLogDsl
    class DesensitizeBuilder {
        private val rules = mutableListOf<DesensitizeRule>()

        /** 默认脱敏策略，用于 [custom] 方法未指定策略时，默认 [Strategy.PARTIAL]。 */
        var defaultStrategy = Strategy.PARTIAL

        /** 添加自定义规则。 */
        fun addRule(rule: DesensitizeRule) {
            rules.add(rule)
        }

        /** 添加中国手机号脱敏规则。 */
        fun phone() {
            rules.add(PHONE)
        }

        /** 添加身份证号脱敏规则。 */
        fun idCard() {
            rules.add(ID_CARD)
        }

        /** 添加银行卡号脱敏规则。 */
        fun bankCard() {
            rules.add(BANK_CARD)
        }

        /** 添加邮箱脱敏规则。 */
        fun email() {
            rules.add(EMAIL)
        }

        /** 添加 key=value 脱敏规则（password/token/secret 等）。 */
        fun keyValue() {
            rules.add(KEY_VALUE)
        }

        /**
         * 添加自定义脱敏规则。
         *
         * @param name 规则名称，用于调试
         * @param pattern 匹配正则表达式
         * @param strategy 脱敏策略，默认使用 [defaultStrategy]
         */
        fun custom(name: String, pattern: Regex, strategy: Strategy = defaultStrategy) {
            rules.add(DesensitizeRule(name, pattern, strategy))
        }

        internal fun build() = AwDesensitizeInterceptor(rules, defaultStrategy)
    }
}
