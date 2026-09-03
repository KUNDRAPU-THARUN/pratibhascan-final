package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.prathibhascanfinal.data.repository.AcademyRepository
import com.example.prathibhascanfinal.data.repository.FirestoreRepository
import com.example.prathibhascanfinal.ui.adapter.FacilityListAdapter
import com.example.prathibhascanfinal.ui.base.BaseActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AcademyFacilityActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()
    private lateinit var repository: AcademyRepository

    private lateinit var adapter: FacilityListAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var academyId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_academy_facility)
        setupEdgeToEdge(findViewById(R.id.facility_root))

        repository = AcademyRepository(this)
        academyId = intent.getIntExtra("ACADEMY_ID", 0)
        
        if (academyId == 0) {
            val session = SessionManager(this)
            val email = session.getEmail() ?: ""
            lifecycleScope.launch {
                val academy = AppDatabase.getDatabase(this@AcademyFacilityActivity).academyDao().getAcademyByEmail(email)
                if (academy != null) {
                    academyId = academy.id
                    initUI()
                    observeFacilities()
                }
            }
        } else {
            initUI()
            observeFacilities()
        }
    }

    private fun initUI() {
        swipeRefresh = findViewById(R.id.swipe_refresh)
        swipeRefresh.setOnRefreshListener { observeFacilities() }
        swipeRefresh.setColorSchemeColors(getColor(R.color.brand_blue))

        val rv = findViewById<RecyclerView>(R.id.rv_facilities)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = FacilityListAdapter(
            onView = { showFacilityDetails(it) },
            onEdit = { showEditFacilityDialog(it) },
            onDelete = { confirmDeleteFacility(it) }
        )
        rv.adapter = adapter

        findViewById<View>(R.id.fab_add_facility).setOnClickListener {
            showCreateFacilityDialog()
        }
    }

    private fun observeFacilities() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            repository.getFacilitiesFlow(academyId).collectLatest { list ->
                adapter.submitList(list)
                findViewById<View>(R.id.layout_empty_facilities).visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showCreateFacilityDialog() {
        showFacilityDialog(null)
    }

    private fun showEditFacilityDialog(facility: Facility) {
        showFacilityDialog(facility)
    }

    private fun showFacilityDialog(facility: Facility?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_facility, null)
        val nameEt = dialogView.findViewById<EditText>(R.id.et_facility_name)
        val typeEt = dialogView.findViewById<EditText>(R.id.et_facility_type)
        val sportEt = dialogView.findViewById<EditText>(R.id.et_facility_sport)
        val capEt = dialogView.findViewById<EditText>(R.id.et_facility_capacity)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btn_confirm_add_facility)

        if (facility != null) {
            nameEt.setText(facility.name)
            typeEt.setText(facility.type)
            sportEt.setText(facility.sport)
            capEt.setText(facility.capacity.toString())
            btnConfirm.text = "Update Facility"
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnConfirm.setOnClickListener {
            val name = nameEt.text.toString().trim()
            val type = typeEt.text.toString().trim()
            val sport = sportEt.text.toString().trim()
            val cap = capEt.text.toString().toIntOrNull() ?: 0

            if (name.isNotEmpty()) {
                val updated = (facility ?: Facility(academyId = academyId)).copy(
                    name = name,
                    type = type,
                    sport = sport,
                    capacity = cap
                )
                lifecycleScope.launch {
                    try {
                        repository.addFacility(updated)
                        Snackbar.make(findViewById(R.id.rv_facilities), "✔ Facility Saved Successfully", Snackbar.LENGTH_LONG).show()
                        dialog.dismiss()
                    } catch (e: Exception) {
                        Toast.makeText(this@AcademyFacilityActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun confirmDeleteFacility(facility: Facility) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Facility")
            .setMessage("Are you sure you want to remove '${facility.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        repository.deleteFacility(facility)
                        Snackbar.make(findViewById(R.id.rv_facilities), "Facility Deleted", Snackbar.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@AcademyFacilityActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFacilityDetails(facility: Facility) {
        val intent = Intent(this, ManagementDetailActivity::class.java).apply {
            putExtra("ENTITY_TYPE", "FACILITY")
            putExtra("ENTITY_ID", facility.id)
        }
        startActivity(intent)
    }
}

