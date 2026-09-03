package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.prathibhascanfinal.ui.base.BaseActivity
import kotlinx.coroutines.launch

class AcademyTransferActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_academy_transfer)
        findViewById<View>(android.R.id.content)?.applySystemBarsPadding()

        findViewById<Button>(R.id.btn_initiate_transfer).setOnClickListener {
            showTransferDialog()
        }
    }

    private fun showTransferDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_initiate_transfer, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<View>(R.id.btn_confirm_transfer).setOnClickListener {
            val aId = dialogView.findViewById<EditText>(R.id.et_transfer_athlete_id).text.toString().toIntOrNull() ?: 0
            val targetId = dialogView.findViewById<EditText>(R.id.et_transfer_target_academy).text.toString().toIntOrNull() ?: 0

            if (aId > 0 && targetId > 0) {
                lifecycleScope.launch {
                    AppDatabase.getDatabase(this@AcademyTransferActivity).academyManagementDao().transferAthlete(aId, targetId)
                    Toast.makeText(this@AcademyTransferActivity, "Transfer process initiated for Athlete ID $aId", Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                }
            } else {
                Toast.makeText(this, "Valid IDs are required", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }
}

