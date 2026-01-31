package com.example.goheung.presentation.fragment.chat.chatdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.goheung.base.BaseFragment
import com.example.goheung.databinding.FragmentChatDetailBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatDetailFragment : BaseFragment<FragmentChatDetailBinding>() {

    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentChatDetailBinding
        get() = FragmentChatDetailBinding::inflate

    private val viewModel: ChatDetailViewModel by viewModels()
    private val args: ChatDetailFragmentArgs by navArgs()
    private lateinit var adapter: ChatDetailAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initObservers()
        viewModel.loadMessages(args.chatRoomId)
    }

    private fun initViews() {
        binding.toolbar.title = args.otherUserName
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        adapter = ChatDetailAdapter()
        binding.rvMessages.adapter = adapter
        binding.rvMessages.layoutManager = LinearLayoutManager(context).apply {
            stackFromEnd = true
        }

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.sendMessage(args.chatRoomId, text)
                binding.etMessage.text?.clear()
            }
        }
    }

    private fun initObservers() {
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            adapter.submitList(messages) {
                if (messages.isNotEmpty()) {
                    binding.rvMessages.scrollToPosition(messages.size - 1)
                }
            }
        }
    }
}
