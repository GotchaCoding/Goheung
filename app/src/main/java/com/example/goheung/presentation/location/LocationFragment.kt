package com.example.goheung.presentation.location

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.goheung.R
import com.example.goheung.data.model.UserLocation
import com.example.goheung.databinding.FragmentLocationBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LocationFragment : Fragment() {

    companion object {
        private const val TAG = "LocationFragment"
    }

    private var _binding: FragmentLocationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LocationViewModel by viewModels()
    private var kakaoMap: KakaoMap? = null

    @Inject
    lateinit var auth: FirebaseAuth

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                viewModel.onPermissionGranted()
            }
            else -> {
                viewModel.onPermissionDenied()
                showPermissionDeniedDialog()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMap()
        setupObservers()
        setupListeners()
        requestLocationPermission()
    }

    private fun setupMap() {
        binding.mapView.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() {
                // Map destroyed
            }

            override fun onMapError(error: Exception) {
                Toast.makeText(requireContext(), "지도 로드 실패: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }, object : KakaoMapReadyCallback() {
            override fun onMapReady(map: KakaoMap) {
                kakaoMap = map
                // 고흥군 중심 좌표로 초기화 (예: 34.8118, 127.6869)
                val goheungCenter = LatLng.from(34.8118, 127.6869)
                map.moveCamera(CameraUpdateFactory.newCenterPosition(goheungCenter, 12))
            }
        })
    }

    private fun setupObservers() {
        viewModel.allLocations.observe(viewLifecycleOwner) { locations ->
            updateMarkers(locations)
        }

        viewModel.arrivalTime.observe(viewLifecycleOwner) { time ->
            if (time != null) {
                binding.textViewArrivalTime.text = time
                binding.cardArrivalTime.isVisible = true
            } else {
                binding.cardArrivalTime.isVisible = false
            }
        }

        viewModel.myLocation.observe(viewLifecycleOwner) { myLocation ->
            myLocation?.let {
                moveCameraToLocation(it.lat, it.lng)
            }
        }

        viewModel.mySpeed.observe(viewLifecycleOwner) { speed ->
            updateSpeedDisplay(speed)
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearErrorMessage()
            }
        }
    }

    private fun setupListeners() {
        binding.fabMyLocation.setOnClickListener {
            viewModel.myLocation.value?.let { myLocation ->
                moveCameraToLocation(myLocation.lat, myLocation.lng, zoom = 15)
            }
        }
    }

    private fun updateMarkers(locations: List<UserLocation>) {
        val map = kakaoMap ?: run {
            Log.w(TAG, "updateMarkers: kakaoMap is null")
            return
        }
        val currentUid = auth.currentUser?.uid
        Log.d(TAG, "updateMarkers: locations=${locations.size}, currentUid=$currentUid")
        Log.d(TAG, "updateMarkers: labelManager=${map.labelManager}, layer=${map.labelManager?.layer}")

        // labelManager나 layer가 null이면 리턴
        val labelManager = map.labelManager
        if (labelManager == null) {
            Log.e(TAG, "labelManager is null!")
            return
        }

        val layer = labelManager.layer
        if (layer == null) {
            Log.e(TAG, "layer is null!")
            return
        }

        layer.removeAll()
        Log.d(TAG, "Removed all existing labels")

        locations.forEach { location ->
            val isMe = location.uid == currentUid
            Log.d(TAG, "Adding marker: uid=${location.uid}, isMe=$isMe, lat=${location.lat}, lng=${location.lng}")

            try {
                val latLng = LatLng.from(location.lat, location.lng)
                Log.d(TAG, "Created LatLng: $latLng")

                // 라벨 스타일 생성 (Android 시스템 아이콘 사용)
                val iconRes = if (isMe) {
                    android.R.drawable.presence_online  // 파란색 온라인 표시
                } else {
                    android.R.drawable.presence_invisible  // 회색 표시
                }

                val labelStyle = LabelStyle.from(iconRes)
                val labelText = if (isMe) getString(R.string.my_location_label) else location.displayName

                val labelOptions = LabelOptions.from(latLng).apply {
                    setStyles(labelStyle)
                    setTexts(labelText)
                }
                Log.d(TAG, "Created LabelOptions with style: $labelOptions")

                val label = layer.addLabel(labelOptions)
                Log.d(TAG, "Label added: $label (isMe=$isMe, text=$labelText)")

                if (label == null) {
                    Log.e(TAG, "addLabel returned null for ${location.uid}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add label for ${location.uid}", e)
                e.printStackTrace()
            }
        }

        Log.d(TAG, "Total labels on layer: ${layer.labelCount}")
    }

    private fun moveCameraToLocation(lat: Double, lng: Double, zoom: Int = 14) {
        kakaoMap?.moveCamera(
            CameraUpdateFactory.newCenterPosition(LatLng.from(lat, lng), zoom)
        )
    }

    private fun updateSpeedDisplay(speed: Float) {
        val speedKmh = (speed * 3.6f).toInt() // m/s → km/h

        if (speedKmh > 0) {
            binding.textViewSpeed.text = speedKmh.toString()
            binding.layoutSpeed.isVisible = true
        } else {
            binding.layoutSpeed.isVisible = false
        }
    }

    private fun requestLocationPermission() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.location_permission_required)
            .setMessage(R.string.location_permission_denied)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                openAppSettings()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", requireContext().packageName, null)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
