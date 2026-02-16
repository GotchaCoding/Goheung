package com.example.goheung.presentation.location

import android.Manifest
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.goheung.util.LocationUtils
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
import com.kakao.vectormap.label.LabelStyles
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

    // 버스 추적 모드 상태
    private var isTrackingBus = false
    private var trackingJob: Job? = null

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
                // 버스 추적 중이 아닐 때만 카메라 자동 이동
                if (!isTrackingBus) {
                    moveCameraToLocation(it.lat, it.lng, zoom = 16)
                }
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
        // 기존 클릭: 내 위치로 이동
        binding.fabMyLocation.setOnClickListener {
            viewModel.myLocation.value?.let { myLocation ->
                moveCameraToLocation(myLocation.lat, myLocation.lng, zoom = 16)
            }
        }

        // 롱클릭: 버스 추적 모드 시작
        binding.fabMyLocation.setOnLongClickListener {
            startBusTracking()
            true
        }

        // 터치 해제 감지: 버스 추적 모드 종료
        binding.fabMyLocation.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP ||
                event.action == MotionEvent.ACTION_CANCEL) {
                if (isTrackingBus) {
                    stopBusTracking()
                }
            }
            false  // 다른 리스너도 동작하도록 false 반환
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
                Log.d(TAG, "Created LatLng: $latLng, role=${location.role}")

                // 역할에 따라 다른 아이콘 사용
                // - 운전기사(DRIVER): 버스 아이콘
                // - 승객(PASSENGER): 동그라미 아이콘
                val iconRes = when {
                    location.role == "DRIVER" && isMe -> R.drawable.ic_bus_marker  // 내 버스 (파란색)
                    location.role == "DRIVER" -> R.drawable.ic_bus_marker_gray     // 다른 버스 (회색)
                    isMe -> R.drawable.ic_passenger_marker                         // 내 위치 (녹색 동그라미)
                    else -> R.drawable.ic_passenger_marker                         // 다른 승객 (녹색 동그라미)
                }
                val bitmap = getBitmapFromVectorDrawable(iconRes)
                val labelText = if (isMe) getString(R.string.my_location_label) else location.displayName

                // LabelStyles로 스타일 생성 (Bitmap 사용)
                val labelStyles = LabelStyles.from(LabelStyle.from(bitmap))

                val labelOptions = LabelOptions.from(latLng).apply {
                    setStyles(labelStyles)
                    setTexts(labelText)
                }
                Log.d(TAG, "Created LabelOptions with bitmap style, role=${location.role}")

                val label = layer.addLabel(labelOptions)
                Log.d(TAG, "Label added: $label (isMe=$isMe, role=${location.role}, text=$labelText)")

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

    /**
     * Vector Drawable을 Bitmap으로 변환
     */
    private fun getBitmapFromVectorDrawable(drawableId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(requireContext(), drawableId)!!
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
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

    /**
     * 버스 추적 모드 시작
     * - 가장 가까운 DRIVER 찾기
     * - 1초마다 카메라 이동
     */
    private fun startBusTracking() {
        isTrackingBus = true
        Toast.makeText(requireContext(), "버스 추적 시작", Toast.LENGTH_SHORT).show()

        // FAB 색상 변경으로 추적 중임을 표시
        binding.fabMyLocation.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.tracking_active))

        trackingJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isTrackingBus) {
                trackNearestBus()
                delay(1000L)  // 1초마다 업데이트
            }
        }
    }

    /**
     * 버스 추적 모드 종료
     */
    private fun stopBusTracking() {
        isTrackingBus = false
        trackingJob?.cancel()
        trackingJob = null
        Toast.makeText(requireContext(), "버스 추적 종료", Toast.LENGTH_SHORT).show()

        // FAB 색상 원복
        binding.fabMyLocation.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.fab_default))
    }

    /**
     * 가장 가까운 버스 찾아서 카메라 이동
     */
    private fun trackNearestBus() {
        val myLocation = viewModel.myLocation.value ?: return
        val allLocations = viewModel.allLocations.value ?: return

        // DRIVER 역할만 필터링 (자신 제외)
        val nearestBus = allLocations
            .filter { it.role == "DRIVER" && it.uid != auth.currentUser?.uid }
            .minByOrNull { location ->
                LocationUtils.calculateDistance(
                    myLocation.lat, myLocation.lng,
                    location.lat, location.lng
                )
            }

        nearestBus?.let { bus ->
            moveCameraToLocation(bus.lat, bus.lng, zoom = 16)
            Log.d(TAG, "Tracking bus: ${bus.displayName} at (${bus.lat}, ${bus.lng})")
        } ?: run {
            Toast.makeText(requireContext(), "추적 가능한 버스가 없습니다", Toast.LENGTH_SHORT).show()
            stopBusTracking()
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
