package com.answufeng.log.demo

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.answufeng.log.AwDesensitizeInterceptor
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
            addInterceptor(AwDesensitizeInterceptor.create {
                phone()
                email()
                keyValue()
            })
        }

        findViewById<Button>(R.id.btnDebug).setOnClickListener {
            AwLogger.d("This is a debug message")
            appendLog("Debug log sent")
        }

        findViewById<Button>(R.id.btnError).setOnClickListener {
            AwLogger.e(RuntimeException("Test error"), "This is an error message")
            appendLog("Error log sent")
        }

        findViewById<Button>(R.id.btnLambda).setOnClickListener {
            AwLogger.d { "Lazy evaluated: ${System.currentTimeMillis()}" }
            appendLog("Lambda log sent")
        }

        findViewById<Button>(R.id.btnTaggedLambda).setOnClickListener {
            AwLogger.d("Network") { "Connection timeout: https://example.com" }
            AwLogger.e("API") { "Request failed: 503 Service Unavailable" }
            AwLogger.i("UI") { "Activity created: ${localClassName}" }
            appendLog("Tagged Lambda logs sent (Network/API/UI)")
        }

        findViewById<Button>(R.id.btnWtf).setOnClickListener {
            AwLogger.wtf("This is a wtf message")
            appendLog("WTF log sent")
        }

        findViewById<Button>(R.id.btnFlush).setOnClickListener {
            AwLogger.flush()
            appendLog("Log flushed")
        }

        findViewById<Button>(R.id.btnJson).setOnClickListener {
            val json = """{"name":"aw-log","version":"2.0.0","features":["debug","file","crash","interceptor","desensitize"]}"""
            AwLogger.json(json, "Demo")
            appendLog("JSON (DEBUG) log sent")
        }

        findViewById<Button>(R.id.btnJsonError).setOnClickListener {
            val errorJson = """{"error":"Unauthorized","code":401,"message":"Invalid token"}"""
            AwLogger.json(errorJson, "API", priority = Log.ERROR)
            appendLog("JSON (ERROR) log sent")
        }

        findViewById<Button>(R.id.btnDesensitize).setOnClickListener {
            AwLogger.i("User phone: 13812345678, email: user@example.com")
            AwLogger.d("Login params: password=secret123, token=abc123xyz")
            AwLogger.i("ID card: 110101199001011234")
            appendLog("Desensitize test sent (check Logcat for masked output)")
        }

        findViewById<Button>(R.id.btnFileInfo).setOnClickListener {
            val logDir = cacheDir.absolutePath + "/logs"
            val size = AwLogFileManager.getTotalSize(logDir)
            val files = AwLogFileManager.getLogFiles(logDir)
            val fileInfo = files.joinToString("\n") { f ->
                "  ${f.name} (${f.length() / 1024}KB)"
            }
            appendLog("Log dir size: ${size / 1024}KB, files: ${files.size}\n$fileInfo")
        }

        findViewById<Button>(R.id.btnCompress).setOnClickListener {
            val logDir = cacheDir.absolutePath + "/logs"
            AwLogFileManager.compressOldLogsAsync(logDir) { count ->
                runOnUiThread {
                    appendLog("Compressed $count old log files (async)")
                }
            }
        }

        findViewById<Button>(R.id.btnDiskSpace).setOnClickListener {
            val logDir = cacheDir.absolutePath + "/logs"
            val available = AwLogFileManager.getAvailableSpace(logDir)
            val availableMB = available / (1024 * 1024)
            appendLog("Available disk space: ${availableMB}MB")
        }

        findViewById<Button>(R.id.btnClearAll).setOnClickListener {
            val logDir = cacheDir.absolutePath + "/logs"
            AwLogFileManager.clearAllAsync(logDir) { count ->
                runOnUiThread {
                    appendLog("Cleared $count log files (async)")
                }
            }
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
