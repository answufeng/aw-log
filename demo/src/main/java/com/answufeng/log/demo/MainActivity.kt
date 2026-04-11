package com.answufeng.log.demo

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.answufeng.log.BrickLogger
import com.answufeng.log.LogConfig
import com.answufeng.log.LogFileManager

class MainActivity : AppCompatActivity() {

    private lateinit var tvLog: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvLog = TextView(this).apply { textSize = 14f }
        val container = findViewById<LinearLayout>(R.id.container)
        container.addView(tvLog)

        BrickLogger.init {
            debug = true
            fileLog = true
            fileDir = cacheDir.absolutePath + "/logs"
            maxFileSize = 2L * 1024 * 1024
            maxFileCount = 5
        }

        container.addView(button("Debug Log") {
            BrickLogger.d("This is a debug message")
            log("Debug log sent")
        })

        container.addView(button("Error Log") {
            BrickLogger.e(RuntimeException("Test error"), "This is an error message")
            log("Error log sent")
        })

        container.addView(button("JSON Log") {
            val json = """{"name":"aw-log","version":"1.0.0","features":["debug","file","crash"]}"""
            BrickLogger.json(json, "Demo")
            log("JSON log sent")
        })

        container.addView(button("Lambda Log") {
            BrickLogger.d("Demo") { "Lazy evaluated: ${System.currentTimeMillis()}" }
            log("Lambda log sent")
        })

        container.addView(button("Log File Info") {
            val logDir = cacheDir.absolutePath + "/logs"
            val size = LogFileManager.getTotalSize(logDir)
            val files = LogFileManager.getLogFiles(logDir)
            log("Log dir size: ${size / 1024}KB, files: ${files.size}")
        })

        container.addView(button("Compress Old Logs") {
            val logDir = cacheDir.absolutePath + "/logs"
            val count = LogFileManager.compressOldLogs(logDir)
            log("Compressed $count old log files")
        })
    }

    private fun button(text: String, onClick: () -> Unit): Button {
        return Button(this).apply { this.text = text; setOnClickListener { onClick() } }
    }

    private fun log(msg: String) { tvLog.append("$msg\n") }
}
