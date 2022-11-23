/*
 * Copyright (c) 2022 University of Florida
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.uf.automoth.ui.imaging.scheduler

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat.CLOCK_12H
import com.uf.automoth.R
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.PendingSession
import com.uf.automoth.databinding.ActivityImagingSchedulerBinding
import com.uf.automoth.imaging.ImagingScheduler
import com.uf.automoth.ui.imaging.AutoStopDialog
import com.uf.automoth.ui.imaging.IntervalDialog
import com.uf.automoth.ui.imaging.autoStopDescription
import com.uf.automoth.ui.imaging.intervalDescription
import com.uf.automoth.utility.SHORT_TIME_FORMATTER
import com.uf.automoth.utility.formatDateTodayTomorrow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.*

class ImagingSchedulerActivity : AppCompatActivity() {

    private val viewModel: ImagingSchedulerViewModel by viewModels()
    private val imagingScheduler by lazy { ImagingScheduler(this) }
    private val binding by lazy { ActivityImagingSchedulerBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AutoMothRepository.loadDefaultImagingSettings(this)?.let {
            viewModel.imagingSettings = it
        }
        updateIntervalText()
        updateAutoStopText()
        validate()

        viewModel.estimatedImageSizeBytes =
            intent.getDoubleExtra(KEY_ESTIMATED_IMAGE_SIZE_BYTES, 0.0)

        setSupportActionBar(binding.appBar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.schedule_session)

        val recyclerView = binding.recyclerView
        val adapter = PendingSessionListAdapter(imagingScheduler)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        viewModel.allPendingSessions.observe(this) { sessions ->
            sessions?.let { adapter.submitList(it) }
        }

        binding.sessionOptions.intervalContainer.setOnClickListener {
            selectInterval()
        }

        binding.sessionOptions.autoStopContainer.setOnClickListener {
            selectAutoStop()
        }

        binding.sessionOptions.date.setOnClickListener {
            selectDate()
        }

        binding.sessionOptions.time.setOnClickListener {
            selectTime()
        }

        binding.sessionOptions.sessionName.doOnTextChanged { text, _, _, _ ->
            val string = text.toString()
            if (string != "") {
                viewModel.sessionName = string
            } else {
                viewModel.sessionName = null
            }
        }

        binding.sessionOptions.scheduleButton.setOnClickListener {
            tryScheduleSession()
        }

        setContentView(binding.root)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> false
        }
    }

    private fun selectDate() {
        val constraints =
            CalendarConstraints.Builder().setValidator(DateValidatorPointForward.now()).build()
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setCalendarConstraints(constraints)
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
            .build()
        datePicker.addOnPositiveButtonClickListener {
            if (it == null) {
                viewModel.sessionDate = null
                return@addOnPositiveButtonClickListener
            }
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = it
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1 // LocalDate months start with 1 not 0
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val date = LocalDate.of(year, month, day)
            viewModel.sessionDate = date
            binding.sessionOptions.date.text = date.formatDateTodayTomorrow(this)
            validate()
        }
        datePicker.show(supportFragmentManager, null)
    }

    private fun selectTime() {
        val defaultHour = (OffsetDateTime.now().hour + 1) % 24
        val hour = viewModel.sessionTime?.hour ?: defaultHour
        val minute = viewModel.sessionTime?.minute ?: 0
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(CLOCK_12H)
            .setHour(hour)
            .setMinute(minute)
            // I want to do this but there is a ridiculous bug... see https://github.com/material-components/material-components-android/issues/2788
//            .setInputMode(MaterialTimePicker.INPUT_MODE_KEYBOARD)
            .build()
        timePicker.addOnPositiveButtonClickListener {
            val time = LocalTime.of(timePicker.hour, timePicker.minute)
            viewModel.sessionTime = time
            binding.sessionOptions.time.text = time.format(SHORT_TIME_FORMATTER)
            validate()
        }
        timePicker.show(supportFragmentManager, null)
    }

    private fun selectInterval() {
        val dialog = IntervalDialog(
            this,
            viewModel.imagingSettings.interval,
            viewModel.estimatedImageSizeBytes
        ) { interval ->
            viewModel.imagingSettings.interval = interval
            updateIntervalText()
        }
        dialog.show()
    }

    private fun selectAutoStop() {
        val dialog = AutoStopDialog(
            this,
            viewModel.imagingSettings,
            viewModel.estimatedImageSizeBytes
        ) { mode, value ->
            viewModel.imagingSettings.autoStopMode = mode
            value?.let {
                viewModel.imagingSettings.autoStopValue = it
            }
            updateAutoStopText()
        }
        dialog.show()
    }

    private fun validate(): Boolean {
        with(viewModel) {
            val start = scheduleStartTime
            if (start == null) {
                binding.sessionOptions.scheduleButton.isEnabled = false
                binding.sessionOptions.time.setTextColor(
                    ContextCompat.getColor(
                        this@ImagingSchedulerActivity,
                        R.color.dark_grey
                    )
                )
                return false
            }
            if (start <= OffsetDateTime.now()) {
                binding.sessionOptions.scheduleButton.isEnabled = false
                binding.sessionOptions.time.setTextColor(
                    ContextCompat.getColor(
                        this@ImagingSchedulerActivity,
                        R.color.destructive_red
                    )
                )
                return false
            }
        }
        binding.sessionOptions.time.setTextColor(
            ContextCompat.getColor(
                this@ImagingSchedulerActivity,
                R.color.dark_grey
            )
        )
        binding.sessionOptions.scheduleButton.isEnabled = true
        return true
    }

    private fun updateIntervalText() {
        binding.sessionOptions.intervalText.text =
            viewModel.imagingSettings.intervalDescription(this, false)
    }

    private fun updateAutoStopText() {
        binding.sessionOptions.autoStopText.text =
            viewModel.imagingSettings.autoStopDescription(this, false)
    }

    private fun tryScheduleSession() {
        if (!validate()) {
            return
        }

        imagingScheduler.requestExactAlarmPermissionIfNecessary(this)

        val startTime = viewModel.scheduleStartTime!!
        val settings = viewModel.imagingSettings

        lifecycleScope.launch {
            val conflict = ImagingScheduler.checkForSchedulingConflicts(startTime, settings)
            if (conflict != null) {
                runOnUiThread {
                    showSchedulingConflictDialog(conflict) {
                        lifecycleScope.launch {
                            scheduleSession(startTime)
                        }
                    }
                }
            } else {
                scheduleSession(startTime)
            }
        }
    }

    private suspend fun scheduleSession(startTime: OffsetDateTime) {
        imagingScheduler.scheduleSession(
            this@ImagingSchedulerActivity,
            viewModel.sessionName ?: getString(R.string.default_session_name),
            viewModel.imagingSettings,
            startTime
        )
        runOnUiThread {
            // Reset to make it harder to spam a bunch of identical sessions
            viewModel.sessionTime = null
            viewModel.sessionName = null
            binding.sessionOptions.time.text = getString(R.string.time)
        }
    }

    private fun showSchedulingConflictDialog(
        conflict: Pair<ImagingScheduler.SchedulingConflictType, PendingSession>,
        onIgnoreConflict: () -> Unit
    ) {
        val dialogBuilder = MaterialAlertDialogBuilder(this)
        dialogBuilder.setTitle(R.string.scheduling_conflict_dialog_title)
        when (conflict.first) {
            ImagingScheduler.SchedulingConflictType.WILL_CANCEL -> {
                val message = getString(R.string.session_will_cancel, conflict.second.name)
                dialogBuilder.setMessage(message)
            }
            ImagingScheduler.SchedulingConflictType.WILL_BE_CANCELLED -> {
                val message = getString(R.string.session_will_be_cancelled, conflict.second.name)
                dialogBuilder.setMessage(message)
            }
        }
        dialogBuilder.setPositiveButton(R.string.schedule_anyway) { dialog, _ ->
            onIgnoreConflict()
            dialog.dismiss()
        }
        dialogBuilder.setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }
        dialogBuilder.create().show()
    }

    companion object {
        const val KEY_ESTIMATED_IMAGE_SIZE_BYTES = "com.uf.automoth.extra.IMAGE_SIZE_BYTES"
    }
}
