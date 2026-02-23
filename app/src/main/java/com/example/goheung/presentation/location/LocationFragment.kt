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
import com.example.goheung.data.model.UserRole
import com.example.goheung.R
import com.example.goheung.data.model.UserLocation
import com.example.goheung.databinding.FragmentLocationBinding
import com.example.goheung.util.LocationUtils
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
        private const val DEFAULT_ZOOM = 16
        private const val INITIAL_ZOOM = 12
        private const val GOHEUNG_LAT = 34.8118
        private const val GOHEUNG_LNG = 127.6869
        private const val MAP_DISPLAY_DISTANCE_THRESHOLD = 15000.0  // 15km (미터)
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
                Toast.makeText(
                    requireContext(),
                    getString(R.string.map_load_failed, error.message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, object : KakaoMapReadyCallback() {
            override fun onMapReady(map: KakaoMap) {
                kakaoMap = map
                val goheungCenter = LatLng.from(GOHEUNG_LAT, GOHEUNG_LNG)
                map.moveCamera(CameraUpdateFactory.newCenterPosition(goheungCenter, INITIAL_ZOOM))
            }
        })
    }

    private fun setupObservers() {
        viewModel.allLocations.observe(viewLifecycleOwner) { locations ->
            val myLocation = viewModel.myLocation.value
            val filteredLocations = if (myLocation != null) {
                locations.filter { location ->
                    location.uid == myLocation.uid ||  // 내 위치는 항상 표시
                    LocationUtils.calculateDistance(
                        myLocation.lat, myLocation.lng,
                        location.lat, location.lng
                    ) <= MAP_DISPLAY_DISTANCE_THRESHOLD
                }
            } else {
                locations
            }
            updateMarkers(filteredLocations)
        }

        viewModel.arrivalTime.observe(viewLifecycleOwner) { time ->
            if (time != null) {
                binding.textViewArrivalTime.text = time
                binding.cardArrivalTime.isVisible = true
            } else {
                binding.cardArrivalTime.isVisible = false
            }
        }

        // 버스까지 거리 (승객만 표시)
        viewModel.distanceToBus.observe(viewLifecycleOwner) { distance ->
            if (distance != null) {
                val formattedDistance = LocationUtils.formatDistance(distance)
                binding.textViewDistance.text = getString(R.string.distance_to_bus, formattedDistance)
                binding.textViewDistance.isVisible = true
            } else {
                binding.textViewDistance.isVisible = false
            }
        }

        viewModel.myLocation.observe(viewLifecycleOwner) { myLocation ->
            myLocation?.let {
                // 버스 추적 중이 아닐 때만 카메라 자동 이동
                if (viewModel.isTrackingBus.value != true) {
                    moveCameraToLocation(it.lat, it.lng, zoom = DEFAULT_ZOOM)
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

        // 버스 추적 상태 관찰
        viewModel.isTrackingBus.observe(viewLifecycleOwner) { isTracking ->
            updateTrackingUI(isTracking)
        }

        // 가장 가까운 버스 위치 관찰
        viewModel.nearestBus.observe(viewLifecycleOwner) { bus ->
            bus?.let {
                moveCameraToLocation(it.lat, it.lng, zoom = DEFAULT_ZOOM)
            }
        }
    }

    private fun updateTrackingUI(isTracking: Boolean) {
        val colorRes = if (isTracking) R.color.tracking_active else R.color.fab_default
        binding.fabMyLocation.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), colorRes))
    }

    private fun setupListeners() {
        // 기존 클릭: 내 위치로 이동
        binding.fabMyLocation.setOnClickListener {
            viewModel.myLocation.value?.let { myLocation ->
                moveCameraToLocation(myLocation.lat, myLocation.lng, zoom = DEFAULT_ZOOM)
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
                if (viewModel.isTrackingBus.value == true) {
                    stopBusTracking()
                }
            }
            false  // 다른 리스너도 동작하도록 false 반환
        }
    }

    private fun updateMarkers(locations: List<UserLocation>) {
        val map = kakaoMap ?: return
        val currentUid = auth.currentUser?.uid
        val labelManager = map.labelManager ?: return
        val layer = labelManager.layer ?: return

        layer.removeAll()

        locations.forEach { location ->
            val isMe = location.uid == currentUid
            val role = UserRole.fromString(location.role)

            try {
                val latLng = LatLng.from(location.lat, location.lng)
                val iconRes = getMarkerIcon(role, isMe)
                val bitmap = getBitmapFromVectorDrawable(iconRes)
                val labelText = if (isMe) getString(R.string.my_location_label) else location.displayName
                val labelStyles = LabelStyles.from(LabelStyle.from(bitmap))

                val labelOptions = LabelOptions.from(latLng).apply {
                    setStyles(labelStyles)
                    setTexts(labelText)
                }

                layer.addLabel(labelOptions)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add label for ${location.uid}", e)
            }
        }
    }

    private fun getMarkerIcon(role: UserRole, isMe: Boolean): Int {
        return when {
            role == UserRole.DRIVER && isMe -> R.drawable.ic_bus_marker
            role == UserRole.DRIVER -> R.drawable.ic_bus_marker_gray
            else -> R.drawable.ic_passenger_marker
        }
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

    private fun startBusTracking() {
        if (!viewModel.hasAvailableBus()) {
            Toast.makeText(requireContext(), R.string.no_bus_available, Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.startBusTracking()
        Toast.makeText(requireContext(), R.string.bus_tracking_started, Toast.LENGTH_SHORT).show()
    }

    private fun stopBusTracking() {
        viewModel.stopBusTracking()
        Toast.makeText(requireContext(), R.string.bus_tracking_stopped, Toast.LENGTH_SHORT).show()
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
