package com.example.prathibhascanfinal

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.prathibhascanfinal.databinding.ItemScoutReportBinding

class ScoutReportAdapter(
    private var reports: List<ScoutReport>,
    private val onActionClick: (ScoutReport) -> Unit
) : RecyclerView.Adapter<ScoutReportAdapter.ReportViewHolder>() {

    class ReportViewHolder(val binding: ItemScoutReportBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemScoutReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reports[position]
        with(holder.binding) {
            tvStudentName.text = report.studentName
            tvStatus.text = report.status.uppercase()
            tvSport.text = "${report.sportCategory} • ${report.institutionName}"
            tvNote.text = report.recommendationNote
            tvScoreLabel.text = "AI Talent Score: ${String.format("%.1f", report.aiScore)}/100"

            btnAction.setOnClickListener { onActionClick(report) }
        }
    }

    override fun getItemCount() = reports.size

    fun updateData(newReports: List<ScoutReport>) {
        reports = newReports
        notifyDataSetChanged()
    }
}
