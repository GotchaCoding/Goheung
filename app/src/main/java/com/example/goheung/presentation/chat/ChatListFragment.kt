package com.example.goheung.presentation.chat

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.goheung.BottomNavController
import com.example.goheung.R
import com.example.goheung.databinding.FragmentChatListBinding
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChatListFragment : Fragment() {

    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatListViewModel by viewModels()
    private lateinit var adapter: ChatListAdapter

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    private val currentUserId: String
        get() = firebaseAuth.currentUser!!.uid

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    private fun setupRecyclerView() {
        adapter = ChatListAdapter { chatRoom ->
            navigateToChatDetail(chatRoom.id, chatRoom.name)
        }

        binding.recyclerViewChatRooms.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ChatListFragment.adapter
            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )
        }
    }

    private fun setupObservers() {
        viewModel.chatRooms.observe(viewLifecycleOwner) { chatRooms ->
            adapter.submitList(chatRooms)
            binding.textViewEmpty.isVisible = chatRooms.isEmpty()
            binding.recyclerViewChatRooms.isVisible = chatRooms.isNotEmpty()
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

        viewModel.createdChatRoom.observe(viewLifecycleOwner) { pair ->
            pair?.let { (chatRoomId, chatRoomName) ->
                viewModel.onNavigatedToCreatedRoom()
                navigateToChatDetail(chatRoomId, chatRoomName)
            }
        }
    }

    private fun setupListeners() {
        binding.fabNewChat.setOnClickListener {
            showCreateChatRoomDialog()
        }
    }

    private fun showCreateChatRoomDialog() {
        val editText = EditText(requireContext()).apply {
            hint = getString(R.string.chat_room_name_hint)
            setPadding(64, 32, 64, 32)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.new_chat_room_title)
            .setView(editText)
            .setPositiveButton(R.string.create) { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotBlank()) {
                    viewModel.createChatRoom(
                        name = name,
                        description = "",
                        userId = currentUserId
                    )
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun navigateToChatDetail(chatRoomId: String, chatRoomName: String) {
        (activity as? BottomNavController)?.hideBottomNav()
        parentFragmentManager.commit {
            replace(
                R.id.fragment_container,
                ChatDetailFragment.newInstance(chatRoomId, chatRoomName)
            )
            addToBackStack(null)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
