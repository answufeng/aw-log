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
import android.view.Menu
import android.view.MenuItem
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
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import java.io.File

class MainActivity : AppCompatActivity() {

    // Console
    private lateinit var tvLog: TextView
    private lateinit var logScrollView: ScrollView

    // Config switches
    private lateinit var switchDebug: MaterialSwitch
    private lateinit var switchFile: MaterialSwitch
    private lateinit var switchCrash: MaterialSwitch
    private lateinit var switchDesensitize: MaterialSwitch
    private lateinit var switchCompact: MaterialSwitch

    // Inputs
    private lateinit var etTag: TextInputEditText
    private lateinit var etMessage: TextInputEditText

    // Chips
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

        bindViews()
        setDefaults()
        setupButtons()

        applyConfig(initial = true)
    }

    private fun bindViews() {
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
    }

    private fun setDefaults() {
        switchDebug.isChecked = true
        switchFile.isChecked = true
        switchCrash.isChecked = true
        switchDesensitize.isChecked = true
        chipMinVerbose.isChecked = true
        chipSendDebug.isChecked = true
    }

    private fun setupButtons() {
        findViewById<MaterialButton>(R.id.btnSend).setOnClickListener { sendQuickLog() }
        findViewById<MaterialButton>(R.id.btnSendThrowable).setOnClickListener { sendQuickThrowable() }
        findViewById<MaterialButton>(R.id.btnJson).setOnClickListener { sendJsonLog() }
        findViewById<MaterialButton>(R.id.btnXml).setOnClickListener { sendXmlLog() }
        findViewById<MaterialButton>(R.id.btnDesensitize).setOnClickListener { testDesensitize() }
        findViewById<MaterialButton>(R.id.btnCopyLog).setOnClickListener { copyConsole() }
        findViewById<MaterialButton>(R.id.btnShareLog).setOnClickListener { shareConsole() }
        findViewById<MaterialButton>(R.id.btnApplyConfig).setOnClickListener { applyConfig() }
        findViewById<MaterialButton>(R.id.btnFileInfo).setOnClickListener { showFileInfo() }
        findViewById<MaterialButton>(R.id.btnCompress).setOnClickListener { compressOldLogs() }
        findViewById<MaterialButton>(R.id.btnExport).setOnClickListener { exportLogs() }
        findViewById<MaterialButton>(R.id.btnClearLogs).setOnClickListener { clearAllLogs() }
        findViewById<MaterialButton>(R.id.btnConcurrent).setOnClickListener { testConcurrent() }
        findViewById<MaterialButton>(R.id.btnFlush).setOnClickListener { flushLogs() }
        findViewById<MaterialButton>(R.id.btnCrash).setOnClickListener { triggerCrash() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.demo_main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_clear_console -> { tvLog.text = ""; true }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        AwLogger.flush()
    }

    // Console

    private fun appendLog(msg: String, color: Int? = null) {
        if (color != null) {
            val text = "$msg\n"
            val spannable = SpannableString(text)
            spannable.setSpan(ForegroundColorSpan(color), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            tvLog.append(spannable)
        } else {
            tvLog.append("$msg\n")
        }
        logScrollView.post { logScrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun status(msg: String) {
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_SHORT).show()
    }

    private fun copyConsole() {
        val text = tvLog.text?.toString().orEmpty().trim()
        if (text.isEmpty()) { status("Console is empty"); return }
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("aw-log-console", text))
        status("Copied ${text.length} chars")
    }

    private fun shareConsole() {
        val text = tvLog.text?.toString().orEmpty().trim()
        if (text.isEmpty()) { status("Console is empty"); return }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "aw-log console")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "Share"))
    }

    // Send

    private fun sendQuickLog() {
        val tag = etTag.text?.toString()?.trim().orEmpty().ifEmpty { null }
        val message = etMessage.text?.toString()?.trim().orEmpty().ifEmpty { "Hello aw-log" }
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
        val ex = RuntimeException("Demo exception")
        AwLogger.e(ex, tag) { "Exception demo" }
    }

    private fun sendJsonLog() {
        val json = """{"name":"aw-log","version":"1.0.0","features":["debug","file","crash","interceptor","desensitize"]}"""
        AwLogger.json(json, "Demo")
    }

    private fun sendXmlLog() {
        val xml = """<response><status>200</status><data><user><id>123</id><name>Zhang San</name></user></data></response>"""
        AwLogger.xml(xml, "API")
    }

    private fun testDesensitize() {
        AwLogger.i("Phone: 13812345678, Email: user@example.com")
        AwLogger.d("Login: password=secret123, token=abc123xyz")
    }

    // Config

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
            appendLog("// aw-log ready. Try sending logs, toggling config, or exporting files.", Color.parseColor("#34D399"))
        } else {
            status("Config applied: min=" + priorityName(selectedMinPriority()) + " file=" + switchFile.isChecked + " crash=" + switchCrash.isChecked)
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

    private fun priorityName(priority: Int): String = when (priority) {
        Log.VERBOSE -> "VERBOSE"; Log.DEBUG -> "DEBUG"; Log.INFO -> "INFO"
        Log.WARN -> "WARN"; Log.ERROR -> "ERROR"; Log.ASSERT -> "ASSERT"
        else -> "?"
    }

    // File Management

    private fun showFileInfo() {
        val logDir = AwLogger.getFileDir()
        if (logDir.isEmpty()) { status("File logging is disabled"); return }
        val size = AwLogFileManager.getTotalSize(logDir)
        val files = AwLogFileManager.getLogFiles(logDir)
        val summary = "${files.size} file(s), ${size / 1024}KB total"
        if (files.isEmpty()) { status(summary); return }
        val detail = files.take(5).joinToString("\n") { "  ${it.name} (${it.length() / 1024}KB)" }
        val extra = if (files.size > 5) "\n  ... and ${files.size - 5} more" else ""
        appendLog("// $summary\n$detail$extra")
        status(summary)
    }

    private fun compressOldLogs() {
        val logDir = AwLogger.getFileDir()
        if (logDir.isEmpty()) { status("File logging is disabled"); return }
        AwLogFileManager.compressOldLogsAsync(logDir) { count ->
            runOnUiThread { status("Compressed $count old file(s)") }
        }
    }

    private fun exportLogs() {
        val logDir = AwLogger.getFileDir()
        if (logDir.isEmpty()) { status("File logging is disabled"); return }
        val exportDir = File(cacheDir, "export")
        exportDir.mkdirs()
        val outputFile = File(exportDir, "logs_${System.currentTimeMillis()}.zip")
        AwLogFileManager.exportLogsAsync(logDir, outputFile) { file ->
            runOnUiThread {
                if (file != null) status("Exported: ${file.absolutePath} (${file.length() / 1024}KB)")
                else status("Export failed: no logs or write error")
            }
        }
    }

    private fun clearAllLogs() {
        val logDir = AwLogger.getFileDir()
        if (logDir.isEmpty()) { status("File logging is disabled"); return }
        AwLogFileManager.clearAllAsync(logDir) { count ->
            runOnUiThread { status("Cleared $count file(s)") }
        }
    }

    private fun flushLogs() {
        AwLogger.flush()
        status("Flushed to disk")
    }

    // Demo

    private fun testConcurrent() {
        status("Spawning 5 threads, 3 logs each...")
        repeat(5) { index ->
            Thread {
                for (i in 1..3) {
                    AwLogger.d("Thread-$index") { "Concurrent log #$i" }
                    try { Thread.sleep(10) } catch (_: InterruptedException) { break }
                }
            }.start()
        }
    }

    private fun triggerCrash() {
        status("Crashing... (check crash handler)")
        throw RuntimeException("aw-log demo crash")
    }

    // Listener

    private val consoleListener = AwLogListener { priority, tag, message, _ ->
        val level = when (priority) {
            Log.VERBOSE -> "V"; Log.DEBUG -> "D"; Log.INFO -> "I"
            Log.WARN -> "W"; Log.ERROR -> "E"; Log.ASSERT -> "A"
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
            appendLog(level + "/" + (tag ?: "-") + " " + message, color)
        }
    }
}
