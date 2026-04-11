# aw-log

Logging utility library based on Timber. Provides structured logging with debug tree, file tree, crash tree, and log file management.

## Installation

Add the dependency in your module-level `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.answufeng:aw-log:1.0.0")
}
```

Make sure you have the JitPack repository in your root `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

## Features

- BrickDebugTree: Logcat output with auto tag and caller location
- FileTree: File logging with date rotation, size limit, and async writing
- CrashTree: Error/Assert level capture with custom crash handler callback
- LogFileManager: Compress, export, and clean log files
- JSON formatting output
- Lambda lazy evaluation for disabled logs
- DSL configuration

## Usage

```kotlin
// Initialize
BrickLogger.init {
    debug = BuildConfig.DEBUG
    fileLog = true
    fileDir = "${cacheDir.absolutePath}/logs"
    maxFileSize = 5L * 1024 * 1024
    maxFileCount = 10
    crashLog = true
    crashHandler = { tag, throwable, message -> /* report to Firebase */ }
}

// Usage
BrickLogger.d("Request succeeded")
BrickLogger.e(exception, "Request failed")
BrickLogger.json(jsonString, "API")
BrickLogger.d("Network") { "Response: ${response.body}" }

// File management
LogFileManager.compressOldLogs(logDir)
LogFileManager.exportLogs(logDir, outputFile)
```

## License

Apache License 2.0. See [LICENSE](LICENSE) for details.
