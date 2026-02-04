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
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.goheung.R
import com.example.goheung.databinding.FragmentChatDetailBinding
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ChatDetailFragment : Fragment() {

    companion object {
        private const val TAG = "ChatDetailFragment"
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

        // 메뉴 inflate
        binding.toolbar.inflateMenu(R.menu.menu_chat_detail)
        Log.d(TAG, "setupToolbar: Menu inflated, items count = ${binding.toolbar.menu.size()}")

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            Log.d(TAG, "Menu item clicked: ${menuItem.itemId}")
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
            Log.d(TAG, "chatRoom observer: name=${chatRoom.name}, participants=${chatRoom.participants.size}")
            binding.toolbar.title = chatRoom.name

            // 그룹 채팅만 초대 및 편집 메뉴 표시
            val isGroupChat = viewModel.canEditChatName()
            Log.d(TAG, "chatRoom observer: isGroupChat=$isGroupChat, canEditChatName=${viewModel.canEditChatName()}")

            val inviteItem = binding.toolbar.menu.findItem(R.id.action_invite_users)
            val editItem = binding.toolbar.menu.findItem(R.id.action_edit_chat_name)

            Log.d(TAG, "Menu items - invite: ${inviteItem != null}, edit: ${editItem != null}")

            inviteItem?.isVisible = isGroupChat
            editItem?.isVisible = isGroupChat

            Log.d(TAG, "Menu visibility set - invite: ${inviteItem?.isVisible}, edit: ${editItem?.isVisible}")
        }

        // 참여자 정보 표시
        viewModel.participantsDisplay.observe(viewLifecycleOwner) { displayText ->
            binding.textViewParticipants.text = displayText
            binding.textViewParticipants.isVisible = displayText.isNotBlank()
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

    private fun showEditChatNameDialog() {
        val currentName = viewModel.chatRoom.value?.name ?: ""
        val editText = EditText(requireContext()).apply {
            hint = getString(R.string.chat_room_name_hint)
            setText(currentName)
            setPadding(64, 32, 64, 32)
            selectAll() // 전체 선택으로 빠른 편집 지원
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

        // 비동기로 사용자 목록 가져오기
        lifecycleScope.launch {
            val result = userRepository.getAllUsers(currentUserId).first()
            result.fold(
                onSuccess = { allUsers ->
                    // 현재 채팅방에 없는 사용자만 필터링
                    val availableUsers = allUsers.filter { user ->
                        user.uid !in currentParticipants
                    }

                    if (availableUsers.isEmpty()) {
                        Toast.makeText(requireContext(), "초대할 수 있는 사용자가 없습니다", Toast.LENGTH_SHORT).show()
                        return@fold
                    }

                    // 사용자 선택 Dialog 표시
                    val userNames = availableUsers.map { it.displayName }.toTypedArray()
                    val selectedUsers = BooleanArray(availableUsers.size) { false }

                    AlertDialog.Builder(requireContext())
                        .setTitle(R.string.select_users_to_invite)
                        .setMultiChoiceItems(userNames, selectedUsers) { _, which, isChecked ->
                            selectedUsers[which] = isChecked
                        }
                        .setPositiveButton(R.string.invite) { _, _ ->
                            val invitedUserIds = availableUsers
                                .filterIndexed { index, _ -> selectedUsers[index] }
                                .map { it.uid }

                            if (invitedUserIds.isNotEmpty()) {
                                viewModel.inviteUsers(invitedUserIds)
                                Toast.makeText(
                                    requireContext(),
                                    "${invitedUserIds.size}명을 초대했습니다",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                },
                onFailure = { e ->
                    Toast.makeText(
                        requireContext(),
                        "사용자 목록을 불러올 수 없습니다: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
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
