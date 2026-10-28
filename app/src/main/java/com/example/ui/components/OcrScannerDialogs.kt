package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.ClassRoutineItem
import com.example.ui.routine.DayOfWeek
import com.example.util.DateUtils
import com.example.util.GeminiVisionOcrService
import com.example.util.OcrParser
import com.example.util.OcrScannerHelper
import com.example.util.ScannedAdmitCardCandidate
import com.example.util.ScannedRoutineCandidate
import kotlinx.coroutines.launch

@Composable
fun TimetableOcrScannerDialog(
    existingRoutines: List<ClassRoutineItem>,
    defaultDay: DayOfWeek,
    onDismiss: () -> Unit,
    onConfirmImport: (List<ScannedRoutineCandidate>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentImageUri by remember { mutableStateOf<Uri?>(null) }
    var rotationDegrees by remember { mutableIntStateOf(0) }
    var candidates by remember { mutableStateOf<List<ScannedRoutineCandidate>>(emptyList()) }
    var expandedCandidateId by remember { mutableStateOf<String?>(null) }
    var usedAiVision by remember { mutableStateOf(GeminiVisionOcrService.isApiKeyConfigured()) }
    var hasScanned by remember { mutableStateOf(false) }

    fun applyTimetableScanOutcome(
        extracted: List<ScannedRoutineCandidate>,
        aiUsed: Boolean
    ) {
        hasScanned = true
        usedAiVision = aiUsed
        if (extracted.isEmpty()) {
            errorMessage = "No clear schedule rows detected. Try rotating the image or tap + Add to enter a class."
            val manualRow = ScannedRoutineCandidate(
                subjectName = "",
                dayOfWeek = defaultDay.key,
                isUncertain = true,
                warningMessage = "Unclear image - fill in details to import."
            )
            candidates = listOf(manualRow)
            expandedCandidateId = manualRow.id
        } else {
            candidates = extracted
            expandedCandidateId = extracted.firstOrNull { it.isUncertain || it.subjectName.isBlank() }?.id
        }
    }

    fun runTimetableOcrFromBitmap(bmp: Bitmap, rot: Int) {
        scope.launch {
            isProcessing = true
            errorMessage = null
            val rotated = OcrScannerHelper.rotateBitmap(bmp, rot)
            previewBitmap = rotated
            val result = OcrScannerHelper.scanTimetableFromBitmap(
                bitmap = rotated,
                existingRoutines = existingRoutines,
                defaultDay = defaultDay.key
            )
            isProcessing = false
            result.onSuccess { outcome ->
                applyTimetableScanOutcome(outcome.candidates, outcome.usedAiVision)
            }.onFailure { err ->
                errorMessage = err.localizedMessage ?: "Failed to process timetable image."
            }
        }
    }

    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            rotationDegrees = 0
            currentImageUri = null
            runTimetableOcrFromBitmap(bitmap, 0)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraPreviewLauncher.launch(null)
        } else {
            errorMessage = "Camera permission is required to capture a timetable photo. You can still select an image or PDF."
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            currentImageUri = uri
            rotationDegrees = 0
            scope.launch {
                isProcessing = true
                errorMessage = null
                val bmp = OcrScannerHelper.decodeSampledBitmapFromUri(context, uri)
                if (bmp != null) {
                    previewBitmap = bmp
                }
                val result = OcrScannerHelper.scanTimetableFromImageUri(
                    context = context,
                    uri = uri,
                    existingRoutines = existingRoutines,
                    defaultDay = defaultDay.key,
                    decodedBitmap = bmp
                )
                isProcessing = false
                result.onSuccess { outcome ->
                    applyTimetableScanOutcome(outcome.candidates, outcome.usedAiVision)
                }.onFailure { err ->
                    errorMessage = err.localizedMessage ?: "Could not load the selected image."
                }
            }
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                errorMessage = null
                val res = OcrScannerHelper.scanTimetableFromPdfUri(
                    context = context,
                    uri = uri,
                    existingRoutines = existingRoutines,
                    defaultDay = defaultDay.key
                )
                isProcessing = false
                res.onSuccess { outcome ->
                    applyTimetableScanOutcome(outcome.candidates, outcome.usedAiVision)
                }.onFailure { err ->
                    errorMessage = err.localizedMessage ?: "Unable to extract text from PDF."
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Scan Class Timetable",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (usedAiVision) {
                            "Gemini AI Vision - Tap any row to edit"
                        } else {
                            "Smart Table OCR - Tap any row to edit"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Input Source Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                cameraPreviewLauncher.launch(null)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ocr_camera_btn"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Camera", fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ocr_gallery_btn"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gallery", fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }

                    OutlinedButton(
                        onClick = {
                            pdfPickerLauncher.launch(arrayOf("application/pdf"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ocr_pdf_btn"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF", fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }
                }

                // Image Preview + Rotate button if an image was captured/selected
                previewBitmap?.let { bmp ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Scanned Timetable Preview",
                                modifier = Modifier
                                    .size(width = 76.dp, height = 48.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (usedAiVision) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (usedAiVision) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Text(
                                        text = if (usedAiVision) "AI Vision" else "Table OCR",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (usedAiVision) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        }
                                    )
                                }
                            }
                        }

                        TextButton(
                            onClick = {
                                runTimetableOcrFromBitmap(bmp, 90)
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate 90 degrees", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rotate", fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }

                if (isProcessing) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (GeminiVisionOcrService.isApiKeyConfigured()) {
                                "Extracting timetable with AI Vision..."
                            } else {
                                "Extracting timetable columns..."
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                errorMessage?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Candidate Review List
                if (candidates.isNotEmpty()) {
                    val selectedCount = candidates.count { it.isSelected }
                    val allSelected = selectedCount == candidates.size

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Extracted Classes ($selectedCount/${candidates.size})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Tap a class row to edit its fields",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    val targetState = !allSelected
                                    candidates = candidates.map { it.copy(isSelected = targetState) }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (allSelected) "None" else "All",
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.clickable {
                                    val newRow = ScannedRoutineCandidate(
                                        subjectName = "",
                                        dayOfWeek = defaultDay.key,
                                        startTime = "10:00 AM",
                                        endTime = "11:15 AM"
                                    )
                                    candidates = candidates + newRow
                                    expandedCandidateId = newRow.id
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Add",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(candidates, key = { _, item -> item.id }) { idx, candidate ->
                            val isExpanded = expandedCandidateId == candidate.id
                            CompactRoutineCandidateRow(
                                candidate = candidate,
                                isExpanded = isExpanded,
                                onToggleExpand = {
                                    expandedCandidateId = if (isExpanded) null else candidate.id
                                },
                                onCandidateChange = { updated ->
                                    candidates = candidates.toMutableList().also {
                                        it[idx] = updated
                                    }
                                },
                                onDelete = {
                                    if (expandedCandidateId == candidate.id) {
                                        expandedCandidateId = null
                                    }
                                    candidates = candidates.toMutableList().also { it.removeAt(idx) }
                                }
                            )
                        }
                    }
                } else if (!isProcessing && !hasScanned) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp)
                            )
                            Text(
                                text = "Capture or select a timetable photo or PDF",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Reads multi-column schedules (Day, Time, Subject, Teacher, Room) into compact rows you can review and edit.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            val validSelected = candidates
                .filter { it.isSelected && it.subjectName.isNotBlank() }
                .map { OcrParser.sanitizeRoutineCandidate(it) }
            Button(
                onClick = { onConfirmImport(validSelected) },
                enabled = validSelected.isNotEmpty() && !isProcessing,
                modifier = Modifier.testTag("confirm_ocr_timetable_import_btn")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Import (${validSelected.size})", maxLines = 1, softWrap = false)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CompactRoutineCandidateRow(
    candidate: ScannedRoutineCandidate,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onCandidateChange: (ScannedRoutineCandidate) -> Unit,
    onDelete: () -> Unit
) {
    val shortDay = DayOfWeek.fromKey(candidate.dayOfWeek).shortName

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                candidate.isDuplicate -> Color(0xFFF59E0B).copy(alpha = 0.08f)
                isExpanded -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            1.dp,
            when {
                candidate.isDuplicate || candidate.isUncertain -> Color(0xFFF59E0B)
                isExpanded -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Compact summary header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = candidate.isSelected,
                    onCheckedChange = { checked ->
                        onCandidateChange(candidate.copy(isSelected = checked))
                    },
                    modifier = Modifier.size(36.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = shortDay,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Text(
                            text = candidate.subjectName.ifBlank { "Tap to enter subject" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (candidate.subjectName.isBlank()) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (candidate.courseCode.isNotBlank() &&
                            !candidate.subjectName.equals(candidate.courseCode, ignoreCase = true)
                        ) {
                            Text(
                                text = candidate.courseCode,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    val metaParts = buildList {
                        add("${candidate.startTime} - ${candidate.endTime}")
                        if (candidate.roomNumber.isNotBlank()) add("Rm ${candidate.roomNumber}")
                        if (candidate.teacherName.isNotBlank()) add(candidate.teacherName)
                    }

                    Text(
                        text = metaParts.joinToString("  •  "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.Edit,
                        contentDescription = if (isExpanded) "Collapse fields" else "Edit class fields",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove class row",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            candidate.warningMessage?.let { warn ->
                Text(
                    text = warn,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFB45309),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }

            // Expandable inline editor
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, start = 4.dp, end = 4.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = candidate.subjectName,
                            onValueChange = { newVal ->
                                onCandidateChange(candidate.copy(subjectName = newVal, isUncertain = false))
                            },
                            label = { Text("Subject") },
                            singleLine = true,
                            modifier = Modifier.weight(1.4f)
                        )
                        OutlinedTextField(
                            value = candidate.courseCode,
                            onValueChange = { newVal ->
                                onCandidateChange(candidate.copy(courseCode = newVal))
                            },
                            label = { Text("Code") },
                            singleLine = true,
                            modifier = Modifier.weight(0.8f)
                        )
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(DayOfWeek.entries.size) { dIdx ->
                            val d = DayOfWeek.entries[dIdx]
                            FilterChip(
                                selected = candidate.dayOfWeek.equals(d.key, ignoreCase = true),
                                onClick = {
                                    onCandidateChange(candidate.copy(dayOfWeek = d.key))
                                },
                                label = { Text(d.shortName, fontSize = 11.sp) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = candidate.startTime,
                            onValueChange = { newVal ->
                                onCandidateChange(candidate.copy(startTime = newVal))
                            },
                            label = { Text("Start") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = candidate.endTime,
                            onValueChange = { newVal ->
                                onCandidateChange(candidate.copy(endTime = newVal))
                            },
                            label = { Text("End") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = candidate.roomNumber,
                            onValueChange = { newVal ->
                                onCandidateChange(candidate.copy(roomNumber = newVal))
                            },
                            label = { Text("Room") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = candidate.teacherName,
                            onValueChange = { newVal ->
                                onCandidateChange(candidate.copy(teacherName = newVal))
                            },
                            label = { Text("Teacher") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdmitCardOcrScannerDialog(
    initialSubjectName: String = "",
    existingSubjects: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onConfirmImport: ((List<ScannedAdmitCardCandidate>) -> Unit)? = null,
    onConfirmExams: ((List<ScannedAdmitCardCandidate>) -> Unit)? = null,
    onConfirmAdmitCard: (ScannedAdmitCardCandidate, savedAttachmentPath: String?) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var savedVaultPath by remember { mutableStateOf<String?>(null) }
    var usedAiVision by remember { mutableStateOf(GeminiVisionOcrService.isApiKeyConfigured()) }

    var subjectName by remember { mutableStateOf(initialSubjectName) }
    var courseCode by remember { mutableStateOf("") }
    var examDateText by remember { mutableStateOf("") }
    var parsedExamDateMillis by remember { mutableStateOf<Long?>(null) }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var examHallBuilding by remember { mutableStateOf("") }
    var roomNumber by remember { mutableStateOf("") }
    var seatNumber by remember { mutableStateOf("") }
    var rollNumber by remember { mutableStateOf("") }
    var registrationNumber by remember { mutableStateOf("") }
    var uncertainFields by remember { mutableStateOf<List<String>>(emptyList()) }
    var hasScanned by remember { mutableStateOf(false) }

    fun applyParsedAdmitCard(parsed: ScannedAdmitCardCandidate, aiUsed: Boolean) {
        val cleaned = parsed.sanitized()
        hasScanned = true
        usedAiVision = aiUsed
        if (cleaned.subjectName.isNotBlank()) subjectName = cleaned.subjectName
        if (cleaned.courseCode.isNotBlank()) courseCode = cleaned.courseCode
        if (cleaned.examDateText.isNotBlank()) examDateText = cleaned.examDateText
        if (cleaned.examDateMillis != null) parsedExamDateMillis = cleaned.examDateMillis
        if (cleaned.startTime.isNotBlank()) startTime = cleaned.startTime
        if (cleaned.endTime.isNotBlank()) endTime = cleaned.endTime
        if (cleaned.examHallBuilding.isNotBlank()) examHallBuilding = cleaned.examHallBuilding
        if (cleaned.roomNumber.isNotBlank()) roomNumber = cleaned.roomNumber
        if (cleaned.seatNumber.isNotBlank()) seatNumber = cleaned.seatNumber
        if (cleaned.rollNumber.isNotBlank()) rollNumber = cleaned.rollNumber
        if (cleaned.registrationNumber.isNotBlank()) registrationNumber = cleaned.registrationNumber
        uncertainFields = cleaned.uncertainFields
    }

    fun processAdmitCardBitmap(bmp: Bitmap, rot: Int) {
        scope.launch {
            isProcessing = true
            errorMessage = null
            val rotated = OcrScannerHelper.rotateBitmap(bmp, rot)
            previewBitmap = rotated
            savedVaultPath = OcrScannerHelper.saveBitmapToVaultStorage(context, rotated, "admit_card")
            val result = OcrScannerHelper.scanAdmitCardFromBitmap(rotated)
            isProcessing = false
            result.onSuccess { outcome ->
                applyParsedAdmitCard(outcome.candidate, outcome.usedAiVision)
                if (!outcome.usedAiVision && outcome.rawText.isBlank()) {
                    errorMessage = "Text was unclear. Try rotating the image or fill in the fields below."
                }
            }.onFailure { err ->
                errorMessage = err.localizedMessage ?: "Failed to scan admit card."
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            processAdmitCardBitmap(bmp, 0)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(null)
        } else {
            errorMessage = "Camera permission denied. You can still select an image or PDF."
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                errorMessage = null
                val bmp = OcrScannerHelper.decodeSampledBitmapFromUri(context, uri)
                if (bmp != null) {
                    previewBitmap = bmp
                    savedVaultPath = OcrScannerHelper.saveBitmapToVaultStorage(context, bmp, "admit_card")
                }
                val result = OcrScannerHelper.scanAdmitCardFromImageUri(context, uri, bmp)
                isProcessing = false
                result.onSuccess { outcome ->
                    applyParsedAdmitCard(outcome.candidate, outcome.usedAiVision)
                    if (!outcome.usedAiVision && outcome.rawText.isBlank()) {
                        errorMessage = "No readable text found. Try rotating or enter details below."
                    }
                }.onFailure { err ->
                    errorMessage = err.localizedMessage ?: "Unable to decode selected image."
                }
            }
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                errorMessage = null
                savedVaultPath = OcrScannerHelper.copyAttachmentToVaultStorage(context, uri, "admit_card_pdf", "pdf")
                val res = OcrScannerHelper.scanAdmitCardFromPdfUri(context, uri)
                isProcessing = false
                res.onSuccess { outcome ->
                    applyParsedAdmitCard(outcome.candidate, outcome.usedAiVision)
                }.onFailure { err ->
                    errorMessage = err.localizedMessage ?: "Could not extract text from PDF."
                }
            }
        }
    }

    val isDuplicateSubject = remember(subjectName, existingSubjects) {
        subjectName.isNotBlank() && existingSubjects.any { it.equals(subjectName.trim(), ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Scan Exam / Admit Card",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (usedAiVision) {
                            "Gemini AI Vision - Extracts Subject, Date, Time & Room"
                        } else {
                            "Extracts Subject, Date, Time & Room"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) cameraLauncher.launch(null)
                            else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Camera", fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gallery", fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }

                    OutlinedButton(
                        onClick = {
                            pdfPickerLauncher.launch(arrayOf("application/pdf"))
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF", fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }
                }

                previewBitmap?.let { bmp ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Admit Card Preview",
                            modifier = Modifier
                                .size(width = 88.dp, height = 56.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )

                        TextButton(onClick = { processAdmitCardBitmap(bmp, 90) }) {
                            Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rotate 90°", fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }

                if (isProcessing) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (GeminiVisionOcrService.isApiKeyConfigured()) {
                                "Analyzing admit card with AI Vision..."
                            } else {
                                "Scanning text..."
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (hasScanned && uncertainFields.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.14f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Please verify: ${uncertainFields.joinToString(", ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB45309),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                if (isDuplicateSubject) {
                    Text(
                        text = "Note: '$subjectName' already exists - confirming will update it.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { subjectName = it },
                    label = { Text("Subject Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time") },
                        placeholder = { Text("10:00 AM") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time") },
                        placeholder = { Text("01:00 PM") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = roomNumber,
                        onValueChange = { roomNumber = it },
                        label = { Text("Room") },
                        placeholder = { Text("Room 302") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = courseCode,
                        onValueChange = { courseCode = it },
                        label = { Text("Course Code") },
                        placeholder = { Text("Optional") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (parsedExamDateMillis != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Exam Date: ${DateUtils.formatFullDate(parsedExamDateMillis!!)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanedSubject = OcrParser.cleanSubjectField(subjectName)
                    val cleanedCode = OcrParser.cleanCourseCodeField(courseCode)
                    val candidate = ScannedAdmitCardCandidate(
                        subjectName = cleanedSubject.ifBlank { cleanedCode.ifBlank { "Scanned Exam" } },
                        courseCode = cleanedCode,
                        examDateMillis = parsedExamDateMillis,
                        examDateText = examDateText.trim(),
                        startTime = startTime.trim(),
                        endTime = endTime.trim(),
                        examHallBuilding = examHallBuilding.trim(),
                        roomNumber = roomNumber.trim(),
                        seatNumber = seatNumber.trim(),
                        rollNumber = rollNumber.trim(),
                        registrationNumber = registrationNumber.trim(),
                        isSelected = true
                    ).sanitized()
                    onConfirmAdmitCard(candidate, savedVaultPath)
                    onConfirmImport?.invoke(listOf(candidate))
                    onConfirmExams?.invoke(listOf(candidate))
                },
                enabled = !isProcessing,
                modifier = Modifier.testTag("confirm_admit_card_ocr_btn")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
