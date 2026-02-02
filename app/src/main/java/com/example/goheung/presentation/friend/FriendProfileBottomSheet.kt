package com.example.goheung.presentation.friend

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.commit
import com.bumptech.glide.Glide
import com.example.goheung.BottomNavController
import com.example.goheung.R
import com.example.goheung.data.repository.AuthRepository
import com.example.goheung.data.repository.ChatRepository
import com.example.goheung.data.repository.FriendRepository
import com.example.goheung.databinding.FragmentFriendProfileBottomSheetBinding
import com.example.goheung.presentation.chat.ChatDetailFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class FriendProfileBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "FriendProfileBottomSheet"
        private const val ARG_FRIEND_UID = "friendUid"
        private const val ARG_FRIEND_NAME = "friendName"
        private const val ARG_FRIEND_PROFILE_IMAGE_URL = "friendProfileImageUrl"

        fun newInstance(
            friendUid: String,
            friendName: String,
            friendProfileImageUrl: String?
        ): FriendProfileBottomSheet {
            return FriendProfileBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_FRIEND_UID, friendUid)
                    putString(ARG_FRIEND_NAME, friendName)
                    putString(ARG_FRIEND_PROFILE_IMAGE_URL, friendProfileImageUrl)
                }
            }
        }
    }

    private var _binding: FragmentFriendProfileBottomSheetBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var chatRepository: ChatRepository

    @Inject
    lateinit var friendRepository: FriendRepository

    @Inject
    lateinit var authRepository: AuthRepository

    private val friendUid: String
        get() = arguments?.getString(ARG_FRIEND_UID) ?: ""

    private val friendName: String
        get() = arguments?.getString(ARG_FRIEND_NAME) ?: ""

    private val friendProfileImageUrl: String?
        get() = arguments?.getString(ARG_FRIEND_PROFILE_IMAGE_URL)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendProfileBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupListeners()
    }

    private fun setupViews() {
        binding.textViewName.text = friendName
        Glide.with(binding.imageViewProfile)
            .load(friendProfileImageUrl)
            .placeholder(R.drawable.ic_person_placeholder)
            .circleCrop()
            .into(binding.imageViewProfile)
    }

    private fun setupListeners() {
        binding.buttonStartChat.setOnClickListener {
            startDirectChat()
        }

        binding.buttonRemoveFriend.setOnClickListener {
            removeFriend()
        }
    }

    private fun startDirectChat() {
        val myUid = authRepository.currentUser?.uid ?: return
        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                chatRepository.findOrCreateDirectChat(myUid, friendUid, friendName)
            }
            result.onSuccess { chatRoomId ->
                dismiss()
                (activity as? BottomNavController)?.hideBottomNav()
                parentFragmentManager.commit {
                    replace(
                        R.id.fragment_container,
                        ChatDetailFragment.newInstance(chatRoomId, friendName)
                    )
                    addToBackStack(null)
                }
            }.onFailure {
                Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun removeFriend() {
        val myUid = authRepository.currentUser?.uid ?: return
        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                friendRepository.removeFriend(myUid, friendUid)
            }
            result.onSuccess {
                Toast.makeText(requireContext(), R.string.friend_removed, Toast.LENGTH_SHORT).show()
                dismiss()
            }.onFailure {
                Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
