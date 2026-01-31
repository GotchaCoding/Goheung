package com.example.goheung.presentation.fragment.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.example.goheung.base.BaseFragment
import com.example.goheung.databinding.FragmentProfileBinding
import com.example.goheung.presentation.activity.LoginActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : BaseFragment<FragmentProfileBinding>() {

    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentProfileBinding
        get() = FragmentProfileBinding::inflate

    private val viewModel: ProfileViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = viewModel
        initObservers()
        viewModel.loadProfile()
    }

    private fun initObservers() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            binding.tvName.text = user.displayName
            binding.tvEmail.text = user.email
        }

        viewModel.logoutEvent.observe(viewLifecycleOwner) {
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
        }
    }
}
