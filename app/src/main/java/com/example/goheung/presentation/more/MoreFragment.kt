package com.example.goheung.presentation.more

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.goheung.BottomNavController
import com.example.goheung.R
import com.example.goheung.data.model.AttendanceStatus
import com.example.goheung.data.model.UserRole
import com.example.goheung.databinding.FragmentMoreBinding
import com.example.goheung.presentation.auth.LoginFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MoreFragment : Fragment() {

    companion object {
        private const val TAG = "MoreFragment"
    }

    private var _binding: FragmentMoreBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MoreViewModel by viewModels()
    private var isSpinnerInitialized = false
    private var isRoleSpinnerInitialized = false

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
        setupAttendanceSpinner()
        setupRoleSpinner()
        setupObservers()
        setupListeners()
    }

    private fun setupAttendanceSpinner() {
        val statuses = AttendanceStatus.values()
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_attendance_item,
            statuses.map { it.displayName }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAttendance.adapter = adapter

        binding.spinnerAttendance.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (isSpinnerInitialized) {
                        viewModel.updateAttendance(statuses[position])
                    }
                    isSpinnerInitialized = true
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
    }

    private fun setupRoleSpinner() {
        val roles = UserRole.values()
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_attendance_item,
            roles.map { it.displayName }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRole.adapter = adapter

        binding.spinnerRole.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedRole = roles[position]
                    Log.d(TAG, "onItemSelected: position=$position, role=${selectedRole.name}, isInitialized=$isRoleSpinnerInitialized")

                    if (isRoleSpinnerInitialized) {
                        Log.d(TAG, "Calling updateRole with ${selectedRole.name}")
                        viewModel.updateRole(selectedRole)
                        Toast.makeText(requireContext(), "역할 변경: ${selectedRole.displayName}", Toast.LENGTH_SHORT).show()
                    }
                    isRoleSpinnerInitialized = true
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
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

        viewModel.currentAttendance.observe(viewLifecycleOwner) { status ->
            isSpinnerInitialized = false
            val statuses = AttendanceStatus.values()
            binding.spinnerAttendance.setSelection(statuses.indexOf(status))
        }

        viewModel.currentRole.observe(viewLifecycleOwner) { role ->
            Log.d(TAG, "currentRole observer: role=${role.name}, setting isRoleSpinnerInitialized=false")
            isRoleSpinnerInitialized = false
            val roles = UserRole.values()
            val index = roles.indexOf(role)
            Log.d(TAG, "Setting spinner selection to index=$index (${role.name})")
            binding.spinnerRole.setSelection(index)
        }

        viewModel.attendanceUpdateSuccess.observe(viewLifecycleOwner) { success ->
            success?.let {
                // 필요시 Toast 표시
            }
        }

        viewModel.roleUpdateSuccess.observe(viewLifecycleOwner) { success ->
            success?.let {
                if (it) {
                    Log.d(TAG, "Role update SUCCESS - saved to Firebase")
                    Toast.makeText(requireContext(), "역할이 저장되었습니다", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e(TAG, "Role update FAILED")
                    Toast.makeText(requireContext(), "역할 저장 실패", Toast.LENGTH_SHORT).show()
                }
            }
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
