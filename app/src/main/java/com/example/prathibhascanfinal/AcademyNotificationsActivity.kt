package com.example.prathibhascanfinal

import androidx.activity.viewModels

import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.example.prathibhascanfinal.ui.base.BaseActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AcademyNotificationsActivity : BaseActivity() {

    override val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleHelper.applySavedLocale(this)
        setContentView(R.layout.activity_notifications)
        findViewById<View>(android.R.id.content)?.applySystemBarsPadding()

        setupSystemInsets()
        setupHeader()
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
        findViewById<TextView>(R.id.tv_welcome_name)?.text = "Alerts & Updates"
        findViewById<TextView>(R.id.tv_profile_subtitle)?.text = "Stay informed"
        
        findViewById<View>(R.id.btn_header_back)?.apply {
            visibility = View.VISIBLE
            setOnClickListener { finish() }
        }
    }
}

