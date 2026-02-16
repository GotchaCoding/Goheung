package com.example.goheung.presentation.chat

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.goheung.R
import com.example.goheung.data.model.User
import com.example.goheung.databinding.FragmentChatDetailBinding
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ChatDetailFragment : Fragment() {

    companion object {
        private const val ARG_CHAT_ROOM_ID = "chatRoomId"
        private const val ARG_CHAT_ROOM_NAME = "chatRoomName"
        private const val DIALOG_PADDING_HORIZONTAL = 64
        private const val DIALOG_PADDING_VERTICAL = 32

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

    @Inject
    lateinit var userRepository: com.example.goheung.data.repository.UserRepository

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

        binding.toolbar.inflateMenu(R.menu.menu_chat_detail)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_invite_users -> {
                    showInviteUsersDialog()
                    true
                }
                R.id.action_edit_chat_name -> {
                    showEditChatNameDialog()
                    true
                }
                else -> false
            }
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
            updateMenuVisibility(viewModel.canEditChatName())
        }

        viewModel.participantsDisplay.observe(viewLifecycleOwner) { displayText ->
            binding.textViewParticipants.text = displayText
            binding.textViewParticipants.isVisible = displayText.isNotBlank()
        }
    }

    private fun updateMenuVisibility(isGroupChat: Boolean) {
        binding.toolbar.menu.findItem(R.id.action_invite_users)?.isVisible = isGroupChat
        binding.toolbar.menu.findItem(R.id.action_edit_chat_name)?.isVisible = isGroupChat
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

    private fun showEditChatNameDialog() {
        val currentName = viewModel.chatRoom.value?.name ?: ""
        val editText = EditText(requireContext()).apply {
            hint = getString(R.string.chat_room_name_hint)
            setText(currentName)
            setPadding(
                DIALOG_PADDING_HORIZONTAL,
                DIALOG_PADDING_VERTICAL,
                DIALOG_PADDING_HORIZONTAL,
                DIALOG_PADDING_VERTICAL
            )
            selectAll()
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.edit_chat_room_name)
            .setView(editText)
            .setPositiveButton(R.string.save) { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotBlank() && newName != currentName) {
                    viewModel.updateChatRoomName(newName)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showInviteUsersDialog() {
        val currentParticipants = viewModel.chatRoom.value?.participants ?: emptyList()

        lifecycleScope.launch {
            val result = userRepository.getAllUsers(currentUserId).first()
            result.fold(
                onSuccess = { allUsers ->
                    val availableUsers = allUsers.filter { it.uid !in currentParticipants }

                    if (availableUsers.isEmpty()) {
                        Toast.makeText(requireContext(), R.string.no_users_to_invite, Toast.LENGTH_SHORT).show()
                        return@fold
                    }

                    showUserSelectionDialog(availableUsers)
                },
                onFailure = { e ->
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.failed_to_load_users, e.message),
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    private fun showUserSelectionDialog(availableUsers: List<User>) {
        val userNames = availableUsers.map { it.displayName }.toTypedArray()
        val selectedUsers = BooleanArray(availableUsers.size) { false }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.select_users_to_invite)
            .setMultiChoiceItems(userNames, selectedUsers) { _, which, isChecked ->
                selectedUsers[which] = isChecked
            }
            .setPositiveButton(R.string.invite) { _, _ ->
                handleUserInvitation(availableUsers, selectedUsers)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun handleUserInvitation(availableUsers: List<User>, selectedUsers: BooleanArray) {
        val invitedUserIds = availableUsers
            .filterIndexed { index, _ -> selectedUsers[index] }
            .map { it.uid }

        when {
            invitedUserIds.isEmpty() -> {
                Toast.makeText(requireContext(), R.string.select_at_least_one, Toast.LENGTH_SHORT).show()
            }
            invitedUserIds.size == 1 -> {
                Toast.makeText(requireContext(), R.string.group_chat_min_users, Toast.LENGTH_SHORT).show()
            }
            else -> {
                viewModel.inviteUsers(invitedUserIds)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.users_invited, invitedUserIds.size),
                    Toast.LENGTH_SHORT
                ).show()
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
