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
import com.uf.automoth.utility.SHORT_DATE_TIME_FORMATTER

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
            viewBinding.sessionTitle.text = session.name
            viewBinding.infoView.text = session.started.format(SHORT_DATE_TIME_FORMATTER)
            viewBinding.root.setOnClickListener {
                val ctx = viewBinding.root.context
                val intent = Intent(ctx, ImageGridActivity::class.java)
                intent.putExtra("SESSION", session.sessionID)
                ctx.startActivity(intent)
            }
        }
    }

    companion object {
        private val SESSION_COMPARATOR = object : DiffUtil.ItemCallback<Session>() {
            override fun areItemsTheSame(oldItem: Session, newItem: Session): Boolean {
                return oldItem.sessionID == newItem.sessionID
            }

            override fun areContentsTheSame(oldItem: Session, newItem: Session): Boolean {
                return oldItem == newItem
            }
        }
    }
}
