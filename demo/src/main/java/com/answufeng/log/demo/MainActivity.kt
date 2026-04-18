package com.answufeng.log.demo

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.answufeng.log.AwDesensitizeInterceptor
import com.answufeng.log.AwLogFileManager
import com.answufeng.log.AwLogFormatter
import com.answufeng.log.AwLogListener
import com.answufeng.log.AwLogger
import com.google.android.material.button.MaterialButton
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var tvLog: TextView
    private lateinit var logScrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        tvLog = findViewById(R.id.tvLog)
        logScrollView = findViewById(R.id.logScrollView)

        findViewById<MaterialButton>(R.id.btnClearLog).setOnClickListener { clearLog() }
        findViewById<MaterialButton>(R.id.btnVerbose).setOnClickListener { sendVerboseLog() }
        findViewById<MaterialButton>(R.id.btnDebug).setOnClickListener { sendDebugLog() }
        findViewById<MaterialButton>(R.id.btnInfo).setOnClickListener { sendInfoLog() }
        findViewById<MaterialButton>(R.id.btnWarn).setOnClickListener { sendWarnLog() }
        findViewById<MaterialButton>(R.id.btnError).setOnClickListener { sendErrorLog() }
        findViewById<MaterialButton>(R.id.btnWtf).setOnClickListener { sendWtfLog() }
        findViewById<MaterialButton>(R.id.btnLambda).setOnClickListener { sendLambdaLog() }
        findViewById<MaterialButton>(R.id.btnTagged).setOnClickListener { sendTaggedLog() }
        findViewById<MaterialButton>(R.id.btnFormat).setOnClickListener { sendFormatLog() }
        findViewById<MaterialButton>(R.id.btnThrowableLambda).setOnClickListener { sendThrowableLambdaLog() }
        findViewById<MaterialButton>(R.id.btnJsonDebug).setOnClickListener { sendJsonLog() }
        findViewById<MaterialButton>(R.id.btnJsonError).setOnClickListener { sendJsonErrorLog() }
        findViewById<MaterialButton>(R.id.btnXml).setOnClickListener { sendXmlLog() }
        findViewById<MaterialButton>(R.id.btnDesensitize).setOnClickListener { testDesensitize() }
        findViewById<MaterialButton>(R.id.btnLevelFilter).setOnClickListener { testLevelFilter() }
        findViewById<MaterialButton>(R.id.btnFileInfo).setOnClickListener { showFileInfo() }
        findViewById<MaterialButton>(R.id.btnCompress).setOnClickListener { compressOldLogs() }
        findViewById<MaterialButton>(R.id.btnDiskSpace).setOnClickListener { checkDiskSpace() }
        findViewById<MaterialButton>(R.id.btnSearch).setOnClickListener { searchLogs() }
        findViewById<MaterialButton>(R.id.btnExport).setOnClickListener { exportLogs() }
        findViewById<MaterialButton>(R.id.btnFlush).setOnClickListener { flushLogs() }
        findViewById<MaterialButton>(R.id.btnClearLogs).setOnClickListener { clearAllLogs() }
        findViewById<MaterialButton>(R.id.btnConcurrent).setOnClickListener { testConcurrent() }
        findViewById<MaterialButton>(R.id.btnDynamicLevel).setOnClickListener { testDynamicLevel() }
        findViewById<MaterialButton>(R.id.btnCustomFormatter).setOnClickListener { testCustomFormatter() }

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
            addListener(AwLogListener { priority, tag, message, _ ->
                val level = when (priority) {
                    Log.VERBOSE -> "V"
                    Log.DEBUG -> "D"
                    Log.INFO -> "I"
                    Log.WARN -> "W"
                    Log.ERROR -> "E"
                    Log.ASSERT -> "A"
                    else -> "?"
                }
                val color = when (priority) {
                    Log.VERBOSE -> Color.GRAY
                    Log.DEBUG -> Color.BLUE
                    Log.INFO -> Color.parseColor("#4CAF50")
                    Log.WARN -> Color.parseColor("#FF9800")
                    Log.ERROR -> Color.RED
                    Log.ASSERT -> Color.parseColor("#9C27B0")
                    else -> Color.DKGRAY
                }
                runOnUiThread {
                    appendColoredLog("$level/${tag ?: "NoTag"}: $message", color)
                }
            })
        }

        appendColoredLog("日志初始化完成", Color.parseColor("#4CAF50"))
    }

    override fun onDestroy() {
        super.onDestroy()
        AwLogger.flush()
    }

    private fun appendLog(msg: String) {
        tvLog.append("$msg\n")
        logScrollView.post { logScrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun appendColoredLog(msg: String, color: Int) {
        val text = "$msg\n"
        val spannable = SpannableString(text)
        spannable.setSpan(ForegroundColorSpan(color), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        tvLog.append(spannable)
        logScrollView.post { logScrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun clearLog() {
        tvLog.text = ""
    }

    private fun sendVerboseLog() {
        AwLogger.v("这是一条 Verbose 日志")
    }

    private fun sendDebugLog() {
        AwLogger.d("这是一条 Debug 日志")
    }

    private fun sendInfoLog() {
        AwLogger.i("这是一条 Info 日志")
    }

    private fun sendWarnLog() {
        AwLogger.w("这是一条 Warn 日志")
    }

    private fun sendErrorLog() {
        AwLogger.e(RuntimeException("测试错误"), "这是一条 Error 日志")
    }

    private fun sendWtfLog() {
        AwLogger.wtf("这是一条 WTF 日志")
    }

    private fun sendLambdaLog() {
        AwLogger.d { "延迟计算: ${System.currentTimeMillis()}" }
    }

    private fun sendTaggedLog() {
        AwLogger.d("网络", "连接超时: https://example.com")
        AwLogger.e("API", "请求失败: 503 服务不可用")
        AwLogger.i("UI", "Activity 创建: ${localClassName}")
    }

    private fun sendFormatLog() {
        AwLogger.i("用户登录: userId=%d, name=%s", 123, "张三")
        AwLogger.d("请求耗时: %dms, 状态码: %d", 256, 200)
    }

    private fun sendThrowableLambdaLog() {
        val ex = RuntimeException("网络超时")
        AwLogger.e(ex, "API") { "请求失败: ${ex.message}" }
    }

    private fun sendJsonLog() {
        val json = """{"name":"aw-log","version":"2.0.0","features":["debug","file","crash","interceptor","desensitize"]}"""
        AwLogger.json(json, "Demo")
    }

    private fun sendJsonErrorLog() {
        val errorJson = """{"error":"Unauthorized","code":401,"message":"Invalid token"}"""
        AwLogger.json(errorJson, "API", priority = Log.ERROR)
    }

    private fun sendXmlLog() {
        val xml = """<response><status>200</status><data><user><id>123</id><name>张三</name></user></data></response>"""
        AwLogger.xml(xml, "API")
    }

    private fun testDesensitize() {
        AwLogger.i("用户手机号: 13812345678, 邮箱: user@example.com")
        AwLogger.d("登录参数: password=secret123, token=abc123xyz")
    }

    private fun testLevelFilter() {
        val oldLevel = AwLogger.setMinPriority(Log.WARN)
        AwLogger.d("这条 Debug 不会显示")
        AwLogger.i("这条 Info 不会显示")
        AwLogger.w("这条 Warn 会显示")
        AwLogger.e("这条 Error 会显示")
        AwLogger.setMinPriority(oldLevel)
        AwLogger.d("级别已恢复，Debug 可以显示了")
    }

    private fun showFileInfo() {
        val logDir = AwLogger.getFileDir()
        val size = AwLogFileManager.getTotalSize(logDir)
        val files = AwLogFileManager.getLogFiles(logDir)
        val fileInfo = if (files.isEmpty()) {
            "暂无日志文件"
        } else {
            files.joinToString("\n") { f ->
                "  ${f.name} (${f.length() / 1024}KB)"
            }
        }
        appendLog("日志目录大小: ${size / 1024}KB, 文件数: ${files.size}\n$fileInfo")
    }

    private fun compressOldLogs() {
        val logDir = AwLogger.getFileDir()
        AwLogFileManager.compressOldLogsAsync(logDir) { count ->
            runOnUiThread {
                appendLog("压缩了 $count 个旧日志文件 (异步)")
            }
        }
    }

    private fun checkDiskSpace() {
        val logDir = AwLogger.getFileDir()
        val available = AwLogFileManager.getAvailableSpace(logDir)
        val availableMB = available / (1024 * 1024)
        appendLog("可用磁盘空间: ${availableMB}MB")
    }

    private fun searchLogs() {
        val logDir = AwLogger.getFileDir()
        AwLogFileManager.searchAsync(logDir, "Error") { results ->
            runOnUiThread {
                if (results.isEmpty()) {
                    appendLog("未搜索到包含 'Error' 的日志")
                } else {
                    appendLog("搜索到 ${results.size} 条包含 'Error' 的日志:")
                    results.take(5).forEach { appendLog("  $it") }
                }
            }
        }
    }

    private fun exportLogs() {
        val logDir = AwLogger.getFileDir()
        val exportDir = File(cacheDir, "export")
        exportDir.mkdirs()
        val outputFile = File(exportDir, "logs_${System.currentTimeMillis()}.zip")
        AwLogFileManager.exportLogsAsync(logDir, outputFile) { file ->
            runOnUiThread {
                if (file != null) {
                    appendLog("日志已导出: ${file.absolutePath} (${file.length() / 1024}KB)")
                } else {
                    appendLog("导出失败")
                }
            }
        }
    }

    private fun flushLogs() {
        AwLogger.flush()
        appendLog("日志已刷新到磁盘")
    }

    private fun clearAllLogs() {
        val logDir = AwLogger.getFileDir()
        AwLogFileManager.clearAllAsync(logDir) { count ->
            runOnUiThread {
                appendLog("清除了 $count 个日志文件 (异步)")
            }
        }
    }

    private fun testConcurrent() {
        appendLog("启动 5 个线程并发写日志...")
        repeat(5) { index ->
            Thread {
                for (i in 1..3) {
                    AwLogger.d("Thread-$index") { "并发日志 #$i from Thread-$index" }
                    try { Thread.sleep(10) } catch (_: InterruptedException) { break }
                }
            }.start()
        }
    }

    private fun testDynamicLevel() {
        val levels = listOf("VERBOSE" to Log.VERBOSE, "DEBUG" to Log.DEBUG, "INFO" to Log.INFO, "WARN" to Log.WARN, "ERROR" to Log.ERROR)
        val items = levels.map { it.first }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("选择最低日志级别")
            .setItems(items) { _, which ->
                val (_, level) = levels[which]
                val oldLevel = AwLogger.setMinPriority(level)
                appendLog("日志级别已设为: ${items[which]} (之前: ${priorityName(oldLevel)})")
            }
            .show()
    }

    private fun testCustomFormatter() {
        AwLogger.init {
            debug = true
            fileLog = true
            fileDir = cacheDir.absolutePath + "/logs"
            maxFileSize = 2L * 1024 * 1024
            maxFileCount = 5
            crashLog = true
            fileFormatter = AwLogFormatter.compact()
            addInterceptor(AwDesensitizeInterceptor.create {
                phone()
                email()
                keyValue()
            })
            addListener(AwLogListener { priority, tag, message, _ ->
                val level = when (priority) {
                    Log.VERBOSE -> "V"
                    Log.DEBUG -> "D"
                    Log.INFO -> "I"
                    Log.WARN -> "W"
                    Log.ERROR -> "E"
                    Log.ASSERT -> "A"
                    else -> "?"
                }
                val color = when (priority) {
                    Log.VERBOSE -> Color.GRAY
                    Log.DEBUG -> Color.BLUE
                    Log.INFO -> Color.parseColor("#4CAF50")
                    Log.WARN -> Color.parseColor("#FF9800")
                    Log.ERROR -> Color.RED
                    Log.ASSERT -> Color.parseColor("#9C27B0")
                    else -> Color.DKGRAY
                }
                runOnUiThread {
                    appendColoredLog("$level/${tag ?: "NoTag"}: $message", color)
                }
            })
        }
        appendLog("已切换为 compact 格式化器 (HH:mm:ss.SSS)")
        AwLogger.i("这条日志使用 compact 格式化器写入文件")
    }

    private fun priorityName(priority: Int): String = when (priority) {
        Log.VERBOSE -> "VERBOSE"
        Log.DEBUG -> "DEBUG"
        Log.INFO -> "INFO"
        Log.WARN -> "WARN"
        Log.ERROR -> "ERROR"
        Log.ASSERT -> "ASSERT"
        else -> "UNKNOWN"
    }
}
