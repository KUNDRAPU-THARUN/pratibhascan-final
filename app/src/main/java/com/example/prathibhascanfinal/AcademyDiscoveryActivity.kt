package com.example.prathibhascanfinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.viewModels
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prathibhascanfinal.ui.adapter.DiscoveryAcademyAdapter
import com.example.prathibhascanfinal.ui.base.BaseActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AcademyDiscoveryActivity : BaseActivity() {

    override val viewModel: AcademyDiscoveryViewModel by viewModels()
    private lateinit var adapter: DiscoveryAcademyAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_academy_discovery)
        setupEdgeToEdge(findViewById(R.id.academy_discovery_root))

        initUI()
        observeState()
    }

    private fun initUI() {
        val rv = findViewById<RecyclerView>(R.id.rv_academy_results)
        adapter = DiscoveryAcademyAdapter(
            onDetails = { showAcademyDetails(it) }
        ) { applyToAcademy(it) }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        findViewById<EditText>(R.id.et_search_academies).addTextChangedListener {
            viewModel.setSearchQuery(it?.toString() ?: "")
        }

        findViewById<ChipGroup>(R.id.chip_group_academy_sports).setOnCheckedStateChangeListener { group, checkedIds ->
            val chipId = checkedIds.firstOrNull() ?: R.id.chip_aca_all
            val chip = group.findViewById<Chip>(chipId)
            val sport = if (chipId == R.id.chip_aca_all) null else chip.text.toString()
            viewModel.setSportFilter(sport)
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                findViewById<ProgressBar>(R.id.progress_academy_discovery).visibility = if (state.isLoading) View.VISIBLE else View.GONE
                adapter.submitList(state.academies)
                findViewById<TextView>(R.id.tv_empty_academy_discovery).visibility = if (!state.isLoading && state.academies.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showAcademyDetails(academy: Academy) {
        val intent = Intent(this, AcademyProfileActivity::class.java).apply {
            putExtra("ACADEMY_ID", academy.id)
            putExtra("VIEW_ONLY", true)
        }
        startActivity(intent)
    }

    private fun applyToAcademy(academy: Academy) {
        val input = EditText(this)
        input.hint = "Tell the academy why you want to join..."
        val container = FrameLayout(this)
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(60, 20, 60, 10)
        input.layoutParams = params
        container.addView(input)
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Apply to ${academy.academyName}")
            .setMessage("Your profile and talent scores will be shared with the academy.")
            .setView(container)
            .setPositiveButton("Submit Application") { _, _ ->
                val msg = input.text.toString().trim()
                viewModel.applyToAcademy(academy, msg)
                Toast.makeText(this, "Application submitted!", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
