package com.example.goheung.presentation.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import com.example.goheung.BottomNavController
import com.example.goheung.R
import com.example.goheung.databinding.FragmentSignUpBinding
import com.example.goheung.presentation.chat.ChatListFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SignUpViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.signUpState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SignUpViewModel.SignUpState.Idle -> {
                    setLoadingState(false)
                }
                is SignUpViewModel.SignUpState.Loading -> {
                    setLoadingState(true)
                }
                is SignUpViewModel.SignUpState.Success -> {
                    setLoadingState(false)
                    navigateToChatList()
                }
                is SignUpViewModel.SignUpState.Error -> {
                    setLoadingState(false)
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
    }

    private fun setupListeners() {
        binding.buttonSignUp.setOnClickListener {
            val displayName = binding.editTextDisplayName.text.toString().trim()
            val email = binding.editTextEmail.text.toString().trim()
            val password = binding.editTextPassword.text.toString()
            val department = binding.editTextDepartment.text.toString().trim()
            viewModel.signUp(displayName, email, password, department)
        }

        binding.textViewLoginLink.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.buttonSignUp.isEnabled = !isLoading
        binding.editTextDisplayName.isEnabled = !isLoading
        binding.editTextEmail.isEnabled = !isLoading
        binding.editTextPassword.isEnabled = !isLoading
        binding.editTextDepartment.isEnabled = !isLoading
    }

    private fun navigateToChatList() {
        (activity as? BottomNavController)?.showBottomNav()
        parentFragmentManager.popBackStack()
        parentFragmentManager.commit {
            replace(R.id.fragment_container, ChatListFragment())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
