package com.example.goheung

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.goheung.presentation.auth.LoginFragment
import com.example.goheung.presentation.chat.ChatListFragment
import com.example.goheung.presentation.more.MoreFragment
import com.example.goheung.presentation.user.UserListFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), BottomNavController {

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_navigation)
        setupBottomNavigation()

        if (savedInstanceState == null) {
            if (firebaseAuth.currentUser != null) {
                showBottomNav()
                switchTab(ChatListFragment(), "chat")
                bottomNav.selectedItemId = R.id.nav_chat
            } else {
                supportFragmentManager.commit {
                    replace(R.id.fragment_container, LoginFragment())
                }
            }
        }

        supportFragmentManager.addOnBackStackChangedListener {
            val current = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (current is ChatListFragment || current is UserListFragment || current is MoreFragment) {
                showBottomNav()
            }
        }
    }

    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_chat -> {
                    switchTab(ChatListFragment(), "chat")
                    true
                }
                R.id.nav_users -> {
                    switchTab(UserListFragment(), "users")
                    true
                }
                R.id.nav_more -> {
                    switchTab(MoreFragment(), "more")
                    true
                }
                else -> false
            }
        }
    }

    private fun switchTab(fragment: Fragment, tag: String) {
        supportFragmentManager.commit {
            replace(R.id.fragment_container, fragment, tag)
        }
    }

    override fun showBottomNav() {
        bottomNav.visibility = View.VISIBLE
    }

    override fun hideBottomNav() {
        bottomNav.visibility = View.GONE
    }
}
