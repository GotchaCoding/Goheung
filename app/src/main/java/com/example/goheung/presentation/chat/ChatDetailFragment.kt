package com.example.goheung.presentation.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.goheung.databinding.FragmentChatDetailBinding
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChatDetailFragment : Fragment() {

    companion object {
        private const val ARG_CHAT_ROOM_ID = "chatRoomId"
        private const val ARG_CHAT_ROOM_NAME = "chatRoomName"

        fun newInstance(chatRoomId: String, chatRoomName: String): ChatDetailFragment {
            return ChatDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CHAT_ROOM_ID, chatRoomId)
                    putString(ARG_CHAT_ROOM_NAME, chatRoomName)
                }
            }
        }
    }

    private var _binding: FragmentChatDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatDetailViewModel by viewModels()
    private lateinit var adapter: ChatDetailAdapter

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    private val currentUserId: String
        get() = firebaseAuth.currentUser!!.uid

    private val currentUserName: String
        get() = firebaseAuth.currentUser?.displayName ?: ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    private fun setupToolbar() {
        val chatRoomName = arguments?.getString(ARG_CHAT_ROOM_NAME) ?: ""
        binding.toolbar.title = chatRoomName
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        adapter = ChatDetailAdapter(currentUserId)

        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).also {
                it.stackFromEnd = true
            }
            adapter = this@ChatDetailFragment.adapter
        }
    }

    private fun setupObservers() {
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            adapter.submitList(messages) {
                if (messages.isNotEmpty()) {
                    binding.recyclerViewMessages.smoothScrollToPosition(messages.size - 1)
                }
            }
            binding.textViewEmpty.isVisible = messages.isEmpty()
            binding.recyclerViewMessages.isVisible = messages.isNotEmpty()
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

        viewModel.sendingMessage.observe(viewLifecycleOwner) { isSending ->
            binding.buttonSend.isEnabled = !isSending
        }

        viewModel.chatRoom.observe(viewLifecycleOwner) { chatRoom ->
            binding.toolbar.title = chatRoom.name
        }
    }

    private fun setupListeners() {
        binding.editTextMessage.doAfterTextChanged { text ->
            binding.buttonSend.isEnabled = !text.isNullOrBlank()
        }

        binding.buttonSend.setOnClickListener {
            val messageText = binding.editTextMessage.text.toString()
            if (messageText.isNotBlank()) {
                viewModel.sendMessage(
                    text = messageText,
                    userId = currentUserId,
                    userName = currentUserName
                )
                binding.editTextMessage.text.clear()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.markMessagesAsRead(currentUserId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
