package com.answufeng.log.demo

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.answufeng.log.AwLogger
import com.answufeng.log.AwLogFileManager

class MainActivity : AppCompatActivity() {

    private lateinit var tvLog: TextView
    private lateinit var scrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scrollView = findViewById(R.id.scrollView)
        tvLog = findViewById(R.id.tvLog)

        AwLogger.init {
            debug = true
            fileLog = true
            fileDir = cacheDir.absolutePath + "/logs"
            maxFileSize = 2L * 1024 * 1024
            maxFileCount = 5
            crashLog = true
        }

        findViewById<Button>(R.id.btnDebug).setOnClickListener {
            AwLogger.d("This is a debug message")
            appendLog("Debug log sent")
        }

        findViewById<Button>(R.id.btnError).setOnClickListener {
            AwLogger.e(RuntimeException("Test error"), "This is an error message")
            appendLog("Error log sent")
        }

        findViewById<Button>(R.id.btnJson).setOnClickListener {
            val json = """{"name":"aw-log","version":"1.0.0","features":["debug","file","crash"]}"""
            AwLogger.json(json, "Demo")
            appendLog("JSON log sent")
        }

        findViewById<Button>(R.id.btnLambda).setOnClickListener {
            AwLogger.d { "Lazy evaluated: ${System.currentTimeMillis()}" }
            appendLog("Lambda log sent")
        }

        findViewById<Button>(R.id.btnWtf).setOnClickListener {
            AwLogger.wtf("This is a wtf message")
            appendLog("WTF log sent")
        }

        findViewById<Button>(R.id.btnFileInfo).setOnClickListener {
            val logDir = cacheDir.absolutePath + "/logs"
            val size = AwLogFileManager.getTotalSize(logDir)
            val files = AwLogFileManager.getLogFiles(logDir)
            appendLog("Log dir size: ${size / 1024}KB, files: ${files.size}")
        }

        findViewById<Button>(R.id.btnCompress).setOnClickListener {
            val logDir = cacheDir.absolutePath + "/logs"
            val count = AwLogFileManager.compressOldLogs(logDir)
            appendLog("Compressed $count old log files")
        }

        findViewById<Button>(R.id.btnFlush).setOnClickListener {
            AwLogger.flush()
            appendLog("Log flushed")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AwLogger.flush()
    }

    private fun appendLog(msg: String) {
        tvLog.append("$msg\n")
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }
}
