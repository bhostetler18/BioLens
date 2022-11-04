package com.uf.automoth.ui.imaging.scheduler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.uf.automoth.R
import com.uf.automoth.data.PendingSession
import com.uf.automoth.databinding.PendingSessionListItemBinding
import com.uf.automoth.imaging.ImagingScheduler
import com.uf.automoth.ui.imaging.autoStopDescription
import com.uf.automoth.ui.imaging.intervalDescription
import com.uf.automoth.utility.SHORT_DATE_TIME_FORMATTER

class PendingSessionListAdapter(private val scheduler: ImagingScheduler) :
    ListAdapter<PendingSession, PendingSessionListAdapter.PendingSessionViewHolder>(
        SESSION_COMPARATOR
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendingSessionViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = PendingSessionListItemBinding.inflate(inflater, parent, false)
        return PendingSessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PendingSessionViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current)
    }

    inner class PendingSessionViewHolder(private val viewBinding: PendingSessionListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {

        fun bind(session: PendingSession) {
            val context = viewBinding.root.context
            viewBinding.sessionTitle.text = session.name
            viewBinding.startTimeText.text =
                "${context.getString(R.string.starts)}: ${
                session.scheduledDateTime.format(
                    SHORT_DATE_TIME_FORMATTER
                )
                }"
            viewBinding.intervalText.text =
                "${context.getString(R.string.interval)}: ${
                intervalDescription(
                    session.interval,
                    context,
                    true
                )
                }"
            viewBinding.autoStopText.text = context.getString(R.string.auto_stop) +
                ": " + autoStopDescription(
                session.autoStopMode,
                session.autoStopValue,
                context,
                true
            ).lowercase()
            viewBinding.cancelButton.setOnClickListener {
                scheduler.cancelPendingSession(session, context)
            }
        }
    }

    companion object {
        private val SESSION_COMPARATOR = object : DiffUtil.ItemCallback<PendingSession>() {
            override fun areItemsTheSame(
                oldItem: PendingSession,
                newItem: PendingSession
            ): Boolean {
                return oldItem.requestCode == newItem.requestCode
            }

            override fun areContentsTheSame(
                oldItem: PendingSession,
                newItem: PendingSession
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
