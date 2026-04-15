package com.answufeng.log.demo

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.answufeng.log.AwDesensitizeInterceptor
import com.answufeng.log.AwLogger
import com.answufeng.log.AwLogFileManager
import com.google.android.material.card.MaterialCardView

/**
 * aw-log 库功能演示
 * 包含：基本日志、JSON、脱敏、文件管理等功能
 */
class MainActivity : AppCompatActivity() {

    private lateinit var tvLog: TextView
    private lateinit var logScrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 主布局
        val mainLayout = findViewById<LinearLayout>(R.id.mainLayout)

        // 标题
        mainLayout.addView(TextView(this).apply {
            text = "📝 aw-log 功能演示"
            textSize = 20f
            setPadding(0, 0, 0, 20)
        })

        // 基本日志卡片
        val basicCard = createCard("基本日志")
        val basicLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        basicLayout.addView(createButton("📄 Debug 日志", ::sendDebugLog))
        basicLayout.addView(createButton("❌ Error 日志", ::sendErrorLog))
        basicLayout.addView(createButton("⚡ Lambda 日志", ::sendLambdaLog))
        basicLayout.addView(createButton("🏷️ 带标签日志", ::sendTaggedLog))
        basicLayout.addView(createButton("😱 WTF 日志", ::sendWtfLog))
        basicCard.addView(basicLayout)
        mainLayout.addView(basicCard)

        // JSON 日志卡片
        val jsonCard = createCard("JSON 日志")
        val jsonLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        jsonLayout.addView(createButton("📋 JSON Debug", ::sendJsonLog))
        jsonLayout.addView(createButton("📋 JSON Error", ::sendJsonErrorLog))
        jsonCard.addView(jsonLayout)
        mainLayout.addView(jsonCard)

        // 脱敏功能卡片
        val desensitizeCard = createCard("脱敏功能")
        val desensitizeLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        desensitizeLayout.addView(createButton("🔒 测试脱敏", ::testDesensitize))
        desensitizeCard.addView(desensitizeLayout)
        mainLayout.addView(desensitizeCard)

        // 文件管理卡片
        val fileCard = createCard("文件管理")
        val fileLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fileLayout.addView(createButton("💾 文件信息", ::showFileInfo))
        fileLayout.addView(createButton("📦 压缩日志", ::compressOldLogs))
        fileLayout.addView(createButton("💿 磁盘空间", ::checkDiskSpace))
        fileLayout.addView(createButton("🗑️ 清除日志", ::clearAllLogs))
        fileLayout.addView(createButton("🔄 刷新日志", ::flushLogs))
        fileCard.addView(fileLayout)
        mainLayout.addView(fileCard)

        // 管理功能卡片
        val manageCard = createCard("管理功能")
        val manageLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        manageLayout.addView(createButton("🗑️ 清除界面日志", ::clearLog))
        manageCard.addView(manageLayout)
        mainLayout.addView(manageCard)

        // 日志区域
        mainLayout.addView(TextView(this).apply {
            text = "操作日志："
            textSize = 16f
            setPadding(0, 20, 0, 10)
        })

        logScrollView = findViewById(R.id.logScrollView)
        tvLog = findViewById(R.id.tvLog)

        // 初始化日志
        AwLogger.init {
            debug = true
            fileLog = true
            fileDir = cacheDir.absolutePath + "/logs"
            maxFileSize = 2L * 1024 * 1024
            maxFileCount = 5
            crashLog = true
            addInterceptor(AwDesensitizeInterceptor.create {
                phone()
                email()
                keyValue()
            })
        }

        log("✅ 日志初始化完成")
        log("📊 点击按钮测试各项功能")
    }

    override fun onDestroy() {
        super.onDestroy()
        AwLogger.flush()
        log("🔚 日志已刷新")
    }

    private fun createCard(title: String): MaterialCardView {
        return MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
            setPadding(20, 20, 20, 20)

            addView(TextView(this@MainActivity).apply {
                text = title
                textSize = 16f
                setPadding(0, 0, 0, 12)
            })
        }
    }

    private fun createButton(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 4)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun log(msg: String) {
        tvLog.append("$msg\n")
        logScrollView.post { logScrollView.fullScroll(ScrollView.FOCUS_DOWN) }
        android.util.Log.d("AwLogDemo", msg)
    }

    private fun clearLog() {
        tvLog.text = "日志已清除\n"
    }

    private fun sendDebugLog() {
        AwLogger.d("这是一条 Debug 日志")
        log("📝 发送了 Debug 日志")
    }

    private fun sendErrorLog() {
        AwLogger.e(RuntimeException("测试错误"), "这是一条 Error 日志")
        log("📝 发送了 Error 日志")
    }

    private fun sendLambdaLog() {
        AwLogger.d { "延迟计算: ${System.currentTimeMillis()}" }
        log("📝 发送了 Lambda 日志")
    }

    private fun sendTaggedLog() {
        AwLogger.d("网络") { "连接超时: https://example.com" }
        AwLogger.e("API") { "请求失败: 503 服务不可用" }
        AwLogger.i("UI") { "Activity 创建: ${localClassName}" }
        log("📝 发送了 带标签日志 (网络/API/UI)")
    }

    private fun sendWtfLog() {
        AwLogger.wtf("这是一条 WTF 日志")
        log("📝 发送了 WTF 日志")
    }

    private fun sendJsonLog() {
        val json = """{"name":"aw-log","version":"2.0.0","features":["debug","file","crash","interceptor","desensitize"]}"""
        AwLogger.json(json, "Demo")
        log("📄 发送了 JSON (DEBUG) 日志")
    }

    private fun sendJsonErrorLog() {
        val errorJson = """{"error":"Unauthorized","code":401,"message":"Invalid token"}"""
        AwLogger.json(errorJson, "API", priority = Log.ERROR)
        log("📄 发送了 JSON (ERROR) 日志")
    }

    private fun testDesensitize() {
        AwLogger.i("用户手机号: 13812345678, 邮箱: user@example.com")
        AwLogger.d("登录参数: password=secret123, token=abc123xyz")
        AwLogger.i("身份证: 110101199001011234")
        log("🔒 发送了 脱敏测试日志 (请查看 Logcat 中的掩码输出)")
    }

    private fun showFileInfo() {
        val logDir = cacheDir.absolutePath + "/logs"
        val size = AwLogFileManager.getTotalSize(logDir)
        val files = AwLogFileManager.getLogFiles(logDir)
        val fileInfo = files.joinToString("\n") { f ->
            "  ${f.name} (${f.length() / 1024}KB)"
        }
        log("💾 日志目录大小: ${size / 1024}KB, 文件数: ${files.size}\n$fileInfo")
    }

    private fun compressOldLogs() {
        val logDir = cacheDir.absolutePath + "/logs"
        AwLogFileManager.compressOldLogsAsync(logDir) { count ->
            runOnUiThread {
                log("💾 压缩了 $count 个旧日志文件 (异步)")
            }
        }
    }

    private fun checkDiskSpace() {
        val logDir = cacheDir.absolutePath + "/logs"
        val available = AwLogFileManager.getAvailableSpace(logDir)
        val availableMB = available / (1024 * 1024)
        log("💾 可用磁盘空间: ${availableMB}MB")
    }

    private fun clearAllLogs() {
        val logDir = cacheDir.absolutePath + "/logs"
        AwLogFileManager.clearAllAsync(logDir) { count ->
            runOnUiThread {
                log("💾 清除了 $count 个日志文件 (异步)")
            }
        }
    }

    private fun flushLogs() {
        AwLogger.flush()
        log("🔄 日志已刷新")
    }
}
