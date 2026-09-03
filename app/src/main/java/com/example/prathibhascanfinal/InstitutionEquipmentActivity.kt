package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.prathibhascanfinal.data.repository.InstitutionRepository
import com.example.prathibhascanfinal.ui.adapter.EquipmentListAdapter
import com.example.prathibhascanfinal.ui.base.BaseActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class InstitutionEquipmentActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()
    private lateinit var repository: InstitutionRepository

    private lateinit var adapter: EquipmentListAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var institutionId: Int = 0
    private var allEquipment = listOf<InstitutionEquipment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_institution_equipment)
        setupEdgeToEdge(findViewById(R.id.inst_equipment_root))

        repository = InstitutionRepository(this)
        
        val session = SessionManager(this)
        val email = session.getEmail() ?: ""
        lifecycleScope.launch {
            val inst = AppDatabase.getDatabase(this@InstitutionEquipmentActivity).institutionDao().getInstitutionByEmail(email)
            if (inst != null) {
                institutionId = inst.id
                initUI()
                observeEquipment()
            } else {
                initUI()
                observeEquipment()
            }
        }
    }

    private fun initUI() {
        swipeRefresh = findViewById(R.id.swipe_refresh)
        swipeRefresh.setOnRefreshListener { observeEquipment() }
        swipeRefresh.setColorSchemeColors(getColor(R.color.brand_blue))

        val rv = findViewById<RecyclerView>(R.id.rv_inst_equipment)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = EquipmentListAdapter(
            onView = { showEquipmentDetails(it) },
            onEdit = { showEquipmentDialog(it) },
            onDelete = { confirmDeleteEquipment(it) }
        )
        rv.adapter = adapter

        findViewById<View>(R.id.fab_add_equipment).setOnClickListener {
            showEquipmentDialog(null)
        }

        findViewById<EditText>(R.id.et_search_equipment)?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterEquipment(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun observeEquipment() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            // Need a Flow for Equipment in InstitutionManagementDao.
            val db = AppDatabase.getDatabase(this@InstitutionEquipmentActivity).institutionManagementDao()
            db.getInventoryFlow(institutionId).collectLatest { list ->
                allEquipment = list
                updateList(list)
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun updateList(list: List<InstitutionEquipment>) {
        adapter.submitList(list)
        findViewById<View>(R.id.layout_empty_inventory).visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun filterEquipment(query: String) {
        val filtered = if (query.isEmpty()) {
            allEquipment
        } else {
            allEquipment.filter { 
                it.name.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true) 
            }
        }
        updateList(filtered)
    }

    private fun showEquipmentDialog(equipment: InstitutionEquipment?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_equipment, null)
        val nameEt = dialogView.findViewById<EditText>(R.id.et_equip_name)
        val catEt = dialogView.findViewById<EditText>(R.id.et_equip_category)
        val qtyEt = dialogView.findViewById<EditText>(R.id.et_equip_quantity)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btn_confirm_add_equip)

        if (equipment != null) {
            nameEt.setText(equipment.name)
            catEt.setText(equipment.category)
            qtyEt.setText(equipment.totalQuantity.toString())
            btnConfirm.text = "UPDATE EQUIPMENT"
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnConfirm.setOnClickListener {
            val name = nameEt.text.toString().trim()
            val cat = catEt.text.toString().trim()
            val qty = qtyEt.text.toString().toIntOrNull() ?: 0

            if (name.isNotEmpty()) {
                val updated = (equipment ?: InstitutionEquipment(institutionId = institutionId)).copy(
                    name = name,
                    category = cat,
                    totalQuantity = qty
                )
                lifecycleScope.launch {
                    try {
                        repository.addEquipment(updated)
                        Snackbar.make(findViewById(R.id.rv_inst_equipment), "✔ Equipment Saved Successfully", Snackbar.LENGTH_LONG).show()
                        dialog.dismiss()
                    } catch (e: Exception) {
                        Toast.makeText(this@InstitutionEquipmentActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun confirmDeleteEquipment(equipment: InstitutionEquipment) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Equipment")
            .setMessage("Are you sure you want to remove '${equipment.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        repository.deleteEquipment(equipment)
                        Snackbar.make(findViewById(R.id.rv_inst_equipment), "Equipment Removed", Snackbar.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@InstitutionEquipmentActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEquipmentDetails(equipment: InstitutionEquipment) {
        val intent = Intent(this, ManagementDetailActivity::class.java).apply {
            putExtra("ENTITY_TYPE", "INST_EQUIPMENT")
            putExtra("ENTITY_ID", equipment.equipmentId)
        }
        startActivity(intent)
    }
}

