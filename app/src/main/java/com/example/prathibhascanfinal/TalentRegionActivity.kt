package com.example.prathibhascanfinal

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.prathibhascanfinal.data.DistrictTalentRegion
import com.example.prathibhascanfinal.data.repository.FirestoreRepository
import com.example.prathibhascanfinal.ui.base.BaseActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TalentRegionActivity : BaseActivity(), OnMapReadyCallback {

    override val viewModel: DashboardViewModel by viewModels()
    private lateinit var mMap: GoogleMap
    private val firestoreRepository = FirestoreRepository()
    private val regionMap = mutableMapOf<String, DistrictTalentRegion>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_talent_region)

        val rootView = findViewById<View>(R.id.main_heatmap_root)
        setupEdgeToEdge(rootView)

        findViewById<View>(R.id.progress_map_loading).visibility = View.VISIBLE

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_container) as? SupportMapFragment
            ?: SupportMapFragment.newInstance().also {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.map_container, it)
                    .commit()
            }

        mapFragment.getMapAsync(this)

        findViewById<View>(R.id.btn_back_heatmap).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btn_info_region)?.setOnClickListener {
            showInfoDialog()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        findViewById<View>(R.id.progress_map_loading).visibility = View.GONE

        // Apply dark mode style
        try {
            val success = mMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this, R.raw.map_dark_style)
            )
            if (!success) Log.e("TalentRegion", "Style parsing failed.")
        } catch (e: Exception) {
            Log.e("TalentRegion", "Can't find style. Error: $e")
        }

        // Center on Visakhapatnam region
        val vskp = LatLng(17.6868, 83.2185)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(vskp, 11f))

        mMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isMyLocationButtonEnabled = true
            isMapToolbarEnabled = false
        }

        mMap.setOnMarkerClickListener { marker ->
            val region = regionMap[marker.title]
            if (region != null) {
                showRegionDetailSheet(region)
            } else {
                marker.showInfoWindow()
            }
            true
        }

        loadRegionData()
    }

    private fun loadRegionData() {
        lifecycleScope.launch {
            // Fetch real users from Firestore to aggregate
            firestoreRepository.getDiscoveryAthletesFlow().collectLatest { users ->
                val regions = if (users.isNotEmpty()) {
                    aggregateUsersToRegions(users)
                } else {
                    getSampleRegions()
                }
                
                displayRegionsOnMap(regions)
            }
        }
    }

    private fun aggregateUsersToRegions(users: List<User>): List<DistrictTalentRegion> {
        // Group by location/city
        val grouped = users.groupBy { it.location?.trim()?.ifEmpty { "Visakhapatnam" } ?: "Visakhapatnam" }
        
        val baseCoordinates = mapOf(
            "Visakhapatnam" to LatLng(17.6868, 83.2185),
            "RK Beach" to LatLng(17.7231, 83.3012),
            "Port Stadium" to LatLng(17.6896, 83.2314),
            "AU Ground" to LatLng(17.7123, 83.2845),
            "Gajuwaka" to LatLng(17.6800, 83.2000),
            "Anakapalle" to LatLng(17.6913, 83.0039)
        )

        return grouped.map { (cityName, userList) ->
            val latLng = baseCoordinates[cityName] ?: LatLng(
                17.6868 + (Math.random() - 0.5) * 0.1,
                83.2185 + (Math.random() - 0.5) * 0.1
            )
            
            val total = userList.size
            val verified = userList.count { it.aadhaarMasked != null || it.currentTier != "School Level" }
            val active = userList.count { it.totalXP > 0 }
            val avgPerf = userList.map { it.technicalImpactScore }.average().takeIf { !it.isNaN() } ?: 65.0
            val avgAi = userList.map { (it.speedScore + it.agilityScore + it.staminaScore) / 3.0 }.average().takeIf { !it.isNaN() } ?: 70.0
            
            DistrictTalentRegion(
                id = cityName.lowercase(),
                name = "$cityName Region",
                center = latLng,
                athleteCount = total,
                verifiedCount = verified,
                activeCount = active,
                avgPerformanceScore = avgPerf,
                avgAiAssessmentScore = avgAi,
                tournamentParticipation = userList.sumOf { if (it.achievements != null) 2 else 1 as Int },
                trend = if (avgPerf > 70) "↑ Improving" else "→ Stable"
            )
        }
    }

    private fun getSampleRegions(): List<DistrictTalentRegion> {
        return listOf(
            DistrictTalentRegion(
                id = "rk_beach",
                name = "Cricket Hub - RK Beach",
                center = LatLng(17.7231, 83.3012),
                athleteCount = 120,
                verifiedCount = 95,
                activeCount = 110,
                avgPerformanceScore = 88.5,
                avgAiAssessmentScore = 84.0,
                tournamentParticipation = 18,
                trend = "↑ Improving"
            ),
            DistrictTalentRegion(
                id = "port_stadium",
                name = "Athletics Center - Port Stadium",
                center = LatLng(17.6896, 83.2314),
                athleteCount = 85,
                verifiedCount = 60,
                activeCount = 70,
                avgPerformanceScore = 62.0,
                avgAiAssessmentScore = 68.5,
                tournamentParticipation = 12,
                trend = "→ Stable"
            ),
            DistrictTalentRegion(
                id = "au_ground",
                name = "Kabaddi Zone - AU Ground",
                center = LatLng(17.7123, 83.2845),
                athleteCount = 45,
                verifiedCount = 15,
                activeCount = 30,
                avgPerformanceScore = 35.0,
                avgAiAssessmentScore = 40.0,
                tournamentParticipation = 5,
                trend = "↓ Declining"
            ),
            DistrictTalentRegion(
                id = "gajuwaka",
                name = "Gajuwaka Sports Complex",
                center = LatLng(17.6800, 83.2000),
                athleteCount = 110,
                verifiedCount = 80,
                activeCount = 95,
                avgPerformanceScore = 75.0,
                avgAiAssessmentScore = 79.0,
                tournamentParticipation = 14,
                trend = "↑ Improving"
            )
        )
    }

    private fun displayRegionsOnMap(regions: List<DistrictTalentRegion>) {
        mMap.clear()
        regionMap.clear()

        var totalAthletesCount = 0

        for (region in regions) {
            regionMap[region.name] = region
            totalAthletesCount += region.athleteCount

            val level = region.getTalentLevel()
            val color = level.color

            // Add Custom Marker
            mMap.addMarker(
                MarkerOptions()
                    .position(region.center)
                    .title(region.name)
                    .snippet("Score: ${region.calculateScore()}/100 • ${level.label}")
                    .icon(BitmapDescriptorFactory.defaultMarker(getHueFromColor(color)))
            )

            // Add Region Circle
            mMap.addCircle(
                CircleOptions()
                    .center(region.center)
                    .radius(1500.0) // 1.5km
                    .strokeWidth(3f)
                    .strokeColor(color)
                    .fillColor(adjustAlpha(color, 0.25f))
            )
        }

        // Update Chip with total athlete count
        val chipPulse = findViewById<Chip>(R.id.chip_live_pulse)
        chipPulse?.text = "LIVE PULSE: $totalAthletesCount Athletes"
    }

    private fun showRegionDetailSheet(region: DistrictTalentRegion) {
        val dialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.layout_region_detail_sheet, null)

        val score = region.calculateScore()
        val level = region.getTalentLevel()

        sheetView.findViewById<TextView>(R.id.tv_detail_region_name).text = region.name
        sheetView.findViewById<TextView>(R.id.tv_detail_talent_level).apply {
            text = "${level.label.uppercase()} TALENT REGION"
            setTextColor(level.color)
        }
        sheetView.findViewById<TextView>(R.id.tv_detail_talent_score).apply {
            text = score.toString()
            setTextColor(level.color)
        }
        
        sheetView.findViewById<TextView>(R.id.tv_detail_total_athletes).text = 
            if (region.athleteCount > 0) region.athleteCount.toString() else "Data unavailable"
            
        sheetView.findViewById<TextView>(R.id.tv_detail_verified_athletes).text = 
            if (region.athleteCount > 0) region.verifiedCount.toString() else "Data unavailable"

        sheetView.findViewById<TextView>(R.id.tv_detail_avg_perf).text = 
            if (region.avgPerformanceScore > 0) String.format("%.1f%%", region.avgPerformanceScore) else "Data unavailable"

        sheetView.findViewById<TextView>(R.id.tv_detail_ai_score).text = 
            if (region.avgAiAssessmentScore > 0) String.format("%.1f%%", region.avgAiAssessmentScore) else "Data unavailable"

        sheetView.findViewById<TextView>(R.id.tv_detail_tournaments).text = 
            "${region.tournamentParticipation} Events"

        sheetView.findViewById<TextView>(R.id.tv_detail_trend).text = region.trend

        dialog.setContentView(sheetView)
        dialog.show()
    }

    private fun showInfoDialog() {
        AlertDialog.Builder(this)
            .setTitle("District Talent Region Methodology")
            .setMessage(
                "District Talent Region shows the relative strength and activity of sports talent across geographic regions.\n\n" +
                "• BLUE — DEVELOPING (0–39)\nLower current talent activity or performance.\n\n" +
                "• GREEN — ACTIVE (40–69)\nModerate and growing talent activity.\n\n" +
                "• YELLOW — STRONG (70–100)\nHigher concentration of verified, high-performing talent.\n\n" +
                "Scores are calculated using a weighted combination of verified athlete counts, average AI biomechanics performance, and tournament participation."
            )
            .setPositiveButton("Got It", null)
            .show()
    }

    private fun getHueFromColor(color: Int): Float {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        return hsv[0]
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = Math.round(Color.alpha(color) * factor)
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }
}
