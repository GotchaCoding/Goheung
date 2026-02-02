package com.example.goheung.presentation.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.goheung.BottomNavController
import com.example.goheung.R
import com.example.goheung.databinding.FragmentMoreBinding
import com.example.goheung.presentation.auth.LoginFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MoreFragment : Fragment() {

    private var _binding: FragmentMoreBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MoreViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.title = getString(R.string.tab_more)
        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.profile.observe(viewLifecycleOwner) { user ->
            binding.textViewDisplayName.text = user.displayName
            binding.textViewEmail.text = user.email
            binding.textViewDepartment.text = user.department
            Glide.with(this)
                .load(user.profileImageUrl)
                .placeholder(R.drawable.ic_person_placeholder)
                .circleCrop()
                .into(binding.imageViewProfile)
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }
    }

    private fun setupListeners() {
        binding.buttonLogout.setOnClickListener {
            viewModel.logout()
            (activity as? BottomNavController)?.hideBottomNav()
            parentFragmentManager.commit {
                replace(R.id.fragment_container, LoginFragment())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
