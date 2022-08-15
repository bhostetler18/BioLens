package com.uf.automoth.ui.sessions

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.uf.automoth.data.Session
import com.uf.automoth.databinding.SessionListItemBinding
import com.uf.automoth.ui.imageGrid.ImageGridActivity
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class SessionListAdapter :
    ListAdapter<Session, SessionListAdapter.SessionViewHolder>(SESSION_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = SessionListItemBinding.inflate(inflater, parent, false)
        return SessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current)
    }

    class SessionViewHolder(private val viewBinding: SessionListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {

        fun bind(session: Session) {
            viewBinding.titleView.text = session.name
            viewBinding.infoView.text = dateFormatter.format(session.started)
            viewBinding.root.setOnClickListener {
                val ctx = viewBinding.root.context
                val intent = Intent(ctx, ImageGridActivity::class.java)
                intent.putExtra("SESSION", session.sessionID)
                ctx.startActivity(intent)
            }
        }

        companion object {
            val dateFormatter: DateTimeFormatter =
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        }
    }

    companion object {
        private val SESSION_COMPARATOR = object : DiffUtil.ItemCallback<Session>() {
            override fun areItemsTheSame(oldItem: Session, newItem: Session): Boolean {
                return oldItem === newItem
            }

            override fun areContentsTheSame(oldItem: Session, newItem: Session): Boolean {
                return oldItem == newItem
            }
        }
    }
}
