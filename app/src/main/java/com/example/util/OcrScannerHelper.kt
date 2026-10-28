package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.max

import com.example.data.model.ClassRoutineItem

data class TimetableScanOutcome(
    val candidates: List<ScannedRoutineCandidate>,
    val usedAiVision: Boolean,
    val rawText: String = ""
)

data class AdmitCardScanOutcome(
    val candidate: ScannedAdmitCardCandidate,
    val usedAiVision: Boolean,
    val rawText: String = ""
)

object OcrScannerHelper {

    /**
     * Extracts timetable rows from a Bitmap using Gemini AI Vision first (if API key is configured),
     * automatically falling back to on-device ML Kit + column-aware OcrParser when offline or unconfigured.
     */
    suspend fun scanTimetableFromBitmap(
        bitmap: Bitmap,
        existingRoutines: List<ClassRoutineItem>,
        defaultDay: String = "MONDAY"
    ): Result<TimetableScanOutcome> = withContext(Dispatchers.IO) {
        if (GeminiVisionOcrService.isApiKeyConfigured()) {
            val aiResult = GeminiVisionOcrService.extractTimetableFromBitmaps(
                bitmaps = listOf(bitmap),
                existingRoutines = existingRoutines,
                defaultDay = defaultDay
            )
            if (aiResult.isSuccess) {
                val aiCandidates = aiResult.getOrNull().orEmpty()
                if (aiCandidates.isNotEmpty()) {
                    return@withContext Result.success(
                        TimetableScanOutcome(
                            candidates = aiCandidates,
                            usedAiVision = true
                        )
                    )
                }
            }
        }

        val textResult = recognizeTextFromBitmap(bitmap, 0)
        textResult.map { rawText ->
            val parsed = if (rawText.isBlank()) {
                emptyList()
            } else {
                OcrParser.parseTimetableText(
                    rawText = rawText,
                    existingRoutines = existingRoutines,
                    defaultDay = defaultDay
                )
            }
            TimetableScanOutcome(
                candidates = parsed,
                usedAiVision = false,
                rawText = rawText
            )
        }
    }

    /**
     * Extracts timetable rows from a Gallery image URI using Gemini AI Vision first (if configured),
     * falling back to EXIF-aware ML Kit + column-aware OcrParser.
     */
    suspend fun scanTimetableFromImageUri(
        context: Context,
        uri: Uri,
        existingRoutines: List<ClassRoutineItem>,
        defaultDay: String = "MONDAY",
        decodedBitmap: Bitmap? = null
    ): Result<TimetableScanOutcome> = withContext(Dispatchers.IO) {
        val bmp = decodedBitmap ?: decodeSampledBitmapFromUri(context, uri, maxDimension = 2200)
        if (bmp != null && GeminiVisionOcrService.isApiKeyConfigured()) {
            val aiResult = GeminiVisionOcrService.extractTimetableFromBitmaps(
                bitmaps = listOf(bmp),
                existingRoutines = existingRoutines,
                defaultDay = defaultDay
            )
            if (aiResult.isSuccess) {
                val aiCandidates = aiResult.getOrNull().orEmpty()
                if (aiCandidates.isNotEmpty()) {
                    return@withContext Result.success(
                        TimetableScanOutcome(
                            candidates = aiCandidates,
                            usedAiVision = true
                        )
                    )
                }
            }
        }

        val textResult = recognizeTextFromImageUri(context, uri, 0)
        textResult.map { rawText ->
            val parsed = if (rawText.isBlank()) {
                emptyList()
            } else {
                OcrParser.parseTimetableText(
                    rawText = rawText,
                    existingRoutines = existingRoutines,
                    defaultDay = defaultDay
                )
            }
            TimetableScanOutcome(
                candidates = parsed,
                usedAiVision = false,
                rawText = rawText
            )
        }
    }

    /**
     * Extracts timetable rows from a PDF document using Gemini AI Vision first (if API key is configured),
     * automatically falling back to on-device ML Kit + column-aware OcrParser.
     */
    suspend fun scanTimetableFromPdfUri(
        context: Context,
        uri: Uri,
        existingRoutines: List<ClassRoutineItem>,
        defaultDay: String = "MONDAY"
    ): Result<TimetableScanOutcome> = withContext(Dispatchers.IO) {
        try {
            val pageBitmaps = renderPdfPagesToBitmaps(context, uri, maxPages = 3)
            if (pageBitmaps.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("The selected PDF has no readable pages."))
            }

            if (GeminiVisionOcrService.isApiKeyConfigured()) {
                val aiResult = GeminiVisionOcrService.extractTimetableFromBitmaps(
                    bitmaps = pageBitmaps,
                    existingRoutines = existingRoutines,
                    defaultDay = defaultDay
                )
                if (aiResult.isSuccess) {
                    val aiCandidates = aiResult.getOrNull().orEmpty()
                    if (aiCandidates.isNotEmpty()) {
                        return@withContext Result.success(
                            TimetableScanOutcome(
                                candidates = aiCandidates,
                                usedAiVision = true
                            )
                        )
                    }
                }
            }

            val allText = StringBuilder()
            for (bmp in pageBitmaps) {
                recognizeTextFromBitmap(bmp, 0).getOrNull()?.let { pageText ->
                    if (pageText.isNotBlank()) allText.appendLine(pageText)
                }
            }
            val rawText = allText.toString().trim()
            val parsed = if (rawText.isBlank()) {
                emptyList()
            } else {
                OcrParser.parseTimetableText(
                    rawText = rawText,
                    existingRoutines = existingRoutines,
                    defaultDay = defaultDay
                )
            }
            Result.success(
                TimetableScanOutcome(
                    candidates = parsed,
                    usedAiVision = false,
                    rawText = rawText
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Extracts admit card details from a Bitmap using Gemini AI Vision first (if API key is configured),
     * automatically falling back to on-device ML Kit + OcrParser.
     */
    suspend fun scanAdmitCardFromBitmap(
        bitmap: Bitmap
    ): Result<AdmitCardScanOutcome> = withContext(Dispatchers.IO) {
        if (GeminiVisionOcrService.isApiKeyConfigured()) {
            val aiResult = GeminiVisionOcrService.extractAdmitCardFromBitmaps(listOf(bitmap))
            if (aiResult.isSuccess) {
                aiResult.getOrNull()?.let { candidate ->
                    if (candidate.subjectName.isNotBlank() || candidate.courseCode.isNotBlank()) {
                        return@withContext Result.success(
                            AdmitCardScanOutcome(
                                candidate = candidate,
                                usedAiVision = true
                            )
                        )
                    }
                }
            }
        }

        val textResult = recognizeTextFromBitmap(bitmap, 0)
        textResult.map { rawText ->
            AdmitCardScanOutcome(
                candidate = OcrParser.parseAdmitCardText(rawText),
                usedAiVision = false,
                rawText = rawText
            )
        }
    }

    /**
     * Extracts admit card details from a Gallery image URI using Gemini AI Vision first (if configured),
     * falling back to EXIF-aware ML Kit + OcrParser.
     */
    suspend fun scanAdmitCardFromImageUri(
        context: Context,
        uri: Uri,
        decodedBitmap: Bitmap? = null
    ): Result<AdmitCardScanOutcome> = withContext(Dispatchers.IO) {
        val bmp = decodedBitmap ?: decodeSampledBitmapFromUri(context, uri, maxDimension = 2200)
        if (bmp != null && GeminiVisionOcrService.isApiKeyConfigured()) {
            val aiResult = GeminiVisionOcrService.extractAdmitCardFromBitmaps(listOf(bmp))
            if (aiResult.isSuccess) {
                aiResult.getOrNull()?.let { candidate ->
                    if (candidate.subjectName.isNotBlank() || candidate.courseCode.isNotBlank()) {
                        return@withContext Result.success(
                            AdmitCardScanOutcome(
                                candidate = candidate,
                                usedAiVision = true
                            )
                        )
                    }
                }
            }
        }

        val textResult = recognizeTextFromImageUri(context, uri, 0)
        textResult.map { rawText ->
            AdmitCardScanOutcome(
                candidate = OcrParser.parseAdmitCardText(rawText),
                usedAiVision = false,
                rawText = rawText
            )
        }
    }

    /**
     * Extracts admit card details from a PDF document using Gemini AI Vision first (if configured),
     * with automatic fallback to on-device ML Kit + OcrParser.
     */
    suspend fun scanAdmitCardFromPdfUri(
        context: Context,
        uri: Uri
    ): Result<AdmitCardScanOutcome> = withContext(Dispatchers.IO) {
        try {
            val pageBitmaps = renderPdfPagesToBitmaps(context, uri, maxPages = 2)
            if (pageBitmaps.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("The selected PDF has no readable pages."))
            }

            if (GeminiVisionOcrService.isApiKeyConfigured()) {
                val aiResult = GeminiVisionOcrService.extractAdmitCardFromBitmaps(pageBitmaps)
                if (aiResult.isSuccess) {
                    aiResult.getOrNull()?.let { candidate ->
                        if (candidate.subjectName.isNotBlank() || candidate.courseCode.isNotBlank()) {
                            return@withContext Result.success(
                                AdmitCardScanOutcome(
                                    candidate = candidate,
                                    usedAiVision = true
                                )
                            )
                        }
                    }
                }
            }

            val allText = StringBuilder()
            for (bmp in pageBitmaps) {
                recognizeTextFromBitmap(bmp, 0).getOrNull()?.let { pageText ->
                    if (pageText.isNotBlank()) allText.appendLine(pageText)
                }
            }
            val rawText = allText.toString().trim()
            Result.success(
                AdmitCardScanOutcome(
                    candidate = OcrParser.parseAdmitCardText(rawText),
                    usedAiVision = false,
                    rawText = rawText
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun renderPdfPagesToBitmaps(
        context: Context,
        uri: Uri,
        maxPages: Int = 3
    ): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return emptyList()
        pfd.use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                val pageCount = renderer.pageCount.coerceAtMost(maxPages)
                for (pageIndex in 0 until pageCount) {
                    renderer.openPage(pageIndex).use { page ->
                        val scale = 2
                        val width = (page.width * scale).coerceIn(600, 2400)
                        val height = (page.height * scale).coerceIn(600, 3200)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmaps.add(bitmap)
                    }
                }
            }
        }
        return bitmaps
    }

    private fun ensureSoftwareBitmap(bitmap: Bitmap): Bitmap {
        return try {
            val needsCopy = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
                bitmap.config == Bitmap.Config.HARDWARE) || bitmap.config != Bitmap.Config.ARGB_8888
            if (needsCopy) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
            } else {
                bitmap
            }
        } catch (_: Exception) {
            bitmap
        }
    }

    /**
     * Recognizes text from a Bitmap using on-device Google ML Kit Text Recognition.
     * Supports rotationDegrees (0, 90, 180, 270) to handle rotated captures.
     */
    suspend fun recognizeTextFromBitmap(
        bitmap: Bitmap,
        rotationDegrees: Int = 0
    ): Result<String> = withContext(Dispatchers.Default) {
        try {
            val safeBitmap = ensureSoftwareBitmap(bitmap)
            val safeRotation = ((rotationDegrees % 360) + 360) % 360
            val image = InputImage.fromBitmap(safeBitmap, safeRotation)
            recognizeInputImage(image)
        } catch (e: Throwable) {
            Result.failure(
                IllegalStateException(
                    e.localizedMessage ?: "Failed to analyze image with on-device OCR.",
                    e
                )
            )
        }
    }

    /**
     * Recognizes text from an image Uri (Gallery or Camera capture).
     * Uses an EXIF-corrected ARGB_8888 bitmap first for reliable cross-device ML Kit recognition,
     * with InputImage.fromFilePath as a secondary fallback.
     */
    suspend fun recognizeTextFromImageUri(
        context: Context,
        uri: Uri,
        rotationDegrees: Int = 0
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bitmap = decodeSampledBitmapFromUri(context, uri, maxDimension = 2200)
            if (bitmap != null) {
                val rotatedBitmap = if (rotationDegrees != 0) rotateBitmap(bitmap, rotationDegrees) else bitmap
                val bitmapResult = recognizeTextFromBitmap(rotatedBitmap, 0)
                if (bitmapResult.isSuccess) {
                    return@withContext bitmapResult
                }
            }
            if (rotationDegrees % 360 == 0) {
                try {
                    val directImage = InputImage.fromFilePath(context, uri)
                    val directResult = recognizeInputImage(directImage)
                    if (directResult.isSuccess) {
                        return@withContext directResult
                    }
                } catch (_: Throwable) {
                    // Handled below
                }
            }
            Result.failure(IllegalArgumentException("Could not decode or recognize the selected image file."))
        } catch (e: Throwable) {
            Result.failure(
                IllegalStateException(
                    e.localizedMessage ?: "Could not process the selected image.",
                    e
                )
            )
        }
    }

    /**
     * Extracts up to 3 pages from a PDF document using Android's PdfRenderer and runs on-device OCR.
     */
    suspend fun recognizeTextFromPdfUri(
        context: Context,
        uri: Uri
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val pageBitmaps = renderPdfPagesToBitmaps(context, uri, maxPages = 3)
            if (pageBitmaps.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("The selected PDF has no pages."))
            }
            val allText = StringBuilder()
            for (bitmap in pageBitmaps) {
                val pageResult = recognizeTextFromBitmap(bitmap, 0)
                pageResult.getOrNull()?.let { text ->
                    if (text.isNotBlank()) {
                        allText.appendLine(text)
                    }
                }
            }
            Result.success(allText.toString().trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun recognizeInputImage(image: InputImage): Result<String> =
        suspendCancellableCoroutine { cont ->
            try {
                val options = TextRecognizerOptions.DEFAULT_OPTIONS
                val recognizer = TextRecognition.getClient(options)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        try {
                            recognizer.close()
                        } catch (_: Throwable) {
                        }
                        if (cont.isActive) {
                            val structured = if (visionText != null) {
                                extractSpatiallySortedText(visionText)
                            } else {
                                ""
                            }
                            cont.resume(Result.success(structured))
                        }
                    }
                    .addOnFailureListener { e ->
                        try {
                            recognizer.close()
                        } catch (_: Throwable) {
                        }
                        if (cont.isActive) {
                            cont.resume(Result.failure(e))
                        }
                    }
            } catch (e: Throwable) {
                if (cont.isActive) {
                    cont.resume(
                        Result.failure(
                            IllegalStateException(
                                e.localizedMessage ?: "Failed to initialize on-device text recognizer.",
                                e
                            )
                        )
                    )
                }
            }
        }

    /**
     * Sorts ML Kit recognized lines top-to-bottom and groups horizontally aligned table columns
     * on the same line (separated by " | ") so multi-column admit cards and routines parse cleanly.
     */
    private fun extractSpatiallySortedText(visionText: Text): String {
        val allLines = visionText.textBlocks.flatMap { it.lines }
        if (allLines.isEmpty()) return visionText.text.trim()

        val linesWithBounds = allLines.filter { it.boundingBox != null && it.text.isNotBlank() }
        if (linesWithBounds.isEmpty()) return visionText.text.trim()

        val sortedByY = linesWithBounds.sortedWith(
            compareBy<Text.Line> { it.boundingBox!!.centerY() }
                .thenBy { it.boundingBox!!.left }
        )

        val groupedRows = mutableListOf<MutableList<Text.Line>>()
        for (line in sortedByY) {
            val box = line.boundingBox!!
            val lineCenterY = box.centerY()
            val lineHeight = max(16, box.height())

            val matchingRow = groupedRows.FirstOrClosestRow(lineCenterY, lineHeight)

            if (matchingRow != null) {
                matchingRow.add(line)
            } else {
                groupedRows.add(mutableListOf(line))
            }
        }

        val sb = StringBuilder()
        for (row in groupedRows) {
            row.sortBy { it.boundingBox?.left ?: 0 }
            val joinedRow = row.joinToString(" | ") { it.text.trim() }
            if (joinedRow.isNotBlank()) {
                sb.appendLine(joinedRow)
            }
        }

        return sb.toString().trim().ifBlank { visionText.text.trim() }
    }

    private fun List<MutableList<Text.Line>>.FirstOrClosestRow(
        lineCenterY: Int,
        lineHeight: Int
    ): MutableList<Text.Line>? {
        var bestRow: MutableList<Text.Line>? = null
        var bestDelta = Int.MAX_VALUE
        for (row in this) {
            val refHeight = row.maxOfOrNull { max(16, it.boundingBox!!.height()) } ?: lineHeight
            val refCenterY = row.map { it.boundingBox!!.centerY() }.average().toInt()
            val tolerance = max(16, (max(lineHeight, refHeight) * 0.65f).toInt())
            val delta = abs(lineCenterY - refCenterY)
            if (delta <= tolerance && delta < bestDelta) {
                bestDelta = delta
                bestRow = row
            }
        }
        return bestRow
    }

    fun rotateBitmap(source: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return source
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun readExifRotationDegrees(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (_: Exception) {
            0
        }
    }

    fun decodeSampledBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = 2200): Bitmap? {
        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            var sampleSize = 1
            while (boundsOptions.outWidth / sampleSize > maxDimension ||
                boundsOptions.outHeight / sampleSize > maxDimension
            ) {
                sampleSize *= 2
            }
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            val oriented = if (readExifRotationDegrees(context, uri) != 0) {
                rotateBitmap(rawBitmap, readExifRotationDegrees(context, uri))
            } else {
                rawBitmap
            }
            ensureSoftwareBitmap(oriented)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Copies a picked image or PDF attachment into app-private storage (filesDir/vault/)
     * so that the Exam Hall Vault retains reliable local access across device restarts.
     */
    suspend fun copyAttachmentToVaultStorage(
        context: Context,
        sourceUri: Uri,
        prefix: String = "vault_doc",
        extension: String = "jpg"
    ): String? = withContext(Dispatchers.IO) {
        try {
            val vaultDir = File(context.filesDir, "vault")
            if (!vaultDir.exists()) vaultDir.mkdirs()

            val fileName = "${prefix}_${UUID.randomUUID().toString().take(8)}.$extension"
            val destFile = File(vaultDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveBitmapToVaultStorage(
        context: Context,
        bitmap: Bitmap,
        prefix: String = "admit_card"
    ): String? = withContext(Dispatchers.IO) {
        try {
            val vaultDir = File(context.filesDir, "vault")
            if (!vaultDir.exists()) vaultDir.mkdirs()

            val fileName = "${prefix}_${UUID.randomUUID().toString().take(8)}.jpg"
            val destFile = File(vaultDir, fileName)
            FileOutputStream(destFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Synchronous/suspending helper used by SubjectDetailViewModel to copy a selected URI
     * into app-private vault storage and return a file:// or path string.
     */
    fun copyUriToInternalStorage(
        context: Context,
        sourceUri: Uri,
        prefix: String = "vault_attachment"
    ): String? {
        return try {
            val mimeType = context.contentResolver.getType(sourceUri) ?: ""
            val ext = when {
                mimeType.contains("pdf", ignoreCase = true) || sourceUri.toString().endsWith(".pdf", ignoreCase = true) -> "pdf"
                mimeType.contains("png", ignoreCase = true) -> "png"
                else -> "jpg"
            }
            val vaultDir = File(context.filesDir, "vault")
            if (!vaultDir.exists()) vaultDir.mkdirs()

            val fileName = "${prefix}_${UUID.randomUUID().toString().take(8)}.$ext"
            val destFile = File(vaultDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
