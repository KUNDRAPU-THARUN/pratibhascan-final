package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.example.prathibhascanfinal.ui.base.BaseActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import androidx.core.graphics.toColorInt

class AcademyAnalyticsActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleHelper.applySavedLocale(this)
        setContentView(R.layout.activity_academy_analytics)
        findViewById<View>(android.R.id.content)?.applySystemBarsPadding()

        setupSystemInsets()
        setupHeader()
        
        setupLineChart()
        setupBarChart()
        setupPieChart()
    }

    private fun setupSystemInsets() {
        val header = findViewById<View>(R.id.layout_global_header)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            header?.setPadding(header.paddingLeft, systemBars.top, header.paddingRight, 0)
            insets
        }
    }

    private fun setupHeader() {
        findViewById<TextView>(R.id.tv_welcome_name)?.text = getString(R.string.academy_analytics_label)
        findViewById<TextView>(R.id.tv_profile_subtitle)?.text = "Real-time performance data"
        
        findViewById<View>(R.id.btn_header_back)?.apply {
            visibility = View.VISIBLE
            setOnClickListener { finish() }
        }
    }

    private fun setupLineChart() {
        val chart = findViewById<LineChart>(R.id.chart_athlete_growth) ?: return
        
        val entries = mutableListOf<Entry>()
        entries.add(Entry(0f, 40f))
        entries.add(Entry(1f, 48f))
        entries.add(Entry(2f, 70f))
        entries.add(Entry(3f, 85f))
        entries.add(Entry(4f, 110f))
        entries.add(Entry(5f, 142f))

        val dataSet = LineDataSet(entries, "Athletes")
        dataSet.color = "#3B82F6".toColorInt()
        dataSet.setCircleColor(Color.WHITE)
        dataSet.lineWidth = 3f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor("#3B82F6")
        dataSet.fillAlpha = 50
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        chart.data = LineData(dataSet)
        chart.description.isEnabled = false
        chart.xAxis.textColor = Color.WHITE
        chart.axisLeft.textColor = Color.WHITE
        chart.axisRight.isEnabled = false
        chart.legend.textColor = Color.WHITE
        chart.animateX(1000)
        chart.invalidate()
    }

    private fun setupBarChart() {
        val chart = findViewById<BarChart>(R.id.chart_revenue_bar) ?: return
        
        val entries = mutableListOf<BarEntry>()
        entries.add(BarEntry(0f, 2.5f))
        entries.add(BarEntry(1f, 3.2f))
        entries.add(BarEntry(2f, 4.8f))
        entries.add(BarEntry(3f, 5.5f))
        entries.add(BarEntry(4f, 8.2f))
        entries.add(BarEntry(5f, 12.4f))

        val dataSet = BarDataSet(entries, "Revenue (in Lakhs)")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextColor = Color.WHITE

        chart.data = BarData(dataSet)
        chart.description.isEnabled = false
        chart.xAxis.textColor = Color.WHITE
        chart.axisLeft.textColor = Color.WHITE
        chart.axisRight.isEnabled = false
        chart.legend.textColor = Color.WHITE
        chart.animateY(1000)
        chart.invalidate()
    }

    private fun setupPieChart() {
        val chart = findViewById<PieChart>(R.id.chart_sports_pie) ?: return
        
        val entries = mutableListOf<PieEntry>()
        entries.add(PieEntry(45f, "Cricket"))
        entries.add(PieEntry(30f, "Football"))
        entries.add(PieEntry(15f, "Badminton"))
        entries.add(PieEntry(10f, "Other"))

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = ColorTemplate.COLORFUL_COLORS.toList()
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 12f

        chart.data = PieData(dataSet)
        chart.description.isEnabled = false
        chart.centerText = "Sports Share"
        chart.setCenterTextColor(Color.BLACK)
        chart.setHoleColor(Color.TRANSPARENT)
        chart.legend.textColor = Color.WHITE
        chart.animateXY(1000, 1000)
        chart.invalidate()
    }
}

