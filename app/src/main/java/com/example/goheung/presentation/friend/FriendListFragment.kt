package com.example.goheung.presentation.friend

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.goheung.BottomNavController
import com.example.goheung.R
import com.example.goheung.data.model.Friend
import com.example.goheung.databinding.FragmentFriendListBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FriendListFragment : Fragment() {

    private var _binding: FragmentFriendListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FriendListViewModel by viewModels()
    private lateinit var adapter: FriendListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        setupObservers()
    }

    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.friend_list_title)
        binding.toolbar.inflateMenu(R.menu.menu_friend_list)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_add_friend -> {
                    navigateToFriendSearch()
                    true
                }
                R.id.action_friend_requests -> {
                    navigateToFriendRequests()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = FriendListAdapter { friend ->
            showFriendProfile(friend)
        }
        binding.recyclerViewFriends.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@FriendListFragment.adapter
            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )
        }
    }

    private fun setupObservers() {
        viewModel.friends.observe(viewLifecycleOwner) { friends ->
            adapter.submitList(friends)
            binding.textViewEmpty.isVisible = friends.isEmpty()
            binding.recyclerViewFriends.isVisible = friends.isNotEmpty()
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.pendingRequestCount.observe(viewLifecycleOwner) { count ->
            val menuItem = binding.toolbar.menu.findItem(R.id.action_friend_requests)
            menuItem?.let {
                it.title = if (count > 0) {
                    "${getString(R.string.friend_requests_title)} ($count)"
                } else {
                    getString(R.string.friend_requests_title)
                }
            }
        }
    }

    private fun showFriendProfile(friend: Friend) {
        val bottomSheet = FriendProfileBottomSheet.newInstance(
            friendUid = friend.uid,
            friendName = friend.displayName,
            friendProfileImageUrl = friend.profileImageUrl
        )
        bottomSheet.show(parentFragmentManager, FriendProfileBottomSheet.TAG)
    }

    private fun navigateToFriendSearch() {
        (activity as? BottomNavController)?.hideBottomNav()
        parentFragmentManager.commit {
            replace(R.id.fragment_container, FriendSearchFragment())
            addToBackStack(null)
        }
    }

    private fun navigateToFriendRequests() {
        (activity as? BottomNavController)?.hideBottomNav()
        parentFragmentManager.commit {
            replace(R.id.fragment_container, FriendRequestListFragment())
            addToBackStack(null)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
