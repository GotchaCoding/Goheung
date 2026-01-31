package com.example.goheung.presentation.fragment.userlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.goheung.base.BaseFragment
import com.example.goheung.base.Resource
import com.example.goheung.databinding.FragmentUserListBinding
import com.example.goheung.model.UserModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UserListFragment : BaseFragment<FragmentUserListBinding>() {

    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentUserListBinding
        get() = FragmentUserListBinding::inflate

    private val viewModel: UserListViewModel by viewModels()
    private val adapter = UserListAdapter { user -> onUserClick(user) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initObservers()
        viewModel.loadUsers()
    }

    private fun initViews() {
        binding.rvUsers.adapter = adapter
        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadUsers()
        }
    }

    private fun initObservers() {
        viewModel.users.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.swipeRefresh.isRefreshing = true
                }
                is Resource.Success -> {
                    binding.swipeRefresh.isRefreshing = false
                    adapter.submitList(result.model)
                }
                is Resource.Fail -> {
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(requireContext(), "사용자 목록 로드 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.chatRoomCreated.observe(viewLifecycleOwner) { event ->
            val bundle = androidx.core.os.bundleOf(
                "chatRoomId" to event.first,
                "otherUserName" to event.second
            )
            findNavController().navigate(
                com.example.goheung.R.id.chatDetailFragment,
                bundle
            )
        }
    }

    private fun onUserClick(user: UserModel) {
        viewModel.createOrGetChatRoom(user)
    }
}
