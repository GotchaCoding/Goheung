package com.example.goheung

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.goheung.presentation.auth.LoginFragment
import com.example.goheung.presentation.chat.ChatDetailFragment
import com.example.goheung.presentation.chat.ChatListFragment
import com.example.goheung.presentation.location.LocationFragment
import com.example.goheung.presentation.more.MoreFragment
import com.example.goheung.presentation.user.UserListFragment
import com.example.goheung.service.GoheungMessagingService
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), BottomNavController {

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var adView: AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // AdMob 초기화
        MobileAds.initialize(this) {}

        // 광고 로드
        adView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        bottomNav = findViewById(R.id.bottom_navigation)
        setupBottomNavigation()

        if (savedInstanceState == null) {
            if (firebaseAuth.currentUser != null) {
                showBottomNav()
                switchTab(ChatListFragment(), "chat")
                bottomNav.selectedItemId = R.id.nav_chat
                handleNotificationIntent(intent)
            } else {
                supportFragmentManager.commit {
                    replace(R.id.fragment_container, LoginFragment())
                }
            }
        }

        supportFragmentManager.addOnBackStackChangedListener {
            val current = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (current is ChatListFragment || current is UserListFragment ||
                current is LocationFragment || current is MoreFragment) {
                showBottomNav()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        intent ?: return
        val chatRoomId = intent.getStringExtra(GoheungMessagingService.EXTRA_CHAT_ROOM_ID) ?: return
        val chatRoomName = intent.getStringExtra(GoheungMessagingService.EXTRA_CHAT_ROOM_NAME) ?: ""

        if (firebaseAuth.currentUser != null) {
            navigateToChatDetail(chatRoomId, chatRoomName)
            // 처리 후 Intent에서 extra 제거
            intent.removeExtra(GoheungMessagingService.EXTRA_CHAT_ROOM_ID)
            intent.removeExtra(GoheungMessagingService.EXTRA_CHAT_ROOM_NAME)
        }
    }

    private fun navigateToChatDetail(chatRoomId: String, chatRoomName: String) {
        hideBottomNav()
        supportFragmentManager.commit {
            replace(R.id.fragment_container, ChatDetailFragment.newInstance(chatRoomId, chatRoomName))
            addToBackStack(null)
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
                R.id.nav_location -> {
                    switchTab(LocationFragment(), "location")
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

    override fun onPause() {
        adView.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        adView.resume()
    }

    override fun onDestroy() {
        adView.destroy()
        super.onDestroy()
    }
}
