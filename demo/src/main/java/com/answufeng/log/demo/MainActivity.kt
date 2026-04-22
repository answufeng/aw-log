package com.answufeng.log.demo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.answufeng.log.AwDesensitizeInterceptor
import com.answufeng.log.AwLogFileManager
import com.answufeng.log.AwLogFormatter
import com.answufeng.log.AwLogListener
import com.answufeng.log.AwLogger
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var tvLog: TextView
    private lateinit var logScrollView: ScrollView

    private lateinit var switchDebug: MaterialSwitch
    private lateinit var switchFile: MaterialSwitch
    private lateinit var switchCrash: MaterialSwitch
    private lateinit var switchDesensitize: MaterialSwitch
    private lateinit var switchCompact: MaterialSwitch

    private lateinit var etTag: TextInputEditText
    private lateinit var etMessage: TextInputEditText

    private lateinit var chipMinVerbose: Chip
    private lateinit var chipMinDebug: Chip
    private lateinit var chipMinInfo: Chip
    private lateinit var chipMinWarn: Chip
    private lateinit var chipMinError: Chip

    private lateinit var chipSendDebug: Chip
    private lateinit var chipSendInfo: Chip
    private lateinit var chipSendWarn: Chip
    private lateinit var chipSendError: Chip

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        tvLog = findViewById(R.id.tvLog)
        logScrollView = findViewById(R.id.logScrollView)

        switchDebug = findViewById(R.id.switchDebug)
        switchFile = findViewById(R.id.switchFile)
        switchCrash = findViewById(R.id.switchCrash)
        switchDesensitize = findViewById(R.id.switchDesensitize)
        switchCompact = findViewById(R.id.switchCompact)

        etTag = findViewById(R.id.etTag)
        etMessage = findViewById(R.id.etMessage)

        chipMinVerbose = findViewById(R.id.chipMinVerbose)
        chipMinDebug = findViewById(R.id.chipMinDebug)
        chipMinInfo = findViewById(R.id.chipMinInfo)
        chipMinWarn = findViewById(R.id.chipMinWarn)
        chipMinError = findViewById(R.id.chipMinError)

        chipSendDebug = findViewById(R.id.chipSendDebug)
        chipSendInfo = findViewById(R.id.chipSendInfo)
        chipSendWarn = findViewById(R.id.chipSendWarn)
        chipSendError = findViewById(R.id.chipSendError)

        // Defaults for demo
        switchDebug.isChecked = true
        switchFile.isChecked = true
        switchCrash.isChecked = true
        switchDesensitize.isChecked = true
        switchCompact.isChecked = false
        chipMinVerbose.isChecked = true
        chipSendDebug.isChecked = true

        findViewById<MaterialButton>(R.id.btnApplyConfig).setOnClickListener { applyConfig() }
        findViewById<MaterialButton>(R.id.btnClearLog).setOnClickListener { clearLog() }
        findViewById<MaterialButton>(R.id.btnCopyLog).setOnClickListener { copyConsole() }
        findViewById<MaterialButton>(R.id.btnShareLog).setOnClickListener { shareConsole() }
        findViewById<MaterialButton>(R.id.btnFlush).setOnClickListener { flushLogs() }

        findViewById<MaterialButton>(R.id.btnSend).setOnClickListener { sendQuickLog() }
        findViewById<MaterialButton>(R.id.btnSendThrowable).setOnClickListener { sendQuickThrowable() }
        findViewById<MaterialButton>(R.id.btnJson).setOnClickListener { sendJsonLog() }
        findViewById<MaterialButton>(R.id.btnXml).setOnClickListener { sendXmlLog() }
        findViewById<MaterialButton>(R.id.btnDesensitize).setOnClickListener { testDesensitize() }

        findViewById<MaterialButton>(R.id.btnFileInfo).setOnClickListener { showFileInfo() }
        findViewById<MaterialButton>(R.id.btnCompress).setOnClickListener { compressOldLogs() }
        findViewById<MaterialButton>(R.id.btnExport).setOnClickListener { exportLogs() }
        findViewById<MaterialButton>(R.id.btnClearLogs).setOnClickListener { clearAllLogs() }

        findViewById<MaterialButton>(R.id.btnConcurrent).setOnClickListener { testConcurrent() }
        findViewById<MaterialButton>(R.id.btnCrash).setOnClickListener { triggerCrash() }

        // First init
        applyConfig(initial = true)
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

    private fun sendQuickLog() {
        val tag = etTag.text?.toString()?.trim().orEmpty().ifEmpty { null }
        val message = etMessage.text?.toString()?.trim().orEmpty().ifEmpty { "Hello aw-log @ ${System.currentTimeMillis()}" }
        when (selectedSendPriority()) {
            Log.DEBUG -> if (tag != null) AwLogger.d(tag, message) else AwLogger.d(message)
            Log.INFO -> if (tag != null) AwLogger.i(tag, message) else AwLogger.i(message)
            Log.WARN -> if (tag != null) AwLogger.w(tag, message) else AwLogger.w(message)
            Log.ERROR -> if (tag != null) AwLogger.e(tag, message) else AwLogger.e(message)
            else -> if (tag != null) AwLogger.d(tag, message) else AwLogger.d(message)
        }
    }

    private fun sendQuickThrowable() {
        val tag = etTag.text?.toString()?.trim().orEmpty().ifEmpty { "Demo" }
        val ex = RuntimeException("Demo exception @ ${System.currentTimeMillis()}")
        AwLogger.e(ex, tag) { "发送一条带异常的日志：${ex.message}" }
    }

    private fun sendJsonLog() {
        val json = """{"name":"aw-log","version":"2.0.0","features":["debug","file","crash","interceptor","desensitize"]}"""
        AwLogger.json(json, "Demo")
    }

    private fun sendXmlLog() {
        val xml = """<response><status>200</status><data><user><id>123</id><name>张三</name></user></data></response>"""
        AwLogger.xml(xml, "API")
    }

    private fun testDesensitize() {
        AwLogger.i("用户手机号: 13812345678, 邮箱: user@example.com")
        AwLogger.d("登录参数: password=secret123, token=abc123xyz")
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

    private fun triggerCrash() {
        appendLog("即将触发崩溃（用于验证 crashLog 写入）。")
        throw RuntimeException("Demo crash from aw-log demo")
    }

    private fun applyConfig(initial: Boolean = false) {
        initLogger(
            debug = switchDebug.isChecked,
            fileLog = switchFile.isChecked,
            crashLog = switchCrash.isChecked,
            desensitize = switchDesensitize.isChecked,
            compactFormatter = switchCompact.isChecked,
            minPriority = selectedMinPriority()
        )
        if (initial) {
            appendColoredLog("aw-log 已初始化：可以开始打点/导出/崩溃测试", Color.parseColor("#34D399"))
        } else {
            appendLog("配置已应用：minLevel=${priorityName(selectedMinPriority())}, fileLog=${switchFile.isChecked}, crashLog=${switchCrash.isChecked}")
        }
    }

    private fun initLogger(
        debug: Boolean,
        fileLog: Boolean,
        crashLog: Boolean,
        desensitize: Boolean,
        compactFormatter: Boolean,
        minPriority: Int
    ) {
        AwLogger.init {
            this.debug = debug
            this.fileLog = fileLog
            fileDir = cacheDir.absolutePath + "/logs"
            maxFileSize = 2L * 1024 * 1024
            maxFileCount = 5
            this.crashLog = crashLog
            if (compactFormatter) {
                fileFormatter = AwLogFormatter.compact()
            }
            if (desensitize) {
                addInterceptor(AwDesensitizeInterceptor.create {
                    phone()
                    email()
                    keyValue()
                })
            }
            addListener(consoleListener)
        }
        AwLogger.setMinPriority(minPriority)
    }

    private val consoleListener = AwLogListener { priority, tag, message, _ ->
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
            Log.VERBOSE -> Color.parseColor("#94A3B8")
            Log.DEBUG -> Color.parseColor("#60A5FA")
            Log.INFO -> Color.parseColor("#34D399")
            Log.WARN -> Color.parseColor("#FBBF24")
            Log.ERROR -> Color.parseColor("#F87171")
            Log.ASSERT -> Color.parseColor("#C084FC")
            else -> Color.LTGRAY
        }
        runOnUiThread {
            appendColoredLog("$level/${tag ?: "NoTag"}: $message", color)
        }
    }

    private fun selectedMinPriority(): Int = when {
        chipMinError.isChecked -> Log.ERROR
        chipMinWarn.isChecked -> Log.WARN
        chipMinInfo.isChecked -> Log.INFO
        chipMinDebug.isChecked -> Log.DEBUG
        else -> Log.VERBOSE
    }

    private fun selectedSendPriority(): Int = when {
        chipSendError.isChecked -> Log.ERROR
        chipSendWarn.isChecked -> Log.WARN
        chipSendInfo.isChecked -> Log.INFO
        else -> Log.DEBUG
    }

    private fun copyConsole() {
        val text = tvLog.text?.toString().orEmpty().trim()
        if (text.isEmpty()) {
            appendLog("控制台为空，暂无可复制内容。")
            return
        }
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("aw-log-console", text))
        appendLog("已复制到剪贴板（${text.length} chars）")
    }

    private fun shareConsole() {
        val text = tvLog.text?.toString().orEmpty().trim()
        if (text.isEmpty()) {
            appendLog("控制台为空，暂无可分享内容。")
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "aw-log demo console")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "分享控制台内容"))
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
