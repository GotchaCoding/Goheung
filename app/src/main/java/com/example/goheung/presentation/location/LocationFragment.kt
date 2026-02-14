package com.example.goheung.presentation.location

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LocationFragment : Fragment() {

    private var _binding: FragmentLocationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LocationViewModel by viewModels()
    private var kakaoMap: KakaoMap? = null

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
        val map = kakaoMap ?: return
        map.labelManager?.layer?.removeAll()

        locations.forEach { location ->
            val labelOptions = LabelOptions.from(
                LatLng.from(location.lat, location.lng)
            ).apply {
                // 역할에 따라 다른 스타일 적용
                val style = if (location.role == "DRIVER") {
                    // 운전기사 마커
                    LabelStyle.from(R.drawable.ic_person_placeholder)
                } else {
                    // 승객 마커
                    LabelStyle.from(R.drawable.ic_person_placeholder)
                }
                setStyles(style)
                setTexts(location.displayName)
            }
            map.labelManager?.layer?.addLabel(labelOptions)
        }
    }

    private fun moveCameraToLocation(lat: Double, lng: Double, zoom: Int = 14) {
        kakaoMap?.moveCamera(
            CameraUpdateFactory.newCenterPosition(LatLng.from(lat, lng), zoom)
        )
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
