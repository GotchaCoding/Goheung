package com.example.goheung.presentation.chat

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
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

    companion object {
        private const val TAG = "ChatListFragment"
    }

    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatListViewModel by viewModels()
    private lateinit var directMessagesAdapter: ChatListAdapter
    private lateinit var groupChatsAdapter: ChatListAdapter

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
        directMessagesAdapter = ChatListAdapter(
            onChatRoomClick = { item ->
                navigateToChatDetail(item.chatRoom.id, item.displayName)
            },
            onFavoriteClick = { chatRoomId ->
                viewModel.toggleFavorite(chatRoomId)
            },
            currentUserId = currentUserId
        )

        groupChatsAdapter = ChatListAdapter(
            onChatRoomClick = { item ->
                navigateToChatDetail(item.chatRoom.id, item.displayName)
            },
            onFavoriteClick = { chatRoomId ->
                viewModel.toggleFavorite(chatRoomId)
            },
            currentUserId = currentUserId
        )

        binding.recyclerViewDirectMessages.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = directMessagesAdapter
            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )
        }

        binding.recyclerViewGroupChats.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = groupChatsAdapter
            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )
        }
    }

    private fun setupObservers() {
        Log.d(TAG, "setupObservers: Setting up observers")
        viewModel.directChats.observe(viewLifecycleOwner) { directChats ->
            Log.d(TAG, "directChats observer: received ${directChats.size} items")
            directChats.forEachIndexed { index, item ->
                Log.d(TAG, "  DM[$index]: ${item.displayName}")
            }
            directMessagesAdapter.submitList(directChats)
            binding.textViewDirectMessagesHeader.isVisible = directChats.isNotEmpty()
            binding.recyclerViewDirectMessages.isVisible = directChats.isNotEmpty()
            updateEmptyState()
        }

        viewModel.groupChats.observe(viewLifecycleOwner) { groupChats ->
            Log.d(TAG, "groupChats observer: received ${groupChats.size} items")
            groupChats.forEachIndexed { index, item ->
                Log.d(TAG, "  Group[$index]: ${item.displayName}")
            }
            groupChatsAdapter.submitList(groupChats)
            binding.textViewGroupChatsHeader.isVisible = groupChats.isNotEmpty()
            binding.recyclerViewGroupChats.isVisible = groupChats.isNotEmpty()
            updateEmptyState()
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

    private fun updateEmptyState() {
        val directChats = viewModel.directChats.value ?: emptyList()
        val groupChats = viewModel.groupChats.value ?: emptyList()
        binding.textViewEmpty.isVisible = directChats.isEmpty() && groupChats.isEmpty()
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
