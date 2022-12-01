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

package com.uf.biolens.ui.sessions

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
import com.uf.biolens.R
import com.uf.biolens.databinding.FragmentSessionsBinding

class SessionsFragment : Fragment(), MenuProvider {

    private var _binding: FragmentSessionsBinding? = null
    private val viewModel: SessionsViewModel by viewModels()
    private val adapter = SessionListAdapter()

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
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(activity)

        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.STARTED)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.allSessions.observe(viewLifecycleOwner) { sessions ->
            sessions?.let { adapter.submitList(it) }
            binding.noSessionsText.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (sessions.isEmpty()) View.GONE else View.VISIBLE
        }
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
