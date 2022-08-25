package com.uf.automoth.ui.sessions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.uf.automoth.R
import com.uf.automoth.databinding.FragmentSessionsBinding

class SessionsFragment : Fragment(), MenuProvider {

    private var _binding: FragmentSessionsBinding? = null
    private val viewModel: SessionsViewModel by viewModels()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionsBinding.inflate(inflater, container, false)

        val recyclerView = binding.recyclerView
        val adapter = SessionListAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(activity)

        viewModel.allSessions.observe(viewLifecycleOwner) { sessions ->
            sessions?.let { adapter.submitList(it) }
            binding.noSessionsText.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (sessions.isEmpty()) View.GONE else View.VISIBLE
        }

        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.session_list_menu, menu)
        when (viewModel.sortMode.value) {
            SessionsViewModel.SessionSortMode.ASCENDING -> menu.findItem(R.id.sort_ascending).isChecked =
                true
            SessionsViewModel.SessionSortMode.DESCENDING -> menu.findItem(R.id.sort_descending).isChecked =
                true
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.sort_ascending -> {
                viewModel.sortMode.value = SessionsViewModel.SessionSortMode.ASCENDING
                menuItem.isChecked = true
                true
            }
            R.id.sort_descending -> {
                viewModel.sortMode.value = SessionsViewModel.SessionSortMode.DESCENDING
                menuItem.isChecked = true
                true
            }
            else -> false
        }
    }
}
