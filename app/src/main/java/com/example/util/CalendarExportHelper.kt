package com.example.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import com.example.data.model.Subject
import com.example.data.model.Topic
import java.util.concurrent.TimeUnit

object CalendarExportHelper {

    /**
     * Builds an Android Calendar ACTION_INSERT Intent pre-populated with exam date,
     * start/end times, location (room, building, seat), and syllabus/vault notes.
     * Requires zero runtime permissions.
     */
    fun createExamCalendarIntent(
        subject: Subject,
        topics: List<Topic> = emptyList()
    ): Intent {
        val startMillis = DateUtils.combineDateAndTime(
            examDateMillis = subject.examDate,
            startTime = subject.startTime,
            defaultHour = 9,
            defaultMinute = 0
        )

        val endMillis = if (subject.endTime.isNotBlank()) {
            val parsedEnd = DateUtils.combineDateAndTime(
                examDateMillis = subject.examDate,
                startTime = subject.endTime,
                defaultHour = 11,
                defaultMinute = 0
            )
            if (parsedEnd > startMillis) parsedEnd else startMillis + TimeUnit.HOURS.toMillis(2)
        } else {
            startMillis + TimeUnit.HOURS.toMillis(2)
        }

        val locationParts = buildList {
            if (subject.roomNumber.isNotBlank()) add("Room ${subject.roomNumber.removePrefix("Room ").removePrefix("Rm ").trim()}")
            if (subject.examHallBuilding.isNotBlank()) add(subject.examHallBuilding.trim())
            if (subject.seatNumber.isNotBlank()) add("Seat ${subject.seatNumber.removePrefix("Seat ").trim()}")
        }
        val locationString = locationParts.joinToString(", ")

        val descriptionParts = buildList {
            add("Exam: ${subject.name}")
            if (subject.examTime.isNotBlank()) add("Time: ${subject.examTime}")
            if (locationString.isNotBlank()) add("Venue: $locationString")
            if (subject.rollNumber.isNotBlank()) add("Roll / ID: ${subject.rollNumber}")
            if (subject.admitCardNumber.isNotBlank()) add("Admit Card: ${subject.admitCardNumber}")
            if (topics.isNotEmpty()) {
                val doneCount = topics.count { it.isDone }
                add("Syllabus Progress: $doneCount/${topics.size} topics completed")
            }
            if (subject.formulaNotes.isNotBlank()) {
                add("Notes: ${subject.formulaNotes}")
            }
        }
        val descriptionString = descriptionParts.joinToString("\n")

        return Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "Exam: ${subject.name}")
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            if (locationString.isNotBlank()) {
                putExtra(CalendarContract.Events.EVENT_LOCATION, locationString)
            }
            putExtra(CalendarContract.Events.DESCRIPTION, descriptionString)
            putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
        }
    }

    /**
     * Launches Google Calendar / system calendar event creation sheet for an exam.
     */
    fun exportExamToCalendar(
        context: Context,
        subject: Subject,
        topics: List<Topic> = emptyList()
    ): Boolean {
        val intent = createExamCalendarIntent(subject, topics)
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "No Calendar app found on this device",
                Toast.LENGTH_SHORT
            ).show()
            false
        } catch (_: Exception) {
            false
        }
    }
}
