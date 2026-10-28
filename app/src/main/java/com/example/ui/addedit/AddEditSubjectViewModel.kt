package com.example.ui.addedit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ExamTrackApplication
import com.example.data.model.Subject
import com.example.notification.NotificationHelper
import com.example.ui.theme.PresetColorHexes
import com.example.util.DateUtils
import com.example.util.ScannedExamCandidate
import com.example.widget.ExamTrackWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class AddEditSubjectUiState(
    val subjectId: String? = null,
    val name: String = "",
    val examDate: Long = 0L,
    val startTime: String = "",
    val endTime: String = "",
    val roomNumber: String = "",
    val examHallBuilding: String = "",
    val seatNumber: String = "",
    val rollNumber: String = "",
    val expectedStudyHours: Int = 8,
    val notes: String = "",
    val referenceLinks: String = "",
    val formulaSheetUri: String = "",
    val selectedColor: String = PresetColorHexes.first(),
    val existingSubjects: List<Subject> = emptyList(),
    val isEditMode: Boolean = false,
    val isSaving: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null
) {
    val isSaveEnabled: Boolean
        get() = name.trim().isNotEmpty() && examDate > 0L
}

class AddEditSubjectViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = (application as ExamTrackApplication).repository
    private val _uiState = MutableStateFlow(AddEditSubjectUiState())
    val uiState: StateFlow<AddEditSubjectUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val all = repository.getAllSubjects()
            _uiState.update { it.copy(existingSubjects = all) }
        }
    }

    fun initialize(subjectId: String?) {
        if (subjectId.isNullOrEmpty()) {
            val nextWeek = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 7)
            }.timeInMillis

            _uiState.update {
                it.copy(
                    subjectId = null,
                    isEditMode = false,
                    examDate = DateUtils.startOfDay(nextWeek),
                    startTime = "10:00 AM",
                    endTime = "01:00 PM",
                    roomNumber = "",
                    examHallBuilding = "",
                    seatNumber = "",
                    rollNumber = "",
                    expectedStudyHours = 8,
                    notes = "",
                    referenceLinks = "",
                    formulaSheetUri = "",
                    selectedColor = PresetColorHexes.first()
                )
            }
        } else {
            viewModelScope.launch {
                val subject = repository.getSubjectById(subjectId)
                if (subject != null) {
                    _uiState.update {
                        it.copy(
                            subjectId = subject.id,
                            name = subject.name,
                            examDate = subject.examDate,
                            startTime = subject.startTime,
                            endTime = subject.endTime,
                            roomNumber = subject.roomNumber,
                            examHallBuilding = subject.examHallBuilding,
                            seatNumber = subject.seatNumber,
                            rollNumber = subject.rollNumber,
                            expectedStudyHours = subject.expectedStudyHours.toInt().coerceAtLeast(1),
                            notes = subject.formulaNotes,
                            referenceLinks = subject.referenceLinks,
                            formulaSheetUri = subject.formulaSheetUri,
                            selectedColor = subject.colorTag,
                            isEditMode = true
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(newName: String) {
        _uiState.update { it.copy(name = newName, errorMessage = null) }
    }

    fun onExamDateChange(newDate: Long) {
        _uiState.update { it.copy(examDate = DateUtils.startOfDay(newDate), errorMessage = null) }
    }

    fun onStartTimeChange(time: String) {
        _uiState.update { it.copy(startTime = time) }
    }

    fun onEndTimeChange(time: String) {
        _uiState.update { it.copy(endTime = time) }
    }

    fun onRoomNumberChange(room: String) {
        _uiState.update { it.copy(roomNumber = room) }
    }

    fun onExamHallBuildingChange(building: String) {
        _uiState.update { it.copy(examHallBuilding = building) }
    }

    fun onSeatNumberChange(seat: String) {
        _uiState.update { it.copy(seatNumber = seat) }
    }

    fun onRollNumberChange(roll: String) {
        _uiState.update { it.copy(rollNumber = roll) }
    }

    fun onExpectedStudyHoursChange(hours: Int) {
        _uiState.update { it.copy(expectedStudyHours = hours.coerceIn(1, 200)) }
    }

    fun onNotesChange(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun onReferenceLinksChange(links: String) {
        _uiState.update { it.copy(referenceLinks = links) }
    }

    fun onColorSelect(hexColor: String) {
        _uiState.update { it.copy(selectedColor = hexColor) }
    }

    fun showDeleteDialog(show: Boolean) {
        _uiState.update { it.copy(showDeleteConfirmation = show) }
    }

    /**
     * Applies scanned Admit Card OCR candidates.
     * - If 1 candidate is selected, populates the form so the user can review or tweak further.
     * - If multiple candidates are selected (and not in single-subject edit mode), imports all
     *   of them into the exam schedule and navigates back.
     */
    fun applyScannedExamCandidates(candidates: List<ScannedExamCandidate>) {
        val selected = candidates
            .filter { it.isSelected && it.isValid }
            .map { it.sanitized() }
        if (selected.isEmpty()) return

        if (selected.size == 1 || _uiState.value.isEditMode) {
            val first = selected.first()
            val scannedDate = first.examDateMillis
            val cleanedDisplayName = when {
                first.subjectName.isNotBlank() &&
                    first.courseCode.isNotBlank() &&
                    !first.subjectName.contains(first.courseCode, ignoreCase = true) ->
                    "${first.courseCode} ${first.subjectName}".trim()
                first.subjectName.isNotBlank() -> first.subjectName
                first.courseCode.isNotBlank() -> first.courseCode
                else -> ""
            }
            _uiState.update { current ->
                current.copy(
                    name = cleanedDisplayName.ifBlank { current.name },
                    examDate = if (scannedDate != null && scannedDate > 0L) DateUtils.startOfDay(scannedDate) else current.examDate,
                    startTime = first.startTime.ifBlank { current.startTime },
                    endTime = first.endTime.ifBlank { current.endTime },
                    roomNumber = first.roomNumber.ifBlank { current.roomNumber },
                    examHallBuilding = first.examHallBuilding.ifBlank { current.examHallBuilding },
                    seatNumber = first.seatNumber.ifBlank { current.seatNumber },
                    rollNumber = first.rollNumber.ifBlank { current.rollNumber },
                    selectedColor = first.colorTag,
                    errorMessage = null
                )
            }
        } else {
            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true) }
                val context = getApplication<Application>()
                val existingList = repository.getAllSubjects()
                for (candidate in selected) {
                    val existingMatch = existingList.firstOrNull {
                        it.name.equals(candidate.subjectName, ignoreCase = true)
                    }
                    if (existingMatch != null) {
                        val updated = existingMatch.copy(
                            examDate = candidate.examDateMillis?.let { DateUtils.startOfDay(it) } ?: existingMatch.examDate,
                            startTime = candidate.startTime.ifBlank { existingMatch.startTime },
                            endTime = candidate.endTime.ifBlank { existingMatch.endTime },
                            roomNumber = candidate.roomNumber.ifBlank { existingMatch.roomNumber },
                            examHallBuilding = candidate.examHallBuilding.ifBlank { existingMatch.examHallBuilding },
                            seatNumber = candidate.seatNumber.ifBlank { existingMatch.seatNumber },
                            rollNumber = candidate.rollNumber.ifBlank { existingMatch.rollNumber }
                        )
                        repository.updateSubject(updated)
                        NotificationHelper.cancelExamReminders(context, updated.id)
                        NotificationHelper.scheduleExamReminders(context, updated)
                    } else {
                        val created = candidate.toSubject()
                        repository.insertSubject(created)
                        NotificationHelper.scheduleExamReminders(context, created)
                    }
                }
                ExamTrackWidgetProvider.updateAllWidgets(context)
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            }
        }
    }

    fun saveSubject() {
        val state = _uiState.value
        val trimmedName = state.name.trim()
        if (trimmedName.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Subject name cannot be empty") }
            return
        }
        if (state.examDate <= 0L) {
            _uiState.update { it.copy(errorMessage = "Please choose an exam date") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val context = getApplication<Application>()

            if (state.isEditMode && state.subjectId != null) {
                val updatedSubject = Subject(
                    id = state.subjectId,
                    name = trimmedName,
                    examDate = state.examDate,
                    colorTag = state.selectedColor,
                    startTime = state.startTime.trim(),
                    endTime = state.endTime.trim(),
                    roomNumber = state.roomNumber.trim(),
                    seatNumber = state.seatNumber.trim(),
                    examHallBuilding = state.examHallBuilding.trim(),
                    rollNumber = state.rollNumber.trim(),
                    formulaSheetUri = state.formulaSheetUri,
                    referenceLinks = state.referenceLinks.trim(),
                    expectedStudyHours = state.expectedStudyHours.toFloat(),
                    formulaNotes = state.notes.trim()
                )
                repository.updateSubject(updatedSubject)

                NotificationHelper.cancelExamReminders(context, updatedSubject.id)
                NotificationHelper.scheduleExamReminders(context, updatedSubject)
            } else {
                val newSubject = Subject(
                    name = trimmedName,
                    examDate = state.examDate,
                    colorTag = state.selectedColor,
                    startTime = state.startTime.trim(),
                    endTime = state.endTime.trim(),
                    roomNumber = state.roomNumber.trim(),
                    seatNumber = state.seatNumber.trim(),
                    examHallBuilding = state.examHallBuilding.trim(),
                    rollNumber = state.rollNumber.trim(),
                    formulaSheetUri = state.formulaSheetUri,
                    referenceLinks = state.referenceLinks.trim(),
                    expectedStudyHours = state.expectedStudyHours.toFloat(),
                    formulaNotes = state.notes.trim()
                )
                repository.insertSubject(newSubject)

                NotificationHelper.scheduleExamReminders(context, newSubject)
            }

            ExamTrackWidgetProvider.updateAllWidgets(context)

            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }

    fun deleteSubject() {
        val state = _uiState.value
        val subjectId = state.subjectId ?: return

        viewModelScope.launch {
            val context = getApplication<Application>()
            NotificationHelper.cancelExamReminders(context, subjectId)
            repository.deleteSubjectById(subjectId)
            ExamTrackWidgetProvider.updateAllWidgets(context)

            _uiState.update {
                it.copy(
                    showDeleteConfirmation = false,
                    isDeleted = true
                )
            }
        }
    }
}
