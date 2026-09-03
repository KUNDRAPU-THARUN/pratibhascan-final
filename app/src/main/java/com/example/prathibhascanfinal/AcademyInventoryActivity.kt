package com.example.prathibhascanfinal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import com.example.prathibhascanfinal.ui.base.BaseActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

import com.example.prathibhascanfinal.ui.adapter.InventoryListAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest

class AcademyInventoryActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    private lateinit var adapter: InventoryListAdapter
    private var academyId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_academy_inventory)
        setupEdgeToEdge(findViewById(R.id.inventory_root))

        academyId = intent.getIntExtra("ACADEMY_ID", 0)
        
        if (academyId == 0) {
            val session = SessionManager(this)
            val email = session.getEmail() ?: ""
            lifecycleScope.launch {
                val academy = AppDatabase.getDatabase(this@AcademyInventoryActivity).academyDao().getAcademyByEmail(email)
                if (academy != null) {
                    academyId = academy.id
                    initUI()
                    observeInventory()
                } else {
                    Toast.makeText(this@AcademyInventoryActivity, "Academy not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else {
            initUI()
            observeInventory()
        }
    }

    private fun initUI() {
        val rv = findViewById<RecyclerView>(R.id.rv_inventory)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = InventoryListAdapter(
            onView = { showEquipmentDetails(it) },
            onEdit = { showEditEquipmentDialog(it) },
            onDelete = { confirmDeleteEquipment(it) }
        )
        rv.adapter = adapter

        findViewById<View>(R.id.fab_add_equipment).setOnClickListener {
            showCreateEquipmentDialog()
        }
    }

    private fun observeInventory() {
        lifecycleScope.launch {
            // Since we don't have a specific Flow for inventory in FirestoreRepository yet, 
            // we can add one or use a one-shot fetch for now.
            // For true dynamic, let's add it to FirestoreRepository.
            loadInventory() 
        }
    }

    private fun loadInventory() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@AcademyInventoryActivity)
            val items = db.academyManagementDao().getInventory(academyId)
            adapter.submitList(items)
            findViewById<View>(R.id.layout_empty_inventory).visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showCreateEquipmentDialog() {
        showEquipmentDialog(null)
    }

    private fun showEditEquipmentDialog(equipment: Equipment) {
        showEquipmentDialog(equipment)
    }

    private fun showEquipmentDialog(equipment: Equipment?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_equipment, null)
        val nameEt = dialogView.findViewById<EditText>(R.id.et_equip_name)
        val catEt = dialogView.findViewById<EditText>(R.id.et_equip_category)
        val qtyEt = dialogView.findViewById<EditText>(R.id.et_equip_quantity)
        val condEt = dialogView.findViewById<EditText>(R.id.et_equip_condition)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btn_confirm_add_equip)

        if (equipment != null) {
            nameEt.setText(equipment.name)
            catEt.setText(equipment.category)
            qtyEt.setText(equipment.totalQuantity.toString())
            condEt.setText(equipment.condition)
            btnConfirm.text = "Update Equipment"
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnConfirm.setOnClickListener {
            val name = nameEt.text.toString().trim()
            val cat = catEt.text.toString().trim()
            val qty = qtyEt.text.toString().toIntOrNull() ?: 0
            val cond = condEt.text.toString().trim()

            if (name.isNotEmpty()) {
                val updated = (equipment ?: Equipment(academyId = academyId)).copy(
                    name = name,
                    category = cat,
                    totalQuantity = qty,
                    availableStock = qty, // simplified
                    condition = cond
                )
                lifecycleScope.launch {
                    val repo = com.example.prathibhascanfinal.data.repository.AcademyRepository(this@AcademyInventoryActivity)
                    repo.addEquipment(updated)
                    loadInventory()
                    dialog.dismiss()
                    Toast.makeText(this@AcademyInventoryActivity, "Equipment Saved Successfully", Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
    }

    private fun confirmDeleteEquipment(equipment: Equipment) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Equipment")
            .setMessage("Are you sure you want to delete '${equipment.name}'? This will also remove it from cloud storage.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val repo = com.example.prathibhascanfinal.data.repository.AcademyRepository(this@AcademyInventoryActivity)
                        repo.deleteEquipment(equipment)
                        loadInventory()
                        Toast.makeText(this@AcademyInventoryActivity, "Equipment Deleted", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@AcademyInventoryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEquipmentDetails(equipment: Equipment) {
        MaterialAlertDialogBuilder(this)
            .setTitle(equipment.name)
            .setMessage("Category: ${equipment.category}\nCondition: ${equipment.condition}")
            .show()
    }
}

