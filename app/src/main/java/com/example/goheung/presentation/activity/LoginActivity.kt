package com.example.goheung.presentation.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.goheung.MainActivity
import com.example.goheung.R
import com.example.goheung.base.Resource
import com.example.goheung.databinding.ActivityLoginBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private var isSignUpMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)

        initViews()
        initObservers()
    }

    private fun initViews() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "이메일과 비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isSignUpMode) {
                val name = binding.etName.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "이름을 입력해주세요", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                viewModel.signUp(email, password, name)
            } else {
                viewModel.login(email, password)
            }
        }

        binding.btnToggleMode.setOnClickListener {
            isSignUpMode = !isSignUpMode
            if (isSignUpMode) {
                binding.tilName.visibility = View.VISIBLE
                binding.btnLogin.text = getString(R.string.signup)
                binding.btnToggleMode.text = getString(R.string.login)
            } else {
                binding.tilName.visibility = View.GONE
                binding.btnLogin.text = getString(R.string.login)
                binding.btnToggleMode.text = getString(R.string.signup)
            }
        }
    }

    private fun initObservers() {
        viewModel.authResult.observe(this) { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnLogin.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                is Resource.Fail -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(this, "인증 실패: ${result.exception}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
