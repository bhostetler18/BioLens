package com.uf.automoth.ui.sessions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.databinding.FragmentSessionsBinding

class SessionsFragment : Fragment() {

    private var _binding: FragmentSessionsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val sessionsViewModel: SessionsViewModel by viewModels {
            SessionsViewModelFactory(AutoMothRepository)
        }

        _binding = FragmentSessionsBinding.inflate(inflater, container, false)

        val recyclerView = binding.recyclerview
        val adapter = SessionListAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(activity)

        sessionsViewModel.allSessions.observe(viewLifecycleOwner) { sessions ->
            sessions?.let { adapter.submitList(it) }
            binding.noSessionsText.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerview.visibility = if (sessions.isEmpty()) View.GONE else View.VISIBLE
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
