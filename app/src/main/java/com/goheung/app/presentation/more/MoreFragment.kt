package com.goheung.app.presentation.more

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
import com.goheung.app.BottomNavController
import com.goheung.app.R
import com.goheung.app.data.model.AttendanceStatus
import com.goheung.app.data.model.UserRole
import com.goheung.app.databinding.FragmentMoreBinding
import com.goheung.app.presentation.auth.LoginFragment
import com.goheung.app.util.Event
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
    private var lastSelectedRole: UserRole? = null

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
        isSpinnerInitialized = false  // View 재생성 시 플래그 리셋
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
                    Log.d(TAG, "onItemSelected: position=$position, role=${selectedRole.name}, lastSelectedRole=${lastSelectedRole?.name}")

                    // 실제 변경이 있을 때만 업데이트 (같은 값이면 무시)
                    if (lastSelectedRole != null && lastSelectedRole != selectedRole) {
                        Log.d(TAG, "Calling updateRole with ${selectedRole.name}")
                        viewModel.updateRole(selectedRole)
                        Toast.makeText(requireContext(), "역할 변경: ${selectedRole.displayName}", Toast.LENGTH_SHORT).show()
                    }
                    lastSelectedRole = selectedRole
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
            // setSelection이 같은 position일 때 onItemSelected 안불리므로 수동 복원
            isSpinnerInitialized = true
        }

        viewModel.currentRole.observe(viewLifecycleOwner) { role ->
            Log.d(TAG, "currentRole observer: role=${role.name}, lastSelectedRole=${lastSelectedRole?.name}")
            lastSelectedRole = role  // 현재 값으로 설정 (스피너 선택 시 비교용)
            val roles = UserRole.values()
            val index = roles.indexOf(role)
            Log.d(TAG, "Setting spinner selection to index=$index (${role.name})")
            binding.spinnerRole.setSelection(index)
        }

        viewModel.attendanceUpdateSuccess.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { success ->
                if (success) {
                    Toast.makeText(requireContext(), "근무 상태가 저장되었습니다", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "근무 상태 저장 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.roleUpdateSuccess.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { success ->
                if (success) {
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
