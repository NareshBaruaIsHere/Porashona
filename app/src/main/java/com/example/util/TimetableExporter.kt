package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import com.example.data.model.ClassRoutineItem
import com.example.ui.routine.DayOfWeek
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TimetableExportFormat {
    STANDARD_WEEKLY_IMAGE,
    LOCKSCREEN_WALLPAPER_IMAGE,
    PRINTABLE_PDF
}

data class ExportedTimetableFile(
    val file: File,
    val mimeType: String
)

object TimetableExporter {

    fun renderTimetableBitmap(
        routines: List<ClassRoutineItem>,
        compactLockScreenFormat: Boolean = false
    ): Bitmap {
        return generateTimetableBitmap(
            routines = routines,
            compactLockScreen = compactLockScreenFormat
        )
    }

    fun exportAsPng(
        context: Context,
        routines: List<ClassRoutineItem>,
        compactLockScreen: Boolean = false
    ): Result<ExportedTimetableFile> {
        return try {
            val file = exportTimetableToImageFile(
                context = context,
                routines = routines,
                compactLockScreen = compactLockScreen
            )
            Result.success(ExportedTimetableFile(file = file, mimeType = "image/png"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportAsPdf(
        context: Context,
        routines: List<ClassRoutineItem>
    ): Result<ExportedTimetableFile> {
        return try {
            val file = exportTimetableToPdfFile(
                context = context,
                routines = routines
            )
            Result.success(ExportedTimetableFile(file = file, mimeType = "application/pdf"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareExportedFile(context: Context, exported: ExportedTimetableFile) {
        shareExportedFile(context, exported.file, exported.mimeType)
    }

    /**
     * Renders a high-resolution Bitmap of the weekly timetable.
     * - Weekly Grid (compactLockScreen = false): Professional Multi-Column Timetable Matrix (Sat-Fri columns x Time rows).
     * - Wallpaper (compactLockScreen = true): Minimal Dark OLED Lock-Screen Theme (#06080C) with clock headroom.
     */
    fun generateTimetableBitmap(
        routines: List<ClassRoutineItem>,
        compactLockScreen: Boolean = false,
        selectedDays: List<DayOfWeek> = DayOfWeek.entries
    ): Bitmap {
        val orderedDays = selectedDays.ifEmpty { DayOfWeek.entries }
        return if (compactLockScreen) {
            generateOledWallpaperBitmap(routines, orderedDays)
        } else {
            generateWeeklyMatrixBitmap(routines, orderedDays)
        }
    }

    /**
     * Generates a printable PDF file containing the weekly timetable.
     */
    fun exportTimetableToPdfFile(
        context: Context,
        routines: List<ClassRoutineItem>,
        selectedDays: List<DayOfWeek> = DayOfWeek.entries
    ): File {
        val bitmap = generateTimetableBitmap(
            routines = routines,
            compactLockScreen = false,
            selectedDays = selectedDays
        )

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
        pdfDocument.finishPage(page)

        val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val file = File(exportDir, "Porashona_Timetable_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }

    /**
     * Saves a generated timetable PNG image to cache/exports for sharing or saving.
     */
    fun exportTimetableToImageFile(
        context: Context,
        routines: List<ClassRoutineItem>,
        compactLockScreen: Boolean = false,
        selectedDays: List<DayOfWeek> = DayOfWeek.entries
    ): File {
        val bitmap = generateTimetableBitmap(
            routines = routines,
            compactLockScreen = compactLockScreen,
            selectedDays = selectedDays
        )
        val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val suffix = if (compactLockScreen) "Wallpaper" else "WeeklyGrid"
        val file = File(exportDir, "Porashona_${suffix}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }

    /**
     * Copies an exported file to a user-chosen document Uri (via CreateDocument launcher).
     */
    fun writeFileToUri(context: Context, sourceFile: File, targetUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Launches Android's native Share Sheet for an exported PNG or PDF file.
     */
    fun shareExportedFile(context: Context, file: File, mimeType: String) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Weekly Class Timetable — Porashona")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share Timetable").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
        }
    }

    private fun generateWeeklyMatrixBitmap(
        routines: List<ClassRoutineItem>,
        days: List<DayOfWeek>
    ): Bitmap {
        val gridDays = days.ifEmpty { DayOfWeek.entries }
        val minStartHour = routines.minOfOrNull {
            val mins = DateUtils.parseTimeToMinutes(it.startTime)
            if (mins > 0) (mins / 60).coerceIn(6, 20) else 8
        } ?: 8
        val maxEndHour = routines.maxOfOrNull {
            val mins = DateUtils.parseTimeToMinutes(it.endTime)
            if (mins > 0) ((mins + 59) / 60).coerceIn(9, 22) else 17
        } ?: 17

        val startHour = minOf(8, minStartHour)
        val endHour = maxOf(17, maxEndHour)
        val totalHours = (endHour - startHour + 1).coerceAtLeast(8)

        val width = 1920
        val padX = 56f
        val topHeaderH = 148f
        val tableHeaderH = 74f
        val hourRowH = 98f
        val bottomPad = 56f
        val totalHeight = (topHeaderH + tableHeaderH + (totalHours * hourRowH) + bottomPad).toInt().coerceAtLeast(1180)

        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
        }
        canvas.drawRect(0f, 0f, width.toFloat(), totalHeight.toFloat(), bgPaint)

        // Header Title & Summary Pill
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("WEEKLY TIMETABLE", padX, 74f, titlePaint)
        val dateStamp = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date())
        canvas.drawText(
            "${routines.size} Scheduled Classes  •  Saturday – Friday  •  $dateStamp",
            padX,
            110f,
            subPaint
        )

        // Outer Matrix Card
        val tableLeft = padX
        val tableTop = topHeaderH
        val tableRight = width - padX
        val tableBottom = tableTop + tableHeaderH + (totalHours * hourRowH)
        val tableRect = RectF(tableLeft, tableTop, tableRight, tableBottom)

        val cardSurfacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(tableRect, 24f, 24f, cardSurfacePaint)

        // Header Row Fill
        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.FILL
        }
        val headerRect = RectF(tableLeft, tableTop, tableRight, tableTop + tableHeaderH)
        canvas.drawRoundRect(headerRect, 24f, 24f, headerBgPaint)
        canvas.drawRect(tableLeft, tableTop + tableHeaderH - 24f, tableRight, tableTop + tableHeaderH, headerBgPaint)

        val timeColW = 152f
        val dayColW = (tableRight - tableLeft - timeColW) / gridDays.size.toFloat()

        val headerTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("TIME", tableLeft + timeColW / 2f, tableTop + 46f, headerTextPaint)

        gridDays.forEachIndexed { index, day ->
            val colCenter = tableLeft + timeColW + (index * dayColW) + (dayColW / 2f)
            canvas.drawText(day.shortName.uppercase(Locale.US), colCenter, tableTop + 46f, headerTextPaint)
        }

        // Hour Rows & Grid Lines
        val gridLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        val timeLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val gridBodyTop = tableTop + tableHeaderH
        for (hIdx in 0 until totalHours) {
            val hour = startHour + hIdx
            val rowTop = gridBodyTop + (hIdx * hourRowH)
            if (hIdx > 0) {
                canvas.drawLine(tableLeft, rowTop, tableRight, rowTop, gridLinePaint)
            }
            val hourLabel = when {
                hour == 0 -> "12:00 AM"
                hour == 12 -> "12:00 PM"
                hour > 12 -> "${hour - 12}:00 PM"
                else -> "$hour:00 AM"
            }
            canvas.drawText(hourLabel, tableLeft + (timeColW / 2f), rowTop + 38f, timeLabelPaint)
        }

        // Vertical Column Lines
        canvas.drawLine(tableLeft + timeColW, tableTop, tableLeft + timeColW, tableBottom, gridLinePaint)
        for (i in 1 until gridDays.size) {
            val colX = tableLeft + timeColW + (i * dayColW)
            canvas.drawLine(colX, tableTop, colX, tableBottom, gridLinePaint)
        }

        // Draw Course Blocks in Each Day Column
        val blockSubjectPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val blockMetaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F1F5F9")
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        gridDays.forEachIndexed { dayIndex, day ->
            val colLeft = tableLeft + timeColW + (dayIndex * dayColW)
            val colRight = colLeft + dayColW
            val dayClasses = routines
                .filter { it.dayOfWeek.equals(day.key, ignoreCase = true) }
                .sortedBy { DateUtils.parseTimeToMinutes(it.startTime) }

            dayClasses.forEach { item ->
                val rawStart = DateUtils.parseTimeToMinutes(item.startTime)
                val rawEnd = DateUtils.parseTimeToMinutes(item.endTime)
                val safeStart = if (rawStart > 0) rawStart else 9 * 60
                val safeEnd = if (rawEnd > safeStart) rawEnd else safeStart + 60

                val clampedStart = safeStart.coerceIn(startHour * 60, endHour * 60 + 45)
                val durationMins = (safeEnd - safeStart).coerceIn(45, 240)

                val topY = gridBodyTop + ((clampedStart - startHour * 60) / 60f) * hourRowH + 5f
                val bottomY = (topY + (durationMins / 60f) * hourRowH - 10f).coerceAtMost(tableBottom - 6f)

                val blockRect = RectF(colLeft + 7f, topY, colRight - 7f, bottomY.coerceAtLeast(topY + 58f))
                val itemColor = try {
                    Color.parseColor(item.colorTag)
                } catch (_: Exception) {
                    Color.parseColor("#0D9488")
                }

                val blockFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = itemColor
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(blockRect, 14f, 14f, blockFill)

                val maxTextW = (blockRect.width() - 22f).coerceAtLeast(60f)
                val titleText = if (item.courseCode.isNotBlank()) {
                    "${item.courseCode} • ${item.subjectName}"
                } else {
                    item.subjectName
                }
                val ellipsizedTitle = TextUtils.ellipsize(
                    titleText,
                    blockSubjectPaint,
                    maxTextW,
                    TextUtils.TruncateAt.END
                ).toString()
                canvas.drawText(ellipsizedTitle, blockRect.left + 11f, blockRect.top + 28f, blockSubjectPaint)

                val timeStr = "${item.startTime} – ${item.endTime}"
                val ellipsizedTime = TextUtils.ellipsize(
                    timeStr,
                    blockMetaPaint,
                    maxTextW,
                    TextUtils.TruncateAt.END
                ).toString()
                canvas.drawText(ellipsizedTime, blockRect.left + 11f, blockRect.top + 52f, blockMetaPaint)

                if (blockRect.height() >= 82f && (item.roomNumber.isNotBlank() || item.teacherName.isNotBlank())) {
                    val locStr = listOf(item.roomNumber, item.teacherName).filter { it.isNotBlank() }.joinToString(" • ")
                    val ellipsizedLoc = TextUtils.ellipsize(
                        locStr,
                        blockMetaPaint,
                        maxTextW,
                        TextUtils.TruncateAt.END
                    ).toString()
                    canvas.drawText(ellipsizedLoc, blockRect.left + 11f, blockRect.top + 74f, blockMetaPaint)
                }
            }
        }

        canvas.drawRoundRect(tableRect, 24f, 24f, borderPaint)
        return bitmap
    }

    private fun generateOledWallpaperBitmap(
        routines: List<ClassRoutineItem>,
        days: List<DayOfWeek>
    ): Bitmap {
        val width = 1080
        val padX = 56f

        val grouped = days.associateWith { day ->
            routines.filter { it.dayOfWeek.equals(day.key, ignoreCase = true) }
                .sortedBy { DateUtils.parseTimeToMinutes(it.startTime) }
        }
        val activeDays = grouped.filterValues { it.isNotEmpty() }.keys.toList().ifEmpty { DayOfWeek.entries }

        // Top clock headroom for lock-screen clock widget
        val topClockHeadroom = 320f
        val headerH = 110f
        var cardsTotalH = 0f
        activeDays.forEach { day ->
            val count = grouped[day]?.size ?: 0
            cardsTotalH += 54f + (count.coerceAtLeast(1) * 86f) + 28f
        }
        val totalHeight = (topClockHeadroom + headerH + cardsTotalH + 120f).toInt().coerceAtLeast(2160)

        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // True Black OLED Background (#06080C)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#06080C")
        }
        canvas.drawRect(0f, 0f, width.toFloat(), totalHeight.toFloat(), bgPaint)

        var currentY = topClockHeadroom

        // Minimal OLED Header
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.06f
        }
        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("WEEKLY SCHEDULE", padX, currentY, titlePaint)
        currentY += 34f
        canvas.drawText("${routines.size} classes  •  Sat – Fri", padX, currentY, subPaint)
        currentY += 44f

        val dayHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#14B8A6")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F1420")
            style = Paint.Style.FILL
        }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        val subjectPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
            textSize = 25f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val timePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 21f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        activeDays.forEach { day ->
            val dayClasses = grouped[day].orEmpty()
            canvas.drawText(day.fullName.uppercase(Locale.US), padX, currentY + 24f, dayHeaderPaint)
            currentY += 38f

            if (dayClasses.isEmpty()) {
                val rect = RectF(padX, currentY, width - padX, currentY + 68f)
                canvas.drawRoundRect(rect, 18f, 18f, cardPaint)
                canvas.drawRoundRect(rect, 18f, 18f, cardBorderPaint)
                canvas.drawText("Free day", padX + 24f, currentY + 42f, metaPaint)
                currentY += 86f
            } else {
                dayClasses.forEach { item ->
                    val rect = RectF(padX, currentY, width - padX, currentY + 76f)
                    canvas.drawRoundRect(rect, 18f, 18f, cardPaint)
                    canvas.drawRoundRect(rect, 18f, 18f, cardBorderPaint)

                    val accentColor = try {
                        Color.parseColor(item.colorTag)
                    } catch (_: Exception) {
                        Color.parseColor("#14B8A6")
                    }
                    dotPaint.color = accentColor
                    val barRect = RectF(padX + 14f, currentY + 18f, padX + 20f, currentY + 58f)
                    canvas.drawRoundRect(barRect, 4f, 4f, dotPaint)

                    val titleWithRoom = buildString {
                        append(item.subjectName)
                        if (item.roomNumber.isNotBlank()) {
                            append("  •  ")
                            append(item.roomNumber)
                        }
                    }
                    val maxTitleW = (width - (padX * 2) - 310f).coerceAtLeast(200f)
                    val ellipsized = TextUtils.ellipsize(
                        titleWithRoom,
                        subjectPaint,
                        maxTitleW,
                        TextUtils.TruncateAt.END
                    ).toString()
                    canvas.drawText(ellipsized, padX + 36f, currentY + 47f, subjectPaint)

                    val timeLabel = "${item.startTime} – ${item.endTime}"
                    canvas.drawText(timeLabel, width - padX - 22f, currentY + 46f, timePaint)

                    currentY += 86f
                }
                currentY += 14f
            }
        }

        return bitmap
    }
}
