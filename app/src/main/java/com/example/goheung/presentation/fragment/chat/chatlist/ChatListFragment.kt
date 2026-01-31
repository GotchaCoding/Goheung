package com.example.goheung.presentation.fragment.chat.chatlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.goheung.base.BaseFragment
import com.example.goheung.databinding.FragmentChatListBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatListFragment : BaseFragment<FragmentChatListBinding>() {

    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentChatListBinding
        get() = FragmentChatListBinding::inflate

    private val viewModel: ChatListViewModel by viewModels()
    private lateinit var adapter: ChatListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initObservers()
        viewModel.loadChatRooms()
    }

    private fun initViews() {
        adapter = ChatListAdapter(
            currentUserId = viewModel.getCurrentUserId(),
            onItemClick = { chatRoom ->
                val otherName = chatRoom.participantNames.entries
                    .firstOrNull { it.key != viewModel.getCurrentUserId() }?.value ?: ""
                val action = ChatListFragmentDirections
                    .actionChatListToChatDetail(chatRoom.chatRoomId, otherName)
                findNavController().navigate(action)
            }
        )
        binding.rvChatList.adapter = adapter
        binding.rvChatList.layoutManager = LinearLayoutManager(context)

        binding.fabNewChat.setOnClickListener {
            viewModel.loadUsers()
        }
    }

    private fun initObservers() {
        viewModel.chatRooms.observe(viewLifecycleOwner) { rooms ->
            adapter.submitList(rooms)
            binding.tvEmpty.visibility = if (rooms.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.users.observe(viewLifecycleOwner) { users ->
            if (users.isNotEmpty()) {
                val names = users.map { it.displayName }.toTypedArray()
                AlertDialog.Builder(requireContext())
                    .setTitle("채팅 상대 선택")
                    .setItems(names) { _, which ->
                        val selectedUser = users[which]
                        viewModel.createChatRoom(selectedUser.uid, selectedUser.displayName)
                    }
                    .show()
            }
        }

        viewModel.newChatRoomId.observe(viewLifecycleOwner) { chatRoomId ->
            val action = ChatListFragmentDirections
                .actionChatListToChatDetail(chatRoomId, "")
            findNavController().navigate(action)
        }
    }
}
