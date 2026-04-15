package com.answufeng.log.demo

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.answufeng.log.AwDesensitizeInterceptor
import com.answufeng.log.AwLogger
import com.answufeng.log.AwLogFileManager

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

        // 查找视图
        tvLog = findViewById(R.id.tvLog)
        logScrollView = findViewById(R.id.logScrollView)

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

        // 基本日志按钮
        findViewById<Button>(R.id.btnDebug).setOnClickListener { sendDebugLog() }
        findViewById<Button>(R.id.btnError).setOnClickListener { sendErrorLog() }
        findViewById<Button>(R.id.btnLambda).setOnClickListener { sendLambdaLog() }
        findViewById<Button>(R.id.btnTagged).setOnClickListener { sendTaggedLog() }
        findViewById<Button>(R.id.btnWtf).setOnClickListener { sendWtfLog() }

        // JSON 日志按钮
        findViewById<Button>(R.id.btnJsonDebug).setOnClickListener { sendJsonLog() }
        findViewById<Button>(R.id.btnJsonError).setOnClickListener { sendJsonErrorLog() }

        // 脱敏功能按钮
        findViewById<Button>(R.id.btnDesensitize).setOnClickListener { testDesensitize() }

        // 文件管理按钮
        findViewById<Button>(R.id.btnFileInfo).setOnClickListener { showFileInfo() }
        findViewById<Button>(R.id.btnCompress).setOnClickListener { compressOldLogs() }
        findViewById<Button>(R.id.btnDiskSpace).setOnClickListener { checkDiskSpace() }
        findViewById<Button>(R.id.btnClearLogs).setOnClickListener { clearAllLogs() }
        findViewById<Button>(R.id.btnFlush).setOnClickListener { flushLogs() }

        // 管理按钮
        findViewById<Button>(R.id.btnClearLog).setOnClickListener { clearLog() }
    }

    override fun onDestroy() {
        super.onDestroy()
        AwLogger.flush()
        log("🔚 日志已刷新")
    }

    private fun log(msg: String) {
        tvLog.append("$msg\n")
        logScrollView.post { logScrollView.fullScroll(ScrollView.FOCUS_DOWN) }
        android.util.Log.d("AwLogDemo", msg)
    }

    private fun clearLog() {
        tvLog.text = "日志输出：\n"
        log("🗑️ 日志已清除")
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
