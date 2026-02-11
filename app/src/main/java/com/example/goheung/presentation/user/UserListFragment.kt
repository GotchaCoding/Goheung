package com.example.goheung.presentation.user

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.goheung.BottomNavController
import com.example.goheung.R
import com.example.goheung.databinding.FragmentUserListBinding
import com.example.goheung.presentation.chat.ChatDetailFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UserListFragment : Fragment() {

    private var _binding: FragmentUserListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserListViewModel by viewModels()
    private lateinit var adapter: UserProfileAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearchView()
        setupObservers()
    }

    private fun setupRecyclerView() {
        adapter = UserProfileAdapter(
            onUserClick = { user ->
                viewModel.onUserClicked(user)
            }
        )
        binding.recyclerViewUsers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@UserListFragment.adapter
            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )
        }
    }

    private fun setupSearchView() {
        // EditText 텍스트 변경 리스너 (TMDB 패턴)
        binding.editTextSearch.addTextChangedListener { editable ->
            val query = editable.toString()

            // ViewModel에 검색어 전달 (debounce 처리는 ViewModel에서)
            viewModel.onSearchQueryChanged(query)

            // Clear 버튼 가시성
            binding.imageViewClearSearch.isVisible = query.isNotEmpty()
        }

        // Clear 버튼 클릭
        binding.imageViewClearSearch.setOnClickListener {
            binding.editTextSearch.text?.clear()
            viewModel.clearSearch()
        }

        // IME 검색 액션 처리 (키보드 엔터)
        binding.editTextSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // 키보드 숨기기
                hideKeyboard()
                true
            } else {
                false
            }
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.editTextSearch.windowToken, 0)
    }

    private fun setupObservers() {
        viewModel.userProfiles.observe(viewLifecycleOwner) { profiles ->
            adapter.submitList(profiles)

            // 빈 목록 메시지 처리 개선
            val isEmpty = profiles.isEmpty()
            val hasSearchQuery = binding.editTextSearch.text?.isNotEmpty() == true

            binding.textViewEmpty.isVisible = isEmpty
            binding.textViewEmpty.text = if (hasSearchQuery) {
                getString(R.string.no_search_results)  // "검색 결과가 없습니다"
            } else {
                getString(R.string.no_users)           // "등록된 사용자가 없습니다"
            }

            binding.recyclerViewUsers.isVisible = !isEmpty
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

        viewModel.navigateToChatDetail.observe(viewLifecycleOwner) { pair ->
            pair?.let { (chatRoomId, chatRoomName) ->
                (activity as? BottomNavController)?.hideBottomNav()
                parentFragmentManager.commit {
                    replace(
                        R.id.fragment_container,
                        ChatDetailFragment.newInstance(chatRoomId, chatRoomName)
                    )
                    addToBackStack(null)
                }
                viewModel.onNavigationComplete()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
