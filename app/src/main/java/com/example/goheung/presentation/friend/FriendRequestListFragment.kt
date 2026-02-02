package com.example.goheung.presentation.friend

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.goheung.databinding.FragmentFriendRequestListBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FriendRequestListFragment : Fragment() {

    private var _binding: FragmentFriendRequestListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FriendRequestListViewModel by viewModels()
    private lateinit var adapter: FriendRequestAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendRequestListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        setupObservers()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        adapter = FriendRequestAdapter(
            onAccept = { request -> viewModel.acceptRequest(request) },
            onReject = { requestId -> viewModel.rejectRequest(requestId) }
        )
        binding.recyclerViewRequests.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@FriendRequestListFragment.adapter
            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )
        }
    }

    private fun setupObservers() {
        viewModel.requests.observe(viewLifecycleOwner) { requests ->
            adapter.submitList(requests)
            binding.textViewEmpty.isVisible = requests.isEmpty()
            binding.recyclerViewRequests.isVisible = requests.isNotEmpty()
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }

        viewModel.message.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearMessage()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
