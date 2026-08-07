package com.gamefocus.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.gamefocus.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun generateTempFile(fileName: String, mimeType: String, onComplete: (Uri?) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val uri = when {
                    fileName.endsWith(".csv") || mimeType == "text/csv" -> {
                        writeCsvToFile(fileName)
                    }
                    fileName.endsWith(".json") || mimeType == "application/json" -> {
                        val content = generateJsonContent()
                        writeToFile(fileName, content.toByteArray())
                    }
                    fileName.endsWith(".pdf") || mimeType == "application/pdf" -> {
                        generatePdfFile(fileName)
                    }
                    else -> null
                }
                launch(Dispatchers.Main) {
                    onComplete(uri)
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    onComplete(null)
                }
            }
        }
    }

    private suspend fun writeCsvToFile(fileName: String): Uri? {
        val db = AppDatabase.getInstance(context)
        val sessionDao = db.gameSessionDao()
        val sessions = sessionDao.getAllSessions().firstOrNull() ?: emptyList()

        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { fos ->
            BufferedWriter(OutputStreamWriter(fos)).use { writer ->
                writer.write("Package Name,Game Name,Start Time,End Time,Duration (ms),Duration")
                writer.newLine()
                for (session in sessions) {
                    writer.write("${sanitizeCsv(session.packageName)},\"${sanitizeCsv(session.gameName)}\",${session.startTime},${session.endTime},${session.durationMillis},${sanitizeCsv(formatDuration(session.durationMillis))}")
                    writer.newLine()
                }
                writer.flush()
            }
        }
        return android.net.Uri.fromFile(file)
    }

    private fun sanitizeCsv(value: String): String {
        val trimmed = value.trim()
        if (trimmed.startsWith("=") || trimmed.startsWith("+") || trimmed.startsWith("-") || trimmed.startsWith("@") || trimmed.startsWith("\t") || trimmed.startsWith("\r")) {
            return "'$trimmed"
        }
        return trimmed
    }

    fun generateCsvTempFile(onComplete: (Uri?) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val uri = writeCsvToFile("analytics_${getTimestamp()}.csv")
                launch(Dispatchers.Main) {
                    onComplete(uri)
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    onComplete(null)
                }
            }
        }
    }

    fun generateJsonTempFile(onComplete: (Uri?) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val content = generateJsonContent()
                val uri = writeToFile("analytics_${getTimestamp()}.json", content.toByteArray())
                launch(Dispatchers.Main) {
                    onComplete(uri)
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    onComplete(null)
                }
            }
        }
    }

    fun generatePdfTempFile(onComplete: (Uri?) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val uri = generatePdfFile("analytics_${getTimestamp()}.pdf")
                launch(Dispatchers.Main) {
                    onComplete(uri)
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    onComplete(null)
                }
            }
        }
    }

    fun getTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        return sdf.format(Date())
    }

    private suspend fun generateJsonContent(): String {
        val db = AppDatabase.getInstance(context)
        val sessionDao = db.gameSessionDao()
        val sessions = sessionDao.getAllSessions().firstOrNull() ?: emptyList()

        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"exportDate\": \"${getTimestamp()}\",\n")
        sb.append("  \"totalSessions\": ${sessions.size},\n")
        sb.append("  \"sessions\": [\n")
        for (index in sessions.indices) {
            val session = sessions[index]
            sb.append("    {\n")
            sb.append("      \"packageName\": \"${session.packageName}\",\n")
            sb.append("      \"gameName\": \"${session.gameName}\",\n")
            sb.append("      \"startTime\": ${session.startTime},\n")
            sb.append("      \"endTime\": ${session.endTime},\n")
            sb.append("      \"durationMillis\": ${session.durationMillis}\n")
            sb.append("    }")
            if (index < sessions.size - 1) sb.append(",") else sb.append("\n")
            sb.append("\n")
        }
        sb.append("  ]\n")
        sb.append("}\n")
        return sb.toString()
    }

    private suspend fun generatePdfFile(fileName: String): Uri? {
        val db = AppDatabase.getInstance(context)
        val sessionDao = db.gameSessionDao()
        val sessions = sessionDao.getAllSessions().firstOrNull() ?: emptyList()

        val totalPlayTime = sessionDao.getTotalPlayTime() ?: 0L
        val totalSessions = sessionDao.getTotalSessionCount()
        val longestSession = sessionDao.getLongestSession()?.durationMillis ?: 0L
        val dailyPlayTime = sessionDao.getDailyPlayTime(getStartOfDay(System.currentTimeMillis())) ?: 0L
        val weeklyPlayTime = sessionDao.getWeeklyPlayTime(getStartOfWeek(System.currentTimeMillis())) ?: 0L
        val monthlyPlayTime = sessionDao.getMonthlyPlayTime(getStartOfMonth(System.currentTimeMillis())) ?: 0L

        val allSessions = sessions
        val firstSession = allSessions.minByOrNull { it.startTime }
        val daysActive = if (firstSession != null) {
            ((System.currentTimeMillis() - firstSession.startTime) / (24 * 60 * 60 * 1000)) + 1
        } else 1L
        val averageDailyPlayTime = if (daysActive > 0) totalPlayTime / daysActive else 0L

        val streak = calculateStreak(allSessions)

        val topGames = sessionDao.getTopPlayedGames().take(5)

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            isFakeBoldText = true
        }
        val headerPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 16f
            isFakeBoldText = true
        }
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        var y = 40
        canvas.drawText("GameFocus Analytics Report", 40f, y.toFloat(), titlePaint)
        y += 40
        canvas.drawText("Export Date: ${getTimestamp()}", 40f, y.toFloat(), textPaint)
        y += 40
        canvas.drawLine(40f, y.toFloat(), 555f, y.toFloat(), linePaint)
        y += 20

        val stats = listOf(
            "Total Play Time" to formatDuration(totalPlayTime),
            "Daily Play Time" to formatDuration(dailyPlayTime),
            "Weekly Play Time" to formatDuration(weeklyPlayTime),
            "Monthly Play Time" to formatDuration(monthlyPlayTime),
            "Total Sessions" to "$totalSessions",
            "Longest Session" to formatDuration(longestSession),
            "Average Daily Play Time" to formatDuration(averageDailyPlayTime),
            "Gaming Streak" to "$streak days"
        )

        for ((label, value) in stats) {
            canvas.drawText("$label:", 40f, y.toFloat(), headerPaint)
            canvas.drawText(value, 250f, y.toFloat(), textPaint)
            y += 25
        }

        y += 20
        canvas.drawLine(40f, y.toFloat(), 555f, y.toFloat(), linePaint)
        y += 20
        canvas.drawText("Top Games", 40f, y.toFloat(), headerPaint)
        y += 25

        for ((game, millis) in topGames) {
            if (y > 800) break
            canvas.drawText("$game - ${formatDuration(millis)}", 40f, y.toFloat(), textPaint)
            y += 20
        }

        document.finishPage(page)

        return try {
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { fos ->
                document.writeTo(fos)
            }
            document.close()
            android.net.Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
            null
        }
    }

    private fun writeToFile(fileName: String, content: ByteArray): Uri? {
        return try {
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { it.write(content) }
            android.net.Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0 -> String.format("%dh %02dm %02ds", hours, minutes, seconds)
            minutes > 0 -> String.format("%dm %02ds", minutes, seconds)
            else -> String.format("%ds", seconds)
        }
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun getStartOfWeek(timestamp: Long): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun getStartOfMonth(timestamp: Long): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun calculateStreak(sessions: List<com.gamefocus.data.models.GameSession>): Int {
        if (sessions.isEmpty()) return 0

        val calendar = java.util.Calendar.getInstance()
        val today = calendar.apply {
            timeInMillis = System.currentTimeMillis()
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val daySet = sessions.map { session ->
            calendar.timeInMillis = session.startTime
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }.toSet()

        var streak = 0
        var checkDate = today
        while (daySet.contains(checkDate)) {
            streak++
            calendar.timeInMillis = checkDate
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
            checkDate = calendar.timeInMillis
        }

        return streak
    }
}
