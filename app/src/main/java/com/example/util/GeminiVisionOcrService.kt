package com.example.util

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.ClassRoutineItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max

@Serializable
data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

@Serializable
data class GeminiInlineData(
    val mimeType: String,
    val data: String
)

@Serializable
data class GeminiContent(
    val role: String = "user",
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiGenerationConfig(
    val responseMimeType: String = "application/json",
    val responseSchema: GeminiSchema? = null,
    val temperature: Float = 0.1f
)

@Serializable
data class GeminiSchema(
    val type: String,
    val description: String? = null,
    val properties: Map<String, GeminiSchema>? = null,
    val items: GeminiSchema? = null,
    val required: List<String>? = null
)

@Serializable
data class GeminiGenerateContentRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent? = null
)

@Serializable
data class GeminiGenerateContentResponse(
    val candidates: List<GeminiCandidate>? = null
)

@Serializable
data class AiTimetableRowDto(
    val dayOfWeek: String = "MONDAY",
    val startTime: String = "09:00 AM",
    val endTime: String = "10:15 AM",
    val subjectName: String = "",
    val courseCode: String = "",
    val teacherName: String = "",
    val roomNumber: String = ""
)

@Serializable
data class AiTimetableExtractionDto(
    val classes: List<AiTimetableRowDto> = emptyList()
)

@Serializable
data class AiAdmitCardExtractionDto(
    val subjectName: String = "",
    val courseCode: String = "",
    val examDateText: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val examHallBuilding: String = "",
    val roomNumber: String = "",
    val seatNumber: String = "",
    val rollNumber: String = "",
    val registrationNumber: String = ""
)

interface GeminiVisionRestApi {
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiGenerateContentRequest
    ): GeminiGenerateContentResponse
}

object GeminiVisionOcrService {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        isLenient = true
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val api: GeminiVisionRestApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiVisionRestApi::class.java)
    }

    fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY.trim()
        return key.isNotEmpty() &&
            key != "MY_GEMINI_API_KEY" &&
            key != "null" &&
            !key.startsWith("YOUR_", ignoreCase = true)
    }

    suspend fun extractTimetableFromBitmaps(
        bitmaps: List<Bitmap>,
        existingRoutines: List<ClassRoutineItem>,
        defaultDay: String = "MONDAY"
    ): Result<List<ScannedRoutineCandidate>> = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured()) {
            return@withContext Result.failure(IllegalStateException("Gemini API key not configured"))
        }
        if (bitmaps.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No images provided"))
        }

        try {
            val imageParts = bitmaps.take(3).map { bmp ->
                GeminiPart(
                    inlineData = GeminiInlineData(
                        mimeType = "image/jpeg",
                        data = encodeBitmapToBase64Jpeg(bmp)
                    )
                )
            }

            val promptText = """
                Extract all class schedule / timetable rows from this image.
                Rules:
                1. If a Day cell spans multiple rows (merged cell or blank day column under a day), carry forward the dayOfWeek from the row above it.
                2. Normalize dayOfWeek to uppercase full name: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, or SUNDAY. Default to $defaultDay if no day is shown.
                3. Format startTime and endTime in 12-hour format with AM/PM (for example "08:30 AM", "11:00 AM", "01:30 PM").
                4. Keep short subject abbreviations intact (for example "SDP", "DC", "AI", "CCS", "CNL", "SE", "CN", "PME", "SEL") or full course titles. Do not include parenthetical section notes like "(All A - 406)" in subjectName.
                5. Extract teacher/faculty initials or names into teacherName (for example "THA", "FK", "THN", "TASMIN", "KD", "EAS", "TMF").
                6. Extract room numbers into roomNumber (for example "608", "405", "404", "601", "612", "611", "903", "409", "1001").
                7. Ignore table headers like "Day | Time | Subject | Teacher | Room".
            """.trimIndent()

            val schema = GeminiSchema(
                type = "OBJECT",
                properties = mapOf(
                    "classes" to GeminiSchema(
                        type = "ARRAY",
                        items = GeminiSchema(
                            type = "OBJECT",
                            properties = mapOf(
                                "dayOfWeek" to GeminiSchema(type = "STRING"),
                                "startTime" to GeminiSchema(type = "STRING"),
                                "endTime" to GeminiSchema(type = "STRING"),
                                "subjectName" to GeminiSchema(type = "STRING"),
                                "courseCode" to GeminiSchema(type = "STRING"),
                                "teacherName" to GeminiSchema(type = "STRING"),
                                "roomNumber" to GeminiSchema(type = "STRING")
                            ),
                            required = listOf("dayOfWeek", "startTime", "endTime", "subjectName", "roomNumber", "teacherName")
                        )
                    )
                ),
                required = listOf("classes")
            )

            val request = GeminiGenerateContentRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = imageParts + GeminiPart(text = promptText)
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json",
                    responseSchema = schema,
                    temperature = 0.1f
                )
            )

            val response = api.generateContent(
                apiKey = BuildConfig.GEMINI_API_KEY.trim(),
                request = request
            )

            val rawJson = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull { !it.text.isNullOrBlank() }
                ?.text
                ?.trim()
                ?: return@withContext Result.failure(IllegalStateException("Empty response from Gemini Vision"))

            val parsedDto = json.decodeFromString(
                AiTimetableExtractionDto.serializer(),
                cleanJsonEnvelope(rawJson)
            )
            if (parsedDto.classes.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("No timetable rows found by Gemini Vision"))
            }

            val candidates = mutableListOf<ScannedRoutineCandidate>()
            for (dto in parsedDto.classes) {
                val cleanedSubj = OcrParser.cleanSubjectField(dto.subjectName)
                val cleanedCode = OcrParser.cleanCourseCodeField(dto.courseCode)
                val finalSubj = cleanedSubj.ifBlank { cleanedCode }
                if (finalSubj.isBlank()) continue

                val normalizedDay = OcrParser.normalizeDayKey(dto.dayOfWeek, defaultDay)
                val normStart = OcrParser.normalizeTimeString(dto.startTime, "AM").ifBlank { "09:00 AM" }
                val normEnd = OcrParser.normalizeTimeString(dto.endTime, "AM", afterStart = normStart).ifBlank { "10:15 AM" }
                val cleanedTeacher = OcrParser.cleanTeacherField(dto.teacherName)
                val cleanedRoom = OcrParser.cleanRoomField(dto.roomNumber)

                val isDup = OcrParser.isDuplicateRoutine(
                    dayOfWeek = normalizedDay,
                    startTime = normStart,
                    subjectName = finalSubj,
                    existing = existingRoutines,
                    alreadyParsed = candidates
                )

                candidates.add(
                    ScannedRoutineCandidate(
                        subjectName = finalSubj,
                        courseCode = cleanedCode,
                        teacherName = cleanedTeacher,
                        dayOfWeek = normalizedDay,
                        startTime = normStart,
                        endTime = normEnd,
                        roomNumber = cleanedRoom,
                        isUncertain = false,
                        isDuplicate = isDup,
                        warningMessage = if (isDup) "Duplicate entry detected for $normalizedDay at $normStart" else null,
                        isSelected = !isDup
                    )
                )
            }

            if (candidates.isEmpty()) {
                Result.failure(IllegalStateException("No valid classes parsed from Gemini response"))
            } else {
                Result.success(candidates)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun extractAdmitCardFromBitmaps(
        bitmaps: List<Bitmap>
    ): Result<ScannedAdmitCardCandidate> = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured()) {
            return@withContext Result.failure(IllegalStateException("Gemini API key not configured"))
        }
        if (bitmaps.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No images provided"))
        }

        try {
            val imageParts = bitmaps.take(2).map { bmp ->
                GeminiPart(
                    inlineData = GeminiInlineData(
                        mimeType = "image/jpeg",
                        data = encodeBitmapToBase64Jpeg(bmp)
                    )
                )
            }

            val promptText = """
                Extract exam admit card or exam schedule details from this image.
                Return subjectName, courseCode, examDateText (in DD/MM/YYYY format if present), startTime (12-hour AM/PM), endTime (12-hour AM/PM), examHallBuilding, roomNumber, seatNumber, rollNumber, and registrationNumber.
            """.trimIndent()

            val schema = GeminiSchema(
                type = "OBJECT",
                properties = mapOf(
                    "subjectName" to GeminiSchema(type = "STRING"),
                    "courseCode" to GeminiSchema(type = "STRING"),
                    "examDateText" to GeminiSchema(type = "STRING"),
                    "startTime" to GeminiSchema(type = "STRING"),
                    "endTime" to GeminiSchema(type = "STRING"),
                    "examHallBuilding" to GeminiSchema(type = "STRING"),
                    "roomNumber" to GeminiSchema(type = "STRING"),
                    "seatNumber" to GeminiSchema(type = "STRING"),
                    "rollNumber" to GeminiSchema(type = "STRING"),
                    "registrationNumber" to GeminiSchema(type = "STRING")
                ),
                required = listOf("subjectName", "courseCode", "examDateText", "startTime", "endTime", "roomNumber")
            )

            val request = GeminiGenerateContentRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = imageParts + GeminiPart(text = promptText)
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json",
                    responseSchema = schema,
                    temperature = 0.1f
                )
            )

            val response = api.generateContent(
                apiKey = BuildConfig.GEMINI_API_KEY.trim(),
                request = request
            )

            val rawJson = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull { !it.text.isNullOrBlank() }
                ?.text
                ?.trim()
                ?: return@withContext Result.failure(IllegalStateException("Empty response from Gemini Vision"))

            val dto = json.decodeFromString(
                AiAdmitCardExtractionDto.serializer(),
                cleanJsonEnvelope(rawJson)
            )
            val parsedMillis = OcrParser.parseDateStringToMillis(dto.examDateText)
            val uncertain = mutableListOf<String>()
            if (dto.subjectName.isBlank() && dto.courseCode.isBlank()) uncertain.add("Subject Name")
            if (parsedMillis == null) uncertain.add("Exam Date")

            val candidate = ScannedAdmitCardCandidate(
                subjectName = dto.subjectName,
                courseCode = dto.courseCode,
                examDateMillis = parsedMillis,
                examDateText = dto.examDateText,
                startTime = dto.startTime.ifBlank { "10:00 AM" },
                endTime = dto.endTime.ifBlank { "01:00 PM" },
                examHallBuilding = dto.examHallBuilding,
                roomNumber = dto.roomNumber,
                seatNumber = dto.seatNumber,
                rollNumber = dto.rollNumber,
                registrationNumber = dto.registrationNumber,
                uncertainFields = uncertain,
                isSelected = true
            ).sanitized()

            Result.success(candidate)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cleanJsonEnvelope(raw: String): String {
        return raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    private fun encodeBitmapToBase64Jpeg(bitmap: Bitmap, maxDim: Int = 1600): String {
        val scaled = scaleBitmapDown(bitmap, maxDim)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDim: Int): Bitmap {
        val largest = max(bitmap.width, bitmap.height)
        if (largest <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / largest.toFloat()
        val targetW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val targetH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }
}
